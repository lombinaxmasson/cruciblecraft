#!/usr/bin/env python3
"""portfolio/crops-food-bees-r0: freeze three crops / food / bees categories."""
from __future__ import annotations

import argparse
import json
import sys
from collections import Counter
from typing import Any

from tools import closeout_seal
from tools import portfolio_one_x as one_x
from tools import census_common as census
from tools.recipe_bulk.slugs import KNOWN_SEMANTIC_SLUGS
from tools.wave_closeout import known_slugs
from tools.wave_closeout import spec_for
from tools.wave_closeout import wave_dir

SLUG = "portfolio/crops-food-bees-r0"
PREDECESSOR = "portfolio/vanilla-replace-r0"
PREDECESSOR_STATUS = "VANILLA_REPLACE_R0_READY"
NON_ORE_SLUG = "portfolio/non-ore-worldgen-r0"
NON_ORE_STATUS = "NON_ORE_WORLDGEN_R0_READY"
GENERIC_IMPORT_SLUG = "portfolio/generic-recipe-generator"
GENERIC_IMPORT_STATUS = "GENERIC_RECIPE_IMPORT_READY"
STATUS = "CROPS_FOOD_BEES_R0_READY"
SOURCE_REVISION = census.SOURCE_REVISION
GENERATED_BY = "python tools/build_crops_food_bees_r0.py"
PINNED_NAMES = ("Crops", "Food", "Bees")
SEED_ANCHOR = "crops / food / bees"
CAPABILITY_ANCHORS = {
    "Crops": "GT6 crops / plants (dump worldgen plant.* + gt.recipe.squeezer)",
    "Food": "GT6 food maps (gt.recipe.juicer / gt.recipe.fermenter)",
    "Bees": "gt.recipe.bumblequeen / gt.recipe.bumblelyzer + WorldgenHives",
}
PINNED_PLANTS = ("plant.glowtus", "plant.bush")
PINNED_HIVES = (
    "overworld.bumblehives",
    "nether.bumblehives",
    "aether.bumblehives",
    "end.bumblehives",
    "twilight.bumblehives",
    "erebus.bumblehives",
    "betweenlands.bumblehives",
    "atum.bumblehives",
    "alfheim.bumblehives",
    "tropics.bumblehives",
)
PINNED_WORLDGEN = {
    "Crops": PINNED_PLANTS,
    "Food": (),
    "Bees": PINNED_HIVES,
}
PINNED_DUMP_MAPS = {
    "Crops": ("gt.recipe.squeezer",),
    "Food": ("gt.recipe.juicer", "gt.recipe.fermenter"),
    "Bees": ("gt.recipe.bumblequeen", "gt.recipe.bumblelyzer"),
}
EXPECTED_DUMP_COUNTS = {
    "gt.recipe.squeezer": 5322,
    "gt.recipe.juicer": 96,
    "gt.recipe.fermenter": 6435,
    "gt.recipe.bumblequeen": 80,
    "gt.recipe.bumblelyzer": 1440,
}
PLANTALYZER_MAP = "gt.recipe.plantalyzer"
EXPECTED_DUMP_CONTEXT = 13373
EXPECTED_SLICE = 12
FULL_OTHER_FEATURES = 190
NON_ORE_SLICE = 18
REMAINDER_AFTER_NON_ORE = 172
REMAINDER_AFTER_THIS_SLICE = 160
EXPECTED_T35_FAMILIES = {
    "Crops": ("multiblock/multi_tile_entity_squeezer",),
    "Food": (
        "misc_tool_blocks/multi_tile_entity_juicer",
        "multiblock/multi_tile_entity_fermenter",
    ),
    "Bees": ("untyped/multi_tile_entity_bumble_hive",),
}
EXPECTED_T35_BEHAVIOR = {
    "misc_tool_blocks/multi_tile_entity_juicer": "MultiTileEntityJuicer",
    "multiblock/multi_tile_entity_fermenter": "MultiTileEntityFermenter",
    "multiblock/multi_tile_entity_squeezer": "MultiTileEntitySqueezer",
    "untyped/multi_tile_entity_bumble_hive": "MultiTileEntityBumbleHive",
}
FEASIBILITY_VALUES = (
    "bounded_extension",
    "requires_new_runtime",
    "defer_to_portfolio",
    "blocked",
)
FORBIDDEN_SUCCESSORS = (
    "crops-implementation",
    "food-implementation",
    "bees-implementation",
    "trees-implementation",
    "dungeons-implementation",
    "planets-implementation",
    "center-implementation",
    "vanilla-implementation",
    "replace-implementation",
    "remainder-other-features",
    "worldgen-ores-vanilla",
    "logistics-cover-net-core",
    "t13c-exclusion-reclaim-core",
    "count-ceiling-kind-envelope",
    "combinatorial",
    "nuclear",
)
ALLOWED_TOPOLOGY_KEYS = (
    "append_only",
    "complete_family_count",
    "generated_by",
    "next_unassigned",
    "remaining_recipe_gap",
    "schema_version",
    "source_revision",
    "status",
    "unique_active_wave",
    "wave_slug",
)
CATEGORY_TOKENS = {
    "Crops": (
        "gt.recipe.squeezer",
        "worldgenglowtus",
        "worldgenbushes",
        "plant.glowtus",
        "plant.bush",
        "multi_tile_entity_squeezer",
    ),
    "Food": (
        "gt.recipe.juicer",
        "gt.recipe.fermenter",
        "multi_tile_entity_juicer",
        "multi_tile_entity_fermenter",
    ),
    "Bees": (
        "gt.recipe.bumblequeen",
        "gt.recipe.bumblelyzer",
        "worldgenhives",
        "bumblehives",
        "multi_tile_entity_bumble_hive",
    ),
}
HOST_TOKENS = (
    "squeezer",
    "juicer",
    "fermenter",
    "bumble",
    "plantalyzer",
)
CROPS_QUESTIONS = {
    "crop_runtime": (
        "plant.glowtus / plant.bush are crop growth, not another scatter JSON "
        "row. R0 must not write them into the current T20 catalog."
    ),
    "delete_later": (
        "If a later realization card marks one category out_of_scope, it must "
        "keep this R0 denominator hash. R0 does not delete a named category."
    ),
    "disposition_policy": (
        "Per-category later cards must choose implemented, planned, deferred, "
        "or out_of_scope. R0 does not fill those dispositions."
    ),
    "fail_closed": (
        "Unknown plant, hive, or food map must not silent no-op."
    ),
    "hive_worldgen": (
        "Ten WorldgenHives span overworld / nether / aether / end / twilight / "
        "erebus / betweenlands / atum / alfheim / tropics. T20 is overworld-only. "
        "R0 must not write hives into the current catalog."
    ),
    "new_recipe_maps": (
        "Whether squeezer / juicer / fermenter / bumblequeen / bumblelyzer "
        "need new RecipeMaps or processing kinds. Generic importer does not "
        "create those maps. R0 must not register them."
    ),
    "player_acquisition": (
        "How survival encounters crops, food processing, or bees."
    ),
    "scale": (
        "13,373 dump recipeCount rows are scale context, not a census. A later "
        "implementation card measures how that context is cut into a production "
        "lock separately from this R0 denominator."
    ),
}
DUMP_ROOT = census.ROOT / "gt6_dump" / "gt6_recipe_dump"
DUMP_INDEX = DUMP_ROOT / "index.json"
WORLDGEN_OTHER = DUMP_ROOT / "worldgen" / "other_features.json"
CAPABILITY_MAP = (
    census.TOOLS / "waves" / "portfolio" / "source-capability-map" / "capability_map.json"
)
GROWTH_ORDER = (
    census.TOOLS
    / "waves"
    / "portfolio"
    / "source-capability-growth-order"
    / "growth_order.json"
)
T35_EXCLUSION = census.TOOLS / "census_excluded_object_reclaim.json"
T20_POLICY = census.TOOLS / "worldgen_worldgen_source_policy.json"
COUNT_CEILING_REFS = (
    census.TOOLS / "waves" / "portfolio" / "source-capability-map-r0" / "count_ceiling_refs.json"
)
LEFTOVER_LATER = (
    census.TOOLS / "waves" / "portfolio" / "source-capability-map-r0" / "leftover_later.json"
)
MACHINE_TIERS = (
    census.ROOT / "src/main/resources/data/cruciblecraft/machine_tiers.json"
)
PROCESSING_MACHINES = (
    census.ROOT
    / "src/main/java/com/masson/cruciblecraft/registry/ModProcessingMachines.java"
)
RECIPE_MAPS_JAVA = (
    census.ROOT / "src/main/java/com/masson/cruciblecraft/registry/ModRecipeMaps.java"
)
SOURCE_IMPORT = census.TOOLS / "recipe_bulk" / "source_import.py"
ORE_VEINS = (
    census.ROOT / "src/main/resources/data/cruciblecraft/worldgen_catalog/ore_veins.json"
)
FLUID_DEPOSITS = (
    census.ROOT
    / "src/main/resources/data/cruciblecraft/worldgen_catalog/fluid_deposits.json"
)
SURFACE_SCATTER = (
    census.ROOT
    / "src/main/resources/data/cruciblecraft/worldgen_catalog/surface_scatter.json"
)
WORLDGEN_CATALOG = (
    census.ROOT / "src/main/resources/data/cruciblecraft/worldgen_catalog"
)
CONFIGURED_FEATURE = (
    census.ROOT
    / "src/main/resources/data/cruciblecraft/worldgen/configured_feature"
)
PLACED_FEATURE = (
    census.ROOT / "src/main/resources/data/cruciblecraft/worldgen/placed_feature"
)
WORLDGEN_JAVA = census.ROOT / "src/main/java/com/masson/cruciblecraft/worldgen"
RECIPE_GENERATED = census.ROOT / "src" / "recipe_generated"


def generated_by(_slug: str = SLUG) -> str:
    return GENERATED_BY


def nuclear_started() -> bool:
    return one_x.nuclear_started()


def leftover_later_count() -> int:
    leftover = census.load_json(LEFTOVER_LATER)
    total = int(leftover["counts"]["total"])
    if total != 39:
        raise ValueError(f"leftover_later_count {total} != 39")
    return total


def require_registered(slug: str) -> list[str]:
    errors: list[str] = []
    if slug not in KNOWN_SEMANTIC_SLUGS:
        errors.append(f"{slug} missing from KNOWN_SEMANTIC_SLUGS")
    if slug not in known_slugs():
        errors.append(f"{slug} missing from wave_closeout")
    return errors


def require_predecessor() -> list[str]:
    errors = closeout_seal.check_wave_seal(PREDECESSOR)
    readiness = census.load_json(wave_dir(PREDECESSOR) / "readiness.json")
    if readiness.get("status") != PREDECESSOR_STATUS:
        errors.append(
            f"{PREDECESSOR} status {readiness.get('status')} != {PREDECESSOR_STATUS}"
        )
    non_ore_errors = closeout_seal.check_wave_seal(NON_ORE_SLUG)
    errors.extend(non_ore_errors)
    non_ore = census.load_json(wave_dir(NON_ORE_SLUG) / "readiness.json")
    if non_ore.get("status") != NON_ORE_STATUS:
        errors.append(
            f"{NON_ORE_SLUG} status {non_ore.get('status')} != {NON_ORE_STATUS}"
        )
    return errors


def authority_hashes() -> dict[str, str]:
    return {
        "capability_map": census.sha256_file(CAPABILITY_MAP),
        "dump_index": census.sha256_file(DUMP_INDEX),
        "growth_order": census.sha256_file(GROWTH_ORDER),
        "non_ore_worldgen_r0_seal": census.sha256_file(
            wave_dir(NON_ORE_SLUG) / "closeout_seal.json"
        ),
        "other_features": census.sha256_file(WORLDGEN_OTHER),
        "worldgen_worldgen_source_policy": census.sha256_file(T20_POLICY),
        "census_excluded_object_reclaim": census.sha256_file(T35_EXCLUSION),
        "vanilla_replace_r0_seal": census.sha256_file(
            wave_dir(PREDECESSOR) / "closeout_seal.json"
        ),
    }


def load_other_features() -> list[dict[str, Any]]:
    if not WORLDGEN_OTHER.is_file():
        raise ValueError("dump worldgen/other_features.json missing")
    document = census.load_json(WORLDGEN_OTHER)
    if not isinstance(document, list):
        raise ValueError("other_features.json is not a list")
    return [dict(row) for row in document]


def feature_name(row: dict[str, Any]) -> str:
    return str(row.get("name") or "")


def feature_type(row: dict[str, Any]) -> str:
    return str(row.get("type") or "")


def category_of(row: dict[str, Any]) -> str | None:
    name = feature_name(row)
    kind = feature_type(row)
    if name in PINNED_PLANTS or kind in {"WorldgenGlowtus", "WorldgenBushes"}:
        return "Crops"
    if name in PINNED_HIVES or kind == "WorldgenHives":
        return "Bees"
    return None


def non_ore_owned_names() -> set[str]:
    inherited = census.load_json(wave_dir(NON_ORE_SLUG) / "inherited_denominator.json")
    names = {str(row.get("name") or "") for row in inherited.get("features") or []}
    if len(names) != NON_ORE_SLICE:
        raise ValueError(f"non-ore owned features {len(names)} != 18")
    return names


def dump_maps() -> dict[str, dict[str, Any]]:
    document = census.load_json(DUMP_INDEX)
    rows = {
        str(row.get("nameInternal") or ""): dict(row)
        for row in document.get("maps") or []
        if row.get("nameInternal")
    }
    wanted = set(EXPECTED_DUMP_COUNTS) | {PLANTALYZER_MAP}
    missing = sorted(wanted - set(rows))
    if missing:
        raise ValueError(f"dump index missing maps: {missing}")
    for name, expected in EXPECTED_DUMP_COUNTS.items():
        count = rows[name].get("recipeCount")
        if count is None or int(count) != expected:
            raise ValueError(f"{name} recipeCount {count} != {expected}")
    plantalyzer = rows[PLANTALYZER_MAP].get("recipeCount")
    if plantalyzer is None or int(plantalyzer) != 0:
        raise ValueError(f"{PLANTALYZER_MAP} recipeCount {plantalyzer} != 0")
    return rows


def dump_map_record(row: dict[str, Any], *, is_census: bool) -> dict[str, Any]:
    return {
        "file": str(row.get("file") or ""),
        "is_census": is_census,
        "nameInternal": str(row.get("nameInternal") or ""),
        "nameLocal": str(row.get("nameLocal") or ""),
        "recipeCount": int(0 if row.get("recipeCount") is None else row["recipeCount"]),
    }


def capability_rows() -> list[dict[str, Any]]:
    document = census.load_json(CAPABILITY_MAP)
    wanted = set(CAPABILITY_ANCHORS.values())
    rows = [
        dict(row)
        for row in document.get("rows") or []
        if str(row.get("gt6_anchor") or "") in wanted
    ]
    if len(rows) != 3:
        raise ValueError(f"capability map crops/food/bees rows {len(rows)} != 3")
    by_anchor = {str(row["gt6_anchor"]): row for row in rows}
    ordered: list[dict[str, Any]] = []
    for name in PINNED_NAMES:
        row = by_anchor.get(CAPABILITY_ANCHORS[name])
        if row is None:
            raise ValueError(f"missing capability map row {CAPABILITY_ANCHORS[name]}")
        if row.get("cc_mechanism") != "none":
            raise ValueError(
                f"{row.get('gt6_anchor')} cc_mechanism is {row.get('cc_mechanism')}"
            )
        if row.get("correspondence_class") != "out_of_scope_historical":
            raise ValueError(
                f"{row.get('gt6_anchor')} correspondence_class is "
                f"{row.get('correspondence_class')}"
            )
        if row.get("growth_blocker") != "no_owner":
            raise ValueError(
                f"{row.get('gt6_anchor')} growth_blocker is {row.get('growth_blocker')}"
            )
        if row.get("1x_disposition") != "not_in_1x":
            raise ValueError(
                f"{row.get('gt6_anchor')} 1x_disposition is {row.get('1x_disposition')}"
            )
        ordered.append(row)
    return ordered


def capability_row_for(category: str) -> dict[str, Any]:
    expected = CAPABILITY_ANCHORS[category]
    for row in capability_rows():
        if row.get("gt6_anchor") == expected:
            return row
    raise ValueError(f"missing capability map row {expected}")


def capability_seed_row() -> dict[str, Any]:
    document = census.load_json(CAPABILITY_MAP)
    for row in document.get("rows") or []:
        if row.get("gt6_anchor") == SEED_ANCHOR:
            return dict(row)
    raise ValueError("missing capability map seed row for crops / food / bees")


def generic_importer_status() -> str:
    readiness = census.load_json(wave_dir(GENERIC_IMPORT_SLUG) / "readiness.json")
    status = str(readiness.get("status") or "")
    if status != GENERIC_IMPORT_STATUS:
        raise ValueError(f"generic importer status {status} != {GENERIC_IMPORT_STATUS}")
    return status


def importer_creates_recipe_maps() -> bool:
    text = SOURCE_IMPORT.read_text(encoding="utf-8").lower()
    return any(
        token in text
        for token in (
            "recipemap.create",
            "new recipemap",
            "gt.recipe.squeezer",
            "gt.recipe.juicer",
            "gt.recipe.fermenter",
            "gt.recipe.bumblequeen",
            "gt.recipe.bumblelyzer",
        )
    )


def dimension_policy() -> str:
    policy = census.load_json(T20_POLICY)
    text = str(policy["classification"]["dimension_policy"])
    if "DESIGN_POLICY_OVERWORLD_ACCESS" not in text:
        raise ValueError("T20 dimension_policy missing DESIGN_POLICY_OVERWORLD_ACCESS")
    return text


def count_ceiling_status() -> str:
    document = census.load_json(COUNT_CEILING_REFS)
    status = str(document.get("status") or "")
    if status != "COUNT_CEILING_REFS_READY":
        raise ValueError(f"count-ceiling status {status} != COUNT_CEILING_REFS_READY")
    return status


def processing_host_kinds() -> list[str]:
    document = census.load_json(MACHINE_TIERS)
    kinds = sorted(
        {
            str(row.get("kind") or "")
            for row in document.get("variants") or []
            if row.get("kind")
        }
    )
    blocked = [
        kind
        for kind in kinds
        if any(token in kind.lower() for token in HOST_TOKENS)
    ]
    if blocked:
        raise ValueError(f"machine_tiers grew crops/food/bees hosts: {blocked}")
    return kinds


def recipe_map_create_ids() -> list[str]:
    text = RECIPE_MAPS_JAVA.read_text(encoding="utf-8")
    ids = [
        line.split('create("', 1)[1].split('")', 1)[0]
        for line in text.splitlines()
        if 'create("' in line
    ]
    blocked = [
        map_id
        for map_id in ids
        if any(token in map_id.lower() for token in HOST_TOKENS)
    ]
    if blocked:
        raise ValueError(f"ModRecipeMaps grew crops/food/bees maps: {blocked}")
    return ids


def processing_machine_text() -> str:
    return PROCESSING_MACHINES.read_text(encoding="utf-8").lower()


def t35_excluded_records() -> dict[str, dict[str, Any]]:
    document = census.load_json(T35_EXCLUSION)
    families = {
        str(row["canonical_family"]): dict(row)
        for row in document.get("family_summaries") or []
    }
    sites = {
        str(row["canonical_family"]): dict(row)
        for row in document.get("source_sites") or []
        if str(row.get("canonical_family") or "") in EXPECTED_T35_BEHAVIOR
    }
    records: dict[str, dict[str, Any]] = {}
    for family, expected_class in EXPECTED_T35_BEHAVIOR.items():
        summary = families.get(family)
        site = sites.get(family)
        if summary is None:
            raise ValueError(f"T35 family {family} missing")
        if site is None:
            raise ValueError(f"T35 site {family} missing")
        if site.get("behavior_class") != expected_class:
            raise ValueError(
                f"{family} behavior_class {site.get('behavior_class')} "
                f"!= {expected_class}"
            )
        if int(summary.get("source_sites") or 0) != 1:
            raise ValueError(f"{family} is not a singleton T35 exclusion")
        records[family] = {
            "behavior_class": expected_class,
            "canonical_family": family,
            "historical_exclusion_reasons": list(
                summary.get("historical_exclusion_reasons") or []
            ),
            "owner_here": False,
            "source_sites": 1,
        }
    return records


def t20_catalog_counts() -> dict[str, Any]:
    veins = census.load_json(ORE_VEINS)
    deposits = census.load_json(FLUID_DEPOSITS)
    scatter = census.load_json(SURFACE_SCATTER)
    vein_count = len(veins.get("veins") or [])
    deposit_count = len(deposits.get("deposits") or [])
    scatter_id = str(scatter.get("id") or "")
    if vein_count != 129:
        raise ValueError(f"ore_veins count {vein_count} != 129")
    if deposit_count != 2:
        raise ValueError(f"fluid_deposits count {deposit_count} != 2")
    if scatter_id != "surface_rock_scatter":
        raise ValueError(f"surface_scatter id {scatter_id} != surface_rock_scatter")
    return {
        "fluid_deposit_count": deposit_count,
        "ore_vein_count": vein_count,
        "owner_here": False,
        "surface_scatter_id": scatter_id,
    }


def worldgen_catalog_mentions_slice() -> list[str]:
    tokens = (
        "plant.glowtus",
        "plant.bush",
        "WorldgenGlowtus",
        "WorldgenBushes",
        "WorldgenHives",
        "bumblehives",
    )
    hits: list[str] = []
    for path in WORLDGEN_CATALOG.glob("*.json"):
        text = path.read_text(encoding="utf-8")
        for token in tokens:
            if token in text:
                hits.append(f"{path.name}:{token}")
    return hits


def runtime_corpus() -> str:
    parts = [
        MACHINE_TIERS.read_text(encoding="utf-8"),
        PROCESSING_MACHINES.read_text(encoding="utf-8"),
        RECIPE_MAPS_JAVA.read_text(encoding="utf-8"),
    ]
    for directory in (CONFIGURED_FEATURE, PLACED_FEATURE, WORLDGEN_JAVA, WORLDGEN_CATALOG):
        if not directory.exists():
            continue
        for path in sorted(directory.rglob("*")):
            if path.is_file() and path.suffix in {".json", ".java"}:
                parts.append(path.read_text(encoding="utf-8"))
    return " ".join(parts).lower()


def named_runtime_hits(category: str) -> list[str]:
    blob = runtime_corpus()
    return sorted(token for token in CATEGORY_TOKENS[category] if token in blob)


def already_closed_elsewhere() -> dict[str, Any]:
    non_ore = census.load_json(wave_dir(NON_ORE_SLUG) / "inherited_denominator.json")
    remainder = non_ore.get("remainder_after_slice") or {}
    histogram = remainder.get("type_histogram") or {}
    if int(non_ore.get("feature_count") or -1) != NON_ORE_SLICE:
        raise ValueError("non-ore feature_count drifted from 18")
    if int(remainder.get("feature_count") or -1) != REMAINDER_AFTER_NON_ORE:
        raise ValueError("non-ore remainder_after_slice drifted from 172")
    if int(histogram.get("WorldgenHives") or -1) != 10:
        raise ValueError("WorldgenHives were not non-ore remainder")
    if int(histogram.get("WorldgenGlowtus") or -1) != 1:
        raise ValueError("WorldgenGlowtus was not non-ore remainder")
    if int(histogram.get("WorldgenBushes") or -1) != 1:
        raise ValueError("WorldgenBushes was not non-ore remainder")
    t20 = t20_catalog_counts()
    return {
        "count_ceiling_kind_envelope": {
            "owner_here": False,
            "status": count_ceiling_status(),
            "telemetry_report_only": True,
        },
        "generic_recipe_import": {
            "creates_recipe_maps": False,
            "owner_here": False,
            "status": generic_importer_status(),
        },
        "non_ore_worldgen_r0": {
            "feature_count": NON_ORE_SLICE,
            "full_other_features": FULL_OTHER_FEATURES,
            "owner_here": False,
            "remainder_after_slice": REMAINDER_AFTER_NON_ORE,
            "status": NON_ORE_STATUS,
        },
        "worldgen_worldgen_catalog": t20,
        "vanilla_replace_r0": {
            "owner_here": False,
            "status": PREDECESSOR_STATUS,
        },
    }


def inherited_denominator_document() -> dict[str, Any]:
    features = load_other_features()
    if len(features) != FULL_OTHER_FEATURES:
        raise ValueError(f"other_features.json count {len(features)} != 190")
    live_by_name = {feature_name(row): row for row in features}
    if len(live_by_name) != len(features):
        raise ValueError("other_features.json names are not unique")
    maps = dump_maps()
    non_ore_names = non_ore_owned_names()
    sliced: list[dict[str, Any]] = []
    remainder: list[dict[str, Any]] = []
    by_category: dict[str, list[dict[str, Any]]] = {name: [] for name in PINNED_NAMES}
    leaked_into_non_ore: list[str] = []
    for row in features:
        category = category_of(row)
        copied = dict(row)
        name = feature_name(row)
        if category is not None:
            if name in non_ore_names:
                leaked_into_non_ore.append(name)
            sliced.append(copied)
            by_category[category].append(copied)
            continue
        if name in non_ore_names:
            continue
        remainder.append(copied)
    if leaked_into_non_ore:
        raise ValueError(
            f"crops/food/bees features owned by non-ore R0: {leaked_into_non_ore}"
        )
    if len(sliced) != EXPECTED_SLICE:
        raise ValueError(f"pinned dump features {len(sliced)} != 12")
    if len(remainder) != REMAINDER_AFTER_THIS_SLICE:
        raise ValueError(f"remainder after this slice {len(remainder)} != 160")
    if len(sliced) + NON_ORE_SLICE + len(remainder) != FULL_OTHER_FEATURES:
        raise ValueError("non-ore 18, this slice 12, and remainder 160 must cover 190")
    leftover_types = {feature_type(row) for row in remainder}
    if leftover_types & {"WorldgenGlowtus", "WorldgenBushes", "WorldgenHives"}:
        raise ValueError("plant.* / WorldgenHives leaked into remainder")
    capability = capability_rows()
    seed = capability_seed_row()
    category_rows: list[dict[str, Any]] = []
    dump_context = 0
    for name in PINNED_NAMES:
        expected_names = PINNED_WORLDGEN[name]
        rows = by_category[name]
        names = tuple(feature_name(row) for row in rows)
        if names != expected_names:
            raise ValueError(f"{name} worldgen names {names} != {expected_names}")
        for row in rows:
            live = live_by_name[feature_name(row)]
            drift = census.first_json_diff(live, row)
            if drift:
                raise ValueError(
                    f"{feature_name(row)} is not byte-identical to dump: {drift}"
                )
        capability_row = capability_row_for(name)
        live_capability = next(
            row for row in capability if row["gt6_anchor"] == capability_row["gt6_anchor"]
        )
        drift = census.first_json_diff(live_capability, capability_row)
        if drift:
            raise ValueError(f"{name} capability row is not byte-identical: {drift}")
        dump_rows = [
            dump_map_record(maps[map_name], is_census=False)
            for map_name in PINNED_DUMP_MAPS[name]
        ]
        dump_context += sum(int(row["recipeCount"]) for row in dump_rows)
        category_rows.append(
            {
                "category": name,
                "capability_row": capability_row,
                "dump_maps": dump_rows,
                "dump_recipe_count_context": sum(
                    int(row["recipeCount"]) for row in dump_rows
                ),
                "feature_count": len(expected_names),
                "features": rows,
                "gt6_anchor": CAPABILITY_ANCHORS[name],
                "names": list(expected_names),
                "types": [feature_type(row) for row in rows],
            }
        )
    if dump_context != EXPECTED_DUMP_CONTEXT:
        raise ValueError(f"dump recipeCount context {dump_context} != 13373")
    plantalyzer = dump_map_record(maps[PLANTALYZER_MAP], is_census=False)
    histogram = dict(sorted(Counter(feature_type(row) for row in remainder).items()))
    return {
        "already_closed_elsewhere": already_closed_elsewhere(),
        "authority_hashes": authority_hashes(),
        "capability_map_seed": seed,
        "categories": category_rows,
        "category_count": 3,
        "dump": {
            "feature_count": FULL_OTHER_FEATURES,
            "path": census.relative(WORLDGEN_OTHER),
            "recipe_index": census.relative(DUMP_INDEX),
            "sha256": census.sha256_file(WORLDGEN_OTHER),
        },
        "dump_recipe_count_context": EXPECTED_DUMP_CONTEXT,
        "feature_count": EXPECTED_SLICE,
        "features": sliced,
        "full_other_features": FULL_OTHER_FEATURES,
        "generated_by": GENERATED_BY,
        "non_ore_owned_feature_count": NON_ORE_SLICE,
        "plantalyzer": plantalyzer,
        "remainder_after_non_ore": REMAINDER_AFTER_NON_ORE,
        "remainder_after_this_slice": {
            "feature_count": REMAINDER_AFTER_THIS_SLICE,
            "type_histogram": histogram,
        },
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "status": "INHERITED_DENOMINATOR_READY",
        "wave_slug": SLUG,
    }


def source_semantics_document(inherited: dict[str, Any]) -> dict[str, Any]:
    by_category = {row["category"]: row for row in inherited["categories"]}
    rows: list[dict[str, Any]] = []
    for name in PINNED_NAMES:
        category = by_category[name]
        if name == "Crops":
            contrast = {
                "not_wood_recipes": True,
                "plant_worldgen": list(PINNED_PLANTS),
                "plantalyzer_is_not_census": True,
                "squeezer_map": "gt.recipe.squeezer",
            }
        elif name == "Food":
            contrast = {
                "fermenter_map": "gt.recipe.fermenter",
                "juicer_map": "gt.recipe.juicer",
                "not_smelter": True,
                "worldgen_in_this_card": False,
            }
        else:
            contrast = {
                "bumblelyzer_map": "gt.recipe.bumblelyzer",
                "bumblequeen_map": "gt.recipe.bumblequeen",
                "hive_dimensions": [
                    name.split(".", 1)[0] for name in PINNED_HIVES
                ],
                "hive_worldgen": list(PINNED_HIVES),
            }
        rows.append(
            {
                "category": name,
                "contrast": contrast,
                "dump_recipe_count_context": category["dump_recipe_count_context"],
                "evidence_class": "SOURCE_BACKED",
                "feature_count": category["feature_count"],
                "gt6_anchor": category["gt6_anchor"],
                "names": list(category["names"]),
            }
        )
    return {
        "categories": rows,
        "generated_by": GENERATED_BY,
        "plantalyzer_recipe_count": inherited["plantalyzer"]["recipeCount"],
        "plantalyzer_used_as_census": False,
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "status": "SOURCE_SEMANTICS_READY",
        "wave_slug": SLUG,
    }


def existing_mechanism_document() -> dict[str, Any]:
    kinds_hosts = processing_host_kinds()
    map_ids = recipe_map_create_ids()
    processing_text = processing_machine_text()
    blocked_hosts = [
        token
        for token in HOST_TOKENS
        if token in processing_text
        or any(token in kind.lower() for kind in kinds_hosts)
        or any(token in map_id.lower() for map_id in map_ids)
    ]
    if blocked_hosts:
        raise ValueError(
            f"CC runtime grew crops/food/bees hosts: {blocked_hosts}"
        )
    if importer_creates_recipe_maps():
        raise ValueError("generic importer must not create crops/food/bees RecipeMaps")
    catalog_hits = worldgen_catalog_mentions_slice()
    if catalog_hits:
        raise ValueError(f"worldgen_catalog grew slice ids: {catalog_hits}")
    excluded = t35_excluded_records()
    importer = generic_importer_status()
    policy = dimension_policy()
    analogs = {
        "Crops": (
            "T20 ore/fluid/scatter catalogs and authored scatter JSON are not "
            "plant.glowtus / plant.bush growth, and there is no squeezer host"
        ),
        "Food": (
            "Existing smelter / mixer RecipeMaps and the generic importer are "
            "not juicer or fermenter maps"
        ),
        "Bees": (
            "T20 DESIGN_POLICY_OVERWORLD_ACCESS is not ten-dimension "
            "WorldgenHives, and there is no bumble host"
        ),
    }
    mapped: list[dict[str, Any]] = []
    for name in PINNED_NAMES:
        hits = named_runtime_hits(name)
        row = capability_row_for(name)
        mechanism = str(row["cc_mechanism"])
        mapped.append(
            {
                "adjacent_analog": analogs[name],
                "category": name,
                "cc_mechanism": mechanism,
                "gap": "mechanism" if mechanism == "none" else "none",
                "named_runtime_hits": hits,
                "census_excluded_mtes": [
                    excluded[family]
                    for family in EXPECTED_T35_FAMILIES[name]
                ],
            }
        )
        if hits:
            raise ValueError(f"{name} unexpectedly maps onto CC runtime {hits}")
    return {
        "already_closed_elsewhere": already_closed_elsewhere(),
        "capability_map_crops_food_bees": capability_rows(),
        "capability_map_seed": capability_seed_row(),
        "count_ceiling_status": count_ceiling_status(),
        "dimension_policy": policy,
        "generated_by": GENERATED_BY,
        "generic_importer_creates_recipe_maps": False,
        "generic_importer_status": importer,
        "kinds": mapped,
        "machine_tier_kind_count": len(kinds_hosts),
        "note": (
            "The gap is a missing mechanism, not a missing smelter recipe. "
            "T35 already excluded juicer / fermenter / squeezer / bumble hive "
            "MTEs from the 1.x machine tree. Generic importer READY and T20 "
            "ore/fluid/scatter catalogs do not cover plant.*, juicer / fermenter, "
            "or WorldgenHives. WorldgenHives were non-ore remainder and are "
            "owned here as Bees. Count-ceiling / kind envelope stays report-only."
        ),
        "nuclear_started": nuclear_started(),
        "recipe_map_ids": map_ids,
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "status": "EXISTING_MECHANISM_READY",
        "worldgen_worldgen_catalog": t20_catalog_counts(),
        "census_excluded_mtes": excluded,
        "wave_slug": SLUG,
        "worldgen_catalog_slice_hits": catalog_hits,
    }


def crops_contract_document() -> dict[str, Any]:
    return {
        "generated_by": GENERATED_BY,
        "implemented": False,
        "questions": dict(CROPS_QUESTIONS),
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "status": "CROPS_CONTRACT_READY",
        "wave_slug": SLUG,
    }


def category_reasons(name: str) -> list[str]:
    if name == "Crops":
        return [
            "Crops capability-map cc_mechanism is none.",
            "plant.glowtus / plant.bush are crop growth, not another scatter JSON row.",
            "There is no squeezer RecipeMap or host. T35 already excluded the squeezer MTE.",
        ]
    if name == "Food":
        return [
            "Food capability-map cc_mechanism is none.",
            "There is no juicer or fermenter host. Generic importer does not create new RecipeMaps.",
            "T35 already excluded juicer and fermenter MTEs from the 1.x machine tree.",
        ]
    return [
        "Bees capability-map cc_mechanism is none.",
        "There is no bumble host. T35 already excluded the bumble hive MTE.",
        "Ten-dimension WorldgenHives do not fit T20 DESIGN_POLICY_OVERWORLD_ACCESS.",
    ]


def derive_feasibility(
    inherited: dict[str, Any],
    semantics: dict[str, Any],
    mechanism: dict[str, Any],
    contract: dict[str, Any],
) -> dict[str, Any]:
    mechanism_by_category = {row["category"]: row for row in mechanism["kinds"]}
    semantics_by_category = {row["category"]: row for row in semantics["categories"]}
    categories: list[dict[str, Any]] = []
    for name in PINNED_NAMES:
        missing: list[str] = []
        summary = next(row for row in inherited["categories"] if row["category"] == name)
        expected_features = len(PINNED_WORLDGEN[name])
        expected_maps = len(PINNED_DUMP_MAPS[name])
        if int(summary["feature_count"]) != expected_features:
            missing.append("dump")
        if len(summary.get("dump_maps") or []) != expected_maps:
            missing.append("dump")
        semantic = semantics_by_category.get(name)
        if semantic is None or int(semantic["feature_count"]) != expected_features:
            missing.append("dump")
        if set(contract["questions"]) != set(CROPS_QUESTIONS):
            missing.append("schema")
        scale_text = str(contract["questions"].get("scale") or "")
        if "13373" not in scale_text.replace(",", "") and "13,373" not in scale_text:
            missing.append("measurement")
        if "production lock" not in scale_text.lower():
            missing.append("measurement")
        mapped = mechanism_by_category[name]
        destination: str | None = None
        if missing:
            verdict = "blocked"
            allows_child = False
            reasons = [f"{name} is missing " + ", ".join(sorted(set(missing)))]
        elif mapped["cc_mechanism"] != "none":
            verdict = "bounded_extension"
            allows_child = True
            reasons = [
                f"{name} maps onto named existing CC runtime {mapped['cc_mechanism']}."
            ]
        else:
            verdict = "requires_new_runtime"
            allows_child = False
            reasons = category_reasons(name)
        if verdict not in FEASIBILITY_VALUES:
            raise ValueError(f"illegal feasibility {verdict}")
        categories.append(
            {
                "allows_implementation_child": allows_child,
                "category": name,
                "destination": destination,
                "missing_evidence": sorted(set(missing)),
                "reasons": reasons,
                "verdict": verdict,
            }
        )
    blocked = [row for row in categories if row["verdict"] == "blocked"]
    allows_any = any(row["allows_implementation_child"] for row in categories)
    return {
        "allows_implementation_child": allows_any,
        "categories": categories,
        "generated_by": GENERATED_BY,
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "status": "BLOCKED" if blocked else "FEASIBILITY_READY",
        "wave_slug": SLUG,
    }


def recipe_generated_mentions_slug() -> bool:
    if not RECIPE_GENERATED.is_dir():
        return False
    token = "crops-food-bees"
    for path in RECIPE_GENERATED.rglob("*"):
        if token in path.as_posix():
            return True
    return False


def readiness_note(feasibility: dict[str, Any]) -> str:
    parts = [
        f"{row['category']} {row['verdict']}" for row in feasibility["categories"]
    ]
    return (
        f"{STATUS}. Three crops / food / bees categories inherited "
        "byte-identical capability-map rows, 12/190 dump features, and "
        "13,373 dump recipeCount as scale. "
        + "; ".join(parts)
        + ". No implementation child is assigned. unique_active_wave is null."
    )


def evidence_document(
    inherited: dict[str, Any],
    feasibility: dict[str, Any],
) -> dict[str, Any]:
    leftover = leftover_later_count()
    return {
        "allows_implementation_child": feasibility["allows_implementation_child"],
        "completion_delta": 0,
        "dump_recipe_count_context": inherited["dump_recipe_count_context"],
        "feasibility_by_category": {
            row["category"]: row["verdict"] for row in feasibility["categories"]
        },
        "full_other_features": inherited["full_other_features"],
        "generated_recipe_count": 0,
        "inherited_category_count": inherited["category_count"],
        "inherited_feature_count": inherited["feature_count"],
        "leftover_later_count": leftover,
        "nuclear_track_c_started": nuclear_started(),
        "owns_families": 0,
        "partial_family_count": 0,
        "plantalyzer_used_as_census": False,
        "production_lock": None,
        "recipe_files_generated": False,
        "remainder_after_this_slice": inherited["remainder_after_this_slice"][
            "feature_count"
        ],
    }


def common_documents(*, evidence: dict[str, Any], note: str) -> dict[str, Any]:
    spec = spec_for(SLUG)
    census = {
        "complete_family_count": 0,
        "completion_delta": 0,
        "generated_by": GENERATED_BY,
        "leftover_later_count": leftover_later_count(),
        "partial_family_count": 0,
        "remaining_recipe_gap": 0,
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "status": "CENSUS_DELTA_READY",
        "wave_slug": SLUG,
        "work_set": {"family_count": 0, "source_rows": 0},
    }
    topology = {
        "append_only": False,
        "complete_family_count": 0,
        "generated_by": GENERATED_BY,
        "next_unassigned": spec.next_unassigned,
        "remaining_recipe_gap": 0,
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "status": "WAVE_READY",
        "unique_active_wave": spec.unique_active_wave,
        "wave_slug": SLUG,
    }
    readiness = {
        "evidence": evidence,
        "generated_by": GENERATED_BY,
        "next_unassigned": spec.next_unassigned,
        "note": note,
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "status": STATUS,
        "unique_active_wave": spec.unique_active_wave,
        "wave_complete": True,
        "wave_slug": SLUG,
    }
    wave = {
        "cohort": "crops-food-bees-r0",
        "depends_on": [PREDECESSOR],
        "generated_by": GENERATED_BY,
        "owns_families": 0,
        "program": SLUG,
        "schema_version": 1,
        "wave_slug": SLUG,
    }
    return {
        "census_delta.json": census,
        "readiness.json": readiness,
        "topology.json": topology,
        "wave.json": wave,
    }


def build_r0_documents() -> dict[str, Any]:
    errors = require_predecessor()
    if errors:
        raise ValueError("; ".join(errors))
    inherited = inherited_denominator_document()
    semantics = source_semantics_document(inherited)
    mechanism = existing_mechanism_document()
    contract = crops_contract_document()
    feasibility = derive_feasibility(inherited, semantics, mechanism, contract)
    blocked = [
        row for row in feasibility["categories"] if row["verdict"] == "blocked"
    ]
    if blocked:
        detail = ", ".join(
            f"{row['category']}:" + ",".join(row["missing_evidence"] or ["blocked"])
            for row in blocked
        )
        raise ValueError(f"feasibility is blocked: {detail}")
    if recipe_generated_mentions_slug():
        raise ValueError("src/recipe_generated mentions crops-food-bees")
    if worldgen_catalog_mentions_slice():
        raise ValueError("worldgen_catalog must not grow plant.* or hive ids")
    if nuclear_started():
        raise ValueError("nuclear Track C started must stay false")
    evidence = evidence_document(inherited, feasibility)
    documents = common_documents(
        evidence=evidence,
        note=readiness_note(feasibility),
    )
    documents["inherited_denominator.json"] = inherited
    documents["source_semantics.json"] = semantics
    documents["existing_mechanism.json"] = mechanism
    documents["crops_contract.json"] = contract
    documents["feasibility.json"] = feasibility
    return documents


def write_seal() -> dict[str, Any]:
    root = wave_dir(SLUG)
    hashes = {
        "census": census.sha256_file(root / "census_delta.json"),
        "gametest_java": None,
        "gametest_log": None,
        "generated_recipes": None,
        "locked_support": None,
        "production_lock": None,
        "publication_group_manifest": None,
        "readiness": census.sha256_file(root / "readiness.json"),
        "receipt": None,
        "runtime_dependency_manifest": None,
        "shard_manifest": None,
        "topology": census.sha256_file(root / "topology.json"),
    }
    seal = {
        "card_id": SLUG,
        "complete_family_count": 0,
        "composed_identity_ledger_v2_sha256": census.sha256_file(
            closeout_seal.IDENTITY_LEDGER_V2
        ),
        "composed_runtime_manifest_v2_sha256": census.sha256_file(
            closeout_seal.RUNTIME_MANIFEST_V2
        ),
        "gametest_status": "NONE",
        "generated_by": f"{GENERATED_BY} --write",
        "hashes": hashes,
        "note": census.load_json(root / "readiness.json").get("note"),
        "production_lock_sha256": None,
        "receipt_sha256": None,
        "reclassification_delta": 0,
        "relation_count": 0,
        "remaining_recipe_gap": 0,
        "schema_version": 1,
        "sealed_at_wave": SLUG,
        "source_revision": SOURCE_REVISION,
        "status": "SEALED",
    }
    census.write_stable(root / "closeout_seal.json", seal)
    return seal


def write_artifacts() -> dict[str, Any]:
    documents = build_r0_documents()
    root = wave_dir(SLUG)
    root.mkdir(parents=True, exist_ok=True)
    for name, document in documents.items():
        census.write_stable(root / name, document)
    write_seal()
    spec = spec_for(SLUG)
    return {
        "status": STATUS,
        "unique_active_wave": spec.unique_active_wave,
        "wave_slug": SLUG,
    }


def check_forbidden_successors(haystack: str) -> list[str]:
    errors: list[str] = []
    for token in FORBIDDEN_SUCCESSORS:
        if token in haystack:
            errors.append(f"topology successor {token} is forbidden")
    return errors


def check_artifacts() -> list[str]:
    errors = require_registered(SLUG)
    if errors:
        return errors
    spec = spec_for(SLUG)
    if spec.owns_families != 0:
        errors.append(f"{SLUG} owns_families must be 0")
    if spec.production_lock is not None:
        errors.append(f"{SLUG} must not carry a production lock")
    if spec.unique_active_wave is not None:
        errors.append("unique_active_wave must be null")
    if not spec.next_unassigned:
        errors.append("next_unassigned must be true")
    errors.extend(check_forbidden_successors(spec.unique_active_wave or ""))
    root = wave_dir(SLUG)
    if not (root / "readiness.json").is_file():
        return errors + [f"{SLUG} artifacts are missing"]
    try:
        live = build_r0_documents()
    except ValueError as error:
        return errors + [str(error)]
    for name, document in live.items():
        committed = census.load_json(root / name)
        drift = census.first_json_diff(document, committed)
        if drift:
            errors.append(f"{name} drifted: {drift}")
    readiness = census.load_json(root / "readiness.json")
    if readiness.get("status") != STATUS:
        errors.append(f"{SLUG} status drifted")
    if readiness.get("unique_active_wave") is not None:
        errors.append(f"{SLUG} unique_active_wave must be null")
    if nuclear_started():
        errors.append("nuclear Track C started must stay false")
    evidence = readiness.get("evidence") or {}
    if int(evidence.get("completion_delta", 1)) != 0:
        errors.append(f"{SLUG} completion_delta must be 0")
    if evidence.get("recipe_files_generated"):
        errors.append(f"{SLUG} must not generate recipes")
    if int(evidence.get("leftover_later_count", 0)) != 39:
        errors.append(f"{SLUG} leftover_later_count must be 39")
    if int(evidence.get("inherited_category_count", 0)) != 3:
        errors.append("inherited_category_count must be 3")
    if int(evidence.get("inherited_feature_count", 0)) != EXPECTED_SLICE:
        errors.append("inherited_feature_count must be 12")
    if int(evidence.get("full_other_features", 0)) != FULL_OTHER_FEATURES:
        errors.append("full_other_features must be 190")
    if int(evidence.get("dump_recipe_count_context", 0)) != EXPECTED_DUMP_CONTEXT:
        errors.append("dump_recipe_count_context must be 13373")
    if int(evidence.get("remainder_after_this_slice", 0)) != REMAINDER_AFTER_THIS_SLICE:
        errors.append("remainder_after_this_slice must be 160")
    if evidence.get("plantalyzer_used_as_census"):
        errors.append("plantalyzer must not be used as census")
    topology = census.load_json(root / "topology.json")
    extra = sorted(set(topology) - set(ALLOWED_TOPOLOGY_KEYS))
    if extra:
        errors.append(f"topology has forbidden keys: {extra}")
    errors.extend(check_forbidden_successors(json.dumps(topology, sort_keys=True)))
    feasibility = census.load_json(root / "feasibility.json")
    by_category = {row["category"]: row for row in feasibility.get("categories") or []}
    if tuple(by_category) != PINNED_NAMES:
        errors.append("feasibility categories drifted from the pinned three")
    for name in PINNED_NAMES:
        row = by_category.get(name)
        if row is None:
            errors.append(f"feasibility missing {name}")
            continue
        if row.get("verdict") not in FEASIBILITY_VALUES:
            errors.append(f"{name} feasibility.verdict is not an allowed enum")
        if row.get("verdict") != "bounded_extension" and row.get(
            "allows_implementation_child"
        ):
            errors.append(f"{name} must not allow an implementation child")
        if row.get("verdict") == "defer_to_portfolio":
            errors.append(f"{name} must not defer_to_portfolio on this slice")
    inherited = census.load_json(root / "inherited_denominator.json")
    if int(inherited.get("category_count", 0)) != 3:
        errors.append("inherited category_count must be 3")
    if int(inherited.get("feature_count", 0)) != EXPECTED_SLICE:
        errors.append("inherited feature_count must be 12")
    if int(inherited.get("dump_recipe_count_context", 0)) != EXPECTED_DUMP_CONTEXT:
        errors.append("inherited dump_recipe_count_context must be 13373")
    live_rows = {row["gt6_anchor"]: row for row in capability_rows()}
    for category in inherited.get("categories") or []:
        live_row = live_rows.get(category.get("gt6_anchor"))
        if live_row is None:
            errors.append(f"{category.get('category')} missing from live capability map")
            continue
        drift = census.first_json_diff(live_row, category.get("capability_row"))
        if drift:
            errors.append(
                f"{category.get('category')} is not byte-identical to capability map: "
                f"{drift}"
            )
    live_features = load_other_features()
    live_by_name = {feature_name(row): row for row in live_features}
    for row in inherited.get("features") or []:
        live_row = live_by_name.get(feature_name(row))
        if live_row is None:
            errors.append(f"{feature_name(row)} missing from live dump")
            continue
        drift = census.first_json_diff(live_row, row)
        if drift:
            errors.append(
                f"{feature_name(row)} is not byte-identical to dump: {drift}"
            )
    hashes = inherited.get("authority_hashes") or {}
    live_hashes = authority_hashes()
    for key, digest in live_hashes.items():
        if hashes.get(key) != digest:
            errors.append(f"authority hash {key} drifted")
    if recipe_generated_mentions_slug():
        errors.append("src/recipe_generated mentions crops-food-bees")
    catalog_hits = worldgen_catalog_mentions_slice()
    if catalog_hits:
        errors.append(f"worldgen_catalog grew slice ids: {catalog_hits}")
    errors.extend(closeout_seal.check_wave_seal(SLUG))
    return errors


def main_for(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=f"Write or check {SLUG}.")
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args(argv)
    if args.write == args.check:
        parser.error("choose --write or --check")
    try:
        if args.write:
            print(json.dumps(write_artifacts(), sort_keys=True))
            return 0
        errors = check_artifacts()
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        print(f"{SLUG} closeout derivation is current")
        return 0
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"{SLUG} failed: {error}", file=sys.stderr)
        return 1
