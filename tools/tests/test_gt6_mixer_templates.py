import copy
import sys
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import gt6_mixer_templates as builder  # noqa: E402


class GT6MixerTemplatesTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.index = builder.load(builder.INDEX_OUTPUT)
        cls.membership = builder.load(builder.MEMBERSHIP_OUTPUT)
        cls.report = builder.load(builder.REPORT_OUTPUT)

    def test_compact_evidence_is_complete_and_current(self):
        builder.validate_compact(
            self.index, self.membership, self.report
        )
        self.assertEqual(3_414, self.report["template_count"])
        self.assertEqual(64_245, self.report["source_recipe_count"])
        self.assertEqual(500, self.report["singleton_templates"])
        self.assertEqual(2_914, self.report["multirow_templates"])
        self.assertEqual(
            {
                "enumerated": 103,
                "material_matrix": 2_628,
                "opaque": 683,
            },
            self.report["shape_templates"],
        )
        self.assertEqual(64_245, sum(self.report["shape_rows"].values()))
        self.assertTrue(self.report["verification"]["replay_verified"])
        self.assertEqual(0, self.report["verification"]["missing_count"])
        self.assertEqual(0, self.report["verification"]["extra_count"])

    def test_membership_is_a_total_bijection_over_source_indexes(self):
        membership = self.membership["membership"]
        self.assertEqual(64_245, len(membership))
        self.assertTrue(all(
            0 <= value < len(self.index["templates"])
            for value in membership
        ))
        assigned = []
        for template_index, template in enumerate(self.index["templates"]):
            for source_index, _values, _digest in template["supports"]:
                self.assertEqual(
                    template_index, membership[source_index]
                )
                assigned.append(source_index)
        self.assertEqual(list(range(64_245)), sorted(assigned))

    def test_support_mutation_breaks_local_replay(self):
        template = next(
            row for row in self.index["templates"]
            if row["binding_paths"] and row["source_count"] > 1
        )
        changed = copy.deepcopy(template["supports"][0])
        original = changed[1][0]
        changed[1][0] = (
            original + 1 if isinstance(original, int) else "__mutation__"
        )
        replay = builder.apply_support(
            template["skeleton"],
            template["binding_paths"],
            changed[1],
        )
        self.assertNotEqual(changed[2], builder.content_hash(replay))

    def test_full_source_replay_matches_compact_artifacts(self):
        expected = builder.build_documents()
        self.assertEqual(self.index, expected[0])
        self.assertEqual(self.membership, expected[1])
        self.assertEqual(self.report, expected[2])


if __name__ == "__main__":
    unittest.main()
