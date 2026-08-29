"""Contract tests for the isolated T39 GameTest receipt gate."""
from __future__ import annotations

import sys
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_t39_gametest_receipt as builder  # noqa: E402
from tools import t35_common as t35  # noqa: E402
from tools import t39_common as t39  # noqa: E402


class T39GameTestReceiptTest(unittest.TestCase):
    def test_source_markers_are_not_sufficient_without_pass_receipt(self) -> None:
        self.assertTrue(t39.player_gametest_source_present())
        self.assertEqual(t39.GAME_TEST_REQUIRED, len(t39.discovered_t39_gametest_ids()))
        errors = t39.gametest_receipt_errors({
            "bound_artifacts": t39.bound_gametest_artifacts(),
            "command": ".\\gradlew.bat runGameTestServer --no-daemon",
            "failed": 0,
            "java_sha256": t35.sha256_file(t39.GAME_TEST_JAVA),
            "java_source": t39.relative(t39.GAME_TEST_JAVA),
            "log_fingerprint": "deadbeef",
            "log_path": "run-t39-recipes/logs/latest.log",
            "namespace": t39.GAME_TEST_NAMESPACE,
            "pass_marker": t39.GAME_TEST_PASS_MARKER,
            "passed": t39.GAME_TEST_REQUIRED,
            "required_tests": t39.GAME_TEST_REQUIRED,
            "schema_version": 3,
            "skip_is_not_pass": True,
            "status": "PASS",
            "test_ids": t39.discovered_t39_gametest_ids(),
        })
        self.assertTrue(any("Pt39Recipes" in error for error in errors))
        self.assertTrue(
            any("committed evidence file" in error for error in errors),
            errors,
        )

    def test_failing_or_missing_log_is_not_pass(self) -> None:
        parsed = t39.parse_gametest_log(
            "0 GAME TESTS COMPLETE\n1 required tests failed"
        )
        self.assertEqual("FAIL", parsed["status"])
        with self.assertRaises(ValueError):
            builder.write(ROOT / "does-not-exist-t39-gametest.log")

    def test_utf16_logs_are_readable_and_normalized(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "latest.log"
            marker = t39.GAME_TEST_PASS_MARKER
            path.write_bytes(f"{marker}\r\n".encode("utf-16"))
            parsed = t39.parse_gametest_log(t39.read_gametest_log(path))
            self.assertEqual("PASS", parsed["status"])
            self.assertEqual(t39.GAME_TEST_REQUIRED, parsed["passed"])
            normalized = t39.normalize_gametest_log_text(t39.read_gametest_log(path))
            self.assertEqual(f"{marker}\n", normalized)
            self.assertNotIn("\r", normalized)

    def test_run_directory_log_is_not_receipt_evidence(self) -> None:
        errors = t39.gametest_receipt_errors({
            "bound_artifacts": t39.bound_gametest_artifacts(),
            "command": t39.GAME_TEST_COMMAND,
            "failed": 0,
            "java_sha256": t35.sha256_file(t39.GAME_TEST_JAVA),
            "java_source": t39.relative(t39.GAME_TEST_JAVA),
            "log_fingerprint": "deadbeef",
            "log_path": "run-t39-recipes/logs/latest.log",
            "namespace": t39.GAME_TEST_NAMESPACE,
            "pass_marker": t39.GAME_TEST_PASS_MARKER,
            "passed": t39.GAME_TEST_REQUIRED,
            "required_tests": t39.GAME_TEST_REQUIRED,
            "schema_version": 3,
            "skip_is_not_pass": True,
            "status": "PASS",
            "test_ids": t39.discovered_t39_gametest_ids(),
        })
        self.assertTrue(
            any("committed evidence file" in error for error in errors),
            errors,
        )

    def test_check_and_presence_agree_on_current_receipt(self) -> None:
        errors = builder.check()
        self.assertEqual(not errors, t39.player_gametest_present())

    def test_bound_artifacts_include_gate_and_recipe_trees(self) -> None:
        bound = t39.bound_gametest_artifacts()
        self.assertTrue(bound["gametest_java"])
        self.assertTrue(bound["material_registration_gate_java"])
        self.assertTrue(bound["material_registration_gate_json"])
        self.assertTrue(bound["t39_generated_recipes"])
        self.assertTrue(bound["t39_locked_support"])
        self.assertTrue(bound["production_lock"])
        self.assertTrue(bound["runtime_dependency_manifest"])
        self.assertTrue(bound["publication_group_manifest"])
        self.assertTrue(bound["shard_manifest"])


if __name__ == "__main__":
    unittest.main()
