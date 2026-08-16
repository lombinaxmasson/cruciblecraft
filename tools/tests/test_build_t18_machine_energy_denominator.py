from __future__ import annotations

import copy
import json
import unittest

from tools import build_t18_machine_energy_denominator as builder


class T18MachineEnergyDenominatorTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.document = builder.build()

    def test_t13_t18_owner_set_is_bidirectionally_classified(self):
        counts = self.document["counts"]
        self.assertEqual(
            "T18_MACHINE_ENERGY_DENOMINATOR_READY",
            self.document["status"],
        )
        self.assertEqual(29, counts["t18_owner_machine_kinds"])
        self.assertEqual(29, counts["classified"])
        self.assertEqual(0, counts["unclassified"])
        self.assertEqual(6, counts["selected_kinds"])
        self.assertEqual(6, counts["selected_source_variants"])
        self.assertEqual(3, counts["preimplemented_reference_kinds"])
        self.assertEqual(20, counts["deferred_kinds"])
        self.assertEqual(2, counts["energy_identities"])
        assignment = self.document["bidirectional_assignment"]
        self.assertTrue(assignment["complete"])
        self.assertTrue(assignment["unique"])
        self.assertEqual(
            assignment["source_canonical_keys"],
            assignment["projected_canonical_keys"],
        )

    def test_selected_and_reference_kinds_are_exact(self):
        rows = {
            row["behavior_class"]: row
            for row in self.document["rows"]
        }
        selected = {
            behavior: row["selected_source"]["source_id"]
            for behavior, row in rows.items()
            if row["disposition"] == "SELECTED_T18"
        }
        self.assertEqual(
            {
                "MultiTileEntityGeneratorMetal": 1102,
                "MultiTileEntityBoilerTank": 1202,
                "MultiTileEntityEngineSteam": 1302,
                "MultiTileEntityDynamoElectric": 10111,
                "MultiTileEntityMotorLiquid": 9147,
                "MultiTileEntityGeneratorGas": 1602,
            },
            selected,
        )
        references = {
            behavior
            for behavior, row in rows.items()
            if row["disposition"] == "PREIMPLEMENTED_REFERENCE"
        }
        self.assertEqual(
            {
                "MultiTileEntityAxle",
                "MultiTileEntityGearBox",
                "MultiTileEntityMotorElectric",
            },
            references,
        )

    def test_steam_and_air_identities_remain_distinct(self):
        energies = self.document["energy_identities"]
        self.assertEqual({"STEAM", "AU"}, set(energies))
        self.assertIsNone(energies["STEAM"]["local_energy_type"])
        self.assertEqual(
            "FLUID_STEAM_TRANSPORT", energies["STEAM"]["topology"]
        )
        self.assertEqual("AIR", energies["AU"]["local_energy_type"])
        self.assertEqual("AIR_PRESSURE", energies["AU"]["topology"])

    def test_standard_bronze_steam_row_resolves_legacy_24_conflict(self):
        engine = next(
            row for row in self.document["rows"]
            if row["behavior_class"] == "MultiTileEntityEngineSteam"
        )
        source = engine["selected_source"]
        self.assertEqual(1302, source["source_id"])
        self.assertNotEqual(1300, source["source_id"])
        self.assertEqual("MT.Bronze", source["material_expression"])
        self.assertEqual("24/STEAM_PER_EU", source["output_expression"])
        catalog = builder.load(builder.CONVERTER_CATALOG)
        profile = next(
            row for row in catalog["profiles"]
            if row["id"] == "cruciblecraft:bronze_steam_engine"
        )
        self.assertEqual(2, catalog["source"]["steamPerEu"])
        self.assertEqual(12, profile["outputPacket"]["size"])
        semantics = engine["output_semantics"]
        self.assertEqual(
            "SOURCE_BACKED",
            semantics["conservation"]["classification"],
        )
        self.assertEqual(
            50,
            semantics["conservation"]["steamInputMb"]
            // semantics["conservation"]["steamMbPerKu"],
        )
        self.assertEqual(
            "SOURCE_DERIVED_NOMINAL",
            semantics["sourceNominal"]["classification"],
        )
        self.assertEqual(
            12,
            semantics["sourceNominal"]["registeredNumerator"]
            // semantics["sourceNominal"]["steamPerEu"],
        )
        self.assertEqual(
            "DESIGN_POLICY_FIXED_OUTPUT",
            semantics["fixedOutput"]["classification"],
        )
        self.assertEqual(
            [6, 24],
            [
                semantics["gt6Runtime"]["minimumKuPerTick"],
                semantics["gt6Runtime"]["maximumKuPerTick"],
            ],
        )
        self.assertTrue(semantics["gt6Runtime"]["replacementCondition"])
        self.assertTrue(semantics["gt6Runtime"]["recheckPoint"])
        self.assertEqual(3, len(semantics["sourceEvidencePaths"]))

    def test_every_deferred_kind_has_an_individual_exit_contract(self):
        deferred = [
            row for row in self.document["rows"]
            if row["disposition"] == "DEFERRED_WITH_REASON"
        ]
        self.assertEqual(
            self.document["counts"]["deferred_kinds"], len(deferred)
        )
        for row in deferred:
            with self.subTest(behavior=row["behavior_class"]):
                for field in builder.DEFERRED_FIELDS:
                    self.assertTrue(str(row[field]).strip())

    def test_policy_mutations_fail_closed(self):
        policy = builder.load(builder.POLICY)
        missing = copy.deepcopy(policy)
        missing["kinds"].pop("MultiTileEntitySolarPanelElectric")
        with self.assertRaisesRegex(ValueError, "exactly 29"):
            builder.build(missing)

        wrong_source = copy.deepcopy(policy)
        wrong_source["kinds"]["MultiTileEntityEngineSteam"][
            "selected_source_id"
        ] = 1300
        with self.assertRaisesRegex(ValueError, "profile differs"):
            builder.build(wrong_source)

        no_exit = copy.deepcopy(policy)
        no_exit["kinds"]["MultiTileEntityTurbineSteam"][
            "replacement_condition"
        ] = ""
        with self.assertRaisesRegex(
            ValueError, "lacks replacement_condition"
        ):
            builder.build(no_exit)

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
