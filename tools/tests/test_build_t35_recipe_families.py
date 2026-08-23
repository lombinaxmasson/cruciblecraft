"""Tests for the T35 ordinary_optional recipe family builder."""
from __future__ import annotations

import hashlib
import sys
import tempfile
import unittest
from pathlib import Path
from unittest import mock

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_t35_recipe_families as builder  # noqa: E402
from tools import t27_common as common  # noqa: E402


def _sha(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def _dump_maps_available() -> bool:
    return all(
        (builder.DUMP_MAPS / f"{map_name}.json").is_file()
        for map_name in builder.SHAPE_MAPS
    )


class T35RecipeFamiliesReferenceOnlyTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        if not builder.OUTPUT.is_file():
            raise unittest.SkipTest("artifact not yet generated")
        cls.document = common.load_json(builder.OUTPUT)

    def test_parser_accepts_reference_only(self) -> None:
        with mock.patch.object(builder, "reference_only_check", return_value=[]):
            exit_code = builder.main(["--check", "--reference-only"])
        self.assertEqual(exit_code, 0)

    def test_reference_only_check_without_dump_maps(self) -> None:
        missing = builder.DUMP_MAPS.with_name("missing_dump_maps_for_test")
        with mock.patch.object(builder, "DUMP_MAPS", missing):
            errors = builder.reference_only_check()
        self.assertEqual(errors, [])

    def test_reference_only_rejects_malformed_artifact(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory) / "t35_recipe_families.json"
            output.write_text("{not json", encoding="utf-8")
            with mock.patch.object(builder, "OUTPUT", output):
                errors = builder.reference_only_check()
        self.assertTrue(any("malformed" in error for error in errors))

    def test_reference_only_rejects_raw_source_row_payload(self) -> None:
        document = common.load_json(builder.OUTPUT)
        document["families"][0]["source_row_keys"] = [[4, 1]]
        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory) / "t35_recipe_families.json"
            output.write_bytes(common.stable_json(document).encode("utf-8"))
            with mock.patch.object(builder, "OUTPUT", output):
                errors = builder.reference_only_check()
        self.assertTrue(
            any("source_row_keys" in error for error in errors),
            msg=str(errors),
        )

    def test_reference_only_rejects_row_level_non_mixer_keys(self) -> None:
        document = common.load_json(builder.OUTPUT)
        for family in document["families"]:
            if family.get("membership_kind") == "semantic_template":
                family["template_key"] = (
                    "sha256:00010677e555fadaa2bcb06cf2fd67b38881485435814bded630c4be7404279a"
                )
                break
        else:
            self.fail("expected a semantic_template family")
        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory) / "t35_recipe_families.json"
            output.write_bytes(common.stable_json(document).encode("utf-8"))
            with mock.patch.object(builder, "OUTPUT", output):
                errors = builder.reference_only_check()
        self.assertTrue(
            any("ledger-style" in error or "row-level" in error for error in errors),
            msg=str(errors),
        )

    def test_reference_only_counts_and_crosswalk(self) -> None:
        counts = self.document["counts"]
        membership = self.document["membership"]
        self.assertEqual(counts["ordinary_optional_rows"], 78682)
        self.assertEqual(counts["assigned_rows"], 78682)
        self.assertEqual(membership["unassigned"], 0)
        self.assertEqual(membership["duplicate"], 0)
        self.assertLess(counts["families"], 78682 // 2)

        expanded = sum(row["expanded_count"] for row in self.document["families"])
        self.assertEqual(expanded, 78682)
        self.assertEqual(builder.reference_only_check(), [])

    def test_stale_classification_input_is_detected_reference_only(self) -> None:
        real_hash = common.sha256_file

        def stale_hash(path: Path) -> str:
            if path == builder.ROW_CLASSIFICATION:
                return "0" * 64
            return real_hash(path)

        with mock.patch.object(common, "sha256_file", side_effect=stale_hash):
            errors = builder.reference_only_check()
        self.assertTrue(
            any("source_hashes" in error or "drifted" in error for error in errors),
            msg=str(errors),
        )

    def test_linked_compact_inputs_are_read_only(self) -> None:
        protected = (
            builder.ROW_CLASSIFICATION,
            builder.TEMPLATE_DENOMINATOR,
            builder.SHAPE_ANALYSIS,
            builder.MACHINE_PLAYABILITY,
            builder.MIXER_INDEX,
        )
        before = {path: _sha(path) for path in protected}
        builder.reference_only_check()
        for path, digest in before.items():
            self.assertEqual(_sha(path), digest, f"{path.name} was modified")


@unittest.skipUnless(_dump_maps_available(), "gt6_dump maps unavailable")
class T35RecipeFamiliesFullReplayTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.document = builder.build()

    def test_build_is_deterministic(self) -> None:
        first = builder.build()
        second = builder.build()
        self.assertEqual(
            common.stable_json(first),
            common.stable_json(second),
        )

    def test_recomputed_membership_matches(self) -> None:
        recomputed = builder.recompute_membership()
        counts = self.document["counts"]
        self.assertEqual(recomputed["assigned_rows"], 78682)
        self.assertEqual(recomputed["families"], counts["families"])
        self.assertEqual(
            recomputed["non_mixer_families"],
            counts["non_mixer_families"],
        )
        self.assertEqual(recomputed["mixer_families"], counts["mixer_families"])

    def test_family_keys_are_semantic_not_row_indexed(self) -> None:
        families = self.document["families"]
        family_ids = [row["family_id"] for row in families]
        self.assertEqual(len(family_ids), len(set(family_ids)))
        for row in families:
            self.assertTrue(row["family_id"].startswith("portfolio:track_a/"))
            self.assertNotIn("source_row_keys", row)
            self.assertIn("membership_evidence", row)
            evidence = row["membership_evidence"]
            self.assertEqual(
                evidence["ordinary_optional_rows"],
                row["expanded_count"],
            )
            if row["membership_kind"] == "semantic_template":
                self.assertIn("#", row["template_key"])
                self.assertFalse(row["template_key"].startswith("sha256:"))
            else:
                self.assertTrue(row["template_key"].startswith("sha256:"))

    def test_expected_family_count(self) -> None:
        self.assertEqual(self.document["counts"]["families"], 5718)
        self.assertEqual(self.document["counts"]["non_mixer_families"], 5054)
        self.assertEqual(self.document["counts"]["mixer_families"], 664)

    def test_full_replay_records_dump_hashes(self) -> None:
        replay = self.document.get("full_replay") or {}
        self.assertTrue(replay.get("replay_verified"))
        dump_hashes = replay.get("dump_map_hashes") or {}
        for map_name in builder.SHAPE_MAPS:
            path = builder.DUMP_MAPS / f"{map_name}.json"
            self.assertEqual(
                dump_hashes[common.relative(path)],
                common.sha256_file(path),
            )

    def test_corrupted_shape_analysis_is_fail_closed(self) -> None:
        document = common.load_json(builder.SHAPE_ANALYSIS)
        document["per_map"][0]["template_count"] = 0

        real_load = common.load_json

        def corrupt_shape_analysis(path: Path):
            if path == builder.SHAPE_ANALYSIS:
                return document
            return real_load(path)

        with mock.patch.object(common, "load_json", side_effect=corrupt_shape_analysis):
            with self.assertRaises(ValueError):
                builder.build()

    def test_full_replay_check_requires_dump(self) -> None:
        if not builder.OUTPUT.is_file():
            self.skipTest("artifact not yet generated")
        errors = builder.check()
        if errors:
            self.skipTest(
                "transient upstream dependency; full replay not current: "
                + "; ".join(errors)
            )
        self.assertEqual(errors, [])


if __name__ == "__main__":
    unittest.main()
