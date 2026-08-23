"""Contract tests for the T37 census overlay."""
from __future__ import annotations

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_t37_census_delta as builder  # noqa: E402
from tools import t37_common as t37  # noqa: E402


class T37CensusDeltaTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.document = builder.build()

    def test_t35_foundation_denominators_are_unchanged(self) -> None:
        self.assertEqual(t37.T35_FOUNDATION, self.document["t35_foundation"])
        self.assertTrue(self.document["t35_foundation_unchanged"])
        self.assertEqual(78682, self.document["t35_foundation"]["recipe_rows_accounted"])
        self.assertEqual(5718, self.document["t35_foundation"]["recipe_families"])
        self.assertEqual(0, self.document["validators"]["t35_foundation_unchanged"])

    def test_exactly_fifty_pilot_identities_are_overlayed(self) -> None:
        ids = [row["canonical_id"] for row in self.document["identities"]]
        self.assertEqual(50, len(ids))
        self.assertEqual(50, len(set(ids)))
        self.assertTrue(all(
            row.startswith(
                "portfolio:track_a/cruciblecraft:assembler/gt.recipe.assembler#"
            )
            for row in ids
        ))
        self.assertEqual(
            "portfolio:track_a/cruciblecraft:assembler/gt.recipe.assembler#0002",
            ids[0],
        )
        self.assertEqual(
            "portfolio:track_a/cruciblecraft:assembler/gt.recipe.assembler#0051",
            ids[-1],
        )
        self.assertTrue(all(row["owner"] == t37.OWNER for row in self.document["identities"]))
        self.assertEqual(0, self.document["validators"]["owner_unchanged"])

    def test_opening_statuses_are_planned_incomplete_pending(self) -> None:
        for row in self.document["identities"]:
            opening = row["opening"]
            self.assertEqual("planned", opening["disposition"])
            self.assertEqual("incomplete", opening["closure"])
            self.assertEqual("pending", opening["load"])
            self.assertEqual("source_backed", opening["fidelity"])
        self.assertIn("SOURCE_BACKED", self.document["fidelity"]["provenance_kinds"])
        self.assertFalse(self.document["fidelity"]["silent_drop"])
        self.assertEqual(0, self.document["validators"]["fidelity_recorded_not_dropped"])

    def test_remaining_ordinary_families_are_not_bulk_complete(self) -> None:
        remaining = self.document["remaining_ordinary"]
        self.assertEqual(5668, remaining["remaining_ordinary_families"])
        self.assertTrue(remaining["overlay_contains_only_pilot_ids"])
        self.assertFalse(remaining["t35_files_rewritten"])
        self.assertEqual(0, self.document["validators"]["remaining_ordinary_not_bulk_complete"])

    def test_t38_roaster_families_are_not_stolen(self) -> None:
        t38_ids = set(self.document["t38_frozen_wave"]["family_ids"])
        overlay_ids = {row["canonical_id"] for row in self.document["identities"]}
        self.assertEqual(29, len(t38_ids))
        self.assertEqual(set(), t38_ids & overlay_ids)
        self.assertEqual("cruciblecraft:roaster", self.document["t38_frozen_wave"]["host"])
        self.assertEqual(0, self.document["validators"]["t38_not_stolen"])

    def test_t14_pending_axes_are_not_zero_filled(self) -> None:
        pending = [
            row
            for row in self.document["t14_load"]["axes"]
            if row["pending"]
        ]
        for row in pending:
            self.assertIsNone(row["delta"])
            self.assertIsNone(row["closing"])
        self.assertFalse(self.document["t14_load"]["hard_ceiling_raised"])
        self.assertEqual(0, self.document["validators"]["t14_hard_ceiling_not_raised"])
        self.assertEqual(0, self.document["validators"]["t14_pending_not_zero_filled"])
        self.assertEqual(
            3566,
            self.document["t14_load"]["opening"]["datapack_authored_entries"],
        )
        self.assertEqual(
            3616,
            self.document["t14_load"]["closing"]["datapack_authored_entries"],
        )
        if t37.production_strategy()["blocked"]:
            self.assertGreater(len(pending), 0)
        else:
            runtime_pending = [
                row["axis"]
                for row in pending
                if row["axis"] not in t37.STRATEGY_AXES
            ]
            self.assertEqual([], runtime_pending)
            measured = {
                row["axis"]: row
                for row in self.document["t14_load"]["axes"]
                if row["axis"] in (
                    "sync_bytes",
                    "server_reload_ms",
                    "lookup_p95_ns",
                    "lookup_candidate_count",
                )
            }
            for axis, row in measured.items():
                self.assertTrue(row["measured"], axis)
                self.assertFalse(row["pending"], axis)
                self.assertIsNotNone(row["closing"], axis)
                self.assertNotEqual(0, row["delta"], axis)

    def test_identity_closeout_is_honest_when_measurements_block(self) -> None:
        closeout = self.document["identity_closeout"]
        if t37.production_strategy()["blocked"] or not t37.player_gametest_present():
            self.assertFalse(closeout["complete"])
            self.assertEqual("T37_CENSUS_DELTA_BLOCKED", self.document["status"])
            for row in self.document["identities"]:
                self.assertEqual("implemented", row["closing"]["disposition"])
                self.assertNotEqual("measured", row["closing"]["load"])
        self.assertEqual(50, self.document["publication"]["authored_compact_family_entries"])
        self.assertEqual(50, self.document["publication"]["logical"])

    def test_check_is_read_only(self) -> None:
        if not builder.OUTPUT.is_file():
            self.skipTest("t37_census_delta.json not generated")
        before = builder.OUTPUT.read_bytes()
        self.assertEqual([], builder.check())
        self.assertEqual(before, builder.OUTPUT.read_bytes())


if __name__ == "__main__":
    unittest.main()
