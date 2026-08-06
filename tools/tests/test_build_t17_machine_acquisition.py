from __future__ import annotations

import copy
import json
import unittest

from tools import build_t17_machine_acquisition as builder


HU_RECIPE_IDS = {
    f"cruciblecraft:machines/{prefix}{kind}"
    for kind in ("distillery", "drying", "smelter")
    for prefix in ("", "invar_", "titanium_")
}
ELECTROLYZER_RECIPE_IDS = {
    "cruciblecraft:machines/electrolyzer",
    "cruciblecraft:machines/aluminium_electrolyzer",
    "cruciblecraft:machines/stainless_steel_electrolyzer",
}


class T17MachineAcquisitionTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.document = builder.build()

    def test_independent_expected_set_locks_three_by_three_hu_variants(self):
        expected = self.document["expected_set"]
        self.assertEqual(
            "independent_t17c_acceptance_policy", expected["source"]
        )
        self.assertFalse(expected["production_mapping_reused"])
        self.assertEqual(
            HU_RECIPE_IDS, set(expected["machine_variant_recipe_ids"])
        )
        self.assertEqual(3, len(expected["selected_kinds"]))
        self.assertEqual(2, len(expected["casing_dependency_recipe_ids"]))

    def test_results_casings_ingredients_and_producer_graph_are_closed(self):
        self.assertEqual(
            "T17C_MACHINE_ACQUISITION_READY",
            self.document["status"],
        )
        self.assertEqual({
            "selected_kinds": 3,
            "selected_variants": 9,
            "casing_dependencies": 2,
            "machine_ingredient_rows": 42,
            "registered_machine_results": 9,
            "producer_forms": 6,
            "material_source_declarations": 7,
            "concrete_source_declarations": 3,
            "preimplemented_reference_variants": 3,
            "gt_recipe_rows_added": 0,
            "unreachable": 0,
        }, self.document["counts"])
        self.assertEqual([], self.document["unreachable"])

        rows = {
            row["result_item"]: row
            for row in self.document["machine_variant_recipes"]
        }
        self.assertEqual(
            {
                recipe_id.replace(
                    "cruciblecraft:machines/", "cruciblecraft:"
                )
                for recipe_id in HU_RECIPE_IDS
            },
            set(rows),
        )
        for result, row in rows.items():
            with self.subTest(result=result):
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
                material = (
                    "titanium"
                    if result.startswith("cruciblecraft:titanium_")
                    else "invar"
                    if result.startswith("cruciblecraft:invar_")
                    else "steel"
                )
                self.assertIn(
                    f"cruciblecraft:{material}/plate", ingredients
                )

    def test_electrolyzers_are_preimplemented_evidence_not_t17_generation(self):
        references = self.document["preimplemented_references"]
        self.assertEqual(
            ELECTROLYZER_RECIPE_IDS,
            {row["recipe_id"] for row in references},
        )
        for row in references:
            self.assertTrue(row["preimplemented"])
            self.assertFalse(row["generated_by_t17c"])
            self.assertNotIn(
                row["recipe_id"],
                self.document["expected_set"][
                    "machine_variant_recipe_ids"
                ],
            )

    def test_currentness_covers_policy_recipes_and_producer_sources(self):
        currentness = self.document["currentness"]
        self.assertEqual(
            {
                "tools/build_t17_machine_acquisition.py",
                "tools/t17_machine_acquisition_policy.json",
            },
            set(currentness["owned_inputs"]),
        )
        self.assertEqual(14, len(currentness["crafting_recipes"]))
        self.assertEqual(6, len(currentness["producer_rules"]))
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
                "cruciblecraft:machines/distillery"
            ].__setitem__(
                "result", "cruciblecraft:invar_distillery"
            ),
            lambda value: value["machine_variant_recipes"][
                "cruciblecraft:machines/invar_drying"
            ].__setitem__(
                "selected_casing",
                "cruciblecraft:titanium_double_machine_casing",
            ),
            lambda value: value["source_declarations"].pop(
                "cruciblecraft:kanthal/ingot"
            ),
            lambda value: value["producer_rules"].pop("quadruple_wire"),
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
