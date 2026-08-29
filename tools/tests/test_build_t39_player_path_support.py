#!/usr/bin/env python3
"""Regression coverage for T39 GT6 player-path support routes."""
from __future__ import annotations

import json
import sys
import unittest
from pathlib import Path

TOOLS = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(TOOLS))

import build_t39_player_path_support as support

ROOT = support.ROOT
GT6_REVISION = support.GT6_REVISION


class T39PlayerPathSupportTests(unittest.TestCase):
    def test_routes_are_dump_pinned_without_tag_inputs(self) -> None:
        document, files = support.build()
        self.assertGreater(len(document["routes"]), 0)
        for route in document["routes"]:
            self.assertIn(
                route["source_map"],
                {"gt.recipe.mixer", "gt.recipe.smelter", "gt.recipe.unboxinator"},
            )
            self.assertIsInstance(route["source_recipe"], int)
            recipe_path = ROOT / route["recipe_path"]
            self.assertIn(recipe_path, files)
            recipe = json.loads(files[recipe_path])
            if recipe.get("type") == "cruciblecraft:gt_recipe":
                provenance = recipe["provenance"]
                self.assertEqual([GT6_REVISION], provenance["evidence_hashes"])
                self.assertEqual(support.t39.OWNER, provenance["claim_owner"])
                self.assertTrue(provenance.get("source_fingerprint"))
                self.assertIn("gt6_dump/gt6_recipe_dump/maps/", provenance["selected_source_recipe"])
                self.assertIn("#recipes[", provenance["selected_source_recipe"])
                for ingredient in recipe.get("item_inputs") or []:
                    self.assertIn("item", ingredient)
                    self.assertNotIn("tag", ingredient)
            elif recipe.get("type") == "minecraft:crafting_shapeless":
                for ingredient in recipe.get("ingredients") or []:
                    self.assertIn("item", ingredient)
                    self.assertNotIn("tag", ingredient)
            else:
                self.fail(f"unexpected recipe type: {recipe.get('type')}")
        sources = [
            (route["source_map"], route["source_recipe"])
            for route in document["routes"]
        ]
        self.assertEqual(len(sources), len(set(sources)))

    def test_manifest_lists_remaining_unmapped(self) -> None:
        document, _files = support.build()
        remaining = document["discovery"]["remaining_unmapped"]
        self.assertIsInstance(remaining, list)
        for identity in remaining:
            self.assertTrue(
                identity.startswith("item:") or identity.startswith("fluid:")
            )

    def test_smelter_glass_uses_registered_molten_fluid(self) -> None:
        crosswalk = support.Crosswalk(support._load(support.CROSS))
        mapped = crosswalk.map_fluid_stack({"fluid": "glass", "amount": 144})
        if mapped is None:
            mapped = crosswalk.map_fluid_stack({"fluid": "molten.glass", "amount": 144})
        self.assertEqual("fluid:cruciblecraft:molten_glass", mapped)
        self.assertIsNone(crosswalk.map_fluid_stack({"fluid": "blaze", "amount": 144}))

    def test_one_dump_recipe_emits_one_support_file(self) -> None:
        document, files = support.build()
        sources = [
            (route["source_map"], route["source_recipe"])
            for route in document["routes"]
        ]
        self.assertEqual(len(sources), len(set(sources)))
        gt_docs = [
            json.loads(content)
            for path, content in files.items()
            if json.loads(content).get("type") == "cruciblecraft:gt_recipe"
        ]
        signatures = []
        for recipe in gt_docs:
            items = tuple(
                (row.get("item"), count)
                for row, count in zip(
                    recipe.get("item_inputs") or [],
                    recipe.get("item_input_counts") or [],
                )
            )
            fluids = tuple(
                (row.get("id"), row.get("amount"))
                for row in recipe.get("fluid_inputs") or []
            )
            signatures.append((recipe.get("map"), items, fluids))
        self.assertEqual(len(signatures), len(set(signatures)))

    def test_zero_count_stack_still_maps_as_catalyst(self) -> None:
        crosswalk = support.Crosswalk(support._load(support.CROSS))
        mapped = crosswalk.map_item_stack({"item": "minecraft:bucket", "count": 0})
        self.assertEqual("item:minecraft:bucket", mapped)

    def test_needed_identities_skip_unproven_alias_families(self) -> None:
        reachable = {"item:minecraft:sand"}
        player_path = {
            "rows": [
                {
                    "alias_fail_closed": True,
                    "consume_ids": ["minecraft:suspicious_sand"],
                    "fluid_input_ids": [],
                    "template_key": "hosted_oil_sand",
                },
                {
                    "alias_fail_closed": False,
                    "consume_ids": ["cruciblecraft:cobalt/tiny_dust"],
                    "fluid_input_ids": [],
                    "template_key": "cobalt_unpack",
                },
            ]
        }
        needed_items, needed_fluids = support._needed_identities(player_path, reachable)
        self.assertNotIn("item:minecraft:suspicious_sand", needed_items)
        self.assertIn("item:cruciblecraft:cobalt/tiny_dust", needed_items)
        self.assertEqual(set(), needed_fluids)

    def test_zero_count_input_is_preserve_not_dropped(self) -> None:
        document = support._gt_recipe_document(
            source_map="gt.recipe.mixer",
            source_index=0,
            cc_map="cruciblecraft:mixer",
            recipe={"duration": 16, "euPerTick": 16, "canBeBuffered": True},
            item_inputs=[{"item": "cruciblecraft:iron/dust"}, {"item": "minecraft:bucket"}],
            item_input_counts=[1, 0],
            item_input_actions=[{"kind": "consume"}, {"kind": "preserve"}],
            item_outputs=[{"count": 1, "id": "cruciblecraft:iron/dust"}],
        )
        self.assertEqual([1, 0], document["item_input_counts"])
        self.assertEqual(
            [{"kind": "consume"}, {"kind": "preserve"}],
            document["item_input_actions"],
        )
        self.assertEqual(support.t39.OWNER, document["provenance"]["claim_owner"])
        self.assertTrue(document["provenance"]["source_fingerprint"])

    def test_no_t37_t38_recovery_copy(self) -> None:
        document, files = support.build()
        t38_root = ROOT / "src/main/resources/data/cruciblecraft/recipe/t38_player_path_recovery"
        t38_names = {path.name for path in t38_root.glob("*.json")} if t38_root.is_dir() else set()
        for path in files:
            self.assertNotIn(path.name, t38_names)
        for route in document["routes"]:
            self.assertNotIn(route["recipe_path"], [
                f"src/main/resources/data/cruciblecraft/recipe/t38_player_path_recovery/{name}"
                for name in t38_names
            ])


if __name__ == "__main__":
    raise SystemExit(unittest.main())
