#!/usr/bin/env python3
"""GT6 EU wire/cable runtime child."""
from __future__ import annotations

import hashlib
import unittest

from tools import capability_ledger as ledger
from tools import census_common as census
from tools import gt6_eu_missing_wire_gauges_runtime as missing_gauges
from tools import gt6_eu_wire_cable_runtime as runtime

SLUG = "content/gt6-eu-wire-cable-runtime"
ROOT = census.ROOT
WAVE = ROOT / "tools" / "waves" / "content" / "gt6-eu-wire-cable-runtime"
CAPABILITY = (
    ROOT
    / "tools"
    / "capabilities"
    / "content"
    / "gt6-eu-wire-cable-runtime"
    / "capability.json"
)
PLAN_ACTIVE = (
    ROOT / "docs" / "history" / "card-plans" / "active" / "GT6导线电缆运行时详细计划.md"
)
PLAN_CLOSED = (
    ROOT / "docs" / "history" / "card-plans" / "closed" / "GT6导线电缆运行时详细计划.md"
)
CATALOG_JAVA = runtime.CATALOG_JAVA
GAME_TESTS = runtime.GAME_TESTS
CORE_TESTS = runtime.CORE_TESTS
R0 = runtime.R0
LEDGER = runtime.LEDGER


class Gt6EuWireCableRuntimeTest(unittest.TestCase):
    def test_capability_depends_on_fold_and_catalog_ids(self) -> None:
        capability = census.load_json(CAPABILITY)
        self.assertEqual(SLUG, capability["slug"])
        self.assertEqual(
            [
                "content/electric-wire-cable-mte-fold",
                "registry/catalog-modern-ids",
            ],
            capability["depends_on"],
        )
        self.assertNotIn(
            "content/gt6-pipe-cable-baseline", capability["depends_on"]
        )
        self.assertEqual(["capability-runtime"], capability["profiles"])
        self.assertEqual("runtime_ready", capability["maturity"])
        self.assertNotEqual("player_complete", capability["maturity"])
        self.assertEqual(
            [
                "cableLossAndOverloadPerSpecification",
                "electricWireGt01IsCableBlock",
                "higherWireGaugesArePlaceableOrExplicitlyUpgrade",
                "redstoneMaterialsAreNotElectricalConductors",
            ],
            sorted(capability["required_test_ids"]),
        )
        compiled = ledger.compile_ledger()
        if capability["workflow"] == "active":
            self.assertEqual(SLUG, compiled["unique_active_slug"])
            self.assertTrue(PLAN_ACTIVE.is_file())
            self.assertFalse(PLAN_CLOSED.is_file())
        else:
            self.assertEqual("accepted", capability["workflow"])
            self.assertNotEqual(SLUG, compiled["unique_active_slug"])
            self.assertFalse(PLAN_ACTIVE.is_file())
            self.assertTrue(PLAN_CLOSED.is_file())

    def test_catalog_excludes_redstone_and_maps_gauges(self) -> None:
        catalog = CATALOG_JAVA.read_text(encoding="utf-8")
        tests = GAME_TESTS.read_text(encoding="utf-8")
        core = CORE_TESTS.read_text(encoding="utf-8")
        if missing_gauges.folded_metas():
            self.assertIn("EXPECTED_WIRE_BLOCKS = 473", catalog)
            self.assertIn("wireGt07", catalog)
        else:
            self.assertIn("EXPECTED_WIRE_BLOCKS = 231", catalog)
            self.assertNotIn("wireGt07", catalog)
        self.assertIn("EXPECTED_CABLE_BLOCKS = 141", catalog)
        self.assertIn("DOUBLE_WIRE", catalog)
        self.assertIn("HEXADECUPLE_WIRE", catalog)
        self.assertIn("red_alloy", catalog)
        self.assertIn('case "cableGt08" -> 12', catalog)
        notes = census.load_json(WAVE / "runtime_notes.json")
        self.assertEqual(231, notes["expected_wires"])
        self.assertEqual(115, notes["expected_cables"])
        self.assertTrue(notes["recipe_mapped_is_not_deletion"])
        self.assertEqual("explicitly_blocked", notes["obtain"])
        self.assertEqual("runtime_ready", notes["close_target"])
        for name in runtime.EXPECTED_TESTS:
            self.assertIn(name, tests)
            self.assertNotIn(f"void {name}", core)

    def test_execution_subset_folds_only_exact_live_hosts(self) -> None:
        subset = census.load_json(WAVE / "execution_subset.json")
        baseline = census.load_json(LEDGER)
        self.assertEqual(SLUG, subset["capability_slug"])
        self.assertEqual(195, int(subset["counts"]["fold_live_block"]))
        self.assertEqual(404, int(subset["counts"]["in_catalog_eu"]))
        self.assertEqual(20, int(subset["counts"]["upgrade_live_item"]))
        self.assertEqual(12, int(subset["counts"]["already_shared"]))
        self.assertEqual(177, int(subset["counts"]["keep_distinct"]))
        by_meta = {int(row["meta"]): row for row in baseline["rows"]}
        folded = [
            row
            for row in subset["rows"]
            if row["disposition"] == "fold_live_block"
        ]
        self.assertEqual(195, len(folded))
        for row in folded:
            source = by_meta[int(row["meta"])]
            self.assertEqual("fold_live_block", source["disposition"])
            self.assertEqual("eu", source["domain"])
            self.assertTrue(source["in_catalog_1817"])
            self.assertEqual(source["live_block"], row["live_block"])
            self.assertIn(source["cc_form"], runtime.MAPPED_FORMS)
            self.assertTrue(
                str(row.get("dummy_path") or "").startswith("electric_wire/")
            )
        keep = [
            row
            for row in subset["rows"]
            if row["disposition"] == "keep_distinct"
        ]
        self.assertEqual(177, len(keep))
        self.assertTrue(
            any(
                str(row.get("dummy_path") or "").endswith("7x_tin_wire")
                for row in keep
            )
        )
        upgrades = [
            row
            for row in subset["rows"]
            if row["disposition"] == "upgrade_live_item"
        ]
        self.assertEqual(20, len(upgrades))
        self.assertTrue(
            all(str(row.get("dummy_path") or "").startswith("electric_wire/")
                for row in upgrades)
        )
        self.assertTrue(
            all(row.get("live_block") is None for row in upgrades)
        )
        bath_items = {
            str(identity.get("registry_path") or "")
            for identity in census.load_json(runtime.BATH_DATA_CATALOG).get(
                "identities"
            )
            or []
            if identity.get("registry_kind") == "item"
        }
        smelter_items = {
            str(identity.get("registry_path") or "")
            for identity in census.load_json(runtime.DATA_CATALOG).get(
                "identities"
            )
            or []
            if identity.get("registry_kind") == "item"
        }
        self.assertEqual(set(), bath_items & smelter_items)
        identities = census.load_json(runtime.DATA_CATALOG).get("identities") or []
        if missing_gauges.folded_metas():
            lead = next(row for row in identities if int(row["meta"]) == 28106)
            self.assertEqual("existing_item", lead["registry_kind"])
            self.assertEqual("lead/septuple_wire", lead["registry_path"])
        else:
            lead = next(
                identity
                for identity in identities
                if identity.get("registry_path") == "electric_wire/7x_lead_wire"
            )
            self.assertEqual("existing_item", lead["registry_kind"])
        self.assertEqual(
            hashlib.sha256(R0.read_bytes()).hexdigest(),
            (WAVE / "r0_disposition_sha256.txt").read_text(encoding="utf-8").strip(),
        )
        self.assertEqual(
            hashlib.sha256(LEDGER.read_bytes()).hexdigest(),
            (WAVE / "baseline_ledger_sha256.txt")
            .read_text(encoding="utf-8")
            .strip(),
        )
        ns = (
            ROOT
            / "src"
            / "main"
            / "resources"
            / "data"
            / "cruciblecraft_wave_content_gt6_eu_wire_cable_runtime"
        )
        self.assertTrue((ns / "structure" / "empty.nbt").is_file())
        self.assertTrue((ns / "gametest" / "structure" / "empty.nbt").is_file())

    def test_unique_active_docs_and_ledger_hand_off(self) -> None:
        topology = census.load_json(WAVE / "topology.json")
        readiness = census.load_json(WAVE / "readiness.json")
        compiled = ledger.compile_ledger()
        capability = census.load_json(CAPABILITY)
        self.assertEqual([], runtime.check())
        if capability["workflow"] == "active":
            self.assertEqual(SLUG, compiled["unique_active_slug"])
            self.assertEqual(SLUG, topology["unique_active_wave"])
            self.assertEqual(SLUG, readiness["unique_active_wave"])
        else:
            self.assertNotEqual(SLUG, compiled["unique_active_slug"])
            self.assertIsNone(topology["unique_active_wave"])
            self.assertIsNone(readiness["unique_active_wave"])


if __name__ == "__main__":
    unittest.main()
