"""Contract tests for T36 readiness and the new topology epoch."""
from __future__ import annotations

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_t36_readiness as builder  # noqa: E402


class T36ReadinessTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.document = builder.build()

    def test_status_ready_when_gates_pass(self) -> None:
        self.assertTrue(all(self.document["gates"].values()))
        self.assertEqual("T36_READY", self.document["status"])
        self.assertTrue(self.document["t35_history_readonly"])

    def test_recipe_waves_precede_storage(self) -> None:
        sequence = self.document["topology"]["sequence"]
        tracks = [row["track"] for row in sequence]
        self.assertLess(tracks.index("recipe_wave"), tracks.index("storage_bundle"))
        self.assertEqual("T38", sequence[2]["id"])
        self.assertEqual("recipe_wave", sequence[2]["track"])
        self.assertEqual("T38", sequence[2]["owner"])
        self.assertEqual("cruciblecraft:roaster", sequence[2]["host"])
        self.assertEqual(29, sequence[2]["size"])
        self.assertEqual(29, len(sequence[2]["family_ids"]))
        self.assertIsNone(sequence[4].get("id"))
        self.assertEqual("storage_bundle", sequence[4]["track"])
        self.assertEqual(
            [f"T{number}" for number in range(38, 47)],
            self.document["topology"]["revoked_unstarted_cards"],
        )
        self.assertEqual(["T36", "T37"], self.document["topology"]["fixed_cards"])
        self.assertEqual(50, sequence[1]["size"])
        self.assertFalse(self.document["topology"]["append_only"])

    def test_new_numbering_keeps_t36_t37(self) -> None:
        self.assertEqual(0, self.document["topology"]["validators"]["t36_t37_fixed"])
        self.assertEqual(0, self.document["topology"]["validators"]["recipe_before_storage"])
        self.assertEqual("T36_RECIPE_THEN_STORAGE", self.document["topology"]["epoch"])

    def test_check_is_read_only(self) -> None:
        if not builder.OUTPUT.is_file():
            self.skipTest("t36_readiness.json not generated")
        before = builder.OUTPUT.read_bytes()
        self.assertEqual([], builder.check())
        self.assertEqual(before, builder.OUTPUT.read_bytes())


if __name__ == "__main__":
    unittest.main()
