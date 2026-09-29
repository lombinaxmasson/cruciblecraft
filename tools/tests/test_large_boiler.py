#!/usr/bin/env python3
"""Large Boiler 17201-17205 unique-active card."""
from __future__ import annotations

import unittest

from tools import capability_ledger as ledger
from tools import census_common as census
from tools import io_common as io

SLUG = "machines/large-boiler"
ROOT = io.ROOT
CAPABILITY = (
    ROOT / "tools" / "capabilities" / "machines" / "large-boiler" / "capability.json"
)
PLAN_ACTIVE = ROOT / "docs" / "history" / "card-plans" / "active" / "大型锅炉详细计划.md"
PLAN_CLOSED = ROOT / "docs" / "history" / "card-plans" / "closed" / "大型锅炉详细计划.md"
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
    / "EnergyConverterCatalogGameTests.java",
)


class LargeBoilerCardTest(unittest.TestCase):
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
        elif capability["workflow"] == "paused":
            self.assertEqual("runtime_ready", capability["maturity"])
            self.assertNotEqual(SLUG, compiled["unique_active_slug"])
            self.assertFalse(PLAN_ACTIVE.is_file())
            self.assertTrue(PLAN_CLOSED.is_file())
        else:
            self.assertEqual("accepted", capability["workflow"])
            self.assertEqual("runtime_ready", capability["maturity"])
            self.assertNotEqual(SLUG, compiled["unique_active_slug"])
            self.assertFalse(PLAN_ACTIVE.is_file())
            self.assertTrue(PLAN_CLOSED.is_file())
        self.assertNotIn("player_complete", capability.get("note") or "")
