#!/usr/bin/env python3
"""Energy batteries closed card: 37 storage blocks, player_complete."""
from __future__ import annotations

import unittest

from tools import capability_ledger as ledger
from tools import io_common as io
from tools.recipe_bulk.slugs import KNOWN_SEMANTIC_SLUGS
from tools.wave_closeout import spec_for

SLUG = "runtime/batteries"
CAPABILITY = "energy/batteries"
ROOT = io.ROOT
WAVE = io.TOOLS / "waves" / "runtime" / "batteries"
CENSUS = ROOT / "tools" / "census_excluded_object_reclaim.json"
FEASIBILITY = (
    ROOT
    / "tools"
    / "waves"
    / "portfolio"
    / "exclusion-reclaim-r0"
    / "feasibility.json"
)
KINDS = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "energy_battery_kinds.json"
)
TIERS = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "energy_battery_tiers.json"
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
MANIFEST = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "assets"
    / "cruciblecraft"
    / "gt6_batteries_art_manifest.json"
)
ENERGY_TYPE = (
    ROOT
    / "src"
    / "main"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "api"
    / "energy"
    / "EnergyType.java"
)
COMPONENTS = (
    ROOT
    / "src"
    / "main"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "registry"
    / "ModComponents.java"
)


class EnergyBatteriesCardTest(unittest.TestCase):
    def test_slug_is_zero_family_runtime_card(self) -> None:
        self.assertIn(SLUG, KNOWN_SEMANTIC_SLUGS)
        spec = spec_for(SLUG)
        self.assertIsNone(spec.unique_active_wave)
        self.assertTrue(spec.next_unassigned)
        self.assertEqual(0, spec.owns_families)
        self.assertIsNone(spec.production_lock)
        self.assertEqual(WAVE / "census_delta.json", spec.census)
        self.assertEqual(WAVE / "gametest_receipt.json", spec.receipt)
        topology = io.load_json(WAVE / "topology.json")
        self.assertIsNone(topology["unique_active_wave"])
        self.assertTrue(topology["next_unassigned"])
        census = io.load_json(WAVE / "census_delta.json")
        self.assertEqual(37, census["work_set"]["source_rows"])

    def test_kinds_and_tiers_are_implemented(self) -> None:
        kinds = io.load_json(KINDS)
        tiers = io.load_json(TIERS)
        self.assertEqual(7, len(kinds["kinds"]))
        self.assertEqual(37, len(tiers["tiers"]))
        kind_energy = {row["id"]: row["energy"] for row in kinds["kinds"]}
        self.assertEqual("EU", kind_energy["cruciblecraft:lead_acid_battery"])
        self.assertEqual("LU", kind_energy["cruciblecraft:red_energium_crystal"])
        self.assertEqual("LU", kind_energy["cruciblecraft:cyan_energium_crystal"])
        ids = {row["id"] for row in tiers["tiers"]}
        self.assertIn("cruciblecraft:lead_acid_battery_ulv", ids)
        self.assertIn("cruciblecraft:red_energium_crystal_iv", ids)
        self.assertIn("cruciblecraft:cyan_energium_crystal_iv", ids)
        source_ids = {row["source_id"] for row in tiers["tiers"]}
        self.assertIn(14000, source_ids)
        self.assertIn(14515, source_ids)
        self.assertNotIn(14600, source_ids)
        lu_rows = [row for row in tiers["tiers"] if row["energy"] == "LU"]
        self.assertEqual(12, len(lu_rows))
        self.assertTrue(all(row["energy"] != "EU" for row in lu_rows))
        machine_blob = str(io.load_json(MACHINE_TIERS)) + str(
            io.load_json(MACHINE_KINDS)
        )
        self.assertNotIn("lead_acid_battery", machine_blob)
        self.assertNotIn("energium_crystal", machine_blob)
        catalog = io.load_json(HISTORICAL)
        historical_ids = [row["id"] for row in catalog["profiles"]]
        self.assertEqual(5, len(historical_ids))
        self.assertIn("cruciblecraft:bronze_gas_generator", historical_ids)

    def test_art_manifest_is_local_gt6_w(self) -> None:
        manifest = io.load_json(MANIFEST)
        self.assertEqual(
            "gt6_referencable_port_code/gregtech6_w",
            manifest["source"],
        )
        self.assertEqual(58, len(manifest["imports"]))
        assets = ROOT / "src" / "main" / "resources"
        for row in manifest["imports"]:
            self.assertTrue(row["gt6_source"].startswith("assets/gregtech/"))
            self.assertIn("textures/block/machine/battery/", row["destination"])
            self.assertNotIn("multiblock_casing", row["destination"])
            self.assertNotIn("conveyor_cover", row["destination"])
            self.assertTrue((assets / row["destination"]).is_file(), row["destination"])

    def test_lu_type_and_charge_component_are_distinct(self) -> None:
        energy = ENERGY_TYPE.read_text(encoding="utf-8")
        self.assertIn("LU,", energy)
        components = COMPONENTS.read_text(encoding="utf-8")
        self.assertIn("BATTERY_CHARGE", components)
        self.assertIn("ELECTRIC_CHARGE", components)
        self.assertIn('"battery_charge"', components)

    def test_capability_is_player_complete(self) -> None:
        capability = ledger.load_capability(
            ledger.CAP_ROOT / "energy" / "batteries" / "capability.json"
        )
        self.assertEqual("runtime_ready", capability["maturity"])
        self.assertEqual("accepted", capability["workflow"])
        self.assertEqual(CAPABILITY, capability["slug"])
        self.assertEqual(SLUG, capability["wave_slug"])
        self.assertEqual(
            None,
            capability["player_signoff"],
        )
        self.assertEqual(["energy/converter-catalog"], capability["depends_on"])
        keys = {row["semantic_key"] for row in capability["identity_disposition"]}
        self.assertIn("battery:lead_acid", keys)
        self.assertIn("energy_type:lu", keys)
        self.assertIn("battery_part:filled_cell", keys)
        self.assertNotIn("recipe:energium_crystal_shaped", keys)
        blocked = {
            row["semantic_key"]
            for row in capability["identity_disposition"]
            if row["disposition"] == "blocked"
        }
        self.assertEqual(set(), blocked)
        self.assertEqual(9, len(keys))
        ns = (
            ROOT
            / "src"
            / "main"
            / "resources"
            / "data"
            / "cruciblecraft_wave_runtime_batteries"
        )
        self.assertTrue((ns / "structure" / "empty.nbt").is_file())
        self.assertTrue((ns / "gametest" / "structure" / "empty.nbt").is_file())

    def test_exclusion_wave_stays_unassigned(self) -> None:
        spec = spec_for("portfolio/exclusion-reclaim-r0")
        self.assertIsNone(spec.unique_active_wave)
        self.assertTrue(spec.next_unassigned)

    def test_census_stays_thirty_seven_requires_new_runtime(self) -> None:
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


    def test_energium_gem_prefixes_alias_crystals(self) -> None:
        materials = (
            ROOT / "src" / "main" / "resources" / "data" / "cruciblecraft" / "materials"
        )
        red = io.load_json(materials / "energium_red.json")
        cyan = io.load_json(materials / "energium_cyan.json")
        self.assertEqual(
            {
                "gem": "cruciblecraft:red_energium_crystal_mv",
                "gem_chipped": "cruciblecraft:red_energium_crystal_ulv",
                "gem_exquisite": "cruciblecraft:red_energium_crystal_ev",
                "gem_flawed": "cruciblecraft:red_energium_crystal_lv",
                "gem_flawless": "cruciblecraft:red_energium_crystal_hv",
                "gem_legendary": "cruciblecraft:red_energium_crystal_iv",
            },
            red["form_items"],
        )
        self.assertEqual(
            {
                "gem": "cruciblecraft:cyan_energium_crystal_mv",
                "gem_chipped": "cruciblecraft:cyan_energium_crystal_ulv",
                "gem_exquisite": "cruciblecraft:cyan_energium_crystal_ev",
                "gem_flawed": "cruciblecraft:cyan_energium_crystal_lv",
                "gem_flawless": "cruciblecraft:cyan_energium_crystal_hv",
                "gem_legendary": "cruciblecraft:cyan_energium_crystal_iv",
            },
            cyan["form_items"],
        )
        for material in (red, cyan):
            self.assertEqual(
                {
                    "gem_chipped",
                    "gem_exquisite",
                    "gem_flawed",
                    "gem_flawless",
                    "gem_legendary",
                },
                set(material["include_prefixes"]),
            )

    def test_recipe_fidelity_debt_is_grepable(self) -> None:
        extract = (
            ROOT / "tools" / "extract_energy_battery_catalog.py"
        ).read_text(encoding="utf-8")
        self.assertIn("battery_part:filled_cell", extract)
        self.assertIn("IL.Battery_*_Cell_Filled", extract)


if __name__ == "__main__":
    unittest.main()
