#!/usr/bin/env python3
"""portfolio/non-ore-worldgen-r0: freeze four non-ore worldgen dump categories."""
from __future__ import annotations

import argparse
import json
import sys
from collections import Counter
from typing import Any

from tools import closeout_seal
from tools import portfolio_one_x as one_x
from tools import t35_common as t35
from tools.recipe_bulk.slugs import KNOWN_SEMANTIC_SLUGS
from tools.wave_closeout import known_slugs
from tools.wave_closeout import spec_for
from tools.wave_closeout import wave_dir

SLUG = "portfolio/non-ore-worldgen-r0"
PREDECESSOR = "portfolio/t13c-exclusion-reclaim-r0"
PREDECESSOR_STATUS = "T13C_EXCLUSION_RECLAIM_R0_READY"
LOGISTICS_SLUG = "portfolio/logistics-cover-net-r0"
STATUS = "NON_ORE_WORLDGEN_R0_READY"
SOURCE_REVISION = t35.SOURCE_REVISION
GENERATED_BY = "python tools/build_non_ore_worldgen_r0.py"
PINNED_CATEGORIES = (
    ("Trees", 9, ("tree.rubber", "tree.maple", "tree.willow", "tree.bluemahoe",
                  "tree.hazel", "tree.cinnamon", "tree.coconut", "tree.rainbowood",
                  "tree.bluespruce")),
    ("Dungeons", 1, ("overworld.structure.dungeon.large",)),
    ("Planets", 3, ("moon.rocks", "mars.rocks", "planet.rocks")),
    ("Center", 5, ("center.biomes", "center.streets", "center.nexus",
                   "center.beacon", "center.testing")),
)
PINNED_NAMES = tuple(row[0] for row in PINNED_CATEGORIES)
EXPECTED_SLICE = 18
FULL_OTHER_FEATURES = 190
REMAINDER_AFTER_SLICE = 172
EXPECTED_ORE_VEINS = 129
EXPECTED_FLUID_DEPOSITS = 2
EXPECTED_AUTHORED_FEATURES = 9
EXPECTED_DUMP_SPRINGS = 16
PLANET_NAMES = frozenset({"planet.rocks", "moon.rocks", "mars.rocks"})
EXTRA_DIMENSION_ROCKS = frozenset({"aether.rocks", "erebus.rocks", "alfheim.rocks"})
TREE_MATERIAL_TOKENS = (
    "rubber",
    "maple",
    "willow",
    "bluemahoe",
    "hazel",
    "cinnamon",
    "coconut",
    "rainbowood",
    "bluespruce",
)
CAPABILITY_ANCHORS = {
    "Trees": "gt6_dump/gt6_recipe_dump/worldgen/other_features.json WorldgenTree*",
    "Dungeons": "gt6_dump/gt6_recipe_dump/worldgen/other_features.json WorldgenDungeonGT",
    "Planets": (
        "gt6_dump/gt6_recipe_dump/worldgen/other_features.json "
        "WorldgenPlanetRocks / moon.rocks / mars.rocks"
    ),
    "Center": (
        "gt6_dump/gt6_recipe_dump/worldgen/other_features.json "
        "WorldgenCenterBiomes / center.*"
    ),
}
SEED_ANCHOR = "non-ore worldgen (trees, dungeons, item scatter remainder)"
FEASIBILITY_VALUES = (
    "bounded_extension",
    "requires_new_runtime",
    "defer_to_portfolio",
    "blocked",
)
FORBIDDEN_SUCCESSORS = (
    "trees-implementation",
    "dungeons-implementation",
    "planets-implementation",
    "center-implementation",
    "remainder-other-features",
    "vanilla-replace",
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
    "Trees": ("worldgentree", "tree.rubber", "tree.maple", "sapling", "treegrower"),
    "Dungeons": ("worldgendungeongt", "overworld.structure.dungeon"),
    "Planets": (
        "moon.rocks",
        "mars.rocks",
        "planet.rocks",
        "worldgenmoonrocks",
        "worldgenmarsrocks",
        "worldgenplanetrocks",
    ),
    "Center": (
        "center.biomes",
        "center.streets",
        "center.nexus",
        "center.beacon",
        "center.testing",
        "worldgencenterbiomes",
        "worldgenstreets",
        "worldgennexus",
        "worldgentesting",
    ),
}
DUMP_ROOT = t35.ROOT / "gt6_dump" / "gt6_recipe_dump"
WORLDGEN_OTHER = DUMP_ROOT / "worldgen" / "other_features.json"
CAPABILITY_MAP = (
    t35.TOOLS / "waves" / "portfolio" / "source-capability-map" / "capability_map.json"
)
GROWTH_ORDER = (
    t35.TOOLS
    / "waves"
    / "portfolio"
    / "source-capability-growth-order"
    / "growth_order.json"
)
T20_POLICY = t35.TOOLS / "t20_worldgen_source_policy.json"
ORE_VEINS = (
    t35.ROOT / "src/main/resources/data/cruciblecraft/worldgen_catalog/ore_veins.json"
)
FLUID_DEPOSITS = (
    t35.ROOT
    / "src/main/resources/data/cruciblecraft/worldgen_catalog/fluid_deposits.json"
)
SURFACE_SCATTER = (
    t35.ROOT
    / "src/main/resources/data/cruciblecraft/worldgen_catalog/surface_scatter.json"
)
WORLDGEN_CATALOG = (
    t35.ROOT / "src/main/resources/data/cruciblecraft/worldgen_catalog"
)
CONFIGURED_FEATURE = (
    t35.ROOT
    / "src/main/resources/data/cruciblecraft/worldgen/configured_feature"
)
PLACED_FEATURE = (
    t35.ROOT / "src/main/resources/data/cruciblecraft/worldgen/placed_feature"
)
WORLDGEN_JAVA = t35.ROOT / "src/main/java/com/masson/cruciblecraft/worldgen"
MATERIALS = (
    t35.ROOT / "src/main/resources/data/cruciblecraft/materials"
)
T33_READINESS = t35.TOOLS / "t33_readiness.json"
T45_READINESS = t35.TOOLS / "t45_readiness.json"
T48_READINESS = t35.TOOLS / "t48_readiness.json"
LEFTOVER_LATER = (
    t35.TOOLS / "waves" / "portfolio" / "source-capability-map-r0" / "leftover_later.json"
)
RECIPE_GENERATED = t35.ROOT / "src" / "recipe_generated"
WORLDGEN_QUESTIONS = {
    "center_semantics": (
        "How center.biomes, center.streets, center.nexus, center.beacon, and "
        "center.testing would be carried. Dump enabled=false is not a skip."
    ),
    "disposition_policy": (
        "Per-category later cards must choose implemented, planned, deferred, "
        "or out_of_scope. R0 does not fill those dispositions."
    ),
    "dungeon_semantics": (
        "Large overworld dungeon as a structure, not scatter or an ore vein."
    ),
    "fail_closed": (
        "Unknown feature must not silent no-op."
    ),
    "feature_vs_structure": (
        "Which of placed feature, structure, or dimension carries each category."
    ),
    "load": (
        "A later implementation card measures chunk generation, structure, and "
        "dimension separately; it cannot share an ore-vein or scatter owner."
    ),
    "planet_semantics": (
        "moon.rocks / mars.rocks / planet.rocks versus DESIGN_POLICY_OVERWORLD_ACCESS. "
        "R0 must not write them into the current overworld catalog."
    ),
    "player_acquisition": (
        "How survival encounters a rubber tree, a large dungeon, or planet rocks."
    ),
    "tree_identities": (
        "Nine WorldgenTree* features versus wood / sapling / leaf identity. "
        "They are not nine wood recipes."
    ),
}


def generated_by(_slug: str = SLUG) -> str:
    return GENERATED_BY


def nuclear_started() -> bool:
    return one_x.nuclear_started()


def leftover_later_count() -> int:
    leftover = t35.load_json(LEFTOVER_LATER)
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
    readiness = t35.load_json(wave_dir(PREDECESSOR) / "readiness.json")
    if readiness.get("status") != PREDECESSOR_STATUS:
        errors.append(
            f"{PREDECESSOR} status {readiness.get('status')} != {PREDECESSOR_STATUS}"
        )
    return errors


def load_other_features() -> list[dict[str, Any]]:
    if not WORLDGEN_OTHER.is_file():
        raise ValueError("dump worldgen/other_features.json missing")
    document = t35.load_json(WORLDGEN_OTHER)
    if not isinstance(document, list):
        raise ValueError("other_features.json is not a list")
    return [dict(row) for row in document]


def feature_name(row: dict[str, Any]) -> str:
    return str(row.get("name") or "")


def feature_type(row: dict[str, Any]) -> str:
    return str(row.get("type") or "")


def is_tree(row: dict[str, Any]) -> bool:
    return feature_type(row).startswith("WorldgenTree")


def is_dungeon(row: dict[str, Any]) -> bool:
    return feature_type(row) == "WorldgenDungeonGT"


def is_planet(row: dict[str, Any]) -> bool:
    return feature_name(row) in PLANET_NAMES


def is_center(row: dict[str, Any]) -> bool:
    return feature_name(row).startswith("center.")


def category_of(row: dict[str, Any]) -> str | None:
    hits = [
        name
        for name, predicate in (
            ("Trees", is_tree),
            ("Dungeons", is_dungeon),
            ("Planets", is_planet),
            ("Center", is_center),
        )
        if predicate(row)
    ]
    if len(hits) > 1:
        raise ValueError(
            f"{feature_name(row)} matched multiple categories: {hits}"
        )
    return hits[0] if hits else None


def authority_hashes() -> dict[str, str]:
    return {
        "capability_map": t35.sha256_file(CAPABILITY_MAP),
        "growth_order": t35.sha256_file(GROWTH_ORDER),
        "logistics_cover_net_r0_seal": t35.sha256_file(
            wave_dir(LOGISTICS_SLUG) / "closeout_seal.json"
        ),
        "other_features": t35.sha256_file(WORLDGEN_OTHER),
        "t13c_exclusion_reclaim_r0_seal": t35.sha256_file(
            wave_dir(PREDECESSOR) / "closeout_seal.json"
        ),
        "t20_worldgen_source_policy": t35.sha256_file(T20_POLICY),
    }


def capability_rows() -> list[dict[str, Any]]:
    document = t35.load_json(CAPABILITY_MAP)
    wanted = set(CAPABILITY_ANCHORS.values())
    rows = [
        dict(row)
        for row in document.get("rows") or []
        if str(row.get("gt6_anchor") or "") in wanted
    ]
    if len(rows) != 4:
        raise ValueError(f"capability map non-ore worldgen rows {len(rows)} != 4")
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
    document = t35.load_json(CAPABILITY_MAP)
    for row in document.get("rows") or []:
        if row.get("gt6_anchor") == SEED_ANCHOR:
            return dict(row)
    raise ValueError("missing capability map seed row for non-ore worldgen")


def ore_vein_ids() -> list[str]:
    document = t35.load_json(ORE_VEINS)
    ids = [str(row["id"]) for row in document.get("veins") or []]
    if len(ids) != EXPECTED_ORE_VEINS:
        raise ValueError(f"ore_veins count {len(ids)} != 129")
    policy = t35.load_json(T20_POLICY)
    if int(policy["target"]["catalog_entries"]) != EXPECTED_ORE_VEINS:
        raise ValueError("T20 policy catalog_entries drifted from 129")
    return ids


def fluid_deposit_ids() -> list[str]:
    document = t35.load_json(FLUID_DEPOSITS)
    ids = [str(row["id"]) for row in document.get("deposits") or []]
    if len(ids) != EXPECTED_FLUID_DEPOSITS:
        raise ValueError(f"fluid_deposits count {len(ids)} != 2")
    return ids


def surface_scatter_id() -> str:
    document = t35.load_json(SURFACE_SCATTER)
    scatter_id = str(document.get("id") or "")
    if scatter_id != "surface_rock_scatter":
        raise ValueError(f"surface_scatter id {scatter_id} != surface_rock_scatter")
    return scatter_id


def authored_feature_stems(directory: Any) -> list[str]:
    stems = sorted(path.stem for path in directory.glob("*.json"))
    if len(stems) != EXPECTED_AUTHORED_FEATURES:
        raise ValueError(
            f"{t35.relative(directory)} count {len(stems)} != 9"
        )
    blocked = [
        stem
        for stem in stems
        if any(
            token in stem.lower()
            for token in ("tree", "dungeon", "moon", "mars", "planet", "center")
        )
    ]
    if blocked:
        raise ValueError(f"authored worldgen features grew slice ids: {blocked}")
    return stems


def worldgen_java_stems() -> list[str]:
    stems = sorted(path.stem for path in WORLDGEN_JAVA.glob("*.java"))
    if not stems:
        raise ValueError("worldgen Java package is empty")
    blocked = [
        stem
        for stem in stems
        if any(
            token in stem.lower()
            for token in ("tree", "sapling", "dungeon", "planet", "moon", "mars", "center")
        )
    ]
    if blocked:
        raise ValueError(f"worldgen Java grew slice types: {blocked}")
    return stems


def dimension_policy() -> str:
    policy = t35.load_json(T20_POLICY)
    text = str(policy["classification"]["dimension_policy"])
    if "DESIGN_POLICY_OVERWORLD_ACCESS" not in text:
        raise ValueError("T20 dimension_policy missing DESIGN_POLICY_OVERWORLD_ACCESS")
    return text


def closed_scatter() -> dict[str, Any]:
    t33 = t35.load_json(T33_READINESS)
    t45 = t35.load_json(T45_READINESS)
    t48 = t35.load_json(T48_READINESS)
    if t33.get("status") != "T33_READY":
        raise ValueError(f"T33 status {t33.get('status')} != T33_READY")
    if t45.get("status") != "T45_READY":
        raise ValueError(f"T45 status {t45.get('status')} != T45_READY")
    if t48.get("status") != "T48_READY":
        raise ValueError(f"T48 status {t48.get('status')} != T48_READY")
    return {
        "fluid_deposits": {
            "catalog_entries": EXPECTED_FLUID_DEPOSITS,
            "ids": fluid_deposit_ids(),
            "owner_here": False,
            "status": "selected_subset",
        },
        "item_scatter": {
            "owner_here": False,
            "t33": "T33_READY",
            "t45": "T45_READY",
            "t48": "T48_READY",
        },
        "ore_veins": {
            "catalog_entries": EXPECTED_ORE_VEINS,
            "owner_here": False,
            "status": "T20 selected_subset",
        },
        "surface_scatter": {
            "id": surface_scatter_id(),
            "owner_here": False,
            "status": "T33_READY",
        },
    }


def wood_material_ids() -> list[str]:
    if not MATERIALS.is_dir():
        raise ValueError("materials directory missing")
    hits = [
        path.stem
        for path in sorted(MATERIALS.glob("*.json"))
        if any(token in path.stem.lower() for token in TREE_MATERIAL_TOKENS)
    ]
    return hits


def runtime_corpus() -> str:
    parts = [
        *ore_vein_ids(),
        *fluid_deposit_ids(),
        surface_scatter_id(),
        *authored_feature_stems(CONFIGURED_FEATURE),
        *authored_feature_stems(PLACED_FEATURE),
        *worldgen_java_stems(),
    ]
    return " ".join(parts).lower()


def named_runtime_hits(category: str) -> list[str]:
    blob = runtime_corpus()
    return sorted(
        token for token in CATEGORY_TOKENS[category] if token in blob
    )


def already_closed_elsewhere(features: list[dict[str, Any]]) -> dict[str, Any]:
    closed = closed_scatter()
    springs = sum(1 for row in features if feature_type(row) == "WorldgenFluidSpring")
    if springs != EXPECTED_DUMP_SPRINGS:
        raise ValueError(f"dump WorldgenFluidSpring count {springs} != 16")
    rocks = sum(1 for row in features if feature_type(row) == "WorldgenRocks")
    closed["fluid_deposits"]["dump_fluid_springs"] = springs
    closed["surface_scatter"]["dump_worldgen_rocks"] = rocks
    return closed


def inherited_denominator_document() -> dict[str, Any]:
    features = load_other_features()
    if len(features) != FULL_OTHER_FEATURES:
        raise ValueError(f"other_features.json count {len(features)} != 190")
    live_by_name = {feature_name(row): row for row in features}
    if len(live_by_name) != len(features):
        raise ValueError("other_features.json names are not unique")
    sliced: list[dict[str, Any]] = []
    remainder: list[dict[str, Any]] = []
    by_category: dict[str, list[dict[str, Any]]] = {name: [] for name in PINNED_NAMES}
    for row in features:
        category = category_of(row)
        copied = dict(row)
        if category is None:
            remainder.append(copied)
            continue
        sliced.append(copied)
        by_category[category].append(copied)
    if len(sliced) != EXPECTED_SLICE:
        raise ValueError(f"pinned dump features {len(sliced)} != 18")
    if len(remainder) != REMAINDER_AFTER_SLICE:
        raise ValueError(f"remainder after slice {len(remainder)} != 172")
    if len(sliced) + len(remainder) != FULL_OTHER_FEATURES:
        raise ValueError("slice and remainder do not cover 190 dump features")
    streets = [
        feature_name(row)
        for row in by_category["Trees"]
        if feature_type(row) == "WorldgenStreets" or "street" in feature_name(row)
    ]
    if streets:
        raise ValueError(f"WorldgenStreets leaked into Trees: {streets}")
    extra_planets = [
        feature_name(row)
        for row in by_category["Planets"]
        if feature_name(row) in EXTRA_DIMENSION_ROCKS
    ]
    if extra_planets:
        raise ValueError(f"extra-dimension rocks leaked into Planets: {extra_planets}")
    leftover_extra = [
        name for name in EXTRA_DIMENSION_ROCKS if name not in live_by_name
    ]
    if leftover_extra:
        raise ValueError(f"extra-dimension rocks missing from dump: {leftover_extra}")
    for name in EXTRA_DIMENSION_ROCKS:
        if category_of(live_by_name[name]) is not None:
            raise ValueError(f"{name} must stay in remainder")
    category_rows: list[dict[str, Any]] = []
    for name, expected_count, expected_names in PINNED_CATEGORIES:
        rows = by_category[name]
        names = tuple(feature_name(row) for row in rows)
        if len(rows) != expected_count:
            raise ValueError(f"{name} feature count {len(rows)} != {expected_count}")
        if names != expected_names:
            raise ValueError(f"{name} names {names} != {expected_names}")
        for row in rows:
            live = live_by_name[feature_name(row)]
            drift = t35.first_json_diff(live, row)
            if drift:
                raise ValueError(
                    f"{feature_name(row)} is not byte-identical to dump: {drift}"
                )
        category_rows.append(
            {
                "category": name,
                "feature_count": expected_count,
                "features": rows,
                "names": list(expected_names),
                "types": [feature_type(row) for row in rows],
            }
        )
    histogram = dict(sorted(Counter(feature_type(row) for row in remainder).items()))
    return {
        "already_closed_elsewhere": already_closed_elsewhere(features),
        "authority_hashes": authority_hashes(),
        "categories": category_rows,
        "dump": {
            "feature_count": FULL_OTHER_FEATURES,
            "path": t35.relative(WORLDGEN_OTHER),
            "sha256": t35.sha256_file(WORLDGEN_OTHER),
        },
        "feature_count": EXPECTED_SLICE,
        "features": sliced,
        "full_other_features": FULL_OTHER_FEATURES,
        "generated_by": GENERATED_BY,
        "remainder_after_slice": {
            "feature_count": REMAINDER_AFTER_SLICE,
            "type_histogram": histogram,
        },
        "schema_version": 1,
        "source_artifact": t35.relative(WORLDGEN_OTHER),
        "source_revision": SOURCE_REVISION,
        "status": "INHERITED_DENOMINATOR_READY",
        "wave_slug": SLUG,
    }


def source_semantics_document(inherited: dict[str, Any]) -> dict[str, Any]:
    by_category = {row["category"]: row for row in inherited["categories"]}
    woods = wood_material_ids()
    policy = dimension_policy()
    rows: list[dict[str, Any]] = []
    for name, expected_count, expected_names in PINNED_CATEGORIES:
        category = by_category[name]
        features = category["features"]
        if name == "Trees":
            contrast = {
                "nine_tree_features_are_not_wood_recipes": True,
                "placement_not_identity": True,
                "streets_are_center_not_trees": True,
                "wood_material_ids": woods,
            }
        elif name == "Dungeons":
            contrast = {
                "is_structure": True,
                "not_ore_vein": True,
                "not_scatter": True,
                "type": feature_type(features[0]),
            }
        elif name == "Planets":
            contrast = {
                "extra_dimension_rocks_not_in_slice": sorted(EXTRA_DIMENSION_ROCKS),
                "in_overworld_catalog": False,
                "overworld_access_policy": policy,
            }
        else:
            contrast = {
                "dump_enabled": [bool(row.get("enabled")) for row in features],
                "enabled_false_is_not_skip": True,
                "open_questions": [
                    "biomes",
                    "streets",
                    "nexus",
                    "beacon",
                    "testing",
                ],
                "types": [feature_type(row) for row in features],
            }
        rows.append(
            {
                "category": name,
                "contrast": contrast,
                "evidence_class": "SOURCE_BACKED",
                "feature_count": expected_count,
                "names": list(expected_names),
                "types": [feature_type(row) for row in features],
            }
        )
    return {
        "categories": rows,
        "generated_by": GENERATED_BY,
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "status": "SOURCE_SEMANTICS_READY",
        "wave_slug": SLUG,
    }


def existing_mechanism_document() -> dict[str, Any]:
    veins = ore_vein_ids()
    deposits = fluid_deposit_ids()
    scatter = surface_scatter_id()
    configured = authored_feature_stems(CONFIGURED_FEATURE)
    placed = authored_feature_stems(PLACED_FEATURE)
    java_stems = worldgen_java_stems()
    capability = capability_rows()
    seed = capability_seed_row()
    already_closed = closed_scatter()
    analogs = {
        "Trees": (
            "Authored placed-feature / scatter JSON is not custom tree growth; "
            "wood materials are not WorldgenTree*"
        ),
        "Dungeons": (
            "T20 ore/fluid/scatter catalogs are not a structure pipeline"
        ),
        "Planets": (
            "DESIGN_POLICY_OVERWORLD_ACCESS is not moon/mars/planet rocks"
        ),
        "Center": (
            "Existing overworld feature JSON is not center biomes, streets, "
            "nexus, beacon, or testing"
        ),
    }
    mapped: list[dict[str, Any]] = []
    for name, _count, _names in PINNED_CATEGORIES:
        hits = named_runtime_hits(name)
        row = capability_row_for(name)
        mechanism = str(row["cc_mechanism"])
        if hits:
            mechanism = hits[0]
        mapped.append(
            {
                "adjacent_analog": analogs[name],
                "category": name,
                "cc_mechanism": mechanism,
                "gap": "mechanism" if mechanism == "none" else "none",
                "named_runtime_hits": hits,
            }
        )
    return {
        "already_closed_elsewhere": already_closed,
        "authored_configured_features": configured,
        "authored_placed_features": placed,
        "capability_map_non_ore": capability,
        "capability_map_seed": seed,
        "dimension_policy": dimension_policy(),
        "fluid_deposit_count": len(deposits),
        "fluid_deposit_ids": deposits,
        "fluid_deposits_sha256": t35.sha256_file(FLUID_DEPOSITS),
        "generated_by": GENERATED_BY,
        "kinds": mapped,
        "note": (
            "The gap is a missing mechanism, not a missing catalog row. "
            "The selected 129 ore veins, 2 fluid deposits, surface rocks, and "
            "closed T33/T45/T48 item scatter do not cover Trees, Dungeons, "
            "Planets, or Center."
        ),
        "nuclear_started": nuclear_started(),
        "ore_vein_count": len(veins),
        "ore_veins_sha256": t35.sha256_file(ORE_VEINS),
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "status": "EXISTING_MECHANISM_READY",
        "surface_scatter_id": scatter,
        "surface_scatter_sha256": t35.sha256_file(SURFACE_SCATTER),
        "t20_policy_sha256": t35.sha256_file(T20_POLICY),
        "wave_slug": SLUG,
        "worldgen_java": java_stems,
    }


def worldgen_contract_document() -> dict[str, Any]:
    return {
        "generated_by": GENERATED_BY,
        "implemented": False,
        "questions": dict(WORLDGEN_QUESTIONS),
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "status": "WORLDGEN_CONTRACT_READY",
        "wave_slug": SLUG,
    }


def category_reasons(name: str) -> list[str]:
    if name == "Trees":
        return [
            "Trees capability-map cc_mechanism is none.",
            "No named ore-vein, fluid-deposit, scatter, or tree-growth runtime "
            "covers WorldgenTree*.",
            "Wood and sapling material identity is not tree placement. The gap "
            "is custom growth, not another scatter JSON row.",
        ]
    if name == "Dungeons":
        return [
            "Dungeons capability-map cc_mechanism is none.",
            "WorldgenDungeonGT is a structure. T20 catalogs and scatter features "
            "are not a structure pipeline.",
        ]
    if name == "Planets":
        return [
            "Planets capability-map cc_mechanism is none.",
            "DESIGN_POLICY_OVERWORLD_ACCESS records GT6 dimensions as source "
            "facts without overworld catalog parity.",
            "moon.rocks / mars.rocks / planet.rocks are not in the current "
            "overworld catalog.",
        ]
    return [
        "Center capability-map cc_mechanism is none.",
        "center.biomes / streets / nexus / beacon / testing have no named CC "
        "biome, structure, or feature runtime.",
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
    for name, expected_count, _names in PINNED_CATEGORIES:
        missing: list[str] = []
        summary = next(row for row in inherited["categories"] if row["category"] == name)
        if int(summary["feature_count"]) != expected_count:
            missing.append("dump")
        semantic = semantics_by_category.get(name)
        if semantic is None or int(semantic["feature_count"]) != expected_count:
            missing.append("dump")
        if set(contract["questions"]) != set(WORLDGEN_QUESTIONS):
            missing.append("schema")
        load_text = str(contract["questions"].get("load") or "")
        if "separately" not in load_text:
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
    token = "non-ore-worldgen"
    for path in RECIPE_GENERATED.rglob("*"):
        if token in path.as_posix():
            return True
    return False


def worldgen_catalog_mentions_slice() -> list[str]:
    tokens = (
        "tree.rubber",
        "WorldgenTree",
        "WorldgenDungeonGT",
        "moon.rocks",
        "mars.rocks",
        "planet.rocks",
        "center.biomes",
        "center.streets",
    )
    hits: list[str] = []
    for path in WORLDGEN_CATALOG.glob("*.json"):
        text = path.read_text(encoding="utf-8")
        for token in tokens:
            if token in text:
                hits.append(f"{path.name}:{token}")
    return hits


def readiness_note(feasibility: dict[str, Any]) -> str:
    parts = [
        f"{row['category']} {row['verdict']}" for row in feasibility["categories"]
    ]
    return (
        f"{STATUS}. Four non-ore worldgen categories inherited byte-identical "
        "18/190. "
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
        "feasibility_by_category": {
            row["category"]: row["verdict"] for row in feasibility["categories"]
        },
        "full_other_features": inherited["full_other_features"],
        "generated_recipe_count": 0,
        "inherited_feature_count": inherited["feature_count"],
        "leftover_later_count": leftover,
        "nuclear_track_c_started": nuclear_started(),
        "owns_families": 0,
        "partial_family_count": 0,
        "production_lock": None,
        "recipe_files_generated": False,
        "remainder_after_slice": inherited["remainder_after_slice"]["feature_count"],
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
        "cohort": "non-ore-worldgen-r0",
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
    contract = worldgen_contract_document()
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
        raise ValueError("src/recipe_generated mentions non-ore-worldgen")
    catalog_hits = worldgen_catalog_mentions_slice()
    if catalog_hits:
        raise ValueError(f"worldgen_catalog grew slice ids: {catalog_hits}")
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
    documents["worldgen_contract.json"] = contract
    documents["feasibility.json"] = feasibility
    return documents


def write_seal() -> dict[str, Any]:
    root = wave_dir(SLUG)
    hashes = {
        "census": t35.sha256_file(root / "census_delta.json"),
        "gametest_java": None,
        "gametest_log": None,
        "generated_recipes": None,
        "locked_support": None,
        "production_lock": None,
        "publication_group_manifest": None,
        "readiness": t35.sha256_file(root / "readiness.json"),
        "receipt": None,
        "runtime_dependency_manifest": None,
        "shard_manifest": None,
        "topology": t35.sha256_file(root / "topology.json"),
    }
    seal = {
        "card_id": SLUG,
        "complete_family_count": 0,
        "composed_identity_ledger_v2_sha256": t35.sha256_file(
            closeout_seal.IDENTITY_LEDGER_V2
        ),
        "composed_runtime_manifest_v2_sha256": t35.sha256_file(
            closeout_seal.RUNTIME_MANIFEST_V2
        ),
        "gametest_status": "NONE",
        "generated_by": f"{GENERATED_BY} --write",
        "hashes": hashes,
        "note": t35.load_json(root / "readiness.json").get("note"),
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
    t35.write_stable(root / "closeout_seal.json", seal)
    return seal


def write_artifacts() -> dict[str, Any]:
    documents = build_r0_documents()
    root = wave_dir(SLUG)
    root.mkdir(parents=True, exist_ok=True)
    for name, document in documents.items():
        t35.write_stable(root / name, document)
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
        committed = t35.load_json(root / name)
        drift = t35.first_json_diff(document, committed)
        if drift:
            errors.append(f"{name} drifted: {drift}")
    readiness = t35.load_json(root / "readiness.json")
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
    if int(evidence.get("inherited_feature_count", 0)) != EXPECTED_SLICE:
        errors.append("inherited_feature_count must be 18")
    if int(evidence.get("full_other_features", 0)) != FULL_OTHER_FEATURES:
        errors.append("full_other_features must be 190")
    if int(evidence.get("remainder_after_slice", 0)) != REMAINDER_AFTER_SLICE:
        errors.append("remainder_after_slice must be 172")
    topology = t35.load_json(root / "topology.json")
    extra = sorted(set(topology) - set(ALLOWED_TOPOLOGY_KEYS))
    if extra:
        errors.append(f"topology has forbidden keys: {extra}")
    errors.extend(check_forbidden_successors(json.dumps(topology, sort_keys=True)))
    feasibility = t35.load_json(root / "feasibility.json")
    by_category = {row["category"]: row for row in feasibility.get("categories") or []}
    if tuple(by_category) != PINNED_NAMES:
        errors.append("feasibility categories drifted from the pinned four")
    for name, _count, _names in PINNED_CATEGORIES:
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
    inherited = t35.load_json(root / "inherited_denominator.json")
    if int(inherited.get("feature_count", 0)) != EXPECTED_SLICE:
        errors.append("inherited feature_count must be 18")
    if int(inherited.get("full_other_features", 0)) != FULL_OTHER_FEATURES:
        errors.append("inherited full_other_features must be 190")
    live_features = load_other_features()
    live_by_name = {feature_name(row): row for row in live_features}
    for row in inherited.get("features") or []:
        live_row = live_by_name.get(feature_name(row))
        if live_row is None:
            errors.append(f"{feature_name(row)} missing from live dump")
            continue
        drift = t35.first_json_diff(live_row, row)
        if drift:
            errors.append(
                f"{feature_name(row)} is not byte-identical to dump: {drift}"
            )
    tree_rows = [
        row
        for row in inherited.get("features") or []
        if category_of(row) == "Trees"
    ]
    if any(feature_type(row) == "WorldgenStreets" for row in tree_rows):
        errors.append("WorldgenStreets must not be classified as Trees")
    hashes = inherited.get("authority_hashes") or {}
    live_hashes = authority_hashes()
    for key, digest in live_hashes.items():
        if hashes.get(key) != digest:
            errors.append(f"authority hash {key} drifted")
    if recipe_generated_mentions_slug():
        errors.append("src/recipe_generated mentions non-ore-worldgen")
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
