#!/usr/bin/env python3
"""Item network core slug, sidecar catalog, and unchanged T19 / R0 fingerprints."""
from __future__ import annotations

import hashlib
import re
import unittest

from tools import io_common as io
from tools.recipe_bulk.slugs import KNOWN_SEMANTIC_SLUGS
from tools.wave_closeout import spec_for

SLUG = "runtime/item-network-core"
ROOT = io.ROOT
WAVE = io.TOOLS / "waves" / "runtime" / "item-network-core"
T19 = ROOT / "src" / "main" / "resources" / "data" / "cruciblecraft" / "cover_definitions.json"
SIDECAR = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "item_network_cover_definitions.json"
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
R0 = io.TOOLS / "waves" / "portfolio" / "logistics-cover-net-r0"
BUILTIN_RE = re.compile(r'registerBuiltin\("([a-z0-9_]+)"')
PINNED_T19_HASH = "207b4d03a66bf088574c1e6fef01971de307255eedf68c8a661741d839e857ec"


class ItemNetworkCoreRegistrationTest(unittest.TestCase):
    def test_slug_is_zero_family_runtime_card(self) -> None:
        self.assertIn(SLUG, KNOWN_SEMANTIC_SLUGS)
        spec = spec_for(SLUG)
        self.assertIsNone(spec.unique_active_wave)
        self.assertTrue(spec.next_unassigned)
        self.assertEqual(0, spec.owns_families)
        self.assertIsNone(spec.production_lock)
        self.assertEqual(WAVE / "census_delta.json", spec.census)
        self.assertEqual(WAVE / "gametest_receipt.json", spec.receipt)


class ItemNetworkCoreCatalogLockTest(unittest.TestCase):
    def test_t19_file_stays_nine_rows_without_logistics_ids(self) -> None:
        catalog = io.load_json(T19)
        rows = catalog["definitions"]
        self.assertEqual(9, len(rows))
        for row in rows:
            self.assertNotIn("logistics_", row["id"])
            self.assertNotIn("network_id", row.get("configurable", []))
        self.assertEqual(
            PINNED_T19_HASH,
            hashlib.sha256(T19.read_bytes()).hexdigest(),
        )

    def test_register_builtin_stays_eight(self) -> None:
        text = REGISTRY.read_text(encoding="utf-8")
        builtins = BUILTIN_RE.findall(text)
        self.assertEqual(8, len(builtins))
        self.assertNotIn("logistics_item_storage", builtins)
        self.assertNotIn("logistics_item_transfer", builtins)
        covers = (
            ROOT
            / "src"
            / "main"
            / "java"
            / "com"
            / "masson"
            / "cruciblecraft"
            / "logistics"
            / "itemnet"
            / "ItemNetworkCovers.java"
        )
        self.assertIn("CoverBehaviorRegistry.register(", covers.read_text(encoding="utf-8"))
        self.assertNotIn("registerBuiltin(", covers.read_text(encoding="utf-8"))

    def test_sidecar_has_three_item_network_definitions(self) -> None:
        sidecar = io.load_json(SIDECAR)
        rows = sidecar["definitions"]
        self.assertEqual(3, len(rows))
        ids = [row["id"] for row in rows]
        self.assertEqual(
            [
                "cruciblecraft:logistics_item_storage",
                "cruciblecraft:logistics_item_import",
                "cruciblecraft:logistics_item_export",
            ],
            ids,
        )
        behaviors = {row["id"]: row["behavior"] for row in rows}
        self.assertEqual(
            "cruciblecraft:logistics_item_storage",
            behaviors["cruciblecraft:logistics_item_storage"],
        )
        self.assertEqual(
            "cruciblecraft:logistics_item_transfer",
            behaviors["cruciblecraft:logistics_item_import"],
        )
        self.assertEqual(
            "cruciblecraft:logistics_item_transfer",
            behaviors["cruciblecraft:logistics_item_export"],
        )
        for row in rows:
            self.assertIn("network_id", row["configurable"])


class ItemNetworkCoreArtifactsTest(unittest.TestCase):
    def test_ready_artifacts_and_unchanged_r0(self) -> None:
        readiness = io.load_json(WAVE / "readiness.json")
        self.assertEqual("ITEM_NETWORK_CORE_READY", readiness["status"])
        self.assertIsNone(readiness["unique_active_wave"])
        self.assertTrue(readiness["next_unassigned"])
        self.assertEqual(0, int(readiness["owns_families"]))
        self.assertEqual(0, int(readiness["generated_recipe_count"]))
        self.assertIsNone(readiness["production_lock"])
        self.assertFalse(readiness["nuclear_started"])
        evidence = readiness["evidence"]
        self.assertEqual(0, int(evidence["completion_delta"]))
        self.assertEqual(39, int(evidence["leftover_later_count"]))
        self.assertEqual("runtime_ready", evidence["item_kinds_status"])
        self.assertEqual("frozen", evidence["fluid_generic_dump_status"])
        topology = io.load_json(WAVE / "topology.json")
        self.assertIsNone(topology["unique_active_wave"])
        self.assertTrue(topology["next_unassigned"])
        census = io.load_json(WAVE / "census_delta.json")
        self.assertEqual(0, int(census["completion_delta"]))
        self.assertEqual(39, int(census["leftover_later_count"]))
        load_axis = io.load_json(WAVE / "load_axis.json")
        self.assertEqual(4096, int(load_axis["max_visited_pipes"]))
        self.assertEqual(256, int(load_axis["max_endpoints_per_component"]))
        self.assertEqual(8, int(load_axis["per_cover_tick_rate"]))
        receipt = io.load_json(WAVE / "gametest_receipt.json")
        self.assertEqual("PASS", receipt["status"])
        self.assertEqual(0, int(receipt["failed"]))
        self.assertGreaterEqual(int(receipt["passed"]), int(receipt["required_tests"]))
        self.assertIn("-PwaveRecipes=runtime/item-network-core", receipt["command"])
        self.assertTrue((WAVE / "gametest.log").is_file())
        r0_mechanism = io.load_json(R0 / "existing_mechanism.json")
        self.assertEqual(PINNED_T19_HASH, r0_mechanism["cover_definitions_sha256"])
        self.assertEqual(9, int(r0_mechanism["definition_count"]))
        self.assertEqual(8, int(r0_mechanism["behavior_count"]))
        feasibility = io.load_json(R0 / "feasibility.json")
        self.assertEqual("requires_new_runtime", feasibility["verdict"])
        wave = io.load_json(WAVE / "wave.json")
        self.assertEqual(SLUG, wave["program"])
        self.assertEqual(["portfolio/logistics-cover-net-r0"], wave["depends_on"])
        network = (
            ROOT
            / "src"
            / "main"
            / "java"
            / "com"
            / "masson"
            / "cruciblecraft"
            / "logistics"
            / "itemnet"
            / "ItemLogisticsNetwork.java"
        ).read_text(encoding="utf-8")
        self.assertNotIn("ConcurrentHashMap", network)
        self.assertNotIn("probeAt", network)
        self.assertIn("record Discovery", network)


if __name__ == "__main__":
    unittest.main()
