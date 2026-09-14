#!/usr/bin/env python3
"""GT6 paper tiny_plate: forceItemGeneration form, slicer overflow gone."""
from __future__ import annotations

import unittest

from tools import census_common as census
from tools import gt6_paper_tiny_plate as paper

ROOT = census.ROOT
GATE = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "material_registration_gate.json"
)
SLICER = ROOT / "tools" / "waves" / "machines" / "slicer"


class PaperTinyPlateTest(unittest.TestCase):
    def test_builder_check_passes(self) -> None:
        self.assertEqual([], paper.check())

    def test_gate_registers_forced_paper_tiny_plate(self) -> None:
        gate = census.load_json(GATE)
        self.assertIn("tiny_plate", gate["materials"]["paper"])
        self.assertIn(
            "tiny_plate",
            gate[paper.GATE_SECTION]["paper"],
        )
        self.assertNotIn("programmed_circuit", str(gate[paper.GATE_SECTION]))

    def test_slicer_overflow_is_empty(self) -> None:
        overflow = census.load_json(SLICER / "overflow.json")
        self.assertEqual(0, overflow["blocked_rows"])
        self.assertEqual([], overflow.get("overflow") or [])
        work = census.load_json(SLICER / "source_pack" / "work_set.json")
        self.assertEqual(33, work["accounting"]["selected_rows"])
        self.assertEqual(0, work["accounting"]["overflow_rows"])

    def test_capability_owns_the_form(self) -> None:
        capability = census.load_json(
            ROOT / "tools" / "capabilities" / paper.SLUG / "capability.json"
        )
        self.assertEqual(paper.SLUG, capability["slug"])
        self.assertEqual("runtime_ready", capability["maturity"])
        self.assertIn(capability["workflow"], {"active", "accepted"})
        owned = capability["identity_disposition"][0]
        self.assertEqual("form:paper:tiny_plate", owned["semantic_key"])
        self.assertEqual(
            ["cruciblecraft:paper/tiny_plate"],
            owned["runtime_ids"],
        )
