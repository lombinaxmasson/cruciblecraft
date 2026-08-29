"""Contract tests for the isolated T40 GameTest receipt gate."""
from __future__ import annotations

import sys
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_t40_gametest_receipt as builder  # noqa: E402
from tools import t35_common as t35  # noqa: E402
from tools import t40_common as t40  # noqa: E402


class T40GameTestReceiptTest(unittest.TestCase):
    def test_source_markers_are_not_sufficient_without_pass_receipt(self) -> None:
        self.assertTrue(t40.player_gametest_source_present())
        self.assertEqual(t40.GAME_TEST_REQUIRED, len(t40.discovered_t40_gametest_ids()))
        errors = t40.gametest_receipt_errors({
            "bound_artifacts": t40.bound_gametest_artifacts(),
            "command": ".\\gradlew.bat runGameTestServer --no-daemon",
            "failed": 0,
            "java_sha256": t35.sha256_file(t40.GAME_TEST_JAVA),
            "java_source": t40.relative(t40.GAME_TEST_JAVA),
            "log_fingerprint": "deadbeef",
            "log_path": "run-t40-recipes/logs/latest.log",
            "namespace": t40.GAME_TEST_NAMESPACE,
            "pass_marker": t40.GAME_TEST_PASS_MARKER,
            "passed": t40.GAME_TEST_REQUIRED,
            "required_tests": t40.GAME_TEST_REQUIRED,
            "schema_version": 3,
            "skip_is_not_pass": True,
            "status": "PASS",
            "test_ids": t40.discovered_t40_gametest_ids(),
        })
        self.assertTrue(any("Pt40Recipes" in error for error in errors))
        self.assertTrue(
            any("committed evidence file" in error for error in errors),
            errors,
        )

    def test_failing_or_missing_log_is_not_pass(self) -> None:
        parsed = t40.parse_gametest_log(
            "0 GAME TESTS COMPLETE\n1 required tests failed"
        )
        self.assertEqual("FAIL", parsed["status"])
        with self.assertRaises(ValueError):
            builder.write(ROOT / "does-not-exist-t40-gametest.log")

    def test_utf16_logs_are_readable_and_normalized(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "latest.log"
            marker = t40.GAME_TEST_PASS_MARKER
            path.write_bytes(f"{marker}\r\n".encode("utf-16"))
            parsed = t40.parse_gametest_log(t40.read_gametest_log(path))
            self.assertEqual("PASS", parsed["status"])
            self.assertEqual(t40.GAME_TEST_REQUIRED, parsed["passed"])
            normalized = t40.normalize_gametest_log_text(t40.read_gametest_log(path))
            self.assertEqual(f"{marker}\n", normalized)
            self.assertNotIn("\r", normalized)

    def test_run_directory_log_is_not_receipt_evidence(self) -> None:
        errors = t40.gametest_receipt_errors({
            "bound_artifacts": t40.bound_gametest_artifacts(),
            "command": t40.GAME_TEST_COMMAND,
            "failed": 0,
            "java_sha256": t35.sha256_file(t40.GAME_TEST_JAVA),
            "java_source": t40.relative(t40.GAME_TEST_JAVA),
            "log_fingerprint": "deadbeef",
            "log_path": "run-t40-recipes/logs/latest.log",
            "namespace": t40.GAME_TEST_NAMESPACE,
            "pass_marker": t40.GAME_TEST_PASS_MARKER,
            "passed": t40.GAME_TEST_REQUIRED,
            "required_tests": t40.GAME_TEST_REQUIRED,
            "schema_version": 3,
            "skip_is_not_pass": True,
            "status": "PASS",
            "test_ids": t40.discovered_t40_gametest_ids(),
        })
        self.assertTrue(any("committed evidence file" in error for error in errors), errors)
