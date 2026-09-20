#!/usr/bin/env python3
"""Loom live machine card: shared RU/EU map and blocked motor tiers."""
from __future__ import annotations

import importlib.util
import unittest

from tools import capability_ledger as ledger
from tools import census_common as census
from tools import io_common as io
from tools.recipe_bulk.slugs import KNOWN_SEMANTIC_SLUGS
from tools.recipe_bulk.waves import recipe_wave

SLUG = "machines/loom"
ROOT = io.ROOT
WAVE = ROOT / "tools" / "waves" / "machines" / "loom"
CAPABILITY = ROOT / "tools" / "capabilities" / "machines" / "loom" / "capability.json"
PLAN_ACTIVE = ROOT / "docs" / "history" / "card-plans" / "active" / "织机详细计划.md"
PLAN_CLOSED = ROOT / "docs" / "history" / "card-plans" / "closed" / "织机详细计划.md"


def _load_builder():
    path = WAVE / "build_loom.py"
    spec = importlib.util.spec_from_file_location("loom_builder", path)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    spec.loader.exec_module(module)
    return module


builder = _load_builder()


class LoomCardTest(unittest.TestCase):
    def test_slug_and_recipe_wave(self) -> None:
        self.assertIn(SLUG, KNOWN_SEMANTIC_SLUGS)
        spec = recipe_wave(SLUG)
        self.assertEqual("loom", spec.path_prefix)
        self.assertEqual("cruciblecraft:loom", spec.host)

    def test_builder_check_passes(self) -> None:
        self.assertEqual([], builder.check())

    def test_selected_and_overflow_stay_exact(self) -> None:
        work = census.load_json(WAVE / "source_pack" / "work_set.json")
        overflow = census.load_json(WAVE / "overflow.json")
        self.assertEqual(1334, work["accounting"]["source_rows"])
        self.assertEqual(477, work["accounting"]["selected_rows"])
        self.assertEqual(857, work["accounting"]["overflow_rows"])
        self.assertEqual(857, overflow["blocked_rows"])
        self.assertNotIn("programmed_circuit", str(overflow))

    def test_d0_mixed_hosts_are_honest(self) -> None:
        document = census.load_json(WAVE / "d0_obtain_matrix.json")
        self.assertEqual(["ShS", "GMG", "SwS"], document["grid"])
        self.assertEqual(
            {
                20211: "source_exact",
                20212: "source_exact",
                20213: "source_exact",
                20214: "source_exact",
                20361: "source_exact",
                20362: "source_exact",
                20363: "source_exact",
                20364: "source_exact",
                20365: "source_exact",
            },
            {row["host"]: row["status"] for row in document["hosts"]},
        )

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
