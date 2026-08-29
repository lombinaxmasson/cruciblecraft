#!/usr/bin/env python3
"""Regression coverage for the withdrawn broad T39 recovery projection."""
from __future__ import annotations

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
TOOLS = ROOT / "tools"
sys.path.insert(0, str(TOOLS))

import build_t39_player_path_recovery as recovery
import t39_common as t39


class T39PlayerPathRecoveryTests(unittest.TestCase):
    def test_projection_remains_rebuildable_but_non_authoritative(self) -> None:
        document, files, fluid_blockers = recovery.build()
        self.assertEqual(len(files), len(document["routes"]) * 2)
        self.assertEqual(
            fluid_blockers["counts"]["unreachable_fluid_ids"],
            fluid_blockers["counts"]["already_produced_by_cc_recipes"]
            + fluid_blockers["counts"]["truly_unmodeled"],
        )
        self.assertNotEqual(recovery.OUTPUT_ROOT, t39.LOCKED_SUPPORT_ROOT)
        self.assertTrue(recovery.OUTPUT_ROOT.is_relative_to(ROOT / "src/test"))
        self.assertFalse(recovery.OUTPUT_ROOT.is_relative_to(ROOT / "src/main"))

    def test_broad_recovery_tree_is_excluded_from_main_resources(self) -> None:
        build_gradle = (ROOT / "build.gradle").read_text(encoding="utf-8")
        self.assertIn(
            "data/cruciblecraft/recipe/t39_player_path_recovery/**",
            build_gradle,
        )
        self.assertIn("src/t39_support_generated/resources", build_gradle)
        self.assertFalse(any(t39.LEGACY_PLAYER_PATH_RECOVERY_ROOT.glob("*.json")))


if __name__ == "__main__":
    raise SystemExit(unittest.main())
