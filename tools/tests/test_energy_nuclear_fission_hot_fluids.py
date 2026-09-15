#!/usr/bin/env python3
"""Fission hot-fluids closed card: 11/9/8, player_complete."""
from __future__ import annotations

import json
import unittest

from tools import capability_ledger as ledger
from tools import io_common as io
from tools.recipe_bulk.slugs import KNOWN_SEMANTIC_SLUGS
from tools.wave_closeout import spec_for

SLUG = "runtime/fission-hot-fluids"
CAPABILITY_SLUG = "energy/nuclear-fission-hot-fluids"
SURVIVAL = "energy/nuclear-fission-survival"
ROOT = io.ROOT
WAVE = io.TOOLS / "waves" / "runtime" / "fission-hot-fluids"
MATRIX = WAVE / "d0_hot_fluid_source_matrix.json"
CAPABILITY = (
    ROOT
    / "tools"
    / "capabilities"
    / "energy"
    / "nuclear-fission-hot-fluids"
    / "capability.json"
)
SURVIVAL_CAPABILITY = (
    ROOT
    / "tools"
    / "capabilities"
    / "energy"
    / "nuclear-fission-survival"
    / "capability.json"
)
MANIFEST = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "assets"
    / "cruciblecraft"
    / "gt6_fission_hot_fluids_art_manifest.json"
)
HOT_IDS = {
    "hot_molten_tin",
    "hot_molten_sodium",
    "hot_semiheavy_water",
    "hot_heavy_water",
    "hot_tritiated_water",
    "hot_molten_licl",
    "hot_carbon_dioxide",
    "hot_helium",
}


class FissionHotFluidsCardTest(unittest.TestCase):
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
        self.assertTrue(topology["next_unassigned"])
        readiness = io.load_json(WAVE / "readiness.json")
        self.assertIsNone(readiness["unique_active_wave"])
        self.assertTrue(readiness["next_unassigned"])
        self.assertTrue(readiness["nuclear_started"])
        self.assertEqual(11, readiness["evidence"]["source_contract_rows"])
        self.assertEqual(9, readiness["evidence"]["cc_owned_conversion_rows"])
        self.assertEqual(8, readiness["evidence"]["hot_output_identity_rows"])
        self.assertEqual(2, readiness["evidence"]["core_identity_count"])
        self.assertEqual(
            "runtime_ready",
            readiness["evidence"]["hot_fluids_status"],
        )

    def test_source_matrix_is_exact_eleven_by_nine_by_eight(self) -> None:
        matrix = io.load_json(MATRIX)
        self.assertEqual(11, matrix["source_contract_rows"])
        self.assertEqual(9, matrix["cc_owned_conversion_rows"])
        self.assertEqual(8, matrix["hot_output_identity_rows"])
        self.assertEqual(0, matrix["missing"])
        self.assertEqual(0, matrix["extra"])
        self.assertEqual(0, matrix["duplicate"])
        self.assertEqual(0, matrix["hot_identity_aliases"])
        self.assertEqual(0, matrix["stand_in_fluids"])
        branches = matrix["branches"]
        self.assertEqual(11, len(branches))
        ready = [row for row in branches if row["disposition"] == "ready"]
        blocked = [
            row
            for row in branches
            if row["disposition"] in {"blocked", "out_of_scope_external"}
        ]
        self.assertEqual(9, len(ready))
        self.assertEqual(2, len(blocked))
        self.assertEqual(
            {"Coolant_IC2 -> Coolant_IC2_Hot", "Thorium_Salt -> LiCl"},
            {row["branch"] for row in blocked},
        )
        hot = {
            row["hot_output_registry_identity"].removeprefix("cruciblecraft:")
            for row in ready
            if row["hot_output_registry_identity"] != "cruciblecraft:steam"
        }
        self.assertEqual(HOT_IDS, hot)

    def test_capability_is_player_complete(self) -> None:
        capability = ledger.load_capability(CAPABILITY)
        self.assertEqual("runtime_ready", capability["maturity"])
        self.assertEqual("accepted", capability["workflow"])
        self.assertEqual(CAPABILITY_SLUG, capability["slug"])
        self.assertEqual(SLUG, capability["wave_slug"])
        self.assertEqual(
            None,
            capability["player_signoff"],
        )
        self.assertEqual([SURVIVAL], capability["depends_on"])
        keys = {
            row["semantic_key"]: row
            for row in capability["identity_disposition"]
        }
        self.assertEqual("new_distinct", keys["reactor:hot_fluids"]["disposition"])
        self.assertEqual(8, len(keys["reactor:hot_fluids"]["runtime_ids"]))
        blocked = {
            row["semantic_key"]
            for row in capability["identity_disposition"]
            if row["disposition"] == "blocked"
        }
        self.assertEqual(
            {
                "reactor:temperature_kelvin",
                "reactor:observation_safety",
                "reactor:coolant_ic2",
                "reactor:thorium_salt",
            },
            blocked,
        )
        survival = ledger.load_capability(SURVIVAL_CAPABILITY)
        survival_hot = next(
            row
            for row in survival["identity_disposition"]
            if row["semantic_key"] == "reactor:hot_fluids"
        )
        self.assertEqual("blocked", survival_hot["disposition"])
        ns = (
            ROOT
            / "src"
            / "main"
            / "resources"
            / "data"
            / "cruciblecraft_wave_runtime_fission_hot_fluids"
        )
        self.assertTrue((ns / "structure" / "empty.nbt").is_file())
        self.assertTrue((ns / "gametest" / "structure" / "empty.nbt").is_file())
        self.assertTrue(MANIFEST.is_file())
        manifest = json.loads(MANIFEST.read_text(encoding="utf-8"))
        png = {
            row["destination"].rsplit("/", 1)[-1].removesuffix(".png")
            for row in manifest["imports"]
            if row["destination"].endswith(".png")
            and not row["destination"].endswith(".mcmeta")
        }
        self.assertEqual(HOT_IDS, png)


if __name__ == "__main__":
    unittest.main()
