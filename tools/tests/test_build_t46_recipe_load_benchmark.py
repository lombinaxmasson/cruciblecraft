"""Contract tests for the T46 materialization policy and decision builder."""
from __future__ import annotations

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_t46_recipe_load_benchmark as builder  # noqa: E402
from tools import t46_common as t46  # noqa: E402


class T46MaterializationPolicyTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        if not builder.POLICY.is_file():
            raise unittest.SkipTest("T46 materialization policy is not on disk yet")
        cls.policy = builder.load(builder.POLICY)

    def test_policy_declares_one_bath_group_and_three_candidates(self) -> None:
        builder.validate_policy(self.policy)
        hybrid = self.policy["publication_group_contract"]["bath_mte"]["hybrid_boundary"]
        self.assertTrue(hybrid["declared_before_measurement"])
        self.assertEqual(
            (hybrid["declared_eager_count"], hybrid["declared_lazy_count"]),
            (0, t46.EXPECTED_RELATION_COUNT),
        )
        self.assertNotEqual(
            (hybrid["declared_eager_count"], hybrid["declared_lazy_count"]),
            (14, 36),
        )
        self.assertEqual(
            [0, t46.EXPECTED_RELATION_COUNT, t46.CACHE_CEILING],
            self.policy["publication_group_contract"]["bath_mte"]["partitions"]["on_demand"],
        )
        self.assertEqual([], self.policy["winner_algorithm"]["gate_order"])
        self.assertEqual(t46.EXPECTED_FAMILY_COUNT, self.policy["scope"]["family_count"])
        self.assertEqual(
            t46.EXPECTED_RELATION_COUNT,
            self.policy["scope"]["production_logical_rows"],
        )
        self.assertEqual(
            t46.production_lock_sha256(),
            self.policy["scope"]["production_lock_sha256"],
        )
        card = self.policy["publication_group_contract"]["card_aggregate"]
        self.assertEqual(t46.EXPECTED_RELATION_COUNT, card["logical_rows"])
        self.assertEqual(
            [0, t46.EXPECTED_RELATION_COUNT, t46.CACHE_CEILING],
            card["partitions"]["on_demand"],
        )
        self.assertEqual(
            [0, t46.EXPECTED_RELATION_COUNT, t46.CACHE_CEILING],
            card["partitions"]["hybrid"],
        )

    def test_v2_authored_overage_does_not_eliminate_winner(self) -> None:
        if not builder.MEASUREMENTS.is_file():
            raise unittest.SkipTest("T46 measurements are not on disk yet")
        measurement = builder.load(builder.MEASUREMENTS)
        scenario = next(
            row
            for row in measurement["scenarios"]
            if row.get("id") == "card" and row.get("scale") == "1x"
        )
        over = builder._derive_scenario_decision(
            "card",
            scenario,
            self.policy,
            family_delta=1835,
            complete=True,
        )
        authored = (
            self.policy["t46_opening"]["datapack_authored_entries"]["closing"] + 1835
        )
        self.assertGreater(authored, self.policy["hard_limits_1x"]["datapack_authored_entries"])
        self.assertIn(over["production_winner"], {"immediate", "on_demand", "hybrid"})
        self.assertTrue(over["survivors"])
        for reasons in over["eliminated"].values():
            self.assertFalse(
                any("datapack_authored_entries" in reason for reason in reasons)
            )
