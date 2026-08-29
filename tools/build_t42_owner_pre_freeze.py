#!/usr/bin/env python3
"""Capture the immutable post-T42-Repair input freeze for T42-Owner."""
from __future__ import annotations

import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35
from tools import t42_owner_common as common

OUTPUT = common.PRE_FREEZE


def build() -> dict[str, Any]:
    base = common.repaired_t42()
    return {
        "artifact": "T42_OWNER_PRE_FREEZE",
        "base_artifact_hashes": common.base_artifact_hashes(),
        "base_closing_execution_gap": common.OPENING_EXECUTION_GAP,
        "evidence_scope": "post_t42_repair",
        "generated_by": "python tools/build_t42_owner_pre_freeze.py",
        "note": (
            "Write-once freeze for the repaired T42 closing state. T42-Owner "
            "does not rewrite T42's historical bucket/lock artifacts; it adds "
            "an owner-track overlay over their retained 5,300-family gap."
        ),
        "owns_families": 0,
        "publication_delta": 0,
        "reference_kind": "owner_partition_input_freeze",
        "schema_version": 1,
        "source_revision": common.SOURCE_REVISION,
        "t42_repair_status": base["repair"].get("status"),
        "t43_not_issued": True,
    }


def freeze_errors(document: dict[str, Any] | None = None) -> list[str]:
    if document is None:
        if not OUTPUT.is_file():
            return ["missing T42-Owner freeze: tools/t42_owner_pre_freeze.json"]
        document = common.load_json(OUTPUT)
    errors: list[str] = []
    if document.get("artifact") != "T42_OWNER_PRE_FREEZE":
        errors.append("T42-Owner freeze artifact name drifted")
    if document.get("base_closing_execution_gap") != common.OPENING_EXECUTION_GAP:
        errors.append("T42-Owner freeze opening gap drifted")
    if document.get("owns_families") != 0:
        errors.append("T42-Owner freeze owns_families must be 0")
    if document.get("t42_repair_status") != "T42_REPAIR_READY":
        errors.append("T42-Owner freeze must pin T42_REPAIR_READY")
    hashes = document.get("base_artifact_hashes") or {}
    for path in common.BASE_ARTIFACTS:
        if not hashes.get(path):
            errors.append(f"T42-Owner freeze missing hash: {path}")
    if hashes and hashes != common.base_artifact_hashes():
        errors.append("T42-Owner freeze inputs drifted from repaired T42 roots")
    return errors


def write() -> dict[str, Any]:
    document = build()
    if OUTPUT.is_file():
        existing = OUTPUT.read_text(encoding="utf-8")
        if existing != t35.stable_json(document):
            raise ValueError(
                "refusing to replace T42-Owner pre-freeze; it is a write-once "
                "post-repair input snapshot"
            )
        return common.load_json(OUTPUT)
    t35.write_stable(OUTPUT, document)
    return document


def check() -> list[str]:
    return freeze_errors()


def main(argv: list[str] | None = None) -> int:
    args = common.parse_managed("Capture T42-Owner post-repair freeze", argv)
    if common.handle_rebind(args, OUTPUT):
        return 0
    try:
        if args.write:
            write()
            print("Wrote T42-Owner pre-freeze.")
            return 0
        errors = check()
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        print("T42-Owner pre-freeze is current.")
        return 0
    except (OSError, ValueError, KeyError, FileNotFoundError) as error:
        print(f"T42-Owner pre-freeze failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
