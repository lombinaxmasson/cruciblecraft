from __future__ import annotations

import json
import shutil
import tempfile
import unittest
from collections import Counter
from pathlib import Path

from tools import build_component_rules as builder


class ComponentRuleBuilderTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.bundle = builder.build_bundle()
        cls.manifest = json.loads(cls.bundle.manifest)

    def test_manifest_locks_baseline_budget_maps_and_classifications(self) -> None:
        bundle = self.bundle
        manifest = self.manifest

        self.assertEqual(51, manifest["source_rules"])
        self.assertEqual(51, manifest["authored_datapack_entries"])
        self.assertEqual(
            manifest["expanded_recipes"],
            manifest["authored_projected_recipes"],
        )
        self.assertTrue(manifest["expansion_is_authored_projection"])
        self.assertEqual(20, manifest["extruder_authored_entries"])
        self.assertEqual(2782, manifest["extruder_logical_relations"])
        self.assertEqual(2782, manifest["extruder_runtime_publication"])
        self.assertEqual(10000, manifest["expansion_budget"])
        self.assertLessEqual(
            manifest["expanded_recipes"],
            manifest["expansion_budget"],
        )
        self.assertTrue(manifest["within_budget"])
        self.assertEqual(144, manifest["unit_conservation"]["unit_scale"])
        self.assertTrue(manifest["per_map"])
        correction = manifest["acceptance_form_corrections"]
        self.assertEqual(2, correction["expansion_delta"])
        self.assertEqual(
            "acceptance_required_not_gt6_original_gate",
            correction["classification"],
        )
        self.assertEqual("iron", correction["entries"][0]["material"])
        self.assertEqual(["wire"], correction["entries"][0]["add_forms"])
        electrical = manifest["electrical_form_expansion"]
        self.assertEqual(5, electrical["expansion_delta"])
        self.assertEqual(
            [
                "blue_alloy",
                "electrotine_alloy",
                "hslasteel",
                "naquadah",
                "yttrium_barium_cuprate",
            ],
            [entry["material"] for entry in electrical["entries"]],
        )
        self.assertTrue(all(
            entry["rule"] == "wiremill/ingot_to_wire"
            for entry in electrical["entries"]
        ))
        extruder = manifest["extruder_templates"]
        self.assertEqual(62, extruder["classified"])
        self.assertEqual(0, extruder["unclassified"])
        self.assertEqual({"playable": 20, "skipped": 42},
                         extruder["classification_counts"])
        self.assertGreater(extruder["prefix_count"], 0)
        self.assertEqual(
            "tools/extruder_compact.json",
            extruder["compact_source"],
        )
        self.assertEqual(len(bundle.generated), manifest["generated_tree"]["files"])
        self.assertEqual(
            ["rubber"],
            manifest["material_groups"]["any_rubber"][
                "member_materials"
            ],
        )
        self.assertEqual(
            {
                "replace": False,
                "values": ["#c:plates/rubber"],
            },
            json.loads(bundle.any_rubber_tag),
        )
        self.assertEqual(0, manifest["shadow_signatures"]["duplicates"])
        self.assertEqual(
            manifest["expanded_recipes"],
            manifest["shadow_signatures"]["unique"],
        )
        print(
            "COMPONENT_RULE_AUTHORED_EXPANDED="
            f"{manifest['expanded_recipes']}",
            flush=True,
        )

    def test_generated_rules_are_runtime_material_rule_json(self) -> None:
        bundle = self.bundle

        self.assertEqual(51, len(bundle.generated))
        self.assertEqual(sorted(bundle.generated), list(bundle.generated))
        for relative, content in bundle.generated.items():
            document = json.loads(content)
            self.assertEqual("cruciblecraft:material_rule", document["type"], relative)
            self.assertEqual(
                f"cruciblecraft:{relative.split('/', 1)[0]}",
                document["target"],
                relative,
            )
            if relative.startswith("extruder/compact/"):
                self.assertNotIn("item_inputs", document)
                self.assertNotIn("item_outputs", document)
                self.assertTrue(document["sparse"]["relations"], relative)
            else:
                self.assertTrue(document["item_inputs"], relative)
                self.assertTrue(document["item_outputs"], relative)
                self.assertGreater(int(document["eut"]), 0, relative)
        extruder = [
            json.loads(content)
            for relative, content in bundle.generated.items()
            if relative.startswith("extruder/")
        ]
        self.assertEqual(20, len(extruder))
        self.assertTrue(all(
            rule["sparse"]["shape_item"].startswith(
                "cruciblecraft:extruder_shape_")
            for rule in extruder
        ))
        self.assertEqual(
            2782,
            sum(len(rule["sparse"]["relations"]) for rule in extruder),
        )
        cable_rules = [
            json.loads(content)
            for relative, content in bundle.generated.items()
            if "_rubber_to_" in relative and "cable" in relative
        ]
        self.assertEqual(5, len(cable_rules))
        self.assertTrue(all(
            rule["item_inputs"][1]["tag"]
            == "cruciblecraft:any_rubber_plates"
            for rule in cable_rules
        ))
        rotor = json.loads(
            bundle.generated["assembler/plates_and_ring_to_rotor.json"]
        )
        self.assertEqual(
            [
                (
                    "has_registered_for("
                    '"processing_target:working", rotor)'
                )
            ],
            rotor["conditions"],
        )

    def test_acceptance_materials_have_explicit_rod_routes(self) -> None:
        bundle = self.bundle
        routes = []
        for relative, content in bundle.generated.items():
            if not relative.startswith("extruder/"):
                continue
            document = json.loads(content)
            for relation in document["sparse"]["relations"]:
                output = relation["output"]["prefix"]
                if output in {"rod", "long_rod"}:
                    routes.append(relation["material"])
        self.assertTrue({"copper", "tin", "iron", "gold"} <= set(routes))

    def test_iron_wire_is_an_explicit_acceptance_correction(self) -> None:
        bundle = self.bundle
        manifest = self.manifest
        correction = manifest["acceptance_form_corrections"]["entries"]

        self.assertEqual(1, len(correction))
        self.assertEqual(
            {
                "material": "iron",
                "add_forms": ["wire"],
                "affects_rules": [
                    "wiremill/ingot_to_wire",
                    "wiremill/wire_to_double_wire",
                ],
                "expected_expansion_delta": 2,
                "classification": (
                    "acceptance_required_not_gt6_original_gate"
                ),
                "reason": (
                    "The component-runtime acceptance matrix requires iron "
                    "ingot to produce iron wire through the ordinary wiremill "
                    "material rule."
                ),
            },
            correction[0],
        )
        rule = json.loads(bundle.generated["wiremill/ingot_to_wire.json"])
        self.assertNotIn("material", rule)
        self.assertEqual(
            "cruciblecraft:ingot", rule["item_inputs"][0]["prefix"]
        )
        self.assertEqual(
            "cruciblecraft:wire", rule["item_outputs"][0]["prefix"]
        )

    def test_budget_failure_happens_before_any_write(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            source = Path(temporary) / "sources"
            shutil.copytree(builder.SOURCE_DIR, source)
            path = source / "component_baseline.json"
            document = json.loads(path.read_text(encoding="utf-8"))
            document["expansion_budget"] = document["expanded_recipes"]
            path.write_text(json.dumps(document), encoding="utf-8")
            with self.assertRaisesRegex(builder.SourceError, "exceeds budget"):
                builder.build_bundle(source)
            self.assertFalse((Path(temporary) / "resources").exists())

    def test_extruder_relation_must_pass_material_registration_gate(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            source = Path(temporary) / "sources"
            shutil.copytree(builder.SOURCE_DIR, source)
            path = source / "extruder_shapes.json"
            document = json.loads(path.read_text(encoding="utf-8-sig"))
            document["recipes"][0]["output"]["prefix"] = "dodecuple_cable"
            path.write_text(json.dumps(document), encoding="utf-8")
            with self.assertRaisesRegex(
                    builder.SourceError, "form is absent from registration gate"):
                builder.build_bundle(source)

    def test_extruder_eut_is_locked_to_the_material_tag(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            source = Path(temporary) / "sources"
            shutil.copytree(builder.SOURCE_DIR, source)
            path = source / "extruder_shapes.json"
            document = json.loads(path.read_text(encoding="utf-8-sig"))
            document["recipes"][0]["eut"] = 17
            path.write_text(json.dumps(document), encoding="utf-8")
            with self.assertRaisesRegex(
                    builder.SourceError,
                    "PROCESSING.EXTRUDABLE_SIMPLE function"):
                builder.build_bundle(source)

    def test_plate_relation_explains_the_apparent_ten_material_mismatch(self) -> None:
        source = json.loads(
            (builder.SOURCE_DIR / "extruder_shapes.json").read_text(
                encoding="utf-8-sig"
            )
        )
        gate = json.loads(
            builder.MATERIAL_REGISTRATION_GATE.read_text(encoding="utf-8")
        )["materials"]
        plate = [
            recipe
            for recipe in source["recipes"]
            if recipe["shape"] == "plate"
        ]
        self.assertEqual(387, len(plate))
        self.assertEqual(
            {"dust": 80, "ingot": 307},
            dict(Counter(
                recipe["input"]["prefix"] for recipe in plate
            )),
        )
        alleged_dead = {
            "annealed_copper",
            "cast_iron",
            "iron_compressed",
            "tungsten_sintered",
            "wrought_iron",
        }
        by_material = {recipe["material"]: recipe for recipe in plate}
        for material in alleged_dead:
            self.assertEqual("ingot", by_material[material]["input"]["prefix"])
            self.assertIn("ingot", gate[material])
            self.assertIn("plate", gate[material])

        redirected_forging = {
            "frozen_iron": "iron",
            "gilded_iron": "iron",
            "obsidian_refined": "obsidian",
            "steel_galvanized": "steel",
        }
        for material, target in redirected_forging.items():
            document = json.loads(
                (builder.MATERIAL_DIR / f"{material}.json").read_text(
                    encoding="utf-8"
                )
            )
            forging = document["gt6_metadata"]["processing_targets"]["forging"]
            self.assertEqual(target, forging["material"])
            self.assertNotIn(material, by_material)

        glass = json.loads(
            (builder.MATERIAL_DIR / "glass.json").read_text(encoding="utf-8")
        )
        self.assertNotIn("plate_gem", gate["glass"])
        self.assertIn(
            "cruciblecraft:generates_plate_gem",
            glass["generation_flags"],
        )
        self.assertNotIn("glass", by_material)

    def test_check_is_bidirectional_for_missing_extra_and_changed_files(self) -> None:
        bundle = self.bundle
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            output = root / "resources"
            manifest = root / "manifest.json"
            builder.write_bundle(bundle, output, manifest)
            self.assertEqual([], builder.check_bundle(bundle, output, manifest))

            victim = output / next(iter(bundle.generated))
            original = victim.read_bytes()
            victim.write_text("{}\n", encoding="utf-8")
            self.assertTrue(any(
                "content drift" in error
                for error in builder.check_bundle(bundle, output, manifest)
            ))
            victim.write_bytes(original)

            victim.unlink()
            self.assertTrue(any(
                "missing generated recipe" in error
                for error in builder.check_bundle(bundle, output, manifest)
            ))
            victim.parent.mkdir(parents=True, exist_ok=True)
            victim.write_bytes(original)

            extra = output / "extra/unowned.json"
            extra.parent.mkdir(parents=True)
            extra.write_text("{}\n", encoding="utf-8")
            self.assertTrue(any(
                "unexpected generated recipe" in error
                for error in builder.check_bundle(bundle, output, manifest)
            ))

            extra.unlink()
            manifest.write_text("{}\n", encoding="utf-8")
            self.assertTrue(any(
                "manifest content drift" in error
                for error in builder.check_bundle(bundle, output, manifest)
            ))

    def test_source_file_set_rejects_missing_and_extra_json(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            source = Path(temporary) / "sources"
            shutil.copytree(builder.SOURCE_DIR, source)
            (source / "component_baseline.json").unlink()
            with self.assertRaisesRegex(builder.SourceError, "source file set drift"):
                builder.build_bundle(source)

            shutil.copy(
                builder.SOURCE_DIR / "component_baseline.json",
                source / "component_baseline.json",
            )
            (source / "unexpected.json").write_text("{}\n", encoding="utf-8")
            with self.assertRaisesRegex(builder.SourceError, "source file set drift"):
                builder.build_bundle(source)

    def test_source_schema_rejects_unknown_fields_and_unit_drift(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            source = Path(temporary) / "sources"
            shutil.copytree(builder.SOURCE_DIR, source)
            path = source / "component_rules.json"
            document = json.loads(path.read_text(encoding="utf-8"))
            document["rules"][0]["invented"] = True
            path.write_text(json.dumps(document), encoding="utf-8")
            with self.assertRaisesRegex(builder.SourceError, "unknown keys"):
                builder.build_bundle(source)

            shutil.copy(builder.SOURCE_DIR / "component_rules.json", path)
            document = json.loads(path.read_text(encoding="utf-8"))
            transform = next(
                rule for rule in document["rules"]
                if rule["kind"] == "target_transform"
            )
            transform["source_units"] += 1
            path.write_text(json.dumps(document), encoding="utf-8")
            with self.assertRaisesRegex(builder.SourceError, "input units"):
                builder.build_bundle(source)

    def test_repository_outputs_are_byte_exact(self) -> None:
        self.assertEqual([], builder.check_bundle(self.bundle))


if __name__ == "__main__":
    unittest.main()
