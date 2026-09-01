from __future__ import annotations

import json
import unittest

from tools import build_t11_hydrocarbon_recipes as builder


class T11HydrocarbonRecipeBuilderTest(unittest.TestCase):
    def test_committed_recipe_set_is_current(self):
        self.assertEqual([], builder.reference_only_check())

    def test_manifest_is_bidirectionally_closed(self):
        manifest = json.loads(builder.MANIFEST.read_text(encoding="utf-8"))
        self.assertEqual("T11_FIXED_ROWS_PUBLISHED", manifest["status"])
        self.assertEqual(
            {
                "chemical_processing": 2,
                "fuel_generation": 2,
                "material_rules": 0,
                "published": 4,
            },
            manifest["counts"],
        )
        self.assertEqual(
            set(builder.ROUTES),
            {row["route"] for row in manifest["recipes"]},
        )

    def test_signed_energy_and_coproducts_are_preserved(self):
        documents = {
            relative: json.loads(path.read_text(encoding="utf-8"))
            for relative, path in builder.actual_files().items()
        }
        engine = documents[
            "data/cruciblecraft/recipe/hydrocarbon/"
            "fuels_engine/fuel_oil.json"
        ]
        gas = documents[
            "data/cruciblecraft/recipe/hydrocarbon/fuels_gas/methane.json"
        ]
        distillery = documents[
            "data/cruciblecraft/recipe/hydrocarbon/distillery/"
            "crude_oil_to_fuel_and_lubricant.json"
        ]
        self.assertEqual(-64, engine["eut"])
        self.assertEqual(-64, gas["eut"])
        self.assertEqual(24, gas["duration"])
        self.assertTrue(
            gas["provenance"]["selected_source_recipe"].endswith(
                "gt.recipe.fuels.burn.json#recipes[20]"
            )
        )
        self.assertEqual(2, len(gas["fluid_outputs"]))
        self.assertEqual(2, len(distillery["fluid_outputs"]))


if __name__ == "__main__":
    unittest.main()
