#!/usr/bin/env python3
"""GT6 redstone-wire behavior correction on the closed identity hosts."""
from __future__ import annotations

import unittest

from tools import capability_ledger as ledger
from tools import census_common as census

SLUG = "content/gt6-redstone-wire-correction"
ROOT = census.ROOT
WAVE = ROOT / "tools" / "waves" / "content" / "gt6-redstone-wire-correction"
CAPABILITY = (
    ROOT
    / "tools"
    / "capabilities"
    / "content"
    / "gt6-redstone-wire-correction"
    / "capability.json"
)
PLAN_ACTIVE = (
    ROOT / "docs" / "history" / "card-plans" / "active" / "GT6红石线行为校正详细计划.md"
)
PLAN_CLOSED = (
    ROOT / "docs" / "history" / "card-plans" / "closed" / "GT6红石线行为校正详细计划.md"
)
IDENTITY_PLAN_ACTIVE = (
    ROOT / "docs" / "history" / "card-plans" / "active" / "MTE红石线详细计划.md"
)
IDENTITY_CAPABILITY = (
    ROOT / "tools" / "capabilities" / "content" / "mte-redstone-wire" / "capability.json"
)
BE = (
    ROOT
    / "src"
    / "main"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "content"
    / "blockentity"
    / "RedstoneWireBlockEntity.java"
)
SINKS = (
    ROOT
    / "src"
    / "main"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "content"
    / "redstonewire"
    / "RedstoneWireSinks.java"
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
    / "RedstoneWireCorrectionGameTests.java"
)
IDENTITY_GAME_TESTS = (
    ROOT
    / "src"
    / "test"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "gametest"
    / "RedstoneWireGameTests.java"
)
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


class Gt6RedstoneWireCorrectionTest(unittest.TestCase):
    def test_capability_depends_on_closed_identity_card(self) -> None:
        capability = census.load_json(CAPABILITY)
        identity = census.load_json(IDENTITY_CAPABILITY)
        self.assertEqual(SLUG, capability["slug"])
        self.assertEqual(["content/mte-redstone-wire"], capability["depends_on"])
        self.assertEqual(["capability-runtime"], capability["profiles"])
        self.assertEqual("runtime_ready", capability["maturity"])
        self.assertNotEqual("player_complete", capability["maturity"])
        self.assertEqual("accepted", identity["workflow"])
        self.assertFalse(IDENTITY_PLAN_ACTIVE.is_file())
        self.assertEqual(
            [
                "foldedOntoWireGt01NotVanilla",
                "lumiumEmitsLight",
                "noSurvivalRecipes",
                "threeIdentitiesAreRegistered",
                "weakAndStrongRedstone",
            ],
            identity["required_test_ids"],
        )
        self.assertEqual(
            [
                "redstoneMixedMaterialUsesSenderLoss",
                "redstoneNotCableBlock",
                "redstoneSinksIgnored",
                "redstoneVanillaInputCachedPerTick",
            ],
            sorted(capability["required_test_ids"]),
        )
        self.assertNotIn("weakAndStrongRedstone", capability["required_test_ids"])
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

    def test_sender_loss_cache_sinks_and_isolation(self) -> None:
        be = BE.read_text(encoding="utf-8")
        sinks = SINKS.read_text(encoding="utf-8")
        tests = GAME_TESTS.read_text(encoding="utf-8")
        identity_tests = IDENTITY_GAME_TESTS.read_text(encoding="utf-8")
        capabilities = CAPABILITIES_JAVA.read_text(encoding="utf-8")
        self.assertIn("return other.minusLoss();", be)
        self.assertNotIn("other.redstone - kind.loss()", be)
        self.assertIn("Arrays.fill(wire.vanillaSides, -1);", be)
        self.assertIn("RedstoneWireSinks.isSink", be)
        self.assertIn("Blocks.TNT", sinks)
        self.assertIn("Blocks.DISPENSER", sinks)
        self.assertIn("Blocks.NOTE_BLOCK", sinks)
        self.assertIn("DoorBlock", sinks)
        self.assertNotIn("CableNetworkTraversal", be)
        self.assertNotIn("PipeTopology", be)
        self.assertNotIn("ModBlockEntities.REDSTONE_WIRE", capabilities)
        self.assertIn("redstoneMixedMaterialUsesSenderLoss", tests)
        self.assertIn("redstoneVanillaInputCachedPerTick", tests)
        self.assertIn("redstoneSinksIgnored", tests)
        self.assertIn("redstoneNotCableBlock", tests)
        self.assertNotIn("redstoneMixedMaterialUsesSenderLoss", identity_tests)
        notes = census.load_json(WAVE / "runtime_notes.json")
        self.assertTrue(notes["sender_loss"])
        self.assertTrue(notes["vanilla_cache"])
        self.assertTrue(notes["sinks_mapped"])
        self.assertFalse(notes["energy_capability"])
        self.assertFalse(notes["cable_network_traversal"])
        self.assertFalse(notes["pipe_topology"])
        self.assertFalse(notes["reopens_identity_card"])
        self.assertEqual("explicitly_blocked", notes["obtain"])
        self.assertEqual("runtime_ready", notes["close_target"])

    def test_execution_subset_does_not_rewrite_baseline(self) -> None:
        subset = census.load_json(WAVE / "execution_subset.json")
        baseline = census.load_json(
            ROOT
            / "tools"
            / "waves"
            / "content"
            / "gt6-pipe-cable-baseline"
            / "identity_resolution_ledger.json"
        )
        self.assertEqual(SLUG, subset["capability_slug"])
        self.assertEqual(
            {27000, 27050, 27500},
            {int(row["meta"]) for row in subset["rows"]},
        )
        by_meta = {int(row["meta"]): row for row in baseline["rows"]}
        self.assertEqual("already_shared", by_meta[27000]["disposition"])
        self.assertEqual("content/mte-redstone-wire", by_meta[27000]["identity_owner"])
        self.assertEqual(SLUG, by_meta[27000]["child_owner"])
        ns = (
            ROOT
            / "src"
            / "main"
            / "resources"
            / "data"
            / "cruciblecraft_wave_content_gt6_redstone_wire_correction"
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
