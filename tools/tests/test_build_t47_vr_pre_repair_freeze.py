"""Contract tests for the T47-VR pre-repair freeze."""
from __future__ import annotations

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_t47_vr_pre_repair_freeze as builder
from tools import t47_vr_common as vr


class T47VrPreRepairFreezeTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.document = builder.build()

    def test_freeze_pins_required_production_facts(self) -> None:
        self.assertEqual("T47_VR_PRE_REPAIR_FREEZE", self.document["artifact"])
        self.assertEqual("freeze_reference", self.document["reference_kind"])
        self.assertEqual("pre_repair", self.document["evidence_scope"])
        self.assertEqual(vr.FROZEN_COUNTS, self.document["frozen_counts"])
        self.assertEqual(vr.FROZEN_COUNTS, vr.live_counts())
        self.assertEqual(
            vr.EXPECTED_T47_LOCK_SHA256,
            self.document["immutable_lock_hashes"]["tools/t47_production_lock.json"],
        )
        self.assertEqual(
            vr.EXPECTED_T46_LOCK_SHA256,
            self.document["immutable_lock_hashes"]["tools/t46_production_lock.json"],
        )
        self.assertTrue(self.document["t48_not_issued"])
        self.assertEqual(list(vr.OPEN_DEBT_IDS), self.document["open_debt_ids"])
        self.assertEqual(395, self.document["frozen_counts"]["t47_production_families"])
        self.assertEqual(13708, self.document["frozen_counts"]["t47_production_relations"])
        self.assertEqual(283, self.document["frozen_counts"]["t47_identities"])
        self.assertEqual(1499, self.document["frozen_counts"]["t47_remaining_ordinary_gap"])
        self.assertEqual(803, self.document["frozen_counts"]["t46_complete_families"])
        self.assertEqual(1894, self.document["frozen_counts"]["t46_remaining_gap_opening"])
        self.assertEqual(15, self.document["frozen_counts"]["composed_runtime_groups"])
        self.assertEqual(10, self.document["frozen_counts"]["t47_gametest_passed"])

    def test_snapshot_covers_locks_census_receipts_and_v2(self) -> None:
        snapshot = self.document["snapshot_hashes"]
        for path in vr.SNAPSHOT_HASH_PATHS:
            self.assertTrue(snapshot.get(path), path)
        self.assertEqual(
            snapshot["tools/t47_production_lock.json"],
            vr.EXPECTED_T47_LOCK_SHA256,
        )
        self.assertEqual(
            snapshot["tools/t46_production_lock.json"],
            vr.EXPECTED_T46_LOCK_SHA256,
        )

    def test_check_is_read_only_when_artifact_exists(self) -> None:
        if not builder.OUTPUT.is_file():
            self.skipTest("t47_vr_pre_repair_freeze.json not generated")
        before = builder.OUTPUT.read_bytes()
        self.assertEqual([], builder.check())
        self.assertEqual(before, builder.OUTPUT.read_bytes())


if __name__ == "__main__":
    unittest.main()
