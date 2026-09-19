#!/usr/bin/env python3
"""GT6 ceramic crucible / mold behavior correction on the live hosts."""
from __future__ import annotations

import unittest

from tools import capability_ledger as ledger
from tools import census_common as census

SLUG = "content/gt6-crucible-mold-behavior-correction"
ROOT = census.ROOT
WAVE = ROOT / "tools" / "waves" / "content" / "gt6-crucible-mold-behavior-correction"
CAPABILITY = (
    ROOT
    / "tools"
    / "capabilities"
    / "content"
    / "gt6-crucible-mold-behavior-correction"
    / "capability.json"
)
PLAN_ACTIVE = (
    ROOT / "docs" / "history" / "card-plans" / "active" / "GT6坩埚模具行为校正详细计划.md"
)
PLAN_CLOSED = (
    ROOT / "docs" / "history" / "card-plans" / "closed" / "GT6坩埚模具行为校正详细计划.md"
)
FOUNDRY_CAPABILITY = (
    ROOT
    / "tools"
    / "capabilities"
    / "content"
    / "gt6-mte-crucible-foundry-runtime"
    / "capability.json"
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
GAME_TESTS = (
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
FOUNDRY_TESTS = (
    ROOT
    / "src"
    / "test"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "gametest"
    / "MteCrucibleFoundryRuntimeGameTests.java"
)
FOUNDRY_TANKS = (
    ROOT
    / "src"
    / "main"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "content"
    / "mte"
    / "MteFoundryTanks.java"
)
INPLACE_BE = (
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
INPLACE_BLOCK = (
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
GENERATOR = ROOT / "tools" / "gt6_mte_inplace_runtime.py"
FOUNDRY_OVERLAY = (
    ROOT
    / "tools"
    / "waves"
    / "content"
    / "gt6-mte-crucible-foundry-runtime"
    / "runtime_overlay.json"
)
FOUNDRY_CAP_BY_CLASS = {
    "MultiTileEntitySmeltery / Smelting Crucibles": 2304,
    "MultiTileEntityMold / Molds": 144,
    "MultiTileEntityBasin / Molds": 1296,
    "MultiTileEntityCrossing / Molds": 0,
}
CAPABILITIES_JAVA = (
    ROOT
    / "src"
    / "main"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "registry"
    / "ModCapabilities.java"
)
REQUIRED_IDS = [
    "crucibleAcceptsSixteenIngots",
    "crucibleHopperInsertsFromTop",
    "crucibleSucksDroppedIngot",
    "foundryTankCapacitiesMatchGt6Units",
    "moldAutoPullsWhenSideEnabled",
    "moldDoesNotAutoPullByDefault",
    "moldHopperExtractsWhenCool",
    "moldRedstoneGatesAutoPull",
]


class Gt6CrucibleMoldBehaviorCorrectionTest(unittest.TestCase):
    def test_capability_depends_on_closed_foundry_card(self) -> None:
        capability = census.load_json(CAPABILITY)
        foundry = census.load_json(FOUNDRY_CAPABILITY)
        self.assertEqual(SLUG, capability["slug"])
        self.assertEqual(
            ["content/gt6-mte-crucible-foundry-runtime"], capability["depends_on"]
        )
        self.assertEqual(["capability-runtime"], capability["profiles"])
        self.assertEqual("runtime_ready", capability["maturity"])
        self.assertNotEqual("player_complete", capability["maturity"])
        self.assertEqual("accepted", foundry["workflow"])
        self.assertEqual(
            ["foundryCrucibleIsNotCeramicCrucible"],
            foundry["required_test_ids"],
        )
        self.assertEqual(REQUIRED_IDS, sorted(capability["required_test_ids"]))
        self.assertNotIn("foundryCrucibleIsNotCeramicCrucible", capability["required_test_ids"])
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

    def test_ceramic_hosts_and_foundry_tank_millibuckets(self) -> None:
        core = CORE.read_text(encoding="utf-8")
        crucible = CRUCIBLE.read_text(encoding="utf-8")
        mold = MOLD.read_text(encoding="utf-8")
        mold_block = MOLD_BLOCK.read_text(encoding="utf-8")
        tests = GAME_TESTS.read_text(encoding="utf-8")
        foundry_tests = FOUNDRY_TESTS.read_text(encoding="utf-8")
        foundry_tanks = FOUNDRY_TANKS.read_text(encoding="utf-8")
        inplace = INPLACE_BE.read_text(encoding="utf-8")
        inplace_block = INPLACE_BLOCK.read_text(encoding="utf-8")
        generator = GENERATOR.read_text(encoding="utf-8")
        capabilities = CAPABILITIES_JAVA.read_text(encoding="utf-8")
        self.assertIn("SINGLE_BLOCK_MAX_INGOTS = 16", core)
        self.assertNotIn("SINGLE_BLOCK_MAX_INGOTS = 8", core)
        self.assertIn("suckDroppedItems", crucible)
        self.assertIn("TopInsertHandler", crucible)
        self.assertIn("tryAutoPull", mold)
        self.assertIn("toggleAutoPull", mold)
        self.assertIn("canHopperExtract", mold)
        self.assertIn("MONKEY_WRENCH", mold_block)
        self.assertIn("SOFT_HAMMER", mold_block)
        self.assertIn("ModBlockEntities.CRUCIBLE.get()", capabilities)
        self.assertIn("ModBlockEntities.CERAMIC_MOLD.get()", capabilities)
        self.assertNotIn("MteFoundryTanks.capacityMb(spec)", inplace)
        self.assertNotIn("foundryTank()", inplace)
        self.assertIn("FoundryCastingBlockEntity", inplace_block)
        self.assertIn("FoundryCrossingBlockEntity", inplace_block)
        self.assertNotIn("? 8_000", inplace)
        self.assertIn("SMELTERY_MB", foundry_tanks)
        self.assertIn("CROSSING_MB = 0", foundry_tanks)
        self.assertIn("MteFoundryTanks.SMELTERY_MB", foundry_tests)
        self.assertNotIn("getCapacity() == 8000", foundry_tests)
        self.assertIn("MteFoundryTanks.SMELTERY_MB", generator)
        self.assertNotIn("getCapacity() == 8000", generator)
        overlay = census.load_json(FOUNDRY_OVERLAY)
        self.assertEqual(85, int(overlay["counts"]["in_place"]))
        class_counts: dict[str, int] = {}
        for row in overlay["rows"]:
            gt6_class = row["gt6_class"]
            self.assertIn(gt6_class, FOUNDRY_CAP_BY_CLASS)
            class_counts[gt6_class] = class_counts.get(gt6_class, 0) + 1
        self.assertEqual(
            {
                "MultiTileEntitySmeltery / Smelting Crucibles": 19,
                "MultiTileEntityMold / Molds": 22,
                "MultiTileEntityBasin / Molds": 22,
                "MultiTileEntityCrossing / Molds": 22,
            },
            class_counts,
        )
        for test_id in REQUIRED_IDS:
            self.assertIn(test_id, tests)
            self.assertNotIn(test_id, foundry_tests)
        notes = census.load_json(WAVE / "runtime_notes.json")
        self.assertEqual(16, notes["ceramic_capacity_ingots"])
        self.assertTrue(notes["drop_suck"])
        self.assertTrue(notes["top_hopper_insert"])
        self.assertTrue(notes["mold_auto_pull"])
        self.assertTrue(notes["mold_redstone_gate"])
        self.assertTrue(notes["mold_hopper_extract_when_cool"])
        self.assertFalse(notes["foundry_tanks_remain_8000mb"])
        self.assertTrue(notes["foundry_dummy_tanks_removed"])
        self.assertEqual(
            {"smeltery": 2304, "mold": 144, "basin": 1296, "crossing": 0},
            notes["foundry_tank_capacities_mb"],
        )
        self.assertFalse(notes["faucet_fill_mold"])
        self.assertFalse(notes["reopens_foundry_identity"])
        self.assertEqual("explicitly_blocked", notes["obtain"])
        self.assertEqual("runtime_ready", notes["close_target"])

    def test_execution_subset_does_not_rewrite_foundry_identities(self) -> None:
        subset = census.load_json(WAVE / "execution_subset.json")
        self.assertEqual(SLUG, subset["capability_slug"])
        self.assertEqual(
            {"cruciblecraft:foundry/smelting_crucible_steel", "cruciblecraft:ceramic_mold"},
            {row["live_block"] for row in subset["rows"]},
        )
        ns = (
            ROOT
            / "src"
            / "main"
            / "resources"
            / "data"
            / "cruciblecraft_wave_content_gt6_crucible_mold_behavior_correction"
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
