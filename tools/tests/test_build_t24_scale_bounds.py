from __future__ import annotations

import copy
import json
import unittest
from unittest import mock

from tools import build_t24_scale_bounds as builder


class T24ScaleBoundsTest(unittest.TestCase):
    def setUp(self) -> None:
        self.evidence = json.loads(
            builder.EVIDENCE.read_text(encoding="utf-8")
        )
        self.manifest = json.loads(
            builder.MANIFEST.read_text(encoding="utf-8")
        )

    def test_committed_artifact_is_current_and_check_is_read_only(self) -> None:
        on_disk = json.loads(
            builder.OUTPUT.read_text(encoding="utf-8")
        )
        expected = builder.build()
        self.assertEqual(expected, on_disk)
        self.assertEqual([], builder.check())

    def test_status_is_measured_with_three_scenarios(self) -> None:
        document = builder.build()
        self.assertEqual("MEASURED", document["status"])
        self.assertTrue(document["worst_case"])
        self.assertEqual(
            {"small", "target", "stress"}, set(document["scenarios"])
        )

    def test_due_pipes_agree_with_the_manifest_partition(self) -> None:
        document = builder.build()
        for name, row in document["scenarios"].items():
            manifest_due = self.manifest["scenarios"][name]["derived"][
                "pipes_due_per_tick"
            ]
            self.assertEqual(
                manifest_due, row["pipes_due_per_tick_max"], name
            )

    def test_scenario_values_stay_within_declared_caps(self) -> None:
        document = builder.build()
        caps = document["declared_caps"]
        for name, row in document["scenarios"].items():
            self.assertLessEqual(
                row["route_discovery_visited_max"],
                caps["route_discovery_visited"],
                name,
            )
            self.assertLessEqual(
                row["route_cache_entries_per_pipe_max"],
                caps["route_cache_entries_per_item_pipe"],
                name,
            )
            self.assertLessEqual(
                row["cover_configuration_payload_bytes_max"],
                caps["cover_configuration_payload_bytes"],
                name,
            )
            self.assertLessEqual(
                row["structure_validation_ops_max"],
                2 * caps["multiblock_scan_volume"],
                name,
            )

    def test_mutation_zeroing_a_scenario_value_fails_closed(self) -> None:
        evidence = copy.deepcopy(self.evidence)
        evidence["scenarios"]["small"]["pipes_due_per_tick_max"] = 0
        with self._evidence_override(evidence):
            self.assertIsNone(builder.build()["status"])

    def test_mutation_dropping_a_scenario_fails_closed(self) -> None:
        evidence = copy.deepcopy(self.evidence)
        del evidence["scenarios"]["stress"]
        with self._evidence_override(evidence):
            self.assertIsNone(builder.build()["status"])

    def test_mutation_breaking_the_manifest_cross_check_fails_closed(
        self,
    ) -> None:
        evidence = copy.deepcopy(self.evidence)
        evidence["scenarios"]["target"]["pipes_due_per_tick_max"] = 99
        with self._evidence_override(evidence):
            self.assertIsNone(builder.build()["status"])

    def test_mutation_exceeding_a_declared_cap_fails_closed(self) -> None:
        evidence = copy.deepcopy(self.evidence)
        evidence["scenarios"]["stress"]["route_discovery_visited_max"] = 40000
        with self._evidence_override(evidence):
            self.assertIsNone(builder.build()["status"])

    def _evidence_override(self, evidence):
        real = builder._load_if_exists
        return mock.patch.object(
            builder,
            "_load_if_exists",
            side_effect=lambda p, d=None: (
                evidence if p == builder.EVIDENCE else real(p, d)
            ),
        )


if __name__ == "__main__":
    unittest.main()
