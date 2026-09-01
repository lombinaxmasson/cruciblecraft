#!/usr/bin/env python3
"""Build the T46 recipe-wave topology overlay. Does not rewrite T35-T44 JSON."""
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
from tools import t46_common as t46

OUTPUT = t46.CARD_TOPOLOGY


def _census_delta() -> dict[str, Any]:
    if t46.CENSUS_DELTA.is_file():
        return t35.load_json(t46.CENSUS_DELTA)
    from tools import build_t46_census_delta as census_builder

    return census_builder.build()


def t46_complete(delta: dict[str, Any]) -> bool:
    from tools import closeout_seal

    if closeout_seal.is_sealed("T46"):
        seal = closeout_seal.load_seal("T46")
        return (
            int(seal.get("complete_family_count") or 0) == t46.COMPLETION_DELTA
            and seal.get("gametest_status") == "PASS"
        )
    closeout = delta.get("identity_closeout") or {}
    return bool(closeout.get("complete")) and not (
        t46.production_strategy()["blocked"]
        or not t46.player_gametest_present()
        or not t46.player_path_real()
        or not t46.equivalence_fields_locked()
        or not t46.java_locked_fields_asserted()
        or not t46.locked_support_tree_current()
        or not t46.integrated_load_measured()
    )


def _t46_wave(delta: dict[str, Any], complete: bool) -> dict[str, Any]:
    family_ids = t46.production_family_ids()
    return {
        "id": "T46",
        "kind": "production_lock_wave",
        "track": "recipe_wave",
        "status": "complete" if complete else "active",
        "owner": "T46",
        "depends_on": ["T45"],
        "host": t46.HOST,
        "size": t46.production_family_count(),
        "family_ids": family_ids,
        "relation_count": t46.production_relation_count(),
        "production_lock_sha256": t46.production_lock_sha256(),
        "catalog_fixture": {
            "families": t46.production_family_count(),
            "relations": t46.production_relation_count(),
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
            "source": t46.T14_OPENING_LOAD_SOURCE,
            "closing": t46.t14_opening_load(),
        },
        "load_closing": {
            "source": "tools/t46_census_delta.json#t14_load",
            "closing": (delta.get("t14_load") or {}).get("closing"),
        },
        "exit_gate": (
            "locked block-object families have live host_exact maps with "
            "schema-validated publication-policy manifests"
        ),
        "append_rule": "this card is closed; do not append families after issuance",
        "replacement_recheck": (
            "T46 closure remeasures host_exact and authored/logical counts "
            "for the production lock only"
        ),
        "note": (
            f"{t46.production_family_count()}/{t46.production_relation_count()} is "
            "the Bath MTE production lock. Remaining Bath families and Mixer stay "
            "out. T47 is named only and is not preassigned a host."
        ),
        "preassigned_host": False,
        "preassigned_family_ids": False,
    }


def _t47_placeholder() -> dict[str, Any]:
    return {
        "kind": "generated_rule",
        "track": "recipe_wave",
        "id_policy": "consecutive_from_T47",
        "owner_rule": "the issued card id owns the wave",
        "depends_on": ["T46"],
        "preassigned_host": False,
        "preassigned_family_ids": False,
        "until": "current recipe execution gap = 0",
        "append_rule": (
            "Issue a new consecutive card. Do not renumber T46. Do not "
            "pre-write T47 host or family ids."
        ),
        "note": (
            "T47 is unassigned. Next recipe-wave content is not preassigned."
        ),
    }


def build() -> dict[str, Any]:
    prior = copy.deepcopy(t35.load_json(t46.T45_CARD_TOPOLOGY))
    delta = _census_delta()
    complete = t46_complete(delta)
    sequence = list(prior.get("sequence") or [])
    replaced = False
    for index, row in enumerate(sequence):
        if row.get("id_policy") == "consecutive_from_T46":
            sequence[index] = _t46_wave(delta, complete)
            sequence.insert(index + 1, _t47_placeholder())
            replaced = True
            break
        if row.get("id") == "T46":
            sequence[index] = _t46_wave(delta, complete)
            replaced = True
    if not replaced:
        raise ValueError("T45 topology is missing the unissued T46 placeholder")
    if not any(row.get("id_policy") == "consecutive_from_T47" for row in sequence):
        t46_index = next(
            index for index, row in enumerate(sequence) if row.get("id") == "T46"
        )
        sequence.insert(t46_index + 1, _t47_placeholder())
    unique_active = None if complete else "T46"
    fixed_cards = list(prior.get("fixed_cards") or [])
    if "T46" not in fixed_cards:
        fixed_cards.append("T46")
    document = dict(prior)
    document.update(
        {
            "epoch": "T46_PRODUCTION_LOCK",
            "fixed_cards": fixed_cards,
            "generated_by": "python tools/build_t46_card_topology.py",
            "next_issue_id": "T47",
            "preassigned_family_ids": False,
            "preassigned_host": False,
            "remaining_recipe_gap": (
                t46.CLOSING_EXECUTION_GAP
                if bool((delta.get("identity_closeout") or {}).get("complete"))
                else t46.OPENING_EXECUTION_GAP
            ),
            "rule": (
                "T45 block-object wave stays complete. T46 is the Bath MTE "
                "exact-relation production-lock wave. T47 is consecutive numbering "
                "only and is not preassigned a host or families."
            ),
            "sequence": sequence,
            "t45_history_readonly": True,
            "t46_complete": complete,
            "t46_history_readonly": False,
            "unique_active_card": unique_active,
        }
    )
    t46_row = next(row for row in sequence if row.get("id") == "T46")
    document["validators"] = {
        "t46_complete_only_if_ready": 0 if (
            (complete and t46_row.get("status") == "complete")
            or (not complete and t46_row.get("status") == "active")
        ) else 1,
        "unique_active_t46_while_incomplete": 0 if (
            (complete and unique_active is None)
            or (not complete and unique_active == "T46")
        ) else 1,
        "t46_is_recipe_wave": 0 if t46_row.get("track") == "recipe_wave" else 1,
        "t46_has_family_assignment": 0 if (
            t46_row.get("family_ids")
            and t46_row.get("size") == t46.production_family_count()
        ) else 1,
        "t47_not_preassigned": 0 if (
            any(row.get("id_policy") == "consecutive_from_T47" for row in sequence)
            and document.get("preassigned_host") is False
            and document.get("preassigned_family_ids") is False
        ) else 1,
    }
    return document


def check() -> list[str]:
    from tools import closeout_seal

    errors = closeout_seal.live_or_sealed_errors(
        "T46",
        "topology",
        lambda: t46.check_document(OUTPUT, build()),
    )
    document = t35.load_json(OUTPUT) if OUTPUT.is_file() else {}
    if document.get("next_issue_id") != "T47":
        errors.append("next_issue_id must be T47")
    if document.get("preassigned_host") or document.get("preassigned_family_ids"):
        errors.append("T47 must not be preassigned")
    if document.get("t46_complete") and document.get("unique_active_card") is not None:
        errors.append("T46 complete must leave unique_active_card null")
    if not document.get("t46_complete") and document.get("unique_active_card") != "T46":
        errors.append("T46 must be the unique active card while not complete")
    validators = document.get("validators") or {}
    if any(int(value or 0) != 0 for value in validators.values()):
        errors.append("T46 topology validators are not zero")
    return errors


def main(argv: list[str] | None = None) -> int:
    return t46.run_managed(
        "Write the T46 card topology overlay",
        OUTPUT,
        build=build,
        check=check,
        argv=argv,
    )


if __name__ == "__main__":
    raise SystemExit(main())
