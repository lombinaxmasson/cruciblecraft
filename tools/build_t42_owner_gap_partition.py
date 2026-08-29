#!/usr/bin/env python3
"""Publish the additive T42-Owner execution-gap partition."""
from __future__ import annotations

import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35
from tools import t42_owner_common as common

OUTPUT = common.GAP_PARTITION


def _lock() -> dict[str, Any]:
    if common.DISPOSITION_LOCK.is_file():
        return common.load_json(common.DISPOSITION_LOCK)
    from tools import build_t42_owner_disposition_lock as lock_builder

    return lock_builder.build()


def build() -> dict[str, Any]:
    lock = _lock()
    opening = common.OPENING_EXECUTION_GAP
    if lock.get("opening_execution_gap") != opening:
        raise ValueError("T42-Owner gap must begin at post-T42 closing 5,300")
    reclassification = int(lock.get("reclassification_delta") or 0)
    closing = opening - reclassification
    if closing < 0:
        raise ValueError("T42-Owner gap partition underflow")
    deferred = [
        row for row in lock.get("families") or []
        if row.get("disposition") == "phase_deferred"
    ]
    retained = [
        row for row in lock.get("families") or []
        if row.get("disposition") == "retained_current_execution_gap"
    ]
    if len(retained) != closing or len(deferred) != reclassification:
        raise ValueError("T42-Owner gap partition lock arithmetic drifted")
    if any(not row.get("current_owner") for row in retained):
        raise ValueError("T42-Owner retained family has no current owner")
    if any(not row.get("future_owner") for row in deferred):
        raise ValueError("T42-Owner deferred family has no future owner")
    return {
        "already_expressed_delta": 0,
        "base_t42_closing_execution_gap": opening,
        "closing_execution_gap": closing,
        "completion_delta": 0,
        "current_owner_counts": common.owner_counts(retained, "current_owner"),
        "deferred_ordinary_ledger_count": len(deferred),
        "deferred_owner_counts": common.owner_counts(deferred, "future_owner"),
        "family_count": int(lock.get("family_count") or 0),
        "generated_by": "python tools/build_t42_owner_gap_partition.py",
        "note": (
            "T42-Owner may subtract only newly proven recycling or explicitly "
            "approved object-expression families. completion_delta remains zero; "
            "all retained families have a current owner instead of null."
        ),
        "opening_execution_gap": opening,
        "owns_families": 0,
        "publication_delta": 0,
        "reclassification_delta": reclassification,
        "retained_current_execution_gap": len(retained),
        "schema_version": 1,
        "source_revision": common.SOURCE_REVISION,
        "status": "T42_OWNER_GAP_PARTITION",
    }


def write() -> None:
    t35.write_stable(OUTPUT, build())


def check() -> list[str]:
    return common.check_document(OUTPUT, build())


def main(argv: list[str] | None = None) -> int:
    args = common.parse_managed("Build T42-Owner gap partition", argv)
    if common.handle_rebind(args, OUTPUT):
        return 0
    try:
        if args.write:
            write()
            print("Wrote T42-Owner gap partition.")
            return 0
        errors = check()
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        print("T42-Owner gap partition is current.")
        return 0
    except (OSError, ValueError, KeyError, FileNotFoundError) as error:
        print(f"T42-Owner gap partition failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
