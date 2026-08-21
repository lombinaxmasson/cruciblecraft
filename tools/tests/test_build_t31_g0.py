from __future__ import annotations

import json
import unittest

from tools import build_t31_g0 as builder
from tools import t31_common as t31


class T31G0Test(unittest.TestCase):
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

    def test_g0_does_not_claim_ready(self) -> None:
        document = builder.build()
        self.assertIn(
            document["mod_version"],
            (t31.OPENING_VERSION, t31.TARGET_VERSION),
        )
        self.assertTrue(document["gates"]["version_discipline"])
        self.assertNotEqual("T31_READY", document.get("status"))
        self.assertNotIn("GA", document["forbidden_draft_hits"])
        if document["status"] == "T31_G0_PASSED":
            self.assertEqual([], document["pending"])
            self.assertEqual([], document["defects"]["release_blockers"])
        else:
            self.assertEqual("T31_G0_BLOCKED", document["status"])
            self.assertTrue(document["pending"])


if __name__ == "__main__":
    unittest.main()
