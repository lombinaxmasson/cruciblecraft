"""Windows atomic writer regressions for T40-VR R2."""
from __future__ import annotations

import json
import os
import threading
import unittest
from pathlib import Path

from tools import atomic_io
from tools import t35_common as t35


class AtomicIoTest(unittest.TestCase):
    def test_write_bytes_uses_utf8_lf_and_replace(self) -> None:
        root = Path(self._tmp())
        path = root / "sample.json"
        atomic_io.write_text(path, "{\r\n  \"a\": 1\r\n}\r\n")
        payload = path.read_bytes()
        self.assertNotIn(b"\r", payload)
        self.assertTrue(payload.endswith(b"\n"))
        leftovers = list(root.glob(".*.tmp"))
        self.assertEqual([], leftovers)

    def test_large_json_roundtrip(self) -> None:
        root = Path(self._tmp())
        path = root / "large.json"
        document = {"rows": ["x" * 1024] * (26 * 1024)}
        payload = json.dumps(document, separators=(",", ":")).encode("utf-8")
        self.assertGreaterEqual(len(payload), 25 * 1024 * 1024)
        atomic_io.write_bytes(path, payload)
        self.assertEqual(payload, path.read_bytes())

    def test_concurrent_writers_keep_valid_json(self) -> None:
        root = Path(self._tmp())
        errors: list[str] = []

        def write(name: str, value: int) -> None:
            try:
                atomic_io.write_text(root / name, json.dumps({"n": value}) + "\n")
            except Exception as error:  # noqa: BLE001
                errors.append(str(error))

        threads = [
            threading.Thread(target=write, args=(f"w{index}.json", index))
            for index in range(8)
        ]
        for thread in threads:
            thread.start()
        for thread in threads:
            thread.join()
        self.assertEqual([], errors)
        for index in range(8):
            document = json.loads((root / f"w{index}.json").read_text(encoding="utf-8"))
            self.assertEqual(index, document["n"])

    def test_interrupted_tmp_does_not_replace_destination(self) -> None:
        root = Path(self._tmp())
        path = root / "keep.json"
        original = b'{"ok":true}\n'
        path.write_bytes(original)
        tmp = path.with_name(f".{path.name}.{os.getpid()}.dead.tmp")
        tmp.write_bytes(b"{not-json")
        tmp.unlink()
        self.assertEqual(original, path.read_bytes())

    def test_stale_error_classifies_corrupt_and_semantic(self) -> None:
        root = Path(self._tmp())
        missing = root / "missing.json"
        class_, message = t35.classify_stale(missing, "{}", "")
        self.assertEqual("MISSING", class_)
        corrupt = root / "corrupt.json"
        corrupt.write_bytes(b"{not json")
        class_, message = t35.classify_stale(corrupt, "{}", corrupt.read_text(encoding="utf-8"))
        self.assertEqual("CORRUPT", class_)
        self.assertIn("line=", message)
        semantic = root / "semantic.json"
        semantic.write_text('{"a":1}\n', encoding="utf-8")
        class_, message = t35.classify_stale(semantic, '{"a":2}\n', semantic.read_text(encoding="utf-8"))
        self.assertEqual("SEMANTIC_DRIFT", class_)
        hash_only = root / "hash.json"
        hash_only.write_text('{"a":1,"generated_by":"old"}\n', encoding="utf-8")
        class_, message = t35.classify_stale(
            hash_only,
            '{"a":1,"generated_by":"new"}\n',
            hash_only.read_text(encoding="utf-8"),
        )
        self.assertEqual("HASH_ONLY_DRIFT", class_)
        self.assertIn("rebuilt_sha256=", message)

    def test_check_compact_passes_envelope_hash_only_drift(self) -> None:
        import tempfile
        from pathlib import Path

        root = Path(tempfile.mkdtemp(prefix="compact_check_"))
        path = root / "ledger.json"
        path.write_text(
            '{"counts":{"n":1},"input_sha256":{"a":"old"},"status":"READY"}\n',
            encoding="utf-8",
        )
        rebuilt = {
            "counts": {"n": 1},
            "input_sha256": {"a": "new"},
            "status": "READY",
        }
        self.assertEqual([], t35.check_compact(path, rebuilt))
        rebuilt["counts"] = {"n": 2}
        errors = t35.check_compact(path, rebuilt)
        self.assertTrue(errors)
        self.assertTrue(errors[0].startswith("SEMANTIC_DRIFT"))

    def _tmp(self) -> str:
        import tempfile

        return tempfile.mkdtemp(prefix="atomic_io_")


if __name__ == "__main__":
    unittest.main()
