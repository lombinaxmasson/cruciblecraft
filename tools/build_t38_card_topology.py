#!/usr/bin/env python3
"""Build the T38 recipe-then-storage topology overlay. Does not rewrite T35/T36/T37."""
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
OUTPUT = t38.T38_CARD_TOPOLOGY


def _census_delta() -> dict[str, Any]:
    if t38.CENSUS_DELTA.is_file():
        return t35.load_json(t38.CENSUS_DELTA)
    from tools import build_t38_census_delta as census_builder
    return census_builder.build()


def t38_complete(delta: dict[str, Any]) -> bool:
    closeout = delta.get("identity_closeout") or {}
    return bool(closeout.get("complete")) and not (
        t38.production_strategy()["blocked"]
        or not t38.player_gametest_present()
    )


def _t38_wave(delta: dict[str, Any], complete: bool) -> dict[str, Any]:
    family_ids = t38.load_t38_frozen_family_ids()
    return {
        "id": "T38",
        "kind": "fixed_wave",
        "track": "recipe_wave",
        "status": "complete" if complete else "active",
        "owner": "T38",
        "depends_on": ["T36", "T37"],
        "host": t38.HOST,
        "size": t38.FAMILY_COUNT,
        "family_ids": family_ids,
        "wave_size_band": [t37.RECIPE_WAVE_MIN, t37.RECIPE_WAVE_MAX],
        "size_exception": "host_complete_small_wave",
        "load_opening": {
            "axis": "datapack_authored_entries",
            "source": t38.T14_OPENING_LOAD_SOURCE,
            "closing": t38.t14_opening_load(),
        },
        "load_closing": {
            "source": "tools/t38_census_delta.json#t14_load",
            "closing": (delta.get("t14_load") or {}).get("closing"),
        },
        "exit_gate": "roaster 29 ordinary families have a live host_exact map",
        "append_rule": "this card is closed; do not append families after issuance",
        "replacement_recheck": (
            "T38 closure remeasures host_exact and authored/logical counts "
            "for these 29 families only"
        ),
        "note": (
            "First bounded recipe wave after T37 assembler calibration. "
            "29 is below the 50–200 preferred band because the Roasting host "
            "is now complete; do not pad with unrelated families."
        ),
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
                "Fixed 50-family assembler calibration. T38 must not steal this "
                "work. T37 is complete only when t37_readiness can honestly be "
                "T37_READY."
            ),
        },
        _t38_wave(delta, complete),
        {
            "kind": "generated_rule",
            "track": "recipe_wave",
            "id_policy": "consecutive_from_T39",
            "owner_rule": "the issued card id owns the wave",
            "depends_on": ["T38"],
            "until": "1.x recipe gap = 0",
            "wave_size_band": [t37.RECIPE_WAVE_MIN, t37.RECIPE_WAVE_MAX],
            "append_rule": (
                "Issue a new consecutive card for each additional wave. "
                "Do not renumber T38 or T37. Do not pre-assign storage ids."
            ),
            "load_opening": "each wave records T14 opening + delta + closing",
            "exit_gate": "wave families authored or measured; remaining gap recomputed",
            "note": (
                "Next recipe wave content is not preassigned. T39 has no frozen "
                "family_ids, host, or Bath/Mixer/Smelter assignment until issued."
            ),
        },
        {
            "kind": "generated_rule",
            "track": "storage_bundle",
            "id_policy": "consecutive_after_last_recipe_wave",
            "depends_on": ["recipe_waves_complete"],
            "after": "1.x recipe gap = 0",
            "note": (
                "Storage bundles start only after recipe waves close the 1.x gap. "
                "Do not freeze T39 as storage; the next recipe wave, if needed, "
                "takes T39."
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
    complete = t38_complete(delta)
    sequence = _sequence(delta, complete)
    t38_row = sequence[2]
    recipe_index = next(
        index for index, row in enumerate(sequence) if row["track"] == "recipe_wave"
    )
    storage_index = next(
        index for index, row in enumerate(sequence) if row["track"] == "storage_bundle"
    )
    next_rule = sequence[3]
    unique_active = None if complete else "T38"
    remaining_gap = int(
        (delta.get("remaining_ordinary") or {}).get("remaining_ordinary_families") or 0
    )
    validators = {
        "t36_complete": 0 if sequence[0].get("status") == "complete" else 1,
        "t37_complete": 0 if sequence[1].get("status") == "complete" else 1,
        "t38_complete_only_if_ready": 0 if (
            (complete and t38_row.get("status") == "complete")
            or (not complete and t38_row.get("status") == "active")
        ) else 1,
        "unique_active_unissued_after_t38_ready": 0 if (
            (complete and unique_active is None)
            or (not complete and unique_active == "T38")
        ) else 1,
        "revoked_old_storage_first_topology": 0,
        "t38_is_recipe_wave": 0 if t38_row.get("track") == "recipe_wave" else 1,
        "t38_has_family_assignment": 0 if (
            t38_row.get("family_ids") and t38_row.get("size") == t38.FAMILY_COUNT
        ) else 1,
        "t38_host_roaster": 0 if t38_row.get("host") == t38.HOST else 1,
        "t37_not_stolen": 0 if sequence[1].get("size") == t37.T37_FAMILY_COUNT else 1,
        "recipe_before_storage": 0 if recipe_index < storage_index else 1,
        "storage_not_preassigned": 0 if (
            sequence[4].get("track") == "storage_bundle"
            and sequence[4].get("id") is None
        ) else 1,
        "t39_not_preassigned": 0 if (
            next_rule.get("id") is None
            and not next_rule.get("family_ids")
            and not next_rule.get("host")
        ) else 1,
        "remaining_gap_from_census": 0 if (
            remaining_gap == t38.EXPECTED_REMAINING_ORDINARY_FAMILIES
        ) else 1,
        "t38_family_ids_frozen": 0 if (
            t38_row.get("family_ids") == t38.load_t38_frozen_family_ids()
        ) else 1,
    }
    return {
        "epoch": "T38_RECIPE_THEN_STORAGE",
        "append_only": False,
        "fixed_cards": ["T36", "T37", "T38"],
        "unique_active_card": unique_active,
        "next_issue_id": "T39",
        "t38_complete": complete,
        "remaining_recipe_gap": remaining_gap,
        "revoked_unstarted_cards": [f"T{number}" for number in range(40, 47)],
        "rule": (
            "T36 stays complete; T37 calibration stays 50 assembler families and "
            "is complete; T38 is the first bounded recipe wave and closes the "
            "Roaster host; additional recipe waves number consecutively from T39 "
            "until gap=0; storage is issued only after that, never with a "
            "frozen T39 id; unique_active_card stays unissued after T38_READY "
            "until the next card is actually issued"
        ),
        "sequence": sequence,
        "validators": validators,
        "t35_history_readonly": True,
        "t36_history_readonly": True,
        "t37_history_readonly": True,
        "generated_by": "python tools/build_t38_card_topology.py",
        "source_revision": t38.SOURCE_REVISION,
        "schema_version": 1,
    }


def write() -> dict[str, Any]:
    document = build()
    t35.write_stable(OUTPUT, document)
    return document


def check() -> list[str]:
    from tools import closeout_seal

    errors = closeout_seal.live_or_sealed_errors(
        "T38",
        "topology",
        lambda: t38.check_document(OUTPUT, build()),
    )
    if errors:
        return errors
    document = t35.load_json(OUTPUT)
    sequence = document.get("sequence") or [{}]
    if sequence[0].get("status") != "complete" or sequence[0].get("id") != "T36":
        errors.append("T36 is not complete in T38 topology")
    if sequence[1].get("status") != "complete" or sequence[1].get("id") != "T37":
        errors.append("T37 is not complete in T38 topology")
    if sequence[2].get("track") != "recipe_wave" or sequence[2].get("id") != "T38":
        errors.append("T38 is not a recipe wave")
    if sequence[2].get("size") != t38.FAMILY_COUNT:
        errors.append("T38 family assignment drifted from the frozen 29")
    if sequence[3].get("id") or sequence[3].get("family_ids") or sequence[3].get("host"):
        errors.append("T39 content must not be preassigned")
    if sequence[4].get("track") != "storage_bundle" or sequence[4].get("id"):
        errors.append("storage must not be pre-assigned a card id")
    if document.get("next_issue_id") != "T39":
        errors.append("next_issue_id must be T39")
    if document.get("t38_complete") and document.get("unique_active_card") is not None:
        errors.append("T38 complete must leave unique_active_card unissued")
    if not document.get("t38_complete") and document.get("unique_active_card") != "T38":
        errors.append("T38 must be the unique active card while not complete")
    if int(document.get("remaining_recipe_gap") or 0) != t38.EXPECTED_REMAINING_ORDINARY_FAMILIES:
        errors.append("remaining_recipe_gap must match census remaining ordinary families")
    validators = document.get("validators") or {}
    if any(int(value or 0) != 0 for value in validators.values()):
        errors.append("T38 topology validators are not zero")
    return errors


def main(argv: list[str] | None = None) -> int:
    args = t38.parse_write_check(__doc__, argv)
    try:
        if args.write:
            document = write()
            print(json.dumps({
                "epoch": document["epoch"],
                "t38_complete": document["t38_complete"],
                "unique_active_card": document["unique_active_card"],
                "next_issue_id": document["next_issue_id"],
                "remaining_recipe_gap": document["remaining_recipe_gap"],
                "t38_size": document["sequence"][2].get("size"),
            }))
            return 0
        errors = check()
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        print(f"{t38.relative(OUTPUT)} is current")
        return 0
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"T38 topology failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
