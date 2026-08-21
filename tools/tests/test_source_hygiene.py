from __future__ import annotations

import unittest
from pathlib import Path


class SourceHygieneTest(unittest.TestCase):
    def test_java_sources_contain_no_crlf_line_endings(self) -> None:
        root = Path(__file__).resolve().parents[2]
        java_files = sorted((root / "src").rglob("*.java"))
        self.assertTrue(java_files)
        offenders = [
            path.relative_to(root).as_posix()
            for path in java_files
            if b"\r\n" in path.read_bytes()
        ]
        self.assertEqual([], offenders)
