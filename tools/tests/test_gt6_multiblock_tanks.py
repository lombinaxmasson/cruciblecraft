#!/usr/bin/env python3
"""GT6 hollow-cube tanks: 3x3x3 already live, 5x5x5 on the same valve contract."""
from __future__ import annotations

import json
import unittest

from tools import capability_ledger as ledger
from tools import census_common as census
from tools import io_common as io

SLUG = "machines/gt6-multiblock-tanks"
ROOT = io.ROOT
CAPABILITY = (
    ROOT
    / "tools"
    / "capabilities"
    / "machines"
    / "gt6-multiblock-tanks"
    / "capability.json"
)
PLAN_ACTIVE = (
    ROOT / "docs" / "history" / "card-plans" / "active" / "GT6多方块储罐详细计划.md"
)
PLAN_CLOSED = (
    ROOT / "docs" / "history" / "card-plans" / "closed" / "GT6多方块储罐详细计划.md"
)
STRUCTURE = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "multiblock_structures"
    / "tank_5x5x5.json"
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
    / "CrucibleCraftGameTests.java",
    ROOT
    / "src"
    / "test"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "gametest"
    / "Tank5x5GameTests.java",
)


class Gt6MultiblockTanksCardTest(unittest.TestCase):
    def test_capability_occupies_the_open_lane(self) -> None:
        capability = census.load_json(CAPABILITY)
        self.assertEqual(SLUG, capability["slug"])
        self.assertEqual(["capability-runtime"], capability["profiles"])
        self.assertEqual("partial", capability["survival_access"])
        self.assertNotIn("player_complete", capability.get("note") or "")
        compiled = ledger.compile_ledger()
        if capability["workflow"] == "active":
            self.assertEqual("runtime_ready", capability["maturity"])
            self.assertEqual(SLUG, compiled["unique_active_slug"])
            self.assertTrue(PLAN_ACTIVE.is_file())
            self.assertFalse(PLAN_CLOSED.is_file())
        else:
            self.assertEqual("accepted", capability["workflow"])
            self.assertEqual("runtime_ready", capability["maturity"])
            self.assertNotEqual(SLUG, compiled["unique_active_slug"])
            self.assertFalse(PLAN_ACTIVE.is_file())
            self.assertTrue(PLAN_CLOSED.is_file())
        source = "\n".join(
            path.read_text(encoding="utf-8") for path in GAME_TESTS
        )
        for test_id in capability["required_test_ids"]:
            self.assertIn(f"void {test_id}", source)

    def test_five_cube_is_the_gt6_hollow_shell(self) -> None:
        structure = json.loads(STRUCTURE.read_text(encoding="utf-8"))
        counts = {"C": 0, "A": 0, "P": 0}
        for cell in structure["structure"]:
            counts[cell["predicate"]] += 1
        self.assertEqual(125, len(structure["structure"]))
        self.assertEqual({"C": 1, "A": 27, "P": 97}, counts)
        self.assertEqual([0, 0, 2], structure["anchors"]["center"])
        self.assertTrue(
            structure["source"]["class"].endswith("MultiTileEntityTank5x5x5")
        )
