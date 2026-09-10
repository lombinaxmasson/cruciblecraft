#!/usr/bin/env python3
"""Prep-only Cluster Mill: exact D0, 307-row lock/overflow, no live landing."""
from __future__ import annotations

import importlib.util
import unittest
from pathlib import Path

from tools import census_common as census
from tools.build_recipe_bulk import WAVE_CHOICES
from tools.recipe_bulk.slugs import KNOWN_SEMANTIC_SLUGS
from tools.recipe_bulk.waves import SEMANTIC_COMPILE_ORDER, recipe_wave


def _load_prep():
    path = (
        census.ROOT
        / "tools"
        / "waves"
        / "prep"
        / "cluster-mill"
        / "build_cluster_mill_prep.py"
    )
    spec = importlib.util.spec_from_file_location("cluster_mill_prep_builder", path)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    spec.loader.exec_module(module)
    return module


prep = _load_prep()

ROOT = census.ROOT
ASSETS = ROOT / "src" / "main" / "resources" / "assets" / "cruciblecraft"
JAVA = ROOT / "src" / "main" / "java" / "com" / "masson" / "cruciblecraft"
LANDING_JAVA = (
    JAVA / "registry" / "ModBlocks.java",
    JAVA / "registry" / "ModItems.java",
    JAVA / "registry" / "ModBlockEntities.java",
    JAVA / "registry" / "ModMenus.java",
    JAVA / "registry" / "ModRecipeMaps.java",
    JAVA / "registry" / "ModProcessingMachines.java",
    JAVA / "registry" / "ModCapabilities.java",
    JAVA / "api" / "energy" / "EnergyType.java",
)
LANDING_DATA = (
    ROOT / "src" / "main" / "resources" / "data" / "cruciblecraft" / "machine_kinds.json",
    ROOT / "src" / "main" / "resources" / "data" / "cruciblecraft" / "machine_tiers.json",
    ROOT / "src" / "main" / "resources" / "data" / "cruciblecraft" / "machine_acquisition.json",
    ROOT / "src" / "main" / "resources" / "data" / "cruciblecraft" / "machine_delivery.json",
)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


class ClusterMillPrepTest(unittest.TestCase):
    def test_prep_check_passes_isolated_compile(self) -> None:
        self.assertEqual([], prep.check())

    def test_d0_hosts_are_exact_or_blocked_without_stand_ins(self) -> None:
        document = census.load_json(prep.WAVE / "d0_obtain_matrix.json")
        self.assertEqual("SSS wGh SMS".split(), document["grid"])
        statuses = {row["host"]: row["status"] for row in document["hosts"]}
        self.assertEqual(
            {
                20141: "source_exact",
                20142: "source_exact",
                20143: "source_exact",
                20144: "source_exact",
            },
            statuses,
        )
        for row in document["hosts"]:
            casing = row["casing"]
            self.assertEqual("ok", casing["status"])
            self.assertTrue(casing["cc"].endswith("/machine_casing_quadruple"))
            self.assertEqual("ok", row["gear"]["status"])
            self.assertNotIn("programmed_circuit", str(row))
            self.assertNotIn("machine_casing_double", casing["cc"])

    def test_dump_lock_and_overflow_are_explicit(self) -> None:
        work = census.load_json(prep.WORK_SET)
        overflow = census.load_json(prep.OVERFLOW)
        source = census.load_json(prep.WAVE / "source.json")
        lock = census.load_json(prep.PRODUCTION_LOCK)
        accounting = work["accounting"]
        self.assertEqual(307, accounting["source_rows"])
        self.assertEqual(307, accounting["selected_rows"])
        self.assertEqual(0, accounting["overflow_rows"])
        self.assertEqual(307, source["relation_count"])
        self.assertEqual(307, lock["production"]["relation_count"])
        self.assertEqual(1, lock["production"]["family_count"])
        self.assertTrue(lock["production_authority"])
        self.assertIn("not player_complete", lock["note"])
        self.assertEqual(0, overflow["blocked_rows"])
        self.assertEqual([], overflow.get("overflow") or [])
        self.assertNotIn("programmed_circuit", str(overflow))
        first = source["relations"][0]
        self.assertEqual(["netherite:plate"], [row["value"] for row in first["item_inputs"]])
        self.assertEqual(["netherite:foil"], [row["value"] for row in first["item_outputs"]])
        self.assertEqual([1], first["item_input_counts"])
        self.assertEqual(4, first["item_outputs"][0]["source"]["count"])
        self.assertEqual(16, first["eut"])

    def test_live_compile_and_known_waves_stay_closed(self) -> None:
        with self.assertRaisesRegex(ValueError, "src/recipe_generated"):
            recipe_wave("prep/cluster-mill")
        self.assertNotIn("prep/cluster-mill", SEMANTIC_COMPILE_ORDER)
        self.assertNotIn("prep/cluster-mill", WAVE_CHOICES)
        self.assertNotIn("prep/cluster-mill", KNOWN_SEMANTIC_SLUGS)
        self.assertIn("machines/cluster-mill", KNOWN_SEMANTIC_SLUGS)

    def test_prep_spec_stays_unregistered_while_live_hosts_land(self) -> None:
        spec = _read(
            JAVA / "machine" / "processing" / "prep" / "ClusterMillPrepSpec.java"
        )
        self.assertIn("UNREGISTERED_MAP", spec)
        self.assertIn("KINETIC_ROTATION", spec)
        processing = _read(JAVA / "registry" / "ModProcessingMachines.java")
        self.assertNotIn("ClusterMillPrepSpec", processing)
        maps = _read(JAVA / "registry" / "ModRecipeMaps.java")
        self.assertIn("create(\"clustermill\")", maps)
        self.assertIn("CLUSTERMILL", processing)
        ledger = census.load_json(ROOT / "tools" / "blocked_recipe_ledger.json")
        self.assertIsNone(ledger["unique_active_wave"])

    def test_source_art_is_copied_and_not_aliased(self) -> None:
        manifest = census.load_json(
            ASSETS / "gt6_cluster_mill_art_manifest.json"
        )
        self.assertGreaterEqual(len(manifest["imports"]), 24)
        gt6_root = ROOT / "gt6_referencable_port_code" / "gregtech6_w"
        self.assertTrue(gt6_root.is_dir())
        for row in manifest["imports"]:
            destination = ROOT / "src" / "main" / "resources" / row["destination"]
            source = gt6_root / row["gt6_source"]
            self.assertTrue(destination.is_file(), row["destination"])
            self.assertTrue(source.is_file(), row["gt6_source"])
            self.assertEqual(
                source.read_bytes(),
                destination.read_bytes(),
                row["destination"],
            )
            self.assertNotIn("multiblock_casing", row["destination"])
            self.assertNotIn("heat_exchanger", row["destination"])
        gui = ASSETS / "textures" / "gui" / "machines" / "clustermill.png"
        self.assertTrue(gui.is_file())


if __name__ == "__main__":
    unittest.main()
