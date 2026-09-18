#!/usr/bin/env python3
"""GT6 storage kind-level art on live T44 and in-place BlockItems."""
from __future__ import annotations

import unittest

from tools import capability_ledger as ledger
from tools import census_common as census
from tools import gt6_storage_art as art

SLUG = "content/gt6-storage-art"
ROOT = census.ROOT
WAVE = ROOT / "tools" / "waves" / "content" / "gt6-storage-art"
CAPABILITY = (
    ROOT / "tools" / "capabilities" / "content" / "gt6-storage-art" / "capability.json"
)
STORAGE = (
    ROOT
    / "tools"
    / "capabilities"
    / "content"
    / "gt6-mte-furniture-storage-runtime"
    / "capability.json"
)
BARREL = (
    ROOT
    / "tools"
    / "capabilities"
    / "content"
    / "gt6-mte-furniture-barrel-runtime"
    / "capability.json"
)
PLAN_ACTIVE = ROOT / "docs" / "history" / "card-plans" / "active" / "GT6仓储美术详细计划.md"
PLAN_CLOSED = ROOT / "docs" / "history" / "card-plans" / "closed" / "GT6仓储美术详细计划.md"
FURNITURE_IDS = ["leadBookshelfIsLiveInventory"]
BARREL_IDS = ["skyrootBarrelIsLiveInventory"]


class Gt6StorageArtTest(unittest.TestCase):
    def test_capability_depends_on_closed_storage_runtimes(self) -> None:
        capability = census.load_json(CAPABILITY)
        storage = census.load_json(STORAGE)
        barrel = census.load_json(BARREL)
        self.assertEqual(SLUG, capability["slug"])
        self.assertEqual(
            [
                "content/gt6-mte-furniture-storage-runtime",
                "content/gt6-mte-furniture-barrel-runtime",
            ],
            capability["depends_on"],
        )
        self.assertEqual(["capability-runtime"], capability["profiles"])
        self.assertEqual("runtime_ready", capability["maturity"])
        self.assertNotEqual("player_complete", capability["maturity"])
        self.assertEqual("accepted", storage["workflow"])
        self.assertEqual("accepted", barrel["workflow"])
        self.assertEqual(FURNITURE_IDS, sorted(storage["required_test_ids"]))
        self.assertEqual(BARREL_IDS, sorted(barrel["required_test_ids"]))
        self.assertEqual(
            sorted(art.EXPECTED_TESTS),
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

    def test_kind_level_art_rejects_vanilla_placeholders(self) -> None:
        self.assertEqual([], art.check())
        notes = census.load_json(WAVE / "runtime_notes.json")
        self.assertTrue(notes["no_vanilla_oak_iron"])
        self.assertTrue(notes["no_per_material_png"])
        self.assertTrue(notes["kind_level_colored_overlay"])
        self.assertFalse(notes["dual_t44_and_inplace_ids_remain"])
        self.assertTrue(notes["face_item_count_books_bottles"])
        self.assertFalse(notes["digits_books_bottles_remain_ber"])
        self.assertFalse(notes["reopens_furniture_required_test_ids"])
        self.assertTrue(notes["metal_inplace_player_storage"])
        self.assertEqual("explicitly_blocked", notes["obtain"])
        barrel = (
            ROOT
            / "src"
            / "main"
            / "resources"
            / "assets"
            / "cruciblecraft"
            / "models"
            / "block"
            / "storage_mass_barrel.json"
        )
        text = barrel.read_text(encoding="utf-8")
        self.assertIn("gt6_import/storage/mass_storage_barrel", text)
        self.assertNotIn("minecraft:block/barrel", text)

    def test_unique_active_docs_and_ledger_hand_off(self) -> None:
        topology = census.load_json(WAVE / "topology.json")
        readiness = census.load_json(WAVE / "readiness.json")
        compiled = ledger.compile_ledger()
        capability = census.load_json(CAPABILITY)
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
