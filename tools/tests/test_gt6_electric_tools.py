#!/usr/bin/env python3
"""GT6 electric-tool unique-active/paused ledger and art-manifest checks."""
from __future__ import annotations

import hashlib
import json
import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))
if str(ROOT / "tools") not in sys.path:
    sys.path.insert(1, str(ROOT / "tools"))

from tools import capability_ledger as ledger
from tools import census_common as census

SLUG = "content/gt6-electric-tools"
CAPABILITY = census.ROOT / "tools" / "capabilities" / SLUG / "capability.json"
PLAN_ACTIVE = (
    census.ROOT / "docs" / "history" / "card-plans" / "active"
    / "GT6电动工具详细计划.md"
)
PLAN_CLOSED = (
    census.ROOT / "docs" / "history" / "card-plans" / "closed"
    / "GT6电动工具详细计划.md"
)
MANIFEST = (
    census.ROOT
    / "src"
    / "main"
    / "resources"
    / "assets"
    / "cruciblecraft"
    / "gt6_electric_tools_art_manifest.json"
)
TEXTURE_ROOT = (
    census.ROOT
    / "src"
    / "main"
    / "resources"
    / "assets"
    / "cruciblecraft"
    / "textures"
    / "item"
    / "tool"
)
CATALOG = (
    census.ROOT
    / "src"
    / "main"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "content"
    / "item"
    / "tool"
    / "ElectricToolCatalog.java"
)


class Gt6ElectricToolsTest(unittest.TestCase):
    def test_slug_and_capability_hand_off_unique_active(self) -> None:
        capability = census.load_json(CAPABILITY)
        self.assertEqual(SLUG, capability["slug"])
        self.assertEqual("frozen", capability["maturity"])
        compiled = ledger.compile_ledger()
        topology = census.load_json(
            census.ROOT / "tools" / "waves" / SLUG / "topology.json"
        )
        readiness = census.load_json(
            census.ROOT / "tools" / "waves" / SLUG / "readiness.json"
        )
        if capability["workflow"] == "active":
            self.assertEqual(SLUG, compiled["unique_active_slug"])
            self.assertTrue(PLAN_ACTIVE.is_file())
            self.assertFalse(PLAN_CLOSED.is_file())
            self.assertEqual(SLUG, topology["unique_active_wave"])
            self.assertEqual(SLUG, readiness["unique_active_wave"])
        else:
            self.assertNotEqual(SLUG, compiled["unique_active_slug"])
            self.assertFalse(PLAN_ACTIVE.is_file())
            self.assertTrue(PLAN_CLOSED.is_file())
            self.assertIsNone(topology["unique_active_wave"])
            self.assertIsNone(readiness["unique_active_wave"])

    def test_catalog_lists_gt6_tool_ids(self) -> None:
        source = CATALOG.read_text(encoding="utf-8")
        for name in (
            "MINING_DRILL_LV",
            "MINING_DRILL_MV",
            "MINING_DRILL_HV",
            "CHAINSAW_LV",
            "CHAINSAW_MV",
            "CHAINSAW_HV",
            "WRENCH_LV",
            "WRENCH_MV",
            "WRENCH_HV",
            "JACKHAMMER_HV",
            "JACKHAMMER_HV_NO_ORES",
            "BUZZSAW_LV",
            "SCREWDRIVER_LV",
            "HAND_DRILL_LV",
            "MIXER_LV",
            "MONKEY_WRENCH_LV",
            "MONKEY_WRENCH_MV",
            "MONKEY_WRENCH_HV",
            "TRIMMER_LV",
        ):
            self.assertIn(name + "(", source)

    def test_art_manifest_copies_exist(self) -> None:
        document = json.loads(MANIFEST.read_text(encoding="utf-8"))
        self.assertGreaterEqual(len(document["rows"]), 20)
        for row in document["rows"]:
            destination = census.ROOT / "src" / "main" / "resources" / row["destination"]
            self.assertTrue(destination.is_file(), row["destination"])
            digest = hashlib.sha256(destination.read_bytes()).hexdigest()
            self.assertEqual(digest, row["sha256"], row["destination"])
            self.assertTrue(row["copied"])
            self.assertTrue(
                str(row["gt6_source"]).startswith(
                    "src/main/resources/assets/gregtech/textures/items/"
                )
            )


if __name__ == "__main__":
    unittest.main()
