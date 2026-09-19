#!/usr/bin/env python3
"""Energy converter catalog closed card: 179 loader rows plus 7 DESIGN_POLICY turbines."""
from __future__ import annotations

import json
import unittest

import re

from tools import capability_ledger as ledger
from tools import io_common as io
from tools.recipe_bulk.slugs import KNOWN_SEMANTIC_SLUGS
from tools.wave_closeout import spec_for

SLUG = "runtime/converter-catalog"
CAPABILITY = "energy/converter-catalog"
ROOT = io.ROOT
WAVE = io.TOOLS / "waves" / "runtime" / "converter-catalog"
KINDS = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "energy_converter_kinds.json"
)
TIERS = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "energy_converter_tiers.json"
)
MACHINE_TIERS = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "machine_tiers.json"
)
MACHINE_KINDS = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "machine_kinds.json"
)
HISTORICAL = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "energy_converters.json"
)
MOD_BLOCKS = (
    ROOT
    / "src"
    / "main"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "registry"
    / "ModBlocks.java"
)
CENSUS = ROOT / "tools" / "census_excluded_object_reclaim.json"
FEASIBILITY = (
    ROOT
    / "tools"
    / "waves"
    / "portfolio"
    / "exclusion-reclaim-r0"
    / "feasibility.json"
)
MANIFEST = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "assets"
    / "cruciblecraft"
    / "gt6_converter_catalog_art_manifest.json"
)
DUST_ART = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "assets"
    / "cruciblecraft"
    / "gt6_dust_forms_art_manifest.json"
)
FLUIDBED_OUT = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "recipe"
    / "energy"
    / "fuels_fluidbed"
)
REQUIRED_FORMS = WAVE / "required_forms.json"


class EnergyConverterCatalogCardTest(unittest.TestCase):
    def test_slug_is_zero_family_runtime_card(self) -> None:
        self.assertIn(SLUG, KNOWN_SEMANTIC_SLUGS)
        spec = spec_for(SLUG)
        self.assertIsNone(spec.unique_active_wave)
        self.assertTrue(spec.next_unassigned)
        self.assertEqual(0, spec.owns_families)
        self.assertIsNone(spec.production_lock)
        self.assertEqual(WAVE / "census_delta.json", spec.census)
        self.assertEqual(WAVE / "gametest_receipt.json", spec.receipt)

    def test_kinds_and_tiers_are_not_processing_catalog(self) -> None:
        kinds = io.load_json(KINDS)
        tiers = io.load_json(TIERS)
        self.assertEqual(19, len(kinds["kinds"]))
        self.assertEqual(186, len(tiers["tiers"]))
        ids = {row["id"] for row in tiers["tiers"]}
        self.assertIn("cruciblecraft:bronze_burning_box_gas", ids)
        self.assertIn("cruciblecraft:bronze_boiler", ids)
        self.assertIn("cruciblecraft:bronze_fuel_engine", ids)
        self.assertIn("cruciblecraft:bronze_small_gas_turbine", ids)
        self.assertIn("cruciblecraft:steel_galvanized_electric_motor", ids)
        self.assertIn("cruciblecraft:steel_galvanized_electric_heater", ids)
        self.assertIn("cruciblecraft:steel_galvanized_electric_engine", ids)
        self.assertNotIn("cruciblecraft:burning_gas_generator", ids)
        machine_tiers = json.dumps(io.load_json(MACHINE_TIERS), ensure_ascii=False)
        machine_kinds = json.dumps(io.load_json(MACHINE_KINDS), ensure_ascii=False)
        self.assertNotIn("burning_box_gas", machine_tiers)
        self.assertNotIn("burning_box_solid", machine_kinds)
        self.assertNotIn("energy_converter", machine_tiers)

    def test_historical_converters_json_keeps_bronze_gas_generator(self) -> None:
        catalog = io.load_json(HISTORICAL)
        ids = [row["id"] for row in catalog["profiles"]]
        self.assertIn("cruciblecraft:bronze_gas_generator", ids)
        self.assertEqual(5, len(ids))

    def test_live_placeholders_are_gone(self) -> None:
        blocks = MOD_BLOCKS.read_text(encoding="utf-8")
        self.assertNotIn('register("fuel_engine"', blocks)
        self.assertNotIn('register("electric_motor"', blocks)
        self.assertNotIn('register("burning_gas_generator"', blocks)
        self.assertIsNone(re.search(r"\bBURNING_GAS_GENERATOR\b", blocks))
        self.assertIsNone(re.search(r"\bFUEL_ENGINE\b", blocks))
        self.assertIsNone(re.search(r"\bELECTRIC_MOTOR\b", blocks))

    def test_art_manifest_is_local_gt6_w(self) -> None:
        manifest = io.load_json(MANIFEST)
        self.assertEqual(
            "gt6_referencable_port_code/gregtech6_w",
            manifest["source"],
        )
        self.assertGreaterEqual(len(manifest["imports"]), 19)
        for row in manifest["imports"]:
            self.assertTrue(row["gt6_source"].startswith("assets/gregtech/"))
            self.assertIn("textures/block/machine/", row["destination"])

    def test_fluidbed_dust_forms_are_source_backed(self) -> None:
        recipes = list(FLUIDBED_OUT.glob("*.json"))
        self.assertEqual(55, len(recipes))
        empty_outputs = 0
        for path in recipes:
            recipe = io.load_json(path)
            self.assertEqual("cruciblecraft:fuels_fluidbed", recipe["map"])
            self.assertLess(recipe["eut"], 0)
            if not recipe["item_outputs"]:
                empty_outputs += 1
        self.assertEqual(17, empty_outputs)
        required = io.load_json(REQUIRED_FORMS)
        self.assertEqual(13, required["counts"]["required_materials"])
        self.assertEqual(62, required["counts"]["required_form_pairs"])
        self.assertIn("storage_dust", required["required_forms"]["peat"])
        self.assertIn("dust_div72", required["required_forms"]["petroleum_coke"])
        dust_art = io.load_json(DUST_ART)
        self.assertEqual(
            "gt6_referencable_port_code/gregtech6_w",
            dust_art["source"],
        )
        destinations = {row["destination"] for row in dust_art["imports"]}
        self.assertIn(
            "assets/cruciblecraft/textures/item/material/dust_div72.png",
            destinations,
        )
        self.assertIn(
            "assets/cruciblecraft/textures/item/material/storage_dust.png",
            destinations,
        )

    def test_capability_is_player_complete(self) -> None:
        capability = ledger.load_capability(
            ledger.CAP_ROOT / "energy" / "converter-catalog" / "capability.json"
        )
        self.assertEqual("runtime_ready", capability["maturity"])
        self.assertEqual("accepted", capability["workflow"])
        self.assertEqual(CAPABILITY, capability["slug"])
        self.assertEqual(
            None,
            capability["player_signoff"],
        )
        ns = (
            ROOT
            / "src"
            / "main"
            / "resources"
            / "data"
            / "cruciblecraft_wave_runtime_converter_catalog"
        )
        self.assertTrue((ns / "structure" / "empty.nbt").is_file())
        self.assertTrue((ns / "gametest" / "structure" / "empty.nbt").is_file())
        notes = io.load_json(
            ledger.CAP_ROOT
            / "energy"
            / "converter-catalog"
            / "player_signoff.json"
        )["notes"]
        self.assertIn("molten_calcite", notes)

    def test_batteries_stay_unimplemented(self) -> None:
        census = io.load_json(CENSUS)
        batteries = next(
            row
            for row in census["category_summaries"]
            if row["category"] == "Batteries"
        )
        self.assertEqual(37, batteries["source_sites"])
        self.assertEqual(37, batteries["expanded_multiplicity"])
        feasibility = io.load_json(FEASIBILITY)
        row = next(
            item
            for item in feasibility["categories"]
            if item["category"] == "Batteries"
        )
        self.assertEqual("requires_new_runtime", row["verdict"])


if __name__ == "__main__":
    unittest.main()
