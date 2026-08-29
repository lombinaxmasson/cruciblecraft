"""Contract tests for the T43 census overlay."""
from __future__ import annotations

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_t43_census_delta as builder  # noqa: E402
from tools import t43_common as t43  # noqa: E402


class T43CensusDeltaTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.document = builder.build()

    def test_t35_foundation_denominators_are_unchanged(self) -> None:
        self.assertEqual(t43.T35_FOUNDATION, self.document["t35_foundation"])
        self.assertTrue(self.document["t35_foundation_unchanged"])
        self.assertEqual(0, self.document["validators"]["t35_foundation_unchanged"])

    def test_exactly_production_locked_identities_are_overlayed(self) -> None:
        ids = [row["canonical_id"] for row in self.document["identities"]]
        self.assertEqual(t43.production_family_count(), len(ids))
        self.assertEqual(t43.production_family_ids(), ids)
        self.assertTrue(all(row["owner"] == t43.OWNER for row in self.document["identities"]))
        self.assertEqual(t43.OPENING_EXECUTION_GAP, self.document["opening_execution_gap"])
        self.assertEqual(0, self.document["partial_family_count"])
        self.assertEqual(0, self.document["reclassification_delta"])
        remaining = self.document["remaining_ordinary"]
        self.assertFalse(remaining["t35_files_rewritten"])
        self.assertFalse(remaining["t42_files_rewritten"])
        self.assertEqual(
            t43.EXPECTED_REMAINING_ORDINARY_FAMILIES,
            remaining["expected_remaining_ordinary_families"],
        )
