#!/usr/bin/env python3
"""Contract tests for T48 B0/B1/B2 player-path proof."""
from __future__ import annotations

import unittest

from tools import build_t48_layered_player_path as layered
from tools import build_t48_player_path as player_path
from tools import build_t48_player_path_support as support
from tools import t48_common as common


class T48PlayerPathTest(unittest.TestCase):
    def test_player_path_covers_locked_relations(self) -> None:
        document = player_path.build()
        self.assertEqual("T48_PLAYER_PATH", document["status"])
        self.assertEqual(common.production_relation_count(), document["relations"])
        self.assertEqual(document["relations"], document["inputs_reachable"])
        self.assertEqual(document["relations"], document["outputs_registered"])

    def test_support_is_real_layered_b1(self) -> None:
        document = support.build()
        self.assertEqual(common.PLAYER_PATH_REAL_KIND, document["kind"])
        self.assertGreater(document["route_count"], 0)
        self.assertEqual(
            2,
            sum(1 for row in document["routes"] if row["kind"] == "bath_gt_recipe"),
        )
        self.assertTrue(any(row["kind"] == "worldgen_drop" for row in document["routes"]))
        for row in document["routes"]:
            self.assertNotIn(row["kind"], {"b1_declaration", "declared_support", "b1_support"})
        self.assertEqual(2, len(common.support_recipe_files()))

    def test_layered_path_closes_without_declaration(self) -> None:
        if not common.PLAYER_PATH.is_file() or not common.PLAYER_PATH_SUPPORT.is_file():
            self.skipTest("player-path sidecars are not frozen yet")
        if (common.load_production_lock().get("support") or {}).get("kind") != (
            common.PLAYER_PATH_REAL_KIND
        ):
            self.skipTest("production lock support is still pending")
        document = layered.build()
        self.assertEqual("T48_LAYERED_PLAYER_PATH", document["status"])
        self.assertEqual(
            common.load_json(common.PLAYER_PATH_SUPPORT)["route_count"],
            document["b1"]["route_count"],
        )
        self.assertEqual(common.production_relation_count(), document["b2"]["relation_count"])


if __name__ == "__main__":
    unittest.main()
