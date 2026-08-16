"""Tests for the T22.5 B2 fluid-gap disposition builder."""
from __future__ import annotations

import json
import sys
import unittest
from pathlib import Path

TOOLS = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(TOOLS))

import build_t22_5_fluid_gap_disposition as builder  # noqa: E402


class T225FluidGapDispositionTest(unittest.TestCase):

    def test_committed_artifact_is_current(self) -> None:
        if not builder.OUTPUT.is_file():
            self.skipTest("artifact not yet generated")
        self.assertEqual(builder.check(), [])

    def test_every_gap_material_is_disposed(self) -> None:
        document = json.loads(
            builder.OUTPUT.read_text(encoding="utf-8")
        )
        a2 = builder._load(builder.A2_ARTIFACT)
        gap_materials = {
            row["cc_material"]
            for row in a2["mapping"]
            if row["disposition"] == "no_cc_fluid"
        }
        disposed = {r["material"] for r in document["records"]}
        self.assertEqual(disposed, gap_materials)
        self.assertEqual(document["counts"]["materials"], 15)
        self.assertEqual(document["counts"]["fluid_names"], 18)

    def test_no_registration_is_expected(self) -> None:
        document = json.loads(
            builder.OUTPUT.read_text(encoding="utf-8")
        )
        for record in document["records"]:
            self.assertEqual(record["disposition"], "no_registration")
            self.assertEqual(record["v1_rows_touching"], 0)

    def test_plan_scope_correction_is_recorded(self) -> None:
        document = json.loads(
            builder.OUTPUT.read_text(encoding="utf-8")
        )
        self.assertIn("hslasteel", document["scope_correction"])
        self.assertIn("15 materials", document["scope_correction"])

    def test_material_tree_digest_is_current(self) -> None:
        document = json.loads(
            builder.OUTPUT.read_text(encoding="utf-8")
        )
        self.assertEqual(
            document["material_tree"]["sha256"],
            builder.material_tree_digest(),
        )

    def test_rationale_points_to_definition(self) -> None:
        document = json.loads(
            builder.OUTPUT.read_text(encoding="utf-8")
        )
        for record in document["records"]:
            self.assertNotIn(
                "cannot translate",
                record["rationale"].lower(),
            )
            self.assertTrue(record["rationale"])


if __name__ == "__main__":
    unittest.main()
