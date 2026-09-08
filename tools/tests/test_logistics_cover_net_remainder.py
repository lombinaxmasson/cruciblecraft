#!/usr/bin/env python3
"""Closed remainder card still answers dump_policy; Dump is not a Generic sidecar."""
from __future__ import annotations

import unittest

from tools import capability_ledger as ledger
from tools import io_common as io
from tools.wave_closeout import spec_for

ROOT = io.ROOT
R0 = io.TOOLS / "waves" / "portfolio" / "logistics-cover-net-r0"
COVER_DEFINITIONS = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "cover_definitions.json"
)
GENERIC_SIDECAR = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "generic_network_cover_definitions.json"
)
DUMP_QUESTION = (
    "How dump differs from transfer and storage, and what a dump cover "
    "flushes or discards on the network."
)


class LogisticsCoverNetRemainderFrozenTest(unittest.TestCase):
    def test_r0_wave_stays_unassigned_and_unimplemented(self) -> None:
        spec = spec_for("portfolio/logistics-cover-net-r0")
        self.assertIsNone(spec.unique_active_wave)
        self.assertTrue(spec.next_unassigned)
        contract = io.load_json(R0 / "network_contract.json")
        self.assertFalse(contract["implemented"])
        self.assertEqual(DUMP_QUESTION, contract["questions"]["dump_policy"])
        topology = io.load_json(R0 / "topology.json")
        self.assertIsNone(topology["unique_active_wave"])
        self.assertTrue(topology["next_unassigned"])

    def test_dump_is_not_a_generic_sidecar_row(self) -> None:
        base_ids = [row["id"] for row in io.load_json(COVER_DEFINITIONS)["definitions"]]
        self.assertEqual(9, len(base_ids))
        self.assertTrue(all("logistics_" not in row_id for row_id in base_ids))
        sidecar_ids = [row["id"] for row in io.load_json(GENERIC_SIDECAR)["definitions"]]
        self.assertEqual(
            [
                "cruciblecraft:logistics_generic_storage",
                "cruciblecraft:logistics_generic_import",
                "cruciblecraft:logistics_generic_export",
            ],
            sidecar_ids,
        )
        self.assertFalse(
            (io.TOOLS / "capabilities" / "logistics" / "generic-network" / "dump").exists()
        )
        self.assertTrue(
            (io.TOOLS / "capabilities" / "logistics" / "logistics-core").exists()
        )
        cover = next(
            row
            for row in (
                ledger.load_capability(path) for path in ledger.capability_files()
            )
            if row["slug"] == "logistics/cover-net-r0"
        )
        self.assertEqual("frozen", cover["maturity"])
        self.assertNotIn(
            "logistics/generic-network/dump",
            ledger.compile_ledger()["declared_player_complete"],
        )


if __name__ == "__main__":
    unittest.main()
