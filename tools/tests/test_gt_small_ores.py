#!/usr/bin/env python3
"""GT6 WorldgenOresSmall independent scatter."""
from __future__ import annotations

import unittest

from tools import census_common as census
from tools import gt_small_ores as ores

ROOT = census.ROOT
JAVA = ROOT / "src" / "main" / "java" / "com" / "masson" / "cruciblecraft"
TEST_JAVA = ROOT / "src" / "test" / "java" / "com" / "masson" / "cruciblecraft"


class GtSmallOresTest(unittest.TestCase):
    def test_builder_check_passes(self) -> None:
        self.assertEqual([], ores.check())

    def test_denominator_is_independent_small_ores(self) -> None:
        document = census.load_json(ores.WAVE / "denominator.json")
        self.assertEqual(37, document["overworld_count"])
        self.assertEqual(19, document["nether_count"])
        self.assertEqual(41, document["unique_name_count"])
        self.assertEqual(129, document["catalog_vein_count"])
        self.assertEqual("cruciblecraft:small_ores", document["feature"])
        self.assertEqual("fluid_springs", document["step"])
        self.assertEqual(32, document["coltan_amount"])
        self.assertEqual(480, document["coltan_range"])
        self.assertEqual("sylvite", document["rocksalt_material"])

    def test_capability_does_not_alias_t20_or_catalog_dump(self) -> None:
        capability = census.load_json(
            ROOT / "tools" / "capabilities" / ores.SLUG / "capability.json"
        )
        self.assertEqual(ores.SLUG, capability["slug"])
        self.assertEqual("runtime_ready", capability["maturity"])
        self.assertIn(capability["workflow"], {"active", "accepted", "paused"})
        identity = capability["identity_disposition"][0]
        self.assertEqual("worldgen:overworld.ores.small", identity["semantic_key"])
        self.assertEqual("new_distinct", identity["disposition"])
        blob = str(capability)
        self.assertNotIn("programmed_circuit", blob)
        self.assertNotIn("GtItemScatterFeature", blob)
        self.assertIn("cruciblecraft:small_ores", identity["runtime_ids"])
        self.assertIn("cruciblecraft:gt_small_ore", identity["runtime_ids"])

    def test_catalog_matches_loader_and_keeps_sylvite(self) -> None:
        parsed = ores.parse_loader()
        catalog = census.load_json(ores.CATALOG)
        self.assertEqual(parsed["entries"], catalog["entries"])
        self.assertEqual(37, catalog["overworld_count"])
        self.assertEqual(19, catalog["nether_count"])
        self.assertEqual(41, catalog["unique_name_count"])
        rocksalt = next(
            row for row in catalog["entries"] if row["gt6_name"] == "ore.small.rocksalt"
        )
        self.assertEqual("sylvite", rocksalt["material"])
        names = {row["gt6_name"] for row in catalog["entries"]}
        self.assertNotIn("ore.small.nikolite", names)
        self.assertNotIn("ore.small.ancientdebris", names)
        self.assertIn("ore.small.coltan", names)
        self.assertIn("ore.small.niter", names)
        self.assertEqual(
            ["coltan", "columbite", "tantalite"], catalog["coltan"]["materials"]
        )

    def test_java_does_not_stand_in_ellipsoids_or_scatter(self) -> None:
        feature = (JAVA / "worldgen" / "SmallOreFeature.java").read_text(
            encoding="utf-8"
        )
        self.assertIn("placeCount", feature)
        self.assertIn("gt6ChunkRandom", feature)
        self.assertIn("WorldgenColtan", feature)
        self.assertNotIn("GtItemScatterFeature", feature)
        self.assertNotIn("programmed_circuit", feature)
        tests = (TEST_JAVA / "gametest" / ores.GAME_TESTS).read_text(encoding="utf-8")
        self.assertIn("must not reuse T20 large-vein ellipsoids", tests)
        self.assertIn("cruciblecraft_wave_worldgen_gt_small_ores", tests)
        self.assertIn("sylvite", tests)
        five = census.load_json(ores.REMOVE_FIVE)
        self.assertEqual(list(ores.FIVE_REMOVED_VEINS), five["features"])

    def test_unique_active_hand_off_matches_capability(self) -> None:
        capability = census.load_json(
            ROOT / "tools" / "capabilities" / ores.SLUG / "capability.json"
        )
        topology = census.load_json(ores.WAVE / "topology.json")
        if capability["workflow"] == "active":
            self.assertEqual(ores.SLUG, topology["unique_active_wave"])
        else:
            self.assertIn(capability["workflow"], {"accepted", "paused"})
            self.assertIsNone(topology["unique_active_wave"])
        self.assertEqual(["worldgen/gt-stone-layer-rocks"], capability["depends_on"])
        self.assertNotIn("content/gt6-pipe-cable-baseline", capability["depends_on"])

    def test_small_ore_overlay_is_gt6_oresmall(self) -> None:
        self.assertEqual([], ores.check_art())
        png = ores.ORE_SMALL_PNG.read_bytes()
        self.assertEqual(png, ores.GT6_ORE_SMALL_SRC.read_bytes())
        self.assertNotEqual(
            png,
            (
                ROOT
                / "src"
                / "main"
                / "resources"
                / "assets"
                / "cruciblecraft"
                / "textures"
                / "block"
                / "material"
                / "ore_flecks.png"
            ).read_bytes(),
        )
        hosted = census.load_json(ores.HOSTED_ORE_STATE)
        self.assertIn(
            "cruciblecraft:block/ore_host/stone",
            hosted["variants"]["host=stone"]["model"],
        )
        small = census.load_json(ores.SMALL_ORE_STATE)
        self.assertEqual(
            "cruciblecraft:block/ore_small_host/stone",
            small["variants"]["host=stone"]["model"],
        )


if __name__ == "__main__":
    unittest.main()
