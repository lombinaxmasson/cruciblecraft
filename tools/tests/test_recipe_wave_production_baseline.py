#!/usr/bin/env python3
"""Contract tests for the frozen compact production baseline."""
from __future__ import annotations

import unittest
from pathlib import Path

from tools import block_object_common as block_object
from tools.recipe_bulk import baseline as baseline_mod
from tools.recipe_bulk.write_guard import (
    ProductionWriteError,
    assert_not_production_write,
    is_authority_protected,
)
from tools.recipe_bulk.waves import SHADOW_ORDER
from tools.tests.support import authority_sandbox

ROOT = Path(__file__).resolve().parents[2]


class RecipeWaveProductionBaselineTest(unittest.TestCase):
    def test_seven_recipe_waves_are_frozen(self) -> None:
        document = baseline_mod.build()
        self.assertEqual("RECIPE_WAVE_PRODUCTION_BASELINE", document["status"])
        self.assertEqual(set(SHADOW_ORDER), set(document["waves"]))
        for wave_id, row in document["waves"].items():
            with self.subTest(wave=wave_id):
                self.assertGreater(row["file_count"], 0)
                self.assertGreater(row["stable_id_count"], 0)
                self.assertEqual(64, len(row["generated_tree_sha256"]))
                self.assertTrue((ROOT / row["generated_root"]).is_dir())

    def test_write_guard_lists_generated_roots(self) -> None:
        document = baseline_mod.build()
        roots = document["write_guard"]["protected_generated_roots"]
        self.assertEqual(len(SHADOW_ORDER), len(roots))
        self.assertTrue(any(path.endswith("/recipe") for path in roots))
        self.assertTrue(any("recipe/assembler/compact" in path for path in roots))

    def test_generated_roots_are_authority_protected(self) -> None:
        assembler = (
            ROOT
            / "src/recipe_generated/resources/data/cruciblecraft/recipe/assembler/compact"
            / "gt_recipe_assembler_0002.json"
        )
        self.assertTrue(assembler.is_file())
        self.assertTrue(is_authority_protected(assembler))
        self.assertTrue(is_authority_protected(block_object.generated_family_files()[0]))

    def test_shadow_write_guard_rejects_generated_root(self) -> None:
        target = block_object.generated_family_files()[0]
        with self.assertRaises(ProductionWriteError):
            assert_not_production_write(target)

    def test_direct_write_to_block_object_generated_is_forbidden(self) -> None:
        authority_sandbox.install_write_guard()
        target = block_object.generated_family_files()[0]
        with self.assertRaises(authority_sandbox.AuthorityWriteError):
            target.write_text("{}\n", encoding="utf-8")


if __name__ == "__main__":
    unittest.main()
