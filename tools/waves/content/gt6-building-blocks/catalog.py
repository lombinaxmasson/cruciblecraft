#!/usr/bin/env python3
"""GT6 building-block identities that are not in the frozen 365/283 catalogs.

Glass and glow glass were never catalogued. Leftover dummy kind=block rows
(extra slab faces, diggable, sands, moldy/rotten grass bales) stay at the same
runtime ids so recipes keep working; turf (diggable meta 2) is new.
"""
from __future__ import annotations

import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[4]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import census_common as census

OUTPUT = (
    ROOT / "src/main/resources/data/cruciblecraft/gt_building_block_catalog.json"
)
SEMANTIC = ROOT / "src/main/resources/data/cruciblecraft/semantic_object_catalog.json"
STATUS = "GT_BUILDING_BLOCK_CATALOG"
VARIANT_COUNT = 246
PROMOTED_LEFTOVER_COUNT = 21
KEEP_LEFTOVER = {"lilypad_glowtus/white_glowtus"}

# GT6 CS.DYE_NAMES / DYES_INT: Black=0 … White=15
DYES = (
    ("black", "Black", "黑"),
    ("red", "Red", "红"),
    ("green", "Green", "绿"),
    ("brown", "Brown", "棕"),
    ("blue", "Blue", "蓝"),
    ("purple", "Purple", "紫"),
    ("cyan", "Cyan", "青"),
    ("light_gray", "Light Gray", "浅灰"),
    ("gray", "Gray", "灰"),
    ("pink", "Pink", "粉"),
    ("lime", "Lime", "黄绿"),
    ("yellow", "Yellow", "黄"),
    ("light_blue", "Light Blue", "浅蓝"),
    ("magenta", "Magenta", "品红"),
    ("orange", "Orange", "橙"),
    ("white", "White", "白"),
)
SLAB_SIDES = (
    (0, "slab_down", "Down", "下"),
    (1, "slab_up", "Up", "上"),
    (2, "slab_north", "North", "北"),
    (3, "slab_south", "South", "南"),
    (4, "slab_west", "West", "西"),
    (5, "slab_east", "East", "东"),
)
TURF = {
    "behavior": "solid",
    "chinese_name": "草皮",
    "english_name": "Turf",
    "kind": "block",
    "meta": 2,
    "registry_path": "diggable/turf",
    "runtime_id": "cruciblecraft:diggable/turf",
    "source_item": "gregtech:gt.block.diggable",
    "texture": "minecraft:block/dirt",
}


def _row(
    *,
    behavior: str,
    english: str,
    chinese: str,
    meta: int,
    registry_path: str,
    source_item: str,
    texture: str,
    kind: str | None = None,
) -> dict[str, Any]:
    return {
        "behavior": behavior,
        "chinese_name": chinese,
        "english_name": english,
        "kind": kind or behavior,
        "meta": meta,
        "registry_path": registry_path,
        "runtime_id": f"cruciblecraft:{registry_path}",
        "source_item": source_item,
        "texture": texture,
    }


def _colored_family(
    *,
    folder: str,
    english_kind: str,
    chinese_kind: str,
    source: str,
    slab_source: str,
    texture: str,
) -> list[dict[str, Any]]:
    rows: list[dict[str, Any]] = []
    for meta, (slug, english, chinese) in enumerate(DYES):
        rows.append(
            _row(
                behavior="glow_glass" if folder == "glow_glass" else "glass",
                english=f"{english} {english_kind}",
                chinese=f"{chinese}色{chinese_kind}",
                meta=meta,
                registry_path=f"{folder}/{slug}",
                source_item=source,
                texture=texture,
            )
        )
        for slab_meta, side, side_en, side_zh in SLAB_SIDES:
            rows.append(
                _row(
                    behavior="slab",
                    english=f"{english} {english_kind} {side_en} Slab",
                    chinese=f"{chinese}色{chinese_kind}{side_zh}台阶",
                    meta=meta,
                    registry_path=f"{folder}/{slug}/{side}",
                    source_item=f"{slab_source}.{slab_meta}",
                    texture=texture,
                )
            )
    return rows


def leftover_block_rows() -> list[dict[str, Any]]:
    document = census.load_json(SEMANTIC)
    rows: list[dict[str, Any]] = []
    for identity in document.get("identities") or []:
        if str(identity.get("kind") or "") != "block":
            continue
        path = str(identity["registry_path"])
        if path in KEEP_LEFTOVER:
            continue
        source = str(identity["source_item"])
        if ".slab." in source:
            behavior = "slab"
        elif "bale" in source:
            behavior = "bale"
        else:
            behavior = "solid"
        rows.append(
            _row(
                behavior=behavior,
                english=str(identity["english_name"]),
                chinese=str(identity["chinese_name"]),
                meta=int(identity["meta"]),
                registry_path=path,
                source_item=source,
                texture=str(identity.get("texture") or "minecraft:item/iron_ingot"),
                kind="block",
            )
        )
    return rows


def leftover_item_models() -> list[Path]:
    root = ROOT / "src/main/resources/assets/cruciblecraft/models/item"
    return [root / f"{row['registry_path']}.json" for row in leftover_block_rows()]


def identities() -> list[dict[str, Any]]:
    glass = "minecraft:block/white_stained_glass"
    rows = leftover_block_rows()
    rows.append(dict(TURF))
    rows.extend(
        _colored_family(
            folder="glass",
            english_kind="Glass",
            chinese_kind="玻璃",
            source="gregtech:gt.block.glass",
            slab_source="gregtech:gt.block.glass.slab",
            texture=glass,
        )
    )
    rows.extend(
        _colored_family(
            folder="glow_glass",
            english_kind="Glow Glass",
            chinese_kind="荧光玻璃",
            source="gregtech:gt.block.glass.glow",
            slab_source="gregtech:gt.block.glass.glow.slab",
            texture=glass,
        )
    )
    return rows


def document() -> dict[str, Any]:
    rows = identities()
    if len(rows) != VARIANT_COUNT:
        raise ValueError(
            f"building-block catalog drifted: {len(rows)} != {VARIANT_COUNT}"
        )
    leftover = leftover_block_rows()
    if len(leftover) != PROMOTED_LEFTOVER_COUNT:
        raise ValueError(
            f"promoted leftover count drifted: {len(leftover)} != "
            f"{PROMOTED_LEFTOVER_COUNT}"
        )
    seen: set[str] = set()
    for row in rows:
        runtime = str(row["runtime_id"])
        if runtime in seen:
            raise ValueError(f"duplicate building-block runtime {runtime}")
        seen.add(runtime)
    return {
        "generated_by": (
            "python tools/waves/content/gt6-building-blocks/catalog.py"
        ),
        "identities": rows,
        "promoted_leftover_count": PROMOTED_LEFTOVER_COUNT,
        "schema_version": 1,
        "source_revision": census.SOURCE_REVISION,
        "status": STATUS,
        "variant_count": VARIANT_COUNT,
    }


def write() -> dict[str, Any]:
    payload = document()
    census.write_stable(OUTPUT, payload)
    return payload


def check() -> list[str]:
    expected = document()
    if not OUTPUT.is_file():
        return [f"missing {census.relative(OUTPUT)}"]
    actual = census.load_json(OUTPUT)
    drift = census.first_json_diff(expected, actual)
    return [] if drift is None else [f"{census.relative(OUTPUT)} drifted: {drift}"]


def main(argv: list[str] | None = None) -> int:
    args = list(sys.argv[1:] if argv is None else argv)
    if "--check" in args:
        errors = check()
        if errors:
            print("; ".join(errors), file=sys.stderr)
            return 1
        return 0
    write()
    print(f"wrote {census.relative(OUTPUT)} ({VARIANT_COUNT})")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
