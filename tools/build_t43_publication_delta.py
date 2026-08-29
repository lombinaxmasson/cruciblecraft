#!/usr/bin/env python3
"""Build the T43 publication-delta ledger from generated families and the stone winner."""
from __future__ import annotations

import json
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from tools import t35_common as t35  # noqa: E402
from tools import t43_common as t43  # noqa: E402

BUILDER = Path(__file__).resolve()
OUTPUT = t43.PUBLICATION_DELTA


def _family_number(template_key: str) -> int:
    return int(template_key.rsplit("#", 1)[-1])


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


def _group_status(winner: str | None, blocked: bool) -> dict[str, Any]:
    if blocked or winner is None:
        return {
            "id": None,
            "status": "pending",
            "verdict": t43.PENDING_LOAD_VERDICT,
            "reason": (
                "tools/t43_materialization_decision.json group winner is "
                "BLOCKED; eager/lazy/cache are not invented from the loader default."
            ),
            "publication_group": t43.STONE_GROUP,
        }
    return {
        "id": winner,
        "status": "derived",
        "verdict": None,
        "reason": "Derived by tools/build_t43_recipe_load_benchmark.py",
        "publication_group": t43.STONE_GROUP,
    }


def _family_ledgers(
    source: dict[str, Any],
    generated: list[Path],
    strategy: dict[str, Any],
) -> list[dict[str, Any]]:
    by_template = {path.name: path for path in generated}
    grouped: dict[str, list[dict[str, Any]]] = {}
    production_templates = set(t43.production_template_keys())
    for relation in source.get("relations") or []:
        template = str(relation.get("template_key") or "")
        if template not in production_templates:
            continue
        grouped.setdefault(template, []).append(relation)
    winner = strategy["group_winners"].get("stone")
    blocked = strategy["blocked"] or winner is None
    eager_ids = set(
        t43.hybrid_eager_stable_ids("stone")
        if winner == "hybrid" and not blocked
        else []
    )
    rows: list[dict[str, Any]] = []
    for template in sorted(grouped.keys(), key=_family_number):
        relations = grouped[template]
        family_id = str(relations[0].get("family_id") or "")
        number = template.rsplit("#", 1)[-1]
        filename = f"gt_recipe_smelter_{number}.json"
        authored = by_template.get(filename)
        source_rows = len(relations)
        eager_relations = sum(
            1
            for relation in relations
            if str(relation.get("stable_id") or "") in eager_ids
        )
        eager, lazy = _partition_rows(
            winner=winner,
            blocked=blocked,
            source_rows=source_rows,
            eager_relation_count=eager_relations,
        )
        rows.append({
            "family_id": family_id,
            "template_key": template,
            "publication_group": t43.STONE_GROUP,
            "source_rows": source_rows,
            "authored_entries": 1 if authored is not None else 0,
            "logical_rows": source_rows,
            "eager_publication_rows": eager,
            "lazy_logical_rows": lazy,
            "strategy": winner or "pending",
            "measurement_pointer": "tools/t43_materialization_decision.json",
            "authored_path": t43.relative(authored) if authored is not None else None,
        })
    return rows


def _support_ledger() -> dict[str, Any]:
    support = (
        t35.load_json(t43.PLAYER_PATH_SUPPORT)
        if t43.PLAYER_PATH_SUPPORT.is_file()
        else {"route_count": 0, "routes": []}
    )
    lock_count = int(
        (t43.load_production_lock().get("support") or {}).get("route_count") or 0
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
    source = t35.load_json(t43.SOURCE)
    generated = t43.generated_family_files()
    strategy = t43.production_strategy()
    families = _family_ledgers(source, generated, strategy)
    support = _support_ledger()
    authored_compact = sum(int(row["authored_entries"] or 0) for row in families)
    logical = sum(int(row["logical_rows"] or 0) for row in families)
    pending_strategy = strategy["blocked"]
    stone_partition = t43.partition_for_winner(
        strategy["group_winners"].get("stone"),
        "stone",
    )
    policy_present = t43.PUBLICATION_POLICY_DATAPACK_FILE.is_file()
    authored_policy = 1 if policy_present else 0
    if pending_strategy:
        eager = None
        lazy = None
        cache = None
    else:
        eager = int(stone_partition["eager_publication_rows"] or 0)
        lazy = int(stone_partition["lazy_logical_rows"] or 0)
        cache = int(stone_partition["lazy_cache_ceiling_rows"] or 0)
    authored_datapack = authored_compact + authored_policy
    card_level = {
        "lazy_cache_ceiling_rows": cache,
        "recorded_once": True,
        "copied_per_family": False,
        "averaged_into_family_rows": False,
        "note": (
            "Shared provider/cache costs are card-level. The single stone-group "
            "cache ceiling is recorded once and is not copied across 407 family ledgers."
        ),
        "pending": pending_strategy,
        "evidence": "tools/t43_materialization_decision.json#decision.card_aggregate_winner",
        "publication_groups": {
            "stone": {
                "publication_group": t43.STONE_GROUP,
                "family_count": t43.PRODUCTION_FAMILY_COUNT,
                "logical_rows": t43.PRODUCTION_RELATION_COUNT,
                "strategy": _group_status(
                    strategy["group_winners"].get("stone"),
                    pending_strategy,
                ),
                "partition": stone_partition,
            }
        },
    }
    support_ok = support["route_count"] == support["lock_route_count"] == 406
    ready = (
        authored_compact == t43.production_family_count()
        and logical == t43.production_relation_count()
        and authored_policy == 1
        and support_ok
        and not pending_strategy
    )
    return {
        "schema_version": 1,
        "status": (
            "T43_PUBLICATION_DELTA_READY" if ready else "T43_PUBLICATION_DELTA_BLOCKED"
        ),
        "source_revision": t43.SOURCE_REVISION,
        "production_lock_sha256": t43.production_lock_sha256(),
        "generated_by": "python tools/build_t43_publication_delta.py",
        "owner": t43.OWNER,
        "host": t43.HOST,
        "authored_compact_family_entries": authored_compact,
        "authored_publication_policy_entries": authored_policy,
        "authored_support_entries": support["authored"],
        "authored_datapack_entries": authored_datapack,
        "logical": logical,
        "eager": eager,
        "lazy": lazy,
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
            "Authored compact entries and logical rows match the 407 production lock. "
            "T14 authored datapack also includes the one compact_publication_policy "
            "manifest. Worldgen scatter is B1 support, not a recipe datapack delta."
        ),
        "validators": {
            "authored_matches_lock": (
                0 if authored_compact == t43.production_family_count() else 1
            ),
            "logical_matches_lock": (
                0 if logical == t43.production_relation_count() else 1
            ),
            "family_ledger_matches_lock": (
                0 if len(families) == t43.production_family_count() else 1
            ),
            "support_present": 0 if support_ok else 1,
            "policy_present": 0 if authored_policy == 1 else 1,
            "cache_not_copied_per_family": 0 if card_level["recorded_once"] else 1,
            "pending_strategy_not_zero_filled": 0 if (
                not pending_strategy
                or (eager is None and lazy is None and cache is None)
            ) else 1,
            "group_winners_not_t37_t38_partitions": 0 if (
                stone_partition != {
                    "eager_publication_rows": 14,
                    "lazy_logical_rows": 36,
                    "lazy_cache_ceiling_rows": 8,
                }
                and stone_partition != {
                    "eager_publication_rows": 0,
                    "lazy_logical_rows": 73,
                    "lazy_cache_ceiling_rows": 16,
                }
            ) else 1,
        },
    }


def write() -> dict[str, Any]:
    document = build()
    t35.write_stable(OUTPUT, document)
    return document


def check() -> list[str]:
    errors = t43.check_document(OUTPUT, build())
    if errors:
        return errors
    document = t35.load_json(OUTPUT)
    validators = document.get("validators") or {}
    structural = (
        "authored_matches_lock",
        "logical_matches_lock",
        "family_ledger_matches_lock",
        "support_present",
        "policy_present",
        "cache_not_copied_per_family",
        "pending_strategy_not_zero_filled",
        "group_winners_not_t37_t38_partitions",
    )
    if any(int(validators.get(key) or 0) != 0 for key in structural):
        errors.append("T43 publication delta structural validators are not zero")
    if document.get("authored_compact_family_entries") != t43.production_family_count():
        errors.append("authored compact family entries do not match production lock")
    if document.get("logical") != t43.production_relation_count():
        errors.append("logical publication does not match production lock")
    if document.get("production_winner_pending"):
        if document.get("eager") is not None or document.get("lazy") is not None:
            errors.append("blocked winner must not zero-fill eager/lazy")
    return errors


def main(argv: list[str] | None = None) -> int:
    args = t43.parse_write_check(__doc__, argv)
    try:
        if args.write:
            document = write()
            print(json.dumps({
                "status": document["status"],
                "authored_compact": document["authored_compact_family_entries"],
                "authored_datapack": document["authored_datapack_entries"],
                "logical": document["logical"],
                "eager": document["eager"],
                "lazy": document["lazy"],
                "group_winners": document["group_winners"],
            }))
            return 0
        errors = check()
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        print(f"{t43.relative(OUTPUT)} is current")
        return 0
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"T43 publication delta failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
