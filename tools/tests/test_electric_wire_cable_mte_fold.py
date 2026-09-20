#!/usr/bin/env python3
"""Electric wire/cable MTE ids fold onto registered CC conductors."""
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
from tools import electric_wire_cable_mte_fold as fold
from tools.recipe_bulk.ordinary_source import load_mte_runtime
from tools.recipe_bulk.slugs import KNOWN_SEMANTIC_SLUGS

SLUG = "content/electric-wire-cable-mte-fold"
PLAN_ACTIVE = (
    census.ROOT / "docs" / "history" / "card-plans" / "active"
    / "导线电缆MTE折回详细计划.md"
)
PLAN_CLOSED = (
    census.ROOT / "docs" / "history" / "card-plans" / "closed"
    / "导线电缆MTE折回详细计划.md"
)


class ElectricWireCableMteFoldTest(unittest.TestCase):
    def test_slug_and_capability_hand_off_unique_active(self) -> None:
        self.assertIn(SLUG, KNOWN_SEMANTIC_SLUGS)
        capability = census.load_json(fold.CAPABILITY)
        self.assertEqual(SLUG, capability["slug"])
        self.assertEqual("runtime_ready", capability["maturity"])
        compiled = ledger.compile_ledger()
        topology = census.load_json(fold.TOPOLOGY)
        readiness = census.load_json(fold.READINESS)
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
        self.assertEqual([], fold.check())

    def test_tin_and_osmium_cables_fold_and_scatter_is_refused(self) -> None:
        document = census.load_json(fold.OPERAND_MAP)
        by_meta = {int(row["meta"]): row["runtime_id"] for row in document["mappings"]}
        self.assertEqual("cruciblecraft:tin/wire", by_meta[28050])
        self.assertEqual("cruciblecraft:tin/cable", by_meta[28066])
        self.assertEqual("cruciblecraft:osmium_elemental/octuple_cable", by_meta[29223])
        self.assertEqual("cruciblecraft:graphene/wire", by_meta[29800])
        self.assertEqual("cruciblecraft:graphene/double_wire", by_meta[29801])
        self.assertEqual("cruciblecraft:superconductor/wire", by_meta[29950])
        self.assertEqual(
            "cruciblecraft:superconductor/hexadecuple_wire", by_meta[29965]
        )
        self.assertEqual("cruciblecraft:hslasteel/double_wire", by_meta[28251])
        self.assertEqual("cruciblecraft:hslasteel/dodecuple_cable", by_meta[28277])
        self.assertFalse(any("/gt_mte/" in str(row) for row in by_meta.values()))
        self.assertEqual(320, int(document["mapped_count"]))
        self.assertEqual(0, int(census.load_json(fold.UNMAPPED)["unmapped_count"]))

    def test_ordinary_source_uses_the_overlay(self) -> None:
        mapped = load_mte_runtime()
        self.assertEqual(
            "cruciblecraft:tin/cable",
            mapped[("gregtech:gt.multitileentity", 28066)],
        )
        self.assertEqual(
            "cruciblecraft:tin/wire",
            mapped[("gregtech:gt.multitileentity", 28050)],
        )

    def test_live_machine_overflow_shrinks_without_stand_ins(self) -> None:
        laminator = census.load_json(
            census.ROOT / "tools" / "waves" / "machines" / "laminator"
            / "overflow.json"
        )
        loom = census.load_json(
            census.ROOT / "tools" / "waves" / "machines" / "loom"
            / "overflow.json"
        )
        nanofab = census.load_json(
            census.ROOT / "tools" / "waves" / "machines" / "nanofab"
            / "overflow.json"
        )
        self.assertEqual(12, laminator["blocked_rows"])
        self.assertEqual(857, loom["blocked_rows"])
        self.assertEqual(57, nanofab["blocked_rows"])
        blob = str(laminator) + str(loom) + str(nanofab)
        self.assertNotIn("programmed_circuit", blob)
        self.assertNotIn("/gt_mte/", blob)
        self.assertNotIn("@28066", str(laminator))
        self.assertNotIn("@28050", str(laminator))
        melter_source = (
            census.ROOT / "tools" / "waves" / "machines" / "melter" / "source.json"
        )
        blob = census.load_json(melter_source)
        self.assertIn("cruciblecraft:copper/dodecuple_wire", str(blob))
        self.assertNotIn("cruciblecraft:gt_mte/mte_28361", str(blob))


if __name__ == "__main__":
    unittest.main()
