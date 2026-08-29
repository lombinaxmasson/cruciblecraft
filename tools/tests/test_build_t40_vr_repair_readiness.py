"""Contract tests for the T40-VR repair account."""
from __future__ import annotations

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_t40_vr_repair_readiness as builder
from tools import t40_vr_common as vr


class T40VrRepairReadinessTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.document = builder.build()

    def test_status_is_derived_and_repair_owns_no_gap(self) -> None:
        failed = sorted(
            name for name, passed in self.document["gates"].items() if not passed
        )
        self.assertEqual(failed, self.document["failed_gates"])
        self.assertEqual(
            "T40_VR_READY" if not failed else "T40_VR_BLOCKED",
            self.document["status"],
        )
        self.assertEqual(0, self.document["owns_families"])
        self.assertEqual(0, self.document["gap_delta"])
        self.assertEqual(vr.FROZEN_COUNTS, self.document["frozen_counts"])
        self.assertEqual(vr.FROZEN_COUNTS, self.document["live_counts"])
        self.assertTrue(self.document["gates"]["repair_owns_no_families"])
        self.assertTrue(self.document["gates"]["repair_gap_delta_zero"])
        self.assertTrue(self.document["gates"]["t41_not_issued"])
        self.assertTrue(self.document["gates"]["open_debt_not_disguised"])
        self.assertIn("T32-VD-002", self.document["open_debt_ids"])
        self.assertIn("T32-VD-004", self.document["open_debt_ids"])

    def test_locks_and_typed_ore_denominators_are_frozen(self) -> None:
        self.assertEqual(13, self.document["frozen_counts"]["t40_production_families"])
        self.assertEqual(22, self.document["frozen_counts"]["t40_production_relations"])
        self.assertEqual(5597, self.document["frozen_counts"]["t40_remaining_ordinary_gap"])
        self.assertEqual(34, self.document["frozen_counts"]["t39_locked_support_routes"])
        self.assertEqual(19, self.document["frozen_counts"]["t38_support_authored"])
        self.assertEqual(15, self.document["frozen_counts"]["t38_support_eager"])
        self.assertEqual(137, self.document["frozen_counts"]["factual_ore_materials"])
        self.assertEqual(147, self.document["frozen_counts"]["registered_ore_materials"])
        self.assertEqual(10, self.document["frozen_counts"]["t38_acquisition_ore_delta"])
        self.assertEqual(8, self.document["frozen_counts"]["t5_semantic_vein_ledger"])
        self.assertTrue(self.document["gates"]["t39_t40_locks_not_resigned"])
        self.assertTrue(self.document["gates"]["frozen_counts_hold"])

    def test_check_is_read_only_when_artifact_exists(self) -> None:
        if not builder.OUTPUT.is_file():
            self.skipTest("t40_vr_repair_readiness.json not generated")
        before = builder.OUTPUT.read_bytes()
        self.assertEqual([], builder.check())
        self.assertEqual(before, builder.OUTPUT.read_bytes())


if __name__ == "__main__":
    unittest.main()
