"""Contract tests for the T43 production lock and shard/catalog symmetry."""
from __future__ import annotations

import unittest

from tools import build_t43_candidate_selection as candidate
from tools import build_t43_production_lock as lock
from tools import build_t43_shard_manifest as shard
from tools import t43_common as common


class T43ProductionLockTest(unittest.TestCase):
    def test_lock_matches_candidate_hash_and_is_407_singletons(self) -> None:
        document = lock.build()
        production = document["production"]
        self.assertEqual("T43_PRODUCTION_LOCKED", document["status"])
        self.assertEqual(407, production["family_count"])
        self.assertEqual(407, production["relation_count"])
        self.assertEqual(common.EXPECTED_SELECTION_SHA256, production["selection_sha256"])
        self.assertEqual(
            common.EXPECTED_SELECTION_SHA256,
            document["candidate_snapshot"]["selection_sha256"],
        )
        self.assertEqual(common.EXPECTED_SELECTION_SHA256, candidate.build()["selection_sha256"])
        self.assertEqual(407, len(set(production["stable_ids"])))
        self.assertEqual(406, document["support"]["route_count"])
        self.assertTrue(all(row["representation"] == "singleton" for row in production["families"]))
        self.assertTrue(
            all(row["publication_group"] == common.STONE_GROUP for row in production["families"])
        )
        mapped = [row["mapped_runtime_id"] for row in production["families"]]
        self.assertEqual(len(mapped), len(set(mapped)))
        noteblock = next(
            row for row in production["families"] if row["cohort"] == "B"
        )
        self.assertEqual("minecraft:note_block", noteblock["mapped_runtime_id"])
        self.assertEqual("b0_ready", noteblock["b0_status"])

    def test_generated_shard_worst_and_overflow_stay_inside_ceiling(self) -> None:
        if not common.GENERATED_ROOT.is_dir():
            self.skipTest("generated T43 recipes are not on disk")
        document = shard.build()
        group = document["groups"][0]
        self.assertEqual("t39-shard-v1", group["routing_schema_version"])
        self.assertLessEqual(group["worst_shard_size"], 128)
        self.assertLessEqual(group["overflow_count"], 128)
        self.assertEqual(407, group["relation_count"])
