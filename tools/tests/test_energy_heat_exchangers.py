#!/usr/bin/env python3
"""Heat-exchanger unique-active card: eight FM.Hot to HU machines."""
from __future__ import annotations

import unittest

from tools import capability_ledger as ledger
from tools import io_common as io
from tools.recipe_bulk.slugs import KNOWN_SEMANTIC_SLUGS
from tools.wave_closeout import spec_for

SLUG = "runtime/heat-exchangers"
CAPABILITY_SLUG = "energy/heat-exchangers"
ROOT = io.ROOT
CAPABILITY = (
    ROOT / "tools" / "capabilities" / "energy" / "heat-exchangers" / "capability.json"
)
CATALOG = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "heat_exchangers.json"
)
HOT = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "recipe"
    / "energy"
    / "fuels_hot"
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
    / "heatexchanger"
)
WAVE = io.TOOLS / "waves" / "runtime" / "heat-exchangers"
D0 = WAVE / "d0_obtain_matrix.json"
MANIFEST = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "assets"
    / "cruciblecraft"
    / "gt6_heat_exchangers_art_manifest.json"
)
PREFIX = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "material_prefixes"
    / "machine_casing_quadruple.json"
)


class EnergyHeatExchangersCardTest(unittest.TestCase):
    def test_slug_is_zero_family_runtime_card(self) -> None:
        self.assertIn(SLUG, KNOWN_SEMANTIC_SLUGS)
        spec = spec_for(SLUG)
        self.assertIsNone(spec.unique_active_wave)
        self.assertTrue(spec.next_unassigned)
        self.assertEqual(0, spec.owns_families)
        self.assertIsNone(spec.production_lock)
        self.assertEqual(WAVE / "census_delta.json", spec.census)
        topology = io.load_json(WAVE / "topology.json")
        self.assertIsNone(topology["unique_active_wave"])
        readiness = io.load_json(WAVE / "readiness.json")
        self.assertIsNone(readiness["unique_active_wave"])
        self.assertEqual("runtime_ready", readiness["evidence"]["heat_exchangers_status"])
        self.assertTrue(topology["next_unassigned"])
        census_path = WAVE / "census_delta.json"
        if census_path.is_file():
            census = io.load_json(census_path)
            self.assertEqual(8, census["work_set"]["source_rows"])

    def test_player_complete_forbids_recipe_stand_ins(self) -> None:
        rule = (
            ROOT
            / ".cursor"
            / "rules"
            / "gt6-recipe-no-placeholder.mdc"
        ).read_text(encoding="utf-8")
        self.assertIn("no stand-in ingredients", rule)
        self.assertIn("缺形态时 DESIGN_POLICY 用已有材料做生存获得", rule)

    def test_d0_is_eight_exact_rows(self) -> None:
        document = io.load_json(D0)
        self.assertEqual(8, len(document["rows"]))
        self.assertEqual(
            {"exact"},
            {row["status"] for row in document["rows"]},
        )
        self.assertEqual(
            {9103, 9107, 9108, 9109, 9153, 9157, 9158, 9159},
            {row["source_id"] for row in document["rows"]},
        )
        blocked = document.get("blocked_extra_fm_hot") or []
        self.assertIn("lava", blocked)

    def test_catalog_and_hot_fuels_are_source_exact(self) -> None:
        catalog = io.load_json(CATALOG)
        self.assertEqual(8, len(catalog["machines"]))
        ids = {row["id"] for row in catalog["machines"]}
        self.assertIn("cruciblecraft:heat_exchanger_invar", ids)
        self.assertIn(
            "cruciblecraft:dense_heat_exchanger_tantalum_hafnium_carbide",
            ids,
        )
        tungstensteel = next(
            row
            for row in catalog["machines"]
            if row["source_id"] == 9108
        )
        self.assertEqual(9000, tungstensteel["efficiency_bps"])
        self.assertEqual(128, tungstensteel["hu_rate"])
        fuels = {path.stem for path in HOT.glob("*.json")}
        self.assertEqual(
            {
                "hot_molten_sodium",
                "hot_molten_tin",
                "hot_heavy_water",
                "hot_semiheavy_water",
                "hot_tritiated_water",
                "hot_carbon_dioxide",
                "hot_helium",
                "hot_molten_licl",
            },
            fuels,
        )
        self.assertNotIn("lava", fuels)
        tin = io.load_json(HOT / "hot_molten_tin.json")
        self.assertEqual("cruciblecraft:fuels_hot", tin["map"])
        self.assertEqual(40, tin["duration"])
        self.assertEqual(-1, tin["eut"])

    def test_quadruple_casing_is_in_scope_and_art_is_copied(self) -> None:
        prefix = io.load_json(PREFIX)
        self.assertEqual(3744, prefix["units"])
        self.assertIn("casingMachineQuadruple", prefix["aliases"])
        manifest = io.load_json(MANIFEST)
        destinations = {row["destination"] for row in manifest["imports"]}
        self.assertIn(
            "assets/cruciblecraft/textures/block/material/"
            "machine_casing_quadruple.png",
            destinations,
        )
        self.assertTrue(
            any("heat_exchanger/colored/front.png" in row for row in destinations)
        )
        self.assertTrue((JAVA / "HeatExchangerBlockEntity.java").is_file())
        java = (JAVA / "HeatExchangerBlockEntity.java").read_text(encoding="utf-8")
        self.assertNotIn("EnergyConverterHost", java)
        self.assertIn("EnergyType.HEAT", java)

    def test_capability_blocks_follow_up_machines(self) -> None:
        capability = io.load_json(CAPABILITY)
        self.assertEqual(CAPABILITY_SLUG, capability["slug"])
        self.assertEqual("runtime_ready", capability["maturity"])
        self.assertEqual("accepted", capability["workflow"])
        self.assertEqual(
            ["capability-runtime"],
            capability["profiles"],
        )
        self.assertNotIn("player-complete", capability["profiles"])
        compiled = ledger.compile_ledger()
        self.assertNotIn(CAPABILITY_SLUG, compiled["declared_player_complete"])
        blocked = {
            row["semantic_key"]
            for row in capability["identity_disposition"]
            if row["disposition"] == "blocked"
        }
        self.assertEqual(
            {
                "heat_exchanger:large_17197",
                "thermal:steam_turbine",
                "thermal:cooler",
            },
            blocked,
        )
        owned = next(
            row
            for row in capability["identity_disposition"]
            if row["semantic_key"] == "heat_exchanger:single_block"
        )
        self.assertEqual(8, len(owned["runtime_ids"]))
        self.assertNotIn(17197, {row["source_id"] for row in io.load_json(CATALOG)["machines"]})
        ns = (
            ROOT
            / "src"
            / "main"
            / "resources"
            / "data"
            / "cruciblecraft_wave_runtime_heat_exchangers"
        )
        self.assertTrue((ns / "structure" / "empty.nbt").is_file())
        self.assertTrue((ns / "gametest" / "structure" / "empty.nbt").is_file())


if __name__ == "__main__":
    unittest.main()
