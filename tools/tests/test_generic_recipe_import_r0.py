#!/usr/bin/env python3
"""Generic recipe import R0 registration and frozen artifacts."""
from __future__ import annotations

import unittest

from tools import portfolio_generic_recipe_import as importer
from tools import census_common as census
from tools.recipe_bulk.slugs import KNOWN_SEMANTIC_SLUGS
from tools.wave_closeout import spec_for


class GenericRecipeImportRegistrationTest(unittest.TestCase):
    def test_slugs_are_registered(self) -> None:
        expected = {
            "portfolio/generic-recipe-generator-r0": (
                "portfolio/generic-recipe-import-core"
            ),
            "portfolio/generic-recipe-import-core": (
                "portfolio/generic-recipe-import-proof"
            ),
            "portfolio/generic-recipe-import-proof": (
                "portfolio/generic-recipe-generator"
            ),
        }
        for slug, nxt in expected.items():
            self.assertIn(slug, KNOWN_SEMANTIC_SLUGS)
            spec = spec_for(slug)
            self.assertEqual(nxt, spec.unique_active_wave)
            self.assertFalse(spec.next_unassigned)
            self.assertEqual(0, spec.owns_families)
            self.assertIsNone(spec.production_lock)

    def test_program_is_terminal(self) -> None:
        slug = "portfolio/generic-recipe-generator"
        self.assertIn(slug, KNOWN_SEMANTIC_SLUGS)
        spec = spec_for(slug)
        self.assertIsNone(spec.unique_active_wave)
        self.assertTrue(spec.next_unassigned)
        self.assertEqual(0, spec.owns_families)


class GenericRecipeImportR0ArtifactsTest(unittest.TestCase):
    def test_r0_when_present(self) -> None:
        root = census.TOOLS / "waves" / "portfolio" / "generic-recipe-generator-r0"
        if not (root / "readiness.json").is_file():
            self.skipTest("R0 artifacts not written yet")
        readiness = census.load_json(root / "readiness.json")
        self.assertEqual("GENERIC_RECIPE_IMPORT_R0_READY", readiness["status"])
        self.assertEqual(
            "portfolio/generic-recipe-import-core",
            readiness["unique_active_wave"],
        )
        self.assertEqual(0, int(readiness["evidence"]["completion_delta"]))
        self.assertFalse(readiness["evidence"]["recipe_files_generated"])
        self.assertEqual(0, int(readiness["evidence"]["combinatorial_completed"]))
        self.assertEqual(0, int(readiness["evidence"]["combinatorial_imported"]))
        inventory = census.load_json(root / "manual_glue_inventory.json")
        kinds = {item["disposition"] for item in inventory["items"]}
        self.assertEqual(
            {
                "extract_to_source_dialect",
                "legacy_frozen",
                "out_of_scope",
                "replace_with_declarative_spec",
                "reuse_as_is",
            },
            kinds,
        )
        later = census.load_json(root / "later_combinatorial.json")
        self.assertEqual(4, len(later["families"]))
        self.assertEqual(1274, int(later["relation_count"]))
        self.assertEqual(0, int(later["completed"]))
        self.assertTrue(
            (census.TOOLS / "source_pack_manifest.schema.json").is_file()
        )
        self.assertTrue(
            (census.TOOLS / "recipe_import_spec.schema.json").is_file()
        )


class GenericRecipeImportProgramArtifactsTest(unittest.TestCase):
    def test_program_when_present(self) -> None:
        root = census.TOOLS / "waves" / "portfolio" / "generic-recipe-generator"
        if not (root / "readiness.json").is_file():
            self.skipTest("program artifacts not written yet")
        readiness = census.load_json(root / "readiness.json")
        self.assertEqual("GENERIC_RECIPE_IMPORT_READY", readiness["status"])
        self.assertIsNone(readiness["unique_active_wave"])
        self.assertTrue(readiness["next_unassigned"])
        self.assertEqual(0, int(readiness["evidence"]["combinatorial_completed"]))
        later = census.load_json(root / "later_star_disposition.json")
        self.assertEqual(4, len(later["families"]))
        self.assertFalse(later["started"])
        contract = census.load_json(root / "import_contract.json")
        self.assertFalse(
            contract["existing_host_new_source_pack"]["requires_per_wave_builder"]
        )
        self.assertFalse(
            contract["existing_host_new_source_pack"]["requires_handwritten_wavespec"]
        )
