from __future__ import annotations

import json
import unittest

from tools import build_t31_compatibility_report as builder
from tools import t31_common as t31


class T31CompatibilityReportTest(unittest.TestCase):
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

    def test_handshake_version_is_one_and_pending_axes_are_not_zero(self) -> None:
        document = builder.build()
        handshake = document["axes"]["handshake_network_version_1"]
        self.assertEqual("measured", handshake["status"])
        self.assertTrue(handshake["value"])
        hopper = document["axes"]["hopper_menu_does_not_mask_protocol"]
        self.assertEqual("measured", hopper["status"])
        pending_names = [
            name
            for name in (
                "save_reload",
                "client_cc_only",
                "dedicated_cc_only",
                "emi",
                "jade",
                "kubejs",
            )
            if document["axes"][name]["status"] == "pending"
        ]
        for name in pending_names:
            row = document["axes"][name]
            self.assertIsNone(row["value"])
            self.assertFalse(row["zero_filled"])
        if pending_names:
            self.assertNotEqual("T31_COMPATIBILITY_COMPLETE", document["status"])
        else:
            self.assertEqual("T31_COMPATIBILITY_COMPLETE", document["status"])
            for name in (
                "save_reload",
                "client_cc_only",
                "dedicated_cc_only",
                "emi",
                "jade",
                "kubejs",
            ):
                self.assertEqual("measured", document["axes"][name]["status"])
                self.assertTrue(document["axes"][name]["value"])
        self.assertEqual(t31.gradle_mod_version(), document["mod_version"])


if __name__ == "__main__":
    unittest.main()
