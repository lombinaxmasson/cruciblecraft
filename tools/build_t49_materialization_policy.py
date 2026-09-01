#!/usr/bin/env python3
"""Freeze tools/t49_materialization_policy.json from generated T49 durations."""
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
from tools import wave_bath_tiny_purified as tiny_purified

OUTPUT = tiny_purified.POLICY
HYBRID_CUTOFF = tiny_purified.HYBRID_CUTOFF
CACHE_CEILING = tiny_purified.CACHE_CEILING


def _group_relations(group_id: str) -> tuple[list[dict[str, Any]], dict[str, int]]:
    relations: list[dict[str, Any]] = []
    for path in tiny_purified.generated_family_files():
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
                f"T49 {label} group has {rows} relations across {families} families "
                f"with duration histogram {histogram}; median duration {median}. "
                f"duration_ticks_lte {HYBRID_CUTOFF} freezes hybrid on T49's shortest "
                f"observed duration class, not T46 duration_ticks_lte 0 zero-eager "
                f"({len(eager)} eager / {rows - len(eager)} lazy). Cache ceiling "
                f"{hybrid_cache} is T49's lookup/shard hard envelope "
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
    tiny_purified.assert_t48_opening_current()
    opening = tiny_purified.t14_opening_load()
    families = tiny_purified.production_family_count()
    rows = tiny_purified.production_relation_count()
    multi_relations, multi_hist = _group_relations(tiny_purified.PUBLICATION_GROUP_EXACT_MULTI)
    if len(multi_relations) != rows:
        raise ValueError("T49 generated relation counts drifted")
    group_manifest = tiny_purified.load_json(tiny_purified.PUBLICATION_GROUP_MANIFEST)
    family_by_group = {
        str(row["publication_group"]): int(row["family_count"])
        for row in group_manifest.get("groups") or []
    }
    multi = _contract(
        group_id=tiny_purified.PUBLICATION_GROUP_EXACT_MULTI,
        families=family_by_group[tiny_purified.PUBLICATION_GROUP_EXACT_MULTI],
        relations=multi_relations,
        histogram=multi_hist,
        label="exact_multi",
    )
    card_eager = multi["hybrid_boundary"]["declared_eager_count"]
    card_on_demand_cache = min(CACHE_CEILING, rows)
    card_hybrid_cache = min(CACHE_CEILING, max(0, rows - card_eager))
    card = {
        **multi,
        "publication_group": "card_aggregate",
        "family_count": families,
        "logical_rows": rows,
        "hybrid_boundary": {
            **multi["hybrid_boundary"],
            "declared_eager_count": card_eager,
            "declared_lazy_count": rows - card_eager,
            "cache_ceiling": card_hybrid_cache,
            "eager_stable_ids": list(multi["hybrid_boundary"]["eager_stable_ids"]),
            "justification": (
                f"T49 card aggregate is the single exact_multi group "
                f"({len(multi_relations)} relations / {families} families). "
                f"Hybrid remains duration_ticks_lte {HYBRID_CUTOFF}; cache "
                f"{card_hybrid_cache} is per-group ceiling {CACHE_CEILING}. "
                "T37-T48 integrated allocation already exceeds 512 MiB; "
                "this card stays on_demand and does not raise hard gates."
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
        "source": tiny_purified.T14_OPENING_LOAD_SOURCE,
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
        "status": "T49_MATERIALIZATION_CANDIDATE_POLICY",
        "source_revision": tiny_purified.SOURCE_REVISION,
        "scope": {
            "family_count": families,
            "production_logical_rows": rows,
            "production_lock_sha256": tiny_purified.production_lock_sha256(),
            "does_not_modify": [
                "T37 14/36/8",
                "T38 0/73/16",
                "T39 19/13",
                "T40 cache 11",
                "T41 cache 16",
                "T45 cache 24",
                "T46 cache 24 / duration_ticks_lte 0",
                "T47 cache 128 / duration_ticks_lte 16",
                "T48 cache 128 / duration_ticks_lte 16",
            ],
        },
        "t49_opening": nested_opening,
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
            "exact_multi": multi,
            "card_aggregate": card,
        },
        "hard_ceilings": hard,
    }


def main(argv: list[str] | None = None) -> int:
    return tiny_purified.run_managed(
        "Freeze T49 materialization candidate policy",
        OUTPUT,
        build=build,
        argv=argv,
    )


if __name__ == "__main__":
    raise SystemExit(main())
