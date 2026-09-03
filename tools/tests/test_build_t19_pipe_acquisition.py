from __future__ import annotations

import copy
import json
import unittest

from tools import build_t19_pipe_acquisition as builder


class T19PipeAcquisitionTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.document = builder.build()

    def test_exact_25_row_classification_is_closed(self):
        counts = self.document["counts"]
        self.assertEqual(25, counts["rows"])
        self.assertEqual(5, counts["materials"])
        self.assertEqual(5, counts["gauges"])
        self.assertEqual(5, counts["gt6_source_crafting"])
        self.assertEqual(20, counts["design_policy_non_gt6"])
        self.assertEqual(0, counts["unreachable"])
        self.assertEqual([], self.document["unreachable"])

    def test_wood_rows_lock_source_lines_patterns_and_operands(self):
        rows = [
            row
            for row in self.document["rows"]
            if row["material"] == "wood"
        ]
        self.assertEqual([1887, 1888, 1889, 1890, 1891], [
            row["source_line"] for row in rows
        ])
        self.assertEqual(
            [
                "OD.slabWood",
                "OD.plankAnyWood",
                "OD.plankAnyWood",
                "OD.plankAnyWood",
                "OD.beamWood",
            ],
            [row["source_operand"] for row in rows],
        )
        self.assertEqual(
            [" s", " W ", "r "], rows[0]["source_pattern"]
        )
        self.assertEqual(
            ["WWs", "W W", "rWW"], rows[3]["source_pattern"]
        )

    def test_design_rows_keep_gt6_false_and_have_produced_operands(self):
        design = [
            row
            for row in self.document["rows"]
            if row["classification"] == "DESIGN_POLICY_NON_GT6"
        ]
        self.assertEqual(20, len(design))
        self.assertTrue(
            all(row["gt6_recipe_flag"] is False for row in design)
        )
        self.assertTrue(
            all(row["reason"] and row["operand_pattern"] for row in design)
        )
        self.assertTrue(
            all(
                producer["reachable"]
                for row in design
                for producer in row["producer_operands"].values()
            )
        )

    def test_every_output_resource_domain_is_reachable(self):
        for row in self.document["rows"]:
            reachability = row["reachability"]
            self.assertTrue(reachability["reachable"], row["output"])
            self.assertTrue(reachability["block_registered"])
            self.assertTrue(reachability["item_registered"])
            self.assertTrue(reachability["recipe"])
            self.assertTrue(reachability["loot"])
            self.assertTrue(reachability["block_tag"].startswith("c:"))
            self.assertTrue(reachability["item_tag"].startswith("c:"))
            self.assertTrue(reachability["block_model"])
            self.assertTrue(
                reachability["item_model"].startswith(
                    "runtime-generated:"
                )
            )

    def test_mixed_gauge_and_material_any_mutations_fail_closed(self):
        policy = builder.load(builder.POLICY)
        changed = copy.deepcopy(policy)
        changed["rows"][0]["specification"] = "pipeSmall"
        with self.assertRaises(ValueError):
            builder.build(changed)

        changed = copy.deepcopy(policy)
        changed["rows"][5]["classification"] = "GT6_SOURCE_CRAFTING"
        with self.assertRaises(ValueError):
            builder.build(changed)

    def test_rows_equal_t8_pipe_domain_acquisition_forms(self):
        """O-27 inversion: #48 proves equality against T8's derived forms."""
        t8 = builder.load(builder.T8_READINESS)
        acquisition = t8["nonmetal_fluid_pipe_acquisition"]
        self.assertEqual("CLOSED_T19C", acquisition["status"])
        expected = {
            (str(row["material"]), str(row["gauge"]))
            for row in acquisition["forms"]
        }
        actual = {
            (str(row["material"]), str(row["gauge"]))
            for row in self.document["rows"]
        }
        self.assertEqual(25, len(expected))
        self.assertEqual(expected, actual)

    def test_t8_readiness_is_an_owned_input(self):
        key = "tools/t8_pipe_readiness.json"
        self.assertIn(key, self.document["currentness"]["owned_inputs"])
        self.assertEqual(
            64,
            len(self.document["currentness"]["owned_inputs"][key]),
        )

    def test_artifact_matches_current_builder(self):
        before = builder.OUTPUT.read_bytes()
        self.assertEqual([], builder.check())
        self.assertEqual(before, builder.OUTPUT.read_bytes())


if __name__ == "__main__":
    unittest.main()
