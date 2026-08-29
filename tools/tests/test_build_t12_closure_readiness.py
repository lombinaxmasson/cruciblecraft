import copy
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
        historical = document["kind_tier"]["historical_vertical_slice"]
        self.assertEqual(9, historical["variants"])
        self.assertEqual(9, len(historical["rows"]))
        observation = document["kind_tier"]["live_catalog_observation"]
        self.assertEqual("T36", observation["owner"])
        self.assertEqual(13, observation["variants"])
        self.assertFalse(observation["included_in_t12_closure_claim"])
        self.assertEqual(
            {
                "cruciblecraft:chromium_electrolyzer",
                "cruciblecraft:titanium_electrolyzer",
                "cruciblecraft:tungstensteel_centrifuge",
                "cruciblecraft:tungstensteel_sifter",
            },
            {row["id"] for row in observation["additional_variants"]},
        )
        self.assertEqual(0, document["kind_tier"]["publication_delta"])
        self.assertEqual(
            ["coke_oven", "distillation_tower", "large_boiler", "large_centrifuge", "large_crucible", "tank_3x3x3"],
            document["multiblock"]["structures"],
        )
        self.assertEqual(
            ["RU", "KU", "EU"], document["energy"]["identities"]
        )
        audit = document["energy"]["processing_machine_audit"]
        self.assertEqual(
            "PROCESSING_MACHINE_ENERGY_AUDIT_READY", audit["status"]
        )
        self.assertEqual(25, audit["machine_specs"])
        self.assertEqual(25, audit["explicit_energy_arguments"])
        self.assertEqual(0, audit["implicit_energy_arguments"])
        self.assertEqual(4, audit["legacy_kinetic"])
        self.assertEqual(0, audit["new_legacy_kinetic"])
        # The audit is written by builder #24, after this closure (#23): the
        # closure records its status/counts but no sha256 (forward edge
        # 23 -> 24 proves equality instead).
        self.assertNotIn("sha256", audit)
        self.assertEqual(
            {"tools/t12a_machine_readiness.json"},
            set(document["currentness"]["dependencies"]),
        )
        self.assertEqual(
            [],
            document["currentness"]["pending_report"]["pending"],
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

    def test_historical_slice_is_contained_by_t36_catalog(self):
        policy = json.loads(MODULE.MACHINE_POLICY.read_text(encoding="utf-8"))
        catalog = json.loads(MODULE.MACHINE_TIERS.read_text(encoding="utf-8"))

        historical = MODULE.historical_vertical_slice(policy)
        observed, by_kind, additions = MODULE.observe_live_catalog(
            catalog,
            historical,
        )

        self.assertEqual(9, len(historical))
        self.assertEqual(9, len(observed))
        self.assertEqual(
            {
                "cruciblecraft:centrifuge": 4,
                "cruciblecraft:sifter": 4,
                "cruciblecraft:electrolyzer": 5,
            },
            by_kind,
        )
        self.assertEqual(
            MODULE.T36_T12_KIND_ADDITIONS,
            {row["id"] for row in additions},
        )

    def test_historical_slice_fails_closed_on_omission_or_semantic_drift(self):
        policy = json.loads(MODULE.MACHINE_POLICY.read_text(encoding="utf-8"))
        catalog = json.loads(MODULE.MACHINE_TIERS.read_text(encoding="utf-8"))
        historical = MODULE.historical_vertical_slice(policy)

        omitted = copy.deepcopy(catalog)
        omitted["variants"] = [
            row
            for row in omitted["variants"]
            if row["id"] != "cruciblecraft:centrifuge"
        ]
        with self.assertRaisesRegex(ValueError, "catalog containment failed"):
            MODULE.observe_live_catalog(omitted, historical)

        drifted = copy.deepcopy(catalog)
        centrifuge = next(
            row
            for row in drifted["variants"]
            if row["id"] == "cruciblecraft:centrifuge"
        )
        centrifuge["energy"] = "KINETIC_PUSH"
        with self.assertRaisesRegex(ValueError, "catalog semantics drifted"):
            MODULE.observe_live_catalog(drifted, historical)


if __name__ == "__main__":
    unittest.main()
