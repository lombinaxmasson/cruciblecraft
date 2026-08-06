from __future__ import annotations

import copy
import json
import unittest

from tools import build_t17_machine_denominator as builder


class T17MachineDenominatorTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.document = builder.build()

    def test_t13_hu_eu_owner_set_is_bidirectionally_classified(self):
        counts = self.document["counts"]
        self.assertEqual(
            "T17_MACHINE_DENOMINATOR_READY", self.document["status"]
        )
        self.assertEqual(28, counts["t17_owner_hu_eu_kinds"])
        self.assertEqual({"EU": 16, "HU": 12}, counts["energy_kinds"])
        self.assertEqual(28, counts["classified"])
        self.assertEqual(0, counts["unclassified"])
        self.assertEqual(2, counts["energy_identities"])
        self.assertEqual(3, counts["selected_kinds"])
        self.assertEqual(1, counts["preimplemented_reference_kinds"])
        self.assertEqual(24, counts["deferred_kinds"])
        self.assertEqual(
            {
                "IMPLEMENTED_T17A": 3,
                "MAPPED_DEFERRED": 3,
                "PREIMPLEMENTED_REFERENCE": 1,
                "T13_ONLY_DEFERRED": 21,
            },
            counts["dispositions"],
        )
        self.assertEqual(
            set(builder.load(builder.POLICY)["kinds"]),
            {row["recipe_map"] for row in self.document["rows"]},
        )

    def test_energy_denominator_preserves_hu_and_eu_identities(self):
        energies = self.document["energy_identities"]
        self.assertEqual({"HU", "EU"}, set(energies))
        self.assertEqual("HEAT", energies["HU"]["local_energy_type"])
        self.assertEqual(
            "HU_ADJACENT_HEAT", energies["HU"]["topology"]
        )
        self.assertEqual("ELECTRIC", energies["EU"]["local_energy_type"])
        self.assertEqual("EU_CABLE_NETWORK", energies["EU"]["topology"])

    def test_selected_heat_tiers_lock_source_materials_windows_and_policy(self):
        rows = {
            row["recipe_map"]: row for row in self.document["rows"]
        }
        selected = {
            recipe_map
            for recipe_map, row in rows.items()
            if row["disposition"] == "IMPLEMENTED_T17A"
        }
        self.assertEqual(
            {"RM.Distillery", "RM.Drying", "RM.Smelter"}, selected
        )
        expected_ids = {
            "RM.Distillery": [20191, 20192, 20193],
            "RM.Drying": [20311, 20312, 20313],
            "RM.Smelter": [20241, 20242, 20243],
        }
        expected_parallel = {
            "RM.Distillery": [8, 16, 32],
            "RM.Drying": [8, 16, 32],
            "RM.Smelter": [1000, 1000, 1000],
        }
        for recipe_map in selected:
            tiers = rows[recipe_map]["source_variants"][:3]
            self.assertEqual(expected_ids[recipe_map], [
                tier["source_id"] for tier in tiers
            ])
            self.assertEqual([1, 2, 3], [
                tier["source_tier"] for tier in tiers
            ])
            self.assertEqual([32, 128, 512], [
                tier["input_nominal"] for tier in tiers
            ])
            self.assertEqual([16, 64, 256], [
                tier["input_minimum"] for tier in tiers
            ])
            self.assertEqual([64, 256, 1024], [
                tier["input_maximum"] for tier in tiers
            ])
            self.assertEqual(["ANY.Steel", "Invar", "Ti"], [
                tier["source_material"] for tier in tiers
            ])
            self.assertEqual(
                [
                    "cruciblecraft:steel",
                    "cruciblecraft:invar",
                    "cruciblecraft:titanium",
                ],
                [
                    tier["material_mapping"]["local_material"]
                    for tier in tiers
                ],
            )
            self.assertEqual(expected_parallel[recipe_map], [
                tier["parallel"] for tier in tiers
            ])
            self.assertTrue(all(
                tier["status"] == "IMPLEMENTED"
                and tier["overclock"] == "CHEAP"
                and tier["parallel_duration"]
                and tier["material_mapping"]["registered"]
                and tier["material_mapping"]["acquisition_blocker"] is None
                for tier in tiers
            ))

    def test_electrolyzer_is_reference_and_higher_tiers_are_deferred(self):
        rows = {
            row["recipe_map"]: row for row in self.document["rows"]
        }
        reference = rows["RM.Electrolyzer"]
        self.assertEqual(
            "PREIMPLEMENTED_REFERENCE", reference["disposition"]
        )
        self.assertEqual(
            [20091, 20092, 20093],
            [tier["source_id"] for tier in reference["source_variants"][:3]],
        )
        self.assertTrue(all(
            tier["status"] == "REFERENCE_IMPLEMENTED"
            for tier in reference["source_variants"][:3]
        ))
        counts = self.document["counts"]
        self.assertEqual(10, counts["heat_tier4_deferred"])
        self.assertEqual(32, counts["electric_tier4_5_deferred"])
        self.assertEqual(42, len(self.document["deferred_source_tiers"]))
        self.assertTrue(all(
            row["reason"]
            and row["replacement_condition"]
            and row["recheck_point"]
            for row in self.document["deferred_source_tiers"]
        ))
        self.assertEqual(
            {"DEFERRED_ELECTRIC_TIER4", "DEFERRED_ELECTRIC_TIER5"},
            {
                tier["status"]
                for tier in reference["source_variants"][3:]
            },
        )

    def test_fixed_hu_rows_remain_explicitly_deferred_not_fake_tiers(self):
        rows = {
            row["recipe_map"]: row for row in self.document["rows"]
        }
        for recipe_map, source_id, material in (
            ("RM.Fermenter", 22003, "MT.StainlessSteel"),
            ("RM.Melter", 22010, "ANY.Iron"),
        ):
            row = rows[recipe_map]
            self.assertEqual("T13_ONLY_DEFERRED", row["disposition"])
            self.assertEqual(1, len(row["source_variants"]))
            variant = row["source_variants"][0]
            self.assertEqual(source_id, variant["source_id"])
            self.assertEqual(material, variant["source_material"])
            self.assertIsNone(variant["source_tier"])
            self.assertIsNone(variant["tier_array"])
            self.assertEqual("DEFERRED", variant["status"])

    def test_catalog_count_and_policy_mutations_fail_closed(self):
        self.assertEqual(
            12,
            self.document["counts"][
                "catalog_variants_selected_and_reference"
            ],
        )
        self.assertEqual(
            33, self.document["counts"]["catalog_variants_total"]
        )
        policy = builder.load(builder.POLICY)
        missing = copy.deepcopy(policy)
        missing["kinds"].pop("RM.Distillery")
        with self.assertRaisesRegex(ValueError, "exactly 28"):
            builder.build(missing)

        wrong_energy = copy.deepcopy(policy)
        wrong_energy["kinds"]["RM.Drying"]["accepted_energy"] = "EU"
        with self.assertRaisesRegex(ValueError, "differs from T13"):
            builder.build(wrong_energy)

        no_exit = copy.deepcopy(policy)
        no_exit["kinds"]["RM.Smelter"]["deferred_tiers"][
            "recheck_point"
        ] = ""
        with self.assertRaisesRegex(ValueError, "missing recheck_point"):
            builder.build(no_exit)

        fake_material = copy.deepcopy(policy)
        fake_material["heat_material_mappings"]["Invar"][
            "registered"
        ] = False
        fake_material["heat_material_mappings"]["Invar"][
            "acquisition_blocker"
        ] = None
        with self.assertRaisesRegex(ValueError, "invalid registered"):
            builder.build(fake_material)

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
