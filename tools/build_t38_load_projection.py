#!/usr/bin/env python3
"""Build the T38 Roaster family load projection input and output."""
from __future__ import annotations

import copy
import json
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from tools import recipe_load_projection as projection  # noqa: E402
from tools import t35_common as t35  # noqa: E402
from tools import t38_common as t38  # noqa: E402

BUILDER = Path(__file__).resolve()
INPUT_OUTPUT = t38.LOAD_PROJECTION_INPUT
PROJECTION_OUTPUT = t38.LOAD_PROJECTION


def _ceil_ns_to_ms(value_ns: int) -> int:
    return (int(value_ns) + 999_999) // 1_000_000


def _interval(min_value: int, max_value: int) -> dict[str, int]:
    return {"min": int(min_value), "max": int(max_value)}


def _timing_interval(metric: dict[str, Any]) -> dict[str, int]:
    return _interval(
        _ceil_ns_to_ms(metric["p50_ns"]),
        _ceil_ns_to_ms(metric["p95_ns"]),
    )


def _projection_t13() -> dict[str, Any]:
    """T13 still defers gt.recipe.roaster; T36/T38 reissue it as in_scope_1x."""
    t13 = projection.load(projection.T13_RECIPE_MAPS)
    patched = copy.deepcopy(t13)
    for row in patched.get("rows") or []:
        if row.get("normalized_row_key") == t38.SOURCE_MAP:
            if row.get("classification") == "in_scope":
                return t13
            row["classification"] = "in_scope"
            return patched
    raise ValueError(f"T13 recipe maps are missing {t38.SOURCE_MAP}")


def _on_demand_candidate(measurements: dict[str, Any]) -> dict[str, Any]:
    for scenario in measurements.get("scenarios") or []:
        if scenario.get("logical_rows") != t38.SOURCE_ROWS:
            continue
        for candidate in scenario.get("candidates") or []:
            if candidate.get("candidate") == "on_demand":
                return candidate
    raise ValueError(
        "tools/t38_materialization_measurements.json is missing the on_demand 1x=73 candidate"
    )


def build_input() -> dict[str, Any]:
    strategy = t38.production_strategy()
    if strategy["blocked"]:
        raise ValueError("T38 production winner is blocked; load projection input is unavailable")
    if strategy["winner"] != "on_demand":
        raise ValueError(
            f"T38 load projection expects on_demand winner, got {strategy['winner']!r}"
        )
    partition = t38.partition_for_winner(strategy["winner"])
    candidate = _on_demand_candidate(t35.load_json(t38.MEASUREMENTS))
    server = candidate["server"]
    client = candidate["dedicated_client"]
    lookup = candidate["lookup"]
    allocation = candidate["allocation"]
    sync = candidate["sync"]
    retained = candidate["retained_memory"]
    sync_bytes = int(sync["bytes"])
    return {
        "schema_version": 2,
        "status": "FAMILY_LOAD_PROJECTION_INPUT",
        "projection_id": "t38/roaster-wave",
        "delivery_phase": "T38",
        "families": [
            {
                "family": "t38/roaster-wave",
                "canonical_ids": [t38.SOURCE_MAP],
                "strategy": "on_demand",
                "publication_domain": "gt_recipe_family",
                "vanilla_datapack_entries": 0,
                "authored_entries": t38.FAMILY_COUNT,
                "logical_rows": t38.SOURCE_ROWS,
                "eager_publication_rows": int(partition["eager_publication_rows"] or 0),
                "lazy_logical_rows": int(partition["lazy_logical_rows"] or 0),
                "lazy_cache_ceiling_rows": int(partition["lazy_cache_ceiling_rows"] or 0),
                "sync_bytes": sync_bytes,
                "measurement_basis": {
                    "kind": "family_specific_measurement",
                    "source": (
                        "T38 R4 1x=73 JUnit CompactRecipeFamilyT38MeasurementHarness. "
                        "Production winner on_demand derived by "
                        "tools/build_t38_recipe_load_benchmark.py "
                        "(0 eager / 73 lazy / cache 16). Intervals are p50–p95 of the "
                        "current on_demand candidate; ns timings convert with ceil-to-ms. "
                        "Card-level cache 16 is not copied into per-family rows."
                    ),
                    "measured_logical_rows": [t38.SOURCE_ROWS],
                },
                "measurement_intervals": {
                    "server_reload_ms": _timing_interval(server["reload"]),
                    "server_index_ms": _timing_interval(server["index"]),
                    "client_reload_ms": _timing_interval(client["reload"]),
                    "client_index_ms": _timing_interval(client["index"]),
                    "retained_memory_bytes": _interval(sync_bytes, sync_bytes),
                    "allocation_bytes": _interval(
                        allocation["p50_bytes"],
                        allocation["p95_bytes"],
                    ),
                    "lookup_p95_ns": _interval(lookup["p50_ns"], lookup["p95_ns"]),
                    "lookup_candidate_count": _interval(
                        lookup["candidates_p95"],
                        lookup["candidates_p95"],
                    ),
                },
            }
        ],
    }


def build() -> dict[str, Any]:
    return projection.project(build_input(), t13=_projection_t13())


def write() -> dict[str, Any]:
    input_document = build_input()
    t35.write_stable(INPUT_OUTPUT, input_document)
    output_document = projection.project(input_document, t13=_projection_t13())
    t35.write_stable(PROJECTION_OUTPUT, output_document)
    return output_document


def check() -> list[str]:
    errors: list[str] = []
    errors.extend(t38.check_document(INPUT_OUTPUT, build_input()))
    errors.extend(t38.check_document(PROJECTION_OUTPUT, build()))
    if errors:
        return errors
    output = t35.load_json(PROJECTION_OUTPUT)
    status = ((output.get("budget_evaluation") or {}).get("status") or "")
    if status == "HARD_CEILING_EXCEEDED":
        errors.append("T38 load projection must not HARD_CEILING_EXCEEDED")
    return errors


def main(argv: list[str] | None = None) -> int:
    args = t38.parse_write_check(__doc__, argv)
    try:
        if args.write:
            document = write()
            counts = document["ledger"]["counts"]
            budget_status = document["budget_evaluation"]["status"]
            print(json.dumps({
                "status": document["status"],
                "delivery_phase": document["delivery_phase"],
                "authored": counts["datapack_authored_entries"],
                "logical": counts["logical_rows"],
                "eager": counts["eager_publication_rows"],
                "lazy": counts["lazy_logical_rows"],
                "cache": counts["lazy_cache_ceiling_rows"],
                "budget_evaluation": budget_status,
            }))
            return 0
        errors = check()
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        print(
            f"{t38.relative(INPUT_OUTPUT)} and "
            f"{t38.relative(PROJECTION_OUTPUT)} are current"
        )
        return 0
    except (
        OSError,
        ValueError,
        KeyError,
        json.JSONDecodeError,
        projection.ProjectionError,
        projection.BudgetExceededError,
    ) as error:
        print(f"T38 load projection failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
