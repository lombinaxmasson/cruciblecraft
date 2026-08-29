"""Contract tests for the T39 materialization policy and decision builder."""
from __future__ import annotations

import json
import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_t39_recipe_load_benchmark as builder  # noqa: E402
from tools import t39_common as t39  # noqa: E402


class T39MaterializationPolicyTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.policy = builder.load(builder.POLICY)

    def test_policy_declares_two_groups_and_three_candidates(self) -> None:
        builder.validate_policy(self.policy)
        contract = self.policy["publication_group_contract"]
        self.assertEqual(
            ["immediate", "on_demand", "hybrid"],
            self.policy["candidate_contract"]["candidates"],
        )
        for group_key in ("singleton", "multi"):
            hybrid = contract[group_key]["hybrid_boundary"]
            self.assertTrue(hybrid["declared_before_measurement"])
            self.assertIsNone(hybrid["hot_modulo"])
            self.assertNotEqual(
                (hybrid["declared_eager_count"], hybrid["declared_lazy_count"]),
                (14, 36),
            )
            self.assertNotEqual(
                (hybrid["declared_eager_count"], hybrid["declared_lazy_count"]),
                (38, 35),
            )
            self.assertNotEqual(
                (hybrid["declared_eager_count"], hybrid["declared_lazy_count"]),
                (0, 73),
            )
            computed = builder.hybrid_eager_stable_ids(self.policy, group_key)
            self.assertEqual(computed, hybrid["eager_stable_ids"])

    def test_scope_matches_production_lock(self) -> None:
        scope = self.policy["scope"]
        counts = t39.production_group_counts()
        self.assertEqual(t39.production_family_count(), scope["family_count"])
        self.assertEqual(
            counts[t39.SINGLETON_GROUP]["families"],
            scope["singleton_families"],
        )
        self.assertEqual(
            counts[t39.MULTI_GROUP]["families"],
            scope["multi_families"],
        )
        self.assertEqual(
            t39.production_relation_count(),
            scope["production_logical_rows"],
        )
        card = self.policy["publication_group_contract"]["card_aggregate"]
        self.assertEqual(t39.production_relation_count(), card["logical_rows"])
        self.assertEqual(t39.production_family_count(), card["family_count"])

    def test_hybrid_partitions_sum_to_group_logical_rows(self) -> None:
        contract = self.policy["publication_group_contract"]
        singleton = contract["singleton"]["hybrid_boundary"]
        multi = contract["multi"]["hybrid_boundary"]
        counts = t39.production_group_counts()
        self.assertEqual(
            counts[t39.SINGLETON_GROUP]["relations"],
            singleton["declared_eager_count"] + singleton["declared_lazy_count"],
        )
        self.assertEqual(
            counts[t39.MULTI_GROUP]["relations"],
            multi["declared_eager_count"] + multi["declared_lazy_count"],
        )
        card = contract["card_aggregate"]["partitions"]["hybrid"]
        self.assertEqual(t39.production_relation_count(), card[0] + card[1])

    def test_winner_gate_order_forbids_composite_score(self) -> None:
        winner = self.policy["winner_algorithm"]
        self.assertEqual(
            ["correctness", "hard_ceiling", "cumulative_soft_budget"],
            winner["gate_order"],
        )
        self.assertTrue(winner["composite_score_forbidden"])
        self.assertTrue(winner["per_group_winners_required"])
        self.assertTrue(winner["card_aggregate_required"])
        self.assertEqual(
            [
                "eager_margin",
                "retained_memory",
                "reload_index",
                "lookup",
                "sync",
                "implementation_complexity",
            ],
            winner["ranking_order_after_all_gates_pass"],
        )

    def test_opening_matches_t38_t14_closing(self) -> None:
        live = builder.t39_opening()
        opening = self.policy["t39_opening"]
        self.assertEqual(3664, opening["datapack_authored_entries"]["closing"])
        self.assertEqual(16626, opening["eager_publication_rows"]["closing"])
        self.assertEqual(2334, opening["lazy_logical_rows"]["closing"])
        self.assertEqual([], opening["pending_runtime_axes"])
        self.assertFalse(opening["pending_runtime_axes_zero_fill"])
        self.assertEqual(live["sync_bytes"], opening["sync_bytes"]["closing"])
        self.assertEqual(live["lookup_p95_ns"], opening["lookup_p95_ns"]["closing"])

    def test_does_not_modify_records_t37_and_t38_partitions(self) -> None:
        blocked = self.policy["scope"]["does_not_modify"]
        self.assertTrue(any("T37 14/36/8" in item for item in blocked))
        self.assertTrue(any("T38 0/73/16" in item for item in blocked))


class T39MaterializationBuilderTest(unittest.TestCase):
    def test_check_fails_closed_when_measurements_missing(self) -> None:
        if builder.MEASUREMENTS.is_file():
            self.skipTest("measurements already present")
        errors = builder.check()
        self.assertTrue(errors)
        self.assertTrue(
            any("missing measurements" in error for error in errors),
            errors,
        )

    def test_build_raises_when_measurements_missing(self) -> None:
        if builder.MEASUREMENTS.is_file():
            self.skipTest("measurements already present")
        with self.assertRaises(FileNotFoundError):
            builder.build()

    @unittest.skipUnless(
        builder.MEASUREMENTS.is_file(),
        "measurements not generated yet",
    )
    def test_measured_logical_rows_include_both_groups_and_card(
        self,
    ) -> None:
        policy = builder.load(builder.POLICY)
        measurement = builder.load(builder.MEASUREMENTS)
        builder.validate_measurement(measurement, policy)
        counts = t39.production_group_counts()
        self.assertEqual(
            sorted([
                counts[t39.SINGLETON_GROUP]["relations"],
                counts[t39.MULTI_GROUP]["relations"],
                t39.production_relation_count(),
            ]),
            sorted(measurement["input"]["measured_logical_rows"]),
        )
        scenario_ids = {row["id"] for row in measurement["scenarios"]}
        self.assertEqual({"singleton", "multi", "card"}, scenario_ids)

    @unittest.skipUnless(
        builder.MEASUREMENTS.is_file(),
        "measurements not generated yet",
    )
    def test_derived_decision_exposes_group_and_card_winners(self) -> None:
        policy = builder.load(builder.POLICY)
        measurement = builder.load(builder.MEASUREMENTS)
        decision = builder.derive_decision(measurement, policy)
        self.assertFalse(decision["composite_score_used"])
        self.assertIn("singleton", decision["group_winners"])
        self.assertIn("multi", decision["group_winners"])
        self.assertIsNotNone(decision["card_aggregate_winner"])
        for scenario_id in builder.SCENARIO_IDS:
            scenario = decision["scenarios"][scenario_id]
            self.assertEqual(list(builder.CANDIDATES), scenario["survivors"])
            self.assertIsNotNone(scenario["production_winner"])
        artifact = builder.build(measurement, policy)
        self.assertEqual(artifact["decision"], decision)


if __name__ == "__main__":
    unittest.main()
