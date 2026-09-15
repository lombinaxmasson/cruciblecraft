#!/usr/bin/env python3
"""Fluid network basic-transfer slug, sidecar catalog, and frozen generic dump."""
from __future__ import annotations

import hashlib
import re
import unittest

from tools import io_common as io
from tools.recipe_bulk.slugs import KNOWN_SEMANTIC_SLUGS
from tools.wave_closeout import spec_for

SLUG = "runtime/fluid-network-basic-transfer"
ROOT = io.ROOT
WAVE = io.TOOLS / "waves" / "runtime" / "fluid-network-basic-transfer"
T19 = ROOT / "src" / "main" / "resources" / "data" / "cruciblecraft" / "cover_definitions.json"
SIDECAR = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "fluid_network_cover_definitions.json"
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


class FluidNetworkCoreRegistrationTest(unittest.TestCase):
    def test_slug_is_zero_family_runtime_card(self) -> None:
        self.assertIn(SLUG, KNOWN_SEMANTIC_SLUGS)
        spec = spec_for(SLUG)
        self.assertIsNone(spec.unique_active_wave)
        self.assertTrue(spec.next_unassigned)
        self.assertEqual(0, spec.owns_families)
        self.assertIsNone(spec.production_lock)
        self.assertEqual(WAVE / "census_delta.json", spec.census)
        self.assertEqual(WAVE / "gametest_receipt.json", spec.receipt)


class FluidNetworkCoreCatalogLockTest(unittest.TestCase):
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
        self.assertNotIn("logistics_fluid_storage", builtins)
        self.assertNotIn("logistics_fluid_transfer", builtins)
        covers = (
            ROOT
            / "src"
            / "main"
            / "java"
            / "com"
            / "masson"
            / "cruciblecraft"
            / "logistics"
            / "fluidnet"
            / "FluidNetworkCovers.java"
        )
        body = covers.read_text(encoding="utf-8")
        self.assertIn("CoverBehaviorRegistry.register(", body)
        self.assertNotIn("registerBuiltin(", body)

    def test_sidecar_has_three_fluid_network_definitions(self) -> None:
        sidecar = io.load_json(SIDECAR)
        rows = sidecar["definitions"]
        self.assertEqual(3, len(rows))
        ids = [row["id"] for row in rows]
        self.assertEqual(
            [
                "cruciblecraft:logistics_fluid_storage",
                "cruciblecraft:logistics_fluid_import",
                "cruciblecraft:logistics_fluid_export",
            ],
            ids,
        )
        behaviors = {row["id"]: row["behavior"] for row in rows}
        self.assertEqual(
            "cruciblecraft:logistics_fluid_storage",
            behaviors["cruciblecraft:logistics_fluid_storage"],
        )
        self.assertEqual(
            "cruciblecraft:logistics_fluid_transfer",
            behaviors["cruciblecraft:logistics_fluid_import"],
        )
        self.assertEqual(
            "cruciblecraft:logistics_fluid_transfer",
            behaviors["cruciblecraft:logistics_fluid_export"],
        )


class FluidNetworkCoreArtifactsTest(unittest.TestCase):
    def test_ready_artifacts(self) -> None:
        readiness = io.load_json(WAVE / "readiness.json")
        self.assertEqual("runtime_ready", readiness["evidence"]["item_kinds_status"])
        self.assertEqual(
            "runtime_ready", readiness["evidence"]["fluid_basic_transfer_status"]
        )
        self.assertEqual("frozen", readiness["evidence"]["fluid_generic_dump_status"])
        receipt = io.load_json(WAVE / "gametest_receipt.json")
        self.assertEqual("PASS", receipt["status"])
        self.assertEqual(0, int(receipt["failed"]))
        self.assertGreaterEqual(int(receipt["passed"]), int(receipt["required_tests"]))
        self.assertIn(
            "-PwaveRecipes=runtime/fluid-network-basic-transfer", receipt["command"]
        )
        self.assertTrue((WAVE / "gametest.log").is_file())
        wave = io.load_json(WAVE / "wave.json")
        self.assertEqual(SLUG, wave["program"])
        network = (
            ROOT
            / "src"
            / "main"
            / "java"
            / "com"
            / "masson"
            / "cruciblecraft"
            / "logistics"
            / "fluidnet"
            / "FluidLogisticsNetwork.java"
        ).read_text(encoding="utf-8")
        self.assertNotIn("ConcurrentHashMap", network)
        self.assertIn("copyWithAmount", network)


if __name__ == "__main__":
    unittest.main()
