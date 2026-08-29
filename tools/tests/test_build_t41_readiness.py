"""Contract tests for T41 readiness."""
from __future__ import annotations

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_t41_readiness as builder  # noqa: E402
from tools import t41_common as t41  # noqa: E402


class T41ReadinessTest(unittest.TestCase):
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
            "T41_READY" if not failed else "T41_BLOCKED",
            self.document["status"],
        )
        self.assertTrue(self.document["t35_history_readonly"])
        self.assertTrue(self.document["t40_history_readonly"])
        self.assertTrue(gates["production_lock_current"])
        self.assertTrue(gates["candidate_is_non_authoritative"])
        self.assertTrue(gates["phase_owner_audited"])
        self.assertTrue(gates["catalog_fixture_is_test_only"])
        self.assertTrue(gates["t40_ready"])

    def test_missing_measurements_or_gametest_block_ready(self) -> None:
        if t41.production_strategy()["blocked"] or not t41.player_gametest_present():
            self.assertEqual("T41_BLOCKED", self.document["status"])
            self.assertTrue(self.document["failed_gates"])
            self.assertFalse(self.document["evidence"].get("t41_complete"))
            self.assertEqual("T41", self.document["evidence"]["unique_active_card"])

    def test_t42_opening_is_unissued(self) -> None:
        opening = self.document["t42_opening"]
        self.assertEqual("T42", opening["next_issue_id"])
        self.assertFalse(opening["preassigned_family_ids"])
        self.assertFalse(opening["preassigned_host"])
        self.assertEqual(
            t41.production_family_count(),
            self.document["evidence"]["generated_file_count"],
        )
        self.assertEqual(
            (
                t41.EXPECTED_REMAINING_ORDINARY_FAMILIES
                if self.document["gates"]["identity_closeout_complete"]
                else t41.T40_REMAINING_ORDINARY_FAMILIES
            ),
            self.document["evidence"]["remaining_recipe_gap"],
        )
