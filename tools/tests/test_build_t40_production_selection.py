"""Contract tests for T40 Electrolyzer candidate selection."""
from __future__ import annotations

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_t40_production_selection as builder  # noqa: E402
from tools import t40_common as t40  # noqa: E402


class T40ProductionSelectionTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.document = builder.build()

    def test_catalog_stays_fixture_while_subset_is_only_candidate(self) -> None:
        self.assertEqual("T40_PRODUCTION_CANDIDATE", self.document["status"])
        withdrawn = self.document["withdrawn_catalog"]
        self.assertEqual(t40.CATALOG_FAMILY_COUNT, withdrawn["family_count"])
        self.assertEqual(t40.CATALOG_RELATION_COUNT, withdrawn["relations"])
        self.assertEqual(t40.EXPECTED_SELECTION_SHA256, withdrawn["selection_sha256"])
        candidate = self.document["candidate"]
        self.assertEqual(13, candidate["families"])
        self.assertEqual(22, candidate["relations"])
        self.assertLess(candidate["families"], t40.CATALOG_FAMILY_COUNT)
        self.assertEqual(
            t40.selection_sha256(candidate["family_ids"]),
            candidate["selection_sha256"],
        )
        self.assertNotEqual(t40.EXPECTED_SELECTION_SHA256, candidate["selection_sha256"])
        self.assertEqual(
            candidate["player_path"]["inputs_reachable"],
            candidate["relations"],
        )
        rejected = {row["template_key"] for row in self.document["review"]["rejected"]}
        self.assertIn("gt.recipe.electrolyzer#0000", rejected)
        self.assertIn("gt.recipe.electrolyzer#0001", rejected)
        self.assertNotIn("gt.recipe.electrolyzer#0000", candidate["template_keys"])
        self.assertNotIn("gt.recipe.electrolyzer#0001", candidate["template_keys"])

    def test_family_atomic_trusted_matches_lock(self) -> None:
        trusted = self.document["family_atomic_trusted"]
        self.assertEqual(13, trusted["families"])
        self.assertEqual(22, trusted["relations"])
        self.assertEqual(11, trusted["singleton_families"])
        self.assertEqual(2, trusted["multi_families"])
        self.assertFalse(trusted.get("includes_t40_support", False))
        self.assertEqual(
            t40.T39_REMAINING_ORDINARY_FAMILIES - trusted["families"],
            self.document["gap_projection"]["if_trusted_subset_candidate"],
        )

    def test_combinatorial_is_blocked_not_gap_closed(self) -> None:
        self.assertEqual(
            set(t40.COMBINATORIAL_TEMPLATE_KEYS),
            {
                str(row["template_key"])
                for row in (t40.load_production_lock().get("phase_deferred") or [])
            },
        )
        self.assertEqual(
            t40.T39_REMAINING_ORDINARY_FAMILIES - 13,
            t40.EXPECTED_REMAINING_ORDINARY_FAMILIES,
        )
