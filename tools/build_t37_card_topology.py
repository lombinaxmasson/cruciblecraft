#!/usr/bin/env python3
"""Build the T37 recipe-then-storage topology overlay. Does not rewrite T35/T36."""
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
OUTPUT = t37.CARD_TOPOLOGY


def _census_delta() -> dict[str, Any]:
    if t37.CENSUS_DELTA.is_file():
        return t35.load_json(t37.CENSUS_DELTA)
    from tools import build_t37_census_delta as census_builder
    return census_builder.build()


def t37_complete(delta: dict[str, Any]) -> bool:
    closeout = delta.get("identity_closeout") or {}
    return bool(closeout.get("complete")) and not (
        t37.production_strategy()["blocked"]
        or not t37.player_gametest_present()
    )


def _roaster_wave(delta: dict[str, Any], complete: bool) -> dict[str, Any]:
    t38 = t37.t38_frozen_wave()
    return {
        "id": "T38",
        "kind": "fixed_wave",
        "track": "recipe_wave",
        "status": "next_active_candidate" if complete else "next_candidate",
        "owner": "T38",
        "depends_on": ["T36", "T37"],
        "host": t38["host"],
        "size": t38["size"],
        "wave_size_band": [t37.RECIPE_WAVE_MIN, t37.RECIPE_WAVE_MAX],
        "size_exception": "host_complete_small_wave",
        "family_ids": t38["family_ids"],
        "load_opening": {
            "axis": "datapack_authored_entries",
            "source": "tools/t37_census_delta.json#t14_load",
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
            "is now complete; do not pad with unrelated families. T38 becomes "
            "the unique active content card only after T37_READY."
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
            "status": "complete" if complete else "active",
            "owner": "T37",
            "host": t37.HOST,
            "size": t37.T37_FAMILY_COUNT,
            "note": (
                "Fixed 50-family assembler calibration. T38 must not steal this "
                "work. T37 is complete only when t37_readiness can honestly be "
                "T37_READY."
            ),
        },
        _roaster_wave(delta, complete),
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
    complete = t37_complete(delta)
    sequence = _sequence(delta, complete)
    t38 = sequence[2]
    recipe_index = next(
        index for index, row in enumerate(sequence) if row["track"] == "recipe_wave"
    )
    storage_index = next(
        index for index, row in enumerate(sequence) if row["track"] == "storage_bundle"
    )
    unique_active = "T38" if complete else "T37"
    validators = {
        "t36_complete": 0 if sequence[0].get("status") == "complete" else 1,
        "t36_t37_fixed": 0 if {sequence[0].get("id"), sequence[1].get("id")} == {"T36", "T37"} else 1,
        "t37_complete_only_if_ready": 0 if (
            (complete and sequence[1].get("status") == "complete")
            or (not complete and sequence[1].get("status") == "active")
        ) else 1,
        "t38_not_unique_active_before_t37_ready": 0 if (
            unique_active != "T38" or complete
        ) else 1,
        "revoked_old_storage_first_topology": 0,
        "new_t38_is_recipe_wave": 0 if t38.get("track") == "recipe_wave" else 1,
        "t38_has_family_assignment": 0 if (
            t38.get("family_ids") and t38.get("size") == t37.T38_FAMILY_COUNT
        ) else 1,
        "t38_host_roaster": 0 if t38.get("host") == t37.T38_HOST else 1,
        "t38_has_owner_and_budget": 0 if (
            t38.get("owner") == "T38"
            and (t38.get("load_opening") or {}).get("source")
            == "tools/t37_census_delta.json#t14_load"
        ) else 1,
        "t37_not_stolen": 0 if sequence[1].get("size") == t37.T37_FAMILY_COUNT else 1,
        "recipe_before_storage": 0 if recipe_index < storage_index else 1,
        "storage_not_preassigned_t39": 0 if (
            sequence[4].get("track") == "storage_bundle"
            and sequence[4].get("id") is None
        ) else 1,
        "t38_family_ids_frozen": 0 if (
            t38.get("family_ids") == t37.t38_frozen_wave()["family_ids"]
        ) else 1,
    }
    return {
        "epoch": "T37_RECIPE_THEN_STORAGE",
        "append_only": False,
        "fixed_cards": ["T36", "T37"],
        "unique_active_card": unique_active,
        "t37_complete": complete,
        "revoked_unstarted_cards": list(t37.REVOKED),
        "rule": (
            "T36 stays complete; T37 calibration stays 50 assembler families and "
            "is complete only when T37_READY; T38 is the first bounded recipe "
            "wave and becomes the unique active content card only after T37 "
            "closes; additional recipe waves number consecutively from T39 "
            "until gap=0; storage is issued only after that, never with a "
            "frozen T39 id"
        ),
        "sequence": sequence,
        "validators": validators,
        "t35_history_readonly": True,
        "t36_history_readonly": True,
        "generated_by": "python tools/build_t37_card_topology.py",
        "source_revision": t37.SOURCE_REVISION,
        "schema_version": 1,
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
    sequence = document.get("sequence") or [{}]
    if sequence[0].get("status") != "complete" or sequence[0].get("id") != "T36":
        errors.append("T36 is not complete in T37 topology")
    if sequence[2].get("track") != "recipe_wave" or sequence[2].get("id") != "T38":
        errors.append("new T38 is not a recipe wave")
    if sequence[2].get("size") != t37.T38_FAMILY_COUNT:
        errors.append("T38 family assignment drifted from the frozen 29")
    if sequence[4].get("track") != "storage_bundle" or sequence[4].get("id"):
        errors.append("storage must not be pre-assigned a T39 card id")
    if document.get("t37_complete") and document.get("unique_active_card") != "T38":
        errors.append("T37 complete must hand unique-active to T38")
    if not document.get("t37_complete") and document.get("unique_active_card") == "T38":
        errors.append("T38 must not become the unique active card before T37_READY")
    validators = document.get("validators") or {}
    if any(int(value or 0) != 0 for value in validators.values()):
        errors.append("T37 topology validators are not zero")
    return errors


def main(argv: list[str] | None = None) -> int:
    args = t37.parse_write_check(__doc__, argv)
    try:
        if args.write:
            document = write()
            print(json.dumps({
                "epoch": document["epoch"],
                "t37_complete": document["t37_complete"],
                "unique_active_card": document["unique_active_card"],
                "t38_size": document["sequence"][2].get("size"),
            }))
            return 0
        errors = check()
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        print(f"{t37.relative(OUTPUT)} is current")
        return 0
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"T37 topology failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
