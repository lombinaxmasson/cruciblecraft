#!/usr/bin/env python3
"""Contract tests for T48 identity overlays that do not load the T47 source."""
from __future__ import annotations

import unittest

from tools import t48_identities as identities


class T48IdentityOverlayTest(unittest.TestCase):
    def test_tool_heads_are_prefix_forms(self) -> None:
        self.assertEqual(
            "tool_head",
            identities.classify_source_item("gregtech:gt.meta.toolHeadArrow"),
        )
        self.assertEqual(
            "tool_head_arrow",
            identities.overlay_prefix_form("gregtech:gt.meta.toolHeadArrow"),
        )
        self.assertEqual(
            "cruciblecraft:iron/tool_head_arrow",
            identities.runtime_id_for(
                "tool_head", "gregtech:gt.meta.toolHeadArrow", 260
            ),
        )
        self.assertFalse(
            identities.runtime_id_for(
                "tool_head", "gregtech:gt.meta.toolHeadArrow", 260
            ).startswith("cruciblecraft:gt_tool_head/")
        )

    def test_prefix_overlay_covers_purified_tiny_and_arrows(self) -> None:
        self.assertEqual(
            "tiny_washed_crushed_ore",
            identities.overlay_prefix_form("gregtech:gt.meta.crushedPurifiedTiny"),
        )
        self.assertEqual(
            "arrow_gt_plastic",
            identities.overlay_prefix_form("gregtech:gt.meta.arrowGtPlastic"),
        )
        self.assertEqual(
            "lens",
            identities.overlay_prefix_form("gregtech:gt.meta.lens"),
        )

    def test_dye_and_wildcard_tags_map_to_1_21(self) -> None:
        self.assertEqual(
            "minecraft:bone_meal",
            identities.vanilla_meta_runtime("minecraft:dye", 15),
        )
        self.assertEqual(
            "cruciblecraft:bath_identity_stained_glass",
            identities.vanilla_wildcard_tag("minecraft:stained_glass"),
        )
        self.assertEqual(16, len(identities.stained_color_items("minecraft:stained_glass")))
