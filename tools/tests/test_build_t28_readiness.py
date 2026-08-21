from __future__ import annotations

import json
import unittest
from unittest import mock

from tools import build_t28_readiness as builder
from tools import t27_common as common


class T28ReadinessTest(unittest.TestCase):
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
        self.assertTrue(closure["cooling_rule_absent"])
        self.assertTrue(closure["cooling_class_absent"])
        self.assertTrue(closure["maintain_does_not_query_cooling"])
        self.assertTrue(closure["smelter_hot_ingot_rule_present"])
        self.assertTrue(closure["o36_replacement_is_absence"])
        self.assertTrue(closure["t29_not_started"])
        fidelity = document["fidelity"]
        self.assertTrue(fidelity["source_revision_pinned"])
        self.assertTrue(fidelity["gt6_passive_conversion_false"])
        self.assertTrue(fidelity["cc_auto_conversion_retired"])
        self.assertTrue(fidelity["contact_heat_source_present"])
        self.assertTrue(fidelity["freezer_not_in_scope"])
        self.assertTrue(fidelity["large_crucible_unregistered"])
        self.assertEqual("strict_no_conversion", fidelity["product_decision"])
        load_data = document["load"]
        self.assertTrue(load_data["eager_hard_ceiling_is_21000"])
        self.assertTrue(load_data["eager_projected_below_ceiling"])
        self.assertLess(load_data["projected_logical_delta"], 0)
        self.assertLess(load_data["projected_eager_delta"], 0)
        self.assertTrue(load_data["measured_pending_not_zero"])
        self.assertEqual("run_full_verification", document["status_owner"])

    def test_status_is_ready_only_when_report_cooling_is_gone(self) -> None:
        document = builder.build()
        runtime = document["runtime"]
        self.assertNotIn("java_unit_tests", runtime)
        self.assertNotIn("elapsed_seconds", runtime)
        if document.get("status") == "T28_READY":
            self.assertEqual(0, runtime["live_cooling_expansion"])
            self.assertTrue(runtime["gametest_passing"])
            self.assertEqual("READY", runtime["report_status"])
            self.assertEqual("T27_READY", runtime["t27_status"])
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

    def test_status_owner_forbids_hand_written_ready(self) -> None:
        document = builder.build()
        self.assertEqual("run_full_verification", document["status_owner"])
        self.assertIn("status", builder.REPORT_OWNED)
        self.assertIn("currentness", builder.REPORT_OWNED)
        self.assertIn("runtime", builder.REPORT_OWNED)


if __name__ == "__main__":
    unittest.main()
