#!/usr/bin/env python3
"""Contract tests for the GT6 building-block catalog."""
from __future__ import annotations

import importlib.util
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


def _load_catalog():
    path = ROOT / "tools/waves/content/gt6-building-blocks/catalog.py"
    spec = importlib.util.spec_from_file_location("gt6_building_block_catalog", path)
    if spec is None or spec.loader is None:
        raise ImportError(f"cannot load {path}")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


building = _load_catalog()


class GtBuildingBlocksTest(unittest.TestCase):
    def test_catalog_matches_committed_json(self) -> None:
        self.assertEqual([], building.check())

    def test_counts_glass_glow_and_promoted_leftovers(self) -> None:
        rows = building.identities()
        self.assertEqual(building.VARIANT_COUNT, len(rows))
        leftover = building.leftover_block_rows()
        self.assertEqual(building.PROMOTED_LEFTOVER_COUNT, len(leftover))
        paths = {str(row["registry_path"]) for row in leftover}
        self.assertIn("asphalt/white/slab_up", paths)
        self.assertIn("diggable/mud", paths)
        self.assertNotIn("lilypad_glowtus/white_glowtus", paths)
        runtimes = [str(row["runtime_id"]) for row in rows]
        self.assertEqual(len(rows), len(set(runtimes)))
        self.assertIn("cruciblecraft:glass/black", runtimes)
        self.assertIn("cruciblecraft:glow_glass/white/slab_east", runtimes)
        self.assertIn("cruciblecraft:diggable/turf", runtimes)


if __name__ == "__main__":
    unittest.main()
