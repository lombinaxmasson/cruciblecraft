#!/usr/bin/env python3
"""Contract tests for unified recipe-wave shadow parity."""
from __future__ import annotations

import json
import unittest
from pathlib import Path

from tools.recipe_bulk.emit import semantic_replay_key
from tools.recipe_bulk import shadow as shadow_mod
from tools.recipe_bulk.waves import SHADOW_ORDER
from tools.recipe_bulk.write_guard import ProductionWriteError, assert_not_production_write
from tools import assembler_compact_common as assembler
from tools import block_object_common as block_object

ROOT = Path(__file__).resolve().parents[2]
FIXTURES = ROOT / "src/test/resources/recipe_wave_shadow_fixtures"


class RecipeWaveShadowParityTest(unittest.TestCase):
    def test_semantic_replay_key_covers_every_ordered_relation(self) -> None:
        document = {
            "family_id": "gt.recipe.roaster#0001",
            "publication_group": None,
            "target_map": "cruciblecraft:roaster",
            "relations": [
                {"duration": 1, "shadow_order": 0, "eut": 16},
                {"duration": 2, "shadow_order": 1, "eut": 16},
            ],
        }
        key = semantic_replay_key(document)
        self.assertEqual(2, len(key["relations"]))
        self.assertEqual(1, key["relations"][0]["duration"])
        self.assertEqual(2, key["relations"][1]["duration"])

    def test_negative_fixtures_fail_closed(self) -> None:
        for name in shadow_mod.NEGATIVE_FIXTURE_CHECKS:
            path = FIXTURES / f"{name}.json"
            with self.subTest(name=name):
                document = json.loads(path.read_text(encoding="utf-8"))
                reason = shadow_mod.evaluate_negative_fixture(name, document)
                self.assertEqual(shadow_mod.NEGATIVE_FIXTURE_CHECKS[name], reason)

    def test_shadow_does_not_write_production_trees_and_uses_unified_compile(self) -> None:
        import tools.recipe_bulk.shadow as shadow
        import tools.recipe_bulk.adapters as adapters

        self.assertFalse(hasattr(shadow, "write_tree"))
        self.assertNotIn("write_tree", shadow.__dict__)
        self.assertNotIn("tools.build_assembler_compact_recipes", getattr(adapters, "__dict__", {}))
        self.assertNotIn("tools.build_roaster_compact_recipes", getattr(adapters, "__dict__", {}))
        self.assertNotIn("tools.build_assembler_wood_recipes", getattr(adapters, "__dict__", {}))
        self.assertIn("compile_wave", adapters.__dict__)

    def test_shadow_cannot_write_production_trees(self) -> None:
        with self.assertRaises(ProductionWriteError):
            assert_not_production_write(assembler.generated_family_files()[0])
        with self.assertRaises(ProductionWriteError):
            assert_not_production_write(block_object.generated_family_files()[0])

    def test_shadow_order_is_semantic_reverse_dependency_order(self) -> None:
        self.assertEqual(
            (
                "block/object",
                "smelter/stone",
                "assembler/wood",
                "electrolyzer/compact",
                "centrifuge/compact",
                "roaster/compact",
                "assembler/compact",
            ),
            SHADOW_ORDER,
        )

    def test_block_and_roaster_shadow_match_production(self) -> None:
        block_row = shadow_mod.compare_wave("block/object")
        self.assertTrue(block_row["ok"], block_row.get("mismatches"))
        self.assertTrue(block_row["byte_identity"])
        roaster_row = shadow_mod.compare_wave("roaster/compact")
        self.assertTrue(roaster_row["ok"], roaster_row.get("mismatches"))
        self.assertGreater(roaster_row["relation_count"], roaster_row["family_count"])

    def test_full_shadow_rebuild_is_deterministic(self) -> None:
        document = shadow_mod.compare_all(persist_ir=False)
        self.assertTrue(document["ok"], document)
        self.assertTrue(document["rebuilds_identical"])
        self.assertEqual(list(SHADOW_ORDER), document["shadow_order"])
        for wave_id in SHADOW_ORDER:
            row = document["waves"][wave_id]
            self.assertTrue(row["ok"], row.get("mismatches"))
            self.assertEqual(row["semantic_root_sha256"], row["production_semantic_root_sha256"])
            self.assertEqual(row["stable_id_sha256"], row["production_stable_id_sha256"])


if __name__ == "__main__":
    unittest.main()
