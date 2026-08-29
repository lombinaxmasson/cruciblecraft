"""Contract tests for T38 readiness."""
from __future__ import annotations

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_t38_readiness as builder  # noqa: E402
from tools import t38_common as t38  # noqa: E402


class T38ReadinessTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.document = builder.build()

    def test_status_is_derived_from_gates(self) -> None:
        gates = self.document["gates"]
        failed = sorted(
            name
            for name, ok in gates.items()
            if name != "twenty_nine_files_not_sufficient" and not ok
        )
        self.assertEqual(failed, self.document["failed_gates"])
        if failed:
            self.assertEqual("T38_BLOCKED", self.document["status"])
        else:
            self.assertEqual("T38_READY", self.document["status"])
        self.assertTrue(self.document["t35_history_readonly"])
        self.assertTrue(self.document["t36_history_readonly"])
        self.assertTrue(self.document["t37_history_readonly"])
        self.assertTrue(gates["twenty_nine_files_not_sufficient"])

    def test_missing_measurements_or_gametest_block_ready(self) -> None:
        if t38.production_strategy()["blocked"] or not t38.player_gametest_present():
            self.assertEqual("T38_BLOCKED", self.document["status"])
            self.assertTrue(self.document["failed_gates"])
            self.assertFalse(self.document["evidence"]["t38_complete"])
            self.assertEqual("T38", self.document["evidence"]["unique_active_card"])

    def test_player_path_and_skip_gates_are_honest(self) -> None:
        gates = self.document["gates"]
        source = self.document["evidence"]["source"]
        self.assertTrue(gates["player_path_current"])
        self.assertNotIn("player_path_current", self.document["failed_gates"])
        self.assertEqual(
            gates["full_replay_dump_verified"],
            bool(source["dump_verified"]) and not bool(source["dump_skip"]),
        )
        self.assertTrue(source["skip_is_not_pass"])
        self.assertTrue(gates["player_gametest_present"])
        if self.document["status"] == "T38_BLOCKED":
            self.assertEqual("T38", self.document["evidence"]["unique_active_card"])
            self.assertTrue(self.document["failed_gates"])
        else:
            self.assertIsNone(self.document["evidence"]["unique_active_card"])
            self.assertFalse(self.document["failed_gates"])
            self.assertTrue(gates["identity_closeout_complete"])
            self.assertTrue(gates["topology_t38_complete"])

    def test_t39_opening_is_unissued_and_points_at_t38_closing(self) -> None:
        opening = self.document["t39_opening"]
        self.assertEqual("T39", opening["next_issue_id"])
        self.assertFalse(opening["preassigned_family_ids"])
        self.assertFalse(opening["preassigned_host"])
        self.assertEqual(29, self.document["evidence"]["generated_file_count"])
        self.assertEqual(5639, self.document["evidence"]["remaining_recipe_gap"])
        self.assertTrue(self.document["gates"]["t37_ready"])
        self.assertTrue(self.document["gates"]["t35_foundation_unchanged"])
        self.assertTrue(self.document["gates"]["t37_not_stolen"])
        self.assertTrue(self.document["gates"]["remaining_gap_5639"])
        if self.document["status"] == "T38_READY":
            self.assertEqual(
                t38.T14_AUTHORED_CLOSING,
                opening["t14_closing"]["datapack_authored_entries"],
            )
            self.assertIsNone(self.document["evidence"]["unique_active_card"])

    def test_twenty_nine_files_are_not_treated_as_ready(self) -> None:
        self.assertEqual(29, self.document["evidence"]["generated_file_count"])
        if self.document["status"] == "T38_READY":
            self.assertFalse(self.document["failed_gates"])
            self.assertTrue(self.document["gates"]["load_measured"])
            self.assertTrue(self.document["gates"]["winner_on_demand"])
            self.assertTrue(self.document["gates"]["player_gametest_present"])
            self.assertTrue(self.document["gates"]["full_replay_dump_verified"])

    def test_check_is_read_only(self) -> None:
        if not builder.OUTPUT.is_file():
            self.skipTest("t38_readiness.json not generated")
        before = builder.OUTPUT.read_bytes()
        self.assertEqual([], builder.check())
        self.assertEqual(before, builder.OUTPUT.read_bytes())


if __name__ == "__main__":
    unittest.main()
