#!/usr/bin/env python3
"""Live Sanding Machine card: all 7637 rows selected."""
from __future__ import annotations

import importlib.util
import unittest

from tools import capability_ledger as ledger
from tools import census_common as census
from tools import io_common as io
from tools.build_recipe_bulk import WAVE_CHOICES
from tools.recipe_bulk.slugs import KNOWN_SEMANTIC_SLUGS
from tools.recipe_bulk.waves import SEMANTIC_COMPILE_ORDER, recipe_wave

SLUG = "machines/sanding"
ROOT = io.ROOT
WAVE = ROOT / "tools" / "waves" / "machines" / "sanding"
CAPABILITY = (
    ROOT / "tools" / "capabilities" / "machines" / "sanding" / "capability.json"
)
PLAN_ACTIVE = (
    ROOT / "docs" / "history" / "card-plans" / "active" / "打磨机详细计划.md"
)
PLAN_CLOSED = (
    ROOT / "docs" / "history" / "card-plans" / "closed" / "打磨机详细计划.md"
)
PLAN_PREP = (
    ROOT / "docs" / "history" / "card-plans" / "prep" / "打磨机详细计划.md"
)
ASSETS = ROOT / "src" / "main" / "resources" / "assets" / "cruciblecraft"


def _load_builder():
    path = WAVE / "build_sanding.py"
    spec = importlib.util.spec_from_file_location("sanding_builder", path)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    spec.loader.exec_module(module)
    return module


builder = _load_builder()


class SandingCardTest(unittest.TestCase):
    def test_slug_and_recipe_wave(self) -> None:
        self.assertIn(SLUG, KNOWN_SEMANTIC_SLUGS)
        self.assertNotIn(SLUG, SEMANTIC_COMPILE_ORDER)
        self.assertNotIn(SLUG, WAVE_CHOICES)
        spec = recipe_wave(SLUG)
        self.assertEqual("sanding", spec.path_prefix)
        self.assertEqual("cruciblecraft:sanding", spec.host)
        self.assertEqual("lock", spec.publication_policy)

    def test_builder_check_passes(self) -> None:
        self.assertEqual([], builder.check())

    def test_selected_and_overflow_stay_exact(self) -> None:
        work = census.load_json(WAVE / "source_pack" / "work_set.json")
        overflow = census.load_json(WAVE / "overflow.json")
        lock = census.load_json(WAVE / "production_lock.json")
        accounting = work["accounting"]
        self.assertEqual(7_637, accounting["source_rows"])
        self.assertEqual(7_637, accounting["selected_rows"])
        self.assertEqual(0, accounting["overflow_rows"])
        self.assertEqual(0, overflow["blocked_rows"])
        self.assertEqual(7_637, lock["production"]["relation_count"])
        self.assertIn("not player_complete", lock["note"])
        self.assertNotIn("programmed_circuit", str(overflow))
        self.assertIn(
            "cruciblecraft:coralium/tool_head_raw_hoe",
            builder.common.registered_runtime_ids(),
        )
        self.assertNotIn("unregistered runtime item", str(overflow))
        generated = builder.live_family_files()
        self.assertEqual(1, len(generated))
        policy = census.load_json(
            ROOT
            / "src"
            / "recipe_generated"
            / "resources"
            / "data"
            / "cruciblecraft"
            / "recipe"
            / "publication_policy"
            / "sanding.json"
        )
        self.assertEqual("immediate", policy["policy_type"])
        self.assertEqual(7_637, policy["relation_count"])
        self.assertEqual("cruciblecraft:sanding", policy["target_map"])
        self.assertNotIn("player_complete", str(policy))

    def test_reclaim_sources_are_source_backed(self) -> None:
        reclaim_source = census.load_json(
            ROOT / "tools" / "tool_head_prefix_reclaim_source.json"
        )
        self.assertEqual(12_278, reclaim_source["counts"]["identity_count"])
        remap = census.load_json(ROOT / "tools" / "tool_head_prefix_remap.json")
        self.assertEqual(13_938, remap["counts"]["mapped"])
        self.assertEqual(0, remap["counts"]["remainder"])
        forms = census.load_json(WAVE / "required_forms.json")
        self.assertEqual(13, forms["counts"]["form_count"])
        self.assertEqual(3_871, forms["counts"]["form_pairs"])
        overlay = census.load_json(WAVE / "mte_runtime_overlay.json")
        mappings = {
            int(row["meta"]): row["runtime_id"]
            for row in overlay["mappings"]
        }
        self.assertEqual(
            {
                14_502: "cruciblecraft:red_energium_crystal_mv",
                14_512: "cruciblecraft:cyan_energium_crystal_mv",
            },
            mappings,
        )
        battery_tiers = census.load_json(
            ROOT
            / "src"
            / "main"
            / "resources"
            / "data"
            / "cruciblecraft"
            / "energy_battery_tiers.json"
        )["tiers"]
        source_to_runtime = {
            int(row["source_id"]): str(row["id"])
            for row in battery_tiers
            if int(row["source_id"]) in mappings
        }
        self.assertEqual(mappings, source_to_runtime)
        self.assertIn(
            "cruciblecraft:red_energium_crystal_mv",
            builder.common.registered_runtime_ids(),
        )
        self.assertIn(
            "cruciblecraft:cyan_energium_crystal_mv",
            builder.common.registered_runtime_ids(),
        )

    def test_d0_four_hosts_are_source_exact(self) -> None:
        document = census.load_json(WAVE / "d0_obtain_matrix.json")
        self.assertEqual(["SGS", "XXX", "wMh"], document["grid"])
        statuses = {row["host"]: row["status"] for row in document["hosts"]}
        self.assertEqual(
            {
                20511: "source_exact",
                20512: "source_exact",
                20513: "source_exact",
                20514: "source_exact",
            },
            statuses,
        )
        self.assertEqual(
            "minecraft:sandstone",
            next(row for row in document["hosts"] if row["host"] == 20511)[
                "sandstone"
            ]["cc"],
        )
        self.assertNotIn("programmed_circuit", str(document))

    def test_prep_import_cannot_write_live_tree(self) -> None:
        with self.assertRaisesRegex(ValueError, "src/recipe_generated"):
            recipe_wave("prep/sanding")
        self.assertNotIn("prep/sanding", SEMANTIC_COMPILE_ORDER)
        self.assertNotIn("prep/sanding", WAVE_CHOICES)

    def test_capability_is_runtime_ready(self) -> None:
        capability = census.load_json(CAPABILITY)
        self.assertEqual(SLUG, capability["slug"])
        self.assertEqual("runtime_ready", capability["maturity"])
        self.assertEqual(["capability-runtime"], capability["profiles"])
        self.assertEqual(
            [
                "bronzeHostRunsFirstLiveRecipe",
                "fourHostsAreSurvivalCraftable",
                "liveMapPublishesSelectedRows",
                "playerSurfaceIsRegistered",
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
            self.assertNotEqual(SLUG, compiled["unique_active_slug"])
            self.assertFalse(PLAN_ACTIVE.is_file())
            self.assertTrue(PLAN_CLOSED.is_file())
        blocked = {
            row["semantic_key"]
            for row in capability["identity_disposition"]
            if row["disposition"] == "blocked"
        }
        self.assertEqual(
            {"machine:sanding:grindstone_32703"},
            blocked,
        )
        owned = next(
            row
            for row in capability["identity_disposition"]
            if row["disposition"] == "new_distinct"
        )
        self.assertEqual(4, len(owned["runtime_ids"]))

    def test_art_is_copied_and_not_aliased(self) -> None:
        manifest = census.load_json(ASSETS / "gt6_sanding_art_manifest.json")
        self.assertGreaterEqual(len(manifest["imports"]), 20)
        gt6_root = ROOT / "gt6_referencable_port_code" / "gregtech6_w"
        self.assertTrue(gt6_root.is_dir())
        for row in manifest["imports"]:
            destination = ROOT / "src" / "main" / "resources" / row["destination"]
            source = gt6_root / row["gt6_source"]
            self.assertTrue(destination.is_file(), row["destination"])
            self.assertTrue(source.is_file(), row["gt6_source"])
            self.assertEqual(source.read_bytes(), destination.read_bytes())
            self.assertNotIn("multiblock_casing", row["destination"])
        self.assertTrue(
            (ASSETS / "textures" / "gui" / "machines" / "sanding.png").is_file()
        )
        self.assertTrue(
            (
                ASSETS
                / "textures"
                / "block"
                / "machine"
                / "sander"
                / "colored"
                / "front.png"
            ).is_file()
        )

    def test_unique_active_docs_and_ledger_hand_off(self) -> None:
        compiled = ledger.compile_ledger()
        topology = census.load_json(WAVE / "topology.json")
        readiness = census.load_json(WAVE / "readiness.json")
        capability = census.load_json(CAPABILITY)
        if capability["workflow"] == "active":
            self.assertEqual(SLUG, compiled["unique_active_slug"])
            self.assertEqual(SLUG, topology["unique_active_wave"])
            self.assertEqual(SLUG, readiness["unique_active_wave"])
        else:
            self.assertNotEqual(SLUG, compiled["unique_active_slug"])
            self.assertIsNone(topology["unique_active_wave"])
            self.assertIsNone(readiness["unique_active_wave"])
        notes = census.load_json(WAVE / "runtime_notes.json")
        self.assertEqual("UP", notes["energy_accepted_sides"])
        self.assertIn("grindstone_32703", notes["out_of_scope"])


if __name__ == "__main__":
    unittest.main()
