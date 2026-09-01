"""Contract tests for the T46 topology overlay."""
from __future__ import annotations

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_t46_card_topology as builder  # noqa: E402
from tools import t46_common as t46  # noqa: E402


class T46CardTopologyTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.document = builder.build()

    def test_next_issue_is_t47_and_is_unassigned(self) -> None:
        self.assertEqual("T47", self.document["next_issue_id"])
        self.assertFalse(self.document["preassigned_host"])
        self.assertFalse(self.document["preassigned_family_ids"])
        self.assertEqual(0, self.document["validators"]["t47_not_preassigned"])
        t46_row = next(row for row in self.document["sequence"] if row.get("id") == "T46")
        self.assertEqual(t46.HOST, t46_row["host"])
        self.assertEqual(t46.production_family_count(), t46_row["size"])
        self.assertFalse(t46_row["preassigned_host"])
        self.assertFalse(t46_row["preassigned_family_ids"])
        self.assertTrue(
            any(row.get("id_policy") == "consecutive_from_T47" for row in self.document["sequence"])
        )
        if self.document.get("t46_complete"):
            self.assertIsNone(self.document["unique_active_card"])
            self.assertEqual(t46.CLOSING_EXECUTION_GAP, self.document["remaining_recipe_gap"])
        else:
            self.assertEqual("T46", self.document["unique_active_card"])
