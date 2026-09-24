#!/usr/bin/env python3
"""Checks the source-backed live Jade migration ledger."""
from __future__ import annotations

import json
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
LEDGER = (
    ROOT
    / "tools"
    / "waves"
    / "presentation"
    / "gt6-tfru-waila-jade"
    / "live_provider_mapping.json"
)
INVENTORY = (
    ROOT
    / "tools"
    / "waves"
    / "presentation"
    / "gt6-tfru-waila-jade"
    / "source_inventory.json"
)
MATRIX = ROOT / "tools" / "jade_observation_matrix.json"


def read_json(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


class JadeSourceMappingTest(unittest.TestCase):
    def test_every_migrated_source_class_is_in_inventory(self) -> None:
        ledger = read_json(LEDGER)
        inventory = read_json(INVENTORY)
        source_classes = {
            row["class"]
            for row in inventory["classes"]
        }
        source_classes.update(
            row.get("source_class")
            for row in inventory["registrations"]
            if row.get("source_class")
        )
        missing = [
            (row["provider"], source_class)
            for row in ledger["providers"]
            if row["status"] == "migrated"
            for source_class in row["source_classes"]
            if source_class not in source_classes
        ]
        self.assertEqual([], missing)

    def test_blocked_rows_have_reasons_and_sets_are_disjoint(self) -> None:
        ledger = read_json(LEDGER)
        providers = ledger["providers"]
        names = [row["provider"] for row in providers]
        self.assertEqual(len(names), len(set(names)))
        self.assertTrue(all(
            row["blocked_reason"]
            for row in providers
            if row["status"] == "blocked"
        ))
        self.assertTrue(all(
            row["status"] in {"migrated", "blocked"}
            for row in providers
        ))
        migrated = set(ledger["source_migration"]["migrated_providers"]) \
            if "source_migration" in ledger else {
                row["provider"]
                for row in providers
                if row["status"] == "migrated"
            }
        blocked = set(ledger["source_migration"]["blocked_providers"]) \
            if "source_migration" in ledger else {
                row["provider"]
                for row in providers
                if row["status"] == "blocked"
            }
        self.assertFalse(migrated & blocked)
        self.assertEqual(
            migrated,
            {
                row["provider"]
                for row in providers
                if row["status"] == "migrated"
            },
        )
        self.assertEqual(
            blocked,
            {
                row["provider"]
                for row in providers
                if row["status"] == "blocked"
            },
        )

    def test_matrix_points_to_the_same_ledger(self) -> None:
        ledger = read_json(LEDGER)
        matrix = read_json(MATRIX)
        migration = matrix["source_waila_migration"]
        self.assertEqual(
            "tools/waves/presentation/gt6-tfru-waila-jade/live_provider_mapping.json",
            migration["ledger"],
        )
        self.assertEqual(
            {
                row["provider"]
                for row in ledger["providers"]
                if row["status"] == "migrated"
            },
            set(migration["migrated_providers"]),
        )
        self.assertEqual(
            {
                row["provider"]
                for row in ledger["providers"]
                if row["status"] == "blocked"
            },
            set(migration["blocked_providers"]),
        )


if __name__ == "__main__":
    unittest.main()
