"""Contract tests for T39 load projection."""
from __future__ import annotations

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_t39_load_projection as builder  # noqa: E402
from tools import t39_common as t39  # noqa: E402


class T39LoadProjectionTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.input_document = builder.build_input()
        cls.output_document = builder.build()

    def test_status_tracks_current_production_decision(self) -> None:
        if t39.production_strategy()["blocked"]:
            self.assertEqual(
                "FAMILY_LOAD_PROJECTION_INPUT_BLOCKED",
                self.input_document["status"],
            )
            self.assertEqual("T39_LOAD_PROJECTION_BLOCKED", self.output_document["status"])
            self.assertTrue(self.input_document.get("blockers"))
            self.assertTrue(self.output_document.get("blockers"))
        else:
            self.assertEqual("FAMILY_LOAD_PROJECTION_INPUT", self.input_document["status"])
            self.assertEqual("PASS", self.output_document["status"])
            self.assertFalse(self.input_document.get("blockers"))
            self.assertFalse(self.output_document.get("blockers"))
            self.assertEqual(
                t39.production_lock_sha256(),
                self.output_document["production_lock_sha256"],
            )
            self.assertEqual(
                t39.production_family_count(),
                self.output_document["family_count"],
            )
            self.assertEqual(
                t39.production_relation_count(),
                self.output_document["logical"],
            )

    def test_delivery_phase_t39_without_crashing(self) -> None:
        self.assertEqual("T39", self.input_document["delivery_phase"])
        self.assertEqual("T39", self.output_document["delivery_phase"])

    def test_does_not_copy_t37_or_t38_partitions_when_blocked(self) -> None:
        import json

        encoded = json.dumps(self.input_document)
        self.assertNotIn('"eager_publication_rows": 14', encoded)
        self.assertNotIn('"lazy_logical_rows": 36', encoded)
        self.assertNotIn('"lazy_cache_ceiling_rows": 8', encoded)
        self.assertNotIn('"lazy_cache_ceiling_rows": 16', encoded)

    def test_check_is_read_only_when_artifacts_exist(self) -> None:
        if not builder.INPUT_OUTPUT.is_file() or not builder.PROJECTION_OUTPUT.is_file():
            self.skipTest("t39 load projection artifacts not generated")
        before_input = builder.INPUT_OUTPUT.read_bytes()
        before_output = builder.PROJECTION_OUTPUT.read_bytes()
        self.assertEqual([], builder.check())
        self.assertEqual(before_input, builder.INPUT_OUTPUT.read_bytes())
        self.assertEqual(before_output, builder.PROJECTION_OUTPUT.read_bytes())


if __name__ == "__main__":
    unittest.main()
