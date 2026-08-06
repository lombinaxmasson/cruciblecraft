from __future__ import annotations

import copy
import json
import unittest

from tools import build_t16_machine_denominator as builder


class T16MachineDenominatorTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.document = builder.build()

    def test_t13_ru_ku_owner_set_is_bidirectionally_classified(self):
        counts = self.document["counts"]
        self.assertEqual("T16_MACHINE_DENOMINATOR_READY",
                         self.document["status"])
        self.assertEqual(20, counts["t16_owner_ru_ku_kinds"])
        self.assertEqual(20, counts["classified"])
        self.assertEqual(0, counts["unclassified"])
        self.assertEqual(2, counts["energy_identities"])
        self.assertEqual(
            {
                "IMPLEMENTED_PRE_T16": 2,
                "IMPLEMENTED_T16A": 4,
                "IMPLEMENTED_T16B": 1,
                "MAPPED_DEFERRED": 6,
                "T13_ONLY_DEFERRED": 7,
            },
            counts["dispositions"],
        )
        self.assertEqual(5, counts["selected_kinds"])
        self.assertEqual(2, counts["preimplemented_kinds"])
        self.assertEqual(13, counts["deferred_kinds"])
        self.assertEqual(
            set(builder.load(builder.POLICY)["kinds"]),
            {row["recipe_map"] for row in self.document["rows"]},
        )

    def test_energy_denominator_preserves_ru_and_ku_identities(self):
        energies = self.document["energy_identities"]
        self.assertEqual({"RU", "KU"}, set(energies))
        self.assertEqual(
            "KINETIC_ROTATION", energies["RU"]["local_energy_type"]
        )
        self.assertEqual("RU_AXLE_GEARBOX", energies["RU"]["topology"])
        self.assertEqual("KINETIC_PUSH", energies["KU"]["local_energy_type"])
        self.assertEqual("KU_ADJACENT_PUSH", energies["KU"]["topology"])

    def test_pre_t16_and_selected_catalog_rows_cover_exact_source_tiers(self):
        rows = {
            row["recipe_map"]: row for row in self.document["rows"]
        }
        self.assertEqual(21, self.document["counts"][
            "catalog_variants_pre_t16_and_selected"
        ])
        selected = {
            recipe_map
            for recipe_map, row in rows.items()
            if row["disposition"] == "IMPLEMENTED_T16A"
        }
        self.assertEqual(
            {"RM.Lathe", "RM.RollingMill", "RM.Shredder", "RM.Wiremill"},
            selected,
        )
        for recipe_map in selected:
            row = rows[recipe_map]
            self.assertEqual("RU", row["accepted_energy"])
            self.assertEqual("KINETIC_ROTATION", row["local_energy_type"])
            self.assertEqual([1, 2, 3], [
                tier["source_tier"] for tier in row["tiers_1_3"]
            ])
            self.assertTrue(all(
                tier["status"] == "IMPLEMENTED"
                and tier["parallel"] == 1
                and tier["overclock"] == "STANDARD"
                and not tier["parallel_duration"]
                for tier in row["tiers_1_3"]
            ))

    def test_every_deferred_kind_and_tier4_has_exit_conditions(self):
        self.assertEqual(20, self.document["counts"]["gt6_tier4_deferred"])
        self.assertEqual(20, len(self.document["gt6_tier4_deferred"]))
        for row in self.document["rows"]:
            for field in ("reason", "replacement_condition", "recheck_point"):
                self.assertTrue(row[field], f"{row['recipe_map']} {field}")
                self.assertTrue(
                    row["tier_4"][field],
                    f"{row['recipe_map']} tier4 {field}",
                )
            self.assertEqual(
                "DEFERRED_GT6_TIER4", row["tier_4"]["status"]
            )
    def test_t16b_locks_selected_preimplemented_deferred_and_source_hash(self):
        rows = {
            row["recipe_map"]: row for row in self.document["rows"]
        }
        selected = {
            recipe_map for recipe_map, row in rows.items()
            if row["disposition"] in {
                "IMPLEMENTED_T16A", "IMPLEMENTED_T16B"
            }
        }
        preimplemented = {
            recipe_map for recipe_map, row in rows.items()
            if row["disposition"] == "IMPLEMENTED_PRE_T16"
        }
        deferred = {
            recipe_map for recipe_map, row in rows.items()
            if row["disposition"] in {
                "MAPPED_DEFERRED", "T13_ONLY_DEFERRED"
            }
        }
        self.assertEqual(
            {
                "RM.Lathe",
                "RM.Press",
                "RM.RollingMill",
                "RM.Shredder",
                "RM.Wiremill",
            },
            selected,
        )
        self.assertEqual({"RM.Centrifuge", "RM.Sifting"}, preimplemented)
        self.assertEqual(
            {
                "RM.BurnMixer",
                "RM.ClusterMill",
                "RM.Compressor",
                "RM.Crusher",
                "RM.Cutter",
                "RM.Loom",
                "RM.Mixer",
                "RM.PressureWasher",
                "RM.RollBender",
                "RM.RollFormer",
                "RM.Sharpening",
                "RM.Sluice",
                "RM.Squeezer",
            },
            deferred,
        )
        self.assertEqual(builder.SOURCE_REVISION,
                         self.document["source_revision"])
        self.assertRegex(self.document["source_revision"], r"^[0-9a-f]{40}$")

        press = rows["RM.Press"]
        self.assertEqual("IMPLEMENTED_T16B", press["disposition"])
        self.assertEqual("KINETIC_PUSH", press["local_energy_type"])
        self.assertEqual("cruciblecraft:press", press["catalog_kind"])
        self.assertEqual([20231, 20232, 20233], [
            tier["source_id"] for tier in press["tiers_1_3"]
        ])
        self.assertEqual([4, 8, 16], [
            tier["parallel"] for tier in press["tiers_1_3"]
        ])
        self.assertTrue(all(
            tier["status"] == "IMPLEMENTED"
            and tier["overclock"] == "STANDARD"
            and tier["parallel_duration"]
            for tier in press["tiers_1_3"]
        ))

    def test_policy_mutations_fail_closed(self):
        policy = builder.load(builder.POLICY)
        missing = copy.deepcopy(policy)
        missing["kinds"].pop("RM.Lathe")
        with self.assertRaisesRegex(ValueError, "exactly 20"):
            builder.build(missing)

        wrong_energy = copy.deepcopy(policy)
        wrong_energy["kinds"]["RM.Lathe"]["accepted_energy"] = "KU"
        with self.assertRaisesRegex(ValueError, "differs from T13"):
            builder.build(wrong_energy)

        no_exit = copy.deepcopy(policy)
        no_exit["kinds"]["RM.Wiremill"]["tier4"]["recheck_point"] = ""
        with self.assertRaisesRegex(ValueError, "missing recheck_point"):
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
