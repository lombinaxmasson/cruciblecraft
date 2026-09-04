#!/usr/bin/env python3
"""Global remainder R0 and operand-foundation closeout."""
from __future__ import annotations

import unittest

from tools.recipe_bulk import ordinary_r0 as r0


class OrdinaryRemainderFoundationTest(unittest.TestCase):
    def test_global_r0_is_334_by_2047(self) -> None:
        document = r0.global_remainder_r0()
        self.assertEqual([], document["errors"])
        self.assertEqual(334, document["family_total"])
        self.assertEqual(2047, document["relation_total"])
        self.assertEqual(0, r0.remaining_summary("cruciblecraft:smelter")["family_count"])
        self.assertEqual(0, r0.remaining_summary("cruciblecraft:mixer")["family_count"])
        self.assertEqual(
            44, document["hosts"]["cruciblecraft:drying"]["family_count"]
        )

    def test_closed_ordinary_locks_are_subtracted(self) -> None:
        closed = r0.closed_ordinary_family_ids()
        self.assertTrue(closed)
        for family_id in r0.remaining_owner_rows("cruciblecraft:drying"):
            self.assertNotIn(family_id["family_id"], closed)

    def test_log_wildcard_uses_minecraft_logs_tag(self) -> None:
        from tools.recipe_bulk.ordinary_source import ORDINARY_VANILLA_WILDCARDS

        self.assertEqual(
            ("cruciblecraft:gt6_legacy_log", "minecraft:oak_log"),
            ORDINARY_VANILLA_WILDCARDS["minecraft:log"],
        )
        self.assertEqual(
            ("cruciblecraft:gt6_legacy_log2", "minecraft:acacia_log"),
            ORDINARY_VANILLA_WILDCARDS["minecraft:log2"],
        )

    def test_tallgrass_metas_split_to_distinct_1_21_items(self) -> None:
        from tools.recipe_bulk.ordinary_source import ORDINARY_VANILLA_SPECIAL

        self.assertEqual(
            "minecraft:short_grass",
            ORDINARY_VANILLA_SPECIAL[("minecraft:tallgrass", 1)],
        )
        self.assertEqual(
            "minecraft:fern",
            ORDINARY_VANILLA_SPECIAL[("minecraft:tallgrass", 2)],
        )
        self.assertNotEqual(
            ORDINARY_VANILLA_SPECIAL[("minecraft:tallgrass", 1)],
            ORDINARY_VANILLA_SPECIAL[("minecraft:tallgrass", 2)],
        )

    def test_foundation_catalog_does_not_overlay_meta_dust(self) -> None:
        from tools.recipe_bulk.ordinary_source import load_semantic_object_overlay

        overlay = load_semantic_object_overlay()
        self.assertFalse(
            any(item == "gregtech:gt.meta.dust" for item, _meta in overlay),
            "foundation diagnostic gt_object dusts must not enter the emit overlay",
        )

    def test_foundation_closing_has_no_completion(self) -> None:
        from tools import census_common as census

        path = census.TOOLS / "waves" / "ordinary-remainder" / "operand-foundation" / "census_delta.json"
        if not path.is_file():
            self.skipTest("foundation census not written yet")
        census = census.load_json(path)
        self.assertEqual(0, int(census["complete_family_count"]))
        self.assertEqual(334, int(census["remaining_recipe_gap"]))
        self.assertEqual(44, int((census.get("drying_candidate") or {}).get("family_count") or 0))


if __name__ == "__main__":
    unittest.main()
