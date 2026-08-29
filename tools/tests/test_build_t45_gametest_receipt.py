"""Contract tests for the isolated T45 GameTest receipt gate."""
from __future__ import annotations

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import t35_common as t35  # noqa: E402
from tools import t45_common as t45  # noqa: E402


class T45GameTestReceiptTest(unittest.TestCase):
    def test_source_markers_are_not_sufficient_without_pass_receipt(self) -> None:
        t45.refresh_gametest_required()
        self.assertTrue(t45.player_gametest_source_present())
        self.assertEqual(t45.GAME_TEST_REQUIRED, len(t45.discovered_gametest_ids()))
        self.assertGreaterEqual(t45.GAME_TEST_REQUIRED, 6)
        errors = t45.gametest_receipt_errors({
            "bound_artifacts": t45.bound_gametest_artifacts(),
            "command": ".\\gradlew.bat runGameTestServer --no-daemon",
            "failed": 0,
            "java_sha256": t35.sha256_file(t45.GAME_TEST_JAVA),
            "java_source": t45.relative(t45.GAME_TEST_JAVA),
            "log_fingerprint": "deadbeef",
            "log_path": "run-t45-recipes/logs/latest.log",
            "namespace": t45.GAME_TEST_NAMESPACE,
            "pass_marker": t45.GAME_TEST_PASS_MARKER,
            "passed": t45.GAME_TEST_REQUIRED,
            "required_tests": t45.GAME_TEST_REQUIRED,
            "schema_version": 3,
            "skip_is_not_pass": True,
            "status": "PASS",
            "test_ids": t45.discovered_gametest_ids(),
        })
        self.assertTrue(any("Pt45Recipes" in error for error in errors))
        self.assertIn("-Pt45Recipes", t45.GAME_TEST_COMMAND)
        self.assertTrue(any("log_path" in error for error in errors))
