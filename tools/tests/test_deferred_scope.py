#!/usr/bin/env python3
"""Post-1.x scope children and deferred ordinary runtime program closeout."""
from __future__ import annotations

import unittest

from tools import recycling_deferred_scope as scope
from tools import census_common as census
from tools.recipe_bulk.slugs import KNOWN_SEMANTIC_SLUGS
from tools.wave_closeout import spec_for


class DeferredScopeChildTest(unittest.TestCase):
    def test_children_are_registered(self) -> None:
        expected = {
            "smelter/deferred-recycling-edge": (
                "autoclave/deferred-recycling",
                2,
            ),
            "autoclave/deferred-recycling": (
                "recycling/non-recycling-scope",
                24,
            ),
            "recycling/non-recycling-scope": (
                "recycling/deferred-ordinary-runtime",
                2,
            ),
        }
        for slug, (nxt, owns) in expected.items():
            self.assertIn(slug, KNOWN_SEMANTIC_SLUGS)
            spec = spec_for(slug)
            self.assertEqual(nxt, spec.unique_active_wave)
            self.assertFalse(spec.next_unassigned)
            self.assertEqual(owns, spec.owns_families)
            self.assertIsNone(spec.production_lock)

    def test_program_is_terminal(self) -> None:
        slug = "recycling/deferred-ordinary-runtime"
        self.assertIn(slug, KNOWN_SEMANTIC_SLUGS)
        spec = spec_for(slug)
        self.assertIsNone(spec.unique_active_wave)
        self.assertTrue(spec.next_unassigned)
        self.assertEqual(0, spec.owns_families)

    def test_edge_reasons_cover_alumina_and_lava(self) -> None:
        self.assertIn("gt.recipe.smelter#1829", scope.EDGE_REASONS)
        self.assertIn("gt.recipe.smelter#1884", scope.EDGE_REASONS)
        self.assertIn("alumina", scope.EDGE_REASONS["gt.recipe.smelter#1829"])
        self.assertIn("lava", scope.EDGE_REASONS["gt.recipe.smelter#1884"].lower())

    def test_written_dispositions_when_present(self) -> None:
        for slug, child in scope.CHILDREN.items():
            root = census.TOOLS / "waves" / slug
            path = root / "scope_dispositions.json"
            if not path.is_file():
                continue
            document = census.load_json(path)
            rows = document["dispositions"]
            self.assertEqual(child.owns_families, len(rows))
            for row in rows:
                self.assertEqual("post_1x_scope", row["disposition"])
                self.assertIn(row["disposition"], scope.ALLOWED)
                self.assertFalse(str(row["future_owner"]).startswith("later:"))
            census = census.load_json(root / "census_delta.json")
            self.assertEqual(0, int(census["completion_delta"]))
            self.assertEqual(0, int(census["remaining_recipe_gap"]))
            remaining = census["remaining_ordinary"]
            self.assertEqual(
                child.opening_deferred_recycling - child.drop_recycling,
                int(remaining["deferred_recycling_count"]),
            )
            self.assertEqual(
                child.opening_deferred_total - child.drop_total,
                int(remaining["deferred_total"]),
            )


class DeferredOrdinaryRuntimeTest(unittest.TestCase):
    def test_program_artifacts_when_present(self) -> None:
        root = census.TOOLS / "waves" / "recycling" / "deferred-ordinary-runtime"
        if not (root / "readiness.json").is_file():
            self.skipTest("program artifacts not written yet")
        readiness = census.load_json(root / "readiness.json")
        self.assertEqual("DEFERRED_ORDINARY_RUNTIME_READY", readiness["status"])
        self.assertIsNone(readiness["unique_active_wave"])
        self.assertTrue(readiness["next_unassigned"])
        ledger = census.load_json(root / "deferred_ledger.json")
        buckets = ledger["deferred_buckets"]
        self.assertEqual(0, int(buckets["later:recycling"]))
        self.assertEqual(0, int(buckets["later:cross_mod"]))
        self.assertEqual(0, int(buckets["later:execution_envelope/gt6_panel"]))
        replay = census.load_json(root / "gap_replay.json")
        self.assertEqual(0, int(replay["execution_gap"]))
        self.assertEqual(1817, int(replay["complete_family_count"]))
        self.assertEqual(28, int(replay["post_1x_scope_count"]))


if __name__ == "__main__":
    unittest.main()
