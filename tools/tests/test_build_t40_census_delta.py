"""Contract tests for the T40 census overlay."""
from __future__ import annotations

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_t40_census_delta as builder  # noqa: E402
from tools import t40_common as t40  # noqa: E402


class T40CensusDeltaTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.document = builder.build()

    def test_t35_foundation_denominators_are_unchanged(self) -> None:
        self.assertEqual(t40.T35_FOUNDATION, self.document["t35_foundation"])
        self.assertTrue(self.document["t35_foundation_unchanged"])
        self.assertEqual(0, self.document["validators"]["t35_foundation_unchanged"])

    def test_exactly_production_locked_identities_are_overlayed(self) -> None:
        ids = [row["canonical_id"] for row in self.document["identities"]]
        self.assertEqual(t40.production_family_count(), len(ids))
        self.assertEqual(t40.production_family_ids(), ids)
        self.assertTrue(all(
            row.startswith(
                "portfolio:track_a/cruciblecraft:electrolyzer/gt.recipe.electrolyzer#"
            )
            for row in ids
        ))
        self.assertTrue(all(row["owner"] == t40.OWNER for row in self.document["identities"]))

    def test_remaining_ordinary_does_not_close_blocked_combinatorial(self) -> None:
        remaining = self.document["remaining_ordinary"]
        self.assertEqual(5610, remaining["t39_remaining_opening"])
        self.assertEqual(0, remaining["reclassified"])
        self.assertEqual(2, remaining["blocked_combinatorial"])
        self.assertEqual(
            remaining["t39_remaining_opening"]
            - remaining["closed_by_t40"]
            - remaining["reclassified"],
            remaining["remaining_ordinary_families"],
        )
        self.assertEqual(
            t40.EXPECTED_REMAINING_ORDINARY_FAMILIES,
            remaining["expected_remaining_ordinary_families"],
        )
        self.assertEqual([], remaining["t38_ids_in_overlay"])
        self.assertEqual([], remaining["t39_ids_in_overlay"])
        self.assertFalse(remaining["t39_files_rewritten"])

    def test_t14_opening_is_t39_closing(self) -> None:
        t14 = self.document["t14_load"]
        self.assertEqual(t40.T14_OPENING_LOAD_SOURCE, t14["opening_source"])
        self.assertFalse(t14["hard_ceiling_raised"])
        self.assertEqual(3720, t14["opening"]["datapack_authored_entries"])
        self.assertEqual(16659, t14["opening"]["eager_publication_rows"])
