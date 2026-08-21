from __future__ import annotations

import json
import unittest

from tools import build_t29_crucible_source_evidence as builder
from tools import t27_common as common


class T29CrucibleSourceEvidenceTest(unittest.TestCase):
    def test_committed_artifact_is_current_and_check_is_read_only(self) -> None:
        on_disk = json.loads(builder.OUTPUT.read_text(encoding="utf-8"))
        expected = builder.build()
        self.assertEqual(expected, on_disk)
        before = builder.OUTPUT.read_bytes()
        self.assertEqual([], builder.check())
        self.assertEqual(before, builder.OUTPUT.read_bytes())

    def test_structure_is_27_and_rejects_28(self) -> None:
        document = builder.build()
        structure = document["structure"]
        self.assertEqual("T29_CRUCIBLE_SOURCE_EVIDENCE", document["status"])
        self.assertEqual(common.SOURCE_REVISION, document["source"]["revision"])
        self.assertEqual(27, structure["positions"])
        self.assertEqual([0, 0, 0], structure["controller"])
        self.assertEqual(24, structure["walls"])
        self.assertEqual(2, structure["air"])
        self.assertEqual(1 + 24 + 2, structure["positions"])
        self.assertIn(28, structure["rejected_counts"])
        self.assertNotEqual(28, structure["positions"])
        self.assertEqual(
            [[0, 1, 0], [0, 2, 0]],
            structure["air_offsets"],
        )
        self.assertTrue(structure["second_loop_is_meta_alternate"])
        self.assertEqual(24, len(structure["wall_offsets"]))
        for offset in structure["wall_offsets"]:
            self.assertNotEqual([0, offset[1], 0], offset)

    def test_capacity_is_432_and_single_block_stays_8(self) -> None:
        document = builder.build()
        capacity = document["capacity"]
        self.assertEqual(432, capacity["ingot_units"])
        self.assertEqual([16, 3, 3, 3], capacity["factors"])
        self.assertEqual(16 * 3 * 3 * 3, capacity["ingot_units"])
        self.assertEqual(8, capacity["cc_single_block_ingot_units"])
        self.assertEqual(100, document["energy"]["kg_per_energy"])
        self.assertEqual(1.10, document["energy"]["heat_resistance_bonus"])

    def test_layer_roles_and_ku_air_are_source_facts(self) -> None:
        document = builder.build()
        layers = {row["y"]: row for row in document["layers"]}
        self.assertEqual("ONLY_ENERGY_IN", layers[0]["gt6_mask"])
        self.assertEqual("energy_input", layers[0]["cc_port"])
        self.assertEqual("ONLY_CRUCIBLE", layers[1]["gt6_mask"])
        self.assertIsNone(layers[1]["cc_port"])
        self.assertEqual("ONLY_ITEM_FLUID", layers[2]["gt6_mask"])
        self.assertEqual("item_fluid", layers[2]["cc_port"])
        self.assertEqual("SOURCE_DERIVED", document["energy"]["ku_to_air"]["fidelity"])
        self.assertTrue(document["fidelity_layering"]["not_a_processing_host"])
        self.assertEqual("DESIGN_POLICY", document["molds"]["fidelity"])
        self.assertEqual("SOURCE_BACKED", document["fidelity_layering"]["structure_27"])

    def test_t23_28_is_an_error_to_correct(self) -> None:
        document = builder.build()
        error = document["t23_error"]
        self.assertEqual(28, error["erroneous_positions"])
        self.assertEqual(27, error["correction_positions"])
        self.assertIn(error["status"], {"pending_correction", "corrected"})
        if error["status"] == "pending_correction":
            self.assertEqual(28, error["current_positions"])
            self.assertIn("26 walls + 2 AIR", error["current_notes"])
        else:
            self.assertEqual(27, error["current_positions"])
            self.assertNotIn("26 walls + 2 AIR", error["current_notes"])


if __name__ == "__main__":
    unittest.main()
