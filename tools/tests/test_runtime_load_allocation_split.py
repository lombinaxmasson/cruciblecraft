#!/usr/bin/env python3
"""v3 load-policy split and fail-closed window tests."""
from __future__ import annotations

import unittest

from tools import forward_v3_budget_decision as v3
from tools import t35_common as t35


class RuntimeLoadAllocationSplitTest(unittest.TestCase):
    def test_v3_policy_splits_allocation_and_stays_blocking(self) -> None:
        policy = v3.load_policy()
        self.assertNotIn("allocation_bytes", policy["budgets"])
        self.assertEqual(
            "HARD",
            v3.axis_verdict(policy["budgets"]["reload_transient_allocation_bytes"]),
        )
        self.assertEqual(
            "HARD",
            v3.axis_verdict(policy["budgets"]["lookup_allocation_bytes_per_operation"]),
        )
        self.assertEqual(
            "HARD",
            v3.axis_verdict(policy["budgets"]["retained_memory_bytes"]),
        )
        self.assertEqual(
            t35.sha256_file(t35.TOOLS / "t14_load_budget_policy.v2.json"),
            policy["v2_base"]["file_sha256"],
        )

    def test_pending_and_zero_fill_fail_closed(self) -> None:
        policy = v3.load_policy()
        pending = v3.classify_actuals({}, policy=policy)
        self.assertTrue(pending.eliminates)
        zero = v3.classify_actuals(
            {axis: 0 for axis in v3.REQUIRED_V3_AXES},
            zero_filled=v3.REQUIRED_V3_AXES,
            policy=policy,
        )
        self.assertTrue(zero.eliminates)
        with self.assertRaises(v3.ForwardV3BudgetError):
            v3.classify_actuals({"allocation_bytes": 1}, policy=policy)

    def test_measured_lookup_zero_is_not_zero_fill(self) -> None:
        policy = v3.load_policy()
        opening = {
            "reload_transient_allocation_bytes": 687226880,
            "lookup_allocation_bytes_per_operation": 0,
            "retained_memory_bytes": 5021175,
            "sync_bytes": 5021175,
            "server_reload_ms": 459,
            "client_reload_ms": 429,
            "server_index_ms": 7,
            "client_index_ms": 8,
            "lookup_p95_ns": 214887,
            "lookup_candidate_count": 10,
            "eager_publication_rows": 14,
            "lazy_logical_rows": 50652,
            "lazy_cache_ceiling_rows": 876,
            "datapack_authored_entries": 6269,
        }
        result = v3.classify_actuals(
            opening,
            measured={axis: True for axis in opening},
            policy=policy,
        )
        self.assertFalse(result.eliminates)
        self.assertEqual("PASS", result.axes["lookup_allocation_bytes_per_operation"].status)


if __name__ == "__main__":
    unittest.main()
