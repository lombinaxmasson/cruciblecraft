#!/usr/bin/env python3
"""Closed remainder card still answers dump_policy; Dump is not a Generic sidecar."""
from __future__ import annotations

import unittest

from tools import capability_ledger as ledger
from tools import io_common as io
from tools.wave_closeout import spec_for

ROOT = io.ROOT
ACTIVE = ROOT / "docs" / "history" / "card-plans" / "active"
CLOSED = ROOT / "docs" / "history" / "card-plans" / "closed"
PLAN = CLOSED / "物流封面网余量详细计划.md"
GAP = ROOT / "docs" / "current" / "unimplemented-gap.md"
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


class LogisticsCoverNetRemainderCardTest(unittest.TestCase):
    def test_unique_active_folder_is_batteries(self) -> None:
        names = sorted(path.name for path in ACTIVE.iterdir() if path.is_file())
        self.assertEqual(["电池详细计划.md"], names)
        self.assertTrue((CLOSED / "显示CPU详细计划.md").is_file())
        self.assertTrue((CLOSED / "能量系统余量详细计划.md").is_file())
        self.assertTrue((CLOSED / "能量转换机目录详细计划.md").is_file())

    def test_closed_plan_answers_dump_policy(self) -> None:
        text = PLAN.read_text(encoding="utf-8")
        self.assertIn("dump_policy", text)
        self.assertIn("CoverLogisticsGenericDump", text)
        self.assertIn("MultiTileEntityLogisticsCore", text)
        self.assertIn("tStackDumps", text)
        self.assertIn("usePriorities()", text)
        self.assertIn("不是虚空", text)
        self.assertIn("logistics/logistics-core", text)
        self.assertIn("不得复用", text)
        self.assertIn("portfolio/logistics-cover-net-r0", text)

    def test_human_ledger_keeps_cover_net_partial(self) -> None:
        text = GAP.read_text(encoding="utf-8")
        self.assertIn("Dump 是 Core 最后一档物品溢出", text)
        self.assertIn("不是 Generic 管网盖板", text)
        self.assertIn("不得把 Display CPU 算进这七 kind", text)


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
