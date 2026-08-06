from __future__ import annotations

import copy
import json
import unittest

from tools import build_t16_machine_acquisition as builder


EXPECTED_RECIPE_IDS = {
    f"cruciblecraft:machines/{prefix}{kind}"
    for kind in ("lathe", "rollingmill", "wiremill", "shredder", "press")
    for prefix in ("", "steel_", "titanium_")
}


class T16MachineAcquisitionTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.document = builder.build()

    def test_independent_expected_set_locks_five_by_three_variants(self):
        expected = self.document["expected_set"]
        self.assertEqual(
            "independent_t16c_acceptance_policy", expected["source"]
        )
        self.assertFalse(expected["production_mapping_reused"])
        self.assertEqual(EXPECTED_RECIPE_IDS, set(
            expected["machine_variant_recipe_ids"]
        ))
        self.assertEqual(5, len(expected["selected_kinds"]))
        self.assertEqual(3, len(expected["casing_dependency_recipe_ids"]))

    def test_results_casings_ingredients_and_producer_graph_are_closed(self):
        self.assertEqual(
            "T16C_MACHINE_ACQUISITION_READY",
            self.document["status"],
        )
        self.assertEqual({
            "selected_kinds": 5,
            "selected_variants": 15,
            "casing_dependencies": 3,
            "machine_ingredient_rows": 51,
            "registered_machine_results": 15,
            "producer_forms": 9,
            "source_declarations": 4,
            "unreachable": 0,
        }, self.document["counts"])
        self.assertEqual([], self.document["unreachable"])

        rows = {
            row["result_item"]: row
            for row in self.document["machine_variant_recipes"]
        }
        expected_results = {
            recipe_id.replace("cruciblecraft:machines/", "cruciblecraft:")
            for recipe_id in EXPECTED_RECIPE_IDS
        }
        self.assertEqual(expected_results, set(rows))
        for result, row in rows.items():
            with self.subTest(result=result):
                path = result.partition(":")[2]
                material = (
                    "titanium"
                    if path.startswith("titanium_")
                    else "steel"
                    if path.startswith("steel_")
                    else "bronze"
                )
                self.assertEqual(
                    f"cruciblecraft:{material}_double_machine_casing",
                    row["selected_casing"],
                )
                self.assertTrue(row["reachable"])
                ingredients = {
                    ingredient["item"]: ingredient
                    for ingredient in row["ingredients"]
                }
                self.assertIn(row["selected_casing"], ingredients)
                self.assertTrue(all(
                    ingredient["registered"]
                    and ingredient["reachable"]
                    and ingredient["producer"]
                    for ingredient in ingredients.values()
                ))

        currentness = self.document["currentness"]
        self.assertEqual(10, len(currentness["producer_rules"]))
        self.assertEqual(4, len(currentness["source_materials"]))
        self.assertIn(
            "tools/build_t15_machine_acquisition.py",
            currentness["support_implementation"],
        )
        self.assertNotIn(
            "ModRecipeProvider.java",
            json.dumps(currentness, sort_keys=True),
        )

    def test_mutations_fail_closed_on_result_casing_source_and_producer(self):
        original = builder.load(builder.POLICY)
        mutations = (
            lambda value: value["machine_variant_recipes"][
                "cruciblecraft:machines/lathe"
            ].__setitem__("result", "cruciblecraft:steel_lathe"),
            lambda value: value["machine_variant_recipes"][
                "cruciblecraft:machines/press"
            ].__setitem__(
                "selected_casing",
                "cruciblecraft:steel_double_machine_casing",
            ),
            lambda value: value["source_declarations"].pop(
                "cruciblecraft:bronze/ingot"
            ),
            lambda value: value["producer_rules"].pop("small_gear"),
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
