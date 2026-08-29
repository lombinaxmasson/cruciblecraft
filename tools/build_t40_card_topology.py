#!/usr/bin/env python3
"""Build the T40 recipe-then-storage topology overlay. Does not rewrite T35-T38."""
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
from tools import t39_common as t39  # noqa: E402
from tools import t40_common as t40  # noqa: E402

BUILDER = Path(__file__).resolve()
OUTPUT = t40.T40_CARD_TOPOLOGY


def _census_delta() -> dict[str, Any]:
    if t40.CENSUS_DELTA.is_file():
        return t35.load_json(t40.CENSUS_DELTA)
    from tools import build_t40_census_delta as census_builder

    return census_builder.build()


def t40_complete(delta: dict[str, Any]) -> bool:
    closeout = delta.get("identity_closeout") or {}
    return bool(closeout.get("complete")) and not (
        t40.production_strategy()["blocked"]
        or not t40.player_gametest_present()
    )


def _t40_wave(delta: dict[str, Any], complete: bool) -> dict[str, Any]:
    family_ids = t40.production_family_ids()
    return {
        "id": "T40",
        "kind": "production_lock_wave",
        "track": "recipe_wave",
        "status": "complete" if complete else "active",
        "owner": "T40",
        "depends_on": ["T36", "T37", "T38", "T39"],
        "host": t40.HOST,
        "size": t40.production_family_count(),
        "family_ids": family_ids,
        "relation_count": t40.production_relation_count(),
        "production_lock_sha256": t40.production_lock_sha256(),
        "catalog_fixture": {
            "families": t40.CATALOG_FAMILY_COUNT,
            "relations": t40.CATALOG_RELATION_COUNT,
            "production": False,
        },
        "phase_deferred": {
            "owner": "later:electrolyzer_combinatorial",
            "family_ids": t40.phase_deferred_family_ids(),
            "reason": "combinatorial_player_path_unproven",
        },
        "wave_size_band": [t37.RECIPE_WAVE_MIN, t37.RECIPE_WAVE_MAX],
        "shard_architecture_start": True,
        "load_opening": {
            "axis": "datapack_authored_entries",
            "source": t40.T14_OPENING_LOAD_SOURCE,
            "closing": t40.t14_opening_load(),
        },
        "load_closing": {
            "source": "tools/t40_census_delta.json#t14_load",
            "closing": (delta.get("t14_load") or {}).get("closing"),
        },
        "exit_gate": (
            "locked Electrolyzer families have a live host_exact map with "
            "shard-routed publication groups; combinatorial #0000/#0001 stay BLOCKED"
        ),
        "append_rule": "this card is closed; do not append families after issuance",
        "replacement_recheck": (
            "T40 closure remeasures host_exact and authored/logical counts "
            "for the production lock only"
        ),
        "note": (
            "61/151 is catalog fixture-only. T40 publishes the reviewed 13/22 lock; "
            "combinatorial #0000/#0001 stay BLOCKED and do not reduce the gap."
        ),
    }


def _t39_wave() -> dict[str, Any]:
    topology = t35.load_json(t39.T39_CARD_TOPOLOGY)
    for row in topology.get("sequence") or []:
        if row.get("id") == "T39":
            wave = dict(row)
            wave["status"] = "complete"
            wave["note"] = (
                "T39 Centrifuge production-lock wave is complete; "
                "T40 continues the recipe track."
            )
            return wave
    raise ValueError("T39 topology is missing the T39 wave")


def _t38_wave() -> dict[str, Any]:
    return {
        "id": "T38",
        "kind": "fixed_wave",
        "track": "recipe_wave",
        "status": "complete",
        "owner": "T38",
        "depends_on": ["T36", "T37"],
        "host": t38.HOST,
        "size": t38.FAMILY_COUNT,
        "family_ids": t38.load_t38_frozen_family_ids(),
        "wave_size_band": [t37.RECIPE_WAVE_MIN, t37.RECIPE_WAVE_MAX],
        "size_exception": "host_complete_small_wave",
        "load_opening": {
            "axis": "datapack_authored_entries",
            "source": t38.T14_OPENING_LOAD_SOURCE,
            "closing": t38.t14_opening_load(),
        },
        "load_closing": {
            "source": "tools/t38_census_delta.json#t14_load",
        },
        "exit_gate": "roaster 29 ordinary families have a live host_exact map",
        "append_rule": "this card is closed; do not append families after issuance",
        "note": "T38 Roaster wave is complete; T39 continues the recipe track.",
    }


def _sequence(delta: dict[str, Any], complete: bool) -> list[dict[str, Any]]:
    return [
        {
            "id": "T36",
            "kind": "fixed",
            "track": "machines",
            "status": "complete",
            "owner": "T36",
        },
        {
            "id": "T37",
            "kind": "fixed",
            "track": "calibration",
            "status": "complete",
            "owner": "T37",
            "host": t37.HOST,
            "size": t37.T37_FAMILY_COUNT,
            "note": (
                "Fixed 50-family assembler calibration. Later waves must not "
                "steal this work."
            ),
        },
        _t38_wave(),
        _t39_wave(),
        _t40_wave(delta, complete),
        {
            "kind": "generated_rule",
            "track": "recipe_wave",
            "id_policy": "consecutive_from_T41",
            "owner_rule": "the issued card id owns the wave",
            "depends_on": ["T40"],
            "preassigned_host": False,
            "until": "1.x recipe gap = 0",
            "wave_size_band": [t37.RECIPE_WAVE_MIN, t37.RECIPE_WAVE_MAX],
            "append_rule": (
                "Issue a new consecutive card for each additional wave. "
                "Do not renumber T38, T39, or T40. Do not pre-assign storage ids."
            ),
            "load_opening": "each wave records T14 opening + delta + closing",
            "exit_gate": "wave families authored or measured; remaining gap recomputed",
            "note": (
                "Next recipe wave content is not preassigned. T41 has no frozen "
                "family_ids or host assignment until issued."
            ),
        },
        {
            "kind": "generated_rule",
            "track": "storage_bundle",
            "id_policy": "consecutive_after_last_recipe_wave",
            "depends_on": ["recipe_waves_complete"],
            "after": "1.x recipe gap = 0",
            "note": (
                "Storage bundles start only after recipe waves close the 1.x gap."
            ),
            "owner_rule": "issued storage card id",
            "load_opening": "each bundle records T14 opening + delta + closing",
            "exit_gate": "canonical storage families implemented under the bundle architecture",
        },
        {
            "kind": "generated_rule",
            "track": "1x_exit_gate",
            "id_policy": "consecutive_after_storage",
            "depends_on": ["storage_bundles"],
            "after": "storage_complete",
            "owner_rule": "issued exit-gate card id",
            "exit_gate": "1.x recipe gap = 0 and storage bundles complete",
        },
        {
            "id": "nuclear_source_physics_census",
            "kind": "later_stage",
            "track": "nuclear",
            "later_stage": "post_1x",
            "owner": "post_1x",
        },
    ]


def build() -> dict[str, Any]:
    delta = _census_delta()
    complete = t40_complete(delta)
    sequence = _sequence(delta, complete)
    t40_row = sequence[4]
    recipe_index = next(
        index for index, row in enumerate(sequence) if row.get("id") == "T40"
    )
    storage_index = next(
        index for index, row in enumerate(sequence) if row["track"] == "storage_bundle"
    )
    next_rule = sequence[5]
    unique_active = None if complete else "T40"
    remaining_gap = int(
        (delta.get("remaining_ordinary") or {}).get("remaining_ordinary_families") or 0
    )
    validators = {
        "t36_complete": 0 if sequence[0].get("status") == "complete" else 1,
        "t37_complete": 0 if sequence[1].get("status") == "complete" else 1,
        "t38_complete": 0 if sequence[2].get("status") == "complete" else 1,
        "t39_complete": 0 if sequence[3].get("status") == "complete" else 1,
        "t40_complete_only_if_ready": 0 if (
            (complete and t40_row.get("status") == "complete")
            or (not complete and t40_row.get("status") == "active")
        ) else 1,
        "unique_active_t40_while_incomplete": 0 if (
            (complete and unique_active is None)
            or (not complete and unique_active == "T40")
        ) else 1,
        "revoked_old_storage_first_topology": 0,
        "t40_is_recipe_wave": 0 if t40_row.get("track") == "recipe_wave" else 1,
        "t40_has_family_assignment": 0 if (
            t40_row.get("family_ids")
            and t40_row.get("size") == t40.production_family_count()
        ) else 1,
        "t40_host_electrolyzer": 0 if t40_row.get("host") == t40.HOST else 1,
        "t38_not_stolen": 0 if sequence[2].get("size") == t38.FAMILY_COUNT else 1,
        "t39_not_stolen": 0 if sequence[3].get("size") == t39.production_family_count() else 1,
        "recipe_before_storage": 0 if recipe_index < storage_index else 1,
        "storage_not_preassigned": 0 if (
            sequence[6].get("track") == "storage_bundle"
            and sequence[6].get("id") is None
        ) else 1,
        "t41_not_preassigned": 0 if (
            next_rule.get("id") is None
            and not next_rule.get("family_ids")
            and not next_rule.get("host")
            and next_rule.get("preassigned_host") is False
        ) else 1,
        "remaining_gap_from_census": 0 if (
            remaining_gap == (
                t40.EXPECTED_REMAINING_ORDINARY_FAMILIES if complete
                else t40.T39_REMAINING_ORDINARY_FAMILIES
            )
        ) else 1,
        "t40_family_ids_locked": 0 if (
            t40_row.get("family_ids") == t40.production_family_ids()
        ) else 1,
    }
    return {
        "epoch": "T40_PRODUCTION_LOCK",
        "append_only": False,
        "fixed_cards": ["T36", "T37", "T38", "T39", "T40"],
        "unique_active_card": unique_active,
        "next_issue_id": "T41",
        "t40_complete": complete,
        "remaining_recipe_gap": remaining_gap,
        "revoked_unstarted_cards": [f"T{number}" for number in range(42, 47)],
        "rule": (
            "T36 stays complete; T37 calibration stays 50 assembler families and "
            "is complete; T38 Roaster wave is complete; T39 Centrifuge wave is "
            "complete; T40 is the Electrolyzer production-lock wave; additional "
            "recipe waves number consecutively from T41 until gap=0; storage is "
            "issued only after that; unique_active_card is T40 while incomplete "
            "and null when READY; T41 is not preassigned"
        ),
        "sequence": sequence,
        "validators": validators,
        "t35_history_readonly": True,
        "t36_history_readonly": True,
        "t37_history_readonly": True,
        "t38_history_readonly": True,
        "t39_history_readonly": True,
        "generated_by": "python tools/build_t40_card_topology.py",
        "source_revision": t40.SOURCE_REVISION,
        "production_lock_sha256": t40.production_lock_sha256(),
        "schema_version": 1,
    }


def write() -> dict[str, Any]:
    document = build()
    t35.write_stable(OUTPUT, document)
    return document


def check() -> list[str]:
    errors = t40.check_document(OUTPUT, build())
    if errors:
        return errors
    document = t35.load_json(OUTPUT)
    sequence = document.get("sequence") or [{}]
    if sequence[0].get("status") != "complete" or sequence[0].get("id") != "T36":
        errors.append("T36 is not complete in T40 topology")
    if sequence[1].get("status") != "complete" or sequence[1].get("id") != "T37":
        errors.append("T37 is not complete in T40 topology")
    if sequence[2].get("status") != "complete" or sequence[2].get("id") != "T38":
        errors.append("T38 is not complete in T40 topology")
    if sequence[3].get("status") != "complete" or sequence[3].get("id") != "T39":
        errors.append("T39 is not complete in T40 topology")
    if sequence[4].get("track") != "recipe_wave" or sequence[4].get("id") != "T40":
        errors.append("T40 is not a recipe wave")
    if sequence[4].get("size") != t40.production_family_count():
        errors.append("T40 family assignment drifted from production lock")
    if sequence[5].get("id") or sequence[5].get("family_ids") or sequence[5].get("host"):
        errors.append("T41 content must not be preassigned")
    if sequence[5].get("preassigned_host") is not False:
        errors.append("T41 preassigned_host must be false")
    if sequence[6].get("track") != "storage_bundle" or sequence[6].get("id"):
        errors.append("storage must not be pre-assigned a card id")
    if document.get("next_issue_id") != "T41":
        errors.append("next_issue_id must be T41")
    if document.get("t40_complete") and document.get("unique_active_card") is not None:
        errors.append("T40 complete must leave unique_active_card null")
    if not document.get("t40_complete") and document.get("unique_active_card") != "T40":
        errors.append("T40 must be the unique active card while not complete")
    expected_gap = (
        t40.EXPECTED_REMAINING_ORDINARY_FAMILIES
        if document.get("t40_complete")
        else t40.T39_REMAINING_ORDINARY_FAMILIES
    )
    if int(document.get("remaining_recipe_gap") or 0) != expected_gap:
        errors.append("remaining_recipe_gap must match census remaining ordinary families")
    validators = document.get("validators") or {}
    if any(int(value or 0) != 0 for value in validators.values()):
        errors.append("T40 topology validators are not zero")
    return errors


def main(argv: list[str] | None = None) -> int:
    args = t40.parse_write_check(__doc__, argv)
    try:
        if args.write:
            document = write()
            print(json.dumps({
                "epoch": document["epoch"],
                "t40_complete": document["t40_complete"],
                "unique_active_card": document["unique_active_card"],
                "next_issue_id": document["next_issue_id"],
                "remaining_recipe_gap": document["remaining_recipe_gap"],
                "t40_size": document["sequence"][4].get("size"),
            }))
            return 0
        errors = check()
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        print(f"{t40.relative(OUTPUT)} is current")
        return 0
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"T40 topology failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
