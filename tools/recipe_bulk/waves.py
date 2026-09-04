#!/usr/bin/python3
"""Data-driven WaveSpec table for historical and semantic recipe waves."""
from __future__ import annotations

from tools import io_common as files
from tools.recipe_bulk.models import WaveSpec
from tools.recipe_bulk.slugs import WaveSlugError, parse_wave_token

SHADOW_ORDER = (
    "block/object",
    "smelter/stone",
    "assembler/wood",
    "electrolyzer/compact",
    "centrifuge/compact",
    "roaster/compact",
    "assembler/compact",
)
COMPILE_ORDER = (
    "assembler/compact",
    "roaster/compact",
    "centrifuge/compact",
    "electrolyzer/compact",
    "assembler/wood",
    "smelter/stone",
    "block/object",
)
FORWARD_COMPILE_ORDER = COMPILE_ORDER + (
    "bath/mte",
    "bath/remainder",
    "bath/identity",
    "bath/tiny-purified",
)
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
    files.ROOT / "src/recipe_generated/resources/data/cruciblecraft/recipe"
)
RECIPE_SUPPORT_ROOT = (
    files.ROOT / "src/recipe_support_generated/resources/data/cruciblecraft/recipe"
)

ASSEMBLER_COMPACT_OPERAND_MAP = files.TOOLS / "assembler_compact_operand_runtime_map.json"
BATH_REMAINDER_LOCK_PATH = files.TOOLS / "bath_remainder_production_lock.json"
BATH_REMAINDER_CANDIDATE_FAMILY_COUNT = 395
BATH_REMAINDER_CANDIDATE_RELATION_COUNT = 13708
BATH_IDENTITY_LOCK_PATH = files.TOOLS / "bath_identity_production_lock.json"
BATH_IDENTITY_CANDIDATE_FAMILY_COUNT = 145
BATH_IDENTITY_CANDIDATE_RELATION_COUNT = 34091
BATH_TINY_PURIFIED_LOCK_PATH = (
    files.TOOLS / "waves" / "bath" / "tiny-purified" / "production_lock.json"
)
BATH_TINY_PURIFIED_CANDIDATE_FAMILY_COUNT = 5
BATH_TINY_PURIFIED_CANDIDATE_RELATION_COUNT = 95
BATH_WAVES = frozenset(
    ("bath/mte", "bath/remainder", "bath/identity", "bath/tiny-purified")
)
BATH_WAVE_ORDER = (
    "bath/mte",
    "bath/remainder",
    "bath/identity",
    "bath/tiny-purified",
)


def _bath_remainder_expected_counts() -> tuple[int, int]:
    if BATH_REMAINDER_LOCK_PATH.is_file():
        production = files.load_json(BATH_REMAINDER_LOCK_PATH).get("production") or {}
        return int(production["family_count"]), int(production["relation_count"])
    return BATH_REMAINDER_CANDIDATE_FAMILY_COUNT, BATH_REMAINDER_CANDIDATE_RELATION_COUNT


_BATH_REMAINDER_FAMILY_COUNT, _BATH_REMAINDER_RELATION_COUNT = (
    _bath_remainder_expected_counts()
)


def _bath_identity_expected_counts() -> tuple[int, int]:
    if BATH_IDENTITY_LOCK_PATH.is_file():
        production = files.load_json(BATH_IDENTITY_LOCK_PATH).get("production") or {}
        return int(production["family_count"]), int(production["relation_count"])
    return BATH_IDENTITY_CANDIDATE_FAMILY_COUNT, BATH_IDENTITY_CANDIDATE_RELATION_COUNT


_BATH_IDENTITY_FAMILY_COUNT, _BATH_IDENTITY_RELATION_COUNT = (
    _bath_identity_expected_counts()
)


def _bath_tiny_purified_expected_counts() -> tuple[int, int]:
    if BATH_TINY_PURIFIED_LOCK_PATH.is_file():
        production = files.load_json(BATH_TINY_PURIFIED_LOCK_PATH).get("production") or {}
        return int(production["family_count"]), int(production["relation_count"])
    return (
        BATH_TINY_PURIFIED_CANDIDATE_FAMILY_COUNT,
        BATH_TINY_PURIFIED_CANDIDATE_RELATION_COUNT,
    )


_BATH_TINY_FAMILY_COUNT, _BATH_TINY_RELATION_COUNT = (
    _bath_tiny_purified_expected_counts()
)

def _build_historical_waves() -> dict[str, WaveSpec]:
    from tools import assembler_compact_common as assembler
    from tools import roaster_common as roaster
    from tools import centrifuge_common as centrifuge
    from tools import electrolyzer_common as electrolyzer
    from tools import assembler_wood_common as assembler_wood
    from tools import smelter_stone_common as smelter_stone
    from tools import block_object_common as block_object

    return {
    "assembler/compact": WaveSpec(
        wave_id="assembler/compact",
        archetype="no_lock_singleton",
        template_kind="exact_singleton",
        host=assembler.HOST,
        target_map="cruciblecraft:assembler",
        source_path=assembler.ASSEMBLER_SOURCE,
        generated_root=assembler.GENERATED_ROOT,
        equivalence_path=assembler.EQUIVALENCE,
        selection_policy="all",
        publication_policy="constant",
        default_publication_group="cruciblecraft:assembler/compact",
        path_layout="flat",
        compile_authority="recipe_bulk",
        relation_sort="template_key",
        stable_id_policy="hex_suffix",
        target_map_policy="relation",
        source_kind_policy="operand_design_first",
        operand_map_path=ASSEMBLER_COMPACT_OPERAND_MAP,
        stable_id_prefix="assembler/compact",
        expected_family_count=assembler.FAMILY_COUNT,
        expected_relation_count=assembler.FAMILY_COUNT,
    ),
    "roaster/compact": WaveSpec(
        wave_id="roaster/compact",
        archetype="no_lock_relation_set",
        template_kind="exact_relation_set",
        host=roaster.HOST,
        target_map=roaster.TARGET_MAP,
        source_path=roaster.SOURCE,
        generated_root=roaster.GENERATED_ROOT,
        equivalence_path=roaster.EQUIVALENCE,
        selection_policy="all",
        publication_policy="constant",
        default_publication_group="cruciblecraft:roaster/compact",
        path_layout="flat",
        compile_authority="recipe_bulk",
        relation_sort="source_recipe_index_then_stable_id",
        stable_id_policy="source",
        target_map_policy="spec",
        source_kind_policy="operand_derived_or_backed",
        operand_map_path=roaster.OPERAND_RUNTIME_MAP,
        expected_family_count=roaster.FAMILY_COUNT,
        expected_relation_count=roaster.SOURCE_ROWS,
    ),
    "centrifuge/compact": WaveSpec(
        wave_id="centrifuge/compact",
        archetype="lock_relation_set",
        template_kind="exact_relation_set",
        host=centrifuge.HOST,
        target_map=centrifuge.TARGET_MAP,
        source_path=centrifuge.SOURCE,
        generated_root=centrifuge.GENERATED_ROOT,
        equivalence_path=centrifuge.EQUIVALENCE,
        selection_policy="lock_templates",
        publication_policy="expanded_count",
        path_layout="flat",
        compile_authority="recipe_bulk",
        relation_sort="source_recipe_index_then_stable_id",
        stable_id_policy="source",
        target_map_policy="spec",
        source_kind_policy="operand_derived_or_backed",
        lock_path=centrifuge.PRODUCTION_LOCK,
        operand_map_path=centrifuge.OPERAND_RUNTIME_MAP,
        singleton_publication_group=centrifuge.SINGLETON_GROUP,
        multi_publication_group=centrifuge.MULTI_GROUP,
        expected_family_count=centrifuge.production_family_count(),
        expected_relation_count=centrifuge.production_relation_count(),
    ),
    "electrolyzer/compact": WaveSpec(
        wave_id="electrolyzer/compact",
        archetype="lock_relation_set",
        template_kind="exact_relation_set",
        host=electrolyzer.HOST,
        target_map=electrolyzer.TARGET_MAP,
        source_path=electrolyzer.SOURCE,
        generated_root=electrolyzer.GENERATED_ROOT,
        equivalence_path=electrolyzer.EQUIVALENCE,
        selection_policy="lock_templates",
        publication_policy="expanded_count",
        path_layout="flat",
        compile_authority="recipe_bulk",
        relation_sort="source_recipe_index_then_stable_id",
        stable_id_policy="source",
        target_map_policy="spec",
        source_kind_policy="operand_design_first",
        lock_path=electrolyzer.PRODUCTION_LOCK,
        operand_map_path=electrolyzer.OPERAND_RUNTIME_MAP,
        singleton_publication_group=electrolyzer.SINGLETON_GROUP,
        multi_publication_group=electrolyzer.MULTI_GROUP,
        combinatorial_template_keys=electrolyzer.COMBINATORIAL_TEMPLATE_KEYS,
        expected_family_count=electrolyzer.production_family_count(),
        expected_relation_count=electrolyzer.production_relation_count(),
    ),
    "assembler/wood": WaveSpec(
        wave_id="assembler/wood",
        archetype="lock_singleton",
        template_kind="exact_singleton",
        host=assembler_wood.HOST,
        target_map=assembler_wood.TARGET_MAP,
        source_path=assembler_wood.SOURCE,
        generated_root=assembler_wood.GENERATED_ROOT,
        equivalence_path=assembler_wood.EQUIVALENCE,
        selection_policy="exclude_combinatorial",
        publication_policy="relation",
        path_layout="flat",
        compile_authority="recipe_bulk",
        relation_sort="template_key",
        stable_id_policy="hex_suffix",
        target_map_policy="spec",
        source_kind_policy="operand_design_first",
        lock_path=assembler_wood.PRODUCTION_LOCK,
        operand_map_path=assembler_wood.OPERAND_RUNTIME_MAP,
        stable_id_prefix="assembler/wood",
        combinatorial_template_keys=assembler_wood.COMBINATORIAL_TEMPLATE_KEYS,
        expected_family_count=assembler_wood.PRODUCTION_FAMILY_COUNT,
        expected_relation_count=assembler_wood.PRODUCTION_RELATION_COUNT,
    ),
    "smelter/stone": WaveSpec(
        wave_id="smelter/stone",
        archetype="lock_singleton",
        template_kind="exact_singleton",
        host=smelter_stone.HOST,
        target_map=smelter_stone.TARGET_MAP,
        source_path=smelter_stone.SOURCE,
        generated_root=smelter_stone.GENERATED_ROOT,
        equivalence_path=smelter_stone.EQUIVALENCE,
        selection_policy="lock_templates",
        publication_policy="constant",
        path_layout="flat",
        compile_authority="recipe_bulk",
        relation_sort="template_key",
        stable_id_policy="lock",
        target_map_policy="spec",
        source_kind_policy="relation_provenance",
        lock_path=smelter_stone.PRODUCTION_LOCK,
        operand_map_path=smelter_stone.OPERAND_RUNTIME_MAP,
        default_publication_group=smelter_stone.STONE_GROUP,
        expected_family_count=smelter_stone.PRODUCTION_FAMILY_COUNT,
        expected_relation_count=smelter_stone.PRODUCTION_RELATION_COUNT,
    ),
    "block/object": WaveSpec(
        wave_id="block/object",
        archetype="lock_singleton",
        template_kind="exact_singleton",
        host=block_object.HOST,
        target_map="cruciblecraft:smelter+drying",
        source_path=block_object.SOURCE,
        generated_root=block_object.GENERATED_ROOT,
        equivalence_path=block_object.EQUIVALENCE,
        selection_policy="lock_templates",
        publication_policy="lock",
        path_layout="host_nested",
        compile_authority="recipe_bulk",
        relation_sort="template_key",
        stable_id_policy="lock",
        target_map_policy="lock_host",
        source_kind_policy="relation_provenance",
        lock_path=block_object.PRODUCTION_LOCK,
        operand_map_path=block_object.OPERAND_RUNTIME_MAP,
        expected_family_count=379,
        expected_relation_count=379,
        tree_prefixes=("smelter/block", "drying/block"),
    ),
    "bath/mte": WaveSpec(
        wave_id="bath/mte",
        archetype="lock_relation_set",
        template_kind="exact_relation_set",
        host="cruciblecraft:bath",
        target_map="cruciblecraft:bath",
        source_path=files.TOOLS / "bath_mte_source.json",
        generated_root=(
            files.ROOT / "src/recipe_generated/resources/data/cruciblecraft/recipe/bath/mte"
        ),
        equivalence_path=files.TOOLS / "bath_mte_equivalence.json",
        selection_policy="lock_templates",
        publication_policy="lock",
        path_layout="flat",
        compile_authority="recipe_bulk",
        relation_sort="source_recipe_index_then_stable_id",
        stable_id_policy="lock",
        target_map_policy="spec",
        source_kind_policy="relation_provenance",
        lock_path=files.TOOLS / "bath_mte_production_lock.json",
        operand_map_path=files.TOOLS / "bath_mte_operand_runtime_map.json",
        default_publication_group="cruciblecraft:bath/mte",
        expected_family_count=803,
        expected_relation_count=1517,
    ),
    "bath/remainder": WaveSpec(
        wave_id="bath/remainder",
        archetype="lock_relation_set",
        template_kind="exact_relation_set",
        host="cruciblecraft:bath",
        target_map="cruciblecraft:bath",
        source_path=files.TOOLS / "bath_remainder_source.json",
        generated_root=(
            files.ROOT
            / "src/recipe_generated/resources/data/cruciblecraft/recipe/bath/remainder"
        ),
        equivalence_path=files.TOOLS / "bath_remainder_equivalence.json",
        selection_policy="lock_templates",
        publication_policy="lock",
        path_layout="flat",
        compile_authority="recipe_bulk",
        relation_sort="source_recipe_index_then_stable_id",
        stable_id_policy="lock",
        target_map_policy="spec",
        source_kind_policy="relation_provenance",
        lock_path=BATH_REMAINDER_LOCK_PATH,
        operand_map_path=files.TOOLS / "bath_remainder_operand_runtime_map.json",
        default_publication_group="cruciblecraft:bath/remainder/exact",
        expected_family_count=_BATH_REMAINDER_FAMILY_COUNT,
        expected_relation_count=_BATH_REMAINDER_RELATION_COUNT,
    ),
    "bath/identity": WaveSpec(
        wave_id="bath/identity",
        archetype="lock_relation_set",
        template_kind="exact_relation_set",
        host="cruciblecraft:bath",
        target_map="cruciblecraft:bath",
        source_path=files.TOOLS / "bath_identity_source.json",
        generated_root=(
            files.ROOT
            / "src/recipe_generated/resources/data/cruciblecraft/recipe/bath/identity"
        ),
        equivalence_path=files.TOOLS / "bath_identity_equivalence.json",
        selection_policy="lock_templates",
        publication_policy="lock",
        path_layout="flat",
        compile_authority="recipe_bulk",
        relation_sort="source_recipe_index_then_stable_id",
        stable_id_policy="lock",
        target_map_policy="spec",
        source_kind_policy="relation_provenance",
        lock_path=BATH_IDENTITY_LOCK_PATH,
        operand_map_path=files.TOOLS / "bath_identity_operand_runtime_map.json",
        default_publication_group="cruciblecraft:bath/identity/exact",
        expected_family_count=_BATH_IDENTITY_FAMILY_COUNT,
        expected_relation_count=_BATH_IDENTITY_RELATION_COUNT,
    ),
    "bath/tiny-purified": WaveSpec(
        wave_id="bath/tiny-purified",
        archetype="lock_relation_set",
        template_kind="exact_relation_set",
        host="cruciblecraft:bath",
        target_map="cruciblecraft:bath",
        source_path=(
            files.TOOLS / "waves" / "bath" / "tiny-purified" / "bath_source.json"
        ),
        generated_root=(
            files.ROOT
            / "src/recipe_generated/resources/data/cruciblecraft/recipe/bath/tiny_purified"
        ),
        equivalence_path=(
            files.TOOLS / "waves" / "bath" / "tiny-purified" / "equivalence.json"
        ),
        selection_policy="lock_templates",
        publication_policy="lock",
        path_layout="flat",
        compile_authority="recipe_bulk",
        relation_sort="source_recipe_index_then_stable_id",
        stable_id_policy="lock",
        target_map_policy="spec",
        source_kind_policy="relation_provenance",
        lock_path=BATH_TINY_PURIFIED_LOCK_PATH,
        operand_map_path=(
            files.TOOLS / "waves" / "bath" / "tiny-purified" / "operand_runtime_map.json"
        ),
        default_publication_group="cruciblecraft:bath/tiny-purified/exact_multi",
        expected_family_count=_BATH_TINY_FAMILY_COUNT,
        expected_relation_count=_BATH_TINY_RELATION_COUNT,
    ),
    }


_HISTORICAL_WAVES: dict[str, WaveSpec] | None = None
_IDENTITY_ONLY_WAVES: dict[str, WaveSpec] | None = None


def historical_waves() -> dict[str, WaveSpec]:
    global _HISTORICAL_WAVES
    if _HISTORICAL_WAVES is None:
        _HISTORICAL_WAVES = _build_historical_waves()
    return _HISTORICAL_WAVES


def identity_only_waves() -> dict[str, WaveSpec]:
    global _IDENTITY_ONLY_WAVES
    if _IDENTITY_ONLY_WAVES is None:
        from tools import storage_common as storage

        _IDENTITY_ONLY_WAVES = {
            "storage/lock": WaveSpec(
                wave_id="storage/lock",
                archetype="lock_singleton",
                template_kind="exact_singleton",
                host="cruciblecraft:storage",
                target_map="cruciblecraft:storage",
                source_path=storage.PRODUCTION_LOCK,
                generated_root=files.ROOT / "src/main/resources/data/cruciblecraft",
                equivalence_path=storage.EQUIVALENCE,
                selection_policy="all",
                publication_policy="omit",
                path_layout="flat",
                compile_authority="recipe_bulk",
                relation_sort="template_key",
                stable_id_policy="lock",
                target_map_policy="spec",
                source_kind_policy="relation_provenance",
                lock_path=storage.PRODUCTION_LOCK,
                identity_only=True,
            ),
        }
    return _IDENTITY_ONLY_WAVES


def __getattr__(name: str):
    if name == "WAVES":
        return historical_waves()
    if name == "IDENTITY_ONLY_WAVES":
        return identity_only_waves()
    raise AttributeError(f"module {__name__!r} has no attribute {name!r}")


def _semantic_wave(
    slug: str,
    *,
    host: str,
    target_map: str,
    cohort: str,
    path_prefix: str,
) -> WaveSpec:
    wave_root = files.TOOLS / "waves" / slug
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
        source_path=files.TOOLS / "waves" / "smelter" / "deferred-recycling" / "source.json",
        generated_root=RECIPE_GENERATED_ROOT,
        equivalence_path=(
            files.TOOLS / "waves" / "smelter" / "deferred-recycling" / "equivalence.json"
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
            files.TOOLS / "waves" / "smelter" / "deferred-recycling" / "production_lock.json"
        ),
        operand_map_path=(
            files.TOOLS
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


def recipe_wave(wave_id: str) -> WaveSpec:
    if wave_id in SEMANTIC_WAVES:
        parse_wave_token(wave_id, schema="semantic-v3")
        return SEMANTIC_WAVES[wave_id]
    historical = historical_waves()
    if wave_id in historical:
        return historical[wave_id]
    try:
        parsed = parse_wave_token(wave_id)
    except WaveSlugError as error:
        raise KeyError(f"unknown recipe wave: {wave_id}") from error
    if parsed.wave_slug and parsed.wave_slug in SEMANTIC_WAVES:
        return SEMANTIC_WAVES[parsed.wave_slug]
    spec = historical.get(parsed.compile_key())
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
