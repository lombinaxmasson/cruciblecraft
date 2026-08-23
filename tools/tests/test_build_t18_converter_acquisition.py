from __future__ import annotations

import copy
import json
import unittest

from tools import build_t18_converter_acquisition as builder


PROFILE_IDS = {
    "cruciblecraft:bronze_firebox",
    "cruciblecraft:bronze_boiler",
    "cruciblecraft:bronze_steam_engine",
    "cruciblecraft:bronze_dynamo",
    "cruciblecraft:bronze_fuel_engine",
    "cruciblecraft:bronze_gas_generator",
}
RESULT_IDS = {
    "cruciblecraft:firebox",
    "cruciblecraft:bronze_boiler",
    "cruciblecraft:bronze_steam_engine",
    "cruciblecraft:bronze_dynamo",
    "cruciblecraft:fuel_engine",
    "cruciblecraft:burning_gas_generator",
}


class T18ConverterAcquisitionTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.document = builder.build()

    def test_independent_expected_set_closes_all_six_profiles(self):
        self.assertEqual(
            "T18D_CONVERTER_ACQUISITION_READY",
            self.document["status"],
        )
        expected = self.document["expected_set"]
        self.assertEqual(
            "independent_t18d_converter_acceptance_policy",
            expected["source"],
        )
        self.assertFalse(expected["production_mapping_reused"])
        self.assertEqual(PROFILE_IDS, set(expected["profile_ids"]))
        self.assertEqual(6, len(expected["recipe_ids"]))
        self.assertEqual(
            {
                "converter_profiles": 6,
                "registered_blocks": 6,
                "registered_items": 6,
                "vanilla_crafting_recipes": 6,
                "recipe_operand_rows": 14,
                "concrete_producer_recipes": 1,
                "producer_rules": 2,
                "recipe_tag_sources": 2,
                "material_source_declarations": 1,
                "minecraft_source_declarations": 8,
                "profile_resource_rows": 24,
                "language_rows": 12,
                "block_tag_rows": 6,
                "gt_recipe_rows_added": 0,
                "unreachable": 0,
            },
            self.document["counts"],
        )

    def test_recipe_results_operands_and_producers_are_reachable(self):
        recipes = self.document["recipes"]
        self.assertEqual(6, len(recipes))
        self.assertEqual(
            RESULT_IDS, {row["result_item"] for row in recipes}
        )
        self.assertEqual([], self.document["unreachable"])
        for recipe in recipes:
            with self.subTest(profile=recipe["profile_id"]):
                self.assertTrue(recipe["reachable"])
                self.assertTrue(recipe["ingredients"])
                self.assertTrue(all(
                    row["registered"]
                    and row["reachable"]
                    and row["producer"]
                    for row in recipe["ingredients"]
                ))
        serialized = json.dumps(recipes, sort_keys=True)
        self.assertIn("concrete_shaped_recipe", serialized)
        self.assertIn("dynamic_material_tag", serialized)
        self.assertIn("material_rule", serialized)

    def test_profile_block_item_model_lang_loot_tag_sets_are_bidirectional(self):
        closure = self.document["resource_closure"]
        self.assertEqual("BIDIRECTIONAL_CLOSURE", closure["status"])
        for component, ids in closure["component_sets"].items():
            with self.subTest(component=component):
                self.assertEqual(PROFILE_IDS, set(ids))
        for profile_id, row in closure["profiles"].items():
            with self.subTest(profile=profile_id):
                self.assertEqual(["en_us", "zh_cn"], row["languages"])
                self.assertEqual(
                    "minecraft:mineable/pickaxe", row["block_tag"]
                )
                self.assertEqual(
                    {"blockstate", "block_model", "item_model", "loot"},
                    set(row["paths"]),
                )

    def test_currentness_covers_every_recipe_producer_and_resource(self):
        currentness = self.document["currentness"]
        self.assertEqual(
            {
                "tools/build_t18_converter_acquisition.py",
                "tools/t18_converter_acquisition_policy.json",
            },
            set(currentness["owned_inputs"]),
        )
        self.assertEqual(6, len(currentness["crafting_recipes"]))
        self.assertEqual(1, len(currentness["concrete_recipes"]))
        self.assertEqual(2, len(currentness["producer_rules"]))
        self.assertGreaterEqual(len(currentness["tag_contracts"]), 3)
        self.assertEqual(28, len(currentness["resources"]))

    def test_policy_mutations_fail_closed(self):
        original = builder.load(builder.POLICY)
        mutations = (
            lambda value: value["profiles"].pop(
                "cruciblecraft:bronze_dynamo"
            ),
            lambda value: value["profiles"][
                "cruciblecraft:bronze_firebox"
            ].__setitem__("result", "cruciblecraft:bronze_boiler"),
            lambda value: value["tag_sources"].pop("c:rods/bronze"),
            lambda value: value["producer_rules"].pop("plate"),
        )
        for mutate in mutations:
            candidate = copy.deepcopy(original)
            mutate(candidate)
            with self.subTest(mutation=mutate):
                with self.assertRaises(ValueError):
                    builder.build(candidate)

    def test_artifact_matches_current_builder(self):
        before = builder.OUTPUT.read_bytes()
        self.assertEqual([], builder.check())
        self.assertEqual(before, builder.OUTPUT.read_bytes())
        self.assertEqual(
            self.document,
            json.loads(builder.OUTPUT.read_text(encoding="utf-8")),
        )


if __name__ == "__main__":
    unittest.main()
