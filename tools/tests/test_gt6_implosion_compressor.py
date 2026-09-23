"""Python contracts for the GT6 Implosion Compressor wave."""
from __future__ import annotations

import json
import runpy
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
WAVE = ROOT / "tools/waves/machines/implosion-compressor"
BUILD = WAVE / "build_implosion_compressor.py"
STRUCTURE = ROOT / (
    "src/main/resources/data/cruciblecraft/multiblock_structures/"
    "implosion_compressor.json"
)
ART = ROOT / (
    "src/main/resources/assets/cruciblecraft/"
    "gt6_implosion_compressor_art_manifest.json"
)


def load(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


class ImplosionCompressorContractTest(unittest.TestCase):
    def test_source_builder_is_current(self) -> None:
        namespace = runpy.run_path(str(BUILD))
        self.assertEqual([], namespace["check"]())

    def test_source_accounting_and_contract(self) -> None:
        work = load(WAVE / "source_pack/work_set.json")
        self.assertEqual(1072, work["accounting"]["source_rows"])
        self.assertEqual(776, work["accounting"]["selected_rows"])
        self.assertEqual(296, work["accounting"]["overflow_rows"])
        contract = load(WAVE / "source_contract.json")
        self.assertEqual(17110, contract["gt6"]["loader_meta"])
        self.assertEqual("RM.ImplosionCompressor", contract["gt6"]["recipe_map"])
        self.assertEqual(["CPC", "PAP", "RMR"], contract["source_tokens"]["pattern"])
        self.assertIn("A=IL.ROBOT_ARMS[2]", contract["source_tokens"]["required"])

    def test_structure_is_source_exact(self) -> None:
        document = load(STRUCTURE)
        palette = document["palette"]
        ports = sum(
            palette[row["predicate"]].get("port") == "item_fluid_energy"
            for row in document["structure"]
        )
        walls = sum(
            palette[row["predicate"]].get("block")
            == "cruciblecraft:multiblock/dense_tungstensteel_wall"
            for row in document["structure"]
        )
        self.assertEqual(27, len(document["structure"]))
        self.assertEqual(25, ports)
        self.assertEqual(25, walls)
        self.assertEqual(
            "gregtech.tileentity.multiblocks.MultiTileEntityImplosionCompressor",
            document["source"]["class"],
        )

    def test_art_manifest_points_to_real_files(self) -> None:
        manifest = load(ART)
        self.assertTrue(manifest["source_present"])
        self.assertGreaterEqual(len(manifest["imports"]), 25)
        for row in manifest["imports"]:
            relative = row["destination"].split(
                "assets/cruciblecraft/", 1
            )[-1]
            self.assertTrue(
                (ROOT / "src/main/resources/assets/cruciblecraft" / relative).is_file(),
                relative,
            )


if __name__ == "__main__":
    unittest.main()
