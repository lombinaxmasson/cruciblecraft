"""Contract tests for the T39 census overlay."""
from __future__ import annotations

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_t39_census_delta as builder  # noqa: E402
from tools import t39_common as t39  # noqa: E402


class T39CensusDeltaTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.document = builder.build()

    def test_t35_foundation_denominators_are_unchanged(self) -> None:
        self.assertEqual(t39.T35_FOUNDATION, self.document["t35_foundation"])
        self.assertTrue(self.document["t35_foundation_unchanged"])
        self.assertEqual(78682, self.document["t35_foundation"]["recipe_rows_accounted"])
        self.assertEqual(5718, self.document["t35_foundation"]["recipe_families"])
        self.assertEqual(0, self.document["validators"]["t35_foundation_unchanged"])

    def test_exactly_production_locked_identities_are_overlayed(self) -> None:
        ids = [row["canonical_id"] for row in self.document["identities"]]
        self.assertEqual(t39.production_family_count(), len(ids))
        self.assertEqual(t39.production_family_count(), len(set(ids)))
        self.assertEqual(t39.production_family_ids(), ids)
        self.assertTrue(all(
            row.startswith(
                "portfolio:track_a/cruciblecraft:centrifuge/gt.recipe.centrifuge#"
            )
            for row in ids
        ))
        self.assertTrue(all(row["owner"] == t39.OWNER for row in self.document["identities"]))
        self.assertEqual(0, self.document["validators"]["owner_closing"])

    def test_opening_statuses_are_t39_planned_incomplete_pending(self) -> None:
        for row in self.document["identities"]:
            opening = row["opening"]
            self.assertEqual("planned", opening["disposition"])
            self.assertEqual("incomplete", opening["closure"])
            self.assertEqual("pending", opening["load"])
            self.assertEqual("source_backed", opening["fidelity"])
        self.assertIn("SOURCE_BACKED", self.document["fidelity"]["provenance_kinds"])
        self.assertFalse(self.document["fidelity"]["silent_drop"])
        self.assertEqual(0, self.document["validators"]["opening_planned_incomplete_pending"])

    def test_remaining_ordinary_separates_completion_and_reclassification(self) -> None:
        remaining = self.document["remaining_ordinary"]
        self.assertEqual(5639, remaining["t38_remaining_opening"])
        self.assertEqual(
            len(t39.phase_deferred_family_ids()),
            remaining["reclassified_to_nuclear"],
        )
        self.assertEqual(
            remaining["t38_remaining_opening"]
            - remaining["closed_by_t39"]
            - remaining["reclassified_to_nuclear"],
            remaining["remaining_ordinary_families"],
        )
        self.assertEqual(
            t39.EXPECTED_REMAINING_ORDINARY_FAMILIES,
            remaining["expected_remaining_ordinary_families"],
        )
        self.assertTrue(remaining["overlay_contains_only_locked_ids"])
        self.assertFalse(remaining["t35_files_rewritten"])
        self.assertFalse(remaining["t38_files_rewritten"])
        self.assertEqual([], remaining["t38_ids_in_overlay"])
        self.assertEqual(
            0 if self.document["identity_closeout"]["complete"] else 1,
            self.document["validators"]["remaining_ordinary_not_bulk_complete"],
        )
        self.assertEqual(0, self.document["validators"]["t38_not_stolen"])

    def test_t14_opening_is_t38_closing(self) -> None:
        t14 = self.document["t14_load"]
        self.assertEqual(t39.T14_OPENING_LOAD_SOURCE, t14["opening_source"])
        self.assertFalse(t14["hard_ceiling_raised"])
        self.assertEqual(3664, t14["opening"]["datapack_authored_entries"])
        if t39.production_strategy()["blocked"]:
            pending = [row for row in t14["axes"] if row["pending"]]
            self.assertGreater(len(pending), 0)

    def test_status_matches_closeout_evidence(self) -> None:
        closeout = self.document["identity_closeout"]
        if closeout["complete"]:
            self.assertEqual("T39_CENSUS_DELTA_READY", self.document["status"])
            self.assertFalse(closeout["blockers"])
        else:
            self.assertEqual("T39_CENSUS_DELTA_BLOCKED", self.document["status"])
            self.assertTrue(closeout["blockers"])

    def test_check_is_read_only_when_artifact_exists(self) -> None:
        if not builder.OUTPUT.is_file():
            self.skipTest("t39_census_delta.json not generated")
        before = builder.OUTPUT.read_bytes()
        self.assertEqual([], builder.check())
        self.assertEqual(before, builder.OUTPUT.read_bytes())


if __name__ == "__main__":
    unittest.main()
