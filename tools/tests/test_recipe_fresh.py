#!/usr/bin/env python3
"""Fresh recipes profile: isolated compile, no receipt PASS, no stand-in lock."""
from __future__ import annotations

import json
import tempfile
import unittest
from pathlib import Path

from tools import build_recipe_fresh
from tools import census_common as census
from tools.recipe_bulk import source_import
from tools.recipe_bulk.pilot import reviewed_lock
from tools.recipe_bulk.source_import import SourceImportError


class RecipeFreshProfileTest(unittest.TestCase):
    def test_isolated_small_and_large_compile(self) -> None:
        errors = build_recipe_fresh.check()
        self.assertEqual([], errors)
        metrics = json.loads(
            (
                census.ROOT / "build" / "verification" / "recipe_fresh_metrics.json"
            ).read_text(encoding="utf-8")
        )
        self.assertEqual("FRESH", metrics["status"])
        self.assertTrue(metrics["receipts_are_not_pass"])
        self.assertEqual("not_executed", metrics["reload_sync_retained_transient"])
        self.assertEqual(1, metrics["small"]["family_count"])
        self.assertEqual(1, metrics["small"]["relation_count"])
        self.assertEqual(1, metrics["small"]["representations"]["exact"])
        self.assertEqual(1, metrics["large"]["family_count"])
        self.assertGreaterEqual(metrics["large"]["relation_count"], 2)
        self.assertEqual(1, metrics["large"]["representations"]["exact_multi"])

    def test_import_source_refuses_production_lock_and_generated_tree(self) -> None:
        document = census.load_json(build_recipe_fresh.SMELTER)
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "recipe_import.json"
            locked = dict(document)
            locked["output_paths"] = dict(document["output_paths"])
            locked["output_paths"]["receipt"] = (
                "src/test/resources/generic_recipe_import/"
                "smelter_exact_singleton/production_lock.json"
            )
            path.write_text(json.dumps(locked), encoding="utf-8")
            with self.assertRaises(SourceImportError):
                source_import.import_documents(path)
            generated = dict(document)
            generated["output_paths"] = dict(document["output_paths"])
            generated["output_paths"]["source"] = (
                "src/test/resources/generic_recipe_import/"
                "smelter_exact_singleton/recipe_generated/source.json"
            )
            path.write_text(json.dumps(generated), encoding="utf-8")
            with self.assertRaises(SourceImportError):
                source_import.import_documents(path)

    def test_blocked_candidate_cannot_become_production_lock(self) -> None:
        with self.assertRaisesRegex(ValueError, "blocked/unresolved"):
            reviewed_lock(
                {
                    "import_slug": "generic-import/blocked",
                    "families": [
                        {
                            "blocked": True,
                            "family_id": "family-a",
                            "relation_count": 1,
                            "representation": "exact",
                        }
                    ],
                },
                publication_group="cruciblecraft:smelter/pilot",
                cohort="pilot",
            )

if __name__ == "__main__":
    unittest.main()
