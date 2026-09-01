#!/usr/bin/env python3
"""Build the T49 recipe-wave topology overlay. Does not rewrite T35-T46 JSON."""
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
from tools import wave_bath_tiny_purified as tiny_purified

OUTPUT = tiny_purified.CARD_TOPOLOGY


def _census_delta() -> dict[str, Any]:
    if tiny_purified.CENSUS_DELTA.is_file():
        return t35.load_json(tiny_purified.CENSUS_DELTA)
    from tools import build_t49_census_delta as census_builder

    return census_builder.build()


def t49_complete(delta: dict[str, Any]) -> bool:
    from tools import closeout_seal

    if closeout_seal.is_sealed("T49"):
        seal = closeout_seal.load_seal("T49")
        return (
            int(seal.get("complete_family_count") or 0) == tiny_purified.COMPLETION_DELTA
            and seal.get("gametest_status") == "PASS"
        )
    closeout = delta.get("identity_closeout") or {}
    return bool(closeout.get("complete")) and not (
        tiny_purified.production_strategy()["blocked"]
        or not tiny_purified.player_gametest_present()
        or not tiny_purified.player_path_real()
        or not tiny_purified.equivalence_fields_locked()
        or not tiny_purified.java_locked_fields_asserted()
        or not tiny_purified.locked_support_tree_current()
        or not tiny_purified.integrated_load_measured()
    )


def _t49_wave(delta: dict[str, Any], complete: bool) -> dict[str, Any]:
    family_ids = tiny_purified.production_family_ids()
    return {
        "id": "T49",
        "kind": "production_lock_wave",
        "track": "recipe_wave",
        "status": "complete" if complete else "active",
        "owner": "T49",
        "depends_on": ["T48"],
        "host": tiny_purified.HOST,
        "size": tiny_purified.production_family_count(),
        "family_ids": family_ids,
        "relation_count": tiny_purified.production_relation_count(),
        "production_lock_sha256": tiny_purified.production_lock_sha256(),
        "catalog_fixture": {
            "families": tiny_purified.production_family_count(),
            "relations": tiny_purified.production_relation_count(),
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
            "source": tiny_purified.T14_OPENING_LOAD_SOURCE,
            "closing": tiny_purified.t14_opening_load(),
        },
        "load_closing": {
            "source": "tools/t49_census_delta.json#t14_load",
            "closing": (delta.get("t14_load") or {}).get("closing"),
        },
        "exit_gate": (
            "locked remainder families have live host_exact maps with "
            "schema-validated publication-policy manifests"
        ),
        "append_rule": "this card is closed; do not append families after issuance",
        "replacement_recheck": (
            "T49 closure remeasures host_exact and authored/logical counts "
            "for the production lock only"
        ),
        "note": (
            f"{tiny_purified.production_family_count()}/{tiny_purified.production_relation_count()} is "
            "the Bath tiny-purified remainder production lock after recycling "
            "correction. Mixer stays out. T50 is named only and is not "
            "preassigned a host."
        ),
        "preassigned_host": False,
        "preassigned_family_ids": False,
    }


def _t50_placeholder() -> dict[str, Any]:
    return {
        "kind": "generated_rule",
        "track": "recipe_wave",
        "id_policy": "consecutive_from_T50",
        "owner_rule": "the issued card id owns the wave",
        "depends_on": ["T49"],
        "preassigned_host": False,
        "preassigned_family_ids": False,
        "until": "current recipe execution gap = 0",
        "append_rule": (
            "Issue a new consecutive card. Do not renumber T49. Do not "
            "pre-write T50 host or family ids."
        ),
        "note": (
            "T50 is unassigned. Mixer is not preassigned. Next recipe-wave "
            "content is not preassigned."
        ),
    }


def build() -> dict[str, Any]:
    prior = copy.deepcopy(t35.load_json(tiny_purified.T48_CARD_TOPOLOGY))
    delta = _census_delta()
    complete = t49_complete(delta)
    sequence = list(prior.get("sequence") or [])
    replaced = False
    for index, row in enumerate(sequence):
        if row.get("id_policy") == "consecutive_from_T49":
            sequence[index] = _t49_wave(delta, complete)
            sequence.insert(index + 1, _t50_placeholder())
            replaced = True
            break
        if row.get("id") == "T49":
            sequence[index] = _t49_wave(delta, complete)
            replaced = True
    if not replaced:
        raise ValueError("T48 topology is missing the unissued T49 placeholder")
    if not any(row.get("id_policy") == "consecutive_from_T50" for row in sequence):
        t49_index = next(
            index for index, row in enumerate(sequence) if row.get("id") == "T49"
        )
        sequence.insert(t49_index + 1, _t50_placeholder())
    unique_active = None if complete else "T49"
    fixed_cards = list(prior.get("fixed_cards") or [])
    if "T49" not in fixed_cards:
        fixed_cards.append("T49")
    document = dict(prior)
    document.update(
        {
            "epoch": "T49_PRODUCTION_LOCK",
            "fixed_cards": fixed_cards,
            "generated_by": "python tools/build_t49_card_topology.py",
            "next_issue_id": "T50",
            "preassigned_family_ids": False,
            "preassigned_host": False,
            "remaining_recipe_gap": (
                tiny_purified.CLOSING_EXECUTION_GAP
                if bool((delta.get("identity_closeout") or {}).get("complete"))
                else tiny_purified.OPENING_EXECUTION_GAP
            ),
            "rule": (
                "T46, T47, and T48 Bath waves stay complete. T49 is the Bath "
                "tiny-purified remainder wave after recycling correction. T50 "
                "is consecutive numbering only and is not preassigned Mixer."
            ),
            "sequence": sequence,
            "t45_history_readonly": True,
            "t46_history_readonly": True,
            "t46_complete": True,
            "t47_complete": True,
            "t47_history_readonly": True,
            "t48_complete": True,
            "t48_history_readonly": True,
            "t49_complete": complete,
            "t49_history_readonly": False,
            "unique_active_card": unique_active,
        }
    )
    t49_row = next(row for row in sequence if row.get("id") == "T49")
    document["validators"] = {
        "t49_complete_only_if_ready": 0 if (
            (complete and t49_row.get("status") == "complete")
            or (not complete and t49_row.get("status") == "active")
        ) else 1,
        "unique_active_t49_while_incomplete": 0 if (
            (complete and unique_active is None)
            or (not complete and unique_active == "T49")
        ) else 1,
        "t49_is_recipe_wave": 0 if t49_row.get("track") == "recipe_wave" else 1,
        "t49_has_family_assignment": 0 if (
            t49_row.get("family_ids")
            and t49_row.get("size") == tiny_purified.production_family_count()
        ) else 1,
        "t49_not_preassigned": 0 if (
            any(row.get("id_policy") == "consecutive_from_T50" for row in sequence)
            and document.get("preassigned_host") is False
            and document.get("preassigned_family_ids") is False
        ) else 1,
    }
    return document


def check() -> list[str]:
    from tools import closeout_seal

    errors = closeout_seal.live_or_sealed_errors(
        "T49",
        "topology",
        lambda: tiny_purified.check_document(OUTPUT, build()),
    )
    document = t35.load_json(OUTPUT) if OUTPUT.is_file() else {}
    if document.get("next_issue_id") != "T50":
        errors.append("next_issue_id must be T50")
    if document.get("preassigned_host") or document.get("preassigned_family_ids"):
        errors.append("T49 must not be preassigned")
    if document.get("t49_complete") and document.get("unique_active_card") is not None:
        errors.append("T49 complete must leave unique_active_card null")
    if not document.get("t49_complete") and document.get("unique_active_card") != "T49":
        errors.append("T49 must be the unique active card while not complete")
    validators = document.get("validators") or {}
    if any(int(value or 0) != 0 for value in validators.values()):
        errors.append("T49 topology validators are not zero")
    return errors


def main(argv: list[str] | None = None) -> int:
    return tiny_purified.run_managed(
        "Write the T49 card topology overlay",
        OUTPUT,
        build=build,
        check=check,
        argv=argv,
    )


if __name__ == "__main__":
    raise SystemExit(main())
