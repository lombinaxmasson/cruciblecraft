#!/usr/bin/env python3
"""Capture the T36-Repair pre-repair hash freeze. Hashes only; not live proof."""
from __future__ import annotations

import json
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35
from tools import t36_repair_common as common

OUTPUT = common.REPAIR_FREEZE


def build() -> dict[str, Any]:
    snapshot = common.acquisition_snapshot()
    generated = [
        row["recipe_path"]
        for row in snapshot
        if row["recipe_path"].startswith("src/generated/")
    ]
    hashes = common.file_sha256_map(common.FREEZE_FILES + tuple(generated))
    return {
        "artifact": "T36_REPAIR_PRE_FREEZE",
        "acquisition_snapshot": snapshot,
        "device_freeze": common.DEVICE_FREEZE,
        "evidence_scope": "pre_repair",
        "generated_by": "python tools/build_t36_repair_pre_freeze.py",
        "giant_artifact_hashes": hashes,
        "live_variant_count": 85,
        "note": (
            "Write-once hash freeze of T36 machine identity, T43 closing "
            "readiness, second-list Java files, and 85 acquisition recipes "
            "before extensibility repair. Live Java may change during "
            "T36-Repair without rewriting this freeze."
        ),
        "owns_families": 0,
        "reference_kind": "freeze_reference",
        "repair_prohibitions": [
            "do_not_revoke_t36_ready",
            "do_not_expand_live_85_into_1x_target",
            "do_not_enable_automatic_kind_tier_completion",
            "do_not_occupy_t44",
            "do_not_issue_storage_bundle",
            "do_not_write_fixture_ids_into_production_datapack",
        ],
        "schema_version": 1,
        "source_revision": common.SOURCE_REVISION,
        "t43_closing_gap": common.T43_CLOSING_GAP,
        "t43_ready": True,
    }


def freeze_errors(document: dict[str, Any] | None = None) -> list[str]:
    if document is None:
        if not OUTPUT.is_file():
            return ["missing T36-Repair freeze: tools/t36_repair_pre_freeze.json"]
        document = common.load_json(OUTPUT)
    errors: list[str] = []
    if document.get("artifact") != "T36_REPAIR_PRE_FREEZE":
        errors.append("T36-Repair freeze artifact name drifted")
    if document.get("owns_families") != 0:
        errors.append("T36-Repair freeze owns_families must be 0")
    if document.get("live_variant_count") != 85:
        errors.append("T36-Repair freeze must pin 85 live variants")
    if document.get("t43_closing_gap") != common.T43_CLOSING_GAP:
        errors.append("T36-Repair freeze T43 closing gap drifted")
    if not document.get("t43_ready"):
        errors.append("T36-Repair freeze must record T43 as READY")
    snapshot = document.get("acquisition_snapshot") or []
    if len(snapshot) != 85:
        errors.append("T36-Repair freeze acquisition snapshot must cover 85 machines")
    hashes = document.get("giant_artifact_hashes") or {}
    for relative_path in common.FREEZE_FILES:
        if not hashes.get(relative_path):
            errors.append(f"freeze missing hash: {relative_path}")
    if not document.get("device_freeze"):
        errors.append("T36-Repair freeze missing device numeric snapshot")
    return errors


def write() -> dict[str, Any]:
    document = build()
    if OUTPUT.is_file():
        existing = OUTPUT.read_text(encoding="utf-8")
        if existing != t35.stable_json(document):
            raise ValueError(
                "refusing to replace the T36-Repair pre-repair freeze; it is a "
                "write-once snapshot of pre-repair hashes"
            )
        return common.load_json(OUTPUT)
    t35.write_stable(OUTPUT, document)
    return document


def check() -> list[str]:
    return freeze_errors()


def main(argv: list[str] | None = None) -> int:
    args = common.parse_managed(
        "Capture the T36-Repair pre-repair hash freeze", argv
    )
    if common.handle_rebind(args, OUTPUT):
        return 0
    try:
        if args.write:
            write()
            print("Wrote T36-Repair pre-repair freeze.")
            return 0
        errors = check()
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        print("T36-Repair pre-repair freeze is current.")
        return 0
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"T36-Repair pre-repair freeze failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
