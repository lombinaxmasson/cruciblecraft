"""Contract tests for the isolated T43 GameTest receipt gate."""
from __future__ import annotations

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import t35_common as t35  # noqa: E402
from tools import t43_common as t43  # noqa: E402


class T43GameTestReceiptTest(unittest.TestCase):
    def test_source_markers_are_not_sufficient_without_pass_receipt(self) -> None:
        t43.refresh_gametest_required()
        self.assertTrue(t43.player_gametest_source_present())
        self.assertEqual(t43.GAME_TEST_REQUIRED, len(t43.discovered_t43_gametest_ids()))
        self.assertGreaterEqual(t43.GAME_TEST_REQUIRED, 6)
        errors = t43.gametest_receipt_errors({
            "bound_artifacts": t43.bound_gametest_artifacts(),
            "command": ".\\gradlew.bat runGameTestServer --no-daemon",
            "failed": 0,
            "java_sha256": t35.sha256_file(t43.GAME_TEST_JAVA),
            "java_source": t43.relative(t43.GAME_TEST_JAVA),
            "log_fingerprint": "deadbeef",
            "log_path": "run-t43-recipes/logs/latest.log",
            "namespace": t43.GAME_TEST_NAMESPACE,
            "pass_marker": t43.GAME_TEST_PASS_MARKER,
            "passed": t43.GAME_TEST_REQUIRED,
            "required_tests": t43.GAME_TEST_REQUIRED,
            "schema_version": 3,
            "skip_is_not_pass": True,
            "status": "PASS",
            "test_ids": t43.discovered_t43_gametest_ids(),
        })
        self.assertTrue(any("Pt43Recipes" in error for error in errors))
        self.assertIn("-Pt43Recipes", t43.GAME_TEST_COMMAND)
