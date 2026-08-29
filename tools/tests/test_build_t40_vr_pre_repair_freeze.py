"""Contract tests for the T40-VR pre-repair freeze."""
from __future__ import annotations

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_t40_vr_pre_repair_freeze as builder
from tools import t40_vr_common as vr


class T40VrPreRepairFreezeTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.document = builder.build()

    def test_freeze_pins_required_production_facts(self) -> None:
        self.assertEqual("T40_VR_PRE_REPAIR_FREEZE", self.document["artifact"])
        self.assertEqual("freeze_reference", self.document["reference_kind"])
        self.assertEqual("pre_repair", self.document["evidence_scope"])
        self.assertEqual(vr.FROZEN_COUNTS, self.document["frozen_counts"])
        self.assertEqual(vr.FROZEN_COUNTS, vr.live_counts())
        self.assertEqual(
            vr.EXPECTED_T40_LOCK_SHA256,
            self.document["immutable_lock_hashes"]["tools/t40_production_lock.json"],
        )
        self.assertEqual(
            vr.EXPECTED_T39_LOCK_SHA256,
            self.document["immutable_lock_hashes"]["tools/t39_production_lock.json"],
        )
        self.assertTrue(self.document["t41_not_issued"])
        self.assertEqual(list(vr.OPEN_DEBT_IDS), self.document["open_debt_ids"])

    def test_snapshot_covers_locks_gate_receipts_and_debt(self) -> None:
        snapshot = self.document["snapshot_hashes"]
        for path in vr.SNAPSHOT_HASH_PATHS:
            self.assertTrue(snapshot.get(path), path)
        self.assertEqual(
            snapshot["tools/t40_production_lock.json"],
            vr.EXPECTED_T40_LOCK_SHA256,
        )

    def test_check_is_read_only_when_artifact_exists(self) -> None:
        if not builder.OUTPUT.is_file():
            self.skipTest("t40_vr_pre_repair_freeze.json not generated")
        before = builder.OUTPUT.read_bytes()
        self.assertEqual([], builder.check())
        self.assertEqual(before, builder.OUTPUT.read_bytes())


if __name__ == "__main__":
    unittest.main()
