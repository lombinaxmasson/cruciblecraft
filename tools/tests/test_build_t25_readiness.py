from __future__ import annotations

import copy
import json
import unittest
from unittest import mock

from tools import build_t25_readiness as builder


class T25ReadinessTest(unittest.TestCase):
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
        self.assertEqual(5, closure["findings_total"])
        self.assertEqual(5, closure["findings_disposed"])
        self.assertTrue(closure["dispositions_are_bijection"])
        self.assertTrue(closure["disposition_vocabulary_ok"])
        self.assertEqual(0, closure["selected"])
        self.assertTrue(closure["zero_content_close"])
        self.assertEqual(
            closure["t24_scenario_gametests_expected"],
            closure["t24_scenario_gametests_present"],
        )
        fidelity = document["fidelity"]
        self.assertEqual(0, fidelity["fixes_applied"])
        self.assertTrue(fidelity["no_approximation_introduced"])
        self.assertTrue(fidelity["source_backed_facts_untouched"])
        load_data = document["load"]
        self.assertEqual(
            {"logical": 0, "eager": 0, "lazy": 0},
            load_data["publication_delta"],
        )
        self.assertTrue(load_data["publication_baseline_consistent"])
        self.assertEqual(0, load_data["before_after_pairs"])
        self.assertEqual(
            "NOT_APPLICABLE_NO_SELECTED_FIXES",
            load_data["remeasurement"],
        )
        self.assertTrue(load_data["skipped_contracts_complete"])
        self.assertEqual(0, load_data["findings_blocking"])
        self.assertEqual(2, len(load_data["t26_recheck_contracts"]))

    def test_status_is_ready_only_when_runtime_evidence_passes(self) -> None:
        document = builder.build()
        if document["runtime"]["gametest_passing"] is True:
            self.assertEqual("T25_READY", document["status"])
            self.assertEqual(
                ["T25a", "T25b"], document["completed_stages"]
            )
        else:
            self.assertNotIn("status", document)

    def test_mutation_selected_finding_fails_closed(self) -> None:
        real = builder._load_if_exists

        def override(path, default=None):
            if path == builder.DISPOSITIONS:
                document = copy.deepcopy(real(path, {}) or {})
                for row in document.get("dispositions", []):
                    if row.get("id") == "T24-F001":
                        row["disposition"] = "selected_fixed"
                return document
            return real(path, default)

        with mock.patch.object(
                builder, "_load_if_exists", side_effect=override):
            self.assertNotIn("status", builder.build())

    def test_mutation_gametest_total_drift_fails_closed(self) -> None:
        real = builder._load_if_exists

        def override(path, default=None):
            if path == builder.REPORT:
                document = copy.deepcopy(real(path, {}) or {})
                document.setdefault("tests", {}).setdefault(
                    "production_game_tests", {}
                )["required_tests"] = 120
                return document
            return real(path, default)

        with mock.patch.object(
                builder, "_load_if_exists", side_effect=override):
            self.assertNotIn("status", builder.build())

    def test_mutation_nonzero_publication_delta_fails_closed(self) -> None:
        real = builder._load_if_exists

        def override(path, default=None):
            if path == builder.PUBLICATION_BASELINE:
                document = copy.deepcopy(real(path, {}) or {})
                ledger = document.setdefault("delta_ledger_policy", {})
                ledger.setdefault("registered_deltas", []).append({
                    "phase": "T25-fake",
                    "logical": 1,
                    "eager": 1,
                    "lazy": 0,
                })
                return document
            return real(path, default)

        with mock.patch.object(
                builder, "_load_if_exists", side_effect=override):
            self.assertNotIn("status", builder.build())

    def test_mutation_missing_disposition_fails_closed(self) -> None:
        real = builder._load_if_exists

        def override(path, default=None):
            if path == builder.DISPOSITIONS:
                document = copy.deepcopy(real(path, {}) or {})
                document["dispositions"] = document.get(
                    "dispositions", [])[:-1]
                return document
            return real(path, default)

        with mock.patch.object(
                builder, "_load_if_exists", side_effect=override):
            self.assertNotIn("status", builder.build())


if __name__ == "__main__":
    unittest.main()
