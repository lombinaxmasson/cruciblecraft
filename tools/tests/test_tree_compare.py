from __future__ import annotations

import tempfile
import unittest
from pathlib import Path

from tools import tree_compare


class TreeCompareTest(unittest.TestCase):
    def test_equal_trees_compare_by_path_and_bytes(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            left = root / "left"
            right = root / "right"
            (left / "nested").mkdir(parents=True)
            (right / "nested").mkdir(parents=True)
            (left / "nested" / "value.json").write_bytes(b'{"value":1}\n')
            (right / "nested" / "value.json").write_bytes(b'{"value":1}\n')
            self.assertEqual([], tree_compare.compare_trees(left, right))

    def test_reports_missing_extra_and_changed_files(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            left = root / "left"
            right = root / "right"
            left.mkdir()
            right.mkdir()
            (left / "changed").write_bytes(b"left")
            (right / "changed").write_bytes(b"right")
            (left / "missing").write_bytes(b"missing")
            (right / "extra").write_bytes(b"extra")
            self.assertEqual(
                [
                    "missing from second tree: missing",
                    "extra in second tree: extra",
                    "file bytes differ: changed",
                ],
                tree_compare.compare_trees(left, right),
            )

    def test_ignored_prefixes_are_excluded(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            left = root / "left"
            right = root / "right"
            (left / "keep").mkdir(parents=True)
            (right / "keep").mkdir(parents=True)
            (left / "models" / "gt_block").mkdir(parents=True)
            (left / "keep" / "same.json").write_bytes(b"{}\n")
            (right / "keep" / "same.json").write_bytes(b"{}\n")
            (left / "models" / "gt_block" / "stub.json").write_bytes(b"left\n")
            self.assertEqual(
                [],
                tree_compare.compare_trees(
                    left,
                    right,
                    ignored_prefixes=("models/gt_block/",),
                ),
            )


if __name__ == "__main__":
    unittest.main()
