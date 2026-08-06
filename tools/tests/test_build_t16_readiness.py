from __future__ import annotations

import copy
import json
import unittest

from tools import build_t16_readiness as builder
from tools import run_full_verification
from tools import verify_full_verification_report


class T16ReadinessTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.document = builder.build()

    def test_t16a_through_t16d_are_complete_and_ready(self):
        self.assertEqual("T16_READY", self.document["status"])
        self.assertEqual(
            ["T16a", "T16b", "T16c", "T16d"],
            self.document["completed_stages"],
        )
        self.assertEqual([], self.document["pending_stages"])
        self.assertTrue(all(
            self.document["stage_gates"][stage]["status"] == "COMPLETE"
            for stage in ("T16a", "T16b", "T16c", "T16d")
        ))

    def test_t16a_evidence_locks_catalog_and_registration_counts(self):
        evidence = self.document["t16a_evidence"]
        self.assertEqual(
            [
                "RM.Lathe",
                "RM.RollingMill",
                "RM.Shredder",
                "RM.Wiremill",
            ],
            evidence["implemented_kinds"],
        )
        self.assertEqual(12, evidence["implemented_variants"])
        self.assertEqual(6, evidence["preserved_pre_t16_variants"])
        self.assertEqual(3, evidence["preserved_non_t16_electric_variants"])
        self.assertEqual(21, evidence["catalog_variants_at_completion"])
        self.assertEqual(8, evidence["new_block_registrations"])
        self.assertEqual(8, evidence["new_item_registrations"])
        self.assertEqual(0, evidence["unclassified"])
        self.assertEqual(1, evidence["controller_profiles"])

    def test_t16b_evidence_locks_press_runtime_scope(self):
        evidence = self.document["t16b_evidence"]
        self.assertEqual("RM.Press", evidence["implemented_kind"])
        self.assertEqual(
            [
                "cruciblecraft:press",
                "cruciblecraft:steel_press",
                "cruciblecraft:titanium_press",
            ],
            evidence["implemented_variants"],
        )
        self.assertEqual(5, evidence["selected_kinds"])
        self.assertEqual(2, evidence["preimplemented_kinds"])
        self.assertEqual(13, evidence["deferred_kinds"])
        self.assertEqual(24, evidence["catalog_variants"])
        self.assertEqual(2, evidence["new_block_registrations"])
        self.assertEqual(2, evidence["new_item_registrations"])
        self.assertEqual("KINETIC_PUSH", evidence["energy_identity"])
        self.assertEqual(
            builder.denominator_builder.SOURCE_REVISION,
            evidence["source_revision"],
        )

    def test_t16d_closes_zero_load_publication_and_emi(self):
        resources = self.document[
            "resource_acquisition_migration_gate"
        ]
        publication = self.document["publication_gate"]
        self.assertEqual("T16C_COMPLETE", resources["status"])
        self.assertTrue(resources["runtime_registrations_are_closure"])
        self.assertEqual(15, resources["selected_variants"])
        self.assertEqual(0, resources["unreachable"])
        self.assertEqual("PASS", publication["status"])
        self.assertTrue(publication["publication_unchanged_claimed"])
        self.assertEqual(0, publication["publication_delta"])
        self.assertTrue(publication["recipe_map_stable_id_set_equal"])
        self.assertTrue(publication["emi_recipe_enumeration_equal"])
        self.assertTrue(publication["logical_eager_lazy_totals_equal"])

        evidence = self.document["t16d_evidence"]
        self.assertEqual("PASS", evidence["status"])
        self.assertEqual(
            {
                "kinds": 20,
                "unclassified": 0,
                "selected_kinds": 5,
                "selected_variants": 15,
                "preimplemented_kinds": 2,
                "deferred_kinds": 13,
                "tier4_deferred": 20,
                "status": "T16_MACHINE_DENOMINATOR_READY",
            },
            evidence["denominator"],
        )
        self.assertEqual(
            {"RU": "KINETIC_ROTATION", "KU": "KINETIC_PUSH"},
            evidence["energy_identity"],
        )
        self.assertEqual(0, evidence["migration_acquisition"][
            "gt_recipe_rows"
        ])
        self.assertEqual(15, evidence["migration_acquisition"][
            "vanilla_crafting_rows"
        ])
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
        self.assertTrue(evidence["publication_baseline"][
            "stable_id_set_equal"
        ])
        self.assertEqual(
            {
                "logical_rows": 18875,
                "eager_rows": 16650,
                "lazy_rows": 2225,
            },
            evidence["publication_baseline"]["publication_totals"],
        )
        self.assertTrue(evidence["emi_enumeration"][
            "recipe_enumeration_equal"
        ])
        self.assertEqual(6, evidence["gametest"]["t16_test_count"])

    def test_t16c_evidence_locks_resources_acquisition_and_migrations(self):
        self.assertEqual({
            "selected_kinds": 5,
            "selected_variants": 15,
            "registered_machine_results": 15,
            "casing_dependencies": 3,
            "machine_ingredient_rows": 51,
            "producer_forms": 9,
            "source_declarations": 4,
            "unreachable": 0,
            "resource_sets_bidirectional": True,
            "exact_legacy_tier1_migrations": 5,
        }, self.document["t16c_evidence"])

    def test_currentness_covers_every_completed_stage(self):
        dependencies = self.document["currentness"]["dependencies"]
        self.assertEqual("T15_READY", dependencies["T15"]["status"])
        self.assertEqual(
            "T16_MACHINE_DENOMINATOR_READY",
            dependencies["denominator"]["status"],
        )
        self.assertEqual(
            "T16C_MACHINE_ACQUISITION_READY",
            dependencies["acquisition"]["status"],
        )
        contracts = self.document["source_contracts"]
        self.assertEqual(
            {
                "behavior_carriers",
                "block_registry",
                "catalog_loader",
                "denominator_builder",
                "denominator_policy",
                "denominator_python_test",
                "item_registry",
                "junit_matrix",
                "machine_tier_catalog",
                "python_test_policy",
                "readiness_python_test",
                "source_expected",
                "t16b_behavior_carrier",
                "t16b_block_registry",
                "t16b_denominator_policy",
                "t16b_denominator_python_test",
                "t16b_gametest_verticals",
                "t16b_item_registry",
                "t16b_junit_execution_matrix",
                "t16b_machine_tier_catalog",
                "t16b_readiness_python_test",
                "t16b_source_expected",
                "t16b_variant_registry",
                "t16c_acquisition_builder",
                "t16c_acquisition_policy",
                "t16c_acquisition_python_test",
                "t16c_block_models",
                "t16c_full_artifact_currentness",
                "t16c_full_builder",
                "t16c_gametests",
                "t16c_identity_migrations",
                "t16c_identity_persistence_junit",
                "t16c_identity_policy_junit",
                "t16c_language",
                "t16c_loot",
                "t16c_machine_recipes",
                "t16c_resource_junit",
                "t16c_tags",
                "t16c_variant_registry",
                "t16d_full_builder",
                "t16d_full_report",
                "t16d_gametest_publication",
                "t16d_history_archive",
                "t16d_load_projection_input",
                "t16d_load_projection_output",
                "t16d_load_python_test",
                "t16d_overall_status",
                "t16d_project_readme",
                "t16d_publication_baseline",
                "t16d_python_affected_closure",
                "t16d_readiness_builder",
                "t16d_readiness_python_test",
                "t16d_third_phase_status",
                "t16d_tools_readme",
                "variant_registry",
            },
            set(contracts),
        )
        self.assertTrue(all(row["sha256"] for row in contracts.values()))
        self.assertEqual(
            {"T16a", "T16b", "T16c", "T16d"},
            set(self.document["currentness"]["completed_stage_sources"]),
        )

    def test_policy_mutations_fail_closed(self):
        policy = builder.load(builder.POLICY)
        early_pending = copy.deepcopy(policy)
        early_pending["stages"]["T16a"]["status"] = "PENDING"
        early_pending["stages"]["T16a"]["reason"] = "mutation"
        early_pending["stages"]["T16a"]["replacement_condition"] = "mutation"
        early_pending["stages"]["T16a"]["recheck_point"] = "mutation"
        with self.assertRaisesRegex(
            ValueError, "cannot complete after an earlier pending gate"
        ):
            builder.build(early_pending)

        missing_exit = copy.deepcopy(policy)
        missing_exit["stages"]["T16d"]["status"] = "PENDING"
        missing_exit["stages"]["T16d"]["reason"] = "mutation"
        missing_exit["stages"]["T16d"]["recheck_point"] = "mutation"
        missing_exit["stages"]["T16d"]["replacement_condition"] = ""
        with self.assertRaisesRegex(ValueError, "lacks replacement_condition"):
            builder.build(missing_exit)

    def test_python_and_full_verification_currentness_are_wired_for_t16d(self):
        policy = json.loads(
            (builder.TOOLS / "python_test_policy.json").read_text(
                encoding="utf-8"
            )
        )
        t16_rule = next(
            row for row in policy["affected_rules"]
            if "tools/build_t16_*.py" in row["paths"]
        )
        self.assertEqual(
            [
                "test_build_t16_machine_acquisition",
                "test_build_t16_machine_denominator",
                "test_build_t16_readiness",
                "test_full_verification_report",
                "test_recipe_load_projection",
            ],
            t16_rule["test_modules"],
        )
        self.assertIn(
            ("tools/build_t16_machine_acquisition.py", "--check"),
            run_full_verification.BUILDER_CHECKS,
        )
        self.assertIn(
            ("tools/build_t16_readiness.py", "--check"),
            run_full_verification.BUILDER_CHECKS,
        )
        self.assertIn(
            "t16_machine_acquisition.json",
            verify_full_verification_report.CORE_ARTIFACTS,
        )
        self.assertIn(
            "t16_readiness.json",
            verify_full_verification_report.CORE_ARTIFACTS,
        )
        self.assertIn(
            "t16_load_projection_input.json",
            verify_full_verification_report.CORE_ARTIFACTS,
        )
        self.assertIn(
            "t16_load_projection.json",
            verify_full_verification_report.CORE_ARTIFACTS,
        )
        self.assertEqual(
            "COMPLETE",
            self.document["stage_gates"]["T16d"]["status"],
        )
        self.assertEqual("T16_READY", self.document["status"])

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
