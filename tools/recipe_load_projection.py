#!/usr/bin/env python3
"""Build a fail-closed, per-family recipe load and budget projection."""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
from pathlib import Path
from typing import Any, Iterable


ROOT = Path(__file__).resolve().parents[1]
T13_RECIPE_MAPS = ROOT / "tools" / "t13_denominators" / "recipe_maps.json"
SCHEMA = ROOT / "tools" / "recipe_load_projection.schema.json"
POLICY = ROOT / "tools" / "t14_load_budget_policy.json"

PENDING = "PENDING_MEASUREMENT"
POLICY_STATUSES = {
    "T14D_LOAD_BUDGET_POLICY_PENDING_T14C",
    "T14D_LOAD_BUDGET_POLICY_MEASURED",
}
STRATEGIES = ("immediate", "on_demand", "hybrid")
DELIVERY_PHASES = ("T15", "T16", "T17", "T18", "T19", "T21")
PROJECTION_PHASES = ("T14",) + DELIVERY_PHASES
COUNT_FIELDS = (
    "authored_entries",
    "logical_rows",
    "eager_publication_rows",
    "lazy_logical_rows",
    "lazy_cache_ceiling_rows",
    "sync_bytes",
)
PUBLICATION_DOMAINS = ("gt_recipe_family", "vanilla_crafting")
INTERVAL_FIELDS = (
    "server_reload_ms",
    "server_index_ms",
    "client_reload_ms",
    "client_index_ms",
    "retained_memory_bytes",
    "allocation_bytes",
    "lookup_p95_ns",
    "lookup_candidate_count",
)
SUM_INTERVAL_FIELDS = INTERVAL_FIELDS[:6]
MAX_INTERVAL_FIELDS = INTERVAL_FIELDS[6:]
BUDGET_CONTRACT = {
    "datapack_authored_entries": ("entries", "sum"),
    "eager_publication_rows": ("rows", "sum"),
    "lazy_logical_rows": ("rows", "sum"),
    "lazy_cache_ceiling_rows": ("rows", "sum"),
    "sync_bytes": ("bytes", "sum"),
    "server_reload_ms": ("ms", "sum_interval"),
    "server_index_ms": ("ms", "sum_interval"),
    "client_reload_ms": ("ms", "sum_interval"),
    "client_index_ms": ("ms", "sum_interval"),
    "retained_memory_bytes": ("bytes", "sum_interval"),
    "allocation_bytes": ("bytes", "sum_interval"),
    "lookup_p95_ns": ("ns", "max_interval"),
    "lookup_candidate_count": ("candidates", "max_interval"),
}


class ProjectionError(ValueError):
    """The projection cannot be proven from the declared family inputs."""


class BudgetExceededError(ProjectionError):
    """At least one projection metric exceeded a hard ceiling."""


def load(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def stable_json(value: Any) -> str:
    return json.dumps(
        value,
        ensure_ascii=False,
        indent=2,
        sort_keys=True,
    ) + "\n"


def canonical_digest(value: Any) -> str:
    encoded = json.dumps(
        value,
        ensure_ascii=False,
        sort_keys=True,
        separators=(",", ":"),
    ).encode("utf-8")
    return hashlib.sha256(encoded).hexdigest()


def _exact_fields(
    value: dict[str, Any],
    expected: Iterable[str],
    context: str,
) -> None:
    expected_set = set(expected)
    actual = set(value)
    if actual != expected_set:
        raise ProjectionError(
            f"{context}: fields differ; "
            f"missing={sorted(expected_set - actual)}, "
            f"extra={sorted(actual - expected_set)}"
        )


def _nonnegative_integer(value: Any, context: str) -> int:
    if isinstance(value, bool) or not isinstance(value, int) or value < 0:
        raise ProjectionError(f"{context}: expected a non-negative integer")
    return value


def _reject_ratio_inputs(value: Any, context: str = "input") -> None:
    if isinstance(value, dict):
        for key, child in value.items():
            normalized = key.lower()
            if normalized == "ratio" or normalized.endswith("_ratio"):
                raise ProjectionError(
                    f"{context}.{key}: ratio inputs are forbidden; "
                    "declare every family count explicitly"
                )
            _reject_ratio_inputs(child, f"{context}.{key}")
    elif isinstance(value, list):
        for index, child in enumerate(value):
            _reject_ratio_inputs(child, f"{context}[{index}]")


def _validate_interval(value: Any, context: str) -> dict[str, int]:
    if not isinstance(value, dict):
        raise ProjectionError(f"{context}: expected an interval object")
    _exact_fields(value, ("min", "max"), context)
    lower = _nonnegative_integer(value["min"], f"{context}.min")
    upper = _nonnegative_integer(value["max"], f"{context}.max")
    if lower > upper:
        raise ProjectionError(f"{context}: min exceeds max")
    return {"min": lower, "max": upper}


def _canonical_rows(t13: dict[str, Any]) -> dict[str, dict[str, Any]]:
    if (
        t13.get("schema_version") != 1
        or t13.get("status") != "T13A_RECIPE_MAP_DENOMINATOR_READY"
    ):
        raise ProjectionError("T13 recipe-map denominator is not canonical-ready")
    rows = t13.get("rows")
    if not isinstance(rows, list) or not rows:
        raise ProjectionError("T13 recipe-map denominator has no rows")
    result: dict[str, dict[str, Any]] = {}
    for row in rows:
        if not isinstance(row, dict):
            raise ProjectionError("T13 recipe-map row is not an object")
        canonical_id = row.get("normalized_row_key")
        if not isinstance(canonical_id, str) or not canonical_id:
            raise ProjectionError("T13 recipe-map row has no canonical id")
        if canonical_id in result:
            raise ProjectionError(f"duplicate T13 canonical id: {canonical_id}")
        result[canonical_id] = row
    counts = t13.get("counts", {})
    if counts.get("rows") != len(result):
        raise ProjectionError("T13 recipe-map canonical row count drifted")
    return result


def validate_schema(schema: dict[str, Any]) -> None:
    if (
        schema.get("$schema")
        != "https://json-schema.org/draft/2020-12/schema"
        or schema.get("title") != "CrucibleCraft family recipe load projection"
        or schema.get("type") != "object"
        or schema.get("additionalProperties") is not False
    ):
        raise ProjectionError("recipe load projection schema contract drifted")


def _validate_family(
    family: Any,
    index: int,
    canonical: dict[str, dict[str, Any]],
    seen_canonical_ids: set[str],
    schema_version: int,
) -> dict[str, Any]:
    context = f"families[{index}]"
    if not isinstance(family, dict):
        raise ProjectionError(f"{context}: expected an object")
    expected_fields = (
        "family",
        "canonical_ids",
        "strategy",
        *COUNT_FIELDS,
        "measurement_basis",
        "measurement_intervals",
    )
    if schema_version == 2:
        expected_fields += (
            "publication_domain",
            "vanilla_datapack_entries",
        )
    _exact_fields(family, expected_fields, context)
    family_id = family["family"]
    if (
        not isinstance(family_id, str)
        or not re.fullmatch(r"[a-z0-9][a-z0-9._/-]*", family_id)
    ):
        raise ProjectionError(f"{context}.family: invalid stable family id")

    canonical_ids = family["canonical_ids"]
    publication_domain = (
        family["publication_domain"]
        if schema_version == 2
        else "gt_recipe_family"
    )
    vanilla_entries = (
        _nonnegative_integer(
            family["vanilla_datapack_entries"],
            f"{context}.vanilla_datapack_entries",
        )
        if schema_version == 2
        else 0
    )
    if publication_domain not in PUBLICATION_DOMAINS:
        raise ProjectionError(
            f"{context}.publication_domain: unknown domain "
            f"{publication_domain!r}"
        )
    if (
        not isinstance(canonical_ids, list)
        or any(not isinstance(value, str) or not value for value in canonical_ids)
        or len(canonical_ids) != len(set(canonical_ids))
        or (
            publication_domain == "gt_recipe_family"
            and not canonical_ids
        )
        or (
            publication_domain == "vanilla_crafting"
            and canonical_ids
        )
    ):
        raise ProjectionError(
            f"{context}.canonical_ids: GT families require unique ids and "
            "vanilla crafting requires an empty list"
        )
    references = []
    for canonical_id in canonical_ids:
        row = canonical.get(canonical_id)
        if row is None:
            raise ProjectionError(
                f"{context}: unknown T13 canonical id {canonical_id!r}"
            )
        classification = row.get("classification")
        if classification != "in_scope":
            raise ProjectionError(
                f"{context}: T13 canonical id {canonical_id!r} is "
                f"{classification!r}, not in_scope"
            )
        if canonical_id in seen_canonical_ids:
            raise ProjectionError(
                f"{context}: canonical id {canonical_id!r} is projected twice"
            )
        seen_canonical_ids.add(canonical_id)
        references.append({
            "canonical_id": canonical_id,
            "classification": classification,
            "owner": row.get("owner"),
        })

    strategy = family["strategy"]
    if strategy not in STRATEGIES:
        raise ProjectionError(f"{context}.strategy: unknown strategy {strategy!r}")
    counts = {
        field: _nonnegative_integer(family[field], f"{context}.{field}")
        for field in COUNT_FIELDS
    }
    if publication_domain == "vanilla_crafting":
        if (
            vanilla_entries <= 0
            or counts["authored_entries"] != vanilla_entries
            or any(
                counts[field] != 0
                for field in (
                    "logical_rows",
                    "eager_publication_rows",
                    "lazy_logical_rows",
                    "lazy_cache_ceiling_rows",
                    "sync_bytes",
                )
            )
        ):
            raise ProjectionError(
                f"{context}: vanilla crafting entries must be counted as "
                "authored datapack entries with zero GT logical/publication "
                "and sync rows"
            )
    elif vanilla_entries != 0:
        raise ProjectionError(
            f"{context}: GT recipe families cannot declare vanilla entries"
        )
    logical = counts["logical_rows"]
    eager = counts["eager_publication_rows"]
    lazy = counts["lazy_logical_rows"]
    cache = counts["lazy_cache_ceiling_rows"]
    if eager + lazy != logical:
        raise ProjectionError(
            f"{context}: eager publication plus lazy rows must equal logical rows"
        )
    if cache > lazy:
        raise ProjectionError(
            f"{context}: lazy cache ceiling exceeds lazy logical rows"
        )
    if strategy == "immediate" and (eager != logical or lazy != 0 or cache != 0):
        raise ProjectionError(
            f"{context}: immediate requires all logical rows eager and no cache"
        )
    if strategy == "on_demand" and (eager != 0 or lazy != logical):
        raise ProjectionError(
            f"{context}: on_demand requires all logical rows lazy"
        )
    if (
        strategy == "hybrid"
        and logical > 0
        and (eager == 0 or lazy == 0)
    ):
        raise ProjectionError(
            f"{context}: non-empty hybrid requires eager and lazy partitions"
        )

    basis = family["measurement_basis"]
    if not isinstance(basis, dict):
        raise ProjectionError(f"{context}.measurement_basis: expected an object")
    _exact_fields(basis, ("kind", "source", "measured_logical_rows"), context)
    if basis["kind"] not in {
        "family_specific_measurement",
        "zero_workload_fixture",
    }:
        raise ProjectionError(f"{context}.measurement_basis: unknown kind")
    if not isinstance(basis["source"], str) or not basis["source"].strip():
        raise ProjectionError(
            f"{context}.measurement_basis.source: expected evidence text"
        )
    measured_rows = basis["measured_logical_rows"]
    if (
        not isinstance(measured_rows, list)
        or not measured_rows
        or any(
            isinstance(value, bool)
            or not isinstance(value, int)
            or value < 0
            for value in measured_rows
        )
        or measured_rows != sorted(set(measured_rows))
    ):
        raise ProjectionError(
            f"{context}.measurement_basis.measured_logical_rows: "
            "expected sorted unique non-negative scale points"
        )
    if logical not in measured_rows:
        raise ProjectionError(
            f"{context}: logical row count is not a measured family scale; "
            "extrapolation is forbidden"
        )

    measurements = family["measurement_intervals"]
    if not isinstance(measurements, dict):
        raise ProjectionError(
            f"{context}.measurement_intervals: expected an object"
        )
    _exact_fields(measurements, INTERVAL_FIELDS, context)
    validated_intervals = {
        field: _validate_interval(
            measurements[field],
            f"{context}.measurement_intervals.{field}",
        )
        for field in INTERVAL_FIELDS
    }
    if basis["kind"] == "zero_workload_fixture":
        if any(counts.values()) or any(
            interval["max"]
            for interval in validated_intervals.values()
        ):
            raise ProjectionError(
                f"{context}: zero-workload fixture cannot project recipes or cost"
            )

    return {
        **family,
        "t13_references": references,
        "measurement_intervals": validated_intervals,
        "publication_account": {
            "domain": publication_domain,
            "vanilla_datapack_entries": vanilla_entries,
            "gt_authored_entries": (
                counts["authored_entries"]
                if publication_domain == "gt_recipe_family"
                else 0
            ),
            "gt_logical_rows": counts["logical_rows"],
        },
        "ratios": _ratios(
            (
                counts["authored_entries"]
                if publication_domain == "gt_recipe_family"
                else 0
            ),
            counts["logical_rows"],
            counts["eager_publication_rows"],
        ),
    }


def validate_input(
    document: dict[str, Any],
    t13: dict[str, Any],
) -> list[dict[str, Any]]:
    if not isinstance(document, dict):
        raise ProjectionError("projection input must be an object")
    _reject_ratio_inputs(document)
    _exact_fields(
        document,
        (
            "schema_version",
            "status",
            "projection_id",
            "delivery_phase",
            "families",
        ),
        "input",
    )
    schema_version = document["schema_version"]
    if (
        schema_version not in {1, 2}
        or document["status"] != "FAMILY_LOAD_PROJECTION_INPUT"
    ):
        raise ProjectionError("projection input schema or status drifted")
    projection_id = document["projection_id"]
    if (
        not isinstance(projection_id, str)
        or not re.fullmatch(r"[a-z0-9][a-z0-9._/-]*", projection_id)
    ):
        raise ProjectionError("projection_id is not a stable lowercase id")
    if document["delivery_phase"] not in PROJECTION_PHASES:
        raise ProjectionError("delivery_phase must be T14 through T21")
    families = document["families"]
    if not isinstance(families, list) or not families:
        raise ProjectionError("projection input must declare at least one family")

    canonical = _canonical_rows(t13)
    seen_ids: set[str] = set()
    validated = [
        _validate_family(
            family,
            index,
            canonical,
            seen_ids,
            schema_version,
        )
        for index, family in enumerate(families)
    ]
    family_ids = [family["family"] for family in validated]
    if len(family_ids) != len(set(family_ids)):
        raise ProjectionError("projection family ids are duplicated")
    return validated


def validate_policy(policy: dict[str, Any]) -> None:
    if (
        policy.get("schema_version") != 1
        or policy.get("status") not in POLICY_STATUSES
        or policy.get("pending_token") != PENDING
        or policy.get("ratio_input_allowed") is not False
    ):
        raise ProjectionError("T14d load budget policy header drifted")
    budgets = policy.get("budgets")
    if not isinstance(budgets, dict) or set(budgets) != set(BUDGET_CONTRACT):
        raise ProjectionError("T14d budget metric set drifted")

    pending_metrics = []
    for metric, (unit, aggregation) in BUDGET_CONTRACT.items():
        spec = budgets[metric]
        if not isinstance(spec, dict):
            raise ProjectionError(f"budget {metric}: expected an object")
        _exact_fields(
            spec,
            (
                "unit",
                "aggregation",
                "soft_budget",
                "hard_ceiling",
                "source",
            ),
            f"budgets.{metric}",
        )
        if spec["unit"] != unit or spec["aggregation"] != aggregation:
            raise ProjectionError(f"budget {metric}: unit or aggregation drifted")
        if not isinstance(spec["source"], str) or not spec["source"].strip():
            raise ProjectionError(f"budget {metric}: source is missing")
        soft = spec["soft_budget"]
        hard = spec["hard_ceiling"]
        if soft == PENDING or hard == PENDING:
            if soft != PENDING or hard != PENDING:
                raise ProjectionError(
                    f"budget {metric}: soft/hard measurement must be filled together"
                )
            pending_metrics.append(metric)
            continue
        if (
            isinstance(soft, bool)
            or isinstance(hard, bool)
            or not isinstance(soft, int)
            or not isinstance(hard, int)
            or soft <= 0
            or hard <= 0
            or soft > hard
        ):
            raise ProjectionError(
                f"budget {metric}: expected positive soft <= hard ceilings"
            )
    declared_pending = policy.get("pending_measurements")
    if declared_pending != sorted(pending_metrics):
        raise ProjectionError("T14d pending measurement list is stale")
    if (
        policy["status"] == "T14D_LOAD_BUDGET_POLICY_MEASURED"
        and pending_metrics
    ):
        raise ProjectionError("measured T14d policy still has pending metrics")
    if (
        policy["status"] == "T14D_LOAD_BUDGET_POLICY_PENDING_T14C"
        and not pending_metrics
    ):
        raise ProjectionError("pending T14d policy has no pending metrics")


def _ratio(numerator: int, denominator: int, formula: str) -> dict[str, Any]:
    return {
        "formula": formula,
        "numerator": numerator,
        "denominator": denominator,
        "value": (
            numerator / denominator
            if denominator
            else None
        ),
        "status": "MEASURED_FROM_EXPLICIT_COUNTS" if denominator else "NOT_APPLICABLE",
    }


def _ratios(authored: int, logical: int, eager: int) -> dict[str, Any]:
    return {
        "authored_to_logical": _ratio(
            logical,
            authored,
            "logical_rows / authored_entries",
        ),
        "authored_to_eager": _ratio(
            eager,
            authored,
            "eager_publication_rows / authored_entries",
        ),
    }


def _sum_intervals(
    families: list[dict[str, Any]],
    field: str,
) -> dict[str, int]:
    return {
        "min": sum(
            family["measurement_intervals"][field]["min"]
            for family in families
        ),
        "max": sum(
            family["measurement_intervals"][field]["max"]
            for family in families
        ),
    }


def _max_intervals(
    families: list[dict[str, Any]],
    field: str,
) -> dict[str, int]:
    return {
        "min": max(
            family["measurement_intervals"][field]["min"]
            for family in families
        ),
        "max": max(
            family["measurement_intervals"][field]["max"]
            for family in families
        ),
    }


def build_ledger(families: list[dict[str, Any]]) -> dict[str, Any]:
    publication_accounts = [
        family.get("publication_account") or {
            "domain": "gt_recipe_family",
            "vanilla_datapack_entries": 0,
            "gt_authored_entries": family["authored_entries"],
            "gt_logical_rows": family["logical_rows"],
        }
        for family in families
    ]
    counts = {
        "datapack_authored_entries": sum(
            family["authored_entries"] for family in families
        ),
        "logical_rows": sum(family["logical_rows"] for family in families),
        "eager_publication_rows": sum(
            family["eager_publication_rows"] for family in families
        ),
        "lazy_logical_rows": sum(
            family["lazy_logical_rows"] for family in families
        ),
        "lazy_cache_ceiling_rows": sum(
            family["lazy_cache_ceiling_rows"] for family in families
        ),
        "sync_bytes": sum(family["sync_bytes"] for family in families),
    }
    domain_counts = {
        "vanilla_datapack_entries": sum(
            account["vanilla_datapack_entries"]
            for account in publication_accounts
        ),
        "gt_authored_entries": sum(
            account["gt_authored_entries"]
            for account in publication_accounts
        ),
        "gt_logical_rows": sum(
            account["gt_logical_rows"]
            for account in publication_accounts
        ),
    }
    measurements = {
        field: _sum_intervals(families, field)
        for field in SUM_INTERVAL_FIELDS
    }
    measurements.update({
        field: _max_intervals(families, field)
        for field in MAX_INTERVAL_FIELDS
    })
    return {
        "counts": counts,
        "publication_domains": domain_counts,
        "ratios": _ratios(
            counts["datapack_authored_entries"],
            counts["logical_rows"],
            counts["eager_publication_rows"],
        ),
        "measurement_intervals": measurements,
        "aggregation": {
            "counts": "sum of explicit family counts",
            "reload_index_memory": "sum of family interval bounds",
            "lookup": "maximum family interval bounds; one lookup is not additive",
        },
    }


def _budget_actual(ledger: dict[str, Any], metric: str) -> int | dict[str, int]:
    counts = ledger["counts"]
    if metric in {
        "datapack_authored_entries",
        "eager_publication_rows",
        "lazy_logical_rows",
        "lazy_cache_ceiling_rows",
        "sync_bytes",
    }:
        return counts[metric]
    return ledger["measurement_intervals"][metric]


def evaluate_budgets(
    ledger: dict[str, Any],
    policy: dict[str, Any],
) -> dict[str, Any]:
    metrics: dict[str, Any] = {}
    hard_failures: list[str] = []
    soft_exceeded: list[str] = []
    pending: list[str] = []
    for metric in BUDGET_CONTRACT:
        spec = policy["budgets"][metric]
        actual = _budget_actual(ledger, metric)
        comparison_value = actual["max"] if isinstance(actual, dict) else actual
        soft = spec["soft_budget"]
        hard = spec["hard_ceiling"]
        if soft == PENDING:
            status = PENDING
            pending.append(metric)
        elif comparison_value > hard:
            status = "HARD_CEILING_EXCEEDED"
            hard_failures.append(
                f"{metric}:{comparison_value}>{hard}"
            )
        elif comparison_value > soft:
            status = "SOFT_BUDGET_EXCEEDED"
            soft_exceeded.append(metric)
        else:
            status = "PASS"
        metrics[metric] = {
            "actual": actual,
            "comparison_value": comparison_value,
            "soft_budget": soft,
            "hard_ceiling": hard,
            "status": status,
        }
    if hard_failures:
        raise BudgetExceededError(
            "hard load budget exceeded: " + ", ".join(hard_failures)
        )
    status = (
        "BLOCKED_PENDING_MEASUREMENT"
        if pending
        else "SOFT_BUDGET_EXCEEDED"
        if soft_exceeded
        else "PASS"
    )
    return {
        "status": status,
        "metrics": metrics,
        "soft_exceeded": soft_exceeded,
        "pending_measurements": pending,
        "hard_failures": [],
    }


def project(
    document: dict[str, Any],
    *,
    t13: dict[str, Any] | None = None,
    policy: dict[str, Any] | None = None,
    schema: dict[str, Any] | None = None,
) -> dict[str, Any]:
    t13 = load(T13_RECIPE_MAPS) if t13 is None else t13
    policy = load(POLICY) if policy is None else policy
    schema = load(SCHEMA) if schema is None else schema
    validate_schema(schema)
    validate_policy(policy)
    families = validate_input(document, t13)
    ledger = build_ledger(families)
    budget_evaluation = evaluate_budgets(ledger, policy)
    return {
        "schema_version": document["schema_version"],
        "status": budget_evaluation["status"],
        "projection_id": document["projection_id"],
        "delivery_phase": document["delivery_phase"],
        "families": families,
        "ledger": ledger,
        "budget_evaluation": budget_evaluation,
        "currentness": {
            "t13_recipe_maps": T13_RECIPE_MAPS.relative_to(ROOT).as_posix(),
            "t13_map_row_sha256": t13.get("map_row_sha256"),
            "t13_canonical_rows": t13.get("counts", {}).get("rows"),
            "projection_schema": SCHEMA.relative_to(ROOT).as_posix(),
            "projection_schema_sha256": canonical_digest(schema),
            "budget_policy": POLICY.relative_to(ROOT).as_posix(),
            "budget_policy_sha256": canonical_digest(policy),
        },
    }


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("input", type=Path, help="family projection input JSON")
    parser.add_argument(
        "--output",
        type=Path,
        help="write the derived projection instead of printing it",
    )
    args = parser.parse_args(argv)
    try:
        result = project(load(args.input))
    except (OSError, json.JSONDecodeError, ProjectionError) as failure:
        print(f"Recipe load projection failed: {failure}", file=sys.stderr)
        return 1
    encoded = stable_json(result)
    if args.output is None:
        print(encoded, end="")
    else:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(encoded, encoding="utf-8", newline="\n")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
