#!/usr/bin/env python3
"""Jade observation matrix contract tests."""
from __future__ import annotations

import unittest

from tools import io_common as io
from tools import jade_observation as jade

ACTIVE = io.ROOT / "docs" / "history" / "card-plans" / "active"
CLOSED = io.ROOT / "docs" / "history" / "card-plans" / "closed"
PLAN = CLOSED / "生成资源注册与Jade第一切片详细计划.md"


class JadeObservationTest(unittest.TestCase):
    def test_matrix_records_foundation_plus_followup_before_slice(self) -> None:
        manifest = jade.compile_manifest()
        self.assertEqual([], manifest["errors"], manifest["errors"])
        self.assertEqual("PASS", manifest["status"])
        self.assertEqual("foundation_plus_followup", manifest["jade_scope_decision"])
        self.assertGreaterEqual(manifest["family_count"], 18)

    def test_crucible_and_transformer_are_the_ready_slice(self) -> None:
        matrix = jade.load_json(jade.MATRIX)
        by_name = {row["family"]: row for row in matrix["families"]}
        self.assertEqual("ready", by_name["crucible"]["status"])
        self.assertEqual("ready", by_name["transformer"]["status"])
        self.assertIn("temperature_k", by_name["crucible"]["display_sections"])
        self.assertIn("per_side_voltage", by_name["transformer"]["display_sections"])
        follow = [
            row["family"]
            for row in matrix["families"]
            if row["status"] == "follow_up"
        ]
        self.assertIn("fusion_reactor", follow)
        self.assertIn("reactor_core", follow)
        self.assertIn("logistics_core", follow)

    def test_closed_plan_stays_archived(self) -> None:
        names = sorted(path.name for path in ACTIVE.iterdir() if path.is_file())
        self.assertNotIn("生成资源注册与Jade第一切片详细计划.md", names)
        self.assertTrue(PLAN.is_file())
        text = PLAN.read_text(encoding="utf-8")
        self.assertIn("presentation/live-art-jade", text)
        self.assertIn("foundation_plus_followup", text)
        self.assertIn("battery_cell_crafting", text)
        self.assertIn("unique_active_wave", text)


if __name__ == "__main__":
    unittest.main()
