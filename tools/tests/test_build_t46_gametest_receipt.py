"""Contract tests for the isolated T46 GameTest receipt gate."""
from __future__ import annotations

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import t35_common as t35  # noqa: E402
from tools import t46_common as t46  # noqa: E402


class T46GameTestReceiptTest(unittest.TestCase):
    def test_source_markers_are_not_sufficient_without_pass_receipt(self) -> None:
        t46.refresh_gametest_required()
        self.assertTrue(t46.player_gametest_source_present())
        self.assertEqual(t46.GAME_TEST_REQUIRED, len(t46.discovered_gametest_ids()))
        self.assertGreaterEqual(t46.GAME_TEST_REQUIRED, 8)
        errors = t46.gametest_receipt_errors({
            "bound_artifacts": t46.bound_gametest_artifacts(),
            "command": ".\\gradlew.bat runGameTestServer --no-daemon",
            "failed": 0,
            "java_sha256": t35.sha256_file(t46.GAME_TEST_JAVA),
            "java_source": t46.relative(t46.GAME_TEST_JAVA),
            "log_fingerprint": "deadbeef",
            "log_path": "run-t46-recipes/logs/latest.log",
            "namespace": t46.GAME_TEST_NAMESPACE,
            "pass_marker": t46.GAME_TEST_PASS_MARKER,
            "passed": t46.GAME_TEST_REQUIRED,
            "required_tests": t46.GAME_TEST_REQUIRED,
            "schema_version": 3,
            "skip_is_not_pass": True,
            "status": "PASS",
            "test_ids": t46.discovered_gametest_ids(),
        })
        self.assertTrue(any("Pt46Recipes" in error for error in errors))
        self.assertIn("-Pt46Recipes", t46.GAME_TEST_COMMAND)
        self.assertTrue(any("log_path" in error for error in errors))

    def test_locked_support_bind_is_the_recipe_tree_not_the_empty_stub(self) -> None:
        t46.refresh_gametest_required()
        self.assertNotEqual(t46.LOCKED_SUPPORT_ROOT, t46.LOCKED_SUPPORT_RECIPE_ROOT)
        self.assertTrue(t46.locked_support_tree_current())
        bound = t46.bound_gametest_artifacts()
        recipe_tree = t46.tree_sha256(t46.LOCKED_SUPPORT_RECIPE_ROOT)
        self.assertEqual(recipe_tree, bound["t46_locked_support"])
        self.assertNotEqual(t46.EMPTY_TREE_SHA256, bound["t46_locked_support"])
        self.assertNotEqual(
            t46.tree_sha256(t46.LOCKED_SUPPORT_ROOT),
            bound["t46_locked_support"],
        )
        fake = {
            "bound_artifacts": {
                **bound,
                "t46_locked_support": t46.EMPTY_TREE_SHA256,
            },
            "command": t46.GAME_TEST_COMMAND,
            "failed": 0,
            "java_sha256": t35.sha256_file(t46.GAME_TEST_JAVA),
            "java_source": t46.relative(t46.GAME_TEST_JAVA),
            "log_fingerprint": "deadbeef",
            "log_path": t46.relative(t46.GAME_TEST_EVIDENCE_LOG),
            "namespace": t46.GAME_TEST_NAMESPACE,
            "pass_marker": t46.GAME_TEST_PASS_MARKER,
            "passed": t46.GAME_TEST_REQUIRED,
            "required_tests": t46.GAME_TEST_REQUIRED,
            "schema_version": 3,
            "skip_is_not_pass": True,
            "status": "PASS",
            "test_ids": t46.discovered_gametest_ids(),
        }
        errors = t46.gametest_receipt_errors(fake)
        self.assertTrue(
            any("empty tree" in error for error in errors),
            errors,
        )
