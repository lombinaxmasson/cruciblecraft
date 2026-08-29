"""Contract tests for the T41 publication overlay."""
from __future__ import annotations

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_t41_publication_delta as builder  # noqa: E402
from tools import t41_common as t41  # noqa: E402


class T41PublicationDeltaTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.document = builder.build()

    def test_status_tracks_current_production_decision(self) -> None:
        if t41.production_strategy()["blocked"]:
            self.assertEqual("T41_PUBLICATION_DELTA_BLOCKED", self.document["status"])
            self.assertTrue(self.document["production_winner_pending"])
        else:
            self.assertEqual("T41_PUBLICATION_DELTA_READY", self.document["status"])
            self.assertFalse(self.document["production_winner_pending"])

    def test_authored_and_logical_counts_match_lock(self) -> None:
        self.assertEqual(t41.production_family_count(), self.document["family_count"])
        self.assertEqual(
            t41.production_family_count(),
            self.document["authored_compact_family_entries"],
        )
        self.assertEqual(t41.production_relation_count(), self.document["logical"])
        self.assertEqual(
            t41.production_family_count(),
            self.document["generated_file_count"],
        )
        support = self.document["support_recipes"]
        self.assertEqual(
            t41.load_production_lock()["support"]["route_count"],
            support["authored"],
        )
        groups = self.document["card_level_shared_costs"]["publication_groups"]
        self.assertEqual(
            {"planks", "fireproof", "planks2"},
            set(groups),
        )
