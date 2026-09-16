#!/usr/bin/env python3
"""GT6 WorldgenRocks overworld surface pebbles."""
from __future__ import annotations

import unittest

from tools import census_common as census
from tools import gt_surface_rocks as rocks

ROOT = census.ROOT
JAVA = ROOT / "src" / "main" / "java" / "com" / "masson" / "cruciblecraft"
TEST_JAVA = ROOT / "src" / "test" / "java" / "com" / "masson" / "cruciblecraft"


class GtSurfaceRocksTest(unittest.TestCase):
    def test_builder_check_passes(self) -> None:
        self.assertEqual([], rocks.check())

    def test_denominator_is_overworld_32757(self) -> None:
        document = census.load_json(rocks.WAVE / "denominator.json")
        self.assertEqual(2, document["amount"])
        self.assertEqual(3, document["probability"])
        self.assertEqual(32757, document["mte"])
        self.assertEqual("cruciblecraft:gt_surface_rock", document["placer"])
        self.assertEqual(rocks.WORLDGEN_LOOT, document["loot"])
        self.assertEqual(len(rocks.OVERWORLD_BIOMES), document["biome_count"])

    def test_capability_does_not_alias_planet_rocks_or_catalog_dump(self) -> None:
        capability = census.load_json(
            ROOT / "tools" / "capabilities" / rocks.SLUG / "capability.json"
        )
        self.assertEqual(rocks.SLUG, capability["slug"])
        self.assertEqual("runtime_ready", capability["maturity"])
        self.assertIn(capability["workflow"], {"active", "accepted"})
        identity = capability["identity_disposition"][0]
        self.assertEqual("worldgen:overworld.rocks", identity["semantic_key"])
        self.assertEqual("new_distinct", identity["disposition"])
        self.assertIn("cruciblecraft:gt_surface_rock", identity["runtime_ids"])
        blob = str(capability)
        self.assertNotIn("moon.rocks", blob)
        self.assertNotIn("programmed_circuit", blob)
        self.assertNotIn("GtItemScatterFeature", blob)

    def test_art_is_copied_from_gregtech6_w(self) -> None:
        manifest = census.load_json(rocks.ART_MANIFEST)
        self.assertEqual(rocks.art_imports(), manifest["imports"])
        self.assertEqual(2, len(manifest["imports"]))
        for row in manifest["imports"]:
            self.assertNotIn("multiblock_casing", row["destination"])
            self.assertNotIn("pipe_filter_cover", row["destination"])
            self.assertNotIn("conveyor_cover", row["destination"])
            dest = ROOT / "src" / "main" / "resources" / row["destination"]
            source = rocks.GT6_W / row["gt6_source"]
            self.assertEqual(source.read_bytes(), dest.read_bytes(), row["destination"])

    def test_java_does_not_sample_catalog_tag(self) -> None:
        feature = (JAVA / "worldgen" / "SurfaceRockFeature.java").read_text(
            encoding="utf-8"
        )
        self.assertNotIn("c:rocks", feature)
        self.assertIn("GT_SURFACE_ROCK", feature)
        self.assertIn("tryPlace", feature)
        tests = (TEST_JAVA / "gametest" / rocks.GAME_TESTS).read_text(
            encoding="utf-8"
        )
        self.assertIn("cruciblecraft_wave_worldgen_gt_surface_rocks", tests)
        self.assertIn("must not place RockBlock", tests)

    def test_models_use_positional_pebble_loader(self) -> None:
        model = census.load_json(
            rocks.ASSETS / "models" / "block" / "gt_surface_rock_stone.json"
        )
        self.assertEqual(
            "cruciblecraft:positional_pebble", model["loader"]
        )
        states = census.load_json(
            rocks.ASSETS / "blockstates" / "gt_surface_rock.json"
        )
        self.assertIn("variants", states)
        self.assertNotIn("multipart", states)
        pebble = (JAVA / "worldgen" / "PebbleShape.java").read_text(encoding="utf-8")
        self.assertNotIn("IntegerProperty.create", pebble)

    def test_unique_active_hand_off_matches_capability(self) -> None:
        capability = census.load_json(
            ROOT / "tools" / "capabilities" / rocks.SLUG / "capability.json"
        )
        topology = census.load_json(rocks.WAVE / "topology.json")
        if capability["workflow"] == "active":
            self.assertEqual(rocks.SLUG, topology["unique_active_wave"])
        else:
            self.assertEqual("accepted", capability["workflow"])
            self.assertIsNone(topology["unique_active_wave"])
        self.assertEqual([], capability["depends_on"])


if __name__ == "__main__":
    unittest.main()
