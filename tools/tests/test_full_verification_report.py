import importlib.util
import json
import os
import re
import sys
import unittest
from pathlib import Path
from unittest import mock


TOOLS = Path(__file__).resolve().parents[1]
SPEC = importlib.util.spec_from_file_location(
    "verify_full_verification_report",
    TOOLS / "verify_full_verification_report.py",
)
assert SPEC and SPEC.loader
MODULE = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = MODULE
SPEC.loader.exec_module(MODULE)


class FullVerificationReportTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        report_path = Path(
            os.environ.get(
                "CRUCIBLECRAFT_FULL_VERIFICATION_REPORT",
                MODULE.REPORT,
            )
        )
        cls.document = json.loads(
            report_path.read_text(encoding="utf-8")
        )
        cls.context = MODULE.load_or_build_validation_context()
        cls.snapshot = cls.context.value("tooling_snapshot")

    def test_builder_record_uses_policy_and_per_script_evidence(self):
        policy = MODULE.builder_policy_document()
        timings = [
            {
                "script": row["script"],
                "argv": row["ordinary_args"],
                "proof_tier": row["proof_tier"],
                "elapsed_ms": 10.0 + index,
                "result": "PASS",
            }
            for index, row in enumerate(policy["builders"])
        ]
        document = {"verification_runs": {}}
        with mock.patch.object(
            MODULE,
            "_current_pending_errors",
            return_value=[],
        ):
            errors = MODULE.record_builder(
                document,
                1000.0,
                "SKIP",
                per_builder=timings,
                persist=False,
            )
        self.assertEqual([], errors)
        record = document["verification_runs"]["builder"]
        self.assertEqual(MODULE.builder_policy_commands(), record["commands"])
        self.assertEqual(timings, record["per_builder"])
        builder_results = record["builder_results"]
        for script, _description in MODULE.REQUIRED_READINESS_BUILDERS:
            self.assertEqual("PASS", builder_results[script], script)
        self.assertEqual(
            "soft_report_only",
            record["performance_regression"]["policy"],
        )

    def test_committed_report_matches_current_tooling_snapshot(self):
        document = self.document
        snapshot = self.snapshot
        self.assertEqual(
            [],
            MODULE.validate_report_document(document, self.context),
        )
        self.assertEqual(
            snapshot["python_test_count"],
            document["tests"]["python_unit_tests"]["tests"],
        )
        self.assertGreater(snapshot["java_source_test_count"], 0)
        self.assertGreater(snapshot["python_test_count"], 0)
        self.assertEqual(137, MODULE.current_game_test_count())
        self.assertEqual(
            snapshot["java_source_test_count"],
            document["tests"]["java_unit_tests"]["tests"],
        )
        if document["status"] == "READY":
            plan = (MODULE.ROOT / "CrucibleCraft-总体规划.md").read_text(
                encoding="utf-8"
            )
            match = re.search(r"Java / Python 单测 \| (\d+) / (\d+)", plan)
            self.assertIsNotNone(match)
            planned_java, planned_python = map(int, match.groups())
            self.assertEqual(snapshot["java_source_test_count"], planned_java)
            self.assertLessEqual(planned_python, snapshot["python_test_count"])
        ore = document["ore_pipeline_acceptance"]
        self.assertEqual(2117, ore["concrete_recipes"])
        self.assertEqual(494, ore["concrete_recipes_by_map"]["crusher"])
        self.assertEqual(137, ore["high_version_ore_block_recipes"])
        self.assertEqual(
            5, ore["ore_block_crusher_ingress"]["output_count"]
        )
        self.assertTrue(all(ore["game_tests"].values()))
        t4 = document["t4_tool_acceptance"]
        self.assertEqual(23, t4["source_rules"])
        self.assertEqual(12, t4["tool_types"])
        self.assertEqual(3657, t4["expanded_recipes"])
        self.assertEqual(0, t4["unclassified"])
        self.assertEqual(0, t4["signature_collisions"])
        self.assertEqual(1127, t4["eligible_without_route"]["total"])
        self.assertEqual(
            208,
            t4["eligible_without_route"]["by_tool"]["pickaxe"],
        )
        self.assertEqual(
            9,
            t4["eligibility_predicate_sources"]["predicate_count"],
        )
        self.assertEqual(
            {"stone", "wood"},
            set(t4["identity_literals"]),
        )
        component = document["component_pipeline_acceptance"]
        self.assertEqual(31, component["shape_count"])
        self.assertEqual(
            {"playable": 20, "skipped": 42},
            component["template_classifications"],
        )
        self.assertEqual(8141, component["expanded_recipes"])
        self.assertEqual(2782, component["extruder_expanded_recipes"])
        self.assertEqual(0, component["shadowed_recipes"])
        self.assertEqual(
            49,
            snapshot["trees"]["component_rule_generated"]["files"],
        )
        t5 = document["t5_chemical_acceptance"]
        self.assertEqual("SOURCE_REPLAY_VERIFIED", t5["readiness_status"])
        self.assertEqual(145, t5["terminal_dust_denominator"])
        self.assertEqual(
            {
                "decomposable_without_route": 36,
                "route_ready": 18,
                "route_tagged_but_quarantined": 57,
                "unresolved_deferred": 34,
            },
            t5["terminal_readiness_classifications"],
        )
        self.assertEqual(145, t5["terminal_dust_live_routes"])
        self.assertEqual(0, t5["terminal_dust_unresolved"])
        self.assertEqual(152, t5["generated_recipes"])
        self.assertEqual(15, t5["registered_chemical_fluids"])
        self.assertTrue(t5["closure_ready"])
        self.assertEqual(
            153,
            snapshot["trees"]["t5_chemical_generated"]["files"],
        )
        t7 = document["t7_material_fact_acceptance"]
        self.assertEqual("READY", t7["readiness_status"])
        self.assertTrue(t7["ledger_current"])
        self.assertEqual(62, t7["classified_tags"])
        self.assertEqual(0, t7["unclassified_tags"])
        self.assertEqual(
            {
                "rule-input": 30,
                "build-time-only": 7,
                "not-applicable": 4,
                "deferred": 21,
            },
            t7["classification_counts"],
        )
        self.assertEqual(
            {"gem_to_dust": 94, "ingot_to_dust": 126},
            t7["mortar_rule_counts"],
        )
        self.assertEqual(
            220, t7["fact_counts"]["new_mortar_rule_expansion_count"]
        )
        self.assertEqual(101, t7["material_tag_vocabulary"]["count"])
        self.assertEqual(
            609, t7["fact_counts"]["formula_visible_material_count"]
        )
        self.assertEqual(
            343,
            t7["fact_counts"]["formula_without_registered_form_count"],
        )
        self.assertEqual(
            0, len(t7["material_rule_audit"]["unknown_tag_references"])
        )
        self.assertEqual(
            "SOURCE_FACT_LOCATED", t7["t10_damage_gate"]["status"]
        )
        self.assertFalse(
            t7["mortar_scope_decision"]["crushed_to_dust"][
                "mortar_grindable_guard"
            ]
        )
        self.assertEqual(
            "NOT_EQUIVALENT_TO_11_TAG_ONLY_RULES",
            t7["extruder_compaction_decision"]["status"],
        )
        self.assertEqual(
            27,
            t7["extruder_compaction_decision"][
                "minimum_io_variant_rule_count"
            ],
        )
        self.assertEqual(
            "OWNERS_ASSIGNED",
            t7["legacy_rule_condition_backfill"]["status"],
        )
        self.assertEqual(
            17326,
            t7["runtime_publication"][
                "post_t7_all_published_recipes"
            ],
        )
        self.assertTrue(t7["within_publication_budget"])
        self.assertTrue(t7["within_t7_authored_material_rule_budget"])
        t8 = document["t8_pipe_acceptance"]
        self.assertEqual("READY", t8["readiness_status"])
        self.assertTrue(t8["ledger_current"])
        self.assertTrue(t8["material_projection_current"])
        self.assertEqual([], t8["gtceu_tracked_files"])
        self.assertEqual(282, t8["registered_pipe_forms"])
        self.assertEqual(
            282, t8["runtime_budget"]["combined_runtime_blocks"]
        )
        self.assertEqual(18048, t8["runtime_budget"]["logical_states"])
        self.assertEqual(8, t8["generic_rule_count"])
        self.assertEqual(257, t8["expanded_pipe_recipes"])
        self.assertEqual(320, t8["material_rule_budget"])
        self.assertEqual(
            25, t8["nonmetal_fluid_pipe_acquisition"]["form_count"]
        )
        self.assertEqual(
            17583,
            t8["runtime_publication"][
                "post_t8_all_published_recipes"
            ],
        )
        self.assertTrue(t8["within_publication_budget"])
        self.assertTrue(all(t8["game_tests"].values()))
        worldgen = document["worldgen_catalog_acceptance"]
        self.assertTrue(worldgen["readiness_current"])
        self.assertTrue(worldgen["generated_resources_current"])
        self.assertEqual(
            129, worldgen["counts"]["closure_vein_classifications"]
        )
        self.assertEqual(
            129, worldgen["counts"]["closure_configured_ore_features"]
        )
        self.assertEqual(
            137, worldgen["counts"]["registered_ore_materials"]
        )
        self.assertEqual(274, worldgen["counts"]["registered_ore_blocks"])
        self.assertEqual(2, worldgen["counts"]["fluid_deposits"])
        self.assertEqual(
            "keep_two_hosts", worldgen["host_policy"]["decision"]
        )
        self.assertEqual(
            "T20_CLASSIFIED",
            worldgen["geometry_policy"]["status"],
        )
        self.assertIsNone(worldgen["geometry_policy"]["open_item"])
        self.assertEqual(
            {"DESIGN_POLICY": 56, "SOURCE_DERIVED": 73},
            worldgen["t20_fidelity"]["statuses"],
        )
        self.assertEqual(0, worldgen["t20_fidelity"]["placeholder"])
        self.assertTrue(worldgen["runtime_registry_placement_test"])
        t10 = document["t10_preflight_acceptance"]
        self.assertEqual("T10_READY", t10["status"])
        self.assertTrue(t10["ledger_current"])
        self.assertEqual(
            {"materials": 321, "recipes": 321},
            t10["route_counts"]["hot_ingot"],
        )
        self.assertEqual(
            {"materials": 323, "recipes": 646},
            t10["route_counts"]["multi_ingot"],
        )
        self.assertEqual(
            21000, t10["budget_projection"]["global_budget"]
        )
        self.assertEqual(57, t10["prefix_facts"]["startup_prefix_count"])
        self.assertEqual(1830, t10["prefix_facts"]["handshake_entry_count"])
        self.assertEqual(
            {
                "double_ingot": 323,
                "triple_ingot": 323,
                "ingot_hot": 321,
            },
            t10["prefix_facts"]["gate_registration_counts"],
        )
        self.assertEqual(
            {"ingotHot": 3.0},
            t10["prefix_facts"]["nonzero_source_heat_damage"],
        )
        self.assertEqual(
            967,
            t10["runtime_publication"]["current_t10_recipe_additions"],
        )
        self.assertEqual(
            1500,
            t10["runtime_publication"][
                "known_form_material_rule_budget"
            ],
        )
        self.assertEqual(
            "CLOSED_WITH_COMPONENT_CELLS",
            t10["container_domains"]["status"],
        )
        self.assertEqual(
            93,
            t10["container_domains"]["acceptance_counts"][
                "new_t10_chemical_fluids"
            ],
        )
        self.assertEqual(3360, t10["load_gate"]["datapack_recipe_entries"])
        self.assertEqual(18550, t10["load_gate"]["published_recipes"])
        t11 = document["t11_preflight_acceptance"]
        self.assertEqual("T11_READY", t11["status"])
        self.assertTrue(t11["ledger_current"])
        self.assertEqual(
            [
                "crude_oil_distillation",
                "fuel_oil_engine",
                "methane_gas_fuel",
                "natural_gas_to_methane",
            ],
            t11["independent_expectation"]["route_ids"],
        )
        self.assertEqual(
            {
                "crude_oil_distillation": 872,
                "fuel_oil_engine": 14,
                "methane_gas_fuel": 20,
                "natural_gas_to_methane": 553,
            },
            t11["selected_recipe_indices"],
        )
        self.assertEqual(
            1517, t11["distillery_counts"]["classified"]
        )
        self.assertEqual(
            0, t11["distillery_counts"]["unclassified"]
        )
        bridge = t11["fluid_identity_closure"][
            "source_fluid_bridges"
        ]["liquid_medium_oil"]
        self.assertEqual("DESIGN_POLICY", bridge["status"])
        self.assertEqual(
            "NO_DIRECT_BINDING_AT_FIXED_REVISION",
            bridge["gt6_equivalence"],
        )
        self.assertEqual("O-37", bridge["closed_item"])
        self.assertEqual(
            "O37_CLOSED_PERMANENT_DESIGN_POLICY",
            bridge["closure"],
        )
        self.assertTrue(bridge["permanent"])
        self.assertEqual("SOURCE_MATERIAL_LAYER_ONLY",
                         bridge["material_9852_role"])
        self.assertEqual(0, bridge["publication_delta"])
        self.assertEqual(
            18875, t11["load_gate"]["projected"]["published_recipes"]
        )
        self.assertEqual(
            3243,
            t11["load_gate"]["projected"][
                "datapack_recipe_entries"
            ],
        )
        self.assertEqual(
            4, t11["recipe_manifest"]["counts"]["published"]
        )
        self.assertEqual(
            0,
            t11["load_gate"]["budgets"][
                "t11_authored_material_rules"
            ],
        )
        t13 = document["t13_denominator_acceptance"]
        self.assertEqual("T13_READY", t13["status"])
        self.assertTrue(t13["manifest_current"])
        self.assertTrue(t13["readiness_current"])
        self.assertEqual(7, t13["acceptance"]["tables"])
        self.assertEqual(0, t13["acceptance"]["unclassified"])
        self.assertEqual(
            0, t13["acceptance"]["normalization_blockers"]
        )
        self.assertEqual(95, t13["acceptance"]["recipe_maps"])
        self.assertEqual(720_841, t13["acceptance"]["recipe_rows"])
        self.assertEqual("CLOSED", t13["acceptance"]["o_33"])
        self.assertEqual(
            {
                "cover_kinds",
                "energy_identities",
                "itemgenerator_domains",
                "machine_kinds",
                "multiblock_kinds",
                "prefixes",
                "recipe_maps",
            },
            set(t13["denominators"]),
        )
        self.assertEqual(
            0, t13["zero_content_delta"]["datapack_delta"]
        )
        self.assertEqual(
            0, t13["zero_content_delta"]["publication_delta"]
        )
        energy_audit = document[
            "processing_machine_energy_audit_acceptance"
        ]
        self.assertEqual(
            "PROCESSING_MACHINE_ENERGY_AUDIT_READY",
            energy_audit["status"],
        )
        self.assertTrue(energy_audit["audit_current"])
        self.assertEqual(25, energy_audit["counts"]["machine_specs"])
        self.assertEqual(
            0, energy_audit["counts"]["implicit_energy_arguments"]
        )
        self.assertEqual(8, energy_audit["counts"]["legacy_kinetic"])
        self.assertEqual(0, energy_audit["counts"]["new_legacy_kinetic"])
        energy_rows = {
            row["id"]: row for row in energy_audit["rows"]
        }
        self.assertEqual(
            "MAPPED_DEFERRED", energy_rows["extruder"]["disposition"]
        )
        self.assertEqual(
            "CROSS_OWNER_DEFERRED",
            energy_rows["compressor"]["disposition"],
        )
        t14 = document["t14_load_acceptance"]
        self.assertEqual("T14_READY", t14["status"])
        self.assertTrue(t14["readiness_current"])
        self.assertEqual("CLOSED", t14["o_26"]["status"])
        self.assertEqual("hybrid", t14["materialization"]["winner"])
        self.assertEqual(
            {
                "authored": 20,
                "cache_ceiling": 512,
                "eager": 557,
                "lazy": 2225,
                "logical": 2782,
            },
            t14["materialization"]["extruder"],
        )
        self.assertEqual(
            3388, t14["load_gate"]["datapack_authored_entries"]
        )
        self.assertEqual([], t14["load_gate"]["pending_measurements"])
        t15 = document["t15_readiness_acceptance"]
        self.assertEqual("T15_READY", t15["status"])
        self.assertTrue(t15["readiness_current"])
        self.assertEqual(
            ["T15a", "T15b", "T15c", "T15d", "T15e"],
            t15["completed_stages"],
        )
        self.assertEqual([], t15["pending_stages"])
        self.assertTrue(t15["historical_immutable_evidence"]["immutable"])
        self.assertFalse(t15["historical_immutable_evidence"]["current"])
        t15c = t15["t15c_acquisition_gate"]
        self.assertEqual("PASS", t15c["status"])
        self.assertEqual(
            {
                "casing_recipes": 6,
                "machine_variant_recipes": 9,
                "registered_results": 15,
                "unreachable": 0,
            },
            t15c["acquisition"]["counts"],
        )
        self.assertEqual(0, t15c["load_projection"]["publication_delta"])
        self.assertTrue(all(
            value == 0
            for value in t15c["load_projection"][
                "incremental_counts"
            ].values()
        ))
        t15d = t15["t15d_identity_gate"]
        self.assertEqual("PASS", t15d["status"])
        self.assertEqual(6, t15d["junit"]["test_count"])
        self.assertEqual(3, t15d["gametest"]["added_test_count"])
        self.assertEqual(56, t15d["gametest"]["full_suite_test_count"])
        self.assertEqual(
            {
                "processingIdentityMismatchesQuarantineAcrossNbtReload",
                "blankProcessingIdentityAdoptsCurrentAcrossNbtReload",
                "tierProfileIdentityIsQuarantinedAcrossNbtReload",
            },
            set(t15d["gametest"]["tests"]),
        )
        t15e = t15["t15e_matcher_boundary_gate"]
        self.assertEqual("PASS", t15e["status"])
        self.assertEqual(
            t15e["physical_structure"]["item_fluid_ports"], 15
        )
        self.assertEqual(
            t15e["physical_structure"]["energy_input_ports"], 2
        )
        self.assertEqual(t15e["physical_structure"]["controllers"], 1)
        self.assertEqual(t15e["host_layout"]["item_inputs"], 1)
        self.assertEqual(t15e["host_layout"]["item_outputs"], 6)
        self.assertEqual(t15e["host_layout"]["fluid_inputs"], 1)
        self.assertEqual(t15e["host_layout"]["fluid_outputs"], 2)
        self.assertEqual(
            t15e["port_host_boundary"]["item_matcher_supplies"], 1
        )
        self.assertEqual(
            t15e["port_host_boundary"]["fluid_matcher_supplies"], 1
        )
        self.assertFalse(
            t15e["port_host_boundary"][
                "physical_ports_expand_matcher_supplies"
            ]
        )
        self.assertFalse(
            t15e["matcher_boundary"]["presence_cap_triggered"]
        )
        self.assertEqual(
            [12, 16, 32, 64],
            t15e["benchmark"]["dense_supply_counts"],
        )
        t16 = document["t16_readiness_acceptance"]
        self.assertEqual("T16_READY", t16["status"])
        self.assertTrue(t16["readiness_current"])
        self.assertEqual(
            ["T16a", "T16b", "T16c", "T16d"],
            t16["completed_stages"],
        )
        self.assertEqual([], t16["pending_stages"])
        self.assertEqual(
            {
                "denominator_kinds": 20,
                "unclassified": 0,
                "selected_kinds": 5,
                "selected_variants": 15,
                "preimplemented_kinds": 2,
                "deferred_kinds": 13,
                "tier4_deferred": 20,
                "publication_delta": 0,
            },
            t16["closure_summary"],
        )
        t16d = t16["t16d_evidence"]
        self.assertEqual("PASS", t16d["status"])
        self.assertEqual(
            {"RU": "KINETIC_ROTATION", "KU": "KINETIC_PUSH"},
            t16d["energy_identity"],
        )
        self.assertEqual(15, t16d["identity_acquisition"][
            "vanilla_crafting_rows"
        ])
        self.assertEqual(0, t16d["identity_acquisition"][
            "gt_recipe_rows"
        ])
        self.assertTrue(all(
            value == 0
            for value in t16d["load_projection"][
                "incremental_counts"
            ].values()
        ))
        self.assertEqual(
            {
                "logical_rows": 18875,
                "eager_rows": 16650,
                "lazy_rows": 2225,
            },
            t16d["publication_baseline"]["publication_totals"],
        )
        self.assertEqual(
            32, len(t16d["publication_baseline"]["recipe_map_ids"])
        )
        self.assertEqual(24, t16d["emi_enumeration"]["configured_maps"])
        self.assertTrue(t16d["emi_enumeration"][
            "recipe_enumeration_equal"
        ])
        self.assertEqual(6, t16d["gametest"]["t16_test_count"])
        self.assertEqual(
            MODULE.current_game_test_count(),
            t16d["gametest"]["full_suite_test_count"],
        )
        t17 = document["t17_readiness_acceptance"]
        self.assertEqual("T17_READY", t17["status"])
        self.assertTrue(t17["readiness_current"])
        self.assertEqual(
            ["T17a", "T17b", "T17c", "T17d"],
            t17["completed_stages"],
        )
        self.assertEqual([], t17["pending_stages"])
        self.assertEqual(
            {
                "denominator_kinds": 28,
                "energy_kind_counts": {"EU": 16, "HU": 12},
                "unclassified": 0,
                "selected_kinds": 3,
                "selected_variants": 9,
                "preimplemented_reference_kinds": 1,
                "deferred_kinds": 24,
                "cross_owner_deferred_kinds": 1,
                "energy_disposition_audit": "AUDITED",
                "processing_machine_energy_audit":
                    "PROCESSING_MACHINE_ENERGY_AUDIT_READY",
                "gt_recipe_row_mutation": 0,
                "hu_execution_variants": 9,
                "eu_reference_variants": 3,
                "electric_mixer_tier_variants": 0,
                "resources_acquisition_full_closure": "COMPLETE_T17C",
                "machine_acquisition_variants": 9,
                "machine_acquisition_unreachable": 0,
                "preimplemented_electrolyzer_variants": 3,
                "heat_tier4_deferred": 10,
                "electric_tier4_5_deferred": 32,
                "publication_delta": 0,
            },
            t17["closure_summary"],
        )
        t17d = t17["t17d_evidence"]
        self.assertEqual("PASS", t17d["status"])
        self.assertEqual(
            {"EU": 16, "HU": 12},
            t17d["denominator"]["energy_kinds"],
        )
        self.assertEqual(24, t17d["denominator"]["deferred_kinds"])
        self.assertEqual(
            1, t17d["denominator"]["cross_owner_deferred_kinds"]
        )
        self.assertEqual(
            "AUDITED",
            t17d["denominator"]["energy_disposition_audit"]["status"],
        )
        self.assertEqual(
            "HEAT_ADJACENT_BOTTOM_FIREBOX",
            t17d["energy_topology"]["HU"],
        )
        self.assertEqual(
            "ELECTRIC_BUFFERED_CABLE_ENDPOINT",
            t17d["energy_topology"]["EU"],
        )
        self.assertEqual(9, t17d["identity_acquisition"][
            "vanilla_crafting_rows"
        ])
        self.assertEqual(0, t17d["identity_acquisition"][
            "gt_recipe_rows"
        ])
        self.assertTrue(all(
            value == 0
            for value in t17d["load_projection"][
                "incremental_counts"
            ].values()
        ))
        self.assertEqual(
            {
                "logical_rows": 18_875,
                "eager_rows": 16_650,
                "lazy_rows": 2_225,
            },
            t17d["publication_baseline"]["publication_totals"],
        )
        self.assertEqual(
            32, len(t17d["publication_baseline"]["recipe_map_ids"])
        )
        self.assertEqual(24, t17d["emi_enumeration"]["configured_maps"])
        self.assertTrue(t17d["emi_enumeration"][
            "recipe_enumeration_equal_to_t16"
        ])
        self.assertEqual(8, t17d["gametest"]["t17_test_count"])
        self.assertEqual(
            MODULE.current_game_test_count(),
            t17d["gametest"]["full_suite_test_count"],
        )
        t18 = document["t18_readiness_acceptance"]
        self.assertEqual("T18_READY", t18["status"])
        self.assertTrue(t18["readiness_current"])
        self.assertEqual(
            ["T18a", "T18b", "T18c", "T18d"],
            t18["completed_stages"],
        )
        self.assertEqual([], t18["pending_stages"])
        self.assertEqual(
            {
                "denominator_kinds": 29,
                "energy_identities": 2,
                "unclassified": 0,
                "selected_kinds": 6,
                "preimplemented_reference_kinds": 3,
                "deferred_kinds": 20,
                "converter_profiles": 6,
                "converter_acquisition_recipes": 6,
                "converter_acquisition_unreachable": 0,
                "current_identity_profiles": 2,
                "four_chain_conservation": "PASS",
                "o37_resolution": "DESIGN_POLICY",
                "o37_publication_delta": 0,
                "gt_recipe_row_mutation": 0,
                "publication_delta": 0,
                "pending": 0,
            },
            t18["closure_summary"],
        )
        steam_engine = t18["t18a_evidence"]["steam_engine"]
        self.assertEqual(1302, steam_engine["source_id"])
        self.assertEqual(586, steam_engine["source_line"])
        self.assertEqual(2, steam_engine["steam_per_eu"])
        self.assertEqual(
            "SOURCE_BACKED",
            steam_engine["source_conservation"]["classification"],
        )
        self.assertEqual(
            50,
            steam_engine["source_conservation"]["steamInputMb"]
            // steam_engine["source_conservation"]["steamMbPerKu"],
        )
        self.assertEqual(
            "SOURCE_DERIVED_NOMINAL",
            steam_engine["source_nominal"]["classification"],
        )
        self.assertEqual(
            12,
            steam_engine["source_nominal"]["registeredNumerator"]
            // steam_engine["source_nominal"]["steamPerEu"],
        )
        self.assertEqual(
            "DESIGN_POLICY_FIXED_OUTPUT",
            steam_engine["fixed_output"]["classification"],
        )
        self.assertEqual(12, steam_engine["fixed_output"]["kuPerTick"])
        self.assertEqual(
            "DEFERRED_REPLACEMENT",
            steam_engine["gt6_runtime"]["classification"],
        )
        self.assertEqual(
            [6, 24],
            [
                steam_engine["gt6_runtime"]["minimumKuPerTick"],
                steam_engine["gt6_runtime"]["maximumKuPerTick"],
            ],
        )
        self.assertTrue(
            steam_engine["gt6_runtime"]["replacementCondition"]
        )
        self.assertTrue(steam_engine["gt6_runtime"]["recheckPoint"])
        self.assertEqual(3, len(steam_engine["source_evidence_paths"]))
        t18d = t18["t18d_evidence"]
        self.assertEqual("PASS", t18d["status"])
        self.assertEqual(29, t18d["denominator"]["machine_kinds"])
        self.assertEqual(6, t18d["denominator"]["selected_kinds"])
        self.assertEqual(3, t18d["denominator"][
            "preimplemented_reference_kinds"
        ])
        self.assertEqual(20, t18d["denominator"]["deferred_kinds"])
        self.assertEqual(0, t18d["denominator"]["unclassified"])
        self.assertEqual(6, t18d["acquisition"]["profiles"])
        self.assertEqual(0, t18d["acquisition"]["unreachable"])
        self.assertEqual(0, t18d["acquisition"]["gt_recipe_rows_added"])
        self.assertTrue(all(
            value == 0
            for value in t18d["load_projection"][
                "incremental_counts"
            ].values()
        ))
        self.assertEqual(
            {
                "logical_rows": 18_875,
                "eager_rows": 16_650,
                "lazy_rows": 2_225,
            },
            t18d["publication_baseline"]["publication_totals"],
        )
        self.assertEqual(
            32, len(t18d["publication_baseline"]["recipe_map_ids"])
        )
        self.assertEqual(24, t18d["emi_enumeration"]["configured_maps"])
        self.assertEqual(
            "DESIGN_POLICY", t18d["o37_publication"]["resolution"]
        )
        self.assertEqual(0, t18d["o37_publication"][
            "publication_delta"
        ])
        self.assertEqual([], t18d["pending"])
        t19 = document["t19_readiness_acceptance"]
        self.assertEqual("T19_READY", t19["status"])
        self.assertTrue(t19["readiness_current"])
        self.assertEqual(
            ["T19a", "T19b", "T19c", "T19d"],
            t19["completed_stages"],
        )
        self.assertEqual([], t19["pending_stages"])
        self.assertEqual(
            {
                "canonical": 47,
                "implemented": 4,
                "selected_t19": 5,
                "deferred_with_reason": 28,
                "out_of_scope": 10,
                "unclassified": 0,
            },
            t19["closure_summary"]["cover_denominator"],
        )
        self.assertEqual(30, t19["closure_summary"][
            "vanilla_datapack_entries_added"
        ])
        self.assertEqual(0, t19["closure_summary"]["gt_recipe_rows_added"])
        self.assertEqual(
            {
                "java_unit_tests": 538,
                "production_game_tests": 83,
                "python_unit_tests": 501,
            },
            t19["closure_summary"]["verification_expectations"],
        )
        self.assertEqual(
            2,
            t19["closure_summary"]["active_recipe_registration"][
                "recipe_type_count"
            ],
        )
        self.assertEqual(
            2,
            t19["closure_summary"]["active_recipe_registration"][
                "recipe_serializer_count"
            ],
        )
        publication = t19["publication_load_gate"]
        self.assertEqual(
            (1001, 1031, 30),
            (
                publication["vanilla_acquisition"][
                    "generated_recipe_files_before_t19"
                ],
                publication["vanilla_acquisition"][
                    "generated_recipe_files_after_t19"
                ],
                publication["vanilla_acquisition"]["entries_added"],
            ),
        )
        self.assertEqual(
            {
                "vanilla_datapack_entries": 30,
                "gt_authored_entries": 0,
                "gt_logical_rows": 0,
            },
            publication["load_projection"]["publication_domains"],
        )
        self.assertEqual(5, t19["performance_gate"][
            "tick_schedule"
        ]["interval_ticks"])
        self.assertEqual(13, t19["performance_gate"][
            "synchronization"
        ]["maximum_configuration_payload_bytes"])
        self.assertEqual([], t19["performance_gate"]["pending"])
        t20 = document["t20_readiness_acceptance"]
        self.assertEqual("T20_READY", t20["status"])
        self.assertTrue(t20["readiness_current"])
        self.assertEqual(
            ["T20a", "T20b", "T20c", "T20d", "T20e"],
            t20["completed_stages"],
        )
        self.assertEqual([], t20["pending_stages"])
        self.assertEqual(
            (40, 75, 1, 129, 134),
            (
                t20["closure"]["source_large_facts"],
                t20["closure"]["source_explicit_small_facts"],
                t20["closure"]["source_dynamic_small_rules"],
                t20["closure"]["catalog_identities"],
                t20["closure"]["runtime_large_veins"],
            ),
        )
        self.assertEqual(
            {"DESIGN_POLICY": 56, "SOURCE_DERIVED": 73},
            t20["fidelity"]["statuses"],
        )
        self.assertEqual(0, t20["fidelity"]["placeholder"])
        self.assertEqual(0, t20["fidelity"]["unverified"])
        self.assertEqual(
            "CODEC_REJECTED",
            t20["save_boundary"]["unknown_profile_version"],
        )
        self.assertEqual(
            0, t20["load"]["publication"]["logical_row_delta"]
        )
        t21 = document["t21_readiness_acceptance"]
        self.assertEqual("T21_READY", t21["status"])
        self.assertTrue(t21["readiness_current"])
        self.assertEqual(
            ["T21a", "T21b", "T21c", "T21d"],
            t21["completed_stages"],
        )
        self.assertEqual([], t21["pending_stages"])
        self.assertEqual(224, t21["closure"]["material_candidates"])
        self.assertEqual(64_245, t21["closure"]["mixer_source_rows"])
        self.assertEqual(3_414, t21["closure"]["mixer_templates"])
        self.assertEqual(0, t21["closure"]["v1_required_remaining"])
        self.assertFalse(
            t21["closure"]["row_diagnostic_is_closure_numerator"]
        )
        self.assertEqual(0, t21["fidelity"]["mixer_missing"])
        self.assertEqual(0, t21["fidelity"]["mixer_extra"])
        self.assertEqual(
            ["carbon", "charcoal", "coal", "coal_coke"],
            t21["runtime"]["family_members"],
        )
        self.assertEqual(
            {"logical": 4, "eager": 4, "lazy": 0},
            t21["load"]["publication_delta"],
        )
        self.assertFalse(
            document["artifact_policy"]["ordinary_ci_requires_local_cache"]
        )

    def test_stale_ready_report_is_rejected(self):
        document = json.loads(json.dumps(self.document))
        document["status"] = "READY"
        document["ready_binding"] = {
            "tooling_snapshot_sha256": "stale",
        }
        errors = MODULE.validate_report_document(document, self.context)
        self.assertTrue(
            any("READY is not bound" in error for error in errors),
            errors,
        )

    def test_tooling_or_process_hash_tampering_is_rejected(self):
        for path in (
            "tools/compare_gt6_recipes.py",
            "tools/build_component_rules.py",
            "tools/tests/test_compare_gt6_recipes.py",
            "tools/gt6_process_expectations.json",
        ):
            with self.subTest(path=path):
                changed = json.loads(json.dumps(self.document))
                changed["artifact_sha256"][path] = "stale"
                errors = MODULE.validate_report_document(
                    changed,
                    self.context,
                )
                self.assertTrue(
                    any("stale artifact hashes" in error for error in errors),
                    errors,
                )

    def test_ore_closure_summary_cannot_be_hand_edited(self):
        document = json.loads(json.dumps(self.document))
        document["ore_pipeline_acceptance"]["unclassified_count"] = 1

        errors = MODULE.validate_report_document(document, self.context)
        self.assertTrue(
            any("not derived from current artifacts" in error for error in errors),
            errors,
        )


if __name__ == "__main__":
    unittest.main()
