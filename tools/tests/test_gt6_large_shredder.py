#!/usr/bin/env python3
"""Python contract tests for the GT6 Large Shredder wave."""
from __future__ import annotations

import json
import runpy
import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))
WAVE = ROOT / "tools/waves/machines/large-shredder"
LANDING = ROOT / "tools/gt6_large_shredder.py"
CAPABILITY = ROOT / "tools/capabilities/machines/large-shredder/capability.json"
STRUCTURE = ROOT / "src/main/resources/data/cruciblecraft/multiblock_structures/large_shredder.json"
ART = ROOT / "src/main/resources/assets/cruciblecraft/gt6_large_shredder_art_manifest.json"


def load(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


class LargeShredderContractTest(unittest.TestCase):
    def test_landing_check_is_current(self) -> None:
        namespace = runpy.run_path(str(LANDING))
        self.assertEqual([], namespace["check"]())

    def test_source_contract(self) -> None:
        contract = load(WAVE / "source_contract.json")
        self.assertEqual(17109, contract["gt6"]["loader_meta"])
        self.assertEqual("RM.Shredder", contract["gt6"]["recipe_map"])
        self.assertTrue(contract["no_constant_power"])
        self.assertEqual("RU", contract["energy"]["type"])
        self.assertEqual(512, contract["energy"]["input_minimum"])
        self.assertEqual(4096, contract["energy"]["input_maximum"])
        self.assertEqual(64, contract["parallel"])
        self.assertEqual(5000, contract["efficiency"])
        self.assertEqual(12, contract["io"]["item_outputs"])

    def test_structure_is_source_exact(self) -> None:
        document = load(STRUCTURE)
        palette = document["palette"]
        counts: dict[str, int] = {}
        for row in document["structure"]:
            predicate = palette[row["predicate"]]
            key = predicate.get("port") or predicate.get("type")
            counts[key] = counts.get(key, 0) + 1
        self.assertEqual(75, len(document["structure"]))
        self.assertEqual(1, counts["controller"])
        self.assertEqual(9, counts["item_fluid_in"])
        self.assertEqual(24, counts["item_fluid_out"])
        self.assertEqual(2, counts["energy_input"])
        self.assertEqual(
            56,
            sum(
                palette[row["predicate"]].get("block")
                == "cruciblecraft:tungstensteel/wall"
                for row in document["structure"]
            ),
        )
        self.assertEqual(
            "gregtech.tileentity.multiblocks.MultiTileEntityShredder",
            document["source"]["class"],
        )

    def test_art_manifest_is_complete_and_real(self) -> None:
        manifest = load(ART)
        self.assertTrue(manifest["source_present"])
        self.assertEqual(
            49,
            len(manifest["imports"]),
        )
        for row in manifest["imports"]:
            destination = (
                ROOT
                / "src/main/resources/assets/cruciblecraft"
                / row["destination"].split("assets/cruciblecraft/", 1)[-1]
            )
            self.assertTrue(destination.is_file(), destination)

    def test_d0_and_overflow_do_not_use_stand_ins(self) -> None:
        d0 = load(WAVE / "d0_obtain_matrix.json")
        self.assertEqual(["SGS", "GSG", "RMC"], d0["grid"])
        self.assertEqual("source_exact", d0["status"])
        self.assertEqual(
            "cruciblecraft:tungstensteel/wall",
            d0["cells"]["M"]["cc"],
        )
        self.assertEqual(18003, d0["cells"]["M"]["meta"])
        self.assertNotIn("programmed_circuit", json.dumps(d0))
        overflow = load(WAVE / "overflow.json")
        self.assertNotIn("programmed_circuit", json.dumps(overflow))

    def test_capability_owns_runtime_and_wave(self) -> None:
        capability = load(CAPABILITY)
        self.assertEqual("machines/large-shredder", capability["slug"])
        self.assertEqual("runtime_ready", capability["maturity"])
        self.assertEqual(["capability-runtime"], capability["profiles"])
        self.assertIn(
            "formedStructureBindsNineInputAndNineStructureBlades",
            capability["required_test_ids"],
        )


if __name__ == "__main__":
    unittest.main()
