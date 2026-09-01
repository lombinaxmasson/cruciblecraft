#!/usr/bin/env python3
"""Contract tests for T47 materialization winner derivation."""
from __future__ import annotations

import unittest

from tools import build_t47_materialization_policy as policy_builder
from tools import build_t47_recipe_load_benchmark as builder
from tools import t47_common as t47


class T47RecipeLoadBenchmarkTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        if not builder.MEASUREMENTS.is_file():
            raise unittest.SkipTest("T47 measurements are not on disk yet")
        cls.policy = policy_builder.build()
        cls.measurement = builder.load(builder.MEASUREMENTS)

    def _scenario(self, scenario_id: str) -> dict:
        return next(
            row
            for row in self.measurement["scenarios"]
            if row.get("id") == scenario_id and row.get("scale") == "1x"
        )

    def test_soft_budget_does_not_eliminate_on_demand(self) -> None:
        exact_multi = builder._derive_scenario_decision(
            "exact_multi",
            self._scenario("exact_multi"),
            self.policy,
            family_delta=builder.EXACT_MULTI_FAMILIES,
            complete=True,
        )
        card = builder._derive_scenario_decision(
            "card",
            self._scenario("card"),
            self.policy,
            family_delta=builder.CARD_FAMILIES,
            complete=True,
        )
        self.assertEqual("on_demand", exact_multi["production_winner"])
        self.assertEqual(["on_demand"], exact_multi["survivors"])
        self.assertIn(
            "soft_budget:lazy_logical_rows:16277>16000",
            exact_multi["soft_budget_warnings"]["on_demand"],
        )
        self.assertEqual("on_demand", card["production_winner"])
        self.assertEqual(["on_demand"], card["survivors"])
        self.assertIn(
            "soft_budget:lazy_logical_rows:16466>16000",
            card["soft_budget_warnings"]["on_demand"],
        )
        self.assertIn(
            "soft_budget:allocation_bytes:176405576>134217728",
            card["soft_budget_warnings"]["on_demand"],
        )
        self.assertEqual(
            ["report_only:soft_budget:datapack_authored_entries:6101>6000"],
            card["report_only_warnings"]["on_demand"],
        )
        for reasons in card["eliminated"].values():
            self.assertFalse(any("datapack_authored_entries" in reason for reason in reasons))
            self.assertFalse(any(reason.startswith("soft_budget:") for reason in reasons))

    def test_authored_hard_reference_overage_is_report_only(self) -> None:
        over = builder._derive_scenario_decision(
            "card",
            self._scenario("card"),
            self.policy,
            family_delta=1000,
            complete=True,
        )
        authored = (
            self.policy["t47_opening"]["datapack_authored_entries"]["closing"] + 1000
        )
        self.assertGreater(authored, self.policy["hard_limits_1x"]["datapack_authored_entries"])
        self.assertEqual("on_demand", over["production_winner"])
        self.assertTrue(over["survivors"])
        self.assertIn(
            f"report_only:hard_ceiling:datapack_authored_entries:{authored}>6600",
            over["report_only_warnings"]["on_demand"],
        )
        for reasons in over["eliminated"].values():
            self.assertFalse(any("datapack_authored_entries" in reason for reason in reasons))

    def test_lookup_hard_ceiling_still_eliminates_eager_paths(self) -> None:
        exact_multi = builder._derive_scenario_decision(
            "exact_multi",
            self._scenario("exact_multi"),
            self.policy,
            family_delta=builder.EXACT_MULTI_FAMILIES,
            complete=True,
        )
        self.assertIn(
            "hard_ceiling:lookup_candidate_count:1341>128",
            exact_multi["eliminated"]["immediate"],
        )
        self.assertIn(
            "hard_ceiling:lookup_candidate_count:383>128",
            exact_multi["eliminated"]["hybrid"],
        )
        self.assertNotIn("on_demand", exact_multi["eliminated"])

    def test_derived_winners_are_on_demand(self) -> None:
        decision = builder.derive_decision(self.measurement, self.policy)
        self.assertEqual("PRODUCTION_WINNER_READY", decision["status"])
        self.assertEqual("on_demand", decision["group_winners"]["exact"])
        self.assertEqual("on_demand", decision["group_winners"]["exact_multi"])
        self.assertEqual(
            "on_demand",
            decision["group_winners"][t47.PUBLICATION_GROUP_EXACT],
        )
        self.assertEqual(
            "on_demand",
            decision["group_winners"][t47.PUBLICATION_GROUP_EXACT_MULTI],
        )
        self.assertEqual("on_demand", decision["card_aggregate_winner"])
        self.assertEqual([], decision["production_blockers"])
        exact = decision["scenarios"]["exact"]
        self.assertEqual("on_demand", exact["production_winner"])
        self.assertIn("on_demand", exact["survivors"])
        self.assertIn("hybrid", exact["survivors"])
        self.assertIn("immediate", exact["survivors"])
        self.assertIn(
            "soft_budget:lookup_p95_ns:1797525>1000000",
            exact["soft_budget_warnings"]["immediate"],
        )

    def test_committed_decision_matches_builder_when_present(self) -> None:
        if not builder.OUTPUT.is_file():
            raise unittest.SkipTest("T47 decision artifact is not on disk yet")
        artifact = builder.load(builder.OUTPUT)
        if artifact.get("status") != "T47_MATERIALIZATION_DECISION_READY":
            raise unittest.SkipTest("T47 decision is still BLOCKED on disk")
        builder.validate_artifact(artifact)
        self.assertEqual("on_demand", artifact["decision"]["card_aggregate_winner"])


if __name__ == "__main__":
    unittest.main()
