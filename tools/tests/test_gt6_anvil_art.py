#!/usr/bin/env python3
"""GT6 anvil voxels use shared iconsets, not misc-tool cubes."""
from __future__ import annotations

import unittest

from tools import gt6_anvil_art as art


class Gt6AnvilArtTest(unittest.TestCase):
    def test_voxels_reuse_iconsets_and_skip_misc_tool_cube(self) -> None:
        self.assertEqual([], art.check())
        rows = art.anvil_rows()
        self.assertGreater(len(rows), 0)
        paths = {str(row["dummy_path"]) for row in rows}
        self.assertIn("stone/anvil", paths)
        self.assertIn("bronze/anvil", paths)
        self.assertIn("ironwood/anvil", paths)
        self.assertEqual("stone", art.iconset_for("stone/anvil"))
        self.assertEqual("wood", art.iconset_for("ironwood/anvil"))
        self.assertEqual("metallic", art.iconset_for("bronze/anvil"))


if __name__ == "__main__":
    unittest.main()
