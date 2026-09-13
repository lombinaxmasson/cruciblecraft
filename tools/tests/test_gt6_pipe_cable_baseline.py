#!/usr/bin/env python3
"""GT6 pipe/cable/redstone baseline audit artifacts."""
from __future__ import annotations

import unittest
from pathlib import Path

from tools import census_common as census
from tools import gt6_pipe_cable_baseline as baseline

WAVE = census.TOOLS / "waves" / "content" / "gt6-pipe-cable-baseline"


class Gt6PipeCableBaselineTest(unittest.TestCase):
    def test_artifacts_are_current(self) -> None:
        self.assertEqual([], baseline.check_artifacts())

    def test_no_capability_or_runtime_claim(self) -> None:
        self.assertFalse(baseline.CAPABILITY_JSON.is_file())
        readiness = census.load_json(WAVE / "readiness.json")
        self.assertEqual("PIPE_CABLE_BASELINE_PREP_READY", readiness["status"])
        self.assertIsNone(readiness["unique_active_wave"])
        self.assertIsNone(readiness["production_lock"])
        self.assertFalse(readiness["player_complete"])
        self.assertFalse(readiness["runtime_ready"])
        self.assertFalse(readiness["capability_created"])
        self.assertFalse(readiness["src_main_modified"])

    def test_identity_ledger_covers_catalog_and_redstone_fold(self) -> None:
        ledger = census.load_json(WAVE / "identity_resolution_ledger.json")
        self.assertEqual(666, int(ledger["catalog_rows"]))
        by_meta = {int(row["meta"]): row for row in ledger["rows"]}
        self.assertEqual("fold_live_block", by_meta[25002]["disposition"])
        self.assertEqual("already_shared", by_meta[26140]["disposition"])
        self.assertEqual("keep_distinct", by_meta[26145]["disposition"])
        self.assertEqual("already_shared", by_meta[27000]["disposition"])
        self.assertEqual(
            "cruciblecraft:red_alloy/wire", by_meta[27000]["live_block"]
        )
        self.assertEqual("already_shared", by_meta[28101]["disposition"])
        self.assertTrue(by_meta[28100]["recipe_mapped"])
        self.assertEqual("fold_live_block", by_meta[28050]["disposition"])
        self.assertEqual(
            "content/mte-redstone-wire", by_meta[27000]["identity_owner"]
        )
        self.assertEqual(
            "content/gt6-redstone-wire-correction", by_meta[27000]["child_owner"]
        )
        self.assertEqual(
            "content/gt6-eu-wire-cable-runtime", by_meta[28050]["child_owner"]
        )
        eu_redstone = [
            row
            for row in ledger["rows"]
            if row["domain"] == "eu"
            and row.get("material") in {"red_alloy", "signalum", "lumium"}
        ]
        self.assertEqual([], eu_redstone)
        insulated = {
            int(row["meta"])
            for row in ledger["rows"]
            if row["domain"] == "redstone_insulated"
        }
        self.assertEqual({27006, 27056, 27506}, insulated)
        self.assertFalse(by_meta[27006]["in_catalog_1817"])

    def test_behavior_and_isolation_gates(self) -> None:
        gaps = census.load_json(WAVE / "behavior_gap_matrix.json")
        fluid = {row["id"]: row["status"] for row in gaps["domains"]["fluid"]}
        item = {row["id"]: row["status"] for row in gaps["domains"]["item"]}
        redstone = {row["id"]: row["status"] for row in gaps["domains"]["redstone"]}
        self.assertEqual("gap", fluid["cadence"])
        self.assertEqual("gap", fluid["plasma_magic_flammable_contact"])
        self.assertEqual("gap", item["inventory"])
        self.assertEqual("gap", redstone["sender_loss"])
        isolation = census.load_json(WAVE / "network_isolation.json")
        self.assertTrue(isolation["has_energy_capability"]["cable"])
        self.assertFalse(isolation["has_energy_capability"]["redstone"])
        self.assertFalse(isolation["uses_cable_network_traversal"]["redstone"])
        self.assertFalse(isolation["uses_pipe_topology"]["cable"])
        self.assertTrue(isolation["uses_pipe_topology"]["pipe"])

    def test_art_selector_uses_gt6_filenames(self) -> None:
        art = census.load_json(WAVE / "art_selector_contract.json")
        self.assertTrue(art["present"]["pipetiny.png"])
        self.assertTrue(art["present"]["wire.png"])
        self.assertTrue(art["present"]["iconsets/pipe_restrictor.png"])
        self.assertTrue(art["no_dedicated_cable_png"])
        self.assertFalse(art["this_card_copies_png"])
        authority = census.load_json(WAVE / "source_authority.json")
        self.assertEqual(baseline.GT6_REVISION, authority["java_tick"]["revision"])
        self.assertEqual(
            baseline.GT6_TICK_FILE_HASHES[
                "src/main/java/gregapi/tileentity/connectors/"
                "MultiTileEntityPipeFluid.java"
            ],
            authority["java_tick"]["files"][
                "src/main/java/gregapi/tileentity/connectors/"
                "MultiTileEntityPipeFluid.java"
            ]["sha256"],
        )

    def test_children_are_split_and_blocked(self) -> None:
        envelopes = census.load_json(WAVE / "child_envelopes.json")
        slugs = [row["slug"] for row in envelopes["children"]]
        self.assertEqual(6, len(slugs))
        self.assertEqual(len(slugs), len(set(slugs)))
        for row in envelopes["children"]:
            self.assertEqual(
                baseline.child_issued(row["slug"]),
                row["issued"],
                row["slug"],
            )
            self.assertTrue(row["no_stand_in"])
            self.assertTrue(row["blocked"])

    def test_plan_gates_are_checked(self) -> None:
        plan = baseline.plan_path()
        self.assertEqual(baseline.PLAN_CLOSED, plan)
        self.assertTrue(plan.is_file(), plan)
        self.assertFalse(baseline.PLAN_PREP.is_file())
        gate = plan.read_text(encoding="utf-8").split("## 5. Prep 验收门")[1]
        gate = gate.split("## 6.")[0]
        self.assertNotIn("- [ ]", gate)


if __name__ == "__main__":
    unittest.main()
