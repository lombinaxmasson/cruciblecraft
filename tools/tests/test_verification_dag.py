"""Verification DAG tests."""
from __future__ import annotations

import unittest

from tools import run_verification_dag as dag


class VerificationDagTest(unittest.TestCase):
    def test_dag_is_acyclic_and_ordered(self) -> None:
        document = dag.load_dag()
        ids = [node["id"] for node in document["nodes"]]
        self.assertEqual(
            [
                "source_and_material_gate",
                "generated_resources_and_runtime_manifest",
                "java_and_gametest_behavior",
                "receipt",
                "census_delta",
                "topology",
                "readiness",
                "t42_freeze",
                "t42_snapshot",
                "t42_inventory",
                "t42_b0",
                "t42_overlay",
                "t42_lock",
                "t42_gap_partition",
                "t42_census_delta",
                "t42_topology",
                "t42_readiness",
                "t42_repair_freeze",
                "t42_repair_readiness",
                "t42_owner_freeze",
                "t42_owner_overlay",
                "t42_owner_lock",
                "t42_owner_gap_partition",
                "t42_owner_readiness",
                "t36_repair_freeze",
                "t36_repair_inventory",
                "t36_repair_readiness",
            ],
            ids,
        )
        self.assertFalse(dag._has_cycle({node["id"]: node for node in document["nodes"]}))

    def test_receipt_write_refused_when_manifest_missing(self) -> None:
        self.assertIsNone(dag.refuse_receipt_write())

    def test_lifecycle_audit_does_not_rewrite_work_set(self) -> None:
        audit = dag.lifecycle_audit()
        self.assertEqual("T40", audit["work_set_unique_active_card"])
        self.assertIsNone(audit["topology_unique_active_card"])
        self.assertTrue(audit["frozen_issuance_not_rewritten"])


if __name__ == "__main__":
    unittest.main()
