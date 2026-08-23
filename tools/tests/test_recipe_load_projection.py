from __future__ import annotations

import copy
import unittest

from tools import recipe_load_projection as projection


FIXTURES = (
    projection.ROOT
    / "tools"
    / "tests"
    / "fixtures"
    / "t14_load_projection_t15_t19.json"
)


def interval(lower: int, upper: int) -> dict[str, int]:
    return {"min": lower, "max": upper}


def family(
    family_id: str,
    canonical_id: str,
    strategy: str,
    *,
    authored: int,
    logical: int,
    eager: int,
    lazy: int,
    cache: int,
    sync: int,
    scale: int,
) -> dict:
    return {
        "family": family_id,
        "canonical_ids": [canonical_id],
        "strategy": strategy,
        "authored_entries": authored,
        "logical_rows": logical,
        "eager_publication_rows": eager,
        "lazy_logical_rows": lazy,
        "lazy_cache_ceiling_rows": cache,
        "sync_bytes": sync,
        "measurement_basis": {
            "kind": "family_specific_measurement",
            "source": f"test family measurement at {logical} logical rows",
            "measured_logical_rows": [logical],
        },
        "measurement_intervals": {
            "server_reload_ms": interval(scale, scale + 1),
            "server_index_ms": interval(2 * scale, 2 * scale + 2),
            "client_reload_ms": interval(3 * scale, 3 * scale + 3),
            "client_index_ms": interval(4 * scale, 4 * scale + 4),
            "retained_memory_bytes": interval(100 * scale, 200 * scale),
            "allocation_bytes": interval(300 * scale, 400 * scale),
            "lookup_p95_ns": interval(100 * scale, 200 * scale),
            "lookup_candidate_count": interval(scale, scale + 1),
        },
    }


def document(*families: dict) -> dict:
    return {
        "schema_version": 1,
        "status": "FAMILY_LOAD_PROJECTION_INPUT",
        "projection_id": "t15/test-ledger",
        "delivery_phase": "T15",
        "families": list(families),
    }


class RecipeLoadProjectionTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.t13 = projection.load(projection.T13_RECIPE_MAPS)
        cls.policy = projection.load(projection.POLICY)
        cls.schema = projection.load(projection.SCHEMA)
        cls.immediate = family(
            "forming/extruder",
            "gt.recipe.extruder",
            "immediate",
            authored=2,
            logical=6,
            eager=6,
            lazy=0,
            cache=0,
            sync=100,
            scale=1,
        )
        cls.on_demand = family(
            "forming/lathe",
            "gt.recipe.lathe",
            "on_demand",
            authored=3,
            logical=9,
            eager=0,
            lazy=9,
            cache=4,
            sync=200,
            scale=2,
        )
        cls.hybrid = family(
            "chemical/mixer",
            "gt.recipe.mixer",
            "hybrid",
            authored=5,
            logical=10,
            eager=4,
            lazy=6,
            cache=3,
            sync=300,
            scale=3,
        )

    def test_canonical_ids_are_t13_in_scope_and_fail_closed(self):
        valid = projection.project(
            document(copy.deepcopy(self.immediate)),
            t13=self.t13,
            policy=self.policy,
            schema=self.schema,
        )
        reference = valid["families"][0]["t13_references"][0]
        self.assertEqual("gt.recipe.extruder", reference["canonical_id"])
        self.assertEqual("in_scope", reference["classification"])

        unknown = document(copy.deepcopy(self.immediate))
        unknown["families"][0]["canonical_ids"] = ["gt.recipe.not-a-map"]
        with self.assertRaises(projection.ProjectionError):
            projection.project(
                unknown,
                t13=self.t13,
                policy=self.policy,
                schema=self.schema,
            )

        deferred = document(copy.deepcopy(self.immediate))
        deferred["families"][0]["canonical_ids"] = ["unnamed"]
        with self.assertRaises(projection.ProjectionError):
            projection.project(
                deferred,
                t13=self.t13,
                policy=self.policy,
                schema=self.schema,
            )

    def test_each_strategy_keeps_eager_and_lazy_ledgers_separate(self):
        result = projection.project(
            document(
                copy.deepcopy(self.immediate),
                copy.deepcopy(self.on_demand),
                copy.deepcopy(self.hybrid),
            ),
            t13=self.t13,
            policy=self.policy,
            schema=self.schema,
        )
        by_strategy = {
            row["strategy"]: row for row in result["families"]
        }
        self.assertEqual(
            (6, 0, 0),
            (
                by_strategy["immediate"]["eager_publication_rows"],
                by_strategy["immediate"]["lazy_logical_rows"],
                by_strategy["immediate"]["lazy_cache_ceiling_rows"],
            ),
        )
        self.assertEqual(
            (0, 9, 4),
            (
                by_strategy["on_demand"]["eager_publication_rows"],
                by_strategy["on_demand"]["lazy_logical_rows"],
                by_strategy["on_demand"]["lazy_cache_ceiling_rows"],
            ),
        )
        self.assertEqual(
            (4, 6, 3),
            (
                by_strategy["hybrid"]["eager_publication_rows"],
                by_strategy["hybrid"]["lazy_logical_rows"],
                by_strategy["hybrid"]["lazy_cache_ceiling_rows"],
            ),
        )

    def test_ledger_sums_counts_and_intervals_without_summing_lookup(self):
        result = projection.project(
            document(
                copy.deepcopy(self.immediate),
                copy.deepcopy(self.on_demand),
                copy.deepcopy(self.hybrid),
            ),
            t13=self.t13,
            policy=self.policy,
            schema=self.schema,
        )
        ledger = result["ledger"]
        self.assertEqual(
            {
                "datapack_authored_entries": 10,
                "logical_rows": 25,
                "eager_publication_rows": 10,
                "lazy_logical_rows": 15,
                "lazy_cache_ceiling_rows": 7,
                "sync_bytes": 600,
            },
            ledger["counts"],
        )
        self.assertEqual(
            {
                "vanilla_datapack_entries": 0,
                "gt_authored_entries": 10,
                "gt_logical_rows": 25,
            },
            ledger["publication_domains"],
        )
        self.assertEqual(
            {"min": 6, "max": 9},
            ledger["measurement_intervals"]["server_reload_ms"],
        )
        self.assertEqual(
            {"min": 300, "max": 600},
            ledger["measurement_intervals"]["lookup_p95_ns"],
        )
        self.assertEqual(
            2.5,
            ledger["ratios"]["authored_to_logical"]["value"],
        )
        self.assertEqual(
            1.0,
            ledger["ratios"]["authored_to_eager"]["value"],
        )

    def test_count_interval_and_duplicate_mutations_fail_closed(self):
        broken_partition = document(copy.deepcopy(self.immediate))
        broken_partition["families"][0]["eager_publication_rows"] = 5
        with self.assertRaises(projection.ProjectionError):
            projection.validate_input(broken_partition, self.t13)

        broken_interval = document(copy.deepcopy(self.immediate))
        broken_interval["families"][0]["measurement_intervals"][
            "server_reload_ms"
        ] = interval(4, 3)
        with self.assertRaises(projection.ProjectionError):
            projection.validate_input(broken_interval, self.t13)

        unmeasured_scale = document(copy.deepcopy(self.immediate))
        unmeasured_scale["families"][0]["measurement_basis"][
            "measured_logical_rows"
        ] = [5]
        with self.assertRaises(projection.ProjectionError):
            projection.validate_input(unmeasured_scale, self.t13)

        duplicate = document(
            copy.deepcopy(self.immediate),
            copy.deepcopy(self.immediate),
        )
        duplicate["families"][1]["family"] = "forming/other-extruder"
        with self.assertRaises(projection.ProjectionError):
            projection.validate_input(duplicate, self.t13)

    def test_ratio_inputs_and_family_extrapolation_are_forbidden(self):
        global_ratio = document(copy.deepcopy(self.immediate))
        global_ratio["global_compression_ratio"] = 3.0
        with self.assertRaises(projection.ProjectionError):
            projection.validate_input(global_ratio, self.t13)

        family_ratio = document(copy.deepcopy(self.immediate))
        family_ratio["families"][0]["compression_ratio"] = 3.0
        with self.assertRaises(projection.ProjectionError):
            projection.validate_input(family_ratio, self.t13)

        explicit = document(copy.deepcopy(self.immediate))
        explicit["families"][0]["logical_rows"] = 7
        explicit["families"][0]["eager_publication_rows"] = 7
        explicit["families"][0]["measurement_basis"][
            "measured_logical_rows"
        ] = [7]
        result = projection.project(
            explicit,
            t13=self.t13,
            policy=self.policy,
            schema=self.schema,
        )
        self.assertEqual(7, result["ledger"]["counts"]["logical_rows"])

    def test_schema_two_keeps_vanilla_entries_out_of_gt_publication(self):
        vanilla = family(
            "t19/vanilla-acquisition",
            "gt.recipe.bath",
            "immediate",
            authored=30,
            logical=0,
            eager=0,
            lazy=0,
            cache=0,
            sync=0,
            scale=0,
        )
        vanilla["canonical_ids"] = []
        vanilla["publication_domain"] = "vanilla_crafting"
        vanilla["vanilla_datapack_entries"] = 30
        vanilla["measurement_basis"] = {
            "kind": "family_specific_measurement",
            "source": (
                "Exact generated T19 vanilla recipe set; GT loader work "
                "remains zero."
            ),
            "measured_logical_rows": [0],
        }
        selected = document(vanilla)
        selected["schema_version"] = 2
        selected["delivery_phase"] = "T19"
        result = projection.project(
            selected,
            t13=self.t13,
            policy=self.policy,
            schema=self.schema,
        )
        self.assertEqual(
            {
                "vanilla_datapack_entries": 30,
                "gt_authored_entries": 0,
                "gt_logical_rows": 0,
            },
            result["ledger"]["publication_domains"],
        )
        self.assertEqual(
            30,
            result["ledger"]["counts"]["datapack_authored_entries"],
        )
        self.assertEqual(0, result["ledger"]["counts"]["logical_rows"])

        mislabeled = copy.deepcopy(selected)
        mislabeled["families"][0]["logical_rows"] = 30
        mislabeled["families"][0]["eager_publication_rows"] = 30
        with self.assertRaises(projection.ProjectionError):
            projection.validate_input(mislabeled, self.t13)

    def test_hard_budget_excess_fails_and_soft_budget_is_distinct(self):
        over = document(copy.deepcopy(self.immediate))
        over["families"][0]["authored_entries"] = 6601
        with self.assertRaises(projection.BudgetExceededError):
            projection.project(
                over,
                t13=self.t13,
                policy=self.policy,
                schema=self.schema,
            )

        warning_policy = copy.deepcopy(self.policy)
        warning_policy["budgets"]["datapack_authored_entries"][
            "soft_budget"
        ] = 1
        warning = projection.project(
            document(copy.deepcopy(self.immediate)),
            t13=self.t13,
            policy=warning_policy,
            schema=self.schema,
        )
        metric = warning["budget_evaluation"]["metrics"][
            "datapack_authored_entries"
        ]
        self.assertEqual("SOFT_BUDGET_EXCEEDED", metric["status"])
        self.assertEqual(1, metric["soft_budget"])
        self.assertEqual(6600, metric["hard_ceiling"])

    def test_policy_records_measured_soft_and_hard_limits(self):
        projection.validate_policy(self.policy)
        budgets = self.policy["budgets"]
        self.assertEqual(
            (6000, 6600),
            (
                budgets["datapack_authored_entries"]["soft_budget"],
                budgets["datapack_authored_entries"]["hard_ceiling"],
            ),
        )
        self.assertEqual(
            (18000, 21000),
            (
                budgets["eager_publication_rows"]["soft_budget"],
                budgets["eager_publication_rows"]["hard_ceiling"],
            ),
        )
        self.assertEqual(
            (5000, 10000),
            (
                budgets["server_reload_ms"]["soft_budget"],
                budgets["server_reload_ms"]["hard_ceiling"],
            ),
        )
        self.assertEqual(
            (500, 1000),
            (
                budgets["server_index_ms"]["soft_budget"],
                budgets["server_index_ms"]["hard_ceiling"],
            ),
        )
        self.assertEqual([], self.policy["pending_measurements"])
        self.assertEqual(
            "T14D_LOAD_BUDGET_POLICY_MEASURED",
            self.policy["status"],
        )
        for metric, budget in budgets.items():
            with self.subTest(metric=metric):
                self.assertIsInstance(budget["soft_budget"], int)
                self.assertIsInstance(budget["hard_ceiling"], int)
                self.assertLessEqual(
                    budget["soft_budget"], budget["hard_ceiling"]
                )
        selected_input = projection.load(
            projection.ROOT
            / "tools"
            / "t14_extruder_load_projection_input.json"
        )
        selected = projection.project(
            selected_input,
            t13=self.t13,
            policy=self.policy,
            schema=self.schema,
        )
        committed = projection.load(
            projection.ROOT
            / "tools"
            / "t14_extruder_load_projection.json"
        )
        self.assertEqual(selected, committed)
        self.assertEqual("PASS", selected["status"])
        self.assertEqual(
            557,
            selected["ledger"]["counts"]["eager_publication_rows"],
        )
        self.assertEqual(
            2_225,
            selected["ledger"]["counts"]["lazy_logical_rows"],
        )

    def test_t15_through_t19_examples_project_no_new_recipes(self):
        fixture_set = projection.load(FIXTURES)
        self.assertEqual(
            ["T15", "T16", "T17", "T18", "T19"],
            [
                fixture["delivery_phase"]
                for fixture in fixture_set["fixtures"]
            ],
        )
        for fixture in fixture_set["fixtures"]:
            result = projection.project(
                fixture,
                t13=self.t13,
                policy=self.policy,
                schema=self.schema,
            )
            self.assertTrue(
                all(value == 0 for value in result["ledger"]["counts"].values())
            )
            self.assertEqual(
                "PASS",
                result["status"],
            )

    def test_committed_t21_projection_is_current_small_immediate_family(self):
        selected_input = projection.load(
            projection.ROOT / "tools" / "t21_load_projection_input.json"
        )
        selected = projection.project(
            selected_input,
            t13=self.t13,
            policy=self.policy,
            schema=self.schema,
        )
        committed = projection.load(
            projection.ROOT / "tools" / "t21_load_projection.json"
        )
        self.assertEqual(selected, committed)
        self.assertEqual("T21", selected["delivery_phase"])
        self.assertEqual("PASS", selected["status"])
        self.assertEqual(
            {
                "datapack_authored_entries": 4,
                "logical_rows": 4,
                "eager_publication_rows": 4,
                "lazy_logical_rows": 0,
                "lazy_cache_ceiling_rows": 0,
                "sync_bytes": 2865,
            },
            selected["ledger"]["counts"],
        )

    def test_committed_t15_projection_is_current_zero_workload(self):
        selected_input = projection.load(
            projection.ROOT / "tools" / "t15_load_projection_input.json"
        )
        selected = projection.project(
            selected_input,
            t13=self.t13,
            policy=self.policy,
            schema=self.schema,
        )
        committed = projection.load(
            projection.ROOT / "tools" / "t15_load_projection.json"
        )
        self.assertEqual(selected, committed)
        self.assertEqual("T15", selected["delivery_phase"])
        self.assertEqual("PASS", selected["status"])
        self.assertTrue(
            all(value == 0 for value in selected["ledger"]["counts"].values())
        )
        self.assertTrue(all(
            interval == {"min": 0, "max": 0}
            for interval in selected["ledger"][
                "measurement_intervals"
            ].values()
        ))

    def test_committed_t16_projection_is_current_zero_workload(self):
        selected_input = projection.load(
            projection.ROOT / "tools" / "t16_load_projection_input.json"
        )
        selected = projection.project(
            selected_input,
            t13=self.t13,
            policy=self.policy,
            schema=self.schema,
        )
        committed = projection.load(
            projection.ROOT / "tools" / "t16_load_projection.json"
        )
        self.assertEqual(selected, committed)
        self.assertEqual("T16", selected["delivery_phase"])
        self.assertEqual("PASS", selected["status"])
        self.assertEqual(
            {
                "datapack_authored_entries": 0,
                "logical_rows": 0,
                "eager_publication_rows": 0,
                "lazy_logical_rows": 0,
                "lazy_cache_ceiling_rows": 0,
                "sync_bytes": 0,
            },
            selected["ledger"]["counts"],
        )
        self.assertTrue(all(
            interval == {"min": 0, "max": 0}
            for interval in selected["ledger"][
                "measurement_intervals"
            ].values()
        ))

    def test_committed_t17_projection_is_current_zero_workload(self):
        selected_input = projection.load(
            projection.ROOT / "tools" / "t17_load_projection_input.json"
        )
        selected = projection.project(
            selected_input,
            t13=self.t13,
            policy=self.policy,
            schema=self.schema,
        )
        committed = projection.load(
            projection.ROOT / "tools" / "t17_load_projection.json"
        )
        self.assertEqual(selected, committed)
        self.assertEqual("T17", selected["delivery_phase"])
        self.assertEqual("PASS", selected["status"])
        self.assertEqual(
            {
                "datapack_authored_entries": 0,
                "logical_rows": 0,
                "eager_publication_rows": 0,
                "lazy_logical_rows": 0,
                "lazy_cache_ceiling_rows": 0,
                "sync_bytes": 0,
            },
            selected["ledger"]["counts"],
        )
        self.assertTrue(all(
            interval == {"min": 0, "max": 0}
            for interval in selected["ledger"][
                "measurement_intervals"
            ].values()
        ))

    def test_committed_t18_projection_is_current_zero_workload(self):
        selected_input = projection.load(
            projection.ROOT / "tools" / "t18_load_projection_input.json"
        )
        selected = projection.project(
            selected_input,
            t13=self.t13,
            policy=self.policy,
            schema=self.schema,
        )
        committed = projection.load(
            projection.ROOT / "tools" / "t18_load_projection.json"
        )
        self.assertEqual(selected, committed)
        self.assertEqual("T18", selected["delivery_phase"])
        self.assertEqual("PASS", selected["status"])
        self.assertEqual(
            {
                "datapack_authored_entries": 0,
                "logical_rows": 0,
                "eager_publication_rows": 0,
                "lazy_logical_rows": 0,
                "lazy_cache_ceiling_rows": 0,
                "sync_bytes": 0,
            },
            selected["ledger"]["counts"],
        )
        self.assertTrue(all(
            interval == {"min": 0, "max": 0}
            for interval in selected["ledger"][
                "measurement_intervals"
            ].values()
        ))

    def test_delivery_phase_t37_is_accepted(self):
        selected_input = projection.load(
            projection.ROOT / "tools" / "t37_load_projection_input.json"
        )
        selected = projection.project(
            selected_input,
            t13=self.t13,
            policy=self.policy,
            schema=self.schema,
        )
        self.assertEqual("T37", selected["delivery_phase"])
        self.assertEqual("t37/assembler-pilot", selected["projection_id"])
        self.assertEqual(50, selected["ledger"]["counts"]["logical_rows"])
        self.assertEqual(50, selected["families"][0]["authored_entries"])
        self.assertIn(
            50,
            selected_input["families"][0]["measurement_basis"]["measured_logical_rows"],
        )
        self.assertEqual("hybrid", selected["families"][0]["strategy"])

        unknown = copy.deepcopy(selected_input)
        unknown["delivery_phase"] = "T99"
        with self.assertRaises(projection.ProjectionError):
            projection.validate_input(unknown, self.t13)

    def test_pending_policy_metrics_are_not_zero_filled(self):
        pending_policy = copy.deepcopy(self.policy)
        pending_policy["status"] = "T14D_LOAD_BUDGET_POLICY_PENDING_T14C"
        pending_axes = [
            "lazy_cache_ceiling_rows",
            "sync_bytes",
            "server_reload_ms",
            "server_index_ms",
            "client_reload_ms",
            "client_index_ms",
            "retained_memory_bytes",
            "allocation_bytes",
            "lookup_p95_ns",
            "lookup_candidate_count",
        ]
        for metric in pending_axes:
            pending_policy["budgets"][metric]["soft_budget"] = projection.PENDING
            pending_policy["budgets"][metric]["hard_ceiling"] = projection.PENDING
        pending_policy["pending_measurements"] = sorted(pending_axes)

        selected_input = projection.load(
            projection.ROOT / "tools" / "t37_load_projection_input.json"
        )
        result = projection.project(
            selected_input,
            t13=self.t13,
            policy=pending_policy,
            schema=self.schema,
        )
        self.assertEqual("BLOCKED_PENDING_MEASUREMENT", result["status"])
        for metric in pending_axes:
            evaluation = result["budget_evaluation"]["metrics"][metric]
            self.assertEqual(projection.PENDING, evaluation["status"])
            self.assertEqual(projection.PENDING, evaluation["soft_budget"])
            self.assertEqual(projection.PENDING, evaluation["hard_ceiling"])
            self.assertNotEqual("PASS", evaluation["status"])
        self.assertEqual(
            [
                metric
                for metric in projection.BUDGET_CONTRACT
                if metric in pending_axes
            ],
            result["budget_evaluation"]["pending_measurements"],
        )
        self.assertEqual(50, result["ledger"]["counts"]["logical_rows"])
        self.assertEqual(14, result["ledger"]["counts"]["eager_publication_rows"])


if __name__ == "__main__":
    unittest.main()
