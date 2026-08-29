"""Contract tests for the isolated T38 GameTest receipt gate."""
from __future__ import annotations

import sys
import tempfile
import unittest
from pathlib import Path
from unittest import mock

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_t38_gametest_receipt as builder  # noqa: E402
from tools import t35_common as t35  # noqa: E402
from tools import t38_common as t38  # noqa: E402


class T38GameTestReceiptTest(unittest.TestCase):
    def test_source_markers_are_not_sufficient_without_pass_receipt(self) -> None:
        self.assertTrue(t38.player_gametest_source_present())
        self.assertEqual(5, len(t38.discovered_t38_gametest_ids()))
        errors = t38.gametest_receipt_errors({
            "bound_artifacts": t38.bound_gametest_artifacts(),
            "command": ".\\gradlew.bat runGameTestServer --no-daemon",
            "failed": 0,
            "java_sha256": t35.sha256_file(t38.GAME_TEST_JAVA),
            "java_source": t38.relative(t38.GAME_TEST_JAVA),
            "log_fingerprint": "deadbeef",
            "log_path": "run-t38-recipes/logs/latest.log",
            "namespace": t38.GAME_TEST_NAMESPACE,
            "pass_marker": t38.GAME_TEST_PASS_MARKER,
            "passed": 5,
            "required_tests": 5,
            "schema_version": 3,
            "skip_is_not_pass": True,
            "status": "PASS",
            "test_ids": t38.discovered_t38_gametest_ids(),
        })
        self.assertTrue(any("Pt38Recipes" in error for error in errors))
        self.assertTrue(
            any("committed evidence file" in error for error in errors),
            errors,
        )

    def test_failing_or_missing_log_is_not_pass(self) -> None:
        parsed = t38.parse_gametest_log(
            "0 GAME TESTS COMPLETE\n1 required tests failed"
        )
        self.assertEqual("FAIL", parsed["status"])
        with self.assertRaises(ValueError):
            builder.write(ROOT / "does-not-exist-t38-gametest.log")

    def test_utf16_logs_are_readable_and_normalized(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "latest.log"
            path.write_bytes("All 5 required tests passed :)\r\n".encode("utf-16"))
            parsed = t38.parse_gametest_log(t38.read_gametest_log(path))
            self.assertEqual("PASS", parsed["status"])
            self.assertEqual(5, parsed["passed"])
            normalized = t38.normalize_gametest_log_text(t38.read_gametest_log(path))
            self.assertEqual("All 5 required tests passed :)\n", normalized)
            self.assertNotIn("\r", normalized)

    def test_run_directory_log_is_not_receipt_evidence(self) -> None:
        errors = t38.gametest_receipt_errors({
            "bound_artifacts": t38.bound_gametest_artifacts(),
            "command": t38.GAME_TEST_COMMAND,
            "failed": 0,
            "java_sha256": t35.sha256_file(t38.GAME_TEST_JAVA),
            "java_source": t38.relative(t38.GAME_TEST_JAVA),
            "log_fingerprint": "deadbeef",
            "log_path": "run-t38-recipes/logs/latest.log",
            "namespace": t38.GAME_TEST_NAMESPACE,
            "pass_marker": t38.GAME_TEST_PASS_MARKER,
            "passed": 5,
            "required_tests": 5,
            "schema_version": 3,
            "skip_is_not_pass": True,
            "status": "PASS",
            "test_ids": t38.discovered_t38_gametest_ids(),
        })
        self.assertTrue(
            any("committed evidence file" in error for error in errors),
            errors,
        )

    def test_check_does_not_require_gitignored_run_log(self) -> None:
        with mock.patch.object(
            t38,
            "GAME_TEST_RUN_LOG_CANDIDATES",
            (ROOT / "does-not-exist-run-t38" / "logs" / "latest.log",),
        ):
            self.assertEqual([], builder.check())
            self.assertTrue(t38.player_gametest_present())

    def test_bound_artifacts_include_gate_and_recipe_trees(self) -> None:
        bound = t38.bound_gametest_artifacts()
        self.assertTrue(bound["gametest_java"])
        self.assertTrue(bound["material_registration_gate_java"])
        self.assertTrue(bound["material_registration_gate_json"])
        self.assertTrue(bound["t38_generated_recipes"])
        self.assertTrue(bound["t38_player_path_recovery"])

    def test_committed_receipt_matches_java_and_t38recipes_command(self) -> None:
        self.assertEqual([], builder.check())
        self.assertTrue(t38.player_gametest_present())
        receipt = t38.load_json(t38.GAME_TEST_RECEIPT)
        self.assertEqual("PASS", receipt["status"])
        self.assertEqual(3, receipt["schema_version"])
        self.assertIn("-Pt38Recipes", receipt["command"])
        self.assertEqual("cruciblecraft_t38", receipt["namespace"])
        self.assertEqual(5, receipt["passed"])
        self.assertEqual(0, receipt["failed"])
        self.assertTrue(receipt["skip_is_not_pass"])
        self.assertEqual(t38.discovered_t38_gametest_ids(), receipt["test_ids"])
        self.assertEqual(t35.sha256_file(t38.GAME_TEST_JAVA), receipt["java_sha256"])
        self.assertEqual(t38.bound_gametest_artifacts(), receipt["bound_artifacts"])
        self.assertEqual("tools/t38_gametest.log", receipt["log_path"])
        self.assertTrue(t38.GAME_TEST_EVIDENCE_LOG.is_file())
        evidence = t38.normalize_gametest_log_text(
            t38.read_gametest_log(t38.GAME_TEST_EVIDENCE_LOG)
        )
        self.assertEqual(t38.log_fingerprint(evidence), receipt["log_fingerprint"])
        self.assertEqual("PASS", t38.parse_gametest_log(evidence)["status"])
        self.assertNotIn("run-t38-recipes", receipt["log_path"])


if __name__ == "__main__":
    unittest.main()
