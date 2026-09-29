#!/usr/bin/env python3
"""Live recipes must not use a stand-in item for a different GT6 part."""
from __future__ import annotations

import json
import tempfile
import unittest
from pathlib import Path

from tools import crosscut_lint as lint


def _write(root: Path, relative: str, document: dict) -> None:
    path = root / relative
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(document), encoding="utf-8")


class CrosscutRecipeStandInTest(unittest.TestCase):
    def test_current_tree_has_no_recipe_stand_in(self) -> None:
        errors = lint.guarded("recipe", lint.recipe_stand_in_errors())
        self.assertEqual([], errors)

    def test_sole_programmed_circuit_fails(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            _write(
                root,
                "src/main/resources/data/cruciblecraft/recipe/fake_circuit.json",
                {
                    "type": "minecraft:crafting_shapeless",
                    "ingredients": [{"item": "cruciblecraft:programmed_circuit"}],
                    "result": {"count": 1, "id": "cruciblecraft:circuit_board"},
                },
            )
            errors = lint.recipe_stand_in_errors(root)
        self.assertEqual(
            [
                "src/main/resources/data/cruciblecraft/recipe/fake_circuit.json: "
                "recipe stand-in cruciblecraft:programmed_circuit"
            ],
            errors,
        )

    def test_pipe_table_flat_plate_fails_and_curved_plate_passes(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            _write(
                root,
                "src/main/resources/data/cruciblecraft/recipe/pipe/table/iron/tiny_fluid_pipe.json",
                {
                    "type": "cruciblecraft:shaped_catalyst",
                    "ingredients": {"P": {"items": "cruciblecraft:iron/plate"}},
                    "result": {"count": 1, "id": "cruciblecraft:iron/tiny_fluid_pipe"},
                },
            )
            _write(
                root,
                "src/main/resources/data/cruciblecraft/recipe/pipe/table/iron/small_fluid_pipe.json",
                {
                    "type": "cruciblecraft:shaped_catalyst",
                    "ingredients": {"P": {"items": "cruciblecraft:curved_plate"}},
                    "result": {"count": 1, "id": "cruciblecraft:iron/small_fluid_pipe"},
                },
            )
            errors = lint.recipe_stand_in_errors(root)
        self.assertEqual(1, len(errors))
        self.assertIn("tiny_fluid_pipe.json", errors[0])
        self.assertIn("plate stand-in", errors[0])


if __name__ == "__main__":
    unittest.main()
