"""Contract tests for the T39 Centrifuge compact recipe generator."""
from __future__ import annotations

import json
import sys
import unittest
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_t39_centrifuge_recipes as builder  # noqa: E402
from tools import t39_common as common  # noqa: E402


class T39CentrifugeRecipesTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        planned = builder.planned_documents()
        cls.recipes, cls.sidecars = builder.split_planned(planned)
        cls.documents = {
            name: json.loads(content) for name, content in cls.recipes.items()
        }
        cls.equivalence = json.loads(cls.sidecars[builder.EQUIVALENCE.name])

    def test_check_current(self) -> None:
        if not common.GENERATED_ROOT.is_dir():
            self.skipTest("T39 generated centrifuge recipes are not committed")
        self.assertEqual([], builder.check())

    def test_exactly_production_locked_families_and_relations(
            self,
    ) -> None:
        self.assertEqual(common.production_family_count(), len(self.recipes))
        relation_count = sum(
            len(document["relations"]) for document in self.documents.values()
        )
        self.assertEqual(common.production_relation_count(), relation_count)
        self.assertEqual(
            common.production_family_count(),
            self.equivalence["generated"]["file_count"],
        )
        self.assertEqual(
            common.production_relation_count(),
            self.equivalence["source"]["relation_count"],
        )

    def test_publication_groups_match_production_lock(
            self,
    ) -> None:
        allowed = {common.SINGLETON_GROUP, common.MULTI_GROUP}
        singleton = 0
        multi = 0
        for document in self.documents.values():
            group = document["publication_group"]
            self.assertIn(group, allowed)
            if group == common.SINGLETON_GROUP:
                singleton += 1
                self.assertEqual(1, len(document["relations"]))
            else:
                multi += 1
                self.assertGreater(len(document["relations"]), 1)
        counts = common.production_group_counts()
        self.assertEqual(counts[common.SINGLETON_GROUP]["families"], singleton)
        self.assertEqual(counts[common.MULTI_GROUP]["families"], multi)

    def test_production_relations_do_not_emit_legacy_dye_ids(self) -> None:
        for document in self.documents.values():
            for relation in document["relations"]:
                for stack in relation.get("item_outputs") or []:
                    self.assertNotEqual("minecraft:dye", stack.get("id"))
                for ingredient in relation.get("item_inputs") or []:
                    self.assertNotEqual("minecraft:dye", ingredient.get("item"))

    def test_copper_nugget_uses_registered_cc_item(self) -> None:
        found = False
        for document in self.documents.values():
            for relation in document["relations"]:
                for stack in relation.get("item_outputs") or []:
                    self.assertNotEqual("minecraft:copper_nugget", stack.get("id"))
                    if stack.get("id") == "cruciblecraft:copper/nugget":
                        found = True
        self.assertTrue(found)

    def test_equivalence_sidecar_hashes_match_generated_tree(self) -> None:
        if not common.GENERATED_ROOT.is_dir():
            self.skipTest("T39 generated centrifuge recipes are not committed")
        committed = json.loads(builder.EQUIVALENCE.read_text(encoding="utf-8"))
        self.assertEqual(
            committed["generated"]["files"],
            self.equivalence["generated"]["files"],
        )
        self.assertEqual(
            committed["generated"]["sha256"],
            self.equivalence["generated"]["sha256"],
        )
        self.assertEqual(
            committed["runtime_expected"]["stable_ids"],
            self.equivalence["runtime_expected"]["stable_ids"],
        )
        self.assertEqual(
            committed["relation_fingerprints"],
            self.equivalence["relation_fingerprints"],
        )
        for filename, expected_hash in self.equivalence["generated"]["files"].items():
            path = common.GENERATED_ROOT / filename
            self.assertTrue(path.is_file(), filename)
            self.assertEqual(expected_hash, builder.digest_file(path))

    def test_fluids_use_minecraft_or_cruciblecraft_namespaces(self) -> None:
        for document in self.documents.values():
            for relation in document["relations"]:
                for stack in relation["fluid_inputs"] + relation["fluid_outputs"]:
                    fluid_id = stack["id"]
                    self.assertTrue(
                        fluid_id.startswith("minecraft:")
                        or fluid_id.startswith("cruciblecraft:"),
                        fluid_id,
                    )
                    self.assertGreater(stack["amount"], 0)

    def test_documented_cross_family_consume_shadow_notes_are_exhaustive(
            self,
    ) -> None:
        notes = self.equivalence.get("cross_family_consume_identity_shadow_notes") or {}
        collisions: dict[str, list[tuple[str, str, str]]] = defaultdict(list)
        for document in self.documents.values():
            for relation in document["relations"]:
                digest = builder.consume_identity(relation)
                collisions[digest].append(
                    (
                        document["family_id"],
                        relation["stable_id"],
                        document["publication_group"],
                    )
                )
        duplicate_groups = {
            digest: rows for digest, rows in collisions.items() if len(rows) > 1
        }
        self.assertEqual({}, notes)
        self.assertEqual({}, duplicate_groups)
        for digest, rows in duplicate_groups.items():
            documented = notes[digest]
            self.assertEqual(
                {row["stable_id"] for row in documented},
                {row[1] for row in rows},
            )
            self.assertEqual(
                {row["template_key"] for row in documented},
                {row[0] for row in rows},
            )
            self.assertEqual(len(rows), len(documented))
            self.assertGreater(len({row[0] for row in rows}), 1)
            self.assertEqual(len({row[1] for row in rows}), len(rows))

    def test_stable_ids_are_unique_and_t39_scoped(self) -> None:
        stable_ids = [
            relation["stable_id"]
            for document in self.documents.values()
            for relation in document["relations"]
        ]
        self.assertEqual(common.production_relation_count(), len(stable_ids))
        self.assertEqual(common.production_relation_count(), len(set(stable_ids)))
        self.assertTrue(
            all(stable_id.startswith("cruciblecraft:t39/") for stable_id in stable_ids)
        )

    def test_second_generation_is_byte_stable(self) -> None:
        self.assertEqual(builder.planned_documents(), builder.planned_documents())

    def test_withdrawn_catalog_generation_remains_test_only_and_complete(self) -> None:
        planned = builder.planned_documents("catalog")
        recipes, _sidecars = builder.split_planned(planned)
        documents = [json.loads(content) for content in recipes.values()]
        self.assertEqual(common.CATALOG_FAMILY_COUNT, len(documents))
        self.assertEqual(
            common.CATALOG_RELATION_COUNT,
            sum(len(document["relations"]) for document in documents),
        )
        self.assertTrue(
            all(
                relation["stable_id"].startswith("cruciblecraft:t39_catalog/")
                for document in documents
                for relation in document["relations"]
            )
        )

    def test_check_is_read_only(self) -> None:
        if not common.GENERATED_ROOT.is_dir():
            self.skipTest("T39 generated centrifuge recipes are not committed")
        recipe_before = {
            path.name: path.read_bytes()
            for path in common.GENERATED_ROOT.glob("*.json")
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
                for path in common.GENERATED_ROOT.glob("*.json")
            },
        )
        self.assertEqual(
            sidecar_before,
            {path: path.read_bytes() for path in sidecar_paths},
        )


if __name__ == "__main__":
    unittest.main()
