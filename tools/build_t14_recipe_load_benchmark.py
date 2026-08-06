#!/usr/bin/env python3
"""Validate T14c compact measurements and derive the production winner."""

from __future__ import annotations

import argparse
import copy
import hashlib
import json
import math
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
POLICY = ROOT / "tools" / "t14_materialization_policy.json"
OUTPUT = ROOT / "tools" / "t14_materialization_decision.json"
DEFAULT_RAW = ROOT / "build" / "t14-benchmark" / "raw_measurements.json"
JAVA_ROOT = ROOT / "src" / "t14Benchmark" / "java"

CANDIDATES = ("immediate", "on_demand", "hybrid")
SCALES = ("baseline", "1x", "5x", "20x")
GATES = (
    "correctness",
    "epoch_invalidation",
    "dedicated_client_enumeration",
    "integrated_client_skip",
)
LOOKUP_TRACES = (
    "uniform_cycle_worst_case",
    "locality_80_20",
    "repeated_current_input",
)
CACHE_POLICIES = {
    "immediate": ("FULLY_EAGER", 0),
    "on_demand": ("NO_CACHE", 0),
    "hybrid": ("BOUNDED_ACCESS_ORDER_LRU", 512),
}
HARD_LIMIT_PATHS = {
    "server_reload_p95_ns": ("server", "reload", "p95_ns"),
    "dedicated_client_reexpansion_p95_ns": (
        "dedicated_client",
        "reload",
        "p95_ns",
    ),
    "retained_total_bytes_p50": (
        "retained_memory",
        "total_bytes",
        "p50_bytes",
    ),
    "lookup_p95_ns": ("lookup", "p95_ns"),
    "enumeration_p95_ns": ("enumeration", "p95_ns"),
    "sync_bytes": ("sync", "bytes"),
    "jfr_allocation_bytes_p50": (
        "jfr_allocation",
        "p50_bytes",
    ),
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


def current_java_hashes() -> dict[str, str]:
    paths = sorted(JAVA_ROOT.rglob("*.java"))
    if not paths:
        raise ValueError("T14b Java source set is empty")
    return {
        path.relative_to(ROOT).as_posix(): sha256(path)
        for path in paths
    }


def validate_policy(policy: dict[str, Any]) -> None:
    if (
        policy.get("schema_version") != 2
        or policy.get("status") != "T14C_PRODUCTION_MEASUREMENT_POLICY"
        or policy.get("scope", {}).get("benchmark_only") is not False
        or policy["scope"].get("production_switch_allowed") is not True
    ):
        raise ValueError("T14c policy scope or schema drifted")
    supersession = policy.get("supersession", {})
    if (
        supersession.get("supersedes_policy_schema") != 1
        or supersession.get("supersedes_measurement_schema") != 1
        or not supersession.get("reason")
    ):
        raise ValueError("T14c policy supersession is incomplete")
    input_policy = policy.get("input", {})
    if (
        input_policy.get("kind") != "t14a_compact_relations"
        or input_policy.get("base_relation_count") != 2782
        or input_policy.get("compact_path")
        != "tools/t14_extruder_compact.json"
        or input_policy.get("compact_sha256")
        != sha256(ROOT / input_policy["compact_path"])
        or not isinstance(input_policy.get("compact_fingerprint"), str)
        or len(input_policy["compact_fingerprint"]) != 64
    ):
        raise ValueError("T14c compact input guard drifted")

    scales = policy.get("scales") or []
    expected_counts = {
        "baseline": 2782,
        "1x": 2782,
        "5x": 13910,
        "20x": 55640,
    }
    if (
        [row.get("id") for row in scales] != list(SCALES)
        or {row["id"]: row.get("relation_count") for row in scales}
        != expected_counts
    ):
        raise ValueError("T14b scale policy drifted")
    contract = policy.get("candidate_contract", {})
    if tuple(contract.get("candidates") or ()) != CANDIDATES:
        raise ValueError("T14b candidate set or order drifted")
    if contract.get("cache_policies") != {
        "immediate": "fully eager; lookup cache disabled",
        "on_demand": "materialize every lookup; zero retained cache",
        "hybrid": (
            "synchronized access-order LinkedHashMap LRU with real "
            "eldest eviction at 512"
        ),
    }:
        raise ValueError("T14c candidate cache policies drifted")
    workload = policy.get("workload", {})
    if (
        tuple(workload.get("lookup_traces") or ()) != LOOKUP_TRACES
        or workload.get("lookup_trace_source")
        != "actual compact lazyByItem groups"
        or workload.get("lookup_hard_limit_aggregation")
        != "maximum trace p95"
    ):
        raise ValueError("T14c lookup trace policy drifted")
    if not all(
        isinstance(value, int) and value > 0
        for value in policy.get("hard_limits_20x", {}).values()
    ):
        raise ValueError("T14b 20x hard limits must be positive")
    if set(policy["hard_limits_20x"]) != set(HARD_LIMIT_PATHS):
        raise ValueError("T14b hard-limit metric set drifted")
    winner = policy.get("winner_algorithm", {})
    if (
        winner.get("ranking_scale") != "20x"
        or winner.get("ranking_metrics")
        != [
            "server_reload",
            "dedicated_client_reexpansion",
            "retained_total_bytes",
            "lookup_p95",
        ]
        or winner.get("tie_preference")
        != ["hybrid", "immediate", "on_demand"]
    ):
        raise ValueError("T14b winner algorithm drifted")
    retained = policy.get("retained_memory_protocol", {})
    if (
        retained.get("forbidden") != "naive before/after heap delta"
        or retained.get("jcmd_unavailable") != "SKIP_AND_BLOCK_DECISION"
        or "GC.class_histogram"
        not in " ".join(retained.get("controlled_gc") or [])
    ):
        raise ValueError("T14b retained-memory protocol drifted")
    allocation = policy.get("allocation_protocol", {})
    if (
        allocation.get("trace") != "uniform_cycle_worst_case"
        or allocation.get("enumeration_included") is not False
        or allocation.get("publication_included") is not False
        or "zero matching allocation events"
        not in allocation.get("zero_event_semantics", "")
    ):
        raise ValueError("T14c allocation protocol drifted")


def _nearest_rank(values: list[int], percentile: float) -> int:
    ordered = sorted(values)
    return ordered[max(0, math.ceil(percentile * len(ordered)) - 1)]


def _validate_timing(
    timing: dict[str, Any],
    context: str,
    expected_samples: int | None = None,
) -> None:
    raw = timing.get("raw_samples_ns")
    if (
        timing.get("status") != "PASS"
        or not isinstance(raw, list)
        or not raw
        or any(not isinstance(value, int) or value <= 0 for value in raw)
        or timing.get("samples") != len(raw)
        or (
            expected_samples is not None
            and len(raw) != expected_samples
        )
    ):
        raise ValueError(f"{context}: invalid positive timing samples")
    expected = {
        "p50_ns": _nearest_rank(raw, 0.50),
        "p95_ns": _nearest_rank(raw, 0.95),
        "max_ns": max(raw),
    }
    if any(timing.get(key) != value for key, value in expected.items()):
        raise ValueError(f"{context}: timing summary is not recomputable")
    for field in ("error_bar", "p95_error_bar"):
        band = timing.get(field, {})
        lower = band.get("lower_ns")
        upper = band.get("upper_ns")
        if (
            not isinstance(lower, int)
            or not isinstance(upper, int)
            or lower <= 0
            or lower > upper
            or lower not in raw
            or upper not in raw
        ):
            raise ValueError(f"{context}: invalid {field}")


def _validate_range(
    measurement: dict[str, Any],
    unit: str,
    context: str,
    expected_samples: int,
    allow_zero: bool = False,
) -> None:
    raw = measurement.get(f"raw_samples_{unit}")
    if (
        measurement.get("status") != "PASS"
        or not isinstance(raw, list)
        or len(raw) != expected_samples
        or measurement.get("samples") != len(raw)
        or any(
            not isinstance(value, int)
            or value < 0
            or (value == 0 and not allow_zero)
            for value in raw
        )
    ):
        raise ValueError(f"{context}: invalid positive range samples")
    expected = {
        f"p50_{unit}": _nearest_rank(raw, 0.50),
        f"min_{unit}": min(raw),
        f"max_{unit}": max(raw),
    }
    if any(measurement.get(key) != value for key, value in expected.items()):
        raise ValueError(f"{context}: range summary is not recomputable")
    band = measurement.get("error_bar", {})
    if (
        band.get(f"lower_{unit}") != min(raw)
        or band.get(f"upper_{unit}") != max(raw)
    ):
        raise ValueError(f"{context}: range error bar drifted")


def _validate_optional_measurement(
    measurement: dict[str, Any],
    context: str,
    expected_samples: int,
    allow_zero: bool = False,
) -> None:
    status = measurement.get("status")
    if status == "SKIP":
        if (
            measurement.get("samples") != 0
            or not measurement.get("reason")
        ):
            raise ValueError(f"{context}: SKIP lacks explicit evidence")
        return
    if status != "PASS":
        raise ValueError(f"{context}: expected PASS or explicit SKIP")
    _validate_range(
        measurement,
        "bytes",
        context,
        expected_samples,
        allow_zero=allow_zero,
    )


def _validate_candidate(
    row: dict[str, Any],
    policy: dict[str, Any],
    scale: str,
) -> None:
    candidate = row.get("candidate")
    context = f"{scale}/{candidate}"
    gates = row.get("gates", {})
    if any(not isinstance(gates.get(gate), bool) for gate in GATES):
        raise ValueError(f"{context}: gate booleans are incomplete")
    if gates.get("all_pass") != all(gates[gate] for gate in GATES):
        raise ValueError(f"{context}: all_pass is not recomputable")

    workload = policy["workload"]
    for side in ("server", "dedicated_client"):
        phase = row.get(side, {})
        if phase.get("status") != "PASS":
            raise ValueError(f"{context}/{side}: publication did not PASS")
        fingerprint = phase.get("stable_fingerprint")
        if (
            not isinstance(fingerprint, str)
            or len(fingerprint) != 64
            or any(character not in "0123456789abcdef"
                   for character in fingerprint)
        ):
            raise ValueError(
                f"{context}/{side}: stable fingerprint is invalid"
            )
        for metric in ("expansion", "index", "reload"):
            _validate_timing(
                phase.get(metric, {}),
                f"{context}/{side}/{metric}",
                workload["reload_samples"],
            )
    if (
        row["server"]["stable_fingerprint"]
        != row["dedicated_client"]["stable_fingerprint"]
    ):
        raise ValueError(
            f"{context}: client/server stable fingerprints differ"
        )
    integrated = row.get("integrated_client", {})
    if (
        integrated.get("status") != "SKIP"
        or integrated.get("independent_reexpansion") is not False
        or integrated.get("reexpansion_ns") is not None
        or not integrated.get("reason")
    ):
        raise ValueError(f"{context}: integrated skip gate drifted")

    lookup = row.get("lookup", {})
    _validate_timing(
        lookup,
        f"{context}/lookup",
        workload["lookup_samples"],
    )
    if (
        lookup.get("indexed_relation_lookup") is not True
        or lookup.get("operations_per_sample")
        != workload["lookup_operations_per_sample"]
        or lookup.get("hard_limit_aggregation") != "maximum trace p95"
    ):
        raise ValueError(f"{context}: lookup contract drifted")
    expected_candidate_samples = (
        workload["lookup_samples"]
        * workload["lookup_operations_per_sample"]
    )
    actual_index = lookup.get("actual_lazy_by_item", {})
    if (
        actual_index.get("lazy_relations", 0) <= 0
        or actual_index.get("input_groups", 0) <= 0
        or actual_index.get("max_candidates_per_input", 0) <= 0
        or actual_index["max_candidates_per_input"]
        > workload["relation_candidates_per_input"]
    ):
        raise ValueError(f"{context}: actual lazyByItem geometry drifted")
    traces = lookup.get("traces") or []
    if [trace.get("trace") for trace in traces] != list(LOOKUP_TRACES):
        raise ValueError(f"{context}: lookup trace set/order drifted")
    expected_policy, expected_ceiling = CACHE_POLICIES[candidate]
    for trace in traces:
        trace_context = f"{context}/lookup/{trace['trace']}"
        _validate_timing(
            trace,
            trace_context,
            workload["lookup_samples"],
        )
        if (
            trace.get("operations") != expected_candidate_samples
            or trace.get("operations_per_sample")
            != workload["lookup_operations_per_sample"]
            or trace.get("indexed_relation_lookup") is not True
            or not trace.get("description")
        ):
            raise ValueError(f"{trace_context}: trace contract drifted")
        candidates = trace.get("candidates", {})
        raw_candidates = candidates.get("raw_samples")
        if (
            not isinstance(raw_candidates, list)
            or len(raw_candidates) != expected_candidate_samples
            or candidates.get("samples") != len(raw_candidates)
            or any(
                not isinstance(value, int)
                or value <= 0
                or value > workload["relation_candidates_per_input"]
                for value in raw_candidates
            )
            or candidates.get("p50")
            != _nearest_rank(raw_candidates, 0.50)
            or candidates.get("p95")
            != _nearest_rank(raw_candidates, 0.95)
            or candidates.get("max") != max(raw_candidates)
        ):
            raise ValueError(
                f"{trace_context}: lookup candidate samples drifted"
            )
        cache = trace.get("cache", {})
        numeric_fields = (
            "size_before",
            "size_after",
            "hits",
            "misses",
            "materializations",
            "evictions",
        )
        if (
            cache.get("policy") != expected_policy
            or cache.get("ceiling") != expected_ceiling
            or any(
                not isinstance(cache.get(field), int)
                or cache[field] < 0
                for field in numeric_fields
            )
            or cache["size_before"] > expected_ceiling
            or cache["size_after"] > expected_ceiling
        ):
            raise ValueError(f"{trace_context}: cache diagnostics drifted")
        operations = trace["operations"]
        if candidate == "immediate":
            expected = {
                "hits": 0,
                "misses": 0,
                "materializations": 0,
                "evictions": 0,
                "size_before": 0,
                "size_after": 0,
            }
            if any(cache[field] != value for field, value in expected.items()):
                raise ValueError(
                    f"{trace_context}: eager cache counters drifted"
                )
        elif candidate == "on_demand":
            if (
                cache["hits"] != 0
                or cache["misses"] != operations
                or cache["materializations"] != operations
                or cache["evictions"] != 0
                or cache["size_before"] != 0
                or cache["size_after"] != 0
            ):
                raise ValueError(
                    f"{trace_context}: zero-cache counters drifted"
                )
        elif (
            cache["hits"] + cache["misses"] != operations
            or cache["materializations"] != cache["misses"]
            or cache["size_after"]
            != cache["size_before"]
            + cache["misses"]
            - cache["evictions"]
        ):
            raise ValueError(
                f"{trace_context}: bounded LRU counters drifted"
            )
    hard_trace = max(traces, key=lambda trace: trace["p95_ns"])
    if (
        lookup.get("hard_limit_trace") != hard_trace["trace"]
        or any(
            lookup.get(field) != hard_trace.get(field)
            for field in (
                "raw_samples_ns",
                "p50_ns",
                "p95_ns",
                "max_ns",
                "error_bar",
                "p95_error_bar",
            )
        )
    ):
        raise ValueError(f"{context}: lookup hard-limit aggregate drifted")

    enumeration = row.get("enumeration", {})
    _validate_timing(
        enumeration,
        f"{context}/enumeration",
        workload["enumeration_samples"],
    )
    if enumeration.get("complete_view") is not True:
        raise ValueError(f"{context}: enumeration view is incomplete")
    sync = row.get("sync", {})
    if (
        sync.get("status") != "PASS"
        or not isinstance(sync.get("bytes"), int)
        or sync["bytes"] <= 0
    ):
        raise ValueError(f"{context}: sync bytes are not positive")

    retained = row.get("retained_memory", {})
    if retained.get("status") == "SKIP":
        if (
            retained.get("samples") != 0
            or retained.get("total_bytes") is not None
            or retained.get("naive_heap_delta_used") is not False
            or not retained.get("reason")
        ):
            raise ValueError(
                f"{context}: retained SKIP must not masquerade as zero PASS"
            )
    elif retained.get("status") == "PASS":
        if (
            retained.get("naive_heap_delta_used") is not False
            or "GC.class_histogram"
            not in retained.get("protocol", "")
            or "enumeration was not executed"
            not in retained.get("retained_scope", "")
            or retained.get("samples")
            != workload["retained_child_samples"]
        ):
            raise ValueError(f"{context}: retained protocol drifted")
        _validate_range(
            retained.get("total_bytes", {}),
            "bytes",
            f"{context}/retained total",
            workload["retained_child_samples"],
        )
        _validate_range(
            retained.get("benchmark_class_bytes", {}),
            "bytes",
            f"{context}/retained benchmark classes",
            workload["retained_child_samples"],
        )
        metadata = retained.get("sample_metadata") or []
        if (
            len(metadata) != workload["retained_child_samples"]
            or any(
                row.get("total_bytes", 0) <= 0
                or row.get("histogram_class_rows", 0) <= 0
                or "GC.run" not in row.get("controlled_gc_command", "")
                or "GC.class_histogram"
                not in row.get("histogram_command", "")
                or not row.get("java_version")
                or not row.get("gc_names")
                or row.get("heap_max_bytes", 0) <= 0
                or row.get("loaded_classes_at_ready", 0) <= 0
                or row.get("cache_policy") != CACHE_POLICIES[candidate][0]
                or row.get("cache_ceiling") != CACHE_POLICIES[candidate][1]
                or row.get("cache_size", -1) < 0
                or row.get("cache_size") > row.get("cache_ceiling")
                or row.get("cache_evictions", -1) < 0
                for row in metadata
            )
        ):
            raise ValueError(f"{context}: retained child metadata is incomplete")
    else:
        raise ValueError(f"{context}: retained status is invalid")

    _validate_optional_measurement(
        row.get("jfr_allocation", {}),
        f"{context}/JFR allocation",
        workload["jfr_allocation_samples"],
        allow_zero=True,
    )
    allocation = row.get("jfr_allocation", {})
    if (
        "lookup-only after publication" not in allocation.get("scope", "")
        or "enumeration excluded" not in allocation.get("scope", "")
        or not allocation.get("zero_event_semantics")
    ):
        raise ValueError(f"{context}: JFR lookup-only scope drifted")


def validate_measurement(
    measurement: dict[str, Any],
    policy: dict[str, Any],
) -> None:
    validate_policy(policy)
    if (
        measurement.get("schema_version") != 2
        or measurement.get("protocol", {}).get("id")
        != "t14_materialization_lookup_cache_v2"
        or measurement["protocol"].get("supersedes")
        != "t14_materialization_publication_enumeration_v1"
        or measurement.get("benchmark_only") is not False
        or measurement.get("production_winner_claimed") is not False
        or measurement.get("input", {}).get("kind")
        != policy["input"]["kind"]
        or measurement["input"].get("t14a_compact_data_present") is not True
        or measurement["input"].get("compact_path")
        != policy["input"]["compact_path"]
        or measurement["input"].get("compact_sha256")
        != policy["input"]["compact_sha256"]
        or measurement["input"].get("compact_fingerprint")
        != policy["input"]["compact_fingerprint"]
    ):
        raise ValueError("T14c raw measurement scope drifted")
    if (
        measurement.get("policy", {}).get("sha256") != sha256(POLICY)
        or measurement["policy"].get("path")
        != POLICY.relative_to(ROOT).as_posix()
    ):
        raise ValueError("T14b raw measurement policy is stale")
    if measurement.get("java_sources", {}).get("files") != current_java_hashes():
        raise ValueError("T14b raw measurement Java sources are stale")
    runtime = measurement.get("runtime", {})
    if any(
        not runtime.get(field)
        for field in ("java_version", "java_vm", "gc_names", "heap_max_bytes")
    ) or runtime.get("loaded_classes", 0) <= 0:
        raise ValueError("T14b JVM/GC/heap/class metadata is incomplete")

    scenarios = measurement.get("scenarios") or []
    expected_counts = {
        row["id"]: row["relation_count"]
        for row in policy["scales"]
    }
    if [row.get("scale") for row in scenarios] != list(SCALES):
        raise ValueError("T14b raw measurement scale set drifted")
    for scenario in scenarios:
        scale = scenario["scale"]
        if scenario.get("relation_count") != expected_counts[scale]:
            raise ValueError(f"{scale}: relation count drifted")
        expected_copies = expected_counts[scale] // 2782
        if scenario.get("actual_distribution_copies") != expected_copies:
            raise ValueError(
                f"{scale}: actual compact distribution was not replayed"
            )
        candidates = scenario.get("candidates") or []
        if [row.get("candidate") for row in candidates] != list(CANDIDATES):
            raise ValueError(f"{scale}: candidate set/order drifted")
        for candidate in candidates:
            _validate_candidate(candidate, policy, scale)


def _at(row: dict[str, Any], path: tuple[str, ...]) -> Any:
    value: Any = row
    for key in path:
        value = value[key]
    return value


def _ranking_metric(
    row: dict[str, Any],
    metric: str,
) -> tuple[float, float, float]:
    if metric == "server_reload":
        timing = row["server"]["reload"]
        band = timing["p95_error_bar"]
        return (
            timing["p95_ns"],
            band["lower_ns"],
            band["upper_ns"],
        )
    if metric == "dedicated_client_reexpansion":
        timing = row["dedicated_client"]["reload"]
        band = timing["p95_error_bar"]
        return (
            timing["p95_ns"],
            band["lower_ns"],
            band["upper_ns"],
        )
    if metric == "retained_total_bytes":
        retained = row["retained_memory"]["total_bytes"]
        band = retained["error_bar"]
        return (
            retained["p50_bytes"],
            band["lower_bytes"],
            band["upper_bytes"],
        )
    if metric == "lookup_p95":
        timing = row["lookup"]
        band = timing["p95_error_bar"]
        return (
            timing["p95_ns"],
            band["lower_ns"],
            band["upper_ns"],
        )
    raise ValueError(f"unknown ranking metric {metric}")


def derive_decision(
    measurement: dict[str, Any],
    policy: dict[str, Any],
) -> dict[str, Any]:
    validate_measurement(measurement, policy)
    scenario = next(
        row for row in measurement["scenarios"]
        if row["scale"] == policy["winner_algorithm"]["ranking_scale"]
    )
    rows = {
        row["candidate"]: row for row in scenario["candidates"]
    }
    eliminated: dict[str, list[str]] = {}
    survivors: list[str] = []
    limits = policy["hard_limits_20x"]

    for candidate in CANDIDATES:
        row = rows[candidate]
        reasons: list[str] = []
        for gate in GATES:
            if not row["gates"][gate]:
                reasons.append(f"gate_failed:{gate}")
        if row["retained_memory"]["status"] != "PASS":
            reasons.append("measurement_unavailable:retained_memory")
        if row["jfr_allocation"]["status"] != "PASS":
            reasons.append("measurement_unavailable:jfr_allocation")
        if not reasons:
            for limit_name, path in HARD_LIMIT_PATHS.items():
                actual = _at(row, path)
                if actual > limits[limit_name]:
                    reasons.append(
                        f"20x_hard_limit:{limit_name}:{actual}>"
                        f"{limits[limit_name]}"
                    )
        if reasons:
            eliminated[candidate] = reasons
        else:
            survivors.append(candidate)

    ranking: list[dict[str, Any]] = []
    prototype_preference: str | None = None
    tied_candidates: list[str] = []
    if survivors:
        metric_names = policy["winner_algorithm"]["ranking_metrics"]
        best_estimates = {
            metric: min(
                _ranking_metric(rows[candidate], metric)[0]
                for candidate in survivors
            )
            for metric in metric_names
        }
        preference = policy["winner_algorithm"]["tie_preference"]
        for candidate in survivors:
            normalized: dict[str, Any] = {}
            for metric in metric_names:
                estimate, lower, upper = _ranking_metric(
                    rows[candidate], metric
                )
                denominator = best_estimates[metric]
                normalized[metric] = {
                    "raw_estimate": estimate,
                    "raw_error_lower": lower,
                    "raw_error_upper": upper,
                    "best_survivor_estimate": denominator,
                    "normalized_estimate": estimate / denominator,
                    "normalized_lower": lower / denominator,
                    "normalized_upper": upper / denominator,
                }
            worst_estimate = max(
                value["normalized_estimate"]
                for value in normalized.values()
            )
            worst_lower = max(
                value["normalized_lower"]
                for value in normalized.values()
            )
            worst_upper = max(
                value["normalized_upper"]
                for value in normalized.values()
            )
            ranking.append({
                "candidate": candidate,
                "normalized_metrics": normalized,
                "worst_normalized_cost": worst_estimate,
                "worst_normalized_error_lower": worst_lower,
                "worst_normalized_error_upper": worst_upper,
            })
        ranking.sort(key=lambda value: (
            value["worst_normalized_cost"],
            preference.index(value["candidate"]),
        ))
        best = ranking[0]
        tied_candidates = [
            row["candidate"] for row in ranking
            if (
                row["worst_normalized_error_lower"]
                <= best["worst_normalized_error_upper"]
                and best["worst_normalized_error_lower"]
                <= row["worst_normalized_error_upper"]
            )
        ]
        prototype_preference = next(
            candidate for candidate in preference
            if candidate in tied_candidates
        )

    production_input_kind = measurement["input"]["kind"]
    required_kind = "t14a_compact_relations"
    production_allowed = (
        production_input_kind == required_kind
        and measurement["input"].get("t14a_compact_data_present") is True
        and measurement["input"].get("compact_sha256")
        == policy["input"]["compact_sha256"]
        and measurement["input"].get("compact_fingerprint")
        == policy["input"]["compact_fingerprint"]
    )
    production_winner = (
        prototype_preference if production_allowed else None
    )
    blockers = []
    if not production_allowed:
        blockers.append(
            f"input_kind={production_input_kind}; required={required_kind}"
        )
    if measurement["input"].get("t14a_compact_data_present") is not True:
        blockers.append("T14a compact relation evidence is absent")
    if not prototype_preference:
        blockers.append("No candidate survived complete 20x evidence gates")

    return {
        "status": (
            "PRODUCTION_WINNER_READY"
            if production_winner
            else "PRODUCTION_WINNER_BLOCKED"
        ),
        "algorithm": copy.deepcopy(policy["winner_algorithm"]),
        "ranking_scale": scenario["scale"],
        "hard_limits": copy.deepcopy(limits),
        "eliminated": eliminated,
        "survivors": survivors,
        "prototype_ranking": ranking,
        "error_band_tied_candidates": tied_candidates,
        "prototype_preference": prototype_preference,
        "production_winner": production_winner,
        "production_blockers": blockers,
    }


def derive_key_measurements(
    measurement: dict[str, Any],
) -> list[dict[str, Any]]:
    result: list[dict[str, Any]] = []
    for scenario in measurement["scenarios"]:
        candidates = []
        for row in scenario["candidates"]:
            retained = row["retained_memory"]
            allocation = row["jfr_allocation"]
            lookup_traces = [
                {
                    "trace": trace["trace"],
                    "p50_ns": trace["p50_ns"],
                    "p95_ns": trace["p95_ns"],
                    "max_ns": trace["max_ns"],
                    "candidates_p50": trace["candidates"]["p50"],
                    "candidates_p95": trace["candidates"]["p95"],
                    "candidates_max": trace["candidates"]["max"],
                    "cache": trace["cache"],
                }
                for trace in row["lookup"]["traces"]
            ]
            hard_trace = next(
                trace for trace in lookup_traces
                if trace["trace"] == row["lookup"]["hard_limit_trace"]
            )
            candidates.append({
                "candidate": row["candidate"],
                "server_expansion_p95_ns": row["server"][
                    "expansion"
                ]["p95_ns"],
                "server_index_p95_ns": row["server"]["index"]["p95_ns"],
                "server_reload_p95_ns": row["server"]["reload"]["p95_ns"],
                "server_stable_fingerprint": row["server"][
                    "stable_fingerprint"
                ],
                "dedicated_client_reexpansion_p95_ns": row[
                    "dedicated_client"
                ]["reload"]["p95_ns"],
                "dedicated_client_index_p95_ns": row[
                    "dedicated_client"
                ]["index"]["p95_ns"],
                "dedicated_client_stable_fingerprint": row[
                    "dedicated_client"
                ]["stable_fingerprint"],
                "integrated_client": "SKIP_SHARED_SERVER_PUBLICATION",
                "lookup_p50_ns": row["lookup"]["p50_ns"],
                "lookup_p95_ns": row["lookup"]["p95_ns"],
                "lookup_max_ns": row["lookup"]["max_ns"],
                "lookup_hard_limit_trace": hard_trace["trace"],
                "lookup_candidates_p50": max(
                    trace["candidates_p50"] for trace in lookup_traces
                ),
                "lookup_candidates_p95": max(
                    trace["candidates_p95"] for trace in lookup_traces
                ),
                "lookup_candidates_max": max(
                    trace["candidates_max"] for trace in lookup_traces
                ),
                "lookup_traces": lookup_traces,
                "enumeration_p95_ns": row["enumeration"]["p95_ns"],
                "sync_bytes": row["sync"]["bytes"],
                "retained_status": retained["status"],
                "retained_total_bytes_p50": (
                    retained["total_bytes"]["p50_bytes"]
                    if retained["status"] == "PASS"
                    else None
                ),
                "jfr_allocation_status": allocation["status"],
                "jfr_allocation_bytes_p50": (
                    allocation["p50_bytes"]
                    if allocation["status"] == "PASS"
                    else None
                ),
            })
        result.append({
            "scale": scenario["scale"],
            "relation_count": scenario["relation_count"],
            "candidates": candidates,
        })
    return result


def build(
    measurement: dict[str, Any],
    policy: dict[str, Any] | None = None,
) -> dict[str, Any]:
    policy = load(POLICY) if policy is None else policy
    decision = derive_decision(measurement, policy)
    complete = all(
        row["retained_memory"]["status"] == "PASS"
        and row["jfr_allocation"]["status"] == "PASS"
        for scenario in measurement["scenarios"]
        for row in scenario["candidates"]
    )
    return {
        "schema_version": 2,
        "status": (
            "T14C_PRODUCTION_DECISION_READY"
            if complete and decision["production_winner"]
            else "T14C_PRODUCTION_DECISION_BLOCKED"
        ),
        "measurement": measurement,
        "key_measurements": derive_key_measurements(measurement),
        "decision": decision,
        "currentness": {
            "measurement_sha256": canonical_digest(measurement),
            "policy_path": POLICY.relative_to(ROOT).as_posix(),
            "policy_sha256": sha256(POLICY),
            "java_sources": current_java_hashes(),
            "builder_path": Path(__file__).resolve()
            .relative_to(ROOT).as_posix(),
            "builder_sha256": sha256(Path(__file__).resolve()),
        },
    }


def validate_artifact(artifact: dict[str, Any]) -> None:
    if artifact.get("schema_version") != 2:
        raise ValueError("T14c artifact schema drifted")
    policy = load(POLICY)
    measurement = artifact.get("measurement")
    if not isinstance(measurement, dict):
        raise ValueError("T14b artifact has no raw measurement")
    validate_measurement(measurement, policy)
    if artifact.get("decision") != derive_decision(measurement, policy):
        raise ValueError("T14b derived winner data is stale")
    if artifact.get("key_measurements") != derive_key_measurements(
        measurement
    ):
        raise ValueError("T14b key measurement summary is stale")
    currentness = artifact.get("currentness", {})
    expected = {
        "measurement_sha256": canonical_digest(measurement),
        "policy_path": POLICY.relative_to(ROOT).as_posix(),
        "policy_sha256": sha256(POLICY),
        "java_sources": current_java_hashes(),
        "builder_path": Path(__file__).resolve()
        .relative_to(ROOT).as_posix(),
        "builder_sha256": sha256(Path(__file__).resolve()),
    }
    if currentness != expected:
        raise ValueError("T14b measurement currentness evidence drifted")
    if artifact["decision"].get("production_winner") is None:
        raise ValueError(
            "T14c compact evidence must derive a production winner"
        )


def check() -> list[str]:
    if not OUTPUT.is_file():
        return [f"missing generated file: {OUTPUT.relative_to(ROOT).as_posix()}"]
    try:
        validate_artifact(load(OUTPUT))
    except (KeyError, TypeError, ValueError) as failure:
        return [str(failure)]
    return []


def write(raw_path: Path) -> dict[str, Any]:
    measurement = load(raw_path)
    artifact = build(measurement)
    OUTPUT.write_text(
        stable_json(artifact),
        encoding="utf-8",
        newline="\n",
    )
    return artifact


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--raw",
        type=Path,
        default=DEFAULT_RAW,
        help="raw Java measurement JSON",
    )
    parser.add_argument(
        "--check",
        action="store_true",
        help="validate committed artifact currentness and derivation",
    )
    args = parser.parse_args()
    if args.check:
        errors = check()
        if errors:
            print("T14b measurement artifact is stale or invalid:")
            for error in errors:
                print(f"- {error}")
            return 1
        print("T14c measurement artifact is current and recomputable.")
        return 0

    artifact = write(args.raw)
    decision = artifact["decision"]
    print(
        "T14c measured preference: "
        f"{decision['prototype_preference']}; "
        f"production winner: {decision['production_winner']}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
