#!/usr/bin/env python3
"""Contract tests for SOURCE_BACKED static block icons."""
from __future__ import annotations

import unittest

from tools import block_art as art
from tools import census_common as census


class BlockArtTest(unittest.TestCase):
    def test_stone_meta_8_is_reinforced_bricks(self) -> None:
        bind = art.resolve("gregtech:gt.stone.andesite", 8)
        self.assertIsNotNone(bind)
        assert bind is not None
        self.assertEqual("cube_all", bind["model"])
        self.assertEqual(
            ["stones/gt.stone.andesite/bricks_reinforced.png"],
            bind["sources"],
        )

    def test_asphalt_shares_one_png_and_dye_tint(self) -> None:
        black = art.resolve("gregtech:gt.block.asphalt", 0)
        white = art.resolve("gregtech:gt.block.asphalt", 15)
        self.assertEqual(black, white)
        assert black is not None
        self.assertEqual("dye", black["tint"])
        self.assertEqual(["iconsets/asphalt.png"], black["sources"])

    def test_leftover_semantic_block_uses_item_layer0(self) -> None:
        identity = {
            "kind": "block",
            "meta": 0,
            "registry_path": "lilypad_glowtus/white_glowtus",
            "source_item": "gregtech:gt.block.lilypad.glowtus",
            "texture": "minecraft:item/iron_ingot",
        }
        self.assertEqual(
            "cruciblecraft:block/gt6/iconsets/glowtus_black",
            art.item_layer0(identity),
        )
        self.assertEqual(
            "cruciblecraft:block/gt6/iconsets/glowtus_black",
            art.identity_layer0(identity),
        )

    def test_glass_shares_one_png_dye_tint_and_translucent(self) -> None:
        cube = art.resolve("gregtech:gt.block.glass", 0)
        slab = art.resolve("gregtech:gt.block.glass.slab.0", 15)
        glow = art.resolve("gregtech:gt.block.glass.glow", 4)
        self.assertIsNotNone(cube)
        assert cube is not None
        self.assertEqual(cube, glow)
        self.assertEqual("dye", cube["tint"])
        self.assertTrue(cube["translucent"])
        self.assertEqual(["iconsets/glass_clear.png"], cube["sources"])
        assert slab is not None
        self.assertEqual("slab", slab["model"])
        self.assertTrue(slab["translucent"])

    def test_catalog_covers_expected_binds_without_overlap(self) -> None:
        rows = art.catalog_rows()
        runtimes = [str(row["runtime_id"]) for row in rows]
        self.assertEqual(art.EXPECTED_BIND_COUNT, len(rows))
        self.assertEqual(art.EXPECTED_BIND_COUNT, len(set(runtimes)))
        catalogs = {str(row["catalog"]) for row in rows}
        self.assertEqual(
            {"block_object", "bath_remainder", "building_block", "stone", "semantic_block"},
            catalogs,
        )

    def test_committed_manifest_and_models_match_copied_pngs(self) -> None:
        if not art.MANIFEST.is_file():
            self.skipTest("block art manifest not generated")
        document = census.load_json(art.MANIFEST)
        self.assertEqual(art.STATUS, document["status"])
        self.assertEqual(census.SOURCE_REVISION, document["source_revision"])
        self.assertEqual(art.EXPECTED_BIND_COUNT, document["identity_count"])
        self.assertEqual([], art.check_payload(document))
        self.assertEqual([], art.check_models_and_pngs(document))


if __name__ == "__main__":
    unittest.main()
