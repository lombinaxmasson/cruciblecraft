#!/usr/bin/env python3
"""Smelter MTE identity catalog: 1817 exact metas, Bath overlap mapped."""
from __future__ import annotations

import unittest

from tools import closeout_seal
from tools import census_common as census
from tools.recipe_bulk.ordinary_source import load_mte_runtime
from tools.recipe_bulk.slugs import KNOWN_SEMANTIC_SLUGS
from tools.wave_closeout import spec_for

SLUG = "recycling/smelter-mte-identity"
ROOT = census.TOOLS / "waves" / "recycling" / "smelter-mte-identity"
CATALOG = census.TOOLS / "smelter_mte_identity_catalog.json"


class SmelterMteIdentityTest(unittest.TestCase):
    def test_slug_and_spec_are_registered(self) -> None:
        self.assertIn(SLUG, KNOWN_SEMANTIC_SLUGS)
        spec = spec_for(SLUG)
        self.assertEqual("smelter/deferred-recycling", spec.unique_active_wave)
        self.assertFalse(spec.next_unassigned)
        self.assertEqual(0, spec.owns_families)

    def test_catalog_is_1817_exact_metas(self) -> None:
        catalog = census.load_json(CATALOG)
        self.assertEqual("SMELTER_MTE_IDENTITY_CATALOG", catalog["status"])
        self.assertEqual(1817, int(catalog["source_meta_count"]))
        self.assertEqual(1817, len(catalog["identities"]))
        metas = [int(row["meta"]) for row in catalog["identities"]]
        self.assertEqual(1817, len(set(metas)))
        overlap = [
            row
            for row in catalog["identities"]
            if row["registry_kind"] == "existing_item"
        ]
        created = [
            row for row in catalog["identities"] if row["registry_kind"] == "item"
        ]
        self.assertEqual(len(overlap), int(catalog["bath_overlap_count"]))
        self.assertEqual(len(created), int(catalog["new_item_count"]))
        self.assertEqual(1817, len(overlap) + len(created))
        for row in overlap:
            self.assertTrue(str(row["runtime_id"]).startswith("cruciblecraft:"))
            self.assertEqual("exact_item", row["mapping_class"])
        for row in created:
            self.assertEqual(f"gt_mte/mte_{row['meta']}", row["registry_path"])

    def test_mte_runtime_overlay_covers_catalog(self) -> None:
        catalog = census.load_json(CATALOG)
        mapped = load_mte_runtime()
        for row in catalog["identities"]:
            self.assertEqual(
                row["runtime_id"],
                mapped[("gregtech:gt.multitileentity", int(row["meta"]))],
                row["family_id"],
            )

    def test_artifacts_are_ready(self) -> None:
        readiness = census.load_json(ROOT / "readiness.json")
        self.assertEqual("SMELTER_MTE_IDENTITY_READY", readiness["status"])
        self.assertEqual("smelter/deferred-recycling", readiness["unique_active_wave"])
        self.assertFalse(readiness["evidence"]["recipe_files_generated"])
        self.assertEqual(0, int(readiness["evidence"]["completion_delta"]))

    def test_seal_is_current(self) -> None:
        self.assertEqual([], closeout_seal.check_wave_seal(SLUG))

    def test_no_recovery_completion(self) -> None:
        readiness = census.load_json(ROOT / "readiness.json")
        self.assertFalse(readiness["evidence"]["recipe_files_generated"])
        self.assertEqual(0, int(readiness["evidence"]["completion_delta"]))
        self.assertEqual(0, int(readiness["evidence"]["complete_family_count"]))


if __name__ == "__main__":
    unittest.main()
