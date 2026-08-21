from __future__ import annotations

import copy
import json
import unittest
from unittest import mock

from tools import build_t26_readiness as builder


class T26ReadinessTest(unittest.TestCase):
    def test_committed_artifact_is_current_and_check_is_read_only(self) -> None:
        on_disk = json.loads(builder.OUTPUT.read_text(encoding="utf-8"))
        expected = builder.build()
        stripped = lambda document: {
            key: value
            for key, value in document.items()
            if key not in builder.REPORT_OWNED
        }
        self.assertEqual(stripped(expected), stripped(on_disk))
        self.assertEqual([], builder.check())

    def test_three_axis_derivation_is_complete(self) -> None:
        document = builder.build()
        closure = document["closure"]
        self.assertEqual("T26_KNOWN_ISSUES_COMPLETE", closure["known_issues_status"])
        self.assertEqual(15, closure["known_issues_total"])
        self.assertTrue(closure["known_issues_complete"])
        self.assertEqual(0, closure["blocks_beta"])
        self.assertEqual(10, closure["playtest_rows"])
        self.assertEqual(5, closure["inherited_t25_rows"])
        self.assertTrue(closure["o15_closed"])
        self.assertTrue(closure["localization_accounted"])
        self.assertTrue(closure["anvil_bend_post_1_0"])
        self.assertTrue(closure["crucible_owner_t27"])
        self.assertTrue(closure["future_version_gametest_present"])
        self.assertTrue(closure["packaging_version_current"])
        fidelity = document["fidelity"]
        self.assertEqual(0, fidelity["new_registrations"])
        self.assertTrue(fidelity["no_placeholder_on_mainline"])
        load_data = document["load"]
        self.assertEqual(
            {"logical": 0, "eager": 0, "lazy": 0},
            load_data["publication_delta"],
        )
        self.assertTrue(load_data["publication_baseline_consistent"])
        self.assertEqual(2118, load_data["headroom_remaining"])
        self.assertEqual("logical_legacy_t25", load_data["headroom_axis"])
        self.assertEqual(
            "SKIPPED_TRANSFERRED_TO_T27_RC",
            load_data["remeasurement"],
        )
        self.assertTrue(load_data["skipped_contracts_complete"])
        self.assertEqual(0, load_data["findings_blocking"])
        self.assertEqual(2, len(load_data["t27_recheck_contracts"]))

    def test_status_is_ready_only_when_runtime_evidence_passes(self) -> None:
        document = builder.build()
        if document.get("status") == "T26_READY":
            self.assertTrue(document["runtime"]["gametest_passing"])
            self.assertIn(document["runtime"]["gametest_total"], (121, 131, 137))
            self.assertEqual(list(builder.STAGES), document["completed_stages"])
        else:
            self.assertNotIn("status", document)

    def test_mutation_blocking_issue_fails_closed(self) -> None:
        real = builder._load_if_exists

        def override(path, default=None):
            if path == builder.KNOWN_ISSUES:
                document = copy.deepcopy(real(path, {}) or {})
                document["issues"][0]["blocks_beta"] = True
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
                )["required_tests"] = 119
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
                    "phase": "T26-fake",
                    "logical": 1,
                    "eager": 1,
                    "lazy": 0,
                })
                return document
            return real(path, default)

        with mock.patch.object(
                builder, "_load_if_exists", side_effect=override):
            self.assertNotIn("status", builder.build())


if __name__ == "__main__":
    unittest.main()
