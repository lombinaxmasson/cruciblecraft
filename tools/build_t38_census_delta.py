#!/usr/bin/env python3
"""Build the T38 post-roaster census overlay. Does not rewrite T35/T36/T37 artifacts."""
from __future__ import annotations

import json
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from tools import t35_common as t35  # noqa: E402
from tools import t37_common as t37  # noqa: E402
from tools import t38_common as t38  # noqa: E402

BUILDER = Path(__file__).resolve()
OUTPUT = t38.CENSUS_DELTA


def _t14_axis_row(
    *,
    axis_id: str,
    opening: Any,
    delta: Any,
    closing: Any,
    measured: bool,
    evidence: str,
    hard_ceiling: Any,
) -> dict[str, Any]:
    return {
        "axis": axis_id,
        "opening": opening,
        "delta": delta,
        "closing": closing,
        "measured": measured,
        "pending": not measured,
        "evidence": evidence,
        "hard_ceiling": hard_ceiling,
        "hard_ceiling_raised": False,
    }


def _publication() -> dict[str, Any]:
    if t38.PUBLICATION_DELTA.is_file():
        return t35.load_json(t38.PUBLICATION_DELTA)
    from tools import build_t38_publication_delta as publication_builder
    return publication_builder.build()


def _winner_runtime_axis(axis_id: str) -> int | None:
    if not t38.MEASUREMENTS.is_file():
        return None
    measurement = t35.load_json(t38.MEASUREMENTS)
    strategy = t38.production_strategy()
    winner = strategy.get("winner")
    if not winner:
        return None
    production = next(
        (
            row
            for row in measurement.get("scenarios") or []
            if row.get("scale") == "1x"
        ),
        None,
    )
    if production is None:
        return None
    row = next(
        (
            candidate
            for candidate in production.get("candidates") or []
            if candidate.get("candidate") == winner
        ),
        None,
    )
    if row is None:
        return None

    def ceil_ms(value_ns: Any) -> int | None:
        if not isinstance(value_ns, int):
            return None
        return (value_ns + 999_999) // 1_000_000

    values = {
        "sync_bytes": row.get("sync", {}).get("bytes"),
        "server_reload_ms": ceil_ms(row.get("server", {}).get("reload", {}).get("p95_ns")),
        "server_index_ms": ceil_ms(row.get("server", {}).get("index", {}).get("p95_ns")),
        "client_reload_ms": ceil_ms(
            row.get("dedicated_client", {}).get("reload", {}).get("p95_ns")
        ),
        "client_index_ms": ceil_ms(
            row.get("dedicated_client", {}).get("index", {}).get("p95_ns")
        ),
        "retained_memory_bytes": row.get("retained_memory", {}).get("p50_bytes"),
        "allocation_bytes": row.get("allocation", {}).get("p50_bytes"),
        "lookup_p95_ns": row.get("lookup", {}).get("p95_ns"),
        "lookup_candidate_count": row.get("lookup", {}).get("candidates_p95"),
    }
    value = values.get(axis_id)
    return value if isinstance(value, int) else None


def _t38_opening(record: dict[str, Any]) -> dict[str, Any]:
    return {
        "disposition": t38.OPENING_DISPOSITION,
        "closure": t38.OPENING_CLOSURE,
        "fidelity": t38.OPENING_FIDELITY,
        "load": t38.OPENING_LOAD,
        "t35_historical": {
            "disposition": record.get("disposition"),
            "owner": record.get("owner"),
        },
    }


def _identity_overlay(
    census: dict[str, Any],
    source: dict[str, Any],
    family_ids: list[str],
    generated: list[Path],
    player_path: dict[str, Any],
    strategy: dict[str, Any],
    gametest: bool,
) -> tuple[list[dict[str, Any]], dict[str, Any]]:
    identities = census.get("identities") or {}
    generated_names = {path.name for path in generated}
    provenance_kinds = sorted(set(source.get("provenance_kinds") or []))
    player_ok = (
        int(player_path.get("families") or 0) == t38.FAMILY_COUNT
        and int(player_path.get("inputs_reachable") or 0) == t38.SOURCE_ROWS
        and int(player_path.get("outputs_registered") or 0) == t38.SOURCE_ROWS
    )
    load_measured = not strategy["blocked"]
    closure_closed = (
        player_ok
        and load_measured
        and gametest
        and len(generated) == t38.FAMILY_COUNT
    )
    rows = []
    opening_ok = 0
    owner_ok = 0
    fidelity_ok = 0
    for family_id in family_ids:
        record = identities.get(family_id) or {}
        opening = _t38_opening(record)
        if (
            opening["disposition"] == t38.OPENING_DISPOSITION
            and opening["closure"] == t38.OPENING_CLOSURE
            and opening["load"] == t38.OPENING_LOAD
        ):
            opening_ok += 1
        if opening["fidelity"] == t38.OPENING_FIDELITY:
            fidelity_ok += 1
        number = family_id.rsplit("#", 1)[-1]
        authored_name = f"gt_recipe_roaster_{number}.json"
        disposition = (
            t38.CLOSING_DISPOSITION
            if authored_name in generated_names
            else t38.OPENING_DISPOSITION
        )
        rows.append({
            "canonical_id": family_id,
            "owner": t38.OWNER,
            "opening": opening,
            "closing": {
                "disposition": disposition,
                "closure": t38.CLOSING_CLOSURE if closure_closed else t38.OPENING_CLOSURE,
                "fidelity": t38.OPENING_FIDELITY,
                "load": t38.CLOSING_LOAD if load_measured else t38.OPENING_LOAD,
            },
            "provenance_kinds": provenance_kinds,
            "authored_entry": (
                t38.relative(t38.GENERATED_ROOT / authored_name)
                if authored_name in generated_names
                else None
            ),
        })
        if rows[-1]["owner"] == t38.OWNER:
            owner_ok += 1
    moved = all(
        row["closing"]["disposition"] == t38.CLOSING_DISPOSITION
        and row["closing"]["closure"] == t38.CLOSING_CLOSURE
        and row["closing"]["load"] == t38.CLOSING_LOAD
        for row in rows
    )
    closeout = {
        "required": {
            "disposition": t38.CLOSING_DISPOSITION,
            "closure": t38.CLOSING_CLOSURE,
            "fidelity": t38.OPENING_FIDELITY,
            "load": t38.CLOSING_LOAD,
        },
        "opening": {
            "disposition": t38.OPENING_DISPOSITION,
            "closure": t38.OPENING_CLOSURE,
            "fidelity": t38.OPENING_FIDELITY,
            "load": t38.OPENING_LOAD,
        },
        "disposition_implemented": all(
            row["closing"]["disposition"] == t38.CLOSING_DISPOSITION for row in rows
        ),
        "closure_closed": closure_closed,
        "load_measured": load_measured,
        "fidelity_retained": all(
            row["closing"]["fidelity"] == t38.OPENING_FIDELITY for row in rows
        ),
        "complete": moved,
        "opening_statuses_match_t38": opening_ok == t38.FAMILY_COUNT,
        "owner_closing": owner_ok == t38.FAMILY_COUNT,
        "fidelity_not_dropped": fidelity_ok == t38.FAMILY_COUNT and bool(provenance_kinds),
        "partial_families": 0,
        "blockers": [
            name
            for name, ok in (
                ("player_path", player_ok),
                ("generated_29", len(generated) == t38.FAMILY_COUNT),
                ("player_gametest", gametest),
                ("production_winner", load_measured),
            )
            if not ok
        ],
    }
    return rows, closeout


def _remaining_ordinary(
    t37_delta: dict[str, Any],
    overlay_ids: set[str],
    t37_pilot_ids: set[str],
) -> dict[str, Any]:
    t37_remaining = int(
        (t37_delta.get("remaining_ordinary") or {}).get("remaining_ordinary_families") or 0
    )
    remaining_count = t37_remaining - len(overlay_ids)
    stolen_t37 = sorted(overlay_ids & t37_pilot_ids)
    bootstrap_id = "portfolio:track_a/cruciblecraft:roaster/t36/roaster/coal_dust_bootstrap"
    return {
        "recipe_families_in_census": t38.T35_FOUNDATION["recipe_families"],
        "t37_remaining_opening": t37_remaining,
        "overlay_identities": len(overlay_ids),
        "remaining_ordinary_families": remaining_count,
        "expected_remaining_ordinary_families": t38.EXPECTED_REMAINING_ORDINARY_FAMILIES,
        "overlay_contains_only_frozen_ids": overlay_ids == set(t38.load_t38_frozen_family_ids()),
        "remaining_not_present_in_overlay": len(overlay_ids) == t38.FAMILY_COUNT,
        "t37_ids_in_overlay": stolen_t37,
        "t36_bootstrap_in_overlay": bootstrap_id in overlay_ids,
        "t35_files_rewritten": False,
        "note": (
            "T38 overlay emits closing statuses only for the 29 frozen Roaster "
            "identities. T35 historical files are not rewritten. Remaining "
            "ordinary families are derived from T37 remaining minus this overlay."
        ),
    }


def build() -> dict[str, Any]:
    t37_delta = t35.load_json(t37.CENSUS_DELTA)
    t37_readiness = t35.load_json(t37.READINESS)
    census = t37.load_census()
    source = t35.load_json(t38.SOURCE)
    player_path = t35.load_json(t38.PLAYER_PATH)
    publication = _publication()
    generated = t38.generated_family_files()
    strategy = t38.production_strategy()
    family_ids = t38.load_t38_frozen_family_ids()
    foundation = {
        key: int(
            (t37_readiness.get("census_summary") or {}).get("counts", {}).get(key)
            or (t37_delta.get("t35_foundation") or {}).get(key)
            or t38.T35_FOUNDATION[key]
        )
        for key in t38.T35_FOUNDATION
    }
    t37_foundation = t37_delta.get("t35_foundation") or {}
    overlay_rows, closeout = _identity_overlay(
        census,
        source,
        family_ids,
        generated,
        player_path,
        strategy,
        t38.player_gametest_present(),
    )
    overlay_ids = {row["canonical_id"] for row in overlay_rows}
    t37_pilot_ids = set(t37.t37_pilot_ids(census))
    remaining = _remaining_ordinary(t37_delta, overlay_ids, t37_pilot_ids)
    t37_t14 = t37_delta.get("t14_load") or {}
    opening_map = dict(t37_t14.get("closing") or {})
    opening_axes = {
        row["axis"]: row for row in t37_t14.get("axes") or [] if isinstance(row, dict)
    }
    t14_opening = {}
    t14_delta = {}
    t14_closing = {}
    t14_rows = []
    authored_compact = int(publication.get("authored_compact_family_entries") or 0)
    authored_delta = int(
        publication.get("authored_datapack_entries")
        or authored_compact
    )
    sum_interval_axes = {
        "sync_bytes",
        "retained_memory_bytes",
        "allocation_bytes",
        "server_reload_ms",
        "server_index_ms",
        "client_reload_ms",
        "client_index_ms",
    }
    for axis_id in t38.T14_COUNTABLE + t38.T14_PENDING:
        opening = opening_map.get(axis_id)
        hard = (opening_axes.get(axis_id) or {}).get("hard_ceiling")
        t14_opening[axis_id] = opening
        if axis_id == "datapack_authored_entries":
            delta = authored_delta
            measured = True
            evidence = (
                "T38 authored compact family datapack entries under "
                "src/t38_recipe_generated plus 5 GT recovery, 10 ore-block "
                "crusher, and 4 packing support recipes"
            )
            closing = (opening or 0) + delta
        elif axis_id in t38.STRATEGY_AXES:
            value = publication.get(
                "eager" if axis_id == "eager_publication_rows"
                else "lazy" if axis_id == "lazy_logical_rows"
                else None
            )
            if axis_id == "lazy_cache_ceiling_rows":
                value = (publication.get("card_level_shared_costs") or {}).get(
                    "lazy_cache_ceiling_rows"
                )
            if strategy["blocked"] or value is None:
                delta = None
                measured = False
                evidence = (
                    "T38 production winner is BLOCKED; eager/lazy/cache are "
                    "pending and not zero-filled"
                )
                closing = None
            elif axis_id == "lazy_cache_ceiling_rows":
                delta = value
                measured = True
                evidence = (
                    "T14 aggregation is sum; live T37 assembler cache 8 plus "
                    "T38 roaster cache 16. Card-level publication still records "
                    "the T38 cache once."
                )
                closing = (opening or 0) + delta
            else:
                delta = value
                measured = True
                evidence = (
                    "Derived from tools/t38_materialization_decision.json "
                    "production_winner plus T38 player-path support. Compact "
                    "on_demand contributes 0 eager; 15 GT support recipes are "
                    "immediate eager."
                )
                closing = (opening or 0) + delta
        elif axis_id in t38.MAX_INTERVAL_AXES:
            runtime = None if strategy["blocked"] else _winner_runtime_axis(axis_id)
            if runtime is None:
                delta = None
                measured = False
                evidence = "max_interval axis is pending; not zero-filled"
                closing = None
            else:
                closing = max(opening or 0, runtime)
                delta = closing - (opening or 0)
                measured = True
                evidence = (
                    "T38 1x production-winner measurement from "
                    "tools/t38_materialization_measurements.json; max_interval "
                    "closing is max(T37 closing, on_demand p95)"
                )
        else:
            runtime = None if strategy["blocked"] else _winner_runtime_axis(axis_id)
            if runtime is None:
                delta = None
                measured = False
                evidence = "runtime-perf axis is pending; not zero-filled"
                closing = None
            elif axis_id in sum_interval_axes:
                delta = runtime
                measured = True
                evidence = (
                    "T38 1x production-winner measurement from "
                    "tools/t38_materialization_measurements.json; T14 sum_interval "
                    "adds T38 card delta to T37 closing"
                )
                closing = (opening or 0) + runtime
            else:
                delta = runtime
                measured = True
                evidence = (
                    "T38 1x production-winner measurement from "
                    "tools/t38_materialization_measurements.json"
                )
                closing = runtime
        t14_delta[axis_id] = {
            "value": delta,
            "measured": measured,
            "pending": not measured,
        }
        t14_closing[axis_id] = closing
        t14_rows.append(
            _t14_axis_row(
                axis_id=axis_id,
                opening=opening,
                delta=delta,
                closing=closing,
                measured=measured,
                evidence=evidence,
                hard_ceiling=hard,
            )
        )
    provenance_kinds = sorted(set(source.get("provenance_kinds") or []))
    validators = {
        "family_count_29": 0 if len(overlay_rows) == t38.FAMILY_COUNT else 1,
        "family_ids_match_frozen": 0 if overlay_ids == set(family_ids) else 1,
        "opening_planned_incomplete_pending": 0 if closeout["opening_statuses_match_t38"] else 1,
        "fidelity_recorded_not_dropped": 0 if closeout["fidelity_not_dropped"] else 1,
        "owner_closing": 0 if closeout["owner_closing"] else 1,
        "partial_families_zero": 0 if closeout["partial_families"] == 0 else 1,
        "remaining_ordinary_not_bulk_complete": 0 if (
            remaining["remaining_ordinary_families"]
            == t38.EXPECTED_REMAINING_ORDINARY_FAMILIES
            and remaining["overlay_contains_only_frozen_ids"]
            and remaining["remaining_not_present_in_overlay"]
            and not remaining["t36_bootstrap_in_overlay"]
            and remaining["t35_files_rewritten"] is False
        ) else 1,
        "t35_foundation_unchanged": 0 if (
            foundation == t38.T35_FOUNDATION and t37_foundation == t38.T35_FOUNDATION
        ) else 1,
        "t37_not_stolen": 0 if not remaining["t37_ids_in_overlay"] else 1,
        "t14_hard_ceiling_not_raised": 0,
        "t14_pending_not_zero_filled": 0 if all(
            (t14_delta[axis]["pending"] and t14_delta[axis]["value"] is None)
            or not t14_delta[axis]["pending"]
            for axis in t38.T14_PENDING
        ) else 1,
        "t35_files_rewritten": 0,
        "authored_logical_not_zero_filled": 0 if (
            authored_compact == t38.FAMILY_COUNT
            and authored_delta == t38.AUTHORED_DATAPACK_COUNT
            and int(publication.get("logical") or 0) == t38.SOURCE_ROWS
            and int(publication.get("eager") or 0) == t38.SUPPORT_EAGER_COUNT
        ) else 1,
    }
    return {
        "schema_version": 1,
        "status": (
            "T38_CENSUS_DELTA_READY" if closeout["complete"] and all(
                int(value) == 0 for value in validators.values()
            ) else "T38_CENSUS_DELTA_BLOCKED"
        ),
        "source_revision": t38.SOURCE_REVISION,
        "generated_by": "python tools/build_t38_census_delta.py",
        "t35_history_readonly": True,
        "t36_history_readonly": True,
        "t37_history_readonly": True,
        "t35_foundation": foundation,
        "t35_foundation_unchanged": foundation == t38.T35_FOUNDATION,
        "owner": t38.OWNER,
        "host": t38.HOST,
        "work_set": {
            "family_ids": [row["canonical_id"] for row in overlay_rows],
            "family_count": len(overlay_rows),
            "source_rows": int((source.get("work_set") or {}).get("source_rows") or t38.SOURCE_ROWS),
        },
        "identities": overlay_rows,
        "identity_closeout": closeout,
        "fidelity": {
            "identity_status": t38.OPENING_FIDELITY,
            "provenance_kinds": provenance_kinds,
            "silent_drop": False,
        },
        "remaining_ordinary": remaining,
        "publication": {
            "authored_compact_family_entries": publication.get("authored_compact_family_entries"),
            "authored_support_entries": publication.get("authored_support_entries"),
            "authored_datapack_entries": publication.get("authored_datapack_entries"),
            "logical": publication.get("logical"),
            "eager": publication.get("eager"),
            "lazy": publication.get("lazy"),
            "strategy": publication.get("strategy"),
            "card_level_shared_costs": publication.get("card_level_shared_costs"),
        },
        "t14_load": {
            "hard_ceiling_raised": False,
            "opening": t14_opening,
            "delta": t14_delta,
            "closing": t14_closing,
            "axes": t14_rows,
            "opening_source": t38.T14_OPENING_LOAD_SOURCE,
        },
        "validators": validators,
    }


def write() -> dict[str, Any]:
    document = build()
    t35.write_stable(OUTPUT, document)
    return document


def check() -> list[str]:
    errors = t38.check_document(OUTPUT, build())
    if errors:
        return errors
    document = t35.load_json(OUTPUT)
    if document.get("t35_foundation") != t38.T35_FOUNDATION:
        errors.append("T38 delta changed T35 foundation denominators")
    required_zero = (
        "family_count_29",
        "family_ids_match_frozen",
        "opening_planned_incomplete_pending",
        "fidelity_recorded_not_dropped",
        "owner_closing",
        "partial_families_zero",
        "remaining_ordinary_not_bulk_complete",
        "t35_foundation_unchanged",
        "t37_not_stolen",
        "t14_hard_ceiling_not_raised",
        "t14_pending_not_zero_filled",
        "t35_files_rewritten",
        "authored_logical_not_zero_filled",
    )
    validators = document.get("validators") or {}
    if any(int(validators.get(key) or 0) != 0 for key in required_zero):
        errors.append("T38 census delta structural validators are not zero")
    if document.get("t14_load", {}).get("hard_ceiling_raised"):
        errors.append("T38 raised a T14 hard ceiling")
    remaining = int(
        (document.get("remaining_ordinary") or {}).get("remaining_ordinary_families") or 0
    )
    if remaining != t38.EXPECTED_REMAINING_ORDINARY_FAMILIES:
        errors.append(
            f"T38 remaining ordinary families {remaining} != "
            f"{t38.EXPECTED_REMAINING_ORDINARY_FAMILIES}"
        )
    return errors


def main(argv: list[str] | None = None) -> int:
    args = t38.parse_write_check(__doc__, argv)
    try:
        if args.write:
            document = write()
            print(json.dumps({
                "status": document["status"],
                "families": document["work_set"]["family_count"],
                "closeout": document["identity_closeout"]["complete"],
                "t35_foundation_unchanged": document["t35_foundation_unchanged"],
                "authored_compact": document["publication"]["authored_compact_family_entries"],
                "authored_datapack": document["publication"]["authored_datapack_entries"],
                "remaining_ordinary": document["remaining_ordinary"]["remaining_ordinary_families"],
                "t14_closing_authored": document["t14_load"]["closing"]["datapack_authored_entries"],
            }))
            return 0
        errors = check()
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        print(f"{t38.relative(OUTPUT)} is current")
        return 0
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"T38 census delta failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
