#!/usr/bin/env python3
"""Freeze tools/t41_materialization_policy.json from generated T41 durations."""

from __future__ import annotations

import argparse
import json
import sys
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t41_common as t41  # noqa: E402
GEN = (
    ROOT
    / "src/t41_recipe_generated/resources/data/cruciblecraft/recipe/t41/assembler"
)
CENSUS = ROOT / "tools/t40_readiness.json"
OUTPUT = ROOT / "tools/t41_materialization_policy.json"

PLANKS_GROUP = t41.PLANKS_GROUP
FIREPROOF_GROUP = t41.FIREPROOF_GROUP
PLANKS2_GROUP = t41.PLANKS2_GROUP
# Match CompactRecipeFamilyT41MeasurementHarness ZERO_EAGER hybrid.
HYBRID_CUTOFF = 0
CACHE_CEILING = 16


def load(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


def stable_json(value: dict) -> str:
    return json.dumps(value, ensure_ascii=False, indent=2) + "\n"


def group_stats(group: str) -> tuple[list[dict], dict[str, int]]:
    relations: list[dict] = []
    for path in sorted(GEN.glob("*.json")):
        document = load(path)
        if document.get("publication_group") != group:
            continue
        relations.extend(document.get("relations") or [])
    histogram = Counter(relation["duration"] for relation in relations)
    return relations, {str(key): histogram[key] for key in sorted(histogram)}


def eager_ids(relations: list[dict], cutoff: int) -> list[str]:
    return sorted(
        relation["stable_id"]
        for relation in relations
        if relation["duration"] <= cutoff
    )


def _group_contract(
    *,
    group_id: str,
    label: str,
    families: int,
    rows: int,
    relations: list[dict],
    histogram: dict[str, int],
) -> dict:
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
            "immediate": (
                "fully eager; lookup cache disabled; cache ceiling 0"
            ),
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
                f"T41 {label} group has {rows} relations across {families} families "
                f"with duration histogram {histogram}; median duration {median}. "
                f"duration_ticks_lte {HYBRID_CUTOFF} freezes hybrid as zero-eager "
                f"({len(eager)} eager / {rows - len(eager)} lazy) to match the Java "
                f"measurement harness ZERO_EAGER selector, not T37 14/36, T38 38/35, "
                f"or T40 cache 11. Cache ceiling {hybrid_cache} is bounded by lazy rows."
            ),
        },
        "partitions": {
            "immediate": [rows, 0, 0],
            "on_demand": [0, rows, cache],
            "hybrid": [
                len(eager),
                rows - len(eager),
                hybrid_cache,
            ],
        },
    }


def build() -> dict:
    census = load(CENSUS)
    opening = ((census.get("t41_opening") or {}).get("t14_closing") or {})
    if not opening:
        raise ValueError("missing tools/t40_readiness.json#t41_opening.t14_closing")
    axes: dict[str, dict] = {
        "datapack_authored_entries": {"hard_ceiling": 6600},
        "eager_publication_rows": {"hard_ceiling": 21000},
        "lazy_logical_rows": {"hard_ceiling": 56000},
        "lazy_cache_ceiling_rows": {"hard_ceiling": 4096},
        "sync_bytes": {"hard_ceiling": 67108864},
        "server_reload_ms": {"hard_ceiling": 10000},
        "server_index_ms": {"hard_ceiling": 1000},
        "client_reload_ms": {"hard_ceiling": 10000},
        "client_index_ms": {"hard_ceiling": 3000},
        "retained_memory_bytes": {"hard_ceiling": 536870912},
        "allocation_bytes": {"hard_ceiling": 536870912},
        "lookup_p95_ns": {"hard_ceiling": 2000000},
        "lookup_candidate_count": {"hard_ceiling": 128},
    }

    planks_relations, planks_hist = group_stats(PLANKS_GROUP)
    fireproof_relations, fireproof_hist = group_stats(FIREPROOF_GROUP)
    planks2_relations, planks2_hist = group_stats(PLANKS2_GROUP)
    group_counts = t41.production_group_counts()
    planks_rows = group_counts[PLANKS_GROUP]["relations"]
    planks_families = group_counts[PLANKS_GROUP]["families"]
    fireproof_rows = group_counts[FIREPROOF_GROUP]["relations"]
    fireproof_families = group_counts[FIREPROOF_GROUP]["families"]
    planks2_rows = group_counts[PLANKS2_GROUP]["relations"]
    planks2_families = group_counts[PLANKS2_GROUP]["families"]
    family_count = t41.production_family_count()
    relation_count = t41.production_relation_count()
    planks_cache = min(CACHE_CEILING, planks_rows)
    fireproof_cache = min(CACHE_CEILING, fireproof_rows)
    planks2_cache = min(CACHE_CEILING, planks2_rows)
    if (
        len(planks_relations) != planks_rows
        or len(fireproof_relations) != fireproof_rows
        or len(planks2_relations) != planks2_rows
    ):
        raise ValueError("T41 generated relation counts drifted")
    planks_contract = _group_contract(
        group_id=PLANKS_GROUP,
        label="planks",
        families=planks_families,
        rows=planks_rows,
        relations=planks_relations,
        histogram=planks_hist,
    )
    fireproof_contract = _group_contract(
        group_id=FIREPROOF_GROUP,
        label="fireproof",
        families=fireproof_families,
        rows=fireproof_rows,
        relations=fireproof_relations,
        histogram=fireproof_hist,
    )
    planks2_contract = _group_contract(
        group_id=PLANKS2_GROUP,
        label="planks2",
        families=planks2_families,
        rows=planks2_rows,
        relations=planks2_relations,
        histogram=planks2_hist,
    )
    hybrid_eager = (
        planks_contract["hybrid_boundary"]["declared_eager_count"]
        + fireproof_contract["hybrid_boundary"]["declared_eager_count"]
        + planks2_contract["hybrid_boundary"]["declared_eager_count"]
    )
    hybrid_cache = (
        planks_contract["hybrid_boundary"]["cache_ceiling"]
        + fireproof_contract["hybrid_boundary"]["cache_ceiling"]
        + planks2_contract["hybrid_boundary"]["cache_ceiling"]
    )

    policy: dict = {
        "schema_version": 1,
        "status": "T41_MATERIALIZATION_CANDIDATE_POLICY",
        "scope": {
            "card": "T41",
            "host": "cruciblecraft:assembler",
            "work_set": (
                f"production lock {family_count} families / "
                f"{relation_count} relations"
            ),
            "family_count": family_count,
            "planks_families": planks_families,
            "fireproof_families": fireproof_families,
            "planks2_families": planks2_families,
            "production_logical_rows": relation_count,
            "production_lock_sha256": t41.production_lock_sha256(),
            "publication_groups": [
                {
                    "id": PLANKS_GROUP,
                    "family_count": planks_families,
                    "logical_rows": planks_rows,
                },
                {
                    "id": FIREPROOF_GROUP,
                    "family_count": fireproof_families,
                    "logical_rows": fireproof_rows,
                },
                {
                    "id": PLANKS2_GROUP,
                    "family_count": planks2_families,
                    "logical_rows": planks2_rows,
                },
            ],
            "benchmark_only": False,
            "production_switch_allowed": False,
            "production_target": "cruciblecraft:assembler",
            "does_not_modify": [
                "Extruder HOT_MODULO",
                "Extruder CACHE_CEILING=512",
                "T14 20x ranking ratios",
                "T35/T36/T37/T38 historical READY artifacts",
                "T37 14/36/8 hybrid partition or cache 8",
                "T38 0/73/16 on_demand winner or cache 16",
                "T39 19/13 on_demand/hybrid winners or cache 19/13",
                "T40 cache 11",
            ],
        },
        "t41_opening": {
            "source": t41.T14_OPENING_LOAD_SOURCE,
        },
        "scales": [
            {
                "id": "1x",
                "logical_rows": relation_count,
                "purpose": (
                    "production candidate comparison denominator; "
                    "actual T41 work set"
                ),
            },
            {
                "id": "5x",
                "logical_rows": relation_count * 5,
                "purpose": (
                    "diagnostic pressure only; must not fill 1x unmeasured fields"
                ),
            },
            {
                "id": "20x",
                "logical_rows": relation_count * 20,
                "purpose": (
                    "diagnostic pressure only; must not fill 1x unmeasured fields "
                    "or copy Extruder 55640"
                ),
            },
        ],
        "publication_group_contract": {
            "planks": planks_contract,
            "fireproof": fireproof_contract,
            "planks2": planks2_contract,
            "card_aggregate": {
                "family_count": family_count,
                "logical_rows": relation_count,
                "candidates": ["immediate", "on_demand", "hybrid"],
                "partitions": {
                    "immediate": [relation_count, 0, 0],
                    "on_demand": [
                        0,
                        relation_count,
                        planks_cache + fireproof_cache + planks2_cache,
                    ],
                    "hybrid": [
                        hybrid_eager,
                        relation_count - hybrid_eager,
                        hybrid_cache,
                    ],
                },
                "note": (
                    "Card aggregate sums per-group policies independently; cache "
                    "ceiling is the sum of independently bounded group caches "
                    f"({CACHE_CEILING}+{CACHE_CEILING}+{CACHE_CEILING}="
                    f"{planks_cache + fireproof_cache + planks2_cache} on_demand)."
                ),
            },
        },
        "candidate_contract": {
            "candidates": ["immediate", "on_demand", "hybrid"],
            "client_gate": {
                "server": (
                    "prepare CompactRecipeFamilyProvider snapshot on the server side"
                ),
                "dedicated_client": (
                    "re-expand from the synchronized compact relation payload"
                ),
                "integrated_client": (
                    "skip re-expansion and reuse the server snapshot; not a second "
                    "wall-clock sample"
                ),
            },
            "enumeration": (
                "complete epoch-bound view of every logical recipe; must not "
                "pre-warm or grow past the cache ceiling"
            ),
            "lookup": (
                "indexed candidate then stable-id selection; p50/p95 from sample "
                "intervals, never one wall-clock reading"
            ),
            "shard_routing": (
                "overflow and indexed candidates must each stay within "
                "CompactRecipeShardRouter.HARD_SHARD_CEILING 128; fixture "
                "routing is never production load evidence"
            ),
        },
        "workload": {
            "production_scale": "1x",
            "reload_warmup_iterations": 2,
            "lookup_warmup_operations": 32,
            "lookup_samples": 21,
            "lookup_operations_per_sample": 16,
            "enumeration_warmup_iterations": 1,
            "timing_error_bar": "nonparametric_95_percent_median_order_statistic",
            "single_wall_clock_sample_forbidden": True,
        },
        "hard_limits_1x": {
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
        },
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
            "gate_order": [
                "correctness",
                "hard_ceiling",
                "cumulative_soft_budget",
            ],
            "correctness_gates": [
                "field_equivalence",
                "full_enumeration",
                "client_consistency",
                "player_execution",
            ],
            "hard_ceiling_fail_eliminates": True,
            "production_candidate_must_sit_inside_cumulative_soft_budget": True,
            "max_interval_soft_budget_does_not_eliminate": [
                "lookup_candidate_count"
            ],
            "max_interval_soft_note": (
                "T14 lookup_candidate_count is max_interval across groups. The "
                "Extruder-calibrated soft 64 is recorded as SOFT_BUDGET_EXCEEDED "
                "and must not copy T37/T38 envelopes as a T41 veto. Hard ceiling "
                "128 is not raised."
            ),
            "composite_score_forbidden": True,
            "ranking_scale": "1x",
            "ranking_logical_rows": relation_count,
            "per_group_winners_required": True,
            "card_aggregate_required": True,
            "ranking_order_after_all_gates_pass": [
                "eager_margin",
                "retained_memory",
                "reload_index",
                "lookup",
                "sync",
                "implementation_complexity",
            ],
            "eager_margin": (
                "If every survivor is inside the cumulative eager soft budget, "
                "treat eager_margin as tied and continue."
            ),
            "implementation_complexity_order": [
                "immediate",
                "on_demand",
                "hybrid",
            ],
            "pending_axis_zero_fill_forbidden": True,
            "diagnostic_5x_20x_must_not_fill_1x": True,
            "production_guard": (
                f"Emit production winners only when 1x={relation_count} logical "
                "rows are "
                "actually measured per group and card aggregate, every ranking "
                "axis used is PASS (not PENDING), and winners are derived by "
                "tools/build_t41_recipe_load_benchmark.py."
            ),
        },
    }

    for axis in (
        "datapack_authored_entries",
        "eager_publication_rows",
        "lazy_logical_rows",
        "lazy_cache_ceiling_rows",
        "sync_bytes",
        "server_reload_ms",
        "server_index_ms",
        "client_reload_ms",
        "client_index_ms",
        "retained_memory_bytes",
        "allocation_bytes",
        "lookup_p95_ns",
        "lookup_candidate_count",
    ):
        row = axes.get(axis, {})
        axis_payload = {
            "closing": opening.get(axis, row.get("closing")),
            "hard_ceiling": row.get("hard_ceiling"),
            "measured": True,
            "pending": False,
        }
        if axis == "lazy_cache_ceiling_rows":
            axis_payload["note"] = (
                "T40 card-level closing; T41 per-group cache ceiling is declared "
                "separately as 16/16/16"
            )
        policy["t41_opening"][axis] = axis_payload
    policy["t41_opening"]["pending_runtime_axes"] = []
    policy["t41_opening"]["pending_runtime_axes_zero_fill"] = False
    return policy


def validate_policy(policy: dict) -> None:
    if policy.get("status") != "T41_MATERIALIZATION_CANDIDATE_POLICY":
        raise ValueError("T41 policy header drifted")
    scope = policy["scope"]
    if scope.get("production_logical_rows") != t41.production_relation_count():
        raise ValueError("T41 scope must match the production lock")
    if scope.get("family_count") != t41.production_family_count():
        raise ValueError("T41 scope family count must match the production lock")
    counts = t41.production_group_counts()
    if (
        scope.get("planks_families") != counts[PLANKS_GROUP]["families"]
        or scope.get("fireproof_families") != counts[FIREPROOF_GROUP]["families"]
        or scope.get("planks2_families") != counts[PLANKS2_GROUP]["families"]
    ):
        raise ValueError("T41 policy group family counts drifted")
    contract = policy["publication_group_contract"]
    for group_key in ("planks", "fireproof", "planks2"):
        hybrid = contract[group_key]["hybrid_boundary"]
        if (hybrid["declared_eager_count"], hybrid["declared_lazy_count"]) in {
            (14, 36),
            (38, 35),
        }:
            raise ValueError("T41 must not copy T37/T38 hybrid partitions")
        if contract[group_key]["partitions"]["on_demand"][2] != CACHE_CEILING:
            raise ValueError(f"T41 {group_key} on_demand cache must be {CACHE_CEILING}")
    on_demand_cache = contract["card_aggregate"]["partitions"]["on_demand"][2]
    if on_demand_cache != CACHE_CEILING * 3:
        raise ValueError("T41 card on_demand cache must be 16+16+16=48")
    does_not_modify = scope.get("does_not_modify") or []
    if not any("T40 cache 11" in item for item in does_not_modify):
        raise ValueError("T41 policy must record does_not_modify T40 cache 11")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    if args.write == args.check:
        parser.error("choose exactly one of --write or --check")
    policy = build()
    validate_policy(policy)
    encoded = stable_json(policy)
    if args.write:
        OUTPUT.write_text(encoded, encoding="utf-8")
        print(f"Wrote {OUTPUT.relative_to(ROOT).as_posix()}")
        return 0
    if not OUTPUT.is_file():
        print("missing policy")
        return 1
    if OUTPUT.read_text(encoding="utf-8") != encoded:
        print("policy is stale")
        return 1
    print("policy is current")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
