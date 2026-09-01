#!/usr/bin/env python3
"""Contract tests for the T47 declared-before-measure policy."""
from __future__ import annotations

import unittest

from tools import build_t47_materialization_policy as policy
from tools import t47_common as common


class T47MaterializationPolicyTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.document = policy.build()
        cls.contract = cls.document["publication_group_contract"]

    def test_policy_is_t47_specific_not_copied_t46(self) -> None:
        self.assertEqual("T47_MATERIALIZATION_CANDIDATE_POLICY", self.document["status"])
        exact = self.contract["exact"]["hybrid_boundary"]
        multi = self.contract["exact_multi"]["hybrid_boundary"]
        card = self.contract["card_aggregate"]["hybrid_boundary"]
        self.assertEqual({"duration_ticks_lte": common.HYBRID_CUTOFF}, exact["eager_selector"])
        self.assertEqual(common.HYBRID_CUTOFF, multi["eager_selector"]["duration_ticks_lte"])
        self.assertGreater(exact["declared_eager_count"], 0)
        self.assertGreater(multi["declared_eager_count"], 0)
        self.assertNotEqual(24, exact["cache_ceiling"])
        self.assertNotEqual(24, multi["cache_ceiling"])
        self.assertLessEqual(exact["cache_ceiling"], common.CACHE_CEILING)
        self.assertEqual(common.CACHE_CEILING, multi["cache_ceiling"])
        self.assertEqual(common.CACHE_CEILING, card["cache_ceiling"])
        self.assertTrue(exact["declared_before_measurement"])
        markers = " ".join(self.document["scope"]["does_not_modify"])
        self.assertIn("T46 cache 24", markers)
        self.assertIn("duration_ticks_lte 0", markers)

    def test_forward_v2_soft_budgets_warn_and_authored_is_report_only(self) -> None:
        winner = self.document["winner_algorithm"]
        self.assertTrue(winner["soft_budget_does_not_eliminate"])
        self.assertEqual("REPORT_ONLY", winner["authored_entries_verdict"])
        self.assertNotIn("max_interval_soft_budget_does_not_eliminate", winner)
        self.assertEqual(
            "REPORT_ONLY",
            self.document["readiness_verdicts_1x"]["datapack_authored_entries"],
        )
        self.assertEqual(128, self.document["hard_limits_1x"]["lookup_candidate_count"])
        self.assertEqual(64, self.document["soft_budgets_1x"]["lookup_candidate_count"])
        self.assertEqual(16000, self.document["soft_budgets_1x"]["lazy_logical_rows"])
        self.assertEqual(56000, self.document["hard_limits_1x"]["lazy_logical_rows"])
        bind = self.document["load_budget_policy_v2"]
        self.assertEqual("tools/t14_load_budget_policy.v2.json", bind["path"])
        self.assertEqual(64, len(bind["sha256"]))

    def test_partitions_match_declared_eager_lazy_cache(self) -> None:
        for key in ("exact", "exact_multi", "card_aggregate"):
            row = self.contract[key]
            hybrid = row["hybrid_boundary"]
            eager, lazy, cache = row["partitions"]["hybrid"]
            self.assertEqual(hybrid["declared_eager_count"], eager)
            self.assertEqual(hybrid["declared_lazy_count"], lazy)
            self.assertEqual(hybrid["cache_ceiling"], cache)
            self.assertEqual(eager + lazy, row["logical_rows"])


if __name__ == "__main__":
    unittest.main()
