#!/usr/bin/env python3
"""Large processing-multiblock parts/maps unique-active card."""
from __future__ import annotations

import unittest

from tools import capability_ledger as ledger
from tools import census_common as census
from tools import gt6_large_processing_parts as parts
from tools import io_common as io
from tools.build_recipe_bulk import WAVE_CHOICES
from tools.recipe_bulk.slugs import KNOWN_SEMANTIC_SLUGS
from tools.recipe_bulk.waves import SEMANTIC_COMPILE_ORDER, recipe_wave

SLUG = "machines/large-processing-parts"
ROOT = io.ROOT
CAPABILITY = (
    ROOT / "tools" / "capabilities" / "machines" / "large-processing-parts" / "capability.json"
)
PLAN_ACTIVE = ROOT / "docs" / "history" / "card-plans" / "active" / "大型加工多方块零件与配方图详细计划.md"
PLAN_CLOSED = ROOT / "docs" / "history" / "card-plans" / "closed" / "大型加工多方块零件与配方图详细计划.md"
GAME_TESTS = (
    ROOT
    / "src"
    / "test"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "gametest"
    / "LargeProcessingPartsGameTests.java"
)
TOPOLOGY = ROOT / "tools" / "waves" / "machines" / "large-processing-parts" / "topology.json"


class LargeProcessingPartsCardTest(unittest.TestCase):
    def test_slug_is_known_and_does_not_compile_a_dump(self) -> None:
        self.assertIn(SLUG, KNOWN_SEMANTIC_SLUGS)
        self.assertNotIn(SLUG, SEMANTIC_COMPILE_ORDER)
        self.assertNotIn(SLUG, WAVE_CHOICES)
        with self.assertRaises(KeyError):
            recipe_wave(SLUG)

    def test_census_is_fresh(self) -> None:
        self.assertEqual([], parts.check())

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
        else:
            self.assertEqual("accepted", capability["workflow"])
            self.assertEqual("runtime_ready", capability["maturity"])
            self.assertNotEqual(SLUG, compiled["unique_active_slug"])
            self.assertIsNone(topology["unique_active_wave"])
            self.assertFalse(PLAN_ACTIVE.is_file())
            self.assertTrue(PLAN_CLOSED.is_file())
            self.assertEqual([], parts.close_errors())

    def test_later_host_would_not_block(self) -> None:
        self.assertEqual([], parts.close_errors())

    def test_wave_pack_has_empty_structure(self) -> None:
        pack = (
            ROOT
            / "src"
            / "main"
            / "resources"
            / "data"
            / "cruciblecraft_wave_machines_large_processing_parts"
        )
        self.assertTrue((pack / "structure" / "empty.nbt").is_file())
        self.assertTrue((pack / "gametest" / "structure" / "empty.nbt").is_file())
        manifest = (
            ROOT
            / "src"
            / "main"
            / "resources"
            / "assets"
            / "cruciblecraft"
            / "gt6_large_processing_parts_art_manifest.json"
        )
        document = census.load_json(manifest)
        self.assertTrue(document["source_present"])
        for row in document["imports"]:
            dest = ROOT / "src" / "main" / "resources" / str(row["destination"])
            self.assertTrue(dest.is_file(), dest.as_posix())
