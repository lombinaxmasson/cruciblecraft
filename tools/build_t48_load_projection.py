#!/usr/bin/env python3
"""Build the T48 Bath remainder family load projection input and output."""
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
from tools import t48_common as t48  # noqa: E402

INPUT_OUTPUT = t48.LOAD_PROJECTION_INPUT
PROJECTION_OUTPUT = t48.LOAD_PROJECTION
PROJECTION_ID = "t48/bath-remainder-wave"
SOURCE_MAPS = ("gt.recipe.bath",)


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
    missing = set(SOURCE_MAPS)
    for row in patched.get("rows") or []:
        key = row.get("normalized_row_key")
        if key not in missing:
            continue
        missing.discard(key)
        if row.get("classification") != "in_scope":
            row["classification"] = "in_scope"
    if missing:
        raise ValueError(f"T13 recipe maps are missing {sorted(missing)}")
    return patched


def _card_candidate(measurements: dict[str, Any], winner: str) -> dict[str, Any]:
    for scenario in measurements.get("scenarios") or []:
        if (
            scenario.get("id") != "card"
            or scenario.get("logical_rows") != t48.production_relation_count()
        ):
            continue
        for candidate in scenario.get("candidates") or []:
            if candidate.get("candidate") == winner:
                return candidate
    raise ValueError(
        f"tools/t48_materialization_measurements.json is missing the {winner} "
        f"1x={t48.production_relation_count()} card candidate"
    )


def _bind_production_lock(document: dict[str, Any]) -> dict[str, Any]:
    bound = dict(document)
    bound["production_lock_sha256"] = t48.production_lock_sha256()
    counts = (bound.get("ledger") or {}).get("counts") or {}
    bound["family_count"] = counts.get("datapack_authored_entries")
    bound["logical"] = counts.get("logical_rows")
    return bound


def _blocked_input(reason: str) -> dict[str, Any]:
    return _bind_production_lock({
        "schema_version": 2,
        "status": "FAMILY_LOAD_PROJECTION_INPUT_BLOCKED",
        "projection_id": PROJECTION_ID,
        "delivery_phase": "T48",
        "blockers": [reason],
        "families": [],
    })


def _blocked_projection(reason: str) -> dict[str, Any]:
    return _bind_production_lock({
        "schema_version": 2,
        "status": "T48_LOAD_PROJECTION_BLOCKED",
        "projection_id": PROJECTION_ID,
        "delivery_phase": "T48",
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
    strategy = t48.production_strategy()
    if strategy["blocked"]:
        return _blocked_input(
            "tools/t48_materialization_decision.json production winner is BLOCKED"
        )
    if not t48.MEASUREMENTS.is_file():
        return _blocked_input("tools/t48_materialization_measurements.json is missing")
    card_winner = strategy["card_aggregate_winner"]
    card_partition = t48.partition_for_winner(card_winner, "card_aggregate")
    if any(value is None for value in card_partition.values()):
        return _blocked_input("T48 card partition is not measured")
    if not t48.integrated_load_measured():
        return _blocked_input("tools/t48_integrated_measurements.json is missing")
    measurements = t35.load_json(t48.MEASUREMENTS)
    candidate = _card_candidate(measurements, card_winner)
    server = candidate["server"]
    client = candidate["dedicated_client"]
    lookup = candidate["lookup"]
    allocation = candidate["allocation"]
    sync = candidate["sync"]
    sync_bytes = int(sync["bytes"])
    publication = (
        t35.load_json(t48.PUBLICATION_DELTA)
        if t48.PUBLICATION_DELTA.is_file()
        else {}
    )
    authored = int(
        publication.get("authored_datapack_entries")
        or t48.production_family_count()
    )
    return {
        "schema_version": 2,
        "status": "FAMILY_LOAD_PROJECTION_INPUT",
        "projection_id": PROJECTION_ID,
        "delivery_phase": "T48",
        "families": [
            {
                "family": PROJECTION_ID,
                "canonical_ids": list(SOURCE_MAPS),
                "strategy": "group_scoped",
                "publication_domain": "gt_recipe_family",
                "vanilla_datapack_entries": 0,
                "authored_entries": authored,
                "logical_rows": t48.production_relation_count(),
                "eager_publication_rows": int(card_partition["eager_publication_rows"] or 0),
                "lazy_logical_rows": int(card_partition["lazy_logical_rows"] or 0),
                "lazy_cache_ceiling_rows": int(
                    card_partition["lazy_cache_ceiling_rows"] or 0
                ),
                "sync_bytes": sync_bytes,
                "measurement_basis": {
                    "kind": "family_specific_measurement",
                    "source": (
                        "T48 R3 card 1x CompactRecipeFamilyT48MeasurementHarness "
                        "on_demand winner. Card partition is the measured 34091-row "
                        "T48 groups. The 18-group integrated mix is census T14 closing "
                        "only; its allocation exceeds the frozen 512 MiB hard ceiling "
                        "and T48 does not raise that gate."
                    ),
                    "measured_logical_rows": [
                        t48.production_relation_count(),
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


def _project(input_document: dict[str, Any]) -> dict[str, Any]:
    policy = t35.load_json(t48.LOAD_POLICY_V2)
    output = projection.project(
        input_document,
        t13=_projection_t13(),
        policy=policy,
    )
    currentness = dict(output.get("currentness") or {})
    currentness["budget_policy"] = t48.relative(t48.LOAD_POLICY_V2)
    currentness["budget_policy_sha256"] = projection.canonical_digest(policy)
    currentness["budget_policy_file_sha256"] = t48.v2_policy_hash()
    currentness["load_budget_policy_v2_sha256"] = t48.v2_policy_hash()
    currentness["integrated_measurements"] = t48.relative(t48.INTEGRATED_MEASUREMENTS)
    output["currentness"] = currentness
    return output


def build() -> dict[str, Any]:
    input_document = build_input()
    if input_document.get("status") == "FAMILY_LOAD_PROJECTION_INPUT_BLOCKED":
        return _blocked_projection(str(input_document.get("blockers") or ["blocked"]))
    return _bind_production_lock(_project(input_document))


def write() -> dict[str, Any]:
    input_document = build_input()
    t35.write_stable(INPUT_OUTPUT, input_document)
    if input_document.get("status") == "FAMILY_LOAD_PROJECTION_INPUT_BLOCKED":
        output_document = _blocked_projection(
            str(input_document.get("blockers") or ["blocked"])
        )
    else:
        output_document = _bind_production_lock(_project(input_document))
    t35.write_stable(PROJECTION_OUTPUT, output_document)
    return output_document


def check() -> list[str]:
    errors: list[str] = []
    errors.extend(t48.check_document(INPUT_OUTPUT, build_input()))
    errors.extend(t48.check_document(PROJECTION_OUTPUT, build()))
    if errors:
        return errors
    output = t35.load_json(PROJECTION_OUTPUT)
    if output.get("status") == "T48_LOAD_PROJECTION_BLOCKED":
        return errors
    status = ((output.get("budget_evaluation") or {}).get("status") or "")
    if status == "HARD_CEILING_EXCEEDED":
        errors.append("T48 load projection must not HARD_CEILING_EXCEEDED")
    currentness = output.get("currentness") or {}
    if currentness.get("budget_policy_file_sha256") != t48.v2_policy_hash():
        errors.append("T48 load projection must pin t14_load_budget_policy.v2.json")
    if currentness.get("budget_policy") != t48.relative(t48.LOAD_POLICY_V2):
        errors.append("T48 load projection currentness path must be the v2 policy")
    return errors


def main(argv: list[str] | None = None) -> int:
    args = t48.parse_write_check(__doc__, argv)
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
            f"{t48.relative(INPUT_OUTPUT)} and "
            f"{t48.relative(PROJECTION_OUTPUT)} are current"
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
        print(f"T48 load projection failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
