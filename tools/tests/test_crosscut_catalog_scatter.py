#!/usr/bin/env python3
"""Retired catalog ItemEntity scatters stay at count zero."""
from __future__ import annotations

import tempfile
import unittest
from pathlib import Path

from tools import crosscut_lint as lint


class CrosscutCatalogScatterTest(unittest.TestCase):
    def test_current_tree_has_no_retired_scatter(self) -> None:
        errors = lint.guarded("scatter", lint.scatter_errors())
        self.assertEqual([], errors)

    def test_retired_feature_type_fails(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            java = (
                root
                / "src"
                / "main"
                / "java"
                / "com"
                / "masson"
                / "cruciblecraft"
                / "worldgen"
                / "GtItemScatterFeature.java"
            )
            java.parent.mkdir(parents=True)
            java.write_text(
                "class GtItemScatterFeature { ItemEntity drop() { return new ItemEntity(); } }\n",
                encoding="utf-8",
            )
            errors = lint.scatter_errors(root)
        self.assertTrue(any("GtItemScatterFeature" in error for error in errors))
        self.assertTrue(any("constructs ItemEntity" in error for error in errors))


if __name__ == "__main__":
    unittest.main()
