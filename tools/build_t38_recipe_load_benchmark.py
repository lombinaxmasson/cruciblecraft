#!/usr/bin/env python3
"""Validate T38 materialization measurements and derive the production winner."""

from __future__ import annotations

import argparse
import copy
import hashlib
import json
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
POLICY = ROOT / "tools" / "t38_materialization_policy.json"
MEASUREMENTS = ROOT / "tools" / "t38_materialization_measurements.json"
OUTPUT = ROOT / "tools" / "t38_materialization_decision.json"
GENERATED_ROOT = ROOT / "src" / "t38_recipe_generated"
T37_CENSUS = ROOT / "tools" / "t37_census_delta.json"
T38_SOURCE = ROOT / "tools" / "t38_roaster_source.json"

CANDIDATES = ("immediate", "on_demand", "hybrid")
PRODUCTION_SCALE = "1x"
FAMILY_COUNT = 29
PRODUCTION_LOGICAL_ROWS = 73
PENDING = "PENDING_MEASUREMENT"
CORRECTNESS_GATES = (
    "field_equivalence",
    "full_enumeration",
    "client_consistency",
    "player_execution",
)
RANKING_ORDER = (
    "eager_margin",
    "retained_memory",
    "reload_index",
    "lookup",
    "sync",
    "implementation_complexity",
)
IMPLEMENTATION_COMPLEXITY = {
    "immediate": 0,
    "on_demand": 1,
    "hybrid": 2,
}
PENDING_RUNTIME_AXES = (
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
)
TIMING_SIDES = ("server", "dedicated_client")
PENDING_METRIC_FIELDS = (
    "enumeration",
    "lookup",
    "sync",
    "retained_memory",
    "allocation",
)
PARTITION = {
    "immediate": (73, 0, 0),
    "on_demand": (0, 73, 16),
    "hybrid": (38, 35, 16),
}
COUNTABLE_AXES = (
    "datapack_authored_entries",
    "eager_publication_rows",
    "lazy_logical_rows",
    "lazy_cache_ceiling_rows",
)
MAX_INTERVAL_SOFT_WARNING_AXES = ("lookup_candidate_count",)


def load(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def stable_json(value: Any) -> str:
    return json.dumps(
        value,
        ensure_ascii=False,
        indent=2,
        sort_keys=True,
    ) + "\n"


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        while chunk := handle.read(1024 * 1024):
            digest.update(chunk)
    return digest.hexdigest()


def canonical_digest(value: Any) -> str:
    encoded = json.dumps(
        value,
        ensure_ascii=False,
        sort_keys=True,
        separators=(",", ":"),
    ).encode("utf-8")
    return hashlib.sha256(encoded).hexdigest()


def generated_datapack_present() -> bool:
    if not GENERATED_ROOT.is_dir():
        return False
    return any(GENERATED_ROOT.rglob("*.json"))


def t37_opening() -> dict[str, Any]:
    census = load(T37_CENSUS)
    axes = {
        row["axis"]: row
        for row in census["t14_load"]["axes"]
    }
    closing = census["t14_load"]["closing"]
    return {
        "source": "tools/t37_census_delta.json#t14_load.closing",
        "datapack_authored_entries": closing["datapack_authored_entries"],
        "eager_publication_rows": closing["eager_publication_rows"],
        "lazy_logical_rows": closing["lazy_logical_rows"],
        "lazy_cache_ceiling_rows": axes["lazy_cache_ceiling_rows"]["closing"],
        "sync_bytes": axes["sync_bytes"]["closing"],
        "server_reload_ms": axes["server_reload_ms"]["closing"],
        "server_index_ms": axes["server_index_ms"]["closing"],
        "client_reload_ms": axes["client_reload_ms"]["closing"],
        "client_index_ms": axes["client_index_ms"]["closing"],
        "retained_memory_bytes": axes["retained_memory_bytes"]["closing"],
        "allocation_bytes": axes["allocation_bytes"]["closing"],
        "lookup_p95_ns": axes["lookup_p95_ns"]["closing"],
        "lookup_candidate_count": axes["lookup_candidate_count"]["closing"],
        "pending_runtime_axes": [],
    }


def hybrid_eager_stable_ids(source: dict[str, Any], policy: dict[str, Any]) -> list[str]:
    selector = policy["hybrid_boundary"]["eager_selector"]
    duration_limit = selector["duration_ticks_lte"]
    eager = []
    for row in sorted(source["relations"], key=lambda item: item["stable_id"]):
        if row["duration"] <= duration_limit:
            eager.append(row["stable_id"])
    return eager


def validate_policy(policy: dict[str, Any]) -> None:
    if (
        policy.get("schema_version") != 1
        or policy.get("status") != "T38_MATERIALIZATION_CANDIDATE_POLICY"
    ):
        raise ValueError("T38 materialization policy header drifted")
    scope = policy.get("scope", {})
    if (
        scope.get("family_count") != FAMILY_COUNT
        or scope.get("production_logical_rows") != PRODUCTION_LOGICAL_ROWS
        or scope.get("authored_entries_per_family") != 1
    ):
        raise ValueError("T38 policy work set drifted from 29 families / 73 logical rows")
    work_set = scope.get("work_set", "")
    if "#0002" in work_set:
        raise ValueError("T38 work set must exclude gt.recipe.roaster#0002")
    contract = policy.get("candidate_contract", {})
    if tuple(contract.get("candidates") or ()) != CANDIDATES:
        raise ValueError("T38 candidate set or order drifted")
    hybrid = policy.get("hybrid_boundary", {})
    if (
        hybrid.get("declared_before_measurement") is not True
        or hybrid.get("hot_modulo") is not None
        or hybrid.get("forbidden_extruder_hot_modulo") != 5
        or hybrid.get("cache_ceiling") != 16
        or hybrid.get("declared_eager_count") != 38
        or hybrid.get("declared_lazy_count") != 35
        or len(hybrid.get("eager_stable_ids") or []) != 38
    ):
        raise ValueError("T38 hybrid boundary must be frozen before measuring")
    if hybrid.get("declared_eager_count") == 14 and hybrid.get("declared_lazy_count") == 36:
        raise ValueError("T38 must not copy T37 14/36 hybrid partition")
    caches = contract.get("cache_policies", {})
    if "512" in json.dumps(caches) or "cache ceiling 8" in json.dumps(caches):
        raise ValueError("T38 must not copy Extruder cache 512 or T37 cache 8")
    scales = {
        row["id"]: row.get("logical_rows")
        for row in policy.get("scales") or []
    }
    if scales.get("1x") != 73 or scales.get("5x") == 13910 or scales.get("20x") == 55640:
        raise ValueError("T38 scales must use 73-row 1x and must not copy Extruder 20x")
    winner = policy.get("winner_algorithm", {})
    if (
        winner.get("composite_score_forbidden") is not True
        or winner.get("ranking_scale") != PRODUCTION_SCALE
        or winner.get("ranking_logical_rows") != PRODUCTION_LOGICAL_ROWS
        or winner.get("ranking_order_after_all_gates_pass") != list(RANKING_ORDER)
        or winner.get("pending_axis_zero_fill_forbidden") is not True
        or winner.get("correctness_gates") != list(CORRECTNESS_GATES)
        or winner.get("max_interval_soft_budget_does_not_eliminate")
        != list(MAX_INTERVAL_SOFT_WARNING_AXES)
    ):
        raise ValueError("T38 winner algorithm drifted")
    opening = policy.get("t38_opening", {})
    live = t37_opening()
    if (
        opening.get("datapack_authored_entries", {}).get("closing")
        != live["datapack_authored_entries"]
        or opening.get("eager_publication_rows", {}).get("closing")
        != live["eager_publication_rows"]
        or opening.get("lazy_logical_rows", {}).get("closing")
        != live["lazy_logical_rows"]
        or opening.get("pending_runtime_axes") != live["pending_runtime_axes"]
        or opening.get("pending_runtime_axes_zero_fill") is not False
    ):
        raise ValueError("T38 opening does not match T37 t14_load closing")
    if T38_SOURCE.is_file():
        source = load(T38_SOURCE)
        computed = hybrid_eager_stable_ids(source, policy)
        if computed != hybrid["eager_stable_ids"]:
            raise ValueError("T38 hybrid eager subset is stale vs roaster source")


def _reject_zero_filled_pending(value: Any, context: str) -> None:
    if isinstance(value, dict):
        status = value.get("status")
        if status == PENDING:
            for key, child in value.items():
                if key in {"p50_ns", "p95_ns", "p50_bytes", "p95_bytes", "bytes"}:
                    if child == 0:
                        raise ValueError(
                            f"{context}.{key}: pending axis was zero-filled"
                        )
                    if child is not None and key != "status":
                        if isinstance(child, (int, float)) and child == 0:
                            raise ValueError(
                                f"{context}.{key}: pending axis was zero-filled"
                            )
                _reject_zero_filled_pending(child, f"{context}.{key}")
            if not value.get("reason") and "reason" not in context:
                if value.get("reason") == "":
                    raise ValueError(f"{context}: PENDING lacks a reason")
        else:
            for key, child in value.items():
                _reject_zero_filled_pending(child, f"{context}.{key}")
    elif isinstance(value, list):
        for index, child in enumerate(value):
            _reject_zero_filled_pending(child, f"{context}[{index}]")


def _require_pending_side(side: dict[str, Any], context: str) -> None:
    if side.get("status") != PENDING:
        raise ValueError(f"{context}: expected {PENDING} until measured")
    if not side.get("reason"):
        raise ValueError(f"{context}: PENDING lacks a reason")
    for invented in ("p50_ns", "p95_ns", "raw_samples_ns"):
        if invented in side and side[invented] is not None:
            raise ValueError(f"{context}: invented {invented} while PENDING")


def validate_measurement(
    measurement: dict[str, Any],
    policy: dict[str, Any],
) -> None:
    validate_policy(policy)
    if measurement.get("schema_version") != 1:
        raise ValueError("T38 measurement schema drifted")
    if measurement.get("production_winner_claimed") is not False:
        raise ValueError("raw measurements must not claim a production winner")
    if "production_winner" in measurement:
        raise ValueError("hand-filled production_winner is forbidden in measurements")
    measured_rows = measurement.get("input", {}).get("measured_logical_rows")
    if (
        not isinstance(measured_rows, list)
        or PRODUCTION_LOGICAL_ROWS not in measured_rows
        or measurement["input"].get("logical_rows") != PRODUCTION_LOGICAL_ROWS
    ):
        raise ValueError("1x logical_rows=73 must appear in measured_logical_rows")
    declared_present = measurement["input"].get("generated_datapack_present")
    if declared_present != generated_datapack_present():
        raise ValueError("generated datapack presence drifted")

    live_opening = t37_opening()
    opening = measurement.get("t37_opening", {})
    if (
        opening.get("datapack_authored_entries")
        != live_opening["datapack_authored_entries"]
        or opening.get("eager_publication_rows")
        != live_opening["eager_publication_rows"]
        or opening.get("lazy_logical_rows")
        != live_opening["lazy_logical_rows"]
        or opening.get("pending_runtime_axes")
        != live_opening["pending_runtime_axes"]
    ):
        raise ValueError("measurement T37 opening drifted")

    diagnostic = measurement.get("diagnostic_scales", {})
    for scale in ("5x", "20x"):
        row = diagnostic.get(scale) or {}
        if row.get("production_use") != "forbidden":
            raise ValueError(f"{scale} must be diagnostic-only")
        if scale == "20x" and row.get("logical_rows") == 55640:
            raise ValueError("T38 20x must not copy Extruder 55640")

    scenarios = measurement.get("scenarios") or []
    production = next(
        (row for row in scenarios if row.get("scale") == PRODUCTION_SCALE),
        None,
    )
    if production is None:
        raise ValueError("1x production scenario is missing")
    if (
        production.get("logical_rows") != PRODUCTION_LOGICAL_ROWS
        or production.get("production_scale") is not True
    ):
        raise ValueError("1x production scenario drifted")
    candidates = production.get("candidates") or []
    if [row.get("candidate") for row in candidates] != list(CANDIDATES):
        raise ValueError("1x candidate set/order drifted")
    for row in candidates:
        candidate = row["candidate"]
        expected = PARTITION[candidate]
        if (
            row.get("eager_publication_rows"),
            row.get("lazy_logical_rows"),
            row.get("lazy_cache_ceiling_rows"),
        ) != expected:
            raise ValueError(f"{candidate}: partition drifted from frozen policy")
        _reject_zero_filled_pending(row, candidate)
        if row.get("status") == PENDING:
            gates = row.get("gates", {})
            for gate in CORRECTNESS_GATES:
                if gates.get(gate) is not None:
                    raise ValueError(
                        f"{candidate}: pending correctness gate {gate} was filled"
                    )
            if gates.get("all_pass") is not False:
                raise ValueError(f"{candidate}: pending all_pass must be false")
            for side in TIMING_SIDES:
                _require_pending_side(row.get(side, {}), f"{candidate}/{side}")
            integrated = row.get("integrated_client", {})
            if integrated.get("independent_reexpansion") is not False:
                raise ValueError(
                    f"{candidate}: integrated client must not be an independent sample"
                )
            _require_pending_side(integrated, f"{candidate}/integrated_client")
            for field in PENDING_METRIC_FIELDS:
                _require_pending_side(row.get(field, {}), f"{candidate}/{field}")
            retained = row.get("retained_memory", {})
            if retained.get("naive_heap_delta_used") is not False:
                raise ValueError(f"{candidate}: naive heap delta is forbidden")
            if retained.get("p50_bytes") == 0:
                raise ValueError(f"{candidate}: retained pending was zero-filled")
            if row.get("sync", {}).get("bytes") == 0:
                raise ValueError(f"{candidate}: sync pending was zero-filled")
        elif row.get("status") == "PASS":
            _validate_pass_candidate(row, candidate)
        else:
            raise ValueError(f"{candidate}: status must be PASS or {PENDING}")


def _require_int(value: Any, context: str) -> int:
    if type(value) is not int:
        raise ValueError(f"{context}: expected int, not {value!r}")
    return value


def _validate_pass_candidate(row: dict[str, Any], candidate: str) -> None:
    gates = row.get("gates", {})
    for gate in ("field_equivalence", "full_enumeration", "client_consistency"):
        if gates.get(gate) is not True:
            raise ValueError(f"{candidate}: PASS requires {gate}=true from JUnit evidence")
    if gates.get("all_pass") is True and gates.get("player_execution") is not True:
        raise ValueError(
            f"{candidate}: all_pass cannot be true while player_execution is pending"
        )
    for side_name in TIMING_SIDES:
        side = row.get(side_name, {})
        if side.get("status") != "PASS":
            raise ValueError(f"{candidate}/{side_name}: expected PASS")
        for part in ("reload", "index"):
            metric = side.get(part, {})
            _require_int(metric.get("p50_ns"), f"{candidate}/{side_name}/{part}.p50_ns")
            _require_int(metric.get("p95_ns"), f"{candidate}/{side_name}/{part}.p95_ns")
    integrated = row.get("integrated_client", {})
    if integrated.get("independent_reexpansion") is not False:
        raise ValueError(
            f"{candidate}: integrated client must not be an independent sample"
        )
    enumeration = row.get("enumeration", {})
    _require_int(enumeration.get("p50_ns"), f"{candidate}/enumeration.p50_ns")
    _require_int(enumeration.get("p95_ns"), f"{candidate}/enumeration.p95_ns")
    lookup = row.get("lookup", {})
    _require_int(lookup.get("p50_ns"), f"{candidate}/lookup.p50_ns")
    _require_int(lookup.get("p95_ns"), f"{candidate}/lookup.p95_ns")
    _require_int(lookup.get("candidates_p95"), f"{candidate}/lookup.candidates_p95")
    sync = row.get("sync", {})
    _require_int(sync.get("bytes"), f"{candidate}/sync.bytes")
    retained = row.get("retained_memory", {})
    _require_int(retained.get("p50_bytes"), f"{candidate}/retained_memory.p50_bytes")
    allocation = row.get("allocation", {})
    if allocation.get("status") == PENDING:
        if allocation.get("p50_bytes") == 0:
            raise ValueError(f"{candidate}: allocation pending was zero-filled")
        if not allocation.get("reason"):
            raise ValueError(f"{candidate}: pending allocation lacks a reason")
    elif allocation.get("status") == "PASS":
        _require_int(allocation.get("p50_bytes"), f"{candidate}/allocation.p50_bytes")
    else:
        raise ValueError(f"{candidate}: allocation must be PASS or {PENDING}")


def _cumulative_counts(
    policy: dict[str, Any],
    row: dict[str, Any],
) -> dict[str, int]:
    opening = policy["t38_opening"]
    return {
        "datapack_authored_entries": (
            opening["datapack_authored_entries"]["closing"]
            + FAMILY_COUNT
        ),
        "eager_publication_rows": (
            opening["eager_publication_rows"]["closing"]
            + row["eager_publication_rows"]
        ),
        "lazy_logical_rows": (
            opening["lazy_logical_rows"]["closing"]
            + row["lazy_logical_rows"]
        ),
        "lazy_cache_ceiling_rows": row["lazy_cache_ceiling_rows"],
    }


def _reload_index_key(row: dict[str, Any]) -> tuple[int, int, int, int]:
    return (
        int(row["server"]["reload"]["p95_ns"]),
        int(row["server"]["index"]["p95_ns"]),
        int(row["dedicated_client"]["reload"]["p95_ns"]),
        int(row["dedicated_client"]["index"]["p95_ns"]),
    )


def _eager_margin_tied(
    survivors: list[str],
    rows: dict[str, dict[str, Any]],
    policy: dict[str, Any],
) -> bool:
    soft = policy["soft_budgets_1x"]["eager_publication_rows"]
    return all(
        _cumulative_counts(policy, rows[candidate])["eager_publication_rows"] <= soft
        for candidate in survivors
    )


def _ranking_record(
    candidate: str,
    row: dict[str, Any],
    *,
    eager_tied: bool,
) -> dict[str, Any]:
    reload_key = _reload_index_key(row)
    return {
        "candidate": candidate,
        "eager_publication_rows": row["eager_publication_rows"],
        "eager_margin": "tied" if eager_tied else row["eager_publication_rows"],
        "retained_memory_bytes": row["retained_memory"]["p50_bytes"],
        "reload_index_p95_ns": {
            "server_reload": reload_key[0],
            "server_index": reload_key[1],
            "client_reload": reload_key[2],
            "client_index": reload_key[3],
        },
        "lookup_p95_ns": row["lookup"]["p95_ns"],
        "sync_bytes": row["sync"]["bytes"],
        "implementation_complexity": IMPLEMENTATION_COMPLEXITY[candidate],
    }


def _ranking_sort_key(
    record: dict[str, Any],
    preference: list[str],
) -> tuple[Any, ...]:
    eager = 0 if record["eager_margin"] == "tied" else record["eager_publication_rows"]
    reload = record["reload_index_p95_ns"]
    return (
        eager,
        record["retained_memory_bytes"],
        (
            reload["server_reload"],
            reload["server_index"],
            reload["client_reload"],
            reload["client_index"],
        ),
        record["lookup_p95_ns"],
        record["sync_bytes"],
        record["implementation_complexity"],
        preference.index(record["candidate"]),
    )


def _axis_value(row: dict[str, Any], axis: str) -> int | None:
    if axis in {
        "datapack_authored_entries",
        "eager_publication_rows",
        "lazy_logical_rows",
        "lazy_cache_ceiling_rows",
    }:
        return None
    if axis == "sync_bytes":
        value = row.get("sync", {}).get("bytes")
        return value if isinstance(value, int) else None
    if axis == "server_reload_ms":
        value = row.get("server", {}).get("reload", {}).get("p95_ns")
        return None if not isinstance(value, int) else (value + 999_999) // 1_000_000
    if axis == "server_index_ms":
        value = row.get("server", {}).get("index", {}).get("p95_ns")
        return None if not isinstance(value, int) else (value + 999_999) // 1_000_000
    if axis == "client_reload_ms":
        value = row.get("dedicated_client", {}).get("reload", {}).get("p95_ns")
        return None if not isinstance(value, int) else (value + 999_999) // 1_000_000
    if axis == "client_index_ms":
        value = row.get("dedicated_client", {}).get("index", {}).get("p95_ns")
        return None if not isinstance(value, int) else (value + 999_999) // 1_000_000
    if axis == "retained_memory_bytes":
        value = row.get("retained_memory", {}).get("p50_bytes")
        return value if isinstance(value, int) else None
    if axis == "allocation_bytes":
        value = row.get("allocation", {}).get("p50_bytes")
        return value if isinstance(value, int) else None
    if axis == "lookup_p95_ns":
        value = row.get("lookup", {}).get("p95_ns")
        return value if isinstance(value, int) else None
    if axis == "lookup_candidate_count":
        value = row.get("lookup", {}).get("candidates_p95")
        return value if isinstance(value, int) else None
    return None


def derive_decision(
    measurement: dict[str, Any],
    policy: dict[str, Any],
) -> dict[str, Any]:
    validate_measurement(measurement, policy)
    production = next(
        row for row in measurement["scenarios"]
        if row["scale"] == PRODUCTION_SCALE
    )
    rows = {row["candidate"]: row for row in production["candidates"]}
    eliminated: dict[str, list[str]] = {}
    survivors: list[str] = []
    soft_warnings: dict[str, list[str]] = {}
    complete = measurement.get("status") == "T38_MATERIALIZATION_MEASUREMENT_READY"
    warning_axes = set(
        policy["winner_algorithm"].get(
            "max_interval_soft_budget_does_not_eliminate"
        )
        or MAX_INTERVAL_SOFT_WARNING_AXES
    )

    for candidate in CANDIDATES:
        row = rows[candidate]
        reasons: list[str] = []
        warnings: list[str] = []
        if row.get("status") != "PASS" or not complete:
            reasons.append("measurement_unavailable:1x")
        gates = row.get("gates", {})
        for gate in CORRECTNESS_GATES:
            if gates.get(gate) is not True:
                reasons.append(f"gate_failed_or_pending:{gate}")
        counts = _cumulative_counts(policy, row)
        for metric, actual in counts.items():
            hard = policy["hard_limits_1x"][metric]
            if actual > hard:
                reasons.append(f"hard_ceiling:{metric}:{actual}>{hard}")
        if not reasons:
            for metric, actual in counts.items():
                soft = policy["soft_budgets_1x"][metric]
                if actual > soft:
                    reasons.append(f"soft_budget:{metric}:{actual}>{soft}")
            for axis, hard in policy["hard_limits_1x"].items():
                if axis in COUNTABLE_AXES:
                    continue
                actual = _axis_value(row, axis)
                if actual is None:
                    if axis in PENDING_RUNTIME_AXES:
                        reasons.append(f"measurement_unavailable:{axis}")
                    continue
                if actual > hard:
                    reasons.append(f"hard_ceiling:{axis}:{actual}>{hard}")
                elif actual > policy["soft_budgets_1x"][axis]:
                    soft = policy["soft_budgets_1x"][axis]
                    note = f"soft_budget:{axis}:{actual}>{soft}"
                    if axis in warning_axes:
                        warnings.append(note)
                    else:
                        reasons.append(note)
        unique = list(dict.fromkeys(reasons))
        unique_warnings = list(dict.fromkeys(warnings))
        if unique_warnings:
            soft_warnings[candidate] = unique_warnings
        if unique:
            eliminated[candidate] = unique
        else:
            survivors.append(candidate)

    ranking: list[dict[str, Any]] = []
    production_winner = None
    if survivors and complete:
        preference = list(policy["winner_algorithm"]["implementation_complexity_order"])
        declared = policy["winner_algorithm"]["ranking_order_after_all_gates_pass"]
        if list(declared) != list(RANKING_ORDER):
            raise ValueError("T38 ranking_order_after_all_gates_pass drifted")
        eager_tied = _eager_margin_tied(survivors, rows, policy)
        ranking = [
            _ranking_record(candidate, rows[candidate], eager_tied=eager_tied)
            for candidate in survivors
        ]
        ranking.sort(key=lambda record: _ranking_sort_key(record, preference))
        production_winner = ranking[0]["candidate"]

    blockers = []
    if not complete:
        blockers.append(
            "1x=73 measurements are PENDING; production winner is not READY"
        )
    if measurement.get("production_winner_claimed") is not False:
        blockers.append("raw measurements claimed a winner")
    if not production_winner:
        blockers.append("No candidate survived the frozen T38 decision rules")

    return {
        "status": (
            "PRODUCTION_WINNER_READY"
            if production_winner
            else "PRODUCTION_WINNER_BLOCKED"
        ),
        "algorithm": copy.deepcopy(policy["winner_algorithm"]),
        "ranking_scale": PRODUCTION_SCALE,
        "ranking_logical_rows": PRODUCTION_LOGICAL_ROWS,
        "eliminated": eliminated,
        "survivors": survivors,
        "soft_budget_warnings": soft_warnings,
        "ranking": ranking,
        "production_winner": production_winner,
        "production_blockers": blockers,
        "composite_score_used": False,
        "diagnostic_5x_20x_used_for_1x": False,
    }


def build(
    measurement: dict[str, Any] | None = None,
    policy: dict[str, Any] | None = None,
) -> dict[str, Any]:
    policy = load(POLICY) if policy is None else policy
    if measurement is None:
        if not MEASUREMENTS.is_file():
            raise FileNotFoundError(
                f"missing measurements: {MEASUREMENTS.relative_to(ROOT).as_posix()}"
            )
        measurement = load(MEASUREMENTS)
    decision = derive_decision(measurement, policy)
    complete = (
        measurement.get("status") == "T38_MATERIALIZATION_MEASUREMENT_READY"
        and decision["production_winner"]
    )
    return {
        "schema_version": 1,
        "status": (
            "T38_MATERIALIZATION_DECISION_READY"
            if complete
            else "T38_MATERIALIZATION_DECISION_BLOCKED"
        ),
        "measurement_status": measurement.get("status"),
        "decision": decision,
        "currentness": {
            "measurement_sha256": canonical_digest(measurement),
            "policy_path": POLICY.relative_to(ROOT).as_posix(),
            "policy_sha256": sha256(POLICY),
            "builder_path": Path(__file__).resolve()
            .relative_to(ROOT).as_posix(),
            "builder_sha256": sha256(Path(__file__).resolve()),
        },
    }


def validate_artifact(artifact: dict[str, Any]) -> None:
    if artifact.get("schema_version") != 1:
        raise ValueError("T38 decision schema drifted")
    if not MEASUREMENTS.is_file():
        raise FileNotFoundError(
            f"missing measurements: {MEASUREMENTS.relative_to(ROOT).as_posix()}"
        )
    policy = load(POLICY)
    measurement = load(MEASUREMENTS)
    expected = derive_decision(measurement, policy)
    if artifact.get("decision") != expected:
        raise ValueError("T38 derived winner data is stale or hand-filled")
    if artifact["decision"].get("production_winner") != expected["production_winner"]:
        raise ValueError("T38 production_winner must be derived, not hand-filled")
    built = build(measurement, policy)
    if artifact.get("currentness") != built["currentness"]:
        raise ValueError("T38 decision currentness evidence drifted")
    if artifact.get("status") != built["status"]:
        raise ValueError("T38 decision status drifted")


def check() -> list[str]:
    errors: list[str] = []
    if not POLICY.is_file():
        errors.append(f"missing policy: {POLICY.relative_to(ROOT).as_posix()}")
    if not MEASUREMENTS.is_file():
        errors.append(
            f"missing measurements: {MEASUREMENTS.relative_to(ROOT).as_posix()}"
        )
        return errors
    if not OUTPUT.is_file():
        errors.append(f"missing generated file: {OUTPUT.relative_to(ROOT).as_posix()}")
        return errors
    try:
        validate_artifact(load(OUTPUT))
    except (KeyError, TypeError, ValueError, FileNotFoundError) as failure:
        errors.append(str(failure))
    return errors


def write() -> dict[str, Any]:
    artifact = build()
    OUTPUT.write_text(
        stable_json(artifact),
        encoding="utf-8",
        newline="\n",
    )
    return artifact


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--check",
        action="store_true",
        help="validate committed artifact currentness and derivation",
    )
    args = parser.parse_args()
    if args.check:
        errors = check()
        if errors:
            print("T38 materialization artifact is stale or invalid:")
            for error in errors:
                print(f"- {error}")
            return 1
        print("T38 materialization artifact is current and recomputable.")
        return 0

    artifact = write()
    decision = artifact["decision"]
    print(
        "T38 measured preference: "
        f"{decision['production_winner']}; "
        f"status: {artifact['status']}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
