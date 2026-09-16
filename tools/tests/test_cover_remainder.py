#!/usr/bin/env python3
"""GT6 machine-cover remainder card: exact denominator, no stand-ins."""
from __future__ import annotations

import json
import unittest
from pathlib import Path

from tools import capability_ledger as ledger
from tools import io_common as io
from tools.recipe_bulk.slugs import KNOWN_SEMANTIC_SLUGS
from tools.wave_closeout import spec_for

SLUG = "runtime/cover-remainder"
CAPABILITY = "logistics/cover-remainder"
ROOT = io.ROOT
WAVE = io.TOOLS / "waves" / "runtime" / "cover-remainder"
CAP = io.TOOLS / "capabilities" / "logistics" / "cover-remainder"
SIDECAR = (
    ROOT
    / "src/main/resources/data/cruciblecraft/machine_cover_definitions.json"
)
MANIFEST = (
    ROOT
    / "src/main/resources/assets/cruciblecraft/"
    / "gt6_machine_covers_art_manifest.json"
)
PLAN_ACTIVE = ROOT / "docs/history/card-plans/active/盖板余量详细计划.md"
PLAN_CLOSED = ROOT / "docs/history/card-plans/closed/盖板余量详细计划.md"
REGISTRY = ROOT / "src/main/java/com/masson/cruciblecraft/logistics/pipe/cover/CoverBehaviorRegistry.java"
GENERATED = ROOT / "src/generated/resources"

ITEM_PATHS = [
    "cover_blank",
    "controller_display",
    "controller_auto",
    "display_energy",
    "controller_redstone",
    "controller_auto_redstone",
    "selector_redstone",
    "controller_auto_timer_1m",
    "controller_auto_timer_5m",
    "controller_auto_timer_10m",
    "controller_auto_timer_20m",
    "controller_auto_timer_30m",
    "scale_energy",
    "detector_running_possible",
    "detector_running_passively",
    "detector_running_actively",
    "scale_progress",
    "detector_running_successfully",
    "redstone_emitter",
    "vent",
    "controller_covers",
    "selector_button_panel",
    "redstone_conductor_in",
    "redstone_conductor_out",
]

DEFINITION_PATHS = ITEM_PATHS + [
    "selector_tag",
    "redstone_torch",
    "redstone_repeater",
]


class CoverRemainderCardTest(unittest.TestCase):
    def test_slug_is_active_and_has_a_closeout_spec(self) -> None:
        self.assertIn(SLUG, KNOWN_SEMANTIC_SLUGS)
        spec = spec_for(SLUG)
        self.assertEqual(WAVE / "census_delta.json", spec.census)
        self.assertEqual(WAVE / "readiness.json", spec.readiness)
        self.assertTrue(spec.next_unassigned)
        self.assertEqual(
            Path("src/main/java/com/masson/cruciblecraft/gametest/")
            / "CoverRemainderGameTests.java",
            spec.gametest_java.relative_to(ROOT),
        )

    def test_capability_is_runtime_ready_but_not_player_complete(self) -> None:
        capability = io.load_json(CAP / "capability.json")
        self.assertEqual(CAPABILITY, capability["slug"])
        self.assertEqual("runtime_ready", capability["maturity"])
        self.assertIn(capability["workflow"], ("active", "accepted"))
        self.assertEqual(
            [
                "autoSwitchRequiresPossibleAndCanTick",
                "blankCoverIsSurvivalCraftable",
                "blankCoverRequiresCraftingTable",
                "controllerStopsProcessingWhenOff",
                "detectorEmitsWhenPossible",
                "displayVisualUsesGt6Layout",
                "playerSurfaceIsRegistered",
                "selectorUsesFakeCircuit",
                "torchRefusesMachineHost",
                "ventFillsCollectableAir",
            ],
            capability["required_test_ids"],
        )
        compiled = ledger.compile_ledger()
        if capability["workflow"] == "active":
            self.assertEqual(CAPABILITY, compiled["unique_active_slug"])
            self.assertTrue(PLAN_ACTIVE.is_file())
            self.assertFalse(PLAN_CLOSED.is_file())
        else:
            self.assertEqual("accepted", capability["workflow"])
            self.assertNotEqual(CAPABILITY, compiled["unique_active_slug"])
            self.assertFalse(PLAN_ACTIVE.is_file())
            self.assertTrue(PLAN_CLOSED.is_file())
        self.assertNotIn(CAPABILITY, compiled["declared_player_complete"])
        receipt = io.load_json(WAVE / "gametest_receipt.json")
        self.assertEqual("PASS", receipt["status"])
        self.assertEqual(0, int(receipt["failed"]))
        self.assertGreaterEqual(int(receipt["passed"]), int(receipt["required_tests"]))
        self.assertIn("-PwaveRecipes=runtime/cover-remainder", receipt["command"])
        self.assertEqual(
            capability["required_test_ids"],
            receipt["test_ids"],
        )

    def test_sidecar_has_the_exact_27_definitions(self) -> None:
        document = io.load_json(SIDECAR)
        rows = document["definitions"]
        self.assertEqual(27, len(rows))
        ids = [row["id"].split(":", 1)[-1] for row in rows]
        self.assertEqual(DEFINITION_PATHS, ids)
        self.assertEqual(
            {"selector_tag", "redstone_torch", "redstone_repeater"},
            set(ids[-3:]),
        )
        for row in rows:
            self.assertNotIn("gt6u", json.dumps(row).lower())
            self.assertNotIn("programmed_circuit", json.dumps(row).lower())

    def test_blank_recipe_is_not_faked_with_programmed_circuit(self) -> None:
        recipe_java = ROOT / (
            "src/main/java/com/masson/cruciblecraft/datagen/"
            "ModRecipeProvider.java"
        )
        text = recipe_java.read_text(encoding="utf-8")
        start = text.index("private static void machineCoverRecipes")
        end = text.index("private static Ingredient blank", start)
        block = text[start:end]
        self.assertIn('"cover_blank"', block)
        self.assertIn("MaterialPrefixes.PLATE", block)
        self.assertIn("MaterialPrefixes.SCREW", block)
        self.assertNotIn(
            '"cover_blank".*programmed_circuit',
            block,
        )
        generated = GENERATED / "data/cruciblecraft/recipe/cover_blank_cover.json"
        if generated.is_file():
            self.assertNotIn(
                "programmed_circuit",
                generated.read_text(encoding="utf-8"),
            )

    def test_art_manifest_is_the_restored_local_gt6_w(self) -> None:
        manifest = io.load_json(MANIFEST)
        self.assertEqual(
            "936083c247a70b1bbc5f19996a83d75c27d196e2",
            manifest["source_revision"],
        )
        self.assertEqual(24, len(manifest["entries"]))
        self.assertGreater(len(manifest["block_entries"]), 0)
        for row in list(manifest["entries"]) + list(manifest["block_entries"]):
            self.assertTrue(
                row["source"].startswith(
                    "gt6_referencable_port_code/gregtech6_w/"
                )
            )
            source = ROOT / row["source"]
            destination = ROOT / "src/main/resources" / row["destination"]
            self.assertTrue(source.is_file(), row["source"])
            self.assertTrue(destination.is_file(), row["destination"])
            self.assertEqual(source.read_bytes(), destination.read_bytes())

    def test_machine_behavior_registry_is_not_skipped(self) -> None:
        text = REGISTRY.read_text(encoding="utf-8")
        self.assertIn("MachineCoverCovers.bootstrap()", text)
        self.assertNotIn("if (MachineCoverKinds.BEHAVIOR_PATHS.contains", text)


if __name__ == "__main__":
    unittest.main()
