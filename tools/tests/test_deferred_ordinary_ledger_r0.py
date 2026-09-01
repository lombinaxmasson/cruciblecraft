#!/usr/bin/env python3
"""Deferred ordinary ledger R0: 1845 enumerated once, zero completion."""
from __future__ import annotations

import unittest

from tools import closeout_seal
from tools import recycling_deferred_r0 as r0
from tools import t35_common as t35
from tools.recipe_bulk.slugs import KNOWN_SEMANTIC_SLUGS
from tools.wave_closeout import spec_for

SLUG = "recycling/deferred-ordinary-ledger-r0"
ROOT = t35.TOOLS / "waves" / "recycling" / "deferred-ordinary-ledger-r0"


class DeferredOrdinaryLedgerR0Test(unittest.TestCase):
    def test_slug_and_spec_are_registered(self) -> None:
        self.assertIn(SLUG, KNOWN_SEMANTIC_SLUGS)
        spec = spec_for(SLUG)
        self.assertEqual("recycling/smelter-mte-identity", spec.unique_active_wave)
        self.assertFalse(spec.next_unassigned)
        self.assertEqual(0, spec.owns_families)
        self.assertIsNone(spec.production_lock)
        self.assertIsNone(spec.receipt)

    def test_live_enumeration_is_1845(self) -> None:
        enumerated = r0.enumerate_universe()
        self.assertEqual([], enumerated["errors"])
        self.assertEqual(1845, len(enumerated["universe"]))
        self.assertEqual(1843, enumerated["enumerated_recycling"])
        self.assertEqual(1817, enumerated["cohort_counts"]["smelter_proven_mte_recovery"])
        self.assertEqual(2, enumerated["cohort_counts"]["smelter_recovery_edge"])
        self.assertEqual(1, enumerated["cohort_counts"]["centrifuge_execution_envelope"])
        self.assertEqual(1, enumerated["cohort_counts"]["centrifuge_cross_mod"])
        autoclave = (
            enumerated["cohort_counts"]["autoclave_tagged_recycling"]
            + enumerated["cohort_counts"]["mislabeled_needs_reclass"]
        )
        self.assertEqual(24, autoclave)
        self.assertEqual(
            1819,
            enumerated["cohort_counts"]["smelter_proven_mte_recovery"]
            + enumerated["cohort_counts"]["smelter_recovery_edge"],
        )
        self.assertTrue(enumerated["inherited_recycling"]["enumerated"])
        self.assertFalse(enumerated["inherited_recycling"]["silently_discarded"])

    def test_partition_covers_exactly_once(self) -> None:
        enumerated = r0.enumerate_universe()
        partition = r0.partition_document(enumerated)
        ids = [
            family_id
            for bucket in partition["cohorts"].values()
            for family_id in bucket["family_ids"]
        ]
        self.assertEqual(1845, len(ids))
        self.assertEqual(1845, len(set(ids)))
        self.assertTrue(partition["covers_exactly_once"])
        self.assertEqual(5, len(partition["n300_exceptions"]))

    def test_identity_candidate_unique_metas(self) -> None:
        enumerated = r0.enumerate_universe()
        candidate = enumerated["candidate"]
        self.assertTrue(candidate["unique_meta_equals_proven_family_count"])
        self.assertEqual(1817, candidate["proven_family_count"])
        self.assertEqual(1817, candidate["unique_meta_count"])
        self.assertEqual([], candidate["duplicate_meta"])
        self.assertEqual([], candidate["missing_single_mte_input"])
        self.assertEqual(118, candidate["bath_catalog_meta_count"])

    def test_artifacts_are_ready(self) -> None:
        readiness = t35.load_json(ROOT / "readiness.json")
        universe = t35.load_json(ROOT / "deferred_universe.json")
        census = t35.load_json(ROOT / "census_delta.json")
        self.assertEqual("RECYCLING_DEFERRED_LEDGER_R0_READY", readiness["status"])
        self.assertEqual(
            "recycling/smelter-mte-identity", readiness["unique_active_wave"]
        )
        self.assertFalse(readiness["next_unassigned"])
        self.assertEqual(0, int(census["complete_family_count"]))
        self.assertEqual(0, int(census["completion_delta"]))
        self.assertEqual(0, int(census["remaining_recipe_gap"]))
        self.assertTrue(universe["enumerated"])
        self.assertEqual(1845, int(universe["family_count"]))
        self.assertTrue(universe["inherited_recycling"]["enumerated"])
        self.assertFalse(universe["inherited_recycling"]["silently_discarded"])
        self.assertFalse(readiness["evidence"]["recipe_files_generated"])
        self.assertFalse(readiness["evidence"]["one_x_joint_exit"])

    def test_seal_is_current(self) -> None:
        self.assertEqual([], closeout_seal.check_wave_seal(SLUG))
        seal = t35.load_json(ROOT / "closeout_seal.json")
        self.assertEqual("SEALED", seal["status"])
        self.assertEqual(0, int(seal["complete_family_count"]))
        self.assertEqual("NONE", seal["gametest_status"])
        self.assertIsNone(seal["production_lock_sha256"])

    def test_no_recipe_tree(self) -> None:
        generated = (
            t35.ROOT
            / "src"
            / "recipe_generated"
            / "resources"
            / "data"
            / "cruciblecraft"
            / "recipe"
            / "recycling"
        )
        self.assertFalse(generated.is_dir() and any(generated.rglob("*.json")))


if __name__ == "__main__":
    unittest.main()
