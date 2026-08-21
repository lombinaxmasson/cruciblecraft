from __future__ import annotations

import json
import unittest

from tools import build_t28_publication_delta as builder


class T28PublicationDeltaTest(unittest.TestCase):
    def test_committed_artifact_is_current_and_check_is_read_only(self) -> None:
        on_disk = json.loads(builder.OUTPUT.read_text(encoding="utf-8"))
        expected = builder.build()
        self.assertEqual(expected, on_disk)
        before = builder.OUTPUT.read_bytes()
        self.assertEqual([], builder.check())
        self.assertEqual(before, builder.OUTPUT.read_bytes())

    def test_projected_delta_is_negative_and_pending_is_not_zero(self) -> None:
        document = builder.build()
        self.assertEqual("T28_PUBLICATION_DELTA", document["status"])
        self.assertLess(document["projected"]["logical"], 0)
        self.assertLess(document["projected"]["eager"], 0)
        self.assertEqual(0, document["projected"]["lazy"])
        measured = document["measured"]
        if measured["status"] == "pending":
            self.assertIsNone(measured["logical"])
            self.assertIsNone(measured["eager"])
            self.assertIsNone(measured["lazy"])
            self.assertFalse(document["same_sign_as_projection"])
            self.assertNotEqual(0, document["live_report_cooling"])
        else:
            self.assertEqual("measured", measured["status"])
            self.assertEqual(document["projected"]["logical"], measured["logical"])
            self.assertEqual(document["projected"]["eager"], measured["eager"])
            self.assertEqual(0, measured["lazy"])
            self.assertTrue(document["same_sign_as_projection"])
            self.assertEqual(0, document["live_report_cooling"])
            self.assertFalse(builder.COOLING_RULE.is_file())


if __name__ == "__main__":
    unittest.main()
