#!/usr/bin/env python3
"""Build the T44 storage topology overlay. Does not rewrite T35-T43 JSON."""
from __future__ import annotations

import copy
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35  # noqa: E402
from tools import t44_common as common  # noqa: E402

OUTPUT = common.CARD_TOPOLOGY


def _t44_card() -> dict[str, Any]:
    return {
        "id": "T44",
        "kind": "storage_bundle",
        "track": "storage_bundle",
        "status": "complete",
        "owner": "T44",
        "depends_on": ["T43", "T36-Repair"],
        "host": common.HOST,
        "storage_source_sites": common.STORAGE_SOURCE_SITES,
        "storage_registrations": common.STORAGE_REGISTRATIONS,
        "logistics_source_sites": common.LOGISTICS_SOURCE_SITES,
        "logistics_registrations": common.LOGISTICS_REGISTRATIONS,
        "recipe_completion_delta": common.COMPLETION_DELTA,
        "preassigned_host": False,
        "preassigned_family_ids": False,
        "exit_gate": "storage 28/624 and logistics 1/1 closed with measured load",
        "append_rule": "this card is closed; do not append unfrozen storage variants",
        "note": (
            "T44 is the Storage bundle. Recipe gap stays 3076. T45 is not "
            "preassigned a host or family ids."
        ),
    }


def _t45_placeholder() -> dict[str, Any]:
    return {
        "kind": "generated_rule",
        "track": "recipe_wave",
        "id_policy": "consecutive_from_T45",
        "owner_rule": "the issued card id owns the wave",
        "depends_on": ["T44"],
        "preassigned_host": False,
        "preassigned_family_ids": False,
        "until": "current recipe execution gap = 0",
        "append_rule": (
            "Issue a new consecutive card. Do not renumber T44. Do not "
            "pre-write Bath or T45 family ids."
        ),
        "note": (
            "T45 is unassigned. Next recipe-wave content is not preassigned."
        ),
    }


def build() -> dict[str, Any]:
    prior = copy.deepcopy(t35.load_json(common.T43_CARD_TOPOLOGY))
    sequence = list(prior.get("sequence") or [])
    replaced = False
    for index, row in enumerate(sequence):
        if row.get("id_policy") == "consecutive_from_T44":
            sequence[index] = _t44_card()
            sequence.insert(index + 1, _t45_placeholder())
            replaced = True
            break
    if not replaced:
        existing = next((row for row in sequence if row.get("id") == "T44"), None)
        if existing is None:
            raise ValueError("T43 topology is missing the unissued T44 placeholder")
        sequence = [
            _t44_card() if row.get("id") == "T44" else row for row in sequence
        ]
        if not any(row.get("id_policy") == "consecutive_from_T45" for row in sequence):
            t44_index = next(
                index for index, row in enumerate(sequence) if row.get("id") == "T44"
            )
            sequence.insert(t44_index + 1, _t45_placeholder())
    for row in sequence:
        if row.get("id") == "T43":
            row["note"] = (
                "407/407 is the production lock. Catalog fixture equals "
                "production. T44 Storage bundle is complete and does not "
                "change the 3076 recipe gap."
            )
    document = dict(prior)
    document.update(
        {
            "epoch": "T44_STORAGE_BUNDLE",
            "fixed_cards": list(prior.get("fixed_cards") or []) + ["T44"],
            "generated_by": "python tools/build_t44_card_topology.py",
            "next_issue_id": "T45",
            "preassigned_family_ids": False,
            "preassigned_host": False,
            "remaining_recipe_gap": common.CLOSING_EXECUTION_GAP,
            "rule": (
                "T43 Smelter stone and T36-Repair stay complete. T44 is the "
                "Storage bundle. T45 is consecutive numbering only and is not "
                "preassigned a host or families."
            ),
            "sequence": sequence,
            "t44_complete": True,
            "t44_history_readonly": False,
            "unique_active_card": None,
        }
    )
    return document


def check() -> list[str]:
    from tools import closeout_seal

    return closeout_seal.live_or_sealed_errors(
        "T44",
        "topology",
        lambda: common.check_document(OUTPUT, build()),
    )


def main(argv: list[str] | None = None) -> int:
    return common.run_managed(
        "Write the T44 card topology overlay",
        OUTPUT,
        build=build,
        check=check,
        argv=argv,
    )


if __name__ == "__main__":
    raise SystemExit(main())
