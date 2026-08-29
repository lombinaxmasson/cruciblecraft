"""Contract tests for T40 readiness."""
from __future__ import annotations

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_t40_readiness as builder  # noqa: E402
from tools import t40_common as t40  # noqa: E402


class T40ReadinessTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.document = builder.build()

    def test_status_is_derived_from_gates(self) -> None:
        gates = self.document["gates"]
        failed = sorted(
            name
            for name, ok in gates.items()
            if name != "catalog_fixture_not_production_evidence" and not ok
        )
        self.assertEqual(failed, self.document["failed_gates"])
        self.assertEqual(
            "T40_READY" if not failed else "T40_BLOCKED",
            self.document["status"],
        )
        self.assertTrue(self.document["t35_history_readonly"])
        self.assertTrue(self.document["t39_history_readonly"])
        self.assertTrue(gates["production_lock_current"])
        self.assertTrue(gates["candidate_is_non_authoritative"])
        self.assertTrue(gates["phase_owner_audited"])
        self.assertTrue(gates["catalog_fixture_is_test_only"])
        self.assertTrue(gates["t39_ready"])

    def test_missing_measurements_or_gametest_block_ready(self) -> None:
        if t40.production_strategy()["blocked"] or not t40.player_gametest_present():
            self.assertEqual("T40_BLOCKED", self.document["status"])
            self.assertTrue(self.document["failed_gates"])
            self.assertFalse(self.document["evidence"]["t40_complete"])
            self.assertEqual("T40", self.document["evidence"]["unique_active_card"])

    def test_t41_opening_is_unissued(self) -> None:
        opening = self.document["t41_opening"]
        self.assertEqual("T41", opening["next_issue_id"])
        self.assertFalse(opening["preassigned_family_ids"])
        self.assertFalse(opening["preassigned_host"])
        self.assertEqual(
            t40.production_family_count(),
            self.document["evidence"]["generated_file_count"],
        )
        self.assertEqual(
            (
                t40.EXPECTED_REMAINING_ORDINARY_FAMILIES
                if self.document["gates"]["identity_closeout_complete"]
                else t40.T39_REMAINING_ORDINARY_FAMILIES
            ),
            self.document["evidence"]["remaining_recipe_gap"],
        )
