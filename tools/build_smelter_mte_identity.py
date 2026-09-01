#!/usr/bin/env python3
"""Smelter MTE identity catalog and B1 scatter acquisition.

Reads recycling/deferred-ordinary-ledger-r0 identity_candidate. Does not
publish recovery recipes or subtract from the deferred ledger.
"""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import closeout_seal
from tools import recycling_deferred_r0 as r0
from tools import t35_common as t35
from tools import t42_common as t42
from tools.recipe_bulk.slugs import KNOWN_SEMANTIC_SLUGS
from tools.wave_closeout import known_slugs
from tools.wave_closeout import spec_for

SLUG = "recycling/smelter-mte-identity"
NEXT_CHILD = "smelter/deferred-recycling"
GENERATED_BY = "python tools/build_smelter_mte_identity.py"
MTE_ITEM = "gregtech:gt.multitileentity"
SOURCE_META_COUNT = 1817
R0_SLUG = "recycling/deferred-ordinary-ledger-r0"
R0_DIR = t35.TOOLS / "waves" / "recycling" / "deferred-ordinary-ledger-r0"
ROOT_DIR = t35.TOOLS / "waves" / "recycling" / "smelter-mte-identity"
TOOLS_CATALOG = t35.TOOLS / "smelter_mte_identity_catalog.json"
BUNDLED_CATALOG = (
    t35.ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "smelter_mte_identity_catalog.json"
)
MODEL_ROOT = (
    t35.ROOT / "src" / "main" / "resources" / "assets" / "cruciblecraft" / "models" / "item"
)
TAG_PATH = (
    t35.ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "tags"
    / "item"
    / "smelter_mte_items.json"
)
DATA_ROOT = t35.ROOT / "src" / "main" / "resources" / "data" / "cruciblecraft"


def _require_r0() -> list[str]:
    errors = closeout_seal.check_wave_seal(R0_SLUG)
    readiness = t35.load_json(R0_DIR / "readiness.json")
    if readiness.get("status") != "RECYCLING_DEFERRED_LEDGER_R0_READY":
        errors.append("R0 is not RECYCLING_DEFERRED_LEDGER_R0_READY")
    candidate = t35.load_json(R0_DIR / "identity_candidate.json")
    if not candidate.get("unique_meta_equals_proven_family_count"):
        errors.append("R0 identity candidate unique meta proof failed")
    if int(candidate.get("proven_family_count") or 0) != SOURCE_META_COUNT:
        errors.append("R0 proven family count drifted from 1817")
    return errors


def _dump_names(candidate: dict[str, Any]) -> dict[int, str]:
    recipes = t42.load_map_recipes("gt.recipe.smelter")
    names: dict[int, str] = {}
    for row in candidate.get("identities") or []:
        meta = int(row["meta"])
        key = str(row.get("relation_key") or "")
        if "#" not in key:
            continue
        index = int(key.rsplit("#", 1)[-1])
        if index < 0 or index >= len(recipes):
            continue
        for operand in recipes[index].get("inputs") or []:
            if (
                str(operand.get("item") or "") == MTE_ITEM
                and int(operand.get("meta") or -1) == meta
            ):
                display = str(operand.get("displayName") or "").strip()
                if display:
                    names[meta] = display
                break
    return names


def build_catalog() -> dict[str, Any]:
    errors = _require_r0()
    if errors:
        raise ValueError("; ".join(errors))
    candidate = t35.load_json(R0_DIR / "identity_candidate.json")
    names = _dump_names(candidate)
    identities: list[dict[str, Any]] = []
    seen_meta: set[int] = set()
    seen_runtime: set[str] = set()
    for row in candidate.get("identities") or []:
        meta = int(row["meta"])
        if meta in seen_meta:
            raise ValueError(f"duplicate smelter identity meta {meta}")
        seen_meta.add(meta)
        overlap = bool(row.get("overlap_with_bath"))
        bath_id = str(row.get("bath_runtime_id") or "")
        if overlap:
            if not bath_id.startswith("cruciblecraft:"):
                raise ValueError(f"Bath overlap missing runtime_id for meta {meta}")
            runtime_id = bath_id
            registry_kind = "existing_item"
            registry_path = bath_id.split(":", 1)[1]
            acquisition = "bath_mte"
        else:
            registry_path = f"gt_mte/mte_{meta}"
            runtime_id = f"cruciblecraft:{registry_path}"
            registry_kind = "item"
            acquisition = "smelter_mte_scatter"
        if runtime_id in seen_runtime and not overlap:
            raise ValueError(f"duplicate runtime_id {runtime_id}")
        seen_runtime.add(runtime_id)
        english = names.get(meta) or f"GT Multitileentity {meta}"
        identities.append(
            {
                "acquisition_authority": acquisition,
                "chinese_name": english,
                "display_requirements": {
                    "distinguishable": True,
                    "holdable": True,
                    "model": "item/generated",
                },
                "english_name": english,
                "family_id": row["family_id"],
                "kind": "mte_item",
                "mapping_class": "exact_item",
                "meta": meta,
                "registry_kind": registry_kind,
                "registry_path": registry_path,
                "runtime_id": runtime_id,
                "source_evidence": "gt6_dump/gt6_recipe_dump/maps/gt.recipe.smelter.json",
                "source_item": MTE_ITEM,
                "source_revision": t35.SOURCE_REVISION,
                "template_key": row["template_key"],
            }
        )
    new_items = [row for row in identities if row["registry_kind"] == "item"]
    overlap = [row for row in identities if row["registry_kind"] == "existing_item"]
    if len(identities) != SOURCE_META_COUNT:
        raise ValueError(f"catalog {len(identities)} != 1817")
    return {
        "bath_overlap_count": len(overlap),
        "generated_by": GENERATED_BY,
        "identities": identities,
        "new_item_count": len(new_items),
        "runtime_identity_count": len(identities),
        "schema_version": 1,
        "source_item": MTE_ITEM,
        "source_meta_count": SOURCE_META_COUNT,
        "source_revision": t35.SOURCE_REVISION,
        "status": "SMELTER_MTE_IDENTITY_CATALOG",
        "wave_slug": SLUG,
    }


def _write_models(catalog: dict[str, Any]) -> None:
    for identity in catalog["identities"]:
        if identity["registry_kind"] != "item":
            continue
        path = MODEL_ROOT / f"{identity['registry_path']}.json"
        path.parent.mkdir(parents=True, exist_ok=True)
        t35.write_stable(
            path,
            {
                "parent": "minecraft:item/generated",
                "textures": {"layer0": "minecraft:item/iron_ingot"},
            },
        )


def _write_acquisition(catalog: dict[str, Any]) -> None:
    new_ids = [
        identity["runtime_id"]
        for identity in catalog["identities"]
        if identity["registry_kind"] == "item"
    ]
    TAG_PATH.parent.mkdir(parents=True, exist_ok=True)
    t35.write_stable(TAG_PATH, {"replace": False, "values": new_ids})
    configured = DATA_ROOT / "worldgen" / "configured_feature" / "smelter_mte_scatter.json"
    placed = DATA_ROOT / "worldgen" / "placed_feature" / "smelter_mte_scatter.json"
    biome = DATA_ROOT / "neoforge" / "biome_modifier" / "add_smelter_mte_scatter.json"
    catalog_row = DATA_ROOT / "worldgen_catalog" / "smelter_mte_scatter.json"
    configured.parent.mkdir(parents=True, exist_ok=True)
    placed.parent.mkdir(parents=True, exist_ok=True)
    biome.parent.mkdir(parents=True, exist_ok=True)
    catalog_row.parent.mkdir(parents=True, exist_ok=True)
    t35.write_stable(
        configured,
        {
            "type": "cruciblecraft:smelter_mte_scatter",
            "config": {
                "item_tag": "cruciblecraft:smelter_mte_items",
                "rarity": 128,
            },
        },
    )
    t35.write_stable(placed, {"feature": "cruciblecraft:smelter_mte_scatter", "placement": []})
    t35.write_stable(
        biome,
        {
            "biomes": "#minecraft:is_overworld",
            "features": ["cruciblecraft:smelter_mte_scatter"],
            "step": "top_layer_modification",
            "type": "neoforge:add_features",
        },
    )
    t35.write_stable(
        catalog_row,
        {
            "biome_modifier": {
                "biomes": "#minecraft:is_overworld",
                "features": ["cruciblecraft:smelter_mte_scatter"],
                "id": "add_smelter_mte_scatter",
                "step": "top_layer_modification",
            },
            "config": {
                "item_tag": "cruciblecraft:smelter_mte_items",
                "rarity": 128,
            },
            "feature_type": "cruciblecraft:smelter_mte_scatter",
            "id": "smelter_mte_scatter",
            "kind": "scatter",
            "placed_feature": "cruciblecraft:smelter_mte_scatter",
        },
    )


def _identity_delta(catalog: dict[str, Any]) -> dict[str, Any]:
    catalog_rel = "tools/smelter_mte_identity_catalog.json"
    catalog_hash = t35.sha256_file(TOOLS_CATALOG)
    records = []
    for identity in catalog["identities"]:
        meta = int(identity["meta"])
        records.append(
            {
                "authorities": [SLUG],
                "blocker_reason": None,
                "disposition": "proven",
                "evidence": [catalog_rel],
                "input_hashes": {catalog_rel: catalog_hash},
                "mapping_class": "exact_item",
                "source_key": f"{SLUG}|item:{MTE_ITEM}@{meta}",
                "target_identity": identity["runtime_id"],
                "target_kind": "item",
            }
        )
    return {
        "blockers": [],
        "generated_by": GENERATED_BY,
        "note": (
            "Smelter MTE exact-item identities. Does not rewrite frozen v2 "
            "records. Bath overlap reuses existing runtime ids."
        ),
        "order": SLUG,
        "records": records,
        "schema_version": 1,
        "status": "SMELTER_MTE_IDENTITY_LEDGER_DELTA",
        "wave_slug": SLUG,
    }


def write_artifacts() -> dict[str, Any]:
    catalog = build_catalog()
    ROOT_DIR.mkdir(parents=True, exist_ok=True)
    t35.write_stable(TOOLS_CATALOG, catalog)
    BUNDLED_CATALOG.parent.mkdir(parents=True, exist_ok=True)
    t35.write_stable(BUNDLED_CATALOG, catalog)
    _write_models(catalog)
    _write_acquisition(catalog)
    t35.write_stable(ROOT_DIR / "identity_ledger_delta.json", _identity_delta(catalog))
    wave = {
        "cohort": "smelter-mte-identity",
        "depends_on": [R0_SLUG],
        "owns_families": 0,
        "schema_version": 1,
        "unique_active_wave": True,
        "wave_slug": SLUG,
    }
    census = {
        "complete_family_count": 0,
        "completion_delta": 0,
        "generated_by": GENERATED_BY,
        "new_item_count": catalog["new_item_count"],
        "partial_family_count": 0,
        "reclassification_delta": 0,
        "remaining_recipe_gap": 0,
        "schema_version": 1,
        "source_meta_count": SOURCE_META_COUNT,
        "source_revision": t35.SOURCE_REVISION,
        "status": "CENSUS_DELTA_READY",
        "wave_slug": SLUG,
        "work_set": {"family_count": 0, "source_rows": 0},
    }
    topology = {
        "append_only": False,
        "complete_family_count": 0,
        "generated_by": GENERATED_BY,
        "next_unassigned": False,
        "remaining_recipe_gap": 0,
        "schema_version": 1,
        "source_revision": t35.SOURCE_REVISION,
        "status": "WAVE_READY",
        "unique_active_wave": NEXT_CHILD,
        "wave_slug": SLUG,
    }
    readiness = {
        "evidence": {
            "acquisition": "smelter_mte_scatter",
            "bath_overlap_count": catalog["bath_overlap_count"],
            "complete_family_count": 0,
            "completion_delta": 0,
            "new_item_count": catalog["new_item_count"],
            "one_x_joint_exit": False,
            "partial_family_count": 0,
            "program_status": "SMELTER_MTE_IDENTITY_READY",
            "recipe_files_generated": False,
            "reclassification_delta": 0,
            "remaining_recipe_gap": 0,
            "source_meta_count": SOURCE_META_COUNT,
            "unique_meta_equals_proven_family_count": True,
        },
        "generated_by": GENERATED_BY,
        "next_unassigned": False,
        "schema_version": 1,
        "source_revision": t35.SOURCE_REVISION,
        "status": "SMELTER_MTE_IDENTITY_READY",
        "unique_active_wave": NEXT_CHILD,
        "wave_complete": True,
        "wave_slug": SLUG,
    }
    t35.write_stable(ROOT_DIR / "wave.json", wave)
    t35.write_stable(ROOT_DIR / "census_delta.json", census)
    t35.write_stable(ROOT_DIR / "topology.json", topology)
    t35.write_stable(ROOT_DIR / "readiness.json", readiness)
    hashes = {
        "census": t35.sha256_file(ROOT_DIR / "census_delta.json"),
        "gametest_java": None,
        "gametest_log": None,
        "generated_recipes": None,
        "locked_support": None,
        "production_lock": None,
        "publication_group_manifest": None,
        "readiness": t35.sha256_file(ROOT_DIR / "readiness.json"),
        "receipt": None,
        "runtime_dependency_manifest": None,
        "shard_manifest": None,
        "topology": t35.sha256_file(ROOT_DIR / "topology.json"),
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
        "generated_by": "python tools/build_smelter_mte_identity.py --write",
        "hashes": hashes,
        "note": (
            "Zero-family Smelter MTE identity catalog. 1817 exact metas. "
            "Bath overlap mapped as existing_item. B1 scatter for new items. "
            "No recovery recipe completion. Hands off to smelter/deferred-recycling."
        ),
        "production_lock_sha256": None,
        "receipt_sha256": None,
        "reclassification_delta": 0,
        "relation_count": 0,
        "remaining_recipe_gap": 0,
        "schema_version": 1,
        "sealed_at_wave": SLUG,
        "source_revision": t35.SOURCE_REVISION,
        "status": "SEALED",
    }
    t35.write_stable(ROOT_DIR / "closeout_seal.json", seal)
    return {
        "bath_overlap_count": catalog["bath_overlap_count"],
        "new_item_count": catalog["new_item_count"],
        "source_meta_count": SOURCE_META_COUNT,
        "status": "SMELTER_MTE_IDENTITY_READY",
        "unique_active_wave": NEXT_CHILD,
        "wave_slug": SLUG,
    }


def check() -> list[str]:
    errors: list[str] = []
    errors.extend(_require_r0())
    try:
        live = build_catalog()
    except ValueError as error:
        return errors + [str(error)]
    if SLUG not in KNOWN_SEMANTIC_SLUGS:
        errors.append("identity slug missing from KNOWN_SEMANTIC_SLUGS")
    if SLUG not in known_slugs():
        errors.append("identity slug missing from wave_closeout")
    spec = spec_for(SLUG)
    if spec.unique_active_wave != NEXT_CHILD:
        errors.append("identity closeout spec unique_active_wave drifted")
    if spec.owns_families != 0 or spec.production_lock is not None:
        errors.append("identity closeout spec must be zero-family infrastructure")
    if not TOOLS_CATALOG.is_file():
        return errors + ["missing smelter MTE identity catalog"]
    committed = t35.load_json(TOOLS_CATALOG)
    drift = t35.first_json_diff(live, committed)
    if drift:
        errors.append(f"smelter MTE catalog drifted: {drift}")
    if BUNDLED_CATALOG.is_file():
        bundled = t35.load_json(BUNDLED_CATALOG)
        if bundled != committed:
            errors.append("bundled smelter MTE catalog drifted from tools catalog")
    else:
        errors.append("missing bundled smelter MTE identity catalog")
    readiness = t35.load_json(ROOT_DIR / "readiness.json")
    if readiness.get("status") != "SMELTER_MTE_IDENTITY_READY":
        errors.append("identity readiness is not SMELTER_MTE_IDENTITY_READY")
    if readiness.get("evidence", {}).get("recipe_files_generated") is not False:
        errors.append("identity child must not generate recovery recipes")
    if int(readiness.get("evidence", {}).get("completion_delta") or 0) != 0:
        errors.append("identity child must not complete deferred families")
    errors.extend(closeout_seal.check_wave_seal(SLUG))
    return errors


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args(argv)
    if args.write == args.check:
        parser.error("choose --write or --check")
    try:
        if args.write:
            payload = write_artifacts()
            print(json.dumps(payload, sort_keys=True))
            return 0
        errors = check()
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        print("smelter MTE identity catalog is current")
        return 0
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"smelter MTE identity failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
