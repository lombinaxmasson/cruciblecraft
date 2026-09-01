"""Contract tests for the T48 topology overlay."""
from __future__ import annotations

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_t48_card_topology as builder  # noqa: E402
from tools import t48_common as t48  # noqa: E402


class T48CardTopologyTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.document = builder.build()

    def test_next_issue_is_t49_and_is_unassigned(self) -> None:
        self.assertEqual("T49", self.document["next_issue_id"])
        self.assertFalse(self.document["preassigned_host"])
        self.assertFalse(self.document["preassigned_family_ids"])
        self.assertEqual(0, self.document["validators"]["t48_not_preassigned"])
        t48_row = next(row for row in self.document["sequence"] if row.get("id") == "T48")
        self.assertEqual(t48.HOST, t48_row["host"])
        self.assertEqual(["T47"], t48_row.get("depends_on"))
        self.assertTrue(
            any(row.get("id_policy") == "consecutive_from_T49" for row in self.document["sequence"])
        )
        if self.document.get("t48_complete"):
            self.assertIsNone(self.document["unique_active_card"])
            self.assertEqual(t48.CLOSING_EXECUTION_GAP, self.document["remaining_recipe_gap"])
        else:
            self.assertEqual("T48", self.document["unique_active_card"])


if __name__ == "__main__":
    unittest.main()
