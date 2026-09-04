"""Six runtime verification fail-closed failure classes."""
from __future__ import annotations

import json
import tempfile
import unittest
from pathlib import Path
from tools import currentness
from tools import census_common as census
from tools import verify as verify_entry


class T40VrFailureClassesTest(unittest.TestCase):
    def test_gate_overlay_change_is_semantic(self) -> None:
        expected = json.dumps({"java_overlay_sections": ["a"]}, sort_keys=True)
        actual = json.dumps({"java_overlay_sections": ["b"]}, sort_keys=True)
        path = Path(tempfile.mkdtemp()) / "gate.json"
        path.write_text(actual + "\n", encoding="utf-8")
        class_, _message = census.classify_stale(path, expected + "\n", actual + "\n")
        self.assertEqual("SEMANTIC_DRIFT", class_)

    def test_hash_only_upstream_is_rebindable(self) -> None:
        path = Path(tempfile.mkdtemp()) / "doc.json"
        path.write_text('{"body":1,"generated_by":"old"}\n', encoding="utf-8")
        class_, _message = census.classify_stale(
            path,
            '{"body":1,"generated_by":"new"}\n',
            path.read_text(encoding="utf-8"),
        )
        self.assertEqual("HASH_ONLY_DRIFT", class_)

    def test_semantic_drift_refuses_rebind(self) -> None:
        artifact = Path(tempfile.mkdtemp()) / "card.json"
        artifact.write_text('{"body":1}\n', encoding="utf-8")
        currentness.write_sidecar(artifact)
        artifact.write_text('{"body":2}\n', encoding="utf-8")
        with self.assertRaises(ValueError) as ctx:
            currentness.rebind_sidecar(artifact)
        self.assertIn("SEMANTIC_DRIFT", str(ctx.exception))

    def test_corrupt_large_json_is_corrupt_not_stale(self) -> None:
        path = Path(tempfile.mkdtemp()) / "huge.json"
        path.write_bytes(b"{" + (b"x" * 100) )
        class_, message = census.classify_stale(path, "{}", path.read_text(encoding="utf-8", errors="replace"))
        self.assertEqual("CORRUPT", class_)
        self.assertIn("size=", message)

    def test_filtered_gradle_xml_is_not_pass(self) -> None:
        summary = {
            "xml_present": False,
            "tests": 0,
            "failures": 0,
            "errors": 0,
            "skipped": 0,
            "rerun_tasks": True,
        }
        errors = verify_entry.gradle_full_suite_errors(summary)
        self.assertTrue(errors)

    def test_interrupted_write_leaves_digest(self) -> None:
        from tools import atomic_io

        root = Path(tempfile.mkdtemp())
        path = root / "keep.json"
        payload = b'{"keep":true}\n'
        path.write_bytes(payload)
        tmp = list(root.glob(".*"))
        self.assertEqual(payload, path.read_bytes())
        self.assertEqual([], tmp)
        atomic_io.write_bytes(path, payload)
        self.assertEqual(payload, path.read_bytes())


if __name__ == "__main__":
    unittest.main()
