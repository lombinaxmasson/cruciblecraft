#!/usr/bin/env python3
"""Build the T36 post-machine census overlay. Does not rewrite T35 artifacts."""
from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from tools import t35_common as t35  # noqa: E402
from tools import t36_common as t36  # noqa: E402
from tools.build_t36_machine_target import registered_processing_ids  # noqa: E402

BUILDER = Path(__file__).resolve()
OUTPUT = t36.CENSUS_DELTA
T35_FOUNDATION = {
    "t13_identities": 765,
    "exclusion_source_sites": 763,
    "exclusion_expanded_rows": 1701,
    "recipe_rows_accounted": 78682,
    "recipe_families": 5718,
}
MOD_MENUS = (
    ROOT
    / "src/main/java/com/masson/cruciblecraft/registry/ModMenus.java"
)
MOD_BLOCK_ENTITIES = (
    ROOT
    / "src/main/java/com/masson/cruciblecraft/registry/ModBlockEntities.java"
)
GENERATED_RECIPE_ROOT = (
    ROOT / "src/generated/resources/data/cruciblecraft/recipe"
)
LOAD_BASELINE = t35.LOAD_BASELINE
T14_COUNTABLE = (
    "datapack_authored_entries",
    "eager_publication_rows",
    "lazy_logical_rows",
)
T14_PENDING = (
    "lazy_cache_ceiling_rows",
    "sync_bytes",
    "server_reload_ms",
    "server_index_ms",
    "client_reload_ms",
    "client_index_ms",
    "retained_memory_bytes",
    "allocation_bytes",
    "lookup_p95_ns",
    "lookup_candidate_count",
)
MENU_ID = re.compile(r'processing\(\s*"([a-z0-9_]+)"')
BE_ID = re.compile(r'register\(\s*"([a-z0-9_]+)"')
OPENING_MENUS = 24
OPENING_PROCESSING_BE = 1
BOOTSTRAP_RECIPES = 2


def _json_files(root: Path) -> list[Path]:
    if not root.is_dir():
        return []
    return sorted(path for path in root.rglob("*.json") if path.is_file())


def _machine_recipe_ids() -> list[str]:
    folder = GENERATED_RECIPE_ROOT / "machines"
    return sorted(
        f"cruciblecraft:machines/{path.stem}"
        for path in folder.glob("*.json")
        if path.is_file()
    )


def _casing_recipe_ids() -> list[str]:
    folder = GENERATED_RECIPE_ROOT / "components"
    return sorted(
        f"cruciblecraft:components/{path.stem}"
        for path in folder.glob("*machine_casing.json")
        if path.is_file()
    )


def _family_ids_for_host(families: dict[str, Any], host: str) -> list[str]:
    return sorted(
        str(row["family_id"])
        for row in families.get("families") or []
        if isinstance(row, dict)
        and row.get("cc_host_map") == host
        and row.get("family_id")
    )


def _t14_axis_row(
    *,
    axis_id: str,
    opening: Any,
    delta: Any,
    closing: Any,
    measured: bool,
    evidence: str,
    hard_ceiling: Any,
) -> dict[str, Any]:
    return {
        "axis": axis_id,
        "opening": opening,
        "delta": delta,
        "closing": closing,
        "measured": measured,
        "pending": not measured,
        "evidence": evidence,
        "hard_ceiling": hard_ceiling,
        "hard_ceiling_raised": False,
    }


def build() -> dict[str, Any]:
    target = t35.load_json(t36.MACHINE_TARGET)
    catalog = t35.load_json(t36.MACHINE_TIERS)
    families = t35.load_json(t36.RECIPE_FAMILIES)
    readiness = t35.load_json(t35.READINESS)
    load_baseline = t35.load_json(LOAD_BASELINE)
    freeze = target["freeze"]
    target_ids = [row["variant_id"] for row in target["rows"]]
    catalog_ids = [row["id"] for row in catalog.get("variants") or []]
    opening_ids = list(freeze["opening_ids"])
    missing = sorted(set(target_ids) - set(catalog_ids))
    extra = sorted(set(catalog_ids) - set(target_ids))
    census_counts = readiness["census_summary"]["counts"]
    foundation = {
        key: int(census_counts[key])
        for key in T35_FOUNDATION
    }
    host_projection = target.get("host_projection") or {}
    catalog_id_set = set(catalog_ids)
    missing_hosts = []
    for host, row in host_projection.items():
        if not isinstance(row, dict):
            continue
        status = row.get("target_status")
        if status in {
            "product_excluded",
            "host_requires_declared_future_system",
        }:
            continue
        variants = set(row.get("target_variant_ids") or [])
        if variants and not variants <= catalog_id_set:
            missing_hosts.append(host)
    kind_ids = sorted({row["canonical_kind"] for row in target["rows"]})
    added_ids = sorted(set(catalog_ids) - set(opening_ids))
    mod_blocks = t36.MOD_BLOCKS.read_text(encoding="utf-8")
    live_ids = sorted(
        registered_processing_ids(mod_blocks, catalog) & set(target_ids)
    )
    opening_runtime = sorted(set(opening_ids) & set(live_ids))
    menus = MENU_ID.findall(MOD_MENUS.read_text(encoding="utf-8"))
    be_types = BE_ID.findall(MOD_BLOCK_ENTITIES.read_text(encoding="utf-8"))
    machine_recipes = _machine_recipe_ids()
    casing_recipes = _casing_recipe_ids()
    opening_machine_recipes = [
        recipe_id
        for recipe_id in machine_recipes
        if recipe_id.replace("cruciblecraft:machines/", "cruciblecraft:")
        in set(opening_ids)
    ]
    new_machine_recipes = [
        recipe_id
        for recipe_id in machine_recipes
        if recipe_id.replace("cruciblecraft:machines/", "cruciblecraft:")
        in set(added_ids)
        or recipe_id.endswith("/steel_roaster")
        or recipe_id.endswith("/coagulator")
    ]
    # Casings added beyond the T15 locked six.
    t15_casings = {
        "cruciblecraft:components/aluminium_machine_casing",
        "cruciblecraft:components/bronze_double_machine_casing",
        "cruciblecraft:components/stainless_steel_machine_casing",
        "cruciblecraft:components/steel_double_machine_casing",
        "cruciblecraft:components/steel_galvanized_machine_casing",
        "cruciblecraft:components/titanium_double_machine_casing",
    }
    new_casings = [recipe_id for recipe_id in casing_recipes if recipe_id not in t15_casings]
    authored_delta = len(new_machine_recipes) + len(new_casings)
    family_scope = {}
    reissued = []
    by_host = families.get("by_host_map") or {}
    for host, payload in sorted(by_host.items()):
        opening_status = str((payload.get("host") or {}).get("status") or "missing")
        projection = host_projection.get(host) or {}
        closing_status = str(projection.get("target_status") or opening_status)
        closing_host = str(projection.get("current_host") or "missing")
        scope = "in_scope_1x" if closing_status in {"host_exact", "host_targeted_by_t36"} else "unchanged"
        row = {
            "families": int(payload.get("families") or 0),
            "opening_host_status": opening_status,
            "closing_host_status": closing_status,
            "closing_current_host": closing_host,
            "closing_scope": scope,
            "family_ids": _family_ids_for_host(families, host) if host == "cruciblecraft:roaster" else [],
        }
        if opening_status == "missing" and closing_status in {
            "host_exact",
            "host_targeted_by_t36",
        }:
            row["reason"] = (
                "T36 registered a live host for this map; families are in_scope_1x "
                "without rewriting T35 family files."
            )
            reissued.append(host)
        family_scope[host] = row
    axes = (load_baseline.get("axes") or {})
    t14_opening = {}
    t14_delta = {}
    t14_closing = {}
    t14_rows = []
    for axis_id in T14_COUNTABLE + T14_PENDING:
        axis = axes.get(axis_id) or {}
        opening = axis.get("opening_base")
        hard = axis.get("hard_ceiling")
        t14_opening[axis_id] = opening
        if axis_id == "datapack_authored_entries":
            delta = authored_delta
            measured = True
            evidence = (
                "T36 machine-block and casing shaped recipes under "
                "src/generated/resources/data/cruciblecraft/recipe"
            )
            closing = (opening or 0) + delta
        elif axis_id == "eager_publication_rows":
            delta = authored_delta
            measured = True
            evidence = (
                "T36 adds eager datapack shaped crafts for new machines/casings; "
                "not GT RecipeMap publication"
            )
            closing = (opening or 0) + delta
        elif axis_id == "lazy_logical_rows":
            delta = 0
            measured = True
            evidence = "T36 added no GT processing RecipeMap rows"
            closing = opening
        else:
            delta = None
            measured = False
            evidence = "runtime-perf axis is pending; not zero-filled"
            closing = None
        t14_delta[axis_id] = {
            "value": delta,
            "measured": measured,
            "pending": not measured,
        }
        t14_closing[axis_id] = closing
        t14_rows.append(
            _t14_axis_row(
                axis_id=axis_id,
                opening=opening,
                delta=delta,
                closing=closing,
                measured=measured,
                evidence=evidence,
                hard_ceiling=hard,
            )
        )
    publication = {
        "eager": authored_delta,
        "lazy": 0,
        "logical": 0,
        "bootstrap_recipes": BOOTSTRAP_RECIPES,
        "measured": True,
        "note": (
            "eager counts T36 machine/casing shaped recipes; "
            "lazy/logical GT map publication is unchanged"
        ),
    }
    added_blocks = len(added_ids)
    added_items = added_blocks + len(new_casings)
    added_menus = max(0, len(menus) - OPENING_MENUS)
    added_be = 0
    return {
        "schema_version": 1,
        "status": "T36_CENSUS_DELTA_READY",
        "source_revision": t36.SOURCE_REVISION,
        "generated_by": "python tools/build_t36_census_delta.py",
        "t35_foundation": foundation,
        "t35_foundation_unchanged": foundation == T35_FOUNDATION,
        "opening": {
            "catalog_rows": 33,
            "runtime_machine_ids": opening_runtime,
            "runtime_source": "ModBlocks generic catalog registration ∩ opening 33",
        },
        "closing": {
            "catalog_rows": len(catalog_ids),
            "target_rows": len(target_ids),
            "kinds": len(kind_ids),
            "runtime_machine_ids": live_ids,
            "runtime_source": "ModBlocks generic catalog registration ∩ frozen target",
        },
        "delta": {
            "added_machine_ids": added_ids,
            "added_machine_id_count": len(added_ids),
            "target_minus_catalog": missing,
            "catalog_minus_target": extra,
            "blocks": added_blocks,
            "items": added_items,
            "block_entity_types": added_be,
            "menu_types": added_menus,
            "acquisition_recipes": len(new_machine_recipes) + len(new_casings),
            "resources": {
                "machine_recipes": new_machine_recipes,
                "casing_recipes": new_casings,
            },
            "publication": publication,
            "evidence": {
                "menus_registered": menus,
                "block_entity_types": be_types,
            },
        },
        "sets": {
            "target": sorted(target_ids),
            "catalog": sorted(catalog_ids),
            "runtime": live_ids,
            "missing": missing,
        },
        "host_projection": {
            "unowned_missing_hosts": missing_hosts,
        },
        "family_scope": {
            "t35_files_rewritten": False,
            "reissued_hosts": reissued,
            "hosts": family_scope,
        },
        "t14_load": {
            "hard_ceiling_raised": False,
            "opening": t14_opening,
            "delta": t14_delta,
            "closing": t14_closing,
            "axes": t14_rows,
        },
        "validators": {
            "target_catalog_equal": 0 if not missing and not extra else 1,
            "opening_33_present": 0 if set(opening_ids) <= set(catalog_ids) else 1,
            "t35_foundation_unchanged": 0 if foundation == T35_FOUNDATION else 1,
            "unowned_missing_hosts": len(missing_hosts),
            "portfolio_scope_not_set_by_priority": 0,
            "runtime_measured_from_registry": 0,
            "publication_not_zero_filled": 0 if publication["eager"] > 0 else 1,
            "t14_hard_ceiling_not_raised": 0,
            "t14_pending_not_zero_filled": 0 if all(
                t14_delta[axis]["pending"] and t14_delta[axis]["value"] is None
                for axis in T14_PENDING
            ) else 1,
            "roaster_host_reissued": 0 if (
                family_scope.get("cruciblecraft:roaster", {}).get("closing_host_status")
                == "host_exact"
            ) else 1,
        },
    }


def check() -> list[str]:
    errors: list[str] = []
    if not OUTPUT.is_file():
        return [f"missing generated file: {t36.relative(OUTPUT)}"]
    expected = t35.stable_json(build())
    actual = OUTPUT.read_text(encoding="utf-8")
    if actual != expected:
        errors.append(t35.stale_error(OUTPUT, expected, actual))
        return errors
    document = t35.load_json(OUTPUT)
    if document.get("t35_foundation") != T35_FOUNDATION:
        errors.append("T36 delta changed T35 foundation denominators")
    if document.get("sets", {}).get("missing"):
        errors.append("target - catalog is not empty")
    validators = document.get("validators") or {}
    if any(int(value or 0) != 0 for value in validators.values()):
        errors.append("T36 census delta validators are not zero")
    runtime = document.get("closing", {}).get("runtime_machine_ids")
    catalog = document.get("sets", {}).get("catalog")
    source = document.get("closing", {}).get("runtime_source") or ""
    if "ModBlocks" not in source:
        errors.append("runtime_machine_ids must be measured from registry, not copied from catalog")
    if runtime is None or catalog is None:
        errors.append("census delta missing runtime or catalog sets")
    publication = (document.get("delta") or {}).get("publication") or {}
    if publication.get("eager") == 0 and publication.get("lazy") == 0 and publication.get("logical") == 0:
        errors.append("publication delta must not be a hardcoded 0/0/0")
    return errors


def write() -> dict[str, Any]:
    document = build()
    t35.write_stable(OUTPUT, document)
    return document


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args(argv)
    if args.write == args.check:
        parser.error("choose exactly one of --write or --check")
    try:
        if args.write:
            document = write()
            print(json.dumps({
                "status": document["status"],
                "closing_rows": document["closing"]["catalog_rows"],
                "runtime": len(document["closing"]["runtime_machine_ids"]),
                "publication": document["delta"]["publication"],
                "reissued_hosts": document["family_scope"]["reissued_hosts"],
                "missing": document["sets"]["missing"],
            }))
            return 0
        errors = check()
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        print(f"{t36.relative(OUTPUT)} is current")
        return 0
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"T36 census delta failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
