#!/usr/bin/env python3
"""Validate T41 materialization measurements and derive production winners."""

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

from tools import t41_common as t41  # noqa: E402

POLICY = ROOT / "tools" / "t41_materialization_policy.json"
MEASUREMENTS = ROOT / "tools" / "t41_materialization_measurements.json"
OUTPUT = ROOT / "tools" / "t41_materialization_decision.json"
GENERATED_ROOT = ROOT / "src" / "t41_recipe_generated"
T39_READINESS = ROOT / "tools" / "t40_readiness.json"

CANDIDATES = ("immediate", "on_demand", "hybrid")
PRODUCTION_SCALE = "1x"
CARD_FAMILIES = t41.production_family_count()
PRODUCTION_LOGICAL_ROWS = t41.production_relation_count()
_GROUP_COUNTS = t41.production_group_counts()
PLANKS_FAMILIES = _GROUP_COUNTS[t41.PLANKS_GROUP]["families"]
FIREPROOF_FAMILIES = _GROUP_COUNTS[t41.FIREPROOF_GROUP]["families"]
PLANKS2_FAMILIES = _GROUP_COUNTS[t41.PLANKS2_GROUP]["families"]
PLANKS_LOGICAL = _GROUP_COUNTS[t41.PLANKS_GROUP]["relations"]
FIREPROOF_LOGICAL = _GROUP_COUNTS[t41.FIREPROOF_GROUP]["relations"]
PLANKS2_LOGICAL = _GROUP_COUNTS[t41.PLANKS2_GROUP]["relations"]
SCENARIO_IDS = ("planks", "fireproof", "planks2", "card")
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
    "shard_routing",
)
COUNTABLE_AXES = (
    "datapack_authored_entries",
    "eager_publication_rows",
    "lazy_logical_rows",
    "lazy_cache_ceiling_rows",
)
MAX_INTERVAL_SOFT_WARNING_AXES = ("lookup_candidate_count",)
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


def t41_opening() -> dict[str, Any]:
    closing = t41.t14_opening_load()
    return {
        "source": t41.T14_OPENING_LOAD_SOURCE,
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
    root = (
        GENERATED_ROOT
        / "resources/data/cruciblecraft/recipe/t41/assembler"
    )
    for path in sorted(root.glob("*.json")):
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
        or policy.get("status") != "T41_MATERIALIZATION_CANDIDATE_POLICY"
    ):
        raise ValueError("T41 materialization policy header drifted")
    scope = policy.get("scope", {})
    if (
        scope.get("family_count") != CARD_FAMILIES
        or scope.get("production_logical_rows") != PRODUCTION_LOGICAL_ROWS
        or scope.get("planks_families") != PLANKS_FAMILIES
        or scope.get("fireproof_families") != FIREPROOF_FAMILIES
        or scope.get("planks2_families") != PLANKS2_FAMILIES
    ):
        raise ValueError("T41 policy work set drifted from the production lock")
    if scope.get("production_lock_sha256") != t41.production_lock_sha256():
        raise ValueError("T41 policy production lock hash drifted")
    contract = policy.get("candidate_contract", {})
    if tuple(contract.get("candidates") or ()) != CANDIDATES:
        raise ValueError("T41 candidate set or order drifted")
    for group_key in ("planks", "fireproof", "planks2"):
        hybrid = policy["publication_group_contract"][group_key]["hybrid_boundary"]
        if hybrid.get("declared_before_measurement") is not True:
            raise ValueError(f"T41 {group_key} hybrid boundary must be frozen before measuring")
        partition_pair = (
            hybrid.get("declared_eager_count"),
            hybrid.get("declared_lazy_count"),
        )
        if partition_pair in FORBIDDEN_PARTITIONS:
            raise ValueError(f"T41 {group_key} must not copy T37/T38 hybrid partitions")
        if partition_pair == (0, 73):
            raise ValueError("T41 must not copy T38 0/73 on_demand partition as hybrid")
        declared = hybrid.get("eager_stable_ids") or []
        computed = hybrid_eager_stable_ids(policy, group_key)
        if computed != declared:
            raise ValueError(f"T41 {group_key} hybrid eager subset is stale vs generated JSON")
    card = policy["publication_group_contract"]["card_aggregate"]
    if card.get("logical_rows") != PRODUCTION_LOGICAL_ROWS:
        raise ValueError("T41 card aggregate logical rows drifted")
    does_not_modify = scope.get("does_not_modify") or []
    if not any("T37 14/36/8" in item for item in does_not_modify):
        raise ValueError("T41 policy must record does_not_modify T37 14/36/8")
    if not any("T38 0/73/16" in item for item in does_not_modify):
        raise ValueError("T41 policy must record does_not_modify T38 0/73/16")
    if not any("T39 19/13" in item for item in does_not_modify):
        raise ValueError("T41 policy must record does_not_modify T39 19/13")
    if not any("T40 cache 11" in item for item in does_not_modify):
        raise ValueError("T41 policy must record does_not_modify T40 cache 11")
    winner = policy.get("winner_algorithm", {})
    if (
        winner.get("composite_score_forbidden") is not True
        or winner.get("per_group_winners_required") is not True
        or winner.get("card_aggregate_required") is not True
        or winner.get("gate_order")
        != ["correctness", "hard_ceiling", "cumulative_soft_budget"]
    ):
        raise ValueError("T41 winner algorithm drifted")
    opening = policy.get("t41_opening", {})
    live = t41_opening()
    if (
        opening.get("datapack_authored_entries", {}).get("closing")
        != live["datapack_authored_entries"]
        or opening.get("pending_runtime_axes_zero_fill") is not False
    ):
        raise ValueError("T41 opening does not match T40 t14_load closing")


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


def _require_pending_side(side: dict[str, Any], context: str) -> None:
    if side.get("status") != PENDING:
        raise ValueError(f"{context}: expected {PENDING} until measured")
    if not side.get("reason"):
        raise ValueError(f"{context}: PENDING lacks a reason")
    for invented in ("p50_ns", "p95_ns", "raw_samples_ns"):
        if invented in side and side[invented] is not None:
            raise ValueError(f"{context}: invented {invented} while PENDING")


def _validate_pass_candidate(row: dict[str, Any], candidate: str) -> None:
    gates = row.get("gates", {})
    for gate in ("field_equivalence", "full_enumeration", "client_consistency"):
        if gates.get(gate) is not True:
            raise ValueError(f"{candidate}: PASS requires {gate}=true from JUnit evidence")
    shard = row.get("shard_routing", {})
    if shard.get("overflow_count", 0) > 128:
        raise ValueError(f"{candidate}: overflow exceeded hard ceiling 128")
    routed = shard.get("routed_candidates") or {}
    if routed.get("max", 0) > 128:
        raise ValueError(f"{candidate}: indexed candidates exceeded hard ceiling 128")
    for side_name in TIMING_SIDES:
        side = row.get(side_name, {})
        if side.get("status") != "PASS":
            raise ValueError(f"{candidate}/{side_name}: expected PASS")
    lookup = row.get("lookup", {})
    if lookup.get("candidates_p95", 0) > 128:
        raise ValueError(f"{candidate}: lookup candidates_p95 exceeded 128")


def validate_measurement(
    measurement: dict[str, Any],
    policy: dict[str, Any],
) -> None:
    validate_policy(policy)
    if measurement.get("schema_version") != 1:
        raise ValueError("T41 measurement schema drifted")
    if measurement.get("production_winner_claimed") is not False:
        raise ValueError("raw measurements must not claim a production winner")
    if "production_winner" in measurement:
        raise ValueError("hand-filled production_winner is forbidden in measurements")
    measured_rows = measurement.get("input", {}).get("measured_logical_rows")
    if (
        not isinstance(measured_rows, list)
        or sorted(measured_rows)
        != sorted([PLANKS_LOGICAL, FIREPROOF_LOGICAL, PLANKS2_LOGICAL, PRODUCTION_LOGICAL_ROWS])
    ):
        raise ValueError("1x measured_logical_rows must match the production lock")
    if measurement["input"].get("logical_rows") != PRODUCTION_LOGICAL_ROWS:
        raise ValueError("input.logical_rows must match the production lock")
    if measurement["input"].get("generated_datapack_present") != generated_datapack_present():
        raise ValueError("generated datapack presence drifted")

    live_opening = t41_opening()
    opening = measurement.get("t41_opening", {})
    if opening.get("datapack_authored_entries") != live_opening["datapack_authored_entries"]:
        raise ValueError("measurement T41 opening drifted")

    scenarios = {
        row["id"]: row
        for row in measurement.get("scenarios") or []
        if row.get("id") in SCENARIO_IDS
    }
    if set(scenarios) != set(SCENARIO_IDS):
        raise ValueError("planks/fireproof/planks2/card scenarios are required")
    for scenario_id in SCENARIO_IDS:
        scenario = scenarios[scenario_id]
        expected_rows = {
            "planks": PLANKS_LOGICAL,
            "fireproof": FIREPROOF_LOGICAL,
            "planks2": PLANKS2_LOGICAL,
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
            elif row.get("status") != PENDING:
                raise ValueError(f"{scenario_id}/{candidate}: status must be PASS or PENDING")


def _cumulative_counts(
    policy: dict[str, Any],
    row: dict[str, Any],
    *,
    family_delta: int,
) -> dict[str, int]:
    opening = policy["t41_opening"]
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
        counts = _cumulative_counts(policy, row, family_delta=family_delta)
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
                    note = f"soft_budget:{axis}:{actual}>{policy['soft_budgets_1x'][axis]}"
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
    complete = measurement.get("status") == "T41_MATERIALIZATION_MEASUREMENT_READY"
    singleton = _derive_scenario_decision(
        "planks",
        scenarios["planks"],
        policy,
        family_delta=PLANKS_FAMILIES,
        complete=complete,
    )
    multi = _derive_scenario_decision(
        "fireproof",
        scenarios["fireproof"],
        policy,
        family_delta=FIREPROOF_FAMILIES,
        complete=complete,
    )
    planks2 = _derive_scenario_decision(
        "planks2",
        scenarios["planks2"],
        policy,
        family_delta=PLANKS2_FAMILIES,
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
        ("planks", singleton),
        ("fireproof", multi),
        ("planks2", planks2),
        ("card", card),
    ):
        if not decision["production_winner"]:
            blockers.append(f"No {label} candidate survived the frozen T41 decision rules")

    card_winner = card["production_winner"]
    group_winners = {
        "planks": singleton["production_winner"],
        "fireproof": multi["production_winner"],
        "planks2": planks2["production_winner"],
    }
    aggregate_ready = (
        card_winner is not None
        and all(group_winners.values())
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
            "planks": singleton,
            "fireproof": multi,
            "planks2": planks2,
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
        measurement.get("status") == "T41_MATERIALIZATION_MEASUREMENT_READY"
        and decision["status"] == "PRODUCTION_WINNER_READY"
    )
    return {
        "schema_version": 1,
        "status": (
            "T41_MATERIALIZATION_DECISION_READY"
            if complete
            else "T41_MATERIALIZATION_DECISION_BLOCKED"
        ),
        "measurement_status": measurement.get("status"),
        "decision": decision,
        "currentness": {
            "measurement_sha256": canonical_digest(measurement),
            "production_lock_sha256": t41.production_lock_sha256(),
            "policy_path": POLICY.relative_to(ROOT).as_posix(),
            "policy_sha256": sha256(POLICY),
            "builder_path": Path(__file__).resolve()
            .relative_to(ROOT).as_posix(),
            "builder_sha256": sha256(Path(__file__).resolve()),
        },
    }


def validate_artifact(artifact: dict[str, Any]) -> None:
    if artifact.get("schema_version") != 1:
        raise ValueError("T41 decision schema drifted")
    if not MEASUREMENTS.is_file():
        raise FileNotFoundError(
            f"missing measurements: {MEASUREMENTS.relative_to(ROOT).as_posix()}"
        )
    policy = load(POLICY)
    measurement = load(MEASUREMENTS)
    expected = derive_decision(measurement, policy)
    if artifact.get("decision") != expected:
        raise ValueError("T41 derived winner data is stale or hand-filled")
    built = build(measurement, policy)
    if artifact.get("currentness") != built["currentness"]:
        raise ValueError("T41 decision currentness evidence drifted")
    if artifact.get("status") != built["status"]:
        raise ValueError("T41 decision status drifted")


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
            print("T41 materialization artifact is stale or invalid:")
            for error in errors:
                print(f"- {error}")
            return 1
        print("T41 materialization artifact is current and recomputable.")
        return 0

    artifact = write()
    decision = artifact["decision"]
    print(
        "T41 group winners: "
        f"planks={decision['group_winners']['planks']}; "
        f"fireproof={decision['group_winners']['fireproof']}; "
        f"planks2={decision['group_winners']['planks2']}; "
        f"card={decision['card_aggregate_winner']}; "
        f"status: {artifact['status']}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
