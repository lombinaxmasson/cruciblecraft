#!/usr/bin/env python3
"""Contract tests for the reusable recipe bulk compiler."""
from __future__ import annotations

import json
import tempfile
import unittest
from pathlib import Path
from unittest import mock

from tools.recipe_bulk.emit import (
    emit_item,
    emit_item_output,
    hex_stable_id,
    project_shared_inventory,
    rewrite_folded_host_runtime,
    semantic_replay_key,
)
from tools.recipe_bulk.resolver import ResolutionError, resolve_operand
from tools.recipe_bulk.templates import expand_family
from tools.recipe_bulk import compile as compile_mod
from tools.recipe_bulk import ordinary_source
from tools.recipe_bulk import replay as replay_mod
from tools.recipe_bulk.compile import semantic_family_count
from tools.recipe_bulk.matrix import authored_relations
from tools.recipe_bulk.transport import (
    assemble_semantic_families,
    reassemble_documents,
    split_document,
)
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
                wave_id="assembler/compact",
                require_proven=True,
            )

    def test_typed_blocker_fails_closed(self) -> None:
        with self.assertRaises(ResolutionError):
            resolve_operand(
                {"tag": "c:unbound_dummy", "source": {}},
                wave_id="roaster/compact",
                index={
                    "records": {},
                    "blockers": {
                        "roaster/compact|tag:c:unbound_dummy": {
                            "source_key": "roaster/compact|tag:c:unbound_dummy",
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
                wave_id="assembler/compact",
                index={
                    "records": {
                        "assembler/compact|item:gregtech:gt.block.planks@0": {
                            "source_key": "assembler/compact|item:gregtech:gt.block.planks@0",
                            "target_identity": "minecraft:spruce_planks",
                            "mapping_class": "exact_item",
                            "evidence": ["SOURCE_DERIVED"],
                        }
                    },
                    "blockers": {},
                },
                require_proven=True,
            )

    def test_public_exchange_dust_stays_unique_item(self) -> None:
        self.assertIsNone(project_shared_inventory("cruciblecraft:copper/dust"))
        self.assertEqual(
            emit_item({"runtime_id": "cruciblecraft:copper/dust"}),
            {"item": "cruciblecraft:copper/dust"},
        )
        self.assertEqual(
            emit_item_output({"runtime_id": "cruciblecraft:iron/plate", "count": 2}),
            {
                "count": 2,
                "id": "cruciblecraft:iron/plate",
            },
        )

    def test_long_tail_inventory_emits_prefix_component(self) -> None:
        self.assertEqual(
            project_shared_inventory("cruciblecraft:copper/crushed_ore"),
            ("cruciblecraft:crushed_ore", {"cruciblecraft:prefix_material": "copper"}),
        )
        self.assertEqual(
            emit_item({"runtime_id": "cruciblecraft:copper/crushed_ore"}),
            {
                "type": "neoforge:components",
                "items": "cruciblecraft:crushed_ore",
                "components": {"cruciblecraft:prefix_material": "copper"},
            },
        )

    def test_public_exchange_leftover_component_json_rewrites_to_unique_item(self) -> None:
        from tools.recipe_bulk.public_exchange import rewrite_document

        self.assertEqual(
            rewrite_document(
                {
                    "type": "neoforge:components",
                    "items": "cruciblecraft:dust",
                    "components": {
                        "cruciblecraft:prefix_material": "copper",
                        "minecraft:max_stack_size": 64,
                    },
                }
            ),
            {"item": "cruciblecraft:copper/dust"},
        )
        self.assertEqual(
            rewrite_document(
                {
                    "id": "cruciblecraft:ingot",
                    "count": 2,
                    "components": {"cruciblecraft:prefix_material": "steel"},
                }
            ),
            {"id": "cruciblecraft:steel/ingot", "count": 2},
        )
        self.assertEqual(
            rewrite_document(
                {
                    "type": "neoforge:components",
                    "items": "cruciblecraft:crushed_ore",
                    "components": {"cruciblecraft:prefix_material": "copper"},
                }
            ),
            {
                "type": "neoforge:components",
                "items": "cruciblecraft:crushed_ore",
                "components": {"cruciblecraft:prefix_material": "copper"},
            },
        )

    def test_unique_hosted_and_external_items_stay_plain(self) -> None:
        self.assertIsNone(project_shared_inventory("cruciblecraft:copper/fluid_pipe"))
        self.assertIsNone(project_shared_inventory("cruciblecraft:copper/wire"))
        self.assertIsNone(project_shared_inventory("minecraft:iron_ingot"))
        self.assertEqual(
            emit_item({"runtime_id": "cruciblecraft:copper/fluid_pipe"}),
            {"item": "cruciblecraft:copper/fluid_pipe"},
        )
        self.assertEqual(
            emit_item({"runtime_id": "minecraft:iron_ingot"}),
            {"item": "minecraft:iron_ingot"},
        )

    def test_component_route_keys_match_java_whitelist(self) -> None:
        from tools.assembler_wood_shard_router import (
            HARD_SHARD_CEILING,
            extract_index_keys,
            prove_route_group,
            route_group,
        )

        keys, unindexed = extract_index_keys(
            {
                "item_inputs": [
                    {
                        "type": "neoforge:components",
                        "items": "cruciblecraft:crushed_ore",
                        "components": {"cruciblecraft:prefix_material": "iron"},
                    }
                ],
                "fluid_inputs": [{"id": "minecraft:water"}],
                "stable_id": "cruciblecraft:bath/identity/tight",
            }
        )
        self.assertFalse(unindexed)
        self.assertEqual(
            [
                "component:cruciblecraft:crushed_ore/cruciblecraft:prefix_material=iron",
                "fluid:minecraft:water",
            ],
            keys,
        )
        _, fat = extract_index_keys(
            {
                "item_inputs": [
                    {
                        "type": "neoforge:components",
                        "items": "cruciblecraft:crushed_ore",
                        "components": {
                            "cruciblecraft:prefix_material": "iron",
                            "minecraft:max_stack_size": 64,
                        },
                    }
                ],
                "stable_id": "cruciblecraft:bath/identity/fat",
            }
        )
        self.assertTrue(fat)
        _, tagged = extract_index_keys(
            {
                "item_inputs": [{"tag": "c:ingots"}],
                "stable_id": "cruciblecraft:bath/identity/tag",
            }
        )
        self.assertTrue(tagged)
        routed = route_group(
            "cruciblecraft:bath",
            "cruciblecraft:bath/identity/exact_multi",
            [
                {
                    "item_inputs": [
                        {
                            "type": "neoforge:components",
                            "items": "cruciblecraft:crushed_ore",
                            "components": {"cruciblecraft:prefix_material": "iron"},
                        }
                    ],
                    "fluid_inputs": [{"id": "minecraft:water"}],
                    "shadow_order": 0,
                    "stable_id": "cruciblecraft:bath/identity/tight",
                }
            ],
        )
        self.assertEqual(0, routed["overflow_count"])
        self.assertLessEqual(routed["worst_shard_size"], HARD_SHARD_CEILING)
        prove_route_group(routed, require_zero_overflow=True)

    def test_folded_dummy_hosts_emit_live_ids(self) -> None:
        self.assertEqual(
            rewrite_folded_host_runtime("cruciblecraft:steam/boiler_tank_lead"),
            "cruciblecraft:lead_boiler",
        )
        self.assertEqual(
            emit_item({"runtime_id": "cruciblecraft:steam/boiler_tank_lead"}),
            {"item": "cruciblecraft:lead_boiler"},
        )
        self.assertEqual(
            emit_item({"runtime_id": "cruciblecraft:electric_wire/1x_tin_wire"}),
            {"item": "cruciblecraft:tin/wire"},
        )
        self.assertEqual(
            emit_item(
                {
                    "runtime_id": (
                        "cruciblecraft:fluid_pipe_tile/tiny_tin_alloy_fluid_pipe"
                    )
                }
            ),
            {"item": "cruciblecraft:tin_alloy/tiny_fluid_pipe"},
        )
        self.assertEqual(
            emit_item(
                {"runtime_id": "cruciblecraft:processing/sanding_machine_steel"}
            ),
            {"item": "cruciblecraft:steel_sanding"},
        )
        self.assertEqual(
            emit_item({"runtime_id": "cruciblecraft:tin/wire"}),
            {"item": "cruciblecraft:tin/wire"},
        )
        self.assertEqual(
            emit_item({"runtime_id": "cruciblecraft:redstone_wire/red_alloy"}),
            {"item": "cruciblecraft:red_alloy/wire"},
        )
        self.assertEqual(
            emit_item({"runtime_id": "cruciblecraft:lumium/wirelamp"}),
            {"item": "cruciblecraft:lumium/wire"},
        )
        self.assertEqual(
            emit_item({"runtime_id": "cruciblecraft:coal_coke"}),
            {"item": "cruciblecraft:coal_coke/gem"},
        )

    def test_unique_catalog_slash_ids_are_not_prefix_projected(self) -> None:
        self.assertIsNone(project_shared_inventory("cruciblecraft:gt_wood/mossy_planks"))
        self.assertIsNone(
            project_shared_inventory(
                "cruciblecraft:furniture/advanced_crafting_table_platinum"
            )
        )
        self.assertIsNone(
            project_shared_inventory("cruciblecraft:ice/cream_basic_milk_gelato")
        )
        self.assertEqual(
            emit_item({"runtime_id": "cruciblecraft:gt_wood/mossy_planks"}),
            {"item": "cruciblecraft:gt_wood/mossy_planks"},
        )
        self.assertEqual(
            emit_item(
                {
                    "runtime_id": (
                        "cruciblecraft:furniture/advanced_crafting_table_platinum"
                    )
                }
            ),
            {"item": "cruciblecraft:furniture/advanced_crafting_table_platinum"},
        )
        self.assertIsNone(project_shared_inventory("cruciblecraft:ice/dust"))

    def test_existing_circuit_components_are_not_overwritten(self) -> None:
        operand = {
            "runtime_id": "cruciblecraft:programmed_circuit",
            "_components": {"cruciblecraft:circuit_config": 1},
        }
        self.assertEqual(
            emit_item(operand),
            {
                "type": "neoforge:components",
                "items": "cruciblecraft:programmed_circuit",
                "components": {"cruciblecraft:circuit_config": 1},
            },
        )
        self.assertEqual(
            emit_item_output(operand),
            {
                "count": 1,
                "id": "cruciblecraft:programmed_circuit",
                "components": {"cruciblecraft:circuit_config": 1},
            },
        )

    def test_component_circuit_projects_neoforge_components(self) -> None:
        result = resolve_operand(
            {
                "source": {
                    "item": "gregapi:gt.integrated_circuit",
                    "meta": 1,
                }
            },
            wave_id="assembler/compact",
            require_proven=True,
        )
        self.assertEqual("cruciblecraft:programmed_circuit", result["runtime_id"])
        self.assertEqual({"cruciblecraft:circuit_config": 1}, result["components"])

    def test_stable_id_hex_suffix_transform(self) -> None:
        self.assertEqual(
            "cruciblecraft:assembler/compact/a5d684f67018b8a3",
            hex_stable_id(
                "cruciblecraft:gt6/a5d684f67018b8a3",
                "assembler/compact",
            ),
        )

    def test_compact_waves_use_semantic_publication_group(self) -> None:
        assembler = compile_mod.planned_documents_for("assembler/compact")[0][1]
        roaster = compile_mod.planned_documents_for("roaster/compact")[0][1]
        self.assertEqual("cruciblecraft:assembler/compact", assembler["publication_group"])
        self.assertEqual("cruciblecraft:roaster/compact", roaster["publication_group"])

    def test_optional_plant_forms_are_not_added_to_base_catalogs(self) -> None:
        catalogs = ordinary_source.load_catalogs(
            {
                "copper": set(ordinary_source.OPTIONAL_PLANT_FORMS),
            }
        )
        registered = catalogs.registered_forms.get("copper", set())
        self.assertTrue(ordinary_source.OPTIONAL_PLANT_FORMS.isdisjoint(registered))

    def test_optional_plant_family_is_reclassified_to_addon_overlay(self) -> None:
        item = ordinary_source.WorkFamily(
            family_id="portfolio:test/plant",
            template_key="gt.recipe.loom#plant",
            record={"expanded_count": 1},
            owner="object_expression/gt_prefix:plantGtBerry",
            relation_count=1,
        )
        verdict = ordinary_source.classify_family(
            item,
            [
                {
                    "eut": 16,
                    "item_inputs": [{"form": "plant_gt_berry"}],
                    "item_input_counts": [1],
                    "item_outputs": [],
                    "fluid_inputs": [],
                    "fluid_outputs": [],
                }
            ],
        )
        self.assertEqual("reclassify", verdict["disposition"])
        self.assertEqual(
            "later:agriculture/plant_form_overlay",
            verdict["future_owner"],
        )

    def test_base_smelter_resources_publish_no_optional_plant_forms(self) -> None:
        root = (
            Path(__file__).resolve().parents[2]
            / "src"
            / "recipe_generated"
            / "resources"
            / "data"
            / "cruciblecraft"
            / "recipe"
            / "smelter"
            / "ordinary_closure"
        )
        self.assertTrue(root.is_dir())
        self.assertFalse(
            any("plant_gt_" in path.read_text(encoding="utf-8") for path in root.rglob("*.json"))
        )
        policy = json.loads(
            (
                root.parent.parent
                / "publication_policy"
                / "smelter_ordinary_closure_gt_prefix.json"
            ).read_text(encoding="utf-8")
        )
        self.assertEqual(25, policy["family_count"])
        self.assertEqual(3150, policy["relation_count"])

    def test_compiler_cannot_write_production(self) -> None:
        from tools.recipe_bulk.write_guard import ProductionWriteError
        from tools.recipe_bulk.waves import recipe_wave

        for wave_id in COMPILE_ORDER:
            with self.subTest(wave=wave_id), self.assertRaises(ProductionWriteError):
                from tools.recipe_bulk.write_guard import assert_not_production_write

                assert_not_production_write(recipe_wave(wave_id).generated_root)

    def test_all_recipe_waves_use_recipe_bulk_compile_authority(self) -> None:
        for wave_id, spec in WAVES.items():
            with self.subTest(wave=wave_id):
                self.assertEqual("recipe_bulk", spec.compile_authority)
                self.assertFalse(hasattr(spec, "emit_delegate"))

    def test_compile_order_covers_seven_historical_waves(self) -> None:
        self.assertEqual(
            (
                "assembler/compact",
                "roaster/compact",
                "centrifuge/compact",
                "electrolyzer/compact",
                "assembler/wood",
                "smelter/stone",
                "block/object",
            ),
            COMPILE_ORDER,
        )

    def test_forward_compile_order_appends_t46(self) -> None:
        from tools.recipe_bulk.waves import FORWARD_COMPILE_ORDER

        self.assertEqual(
            COMPILE_ORDER
            + ("bath/mte", "bath/remainder", "bath/identity", "bath/tiny-purified"),
            FORWARD_COMPILE_ORDER,
        )
        self.assertEqual("lock_relation_set", WAVES["bath/mte"].archetype)
        self.assertEqual("exact_relation_set", WAVES["bath/mte"].template_kind)
        self.assertEqual("lock_relation_set", WAVES["bath/remainder"].archetype)
        self.assertEqual("exact_relation_set", WAVES["bath/remainder"].template_kind)

    def test_exact_singleton_rejects_multi_relation(self) -> None:
        with self.assertRaises(ValueError):
            expand_family(
                family_id="x",
                relations=[{}, {}],
                template_kind="exact_singleton",
            )

    def test_smelter_stone_replay_matches_generated_root(self) -> None:
        from tools import smelter_stone_common as smelter_stone

        if not smelter_stone.GENERATED_ROOT.is_dir():
            self.skipTest("smelter/stone generated compact tree is not present")
        document = replay_mod.replay_smelter_stone()
        self.assertTrue(document["ok"], document.get("mismatches"))
        self.assertEqual(407, document["compared"])

    def test_semantic_replay_key_compares_complete_relation_sets(self) -> None:
        truncated = {
            "family_id": "gt.recipe.centrifuge#0008",
            "publication_group": "cruciblecraft:centrifuge/multi",
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

    def test_write_stable_skips_unchanged_bytes(self) -> None:
        from tools import io_common as files

        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "doc.json"
            files.write_stable(path, {"ok": True})
            with mock.patch("tools.atomic_io.write_bytes") as writer:
                files.write_stable(path, {"ok": True})
            writer.assert_not_called()

    def test_write_tree_is_incremental_and_scoped(self) -> None:
        from tools import io_common as files

        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            keep = root / "pilot" / "host" / "gt_recipe_keep.json"
            extra = root / "pilot" / "host" / "gt_recipe_extra.json"
            other = root / "other" / "gt_recipe_other.json"
            document = {
                "family_id": "keep",
                "type": "cruciblecraft:compact_gt_recipe_family",
            }
            files.write_stable(keep, document)
            files.write_stable(extra, {"family_id": "extra", "type": document["type"]})
            files.write_stable(other, {"family_id": "other", "type": document["type"]})
            compile_mod.write_tree(
                [(keep, document)],
                root,
                root,
                path_prefix="pilot/host",
            )
            self.assertTrue(keep.is_file())
            self.assertFalse(extra.is_file())
            self.assertTrue(other.is_file())

    def test_scoped_family_load_skips_other_prefixes(self) -> None:
        from tools import io_common as files
        from tools.recipe_bulk.matrix import load_compact_family_documents

        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            wanted = root / "pilot" / "smelter" / "gt_recipe_keep.json"
            other = root / "smelter" / "ordinary_closure" / "gt_recipe_other.json"
            payload = {
                "family_id": "keep",
                "type": "cruciblecraft:compact_gt_recipe_family",
            }
            files.write_stable(wanted, payload)
            files.write_stable(
                other,
                {
                    "family_id": "other",
                    "type": "cruciblecraft:compact_gt_recipe_family",
                },
            )
            loaded = load_compact_family_documents(root, path_prefix="pilot/smelter")
            self.assertEqual([str(wanted)], list(loaded))

    def test_parameterized_source_fails_closed(self) -> None:
        from tools import io_common as files
        from tools.recipe_bulk.models import WaveSpec

        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            source_path = root / "source.json"
            lock_path = root / "production_lock.json"
            files.write_stable(
                source_path,
                {
                    "relations": [
                        {
                            "family_id": "family-a",
                            "parameterized": True,
                            "source_recipe_index": 0,
                            "stable_id": "cruciblecraft:gt6/dead",
                            "template_key": "gt.recipe.smelter#0001",
                        }
                    ]
                },
            )
            files.write_stable(
                lock_path,
                {
                    "production": {
                        "families": [
                            {
                                "cohort": "pilot",
                                "publication_group": "cruciblecraft:smelter/pilot",
                                "template_key": "gt.recipe.smelter#0001",
                            }
                        ]
                    }
                },
            )
            spec = WaveSpec(
                wave_id="generic-import/param-forbidden",
                archetype="lock_relation_set",
                template_kind="exact_relation_set",
                host="cruciblecraft:smelter",
                target_map="cruciblecraft:smelter",
                source_path=source_path,
                generated_root=root / "recipe_generated",
                equivalence_path=root / "equivalence.json",
                selection_policy="lock_templates",
                publication_policy="lock",
                path_layout="cohort_nested",
                compile_authority="recipe_bulk",
                relation_sort="source_recipe_index_then_stable_id",
                stable_id_policy="lock",
                target_map_policy="spec",
                source_kind_policy="relation_provenance",
                lock_path=lock_path,
                wave_slug="generic-import/param-forbidden",
                cohort="pilot",
                path_prefix="pilot/smelter/param_forbidden",
            )
            with self.assertRaisesRegex(ValueError, "parameterized source must not compile"):
                compile_mod.planned_documents_for(
                    "generic-import/param-forbidden",
                    spec=spec,
                )


class CompactTransportSplitTest(unittest.TestCase):
    def test_under_ceiling_stays_unsplit(self) -> None:
        document = {
            "family_id": "gt.recipe.bath#tiny",
            "publication_group": "cruciblecraft:bath/test",
            "source_revision": "3703e40308c8c030763fd6297dea8b210d2a77b1",
            "target_map": "cruciblecraft:bath",
            "type": "cruciblecraft:compact_gt_recipe_family",
            "relations": [
                _matrix_relation("cruciblecraft:bath/first", "minecraft:iron_ingot"),
                _matrix_relation("cruciblecraft:bath/second", "minecraft:gold_ingot"),
            ],
        }
        planned = split_document(Path("gt_recipe_bath_tiny.json"), document)
        self.assertEqual(1, len(planned))
        self.assertEqual("gt_recipe_bath_tiny.json", planned[0][0].name)
        self.assertNotIn("transport_fragment", planned[0][1])

    def test_over_ceiling_splits_reassembles_and_is_byte_identical(self) -> None:
        relations = []
        for index in range(4_097):
            row = _matrix_relation(
                f"cruciblecraft:sanding/{index:04x}",
                "minecraft:iron_ingot",
            )
            row["shadow_order"] = index
            relations.append(row)
        document = {
            "family_id": "gt.recipe.sharpener#0000",
            "publication_group": "cruciblecraft:sanding/pilot/sanding",
            "source_revision": "3703e40308c8c030763fd6297dea8b210d2a77b1",
            "target_map": "cruciblecraft:sanding",
            "type": "cruciblecraft:compact_gt_recipe_family",
            "relations": relations,
        }
        first = split_document(Path("gt_recipe_sharpener_0000.json"), document)
        second = split_document(Path("gt_recipe_sharpener_0000.json"), document)
        self.assertGreaterEqual(len(first), 2)
        self.assertEqual(
            [(str(path), doc) for path, doc in first],
            [(str(path), doc) for path, doc in second],
        )
        family_ids = {str(doc.get("family_id")) for _path, doc in first}
        self.assertEqual({"gt.recipe.sharpener#0000"}, family_ids)
        assembled = reassemble_documents([doc for _path, doc in first])
        self.assertNotIn("transport_fragment", assembled)
        self.assertEqual(4_097, len(authored_relations(assembled)))
        self.assertEqual(
            [row["stable_id"] for row in relations],
            [row["stable_id"] for row in authored_relations(assembled)],
        )
        self.assertTrue(
            all(path.name.startswith("gt_recipe_sharpener_0000_fragment_") for path, _ in first)
        )
        self.assertEqual(1, semantic_family_count(first))
        self.assertEqual(
            1,
            len(assemble_semantic_families([doc for _path, doc in first])),
        )

    def test_missing_fragment_fails_closed(self) -> None:
        relations = []
        for index in range(4_097):
            row = _matrix_relation(
                f"cruciblecraft:sanding/{index:04x}",
                "minecraft:iron_ingot",
            )
            row["shadow_order"] = index
            relations.append(row)
        document = {
            "family_id": "gt.recipe.sharpener#0000",
            "publication_group": "cruciblecraft:sanding/pilot/sanding",
            "source_revision": "3703e40308c8c030763fd6297dea8b210d2a77b1",
            "target_map": "cruciblecraft:sanding",
            "type": "cruciblecraft:compact_gt_recipe_family",
            "relations": relations,
        }
        fragments = [doc for _path, doc in split_document(Path("gt_recipe_sharpener_0000.json"), document)]
        with self.assertRaisesRegex(
            ValueError,
            "missing or duplicate fragment|fragment count .* !=",
        ):
            reassemble_documents(fragments[1:])


class RecipeRuleIrTest(unittest.TestCase):
    def test_coverage_mismatch_fails_closed(self) -> None:
        from tools.recipe_bulk import rule_ir as rule_ir_mod
        from tools.recipe_bulk.handlers import tool_head_cycle

        rows = [
            {
                "duration": 16,
                "eut": 16,
                "item_input_actions": [{"kind": "CONSUME"}],
                "item_inputs": [
                    {
                        "form": None,
                        "material": None,
                        "runtime_id": "cruciblecraft:iron/tool_head_raw_hoe",
                        "value": "cruciblecraft:iron/tool_head_raw_hoe",
                    }
                ],
                "item_outputs": [
                    {
                        "form": None,
                        "material": None,
                        "runtime_id": "cruciblecraft:iron/tool_head_hoe",
                        "value": "cruciblecraft:iron/tool_head_hoe",
                    }
                ],
                "provenance": {"kinds": ["SOURCE_BACKED"]},
                "source_row_sha256": "ab" * 32,
                "stable_id": "cruciblecraft:sanding/one",
            }
        ]
        self.assertTrue(tool_head_cycle.matches(rows[0], {}))
        document = {
            "blocked": [],
            "family_id": "gt.recipe.sharpener#0000",
            "handlers": [
                {
                    "covered_source_row_sha256": ["cd" * 32],
                    "handler_id": "sanding_exact_remainder",
                    "kind": "exact_remainder",
                }
            ],
            "schema": "rule_ir_v1",
            "schema_version": 1,
            "source_map": "gt.recipe.sharpener",
            "source_revision": "3703e40308c8c030763fd6297dea8b210d2a77b1",
            "target_map": "cruciblecraft:sanding",
        }
        with self.assertRaisesRegex(ValueError, "remainder hashes missing|coverage drifted"):
            rule_ir_mod.prove_coverage(document, rows)

    def test_handler_overlap_fails_closed(self) -> None:
        from tools.recipe_bulk import rule_ir as rule_ir_mod

        rows = [
            {
                "duration": 16,
                "eut": 16,
                "item_input_actions": [{"kind": "CONSUME"}],
                "item_inputs": [
                    {
                        "form": None,
                        "material": None,
                        "runtime_id": "cruciblecraft:iron/tool_head_raw_hoe",
                        "value": "cruciblecraft:iron/tool_head_raw_hoe",
                    }
                ],
                "item_outputs": [
                    {
                        "form": None,
                        "material": None,
                        "runtime_id": "cruciblecraft:iron/tool_head_hoe",
                        "value": "cruciblecraft:iron/tool_head_hoe",
                    }
                ],
                "provenance": {"kinds": ["SOURCE_BACKED"]},
                "source_row_sha256": "ab" * 32,
                "stable_id": "cruciblecraft:sanding/one",
            }
        ]
        document = {
            "blocked": [],
            "family_id": "gt.recipe.sharpener#0000",
            "handlers": [
                {
                    "handler_id": "sanding_tool_head_cycle",
                    "kind": "tool_head_cycle",
                    "template": {
                        "item_input_count": 1,
                        "item_input_prefix": "tool_head_raw_",
                        "item_output_prefix": "tool_head_",
                    },
                },
                {
                    "covered_source_row_sha256": ["ab" * 32],
                    "handler_id": "sanding_exact_remainder",
                    "kind": "exact_remainder",
                },
            ],
            "schema": "rule_ir_v1",
            "schema_version": 1,
            "source_map": "gt.recipe.sharpener",
            "source_revision": "3703e40308c8c030763fd6297dea8b210d2a77b1",
            "target_map": "cruciblecraft:sanding",
        }
        with self.assertRaisesRegex(ValueError, "overlap"):
            rule_ir_mod.prove_coverage(document, rows)


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
