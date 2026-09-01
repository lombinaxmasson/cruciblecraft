#!/usr/bin/env python3
"""Contract tests for T47 remainder work-set freeze."""
from __future__ import annotations

import unittest

from tools import build_t47_work_set as work_set
from tools import t47_common as common


class T47WorkSetTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.document = work_set.build()

    def test_work_set_is_bath_remainder_universe(self) -> None:
        self.assertEqual(common.CANDIDATE_FAMILY_COUNT, self.document["family_count"])
        self.assertEqual(common.CANDIDATE_RELATION_COUNT, self.document["relation_count"])
        self.assertEqual("T47", self.document["unique_active_card"])
        self.assertEqual("T47_WORK_SET_FROZEN", self.document["status"])
        self.assertEqual(common.HOST, self.document["host"])
        self.assertTrue(self.document["candidate_universe"])
        self.assertFalse(self.document["production_authority"])
        self.assertGreaterEqual(common.CANDIDATE_FAMILY_COUNT, common.MIN_PRODUCTION_FAMILIES)

    def test_representation_matches_remainder_replay(self) -> None:
        representation = self.document["representation"]
        self.assertEqual(common.CANDIDATE_EXACT_FAMILIES, representation["exact"])
        self.assertEqual(common.CANDIDATE_EXACT_MULTI_FAMILIES, representation["exact_multi"])
        self.assertEqual(
            common.CANDIDATE_EXACT_MULTI_RELATIONS,
            representation["exact_multi_relations"],
        )

    def test_selection_hash_is_canonical(self) -> None:
        self.assertEqual(
            common.selection_sha256(self.document["family_ids"]),
            self.document["selection_sha256"],
        )

    def test_t46_mte_families_are_excluded(self) -> None:
        closed = common.t46_closed_family_ids()
        overlap = closed.intersection(self.document["family_ids"])
        self.assertEqual(set(), overlap)

    def test_opening_is_t46_closing(self) -> None:
        opening = self.document["opening"]
        self.assertEqual(common.OPENING_EXECUTION_GAP, opening["execution_gap"])
        self.assertEqual(common.OPENING_AUTHORED_ENTRIES, opening["authored_entries"])
        self.assertEqual(common.OPENING_EAGER_ROWS, opening["eager_rows"])
        self.assertEqual(common.OPENING_LAZY_ROWS, opening["lazy_rows"])
