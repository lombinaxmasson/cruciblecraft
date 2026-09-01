#!/usr/bin/env python3
"""Contract tests for T48 remainder work-set freeze."""
from __future__ import annotations

import unittest

from tools import build_t48_work_set as work_set
from tools import t48_common as common


class T48WorkSetTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.document = work_set.build()

    def test_work_set_is_bath_blocked_universe(self) -> None:
        self.assertEqual(common.CANDIDATE_FAMILY_COUNT, self.document["family_count"])
        self.assertEqual(common.CANDIDATE_RELATION_COUNT, self.document["relation_count"])
        self.assertEqual("T48", self.document["unique_active_card"])
        self.assertEqual("T48_WORK_SET_FROZEN", self.document["status"])
        self.assertEqual(common.HOST, self.document["host"])
        self.assertTrue(self.document["candidate_universe"])
        self.assertFalse(self.document["production_authority"])

    def test_n_less_than_300_exception_is_recorded(self) -> None:
        exception = self.document["size_exception"]
        self.assertEqual(common.EXCEPTION_KIND, exception["exception_kind"])
        self.assertEqual(150, exception["candidate_family_ceiling"])
        self.assertEqual(34186, exception["candidate_relation_ceiling"])
        self.assertEqual(1, exception["minimum_N"])
        self.assertIn("Mixer", exception["padding_hosts_forbidden"])

    def test_representation_matches_blocked_replay(self) -> None:
        representation = self.document["representation"]
        self.assertEqual(common.CANDIDATE_EXACT_FAMILIES, representation["exact"])
        self.assertEqual(common.CANDIDATE_EXACT_MULTI_FAMILIES, representation["exact_multi"])
        self.assertEqual(
            common.CANDIDATE_EXACT_MULTI_RELATIONS,
            representation["exact_multi_relations"],
        )

    def test_prior_locks_are_excluded(self) -> None:
        closed = common.prior_locked_family_ids()
        overlap = closed.intersection(self.document["family_ids"])
        self.assertEqual(set(), overlap)

    def test_opening_is_t47_closing(self) -> None:
        opening = self.document["opening"]
        self.assertEqual(common.OPENING_EXECUTION_GAP, opening["execution_gap"])
        self.assertEqual(common.OPENING_AUTHORED_ENTRIES, opening["authored_entries"])
        self.assertEqual(common.OPENING_EAGER_ROWS, opening["eager_rows"])
        self.assertEqual(common.OPENING_LAZY_ROWS, opening["lazy_rows"])
        self.assertEqual(common.OPENING_CACHE_CEILING_ROWS, opening["cache_ceiling_rows"])
