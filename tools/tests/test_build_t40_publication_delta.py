"""Contract tests for the T40 publication overlay."""
from __future__ import annotations

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_t40_publication_delta as builder  # noqa: E402
from tools import t40_common as t40  # noqa: E402


class T40PublicationDeltaTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.document = builder.build()

    def test_status_tracks_current_production_decision(self) -> None:
        if t40.production_strategy()["blocked"]:
            self.assertEqual("T40_PUBLICATION_DELTA_BLOCKED", self.document["status"])
            self.assertTrue(self.document["production_winner_pending"])
        else:
            self.assertEqual("T40_PUBLICATION_DELTA_READY", self.document["status"])
            self.assertFalse(self.document["production_winner_pending"])

    def test_authored_and_logical_counts_match_lock(self) -> None:
        self.assertEqual(t40.production_family_count(), self.document["family_count"])
        self.assertEqual(
            t40.production_family_count(),
            self.document["authored_compact_family_entries"],
        )
        self.assertEqual(t40.production_relation_count(), self.document["logical"])
        self.assertEqual(
            t40.production_family_count(),
            self.document["generated_file_count"],
        )
        support = self.document["support_recipes"]
        self.assertEqual(0, support["authored"])
