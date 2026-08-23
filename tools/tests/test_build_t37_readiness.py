"""Contract tests for T37 readiness."""
from __future__ import annotations

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_t37_readiness as builder  # noqa: E402
from tools import t37_common as t37  # noqa: E402


class T37ReadinessTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.document = builder.build()

    def test_status_is_derived_from_gates(self) -> None:
        gates = self.document["gates"]
        failed = sorted(
            name
            for name, ok in gates.items()
            if name != "fifty_files_not_sufficient" and not ok
        )
        self.assertEqual(failed, self.document["failed_gates"])
        if failed:
            self.assertEqual("T37_BLOCKED", self.document["status"])
        else:
            self.assertEqual("T37_READY", self.document["status"])
        self.assertTrue(self.document["t35_history_readonly"])
        self.assertTrue(self.document["t36_history_readonly"])
        self.assertTrue(gates["fifty_files_not_sufficient"])

    def test_missing_measurements_or_gametest_block_ready(self) -> None:
        if t37.production_strategy()["blocked"] or not t37.player_gametest_present():
            self.assertEqual("T37_BLOCKED", self.document["status"])
            self.assertTrue(self.document["failed_gates"])
            self.assertFalse(self.document["evidence"]["t37_complete"])
            self.assertEqual("T37", self.document["evidence"]["unique_active_card"])

    def test_t38_opening_points_at_t37_closing(self) -> None:
        opening = self.document["t38_opening"]
        self.assertEqual(
            "tools/t37_census_delta.json#t14_load",
            opening["load_opening_source"],
        )
        self.assertEqual("cruciblecraft:roaster", opening["host"])
        self.assertEqual(29, opening["families"])
        self.assertEqual(
            3616,
            opening["t14_closing"]["datapack_authored_entries"],
        )
        self.assertTrue(self.document["gates"]["t36_ready"])
        self.assertTrue(self.document["gates"]["t35_foundation_unchanged"])
        self.assertTrue(self.document["gates"]["t38_not_stolen"])

    def test_fifty_files_are_not_treated_as_ready(self) -> None:
        self.assertEqual(50, self.document["evidence"]["generated_file_count"])
        if self.document["status"] == "T37_READY":
            self.assertFalse(self.document["failed_gates"])
            self.assertTrue(self.document["gates"]["load_measured"])
            self.assertTrue(self.document["gates"]["player_gametest_present"])
            self.assertTrue(self.document["gates"]["full_replay_dump_verified"])

    def test_check_is_read_only(self) -> None:
        if not builder.OUTPUT.is_file():
            self.skipTest("t37_readiness.json not generated")
        before = builder.OUTPUT.read_bytes()
        self.assertEqual([], builder.check())
        self.assertEqual(before, builder.OUTPUT.read_bytes())


if __name__ == "__main__":
    unittest.main()
