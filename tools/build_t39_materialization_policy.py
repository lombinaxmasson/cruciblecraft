#!/usr/bin/env python3
"""Freeze tools/t39_materialization_policy.json from generated T39 durations."""

from __future__ import annotations

import argparse
import json
import sys
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t39_common as t39  # noqa: E402
GEN = (
    ROOT
    / "src/t39_recipe_generated/resources/data/cruciblecraft/recipe/t39/centrifuge"
)
CENSUS = ROOT / "tools/t38_census_delta.json"
OUTPUT = ROOT / "tools/t39_materialization_policy.json"

SINGLETON_GROUP = "cruciblecraft:t39_centrifuge_singleton"
MULTI_GROUP = "cruciblecraft:t39_centrifuge_multi"
SINGLETON_CUTOFF = 256
MULTI_CUTOFF = 584
CACHE_CEILING = 32


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


def build() -> dict:
    census = load(CENSUS)
    opening = census["t14_load"]["closing"]
    axes = {row["axis"]: row for row in census["t14_load"]["axes"]}

    singleton_relations, singleton_hist = group_stats(SINGLETON_GROUP)
    multi_relations, multi_hist = group_stats(MULTI_GROUP)
    group_counts = t39.production_group_counts()
    singleton_rows = group_counts[SINGLETON_GROUP]["relations"]
    singleton_families = group_counts[SINGLETON_GROUP]["families"]
    multi_rows = group_counts[MULTI_GROUP]["relations"]
    multi_families = group_counts[MULTI_GROUP]["families"]
    family_count = t39.production_family_count()
    relation_count = t39.production_relation_count()
    singleton_cache = min(CACHE_CEILING, singleton_rows)
    multi_cache = min(CACHE_CEILING, multi_rows)
    if (
        len(singleton_relations) != singleton_rows
        or len(multi_relations) != multi_rows
    ):
        raise ValueError("T39 generated relation counts drifted")
    singleton_eager = eager_ids(singleton_relations, SINGLETON_CUTOFF)
    multi_eager = eager_ids(multi_relations, MULTI_CUTOFF)
    singleton_hybrid_cache = min(
        singleton_cache, singleton_rows - len(singleton_eager)
    )
    multi_hybrid_cache = min(multi_cache, multi_rows - len(multi_eager))
    singleton_median = sorted(
        relation["duration"] for relation in singleton_relations
    )[len(singleton_relations) // 2]
    multi_median = sorted(relation["duration"] for relation in multi_relations)[
        len(multi_relations) // 2
    ]

    policy: dict = {
        "schema_version": 1,
        "status": "T39_MATERIALIZATION_CANDIDATE_POLICY",
        "scope": {
            "card": "T39",
            "host": "cruciblecraft:centrifuge",
            "work_set": (
                f"production lock {family_count} families / "
                f"{relation_count} relations"
            ),
            "family_count": family_count,
            "singleton_families": singleton_families,
            "multi_families": multi_families,
            "production_logical_rows": relation_count,
            "production_lock_sha256": t39.production_lock_sha256(),
            "publication_groups": [
                {
                    "id": SINGLETON_GROUP,
                    "family_count": singleton_families,
                    "logical_rows": singleton_rows,
                },
                {
                    "id": MULTI_GROUP,
                    "family_count": multi_families,
                    "logical_rows": multi_rows,
                },
            ],
            "benchmark_only": False,
            "production_switch_allowed": False,
            "production_target": "cruciblecraft:centrifuge",
            "does_not_modify": [
                "Extruder HOT_MODULO",
                "Extruder CACHE_CEILING=512",
                "T14 20x ranking ratios",
                "T35/T36/T37/T38 historical READY artifacts",
                "T37 14/36/8 hybrid partition or cache 8",
                "T38 0/73/16 on_demand winner or cache 16",
            ],
        },
        "t39_opening": {
            "source": "tools/t38_census_delta.json#t14_load.closing",
        },
        "scales": [
            {
                "id": "1x",
                "logical_rows": relation_count,
                "purpose": (
                    "production candidate comparison denominator; "
                    "actual T39 work set"
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
            "singleton": {
                "publication_group": SINGLETON_GROUP,
                "family_count": singleton_families,
                "logical_rows": singleton_rows,
                "candidates": ["immediate", "on_demand", "hybrid"],
                "cache_policies": {
                    "immediate": (
                        "fully eager; lookup cache disabled; cache ceiling 0"
                    ),
                    "on_demand": "all lazy; bounded cache ceiling 32",
                    "hybrid": (
                        "eager selector frozen below; bounded cache ceiling 32"
                    ),
                },
                "hybrid_boundary": {
                    "declared_before_measurement": True,
                    "hot_modulo": None,
                    "forbidden_extruder_hot_modulo": 5,
                    "eager_selector": {"duration_ticks_lte": SINGLETON_CUTOFF},
                    "cache_ceiling": singleton_hybrid_cache,
                    "declared_eager_count": len(singleton_eager),
                    "declared_lazy_count": singleton_rows - len(singleton_eager),
                    "eager_stable_ids": singleton_eager,
                    "justification": (
                        f"T39 singleton group has {singleton_rows} relations "
                        f"with duration "
                        f"histogram {singleton_hist}; median duration "
                        f"{singleton_median}. duration_ticks_lte "
                        f"{SINGLETON_CUTOFF} selects short/medium tick relations "
                        f"by stable_id ({len(singleton_eager)} eager / "
                        f"{singleton_rows - len(singleton_eager)} lazy), not "
                        f"T37 14/36 or T38 38/35. Cache ceiling "
                        f"{singleton_hybrid_cache} is bounded by lazy rows."
                    ),
                },
                "partitions": {
                    "immediate": [singleton_rows, 0, 0],
                    "on_demand": [0, singleton_rows, singleton_cache],
                    "hybrid": [
                        len(singleton_eager),
                        singleton_rows - len(singleton_eager),
                        singleton_hybrid_cache,
                    ],
                },
            },
            "multi": {
                "publication_group": MULTI_GROUP,
                "family_count": multi_families,
                "logical_rows": multi_rows,
                "candidates": ["immediate", "on_demand", "hybrid"],
                "cache_policies": {
                    "immediate": (
                        "fully eager; lookup cache disabled; cache ceiling 0"
                    ),
                    "on_demand": "all lazy; bounded cache ceiling 32",
                    "hybrid": (
                        "eager selector frozen below; bounded cache ceiling 32"
                    ),
                },
                "hybrid_boundary": {
                    "declared_before_measurement": True,
                    "hot_modulo": None,
                    "forbidden_extruder_hot_modulo": 5,
                    "eager_selector": {"duration_ticks_lte": MULTI_CUTOFF},
                    "cache_ceiling": multi_hybrid_cache,
                    "declared_eager_count": len(multi_eager),
                    "declared_lazy_count": multi_rows - len(multi_eager),
                    "eager_stable_ids": multi_eager,
                    "justification": (
                        f"T39 multi group has {multi_rows} relations across "
                        f"{multi_families} families "
                        f"with duration histogram {multi_hist}; median duration "
                        f"{multi_median}. duration_ticks_lte {MULTI_CUTOFF} "
                        f"partitions at the dominant short-cycle bucket "
                        f"({len(multi_eager)} eager / "
                        f"{multi_rows - len(multi_eager)} lazy), not T37 14/36 "
                        f"or T38 38/35. Cache ceiling {multi_hybrid_cache} is "
                        f"bounded by lazy rows."
                    ),
                },
                "partitions": {
                    "immediate": [multi_rows, 0, 0],
                    "on_demand": [0, multi_rows, multi_cache],
                    "hybrid": [
                        len(multi_eager),
                        multi_rows - len(multi_eager),
                        multi_hybrid_cache,
                    ],
                },
            },
            "card_aggregate": {
                "family_count": family_count,
                "logical_rows": relation_count,
                "candidates": ["immediate", "on_demand", "hybrid"],
                "partitions": {
                    "immediate": [relation_count, 0, 0],
                    "on_demand": [
                        0, relation_count, singleton_cache + multi_cache
                    ],
                    "hybrid": [
                        len(singleton_eager) + len(multi_eager),
                        relation_count - len(singleton_eager) - len(multi_eager),
                        singleton_hybrid_cache + multi_hybrid_cache,
                    ],
                },
                "note": (
                    "Card aggregate sums per-group policies independently; cache "
                    "ceiling is the sum of independently bounded group caches."
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
                "and must not copy T37/T38 envelopes as a T39 veto. Hard ceiling "
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
                "tools/build_t39_recipe_load_benchmark.py."
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
                "T38 card-level closing; T39 per-group cache ceiling is declared "
                "separately"
            )
        policy["t39_opening"][axis] = axis_payload
    policy["t39_opening"]["pending_runtime_axes"] = []
    policy["t39_opening"]["pending_runtime_axes_zero_fill"] = False
    return policy


def validate_policy(policy: dict) -> None:
    if policy.get("status") != "T39_MATERIALIZATION_CANDIDATE_POLICY":
        raise ValueError("T39 policy header drifted")
    scope = policy["scope"]
    if scope.get("production_logical_rows") != t39.production_relation_count():
        raise ValueError("T39 scope must match the production lock")
    if scope.get("family_count") != t39.production_family_count():
        raise ValueError("T39 scope family count must match the production lock")
    contract = policy["publication_group_contract"]
    singleton = contract["singleton"]["hybrid_boundary"]
    multi = contract["multi"]["hybrid_boundary"]
    if (singleton["declared_eager_count"], singleton["declared_lazy_count"]) in {
        (14, 36),
        (38, 35),
    }:
        raise ValueError("T39 must not copy T37/T38 hybrid partitions")
    if (multi["declared_eager_count"], multi["declared_lazy_count"]) in {
        (14, 36),
        (38, 35),
    }:
        raise ValueError("T39 must not copy T37/T38 hybrid partitions")


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
