#!/usr/bin/env python3
"""Logistics Core unique-active card: local gt6_code, dump sidecar, catalog 19."""
from __future__ import annotations

import hashlib
import re
import unittest

from tools import capability_ledger as ledger
from tools import io_common as io
from tools.recipe_bulk.slugs import KNOWN_SEMANTIC_SLUGS
from tools.wave_closeout import spec_for

SLUG = "runtime/logistics-core"
CAPABILITY = "logistics/logistics-core"
ROOT = io.ROOT
CLOSED = ROOT / "docs" / "history" / "card-plans" / "closed"
PLAN = CLOSED / "物流核心详细计划.md"
WAVE = io.TOOLS / "waves" / "runtime" / "logistics-core"
T19 = (
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
DUMP_SIDECAR = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "logistics_dump_cover_definitions.json"
)
REGISTRY = (
    ROOT
    / "src"
    / "main"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "logistics"
    / "pipe"
    / "cover"
    / "CoverBehaviorRegistry.java"
)
BUILTIN_RE = re.compile(r'registerBuiltin\("([a-z0-9_]+)"')
PINNED_T19_HASH = "207b4d03a66bf088574c1e6fef01971de307255eedf68c8a661741d839e857ec"


class LogisticsCoreCardTest(unittest.TestCase):
    def test_slug_is_zero_family_runtime_card(self) -> None:
        self.assertIn(SLUG, KNOWN_SEMANTIC_SLUGS)
        spec = spec_for(SLUG)
        self.assertIsNone(spec.unique_active_wave)
        self.assertTrue(spec.next_unassigned)
        self.assertEqual(0, spec.owns_families)
        self.assertIsNone(spec.production_lock)
        self.assertEqual(WAVE / "census_delta.json", spec.census)
        self.assertEqual(WAVE / "gametest_receipt.json", spec.receipt)

    def test_closed_plan_stays_archived(self) -> None:
        self.assertTrue(PLAN.is_file())
        active = ROOT / "docs" / "history" / "card-plans" / "active"
        names = sorted(path.name for path in active.iterdir() if path.is_file())
        self.assertNotIn("物流核心详细计划.md", names)

    def test_plan_uses_local_gt6_code_not_github_fetch(self) -> None:
        text = PLAN.read_text(encoding="utf-8")
        self.assertIn("gt6_code/gregtech6", text)
        self.assertIn("3703e40308c8c030763fd6297dea8b210d2a77b1", text)
        self.assertIn("MultiTileEntityLogisticsCore", text)
        self.assertIn("CoverLogisticsGenericDump", text)
        self.assertIn("logistics/logistics-core", text)
        self.assertNotIn("raw.githubusercontent.com", text)
        self.assertNotIn("github.com/GregTech6", text)
        self.assertIn(
            "`tools/capabilities/logistics/generic-network/dump/`",
            text,
        )
        self.assertIn("gregtech6_w", text)
        self.assertNotIn("贴图暂借记", text)

    def test_art_manifest_is_local_gt6_w(self) -> None:
        manifest = io.load_json(
            ROOT
            / "src"
            / "main"
            / "resources"
            / "assets"
            / "cruciblecraft"
            / "logistics_core_art_manifest.json"
        )
        self.assertEqual("gt6_referencable_port_code/gregtech6_w", manifest["source"])
        self.assertEqual(55, len(manifest["imports"]))
        dump = ROOT / (
            "src/main/resources/assets/cruciblecraft/textures/item/"
            "gt6_import/logistics_generic_dump_cover.png"
        )
        self.assertTrue(dump.is_file())


class LogisticsCoreCatalogLockTest(unittest.TestCase):
    def test_base_cover_file_stays_nine_rows_without_logistics_ids(self) -> None:
        catalog = io.load_json(T19)
        rows = catalog["definitions"]
        self.assertEqual(9, len(rows))
        for row in rows:
            self.assertNotIn("logistics_", row["id"])
        self.assertEqual(
            PINNED_T19_HASH,
            hashlib.sha256(T19.read_bytes()).hexdigest(),
        )

    def test_register_builtin_stays_eight(self) -> None:
        text = REGISTRY.read_text(encoding="utf-8")
        builtins = BUILTIN_RE.findall(text)
        self.assertEqual(8, len(builtins))
        self.assertNotIn("logistics_generic_dump", builtins)

    def test_dump_sidecar_is_owned_by_core_not_generic(self) -> None:
        generic = [row["id"] for row in io.load_json(GENERIC_SIDECAR)["definitions"]]
        self.assertEqual(
            [
                "cruciblecraft:logistics_generic_storage",
                "cruciblecraft:logistics_generic_import",
                "cruciblecraft:logistics_generic_export",
            ],
            generic,
        )
        dump = io.load_json(DUMP_SIDECAR)["definitions"]
        self.assertEqual(1, len(dump))
        self.assertEqual("cruciblecraft:logistics_generic_dump", dump[0]["id"])
        self.assertEqual("item", dump[0]["medium"])
        self.assertEqual(["network_id"], dump[0]["configurable"])
        self.assertFalse(
            (io.TOOLS / "capabilities" / "logistics" / "generic-network" / "dump").exists()
        )


class LogisticsCoreCapabilityTest(unittest.TestCase):
    def test_core_capability_is_player_complete(self) -> None:
        documents = [
            ledger.load_capability(path) for path in ledger.capability_files()
        ]
        core = next(row for row in documents if row["slug"] == CAPABILITY)
        self.assertEqual("player_complete", core["maturity"])
        self.assertEqual("accepted", core["workflow"])
        self.assertEqual(
            "tools/capabilities/logistics/logistics-core/player_signoff.json",
            core.get("player_signoff"),
        )
        self.assertEqual(
            [
                "dumpCoverIsSurvivalCraftable",
                "formedCoreDumpsLeftoverItems",
                "missingCpuTypeDoesNotForm",
                "missingDumpChestDoesNotVoid",
                "playerSurfaceIsRegistered",
            ],
            core["required_test_ids"],
        )
        compiled = ledger.compile_ledger()
        self.assertIn(CAPABILITY, compiled["declared_player_complete"])


if __name__ == "__main__":
    unittest.main()
