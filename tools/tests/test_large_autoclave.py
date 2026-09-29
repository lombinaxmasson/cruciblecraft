#!/usr/bin/env python3
"""Large Autoclave 17112 unique-active card."""
from __future__ import annotations

import unittest

from tools import capability_ledger as ledger
from tools import census_common as census
from tools import gt6_large_autoclave as autoclave
from tools import io_common as io
from tools.build_recipe_bulk import WAVE_CHOICES
from tools.recipe_bulk.slugs import KNOWN_SEMANTIC_SLUGS
from tools.recipe_bulk.waves import SEMANTIC_COMPILE_ORDER, recipe_wave

SLUG = "machines/large-autoclave"
ROOT = io.ROOT
CAPABILITY = (
    ROOT / "tools" / "capabilities" / "machines" / "large-autoclave" / "capability.json"
)
PLAN_ACTIVE = ROOT / "docs" / "history" / "card-plans" / "active" / "大型高压釜详细计划.md"
PLAN_CLOSED = ROOT / "docs" / "history" / "card-plans" / "closed" / "大型高压釜详细计划.md"
GAME_TESTS = (
    ROOT
    / "src"
    / "test"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "gametest"
    / "LargeAutoclaveGameTests.java"
)
TOPOLOGY = ROOT / "tools" / "waves" / "machines" / "large-autoclave" / "topology.json"


class LargeAutoclaveCardTest(unittest.TestCase):
    def test_slug_is_known_and_does_not_compile_a_dump(self) -> None:
        self.assertIn(SLUG, KNOWN_SEMANTIC_SLUGS)
        self.assertNotIn(SLUG, SEMANTIC_COMPILE_ORDER)
        self.assertNotIn(SLUG, WAVE_CHOICES)
        with self.assertRaises(KeyError):
            recipe_wave(SLUG)

    def test_source_check_passes(self) -> None:
        self.assertEqual([], autoclave.check())

    def test_capability_and_game_tests_are_scoped(self) -> None:
        capability = census.load_json(CAPABILITY)
        self.assertEqual(SLUG, capability["slug"])
        self.assertEqual(["capability-runtime"], capability["profiles"])
        compiled = ledger.compile_ledger()
        topology = census.load_json(TOPOLOGY)
        if capability["workflow"] == "active":
            self.assertEqual("frozen", capability["maturity"])
            self.assertEqual(SLUG, compiled["unique_active_slug"])
            self.assertEqual(SLUG, topology["unique_active_wave"])
            self.assertTrue(PLAN_ACTIVE.is_file())
            self.assertFalse(PLAN_CLOSED.is_file())
        elif capability["workflow"] == "paused":
            self.assertEqual("frozen", capability["maturity"])
            self.assertNotEqual(SLUG, compiled["unique_active_slug"])
            self.assertIsNone(topology["unique_active_wave"])
            self.assertFalse(PLAN_ACTIVE.is_file())
            self.assertTrue(PLAN_CLOSED.is_file())
        else:
            self.assertEqual("accepted", capability["workflow"])
            self.assertEqual("runtime_ready", capability["maturity"])
            self.assertNotEqual(SLUG, compiled["unique_active_slug"])
            self.assertIsNone(topology["unique_active_wave"])
            self.assertFalse(PLAN_ACTIVE.is_file())
            self.assertTrue(PLAN_CLOSED.is_file())
        self.assertNotIn("player_complete", capability.get("note") or "")
