#!/usr/bin/env python3
"""GT trees: nine WorldgenTree* features, source art, unique-active landing."""
from __future__ import annotations

import unittest

from tools import census_common as census
from tools import gt_trees as trees
from tools.build_recipe_bulk import WAVE_CHOICES
from tools.recipe_bulk.slugs import KNOWN_SEMANTIC_SLUGS
from tools.recipe_bulk.waves import SEMANTIC_COMPILE_ORDER, recipe_wave

ROOT = census.ROOT
JAVA = ROOT / "src" / "main" / "java" / "com" / "masson" / "cruciblecraft"
TEST_JAVA = ROOT / "src" / "test" / "java" / "com" / "masson" / "cruciblecraft"
LEDGER = ROOT / "tools" / "blocked_recipe_ledger.json"


class GtTreesPrepTest(unittest.TestCase):
    def test_prep_check_passes(self) -> None:
        self.assertEqual([], trees.check())

    def test_denominator_is_nine_worldgen_tree_features(self) -> None:
        document = census.load_json(trees.WAVE / "denominator.json")
        self.assertEqual(9, document["feature_count"])
        self.assertEqual(3, document["tree_hole_count"])
        self.assertIsNone(document["unique_active_wave"])
        names = [row["feature"] for row in document["features"]]
        self.assertEqual(
            [
                "tree.rubber",
                "tree.maple",
                "tree.willow",
                "tree.bluemahoe",
                "tree.hazel",
                "tree.cinnamon",
                "tree.coconut",
                "tree.rainbowood",
                "tree.bluespruce",
            ],
            names,
        )
        rubber = document["features"][0]
        self.assertEqual("grow_rubber", rubber["hole_mode"])
        self.assertEqual(32762, rubber["hole_source_id"])
        maple = document["features"][1]
        self.assertEqual("drill_maple", maple["hole_mode"])
        rainbow = document["features"][7]
        self.assertEqual("drill_rainbowood", rainbow["hole_mode"])

    def test_biome_map_stays_overworld_and_rainbow_is_rare(self) -> None:
        document = census.load_json(trees.WAVE / "biome_map.json")
        self.assertEqual(8192, document["rainbowood_rare_chance"])
        by_id = {row["id"]: row for row in document["trees"]}
        self.assertEqual(["minecraft:beach"], by_id["coconut"]["overworld_biomes"])
        self.assertEqual([], by_id["rainbowood"]["overworld_biomes"])
        self.assertIn("minecraft:taiga", by_id["rubber"]["overworld_biomes"])
        blob = str(document)
        self.assertNotIn("twilight", blob)
        self.assertNotIn("erebus", blob)

    def test_holes_do_not_stand_in_fluids(self) -> None:
        holes = census.load_json(trees.WAVE / "tree_holes.json")
        blocked = census.load_json(trees.WAVE / "blocked.json")
        by_id = {row["source_id"]: row for row in holes["holes"]}
        self.assertEqual("new_distinct", by_id[32762]["fluid_status"])
        self.assertEqual("explicitly_blocked", by_id[32761]["fluid_status"])
        self.assertEqual("reuse_canonical", by_id[32760]["fluid_status"])
        self.assertEqual("cruciblecraft:rainbow_sap", by_id[32760]["cc_fluid"])
        self.assertEqual("cruciblecraft:rubber_tree_sap", by_id[32762]["cc_fluid"])
        self.assertIn("latex is not a stand-in", by_id[32762]["fluid_reason"])
        reasons = str(blocked)
        self.assertIn("slime_ball_to_rubber_plate", reasons)
        self.assertIn("explicitly_blocked", reasons)
        self.assertNotIn("programmed_circuit", reasons)

    def test_art_is_copied_from_gregtech6_w_and_logs_are_already_present(self) -> None:
        manifest = census.load_json(trees.ART_MANIFEST)
        self.assertGreaterEqual(len(manifest["imports"]), 40)
        copied = [row for row in manifest["imports"] if row.get("status") != "already_present"]
        present = [row for row in manifest["imports"] if row.get("status") == "already_present"]
        self.assertEqual(18, len(present))
        destinations = [row["destination"] for row in copied]
        self.assertIn("assets/cruciblecraft/textures/item/tree/rubber_resin.png", destinations)
        self.assertIn(
            "assets/cruciblecraft/textures/block/tree/rubber/sapling.png",
            destinations,
        )
        self.assertTrue(
            all("/tree/" in row["destination"] for row in copied),
            copied,
        )
        for row in manifest["imports"]:
            self.assertNotIn("multiblock_casing", row["destination"])
            self.assertNotIn("pipe_filter_cover", row["destination"])

    def test_live_compile_stays_closed(self) -> None:
        self.assertNotIn("prep/gt-trees", SEMANTIC_COMPILE_ORDER)
        self.assertNotIn("prep/gt-trees", WAVE_CHOICES)
        self.assertNotIn("prep/gt-trees", KNOWN_SEMANTIC_SLUGS)
        self.assertNotIn("worldgen/gt-trees", KNOWN_SEMANTIC_SLUGS)
        with self.assertRaises((ValueError, KeyError)):
            recipe_wave("prep/gt-trees")

    def test_landing_registers_trees(self) -> None:
        for path, needles in (
            (trees.LANDING_JAVA[0], ("GtTreeSpecies", "GtTreeSaplingBlock")),
            (trees.LANDING_JAVA[1], ("GtTreeSpecies", "tree/rubber_resin")),
            (trees.LANDING_JAVA[2], ("GtTreeHoleBlockEntity",)),
            (trees.LANDING_JAVA[3], ("GtTreeFeature", "GtTreeGrower")),
        ):
            text = path.read_text(encoding="utf-8")
            for needle in needles:
                self.assertIn(needle, text, path.name)
        processing = (JAVA / "registry" / "ModProcessingMachines.java").read_text(
            encoding="utf-8"
        )
        self.assertNotIn("GtTreeGrower", processing)
        self.assertTrue(
            (
                ROOT
                / "src"
                / "main"
                / "resources"
                / "data"
                / "cruciblecraft"
                / "worldgen"
                / "configured_feature"
                / "tree_rubber.json"
            ).is_file()
        )
        grower = JAVA / "worldgen" / "tree" / "prep" / "GtTreeGrower.java"
        self.assertIn("package com.masson.cruciblecraft.worldgen.tree.prep", grower.read_text(encoding="utf-8"))
        self.assertTrue(
            (ROOT / "src" / "test" / "java" / "com" / "masson" / "cruciblecraft" / "worldgen" / "tree" / "prep" / "GtTreeGrowerTest.java").is_file()
        )
        self.assertTrue(
            (TEST_JAVA / "gametest" / "GtTreesGameTests.java").is_file()
        )


if __name__ == "__main__":
    unittest.main()
