#!/usr/bin/env python3
"""Closeout integrity: WAVE_READY cannot ignore pending load measurements."""
from __future__ import annotations

import unittest

from tools.recipe_bulk import ordinary_wave as wave


class OrdinaryWaveReadyTest(unittest.TestCase):
    def test_pending_load_is_not_wave_ready(self) -> None:
        load = {
            "status": "LOAD_PENDING_MEASUREMENT",
            "decision": {
                "eliminate_reasons": [
                    "measurement_unavailable:client_reload_ms",
                    "measurement_unavailable:retained_memory_bytes",
                ],
                "axes": {
                    "client_reload_ms": {
                        "actual": None,
                        "eliminates": True,
                        "status": "measurement_unavailable",
                    }
                },
            },
            "client_reload_ms": None,
            "retained_memory_bytes": None,
        }
        blockers = wave.load_measurement_blockers(load)
        self.assertTrue(any("client_reload_ms" in row for row in blockers))
        verdict = wave.evaluate_wave_ready(
            "smelter/ordinary-closure",
            source={"status": "SOURCE_READY"},
            lock={"production": {"family_count": 338}},
            equivalence={"status": "EQUIVALENCE_READY"},
            player_path={"status": "PLAYER_PATH_READY"},
            receipt={"status": "PASS"},
            load=load,
            census={
                "complete_family_count": 338,
                "partial_family_count": 0,
                "load": load,
            },
            unique_active=None,
        )
        self.assertFalse(verdict["ready"])
        self.assertNotEqual("WAVE_READY", verdict["status"])
        topology = wave.build_topology(
            "smelter/ordinary-closure",
            {"remaining_recipe_gap": 997},
            {"production": {"family_count": 338}},
            None,
            True,
            verdict=verdict,
        )
        self.assertNotEqual("WAVE_READY", topology["status"])
        self.assertFalse(topology["next_unassigned"])

    def test_zero_filled_client_reload_is_not_wave_ready(self) -> None:
        load = {
            "status": "LOAD_READY",
            "decision": {"eliminate_reasons": [], "axes": {}},
            "client_reload_ms": 0,
            "client_index_ms": 10,
            "reload_transient_allocation_bytes": 1,
            "lookup_allocation_bytes_per_operation": 0,
            "retained_memory_bytes": 1,
            "server_reload_ms": 1,
            "server_index_ms": 1,
            "sync_bytes": 1,
            "lookup_p95_ns": 1,
            "lookup_candidate_count": 1,
        }
        blockers = wave.load_measurement_blockers(load)
        self.assertIn("zero-filled:client_reload_ms", blockers)

    def test_complete_measurements_are_wave_ready(self) -> None:
        load = {
            "status": "LOAD_READY",
            "decision": {"eliminate_reasons": [], "axes": {}},
            "client_reload_ms": 400,
            "client_index_ms": 20,
            "reload_transient_allocation_bytes": 1000,
            "lookup_allocation_bytes_per_operation": 0,
            "retained_memory_bytes": 5000,
            "server_reload_ms": 4000,
            "server_index_ms": 80,
            "sync_bytes": 7000000,
            "lookup_p95_ns": 300000,
            "lookup_candidate_count": 8,
        }
        verdict = wave.evaluate_wave_ready(
            "mixer/ordinary-closure",
            source={"status": "SOURCE_READY"},
            lock={"production": {"family_count": 648}},
            equivalence={"status": "EQUIVALENCE_READY"},
            player_path={"status": "PLAYER_PATH_READY"},
            receipt={"status": "PASS"},
            load=load,
            census={
                "complete_family_count": 648,
                "partial_family_count": 0,
                "load": load,
            },
            unique_active=None,
        )
        self.assertEqual([], verdict["blockers"])
        self.assertTrue(verdict["ready"])
        self.assertEqual("WAVE_READY", verdict["status"])

    def test_seal_write_is_write_once_after_repair(self) -> None:
        from tools import closeout_seal

        with self.assertRaises(ValueError) as raised:
            closeout_seal.write_wave_seal("smelter/ordinary-closure")
        self.assertIn("write-once", str(raised.exception))

    def test_live_on_disk_status_matches_derivation(self) -> None:
        from tools import census_common as census

        for slug in ("smelter/ordinary-closure", "mixer/ordinary-closure"):
            verdict = wave.evaluate_live_wave_ready(slug)
            document = census.load_json(wave.wave_dir(slug) / "readiness.json")
            self.assertEqual(verdict["status"], document.get("status"))
            self.assertTrue(verdict["ready"])
            self.assertEqual("WAVE_READY", document.get("status"))
        mixer = census.load_json(wave.wave_dir("mixer/ordinary-closure") / "census_delta.json")
        self.assertEqual(334, int(mixer["remaining_recipe_gap"]))


if __name__ == "__main__":
    unittest.main()
