from __future__ import annotations

import json
import unittest

from tools import build_t28_hot_ingot_source_evidence as builder
from tools import t27_common as common


class T28HotIngotSourceEvidenceTest(unittest.TestCase):
    def test_committed_artifact_is_current_and_check_is_read_only(self) -> None:
        on_disk = json.loads(builder.OUTPUT.read_text(encoding="utf-8"))
        expected = builder.build()
        self.assertEqual(expected, on_disk)
        before = builder.OUTPUT.read_bytes()
        self.assertEqual([], builder.check())
        self.assertEqual(before, builder.OUTPUT.read_bytes())

    def test_fixed_revision_has_no_passive_ingot_hot_conversion(self) -> None:
        document = builder.build()
        self.assertEqual("T28_HOT_INGOT_SOURCE_EVIDENCE", document["status"])
        self.assertEqual(common.SOURCE_REVISION, document["source_revision"])
        self.assertFalse(document["conclusion"]["gt6_passive_ingotHot_to_ingot"])
        self.assertFalse(document["conclusion"]["gt6_generic_cooler_map"])
        self.assertEqual(
            "strict_no_conversion",
            document["conclusion"]["product_decision"],
        )
        self.assertEqual(0, document["conversion_search"]["cooler_map_hits"])
        self.assertEqual(3.0, document["heat_damage"]["value"])
        self.assertIn(
            "ingotHot.mHeatDamage = 3.0F",
            document["heat_damage"]["prefix_assignment"]["text"],
        )
        handler_hits = [
            hit
            for hit in document["conversion_search"]["hits"]
            if "Loader_Recipes_Handlers.java" in hit["path"]
        ]
        self.assertTrue(handler_hits)
        self.assertTrue(
            all(hit["classification"] == "recipe_exclusion" for hit in handler_hits)
        )
        for hit in document["conversion_search"]["hits"]:
            self.assertIn(hit["classification"], builder.ALLOWED_INGOT_HOT_CLASSES)

    def test_freezer_is_post_1_0_and_not_a_replacement(self) -> None:
        document = builder.build()
        freezer = document["freezer"]
        self.assertEqual("gt.recipe.freezer", freezer["canonical_id"])
        self.assertEqual("post_1_0", freezer["disposition"])
        self.assertFalse(freezer["in_card_scope"])
        self.assertEqual(0, freezer["java_ingotHot_hits"])
        self.assertEqual("deferred_with_reason", freezer["t13_classification"])
        dump = freezer["dump"]
        self.assertEqual(0, dump["prefix_conversion_recipes"])
        if dump["present"]:
            self.assertGreater(dump["material_transform_recipes"], 0)
            self.assertTrue(
                any(
                    "Hot Frozen Iron Ingot" in example["outputs"]
                    for example in dump["examples"]
                )
            )

    def test_cc_auto_conversion_path_is_retired(self) -> None:
        document = builder.build()
        conversion = document["cc_auto_conversion"]
        self.assertFalse(conversion["present"])
        self.assertEqual("retired", conversion["status"])
        self.assertNotIn("MaterialItemCooling.coolIfReady", conversion["runtime_path"])
        self.assertFalse((common.ROOT / conversion["cooling_rule"]["path"]).is_file())
        self.assertIn("ItemHeat.clearIfCooled", conversion["runtime_path"])

    def test_unexpected_ingot_hot_hit_is_conversion_class(self) -> None:
        self.assertEqual(
            "prefix_conversion",
            builder.classify_ingot_hot_hit(
                {
                    "path": "gregapi/GT_API_Proxy.java",
                    "line": 1,
                    "text": "ST.set(tStack, OP.ingotHot); OP.ingot, remainder",
                }
            ),
        )
        self.assertEqual(
            "unexpected",
            builder.classify_ingot_hot_hit(
                {
                    "path": "gregapi/unknown/Tick.java",
                    "line": 9,
                    "text": "if (prefix == ingotHot) convert();",
                }
            ),
        )
        self.assertEqual(
            "prefix_conversion",
            builder.classify_freezer_recipe(
                {
                    "inputs": [{"item": "gregtech:gt.meta.ingotHot"}],
                    "outputs": [{"item": "gregtech:gt.meta.ingot"}],
                }
            ),
        )
        self.assertEqual(
            "material_transform",
            builder.classify_freezer_recipe(
                {
                    "inputs": [{"item": "gregtech:gt.meta.ingotHot"}],
                    "outputs": [{"item": "gregtech:gt.meta.ingotHot"}],
                }
            ),
        )
