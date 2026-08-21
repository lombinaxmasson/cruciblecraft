from __future__ import annotations

import json
import unittest
from unittest import mock

from tools import build_t30_readiness as builder
from tools import t27_common as common


class T30ReadinessTest(unittest.TestCase):
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
        self.assertTrue(closure["source_rows_60"])
        self.assertTrue(closure["hopper_identities_120"])
        self.assertTrue(closure["dust_funnel_1"])
        self.assertTrue(closure["hopper_host_present"])
        self.assertTrue(closure["vanilla_recipes_121"])
        self.assertTrue(closure["no_bare_hopper_ids"])
        self.assertTrue(closure["lifecycle_gametests_present"])
        self.assertTrue(closure["replacement_satisfied"])
        fidelity = document["fidelity"]
        self.assertTrue(fidelity["source_revision_pinned"])
        self.assertTrue(fidelity["slots_source_backed"])
        self.assertTrue(fidelity["recipes_source_derived"])
        self.assertTrue(fidelity["dust_three_form_bounded"])
        self.assertTrue(fidelity["art_derived"])
        self.assertTrue(fidelity["no_plate_curved_runtime"])
        self.assertTrue(fidelity["no_block_dust"])
        self.assertTrue(fidelity["rc_number_null"])
        load_data = document["load"]
        self.assertTrue(load_data["eager_hard_ceiling_is_21000"])
        self.assertTrue(load_data["datapack_hard_ceiling_is_6600"])
        self.assertTrue(load_data["gt_delta_zero"])
        self.assertTrue(load_data["measured_zero"])
        self.assertTrue(load_data["vanilla_hopper_accounted"])
        self.assertTrue(load_data["pending_axes_not_zeroed"])
        self.assertEqual(6, document["t30_gametests_found"])
        self.assertEqual(121, document["vanilla_recipe_count"])
        self.assertEqual("run_full_verification", document["status_owner"])

    def test_status_is_ready_only_when_report_counts_match(self) -> None:
        document = builder.build()
        runtime = document["runtime"]
        if document.get("status") == "T30_READY":
            self.assertTrue(runtime["gametest_passing"])
            self.assertEqual(
                builder._source_game_test_count(),
                runtime["gametest_total"],
            )
            self.assertTrue(runtime["java_unit_tests_passing"])
            self.assertEqual("READY", runtime["report_status"])
            self.assertEqual("T29_READY", runtime["t29_status"])
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
