#!/usr/bin/env python3
"""Build the T45 recipe-wave topology overlay. Does not rewrite T35-T44 JSON."""
from __future__ import annotations

import copy
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35
from tools import t37_common as t37
from tools import t44_common as t44
from tools import t45_common as t45

OUTPUT = t45.CARD_TOPOLOGY


def _census_delta() -> dict[str, Any]:
    if t45.CENSUS_DELTA.is_file():
        return t35.load_json(t45.CENSUS_DELTA)
    from tools import build_t45_census_delta as census_builder

    return census_builder.build()


def t45_complete(delta: dict[str, Any]) -> bool:
    closeout = delta.get("identity_closeout") or {}
    return bool(closeout.get("complete")) and not (
        t45.production_strategy()["blocked"]
        or not t45.player_gametest_present()
    )


def _t45_wave(delta: dict[str, Any], complete: bool) -> dict[str, Any]:
    family_ids = t45.production_family_ids()
    return {
        "id": "T45",
        "kind": "production_lock_wave",
        "track": "recipe_wave",
        "status": "complete" if complete else "active",
        "owner": "T45",
        "depends_on": ["T44"],
        "host": t45.HOST,
        "size": t45.production_family_count(),
        "family_ids": family_ids,
        "relation_count": t45.production_relation_count(),
        "production_lock_sha256": t45.production_lock_sha256(),
        "catalog_fixture": {
            "families": t45.production_family_count(),
            "relations": t45.production_relation_count(),
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
            "source": t45.T14_OPENING_LOAD_SOURCE,
            "closing": t45.t14_opening_load(),
        },
        "load_closing": {
            "source": "tools/t45_census_delta.json#t14_load",
            "closing": (delta.get("t14_load") or {}).get("closing"),
        },
        "exit_gate": (
            "locked block-object families have live host_exact maps with "
            "schema-validated publication-policy manifests"
        ),
        "append_rule": "this card is closed; do not append families after issuance",
        "replacement_recheck": (
            "T45 closure remeasures host_exact and authored/logical counts "
            "for the production lock only"
        ),
        "note": (
            f"{t45.production_family_count()}/{t45.production_family_count()} is "
            "the production lock. Centrifuge sands and wildcard-meta families "
            "stay out. T46 is named only and is not preassigned a host."
        ),
        "preassigned_host": False,
        "preassigned_family_ids": False,
    }


def _t46_placeholder() -> dict[str, Any]:
    return {
        "kind": "generated_rule",
        "track": "recipe_wave",
        "id_policy": "consecutive_from_T46",
        "owner_rule": "the issued card id owns the wave",
        "depends_on": ["T45"],
        "preassigned_host": False,
        "preassigned_family_ids": False,
        "until": "current recipe execution gap = 0",
        "append_rule": (
            "Issue a new consecutive card. Do not renumber T45. Do not "
            "pre-write T46 host or family ids."
        ),
        "note": (
            "T46 is unassigned. Next recipe-wave content is not preassigned."
        ),
    }


def build() -> dict[str, Any]:
    prior = copy.deepcopy(t35.load_json(t44.CARD_TOPOLOGY))
    delta = _census_delta()
    complete = t45_complete(delta)
    sequence = list(prior.get("sequence") or [])
    replaced = False
    for index, row in enumerate(sequence):
        if row.get("id_policy") == "consecutive_from_T45":
            sequence[index] = _t45_wave(delta, complete)
            sequence.insert(index + 1, _t46_placeholder())
            replaced = True
            break
        if row.get("id") == "T45":
            sequence[index] = _t45_wave(delta, complete)
            replaced = True
    if not replaced:
        raise ValueError("T44 topology is missing the unissued T45 placeholder")
    if not any(row.get("id_policy") == "consecutive_from_T46" for row in sequence):
        t45_index = next(
            index for index, row in enumerate(sequence) if row.get("id") == "T45"
        )
        sequence.insert(t45_index + 1, _t46_placeholder())
    unique_active = None if complete else "T45"
    document = dict(prior)
    document.update(
        {
            "epoch": "T45_PRODUCTION_LOCK",
            "fixed_cards": list(prior.get("fixed_cards") or []) + ["T45"],
            "generated_by": "python tools/build_t45_card_topology.py",
            "next_issue_id": "T46",
            "preassigned_family_ids": False,
            "preassigned_host": False,
            "remaining_recipe_gap": (
                t45.CLOSING_EXECUTION_GAP if complete else t45.OPENING_EXECUTION_GAP
            ),
            "rule": (
                "T44 Storage bundle stays complete. T45 is the block-object "
                "production-lock wave. T46 is consecutive numbering only and is "
                "not preassigned a host or families."
            ),
            "sequence": sequence,
            "t45_complete": complete,
            "t45_history_readonly": False,
            "unique_active_card": unique_active,
        }
    )
    t45_row = next(row for row in sequence if row.get("id") == "T45")
    document["validators"] = {
        "t45_complete_only_if_ready": 0 if (
            (complete and t45_row.get("status") == "complete")
            or (not complete and t45_row.get("status") == "active")
        ) else 1,
        "unique_active_t45_while_incomplete": 0 if (
            (complete and unique_active is None)
            or (not complete and unique_active == "T45")
        ) else 1,
        "t45_is_recipe_wave": 0 if t45_row.get("track") == "recipe_wave" else 1,
        "t45_has_family_assignment": 0 if (
            t45_row.get("family_ids")
            and t45_row.get("size") == t45.production_family_count()
        ) else 1,
        "t46_not_preassigned": 0 if (
            any(row.get("id_policy") == "consecutive_from_T46" for row in sequence)
            and document.get("preassigned_host") is False
            and document.get("preassigned_family_ids") is False
        ) else 1,
    }
    return document


def check() -> list[str]:
    from tools import closeout_seal

    errors = closeout_seal.live_or_sealed_errors(
        "T45",
        "topology",
        lambda: t45.check_document(OUTPUT, build()),
    )
    document = t35.load_json(OUTPUT) if OUTPUT.is_file() else {}
    if document.get("next_issue_id") != "T46":
        errors.append("next_issue_id must be T46")
    if document.get("preassigned_host") or document.get("preassigned_family_ids"):
        errors.append("T46 must not be preassigned")
    if document.get("t45_complete") and document.get("unique_active_card") is not None:
        errors.append("T45 complete must leave unique_active_card null")
    if not document.get("t45_complete") and document.get("unique_active_card") != "T45":
        errors.append("T45 must be the unique active card while not complete")
    validators = document.get("validators") or {}
    if any(int(value or 0) != 0 for value in validators.values()):
        errors.append("T45 topology validators are not zero")
    return errors


def main(argv: list[str] | None = None) -> int:
    return t45.run_managed(
        "Write the T45 card topology overlay",
        OUTPUT,
        build=build,
        check=check,
        argv=argv,
    )


if __name__ == "__main__":
    raise SystemExit(main())
