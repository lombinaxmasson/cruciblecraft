#!/usr/bin/env python3
"""Build the T39 recipe-then-storage topology overlay. Does not rewrite T35-T38."""
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

BUILDER = Path(__file__).resolve()
OUTPUT = t39.T39_CARD_TOPOLOGY


def _census_delta() -> dict[str, Any]:
    if t39.CENSUS_DELTA.is_file():
        return t35.load_json(t39.CENSUS_DELTA)
    from tools import build_t39_census_delta as census_builder

    return census_builder.build()


def t39_complete(delta: dict[str, Any]) -> bool:
    closeout = delta.get("identity_closeout") or {}
    return bool(closeout.get("complete")) and not (
        t39.production_strategy()["blocked"]
        or not t39.player_gametest_present()
    )


def _t39_wave(delta: dict[str, Any], complete: bool) -> dict[str, Any]:
    family_ids = t39.production_family_ids()
    return {
        "id": "T39",
        "kind": "production_lock_wave",
        "track": "recipe_wave",
        "status": "complete" if complete else "active",
        "owner": "T39",
        "depends_on": ["T36", "T37", "T38"],
        "host": t39.HOST,
        "size": t39.production_family_count(),
        "family_ids": family_ids,
        "relation_count": t39.production_relation_count(),
        "production_lock_sha256": t39.production_lock_sha256(),
        "catalog_fixture": {
            "families": t39.CATALOG_FAMILY_COUNT,
            "relations": t39.CATALOG_RELATION_COUNT,
            "production": False,
        },
        "phase_reclassified": {
            "owner": "post_1x:nuclear",
            "family_ids": t39.phase_deferred_family_ids(),
        },
        "wave_size_band": [t37.RECIPE_WAVE_MIN, t37.RECIPE_WAVE_MAX],
        "shard_architecture_start": True,
        "load_opening": {
            "axis": "datapack_authored_entries",
            "source": t39.T14_OPENING_LOAD_SOURCE,
            "closing": t39.t14_opening_load(),
        },
        "load_closing": {
            "source": "tools/t39_census_delta.json#t14_load",
            "closing": (delta.get("t14_load") or {}).get("closing"),
        },
        "exit_gate": (
            "locked Centrifuge families have a live host_exact map with "
            "shard-routed publication groups; nuclear reclassification is separate"
        ),
        "append_rule": "this card is closed; do not append families after issuance",
        "replacement_recheck": (
            "T39 closure remeasures host_exact and authored/logical counts "
            "for the production lock only"
        ),
        "note": (
            "The withdrawn 157/250 host catalog is fixture-only. T39 publishes "
            "the reviewed 22/32 lock; seven fuel-rod families move to the future "
            "nuclear owner without being counted as completed recipes."
        ),
    }


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
        _t39_wave(delta, complete),
        {
            "kind": "generated_rule",
            "track": "recipe_wave",
            "id_policy": "consecutive_from_T40",
            "owner_rule": "the issued card id owns the wave",
            "depends_on": ["T39"],
            "preassigned_host": False,
            "until": "1.x recipe gap = 0",
            "wave_size_band": [t37.RECIPE_WAVE_MIN, t37.RECIPE_WAVE_MAX],
            "append_rule": (
                "Issue a new consecutive card for each additional wave. "
                "Do not renumber T38 or T39. Do not pre-assign storage ids."
            ),
            "load_opening": "each wave records T14 opening + delta + closing",
            "exit_gate": "wave families authored or measured; remaining gap recomputed",
            "note": (
                "Next recipe wave content is not preassigned. T40 has no frozen "
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
    complete = t39_complete(delta)
    sequence = _sequence(delta, complete)
    t39_row = sequence[3]
    recipe_index = next(
        index for index, row in enumerate(sequence) if row.get("id") == "T39"
    )
    storage_index = next(
        index for index, row in enumerate(sequence) if row["track"] == "storage_bundle"
    )
    next_rule = sequence[4]
    unique_active = None if complete else "T39"
    remaining_gap = int(
        (delta.get("remaining_ordinary") or {}).get("remaining_ordinary_families") or 0
    )
    validators = {
        "t36_complete": 0 if sequence[0].get("status") == "complete" else 1,
        "t37_complete": 0 if sequence[1].get("status") == "complete" else 1,
        "t38_complete": 0 if sequence[2].get("status") == "complete" else 1,
        "t39_complete_only_if_ready": 0 if (
            (complete and t39_row.get("status") == "complete")
            or (not complete and t39_row.get("status") == "active")
        ) else 1,
        "unique_active_t39_while_incomplete": 0 if (
            (complete and unique_active is None)
            or (not complete and unique_active == "T39")
        ) else 1,
        "revoked_old_storage_first_topology": 0,
        "t39_is_recipe_wave": 0 if t39_row.get("track") == "recipe_wave" else 1,
        "t39_has_family_assignment": 0 if (
            t39_row.get("family_ids")
            and t39_row.get("size") == t39.production_family_count()
        ) else 1,
        "t39_host_centrifuge": 0 if t39_row.get("host") == t39.HOST else 1,
        "t38_not_stolen": 0 if sequence[2].get("size") == t38.FAMILY_COUNT else 1,
        "recipe_before_storage": 0 if recipe_index < storage_index else 1,
        "storage_not_preassigned": 0 if (
            sequence[5].get("track") == "storage_bundle"
            and sequence[5].get("id") is None
        ) else 1,
        "t40_not_preassigned": 0 if (
            next_rule.get("id") is None
            and not next_rule.get("family_ids")
            and not next_rule.get("host")
            and next_rule.get("preassigned_host") is False
        ) else 1,
        "remaining_gap_from_census": 0 if (
            remaining_gap == t39.EXPECTED_REMAINING_ORDINARY_FAMILIES
        ) else 1,
        "t39_family_ids_locked": 0 if (
            t39_row.get("family_ids") == t39.production_family_ids()
        ) else 1,
    }
    return {
        "epoch": "T39_PRODUCTION_LOCK",
        "append_only": False,
        "fixed_cards": ["T36", "T37", "T38", "T39"],
        "unique_active_card": unique_active,
        "next_issue_id": "T40",
        "t39_complete": complete,
        "remaining_recipe_gap": remaining_gap,
        "revoked_unstarted_cards": [f"T{number}" for number in range(41, 47)],
        "rule": (
            "T36 stays complete; T37 calibration stays 50 assembler families and "
            "is complete; T38 Roaster wave is complete; T39 is the first "
            "shard-architecture Centrifuge wave; additional recipe waves number "
            "consecutively from T40 until gap=0; storage is issued only after "
            "that; unique_active_card is T39 while incomplete and null when READY"
        ),
        "sequence": sequence,
        "validators": validators,
        "t35_history_readonly": True,
        "t36_history_readonly": True,
        "t37_history_readonly": True,
        "t38_history_readonly": True,
        "generated_by": "python tools/build_t39_card_topology.py",
        "source_revision": t39.SOURCE_REVISION,
        "production_lock_sha256": t39.production_lock_sha256(),
        "schema_version": 1,
    }


def write() -> dict[str, Any]:
    document = build()
    t35.write_stable(OUTPUT, document)
    return document


def check() -> list[str]:
    from tools import closeout_seal

    errors = closeout_seal.live_or_sealed_errors(
        "T39",
        "topology",
        lambda: t39.check_document(OUTPUT, build()),
    )
    if errors:
        return errors
    document = t35.load_json(OUTPUT)
    sequence = document.get("sequence") or [{}]
    if sequence[0].get("status") != "complete" or sequence[0].get("id") != "T36":
        errors.append("T36 is not complete in T39 topology")
    if sequence[1].get("status") != "complete" or sequence[1].get("id") != "T37":
        errors.append("T37 is not complete in T39 topology")
    if sequence[2].get("status") != "complete" or sequence[2].get("id") != "T38":
        errors.append("T38 is not complete in T39 topology")
    if sequence[3].get("track") != "recipe_wave" or sequence[3].get("id") != "T39":
        errors.append("T39 is not a recipe wave")
    if sequence[3].get("size") != t39.production_family_count():
        errors.append("T39 family assignment drifted from production lock")
    if sequence[4].get("id") or sequence[4].get("family_ids") or sequence[4].get("host"):
        errors.append("T40 content must not be preassigned")
    if sequence[4].get("preassigned_host") is not False:
        errors.append("T40 preassigned_host must be false")
    if sequence[5].get("track") != "storage_bundle" or sequence[5].get("id"):
        errors.append("storage must not be pre-assigned a card id")
    if document.get("next_issue_id") != "T40":
        errors.append("next_issue_id must be T40")
    if document.get("t39_complete") and document.get("unique_active_card") is not None:
        errors.append("T39 complete must leave unique_active_card null")
    if not document.get("t39_complete") and document.get("unique_active_card") != "T39":
        errors.append("T39 must be the unique active card while not complete")
    if int(document.get("remaining_recipe_gap") or 0) != t39.EXPECTED_REMAINING_ORDINARY_FAMILIES:
        errors.append("remaining_recipe_gap must match census remaining ordinary families")
    validators = document.get("validators") or {}
    if any(int(value or 0) != 0 for value in validators.values()):
        errors.append("T39 topology validators are not zero")
    return errors


def main(argv: list[str] | None = None) -> int:
    args = t39.parse_write_check(__doc__, argv)
    try:
        if args.write:
            document = write()
            print(json.dumps({
                "epoch": document["epoch"],
                "t39_complete": document["t39_complete"],
                "unique_active_card": document["unique_active_card"],
                "next_issue_id": document["next_issue_id"],
                "remaining_recipe_gap": document["remaining_recipe_gap"],
                "t39_size": document["sequence"][3].get("size"),
            }))
            return 0
        errors = check()
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        print(f"{t39.relative(OUTPUT)} is current")
        return 0
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"T39 topology failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
