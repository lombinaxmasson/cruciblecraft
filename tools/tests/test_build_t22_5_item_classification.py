"""Tests for the T22.5 A3 unmapped-meta-item classification builder."""
from __future__ import annotations

import copy
import json
import sys
import unittest
from pathlib import Path

TOOLS = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(TOOLS))

import build_t22_5_item_classification as builder  # noqa: E402


class T225ItemClassificationTest(unittest.TestCase):

    def setUp(self) -> None:
        self.rules = builder._load(builder.RULES)
        self.items = builder._load(builder.OPERANDS)[
            "unmapped_gt_meta_items"
        ]

    def test_all_115481_occurrences_classified_once(self) -> None:
        classification = builder.classify(self.rules, self.items)
        counts = classification["counts"]
        self.assertEqual(counts["total_keys"], 49)
        self.assertEqual(counts["total_occurrences"], 115481)
        self.assertEqual(counts["unclassified"], 0)
        self.assertEqual(
            sum(counts["class_totals"].values()), 115481
        )
        # Every record is unique and carries its class.
        rows = classification["records"]
        self.assertEqual(len({r["item"] for r in rows}), 49)
        for row in rows:
            self.assertEqual(
                row["occurrences"], self.items[row["item"]]
            )

    def test_class_totals_match_known_families(self) -> None:
        classification = builder.classify(self.rules, self.items)
        totals = classification["counts"]["class_totals"]
        self.assertEqual(totals["equivalence_crushed_ore"], 8639 + 4774)
        self.assertEqual(
            totals["equivalence_block_variant"],
            self.items["gregtech:gt.meta.storage.ingot"],
        )
        self.assertEqual(
            totals["equivalence_dense_plate"],
            self.items["gregtech:gt.meta.storage.plate"],
        )

    def test_mutation_removing_a_discriminant_goes_red(self) -> None:
        mutated = copy.deepcopy(self.rules)
        mutated["classes"] = [
            c for c in mutated["classes"]
            if c["class"] != "out_of_scope_packaging_form"
        ]
        with self.assertRaises(ValueError):
            builder.classify(mutated, self.items)

    def test_mutation_reassigning_a_key_goes_red(self) -> None:
        mutated = copy.deepcopy(self.rules)
        mutated["keys"]["gregtech:gt.meta.crate.64.dust"] = (
            "equivalence_crushed_ore"
        )
        with self.assertRaises(ValueError):
            builder.classify(mutated, self.items)

    def test_reason_is_class_definition_never_translation(self) -> None:
        for entry in self.rules["classes"]:
            self.assertNotIn("translate", entry["definition"].lower())
            self.assertIn("recheck_point", entry)
            self.assertTrue(entry["recheck_point"])

    def test_committed_artifact_is_current(self) -> None:
        if not builder.OUTPUT.is_file():
            self.skipTest("artifact not yet generated")
        self.assertEqual(builder.check(), [])

    def test_committed_artifact_block(self) -> None:
        if not builder.OUTPUT.is_file():
            self.skipTest("artifact not yet generated")
        document = json.loads(
            builder.OUTPUT.read_text(encoding="utf-8")
        )
        self.assertEqual(
            document["status"], "T22_5_ITEM_CLASSIFICATION_READY"
        )
        self.assertEqual(document["counts"]["unclassified"], 0)
        self.assertEqual(document["counts"]["total_occurrences"], 115481)


if __name__ == "__main__":
    unittest.main()
