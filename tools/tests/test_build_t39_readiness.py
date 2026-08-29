"""Contract tests for T39 readiness."""
from __future__ import annotations

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_t39_readiness as builder  # noqa: E402
from tools import t39_common as t39  # noqa: E402


class T39ReadinessTest(unittest.TestCase):
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
            "T39_READY" if not failed else "T39_BLOCKED",
            self.document["status"],
        )
        self.assertTrue(self.document["t35_history_readonly"])
        self.assertTrue(self.document["t38_history_readonly"])
        self.assertTrue(gates["production_lock_current"])
        self.assertTrue(gates["candidate_is_non_authoritative"])
        self.assertTrue(gates["phase_owner_audited"])
        self.assertTrue(gates["catalog_fixture_is_test_only"])

    def test_missing_measurements_or_gametest_block_ready(self) -> None:
        if t39.production_strategy()["blocked"] or not t39.player_gametest_present():
            self.assertEqual("T39_BLOCKED", self.document["status"])
            self.assertTrue(self.document["failed_gates"])
            self.assertFalse(self.document["evidence"]["t39_complete"])
            self.assertEqual("T39", self.document["evidence"]["unique_active_card"])

    def test_player_path_gate_uses_layered_production_lock_proof(self) -> None:
        player = self.document["evidence"]["player_path"]
        self.assertEqual(
            self.document["gates"]["player_path_current"],
            player["current"],
        )
        self.assertEqual(
            t39.production_relation_count(),
            player["relations"],
        )
        self.assertEqual(
            t39.production_lock_sha256(),
            player["production_lock_sha256"],
        )

    def test_t40_opening_is_unissued(self) -> None:
        opening = self.document["t40_opening"]
        self.assertEqual("T40", opening["next_issue_id"])
        self.assertFalse(opening["preassigned_family_ids"])
        self.assertFalse(opening["preassigned_host"])
        self.assertEqual(
            t39.production_family_count(),
            self.document["evidence"]["generated_file_count"],
        )
        self.assertEqual(
            (
                t39.EXPECTED_REMAINING_ORDINARY_FAMILIES
                if self.document["gates"]["identity_closeout_complete"]
                else t39.T38_REMAINING_ORDINARY_FAMILIES
                - len(t39.phase_deferred_family_ids())
            ),
            self.document["evidence"]["remaining_recipe_gap"],
        )
        self.assertTrue(self.document["gates"]["t35_foundation_unchanged"])
        self.assertTrue(self.document["gates"]["t38_not_stolen"])
        self.assertEqual(
            self.document["gates"]["identity_closeout_complete"],
            self.document["gates"]["remaining_gap_honest"],
        )

    def test_write_requires_all_gates(self) -> None:
        from tools.tests.support import authority_sandbox

        if self.document["failed_gates"]:
            with self.assertRaises(ValueError):
                builder.write()
            return
        with authority_sandbox.patch_builder_path(builder, "OUTPUT"):
            self.assertEqual("T39_READY", builder.write()["status"])

    def test_check_is_read_only_when_artifact_exists(self) -> None:
        if not builder.OUTPUT.is_file():
            self.skipTest("t39_readiness.json not generated")
        before = builder.OUTPUT.read_bytes()
        self.assertEqual([], builder.check())
        self.assertEqual(before, builder.OUTPUT.read_bytes())


if __name__ == "__main__":
    unittest.main()
