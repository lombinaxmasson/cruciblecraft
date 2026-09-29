#!/usr/bin/env python3
"""Electric/flux cooler unique-active card: ten GT6 coolers."""
from __future__ import annotations

import unittest

from tools import capability_ledger as ledger
from tools import io_common as io
from tools.recipe_bulk.slugs import KNOWN_SEMANTIC_SLUGS
from tools.wave_closeout import spec_for

SLUG = "runtime/cooler"
CAPABILITY_SLUG = "energy/cooler"
ROOT = io.ROOT
CAPABILITY = (
    ROOT / "tools" / "capabilities" / "energy" / "cooler" / "capability.json"
)
CATALOG = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "coolers.json"
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
    / "cooler"
)
WAVE = io.TOOLS / "waves" / "runtime" / "cooler"
D0 = WAVE / "d0_obtain_matrix.json"
MANIFEST = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "assets"
    / "cruciblecraft"
    / "gt6_coolers_art_manifest.json"
)
ACTIVE_PLAN = ROOT / "docs" / "history" / "card-plans" / "active" / "冷却器详细计划.md"
PREP_PLAN = ROOT / "docs" / "history" / "card-plans" / "prep" / "冷却器详细计划.md"


class EnergyCoolersCardTest(unittest.TestCase):
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
        self.assertEqual("runtime_ready", readiness["evidence"]["coolers_status"])
        census_path = WAVE / "census_delta.json"
        if census_path.is_file():
            census = io.load_json(census_path)
            self.assertEqual(10, census["work_set"]["source_rows"])

    def test_player_complete_forbids_recipe_stand_ins(self) -> None:
        rule = (
            ROOT
            / ".cursor"
            / "rules"
            / "gt6-recipe-no-placeholder.mdc"
        ).read_text(encoding="utf-8")
        self.assertIn("no stand-in ingredients", rule)
        self.assertIn("缺形态时 DESIGN_POLICY 用已有材料做生存获得", rule)

    def test_d0_is_ten_exact_rows(self) -> None:
        document = io.load_json(D0)
        self.assertEqual(10, len(document["rows"]))
        self.assertEqual(
            {"exact"},
            {row["status"] for row in document["rows"]},
        )
        self.assertEqual(
            {10161, 10162, 10163, 10164, 10165,
             11161, 11162, 11163, 11164, 11165},
            {row["source_id"] for row in document["rows"]},
        )
        lv = next(row for row in document["rows"] if row["source_id"] == 10161)
        self.assertEqual(
            "cruciblecraft:thermoelectric_cooler_lv", lv["cc_id"]
        )
        self.assertEqual(
            "cruciblecraft:steel_galvanized/machine_casing",
            lv["slots"]["M"]["cc"],
        )
        self.assertEqual(
            "cruciblecraft:material_wire_cutter", lv["slots"]["x"]["cc"]
        )
        self.assertEqual("wirecutter catalyst", lv["slots"]["x"]["gt6"])
        self.assertNotIn("screwdriver", lv["slots"]["x"]["gt6"])

    def test_catalog_is_source_exact(self) -> None:
        catalog = io.load_json(CATALOG)
        self.assertEqual(10, len(catalog["machines"]))
        ids = {row["id"] for row in catalog["machines"]}
        self.assertIn("cruciblecraft:thermoelectric_cooler_lv", ids)
        self.assertIn("cruciblecraft:thermofluxic_cooler_enderium", ids)
        lv = next(row for row in catalog["machines"] if row["source_id"] == 10161)
        self.assertEqual("steel_galvanized", lv["material"])
        self.assertEqual(32, lv["nbt_input"])
        self.assertEqual(8, lv["nbt_output"])
        self.assertEqual(["w", "x"], lv["recipe"]["catalysts"])
        self.assertNotIn("screwdriver", str(lv["recipe"]))
        lead = next(row for row in catalog["machines"] if row["source_id"] == 11161)
        self.assertEqual("lead", lead["material"])
        self.assertEqual("cruciblecraft:thermoelectric_cooler_lv", lead["host_id"])
        self.assertEqual([], lead["recipe"]["catalysts"])

    def test_art_and_java_are_source_backed(self) -> None:
        manifest = io.load_json(MANIFEST)
        destinations = {row["destination"] for row in manifest["imports"]}
        self.assertTrue(
            any("cooler/cryo_electric/colored/front.png" in row for row in destinations)
        )
        self.assertTrue(
            any("cooler/cryo_flux/overlay_active/back.png" in row for row in destinations)
        )
        for destination in destinations:
            self.assertTrue(
                (ROOT / "src" / "main" / "resources" / destination).is_file(),
                destination,
            )
        java = (JAVA / "CoolerBlockEntity.java").read_text(encoding="utf-8")
        self.assertNotIn("EnergyConverterHost", java)
        self.assertNotIn("EnergyType.RF", java)
        self.assertIn("EnergyType.CU", java)
        self.assertIn("EnergyType.HEAT", java)
        self.assertIn("IEnergyStorage", java)
        self.assertIn("NBT_WASTE_ENERGY", java)

    def test_capability_owns_ten_coolers(self) -> None:
        capability = io.load_json(CAPABILITY)
        self.assertEqual(CAPABILITY_SLUG, capability["slug"])
        self.assertEqual("runtime_ready", capability["maturity"])
        self.assertIn(capability["workflow"], {"active", "accepted", "paused"})
        self.assertEqual(["capability-runtime"], capability["profiles"])
        self.assertNotIn("player-complete", capability["profiles"])
        compiled = ledger.compile_ledger()
        self.assertNotIn(CAPABILITY_SLUG, compiled["declared_player_complete"])
        owned = capability["identity_disposition"][0]
        self.assertEqual("thermal:cooler", owned["semantic_key"])
        self.assertEqual("new_distinct", owned["disposition"])
        self.assertEqual(10, len(owned["runtime_ids"]))
        self.assertIn("cruciblecraft:thermoelectric_cooler_lv", owned["runtime_ids"])
        self.assertIn("not EnergyType.RF", owned["reason"])
        ns = (
            ROOT
            / "src"
            / "main"
            / "resources"
            / "data"
            / "cruciblecraft_wave_runtime_cooler"
        )
        self.assertTrue((ns / "structure" / "empty.nbt").is_file())
        self.assertTrue((ns / "gametest" / "structure" / "empty.nbt").is_file())


if __name__ == "__main__":
    unittest.main()
