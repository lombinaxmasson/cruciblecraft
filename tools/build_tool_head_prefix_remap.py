#!/usr/bin/env python3
"""Freeze the single bath ∪ semantic tool-head prefix remap."""
from __future__ import annotations

import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35
from tools import t48_common as common
from tools import tool_head_prefix as thp

OUTPUT = thp.REMAP_TOOLS
BUNDLED = thp.BUNDLED_REMAP
REQUIRED_FORMS = thp.REQUIRED_FORMS


def build() -> dict[str, Any]:
    return thp.build_remap()


def _check_payload(document: dict[str, Any]) -> list[str]:
    errors: list[str] = []
    if document.get("status") != "TOOL_HEAD_PREFIX_REMAP":
        errors.append("tool-head remap status drifted")
    if document.get("source_revision") != common.SOURCE_REVISION:
        errors.append("tool-head remap source revision drifted")
    source_items = list(document.get("source_item_set") or [])
    if len(source_items) < 36:
        errors.append(
            f"tool-head remap source_item set is {len(source_items)}, expected at least 36"
        )
    for item in source_items:
        if not str(item).startswith("gregtech:gt.meta.toolHead"):
            errors.append(f"tool-head remap source_item is not toolHead*: {item}")
        if str(item) in {
            "gregtech:gt.meta.arrowGtWood",
            "gregtech:gt.meta.arrowGtPlastic",
        }:
            errors.append(f"tool-head remap merged a non-toolHead source_item: {item}")
    prefixes = document.get("prefixes") or {}
    if "arrow_gt_wood" in prefixes or "arrow_gt_plastic" in prefixes:
        errors.append("tool-head remap reused arrowGt* prefixes for toolHeadArrow")
    mapped = list(document.get("mapped") or [])
    remainder = list(document.get("remainder") or [])
    seen: set[tuple[str, int]] = set()
    winners: dict[tuple[str, int], str] = {}
    for row in mapped:
        key = (str(row.get("source_item") or ""), int(row["meta"]))
        if key in seen:
            errors.append(f"duplicate mapped tool-head key {key}")
        seen.add(key)
        runtime = str(row.get("runtime_id") or "")
        if key in winners and winners[key] != runtime:
            errors.append(f"tool-head winner conflict for {key}")
        winners[key] = runtime
        if runtime.startswith("cruciblecraft:gt_tool_head/"):
            errors.append(f"mapped tool-head kept unique item id: {runtime}")
        material = str(row.get("material") or "")
        prefix = str(row.get("prefix") or "")
        expected = f"cruciblecraft:{material}/{prefix}"
        if runtime != expected:
            errors.append(f"mapped runtime drifted: {runtime} != {expected}")
    for row in remainder:
        if not row.get("reason"):
            errors.append(f"remainder row missing reason: {row}")
        if row.get("runtime_id") is None and row.get("material"):
            errors.append(f"remainder row guessed a material: {row}")
    counts = document.get("counts") or {}
    if int(counts.get("mapped") or 0) != len(mapped):
        errors.append("mapped count drifted")
    if int(counts.get("remainder") or 0) != len(remainder):
        errors.append("remainder count drifted")
    return errors


def write() -> dict[str, Any]:
    document = build()
    errors = _check_payload(document)
    if errors:
        raise ValueError("; ".join(errors))
    t35.write_stable(OUTPUT, document)
    BUNDLED.parent.mkdir(parents=True, exist_ok=True)
    t35.write_stable(BUNDLED, document)
    t35.write_stable(REQUIRED_FORMS, thp.required_forms_document(document))
    thp.write_prefix_json(document)
    thp.copy_source_backed_textures(document)
    thp.write_zh_prefix_names(document)
    thp.clear_cache()
    return document


def check() -> list[str]:
    expected = build()
    errors = common.check_document(OUTPUT, expected)
    if BUNDLED.is_file():
        bundled = common.load_json(BUNDLED)
        if bundled != expected:
            errors.append("bundled tool-head remap drifted from tools remap")
    else:
        errors.append(f"missing bundled remap {t35.relative(BUNDLED)}")
    errors.extend(_check_payload(expected))
    required = thp.required_forms_document(expected)
    errors.extend(common.check_document(REQUIRED_FORMS, required))
    for prefix, meta in (expected.get("prefixes") or {}).items():
        prefix_path = thp.PREFIX_ROOT / f"{prefix}.json"
        if not prefix_path.is_file():
            errors.append(f"missing material prefix JSON: {t35.relative(prefix_path)}")
            continue
        committed = common.load_json(prefix_path)
        wanted = thp.prefix_document(prefix, str(meta.get("gt_prefix") or ""))
        if committed != wanted:
            errors.append(f"material prefix JSON drifted: {prefix}")
        texture = thp.TEXTURE_DEST / f"{prefix}.png"
        if not texture.is_file():
            errors.append(f"missing SOURCE_BACKED tool-head texture: {t35.relative(texture)}")
        model_texture = str(committed.get("model_texture") or "")
        if model_texture == "minecraft:item/iron_ingot":
            errors.append(f"{prefix} used vanilla ingot texture")
        if not model_texture.startswith("cruciblecraft:item/material/"):
            errors.append(f"{prefix} model_texture is not a material prefix texture")
    if thp.PREFIX_INDEX.is_file():
        index = common.load_json(thp.PREFIX_INDEX)
        for prefix in expected.get("prefixes") or {}:
            if f"{prefix}.json" not in index:
                errors.append(f"{prefix}.json missing from material_prefixes/index.json")
    return errors


def main(argv: list[str] | None = None) -> int:
    return common.run_managed(
        "Freeze the tool-head prefix remap",
        OUTPUT,
        build=build,
        write=write,
        check=check,
        argv=argv,
    )


if __name__ == "__main__":
    raise SystemExit(main())
