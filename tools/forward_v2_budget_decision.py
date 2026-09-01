#!/usr/bin/env python3
"""Forward-v2 materialization budget classification for T47 and later waves.

Hard ceilings eliminate non-authored axes. Soft budgets warn and never
eliminate. ``datapack_authored_entries`` is REPORT_ONLY for both soft and
hard reference overage and never participates in winner or failed_gates.
T37-T46 builders keep their frozen elimination rules.
"""

from __future__ import annotations

from dataclasses import dataclass, field
from pathlib import Path
from typing import Any, Iterable, Mapping

from tools import t35_common as t35

LOAD_POLICY_V2 = t35.TOOLS / "t14_load_budget_policy.v2.json"
AUTHORED_AXIS = "datapack_authored_entries"
VERDICT_HARD = "HARD"
VERDICT_REPORT_ONLY = "REPORT_ONLY"
VERDICT_UNVERIFIED_SCALE = "UNVERIFIED_SCALE"
STATUS_PASS = "PASS"
STATUS_SOFT = "SOFT_BUDGET_EXCEEDED"
STATUS_HARD = "HARD_CEILING_EXCEEDED"
STATUS_REPORT_ONLY = "REPORT_ONLY_REFERENCE_EXCEEDED"
STATUS_PENDING = "measurement_unavailable"


@dataclass(frozen=True)
class AxisClassification:
    axis: str
    actual: int | None
    soft_budget: int
    hard_ceiling: int
    readiness_verdict: str
    status: str
    eliminates: bool
    note: str | None = None


@dataclass(frozen=True)
class CandidateBudgetResult:
    axes: dict[str, AxisClassification] = field(default_factory=dict)
    eliminate_reasons: list[str] = field(default_factory=list)
    soft_warnings: list[str] = field(default_factory=list)
    report_only_warnings: list[str] = field(default_factory=list)

    @property
    def eliminates(self) -> bool:
        return bool(self.eliminate_reasons)


def load_policy(path: Path | None = None) -> dict[str, Any]:
    document = t35.load_json(path or LOAD_POLICY_V2)
    if document.get("status") != "FORWARD_LOAD_BUDGET_POLICY_V2":
        raise ValueError("forward-v2 load budget policy status drifted")
    if document.get("schema_version") != 2:
        raise ValueError("forward-v2 load budget policy schema drifted")
    return document


def policy_sha256(path: Path | None = None) -> str:
    target = path or LOAD_POLICY_V2
    if not target.is_file():
        raise FileNotFoundError(target)
    return t35.sha256_file(target)


def axis_verdict(spec: Mapping[str, Any]) -> str:
    verdict = spec.get("readiness_verdict") or VERDICT_HARD
    if verdict not in {VERDICT_HARD, VERDICT_REPORT_ONLY, VERDICT_UNVERIFIED_SCALE}:
        raise ValueError(f"unknown readiness_verdict {verdict!r}")
    return str(verdict)


def limit_maps(policy: Mapping[str, Any] | None = None) -> dict[str, dict[str, Any]]:
    document = policy if policy is not None else load_policy()
    budgets = document.get("budgets") or {}
    hard: dict[str, int] = {}
    soft: dict[str, int] = {}
    verdicts: dict[str, str] = {}
    for axis, spec in budgets.items():
        if not isinstance(spec, dict):
            continue
        hard[axis] = int(spec["hard_ceiling"])
        soft[axis] = int(spec["soft_budget"])
        verdicts[axis] = axis_verdict(spec)
    if verdicts.get(AUTHORED_AXIS) != VERDICT_REPORT_ONLY:
        raise ValueError("datapack_authored_entries must stay REPORT_ONLY in forward-v2")
    return {"hard": hard, "soft": soft, "verdicts": verdicts}


def hard_limits(policy: Mapping[str, Any] | None = None) -> dict[str, int]:
    return dict(limit_maps(policy)["hard"])


def soft_budgets(policy: Mapping[str, Any] | None = None) -> dict[str, int]:
    return dict(limit_maps(policy)["soft"])


def readiness_verdicts(policy: Mapping[str, Any] | None = None) -> dict[str, str]:
    return dict(limit_maps(policy)["verdicts"])


def classify_axis(
    axis: str,
    actual: int,
    *,
    soft_budget: int,
    hard_ceiling: int,
    readiness_verdict: str = VERDICT_HARD,
) -> AxisClassification:
    verdict = readiness_verdict or VERDICT_HARD
    if verdict == VERDICT_UNVERIFIED_SCALE:
        notes: list[str] = []
        if actual > hard_ceiling:
            notes.append(f"unverified_scale:{axis}:{actual}>{hard_ceiling}")
        if actual > soft_budget:
            notes.append(f"unverified_scale:soft_budget:{axis}:{actual}>{soft_budget}")
        if notes:
            return AxisClassification(
                axis=axis,
                actual=actual,
                soft_budget=soft_budget,
                hard_ceiling=hard_ceiling,
                readiness_verdict=verdict,
                status=STATUS_REPORT_ONLY,
                eliminates=False,
                note=";".join(notes),
            )
        return AxisClassification(
            axis=axis,
            actual=actual,
            soft_budget=soft_budget,
            hard_ceiling=hard_ceiling,
            readiness_verdict=verdict,
            status=STATUS_PASS,
            eliminates=False,
        )
    if verdict == VERDICT_REPORT_ONLY:
        notes: list[str] = []
        if actual > hard_ceiling:
            notes.append(f"report_only:hard_ceiling:{axis}:{actual}>{hard_ceiling}")
        if actual > soft_budget:
            notes.append(f"report_only:soft_budget:{axis}:{actual}>{soft_budget}")
        if notes:
            return AxisClassification(
                axis=axis,
                actual=actual,
                soft_budget=soft_budget,
                hard_ceiling=hard_ceiling,
                readiness_verdict=verdict,
                status=STATUS_REPORT_ONLY,
                eliminates=False,
                note=";".join(notes),
            )
        return AxisClassification(
            axis=axis,
            actual=actual,
            soft_budget=soft_budget,
            hard_ceiling=hard_ceiling,
            readiness_verdict=verdict,
            status=STATUS_PASS,
            eliminates=False,
        )
    if actual > hard_ceiling:
        note = f"hard_ceiling:{axis}:{actual}>{hard_ceiling}"
        return AxisClassification(
            axis=axis,
            actual=actual,
            soft_budget=soft_budget,
            hard_ceiling=hard_ceiling,
            readiness_verdict=verdict,
            status=STATUS_HARD,
            eliminates=True,
            note=note,
        )
    if actual > soft_budget:
        note = f"soft_budget:{axis}:{actual}>{soft_budget}"
        return AxisClassification(
            axis=axis,
            actual=actual,
            soft_budget=soft_budget,
            hard_ceiling=hard_ceiling,
            readiness_verdict=verdict,
            status=STATUS_SOFT,
            eliminates=False,
            note=note,
        )
    return AxisClassification(
        axis=axis,
        actual=actual,
        soft_budget=soft_budget,
        hard_ceiling=hard_ceiling,
        readiness_verdict=verdict,
        status=STATUS_PASS,
        eliminates=False,
    )


def classify_actuals(
    actuals: Mapping[str, int | None],
    *,
    hard_limits_1x: Mapping[str, int] | None = None,
    soft_budgets_1x: Mapping[str, int] | None = None,
    readiness_verdicts_1x: Mapping[str, str] | None = None,
    pending_if_missing: Iterable[str] = (),
    policy: Mapping[str, Any] | None = None,
) -> CandidateBudgetResult:
    limits = None
    if hard_limits_1x is None or soft_budgets_1x is None or readiness_verdicts_1x is None:
        limits = limit_maps(policy)
    hard = dict(limits["hard"] if hard_limits_1x is None else hard_limits_1x)
    soft = dict(limits["soft"] if soft_budgets_1x is None else soft_budgets_1x)
    verdicts = dict(
        limits["verdicts"] if readiness_verdicts_1x is None else readiness_verdicts_1x
    )
    pending = set(pending_if_missing)
    axes: dict[str, AxisClassification] = {}
    eliminate_reasons: list[str] = []
    soft_warnings: list[str] = []
    report_only_warnings: list[str] = []
    for axis in hard:
        actual = actuals.get(axis)
        if actual is None:
            if axis in pending:
                note = f"{STATUS_PENDING}:{axis}"
                axes[axis] = AxisClassification(
                    axis=axis,
                    actual=None,
                    soft_budget=int(soft[axis]),
                    hard_ceiling=int(hard[axis]),
                    readiness_verdict=str(verdicts.get(axis) or VERDICT_HARD),
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
            readiness_verdict=str(verdicts.get(axis) or VERDICT_HARD),
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
