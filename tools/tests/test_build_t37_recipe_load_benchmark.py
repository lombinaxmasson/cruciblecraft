"""Contract tests for the T37 materialization policy and decision builder."""
from __future__ import annotations

import copy
import json
import sys
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_t37_recipe_load_benchmark as builder  # noqa: E402
from tools import recipe_load_projection as projection  # noqa: E402


class T37MaterializationPolicyTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.policy = builder.load(builder.POLICY)
        cls.measurement = builder.load(builder.MEASUREMENTS)

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
        self.assertEqual(8, hybrid["cache_ceiling"])
        self.assertEqual(14, hybrid["declared_eager_count"])
        self.assertEqual(36, hybrid["declared_lazy_count"])
        self.assertEqual(14, len(hybrid["eager_family_ids"]))
        self.assertNotIn("HOT_MODULO", json.dumps(hybrid))
        self.assertNotIn('"hot_modulo": 5', json.dumps(hybrid))
        self.assertNotIn("512", json.dumps({
            key: hybrid[key]
            for key in hybrid
            if key != "justification"
        }))

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
        self.assertEqual(50, winner["ranking_logical_rows"])
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

    def test_opening_matches_t36_and_pending_axes_are_not_zero_filled(self) -> None:
        live = builder.t36_opening()
        opening = self.policy["t37_opening"]
        self.assertEqual(3566, opening["datapack_authored_entries"]["closing"])
        self.assertEqual(16597, opening["eager_publication_rows"]["closing"])
        self.assertEqual(2225, opening["lazy_logical_rows"]["closing"])
        self.assertEqual(6600, opening["datapack_authored_entries"]["hard_ceiling"])
        self.assertEqual(21000, opening["eager_publication_rows"]["hard_ceiling"])
        self.assertEqual(56000, opening["lazy_logical_rows"]["hard_ceiling"])
        self.assertEqual(live["pending_runtime_axes"], opening["pending_runtime_axes"])
        self.assertFalse(opening["pending_runtime_axes_zero_fill"])
        self.assertTrue(live["pending_runtime_axes"])
        for axis in live["pending_runtime_axes"]:
            self.assertNotEqual(0, axis)


class T37MaterializationBuilderTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.policy = builder.load(builder.POLICY)
        cls.measurement = builder.load(builder.MEASUREMENTS)

    def test_one_x_logical_rows_appear_in_measured_logical_rows(self) -> None:
        builder.validate_measurement(self.measurement, self.policy)
        self.assertEqual(
            [50],
            self.measurement["input"]["measured_logical_rows"],
        )
        self.assertEqual(50, self.measurement["input"]["logical_rows"])
        self.assertEqual(50, self.measurement["scenarios"][0]["logical_rows"])
        self.assertIn(50, self.measurement["input"]["measured_logical_rows"])

    def test_derived_winner_is_hybrid(self) -> None:
        decision = builder.derive_decision(self.measurement, self.policy)
        self.assertEqual("PRODUCTION_WINNER_READY", decision["status"])
        self.assertEqual("hybrid", decision["production_winner"])
        self.assertCountEqual(
            ["immediate", "on_demand", "hybrid"],
            decision["survivors"],
        )
        self.assertEqual({}, decision["eliminated"])
        self.assertEqual(
            ["hybrid", "on_demand", "immediate"],
            [row["candidate"] for row in decision["ranking"]],
        )
        self.assertTrue(all(
            row["eager_margin"] == "tied" for row in decision["ranking"]
        ))
        self.assertFalse(
            any("measurement_unavailable:lazy_cache_ceiling_rows" in reason
                for reasons in decision["eliminated"].values()
                for reason in reasons)
        )
        artifact = builder.build(self.measurement, self.policy)
        self.assertEqual("T37_MATERIALIZATION_DECISION_READY", artifact["status"])
        self.assertEqual(
            "T37_MATERIALIZATION_MEASUREMENT_READY",
            self.measurement["status"],
        )
        self.assertFalse(self.measurement["production_winner_claimed"])

    def test_ranking_uses_declared_order_not_complexity_only(self) -> None:
        decision = builder.derive_decision(self.measurement, self.policy)
        self.assertEqual(
            [
                "eager_margin",
                "retained_memory",
                "reload_index",
                "lookup",
                "sync",
                "implementation_complexity",
            ],
            decision["algorithm"]["ranking_order_after_all_gates_pass"],
        )
        self.assertEqual(0, builder.IMPLEMENTATION_COMPLEXITY["immediate"])
        self.assertEqual(2, builder.IMPLEMENTATION_COMPLEXITY["hybrid"])
        complexity_only = sorted(
            decision["survivors"],
            key=lambda candidate: (
                builder.IMPLEMENTATION_COMPLEXITY[candidate],
                candidate,
            ),
        )
        self.assertEqual("immediate", complexity_only[0])
        self.assertEqual("hybrid", decision["production_winner"])
        hybrid = next(
            row for row in decision["ranking"] if row["candidate"] == "hybrid"
        )
        immediate = next(
            row for row in decision["ranking"] if row["candidate"] == "immediate"
        )
        self.assertLess(
            hybrid["reload_index_p95_ns"]["server_reload"],
            immediate["reload_index_p95_ns"]["server_reload"],
        )

    def test_complexity_only_wins_when_higher_axes_tie(self) -> None:
        mutated = copy.deepcopy(self.measurement)
        immediate = mutated["scenarios"][0]["candidates"][0]
        for candidate in mutated["scenarios"][0]["candidates"][1:]:
            for axis in ("server", "dedicated_client"):
                candidate[axis] = copy.deepcopy(immediate[axis])
            candidate["lookup"] = copy.deepcopy(immediate["lookup"])
            candidate["retained_memory"] = copy.deepcopy(immediate["retained_memory"])
            candidate["sync"] = copy.deepcopy(immediate["sync"])
        decision = builder.derive_decision(mutated, self.policy)
        self.assertCountEqual(
            ["immediate", "on_demand", "hybrid"],
            decision["survivors"],
        )
        self.assertEqual("immediate", decision["production_winner"])

    def test_builder_refuses_hand_filled_winner(self) -> None:
        mutated = copy.deepcopy(self.measurement)
        mutated["production_winner"] = "immediate"
        with self.assertRaises(ValueError):
            builder.validate_measurement(mutated, self.policy)

        claimed = copy.deepcopy(self.measurement)
        claimed["production_winner_claimed"] = True
        with self.assertRaises(ValueError):
            builder.validate_measurement(claimed, self.policy)

        artifact = builder.build(self.measurement, self.policy)
        hand = copy.deepcopy(artifact)
        hand["decision"]["production_winner"] = "immediate"
        with self.assertRaises(ValueError):
            builder.validate_artifact(hand)

    def test_measured_axes_are_not_zero_filled(self) -> None:
        for row in self.measurement["scenarios"][0]["candidates"]:
            self.assertEqual("PASS", row["status"])
            for field in (
                "server",
                "dedicated_client",
                "enumeration",
                "lookup",
            ):
                metric = row[field]
                self.assertEqual("PASS", metric["status"])
                if field in {"server", "dedicated_client"}:
                    self.assertGreater(metric["reload"]["p50_ns"], 0)
                    self.assertGreater(metric["reload"]["p95_ns"], 0)
                    self.assertGreater(metric["index"]["p50_ns"], 0)
                    self.assertGreater(metric["index"]["p95_ns"], 0)
                else:
                    self.assertGreater(metric["p50_ns"], 0)
                    self.assertGreater(metric["p95_ns"], 0)
            self.assertGreater(row["sync"]["bytes"], 0)
            self.assertGreater(row["retained_memory"]["p50_bytes"], 0)
            self.assertFalse(row["retained_memory"]["naive_heap_delta_used"])
            self.assertFalse(row["integrated_client"]["independent_reexpansion"])

        zeroed = copy.deepcopy(self.measurement)
        zeroed["scenarios"][0]["candidates"][0]["retained_memory"]["status"] = builder.PENDING
        zeroed["scenarios"][0]["candidates"][0]["retained_memory"]["p50_bytes"] = 0
        with self.assertRaises(ValueError):
            builder.validate_measurement(zeroed, self.policy)

        zero_sync = copy.deepcopy(self.measurement)
        zero_sync["scenarios"][0]["candidates"][1]["sync"]["status"] = builder.PENDING
        zero_sync["scenarios"][0]["candidates"][1]["sync"]["bytes"] = 0
        with self.assertRaises(ValueError):
            builder.validate_measurement(zero_sync, self.policy)

    def test_diagnostic_scales_must_not_copy_extruder_or_fill_1x(self) -> None:
        diagnostic = self.measurement["diagnostic_scales"]
        self.assertEqual("forbidden", diagnostic["5x"]["production_use"])
        self.assertEqual("forbidden", diagnostic["20x"]["production_use"])
        self.assertEqual(250, diagnostic["5x"]["logical_rows"])
        self.assertEqual(1000, diagnostic["20x"]["logical_rows"])
        self.assertNotEqual(55640, diagnostic["20x"]["logical_rows"])
        self.assertNotEqual(13910, diagnostic["5x"]["logical_rows"])

    def test_integrated_client_is_reuse_not_a_second_sample(self) -> None:
        for row in self.measurement["scenarios"][0]["candidates"]:
            integrated = row["integrated_client"]
            self.assertFalse(integrated["independent_reexpansion"])
            self.assertIn("reuse", integrated["reason"].lower())

    def test_committed_decision_is_derived(self) -> None:
        self.assertEqual([], builder.check())
        artifact = builder.load(builder.OUTPUT)
        self.assertEqual(
            artifact["decision"],
            builder.derive_decision(self.measurement, self.policy),
        )
        self.assertEqual("hybrid", artifact["decision"]["production_winner"])
        self.assertFalse(artifact["decision"]["composite_score_used"])


class T37LoadProjectionPhaseTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.t13 = projection.load(projection.T13_RECIPE_MAPS)
        cls.policy = projection.load(projection.POLICY)
        cls.schema = projection.load(projection.SCHEMA)
        cls.input_path = ROOT / "tools" / "t37_load_projection_input.json"

    def test_delivery_phase_t37_is_accepted(self) -> None:
        document = projection.load(self.input_path)
        self.assertEqual("T37", document["delivery_phase"])
        result = projection.project(
            document,
            t13=self.t13,
            policy=self.policy,
            schema=self.schema,
        )
        self.assertEqual("T37", result["delivery_phase"])
        self.assertEqual(50, result["ledger"]["counts"]["logical_rows"])
        self.assertIn(
            50,
            document["families"][0]["measurement_basis"]["measured_logical_rows"],
        )

    def test_unknown_phase_still_fails_closed(self) -> None:
        document = projection.load(self.input_path)
        document["delivery_phase"] = "T99"
        with self.assertRaises(projection.ProjectionError):
            projection.validate_input(document, self.t13)


class T37BuilderWriteTest(unittest.TestCase):
    def test_check_mode_does_not_rewrite(self) -> None:
        before_decision = builder.OUTPUT.read_bytes()
        before_policy = builder.POLICY.read_bytes()
        before_measurement = builder.MEASUREMENTS.read_bytes()
        self.assertEqual([], builder.check())
        self.assertEqual(before_decision, builder.OUTPUT.read_bytes())
        self.assertEqual(before_policy, builder.POLICY.read_bytes())
        self.assertEqual(before_measurement, builder.MEASUREMENTS.read_bytes())

    def test_write_is_deterministic(self) -> None:
        artifact = builder.build()
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "decision.json"
            path.write_text(builder.stable_json(artifact), encoding="utf-8", newline="\n")
            self.assertEqual(
                builder.stable_json(artifact),
                path.read_text(encoding="utf-8"),
            )


if __name__ == "__main__":
    unittest.main()
