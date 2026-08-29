"""Contract tests for T45 readiness accounting."""
from __future__ import annotations

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_t45_readiness as builder  # noqa: E402


class T45ReadinessTest(unittest.TestCase):
    def test_failed_gates_match_derived_gates(self) -> None:
        document = builder.build()
        failed = sorted(name for name, ok in document["gates"].items() if not ok)
        self.assertEqual(failed, document["failed_gates"])
        expected = "T45_READY" if not failed else "T45_BLOCKED"
        self.assertEqual(expected, document["status"])
        self.assertTrue(document["t44_history_readonly"])
        self.assertEqual("T46", document["t46_opening"]["next_issue_id"])
        self.assertFalse(document["t46_opening"]["preassigned_host"])
        self.assertFalse(document["t46_opening"]["preassigned_family_ids"])
        self.assertTrue(document["gates"]["no_t45_java_historical_whitelist"])
        self.assertTrue(document["gates"]["centrifuge_premerge_whitelist_unchanged"])
