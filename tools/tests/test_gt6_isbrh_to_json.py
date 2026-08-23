from __future__ import annotations

import unittest

from tools import gt6_isbrh_to_json as conv


def _xyzy(element: dict) -> tuple:
    return tuple(element["from"] + element["to"])


class Gt6IsbrhToJsonTest(unittest.TestCase):
    def test_anvil_empty_drops_workpieces_and_nei(self) -> None:
        result = conv.convert_class("MultiTileEntityAnvil")
        self.assertIsNone(result.error, result.error)
        self.assertEqual(5, len(result.elements))
        boxes = [_xyzy(element) for element in result.elements]
        self.assertIn((2.0, 0.0, 4.0, 14.0, 4.0, 12.0), boxes)
        self.assertIn((4.0, 4.0, 6.0, 12.0, 8.0, 10.0), boxes)
        for element in result.elements:
            from_y, to_y = element["from"][1], element["to"][1]
            self.assertFalse(from_y >= 12.0 and to_y >= 16.0)
            self.assertNotEqual(
                (round(element["from"][0], 4), round(element["from"][1], 4)),
                (12.0, 0.0),
            )
        neck = result.elements[1]
        self.assertEqual({"north", "south", "west", "east"}, set(neck["faces"]))
        self.assertNotIn("up", neck["faces"])
        self.assertNotIn("down", neck["faces"])

    def test_basin_empty_drops_molten_fill_and_culls_wall_faces(self) -> None:
        result = conv.convert_class("MultiTileEntityBasin")
        self.assertIsNone(result.error, result.error)
        self.assertEqual(5, len(result.elements))
        boxes = [_xyzy(element) for element in result.elements]
        self.assertNotIn((0.0, 0.0, 0.0, 16.0, 15.0, 16.0), boxes)
        west = result.elements[0]
        self.assertEqual((0.0, 0.0, 0.0, 1.0, 16.0, 16.0), _xyzy(west))
        self.assertEqual({"up", "west", "east"}, set(west["faces"]))
        self.assertNotIn("north", west["faces"])
        self.assertNotIn("south", west["faces"])
        self.assertNotIn("down", west["faces"])

    def test_nested_ternary_converts_right_associative_java(self) -> None:
        self.assertEqual(
            "((6 if mTextureA == None else 7) if mTextureB == None else 8)",
            conv.convert_java_expr("mTextureB == null ? mTextureA == null ? 6 : 7 : 8"),
        )
        self.assertEqual(
            "box(aBlock, PX_P[(4 if SIDES_AXIS_X[mFacing] else 2)], PX_P[ 0])",
            conv.convert_java_expr("box(aBlock, PX_P[SIDES_AXIS_X[mFacing]? 4: 2], PX_P[ 0])"),
        )

    def test_cup_empty_keeps_walls_without_liquid(self) -> None:
        result = conv.convert_class("MultiTileEntityCup")
        self.assertIsNone(result.error, result.error)
        self.assertEqual(5, len(result.elements))
        boxes = [_xyzy(element) for element in result.elements]
        self.assertIn((6.0, 0.0, 6.0, 10.0, 1.0, 10.0), boxes)
        self.assertNotIn((6.0, 0.0, 6.0, 10.0, 4.0, 10.0), boxes)


if __name__ == "__main__":
    unittest.main()
