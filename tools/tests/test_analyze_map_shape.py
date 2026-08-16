import sys
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import analyze_map_shape as analyzer  # noqa: E402


class AnalyzeMapShapeTest(unittest.TestCase):
    def test_material_meta_is_rendered_as_a_material_axis(self):
        token = analyzer.ingredient_key(
            {
                "item": "gregtech:gt.meta.dust",
                "meta": 8334,
                "count": 1,
            },
            {8334: "Coal"},
        )
        self.assertEqual(
            "gregtech:gt.meta.dust/coal|count=1", token
        )
        self.assertEqual(
            "material_matrix",
            analyzer.varying_axis([
                "gregtech:gt.meta.dust/coal|count=1",
                "gregtech:gt.meta.dust/charcoal|count=1",
            ]),
        )

    def test_leave_one_out_groups_do_not_double_count_rows(self):
        rows = [
            {
                "inputs": [{"item": f"c:dusts/{name}", "count": 1}],
                "outputs": [{"item": "minecraft:gunpowder", "count": 1}],
                "duration": 16,
            }
            for name in ("coal", "charcoal", "carbon")
        ]
        result = analyzer.analyze(
            rows,
            ["inputs"],
            ["outputs"],
            ["duration"],
            2,
        )
        self.assertEqual(3, result["rows_in_groups"])
        self.assertEqual(0, result["residual_named_rows"])
        self.assertEqual(1, result["authored_rule_estimate"])


if __name__ == "__main__":
    unittest.main()
