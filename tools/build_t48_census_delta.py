#!/usr/bin/env python3
"""Build the T48 Bath remainder census overlay. Does not rewrite T35-T46 artifacts."""
from __future__ import annotations

import json
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from tools import t35_common as t35  # noqa: E402
from tools import t37_common as t37  # noqa: E402
from tools import t47_common as t47_hist  # noqa: E402
from tools import t48_common as t48  # noqa: E402

OUTPUT = t48.CENSUS_DELTA


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
    if t48.PUBLICATION_DELTA.is_file():
        return t35.load_json(t48.PUBLICATION_DELTA)
    from tools import build_t48_publication_delta as publication_builder

    return publication_builder.build()


def _axis_from_candidate(axis_id: str, row: dict[str, Any]) -> int | None:
    def ceil_ms(value_ns: Any) -> int | None:
        if not isinstance(value_ns, int):
            return None
        return (value_ns + 999_999) // 1_000_000

    allocation = row.get("allocation") or {}
    values = {
        "eager_publication_rows": row.get("eager_publication_rows"),
        "lazy_logical_rows": row.get("lazy_logical_rows"),
        "lazy_cache_ceiling_rows": row.get("lazy_cache_ceiling_rows"),
        "sync_bytes": (row.get("sync") or {}).get("bytes"),
        "server_reload_ms": ceil_ms(
            ((row.get("server") or {}).get("reload") or {}).get("p95_ns")
        ),
        "server_index_ms": ceil_ms(
            ((row.get("server") or {}).get("index") or {}).get("p95_ns")
        ),
        "client_reload_ms": ceil_ms(
            ((row.get("dedicated_client") or {}).get("reload") or {}).get("p95_ns")
        ),
        "client_index_ms": ceil_ms(
            ((row.get("dedicated_client") or {}).get("index") or {}).get("p95_ns")
        ),
        "retained_memory_bytes": (row.get("retained_memory") or {}).get("p50_bytes"),
        "allocation_bytes": allocation.get("p50_bytes"),
        "lookup_p95_ns": (row.get("lookup") or {}).get("p95_ns"),
        "lookup_candidate_count": (row.get("lookup") or {}).get("candidates_p95"),
    }
    value = values.get(axis_id)
    return value if isinstance(value, int) else None


def _winner_runtime_axis(axis_id: str) -> int | None:
    row = t48.integrated_production_row()
    if row is None:
        return None
    return _axis_from_candidate(axis_id, row)


def _opening_status(record: dict[str, Any]) -> dict[str, Any]:
    return {
        "disposition": t48.OPENING_DISPOSITION,
        "closure": t48.OPENING_CLOSURE,
        "fidelity": t48.OPENING_FIDELITY,
        "load": t48.OPENING_LOAD,
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
    lock_ids = set(family_ids)
    provenance_kinds = sorted({
        str(kind)
        for relation in source.get("relations") or []
        if str(relation.get("family_id") or "") in lock_ids
        for kind in ((relation.get("provenance") or {}).get("kinds") or [])
    })
    layered = t35.load_json(t48.LAYERED_PLAYER_PATH)
    player_ok = (
        int(player_path.get("families") or 0) == t48.production_family_count()
        and int(player_path.get("inputs_reachable") or 0)
        == t48.production_relation_count()
        and int(player_path.get("outputs_registered") or 0)
        == t48.production_relation_count()
        and layered.get("status") == t48.LAYERED_PLAYER_PATH_STATUS
        and int((layered.get("b2") or {}).get("relation_count") or 0)
        == t48.production_relation_count()
        and t48.player_path_real()
        and t48.player_gametest_present()
    )
    load_measured = not strategy["blocked"]
    closure_closed = (
        player_ok
        and load_measured
        and gametest
        and len(generated) == t48.production_family_count()
    )
    rows = []
    opening_ok = 0
    owner_ok = 0
    fidelity_ok = 0
    lock_by_id = {
        str(row["family_id"]): row for row in t48.production_families()
    }
    for family_id in family_ids:
        record = identities.get(family_id) or {}
        opening = _opening_status(record)
        if (
            opening["disposition"] == t48.OPENING_DISPOSITION
            and opening["closure"] == t48.OPENING_CLOSURE
            and opening["load"] == t48.OPENING_LOAD
        ):
            opening_ok += 1
        if opening["fidelity"] == t48.OPENING_FIDELITY:
            fidelity_ok += 1
        lock_row = lock_by_id[family_id]
        authored_path = t48.authored_family_path(lock_row)
        disposition = (
            t48.CLOSING_DISPOSITION
            if authored_path.is_file()
            else t48.OPENING_DISPOSITION
        )
        rows.append({
            "canonical_id": family_id,
            "owner": t48.OWNER,
            "opening": opening,
            "closing": {
                "disposition": disposition,
                "closure": t48.CLOSING_CLOSURE if closure_closed else t48.OPENING_CLOSURE,
                "fidelity": t48.OPENING_FIDELITY,
                "load": t48.CLOSING_LOAD if load_measured else t48.OPENING_LOAD,
                "expressed_by": "t48",
            },
            "provenance_kinds": provenance_kinds,
            "authored_entry": (
                t48.relative(authored_path) if authored_path.is_file() else None
            ),
        })
        if rows[-1]["owner"] == t48.OWNER:
            owner_ok += 1
    moved = all(
        row["closing"]["disposition"] == t48.CLOSING_DISPOSITION
        and row["closing"]["closure"] == t48.CLOSING_CLOSURE
        and row["closing"]["load"] == t48.CLOSING_LOAD
        for row in rows
    )
    closeout = {
        "required": {
            "disposition": t48.CLOSING_DISPOSITION,
            "closure": t48.CLOSING_CLOSURE,
            "fidelity": t48.OPENING_FIDELITY,
            "load": t48.CLOSING_LOAD,
        },
        "opening": {
            "disposition": t48.OPENING_DISPOSITION,
            "closure": t48.OPENING_CLOSURE,
            "fidelity": t48.OPENING_FIDELITY,
            "load": t48.OPENING_LOAD,
        },
        "disposition_implemented": all(
            row["closing"]["disposition"] == t48.CLOSING_DISPOSITION for row in rows
        ),
        "closure_closed": closure_closed,
        "load_measured": load_measured,
        "fidelity_retained": all(
            row["closing"]["fidelity"] == t48.OPENING_FIDELITY for row in rows
        ),
        "complete": moved,
        "opening_statuses_match_owner_gap": opening_ok == t48.production_family_count(),
        "owner_closing": owner_ok == t48.production_family_count(),
        "fidelity_not_dropped": (
            fidelity_ok == t48.production_family_count() and bool(provenance_kinds)
        ),
        "partial_families": 0,
        "blockers": [
            name
            for name, ok in (
                ("player_path", player_ok),
                (
                    "generated_matches_lock",
                    len(generated) == t48.production_family_count(),
                ),
                ("player_gametest", gametest),
                ("production_winner", load_measured),
            )
            if not ok
        ],
    }
    return rows, closeout


def build() -> dict[str, Any]:
    t45_delta = t35.load_json(t47_hist.CENSUS_DELTA)
    census = t37.load_census()
    source = t35.load_json(t48.SOURCE)
    player_path = t35.load_json(t48.PLAYER_PATH)
    publication = _publication()
    generated = t48.generated_family_files()
    strategy = t48.production_strategy()
    family_ids = t48.production_family_ids()
    foundation = dict(t48.T35_FOUNDATION)
    overlay_rows, closeout = _identity_overlay(
        census,
        source,
        family_ids,
        generated,
        player_path,
        strategy,
        t48.player_gametest_present(),
    )
    overlay_ids = {row["canonical_id"] for row in overlay_rows}
    remaining = {
        "opening_execution_gap": t48.OPENING_EXECUTION_GAP,
        "complete_family_count": t48.COMPLETION_DELTA if closeout["complete"] else 0,
        "partial_family_count": 0,
        "completion_delta": t48.COMPLETION_DELTA if closeout["complete"] else 0,
        "reclassification_delta": t48.RECLASSIFICATION_DELTA,
        "remaining_ordinary_families": (
            t48.CLOSING_EXECUTION_GAP if closeout["complete"] else t48.OPENING_EXECUTION_GAP
        ),
        "expected_remaining_ordinary_families": t48.EXPECTED_REMAINING_ORDINARY_FAMILIES,
        "deferred_recycling_count": t48.DEFERRED_RECYCLING_COUNT,
        "overlay_contains_only_locked_ids": overlay_ids == set(family_ids),
        "remaining_not_present_in_overlay": len(overlay_ids) == t48.production_family_count(),
        "t35_files_rewritten": False,
        "t41_files_rewritten": False,
        "t42_files_rewritten": False,
        "note": (
            "T48 closes 145 Bath identity/form remainder families from the T47 recipe "
            "gap of 1499. Remaining current execution gap is 1354 when closeout "
            "is complete. Deferred recycling stays 1817. T35-T47 files are not "
            "rewritten."
        ),
    }
    t45_t14 = t45_delta.get("t14_load") or {}
    opening_map = dict(t48.t14_opening_load())
    opening_axes = {
        row["axis"]: row for row in t45_t14.get("axes") or [] if isinstance(row, dict)
    }
    t14_opening = {}
    t14_delta = {}
    t14_closing = {}
    t14_rows = []
    authored_compact = int(publication.get("authored_compact_family_entries") or 0)
    authored_delta = int(
        publication.get("authored_datapack_entries") or authored_compact
    )
    for axis_id in t48.T14_COUNTABLE + t48.T14_PENDING:
        opening = opening_map.get(axis_id)
        hard = (opening_axes.get(axis_id) or {}).get("hard_ceiling")
        t14_opening[axis_id] = opening
        if axis_id == "datapack_authored_entries":
            delta = authored_delta
            measured = True
            evidence = (
                "T48 RecipeManager JSON: compact families + three publication "
                "policies + real B1 Bath support recipes"
            )
            closing = (opening or 0) + delta
        else:
            runtime = None if strategy["blocked"] else _winner_runtime_axis(axis_id)
            if runtime is None:
                delta = None
                measured = False
                evidence = (
                    "T37-T48 integrated co-load is pending; not zero-filled "
                    "and not T46-opening plus T48-card arithmetic"
                )
                closing = None
            else:
                closing = runtime
                delta = closing - (opening or 0)
                measured = True
                evidence = (
                    "T37-T48 integrated production mix from "
                    "tools/t48_integrated_measurements.json winner row; "
                    "not T46 opening plus T48 card arithmetic"
                )
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
            0 if len(overlay_rows) == t48.production_family_count() else 1
        ),
        "family_ids_match_lock": 0 if overlay_ids == set(family_ids) else 1,
        "opening_planned_incomplete_pending": 0 if closeout["opening_statuses_match_owner_gap"] else 1,
        "fidelity_recorded_not_dropped": 0 if closeout["fidelity_not_dropped"] else 1,
        "owner_closing": 0 if closeout["owner_closing"] else 1,
        "partial_families_zero": 0 if closeout["partial_families"] == 0 else 1,
        "remaining_ordinary_not_bulk_complete": 0 if (
            remaining["remaining_ordinary_families"]
            == (
                t48.EXPECTED_REMAINING_ORDINARY_FAMILIES if closeout["complete"]
                else t48.OPENING_EXECUTION_GAP
            )
            and remaining["overlay_contains_only_locked_ids"]
            and remaining["remaining_not_present_in_overlay"]
            and remaining["t35_files_rewritten"] is False
            and remaining["t41_files_rewritten"] is False
            and remaining["t42_files_rewritten"] is False
        ) else 1,
        "t35_foundation_unchanged": 0 if foundation == t48.T35_FOUNDATION else 1,
        "t14_hard_ceiling_not_raised": 0,
        "t14_pending_not_zero_filled": 0 if all(
            (t14_delta[axis]["pending"] and t14_delta[axis]["value"] is None)
            or not t14_delta[axis]["pending"]
            for axis in t48.T14_PENDING
        ) else 1,
        "t35_files_rewritten": 0,
        "t42_files_rewritten": 0,
        "authored_logical_not_zero_filled": 0 if (
            authored_compact == t48.production_family_count()
            and int(publication.get("logical") or 0)
            == t48.production_relation_count()
        ) else 1,
        "integrated_closing_measured": 0 if all(
            not row["pending"] for row in t14_rows
        ) else 1,
    }
    return {
        "schema_version": 1,
        "status": (
            "T48_CENSUS_DELTA_READY" if closeout["complete"] and all(
                int(value) == 0 for value in validators.values()
            ) else "T48_CENSUS_DELTA_BLOCKED"
        ),
        "source_revision": t48.SOURCE_REVISION,
        "production_lock_sha256": t48.production_lock_sha256(),
        "generated_by": "python tools/build_t48_census_delta.py",
        "t35_history_readonly": True,
        "t36_history_readonly": True,
        "t37_history_readonly": True,
        "t38_history_readonly": True,
        "t39_history_readonly": True,
        "t40_history_readonly": True,
        "t41_history_readonly": True,
        "t42_history_readonly": True,
        "t43_history_readonly": True,
        "t44_history_readonly": True,
        "t45_history_readonly": True,
        "t46_history_readonly": True,
        "t47_history_readonly": True,
        "t35_foundation": foundation,
        "t35_foundation_unchanged": foundation == t48.T35_FOUNDATION,
        "owner": t48.OWNER,
        "host": t48.HOST,
        "complete_family_count": (
            t48.COMPLETION_DELTA if closeout["complete"] else 0
        ),
        "partial_family_count": 0,
        "opening_execution_gap": t48.OPENING_EXECUTION_GAP,
        "completion_delta": t48.COMPLETION_DELTA if closeout["complete"] else 0,
        "reclassification_delta": t48.RECLASSIFICATION_DELTA,
        "remaining_recipe_gap": remaining["remaining_ordinary_families"],
        "work_set": {
            "family_ids": [row["canonical_id"] for row in overlay_rows],
            "family_count": len(overlay_rows),
            "source_rows": t48.production_relation_count(),
            "phase_deferred_family_ids": [],
        },
        "identities": overlay_rows,
        "identity_closeout": closeout,
        "fidelity": {
            "identity_status": t48.OPENING_FIDELITY,
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
            "opening_source": t48.T14_OPENING_LOAD_SOURCE,
            "closing_basis": t48.T14_CLOSING_BASIS,
        },
        "currentness": {
            "load_budget_policy_v2_sha256": t48.v2_policy_hash(),
            "integrated_measurements": (
                t48.relative(t48.INTEGRATED_MEASUREMENTS)
                if t48.INTEGRATED_MEASUREMENTS.is_file()
                else None
            ),
        },
        "validators": validators,
    }


def write() -> dict[str, Any]:
    document = build()
    if document["status"] != "T48_CENSUS_DELTA_READY":
        raise ValueError(
            "T48 census --write is fail-closed until locked player path, "
            "GameTest PASS receipt, and materialization decision exist"
        )
    t35.write_stable(OUTPUT, document)
    return document


def check() -> list[str]:
    from tools import closeout_seal

    errors = closeout_seal.live_or_sealed_errors(
        "T48",
        "census",
        lambda: t48.check_document(OUTPUT, build()),
    )
    if errors:
        return errors
    document = t35.load_json(OUTPUT)
    if document.get("t35_foundation") != t48.T35_FOUNDATION:
        errors.append("T48 delta changed T35 foundation denominators")
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
        "integrated_closing_measured",
    )
    validators = document.get("validators") or {}
    if any(int(validators.get(key) or 0) != 0 for key in required_zero):
        errors.append("T48 census delta structural validators are not zero")
    if document.get("t14_load", {}).get("hard_ceiling_raised"):
        errors.append("T48 raised a T14 hard ceiling")
    remaining = int(
        (document.get("remaining_ordinary") or {}).get("remaining_ordinary_families") or 0
    )
    if remaining != t48.EXPECTED_REMAINING_ORDINARY_FAMILIES:
        errors.append(
            f"T48 remaining ordinary families {remaining} != "
            f"{t48.EXPECTED_REMAINING_ORDINARY_FAMILIES}"
        )
    t14 = document.get("t14_load") or {}
    if t14.get("closing_basis") != t48.T14_CLOSING_BASIS:
        errors.append(
            "T48 T14 closing_basis must be compact_production_18_publication_groups"
        )
    closing = t14.get("closing") or {}
    if int(closing.get("eager_publication_rows") or -1) != t48.CLOSING_EAGER_ROWS:
        errors.append("T48 T14 closing eager is not the 18-group production mix")
    if int(closing.get("lazy_logical_rows") or -1) != t48.CLOSING_LAZY_ROWS:
        errors.append("T48 T14 closing lazy is not the 18-group production mix")
    if int(closing.get("lazy_cache_ceiling_rows") or -1) != t48.CLOSING_CACHE_CEILING_ROWS:
        errors.append("T48 T14 closing cache is not the 18-group production mix")
    return errors


def main(argv: list[str] | None = None) -> int:
    args = t48.parse_write_check(__doc__, argv)
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
        print(f"{t48.relative(OUTPUT)} is current")
        return 0
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"T48 census delta failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
