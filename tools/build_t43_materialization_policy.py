#!/usr/bin/env python3
"""Freeze tools/t43_materialization_policy.json from generated T43 durations."""
from __future__ import annotations

import json
import sys
from collections import Counter
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35  # noqa: E402
from tools import t43_common as t43  # noqa: E402

GEN = t43.GENERATED_ROOT
OUTPUT = t43.POLICY
HYBRID_CUTOFF = t43.HYBRID_CUTOFF
CACHE_CEILING = t43.CACHE_CEILING


def group_stats() -> tuple[list[dict[str, Any]], dict[str, int]]:
    relations: list[dict[str, Any]] = []
    for path in sorted(GEN.glob("*.json")):
        document = json.loads(path.read_text(encoding="utf-8"))
        if document.get("publication_group") != t43.STONE_GROUP:
            continue
        relations.extend(document.get("relations") or [])
    histogram = Counter(relation["duration"] for relation in relations)
    return relations, {str(key): histogram[key] for key in sorted(histogram)}


def eager_ids(relations: list[dict[str, Any]], cutoff: int) -> list[str]:
    return sorted(
        relation["stable_id"]
        for relation in relations
        if relation["duration"] <= cutoff
    )


def build() -> dict[str, Any]:
    opening = t43.t14_opening_load()
    relations, histogram = group_stats()
    rows = t43.PRODUCTION_RELATION_COUNT
    families = t43.PRODUCTION_FAMILY_COUNT
    if len(relations) != rows:
        raise ValueError("T43 generated relation counts drifted")
    cache = min(CACHE_CEILING, rows)
    eager = eager_ids(relations, HYBRID_CUTOFF)
    hybrid_cache = min(cache, rows - len(eager))
    median = sorted(relation["duration"] for relation in relations)[len(relations) // 2]
    hard = {
        "datapack_authored_entries": 6600,
        "eager_publication_rows": 21000,
        "lazy_logical_rows": 56000,
        "lazy_cache_ceiling_rows": 4096,
        "sync_bytes": 67108864,
        "server_reload_ms": 10000,
        "server_index_ms": 1000,
        "client_reload_ms": 10000,
        "client_index_ms": 3000,
        "retained_memory_bytes": 536870912,
        "allocation_bytes": 536870912,
        "lookup_p95_ns": 2000000,
        "lookup_candidate_count": 128,
    }
    nested_opening = {
        "source": t43.T14_OPENING_LOAD_SOURCE,
        "pending_runtime_axes_zero_fill": False,
    }
    for axis, value in opening.items():
        nested_opening[axis] = {
            "closing": value,
            "hard_ceiling": hard.get(axis),
            "measured": True,
            "pending": False,
        }
    contract = {
        "publication_group": t43.STONE_GROUP,
        "family_count": families,
        "logical_rows": rows,
        "candidates": ["immediate", "on_demand", "hybrid"],
        "cache_policies": {
            "immediate": "fully eager; lookup cache disabled; cache ceiling 0",
            "on_demand": f"all lazy; bounded cache ceiling {CACHE_CEILING}",
            "hybrid": (
                f"eager selector frozen below; bounded cache ceiling {CACHE_CEILING}"
            ),
        },
        "hybrid_boundary": {
            "declared_before_measurement": True,
            "hot_modulo": None,
            "forbidden_extruder_hot_modulo": 5,
            "eager_selector": {"duration_ticks_lte": HYBRID_CUTOFF},
            "cache_ceiling": hybrid_cache,
            "declared_eager_count": len(eager),
            "declared_lazy_count": rows - len(eager),
            "eager_stable_ids": eager,
            "justification": (
                f"T43 stone group has {rows} relations across {families} families "
                f"with duration histogram {histogram}; median duration {median}. "
                f"duration_ticks_lte {HYBRID_CUTOFF} freezes hybrid as zero-eager "
                f"({len(eager)} eager / {rows - len(eager)} lazy). Cache ceiling "
                f"{hybrid_cache} is T43's declared-before-measure bound, not T37 8, "
                f"T38 16, T39 19, T40 11, or T41 16."
            ),
        },
        "partitions": {
            "immediate": [rows, 0, 0],
            "on_demand": [0, rows, cache],
            "hybrid": [len(eager), rows - len(eager), hybrid_cache],
        },
    }
    return {
        "schema_version": 1,
        "status": "T43_MATERIALIZATION_CANDIDATE_POLICY",
        "source_revision": t43.SOURCE_REVISION,
        "scope": {
            "family_count": families,
            "production_logical_rows": rows,
            "production_lock_sha256": t43.production_lock_sha256(),
            "does_not_modify": [
                "T37 14/36/8",
                "T38 0/73/16",
                "T39 19/13",
                "T40 cache 11",
                "T41 cache 16",
            ],
        },
        "t43_opening": nested_opening,
        "candidate_contract": {
            "candidates": ["immediate", "on_demand", "hybrid"],
            "ranking_order": [
                "eager_margin",
                "retained_memory",
                "reload_index",
                "lookup",
                "sync",
                "implementation_complexity",
            ],
        },
        "hard_limits_1x": hard,
        "soft_budgets_1x": {
            "datapack_authored_entries": 6000,
            "eager_publication_rows": 18000,
            "lazy_logical_rows": 16000,
            "lazy_cache_ceiling_rows": 1024,
            "sync_bytes": 16777216,
            "server_reload_ms": 5000,
            "server_index_ms": 500,
            "client_reload_ms": 5000,
            "client_index_ms": 1000,
            "retained_memory_bytes": 134217728,
            "allocation_bytes": 134217728,
            "lookup_p95_ns": 1000000,
            "lookup_candidate_count": 64,
        },
        "winner_algorithm": {
            "composite_score_forbidden": True,
            "per_group_winners_required": True,
            "card_aggregate_required": True,
            "gate_order": [],
            "implementation_complexity_order": [
                "immediate",
                "on_demand",
                "hybrid",
            ],
        },
        "publication_group_contract": {
            "stone": contract,
            "card_aggregate": {
                **contract,
                "logical_rows": rows,
            },
        },
        "hard_ceilings": hard,
    }


def main(argv: list[str] | None = None) -> int:
    return t43.run_managed(
        "Freeze T43 materialization candidate policy",
        OUTPUT,
        build=build,
        argv=argv,
    )


if __name__ == "__main__":
    raise SystemExit(main())
