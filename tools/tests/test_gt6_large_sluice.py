#!/usr/bin/env python3
"""Python contract tests for GT6 Large Sluice 17107."""
from __future__ import annotations

import json
import runpy
import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))
LANDING = ROOT / "tools/gt6_large_sluice.py"
WAVE = ROOT / "tools/waves/machines/large-sluice"
STRUCTURE = ROOT / "src/main/resources/data/cruciblecraft/multiblock_structures/large_sluice.json"
ART = ROOT / "src/main/resources/assets/cruciblecraft/gt6_large_sluice_art_manifest.json"


def load(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


class LargeSluiceContractTest(unittest.TestCase):
    def test_landing_check_is_current(self) -> None:
        namespace = runpy.run_path(str(LANDING))
        self.assertEqual([], namespace["check"]())

    def test_source_contract(self) -> None:
        contract = load(WAVE / "source_contract.json")
        self.assertEqual(17107, contract["gt6"]["loader_meta"])
        self.assertEqual("RM.Sluice", contract["gt6"]["recipe_map"])
        self.assertEqual("RU", contract["energy"]["type"])
        self.assertEqual(512, contract["energy"]["input_minimum"])
        self.assertEqual(4096, contract["energy"]["input_maximum"])
        self.assertEqual(64, contract["parallel"])
        self.assertTrue(contract["parallel_duration"])
        self.assertEqual(5000, contract["efficiency"])
        self.assertEqual([3, 7, 3], contract["structure"]["dimensions"])

    def test_structure_counts_source_exact_ports(self) -> None:
        document = load(STRUCTURE)
        palette = document["palette"]
        counts: dict[str, int] = {}
        for row in document["structure"]:
            predicate = palette[row["predicate"]]
            key = predicate.get("port") or predicate.get("type")
            counts[key] = counts.get(key, 0) + 1
        self.assertEqual(63, len(document["structure"]))
        self.assertEqual(1, counts["controller"])
        self.assertEqual(3, counts["item_fluid_in"])
        self.assertEqual(2, counts["item_fluid_out"])
        self.assertEqual(2, counts["energy_input"])
        self.assertEqual(
            41,
            sum(
                palette[row["predicate"]].get("block")
                == "cruciblecraft:titanium/wall"
                for row in document["structure"]
            ),
        )
        self.assertEqual(
            21,
            sum(
                palette[row["predicate"]].get("block")
                == "cruciblecraft:multiblock/sluice_part"
                for row in document["structure"]
            ),
        )

    def test_art_manifest_destinations_exist(self) -> None:
        manifest = load(ART)
        self.assertTrue(manifest["source_present"])
        self.assertGreaterEqual(len(manifest["imports"]), 24)
        for row in manifest["imports"]:
            destination = (
                ROOT
                / "src/main/resources/assets/cruciblecraft"
                / row["destination"].split("assets/cruciblecraft/", 1)[-1]
            )
            self.assertTrue(destination.is_file(), destination)

    def test_d0_has_no_stand_ins(self) -> None:
        d0 = load(WAVE / "d0_obtain_matrix.json")
        self.assertEqual(["GGG", "SwS", "RMC"], d0["pattern"])
        self.assertEqual("source_exact", d0["status"])
        self.assertTrue(d0["ok"])
        self.assertNotIn("programmed_circuit", json.dumps(d0))


if __name__ == "__main__":
    unittest.main()
