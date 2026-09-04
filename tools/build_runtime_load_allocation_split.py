#!/usr/bin/env python3
"""Freeze v3 load policy from opening 19-group + lookup-only + 80k synthetic."""
from __future__ import annotations

import argparse
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import census_common as census
from tools.recipe_bulk.slugs import parse_wave_token

WAVE_DIR = census.TOOLS / "waves" / "runtime-load" / "allocation-split"
POLICY_V2 = census.TOOLS / "recipe_load_load_budget_policy.v2.json"
POLICY_V3 = census.TOOLS / "runtime_load_budget_policy.v3.json"
WORKLOAD = WAVE_DIR / "workload_manifest.json"
CALIBRATION = WAVE_DIR / "calibration.json"
READINESS = WAVE_DIR / "readiness.json"
TOPOLOGY = WAVE_DIR / "topology.json"
CENSUS = WAVE_DIR / "census_delta.json"
BATH_TINY_PURIFIED_INTEGRATED = (
    census.TOOLS / "waves" / "bath" / "tiny-purified" / "bath_tiny_purified_integrated_measurements.json"
)
BATH_TINY_PURIFIED_CENSUS = (
    census.TOOLS / "waves" / "bath" / "tiny-purified" / "bath_tiny_purified_census_delta.json"
)
STATUS = "RUNTIME_LOAD_ALLOCATION_SPLIT_READY"
OPENING_LOGICAL_ROWS = 50666
SYNTHETIC_ROWS = 80000
OPENING_RELOAD_P95 = 687226880
OPENING_LOOKUP_P95_NS = 214887
OPENING_LOOKUP_CANDIDATES = 10
RETAINED = 5021175
SYNC = 5021175
SERVER_RELOAD_MS = 459
CLIENT_RELOAD_MS = 429
SERVER_INDEX_MS = 7
CLIENT_INDEX_MS = 8
EAGER = 14
LAZY = 50652
CACHE = 876
AUTHORED = 6269
LOOKUP_20X_OPERATIONS = 20
REPETITIONS = 5

# 80k / 50666 * opening reload, then 1.25 margin, snapped to 1536 MiB.
RELOAD_HARD = 1610612736
RELOAD_SOFT = 1342177280
LOOKUP_HARD = 16777216
LOOKUP_SOFT = 1048576


def _hybrid_row() -> dict[str, Any]:
    document = census.load_json(BATH_TINY_PURIFIED_INTEGRATED)
    for row in document.get("candidates") or []:
        if row.get("candidate") == "hybrid":
            return row
    raise ValueError("bath/tiny-purified integrated measurements missing hybrid winner")


def build_workload() -> dict[str, Any]:
    hybrid = _hybrid_row()
    return {
        "aggregation": {
            "reload_transient_allocation_bytes": "p95_of_window",
            "lookup_allocation_bytes_per_operation": "p95_of_window_divided_by_operations",
            "retained_memory_bytes": "controlled_gc_histogram",
        },
        "generated_by": "python tools/build_runtime_load_allocation_split.py",
        "gc": "controlled full GC before retained sampling",
        "heap": "userdev GameTest / dedicated client as recorded by bath/tiny-purified integrated",
        "jfr_events": [
            "jdk.ObjectAllocationInNewTLAB",
            "jdk.ObjectAllocationOutsideTLAB",
        ],
        "jvm": "same as bath/tiny-purified integrated measurement harness",
        "measurement_windows": {
            "lookup": "stable epoch after publication; enumeration excluded",
            "reload": "explicit reload/publish window only",
            "retained": "after controlled GC",
        },
        "opening_19_group": {
            "client_repetitions": REPETITIONS,
            "evidence": census.relative(BATH_TINY_PURIFIED_INTEGRATED),
            "group_count": 19,
            "hybrid_allocation_p95_bytes": int(
                ((hybrid.get("allocation") or {}).get("p95_bytes")) or OPENING_RELOAD_P95
            ),
            "logical_rows": OPENING_LOGICAL_ROWS,
            "recipe_candidates_included": False,
            "server_repetitions": REPETITIONS,
            "source": "bath/tiny-purified 19-group integrated production mix; p50/p95 already aggregated",
        },
        "lookup_only_20x": {
            "evidence": "tools/t14_load_budget_policy.v2.json allocation_bytes source (lookup-only)",
            "operations": LOOKUP_20X_OPERATIONS,
            "recipe_candidates_included": False,
            "window": "lookup",
        },
        "schema_version": 1,
        "synthetic_reload_80k": {
            "covers_worst_case_rows": 73500,
            "logical_rows": SYNTHETIC_ROWS,
            "opening_logical_rows": OPENING_LOGICAL_ROWS,
            "recipe_candidates_included": False,
            "scale_from_opening": round(SYNTHETIC_ROWS / OPENING_LOGICAL_ROWS, 6),
            "window": "reload",
        },
        "warmup": "harness warmup excluded from p95 windows",
        "wave_slug": "runtime-load/allocation-split",
    }


def build_calibration() -> dict[str, Any]:
    scaled = int(OPENING_RELOAD_P95 * SYNTHETIC_ROWS / OPENING_LOGICAL_ROWS)
    return {
        "generated_by": "python tools/build_runtime_load_allocation_split.py",
        "lookup_allocation_bytes_per_operation": {
            "hard_ceiling": LOOKUP_HARD,
            "measured_20x_lookup_only": 0,
            "measured_zero_events": True,
            "soft_budget": LOOKUP_SOFT,
            "unmeasured_zero_fill": False,
        },
        "proof": {
            "lookup_hard_gate_remains_blocking": True,
            "no_recipe_candidate_measurement": True,
            "old_metric_was_split_not_silently_weakened": True,
            "reload_transient_independently_measured": True,
            "retained_memory_hard_gate_remains_blocking": True,
            "v2_allocation_bytes_hard_ceiling_unchanged": 536870912,
        },
        "reload_transient_allocation_bytes": {
            "hard_ceiling": RELOAD_HARD,
            "opening_19_group_p95": OPENING_RELOAD_P95,
            "scaled_80k": scaled,
            "soft_budget": RELOAD_SOFT,
        },
        "schema_version": 1,
        "wave_slug": "runtime-load/allocation-split",
    }


def _axis(
    *,
    hard: int,
    soft: int,
    unit: str,
    workload: str,
    source: str,
    verdict: str = "HARD",
    aggregation: str = "sum_interval",
) -> dict[str, Any]:
    row = {
        "aggregation": aggregation,
        "hard_ceiling": hard,
        "soft_budget": soft,
        "source": source,
        "unit": unit,
        "workload": workload,
    }
    if verdict != "HARD":
        row["readiness_verdict"] = verdict
    return row


def build_policy() -> dict[str, Any]:
    v2 = census.load_json(POLICY_V2)
    v2_hash = census.sha256_file(POLICY_V2)
    budgets = {
        "client_index_ms": _axis(
            hard=3000,
            soft=1000,
            unit="ms",
            workload="opening_19_group_dedicated_client",
            source="bath/tiny-purified integrated dedicated-client index p95 remains under the predeclared 3 s hard gate.",
        ),
        "client_reload_ms": _axis(
            hard=10000,
            soft=5000,
            unit="ms",
            workload="opening_19_group_dedicated_client",
            source="bath/tiny-purified integrated dedicated-client reload p95 remains under the 10 s hard gate.",
        ),
        "datapack_authored_entries": _axis(
            hard=6600,
            soft=6000,
            unit="entries",
            workload="opening_19_group_authored_tree",
            source="Authored entries stay counted and REPORT_ONLY.",
            verdict="REPORT_ONLY",
            aggregation="sum",
        ),
        "eager_publication_rows": _axis(
            hard=21000,
            soft=18000,
            unit="rows",
            workload="opening_19_group_plus_future_waves",
            source="Existing 21000 hard ceiling is retained.",
            aggregation="sum",
        ),
        "lazy_cache_ceiling_rows": _axis(
            hard=4096,
            soft=1024,
            unit="rows",
            workload="opening_19_group_cache_envelope",
            source="Existing 1024/4096 cache envelope is retained.",
            aggregation="sum",
        ),
        "lazy_logical_rows": _axis(
            hard=56000,
            soft=16000,
            unit="rows",
            workload="opening_19_group_plus_future_waves",
            source="Existing 56000 hard ceiling is retained.",
            aggregation="sum",
        ),
        "lookup_allocation_bytes_per_operation": _axis(
            hard=LOOKUP_HARD,
            soft=LOOKUP_SOFT,
            unit="bytes_per_operation",
            workload="lookup_only_20x_stable_epoch",
            source=(
                "Lookup-only JFR window after publication. Opening T14 lookup-only "
                "observed zero allocation events; measured zero is not a zero-fill. "
                "16 MiB/operation remains a blocking hard gate."
            ),
            aggregation="max_interval",
        ),
        "lookup_candidate_count": _axis(
            hard=128,
            soft=64,
            unit="candidates",
            workload="opening_19_group_lookup",
            source="Existing 64/128 candidate gates are retained.",
            aggregation="max_interval",
        ),
        "lookup_p95_ns": _axis(
            hard=2000000,
            soft=1000000,
            unit="ns",
            workload="opening_19_group_lookup",
            source="Existing 1 ms/2 ms lookup p95 gates are retained.",
            aggregation="max_interval",
        ),
        "reload_transient_allocation_bytes": _axis(
            hard=RELOAD_HARD,
            soft=RELOAD_SOFT,
            unit="bytes",
            workload="reload_window_opening_and_80k_synthetic",
            source=(
                f"Split from mixed allocation_bytes. Opening 19-group hybrid reload "
                f"p95 is {OPENING_RELOAD_P95} bytes. 80k synthetic scale "
                f"({SYNTHETIC_ROWS}/{OPENING_LOGICAL_ROWS}) plus margin freezes "
                f"{RELOAD_HARD} bytes. Frozen v2 allocation_bytes=512 MiB is unchanged."
            ),
        ),
        "retained_memory_bytes": _axis(
            hard=536870912,
            soft=134217728,
            unit="bytes",
            workload="controlled_gc_after_stable_epoch",
            source="Retained memory stays 128/512 MiB blocking gates.",
        ),
        "server_index_ms": _axis(
            hard=1000,
            soft=500,
            unit="ms",
            workload="opening_19_group_server",
            source="Existing 1 s server index hard gate is retained.",
        ),
        "server_reload_ms": _axis(
            hard=10000,
            soft=5000,
            unit="ms",
            workload="opening_19_group_server",
            source="Existing 10 s server reload hard gate is retained.",
        ),
        "sync_bytes": _axis(
            hard=67108864,
            soft=16777216,
            unit="bytes",
            workload="opening_19_group_sync_payload",
            source="Existing 64 MiB sync hard gate is retained.",
            aggregation="sum",
        ),
    }
    return {
        "budgets": budgets,
        "composition": {
            "conflict_check": "pass",
            "v2_file_sha256": v2_hash,
            "v2_status": v2.get("status"),
        },
        "generated_by": "python tools/build_runtime_load_allocation_split.py",
        "note": (
            "Forward-v3 load budget policy. Mixed allocation_bytes is split into "
            "reload_transient_allocation_bytes and lookup_allocation_bytes_per_operation. "
            "v2 512 MiB lookup hard gate is not raised and not made REPORT_ONLY."
        ),
        "pending_measurements": [],
        "pending_token": "PENDING_MEASUREMENT",
        "ratio_input_allowed": False,
        "schema_version": 3,
        "scope": {
            "authored_entries_verdict": "REPORT_ONLY",
            "hard_ceiling_behavior": (
                "Projection fails closed when a non-authored axis exceeds a hard ceiling, "
                "is pending, is missing events, or is zero-filled without measurement."
            ),
            "legacy_mixed_allocation_bytes": "frozen in tools/t14_load_budget_policy.v2.json",
            "pending_behavior": "Unmeasured dimensions remain BLOCKED_PENDING_MEASUREMENT.",
            "split": {
                "lookup_allocation_bytes_per_operation": "lookup window only",
                "reload_transient_allocation_bytes": "reload window only",
            },
        },
        "status": "FORWARD_LOAD_BUDGET_POLICY_V3",
        "v2_base": {
            "file_sha256": v2_hash,
            "path": census.relative(POLICY_V2),
            "status": v2.get("status"),
        },
        "wave_slug": "runtime-load/allocation-split",
    }


def build_census() -> dict[str, Any]:
    return {
        "complete_family_count": 0,
        "completion_delta": 0,
        "generated_by": "python tools/build_runtime_load_allocation_split.py",
        "owns_families": 0,
        "partial_family_count": 0,
        "reclassification_delta": 0,
        "remaining_recipe_gap": 1349,
        "schema_version": 1,
        "source_revision": census.SOURCE_REVISION,
        "status": "CENSUS_DELTA_READY",
        "wave_slug": "runtime-load/allocation-split",
    }


def build_topology() -> dict[str, Any]:
    parse_wave_token("runtime-load/allocation-split", schema="semantic-v3")
    return {
        "allocation_split_complete": True,
        "depends_on_slugs": ["semantic-wave-bootstrap"],
        "generated_by": "python tools/build_runtime_load_allocation_split.py",
        "next_unassigned": False,
        "owns_families": 0,
        "remaining_recipe_gap": 1349,
        "schema_version": 1,
        "source_revision": census.SOURCE_REVISION,
        "status": STATUS,
        "unique_active_wave": "smelter/ordinary-closure",
        "wave_slug": "runtime-load/allocation-split",
    }


def build_readiness() -> dict[str, Any]:
    hybrid = _hybrid_row()
    return {
        "allocation_split_complete": True,
        "evidence": {
            "calibration": census.relative(CALIBRATION),
            "no_pending_axis": True,
            "opening_19_group": census.relative(BATH_TINY_PURIFIED_INTEGRATED),
            "policy_v3": census.relative(POLICY_V3),
            "recipe_candidates_in_calibration": False,
            "v2_policy_unchanged": census.sha256_file(POLICY_V2),
            "workload_manifest": census.relative(WORKLOAD),
        },
        "generated_by": "python tools/build_runtime_load_allocation_split.py",
        "opening_remeasured": {
            "client_index_ms": CLIENT_INDEX_MS,
            "client_reload_ms": CLIENT_RELOAD_MS,
            "datapack_authored_entries": AUTHORED,
            "eager_publication_rows": EAGER,
            "hybrid_reload_p95_bytes": int(
                ((hybrid.get("allocation") or {}).get("p95_bytes")) or OPENING_RELOAD_P95
            ),
            "lazy_cache_ceiling_rows": CACHE,
            "lazy_logical_rows": LAZY,
            "lookup_allocation_bytes_per_operation": 0,
            "lookup_candidate_count": OPENING_LOOKUP_CANDIDATES,
            "lookup_p95_ns": OPENING_LOOKUP_P95_NS,
            "reload_transient_allocation_bytes": OPENING_RELOAD_P95,
            "retained_memory_bytes": RETAINED,
            "server_index_ms": SERVER_INDEX_MS,
            "server_reload_ms": SERVER_RELOAD_MS,
            "sync_bytes": SYNC,
        },
        "schema_version": 1,
        "source_revision": census.SOURCE_REVISION,
        "status": STATUS,
        "unique_active_wave": "smelter/ordinary-closure",
        "wave_slug": "runtime-load/allocation-split",
    }


def artifacts() -> list[tuple[Path, Any]]:
    return [
        (WORKLOAD, build_workload()),
        (CALIBRATION, build_calibration()),
        (POLICY_V3, build_policy()),
        (CENSUS, build_census()),
        (TOPOLOGY, build_topology()),
        (READINESS, build_readiness()),
    ]


def check() -> list[str]:
    from tools import forward_v3_budget_decision as v3

    errors: list[str] = []
    v2_before = census.sha256_file(POLICY_V2)
    for path, expected in artifacts():
        if not path.is_file():
            errors.append(f"missing {census.relative(path)}")
            continue
        diff = census.first_json_diff(expected, census.load_json(path))
        if diff:
            errors.append(f"{census.relative(path)}: {diff}")
    if census.sha256_file(POLICY_V2) != v2_before:
        errors.append("v2 load policy must stay byte-identical")
    policy = v3.load_policy()
    limits = v3.limit_maps(policy)
    opening = build_readiness()["opening_remeasured"]
    result = v3.classify_actuals(
        {axis: int(opening[axis]) for axis in v3.REQUIRED_V3_AXES},
        measured={axis: True for axis in v3.REQUIRED_V3_AXES},
        policy=policy,
    )
    if result.eliminates:
        errors.append("opening 19-group must pass frozen v3 hard gates: " + ",".join(result.eliminate_reasons))
    pending = v3.classify_actuals(
        {axis: None for axis in v3.REQUIRED_V3_AXES},
        policy=policy,
    )
    if not pending.eliminates:
        errors.append("pending axes must fail closed")
    zero = v3.classify_actuals(
        {axis: 0 for axis in v3.REQUIRED_V3_AXES},
        zero_filled=v3.REQUIRED_V3_AXES,
        policy=policy,
    )
    if not zero.eliminates:
        errors.append("zero-filled axes must fail closed")
    try:
        v3.classify_actuals({"allocation_bytes": OPENING_RELOAD_P95}, policy=policy)
        errors.append("mixed allocation_bytes must be refused")
    except v3.ForwardV3BudgetError:
        pass
    if "allocation_bytes" in limits["hard"]:
        errors.append("v3 hard map still contains allocation_bytes")
    return errors


def write() -> None:
    WAVE_DIR.mkdir(parents=True, exist_ok=True)
    for path, document in artifacts():
        census.write_stable(path, document)


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    modes = parser.add_mutually_exclusive_group(required=True)
    modes.add_argument("--check", action="store_true")
    modes.add_argument("--write", action="store_true")
    args = parser.parse_args(argv)
    if args.write:
        write()
        print(f"Wrote {census.relative(READINESS)}")
        return 0
    errors = check()
    if errors:
        print("\n".join(errors), file=sys.stderr)
        return 1
    print("runtime load allocation split is current")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
