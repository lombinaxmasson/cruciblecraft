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

    def test_layers_match_loader_worldgen_no_mod_weights(self) -> None:
        layers = rocks.extract_layers()
        self.assertEqual(rocks.LAYER_COUNT, len(layers))
        catalog = census.load_json(rocks.CATALOG)
        self.assertEqual(
            [row["material"] for row in layers],
            [row["material"] for row in catalog["layers"]],
        )
        self.assertEqual(3, sum(1 for row in layers if row["no_deep"]))
        self.assertIn("granite_black", {row["material"] for row in layers})

    def test_java_replaces_stone_and_does_not_dump_catalog(self) -> None:
        feature = (JAVA / "worldgen" / "StoneLayerRockFeature.java").read_text(
            encoding="utf-8"
        )
        self.assertNotIn("c:rocks", feature)
        self.assertNotIn("instanceof RockBlock", feature)
        self.assertIn("GT_SURFACE_ROCK", feature)
        self.assertIn("tryReplace", feature)
        self.assertIn("Blocks.TUFF", feature)
        self.assertIn("nextInt", feature)
        cube = (JAVA / "content" / "block" / "StoneLayerStoneBlock.java").read_text(
            encoding="utf-8"
        )
        self.assertIn("canEntityDestroy", cube)
        tests = (TEST_JAVA / "gametest" / rocks.GAME_TESTS).read_text(
            encoding="utf-8"
        )
        self.assertIn("cruciblecraft_wave_worldgen_gt_stone_layer_rocks", tests)
        self.assertIn("granite_black", tests)
        self.assertIn("stoneLayerReplacesVanillaStone", tests)
        self.assertIn("stoneLayerManifestResolvesLocalGt6", tests)
        self.assertIn("stoneLayerDoesNotDumpCatalog", tests)

    def test_art_copies_gt6_stone_cubes(self) -> None:
        self.assertEqual(rocks.STONE_BLOCK_COUNT, len(rocks.art_imports()))
        self.assertTrue(
            any(
                "gt.stone.granite.black/stone.png" in row["destination"]
                for row in rocks.art_imports()
            )
        )
        cubes = rocks.stone_blocks()
        self.assertEqual(rocks.STONE_BLOCK_COUNT, len(cubes))
        self.assertEqual("granite_black/stone", cubes[0]["registry_path"])
        catalog = census.load_json(
            ROOT / "src/main/resources/data/cruciblecraft/gt_stone_catalog.json"
        )
        existing: set[str] = set()
        for identity in catalog.get("identities") or []:
            for variant in identity.get("variants") or []:
                existing.add(variant["registry_path"])
        self.assertIn("marble/stone", existing)
        self.assertNotIn("granite_black/stone", existing)
