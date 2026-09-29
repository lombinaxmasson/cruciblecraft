#!/usr/bin/env python3
"""GT6 insulated redstone 27006/27056/27506 runtime child."""
from __future__ import annotations

import unittest

from tools import capability_ledger as ledger
from tools import census_common as census
from tools import gt6_insulated_redstone_runtime as runtime

SLUG = "content/gt6-insulated-redstone-runtime"
ROOT = census.ROOT
WAVE = ROOT / "tools" / "waves" / "content" / "gt6-insulated-redstone-runtime"
CAPABILITY = (
    ROOT
    / "tools"
    / "capabilities"
    / "content"
    / "gt6-insulated-redstone-runtime"
    / "capability.json"
)
PLAN_ACTIVE = (
    ROOT / "docs" / "history" / "card-plans" / "active" / "GT6绝缘红石运行时详细计划.md"
)
PLAN_CLOSED = (
    ROOT / "docs" / "history" / "card-plans" / "closed" / "GT6绝缘红石运行时详细计划.md"
)
CLOSED_REDSTONE = (
    ROOT / "tools" / "waves" / "content" / "mte-redstone-wire" / "identity_resolution_ledger.json"
)


class Gt6InsulatedRedstoneRuntimeTest(unittest.TestCase):
    def test_capability_depends_on_live_redstone_children(self) -> None:
        capability = census.load_json(CAPABILITY)
        self.assertEqual(SLUG, capability["slug"])
        self.assertEqual(
            [
                "registry/catalog-modern-ids",
                "content/mte-redstone-wire",
                "content/gt6-redstone-wire-correction",
            ],
            capability["depends_on"],
        )
        self.assertNotIn("content/gt6-pipe-cable-baseline", capability["depends_on"])
        self.assertNotIn("content/gt6-eu-wire-cable-runtime", capability["depends_on"])
        self.assertEqual(["capability-runtime"], capability["profiles"])
        self.assertNotEqual("player_complete", capability["maturity"])
        self.assertEqual(
            [
                "insulatedCablesAreLiveRedstoneBlocks",
                "insulatedCablesAreNotEuOrTinAlias",
                "insulatedJoinsBareRedstoneNetwork",
                "lumiumCableDoesNotGlow",
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

    def test_in_place_cables_are_not_eu_and_keep_bare_size(self) -> None:
        kind = runtime.KIND_JAVA.read_text(encoding="utf-8")
        core = runtime.CORE_TESTS.read_text(encoding="utf-8")
        self.assertIn("EXPECTED_SIZE = 3", kind)
        self.assertIn("EXPECTED_CATALOG = 6", kind)
        self.assertIn("RED_ALLOY_CABLE", kind)
        self.assertIn("boolean insulated", kind)
        for name in runtime.EXPECTED_TESTS:
            self.assertNotIn(name, core)
        overlay = census.load_json(runtime.OVERLAY_PATH)
        self.assertEqual(3, overlay["counts"]["in_place"])
        self.assertEqual(0, overlay["counts"]["catalog_dummy_folds"])
        self.assertEqual(
            {27006, 27056, 27506},
            {int(row["meta"]) for row in overlay["rows"]},
        )

    def test_closed_redstone_ledger_still_lists_extras(self) -> None:
        document = census.load_json(CLOSED_REDSTONE)
        extras = {row["meta"] for row in document["out_of_denominator"]}
        self.assertEqual({27006, 27056, 27506}, extras)

    def test_builder_check_passes(self) -> None:
        self.assertEqual([], runtime.check())


if __name__ == "__main__":
    unittest.main()
