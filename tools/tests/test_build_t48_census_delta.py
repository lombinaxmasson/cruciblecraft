"""Contract tests for the T48 census overlay."""
from __future__ import annotations

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_t48_census_delta as builder  # noqa: E402
from tools import t48_common as t48  # noqa: E402


class T48CensusDeltaTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        if not t48.PLAYER_PATH.is_file() or not t48.LAYERED_PLAYER_PATH.is_file():
            raise unittest.SkipTest("T48 player-path artifacts are not on disk yet")
        cls.document = builder.build()

    def test_t35_foundation_denominators_are_unchanged(self) -> None:
        self.assertEqual(t48.T35_FOUNDATION, self.document["t35_foundation"])
        self.assertTrue(self.document["t35_foundation_unchanged"])
        self.assertEqual(0, self.document["validators"]["t35_foundation_unchanged"])
        self.assertTrue(self.document["t47_history_readonly"])

    def test_exactly_production_locked_identities_are_overlayed(self) -> None:
        ids = [row["canonical_id"] for row in self.document["identities"]]
        self.assertEqual(t48.production_family_count(), len(ids))
        self.assertEqual(t48.production_family_ids(), ids)
        self.assertTrue(all(row["owner"] == t48.OWNER for row in self.document["identities"]))
        self.assertEqual(t48.OPENING_EXECUTION_GAP, self.document["opening_execution_gap"])
        self.assertEqual(0, self.document["partial_family_count"])
        self.assertEqual(0, self.document["reclassification_delta"])
        remaining = self.document["remaining_ordinary"]
        self.assertFalse(remaining["t35_files_rewritten"])
        self.assertEqual(
            t48.DEFERRED_RECYCLING_COUNT,
            remaining["deferred_recycling_count"],
        )
        self.assertEqual(
            t48.EXPECTED_REMAINING_ORDINARY_FAMILIES,
            remaining["expected_remaining_ordinary_families"],
        )
        if self.document["identity_closeout"]["complete"]:
            self.assertEqual(t48.COMPLETION_DELTA, self.document["completion_delta"])
            self.assertEqual(t48.CLOSING_EXECUTION_GAP, self.document["remaining_recipe_gap"])
        else:
            self.assertEqual(0, self.document["completion_delta"])
            self.assertEqual(t48.OPENING_EXECUTION_GAP, self.document["remaining_recipe_gap"])

    def test_t14_closing_is_remeasured_production_mix(self) -> None:
        t14 = self.document["t14_load"]
        self.assertEqual(t48.T14_CLOSING_BASIS, t14["closing_basis"])
        if t14["closing"].get("eager_publication_rows") is None:
            self.assertTrue(t48.production_strategy()["blocked"] or not t48.integrated_load_measured())
            return
        self.assertEqual(t48.CLOSING_EAGER_ROWS, t14["closing"]["eager_publication_rows"])
        self.assertEqual(t48.CLOSING_LAZY_ROWS, t14["closing"]["lazy_logical_rows"])
        self.assertEqual(t48.CLOSING_CACHE_CEILING_ROWS, t14["closing"]["lazy_cache_ceiling_rows"])


if __name__ == "__main__":
    unittest.main()
