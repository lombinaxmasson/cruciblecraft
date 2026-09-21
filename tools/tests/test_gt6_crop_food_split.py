#!/usr/bin/env python3
"""GT6 crop/food optional-split ledger and Core plant-form strip."""
from __future__ import annotations

import importlib.util
import unittest
from pathlib import Path

from tools import census_common as census

ROOT = census.ROOT
MODULE_PATH = (
    ROOT
    / "tools"
    / "waves"
    / "prep"
    / "gt6-crop-food-split"
    / "crop_food_split.py"
)


def _load():
    spec = importlib.util.spec_from_file_location("gt6_crop_food_split", MODULE_PATH)
    if spec is None or spec.loader is None:
        raise RuntimeError("failed to load crop_food_split.py")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


split = _load()


class Gt6CropFoodSplitTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.errors = split.check()
        cls.ledger = split.io.load_json(split.LEDGER)

    def test_builder_check_passes(self) -> None:
        self.assertEqual([], self.errors)

    def test_denominator_excludes_squeezer_ignore_rows(self) -> None:
        self.assertEqual(59, self.ledger["gt6_base_crop_count"])
        self.assertEqual(["ferru", "aurelia"], self.ledger["ic2_drop_rewrites"])
        self.assertEqual(61, self.ledger["crop_count"])
        self.assertEqual(35, self.ledger["food_card_count"])
        self.assertEqual(13, self.ledger["named_plant_form_count"])
        self.assertEqual(5215, self.ledger["ignored_squeezer_plant_gt_rows"])
        self.assertEqual(
            5215,
            self.ledger["not_crop_denominator"]["ignored_plant_gt_rows"],
        )
        self.assertEqual(
            "scale, not a crop card count",
            self.ledger["not_crop_denominator"]["plants_generation_flag_materials"],
        )
        ids = [crop["id"] for crop in self.ledger["crops"]]
        self.assertIn("indigo", ids)
        self.assertIn("ferru", ids)
        self.assertIn("aurelia", ids)
        self.assertNotIn("plant_gt_berry", ids)
        unparsed = [
            crop["id"]
            for crop in self.ledger["crops"]
            if crop["drop"].get("kind") == "unparsed"
            or crop["base_seed"].get("kind") == "unparsed"
        ]
        self.assertEqual([], unparsed)

    def test_named_pairs_are_bounded_plant_forms(self) -> None:
        pairs = {
            (row["material"], row["prefix"]) for row in self.ledger["named_plant_forms"]
        }
        self.assertEqual(13, len(pairs))
        self.assertIn(("indigo", "plant_gt_blossom"), pairs)
        self.assertIn(("iron", "plant_gt_blossom"), pairs)
        self.assertIn(("gold", "plant_gt_blossom"), pairs)
        self.assertIn(("copper", "plant_gt_fiber"), pairs)

    def test_core_gate_has_no_plant_forms(self) -> None:
        gate = split.io.load_json(split.GATE)
        live = 0
        for forms in (gate.get("materials") or {}).values():
            live += sum(1 for form in forms if form in split.PLANT_FORMS)
        self.assertEqual(0, live)

    def test_red_apple_stays_vanilla(self) -> None:
        apple = next(
            row
            for row in self.ledger["food_crop_items"]
            if row["il"] == "Food_Apple_Red"
        )
        self.assertEqual("minecraft:apple", apple["reuse_canonical"])


if __name__ == "__main__":
    unittest.main()
