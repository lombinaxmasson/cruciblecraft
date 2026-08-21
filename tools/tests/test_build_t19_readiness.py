from __future__ import annotations

import copy
import json
import unittest

from tools import build_t19_readiness as builder


class T19ReadinessTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.document = builder.build()

    def test_readiness_completes_abcd_and_final_closure(self):
        self.assertEqual("T19_READY", self.document["status"])
        self.assertEqual(
            ["T19a", "T19b", "T19c", "T19d"],
            self.document["completed_stages"],
        )
        self.assertEqual([], self.document["pending_stages"])
        self.assertTrue(
            self.document["closure_policy"]["final_closure_attempted"]
        )
        self.assertEqual([], self.document["closure_policy"]["pending"])

    def test_ab_evidence_locks_denominator_acquisition_and_bounds(self):
        evidence = self.document["t19ab_evidence"]
        self.assertEqual(47, evidence["denominator"]["canonical"])
        self.assertEqual(4, evidence["denominator"]["implemented"])
        self.assertEqual(5, evidence["denominator"]["selected_t19"])
        self.assertEqual(0, evidence["denominator"]["unclassified"])
        self.assertEqual(5, evidence["acquisition"]["registered_items"])
        self.assertEqual(0, evidence["acquisition"]["unreachable"])
        self.assertEqual([], evidence["unreachable"])
        self.assertEqual(5, evidence["shared_pipe_phase_ticks"])
        self.assertEqual(6, evidence["maximum_cover_slots_per_pipe"])
        self.assertEqual("CLOSED", evidence["status"])

    def test_completed_stage_currentness_covers_runtime_sources(self):
        currentness = self.document["currentness"]
        self.assertEqual(
            {
                "definition_catalog",
                "definition_schema",
                "definition_record",
                "definition_loader",
                "behavior_registry",
                "definition_junit",
                "language_provider",
                "language_junit",
                "processing_language_junit",
                "generated_language_english",
                "generated_language_chinese",
                "recipe_registry",
                "recipe_registry_junit",
                "item_pipe_runtime",
                "fluid_pipe_runtime",
                "configuration_payload",
                "configuration_junit",
                "pipe_transfer_phase",
                "item_route_discovery",
                "item_route_cache",
                "item_transfer_conservation",
                "cover_set_bounds",
                "pipe_catalog",
                "pipe_recipe_catalog",
                "material_rule_expansion",
                "rule_expression",
                "processing_progress_sync",
                "configured_machine_host",
                "configured_machine_menu",
                "shared_processing_menu",
                "processing_machine_screen",
                "crusher_host_audit",
                "crusher_menu_audit",
                "coke_oven_host_audit",
                "coke_oven_menu_audit",
                "production_gametest",
                "progress_junit",
                "menu_junit",
            },
            set(self.document["source_contracts"]),
        )
        self.assertEqual(
            {"junit", "gametest", "python", "lint"},
            set(currentness["verification_targets"]),
        )
        self.assertIn(
            "src/main/resources/data/cruciblecraft/recipe/t8",
            currentness["owned_inputs"],
        )
        pending = currentness["pending_report"]
        self.assertEqual(
            "BOUND_TO_FULL_VERIFICATION_REPORT", pending["status"]
        )
        self.assertTrue(pending["this_refresh_final_closure_attempted"])
        self.assertEqual([], pending["pending"])
        self.assertEqual(
            "tools/full_verification_report.json", pending["evidence"]
        )

    def test_c_evidence_closes_pipe_rows_for_final_closure(self):
        evidence = self.document["t19c_evidence"]
        self.assertEqual(25, evidence["pipe_acquisition"]["rows"])
        self.assertEqual(
            {
                "GT6_SOURCE_CRAFTING": 5,
                "DESIGN_POLICY_NON_GT6": 20,
            },
            evidence["classifications"],
        )
        self.assertEqual(0, evidence["pipe_acquisition"]["unreachable"])
        self.assertEqual(257, evidence["logical_t8_expansion_count"])
        self.assertEqual(0, evidence["material_rule_rows_added"])
        self.assertFalse(evidence["gt6_recipe_flags_modified"])
        self.assertEqual(
            {
                "existing_rules_updated_in_place": 8,
                "fluid_gauge_rules": 5,
                "item_gauge_rules": 3,
                "predicate_scope": (
                    "one output/specification gauge per existing T8 rule"
                ),
                "logical_t8_expansion_before": 257,
                "logical_t8_expansion_after": 257,
                "material_rule_rows_added": 0,
                "gt_recipe_rows_added": 0,
            },
            evidence["o28_rationale"],
        )
        self.assertEqual("CLOSED", evidence["status"])
        self.assertTrue(
            all(
                row["sha256"]
                for row in self.document["source_contracts"].values()
            )
        )

    def test_d_evidence_limits_o20_to_configured_machines(self):
        evidence = self.document["t19d_evidence"]
        self.assertEqual(
            ["status", "status_argument", "progress_permille"],
            evidence["container_data_slots"],
        )
        self.assertEqual([0, 1000], evidence["progress_permille_bounds"])
        self.assertTrue(evidence["menu_updates_each_active_tick"])
        self.assertTrue(evidence["exact_block_entity_progress_preserved"])
        self.assertTrue(evidence["long_work_supported"])
        self.assertFalse(evidence["per_tick_full_block_entity_sync"])
        self.assertEqual(
            {
                "Crusher": "EXISTING_PER_TICK_CONTAINER_DATA_SUFFICIENT",
                "Crusher_container_data_slots": 7,
                "CokeOven": "EXISTING_PER_TICK_CONTAINER_DATA_SUFFICIENT",
                "CokeOven_container_data_slots": 6,
                "strategy_extended": False,
            },
            evidence["existing_host_audit"],
        )
        self.assertEqual("CLOSED", evidence["status"])

    def test_publication_load_and_performance_are_closed(self):
        publication = self.document["publication_load_gate"]
        self.assertEqual(32, publication["publication_baseline"]["recipe_map_count"])
        self.assertEqual(
            {
                "logical_rows": 18_875,
                "eager_rows": 16_650,
                "lazy_rows": 2_225,
            },
            publication["publication_baseline"]["publication_totals"],
        )
        self.assertEqual(24, publication["emi_enumeration"]["configured_maps"])
        self.assertEqual(
            8,
            publication["o28_rationale"][
                "existing_rules_updated_in_place"
            ],
        )
        vanilla = publication["vanilla_acquisition"]
        self.assertEqual((5, 25, 30), (
            vanilla["cover_entries"],
            vanilla["pipe_entries"],
            vanilla["entries_added"],
        ))
        self.assertEqual(
            1031,
            vanilla["generated_recipe_files_after_t19"],
        )
        self.assertEqual(0, vanilla["gt_recipe_rows_added"])
        summary = self.document["closure_summary"]
        self.assertEqual(
            {
                "english_keys": 3_316,
                "chinese_translations": 946,
                "visible_chinese_debt": 2_370,
                "missing_material_names": 1_566,
            },
            summary["localization"],
        )
        self.assertEqual(
            {
                "recipe_types": ["gt_recipe", "material_rule"],
                "recipe_type_count": 2,
                "recipe_serializers": ["gt_recipe", "material_rule"],
                "recipe_serializer_count": 2,
            },
            summary["active_recipe_registration"],
        )
        self.assertEqual(
            {
                "java_unit_tests": 538,
                "production_game_tests": 83,
                "python_unit_tests": 501,
            },
            summary["verification_expectations"],
        )
        self.assertEqual(
            {
                "vanilla_datapack_entries": 30,
                "gt_authored_entries": 0,
                "gt_logical_rows": 0,
            },
            publication["load_projection"]["publication_domains"],
        )
        performance = self.document["performance_gate"]
        self.assertEqual(5, performance["tick_schedule"]["interval_ticks"])
        self.assertEqual(
            32_768,
            performance["route_discovery"]["maximum_visited_pipes"],
        )
        self.assertEqual(256, performance["memory"][
            "maximum_route_cache_entries_per_item_pipe"
        ])
        self.assertEqual(
            13,
            performance["synchronization"][
                "maximum_configuration_payload_bytes"
            ],
        )
        self.assertEqual([], performance["blocked_conservation"]["pending"])

    def test_pending_or_final_closure_mutations_fail_closed(self):
        policy = builder.load(builder.POLICY)
        changed = copy.deepcopy(policy)
        changed["stages"]["T19b"]["status"] = "PENDING"
        for field in ("reason", "replacement_condition", "recheck_point"):
            changed["stages"]["T19b"][field] = "mutation"
        with self.assertRaises(ValueError):
            builder.build(changed)
        changed = copy.deepcopy(policy)
        changed["closure_policy"]["final_closure_attempted"] = False
        with self.assertRaises(ValueError):
            builder.build(changed)

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
