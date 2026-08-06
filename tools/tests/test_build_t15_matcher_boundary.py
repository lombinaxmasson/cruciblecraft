from __future__ import annotations

import copy
import json
import unittest

from tools import build_t15_matcher_boundary as builder


class T15MatcherBoundaryTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.document = builder.build()

    def test_committed_boundary_is_current(self):
        self.assertEqual(
            builder.stable(self.document),
            builder.OUTPUT.read_text(encoding="utf-8"),
        )
        self.assertEqual([], builder.check())

    def test_live_structure_derives_fifteen_ports_and_controller(self):
        physical = self.document["physical_structure"]
        self.assertEqual(18, physical["scan_volume"])
        self.assertEqual(15, physical["item_fluid_ports"])
        self.assertEqual(2, physical["energy_input_ports"])
        self.assertEqual(1, physical["controllers"])

        mutated = copy.deepcopy(builder.load(builder.STRUCTURE))
        controller = next(
            row
            for row in mutated["structure"]
            if row["predicate"] == "C"
        )
        controller["predicate"] = "P"
        with self.assertRaises(ValueError):
            builder.parse_structure(mutated)

    def test_live_centrifuge_spec_derives_host_layout(self):
        host = self.document["host_layout"]
        self.assertEqual(
            {
                "item_inputs": 1,
                "item_outputs": 6,
                "item_slots": 7,
                "fluid_inputs": 1,
                "fluid_outputs": 2,
                "fluid_tanks": 3,
            },
            {
                key: host[key]
                for key in (
                    "item_inputs",
                    "item_outputs",
                    "item_slots",
                    "fluid_inputs",
                    "fluid_outputs",
                    "fluid_tanks",
                )
            },
        )
        source = builder.PROCESSING_SPECS.read_text(encoding="utf-8")
        mutated = source.replace(
            '1, 6, 1, 2, 4_000, 8_000,',
            '2, 6, 1, 2, 4_000, 8_000,',
            1,
        )
        with self.assertRaises(ValueError):
            builder.parse_centrifuge_spec(mutated)

    def test_fifteen_physical_ports_share_one_matcher_host(self):
        boundary = self.document["port_host_boundary"]
        self.assertEqual(15, boundary["physical_item_fluid_ports"])
        self.assertEqual(1, boundary["shared_processing_hosts"])
        self.assertEqual(1, boundary["item_matcher_supplies"])
        self.assertEqual(1, boundary["fluid_matcher_supplies"])
        self.assertFalse(boundary["physical_ports_expand_matcher_supplies"])

        matcher = self.document["matcher_boundary"]
        self.assertEqual(12, matcher["presence_item_supply_cap"])
        self.assertEqual(1, matcher["current_item_supply_count"])
        self.assertFalse(matcher["presence_cap_triggered"])
        self.assertFalse(matcher["matcher_rewritten"])

    def test_benchmark_keeps_dense_and_presence_rejection_evidence(self):
        benchmark = self.document["benchmark"]
        self.assertEqual([12, 16, 32, 64], benchmark["dense_supply_counts"])
        self.assertEqual(
            [16, 32, 64],
            benchmark["presence_cap_rejection_supply_counts"],
        )
        self.assertTrue(benchmark["all_within_budget"])
        self.assertFalse(benchmark["matcher_rewritten"])

    def test_expected_sixteen_error_is_audited_not_repeated_as_current(self):
        audit = self.document["historical_correction_audit"]
        self.assertEqual(16, audit["historical_expected_item_fluid_ports"])
        self.assertEqual(
            "SUPERSEDED_INCORRECT_EXPECTATION",
            audit["historical_status"],
        )
        self.assertIn("controller", audit["error"].lower())
        self.assertIn(
            "15 item/fluid + 2 energy + 1 controller",
            audit["correction"],
        )
        self.assertEqual(
            "tools/t15_matcher_boundary.json#physical_structure",
            audit["corrected_authority"],
        )
        encoded = json.dumps(self.document, sort_keys=True)
        self.assertNotIn('"item_fluid_ports": 16', encoded)


if __name__ == "__main__":
    unittest.main()
