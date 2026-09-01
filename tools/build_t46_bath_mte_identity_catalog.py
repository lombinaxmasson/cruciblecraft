#!/usr/bin/env python3
"""Freeze the T46 Bath MTE identity catalog (118 exact source metas)."""
from __future__ import annotations

import argparse
import json
import sys
from collections import Counter
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35
from tools import t46_common as common
from tools import t46_identities as identities

OUTPUT = common.MTE_CATALOG
BUNDLED = common.BUNDLED_MTE_CATALOG


def _cohort_objects() -> list[tuple[str, int]]:
    selected = {str(row["family_id"]) for row in common.overlay_bath_mte_families()}
    blocker = common.load_json(common.T42_BLOCKER)
    seen: dict[tuple[str, int], None] = {}
    for row in blocker.get("families") or []:
        if str(row.get("family_id") or "") not in selected:
            continue
        for obj in row.get("unique_objects") or []:
            item, _, meta = str(obj).partition("@")
            seen[(item, int(meta))] = None
    keys = sorted(seen)
    if len(keys) != common.EXPECTED_MTE_METAS:
        raise ValueError(f"T46 unique MTE metas {len(keys)} != 118")
    if any(item != identities.SOURCE_ITEM for item, _meta in keys):
        raise ValueError("T46 MTE catalog saw a non-gt.multitileentity source item")
    return keys


def _counts_from_snapshot() -> dict[int, dict[str, int]]:
    selected = {str(row["family_id"]) for row in common.overlay_bath_mte_families()}
    snapshot = common.load_json(common.T42_SNAPSHOT)
    items = list((snapshot.get("dictionaries") or {}).get("items") or [])
    counts: dict[int, dict[str, int]] = {}
    for family in snapshot.get("families") or []:
        if str(family.get("family_id") or "") not in selected:
            continue
        for relation in family.get("relations") or []:
            for operand in relation.get("operands") or []:
                item_index = operand[1]
                meta = operand[3]
                side = operand[0]
                if item_index is None or not isinstance(meta, int):
                    continue
                item = items[item_index]
                if item != identities.SOURCE_ITEM:
                    continue
                row = counts.setdefault(int(meta), {"input_count": 0, "output_count": 0})
                if side == 0:
                    row["input_count"] += 1
                elif side == 1:
                    row["output_count"] += 1
    return counts


def _names_from_dump() -> dict[int, str]:
    if not common.dump_present():
        raise OSError("missing GT6 Bath dump required for full replay catalog names")
    document = json.loads(common.BATH_DUMP.read_text(encoding="utf-8"))
    names: dict[int, str] = {}
    for recipe in document.get("recipes") or []:
        for side in ("inputs", "outputs"):
            for item in recipe.get(side) or []:
                if not item:
                    continue
                if item.get("item") != identities.SOURCE_ITEM:
                    continue
                meta = item.get("meta")
                if not isinstance(meta, int):
                    continue
                display = item.get("displayName")
                if display:
                    names[meta] = str(display)
    return names


def build(*, names: dict[int, str] | None = None) -> dict[str, Any]:
    objects = _cohort_objects()
    counts = _counts_from_snapshot()
    if names is None:
        if OUTPUT.is_file():
            committed = common.load_json(OUTPUT)
            names = {
                int(row["meta"]): str(row["english_name"])
                for row in committed.get("identities") or []
            }
        else:
            names = _names_from_dump()
    rows = []
    for item, meta in objects:
        english = names.get(meta)
        if not english:
            raise ValueError(f"missing source display name for {item}@{meta}")
        usage = counts.get(meta) or {"input_count": 0, "output_count": 0}
        rows.append(
            identities.identity_row(
                meta=meta,
                english_name=english,
                input_count=int(usage["input_count"]),
                output_count=int(usage["output_count"]),
            )
        )
    runtime_ids = [row["runtime_id"] for row in rows]
    if len(set(runtime_ids)) != len(runtime_ids):
        raise ValueError("T46 MTE catalog runtime identities collided")
    kind_counts = Counter(row["kind"] for row in rows)
    registry_counts = Counter(row["registry_kind"] for row in rows)
    return {
        "generated_by": "python tools/build_t46_bath_mte_identity_catalog.py",
        "identities": rows,
        "identity_count": len(rows),
        "kind_counts": dict(sorted(kind_counts.items())),
        "registry_kind_counts": dict(sorted(registry_counts.items())),
        "runtime_identity_count": len(rows),
        "schema_version": 1,
        "source_meta_count": len(rows),
        "source_revision": common.SOURCE_REVISION,
        "status": "T46_BATH_MTE_IDENTITY_CATALOG",
        "variant_count": len(rows),
    }


MODEL_TEXTURES = {
    "concrete_panel": "minecraft:block/stone",
    "cfoam_panel": "minecraft:block/white_concrete",
    "asphalt_panel": "minecraft:block/black_concrete",
}


def _write_item_models(document: dict[str, Any]) -> None:
    root = common.ROOT / "src/main/resources/assets/cruciblecraft/models/item"
    for identity in document.get("identities") or []:
        if identity.get("registry_kind") != "item":
            continue
        path = root / f"{identity['registry_path']}.json"
        path.parent.mkdir(parents=True, exist_ok=True)
        kind = str(identity.get("kind") or "")
        texture = MODEL_TEXTURES.get(kind, "minecraft:item/iron_ingot")
        path.write_text(
            t35.stable_json(
                {
                    "parent": "minecraft:item/generated",
                    "textures": {"layer0": texture},
                }
            ),
            encoding="utf-8",
            newline="\n",
        )


def write() -> dict[str, Any]:
    names = _names_from_dump() if common.dump_present() else None
    document = build(names=names)
    t35.write_stable(OUTPUT, document)
    BUNDLED.parent.mkdir(parents=True, exist_ok=True)
    t35.write_stable(BUNDLED, document)
    _write_item_models(document)
    return document


def check() -> list[str]:
    errors = common.check_document(OUTPUT, build())
    if BUNDLED.is_file():
        bundled = common.load_json(BUNDLED)
        committed = common.load_json(OUTPUT)
        if bundled != committed:
            errors.append("bundled T46 MTE catalog drifted from tools catalog")
    else:
        errors.append("missing bundled T46 MTE identity catalog")
    return errors


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Freeze T46 Bath MTE identity catalog")
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--full-replay", action="store_true")
    args = parser.parse_args(argv)
    if args.write:
        write()
        print(f"Wrote {common.relative(OUTPUT)}")
        return 0
    if args.check:
        if args.full_replay:
            live = build(names=_names_from_dump())
            errors = common.check_document(OUTPUT, live)
        else:
            errors = check()
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        print(f"{common.relative(OUTPUT)} is current")
        return 0
    parser.error("choose --write or --check")
    return 2


if __name__ == "__main__":
    raise SystemExit(main())
