"""Contract tests for the T39 publication overlay."""
from __future__ import annotations

import json
import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_t39_publication_delta as builder  # noqa: E402
from tools import t39_common as t39  # noqa: E402


class T39PublicationDeltaTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.document = builder.build()

    def test_status_tracks_current_production_decision(self) -> None:
        if t39.production_strategy()["blocked"]:
            self.assertEqual("T39_PUBLICATION_DELTA_BLOCKED", self.document["status"])
            self.assertTrue(self.document["production_winner_pending"])
        else:
            self.assertEqual("T39_PUBLICATION_DELTA_READY", self.document["status"])
            self.assertFalse(self.document["production_winner_pending"])

    def test_authored_and_logical_counts_match_work_set(self) -> None:
        self.assertEqual(t39.production_family_count(), self.document["family_count"])
        self.assertEqual(
            t39.production_family_count(),
            self.document["authored_compact_family_entries"],
        )
        self.assertEqual(t39.production_relation_count(), self.document["logical"])
        self.assertEqual(
            t39.production_family_count(),
            self.document["generated_file_count"],
        )
        support = self.document["support_recipes"]
        ledger = t39.support_recipe_ledger()
        self.assertEqual(ledger["gt_recovery_count"], support["gt_recovery_count"])
        self.assertEqual(ledger["crafting_count"], support["crafting_count"])
        self.assertEqual(ledger["authored"], support["authored"])
        self.assertEqual(ledger["eager"], support["eager"])
        self.assertGreater(support["authored"], 0)
        self.assertEqual(
            t39.production_family_count() + ledger["authored"],
            self.document["authored_datapack_entries"],
        )

    def test_two_publication_groups_are_declared(self) -> None:
        groups = self.document["card_level_shared_costs"]["publication_groups"]
        self.assertIn("singleton", groups)
        self.assertIn("multi", groups)
        self.assertEqual(t39.SINGLETON_GROUP, groups["singleton"]["publication_group"])
        self.assertEqual(t39.MULTI_GROUP, groups["multi"]["publication_group"])
        counts = t39.production_group_counts()
        self.assertEqual(
            counts[t39.SINGLETON_GROUP]["relations"],
            groups["singleton"]["logical_rows"],
        )
        self.assertEqual(
            counts[t39.MULTI_GROUP]["relations"],
            groups["multi"]["logical_rows"],
        )

    def test_does_not_copy_t37_or_t38_partitions(self) -> None:
        encoded = json.dumps(self.document)
        self.assertNotIn('"eager": 14', encoded)
        self.assertNotIn('"lazy": 36', encoded)
        self.assertNotIn('"lazy_cache_ceiling_rows": 8', encoded)
        self.assertNotIn('"eager_publication_rows": 0, "lazy_logical_rows": 73', encoded)
        self.assertEqual(0, self.document["validators"]["group_winners_not_t37_t38_partitions"])

    def test_pending_strategy_does_not_zero_fill_card_totals(self) -> None:
        if self.document["production_winner_pending"]:
            self.assertIsNone(self.document["eager"])
            self.assertIsNone(self.document["lazy"])
            card = self.document["card_level_shared_costs"]
            self.assertIsNone(card["lazy_cache_ceiling_rows"])

    def test_check_is_read_only_when_artifact_exists(self) -> None:
        if not builder.OUTPUT.is_file():
            self.skipTest("t39_publication_delta.json not generated")
        before = builder.OUTPUT.read_bytes()
        self.assertEqual([], builder.check())
        self.assertEqual(before, builder.OUTPUT.read_bytes())


if __name__ == "__main__":
    unittest.main()
