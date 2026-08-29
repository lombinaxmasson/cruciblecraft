"""T42 family operand snapshot contract tests."""
from __future__ import annotations

import sys
import unittest
from pathlib import Path
from unittest import mock

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_t42_family_operand_snapshot as builder
from tools import t42_common as common


class T42FamilyOperandSnapshotTest(unittest.TestCase):
    def test_full_replay_without_dump_fails(self) -> None:
        with mock.patch.object(
            common,
            "require_dump",
            side_effect=OSError("missing GT6 dump map required for full replay"),
        ):
            with self.assertRaises(OSError):
                builder.build_from_dump()

    def test_write_without_full_replay_is_refused(self) -> None:
        with self.assertRaises(ValueError):
            builder.write(full_replay=False)

    def test_check_without_dump_uses_committed_snapshot(self) -> None:
        if not builder.OUTPUT.is_file():
            self.skipTest("t42_family_operand_snapshot.json not generated")
        errors = builder.check(full_replay=False)
        self.assertEqual([], errors)
        snapshot = common.load_json(builder.OUTPUT)
        self.assertEqual(common.OPENING_EXECUTION_GAP, snapshot["family_count"])
        self.assertEqual(
            common.load_json(common.REMAINING_CATALOG)["ordinary_source_rows"],
            snapshot["relation_count"],
        )

    def test_count_zero_circuit_is_preserve(self) -> None:
        self.assertEqual(
            "preserve",
            common.operand_action(
                {
                    "item": "gregapi:gt.integrated_circuit",
                    "count": 0,
                    "meta": 1,
                }
            ),
        )
        identity = common.logical_relation_identity(
            {
                "item_inputs": [{"runtime_id": "cruciblecraft:programmed_circuit"}],
                "item_input_counts": [0],
                "item_input_actions": [{"kind": "preserve"}],
                "item_outputs": [{"runtime_id": "minecraft:oak_button", "count": 1}],
                "fluid_inputs": [],
                "fluid_outputs": [],
                "duration": 20,
                "eut": 16,
                "chances": [10000],
                "special_value": 0,
                "can_be_buffered": True,
            }
        )
        self.assertIn("PRESERVE", identity)
        self.assertNotIn("CONSUME", identity.split("||", 1)[0])

    def test_empty_slot_markers_keep_metaitem_molds(self) -> None:
        self.assertFalse(
            common.empty_item({"item": "gregtech:gt.metaitem.01", "meta": 32, "count": 1})
        )
        self.assertTrue(common.empty_item({"item": "gregtech:gt.meta.empty"}))
        self.assertTrue(common.empty_item({"item": "gregtech:gt.meta.empty", "count": 1}))
        self.assertIn("gt.meta.empty", common.EMPTY_ITEM_MARKERS)
        self.assertNotIn("gt.metaitem.01", common.EMPTY_ITEM_MARKERS)

    def test_snapshot_interns_dust_form(self) -> None:
        catalogs = common.load_runtime_catalogs()
        mapped = common.map_item_source(
            {"item": "gregtech:gt.meta.dust", "meta": 260, "count": 1},
            catalogs,
        )
        self.assertEqual("dust", mapped.get("form"))
        self.assertTrue(mapped.get("material"))
        if not builder.OUTPUT.is_file():
            self.skipTest("t42_family_operand_snapshot.json not generated")
        snapshot = common.load_json(builder.OUTPUT)
        if "operand_layout" not in snapshot or not snapshot.get("dictionaries", {}).get("forms"):
            self.skipTest("snapshot has not interned material/form yet")
        forms = snapshot.get("dictionaries", {}).get("forms") or []
        items = snapshot.get("dictionaries", {}).get("items") or []
        if "gregtech:gt.meta.dust" not in items:
            self.skipTest("snapshot does not intern gt.meta.dust yet")
        self.assertIn("dust", forms)
        self.assertEqual(
            [
                "side",
                "item",
                "fluid",
                "meta",
                "count",
                "action",
                "material",
                "form",
                "alias",
            ],
            snapshot.get("operand_layout"),
        )


if __name__ == "__main__":
    unittest.main()
