#!/usr/bin/env python3
"""Identity overlay helpers for the bath/remainder Bath remainder wave."""
from __future__ import annotations

from typing import Any

from tools import block_object_common as block_object
from tools import bath_mte_common as bath_mte
from tools import bath_remainder_common as common

VANILLA_DYE_COLORS = (
    "white",
    "orange",
    "magenta",
    "light_blue",
    "yellow",
    "lime",
    "pink",
    "gray",
    "light_gray",
    "cyan",
    "purple",
    "blue",
    "brown",
    "green",
    "red",
    "black",
)
VANILLA_COLOR_META_ITEMS = {
    "minecraft:carpet": "minecraft:{color}_carpet",
    "minecraft:stained_glass": "minecraft:{color}_stained_glass",
    "minecraft:stained_glass_pane": "minecraft:{color}_stained_glass_pane",
    "minecraft:stained_hardened_clay": "minecraft:{color}_terracotta",
    "minecraft:wool": "minecraft:{color}_wool",
}
VANILLA_SPECIAL_META = {
    ("minecraft:golden_apple", 1): "minecraft:enchanted_golden_apple",
    ("minecraft:sand", 1): "minecraft:red_sand",
}
# 1.12 ids that 1.21 no longer registers. Java Ingredient.getItems() cannot
# resolve them, so CompactRecipeShardRouter treats those relations as unindexed
# overflow and shard_count collapses (exact 189 -> 185).
VANILLA_RENAMES = {
    "minecraft:hardened_clay": "minecraft:terracotta",
    "minecraft:grass": "minecraft:grass_block",
    "minecraft:quartz_ore": "minecraft:nether_quartz_ore",
    "minecraft:reeds": "minecraft:sugar_cane",
    # 1.12 melon is the slice; 1.21 minecraft:melon is the block.
    "minecraft:melon": "minecraft:melon_slice",
    "minecraft:speckled_melon": "minecraft:glistering_melon_slice",
}


def load_mte_runtime_map() -> dict[tuple[str, int], str]:
    catalog = common.load_json(common.BATH_MTE_MTE_CATALOG)
    mapped: dict[tuple[str, int], str] = {}
    for identity in catalog.get("identities") or []:
        mapped[(str(identity["source_item"]), int(identity["meta"]))] = str(
            identity["runtime_id"]
        )
    return mapped


def block_kind(source_item: str) -> str:
    item = str(source_item)
    if ".beam." in item:
        return "log"
    return block_object.object_kind(item)


def block_texture(source_item: str, meta: int) -> str:
    item = str(source_item)
    if "planks" in item:
        return "minecraft:block/oak_planks"
    if ".beam." in item:
        return "minecraft:block/oak_log"
    if "grass" in item:
        return "minecraft:block/grass_block_top"
    return block_object.texture_for(item, meta)


def load_block_runtime_map() -> dict[tuple[str, int], str]:
    catalog = common.load_json(block_object.BLOCK_CATALOG)
    mapped: dict[tuple[str, int], str] = {}
    for identity in catalog.get("identities") or []:
        mapped[(str(identity["source_item"]), int(identity["meta"]))] = str(
            identity["runtime_id"]
        )
    catalog_path = (
        common.IDENTITY_CATALOG
        if common.IDENTITY_CATALOG.is_file()
        else common.BUNDLED_IDENTITY_CATALOG
    )
    if catalog_path.is_file():
        catalog_document = common.load_json(catalog_path)
        for identity in catalog_document.get("identities") or []:
            if str(identity.get("kind") or "") != "block":
                continue
            mapped[(str(identity["source_item"]), int(identity["meta"]))] = str(
                identity["runtime_id"]
            )
    return mapped


def load_fluid_base_overlay() -> dict[str, str]:
    overlay: dict[str, str] = {}
    t22 = common.load_json(common.T22_5_FLUID_MAPPING)
    for row in t22.get("mapping") or []:
        if not isinstance(row, dict):
            continue
        source = str(row.get("source_fluid") or row.get("fluid") or "")
        runtime = str(row.get("cc_fluid_id") or row.get("runtime_id") or "")
        if source and runtime:
            overlay[source] = runtime
    bath_mte_fluids = common.load_json(common.BATH_MTE_FLUID_MAPPING)
    for row in bath_mte_fluids.get("mapping") or []:
        if not isinstance(row, dict):
            continue
        source = str(row.get("source_fluid") or "")
        runtime = str(row.get("cc_fluid_id") or "")
        if source and runtime:
            overlay[source] = runtime
    return overlay


def load_bath_remainder_item_overlay() -> dict[tuple[str, int | None], dict[str, Any]]:
    if not common.IDENTITY_CATALOG.is_file():
        return {}
    catalog = common.load_json(common.IDENTITY_CATALOG)
    mapped: dict[tuple[str, int | None], dict[str, Any]] = {}
    for identity in catalog.get("identities") or []:
        source_item = str(identity.get("source_item") or "")
        meta = identity.get("meta")
        key = (source_item, int(meta) if isinstance(meta, int) else None)
        mapped[key] = identity
    return mapped


def vanilla_meta_runtime(item_id: str, meta: Any) -> str | None:
    """Map GT6 flattened vanilla metas onto 1.21 split item ids."""
    item = str(item_id)
    renamed = VANILLA_RENAMES.get(item)
    if renamed is not None and (meta is None or meta in (0, "0")):
        return renamed
    if isinstance(meta, int) and (item, meta) in VANILLA_SPECIAL_META:
        return VANILLA_SPECIAL_META[(item, meta)]
    template = VANILLA_COLOR_META_ITEMS.get(item)
    if template and isinstance(meta, int) and 0 <= meta < len(VANILLA_DYE_COLORS):
        return template.format(color=VANILLA_DYE_COLORS[meta])
    return None


def vanilla_wildcard_meta(item_id: str, meta: Any) -> bool:
    return str(item_id) in VANILLA_COLOR_META_ITEMS and meta == "*"


def load_bath_remainder_fluid_overlay() -> dict[str, str]:
    if not common.FLUID_MAPPING.is_file():
        return {}
    mapping = common.load_json(common.FLUID_MAPPING)
    return {
        str(row["source_fluid"]): str(row["cc_fluid_id"])
        for row in mapping.get("mapping") or []
        if isinstance(row, dict) and row.get("source_fluid") and row.get("cc_fluid_id")
    }


def classify_source_item(item_id: str) -> str:
    if item_id == "gregtech:gt.multitileentity":
        return "mte"
    if item_id.startswith("gregtech:gt.block."):
        return "block"
    if item_id.startswith("gregtech:gt.multiitem."):
        return "multiitem"
    if "tool" in item_id.lower() and item_id.startswith("gregtech:"):
        return "tool_head"
    if item_id.startswith("gregtech:gt.meta."):
        return "gt_prefix"
    if item_id.startswith("minecraft:") or item_id.startswith("cruciblecraft:"):
        return "runtime"
    return "other"
