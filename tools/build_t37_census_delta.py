#!/usr/bin/env python3
"""Build the T37 post-assembler census overlay. Does not rewrite T35/T36 artifacts."""
from __future__ import annotations

import json
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from tools import t35_common as t35  # noqa: E402
from tools import t37_common as t37  # noqa: E402

BUILDER = Path(__file__).resolve()
OUTPUT = t37.CENSUS_DELTA


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
    if t37.PUBLICATION_DELTA.is_file():
        return t35.load_json(t37.PUBLICATION_DELTA)
    from tools import build_t37_publication_delta as publication_builder
    return publication_builder.build()


def _winner_runtime_axis(axis_id: str) -> int | None:
    if not t37.MEASUREMENTS.is_file():
        return None
    measurement = t35.load_json(t37.MEASUREMENTS)
    strategy = t37.production_strategy()
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


def _identity_overlay(
    census: dict[str, Any],
    source: dict[str, Any],
    generated: list[Path],
    player_path: dict[str, Any],
    strategy: dict[str, Any],
    gametest: bool,
) -> tuple[list[dict[str, Any]], dict[str, Any]]:
    pilot_ids = t37.t37_pilot_ids(census)
    identities = census.get("identities") or {}
    generated_names = {path.name for path in generated}
    provenance_kinds = sorted(set(source.get("provenance_kinds") or []))
    player_ok = (
        int(player_path.get("families") or 0) == t37.T37_FAMILY_COUNT
        and int(player_path.get("inputs_reachable") or 0) == t37.T37_FAMILY_COUNT
        and int(player_path.get("outputs_registered") or 0) == t37.T37_FAMILY_COUNT
    )
    load_measured = not strategy["blocked"]
    closure_closed = player_ok and load_measured and gametest and len(generated) == t37.T37_FAMILY_COUNT
    rows = []
    opening_ok = 0
    owner_ok = 0
    fidelity_ok = 0
    for family_id in pilot_ids:
        record = identities.get(family_id) or {}
        axes = record.get("axes") or {}
        opening = {
            "disposition": record.get("disposition"),
            "closure": (axes.get("closure") or {}).get("status"),
            "fidelity": (axes.get("fidelity") or {}).get("status"),
            "load": (axes.get("load") or {}).get("status"),
        }
        if (
            opening["disposition"] == t37.OPENING_DISPOSITION
            and opening["closure"] == t37.OPENING_CLOSURE
            and opening["load"] == t37.OPENING_LOAD
        ):
            opening_ok += 1
        if record.get("owner") == t37.OWNER:
            owner_ok += 1
        if opening["fidelity"] == t37.OPENING_FIDELITY:
            fidelity_ok += 1
        number = family_id.rsplit("#", 1)[-1]
        authored_name = f"gt_recipe_assembler_{number}.json"
        disposition = (
            t37.CLOSING_DISPOSITION if authored_name in generated_names else t37.OPENING_DISPOSITION
        )
        fidelity = opening["fidelity"] or t37.OPENING_FIDELITY
        rows.append({
            "canonical_id": family_id,
            "owner": record.get("owner") or t37.OWNER,
            "opening": opening,
            "closing": {
                "disposition": disposition,
                "closure": t37.CLOSING_CLOSURE if closure_closed else t37.OPENING_CLOSURE,
                "fidelity": fidelity,
                "load": t37.CLOSING_LOAD if load_measured else t37.OPENING_LOAD,
            },
            "provenance_kinds": provenance_kinds,
            "authored_entry": (
                t37.relative(t37.GENERATED_ROOT / authored_name)
                if authored_name in generated_names
                else None
            ),
        })
    moved = all(
        row["closing"]["disposition"] == t37.CLOSING_DISPOSITION
        and row["closing"]["closure"] == t37.CLOSING_CLOSURE
        and row["closing"]["load"] == t37.CLOSING_LOAD
        for row in rows
    )
    closeout = {
        "required": {
            "disposition": t37.CLOSING_DISPOSITION,
            "closure": t37.CLOSING_CLOSURE,
            "fidelity": t37.OPENING_FIDELITY,
            "load": t37.CLOSING_LOAD,
        },
        "opening": {
            "disposition": t37.OPENING_DISPOSITION,
            "closure": t37.OPENING_CLOSURE,
            "fidelity": t37.OPENING_FIDELITY,
            "load": t37.OPENING_LOAD,
        },
        "disposition_implemented": all(
            row["closing"]["disposition"] == t37.CLOSING_DISPOSITION for row in rows
        ),
        "closure_closed": closure_closed,
        "load_measured": load_measured,
        "fidelity_retained": all(
            row["closing"]["fidelity"] == t37.OPENING_FIDELITY for row in rows
        ),
        "complete": moved,
        "opening_statuses_match_t35": opening_ok == t37.T37_FAMILY_COUNT,
        "owner_unchanged": owner_ok == t37.T37_FAMILY_COUNT,
        "fidelity_not_dropped": fidelity_ok == t37.T37_FAMILY_COUNT and bool(provenance_kinds),
        "blockers": [
            name
            for name, ok in (
                ("player_path", player_ok),
                ("generated_50", len(generated) == t37.T37_FAMILY_COUNT),
                ("player_gametest", gametest),
                ("production_winner", load_measured),
            )
            if not ok
        ],
    }
    return rows, closeout


def _remaining_ordinary(
    census: dict[str, Any],
    overlay_ids: set[str],
    t38_ids: set[str],
) -> dict[str, Any]:
    identities = census.get("identities") or {}
    recipe_ids = {
        canonical_id
        for canonical_id, record in identities.items()
        if isinstance(record, dict) and record.get("domain") == "recipe"
    }
    recipe_families = int((census.get("counts") or {}).get("recipe_families") or len(recipe_ids))
    remaining_count = recipe_families - len(overlay_ids)
    stolen = sorted(overlay_ids & t38_ids)
    return {
        "recipe_families_in_census": recipe_families,
        "overlay_identities": len(overlay_ids),
        "remaining_ordinary_families": remaining_count,
        "expected_remaining_ordinary_families": t37.REMAINING_ORDINARY_FAMILIES,
        "overlay_contains_only_pilot_ids": overlay_ids <= recipe_ids and len(overlay_ids) == t37.T37_FAMILY_COUNT,
        "remaining_not_present_in_overlay": len(overlay_ids) == t37.T37_FAMILY_COUNT,
        "t38_ids_in_overlay": stolen,
        "t35_files_rewritten": False,
        "note": (
            "T37 overlay emits closing statuses only for the 50 pilot identities. "
            "T35 historical files are not rewritten, so the remaining 5,668 "
            "ordinary families are not bulk-marked complete."
        ),
    }


def build() -> dict[str, Any]:
    t36_delta = t35.load_json(t37.T36_CENSUS_DELTA)
    t35_readiness = t35.load_json(t35.READINESS)
    census = t37.load_census()
    source = t35.load_json(t37.ASSEMBLER_SOURCE)
    player_path = t35.load_json(t37.PLAYER_PATH)
    publication = _publication()
    generated = t37.generated_family_files()
    strategy = t37.production_strategy()
    t38 = t37.t38_frozen_wave()
    foundation = {
        key: int((t35_readiness.get("census_summary") or {}).get("counts", {}).get(key)
                 or t36_delta.get("t35_foundation", {}).get(key))
        for key in t37.T35_FOUNDATION
    }
    t36_foundation = t36_delta.get("t35_foundation") or {}
    overlay_rows, closeout = _identity_overlay(
        census,
        source,
        generated,
        player_path,
        strategy,
        t37.player_gametest_present(),
    )
    overlay_ids = {row["canonical_id"] for row in overlay_rows}
    remaining = _remaining_ordinary(census, overlay_ids, set(t38["family_ids"]))
    t36_t14 = t36_delta.get("t14_load") or {}
    opening_map = dict(t36_t14.get("closing") or {})
    opening_axes = {
        row["axis"]: row for row in t36_t14.get("axes") or [] if isinstance(row, dict)
    }
    t14_opening = {}
    t14_delta = {}
    t14_closing = {}
    t14_rows = []
    authored_delta = int(publication.get("authored_compact_family_entries") or 0)
    for axis_id in t37.T14_COUNTABLE + t37.T14_PENDING:
        opening = opening_map.get(axis_id)
        hard = (opening_axes.get(axis_id) or {}).get("hard_ceiling")
        t14_opening[axis_id] = opening
        if axis_id == "datapack_authored_entries":
            delta = authored_delta
            measured = True
            evidence = (
                "T37 authored compact family datapack entries under "
                "src/t37_recipe_generated/resources/data/cruciblecraft/recipe/t37/assembler"
            )
            closing = (opening or 0) + delta
        elif axis_id in t37.STRATEGY_AXES:
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
                    "T37 production winner is BLOCKED; eager/lazy/cache are "
                    "pending and not zero-filled"
                )
                closing = None
            else:
                delta = value
                measured = True
                evidence = (
                    "Derived from tools/t37_materialization_decision.json "
                    "production_winner; cache recorded once at card level"
                )
                closing = (opening or 0) + delta if axis_id != "lazy_cache_ceiling_rows" else delta
        else:
            runtime = None if strategy["blocked"] else _winner_runtime_axis(axis_id)
            if runtime is None:
                delta = None
                measured = False
                evidence = "runtime-perf axis is pending; not zero-filled"
                closing = None
            else:
                delta = runtime
                measured = True
                evidence = (
                    "T37 1x production-winner measurement from "
                    "tools/t37_materialization_measurements.json; T36 closing "
                    "was unmeasured so opening stays null and closing is T37 "
                    "card-level, not a reconstructed T14 cumulative"
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
        "pilot_count_50": 0 if len(overlay_rows) == t37.T37_FAMILY_COUNT else 1,
        "pilot_ids_match_census": 0 if overlay_ids == set(t37.t37_pilot_ids(census)) else 1,
        "opening_planned_incomplete_pending": 0 if closeout["opening_statuses_match_t35"] else 1,
        "fidelity_recorded_not_dropped": 0 if closeout["fidelity_not_dropped"] else 1,
        "owner_unchanged": 0 if closeout["owner_unchanged"] else 1,
        "remaining_ordinary_not_bulk_complete": 0 if (
            remaining["remaining_ordinary_families"]
            == t37.REMAINING_ORDINARY_FAMILIES
            and remaining["overlay_contains_only_pilot_ids"]
            and remaining["remaining_not_present_in_overlay"]
            and remaining["t35_files_rewritten"] is False
        ) else 1,
        "t35_foundation_unchanged": 0 if (
            foundation == t37.T35_FOUNDATION and t36_foundation == t37.T35_FOUNDATION
        ) else 1,
        "t38_not_stolen": 0 if not remaining["t38_ids_in_overlay"] else 1,
        "t14_hard_ceiling_not_raised": 0,
        "t14_pending_not_zero_filled": 0 if all(
            (t14_delta[axis]["pending"] and t14_delta[axis]["value"] is None)
            or not t14_delta[axis]["pending"]
            for axis in t37.T14_PENDING
        ) else 1,
        "t35_files_rewritten": 0,
        "authored_logical_not_zero_filled": 0 if authored_delta == t37.T37_FAMILY_COUNT else 1,
    }
    return {
        "schema_version": 1,
        "status": (
            "T37_CENSUS_DELTA_READY" if closeout["complete"] and all(
                int(value) == 0 for value in validators.values()
            ) else "T37_CENSUS_DELTA_BLOCKED"
        ),
        "source_revision": t37.SOURCE_REVISION,
        "generated_by": "python tools/build_t37_census_delta.py",
        "t35_history_readonly": True,
        "t36_history_readonly": True,
        "t35_foundation": foundation,
        "t35_foundation_unchanged": foundation == t37.T35_FOUNDATION,
        "owner": t37.OWNER,
        "host": t37.HOST,
        "work_set": {
            "family_ids": [row["canonical_id"] for row in overlay_rows],
            "family_count": len(overlay_rows),
            "source_rows": int((source.get("work_set") or {}).get("source_rows") or 0),
        },
        "identities": overlay_rows,
        "identity_closeout": closeout,
        "fidelity": {
            "identity_status": t37.OPENING_FIDELITY,
            "provenance_kinds": provenance_kinds,
            "silent_drop": False,
        },
        "remaining_ordinary": remaining,
        "t38_frozen_wave": t38,
        "publication": {
            "authored_compact_family_entries": publication.get("authored_compact_family_entries"),
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
            "opening_source": "tools/t36_census_delta.json#t14_load",
        },
        "validators": validators,
    }


def write() -> dict[str, Any]:
    document = build()
    t35.write_stable(OUTPUT, document)
    return document


def check() -> list[str]:
    errors = t37.check_document(OUTPUT, build())
    if errors:
        return errors
    document = t35.load_json(OUTPUT)
    if document.get("t35_foundation") != t37.T35_FOUNDATION:
        errors.append("T37 delta changed T35 foundation denominators")
    required_zero = (
        "pilot_count_50",
        "pilot_ids_match_census",
        "opening_planned_incomplete_pending",
        "fidelity_recorded_not_dropped",
        "owner_unchanged",
        "remaining_ordinary_not_bulk_complete",
        "t35_foundation_unchanged",
        "t38_not_stolen",
        "t14_hard_ceiling_not_raised",
        "t14_pending_not_zero_filled",
        "t35_files_rewritten",
        "authored_logical_not_zero_filled",
    )
    validators = document.get("validators") or {}
    if any(int(validators.get(key) or 0) != 0 for key in required_zero):
        errors.append("T37 census delta structural validators are not zero")
    if document.get("t14_load", {}).get("hard_ceiling_raised"):
        errors.append("T37 raised a T14 hard ceiling")
    return errors


def main(argv: list[str] | None = None) -> int:
    args = t37.parse_write_check(__doc__, argv)
    try:
        if args.write:
            document = write()
            print(json.dumps({
                "status": document["status"],
                "pilot": document["work_set"]["family_count"],
                "closeout": document["identity_closeout"]["complete"],
                "t35_foundation_unchanged": document["t35_foundation_unchanged"],
                "authored": document["publication"]["authored_compact_family_entries"],
                "t14_closing_authored": document["t14_load"]["closing"]["datapack_authored_entries"],
            }))
            return 0
        errors = check()
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        print(f"{t37.relative(OUTPUT)} is current")
        return 0
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"T37 census delta failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
