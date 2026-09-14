#!/usr/bin/env python3
"""GT6 connector survival-obtain children share one python module."""
from __future__ import annotations

import unittest

from pathlib import Path

from tools import capability_ledger as ledger
from tools import census_common as census
from tools import gt6_connector_acquisition as runtime

SLUG = "content/gt6-fluid-pipe-acquisition"
ITEM_SLUG = "content/gt6-item-pipe-acquisition"
ROOT = census.ROOT
WAVE = ROOT / "tools" / "waves" / "content" / "gt6-fluid-pipe-acquisition"
CAPABILITY = (
    ROOT
    / "tools"
    / "capabilities"
    / "content"
    / "gt6-fluid-pipe-acquisition"
    / "capability.json"
)
ITEM_CAPABILITY = (
    ROOT
    / "tools"
    / "capabilities"
    / "content"
    / "gt6-item-pipe-acquisition"
    / "capability.json"
)
PLAN_ACTIVE = (
    ROOT
    / "docs"
    / "history"
    / "card-plans"
    / "active"
    / "GT6流体管获得格详细计划.md"
)
PLAN_CLOSED = (
    ROOT
    / "docs"
    / "history"
    / "card-plans"
    / "closed"
    / "GT6流体管获得格详细计划.md"
)
ITEM_PLAN_ACTIVE = (
    ROOT
    / "docs"
    / "history"
    / "card-plans"
    / "active"
    / "GT6物品管获得格详细计划.md"
)
ITEM_PLAN_CLOSED = (
    ROOT
    / "docs"
    / "history"
    / "card-plans"
    / "closed"
    / "GT6物品管获得格详细计划.md"
)
EU_SLUG = "content/gt6-eu-cable-acquisition"
EU_CAPABILITY = (
    ROOT
    / "tools"
    / "capabilities"
    / "content"
    / "gt6-eu-cable-acquisition"
    / "capability.json"
)
EU_PLAN_ACTIVE = (
    ROOT
    / "docs"
    / "history"
    / "card-plans"
    / "active"
    / "GT6EU线缆获得格详细计划.md"
)
EU_PLAN_CLOSED = (
    ROOT
    / "docs"
    / "history"
    / "card-plans"
    / "closed"
    / "GT6EU线缆获得格详细计划.md"
)
REDSTONE_SLUG = "content/gt6-redstone-wire-acquisition"
REDSTONE_CAPABILITY = (
    ROOT
    / "tools"
    / "capabilities"
    / "content"
    / "gt6-redstone-wire-acquisition"
    / "capability.json"
)
REDSTONE_PLAN_ACTIVE = (
    ROOT
    / "docs"
    / "history"
    / "card-plans"
    / "active"
    / "GT6绝缘红石获得格详细计划.md"
)
REDSTONE_PLAN_CLOSED = (
    ROOT
    / "docs"
    / "history"
    / "card-plans"
    / "closed"
    / "GT6绝缘红石获得格详细计划.md"
)


class Gt6ConnectorAcquisitionTest(unittest.TestCase):
    def test_fluid_capability_depends_on_live_combo(self) -> None:
        capability = census.load_json(CAPABILITY)
        self.assertEqual(SLUG, capability["slug"])
        self.assertEqual(
            [
                "registry/catalog-modern-ids",
                "content/gt6-fluid-pipe-runtime",
                "content/gt6-fluid-combo-pipe-runtime",
            ],
            capability["depends_on"],
        )
        self.assertNotIn(
            "content/gt6-pipe-cable-baseline", capability["depends_on"]
        )
        self.assertEqual(["capability-runtime"], capability["profiles"])
        self.assertNotEqual("player_complete", capability["maturity"])
        self.assertEqual(
            runtime.EXPECTED_TESTS,
            sorted(capability["required_test_ids"]),
        )
        compiled = ledger.compile_ledger()
        if capability["workflow"] == "active":
            self.assertEqual(SLUG, compiled["unique_active_slug"])
            self.assertTrue(PLAN_ACTIVE.is_file())
            self.assertFalse(PLAN_CLOSED.is_file())
        else:
            self.assertEqual("accepted", capability["workflow"])
            self.assertNotEqual(SLUG, compiled["unique_active_slug"])
            self.assertFalse(PLAN_ACTIVE.is_file())
            self.assertTrue(PLAN_CLOSED.is_file())

    def test_combo_unpack_and_no_plate_stand_in(self) -> None:
        errors = runtime.check()
        self.assertEqual([], errors)
        overlay = census.load_json(runtime.OVERLAY_PATH)
        self.assertEqual("fluid", overlay["domain"])
        self.assertEqual(overlay["combo_pack_recipes"], overlay["combo_unpack_recipes"])
        self.assertGreater(overlay["combo_pack_recipes"], 0)
        self.assertTrue(overlay["copper_tiny_table"])
        self.assertTrue(overlay["copper_huge_table"])
        self.assertIn("curved_plate", overlay["five_gauge_table_operands"])
        self.assertIn("double_plate", overlay["five_gauge_table_operands"])
        self.assertNotIn("plate", overlay["five_gauge_table_operands"])
        provider = runtime.RECIPE_PROVIDER.read_text(encoding="utf-8")
        self.assertIn("unpack_quadruple_fluid_pipe", provider)
        self.assertIn("CURVED_PLATE", provider)
        self.assertIn("DOUBLE_PLATE", provider)
        tests = runtime.GAME_TESTS.read_text(encoding="utf-8")
        self.assertIn("copperTinyTableCraftsFromCurvedPlate", tests)
        self.assertIn("fiveGaugeTableDoesNotUseFlatPlate", tests)
        self.assertNotIn("from tools import gt6_fluid" + "_pipe_runtime", Path(runtime.__file__).read_text(encoding="utf-8"))

    def test_item_capability_depends_on_live_restrictive(self) -> None:
        capability = census.load_json(ITEM_CAPABILITY)
        self.assertEqual(ITEM_SLUG, capability["slug"])
        self.assertEqual(
            [
                "registry/catalog-modern-ids",
                "content/gt6-item-pipe-runtime",
                "content/gt6-restrictive-item-pipe-runtime",
            ],
            capability["depends_on"],
        )
        self.assertNotIn(
            "content/gt6-pipe-cable-baseline", capability["depends_on"]
        )
        self.assertEqual(["capability-runtime"], capability["profiles"])
        self.assertNotEqual("player_complete", capability["maturity"])
        self.assertEqual(
            runtime.ITEM_EXPECTED_TESTS,
            sorted(capability["required_test_ids"]),
        )
        compiled = ledger.compile_ledger()
        if capability["workflow"] == "active":
            self.assertEqual(ITEM_SLUG, compiled["unique_active_slug"])
            self.assertTrue(ITEM_PLAN_ACTIVE.is_file())
            self.assertFalse(ITEM_PLAN_CLOSED.is_file())
        else:
            self.assertEqual("accepted", capability["workflow"])
            self.assertNotEqual(ITEM_SLUG, compiled["unique_active_slug"])
            self.assertFalse(ITEM_PLAN_ACTIVE.is_file())
            self.assertTrue(ITEM_PLAN_CLOSED.is_file())

    def test_item_table_and_restrictive_use_gt6_operands(self) -> None:
        if not runtime.ITEM_OVERLAY_PATH.is_file():
            self.skipTest("item overlay not written yet")
        errors = runtime.check()
        self.assertEqual([], errors)
        overlay = census.load_json(runtime.ITEM_OVERLAY_PATH)
        self.assertEqual("item", overlay["domain"])
        self.assertTrue(overlay["copper_medium_table"])
        self.assertTrue(overlay["copper_huge_table"])
        self.assertTrue(overlay["copper_restrictive"])
        self.assertIn("curved_plate", overlay["item_table_operands"])
        self.assertIn("double_plate", overlay["item_table_operands"])
        self.assertNotIn("plate", overlay["item_table_operands"])
        self.assertGreater(overlay["restrictive_live"], 0)
        provider = runtime.RECIPE_PROVIDER.read_text(encoding="utf-8")
        self.assertIn("addMetalItemPipeTableRecipes", provider)
        self.assertIn("addRestrictiveItemPipeRecipes", provider)
        self.assertIn("pipe/item_table/", provider)
        tests = runtime.ITEM_GAME_TESTS.read_text(encoding="utf-8")
        self.assertIn("copperMediumTableCraftsFromCurvedPlate", tests)
        self.assertIn("restrictiveUsesSteelRingNotInvented", tests)
        self.assertNotIn(
            "from tools import gt6_item" + "_pipe_runtime",
            Path(runtime.__file__).read_text(encoding="utf-8"),
        )

    def test_eu_capability_depends_on_live_conductors(self) -> None:
        if not EU_CAPABILITY.is_file():
            self.skipTest("eu capability not issued yet")
        capability = census.load_json(EU_CAPABILITY)
        self.assertEqual(EU_SLUG, capability["slug"])
        self.assertEqual(
            [
                "registry/catalog-modern-ids",
                "content/gt6-eu-wire-cable-runtime",
                "content/gt6-eu-missing-wire-gauges-runtime",
            ],
            capability["depends_on"],
        )
        self.assertNotIn(
            "content/gt6-pipe-cable-baseline", capability["depends_on"]
        )
        self.assertEqual(["capability-runtime"], capability["profiles"])
        self.assertNotEqual("player_complete", capability["maturity"])
        self.assertEqual(
            runtime.EU_EXPECTED_TESTS,
            sorted(capability["required_test_ids"]),
        )
        compiled = ledger.compile_ledger()
        if capability["workflow"] == "active":
            self.assertEqual(EU_SLUG, compiled["unique_active_slug"])
            self.assertTrue(EU_PLAN_ACTIVE.is_file())
            self.assertFalse(EU_PLAN_CLOSED.is_file())
        else:
            self.assertEqual("accepted", capability["workflow"])
            self.assertNotEqual(EU_SLUG, compiled["unique_active_slug"])
            self.assertFalse(EU_PLAN_ACTIVE.is_file())
            self.assertTrue(EU_PLAN_CLOSED.is_file())

    def test_eu_plate2wire_and_rubber_not_red_alloy(self) -> None:
        if not runtime.EU_OVERLAY_PATH.is_file():
            self.skipTest("eu overlay not written yet")
        errors = runtime.check()
        self.assertEqual([], errors)
        overlay = census.load_json(runtime.EU_OVERLAY_PATH)
        self.assertEqual("eu", overlay["domain"])
        self.assertTrue(overlay["copper_plate2wire"])
        self.assertTrue(overlay["copper_shapeless_cable"])
        self.assertTrue(overlay["copper_pack_double"])
        self.assertTrue(overlay["copper_unpack_double"])
        self.assertFalse(overlay["red_alloy_eu_table"])
        self.assertGreater(overlay["plate2wire_live"], 0)
        provider = runtime.RECIPE_PROVIDER.read_text(encoding="utf-8")
        self.assertIn("addEuWireTableRecipes", provider)
        self.assertIn("addEuCableShapelessRecipes", provider)
        self.assertIn("any_rubber_plates", provider)
        tests = runtime.EU_GAME_TESTS.read_text(encoding="utf-8")
        self.assertIn("copperPlateCraftsWireWithCutter", tests)
        self.assertIn("redAlloyHasNoEuPlate2wire", tests)
        self.assertNotIn(
            "from tools import gt6_eu" + "_wire_cable_runtime",
            Path(runtime.__file__).read_text(encoding="utf-8"),
        )

    def test_redstone_capability_depends_on_insulated_runtime(self) -> None:
        if not REDSTONE_CAPABILITY.is_file():
            self.skipTest("redstone capability not issued yet")
        capability = census.load_json(REDSTONE_CAPABILITY)
        self.assertEqual(REDSTONE_SLUG, capability["slug"])
        self.assertEqual(
            [
                "registry/catalog-modern-ids",
                "content/gt6-insulated-redstone-runtime",
            ],
            capability["depends_on"],
        )
        self.assertNotIn(
            "content/gt6-pipe-cable-baseline", capability["depends_on"]
        )
        self.assertEqual(["capability-runtime"], capability["profiles"])
        self.assertNotEqual("player_complete", capability["maturity"])
        self.assertEqual(
            runtime.REDSTONE_EXPECTED_TESTS,
            sorted(capability["required_test_ids"]),
        )
        compiled = ledger.compile_ledger()
        if capability["workflow"] == "active":
            self.assertEqual(REDSTONE_SLUG, compiled["unique_active_slug"])
            self.assertTrue(REDSTONE_PLAN_ACTIVE.is_file())
            self.assertFalse(REDSTONE_PLAN_CLOSED.is_file())
        else:
            self.assertEqual("accepted", capability["workflow"])
            self.assertNotEqual(REDSTONE_SLUG, compiled["unique_active_slug"])
            self.assertFalse(REDSTONE_PLAN_ACTIVE.is_file())
            self.assertTrue(REDSTONE_PLAN_CLOSED.is_file())

    def test_redstone_laminator_not_tin_or_circuit(self) -> None:
        if not runtime.REDSTONE_OVERLAY_PATH.is_file():
            self.skipTest("redstone overlay not written yet")
        errors = runtime.check()
        self.assertEqual([], errors)
        overlay = census.load_json(runtime.REDSTONE_OVERLAY_PATH)
        self.assertEqual("redstone", overlay["domain"])
        self.assertTrue(overlay["red_alloy_plate"])
        self.assertTrue(overlay["signalum_plate"])
        self.assertTrue(overlay["lumium_plate"])
        self.assertEqual(3, overlay["plate_live"])
        provider = runtime.RECIPE_PROVIDER.read_text(encoding="utf-8")
        self.assertIn("addInsulatedRedstoneLaminatorRecipes", provider)
        self.assertIn("redstone/laminator/", provider)
        tests = runtime.REDSTONE_GAME_TESTS.read_text(encoding="utf-8")
        self.assertIn("redAlloyCableLaminatesFromRubberPlate", tests)
        self.assertIn("insulatedCablesAreNotEuTinOutputs", tests)
        self.assertNotIn(
            "from tools import gt6_insulated" + "_redstone_runtime",
            Path(runtime.__file__).read_text(encoding="utf-8"),
        )
