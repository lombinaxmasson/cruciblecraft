#!/usr/bin/env python3
"""Live Melter card: exact selected family, source-exact host, load note."""
from __future__ import annotations

import importlib.util
import unittest

from tools import capability_ledger as ledger
from tools import census_common as census
from tools import io_common as io
from tools.recipe_bulk.slugs import KNOWN_SEMANTIC_SLUGS
from tools.recipe_bulk.waves import recipe_wave

SLUG = "machines/melter"
ROOT = io.ROOT
WAVE = ROOT / "tools" / "waves" / "machines" / "melter"
CAPABILITY = (
    ROOT / "tools" / "capabilities" / "machines" / "melter" / "capability.json"
)
PLAN_ACTIVE = (
    ROOT / "docs" / "history" / "card-plans" / "active" / "熔融机详细计划.md"
)
PLAN_CLOSED = (
    ROOT / "docs" / "history" / "card-plans" / "closed" / "熔融机详细计划.md"
)


def _load_builder():
    path = WAVE / "build_melter.py"
    spec = importlib.util.spec_from_file_location("melter_builder", path)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    spec.loader.exec_module(module)
    return module


builder = _load_builder()


class MelterCardTest(unittest.TestCase):
    def test_slug_and_recipe_wave(self) -> None:
        self.assertIn(SLUG, KNOWN_SEMANTIC_SLUGS)
        spec = recipe_wave(SLUG)
        self.assertEqual("melter", spec.path_prefix)
        self.assertEqual("cruciblecraft:melter", spec.host)

    def test_builder_check_passes(self) -> None:
        self.assertEqual([], builder.check())

    def test_selected_and_overflow_stay_exact(self) -> None:
        work = census.load_json(WAVE / "source_pack" / "work_set.json")
        overflow = census.load_json(WAVE / "overflow.json")
        self.assertEqual(6_756, work["accounting"]["source_rows"])
        self.assertEqual(3_961, work["accounting"]["selected_rows"])
        self.assertEqual(2_795, work["accounting"]["overflow_rows"])
        self.assertEqual(2_795, overflow["blocked_rows"])
        blocked_inputs = str(overflow)
        self.assertIn("gregtech:gt.multiitem.technological@1000", blocked_inputs)
        self.assertNotIn("programmed_circuit", str(overflow))

    def test_d0_host_and_load_note_are_source_exact(self) -> None:
        d0 = census.load_json(WAVE / "d0_obtain_matrix.json")
        self.assertEqual(["wUh", "PMP", "BCB"], d0["grid"])
        self.assertEqual([22010], [row["host"] for row in d0["hosts"]])
        host = d0["hosts"][0]
        self.assertEqual("source_exact", host["status"])
        self.assertEqual("cruciblecraft:crucible", host["crucible"]["cc"])
        self.assertEqual("minecraft:bricks", host["bricks"]["cc"])
        notes = census.load_json(WAVE / "runtime_notes.json")
        self.assertEqual(1_000, notes["parallel"])
        self.assertTrue(notes["parallel_duration"])
        self.assertEqual("UNVERIFIED_SCALE", notes["load_scale"])

    def test_capability_is_runtime_ready(self) -> None:
        capability = census.load_json(CAPABILITY)
        self.assertEqual(SLUG, capability["slug"])
        self.assertEqual("runtime_ready", capability["maturity"])
        self.assertEqual(["capability-runtime"], capability["profiles"])
        compiled = ledger.compile_ledger()
        if capability["workflow"] == "active":
            self.assertEqual(SLUG, compiled["unique_active_slug"])
            self.assertTrue(PLAN_ACTIVE.is_file())
            self.assertFalse(PLAN_CLOSED.is_file())
        else:
            self.assertNotEqual(SLUG, compiled["unique_active_slug"])
            self.assertFalse(PLAN_ACTIVE.is_file())
            self.assertTrue(PLAN_CLOSED.is_file())


if __name__ == "__main__":
    unittest.main()
