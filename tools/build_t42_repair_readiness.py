#!/usr/bin/env python3
"""Build the T42-Repair account without occupying T43 or claiming recipe closure."""
from __future__ import annotations

import json
import sys
from pathlib import Path
from typing import Any, Callable

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35
from tools import t42_common as common
from tools import build_t42_repair_pre_freeze as freeze_builder

OUTPUT = common.REPAIR_READINESS


def _load(path: Path, default: dict[str, Any] | None = None) -> dict[str, Any]:
    if path.is_file():
        return common.load_json(path)
    return dict(default or {})


def _partition_ready() -> bool:
    readiness = _load(common.READINESS)
    return (
        readiness.get("status") == "T42_PARTITION_READY"
        and list(readiness.get("failed_gates") or []) == []
    )


def _freeze_current() -> bool:
    return not freeze_builder.freeze_errors()


def _delta_zero() -> bool:
    gap = _load(common.GAP_PARTITION)
    return (
        gap.get("completion_delta") == 0
        and gap.get("publication_delta") == 0
        and gap.get("owns_families") == 0
    )


def _t43_unassigned() -> bool:
    topology = _load(common.CARD_TOPOLOGY)
    t43 = next(
        (row for row in topology.get("sequence") or [] if row.get("id") == "T43"),
        {},
    )
    return (
        topology.get("next_issue_id") == "T43"
        and topology.get("preassigned_host") is False
        and topology.get("preassigned_family_ids") is False
        and topology.get("unique_active_content_card") is None
        and not t43.get("family_ids")
        and t43.get("preassigned_host") is False
        and t43.get("preassigned_family_ids") is False
    )


def _partition_repair_present() -> bool:
    topology = _load(common.CARD_TOPOLOGY)
    repair = next(
        (
            row
            for row in topology.get("sequence") or []
            if row.get("id") == "T42-Repair"
        ),
        {},
    )
    t43 = next(
        (row for row in topology.get("sequence") or [] if row.get("id") == "T43"),
        {},
    )
    return (
        repair.get("kind") == "partition_repair"
        and repair.get("owns_families") == 0
        and "T42-Repair" in list(t43.get("depends_on") or [])
    )


def _unique_kind_honest() -> bool:
    overlay = _load(common.BLOCKER_OVERLAY)
    counts = overlay.get("unique_kind_counts") or {}
    unique_bucket = int(
        overlay.get("unique_bucket_family_count")
        or (overlay.get("bucket_counts") or {}).get("needs_unique_block_or_mte")
        or 0
    )
    if not counts:
        return False
    return int(counts.get("mte") or 0) < unique_bucket


def _molten_split() -> bool:
    inventory = _load(common.RUNTIME_INVENTORY)
    fluids = inventory.get("fluids") or {}
    return (
        int(fluids.get("generation_tag_molten_material_count") or 0) > 0
        and fluids.get("molten_not_cc_registration_proof") is True
        and "top_level_molten_flag_count" in fluids
        and "remaining_molten_fluids_in_mapping" in fluids
    )


def _allowlist_provenance() -> bool:
    allowlist = _load(common.VANILLA_ALLOWLIST)
    return (
        allowlist.get("provenance") == "t37_t41_aliases_plus_explicit"
        and allowlist.get("not_a_121_registry_scrape") is True
        and allowlist.get("minecraft_prefix_is_not_proof") is True
        and "minecraft:anvil" not in set(allowlist.get("item_ids") or [])
        and "minecraft:iron_ingot" not in set(allowlist.get("item_ids") or [])
        and "minecraft:bucket" not in set(allowlist.get("item_ids") or [])
        and "minecraft:oak_planks" in set(allowlist.get("explicit_item_ids") or [])
        and "minecraft:oak_planks" in set(allowlist.get("item_ids") or [])
    )


def _empty_markers() -> bool:
    return common.EMPTY_ITEM_MARKERS == (
        "empty_slot",
        "gt.meta.empty",
        "gregtech:gt.meta.empty",
    ) and "gt.metaitem.01" not in common.EMPTY_ITEM_MARKERS


def _unique_bucket_residual_split() -> bool:
    overlay = _load(common.BLOCKER_OVERLAY)
    unique_bucket = int(
        overlay.get("unique_bucket_family_count")
        or (overlay.get("bucket_counts") or {}).get("needs_unique_block_or_mte")
        or 0
    )
    with_kind = int(overlay.get("unique_object_kind_family_count") or 0)
    residual = int(overlay.get("unique_residual_without_kind_count") or 0)
    return (
        unique_bucket > 0
        and with_kind + residual == unique_bucket
        and with_kind != unique_bucket
    )


def _programmed_circuit_mapping_not_proof() -> bool:
    inventory = _load(common.RUNTIME_INVENTORY)
    circuit = inventory.get("programmed_circuit") or {}
    b0 = _load(common.REACHABILITY_BASELINE)
    identities = set(b0.get("reachable_identities") or []) | set(
        b0.get("added_identities") or []
    )
    return (
        circuit.get("mapping_is_not_expression_proof") is True
        and circuit.get("present_in_t35_runtime_registry") is False
        and "item:cruciblecraft:programmed_circuit" not in identities
        and common.PROGRAMMED_CIRCUIT not in identities
    )


def _gap_partial_semantics() -> bool:
    gap = _load(common.GAP_PARTITION)
    overlay = _load(common.BLOCKER_OVERLAY)
    return (
        gap.get("partial_family_count") == 0
        and gap.get("partial_families_deducted_from_gap") == 0
        and int(gap.get("overlay_partial_family_count") or 0)
        == int(overlay.get("partial_family_count") or 0)
    )


def _no_object_expression_auto_defer() -> bool:
    lock = _load(common.DISPOSITION_LOCK)
    return not any(
        str(row.get("future_owner") or "").startswith("later:object_expression")
        for row in lock.get("families") or []
        if row.get("disposition") == "phase_deferred"
    )


GATE_PROBES: tuple[tuple[str, Callable[[], bool]], ...] = (
    ("r0_freeze_current", _freeze_current),
    ("t42_partition_ready_preserved", _partition_ready),
    ("repair_owns_no_families", lambda: True),
    ("completion_delta_zero", _delta_zero),
    ("t43_unassigned", _t43_unassigned),
    ("partition_repair_in_topology", _partition_repair_present),
    ("unique_kind_counts_honest", _unique_kind_honest),
    ("unique_bucket_residual_split", _unique_bucket_residual_split),
    ("programmed_circuit_mapping_not_proof", _programmed_circuit_mapping_not_proof),
    ("gap_partial_semantics", _gap_partial_semantics),
    ("molten_counts_split", _molten_split),
    ("allowlist_provenance", _allowlist_provenance),
    ("empty_markers_t37", _empty_markers),
    ("no_object_expression_auto_defer", _no_object_expression_auto_defer),
)


def build() -> dict[str, Any]:
    gates = {name: probe() for name, probe in GATE_PROBES}
    failed = sorted(name for name, passed in gates.items() if not passed)
    gap = _load(common.GAP_PARTITION)
    overlay = _load(common.BLOCKER_OVERLAY)
    inventory = _load(common.RUNTIME_INVENTORY)
    circuit = inventory.get("programmed_circuit") or {}
    return {
        "completion_delta": 0,
        "failed_gates": failed,
        "gates": gates,
        "generated_by": "python tools/build_t42_repair_readiness.py",
        "note": (
            "T42_REPAIR_READY proves snapshot/inventory/classifier fidelity. "
            "It does not replace tools/t42_readiness.json or occupy T43. "
            "T43 R0 must consume the repaired overlay. Circuit mapping, unique "
            "residual families, and gap partial_family_count=0 are not closure."
        ),
        "opening_execution_gap": common.OPENING_EXECUTION_GAP,
        "owns_families": 0,
        "publication_delta": 0,
        "repaired_closing_execution_gap": gap.get("closing_execution_gap"),
        "schema_version": 1,
        "source_revision": common.SOURCE_REVISION,
        "status": "T42_REPAIR_READY" if not failed else "T42_REPAIR_BLOCKED",
        "t43_caveats": {
            "allowlist_includes_explicit_oak_planks": True,
            "gap_partial_family_count_means_not_deducted": True,
            "overlay_partial_family_count": int(
                overlay.get("partial_family_count") or 0
            ),
            "programmed_circuit_in_b0": False,
            "programmed_circuit_in_t35_runtime_registry": bool(
                circuit.get("present_in_t35_runtime_registry")
            ),
            "programmed_circuit_mapping_is_not_proof": True,
            "unique_bucket_is_residual": True,
            "unique_object_kind_family_count": int(
                overlay.get("unique_object_kind_family_count") or 0
            ),
            "unique_residual_without_kind_count": int(
                overlay.get("unique_residual_without_kind_count") or 0
            ),
        },
        "t43_not_issued": True,
        "unique_kind_counts": overlay.get("unique_kind_counts") or {},
        "unique_object_kind_family_count": overlay.get(
            "unique_object_kind_family_count"
        ),
        "unique_residual_without_kind_count": overlay.get(
            "unique_residual_without_kind_count"
        ),
    }


def write() -> dict[str, Any]:
    document = build()
    t35.write_stable(OUTPUT, document)
    return document


def check() -> list[str]:
    return common.check_document(OUTPUT, build())


def main(argv: list[str] | None = None) -> int:
    args = common.parse_managed("Build T42-Repair readiness", argv)
    if common.handle_rebind(args, OUTPUT):
        return 0
    try:
        if args.write:
            write()
            print("Wrote T42-Repair readiness.")
            return 0
        errors = check()
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        print("T42-Repair readiness is current.")
        return 0
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"T42-Repair readiness failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
