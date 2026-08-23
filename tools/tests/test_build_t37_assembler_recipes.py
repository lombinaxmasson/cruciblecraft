"""Contract tests for the T37 Assembler recipe generator."""
from __future__ import annotations

import json
import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_t37_assembler_recipes as builder  # noqa: E402


class T37AssemblerRecipesTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        planned = builder.planned_documents()
        cls.recipes, cls.sidecars = builder.split_planned(planned)
        cls.documents = {
            name: json.loads(content) for name, content in cls.recipes.items()
        }

    def test_exactly_fifty_families_0002_through_0051(self) -> None:
        self.assertEqual(50, len(self.recipes))
        expected = {
            f"gt_recipe_assembler_{number:04d}.json" for number in range(2, 52)
        }
        self.assertEqual(expected, set(self.recipes))
        family_ids = {document["family_id"] for document in self.documents.values()}
        self.assertEqual(
            {f"gt.recipe.assembler#{number:04d}" for number in range(2, 52)},
            family_ids,
        )

    def test_generated_json_omits_parameterized_and_gt_ids(self) -> None:
        texts = list(self.recipes.values())
        joined = "\n".join(texts)
        self.assertNotIn("gregtech:gt.block.planks", joined)
        self.assertNotIn("gregapi:gt.integrated_circuit", joined)
        for document in self.documents.values():
            self.assertNotIn("parameterized", document)
            self.assertEqual(
                "cruciblecraft:compact_gt_recipe_family",
                document["type"],
            )
            relation = document["relations"][0]
            self.assertTrue(relation["stable_id"].startswith("cruciblecraft:t37/"))
            for ingredient, action, count in zip(
                    relation["item_inputs"],
                    relation["item_input_actions"],
                    relation["item_input_counts"],
                    strict=True,
            ):
                if action["kind"] == "consume":
                    item_id = ingredient["item"]
                    self.assertTrue(
                        item_id.startswith("minecraft:")
                        or item_id.startswith("cruciblecraft:"),
                        item_id,
                    )
                    self.assertGreater(count, 0)
                elif action["kind"] == "preserve":
                    self.assertEqual("neoforge:components", ingredient["type"])
                    self.assertEqual(
                        "cruciblecraft:programmed_circuit",
                        ingredient["items"],
                    )
                    self.assertIn(
                        "cruciblecraft:circuit_config",
                        ingredient["components"],
                    )
                    self.assertEqual(0, count)
                else:
                    self.fail(f"unexpected action {action}")

    def test_preserve_circuits_keep_distinct_configs(self) -> None:
        configs = set()
        for document in self.documents.values():
            relation = document["relations"][0]
            for ingredient, action in zip(
                    relation["item_inputs"],
                    relation["item_input_actions"],
                    strict=True,
            ):
                if action["kind"] != "preserve":
                    continue
                configs.add(ingredient["components"]["cruciblecraft:circuit_config"])
        self.assertGreaterEqual(len(configs), 2)

    def test_check_is_read_only(self) -> None:
        if not builder.OUTPUT_ROOT.is_dir():
            self.skipTest("T37 assembler recipes are not generated")
        before = {
            path.name: path.read_bytes()
            for path in builder.OUTPUT_ROOT.glob("*.json")
        }
        sidecar_before = {
            path: path.read_bytes()
            for path in (builder.OPERAND_MAP, builder.PLAYER_PATH, builder.EQUIVALENCE)
            if path.is_file()
        }
        self.assertEqual([], builder.check())
        after = {
            path.name: path.read_bytes()
            for path in builder.OUTPUT_ROOT.glob("*.json")
        }
        self.assertEqual(before, after)
        for path, content in sidecar_before.items():
            self.assertEqual(content, path.read_bytes())

    def test_second_generation_is_byte_stable(self) -> None:
        first = builder.planned_documents()
        second = builder.planned_documents()
        self.assertEqual(first, second)

    def test_player_path_is_complete(self) -> None:
        player_path = json.loads(self.sidecars[builder.PLAYER_PATH.name])
        self.assertEqual(50, player_path["families"])
        self.assertEqual(50, player_path["inputs_reachable"])
        self.assertEqual(50, player_path["outputs_registered"])
        self.assertTrue(player_path["frozen_review_unmodified"])


if __name__ == "__main__":
    unittest.main()
