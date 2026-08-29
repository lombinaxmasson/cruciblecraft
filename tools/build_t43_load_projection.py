#!/usr/bin/env python3
"""Build the T43 Smelter stone family load projection input and output."""
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
from tools import t43_common as t43  # noqa: E402

BUILDER = Path(__file__).resolve()
INPUT_OUTPUT = t43.LOAD_PROJECTION_INPUT
PROJECTION_OUTPUT = t43.LOAD_PROJECTION
PROJECTION_ID = "t43/smelter-stone-wave"


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
        if row.get("normalized_row_key") == t43.SOURCE_MAP:
            if row.get("classification") == "in_scope":
                return t13
            row["classification"] = "in_scope"
            return patched
    raise ValueError(f"T13 recipe maps are missing {t43.SOURCE_MAP}")


def _card_candidate(measurements: dict[str, Any], winner: str) -> dict[str, Any]:
    for scenario in measurements.get("scenarios") or []:
        if (
            scenario.get("id") != "card"
            or scenario.get("logical_rows") != t43.production_relation_count()
        ):
            continue
        for candidate in scenario.get("candidates") or []:
            if candidate.get("candidate") == winner:
                return candidate
    raise ValueError(
        f"tools/t43_materialization_measurements.json is missing the {winner} "
        f"1x={t43.production_relation_count()} card candidate"
    )


def _bind_production_lock(document: dict[str, Any]) -> dict[str, Any]:
    bound = dict(document)
    bound["production_lock_sha256"] = t43.production_lock_sha256()
    counts = (bound.get("ledger") or {}).get("counts") or {}
    bound["family_count"] = counts.get("datapack_authored_entries")
    bound["logical"] = counts.get("logical_rows")
    return bound


def _blocked_input(reason: str) -> dict[str, Any]:
    return _bind_production_lock({
        "schema_version": 2,
        "status": "FAMILY_LOAD_PROJECTION_INPUT_BLOCKED",
        "projection_id": PROJECTION_ID,
        "delivery_phase": "T43",
        "blockers": [reason],
        "families": [],
    })


def _blocked_projection(reason: str) -> dict[str, Any]:
    return _bind_production_lock({
        "schema_version": 2,
        "status": "T43_LOAD_PROJECTION_BLOCKED",
        "projection_id": PROJECTION_ID,
        "delivery_phase": "T43",
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
    strategy = t43.production_strategy()
    if strategy["blocked"]:
        return _blocked_input(
            "tools/t43_materialization_decision.json production winner is BLOCKED"
        )
    if not t43.MEASUREMENTS.is_file():
        return _blocked_input("tools/t43_materialization_measurements.json is missing")
    stone_partition = t43.partition_for_winner(
        strategy["group_winners"].get("stone"),
        "stone",
    )
    card_winner = strategy["card_aggregate_winner"]
    measured_card_partition = t43.partition_for_winner(card_winner, "card")
    if stone_partition != measured_card_partition:
        return _blocked_input(
            "T43 stone partition has no equivalent measured card scenario"
        )
    candidate = _card_candidate(t35.load_json(t43.MEASUREMENTS), card_winner)
    server = candidate["server"]
    client = candidate["dedicated_client"]
    lookup = candidate["lookup"]
    allocation = candidate["allocation"]
    sync = candidate["sync"]
    sync_bytes = int(sync["bytes"])
    publication = (
        t35.load_json(t43.PUBLICATION_DELTA)
        if t43.PUBLICATION_DELTA.is_file()
        else {}
    )
    authored = int(
        publication.get("authored_datapack_entries")
        or t43.production_family_count()
    )
    return {
        "schema_version": 2,
        "status": "FAMILY_LOAD_PROJECTION_INPUT",
        "projection_id": PROJECTION_ID,
        "delivery_phase": "T43",
        "families": [
            {
                "family": PROJECTION_ID,
                "canonical_ids": [t43.SOURCE_MAP],
                "strategy": "group_scoped",
                "publication_domain": "gt_recipe_family",
                "vanilla_datapack_entries": 0,
                "authored_entries": authored,
                "logical_rows": t43.production_relation_count(),
                "eager_publication_rows": int(stone_partition["eager_publication_rows"] or 0),
                "lazy_logical_rows": int(stone_partition["lazy_logical_rows"] or 0),
                "lazy_cache_ceiling_rows": int(
                    stone_partition["lazy_cache_ceiling_rows"] or 0
                ),
                "sync_bytes": sync_bytes,
                "measurement_basis": {
                    "kind": "family_specific_measurement",
                    "source": (
                        "T43 R3 production-lock JUnit "
                        "CompactRecipeFamilyT43MeasurementHarness. "
                        "The stone group partition equals the measured card candidate. "
                        "Intervals are p50-p95; ns timings convert with ceil-to-ms. "
                        "Card-level cache is not copied into per-family rows."
                    ),
                    "measured_logical_rows": [
                        t43.production_relation_count(),
                    ],
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
        output_document = _blocked_projection(
            str(input_document.get("blockers") or ["blocked"])
        )
    else:
        output_document = _bind_production_lock(
            projection.project(input_document, t13=_projection_t13())
        )
    t35.write_stable(PROJECTION_OUTPUT, output_document)
    return output_document


def check() -> list[str]:
    errors: list[str] = []
    errors.extend(t43.check_document(INPUT_OUTPUT, build_input()))
    errors.extend(t43.check_document(PROJECTION_OUTPUT, build()))
    if errors:
        return errors
    output = t35.load_json(PROJECTION_OUTPUT)
    if output.get("status") == "T43_LOAD_PROJECTION_BLOCKED":
        return errors
    status = ((output.get("budget_evaluation") or {}).get("status") or "")
    if status == "HARD_CEILING_EXCEEDED":
        errors.append("T43 load projection must not HARD_CEILING_EXCEEDED")
    return errors


def main(argv: list[str] | None = None) -> int:
    args = t43.parse_write_check(__doc__, argv)
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
            f"{t43.relative(INPUT_OUTPUT)} and "
            f"{t43.relative(PROJECTION_OUTPUT)} are current"
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
        print(f"T43 load projection failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
