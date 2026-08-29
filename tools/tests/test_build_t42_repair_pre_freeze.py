"""T42-Repair pre-repair freeze: hashes only, write-once."""
from __future__ import annotations

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_t42_repair_pre_freeze as builder
from tools import t42_common as common


class T42RepairPreFreezeTest(unittest.TestCase):
    def test_freeze_lists_giant_artifact_hashes_only(self) -> None:
        if builder.OUTPUT.is_file():
            document = common.load_json(builder.OUTPUT)
        else:
            document = builder.build()
        self.assertEqual("T42_REPAIR_PRE_FREEZE", document["artifact"])
        self.assertEqual("freeze_reference", document["reference_kind"])
        self.assertEqual("pre_repair", document["evidence_scope"])
        self.assertEqual(common.OPENING_EXECUTION_GAP, document["opening_execution_gap"])
        self.assertTrue(document["t43_not_issued"])
        hashes = document["giant_artifact_hashes"]
        self.assertEqual(set(common.REPAIR_GIANT_ARTIFACTS), set(hashes))
        self.assertTrue(all(hashes.values()))
        self.assertNotIn("families", document)

    def test_write_once_refuses_hash_rewrite(self) -> None:
        if not builder.OUTPUT.is_file():
            self.skipTest("t42_repair_pre_freeze.json not generated")
        before = builder.OUTPUT.read_bytes()
        self.assertEqual([], builder.check())
        stored = common.load_json(builder.OUTPUT)
        live = builder.build()
        if live["giant_artifact_hashes"] == stored["giant_artifact_hashes"]:
            builder.write()
            self.assertEqual(before, builder.OUTPUT.read_bytes())
            return
        with self.assertRaises(ValueError):
            builder.write()
        self.assertEqual(before, builder.OUTPUT.read_bytes())


if __name__ == "__main__":
    unittest.main()
