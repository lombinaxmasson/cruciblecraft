import importlib.util
import json
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SCRIPT = ROOT / "tools/build_t12_closure_readiness.py"
SPEC = importlib.util.spec_from_file_location(
    "build_t12_closure_readiness", SCRIPT
)
MODULE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(MODULE)


class T12ClosureReadinessTest(unittest.TestCase):
    def test_committed_readiness_is_current(self):
        expected = MODULE.stable(MODULE.build())
        self.assertEqual(
            expected,
            MODULE.OUTPUT.read_text(encoding="utf-8"),
        )

    def test_kind_tier_multiblock_and_load_axes_are_closed(self):
        document = MODULE.build()
        self.assertEqual("T12_READY", document["status"])
        self.assertEqual(9, document["kind_tier"]["variants"])
        self.assertEqual(0, document["kind_tier"]["publication_delta"])
        self.assertEqual(
            ["coke_oven", "large_centrifuge"],
            document["multiblock"]["structures"],
        )
        self.assertEqual(
            ["RU", "KU", "EU"], document["energy"]["identities"]
        )
        self.assertGreaterEqual(
            document["load_gate"]["compression_ratio"], 3.0
        )

    def test_closure_uses_historical_t12a_and_matcher_evidence(self):
        document = MODULE.build()
        evidence = document["t12a_historical_evidence"]
        self.assertEqual(
            "T12A_READY", evidence["status"]
        )
        self.assertEqual(
            "T12A_PREPROJECTION_READY", evidence["load_status"]
        )
        self.assertFalse(evidence["current"])
        self.assertTrue(evidence["immutable"])
        self.assertEqual(
            "tools/t12a_machine_readiness.json", evidence["path"]
        )
        self.assertTrue(document["matcher"]["all_within_budget"])
        self.assertEqual(12, document["matcher"]["presence_supply_cap"])
        self.assertFalse(
            document["matcher"]["historical_t12_claim"]["current"]
        )
        self.assertIn(
            "six runtime supplies",
            document["matcher"]["historical_t12_claim"][
                "superseded_statement"
            ],
        )
        self.assertEqual(
            "tools/t15_matcher_boundary.json",
            document["matcher"]["t15_corrected_authority"]["artifact"],
        )
        self.assertNotIn(
            "large_centrifuge_runtime_supply_count",
            document["matcher"],
        )
        self.assertEqual(
            53, document["runtime_acceptance"]["expected_game_tests"]
        )
        self.assertEqual(
            json.loads(MODULE.T12A.read_text(encoding="utf-8"))[
                "gt6_source"
            ]["revision"],
            document["source_revision"],
        )


if __name__ == "__main__":
    unittest.main()
