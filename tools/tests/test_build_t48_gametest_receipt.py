"""Contract tests for the isolated T48 GameTest receipt gate."""
from __future__ import annotations

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import t35_common as t35  # noqa: E402
from tools import t48_common as t48  # noqa: E402


class T48GameTestReceiptTest(unittest.TestCase):
    def test_source_markers_are_not_sufficient_without_pass_receipt(self) -> None:
        t48.refresh_gametest_required()
        self.assertTrue(t48.player_gametest_source_present())
        self.assertEqual(t48.GAME_TEST_REQUIRED, len(t48.discovered_gametest_ids()))
        self.assertGreaterEqual(t48.GAME_TEST_REQUIRED, 10)
        errors = t48.gametest_receipt_errors({
            "bound_artifacts": t48.bound_gametest_artifacts(),
            "command": ".\\gradlew.bat runGameTestServer --no-daemon",
            "failed": 0,
            "java_sha256": t35.sha256_file(t48.GAME_TEST_JAVA),
            "java_source": t48.relative(t48.GAME_TEST_JAVA),
            "log_fingerprint": "deadbeef",
            "log_path": "run-t48-recipes/logs/latest.log",
            "namespace": t48.GAME_TEST_NAMESPACE,
            "pass_marker": t48.GAME_TEST_PASS_MARKER,
            "passed": t48.GAME_TEST_REQUIRED,
            "required_tests": t48.GAME_TEST_REQUIRED,
            "schema_version": 3,
            "skip_is_not_pass": True,
            "status": "PASS",
            "test_ids": t48.discovered_gametest_ids(),
        })
        self.assertTrue(any("Pt48Recipes" in error for error in errors))
        self.assertIn("-Pt48Recipes", t48.GAME_TEST_COMMAND)
        self.assertTrue(any("log_path" in error for error in errors))

    def test_locked_support_bind_is_the_recipe_tree_not_the_empty_stub(self) -> None:
        t48.refresh_gametest_required()
        self.assertNotEqual(t48.LOCKED_SUPPORT_ROOT, t48.LOCKED_SUPPORT_RECIPE_ROOT)
        self.assertTrue(t48.locked_support_tree_current())
        bound = t48.bound_gametest_artifacts()
        recipe_tree = t48.tree_sha256(t48.LOCKED_SUPPORT_RECIPE_ROOT)
        self.assertEqual(recipe_tree, bound["t48_locked_support"])
        self.assertNotEqual(t48.EMPTY_TREE_SHA256, bound["t48_locked_support"])
        fake = {
            "bound_artifacts": {
                **bound,
                "t48_locked_support": t48.EMPTY_TREE_SHA256,
            },
            "command": t48.GAME_TEST_COMMAND,
            "failed": 0,
            "java_sha256": t35.sha256_file(t48.GAME_TEST_JAVA),
            "java_source": t48.relative(t48.GAME_TEST_JAVA),
            "log_fingerprint": "deadbeef",
            "log_path": t48.relative(t48.GAME_TEST_EVIDENCE_LOG),
            "namespace": t48.GAME_TEST_NAMESPACE,
            "pass_marker": t48.GAME_TEST_PASS_MARKER,
            "passed": t48.GAME_TEST_REQUIRED,
            "required_tests": t48.GAME_TEST_REQUIRED,
            "schema_version": 3,
            "skip_is_not_pass": True,
            "status": "PASS",
            "test_ids": t48.discovered_gametest_ids(),
        }
        errors = t48.gametest_receipt_errors(fake)
        self.assertTrue(
            any("empty tree" in error for error in errors),
            errors,
        )


if __name__ == "__main__":
    unittest.main()
