#!/usr/bin/env python3
"""Generated resource / art gate tests."""
from __future__ import annotations

import unittest

from tools import generated_resource_gate as gate


class GeneratedResourceGateTest(unittest.TestCase):
    def test_brace_destinations_expand_to_real_png_paths(self) -> None:
        paths = gate.expand_brace_paths(
            "assets/cruciblecraft/textures/block/machine/laser_engraver/"
            "colored/{back,bottom}.png"
        )
        self.assertEqual(
            [
                "assets/cruciblecraft/textures/block/machine/laser_engraver/"
                "colored/back.png",
                "assets/cruciblecraft/textures/block/machine/laser_engraver/"
                "colored/bottom.png",
            ],
            paths,
        )

    def test_live_catalog_has_no_self_parent_or_missing_item_models(self) -> None:
        manifest = gate.compile_manifest()
        self.assertEqual("PASS", manifest["status"], manifest["errors"][:40])
        self.assertEqual([], manifest["errors"][:40], manifest["errors"][:40])
        self.assertGreater(manifest["live_block_count"], 1000)


if __name__ == "__main__":
    unittest.main()
