import importlib.util
import hashlib
import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch


MODULE_PATH = Path(__file__).parents[1] / "import_gt6_oredict.py"
SPEC = importlib.util.spec_from_file_location("import_gt6_oredict", MODULE_PATH)
MODULE = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
sys.modules[SPEC.name] = MODULE
SPEC.loader.exec_module(MODULE)


class GT6ImportUnitsTest(unittest.TestCase):
    def test_known_copper_iron_tin_temperatures(self):
        records = json.loads(MODULE.NORMALIZED_MATERIALS.read_text(encoding="utf-8"))["records"]
        by_name = {record["source_name"]: record for record in records}
        self.assertAlmostEqual(1083.85, by_name["Copper"]["thermal"]["melting_point_celsius"])
        self.assertAlmostEqual(1537.85, by_name["Iron"]["thermal"]["melting_point_celsius"])
        self.assertAlmostEqual(231.85, by_name["Tin"]["thermal"]["melting_point_celsius"])

    def test_exact_and_non_integral_u_are_not_rounded(self):
        self.assertEqual(144, MODULE.GT6ImportUnits.exact_amount(648_648_000)["cc_units"])
        fraction = MODULE.GT6ImportUnits.exact_amount(1)
        self.assertFalse(fraction["integral_cc_units"])
        self.assertIsNone(fraction["cc_units"])
        self.assertEqual(1, fraction["numerator_u"])


class DumpSchemaTest(unittest.TestCase):
    def test_recipe_evidence_rejects_negative_material_ids(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            maps = root / "maps"
            maps.mkdir()
            (maps / "test.map.json").write_text(
                json.dumps({
                    "nameInternal": "test.map",
                    "recipes": [{
                        "inputs": [{
                            "item": "gregtech:gt.meta.dust",
                            "meta": -1,
                            "count": 1,
                        }],
                        "outputs": [],
                    }],
                }),
                encoding="utf-8",
            )
            with self.assertRaisesRegex(
                MODULE.ImportError,
                "cannot use negative material id -1",
            ):
                MODULE.recipe_material_evidence(
                    root,
                    ["test.map"],
                    {},
                )

    def test_sentinel_source_never_performs_evidence_lookup(self):
        class LookupGuard(dict):
            def get(self, key, default=None):
                if key < 0:
                    raise AssertionError(f"negative lookup: {key}")
                return super().get(key, default)

        evidence = LookupGuard({
            -1: {"item": {"should-never-be-read": 441}},
            10: {"item": {"test.map": 1}},
        })
        self.assertEqual(
            {},
            MODULE.material_evidence_for_source(evidence, -1),
        )
        self.assertEqual(
            {"item": {"test.map": 1}},
            MODULE.material_evidence_for_source(evidence, 10),
        )

    def test_index_schema_provenance_and_counts(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            materials = [{"id": 1}]
            prefixes = [{"id": "dust"}]
            fluids = {"water": {"material": "Water", "materialId": 1, "amount": 1000}}
            (root / "materials.json").write_text(json.dumps(materials), encoding="utf-8")
            (root / "prefixes.json").write_text(json.dumps(prefixes), encoding="utf-8")
            (root / "fluid_map.json").write_text(json.dumps(fluids), encoding="utf-8")
            (root / "index.json").write_text(json.dumps({
                "materialCount": 1,
                "prefixCount": 1,
                "fluidMapCount": 1,
                "materialsFile": "oredict/materials.json",
                "prefixesFile": "oredict/prefixes.json",
                "fluidMapFile": "oredict/fluid_map.json",
            }), encoding="utf-8")
            with patch.dict(MODULE.EXPECTED_COUNTS, {
                    "materialCount": 1, "prefixCount": 1, "fluidMapCount": 1}, clear=True):
                provenance, loaded_materials, loaded_prefixes, loaded_fluids = MODULE.load_dump(root)
            self.assertEqual(materials, loaded_materials)
            self.assertEqual(prefixes, loaded_prefixes)
            self.assertEqual(fluids, loaded_fluids)
            self.assertEqual(4, len(provenance["sha256"]))

    def test_reference_only_check_is_dump_independent_and_deterministic(self):
        first = MODULE.check_committed()
        second = MODULE.check_committed()
        self.assertEqual(first, second)
        self.assertEqual(2214, first["normalized_materials"])
        self.assertEqual(468, first["normalized_prefixes"])
        self.assertNotIn("active_prefixes", first)
        self.assertEqual(0, first["newly_activated_prefixes"])
        self.assertEqual(43, first["runtime_prefixes_total"])

    def test_check_modes_do_not_modify_import_artifacts(self):
        paths = [
            MODULE.NORMALIZED_MATERIALS,
            MODULE.NORMALIZED_PREFIXES,
            MODULE.NORMALIZED_FLUIDS,
            MODULE.POLICY,
            MODULE.CROSS_REFERENCE,
            MODULE.MANIFEST,
            MODULE.MATERIAL_DIR / "index.json",
        ]

        def snapshot():
            return {
                str(path): hashlib.sha256(path.read_bytes()).hexdigest()
                for path in paths
            }

        for mode in ([], ["--reference-only"]):
            with self.subTest(mode=mode or ["raw"]):
                before = snapshot()
                completed = subprocess.run(
                    [
                        sys.executable,
                        str(MODULE_PATH),
                        "--check",
                        *mode,
                    ],
                    cwd=MODULE.ROOT,
                    capture_output=True,
                    text=True,
                    timeout=300,
                )
                self.assertEqual(
                    0,
                    completed.returncode,
                    completed.stdout + completed.stderr,
                )
                self.assertEqual(before, snapshot())

    def test_manifest_rejects_extra_or_modified_generated_materials(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            index = ["foo.json"]
            foo = {"id": "foo"}
            (root / "index.json").write_text(json.dumps(index), encoding="utf-8")
            (root / "foo.json").write_text(json.dumps(foo), encoding="utf-8")
            manifest = {
                "expected_material_index": index,
                "generated_material_files": ["foo.json"],
                "material_definition_hashes": {
                    "index.json": MODULE.stable_hash(index),
                    "foo.json": MODULE.stable_hash(foo),
                },
            }
            MODULE.validate_material_manifest(manifest, root, check_authored=False)
            (root / "extra.json").write_text('{"id":"extra"}', encoding="utf-8")
            with self.assertRaisesRegex(MODULE.ImportError, "file set mismatch"):
                MODULE.validate_material_manifest(manifest, root, check_authored=False)
            (root / "extra.json").unlink()
            (root / "foo.json").write_text('{"id":"changed"}', encoding="utf-8")
            with self.assertRaisesRegex(MODULE.ImportError, "hash mismatch"):
                MODULE.validate_material_manifest(manifest, root, check_authored=False)

    def test_manifest_covers_aliases_and_every_activation_evidence_map(self):
        manifest = json.loads(MODULE.MANIFEST.read_text(encoding="utf-8"))
        policy = json.loads(MODULE.POLICY.read_text(encoding="utf-8"))
        self.assertIn(
            MODULE.ALIASES.name,
            manifest["artifact_hashes"],
        )
        evidence = manifest["provenance"]["activation_evidence_maps"]
        self.assertEqual(
            set(policy["policy"]["evidence_maps"]),
            set(evidence),
        )
        self.assertTrue(all(
            set(record) == {"path", "sha256"}
            and len(record["sha256"]) == 64
            for record in evidence.values()
        ))

    def test_reference_manifest_rejects_alias_tampering(self):
        required_names = {
            MODULE.NORMALIZED_MATERIALS.name,
            MODULE.NORMALIZED_PREFIXES.name,
            MODULE.NORMALIZED_FLUIDS.name,
            MODULE.POLICY.name,
            MODULE.CROSS_REFERENCE.name,
            MODULE.ALIASES.name,
            MODULE.ACTIVATION_OVERRIDES.name,
            "acceptance_form_corrections.json",
            MODULE.AUTHORED_BASELINE.name,
            MODULE.gt6_l3_materials.GENERATION_BITS_PATH.name,
            MODULE.gt6_l3_materials.MAPPING_PATH.name,
            MODULE.gt6_l3_materials.OUT.name,
            MODULE.GATE_OPERANDS.name,
            MODULE.ORE_CHAIN.name,
            MODULE.ORE_CHAIN_OPERANDS.name,
            MODULE.REGISTRY_BUDGET.name,
            MODULE.REGISTRATION_GATE_MANIFEST_KEY,
        }
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            artifact_hashes = {}
            for name in required_names:
                value = {"name": name}
                (root / name).write_text(json.dumps(value), encoding="utf-8")
                artifact_hashes[name] = MODULE.stable_hash(value)
            manifest = {"artifact_hashes": artifact_hashes}
            MODULE.validate_manifest_artifact_hashes(manifest, root)
            (root / MODULE.ALIASES.name).write_text(
                '{"tampered":true}',
                encoding="utf-8",
            )
            with self.assertRaisesRegex(MODULE.ImportError, "hash mismatch"):
                MODULE.validate_manifest_artifact_hashes(manifest, root)


class NormalizedDataTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.materials = json.loads(
            MODULE.NORMALIZED_MATERIALS.read_text(encoding="utf-8"))["records"]
        cls.prefixes = json.loads(
            MODULE.NORMALIZED_PREFIXES.read_text(encoding="utf-8"))["records"]
        cls.policy = json.loads(MODULE.POLICY.read_text(encoding="utf-8"))

    def test_bronze_composition_targets_and_byproducts(self):
        bronze = next(record for record in self.materials if record["source_name"] == "Bronze")
        self.assertEqual(
            [("Copper", 108), ("Tin", 36)],
            [(entry["material"], entry["cc_units"]) for entry in bronze["components"]],
        )
        self.assertEqual(144, bronze["processing_targets"]["smelting"]["cc_units"])
        self.assertEqual(["Copper", "Tin", "Arsenic"], bronze["byproducts"])

    def test_prefix_generation_and_unit_mapping_without_activation(self):
        by_name = {record["source_name"]: record for record in self.prefixes}
        self.assertEqual("ingot", by_name["ingot"]["t0_mapping"])
        self.assertEqual(144, by_name["ingot"]["amount"]["cc_units"])
        self.assertEqual("crushed_ore", by_name["crushed"]["t0_mapping"])
        self.assertEqual(162, by_name["crushed"]["amount"]["cc_units"])
        self.assertTrue(all(not record["activated"] for record in self.prefixes))

    def test_factual_ore_domain_and_explicit_world_ore_exceptions(self):
        l3_document = json.loads(
            MODULE.gt6_l3_materials.OUT.read_text(encoding="utf-8")
        )
        index = json.loads(
            (MODULE.MATERIAL_DIR / "index.json").read_text(encoding="utf-8")
        )
        ore_materials = []
        for filename in index:
            material = json.loads(
                (MODULE.MATERIAL_DIR / filename).read_text(encoding="utf-8")
            )
            if "ore" in MODULE.gt6_l3_materials.resolve_material_forms(
                material, l3_document
            ):
                ore_materials.append(material["id"])
        budget = json.loads(MODULE.REGISTRY_BUDGET.read_text(encoding="utf-8"))[
            "required_assertions"
        ]
        self.assertEqual(budget["factual_ore_materials"], len(ore_materials))
        self.assertEqual(137, len(ore_materials))
        self.assertIn(
            "ore",
            json.loads(
                (MODULE.MATERIAL_DIR / "iron.json").read_text(encoding="utf-8")
            )["include_prefixes"],
        )
        self.assertIn(
            "ore",
            json.loads(
                (MODULE.MATERIAL_DIR / "tungsten.json").read_text(encoding="utf-8")
            )["include_prefixes"],
        )

    def test_selection_policy_covers_every_record_with_evidence(self):
        records = self.policy["records"]
        self.assertEqual(2214, len(records))
        self.assertEqual({"ACTIVE", "OUT_OF_SCOPE"},
                         {record["status"] for record in records})
        self.assertTrue(all(record["reason"] for record in records))
        authored = {record["cc_id"] for record in records if "preserved authored" in record["reason"]}
        self.assertEqual(set(MODULE.AUTHORED_IDS), authored)
        active = {record["source_name"] for record in records if record["status"] == "ACTIVE"}
        self.assertEqual(1773, len(active))
        self.assertTrue(all(
            record["recipe_evidence"]
            or record["source_name"] in MODULE.AUTHORED_IDS.values()
            or record["reason"].startswith("transitive ")
            or record["activation_provenance"]["kind"] == "stable_catalog"
            for record in records if record["status"] == "ACTIVE"))
        self.assertEqual(0, self.policy["reference_validation"]["dropped_active_references"])

    def test_fluid_and_transitive_dependency_fixtures(self):
        records = {
            record["source_name"]: record
            for record in self.policy["records"] if record["status"] == "ACTIVE"
        }
        self.assertIn("fluid evidence", records["Oxygen"]["reason"])
        self.assertIn("fluid evidence", records["Water"]["reason"])
        self.assertEqual([], records["Oxygen"]["t0_forms"])
        self.assertEqual([], records["Water"]["t0_forms"])
        self.assertIn("SiliconDioxide", records)

        runtime = {
            Path(filename).stem: json.loads(
                (MODULE.MATERIAL_DIR / filename).read_text(encoding="utf-8"))
            for filename in json.loads(
                (MODULE.MATERIAL_DIR / "index.json").read_text(encoding="utf-8"))
        }
        self.assertTrue(runtime["oxygen"]["metadata_only"])
        self.assertTrue(runtime["water"]["metadata_only"])
        self.assertEqual(
            {"oxygen": 2, "silicon": 1},
            runtime["silicon_dioxide"]["composition"])

    def test_transitive_provenance_never_uses_self_parent(self):
        active = [
            record for record in self.policy["records"]
            if record["status"] == "ACTIVE"
        ]
        transitive = [
            record for record in active
            if record["activation_provenance"]["kind"].startswith("transitive_")
        ]
        self.assertTrue(transitive)
        self.assertTrue(all(
            record["activation_provenance"]["parent"] != record["source_name"]
            for record in transitive))
        adamantine = next(
            record for record in active if record["source_name"] == "Adamantine")
        self.assertNotIn("required by Adamantine", adamantine["reason"])
        if adamantine["activation_provenance"]["kind"].startswith("transitive_"):
            self.assertNotEqual(
                "Adamantine", adamantine["activation_provenance"]["parent"])

    def test_policy_forms_match_every_indexed_runtime_definition(self):
        policy = {
            record["cc_id"]: record
            for record in self.policy["records"] if record["status"] == "ACTIVE"
        }
        index = json.loads(
            (MODULE.MATERIAL_DIR / "index.json").read_text(encoding="utf-8"))
        self.assertEqual(set(policy), {Path(filename).stem for filename in index})
        l3_document = json.loads(
            MODULE.gt6_l3_materials.OUT.read_text(encoding="utf-8")
        )
        for filename in index:
            runtime = json.loads(
                (MODULE.MATERIAL_DIR / filename).read_text(encoding="utf-8"))
            forms = MODULE.gt6_l3_materials.resolve_material_forms(
                runtime,
                l3_document,
            )
            record = policy[runtime["id"]]
            self.assertEqual(
                forms,
                MODULE.gt6_l3_materials.close_implied_prefixes(
                    record["t0_forms"]
                ),
                runtime["id"],
            )
            self.assertEqual(not forms, record["metadata_only"], runtime["id"])
            if not forms:
                self.assertIn("metadata_only=true", record["reason"])
        alumite = next(
            record for record in policy.values()
            if record["source_name"] == "Alumite")
        self.assertIn("ingot", alumite["t0_forms"])
        self.assertIn("dust", alumite["t0_forms"])
        self.assertFalse(alumite["metadata_only"])
        alumite_runtime = json.loads(
            (MODULE.MATERIAL_DIR / "alumite.json").read_text(encoding="utf-8")
        )
        self.assertTrue(alumite_runtime["no_decompose"])
        iron = policy["iron"]
        self.assertEqual(
            ["wire"],
            iron["acceptance_form_corrections"][0]["add_forms"],
        )
        self.assertEqual(
            "t3_acceptance_required_not_gt6_original_gate",
            iron["acceptance_form_corrections"][0]["classification"],
        )
        self.assertNotIn("wire", iron["gt6_original_t0_forms"])
        self.assertIn("wire", iron["t0_forms"])

    def test_metadata_only_materials_have_no_mappable_gt6_prefix(self):
        active = {
            record["cc_id"]: record
            for record in self.policy["records"]
            if record["status"] == "ACTIVE"
        }
        runtime = {
            Path(filename).stem: json.loads(
                (MODULE.MATERIAL_DIR / filename).read_text(encoding="utf-8")
            )
            for filename in json.loads(
                (MODULE.MATERIAL_DIR / "index.json").read_text(encoding="utf-8")
            )
        }
        metadata_only = {
            material_id: value
            for material_id, value in runtime.items()
            if value.get("metadata_only")
        }
        self.assertEqual(663, len(metadata_only))
        self.assertEqual(1110, len(runtime) - len(metadata_only))
        self.assertEqual(
            set(metadata_only),
            {
                material_id
                for material_id, record in active.items()
                if record["metadata_only"]
            },
        )
        self.assertTrue(all(
            not active[material_id]["available_t0_forms"]
            for material_id in metadata_only
        ))
        self.assertFalse(runtime["adamantine"].get("metadata_only", False))
        self.assertTrue(runtime["adamantine"]["generation_flags"])

    def test_direct_gt6_flags_exactly_match_active_source_tag_domains(self):
        index = json.loads(
            (MODULE.MATERIAL_DIR / "index.json").read_text(encoding="utf-8")
        )
        runtime = [
            json.loads(
                (MODULE.MATERIAL_DIR / filename).read_text(encoding="utf-8")
            )
            for filename in index
        ]
        plans = json.loads(
            MODULE.gt6_l3_materials.OUT.read_text(encoding="utf-8")
        )["prefixes"]
        expected_by_flag = {}
        for plan in plans.values():
            flag = plan["generation_flag"]
            if not flag.startswith("gt6:itemgenerator/"):
                continue
            expected_by_flag.setdefault(flag, set()).update(
                plan["base_materials"]
            )
        active_sources = {
            value["gt6_metadata"]["source_name"]
            for value in runtime
        }
        for flag, expected in expected_by_flag.items():
            with self.subTest(flag=flag):
                actual = {
                    value["gt6_metadata"]["source_name"]
                    for value in runtime
                    if flag in value["generation_flags"]
                }
                self.assertEqual(expected & active_sources, actual)

    def test_component_forms_are_fixed_point_closed_and_cables_need_rubber(self):
        active = [
            record for record in self.policy["records"]
            if record["status"] == "ACTIVE"
        ]
        retained = sum(
            len(set(record["t0_forms"]) & MODULE.T3_COMPONENT_FORMS)
            for record in active)
        validation = self.policy["reference_validation"]
        self.assertEqual(retained, validation["retained_component_forms"])
        self.assertEqual(0, validation["unproduced_component_items"])
        cable_forms = {
            "cable", "double_cable", "quadruple_cable",
            "octuple_cable", "dodecuple_cable",
        }
        with_cables = [
            record for record in active
            if set(record["t0_forms"]) & cable_forms
        ]
        counts = {
            form: sum(form in record["t0_forms"] for record in active)
            for form in cable_forms
        }
        self.assertEqual({
            "cable": 31,
            "double_cable": 28,
            "quadruple_cable": 28,
            "octuple_cable": 28,
            "dodecuple_cable": 28,
        }, counts)
        rubber = next(record for record in active
                      if record["source_name"] == "Rubber")
        self.assertEqual(["plate"], rubber["t0_forms"])
        self.assertIn("insulation form include", rubber["reason"])
        self.assertEqual(143, sum(counts.values()))
        self.assertEqual(143, sum(
            len(set(record["t0_forms"]) & cable_forms)
            for record in with_cables))
        self.assertEqual(0, validation["pruned_unproducible_component_forms"])

    def test_ambient_phase_uses_kelvin_boundaries(self):
        by_name = {record["source_name"]: record for record in self.materials}
        for name in ("Copper", "Bronze", "Iron"):
            self.assertEqual("solid", by_name[name]["state"])
        self.assertEqual("liquid", by_name["Water"]["state"])
        self.assertEqual("gas", by_name["Oxygen"]["state"])

    def test_generated_compositions_are_reduced_and_references_resolved(self):
        index = json.loads((MODULE.MATERIAL_DIR / "index.json").read_text(encoding="utf-8"))
        generated = [
            json.loads((MODULE.MATERIAL_DIR / filename).read_text(encoding="utf-8"))
            for filename in index if Path(filename).stem not in MODULE.AUTHORED_IDS
        ]
        compositions = [value for value in generated if value.get("composition")]
        self.assertTrue(compositions)
        for value in compositions:
            ratios = list(value["composition"].values())
            self.assertTrue(all(isinstance(ratio, int) and ratio > 0 for ratio in ratios))
            self.assertEqual(1, __import__("functools").reduce(__import__("math").gcd, ratios))
        for value in generated:
            metadata = value["gt6_metadata"]
            self.assertTrue(all(
                set(reference) == {"material", "source_id", "source_name"}
                for reference in metadata["byproducts"]))
            self.assertTrue(all(
                target["material"] in {Path(filename).stem for filename in index}
                for target in metadata["processing_targets"].values()))
        self.assertTrue(any(
            target["numerator_u"] == 0 and target.get("cc_units") == 0
            for value in generated
            for target in value["gt6_metadata"]["processing_targets"].values()))

    def test_authored_metadata_keeps_every_resolved_reference(self):
        normalized = {record["source_name"]: record for record in self.materials}
        for cc_id, source_name in MODULE.AUTHORED_IDS.items():
            runtime = json.loads(
                (MODULE.MATERIAL_DIR / f"{cc_id}.json").read_text(encoding="utf-8"))
            metadata = runtime["gt6_metadata"]
            self.assertEqual(
                normalized[source_name]["byproducts"],
                [entry["source_name"] for entry in metadata["byproducts"]],
                cc_id,
            )
            self.assertEqual(
                set(normalized[source_name]["processing_targets"]),
                set(metadata["processing_targets"]),
                cc_id,
            )
        iron = json.loads(
            (MODULE.MATERIAL_DIR / "iron.json").read_text(encoding="utf-8"))
        self.assertEqual(
            "Hematite",
            iron["gt6_metadata"]["processing_targets"]["crushing"][
                "source_material_name"])

    def test_composition_cycle_is_rejected(self):
        base = {
            "source_id": 1,
            "aliases": [],
            "processing_targets": {},
            "byproducts": [],
        }
        a = {**base, "source_name": "A", "components": [
            {"material": "B", "material_id": 2}]}
        b = {**base, "source_id": 2, "source_name": "B", "components": [
            {"material": "A", "material_id": 1}]}
        with self.assertRaisesRegex(MODULE.ImportError, "composition cycle"):
            MODULE.validate_references([a, b])


if __name__ == "__main__":
    unittest.main()
