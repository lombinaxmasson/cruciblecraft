#!/usr/bin/env python3
"""GT6 ceramic crucible / mold interaction on the live hosts."""
from __future__ import annotations

import unittest

from tools import capability_ledger as ledger
from tools import census_common as census

SLUG = "content/gt6-crucible-mold-interaction"
ROOT = census.ROOT
WAVE = ROOT / "tools" / "waves" / "content" / "gt6-crucible-mold-interaction"
CAPABILITY = (
    ROOT
    / "tools"
    / "capabilities"
    / "content"
    / "gt6-crucible-mold-interaction"
    / "capability.json"
)
CORRECTION = (
    ROOT
    / "tools"
    / "capabilities"
    / "content"
    / "gt6-crucible-mold-behavior-correction"
    / "capability.json"
)
PLAN_ACTIVE = (
    ROOT / "docs" / "history" / "card-plans" / "active" / "GT6坩埚模具交互详细计划.md"
)
PLAN_CLOSED = (
    ROOT / "docs" / "history" / "card-plans" / "closed" / "GT6坩埚模具交互详细计划.md"
)
MOLD = (
    ROOT
    / "src"
    / "main"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "content"
    / "blockentity"
    / "CeramicMoldBlockEntity.java"
)
MOLD_BLOCK = (
    ROOT
    / "src"
    / "main"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "content"
    / "block"
    / "CeramicMoldBlock.java"
)
CRUCIBLE = (
    ROOT
    / "src"
    / "main"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "content"
    / "blockentity"
    / "CrucibleBlockEntity.java"
)
CRUCIBLE_BLOCK = (
    ROOT
    / "src"
    / "main"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "content"
    / "block"
    / "MteInPlaceBlock.java"
)
CORE = (
    ROOT
    / "src"
    / "main"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "machine"
    / "component"
    / "CrucibleProcessCore.java"
)
INPLACE = (
    ROOT
    / "src"
    / "main"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "content"
    / "blockentity"
    / "MteInPlaceBlockEntity.java"
)
THERMOMETER = (
    ROOT
    / "src"
    / "main"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "content"
    / "item"
    / "ThermometerItem.java"
)
GAME_TESTS = (
    ROOT
    / "src"
    / "test"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "gametest"
    / "CrucibleMoldInteractionGameTests.java"
)
CORRECTION_TESTS = (
    ROOT
    / "src"
    / "test"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "gametest"
    / "CrucibleMoldBehaviorCorrectionGameTests.java"
)
REQUIRED_IDS = [
    "crucibleMeltdownBecomesLava",
    "emptyHandPickupCanBurn",
    "faucetPoursCeramicCrucibleIntoMold",
    "fillMoldConsumesOneIngot",
    "moldChiselORsBitAndCastsNuggets",
    "moldContactBurns",
    "moldCuCoolsTemperature",
    "moldTopEdgePoursOnlyThatNeighbor",
    "pincersPickupSkipsHeatDamage",
    "wrenchRotatesIngotRecipe",
]
CORRECTION_IDS = [
    "crucibleAcceptsSixteenIngots",
    "crucibleHopperInsertsFromTop",
    "crucibleSucksDroppedIngot",
    "foundryTankCapacitiesMatchGt6Units",
    "moldAutoPullsWhenSideEnabled",
    "moldDoesNotAutoPullByDefault",
    "moldHopperExtractsWhenCool",
    "moldRedstoneGatesAutoPull",
]


class Gt6CrucibleMoldInteractionTest(unittest.TestCase):
    def test_capability_depends_on_closed_correction_card(self) -> None:
        capability = census.load_json(CAPABILITY)
        correction = census.load_json(CORRECTION)
        self.assertEqual(SLUG, capability["slug"])
        self.assertEqual(
            ["content/gt6-crucible-mold-behavior-correction"],
            capability["depends_on"],
        )
        self.assertEqual(["capability-runtime"], capability["profiles"])
        self.assertEqual("runtime_ready", capability["maturity"])
        self.assertNotEqual("player_complete", capability["maturity"])
        self.assertEqual("accepted", correction["workflow"])
        self.assertEqual(CORRECTION_IDS, sorted(correction["required_test_ids"]))
        self.assertEqual(REQUIRED_IDS, sorted(capability["required_test_ids"]))
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

    def test_fill_mold_chisel_heat_and_faucet_are_wired(self) -> None:
        mold = MOLD.read_text(encoding="utf-8")
        mold_block = MOLD_BLOCK.read_text(encoding="utf-8")
        crucible = CRUCIBLE.read_text(encoding="utf-8")
        crucible_block = CRUCIBLE_BLOCK.read_text(encoding="utf-8")
        core = CORE.read_text(encoding="utf-8")
        inplace = INPLACE.read_text(encoding="utf-8")
        thermometer = THERMOMETER.read_text(encoding="utf-8")
        tests = GAME_TESTS.read_text(encoding="utf-8")
        correction_tests = CORRECTION_TESTS.read_text(encoding="utf-8")
        self.assertIn("implements MoldHost", mold)
        self.assertIn("EnergyType.CU", mold)
        self.assertIn("CHISEL", mold_block)
        self.assertIn("PINCERS", mold_block)
        self.assertIn("fillMoldAtSide", core)
        self.assertIn("CruciblePour", crucible)
        self.assertIn("fillMoldAtSide", crucible)
        self.assertIn("Blocks.LAVA", crucible)
        self.assertNotIn("destroyBlock(pos, false)", crucible)
        self.assertIn("TemperatureDamage", crucible_block)
        self.assertIn("pourFaucet", inplace)
        self.assertIn("CruciblePour", inplace)
        self.assertIn("thermometer_kelvin", thermometer)
        self.assertIn("CrucibleBlockEntity", thermometer)
        notes = census.load_json(WAVE / "runtime_notes.json")
        self.assertTrue(notes["fill_mold"])
        self.assertTrue(notes["chisel_shape"])
        self.assertTrue(notes["faucet_fill_mold_ceramic"])
        self.assertTrue(notes["faucet_still_pours_generic_fluid_when_not_crucible"])
        self.assertFalse(notes["foundry_tanks_remain_dummy"])
        self.assertFalse(notes["reopens_foundry_identity"])
        self.assertFalse(notes["reopens_correction_required_test_ids"])
        self.assertEqual("explicitly_blocked", notes["obtain"])
        self.assertEqual("runtime_ready", notes["close_target"])
        for test_id in REQUIRED_IDS:
            self.assertIn(test_id, tests)
            self.assertNotIn(test_id, correction_tests)
        for test_id in CORRECTION_IDS:
            self.assertIn(test_id, correction_tests)
            self.assertNotIn(test_id, tests)

    def test_execution_subset_and_structure(self) -> None:
        subset = census.load_json(WAVE / "execution_subset.json")
        self.assertEqual(SLUG, subset["capability_slug"])
        self.assertEqual(
            {
                "cruciblecraft:foundry/smelting_crucible_steel",
                "cruciblecraft:ceramic_mold",
                "cruciblecraft:fluid_attachment/crucible_faucet_stone",
            },
            {row["live_block"] for row in subset["rows"]},
        )
        ns = (
            ROOT
            / "src"
            / "main"
            / "resources"
            / "data"
            / "cruciblecraft_wave_content_gt6_crucible_mold_interaction"
        )
        self.assertTrue((ns / "structure" / "empty.nbt").is_file())
        self.assertTrue((ns / "gametest" / "structure" / "empty.nbt").is_file())

    def test_unique_active_docs_and_ledger_hand_off(self) -> None:
        topology = census.load_json(WAVE / "topology.json")
        readiness = census.load_json(WAVE / "readiness.json")
        compiled = ledger.compile_ledger()
        capability = census.load_json(CAPABILITY)
        if capability["workflow"] == "active":
            self.assertEqual(SLUG, compiled["unique_active_slug"])
            self.assertEqual(SLUG, topology["unique_active_wave"])
            self.assertEqual(SLUG, readiness["unique_active_wave"])
        else:
            self.assertNotEqual(SLUG, compiled["unique_active_slug"])
            self.assertIsNone(topology["unique_active_wave"])
            self.assertIsNone(readiness["unique_active_wave"])


if __name__ == "__main__":
    unittest.main()
