"""Compact-only checks for the T35 recipe-family artifact.

Full dump replay tests intentionally stay in test_build_t35_recipe_families.py
and run only in the census-replay profile.
"""
from __future__ import annotations

import hashlib
import sys
import unittest
from pathlib import Path
from unittest import mock

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_t35_recipe_families as builder  # noqa: E402
from tools import build_t35_recipe_families_compact as compact  # noqa: E402
from tools import t27_common as common  # noqa: E402


def _sha(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


class T35RecipeFamiliesCompactTest(unittest.TestCase):
    def test_entrypoint_runs_reference_only_check(self) -> None:
        with mock.patch.object(compact, "check", return_value=[]) as check:
            self.assertEqual(0, compact.main(["--check"]))
        check.assert_called_once_with()

    def test_compact_check_does_not_read_or_write_dump_maps(self) -> None:
        if not builder.OUTPUT.is_file():
            self.skipTest("t35_recipe_families.json not generated")
        protected = (
            builder.OUTPUT,
            builder.ROW_CLASSIFICATION,
            builder.SHAPE_ANALYSIS,
            builder.MACHINE_PLAYABILITY,
            builder.MIXER_INDEX,
        )
        before = {path: _sha(path) for path in protected}
        self.assertEqual([], compact.check())
        self.assertEqual(before, {path: _sha(path) for path in protected})

    def test_compact_receipt_preserves_expected_counts(self) -> None:
        document = common.load_json(builder.OUTPUT)
        self.assertEqual(78682, document["counts"]["ordinary_optional_rows"])
        self.assertEqual(5718, document["counts"]["families"])
        self.assertTrue((document.get("full_replay") or {}).get("replay_verified"))


if __name__ == "__main__":
    unittest.main()
