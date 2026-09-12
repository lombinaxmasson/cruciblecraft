#!/usr/bin/env python3
"""Generated project-status page is the only dynamic status recap."""
from __future__ import annotations

import unittest

from tools import capability_ledger
from tools import project_status

FOUNDATION = "content/technological-parts-foundation"
TREES = "worldgen/gt-trees"
DUNGEON = "worldgen/gt-dungeon"
SANDING = "machines/sanding"
OVEN = "machines/oven"
SENSORS = "content/sensors"


class ProjectStatusTest(unittest.TestCase):
    def test_render_projects_unique_active_and_player_complete(self) -> None:
        compiled = capability_ledger.compile_ledger()
        text = project_status.render_status(compiled)
        self.assertIn("# 项目状态", text)
        self.assertIn("不要手改", text)
        self.assertIsNone(compiled["unique_active_slug"])
        self.assertIn("`worldgen/gt-trees`", text)
        self.assertIn("`worldgen/gt-dungeon`", text)
        self.assertIn("`machines/sanding`", text)
        self.assertIn("`machines/oven`", text)
        self.assertIn("`content/sensors`", text)
        self.assertIn("`content/electric-wire-cable-mte-fold`", text)
        self.assertIn("`machines/slicer`", text)
        self.assertIn("workflow=accepted", text)
        self.assertIn("machines/cluster-mill", text)
        self.assertIn("machines/roll-former", text)
        self.assertIn("## Prep（不占落地锁）", text)
        self.assertIn("## runtime_ready", text)
        self.assertIn("python tools/close_capability.py", text)

    def test_pointer_files_link_status_and_do_not_recite(self) -> None:
        self.assertEqual([], project_status.pointer_errors())

    def test_committed_status_matches_render(self) -> None:
        self.assertEqual([], project_status.check_status())


if __name__ == "__main__":
    unittest.main()
