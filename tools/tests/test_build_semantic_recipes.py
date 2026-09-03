from __future__ import annotations

import unittest

from tools import build_semantic_recipes


class SemanticRecipeBuilderTest(unittest.TestCase):
    def test_semantic_wave_compares_parsed_documents_directly(self) -> None:
        self.assertEqual(
            [],
            build_semantic_recipes.compare_wave(
                "compressor/ordinary-closure"
            ),
        )
        self.assertEqual(
            [],
            build_semantic_recipes.compare_wave("mixer/ordinary-closure"),
        )

    def test_report_contains_only_recipe_semantics(self) -> None:
        expected = build_semantic_recipes.expected_documents(
            "compressor/ordinary-closure"
        )
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
