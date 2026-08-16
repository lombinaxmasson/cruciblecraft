from __future__ import annotations

import copy
import json
import tempfile
import unittest
from pathlib import Path
from unittest import mock

from tools import build_t23_multiblock_classification as builder


class T23MultiblockClassificationTest(unittest.TestCase):
    def setUp(self) -> None:
        self.policy = json.loads(
            builder.POLICY.read_text(encoding="utf-8")
        )
        self.t13 = json.loads(
            builder.T13_KINDS.read_text(encoding="utf-8")
        )

    def test_committed_artifact_is_current_and_check_is_read_only(self) -> None:
        on_disk = json.loads(
            builder.OUTPUT.read_text(encoding="utf-8")
        )
        expected = builder.build()
        self.assertEqual(expected, on_disk)
        self.assertEqual([], builder.check())

    def test_counts_are_closed(self) -> None:
        document = builder.build()
        self.assertEqual(
            {
                "canonical": 30,
                "classified": 30,
                "v1_required": 6,
                "post_1_0": 19,
                "out_of_scope": 5,
                "unclassified": 0,
            },
            document["counts"],
        )
        self.assertEqual(
            "T23_MULTIBLOCK_CLASSIFICATION_READY", document["status"]
        )

    def test_v1_required_set_serves_closed_chains(self) -> None:
        document = builder.build()
        v1_ids = {
            row["canonical_id"]
            for row in document["kinds"]
            if row["classification"]["class"] == "v1_required"
        }
        self.assertEqual(
            {
                "centrifuge",
                "coke_oven",
                "crucible",
                "distillation_tower",
                "large_boiler",
                "tank_3x3x3",
            },
            v1_ids,
        )

    def test_kind_ids_are_a_bijection_with_t13_and_raw_members_match(
        self,
    ) -> None:
        document = builder.build()
        t13_rows = {
            row["canonical_id"]: row for row in self.t13["canonical_kinds"]
        }
        self.assertEqual(
            set(t13_rows),
            {row["canonical_id"] for row in document["kinds"]},
        )
        for row in document["kinds"]:
            self.assertEqual(
                t13_rows[row["canonical_id"]]["raw_members"],
                row["source_identity"]["raw_members"],
                row["canonical_id"],
            )

    def test_every_kind_has_all_five_fact_groups(self) -> None:
        document = builder.build()
        for row in document["kinds"]:
            for group in (
                "source_identity",
                "schema_expressibility",
                "controller_behavior",
                "economy",
                "classification",
            ):
                self.assertTrue(row.get(group), (row["canonical_id"], group))

    def test_non_v1_rows_carry_complete_deferred_contracts(self) -> None:
        document = builder.build()
        for row in document["kinds"]:
            classification = row["classification"]
            if classification["class"] in ("post_1_0", "out_of_scope"):
                for field in builder.DEFERRED_FIELDS:
                    self.assertTrue(
                        classification.get(field),
                        (row["canonical_id"], field),
                    )

    def test_selection_has_four_criteria_and_unselected_has_recheck(self) -> None:
        document = builder.build()
        selected = document["selected"]
        self.assertEqual(
            {"distillation_tower", "large_boiler", "tank_3x3x3"},
            {row["canonical_id"] for row in selected},
        )
        v1_ids = {
            row["canonical_id"]
            for row in document["kinds"]
            if row["classification"]["class"] == "v1_required"
        }
        self.assertTrue({row["canonical_id"] for row in selected}.issubset(v1_ids))
        for row in selected:
            criteria = row["selection_criteria"]
            for key in (
                "serves_closed_chain",
                "inputs_obtainable_products_consumed",
                "schema_v1_expressible",
                "behavioral_difference",
            ):
                self.assertTrue(criteria.get(key), (row["canonical_id"], key))
            self.assertTrue(row["fidelity_evidence"])
            self.assertIn("acquisition", row)
            self.assertTrue(row["consumers"])
        unselected = document["unselected_v1_required"]
        self.assertEqual(
            {"crucible"}, {row["canonical_id"] for row in unselected}
        )
        for row in unselected:
            self.assertTrue(row["reason"])
            self.assertTrue(row["recheck_point"])

    def test_schema_policy_is_zero_extension(self) -> None:
        document = builder.build()
        policy = document["schema_policy"]
        self.assertEqual(1, policy["schema_version"])
        self.assertEqual("no_extension", policy["verdict"])
        self.assertTrue(policy["justification"])
        self.assertTrue(policy["old_structures_untouched"])

    def test_schema_expressibility_verdicts_are_recorded(self) -> None:
        document = builder.build()
        for row in document["kinds"]:
            expr = row["schema_expressibility"]
            self.assertIn(
                expr["verdict"],
                ("v1_expressible", "needs_extension"),
            )
            self.assertGreaterEqual(expr["positions"], 1)
            self.assertIsInstance(expr["needed_extensions"], list)

    def test_mutation_flipping_class_to_unknown_fails_closed(self) -> None:
        policy = copy.deepcopy(self.policy)
        policy["kinds"][0]["classification"]["class"] = "v2_only"
        with self._policy_override(policy):
            with self.assertRaises(ValueError):
                builder.build()

    def test_mutation_dropping_recheck_point_fails_closed(self) -> None:
        policy = copy.deepcopy(self.policy)
        for row in policy["kinds"]:
            if row["classification"]["class"] == "post_1_0":
                row["classification"]["recheck_point"] = None
                break
        with self._policy_override(policy):
            with self.assertRaises(ValueError):
                builder.build()

    def test_mutation_missing_kind_fails_closed(self) -> None:
        policy = copy.deepcopy(self.policy)
        policy["kinds"] = policy["kinds"][:-1]
        with self._policy_override(policy):
            with self.assertRaises(ValueError):
                builder.build()

    def _policy_override(self, policy):
        real = builder._load_if_exists
        return mock.patch.object(
            builder,
            "_load_if_exists",
            side_effect=lambda p, d=None: (
                policy if p == builder.POLICY else real(p, d)
            ),
        )


if __name__ == "__main__":
    unittest.main()
