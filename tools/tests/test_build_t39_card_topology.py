"""Contract tests for T39 topology overlay."""
from __future__ import annotations

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_t39_card_topology as builder  # noqa: E402
from tools import t39_common as t39  # noqa: E402


class T39CardTopologyTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.document = builder.build()

    def test_prior_cards_complete_and_t39_is_active_centrifuge_wave(self) -> None:
        sequence = self.document["sequence"]
        self.assertEqual("complete", sequence[0]["status"])
        self.assertEqual("T36", sequence[0]["id"])
        self.assertEqual("complete", sequence[1]["status"])
        self.assertEqual("T37", sequence[1]["id"])
        self.assertEqual("complete", sequence[2]["status"])
        self.assertEqual("T38", sequence[2]["id"])
        self.assertEqual("T39", sequence[3]["id"])
        self.assertEqual("recipe_wave", sequence[3]["track"])
        self.assertEqual("cruciblecraft:centrifuge", sequence[3]["host"])
        self.assertEqual(t39.production_family_count(), sequence[3]["size"])
        self.assertEqual(t39.production_family_count(), len(sequence[3]["family_ids"]))
        self.assertEqual(
            t39.production_family_ids(),
            sequence[3]["family_ids"],
        )
        self.assertEqual(["T36", "T37", "T38", "T39"], self.document["fixed_cards"])
        self.assertEqual("T40", self.document["next_issue_id"])
        self.assertEqual(
            (
                t39.EXPECTED_REMAINING_ORDINARY_FAMILIES
                if self.document["t39_complete"]
                else t39.T38_REMAINING_ORDINARY_FAMILIES
                - len(t39.phase_deferred_family_ids())
            ),
            self.document["remaining_recipe_gap"],
        )

    def test_t39_active_while_incomplete(self) -> None:
        sequence = self.document["sequence"]
        if self.document["t39_complete"]:
            self.assertEqual("complete", sequence[3]["status"])
            self.assertIsNone(self.document["unique_active_card"])
        else:
            self.assertEqual("active", sequence[3]["status"])
            self.assertEqual("T39", self.document["unique_active_card"])

    def test_wave_size_band_and_shard_note(self) -> None:
        sequence = self.document["sequence"]
        self.assertEqual([50, 200], sequence[3]["wave_size_band"])
        self.assertTrue(sequence[3].get("shard_architecture_start"))
        self.assertIn("fixture-only", sequence[3]["note"].lower())
        self.assertEqual("production_lock_wave", sequence[3]["kind"])
        self.assertEqual(
            t39.production_lock_sha256(),
            sequence[3]["production_lock_sha256"],
        )

    def test_recipe_waves_precede_storage_and_t40_is_not_preassigned(self) -> None:
        sequence = self.document["sequence"]
        tracks = [row["track"] for row in sequence]
        self.assertLess(tracks.index("recipe_wave"), tracks.index("storage_bundle"))
        self.assertIsNone(sequence[4].get("id"))
        self.assertIsNone(sequence[4].get("family_ids"))
        self.assertIsNone(sequence[4].get("host"))
        self.assertFalse(sequence[4].get("preassigned_host"))
        self.assertEqual("consecutive_from_T40", sequence[4]["id_policy"])
        self.assertIsNone(sequence[5].get("id"))
        self.assertEqual("storage_bundle", sequence[5]["track"])
        self.assertEqual(0, self.document["validators"]["storage_not_preassigned"])
        self.assertEqual(0, self.document["validators"]["t40_not_preassigned"])

    def test_check_is_read_only_when_artifact_exists(self) -> None:
        if not builder.OUTPUT.is_file():
            self.skipTest("t39_card_topology.json not generated")
        before = builder.OUTPUT.read_bytes()
        self.assertEqual([], builder.check())
        self.assertEqual(before, builder.OUTPUT.read_bytes())


if __name__ == "__main__":
    unittest.main()
