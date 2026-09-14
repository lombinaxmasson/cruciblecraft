#!/usr/bin/env python3
"""GT6 MTE dummy identities fold onto live hosts."""
from __future__ import annotations

import hashlib
import unittest

from tools import capability_ledger as ledger
from tools import census_common as census
from tools import gt6_mte_host_fold as runtime

ROOT = census.ROOT
CONVERTER_SLUG = "content/gt6-mte-converter-host-fold"
HOPPER_SLUG = "content/gt6-mte-hopper-host-fold"
PROCESSING_SLUG = "content/gt6-mte-processing-host-fold"
REACTOR_SLUG = "content/gt6-mte-reactor-rod-host-fold"
FOLD_SLUGS = (CONVERTER_SLUG, HOPPER_SLUG, PROCESSING_SLUG, REACTOR_SLUG)
R0 = runtime.R0
LEDGER = runtime.LEDGER


def _capability(slug: str) -> dict:
    path = (
        ROOT
        / "tools"
        / "capabilities"
        / slug
        / "capability.json"
    )
    if not path.is_file():
        return {}
    return census.load_json(path)


def _plan_paths(domain: str) -> tuple[object, object]:
    stem = runtime.DOMAINS[domain]["plan_stem"]
    return (
        ROOT / "docs" / "history" / "card-plans" / "active" / stem,
        ROOT / "docs" / "history" / "card-plans" / "closed" / stem,
    )


class Gt6MteHostFoldTest(unittest.TestCase):
    def test_converter_capability_is_catalog_fold_only(self) -> None:
        capability = _capability(CONVERTER_SLUG)
        self.assertEqual(CONVERTER_SLUG, capability["slug"])
        self.assertEqual(["registry/catalog-modern-ids"], capability["depends_on"])
        self.assertEqual(["capability-runtime"], capability["profiles"])
        self.assertNotEqual("player_complete", capability["maturity"])
        self.assertEqual(
            sorted(runtime.DOMAINS["converter"]["tests"]),
            sorted(capability["required_test_ids"]),
        )
        compiled = ledger.compile_ledger()
        active, closed = _plan_paths("converter")
        if capability["workflow"] == "active":
            self.assertEqual(CONVERTER_SLUG, compiled["unique_active_slug"])
            self.assertTrue(active.is_file())
            self.assertFalse(closed.is_file())
        else:
            self.assertEqual("accepted", capability["workflow"])
            self.assertEqual("runtime_ready", capability["maturity"])
            self.assertNotEqual(CONVERTER_SLUG, compiled["unique_active_slug"])
            self.assertFalse(active.is_file())
            self.assertTrue(closed.is_file())

    def test_converter_overlay_folds_seventy_one_live_hosts(self) -> None:
        wave = runtime._wave("converter")
        overlay = census.load_json(wave / "fold_overlay.json")
        self.assertEqual(CONVERTER_SLUG, overlay["capability_slug"])
        self.assertEqual(71, int(overlay["counts"]["fold_live_block"]))
        self.assertEqual(8, int(overlay["counts"]["keep_distinct"]))
        by_meta = {int(row["meta"]): row for row in overlay["rows"]}
        self.assertEqual("cruciblecraft:lead_boiler", by_meta[1200]["live_block"])
        self.assertEqual(
            "cruciblecraft:tantalum_hafnium_carbide_burning_box_solid",
            by_meta[1109]["live_block"],
        )
        keep = {int(row["meta"]) for row in overlay["keep_distinct"]}
        self.assertEqual(
            {1512, 1522, 1528, 1530, 1545, 1548, 10086, 10087},
            keep,
        )
        self.assertEqual(
            hashlib.sha256(R0.read_bytes()).hexdigest(),
            (wave / "r0_disposition_sha256.txt").read_text(encoding="utf-8").strip(),
        )
        self.assertEqual(
            hashlib.sha256(LEDGER.read_bytes()).hexdigest(),
            (wave / "baseline_ledger_sha256.txt")
            .read_text(encoding="utf-8")
            .strip(),
        )

    def test_hopper_overlay_folds_one_hundred_one_live_hosts(self) -> None:
        wave = runtime._wave("hopper")
        if not (wave / "fold_overlay.json").is_file():
            self.skipTest("hopper overlay not issued yet")
        overlay = census.load_json(wave / "fold_overlay.json")
        self.assertEqual(HOPPER_SLUG, overlay["capability_slug"])
        self.assertEqual(101, int(overlay["counts"]["fold_live_block"]))
        self.assertEqual(0, int(overlay["counts"]["keep_distinct"]))
        by_meta = {int(row["meta"]): row for row in overlay["rows"]}
        self.assertEqual("cruciblecraft:lead_hopper", by_meta[8000]["live_block"])
        self.assertEqual(
            "cruciblecraft:lead_queue_hopper", by_meta[8200]["live_block"]
        )
        self.assertEqual(
            "cruciblecraft:steel_dust_funnel", by_meta[32704]["live_block"]
        )

    def test_processing_overlay_folds_source_id_hosts_only(self) -> None:
        wave = runtime._wave("processing")
        if not (wave / "fold_overlay.json").is_file():
            self.skipTest("processing overlay not issued yet")
        overlay = census.load_json(wave / "fold_overlay.json")
        self.assertEqual(PROCESSING_SLUG, overlay["capability_slug"])
        self.assertEqual(58, int(overlay["counts"]["fold_live_block"]))
        self.assertEqual(28, int(overlay["counts"]["keep_distinct"]))
        by_meta = {int(row["meta"]): row for row in overlay["rows"]}
        self.assertEqual("cruciblecraft:sifter", by_meta[20051]["live_block"])
        keep = {int(row["meta"]) for row in overlay["keep_distinct"]}
        self.assertIn(15001, keep)
        self.assertIn(20071, keep)
        self.assertIn(20221, keep)
        self.assertIn(20301, keep)
        self.assertIn(20321, keep)

    def test_reactor_overlay_folds_meta_9203_only(self) -> None:
        wave = runtime._wave("reactor")
        if not (wave / "fold_overlay.json").is_file():
            self.skipTest("reactor overlay not issued yet")
        overlay = census.load_json(wave / "fold_overlay.json")
        self.assertEqual(REACTOR_SLUG, overlay["capability_slug"])
        self.assertEqual(1, int(overlay["counts"]["fold_live_block"]))
        self.assertEqual(0, int(overlay["counts"]["keep_distinct"]))
        row = overlay["rows"][0]
        self.assertEqual(9203, int(row["meta"]))
        self.assertEqual("neutron/reflector_rod", row["dummy_path"])
        self.assertEqual(
            "cruciblecraft:neutron_reflector_rod", row["live_block"]
        )

    def test_issued_overlays_and_unique_active_hand_off(self) -> None:
        compiled = ledger.compile_ledger()
        self.assertEqual([], runtime.check())
        active = [
            slug
            for slug in FOLD_SLUGS
            if _capability(slug).get("workflow") == "active"
        ]
        self.assertLessEqual(len(active), 1)
        if active:
            self.assertEqual(active[0], compiled["unique_active_slug"])
        for domain, spec in runtime.DOMAINS.items():
            overlay = runtime._wave(domain) / "fold_overlay.json"
            if not overlay.is_file():
                continue
            topology = census.load_json(runtime._wave(domain) / "topology.json")
            readiness = census.load_json(runtime._wave(domain) / "readiness.json")
            capability = _capability(spec["slug"])
            if capability.get("workflow") == "active":
                self.assertEqual(spec["slug"], topology["unique_active_wave"])
                self.assertEqual(spec["slug"], readiness["unique_active_wave"])
            else:
                self.assertIsNone(topology["unique_active_wave"])
                self.assertIsNone(readiness["unique_active_wave"])


if __name__ == "__main__":
    unittest.main()
