#!/usr/bin/env python3
"""Nanofab live machine card: 7 selected rows and five source-exact hosts."""
from __future__ import annotations

import importlib.util
import unittest

from tools import capability_ledger as ledger
from tools import census_common as census
from tools import io_common as io
from tools.recipe_bulk.slugs import KNOWN_SEMANTIC_SLUGS
from tools.recipe_bulk.waves import recipe_wave

SLUG = "machines/nanofab"
ROOT = io.ROOT
WAVE = ROOT / "tools" / "waves" / "machines" / "nanofab"
CAPABILITY = ROOT / "tools" / "capabilities" / "machines" / "nanofab" / "capability.json"
PLAN_ACTIVE = ROOT / "docs" / "history" / "card-plans" / "active" / "纳米加工机详细计划.md"
PLAN_CLOSED = ROOT / "docs" / "history" / "card-plans" / "closed" / "纳米加工机详细计划.md"


def _load_builder():
    path = WAVE / "build_nanofab.py"
    spec = importlib.util.spec_from_file_location("nanofab_builder", path)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    spec.loader.exec_module(module)
    return module


builder = _load_builder()


class NanofabCardTest(unittest.TestCase):
    def test_slug_and_recipe_wave(self) -> None:
        self.assertIn(SLUG, KNOWN_SEMANTIC_SLUGS)
        spec = recipe_wave(SLUG)
        self.assertEqual("nanofab", spec.path_prefix)
        self.assertEqual("cruciblecraft:nanofab", spec.host)

    def test_builder_check_passes(self) -> None:
        self.assertEqual([], builder.check())

    def test_selected_and_overflow_stay_exact(self) -> None:
        work = census.load_json(WAVE / "source_pack" / "work_set.json")
        overflow = census.load_json(WAVE / "overflow.json")
        self.assertEqual(64, work["accounting"]["source_rows"])
        self.assertEqual(7, work["accounting"]["selected_rows"])
        self.assertEqual(57, work["accounting"]["overflow_rows"])
        self.assertEqual(57, overflow["blocked_rows"])
        self.assertNotIn("programmed_circuit", str(overflow))

    def test_d0_all_hosts_source_exact(self) -> None:
        document = census.load_json(WAVE / "d0_obtain_matrix.json")
        self.assertEqual(["KAX", "ZMY", "CSC"], document["grid"])
        statuses = {row["host"]: row["status"] for row in document["hosts"]}
        self.assertEqual(
            {"source_exact"},
            set(statuses.values()),
        )
        self.assertEqual(
            {20441, 20442, 20443, 20444, 20445},
            set(statuses),
        )
        for row in document["hosts"]:
            self.assertEqual("cruciblecraft:laser_gas_ar", row["argon_laser"]["cc"])
            self.assertEqual("cruciblecraft:laser_gas_kr", row["krypton_laser"]["cc"])
            self.assertEqual("cruciblecraft:laser_gas_xe", row["xenon_laser"]["cc"])
            self.assertEqual(
                "cruciblecraft:processor_crystal_sapphire",
                row["sapphire_processor"]["cc"],
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
