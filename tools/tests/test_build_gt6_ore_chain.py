import json
import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_gt6_ore_chain as builder  # noqa: E402


class OreChainBuilderTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.index, cls.operands, cls.files = builder.load_committed_outputs()
        cls.materials = builder.compare.load_cc_materials()

    def test_committed_outputs_are_current_without_reference_rebuild(self):
        self.assertEqual([], builder.check_committed_outputs())

    def test_acceptance_materials_have_all_six_stages(self):
        emitted = {
            (row["material"], row["family"])
            for row in self.index["recipes"]
        }
        for material in builder.ACCEPTANCE_MATERIALS:
            for family in builder.STAGE_ORDER:
                self.assertIn((material, family), emitted)

    def test_tungsten_factual_ore_has_six_topology_fallback_stages(self):
        rows = [
            row
            for row in self.index["recipes"]
            if row["material"] == "tungsten"
            and row["family"] in builder.STAGE_ORDER
        ]
        self.assertEqual(set(builder.STAGE_ORDER), {
            row["family"] for row in rows
        })
        self.assertTrue(all(
            row["source"] == "data_driven_ore_topology_projection"
            for row in rows
        ))
        for row in rows:
            relative = row["path"].split("ore_chain/", 1)[1]
            document = json.loads(self.files[relative])
            self.assertEqual(
                "topology_fallback",
                document["provenance"]["source_kind"],
            )

    def test_processing_candidates_include_all_factual_ore_declarations(self):
        materials = self.materials
        factual_ores = {
            material_id
            for material_id, document in materials.items()
            if "ore" in document["_factual_forms"]
        }
        self.assertEqual(
            len(factual_ores),
            self.index["counts"]["factual_ore_materials"],
        )
        self.assertGreaterEqual(
            self.index["counts"]["processing_candidates"],
            len(factual_ores),
        )

    def test_recipe_ids_are_content_hashes_and_inputs_do_not_shadow(self):
        signatures = set()
        for row in self.index["recipes"]:
            relative = row["path"].split("ore_chain/", 1)[1]
            document = json.loads(self.files[relative])
            self.assertEqual(
                row["semantic_hash"],
                builder.value_hash({
                    key: value
                    for key, value in document.items()
                    if key != "provenance"
                }),
            )
            signature = builder.value_hash(
                {
                    "map": document["map"],
                    "item_inputs": document.get("item_inputs", []),
                    "item_input_counts": document.get(
                        "item_input_counts", []
                    ),
                    "fluid_inputs": document.get("fluid_inputs", []),
                },
                64,
            )
            key = (document["map"], signature)
            self.assertNotIn(key, signatures)
            signatures.add(key)

    def test_every_concrete_recipe_exposes_runtime_provenance(self):
        for row in self.index["recipes"]:
            relative = row["path"].split("ore_chain/", 1)[1]
            document = json.loads(self.files[relative])
            provenance = document["provenance"]
            self.assertIn(
                provenance["source_kind"],
                {
                    "gt6_evidence",
                    "topology_fallback",
                    builder.ORE_BLOCK_SOURCE_KIND,
                },
            )
            self.assertEqual(
                row["gt6_evidence_hashes"],
                provenance["evidence_hashes"],
            )
            if row["source"] == builder.ORE_BLOCK_SOURCE_KIND:
                self.assertIsNone(row["selected_source_recipe"])
                self.assertEqual(
                    row["derivation"]["runtime_source"],
                    provenance["selected_source_recipe"],
                )
                self.assertEqual([], provenance["evidence_hashes"])
            elif row["selected_source_recipe"]:
                self.assertEqual(
                    row["selected_source_recipe"],
                    provenance["selected_source_recipe"],
                )
            else:
                self.assertEqual(
                    f"topology_projection:{row['semantic_hash']}",
                    provenance["selected_source_recipe"],
                )
            self.assertEqual(
                row["source"]
                == "gt6_normalized_stage_evidence_projection",
                provenance["source_kind"] == "gt6_evidence",
            )

    def test_registered_ore_blocks_have_exact_derived_crusher_ingress(self):
        gate = builder.load(builder.REGISTRATION_GATE)
        materials = self.materials
        ore_materials = {
            material_id
            for material_id, forms in gate["materials"].items()
            if "ore" in forms
        }
        raw_rows = {
            row["material"]: row
            for row in self.index["recipes"]
            if row["family"] == "crush_raw_to_crushed"
        }
        ore_rows = {
            row["material"]: row
            for row in self.index["recipes"]
            if row["family"] == builder.ORE_BLOCK_ROUTE_FAMILY
        }
        self.assertEqual(137, len(ore_materials))
        self.assertEqual(357, len(raw_rows))
        self.assertEqual(ore_materials, set(ore_rows))
        self.assertEqual(ore_materials, ore_materials & set(raw_rows))
        self.assertEqual(set(), ore_materials - set(raw_rows))
        self.assertEqual(220, len(set(raw_rows) - ore_materials))
        self.assertEqual(
            137,
            self.index["counts"]["high_version_ore_block_recipes"],
        )
        self.assertEqual(494, self.index["counts"]["recipes_by_map"]["crusher"])
        self.assertEqual(2117, self.index["counts"]["recipes"])
        self.assertTrue(all(
            row.get("input_substituted") is True
            for row in raw_rows.values()
        ))
        self.assertEqual(
            357,
            sum("input_substituted" in row for row in self.index["recipes"]),
        )

        for material_id, row in ore_rows.items():
            baseline = raw_rows[material_id]
            baseline_document = json.loads(
                self.files[
                    baseline["path"].split("ore_chain/", 1)[1]
                ]
            )
            document = json.loads(
                self.files[row["path"].split("ore_chain/", 1)[1]]
            )
            tag_name = materials[material_id].get("tag_name") or material_id
            self.assertEqual(
                [{"tag": f"c:ores/{tag_name}"}],
                document["item_inputs"],
            )
            self.assertEqual([1], document["item_input_counts"])
            self.assertEqual(
                builder.ORE_BLOCK_OUTPUT_COUNT,
                document["item_outputs"][0]["count"],
            )
            self.assertEqual(
                baseline_document["item_outputs"][0]["id"],
                document["item_outputs"][0]["id"],
            )
            for field in ("duration", "eut", "output_chances"):
                self.assertEqual(
                    baseline_document[field],
                    document[field],
                    f"{material_id}: {field}",
                )
            self.assertEqual(
                builder.ORE_BLOCK_SOURCE_KIND,
                document["provenance"]["source_kind"],
            )
            self.assertEqual([], document["provenance"]["evidence_hashes"])
            self.assertEqual(
                baseline["semantic_hash"],
                row["derivation"]["base_recipe_semantic_hash"],
            )
            self.assertEqual(
                baseline["path"],
                row["derivation"]["base_recipe_path"],
            )
            self.assertEqual(
                {"numerator": 5, "denominator": 2},
                row["derivation"]["output_multiplier"],
            )

    def test_operand_projection_matches_recipe_index(self):
        self.assertEqual(
            self.index["counts"]["recipes"],
            self.operands["counts"]["recipes"],
        )
        indexed = {
            row["path"].split("ore_chain/", 1)[1].removesuffix(
                ".json"
            )
            for row in self.index["recipes"]
        }
        self.assertEqual(
            indexed,
            {row["recipe"] for row in self.operands["recipes"]},
        )

    def test_every_normalized_source_is_accounted_for(self):
        rows = self.index["source_accounting"]
        self.assertEqual(
            self.index["counts"]["normalized_sources"], len(rows)
        )
        self.assertEqual(
            {"emitted", "unsupported"},
            {row["status"] for row in rows},
        )
        for row in rows:
            if row["status"] == "emitted":
                self.assertIn("recipe_path", row)
            else:
                self.assertTrue(row["reason"])

    def test_nonfirst_stages_are_upstream_closed(self):
        stages = {
            family: {
                row["material"]
                for row in self.index["recipes"]
                if row["family"] == family
            }
            for family in builder.STAGE_ORDER
        }
        upstream = stages[builder.STAGE_ORDER[0]]
        for family in builder.STAGE_ORDER[1:]:
            self.assertLessEqual(stages[family], upstream)
            upstream = stages[family]
        for material in ("carbon", "clay", "steel"):
            self.assertTrue(all(
                material not in stages[family]
                for family in builder.STAGE_ORDER[1:]
            ))

    def test_direct_cross_material_smelting_targets_are_concrete(self):
        emitted = {
            row["material"]: row
            for row in self.index["recipes"]
            if row["family"] == "chain_smelter"
        }
        materials = self.materials
        eligible = set()
        for material, document in materials.items():
            target = (
                document.get("gt6_metadata", {})
                .get("processing_targets", {})
                .get("smelting")
            )
            if not isinstance(target, dict):
                continue
            target_id = target.get("material")
            units = target.get("cc_units")
            if (
                isinstance(target_id, str)
                and isinstance(units, int)
                and units > 0
                and target_id in materials
                and "ingot" in builder.compare.material_forms(
                    materials[target_id]
                )
            ):
                eligible.add(material)
        sifter_materials = {
            row["material"]
            for row in self.index["recipes"]
            if row["family"] == "chain_sifter"
        }
        self.assertLessEqual(eligible & sifter_materials, set(emitted))

        aquamarine = emitted["aquamarine"]
        relative = aquamarine["path"].split("ore_chain/", 1)[1]
        document = json.loads(self.files[relative])
        self.assertEqual([36], document["item_input_counts"])
        self.assertEqual(
            [{"count": 1, "id": "cruciblecraft:beryllium/ingot"}],
            document["item_outputs"],
        )
        self.assertEqual(
            "topology_fallback",
            document["provenance"]["source_kind"],
        )

    def test_source_parameters_and_stage_byproducts_are_not_global_templates(self):
        self.assertGreater(
            self.index["counts"]["gt6_evidenced_recipes"], 0
        )
        signatures = {}
        for family in (
            "chain_sluice",
            "chain_centrifuge",
            "chain_shredder",
        ):
            row = next(
                row
                for row in self.index["recipes"]
                if row["family"] == family
                and row["material"] == "copper"
            )
            relative = row["path"].split("ore_chain/", 1)[1]
            document = json.loads(self.files[relative])
            signatures[family] = (
                document["item_outputs"][1:],
                document["output_chances"][1:],
            )
        self.assertGreater(len(set(map(str, signatures.values()))), 1)
        by_material = {}
        for row in self.index["recipes"]:
            if row["family"] not in {
                "chain_sluice",
                "chain_centrifuge",
                "chain_shredder",
            }:
                continue
            relative = row["path"].split("ore_chain/", 1)[1]
            document = json.loads(self.files[relative])
            by_material.setdefault(row["material"], []).append((
                document["item_outputs"][1:],
                document["output_chances"][1:],
            ))
        duplicated = [
            material
            for material, stage_outputs in by_material.items()
            if any(outputs for outputs, _ in stage_outputs)
            and len(stage_outputs) == 3
            and len(set(map(str, stage_outputs))) == 1
        ]
        self.assertEqual([], duplicated)

    @unittest.skipUnless(
        builder.REFERENCE.is_file(),
        "full GT6 source replay cache is not installed",
    )
    def test_copper_stage_chances_are_pinned_to_selected_gt6_evidence(self):
        expected = {
            "chain_sluice": (
                "df9eede93e9972ef7067",
                [10_000, 300, 300, 300],
            ),
            "chain_shredder": (
                "fe6db051602d4e6833dd",
                [10_000],
            ),
        }
        reference = builder.load(builder.REFERENCE)
        materials = self.materials
        prefixes = builder.prefix_definitions()
        for family, (source_hash, chances) in expected.items():
            with self.subTest(family=family):
                row = next(
                    row
                    for row in self.index["recipes"]
                    if row["family"] == family
                    and row["material"] == "copper"
                )
                relative = row["path"].split("ore_chain/", 1)[1]
                document = json.loads(self.files[relative])
                self.assertEqual(source_hash, row["selected_source_recipe"])
                self.assertIn(source_hash, row["gt6_evidence_hashes"])
                self.assertEqual(
                    source_hash,
                    document["provenance"]["selected_source_recipe"],
                )
                self.assertEqual(
                    row["gt6_evidence_hashes"],
                    document["provenance"]["evidence_hashes"],
                )
                source = next(
                    source
                    for source in reference["families"][family]
                    if builder.value_hash(source) == source_hash
                )
                self.assertEqual("copper", source["material"])
                normalized = builder.reference_recipe(
                    reference,
                    family,
                    "copper",
                    "copper",
                    document["item_outputs"][0]["count"],
                    materials,
                )
                self.assertIsNotNone(normalized)
                self.assertEqual(f"gt6:{source_hash}", normalized.raw_hint)
                self.assertEqual(chances, list(normalized.chances))
                self.assertEqual(chances, document["output_chances"])
        centrifuge = next(
            row
            for row in self.index["recipes"]
            if row["family"] == "chain_centrifuge"
            and row["material"] == "copper"
        )
        self.assertIsNone(centrifuge["selected_source_recipe"])
        document = json.loads(
            self.files[centrifuge["path"].split("ore_chain/", 1)[1]]
        )
        self.assertEqual(
            "topology_fallback",
            document["provenance"]["source_kind"],
        )

    def test_t2_coverage_debts_are_committed_as_ledger(self):
        ledger = self.index["coverage_ledger"]
        self.assertEqual(
            {
                "copper",
                "gold",
                "iron",
                "lead",
                "nickel",
                "tin",
                "tungsten",
                "zinc",
            },
            set(ledger["worldgen_ore_materials"]),
        )
        self.assertTrue(ledger["crusher_without_worldgen"])
        self.assertTrue(ledger["sifter_dust_without_smelter"])
        self.assertTrue(ledger["incomplete_routes_from_crusher"])
        self.assertEqual(
            "retained compatibility route",
            ledger["furnace_shortcut_policy"]["decision"],
        )
        shortcuts = ledger["furnace_shortcut_policy"]
        self.assertEqual(
            "furnace shortcuts and the six-stage chain yield equal "
            "main-output material units",
            shortcuts["main_output_policy"],
        )
        self.assertEqual(
            "the six-stage chain's additional value is staged byproducts",
            shortcuts["byproduct_value"],
        )
        self.assertEqual(
            builder.COMPAT_SHORTCUT_GROUP,
            shortcuts["group"],
        )
        generated = (
            builder.ROOT
            / "src/generated/resources/data/cruciblecraft/recipe"
        )
        expected = {
            (form, process): {
                path.parent.name
                for path in generated.glob(
                    f"*/{form}_ore_{process}.json"
                )
            }
            for form in ("raw", "crushed")
            for process in ("smelting", "blasting")
        }
        self.assertEqual(
            len(expected[("raw", "smelting")]),
            shortcuts["raw_pairs"],
        )
        self.assertEqual(
            len(expected[("crushed", "smelting")]),
            shortcuts["crushed_pairs"],
        )
        self.assertEqual(
            sum(len(expected[(form, "smelting")]) for form in ("raw", "crushed")),
            shortcuts["smelting_files"],
        )
        self.assertEqual(
            sum(len(expected[(form, "blasting")]) for form in ("raw", "crushed")),
            shortcuts["blasting_files"],
        )
        self.assertEqual(
            shortcuts["total_files"],
            shortcuts["smelting_files"] + shortcuts["blasting_files"],
        )
        grouped = [
            path
            for form in ("raw", "crushed")
            for process in ("smelting", "blasting")
            for path in generated.glob(f"*/{form}_ore_{process}.json")
        ]
        self.assertEqual(shortcuts["total_files"], len(grouped))
        for path in grouped:
            with self.subTest(path=path):
                self.assertEqual(
                    builder.COMPAT_SHORTCUT_GROUP,
                    builder.load(path).get("group"),
                )


if __name__ == "__main__":
    unittest.main()
