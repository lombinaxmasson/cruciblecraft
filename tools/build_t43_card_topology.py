#!/usr/bin/env python3
"""Build the T43 recipe-wave topology overlay. Does not rewrite T35-T42."""
from __future__ import annotations

import copy
import json
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from tools import t35_common as t35  # noqa: E402
from tools import t37_common as t37  # noqa: E402
from tools import t43_common as t43  # noqa: E402

OUTPUT = t43.T43_CARD_TOPOLOGY


def _census_delta() -> dict[str, Any]:
    if t43.CENSUS_DELTA.is_file():
        return t35.load_json(t43.CENSUS_DELTA)
    from tools import build_t43_census_delta as census_builder

    return census_builder.build()


def t43_complete(delta: dict[str, Any]) -> bool:
    closeout = delta.get("identity_closeout") or {}
    return bool(closeout.get("complete")) and not (
        t43.production_strategy()["blocked"]
        or not t43.player_gametest_present()
    )


def _t43_wave(delta: dict[str, Any], complete: bool) -> dict[str, Any]:
    family_ids = t43.production_family_ids()
    return {
        "id": "T43",
        "kind": "production_lock_wave",
        "track": "recipe_wave",
        "status": "complete" if complete else "active",
        "owner": "T43",
        "depends_on": ["T42", "T42-Repair", "T42-Owner"],
        "host": t43.HOST,
        "size": t43.production_family_count(),
        "family_ids": family_ids,
        "relation_count": t43.production_relation_count(),
        "production_lock_sha256": t43.production_lock_sha256(),
        "catalog_fixture": {
            "families": t43.CATALOG_FAMILY_COUNT,
            "relations": t43.CATALOG_RELATION_COUNT,
            "production": False,
        },
        "phase_deferred": {
            "owner": None,
            "family_ids": [],
            "reason": None,
        },
        "wave_size_band": [t37.RECIPE_WAVE_MIN, t37.RECIPE_WAVE_MAX],
        "load_opening": {
            "axis": "datapack_authored_entries",
            "source": t43.T14_OPENING_LOAD_SOURCE,
            "closing": t43.t14_opening_load(),
        },
        "load_closing": {
            "source": "tools/t43_census_delta.json#t14_load",
            "closing": (delta.get("t14_load") or {}).get("closing"),
        },
        "exit_gate": (
            "locked Smelter stone families have a live host_exact map with "
            "a schema-validated publication-policy manifest"
        ),
        "append_rule": "this card is closed; do not append families after issuance",
        "replacement_recheck": (
            "T43 closure remeasures host_exact and authored/logical counts "
            "for the production lock only"
        ),
        "note": (
            "407/407 is the production lock. Catalog fixture equals production. "
            "T44 is named only and is not preassigned a host."
        ),
    }


def _t44_placeholder() -> dict[str, Any]:
    return {
        "kind": "generated_rule",
        "track": "recipe_wave",
        "id_policy": "consecutive_from_T44",
        "owner_rule": "the issued card id owns the wave",
        "depends_on": ["T43"],
        "preassigned_host": False,
        "preassigned_family_ids": False,
        "until": "current recipe execution gap = 0",
        "wave_size_band": [t37.RECIPE_WAVE_MIN, t37.RECIPE_WAVE_MAX],
        "append_rule": (
            "Issue a new consecutive card for each additional wave. "
            "Do not renumber T43. Do not pre-assign storage ids. "
            "T36-Repair is issued after T43 closeout and does not occupy T44."
        ),
        "load_opening": "each wave records T14 opening + delta + closing",
        "exit_gate": "wave families authored or measured; remaining gap recomputed",
        "note": (
            "Next recipe wave content is not preassigned. T44 has no frozen "
            "family_ids or host assignment until issued."
        ),
    }


def build() -> dict[str, Any]:
    delta = _census_delta()
    complete = t43_complete(delta)
    prior = copy.deepcopy(t35.load_json(t43.T42_CARD_TOPOLOGY))
    sequence = list(prior.get("sequence") or [])
    replaced = False
    for index, row in enumerate(sequence):
        if row.get("id") == "T42-Owner":
            row = dict(row)
            row["status"] = "complete"
            row["note"] = (
                "T42-Owner is complete. It does not occupy T44 and does not "
                "publish recipes."
            )
            sequence[index] = row
        if row.get("id") == "T43" and row.get("kind") == "generated_rule":
            sequence[index] = _t43_wave(delta, complete)
            sequence.insert(index + 1, _t44_placeholder())
            replaced = True
            break
    if not replaced:
        raise ValueError("T42 topology is missing the unissued T43 placeholder")
    t43_row = next(row for row in sequence if row.get("id") == "T43")
    t44_row = next(
        row for row in sequence
        if row.get("id_policy") == "consecutive_from_T44"
    )
    storage = next(row for row in sequence if row.get("track") == "storage_bundle")
    unique_active = None if complete else "T43"
    remaining_gap = int(
        (delta.get("remaining_ordinary") or {}).get("remaining_ordinary_families") or 0
    )
    validators = {
        "t42_owner_complete": 0 if next(
            row for row in sequence if row.get("id") == "T42-Owner"
        ).get("status") == "complete" else 1,
        "t43_complete_only_if_ready": 0 if (
            (complete and t43_row.get("status") == "complete")
            or (not complete and t43_row.get("status") == "active")
        ) else 1,
        "unique_active_t43_while_incomplete": 0 if (
            (complete and unique_active is None)
            or (not complete and unique_active == "T43")
        ) else 1,
        "t43_is_recipe_wave": 0 if t43_row.get("track") == "recipe_wave" else 1,
        "t43_has_family_assignment": 0 if (
            t43_row.get("family_ids")
            and t43_row.get("size") == t43.production_family_count()
        ) else 1,
        "t43_host_smelter": 0 if t43_row.get("host") == t43.HOST else 1,
        "t44_not_preassigned": 0 if (
            t44_row.get("id") is None
            and not t44_row.get("family_ids")
            and not t44_row.get("host")
            and t44_row.get("preassigned_host") is False
            and t44_row.get("preassigned_family_ids") is False
        ) else 1,
        "storage_not_preassigned": 0 if storage.get("id") is None else 1,
        "t36_repair_does_not_occupy_t44": 0 if (
            "T36-Repair" not in {row.get("id") for row in sequence}
        ) else 1,
        "remaining_gap_from_census": 0 if (
            remaining_gap == (
                t43.EXPECTED_REMAINING_ORDINARY_FAMILIES if complete
                else t43.OPENING_EXECUTION_GAP
            )
        ) else 1,
        "t43_family_ids_locked": 0 if (
            t43_row.get("family_ids") == t43.production_family_ids()
        ) else 1,
    }
    return {
        "epoch": "T43_PRODUCTION_LOCK",
        "append_only": False,
        "fixed_cards": ["T36", "T37", "T38", "T39", "T40", "T41", "T42", "T43"],
        "unique_active_card": unique_active,
        "next_issue_id": "T44",
        "preassigned_host": False,
        "preassigned_family_ids": False,
        "t43_complete": complete,
        "remaining_recipe_gap": remaining_gap,
        "rule": (
            "T42 partition/repair/owner stay complete and own zero families. "
            "T43 is the Smelter stone production-lock wave. T44 is consecutive "
            "numbering only. T36-Repair is issued after this closeout and does "
            "not occupy T44. Storage 28/624 is not preassigned."
        ),
        "sequence": sequence,
        "validators": validators,
        "t35_history_readonly": True,
        "t36_history_readonly": True,
        "t37_history_readonly": True,
        "t38_history_readonly": True,
        "t39_history_readonly": True,
        "t40_history_readonly": True,
        "t41_history_readonly": True,
        "t42_history_readonly": True,
        "generated_by": "python tools/build_t43_card_topology.py",
        "source_revision": t43.SOURCE_REVISION,
        "production_lock_sha256": t43.production_lock_sha256(),
        "schema_version": 1,
    }


def write() -> dict[str, Any]:
    document = build()
    t35.write_stable(OUTPUT, document)
    return document


def check() -> list[str]:
    from tools import closeout_seal

    errors = closeout_seal.live_or_sealed_errors(
        "T43",
        "topology",
        lambda: t43.check_document(OUTPUT, build()),
    )
    if errors:
        return errors
    document = t35.load_json(OUTPUT)
    if document.get("next_issue_id") != "T44":
        errors.append("next_issue_id must be T44")
    if document.get("preassigned_host") is not False:
        errors.append("preassigned_host must be false")
    if document.get("preassigned_family_ids") is not False:
        errors.append("preassigned_family_ids must be false")
    if document.get("t43_complete") and document.get("unique_active_card") is not None:
        errors.append("T43 complete must leave unique_active_card null")
    if not document.get("t43_complete") and document.get("unique_active_card") != "T43":
        errors.append("T43 must be the unique active card while not complete")
    expected_gap = (
        t43.EXPECTED_REMAINING_ORDINARY_FAMILIES
        if document.get("t43_complete")
        else t43.OPENING_EXECUTION_GAP
    )
    if int(document.get("remaining_recipe_gap") or 0) != expected_gap:
        errors.append("remaining_recipe_gap must match census remaining ordinary families")
    validators = document.get("validators") or {}
    if any(int(value or 0) != 0 for value in validators.values()):
        errors.append("T43 topology validators are not zero")
    return errors


def main(argv: list[str] | None = None) -> int:
    args = t43.parse_write_check(__doc__, argv)
    try:
        if args.write:
            document = write()
            print(json.dumps({
                "epoch": document["epoch"],
                "t43_complete": document["t43_complete"],
                "unique_active_card": document["unique_active_card"],
                "next_issue_id": document["next_issue_id"],
                "remaining_recipe_gap": document["remaining_recipe_gap"],
            }))
            return 0
        errors = check()
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        print(f"{t43.relative(OUTPUT)} is current")
        return 0
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"T43 topology failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
