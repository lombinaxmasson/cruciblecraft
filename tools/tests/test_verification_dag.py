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
                "owner_freeze",
                "owner_snapshot",
                "owner_inventory",
                "owner_b0",
                "owner_overlay",
                "owner_lock",
                "owner_gap_partition",
                "owner_census_delta",
                "owner_topology",
                "owner_readiness",
                "owner_repair_freeze",
                "owner_repair_readiness",
                "owner_runtime_freeze",
                "owner_runtime_overlay",
                "owner_runtime_lock",
                "owner_runtime_gap_partition",
                "owner_runtime_readiness",
                "repair_freeze",
                "repair_inventory",
                "repair_readiness",
            ],
            ids,
        )
        self.assertFalse(dag._has_cycle({node["id"]: node for node in document["nodes"]}))

    def test_receipt_write_refused_when_manifest_missing(self) -> None:
        self.assertIsNone(dag.refuse_receipt_write())

    def test_lifecycle_audit_does_not_rewrite_work_set(self) -> None:
        audit = dag.lifecycle_audit()
        self.assertEqual("electrolyzer/compact", audit["work_set_unique_active_card"])
        self.assertIsNone(audit["topology_unique_active_card"])
        self.assertTrue(audit["frozen_issuance_not_rewritten"])


if __name__ == "__main__":
    unittest.main()
