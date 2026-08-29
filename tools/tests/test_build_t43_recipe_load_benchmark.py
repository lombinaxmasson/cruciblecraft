"""Contract tests for the T43 materialization policy and decision builder."""
from __future__ import annotations

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_t43_recipe_load_benchmark as builder  # noqa: E402
from tools import t43_common as t43  # noqa: E402


class T43MaterializationPolicyTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.policy = builder.load(builder.POLICY)

    def test_policy_declares_stone_group_and_three_candidates(self) -> None:
        builder.validate_policy(self.policy)
        hybrid = self.policy["publication_group_contract"]["stone"]["hybrid_boundary"]
        self.assertTrue(hybrid["declared_before_measurement"])
        self.assertEqual(
            (hybrid["declared_eager_count"], hybrid["declared_lazy_count"]),
            (0, 407),
        )
        self.assertNotEqual(
            (hybrid["declared_eager_count"], hybrid["declared_lazy_count"]),
            (14, 36),
        )
        self.assertNotEqual(
            (hybrid["declared_eager_count"], hybrid["declared_lazy_count"]),
            (0, 73),
        )
        self.assertEqual(24, hybrid["cache_ceiling"])
        self.assertEqual(
            [0, 407, 24],
            self.policy["publication_group_contract"]["stone"]["partitions"]["on_demand"],
        )
        self.assertEqual([], self.policy["winner_algorithm"]["gate_order"])
        self.assertEqual(t43.PRODUCTION_FAMILY_COUNT, self.policy["scope"]["family_count"])
        self.assertEqual(
            t43.production_lock_sha256(),
            self.policy["scope"]["production_lock_sha256"],
        )
