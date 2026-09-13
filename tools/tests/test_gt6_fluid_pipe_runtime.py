#!/usr/bin/env python3
"""GT6 five-gauge fluid-pipe runtime child."""
from __future__ import annotations

import hashlib
import unittest

from tools import capability_ledger as ledger
from tools import census_common as census
from tools import gt6_fluid_pipe_runtime as runtime

SLUG = "content/gt6-fluid-pipe-runtime"
ROOT = census.ROOT
WAVE = ROOT / "tools" / "waves" / "content" / "gt6-fluid-pipe-runtime"
CAPABILITY = (
    ROOT
    / "tools"
    / "capabilities"
    / "content"
    / "gt6-fluid-pipe-runtime"
    / "capability.json"
)
PLAN_ACTIVE = (
    ROOT / "docs" / "history" / "card-plans" / "active" / "GT6流体管运行时详细计划.md"
)
PLAN_CLOSED = (
    ROOT / "docs" / "history" / "card-plans" / "closed" / "GT6流体管运行时详细计划.md"
)
BE = runtime.BE
PHASE = runtime.PHASE
GAME_TESTS = runtime.GAME_TESTS
CORE_TESTS = runtime.CORE_TESTS
R0 = runtime.R0
LEDGER = runtime.LEDGER


class Gt6FluidPipeRuntimeTest(unittest.TestCase):
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
                "fluidPipeCapacityMatchesGt6Gauge",
                "fluidPipeChunkUnloadRetainsTank",
                "fluidPipePlasmaMagicAcidGasFailures",
                "fluidPipeSideContract",
                "fluidPipeTickCadenceMatchesGt6",
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

    def test_cadence_capacity_fail_closed_and_isolation(self) -> None:
        be = BE.read_text(encoding="utf-8")
        phase = PHASE.read_text(encoding="utf-8")
        tests = GAME_TESTS.read_text(encoding="utf-8")
        core = CORE_TESTS.read_text(encoding="utf-8")
        self.assertIn("pipe.distribute(level);", be)
        self.assertIn("PipeTransferPhase.isDue", be)
        self.assertIn("tickCovers", be)
        self.assertNotIn("8_000", be)
        self.assertIn("INTERVAL = 5", phase)
        self.assertIn("FluidPipeBlockedMedia.rejects", be)
        self.assertIn("FluidPipeCadence.scanOrder", be)
        self.assertNotIn("CableNetworkTraversal", be)
        self.assertNotIn("ModCapabilities.ENERGY", be)
        self.assertIn("fluidPipeCapacityMatchesGt6Gauge", tests)
        self.assertIn("fluidPipeTickCadenceMatchesGt6", tests)
        self.assertIn("fluidPipePlasmaMagicAcidGasFailures", tests)
        self.assertIn("fluidPipeChunkUnloadRetainsTank", tests)
        self.assertIn("fluidPipeSideContract", tests)
        self.assertNotIn("fluidPipeCapacityMatchesGt6Gauge", core)
        notes = census.load_json(WAVE / "runtime_notes.json")
        self.assertTrue(notes["pipe_to_pipe_every_server_tick"])
        self.assertEqual(5, notes["cover_phase_ticks"])
        self.assertFalse(notes["quadruple_nonuple_registered"])
        self.assertFalse(notes["energy_capability"])
        self.assertEqual("explicitly_blocked", notes["obtain"])
        self.assertEqual("runtime_ready", notes["close_target"])

    def test_execution_subset_folds_only_exact_live_hosts(self) -> None:
        subset = census.load_json(WAVE / "execution_subset.json")
        baseline = census.load_json(LEDGER)
        self.assertEqual(SLUG, subset["capability_slug"])
        self.assertEqual(125, int(subset["counts"]["fold_live_block"]))
        self.assertEqual(196, int(subset["counts"]["in_catalog_fluid"]))
        self.assertEqual(0, int(subset["counts"]["upgrade_live_item"]))
        by_meta = {int(row["meta"]): row for row in baseline["rows"]}
        folded = [
            row
            for row in subset["rows"]
            if row["disposition"] == "fold_live_block"
        ]
        self.assertEqual(125, len(folded))
        for row in folded:
            source = by_meta[int(row["meta"])]
            self.assertEqual("fold_live_block", source["disposition"])
            self.assertEqual("fluid", source["domain"])
            self.assertTrue(source["in_catalog_1817"])
            self.assertEqual(source["live_block"], row["live_block"])
            self.assertIn(source["cc_form"], runtime.FIVE_GAUGES)
        keep = [
            row
            for row in subset["rows"]
            if row["disposition"] == "keep_distinct"
        ]
        self.assertGreater(len(keep), 0)
        self.assertTrue(
            any("quadruple" in str(row.get("dummy_path") or "") for row in keep)
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
            / "cruciblecraft_wave_content_gt6_fluid_pipe_runtime"
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
