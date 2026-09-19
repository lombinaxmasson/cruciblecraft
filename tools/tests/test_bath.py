#!/usr/bin/env python3
"""Bath host card: stainless machine, bathing pots, no glue stand-in."""
from __future__ import annotations

import unittest

from tools import capability_ledger as ledger
from tools import census_common as census
from tools import gt6_bath as bath

SLUG = "machines/bath"
ROOT = census.ROOT
CAPABILITY = ROOT / "tools" / "capabilities" / "machines" / "bath" / "capability.json"
PLAN_ACTIVE = ROOT / "docs" / "history" / "card-plans" / "active" / "洗矿浴池详细计划.md"
PLAN_CLOSED = ROOT / "docs" / "history" / "card-plans" / "closed" / "洗矿浴池详细计划.md"


class BathHostTest(unittest.TestCase):
    def test_capability_unique_active_and_ids(self) -> None:
        capability = census.load_json(CAPABILITY)
        self.assertEqual(SLUG, capability["slug"])
        self.assertEqual(["capability-runtime"], capability["profiles"])
        self.assertNotEqual("player_complete", capability["maturity"])
        self.assertEqual(bath.EXPECTED_TESTS, capability["required_test_ids"])
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

    def test_source_exact_hosts_and_blocked_glue(self) -> None:
        self.assertEqual([], bath.check())
        capability = census.load_json(CAPABILITY)
        blocked = {
            row["semantic_key"]: row
            for row in capability["identity_disposition"]
            if row["disposition"] == "blocked"
        }
        self.assertIn("mte:wooden_bathing_pot:32721-32720", blocked)
        self.assertIn("machine:bath:large-vat-17104", blocked)
        self.assertIn("Obtain stays explicitly_blocked", capability["note"])

    def test_not_a_recipe_import_wave(self) -> None:
        from tools.recipe_bulk.slugs import KNOWN_SEMANTIC_SLUGS

        self.assertNotIn(SLUG, KNOWN_SEMANTIC_SLUGS)


if __name__ == "__main__":
    unittest.main()
