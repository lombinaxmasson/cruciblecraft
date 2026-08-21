from __future__ import annotations

import json
import unittest
from unittest import mock

from tools import build_t31_scale_recheck as builder
from tools import t27_common as common
from tools import t31_common as t31


class T31ScaleRecheckTest(unittest.TestCase):
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

    def test_t24_historical_skip_is_preserved(self) -> None:
        document = builder.build()
        historical = document["historical_t24"]
        self.assertEqual("SKIP", historical["target_status"])
        self.assertEqual("SKIP", historical["stress_status"])
        self.assertFalse(historical["rewritten_as_measured_at_t24"])
        evidence = common.load_json(t31.T24_EVIDENCE)
        self.assertEqual(
            "SKIP", evidence["measured_at_scale"]["target"]["status"]
        )
        self.assertEqual(
            "SKIP", evidence["measured_at_scale"]["stress"]["status"]
        )
        self.assertNotIn("MEASURED_AT_SCALE", historical["target_status"])

    def test_missing_samples_are_pending_not_zero_filled(self) -> None:
        document = builder.build()
        for name in ("target", "stress"):
            row = document["current"][name]
            if row["status"] == "PENDING_MEASUREMENT":
                self.assertIsNone(row["visited_max"])
                self.assertIsNone(row["network_sync_bytes"])
                self.assertFalse(row["zero_filled"])
                self.assertEqual(0, row["samples"])

    def test_mutating_t24_skip_fails_closed(self) -> None:
        real = common.load_json

        def override(path):
            document = real(path)
            if path == t31.T24_EVIDENCE:
                document = json.loads(json.dumps(document))
                document["measured_at_scale"]["target"]["status"] = "MEASURED_AT_SCALE"
            return document

        with mock.patch.object(common, "load_json", side_effect=override):
            with self.assertRaises(ValueError):
                builder.build()


if __name__ == "__main__":
    unittest.main()
