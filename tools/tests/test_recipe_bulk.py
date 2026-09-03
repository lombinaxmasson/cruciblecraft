#!/usr/bin/env python3
"""Contract tests for the reusable recipe bulk compiler."""
from __future__ import annotations

import unittest

from tools.recipe_bulk.emit import hex_stable_id, semantic_replay_key
from tools.recipe_bulk.resolver import ResolutionError, resolve_operand
from tools.recipe_bulk.templates import expand_family
from tools.recipe_bulk import compile as compile_mod
from tools.recipe_bulk import replay as replay_mod
from tools.recipe_bulk.waves import COMPILE_ORDER, WAVES


class RecipeBulkCompilerTest(unittest.TestCase):
    def test_unresolved_does_not_guess_by_display_name(self) -> None:
        result = resolve_operand(
            {
                "source": {
                    "displayName": "Looks Like A Block",
                    "item": "gregtech:gt.block.unknown",
                    "meta": 0,
                }
            }
        )
        self.assertEqual("unresolved", result["class"])
        self.assertIsNone(result["runtime_id"])

    def test_missing_identity_fails_closed_when_proven_required(self) -> None:
        with self.assertRaises(ResolutionError):
            resolve_operand(
                {
                    "source": {
                        "displayName": "Looks Like A Block",
                        "item": "gregtech:gt.block.unknown",
                        "meta": 0,
                    }
                },
                wave_id="T37",
                require_proven=True,
            )

    def test_typed_blocker_fails_closed(self) -> None:
        with self.assertRaises(ResolutionError):
            resolve_operand(
                {"tag": "c:unbound_dummy", "source": {}},
                wave_id="T38",
                index={
                    "records": {},
                    "blockers": {
                        "T38|tag:c:unbound_dummy": {
                            "source_key": "T38|tag:c:unbound_dummy",
                            "mapping_class": "unbound_tag",
                            "blocker_reason": "tag_only",
                            "target_identity": None,
                        }
                    },
                },
                require_proven=True,
            )

    def test_runtime_mismatch_fails_closed(self) -> None:
        with self.assertRaises(ResolutionError):
            resolve_operand(
                {
                    "runtime_id": "minecraft:oak_planks",
                    "source": {"item": "gregtech:gt.block.planks", "meta": 0},
                },
                wave_id="T37",
                index={
                    "records": {
                        "T37|item:gregtech:gt.block.planks@0": {
                            "source_key": "T37|item:gregtech:gt.block.planks@0",
                            "target_identity": "minecraft:spruce_planks",
                            "mapping_class": "exact_item",
                            "evidence": ["SOURCE_DERIVED"],
                        }
                    },
                    "blockers": {},
                },
                require_proven=True,
            )

    def test_component_circuit_projects_neoforge_components(self) -> None:
        result = resolve_operand(
            {
                "source": {
                    "item": "gregapi:gt.integrated_circuit",
                    "meta": 1,
                }
            },
            wave_id="T37",
            require_proven=True,
        )
        self.assertEqual("cruciblecraft:programmed_circuit", result["runtime_id"])
        self.assertEqual({"cruciblecraft:circuit_config": 1}, result["components"])

    def test_stable_id_hex_suffix_transform(self) -> None:
        self.assertEqual(
            "cruciblecraft:t37/a5d684f67018b8a3",
            hex_stable_id("cruciblecraft:gt6/a5d684f67018b8a3", "t37"),
        )

    def test_t37_and_t38_omit_publication_group(self) -> None:
        t37 = compile_mod.planned_documents_for("T37")[0][1]
        t38 = compile_mod.planned_documents_for("T38")[0][1]
        self.assertNotIn("publication_group", t37)
        self.assertNotIn("publication_group", t38)

    def test_legacy_builders_cannot_write_production(self) -> None:
        from tools.recipe_bulk.write_guard import ProductionWriteError
        from tools import build_t37_assembler_recipes as t37
        from tools import build_t41_assembler_recipes as t41
        from tools import build_t43_smelter_recipes as t43

        with self.assertRaises(ProductionWriteError):
            t37.write()
        with self.assertRaises(ProductionWriteError):
            t41.write()
        with self.assertRaises(ProductionWriteError):
            t43.write()

    def test_all_recipe_waves_use_recipe_bulk_compile_authority(self) -> None:
        for wave_id, spec in WAVES.items():
            with self.subTest(wave=wave_id):
                self.assertEqual("recipe_bulk", spec.compile_authority)
                self.assertFalse(hasattr(spec, "emit_delegate"))

    def test_compile_order_covers_seven_historical_waves(self) -> None:
        self.assertEqual(
            ("T37", "T38", "T39", "T40", "T41", "T43", "T45"),
            COMPILE_ORDER,
        )

    def test_forward_compile_order_appends_t46(self) -> None:
        from tools.recipe_bulk.waves import FORWARD_COMPILE_ORDER

        self.assertEqual(COMPILE_ORDER + ("T46", "T47", "T48", "T49"), FORWARD_COMPILE_ORDER)
        self.assertEqual("lock_relation_set", WAVES["T46"].archetype)
        self.assertEqual("exact_relation_set", WAVES["T46"].template_kind)
        self.assertEqual("lock_relation_set", WAVES["T47"].archetype)
        self.assertEqual("exact_relation_set", WAVES["T47"].template_kind)

    def test_exact_singleton_rejects_multi_relation(self) -> None:
        with self.assertRaises(ValueError):
            expand_family(
                family_id="x",
                relations=[{}, {}],
                template_kind="exact_singleton",
            )

    def test_t43_replay_matches_generated_root(self) -> None:
        from tools import t43_common as t43

        if not t43.GENERATED_ROOT.is_dir():
            self.skipTest("T43 generated compact tree is not present")
        document = replay_mod.replay_t43()
        self.assertTrue(document["ok"], document.get("mismatches"))
        self.assertEqual(407, document["compared"])

    def test_semantic_replay_key_compares_complete_relation_sets(self) -> None:
        truncated = {
            "family_id": "gt.recipe.centrifuge#0008",
            "publication_group": "cruciblecraft:t39_centrifuge_multi",
            "target_map": "cruciblecraft:centrifuge",
            "relations": [{"duration": 1, "shadow_order": 0}],
        }
        complete = {
            **truncated,
            "relations": [
                {"duration": 1, "shadow_order": 0},
                {"duration": 2, "shadow_order": 1},
            ],
        }
        self.assertNotEqual(
            semantic_replay_key(truncated),
            semantic_replay_key(complete),
        )

    def test_shared_shape_emits_matrix_and_round_trips(self) -> None:
        from tools.recipe_bulk.matrix import (
            authored_relation_count,
            can_emit_matrix,
            emit_matrix_body,
            expand_matrix,
            wrap_document,
        )

        first = _matrix_relation("cruciblecraft:bath/matrix/first", "minecraft:iron_ingot")
        second = _matrix_relation("cruciblecraft:bath/matrix/second", "minecraft:gold_ingot")
        mixed = dict(second)
        mixed["duration"] = 99
        self.assertTrue(can_emit_matrix([first, second]))
        self.assertFalse(can_emit_matrix([first]))
        self.assertFalse(can_emit_matrix([first, mixed]))
        expanded = expand_matrix(emit_matrix_body([first, second]))
        self.assertEqual(first["stable_id"], expanded[0]["stable_id"])
        self.assertEqual(second["item_inputs"], expanded[1]["item_inputs"])
        self.assertEqual(first["item_outputs"], expanded[0]["item_outputs"])
        self.assertEqual(
            first["provenance"]["evidence_hashes"],
            expanded[0]["provenance"]["evidence_hashes"],
        )
        wrapped = wrap_document(
            {
                "type": "cruciblecraft:compact_gt_recipe_family",
                "family_id": "gt.recipe.bath#matrix",
                "target_map": "cruciblecraft:bath",
                "publication_group": "cruciblecraft:bath/identity/exact_multi",
            },
            [first, second],
        )
        self.assertEqual("matrix_v1", wrapped["authored_form"])
        self.assertNotIn("relations", wrapped)
        self.assertNotIn("parameterized", wrapped)
        self.assertEqual(2, authored_relation_count(wrapped))
        self.assertEqual(
            semantic_replay_key(
                {
                    "family_id": wrapped["family_id"],
                    "publication_group": wrapped["publication_group"],
                    "target_map": wrapped["target_map"],
                    "relations": [first, second],
                }
            ),
            semantic_replay_key(wrapped),
        )
        singleton = wrap_document({"family_id": "gt.recipe.bath#one"}, [first])
        self.assertEqual([first], singleton["relations"])
        self.assertNotIn("matrix", singleton)
        mixed_doc = wrap_document({"family_id": "gt.recipe.bath#mixed"}, [first, mixed])
        self.assertEqual([first, mixed], mixed_doc["relations"])
        self.assertNotIn("matrix", mixed_doc)


def _matrix_relation(stable_id: str, item: str) -> dict:
    return {
        "stable_id": stable_id,
        "item_inputs": [{"item": item}],
        "item_input_counts": [1],
        "item_input_actions": [{"kind": "consume"}],
        "item_outputs": [{"id": "minecraft:iron_nugget", "count": 1}],
        "fluid_inputs": [],
        "fluid_outputs": [],
        "output_chances": [10000],
        "duration": 32,
        "eut": 16,
        "special_value": 0,
        "can_be_buffered": True,
        "shadow_order": 0 if "first" in stable_id else 1,
        "provenance": {
            "source_kind": "SOURCE_BACKED",
            "selected_source_recipe": "gt.recipe.bath#matrix",
            "evidence_hashes": ["ab" * 32],
        },
    }


if __name__ == "__main__":
    unittest.main()
