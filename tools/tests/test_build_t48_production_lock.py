#!/usr/bin/env python3
"""Contract tests for the T48 production lock."""
from __future__ import annotations

import unittest

from tools import build_t48_production_lock as lock
from tools import t41_shard_router as router
from tools import t48_common as common


class T48ProductionLockTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.document = lock.build()
        cls.production = cls.document["production"]

    def test_lock_is_complete_cohort_above_exception_floor(self) -> None:
        self.assertEqual("T48_PRODUCTION_LOCKED", self.document["status"])
        self.assertGreaterEqual(
            self.production["family_count"],
            common.MIN_PRODUCTION_FAMILIES,
        )
        self.assertEqual(145, self.production["family_count"])
        self.assertEqual(34091, self.production["relation_count"])
        self.assertEqual(self.production["family_count"], len(self.production["families"]))
        self.assertEqual(self.production["relation_count"], len(self.production["stable_ids"]))
        self.assertEqual(
            self.production["relation_count"],
            len(set(self.production["stable_ids"])),
        )
        self.assertEqual(0, self.document["partial_family_count"])
        self.assertEqual([], self.document["phase_deferred"])

    def test_lock_stays_inside_remainder_and_out_of_prior_waves(self) -> None:
        locked = set(self.production["family_ids"])
        self.assertTrue(locked.isdisjoint(common.prior_locked_family_ids()))
        hosts = {row["host"] for row in self.production["families"]}
        self.assertEqual({common.HOST}, hosts)
        groups = {row["publication_group"] for row in self.production["families"]}
        self.assertEqual(set(common.PUBLICATION_GROUPS), groups)
        self.assertNotIn(common.T46_PUBLICATION_GROUP, groups)
        self.assertNotIn(common.T47_PUBLICATION_GROUP_EXACT, groups)
        self.assertNotIn(common.CANDIDATE_PUBLICATION_GROUP, groups)
        representations = {row["representation"] for row in self.production["families"]}
        self.assertEqual({"exact", "exact_multi"}, representations)
        tool_head = [
            row
            for row in self.production["families"]
            if row["publication_group"] == common.PUBLICATION_GROUP_TOOL_HEAD
        ]
        self.assertEqual(36, len(tool_head))

    def test_router_dry_run_respects_hard_ceiling(self) -> None:
        dry_run = self.document["router_dry_run"]
        self.assertEqual("t39-shard-v1", dry_run["routing_schema_version"])
        self.assertEqual(router.HARD_SHARD_CEILING, dry_run["hard_ceiling"])
        self.assertTrue(dry_run["overflow_explicit"])
        self.assertEqual(len(common.PUBLICATION_GROUPS), len(dry_run["groups"]))
        for group in dry_run["groups"]:
            self.assertEqual(0, group["overflow_count"])
            self.assertLessEqual(group["worst_shard_size"], router.HARD_SHARD_CEILING)
            self.assertGreater(group["relation_count"], 0)

    def test_opening_pins_t47_closing(self) -> None:
        opening = self.document["opening"]
        self.assertEqual(common.OPENING_EXECUTION_GAP, opening["execution_gap"])
        self.assertEqual(common.OPENING_AUTHORED_ENTRIES, opening["authored_entries"])
        self.assertEqual(common.OPENING_EAGER_ROWS, opening["eager_rows"])
        self.assertEqual(common.OPENING_LAZY_ROWS, opening["lazy_rows"])
        self.assertEqual(common.OPENING_CACHE_CEILING_ROWS, opening["cache_ceiling_rows"])

    def test_n_less_than_300_exception_is_recorded(self) -> None:
        exception = self.document["size_exception"]
        self.assertEqual(common.EXCEPTION_KIND, exception["exception_kind"])
        self.assertEqual(1, exception["minimum_N"])
        self.assertIn("Mixer", exception["padding_hosts_forbidden"])


if __name__ == "__main__":
    unittest.main()
