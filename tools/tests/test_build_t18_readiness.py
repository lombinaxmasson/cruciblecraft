from __future__ import annotations

import copy
import json
import unittest

from tools import build_t18_readiness as builder


class T18ReadinessTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.document = builder.build()

    def test_t18a_through_d_complete_with_no_pending(self):
        self.assertEqual("T18_READY", self.document["status"])
        self.assertEqual(
            ["T18a", "T18b", "T18c", "T18d"],
            self.document["completed_stages"],
        )
        self.assertEqual([], self.document["pending_stages"])
        for stage in ("T18a", "T18b", "T18c", "T18d"):
            self.assertEqual(
                "COMPLETE",
                self.document["stage_gates"][stage]["status"],
            )
        self.assertTrue(
            self.document["closure_policy"][
                "final_closure_attempted"
            ]
        )
        self.assertEqual([], self.document["closure_policy"]["pending"])
        self.assertEqual(
            [],
            self.document["currentness"]["pending_report"]["pending"],
        )

    def test_t18a_evidence_locks_profiles_conservation_and_resources(
        self,
    ):
        evidence = self.document["t18a_evidence"]
        self.assertEqual(
            {
                "machine_kinds": 29,
                "selected_kinds": 6,
                "preimplemented_reference_kinds": 3,
                "deferred_kinds": 20,
                "energy_identities": 2,
                "unclassified": 0,
            },
            evidence["denominator"],
        )
        self.assertEqual(
            [
                "MultiTileEntityBoilerTank",
                "MultiTileEntityEngineSteam",
                "MultiTileEntityGeneratorMetal",
            ],
            evidence["selected_t18a_kinds"],
        )
        self.assertEqual(24, evidence["firebox"]["hu_per_tick"])
        self.assertEqual(
            7500, evidence["firebox"]["efficiency_bps"]
        )
        self.assertEqual(
            {"hu": 80, "water_mb": 1, "steam_mb": 160},
            evidence["boiler"],
        )
        self.assertEqual(1302, evidence["steam_engine"]["source_id"])
        self.assertEqual(586, evidence["steam_engine"]["source_line"])
        self.assertEqual(2, evidence["steam_engine"]["steam_per_eu"])
        self.assertEqual(12, evidence["steam_engine"]["ku_packet"])
        conservation = evidence["steam_engine"]["source_conservation"]
        self.assertEqual("SOURCE_BACKED", conservation["classification"])
        self.assertEqual(200, conservation["steamInputMb"])
        self.assertEqual(50, conservation["kuOutput"])
        self.assertEqual(4, conservation["steamMbPerKu"])
        self.assertEqual(
            conservation["steamInputMb"],
            conservation["kuOutput"] * conservation["steamMbPerKu"],
        )
        nominal = evidence["steam_engine"]["source_nominal"]
        self.assertEqual(
            "SOURCE_DERIVED_NOMINAL", nominal["classification"]
        )
        self.assertEqual(
            nominal["mOutputKu"],
            nominal["registeredNumerator"] // nominal["steamPerEu"],
        )
        fixed = evidence["steam_engine"]["fixed_output"]
        self.assertEqual(
            "DESIGN_POLICY_FIXED_OUTPUT", fixed["classification"]
        )
        self.assertEqual(12, fixed["kuPerTick"])
        runtime = evidence["steam_engine"]["gt6_runtime"]
        self.assertEqual("DEFERRED_REPLACEMENT", runtime["classification"])
        self.assertEqual([6, 24], [
            runtime["minimumKuPerTick"],
            runtime["maximumKuPerTick"],
        ])
        self.assertTrue(runtime["replacementCondition"])
        self.assertTrue(runtime["recheckPoint"])
        self.assertEqual(
            [
                "gt6_code/gregtech6/src/main/java/gregtech/loaders/b/"
                "Loader_MultiTileEntities.java:586",
                "gt6_code/gregtech6/src/main/java/gregapi/data/CS.java:240",
                "gt6_code/gregtech6/src/main/java/gregtech/tileentity/"
                "energy/converters/MultiTileEntityEngineSteam.java:"
                "58,62-63,77-80,98-103,121-146",
            ],
            evidence["steam_engine"]["source_evidence_paths"],
        )
        self.assertEqual(5, len(evidence["resources"]))
        self.assertEqual(2, len(evidence["game_tests"]))

    def test_currentness_covers_every_completed_t18a_source(self):
        currentness = self.document["currentness"]
        self.assertEqual(
            {
                "tools/build_t18_readiness.py",
                "tools/t18_readiness_policy.json",
            },
            set(currentness["owned_inputs"]),
        )
        self.assertEqual(
            {"T17", "denominator", "acquisition", "O37"},
            set(currentness["dependencies"]),
        )
        contracts = self.document["source_contracts"]
        self.assertTrue(contracts)
        t18a = {
            owner: row for owner, row in contracts.items()
            if row["stage"] == "T18a"
        }
        self.assertTrue(all(row["sha256"] for row in t18a.values()))
        self.assertEqual(
            set(t18a),
            set(currentness["completed_stage_sources"]["T18a"]),
        )
        t18b = {
            owner: row for owner, row in contracts.items()
            if row["stage"] == "T18b"
        }
        self.assertTrue(all(row["sha256"] for row in t18b.values()))
        self.assertEqual(
            set(t18b),
            set(currentness["completed_stage_sources"]["T18b"]),
        )
        t18c = {
            owner: row for owner, row in contracts.items()
            if row["stage"] == "T18c"
        }
        self.assertTrue(all(row["sha256"] for row in t18c.values()))
        self.assertEqual(
            set(t18c),
            set(currentness["completed_stage_sources"]["T18c"]),
        )
        t18d = {
            owner: row for owner, row in contracts.items()
            if row["stage"] == "T18d"
        }
        self.assertTrue(all(row["sha256"] for row in t18d.values()))
        self.assertEqual(
            set(t18d),
            set(currentness["completed_stage_sources"]["T18d"]),
        )

    def test_t18b_evidence_locks_source_conservation_and_current_identity(self):
        evidence = self.document["t18b_evidence"]
        self.assertEqual(
            {
                "cruciblecraft:bronze_dynamo": 10111,
                "cruciblecraft:bronze_fuel_engine": 9147,
            },
            evidence["selected_source_ids"],
        )
        self.assertEqual(
            [16, 32, 64],
            evidence["dynamo"]["input_window_ru"],
        )
        self.assertEqual(22, evidence["dynamo"]["output_eu"])
        self.assertEqual(10, evidence["dynamo"]["loss_units"])
        self.assertEqual(6875, evidence["dynamo"]["efficiency_bps"])
        self.assertTrue(evidence["dynamo"]["waste_energy"])
        self.assertEqual("RU", evidence["fuel_engine"]["output_identity"])
        self.assertEqual(16, evidence["fuel_engine"]["packet_ru"])
        self.assertEqual(
            512, evidence["fuel_engine"]["fuel_recipe_total_ru"]
        )
        self.assertEqual(
            "KINETIC_ROTATION",
            evidence["fuel_engine"]["current_energy_identity"],
        )
        self.assertEqual(
            "CURRENT_COMPLETE_ONLY",
            evidence["fuel_engine"]["identity_policy"],
        )
        self.assertEqual(
            "QUARANTINE",
            evidence["fuel_engine"]["missing_partial_wrong"],
        )
        self.assertEqual(3, len(evidence["game_tests"]))

    def test_t18c_evidence_locks_hu_conservation_and_o37_receipt(self):
        evidence = self.document["t18c_evidence"]
        self.assertEqual(
            {"cruciblecraft:bronze_gas_generator": 1602},
            evidence["selected_source_ids"],
        )
        gas = evidence["gas_generator"]
        self.assertEqual("FM.Burn", gas["fuel_map"])
        self.assertEqual("HU", gas["output_identity"])
        self.assertEqual(24, gas["maximum_hu_per_tick"])
        self.assertEqual(7_500, gas["efficiency_bps"])
        self.assertEqual(1_536, gas["source_energy_units"])
        self.assertEqual(1_152, gas["generated_hu"])
        self.assertEqual(9, (
            gas["water_exhaust_mb"] + gas["carbon_dioxide_exhaust_mb"]
        ))
        self.assertEqual("UP", gas["energy_output_face"])
        self.assertEqual("HEAT", gas["current_energy_identity"])
        self.assertEqual("CURRENT_COMPLETE_ONLY", gas["identity_policy"])
        self.assertEqual("QUARANTINE", gas["missing_partial_wrong"])
        o37 = evidence["o37"]
        self.assertEqual("O37_CLOSED", o37["status"])
        self.assertEqual("DESIGN_POLICY", o37["resolution"])
        self.assertEqual(0, o37["direct_binding_candidates"])
        self.assertEqual("SOURCE_MATERIAL_LAYER_ONLY",
                         o37["material_9852_role"])
        self.assertEqual(0, o37["publication_delta"])
        self.assertEqual(3, len(evidence["game_tests"]))

    def test_t18d_closes_denominator_acquisition_load_and_o37(self):
        evidence = self.document["t18d_evidence"]
        self.assertEqual("PASS", evidence["status"])
        self.assertEqual(
            {
                "machine_kinds": 29,
                "energy_identities": 2,
                "unclassified": 0,
                "selected_kinds": 6,
                "preimplemented_reference_kinds": 3,
                "deferred_kinds": 20,
            },
            evidence["denominator"],
        )
        self.assertEqual(
            {
                "steam_chain",
                "fuel_to_ru",
                "ru_to_eu",
                "gas_to_hu",
            },
            set(evidence["four_chain_conservation"]),
        )
        self.assertEqual(2, evidence["identity"][
            "current_identity_profiles"
        ])
        self.assertEqual([], evidence["identity"]["pending"])
        acquisition = evidence["acquisition"]
        self.assertEqual(6, acquisition["profiles"])
        self.assertEqual(6, acquisition["vanilla_crafting_recipes"])
        self.assertEqual("BIDIRECTIONAL_CLOSURE",
                         acquisition["resource_closure"])
        self.assertEqual(0, acquisition["gt_recipe_rows_added"])
        self.assertEqual(0, acquisition["unreachable"])
        self.assertEqual([], acquisition["pending"])
        load = evidence["load_projection"]
        self.assertEqual("T18", load["delivery_phase"])
        self.assertEqual("PASS", load["status"])
        self.assertTrue(all(
            value == 0 for value in load["incremental_counts"].values()
        ))
        self.assertTrue(all(
            interval == {"min": 0, "max": 0}
            for interval in load["measurement_intervals"].values()
        ))
        publication = evidence["publication_baseline"]
        self.assertEqual(32, len(publication["recipe_map_ids"]))
        self.assertEqual(
            {
                "logical_rows": 18_875,
                "eager_rows": 16_650,
                "lazy_rows": 2_225,
            },
            publication["publication_totals"],
        )
        self.assertTrue(publication["stable_id_set_equal_to_t17"])
        self.assertEqual(0, publication["publication_delta"])
        self.assertEqual(24, evidence["emi_enumeration"][
            "configured_maps"
        ])
        self.assertTrue(evidence["emi_enumeration"][
            "recipe_enumeration_equal_to_t17"
        ])
        o37 = evidence["o37_publication"]
        self.assertEqual("DESIGN_POLICY", o37["resolution"])
        self.assertEqual(
            "O37_CLOSED_PERMANENT_DESIGN_POLICY", o37["closure"]
        )
        self.assertEqual(0, o37["direct_binding_candidates"])
        self.assertEqual(0, o37["runtime_registrations_added"])
        self.assertEqual(0, o37["gt_recipe_rows_added"])
        self.assertEqual(0, o37["publication_delta"])
        self.assertEqual([], o37["pending"])
        self.assertEqual([], evidence["pending"])

    def test_closure_summary_has_no_pending(self):
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
            self.document["closure_summary"],
        )
        self.assertEqual("PASS", self.document["publication_gate"][
            "status"
        ])
        self.assertEqual(
            "T18D_COMPLETE",
            self.document["resource_acquisition_identity_gate"]["status"],
        )

    def test_minimal_python_currentness_is_wired(self):
        policy = json.loads(
            (builder.TOOLS / "python_test_policy.json").read_text(
                encoding="utf-8"
            )
        )
        rule = next(
            row for row in policy["affected_rules"]
            if "tools/build_t18_*.py" in row["paths"]
        )
        self.assertEqual(
            [
                "test_build_t18_converter_acquisition",
                "test_build_t18_machine_energy_denominator",
                "test_build_t18_o37_identity_projection",
                "test_build_t18_readiness",
                "test_full_verification_report",
                "test_recipe_load_projection",
            ],
            rule["test_modules"],
        )

    def test_stage_mutations_fail_closed(self):
        policy = builder.load(builder.POLICY)
        pending = copy.deepcopy(policy)
        pending["stages"]["T18d"]["status"] = "PENDING"
        for field in (
            "reason",
            "replacement_condition",
            "recheck_point",
        ):
            pending["stages"]["T18d"][field] = "mutation"
        with self.assertRaisesRegex(
            ValueError, "T18 readiness requires T18a-d complete"
        ):
            builder.build(pending)

        incomplete_pending = copy.deepcopy(pending)
        incomplete_pending["stages"]["T18d"]["recheck_point"] = ""
        with self.assertRaisesRegex(
            ValueError, "pending gate lacks recheck_point"
        ):
            builder.build(incomplete_pending)

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
