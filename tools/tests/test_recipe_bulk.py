#!/usr/bin/env python3
"""Contract tests for the reusable recipe bulk compiler."""
from __future__ import annotations

import unittest

from tools.recipe_bulk.resolver import resolve_operand
from tools.recipe_bulk.templates import expand_family
from tools.recipe_bulk import replay as replay_mod


class RecipeBulkCompilerTest(unittest.TestCase):
    def test_unresolved_does_not_guess_by_display_name(self) -> None:
        result = resolve_operand(
            {
                "source": {
                    "displayName": "Looks Like A Block",
                    "item": "gregtech:gt.block.unknown",
                    "meta": 0,
                }
            }
        )
        self.assertEqual("unresolved", result["class"])
        self.assertIsNone(result["runtime_id"])

    def test_exact_singleton_rejects_multi_relation(self) -> None:
        with self.assertRaises(ValueError):
            expand_family(
                family_id="x",
                relations=[{}, {}],
                template_kind="exact_singleton",
            )

    def test_t43_replay_matches_generated_root(self) -> None:
        document = replay_mod.replay_t43()
        self.assertTrue(document["ok"], document.get("mismatches"))
        self.assertEqual(407, document["compared"])
