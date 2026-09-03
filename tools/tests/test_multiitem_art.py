#!/usr/bin/env python3
"""Contract tests for SOURCE_BACKED identity multiitem icons."""
from __future__ import annotations

import unittest

from tools import multiitem_art as art
from tools import t35_common as t35


class MultiitemArtTest(unittest.TestCase):
    def test_food_meta_binds_to_gt6_w_family_png(self) -> None:
        self.assertEqual(
            "textures/items/gt.multiitem.food/10.png",
            art.source_rel("gregtech:gt.multiitem.food", 10),
        )
        self.assertEqual(
            "cruciblecraft:item/gt_multiitem/multiitem_food_m10",
            art.texture_id("gt_multiitem/multiitem_food_m10"),
        )
        identity = {
            "kind": "multiitem",
            "meta": 10,
            "registry_path": "gt_multiitem/multiitem_food_m10",
            "source_item": "gregtech:gt.multiitem.food",
            "texture": art.PLACEHOLDER_TEXTURE,
        }
        self.assertTrue(art.bindable(identity))
        self.assertEqual(
            "cruciblecraft:item/gt_multiitem/multiitem_food_m10",
            art.model_layer0(identity),
        )

    def test_non_multiitem_keeps_catalog_texture(self) -> None:
        identity = {
            "kind": "object",
            "meta": 0,
            "registry_path": "gt_object/other_m0",
            "source_item": "gregtech:gt.meta.ingot",
            "texture": art.PLACEHOLDER_TEXTURE,
        }
        self.assertFalse(art.bindable(identity))
        self.assertEqual(art.PLACEHOLDER_TEXTURE, art.model_layer0(identity))

    def test_bath_and_semantic_cover_expected_binds_without_overlap(self) -> None:
        rows = art.catalog_rows()
        runtimes = [str(row["runtime_id"]) for row in rows]
        self.assertEqual(art.EXPECTED_BIND_COUNT, len(rows))
        self.assertEqual(art.EXPECTED_BIND_COUNT, len(set(runtimes)))
        self.assertTrue(all(row["source_item"] in art.SOURCE_FAMILY for row in rows))
        catalogs = {str(row["catalog"]) for row in rows}
        self.assertEqual({"bath", "semantic"}, catalogs)

    def test_committed_manifest_and_models_match_copied_pngs(self) -> None:
        if not art.MANIFEST.is_file():
            self.skipTest("multiitem art manifest not generated")
        document = t35.load_json(art.MANIFEST)
        self.assertEqual(art.STATUS, document["status"])
        self.assertEqual(t35.SOURCE_REVISION, document["source_revision"])
        self.assertEqual(art.EXPECTED_BIND_COUNT, document["identity_count"])
        errors = art.check_models_and_pngs(document)
        self.assertEqual([], errors)


if __name__ == "__main__":
    unittest.main()
