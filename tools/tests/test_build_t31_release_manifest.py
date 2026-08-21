from __future__ import annotations

import json
import unittest

from tools import build_t31_release_manifest as builder
from tools import t31_common as t31


class T31ReleaseManifestTest(unittest.TestCase):
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

    def test_missing_packages_are_pending_until_rc1_artifacts_exist(self) -> None:
        document = builder.build()
        version = t31.gradle_mod_version()
        self.assertEqual(version == t31.OPENING_VERSION, document["docs"]["version_still_beta"])
        self.assertEqual(version == t31.TARGET_VERSION, document["docs"]["version_is_rc1"])
        if version != t31.TARGET_VERSION:
            self.assertIn("mod_version", document["pending"])
        for row in document["packages"].values():
            if row["status"] == "pending":
                self.assertIsNone(row["sha256"])
                self.assertIsNone(row["bytes"])
                self.assertFalse(row["zero_filled"])
        if document["packages"]["jar"]["status"] != "measured" or document["packages"]["zip"]["status"] != "measured":
            self.assertNotEqual("T31_RELEASE_MANIFEST_COMPLETE", document["status"])
            self.assertIn("jar_content", document["pending"])
            self.assertIn("zip_content", document["pending"])
        else:
            self.assertEqual("measured", document["content_checks"]["jar"]["status"])
            self.assertEqual("measured", document["content_checks"]["zip"]["status"])
            self.assertEqual(t31.TARGET_VERSION, document["content_checks"]["jar"]["embedded_version"])
            self.assertFalse(document["content_checks"]["zip"]["contains_beta_jar"])


if __name__ == "__main__":
    unittest.main()
