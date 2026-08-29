"""T36-Repair pre-repair freeze: hashes only, write-once."""
from __future__ import annotations

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_t36_repair_pre_freeze as builder
from tools import t36_repair_common as common


class T36RepairPreFreezeTest(unittest.TestCase):
    def test_freeze_covers_85_acquisition_recipes(self) -> None:
        if builder.OUTPUT.is_file():
            document = common.load_json(builder.OUTPUT)
        else:
            document = builder.build()
        self.assertEqual("T36_REPAIR_PRE_FREEZE", document["artifact"])
        self.assertEqual("freeze_reference", document["reference_kind"])
        self.assertEqual("pre_repair", document["evidence_scope"])
        self.assertEqual(85, document["live_variant_count"])
        self.assertEqual(85, len(document["acquisition_snapshot"]))
        self.assertEqual(common.T43_CLOSING_GAP, document["t43_closing_gap"])
        self.assertTrue(document["t43_ready"])
        hashes = document["giant_artifact_hashes"]
        for relative_path in common.FREEZE_FILES:
            self.assertTrue(hashes.get(relative_path), relative_path)
        mixer = next(
            row
            for row in document["acquisition_snapshot"]
            if row["variant_id"] == "cruciblecraft:mixer"
        )
        steel_mixer = next(
            row
            for row in document["acquisition_snapshot"]
            if row["variant_id"] == "cruciblecraft:steel_mixer"
        )
        invar_smelter = next(
            row
            for row in document["acquisition_snapshot"]
            if row["variant_id"] == "cruciblecraft:invar_smelter"
        )
        invar_roaster = next(
            row
            for row in document["acquisition_snapshot"]
            if row["variant_id"] == "cruciblecraft:invar_roaster"
        )
        self.assertEqual("machine_generic", mixer["template"])
        self.assertEqual("centrifuge", steel_mixer["template"])
        self.assertEqual(
            "cruciblecraft:steel_double_machine_casing",
            invar_smelter["casing_item"],
        )
        self.assertEqual(
            "cruciblecraft:invar_double_machine_casing",
            invar_roaster["casing_item"],
        )
        self.assertEqual(
            2, document["device_freeze"]["crucible"]["materials"]["ceramic"]["processing_tier"]
        )

    def test_write_once_refuses_hash_rewrite(self) -> None:
        if not builder.OUTPUT.is_file():
            self.skipTest("t36_repair_pre_freeze.json not generated")
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
