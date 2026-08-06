from __future__ import annotations

import copy
import json
import unittest

from tools import build_t17_readiness as builder
from tools import run_full_verification
from tools import verify_full_verification_report


class T17ReadinessTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.document = builder.build()

    def test_t17a_through_t17d_are_complete_and_ready(
        self,
    ):
        self.assertEqual("T17_READY", self.document["status"])
        self.assertEqual(
            ["T17a", "T17b", "T17c", "T17d"],
            self.document["completed_stages"],
        )
        self.assertEqual([], self.document["pending_stages"])
        self.assertTrue(all(
            self.document["stage_gates"][stage]["status"] == "COMPLETE"
            for stage in ("T17a", "T17b", "T17c", "T17d")
        ))

    def test_t17a_evidence_locks_denominator_catalog_and_zero_gt_rows(self):
        evidence = self.document["t17a_evidence"]
        self.assertEqual(
            ["RM.Distillery", "RM.Drying", "RM.Smelter"],
            evidence["implemented_kinds"],
        )
        self.assertEqual(9, evidence["implemented_variants"])
        self.assertEqual(
            "RM.Electrolyzer", evidence["preimplemented_reference"]
        )
        self.assertEqual(28, evidence["owner_kinds"])
        self.assertEqual(
            {"EU": 16, "HU": 12}, evidence["energy_kind_counts"]
        )
        self.assertEqual(24, evidence["deferred_kinds"])
        self.assertEqual(10, evidence["heat_tier4_deferred"])
        self.assertEqual(32, evidence["electric_tier4_5_deferred"])
        self.assertEqual(33, evidence["catalog_variants"])
        self.assertEqual(6, evidence["new_block_registrations"])
        self.assertEqual(6, evidence["new_item_registrations"])
        self.assertEqual(0, evidence["gt_recipe_row_mutation"])
        self.assertEqual(0, evidence["unclassified"])

    def test_t17b_records_execution_tests_and_source_contracts(self):
        gate = self.document["stage_gates"]["T17b"]
        evidence = self.document["t17b_evidence"]
        self.assertEqual(
            [
                "huExecutionMatrixCoversThreeKindsThreeTiersAndCheapParallelDuration",
                "smelterParallelThousandBoundaryRejectsOverflowAndCompactsOutputs",
                "electrolyzerReferenceCoversThreeBufferedElectricTierEndpoints",
                "t17FailureStatesRemainObservableInTheSharedRuntime",
            ],
            gate["junit_tests"],
        )
        self.assertEqual(gate["junit_tests"], evidence["junit_tests"])
        self.assertEqual(gate["game_tests"], evidence["game_tests"])
        self.assertEqual(
            set(gate["source_contracts"]),
            set(evidence["source_contracts"]),
        )
        for owner in evidence["source_contracts"]:
            self.assertEqual(
                "T17b", self.document["source_contracts"][owner]["stage"]
            )

    def test_t17b_evidence_keeps_electrolyzer_as_reference_without_mixer_tiers(
        self,
    ):
        evidence = self.document["t17b_evidence"]
        self.assertEqual(3, len(evidence["hu_execution_kinds"]))
        self.assertEqual(9, evidence["hu_execution_variants"])
        self.assertEqual(
            "HEAT_ADJACENT_BOTTOM_FIREBOX",
            evidence["hu_energy_contract"],
        )
        self.assertEqual(
            "cruciblecraft:electrolyzer", evidence["eu_reference"]
        )
        self.assertEqual(3, len(evidence["eu_reference_variants"]))
        self.assertEqual([1, 2, 4], evidence["eu_parallel_limits"])
        self.assertEqual(
            "ELECTRIC_BUFFERED_CABLE_ENDPOINT",
            evidence["eu_energy_contract"],
        )
        self.assertEqual(0, evidence["electric_mixer_tier_variants"])

    def test_t17c_records_closed_acquisition_resources_and_migrations(self):
        gate = self.document["stage_gates"]["T17c"]
        evidence = self.document["t17c_evidence"]
        self.assertEqual(3, evidence["selected_kinds"])
        self.assertEqual(9, evidence["selected_variants"])
        self.assertEqual(9, len(evidence["machine_variant_recipe_ids"]))
        self.assertEqual(2, evidence["casing_dependencies"])
        self.assertEqual(42, evidence["machine_ingredient_rows"])
        self.assertEqual(6, evidence["producer_forms"])
        self.assertEqual(
            3, len(evidence["preimplemented_electrolyzer_variants"])
        )
        self.assertEqual(0, evidence["gt_recipe_rows_added"])
        self.assertEqual(0, evidence["unreachable"])
        self.assertEqual(gate["junit_tests"], evidence["junit_tests"])
        self.assertEqual(gate["game_tests"], evidence["game_tests"])
        self.assertEqual(
            set(gate["source_contracts"]),
            set(evidence["source_contracts"]),
        )
        self.assertEqual(
            "COMPLETE_T17C",
            self.document["closure_summary"][
                "resources_acquisition_full_closure"
            ],
        )

    def test_t17d_closes_zero_load_publication_and_emi(self):
        evidence = self.document["t17d_evidence"]
        self.assertEqual("PASS", evidence["status"])
        self.assertEqual(
            {
                "status": "T17_MACHINE_DENOMINATOR_READY",
                "kinds": 28,
                "energy_kinds": {"EU": 16, "HU": 12},
                "unclassified": 0,
                "selected_kinds": 3,
                "selected_variants": 9,
                "preimplemented_reference_kinds": 1,
                "deferred_kinds": 24,
                "heat_tier4_deferred": 10,
                "electric_tier4_5_deferred": 32,
            },
            evidence["denominator"],
        )
        self.assertEqual(
            {
                "HU": "HEAT_ADJACENT_BOTTOM_FIREBOX",
                "EU": "ELECTRIC_BUFFERED_CABLE_ENDPOINT",
                "hu_execution_variants": 9,
                "eu_reference_variants": 3,
                "electric_mixer_tier_variants": 0,
            },
            evidence["energy_topology"],
        )
        self.assertEqual(3, evidence["migration_acquisition"][
            "exact_legacy_tier1_migrations"
        ])
        self.assertEqual(9, evidence["migration_acquisition"][
            "vanilla_crafting_rows"
        ])
        self.assertEqual(0, evidence["migration_acquisition"][
            "gt_recipe_rows"
        ])
        self.assertEqual(0, evidence["migration_acquisition"]["unreachable"])
        self.assertEqual("T17", evidence["load_projection"]["delivery_phase"])
        self.assertEqual("PASS", evidence["load_projection"]["status"])
        self.assertTrue(all(
            value == 0
            for value in evidence["load_projection"][
                "incremental_counts"
            ].values()
        ))
        self.assertTrue(all(
            interval == {"min": 0, "max": 0}
            for interval in evidence["load_projection"][
                "measurement_intervals"
            ].values()
        ))
        self.assertEqual(
            {
                "logical_rows": 18_875,
                "eager_rows": 16_650,
                "lazy_rows": 2_225,
            },
            evidence["publication_baseline"]["publication_totals"],
        )
        self.assertEqual(
            32, len(evidence["publication_baseline"]["recipe_map_ids"])
        )
        self.assertTrue(evidence["publication_baseline"][
            "stable_id_set_equal_to_t16"
        ])
        self.assertEqual(24, evidence["emi_enumeration"]["configured_maps"])
        self.assertTrue(evidence["emi_enumeration"][
            "recipe_enumeration_equal_to_t16"
        ])
        self.assertEqual(8, evidence["gametest"]["t17_test_count"])
        self.assertEqual("PASS", self.document["publication_gate"]["status"])
        self.assertEqual(
            0, self.document["publication_gate"]["publication_delta"]
        )

    def test_currentness_covers_every_completed_t17_source_contract(self):
        currentness = self.document["currentness"]
        self.assertEqual(
            {
                "tools/build_t17_readiness.py",
                "tools/t17_readiness_policy.json",
            },
            set(currentness["owned_inputs"]),
        )
        current_contracts = currentness["completed_stage_sources"]
        self.assertEqual(
            {"T17a", "T17b", "T17c", "T17d"},
            set(current_contracts),
        )
        for stage in current_contracts:
            self.assertEqual(
                {
                    owner
                    for owner, row in self.document[
                        "source_contracts"
                    ].items()
                    if row["stage"] == stage
                },
                set(current_contracts[stage]),
            )
        self.assertEqual(
            {"T16", "denominator", "acquisition"},
            set(currentness["dependencies"]),
        )

    def test_stage_policy_mutations_fail_closed(self):
        policy = builder.load(builder.POLICY)
        pending = copy.deepcopy(policy)
        pending["stages"]["T17d"]["status"] = "PENDING"
        pending["stages"]["T17d"]["reason"] = "mutation"
        pending["stages"]["T17d"]["replacement_condition"] = "mutation"
        pending["stages"]["T17d"]["recheck_point"] = "mutation"
        with self.assertRaisesRegex(
            ValueError, "T17a-d complete"
        ):
            builder.build(pending)

    def test_python_and_full_verification_currentness_are_wired_for_t17d(self):
        policy = json.loads(
            (builder.TOOLS / "python_test_policy.json").read_text(
                encoding="utf-8"
            )
        )
        t17_rule = next(
            row for row in policy["affected_rules"]
            if "tools/build_t17_*.py" in row["paths"]
        )
        self.assertEqual(
            [
                "test_build_machine_crafting_readiness",
                "test_build_t17_machine_acquisition",
                "test_build_t17_machine_denominator",
                "test_build_t17_readiness",
                "test_full_verification_report",
                "test_recipe_load_projection",
            ],
            t17_rule["test_modules"],
        )
        self.assertIn(
            ("tools/build_t17_machine_denominator.py", "--check"),
            run_full_verification.BUILDER_CHECKS,
        )
        self.assertIn(
            ("tools/build_t17_machine_acquisition.py", "--check"),
            run_full_verification.BUILDER_CHECKS,
        )
        self.assertIn(
            ("tools/build_t17_readiness.py", "--check"),
            run_full_verification.BUILDER_CHECKS,
        )
        for artifact in (
            "t17_machine_denominator.json",
            "t17_machine_acquisition.json",
            "t17_load_projection_input.json",
            "t17_load_projection.json",
            "t17_readiness_policy.json",
            "t17_readiness.json",
        ):
            self.assertIn(
                artifact, verify_full_verification_report.CORE_ARTIFACTS
            )

    def test_artifact_matches_current_builder(self):
        before = builder.OUTPUT.read_bytes()
        self.assertEqual([], builder.check())
        self.assertEqual(before, builder.OUTPUT.read_bytes())
        self.assertEqual(
            self.document,
            json.loads(builder.OUTPUT.read_text(encoding="utf-8")),
        )


if __name__ == "__main__":
    unittest.main()
