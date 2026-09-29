#!/usr/bin/env python3
"""GT6 redstone-wire runtime child: 3 upgrade_live_item identities."""
from __future__ import annotations

import unittest

from tools import capability_ledger as ledger
from tools import census_common as census

SLUG = "content/mte-redstone-wire"
ROOT = census.ROOT
WAVE = ROOT / "tools" / "waves" / "content" / "mte-redstone-wire"
CAPABILITY = (
    ROOT / "tools" / "capabilities" / "content" / "mte-redstone-wire" / "capability.json"
)
PLAN_ACTIVE = ROOT / "docs" / "history" / "card-plans" / "active" / "MTE红石线详细计划.md"
PLAN_CLOSED = ROOT / "docs" / "history" / "card-plans" / "closed" / "MTE红石线详细计划.md"
PLAN_PREP = ROOT / "docs" / "history" / "card-plans" / "prep" / "MTE红石线详细计划.md"
ASSETS = ROOT / "src" / "main" / "resources" / "assets" / "cruciblecraft"
SOURCE_IDS = [27000, 27050, 27500]
LIVE_PATHS = {
    27000: "red_alloy/wire",
    27050: "signalum/wire",
    27500: "lumium/wire",
}


class MteRedstoneWireCardTest(unittest.TestCase):
    def test_d0_three_hosts_are_explicitly_blocked(self) -> None:
        document = census.load_json(WAVE / "d0_obtain_matrix.json")
        hosts = {row["host"]: row for row in document["hosts"]}
        self.assertEqual(SOURCE_IDS, list(hosts))
        for host in SOURCE_IDS:
            self.assertEqual("explicitly_blocked", hosts[host]["status"])
            self.assertEqual(LIVE_PATHS[host], hosts[host]["path"])
        self.assertNotIn("programmed_circuit", str(document))

    def test_identity_resolution_is_upgrade_live_item(self) -> None:
        document = census.load_json(WAVE / "identity_resolution_ledger.json")
        rows = {row["meta"]: row for row in document["rows"]}
        self.assertEqual(SOURCE_IDS, list(rows))
        for meta, path in LIVE_PATHS.items():
            self.assertEqual("upgrade_live_item", rows[meta]["disposition"])
            self.assertEqual(path, rows[meta]["registry_path"])
            self.assertEqual(f"cruciblecraft:{path}", rows[meta]["live_item"])
            self.assertEqual(f"cruciblecraft:{path}", rows[meta]["live_block"])
            self.assertEqual("existing_item", rows[meta]["registry_kind"])
        extras = {row["meta"] for row in document["out_of_denominator"]}
        self.assertEqual({27006, 27056, 27506}, extras)
        self.assertNotIn("keep_distinct", str(document))

    def test_capability_profiles_and_tests(self) -> None:
        capability = census.load_json(CAPABILITY)
        self.assertEqual(SLUG, capability["slug"])
        self.assertEqual(["capability-runtime"], capability["profiles"])
        self.assertEqual(
            [
                "foldedOntoWireGt01NotVanilla",
                "lumiumEmitsLight",
                "noSurvivalRecipes",
                "threeIdentitiesAreRegistered",
                "weakAndStrongRedstone",
            ],
            capability["required_test_ids"],
        )
        self.assertNotEqual("player_complete", capability["maturity"])
        compiled = ledger.compile_ledger()
        if capability["workflow"] == "active":
            self.assertEqual(SLUG, compiled["unique_active_slug"])
            self.assertTrue(PLAN_ACTIVE.is_file())
            self.assertFalse(PLAN_CLOSED.is_file())
            self.assertFalse(PLAN_PREP.is_file())
        else:
            self.assertEqual("accepted", capability["workflow"])
            self.assertNotEqual(SLUG, compiled["unique_active_slug"])
            self.assertFalse(PLAN_ACTIVE.is_file())
            self.assertTrue(PLAN_CLOSED.is_file())
        reused = {
            row["semantic_key"]
            for row in capability["identity_disposition"]
            if row["disposition"] == "reuse_canonical"
        }
        self.assertEqual(
            {
                "connector:redstone-wire:27000-27500",
                "connector:redstone-wire:covers",
            },
            reused,
        )
        blocked = {
            row["semantic_key"]
            for row in capability["identity_disposition"]
            if row["disposition"] == "blocked"
        }
        self.assertEqual(
            {
                "connector:redstone-wire:insulated-extras",
            },
            blocked,
        )
        runtime_ids = next(
            row["runtime_ids"]
            for row in capability["identity_disposition"]
            if row["semantic_key"] == "connector:redstone-wire:27000-27500"
        )
        self.assertEqual(
            [
                "cruciblecraft:red_alloy/wire",
                "cruciblecraft:signalum/wire",
                "cruciblecraft:lumium/wire",
            ],
            runtime_ids,
        )

    def test_art_is_copied_and_not_aliased(self) -> None:
        manifest = census.load_json(ASSETS / "gt6_redstone_wire_art_manifest.json")
        self.assertEqual(2, len(manifest["imports"]))
        for row in manifest["imports"]:
            destination = ROOT / "src" / "main" / "resources" / row["destination"]
            self.assertTrue(destination.is_file(), row["destination"])
            self.assertNotIn("oven", row["destination"])
            self.assertNotIn("multiblock_casing", row["destination"])
            self.assertNotIn("pipe_filter_cover", row["destination"])
            self.assertNotIn("iron_ingot", row["destination"])
            self.assertEqual(
                [
                    "cruciblecraft:red_alloy/wire",
                    "cruciblecraft:signalum/wire",
                    "cruciblecraft:lumium/wire",
                ],
                row["runtime_ids"],
            )

    def test_unique_active_docs_and_ledger_hand_off(self) -> None:
        topology = census.load_json(WAVE / "topology.json")
        readiness = census.load_json(WAVE / "readiness.json")
        notes = census.load_json(WAVE / "runtime_notes.json")
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
        self.assertFalse(notes["covers_allowed"])
        self.assertEqual("explicitly_blocked", notes["obtain"])
        self.assertEqual("runtime_ready", notes["close_target"])
        ns = (
            ROOT
            / "src"
            / "main"
            / "resources"
            / "data"
            / "cruciblecraft_wave_content_mte_redstone_wire"
        )
        self.assertTrue((ns / "structure" / "empty.nbt").is_file())
        self.assertTrue((ns / "gametest" / "structure" / "empty.nbt").is_file())
        for path in LIVE_PATHS.values():
            self.assertTrue(
                (ASSETS / "blockstates" / path).with_suffix(".json").is_file(),
                path,
            )
            self.assertTrue(
                (ASSETS / "models" / "item" / path).with_suffix(".json").is_file(),
                path,
            )
        self.assertFalse(
            (ASSETS / "blockstates" / "redstone_wire" / "red_alloy.json").is_file()
        )
        self.assertFalse((ASSETS / "blockstates" / "lumium" / "wirelamp.json").is_file())

    def test_live_recipes_do_not_keep_withdrawn_dummy_ids(self) -> None:
        dummy_ids = (
            "cruciblecraft:redstone_wire/red_alloy",
            "cruciblecraft:redstone_wire/signalum",
            "cruciblecraft:lumium/wirelamp",
        )
        live_roots = (
            ROOT / "src" / "recipe_generated",
            ROOT / "src" / "main" / "resources" / "data",
        )
        leftover: list[str] = []
        for root in live_roots:
            if not root.is_dir():
                continue
            for path in root.rglob("*.json"):
                text = path.read_text(encoding="utf-8")
                for dummy in dummy_ids:
                    if dummy in text:
                        leftover.append(f"{census.relative(path)}:{dummy}")
        self.assertEqual([], leftover)


if __name__ == "__main__":
    unittest.main()
