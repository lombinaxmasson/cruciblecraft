#!/usr/bin/env python3
"""Flux FE↔GU converter card: thirty GT6 flux machines."""
from __future__ import annotations

import unittest

from tools import capability_ledger as ledger
from tools import io_common as io
from tools.recipe_bulk.slugs import KNOWN_SEMANTIC_SLUGS
from tools.wave_closeout import spec_for

SLUG = "runtime/flux-converters"
CAPABILITY_SLUG = "energy/flux-converters"
ROOT = io.ROOT
CAPABILITY = (
    ROOT / "tools" / "capabilities" / "energy" / "flux-converters" / "capability.json"
)
CATALOG = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "flux_converters.json"
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
    / "flux"
)
WAVE = io.TOOLS / "waves" / "runtime" / "flux-converters"
D0 = WAVE / "d0_obtain_matrix.json"
MANIFEST = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "assets"
    / "cruciblecraft"
    / "gt6_flux_converters_art_manifest.json"
)
ACTIVE_PLAN = ROOT / "docs" / "history" / "card-plans" / "active" / "通量转换器详细计划.md"
PREP_PLAN = ROOT / "docs" / "history" / "card-plans" / "prep" / "通量转换器详细计划.md"


class EnergyFluxConvertersCardTest(unittest.TestCase):
    def test_slug_is_zero_family_runtime_card(self) -> None:
        self.assertIn(SLUG, KNOWN_SEMANTIC_SLUGS)
        spec = spec_for(SLUG)
        capability = io.load_json(CAPABILITY)
        topology = io.load_json(WAVE / "topology.json")
        readiness = io.load_json(WAVE / "readiness.json")
        if capability["workflow"] == "active":
            self.assertEqual(SLUG, spec.unique_active_wave)
            self.assertEqual(SLUG, topology["unique_active_wave"])
            self.assertEqual(SLUG, readiness["unique_active_wave"])
            compiled = ledger.compile_ledger()
            self.assertEqual(CAPABILITY_SLUG, compiled["unique_active_slug"])
            self.assertTrue(ACTIVE_PLAN.is_file())
            self.assertFalse(PREP_PLAN.is_file())
        else:
            self.assertIsNone(spec.unique_active_wave)
            self.assertIsNone(topology["unique_active_wave"])
            self.assertIsNone(readiness["unique_active_wave"])
        self.assertTrue(spec.next_unassigned)
        self.assertEqual(0, spec.owns_families)
        self.assertIsNone(spec.production_lock)
        self.assertEqual(WAVE / "census_delta.json", spec.census)
        self.assertTrue(topology["next_unassigned"])
        self.assertEqual(
            "runtime_ready", readiness["evidence"]["flux_converters_status"]
        )
        census_path = WAVE / "census_delta.json"
        if census_path.is_file():
            census = io.load_json(census_path)
            self.assertEqual(30, census["work_set"]["source_rows"])

    def test_player_complete_forbids_recipe_stand_ins(self) -> None:
        rule = (
            ROOT
            / ".cursor"
            / "rules"
            / "gt6-recipe-no-placeholder.mdc"
        ).read_text(encoding="utf-8")
        self.assertIn("no stand-in ingredients", rule)
        self.assertIn("缺形态时 DESIGN_POLICY 用已有材料做生存获得", rule)

    def test_d0_is_thirty_exact_rows(self) -> None:
        document = io.load_json(D0)
        self.assertEqual(30, len(document["rows"]))
        statuses = {row["status"] for row in document["rows"]}
        self.assertEqual({"exact"}, statuses)
        self.assertEqual(
            30, sum(1 for row in document["rows"] if row["status"] == "exact")
        )
        heater = next(row for row in document["rows"] if row["source_id"] == 11001)
        self.assertEqual("cruciblecraft:flux_heater_lead", heater["cc_id"])
        self.assertEqual(
            "cruciblecraft:steel_galvanized_electric_heater",
            heater["slots"]["M"]["cc"],
        )
        self.assertEqual("cruciblecraft:lead/long_rod", heater["slots"]["S"]["cc"])
        magnet = next(row for row in document["rows"] if row["source_id"] == 11031)
        self.assertEqual("exact", magnet["status"])
        self.assertEqual(
            "cruciblecraft:steel_galvanized_electromagnet",
            magnet["slots"]["M"]["cc"],
        )
        laser = next(row for row in document["rows"] if row["source_id"] == 11101)
        self.assertEqual("exact", laser["status"])
        self.assertEqual(
            "cruciblecraft:steel_galvanized_laser_electric",
            laser["slots"]["M"]["cc"],
        )
        dynamo = next(row for row in document["rows"] if row["source_id"] == 11111)
        self.assertEqual("cruciblecraft:bronze_dynamo", dynamo["slots"]["M"]["cc"])

    def test_catalog_is_source_exact(self) -> None:
        catalog = io.load_json(CATALOG)
        self.assertEqual(30, len(catalog["machines"]))
        ids = {row["id"] for row in catalog["machines"]}
        self.assertIn("cruciblecraft:flux_heater_lead", ids)
        self.assertIn("cruciblecraft:flux_dynamo_enderium", ids)
        lead = next(row for row in catalog["machines"] if row["source_id"] == 11001)
        self.assertEqual("lead", lead["material"])
        self.assertEqual(128, lead["nbt_input"])
        self.assertEqual(16, lead["nbt_output"])
        self.assertEqual("RF", lead["accepts"])
        self.assertEqual("HU", lead["emits"])
        self.assertTrue(lead["recipe_live"])
        dynamo = next(row for row in catalog["machines"] if row["source_id"] == 11115)
        self.assertEqual(8192, dynamo["nbt_input"])
        self.assertEqual(22528, dynamo["nbt_output"])
        self.assertEqual("RU", dynamo["accepts"])
        self.assertEqual("RF", dynamo["emits"])
        magnet = next(row for row in catalog["machines"] if row["source_id"] == 11031)
        self.assertTrue(magnet["recipe_live"])
        self.assertEqual(
            "cruciblecraft:steel_galvanized_electromagnet", magnet["host_id"]
        )
        laser = next(row for row in catalog["machines"] if row["source_id"] == 11101)
        self.assertTrue(laser["recipe_live"])
        self.assertEqual(
            "cruciblecraft:steel_galvanized_laser_electric", laser["host_id"]
        )

    def test_art_and_java_are_source_backed(self) -> None:
        manifest = io.load_json(MANIFEST)
        destinations = {row["destination"] for row in manifest["imports"]}
        self.assertTrue(
            any("flux_heater/colored/front.png" in row for row in destinations)
        )
        self.assertTrue(
            any("flux_dynamo/overlay_active/back.png" in row for row in destinations)
        )
        for destination in destinations:
            self.assertTrue(
                (ROOT / "src" / "main" / "resources" / destination).is_file(),
                destination,
            )
        entity = (JAVA / "FluxBlockEntity.java").read_text(encoding="utf-8")
        profile = (JAVA / "FluxProfile.java").read_text(encoding="utf-8")
        java = entity + "\n" + profile
        self.assertNotIn("EnergyConverterHost", entity)
        self.assertNotIn("EnergyType.RF", java)
        self.assertIn("EnergyType.HEAT", java)
        self.assertIn("EnergyType.KINETIC_PUSH", java)
        self.assertIn("EnergyType.KINETIC_ROTATION", java)
        self.assertIn("EnergyType.MU", java)
        self.assertIn("EnergyType.LU", java)
        self.assertIn("IEnergyStorage", java)
        self.assertIn("NBT_WASTE_ENERGY", java)

    def test_capability_owns_thirty_flux_converters(self) -> None:
        capability = io.load_json(CAPABILITY)
        self.assertEqual(CAPABILITY_SLUG, capability["slug"])
        self.assertEqual("runtime_ready", capability["maturity"])
        self.assertIn(capability["workflow"], {"active", "accepted", "paused"})
        self.assertEqual(["capability-runtime"], capability["profiles"])
        self.assertNotIn("player-complete", capability["profiles"])
        compiled = ledger.compile_ledger()
        self.assertNotIn(CAPABILITY_SLUG, compiled["declared_player_complete"])
        owned = capability["identity_disposition"][0]
        self.assertEqual("energy:flux-converter", owned["semantic_key"])
        self.assertEqual("new_distinct", owned["disposition"])
        self.assertEqual(30, len(owned["runtime_ids"]))
        self.assertIn("cruciblecraft:flux_heater_lead", owned["runtime_ids"])
        self.assertIn("no CC EnergyType", owned["reason"])
        ns = (
            ROOT
            / "src"
            / "main"
            / "resources"
            / "data"
            / "cruciblecraft_wave_runtime_flux_converters"
        )
        self.assertTrue((ns / "structure" / "empty.nbt").is_file())
        self.assertTrue((ns / "gametest" / "structure" / "empty.nbt").is_file())


if __name__ == "__main__":
    unittest.main()
