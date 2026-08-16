from __future__ import annotations

import copy
import hashlib
import json
import unittest
from unittest import mock

from tools import build_t24_workload_manifest as builder


class T24WorkloadManifestTest(unittest.TestCase):
    def setUp(self) -> None:
        self.policy = json.loads(
            builder.POLICY.read_text(encoding="utf-8")
        )

    def test_committed_artifact_is_current_and_check_is_read_only(self) -> None:
        on_disk = json.loads(
            builder.OUTPUT.read_text(encoding="utf-8")
        )
        expected = builder.build()
        self.assertEqual(expected, on_disk)
        self.assertEqual([], builder.check())

    def test_check_mode_does_not_modify_files(self) -> None:
        before_mtime = builder.OUTPUT.stat().st_mtime_ns
        before_sha = hashlib.sha256(builder.OUTPUT.read_bytes()).hexdigest()
        builder.check()
        self.assertEqual(before_mtime, builder.OUTPUT.stat().st_mtime_ns)
        self.assertEqual(
            before_sha,
            hashlib.sha256(builder.OUTPUT.read_bytes()).hexdigest(),
        )

    def test_three_scenarios_are_partition_exact(self) -> None:
        document = builder.build()
        self.assertEqual("MANIFEST_COMPLETE", document["status"])
        self.assertEqual(
            {"small", "target", "stress"}, set(document["scenarios"])
        )
        for name, row in document["scenarios"].items():
            derived = row["derived"]
            self.assertTrue(derived["pipes_partition_exact"], name)
            self.assertEqual(
                derived["pipes_total"],
                derived["pipes_due_per_tick"] * 5,
                name,
            )

    def test_expected_due_pipes_per_tick(self) -> None:
        document = builder.build()
        due = {
            name: row["derived"]["pipes_due_per_tick"]
            for name, row in document["scenarios"].items()
        }
        self.assertEqual(
            {"small": 4, "target": 100, "stress": 400}, due
        )

    def test_workload_identity_is_stable_across_rebuilds(self) -> None:
        first = builder.build()
        second = builder.build()
        self.assertEqual(first["workload_identity"], second["workload_identity"])
        # Identity covers the scenarios block and excludes currentness.
        expected = hashlib.sha256(
            builder._stable({"scenarios": first["scenarios"]}).encode("utf-8")
        ).hexdigest()
        self.assertEqual(expected, first["workload_identity"])

    def test_workload_identity_changes_when_a_scenario_count_changes(
        self,
    ) -> None:
        baseline = builder.build()["workload_identity"]
        policy = copy.deepcopy(self.policy)
        policy["scenarios"]["target"]["counts"]["fluid_pipes"] = 251
        with self._policy_override(policy):
            mutated = builder.build()
        self.assertNotEqual(baseline, mutated["workload_identity"])
        # 251 + 250 = 501 is not divisible by 5 -> fail-closed status.
        self.assertIsNone(mutated["status"])

    def test_mutation_missing_scenario_fails_closed(self) -> None:
        policy = copy.deepcopy(self.policy)
        del policy["scenarios"]["stress"]
        with self._policy_override(policy):
            self.assertIsNone(builder.build()["status"])

    def test_mutation_inconsistent_converter_pairs_fails_closed(self) -> None:
        policy = copy.deepcopy(self.policy)
        policy["scenarios"]["small"]["composition"]["converter_pairs"][
            "firebox_boiler"
        ] = 2
        with self._policy_override(policy):
            self.assertIsNone(builder.build()["status"])

    def _policy_override(self, policy):
        real = builder._load_if_exists
        return mock.patch.object(
            builder,
            "_load_if_exists",
            side_effect=lambda p, d=None: (
                policy if p == builder.POLICY else real(p, d)
            ),
        )


if __name__ == "__main__":
    unittest.main()
