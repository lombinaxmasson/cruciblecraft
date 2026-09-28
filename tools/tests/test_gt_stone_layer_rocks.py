#!/usr/bin/env python3
"""GT6 WorldgenStoneLayers overworld pebbles."""
from __future__ import annotations

import unittest

from tools import census_common as census
from tools import gt_stone_layer_rocks as rocks

ROOT = census.ROOT
JAVA = ROOT / "src" / "main" / "java" / "com" / "masson" / "cruciblecraft"
TEST_JAVA = ROOT / "src" / "test" / "java" / "com" / "masson" / "cruciblecraft"


class GtStoneLayerRocksTest(unittest.TestCase):
    def test_builder_check_passes(self) -> None:
        self.assertEqual([], rocks.check())

    def test_denominator_is_overworld_32757_layers(self) -> None:
        document = census.load_json(rocks.WAVE / "denominator.json")
        self.assertEqual(128, document["probability"])
        self.assertEqual(32757, document["mte"])
        self.assertEqual("cruciblecraft:gt_surface_rock", document["placer"])
        self.assertEqual(rocks.LAYER_COUNT, document["layer_count"])
        self.assertEqual(rocks.ROCK_ORE_COUNT, document["rock_ore_count"])
        self.assertEqual(1, document["nether_rock_ore_count"])
        self.assertEqual(rocks.STONE_BLOCK_COUNT, document["stone_block_count"])
        self.assertGreaterEqual(document["material_counts"]["granite_black"], 1)
        self.assertNotIn("moon", str(document))

    def test_capability_does_not_alias_planet_rocks_or_catalog_dump(self) -> None:
        capability = census.load_json(
            ROOT / "tools" / "capabilities" / rocks.SLUG / "capability.json"
        )
        self.assertEqual(rocks.SLUG, capability["slug"])
        self.assertEqual("runtime_ready", capability["maturity"])
        self.assertIn(capability["workflow"], {"active", "accepted"})
        identity = capability["identity_disposition"][0]
        self.assertEqual(
            "worldgen:overworld.stonelayers.rocks", identity["semantic_key"]
        )
        blob = str(capability)
        self.assertNotIn("moon.rocks", blob)
        self.assertNotIn("programmed_circuit", blob)
        self.assertNotIn("GtItemScatterFeature", blob)
        self.assertIn("coal/dense_ore", blob)
        self.assertIn("nether_netherquartz", blob)
        self.assertIn("nether_quartz/dense_ore", blob)

    def test_layers_match_loader_worldgen_no_mod_weights(self) -> None:
        loader = rocks.extract_layers()
        dense = rocks.extract_rock_ores()
        self.assertEqual(rocks.LOADER_LAYER_COUNT, len(loader))
        self.assertEqual(rocks.ROCK_ORE_COUNT, len(dense))
        catalog = census.load_json(rocks.CATALOG)
        self.assertEqual(
            [row["material"] for row in dense + loader],
            [row["material"] for row in catalog["layers"]],
        )
        self.assertEqual(rocks.LAYER_COUNT, catalog["layer_count"])
        self.assertEqual(rocks.UNIT, catalog["unit"])
        self.assertGreater(sum(len(row["ores"]) for row in catalog["layers"]), 0)
        self.assertGreater(len(catalog["deepslate_layer"]["ores"]), 0)
        self.assertGreater(len(catalog["boundaries"]), 0)
        self.assertEqual(8, catalog["rock_ore_count"])
        self.assertEqual(9, len(catalog["rock_ores"]))
        self.assertEqual("nether_quartz", catalog["rock_ores"][-1]["material"])
        self.assertNotIn(
            "nether_quartz",
            {row["material"] for row in catalog["layers"]},
        )
        self.assertEqual("coal", catalog["layers"][0]["material"])
        self.assertEqual(
            11,
            sum(1 for row in catalog["layers"] if row["no_deep"]),
        )
        self.assertIn("granite_black", {row["material"] for row in loader})

    def test_java_replaces_stone_and_does_not_dump_catalog(self) -> None:
        feature = (JAVA / "worldgen" / "StoneLayerRockFeature.java").read_text(
            encoding="utf-8"
        )
        self.assertNotIn("c:rocks", feature)
        self.assertNotIn("instanceof RockBlock", feature)
        self.assertIn("GT_SURFACE_ROCK", feature)
        self.assertIn("tryReplace", feature)
        self.assertIn("tryReplaceVillageBrick", feature)
        self.assertIn("StructureTags.VILLAGE", feature)
        self.assertIn("tryPlaceOre", feature)
        self.assertIn("minBuildHeight", feature)
        self.assertIn("scan[6] == scan[0]", feature)
        self.assertIn("Blocks.TUFF", feature)
        self.assertIn("nextInt", feature)
        cube = (JAVA / "content" / "block" / "StoneLayerStoneBlock.java").read_text(
            encoding="utf-8"
        )
        self.assertIn("canEntityDestroy", cube)
        tests = (TEST_JAVA / "gametest" / rocks.GAME_TESTS).read_text(
            encoding="utf-8"
        )
        self.assertIn("cruciblecraft_worldgen", tests)
        self.assertIn("granite_black", tests)
        self.assertIn("stoneLayerReplacesVanillaStone", tests)
        self.assertIn("stoneLayerPlacesLayerOres", tests)
        self.assertIn("stoneLayerPlacesDenseRockOres", tests)
        self.assertIn("stoneLayerPlacesNetherQuartz", tests)
        self.assertIn("stoneLayerManifestResolvesLocalGt6", tests)
        self.assertIn("stoneLayerDoesNotEatVillageCobble", tests)
        self.assertIn("stoneLayerCubeItemModelsExist", tests)
        self.assertIn("stoneLayerDoesNotDumpCatalog", tests)
        self.assertIn("MaterialPrefixes.RAW_ORE", tests)
        self.assertIn("remove_overworld_large_veins", tests)
        rock_ore = (JAVA / "content" / "block" / "StoneLayerRockOreBlock.java").read_text(
            encoding="utf-8"
        )
        self.assertIn("spawnAfterBreak", rock_ore)
        nether = (JAVA / "worldgen" / "NetherQuartzLayerFeature.java").read_text(
            encoding="utf-8"
        )
        self.assertIn("Blocks.NETHERRACK", nether)
        self.assertIn("nether_quartz", nether)
        self.assertNotIn("c:rocks", nether)
        generated = ROOT / "src/generated/resources"
        self.assertTrue(
            (generated / "assets/cruciblecraft/blockstates/coal/dense_ore.json").is_file()
        )
        loot = (
            generated / "data/cruciblecraft/loot_table/blocks/coal/dense_ore.json"
        ).read_text(encoding="utf-8")
        self.assertIn('"name": "cruciblecraft:raw_ore"', loot)
        self.assertIn('"cruciblecraft:prefix_material": "coal"', loot)
        quartz_loot = (
            generated
            / "data/cruciblecraft/loot_table/blocks/nether_quartz/dense_ore.json"
        ).read_text(encoding="utf-8")
        self.assertIn('"name": "cruciblecraft:raw_ore"', quartz_loot)
        self.assertIn(
            '"cruciblecraft:prefix_material": "nether_quartz"', quartz_loot
        )

    def test_art_copies_gt6_stone_cubes(self) -> None:
        self.assertEqual(
            rocks.STONE_BLOCK_COUNT
            + rocks.ROCK_ORE_COUNT
            + 1
            + rocks.VILLAGE_BRICK_COUNT,
            len(rocks.art_imports()),
        )
        self.assertTrue(
            any(
                "gt.stone.granite.black/stone.png" in row["destination"]
                for row in rocks.art_imports()
            )
        )
        self.assertTrue(
            any(
                "gt.stone.andesite/small_bricks.png" in row["destination"]
                for row in rocks.art_imports()
            )
        )
        self.assertTrue(
            any(
                "ore_anthracite.png" in row["destination"]
                for row in rocks.art_imports()
            )
        )
        self.assertTrue(
            any(
                "ore_netherquartz.png" in row["destination"]
                for row in rocks.art_imports()
            )
        )
        cubes = rocks.stone_blocks()
        self.assertEqual(rocks.STONE_BLOCK_COUNT, len(cubes))
        self.assertEqual("granite_black/stone", cubes[0]["registry_path"])
        bricks = rocks.village_brick_blocks()
        self.assertEqual(rocks.VILLAGE_BRICK_COUNT, len(bricks))
        self.assertEqual("andesite", bricks[7]["material"])
        self.assertEqual("andesite/small_bricks", bricks[7]["registry_path"])
        catalog = census.load_json(
            ROOT / "src/main/resources/data/cruciblecraft/gt_stone_catalog.json"
        )
        existing: set[str] = set()
        for identity in catalog.get("identities") or []:
            for variant in identity.get("variants") or []:
                existing.add(variant["registry_path"])
        self.assertIn("marble/stone", existing)
        self.assertNotIn("granite_black/stone", existing)
