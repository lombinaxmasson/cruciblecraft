"""Tests for the T22.5 C1 four-column denominator recompute builder."""
from __future__ import annotations

import json
import sys
import unittest
from pathlib import Path

TOOLS = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(TOOLS))

import build_t22_5_denominator_recompute as builder  # noqa: E402


class T225DenominatorRecomputeTest(unittest.TestCase):

    def test_committed_artifact_is_current(self) -> None:
        if not builder.OUTPUT.is_file():
            self.skipTest("artifact not yet generated")
        self.assertEqual(builder.check(), [])

    def test_four_columns_sum_to_the_universe(self) -> None:
        document = json.loads(
            builder.OUTPUT.read_text(encoding="utf-8")
        )
        columns = document["columns"]
        self.assertEqual(sum(columns.values()), 146841)
        self.assertEqual(columns["v1"], 0)
        self.assertEqual(
            columns["post_1_0_portfolio"]
            + columns["out_of_scope"]
            + columns["unlockable"],
            146841,
        )

    def test_independent_paths_agree(self) -> None:
        document = json.loads(
            builder.OUTPUT.read_text(encoding="utf-8")
        )
        paths = document["independent_paths"]
        self.assertTrue(paths["equal"])
        self.assertEqual(paths["ledger_2_diagnostic"], 146841)
        self.assertEqual(paths["b1_classification_total"], 146841)

    def test_ceiling_is_not_adjusted(self) -> None:
        document = json.loads(
            builder.OUTPUT.read_text(encoding="utf-8")
        )
        ceiling = document["ceiling"]
        self.assertEqual(ceiling["adjustment"], "not required")
        self.assertEqual(ceiling["hard_ceiling"], 21000)
        self.assertGreater(ceiling["headroom"], 0)
        self.assertEqual(ceiling["v1_incremental_unpublished"], 0)
        self.assertIn("T14", ceiling["trigger"])

    def test_unlockable_is_within_scanned(self) -> None:
        document = json.loads(
            builder.OUTPUT.read_text(encoding="utf-8")
        )
        scanned = document["unlockable_scan"]["scanned_rows"]
        self.assertGreaterEqual(scanned, document["columns"]["unlockable"])


if __name__ == "__main__":
    unittest.main()
