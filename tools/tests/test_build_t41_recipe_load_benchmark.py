"""Contract tests for the T41 materialization policy and decision builder."""
from __future__ import annotations

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_t41_recipe_load_benchmark as builder  # noqa: E402
from tools import t41_common as t41  # noqa: E402


class T41MaterializationPolicyTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.policy = builder.load(builder.POLICY)

    def test_policy_declares_three_groups_and_three_candidates(self) -> None:
        builder.validate_policy(self.policy)
        contract = self.policy["publication_group_contract"]
        self.assertEqual(
            ["immediate", "on_demand", "hybrid"],
            self.policy["candidate_contract"]["candidates"],
        )
        for group_key in ("planks", "fireproof", "planks2"):
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
            self.assertEqual(16, contract[group_key]["partitions"]["on_demand"][2])
        self.assertEqual(48, contract["card_aggregate"]["partitions"]["on_demand"][2])

    def test_scope_matches_production_lock(self) -> None:
        scope = self.policy["scope"]
        counts = t41.production_group_counts()
        self.assertEqual(t41.production_family_count(), scope["family_count"])
        self.assertEqual(t41.production_relation_count(), scope["production_logical_rows"])
        self.assertEqual(
            counts[t41.PLANKS_GROUP]["families"],
            scope["planks_families"],
        )
        self.assertEqual(
            counts[t41.FIREPROOF_GROUP]["families"],
            scope["fireproof_families"],
        )
        self.assertEqual(
            counts[t41.PLANKS2_GROUP]["families"],
            scope["planks2_families"],
        )
        self.assertEqual(t41.production_lock_sha256(), scope["production_lock_sha256"])
        self.assertTrue(any("T40 cache 11" in item for item in scope["does_not_modify"]))

    def test_opening_is_t40_closing(self) -> None:
        opening = self.policy["t41_opening"]
        live = builder.t41_opening()
        self.assertEqual(t41.T14_OPENING_LOAD_SOURCE, opening["source"])
        self.assertEqual(live["datapack_authored_entries"], opening["datapack_authored_entries"]["closing"])
        self.assertFalse(opening["pending_runtime_axes_zero_fill"])

    def test_decision_is_blocked_until_measurements_exist(self) -> None:
        strategy = t41.production_strategy()
        if not t41.MEASUREMENTS.is_file():
            self.assertTrue(strategy["blocked"])
