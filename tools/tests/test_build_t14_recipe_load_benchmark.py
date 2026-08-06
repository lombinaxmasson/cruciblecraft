from __future__ import annotations

import copy
import unittest

from tools import build_t14_recipe_load_benchmark as builder


class T14RecipeLoadBenchmarkTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.policy = builder.load(builder.POLICY)
        cls.artifact = builder.load(builder.OUTPUT)
        cls.measurement = cls.artifact["measurement"]

    def test_artifact_is_current_and_recomputable(self):
        builder.validate_artifact(self.artifact)
        self.assertEqual([], builder.check())
        self.assertEqual(
            self.artifact["decision"],
            builder.derive_decision(self.measurement, self.policy),
        )

    def test_all_scales_and_candidate_raw_measurements_are_retained(self):
        self.assertEqual(
            ["baseline", "1x", "5x", "20x"],
            [
                scenario["scale"]
                for scenario in self.measurement["scenarios"]
            ],
        )
        self.assertEqual(
            [2782, 2782, 13910, 55640],
            [
                scenario["relation_count"]
                for scenario in self.measurement["scenarios"]
            ],
        )
        for scenario in self.measurement["scenarios"]:
            self.assertEqual(
                ["immediate", "on_demand", "hybrid"],
                [
                    row["candidate"]
                    for row in scenario["candidates"]
                ],
            )
            for row in scenario["candidates"]:
                self.assertIn("raw_samples_ns", row["server"]["reload"])
                self.assertIn("raw_samples_ns", row["lookup"])
                self.assertEqual(
                    list(builder.LOOKUP_TRACES),
                    [
                        trace["trace"]
                        for trace in row["lookup"]["traces"]
                    ],
                )
                for trace in row["lookup"]["traces"]:
                    self.assertIn("raw_samples", trace["candidates"])
                    self.assertIn("hits", trace["cache"])
                    self.assertIn("misses", trace["cache"])
                    self.assertIn("materializations", trace["cache"])
                    self.assertIn("evictions", trace["cache"])

    def test_retained_memory_never_uses_heap_delta_or_zero_pass(self):
        for scenario in self.measurement["scenarios"]:
            for row in scenario["candidates"]:
                retained = row["retained_memory"]
                self.assertFalse(retained["naive_heap_delta_used"])
                if retained["status"] == "PASS":
                    self.assertGreater(
                        retained["total_bytes"]["p50_bytes"], 0
                    )
                    self.assertGreaterEqual(retained["samples"], 2)
                    for sample in retained["sample_metadata"]:
                        self.assertIn(
                            "GC.class_histogram",
                            sample["histogram_command"],
                        )
                        self.assertGreater(
                            sample["loaded_classes_at_ready"], 0
                        )
                        self.assertLessEqual(
                            sample["cache_size"],
                            sample["cache_ceiling"],
                        )
                else:
                    self.assertEqual("SKIP", retained["status"])
                    self.assertEqual(0, retained["samples"])
                    self.assertIsNone(retained["total_bytes"])
                    self.assertTrue(retained["reason"])

    def test_production_winner_is_derived_from_actual_t14a_compact_data(self):
        decision = self.artifact["decision"]
        self.assertEqual(
            ["hybrid", "immediate", "on_demand"],
            decision["algorithm"]["tie_preference"],
        )
        self.assertIn(
            decision["production_winner"],
            ["immediate", "on_demand", "hybrid"],
        )
        self.assertFalse(decision["production_blockers"])
        self.assertTrue(
            self.measurement["input"]["t14a_compact_data_present"]
        )
        self.assertEqual("t14a_compact_relations",
                         self.measurement["input"]["kind"])
        self.assertEqual(
            self.policy["input"]["compact_sha256"],
            self.measurement["input"]["compact_sha256"],
        )
        self.assertEqual(
            self.policy["input"]["compact_fingerprint"],
            self.measurement["input"]["compact_fingerprint"],
        )
        self.assertEqual(
            [1, 1, 5, 20],
            [
                scenario["actual_distribution_copies"]
                for scenario in self.measurement["scenarios"]
            ],
        )

    def test_timing_summary_mutation_fails_closed(self):
        mutated = copy.deepcopy(self.measurement)
        lookup = mutated["scenarios"][0]["candidates"][0]["lookup"]
        lookup["p95_ns"] += 1
        with self.assertRaises(ValueError):
            builder.validate_measurement(mutated, self.policy)

    def test_zero_retained_pass_mutation_fails_closed(self):
        mutated = copy.deepcopy(self.measurement)
        retained = mutated["scenarios"][0]["candidates"][0][
            "retained_memory"
        ]
        retained["status"] = "PASS"
        retained["samples"] = self.policy["workload"][
            "retained_child_samples"
        ]
        retained["total_bytes"] = {
            "status": "PASS",
            "samples": 2,
            "raw_samples_bytes": [0, 0],
            "p50_bytes": 0,
            "min_bytes": 0,
            "max_bytes": 0,
            "error_bar": {
                "lower_bytes": 0,
                "upper_bytes": 0,
            },
        }
        with self.assertRaises(ValueError):
            builder.validate_measurement(mutated, self.policy)

    def test_epoch_gate_mutation_eliminates_candidate(self):
        mutated = copy.deepcopy(self.measurement)
        twenty_x = next(
            row for row in mutated["scenarios"]
            if row["scale"] == "20x"
        )
        immediate = next(
            row for row in twenty_x["candidates"]
            if row["candidate"] == "immediate"
        )
        immediate["gates"]["epoch_invalidation"] = False
        immediate["gates"]["all_pass"] = False
        decision = builder.derive_decision(mutated, self.policy)
        self.assertIn("immediate", decision["eliminated"])
        self.assertIn(
            "gate_failed:epoch_invalidation",
            decision["eliminated"]["immediate"],
        )

    def test_cache_and_trace_mutations_fail_closed(self):
        mutated = copy.deepcopy(self.measurement)
        trace = mutated["scenarios"][0]["candidates"][2]["lookup"][
            "traces"
        ][0]
        trace["cache"]["evictions"] += 1
        with self.assertRaises(ValueError):
            builder.validate_measurement(mutated, self.policy)

        mutated = copy.deepcopy(self.measurement)
        traces = mutated["scenarios"][0]["candidates"][0]["lookup"][
            "traces"
        ]
        traces[0], traces[1] = traces[1], traces[0]
        with self.assertRaises(ValueError):
            builder.validate_measurement(mutated, self.policy)

    def test_policy_and_currentness_mutations_fail_closed(self):
        changed_policy = copy.deepcopy(self.policy)
        changed_policy["hard_limits_20x"][
            "lookup_p95_ns"
        ] = 0
        with self.assertRaises(ValueError):
            builder.validate_policy(changed_policy)

        stale_artifact = copy.deepcopy(self.artifact)
        stale_artifact["currentness"]["measurement_sha256"] = "0" * 64
        with self.assertRaises(ValueError):
            builder.validate_artifact(stale_artifact)

        stale_measurement = copy.deepcopy(self.measurement)
        first_path = next(
            iter(stale_measurement["java_sources"]["files"])
        )
        stale_measurement["java_sources"]["files"][first_path] = "0" * 64
        with self.assertRaises(ValueError):
            builder.validate_measurement(stale_measurement, self.policy)


if __name__ == "__main__":
    unittest.main()
