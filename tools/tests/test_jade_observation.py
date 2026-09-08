#!/usr/bin/env python3
"""Jade observation matrix contract tests."""
from __future__ import annotations

import unittest

from tools import jade_observation as jade


class JadeObservationTest(unittest.TestCase):
    def test_matrix_records_foundation_plus_followup_before_slice(self) -> None:
        manifest = jade.compile_manifest()
        self.assertEqual([], manifest["errors"], manifest["errors"])
        self.assertEqual("PASS", manifest["status"])
        self.assertEqual("foundation_plus_followup", manifest["jade_scope_decision"])
        self.assertGreaterEqual(manifest["family_count"], 18)

    def test_crucible_and_transformer_are_the_ready_slice(self) -> None:
        matrix = jade.load_json(jade.MATRIX)
        by_name = {row["family"]: row for row in matrix["families"]}
        self.assertEqual("ready", by_name["crucible"]["status"])
        self.assertEqual("ready", by_name["transformer"]["status"])
        self.assertIn("temperature_k", by_name["crucible"]["display_sections"])
        self.assertIn("per_side_voltage", by_name["transformer"]["display_sections"])
        follow = [
            row["family"]
            for row in matrix["families"]
            if row["status"] == "follow_up"
        ]
        self.assertEqual("ready", by_name["reactor_core"]["status"])
        self.assertEqual("ready", by_name["battery"]["status"])
        self.assertEqual("ready", by_name["converter_dynamo"]["status"])
        self.assertIn("fusion_reactor", follow)
        self.assertIn("logistics_core", follow)


if __name__ == "__main__":
    unittest.main()
