#!/usr/bin/env python3
"""Freeze the T45 source-backed block-object catalog used by Java registration."""
from __future__ import annotations

import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35
from tools import t45_common as common

OUTPUT = common.BLOCK_CATALOG
BUNDLED = common.BUNDLED_BLOCK_CATALOG


def _identities_from_work_set() -> list[dict[str, Any]]:
    work_set = common.load_json(common.WORK_SET)
    family_ids = {str(value) for value in work_set.get("family_ids") or []}
    snapshot = common.load_json(common.T42_SNAPSHOT)
    if snapshot.get("source_revision") != common.SOURCE_REVISION:
        raise ValueError("T42 family operand snapshot source_revision drifted")
    items = list((snapshot.get("dictionaries") or {}).get("items") or [])
    seen: dict[tuple[str, int], dict[str, Any]] = {}
    for family in snapshot.get("families") or []:
        if str(family.get("family_id") or "") not in family_ids:
            continue
        for relation in family.get("relations") or []:
            for operand in relation.get("operands") or []:
                item_index = operand[1]
                meta = operand[3]
                if item_index is None or not isinstance(meta, int):
                    continue
                item = items[item_index]
                if not str(item).startswith("gregtech:gt.block."):
                    continue
                kind = common.object_kind(item)
                if kind == "sands":
                    raise ValueError(f"sands identity leaked into T45 catalog: {item}@{meta}")
                key = (str(item), int(meta))
                seen[key] = {
                    "behavior": kind,
                    "chinese_name": common.chinese_name(item, meta),
                    "english_name": common.english_name(item, meta),
                    "kind": kind,
                    "meta": int(meta),
                    "registry_path": common.registry_path(item, meta),
                    "runtime_id": common.runtime_id(item, meta),
                    "source_item": item,
                    "texture": common.texture_for(item, meta),
                }
    identities = [seen[key] for key in sorted(seen)]
    if not identities:
        raise ValueError("T45 block catalog is empty")
    return identities


def build() -> dict[str, Any]:
    identities = _identities_from_work_set()
    kinds = {}
    for identity in identities:
        kinds[identity["kind"]] = kinds.get(identity["kind"], 0) + 1
    document = {
        "generated_by": "python tools/build_t45_block_object_catalog.py",
        "identities": identities,
        "identity_count": len(identities),
        "kind_counts": dict(sorted(kinds.items())),
        "schema_version": 1,
        "source_revision": common.SOURCE_REVISION,
        "status": "T45_BLOCK_OBJECT_CATALOG",
        "variant_count": len(identities),
    }
    return document


def write() -> dict[str, Any]:
    document = build()
    t35.write_stable(OUTPUT, document)
    BUNDLED.parent.mkdir(parents=True, exist_ok=True)
    t35.write_stable(BUNDLED, document)
    return document


def check() -> list[str]:
    document = build()
    errors = common.check_document(OUTPUT, document)
    if BUNDLED.is_file():
        errors.extend(common.check_document(BUNDLED, document))
    else:
        errors.append("missing bundled gt_block_object_catalog.json")
    return errors


def main(argv: list[str] | None = None) -> int:
    return common.run_managed(
        "Freeze the T45 block-object catalog",
        OUTPUT,
        build=build,
        write=write,
        check=check,
        argv=argv,
    )


if __name__ == "__main__":
    raise SystemExit(main())
