#!/usr/bin/env python3
"""Freeze T49 identity catalog: consume existing tiny-washed mappings, register none."""
from __future__ import annotations

import sys
from collections import defaultdict
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35
from tools import wave_bath_tiny_purified as common
from tools.build_t48_identity_catalog import remaining_unmapped_allowed

OUTPUT = common.IDENTITY_CATALOG


def lock_family_ids() -> set[str]:
    if not common.SOURCE.is_file():
        return set()
    source = common.load_json(common.SOURCE)
    if source.get("status") != "T49_BATH_SOURCE_FROZEN":
        return set()
    work_set = common.load_json(common.WORK_SET)
    by_family: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for relation in source.get("relations") or []:
        by_family[str(relation["family_id"])].append(relation)
    selected: set[str] = set()
    for row in work_set.get("families") or []:
        family_id = str(row["family_id"])
        relations = by_family.get(family_id) or []
        if not relations:
            continue
        if all(
            remaining_unmapped_allowed(operand)
            for relation in relations
            for operand in (
                list(relation.get("item_inputs") or [])
                + list(relation.get("item_outputs") or [])
                + list(relation.get("fluid_inputs") or [])
                + list(relation.get("fluid_outputs") or [])
            )
        ):
            selected.add(family_id)
    if selected != set(common.LOCK_FAMILY_IDS):
        raise ValueError("T49 identity catalog lock set is not the 5 Bath remainder families")
    return selected


def build() -> dict[str, Any]:
    work_set = common.load_json(common.WORK_SET)
    source = common.load_json(common.SOURCE)
    lock_ids = lock_family_ids()
    consumed: dict[str, dict[str, Any]] = {}
    for relation in source.get("relations") or []:
        if str(relation.get("family_id") or "") not in lock_ids:
            continue
        for operand in list(relation.get("item_inputs") or []) + list(
            relation.get("item_outputs") or []
        ):
            runtime = str(operand.get("runtime_id") or "")
            source_row = operand.get("source") or {}
            if not runtime:
                continue
            common.assert_runtime_id(runtime, consume=False)
            consumed[runtime] = {
                "form": operand.get("form"),
                "mapping": operand.get("mapping"),
                "material": operand.get("material"),
                "runtime_id": runtime,
                "source_item": source_row.get("item"),
                "source_meta": source_row.get("meta"),
            }
    consumed_rows = [consumed[key] for key in sorted(consumed)]
    return {
        "consumed_identities": consumed_rows,
        "covered_family_count": len(lock_ids),
        "generated_by": "python tools/build_t49_identity_catalog.py",
        "identities": [],
        "identity_count": 0,
        "kind_counts": {},
        "lock_family_count": len(lock_ids),
        "lock_family_ids": sorted(lock_ids),
        "note": (
            "T49 consumes T48 tiny_washed_crushed_ore mappings. "
            "No new identity kinds are registered."
        ),
        "reused_alias_count": len(consumed_rows),
        "schema_version": 1,
        "source_revision": common.SOURCE_REVISION,
        "status": "T49_IDENTITY_CATALOG",
        "variant_count": 0,
        "work_set_family_count": len(work_set.get("families") or []),
    }


def main(argv: list[str] | None = None) -> int:
    return common.run_managed(
        "Freeze the T49 Bath identity catalog",
        OUTPUT,
        build=build,
        argv=argv,
    )


if __name__ == "__main__":
    raise SystemExit(main())
