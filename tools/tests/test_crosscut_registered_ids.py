#!/usr/bin/env python3
"""Live recipes and tags must not cite a withdrawn dummy id."""
from __future__ import annotations

import json
import tempfile
import unittest
from pathlib import Path

from tools import crosscut_lint as lint

WITHDRAWN = "cruciblecraft:redstone_wire/red_alloy"


class CrosscutRegisteredIdsTest(unittest.TestCase):
    def test_current_tree_has_no_withdrawn_dummy(self) -> None:
        errors = lint.guarded("registered_ids", lint.withdrawn_dummy_errors())
        self.assertEqual([], errors)

    def test_withdrawn_dummy_id_fails(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            path = (
                root
                / "src"
                / "main"
                / "resources"
                / "data"
                / "cruciblecraft"
                / "recipe"
                / "fake_wire.json"
            )
            path.parent.mkdir(parents=True)
            path.write_text(
                json.dumps(
                    {
                        "type": "minecraft:crafting_shapeless",
                        "ingredients": [{"item": WITHDRAWN}],
                        "result": {"count": 1, "id": "cruciblecraft:red_alloy/wire"},
                    }
                ),
                encoding="utf-8",
            )
            errors = lint.withdrawn_dummy_errors(root)
        self.assertEqual(1, len(errors))
        self.assertIn(WITHDRAWN, errors[0])
        self.assertIn("cruciblecraft:red_alloy/wire", errors[0])


if __name__ == "__main__":
    unittest.main()
