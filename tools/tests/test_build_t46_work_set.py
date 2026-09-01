#!/usr/bin/env python3
"""Contract tests for T46 work-set freeze."""
from __future__ import annotations

import unittest

from tools import build_t46_work_set as work_set
from tools import t46_common as common


class T46WorkSetTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.document = work_set.build()

    def test_work_set_is_bath_mte_exact_relation_lock_size(self) -> None:
        self.assertEqual(common.EXPECTED_FAMILY_COUNT, self.document["family_count"])
        self.assertEqual("T46", self.document["unique_active_card"])
        self.assertEqual("T46_WORK_SET_FROZEN", self.document["status"])
        self.assertEqual(common.HOST, self.document["host"])
        self.assertGreaterEqual(self.document["family_count"], common.MIN_PRODUCTION_FAMILIES)

    def test_selection_hash_is_canonical(self) -> None:
        self.assertEqual(
            common.selection_sha256(self.document["family_ids"]),
            self.document["selection_sha256"],
        )
