from __future__ import annotations

import unittest

from tools import build_semantic_recipes
from tools.recipe_bulk.waves import recipe_wave


def _documents(wave: str) -> dict:
    spec = recipe_wave(wave)
    if spec.source_path.is_file():
        return build_semantic_recipes.expected_documents(wave)
    return build_semantic_recipes.committed_documents(wave)


class SemanticRecipeBuilderTest(unittest.TestCase):
    def test_semantic_wave_compares_parsed_documents_directly(self) -> None:
        for wave in ("compressor/ordinary-closure", "mixer/ordinary-closure"):
            spec = recipe_wave(wave)
            if spec.source_path.is_file():
                self.assertEqual([], build_semantic_recipes.compare_wave(wave))
                continue
            self.assertTrue(
                build_semantic_recipes.committed_documents(wave),
                wave,
            )

    def test_report_contains_only_recipe_semantics(self) -> None:
        expected = _documents("compressor/ordinary-closure")
        self.assertTrue(expected)
        forbidden = {
            "inputs_" + "sha256",
            "builder_" + "sha256",
            "semantic_root_" + "sha256",
        }
        self.assertFalse(
            any(
                key.lower() in forbidden
                for document in expected.values()
                for key in document
            ),
            "top-level recipe documents must contain recipe semantics only",
        )


if __name__ == "__main__":
    unittest.main()
