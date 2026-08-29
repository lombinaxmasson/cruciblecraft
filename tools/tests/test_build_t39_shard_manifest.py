#!/usr/bin/env python3
"""Regression tests for T39 publication-group and shard builders."""
from __future__ import annotations

import unittest

import json

from tools import t39_common as common
from tools import t39_shard_router as router
from tools.build_t39_publication_group_manifest import build as build_groups
from tools.build_t39_shard_manifest import build as build_shards


class T39ManifestTests(unittest.TestCase):
    def test_publication_groups_match_frozen_denominators(self) -> None:
        document = build_groups()
        self.assertEqual("T39_PRODUCTION_PUBLICATION_GROUPS_FROZEN", document["status"])
        self.assertEqual(2, document["group_count"])
        singleton, multi = document["groups"]
        counts = common.production_group_counts()
        self.assertEqual(
            counts[common.SINGLETON_GROUP]["families"],
            singleton["family_count"],
        )
        self.assertEqual(
            counts[common.SINGLETON_GROUP]["relations"],
            singleton["relation_count"],
        )
        self.assertEqual(
            counts[common.MULTI_GROUP]["families"],
            multi["family_count"],
        )
        self.assertEqual(
            counts[common.MULTI_GROUP]["relations"],
            multi["relation_count"],
        )

    def test_shards_stay_under_hard_ceiling(self) -> None:
        document = build_shards()
        self.assertEqual("T39_PRODUCTION_SHARDS_FROZEN", document["status"])
        self.assertEqual(common.production_relation_count(), document["relation_count"])
        self.assertEqual(router.HARD_SHARD_CEILING, document["hard_ceiling"])
        self.assertGreaterEqual(document["shard_count"], 1)
        self.assertLessEqual(document["shard_count"], common.production_relation_count())
        for group in document["groups"]:
            self.assertEqual(0, group["overflow_count"])
            self.assertLessEqual(group["worst_shard_size"], router.HARD_SHARD_CEILING)
            self.assertEqual(
                group["relation_count"],
                sum(shard["relation_count"] for shard in group["shards"]),
            )

    def test_python_router_shard_ids_match_committed_manifest(self) -> None:
        if not common.SHARD_MANIFEST.is_file():
            self.skipTest("T39 shard manifest is not committed")
        manifest = json.loads(common.SHARD_MANIFEST.read_text(encoding="utf-8"))
        live = build_shards()
        self.assertEqual(manifest["shard_count"], live["shard_count"])
        for manifest_group, live_group in zip(
                manifest["groups"],
                live["groups"],
                strict=True,
        ):
            self.assertEqual(
                manifest_group["publication_group"],
                live_group["publication_group"],
            )
            self.assertEqual(
                manifest_group["relations"],
                live_group["relations"],
            )
            self.assertEqual(manifest_group["shards"], live_group["shards"])


if __name__ == "__main__":
    unittest.main()
