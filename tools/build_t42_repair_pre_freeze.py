#!/usr/bin/env python3
"""Capture the T42-Repair pre-repair hash freeze. Hashes only; not live proof."""
from __future__ import annotations

import json
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35
from tools import t42_common as common

OUTPUT = common.REPAIR_FREEZE


def snapshot_hashes() -> dict[str, str]:
    return common.file_sha256_map(common.REPAIR_GIANT_ARTIFACTS)


def build() -> dict[str, Any]:
    hashes = snapshot_hashes()
    return {
        "artifact": "T42_REPAIR_PRE_FREEZE",
        "evidence_scope": "pre_repair",
        "generated_by": "python tools/build_t42_repair_pre_freeze.py",
        "giant_artifact_hashes": hashes,
        "note": (
            "Write-once hash freeze of T42 giant artifacts before fidelity "
            "repair. This is not a second copy of the family operand snapshot. "
            "Live overlay/lock hashes may change during T42-Repair without "
            "rewriting this freeze."
        ),
        "opening_execution_gap": common.OPENING_EXECUTION_GAP,
        "owns_families": 0,
        "reference_kind": "freeze_reference",
        "repair_prohibitions": [
            "do_not_generate_recipes_forms_or_gametests",
            "do_not_occupy_t43",
            "do_not_scrape_dump_minecraft_ids_onto_allowlist",
            "do_not_fold_armor_or_tool_head_into_prefix_item_to_form",
            "do_not_treat_minecraft_prefix_as_121_proof",
        ],
        "schema_version": 1,
        "source_revision": common.SOURCE_REVISION,
        "t43_not_issued": True,
    }


def freeze_errors(document: dict[str, Any] | None = None) -> list[str]:
    if document is None:
        if not OUTPUT.is_file():
            return ["missing T42-Repair freeze: tools/t42_repair_pre_freeze.json"]
        document = common.load_json(OUTPUT)
    errors: list[str] = []
    if document.get("artifact") != "T42_REPAIR_PRE_FREEZE":
        errors.append("T42-Repair freeze artifact name drifted")
    if document.get("opening_execution_gap") != common.OPENING_EXECUTION_GAP:
        errors.append("T42-Repair freeze opening gap drifted")
    if document.get("owns_families") != 0:
        errors.append("T42-Repair freeze owns_families must be 0")
    if not document.get("t43_not_issued"):
        errors.append("T42-Repair freeze must record T43 as unissued")
    hashes = document.get("giant_artifact_hashes") or {}
    for relative_path in common.REPAIR_GIANT_ARTIFACTS:
        digest = hashes.get(relative_path)
        if not digest:
            errors.append(f"freeze missing hash: {relative_path}")
    return errors


def write() -> dict[str, Any]:
    document = build()
    if OUTPUT.is_file():
        existing = OUTPUT.read_text(encoding="utf-8")
        if existing != t35.stable_json(document):
            raise ValueError(
                "refusing to replace the T42-Repair pre-repair freeze; it is a "
                "write-once snapshot of pre-repair hashes"
            )
        return common.load_json(OUTPUT)
    t35.write_stable(OUTPUT, document)
    return document


def check() -> list[str]:
    return freeze_errors()


def main(argv: list[str] | None = None) -> int:
    args = common.parse_managed("Capture the T42-Repair pre-repair hash freeze", argv)
    if common.handle_rebind(args, OUTPUT):
        return 0
    try:
        if args.write:
            write()
            print("Wrote T42-Repair pre-repair freeze.")
            return 0
        errors = check()
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        print("T42-Repair pre-repair freeze is current.")
        return 0
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"T42-Repair pre-repair freeze failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
