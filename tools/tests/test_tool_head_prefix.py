#!/usr/bin/env python3
"""Contract tests for the tool-head prefix remap authority."""
from __future__ import annotations

import unittest

from tools import tool_head_prefix as thp


class ToolHeadPrefixTest(unittest.TestCase):
    def test_camel_case_prefix_names_are_snake_and_not_arrows(self) -> None:
        self.assertEqual("tool_head_raw_pickaxe", thp.cc_prefix_id("toolHeadRawPickaxe"))
        self.assertEqual("tool_head_arrow", thp.cc_prefix_id("toolHeadArrow"))
        self.assertEqual(
            "tool_head_raw_pickaxe",
            thp.source_item_to_prefix("gregtech:gt.meta.toolHeadRawPickaxe"),
        )
        self.assertIsNone(thp.source_item_to_prefix("gregtech:gt.meta.arrowGtWood"))
        self.assertNotEqual(thp.cc_prefix_id("toolHeadArrow"), "arrow_gt_wood")

    def test_remap_covers_frozen_source_items_without_unique_ids(self) -> None:
        if not thp.REMAP_TOOLS.is_file():
            self.skipTest("tool_head_prefix_remap.json not generated")
        document = thp.build_remap()
        self.assertEqual("TOOL_HEAD_PREFIX_REMAP", document["status"])
        source_items = list(document.get("source_item_set") or [])
        self.assertGreaterEqual(len(source_items), 36)
        self.assertTrue(
            all(item.startswith("gregtech:gt.meta.toolHead") for item in source_items)
        )
        self.assertNotIn("gregtech:gt.meta.arrowGtWood", source_items)
        self.assertNotIn("arrow_gt_wood", document.get("prefixes") or {})
        mapped = list(document.get("mapped") or [])
        remainder = list(document.get("remainder") or [])
        counts = document.get("counts") or {}
        self.assertIn("mapped", counts)
        self.assertIn("remainder", counts)
        self.assertEqual(len(mapped), int(counts["mapped"]))
        self.assertEqual(len(remainder), int(counts["remainder"]))
        keys = {(row["source_item"], row["meta"]) for row in mapped}
        self.assertEqual(len(keys), len(mapped))
        for row in mapped:
            runtime = str(row["runtime_id"])
            self.assertFalse(runtime.startswith("cruciblecraft:gt_tool_head/"))
            self.assertEqual(
                f"cruciblecraft:{row['material']}/{row['prefix']}",
                runtime,
            )
            self.assertTrue(thp.is_mapped(row["source_item"], row["meta"]))
            self.assertEqual(runtime, thp.mapped_runtime(row["source_item"], row["meta"]))
        for row in remainder:
            self.assertTrue(row.get("reason"))
            self.assertFalse(thp.is_mapped(row["source_item"], row["meta"]))

    def test_forbidden_unique_ids_follow_remainder(self) -> None:
        if not thp.REMAP_TOOLS.is_file():
            self.skipTest("tool_head_prefix_remap.json not generated")
        self.assertTrue(
            thp.is_forbidden_unique_tool_head(
                "cruciblecraft:gt_tool_head/toolheadrawpickaxe_m260"
            )
        )
        self.assertFalse(
            thp.is_forbidden_unique_tool_head("cruciblecraft:iron/tool_head_pickaxe")
        )

    def test_bundled_catalogs_drop_mapped_tool_heads(self) -> None:
        from tools import census_common as census

        bath = census.load_json(thp.BUNDLED_BATH)
        semantic = census.load_json(thp.BUNDLED_SEMANTIC)
        self.assertEqual("BATH_IDENTITY_CATALOG", bath["status"])
        self.assertEqual(71, bath["identity_count"])
        self.assertEqual("SEMANTIC_OBJECT_CATALOG", semantic["status"])
        self.assertEqual(244, semantic["identity_count"])
        for catalog in (bath, semantic):
            for row in catalog.get("identities") or []:
                self.assertNotEqual("tool_head", row.get("kind"))
                runtime = str(row.get("runtime_id") or "")
                self.assertFalse(runtime.startswith("cruciblecraft:gt_tool_head/"))


if __name__ == "__main__":
    unittest.main()
