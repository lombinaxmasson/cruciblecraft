"""T36-Repair second-list inventory contracts."""
from __future__ import annotations

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_t36_repair_second_list_inventory as builder
from tools import t36_repair_common as common


class T36RepairInventoryTest(unittest.TestCase):
    def test_inventory_classifies_blocking_and_later_rows(self) -> None:
        document = builder.build()
        self.assertEqual("T36_REPAIR_SECOND_LIST_INVENTORY", document["artifact"])
        self.assertEqual(85, document["live_variant_count"])
        self.assertEqual(0, document["owns_families"])
        ids = {row["id"]: row for row in document["lists"]}
        self.assertIn("authored_acquisition_set", ids)
        self.assertIn("processing_kind_behavior", ids)
        self.assertEqual("later:kind_behavior", ids["processing_kind_behavior"]["later"])
        self.assertFalse(ids["processing_kind_behavior"]["blocks_new_row"])
        self.assertFalse(ids["tiered_processing_block_aliases"]["blocks_new_row"])
        self.assertEqual(
            document["blocking_count"],
            len(document["blocking_ids"]),
        )
        for row in document["lists"]:
            if row["blocks_new_row"]:
                self.assertNotEqual("later", row["axis"])
                self.assertIsNone(row["later"])

    def test_cleared_lists_do_not_block(self) -> None:
        for row in common.second_list_rows():
            if row["status"] == "cleared":
                self.assertFalse(row["blocks_new_row"])


if __name__ == "__main__":
    unittest.main()
