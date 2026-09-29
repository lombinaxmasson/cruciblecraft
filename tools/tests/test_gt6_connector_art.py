#!/usr/bin/env python3
"""GT6 connector art child."""
from __future__ import annotations

import unittest

from tools import capability_ledger as ledger
from tools import census_common as census
from tools import gt6_connector_art as art

SLUG = "content/gt6-connector-art"
ROOT = census.ROOT
WAVE = ROOT / "tools" / "waves" / "content" / "gt6-connector-art"
CAPABILITY = (
    ROOT / "tools" / "capabilities" / "content" / "gt6-connector-art" / "capability.json"
)
PLAN_ACTIVE = (
    ROOT / "docs" / "history" / "card-plans" / "active" / "GT6连接件美术详细计划.md"
)
PLAN_CLOSED = (
    ROOT / "docs" / "history" / "card-plans" / "closed" / "GT6连接件美术详细计划.md"
)


class Gt6ConnectorArtTest(unittest.TestCase):
    def test_capability_depends_on_runtime_children(self) -> None:
        capability = census.load_json(CAPABILITY)
        self.assertEqual(SLUG, capability["slug"])
        self.assertEqual(
            [
                "content/gt6-fluid-pipe-runtime",
                "content/gt6-item-pipe-runtime",
                "content/gt6-eu-wire-cable-runtime",
            ],
            capability["depends_on"],
        )
        self.assertNotIn(
            "content/gt6-pipe-cable-baseline", capability["depends_on"]
        )
        self.assertEqual(["capability-runtime"], capability["profiles"])
        self.assertEqual("runtime_ready", capability["maturity"])
        self.assertNotEqual("player_complete", capability["maturity"])
        self.assertEqual(
            [
                "connectorArtManifestResolvesLocalGt6",
                "foldedRowsCopyZeroPng",
                "inPlaceRowsHaveNoIronIngotModel",
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

    def test_manifests_resolve_local_gt6_and_skip_redstone_recopy(self) -> None:
        self.assertEqual([], art.check())
        notes = census.load_json(WAVE / "runtime_notes.json")
        self.assertTrue(notes["no_invented_cable_png"])
        self.assertTrue(notes["redstone_wire_not_recopied"])
        self.assertEqual("runtime_ready", notes["close_target"])
        eu = census.load_json(art.MANIFESTS["eu"])
        destinations = [row["destination"] for row in eu["imports"]]
        self.assertIn(art.REDSTONE_WIRE, destinations)
        self.assertTrue(
            any(row["destination"] == art.REDSTONE_WIRE and not row["copied"]
                for row in eu["imports"])
        )
        self.assertFalse(any("cable.png" in row["gt6_source"] for row in eu["imports"]))
        self.assertEqual(0, art.extra_png_for_folds())
        tests = art.GAME_TESTS.read_text(encoding="utf-8")
        core = art.CORE_TESTS.read_text(encoding="utf-8")
        for name in art.EXPECTED_TESTS:
            self.assertIn(name, tests)
            self.assertNotIn(f"void {name}", core)

    def test_keep_distinct_models_drop_iron_ingot(self) -> None:
        upgrades = art._subset_rows(art.EU_SUBSET, "upgrade_live_item")
        leftover = next(
            row for row in upgrades
            if str(row.get("dummy_path") or "").endswith("2x_blue_alloy_wire")
        )
        model = art.ITEM_MODELS / f"{leftover['dummy_path']}.json"
        self.assertFalse(model.is_file())
        folded = art._subset_rows(art.EU_SUBSET, "fold_live_block")[0]
        leftover = art.ITEM_MODELS / f"{folded['dummy_path']}.json"
        self.assertFalse(leftover.is_file())

    def test_unique_active_docs_and_ledger_hand_off(self) -> None:
        topology = census.load_json(WAVE / "topology.json")
        readiness = census.load_json(WAVE / "readiness.json")
        compiled = ledger.compile_ledger()
        capability = census.load_json(CAPABILITY)
        ns = (
            ROOT
            / "src"
            / "main"
            / "resources"
            / "data"
            / "cruciblecraft_wave_content_gt6_connector_art"
        )
        self.assertTrue((ns / "structure" / "empty.nbt").is_file())
        self.assertTrue((ns / "gametest" / "structure" / "empty.nbt").is_file())
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
