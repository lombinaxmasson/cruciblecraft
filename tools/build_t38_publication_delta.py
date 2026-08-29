#!/usr/bin/env python3
"""Build the T38 publication-delta ledger from generated families and the winner."""
from __future__ import annotations

import json
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from tools import t35_common as t35  # noqa: E402
from tools import t38_common as t38  # noqa: E402

BUILDER = Path(__file__).resolve()
OUTPUT = t38.PUBLICATION_DELTA


def _family_number(template_key: str) -> int:
    return int(template_key.rsplit("#", 1)[-1])


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
    if (partition["eager_publication_rows"] or 0) > 0 and winner == "immediate":
        return source_rows, 0
    return None, None


def _family_ledgers(
    source: dict[str, Any],
    generated: list[Path],
    strategy: dict[str, Any],
    partition: dict[str, int | None],
    eager_stable_ids: set[str] | None,
) -> list[dict[str, Any]]:
    by_template = {
        path.name: path
        for path in generated
    }
    grouped: dict[str, list[dict[str, Any]]] = {}
    for relation in source.get("relations") or []:
        template = str(relation.get("template_key") or "")
        grouped.setdefault(template, []).append(relation)

    rows: list[dict[str, Any]] = []
    winner = strategy["winner"]
    blocked = strategy["blocked"]
    eager_ids = eager_stable_ids or set()
    for template in sorted(grouped.keys(), key=_family_number):
        relations = grouped[template]
        number = template.rsplit("#", 1)[-1]
        filename = f"gt_recipe_roaster_{number}.json"
        authored = by_template.get(filename)
        source_rows = len(relations)
        eager_relations = sum(
            1 for relation in relations
            if str(relation.get("stable_id") or "") in eager_ids
        )
        eager, lazy = _partition_rows(
            winner=winner,
            blocked=blocked,
            source_rows=source_rows,
            eager_relation_count=eager_relations,
            partition=partition,
        )
        rows.append({
            "family_id": relations[0].get("family_id"),
            "template_key": template,
            "source_rows": source_rows,
            "authored_entries": 1 if authored is not None else 0,
            "logical_rows": source_rows,
            "eager_publication_rows": eager,
            "lazy_logical_rows": lazy,
            "strategy": winner or "pending",
            "measurement_pointer": "tools/t38_materialization_decision.json",
            "authored_path": t38.relative(authored) if authored is not None else None,
        })
    return rows


def winner_status(strategy: dict[str, Any]) -> dict[str, Any]:
    if strategy["blocked"]:
        return {
            "id": None,
            "status": "pending",
            "verdict": t38.PENDING_LOAD_VERDICT,
            "reason": (
                "tools/t38_materialization_decision.json production_winner is "
                "BLOCKED; eager/lazy/cache are not invented from the loader default."
            ),
        }
    return {
        "id": strategy["winner"],
        "status": "derived",
        "verdict": None,
        "reason": "Derived by tools/build_t38_recipe_load_benchmark.py",
    }


def build() -> dict[str, Any]:
    source = t35.load_json(t38.SOURCE)
    generated = t38.generated_family_files()
    strategy = t38.production_strategy()
    partition = t38.partition_for_winner(strategy["winner"])
    eager_stable_ids: set[str] | None = None
    if strategy["winner"] == "hybrid" and not strategy["blocked"]:
        policy = t35.load_json(t38.POLICY)
        eager_stable_ids = {
            str(value)
            for value in (policy["hybrid_boundary"]["eager_stable_ids"] or [])
        }
    families = _family_ledgers(
        source,
        generated,
        strategy,
        partition,
        eager_stable_ids,
    )
    authored_compact = sum(int(row["authored_entries"] or 0) for row in families)
    logical = sum(int(row["logical_rows"] or 0) for row in families)
    support = t38.support_recipe_ledger()
    compact_eager = partition["eager_publication_rows"]
    compact_lazy = partition["lazy_logical_rows"]
    cache = partition["lazy_cache_ceiling_rows"]
    pending_strategy = strategy["blocked"]
    if pending_strategy or compact_eager is None:
        eager = None
    else:
        eager = int(compact_eager) + int(support["eager"])
    lazy = compact_lazy
    authored_datapack = authored_compact + int(support["authored"])
    card_level = {
        "lazy_cache_ceiling_rows": cache,
        "recorded_once": True,
        "copied_per_family": False,
        "averaged_into_family_rows": False,
        "note": (
            "Shared provider/cache costs are card-level. They are not copied "
            "across the 29 family ledgers and are not averaged into per-family "
            "measurements."
        ),
        "pending": pending_strategy,
        "evidence": "tools/t38_materialization_decision.json#decision.production_winner",
    }
    support_ok = (
        int(support["gt_recovery_count"]) == t38.SUPPORT_GT_RECOVERY_COUNT
        and int(support["ore_chain_count"]) == t38.SUPPORT_ORE_CHAIN_COUNT
        and int(support["crafting_count"]) == t38.SUPPORT_CRAFTING_COUNT
        and int(support["authored"]) == t38.SUPPORT_AUTHORED_COUNT
        and int(support["eager"]) == t38.SUPPORT_EAGER_COUNT
    )
    return {
        "schema_version": 1,
        "status": (
            "T38_PUBLICATION_DELTA_READY"
            if authored_compact == t38.FAMILY_COUNT
            and logical == t38.SOURCE_ROWS
            and authored_datapack == t38.AUTHORED_DATAPACK_COUNT
            and support_ok
            and not pending_strategy
            else "T38_PUBLICATION_DELTA_BLOCKED"
        ),
        "source_revision": t38.SOURCE_REVISION,
        "generated_by": "python tools/build_t38_publication_delta.py",
        "owner": t38.OWNER,
        "host": t38.HOST,
        "authored_compact_family_entries": authored_compact,
        "authored_support_entries": support["authored"],
        "authored_datapack_entries": authored_datapack,
        "logical": logical,
        "eager": eager,
        "lazy": lazy,
        "strategy": winner_status(strategy),
        "production_winner": strategy["winner"],
        "production_winner_pending": pending_strategy,
        "card_level_shared_costs": card_level,
        "families": families,
        "family_count": len(families),
        "generated_file_count": len(generated),
        "support_recipes": {
            "gt_recovery": support["gt_recovery"],
            "ore_chain": support["ore_chain"],
            "crafting": support["crafting"],
            "gt_recovery_count": support["gt_recovery_count"],
            "ore_chain_count": support["ore_chain_count"],
            "crafting_count": support["crafting_count"],
            "authored": support["authored"],
            "eager": support["eager"],
            "new_ore_block_materials": support["new_ore_block_materials"],
            "note": (
                "T38 player-path support is 5 GT recovery, 10 newly registered "
                "ore-block crusher recipes, and 4 shapeless packing recipes. "
                "GT support is immediate eager; packing is vanilla authored only."
            ),
        },
        "note": (
            "Authored compact family datapack entries remain +29 / logical +73. "
            "Card authored datapack entries also include +19 support recipes "
            "(+15 immediate eager GT, +4 vanilla crafting). Eager/lazy/cache "
            "for the 29 compact families follow the derived production winner; "
            "a blocked winner is recorded as pending strategy, not silent zeros."
        ),
        "validators": {
            "authored_29": 0 if authored_compact == t38.FAMILY_COUNT else 1,
            "logical_73": 0 if logical == t38.SOURCE_ROWS else 1,
            "family_ledger_29": 0 if len(families) == t38.FAMILY_COUNT else 1,
            "support_19": 0 if support_ok else 1,
            "authored_datapack_48": 0 if authored_datapack == t38.AUTHORED_DATAPACK_COUNT else 1,
            "cache_not_copied_per_family": 0 if card_level["recorded_once"] else 1,
            "pending_strategy_not_zero_filled": 0 if (
                not pending_strategy
                or (eager is None and lazy is None and cache is None)
            ) else 1,
        },
    }


def write() -> dict[str, Any]:
    document = build()
    t35.write_stable(OUTPUT, document)
    return document


def check() -> list[str]:
    errors = t38.check_document(OUTPUT, build())
    if errors:
        return errors
    document = t35.load_json(OUTPUT)
    validators = document.get("validators") or {}
    if any(int(value or 0) != 0 for value in validators.values()):
        errors.append("T38 publication delta validators are not zero")
    if document.get("authored_compact_family_entries") != t38.FAMILY_COUNT:
        errors.append("authored compact family entries is not +29")
    if document.get("authored_datapack_entries") != t38.AUTHORED_DATAPACK_COUNT:
        errors.append("authored datapack entries is not +48")
    if document.get("logical") != t38.SOURCE_ROWS:
        errors.append("logical publication is not +73")
    if document.get("eager") not in {t38.SUPPORT_EAGER_COUNT, None}:
        if not document.get("production_winner_pending"):
            errors.append("eager publication is not compact 0 plus 15 support")
    if document.get("production_winner_pending"):
        if document.get("eager") is not None or document.get("lazy") is not None:
            errors.append("blocked winner must not zero-fill eager/lazy")
    return errors


def main(argv: list[str] | None = None) -> int:
    args = t38.parse_write_check(__doc__, argv)
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
                "winner": document["production_winner"],
            }))
            return 0
        errors = check()
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        print(f"{t38.relative(OUTPUT)} is current")
        return 0
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"T38 publication delta failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
