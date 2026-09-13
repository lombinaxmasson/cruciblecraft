#!/usr/bin/env python3
"""GT6 ordinary item-pipe runtime child."""
from __future__ import annotations

import hashlib
import unittest

from tools import capability_ledger as ledger
from tools import census_common as census
from tools import gt6_item_pipe_runtime as runtime

SLUG = "content/gt6-item-pipe-runtime"
ROOT = census.ROOT
WAVE = ROOT / "tools" / "waves" / "content" / "gt6-item-pipe-runtime"
CAPABILITY = (
    ROOT
    / "tools"
    / "capabilities"
    / "content"
    / "gt6-item-pipe-runtime"
    / "capability.json"
)
PLAN_ACTIVE = (
    ROOT / "docs" / "history" / "card-plans" / "active" / "GT6物品管运行时详细计划.md"
)
PLAN_CLOSED = (
    ROOT / "docs" / "history" / "card-plans" / "closed" / "GT6物品管运行时详细计划.md"
)
BE = runtime.BE
PHASE = runtime.PHASE
GAME_TESTS = runtime.GAME_TESTS
CORE_TESTS = runtime.CORE_TESTS
R0 = runtime.R0
LEDGER = runtime.LEDGER


class Gt6ItemPipeRuntimeTest(unittest.TestCase):
    def test_capability_depends_on_catalog_modern_ids_only(self) -> None:
        capability = census.load_json(CAPABILITY)
        self.assertEqual(SLUG, capability["slug"])
        self.assertEqual(["registry/catalog-modern-ids"], capability["depends_on"])
        self.assertNotIn("content/gt6-pipe-cable-baseline", capability["depends_on"])
        self.assertEqual(["capability-runtime"], capability["profiles"])
        self.assertEqual("runtime_ready", capability["maturity"])
        self.assertNotEqual("player_complete", capability["maturity"])
        self.assertEqual(
            [
                "itemPipeDisabledInputsOutputs",
                "itemPipeFullDoesNotVoid",
                "itemPipeHasInternalInventory",
                "itemPipeRestrictiveStepSize",
                "itemPipeTenTickSend",
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

    def test_inventory_send_disabled_io_and_isolation(self) -> None:
        be = BE.read_text(encoding="utf-8")
        phase = PHASE.read_text(encoding="utf-8")
        tests = GAME_TESTS.read_text(encoding="utf-8")
        core = CORE_TESTS.read_text(encoding="utf-8")
        self.assertIn("SEND_INTERVAL = 10", be)
        self.assertIn("storeIncoming", be)
        self.assertIn("sendStored", be)
        self.assertIn("cycleDisabledIo", be)
        self.assertIn("PipeTransferPhase.isDue", be)
        self.assertIn("tickCovers", be)
        self.assertIn("INTERVAL = 5", phase)
        self.assertNotIn("CableNetworkTraversal", be)
        self.assertNotIn("ModCapabilities.ENERGY", be)
        for name in runtime.EXPECTED_TESTS:
            self.assertIn(name, tests)
            self.assertNotIn(name, core)
        notes = census.load_json(WAVE / "runtime_notes.json")
        self.assertEqual(10, notes["inventory_send_ticks"])
        self.assertEqual(5, notes["cover_phase_ticks"])
        self.assertFalse(notes["restrictive_registered"])
        self.assertFalse(notes["energy_capability"])
        self.assertEqual("explicitly_blocked", notes["obtain"])
        self.assertEqual("runtime_ready", notes["close_target"])

    def test_execution_subset_folds_only_exact_live_hosts(self) -> None:
        subset = census.load_json(WAVE / "execution_subset.json")
        baseline = census.load_json(LEDGER)
        self.assertEqual(SLUG, subset["capability_slug"])
        self.assertEqual(57, int(subset["counts"]["fold_live_block"]))
        self.assertEqual(63, int(subset["counts"]["in_catalog_item"]))
        self.assertEqual(0, int(subset["counts"]["upgrade_live_item"]))
        by_meta = {int(row["meta"]): row for row in baseline["rows"]}
        folded = [
            row
            for row in subset["rows"]
            if row["disposition"] == "fold_live_block"
        ]
        self.assertEqual(57, len(folded))
        for row in folded:
            source = by_meta[int(row["meta"])]
            self.assertEqual("fold_live_block", source["disposition"])
            self.assertEqual("item", source["domain"])
            self.assertTrue(source["in_catalog_1817"])
            self.assertEqual(source["live_block"], row["live_block"])
            self.assertIn(source["cc_form"], runtime.ORDINARY_FORMS)
            self.assertFalse((source.get("spec") or {}).get("restrictive"))
        keep = [
            row
            for row in subset["rows"]
            if row["disposition"] == "keep_distinct"
        ]
        self.assertEqual(6, len(keep))
        self.assertTrue(
            all("restrictive" in str(row.get("dummy_path") or "") for row in keep)
        )
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
            / "cruciblecraft_wave_content_gt6_item_pipe_runtime"
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
