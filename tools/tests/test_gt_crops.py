#!/usr/bin/env python3
"""GT crops: WorldgenGlowtus + WorldgenBushes overworld runtime."""
from __future__ import annotations

import unittest

from tools import census_common as census
from tools import gt_crops as crops

ROOT = census.ROOT
JAVA = ROOT / "src" / "main" / "java" / "com" / "masson" / "cruciblecraft"
TEST_JAVA = ROOT / "src" / "test" / "java" / "com" / "masson" / "cruciblecraft"


class GtCropsTest(unittest.TestCase):
    def test_builder_check_passes(self) -> None:
        self.assertEqual([], crops.check())

    def test_denominator_is_two_overworld_features(self) -> None:
        document = census.load_json(crops.WAVE / "denominator.json")
        self.assertEqual(2, document["feature_count"])
        self.assertEqual(["plant.glowtus", "plant.bush"], document["features"])
        self.assertEqual(16, document["glowtus_colors"])
        self.assertEqual(32759, document["bush_mte"])
        self.assertEqual(16, len(crops.GLOWTUS))
        self.assertEqual("black", crops.GLOWTUS[0])
        self.assertEqual("white", crops.GLOWTUS[-1])

    def test_glowtus_ids_are_not_white_glowtus_alias(self) -> None:
        capability = census.load_json(
            ROOT / "tools" / "capabilities" / crops.SLUG / "capability.json"
        )
        self.assertEqual(crops.SLUG, capability["slug"])
        self.assertEqual("runtime_ready", capability["maturity"])
        self.assertIn(capability["workflow"], {"active", "accepted"})
        glowtus = capability["identity_disposition"][0]
        bush = capability["identity_disposition"][1]
        self.assertEqual("worldgen:plant.glowtus", glowtus["semantic_key"])
        self.assertEqual("new_distinct", glowtus["disposition"])
        self.assertEqual(
            [f"cruciblecraft:plant/glowtus_{color}" for color in crops.GLOWTUS],
            glowtus["runtime_ids"],
        )
        blob = str(capability)
        self.assertNotIn("lilypad_glowtus/white_glowtus", glowtus["runtime_ids"])
        self.assertNotIn("programmed_circuit", blob)
        self.assertEqual("worldgen:plant.bush", bush["semantic_key"])
        self.assertEqual(["cruciblecraft:plant/gt_bush"], bush["runtime_ids"])
        self.assertIn("sweet_berries", bush["reason"])
        self.assertIn("never string", bush["reason"])

    def test_art_is_copied_from_gregtech6_w(self) -> None:
        manifest = census.load_json(crops.ART_MANIFEST)
        self.assertEqual(crops.art_imports(), manifest["imports"])
        self.assertEqual(22, len(manifest["imports"]))
        destinations = [row["destination"] for row in manifest["imports"]]
        self.assertIn(
            "assets/cruciblecraft/textures/block/gt6/iconsets/glowtus_white.png",
            destinations,
        )
        self.assertIn(
            "assets/cruciblecraft/textures/block/gt6_import/plants/bush/colored/bush.png",
            destinations,
        )
        for row in manifest["imports"]:
            self.assertNotIn("multiblock_casing", row["destination"])
            self.assertNotIn("pipe_filter_cover", row["destination"])
            self.assertNotIn("conveyor_cover", row["destination"])
            dest = ROOT / "src" / "main" / "resources" / row["destination"]
            source = crops.GT6_W / row["gt6_source"]
            self.assertEqual(source.read_bytes(), dest.read_bytes(), row["destination"])

    def test_java_does_not_stand_in_string_or_cover(self) -> None:
        bush = (
            JAVA / "content" / "blockentity" / "GtBushBlockEntity.java"
        ).read_text(encoding="utf-8")
        self.assertIn("SWEET_BERRIES", bush)
        self.assertIn("is(Items.STRING)", bush)
        model = (
            ROOT
            / "src"
            / "main"
            / "resources"
            / "assets"
            / "cruciblecraft"
            / "models"
            / "block"
            / "plant"
            / "gt_bush_cube_bare.json"
        ).read_text(encoding="utf-8")
        self.assertIn("overlay/bush", model)
        self.assertIn("tintindex", model)
        self.assertNotIn("cube_all", model)
        feature = (JAVA / "worldgen" / "crop" / "GtCropFeature.java").read_text(
            encoding="utf-8"
        )
        self.assertNotIn("lilypad_glowtus", feature)
        tests = (TEST_JAVA / "gametest" / "GtCropsGameTests.java").read_text(
            encoding="utf-8"
        )
        self.assertIn("must not reuse lilypad_glowtus/white_glowtus", tests)
        self.assertIn("cruciblecraft_wave_worldgen_gt_crops", tests)

    def test_unique_active_hand_off_matches_capability(self) -> None:
        capability = census.load_json(
            ROOT / "tools" / "capabilities" / crops.SLUG / "capability.json"
        )
        topology = census.load_json(crops.WAVE / "topology.json")
        if capability["workflow"] == "active":
            self.assertEqual(crops.SLUG, topology["unique_active_wave"])
        else:
            self.assertEqual("accepted", capability["workflow"])
            self.assertIsNone(topology["unique_active_wave"])
        self.assertEqual(["worldgen/gt-trees"], capability["depends_on"])
        self.assertNotIn("content/gt6-pipe-cable-baseline", capability["depends_on"])


if __name__ == "__main__":
    unittest.main()
