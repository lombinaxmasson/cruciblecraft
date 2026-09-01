#!/usr/bin/env python3
"""Capture the T47-VR pre-repair freeze. The freeze is a snapshot, not live proof."""
from __future__ import annotations

import json
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35
from tools import t47_vr_common as vr

OUTPUT = vr.FREEZE


def build() -> dict[str, Any]:
    live = vr.live_counts()
    if live != vr.FROZEN_COUNTS:
        raise ValueError(
            "live production facts drifted from T47-VR freeze contract: "
            + json.dumps({"expected": vr.FROZEN_COUNTS, "live": live}, sort_keys=True)
        )
    lock_hashes = vr.live_lock_hashes()
    if lock_hashes["tools/t46_production_lock.json"] != vr.EXPECTED_T46_LOCK_SHA256:
        raise ValueError("T46 production lock hash drifted; freeze refuses to capture")
    if lock_hashes["tools/t47_production_lock.json"] != vr.EXPECTED_T47_LOCK_SHA256:
        raise ValueError("T47 production lock hash drifted; freeze refuses to capture")
    snapshot = vr.snapshot_hashes()
    missing = [path for path, digest in snapshot.items() if not digest]
    if missing:
        raise ValueError("freeze snapshot is missing files: " + ", ".join(missing))
    return {
        "schema_version": 1,
        "artifact": "T47_VR_PRE_REPAIR_FREEZE",
        "reference_kind": "freeze_reference",
        "evidence_scope": "pre_repair",
        "statement": "This is a freeze reference, not post-repair evidence.",
        "source_revision": vr.SOURCE_REVISION,
        "generated_by": "python tools/build_t47_vr_pre_repair_freeze.py",
        "frozen_counts": dict(vr.FROZEN_COUNTS),
        "immutable_lock_hashes": lock_hashes,
        "snapshot_hashes": snapshot,
        "open_debt_ids": list(vr.OPEN_DEBT_IDS),
        "repair_prohibitions": [
            "do_not_approve_resign_t46_or_t47_lock",
            "do_not_change_family_gap_or_census_identity_to_pass_repair",
            "do_not_disguise_open_upstream_debt_as_pass_skip_or_inherited_ready",
            "do_not_issue_t48_or_prewrite_t48_host_or_family_ids",
        ],
        "t48_not_issued": (
            bool(vr.load_json(OUTPUT).get("t48_not_issued"))
            if OUTPUT.is_file()
            else vr.t48_not_issued()
        ),
        "note": (
            "Immutable lock hashes must keep matching live T46/T47 production "
            "locks. Other snapshot hashes record pre-repair currentness and may "
            "change during T47-VR without rewriting this freeze. Untracked T47 "
            "artifacts on HEAD are a persist gap, not a T47_READY failure."
        ),
    }


def freeze_errors(document: dict[str, Any] | None = None) -> list[str]:
    if document is None:
        if not OUTPUT.is_file():
            return ["missing T47-VR freeze: tools/t47_vr_pre_repair_freeze.json"]
        document = vr.load_json(OUTPUT)
    errors: list[str] = []
    if document.get("artifact") != "T47_VR_PRE_REPAIR_FREEZE":
        errors.append("T47-VR freeze artifact name drifted")
    if document.get("frozen_counts") != vr.FROZEN_COUNTS:
        errors.append("T47-VR freeze frozen_counts drifted")
    live = vr.live_counts()
    if live != vr.FROZEN_COUNTS:
        errors.append("live production facts drifted from T47-VR freeze")
    immutable = document.get("immutable_lock_hashes") or {}
    live_locks = vr.live_lock_hashes()
    for path in vr.IMMUTABLE_HASH_PATHS:
        if immutable.get(path) != live_locks.get(path):
            errors.append(f"live lock hash drifted from freeze: {path}")
    if not document.get("t48_not_issued"):
        errors.append("T47-VR freeze must record T48 as unissued")
    if list(document.get("open_debt_ids") or []) != list(vr.OPEN_DEBT_IDS):
        errors.append("T47-VR freeze open_debt_ids drifted")
    if not vr.open_debt_not_disguised():
        errors.append("open verification debt was disguised as PASS/skip/READY")
    return errors


def write() -> dict[str, Any]:
    document = build()
    if OUTPUT.is_file():
        existing = OUTPUT.read_text(encoding="utf-8")
        if existing != t35.stable_json(document):
            raise ValueError(
                "refusing to replace the T47-VR pre-repair freeze; it is a "
                "write-once snapshot"
            )
        return vr.load_json(OUTPUT)
    t35.write_stable(OUTPUT, document)
    return document


def check() -> list[str]:
    return freeze_errors()


def main(argv: list[str] | None = None) -> int:
    args = vr.parse_write_check(__doc__, argv)
    try:
        if args.write:
            write()
        else:
            errors = check()
            if errors:
                print("\n".join(errors), file=sys.stderr)
                return 1
        return 0
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"T47-VR freeze failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
