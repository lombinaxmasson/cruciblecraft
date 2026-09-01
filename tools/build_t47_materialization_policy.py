#!/usr/bin/env python3
"""Freeze tools/t47_materialization_policy.json from generated T47 durations."""
from __future__ import annotations

import json
import sys
from collections import Counter
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import forward_v2_budget_decision as v2_budget
from tools import t47_common as t47

OUTPUT = t47.POLICY
HYBRID_CUTOFF = t47.HYBRID_CUTOFF
CACHE_CEILING = t47.CACHE_CEILING


def _group_relations(group_id: str) -> tuple[list[dict[str, Any]], dict[str, int]]:
    relations: list[dict[str, Any]] = []
    for path in t47.generated_family_files():
        document = json.loads(path.read_text(encoding="utf-8"))
        if document.get("publication_group") != group_id:
            continue
        relations.extend(document.get("relations") or [])
    histogram = Counter(int(relation["duration"]) for relation in relations)
    return relations, {str(key): histogram[key] for key in sorted(histogram)}


def eager_ids(relations: list[dict[str, Any]], cutoff: int) -> list[str]:
    return sorted(
        relation["stable_id"]
        for relation in relations
        if int(relation["duration"]) <= cutoff
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
    cache = min(CACHE_CEILING, rows)
    eager = eager_ids(relations, HYBRID_CUTOFF)
    hybrid_cache = min(cache, max(0, rows - len(eager)))
    durations = sorted(int(relation["duration"]) for relation in relations)
    median = durations[len(durations) // 2]
    return {
        "publication_group": group_id,
        "family_count": families,
        "logical_rows": rows,
        "candidates": ["immediate", "on_demand", "hybrid"],
        "cache_policies": {
            "immediate": "fully eager; lookup cache disabled; cache ceiling 0",
            "on_demand": f"all lazy; bounded cache ceiling {cache}",
            "hybrid": (
                f"eager selector frozen below; bounded cache ceiling {hybrid_cache}"
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
                f"T47 {label} group has {rows} relations across {families} families "
                f"with duration histogram {histogram}; median duration {median}. "
                f"duration_ticks_lte {HYBRID_CUTOFF} freezes hybrid on T47's shortest "
                f"observed duration class, not T46 duration_ticks_lte 0 zero-eager "
                f"({len(eager)} eager / {rows - len(eager)} lazy). Cache ceiling "
                f"{hybrid_cache} is T47's lookup/shard hard envelope "
                f"{CACHE_CEILING}, not T46 cache 24 copied as a winner."
            ),
        },
        "partitions": {
            "immediate": [rows, 0, 0],
            "on_demand": [0, rows, cache],
            "hybrid": [len(eager), rows - len(eager), hybrid_cache],
        },
    }


def build() -> dict[str, Any]:
    t47.assert_t46_opening_current()
    opening = t47.t14_opening_load()
    families = t47.production_family_count()
    rows = t47.production_relation_count()
    exact_relations, exact_hist = _group_relations(t47.PUBLICATION_GROUP_EXACT)
    multi_relations, multi_hist = _group_relations(t47.PUBLICATION_GROUP_EXACT_MULTI)
    if len(exact_relations) + len(multi_relations) != rows:
        raise ValueError("T47 generated relation counts drifted")
    exact = _contract(
        group_id=t47.PUBLICATION_GROUP_EXACT,
        families=t47.load_production_lock()["production"]["exact_families"],
        relations=exact_relations,
        histogram=exact_hist,
        label="exact",
    )
    multi = _contract(
        group_id=t47.PUBLICATION_GROUP_EXACT_MULTI,
        families=t47.load_production_lock()["production"]["exact_multi_families"],
        relations=multi_relations,
        histogram=multi_hist,
        label="exact_multi",
    )
    if exact["logical_rows"] != exact["family_count"]:
        raise ValueError("T47 exact group is not 1:1 family/relation")
    card_eager = (
        exact["hybrid_boundary"]["declared_eager_count"]
        + multi["hybrid_boundary"]["declared_eager_count"]
    )
    card_on_demand_cache = min(CACHE_CEILING, rows)
    card_hybrid_cache = min(CACHE_CEILING, max(0, rows - card_eager))
    card = {
        **exact,
        "publication_group": "card_aggregate",
        "family_count": families,
        "logical_rows": rows,
        "hybrid_boundary": {
            **exact["hybrid_boundary"],
            "declared_eager_count": card_eager,
            "declared_lazy_count": rows - card_eager,
            "cache_ceiling": card_hybrid_cache,
            "eager_stable_ids": (
                exact["hybrid_boundary"]["eager_stable_ids"]
                + multi["hybrid_boundary"]["eager_stable_ids"]
            ),
            "justification": (
                f"T47 card aggregate has {rows} relations (exact "
                f"{len(exact_relations)} + exact_multi {len(multi_relations)}). "
                f"Hybrid remains duration_ticks_lte {HYBRID_CUTOFF}; cache "
                f"{card_hybrid_cache} is per-group ceiling {CACHE_CEILING}, not T46 24."
            ),
        },
        "partitions": {
            "immediate": [rows, 0, 0],
            "on_demand": [0, rows, card_on_demand_cache],
            "hybrid": [card_eager, rows - card_eager, card_hybrid_cache],
        },
    }
    limits = v2_budget.limit_maps()
    hard = limits["hard"]
    soft = limits["soft"]
    verdicts = limits["verdicts"]
    nested_opening = {
        "source": t47.T14_OPENING_LOAD_SOURCE,
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
        "status": "T47_MATERIALIZATION_CANDIDATE_POLICY",
        "source_revision": t47.SOURCE_REVISION,
        "scope": {
            "family_count": families,
            "production_logical_rows": rows,
            "production_lock_sha256": t47.production_lock_sha256(),
            "does_not_modify": [
                "T37 14/36/8",
                "T38 0/73/16",
                "T39 19/13",
                "T40 cache 11",
                "T41 cache 16",
                "T45 cache 24",
                "T46 cache 24 / duration_ticks_lte 0",
            ],
        },
        "t47_opening": nested_opening,
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
        "load_budget_policy_v2": {
            "path": v2_budget.LOAD_POLICY_V2.relative_to(ROOT).as_posix(),
            "sha256": v2_budget.policy_sha256(),
            "status": "FORWARD_LOAD_BUDGET_POLICY_V2",
        },
        "hard_limits_1x": hard,
        "soft_budgets_1x": soft,
        "readiness_verdicts_1x": verdicts,
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
            "soft_budget_does_not_eliminate": True,
            "authored_entries_verdict": "REPORT_ONLY",
        },
        "publication_group_contract": {
            "exact": exact,
            "exact_multi": multi,
            "card_aggregate": card,
        },
        "hard_ceilings": hard,
    }


def main(argv: list[str] | None = None) -> int:
    return t47.run_managed(
        "Freeze T47 materialization candidate policy",
        OUTPUT,
        build=build,
        argv=argv,
    )


if __name__ == "__main__":
    raise SystemExit(main())
