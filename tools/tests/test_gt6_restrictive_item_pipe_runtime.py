#!/usr/bin/env python3
"""GT6 restrictive item-pipe runtime child."""
from __future__ import annotations

import unittest

from tools import capability_ledger as ledger
from tools import census_common as census
from tools import gt6_restrictive_item_pipe_runtime as runtime

SLUG = "content/gt6-restrictive-item-pipe-runtime"
ROOT = census.ROOT
WAVE = ROOT / "tools" / "waves" / "content" / "gt6-restrictive-item-pipe-runtime"
CAPABILITY = (
    ROOT
    / "tools"
    / "capabilities"
    / "content"
    / "gt6-restrictive-item-pipe-runtime"
    / "capability.json"
)
PLAN_ACTIVE = (
    ROOT / "docs" / "history" / "card-plans" / "active" / "GT6限制物品管运行时详细计划.md"
)
PLAN_CLOSED = (
    ROOT / "docs" / "history" / "card-plans" / "closed" / "GT6限制物品管运行时详细计划.md"
)
R0 = runtime.R0
LEDGER = runtime.LEDGER


class Gt6RestrictiveItemPipeRuntimeTest(unittest.TestCase):
    def test_capability_depends_on_live_item_children(self) -> None:
        capability = census.load_json(CAPABILITY)
        self.assertEqual(SLUG, capability["slug"])
        self.assertEqual(
            [
                "registry/catalog-modern-ids",
                "content/gt6-item-pipe-runtime",
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
                "restrictiveCatalogDummiesFoldOntoLiveHost",
                "restrictiveStepSizeIsTimes100",
                "restrictiveUsesRestrictorOverlayNotOrdinaryAlias",
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

    def test_step_size_overlay_and_not_ordinary_alias(self) -> None:
        catalog = runtime.CATALOG_JAVA.read_text(encoding="utf-8")
        core = runtime.CORE_TESTS.read_text(encoding="utf-8")
        self.assertIn("RESTRICTIVE_ITEM_PIPE", catalog)
        self.assertIn("pipeRestrictiveMedium", catalog)
        self.assertIn("MAX_RUNTIME_BLOCKS = 500", catalog)
        self.assertIn("multiplyExact(source.stepSize(), 100L)", catalog)
        for name in runtime.EXPECTED_TESTS:
            self.assertNotIn(name, core)
        overlay = census.load_json(runtime.OVERLAY_PATH)
        self.assertEqual(6, int(overlay["counts"]["fold_live_block"]))
        self.assertEqual(2, overlay["counts"]["medium"])
        self.assertEqual(overlay["counts"]["medium"], overlay["counts"]["large"])
        self.assertEqual(overlay["counts"]["large"], overlay["counts"]["huge"])
        self.assertEqual(0, int(overlay["counts"]["skipped"]))
        self.assertEqual(57, int(overlay["counts"]["loader_out"]))

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
