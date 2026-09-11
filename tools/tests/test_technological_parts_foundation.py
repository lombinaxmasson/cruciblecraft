#!/usr/bin/env python3
"""Technological-parts foundation: all-tier exact technological parts."""
from __future__ import annotations

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
from tools import technological_parts_foundation as foundation
from tools.recipe_bulk.slugs import KNOWN_SEMANTIC_SLUGS

SLUG = "content/technological-parts-foundation"
PLAN_ACTIVE = (
    census.ROOT / "docs" / "history" / "card-plans" / "active"
    / "技术中间件基础详细计划.md"
)
PLAN_CLOSED = (
    census.ROOT / "docs" / "history" / "card-plans" / "closed"
    / "技术中间件基础详细计划.md"
)
LIVE_PRINTER = (
    census.ROOT
    / "src"
    / "recipe_generated"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "recipe"
)


class TechnologicalPartsFoundationTest(unittest.TestCase):
    def test_slug_and_capability_hand_off_unique_active(self) -> None:
        self.assertIn(SLUG, KNOWN_SEMANTIC_SLUGS)
        capability = census.load_json(foundation.CAPABILITY)
        self.assertEqual(SLUG, capability["slug"])
        self.assertEqual("runtime_ready", capability["maturity"])
        compiled = ledger.compile_ledger()
        topology = census.load_json(foundation.TOPOLOGY)
        readiness = census.load_json(foundation.READINESS)
        if capability["workflow"] == "active":
            self.assertEqual(SLUG, compiled["unique_active_slug"])
            self.assertTrue(PLAN_ACTIVE.is_file())
            self.assertFalse(PLAN_CLOSED.is_file())
            self.assertEqual(SLUG, topology["unique_active_wave"])
            self.assertEqual(SLUG, readiness["unique_active_wave"])
        else:
            self.assertEqual("accepted", capability["workflow"])
            self.assertNotEqual(SLUG, compiled["unique_active_slug"])
            self.assertFalse(PLAN_ACTIVE.is_file())
            self.assertTrue(PLAN_CLOSED.is_file())
            self.assertIsNone(topology["unique_active_wave"])
            self.assertIsNone(readiness["unique_active_wave"])

    def test_builder_check_passes(self) -> None:
        self.assertEqual([], foundation.check())

    def test_landed_parts_are_source_exact_and_cover_backed(self) -> None:
        document = census.load_json(foundation.LEDGER)
        by_key = {
            (row["family"], row["tier"]): row for row in document["rows"]
        }
        self.assertEqual(
            {"source_exact"},
            {
                row["status"]
                for row in document["rows"]
                if row["status"] == "source_exact"
            },
        )
        self.assertEqual(45, sum(
            row["status"] == "source_exact" for row in document["rows"]
        ))
        self.assertEqual(0, document["blocked_count"])
        self.assertEqual("reuse_canonical", by_key[("motors", 1)]["status"])
        self.assertEqual("reuse_canonical", by_key[("pistons", 4)]["status"])
        self.assertEqual("source_exact", by_key[("motors", 2)]["status"])
        self.assertEqual("source_exact", by_key[("emitters", 1)]["status"])
        self.assertEqual("source_exact", by_key[("sensors", 4)]["status"])
        self.assertEqual("source_exact", by_key[("pumps", 5)]["status"])
        self.assertIn(
            "compact_electric_pump_iv",
            by_key[("pumps", 5)]["cc"],
        )
        self.assertEqual("source_exact", by_key[("robot_arms", 3)]["status"])
        self.assertEqual("source_exact", by_key[("field_generators", 4)]["status"])
        blob = str(document)
        self.assertFalse(foundation.has_split_module_standin(blob))
        self.assertNotIn("programmed_circuit", blob)

    def test_printer_has_no_live_family(self) -> None:
        if LIVE_PRINTER.is_dir():
            self.assertEqual([], list(LIVE_PRINTER.rglob("*printer*")))


if __name__ == "__main__":
    unittest.main()
