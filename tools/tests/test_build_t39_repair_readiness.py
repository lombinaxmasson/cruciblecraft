"""Contract tests for the T39-Repair exit account."""
from __future__ import annotations

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_t39_repair_readiness as builder  # noqa: E402


class T39RepairReadinessTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.document = builder.build()

    def test_status_is_derived_and_repair_owns_no_gap(self) -> None:
        failed = sorted(
            name for name, passed in self.document["gates"].items() if not passed
        )
        self.assertEqual(failed, self.document["failed_gates"])
        self.assertEqual(
            "T39_REPAIR_READY" if not failed else "T39_REPAIR_BLOCKED",
            self.document["status"],
        )
        self.assertEqual(0, self.document["owns_families"])
        self.assertEqual(0, self.document["gap_delta"])

    def test_catalog_candidate_and_production_are_separate(self) -> None:
        self.assertEqual(157, self.document["catalog_fixture"]["families"])
        self.assertEqual(250, self.document["catalog_fixture"]["relations"])
        self.assertEqual(22, self.document["production"]["families"])
        self.assertEqual(32, self.document["production"]["relations"])
        self.assertEqual(34, self.document["production"]["support_routes"])
        self.assertTrue(self.document["gates"]["candidate_is_non_authoritative"])
        self.assertTrue(self.document["gates"]["production_and_fixture_ids_disjoint"])
        self.assertTrue(self.document["gates"]["production_unproven_lossy_alias_zero"])

    def test_check_is_read_only_when_artifact_exists(self) -> None:
        if not builder.OUTPUT.is_file():
            self.skipTest("t39_repair_readiness.json not generated")
        before = builder.OUTPUT.read_bytes()
        self.assertEqual([], builder.check())
        self.assertEqual(before, builder.OUTPUT.read_bytes())


if __name__ == "__main__":
    unittest.main()
