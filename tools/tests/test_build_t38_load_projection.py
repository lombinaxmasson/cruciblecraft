from __future__ import annotations

import json
import unittest

from tools import build_t38_load_projection as builder
from tools import t38_common as t38


class T38LoadProjectionTest(unittest.TestCase):
    def test_committed_artifacts_are_current_and_check_is_read_only(self) -> None:
        on_disk_input = json.loads(builder.INPUT_OUTPUT.read_text(encoding="utf-8"))
        on_disk_output = json.loads(builder.PROJECTION_OUTPUT.read_text(encoding="utf-8"))
        self.assertEqual(builder.build_input(), on_disk_input)
        self.assertEqual(builder.build(), on_disk_output)
        before_input = builder.INPUT_OUTPUT.read_bytes()
        before_output = builder.PROJECTION_OUTPUT.read_bytes()
        self.assertEqual([], builder.check())
        self.assertEqual(before_input, builder.INPUT_OUTPUT.read_bytes())
        self.assertEqual(before_output, builder.PROJECTION_OUTPUT.read_bytes())

    def test_delivery_phase_t38_is_accepted_with_on_demand_counts(self) -> None:
        input_document = builder.build_input()
        self.assertEqual("T38", input_document["delivery_phase"])
        family = input_document["families"][0]
        self.assertEqual("on_demand", family["strategy"])
        self.assertEqual(t38.FAMILY_COUNT, family["authored_entries"])
        self.assertEqual(t38.SOURCE_ROWS, family["logical_rows"])
        self.assertEqual(0, family["eager_publication_rows"])
        self.assertEqual(t38.SOURCE_ROWS, family["lazy_logical_rows"])
        self.assertEqual(16, family["lazy_cache_ceiling_rows"])
        self.assertEqual(7581, family["sync_bytes"])
        self.assertEqual([73], family["measurement_basis"]["measured_logical_rows"])

        result = builder.build()
        self.assertEqual("T38", result["delivery_phase"])
        counts = result["ledger"]["counts"]
        self.assertEqual(t38.FAMILY_COUNT, counts["datapack_authored_entries"])
        self.assertEqual(t38.SOURCE_ROWS, counts["logical_rows"])
        self.assertEqual(0, counts["eager_publication_rows"])
        self.assertEqual(t38.SOURCE_ROWS, counts["lazy_logical_rows"])
        self.assertEqual(16, counts["lazy_cache_ceiling_rows"])

    def test_does_not_copy_t37_hybrid_or_cache_eight(self) -> None:
        input_document = builder.build_input()
        encoded = json.dumps(input_document)
        self.assertNotIn('"eager_publication_rows": 14', encoded)
        self.assertNotIn('"lazy_logical_rows": 36', encoded)
        self.assertNotIn('"lazy_cache_ceiling_rows": 8', encoded)

    def test_projection_soft_lookup_allowed_not_hard_ceiling(self) -> None:
        document = builder.build()
        budget = document["budget_evaluation"]
        self.assertNotEqual("HARD_CEILING_EXCEEDED", budget["status"])
        lookup = budget["metrics"]["lookup_candidate_count"]
        self.assertEqual(73, lookup["comparison_value"])
        self.assertEqual("SOFT_BUDGET_EXCEEDED", lookup["status"])
        self.assertIn("lookup_candidate_count", budget["soft_exceeded"])


if __name__ == "__main__":
    unittest.main()
