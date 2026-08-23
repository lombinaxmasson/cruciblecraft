#!/usr/bin/env python3
"""Build T35 canonical load baseline for 1.x census aggregation.

Treats T14 policy as numerical authority, pins T30/T31 publication opening,
records T27 opening as superseded history, separates scale topology from route
traversal, and labels F005 canonical assessment as deferred release remeasurement.
"""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from tools import t27_common as common  # noqa: E402
from tools import t31_common as t31  # noqa: E402
from tools.recipe_load_projection import BUDGET_CONTRACT  # noqa: E402

OUTPUT = common.TOOLS / "t35_load_baseline.json"
BUILDER = Path(__file__).resolve()
T14 = common.TOOLS / "t14_load_budget_policy.json"
DELTA = common.TOOLS / "t30_publication_delta.json"
T27_OPENING = common.TOOLS / "t27_opening_snapshot.json"
T30_LOAD = common.TOOLS / "t30_load_projection.json"
T31_LOAD = common.TOOLS / "t31_load_report.json"
SCALE = common.TOOLS / "t31_scale_recheck.json"
MANIFEST = common.TOOLS / "t24_workload_manifest.json"
REPORT = t31.REPORT
REPORT_OWNED = ("currentness",)
NS_PER_MS = 1_000_000.0
T14_1X = "tools/full_verification_report.json#t14_load_acceptance.measurements.selected_1x"
ZERO_DELTA = {"min": 0, "max": 0}
PUBLICATION_DELTA = {"eager": 0, "lazy": 0, "logical": 0}
T14_AXIS_IDS = tuple(BUDGET_CONTRACT)
DOMAIN_BY_AXIS: dict[str, str] = {
    "datapack_authored_entries": "recipe",
    "eager_publication_rows": "recipe",
    "lazy_logical_rows": "recipe",
    "lazy_cache_ceiling_rows": "recipe",
    "sync_bytes": "runtime_perf",
    "server_reload_ms": "runtime_perf",
    "server_index_ms": "runtime_perf",
    "client_reload_ms": "runtime_perf",
    "client_index_ms": "runtime_perf",
    "retained_memory_bytes": "runtime_perf",
    "allocation_bytes": "runtime_perf",
    "lookup_p95_ns": "runtime_perf",
    "lookup_candidate_count": "runtime_perf",
}
MEASUREMENT_METHOD: dict[str, str] = {
    "datapack_authored_entries": "t30_load_projection_upper_bound",
    "eager_publication_rows": "t30_publication_opening",
    "lazy_logical_rows": "t30_publication_opening",
    "lazy_cache_ceiling_rows": "selected_1x_lookup_trace_cache_ceiling",
    "sync_bytes": "selected_1x_sync_bytes",
    "server_reload_ms": "selected_1x_server_reload_p95_ns",
    "server_index_ms": "selected_1x_server_index_p95_ns",
    "client_reload_ms": "selected_1x_dedicated_client_reexpansion_p95_ns",
    "client_index_ms": "selected_1x_dedicated_client_index_p95_ns",
    "retained_memory_bytes": "selected_1x_retained_total_bytes_p50",
    "allocation_bytes": "selected_1x_jfr_allocation_bytes_p50",
    "lookup_p95_ns": "selected_1x_lookup_p95_ns",
    "lookup_candidate_count": "selected_1x_lookup_candidates_p95",
}
INCOMPATIBLE_PAIRS = (
    ("topology_pipes_total", "item_route_visited_max"),
)


def _verdict(value: int | float | None, soft: int | float | None, hard: int | float | None) -> str | None:
    if value is None:
        return common.PENDING_LOAD_VERDICT
    if hard is not None and value > hard:
        return "HARD_CEILING_EXCEEDED"
    if soft is not None and value > soft:
        return "SOFT_BUDGET_EXCEEDED"
    return "PASS"


def _comparable_to(
    canonical_id: str,
    *,
    domain: str,
    unit: str,
    aggregation: str,
    measurement_method: str,
    scale: str,
) -> list[str]:
    peers = [
        axis_id
        for axis_id in T14_AXIS_IDS
        if DOMAIN_BY_AXIS[axis_id] == domain
        and BUDGET_CONTRACT[axis_id][0] == unit
        and BUDGET_CONTRACT[axis_id][1] == aggregation
        and MEASUREMENT_METHOD[axis_id] == measurement_method
        and axis_id != canonical_id
    ]
    if canonical_id in T14_AXIS_IDS:
        return sorted(peers)
    return []


def _axis_record(
    canonical_id: str,
    *,
    domain: str,
    unit: str,
    aggregation: str,
    measurement_method: str,
    scale: str,
    opening_base: Any,
    projected_delta: dict[str, int],
    soft_budget: int | float | None,
    hard_ceiling: int | float | None,
    evidence_pointer: str,
    comparable_to: list[str] | None = None,
    diagnostic: bool = False,
    extra: dict[str, Any] | None = None,
) -> dict[str, Any]:
    measured = opening_base
    if isinstance(opening_base, dict):
        measured = opening_base
    elif projected_delta["max"] == 0 and projected_delta["min"] == 0:
        measured = opening_base
    else:
        measured = (
            opening_base + projected_delta["max"]
            if isinstance(opening_base, (int, float))
            else opening_base
        )
    verdict = None if diagnostic else _verdict(
        measured if isinstance(measured, (int, float)) else None,
        soft_budget,
        hard_ceiling,
    )
    row: dict[str, Any] = {
        "aggregation": aggregation,
        "canonical_id": canonical_id,
        "comparable_to": sorted(comparable_to or _comparable_to(
            canonical_id,
            domain=domain,
            unit=unit,
            aggregation=aggregation,
            measurement_method=measurement_method,
            scale=scale,
        )),
        "domain": domain,
        "evidence_pointer": evidence_pointer,
        "hard_ceiling": hard_ceiling,
        "measurement_method": measurement_method,
        "opening_base": opening_base,
        "projected_delta": projected_delta,
        "scale": scale,
        "soft_budget": soft_budget,
        "unit": unit,
        "verdict": verdict,
        "zero_filled": False,
    }
    if diagnostic:
        row["diagnostic"] = True
    if extra:
        row.update(extra)
    return row


def _validate_policy(policy: dict[str, Any]) -> None:
    budgets = policy.get("budgets") or {}
    if sorted(budgets) != sorted(T14_AXIS_IDS):
        raise ValueError("T14 policy budget keys do not match the 13-axis contract")
    for axis_id, spec in budgets.items():
        unit, aggregation = BUDGET_CONTRACT[axis_id]
        if spec.get("unit") != unit or spec.get("aggregation") != aggregation:
            raise ValueError(
                f"{axis_id}: policy unit/aggregation mismatch with BUDGET_CONTRACT"
            )


def _validate_opening(current: dict[str, int], *, t30_opening: dict[str, int], t27_opening: dict[str, int]) -> None:
    if current != t30_opening:
        raise ValueError("opening_publication must match tools/t30_publication_delta.json")
    averaged = {
        "logical": (t27_opening["logical"] + t30_opening["logical"]) // 2,
        "eager": (t27_opening["eager"] + t30_opening["eager"]) // 2,
        "lazy": (t27_opening["lazy"] + t30_opening["lazy"]) // 2,
    }
    if current == averaged:
        raise ValueError("opening_publication must not average T27 and T30/T31 values")


def _validate_axes(axes: dict[str, Any]) -> None:
    t14_axes = {key: value for key, value in axes.items() if key in T14_AXIS_IDS}
    if len(t14_axes) != len(T14_AXIS_IDS):
        missing = sorted(set(T14_AXIS_IDS) - set(t14_axes))
        extra = sorted(set(t14_axes) - set(T14_AXIS_IDS))
        raise ValueError(f"T14 axis count mismatch missing={missing} extra={extra}")
    logical = axes["logical_publication_rows"]
    if logical.get("hard_ceiling") is not None or logical.get("soft_budget") is not None:
        raise ValueError("logical_publication_rows must not borrow eager soft/hard budgets")
    if logical.get("verdict") == "PASS" and logical.get("diagnostic") is not True:
        raise ValueError("logical_publication_rows must remain diagnostic")
    for left, right in INCOMPATIBLE_PAIRS:
        left_row = axes[left]
        right_row = axes[right]
        if right in left_row.get("comparable_to", []) or left in right_row.get("comparable_to", []):
            raise ValueError(f"incompatible comparability declared between {left} and {right}")
    f005 = axes["f005_release_scale"]
    if f005.get("canonical_assessment") != "DEFERRED_RELEASE_REMEASUREMENT":
        raise ValueError("F005 canonical assessment must be DEFERRED_RELEASE_REMEASUREMENT")
    if f005.get("verdict") == "PASS":
        raise ValueError("F005 canonical verdict must not be PASS")
    for metric in (f005.get("metrics") or {}).values():
        if metric.get("verdict") == "PASS":
            raise ValueError("F005 metric verdict must not synthesize PASS")
    for axis_id, row in axes.items():
        if row.get("zero_filled"):
            raise ValueError(f"{axis_id} must not zero-fill pending measurements")


def assert_comparable(axis_a: str, axis_b: str, axes: dict[str, Any]) -> None:
    if axis_a == axis_b:
        return
    if (axis_a, axis_b) in INCOMPATIBLE_PAIRS or (axis_b, axis_a) in INCOMPATIBLE_PAIRS:
        raise ValueError(f"{axis_a} and {axis_b} are incompatible for comparison")
    left = axes[axis_a]
    right = axes[axis_b]
    if (
        left.get("unit") != right.get("unit")
        or left.get("aggregation") != right.get("aggregation")
        or left.get("scale") != right.get("scale")
        or left.get("measurement_method") != right.get("measurement_method")
    ):
        raise ValueError(f"{axis_a} and {axis_b} lack matching probe/unit/aggregation/scale")
    if axis_b not in left.get("comparable_to", []) and axis_a not in right.get("comparable_to", []):
        raise ValueError(f"{axis_a} and {axis_b} are not declared comparable")


def _t14_axes(
    policy: dict[str, Any],
    *,
    opening: dict[str, int],
    delta_doc: dict[str, Any],
    t30_load: dict[str, Any],
    selected: dict[str, Any],
) -> dict[str, Any]:
    budgets = policy["budgets"]
    datapack_base = ((t30_load.get("t14_axes") or {}).get("datapack_authored_entries") or {})
    datapack_opening = (
        int(datapack_base["projected"])
        if datapack_base.get("projected") is not None
        else None
    )
    axes: dict[str, Any] = {}
    for axis_id in T14_AXIS_IDS:
        spec = budgets[axis_id]
        unit = spec["unit"]
        aggregation = spec["aggregation"]
        soft = spec["soft_budget"]
        hard = spec["hard_ceiling"]
        if axis_id == "eager_publication_rows":
            opening_base = int(opening["eager"])
            evidence = "tools/t30_publication_delta.json#opening_publication.eager"
        elif axis_id == "lazy_logical_rows":
            opening_base = int(opening["lazy"])
            evidence = "tools/t30_publication_delta.json#opening_publication.lazy"
        elif axis_id == "datapack_authored_entries":
            opening_base = datapack_opening
            evidence = "tools/t30_load_projection.json#t14_axes.datapack_authored_entries.projected"
        elif axis_id == "lazy_cache_ceiling_rows":
            opening_base = 512
            evidence = T14_1X + ".lookup_traces cache.ceiling"
        elif axis_id == "sync_bytes":
            opening_base = selected.get("sync_bytes")
            evidence = T14_1X + ".sync_bytes"
        elif axis_id == "server_reload_ms":
            opening_base = (
                selected["server_reload_p95_ns"] / NS_PER_MS
                if selected.get("server_reload_p95_ns") is not None
                else None
            )
            evidence = T14_1X + ".server_reload_p95_ns / 1e6"
        elif axis_id == "server_index_ms":
            opening_base = (
                selected["server_index_p95_ns"] / NS_PER_MS
                if selected.get("server_index_p95_ns") is not None
                else None
            )
            evidence = T14_1X + ".server_index_p95_ns / 1e6"
        elif axis_id == "client_reload_ms":
            opening_base = (
                selected["dedicated_client_reexpansion_p95_ns"] / NS_PER_MS
                if selected.get("dedicated_client_reexpansion_p95_ns") is not None
                else None
            )
            evidence = T14_1X + ".dedicated_client_reexpansion_p95_ns / 1e6"
        elif axis_id == "client_index_ms":
            opening_base = (
                selected["dedicated_client_index_p95_ns"] / NS_PER_MS
                if selected.get("dedicated_client_index_p95_ns") is not None
                else None
            )
            evidence = T14_1X + ".dedicated_client_index_p95_ns / 1e6"
        elif axis_id == "retained_memory_bytes":
            opening_base = selected.get("retained_total_bytes_p50")
            evidence = T14_1X + ".retained_total_bytes_p50"
        elif axis_id == "allocation_bytes":
            opening_base = selected.get("jfr_allocation_bytes_p50")
            evidence = T14_1X + ".jfr_allocation_bytes_p50"
        elif axis_id == "lookup_p95_ns":
            opening_base = selected.get("lookup_p95_ns")
            evidence = T14_1X + ".lookup_p95_ns"
        elif axis_id == "lookup_candidate_count":
            opening_base = selected.get("lookup_candidates_p95")
            evidence = T14_1X + ".lookup_candidates_p95"
        else:
            raise ValueError(f"unexpected T14 axis {axis_id}")
        axes[axis_id] = _axis_record(
            axis_id,
            domain=DOMAIN_BY_AXIS[axis_id],
            unit=unit,
            aggregation=aggregation,
            measurement_method=MEASUREMENT_METHOD[axis_id],
            scale="selected_1x",
            opening_base=opening_base,
            projected_delta=ZERO_DELTA,
            soft_budget=soft,
            hard_ceiling=hard,
            evidence_pointer=evidence,
        )
    axes["logical_publication_rows"] = _axis_record(
        "logical_publication_rows",
        domain="recipe",
        unit="rows",
        aggregation="sum",
        measurement_method="t30_publication_opening",
        scale="selected_1x",
        opening_base=int(opening["logical"]),
        projected_delta=ZERO_DELTA,
        soft_budget=None,
        hard_ceiling=None,
        evidence_pointer="tools/t30_publication_delta.json#opening_publication.logical",
        comparable_to=[],
        diagnostic=True,
        extra={
            "verdict": "DIAGNOSTIC_ONLY",
            "note": "Diagnostic total; eager hard ceiling must not gate logical publication",
        },
    )
    return axes


def _scale_axes(scale: dict[str, Any], manifest: dict[str, Any]) -> dict[str, Any]:
    current = scale.get("current") or {}
    target = current.get("target") or {}
    stress = current.get("stress") or {}
    target_manifest = manifest["scenarios"]["target"]["derived"]["pipes_total"]
    stress_manifest = manifest["scenarios"]["stress"]["derived"]["pipes_total"]
    target_identity = (target.get("identity") or {}).get("pipes_total")
    stress_identity = (stress.get("identity") or {}).get("pipes_total")
    if target_identity != target_manifest or stress_identity != stress_manifest:
        raise ValueError("scale identity pipes_total must match t24 workload manifest")
    return {
        "topology_pipes_total": _axis_record(
            "topology_pipes_total",
            domain="scale_topology",
            unit="pipes",
            aggregation="point",
            measurement_method="T31ScaleHarness.identity.pipes_total",
            scale="target_stress",
            opening_base={"target": target_manifest, "stress": stress_manifest},
            projected_delta=ZERO_DELTA,
            soft_budget=None,
            hard_ceiling=None,
            evidence_pointer="tools/t24_workload_manifest.json#scenarios.target/stress.derived.pipes_total",
            comparable_to=[],
            extra={
                "measured": {
                    "target": target_identity,
                    "stress": stress_identity,
                },
                "verdict": "DIAGNOSTIC_ONLY",
            },
        ),
        "item_route_visited_max": _axis_record(
            "item_route_visited_max",
            domain="scale_route",
            unit="visited_nodes",
            aggregation="max_of_samples",
            measurement_method="ItemPipeNetworkTraversal.discover.visited_max",
            scale="target_stress",
            opening_base={
                "target": target.get("visited_max"),
                "stress": stress.get("visited_max"),
            },
            projected_delta=ZERO_DELTA,
            soft_budget=None,
            hard_ceiling=None,
            evidence_pointer="tools/t31_scale_recheck.json#current.target/stress.visited_max",
            comparable_to=[],
            extra={
                "network_sync_bytes": {
                    "target": target.get("network_sync_bytes"),
                    "stress": stress.get("network_sync_bytes"),
                },
                "verdict": "DIAGNOSTIC_ONLY",
            },
        ),
    }


def _difference_or_none(after: Any, before: Any) -> int | float | None:
    if not isinstance(after, (int, float)) or not isinstance(before, (int, float)):
        return None
    return after - before


def _f005_block(scale: dict[str, Any]) -> dict[str, Any]:
    historical = dict(scale.get("f005") or {})
    current = scale.get("current") or {}
    target = current.get("target") or {}
    stress = current.get("stress") or {}
    metrics = {
        "network_sync_bytes": {
            "aggregation": "max_of_samples",
            "evidence_pointer": "tools/t31_scale_recheck.json#current.target/stress.network_sync_bytes",
            "measurement_method": "T31ScaleHarness.network_sync_bytes",
            "opening_base": {
                "target": target.get("network_sync_bytes"),
                "stress": stress.get("network_sync_bytes"),
            },
            "scale": "target_stress",
            "unit": "bytes",
            "verdict": "DEFERRED_RELEASE_REMEASUREMENT",
            "zero_filled": False,
        },
        "retained_heap_delta_bytes": {
            "aggregation": "max_of_samples",
            "evidence_pointer": "tools/t31_scale_recheck.json#current.target/stress.retained_heap_after-before",
            "measurement_method": "T31ScaleHarness.retained_heap_delta",
            "opening_base": {
                "target": _difference_or_none(
                    target.get("retained_heap_after_bytes"),
                    target.get("retained_heap_before_bytes"),
                ),
                "stress": _difference_or_none(
                    stress.get("retained_heap_after_bytes"),
                    stress.get("retained_heap_before_bytes"),
                ),
            },
            "scale": "target_stress",
            "unit": "bytes",
            "verdict": "DEFERRED_RELEASE_REMEASUREMENT",
            "zero_filled": False,
        },
        "tick_wall_clock_p95_nanos": {
            "aggregation": "max_of_samples",
            "evidence_pointer": "tools/t31_scale_recheck.json#current.target/stress.tick_wall_clock_p95_nanos",
            "measurement_method": "T31ScaleHarness.tick_wall_clock_p95_nanos",
            "opening_base": {
                "target": target.get("tick_wall_clock_p95_nanos"),
                "stress": stress.get("tick_wall_clock_p95_nanos"),
            },
            "scale": "target_stress",
            "unit": "ns",
            "verdict": "DEFERRED_RELEASE_REMEASUREMENT",
            "zero_filled": False,
        },
    }
    return _axis_record(
        "f005_release_scale",
        domain="scale_f005",
        unit="mixed",
        aggregation="max_of_samples",
        measurement_method="T31ScaleHarness.release_scale_probe",
        scale="target_stress",
        opening_base=None,
        projected_delta=ZERO_DELTA,
        soft_budget=None,
        hard_ceiling=None,
        evidence_pointer="tools/t31_scale_recheck.json#f005",
        comparable_to=[],
        extra={
            "canonical_assessment": "DEFERRED_RELEASE_REMEASUREMENT",
            "historical": {
                "artifact": historical.get("current_artifact"),
                "current_status": historical.get("current_status"),
                "historical_disposition": historical.get("historical_disposition"),
                "historical_evidence_artifact": historical.get("historical_evidence_artifact"),
                "historical_owner": historical.get("historical_owner"),
                "measured": historical.get("measured"),
                "network_sync_measured": historical.get("network_sync_measured"),
                "retained_memory_measured": historical.get("retained_memory_measured"),
                "wall_clock_measured": historical.get("wall_clock_measured"),
                "zero_filled": historical.get("zero_filled"),
            },
            "metrics": metrics,
            "owner": "future player release card",
            "verdict": "DEFERRED_RELEASE_REMEASUREMENT",
        },
    )


def _owned_inputs() -> dict[str, str]:
    paths = [
        BUILDER,
        T14,
        DELTA,
        T27_OPENING,
        T30_LOAD,
        T31_LOAD,
        SCALE,
        MANIFEST,
    ]
    if REPORT.is_file():
        paths.append(REPORT)
    return {common.relative(path): common.sha256_file(path) for path in paths}


def build() -> dict[str, Any]:
    policy = common.load_json(T14)
    delta = common.load_json(DELTA)
    t27 = common.load_json(T27_OPENING)
    t30_load = common.load_json(T30_LOAD)
    scale = common.load_json(SCALE)
    manifest = common.load_json(MANIFEST)
    report = common.load_json(REPORT) if REPORT.is_file() else {}
    _validate_policy(policy)
    opening = dict(delta["opening_publication"])
    t27_opening = dict(t27["publication"]["current"])
    _validate_opening(opening, t30_opening=opening, t27_opening=t27_opening)
    selected = (
        ((report.get("t14_load_acceptance") or {}).get("measurements") or {}).get(
            "selected_1x"
        )
        or {}
    )
    axes = _t14_axes(
        policy,
        opening=opening,
        delta_doc=delta,
        t30_load=t30_load,
        selected=selected,
    )
    axes.update(_scale_axes(scale, manifest))
    axes["f005_release_scale"] = _f005_block(scale)
    _validate_axes(axes)
    owned_inputs = _owned_inputs()
    return {
        "authorities": {
            "budget_policy": "tools/t14_load_budget_policy.json",
            "opening_publication": "tools/t30_publication_delta.json#opening_publication",
            "performance_1x": T14_1X,
            "scale_current": "tools/t31_scale_recheck.json#current",
            "workload_manifest": "tools/t24_workload_manifest.json",
        },
        "axes": axes,
        "card": "T35",
        "currentness": {"owned_inputs": dict(owned_inputs)},
        "generated_by": "python tools/build_t35_load_baseline.py --write",
        "opening_publication": opening,
        "owned_inputs": owned_inputs,
        "publication_delta": dict(PUBLICATION_DELTA),
        "schema_version": 1,
        "status": "T35_LOAD_BASELINE_READY",
        "status_owner": "build_t35_load_baseline",
        "supersedes": [
            {
                "artifact": "tools/t27_opening_snapshot.json#publication.current",
                "label": "T27 opening snapshot",
                "note": "superseded historical snapshot for current 1.x planning; values are not averaged",
                "opening": t27_opening,
                "reason": "T30/T31 current planning publication opening is authoritative",
            }
        ],
        "t14_axis_count": len(T14_AXIS_IDS),
        "t14_axis_ids": list(T14_AXIS_IDS),
    }


def write() -> dict[str, Any]:
    document = build()
    common.write_stable(OUTPUT, document)
    return document


def validate_inputs() -> list[str]:
    errors: list[str] = []
    required = [T14, DELTA, T27_OPENING, T30_LOAD, SCALE, MANIFEST]
    for path in required:
        if not path.is_file():
            errors.append(f"missing input: {common.relative(path)}")
    if errors:
        return errors
    try:
        build()
    except (OSError, ValueError, json.JSONDecodeError, KeyError, TypeError) as exc:
        errors.append(str(exc))
    return errors


def check() -> list[str]:
    if not OUTPUT.is_file():
        return [f"missing generated file: {common.relative(OUTPUT)}"]
    input_errors = validate_inputs()
    if input_errors:
        return input_errors
    expected = build()
    actual = json.loads(OUTPUT.read_text(encoding="utf-8"))
    for key in REPORT_OWNED:
        expected.pop(key, None)
        actual.pop(key, None)
    if common.stable_json(expected) != common.stable_json(actual):
        return [f"{common.relative(OUTPUT)} is stale"]
    return []


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args(argv)
    if bool(args.write) == bool(args.check):
        parser.error("choose exactly one of --write, --check")
    try:
        if args.write:
            document = write()
            print(
                f"wrote {common.relative(OUTPUT)} "
                f"status={document.get('status')} "
                f"t14_axes={document.get('t14_axis_count')}"
            )
            return 0
        errors = check()
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        print(f"{common.relative(OUTPUT)} is current")
        return 0
    except (OSError, ValueError, json.JSONDecodeError, KeyError, TypeError) as exc:
        print(str(exc), file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
