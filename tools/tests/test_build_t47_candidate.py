#!/usr/bin/env python3
"""Contract tests for T47 Bath remainder candidate selection."""
from __future__ import annotations

import unittest

from tools import build_t47_candidate_selection as candidate
from tools import t47_common as common

COORDINATION_OWNER = "coordination/multi_axis"


class T47CandidateTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.document = common.load_json(candidate.OUTPUT)
        cls.work_set = common.load_json(common.WORK_SET)

    def test_all_545_families_appear_exactly_once(self) -> None:
        families = list(self.document["families"] or [])
        family_ids = [str(row["family_id"]) for row in families]
        work_ids = list(self.work_set["family_ids"] or [])
        self.assertEqual("T47_CANDIDATE_SELECTION", self.document["status"])
        self.assertEqual(common.CANDIDATE_FAMILY_COUNT, self.document["coverage"]["family_count"])
        self.assertEqual(common.CANDIDATE_FAMILY_COUNT, len(families))
        self.assertEqual(common.CANDIDATE_FAMILY_COUNT, len(set(family_ids)))
        self.assertEqual(work_ids, family_ids)
        self.assertEqual(
            common.selection_sha256(work_ids),
            self.document["selection_sha256"],
        )

    def test_coverage_partitions_the_remainder_universe(self) -> None:
        coverage = self.document["coverage"]
        production = int(coverage["production"])
        blocked = int(coverage["blocked"])
        reclassified = int(coverage["reclassified"])
        self.assertGreaterEqual(production, common.MIN_PRODUCTION_FAMILIES)
        self.assertGreaterEqual(blocked, 0)
        self.assertEqual(0, reclassified)
        self.assertEqual(common.CANDIDATE_FAMILY_COUNT, production + blocked + reclassified)
        self.assertEqual(production, len(self.document["production_family_ids"]))
        outcomes = [str(row["candidate_outcome"]) for row in self.document["families"]]
        self.assertEqual(production, outcomes.count("production"))
        self.assertEqual(blocked, outcomes.count("blocked"))
        self.assertEqual(reclassified, outcomes.count("reclassified"))
        self.assertEqual(
            list(self.document["production_family_ids"]),
            [
                str(row["family_id"])
                for row in self.document["families"]
                if row["candidate_outcome"] == "production"
            ],
        )

    def test_remainder_excludes_t46_closed_family_ids(self) -> None:
        closed = common.t46_closed_family_ids()
        remainder = {str(row["family_id"]) for row in self.document["families"]}
        self.assertEqual(common.T46_CLOSED_FAMILIES, len(closed))
        self.assertEqual(set(), closed.intersection(remainder))
        self.assertEqual(set(), closed.intersection(set(self.document["production_family_ids"])))

    def test_coordination_multi_axis_is_recorded_but_not_a_production_owner(self) -> None:
        self.assertFalse(self.document["production_authority"])
        self.assertIn("not a production owner", self.document["note"])
        self.assertIn(COORDINATION_OWNER, self.document["note"])
        coordination = [
            row
            for row in self.document["families"]
            if row.get("current_owner") == COORDINATION_OWNER
        ]
        self.assertTrue(coordination)
        outcomes = {str(row["candidate_outcome"]) for row in coordination}
        self.assertIn("blocked", outcomes)
        self.assertNotEqual({"production"}, outcomes)
        production_owners = {
            str(row["current_owner"] or "")
            for row in self.document["families"]
            if row["candidate_outcome"] == "production"
        }
        self.assertNotEqual({COORDINATION_OWNER}, production_owners)
        self.assertEqual(candidate.OUTPUT, common.CANDIDATE_SELECTION)
