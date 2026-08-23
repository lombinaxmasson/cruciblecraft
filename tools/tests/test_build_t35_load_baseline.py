from __future__ import annotations

import json
import unittest

from tools import build_t35_load_baseline as builder


class T35LoadBaselineTest(unittest.TestCase):
    def test_committed_artifact_is_current_and_check_is_read_only(self) -> None:
        on_disk = json.loads(builder.OUTPUT.read_text(encoding="utf-8"))
        expected = builder.build()
        stripped = lambda document: {
            key: value
            for key, value in document.items()
            if key not in builder.REPORT_OWNED
        }
        self.assertEqual(stripped(expected), stripped(on_disk))
        before = builder.OUTPUT.read_bytes()
        self.assertEqual([], builder.check())
        self.assertEqual(before, builder.OUTPUT.read_bytes())

    def test_t14_axis_count_and_budgets_match_policy(self) -> None:
        document = builder.build()
        self.assertEqual(13, document["t14_axis_count"])
        self.assertEqual(list(builder.T14_AXIS_IDS), document["t14_axis_ids"])
        policy = json.loads(builder.T14.read_text(encoding="utf-8"))
        for axis_id in builder.T14_AXIS_IDS:
            axis = document["axes"][axis_id]
            spec = policy["budgets"][axis_id]
            self.assertEqual(spec["unit"], axis["unit"])
            self.assertEqual(spec["aggregation"], axis["aggregation"])
            self.assertEqual(spec["soft_budget"], axis["soft_budget"])
            self.assertEqual(spec["hard_ceiling"], axis["hard_ceiling"])
            self.assertFalse(axis["zero_filled"])

    def test_opening_uses_t30_t31_chain_and_supersedes_t27_without_averaging(self) -> None:
        document = builder.build()
        delta = json.loads(builder.DELTA.read_text(encoding="utf-8"))
        t27 = json.loads(builder.T27_OPENING.read_text(encoding="utf-8"))
        self.assertEqual(delta["opening_publication"], document["opening_publication"])
        self.assertEqual({"eager": 0, "lazy": 0, "logical": 0}, document["publication_delta"])
        self.assertEqual(18766, document["opening_publication"]["logical"])
        self.assertEqual(16541, document["opening_publication"]["eager"])
        self.assertEqual(2225, document["opening_publication"]["lazy"])
        superseded = document["supersedes"][0]
        self.assertEqual(t27["publication"]["current"], superseded["opening"])
        self.assertEqual(19087, superseded["opening"]["logical"])
        self.assertEqual(16862, superseded["opening"]["eager"])
        averaged = {
            "logical": (19087 + 18766) // 2,
            "eager": (16862 + 16541) // 2,
            "lazy": 2225,
        }
        self.assertNotEqual(document["opening_publication"], averaged)

    def test_logical_publication_is_diagnostic_without_eager_hard_cap(self) -> None:
        document = builder.build()
        logical = document["axes"]["logical_publication_rows"]
        eager = document["axes"]["eager_publication_rows"]
        self.assertTrue(logical["diagnostic"])
        self.assertIsNone(logical["soft_budget"])
        self.assertIsNone(logical["hard_ceiling"])
        self.assertEqual("DIAGNOSTIC_ONLY", logical["verdict"])
        self.assertEqual(21000, eager["hard_ceiling"])
        self.assertNotEqual(logical["hard_ceiling"], eager["hard_ceiling"])

    def test_scale_topology_and_route_axes_are_independent(self) -> None:
        document = builder.build()
        pipes = document["axes"]["topology_pipes_total"]
        visited = document["axes"]["item_route_visited_max"]
        self.assertEqual({"target": 500, "stress": 2000}, pipes["opening_base"])
        self.assertEqual({"target": 250, "stress": 1000}, visited["opening_base"])
        self.assertEqual([], pipes["comparable_to"])
        self.assertEqual([], visited["comparable_to"])
        with self.assertRaises(ValueError):
            builder.assert_comparable(
                "topology_pipes_total",
                "item_route_visited_max",
                document["axes"],
            )

    def test_f005_preserves_historical_closed_without_synthetic_pass(self) -> None:
        document = builder.build()
        scale = json.loads(builder.SCALE.read_text(encoding="utf-8"))
        f005 = document["axes"]["f005_release_scale"]
        self.assertEqual("CLOSED", f005["historical"]["current_status"])
        self.assertEqual(scale["f005"]["current_status"], f005["historical"]["current_status"])
        self.assertEqual("DEFERRED_RELEASE_REMEASUREMENT", f005["canonical_assessment"])
        self.assertEqual("DEFERRED_RELEASE_REMEASUREMENT", f005["verdict"])
        self.assertIsNone(f005["soft_budget"])
        self.assertIsNone(f005["hard_ceiling"])
        for metric in f005["metrics"].values():
            self.assertEqual("DEFERRED_RELEASE_REMEASUREMENT", metric["verdict"])
            self.assertNotEqual("PASS", metric["verdict"])

    def test_incompatible_comparability_is_rejected(self) -> None:
        document = builder.build()
        axes = document["axes"]
        with self.assertRaises(ValueError):
            builder.assert_comparable(
                "topology_pipes_total",
                "item_route_visited_max",
                axes,
            )
        with self.assertRaises(ValueError):
            builder.assert_comparable(
                "sync_bytes",
                "server_reload_ms",
                axes,
            )

    def test_declared_comparable_axes_share_full_measurement_contract(self) -> None:
        axes = builder.build()["axes"]
        for axis_id, axis in axes.items():
            for peer_id in axis["comparable_to"]:
                peer = axes[peer_id]
                self.assertEqual(axis["unit"], peer["unit"], axis_id)
                self.assertEqual(axis["aggregation"], peer["aggregation"], axis_id)
                self.assertEqual(
                    axis["measurement_method"],
                    peer["measurement_method"],
                    axis_id,
                )
                self.assertEqual(axis["scale"], peer["scale"], axis_id)

    def test_missing_f005_heap_measurement_is_not_zero_filled(self) -> None:
        self.assertIsNone(builder._difference_or_none(None, 1))
        self.assertIsNone(builder._difference_or_none(1, None))
        self.assertEqual(3, builder._difference_or_none(5, 2))

    def test_build_is_deterministic(self) -> None:
        first = builder.build()
        second = builder.build()
        self.assertEqual(
            builder.common.stable_json(first),
            builder.common.stable_json(second),
        )


if __name__ == "__main__":
    unittest.main()
