#!/usr/bin/env python3
"""portfolio/vanilla-replace-r0: freeze two vanilla replace loader categories."""
from __future__ import annotations

import argparse
import json
import sys
from typing import Any

from tools import closeout_seal
from tools import portfolio_one_x as one_x
from tools import census_common as census
from tools.recipe_bulk.slugs import KNOWN_SEMANTIC_SLUGS
from tools.wave_closeout import known_slugs
from tools.wave_closeout import spec_for
from tools.wave_closeout import wave_dir

SLUG = "portfolio/vanilla-replace-r0"
PREDECESSOR = "portfolio/non-ore-worldgen-r0"
PREDECESSOR_STATUS = "NON_ORE_WORLDGEN_R0_READY"
T13C_SLUG = "portfolio/exclusion-reclaim-r0"
LOGISTICS_SLUG = "portfolio/logistics-cover-net-r0"
GENERIC_IMPORT_SLUG = "portfolio/generic-recipe-generator"
GENERIC_IMPORT_STATUS = "GENERIC_RECIPE_IMPORT_READY"
STATUS = "VANILLA_REPLACE_R0_READY"
SOURCE_REVISION = census.SOURCE_REVISION
GENERATED_BY = "python tools/build_vanilla_replace_r0.py"
PINNED_NAMES = ("Vanilla", "Replace")
SEED_ANCHOR = "GT6 Vanilla replace loaders"
CAPABILITY_ANCHORS = {
    "Vanilla": "gregtech.loaders.c.Loader_Recipes_Vanilla",
    "Replace": (
        "gregtech.loaders.c.Loader_Recipes_Replace + "
        "gregtech.asm.transformers.minecraft.Replacements"
    ),
}
PINNED_FILES = {
    "Vanilla": (
        "src/main/java/gregtech/loaders/c/Loader_Recipes_Vanilla.java",
    ),
    "Replace": (
        "src/main/java/gregtech/loaders/c/Loader_Recipes_Replace.java",
        "src/main/java/gregtech/asm/transformers/minecraft/Replacements.java",
    ),
}
EXPECTED_BLOBS = {
    "src/main/java/gregtech/loaders/c/Loader_Recipes_Vanilla.java": (
        "4c459acd2c7729d4186c5ada9ccd76181745bacf",
        92644,
    ),
    "src/main/java/gregtech/loaders/c/Loader_Recipes_Replace.java": (
        "d75124ccf34297b4a59f87553564a02d887c0bc0",
        25382,
    ),
    "src/main/java/gregtech/asm/transformers/minecraft/Replacements.java": (
        "584a2030723c11e984b5b7437ed17f7a9bc93e5d",
        5592,
    ),
}
EXPECTED_NESTED = {
    "src/main/java/gregtech/loaders/c/Loader_Recipes_Vanilla.java": (
        "Loader_Recipes_Vanilla",
    ),
    "src/main/java/gregtech/loaders/c/Loader_Recipes_Replace.java": (
        "Loader_Recipes_Replace",
        "RecipeReplacement",
        "RecipeReplacer",
    ),
    "src/main/java/gregtech/asm/transformers/minecraft/Replacements.java": (
        "Replacements",
    ),
}
EMPTY_DUMP_MAPS = (
    "mc.recipe.furnace",
    "mc.recipe.furnacefuel",
    "gt.recipe.autocrafting",
)
EXPECTED_HOPPER_VANILLA = 121
EXPECTED_WORLDGEN_ORES_VANILLA = 24
EXPECTED_FULL_OTHER_FEATURES = 190
EXPECTED_REMAINDER_AFTER_NON_ORE = 172
EXPECTED_SOURCE_FILE_COUNT = 3
FEASIBILITY_VALUES = (
    "bounded_extension",
    "requires_new_runtime",
    "defer_to_portfolio",
    "blocked",
)
FORBIDDEN_SUCCESSORS = (
    "vanilla-implementation",
    "replace-implementation",
    "crops-implementation",
    "food-implementation",
    "bees-implementation",
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
    "Vanilla": (
        "loader_recipes_vanilla",
        "data/minecraft/recipe",
    ),
    "Replace": (
        "loader_recipes_replace",
        "asm.transformers.minecraft.replacements",
        "gregtech.asm.transformers.minecraft",
    ),
}
REPLACE_QUESTIONS = {
    "add_vs_remove": (
        "Vanilla loader adds GT6 vanilla-adjacent recipes. Replace removes or "
        "substitutes vanilla recipes. A later card must keep those axes distinct."
    ),
    "asm_vs_datapack": (
        "ASM Replacements is bytecode, not a recipe JSON. NeoForge recipe "
        "conditions are not a GT6 transformer."
    ),
    "disposition_policy": (
        "Per-category later cards must choose implemented, planned, deferred, "
        "or out_of_scope. R0 does not fill those dispositions."
    ),
    "fail_closed": (
        "Unknown vanilla recipe must not silent no-op."
    ),
    "load": (
        "A later implementation card measures recipe addition, deletion, and "
        "substitution separately; it cannot share a hopper or mold-firing owner."
    ),
    "minecraft_namespace": (
        "Whether a later card may write data/minecraft recipe overrides. R0 "
        "must not create that tree."
    ),
    "player_acquisition": (
        "Whether survival still has vanilla crafting-table and furnace recipes."
    ),
}
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
TREE_MANIFEST = census.TOOLS / "machine_tree_gt6_tree_manifest.json"
SYMBOL_INVENTORY = census.TOOLS / "machine_tree_source_symbol_inventory.json"
RECIPE_MAPS = census.TOOLS / "machine_tree_denominators" / "recipe_maps.json"
T30_READINESS = census.TOOLS / "hopper_readiness.json"
LEFTOVER_LATER = (
    census.TOOLS / "waves" / "portfolio" / "source-capability-map-r0" / "leftover_later.json"
)
AUTHORED_RECIPES = (
    census.ROOT / "src" / "main" / "resources" / "data" / "cruciblecraft" / "recipe"
)
HOPPER_RECIPES = (
    census.ROOT
    / "src"
    / "generated"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "recipe"
    / "hoppers"
)
MIXINS_JSON = census.ROOT / "src" / "main" / "resources" / "cruciblecraft.mixins.json"
SOURCE_IMPORT = census.TOOLS / "recipe_bulk" / "source_import.py"
RECIPE_GENERATED = census.ROOT / "src" / "recipe_generated"
RESOURCE_ROOTS = (
    census.ROOT / "src" / "main" / "resources",
    census.ROOT / "src" / "generated" / "resources",
)


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
    return errors


def authority_hashes() -> dict[str, str]:
    return {
        "capability_map": census.sha256_file(CAPABILITY_MAP),
        "growth_order": census.sha256_file(GROWTH_ORDER),
        "logistics_cover_net_r0_seal": census.sha256_file(
            wave_dir(LOGISTICS_SLUG) / "closeout_seal.json"
        ),
        "non_ore_worldgen_r0_seal": census.sha256_file(
            wave_dir(PREDECESSOR) / "closeout_seal.json"
        ),
        "machine_tree_gt6_tree_manifest": census.sha256_file(TREE_MANIFEST),
        "machine_tree_source_symbol_inventory": census.sha256_file(SYMBOL_INVENTORY),
        "t13c_exclusion_reclaim_r0_seal": census.sha256_file(
            wave_dir(T13C_SLUG) / "closeout_seal.json"
        ),
    }


def tree_entries() -> dict[str, dict[str, Any]]:
    manifest = census.load_json(TREE_MANIFEST)
    if manifest.get("revision") != SOURCE_REVISION:
        raise ValueError(
            f"T13 tree revision {manifest.get('revision')} != {SOURCE_REVISION}"
        )
    by_path = {str(row["path"]): dict(row) for row in manifest.get("entries") or []}
    return by_path


def symbol_classes_for(path: str) -> list[str]:
    inventory = census.load_json(SYMBOL_INVENTORY)
    if inventory.get("source_revision") != SOURCE_REVISION:
        raise ValueError(
            f"symbol inventory revision {inventory.get('source_revision')} "
            f"!= {SOURCE_REVISION}"
        )
    names = [
        str(row["class"])
        for row in inventory.get("declarations") or []
        if row.get("kind") == "class" and str(row.get("path") or "") == path
    ]
    return names


def source_file_record(path: str) -> dict[str, Any]:
    expected_sha, expected_size = EXPECTED_BLOBS[path]
    live = tree_entries().get(path)
    if live is None:
        raise ValueError(f"{path} missing from T13 tree manifest")
    sha = str(live.get("sha") or "")
    size = int(live.get("size") or -1)
    if sha != expected_sha or size != expected_size:
        raise ValueError(
            f"{path} blob {sha}/{size} != pinned {expected_sha}/{expected_size}"
        )
    classes = tuple(symbol_classes_for(path))
    expected_classes = EXPECTED_NESTED[path]
    if classes != expected_classes:
        raise ValueError(f"{path} nested classes {classes} != {expected_classes}")
    inventory = census.load_json(SYMBOL_INVENTORY)
    blobs = {
        str(row.get("blob_sha1") or "")
        for row in inventory.get("declarations") or []
        if row.get("kind") == "class" and str(row.get("path") or "") == path
    }
    if blobs != {expected_sha}:
        raise ValueError(f"{path} symbol blob {blobs} != {{{expected_sha}}}")
    return {
        "git_blob_sha1": sha,
        "nested_classes": list(expected_classes),
        "path": path,
        "size": size,
    }


def capability_rows() -> list[dict[str, Any]]:
    document = census.load_json(CAPABILITY_MAP)
    wanted = set(CAPABILITY_ANCHORS.values())
    rows = [
        dict(row)
        for row in document.get("rows") or []
        if str(row.get("gt6_anchor") or "") in wanted
    ]
    if len(rows) != 2:
        raise ValueError(f"capability map vanilla-replace rows {len(rows)} != 2")
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
    raise ValueError("missing capability map seed row for vanilla replace")


def empty_dump_maps() -> dict[str, int]:
    document = census.load_json(RECIPE_MAPS)
    by_name = {
        str(row.get("name_internal") or ""): row.get("recipe_count")
        for row in document.get("rows") or []
    }
    counts: dict[str, int] = {}
    for name in EMPTY_DUMP_MAPS:
        if name not in by_name:
            raise ValueError(f"recipe_maps.json missing {name}")
        count = by_name[name]
        if count is None or int(count) != 0:
            raise ValueError(f"{name} recipe_count {count} != 0")
        counts[name] = 0
    return counts


def hopper_vanilla_count() -> int:
    hopper = census.load_json(T30_READINESS)
    if hopper.get("status") != "T30_READY":
        raise ValueError(f"T30 status {hopper.get('status')} != T30_READY")
    count = int(hopper.get("vanilla_recipe_count") or -1)
    if count != EXPECTED_HOPPER_VANILLA:
        raise ValueError(f"T30 vanilla_recipe_count {count} != 121")
    if not hopper.get("closure", {}).get("vanilla_recipes_121"):
        raise ValueError("T30 vanilla_recipes_121 is not true")
    files = sorted(HOPPER_RECIPES.glob("*.json"))
    if len(files) != EXPECTED_HOPPER_VANILLA:
        raise ValueError(f"hopper recipe files {len(files)} != 121")
    return count


def worldgen_ores_vanilla() -> dict[str, Any]:
    inherited = census.load_json(
        wave_dir(PREDECESSOR) / "inherited_denominator.json"
    )
    remainder = inherited.get("remainder_after_slice") or {}
    histogram = remainder.get("type_histogram") or {}
    ores = int(histogram.get("WorldgenOresVanilla") or -1)
    if ores != EXPECTED_WORLDGEN_ORES_VANILLA:
        raise ValueError(f"WorldgenOresVanilla {ores} != 24")
    full = int(inherited.get("full_other_features") or -1)
    leftover = int(remainder.get("feature_count") or -1)
    if full != EXPECTED_FULL_OTHER_FEATURES:
        raise ValueError(f"full_other_features {full} != 190")
    if leftover != EXPECTED_REMAINDER_AFTER_NON_ORE:
        raise ValueError(f"remainder_after_slice {leftover} != 172")
    return {
        "feature_count": ores,
        "full_other_features": full,
        "owner_here": False,
        "remainder_after_non_ore_slice": leftover,
        "status": "non-ore-worldgen remainder",
    }


def authored_smelting_recipes() -> list[str]:
    if not AUTHORED_RECIPES.is_dir():
        raise ValueError("authored cruciblecraft recipe tree is missing")
    hits: list[str] = []
    for path in sorted(AUTHORED_RECIPES.rglob("*.json")):
        document = census.load_json(path)
        if document.get("type") != "minecraft:smelting":
            continue
        relative = census.relative(path)
        if "data/minecraft/" in relative.replace("\\", "/"):
            raise ValueError(f"authored smelting leaked into minecraft: {relative}")
        hits.append(relative)
    if not hits:
        raise ValueError("no authored minecraft:smelting recipes found")
    return hits


def minecraft_recipe_files() -> list[str]:
    hits: list[str] = []
    for root in RESOURCE_ROOTS:
        recipe = root / "data" / "minecraft" / "recipe"
        if not recipe.exists():
            continue
        hits.extend(census.relative(path) for path in sorted(recipe.rglob("*")) if path.is_file())
    return hits


def mixin_classes() -> list[str]:
    document = census.load_json(MIXINS_JSON)
    mixins = [str(name) for name in document.get("mixins") or []]
    if mixins != ["ItemStackMixin"]:
        raise ValueError(f"mixins {mixins} != ['ItemStackMixin']")
    return mixins


def generic_importer_status() -> str:
    readiness = census.load_json(wave_dir(GENERIC_IMPORT_SLUG) / "readiness.json")
    status = str(readiness.get("status") or "")
    if status != GENERIC_IMPORT_STATUS:
        raise ValueError(f"generic importer status {status} != {GENERIC_IMPORT_STATUS}")
    return status


def importer_removes_minecraft() -> bool:
    text = SOURCE_IMPORT.read_text(encoding="utf-8").lower()
    return "data/minecraft/recipe" in text or "remove_recipe" in text


def runtime_corpus() -> str:
    # R0-era runtime only. Live data/minecraft/recipe is owned by
    # content/vanilla-replace-mvp and must not drift this snapshot.
    parts = [
        *mixin_classes(),
        census.relative(MIXINS_JSON),
    ]
    return " ".join(parts).lower()


def named_runtime_hits(category: str) -> list[str]:
    blob = runtime_corpus()
    return sorted(token for token in CATEGORY_TOKENS[category] if token in blob)


def already_closed_elsewhere() -> dict[str, Any]:
    hopper = hopper_vanilla_count()
    return {
        "empty_dump_maps": {
            "is_census": False,
            "recipe_counts": empty_dump_maps(),
        },
        "hopper_hopper_vanilla": {
            "owner_here": False,
            "recipe_count": hopper,
            "status": "T30_READY",
        },
        "worldgen_ores_vanilla": worldgen_ores_vanilla(),
    }


def inherited_denominator_document() -> dict[str, Any]:
    capability = capability_rows()
    seed = capability_seed_row()
    categories: list[dict[str, Any]] = []
    files: list[dict[str, Any]] = []
    for name in PINNED_NAMES:
        row = capability_row_for(name)
        drift = census.first_json_diff(row, next(
            live for live in capability if live["gt6_anchor"] == row["gt6_anchor"]
        ))
        if drift:
            raise ValueError(f"{name} capability row is not byte-identical: {drift}")
        records = [source_file_record(path) for path in PINNED_FILES[name]]
        files.extend(records)
        categories.append(
            {
                "category": name,
                "capability_row": row,
                "gt6_anchor": CAPABILITY_ANCHORS[name],
                "source_file_count": len(records),
                "source_files": records,
            }
        )
    if len(files) != EXPECTED_SOURCE_FILE_COUNT:
        raise ValueError(f"pinned source files {len(files)} != 3")
    replace_paths = [row["path"] for row in categories[1]["source_files"]]
    if len(replace_paths) != 2:
        raise ValueError("Replace must keep loader + ASM as one category")
    return {
        "already_closed_elsewhere": already_closed_elsewhere(),
        "authority_hashes": authority_hashes(),
        "capability_map_seed": seed,
        "categories": categories,
        "category_count": 2,
        "generated_by": GENERATED_BY,
        "schema_version": 1,
        "source_file_count": EXPECTED_SOURCE_FILE_COUNT,
        "source_revision": SOURCE_REVISION,
        "status": "INHERITED_DENOMINATOR_READY",
        "wave_slug": SLUG,
    }


def source_semantics_document(inherited: dict[str, Any]) -> dict[str, Any]:
    empty = inherited["already_closed_elsewhere"]["empty_dump_maps"]["recipe_counts"]
    rows: list[dict[str, Any]] = []
    for category in inherited["categories"]:
        name = category["category"]
        files = category["source_files"]
        if name == "Vanilla":
            contrast = {
                "adds_vanilla_adjacent_recipes": True,
                "empty_dump_maps_are_not_census": True,
                "furnace_dump_recipe_count": empty["mc.recipe.furnace"],
                "not_a_recipe_json_census": True,
            }
        else:
            replace_file = files[0]
            asm_file = files[1]
            contrast = {
                "asm_replacements_is_bytecode": True,
                "empty_dump_maps_are_not_census": True,
                "nested_helpers_same_file": [
                    name
                    for name in replace_file["nested_classes"]
                    if name != "Loader_Recipes_Replace"
                ],
                "not_recipe_json": True,
                "removes_or_substitutes": True,
                "replacements_class": asm_file["nested_classes"],
            }
        rows.append(
            {
                "category": name,
                "contrast": contrast,
                "evidence_class": "SOURCE_BACKED",
                "gt6_anchor": category["gt6_anchor"],
                "source_file_count": category["source_file_count"],
                "source_files": [row["path"] for row in files],
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
    smelting = authored_smelting_recipes()
    mixins = mixin_classes()
    hopper = hopper_vanilla_count()
    importer = generic_importer_status()
    if importer_removes_minecraft():
        raise ValueError("generic importer must not remove minecraft recipes")
    # Live data/minecraft/recipe is owned by content/vanilla-replace-mvp.
    # This R0 document still records the R0-era override count of 0.
    analogs = {
        "Vanilla": (
            "Additive cruciblecraft datapack recipes and authored "
            "minecraft:smelting mold-firing are not Loader_Recipes_Vanilla"
        ),
        "Replace": (
            "Generic Source Pack importer does not remove minecraft recipes; "
            "there is no data/minecraft/recipe override tree and no GT6 ASM "
            "transformer"
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
            }
        )
    return {
        "already_closed_elsewhere": already_closed_elsewhere(),
        "authored_minecraft_smelting": smelting,
        "capability_map_seed": capability_seed_row(),
        "capability_map_vanilla_replace": capability_rows(),
        "generated_by": GENERATED_BY,
        "generic_importer_removes_minecraft_recipes": False,
        "generic_importer_status": importer,
        "kinds": mapped,
        "minecraft_recipe_override_count": 0,
        "mixins": mixins,
        "note": (
            "The gap is a missing mechanism, not a missing hopper or mold-firing "
            "recipe. T30 121 hopper vanilla recipes and authored smelting do not "
            "cover Loader_Recipes_Vanilla, Loader_Recipes_Replace, or ASM "
            "Replacements."
        ),
        "nuclear_started": nuclear_started(),
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "status": "EXISTING_MECHANISM_READY",
        "hopper_hopper_vanilla_count": hopper,
        "wave_slug": SLUG,
    }


def replace_contract_document() -> dict[str, Any]:
    return {
        "generated_by": GENERATED_BY,
        "implemented": False,
        "questions": dict(REPLACE_QUESTIONS),
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "status": "REPLACE_CONTRACT_READY",
        "wave_slug": SLUG,
    }


def category_reasons(name: str) -> list[str]:
    if name == "Vanilla":
        return [
            "Vanilla capability-map cc_mechanism is none.",
            "Additive cruciblecraft recipes and mold-firing smelting are not "
            "Loader_Recipes_Vanilla.",
            "The gap is vanilla recipe addition plus deletion/replace policy, "
            "not another Source Pack.",
        ]
    return [
        "Replace capability-map cc_mechanism is none.",
        "Loader_Recipes_Replace and ASM Replacements stay one category.",
        "1.21 NeoForge has no GT6 transformer, and the generic importer does "
        "not remove minecraft recipes.",
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
        expected_files = len(PINNED_FILES[name])
        if int(summary["source_file_count"]) != expected_files:
            missing.append("dump")
        semantic = semantics_by_category.get(name)
        if semantic is None or int(semantic["source_file_count"]) != expected_files:
            missing.append("dump")
        if set(contract["questions"]) != set(REPLACE_QUESTIONS):
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
    token = "vanilla-replace"
    for path in RECIPE_GENERATED.rglob("*"):
        if token in path.as_posix():
            return True
    return False


def readiness_note(feasibility: dict[str, Any]) -> str:
    parts = [
        f"{row['category']} {row['verdict']}" for row in feasibility["categories"]
    ]
    return (
        f"{STATUS}. Two vanilla replace loader categories inherited "
        "byte-identical capability-map rows and three T13 source blobs. "
        + "; ".join(parts)
        + ". No implementation child is assigned. unique_active_wave is null."
    )


def evidence_document(
    inherited: dict[str, Any],
    feasibility: dict[str, Any],
) -> dict[str, Any]:
    leftover = leftover_later_count()
    closed = inherited["already_closed_elsewhere"]
    return {
        "allows_implementation_child": feasibility["allows_implementation_child"],
        "completion_delta": 0,
        "feasibility_by_category": {
            row["category"]: row["verdict"] for row in feasibility["categories"]
        },
        "generated_recipe_count": 0,
        "inherited_category_count": inherited["category_count"],
        "inherited_source_file_count": inherited["source_file_count"],
        "leftover_later_count": leftover,
        "minecraft_recipe_override_count": 0,
        "nuclear_track_c_started": False,
        "owns_families": 0,
        "partial_family_count": 0,
        "production_lock": None,
        "recipe_files_generated": False,
        "hopper_hopper_vanilla_count": closed["hopper_hopper_vanilla"]["recipe_count"],
        "worldgen_ores_vanilla_count": closed["worldgen_ores_vanilla"]["feature_count"],
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
        "cohort": "vanilla-replace-r0",
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
    contract = replace_contract_document()
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
        raise ValueError("src/recipe_generated mentions vanilla-replace")
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
    documents["replace_contract.json"] = contract
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
    if int(evidence.get("inherited_category_count", 0)) != 2:
        errors.append("inherited_category_count must be 2")
    if int(evidence.get("inherited_source_file_count", 0)) != 3:
        errors.append("inherited_source_file_count must be 3")
    if int(evidence.get("minecraft_recipe_override_count", 1)) != 0:
        errors.append("minecraft_recipe_override_count must be 0")
    if int(evidence.get("hopper_hopper_vanilla_count", 0)) != EXPECTED_HOPPER_VANILLA:
        errors.append("t30_hopper_vanilla_count must be 121")
    topology = census.load_json(root / "topology.json")
    extra = sorted(set(topology) - set(ALLOWED_TOPOLOGY_KEYS))
    if extra:
        errors.append(f"topology has forbidden keys: {extra}")
    errors.extend(check_forbidden_successors(json.dumps(topology, sort_keys=True)))
    feasibility = census.load_json(root / "feasibility.json")
    by_category = {row["category"]: row for row in feasibility.get("categories") or []}
    if tuple(by_category) != PINNED_NAMES:
        errors.append("feasibility categories drifted from the pinned two")
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
    if int(inherited.get("category_count", 0)) != 2:
        errors.append("inherited category_count must be 2")
    if int(inherited.get("source_file_count", 0)) != 3:
        errors.append("inherited source_file_count must be 3")
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
        for record in category.get("source_files") or []:
            path = str(record.get("path") or "")
            expected = EXPECTED_BLOBS.get(path)
            if expected is None:
                errors.append(f"unexpected source file {path}")
                continue
            if (
                record.get("git_blob_sha1") != expected[0]
                or int(record.get("size") or -1) != expected[1]
            ):
                errors.append(f"{path} blob drifted from the pinned T13 tree")
    hashes = inherited.get("authority_hashes") or {}
    live_hashes = authority_hashes()
    for key, digest in live_hashes.items():
        if hashes.get(key) != digest:
            errors.append(f"authority hash {key} drifted")
    if recipe_generated_mentions_slug():
        errors.append("src/recipe_generated mentions vanilla-replace")
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
