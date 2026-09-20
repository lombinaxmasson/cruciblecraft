#!/usr/bin/env python3
"""Isolated contract tests for the localization naming prep card."""
from __future__ import annotations

import importlib.util
import unittest
from pathlib import Path

WAVE = Path(__file__).resolve().parent
ROOT = WAVE.parents[3]


def _load():
    path = WAVE / "isolated_audit.py"
    spec = importlib.util.spec_from_file_location("localization_isolated_audit", path)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    spec.loader.exec_module(module)
    return module


audit = _load()


class LanguageKeyContractTest(unittest.TestCase):
    def test_slash_registry_paths_become_dotted_keys(self) -> None:
        self.assertEqual(
            "block.cruciblecraft.aluminium.fluid_pipe",
            audit.translation_key("block", "aluminium/fluid_pipe"),
        )
        self.assertEqual(
            "block.cruciblecraft.aluminium.block",
            audit.translation_key("block", "aluminium/block"),
        )
        self.assertEqual(
            "item.cruciblecraft.water.plant_gt_twig",
            audit.translation_key("item", "water/plant_gt_twig"),
        )
        self.assertEqual(
            "item.cruciblecraft.red_alloy.wire",
            audit.translation_key("item", "red_alloy.wire"),
        )
        self.assertEqual(
            "fluid.cruciblecraft.dye_flower_black",
            audit.translation_key("fluid", "dye_flower_black"),
        )
        self.assertEqual(
            "fluid_type.cruciblecraft.molten_aluminium",
            audit.translation_key("fluid_type", "molten_aluminium"),
        )

    def test_block_and_item_share_the_same_path_mapping(self) -> None:
        path = "steel/item_pipe"
        self.assertEqual(
            audit.translation_key("item", path).split(".", 1)[1],
            audit.translation_key("block", path).split(".", 1)[1],
        )

    def test_slash_keys_are_illegal_for_registry_backed_entries(self) -> None:
        self.assertTrue(
            audit.is_illegal_slash_key("block.cruciblecraft.aluminium/fluid_pipe")
        )
        self.assertFalse(
            audit.is_illegal_slash_key("block.cruciblecraft.aluminium.fluid_pipe")
        )
        self.assertFalse(
            audit.is_illegal_slash_key("item.cruciblecraft.material_form.block")
        )
        self.assertFalse(audit.is_template_key("block.cruciblecraft.aluminium.block"))
        self.assertTrue(
            audit.is_template_key("item.cruciblecraft.material_form.fluid_pipe")
        )

    def test_unknown_object_type_is_rejected(self) -> None:
        with self.assertRaises(ValueError):
            audit.translation_key("blockstate", "aluminium/block")


class EnglishDisplayNameTest(unittest.TestCase):
    def test_snake_case_title_cases_every_word(self) -> None:
        self.assertEqual("Fluid Pipe", audit.format_english_id("fluid_pipe"))
        self.assertEqual("Huge Item Pipe", audit.format_english_id("huge_item_pipe"))
        self.assertEqual("Ancient Debris", audit.format_english_id("ancient_debris"))
        self.assertEqual("Stainless Steel", audit.format_english_id("stainless_steel"))
        self.assertEqual(
            "Aluminium Fluid Pipe",
            audit.compose_english("aluminium", "fluid_pipe"),
        )
        self.assertEqual("Aluminium Block", audit.compose_english("aluminium", "block"))

    def test_gt6_acronyms_and_special_forms(self) -> None:
        self.assertEqual("HSLA Steel", audit.format_english_id("hslasteel"))
        self.assertEqual(
            "Bedrock HSLA Alloy",
            audit.format_english_id("bedrock_hslaalloy"),
        )
        self.assertEqual(
            "HSLA Tungsten Alloy",
            audit.format_english_id("hslatungsten_alloy"),
        )
        self.assertEqual("LuV", audit.format_english_id("luv"))
        self.assertEqual("OMEGA", audit.format_english_id("omega"))
        self.assertEqual("Capsule Cell Container", audit.format_english_id("capcellcon"))
        self.assertEqual(
            "Double Machine Casing",
            audit.format_english_id("machine_casing_double"),
        )

    def test_plant_gt_prefix_keeps_source_backed_english(self) -> None:
        self.assertEqual("Twig", audit.format_english_id("plant_gt_twig"))
        self.assertEqual("Fiber", audit.format_english_id("plant_gt_fiber"))
        self.assertEqual("Water Twig", audit.compose_english("water", "plant_gt_twig"))
        self.assertEqual("Lava Twig", audit.compose_english("lava", "plant_gt_twig"))
        self.assertEqual(
            "Heavy Water Twig",
            audit.compose_english("heavy_water", "plant_gt_twig"),
        )

    def test_player_english_keeps_serial_bookshelf_and_adds_variants(self) -> None:
        self.assertEqual(
            "Wooden Bookshelf",
            audit.names.player_english("Wooden Bookshelf", "bookshelf_7000"),
        )
        self.assertEqual(
            "1 Beam Orange",
            audit.names.player_english("1 Beam", "beam_1/orange"),
        )
        self.assertEqual(
            "Asphalt Black Down Slab",
            audit.names.player_english("Asphalt Black", "asphalt/black/slab_down"),
        )
        self.assertEqual(
            "Water Twig",
            audit.names.player_english("Water Twig", "water/plant_gt_twig"),
        )
        self.assertEqual(
            "Andesite Reinforced Bricks",
            audit.names.player_english("Andesite m8", "andesite/reinforced_bricks"),
        )
        self.assertEqual(
            "Neutronium Autocrafter PUV2",
            audit.names.player_english(
                "Neutronium Autocrafter", "neutronium_autocrafter_puv2"
            ),
        )
        self.assertEqual(
            "Neutronium Electric Mixer PUV2",
            audit.names.player_english(
                "Neutronium Electric Mixer", "neutronium_electric_mixer_puv2"
            ),
        )
        self.assertEqual(
            "Fireproof Beam 1",
            audit.names.player_english(
                "Fireproof Beam", "beam_1_fireproof/fireproof_beam"
            ),
        )
        self.assertEqual(
            "Wooden Item Barrel (Cheap)",
            audit.names.player_english(
                "Wooden Item Barrel (Cheap)", "item_barrel_6990"
            ),
        )
        self.assertEqual(
            "Apple",
            audit.names.player_english("Apple", "applewood/apple"),
        )
        self.assertEqual(
            "Apple Applewood",
            audit.names.player_english("Apple", "apple/applewood"),
        )
        self.assertEqual(
            "Apple Slice",
            audit.names.player_english("Apple Slice", "applewood/slice"),
        )
        self.assertEqual(
            "Lighter (Empty)",
            audit.names.player_english(
                "Lighter (Empty)",
                "tool/lighter_empty_requires_canning_machine_to_be_filled",
            ),
        )
        self.assertEqual(
            "Blue Spruce Planks",
            audit.names.player_english("Blue Spruce Planks", "planks2/orange"),
        )
        self.assertEqual(
            "Blue Spruce Slab",
            audit.names.player_english(
                "Blue Spruce Slab", "planks2/orange/slab_down"
            ),
        )
        self.assertEqual(
            "Blue Spruce Planks (Fireproof)",
            audit.names.player_english(
                "Blue Spruce Planks (Fireproof)",
                "planks2_fireproof/orange",
            ),
        )


class ChineseFallbackTest(unittest.TestCase):
    def test_english_copy_detector(self) -> None:
        self.assertTrue(audit.is_english_copy("Water Twig", "Water Twig"))
        self.assertTrue(audit.is_english_copy("Flower Black dye", "Flower Black dye"))
        self.assertTrue(audit.is_english_copy("Asphalt Black"))
        self.assertFalse(audit.is_english_copy("铝块", "Aluminium Block"))
        self.assertFalse(audit.is_english_copy("水枝条", "Water Twig"))
        self.assertFalse(audit.is_english_copy("", "Water Twig"))
        self.assertFalse(
            audit.is_english_copy("%s：%s / %s", "%s: %s / %s")
        )

    def test_material_table_landing_fixes_are_loaded(self) -> None:
        table = audit.names.material_zh_table()
        self.assertEqual("中子素", table["materials"]["neutronium"])
        self.assertEqual("超导体", table["materials"]["superconductor"])
        self.assertNotIn("neutronium", table)
        self.assertNotIn("superconductor", table)
        self.assertEqual("流体管道", table["pipes"]["fluid_pipe"])
        self.assertEqual("物品管道", table["pipes"]["item_pipe"])
        self.assertNotIn("normal_fluid_pipe", table["pipes"])
        self.assertEqual("枝条", table["prefixes"]["plant_gt_twig"])
        self.assertEqual("水枝条", audit.names.compose_material_form_zh("water/plant_gt_twig"))


class CollisionAndGateTest(unittest.TestCase):
    def test_block_and_item_same_path_are_one_identity(self) -> None:
        table = {
            "block.cruciblecraft.steel.item_pipe": "Steel Item Pipe",
            "item.cruciblecraft.steel.item_pipe": "Steel Item Pipe",
            "block.cruciblecraft.asphalt.black": "Asphalt Black",
            "block.cruciblecraft.asphalt.black.slab_down": "Asphalt Black",
        }
        collisions = audit.grouped_collisions(table)
        self.assertEqual(1, len(collisions))
        self.assertEqual("Asphalt Black", collisions[0]["display_name"])
        self.assertEqual(2, len(collisions[0]["identities"]))

    def test_allowlist_covers_source_backed_shared_storage_names(self) -> None:
        self.assertIn("Wooden Bookshelf", audit.names.allowlisted_names("en_us"))
        self.assertIn("木制书架", audit.names.allowlisted_names("zh_cn"))
        self.assertIn("Wooden Bottlecrate", audit.names.allowlisted_names("en_us"))
        self.assertIn("木制瓶箱", audit.names.allowlisted_names("zh_cn"))
        self.assertIn("Wooden Item Barrel (Cheap)", audit.names.allowlisted_names("en_us"))
        self.assertIn("Apple Slice", audit.names.allowlisted_names("en_us"))
        self.assertIn("Progress Sensor", audit.names.allowlisted_names("en_us"))
        self.assertIn("Rubber Resin", audit.names.allowlisted_names("en_us"))
        self.assertIn("Blue Spruce Planks", audit.names.allowlisted_names("en_us"))
        self.assertIn("蓝云杉木板", audit.names.allowlisted_names("zh_cn"))

    def test_generated_lang_uses_dotted_keys_and_real_chinese(self) -> None:
        document = audit.scan_lang_tables()
        self.assertEqual(0, document["slash_keys"]["en_us_count"])
        self.assertEqual(0, document["slash_keys"]["zh_cn_count"])
        self.assertFalse(document["focus"]["aluminium_fluid_pipe_slash"])
        self.assertTrue(document["focus"]["aluminium_fluid_pipe_dotted"])
        self.assertFalse(document["focus"]["aluminium_block_slash"])
        self.assertEqual("Water Twig", document["focus"]["water_twig"])
        self.assertNotEqual(
            document["focus"]["asphalt_black"],
            document["focus"]["asphalt_black_slab"],
        )
        self.assertIsNone(document["focus"]["flower_black_dye_zh"])
        self.assertEqual(
            0, document["english_copy_zh"]["equal_to_en_count"]
        )
        self.assertEqual(0, document["english_copy_zh"]["latin_only_count"])
        self.assertEqual(0, document["collisions"]["en_us_count"])
        self.assertEqual(0, document["collisions"]["zh_cn_count"])
        self.assertEqual(
            0,
            document["gate_inventory"]["slash_block_lang_missing_from_live"],
        )

    def test_contracts_declare_the_prep_slug(self) -> None:
        for reader in (
            audit.language_key_contract,
            audit.english_contract,
            audit.zh_contract,
            audit.collision_contract,
        ):
            self.assertEqual(audit.SLUG, reader()["capability_slug"])
            self.assertEqual("FROZEN_FOR_LANDING", reader()["status"])


if __name__ == "__main__":
    unittest.main()
