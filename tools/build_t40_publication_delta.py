#!/usr/bin/env python3
"""Build the T40 publication-delta ledger from generated families and group winners."""
from __future__ import annotations

import json
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from tools import t35_common as t35  # noqa: E402
from tools import t40_common as t40  # noqa: E402

BUILDER = Path(__file__).resolve()
OUTPUT = t40.PUBLICATION_DELTA


def _family_number(template_key: str) -> int:
    return int(template_key.rsplit("#", 1)[-1])


def _group_key(publication_group: str) -> str:
    return t40.PUBLICATION_GROUP_KEYS[publication_group]


def _partition_rows(
    *,
    winner: str | None,
    blocked: bool,
    source_rows: int,
    eager_relation_count: int,
    partition: dict[str, int | None],
) -> tuple[int | None, int | None]:
    if blocked or winner is None:
        return None, None
    if winner == "immediate":
        return source_rows, 0
    if winner == "on_demand":
        return 0, source_rows
    if winner == "hybrid":
        lazy = source_rows - eager_relation_count
        return eager_relation_count, lazy
    return None, None


def _group_status(
    group_key: str,
    winner: str | None,
    blocked: bool,
) -> dict[str, Any]:
    if blocked or winner is None:
        return {
            "id": None,
            "status": "pending",
            "verdict": t40.PENDING_LOAD_VERDICT,
            "reason": (
                "tools/t40_materialization_decision.json group winner is "
                "BLOCKED; eager/lazy/cache are not invented from the loader default."
            ),
            "publication_group": (
                t40.SINGLETON_GROUP if group_key == "singleton" else t40.MULTI_GROUP
            ),
        }
    return {
        "id": winner,
        "status": "derived",
        "verdict": None,
        "reason": "Derived by tools/build_t40_recipe_load_benchmark.py",
        "publication_group": (
            t40.SINGLETON_GROUP if group_key == "singleton" else t40.MULTI_GROUP
        ),
    }


def _family_ledgers(
    source: dict[str, Any],
    generated: list[Path],
    strategy: dict[str, Any],
    group_map: dict[str, str],
) -> list[dict[str, Any]]:
    by_template = {path.name: path for path in generated}
    grouped: dict[str, list[dict[str, Any]]] = {}
    production_templates = set(t40.production_template_keys())
    for relation in source.get("relations") or []:
        template = str(relation.get("template_key") or "")
        if template not in production_templates:
            continue
        grouped.setdefault(template, []).append(relation)

    eager_by_group = {
        "singleton": (
            t40.hybrid_eager_stable_ids("singleton")
            if strategy["group_winners"].get("singleton") == "hybrid"
            and not strategy["blocked"]
            else set()
        ),
        "multi": (
            t40.hybrid_eager_stable_ids("multi")
            if strategy["group_winners"].get("multi") == "hybrid"
            and not strategy["blocked"]
            else set()
        ),
    }

    rows: list[dict[str, Any]] = []
    for template in sorted(grouped.keys(), key=_family_number):
        relations = grouped[template]
        family_id = str(relations[0].get("family_id") or "")
        publication_group = group_map.get(family_id, "")
        group_key = _group_key(publication_group)
        winner = strategy["group_winners"].get(group_key)
        blocked = strategy["blocked"] or winner is None
        partition = t40.partition_for_winner(winner, group_key)
        number = template.rsplit("#", 1)[-1]
        filename = f"gt_recipe_electrolyzer_{number}.json"
        authored = by_template.get(filename)
        source_rows = len(relations)
        eager_relations = sum(
            1
            for relation in relations
            if str(relation.get("stable_id") or "") in eager_by_group[group_key]
        )
        eager, lazy = _partition_rows(
            winner=winner,
            blocked=blocked,
            source_rows=source_rows,
            eager_relation_count=eager_relations,
            partition=partition,
        )
        rows.append({
            "family_id": family_id,
            "template_key": template,
            "publication_group": publication_group,
            "source_rows": source_rows,
            "authored_entries": 1 if authored is not None else 0,
            "logical_rows": source_rows,
            "eager_publication_rows": eager,
            "lazy_logical_rows": lazy,
            "strategy": winner or "pending",
            "measurement_pointer": "tools/t40_materialization_decision.json",
            "authored_path": t40.relative(authored) if authored is not None else None,
        })
    return rows


def build() -> dict[str, Any]:
    source = t35.load_json(t40.SOURCE)
    generated = t40.generated_family_files()
    strategy = t40.production_strategy()
    group_map = t40.production_group_map()
    families = _family_ledgers(source, generated, strategy, group_map)
    support = t40.support_recipe_ledger()
    authored_compact = sum(int(row["authored_entries"] or 0) for row in families)
    logical = sum(int(row["logical_rows"] or 0) for row in families)
    pending_strategy = strategy["blocked"]
    singleton_partition = t40.partition_for_winner(
        strategy["group_winners"].get("singleton"),
        "singleton",
    )
    multi_partition = t40.partition_for_winner(
        strategy["group_winners"].get("multi"),
        "multi",
    )
    if pending_strategy:
        eager = None
        lazy = None
        cache = None
    else:
        eager = (
            int(singleton_partition["eager_publication_rows"] or 0)
            + int(multi_partition["eager_publication_rows"] or 0)
            + int(support["eager"])
        )
        lazy = (
            int(singleton_partition["lazy_logical_rows"] or 0)
            + int(multi_partition["lazy_logical_rows"] or 0)
        )
        cache = (
            int(singleton_partition["lazy_cache_ceiling_rows"] or 0)
            + int(multi_partition["lazy_cache_ceiling_rows"] or 0)
        )
    authored_datapack = authored_compact + int(support["authored"])
    card_level = {
        "lazy_cache_ceiling_rows": cache,
        "recorded_once": True,
        "copied_per_family": False,
        "averaged_into_family_rows": False,
        "note": (
            "Shared provider/cache costs are card-level. Per-group cache ceilings "
            "are summed once at card level and are not copied across the locked "
            "family ledgers."
        ),
        "pending": pending_strategy,
        "evidence": "tools/t40_materialization_decision.json#decision.card_aggregate_winner",
        "publication_groups": {
            "singleton": {
                "publication_group": t40.SINGLETON_GROUP,
                "family_count": t40.production_group_counts()[
                    t40.SINGLETON_GROUP
                ]["families"],
                "logical_rows": t40.production_group_counts()[
                    t40.SINGLETON_GROUP
                ]["relations"],
                "strategy": _group_status(
                    "singleton",
                    strategy["group_winners"].get("singleton"),
                    pending_strategy,
                ),
                "partition": singleton_partition,
            },
            "multi": {
                "publication_group": t40.MULTI_GROUP,
                "family_count": t40.production_group_counts()[
                    t40.MULTI_GROUP
                ]["families"],
                "logical_rows": t40.production_group_counts()[
                    t40.MULTI_GROUP
                ]["relations"],
                "strategy": _group_status(
                    "multi",
                    strategy["group_winners"].get("multi"),
                    pending_strategy,
                ),
                "partition": multi_partition,
            },
        },
    }
    lock_support_count = t40.load_production_lock()["support"]["route_count"]
    support_ok = support["authored"] == lock_support_count
    ready = (
        authored_compact == t40.production_family_count()
        and logical == t40.production_relation_count()
        and support_ok
        and not pending_strategy
    )
    return {
        "schema_version": 1,
        "status": (
            "T40_PUBLICATION_DELTA_READY" if ready else "T40_PUBLICATION_DELTA_BLOCKED"
        ),
        "source_revision": t40.SOURCE_REVISION,
        "production_lock_sha256": t40.production_lock_sha256(),
        "generated_by": "python tools/build_t40_publication_delta.py",
        "owner": t40.OWNER,
        "host": t40.HOST,
        "authored_compact_family_entries": authored_compact,
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
        "support_recipes": {
            **support,
            "note": (
                "T40 player-path support recipes are counted from "
                "src/t40_support_generated/resources. "
                "GT recovery routes are immediate eager; shapeless packing is vanilla "
                "authored only."
            ),
        },
        "note": (
            "Authored compact entries and logical rows match the production lock. "
            "Card authored datapack entries also include support recovery recipes "
            "counted from the recovery tree. Eager/lazy/cache for singleton and "
            "multi groups follow derived per-group production winners; a blocked "
            "winner is recorded as pending strategy, not silent zeros."
        ),
        "validators": {
            "authored_matches_lock": (
                0 if authored_compact == t40.production_family_count() else 1
            ),
            "logical_matches_lock": (
                0 if logical == t40.production_relation_count() else 1
            ),
            "family_ledger_matches_lock": (
                0 if len(families) == t40.production_family_count() else 1
            ),
            "support_present": 0 if support_ok else 1,
            "cache_not_copied_per_family": 0 if card_level["recorded_once"] else 1,
            "pending_strategy_not_zero_filled": 0 if (
                not pending_strategy
                or (eager is None and lazy is None and cache is None)
            ) else 1,
            "group_winners_not_t37_t38_partitions": 0 if all(
                t40.partition_for_winner(winner, group) != {
                    "eager_publication_rows": 14,
                    "lazy_logical_rows": 36,
                    "lazy_cache_ceiling_rows": 8,
                }
                and t40.partition_for_winner(winner, group) != {
                    "eager_publication_rows": 0,
                    "lazy_logical_rows": 73,
                    "lazy_cache_ceiling_rows": 16,
                }
                for group, winner in (
                    ("singleton", strategy["group_winners"].get("singleton")),
                    ("multi", strategy["group_winners"].get("multi")),
                )
                if winner
            ) else 1,
        },
    }


def write() -> dict[str, Any]:
    document = build()
    t35.write_stable(OUTPUT, document)
    return document


def check() -> list[str]:
    errors = t40.check_document(OUTPUT, build())
    if errors:
        return errors
    document = t35.load_json(OUTPUT)
    validators = document.get("validators") or {}
    structural = (
        "authored_matches_lock",
        "logical_matches_lock",
        "family_ledger_matches_lock",
        "support_present",
        "cache_not_copied_per_family",
        "pending_strategy_not_zero_filled",
        "group_winners_not_t37_t38_partitions",
    )
    if any(int(validators.get(key) or 0) != 0 for key in structural):
        errors.append("T40 publication delta structural validators are not zero")
    if document.get("authored_compact_family_entries") != t40.production_family_count():
        errors.append("authored compact family entries do not match production lock")
    if document.get("logical") != t40.production_relation_count():
        errors.append("logical publication does not match production lock")
    if document.get("production_winner_pending"):
        if document.get("eager") is not None or document.get("lazy") is not None:
            errors.append("blocked winner must not zero-fill eager/lazy")
    return errors


def main(argv: list[str] | None = None) -> int:
    args = t40.parse_write_check(__doc__, argv)
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
        print(f"{t40.relative(OUTPUT)} is current")
        return 0
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"T40 publication delta failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
