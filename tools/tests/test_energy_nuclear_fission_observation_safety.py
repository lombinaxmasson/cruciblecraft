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
    "radiation/hazard_suit_helmet",
    "radiation/hazard_suit_shirt",
    "radiation/hazard_suit_pants",
    "radiation/hazard_suit_boots",
    "heat/protection_suit_helmet",
    "heat/protection_suit_shirt",
    "heat/protection_suit_pants",
    "heat/protection_suit_boots",
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
        self.assertEqual(13, matrix["exact_rows"])
        self.assertEqual(0, matrix["explicitly_blocked_rows"])
        self.assertEqual(0, matrix["stand_in_parts"])
        self.assertEqual(0, matrix["kelvin_fields"])
        self.assertEqual(0, matrix["world_explode_uncommented"])
        ready = [row for row in matrix["branches"] if row["disposition"] == "ready"]
        blocked = [
            row
            for row in matrix["branches"]
            if row["disposition"] == "explicitly_blocked"
        ]
        self.assertEqual(13, len(ready))
        self.assertEqual(0, len(blocked))
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
                "reactor:backpack_radioactivity",
            },
            blocked,
        )
        self.assertEqual("new_distinct", keys["reactor:geiger_empty_capcellcon"]["disposition"])
        self.assertIn(
            "cruciblecraft:aluminium/capcellcon",
            keys["reactor:geiger_empty_capcellcon"]["runtime_ids"],
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
        self.assertEqual(249, catalog["identity_count"])
        by_path = {row["registry_path"]: row for row in catalog["identities"]}
        self.assertEqual(HAZMAT, HAZMAT & set(by_path))
        for path in HAZMAT:
            self.assertNotEqual("minecraft:item/iron_ingot", by_path[path]["texture"])
            self.assertTrue(by_path[path]["texture"].startswith("cruciblecraft:item/gt6_import/"))
        self.assertIn("mercury/thermometer_measures_temperature", by_path)
        self.assertEqual(
            "cruciblecraft:item/gt6_import/thermometer_quicksilver",
            by_path["mercury/thermometer_measures_temperature"]["texture"],
        )
        self.assertEqual(
            "cruciblecraft:item/gt6_import/geiger_empty",
            by_path["tool/geiger_counter_empty_fill_with_proper_inert_gas"]["texture"],
        )
        self.assertEqual(
            "cruciblecraft:item/gt6_import/geiger_filled",
            by_path["tool/geiger_counter_measures_neutron_count"]["texture"],
        )

    def test_art_is_copied_from_gregtech6_w(self) -> None:
        manifest = json.loads(MANIFEST.read_text(encoding="utf-8"))
        self.assertTrue(manifest["source_present"])
        self.assertEqual("gt6_referencable_port_code/gregtech6_w", manifest["source"])
        assets = ROOT / "src" / "main" / "resources"
        for row in manifest["imports"]:
            dest = assets / row["destination"]
            self.assertTrue(dest.is_file(), row["destination"])
            self.assertGreater(dest.stat().st_size, 120, row["destination"])
            self.assertNotIn("palette icon", row.get("note", ""))
            self.assertNotIn("gregtech6_w absent", row.get("note", ""))
        thermometer = (
            assets
            / "assets"
            / "cruciblecraft"
            / "textures"
            / "item"
            / "gt6_import"
            / "thermometer_quicksilver.png"
        )
        self.assertEqual(512, thermometer.stat().st_size)
        oracle = (
            ROOT
            / "gt6_referencable_port_code"
            / "gregtech6_w"
            / "src"
            / "main"
            / "resources"
        )
        if not oracle.is_dir():
            return
        copies = 0
        for row in manifest["imports"]:
            src = oracle / row["gt6_source"]
            dest = assets / row["destination"]
            if src.is_file() and dest.read_bytes() == src.read_bytes():
                copies += 1
        self.assertGreaterEqual(copies, 11)

    def test_jade_families_are_ready(self) -> None:
        matrix = json.loads(JADE.read_text(encoding="utf-8"))
        families = {row["family"]: row for row in matrix["families"]}
        for name in ("reactor_core", "battery", "converter_dynamo"):
            row = families[name]
            self.assertEqual("ready", row["status"])
            self.assertEqual("jade_server_data", row["sync_path"])
            self.assertEqual(CAPABILITY_SLUG, row["owner"])
            self.assertTrue(row["translation_keys"])

    def test_geiger_obtain_recipes_are_source_exact(self) -> None:
        recipe = (
            ROOT
            / "src"
            / "main"
            / "resources"
            / "data"
            / "cruciblecraft"
            / "recipe"
        )
        empty = json.loads(
            (recipe / "tool" / "geiger_counter_empty_fill_with_proper_inert_gas.json").read_text(
                encoding="utf-8"
            )
        )
        self.assertEqual(["TXT", "PCP", "TdT"], empty["pattern"])
        self.assertEqual(
            "cruciblecraft:aluminium/capcellcon",
            empty["ingredients"]["X"]["item"],
        )
        self.assertEqual(
            "cruciblecraft:circuit_basic",
            empty["ingredients"]["C"]["item"],
        )
        self.assertEqual(
            "cruciblecraft:material_screwdriver",
            empty["catalysts"]["d"]["item"],
        )
        capcellcon = json.loads(
            (recipe / "nuclear" / "aluminium_capcellcon.json").read_text(encoding="utf-8")
        )
        self.assertEqual("cruciblecraft:extruder", capcellcon["map"])
        self.assertEqual(64, capcellcon["duration"])
        self.assertEqual(16, capcellcon["eut"])
        self.assertEqual([1, 0], capcellcon["item_input_counts"])
        self.assertEqual(9, capcellcon["item_outputs"][0]["count"])
        for gas in ("helium", "neon", "argon"):
            fill = json.loads(
                (recipe / "nuclear" / f"geiger_canner_{gas}.json").read_text(
                    encoding="utf-8"
                )
            )
            self.assertEqual("cruciblecraft:canner", fill["map"])
            self.assertEqual(64, fill["duration"])
            self.assertEqual(16, fill["eut"])
            self.assertEqual(
                f"cruciblecraft:{gas}",
                fill["fluid_inputs"][0]["id"],
            )
            self.assertEqual(1000, fill["fluid_inputs"][0]["amount"])
        prefix = json.loads(
            (
                ROOT
                / "src"
                / "main"
                / "resources"
                / "data"
                / "cruciblecraft"
                / "material_prefixes"
                / "capcellcon.json"
            ).read_text(encoding="utf-8")
        )
        self.assertEqual(16, prefix["units"])
        self.assertEqual([], prefix["capabilities"])
        manifest = json.loads(
            (
                ROOT
                / "src"
                / "main"
                / "resources"
                / "assets"
                / "cruciblecraft"
                / "gt6_geiger_obtain_art_manifest.json"
            ).read_text(encoding="utf-8")
        )
        self.assertTrue(manifest["source_present"])
        assets = ROOT / "src" / "main" / "resources"
        for row in manifest["imports"]:
            dest = assets / row["destination"]
            self.assertTrue(dest.is_file(), row["destination"])
            self.assertGreater(dest.stat().st_size, 80, row["destination"])
        readiness = io.load_json(WAVE / "readiness.json")
        self.assertEqual(15, readiness["generated_recipe_count"])
        signoff = io.load_json(
            ROOT
            / "tools"
            / "capabilities"
            / "energy"
            / "nuclear-fission-observation-safety"
            / "player_signoff.json"
        )
        self.assertFalse(signoff["checklist"]["geiger_obtain_explicitly_blocked"])
        self.assertTrue(signoff["checklist"]["no_stand_in_capcellcon"])
        self.assertIn(
            "tool/geiger_counter_empty_fill_with_proper_inert_gas",
            signoff["craftable_items"],
        )

    def test_aluminium_capcellcon_is_a_required_gated_form(self) -> None:
        required = io.load_json(WAVE / "required_forms.json")
        self.assertIn("capcellcon", required["required_forms"]["aluminium"])
        gate = json.loads(
            (
                ROOT
                / "src"
                / "main"
                / "resources"
                / "data"
                / "cruciblecraft"
                / "material_registration_gate.json"
            ).read_text(encoding="utf-8")
        )
        self.assertIn("capcellcon", gate["materials"]["aluminium"])
