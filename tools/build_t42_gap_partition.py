#!/usr/bin/env python3
"""Publish the T42 execution-gap partition. Completion delta stays 0."""
from __future__ import annotations

import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35
from tools import t42_common as common

OUTPUT = common.GAP_PARTITION


def _lock() -> dict[str, Any]:
    if common.DISPOSITION_LOCK.is_file():
        return common.load_json(common.DISPOSITION_LOCK)
    from tools import build_t42_disposition_lock as lock_builder

    return lock_builder.build()


def _overlay() -> dict[str, Any]:
    if common.BLOCKER_OVERLAY.is_file():
        return common.load_json(common.BLOCKER_OVERLAY)
    from tools import build_t42_blocker_overlay as overlay_builder

    return overlay_builder.build()["overlay"]


def build() -> dict[str, Any]:
    lock = _lock()
    overlay = _overlay()
    already = int(lock.get("already_expressed_delta") or 0)
    reclass = int(lock.get("reclassification_delta") or 0)
    opening = common.OPENING_EXECUTION_GAP
    closing = opening - already - reclass
    if closing < 0:
        raise ValueError("T42 gap partition underflow")
    deferred = list(lock.get("deferred_family_ids") or [])
    lock_by_id = {row["family_id"]: row for row in lock.get("families") or []}
    deducted_partials = []
    for family in overlay.get("families") or []:
        if "partial" not in set(family.get("secondary_blockers") or []):
            continue
        locked = lock_by_id.get(family["family_id"]) or {}
        if locked.get("disposition") != "retained_current_execution_gap":
            deducted_partials.append(family["family_id"])
    if deducted_partials:
        raise ValueError(
            "partial families cannot leave the execution gap: "
            + ", ".join(deducted_partials[:10])
        )
    if int(lock.get("family_count") or 0) != opening:
        raise ValueError("lock family_count drifted from opening gap")
    overlay_partial = sum(
        1
        for family in overlay.get("families") or []
        if "partial" in set(family.get("secondary_blockers") or [])
    )
    return {
        "already_expressed_delta": already,
        "closing_execution_gap": closing,
        "completion_delta": 0,
        "deferred_ordinary_ledger_count": len(deferred),
        "generated_by": "python tools/build_t42_gap_partition.py",
        "note": (
            "partial_family_count is families deducted from the execution gap, "
            "not overlay secondary-blocker partials. T42 deducts none; overlay "
            "partial families stay inside the closing 5,300."
        ),
        "opening_execution_gap": opening,
        "overlay_partial_family_count": overlay_partial,
        "owns_families": 0,
        "partial_families_deducted_from_gap": 0,
        "partial_family_count": 0,
        "publication_delta": 0,
        "reclassification_delta": reclass,
        "retained_needs_form_or_molten_count": int(
            lock.get("retained_needs_form_or_molten_count") or 0
        ),
        "retained_ready_count": int(lock.get("retained_ready_count") or 0),
        "schema_version": 1,
        "source_revision": common.SOURCE_REVISION,
        "status": "T42_GAP_PARTITION",
    }


def write() -> None:
    t35.write_stable(OUTPUT, build())


def check() -> list[str]:
    return common.check_document(OUTPUT, build())


def main(argv: list[str] | None = None) -> int:
    args = common.parse_managed("Build T42 gap partition", argv)
    if common.handle_rebind(args, OUTPUT):
        return 0
    try:
        if args.write:
            write()
            print("Wrote T42 gap partition.")
            return 0
        errors = check()
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        print("T42 gap partition is current.")
        return 0
    except (OSError, ValueError, KeyError, FileNotFoundError) as error:
        print(f"T42 gap partition failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
