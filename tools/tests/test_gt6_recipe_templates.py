from __future__ import annotations

import unittest

from tools import gt6_recipe_templates as templates


class RecipeTemplateExtractionTest(unittest.TestCase):
    def test_mixer_foam_family_collapses_to_cartesian_axes(self) -> None:
        recipes = templates.load_map("gt.recipe.mixer")
        extracted = templates.extract_map_templates("gt.recipe.mixer", recipes)
        self.assertGreater(len(extracted), 0)
        top = extracted[0]
        # Construction-foam family should dominate mixer.
        self.assertGreaterEqual(top.expanded_count, 1000)
        paths = {axis.path: axis.value_count for axis in top.axes}
        self.assertIn("inputs[0].meta", paths)
        self.assertGreaterEqual(paths["inputs[0].meta"], 50)
        self.assertAlmostEqual(top.cartesian_ratio, 1.0, places=3)

    def test_material_heat_prefers_templates_for_sio2(self) -> None:
        recipes = templates.load_map("gt.recipe.mixer")
        heat = templates.build_material_heat("gt.recipe.mixer", recipes)
        sio2 = heat[8000]
        self.assertGreater(sio2["expanded_appearances"], 2000)
        self.assertLess(sio2["template_appearances"], 50)
        self.assertGreater(
            sio2["expanded_appearances"] / sio2["template_appearances"],
            20,
        )


if __name__ == "__main__":
    unittest.main()
