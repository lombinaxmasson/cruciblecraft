#!/usr/bin/env python3
"""Non-ore worldgen R0 registration and frozen artifacts."""
from __future__ import annotations

import unittest

from tools import portfolio_non_ore_worldgen as worldgen
from tools import census_common as census
from tools.recipe_bulk.slugs import KNOWN_SEMANTIC_SLUGS
from tools.wave_closeout import spec_for

SLUG = "portfolio/non-ore-worldgen-r0"
PINNED = ("Trees", "Dungeons", "Planets", "Center")
TREE_NAMES = (
    "tree.rubber",
    "tree.maple",
    "tree.willow",
    "tree.bluemahoe",
    "tree.hazel",
    "tree.cinnamon",
    "tree.coconut",
    "tree.rainbowood",
    "tree.bluespruce",
)


class NonOreWorldgenR0RegistrationTest(unittest.TestCase):
    def test_slug_is_terminal(self) -> None:
        self.assertIn(SLUG, KNOWN_SEMANTIC_SLUGS)
        spec = spec_for(SLUG)
        self.assertIsNone(spec.unique_active_wave)
        self.assertTrue(spec.next_unassigned)
        self.assertEqual(0, spec.owns_families)
        self.assertIsNone(spec.production_lock)


class NonOreWorldgenR0ArtifactsTest(unittest.TestCase):
    def test_artifacts_when_present(self) -> None:
        root = census.TOOLS / "waves" / "portfolio" / "non-ore-worldgen-r0"
        if not (root / "readiness.json").is_file():
            self.skipTest("R0 artifacts not written yet")
        readiness = census.load_json(root / "readiness.json")
        self.assertEqual("NON_ORE_WORLDGEN_R0_READY", readiness["status"])
        self.assertIsNone(readiness["unique_active_wave"])
        self.assertTrue(readiness["next_unassigned"])
        evidence = readiness["evidence"]
        self.assertEqual(0, int(evidence["completion_delta"]))
        self.assertEqual(0, int(evidence["owns_families"]))
        self.assertEqual(0, int(evidence["generated_recipe_count"]))
        self.assertFalse(evidence["recipe_files_generated"])
        self.assertEqual(39, int(evidence["leftover_later_count"]))
        self.assertEqual(18, int(evidence["inherited_feature_count"]))
        self.assertEqual(190, int(evidence["full_other_features"]))
        self.assertEqual(172, int(evidence["remainder_after_slice"]))
        self.assertFalse(evidence["nuclear_track_c_started"])
        self.assertFalse(evidence["allows_implementation_child"])
        for name in PINNED:
            self.assertEqual(
                "requires_new_runtime",
                evidence["feasibility_by_category"][name],
            )
        inherited = census.load_json(root / "inherited_denominator.json")
        self.assertEqual(
            list(PINNED),
            [row["category"] for row in inherited["categories"]],
        )
        self.assertEqual(18, int(inherited["feature_count"]))
        self.assertEqual(190, int(inherited["full_other_features"]))
        self.assertEqual(172, int(inherited["remainder_after_slice"]["feature_count"]))
        trees = inherited["categories"][0]
        self.assertEqual(9, int(trees["feature_count"]))
        self.assertEqual(list(TREE_NAMES), trees["names"])
        self.assertNotIn("center.streets", trees["names"])
        self.assertNotIn("WorldgenStreets", trees["types"])
        self.assertEqual(
            ["overworld.structure.dungeon.large"],
            inherited["categories"][1]["names"],
        )
        self.assertEqual(
            ["moon.rocks", "mars.rocks", "planet.rocks"],
            inherited["categories"][2]["names"],
        )
        self.assertEqual(
            [
                "center.biomes",
                "center.streets",
                "center.nexus",
                "center.beacon",
                "center.testing",
            ],
            inherited["categories"][3]["names"],
        )
        slice_names = {row["name"] for row in inherited["features"]}
        self.assertNotIn("aether.rocks", slice_names)
        self.assertNotIn("erebus.rocks", slice_names)
        self.assertNotIn("alfheim.rocks", slice_names)
        self.assertNotIn("center.streets", inherited["categories"][0]["names"])
        histogram = inherited["remainder_after_slice"]["type_histogram"]
        self.assertEqual(90, int(histogram["WorldgenStone"]))
        self.assertEqual(16, int(histogram["WorldgenFluidSpring"]))
        self.assertEqual(10, int(histogram["WorldgenHives"]))
        feasibility = census.load_json(root / "feasibility.json")
        by_category = {row["category"]: row for row in feasibility["categories"]}
        self.assertEqual(list(PINNED), list(by_category))
        for name in PINNED:
            self.assertEqual("requires_new_runtime", by_category[name]["verdict"])
            self.assertFalse(by_category[name]["allows_implementation_child"])
            self.assertIn(by_category[name]["verdict"], worldgen.FEASIBILITY_VALUES)
            self.assertIsNone(by_category[name]["destination"])
        topology = census.load_json(root / "topology.json")
        self.assertIsNone(topology["unique_active_wave"])
        self.assertTrue(topology["next_unassigned"])
        self.assertEqual(set(worldgen.ALLOWED_TOPOLOGY_KEYS), set(topology))
        census = census.load_json(root / "census_delta.json")
        self.assertEqual(39, int(census["leftover_later_count"]))
        wave = census.load_json(root / "wave.json")
        self.assertEqual(SLUG, wave["program"])
        self.assertEqual(["portfolio/exclusion-reclaim-r0"], wave["depends_on"])
        contract = census.load_json(root / "worldgen_contract.json")
        self.assertFalse(contract["implemented"])
        self.assertIn("load", contract["questions"])
        self.assertIn("tree_identities", contract["questions"])
        self.assertIn("planet_semantics", contract["questions"])
        mechanism = census.load_json(root / "existing_mechanism.json")
        self.assertEqual(129, int(mechanism["ore_vein_count"]))
        self.assertEqual(2, int(mechanism["fluid_deposit_count"]))
        self.assertEqual("surface_rock_scatter", mechanism["surface_scatter_id"])
        self.assertEqual(4, len(mechanism["capability_map_non_ore"]))
        self.assertTrue(all(row["cc_mechanism"] == "none" for row in mechanism["kinds"]))
        self.assertFalse(mechanism["nuclear_started"])
        semantics = census.load_json(root / "source_semantics.json")
        names = [row["category"] for row in semantics["categories"]]
        self.assertEqual(list(PINNED), names)
        trees_sem = semantics["categories"][0]
        self.assertTrue(trees_sem["contrast"]["streets_are_center_not_trees"])
        self.assertTrue(trees_sem["contrast"]["nine_tree_features_are_not_wood_recipes"])
        self.assertTrue(trees_sem["contrast"]["placement_not_identity"])
        dungeons = semantics["categories"][1]
        self.assertTrue(dungeons["contrast"]["is_structure"])
        self.assertTrue(dungeons["contrast"]["not_scatter"])
        planets = semantics["categories"][2]
        self.assertFalse(planets["contrast"]["in_overworld_catalog"])
        self.assertIn(
            "DESIGN_POLICY_OVERWORLD_ACCESS",
            planets["contrast"]["overworld_access_policy"],
        )
        center = semantics["categories"][3]
        self.assertTrue(center["contrast"]["enabled_false_is_not_skip"])
        self.assertEqual(
            [False, False, False, False, False],
            center["contrast"]["dump_enabled"],
        )


if __name__ == "__main__":
    unittest.main()
