#!/usr/bin/env python3
"""Contract tests for the compact runtime compatibility manifest."""
from __future__ import annotations

import json
import unittest
from pathlib import Path

from tools.recipe_bulk import runtime as runtime_mod
from tools.recipe_bulk.membership import membership_root
from tools.recipe_bulk.matrix import authored_relations

ROOT = Path(__file__).resolve().parents[2]
FIXTURES = ROOT / "src/test/resources/compact_recipe_runtime_fixtures"


class CompactRecipeRuntimeManifestTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.document = runtime_mod.build()
        cls.groups = {
            row["publication_group"]: row for row in cls.document["groups"]
        }

    def test_twelve_groups_and_four_dedup_rules(self) -> None:
        self.assertEqual("COMPACT_RECIPE_RUNTIME_MANIFEST", self.document["status"])
        self.assertEqual(0, self.document["owns_families"])
        self.assertEqual(12, self.document["group_count"])
        self.assertEqual(12, len(self.document["groups"]))
        self.assertEqual(4, len(self.document["dedup_rules"]))
        self.assertEqual(14, self.document["assembler_compact_eager_count"])
        self.assertEqual(36, self.document["assembler_compact_lazy_count"])
        self.assertEqual(292, self.document["assembler_wood_authored_relation_count"])
        self.assertEqual(242, self.document["assembler_wood_live_relation_count"])

    def test_assembler_compact_hybrid_is_fourteen_explicit_eager_ids(self) -> None:
        row = self.groups["cruciblecraft:assembler/compact"]
        self.assertEqual("hybrid", row["policy_type"])
        self.assertEqual(8, row["cache_ceiling"])
        self.assertEqual(14, len(row["eager_stable_ids"]))
        self.assertEqual(50, row["effective_relation_count"])
        self.assertTrue(
            all(
                value.startswith("cruciblecraft:assembler/compact/")
                for value in row["eager_stable_ids"]
            )
        )

    def test_assembler_wood_policy_membership_binds_live_counts(self) -> None:
        planks2 = self.groups["cruciblecraft:assembler/wood/planks2"]
        self.assertEqual(63, planks2["authored_relation_count"])
        self.assertEqual(13, planks2["effective_relation_count"])
        self.assertEqual(
            85,
            self.groups["cruciblecraft:assembler/wood/planks"][
                "effective_relation_count"
            ],
        )
        self.assertEqual(
            144,
            self.groups["cruciblecraft:assembler/wood/fireproof"][
                "effective_relation_count"
            ],
        )
        live = sum(
            self.groups[group_id]["effective_relation_count"]
            for group_id in (
                "cruciblecraft:assembler/wood/planks",
                "cruciblecraft:assembler/wood/fireproof",
                "cruciblecraft:assembler/wood/planks2",
            )
        )
        self.assertEqual(242, live)

    def test_membership_root_matches_java_line_algorithm(self) -> None:
        row = self.groups["cruciblecraft:roaster/compact"]
        families = [
            family
            for family in runtime_mod.load_wave_families("roaster/compact")
            if runtime_mod.resolved_publication_group(family)
            == "cruciblecraft:roaster/compact"
        ]
        family_ids = [str(family["family_id"]) for family in families]
        stable_ids = [
            str(relation["stable_id"])
            for family in families
            for relation in authored_relations(family)
        ]
        self.assertEqual(
            membership_root(family_ids, stable_ids),
            row["membership_root_sha256"],
        )

    def test_negative_unknown_mode_fails_closed(self) -> None:
        rules = json.loads(
            (FIXTURES / "unknown_match_mode.json").read_text(encoding="utf-8")
        )
        with self.assertRaises(ValueError) as raised:
            runtime_mod.validate_dedup_rules(rules)
        self.assertIn("match_mode", str(raised.exception))

    def test_negative_selector_overlap_fails_closed(self) -> None:
        rules = json.loads(
            (FIXTURES / "selector_overlap.json").read_text(encoding="utf-8")
        )
        with self.assertRaises(ValueError) as raised:
            runtime_mod.validate_dedup_rules(rules)
        self.assertIn("overlap", str(raised.exception))

    def test_negative_undeclared_owner_fails_closed(self) -> None:
        rules = json.loads(
            (FIXTURES / "undeclared_owner.json").read_text(encoding="utf-8")
        )
        with self.assertRaises(ValueError) as raised:
            runtime_mod.validate_dedup_rules(rules)
        self.assertIn("undeclared", str(raised.exception))

    def test_negative_duplicate_rule_id_fails_closed(self) -> None:
        rules = json.loads(
            (FIXTURES / "duplicate_rule_id.json").read_text(encoding="utf-8")
        )
        with self.assertRaises(ValueError) as raised:
            runtime_mod.validate_dedup_rules(rules)
        self.assertIn("duplicate", str(raised.exception).lower())

    def test_negative_membership_root_mismatch_fails_closed(self) -> None:
        fixture = json.loads(
            (FIXTURES / "membership_mismatch.json").read_text(encoding="utf-8")
        )
        live = membership_root(fixture["family_ids"], fixture["stable_ids"])
        self.assertNotEqual(fixture["membership_root_sha256"], live)
        row = self.groups["cruciblecraft:roaster/compact"]
        self.assertNotEqual(fixture["membership_root_sha256"], row["membership_root_sha256"])

    def test_nine_cutover_policies_and_four_dedup_resources_exist(self) -> None:
        documents = runtime_mod.cutover_policy_documents()
        self.assertEqual(9, len(documents))
        self.assertEqual(4, len(runtime_mod.dedup_rules()))


if __name__ == "__main__":
    unittest.main()
