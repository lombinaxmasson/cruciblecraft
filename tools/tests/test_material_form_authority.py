"""Tests for the single material-form authority."""
from __future__ import annotations

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import material_form_authority as authority
from tools import t40_vr_common as vr


class MaterialFormAuthorityTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.document = authority.build()

    def test_java_overlay_sections_are_explicit_and_stable(self) -> None:
        declared = authority.java_overlay_sections(self.document)
        self.assertEqual(declared, self.document["java_overlay_sections"])
        self.assertIn("t48_required_forms", declared)
        self.assertIn("tool_head_required_forms", declared)
        self.assertEqual(137, self.document["typed_ore_denominators"]["factual_ore_materials"])
        self.assertEqual(147, self.document["typed_ore_denominators"]["registered_ore_materials"])
        self.assertEqual(10, self.document["typed_ore_denominators"]["t38_acquisition_ore_delta"])
        self.assertEqual(8, self.document["typed_ore_denominators"]["t5_semantic_vein_ledger"])

    def test_gate_overlay_reads_authority_not_hardcoded_card_lists(self) -> None:
        gate = vr.gate_document()
        overlay = authority.overlay_forms_from_gate(gate, document=self.document)
        self.assertIn("ore", overlay.get("arsenopyrite", set()))
        compare = (ROOT / "tools" / "compare_gt6_recipes.py").read_text(encoding="utf-8")
        veins = (ROOT / "tools" / "build_gt6_veins.py").read_text(encoding="utf-8")
        self.assertIn("overlay_forms_from_gate", compare)
        self.assertIn("overlay_forms_from_gate", veins)
        self.assertNotIn(
            '"t38_source_backed_acquisition_forms",\n        "t38_required_forms"',
            compare,
        )

    def test_check_is_read_only_when_artifact_exists(self) -> None:
        if not authority.OUTPUT.is_file():
            self.skipTest("material_form_authority.json not generated")
        before = authority.OUTPUT.read_bytes()
        self.assertEqual([], authority.check())
        self.assertEqual(before, authority.OUTPUT.read_bytes())


if __name__ == "__main__":
    unittest.main()
