#!/usr/bin/env python3
"""MTE identity disposition R0 registration and frozen artifacts."""
from __future__ import annotations

import unittest

from tools import portfolio_mte_identity_disposition as mte
from tools import census_common as census
from tools.recipe_bulk.slugs import KNOWN_SEMANTIC_SLUGS
from tools.wave_closeout import spec_for

SLUG = "portfolio/mte-identity-disposition-r0"


class MteIdentityDispositionR0RegistrationTest(unittest.TestCase):
    def test_slug_is_terminal(self) -> None:
        self.assertIn(SLUG, KNOWN_SEMANTIC_SLUGS)
        spec = spec_for(SLUG)
        self.assertIsNone(spec.unique_active_wave)
        self.assertTrue(spec.next_unassigned)
        self.assertEqual(0, spec.owns_families)
        self.assertIsNone(spec.production_lock)


class MteIdentityDispositionR0ArtifactsTest(unittest.TestCase):
    def test_artifacts_when_present(self) -> None:
        root = census.TOOLS / "waves" / "portfolio" / "mte-identity-disposition-r0"
        if not (root / "readiness.json").is_file():
            self.skipTest("R0 artifacts not written yet")
        self.assertEqual([], mte.check_artifacts())
        readiness = census.load_json(root / "readiness.json")
        self.assertEqual("MTE_IDENTITY_DISPOSITION_R0_READY", readiness["status"])
        self.assertIsNone(readiness["unique_active_wave"])
        self.assertTrue(readiness["next_unassigned"])
        evidence = readiness["evidence"]
        self.assertEqual(0, int(evidence["completion_delta"]))
        self.assertEqual(0, int(evidence["owns_families"]))
        self.assertEqual(0, int(evidence["generated_recipe_count"]))
        self.assertFalse(evidence["recipe_files_generated"])
        self.assertEqual(1817, int(evidence["catalog_identities"]))
        self.assertEqual(33, int(evidence["attachment_candidate"]))
        self.assertEqual(0, int(evidence["unmatched"]))
        self.assertEqual(23, int(evidence["wooden_panel_count"]))
        self.assertEqual(63, int(evidence["axle_gearbox_count"]))
        self.assertTrue(evidence["dust_funnel_realized_natively"])
        self.assertFalse(evidence["allows_implementation_child"])
        self.assertFalse(evidence["nuclear_track_c_started"])
        ledger = census.load_json(root / "disposition_ledger.json")
        by_meta = {int(row["meta"]): row for row in ledger["identities"]}
        self.assertEqual(1817, len(by_meta))
        self.assertEqual("attachment_candidate", by_meta[1700]["disposition"])
        self.assertEqual("attachment_candidate", by_meta[32730]["disposition"])
        self.assertEqual("attachment_candidate", by_meta[32749]["disposition"])
        self.assertEqual("attachment_candidate", by_meta[32061]["disposition"])
        self.assertEqual("attachment_candidate", by_meta[32725]["disposition"])
        self.assertEqual("realized_natively", by_meta[32704]["disposition"])
        self.assertEqual("DustFunnelBlock / steel_dust_funnel", by_meta[32704]["evidence"])
        wooden = [
            row for row in ledger["identities"] if row["english_name"] == "Wooden Panel"
        ]
        self.assertEqual(23, len(wooden))
        self.assertEqual({"decorative"}, {row["family"] for row in wooden})
        drive = [row for row in ledger["identities"] if row["family"] == "drive"]
        self.assertEqual(63, len(drive))
        self.assertTrue(all(row["disposition"] == "identity_only" for row in drive))
        feasibility = census.load_json(root / "feasibility.json")
        self.assertEqual("requires_new_runtime", feasibility["verdict"])
        self.assertFalse(feasibility["allows_implementation_child"])
        topology = census.load_json(root / "topology.json")
        self.assertEqual(set(mte.ALLOWED_TOPOLOGY_KEYS), set(topology))
        self.assertIsNone(topology["unique_active_wave"])
        wave = census.load_json(root / "wave.json")
        self.assertEqual(SLUG, wave["program"])
        self.assertEqual(["recycling/smelter-mte-identity"], wave["depends_on"])
        self.assertFalse(
            (
                census.ROOT
                / "tools"
                / "capabilities"
                / "portfolio"
                / "mte-identity-disposition-r0"
                / "capability.json"
            ).is_file()
        )


if __name__ == "__main__":
    unittest.main()
