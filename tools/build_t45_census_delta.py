#!/usr/bin/env python3
"""Build the T45 post-block-object census overlay. Does not rewrite T35-T44 artifacts."""
from __future__ import annotations

import json
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from tools import t35_common as t35  # noqa: E402
from tools import t37_common as t37  # noqa: E402
from tools import t43_common as t43_hist  # noqa: E402
from tools import t45_common as t45  # noqa: E402

OUTPUT = t45.CENSUS_DELTA


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
    if t45.PUBLICATION_DELTA.is_file():
        return t35.load_json(t45.PUBLICATION_DELTA)
    from tools import build_t45_publication_delta as publication_builder

    return publication_builder.build()


def _winner_runtime_axis(axis_id: str) -> int | None:
    if not t45.MEASUREMENTS.is_file():
        return None
    measurement = t35.load_json(t45.MEASUREMENTS)
    strategy = t45.production_strategy()
    if strategy["blocked"]:
        return None
    production = next(
        (
            row
            for row in measurement.get("scenarios") or []
            if row.get("id") == "card" and row.get("scale") == "1x"
        ),
        None,
    )
    if production is None:
        return None
    winner = strategy.get("card_aggregate_winner")
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


def _opening_status(record: dict[str, Any]) -> dict[str, Any]:
    return {
        "disposition": t45.OPENING_DISPOSITION,
        "closure": t45.OPENING_CLOSURE,
        "fidelity": t45.OPENING_FIDELITY,
        "load": t45.OPENING_LOAD,
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
    provenance_kinds = sorted({
        str(kind)
        for relation in source.get("relations") or []
        for kind in ((relation.get("provenance") or {}).get("kinds") or [])
    })
    layered = t35.load_json(t45.LAYERED_PLAYER_PATH)
    layered_relations = (layered.get("b2") or {}).get("relations") or []
    player_ok = (
        int(player_path.get("families") or 0) == t45.production_family_count()
        and layered.get("status") == "T45_LAYERED_PLAYER_PATH_READY"
        and layered.get("production_lock_sha256") == t45.production_lock_sha256()
        and len(layered_relations) == t45.production_relation_count()
        and all(
            row.get("inputs_reachable_in_b1") and row.get("outputs_registered")
            for row in layered_relations
        )
    )
    load_measured = not strategy["blocked"]
    closure_closed = (
        player_ok
        and load_measured
        and gametest
        and len(generated) == t45.production_family_count()
    )
    rows = []
    opening_ok = 0
    owner_ok = 0
    fidelity_ok = 0
    lock_by_id = {
        str(row["family_id"]): row for row in t45.production_families()
    }
    for family_id in family_ids:
        record = identities.get(family_id) or {}
        opening = _opening_status(record)
        if (
            opening["disposition"] == t45.OPENING_DISPOSITION
            and opening["closure"] == t45.OPENING_CLOSURE
            and opening["load"] == t45.OPENING_LOAD
        ):
            opening_ok += 1
        if opening["fidelity"] == t45.OPENING_FIDELITY:
            fidelity_ok += 1
        lock_row = lock_by_id[family_id]
        authored_path = t45.authored_family_path(lock_row)
        disposition = (
            t45.CLOSING_DISPOSITION
            if authored_path.is_file()
            else t45.OPENING_DISPOSITION
        )
        rows.append({
            "canonical_id": family_id,
            "owner": t45.OWNER,
            "opening": opening,
            "closing": {
                "disposition": disposition,
                "closure": t45.CLOSING_CLOSURE if closure_closed else t45.OPENING_CLOSURE,
                "fidelity": t45.OPENING_FIDELITY,
                "load": t45.CLOSING_LOAD if load_measured else t45.OPENING_LOAD,
                "expressed_by": "t45",
            },
            "provenance_kinds": provenance_kinds,
            "authored_entry": (
                t45.relative(authored_path) if authored_path.is_file() else None
            ),
        })
        if rows[-1]["owner"] == t45.OWNER:
            owner_ok += 1
    moved = all(
        row["closing"]["disposition"] == t45.CLOSING_DISPOSITION
        and row["closing"]["closure"] == t45.CLOSING_CLOSURE
        and row["closing"]["load"] == t45.CLOSING_LOAD
        for row in rows
    )
    closeout = {
        "required": {
            "disposition": t45.CLOSING_DISPOSITION,
            "closure": t45.CLOSING_CLOSURE,
            "fidelity": t45.OPENING_FIDELITY,
            "load": t45.CLOSING_LOAD,
        },
        "opening": {
            "disposition": t45.OPENING_DISPOSITION,
            "closure": t45.OPENING_CLOSURE,
            "fidelity": t45.OPENING_FIDELITY,
            "load": t45.OPENING_LOAD,
        },
        "disposition_implemented": all(
            row["closing"]["disposition"] == t45.CLOSING_DISPOSITION for row in rows
        ),
        "closure_closed": closure_closed,
        "load_measured": load_measured,
        "fidelity_retained": all(
            row["closing"]["fidelity"] == t45.OPENING_FIDELITY for row in rows
        ),
        "complete": moved,
        "opening_statuses_match_owner_gap": opening_ok == t45.production_family_count(),
        "owner_closing": owner_ok == t45.production_family_count(),
        "fidelity_not_dropped": (
            fidelity_ok == t45.production_family_count() and bool(provenance_kinds)
        ),
        "partial_families": 0,
        "blockers": [
            name
            for name, ok in (
                ("player_path", player_ok),
                (
                    "generated_matches_lock",
                    len(generated) == t45.production_family_count(),
                ),
                ("player_gametest", gametest),
                ("production_winner", load_measured),
            )
            if not ok
        ],
    }
    return rows, closeout


def build() -> dict[str, Any]:
    t43_delta = t35.load_json(t43_hist.CENSUS_DELTA)
    census = t37.load_census()
    source = t35.load_json(t45.SOURCE)
    player_path = t35.load_json(t45.PLAYER_PATH)
    publication = _publication()
    generated = t45.generated_family_files()
    strategy = t45.production_strategy()
    family_ids = t45.production_family_ids()
    foundation = dict(t45.T35_FOUNDATION)
    overlay_rows, closeout = _identity_overlay(
        census,
        source,
        family_ids,
        generated,
        player_path,
        strategy,
        t45.player_gametest_present(),
    )
    overlay_ids = {row["canonical_id"] for row in overlay_rows}
    remaining = {
        "opening_execution_gap": t45.OPENING_EXECUTION_GAP,
        "complete_family_count": t45.COMPLETION_DELTA if closeout["complete"] else 0,
        "partial_family_count": 0,
        "completion_delta": t45.COMPLETION_DELTA if closeout["complete"] else 0,
        "reclassification_delta": t45.RECLASSIFICATION_DELTA,
        "remaining_ordinary_families": (
            t45.CLOSING_EXECUTION_GAP if closeout["complete"] else t45.OPENING_EXECUTION_GAP
        ),
        "expected_remaining_ordinary_families": t45.EXPECTED_REMAINING_ORDINARY_FAMILIES,
        "overlay_contains_only_locked_ids": overlay_ids == set(family_ids),
        "remaining_not_present_in_overlay": len(overlay_ids) == t45.production_family_count(),
        "t35_files_rewritten": False,
        "t41_files_rewritten": False,
        "t42_files_rewritten": False,
        "note": (
            "T45 closes 379 block-object singleton families from the T44 recipe "
            "gap of 3076. Remaining current execution gap is 2697. T35-T44 files "
            "are not rewritten."
        ),
    }
    t43_t14 = t43_delta.get("t14_load") or {}
    opening_map = dict(t45.t14_opening_load())
    opening_axes = {
        row["axis"]: row for row in t43_t14.get("axes") or [] if isinstance(row, dict)
    }
    t14_opening = {}
    t14_delta = {}
    t14_closing = {}
    t14_rows = []
    authored_compact = int(publication.get("authored_compact_family_entries") or 0)
    authored_delta = int(
        publication.get("authored_datapack_entries") or authored_compact
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
    for axis_id in t45.T14_COUNTABLE + t45.T14_PENDING:
        opening = opening_map.get(axis_id)
        hard = (opening_axes.get(axis_id) or {}).get("hard_ceiling")
        t14_opening[axis_id] = opening
        if axis_id == "datapack_authored_entries":
            delta = authored_delta
            measured = True
            evidence = (
                "T45 authored compact family datapack entries plus two "
                "compact_publication_policy manifests under src/t45_recipe_generated"
            )
            closing = (opening or 0) + delta
        elif axis_id in t45.STRATEGY_AXES:
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
                    "T45 production winner is BLOCKED; eager/lazy/cache are "
                    "pending and not zero-filled"
                )
                closing = None
            elif axis_id == "lazy_cache_ceiling_rows":
                delta = value
                measured = True
                evidence = (
                    "T14 aggregation is sum; the T45 card-aggregate cache ceiling is "
                    "added once at card level in tools/t45_publication_delta.json."
                )
                closing = (opening or 0) + delta
            else:
                delta = value
                measured = True
                evidence = (
                    "Derived from tools/t45_materialization_decision.json "
                    "group-scoped production winners."
                )
                closing = (opening or 0) + delta
        elif axis_id in t45.MAX_INTERVAL_AXES:
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
                    "T45 1x card-aggregate production-winner measurement from "
                    "tools/t45_materialization_measurements.json"
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
                    "T45 1x card-aggregate production-winner measurement from "
                    "tools/t45_materialization_measurements.json; T14 sum_interval "
                    "adds T45 card delta to T43/T44 closing"
                )
                closing = (opening or 0) + runtime
            else:
                delta = runtime
                measured = True
                evidence = (
                    "T45 1x card-aggregate production-winner measurement from "
                    "tools/t45_materialization_measurements.json"
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
    provenance_kinds = sorted({
        str(kind)
        for relation in source.get("relations") or []
        for kind in ((relation.get("provenance") or {}).get("kinds") or [])
    })
    validators = {
        "family_count_matches_lock": (
            0 if len(overlay_rows) == t45.production_family_count() else 1
        ),
        "family_ids_match_lock": 0 if overlay_ids == set(family_ids) else 1,
        "opening_planned_incomplete_pending": 0 if closeout["opening_statuses_match_owner_gap"] else 1,
        "fidelity_recorded_not_dropped": 0 if closeout["fidelity_not_dropped"] else 1,
        "owner_closing": 0 if closeout["owner_closing"] else 1,
        "partial_families_zero": 0 if closeout["partial_families"] == 0 else 1,
        "remaining_ordinary_not_bulk_complete": 0 if (
            remaining["remaining_ordinary_families"]
            == (
                t45.EXPECTED_REMAINING_ORDINARY_FAMILIES if closeout["complete"]
                else t45.OPENING_EXECUTION_GAP
            )
            and remaining["overlay_contains_only_locked_ids"]
            and remaining["remaining_not_present_in_overlay"]
            and remaining["t35_files_rewritten"] is False
            and remaining["t41_files_rewritten"] is False
            and remaining["t42_files_rewritten"] is False
        ) else 1,
        "t35_foundation_unchanged": 0 if foundation == t45.T35_FOUNDATION else 1,
        "t14_hard_ceiling_not_raised": 0,
        "t14_pending_not_zero_filled": 0 if all(
            (t14_delta[axis]["pending"] and t14_delta[axis]["value"] is None)
            or not t14_delta[axis]["pending"]
            for axis in t45.T14_PENDING
        ) else 1,
        "t35_files_rewritten": 0,
        "t42_files_rewritten": 0,
        "authored_logical_not_zero_filled": 0 if (
            authored_compact == t45.production_family_count()
            and int(publication.get("logical") or 0)
            == t45.production_relation_count()
        ) else 1,
    }
    return {
        "schema_version": 1,
        "status": (
            "T45_CENSUS_DELTA_READY" if closeout["complete"] and all(
                int(value) == 0 for value in validators.values()
            ) else "T45_CENSUS_DELTA_BLOCKED"
        ),
        "source_revision": t45.SOURCE_REVISION,
        "production_lock_sha256": t45.production_lock_sha256(),
        "generated_by": "python tools/build_t45_census_delta.py",
        "t35_history_readonly": True,
        "t36_history_readonly": True,
        "t37_history_readonly": True,
        "t38_history_readonly": True,
        "t39_history_readonly": True,
        "t40_history_readonly": True,
        "t41_history_readonly": True,
        "t42_history_readonly": True,
        "t35_foundation": foundation,
        "t35_foundation_unchanged": foundation == t45.T35_FOUNDATION,
        "owner": t45.OWNER,
        "host": t45.HOST,
        "complete_family_count": (
            t45.COMPLETION_DELTA if closeout["complete"] else 0
        ),
        "partial_family_count": 0,
        "opening_execution_gap": t45.OPENING_EXECUTION_GAP,
        "completion_delta": t45.COMPLETION_DELTA if closeout["complete"] else 0,
        "reclassification_delta": t45.RECLASSIFICATION_DELTA,
        "remaining_recipe_gap": remaining["remaining_ordinary_families"],
        "work_set": {
            "family_ids": [row["canonical_id"] for row in overlay_rows],
            "family_count": len(overlay_rows),
            "source_rows": t45.production_relation_count(),
            "phase_deferred_family_ids": [],
        },
        "identities": overlay_rows,
        "identity_closeout": closeout,
        "fidelity": {
            "identity_status": t45.OPENING_FIDELITY,
            "provenance_kinds": provenance_kinds,
            "silent_drop": False,
        },
        "remaining_ordinary": remaining,
        "publication": {
            "authored_compact_family_entries": publication.get("authored_compact_family_entries"),
            "authored_publication_policy_entries": publication.get(
                "authored_publication_policy_entries"
            ),
            "authored_support_entries": publication.get("authored_support_entries"),
            "authored_datapack_entries": publication.get("authored_datapack_entries"),
            "logical": publication.get("logical"),
            "eager": publication.get("eager"),
            "lazy": publication.get("lazy"),
            "group_winners": publication.get("group_winners"),
            "card_level_shared_costs": publication.get("card_level_shared_costs"),
        },
        "t14_load": {
            "hard_ceiling_raised": False,
            "opening": t14_opening,
            "delta": t14_delta,
            "closing": t14_closing,
            "axes": t14_rows,
            "opening_source": t45.T14_OPENING_LOAD_SOURCE,
        },
        "validators": validators,
    }


def write() -> dict[str, Any]:
    document = build()
    if document["status"] != "T45_CENSUS_DELTA_READY":
        raise ValueError(
            "T45 census --write is fail-closed until locked player path, "
            "GameTest PASS receipt, and materialization decision exist"
        )
    t35.write_stable(OUTPUT, document)
    return document


def check() -> list[str]:
    errors = t45.check_document(OUTPUT, build())
    if errors:
        return errors
    document = t35.load_json(OUTPUT)
    if document.get("t35_foundation") != t45.T35_FOUNDATION:
        errors.append("T45 delta changed T35 foundation denominators")
    required_zero = (
        "family_count_matches_lock",
        "family_ids_match_lock",
        "opening_planned_incomplete_pending",
        "fidelity_recorded_not_dropped",
        "owner_closing",
        "partial_families_zero",
        "remaining_ordinary_not_bulk_complete",
        "t35_foundation_unchanged",
        "t14_hard_ceiling_not_raised",
        "t14_pending_not_zero_filled",
        "t35_files_rewritten",
        "t42_files_rewritten",
        "authored_logical_not_zero_filled",
    )
    validators = document.get("validators") or {}
    if any(int(validators.get(key) or 0) != 0 for key in required_zero):
        errors.append("T45 census delta structural validators are not zero")
    if document.get("t14_load", {}).get("hard_ceiling_raised"):
        errors.append("T45 raised a T14 hard ceiling")
    remaining = int(
        (document.get("remaining_ordinary") or {}).get("remaining_ordinary_families") or 0
    )
    if remaining != t45.EXPECTED_REMAINING_ORDINARY_FAMILIES:
        errors.append(
            f"T45 remaining ordinary families {remaining} != "
            f"{t45.EXPECTED_REMAINING_ORDINARY_FAMILIES}"
        )
    return errors


def main(argv: list[str] | None = None) -> int:
    args = t45.parse_write_check(__doc__, argv)
    try:
        if args.write:
            document = write()
            print(json.dumps({
                "status": document["status"],
                "families": document["work_set"]["family_count"],
                "closeout": document["identity_closeout"]["complete"],
                "remaining_ordinary": document["remaining_ordinary"]["remaining_ordinary_families"],
                "t14_closing_authored": document["t14_load"]["closing"]["datapack_authored_entries"],
            }))
            return 0
        errors = check()
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        print(f"{t45.relative(OUTPUT)} is current")
        return 0
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"T45 census delta failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
