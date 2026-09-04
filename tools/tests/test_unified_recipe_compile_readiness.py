#!/usr/bin/env python3
"""Contract tests for the Phase-3 unified recipe compile-authority closeout."""
from __future__ import annotations

import unittest

from tools import census_common as census
from tools.recipe_bulk import compile_readiness as readiness
from tools.recipe_bulk import schema_lite
from tools.recipe_bulk.waves import COMPILE_ORDER, WAVES


class UnifiedRecipeCompileReadinessTest(unittest.TestCase):
    def test_compile_authority_is_recipe_bulk_for_all_waves(self) -> None:
        for wave_id in COMPILE_ORDER:
            spec = WAVES[wave_id]
            self.assertEqual("recipe_bulk", spec.compile_authority)

    def test_readiness_is_ready_without_family_ownership(self) -> None:
        document = readiness.build()
        self.assertEqual(readiness.STATUS_READY, document["status"], document.get("failed_gates"))
        self.assertEqual([], document["failed_gates"])
        self.assertEqual(0, document["owns_families"])
        self.assertTrue(document["rebuilds_identical"])
        self.assertEqual(7, len(document["wave_bindings"]))
        self.assertTrue(document["gates"]["unique_write_authority"])
        self.assertTrue(document["gates"]["no_legacy_emit_delegate"])
        self.assertTrue(document["gates"]["compact_wave_ledger_coverage"])
        self.assertTrue(document["gates"]["byte_stable_id_parity"])

    def test_schema_accepts_ready_document(self) -> None:
        schema = census.load_json(census.TOOLS / "unified_recipe_compile_readiness.schema.json")
        schema_lite.validate(readiness.build(), schema)


if __name__ == "__main__":
    unittest.main()
