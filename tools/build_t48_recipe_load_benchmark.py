#!/usr/bin/env python3
"""Validate T48 materialization measurements and derive production winners."""

from __future__ import annotations

import argparse
import copy
import hashlib
import json
import sys
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import forward_v2_budget_decision as v2_budget  # noqa: E402
from tools import t48_common as t48  # noqa: E402

POLICY = t48.POLICY
MEASUREMENTS = t48.MEASUREMENTS
OUTPUT = t48.DECISION
GENERATED_ROOT = ROOT / "src" / "t48_recipe_generated"

CANDIDATES = ("immediate", "on_demand", "hybrid")
PRODUCTION_SCALE = "1x"
CARD_FAMILIES = t48.production_family_count()
PRODUCTION_LOGICAL_ROWS = t48.production_relation_count()


def _group_counts() -> dict[str, tuple[int, int]]:
    counts: dict[str, list[int]] = {}
    for path in t48.generated_family_files():
        document = json.loads(path.read_text(encoding="utf-8"))
        group_id = str(document.get("publication_group") or "")
        row = counts.setdefault(group_id, [0, 0])
        row[0] += 1
        row[1] += len(document.get("relations") or [])
    return {key: (value[0], value[1]) for key, value in counts.items()}


_GROUP_COUNTS = _group_counts()
EXACT_FAMILIES, EXACT_LOGICAL = _GROUP_COUNTS.get(t48.PUBLICATION_GROUP_EXACT, (0, 0))
EXACT_MULTI_FAMILIES, EXACT_MULTI_LOGICAL = _GROUP_COUNTS.get(t48.PUBLICATION_GROUP_EXACT_MULTI, (0, 0))
TOOL_HEAD_FAMILIES, TOOL_HEAD_LOGICAL = _GROUP_COUNTS.get(t48.PUBLICATION_GROUP_TOOL_HEAD, (0, 0))
SCENARIO_IDS = ("exact", "exact_multi", "tool_head", "card")
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
COUNTABLE_AXES = (
    "datapack_authored_entries",
    "eager_publication_rows",
    "lazy_logical_rows",
    "lazy_cache_ceiling_rows",
)
FORBIDDEN_PARTITIONS = {
    (14, 36),
    (38, 35),
    (0, 73),
}


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


def live_opening() -> dict[str, Any]:
    closing = t48.t14_opening_load()
    return {
        "source": t48.T14_OPENING_LOAD_SOURCE,
        "datapack_authored_entries": closing["datapack_authored_entries"],
        "eager_publication_rows": closing["eager_publication_rows"],
        "lazy_logical_rows": closing["lazy_logical_rows"],
        "lazy_cache_ceiling_rows": closing["lazy_cache_ceiling_rows"],
        "sync_bytes": closing["sync_bytes"],
        "server_reload_ms": closing["server_reload_ms"],
        "server_index_ms": closing["server_index_ms"],
        "client_reload_ms": closing["client_reload_ms"],
        "client_index_ms": closing["client_index_ms"],
        "retained_memory_bytes": closing["retained_memory_bytes"],
        "allocation_bytes": closing["allocation_bytes"],
        "lookup_p95_ns": closing["lookup_p95_ns"],
        "lookup_candidate_count": closing["lookup_candidate_count"],
        "pending_runtime_axes": [],
    }


def hybrid_eager_stable_ids(
    policy: dict[str, Any],
    group_key: str,
) -> list[str]:
    boundary = policy["publication_group_contract"][group_key]["hybrid_boundary"]
    selector = boundary["eager_selector"]
    duration_limit = selector["duration_ticks_lte"]
    eager: list[str] = []
    group_id = policy["publication_group_contract"][group_key]["publication_group"]
    root = t48.GENERATED_ROOT
    for path in sorted(root.rglob("*.json")):
        document = load(path)
        if document.get("publication_group") != group_id:
            continue
        for relation in document.get("relations") or []:
            if relation["duration"] <= duration_limit:
                eager.append(relation["stable_id"])
    return sorted(eager)


def partition(policy: dict[str, Any], scenario_id: str, candidate: str) -> tuple[int, int, int]:
    if scenario_id == "card":
        contract = policy["publication_group_contract"]["card_aggregate"]
    else:
        contract = policy["publication_group_contract"][scenario_id]
    eager, lazy, cache = contract["partitions"][candidate]
    return int(eager), int(lazy), int(cache)


def validate_policy(policy: dict[str, Any]) -> None:
    if (
        policy.get("schema_version") != 1
        or policy.get("status") != "T48_MATERIALIZATION_CANDIDATE_POLICY"
    ):
        raise ValueError("T48 materialization policy header drifted")
    scope = policy.get("scope", {})
    if (
        scope.get("family_count") != CARD_FAMILIES
        or scope.get("production_logical_rows") != PRODUCTION_LOGICAL_ROWS
    ):
        raise ValueError("T48 policy work set drifted from the production lock")
    if scope.get("production_lock_sha256") != t48.production_lock_sha256():
        raise ValueError("T48 policy production lock hash drifted")
    contract = policy.get("candidate_contract", {})
    if tuple(contract.get("candidates") or ()) != CANDIDATES:
        raise ValueError("T48 candidate set or order drifted")
    for group_key in ("exact", "exact_multi", "tool_head"):
        hybrid = policy["publication_group_contract"][group_key]["hybrid_boundary"]
        if hybrid.get("declared_before_measurement") is not True:
            raise ValueError(f"T48 {group_key} hybrid boundary must be frozen before measuring")
        selector = hybrid.get("eager_selector") or {}
        if selector.get("duration_ticks_lte") != t48.HYBRID_CUTOFF:
            raise ValueError(
                f"T48 {group_key} must freeze duration_ticks_lte "
                f"{t48.HYBRID_CUTOFF}, not T46 zero-eager"
            )
        if hybrid.get("declared_eager_count") == 0:
            raise ValueError(f"T48 {group_key} must not copy T46 duration_ticks_lte 0")
        if hybrid.get("cache_ceiling") == 24:
            raise ValueError(f"T48 {group_key} must not copy T46 cache 24")
        partition_pair = (
            hybrid.get("declared_eager_count"),
            hybrid.get("declared_lazy_count"),
        )
        if partition_pair in FORBIDDEN_PARTITIONS:
            raise ValueError(f"T48 {group_key} must not copy T37/T38 hybrid partitions")
        declared = hybrid.get("eager_stable_ids") or []
        computed = hybrid_eager_stable_ids(policy, group_key)
        if computed != declared:
            raise ValueError(f"T48 {group_key} hybrid eager subset is stale vs generated JSON")
    card = policy["publication_group_contract"]["card_aggregate"]
    if card.get("logical_rows") != PRODUCTION_LOGICAL_ROWS:
        raise ValueError("T48 card aggregate logical rows drifted")
    does_not_modify = scope.get("does_not_modify") or []
    for marker in (
        "T37 14/36/8",
        "T38 0/73/16",
        "T39 19/13",
        "T40 cache 11",
        "T41 cache 16",
        "T45 cache 24",
        "T46 cache 24 / duration_ticks_lte 0",
    ):
        if not any(marker in item for item in does_not_modify):
            raise ValueError(f"T48 policy must record does_not_modify {marker}")
    winner = policy.get("winner_algorithm", {})
    if (
        winner.get("composite_score_forbidden") is not True
        or winner.get("per_group_winners_required") is not True
        or winner.get("card_aggregate_required") is not True
        or winner.get("gate_order") != []
        or winner.get("soft_budget_does_not_eliminate") is not True
        or winner.get("authored_entries_verdict") != "REPORT_ONLY"
        or "max_interval_soft_budget_does_not_eliminate" in winner
    ):
        raise ValueError("T48 winner algorithm drifted")
    limits = v2_budget.limit_maps()
    if policy.get("hard_limits_1x") != limits["hard"]:
        raise ValueError("T48 hard limits drifted from forward-v2 load budget")
    if policy.get("soft_budgets_1x") != limits["soft"]:
        raise ValueError("T48 soft budgets drifted from forward-v2 load budget")
    if policy.get("readiness_verdicts_1x") != limits["verdicts"]:
        raise ValueError("T48 readiness verdicts drifted from forward-v2 load budget")
    v2_bind = policy.get("load_budget_policy_v2") or {}
    if (
        v2_bind.get("path") != v2_budget.LOAD_POLICY_V2.relative_to(ROOT).as_posix()
        or v2_bind.get("sha256") != v2_budget.policy_sha256()
        or v2_bind.get("status") != "FORWARD_LOAD_BUDGET_POLICY_V2"
    ):
        raise ValueError("T48 policy is not bound to the current forward-v2 load budget")
    opening = policy.get("t48_opening", {})
    live = live_opening()
    if (
        opening.get("datapack_authored_entries", {}).get("closing")
        != live["datapack_authored_entries"]
        or opening.get("pending_runtime_axes_zero_fill") is not False
    ):
        raise ValueError("T48 opening does not match T46 t14_load closing")


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
        else:
            for key, child in value.items():
                _reject_zero_filled_pending(child, f"{context}.{key}")
    elif isinstance(value, list):
        for index, child in enumerate(value):
            _reject_zero_filled_pending(child, f"{context}[{index}]")


def _validate_pass_candidate(row: dict[str, Any], candidate: str) -> None:
    gates = row.get("gates", {})
    for gate in ("field_equivalence", "full_enumeration", "client_consistency"):
        if gates.get(gate) is not True:
            raise ValueError(f"{candidate}: PASS requires {gate}=true from JUnit evidence")
    shard = row.get("shard_routing", {})
    overflow = int(shard.get("overflow_count") or 0)
    expected_overflow = 3 if candidate.startswith(("exact/", "card/")) else 0
    if overflow != expected_overflow:
        raise ValueError(
            f"{candidate}: overflow_count must be {expected_overflow}, got {overflow}"
        )
    for side_name in TIMING_SIDES:
        side = row.get(side_name, {})
        if side.get("status") != "PASS":
            raise ValueError(f"{candidate}/{side_name}: expected PASS")
    # Lookup/index candidate counts above 128 eliminate the candidate in
    # derive_decision (lookup_candidate_count hard ceiling). Immediate eager
    # publication can land in RecipeMap.unindexed() and exceed 128 without
    # implying shard overflow; that is ranking evidence, not a stale artifact.


def validate_measurement(
    measurement: dict[str, Any],
    policy: dict[str, Any],
) -> None:
    validate_policy(policy)
    if measurement.get("schema_version") != 1:
        raise ValueError("T48 measurement schema drifted")
    if measurement.get("production_winner_claimed") is not False:
        raise ValueError("raw measurements must not claim a production winner")
    if "production_winner" in measurement:
        raise ValueError("hand-filled production_winner is forbidden in measurements")
    measured_rows = measurement.get("input", {}).get("measured_logical_rows")
    if (
        not isinstance(measured_rows, list)
        or sorted(measured_rows) != sorted(
            [EXACT_LOGICAL, EXACT_MULTI_LOGICAL, TOOL_HEAD_LOGICAL, PRODUCTION_LOGICAL_ROWS]
        )
    ):
        raise ValueError("1x measured_logical_rows must match the production lock")
    if measurement["input"].get("logical_rows") != PRODUCTION_LOGICAL_ROWS:
        raise ValueError("input.logical_rows must match the production lock")
    if measurement["input"].get("generated_datapack_present") != generated_datapack_present():
        raise ValueError("generated datapack presence drifted")

    live = live_opening()
    opening = measurement.get("t48_opening", {})
    if opening.get("datapack_authored_entries") != live["datapack_authored_entries"]:
        raise ValueError("measurement T48 opening drifted")

    scenarios = {
        row["id"]: row
        for row in measurement.get("scenarios") or []
        if row.get("id") in SCENARIO_IDS
    }
    if set(scenarios) != set(SCENARIO_IDS):
        raise ValueError("exact/exact_multi/tool_head/card scenarios are required")
    for scenario_id in SCENARIO_IDS:
        scenario = scenarios[scenario_id]
        expected_rows = {
            "exact": EXACT_LOGICAL,
            "exact_multi": EXACT_MULTI_LOGICAL,
            "tool_head": TOOL_HEAD_LOGICAL,
            "card": PRODUCTION_LOGICAL_ROWS,
        }[scenario_id]
        if scenario.get("logical_rows") != expected_rows:
            raise ValueError(f"{scenario_id} logical_rows drifted")
        candidates = scenario.get("candidates") or []
        if [row.get("candidate") for row in candidates] != list(CANDIDATES):
            raise ValueError(f"{scenario_id} candidate set/order drifted")
        for row in candidates:
            candidate = row["candidate"]
            expected = partition(policy, scenario_id, candidate)
            actual = (
                row.get("eager_publication_rows"),
                row.get("lazy_logical_rows"),
                row.get("lazy_cache_ceiling_rows"),
            )
            if actual != expected:
                raise ValueError(
                    f"{scenario_id}/{candidate}: partition drifted from frozen policy"
                )
            pair = (actual[0], actual[1])
            if pair in FORBIDDEN_PARTITIONS:
                raise ValueError(
                    f"{scenario_id}/{candidate}: copied forbidden T37/T38 partition"
                )
            _reject_zero_filled_pending(row, f"{scenario_id}/{candidate}")
            if row.get("status") == "PASS":
                _validate_pass_candidate(row, f"{scenario_id}/{candidate}")
            elif row.get("status") == "SKIPPED":
                raise ValueError(
                    f"{scenario_id}/{candidate}: SKIPPED is not PASS"
                )
            elif row.get("status") != PENDING:
                raise ValueError(f"{scenario_id}/{candidate}: status must be PASS or PENDING")


def _cumulative_counts(
    policy: dict[str, Any],
    row: dict[str, Any],
    *,
    family_delta: int,
) -> dict[str, int]:
    opening = policy["t48_opening"]
    return {
        "datapack_authored_entries": (
            opening["datapack_authored_entries"]["closing"] + family_delta
        ),
        "eager_publication_rows": (
            opening["eager_publication_rows"]["closing"]
            + row["eager_publication_rows"]
        ),
        "lazy_logical_rows": (
            opening["lazy_logical_rows"]["closing"] + row["lazy_logical_rows"]
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


def _axis_value(row: dict[str, Any], axis: str) -> int | None:
    if axis in COUNTABLE_AXES:
        return None
    if axis == "sync_bytes":
        value = row.get("sync", {}).get("bytes")
        return value if isinstance(value, int) else None
    if axis == "lookup_p95_ns":
        value = row.get("lookup", {}).get("p95_ns")
        return value if isinstance(value, int) else None
    if axis == "lookup_candidate_count":
        routed = row.get("shard_routing", {}).get("routed_candidates") or {}
        value = routed.get("max")
        if value is None:
            value = row.get("lookup", {}).get("candidates_p95")
        return value if isinstance(value, int) else None
    if axis.endswith("_ms"):
        side = "server" if axis.startswith("server_") else "dedicated_client"
        part = "reload" if "reload" in axis else "index"
        value = row.get(side, {}).get(part, {}).get("p95_ns")
        return None if not isinstance(value, int) else (value + 999_999) // 1_000_000
    if axis == "retained_memory_bytes":
        value = row.get("retained_memory", {}).get("p50_bytes")
        return value if isinstance(value, int) else None
    if axis == "allocation_bytes":
        value = row.get("allocation", {}).get("p50_bytes")
        return value if isinstance(value, int) else None
    return None


def _eager_margin_tied(
    survivors: list[str],
    rows: dict[str, dict[str, Any]],
    policy: dict[str, Any],
) -> bool:
    soft = policy["soft_budgets_1x"]["eager_publication_rows"]
    return all(
        _cumulative_counts(policy, rows[candidate], family_delta=0)[
            "eager_publication_rows"
        ]
        <= soft
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


def _derive_scenario_decision(
    scenario_id: str,
    scenario: dict[str, Any],
    policy: dict[str, Any],
    *,
    family_delta: int,
    complete: bool,
) -> dict[str, Any]:
    rows = {row["candidate"]: row for row in scenario["candidates"]}
    eliminated: dict[str, list[str]] = {}
    survivors: list[str] = []
    soft_warnings: dict[str, list[str]] = {}
    report_only_warnings: dict[str, list[str]] = {}

    for candidate in CANDIDATES:
        row = rows[candidate]
        reasons: list[str] = []
        if row.get("status") != "PASS" or not complete:
            reasons.append("measurement_unavailable:1x")
        gates = row.get("gates", {})
        for gate in CORRECTNESS_GATES:
            if gates.get(gate) is not True:
                reasons.append(f"gate_failed_or_pending:{gate}")
        actuals = dict(_cumulative_counts(policy, row, family_delta=family_delta))
        for axis in policy["hard_limits_1x"]:
            if axis in COUNTABLE_AXES:
                continue
            actuals[axis] = _axis_value(row, axis)
        budget = v2_budget.classify_actuals(
            actuals,
            hard_limits_1x=policy["hard_limits_1x"],
            soft_budgets_1x=policy["soft_budgets_1x"],
            readiness_verdicts_1x=policy["readiness_verdicts_1x"],
            pending_if_missing=PENDING_RUNTIME_AXES,
        )
        reasons.extend(budget.eliminate_reasons)
        unique = list(dict.fromkeys(reasons))
        if budget.soft_warnings:
            soft_warnings[candidate] = list(budget.soft_warnings)
        if budget.report_only_warnings:
            report_only_warnings[candidate] = list(budget.report_only_warnings)
        if unique:
            eliminated[candidate] = unique
        else:
            survivors.append(candidate)

    ranking: list[dict[str, Any]] = []
    production_winner = None
    if survivors and complete:
        preference = list(policy["winner_algorithm"]["implementation_complexity_order"])
        eager_tied = _eager_margin_tied(survivors, rows, policy)
        ranking = [
            _ranking_record(candidate, rows[candidate], eager_tied=eager_tied)
            for candidate in survivors
        ]
        ranking.sort(key=lambda record: _ranking_sort_key(record, preference))
        production_winner = ranking[0]["candidate"]

    return {
        "scenario_id": scenario_id,
        "logical_rows": scenario["logical_rows"],
        "eliminated": eliminated,
        "survivors": survivors,
        "soft_budget_warnings": soft_warnings,
        "report_only_warnings": report_only_warnings,
        "ranking": ranking,
        "production_winner": production_winner,
    }


def derive_decision(
    measurement: dict[str, Any],
    policy: dict[str, Any],
) -> dict[str, Any]:
    validate_measurement(measurement, policy)
    scenarios = {
        row["id"]: row
        for row in measurement.get("scenarios") or []
        if row.get("id") in SCENARIO_IDS
    }
    complete = measurement.get("status") == "T48_MATERIALIZATION_MEASUREMENT_READY"
    exact = _derive_scenario_decision(
        "exact",
        scenarios["exact"],
        policy,
        family_delta=EXACT_FAMILIES,
        complete=complete,
    )
    exact_multi = _derive_scenario_decision(
        "exact_multi",
        scenarios["exact_multi"],
        policy,
        family_delta=EXACT_MULTI_FAMILIES,
        complete=complete,
    )
    tool_head = _derive_scenario_decision(
        "tool_head",
        scenarios["tool_head"],
        policy,
        family_delta=TOOL_HEAD_FAMILIES,
        complete=complete,
    )
    card = _derive_scenario_decision(
        "card",
        scenarios["card"],
        policy,
        family_delta=CARD_FAMILIES,
        complete=complete,
    )

    blockers: list[str] = []
    if not complete:
        blockers.append(
            f"1x={PRODUCTION_LOGICAL_ROWS} measurements are PENDING; "
            "production winners are not READY"
        )
    for label, decision in (
        ("exact", exact),
        ("exact_multi", exact_multi),
        ("tool_head", tool_head),
        ("card", card),
    ):
        if not decision["production_winner"]:
            blockers.append(f"No {label} candidate survived the frozen T48 decision rules")

    card_winner = card["production_winner"]
    group_winners = {
        "exact": exact["production_winner"],
        "exact_multi": exact_multi["production_winner"],
        "tool_head": tool_head["production_winner"],
        t48.PUBLICATION_GROUP_EXACT: exact["production_winner"],
        t48.PUBLICATION_GROUP_EXACT_MULTI: exact_multi["production_winner"],
        t48.PUBLICATION_GROUP_TOOL_HEAD: tool_head["production_winner"],
    }
    aggregate_ready = (
        card_winner is not None
        and exact["production_winner"]
        and exact_multi["production_winner"]
        and tool_head["production_winner"]
    )

    return {
        "status": (
            "PRODUCTION_WINNER_READY"
            if aggregate_ready and complete
            else "PRODUCTION_WINNER_BLOCKED"
        ),
        "algorithm": copy.deepcopy(policy["winner_algorithm"]),
        "ranking_scale": PRODUCTION_SCALE,
        "ranking_logical_rows": PRODUCTION_LOGICAL_ROWS,
        "group_winners": group_winners,
        "card_aggregate_winner": card_winner,
        "scenarios": {
            "exact": exact,
            "exact_multi": exact_multi,
            "tool_head": tool_head,
            "card": card,
        },
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
        measurement.get("status") == "T48_MATERIALIZATION_MEASUREMENT_READY"
        and decision["status"] == "PRODUCTION_WINNER_READY"
    )
    return {
        "schema_version": 1,
        "status": (
            "T48_MATERIALIZATION_DECISION_READY"
            if complete
            else "T48_MATERIALIZATION_DECISION_BLOCKED"
        ),
        "measurement_status": measurement.get("status"),
        "decision": decision,
        "currentness": {
            "measurement_sha256": canonical_digest(measurement),
            "production_lock_sha256": t48.production_lock_sha256(),
            "policy_path": POLICY.relative_to(ROOT).as_posix(),
            "policy_sha256": sha256(POLICY),
            "load_budget_policy_v2_sha256": v2_budget.policy_sha256(),
            "builder_path": Path(__file__).resolve()
            .relative_to(ROOT).as_posix(),
            "builder_sha256": sha256(Path(__file__).resolve()),
        },
    }


def validate_artifact(artifact: dict[str, Any]) -> None:
    if artifact.get("schema_version") != 1:
        raise ValueError("T48 decision schema drifted")
    if not MEASUREMENTS.is_file():
        raise FileNotFoundError(
            f"missing measurements: {MEASUREMENTS.relative_to(ROOT).as_posix()}"
        )
    policy = load(POLICY)
    measurement = load(MEASUREMENTS)
    expected = derive_decision(measurement, policy)
    if artifact.get("decision") != expected:
        raise ValueError("T48 derived winner data is stale or hand-filled")
    built = build(measurement, policy)
    if artifact.get("currentness") != built["currentness"]:
        raise ValueError("T48 decision currentness evidence drifted")
    if artifact.get("status") != built["status"]:
        raise ValueError("T48 decision status drifted")


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
            print("T48 materialization artifact is stale or invalid:")
            for error in errors:
                print(f"- {error}")
            return 1
        print("T48 materialization artifact is current and recomputable.")
        return 0

    artifact = write()
    decision = artifact["decision"]
    print(
        "T48 group winners: "
        f"exact={decision['group_winners']['exact']}; "
        f"exact_multi={decision['group_winners']['exact_multi']}; "
        f"tool_head={decision['group_winners']['tool_head']}; "
        f"card={decision['card_aggregate_winner']}; "
        f"status: {artifact['status']}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
