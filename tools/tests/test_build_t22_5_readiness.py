"""Tests for the T22.5 readiness builder (D1)."""
from __future__ import annotations

import json
import sys
import unittest
from pathlib import Path

TOOLS = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(TOOLS))

import build_t22_5_readiness as builder  # noqa: E402


class T225ReadinessTest(unittest.TestCase):

    def test_committed_artifact_is_current(self) -> None:
        if not builder.OUTPUT.is_file():
            self.skipTest("artifact not yet generated")
        self.assertEqual(builder.check(), [])

    def test_status_derives_from_evidence_not_handwriting(self) -> None:
        document = builder.build()
        self.assertEqual(document["status"], "T22_5_READY")
        self.assertEqual(
            document["completed_stages"],
            ["T22_5a", "T22_5b", "T22_5c", "T22_5d"],
        )
        # A hand-edited status cannot survive check(): the fresh build
        # derives status from evidence, never from the committed file.
        committed = json.loads(
            builder.OUTPUT.read_text(encoding="utf-8")
        )
        self.assertEqual(committed["status"], "T22_5_READY")

    def test_gates_have_concrete_evidence_values(self) -> None:
        document = builder.build()
        evidence = document["evidence"]
        self.assertTrue(evidence["a0_ledger_terminology"]["marked"])
        self.assertEqual(evidence["a1_shape_analysis"]["maps"], 9)
        self.assertEqual(evidence["a1_shape_analysis"]["unassigned"], 0)
        self.assertEqual(evidence["a1_shape_analysis"]["duplicate"], 0)
        self.assertEqual(evidence["a2_fluid_mapping"]["unclassified"], 0)
        self.assertEqual(evidence["a2_fluid_mapping"]["total"], 322)
        self.assertEqual(
            evidence["a3_item_classification"]["total_occurrences"], 115481
        )
        self.assertEqual(evidence["b1_row_classification"]["total"], 146841)
        self.assertEqual(evidence["b2_fluid_gap_disposition"]["register"], 0)
        self.assertEqual(evidence["c0_machine_playability"]["registered_maps"], 32)
        self.assertTrue(evidence["c0_machine_playability"]["blockers_all_owned"])
        self.assertEqual(evidence["c1_denominator_recompute"]["column_sum"], 146841)
        self.assertEqual(evidence["c1_denominator_recompute"]["v1"], 0)
        self.assertTrue(evidence["c2_beta_wording"]["wording_updated"])

    def test_closure_policy_pending_is_substantive_only(self) -> None:
        document = builder.build()
        closure = document["closure_policy"]
        self.assertEqual(closure["pending"], [])
        self.assertTrue(closure["final_closure_attempted"])
        self.assertIn("full snapshot report", closure["reason"])

    def test_required_keys_precheck_reports_human_readable_error(self) -> None:
        errors = builder.check()
        self.assertEqual(errors, [])
        # Simulate a truncated artifact: the pre-check must report the
        # missing evidence key instead of raising.
        on_disk = {"schema_version": 1, "status_owner": "run_full_verification",
                   "evidence": {}, "closure_policy": {}, "currentness": {}}
        errors = []
        builder._check_required_keys(on_disk, builder.build(), errors)
        self.assertTrue(any("evidence is missing keys" in e for e in errors))

    def test_material_tree_digest_is_stable(self) -> None:
        self.assertEqual(
            builder.material_tree_digest(), builder.material_tree_digest()
        )


if __name__ == "__main__":
    unittest.main()
