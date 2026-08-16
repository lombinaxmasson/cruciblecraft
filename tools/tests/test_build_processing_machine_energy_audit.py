from __future__ import annotations

import copy
import json
import unittest

from tools import build_processing_machine_energy_audit as builder


class ProcessingMachineEnergyAuditTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.document = builder.build()

    def test_all_25_specs_have_explicit_unchanged_energy_arguments(self):
        counts = self.document["counts"]
        self.assertEqual(
            "PROCESSING_MACHINE_ENERGY_AUDIT_READY",
            self.document["status"],
        )
        self.assertEqual(25, counts["machine_specs"])
        self.assertEqual(25, counts["explicit_energy_arguments"])
        self.assertEqual(0, counts["implicit_energy_arguments"])
        self.assertTrue(all(
            row["actual"] == row["expected"]
            for row in self.document["rows"]
        ))
        self.assertEqual(
            {
                "ELECTRIC": 5,
                "HEAT": 3,
                "KINETIC": 8,
                "KINETIC_PUSH": 3,
                "KINETIC_ROTATION": 6,
            },
            counts["energy_types"],
        )

    def test_default_energy_overloads_are_forbidden(self):
        audit = self.document["default_overload_audit"]
        self.assertEqual(
            "DEFAULT_ENERGY_OVERLOAD_TOKEN_FORBIDDEN", audit["policy"]
        )
        self.assertEqual(["mechanical", "t3", "t5"], audit["helpers"])
        self.assertEqual([], audit["forbidden_default_overloads"])
        self.assertEqual(
            {"mechanical": 1, "t3": 1, "t5": 1},
            audit["helper_declarations"],
        )

        source = builder.SOURCE.read_text(encoding="utf-8")
        implicit = source + """
private static ProcessingMachineSpec mechanical(
        String id, Supplier<RecipeMap> map, boolean waterInput) {
    throw new UnsupportedOperationException();
}
"""
        with self.assertRaisesRegex(ValueError, "overload policy failed"):
            builder.build(source=implicit)

    def test_exact_eight_legacy_kinetic_hosts_have_fixed_or_deferred_ledgers(
        self,
    ):
        expected = {
            "assembler",
            "bath",
            "bender",
            "cutter",
            "extruder",
            "mortar",
            "rollbender",
            "welder",
        }
        self.assertEqual(
            expected, set(self.document["allowed_legacy_kinetic_ids"])
        )
        self.assertEqual(8, self.document["counts"]["legacy_kinetic"])
        self.assertEqual(0, self.document["counts"]["new_legacy_kinetic"])
        rows = {
            row["id"]: row for row in self.document["rows"]
            if row["actual"] == "KINETIC"
        }
        self.assertEqual(expected, set(rows))
        self.assertTrue(all(
            row["disposition"] in {"FIXED_UTILITY", "MAPPED_DEFERRED"}
            for row in rows.values()
        ))
        self.assertEqual(
            "MAPPED_DEFERRED", rows["extruder"]["disposition"]
        )
        self.assertEqual(
            "tools/t17_machine_denominator_policy.json",
            rows["extruder"]["ledger"]["path"],
        )

    def test_compressor_is_explicit_electric_cross_owner_debt(self):
        compressor = next(
            row for row in self.document["rows"]
            if row["id"] == "compressor"
        )
        self.assertEqual("ELECTRIC", compressor["actual"])
        self.assertEqual("ELECTRIC", compressor["expected"])
        self.assertEqual(
            "CROSS_OWNER_DEFERRED", compressor["disposition"]
        )
        self.assertEqual(
            "cross_owner_dispositions",
            compressor["ledger"]["section"],
        )
        self.assertEqual("RM.Compressor", compressor["ledger"]["key"])

    def test_policy_or_source_drift_fails_closed(self):
        policy = builder.load(builder.POLICY)
        missing = copy.deepcopy(policy)
        missing["machines"].pop("WELDER")
        with self.assertRaisesRegex(ValueError, "exactly 25"):
            builder.build(missing)

        source = builder.SOURCE.read_text(encoding="utf-8")
        changed = source.replace(
            "4, 1, 3, 2, 32_000, 32_000,\n"
            "                    6, 1, 6, 2,\n"
            "                    EnergyType.ELECTRIC);",
            "4, 1, 3, 2, 32_000, 32_000,\n"
            "                    6, 1, 6, 2,\n"
            "                    EnergyType.KINETIC);",
            1,
        )
        self.assertNotEqual(source, changed)
        with self.assertRaisesRegex(ValueError, "actual energy"):
            builder.build(source=changed)

    def test_t12_closure_cross_check_is_a_forward_edge(self):
        """Forward edge 23 -> 24: the audit proves the T12 closure's recorded
        audit block matches its own counts."""
        key = "tools/t12_closure_readiness.json"
        self.assertIn(key, self.document["currentness"]["ledgers"])
        self.assertEqual(64, len(self.document["currentness"]["ledgers"][key]))
        closure = builder.load(builder.T12_CLOSURE)
        recorded = closure["energy"]["processing_machine_audit"]
        self.assertEqual(
            "PROCESSING_MACHINE_ENERGY_AUDIT_READY",
            recorded["status"],
        )
        for name in (
            "machine_specs",
            "explicit_energy_arguments",
            "implicit_energy_arguments",
            "legacy_kinetic",
            "new_legacy_kinetic",
        ):
            self.assertEqual(
                self.document["counts"][name],
                recorded[name],
                name,
            )

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
