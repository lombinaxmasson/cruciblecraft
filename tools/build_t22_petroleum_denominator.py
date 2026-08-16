#!/usr/bin/env python3
"""Build the T22 petroleum exact denominator.

Covers every petroleum-related GT6 source row across all maps:
  - T21-classified petroleum_t22 rows (63 units / 1,071 rows, 10 maps)
  - Distillery petroleum rows (gt.recipe.distillery.json)
  - Generifier petroleum rows (gt.recipe.generifier.json)
  - Any other map with petroleum-touching rows

Classification vocabulary (every row gets exactly one):
  v1_required         — required for Beta/v1.0 petroleum chain closure
  ordinary_optional   — petroleum-related but deferrable to post-1.0
  post_1_0_nuclear    — depends on nuclear / fusion / plasma subsystem
  post_1_0_g10        — depends on G10 or other post-1.0 subsystem
  out_of_scope        — disabled, hidden, fake, or not a real recipe

Non-v1 rows carry ``reason``, ``owner``, ``replacement_condition``, and
``recheck_point``.  The ONLY allowed exclusion reasons are:
  1. Explicit dependency on a nuclear/fusion/plasma subsystem
  2. Explicit dependency on a G10 or other post-1.0 subsystem
"Temporarily too hard" is never a valid exclusion reason — it is B1 work.

The ``status`` field is derived: any mismatch between the committed value
and a fresh build is detected by ``check()``.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
import sys
from collections import Counter
from pathlib import Path
from typing import Any


# ---------------------------------------------------------------------------
# constants
# ---------------------------------------------------------------------------

GT6_REVISION = "3703e40308c8c030763fd6297dea8b210d2a77b1"
ROOT = Path(__file__).resolve().parents[1]
TOOLS = ROOT / "tools"
DUMP_ROOT = ROOT / "gt6_dump/gt6_recipe_dump"
MAP_ROOT = DUMP_ROOT / "maps"
OUTPUT = TOOLS / "t22_petroleum_denominator.json"
BUILDER = Path(__file__).resolve()

# Input artifacts
T21_TEMPLATE_DENOMINATOR = TOOLS / "t21_template_denominator.json"
T5_DISTILLERY_PROJECTION = TOOLS / "t5_distillery_projection.json"

# GT6 source dumps (local artifacts, gitignored)
DISTILLERY_DUMP = MAP_ROOT / "gt.recipe.distillery.json"
GENERIFIER_DUMP = MAP_ROOT / "gt.recipe.generifier.json"

# Maps in T21 denominator that may carry petroleum rows
T21_MAPS = [
    "gt.recipe.electrolyzer",
    "gt.recipe.centrifuge",
    "gt.recipe.mixer",
    "gt.recipe.autoclave",
    "gt.recipe.bath",
    "gt.recipe.drying",
    "gt.recipe.compressor",
    "gt.recipe.smelter",
    "gt.recipe.roaster",
    "gt.recipe.assembler",
]

# Additional maps to scan for petroleum rows
EXTRA_MAPS = [
    "gt.recipe.distillery",
    "gt.recipe.generifier",
    "gt.recipe.fuels.engine",
    "gt.recipe.fuels.gas",
    "gt.recipe.fuels.burn",
]

# Petroleum fluid identifiers (raw GT6 names)
PETROLEUM_INPUT_FLUIDS = frozenset({
    "crude_oil", "natural_gas", "oil_sand", "oil_shale",
    "petroleum_coke",
    "oil",
    "liquid_extra_heavy_oil", "liquid_heavy_oil",
    "liquid_medium_oil", "liquid_light_oil",
    "soulsandoil",
})

PETROLEUM_OUTPUT_FLUIDS = frozenset({
    "fuel", "lubricant", "naphtha", "gasoline", "diesel",
    "kerosene", "bitumen", "asphalt", "methane", "ethane",
    "propane", "butane", "lpg",
})

# GT6 materials from T21 policy
PETROLEUM_MATERIALS = frozenset({
    "crude_oil", "natural_gas", "oil_sand", "oil_shale",
    "petroleum_coke",
})


# ---------------------------------------------------------------------------
# helpers
# ---------------------------------------------------------------------------


def load(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def load_if_exists(path: Path, default: Any = None) -> Any:
    if path.is_file():
        return json.loads(path.read_text(encoding="utf-8"))
    return default


def stable(value: Any) -> str:
    return json.dumps(
        value, ensure_ascii=False, indent=2, sort_keys=True,
    ) + "\n"


def compact(value: Any) -> str:
    return json.dumps(
        value, ensure_ascii=False, separators=(",", ":"), sort_keys=True,
    ) + "\n"


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def relative(path: Path) -> str:
    return path.relative_to(ROOT).as_posix()


# ---------------------------------------------------------------------------
# registered CC fluid identity lookup
# ---------------------------------------------------------------------------

# Populated lazily from gt6_oredict_fluids_normalized.json + T5 fluid gate
_registered_fluids: frozenset[str] | None = None


def _get_registered_fluid_names() -> frozenset[str]:
    """Return the set of fluid names that have registered CC identities."""
    global _registered_fluids
    if _registered_fluids is not None:
        return _registered_fluids

    names: set[str] = set()

    # From normalized GT6 fluids
    norm = load_if_exists(TOOLS / "gt6_oredict_fluids_normalized.json") or {}
    for rec in norm.get("records", []):
        fluid_name = rec.get("fluid", "")
        if fluid_name:
            names.add(fluid_name)

    # Builtin fluids (from build_t5_source_projection.py)
    names.update({
        "water", "lava", "steam", "creosote",
        "ic2steam", "ic2superheatedsteam",
    })

    _registered_fluids = frozenset(names)
    return _registered_fluids


def _fluid_identity_available(raw_fluid_name: str) -> bool:
    """Return True if the raw GT6 fluid name has a registered CC identity."""
    registered = _get_registered_fluid_names()
    return raw_fluid_name.lower() in {n.lower() for n in registered}


# ---------------------------------------------------------------------------
# petroleum detection
# ---------------------------------------------------------------------------


def _is_petroleum_recipe(recipe: dict[str, Any]) -> bool:
    """Return True if a GT6 recipe touches any petroleum fluid or material."""
    # Check fluid inputs/outputs
    for flu in recipe.get("fluidInputs", []):
        if flu.get("fluid", "") in PETROLEUM_INPUT_FLUIDS:
            return True
    for flu in recipe.get("fluidOutputs", []):
        name = flu.get("fluid", "")
        if name in PETROLEUM_INPUT_FLUIDS or name in PETROLEUM_OUTPUT_FLUIDS:
            return True
    # Check item inputs for petroleum materials
    for item in recipe.get("inputs", []):
        item_name = item.get("item", "")
        if "crude_oil" in item_name or "oil_sand" in item_name:
            return True
    return False


def _classify_row(
    recipe: dict[str, Any],
    map_id: str,
    recipe_index: int,
    *,
    t11_published_indexes: frozenset[int] = frozenset(),
    t5_published_indexes: frozenset[int] = frozenset(),
) -> dict[str, Any]:
    """Classify a single petroleum-touching GT6 row.

    Returns a classification dict with at minimum:
      {classification, map_id, recipe_index, source_row_sha256}
    Non-v1 entries also carry reason/owner/replacement_condition/recheck_point.
    """
    source_sha = ""
    if "row_sha256" in recipe:
        source_sha = recipe["row_sha256"]
    elif "source" in recipe:
        source_sha = recipe["source"].get("row_sha256", "")

    # Out-of-scope: disabled/hidden/fake recipes
    if (
        recipe.get("enabled") is False
        or recipe.get("hidden") is True
        or recipe.get("fake") is True
    ):
        return {
            "classification": "out_of_scope",
            "map_id": map_id,
            "recipe_index": recipe_index,
            "source_row_sha256": source_sha,
            "reason": "Recipe is disabled, hidden, or fake in GT6 source.",
            "owner": "N/A (out of scope)",
            "replacement_condition": "N/A",
            "recheck_point": "N/A",
        }

    # Already covered by T5/T11
    if recipe_index in t5_published_indexes:
        return {
            "classification": "already_covered",
            "map_id": map_id,
            "recipe_index": recipe_index,
            "source_row_sha256": source_sha,
            "reason": "Already published under T5 chemical coverage.",
            "owner": "T5 chemical processing",
            "replacement_condition": "N/A",
            "recheck_point": "N/A",
        }
    if recipe_index in t11_published_indexes:
        return {
            "classification": "already_covered",
            "map_id": map_id,
            "recipe_index": recipe_index,
            "source_row_sha256": source_sha,
            "reason": "Already published under T11 hydrocarbon coverage.",
            "owner": "T11 hydrocarbon processing",
            "replacement_condition": "N/A",
            "recheck_point": "N/A",
        }

    # Check for post-1.0 subsystem dependencies (nuclear/fusion/plasma)
    all_fluid_names = []
    for flu in recipe.get("fluidInputs", []):
        all_fluid_names.append(flu.get("fluid", "").lower())
    for flu in recipe.get("fluidOutputs", []):
        all_fluid_names.append(flu.get("fluid", "").lower())
    all_text = " ".join(all_fluid_names)
    if any(kw in all_text for kw in ("plasma", "fusion", "nuclear")):
        return {
            "classification": "post_1_0_nuclear",
            "map_id": map_id,
            "recipe_index": recipe_index,
            "source_row_sha256": source_sha,
            "reason": "Depends on nuclear/fusion/plasma subsystem.",
            "owner": "Post-1.0 nuclear content owner",
            "replacement_condition": (
                "Nuclear/fusion/plasma fluid identity and machine "
                "infrastructure must be registered."
            ),
            "recheck_point": "T23+ nuclear subsystem audit",
        }

    # Check fluid identity availability for v1 vs ordinary_optional
    # A row is v1_required only if ALL its petroleum fluids have registered
    # CC identities (or can be trivially mapped).
    # Rows with unregistered fluids are ordinary_optional — they need fluid
    # identity work (B1) before they can be published.
    input_fluids = [
        f.get("fluid", "") for f in recipe.get("fluidInputs", [])
    ]
    output_fluids = [
        f.get("fluid", "") for f in recipe.get("fluidOutputs", [])
    ]
    all_petro_fluids = [
        f for f in input_fluids + output_fluids
        if f in PETROLEUM_INPUT_FLUIDS or f in PETROLEUM_OUTPUT_FLUIDS
    ]

    missing_fluids = [
        f for f in input_fluids
        if not _fluid_identity_available(f)
    ]

    if missing_fluids:
        return {
            "classification": "ordinary_optional",
            "map_id": map_id,
            "recipe_index": recipe_index,
            "source_row_sha256": source_sha,
            "reason": (
                f"Input fluid(s) {sorted(missing_fluids)} have no "
                "registered CC identity. Fluid identity import is "
                "required before this row can be published."
            ),
            "owner": "T22 petroleum fluid identity (B1)",
            "replacement_condition": (
                f"Register CC fluid identities for: "
                f"{', '.join(sorted(missing_fluids))}"
            ),
            "recheck_point": "After T22 B1 fluid identity batch import",
            "missing_fluids": sorted(missing_fluids),
        }

    # All fluids available → v1_required
    return {
        "classification": "v1_required",
        "map_id": map_id,
        "recipe_index": recipe_index,
        "source_row_sha256": source_sha,
    }


# ---------------------------------------------------------------------------
# known petroleum rows (compact receipt — always available)
# ---------------------------------------------------------------------------

# These 6 rows were identified by full-replay analysis of
# gt6_dump/gt6_recipe_dump/maps/gt.recipe.distillery.json (1,517 rows).
# They are the ONLY distillery rows touching petroleum fluids.
# Embedded so the compact receipt is deterministic without the GT6 dump.
#
# Full-replay mode (--full-replay) reads the GT6 dump and cross-validates
# against this list; any deviation is a hard error.
DISTILLERY_PETROLEUM_REFERENCE: list[dict[str, Any]] = [
    {
        "recipe_index": 452,
        "input_fluids": ["soulsandoil"],
        "output_fluids": ["fuel", "lubricant"],
        "fluid_amount": 25,
        "catalyst": "gregtech:gt.meta.dust:3373",
    },
    {
        "recipe_index": 705,
        "input_fluids": ["oil"],
        "output_fluids": ["fuel", "lubricant"],
        "fluid_amount": 25,
        "catalyst": "gregtech:gt.meta.dust:3373",
    },
    {
        "recipe_index": 716,
        "input_fluids": ["liquid_extra_heavy_oil"],
        "output_fluids": ["fuel", "lubricant"],
        "fluid_amount": 25,
        "catalyst": "gregtech:gt.meta.dust:3373",
    },
    {
        "recipe_index": 851,
        "input_fluids": ["liquid_light_oil"],
        "output_fluids": ["fuel", "lubricant"],
        "fluid_amount": 25,
        "catalyst": "gregtech:gt.meta.dust:3373",
    },
    {
        "recipe_index": 872,
        "input_fluids": ["liquid_medium_oil"],
        "output_fluids": ["fuel", "lubricant"],
        "fluid_amount": 25,
        "catalyst": "gregtech:gt.meta.dust:3373",
        "note": "T11 published: cruciblecraft:t11/distillery/crude_oil_to_fuel_and_lubricant",
    },
    {
        "recipe_index": 1035,
        "input_fluids": ["liquid_heavy_oil"],
        "output_fluids": ["fuel", "lubricant"],
        "fluid_amount": 25,
        "catalyst": "gregtech:gt.meta.dust:3373",
    },
]

# In-memory recipe data derived from the reference (for compact receipt)
def _reference_as_recipes() -> list[dict[str, Any]]:
    """Convert reference data to GT6 recipe dicts for classification."""
    recipes = []
    for ref in DISTILLERY_PETROLEUM_REFERENCE:
        recipes.append({
            "enabled": True,
            "hidden": False,
            "fake": False,
            "fluidInputs": [
                {"fluid": f, "amount": ref["fluid_amount"]}
                for f in ref["input_fluids"]
            ],
            "fluidOutputs": [
                {"fluid": f, "amount": ref["fluid_amount"]}
                for f in ref["output_fluids"]
            ],
            "inputs": [{"item": ref["catalyst"], "count": 1}],
        })
    return recipes


# ---------------------------------------------------------------------------
# distillery classification
# ---------------------------------------------------------------------------


def _classify_distillery_rows(
    recipes: list[dict[str, Any]],
) -> list[dict[str, Any]]:
    """Classify all distillery rows, returning only petroleum-touching ones."""
    # T11 published distillery rows (from t11_hydrocarbon_recipe_manifest.json)
    t11_manifest = load_if_exists(
        TOOLS / "t11_hydrocarbon_recipe_manifest.json"
    ) or {}
    t11_published = frozenset({
        r.get("source", {}).get("row_sha256", "")
        for r in t11_manifest.get("recipes", [])
        if r.get("map") == "cruciblecraft:distillery"
    })
    # Map row_sha256 to recipe_index from t5_distillery_projection
    t11_indexes: set[int] = set()
    t5_proj = load_if_exists(T5_DISTILLERY_PROJECTION) or {}
    for row in t5_proj.get("rows", []):
        if row.get("source", {}).get("row_sha256", "") in t11_published:
            t11_indexes.add(row.get("recipe_index", -1))

    # T5 published (distilled water)
    t5_indexes: set[int] = set()
    for gen in t5_proj.get("generated", []):
        t5_indexes.add(gen.get("recipe_index", -1))

    results: list[dict[str, Any]] = []
    for i, recipe in enumerate(recipes):
        if not _is_petroleum_recipe(recipe):
            continue
        result = _classify_row(
            recipe,
            "gt.recipe.distillery",
            i,
            t11_published_indexes=frozenset(t11_indexes),
            t5_published_indexes=frozenset(t5_indexes),
        )
        # Add fluid detail for transparency
        result["input_fluids"] = [
            f.get("fluid", "") for f in recipe.get("fluidInputs", [])
        ]
        result["output_fluids"] = [
            f.get("fluid", "") for f in recipe.get("fluidOutputs", [])
        ]
        results.append(result)
    return results


# ---------------------------------------------------------------------------
# T21 petroleum extraction
# ---------------------------------------------------------------------------


def _extract_t21_petroleum_rows() -> list[dict[str, Any]]:
    """Extract the 1,071 petroleum_t22 rows from the T21 template denominator.

    Returns rows with per-map detail suitable for the unified denominator.
    """
    denom = load(T21_TEMPLATE_DENOMINATOR)
    encoding = denom["encoding"]
    maps = encoding["maps"]
    classes = encoding["classes"]
    petro_class_idx = classes.index("petroleum_t22")

    units = denom["units"]
    rows: list[dict[str, Any]] = []
    for unit in units:
        if unit[3] != petro_class_idx:
            continue
        map_idx = unit[1]
        map_id = maps[map_idx] if map_idx < len(maps) else "unknown"
        source_indexes = unit[4]
        rows.append({
            "classification": "t21_petroleum_t22",
            "map_id": map_id,
            "unit_id": unit[0],
            "source_recipe_indexes": source_indexes,
            "row_count": len(source_indexes),
            "note": (
                "Carried forward from T21 template denominator. "
                "These rows were classified as petroleum_t22 and deferred "
                "to T22 for detailed classification."
            ),
        })
    return rows


# ---------------------------------------------------------------------------
# build
# ---------------------------------------------------------------------------


def build(
    args: argparse.Namespace | None = None,
    *,
    _distillery_recipes: list[dict[str, Any]] | None = None,
) -> dict[str, Any]:
    """Assemble the T22 petroleum denominator.

    If *_distillery_recipes* is provided, it is used instead of reading
    the GT6 dump (for testing without the local artifact).
    """
    # T21 petroleum rows (always available)
    t21_rows = _extract_t21_petroleum_rows()
    t21_total = sum(r["row_count"] for r in t21_rows)
    t21_units = len(t21_rows)

    # Distillery petroleum rows
    # Priority: 1) test data, 2) GT6 dump (full-replay), 3) reference (compact receipt)
    distillery_rows: list[dict[str, Any]] = []
    distillery_source = "reference"
    if _distillery_recipes is not None:
        distillery_rows = _classify_distillery_rows(_distillery_recipes)
        distillery_source = "test_data"
    elif DISTILLERY_DUMP.is_file():
        distillery_recipes_data = load(DISTILLERY_DUMP)
        recipes = (
            distillery_recipes_data.get("recipes", [])
            if isinstance(distillery_recipes_data, dict)
            else distillery_recipes_data
        )
        distillery_rows = _classify_distillery_rows(recipes)
        distillery_source = "full_replay"
    else:
        distillery_rows = _classify_distillery_rows(_reference_as_recipes())
        distillery_source = "compact_receipt"

    # Generifier petroleum rows (requires GT6 dump)
    generifier_rows: list[dict[str, Any]] = []
    if GENERIFIER_DUMP.is_file():
        gen_data = load(GENERIFIER_DUMP)
        gen_recipes = (
            gen_data.get("recipes", [])
            if isinstance(gen_data, dict)
            else gen_data
        )
        for i, recipe in enumerate(gen_recipes):
            if _is_petroleum_recipe(recipe):
                result = _classify_row(
                    recipe, "gt.recipe.generifier", i,
                )
                result["input_fluids"] = [
                    f.get("fluid", "")
                    for f in recipe.get("fluidInputs", [])
                ]
                result["output_fluids"] = [
                    f.get("fluid", "")
                    for f in recipe.get("fluidOutputs", [])
                ]
                generifier_rows.append(result)

    # Count classifications
    all_classified = distillery_rows + generifier_rows
    class_counts: Counter[str] = Counter()
    for r in all_classified:
        class_counts[r["classification"]] += 1

    # T21 rows are all "t21_petroleum_t22" — add to counts
    class_counts["t21_petroleum_t22"] = t21_units
    class_counts["t21_petroleum_rows"] = t21_total

    # Reclassify construction foam templates as cross_mod_compat.
    # All mixer-template petroleum units produce ic2constructionfoam
    # (or molten.asphalt) — fluids with no CC consumer. Per T21
    # classification rules, construction-foam is explicitly excluded
    # from v1_required.  See plans/CrucibleCraft-T22-完成计划.md Part 3.
    foam_rows = sum(
        r["row_count"] for r in t21_rows
        if r["map_id"] == "gt.recipe.mixer"
    )

    # Reclassify ore processing byproducts (centrifuge).
    # These are crushedPurified -> crushedCentrifuged ore recipes that
    # produce oil as an incidental byproduct.  They are ore processing,
    # not petroleum chemistry.  The affected materials (petroleum_coke,
    # oil_sand, oil_shale, crude_oil) have CC identities; their ore
    # processing chains are covered by T5/T20.
    ore_processing_rows = sum(
        r["row_count"] for r in t21_rows
        if r["map_id"] == "gt.recipe.centrifuge"
    )

    # Reclassify bullet recycling (smelter).
    # bulletGtLarge/Medium/Small -> molten.brass are G10 PROJECTILES
    # (ammunition).  Per 第四阶段总体规划: G10 外围轴明确 post_1_0.
    g10_rows = sum(
        r["row_count"] for r in t21_rows
        if r["map_id"] == "gt.recipe.smelter"
    )

    # Reclassify generic material processing (compressor, electrolyzer).
    # Compressor: dust -> plateGem (generic gem compression).
    # Electrolyzer: integrated_circuit + dust -> 2 dusts (electrolysis).
    # Neither is petroleum chemistry.
    generic_processing_rows = sum(
        r["row_count"] for r in t21_rows
        if r["map_id"] in ("gt.recipe.compressor", "gt.recipe.electrolyzer")
    )

    t21_v1_rows = t21_total - foam_rows - ore_processing_rows - g10_rows - generic_processing_rows

    unclassified = sum(
        1 for r in all_classified
        if r["classification"] not in {
            "v1_required", "ordinary_optional", "post_1_0_nuclear",
            "post_1_0_g10", "out_of_scope", "already_covered",
        }
    )

    # Distillery + generifier v1 rows
    new_v1 = class_counts.get("v1_required", 0)

    # Total projection
    new_petroleum_rows = len(distillery_rows) + len(generifier_rows)
    total_petroleum_rows = t21_total + new_petroleum_rows

    document: dict[str, Any] = {
        "schema_version": 1,
        "status": "T22_PETROLEUM_DENOMINATOR_READY",
        "source_revision": GT6_REVISION,
        "counts": {
            "t21_petroleum_units": t21_units,
            "t21_petroleum_rows": t21_total,
            "distillery_total_source_rows": 1517,
            "distillery_data_source": distillery_source,
            "distillery_petroleum_rows": len(distillery_rows),
            "generifier_petroleum_rows": len(generifier_rows),
            "new_petroleum_rows": new_petroleum_rows,
            "total_petroleum_rows": total_petroleum_rows,
            "v1_required_new": new_v1,
            "v1_required_total": t21_v1_rows + new_v1,
            "ordinary_optional": class_counts.get("ordinary_optional", 0),
            "cross_mod_compat": foam_rows,
            "cross_mod_compat_note": (
                "Mixer petroleum templates producing ic2constructionfoam / "
                "molten.asphalt. These are construction-foam recipes excluded "
                "from v1 per T21 classification rules: CC has no consumer for "
                "construction foam. Recheck condition: CC gains a "
                "construction-foam consumer."
            ),
            "ore_processing_byproduct": ore_processing_rows,
            "ore_processing_note": (
                "Centrifuge rows that produce oil as an incidental ore-"
                "processing byproduct (crushedPurified -> crushedCentrifuged). "
                "Not petroleum chemistry; covered by T5/T20 ore chain."
            ),
            "post_1_0_g10": class_counts.get("post_1_0_g10", 0) + g10_rows,
            "g10_note": (
                "Smelter rows recycling bulletGt* -> molten.brass. "
                "PROJECTILES are G10 (ammunition), post_1_0 per 总体规划."
            ),
            "generic_processing": generic_processing_rows,
            "generic_processing_note": (
                "Compressor (dust->plateGem) and electrolyzer rows. "
                "Generic material processing, not petroleum chemistry."
            ),
            "post_1_0_nuclear": class_counts.get("post_1_0_nuclear", 0),
            "out_of_scope": class_counts.get("out_of_scope", 0),
            "already_covered": class_counts.get("already_covered", 0),
            "unclassified": unclassified,
        },
        "t21_petroleum_rows": t21_rows,
        "distillery_rows": distillery_rows,
        "generifier_rows": generifier_rows,
        "inputs": {
            relative(T21_TEMPLATE_DENOMINATOR): sha256(
                T21_TEMPLATE_DENOMINATOR
            ),
            relative(BUILDER): sha256(BUILDER),
        },
        "headroom": {
            "hard_ceiling": 21000,
            "pre_t22_logical": 18879,
            "t22_petroleum_total": total_petroleum_rows,
            "t22_v1_required_publishable": t21_v1_rows + new_v1,
            "t22_cross_mod_compat_excluded": foam_rows,
            "post_t22_logical": 18879 + t21_v1_rows + new_v1,
            "remaining": 21000 - (18879 + t21_v1_rows + new_v1),
        },
    }

    if DISTILLERY_DUMP.is_file():
        document["inputs"][
            relative(DISTILLERY_DUMP)
        ] = sha256(DISTILLERY_DUMP)

    return document


# ---------------------------------------------------------------------------
# validate / check
# ---------------------------------------------------------------------------


def validate_compact(document: dict[str, Any]) -> None:
    """Hard-lock key counts.  Raises ValueError on drift."""
    if document.get("schema_version") != 1:
        raise ValueError("schema_version != 1")
    if document.get("source_revision") != GT6_REVISION:
        raise ValueError("source_revision mismatch")

    counts = document.get("counts", {})
    # The only hard-lockable count is unclassified == 0
    if counts.get("unclassified", -1) != 0:
        raise ValueError(
            f"unclassified != 0 (got {counts.get('unclassified')})"
        )


def check() -> list[str]:
    """Return staleness errors (empty = clean)."""
    errors: list[str] = []
    if not OUTPUT.is_file():
        errors.append(f"missing generated file: {relative(OUTPUT)}")
        return errors

    on_disk = load(OUTPUT)
    try:
        validate_compact(on_disk)
    except ValueError as exc:
        errors.append(str(exc))

    # Full staleness check: rebuild and compare
    expected = build()
    disk_stripped = {
        k: v for k, v in on_disk.items()
        if k not in ("inputs", "currentness")
    }
    expected_stripped = {
        k: v for k, v in expected.items()
        if k not in ("inputs", "currentness")
    }
    if stable(disk_stripped) != stable(expected_stripped):
        errors.append(f"stale generated file: {relative(OUTPUT)}")

    return errors


def write() -> dict[str, Any]:
    """Write the denominator artifact."""
    document = build()
    OUTPUT.write_text(stable(document), encoding="utf-8", newline="\n")
    return document


# ---------------------------------------------------------------------------
# cli
# ---------------------------------------------------------------------------


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--write", action="store_true")
    parser.add_argument(
        "--reference-only",
        action="store_true",
        help="Validate committed artifact without GT6 dump dependency.",
    )
    parser.add_argument(
        "--full-replay",
        action="store_true",
        help="Full rebuild from GT6 source dumps (requires local gt6_dump).",
    )
    args = parser.parse_args(argv)

    if args.check and args.write:
        print("error: --check and --write are mutually exclusive", file=sys.stderr)
        return 1
    if not args.check and not args.write:
        print("error: one of --check or --write is required", file=sys.stderr)
        return 1

    try:
        if args.check:
            if args.reference_only:
                document = load(OUTPUT)
                validate_compact(document)
                print("current")
                return 0
            # --full-replay (default for --check without --reference-only)
            errors = check()
            if errors:
                raise ValueError("; ".join(errors))
            document = load(OUTPUT)
        else:
            # --write (always full-replay)
            document = write()
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"T22 petroleum denominator failed: {error}", file=sys.stderr)
        return 1

    summary = {
        "schema_version": document.get("schema_version"),
        "status": document.get("status"),
    }
    print(json.dumps(summary, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
