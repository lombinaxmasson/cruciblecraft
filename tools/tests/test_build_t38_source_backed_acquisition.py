#!/usr/bin/env python3
"""Regression coverage for T38's four source-backed mineral inputs."""
from __future__ import annotations

import json
import sys
import unittest
from pathlib import Path

TOOLS = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(TOOLS))

import build_t38_source_backed_acquisition as acquisition


class T38SourceBackedAcquisitionTests(unittest.TestCase):
    def test_all_four_roaster_minerals_have_worldgen_to_dust_forms(self) -> None:
        document = acquisition.build()
        expected = {
            "arsenopyrite",
            "chalcopyrite",
            "cooperite",
            "molybdenite",
        }
        self.assertEqual(expected, set(document["required_materials"]))
        for material in expected:
            self.assertEqual(
                document["required_forms"][material][-1],
                "dust",
            )
            self.assertIn("raw_ore", document["required_forms"][material])

    def test_pinned_veins_preserve_distinct_gt6_source_facts(self) -> None:
        document = acquisition.build()
        facts = {
            vein["provenance"]["source_fact_id"] for vein in document["veins"]
        }
        self.assertEqual(
            facts,
            {
                "ore.large.gold",
                "ore.large.platinum",
                "ore.large.molybdenum",
            },
        )

    def test_every_projected_ore_form_is_runtime_registered(self) -> None:
        document = acquisition.build()
        gate = json.loads(
            (
                TOOLS.parent
                / "src/main/resources/data/cruciblecraft/"
                / "material_registration_gate.json"
            ).read_text(encoding="utf-8")
        )
        for material, forms in document["required_forms"].items():
            self.assertTrue(
                set(forms) <= set(gate["materials"][material]),
                material,
            )


if __name__ == "__main__":
    raise SystemExit(unittest.main())
