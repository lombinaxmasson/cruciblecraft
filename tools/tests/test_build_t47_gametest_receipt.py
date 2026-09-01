"""Contract tests for the isolated T47 GameTest receipt gate."""
from __future__ import annotations

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import t35_common as t35  # noqa: E402
from tools import t47_common as t47  # noqa: E402


class T47GameTestReceiptTest(unittest.TestCase):
    def test_source_markers_are_not_sufficient_without_pass_receipt(self) -> None:
        t47.refresh_gametest_required()
        self.assertTrue(t47.player_gametest_source_present())
        self.assertEqual(t47.GAME_TEST_REQUIRED, len(t47.discovered_gametest_ids()))
        self.assertGreaterEqual(t47.GAME_TEST_REQUIRED, 8)
        errors = t47.gametest_receipt_errors({
            "bound_artifacts": t47.bound_gametest_artifacts(),
            "command": ".\\gradlew.bat runGameTestServer --no-daemon",
            "failed": 0,
            "java_sha256": t35.sha256_file(t47.GAME_TEST_JAVA),
            "java_source": t47.relative(t47.GAME_TEST_JAVA),
            "log_fingerprint": "deadbeef",
            "log_path": "run-t47-recipes/logs/latest.log",
            "namespace": t47.GAME_TEST_NAMESPACE,
            "pass_marker": t47.GAME_TEST_PASS_MARKER,
            "passed": t47.GAME_TEST_REQUIRED,
            "required_tests": t47.GAME_TEST_REQUIRED,
            "schema_version": 3,
            "skip_is_not_pass": True,
            "status": "PASS",
            "test_ids": t47.discovered_gametest_ids(),
        })
        self.assertTrue(any("Pt47Recipes" in error for error in errors))
        self.assertIn("-Pt47Recipes", t47.GAME_TEST_COMMAND)
        self.assertTrue(any("log_path" in error for error in errors))

    def test_locked_support_bind_is_the_recipe_tree_not_the_empty_stub(self) -> None:
        t47.refresh_gametest_required()
        self.assertNotEqual(t47.LOCKED_SUPPORT_ROOT, t47.LOCKED_SUPPORT_RECIPE_ROOT)
        self.assertTrue(t47.locked_support_tree_current())
        bound = t47.bound_gametest_artifacts()
        recipe_tree = t47.tree_sha256(t47.LOCKED_SUPPORT_RECIPE_ROOT)
        self.assertEqual(recipe_tree, bound["t47_locked_support"])
        self.assertNotEqual(t47.EMPTY_TREE_SHA256, bound["t47_locked_support"])
        self.assertNotEqual(
            t47.tree_sha256(t47.LOCKED_SUPPORT_ROOT),
            bound["t47_locked_support"],
        )
        fake = {
            "bound_artifacts": {
                **bound,
                "t47_locked_support": t47.EMPTY_TREE_SHA256,
            },
            "command": t47.GAME_TEST_COMMAND,
            "failed": 0,
            "java_sha256": t35.sha256_file(t47.GAME_TEST_JAVA),
            "java_source": t47.relative(t47.GAME_TEST_JAVA),
            "log_fingerprint": "deadbeef",
            "log_path": t47.relative(t47.GAME_TEST_EVIDENCE_LOG),
            "namespace": t47.GAME_TEST_NAMESPACE,
            "pass_marker": t47.GAME_TEST_PASS_MARKER,
            "passed": t47.GAME_TEST_REQUIRED,
            "required_tests": t47.GAME_TEST_REQUIRED,
            "schema_version": 3,
            "skip_is_not_pass": True,
            "status": "PASS",
            "test_ids": t47.discovered_gametest_ids(),
        }
        errors = t47.gametest_receipt_errors(fake)
        self.assertTrue(
            any("empty tree" in error for error in errors),
            errors,
        )
