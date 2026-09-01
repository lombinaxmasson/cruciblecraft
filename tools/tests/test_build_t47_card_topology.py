"""Contract tests for the T47 topology overlay."""
from __future__ import annotations

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_t47_card_topology as builder  # noqa: E402
from tools import t47_common as t47  # noqa: E402


class T47CardTopologyTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.document = builder.build()

    def test_next_issue_is_t48_and_is_unassigned(self) -> None:
        self.assertEqual("T48", self.document["next_issue_id"])
        self.assertFalse(self.document["preassigned_host"])
        self.assertFalse(self.document["preassigned_family_ids"])
        self.assertEqual(0, self.document["validators"]["t48_not_preassigned"])
        t47_row = next(row for row in self.document["sequence"] if row.get("id") == "T47")
        self.assertEqual(t47.HOST, t47_row["host"])
        self.assertEqual(t47.production_family_count(), t47_row["size"])
        self.assertFalse(t47_row["preassigned_host"])
        self.assertFalse(t47_row["preassigned_family_ids"])
        self.assertTrue(
            any(row.get("id_policy") == "consecutive_from_T48" for row in self.document["sequence"])
        )
        if self.document.get("t47_complete"):
            self.assertIsNone(self.document["unique_active_card"])
            self.assertEqual(t47.CLOSING_EXECUTION_GAP, self.document["remaining_recipe_gap"])
        else:
            self.assertEqual("T47", self.document["unique_active_card"])
