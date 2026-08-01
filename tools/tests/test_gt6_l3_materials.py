from __future__ import annotations

import json
import unittest

from tools import gt6_l3_materials as l3


class GT6L3MaterialsTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.document = l3.extract()

    def test_pinned_plan_covers_every_bundled_prefix(self) -> None:
        artifact = json.loads(l3.OUT.read_text(encoding="utf-8"))
        self.assertEqual(self.document, artifact)
        self.assertEqual(43, artifact["verification"]["prefix_count"])
        self.assertEqual(
            16,
            artifact["verification"]["direct_tag_prefix_count"],
        )
        self.assertEqual(
            25,
            artifact["verification"]["composite_prefix_count"],
        )
        self.assertEqual(
            2,
            artifact["verification"]["shared_prefix_count"],
        )
        self.assertTrue(artifact["verification"]["catalog_coverage_verified"])

    def test_composite_and_direct_flags_follow_the_plan(self) -> None:
        plans = self.document["prefixes"]
        self.assertEqual(
            "cruciblecraft:generates_plate",
            plans["plate"]["generation_flag"],
        )
        self.assertEqual("composite", plans["plate"]["mode"])
        self.assertEqual(
            "gt6:itemgenerator/gems",
            plans["gem"]["generation_flag"],
        )
        self.assertEqual("direct_tag", plans["gem"]["mode"])
        self.assertEqual(
            ["blockGem", "blockIngot"],
            plans["block"]["source_prefixes"],
        )
        self.assertEqual(["ore"], plans["ore"]["source_prefixes"])
        self.assertEqual(135, plans["ore"]["base_material_count"])
        self.assertEqual(136, plans["ore"]["target_material_count"])
        self.assertEqual(["Iron"], plans["ore"]["include_materials"])
        self.assertEqual(
            "explicit_enumeration",
            json.loads(
                l3.GENERATION_BITS_PATH.read_text(encoding="utf-8")
            )["explicit_prefix_domains"]["wireGt01"]["status"],
        )

    def test_material_patches_disambiguate_shared_flags(self) -> None:
        encoded = l3.encode_material_forms(
            "Copper",
            ["tiny_dust"],
            self.document,
        )
        self.assertIn(
            "cruciblecraft:generates_dust",
            encoded["generation_flags"],
        )
        self.assertIn("dust", encoded["exclude_prefixes"])
        self.assertIn("small_dust", encoded["exclude_prefixes"])
        self.assertEqual(
            {"tiny_dust"},
            l3.resolve_material_forms(encoded, self.document),
        )

    def test_ore_implications_close_before_explicit_excludes(self) -> None:
        resolved = l3.resolve_material_forms(
            {
                "include_prefixes": ["ore"],
                "exclude_prefixes": ["washed_crushed_ore"],
            },
            self.document,
        )
        self.assertEqual(
            {
                "ore",
                "crushed_ore",
                "centrifuged_crushed_ore",
                "purified_dust",
            },
            resolved,
        )
        self.assertTrue(
            {"raw_ore", "dust", "ingot"}.isdisjoint(resolved)
        )

    def test_ore_implication_closure_is_loaded_from_prefix_schema(self) -> None:
        self.assertEqual(
            {
                "crushed_ore",
                "washed_crushed_ore",
                "centrifuged_crushed_ore",
                "purified_dust",
            },
            set(l3.prefix_implication_closures()["ore"]),
        )

    def test_material_encoder_preserves_ore_closure_without_manual_forms(
        self,
    ) -> None:
        encoded = l3.encode_material_forms(
            "Tungsten",
            ["ore", "raw_ore", "dust", "ingot"],
            self.document,
        )
        self.assertLessEqual(
            {
                "ore",
                "raw_ore",
                "crushed_ore",
                "washed_crushed_ore",
                "centrifuged_crushed_ore",
                "purified_dust",
                "dust",
                "ingot",
            },
            l3.resolve_material_forms(encoded, self.document),
        )

    def test_applied_structure_removes_legacy_forms_and_replays_exactly(self) -> None:
        material = {
            "id": "testium",
            "forms": ["dust"],
            "thermal": {"melting_point": 1000},
        }
        result = l3.apply_material_structure(
            material,
            "Testium",
            ["ingot", "dust", "plate", "block"],
            self.document,
        )
        self.assertNotIn("forms", result)
        self.assertIn("generation_flags", result)
        self.assertEqual(
            {"ingot", "dust", "plate", "block"},
            l3.resolve_material_forms(result, self.document),
        )

    def test_empty_forms_require_explicit_direct_evidence(self) -> None:
        material = {
            "id": "testium",
            "thermal": {"melting_point": 1000},
        }
        with self.assertRaisesRegex(ValueError, "no material forms"):
            l3.apply_material_structure(
                material,
                "Testium",
                [],
                self.document,
            )
        result = l3.apply_material_structure(
            material,
            "Testium",
            [],
            self.document,
            metadata_only=True,
        )
        self.assertTrue(result["metadata_only"])

    def test_prefix_definitions_use_the_planned_flags(self) -> None:
        for path, expected in l3.prefix_definition_outputs(
            self.document
        ).items():
            with self.subTest(path=path.name):
                self.assertEqual(
                    expected,
                    path.read_text(encoding="utf-8"),
                )


if __name__ == "__main__":
    unittest.main()
