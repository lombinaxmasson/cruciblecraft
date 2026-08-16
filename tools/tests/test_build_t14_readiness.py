import unittest

from tools import build_t14_readiness as readiness


class T14ReadinessTest(unittest.TestCase):
    def test_committed_readiness_is_current(self):
        document = readiness.build()
        self.assertEqual(
            readiness.stable(document),
            readiness.OUTPUT.read_text(encoding="utf-8"),
        )
        self.assertIn(
            "tools/t12_closure_readiness.json",
            document["currentness"],
        )
        self.assertNotIn(
            "tools/t12a_machine_readiness.json",
            document["currentness"],
        )
        self.assertEqual(
            [],
            document["currentness"]["pending_report"]["pending"],
        )
        self.assertTrue(
            {
                "provider",
                "provider_test",
                "material_rule",
                "material_rule_expansion",
            }.issubset(document["source_contracts"])
        )

    def test_o26_is_closed_by_full_field_compact_equivalence(self):
        document = readiness.build()
        o26 = document["o_26"]
        self.assertEqual("CLOSED", o26["status"])
        self.assertEqual(2_782, o26["authored_before"])
        self.assertEqual(20, o26["authored_after"])
        self.assertEqual(2_782, o26["logical_relations"])
        self.assertTrue(o26["full_field_bidirectional_equivalence"])
        self.assertEqual(257, o26["t8_pipe_rows_separate"])
        self.assertEqual(42, o26["skipped_templates"])

    def test_winner_is_recomputed_from_actual_compact_measurements(self):
        document = readiness.build()
        materialization = document["materialization"]
        decision = readiness.load(readiness.DECISION)
        projection = readiness.load(readiness.LOAD_PROJECTION)
        self.assertEqual("hybrid", materialization["winner"])
        self.assertEqual(
            decision["decision"]["production_winner"],
            materialization["winner"],
        )
        self.assertEqual(
            materialization["winner"],
            projection["families"][0]["strategy"],
        )
        self.assertEqual(2, decision["measurement"]["schema_version"])
        self.assertFalse(
            document["measurements"]["superseded_protocol"]["current"]
        )
        self.assertEqual("PRODUCTION_WINNER_READY",
                         materialization["decision_status"])
        self.assertTrue(materialization["client_server_fingerprint_equal"])
        self.assertEqual(
            {
                "logical": 2_782,
                "eager": 557,
                "lazy": 2_225,
                "cache_ceiling": 512,
                "authored": 20,
            },
            materialization["extruder"],
        )

    def test_load_budget_and_projection_have_no_pending_dimensions(self):
        document = readiness.build()
        gate = document["load_gate"]
        self.assertEqual(3_263, gate["datapack_authored_entries"])
        self.assertEqual(18_875, gate["logical_recipes"])
        self.assertEqual(16_650, gate["eager_recipes"])
        self.assertEqual(2_225, gate["lazy_recipes"])
        self.assertEqual([], gate["pending_measurements"])
        self.assertEqual(
            "PASS",
            document["projection_template"]["selected_projection_status"],
        )
        for metric, budget in gate["budgets"].items():
            with self.subTest(metric=metric):
                self.assertIsInstance(budget["soft_budget"], int)
                self.assertIsInstance(budget["hard_ceiling"], int)
                self.assertLessEqual(
                    budget["soft_budget"], budget["hard_ceiling"]
                )

    def test_relation_count_cannot_drift_from_ready_contract(self):
        compact = readiness.load(readiness.COMPACT)
        self.assertEqual(2_782, compact["logical_relation_count"])


if __name__ == "__main__":
    unittest.main()
