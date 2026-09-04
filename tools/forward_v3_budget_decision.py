#!/usr/bin/env python3
"""Forward-v3 materialization budget classification.

allocation_bytes is not a v3 axis. Reload transient and lookup allocation are
independent blocking gates. Unmeasured, missing-event, and zero-filled values
fail closed. datapack_authored_entries stays REPORT_ONLY.
"""
from __future__ import annotations

from typing import Any, Iterable, Mapping

from tools.forward_v2_budget_decision import (
    AUTHORED_AXIS,
    STATUS_HARD,
    STATUS_PASS,
    STATUS_PENDING,
    STATUS_REPORT_ONLY,
    STATUS_SOFT,
    VERDICT_HARD,
    VERDICT_REPORT_ONLY,
    AxisClassification,
    CandidateBudgetResult,
    axis_verdict,
    classify_axis,
)
from tools import census_common as census

LOAD_POLICY_V3 = census.TOOLS / "runtime_load_budget_policy.v3.json"
SPLIT_AXES = (
    "reload_transient_allocation_bytes",
    "lookup_allocation_bytes_per_operation",
)
FORBIDDEN_V3_AXES = frozenset({"allocation_bytes"})
REQUIRED_V3_AXES = (
    "reload_transient_allocation_bytes",
    "lookup_allocation_bytes_per_operation",
    "retained_memory_bytes",
    "sync_bytes",
    "server_reload_ms",
    "client_reload_ms",
    "server_index_ms",
    "client_index_ms",
    "lookup_p95_ns",
    "lookup_candidate_count",
    "eager_publication_rows",
    "lazy_logical_rows",
    "lazy_cache_ceiling_rows",
    "datapack_authored_entries",
)


class ForwardV3BudgetError(ValueError):
    """v3 budget decision failed closed."""


def load_policy(path=None) -> dict[str, Any]:
    document = census.load_json(path or LOAD_POLICY_V3)
    if document.get("status") != "FORWARD_LOAD_BUDGET_POLICY_V3":
        raise ForwardV3BudgetError("forward-v3 load budget policy status drifted")
    if document.get("schema_version") != 3:
        raise ForwardV3BudgetError("forward-v3 load budget policy schema drifted")
    budgets = document.get("budgets") or {}
    for axis in FORBIDDEN_V3_AXES:
        if axis in budgets:
            raise ForwardV3BudgetError(
                "v3 policy must not carry mixed allocation_bytes; the metric was split"
            )
    for axis in REQUIRED_V3_AXES:
        if axis not in budgets:
            raise ForwardV3BudgetError(f"v3 policy missing required axis {axis}")
    return document


def policy_sha256(path=None) -> str:
    target = path or LOAD_POLICY_V3
    if not target.is_file():
        raise FileNotFoundError(target)
    return census.sha256_file(target)


def limit_maps(policy: Mapping[str, Any] | None = None) -> dict[str, dict[str, Any]]:
    document = policy if policy is not None else load_policy()
    budgets = document.get("budgets") or {}
    hard: dict[str, int] = {}
    soft: dict[str, int] = {}
    verdicts: dict[str, str] = {}
    units: dict[str, str] = {}
    workloads: dict[str, str] = {}
    for axis, spec in budgets.items():
        if not isinstance(spec, dict):
            continue
        hard[axis] = int(spec["hard_ceiling"])
        soft[axis] = int(spec["soft_budget"])
        verdicts[axis] = axis_verdict(spec)
        units[axis] = str(spec.get("unit") or "")
        workloads[axis] = str(spec.get("workload") or "")
        if not units[axis] or not workloads[axis]:
            raise ForwardV3BudgetError(f"{axis} must declare unit and workload")
    if verdicts.get(AUTHORED_AXIS) != VERDICT_REPORT_ONLY:
        raise ForwardV3BudgetError("datapack_authored_entries must stay REPORT_ONLY")
    for axis in SPLIT_AXES:
        if verdicts.get(axis) != VERDICT_HARD:
            raise ForwardV3BudgetError(f"{axis} must remain blocking")
    if verdicts.get("retained_memory_bytes") != VERDICT_HARD:
        raise ForwardV3BudgetError("retained_memory_bytes must remain blocking")
    return {
        "hard": hard,
        "soft": soft,
        "verdicts": verdicts,
        "units": units,
        "workloads": workloads,
    }


def classify_actuals(
    actuals: Mapping[str, int | None],
    *,
    measured: Mapping[str, bool] | None = None,
    zero_filled: Iterable[str] = (),
    missing_events: Iterable[str] = (),
    policy: Mapping[str, Any] | None = None,
) -> CandidateBudgetResult:
    if any(axis in actuals for axis in FORBIDDEN_V3_AXES):
        raise ForwardV3BudgetError(
            "v3 decision refuses mixed allocation_bytes; use split axes"
        )
    limits = limit_maps(policy)
    hard = limits["hard"]
    soft = limits["soft"]
    verdicts = limits["verdicts"]
    measured_map = dict(measured or {})
    zero = set(zero_filled)
    missing = set(missing_events)
    axes: dict[str, AxisClassification] = {}
    eliminate_reasons: list[str] = []
    soft_warnings: list[str] = []
    report_only_warnings: list[str] = []
    for axis in REQUIRED_V3_AXES:
        if axis in zero:
            note = f"zero_filled:{axis}"
            axes[axis] = AxisClassification(
                axis=axis,
                actual=0,
                soft_budget=int(soft[axis]),
                hard_ceiling=int(hard[axis]),
                readiness_verdict=str(verdicts[axis]),
                status=STATUS_PENDING,
                eliminates=True,
                note=note,
            )
            eliminate_reasons.append(note)
            continue
        if axis in missing:
            note = f"missing_event:{axis}"
            axes[axis] = AxisClassification(
                axis=axis,
                actual=None,
                soft_budget=int(soft[axis]),
                hard_ceiling=int(hard[axis]),
                readiness_verdict=str(verdicts[axis]),
                status=STATUS_PENDING,
                eliminates=True,
                note=note,
            )
            eliminate_reasons.append(note)
            continue
        actual = actuals.get(axis)
        if actual is None or measured_map.get(axis) is False:
            note = f"{STATUS_PENDING}:{axis}"
            axes[axis] = AxisClassification(
                axis=axis,
                actual=None,
                soft_budget=int(soft[axis]),
                hard_ceiling=int(hard[axis]),
                readiness_verdict=str(verdicts[axis]),
                status=STATUS_PENDING,
                eliminates=True,
                note=note,
            )
            eliminate_reasons.append(note)
            continue
        row = classify_axis(
            axis,
            int(actual),
            soft_budget=int(soft[axis]),
            hard_ceiling=int(hard[axis]),
            readiness_verdict=str(verdicts[axis]),
        )
        axes[axis] = row
        if row.eliminates and row.note:
            eliminate_reasons.append(row.note)
        elif row.status == STATUS_SOFT and row.note:
            soft_warnings.append(row.note)
        elif row.status == STATUS_REPORT_ONLY and row.note:
            report_only_warnings.extend(row.note.split(";"))
    return CandidateBudgetResult(
        axes=axes,
        eliminate_reasons=list(dict.fromkeys(eliminate_reasons)),
        soft_warnings=list(dict.fromkeys(soft_warnings)),
        report_only_warnings=list(dict.fromkeys(report_only_warnings)),
    )
