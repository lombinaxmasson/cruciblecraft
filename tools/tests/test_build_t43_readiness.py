"""Contract tests for T43 readiness accounting."""
from __future__ import annotations

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_t43_readiness as builder  # noqa: E402


class T43ReadinessTest(unittest.TestCase):
    def test_failed_gates_match_derived_gates(self) -> None:
        document = builder.build()
        failed = sorted(
            name
            for name, ok in document["gates"].items()
            if name != "catalog_fixture_not_production_evidence" and not ok
        )
        self.assertEqual(failed, document["failed_gates"])
        expected = "T43_READY" if not failed else "T43_BLOCKED"
        self.assertEqual(expected, document["status"])
        self.assertTrue(document["t42_history_readonly"])
        self.assertEqual("T44", document["t44_opening"]["next_issue_id"])
        self.assertFalse(document["t44_opening"]["preassigned_host"])
        self.assertFalse(document["t44_opening"]["preassigned_family_ids"])
