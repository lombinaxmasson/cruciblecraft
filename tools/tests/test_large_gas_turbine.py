#!/usr/bin/env python3
"""Large gas turbine unique-active card: GT6 17231-17234, not kTFRUAddon 10000-10006."""
from __future__ import annotations

import unittest

from tools import capability_ledger as ledger
from tools import io_common as io

SLUG = "energy/large-gas-turbine"
ROOT = io.ROOT
CAPABILITY = (
    ROOT / "tools" / "capabilities" / "energy" / "large-gas-turbine" / "capability.json"
)
CATALOG = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "large_gas_turbines.json"
)
JAVA = (
    ROOT
    / "src"
    / "main"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "energy"
    / "largegasturbine"
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
    / "LargeGasTurbineGameTests.java"
)
MANIFEST = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "assets"
    / "cruciblecraft"
    / "gt6_large_gas_turbine_art_manifest.json"
)
ASSETS = ROOT / "src" / "main" / "resources" / "assets" / "cruciblecraft"
KIND = (
    ROOT
    / "src"
    / "main"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "content"
    / "mte"
    / "MteInPlaceKind.java"
)
LANG = (
    ROOT
    / "src"
    / "main"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "datagen"
    / "ModLanguageProvider.java"
)
EMI = (
    ROOT
    / "src"
    / "main"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "compat"
    / "emi"
    / "EmiStackGroupPlan.java"
)
PLAN_ACTIVE = ROOT / "docs" / "history" / "card-plans" / "active" / "大型燃气轮机详细计划.md"
PLAN_CLOSED = ROOT / "docs" / "history" / "card-plans" / "closed" / "大型燃气轮机详细计划.md"
WAVE = io.TOOLS / "waves" / "energy" / "large-gas-turbine"


class LargeGasTurbineCardTest(unittest.TestCase):
    def test_unique_active_and_scope(self) -> None:
        capability = io.load_json(CAPABILITY)
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
        topology = io.load_json(WAVE / "topology.json")
        readiness = io.load_json(WAVE / "readiness.json")
        self.assertEqual(SLUG, topology["wave_slug"])
        self.assertEqual(4, topology["housing_ids"])
        self.assertEqual(4, readiness["housing_count"])

    def test_catalog_is_four_gt6_housings(self) -> None:
        catalog = io.load_json(CATALOG)
        self.assertEqual(1, catalog["schema_version"])
        self.assertEqual(4, catalog["expected_count"])
        self.assertEqual(4, len(catalog["machines"]))
        ids = [row["source_id"] for row in catalog["machines"]]
        self.assertEqual([17231, 17232, 17233, 17234], ids)
        self.assertEqual(
            {
                "cruciblecraft:magnalium/gas_turbine_main_housing",
                "cruciblecraft:trinitanium/gas_turbine_main_housing",
                "cruciblecraft:graphene/gas_turbine_main_housing",
                "cruciblecraft:vibramantium/gas_turbine_main_housing",
            },
            {row["id"] for row in catalog["machines"]},
        )
        for row in catalog["machines"]:
            self.assertEqual("MultiTileEntityLargeTurbineGas", row["gt6_class"])
            self.assertEqual(["PwP", "BMC", "PEP"], row["recipe"]["pattern"])
            self.assertEqual("w", row["recipe"]["catalyst"])
            self.assertNotIn("programmed_circuit", str(row["recipe"]))

    def test_runtime_is_not_small_gas_or_steam(self) -> None:
        java = "\n".join(path.read_text(encoding="utf-8") for path in JAVA.glob("*.java"))
        self.assertIn("17231", java)
        self.assertIn("FUELS_GAS_TURBINE", java)
        self.assertIn("SteamTurbineStructure", java)
        self.assertIn("LargeTurbineHatches", java)
        self.assertIn("bindHatches", java)
        self.assertIn("aabbLoaded", java)
        self.assertIn("ONLY_ITEM_FLUID_IN", java)
        self.assertNotIn("MultiTileEntityGasMotor", java)
        self.assertNotIn("bronze_small_gas_turbine", java)
        kind = KIND.read_text(encoding="utf-8")
        self.assertIn("GAS_TURBINE", kind)
        self.assertIn("STEAM_TURBINE", kind)

    def test_holder_stays_on_the_multiblock_grid(self) -> None:
        source = GAME_TESTS.read_text(encoding="utf-8")
        self.assertIn("cruciblecraft_multiblock", source)
        self.assertNotIn("bronze_small_gas_turbine", source)

    def test_art_is_twelve_gasturbine_sheets(self) -> None:
        manifest = io.load_json(MANIFEST)
        self.assertTrue(manifest["source_present"])
        housing = [
            row
            for row in manifest["imports"]
            if "multiblockmains/gasturbine" in row["gt6_source"]
        ]
        self.assertEqual(12, len(housing))
        sources = {row["gt6_source"] for row in manifest["imports"]}
        self.assertIn(
            "src/main/resources/assets/gregtech/textures/blocks/machines/multiblockmains/turbine.png",
            sources,
        )
        self.assertIn(
            "src/main/resources/assets/gregtech/textures/blocks/machines/multiblockmains/turbine_active.png",
            sources,
        )
        self.assertTrue(
            any("metalwalldense/3/" in row["gt6_source"] for row in manifest["imports"])
        )
        assets_root = ASSETS.parent.parent
        for row in manifest["imports"]:
            dest = assets_root / row["destination"]
            self.assertTrue(dest.is_file(), dest)
            self.assertNotIn("multiblock_casing", row["destination"])
            self.assertNotIn("steam_turbine", row["gt6_source"])

    def test_emi_group_is_not_small_gas_turbine(self) -> None:
        emi = EMI.read_text(encoding="utf-8")
        lang = LANG.read_text(encoding="utf-8")
        self.assertIn("energy/large_gas_turbine", emi)
        self.assertIn("energy/large_gas_turbine", lang)
        self.assertIn("GAS_TURBINE", emi)
