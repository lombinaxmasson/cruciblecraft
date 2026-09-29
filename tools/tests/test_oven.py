#!/usr/bin/env python3
"""Live Oven card: Heat_T hosts, vanilla SMELTING snapshot, no dump compile."""
from __future__ import annotations

import unittest

from tools import capability_ledger as ledger
from tools import census_common as census
from tools import io_common as io
from tools.build_recipe_bulk import WAVE_CHOICES
from tools.recipe_bulk.slugs import KNOWN_SEMANTIC_SLUGS
from tools.recipe_bulk.waves import SEMANTIC_COMPILE_ORDER, recipe_wave

SLUG = "machines/oven"
ROOT = io.ROOT
WAVE = ROOT / "tools" / "waves" / "machines" / "oven"
PREP = ROOT / "tools" / "waves" / "prep" / "oven"
CAPABILITY = ROOT / "tools" / "capabilities" / "machines" / "oven" / "capability.json"
PLAN_ACTIVE = ROOT / "docs" / "history" / "card-plans" / "active" / "熔炉详细计划.md"
PLAN_CLOSED = ROOT / "docs" / "history" / "card-plans" / "closed" / "熔炉详细计划.md"
PLAN_PREP = ROOT / "docs" / "history" / "card-plans" / "prep" / "熔炉详细计划.md"
ASSETS = ROOT / "src" / "main" / "resources" / "assets" / "cruciblecraft"


class OvenCardTest(unittest.TestCase):
    def test_slug_is_known_and_does_not_compile_a_dump(self) -> None:
        self.assertIn(SLUG, KNOWN_SEMANTIC_SLUGS)
        self.assertNotIn(SLUG, SEMANTIC_COMPILE_ORDER)
        self.assertNotIn(SLUG, WAVE_CHOICES)
        with self.assertRaises(KeyError):
            recipe_wave(SLUG)
        with self.assertRaisesRegex((ValueError, KeyError), "src/recipe_generated|unknown recipe wave"):
            recipe_wave("prep/oven")

    def test_d0_four_hosts_are_source_exact(self) -> None:
        document = census.load_json(WAVE / "d0_obtain_matrix.json")
        self.assertEqual(["wMh", "BCB"], document["grid"])
        statuses = {row["host"]: row["status"] for row in document["hosts"]}
        self.assertEqual(
            {
                20001: "source_exact",
                20002: "source_exact",
                20003: "source_exact",
                20004: "source_exact",
            },
            statuses,
        )
        steel = next(row for row in document["hosts"] if row["host"] == 20001)
        self.assertEqual("cruciblecraft:steel/machine_casing", steel["casing"]["cc"])
        self.assertEqual("minecraft:bricks", steel["bricks"]["cc"])
        self.assertNotIn("programmed_circuit", str(document))
        self.assertNotIn("machine_casing_double", str(document))

    def test_capability_is_runtime_ready(self) -> None:
        capability = census.load_json(CAPABILITY)
        self.assertEqual(SLUG, capability["slug"])
        self.assertEqual("runtime_ready", capability["maturity"])
        self.assertEqual(["capability-runtime"], capability["profiles"])
        self.assertEqual(
            [
                "fourHostsAreSurvivalCraftable",
                "liveMapMirrorsVanillaSmelting",
                "playerSurfaceIsRegistered",
                "steelHostSmeltsCobble",
            ],
            capability["required_test_ids"],
        )
        compiled = ledger.compile_ledger()
        if capability["workflow"] == "active":
            self.assertEqual(SLUG, compiled["unique_active_slug"])
            self.assertTrue(PLAN_ACTIVE.is_file())
            self.assertFalse(PLAN_CLOSED.is_file())
            self.assertFalse(PLAN_PREP.is_file())
        else:
            self.assertEqual("accepted", capability["workflow"])
            self.assertNotEqual(SLUG, compiled["unique_active_slug"])
            self.assertFalse(PLAN_ACTIVE.is_file())
            self.assertTrue(PLAN_CLOSED.is_file())
        blocked = {
            row["semantic_key"]
            for row in capability["identity_disposition"]
            if row["disposition"] == "blocked"
        }
        self.assertEqual({"recipe:oven:cooking_oil_xp"}, blocked)

    def test_art_is_copied_and_not_aliased(self) -> None:
        manifest = census.load_json(ASSETS / "gt6_oven_art_manifest.json")
        self.assertGreaterEqual(len(manifest["imports"]), 20)
        for row in manifest["imports"]:
            destination = ROOT / "src" / "main" / "resources" / row["destination"]
            self.assertTrue(destination.is_file(), row["destination"])
            self.assertNotIn("smelter", row["destination"])
            self.assertNotIn("melter", row["destination"])
            self.assertNotIn("multiblock_casing", row["destination"])

    def test_unique_active_docs_and_ledger_hand_off(self) -> None:
        topology = census.load_json(WAVE / "topology.json")
        readiness = census.load_json(WAVE / "readiness.json")
        lock = census.load_json(WAVE / "production_lock.json")
        notes = census.load_json(WAVE / "runtime_notes.json")
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
        self.assertEqual("DOWN", notes["energy_accepted_sides"])
        self.assertIn("not player_complete", lock["note"])
        self.assertEqual(0, lock["production"]["relation_count"])
        self.assertTrue((PREP / "d0_obtain_matrix.json").is_file())


if __name__ == "__main__":
    unittest.main()
