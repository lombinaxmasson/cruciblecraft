from __future__ import annotations

import json
import unittest
from unittest import mock

from tools import build_t31_readiness as builder
from tools import t27_common as common


class T31ReadinessTest(unittest.TestCase):
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

    def test_status_is_absent_until_gates_pass(self) -> None:
        document = builder.build()
        if document.get("status") == "T31_READY":
            self.assertTrue(all(document["closure"].values()))
            self.assertEqual("0.1.0-rc.1", document["runtime"]["mod_version"])
        else:
            self.assertNotIn("status", document)
            policy = document.get("closure_policy") or {}
            self.assertTrue(
                (not all(document["closure"].values()))
                or bool(policy.get("pending"))
                or policy.get("final_closure_attempted") is not True
            )

    def test_hand_written_ready_cannot_appear_from_build(self) -> None:
        document = builder.build()
        if not all(document["closure"].values()):
            self.assertNotEqual("T31_READY", document.get("status"))

    def test_mutating_scale_hash_fails_closed(self) -> None:
        real = common.sha256_file

        def override(path):
            if path == builder.SCALE:
                return "0" * 64
            return real(path)

        with mock.patch.object(common, "sha256_file", side_effect=override):
            mutated = builder.build()
        on_disk = json.loads(builder.OUTPUT.read_text(encoding="utf-8"))
        self.assertNotEqual(
            mutated["owned_inputs"][common.relative(builder.SCALE)],
            on_disk["owned_inputs"][common.relative(builder.SCALE)],
        )


if __name__ == "__main__":
    unittest.main()
