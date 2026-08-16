import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import gt6_gui_layout as layout  # noqa: E402


class GT6GuiLayoutTest(unittest.TestCase):
    def test_one_in_one_out_matches_gt6_default_row(self):
        converted = layout.layout(1, 1, 0, 0, 1, 1, 0, 0)
        self.assertEqual(
            [{"x": 53, "y": 25}, {"x": 107, "y": 25}],
            converted["item_slots"],
        )
        self.assertEqual([], converted["tanks"])
        self.assertEqual(layout.PROGRESS, converted["progress"])

    def test_crusher_uses_first_of_twelve_outputs(self):
        converted = layout.layout(1, 12, 0, 0, 1, 1, 0, 0)
        self.assertEqual(
            [{"x": 53, "y": 25}, {"x": 107, "y": 7}],
            converted["item_slots"],
        )

    def test_mixer_keeps_six_fluid_item_shift(self):
        converted = layout.layout(6, 1, 6, 2, 4, 1, 3, 2)
        self.assertEqual(
            [
                {"x": 17, "y": 7},
                {"x": 35, "y": 7},
                {"x": 53, "y": 7},
                {"x": 17, "y": 25},
                {"x": 107, "y": 25},
            ],
            converted["item_slots"],
        )
        self.assertEqual(
            [
                {"tank": 0, "x": 53, "y": 63, "width": 18, "height": 18},
                {"tank": 1, "x": 35, "y": 63, "width": 18, "height": 18},
                {"tank": 2, "x": 17, "y": 63, "width": 18, "height": 18},
                {"tank": 3, "x": 107, "y": 63, "width": 18, "height": 18},
                {"tank": 4, "x": 125, "y": 63, "width": 18, "height": 18},
            ],
            converted["tanks"],
        )

    def test_extruder_inserts_special_slot_between_material_and_output(self):
        converted = layout.layout(2, 2, 0, 0, 1, 1, 0, 0, 1)
        self.assertEqual(
            [
                {"x": 35, "y": 25},
                {"x": 80, "y": 43},
                {"x": 107, "y": 25},
            ],
            converted["item_slots"],
        )

    def test_smelter_uses_larger_output_grid_when_cc_has_more_slots(self):
        converted = layout.layout(1, 1, 1, 1, 1, 4, 0, 1)
        self.assertEqual({"x": 53, "y": 25}, converted["item_slots"][0])
        self.assertEqual(
            [
                {"x": 53, "y": 25},
                {"x": 107, "y": 16},
                {"x": 125, "y": 16},
                {"x": 107, "y": 34},
                {"x": 125, "y": 34},
            ],
            converted["item_slots"],
        )
        self.assertEqual(
            [{"tank": 0, "x": 107, "y": 63, "width": 18, "height": 18}],
            converted["tanks"],
        )

    def test_committed_json_matches_algorithm(self):
        layout.check()


if __name__ == "__main__":
    unittest.main()
