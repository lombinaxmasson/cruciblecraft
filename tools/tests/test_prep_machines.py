#!/usr/bin/env python3
"""Prep-only machines other than roll-former / cluster-mill."""
from __future__ import annotations

import importlib.util
import unittest
from pathlib import Path

from tools import census_common as census
from tools.build_recipe_bulk import WAVE_CHOICES
from tools.recipe_bulk.slugs import KNOWN_SEMANTIC_SLUGS
from tools.recipe_bulk.waves import SEMANTIC_COMPILE_ORDER, recipe_wave

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
EXPECTED = {
    "slicer": (33, 32, 1),
    "loom": (1334, 1129, 205),
    "pressure-washer": (312, 192, 120),
    "injector": (638, 611, 27),
    "printer": (22, 0, 22),
    "laminator": (498, 232, 266),
    "melter": (6756, 3960, 2796),
    "nanofab": (64, 52, 12),
    "sanding": (7637, 7637, 0),
}
NEEDLES = {
    "printer": ("printer", "PrinterPrepSpec"),
}


def _load_builder():
    path = ROOT / "tools" / "waves" / "prep" / "build_prep_machines.py"
    spec = importlib.util.spec_from_file_location("prep_machines_builder", path)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    spec.loader.exec_module(module)
    return module


prep = _load_builder()
OPEN_PREP_MACHINES = ("printer",)


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


class PrepMachinesTest(unittest.TestCase):
    def test_prep_check_passes_isolated_compile(self) -> None:
        for name in OPEN_PREP_MACHINES:
            with self.subTest(name=name):
                self.assertEqual([], prep.check_machine(name))

    def test_dump_lock_and_overflow_match_the_card_denominators(self) -> None:
        for name, (source_rows, selected, overflow_rows) in EXPECTED.items():
            with self.subTest(name=name):
                wave = prep.common.wave_dir(name)
                work = census.load_json(wave / "source_pack" / "work_set.json")
                overflow = census.load_json(wave / "overflow.json")
                lock = census.load_json(wave / "production_lock.json")
                accounting = work["accounting"]
                self.assertEqual(source_rows, accounting["source_rows"])
                if selected is None:
                    selected = accounting["selected_rows"]
                    overflow_rows = accounting["overflow_rows"]
                    self.assertEqual(source_rows, selected + overflow_rows)
                self.assertEqual(selected, accounting["selected_rows"])
                self.assertEqual(overflow_rows, accounting["overflow_rows"])
                self.assertEqual(overflow_rows, overflow["blocked_rows"])
                self.assertEqual(selected, lock["production"]["relation_count"])
                self.assertTrue(lock["production_authority"])
                self.assertIn("not player_complete", lock["note"])
                self.assertNotIn("programmed_circuit", str(overflow))

    def test_d0_hosts_are_exact_or_blocked_without_stand_ins(self) -> None:
        slicer = census.load_json(prep.common.wave_dir("slicer") / "d0_obtain_matrix.json")
        self.assertEqual(["PRw", "YMC"], slicer["grid"])
        slicer_status = {row["host"]: row["status"] for row in slicer["hosts"]}
        self.assertEqual(
            {
                20381: "source_exact",
                20382: "source_exact",
                20383: "source_exact",
                20384: "source_exact",
                20385: "source_exact",
            },
            slicer_status,
        )
        ev = next(row for row in slicer["hosts"] if row["host"] == 20384)
        self.assertEqual("ok", ev["piston"]["status"])
        self.assertEqual("ok", ev["conveyor"]["status"])

        loom = census.load_json(prep.common.wave_dir("loom") / "d0_obtain_matrix.json")
        loom_status = {row["host"]: row["status"] for row in loom["hosts"]}
        self.assertEqual("source_exact", loom_status[20211])
        self.assertEqual("source_exact", loom_status[20214])
        self.assertEqual("source_exact", loom_status[20361])
        self.assertEqual("source_exact", loom_status[20364])
        self.assertEqual("source_exact", loom_status[20362])
        self.assertEqual("source_exact", loom_status[20363])
        self.assertEqual("source_exact", loom_status[20365])
        self.assertEqual(5_000, census.load_json(
            prep.common.wave_dir("loom") / "runtime_notes.json"
        )["electric_efficiency_permille"])

        washer = census.load_json(
            prep.common.wave_dir("pressure-washer") / "d0_obtain_matrix.json"
        )
        self.assertEqual(
            {20551, 20552, 20553, 20554},
            {row["host"] for row in washer["hosts"] if row["status"] == "source_exact"},
        )
        injector = census.load_json(
            prep.common.wave_dir("injector") / "d0_obtain_matrix.json"
        )
        inj = {row["host"]: row["status"] for row in injector["hosts"]}
        self.assertEqual("source_exact", inj[20264])
        self.assertEqual("source_exact", inj[20261])

        printer = census.load_json(
            prep.common.wave_dir("printer") / "d0_obtain_matrix.json"
        )
        printer_status = {row["host"]: row["status"] for row in printer["hosts"]}
        self.assertEqual("source_exact", printer_status[20271])
        self.assertEqual("source_exact", printer_status[20274])
        self.assertTrue(all(status == "source_exact" for status in printer_status.values()))
        self.assertEqual(
            "cruciblecraft:compact_electric_conveyor_lv",
            next(row for row in printer["hosts"] if row["host"] == 20271)["conveyor"]["cc"],
        )

        laminator = census.load_json(
            prep.common.wave_dir("laminator") / "d0_obtain_matrix.json"
        )
        self.assertTrue(all(row["status"] == "source_exact" for row in laminator["hosts"]))

        melter = census.load_json(prep.common.wave_dir("melter") / "d0_obtain_matrix.json")
        host = melter["hosts"][0]
        self.assertEqual("source_exact", host["status"])
        self.assertEqual("cruciblecraft:foundry/smelting_crucible_steel", host["crucible"]["cc"])
        self.assertEqual("minecraft:bricks", host["bricks"]["cc"])
        self.assertEqual(
            1000,
            census.load_json(prep.common.wave_dir("melter") / "runtime_notes.json")[
                "parallel"
            ],
        )

        nanofab = census.load_json(
            prep.common.wave_dir("nanofab") / "d0_obtain_matrix.json"
        )
        self.assertTrue(all(row["status"] == "source_exact" for row in nanofab["hosts"]))
        sanding = census.load_json(
            prep.common.wave_dir("sanding") / "d0_obtain_matrix.json"
        )
        self.assertEqual(["SGS", "XXX", "wMh"], sanding["grid"])
        self.assertTrue(all(row["status"] == "source_exact" for row in sanding["hosts"]))
        self.assertEqual(
            "minecraft:sandstone",
            next(row for row in sanding["hosts"] if row["host"] == 20511)["sandstone"]["cc"],
        )
        self.assertEqual(
            "UP",
            census.load_json(prep.common.wave_dir("sanding") / "runtime_notes.json")[
                "energy_accepted_sides"
            ],
        )
        gate = census.load_json(prep.common.wave_dir("sanding") / "landing_gate.json")
        self.assertIsNone(gate["landing_blocked_by"])
        self.assertEqual(prep.sanding_landing_gate(), gate)
        self.assertIsNone(
            census.load_json(ROOT / "tools" / "capabilities" / "ledger.json")[
                "unique_active_slug"
            ]
        )
        oven = census.load_json(
            prep.common.wave_dir("oven") / "d0_obtain_matrix.json"
        )
        self.assertEqual(["wMh", "BCB"], oven["grid"])
        self.assertTrue(all(row["status"] == "source_exact" for row in oven["hosts"]))
        self.assertEqual(
            "DOWN",
            census.load_json(prep.common.wave_dir("oven") / "runtime_notes.json")[
                "energy_accepted_sides"
            ],
        )
        blob = str(slicer) + str(loom) + str(printer) + str(nanofab) + str(injector) + str(sanding) + str(oven)
        self.assertNotIn("programmed_circuit", blob)
        from tools.technological_parts_foundation import has_split_module_standin
        self.assertFalse(has_split_module_standin(blob))

    def test_live_compile_and_known_waves_stay_closed(self) -> None:
        for name in prep.MACHINES:
            slug = f"prep/{name}"
            with self.subTest(slug=slug):
                if prep.MACHINES[name].get("skip_source_pack"):
                    with self.assertRaises((ValueError, KeyError)):
                        recipe_wave(slug)
                else:
                    with self.assertRaisesRegex(ValueError, "src/recipe_generated"):
                        recipe_wave(slug)
                self.assertNotIn(slug, SEMANTIC_COMPILE_ORDER)
                self.assertNotIn(slug, WAVE_CHOICES)
                self.assertNotIn(slug, KNOWN_SEMANTIC_SLUGS)
        live = ROOT / "src" / "recipe_generated" / "resources" / "data" / "cruciblecraft" / "recipe"
        for needle in ("printer",):
            self.assertEqual([], list(live.rglob(f"*{needle}*")))
        self.assertEqual([], list(live.rglob("*electricloom*")))
        self.assertTrue(
            any(
                path.name.lower() == "melter"
                for path in live.rglob("*melter*")
            )
        )

    def test_landing_owned_paths_do_not_register_the_machines(self) -> None:
        processing = _read(JAVA / "registry" / "ModProcessingMachines.java")
        maps = _read(JAVA / "registry" / "ModRecipeMaps.java")
        for name, needles in NEEDLES.items():
            with self.subTest(name=name):
                spec_name = needles[-1]
                self.assertNotIn(spec_name, processing)
                self.assertNotIn(spec_name, maps)
        for path in LANDING_JAVA + LANDING_DATA:
            text = _read(path)
            lowered = text.lower()
            self.assertNotIn("printer", lowered, path.name)
        ledger = census.load_json(ROOT / "tools" / "blocked_recipe_ledger.json")
        self.assertIsNone(ledger["unique_active_wave"])

    def test_source_art_is_copied_and_not_aliased(self) -> None:
        manifests = [
            machine["art"]["manifest"]
            for machine in prep.MACHINES.values()
        ] + [
            extra["manifest"]
            for machine in prep.MACHINES.values()
            for extra in machine.get("extra_art") or ()
        ]
        gt6_root = ROOT / "gt6_referencable_port_code" / "gregtech6_w"
        self.assertTrue(gt6_root.is_dir())
        for name in manifests:
            with self.subTest(manifest=name):
                manifest = census.load_json(ASSETS / name)
                self.assertGreaterEqual(len(manifest["imports"]), 20)
                for row in manifest["imports"]:
                    destination = ROOT / "src" / "main" / "resources" / row["destination"]
                    source = gt6_root / row["gt6_source"]
                    self.assertTrue(destination.is_file(), row["destination"])
                    self.assertTrue(source.is_file(), row["gt6_source"])
                    self.assertEqual(source.read_bytes(), destination.read_bytes())
                    self.assertNotIn("multiblock_casing", row["destination"])
        self.assertTrue((ASSETS / "textures" / "gui" / "machines" / "slicer.png").is_file())
        self.assertTrue((ASSETS / "textures" / "gui" / "machines" / "melter.png").is_file())
        self.assertTrue((ASSETS / "textures" / "gui" / "machines" / "sanding.png").is_file())
        self.assertTrue((ASSETS / "textures" / "gui" / "machines" / "oven.png").is_file())
        self.assertTrue(
            (ASSETS / "textures" / "block" / "machine" / "sander" / "colored" / "front.png").is_file()
        )
        self.assertTrue(
            (ASSETS / "textures" / "block" / "machine" / "oven" / "colored" / "front.png").is_file()
        )
        self.assertTrue(
            (ASSETS / "textures" / "block" / "machine" / "debarker" / "colored" / "front.png").is_file()
        )
        self.assertTrue(
            (ASSETS / "textures" / "block" / "machine" / "electricloom" / "colored" / "front.png").is_file()
        )


if __name__ == "__main__":
    unittest.main()
