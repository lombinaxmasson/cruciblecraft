#!/usr/bin/env python3
"""Build the T37 publication-delta ledger from generated families and the winner."""
from __future__ import annotations

import json
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from tools import t35_common as t35  # noqa: E402
from tools import t37_common as t37  # noqa: E402

BUILDER = Path(__file__).resolve()
OUTPUT = t37.PUBLICATION_DELTA


def _family_ledgers(
    source: dict[str, Any],
    generated: list[Path],
    strategy: dict[str, Any],
    partition: dict[str, int | None],
) -> list[dict[str, Any]]:
    by_template = {
        path.name: path
        for path in generated
    }
    rows = []
    winner = strategy["winner"]
    for relation in source.get("relations") or []:
        template = str(relation.get("template_key") or "")
        number = template.rsplit("#", 1)[-1]
        filename = f"gt_recipe_assembler_{number}.json"
        authored = by_template.get(filename)
        eager = None if strategy["blocked"] else (
            1 if (partition["eager_publication_rows"] or 0) > 0 and winner == "immediate"
            else 0 if winner == "on_demand"
            else None
        )
        lazy = None if strategy["blocked"] else (
            0 if winner == "immediate"
            else 1 if winner == "on_demand"
            else None
        )
        if winner == "hybrid" and not strategy["blocked"]:
            family_id = str(relation.get("family_id") or "")
            short_id = family_id.split("/")[-1]
            eager_ids = partition.get("eager_family_ids") or set()
            eager = 1 if family_id in eager_ids or short_id in eager_ids else 0
            lazy = 0 if eager else 1
        rows.append({
            "family_id": relation.get("family_id"),
            "template_key": template,
            "source_rows": 1,
            "authored_entries": 1 if authored is not None else 0,
            "logical_rows": 1,
            "eager_publication_rows": eager,
            "lazy_logical_rows": lazy,
            "strategy": winner or "pending",
            "measurement_pointer": "tools/t37_materialization_decision.json",
            "authored_path": t37.relative(authored) if authored is not None else None,
        })
    return rows


def build() -> dict[str, Any]:
    source = t35.load_json(t37.ASSEMBLER_SOURCE)
    generated = t37.generated_family_files()
    strategy = t37.production_strategy()
    partition = t37.partition_for_winner(strategy["winner"])
    if strategy["winner"] == "hybrid" and not strategy["blocked"]:
        policy = t35.load_json(t37.POLICY)
        partition = dict(partition)
        partition["eager_family_ids"] = set(
            policy["hybrid_boundary"]["eager_family_ids"]
        )
    families = _family_ledgers(source, generated, strategy, partition)
    authored = sum(int(row["authored_entries"] or 0) for row in families)
    logical = sum(int(row["logical_rows"] or 0) for row in families)
    eager = partition["eager_publication_rows"]
    lazy = partition["lazy_logical_rows"]
    cache = partition["lazy_cache_ceiling_rows"]
    pending_strategy = strategy["blocked"]
    card_level = {
        "lazy_cache_ceiling_rows": cache,
        "recorded_once": True,
        "copied_per_family": False,
        "averaged_into_family_rows": False,
        "note": (
            "Shared provider/cache costs are card-level. They are not copied "
            "across the 50 family ledgers and are not averaged into per-family "
            "measurements."
        ),
        "pending": pending_strategy,
        "evidence": "tools/t37_materialization_decision.json#decision.production_winner",
    }
    return {
        "schema_version": 1,
        "status": (
            "T37_PUBLICATION_DELTA_READY"
            if authored == t37.T37_FAMILY_COUNT
            and logical == t37.T37_FAMILY_COUNT
            and not pending_strategy
            else "T37_PUBLICATION_DELTA_BLOCKED"
        ),
        "source_revision": t37.SOURCE_REVISION,
        "generated_by": "python tools/build_t37_publication_delta.py",
        "owner": t37.OWNER,
        "host": t37.HOST,
        "authored_compact_family_entries": authored,
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
        "note": (
            "Authored +50 compact family datapack entries and logical +50 are "
            "counted from generated files and frozen source. Eager/lazy/cache "
            "follow the derived production winner; a blocked winner is recorded "
            "as pending strategy, not as silent zeros."
        ),
        "validators": {
            "authored_50": 0 if authored == t37.T37_FAMILY_COUNT else 1,
            "logical_50": 0 if logical == t37.T37_FAMILY_COUNT else 1,
            "family_ledger_50": 0 if len(families) == t37.T37_FAMILY_COUNT else 1,
            "cache_not_copied_per_family": 0 if card_level["recorded_once"] else 1,
            "pending_strategy_not_zero_filled": 0 if (
                not pending_strategy
                or (eager is None and lazy is None and cache is None)
            ) else 1,
        },
    }


def winner_status(strategy: dict[str, Any]) -> dict[str, Any]:
    if strategy["blocked"]:
        return {
            "id": None,
            "status": "pending",
            "verdict": t37.PENDING_LOAD_VERDICT,
            "reason": (
                "tools/t37_materialization_decision.json production_winner is "
                "BLOCKED; eager/lazy/cache are not invented from the loader default."
            ),
        }
    return {
        "id": strategy["winner"],
        "status": "derived",
        "verdict": None,
        "reason": "Derived by tools/build_t37_recipe_load_benchmark.py",
    }


def write() -> dict[str, Any]:
    document = build()
    t35.write_stable(OUTPUT, document)
    return document


def check() -> list[str]:
    errors = t37.check_document(OUTPUT, build())
    if errors:
        return errors
    document = t35.load_json(OUTPUT)
    validators = document.get("validators") or {}
    if any(int(value or 0) != 0 for value in validators.values()):
        errors.append("T37 publication delta validators are not zero")
    if document.get("authored_compact_family_entries") != t37.T37_FAMILY_COUNT:
        errors.append("authored compact family entries is not +50")
    if document.get("logical") != t37.T37_FAMILY_COUNT:
        errors.append("logical publication is not +50")
    if document.get("production_winner_pending"):
        if document.get("eager") is not None or document.get("lazy") is not None:
            errors.append("blocked winner must not zero-fill eager/lazy")
    return errors


def main(argv: list[str] | None = None) -> int:
    args = t37.parse_write_check(__doc__, argv)
    try:
        if args.write:
            document = write()
            print(json.dumps({
                "status": document["status"],
                "authored": document["authored_compact_family_entries"],
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
        print(f"{t37.relative(OUTPUT)} is current")
        return 0
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"T37 publication delta failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
