"""Contract tests for the isolated T41 GameTest receipt gate."""
from __future__ import annotations

import sys
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_t41_gametest_receipt as builder  # noqa: E402
from tools import t35_common as t35  # noqa: E402
from tools import t41_common as t41  # noqa: E402


class T41GameTestReceiptTest(unittest.TestCase):
    def test_source_markers_are_not_sufficient_without_pass_receipt(self) -> None:
        self.assertTrue(t41.player_gametest_source_present())
        self.assertEqual(t41.GAME_TEST_REQUIRED, len(t41.discovered_t41_gametest_ids()))
        errors = t41.gametest_receipt_errors({
            "bound_artifacts": t41.bound_gametest_artifacts(),
            "command": ".\\gradlew.bat runGameTestServer --no-daemon",
            "failed": 0,
            "java_sha256": t35.sha256_file(t41.GAME_TEST_JAVA),
            "java_source": t41.relative(t41.GAME_TEST_JAVA),
            "log_fingerprint": "deadbeef",
            "log_path": "run-t41-recipes/logs/latest.log",
            "namespace": t41.GAME_TEST_NAMESPACE,
            "pass_marker": t41.GAME_TEST_PASS_MARKER,
            "passed": t41.GAME_TEST_REQUIRED,
            "required_tests": t41.GAME_TEST_REQUIRED,
            "schema_version": 3,
            "skip_is_not_pass": True,
            "status": "PASS",
            "test_ids": t41.discovered_t41_gametest_ids(),
        })
        self.assertTrue(any("Pt41Recipes" in error for error in errors))
        self.assertTrue(
            any("committed evidence file" in error for error in errors),
            errors,
        )

    def test_failing_or_missing_log_is_not_pass(self) -> None:
        parsed = t41.parse_gametest_log(
            "0 GAME TESTS COMPLETE\n1 required tests failed"
        )
        self.assertEqual("FAIL", parsed["status"])
        with self.assertRaises(ValueError):
            builder.write(ROOT / "does-not-exist-t41-gametest.log")

    def test_utf16_logs_are_readable_and_normalized(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "latest.log"
            marker = t41.GAME_TEST_PASS_MARKER
            path.write_bytes(f"{marker}\r\n".encode("utf-16"))
            parsed = t41.parse_gametest_log(t41.read_gametest_log(path))
            self.assertEqual("PASS", parsed["status"])
            self.assertEqual(t41.GAME_TEST_REQUIRED, parsed["passed"])
            normalized = t41.normalize_gametest_log_text(t41.read_gametest_log(path))
            self.assertEqual(f"{marker}\n", normalized)
            self.assertNotIn("\r", normalized)

    def test_run_directory_log_is_not_receipt_evidence(self) -> None:
        errors = t41.gametest_receipt_errors({
            "bound_artifacts": t41.bound_gametest_artifacts(),
            "command": t41.GAME_TEST_COMMAND,
            "failed": 0,
            "java_sha256": t35.sha256_file(t41.GAME_TEST_JAVA),
            "java_source": t41.relative(t41.GAME_TEST_JAVA),
            "log_fingerprint": "deadbeef",
            "log_path": "run-t41-recipes/logs/latest.log",
            "namespace": t41.GAME_TEST_NAMESPACE,
            "pass_marker": t41.GAME_TEST_PASS_MARKER,
            "passed": t41.GAME_TEST_REQUIRED,
            "required_tests": t41.GAME_TEST_REQUIRED,
            "schema_version": 3,
            "skip_is_not_pass": True,
            "status": "PASS",
            "test_ids": t41.discovered_t41_gametest_ids(),
        })
        self.assertTrue(any("committed evidence file" in error for error in errors), errors)
