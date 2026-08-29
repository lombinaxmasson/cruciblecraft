"""T36-Repair readiness: zero ownership, T36_READY preserved, T44 unassigned."""
from __future__ import annotations

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_t36_repair_readiness as builder
from tools import t36_repair_common as common


class T36RepairReadinessTest(unittest.TestCase):
    def test_status_is_derived_from_failed_gates(self) -> None:
        document = builder.build()
        failed = sorted(
            name for name, passed in document["gates"].items() if not passed
        )
        self.assertEqual(failed, document["failed_gates"])
        self.assertEqual(
            "T36_REPAIR_READY" if not failed else "T36_REPAIR_BLOCKED",
            document["status"],
        )
        self.assertEqual(0, document["owns_families"])
        self.assertEqual(0, document["completion_delta"])
        self.assertIsNone(document["unique_active_content_card"])
        self.assertEqual("T44", document["next_issue_id"])
        self.assertFalse(document["preassigned_host"])
        if document["failed_gates"]:
            self.assertNotEqual("T36_REPAIR_READY", document["status"])

    def test_ready_requires_inventory_clear_and_preserved_t36(self) -> None:
        document = builder.build()
        if document["failed_gates"]:
            self.skipTest("T36-Repair inventory or overlay proofs not green yet")
        self.assertEqual("T36_REPAIR_READY", document["status"])
        self.assertEqual([], document["failed_gates"])
        self.assertTrue(document["t36_ready_preserved"])
        self.assertEqual(common.T43_CLOSING_GAP, document["t43_closing_gap"])
        self.assertEqual(85, document["live_variant_count"])
        self.assertTrue(document["gates"]["inventory_blocking_zero"])
        self.assertTrue(document["gates"]["overlay_l1_lathe_invar"])
        self.assertTrue(document["gates"]["overlay_l2_iron_casing"])
        self.assertTrue(document["gates"]["overlay_device_iron_crucible"])
        self.assertTrue(document["gates"]["fixture_absent_from_production"])
        self.assertTrue(document["t44_not_issued"])


if __name__ == "__main__":
    unittest.main()
