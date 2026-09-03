#!/usr/bin/env python3
"""Crops / food / bees R0 registration and frozen artifacts."""
from __future__ import annotations

import unittest

from tools import portfolio_crops_food_bees as crops
from tools import t35_common as t35
from tools.recipe_bulk.slugs import KNOWN_SEMANTIC_SLUGS
from tools.wave_closeout import spec_for

SLUG = "portfolio/crops-food-bees-r0"
PINNED = ("Crops", "Food", "Bees")
PLANT_NAMES = ("plant.glowtus", "plant.bush")
HIVE_NAMES = (
    "overworld.bumblehives",
    "nether.bumblehives",
    "aether.bumblehives",
    "end.bumblehives",
    "twilight.bumblehives",
    "erebus.bumblehives",
    "betweenlands.bumblehives",
    "atum.bumblehives",
    "alfheim.bumblehives",
    "tropics.bumblehives",
)


class CropsFoodBeesR0RegistrationTest(unittest.TestCase):
    def test_slug_is_terminal(self) -> None:
        self.assertIn(SLUG, KNOWN_SEMANTIC_SLUGS)
        spec = spec_for(SLUG)
        self.assertIsNone(spec.unique_active_wave)
        self.assertTrue(spec.next_unassigned)
        self.assertEqual(0, spec.owns_families)
        self.assertIsNone(spec.production_lock)


class CropsFoodBeesR0ArtifactsTest(unittest.TestCase):
    def test_artifacts_when_present(self) -> None:
        root = t35.TOOLS / "waves" / "portfolio" / "crops-food-bees-r0"
        if not (root / "readiness.json").is_file():
            self.skipTest("R0 artifacts not written yet")
        readiness = t35.load_json(root / "readiness.json")
        self.assertEqual("CROPS_FOOD_BEES_R0_READY", readiness["status"])
        self.assertIsNone(readiness["unique_active_wave"])
        self.assertTrue(readiness["next_unassigned"])
        evidence = readiness["evidence"]
        self.assertEqual(0, int(evidence["completion_delta"]))
        self.assertEqual(0, int(evidence["owns_families"]))
        self.assertEqual(0, int(evidence["generated_recipe_count"]))
        self.assertFalse(evidence["recipe_files_generated"])
        self.assertEqual(39, int(evidence["leftover_later_count"]))
        self.assertEqual(3, int(evidence["inherited_category_count"]))
        self.assertEqual(12, int(evidence["inherited_feature_count"]))
        self.assertEqual(190, int(evidence["full_other_features"]))
        self.assertEqual(13373, int(evidence["dump_recipe_count_context"]))
        self.assertEqual(160, int(evidence["remainder_after_this_slice"]))
        self.assertFalse(evidence["plantalyzer_used_as_census"])
        self.assertFalse(evidence["nuclear_track_c_started"])
        self.assertFalse(evidence["allows_implementation_child"])
        for name in PINNED:
            self.assertEqual(
                "requires_new_runtime",
                evidence["feasibility_by_category"][name],
            )
        inherited = t35.load_json(root / "inherited_denominator.json")
        self.assertEqual(
            list(PINNED),
            [row["category"] for row in inherited["categories"]],
        )
        self.assertEqual(3, int(inherited["category_count"]))
        self.assertEqual(12, int(inherited["feature_count"]))
        self.assertEqual(190, int(inherited["full_other_features"]))
        self.assertEqual(13373, int(inherited["dump_recipe_count_context"]))
        self.assertEqual(
            160,
            int(inherited["remainder_after_this_slice"]["feature_count"]),
        )
        self.assertEqual(172, int(inherited["remainder_after_non_ore"]))
        self.assertEqual(18, int(inherited["non_ore_owned_feature_count"]))
        crops_row = inherited["categories"][0]
        self.assertEqual(
            "GT6 crops / plants (dump worldgen plant.* + gt.recipe.squeezer)",
            crops_row["gt6_anchor"],
        )
        self.assertEqual("none", crops_row["capability_row"]["cc_mechanism"])
        self.assertEqual(list(PLANT_NAMES), crops_row["names"])
        self.assertEqual(
            ["WorldgenGlowtus", "WorldgenBushes"],
            crops_row["types"],
        )
        self.assertEqual(
            ["gt.recipe.squeezer"],
            [row["nameInternal"] for row in crops_row["dump_maps"]],
        )
        self.assertEqual(5322, int(crops_row["dump_maps"][0]["recipeCount"]))
        self.assertFalse(crops_row["dump_maps"][0]["is_census"])
        food_row = inherited["categories"][1]
        self.assertEqual([], food_row["names"])
        self.assertEqual(0, int(food_row["feature_count"]))
        self.assertEqual(
            ["gt.recipe.juicer", "gt.recipe.fermenter"],
            [row["nameInternal"] for row in food_row["dump_maps"]],
        )
        self.assertEqual(96, int(food_row["dump_maps"][0]["recipeCount"]))
        self.assertEqual(6435, int(food_row["dump_maps"][1]["recipeCount"]))
        bees_row = inherited["categories"][2]
        self.assertEqual(list(HIVE_NAMES), bees_row["names"])
        self.assertEqual(["WorldgenHives"] * 10, bees_row["types"])
        self.assertEqual(
            ["gt.recipe.bumblequeen", "gt.recipe.bumblelyzer"],
            [row["nameInternal"] for row in bees_row["dump_maps"]],
        )
        self.assertEqual(80, int(bees_row["dump_maps"][0]["recipeCount"]))
        self.assertEqual(1440, int(bees_row["dump_maps"][1]["recipeCount"]))
        self.assertEqual(0, int(inherited["plantalyzer"]["recipeCount"]))
        self.assertFalse(inherited["plantalyzer"]["is_census"])
        histogram = inherited["remainder_after_this_slice"]["type_histogram"]
        self.assertNotIn("WorldgenHives", histogram)
        self.assertNotIn("WorldgenGlowtus", histogram)
        self.assertNotIn("WorldgenBushes", histogram)
        self.assertEqual(90, int(histogram["WorldgenStone"]))
        self.assertEqual(24, int(histogram["WorldgenOresVanilla"]))
        closed = inherited["already_closed_elsewhere"]
        self.assertFalse(closed["non_ore_worldgen_r0"]["owner_here"])
        self.assertEqual(18, int(closed["non_ore_worldgen_r0"]["feature_count"]))
        self.assertEqual(
            172,
            int(closed["non_ore_worldgen_r0"]["remainder_after_slice"]),
        )
        self.assertTrue(closed["count_ceiling_kind_envelope"]["telemetry_report_only"])
        feasibility = t35.load_json(root / "feasibility.json")
        by_category = {row["category"]: row for row in feasibility["categories"]}
        self.assertEqual(list(PINNED), list(by_category))
        for name in PINNED:
            self.assertEqual("requires_new_runtime", by_category[name]["verdict"])
            self.assertFalse(by_category[name]["allows_implementation_child"])
            self.assertIn(by_category[name]["verdict"], crops.FEASIBILITY_VALUES)
            self.assertIsNone(by_category[name]["destination"])
        topology = t35.load_json(root / "topology.json")
        self.assertIsNone(topology["unique_active_wave"])
        self.assertTrue(topology["next_unassigned"])
        self.assertEqual(set(crops.ALLOWED_TOPOLOGY_KEYS), set(topology))
        census = t35.load_json(root / "census_delta.json")
        self.assertEqual(39, int(census["leftover_later_count"]))
        wave = t35.load_json(root / "wave.json")
        self.assertEqual(SLUG, wave["program"])
        self.assertEqual(["portfolio/vanilla-replace-r0"], wave["depends_on"])
        contract = t35.load_json(root / "crops_contract.json")
        self.assertFalse(contract["implemented"])
        for key in (
            "disposition_policy",
            "new_recipe_maps",
            "crop_runtime",
            "hive_worldgen",
            "player_acquisition",
            "scale",
            "fail_closed",
            "delete_later",
        ):
            self.assertIn(key, contract["questions"])
        self.assertIn("13,373", contract["questions"]["scale"])
        mechanism = t35.load_json(root / "existing_mechanism.json")
        self.assertEqual("GENERIC_RECIPE_IMPORT_READY", mechanism["generic_importer_status"])
        self.assertFalse(mechanism["generic_importer_creates_recipe_maps"])
        self.assertEqual(3, len(mechanism["capability_map_crops_food_bees"]))
        self.assertTrue(all(row["cc_mechanism"] == "none" for row in mechanism["kinds"]))
        self.assertTrue(all(row["gap"] == "mechanism" for row in mechanism["kinds"]))
        self.assertFalse(mechanism["nuclear_started"])
        self.assertEqual([], mechanism["worldgen_catalog_slice_hits"])
        self.assertIn(
            "MultiTileEntitySqueezer",
            mechanism["t35_excluded_mtes"][
                "multiblock/multi_tile_entity_squeezer"
            ]["behavior_class"],
        )
        semantics = t35.load_json(root / "source_semantics.json")
        names = [row["category"] for row in semantics["categories"]]
        self.assertEqual(list(PINNED), names)
        crops_sem = semantics["categories"][0]
        self.assertTrue(crops_sem["contrast"]["not_wood_recipes"])
        self.assertTrue(crops_sem["contrast"]["plantalyzer_is_not_census"])
        self.assertEqual("gt.recipe.squeezer", crops_sem["contrast"]["squeezer_map"])
        food_sem = semantics["categories"][1]
        self.assertTrue(food_sem["contrast"]["not_smelter"])
        self.assertFalse(food_sem["contrast"]["worldgen_in_this_card"])
        bees_sem = semantics["categories"][2]
        self.assertEqual(list(HIVE_NAMES), bees_sem["contrast"]["hive_worldgen"])
        self.assertEqual(0, int(semantics["plantalyzer_recipe_count"]))
        self.assertFalse(semantics["plantalyzer_used_as_census"])


if __name__ == "__main__":
    unittest.main()
