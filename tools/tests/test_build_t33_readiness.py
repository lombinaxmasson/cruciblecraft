from __future__ import annotations

import json
import unittest
from unittest import mock

from tools import build_t33_readiness as builder
from tools import t27_common as common


class T33ReadinessTest(unittest.TestCase):
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

    def test_static_closure_fidelity_and_load_gates(self) -> None:
        document = builder.build()
        closure = document["closure"]
        self.assertTrue(closure["audit_mode_check_only"])
        self.assertTrue(closure["catalog_generated_files_263"])
        self.assertTrue(closure["all_worldgen_files_274"])
        self.assertTrue(closure["closure_vein_classifications_129"])
        self.assertTrue(closure["seed_surface_rock_nonzero"])
        self.assertTrue(closure["s0_seeds_match_dynamic_tag_members"])
        self.assertTrue(closure["rock_pack_not_unreachable"])
        self.assertTrue(closure["cc_4_5_p4_closed_open_item"])
        self.assertTrue(closure["cc_4_5_p4_owner_t33"])
        self.assertTrue(closure["gametest_contract_present"])
        self.assertIn(
            common.relative(builder.SURFACE_FEATURE),
            document["owned_inputs"],
        )
        fidelity = document["fidelity"]
        self.assertTrue(fidelity["design_policy_surface_scatter"])
        self.assertTrue(fidelity["source_derived_and_design_policy_semantics"])
        load_data = document["load"]
        self.assertTrue(load_data["no_publication_delta_claim"])
        self.assertTrue(load_data["no_t20_generated_output_delta"])
        self.assertEqual("static_gates_only", document["status_owner"])
        self.assertTrue(document["runtime"]["manual_gametest_required"])

    def test_status_is_ready_only_when_all_static_gates_pass(self) -> None:
        document = builder.build()
        if document.get("status") == "T33_READY":
            self.assertTrue(all(document["closure"].values()))
            self.assertTrue(all(document["fidelity"].values()))
            self.assertTrue(all(document["load"].values()))
            self.assertEqual(688, document["surface_scatter_summary"]["seed_surface_rock_count"])
        else:
            self.assertNotIn("status", document)

    def test_mutating_evidence_hash_fails_closed(self) -> None:
        real = common.sha256_file

        def override(path):
            if path == builder.REACHABILITY:
                return "0" * 64
            return real(path)

        with mock.patch.object(common, "sha256_file", side_effect=override):
            errors = builder.check()
        self.assertTrue(any("stale" in error for error in errors))


if __name__ == "__main__":
    unittest.main()
