#!/usr/bin/env python3
"""Build the T48 publication-delta ledger from generated families and group winners."""
from __future__ import annotations

import json
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from tools import t35_common as t35
from tools import t48_common as t48

BUILDER = Path(__file__).resolve()
OUTPUT = t48.PUBLICATION_DELTA


def _partition_rows(
    *,
    winner: str | None,
    blocked: bool,
    source_rows: int,
    eager_relation_count: int,
) -> tuple[int | None, int | None]:
    if blocked or winner is None:
        return None, None
    if winner == "immediate":
        return source_rows, 0
    if winner == "on_demand":
        return 0, source_rows
    if winner == "hybrid":
        return eager_relation_count, source_rows - eager_relation_count
    return None, None


def _group_status(winner: str | None, blocked: bool, group_id: str) -> dict[str, Any]:
    if blocked or winner is None:
        return {
            "id": None,
            "status": "pending",
            "verdict": t48.PENDING_LOAD_VERDICT,
            "reason": (
                "tools/t48_materialization_decision.json group winner is "
                "BLOCKED; eager/lazy/cache are not invented from the loader default."
            ),
            "publication_group": group_id,
        }
    return {
        "id": winner,
        "status": "derived",
        "verdict": None,
        "reason": "Derived by tools/build_t48_recipe_load_benchmark.py",
        "publication_group": group_id,
    }


def _family_ledgers(
    generated: list[Path],
    strategy: dict[str, Any],
) -> list[dict[str, Any]]:
    by_name = {path.name: path for path in generated}
    blocked = strategy["blocked"]
    rows: list[dict[str, Any]] = []
    for family in t48.production_families():
        template = str(family["template_key"])
        filename = template.replace(".", "_").replace("#", "_") + ".json"
        authored = by_name.get(filename)
        group_id = str(family["publication_group"])
        group_key = t48.PUBLICATION_GROUP_KEYS[group_id]
        winner = strategy["group_winners"].get(group_key)
        eager_ids = set(
            t48.hybrid_eager_stable_ids(group_key)
            if winner == "hybrid" and not blocked
            else []
        )
        stable_ids = [str(value) for value in (family.get("stable_ids") or [])]
        source_rows = len(stable_ids) or int(family.get("expanded_count") or 1)
        eager_relation_count = sum(1 for stable_id in stable_ids if stable_id in eager_ids)
        eager, lazy = _partition_rows(
            winner=winner,
            blocked=blocked,
            source_rows=source_rows,
            eager_relation_count=eager_relation_count,
        )
        rows.append({
            "family_id": family["family_id"],
            "template_key": template,
            "publication_group": group_id,
            "source_rows": source_rows,
            "authored_entries": 1 if authored is not None else 0,
            "logical_rows": source_rows,
            "eager_publication_rows": eager,
            "lazy_logical_rows": lazy,
            "strategy": winner or "pending",
            "measurement_pointer": "tools/t48_materialization_decision.json",
            "authored_path": t48.relative(authored) if authored is not None else None,
        })
    return rows


def _datapack_files(paths: list[Path]) -> list[str]:
    return [t48.relative(path) for path in paths if path.is_file()]


def _support_ledger() -> dict[str, Any]:
    support = (
        t35.load_json(t48.PLAYER_PATH_SUPPORT)
        if t48.PLAYER_PATH_SUPPORT.is_file()
        else {"route_count": 0, "routes": []}
    )
    lock_count = int(
        (t48.load_production_lock().get("support") or {}).get("route_count") or 0
    )
    recipes = t48.support_recipe_files()
    fluid_routes = [
        row
        for row in support.get("routes") or []
        if str(row.get("kind")) == "bath_gt_recipe"
    ]
    item_routes = [
        row
        for row in support.get("routes") or []
        if str(row.get("kind")) == "worldgen_drop"
    ]
    return {
        "authored": len(recipes),
        "eager": 0,
        "kind": support.get("kind") or t48.PLAYER_PATH_REAL_KIND,
        "route_count": int(support.get("route_count") or 0),
        "lock_route_count": lock_count,
        "fluid_route_count": len(fluid_routes),
        "item_route_count": len(item_routes),
        "source_map": support.get("source_map"),
        "recipe_paths": _datapack_files(recipes),
        "note": (
            "RecipeManager-loaded B1 support is the TIME Bath fluid recipes. "
            "Item scatter worldgen/tag files are listed under worldgen, not as "
            "compact family documents."
        ),
    }


def _identity_fluid_datapack() -> dict[str, Any]:
    files = _datapack_files(
        [
            t48.BUNDLED_IDENTITY_CATALOG,
            t48.BUNDLED_FLUID_MAPPING,
        ]
    )
    return {
        "kind": "identity_fluid_runtime_datapack",
        "file_count": len(files),
        "files": files,
        "note": (
            "Catalog and overlay JSON are runtime datapack, not RecipeManager "
            "compact family documents and not 118 extra recipes."
        ),
    }


def _worldgen_datapack() -> dict[str, Any]:
    files = _datapack_files(
        [
            t48.SCATTER_CONFIGURED,
            t48.SCATTER_PLACED,
            t48.SCATTER_BIOME_MODIFIER,
            t48.SCATTER_CATALOG,
            t48.SCATTER_ITEM_TAG,
        ]
    )
    return {
        "kind": "player_path_worldgen",
        "file_count": len(files),
        "files": files,
    }


def build() -> dict[str, Any]:
    generated = t48.generated_family_files()
    strategy = t48.production_strategy()
    families = _family_ledgers(generated, strategy)
    support = _support_ledger()
    identity_fluid = _identity_fluid_datapack()
    worldgen = _worldgen_datapack()
    authored_compact = sum(int(row["authored_entries"] or 0) for row in families)
    logical = sum(int(row["logical_rows"] or 0) for row in families)
    pending_strategy = strategy["blocked"]
    authored_policy = sum(
        1 for path in t48.PUBLICATION_POLICY_DATAPACK_FILES.values() if path.is_file()
    )
    publication_groups: dict[str, Any] = {}
    eager_total = 0 if not pending_strategy else None
    lazy_total = 0 if not pending_strategy else None
    for group_id, group_key in t48.PUBLICATION_GROUP_KEYS.items():
        partition = t48.partition_for_winner(
            strategy["group_winners"].get(group_key),
            group_key,
        )
        count = sum(1 for row in families if row["publication_group"] == group_id)
        group_logical = sum(
            int(row["logical_rows"] or 0)
            for row in families
            if row["publication_group"] == group_id
        )
        publication_groups[group_key] = {
            "publication_group": group_id,
            "family_count": count,
            "logical_rows": group_logical,
            "strategy": _group_status(
                strategy["group_winners"].get(group_key),
                pending_strategy,
                group_id,
            ),
            "partition": partition,
        }
        if not pending_strategy:
            eager_total += int(partition["eager_publication_rows"] or 0)
            lazy_total += int(partition["lazy_logical_rows"] or 0)
    card_partition = t48.partition_for_winner(
        strategy["card_aggregate_winner"],
        "card_aggregate",
    )
    cache = None if pending_strategy else int(card_partition["lazy_cache_ceiling_rows"] or 0)
    authored_datapack = authored_compact + authored_policy + int(support["authored"] or 0)
    card_level = {
        "lazy_cache_ceiling_rows": cache,
        "recorded_once": True,
        "copied_per_family": False,
        "averaged_into_family_rows": False,
        "note": (
            "Shared provider/cache costs are card-level. The card-aggregate "
            "cache ceiling is recorded once and is not copied across family ledgers."
        ),
        "pending": pending_strategy,
        "evidence": "tools/t48_materialization_decision.json#decision.card_aggregate_winner",
        "publication_groups": publication_groups,
    }
    support_ok = support["route_count"] == support["lock_route_count"] != 0
    ready = (
        authored_compact == t48.production_family_count()
        and logical == t48.production_relation_count()
        and authored_policy == 3
        and support_ok
        and int(support["authored"] or 0) == len(t48.support_recipe_files())
        and support.get("kind") == t48.PLAYER_PATH_REAL_KIND
        and not pending_strategy
    )
    return {
        "schema_version": 1,
        "status": (
            "T48_PUBLICATION_DELTA_READY" if ready else "T48_PUBLICATION_DELTA_BLOCKED"
        ),
        "source_revision": t48.SOURCE_REVISION,
        "production_lock_sha256": t48.production_lock_sha256(),
        "generated_by": "python tools/build_t48_publication_delta.py",
        "owner": t48.OWNER,
        "host": t48.HOST,
        "authored_compact_family_entries": authored_compact,
        "authored_publication_policy_entries": authored_policy,
        "authored_support_entries": support["authored"],
        "authored_datapack_entries": authored_datapack,
        "logical": logical,
        "eager": eager_total,
        "lazy": lazy_total,
        "production_winner": "group_scoped",
        "measured_card_aggregate_winner": strategy["card_aggregate_winner"],
        "group_winners": strategy["group_winners"],
        "production_winner_pending": pending_strategy,
        "card_level_shared_costs": card_level,
        "families": families,
        "family_count": len(families),
        "generated_file_count": len(generated),
        "support_recipes": support,
        "identity_fluid_datapack": identity_fluid,
        "player_path_worldgen": worldgen,
        "vanilla_datapack_entries": 0,
        "note": (
            "Authored RecipeManager JSON is compact families + three publication "
            "policies + real B1 Bath support recipes. Identity/fluid catalogs and "
            "item scatter worldgen are listed separately and are not compact families. "
            f"Closing authored = {t48.OPENING_AUTHORED_ENTRIES} + actual recipe delta."
        ),
        "validators": {
            "authored_matches_lock": (
                0 if authored_compact == t48.production_family_count() else 1
            ),
            "logical_matches_lock": (
                0 if logical == t48.production_relation_count() else 1
            ),
            "family_ledger_matches_lock": (
                0 if len(families) == t48.production_family_count() else 1
            ),
            "support_present": 0 if support_ok else 1,
            "policy_present": 0 if authored_policy == 3 else 1,
            "cache_not_copied_per_family": 0 if card_level["recorded_once"] else 1,
            "pending_strategy_not_zero_filled": 0 if (
                not pending_strategy
                or (eager_total is None and lazy_total is None and cache is None)
            ) else 1,
        },
    }


def write() -> dict[str, Any]:
    document = build()
    t35.write_stable(OUTPUT, document)
    return document


def check() -> list[str]:
    errors = t48.check_document(OUTPUT, build())
    if errors:
        return errors
    document = t35.load_json(OUTPUT)
    validators = document.get("validators") or {}
    if any(int(validators.get(key) or 0) != 0 for key in validators):
        errors.append("T48 publication delta structural validators are not zero")
    if document.get("authored_compact_family_entries") != t48.production_family_count():
        errors.append("authored compact family entries do not match production lock")
    if document.get("logical") != t48.production_relation_count():
        errors.append("logical publication does not match production lock")
    if document.get("production_winner_pending"):
        if document.get("eager") is not None or document.get("lazy") is not None:
            errors.append("blocked winner must not zero-fill eager/lazy")
    return errors


def main(argv: list[str] | None = None) -> int:
    args = t48.parse_write_check(__doc__, argv)
    try:
        if args.write:
            write()
            print(f"Wrote {t48.relative(OUTPUT)}")
            return 0
        errors = check()
        if errors:
            print("T48 publication delta is stale:")
            for error in errors:
                print(f"- {error}")
            return 1
        print("T48 publication delta is current.")
        return 0
    except ValueError as failure:
        print(str(failure), file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
