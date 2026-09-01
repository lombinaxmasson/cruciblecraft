#!/usr/bin/env python3
"""Contract tests for semantic-v3 slug dispatch and empty v3 composition."""
from __future__ import annotations

import unittest

from tools.recipe_bulk import identity_v2
from tools.recipe_bulk import identity_v3
from tools.recipe_bulk import runtime_v2
from tools.recipe_bulk import runtime_v3
from tools.recipe_bulk.slugs import WaveSlugError, gradle_namespace, parse_wave_token
from tools.recipe_bulk.waves import SEMANTIC_COMPILE_ORDER, recipe_wave


class SemanticWaveBootstrapTest(unittest.TestCase):
    def test_semantic_slug_parser_rejects_t50(self) -> None:
        with self.assertRaises(WaveSlugError):
            parse_wave_token("T50", schema="semantic-v3")
        with self.assertRaises(WaveSlugError):
            parse_wave_token("t50", schema="semantic-v3")
        parsed = parse_wave_token("smelter/ordinary-closure", schema="semantic-v3")
        self.assertEqual("smelter/ordinary-closure", parsed.wave_slug)
        self.assertFalse(parsed.is_legacy)

    def test_legacy_schema_still_reads_t49(self) -> None:
        parsed = parse_wave_token("T49", schema="forward-v2")
        self.assertEqual("T49", parsed.wave_id)
        self.assertTrue(parsed.is_legacy)

    def test_gradle_namespace_is_semantic(self) -> None:
        self.assertEqual(
            "cruciblecraft_wave_smelter_ordinary_closure",
            gradle_namespace("smelter/ordinary-closure"),
        )
        self.assertNotIn("t50", gradle_namespace("mixer/ordinary-closure"))

    def test_semantic_wave_specs_exist(self) -> None:
        self.assertEqual(
            ("smelter/ordinary-closure", "mixer/ordinary-closure"),
            SEMANTIC_COMPILE_ORDER,
        )
        smelter = recipe_wave("smelter/ordinary-closure")
        self.assertEqual("smelter/ordinary-closure", smelter.wave_slug)
        self.assertEqual("cohort_nested", smelter.path_layout)
        self.assertTrue(smelter.dry_run_without_lock)
        mixer = recipe_wave("mixer/ordinary-closure")
        self.assertEqual("cruciblecraft:mixer", mixer.host)

    def test_empty_v3_identity_equals_frozen_v2_logical_root(self) -> None:
        v2 = identity_v2.compose()
        v3 = identity_v3.compose()
        self.assertEqual(
            (v2.get("composition") or {}).get("semantic_root_sha256"),
            (v3.get("composition") or {}).get("v2_logical_identity_root_sha256"),
        )
        if identity_v3.DELTA_ORDER:
            self.assertEqual(
                list(identity_v3.DELTA_ORDER),
                [
                    row["wave_slug"]
                    for row in (v3.get("composition") or {}).get("consumed_deltas") or []
                ],
            )
            self.assertGreaterEqual(v3.get("record_count"), v2.get("record_count"))
        else:
            self.assertTrue((v3.get("composition") or {}).get("logical_identities_equal_v2"))
            self.assertEqual([], (v3.get("composition") or {}).get("consumed_deltas"))
            self.assertEqual(v2.get("record_count"), v3.get("record_count"))

    def test_empty_v3_runtime_equals_frozen_v2_groups(self) -> None:
        v2 = runtime_v2.compose()
        v3 = runtime_v3.compose()
        self.assertEqual(
            (v2.get("composition") or {}).get("semantic_root_sha256"),
            (v3.get("composition") or {}).get("v2_logical_identity_root_sha256"),
        )
        if runtime_v3.DELTA_ORDER:
            self.assertGreaterEqual(v3.get("group_count"), v2.get("group_count"))
            self.assertEqual(
                list(runtime_v3.DELTA_ORDER),
                [
                    row["wave_slug"]
                    for row in (v3.get("composition") or {}).get("consumed_deltas") or []
                ],
            )
        else:
            self.assertEqual(v2.get("group_count"), v3.get("group_count"))
            self.assertTrue((v3.get("composition") or {}).get("logical_identities_equal_v2"))
            self.assertEqual([], (v3.get("composition") or {}).get("consumed_deltas"))

    def test_ordinary_closure_required_forms_are_wave_paths(self) -> None:
        from tools import material_form_authority as authority

        smelter = authority.source_by_id("smelter_ordinary_closure_required_forms")
        self.assertTrue(str(smelter["path"]).startswith("tools/waves/"))
        self.assertFalse(str(smelter["id"]).endswith("_ordinary_required_forms"))
        self.assertTrue(str(smelter["id"]).endswith("_ordinary_closure_required_forms"))


if __name__ == "__main__":
    unittest.main()
