#!/usr/bin/python3
"""Data-driven WaveSpec table for historical T37–T45 recipe waves."""
from __future__ import annotations

from tools import t35_common as t35
from tools import t37_common as t37
from tools import t38_common as t38
from tools import t39_common as t39
from tools import t40_common as t40
from tools import t41_common as t41
from tools import t43_common as t43
from tools import t44_common as t44
from tools import t45_common as t45
from tools.recipe_bulk.models import WaveSpec

SHADOW_ORDER = ("T45", "T43", "T41", "T40", "T39", "T38", "T37")
COMPILE_ORDER = ("T37", "T38", "T39", "T40", "T41", "T43", "T45")

T37_OPERAND_MAP = t35.TOOLS / "t37_operand_runtime_map.json"

WAVES: dict[str, WaveSpec] = {
    "T37": WaveSpec(
        wave_id="T37",
        archetype="no_lock_singleton",
        template_kind="exact_singleton",
        host=t37.HOST,
        target_map="cruciblecraft:assembler",
        source_path=t37.ASSEMBLER_SOURCE,
        generated_root=t37.GENERATED_ROOT,
        equivalence_path=t37.EQUIVALENCE,
        selection_policy="all",
        publication_policy="omit",
        path_layout="flat",
        compile_authority="recipe_bulk",
        relation_sort="template_key",
        stable_id_policy="hex_suffix",
        target_map_policy="relation",
        source_kind_policy="operand_design_first",
        operand_map_path=T37_OPERAND_MAP,
        stable_id_prefix="t37",
        expected_family_count=t37.T37_FAMILY_COUNT,
        expected_relation_count=t37.T37_FAMILY_COUNT,
    ),
    "T38": WaveSpec(
        wave_id="T38",
        archetype="no_lock_relation_set",
        template_kind="exact_relation_set",
        host=t38.HOST,
        target_map=t38.TARGET_MAP,
        source_path=t38.SOURCE,
        generated_root=t38.GENERATED_ROOT,
        equivalence_path=t38.EQUIVALENCE,
        selection_policy="all",
        publication_policy="omit",
        path_layout="flat",
        compile_authority="recipe_bulk",
        relation_sort="source_recipe_index_then_stable_id",
        stable_id_policy="source",
        target_map_policy="spec",
        source_kind_policy="operand_derived_or_backed",
        operand_map_path=t38.OPERAND_RUNTIME_MAP,
        expected_family_count=t38.FAMILY_COUNT,
        expected_relation_count=t38.SOURCE_ROWS,
    ),
    "T39": WaveSpec(
        wave_id="T39",
        archetype="lock_relation_set",
        template_kind="exact_relation_set",
        host=t39.HOST,
        target_map=t39.TARGET_MAP,
        source_path=t39.SOURCE,
        generated_root=t39.GENERATED_ROOT,
        equivalence_path=t39.EQUIVALENCE,
        selection_policy="lock_templates",
        publication_policy="expanded_count",
        path_layout="flat",
        compile_authority="recipe_bulk",
        relation_sort="source_recipe_index_then_stable_id",
        stable_id_policy="source",
        target_map_policy="spec",
        source_kind_policy="operand_derived_or_backed",
        lock_path=t39.PRODUCTION_LOCK,
        operand_map_path=t39.OPERAND_RUNTIME_MAP,
        singleton_publication_group=t39.SINGLETON_GROUP,
        multi_publication_group=t39.MULTI_GROUP,
        expected_family_count=t39.production_family_count(),
        expected_relation_count=t39.production_relation_count(),
    ),
    "T40": WaveSpec(
        wave_id="T40",
        archetype="lock_relation_set",
        template_kind="exact_relation_set",
        host=t40.HOST,
        target_map=t40.TARGET_MAP,
        source_path=t40.SOURCE,
        generated_root=t40.GENERATED_ROOT,
        equivalence_path=t40.EQUIVALENCE,
        selection_policy="lock_templates",
        publication_policy="expanded_count",
        path_layout="flat",
        compile_authority="recipe_bulk",
        relation_sort="source_recipe_index_then_stable_id",
        stable_id_policy="source",
        target_map_policy="spec",
        source_kind_policy="operand_design_first",
        lock_path=t40.PRODUCTION_LOCK,
        operand_map_path=t40.OPERAND_RUNTIME_MAP,
        singleton_publication_group=t40.SINGLETON_GROUP,
        multi_publication_group=t40.MULTI_GROUP,
        combinatorial_template_keys=t40.COMBINATORIAL_TEMPLATE_KEYS,
        expected_family_count=t40.production_family_count(),
        expected_relation_count=t40.production_relation_count(),
    ),
    "T41": WaveSpec(
        wave_id="T41",
        archetype="lock_singleton",
        template_kind="exact_singleton",
        host=t41.HOST,
        target_map=t41.TARGET_MAP,
        source_path=t41.SOURCE,
        generated_root=t41.GENERATED_ROOT,
        equivalence_path=t41.EQUIVALENCE,
        selection_policy="exclude_combinatorial",
        publication_policy="relation",
        path_layout="flat",
        compile_authority="recipe_bulk",
        relation_sort="template_key",
        stable_id_policy="hex_suffix",
        target_map_policy="spec",
        source_kind_policy="operand_design_first",
        lock_path=t41.PRODUCTION_LOCK,
        operand_map_path=t41.OPERAND_RUNTIME_MAP,
        stable_id_prefix="t41",
        combinatorial_template_keys=t41.COMBINATORIAL_TEMPLATE_KEYS,
        expected_family_count=t41.PRODUCTION_FAMILY_COUNT,
        expected_relation_count=t41.PRODUCTION_RELATION_COUNT,
    ),
    "T43": WaveSpec(
        wave_id="T43",
        archetype="lock_singleton",
        template_kind="exact_singleton",
        host=t43.HOST,
        target_map=t43.TARGET_MAP,
        source_path=t43.SOURCE,
        generated_root=t43.GENERATED_ROOT,
        equivalence_path=t43.EQUIVALENCE,
        selection_policy="lock_templates",
        publication_policy="constant",
        path_layout="flat",
        compile_authority="recipe_bulk",
        relation_sort="template_key",
        stable_id_policy="lock",
        target_map_policy="spec",
        source_kind_policy="relation_provenance",
        lock_path=t43.PRODUCTION_LOCK,
        operand_map_path=t43.OPERAND_RUNTIME_MAP,
        default_publication_group=t43.STONE_GROUP,
        expected_family_count=t43.PRODUCTION_FAMILY_COUNT,
        expected_relation_count=t43.PRODUCTION_RELATION_COUNT,
    ),
    "T45": WaveSpec(
        wave_id="T45",
        archetype="lock_singleton",
        template_kind="exact_singleton",
        host=t45.HOST,
        target_map="cruciblecraft:smelter+drying",
        source_path=t45.SOURCE,
        generated_root=t45.GENERATED_ROOT,
        equivalence_path=t45.EQUIVALENCE,
        selection_policy="lock_templates",
        publication_policy="lock",
        path_layout="host_nested",
        compile_authority="recipe_bulk",
        relation_sort="template_key",
        stable_id_policy="lock",
        target_map_policy="lock_host",
        source_kind_policy="relation_provenance",
        lock_path=t45.PRODUCTION_LOCK,
        operand_map_path=t45.OPERAND_RUNTIME_MAP,
        expected_family_count=379,
        expected_relation_count=379,
    ),
}

IDENTITY_ONLY_WAVES: dict[str, WaveSpec] = {
    "T44": WaveSpec(
        wave_id="T44",
        archetype="lock_singleton",
        template_kind="exact_singleton",
        host="cruciblecraft:storage",
        target_map="cruciblecraft:storage",
        source_path=t44.PRODUCTION_LOCK,
        generated_root=t35.ROOT / "src/main/resources/data/cruciblecraft",
        equivalence_path=t44.EQUIVALENCE,
        selection_policy="all",
        publication_policy="omit",
        path_layout="flat",
        compile_authority="recipe_bulk",
        relation_sort="template_key",
        stable_id_policy="lock",
        target_map_policy="spec",
        source_kind_policy="relation_provenance",
        lock_path=t44.PRODUCTION_LOCK,
        identity_only=True,
    ),
}


def recipe_wave(wave_id: str) -> WaveSpec:
    spec = WAVES.get(wave_id)
    if spec is None:
        raise KeyError(f"unknown recipe wave: {wave_id}")
    return spec


def shadow_waves() -> tuple[WaveSpec, ...]:
    return tuple(recipe_wave(wave_id) for wave_id in SHADOW_ORDER)


def compile_waves() -> tuple[WaveSpec, ...]:
    return tuple(recipe_wave(wave_id) for wave_id in COMPILE_ORDER)
