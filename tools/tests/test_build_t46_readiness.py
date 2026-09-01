"""Contract tests for T46 readiness accounting."""
from __future__ import annotations

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_t46_readiness as builder  # noqa: E402
from tools import t46_common as t46  # noqa: E402


class T46ReadinessTest(unittest.TestCase):
    def test_failed_gates_match_derived_gates(self) -> None:
        document = builder.build()
        failed = sorted(name for name, ok in document["gates"].items() if not ok)
        self.assertEqual(failed, document["failed_gates"])
        expected = "T46_READY" if not failed else "T46_BLOCKED"
        self.assertEqual(expected, document["status"])
        self.assertTrue(document["t45_history_readonly"])
        self.assertEqual("T47", document["t47_opening"]["next_issue_id"])
        self.assertFalse(document["t47_opening"]["preassigned_host"])
        self.assertFalse(document["t47_opening"]["preassigned_family_ids"])
        self.assertTrue(document["gates"]["no_t46_java_historical_whitelist"])
        self.assertTrue(document["gates"]["centrifuge_premerge_whitelist_unchanged"])
        self.assertIn("production_denominator_803_1517_118", document["gates"])
        self.assertIn("v2_load_policy_pinned", document["gates"])
        self.assertIn("java_locked_fields_asserted", document["gates"])
        self.assertIn("locked_support_tree_current", document["gates"])
        self.assertEqual(
            t46.T14_CLOSING_BASIS,
            document["t47_opening"]["t14_closing_basis"],
        )
