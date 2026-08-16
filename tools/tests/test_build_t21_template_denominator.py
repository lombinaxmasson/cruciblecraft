import sys
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_t21_template_denominator as builder  # noqa: E402


class T21TemplateDenominatorTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.document = builder.load(builder.OUTPUT)

    def test_compact_denominator_is_current_and_classified(self):
        builder.validate_compact(self.document)
        counts = self.document["counts"]
        self.assertEqual(151_433, counts["source_rows"])
        self.assertEqual(93_133, counts["denominator_units"])
        self.assertEqual(3_414, counts["mixer_templates"])
        self.assertEqual(1, counts["v1_required_units"])
        self.assertEqual(0, counts["unclassified"])

    def test_row_diagnostic_is_not_the_closure_numerator(self):
        policy = builder.load(builder.POLICY)
        self.assertFalse(
            policy["row_diagnostic"]["closure_numerator"]
        )
        self.assertEqual(
            "DIAGNOSTIC_ONLY",
            policy["row_diagnostic"]["status"],
        )
        self.assertEqual(
            4,
            self.document["counts"]["expanded_row_diagnostics"][
                "v1_required"
            ],
        )

    def test_v1_template_has_positive_reachability_evidence(self):
        classes = self.document["encoding"]["classes"]
        selected = [
            row for row in self.document["units"]
            if classes[row[3]] == "v1_required"
        ]
        self.assertEqual(1, len(selected))
        self.assertEqual(4, len(selected[0][4]))
        self.assertTrue(selected[0][5])
        forced = builder.load(builder.POLICY)["forced_v1_templates"]
        self.assertEqual(1, len(forced))
        self.assertIn("gunpowder", forced[0]["reason"])

    def test_coverage_is_split_before_denominator_publication(self):
        coverage = self.document["coverage"]
        self.assertEqual(145, coverage["current_t5_source_rows"])
        self.assertGreater(
            coverage["partially_covered_templates_split"], 0
        )
        self.assertEqual(0, coverage["partially_covered_units"])

    def test_full_template_denominator_replay_matches_compact(self):
        self.assertEqual(self.document, builder.build())


if __name__ == "__main__":
    unittest.main()
