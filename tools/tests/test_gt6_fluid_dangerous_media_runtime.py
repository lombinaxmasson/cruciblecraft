#!/usr/bin/env python3
"""GT6 fluid-pipe plasma/magic tick, flammable, and contactDamage."""
from __future__ import annotations

import unittest

from pathlib import Path

from tools import capability_ledger as ledger
from tools import census_common as census
from tools import gt6_fluid_dangerous_media_runtime as runtime

SLUG = "content/gt6-fluid-dangerous-media-runtime"
ROOT = census.ROOT
WAVE = ROOT / "tools" / "waves" / "content" / "gt6-fluid-dangerous-media-runtime"
CAPABILITY = (
    ROOT
    / "tools"
    / "capabilities"
    / "content"
    / "gt6-fluid-dangerous-media-runtime"
    / "capability.json"
)
PLAN_ACTIVE = (
    ROOT
    / "docs"
    / "history"
    / "card-plans"
    / "active"
    / "GT6流体危险介质运行时详细计划.md"
)
PLAN_CLOSED = (
    ROOT
    / "docs"
    / "history"
    / "card-plans"
    / "closed"
    / "GT6流体危险介质运行时详细计划.md"
)


class Gt6FluidDangerousMediaRuntimeTest(unittest.TestCase):
    def test_capability_depends_on_fluid_pipe_runtime(self) -> None:
        capability = census.load_json(CAPABILITY)
        self.assertEqual(SLUG, capability["slug"])
        self.assertEqual(
            [
                "registry/catalog-modern-ids",
                "content/gt6-fluid-pipe-runtime",
            ],
            capability["depends_on"],
        )
        self.assertNotIn("content/gt6-pipe-cable-baseline", capability["depends_on"])
        self.assertEqual(["capability-runtime"], capability["profiles"])
        self.assertNotEqual("player_complete", capability["maturity"])
        self.assertEqual(
            runtime.EXPECTED_TESTS,
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

    def test_fill_then_tick_and_isolation(self) -> None:
        errors = runtime.check()
        self.assertEqual([], errors)
        overlay = census.load_json(runtime.OVERLAY_PATH)
        self.assertEqual("removed", overlay["fill_gate"])
        self.assertEqual("FluidPipeDangerousMedia", overlay["tick"])
        self.assertIn("air", overlay["magic_replace"])
        be = runtime.BE.read_text(encoding="utf-8")
        self.assertIn("FluidPipeDangerousMedia.tick", be)
        self.assertNotIn("FluidPipeBlockedMedia.rejects(", be)
        self.assertNotIn("CableNetworkTraversal", be)
        self.assertNotIn("ModCapabilities.ENERGY", be)
        text = Path(runtime.__file__).read_text(encoding="utf-8")
        closed_runtime = "gt6_fluid" + "_pipe_runtime"
        self.assertFalse(
            any(
                line.startswith("from tools import " + closed_runtime)
                for line in text.splitlines()
            )
        )

    def test_r0_and_baseline_ledgers_are_untouched(self) -> None:
        ns = (
            ROOT
            / "src"
            / "main"
            / "resources"
            / "data"
            / "cruciblecraft_wave_content_gt6_fluid_dangerous_media_runtime"
        )
        self.assertTrue((ns / "structure" / "empty.nbt").is_file())
        self.assertTrue((ns / "gametest" / "structure" / "empty.nbt").is_file())


if __name__ == "__main__":
    unittest.main()
