"""Contract tests for the T40 Electrolyzer compact recipe generator."""
from __future__ import annotations

import json
import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_t40_electrolyzer_recipes as builder  # noqa: E402
from tools import t40_common as common  # noqa: E402


class T40ElectrolyzerRecipesTest(unittest.TestCase):
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
            self.skipTest("T40 generated electrolyzer recipes are not committed")
        self.assertEqual([], builder.check())

    def test_exactly_production_locked_families_and_relations(self) -> None:
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

    def test_publication_groups_match_production_lock(self) -> None:
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

    def test_catalog_fixture_stays_61_151(self) -> None:
        catalog = builder.planned_documents(scope="catalog")
        recipes, _sidecars = builder.split_planned(catalog)
        self.assertEqual(common.CATALOG_FAMILY_COUNT, len(recipes))
        relations = sum(
            len(json.loads(content)["relations"]) for content in recipes.values()
        )
        self.assertEqual(common.CATALOG_RELATION_COUNT, relations)
