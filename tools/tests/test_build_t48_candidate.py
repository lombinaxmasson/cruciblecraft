#!/usr/bin/env python3
"""Contract tests for T48 Bath remainder candidate selection."""
from __future__ import annotations

import unittest

from tools import t48_common as common


class T48CandidateTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.document = common.load_json(common.CANDIDATE_SELECTION)
        cls.work_set = common.load_json(common.WORK_SET)

    def test_all_150_families_appear_exactly_once(self) -> None:
        families = list(self.document["families"] or [])
        family_ids = [str(row["family_id"]) for row in families]
        work_ids = list(self.work_set["family_ids"] or [])
        self.assertEqual("T48_CANDIDATE_SELECTION", self.document["status"])
        self.assertEqual(common.CANDIDATE_FAMILY_COUNT, self.document["coverage"]["family_count"])
        self.assertEqual(common.CANDIDATE_FAMILY_COUNT, len(families))
        self.assertEqual(work_ids, family_ids)
        self.assertIs(False, self.document.get("production_authority"))

    def test_coverage_partitions_the_blocked_universe(self) -> None:
        coverage = self.document["coverage"]
        production = int(coverage["production"])
        blocked = int(coverage["blocked"])
        reclassified = int(coverage["reclassified"])
        self.assertEqual(145, production)
        self.assertEqual(5, blocked)
        self.assertEqual(0, reclassified)
        self.assertEqual(common.CANDIDATE_FAMILY_COUNT, production + blocked + reclassified)
        self.assertEqual(production, len(self.document["production_family_ids"]))


if __name__ == "__main__":
    unittest.main()
