"""Contract tests for T41 topology overlay."""
from __future__ import annotations

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_t41_card_topology as builder  # noqa: E402
from tools import t41_common as t41  # noqa: E402


class T41CardTopologyTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.document = builder.build()

    def test_prior_cards_complete_and_t41_is_assembler_wave(self) -> None:
        sequence = self.document["sequence"]
        self.assertEqual("T36", sequence[0]["id"])
        self.assertEqual("complete", sequence[0]["status"])
        self.assertEqual("T37", sequence[1]["id"])
        self.assertEqual("complete", sequence[1]["status"])
        self.assertEqual("T38", sequence[2]["id"])
        self.assertEqual("complete", sequence[2]["status"])
        self.assertEqual("T39", sequence[3]["id"])
        self.assertEqual("complete", sequence[3]["status"])
        self.assertEqual("T40", sequence[4]["id"])
        self.assertEqual("complete", sequence[4]["status"])
        self.assertEqual("T41", sequence[5]["id"])
        self.assertEqual("recipe_wave", sequence[5]["track"])
        self.assertEqual(t41.HOST, sequence[5]["host"])
        self.assertEqual(t41.production_family_count(), sequence[5]["size"])
        self.assertEqual(
            ["T36", "T37", "T38", "T39", "T40", "T41"],
            self.document["fixed_cards"],
        )
        self.assertEqual("T42", self.document["next_issue_id"])
        self.assertEqual(
            (
                t41.EXPECTED_REMAINING_ORDINARY_FAMILIES
                if self.document["t41_complete"]
                else t41.T40_REMAINING_ORDINARY_FAMILIES
            ),
            self.document["remaining_recipe_gap"],
        )

    def test_t41_active_while_incomplete(self) -> None:
        sequence = self.document["sequence"]
        if self.document["t41_complete"]:
            self.assertEqual("complete", sequence[5]["status"])
            self.assertIsNone(self.document["unique_active_card"])
        else:
            self.assertEqual("active", sequence[5]["status"])
            self.assertEqual("T41", self.document["unique_active_card"])

    def test_t42_is_not_preassigned(self) -> None:
        sequence = self.document["sequence"]
        self.assertIsNone(sequence[6].get("id"))
        self.assertIsNone(sequence[6].get("family_ids"))
        self.assertIsNone(sequence[6].get("host"))
        self.assertFalse(sequence[6].get("preassigned_host"))
        self.assertEqual("consecutive_from_T42", sequence[6]["id_policy"])
        self.assertEqual("storage_bundle", sequence[7]["track"])
        self.assertIsNone(sequence[7].get("id"))
        self.assertEqual(0, self.document["validators"]["t42_not_preassigned"])
