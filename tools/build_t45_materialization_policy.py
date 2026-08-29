#!/usr/bin/env python3
"""Freeze tools/t45_materialization_policy.json from generated T45 durations."""
from __future__ import annotations

import json
import sys
from collections import Counter
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35
from tools import t45_common as t45

GEN = t45.GENERATED_ROOT
OUTPUT = t45.POLICY
HYBRID_CUTOFF = t45.HYBRID_CUTOFF
CACHE_CEILING = t45.CACHE_CEILING


def _group_relations(group_id: str) -> tuple[list[dict[str, Any]], dict[str, int]]:
    relations: list[dict[str, Any]] = []
    for path in t45.generated_family_files():
        document = json.loads(path.read_text(encoding="utf-8"))
        if document.get("publication_group") != group_id:
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


def _contract(
    *,
    group_id: str,
    families: int,
    relations: list[dict[str, Any]],
    histogram: dict[str, int],
    label: str,
) -> dict[str, Any]:
    rows = len(relations)
    if rows != families:
        raise ValueError(f"T45 {label} family/relation counts drifted")
    cache = min(CACHE_CEILING, rows)
    eager = eager_ids(relations, HYBRID_CUTOFF)
    hybrid_cache = min(cache, rows - len(eager))
    median = sorted(relation["duration"] for relation in relations)[len(relations) // 2]
    return {
        "publication_group": group_id,
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
                f"T45 {label} group has {rows} relations across {families} families "
                f"with duration histogram {histogram}; median duration {median}. "
                f"duration_ticks_lte {HYBRID_CUTOFF} freezes hybrid as zero-eager "
                f"({len(eager)} eager / {rows - len(eager)} lazy). Cache ceiling "
                f"{hybrid_cache} is T45's declared-before-measure bound, not T37 8, "
                f"T38 16, T39 19, T40 11, T41 16, or T43 24 copied as a winner."
            ),
        },
        "partitions": {
            "immediate": [rows, 0, 0],
            "on_demand": [0, rows, cache],
            "hybrid": [len(eager), rows - len(eager), hybrid_cache],
        },
    }


def build() -> dict[str, Any]:
    opening = t45.t14_opening_load()
    families = t45.production_family_count()
    rows = t45.production_relation_count()
    smelter_relations, smelter_hist = _group_relations(t45.SMELTER_GROUP)
    drying_relations, drying_hist = _group_relations(t45.DRYING_GROUP)
    if len(smelter_relations) + len(drying_relations) != rows:
        raise ValueError("T45 generated relation counts drifted")
    smelter = _contract(
        group_id=t45.SMELTER_GROUP,
        families=len(smelter_relations),
        relations=smelter_relations,
        histogram=smelter_hist,
        label="smelter",
    )
    drying = _contract(
        group_id=t45.DRYING_GROUP,
        families=len(drying_relations),
        relations=drying_relations,
        histogram=drying_hist,
        label="drying",
    )
    card_eager = (
        smelter["hybrid_boundary"]["declared_eager_count"]
        + drying["hybrid_boundary"]["declared_eager_count"]
    )
    card_cache = min(
        CACHE_CEILING,
        smelter["hybrid_boundary"]["cache_ceiling"]
        + drying["hybrid_boundary"]["cache_ceiling"],
    )
    card = {
        **smelter,
        "publication_group": "card_aggregate",
        "family_count": families,
        "logical_rows": rows,
        "hybrid_boundary": {
            **smelter["hybrid_boundary"],
            "declared_eager_count": card_eager,
            "declared_lazy_count": rows - card_eager,
            "cache_ceiling": card_cache,
            "eager_stable_ids": (
                smelter["hybrid_boundary"]["eager_stable_ids"]
                + drying["hybrid_boundary"]["eager_stable_ids"]
            ),
            "justification": (
                f"T45 card aggregate has {rows} relations (smelter "
                f"{len(smelter_relations)} + drying {len(drying_relations)}). "
                f"Hybrid remains duration_ticks_lte {HYBRID_CUTOFF}."
            ),
        },
        "partitions": {
            "immediate": [rows, 0, 0],
            "on_demand": [0, rows, min(CACHE_CEILING, rows)],
            "hybrid": [card_eager, rows - card_eager, card_cache],
        },
    }
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
        "source": t45.T14_OPENING_LOAD_SOURCE,
        "pending_runtime_axes_zero_fill": False,
    }
    for axis, value in opening.items():
        nested_opening[axis] = {
            "closing": value,
            "hard_ceiling": hard.get(axis),
            "measured": True,
            "pending": False,
        }
    return {
        "schema_version": 1,
        "status": "T45_MATERIALIZATION_CANDIDATE_POLICY",
        "source_revision": t45.SOURCE_REVISION,
        "scope": {
            "family_count": families,
            "production_logical_rows": rows,
            "production_lock_sha256": t45.production_lock_sha256(),
            "does_not_modify": [
                "T37 14/36/8",
                "T38 0/73/16",
                "T39 19/13",
                "T40 cache 11",
                "T41 cache 16",
                "T43 cache 24",
            ],
        },
        "t45_opening": nested_opening,
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
            "max_interval_soft_budget_does_not_eliminate": [
                "lookup_candidate_count",
            ],
        },
        "publication_group_contract": {
            "smelter": smelter,
            "drying": drying,
            "card_aggregate": card,
        },
        "hard_ceilings": hard,
    }


def main(argv: list[str] | None = None) -> int:
    return t45.run_managed(
        "Freeze T45 materialization candidate policy",
        OUTPUT,
        build=build,
        argv=argv,
    )


if __name__ == "__main__":
    raise SystemExit(main())
