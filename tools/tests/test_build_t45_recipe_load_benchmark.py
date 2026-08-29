"""Contract tests for the T45 materialization policy and decision builder."""
from __future__ import annotations

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_t45_recipe_load_benchmark as builder  # noqa: E402
from tools import t45_common as t45  # noqa: E402


class T45MaterializationPolicyTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        if not builder.POLICY.is_file():
            raise unittest.SkipTest("T45 materialization policy is not on disk yet")
        cls.policy = builder.load(builder.POLICY)

    def test_policy_declares_two_hosts_and_three_candidates(self) -> None:
        builder.validate_policy(self.policy)
        for group_key, rows in (("smelter", 271), ("drying", 108)):
            hybrid = self.policy["publication_group_contract"][group_key]["hybrid_boundary"]
            self.assertTrue(hybrid["declared_before_measurement"])
            self.assertEqual(
                (hybrid["declared_eager_count"], hybrid["declared_lazy_count"]),
                (0, rows),
            )
            self.assertNotEqual(
                (hybrid["declared_eager_count"], hybrid["declared_lazy_count"]),
                (14, 36),
            )
            self.assertEqual(
                [0, rows, min(24, rows)],
                self.policy["publication_group_contract"][group_key]["partitions"]["on_demand"],
            )
        self.assertEqual([], self.policy["winner_algorithm"]["gate_order"])
        self.assertEqual(379, self.policy["scope"]["family_count"])
        self.assertEqual(
            t45.production_lock_sha256(),
            self.policy["scope"]["production_lock_sha256"],
        )
        card = self.policy["publication_group_contract"]["card_aggregate"]
        self.assertEqual(379, card["logical_rows"])
        self.assertEqual([0, 379, 24], card["partitions"]["on_demand"])
        self.assertEqual([0, 379, 24], card["partitions"]["hybrid"])
