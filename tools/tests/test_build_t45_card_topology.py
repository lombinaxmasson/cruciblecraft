"""Contract tests for the T45 topology overlay."""
from __future__ import annotations

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_t45_card_topology as builder  # noqa: E402
from tools import t45_common as t45  # noqa: E402


class T45CardTopologyTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.document = builder.build()

    def test_next_issue_is_t46_and_is_unassigned(self) -> None:
        self.assertEqual("T46", self.document["next_issue_id"])
        self.assertFalse(self.document["preassigned_host"])
        self.assertFalse(self.document["preassigned_family_ids"])
        self.assertEqual(0, self.document["validators"]["t46_not_preassigned"])
        t45_row = next(row for row in self.document["sequence"] if row.get("id") == "T45")
        self.assertEqual(t45.HOST, t45_row["host"])
        self.assertEqual(t45.production_family_count(), t45_row["size"])
        self.assertFalse(t45_row["preassigned_host"])
        self.assertFalse(t45_row["preassigned_family_ids"])
        self.assertTrue(
            any(row.get("id_policy") == "consecutive_from_T46" for row in self.document["sequence"])
        )
        if self.document.get("t45_complete"):
            self.assertIsNone(self.document["unique_active_card"])
            self.assertEqual(t45.CLOSING_EXECUTION_GAP, self.document["remaining_recipe_gap"])
        else:
            self.assertEqual("T45", self.document["unique_active_card"])
