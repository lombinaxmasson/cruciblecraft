#!/usr/bin/env python3
"""Every declared required_test_ids entry exists as a Java test method."""
from __future__ import annotations

import unittest

from tools import capability_ledger as ledger
from tools import player_complete


class RequiredTestIdContractTest(unittest.TestCase):
    def test_every_declared_id_exists_once(self) -> None:
        errors: list[str] = []
        seen = 0
        for path in ledger.capability_files():
            capability = ledger.load_capability(path)
            if not capability.get("required_test_ids"):
                continue
            seen += 1
            errors.extend(player_complete.check_declared_test_ids(capability))
        self.assertGreaterEqual(seen, 100)
        self.assertEqual([], errors)

    def test_missing_id_fails(self) -> None:
        errors = player_complete.evaluate_required_tests(
            "fixture/missing",
            ["realMethod", "missingMethod"],
            {"realMethod": ("src/test/java/Fixture.java:10",)},
        )
        self.assertEqual(
            ["fixture/missing: required test missingMethod was not found"],
            errors,
        )

    def test_renamed_java_method_fails(self) -> None:
        errors = player_complete.evaluate_required_tests(
            "fixture/renamed",
            ["oldName"],
            {"newName": ("src/test/java/Fixture.java:12",)},
        )
        self.assertEqual(
            ["fixture/renamed: required test oldName was not found"],
            errors,
        )

    def test_duplicate_declaration_fails(self) -> None:
        errors = player_complete.evaluate_required_tests(
            "fixture/duplicate",
            ["sameMethod", "sameMethod"],
            {"sameMethod": ("src/test/java/Fixture.java:4",)},
        )
        self.assertEqual(
            ["fixture/duplicate: required_test_ids has duplicates ['sameMethod']"],
            errors,
        )

    def test_owned_java_binds_a_shared_method_name(self) -> None:
        errors = player_complete.evaluate_required_tests(
            "fixture/owned",
            ["sharedName"],
            {
                "sharedName": (
                    "src/test/java/A.java:3",
                    "src/test/java/B.java:9",
                )
            },
            {"src/test/java/A.java"},
        )
        self.assertEqual([], errors)
        errors = player_complete.evaluate_required_tests(
            "fixture/ambiguous",
            ["sharedName"],
            {
                "sharedName": (
                    "src/test/java/A.java:3",
                    "src/test/java/B.java:9",
                )
            },
        )
        self.assertIn("defined in multiple places", errors[0])


if __name__ == "__main__":
    unittest.main()
