from __future__ import annotations

import copy
import json
import unittest

from tools import build_t19_cover_denominator as builder


class T19CoverDenominatorTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.document = builder.build()

    def test_t13_47_kinds_are_exactly_partitioned(self):
        self.assertEqual(
            {
                "canonical": 47,
                "implemented": 4,
                "selected_t19": 5,
                "deferred_with_reason": 28,
                "out_of_scope": 10,
                "unclassified": 0,
            },
            self.document["counts"],
        )
        self.assertEqual(47, len(self.document["rows"]))

    def test_selected_set_is_exact_and_second_definition_is_data_only(self):
        self.assertEqual(
            {
                "conveyor",
                "retriever_item",
                "robot_arm",
                "pressure_valve",
                "selector_manual",
            },
            set(self.document["selected"]),
        )
        reuse = self.document["pure_json_reuse"]
        self.assertEqual("cruciblecraft:conveyor", reuse["required_behavior"])
        self.assertEqual(0, reuse["java_plugin_delta"])
        self.assertNotEqual(reuse["base_rate"], reuse["second_rate"])

    def test_policy_count_mutation_fails_closed(self):
        policy = builder.load(builder.POLICY)
        changed = copy.deepcopy(policy)
        changed["expected_counts"]["selected_t19"] = 6
        with self.assertRaises(ValueError):
            builder.build(changed)

    def test_artifact_matches_current_builder(self):
        before = builder.OUTPUT.read_bytes()
        self.assertEqual([], builder.check())
        self.assertEqual(before, builder.OUTPUT.read_bytes())


if __name__ == "__main__":
    unittest.main()
