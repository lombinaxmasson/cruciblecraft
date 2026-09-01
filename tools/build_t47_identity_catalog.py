#!/usr/bin/env python3
"""Freeze T47 remainder identity catalog for lock-needed non-MTE objects."""
from __future__ import annotations

import sys
from collections import defaultdict
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35
from tools import t45_common as t45
from tools import t47_common as common
from tools import t47_identities as identities

OUTPUT = common.IDENTITY_CATALOG
BUNDLED = common.BUNDLED_IDENTITY_CATALOG


def _operands(relation: dict[str, Any]) -> list[dict[str, Any]]:
    return (
        list(relation.get("item_inputs") or [])
        + list(relation.get("item_outputs") or [])
        + list(relation.get("fluid_inputs") or [])
        + list(relation.get("fluid_outputs") or [])
    )


def remaining_unmapped_allowed(operand: dict[str, Any]) -> bool:
    runtime = operand.get("runtime_id")
    if runtime and not str(runtime).startswith(("gregtech:", "gregapi:")):
        return True
    source = operand.get("source") or {}
    item = str(source.get("item") or "")
    if item.startswith("gregtech:gt.block."):
        return True
    return str(source.get("fluid") or "") == "potion.fireresistance.long"


def lock_family_ids() -> set[str]:
    work_set = common.load_json(common.WORK_SET)
    source = common.load_json(common.SOURCE)
    by_family: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for relation in source.get("relations") or []:
        by_family[str(relation["family_id"])].append(relation)
    selected: set[str] = set()
    for row in work_set.get("families") or []:
        family_id = str(row["family_id"])
        ok = True
        for relation in by_family.get(family_id) or []:
            for operand in _operands(relation):
                if not remaining_unmapped_allowed(operand):
                    ok = False
                    break
            if not ok:
                break
        if ok:
            selected.add(family_id)
    if len(selected) < common.MIN_PRODUCTION_FAMILIES:
        raise ValueError(
            f"T47 identity lock set {len(selected)} < {common.MIN_PRODUCTION_FAMILIES}"
        )
    return selected


def build() -> dict[str, Any]:
    work_set = common.load_json(common.WORK_SET)
    source = common.load_json(common.SOURCE)
    t45_map = {
        (str(row["source_item"]), int(row["meta"])): str(row["runtime_id"])
        for row in common.load_json(t45.BLOCK_CATALOG).get("identities") or []
    }
    lock_ids = lock_family_ids()
    by_family: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for relation in source.get("relations") or []:
        by_family[str(relation["family_id"])].append(relation)
    wanted: dict[tuple[str, int], dict[str, int]] = {}
    reused = 0
    for row in work_set.get("families") or []:
        family_id = str(row["family_id"])
        if family_id not in lock_ids:
            continue
        for relation in by_family.get(family_id) or []:
            for operand in list(relation.get("item_inputs") or []) + list(
                relation.get("item_outputs") or []
            ):
                source_row = operand.get("source") or {}
                item = str(source_row.get("item") or "")
                if not item.startswith("gregtech:gt.block."):
                    continue
                meta = int(source_row["meta"]) if isinstance(source_row.get("meta"), int) else 0
                key = (item, meta)
                counts = wanted.setdefault(key, {"input_count": 0, "output_count": 0})
                if operand in (relation.get("item_inputs") or []):
                    counts["input_count"] += 1
                else:
                    counts["output_count"] += 1
    identities_out: list[dict[str, Any]] = []
    for (item, meta), counts in sorted(wanted.items()):
        existing = t45_map.get((item, meta))
        if existing:
            reused += 1
            continue
        kind = identities.block_kind(item)
        identities_out.append(
            {
                "acquisition_authority": "T47",
                "behavior": kind,
                "chinese_name": t45.chinese_name(item, meta),
                "display_requirements": {
                    "distinguishable": True,
                    "holdable": True,
                    "model": "block/cube_all",
                },
                "english_name": t45.english_name(item, meta),
                "input_count": counts["input_count"],
                "kind": "block",
                "mapping_class": "exact_item",
                "meta": meta,
                "occurrence_count": counts["input_count"] + counts["output_count"],
                "output_count": counts["output_count"],
                "registry_kind": "t47_block",
                "registry_path": t45.registry_path(item, meta),
                "runtime_id": t45.runtime_id(item, meta),
                "source_evidence": "gt6_dump/gt6_recipe_dump/maps/gt.recipe.bath.json",
                "source_item": item,
                "texture": identities.block_texture(item, meta),
            }
        )
    return {
        "generated_by": "python tools/build_t47_identity_catalog.py",
        "identities": identities_out,
        "identity_count": len(identities_out),
        "kind_counts": {"block": len(identities_out)},
        "lock_family_count": len(lock_ids),
        "reused_t45_block_identities": reused,
        "schema_version": 1,
        "source_revision": common.SOURCE_REVISION,
        "status": "T47_IDENTITY_CATALOG",
        "variant_count": len(identities_out),
    }


def write() -> dict[str, Any]:
    document = build()
    t35.write_stable(OUTPUT, document)
    BUNDLED.parent.mkdir(parents=True, exist_ok=True)
    t35.write_stable(BUNDLED, document)
    return document


def main(argv: list[str] | None = None) -> int:
    return common.run_managed(
        "Freeze the T47 remainder identity catalog",
        OUTPUT,
        build=build,
        write=write,
        argv=argv,
    )


if __name__ == "__main__":
    raise SystemExit(main())
