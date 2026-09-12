#!/usr/bin/env python3
"""Live Sensors card: 21 GT6 identities; ComputerCraft peripheral stays blocked."""
from __future__ import annotations

import unittest

from tools import capability_ledger as ledger
from tools import census_common as census
from tools import io_common as io

SLUG = "content/sensors"
ROOT = io.ROOT
WAVE = ROOT / "tools" / "waves" / "content" / "sensors"
CAPABILITY = ROOT / "tools" / "capabilities" / "content" / "sensors" / "capability.json"
PLAN_ACTIVE = ROOT / "docs" / "history" / "card-plans" / "active" / "Sensors详细计划.md"
PLAN_CLOSED = ROOT / "docs" / "history" / "card-plans" / "closed" / "Sensors详细计划.md"
PLAN_PREP = ROOT / "docs" / "history" / "card-plans" / "prep" / "Sensors详细计划.md"
ASSETS = ROOT / "src" / "main" / "resources" / "assets" / "cruciblecraft"
SOURCE_IDS = [
    31000,
    31001,
    31002,
    31003,
    31004,
    31005,
    31006,
    31007,
    31010,
    31011,
    31012,
    31013,
    31015,
    31016,
    31017,
    31018,
    31019,
    31020,
    31021,
    31022,
    31023,
]


class SensorsCardTest(unittest.TestCase):
    def test_d0_twenty_one_hosts_and_blocked_tools(self) -> None:
        document = census.load_json(WAVE / "d0_obtain_matrix.json")
        hosts = {row["host"]: row for row in document["hosts"]}
        self.assertEqual(SOURCE_IDS, list(hosts))
        self.assertEqual(21, len(hosts))
        self.assertEqual("source_exact", hosts[31015]["status"])
        self.assertEqual("source_exact", hosts[31019]["status"])
        self.assertEqual(
            "cruciblecraft:gt_multiitem/multiitem_randomtools_m10003",
            hosts[31015]["x"]["cc"],
        )
        self.assertEqual(
            "cruciblecraft:gt_multiitem/multiitem_randomtools_m10004",
            hosts[31019]["x"]["cc"],
        )
        self.assertEqual(
            "cruciblecraft:compact_sensor_lv",
            hosts[31021]["x"]["cc"],
        )
        self.assertEqual(
            "cruciblecraft:glass/gem",
            hosts[31001]["x"]["cc"],
        )
        self.assertNotIn("programmed_circuit", str(document))
        self.assertNotIn("compact_sensor_lv", hosts[31000]["path"])

    def test_capability_is_runtime_ready(self) -> None:
        capability = census.load_json(CAPABILITY)
        self.assertEqual(SLUG, capability["slug"])
        self.assertEqual("runtime_ready", capability["maturity"])
        self.assertEqual(["capability-runtime"], capability["profiles"])
        self.assertEqual(
            [
                "facingAndWeakRedstone",
                "liveHostsReportNonZero",
                "playerSurfaceIsRegistered",
                "survivalCraftableHostsMatchD0",
                "twentyOneIdentitiesAreRegistered",
            ],
            capability["required_test_ids"],
        )
        compiled = ledger.compile_ledger()
        if capability["workflow"] == "active":
            self.assertEqual(SLUG, compiled["unique_active_slug"])
            self.assertTrue(PLAN_ACTIVE.is_file())
            self.assertFalse(PLAN_CLOSED.is_file())
            self.assertFalse(PLAN_PREP.is_file())
        else:
            self.assertEqual("accepted", capability["workflow"])
            self.assertNotEqual(SLUG, compiled["unique_active_slug"])
            self.assertFalse(PLAN_ACTIVE.is_file())
            self.assertTrue(PLAN_CLOSED.is_file())
        blocked = {
            row["semantic_key"]
            for row in capability["identity_disposition"]
            if row["disposition"] == "blocked"
        }
        self.assertEqual(
            {
                "peripheral:sensors:computercraft",
            },
            blocked,
        )
        owned = next(
            row
            for row in capability["identity_disposition"]
            if row["disposition"] == "new_distinct"
        )
        self.assertEqual(21, len(owned["runtime_ids"]))

    def test_art_is_copied_and_not_aliased(self) -> None:
        manifest = census.load_json(ASSETS / "gt6_sensors_art_manifest.json")
        self.assertEqual(126, len(manifest["imports"]))
        gt6_root = ROOT / "gt6_referencable_port_code" / "gregtech6_w"
        self.assertTrue(gt6_root.is_dir())
        for row in manifest["imports"]:
            destination = ROOT / "src" / "main" / "resources" / row["destination"]
            source = gt6_root / row["gt6_source"]
            self.assertTrue(destination.is_file(), row["destination"])
            self.assertTrue(source.is_file(), row["gt6_source"])
            self.assertEqual(source.read_bytes(), destination.read_bytes())
            self.assertNotIn("oven", row["destination"])
            self.assertNotIn("multiblock_casing", row["destination"])
            self.assertNotIn("pipe_filter_cover", row["destination"])

    def test_tool_art_is_copied_and_not_aliased(self) -> None:
        manifest = census.load_json(ASSETS / "gt6_sensor_tools_art_manifest.json")
        self.assertEqual(2, len(manifest["imports"]))
        gt6_root = ROOT / "gt6_referencable_port_code" / "gregtech6_w"
        self.assertTrue(gt6_root.is_dir())
        for row in manifest["imports"]:
            destination = ROOT / "src" / "main" / "resources" / row["destination"]
            source = gt6_root / row["gt6_source"]
            self.assertTrue(destination.is_file(), row["destination"])
            self.assertTrue(source.is_file(), row["gt6_source"])
            self.assertEqual(source.read_bytes(), destination.read_bytes())
            self.assertNotIn("oven", row["destination"])
            self.assertNotIn("multiblock_casing", row["destination"])
        tools = {
            row["destination"]: row for row in manifest["imports"]
        }
        self.assertIn(
            "assets/cruciblecraft/textures/item/gt6_import/electrometer.png",
            tools,
        )
        self.assertIn(
            "assets/cruciblecraft/textures/item/gt6_import/tachometer.png",
            tools,
        )

    def test_unique_active_docs_and_ledger_hand_off(self) -> None:
        topology = census.load_json(WAVE / "topology.json")
        readiness = census.load_json(WAVE / "readiness.json")
        notes = census.load_json(WAVE / "runtime_notes.json")
        compiled = ledger.compile_ledger()
        capability = census.load_json(CAPABILITY)
        if capability["workflow"] == "active":
            self.assertEqual(SLUG, compiled["unique_active_slug"])
            self.assertEqual(SLUG, topology["unique_active_wave"])
            self.assertEqual(SLUG, readiness["unique_active_wave"])
        else:
            self.assertNotEqual(SLUG, compiled["unique_active_slug"])
            self.assertIsNone(topology["unique_active_wave"])
            self.assertIsNone(readiness["unique_active_wave"])
        self.assertFalse(notes["covers_allowed"])
        self.assertEqual("blocked", notes["computer_craft_peripheral"])
        self.assertEqual([], notes["fail_closed"])


if __name__ == "__main__":
    unittest.main()
