#!/usr/bin/env python3
"""Inventory Java second lists that still block T36-Repair L1/L2 extension."""
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

OUTPUT = common.REPAIR_INVENTORY


def build() -> dict[str, Any]:
    rows = common.second_list_rows()
    blocking = common.blocking_rows(rows)
    return {
        "artifact": "T36_REPAIR_SECOND_LIST_INVENTORY",
        "blocking_count": len(blocking),
        "blocking_ids": [row["id"] for row in blocking],
        "generated_by": "python tools/build_t36_repair_second_list_inventory.py",
        "lists": rows,
        "live_variant_count": common.live_variant_count(),
        "note": (
            "Living inventory of second Java lists. blocking_count must be 0 "
            "before T36_REPAIR_READY. compatibility aliases and "
            "later:kind_behavior are classified and do not block L1/L2."
        ),
        "owns_families": 0,
        "schema_version": 1,
        "source_revision": common.SOURCE_REVISION,
    }


def inventory_errors(document: dict[str, Any] | None = None) -> list[str]:
    rebuilt = build()
    if document is None:
        if not OUTPUT.is_file():
            return [
                "missing T36-Repair inventory: "
                "tools/t36_repair_second_list_inventory.json"
            ]
        return t35.check_generated_document(OUTPUT, rebuilt)
    errors: list[str] = []
    if document.get("artifact") != "T36_REPAIR_SECOND_LIST_INVENTORY":
        errors.append("T36-Repair inventory artifact name drifted")
    if document.get("owns_families") != 0:
        errors.append("T36-Repair inventory owns_families must be 0")
    if document.get("live_variant_count") != 85:
        errors.append("T36-Repair inventory live variant count drifted")
    if int(document.get("blocking_count") or -1) != len(
        document.get("blocking_ids") or []
    ):
        errors.append("T36-Repair inventory blocking_count does not match ids")
    return errors


def write() -> dict[str, Any]:
    document = build()
    t35.write_stable(OUTPUT, document)
    return document


def check() -> list[str]:
    return inventory_errors()


def main(argv: list[str] | None = None) -> int:
    args = common.parse_managed(
        "Inventory T36-Repair second Java lists", argv
    )
    if common.handle_rebind(args, OUTPUT):
        return 0
    try:
        if args.write:
            document = write()
            print(
                "Wrote T36-Repair second-list inventory; "
                f"blocking_count={document['blocking_count']}."
            )
            return 0
        errors = check()
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        print("T36-Repair second-list inventory is current.")
        return 0
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"T36-Repair inventory failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
