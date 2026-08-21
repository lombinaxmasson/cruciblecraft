from __future__ import annotations

import json
import unittest

from tools import build_t31_load_report as builder


class T31LoadReportTest(unittest.TestCase):
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

    def test_hard_ceilings_and_pending_package_size_are_not_zero_filled(self) -> None:
        document = builder.build()
        self.assertEqual(21_000, document["eager_hard_ceiling"])
        self.assertEqual(6_600, document["datapack_hard_ceiling"])
        self.assertEqual(0, document["publication_delta_measured"]["eager"])
        package = document["axes"]["package_size"]
        self.assertFalse(package["zero_filled"])
        if package["status"] == "pending":
            self.assertIsNone(package["jar_bytes"])
            self.assertIsNone(package["zip_bytes"])
        else:
            self.assertEqual("measured", package["status"])
            self.assertGreater(package["jar_bytes"], 0)
            self.assertGreater(package["zip_bytes"], 0)
        eager = document["axes"]["eager_publication_rows"]
        self.assertEqual("PASS", eager["verdict"])
        self.assertEqual(16_541, eager["measured"])
        self.assertEqual(
            "MEASURED_AT_SCALE", document["axes"]["target_scale"]["status"]
        )
        self.assertEqual(
            "MEASURED_AT_SCALE", document["axes"]["stress_scale"]["status"]
        )
        self.assertEqual([], document["hard_ceiling_failures"])


if __name__ == "__main__":
    unittest.main()
