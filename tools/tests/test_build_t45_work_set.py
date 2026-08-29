#!/usr/bin/env python3
"""Contract tests for T45 R0 freeze artifacts."""
from __future__ import annotations

import unittest

from tools import build_t45_work_set as work_set
from tools import t45_common as common


class T45WorkSetTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.document = work_set.build()

    def test_work_set_is_cross_host_block_object_and_above_floor(self) -> None:
        self.assertGreaterEqual(self.document["family_count"], common.MIN_PRODUCTION_FAMILIES)
        self.assertEqual("T45", self.document["unique_active_card"])
        self.assertEqual("T45_WORK_SET_FROZEN", self.document["status"])
        self.assertNotIn("cruciblecraft:centrifuge", self.document["hosts"])
        self.assertEqual({"1": self.document["family_count"]}, self.document["expanded_count_distribution"])
        templates = set(self.document["template_keys"])
        self.assertNotIn("gt.recipe.centrifuge#0085", templates)

    def test_selection_hash_is_canonical(self) -> None:
        self.assertEqual(
            common.selection_sha256(self.document["family_ids"]),
            self.document["selection_sha256"],
        )
