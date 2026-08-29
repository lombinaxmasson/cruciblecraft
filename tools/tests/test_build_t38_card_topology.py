"""Contract tests for T38 topology overlay."""
from __future__ import annotations

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_t38_card_topology as builder  # noqa: E402
from tools import t38_common as t38  # noqa: E402


class T38CardTopologyTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.document = builder.build()

    def test_t36_t37_complete_and_t38_stays_frozen_roaster_wave(self) -> None:
        sequence = self.document["sequence"]
        self.assertEqual("complete", sequence[0]["status"])
        self.assertEqual("T36", sequence[0]["id"])
        self.assertEqual("complete", sequence[1]["status"])
        self.assertEqual("T37", sequence[1]["id"])
        self.assertEqual("T38", sequence[2]["id"])
        self.assertEqual("recipe_wave", sequence[2]["track"])
        self.assertEqual("cruciblecraft:roaster", sequence[2]["host"])
        self.assertEqual(29, sequence[2]["size"])
        self.assertEqual(29, len(sequence[2]["family_ids"]))
        self.assertEqual(
            t38.load_t38_frozen_family_ids(),
            sequence[2]["family_ids"],
        )
        self.assertEqual(
            t38.T14_OPENING_LOAD_SOURCE,
            sequence[2]["load_opening"]["source"],
        )
        self.assertEqual(["T36", "T37", "T38"], self.document["fixed_cards"])
        self.assertEqual("T39", self.document["next_issue_id"])
        self.assertEqual(5639, self.document["remaining_recipe_gap"])

    def test_t38_complete_only_when_honestly_ready(self) -> None:
        sequence = self.document["sequence"]
        if self.document["t38_complete"]:
            self.assertEqual("complete", sequence[2]["status"])
            self.assertIsNone(self.document["unique_active_card"])
        else:
            self.assertEqual("active", sequence[2]["status"])
            self.assertEqual("T38", self.document["unique_active_card"])
            self.assertNotEqual("complete", sequence[2]["status"])

    def test_recipe_waves_precede_storage_and_t39_is_not_preassigned(self) -> None:
        sequence = self.document["sequence"]
        tracks = [row["track"] for row in sequence]
        self.assertLess(tracks.index("recipe_wave"), tracks.index("storage_bundle"))
        self.assertIsNone(sequence[3].get("id"))
        self.assertIsNone(sequence[3].get("family_ids"))
        self.assertIsNone(sequence[3].get("host"))
        self.assertEqual("consecutive_from_T39", sequence[3]["id_policy"])
        self.assertIsNone(sequence[4].get("id"))
        self.assertEqual("storage_bundle", sequence[4]["track"])
        self.assertEqual(0, self.document["validators"]["storage_not_preassigned"])
        self.assertEqual(0, self.document["validators"]["t39_not_preassigned"])
        self.assertEqual(0, self.document["validators"]["recipe_before_storage"])
        self.assertFalse(self.document["append_only"])
        self.assertEqual("T38_RECIPE_THEN_STORAGE", self.document["epoch"])

    def test_check_is_read_only(self) -> None:
        if not builder.OUTPUT.is_file():
            self.skipTest("t38_card_topology.json not generated")
        before = builder.OUTPUT.read_bytes()
        self.assertEqual([], builder.check())
        self.assertEqual(before, builder.OUTPUT.read_bytes())


if __name__ == "__main__":
    unittest.main()
