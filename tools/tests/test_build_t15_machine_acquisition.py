from __future__ import annotations

import copy
import hashlib
import json
import unittest
from pathlib import Path

from tools import build_t15_machine_acquisition as builder


def digest(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


class T15MachineAcquisitionTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.document = builder.build()

    def test_independent_expected_set_locks_six_casings_and_nine_variants(self):
        expected = self.document["expected_set"]
        self.assertEqual(
            "independent_t15c_acceptance_policy", expected["source"]
        )
        self.assertFalse(expected["production_mapping_reused"])
        self.assertEqual(6, len(expected["casing_recipe_ids"]))
        self.assertEqual(
            {
                "cruciblecraft:machines/aluminium_electrolyzer",
                "cruciblecraft:machines/centrifuge",
                "cruciblecraft:machines/electrolyzer",
                "cruciblecraft:machines/sifter",
                "cruciblecraft:machines/stainless_steel_electrolyzer",
                "cruciblecraft:machines/steel_centrifuge",
                "cruciblecraft:machines/steel_sifter",
                "cruciblecraft:machines/titanium_centrifuge",
                "cruciblecraft:machines/titanium_sifter",
            },
            set(expected["machine_variant_recipe_ids"]),
        )
        self.assertNotIn(
            "ModRecipeProvider.java",
            json.dumps(self.document["currentness"], sort_keys=True),
        )

    def test_recipe_results_casing_choices_and_operand_closure_are_locked(self):
        counts = self.document["counts"]
        self.assertEqual("T15C_MACHINE_ACQUISITION_READY", self.document["status"])
        self.assertEqual(6, counts["casing_recipes"])
        self.assertEqual(9, counts["machine_variant_recipes"])
        self.assertEqual(15, counts["registered_results"])
        self.assertEqual(36, counts["ingredient_rows"])
        self.assertEqual(0, counts["unreachable"])
        self.assertEqual([], self.document["unreachable"])

        expected_casings = {
            "cruciblecraft:centrifuge": "cruciblecraft:bronze_double_machine_casing",
            "cruciblecraft:steel_centrifuge": "cruciblecraft:steel_double_machine_casing",
            "cruciblecraft:titanium_centrifuge": "cruciblecraft:titanium_double_machine_casing",
            "cruciblecraft:sifter": "cruciblecraft:bronze_double_machine_casing",
            "cruciblecraft:steel_sifter": "cruciblecraft:steel_double_machine_casing",
            "cruciblecraft:titanium_sifter": "cruciblecraft:titanium_double_machine_casing",
            "cruciblecraft:electrolyzer": "cruciblecraft:steel_galvanized_machine_casing",
            "cruciblecraft:aluminium_electrolyzer": "cruciblecraft:aluminium_machine_casing",
            "cruciblecraft:stainless_steel_electrolyzer": "cruciblecraft:stainless_steel_machine_casing",
        }
        rows = {
            row["result_item"]: row
            for row in self.document["machine_variant_recipes"]
        }
        self.assertEqual(expected_casings, {
            result: row["selected_casing"]
            for result, row in rows.items()
        })
        for recipe in (
            self.document["casing_recipes"]
            + self.document["machine_variant_recipes"]
        ):
            with self.subTest(recipe=recipe["recipe_id"]):
                self.assertTrue(recipe["reachable"])
                self.assertTrue(recipe["ingredients"])
                for ingredient in recipe["ingredients"]:
                    self.assertTrue(ingredient["registered"])
                    self.assertTrue(ingredient["reachable"])
                    self.assertIn(
                        ingredient["classification"],
                        {
                            "compact_material_rule",
                            "concrete_shaped_recipe",
                            "material_rule",
                        },
                    )
                    self.assertTrue(ingredient["producer"])

    def test_mutations_fail_closed_on_result_casing_and_source_reachability(self):
        original = json.loads(builder.POLICY.read_text(encoding="utf-8"))
        mutations = (
            lambda value: value["casing_recipes"][
                "cruciblecraft:components/bronze_double_machine_casing"
            ].__setitem__(
                "result", "cruciblecraft:steel_double_machine_casing"
            ),
            lambda value: value["machine_variant_recipes"][
                "cruciblecraft:machines/centrifuge"
            ].__setitem__(
                "selected_casing",
                "cruciblecraft:steel_double_machine_casing",
            ),
            lambda value: value["source_declarations"].pop(
                "cruciblecraft:bronze/ingot"
            ),
        )
        for mutate in mutations:
            candidate = copy.deepcopy(original)
            mutate(candidate)
            with self.subTest(mutation=mutate):
                with self.assertRaises(ValueError):
                    builder.build(candidate)

    def test_check_mode_is_current_and_immutable(self):
        before = digest(builder.OUTPUT)
        self.assertEqual([], builder.check())
        self.assertEqual(before, digest(builder.OUTPUT))
        self.assertEqual(
            self.document,
            json.loads(builder.OUTPUT.read_text(encoding="utf-8")),
        )


if __name__ == "__main__":
    unittest.main()
