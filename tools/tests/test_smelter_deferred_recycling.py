#!/usr/bin/env python3
"""Smelter deferred-recycling: 1817 compact exact MTE recovery families."""
from __future__ import annotations

import unittest

from tools import census_common as census
from tools.recipe_bulk.slugs import KNOWN_SEMANTIC_SLUGS
from tools.recipe_bulk.waves import recipe_wave
from tools.wave_closeout import spec_for

SLUG = "smelter/deferred-recycling"
ROOT = census.TOOLS / "waves" / "smelter" / "deferred-recycling"
GENERATED = (
    census.ROOT
    / "src"
    / "recipe_generated"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "recipe"
    / "smelter"
    / "deferred_recycling"
)
EDGE = ("gt.recipe.smelter#1829", "gt.recipe.smelter#1884")


class SmelterDeferredRecyclingTest(unittest.TestCase):
    def test_slug_and_spec_are_registered(self) -> None:
        self.assertIn(SLUG, KNOWN_SEMANTIC_SLUGS)
        spec = spec_for(SLUG)
        self.assertEqual("smelter/deferred-recycling-edge", spec.unique_active_wave)
        self.assertFalse(spec.next_unassigned)
        self.assertEqual(1817, spec.owns_families)
        wave = recipe_wave(SLUG)
        self.assertEqual("exact", wave.representation)
        self.assertEqual("smelter/deferred_recycling", wave.path_prefix)

    def test_lock_is_1817_exact_singletons(self) -> None:
        if not (ROOT / "production_lock.json").is_file():
            self.skipTest("production lock not written yet")
        lock = census.load_json(ROOT / "production_lock.json")
        production = lock["production"]
        self.assertEqual(1817, int(production["family_count"]))
        self.assertEqual(1817, int(production["exact_families"]))
        self.assertEqual(0, int(production["exact_multi_families"]))
        self.assertEqual(1817, int(production["relation_count"]))
        templates = [str(row["template_key"]) for row in production["families"]]
        self.assertEqual(1817, len(set(templates)))
        for key in EDGE:
            self.assertNotIn(key, templates)
        groups = {str(row["publication_group"]) for row in production["families"]}
        self.assertEqual(81, len(groups))
        for group in groups:
            self.assertTrue(
                group.startswith("cruciblecraft:smelter/deferred_recycling/")
            )
            self.assertNotIn("ordinary_closure", group)

    def test_generated_tree_is_1817_compact_exact(self) -> None:
        if not GENERATED.is_dir():
            self.skipTest("generated tree not written yet")
        files = list(GENERATED.rglob("gt_recipe_*.json"))
        self.assertEqual(1817, len(files))
        for path in files[:12]:
            document = census.load_json(path)
            self.assertEqual(
                "cruciblecraft:compact_gt_recipe_family", document["type"]
            )
            self.assertEqual("cruciblecraft:smelter", document["target_map"])
            self.assertEqual(1, len(document.get("relations") or []))
            self.assertFalse(document.get("parameterized"))
            relation = document["relations"][0]
            self.assertEqual(1, len(relation.get("item_inputs") or []))
            self.assertEqual(1, len(relation.get("fluid_outputs") or []))
            consume = str(relation["item_inputs"][0].get("item") or "")
            self.assertTrue(consume.startswith("cruciblecraft:"))
            molten = str(relation["fluid_outputs"][0].get("id") or "")
            self.assertTrue(molten.startswith("cruciblecraft:molten_"))

    def test_census_drops_deferred_recycling_by_1817(self) -> None:
        if not (ROOT / "census_delta.json").is_file():
            self.skipTest("census not written yet")
        census = census.load_json(ROOT / "census_delta.json")
        remaining = census.get("remaining_ordinary") or {}
        self.assertEqual(1817, int(census["complete_family_count"]))
        self.assertEqual(1817, int(census["completion_delta"]))
        self.assertEqual(0, int(census["remaining_recipe_gap"]))
        self.assertEqual(0, int(census["partial_family_count"]))
        self.assertEqual(26, int(remaining["deferred_recycling_count"]))
        self.assertEqual(28, int(remaining["deferred_total"]))
        r0 = census.load_json(
            census.TOOLS
            / "waves"
            / "recycling"
            / "deferred-ordinary-ledger-r0"
            / "census_delta.json"
        )
        self.assertEqual(
            1843,
            int(r0["remaining_ordinary"]["deferred_recycling_count"]),
        )


if __name__ == "__main__":
    unittest.main()
