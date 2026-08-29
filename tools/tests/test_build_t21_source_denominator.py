import copy
import sys
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_t21_source_denominator as builder  # noqa: E402


class T21SourceDenominatorTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.document = builder.load(builder.OUTPUT)

    def test_artifact_matches_current_fixed_source(self):
        builder.validate_compact(self.document)
        self.assertEqual(
            "T21_SOURCE_DENOMINATOR_READY", self.document["status"]
        )
        self.assertEqual(
            "3703e40308c8c030763fd6297dea8b210d2a77b1",
            self.document["source"]["revision"],
        )

    def test_exact_source_row_and_disposition_counts_are_locked(self):
        counts = self.document["counts"]
        self.assertEqual(224, counts["material_candidates"])
        self.assertEqual(45_353, counts["source_rows"])
        self.assertEqual(
            {
                "ordinary_v1_required": 45_044,
                "petroleum_t22": 5,
                "post_1_0_nuclear": 304,
            },
            counts["dispositions"],
        )
        self.assertEqual(0, counts["unclassified"])
        self.assertEqual(
            45_353,
            counts["translations"]["translatable"]
            + counts["translations"]["blocked_translation"],
        )

    def test_ordinary_translation_gap_is_explicit_not_hidden(self):
        counts = self.document["counts"]
        self.assertEqual(45_044, counts["ordinary_v1_required"])
        self.assertEqual(2_638, counts["ordinary_translatable"])
        self.assertEqual(42_406, counts["ordinary_blocked_translation"])
        rejections = {
            row["reason"]: row["rows"]
            for row in self.document["rejection_summary"]
        }
        self.assertEqual(30_437, rejections["fluid_mapping"])
        self.assertEqual(3_647, rejections["chemical_fluid_state"])
        self.assertEqual(3_019, rejections["item_mapping"])
        self.assertEqual(2_508, rejections["item_registration"])
        self.assertEqual(2_039, rejections["machine_shape"])

    def test_compact_rows_have_unique_fixed_source_identity(self):
        rows = self.document["rows"]
        keys = [(row[0], row[1]) for row in rows]
        self.assertEqual(len(keys), len(set(keys)))
        self.assertTrue(all(len(row[7]) == 64 for row in rows))
        self.assertEqual(
            10, len(self.document["map_summaries"])
        )

    def test_full_source_denominator_replay_matches_compact(self):
        from tools import currentness

        builder.validate_compact(self.document)
        self.assertEqual([], currentness.check_sidecar(builder.OUTPUT))

    def test_rebuild_preserves_semantic_document_fields(self):
        from tools import currentness

        self.assertEqual([], currentness.check_sidecar(builder.OUTPUT))
        candidate = copy.deepcopy(self.document)
        candidate["inputs"] = {
            "tools/t5_chemical_readiness.json": "0" * 64,
            "tools/build_t5_source_projection.py": "1" * 64,
        }
        self.assertEqual(
            builder.semantic_document(self.document),
            builder.semantic_document(candidate),
        )

    def test_metadata_rebase_guard_accepts_readiness_hash_refresh(self):
        candidate = copy.deepcopy(self.document)
        pre_repair = copy.deepcopy(self.document)
        pre_repair["inputs"]["tools/t5_chemical_readiness.json"] = (
            "2cc6c9fbaca4a4b41d1abe7bb1a5eb00ff6d6a572aaad86ac3597bd35b1a7062"
        )
        self.assertEqual(
            [],
            builder.verify_metadata_rebase(pre_repair, candidate),
        )


if __name__ == "__main__":
    unittest.main()
