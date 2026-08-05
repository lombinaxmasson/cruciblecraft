import importlib.util
import json
import re
import sys
import unittest
from pathlib import Path


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
        cls.document = json.loads(
            MODULE.REPORT.read_text(encoding="utf-8")
        )
        cls.context = MODULE.build_validation_context()
        cls.snapshot = cls.context.value("tooling_snapshot")

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
        self.assertEqual(433, snapshot["java_source_test_count"])
        self.assertEqual(252, snapshot["python_test_count"])
        self.assertEqual(47, MODULE.current_game_test_count())
        self.assertEqual(
            snapshot["java_source_test_count"],
            document["tests"]["java_unit_tests"]["tests"],
        )
        plan = (MODULE.ROOT / "CrucibleCraft-总体规划.md").read_text(
            encoding="utf-8"
        )
        match = re.search(r"Java / Python 单测 \| (\d+) / (\d+)", plan)
        self.assertIsNotNone(match)
        self.assertEqual(
            (
                snapshot["java_source_test_count"],
                snapshot["python_test_count"],
            ),
            tuple(map(int, match.groups())),
        )
        ore = document["ore_pipeline_acceptance"]
        self.assertEqual(2117, ore["concrete_recipes"])
        self.assertEqual(494, ore["concrete_recipes_by_map"]["crusher"])
        self.assertEqual(137, ore["high_version_ore_block_recipes"])
        self.assertEqual(
            5, ore["ore_block_crusher_ingress"]["output_count"]
        )
        self.assertTrue(all(ore["game_tests"].values()))
        t4 = document["t4_tool_acceptance"]
        self.assertEqual(21, t4["source_rules"])
        self.assertEqual(11, t4["tool_types"])
        self.assertEqual(3452, t4["expanded_recipes"])
        self.assertEqual(0, t4["unclassified"])
        self.assertEqual(0, t4["signature_collisions"])
        self.assertEqual(1020, t4["eligible_without_route"]["total"])
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
            2811,
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
            605, t7["fact_counts"]["formula_visible_material_count"]
        )
        self.assertEqual(
            347,
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
            25, t8["unobtainable_nonmetal_fluid_pipes"]["form_count"]
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
            "UNIFORM_PLACEHOLDER",
            worldgen["geometry_policy"]["status"],
        )
        self.assertEqual(
            "O-29", worldgen["geometry_policy"]["open_item"]
        )
        self.assertTrue(worldgen["runtime_registry_placement_test"])
        t10 = document["t10_preflight_acceptance"]
        self.assertEqual("T10_READY", t10["status"])
        self.assertTrue(t10["ledger_current"])
        self.assertEqual(
            {"materials": 321, "recipes": 642},
            t10["route_counts"]["hot_ingot"],
        )
        self.assertEqual(
            {"materials": 323, "recipes": 646},
            t10["route_counts"]["multi_ingot"],
        )
        self.assertEqual(
            21000, t10["budget_projection"]["global_budget"]
        )
        self.assertEqual(56, t10["prefix_facts"]["startup_prefix_count"])
        self.assertEqual(1829, t10["prefix_facts"]["handshake_entry_count"])
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
            1288,
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
        self.assertEqual(5958, t10["load_gate"]["datapack_recipe_entries"])
        self.assertEqual(18871, t10["load_gate"]["published_recipes"])
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
