from __future__ import annotations

import json
import unittest
from unittest import mock

from tools import build_t24_readiness as builder


class T24ReadinessTest(unittest.TestCase):
    def test_committed_artifact_is_current_and_check_is_read_only(self) -> None:
        on_disk = json.loads(
            builder.OUTPUT.read_text(encoding="utf-8")
        )
        expected = builder.build()
        stripped = lambda d: {
            k: v for k, v in d.items() if k not in builder.REPORT_OWNED
        }
        self.assertEqual(stripped(expected), stripped(on_disk))
        self.assertEqual([], builder.check())

    def test_three_axis_derivation_is_complete(self) -> None:
        document = builder.build()
        closure = document["closure"]
        self.assertEqual(3, closure["scenarios_defined"])
        self.assertTrue(closure["scenarios_rebuildable_from_empty"])
        self.assertTrue(closure["workload_identity"])
        self.assertEqual(
            closure["gametest_scenarios_expected"],
            closure["gametest_scenarios_executed"],
        )
        self.assertTrue(closure["mutation_gate_present"])
        fidelity = document["fidelity"]
        self.assertTrue(fidelity["design_policy_declared"])
        self.assertFalse(fidelity["synthetic_marked_as_measured"])
        self.assertEqual(["stress", "target"], sorted(fidelity["skipped_metrics"]))
        load_data = document["load"]
        self.assertEqual(
            {"logical": 0, "eager": 0, "lazy": 0},
            load_data["publication_delta"],
        )
        self.assertTrue(load_data["publication_baseline_consistent"])
        self.assertEqual("MEASURED", load_data["bounded_counts_status"])
        self.assertEqual(0, load_data["findings_blocking"])
        self.assertEqual(
            "FINDINGS_LEDGER_READY", load_data["findings_status"]
        )

    def test_status_is_ready_only_when_runtime_evidence_passes(self) -> None:
        document = builder.build()
        if document["runtime"]["gametest_passing"] is True:
            self.assertEqual("T24_READY", document["status"])
            self.assertEqual(
                ["T24a", "T24b", "T24c"], document["completed_stages"]
            )
        else:
            self.assertNotIn("status", document)

    def test_mutation_blocking_finding_fails_closed(self) -> None:
        real = builder._load_if_exists

        def override(path, default=None):
            if path == builder.FINDINGS:
                document = real(path, {}) or {}
                counts = dict(document.get("counts") or {})
                counts["blocking"] = 1
                document["counts"] = counts
                return document
            return real(path, default)

        with mock.patch.object(
                builder, "_load_if_exists", side_effect=override):
            self.assertNotIn("status", builder.build())

    def test_mutation_synthetic_marked_as_measured_fails_closed(self) -> None:
        real = builder._load_if_exists

        def override(path, default=None):
            if path == builder.SCALE_EVIDENCE:
                document = real(path, {}) or {}
                measured = dict(document.get("measured_at_scale") or {})
                measured["target"] = {
                    "status": "MEASURED_AT_SCALE",
                    "evidence_class": "SYNTHETIC_BENCHMARK",
                }
                document["measured_at_scale"] = measured
                return document
            return real(path, default)

        with mock.patch.object(
                builder, "_load_if_exists", side_effect=override):
            self.assertNotIn("status", builder.build())

    def test_mutation_missing_scenario_fails_closed(self) -> None:
        real = builder._load_if_exists

        def override(path, default=None):
            if path == builder.WORKLOAD_MANIFEST:
                document = real(path, {}) or {}
                scenarios = dict(document.get("scenarios") or {})
                del scenarios["stress"]
                document["scenarios"] = scenarios
                return document
            return real(path, default)

        with mock.patch.object(
                builder, "_load_if_exists", side_effect=override):
            self.assertNotIn("status", builder.build())


if __name__ == "__main__":
    unittest.main()
