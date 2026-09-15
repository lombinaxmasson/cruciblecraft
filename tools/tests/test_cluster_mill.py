#!/usr/bin/env python3
"""Cluster Mill player_complete card: 307 selected rows, foil prefix gated."""
from __future__ import annotations

import importlib.util
import unittest
from pathlib import Path

from tools import capability_ledger as ledger
from tools import census_common as census
from tools import io_common as io
from tools.build_recipe_bulk import WAVE_CHOICES
from tools.recipe_bulk.slugs import KNOWN_SEMANTIC_SLUGS
from tools.recipe_bulk.waves import SEMANTIC_COMPILE_ORDER, recipe_wave

SLUG = "machines/cluster-mill"
CAPABILITY_SLUG = "machines/cluster-mill"
ROOT = io.ROOT
WAVE = ROOT / "tools" / "waves" / "machines" / "cluster-mill"
CAPABILITY = (
    ROOT / "tools" / "capabilities" / "machines" / "cluster-mill" / "capability.json"
)
ASSETS = ROOT / "src" / "main" / "resources" / "assets" / "cruciblecraft"


def _load_builder():
    path = WAVE / "build_cluster_mill.py"
    spec = importlib.util.spec_from_file_location("cluster_mill_builder", path)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    spec.loader.exec_module(module)
    return module


builder = _load_builder()


class ClusterMillCardTest(unittest.TestCase):
    def test_slug_is_known_and_not_a_recipes_profile_gate(self) -> None:
        self.assertIn(SLUG, KNOWN_SEMANTIC_SLUGS)
        self.assertNotIn(SLUG, SEMANTIC_COMPILE_ORDER)
        self.assertNotIn(SLUG, WAVE_CHOICES)
        spec = recipe_wave(SLUG)
        self.assertEqual("clustermill", spec.path_prefix)
        self.assertEqual("cruciblecraft:clustermill", spec.host)
        self.assertEqual("lock", spec.publication_policy)

    def test_builder_check_passes(self) -> None:
        self.assertEqual([], builder.check())

    def test_selected_and_overflow_stay_exact(self) -> None:
        work = census.load_json(WAVE / "source_pack" / "work_set.json")
        overflow = census.load_json(WAVE / "overflow.json")
        lock = census.load_json(WAVE / "production_lock.json")
        accounting = work["accounting"]
        self.assertEqual(307, accounting["source_rows"])
        self.assertEqual(307, accounting["selected_rows"])
        self.assertEqual(0, accounting["overflow_rows"])
        self.assertEqual(0, overflow["blocked_rows"])
        self.assertEqual(307, lock["production"]["relation_count"])
        self.assertIn("not player_complete", lock["note"])
        self.assertEqual([], overflow.get("overflow") or [])
        self.assertNotIn("programmed_circuit", str(overflow))
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
            / "cluster_mill.json"
        )
        self.assertEqual("immediate", policy["policy_type"])
        self.assertEqual(307, policy["relation_count"])
        self.assertEqual("cruciblecraft:clustermill", policy["target_map"])
        self.assertEqual(0, policy["cache_ceiling"])
        self.assertNotIn("player_complete", str(policy))
        required = census.load_json(WAVE / "required_forms.json")
        self.assertEqual(60, required["counts"]["required_materials"])
        self.assertEqual(61, required["counts"]["required_form_pairs"])
        self.assertEqual(
            ["foil", "plate"], required["required_forms"]["netherized_diamond"]
        )
        for forms in required["required_forms"].values():
            self.assertIn("foil", forms)
            self.assertNotIn("plate_gem", forms)
            self.assertNotIn("dense_plate", forms)
        gate = census.load_json(
            ROOT
            / "src"
            / "main"
            / "resources"
            / "data"
            / "cruciblecraft"
            / "material_registration_gate.json"
        )
        self.assertIn("foil", gate["materials"]["kalendrite"])
        self.assertIn("foil", gate["materials"]["netherized_diamond"])
        self.assertIn("plate", gate["materials"]["netherized_diamond"])
        self.assertIn(
            "machines_cluster_mill_required_forms",
            gate["java_overlay_sections"],
        )

    def test_d0_four_hosts_are_source_exact(self) -> None:
        document = census.load_json(WAVE / "d0_obtain_matrix.json")
        self.assertEqual(["SSS", "wGh", "SMS"], document["grid"])
        statuses = {row["host"]: row["status"] for row in document["hosts"]}
        self.assertEqual(
            {
                20141: "source_exact",
                20142: "source_exact",
                20143: "source_exact",
                20144: "source_exact",
            },
            statuses,
        )
        for row in document["hosts"]:
            casing = row["casing"]["cc"]
            self.assertTrue(casing.endswith("/machine_casing_quadruple"))
            self.assertNotIn("machine_casing_double", casing)
            self.assertEqual("ok", row["small_gear"]["status"])
        self.assertNotIn("programmed_circuit", str(document))

    def test_prep_import_cannot_write_live_tree(self) -> None:
        with self.assertRaisesRegex(ValueError, "src/recipe_generated"):
            recipe_wave("prep/cluster-mill")
        self.assertNotIn("prep/cluster-mill", SEMANTIC_COMPILE_ORDER)
        self.assertNotIn("prep/cluster-mill", WAVE_CHOICES)
        self.assertNotIn("prep/cluster-mill", KNOWN_SEMANTIC_SLUGS)

    def test_capability_is_player_complete(self) -> None:
        capability = census.load_json(CAPABILITY)
        self.assertEqual(CAPABILITY_SLUG, capability["slug"])
        self.assertEqual("runtime_ready", capability["maturity"])
        self.assertEqual("accepted", capability["workflow"])
        self.assertEqual(
            ["capability-runtime"],
            capability["profiles"],
        )
        self.assertEqual(
            [
                "bronzeHostRunsFirstLiveRecipe",
                "fourHostsAreSurvivalCraftable",
                "liveMapPublishesThreeHundredSevenSelectedRows",
                "playerSurfaceIsRegistered",
            ],
            capability["required_test_ids"],
        )
        self.assertEqual(
            None,
            capability["player_signoff"],
        )
        compiled = ledger.compile_ledger()
        self.assertNotIn(CAPABILITY_SLUG, compiled["declared_player_complete"])
        blocked = {
            row["semantic_key"]
            for row in capability["identity_disposition"]
            if row["disposition"] == "blocked"
        }
        self.assertEqual(set(), blocked)
        owned = next(
            row
            for row in capability["identity_disposition"]
            if row["disposition"] == "new_distinct"
        )
        self.assertEqual(4, len(owned["runtime_ids"]))

    def test_art_is_copied_and_not_aliased(self) -> None:
        manifest = census.load_json(ASSETS / "gt6_cluster_mill_art_manifest.json")
        self.assertGreaterEqual(len(manifest["imports"]), 24)
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
            (ASSETS / "textures" / "gui" / "machines" / "clustermill.png").is_file()
        )

    def test_unique_active_docs_and_ledger_hand_off(self) -> None:
        ledger_doc = census.load_json(ROOT / "tools" / "blocked_recipe_ledger.json")
        self.assertIsNone(ledger_doc["unique_active_wave"])
        plan = (
            ROOT
            / "docs"
            / "history"
            / "card-plans"
            / "closed"
            / "集群轧机详细计划.md"
        )
        self.assertTrue(plan.is_file())
        self.assertFalse(
            (
                ROOT
                / "docs"
                / "history"
                / "card-plans"
                / "active"
                / "集群轧机详细计划.md"
            ).is_file()
        )
        topology = census.load_json(WAVE / "topology.json")
        readiness = census.load_json(WAVE / "readiness.json")
        self.assertIsNone(topology["unique_active_wave"])
        self.assertIsNone(readiness["unique_active_wave"])
        self.assertEqual("runtime_ready", readiness["evidence"]["status"])


if __name__ == "__main__":
    unittest.main()
