"""Contract tests for T48 readiness accounting."""
from __future__ import annotations

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_t48_readiness as builder  # noqa: E402
from tools import t48_common as t48  # noqa: E402


class T48ReadinessTest(unittest.TestCase):
    def test_failed_gates_match_derived_gates(self) -> None:
        document = builder.build()
        failed = sorted(name for name, ok in document["gates"].items() if not ok)
        self.assertEqual(failed, document["failed_gates"])
        expected = "T48_READY" if not failed else "T48_BLOCKED"
        self.assertEqual(expected, document["status"])
        self.assertTrue(document.get("t47_history_readonly"))
        self.assertEqual("T49", document["t48_opening"]["next_issue_id"])
        self.assertFalse(document["t48_opening"]["preassigned_host"])
        self.assertFalse(document["t48_opening"]["preassigned_family_ids"])
        self.assertTrue(document["gates"]["no_t48_java_historical_whitelist"])
        self.assertTrue(document["gates"]["centrifuge_premerge_whitelist_unchanged"])
        self.assertIn("production_denominator_145_34091_3532", document["gates"])
        self.assertIn("v2_load_policy_pinned", document["gates"])
        self.assertIn("java_locked_fields_asserted", document["gates"])
        self.assertIn("locked_support_tree_current", document["gates"])
        self.assertEqual(
            t48.T14_CLOSING_BASIS,
            document["t48_opening"]["t14_closing_basis"],
        )


if __name__ == "__main__":
    unittest.main()
