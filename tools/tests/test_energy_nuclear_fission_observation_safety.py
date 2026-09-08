#!/usr/bin/env python3
"""Fission observation/safety closed card: Jade + hazmat + HU, player_complete."""
from __future__ import annotations

import json
import unittest

from tools import capability_ledger as ledger
from tools import io_common as io
from tools.recipe_bulk.slugs import KNOWN_SEMANTIC_SLUGS
from tools.wave_closeout import spec_for

SLUG = "runtime/fission-observation-safety"
CAPABILITY_SLUG = "energy/nuclear-fission-observation-safety"
HOT = "energy/nuclear-fission-hot-fluids"
ROOT = io.ROOT
WAVE = io.TOOLS / "waves" / "runtime" / "fission-observation-safety"
MATRIX = WAVE / "d0_observation_source_matrix.json"
CAPABILITY = (
    ROOT
    / "tools"
    / "capabilities"
    / "energy"
    / "nuclear-fission-observation-safety"
    / "capability.json"
)
HOT_CAPABILITY = (
    ROOT / "tools" / "capabilities" / "energy" / "nuclear-fission-hot-fluids" / "capability.json"
)
MANIFEST = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "assets"
    / "cruciblecraft"
    / "gt6_fission_observation_art_manifest.json"
)
JADE = ROOT / "tools" / "jade_observation_matrix.json"
CATALOG = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "semantic_object_catalog.json"
)
HAZMAT = {
    "gt_object/gt_armor_hazmat_radiation_head_m0",
    "gt_object/gt_armor_hazmat_radiation_chest_m0",
    "gt_object/gt_armor_hazmat_radiation_legs_m0",
    "gt_object/gt_armor_hazmat_radiation_boots_m0",
    "gt_object/gt_armor_hazmat_heat_head_m0",
    "gt_object/gt_armor_hazmat_heat_chest_m0",
    "gt_object/gt_armor_hazmat_heat_legs_m0",
    "gt_object/gt_armor_hazmat_heat_boots_m0",
}


class FissionObservationSafetyCardTest(unittest.TestCase):
    def test_slug_is_zero_family_runtime_card(self) -> None:
        self.assertIn(SLUG, KNOWN_SEMANTIC_SLUGS)
        spec = spec_for(SLUG)
        self.assertIsNone(spec.unique_active_wave)
        self.assertTrue(spec.next_unassigned)
        self.assertEqual(0, spec.owns_families)
        topology = io.load_json(WAVE / "topology.json")
        self.assertIsNone(topology["unique_active_wave"])
        self.assertTrue(topology["next_unassigned"])
        readiness = io.load_json(WAVE / "readiness.json")
        self.assertIsNone(readiness["unique_active_wave"])
        self.assertTrue(readiness["nuclear_started"])
        self.assertEqual(2, readiness["evidence"]["core_identity_count"])
        self.assertEqual(37, readiness["evidence"]["battery_identity_count"])
        self.assertEqual(179, readiness["evidence"]["converter_live_rows"])
        self.assertEqual(8, readiness["evidence"]["hazmat_piece_count"])
        self.assertEqual(
            "player_complete",
            readiness["evidence"]["observation_safety_status"],
        )

    def test_source_matrix_is_exact_or_blocked(self) -> None:
        matrix = io.load_json(MATRIX)
        self.assertEqual(13, matrix["source_contract_rows"])
        self.assertEqual(9, matrix["exact_rows"])
        self.assertEqual(4, matrix["explicitly_blocked_rows"])
        self.assertEqual(0, matrix["stand_in_parts"])
        self.assertEqual(0, matrix["kelvin_fields"])
        self.assertEqual(0, matrix["world_explode_uncommented"])
        ready = [row for row in matrix["branches"] if row["disposition"] == "ready"]
        blocked = [
            row
            for row in matrix["branches"]
            if row["disposition"] == "explicitly_blocked"
        ]
        self.assertEqual(9, len(ready))
        self.assertEqual(4, len(blocked))
        self.assertEqual(
            {
                "geiger_empty",
                "geiger_canner_helium",
                "geiger_canner_neon",
                "geiger_canner_argon",
            },
            {row["branch"] for row in blocked},
        )
        self.assertEqual("HU", matrix["temperature_contract"]["heat_unit"])
        self.assertEqual("blocked", matrix["fail_semantics"]["world_explode"])

    def test_capability_is_player_complete(self) -> None:
        capability = ledger.load_capability(CAPABILITY)
        self.assertEqual("player_complete", capability["maturity"])
        self.assertEqual("accepted", capability["workflow"])
        self.assertEqual(CAPABILITY_SLUG, capability["slug"])
        self.assertEqual(SLUG, capability["wave_slug"])
        self.assertEqual([HOT], capability["depends_on"])
        keys = {row["semantic_key"]: row for row in capability["identity_disposition"]}
        self.assertEqual("new_distinct", keys["reactor:observation_safety"]["disposition"])
        self.assertEqual(8, len(keys["reactor:hazmat"]["runtime_ids"]))
        blocked = {
            row["semantic_key"]
            for row in capability["identity_disposition"]
            if row["disposition"] == "blocked"
        }
        self.assertEqual(
            {
                "reactor:temperature_kelvin",
                "reactor:world_explode",
                "reactor:geiger_empty_capcellcon",
                "reactor:backpack_radioactivity",
            },
            blocked,
        )
        hot = ledger.load_capability(HOT_CAPABILITY)
        hot_obs = next(
            row
            for row in hot["identity_disposition"]
            if row["semantic_key"] == "reactor:observation_safety"
        )
        self.assertEqual("blocked", hot_obs["disposition"])
        ns = (
            ROOT
            / "src"
            / "main"
            / "resources"
            / "data"
            / "cruciblecraft_wave_runtime_fission_observation_safety"
        )
        self.assertTrue((ns / "structure" / "empty.nbt").is_file())
        self.assertTrue((ns / "gametest" / "structure" / "empty.nbt").is_file())
        self.assertTrue(MANIFEST.is_file())

    def test_catalog_has_eight_wearable_and_thermometer(self) -> None:
        catalog = json.loads(CATALOG.read_text(encoding="utf-8"))
        self.assertEqual(247, catalog["identity_count"])
        by_path = {row["registry_path"]: row for row in catalog["identities"]}
        self.assertEqual(HAZMAT, HAZMAT & set(by_path))
        for path in HAZMAT:
            self.assertNotEqual("minecraft:item/iron_ingot", by_path[path]["texture"])
            self.assertTrue(by_path[path]["texture"].startswith("cruciblecraft:item/gt6_import/"))
        self.assertIn("gt_multiitem/multiitem_randomtools_m10000", by_path)

    def test_jade_families_are_ready(self) -> None:
        matrix = json.loads(JADE.read_text(encoding="utf-8"))
        families = {row["family"]: row for row in matrix["families"]}
        for name in ("reactor_core", "battery", "converter_dynamo"):
            row = families[name]
            self.assertEqual("ready", row["status"])
            self.assertEqual("jade_server_data", row["sync_path"])
            self.assertEqual(CAPABILITY_SLUG, row["owner"])
            self.assertTrue(row["translation_keys"])
