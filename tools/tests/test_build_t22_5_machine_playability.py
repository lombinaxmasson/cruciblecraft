"""Tests for the T22.5 C0 per-machine playability audit builder."""
from __future__ import annotations

import json
import sys
import unittest
from pathlib import Path

TOOLS = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(TOOLS))

import build_t22_5_machine_playability as builder  # noqa: E402


class T225MachinePlayabilityTest(unittest.TestCase):

    def test_committed_artifact_is_current(self) -> None:
        if not builder.OUTPUT.is_file():
            self.skipTest("artifact not yet generated")
        self.assertEqual(builder.check(), [])

    def test_all_32_maps_land_in_a_state(self) -> None:
        document = json.loads(
            builder.OUTPUT.read_text(encoding="utf-8")
        )
        records = document["records"]
        self.assertEqual(len(records), 32)
        self.assertEqual(
            {r["status"] for r in records},
            {"registered_playable", "registered_zero_logical"},
        )
        for row in records:
            self.assertTrue(row["v1_role"])

    def test_zero_logical_is_derived(self) -> None:
        document = json.loads(
            builder.OUTPUT.read_text(encoding="utf-8")
        )
        for row in document["records"]:
            if row["status"] == "registered_zero_logical":
                self.assertEqual(row["authored_gt_recipes"], 0)
                self.assertEqual(row["material_rule_files"], 0)
                self.assertEqual(row["rule_expanded_from_manifests"], 0)
                self.assertEqual(row["logical_lower_bound"], 0)
                self.assertTrue(row["blocker"])
                self.assertTrue(row["owner"])
            else:
                self.assertGreater(
                    row["authored_gt_recipes"]
                    + row["material_rule_files"]
                    + row["rule_expanded_from_manifests"],
                    0,
                )

    def test_blockers_are_the_bend_maps_and_retired_cooling(self) -> None:
        document = json.loads(
            builder.OUTPUT.read_text(encoding="utf-8")
        )
        self.assertEqual(
            document["counts"]["blockers"],
            [
                "cruciblecraft:anvil_bend_big",
                "cruciblecraft:anvil_bend_small",
                "cruciblecraft:cooling",
            ],
        )

    def test_authored_scan_matches_known_counts(self) -> None:
        authored, rules = builder.scan_authored()
        self.assertEqual(authored["cruciblecraft:crusher"], 494)
        self.assertEqual(authored["cruciblecraft:centrifuge"], 371)
        self.assertEqual(authored["cruciblecraft:sluice"], 357)
        self.assertGreater(rules["cruciblecraft:anvil"], 0)
        self.assertEqual(rules.get("cruciblecraft:cooling", 0), 0)

    def test_runtime_guard_game_test_exists(self) -> None:
        path = (
            builder.ROOT
            / "src/main/java/com/masson/cruciblecraft/gametest/"
            "CrucibleCraftGameTests.java"
        )
        source = path.read_text(encoding="utf-8")
        self.assertIn("t22_5RegisteredMapsHaveLogicalRecipes", source)


if __name__ == "__main__":
    unittest.main()
