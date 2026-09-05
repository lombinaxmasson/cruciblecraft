from __future__ import annotations

import tempfile
import unittest
from pathlib import Path
from unittest import mock

from tools import check_markdown_links as checker


class MarkdownLinkCheckTest(unittest.TestCase):
    def test_current_docs_have_no_dangling_workspace_links(self) -> None:
        self.assertEqual([], checker.check())

    def test_closed_card_plans_are_not_current_docs(self) -> None:
        closed = next(
            path
            for path in (
                checker.ROOT / "docs" / "history" / "card-plans" / "closed"
            ).iterdir()
            if path.suffix == ".md"
        )
        self.assertTrue(checker._is_closed_card_plan(closed))
        self.assertFalse(
            checker._is_closed_card_plan(
                checker.ROOT
                / "docs"
                / "history"
                / "card-plans"
                / "active"
                / "电池详细计划.md"
            )
        )

    def test_missing_relative_link_is_reported(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            source = root / "README.md"
            source.write_text("[gone](missing.md)\n", encoding="utf-8")
            with mock.patch.object(checker, "ROOT", root), mock.patch.object(
                checker,
                "PATH_MAP",
                root / "docs" / "history" / "path-map.json",
            ), mock.patch.object(
                checker,
                "CURRENT_ROOTS",
                (source,),
            ):
                errors = checker.check()
        self.assertEqual(["README.md: missing missing.md"], errors)
