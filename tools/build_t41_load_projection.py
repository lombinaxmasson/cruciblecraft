#!/usr/bin/env python3
"""Build the T41 Assembler family load projection input and output."""
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
from tools import t41_common as t41  # noqa: E402

BUILDER = Path(__file__).resolve()
INPUT_OUTPUT = t41.LOAD_PROJECTION_INPUT
PROJECTION_OUTPUT = t41.LOAD_PROJECTION


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
    t13 = projection.load(projection.T13_RECIPE_MAPS)
    patched = copy.deepcopy(t13)
    for row in patched.get("rows") or []:
        if row.get("normalized_row_key") == t41.SOURCE_MAP:
            if row.get("classification") == "in_scope":
                return t13
            row["classification"] = "in_scope"
            return patched
    raise ValueError(f"T13 recipe maps are missing {t41.SOURCE_MAP}")


def _card_candidate(measurements: dict[str, Any], winner: str) -> dict[str, Any]:
    for scenario in measurements.get("scenarios") or []:
        if (
            scenario.get("id") != "card"
            or scenario.get("logical_rows") != t41.production_relation_count()
        ):
            continue
        for candidate in scenario.get("candidates") or []:
            if candidate.get("candidate") == winner:
                return candidate
    raise ValueError(
        f"tools/t41_materialization_measurements.json is missing the {winner} "
        f"1x={t41.production_relation_count()} card candidate"
    )


def _blocked_input(reason: str) -> dict[str, Any]:
    return _bind_production_lock({
        "schema_version": 2,
        "status": "FAMILY_LOAD_PROJECTION_INPUT_BLOCKED",
        "projection_id": "t41/assembler-wave",
        "delivery_phase": "T41",
        "blockers": [reason],
        "families": [],
    })


def _bind_production_lock(document: dict[str, Any]) -> dict[str, Any]:
    bound = dict(document)
    bound["production_lock_sha256"] = t41.production_lock_sha256()
    counts = (bound.get("ledger") or {}).get("counts") or {}
    bound["family_count"] = counts.get("datapack_authored_entries")
    bound["logical"] = counts.get("logical_rows")
    return bound


def _blocked_projection(reason: str) -> dict[str, Any]:
    return _bind_production_lock({
        "schema_version": 2,
        "status": "T41_LOAD_PROJECTION_BLOCKED",
        "projection_id": "t41/assembler-wave",
        "delivery_phase": "T41",
        "blockers": [reason],
        "ledger": {
            "counts": {
                "datapack_authored_entries": None,
                "logical_rows": None,
                "eager_publication_rows": None,
                "lazy_logical_rows": None,
                "lazy_cache_ceiling_rows": None,
            }
        },
        "budget_evaluation": {
            "status": "BLOCKED_PENDING_MEASUREMENT",
            "blockers": [reason],
        },
    })


def build_input() -> dict[str, Any]:
    strategy = t41.production_strategy()
    if strategy["blocked"]:
        return _blocked_input(
            "tools/t41_materialization_decision.json production winner is BLOCKED"
        )
    if not t41.MEASUREMENTS.is_file():
        return _blocked_input("tools/t41_materialization_measurements.json is missing")
    group_partitions = {
        group: t41.partition_for_winner(winner, group)
        for group, winner in strategy["group_winners"].items()
    }
    partition = {
        key: sum(int(row[key] or 0) for row in group_partitions.values())
        for key in (
            "eager_publication_rows",
            "lazy_logical_rows",
            "lazy_cache_ceiling_rows",
        )
    }
    card_winner = strategy["card_aggregate_winner"]
    measured_card_partition = t41.partition_for_winner(card_winner, "card")
    if partition != measured_card_partition:
        return _blocked_input(
            "T41 mixed group partition has no equivalent measured card scenario"
        )
    candidate = _card_candidate(t35.load_json(t41.MEASUREMENTS), card_winner)
    server = candidate["server"]
    client = candidate["dedicated_client"]
    lookup = candidate["lookup"]
    allocation = candidate["allocation"]
    sync = candidate["sync"]
    sync_bytes = int(sync["bytes"])
    return {
        "schema_version": 2,
        "status": "FAMILY_LOAD_PROJECTION_INPUT",
        "projection_id": "t41/assembler-wave",
        "delivery_phase": "T41",
        "families": [
            {
                "family": "t41/assembler-wave",
                "canonical_ids": [t41.SOURCE_MAP],
                "strategy": "group_scoped",
                "publication_domain": "gt_recipe_family",
                "vanilla_datapack_entries": 0,
                "authored_entries": t41.production_family_count(),
                "logical_rows": t41.production_relation_count(),
                "eager_publication_rows": int(partition["eager_publication_rows"] or 0),
                "lazy_logical_rows": int(partition["lazy_logical_rows"] or 0),
                "lazy_cache_ceiling_rows": int(partition["lazy_cache_ceiling_rows"] or 0),
                "sync_bytes": sync_bytes,
                "measurement_basis": {
                    "kind": "family_specific_measurement",
                    "source": (
                        "T41 R4 production-lock JUnit "
                        "CompactRecipeFamilyT41MeasurementHarness. "
                        "Production partitions are summed from the independently "
                        "derived planks/fireproof/planks2 winners. Their aggregate is exactly "
                        f"equivalent to the measured {card_winner} card candidate. "
                        "Intervals are p50-p95; ns timings convert with ceil-to-ms. "
                        "Card-level cache is not copied into per-family rows."
                    ),
                    "measured_logical_rows": sorted({
                        t41.production_group_counts()[t41.PLANKS_GROUP]["relations"],
                        t41.production_group_counts()[t41.FIREPROOF_GROUP]["relations"],
                        t41.production_group_counts()[t41.PLANKS2_GROUP]["relations"],
                        t41.production_relation_count(),
                    }),
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
    input_document = build_input()
    if input_document.get("status") == "FAMILY_LOAD_PROJECTION_INPUT_BLOCKED":
        return _blocked_projection(str(input_document.get("blockers") or ["blocked"]))
    return _bind_production_lock(
        projection.project(input_document, t13=_projection_t13())
    )


def write() -> dict[str, Any]:
    input_document = build_input()
    t35.write_stable(INPUT_OUTPUT, input_document)
    if input_document.get("status") == "FAMILY_LOAD_PROJECTION_INPUT_BLOCKED":
        output_document = _blocked_projection(str(input_document.get("blockers") or ["blocked"]))
    else:
        output_document = _bind_production_lock(
            projection.project(input_document, t13=_projection_t13())
        )
    t35.write_stable(PROJECTION_OUTPUT, output_document)
    return output_document


def check() -> list[str]:
    errors: list[str] = []
    errors.extend(t41.check_document(INPUT_OUTPUT, build_input()))
    errors.extend(t41.check_document(PROJECTION_OUTPUT, build()))
    if errors:
        return errors
    output = t35.load_json(PROJECTION_OUTPUT)
    if output.get("status") == "T41_LOAD_PROJECTION_BLOCKED":
        return errors
    status = ((output.get("budget_evaluation") or {}).get("status") or "")
    if status == "HARD_CEILING_EXCEEDED":
        errors.append("T41 load projection must not HARD_CEILING_EXCEEDED")
    return errors


def main(argv: list[str] | None = None) -> int:
    args = t41.parse_write_check(__doc__, argv)
    try:
        if args.write:
            document = write()
            counts = (document.get("ledger") or {}).get("counts") or {}
            budget_status = (document.get("budget_evaluation") or {}).get("status")
            print(json.dumps({
                "status": document["status"],
                "delivery_phase": document["delivery_phase"],
                "authored": counts.get("datapack_authored_entries"),
                "logical": counts.get("logical_rows"),
                "eager": counts.get("eager_publication_rows"),
                "lazy": counts.get("lazy_logical_rows"),
                "cache": counts.get("lazy_cache_ceiling_rows"),
                "budget_evaluation": budget_status,
            }))
            return 0
        errors = check()
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        print(
            f"{t41.relative(INPUT_OUTPUT)} and "
            f"{t41.relative(PROJECTION_OUTPUT)} are current"
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
        print(f"T41 load projection failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
