"""Semantic ID map loader contract. Tests iterate the archive map."""
from __future__ import annotations

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import semantic_ids
from tools.recipe_bulk.membership import membership_root


class SemanticIdMapTest(unittest.TestCase):
    def test_map_has_nineteen_publication_groups(self) -> None:
        groups = semantic_ids.publication_groups()
        self.assertEqual(19, len(groups))
        self.assertEqual(19, len(set(groups.values())))
        for old, new in groups.items():
            self.assertEqual(new, semantic_ids.remap_publication_group(old))
            self.assertEqual(new, semantic_ids.remap_publication_group(new))
            self.assertNotEqual(old, new)

    def test_remapped_groups_use_host_paths(self) -> None:
        for new in semantic_ids.publication_groups().values():
            path = new.split(":", 1)[-1]
            self.assertIn("/", path)

    def test_routing_schema_collapses_to_one_live_name(self) -> None:
        live = semantic_ids.routing_schema_new()
        self.assertTrue(live)
        self.assertTrue(live.startswith("compact-"))
        for old in semantic_ids.routing_schema_old():
            self.assertEqual(live, semantic_ids.remap_routing_schema(old))
        self.assertEqual(live, semantic_ids.remap_routing_schema(live))

    def test_policy_resource_moves_into_shared_generated_root(self) -> None:
        roots = semantic_ids.load_map()["policy_resource_roots"]
        old_root, new_root = next(iter(roots.items()))
        old_name, new_name = next(
            iter(semantic_ids.load_map()["policy_filenames"].items())
        )
        remapped = semantic_ids.remap_policy_resource(
            f"{old_root}resources/data/cruciblecraft/recipe/publication_policy/{old_name}"
        )
        self.assertTrue(remapped.startswith(str(new_root)))
        self.assertTrue(remapped.endswith(new_name))

    def test_runtime_group_drops_wave_id(self) -> None:
        old_group, new_group = next(iter(semantic_ids.publication_groups().items()))
        remapped = semantic_ids.remap_runtime_group(
            {
                "publication_group": old_group,
                "eager_stable_ids": [],
                "policy_resource": "src/keep/policy.json",
                "wave_id": "legacy",
                "membership_root_sha256": "0" * 64,
            }
        )
        self.assertEqual(new_group, remapped["publication_group"])
        self.assertNotIn("wave_id", remapped)

    def test_membership_root_changes_when_stable_ids_change(self) -> None:
        old_group, new_group = next(iter(semantic_ids.publication_groups().items()))
        prefixes = semantic_ids._prefix_rows("stable_id_prefixes")
        old_prefix = str(prefixes[0]["old"])
        new_prefix = semantic_ids.remap_stable_id(old_prefix + "deadbeef")
        self.assertNotEqual(old_prefix + "deadbeef", new_prefix)
        family = ["gt.recipe.example#0001"]
        before = membership_root(family, [old_prefix + "deadbeef"])
        after = membership_root(family, [new_prefix])
        self.assertNotEqual(before, after)
        self.assertEqual(64, len(after))
        self.assertNotEqual(old_group, new_group)


if __name__ == "__main__":
    unittest.main()
