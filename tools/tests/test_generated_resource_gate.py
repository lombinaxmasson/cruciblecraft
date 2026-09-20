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

    def test_resource_roots_have_no_duplicate_relative_paths(self) -> None:
        self.assertEqual([], gate.duplicate_resource_relative_paths()[:20])

    def test_check_duplicates_cli_rejects_mixed_modes(self) -> None:
        with self.assertRaises(SystemExit):
            gate.main(["--check-duplicates", "--write"])
        with self.assertRaises(SystemExit):
            gate.main(["--stamp", "build/verification/resource-duplicates.ok"])

    def test_live_catalog_has_no_self_parent_or_missing_item_models(self) -> None:
        manifest = gate.compile_manifest()
        self.assertEqual("PASS", manifest["status"], manifest["errors"][:40])
        self.assertEqual([], manifest["errors"][:40], manifest["errors"][:40])
        self.assertGreater(manifest["live_block_count"], 1000)

    def test_generated_lang_has_dotted_keys_and_no_unallowlisted_collisions(self) -> None:
        from tools import language_names as names

        manifest = gate.compile_manifest()
        self.assertEqual("PASS", manifest["status"], manifest["errors"][:40])
        self.assertGreater(manifest["lang_blockstate_count"], manifest["live_block_count"])
        english = gate.load_json(
            gate.GENERATED / "assets/cruciblecraft/lang/en_us.json"
        )
        chinese = gate.load_json(
            gate.GENERATED / "assets/cruciblecraft/lang/zh_cn.json"
        )
        self.assertFalse(
            any(names.is_illegal_slash_key(key) for key in english)
        )
        self.assertFalse(
            any(names.is_illegal_slash_key(key) for key in chinese)
        )
        self.assertIn("block.cruciblecraft.aluminium.fluid_pipe", english)
        self.assertNotIn("block.cruciblecraft.aluminium/fluid_pipe", english)
        self.assertEqual([], names.grouped_collisions(english, locale="en_us"))
        self.assertEqual([], names.grouped_collisions(chinese, locale="zh_cn"))
        copies = [
            key
            for key, zh in chinese.items()
            if isinstance(zh, str)
            and names.registry_backed_match(key) is not None
            and not names.is_template_key(key)
            and names.is_english_copy(
                zh, english.get(key) if isinstance(english.get(key), str) else None
            )
        ]
        self.assertEqual([], copies[:20], copies[:20])


if __name__ == "__main__":
    unittest.main()
