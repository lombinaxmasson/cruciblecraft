#!/usr/bin/env python3
"""Axis-level contract tests for forward-v2 budget classification."""
from __future__ import annotations

import unittest

from tools import forward_v2_budget_decision as v2_budget


class ForwardV2BudgetDecisionTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.limits = v2_budget.limit_maps()

    def test_v2_policy_keeps_authored_report_only_and_lookup_hard_128(self) -> None:
        self.assertEqual("REPORT_ONLY", self.limits["verdicts"]["datapack_authored_entries"])
        self.assertEqual(6000, self.limits["soft"]["datapack_authored_entries"])
        self.assertEqual(6600, self.limits["hard"]["datapack_authored_entries"])
        self.assertEqual(16000, self.limits["soft"]["lazy_logical_rows"])
        self.assertEqual(56000, self.limits["hard"]["lazy_logical_rows"])
        self.assertEqual(64, self.limits["soft"]["lookup_candidate_count"])
        self.assertEqual(128, self.limits["hard"]["lookup_candidate_count"])
        self.assertEqual(134217728, self.limits["soft"]["allocation_bytes"])
        self.assertEqual(536870912, self.limits["hard"]["allocation_bytes"])
        self.assertEqual("HARD", self.limits["verdicts"]["lazy_logical_rows"])
        self.assertEqual("HARD", self.limits["verdicts"]["lookup_candidate_count"])

    def test_soft_overage_warns_and_does_not_eliminate(self) -> None:
        result = v2_budget.classify_actuals(
            {
                "lazy_logical_rows": 16466,
                "allocation_bytes": 176405576,
                "lookup_candidate_count": 100,
            }
        )
        self.assertFalse(result.eliminates)
        self.assertEqual([], result.eliminate_reasons)
        self.assertIn("soft_budget:lazy_logical_rows:16466>16000", result.soft_warnings)
        self.assertIn(
            "soft_budget:allocation_bytes:176405576>134217728",
            result.soft_warnings,
        )
        self.assertIn(
            "soft_budget:lookup_candidate_count:100>64",
            result.soft_warnings,
        )
        self.assertEqual([], result.report_only_warnings)

    def test_authored_soft_and_hard_overage_are_report_only(self) -> None:
        soft = v2_budget.classify_actuals({"datapack_authored_entries": 6101})
        self.assertFalse(soft.eliminates)
        self.assertEqual([], soft.eliminate_reasons)
        self.assertEqual(
            ["report_only:soft_budget:datapack_authored_entries:6101>6000"],
            soft.report_only_warnings,
        )
        hard = v2_budget.classify_actuals({"datapack_authored_entries": 6706})
        self.assertFalse(hard.eliminates)
        self.assertEqual([], hard.eliminate_reasons)
        self.assertEqual(
            [
                "report_only:hard_ceiling:datapack_authored_entries:6706>6600",
                "report_only:soft_budget:datapack_authored_entries:6706>6000",
            ],
            hard.report_only_warnings,
        )

    def test_non_authored_hard_ceiling_eliminates(self) -> None:
        hybrid = v2_budget.classify_actuals({"lookup_candidate_count": 383})
        self.assertTrue(hybrid.eliminates)
        self.assertEqual(
            ["hard_ceiling:lookup_candidate_count:383>128"],
            hybrid.eliminate_reasons,
        )
        immediate = v2_budget.classify_actuals(
            {
                "lookup_candidate_count": 1341,
                "lookup_p95_ns": 3020568,
            }
        )
        self.assertEqual(
            [
                "hard_ceiling:lookup_candidate_count:1341>128",
                "hard_ceiling:lookup_p95_ns:3020568>2000000",
            ],
            immediate.eliminate_reasons,
        )
        self.assertEqual([], immediate.soft_warnings)

    def test_pending_missing_axis_eliminates_only_when_declared(self) -> None:
        skipped = v2_budget.classify_actuals({"lazy_logical_rows": 100})
        self.assertFalse(skipped.eliminates)
        self.assertNotIn("lookup_p95_ns", skipped.axes)
        pending = v2_budget.classify_actuals(
            {"lazy_logical_rows": 100},
            pending_if_missing=("lookup_p95_ns",),
        )
        self.assertTrue(pending.eliminates)
        self.assertEqual(["measurement_unavailable:lookup_p95_ns"], pending.eliminate_reasons)


if __name__ == "__main__":
    unittest.main()
