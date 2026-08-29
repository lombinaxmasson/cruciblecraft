"""Tests for Python/Java/ore-chain material overlay closure."""
from __future__ import annotations

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_t40_vr_material_cross_stack_closure as builder


class T40VrCrossStackClosureTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.document = builder.build()

    def test_typed_ore_denominators_stay_split(self) -> None:
        denominators = self.document["typed_ore_denominators"]
        self.assertEqual(137, denominators["factual_ore_materials"])
        self.assertEqual(147, denominators["registered_ore_materials"])
        self.assertEqual(10, denominators["t38_acquisition_ore_delta"])
        self.assertEqual(8, denominators["t5_semantic_vein_ledger"])
        self.assertNotEqual(
            denominators["factual_ore_materials"],
            denominators["registered_ore_materials"],
        )
        self.assertNotEqual(
            denominators["t5_semantic_vein_ledger"],
            denominators["registered_ore_materials"],
        )

    def test_closure_gates_pass(self) -> None:
        self.assertEqual([], self.document["failed_gates"])
        self.assertEqual("T40_VR_CROSS_STACK_CLOSED", self.document["status"])
        self.assertTrue(self.document["gates"]["registered_ore_matches_crush_rows"])
        self.assertTrue(self.document["gates"]["t38_support_19_15"])
        self.assertTrue(self.document["gates"]["card_builders_do_not_write_gate"])

    def test_check_is_read_only_when_artifact_exists(self) -> None:
        if not builder.OUTPUT.is_file():
            self.skipTest("cross-stack closure not generated")
        before = builder.OUTPUT.read_bytes()
        self.assertEqual([], builder.check())
        self.assertEqual(before, builder.OUTPUT.read_bytes())


if __name__ == "__main__":
    unittest.main()
