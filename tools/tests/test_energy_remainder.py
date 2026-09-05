#!/usr/bin/env python3
"""Energy remainder card: placeholders vs early artifacts; firebox gone."""
from __future__ import annotations

import unittest

from tools import io_common as io
from tools.wave_closeout import spec_for

ROOT = io.ROOT
ACTIVE = ROOT / "docs" / "history" / "card-plans" / "active"
CLOSED = ROOT / "docs" / "history" / "card-plans" / "closed"
PLAN = CLOSED / "能量系统余量详细计划.md"
GAP = ROOT / "docs" / "current" / "unimplemented-gap.md"
CONVERTERS = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "energy_converters.json"
)
COAL_COKE_MATERIAL = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "materials"
    / "coal_coke.json"
)
MOD_BLOCKS = (
    ROOT
    / "src"
    / "main"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "registry"
    / "ModBlocks.java"
)
MOD_ITEMS = (
    ROOT
    / "src"
    / "main"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "registry"
    / "ModItems.java"
)
GUIDE = ROOT / "docs" / "current" / "player-guide.md"
CENSUS = ROOT / "tools" / "census_excluded_object_reclaim.json"
FEASIBILITY = (
    ROOT
    / "tools"
    / "waves"
    / "portfolio"
    / "exclusion-reclaim-r0"
    / "feasibility.json"
)
HEAT_SOURCES = (
    ROOT
    / "src"
    / "main"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "gametest"
    / "GameTestHeatSources.java"
)
LANG = (
    ROOT
    / "src"
    / "main"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "datagen"
    / "ModLanguageProvider.java"
)


class EnergyRemainderCardTest(unittest.TestCase):
    def test_closed_plan_stays_archived(self) -> None:
        names = sorted(path.name for path in ACTIVE.iterdir() if path.is_file())
        self.assertEqual(["电池详细计划.md"], names)
        self.assertTrue(PLAN.is_file())
        self.assertTrue((CLOSED / "显示CPU详细计划.md").is_file())
        self.assertTrue((CLOSED / "能量转换机目录详细计划.md").is_file())

    def test_plan_pins_catalog_and_early_artifact_policy(self) -> None:
        text = PLAN.read_text(encoding="utf-8")
        self.assertIn("converter_catalog_policy", text)
        self.assertIn("battery_policy", text)
        self.assertIn("transformer_policy", text)
        self.assertIn("Burning Boxes", text)
        self.assertIn("machine_kinds.json", text)
        self.assertIn("coal_coke/gem", text)
        self.assertIn("burning_gas_generator", text)
        self.assertIn("先加档位再换机器", text)
        self.assertIn("燃气燃烧室", text)
        self.assertIn("邻接 HU 测试不改 `insert`", text)
        self.assertIn("MultiTileEntityBattery", text)
        self.assertIn("验收结果", text)
        catalog = (CLOSED / "能量转换机目录详细计划.md").read_text(
            encoding="utf-8"
        )
        self.assertIn("energy/converter-catalog", catalog)
        self.assertIn("energy_converter_kinds.json", catalog)
        self.assertIn("Loader_MultiTileEntities", catalog)
        self.assertIn("bronze_burning_box_gas", catalog)
        self.assertIn("不改 `insert`", catalog)
        self.assertNotIn("raw.githubusercontent.com", text)

    def test_human_ledger_keeps_remainder_closeout(self) -> None:
        text = GAP.read_text(encoding="utf-8")
        self.assertIn("能量系统余量", text)
        self.assertIn("converter_catalog_policy", text)
        self.assertIn("battery_policy", text)
        self.assertIn("transformer_policy", text)
        self.assertIn("固体燃料燃烧室已删除", text)

    def test_player_guide_drops_firebox_first_hour(self) -> None:
        text = GUIDE.read_text(encoding="utf-8")
        self.assertIn("固体燃料燃烧室已删除", text)
        self.assertIn("燃气燃烧室", text)
        self.assertIn("bronze_burning_box_gas", text)
        self.assertNotIn("cruciblecraft:firebox", text)
        self.assertNotIn("cruciblecraft:bellows", text)


class EnergyRemainderRuntimeLockTest(unittest.TestCase):
    def test_live_catalog_has_no_firebox_profile(self) -> None:
        catalog = io.load_json(CONVERTERS)
        ids = [row["id"] for row in catalog["profiles"]]
        self.assertNotIn("cruciblecraft:bronze_firebox", ids)
        self.assertIn("cruciblecraft:bronze_boiler", ids)
        self.assertIn("cruciblecraft:bronze_gas_generator", ids)

    def test_coal_coke_gem_is_not_the_unique_item(self) -> None:
        material = io.load_json(COAL_COKE_MATERIAL)
        gem = material.get("form_items", {}).get("gem")
        self.assertNotEqual("cruciblecraft:coal_coke", gem)
        self.assertTrue(COAL_COKE_MATERIAL.is_file())
        items = MOD_ITEMS.read_text(encoding="utf-8")
        self.assertNotIn('registerSimpleItem("coal_coke"', items)

    def test_firebox_and_bellows_are_unregistered(self) -> None:
        blocks = MOD_BLOCKS.read_text(encoding="utf-8")
        self.assertNotIn("FIREBOX", blocks)
        self.assertNotIn("BELLOWS", blocks)
        self.assertNotIn("FireboxBlock", blocks)
        self.assertNotIn("BellowsBlock", blocks)

    def test_adjacent_hu_fixture_stays_gas_burning_box(self) -> None:
        text = HEAT_SOURCES.read_text(encoding="utf-8")
        self.assertIn("bronze_burning_box_gas", text)
        self.assertNotIn("BURNING_GAS_GENERATOR", text)
        self.assertNotIn("FIREBOX", text)
        self.assertNotIn("insert", text.lower())
        lang = LANG.read_text(encoding="utf-8")
        self.assertIn("燃气燃烧室", lang)
        self.assertIn("Gas Burning Box", lang)

    def test_batteries_stay_unimplemented(self) -> None:
        census = io.load_json(CENSUS)
        batteries = next(
            row
            for row in census["category_summaries"]
            if row["category"] == "Batteries"
        )
        self.assertEqual(37, batteries["source_sites"])
        self.assertEqual(37, batteries["expanded_multiplicity"])
        feasibility = io.load_json(FEASIBILITY)
        row = next(
            item
            for item in feasibility["categories"]
            if item["category"] == "Batteries"
        )
        self.assertEqual("requires_new_runtime", row["verdict"])

    def test_exclusion_wave_stays_unassigned(self) -> None:
        spec = spec_for("portfolio/exclusion-reclaim-r0")
        self.assertIsNone(spec.unique_active_wave)
        self.assertTrue(spec.next_unassigned)


if __name__ == "__main__":
    unittest.main()
