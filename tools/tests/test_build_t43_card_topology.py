"""Contract tests for the T43 topology overlay."""
from __future__ import annotations

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_t43_card_topology as builder  # noqa: E402
from tools import t43_common as t43  # noqa: E402


class T43CardTopologyTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.document = builder.build()

    def test_next_issue_is_t44_and_storage_is_unassigned(self) -> None:
        self.assertEqual("T44", self.document["next_issue_id"])
        self.assertFalse(self.document["preassigned_host"])
        self.assertFalse(self.document["preassigned_family_ids"])
        self.assertEqual(0, self.document["validators"]["t44_not_preassigned"])
        self.assertEqual(0, self.document["validators"]["storage_not_preassigned"])
        self.assertEqual(0, self.document["validators"]["t36_repair_does_not_occupy_t44"])
        t43_row = next(row for row in self.document["sequence"] if row.get("id") == "T43")
        self.assertEqual(t43.HOST, t43_row["host"])
        self.assertEqual(t43.production_family_count(), t43_row["size"])
        self.assertNotIn("T36-Repair", {row.get("id") for row in self.document["sequence"]})
