"""Contract tests for T40 topology overlay."""
from __future__ import annotations

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_t40_card_topology as builder  # noqa: E402
from tools import t40_common as t40  # noqa: E402


class T40CardTopologyTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.document = builder.build()

    def test_prior_cards_complete_and_t40_is_electrolyzer_wave(self) -> None:
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
        self.assertEqual("recipe_wave", sequence[4]["track"])
        self.assertEqual(t40.HOST, sequence[4]["host"])
        self.assertEqual(t40.production_family_count(), sequence[4]["size"])
        self.assertEqual(["T36", "T37", "T38", "T39", "T40"], self.document["fixed_cards"])
        self.assertEqual("T41", self.document["next_issue_id"])
        self.assertEqual(
            (
                t40.EXPECTED_REMAINING_ORDINARY_FAMILIES
                if self.document["t40_complete"]
                else t40.T39_REMAINING_ORDINARY_FAMILIES
            ),
            self.document["remaining_recipe_gap"],
        )

    def test_t40_active_while_incomplete(self) -> None:
        sequence = self.document["sequence"]
        if self.document["t40_complete"]:
            self.assertEqual("complete", sequence[4]["status"])
            self.assertIsNone(self.document["unique_active_card"])
        else:
            self.assertEqual("active", sequence[4]["status"])
            self.assertEqual("T40", self.document["unique_active_card"])

    def test_t41_is_not_preassigned(self) -> None:
        sequence = self.document["sequence"]
        self.assertIsNone(sequence[5].get("id"))
        self.assertIsNone(sequence[5].get("family_ids"))
        self.assertIsNone(sequence[5].get("host"))
        self.assertFalse(sequence[5].get("preassigned_host"))
        self.assertEqual("consecutive_from_T41", sequence[5]["id_policy"])
        self.assertEqual("storage_bundle", sequence[6]["track"])
        self.assertIsNone(sequence[6].get("id"))
        self.assertEqual(0, self.document["validators"]["t41_not_preassigned"])
