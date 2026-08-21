from __future__ import annotations

import json
import unittest
from unittest import mock

from tools import build_t29_readiness as builder
from tools import t27_common as common


class T29ReadinessTest(unittest.TestCase):
    def test_committed_artifact_is_current_and_check_is_read_only(self) -> None:
        on_disk = json.loads(builder.OUTPUT.read_text(encoding="utf-8"))
        expected = builder.build()
        stripped = lambda document: {
            key: value
            for key, value in document.items()
            if key not in builder.REPORT_OWNED
        }
        self.assertEqual(stripped(expected), stripped(on_disk))
        before = builder.OUTPUT.read_bytes()
        self.assertEqual([], builder.check())
        self.assertEqual(before, builder.OUTPUT.read_bytes())

    def test_three_axis_replacement_evidence_is_complete(self) -> None:
        document = builder.build()
        closure = document["closure"]
        self.assertTrue(closure["large_crucible_registered"])
        self.assertTrue(closure["single_block_crucible_retained"])
        self.assertTrue(closure["structure_positions_27"])
        self.assertTrue(closure["capacity_432"])
        self.assertTrue(closure["single_capacity_8"])
        self.assertTrue(closure["lifecycle_gametests_present"])
        self.assertTrue(closure["replacement_satisfied"])
        fidelity = document["fidelity"]
        self.assertTrue(fidelity["source_revision_pinned"])
        self.assertTrue(fidelity["structure_27_source_backed"])
        self.assertTrue(fidelity["capacity_432_source_backed"])
        self.assertTrue(fidelity["t23_positions_27"])
        self.assertTrue(fidelity["plugin_mirrors_agree"])
        self.assertTrue(fidelity["not_processing_host"])
        self.assertTrue(fidelity["heat_scale_design_policy"])
        self.assertTrue(fidelity["steelmaking_source_derived"])
        load_data = document["load"]
        self.assertTrue(load_data["gt_delta_zero"])
        self.assertTrue(load_data["measured_zero"])
        self.assertTrue(load_data["vanilla_controller_accounted"])
        self.assertEqual(10, document["t29_gametests_found"])
        self.assertEqual("run_full_verification", document["status_owner"])

    def test_status_is_ready_only_when_report_counts_match(self) -> None:
        document = builder.build()
        runtime = document["runtime"]
        if document.get("status") == "T29_READY":
            self.assertTrue(runtime["gametest_passing"])
            self.assertGreaterEqual(runtime["gametest_total"], 131)
            self.assertTrue(runtime["java_unit_tests_passing"])
            self.assertEqual("READY", runtime["report_status"])
            self.assertEqual("T28_READY", runtime["t28_status"])
            self.assertIn(runtime["rc_number"], (None, ""))
        else:
            self.assertNotIn("status", document)

    def test_mutating_evidence_hash_fails_closed(self) -> None:
        real = common.sha256_file

        def override(path):
            if path == builder.EVIDENCE:
                return "0" * 64
            return real(path)

        with mock.patch.object(common, "sha256_file", side_effect=override):
            errors = builder.check()
        self.assertTrue(any("stale" in error for error in errors))


if __name__ == "__main__":
    unittest.main()
