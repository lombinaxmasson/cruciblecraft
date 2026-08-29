"""Contract tests for the T41 census overlay."""
from __future__ import annotations

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_t41_census_delta as builder  # noqa: E402
from tools import t41_common as t41  # noqa: E402


class T41CensusDeltaTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.document = builder.build()

    def test_t35_foundation_denominators_are_unchanged(self) -> None:
        self.assertEqual(t41.T35_FOUNDATION, self.document["t35_foundation"])
        self.assertTrue(self.document["t35_foundation_unchanged"])
        self.assertEqual(0, self.document["validators"]["t35_foundation_unchanged"])

    def test_exactly_production_locked_identities_are_overlayed(self) -> None:
        ids = [row["canonical_id"] for row in self.document["identities"]]
        self.assertEqual(t41.production_family_count(), len(ids))
        self.assertEqual(t41.production_family_ids(), ids)
        self.assertTrue(all(
            row.startswith(
                "portfolio:track_a/cruciblecraft:assembler/gt.recipe.assembler#"
            )
            for row in ids
        ))
        self.assertTrue(all(row["owner"] == t41.OWNER for row in self.document["identities"]))

    def test_remaining_ordinary_does_not_close_blocked_combinatorial(self) -> None:
        remaining = self.document["remaining_ordinary"]
        self.assertEqual(5597, remaining["t40_remaining_opening"])
        self.assertEqual(0, remaining["reclassified"])
        self.assertEqual(2, remaining["blocked_combinatorial"])
        self.assertEqual(
            remaining["t40_remaining_opening"]
            - remaining["closed_by_t41"]
            - remaining["already_expressed_by_t37"]
            - remaining["reclassified"],
            remaining["remaining_ordinary_families"],
        )
        self.assertEqual(
            t41.EXPECTED_REMAINING_ORDINARY_FAMILIES,
            remaining["expected_remaining_ordinary_families"],
        )
        self.assertEqual([], remaining["t38_ids_in_overlay"])
        self.assertEqual([], remaining["t40_ids_in_overlay"])
        self.assertFalse(remaining["t40_files_rewritten"])

    def test_t37_equivalent_leftovers_are_expressed_not_double_counted(self) -> None:
        expressed = t41.t37_expressed_t41_family_ids()
        self.assertEqual(
            [
                "portfolio:track_a/cruciblecraft:assembler/"
                f"gt.recipe.assembler#{index:04d}"
                for index in range(290, 340)
            ],
            expressed,
        )
        self.assertEqual(
            t41.production_family_count() - 50,
            t41.independent_live_family_count(),
        )
        self.assertEqual(
            t41.independent_live_family_count(),
            t41.gametest_locked_live_count(),
        )
        remaining = self.document["remaining_ordinary"]
        closeout = self.document["identity_closeout"]
        if closeout["complete"]:
            self.assertEqual(242, remaining["closed_by_t41"])
            self.assertEqual(50, remaining["already_expressed_by_t37"])
            self.assertEqual(5305, remaining["remaining_ordinary_families"])
            self.assertEqual("T41_CENSUS_DELTA_READY", self.document["status"])
            tagged = [
                row["canonical_id"]
                for row in self.document["identities"]
                if row["closing"].get("expressed_by") == "t37"
            ]
            self.assertEqual(expressed, tagged)
        else:
            self.assertIn("t37_expressed_accounted", closeout["blockers"])
            self.assertEqual(0, remaining["closed_by_t41"])
            self.assertEqual(0, remaining["already_expressed_by_t37"])
            self.assertEqual(
                t41.T40_REMAINING_ORDINARY_FAMILIES,
                remaining["remaining_ordinary_families"],
            )

    def test_t14_opening_is_t40_closing(self) -> None:
        t14 = self.document["t14_load"]
        self.assertEqual(t41.T14_OPENING_LOAD_SOURCE, t14["opening_source"])
        self.assertFalse(t14["hard_ceiling_raised"])
        self.assertEqual(3733, t14["opening"]["datapack_authored_entries"])
