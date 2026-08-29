#!/usr/bin/env python3
"""Contract tests for the unified import shadow repair-gate closeout."""
from __future__ import annotations

import unittest

from tools.recipe_bulk import schema_lite
from tools.recipe_bulk import shadow_readiness as readiness
from tools import t35_common as t35

TOOLS = t35.TOOLS


class UnifiedImportShadowReadinessTest(unittest.TestCase):
    def test_committed_ledgers_match_their_schemas(self) -> None:
        pairs = (
            (
                TOOLS / "recipe_wave_production_baseline.json",
                TOOLS / "recipe_wave_production_baseline.schema.json",
            ),
            (
                TOOLS / "recipe_wave_shadow_parity.json",
                TOOLS / "recipe_wave_shadow_parity.schema.json",
            ),
            (
                TOOLS / "global_build_identity_ledger.json",
                TOOLS / "global_build_identity_ledger.schema.json",
            ),
        )
        for path, schema in pairs:
            with self.subTest(path=path.name):
                schema_lite.validate(t35.load_json(path), t35.load_json(schema))

    def test_readiness_is_ready_with_cross_ledger_stable_ids(self) -> None:
        document = readiness.build()
        self.assertEqual(readiness.STATUS_READY, document["status"])
        self.assertEqual([], document["failed_gates"])
        self.assertEqual(0, document["owns_families"])
        self.assertTrue(document["rebuilds_identical"])
        self.assertEqual(7, len(document["wave_bindings"]))
        self.assertEqual(64, len(document["identity_semantic_root_sha256"]))
        self.assertGreater(document["typed_identity_blocker_count"], 0)

    def test_unclassified_blocker_fails_closed(self) -> None:
        self.assertIn("unproven_alias", readiness.TYPED_BLOCKER_CLASSES)
        self.assertNotIn("material_form", readiness.TYPED_BLOCKER_CLASSES)


if __name__ == "__main__":
    unittest.main()
