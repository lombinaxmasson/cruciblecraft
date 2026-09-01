#!/usr/bin/env python3
"""Contract tests for the T47 production lock."""
from __future__ import annotations

import unittest

from tools import build_t47_production_lock as lock
from tools import t41_shard_router as router
from tools import t47_common as common


class T47ProductionLockTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.document = lock.build()
        cls.production = cls.document["production"]

    def test_lock_is_complete_cohort_at_or_above_floor(self) -> None:
        self.assertEqual("T47_PRODUCTION_LOCKED", self.document["status"])
        self.assertGreaterEqual(
            self.production["family_count"],
            common.MIN_PRODUCTION_FAMILIES,
        )
        self.assertEqual(self.production["family_count"], len(self.production["families"]))
        self.assertEqual(self.production["relation_count"], len(self.production["stable_ids"]))
        self.assertEqual(
            self.production["relation_count"],
            len(set(self.production["stable_ids"])),
        )
        self.assertEqual(0, self.document["partial_family_count"])
        self.assertEqual([], self.document["phase_deferred"])
        self.assertEqual(
            "PRODUCTION_COMPLETE_COHORT_SUBSET",
            self.document["catalog_fixture"]["status"],
        )

    def test_lock_stays_inside_remainder_and_out_of_t46(self) -> None:
        locked = set(self.production["family_ids"])
        self.assertTrue(locked.isdisjoint(common.t46_closed_family_ids()))
        hosts = {row["host"] for row in self.production["families"]}
        self.assertEqual({common.HOST}, hosts)
        groups = {row["publication_group"] for row in self.production["families"]}
        self.assertEqual(set(common.PUBLICATION_GROUPS), groups)
        self.assertNotIn(common.T46_PUBLICATION_GROUP, groups)
        self.assertNotIn(common.CANDIDATE_PUBLICATION_GROUP, groups)
        representations = {row["representation"] for row in self.production["families"]}
        self.assertEqual({"exact", "exact_multi"}, representations)
        self.assertEqual(
            self.production["exact_families"],
            sum(1 for row in self.production["families"] if row["representation"] == "exact"),
        )
        self.assertEqual(
            self.production["exact_multi_families"],
            sum(
                1
                for row in self.production["families"]
                if row["representation"] == "exact_multi"
            ),
        )

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

    def test_opening_pins_t46_closing(self) -> None:
        opening = self.document["opening"]
        self.assertEqual(common.OPENING_EXECUTION_GAP, opening["execution_gap"])
        self.assertEqual(common.OPENING_AUTHORED_ENTRIES, opening["authored_entries"])
        self.assertEqual(common.OPENING_EAGER_ROWS, opening["eager_rows"])
        self.assertEqual(common.OPENING_LAZY_ROWS, opening["lazy_rows"])
        self.assertEqual(common.OPENING_CACHE_CEILING_ROWS, opening["cache_ceiling_rows"])

    def test_selection_hash_is_canonical(self) -> None:
        self.assertEqual(
            common.selection_sha256(self.production["family_ids"]),
            self.production["selection_sha256"],
        )
        self.assertEqual(
            len(self.production["source_row_sha256"]),
            self.production["relation_count"],
        )
        support = self.document["support"]
        if common.player_path_real():
            self.assertEqual(common.PLAYER_PATH_REAL_KIND, support["kind"])
            self.assertGreater(support["route_count"], 0)
            self.assertEqual(support["route_count"], len(support["route_keys"]))
            self.assertNotEqual(common.EMPTY_TREE_SHA256, support["tree_sha256"])
        else:
            self.assertEqual(0, support["route_count"])
            self.assertEqual("pending_layered_b1", support["kind"])

    def test_mapped_runtimes_are_registered_1_21_ids(self) -> None:
        from tools import t47_identities as identities

        retired = set(identities.VANILLA_RENAMES)
        for family in self.production["families"]:
            self.assertNotIn(str(family.get("mapped_runtime_id") or ""), retired)


if __name__ == "__main__":
    unittest.main()
