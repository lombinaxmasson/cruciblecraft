"""Tests for the T22.5 A1 nine-map shape analysis builder."""
from __future__ import annotations

import json
import sys
import unittest
from pathlib import Path

TOOLS = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(TOOLS))

import build_t22_5_shape_analysis as builder  # noqa: E402


class T225ShapeAnalysisTest(unittest.TestCase):

    def test_committed_artifact_is_current(self) -> None:
        if not builder.OUTPUT.is_file():
            self.skipTest("artifact not yet generated")
        self.assertEqual(builder.check(), [])

    def test_membership_is_a_total_bijection(self) -> None:
        if not builder.OUTPUT.is_file():
            self.skipTest("artifact not yet generated")
        document = json.loads(
            builder.OUTPUT.read_text(encoding="utf-8")
        )
        for row in document["per_map"]:
            self.assertEqual(row["membership"]["unassigned"], 0)
            self.assertEqual(row["membership"]["duplicate"], 0)
            self.assertEqual(
                row["membership"]["total"], row["enabled_rows"]
            )
            self.assertEqual(
                row["membership"]["total"],
                row["expanded_recipe_count"],
            )

    def test_maps_and_unit_names_are_pinned(self) -> None:
        document = json.loads(
            builder.OUTPUT.read_text(encoding="utf-8")
        )
        self.assertEqual(
            [row["map"] for row in document["per_map"]],
            list(builder.TARGET_MAPS),
        )
        for row in document["per_map"]:
            self.assertEqual(row["publication_delta"], 0)

    def test_bath_is_matrix_generated(self) -> None:
        document = json.loads(
            builder.OUTPUT.read_text(encoding="utf-8")
        )
        bath = next(
            row for row in document["per_map"]
            if row["map"] == "gt.recipe.bath"
        )
        # The T22.5 hypothesis: bath at 1.00 rows/unit is wrong; it is
        # matrix-generated with a large compression ratio.
        self.assertGreater(bath["rows_per_unit"], 10)
        self.assertGreater(bath["largest_family_expanded_count"], 500)

    def test_singletons_plus_combinatorials_tile_every_map(self) -> None:
        document = json.loads(
            builder.OUTPUT.read_text(encoding="utf-8")
        )
        for row in document["per_map"]:
            self.assertEqual(
                row["singleton_template_count"]
                + row["combinatorial_template_count"],
                row["template_count"],
            )

    def test_check_is_reference_only_without_dump(self) -> None:
        document = json.loads(
            builder.OUTPUT.read_text(encoding="utf-8")
        )
        inputs = document["inputs"]
        self.assertTrue(
            all(k.startswith("gt6_dump/") for k in inputs if k != "tools/build_t22_5_shape_analysis.py")
        )

    def test_full_source_replay_matches_compact_artifacts(self) -> None:
        if not (
            builder.DUMP / "gt.recipe.bath.json"
        ).is_file():
            self.skipTest("GT6 dump not present")
        fresh = builder.build_from_dump()
        committed = json.loads(
            builder.OUTPUT.read_text(encoding="utf-8")
        )
        for key in ("aggregate", "per_map", "verification"):
            self.assertEqual(fresh[key], committed[key])


if __name__ == "__main__":
    unittest.main()
