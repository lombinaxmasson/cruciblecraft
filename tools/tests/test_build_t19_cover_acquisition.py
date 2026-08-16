from __future__ import annotations

import copy
import json
import unittest

from tools import build_t19_cover_acquisition as builder


class T19CoverAcquisitionTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.document = builder.build()

    def test_selected_items_recipes_resources_and_producers_are_reachable(self):
        self.assertEqual(
            "T19_COVER_ACQUISITION_READY", self.document["status"]
        )
        self.assertEqual(5, self.document["counts"]["selected_kinds"])
        self.assertEqual(5, self.document["counts"]["registered_items"])
        self.assertEqual(
            5, self.document["counts"]["vanilla_crafting_recipes"]
        )
        self.assertEqual(5, self.document["counts"]["item_models"])
        self.assertEqual(10, self.document["counts"]["language_rows"])
        self.assertEqual(0, self.document["counts"]["unreachable"])
        self.assertEqual([], self.document["unreachable"])
        for row in self.document["rows"]:
            self.assertTrue(row["reachable"])
            self.assertTrue(row["registered"])
            self.assertTrue(row["ingredients"])
            self.assertTrue(
                all(
                    ingredient["registered"]
                    and ingredient["reachable"]
                    and ingredient["producer"] == "minecraft_survival"
                    for ingredient in row["ingredients"]
                )
            )
            self.assertTrue(all(row["resources"]["languages"].values()))

    def test_expected_set_is_independent_and_bidirectional(self):
        expected = self.document["expected_set"]
        self.assertEqual(
            "independent_t19_selected_cover_policy", expected["source"]
        )
        self.assertEqual(
            {row["canonical_id"] for row in self.document["rows"]},
            set(expected["canonical_ids"]),
        )
        self.assertEqual(
            {row["item"] for row in self.document["rows"]},
            set(expected["item_ids"]),
        )

    def test_missing_source_declaration_fails_closed(self):
        policy = builder.load(builder.POLICY)
        changed = copy.deepcopy(policy)
        changed["survival_sources"].remove("minecraft:hopper")
        with self.assertRaises(ValueError):
            builder.build(changed)

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
