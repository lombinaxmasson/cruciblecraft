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
from tools.recipe_bulk.slugs import WaveSlugError, parse_wave_token

SHADOW_ORDER = ("T45", "T43", "T41", "T40", "T39", "T38", "T37")
COMPILE_ORDER = ("T37", "T38", "T39", "T40", "T41", "T43", "T45")
FORWARD_COMPILE_ORDER = COMPILE_ORDER + ("T46", "T47", "T48", "T49")
SEMANTIC_COMPILE_ORDER = (
    "smelter/ordinary-closure",
    "mixer/ordinary-closure",
    "drying/ordinary-closure",
    "electrolyzer/ordinary-closure",
    "centrifuge/ordinary-closure",
    "autoclave/ordinary-closure",
    "compressor/ordinary-closure",
)
RECIPE_GENERATED_ROOT = (
    t35.ROOT / "src/recipe_generated/resources/data/cruciblecraft/recipe"
)
RECIPE_SUPPORT_ROOT = (
    t35.ROOT / "src/recipe_support_generated/resources/data/cruciblecraft/recipe"
)

T37_OPERAND_MAP = t35.TOOLS / "t37_operand_runtime_map.json"
T47_LOCK_PATH = t35.TOOLS / "t47_production_lock.json"
T47_CANDIDATE_FAMILY_COUNT = 395
T47_CANDIDATE_RELATION_COUNT = 13708
T48_LOCK_PATH = t35.TOOLS / "t48_production_lock.json"
T48_CANDIDATE_FAMILY_COUNT = 145
T48_CANDIDATE_RELATION_COUNT = 34091
T49_LOCK_PATH = (
    t35.TOOLS / "waves" / "bath" / "tiny-purified" / "t49_production_lock.json"
)
T49_CANDIDATE_FAMILY_COUNT = 5
T49_CANDIDATE_RELATION_COUNT = 95


def _t47_expected_counts() -> tuple[int, int]:
    if T47_LOCK_PATH.is_file():
        production = t35.load_json(T47_LOCK_PATH).get("production") or {}
        return int(production["family_count"]), int(production["relation_count"])
    return T47_CANDIDATE_FAMILY_COUNT, T47_CANDIDATE_RELATION_COUNT


_T47_FAMILY_COUNT, _T47_RELATION_COUNT = _t47_expected_counts()


def _t48_expected_counts() -> tuple[int, int]:
    if T48_LOCK_PATH.is_file():
        production = t35.load_json(T48_LOCK_PATH).get("production") or {}
        return int(production["family_count"]), int(production["relation_count"])
    return T48_CANDIDATE_FAMILY_COUNT, T48_CANDIDATE_RELATION_COUNT


_T48_FAMILY_COUNT, _T48_RELATION_COUNT = _t48_expected_counts()


def _t49_expected_counts() -> tuple[int, int]:
    if T49_LOCK_PATH.is_file():
        production = t35.load_json(T49_LOCK_PATH).get("production") or {}
        return int(production["family_count"]), int(production["relation_count"])
    return T49_CANDIDATE_FAMILY_COUNT, T49_CANDIDATE_RELATION_COUNT


_T49_FAMILY_COUNT, _T49_RELATION_COUNT = _t49_expected_counts()

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
    "T46": WaveSpec(
        wave_id="T46",
        archetype="lock_relation_set",
        template_kind="exact_relation_set",
        host="cruciblecraft:bath",
        target_map="cruciblecraft:bath",
        source_path=t35.TOOLS / "t46_bath_source.json",
        generated_root=(
            t35.ROOT / "src/t46_recipe_generated/resources/data/cruciblecraft/recipe/t46"
        ),
        equivalence_path=t35.TOOLS / "t46_equivalence.json",
        selection_policy="lock_templates",
        publication_policy="lock",
        path_layout="host_nested",
        compile_authority="recipe_bulk",
        relation_sort="source_recipe_index_then_stable_id",
        stable_id_policy="lock",
        target_map_policy="spec",
        source_kind_policy="relation_provenance",
        lock_path=t35.TOOLS / "t46_production_lock.json",
        operand_map_path=t35.TOOLS / "t46_operand_runtime_map.json",
        default_publication_group="cruciblecraft:t46_bath_mte",
        expected_family_count=803,
        expected_relation_count=1517,
    ),
    "T47": WaveSpec(
        wave_id="T47",
        archetype="lock_relation_set",
        template_kind="exact_relation_set",
        host="cruciblecraft:bath",
        target_map="cruciblecraft:bath",
        source_path=t35.TOOLS / "t47_bath_source.json",
        generated_root=(
            t35.ROOT / "src/t47_recipe_generated/resources/data/cruciblecraft/recipe/t47"
        ),
        equivalence_path=t35.TOOLS / "t47_equivalence.json",
        selection_policy="lock_templates",
        publication_policy="lock",
        path_layout="host_nested",
        compile_authority="recipe_bulk",
        relation_sort="source_recipe_index_then_stable_id",
        stable_id_policy="lock",
        target_map_policy="spec",
        source_kind_policy="relation_provenance",
        lock_path=T47_LOCK_PATH,
        operand_map_path=t35.TOOLS / "t47_operand_runtime_map.json",
        default_publication_group="cruciblecraft:t47_bath_exact",
        expected_family_count=_T47_FAMILY_COUNT,
        expected_relation_count=_T47_RELATION_COUNT,
    ),
    "T48": WaveSpec(
        wave_id="T48",
        archetype="lock_relation_set",
        template_kind="exact_relation_set",
        host="cruciblecraft:bath",
        target_map="cruciblecraft:bath",
        source_path=t35.TOOLS / "t48_bath_source.json",
        generated_root=(
            t35.ROOT / "src/t48_recipe_generated/resources/data/cruciblecraft/recipe/t48"
        ),
        equivalence_path=t35.TOOLS / "t48_equivalence.json",
        selection_policy="lock_templates",
        publication_policy="lock",
        path_layout="host_nested",
        compile_authority="recipe_bulk",
        relation_sort="source_recipe_index_then_stable_id",
        stable_id_policy="lock",
        target_map_policy="spec",
        source_kind_policy="relation_provenance",
        lock_path=T48_LOCK_PATH,
        operand_map_path=t35.TOOLS / "t48_operand_runtime_map.json",
        default_publication_group="cruciblecraft:t48_bath_exact",
        expected_family_count=_T48_FAMILY_COUNT,
        expected_relation_count=_T48_RELATION_COUNT,
    ),
    "T49": WaveSpec(
        wave_id="T49",
        archetype="lock_relation_set",
        template_kind="exact_relation_set",
        host="cruciblecraft:bath",
        target_map="cruciblecraft:bath",
        source_path=(
            t35.TOOLS / "waves" / "bath" / "tiny-purified" / "t49_bath_source.json"
        ),
        generated_root=(
            t35.ROOT
            / "src/recipe_generated/resources/data/cruciblecraft/recipe/bath/tiny_purified"
        ),
        equivalence_path=(
            t35.TOOLS / "waves" / "bath" / "tiny-purified" / "t49_equivalence.json"
        ),
        selection_policy="lock_templates",
        publication_policy="lock",
        path_layout="host_nested",
        compile_authority="recipe_bulk",
        relation_sort="source_recipe_index_then_stable_id",
        stable_id_policy="lock",
        target_map_policy="spec",
        source_kind_policy="relation_provenance",
        lock_path=T49_LOCK_PATH,
        operand_map_path=(
            t35.TOOLS / "waves" / "bath" / "tiny-purified" / "t49_operand_runtime_map.json"
        ),
        default_publication_group="cruciblecraft:t49_bath_exact_multi",
        expected_family_count=_T49_FAMILY_COUNT,
        expected_relation_count=_T49_RELATION_COUNT,
    ),
}

def _semantic_wave(
    slug: str,
    *,
    host: str,
    target_map: str,
    cohort: str,
    path_prefix: str,
) -> WaveSpec:
    wave_root = t35.TOOLS / "waves" / slug
    return WaveSpec(
        wave_id=slug,
        archetype="lock_relation_set",
        template_kind="exact_relation_set",
        host=host,
        target_map=target_map,
        source_path=wave_root / "source.json",
        generated_root=RECIPE_GENERATED_ROOT,
        equivalence_path=wave_root / "equivalence.json",
        selection_policy="lock_templates",
        publication_policy="lock",
        path_layout="cohort_nested",
        compile_authority="recipe_bulk",
        relation_sort="source_recipe_index_then_stable_id",
        stable_id_policy="lock",
        target_map_policy="spec",
        source_kind_policy="relation_provenance",
        lock_path=wave_root / "production_lock.json",
        operand_map_path=wave_root / "operand_runtime_map.json",
        wave_slug=slug,
        cohort=cohort,
        representation="exact_or_exact_multi",
        depends_on_slugs=("runtime-load/allocation-split",),
        path_prefix=path_prefix,
        dry_run_without_lock=True,
    )


SEMANTIC_WAVES: dict[str, WaveSpec] = {
    "smelter/ordinary-closure": _semantic_wave(
        "smelter/ordinary-closure",
        host="cruciblecraft:smelter",
        target_map="cruciblecraft:smelter",
        cohort="ordinary_closure",
        path_prefix="smelter/ordinary_closure",
    ),
    "mixer/ordinary-closure": _semantic_wave(
        "mixer/ordinary-closure",
        host="cruciblecraft:mixer",
        target_map="cruciblecraft:mixer",
        cohort="ordinary_closure",
        path_prefix="mixer/ordinary_closure",
    ),
    "drying/ordinary-closure": _semantic_wave(
        "drying/ordinary-closure",
        host="cruciblecraft:drying",
        target_map="cruciblecraft:drying",
        cohort="ordinary_closure",
        path_prefix="drying/ordinary_closure",
    ),
    "electrolyzer/ordinary-closure": _semantic_wave(
        "electrolyzer/ordinary-closure",
        host="cruciblecraft:electrolyzer",
        target_map="cruciblecraft:electrolyzer",
        cohort="ordinary_closure",
        path_prefix="electrolyzer/ordinary_closure",
    ),
    "centrifuge/ordinary-closure": _semantic_wave(
        "centrifuge/ordinary-closure",
        host="cruciblecraft:centrifuge",
        target_map="cruciblecraft:centrifuge",
        cohort="ordinary_closure",
        path_prefix="centrifuge/ordinary_closure",
    ),
    "autoclave/ordinary-closure": _semantic_wave(
        "autoclave/ordinary-closure",
        host="cruciblecraft:autoclave",
        target_map="cruciblecraft:autoclave",
        cohort="ordinary_closure",
        path_prefix="autoclave/ordinary_closure",
    ),
    "compressor/ordinary-closure": _semantic_wave(
        "compressor/ordinary-closure",
        host="cruciblecraft:compressor",
        target_map="cruciblecraft:compressor",
        cohort="ordinary_closure",
        path_prefix="compressor/ordinary_closure",
    ),
    "smelter/deferred-recycling": WaveSpec(
        wave_id="smelter/deferred-recycling",
        archetype="lock_relation_set",
        template_kind="exact_relation_set",
        host="cruciblecraft:smelter",
        target_map="cruciblecraft:smelter",
        source_path=t35.TOOLS / "waves" / "smelter" / "deferred-recycling" / "source.json",
        generated_root=RECIPE_GENERATED_ROOT,
        equivalence_path=(
            t35.TOOLS / "waves" / "smelter" / "deferred-recycling" / "equivalence.json"
        ),
        selection_policy="lock_templates",
        publication_policy="lock",
        path_layout="cohort_nested",
        compile_authority="recipe_bulk",
        relation_sort="source_recipe_index_then_stable_id",
        stable_id_policy="lock",
        target_map_policy="spec",
        source_kind_policy="relation_provenance",
        lock_path=(
            t35.TOOLS / "waves" / "smelter" / "deferred-recycling" / "production_lock.json"
        ),
        operand_map_path=(
            t35.TOOLS
            / "waves"
            / "smelter"
            / "deferred-recycling"
            / "operand_runtime_map.json"
        ),
        wave_slug="smelter/deferred-recycling",
        cohort="deferred_recycling",
        representation="exact",
        depends_on_slugs=("recycling/smelter-mte-identity",),
        path_prefix="smelter/deferred_recycling",
        dry_run_without_lock=True,
        expected_family_count=1817,
        expected_relation_count=1817,
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
    if wave_id in SEMANTIC_WAVES:
        parse_wave_token(wave_id, schema="semantic-v3")
        return SEMANTIC_WAVES[wave_id]
    if wave_id in WAVES:
        return WAVES[wave_id]
    try:
        parsed = parse_wave_token(wave_id)
    except WaveSlugError as error:
        raise KeyError(f"unknown recipe wave: {wave_id}") from error
    if parsed.wave_slug and parsed.wave_slug in SEMANTIC_WAVES:
        return SEMANTIC_WAVES[parsed.wave_slug]
    spec = WAVES.get(parsed.compile_key())
    if spec is not None:
        return spec
    from tools.recipe_bulk.spec_registry import SpecRegistryError, try_derive_wave_spec

    try:
        derived = try_derive_wave_spec(wave_id)
    except SpecRegistryError as error:
        raise ValueError(str(error)) from error
    if derived is not None:
        return derived
    raise KeyError(f"unknown recipe wave: {wave_id}")


def shadow_waves() -> tuple[WaveSpec, ...]:
    return tuple(recipe_wave(wave_id) for wave_id in SHADOW_ORDER)


def compile_waves() -> tuple[WaveSpec, ...]:
    return tuple(recipe_wave(wave_id) for wave_id in COMPILE_ORDER)
