#!/usr/bin/env python3
"""Build the T45 publication-delta ledger from generated families and group winners."""
from __future__ import annotations

import json
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from tools import t35_common as t35
from tools import t45_common as t45

BUILDER = Path(__file__).resolve()
OUTPUT = t45.PUBLICATION_DELTA


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
            "verdict": t45.PENDING_LOAD_VERDICT,
            "reason": (
                "tools/t45_materialization_decision.json group winner is "
                "BLOCKED; eager/lazy/cache are not invented from the loader default."
            ),
            "publication_group": group_id,
        }
    return {
        "id": winner,
        "status": "derived",
        "verdict": None,
        "reason": "Derived by tools/build_t45_recipe_load_benchmark.py",
        "publication_group": group_id,
    }


def _family_ledgers(
    generated: list[Path],
    strategy: dict[str, Any],
) -> list[dict[str, Any]]:
    by_name = {path.name: path for path in generated}
    blocked = strategy["blocked"]
    rows: list[dict[str, Any]] = []
    for family in t45.production_families():
        template = str(family["template_key"])
        filename = template.replace(".", "_").replace("#", "_") + ".json"
        authored = by_name.get(filename)
        group_id = str(family["publication_group"])
        group_key = t45.PUBLICATION_GROUP_KEYS[group_id]
        winner = strategy["group_winners"].get(group_key)
        eager_ids = set(
            t45.hybrid_eager_stable_ids(group_key)
            if winner == "hybrid" and not blocked
            else []
        )
        eager, lazy = _partition_rows(
            winner=winner,
            blocked=blocked,
            source_rows=1,
            eager_relation_count=1 if family["stable_id"] in eager_ids else 0,
        )
        rows.append({
            "family_id": family["family_id"],
            "template_key": template,
            "publication_group": group_id,
            "source_rows": 1,
            "authored_entries": 1 if authored is not None else 0,
            "logical_rows": 1,
            "eager_publication_rows": eager,
            "lazy_logical_rows": lazy,
            "strategy": winner or "pending",
            "measurement_pointer": "tools/t45_materialization_decision.json",
            "authored_path": t45.relative(authored) if authored is not None else None,
        })
    return rows


def _support_ledger() -> dict[str, Any]:
    support = (
        t35.load_json(t45.PLAYER_PATH_SUPPORT)
        if t45.PLAYER_PATH_SUPPORT.is_file()
        else {"route_count": 0, "routes": []}
    )
    lock_count = int(
        (t45.load_production_lock().get("support") or {}).get("route_count") or 0
    )
    return {
        "authored": 0,
        "eager": 0,
        "kind": support.get("kind") or "worldgen_drop",
        "route_count": int(support.get("route_count") or 0),
        "lock_route_count": lock_count,
        "source_map": support.get("source_map"),
        "note": (
            "B1 acquisition is catalog-backed worldgen scatter, not vanilla or GT "
            "support recipes. Worldgen JSON lives in main resources and is not "
            "copied into T14 authored recipe counts."
        ),
    }


def build() -> dict[str, Any]:
    generated = t45.generated_family_files()
    strategy = t45.production_strategy()
    families = _family_ledgers(generated, strategy)
    support = _support_ledger()
    authored_compact = sum(int(row["authored_entries"] or 0) for row in families)
    logical = sum(int(row["logical_rows"] or 0) for row in families)
    pending_strategy = strategy["blocked"]
    authored_policy = sum(
        1 for path in t45.PUBLICATION_POLICY_DATAPACK_FILES.values() if path.is_file()
    )
    publication_groups: dict[str, Any] = {}
    eager_total = 0 if not pending_strategy else None
    lazy_total = 0 if not pending_strategy else None
    for group_id, group_key in t45.PUBLICATION_GROUP_KEYS.items():
        partition = t45.partition_for_winner(
            strategy["group_winners"].get(group_key),
            group_key,
        )
        count = sum(1 for row in families if row["publication_group"] == group_id)
        publication_groups[group_key] = {
            "publication_group": group_id,
            "family_count": count,
            "logical_rows": count,
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
    card_partition = t45.partition_for_winner(
        strategy["card_aggregate_winner"],
        "card",
    )
    cache = None if pending_strategy else int(card_partition["lazy_cache_ceiling_rows"] or 0)
    authored_datapack = authored_compact + authored_policy
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
        "evidence": "tools/t45_materialization_decision.json#decision.card_aggregate_winner",
        "publication_groups": publication_groups,
    }
    support_ok = support["route_count"] == support["lock_route_count"] != 0
    ready = (
        authored_compact == t45.production_family_count()
        and logical == t45.production_relation_count()
        and authored_policy == 2
        and support_ok
        and not pending_strategy
    )
    return {
        "schema_version": 1,
        "status": (
            "T45_PUBLICATION_DELTA_READY" if ready else "T45_PUBLICATION_DELTA_BLOCKED"
        ),
        "source_revision": t45.SOURCE_REVISION,
        "production_lock_sha256": t45.production_lock_sha256(),
        "generated_by": "python tools/build_t45_publication_delta.py",
        "owner": t45.OWNER,
        "host": t45.HOST,
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
        "note": (
            "Authored compact entries and logical rows match the 379 production lock. "
            "T14 authored datapack also includes two compact_publication_policy "
            "manifests. Worldgen scatter is B1 support, not a recipe datapack delta."
        ),
        "validators": {
            "authored_matches_lock": (
                0 if authored_compact == t45.production_family_count() else 1
            ),
            "logical_matches_lock": (
                0 if logical == t45.production_relation_count() else 1
            ),
            "family_ledger_matches_lock": (
                0 if len(families) == t45.production_family_count() else 1
            ),
            "support_present": 0 if support_ok else 1,
            "policy_present": 0 if authored_policy == 2 else 1,
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
    errors = t45.check_document(OUTPUT, build())
    if errors:
        return errors
    document = t35.load_json(OUTPUT)
    validators = document.get("validators") or {}
    if any(int(validators.get(key) or 0) != 0 for key in validators):
        errors.append("T45 publication delta structural validators are not zero")
    if document.get("authored_compact_family_entries") != t45.production_family_count():
        errors.append("authored compact family entries do not match production lock")
    if document.get("logical") != t45.production_relation_count():
        errors.append("logical publication does not match production lock")
    if document.get("production_winner_pending"):
        if document.get("eager") is not None or document.get("lazy") is not None:
            errors.append("blocked winner must not zero-fill eager/lazy")
    return errors


def main(argv: list[str] | None = None) -> int:
    args = t45.parse_write_check(__doc__, argv)
    try:
        if args.write:
            write()
            print(f"Wrote {t45.relative(OUTPUT)}")
            return 0
        errors = check()
        if errors:
            print("T45 publication delta is stale:")
            for error in errors:
                print(f"- {error}")
            return 1
        print("T45 publication delta is current.")
        return 0
    except ValueError as failure:
        print(str(failure), file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
