#!/usr/bin/env python3
"""GT6 foundry voxel art on the live 85 BlockItems."""
from __future__ import annotations

import unittest

from tools import capability_ledger as ledger
from tools import census_common as census
from tools import gt6_foundry_art as art

SLUG = "content/gt6-foundry-art"
ROOT = census.ROOT
WAVE = ROOT / "tools" / "waves" / "content" / "gt6-foundry-art"
CAPABILITY = (
    ROOT / "tools" / "capabilities" / "content" / "gt6-foundry-art" / "capability.json"
)
FOUNDRY = (
    ROOT
    / "tools"
    / "capabilities"
    / "content"
    / "gt6-mte-crucible-foundry-runtime"
    / "capability.json"
)
PLAN_ACTIVE = ROOT / "docs" / "history" / "card-plans" / "active" / "GT6铸造美术详细计划.md"
PLAN_CLOSED = ROOT / "docs" / "history" / "card-plans" / "closed" / "GT6铸造美术详细计划.md"
FOUNDRY_IDS = ["foundryCrucibleIsNotCeramicCrucible"]


class Gt6FoundryArtTest(unittest.TestCase):
    def test_capability_depends_on_closed_foundry_and_interaction(self) -> None:
        capability = census.load_json(CAPABILITY)
        foundry = census.load_json(FOUNDRY)
        self.assertEqual(SLUG, capability["slug"])
        self.assertEqual(
            [
                "content/gt6-mte-crucible-foundry-runtime",
                "content/gt6-crucible-mold-interaction",
            ],
            capability["depends_on"],
        )
        self.assertEqual(["capability-runtime"], capability["profiles"])
        self.assertEqual("runtime_ready", capability["maturity"])
        self.assertNotEqual("player_complete", capability["maturity"])
        self.assertEqual("accepted", foundry["workflow"])
        self.assertEqual(FOUNDRY_IDS, sorted(foundry["required_test_ids"]))
        self.assertEqual(art.EXPECTED_TESTS, sorted(capability["required_test_ids"]))
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

    def test_voxels_reuse_iconsets_and_skip_large_crucible(self) -> None:
        self.assertEqual([], art.check())
        notes = census.load_json(WAVE / "runtime_notes.json")
        self.assertTrue(notes["no_large_crucible_side"])
        self.assertTrue(notes["no_per_material_png"])
        self.assertTrue(notes["reuse_faucet_metallic"])
        self.assertTrue(notes["foundry_tanks_remain_dummy"])
        self.assertFalse(notes["reopens_foundry_identity"])
        self.assertFalse(notes["reopens_foundry_required_test_ids"])
        self.assertEqual("explicitly_blocked", notes["obtain"])
        self.assertEqual(0, art.extra_png_for_metals())
        self.assertEqual(85, len(art.foundry_rows()))

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
