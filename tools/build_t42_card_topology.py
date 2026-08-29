#!/usr/bin/env python3
"""T42 topology overlay. T43 is named only; host/families stay unassigned."""
from __future__ import annotations

import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35
from tools import t41_common as t41
from tools import t42_common as common

OUTPUT = common.CARD_TOPOLOGY


def _gap() -> dict[str, Any]:
    if common.GAP_PARTITION.is_file():
        return common.load_json(common.GAP_PARTITION)
    from tools import build_t42_gap_partition as gap_builder

    return gap_builder.build()


def _t41_sequence() -> list[dict[str, Any]]:
    topology = common.load_json(t41.T41_CARD_TOPOLOGY)
    sequence = []
    for row in topology.get("sequence") or []:
        if row.get("id_policy") == "consecutive_from_T42" or row.get("track") in {
            "storage_bundle",
            "1x_exit_gate",
            "nuclear",
        }:
            continue
        copied = dict(row)
        if copied.get("id") == "T41":
            copied["status"] = "complete"
        sequence.append(copied)
    return sequence


def build() -> dict[str, Any]:
    gap = _gap()
    if gap.get("completion_delta") != 0:
        raise ValueError("T42 topology forbids completion_delta != 0")
    sequence = _t41_sequence()
    sequence.append(
        {
            "depends_on": ["T41"],
            "id": "T42",
            "kind": "partition_diagnostic",
            "note": (
                "Zero-ownership remaining-family partition. Does not publish "
                "recipes. T43 host and family ids are not assigned here."
            ),
            "owns_families": 0,
            "owner": "T42",
            "preassigned_family_ids": False,
            "preassigned_host": False,
            "publication_delta": 0,
            "status": "complete",
            "track": "recipe_partition",
        }
    )
    sequence.append(
        {
            "depends_on": ["T42"],
            "id": "T42-Repair",
            "kind": "partition_repair",
            "note": (
                "Internal fidelity repair. owns_families=0, completion_delta=0, "
                "publication_delta=0. Does not occupy T43. T43 R0 must re-freeze "
                "from the repaired overlay, not the 2026-08-28 T42 lock."
            ),
            "owns_families": 0,
            "owner": "T42-Repair",
            "preassigned_family_ids": False,
            "preassigned_host": False,
            "publication_delta": 0,
            "status": "complete",
            "track": "recipe_partition",
        }
    )
    sequence.append(
        {
            "depends_on": ["T42-Repair"],
            "id": "T42-Owner",
            "kind": "owner_partition_repair",
            "note": (
                "Internal owner-partition repair. Every retained family must "
                "have a current owner before T43 or storage can be issued. "
                "It does not occupy T43 or publish recipes."
            ),
            "owns_families": 0,
            "owner": "T42-Owner",
            "preassigned_family_ids": False,
            "preassigned_host": False,
            "publication_delta": 0,
            "status": "internal_gate_required",
            "track": "recipe_partition",
        }
    )
    sequence.append(
        {
            "append_rule": (
                "T43 R0 must re-freeze from the repaired T42 overlay. If the "
                "drying ready slice fails gates, withdraw the intent and "
                "re-issue from wave_candidates; do not silently pad."
            ),
            "depends_on": ["T42-Repair", "T42-Owner"],
            "exit_gate": "wave families authored or measured; remaining gap recomputed",
            "id": "T43",
            "id_policy": "consecutive_from_T43",
            "kind": "generated_rule",
            "note": (
                "Next content card is not preassigned and stays unissued until "
                "T42_OWNER_READY. Drying current_closure_ready is a priority "
                "intent in tools/t42_wave_candidates.json only."
            ),
            "owner_rule": "the issued card id owns the wave",
            "preassigned_family_ids": False,
            "preassigned_host": False,
            "track": "recipe_wave",
            "until": "current recipe execution gap = 0",
        }
    )
    sequence.extend(
        [
            {
                "after": "T42_OWNER_READY and owner partition complete",
                "depends_on": ["T42-Owner", "recipe_waves_or_serial_slot"],
                "exit_gate": "canonical storage families implemented under the bundle architecture",
                "id_policy": "consecutive_serial_after_deferred_ledger_locked",
                "kind": "generated_rule",
                "note": (
                    "Storage / presentation / multiblock bundles may interleave "
                    "serially only after the full owner partition is locked. "
                    "Still one active content card."
                ),
                "owner_rule": "issued storage card id",
                "track": "storage_bundle",
            },
            {
                "after": "storage_complete and execution gap 0",
                "depends_on": ["storage_bundles", "deferred_ordinary_ledger"],
                "exit_gate": (
                    "execution gap = 0 and every deferred ledger item closed "
                    "or explicitly scoped post-1.x"
                ),
                "id_policy": "consecutive_after_storage",
                "kind": "generated_rule",
                "owner_rule": "issued exit-gate card id",
                "track": "1x_exit_gate",
            },
            {
                "id": "nuclear_source_physics_census",
                "kind": "later_stage",
                "later_stage": "post_1x",
                "owner": "post_1x",
                "track": "nuclear",
            },
        ]
    )
    return {
        "append_only": False,
        "epoch": "T42_PARTITION",
        "fixed_cards": ["T36", "T37", "T38", "T39", "T40", "T41", "T42"],
        "generated_by": "python tools/build_t42_card_topology.py",
        "next_issue_id": "T43",
        "owns_families": 0,
        "preassigned_family_ids": False,
        "preassigned_host": False,
        "remaining_recipe_gap": gap["closing_execution_gap"],
        "rule": (
            "T42 is a numbered partition card, not a recipe wave. T42-Repair "
            "and T42-Owner are internal gates and do not occupy T43. T43 is "
            "named only. Storage may interleave serially only after "
            "T42_OWNER_READY and owner_partition_complete. "
            "unique_active_content_card is null."
        ),
        "schema_version": 1,
        "sequence": sequence,
        "source_revision": common.SOURCE_REVISION,
        "status": "T42_CARD_TOPOLOGY",
        "storage_interleave_gate": "T42_OWNER_READY && owner_partition_complete",
        "t35_history_readonly": True,
        "t36_history_readonly": True,
        "t37_history_readonly": True,
        "t38_history_readonly": True,
        "t39_history_readonly": True,
        "t40_history_readonly": True,
        "t41_history_readonly": True,
        "unique_active_card": None,
        "unique_active_content_card": None,
        "validators": {
            "no_t43_family_ids": 0,
            "owns_families_zero": 0,
            "preassigned_family_ids_false": 0,
            "preassigned_host_false": 0,
            "unique_active_content_card_null": 0,
        },
    }


def write() -> None:
    t35.write_stable(OUTPUT, build())


def check() -> list[str]:
    document = build()
    errors = common.check_document(OUTPUT, document)
    if document.get("next_issue_id") != "T43":
        errors.append("next_issue_id must be T43")
    if document.get("preassigned_host") or document.get("preassigned_family_ids"):
        errors.append("T43 must not be preassigned")
    if document.get("unique_active_content_card") is not None:
        errors.append("unique_active_content_card must be null")
    t43 = next((row for row in document["sequence"] if row.get("id") == "T43"), None)
    if t43 and t43.get("family_ids"):
        errors.append("T42 topology must not write T43 family ids")
    repair = next(
        (row for row in document["sequence"] if row.get("id") == "T42-Repair"),
        None,
    )
    if not repair or repair.get("kind") != "partition_repair":
        errors.append("T42 topology must insert partition_repair after T42")
    if t43 and "T42-Repair" not in list(t43.get("depends_on") or []):
        errors.append("T43 must depend on T42-Repair")
    owner = next(
        (row for row in document["sequence"] if row.get("id") == "T42-Owner"),
        None,
    )
    if not owner or owner.get("kind") != "owner_partition_repair":
        errors.append("T42 topology must insert T42-Owner after T42-Repair")
    if t43 and "T42-Owner" not in list(t43.get("depends_on") or []):
        errors.append("T43 must depend on T42-Owner")
    storage = next(
        (
            row
            for row in document["sequence"]
            if row.get("track") == "storage_bundle"
        ),
        None,
    )
    if (
        document.get("storage_interleave_gate")
        != "T42_OWNER_READY && owner_partition_complete"
        or not storage
        or storage.get("after") != "T42_OWNER_READY and owner partition complete"
        or "T42-Owner" not in list(storage.get("depends_on") or [])
    ):
        errors.append("storage interleave must require T42-Owner partition readiness")
    return errors


def main(argv: list[str] | None = None) -> int:
    args = common.parse_managed("Build T42 card topology", argv)
    if common.handle_rebind(args, OUTPUT):
        return 0
    try:
        if args.write:
            write()
            print("Wrote T42 card topology.")
            return 0
        errors = check()
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        print("T42 card topology is current.")
        return 0
    except (OSError, ValueError, KeyError, FileNotFoundError) as error:
        print(f"T42 card topology failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
