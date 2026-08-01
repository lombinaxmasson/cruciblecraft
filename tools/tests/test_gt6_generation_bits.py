from __future__ import annotations

import json
import unittest

from tools import build_gt6_generation_bits as generation


class GT6GenerationBitsTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.materials = json.loads(
            generation.MATERIALS_PATH.read_text(encoding="utf-8")
        )
        cls.prefixes = json.loads(
            generation.PREFIXES_PATH.read_text(encoding="utf-8")
        )
        cls.fluid_map = json.loads(
            generation.FLUID_MAP_PATH.read_text(encoding="utf-8")
        )
        cls.document = generation.build_document(
            cls.materials,
            cls.prefixes,
            cls.fluid_map,
        )

    def test_pinned_artifact_matches_recomputed_mapping(self) -> None:
        artifact = json.loads(generation.OUT.read_text(encoding="utf-8"))
        self.assertEqual(self.document, artifact)
        self.assertEqual(3, artifact["schema_version"])
        self.assertEqual(
            13,
            artifact["verification"]["mapped_tag_count"],
        )
        self.assertEqual(
            70,
            artifact["verification"]["mapped_prefix_count"],
        )

    def test_every_mapping_is_exact_on_stable_name_domain(self) -> None:
        stable_names = {
            row["nameInternal"]
            for row in self.materials
            if isinstance(row.get("id"), int) and row["id"] >= 0
        }
        by_name = {
            row["nameInternal"]: row for row in self.materials
        }
        prefix_sets = {
            row["nameInternal"]: (
                set(row.get("registeredMaterials") or []) & stable_names
            )
            for row in self.prefixes
        }
        for tag, prefixes in self.document[
            "itemgenerator_to_prefixes"
        ].items():
            tag_members = {
                name
                for name in stable_names
                if tag in (by_name[name].get("tags") or [])
            }
            for prefix in prefixes:
                self.assertEqual(
                    tag_members,
                    prefix_sets[prefix],
                    f"{tag} != {prefix}",
                )

    def test_all_full_domain_disagreements_are_sentinel_only(self) -> None:
        verification = self.document["verification"]
        self.assertEqual(441, verification["sentinel_record_count"])
        self.assertTrue(
            verification[
                "all_noncanonical_differences_are_sentinel_names"
            ]
        )
        self.assertEqual(1, verification["full_name_exact_tag_count"])
        self.assertEqual(
            {"ITEMGENERATOR.EMPTY"},
            {
                row["tag"]
                for row in verification["records"]
                if row["full_name_sets_equal"]
            },
        )

    def test_every_accepted_prefix_rule_replays_exactly(self) -> None:
        stable_names = {
            row["nameInternal"]
            for row in self.materials
            if isinstance(row.get("id"), int) and row["id"] >= 0
        }
        by_name = {
            row["nameInternal"]: row for row in self.materials
        }
        tag_sets = {
            tag: frozenset(
                name
                for name in stable_names
                if tag in (by_name[name].get("tags") or [])
            )
            for tag in {
                tag
                for name in stable_names
                for tag in (by_name[name].get("tags") or [])
            }
        }
        prefix_sets = {
            row["nameInternal"]: frozenset(
                set(row.get("registeredMaterials") or [])
                & stable_names
            )
            for row in self.prefixes
        }
        rules = self.document["prefix_generation_rules"]
        self.assertEqual(155, len(rules))
        for prefix, rule in rules.items():
            with self.subTest(prefix=prefix):
                self.assertIn(
                    rule["status"],
                    {"exact", "accepted_with_exceptions"},
                )
                self.assertLessEqual(
                    rule["expression_tag_count"],
                    generation.MAX_EXPRESSION_TAGS,
                )
                self.assertEqual(
                    prefix_sets[prefix],
                    generation.replay_rule(rule, tag_sets),
                )

    def test_core_prefix_rules_separate_accepted_from_diagnostics(self) -> None:
        verification = self.document["verification"]
        self.assertEqual(
            110,
            verification["exact_prefix_rule_count"],
        )
        self.assertEqual(
            45,
            verification["exception_prefix_rule_count"],
        )
        self.assertEqual([], verification["unresolved_core_prefixes"])
        self.assertEqual(
            {"block", "wire"},
            set(verification["compatibility_absorption_core_prefixes"]),
        )
        self.assertEqual(
            {"wireGt01"},
            set(verification["explicit_core_prefixes"]),
        )
        diagnostics = self.document["core_prefix_diagnostics"]
        self.assertEqual(
            2,
            diagnostics["ingot"]["exception_count"],
        )
        self.assertEqual(
            0,
            diagnostics["dust"]["exception_count"],
        )
        self.assertEqual(
            6,
            diagnostics["crushed"]["exception_count"],
        )
        self.assertEqual(
            "accepted_with_exceptions",
            diagnostics["plate"]["status"],
        )
        self.assertEqual(
            25,
            diagnostics["plate"]["exception_count"],
        )
        self.assertEqual(
            28,
            diagnostics["plate"]["exception_budget"],
        )
        self.assertEqual(
            "explicit_enumeration",
            diagnostics["wireGt01"]["status"],
        )
        self.assertEqual(
            32,
            diagnostics["wireGt01"]["target_material_count"],
        )

    def test_molten_tag_plus_exceptions_replays_fluid_domain(self) -> None:
        stable_names = {
            row["nameInternal"]
            for row in self.materials
            if isinstance(row.get("id"), int) and row["id"] >= 0
        }
        by_name = {
            row["nameInternal"]: row for row in self.materials
        }
        molten_tags = {
            f"{generation.TAG_PREFIX}MOLTEN",
            f"{generation.TAG_PREFIX}LIQUID",
        }
        tag_sets = {
            tag: frozenset(
                name
                for name in stable_names
                if tag in (by_name[name].get("tags") or [])
            )
            for tag in molten_tags
        }
        target = frozenset(
            value["material"]
            for fluid, value in self.fluid_map.items()
            if fluid.startswith("molten.")
            and value.get("material") in stable_names
            and value.get("materialId", -1) >= 0
        )
        rule = self.document["fluid_generation_rules"]["molten"]
        self.assertEqual(184, rule["base_material_count"])
        self.assertEqual(200, rule["target_material_count"])
        self.assertEqual(16, rule["exception_count"])
        self.assertEqual(
            48,
            rule["molten_or_liquid_trial"]["exception_count"],
        )
        self.assertEqual(32, rule["liquid_incremental_material_count"])
        self.assertEqual(2, rule["sentinel_fluid_record_count"])
        self.assertEqual(
            target,
            generation.replay_rule(rule, tag_sets),
        )


if __name__ == "__main__":
    unittest.main()
