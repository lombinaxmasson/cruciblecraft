"""Concurrency and no-write guarantees for T35R compact checks."""
from __future__ import annotations

import hashlib
import subprocess
import sys
import unittest
from pathlib import Path
from tempfile import TemporaryDirectory

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import t35_common as t35  # noqa: E402


def _digest(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


class T35ConcurrentChecksTest(unittest.TestCase):
    def test_atomic_write_and_first_difference_diagnostic(self) -> None:
        with TemporaryDirectory() as directory:
            output = Path(directory) / "artifact.json"
            t35.write_stable(output, {"alpha": {"beta": 1}})
            self.assertEqual(
                '{\n  "alpha": {\n    "beta": 1\n  }\n}\n',
                output.read_text(encoding="utf-8"),
            )
            self.assertFalse((Path(directory) / "artifact.json.tmp").exists())
            diff = t35.first_json_diff(
                {"alpha": {"beta": 1}},
                {"alpha": {"beta": 2}},
            )
            self.assertEqual("$.alpha.beta: 1 != 2", diff)

    def test_two_census_checks_are_equal_and_write_nothing(self) -> None:
        guarded = (
            t35.INPUTS,
            t35.RUNTIME_REGISTRY,
            t35.EXCLUSION_RECLAIM,
            t35.RECIPE_FAMILIES,
            t35.LOAD_BASELINE,
            t35.MACHINE_TRACK,
            t35.CENSUS,
            t35.CARD_TOPOLOGY,
            t35.READINESS,
        )
        if any(not path.is_file() for path in guarded):
            self.skipTest("T35R artifacts are not generated yet")
        before = {path: _digest(path) for path in guarded}
        command = [sys.executable, "tools/build_t35_census.py", "--check"]
        first = subprocess.Popen(
            command,
            cwd=ROOT,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
        )
        second = subprocess.Popen(
            command,
            cwd=ROOT,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
        )
        first_stdout, first_stderr = first.communicate()
        second_stdout, second_stderr = second.communicate()
        self.assertEqual(0, first.returncode, first_stderr)
        self.assertEqual(0, second.returncode, second_stderr)
        self.assertEqual(first_stdout, second_stdout)
        self.assertEqual(first_stderr, second_stderr)
        self.assertEqual(before, {path: _digest(path) for path in guarded})


if __name__ == "__main__":
    unittest.main()
