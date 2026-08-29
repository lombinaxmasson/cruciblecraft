"""Contract tests for the T38 materialization policy and decision builder."""
from __future__ import annotations

import json
import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_t38_recipe_load_benchmark as builder  # noqa: E402


class T38MaterializationPolicyTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.policy = builder.load(builder.POLICY)

    def test_policy_declares_all_three_candidates_before_measuring(self) -> None:
        builder.validate_policy(self.policy)
        self.assertEqual(
            ["immediate", "on_demand", "hybrid"],
            self.policy["candidate_contract"]["candidates"],
        )
        hybrid = self.policy["hybrid_boundary"]
        self.assertTrue(hybrid["declared_before_measurement"])
        self.assertIsNone(hybrid["hot_modulo"])
        self.assertEqual(5, hybrid["forbidden_extruder_hot_modulo"])
        self.assertEqual(16, hybrid["cache_ceiling"])
        self.assertEqual(38, hybrid["declared_eager_count"])
        self.assertEqual(35, hybrid["declared_lazy_count"])
        self.assertEqual(38, len(hybrid["eager_stable_ids"]))
        self.assertNotIn("HOT_MODULO", json.dumps(hybrid))
        self.assertNotIn('"hot_modulo": 5', json.dumps(hybrid))
        self.assertNotIn(14, (hybrid["declared_eager_count"], hybrid["declared_lazy_count"]))
        self.assertNotIn(36, (hybrid["declared_eager_count"], hybrid["declared_lazy_count"]))
        self.assertNotIn("512", json.dumps({
            key: hybrid[key]
            for key in hybrid
            if key != "justification"
        }))
        caches = self.policy["candidate_contract"]["cache_policies"]
        self.assertNotIn("cache ceiling 8", json.dumps(caches))

    def test_seventy_three_logical_rows_and_work_set_excludes_0002(self) -> None:
        scope = self.policy["scope"]
        self.assertEqual(29, scope["family_count"])
        self.assertEqual(73, scope["production_logical_rows"])
        self.assertEqual(73, self.policy["scales"][0]["logical_rows"])
        self.assertNotIn("#0002", scope["work_set"])
        self.assertIn("#0000", scope["work_set"])
        self.assertIn("#0029", scope["work_set"])

    def test_hybrid_eager_plus_lazy_equals_seventy_three(self) -> None:
        hybrid = self.policy["hybrid_boundary"]
        self.assertEqual(
            73,
            hybrid["declared_eager_count"] + hybrid["declared_lazy_count"],
        )
        if builder.T38_SOURCE.is_file():
            source = builder.load(builder.T38_SOURCE)
            computed = builder.hybrid_eager_stable_ids(source, self.policy)
            self.assertEqual(38, len(computed))
            self.assertEqual(computed, hybrid["eager_stable_ids"])

    def test_correctness_gates_precede_ranking(self) -> None:
        winner = self.policy["winner_algorithm"]
        self.assertEqual(
            ["correctness", "hard_ceiling", "cumulative_soft_budget"],
            winner["gate_order"],
        )
        self.assertEqual(
            [
                "field_equivalence",
                "full_enumeration",
                "client_consistency",
                "player_execution",
            ],
            winner["correctness_gates"],
        )
        self.assertTrue(winner["composite_score_forbidden"])
        self.assertEqual("1x", winner["ranking_scale"])
        self.assertEqual(73, winner["ranking_logical_rows"])
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

    def test_opening_matches_t37_census_closing(self) -> None:
        live = builder.t37_opening()
        opening = self.policy["t38_opening"]
        self.assertEqual(3616, opening["datapack_authored_entries"]["closing"])
        self.assertEqual(16611, opening["eager_publication_rows"]["closing"])
        self.assertEqual(2261, opening["lazy_logical_rows"]["closing"])
        self.assertEqual(6600, opening["datapack_authored_entries"]["hard_ceiling"])
        self.assertEqual(21000, opening["eager_publication_rows"]["hard_ceiling"])
        self.assertEqual(56000, opening["lazy_logical_rows"]["hard_ceiling"])
        self.assertEqual([], opening["pending_runtime_axes"])
        self.assertFalse(opening["pending_runtime_axes_zero_fill"])
        self.assertEqual(live["sync_bytes"], opening["sync_bytes"]["closing"])
        self.assertEqual(live["lookup_p95_ns"], opening["lookup_p95_ns"]["closing"])


class T38MaterializationBuilderTest(unittest.TestCase):
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
    def test_one_x_logical_rows_appear_in_measured_logical_rows(self) -> None:
        policy = builder.load(builder.POLICY)
        measurement = builder.load(builder.MEASUREMENTS)
        builder.validate_measurement(measurement, policy)
        self.assertEqual(
            [73],
            measurement["input"]["measured_logical_rows"],
        )
        self.assertEqual(73, measurement["input"]["logical_rows"])
        self.assertEqual(73, measurement["scenarios"][0]["logical_rows"])

    @unittest.skipUnless(
        builder.MEASUREMENTS.is_file(),
        "measurements not generated yet",
    )
    def test_derived_decision_is_recomputable(self) -> None:
        policy = builder.load(builder.POLICY)
        measurement = builder.load(builder.MEASUREMENTS)
        decision = builder.derive_decision(measurement, policy)
        self.assertFalse(decision["composite_score_used"])
        self.assertEqual(73, decision["ranking_logical_rows"])
        self.assertEqual(
            ["immediate", "on_demand", "hybrid"],
            decision["survivors"],
        )
        self.assertEqual("on_demand", decision["production_winner"])
        self.assertNotEqual("hybrid", decision["production_winner"])
        for candidate, warnings in decision["soft_budget_warnings"].items():
            self.assertTrue(
                all("lookup_candidate_count" in warning for warning in warnings),
                (candidate, warnings),
            )
        artifact = builder.build(measurement, policy)
        self.assertEqual(
            artifact["decision"],
            decision,
        )


if __name__ == "__main__":
    unittest.main()
