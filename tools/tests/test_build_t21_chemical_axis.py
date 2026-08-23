import copy
import sys
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_t21_chemical_axis as builder  # noqa: E402


class T21ChemicalAxisTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.policy = builder.load(builder.POLICY)
        cls.axis = builder.load(builder.AXIS_OUTPUT)
        cls.expected = builder.load(builder.EXPECTED_OUTPUT)
        cls.expansion = builder.load(builder.EXPANSION_OUTPUT)

    def test_authoritative_denominator_corrects_stale_234_prose(self):
        self.assertEqual(224, self.axis["denominator"]["candidates"])
        self.assertEqual(234, self.axis["denominator"]["stale_prose_value"])
        self.assertEqual(
            {
                "blocked_by_new_subsystem": 70,
                "composition_generated": 84,
                "named_reaction": 70,
                "prefix_matrix": 0,
            },
            self.axis["counts"]["classifications"],
        )
        self.assertEqual(0, self.axis["counts"]["unclassified"])
        self.assertEqual(
            {
                "rows": 110,
                "overlap_with_candidates": 60,
                "outside_candidates": 50,
                "added_to_candidate_denominator": 0,
            },
            self.axis["byproduct_only_debt"],
        )

    def test_every_candidate_and_referenced_map_is_classified(self):
        candidates = self.axis["candidates"]
        self.assertEqual(224, len(candidates))
        self.assertEqual(224, len({row["material"] for row in candidates}))
        self.assertTrue(
            all(row["classification"] in {
                "composition_generated",
                "named_reaction",
                "blocked_by_new_subsystem",
            } for row in candidates)
        )
        blocked = [
            row for row in candidates
            if row["classification"] == "blocked_by_new_subsystem"
        ]
        self.assertEqual(70, len(blocked))
        for row in blocked:
            self.assertTrue(row["blocking"]["reason"])
            self.assertTrue(row["blocking"]["dependency"])
            self.assertTrue(row["blocking"]["owner"])
            self.assertTrue(row["blocking"]["replacement_condition"])
            self.assertTrue(row["blocking"]["recheck_point"])
        self.assertEqual(9, len(self.axis["recipe_maps"]))
        self.assertTrue(all(
            row["status"] == "CLASSIFIED"
            for row in self.axis["recipe_maps"]
        ))

    def test_carbon_family_expected_expansion_and_runtime_are_equal(self):
        builder.validate_policy(self.policy)
        builder.validate_compact_expected(self.policy, self.expected)
        rebuilt = builder.build_expansion(self.policy, self.expected)
        self.assertEqual(self.expansion, rebuilt)
        self.assertEqual(
            ["charcoal", "coal"],
            [row["material"] for row in rebuilt["rows"]],
        )
        self.assertEqual(
            {
                "source_facts": 2,
                "authored_rules": 1,
                "datapack_files": 2,
                "logical_rows": 2,
                "eager_rows": 2,
                "lazy_rows": 0,
            },
            rebuilt["counts"],
        )
        self.assertEqual(
            {"logical": 0, "eager": 0, "lazy": 0},
            rebuilt["publication_delta"],
        )
        for row in rebuilt["rows"]:
            recipe = row["recipe"]
            self.assertEqual("cruciblecraft:electrolyzer", recipe["map"])
            self.assertEqual(292, recipe["duration"])
            self.assertEqual(16, recipe["eut"])
            self.assertEqual(
                "cruciblecraft:carbon/dust",
                recipe["item_outputs"][0]["id"],
            )

    def test_wrong_expected_field_breaks_composition_equivalence(self):
        changed = copy.deepcopy(self.expected)
        changed["rows"][0]["recipe"]["duration"] += 1
        with self.assertRaisesRegex(ValueError, "differs from independent expected"):
            builder.build_expansion(self.policy, changed)

    def test_full_source_rows_replay_to_compact_expected(self):
        replay = builder.build_expected_full_replay(self.policy)
        self.assertEqual(self.expected, replay)

    def test_rebuild_preserves_semantic_axis_fields(self):
        expansion = builder.build_expansion(self.policy, self.expected)
        candidate = builder.build_axis(self.policy, expansion)
        self.assertEqual(
            builder.semantic_axis(self.axis),
            builder.semantic_axis(candidate),
        )

    def test_metadata_rebase_guard_accepts_t5_input_hash_refresh(self):
        expansion = builder.build_expansion(self.policy, self.expected)
        candidate = builder.build_axis(self.policy, expansion)
        pre_repair = copy.deepcopy(self.axis)
        pre_repair["inputs"]["tools/t5_chemical_readiness.json"] = (
            "2cc6c9fbaca4a4b41d1abe7bb1a5eb00ff6d6a572aaad86ac3597bd35b1a7062"
        )
        pre_repair["inputs"]["tools/t5_chemical_recipe_manifest.json"] = (
            "c31b40f91de43f15a9e1a6ff64862fae683e28df04fc31015d6c77bb7555b383"
        )
        self.assertEqual(
            [],
            builder.verify_metadata_rebase(pre_repair, candidate),
        )


if __name__ == "__main__":
    unittest.main()
