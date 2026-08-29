#!/usr/bin/env python3
"""Regression tests for T40 publication-group and shard builders."""
from __future__ import annotations

import json
import unittest

from tools import t40_common as common
from tools import t40_shard_router as router
from tools.build_t40_publication_group_manifest import build as build_groups
from tools.build_t40_shard_manifest import build as build_shards


class T40ManifestTests(unittest.TestCase):
    def test_publication_groups_match_frozen_lock(self) -> None:
        document = build_groups()
        self.assertEqual("T40_PRODUCTION_PUBLICATION_GROUPS_FROZEN", document["status"])
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
        groups = {row["publication_group"] for row in document["groups"]}
        self.assertNotIn(common.COMBINATORIAL_GROUP, groups)

    def test_shards_stay_under_hard_ceiling(self) -> None:
        document = build_shards()
        self.assertEqual("T40_PRODUCTION_SHARDS_FROZEN", document["status"])
        self.assertEqual(common.production_relation_count(), document["relation_count"])
        self.assertEqual(router.HARD_SHARD_CEILING, document["hard_ceiling"])
        self.assertLessEqual(document["shard_count"], common.production_relation_count())
        for group in document["groups"]:
            self.assertLessEqual(group["overflow_count"], router.HARD_SHARD_CEILING)
            self.assertLessEqual(group["worst_shard_size"], router.HARD_SHARD_CEILING)

    def test_python_router_shard_ids_match_committed_manifest(self) -> None:
        if not common.SHARD_MANIFEST.is_file():
            self.skipTest("T40 shard manifest is not committed")
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
            self.assertEqual(manifest_group["shards"], live_group["shards"])
