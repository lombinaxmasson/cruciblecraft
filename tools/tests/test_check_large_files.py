"""The large-file ratchet blocks new blobs and ignores archives."""
from __future__ import annotations

import json
import subprocess
import tempfile
import unittest
from pathlib import Path

from tools.repo_slimming import check_large_files as checker

ROOT = Path(__file__).resolve().parents[2]
LIMIT = checker.DEFAULT_LIMIT_BYTES


class CheckLargeFilesTest(unittest.TestCase):
    def test_six_megabyte_file_is_rejected(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            blob = Path(tmp) / "six.bin"
            blob.write_bytes(b"\0" * (6 * 1024 * 1024))
            findings = checker.oversized_paths(
                {"payload/six.bin": blob.stat().st_size},
                set(),
                limit_bytes=LIMIT,
            )
        self.assertEqual(["payload/six.bin"], findings)
        self.assertGreater(6 * 1024 * 1024, LIMIT)

    def test_allowlisted_file_is_kept(self) -> None:
        self.assertEqual(
            [],
            checker.oversized_paths(
                {"tools/already.json": LIMIT + 1},
                {"tools/already.json"},
                limit_bytes=LIMIT,
            ),
        )

    def test_allowlist_may_shrink_and_may_not_grow(self) -> None:
        previous = {"tools/old.json", "tools/stay.json"}
        self.assertEqual(
            [],
            checker.allowlist_growth(previous, {"tools/stay.json"}),
        )
        self.assertEqual(
            ["tools/new.json"],
            checker.allowlist_growth(previous, {"tools/stay.json", "tools/new.json"}),
        )
        self.assertEqual([], checker.allowlist_growth(None, {"tools/new.json"}))

    def test_stale_allowlist_entry_must_be_removed(self) -> None:
        self.assertEqual(
            ["tools/gone.json"],
            checker.stale_allowlist_paths(
                {"tools/stay.json": LIMIT + 1},
                {"tools/stay.json", "tools/gone.json"},
                limit_bytes=LIMIT,
            ),
        )

    def test_repository_ratchet_holds(self) -> None:
        self.assertEqual([], checker.collect_findings(ROOT))

    def test_archives_are_ignored_and_gradle_wrapper_is_not(self) -> None:
        ignored = subprocess.run(
            [
                "git",
                "check-ignore",
                "--no-index",
                "--",
                "repo-slimming-probe.7z",
                "repo-slimming-probe.zip",
                "repo-slimming-probe.rar",
                "repo-slimming-probe.jar",
            ],
            cwd=ROOT,
            text=True,
            capture_output=True,
            check=False,
        )
        self.assertEqual(0, ignored.returncode, ignored.stderr)
        self.assertEqual(
            [
                "repo-slimming-probe.7z",
                "repo-slimming-probe.zip",
                "repo-slimming-probe.rar",
                "repo-slimming-probe.jar",
            ],
            ignored.stdout.splitlines(),
        )
        wrapper = subprocess.run(
            [
                "git",
                "check-ignore",
                "--no-index",
                "--",
                "gradle/wrapper/gradle-wrapper.jar",
            ],
            cwd=ROOT,
            capture_output=True,
            check=False,
        )
        self.assertEqual(1, wrapper.returncode, wrapper.stdout)
        self.assertEqual(b"", wrapper.stdout)

    def test_allowlist_document_lists_strings(self) -> None:
        document = json.loads(
            (ROOT / checker.ALLOWLIST_RELATIVE).read_text(encoding="utf-8")
        )
        self.assertEqual(LIMIT, document["limit_bytes"])
        self.assertIsInstance(document["paths"], list)
        self.assertEqual(sorted(set(document["paths"])), document["paths"])


if __name__ == "__main__":
    unittest.main()
