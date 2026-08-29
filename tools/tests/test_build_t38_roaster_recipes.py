"""Contract tests for the T38 Roaster compact recipe generator."""
from __future__ import annotations

import json
import shutil
import sys
import tempfile
import unittest
from pathlib import Path
from unittest import mock

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_t38_roaster_recipes as builder  # noqa: E402


class T38RoasterRecipesTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        planned = builder.planned_documents()
        cls.recipes, cls.sidecars = builder.split_planned(planned)
        cls.documents = {
            name: json.loads(content) for name, content in cls.recipes.items()
        }

    def test_exactly_twenty_nine_families_without_0002(self) -> None:
        expected_numbers = (0, 1, *range(3, 30))
        expected = {
            f"gt_recipe_roaster_{number:04d}.json"
            for number in expected_numbers
        }
        self.assertEqual(29, len(self.recipes))
        self.assertEqual(expected, set(self.recipes))
        self.assertNotIn("gt_recipe_roaster_0002.json", self.recipes)
        self.assertEqual(
            {
                f"gt.recipe.roaster#{number:04d}"
                for number in expected_numbers
            },
            {document["family_id"] for document in self.documents.values()},
        )

    def test_emits_all_seventy_three_relations_and_0000_has_thirty_eight(self) -> None:
        relation_count = sum(
            len(document["relations"]) for document in self.documents.values()
        )
        self.assertEqual(73, relation_count)
        family_0000 = self.documents["gt_recipe_roaster_0000.json"]
        self.assertEqual(38, len(family_0000["relations"]))

    def test_stable_ids_are_unique_and_t38_scoped(self) -> None:
        stable_ids = [
            relation["stable_id"]
            for document in self.documents.values()
            for relation in document["relations"]
        ]
        self.assertEqual(73, len(stable_ids))
        self.assertEqual(73, len(set(stable_ids)))
        self.assertTrue(
            all(stable_id.startswith("cruciblecraft:t38/") for stable_id in stable_ids)
        )

    def test_generated_json_uses_compact_runtime_ids_and_fluid_stacks(self) -> None:
        source = json.loads(builder.SOURCE.read_text(encoding="utf-8"))
        source_by_stable_id = {
            relation["stable_id"]: relation for relation in source["relations"]
        }
        joined = "\n".join(self.recipes.values())
        self.assertNotIn("gregtech:", joined)
        self.assertNotIn("gregapi:", joined)
        self.assertNotIn("fixed:", joined)
        for document in self.documents.values():
            self.assertEqual(
                "cruciblecraft:compact_gt_recipe_family",
                document["type"],
            )
            self.assertNotIn("parameterized", document)
            for relation in document["relations"]:
                source_relation = source_by_stable_id[relation["stable_id"]]
                if source_relation["fluid_inputs"]:
                    self.assertTrue(relation["fluid_inputs"])
                if source_relation["fluid_outputs"]:
                    self.assertTrue(relation["fluid_outputs"])
                for stack in relation["fluid_inputs"] + relation["fluid_outputs"]:
                    self.assertGreater(stack["amount"], 0)
                    self.assertIn(stack["id"], builder.ALLOWED_FLUIDS)
                for action in relation["item_input_actions"]:
                    self.assertEqual(action["kind"], action["kind"].lower())

    def test_dust_div72_outputs_use_registered_cruciblecraft_ids(self) -> None:
        outputs = [
            stack["id"]
            for document in self.documents.values()
            for relation in document["relations"]
            for stack in relation["item_outputs"]
            if stack["id"].endswith("/dust_div72")
        ]
        self.assertEqual(26, len(outputs))
        self.assertTrue(all(item_id.startswith("cruciblecraft:") for item_id in outputs))
        self.assertTrue(all("/dust_div72" in item_id for item_id in outputs))

    def test_player_path_uses_current_typed_closure_not_namespace(self) -> None:
        sidecar = json.loads(self.sidecars[builder.PLAYER_PATH.name])
        self.assertEqual(73, sidecar["relations"])
        self.assertEqual(73, sidecar["outputs_registered"])
        self.assertEqual(73, sidecar["inputs_reachable"])
        self.assertEqual(0, sidecar["relations_with_unreachable_inputs"])
        self.assertEqual(0, sidecar["unreachable_inputs"])
        self.assertEqual(0, sidecar["unreachable_item_input_occurrences"])
        self.assertEqual(0, sidecar["unreachable_fluid_input_occurrences"])
        self.assertIn("Namespace or fluid whitelist is not player reachability", sidecar["note"])
        self.assertEqual(
            "tools/t21_operand_reachability.json#closure.reachable_identities",
            sidecar["reachability_source"],
        )
        self.assertTrue(all(row["inputs_reachable"] for row in sidecar["rows"]))

    def test_second_generation_is_byte_stable(self) -> None:
        self.assertEqual(builder.planned_documents(), builder.planned_documents())

    def test_check_is_read_only(self) -> None:
        recipe_before = {
            path.name: path.read_bytes()
            for path in builder.OUTPUT_ROOT.glob("*.json")
        }
        sidecar_paths = (
            builder.OPERAND_MAP,
            builder.PLAYER_PATH,
            builder.EQUIVALENCE,
            builder.REQUIRED_FORMS,
        )
        sidecar_before = {path: path.read_bytes() for path in sidecar_paths}
        self.assertEqual([], builder.check())
        self.assertEqual(
            recipe_before,
            {
                path.name: path.read_bytes()
                for path in builder.OUTPUT_ROOT.glob("*.json")
            },
        )
        self.assertEqual(
            sidecar_before,
            {path: path.read_bytes() for path in sidecar_paths},
        )

    def test_write_deletes_stale_t38_root_file(self) -> None:
        tmp = Path(tempfile.mkdtemp(prefix="t38_recipes_"))
        try:
            stale = tmp / "stale_t38_recipe.json"
            stale.write_text("{}\n", encoding="utf-8")
            with mock.patch.multiple(
                builder,
                OUTPUT_ROOT=tmp,
                OPERAND_MAP=tmp / builder.OPERAND_MAP.name,
                PLAYER_PATH=tmp / builder.PLAYER_PATH.name,
                EQUIVALENCE=tmp / builder.EQUIVALENCE.name,
                REQUIRED_FORMS=tmp / builder.REQUIRED_FORMS.name,
            ):
                builder.write()
            self.assertFalse(stale.exists())
        finally:
            shutil.rmtree(tmp, ignore_errors=True)


if __name__ == "__main__":
    unittest.main()
