#!/usr/bin/env python3
"""GT6 missing EU wire-gauge runtime child."""
from __future__ import annotations

import hashlib
import unittest

from tools import capability_ledger as ledger
from tools import census_common as census
from tools import gt6_eu_missing_wire_gauges_runtime as runtime

SLUG = "content/gt6-eu-missing-wire-gauges-runtime"
ROOT = census.ROOT
WAVE = ROOT / "tools" / "waves" / "content" / "gt6-eu-missing-wire-gauges-runtime"
CAPABILITY = (
    ROOT
    / "tools"
    / "capabilities"
    / "content"
    / "gt6-eu-missing-wire-gauges-runtime"
    / "capability.json"
)
PLAN_ACTIVE = (
    ROOT / "docs" / "history" / "card-plans" / "active" / "GT6缺线规运行时详细计划.md"
)
PLAN_CLOSED = (
    ROOT / "docs" / "history" / "card-plans" / "closed" / "GT6缺线规运行时详细计划.md"
)
R0 = runtime.R0
LEDGER = runtime.LEDGER
GATE = runtime.MATERIAL_GATE


class Gt6EuMissingWireGaugesRuntimeTest(unittest.TestCase):
    def test_capability_depends_on_live_item_children(self) -> None:
        capability = census.load_json(CAPABILITY)
        self.assertEqual(SLUG, capability["slug"])
        self.assertEqual(
            [
                "registry/catalog-modern-ids",
                "content/gt6-eu-wire-cable-runtime",
                "content/gt6-connector-art",
                "content/gt6-connector-alias-repair",
            ],
            capability["depends_on"],
        )
        self.assertNotIn("content/gt6-pipe-cable-baseline", capability["depends_on"])
        self.assertEqual(["capability-runtime"], capability["profiles"])
        self.assertNotEqual("player_complete", capability["maturity"])
        self.assertEqual(
            [
                "missingGaugeDummiesFoldOntoLiveHost",
                "missingGaugeWiresAreLiveCableBlocks",
                "missingGaugesAreNotMappedWireAlias",
            ],
            sorted(capability["required_test_ids"]),
        )
        compiled = ledger.compile_ledger()
        if capability["workflow"] == "active":
            self.assertEqual(SLUG, compiled["unique_active_slug"])
            self.assertTrue(PLAN_ACTIVE.is_file())
            self.assertFalse(PLAN_CLOSED.is_file())
        else:
            self.assertEqual("accepted", capability["workflow"])
            self.assertNotEqual(SLUG, compiled["unique_active_slug"])
            self.assertFalse(PLAN_ACTIVE.is_file())
            self.assertTrue(PLAN_CLOSED.is_file())

    def test_missing_gauges_are_live_and_not_mapped_aliases(self) -> None:
        catalog = runtime.CATALOG_JAVA.read_text(encoding="utf-8")
        tests = runtime.GAME_TESTS.read_text(encoding="utf-8")
        core = runtime.CORE_TESTS.read_text(encoding="utf-8")
        self.assertIn("EXPECTED_WIRE_BLOCKS = 473", catalog)
        self.assertIn("SEPTUPLE_WIRE", catalog)
        self.assertIn("wireGt07", catalog)
        self.assertIn("red_alloy", catalog)
        for name in runtime.EXPECTED_TESTS:
            self.assertIn(f"void {name}", tests)
            self.assertNotIn(name, core)
        overlay = census.load_json(runtime.OVERLAY_PATH)
        self.assertEqual(168, int(overlay["counts"]["fold_live_block"]))
        self.assertEqual(0, int(overlay["counts"]["skipped"]))
        self.assertEqual(24, overlay["counts"]["septuple_wire"])
        self.assertEqual(
            overlay["counts"]["septuple_wire"],
            overlay["counts"]["pentadecuple_wire"],
        )
        gate = census.load_json(GATE)
        self.assertEqual(29, int(gate["counts"]["electrical_wire_forms"]))
        self.assertEqual(203, int(gate["counts"]["missing_wire_forms"]))
        self.assertEqual(
            hashlib.sha256(R0.read_bytes()).hexdigest(),
            (WAVE / "r0_disposition_sha256.txt").read_text(encoding="utf-8").strip(),
        )
        self.assertEqual(
            hashlib.sha256(LEDGER.read_bytes()).hexdigest(),
            (WAVE / "baseline_ledger_sha256.txt")
            .read_text(encoding="utf-8")
            .strip(),
        )

    def test_unique_active_docs_and_ledger_hand_off(self) -> None:
        topology = census.load_json(WAVE / "topology.json")
        readiness = census.load_json(WAVE / "readiness.json")
        compiled = ledger.compile_ledger()
        capability = census.load_json(CAPABILITY)
        self.assertEqual([], runtime.check())
        if capability["workflow"] == "active":
            self.assertEqual(SLUG, compiled["unique_active_slug"])
            self.assertEqual(SLUG, topology["unique_active_wave"])
            self.assertEqual(SLUG, readiness["unique_active_wave"])
        else:
            self.assertNotEqual(SLUG, compiled["unique_active_slug"])
            self.assertIsNone(topology["unique_active_wave"])
            self.assertIsNone(readiness["unique_active_wave"])


if __name__ == "__main__":
    unittest.main()
