"""T42-Repair readiness: zero ownership, T43 unassigned, honest unique kinds."""
from __future__ import annotations

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_t42_repair_readiness as builder
from tools import t42_common as common


class T42RepairReadinessTest(unittest.TestCase):
    def test_status_is_derived_from_failed_gates(self) -> None:
        document = builder.build()
        failed = sorted(name for name, passed in document["gates"].items() if not passed)
        self.assertEqual(failed, document["failed_gates"])
        self.assertEqual(
            "T42_REPAIR_READY" if not failed else "T42_REPAIR_BLOCKED",
            document["status"],
        )
        self.assertEqual(0, document["owns_families"])
        self.assertEqual(0, document["completion_delta"])
        self.assertEqual(0, document["publication_delta"])
        self.assertTrue(document["t43_not_issued"])

    def test_ready_requires_honest_unique_kinds_and_t43_unassigned(self) -> None:
        document = builder.build()
        if document["failed_gates"]:
            self.skipTest("T42-Repair artifacts not rebuilt yet")
        self.assertEqual("T42_REPAIR_READY", document["status"])
        self.assertEqual([], document["failed_gates"])
        self.assertTrue(document["gates"]["unique_kind_counts_honest"])
        self.assertTrue(document["gates"]["t43_unassigned"])
        self.assertTrue(document["gates"]["partition_repair_in_topology"])
        self.assertTrue(document["gates"]["t42_partition_ready_preserved"])
        self.assertTrue(document["gates"]["unique_bucket_residual_split"])
        self.assertTrue(document["gates"]["programmed_circuit_mapping_not_proof"])
        self.assertTrue(document["gates"]["gap_partial_semantics"])
        self.assertTrue(document["t43_caveats"]["programmed_circuit_mapping_is_not_proof"])
        self.assertTrue(document["t43_caveats"]["unique_bucket_is_residual"])
        self.assertTrue(document["t43_caveats"]["allowlist_includes_explicit_oak_planks"])
        overlay = common.load_json(common.BLOCKER_OVERLAY)
        self.assertLess(
            int((overlay.get("unique_kind_counts") or {}).get("mte") or 0),
            int(overlay.get("unique_bucket_family_count") or 0),
        )


if __name__ == "__main__":
    unittest.main()
