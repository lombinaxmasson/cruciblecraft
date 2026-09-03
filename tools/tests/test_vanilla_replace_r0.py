#!/usr/bin/env python3
"""Vanilla replace R0 registration and frozen artifacts."""
from __future__ import annotations

import unittest

from tools import portfolio_vanilla_replace as vanilla
from tools import t35_common as t35
from tools.recipe_bulk.slugs import KNOWN_SEMANTIC_SLUGS
from tools.wave_closeout import spec_for

SLUG = "portfolio/vanilla-replace-r0"
PINNED = ("Vanilla", "Replace")
VANILLA_PATH = "src/main/java/gregtech/loaders/c/Loader_Recipes_Vanilla.java"
REPLACE_PATH = "src/main/java/gregtech/loaders/c/Loader_Recipes_Replace.java"
ASM_PATH = "src/main/java/gregtech/asm/transformers/minecraft/Replacements.java"


class VanillaReplaceR0RegistrationTest(unittest.TestCase):
    def test_slug_is_terminal(self) -> None:
        self.assertIn(SLUG, KNOWN_SEMANTIC_SLUGS)
        spec = spec_for(SLUG)
        self.assertIsNone(spec.unique_active_wave)
        self.assertTrue(spec.next_unassigned)
        self.assertEqual(0, spec.owns_families)
        self.assertIsNone(spec.production_lock)


class VanillaReplaceR0ArtifactsTest(unittest.TestCase):
    def test_artifacts_when_present(self) -> None:
        root = t35.TOOLS / "waves" / "portfolio" / "vanilla-replace-r0"
        if not (root / "readiness.json").is_file():
            self.skipTest("R0 artifacts not written yet")
        readiness = t35.load_json(root / "readiness.json")
        self.assertEqual("VANILLA_REPLACE_R0_READY", readiness["status"])
        self.assertIsNone(readiness["unique_active_wave"])
        self.assertTrue(readiness["next_unassigned"])
        evidence = readiness["evidence"]
        self.assertEqual(0, int(evidence["completion_delta"]))
        self.assertEqual(0, int(evidence["owns_families"]))
        self.assertEqual(0, int(evidence["generated_recipe_count"]))
        self.assertFalse(evidence["recipe_files_generated"])
        self.assertEqual(39, int(evidence["leftover_later_count"]))
        self.assertEqual(2, int(evidence["inherited_category_count"]))
        self.assertEqual(3, int(evidence["inherited_source_file_count"]))
        self.assertEqual(0, int(evidence["minecraft_recipe_override_count"]))
        self.assertEqual(121, int(evidence["t30_hopper_vanilla_count"]))
        self.assertEqual(24, int(evidence["worldgen_ores_vanilla_count"]))
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
        self.assertEqual(2, int(inherited["category_count"]))
        self.assertEqual(3, int(inherited["source_file_count"]))
        vanilla_row = inherited["categories"][0]
        self.assertEqual(
            "gregtech.loaders.c.Loader_Recipes_Vanilla",
            vanilla_row["gt6_anchor"],
        )
        self.assertEqual("none", vanilla_row["capability_row"]["cc_mechanism"])
        self.assertEqual(
            [VANILLA_PATH],
            [row["path"] for row in vanilla_row["source_files"]],
        )
        self.assertEqual(
            "4c459acd2c7729d4186c5ada9ccd76181745bacf",
            vanilla_row["source_files"][0]["git_blob_sha1"],
        )
        self.assertEqual(92644, int(vanilla_row["source_files"][0]["size"]))
        replace_row = inherited["categories"][1]
        self.assertEqual(
            [
                REPLACE_PATH,
                ASM_PATH,
            ],
            [row["path"] for row in replace_row["source_files"]],
        )
        self.assertEqual(
            ["Loader_Recipes_Replace", "RecipeReplacement", "RecipeReplacer"],
            replace_row["source_files"][0]["nested_classes"],
        )
        self.assertEqual(
            "584a2030723c11e984b5b7437ed17f7a9bc93e5d",
            replace_row["source_files"][1]["git_blob_sha1"],
        )
        closed = inherited["already_closed_elsewhere"]
        self.assertFalse(closed["t30_hopper_vanilla"]["owner_here"])
        self.assertEqual(121, int(closed["t30_hopper_vanilla"]["recipe_count"]))
        self.assertFalse(closed["empty_dump_maps"]["is_census"])
        self.assertEqual(
            0,
            int(closed["empty_dump_maps"]["recipe_counts"]["mc.recipe.furnace"]),
        )
        self.assertEqual(
            24,
            int(closed["worldgen_ores_vanilla"]["feature_count"]),
        )
        feasibility = t35.load_json(root / "feasibility.json")
        by_category = {row["category"]: row for row in feasibility["categories"]}
        self.assertEqual(list(PINNED), list(by_category))
        for name in PINNED:
            self.assertEqual("requires_new_runtime", by_category[name]["verdict"])
            self.assertFalse(by_category[name]["allows_implementation_child"])
            self.assertIn(by_category[name]["verdict"], vanilla.FEASIBILITY_VALUES)
            self.assertIsNone(by_category[name]["destination"])
        topology = t35.load_json(root / "topology.json")
        self.assertIsNone(topology["unique_active_wave"])
        self.assertTrue(topology["next_unassigned"])
        self.assertEqual(set(vanilla.ALLOWED_TOPOLOGY_KEYS), set(topology))
        census = t35.load_json(root / "census_delta.json")
        self.assertEqual(39, int(census["leftover_later_count"]))
        wave = t35.load_json(root / "wave.json")
        self.assertEqual(SLUG, wave["program"])
        self.assertEqual(["portfolio/non-ore-worldgen-r0"], wave["depends_on"])
        contract = t35.load_json(root / "replace_contract.json")
        self.assertFalse(contract["implemented"])
        self.assertIn("load", contract["questions"])
        self.assertIn("separately", contract["questions"]["load"])
        self.assertIn("add_vs_remove", contract["questions"])
        self.assertIn("asm_vs_datapack", contract["questions"])
        mechanism = t35.load_json(root / "existing_mechanism.json")
        self.assertEqual(121, int(mechanism["t30_hopper_vanilla_count"]))
        self.assertEqual(0, int(mechanism["minecraft_recipe_override_count"]))
        self.assertFalse(mechanism["generic_importer_removes_minecraft_recipes"])
        self.assertEqual("GENERIC_RECIPE_IMPORT_READY", mechanism["generic_importer_status"])
        self.assertEqual(["ItemStackMixin"], mechanism["mixins"])
        self.assertEqual(2, len(mechanism["capability_map_vanilla_replace"]))
        self.assertTrue(all(row["cc_mechanism"] == "none" for row in mechanism["kinds"]))
        self.assertTrue(all(row["gap"] == "mechanism" for row in mechanism["kinds"]))
        self.assertFalse(mechanism["nuclear_started"])
        self.assertTrue(
            any("mold_firing" in path or "crucible_firing" in path
                for path in mechanism["authored_minecraft_smelting"])
        )
        semantics = t35.load_json(root / "source_semantics.json")
        names = [row["category"] for row in semantics["categories"]]
        self.assertEqual(list(PINNED), names)
        vanilla_sem = semantics["categories"][0]
        self.assertTrue(vanilla_sem["contrast"]["adds_vanilla_adjacent_recipes"])
        self.assertTrue(vanilla_sem["contrast"]["empty_dump_maps_are_not_census"])
        self.assertEqual(0, int(vanilla_sem["contrast"]["furnace_dump_recipe_count"]))
        replace_sem = semantics["categories"][1]
        self.assertTrue(replace_sem["contrast"]["removes_or_substitutes"])
        self.assertTrue(replace_sem["contrast"]["asm_replacements_is_bytecode"])
        self.assertEqual(
            ["RecipeReplacement", "RecipeReplacer"],
            replace_sem["contrast"]["nested_helpers_same_file"],
        )


if __name__ == "__main__":
    unittest.main()
