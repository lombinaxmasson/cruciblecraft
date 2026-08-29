"""Contract tests for the T40 materialization policy and decision builder."""
from __future__ import annotations

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_t40_recipe_load_benchmark as builder  # noqa: E402
from tools import t40_common as t40  # noqa: E402


class T40MaterializationPolicyTest(unittest.TestCase):
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
        counts = t40.production_group_counts()
        self.assertEqual(t40.production_family_count(), scope["family_count"])
        self.assertEqual(t40.production_relation_count(), scope["production_logical_rows"])
        self.assertEqual(
            counts[t40.SINGLETON_GROUP]["families"],
            scope["singleton_families"],
        )
        self.assertEqual(
            counts[t40.MULTI_GROUP]["families"],
            scope["multi_families"],
        )
        self.assertEqual(t40.production_lock_sha256(), scope["production_lock_sha256"])
        self.assertTrue(any("T39 19/13" in item for item in scope["does_not_modify"]))

    def test_opening_is_t39_closing(self) -> None:
        opening = self.policy["t40_opening"]
        live = builder.t40_opening()
        self.assertEqual(t40.T14_OPENING_LOAD_SOURCE, opening["source"])
        self.assertEqual(live["datapack_authored_entries"], opening["datapack_authored_entries"]["closing"])
        self.assertFalse(opening["pending_runtime_axes_zero_fill"])

    def test_decision_is_blocked_until_measurements_exist(self) -> None:
        strategy = t40.production_strategy()
        if not t40.MEASUREMENTS.is_file():
            self.assertTrue(strategy["blocked"])
