#!/usr/bin/env python3
"""Derive T42_OWNER_READY from the owner partition and its tightened topology gate."""
from __future__ import annotations

import sys
from pathlib import Path
from typing import Any, Callable

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35
from tools import t42_common as t42
from tools import t42_owner_common as common
from tools import build_t42_owner_pre_freeze as freeze_builder

OUTPUT = common.READINESS


def _load(path: Path, fallback: Callable[[], dict[str, Any]]) -> dict[str, Any]:
    return common.load_json(path) if path.is_file() else fallback()


def _base_ready() -> bool:
    repair = common.repaired_t42()["repair"]
    return repair.get("status") == "T42_REPAIR_READY" and repair.get("failed_gates") == []


def _owner_partition_complete() -> bool:
    overlay = _load(
        common.TRACK_OVERLAY,
        lambda: __import__(
            "tools.build_t42_owner_track_overlay", fromlist=["build"]
        ).build()["overlay"],
    )
    return (
        overlay.get("family_count") == common.OPENING_EXECUTION_GAP
        and all(
            common.valid_owner_track(row.get("current_owner"))
            and row.get("owner_state") in common.OWNER_STATES
            for row in overlay.get("families") or []
        )
    )


def _lock_complete() -> bool:
    lock = _load(
        common.DISPOSITION_LOCK,
        lambda: __import__(
            "tools.build_t42_owner_disposition_lock", fromlist=["build"]
        ).build(),
    )
    retained = [
        row
        for row in lock.get("families") or []
        if row.get("disposition") == "retained_current_execution_gap"
    ]
    deferred = [
        row
        for row in lock.get("families") or []
        if row.get("disposition") == "phase_deferred"
    ]
    return (
        lock.get("append_only") is True
        and len(retained) + len(deferred) == common.OPENING_EXECUTION_GAP
        and all(common.valid_owner_track(row.get("current_owner")) for row in retained)
        and all(
            row.get("current_owner") is None
            and str(row.get("future_owner") or "").startswith("later:")
            and row.get("recheck_condition")
            and row.get("evidence_root_sha256")
            for row in deferred
        )
    )


def _gap_closes() -> bool:
    gap = _load(
        common.GAP_PARTITION,
        lambda: __import__(
            "tools.build_t42_owner_gap_partition", fromlist=["build"]
        ).build(),
    )
    return (
        gap.get("opening_execution_gap") == common.OPENING_EXECUTION_GAP
        and gap.get("completion_delta") == 0
        and gap.get("publication_delta") == 0
        and gap.get("closing_execution_gap")
        == common.OPENING_EXECUTION_GAP - gap.get("reclassification_delta", 0)
        and gap.get("retained_current_execution_gap") == gap.get("closing_execution_gap")
    )


def _topology_gated() -> bool:
    topology = common.repaired_t42()["topology"]
    owner = next(
        (row for row in topology.get("sequence") or [] if row.get("id") == "T42-Owner"),
        {},
    )
    t43 = next(
        (row for row in topology.get("sequence") or [] if row.get("id") == "T43"),
        {},
    )
    storage = next(
        (
            row
            for row in topology.get("sequence") or []
            if row.get("track") == "storage_bundle"
        ),
        {},
    )
    return (
        owner.get("kind") == "owner_partition_repair"
        and owner.get("owns_families") == 0
        and "T42-Owner" in list(t43.get("depends_on") or [])
        and topology.get("storage_interleave_gate")
        == "T42_OWNER_READY && owner_partition_complete"
        and storage.get("after") == "T42_OWNER_READY and owner partition complete"
        and "T42-Owner" in list(storage.get("depends_on") or [])
        and topology.get("next_issue_id") == "T43"
        and topology.get("preassigned_host") is False
        and topology.get("preassigned_family_ids") is False
        and topology.get("unique_active_content_card") is None
    )


def _no_auto_object_expression_defer() -> bool:
    lock = _load(
        common.DISPOSITION_LOCK,
        lambda: __import__(
            "tools.build_t42_owner_disposition_lock", fromlist=["build"]
        ).build(),
    )
    return not any(
        str(row.get("future_owner") or "").startswith("later:object_expression/")
        for row in lock.get("families") or []
        if row.get("disposition") == "phase_deferred"
    )


GATE_PROBES: tuple[tuple[str, Callable[[], bool]], ...] = (
    ("t42_repair_ready", _base_ready),
    ("post_repair_freeze_current", lambda: not freeze_builder.freeze_errors()),
    ("owner_partition_complete", _owner_partition_complete),
    ("owner_lock_complete", _lock_complete),
    ("owner_gap_arithmetic", _gap_closes),
    ("topology_gates_t43_and_storage", _topology_gated),
    ("no_object_expression_auto_defer", _no_auto_object_expression_defer),
)


def build() -> dict[str, Any]:
    gates = {name: probe() for name, probe in GATE_PROBES}
    failed = sorted(name for name, passed in gates.items() if not passed)
    gap = _load(
        common.GAP_PARTITION,
        lambda: __import__(
            "tools.build_t42_owner_gap_partition", fromlist=["build"]
        ).build(),
    )
    lock = _load(
        common.DISPOSITION_LOCK,
        lambda: __import__(
            "tools.build_t42_owner_disposition_lock", fromlist=["build"]
        ).build(),
    )
    return {
        "base_t42_closing_execution_gap": common.OPENING_EXECUTION_GAP,
        "closing_execution_gap": gap.get("closing_execution_gap"),
        "completion_delta": 0,
        "current_owner_counts": lock.get("current_owner_counts") or {},
        "failed_gates": failed,
        "gates": gates,
        "generated_by": "python tools/build_t42_owner_readiness.py",
        "note": (
            "T42_OWNER_READY is an internal governance gate. It does not occupy "
            "T43 or complete recipes. It proves every retained family has an "
            "actionable current owner before T43 or storage may be issued."
        ),
        "owns_families": 0,
        "publication_delta": 0,
        "reclassification_delta": gap.get("reclassification_delta"),
        "schema_version": 1,
        "source_revision": common.SOURCE_REVISION,
        "status": "T42_OWNER_READY" if not failed else "T42_OWNER_BLOCKED",
        "t43_not_issued": True,
    }


def write() -> None:
    t35.write_stable(OUTPUT, build())


def check() -> list[str]:
    return common.check_document(OUTPUT, build())


def main(argv: list[str] | None = None) -> int:
    args = common.parse_managed("Build T42-Owner readiness", argv)
    if common.handle_rebind(args, OUTPUT):
        return 0
    try:
        if args.write:
            write()
            print("Wrote T42-Owner readiness.")
            return 0
        errors = check()
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        print("T42-Owner readiness is current.")
        return 0
    except (OSError, ValueError, KeyError, FileNotFoundError) as error:
        print(f"T42-Owner readiness failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
