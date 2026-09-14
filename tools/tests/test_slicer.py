#!/usr/bin/env python3
"""Slicer live machine card: 33 selected rows, LV/EV host obtain exact."""
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

SLUG = "machines/slicer"
CAPABILITY_SLUG = "machines/slicer"
ROOT = io.ROOT
WAVE = ROOT / "tools" / "waves" / "machines" / "slicer"
CAPABILITY = (
    ROOT / "tools" / "capabilities" / "machines" / "slicer" / "capability.json"
)
ASSETS = ROOT / "src" / "main" / "resources" / "assets" / "cruciblecraft"
PLAN_ACTIVE = (
    ROOT / "docs" / "history" / "card-plans" / "active" / "切片机详细计划.md"
)
PLAN_CLOSED = (
    ROOT / "docs" / "history" / "card-plans" / "closed" / "切片机详细计划.md"
)


def _load_builder():
    path = WAVE / "build_slicer.py"
    spec = importlib.util.spec_from_file_location("slicer_builder", path)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    spec.loader.exec_module(module)
    return module


builder = _load_builder()


class SlicerCardTest(unittest.TestCase):
    def test_slug_is_known_and_not_a_recipes_profile_gate(self) -> None:
        self.assertIn(SLUG, KNOWN_SEMANTIC_SLUGS)
        self.assertNotIn(SLUG, SEMANTIC_COMPILE_ORDER)
        self.assertNotIn(SLUG, WAVE_CHOICES)
        spec = recipe_wave(SLUG)
        self.assertEqual("slicer", spec.path_prefix)
        self.assertEqual("cruciblecraft:slicer", spec.host)
        self.assertEqual("lock", spec.publication_policy)

    def test_builder_check_passes(self) -> None:
        self.assertEqual([], builder.check())

    def test_food_multiitem_output_stays_registered(self) -> None:
        from tools.build_assembler_source import Catalogs
        from tools.recipe_bulk.ordinary_source import map_item_operand

        catalogs = Catalogs(
            prefix_item_to_form={},
            material_id_to_cc={},
            registered_forms={},
            form_items={},
            prefix_tags={},
            fluid_to_cc={},
            reachable=set(),
        )
        operand, errors = map_item_operand(
            {
                "item": "gregtech:gt.multiitem.food",
                "meta": 241,
                "count": 4,
            },
            catalogs,
            stone_runtime={},
            mte_runtime={},
            block_runtime={},
            item_overlay={},
            reused_aliases={},
            side="output",
        )
        self.assertEqual([], errors)
        self.assertEqual("explicit_object_expression", operand["mapping"])
        self.assertEqual("multiitem", operand["kind"])
        self.assertEqual(
            "cruciblecraft:apple/slice",
            operand["runtime_id"],
        )

    def test_selected_and_overflow_stay_exact(self) -> None:
        work = census.load_json(WAVE / "source_pack" / "work_set.json")
        overflow = census.load_json(WAVE / "overflow.json")
        lock = census.load_json(WAVE / "production_lock.json")
        accounting = work["accounting"]
        self.assertEqual(33, accounting["source_rows"])
        self.assertEqual(33, accounting["selected_rows"])
        self.assertEqual(0, accounting["overflow_rows"])
        self.assertEqual(0, overflow["blocked_rows"])
        self.assertEqual(33, lock["production"]["relation_count"])
        self.assertIn("not player_complete", lock["note"])
        self.assertNotIn("tiny_plate", str(overflow))
        self.assertNotIn("programmed_circuit", str(overflow))
        self.assertNotIn("compact_electric_conveyor", str(overflow))
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
            / "slicer.json"
        )
        self.assertEqual("immediate", policy["policy_type"])
        self.assertEqual(33, policy["relation_count"])
        self.assertEqual("cruciblecraft:slicer", policy["target_map"])
        self.assertNotIn("player_complete", str(policy))

    def test_d0_lv_and_ev_hosts_are_exact(self) -> None:
        document = census.load_json(WAVE / "d0_obtain_matrix.json")
        self.assertEqual(["PRw", "YMC"], document["grid"])
        statuses = {row["host"]: row["status"] for row in document["hosts"]}
        self.assertEqual(
            {
                20381: "source_exact",
                20382: "source_exact",
                20383: "source_exact",
                20384: "source_exact",
                20385: "source_exact",
            },
            statuses,
        )
        lv = next(row for row in document["hosts"] if row["host"] == 20381)
        ev = next(row for row in document["hosts"] if row["host"] == 20384)
        self.assertEqual("ok", lv["piston"]["status"])
        self.assertEqual("ok", lv["conveyor"]["status"])
        self.assertEqual(
            "cruciblecraft:compact_electric_conveyor_lv",
            lv["conveyor"]["cc"],
        )
        self.assertEqual("ok", ev["piston"]["status"])
        self.assertEqual("ok", ev["conveyor"]["status"])
        self.assertNotIn("programmed_circuit", str(document))
        from tools.technological_parts_foundation import has_split_module_standin
        self.assertFalse(has_split_module_standin(str(document)))

    def test_prep_import_cannot_write_live_tree(self) -> None:
        with self.assertRaisesRegex(ValueError, "src/recipe_generated"):
            recipe_wave("prep/slicer")
        self.assertNotIn("prep/slicer", SEMANTIC_COMPILE_ORDER)
        self.assertNotIn("prep/slicer", WAVE_CHOICES)
        self.assertNotIn("prep/slicer", KNOWN_SEMANTIC_SLUGS)

    def test_capability_is_runtime_ready(self) -> None:
        capability = census.load_json(CAPABILITY)
        self.assertEqual(CAPABILITY_SLUG, capability["slug"])
        self.assertEqual("runtime_ready", capability["maturity"])
        self.assertIn(capability["workflow"], {"active", "accepted"})
        self.assertEqual(["capability-runtime"], capability["profiles"])
        self.assertNotIn("player_signoff", capability)
        self.assertNotIn("required_test_ids", capability)
        compiled = ledger.compile_ledger()
        self.assertNotIn(CAPABILITY_SLUG, compiled["declared_player_complete"])
        blocked = {
            row["semantic_key"]
            for row in capability["identity_disposition"]
            if row["disposition"] == "blocked"
        }
        self.assertEqual(set(), blocked)
        folded = next(
            row
            for row in capability["identity_disposition"]
            if row["semantic_key"] == "recipe:slicer:paper_tiny_plate"
        )
        self.assertEqual("reuse_canonical", folded["disposition"])
        self.assertEqual(
            ["cruciblecraft:paper/tiny_plate"],
            folded["runtime_ids"],
        )
        owned = next(
            row
            for row in capability["identity_disposition"]
            if row["disposition"] == "new_distinct"
        )
        self.assertEqual(5, len(owned["runtime_ids"]))

    def test_art_is_copied_and_not_aliased(self) -> None:
        manifest = census.load_json(ASSETS / "gt6_slicer_art_manifest.json")
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
            self.assertNotIn("conveyor_cover", row["destination"])
        self.assertTrue(
            (ASSETS / "textures" / "gui" / "machines" / "slicer.png").is_file()
        )

    def test_unique_active_docs_and_ledger_hand_off(self) -> None:
        ledger_doc = census.load_json(ROOT / "tools" / "blocked_recipe_ledger.json")
        self.assertIsNone(ledger_doc["unique_active_wave"])
        capability = census.load_json(CAPABILITY)
        topology = census.load_json(WAVE / "topology.json")
        readiness = census.load_json(WAVE / "readiness.json")
        compiled = ledger.compile_ledger()
        if capability["workflow"] == "active":
            self.assertTrue(PLAN_ACTIVE.is_file())
            self.assertFalse(PLAN_CLOSED.is_file())
            self.assertEqual(CAPABILITY_SLUG, compiled["unique_active_slug"])
            self.assertEqual(CAPABILITY_SLUG, topology["unique_active_wave"])
            self.assertEqual(CAPABILITY_SLUG, readiness["unique_active_wave"])
        else:
            self.assertTrue(PLAN_CLOSED.is_file())
            self.assertFalse(PLAN_ACTIVE.is_file())
            self.assertNotEqual(CAPABILITY_SLUG, compiled["unique_active_slug"])
            self.assertIsNone(topology["unique_active_wave"])
            self.assertIsNone(readiness["unique_active_wave"])


if __name__ == "__main__":
    unittest.main()
