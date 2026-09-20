#!/usr/bin/env python3
"""Distillation-tower GT6 alignment: maps, formed part art, 17101 fold."""
from __future__ import annotations

import unittest

from tools import capability_ledger as ledger
from tools import census_common as census
from tools import io_common as io
from tools.build_recipe_bulk import WAVE_CHOICES
from tools.gt6_distillation_tower import check
from tools.recipe_bulk.slugs import KNOWN_SEMANTIC_SLUGS
from tools.recipe_bulk.waves import SEMANTIC_COMPILE_ORDER, recipe_wave

SLUG = "machines/distillation-tower"
ROOT = io.ROOT
CAPABILITY = ROOT / "tools" / "capabilities" / "machines" / "distillation-tower" / "capability.json"
PLAN_ACTIVE = ROOT / "docs" / "history" / "card-plans" / "active" / "蒸馏塔GT6对齐详细计划.md"
PLAN_CLOSED = ROOT / "docs" / "history" / "card-plans" / "closed" / "蒸馏塔GT6对齐详细计划.md"
ASSETS = ROOT / "src" / "main" / "resources" / "assets" / "cruciblecraft"
GAME_TESTS = (
    ROOT
    / "src"
    / "test"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "gametest"
    / "CrucibleCraftGameTests.java"
)
PORT_BLOCK = (
    ROOT
    / "src"
    / "main"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "content"
    / "block"
    / "MultiblockPortBlock.java"
)


class DistillationTowerCardTest(unittest.TestCase):
    def test_slug_is_known_and_does_not_compile_a_dump(self) -> None:
        self.assertIn(SLUG, KNOWN_SEMANTIC_SLUGS)
        self.assertNotIn(SLUG, SEMANTIC_COMPILE_ORDER)
        self.assertNotIn(SLUG, WAVE_CHOICES)
        with self.assertRaises(KeyError):
            recipe_wave(SLUG)

    def test_generator_check_passes(self) -> None:
        self.assertEqual([], check())

    def test_capability_and_game_tests_are_scoped(self) -> None:
        capability = census.load_json(CAPABILITY)
        self.assertEqual(SLUG, capability["slug"])
        self.assertEqual(["capability-runtime"], capability["profiles"])
        self.assertNotIn("player-complete", capability.get("profiles") or [])
        compiled = ledger.compile_ledger()
        if capability["workflow"] == "active":
            self.assertEqual("frozen", capability["maturity"])
            self.assertEqual(SLUG, compiled["unique_active_slug"])
            self.assertTrue(PLAN_ACTIVE.is_file())
            self.assertFalse(PLAN_CLOSED.is_file())
        else:
            self.assertEqual("accepted", capability["workflow"])
            self.assertEqual("runtime_ready", capability["maturity"])
            self.assertNotEqual(SLUG, compiled["unique_active_slug"])
            self.assertFalse(PLAN_ACTIVE.is_file())
            self.assertTrue(PLAN_CLOSED.is_file())
        source = GAME_TESTS.read_text(encoding="utf-8")
        for test_id in capability["required_test_ids"]:
            self.assertIn(f"void {test_id}", source)
        self.assertIn("TOWER_SKIN", PORT_BLOCK.read_text(encoding="utf-8"))
        self.assertIn("BACK_HOLE", PORT_BLOCK.read_text(encoding="utf-8"))
        tests = GAME_TESTS.read_text(encoding="utf-8")
        self.assertIn("structurePaletteBlock", tests)
        self.assertIn("DistillationTowerParts.TOWER_PART", tests)
        self.assertIn("DistillationTowerParts.HEAT_TRANSMITTER", tests)
        self.assertIn("Catalog dummy 17101 is still registered", tests)
        parts = (
            ROOT
            / "src"
            / "main"
            / "java"
            / "com"
            / "masson"
            / "cruciblecraft"
            / "content"
            / "block"
            / "DistillationTowerParts.java"
        )
        self.assertTrue(parts.is_file())
        self.assertIn("18102", parts.read_text(encoding="utf-8"))

    def test_dummy_17101_is_folded_onto_live(self) -> None:
        from tools.gt6_distillation_tower import (
            ACQUISITION_CATALOG,
            CRAFT_HOT,
            DUMMY_ASSETS,
            DUMMY_CONTROLLER_ID,
            DUMMY_CRAFT,
            INPLACE_CATALOG,
            LIVE_CONTROLLER_ID,
        )

        self.assertFalse(CRAFT_HOT.is_file())
        craft = census.load_json(DUMMY_CRAFT)
        self.assertEqual(LIVE_CONTROLLER_ID, craft["result"]["id"])
        metas = {
            int(row["meta"])
            for row in census.load_json(INPLACE_CATALOG).get("identities") or []
        }
        self.assertNotIn(17101, metas)
        self.assertIn(18102, metas)
        for path in DUMMY_ASSETS:
            self.assertFalse(path.is_file(), path)
        acquisition = census.load_json(ACQUISITION_CATALOG)
        dummy = next(
            row
            for row in acquisition["recipes"]
            if row.get("path") == "multiblock/distillation_tower"
        )
        self.assertEqual(LIVE_CONTROLLER_ID, dummy["result"]["id"])
        part = (
            ROOT
            / "src"
            / "generated"
            / "resources"
            / "data"
            / "cruciblecraft"
            / "recipe"
            / "multiblock"
            / "distillation_tower_part.json"
        )
        self.assertIn(
            "cruciblecraft:multiblock/distillation_tower_part",
            part.read_text(encoding="utf-8"),
        )
        self.assertNotIn(f'"{DUMMY_CONTROLLER_ID}"', part.read_text(encoding="utf-8"))

    def test_part_art_is_copied_and_not_aliased(self) -> None:
        manifest = census.load_json(
            ASSETS / "gt6_distillation_tower_part_art_manifest.json"
        )
        self.assertGreaterEqual(len(manifest["imports"]), 18)
        gt6_root = ROOT / "gt6_referencable_port_code" / "gregtech6_w"
        self.assertTrue(gt6_root.is_dir())
        for row in manifest["imports"]:
            destination = ROOT / "src" / "main" / "resources" / row["destination"]
            self.assertTrue(destination.is_file(), destination)
            self.assertNotIn("distillery", row["destination"])
            self.assertNotIn("multiblock_casing", row["destination"])


if __name__ == "__main__":
    unittest.main()
