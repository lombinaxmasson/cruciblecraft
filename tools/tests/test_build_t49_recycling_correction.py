#!/usr/bin/env python3
"""T49 append-only recycling_candidate correction contract."""
from __future__ import annotations

import unittest

from tools import recycling_candidate
from tools import t35_common as t35
from tools import t42_common as t42
from tools.build_t49_recycling_candidate_correction import (
    BATH_TINY,
    EXPECTED_IDS,
    build,
)


class T49RecyclingCorrectionTest(unittest.TestCase):
    def test_overlay_flagged_set_is_exactly_thirteen(self) -> None:
        overlay = t35.load_json(t42.BLOCKER_OVERLAY)
        flagged = {
            str(row["family_id"])
            for row in overlay.get("families") or []
            if row.get("recycling_candidate")
        }
        self.assertEqual(EXPECTED_IDS, flagged)

    def test_correction_clears_effective_flag_without_rewriting_overlay(self) -> None:
        document = build()
        self.assertEqual("T49_RECYCLING_CANDIDATE_CORRECTION", document["status"])
        self.assertTrue(document["t42_overlay_unmodified"])
        self.assertEqual(13, document["family_count"])
        self.assertEqual(1817, document["deferred_recycling_untouched"])
        self.assertEqual(list(BATH_TINY), document["lock_family_ids"])
        overlay = recycling_candidate.overlay_by_id()
        for family_id in EXPECTED_IDS:
            self.assertTrue(overlay[family_id].get("recycling_candidate"))
            self.assertFalse(
                recycling_candidate.effective_recycling_candidate(
                    family_id,
                    overlay[family_id],
                    correction=document,
                )
            )
        for row in document["families"]:
            self.assertIs(row["recycling_candidate"], False)
            self.assertIs(row["overlay_recycling_candidate"], True)


if __name__ == "__main__":
    unittest.main()
