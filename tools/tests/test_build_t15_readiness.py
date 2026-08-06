from __future__ import annotations

import copy
import hashlib
import json
import unittest
from pathlib import Path

from tools import build_t15_readiness as builder
from tools import run_full_verification
from tools import verify_full_verification_report


def digest(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


class T15ReadinessTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.document = builder.build()

    def test_t15a_e_are_complete_and_t15_is_ready(self):
        self.assertEqual("T15_READY", self.document["status"])
        self.assertEqual(
            ["T15a", "T15b", "T15c", "T15d", "T15e"],
            self.document["completed_stages"],
        )
        self.assertEqual([], self.document["pending_stages"])
        gates = self.document["stage_gates"]
        self.assertTrue(all(
            gate["status"] == "COMPLETE" for gate in gates.values()
        ))

    def test_t12a_is_immutable_history_not_a_current_dependency(self):
        history = self.document["historical_immutable_evidence"]
        self.assertEqual(
            "tools/t12a_machine_readiness.json", history["path"]
        )
        self.assertEqual("T12A_READY", history["status"])
        self.assertEqual(
            "T12A_PREPROJECTION_READY", history["load_status"]
        )
        self.assertTrue(history["immutable"])
        self.assertFalse(history["current"])
        current_paths = set(
            self.document["currentness"]["current_dependencies"]
        )
        self.assertNotIn(history["path"], current_paths)
        self.assertEqual(
            {
                "tools/t12_closure_readiness.json",
                "tools/t13_denominator_readiness.json",
                "tools/t14_readiness.json",
            },
            current_paths,
        )

    def test_supersession_names_t15_as_the_only_current_owner(self):
        supersession = self.document["supersession"]
        self.assertEqual("T15", supersession["current_owner"])
        self.assertEqual(
            "tools/t15_readiness.json", supersession["current_artifact"]
        )
        self.assertEqual(
            "tools/t12a_machine_readiness.json",
            supersession["historical_artifact"],
        )
        self.assertTrue(supersession["history_preserved"])
        self.assertFalse(supersession["historical_currentness_claimed"])

    def test_source_contracts_cover_the_readiness_split(self):
        contracts = self.document["source_contracts"]
        self.assertEqual(
            {
                "coke_oven_resource",
                "documentation",
                "full_verification_reporter",
                "full_verification_runner",
                "large_centrifuge_controller",
                "machine_identity_migration",
                "machine_tier_catalog_loader",
                "machine_tier_catalog_resource",
                "processing_identity_integration",
                "python_test_policy",
                "t12_closure_builder",
                "t14_currentness",
                "t15b_java_test",
                "t15c_acquisition_builder",
                "t15c_acquisition_policy",
                "t15c_java_resource_test",
                "t15c_live_recipe_gametest",
                "t15c_load_currentness_test",
                "t15c_load_projection_input",
                "t15c_python_test",
                "t15d_full_verification_reporter",
                "t15d_full_verification_test",
                "t15d_identity_gametest",
                "t15d_persistence_junit",
                "t15d_policy_junit",
                "t15d_processing_host",
                "t15d_readiness_test",
                "t15e_boundary_builder",
                "t15e_python_test",
                "t15e_structure_junit",
                "t15e_profile_junit",
                "t15e_shared_host_gametest",
                "t15e_t12_policy_audit",
                "t15e_t12_closure_audit",
                "t15e_full_verification_runner",
                "t15e_full_verification_reporter",
                "t15e_full_verification_test",
                "t15e_documentation",
            },
            set(contracts),
        )
        self.assertTrue(
            all(row["sha256"] for row in contracts.values())
        )
        test_policy = json.loads(
            (builder.TOOLS / "python_test_policy.json").read_text(
                encoding="utf-8"
            )
        )
        t15_rule = next(
            rule
            for rule in test_policy["affected_rules"]
            if "tools/build_t15_*.py" in rule["paths"]
        )
        self.assertIn("tools/t12a_*.json", {
            path
            for rule in test_policy["affected_rules"]
            for path in rule["paths"]
        })
        self.assertEqual(
            [
                "test_build_t15_machine_acquisition",
                "test_build_t15_matcher_boundary",
                "test_build_t15_readiness",
                "test_full_verification_report",
                "test_recipe_load_projection",
            ],
            t15_rule["test_modules"],
        )
        stage_sources = self.document["currentness"][
            "completed_stage_sources"
        ]
        self.assertEqual(
            {"T15a", "T15b", "T15c", "T15d", "T15e"},
            set(stage_sources),
        )
        self.assertEqual(
            {
                "large_centrifuge_controller",
                "machine_identity_migration",
                "machine_tier_catalog_loader",
                "machine_tier_catalog_resource",
                "processing_identity_integration",
                "t15b_java_test",
            },
            set(stage_sources["T15b"]),
        )
        self.assertEqual(
            {
                "t15c_acquisition_builder",
                "t15c_acquisition_policy",
                "t15c_java_resource_test",
                "t15c_live_recipe_gametest",
                "t15c_load_currentness_test",
                "t15c_load_projection_input",
                "t15c_python_test",
            },
            set(stage_sources["T15c"]),
        )
        self.assertEqual(
            {
                "t15d_full_verification_reporter",
                "t15d_full_verification_test",
                "t15d_identity_gametest",
                "t15d_persistence_junit",
                "t15d_policy_junit",
                "t15d_processing_host",
                "t15d_readiness_test",
            },
            set(stage_sources["T15d"]),
        )
        self.assertEqual(
            {
                "t15e_boundary_builder",
                "t15e_documentation",
                "t15e_full_verification_reporter",
                "t15e_full_verification_runner",
                "t15e_full_verification_test",
                "t15e_profile_junit",
                "t15e_python_test",
                "t15e_shared_host_gametest",
                "t15e_structure_junit",
                "t15e_t12_closure_audit",
                "t15e_t12_policy_audit",
            },
            set(stage_sources["T15e"]),
        )

    def test_t15c_gate_records_closed_acquisition_and_zero_load(self):
        gate = self.document["t15c_acquisition_gate"]
        self.assertEqual("PASS", gate["status"])
        self.assertEqual(
            {
                "casing_recipes": 6,
                "machine_variant_recipes": 9,
                "registered_results": 15,
                "unreachable": 0,
            },
            gate["acquisition"]["counts"],
        )
        load = gate["load_projection"]
        self.assertEqual("PASS", load["status"])
        self.assertEqual("T15", load["delivery_phase"])
        self.assertEqual(0, load["publication_delta"])
        self.assertTrue(
            all(value == 0 for value in load["incremental_counts"].values())
        )
        self.assertTrue(all(
            interval == {"min": 0, "max": 0}
            for interval in load["measurement_intervals"].values()
        ))

    def test_t15d_gate_records_identity_regression_matrix(self):
        gate = self.document["t15d_identity_gate"]
        self.assertEqual("PASS", gate["status"])
        self.assertEqual(gate["junit"]["test_count"], 4)
        self.assertEqual(
            {
                "kindTierMaterialAndEnergyMismatchesAllQuarantine",
                "onlyExactLegacyLargeCentrifugeTupleMigrates",
                "quarantineReloadStopsProcessingAndPreservesRecoverableState",
                "exactMigrationReloadResumesAndWritesCurrentIdentityOnNextSave",
            },
            {
                name
                for row in gate["junit"]["classes"]
                for name in row["tests"]
            },
        )
        self.assertEqual(gate["gametest"]["added_test_count"], 2)
        self.assertEqual(gate["gametest"]["full_suite_test_count"], 56)
        self.assertEqual(
            {
                "processingIdentityMismatchesQuarantineAcrossNbtReload",
                "exactLargeCentrifugeIdentityMigratesAcrossNbtReload",
            },
            set(gate["gametest"]["tests"]),
        )
        self.assertEqual(
            set(gate["source_contract_sha256"]),
            set(self.document["currentness"]["completed_stage_sources"]["T15d"]),
        )

    def test_t15e_gate_records_source_derived_port_matcher_boundary(self):
        gate = self.document["t15e_matcher_boundary_gate"]
        self.assertEqual("PASS", gate["status"])
        self.assertEqual(
            "T15E_MATCHER_BOUNDARY_READY",
            gate["artifact"]["status"],
        )
        self.assertEqual(
            {
                "scan_volume": 18,
                "item_fluid_ports": 15,
                "energy_input_ports": 2,
                "controllers": 1,
            },
            {
                key: gate["physical_structure"][key]
                for key in (
                    "scan_volume",
                    "item_fluid_ports",
                    "energy_input_ports",
                    "controllers",
                )
            },
        )
        self.assertEqual(1, gate["host_layout"]["item_inputs"])
        self.assertEqual(6, gate["host_layout"]["item_outputs"])
        self.assertEqual(1, gate["host_layout"]["fluid_inputs"])
        self.assertEqual(2, gate["host_layout"]["fluid_outputs"])
        self.assertEqual(
            1, gate["port_host_boundary"]["item_matcher_supplies"]
        )
        self.assertEqual(
            1, gate["port_host_boundary"]["fluid_matcher_supplies"]
        )
        self.assertFalse(
            gate["port_host_boundary"][
                "physical_ports_expand_matcher_supplies"
            ]
        )
        self.assertEqual(
            12, gate["matcher_boundary"]["presence_item_supply_cap"]
        )
        self.assertFalse(
            gate["matcher_boundary"]["presence_cap_triggered"]
        )
        self.assertEqual(
            [12, 16, 32, 64],
            gate["benchmark"]["dense_supply_counts"],
        )
        self.assertEqual(
            [16, 32, 64],
            gate["benchmark"]["presence_cap_rejection_supply_counts"],
        )

    def test_stage_policy_extends_to_t15_ready_without_schema_changes(self):
        policy = json.loads(builder.POLICY.read_text(encoding="utf-8"))
        for stage_id in ("T15b", "T15c", "T15d", "T15e"):
            policy["stages"][stage_id]["status"] = "COMPLETE"
        status, completed, pending = builder.stage_summary(policy)
        self.assertEqual("T15_READY", status)
        self.assertEqual(list(builder.STAGE_ORDER), completed)
        self.assertEqual([], pending)

    def test_nonsequential_stage_completion_fails_closed(self):
        policy = json.loads(builder.POLICY.read_text(encoding="utf-8"))
        policy = copy.deepcopy(policy)
        policy["stages"]["T15d"] = {
            "name": "identity_quarantine",
            "status": "PENDING",
            "replacement_condition": "test gap",
        }
        policy["stages"]["T15e"]["status"] = "COMPLETE"
        with self.assertRaises(ValueError):
            builder.stage_summary(policy)

    def test_check_mode_is_immutable(self):
        before = digest(builder.OUTPUT)
        self.assertEqual([], builder.check())
        self.assertEqual(before, digest(builder.OUTPUT))
        self.assertEqual(
            self.document,
            json.loads(builder.OUTPUT.read_text(encoding="utf-8")),
        )

    def test_full_verification_checks_history_and_current_t15(self):
        self.assertIn(
            (
                "tools/build_t12_machine_readiness.py",
                "--check",
                "--reference-only",
            ),
            run_full_verification.BUILDER_CHECKS,
        )
        self.assertIn(
            ("tools/build_t15_machine_acquisition.py", "--check"),
            run_full_verification.BUILDER_CHECKS,
        )
        self.assertIn(
            ("tools/build_t15_matcher_boundary.py", "--check"),
            run_full_verification.BUILDER_CHECKS,
        )
        self.assertIn(
            ("tools/build_t15_readiness.py", "--check"),
            run_full_verification.BUILDER_CHECKS,
        )
        self.assertIn(
            "t12a_machine_readiness.json",
            verify_full_verification_report.CORE_ARTIFACTS,
        )
        self.assertIn(
            "t15_machine_acquisition.json",
            verify_full_verification_report.CORE_ARTIFACTS,
        )
        self.assertIn(
            "t15_load_projection.json",
            verify_full_verification_report.CORE_ARTIFACTS,
        )
        self.assertIn(
            "t15_matcher_boundary.json",
            verify_full_verification_report.CORE_ARTIFACTS,
        )
        self.assertIn(
            "t15_readiness.json",
            verify_full_verification_report.CORE_ARTIFACTS,
        )
        self.assertNotIn(
            "t12_machine_readiness.json",
            verify_full_verification_report.CORE_ARTIFACTS,
        )


if __name__ == "__main__":
    unittest.main()
