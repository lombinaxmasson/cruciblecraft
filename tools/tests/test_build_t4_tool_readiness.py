import copy
import importlib.util
import json
import sys
import unittest


TOOLS = __import__("pathlib").Path(__file__).resolve().parents[1]
SPEC = importlib.util.spec_from_file_location(
    "build_t4_tool_readiness",
    TOOLS / "build_t4_tool_readiness.py",
)
assert SPEC and SPEC.loader
MODULE = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = MODULE
SPEC.loader.exec_module(MODULE)


class T4ToolReadinessTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.policy = json.loads(MODULE.POLICY.read_text(encoding="utf-8"))
        cls.document = MODULE.build()

    def test_committed_ledger_matches_current_sources_and_policy(self):
        self.assertEqual(
            MODULE.stable_json(self.document),
            MODULE.OUTPUT.read_text(encoding="utf-8"),
        )
        self.assertEqual(2, self.document["schema_version"])
        self.assertEqual("READY_FOR_T4_IMPLEMENTATION", self.document["status"])
        self.assertEqual(0, self.document["closure"]["unclassified"])
        self.assertEqual(546 * 11, self.document["closure"]["classified"])
        self.assertEqual(
            self.document["closure"]["classified"],
            self.document["closure"]["tool_material_pair_count"],
        )

    def test_domain_and_diagnostic_form_counts_are_locked(self):
        facts = self.document["facts"]
        self.assertEqual(546, facts["tool_materials"])
        self.assertEqual({"1": 232, "2": 28, "3": 286}, facts["types"])
        self.assertEqual(285, facts["no_advanced_tools"])
        self.assertEqual(
            {"1": 232, "2": 28, "3": 25},
            facts["types_no_advanced"],
        )
        self.assertEqual(44, facts["durability_positive_without_tool_domain"])
        self.assertEqual(292, facts["forms"]["plate"])
        self.assertEqual(542, facts["forms"]["rod"])
        self.assertEqual(290, facts["plate_and_rod"])
        self.assertEqual(
            {
                "both": 290,
                "neither": 2,
                "plate_without_rod": 2,
                "rod_without_plate": 252,
            },
            facts["plate_rod_quadrants"],
        )
        self.assertEqual(
            ["anti_adamantium", "anti_vibranium"],
            facts["gate_empty_materials"],
        )

    def test_exact_gt6_eligibility_counts_replace_advanced_blanket(self):
        projection = self.document["strategy_projections"]["tool_recipes"]
        expected = {
            "pickaxe": (546, 538, 546, 538),
            "shovel": (546, 538, 546, 538),
            "axe": (546, 538, 546, 538),
            "hoe": (546, 538, 546, 538),
            "sword": (546, 538, 546, 538),
            "smithing_hammer": (536, 407, 412, 397),
            "file": (314, 148, 546, 148),
            "chisel": (314, 309, 546, 309),
            "saw": (314, 309, 546, 309),
            "screwdriver": (314, 309, 546, 309),
            "wrench": (314, 310, 546, 310),
        }
        for tool, counts in expected.items():
            with self.subTest(tool=tool):
                self.assertEqual(
                    counts,
                    (
                        projection[tool]["prefix_eligible"],
                        projection[tool]["listener_eligible"],
                        projection[tool]["full_tool_eligible"],
                        projection[tool]["exact_eligible"],
                    ),
                )
        encoded_rules = MODULE.stable_json(self.policy["tool_rules"])
        self.assertNotIn('"advanced"', encoded_rules)
        self.assertNotIn("NO_ADVANCED_TOOLS", encoded_rules)
        self.assertEqual(
            "diagnostic_only_for_these_11_tools",
            self.policy["material_domain"]["no_advanced_tools_usage"],
        )

    def test_item_eligible_without_route_gap_is_explicitly_budgeted(self):
        gaps = self.document["strategy_projections"][
            "eligibility_route_gaps"
        ]
        expected = {
            "pickaxe": 208,
            "shovel": 126,
            "axe": 209,
            "hoe": 209,
            "sword": 126,
            "smithing_hammer": 80,
            "file": 56,
            "chisel": 2,
            "saw": 2,
            "screwdriver": 0,
            "wrench": 2,
        }
        self.assertEqual(1020, gaps["total"])
        self.assertEqual(
            expected,
            {
                tool: row["count"]
                for tool, row in gaps["by_tool"].items()
            },
        )
        self.assertIn(
            "oak",
            gaps["by_tool"]["pickaxe"]["materials"],
        )
        projection = self.document["strategy_projections"]["tool_recipes"]
        for tool, gap in expected.items():
            with self.subTest(tool=tool):
                self.assertEqual(
                    gap,
                    projection[tool]["eligible_without_route"],
                )
                self.assertEqual(
                    gap,
                    len(
                        projection[tool][
                            "eligible_without_route_materials"
                        ]
                    ),
                )

    def test_every_eligibility_predicate_has_pinned_source_and_reason(self):
        ledger = self.document["strategy_projections"][
            "eligibility_predicate_sources"
        ]
        self.assertEqual(9, ledger["predicate_count"])
        self.assertEqual(
            self.policy["gt6_source"]["revision"],
            ledger["pinned_revision"],
        )
        self.assertEqual(
            {
                "antimatter_exclusion": 2,
                "coated_exclusion_with_wrench_exception": 5,
                "hammer_wood_exclusion": 128,
                "bouncy_exclusion_with_screwdriver_exception": 6,
                "stretchy_exclusion_with_screwdriver_exception": 1,
            },
            ledger["observed_tag_populations"],
        )
        for predicate_id, row in self.policy[
            "eligibility_predicate_sources"
        ].items():
            with self.subTest(predicate=predicate_id):
                self.assertTrue(row["reason"].strip())
                self.assertEqual(
                    self.policy["gt6_source"]["revision"],
                    row["source"]["revision"],
                )

    def test_identity_and_tag_conditions_remain_distinct(self):
        records = self.document["records"]
        self.assertFalse(
            records["wood"]["tool_decisions"]["pickaxe"]["eligible"]
        )
        self.assertTrue(
            records["oak"]["tool_decisions"]["pickaxe"]["eligible"]
        )
        self.assertFalse(
            records["oak"]["tool_decisions"]["smithing_hammer"]["eligible"]
        )
        self.assertFalse(
            records["rubber"]["tool_decisions"]["smithing_hammer"]["eligible"]
        )
        self.assertFalse(
            records["gilded_iron"]["tool_decisions"]["pickaxe"]["eligible"]
        )
        self.assertTrue(
            records["gilded_iron"]["tool_decisions"]["wrench"]["eligible"]
        )
        self.assertTrue(
            records["copper"]["tool_decisions"]["file"]["eligible"]
        )
        self.assertFalse(
            records["diamond"]["tool_decisions"]["file"]["eligible"]
        )
        self.assertFalse(
            records["copper"]["tool_decisions"]["wrench"]["eligible"]
        )
        self.assertTrue(
            records["lead"]["tool_decisions"]["wrench"]["eligible"]
        )

    def test_identity_ledger_counts_literal_ids_not_route_occurrences(self):
        ledger = self.document["strategy_projections"]["identity_ledger"]
        self.assertEqual(
            ["stone", "wood"],
            ledger["distinct_literal_material_ids"],
        )
        self.assertEqual(2, ledger["identity_use_count"])
        self.assertEqual(12, ledger["policy_reference_occurrences"])
        self.assertEqual(
            {"exact_wood_exclusion": 11, "stone_pickaxe_route": 1},
            ledger["policy_references_by_use"],
        )
        self.assertEqual(
            {"stone": 3, "wood": 21},
            ledger["projected_occurrences_by_material_id"],
        )
        self.assertEqual(24, ledger["projected_material_is_occurrences"])
        for use in ledger["entries"].values():
            self.assertTrue(use["reason"])
            self.assertEqual(
                "3703e40308c8c030763fd6297dea8b210d2a77b1",
                use["source"]["revision"],
            )

        bad = copy.deepcopy(self.policy)
        del bad["condition_semantics"]["identity_uses"][
            "exact_wood_exclusion"
        ]["reason"]
        with self.assertRaisesRegex(ValueError, "lacks reason"):
            MODULE.validate_identity_uses(bad)

    def test_gt6_recipe_patterns_record_forms_counts_and_catalysts(self):
        recipes = self.policy["gt6_recipe_facts"]["tools"]
        pickaxe = recipes["pickaxe"]
        self.assertEqual(["PII", "f h"], pickaxe["head_patterns"][0]["rows"])
        self.assertEqual(
            [
                {"form": "plate", "count": 1},
                {"form": "ingot", "count": 2},
            ],
            pickaxe["head_patterns"][0]["material_inputs"],
        )
        self.assertEqual(
            ["file", "smithing_hammer"],
            pickaxe["head_patterns"][0]["catalysts"],
        )
        self.assertEqual(1, pickaxe["full_assembly"]["handle_count"])

        hammer = recipes["smithing_hammer"]
        self.assertEqual(
            [{"form": "ingot", "count": 6}],
            hammer["head_patterns"][0]["material_inputs"],
        )
        file_recipe = recipes["file"]["head_patterns"][0]
        self.assertEqual(["knife"], file_recipe["catalysts"])
        self.assertEqual(
            [{"form": "plate", "count": 2}],
            file_recipe["material_inputs"],
        )
        chisel = recipes["chisel"]["head_patterns"][0]
        self.assertEqual(
            [
                {"form": "plate", "count": 1},
                {"form": "stick", "count": 1},
            ],
            chisel["material_inputs"],
        )
        screwdriver = recipes["screwdriver"]["head_patterns"][0]
        self.assertEqual(
            [{"form": "stick", "count": 2}],
            screwdriver["material_inputs"],
        )
        wrench = recipes["wrench"]
        self.assertEqual(2, len(wrench["head_patterns"]))
        self.assertEqual(2, len(wrench["full_patterns"]))
        self.assertEqual(
            [{"form": "plate", "count": 4}],
            wrench["full_patterns"][0]["material_inputs"],
        )
        self.assertEqual(0, wrench["full_patterns"][0]["handle_count"])

    def test_flattened_patterns_use_actual_per_tool_registered_forms(self):
        expected_inputs = {
            ("pickaxe", "metal"): [
                {"form": "ingot", "count": 2},
                {"form": "plate", "count": 1},
                {"form": "rod", "count": 1},
            ],
            ("shovel", "metal"): [
                {"form": "plate", "count": 1},
                {"form": "rod", "count": 1},
            ],
            ("axe", "metal"): [
                {"form": "ingot", "count": 1},
                {"form": "plate", "count": 2},
                {"form": "rod", "count": 1},
            ],
            ("chisel", "metal"): [
                {"form": "plate", "count": 1},
                {"form": "rod", "count": 2},
            ],
            ("screwdriver", "rod"): [
                {"form": "rod", "count": 3},
            ],
            ("wrench", "metal"): [
                {"form": "plate", "count": 4},
            ],
        }
        for (tool, pattern_id), expected in expected_inputs.items():
            patterns = MODULE.tool_patterns(tool, self.policy)
            pattern = MODULE.pattern_by_id(patterns, pattern_id)
            with self.subTest(tool=tool, pattern=pattern_id):
                self.assertEqual(expected, pattern["material_inputs"])

        records = self.document["records"]
        self.assertEqual(
            "metal",
            records["iron"]["tool_decisions"]["pickaxe"]["pattern"],
        )
        self.assertEqual(
            "gem",
            records["diamond"]["tool_decisions"]["pickaxe"]["pattern"],
        )
        self.assertEqual(
            "stone_rod_exception",
            records["stone"]["tool_decisions"]["pickaxe"]["pattern"],
        )
        self.assertEqual(
            "skipped_missing_registered_forms",
            records["oak"]["tool_decisions"]["pickaxe"]["decision"],
        )
        self.assertEqual(
            "rod",
            records["iron"]["tool_decisions"]["screwdriver"]["pattern"],
        )

    def test_recipe_counts_and_pattern_projections_are_locked(self):
        tools = self.document["strategy_projections"]["tool_recipes"]
        expected = {
            "pickaxe": (330, {"gem": 126, "metal": 203, "stone_rod_exception": 1}),
            "shovel": (412, {"gem": 125, "metal": 287}),
            "axe": (329, {"gem": 126, "metal": 203}),
            "hoe": (329, {"gem": 126, "metal": 203}),
            "sword": (412, {"gem": 125, "metal": 287}),
            "smithing_hammer": (317, {"gem": 124, "metal": 193}),
            "file": (92, {"metal": 92}),
            "chisel": (307, {"gem": 107, "metal": 200}),
            "saw": (307, {"gem": 107, "metal": 200}),
            "screwdriver": (309, {"rod": 309}),
            "wrench": (308, {"gem": 108, "metal": 200}),
        }
        for tool, (total, patterns) in expected.items():
            with self.subTest(tool=tool):
                self.assertEqual(total, tools[tool]["recipe_ready"])
                self.assertEqual(patterns, tools[tool]["patterns"])
        self.assertEqual(
            sum(total for total, _ in expected.values()),
            self.document["strategy_projections"]["recipe_signatures"][
                "projected_recipes"
            ],
        )

    def test_port_strategy_preserves_selector_and_flint_bootstrap(self):
        strategy = self.policy["port_recipe_strategy"]
        self.assertEqual("strategy_not_source_fact", strategy["kind"])
        self.assertEqual(
            "flatten_head_and_handle_into_one_recipe",
            strategy["assembly"],
        )
        self.assertEqual("preserve", strategy["pattern_selector"]["action"])
        self.assertEqual(
            "fixed_flint_knife",
            strategy["catalyst_mapping"]["knife"]["bootstrap"],
        )
        file_inputs = self.document["strategy_projections"]["tool_recipes"][
            "file"
        ]["signature_projection_inputs"][0]
        self.assertEqual(
            {"form": "plate", "count": 2},
            file_inputs["material_inputs"][0],
        )
        self.assertEqual(1, file_inputs["handle_count"])
        self.assertEqual(
            ["cruciblecraft:flint_knife", "cruciblecraft:tool_pattern_file"],
            [row["item"] for row in file_inputs["catalysts"]],
        )

    def test_signature_projection_has_no_cross_tool_shadows(self):
        signatures = self.document["strategy_projections"][
            "recipe_signatures"
        ]
        self.assertEqual(3452, signatures["projected_recipes"])
        self.assertEqual(3452, signatures["distinct_signatures"])
        self.assertEqual(0, signatures["collision_count"])
        self.assertEqual([], signatures["collision_groups"])
        self.assertEqual("preserve", signatures["pattern_selector_action"])
        selectors = {
            catalyst["item"]
            for tool in self.document["strategy_projections"][
                "tool_recipes"
            ].values()
            for pattern in tool["signature_projection_inputs"]
            for catalyst in pattern["catalysts"]
            if catalyst["source_catalyst"] == "tool_pattern_selector"
        }
        self.assertEqual(11, len(selectors))

    def test_durability_and_mining_speed_strategies_still_saturate(self):
        self.assertEqual(1, MODULE.max_damage(0, self.policy))
        self.assertEqual(256, MODULE.max_damage(256, self.policy))
        self.assertEqual(
            2_147_483_647,
            MODULE.max_damage(float("inf"), self.policy),
        )
        self.assertEqual(0.1, MODULE.mining_speed(0, self.policy))
        self.assertEqual(6.0, MODULE.mining_speed(6, self.policy))
        self.assertEqual(20.0, MODULE.mining_speed(1_000_000_000, self.policy))
        projection = self.document["strategy_projections"]["mining_speed"]
        self.assertEqual(20.0, projection["maximum"])
        self.assertEqual(12, projection["source_above_max"])


if __name__ == "__main__":
    unittest.main()
