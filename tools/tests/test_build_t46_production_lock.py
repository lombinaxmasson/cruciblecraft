#!/usr/bin/env python3
"""Contract tests for the T46 production lock."""
from __future__ import annotations

import unittest

from tools import build_t46_production_lock as lock
from tools import t46_common as common


class T46ProductionLockTest(unittest.TestCase):
    def test_lock_is_803_1517_bath_mte(self) -> None:
        document = lock.build()
        production = document["production"]
        self.assertEqual("T46_PRODUCTION_LOCKED", document["status"])
        self.assertEqual(common.EXPECTED_FAMILY_COUNT, production["family_count"])
        self.assertEqual(common.EXPECTED_RELATION_COUNT, production["relation_count"])
        self.assertEqual(common.EXPECTED_FAMILY_COUNT, len(production["families"]))
        self.assertEqual(
            common.EXPECTED_RELATION_COUNT,
            len(set(production["stable_ids"])),
        )
        hosts = {row["host"] for row in production["families"]}
        self.assertEqual({common.HOST}, hosts)
        representations = {row["representation"] for row in production["families"]}
        self.assertEqual({"exact", "exact_multi"}, representations)
        groups = {row["publication_group"] for row in production["families"]}
        self.assertEqual({common.PUBLICATION_GROUP}, groups)
        self.assertGreaterEqual(document["support"]["route_count"], 1)
