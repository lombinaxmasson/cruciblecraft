#!/usr/bin/env python3
"""Injector live machine card: 611 selected rows and EV-only exact host."""
from __future__ import annotations

import importlib.util
import unittest

from tools import capability_ledger as ledger
from tools import census_common as census
from tools import io_common as io
from tools.recipe_bulk.slugs import KNOWN_SEMANTIC_SLUGS
from tools.recipe_bulk.waves import recipe_wave

SLUG = "machines/injector"
ROOT = io.ROOT
WAVE = ROOT / "tools" / "waves" / "machines" / "injector"
CAPABILITY = ROOT / "tools" / "capabilities" / "machines" / "injector" / "capability.json"
PLAN_ACTIVE = ROOT / "docs" / "history" / "card-plans" / "active" / "注射机详细计划.md"
PLAN_CLOSED = ROOT / "docs" / "history" / "card-plans" / "closed" / "注射机详细计划.md"


def _load_builder():
    path = WAVE / "build_injector.py"
    spec = importlib.util.spec_from_file_location("injector_builder", path)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    spec.loader.exec_module(module)
    return module


builder = _load_builder()


class InjectorCardTest(unittest.TestCase):
    def test_slug_and_recipe_wave(self) -> None:
        self.assertIn(SLUG, KNOWN_SEMANTIC_SLUGS)
        spec = recipe_wave(SLUG)
        self.assertEqual("injector", spec.path_prefix)
        self.assertEqual("cruciblecraft:injector", spec.host)

    def test_builder_check_passes(self) -> None:
        self.assertEqual([], builder.check())

    def test_selected_and_overflow_stay_exact(self) -> None:
        work = census.load_json(WAVE / "source_pack" / "work_set.json")
        overflow = census.load_json(WAVE / "overflow.json")
        self.assertEqual(638, work["accounting"]["source_rows"])
        self.assertEqual(103, work["accounting"]["selected_rows"])
        self.assertEqual(535, work["accounting"]["overflow_rows"])
        self.assertEqual(535, overflow["blocked_rows"])
        self.assertNotIn("programmed_circuit", str(overflow))

    def test_d0_ev_only_exact(self) -> None:
        document = census.load_json(WAVE / "d0_obtain_matrix.json")
        self.assertEqual(["XPw", "CMW"], document["grid"])
        statuses = {row["host"]: row["status"] for row in document["hosts"]}
        self.assertEqual("source_exact", statuses[20264])
        self.assertEqual(
            {"explicitly_blocked"},
            {statuses[host] for host in (20261, 20262, 20263, 20265)},
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
            self.assertIsNone(compiled["unique_active_slug"])
            self.assertFalse(PLAN_ACTIVE.is_file())
            self.assertTrue(PLAN_CLOSED.is_file())


if __name__ == "__main__":
    unittest.main()
