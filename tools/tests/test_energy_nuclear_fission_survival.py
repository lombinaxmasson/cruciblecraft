#!/usr/bin/env python3
"""Fission survival closed card: 46 rods / 8 kinds / 2 cores / 48 relations."""
from __future__ import annotations

import json
import unittest

from tools import capability_ledger as ledger
from tools import io_common as io
from tools.recipe_bulk.slugs import KNOWN_SEMANTIC_SLUGS
from tools.wave_closeout import spec_for

SLUG = "runtime/fission-survival"
CAPABILITY_SLUG = "energy/nuclear-fission-survival"
ROOT = io.ROOT
WAVE = io.TOOLS / "waves" / "runtime" / "fission-survival"
MATRIX = WAVE / "d0_nuclear_source_matrix.json"
CAPABILITY = (
    ROOT
    / "tools"
    / "capabilities"
    / "energy"
    / "nuclear-fission-survival"
    / "capability.json"
)
MACHINE_TIERS = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "machine_tiers.json"
)
PHASE5 = ROOT / "tools" / "phase5_portfolio_contract.json"
GROWTH = (
    ROOT
    / "tools"
    / "waves"
    / "portfolio"
    / "source-capability-growth-order"
    / "growth_order.json"
)


class FissionSurvivalCardTest(unittest.TestCase):
    def test_slug_is_zero_family_runtime_card(self) -> None:
        self.assertIn(SLUG, KNOWN_SEMANTIC_SLUGS)
        spec = spec_for(SLUG)
        self.assertIsNone(spec.unique_active_wave)
        self.assertTrue(spec.next_unassigned)
        self.assertEqual(0, spec.owns_families)
        self.assertIsNone(spec.production_lock)
        self.assertEqual(WAVE / "census_delta.json", spec.census)
        topology = io.load_json(WAVE / "topology.json")
        self.assertIsNone(topology["unique_active_wave"])
        self.assertTrue(topology["next_unassigned"])
        readiness = io.load_json(WAVE / "readiness.json")
        self.assertIsNone(readiness["unique_active_wave"])
        self.assertTrue(readiness["next_unassigned"])
        self.assertTrue(readiness["nuclear_started"])
        self.assertEqual(46, readiness["evidence"]["rod_identity_count"])
        self.assertEqual(8, readiness["evidence"]["rod_kind_count"])
        self.assertEqual(2, readiness["evidence"]["core_identity_count"])
        self.assertEqual(48, readiness["evidence"]["nuclear_source_relations"])
        self.assertEqual(1, readiness["evidence"]["canner_machine_tiers"])
        self.assertEqual(
            "player_complete",
            readiness["evidence"]["fission_survival_status"],
        )

    def test_source_matrix_is_exact_forty_eight(self) -> None:
        matrix = io.load_json(MATRIX)
        self.assertEqual(48, matrix["nuclear_source_relations"])
        self.assertEqual(0, matrix["missing"])
        self.assertEqual(0, matrix["extra"])
        self.assertEqual(0, matrix["duplicate"])
        relations = matrix["relations"]
        self.assertEqual(48, len(relations))
        self.assertEqual(48, len({row["recipe_id"] for row in relations}))
        self.assertTrue(all(row["status"] == "ready" for row in relations))

    def test_canner_lv_host_remains(self) -> None:
        tiers = io.load_json(MACHINE_TIERS)
        canners = [
            row
            for row in tiers["variants"]
            if row["kind"] == "cruciblecraft:canner"
        ]
        by_id = {row["id"]: row for row in canners}
        self.assertEqual(10, len(canners))
        lv = by_id["cruciblecraft:canner"]
        self.assertEqual(20161, lv["sourceId"])
        self.assertEqual("ELECTRIC", lv["energy"])
        self.assertEqual(
            "Loader_MultiTileEntities.java:1379",
            tiers["source"]["variant_rows"]["cruciblecraft:canner"],
        )
        self.assertEqual(20162, by_id["cruciblecraft:aluminium_canner"]["sourceId"])
        self.assertEqual(20165, by_id["cruciblecraft:titanium_canner"]["sourceId"])
        self.assertEqual(
            81014, by_id["cruciblecraft:neutronium_canner_omega"]["sourceId"]
        )

    def test_track_c_started_and_sealed_growth_order_stays_false(self) -> None:
        contract = json.loads(PHASE5.read_text(encoding="utf-8"))
        self.assertTrue(contract["tracks"]["C"]["started"])
        growth = io.load_json(GROWTH)
        self.assertFalse(growth["nuclear_started"])

    def test_capability_is_player_complete(self) -> None:
        capability = ledger.load_capability(CAPABILITY)
        self.assertEqual("player_complete", capability["maturity"])
        self.assertEqual("accepted", capability["workflow"])
        self.assertEqual(CAPABILITY_SLUG, capability["slug"])
        self.assertEqual(SLUG, capability["wave_slug"])
        self.assertEqual(
            "tools/capabilities/energy/nuclear-fission-survival/player_signoff.json",
            capability["player_signoff"],
        )
        self.assertEqual(["energy/transformers"], capability["depends_on"])
        keys = {row["semantic_key"] for row in capability["identity_disposition"]}
        self.assertIn("reactor:core", keys)
        self.assertIn("reactor:rods", keys)
        self.assertIn("machine:canner_lv", keys)
        blocked = {
            row["semantic_key"]
            for row in capability["identity_disposition"]
            if row["disposition"] == "blocked"
        }
        self.assertEqual(
            {
                "reactor:hot_fluids",
                "reactor:observation_safety",
                "reactor:fusion",
            },
            blocked,
        )
        ns = (
            ROOT
            / "src"
            / "main"
            / "resources"
            / "data"
            / "cruciblecraft_wave_runtime_fission_survival"
        )
        self.assertTrue((ns / "structure" / "empty.nbt").is_file())
        self.assertTrue((ns / "gametest" / "structure" / "empty.nbt").is_file())


if __name__ == "__main__":
    unittest.main()
