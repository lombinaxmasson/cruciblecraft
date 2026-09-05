#!/usr/bin/env python3
"""Membership recompute for live semantic rewrite must use matrix relations."""
from __future__ import annotations

import json
import tempfile
import unittest
from pathlib import Path

from tools.recipe_bulk.membership import membership_root
from tools.rewrite_live_semantic_ids import recompute_policy_membership

MATRIX_FAMILY = {
    "authored_form": "matrix_v1",
    "family_id": "gt.recipe.bath#matrix",
    "matrix": {
        "dicts": {
            "fluids": [{"fluid_inputs": [], "fluid_outputs": []}],
            "hashes": [""],
            "item_inputs": [[{"item": "minecraft:iron_ingot"}]],
            "item_outputs": [[{"item": "minecraft:iron_nugget"}]],
        },
        "rows": [
            [0, 0, 0, "stable-a", 0, 0],
            [0, 0, 0, "stable-b", 0, 0],
        ],
        "shared": {
            "can_be_buffered": True,
            "duration": 16,
            "eut": 0,
            "item_input_actions": [{"kind": "consume"}],
            "item_input_counts": [1],
            "output_chances": [],
            "selected_source_recipe": "",
            "source_kind": "gt_recipe",
            "special_value": 0,
        },
    },
    "publication_group": "cruciblecraft:bath/identity/exact_multi",
    "type": "cruciblecraft:compact_gt_recipe_family",
}

POLICY = {
    "cache_ceiling": 2,
    "eager_stable_ids": [],
    "family_count": 1,
    "membership_root_sha256": "0" * 64,
    "policy_type": "on_demand",
    "publication_group": "cruciblecraft:bath/identity/exact_multi",
    "relation_count": 0,
    "routing_schema_version": "compact-shard-v1",
    "target_map": "cruciblecraft:bath",
    "type": "cruciblecraft:compact_publication_policy",
}


class RewriteLiveSemanticIdsTest(unittest.TestCase):
    def test_recompute_counts_matrix_authored_relations(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            (root / "identity").mkdir()
            policy_root = root / "publication_policy"
            policy_root.mkdir()
            (root / "identity" / "family.json").write_text(
                json.dumps(MATRIX_FAMILY) + "\n",
                encoding="utf-8",
            )
            policy_path = policy_root / "bath_identity_exact_multi.json"
            policy_path.write_text(json.dumps(POLICY) + "\n", encoding="utf-8")
            updated = recompute_policy_membership(root)
            self.assertEqual(1, len(updated))
            document = json.loads(policy_path.read_text(encoding="utf-8"))
            self.assertEqual(1, document["family_count"])
            self.assertEqual(2, document["relation_count"])
            self.assertEqual(
                membership_root(
                    ["gt.recipe.bath#matrix"],
                    ["stable-a", "stable-b"],
                ),
                document["membership_root_sha256"],
            )

    def test_recompute_clears_stale_count_when_live_families_are_missing(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            policy_root = root / "publication_policy"
            policy_root.mkdir()
            policy_path = policy_root / "bath_identity_exact.json"
            stale = dict(POLICY)
            stale["publication_group"] = "cruciblecraft:bath/identity/exact"
            stale["family_count"] = 47
            stale["relation_count"] = 47
            policy_path.write_text(json.dumps(stale) + "\n", encoding="utf-8")
            updated = recompute_policy_membership(root)
            self.assertEqual(1, len(updated))
            document = json.loads(policy_path.read_text(encoding="utf-8"))
            self.assertEqual(0, document["family_count"])
            self.assertEqual(0, document["relation_count"])
            self.assertEqual(membership_root([], []), document["membership_root_sha256"])

    def test_recompute_does_not_overwrite_live_bound_wood_counts(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            (root / "wood").mkdir()
            policy_root = root / "publication_policy"
            policy_root.mkdir()
            family = {
                "family_id": "gt.recipe.assembler#w",
                "publication_group": "cruciblecraft:assembler/wood/planks2",
                "relations": [{"stable_id": "s1"}],
                "type": "cruciblecraft:compact_gt_recipe_family",
            }
            (root / "wood" / "family.json").write_text(
                json.dumps(family) + "\n",
                encoding="utf-8",
            )
            policy = dict(POLICY)
            policy["publication_group"] = "cruciblecraft:assembler/wood/planks2"
            policy["family_count"] = 13
            policy["relation_count"] = 13
            policy_path = policy_root / "assembler_planks2.json"
            policy_path.write_text(json.dumps(policy) + "\n", encoding="utf-8")
            updated = recompute_policy_membership(root)
            self.assertEqual([], updated)
            document = json.loads(policy_path.read_text(encoding="utf-8"))
            self.assertEqual(13, document["family_count"])
            self.assertEqual(13, document["relation_count"])


if __name__ == "__main__":
    unittest.main()
