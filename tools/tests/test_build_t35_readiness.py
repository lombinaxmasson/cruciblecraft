from __future__ import annotations

import json
import unittest
from pathlib import Path
from unittest import mock

from tools import build_t35_readiness as builder
from tools import t27_common as common
from tools import t35_common as t35


class T35ReadinessTest(unittest.TestCase):
    def test_committed_artifact_is_current_and_check_is_read_only(self) -> None:
        if not builder.OUTPUT.is_file():
            self.skipTest("artifact not yet generated")
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
        self.assertTrue(closure["t32_vd_001_closed"])
        self.assertTrue(closure["census_validators_zero"])
        self.assertTrue(closure["census_unscoped_zero"])
        self.assertTrue(closure["census_scope_errors_zero"])
        self.assertTrue(closure["all_identities_have_portfolio_scope"])
        self.assertTrue(closure["exclusion_source_sites_763"])
        self.assertTrue(closure["exclusion_expanded_rows_1701"])
        self.assertTrue(closure["storage_source_sites_28"])
        self.assertTrue(closure["storage_expanded_rows_624"])
        self.assertTrue(closure["recipe_rows_accounted_78682"])
        self.assertTrue(closure["runtime_registry_gate_fixture_hash_locked"])
        self.assertTrue(closure["generated_cards_consecutive_from_t38"])
        self.assertTrue(closure["fixed_nodes_t36_t37"])
        self.assertTrue(closure["t37_pilot_not_in_generated_cards"])
        self.assertTrue(closure["machine_track_current"])
        self.assertTrue(closure["machine_track_validators_zero"])
        self.assertTrue(closure["machine_track_conclusion_a"])
        self.assertTrue(closure["storage_scope_current"])
        self.assertTrue(closure["storage_adjacent_families_scoped"])
        self.assertTrue(closure["topology_epoch_t35r_a"])
        self.assertTrue(closure["topology_global_execution_gate_t37"])
        self.assertTrue(closure["epoch_a_generated_cards_depend_on_t37"])
        self.assertTrue(closure["epoch_b_append_only_contract"])
        fidelity = document["fidelity"]
        self.assertTrue(fidelity["f005_historical_closed_preserved"])
        self.assertTrue(fidelity["f005_canonical_deferred_release_remeasurement"])
        self.assertTrue(fidelity["topology_pipes_item_route_scales_distinct"])
        load_data = document["load"]
        self.assertTrue(load_data["publication_delta_zero"])
        self.assertTrue(load_data["pending_load_not_zero_filled"])
        self.assertEqual("static_gates_only", document["status_owner"])
        runtime = document["runtime"]
        self.assertFalse(runtime["manual_runtime_registry_gate_required"])
        self.assertTrue(runtime["runtime_registry_gate_implemented"])
        self.assertTrue(runtime["runtime_registry_gate_configured"])
        self.assertTrue(runtime["runtime_registry_gate_fixture_hash_locked"])
        self.assertTrue(runtime["manual_gametest_required"])
        self.assertEqual(
            "com.masson.cruciblecraft.census.T35CensusGameTests",
            runtime["runtime_registry_gate_gametest_class"],
        )
        self.assertEqual("cruciblecraft_census", runtime["runtime_registry_gate_namespace"])

    def test_status_is_ready_only_when_all_static_gates_pass(self) -> None:
        document = builder.build()
        if document.get("status") == "T35_CENSUS_READY":
            self.assertTrue(all(document["closure"].values()))
            self.assertTrue(all(document["fidelity"].values()))
            self.assertTrue(all(document["load"].values()))
            self.assertEqual(
                ["T38", "T39", "T40", "T41", "T42", "T43", "T44", "T45", "T46"],
                document["topology_summary"]["generated_card_ids"],
            )
        else:
            self.assertNotIn("status", document)

    def test_mutating_evidence_hash_fails_closed(self) -> None:
        if not builder.OUTPUT.is_file():
            self.skipTest("artifact not yet generated")
        from tools import currentness
        from tools import semantic_projection as projection

        real = projection.sha256_file

        def override(path):
            if Path(path).resolve() == builder.BUILDER:
                return "0" * 64
            return real(path)

        with mock.patch.object(t35, "sha256_file", side_effect=override):
            errors = builder.check()
        self.assertEqual(
            [],
            errors,
            "builder-hash-only drift is sidecar HASH_ONLY, not compact semantic stale",
        )
        with mock.patch.object(projection, "sha256_file", side_effect=override):
            hash_only = currentness.check_sidecar(
                builder.OUTPUT,
                include_hash_only=True,
            )
        self.assertTrue(
            any("HASH_ONLY_DRIFT" in error for error in hash_only),
            hash_only,
        )

    def test_handwritten_status_fails_check(self) -> None:
        if not builder.OUTPUT.is_file():
            self.skipTest("artifact not yet generated")
        from tools.tests.support import authority_sandbox

        on_disk = json.loads(builder.OUTPUT.read_text(encoding="utf-8"))
        on_disk["status"] = "HAND_WRITTEN_READY"
        with authority_sandbox.patch_builder_path(builder, "OUTPUT") as output:
            output.write_text(
                common.stable_json(on_disk) + "\n",
                encoding="utf-8",
            )
            errors = builder.check()
            self.assertTrue(any("hand-written" in error for error in errors))

    def test_open_recipe_debt_blocks_build(self) -> None:
        debt = common.load_json(builder.VERIFICATION_DEBT)
        issue = builder._debt_issue(debt, builder.T32_VD_001)
        patched = dict(issue)
        patched["status"] = "open"
        with mock.patch.object(
            builder,
            "_debt_issue",
            return_value=patched,
        ):
            with self.assertRaises(ValueError):
                builder.build()

    def test_f005_pending_semantics_never_pass(self) -> None:
        document = builder.build()
        fidelity = document["fidelity"]
        self.assertTrue(fidelity["f005_historical_closed_preserved"])
        self.assertTrue(fidelity["f005_canonical_deferred_release_remeasurement"])
        self.assertTrue(fidelity["f005_metrics_not_pass"])
        load_baseline = common.load_json(t35.LOAD_BASELINE)
        f005 = load_baseline["axes"]["f005_release_scale"]
        self.assertEqual("CLOSED", f005["historical"]["current_status"])
        self.assertEqual("DEFERRED_RELEASE_REMEASUREMENT", f005["canonical_assessment"])
        self.assertNotEqual("PASS", f005["verdict"])

    def test_publication_delta_zero_across_receipt(self) -> None:
        document = builder.build()
        self.assertEqual({"eager": 0, "lazy": 0, "logical": 0}, document["census_summary"]["publication_delta"])
        self.assertEqual({"eager": 0, "lazy": 0, "logical": 0}, document["topology_summary"]["publication_delta"])
        self.assertTrue(document["closure"]["census_publication_delta_zero"])
        self.assertTrue(document["closure"]["topology_publication_delta_zero"])
        self.assertTrue(document["load"]["publication_delta_zero"])

    def test_pending_zero_blocks_ready_status(self) -> None:
        real_load = common.load_json
        census = real_load(t35.CENSUS)
        patched_census = json.loads(common.stable_json(census))
        first_key = next(iter(patched_census["identities"]))
        patched_census["identities"][first_key]["axes"]["load"] = {
            "status": "pending",
            "verdict": t35.PENDING_LOAD_VERDICT,
            "logical": 0,
        }

        def load_json(path):
            if path == t35.CENSUS:
                return patched_census
            return real_load(path)

        with mock.patch.object(common, "load_json", side_effect=load_json):
            document = builder.build()
        self.assertNotIn("status", document)
        self.assertFalse(document["load"]["pending_load_not_zero_filled"])

    def test_topology_pending_zero_blocks_ready_status(self) -> None:
        real_load = common.load_json
        topology = real_load(t35.CARD_TOPOLOGY)
        patched_topology = json.loads(common.stable_json(topology))
        patched_topology["cards"][0]["axes"]["load"]["logical"] = 0

        def load_json(path):
            if path == t35.CARD_TOPOLOGY:
                return patched_topology
            return real_load(path)

        with mock.patch.object(common, "load_json", side_effect=load_json):
            document = builder.build()
        self.assertNotIn("status", document)
        self.assertFalse(document["load"]["pending_load_not_zero_filled"])

    def test_nonzero_upstream_publication_delta_blocks_ready(self) -> None:
        real_load = common.load_json
        inputs = real_load(t35.INPUTS)
        patched_inputs = json.loads(common.stable_json(inputs))
        patched_inputs["publication_delta"] = {"logical": 1, "eager": 0, "lazy": 0}

        def load_json(path):
            if path == t35.INPUTS:
                return patched_inputs
            return real_load(path)

        with mock.patch.object(common, "load_json", side_effect=load_json):
            with self.assertRaises(ValueError):
                builder.build()


if __name__ == "__main__":
    unittest.main()
