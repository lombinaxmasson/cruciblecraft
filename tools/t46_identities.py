#!/usr/bin/env python3
"""Exact MTE/fluid identity helpers for the T46 Bath wave."""
from __future__ import annotations

import hashlib
import re
from typing import Any

from tools import t46_common as common

SOURCE_ITEM = "gregtech:gt.multitileentity"
PIPE_SIZE_FORMS = {
    "Tiny": "tiny_fluid_pipe",
    "Small": "small_fluid_pipe",
    "": "fluid_pipe",
    "Large": "large_fluid_pipe",
    "Huge": "huge_fluid_pipe",
}
PIPE_MATERIALS = {
    "Wood": "wood",
    "Treated Wood": "wood_treated",
    "Steel": "steel",
    "Galvanized Steel": "steel_galvanized",
}
WIRE_FORMS = {
    1: "wire",
    2: "double_wire",
    4: "quadruple_wire",
    8: "octuple_wire",
    12: "dodecuple_wire",
    16: "hexadecuple_wire",
}
CABLE_FORMS = {
    1: "cable",
    2: "double_cable",
    4: "quadruple_cable",
    8: "octuple_cable",
    12: "dodecuple_cable",
}
PIPE_RE = re.compile(
    r"^(Tiny |Small |Large |Huge |Quadruple |Nonuple )?"
    r"(Wood|Treated Wood|Steel|Galvanized Steel) Fluid Pipe$"
)
WIRE_RE = re.compile(r"^(\d+)x (Lead|Gold) (Wire|Cable)$")
PANEL_KINDS = {
    "Concrete Panel": "concrete_panel",
    "C-Foam Panel": "cfoam_panel",
    "Asphalt Panel": "asphalt_panel",
}
FLUID_COLORS = {
    "black": "#1D1D21",
    "blue": "#3C44AA",
    "brown": "#835432",
    "cyan": "#169C9C",
    "gray": "#474F52",
    "green": "#5E7C16",
    "lightblue": "#3AB3DA",
    "lightgray": "#9D9D97",
    "lime": "#80C71F",
    "magenta": "#C74EBD",
    "orange": "#F9801D",
    "pink": "#F38BAA",
    "purple": "#8932B8",
    "red": "#B02E26",
    "white": "#F9FFFE",
    "yellow": "#FED83D",
}
B0_OIL_ROUTES = {
    "cruciblecraft:hemp_oil": "item:minecraft:wheat",
    "cruciblecraft:lin_oil": "item:minecraft:wheat_seeds",
    "cruciblecraft:molten_midasium": "item:cruciblecraft:midasium/rock",
    "cruciblecraft:nut_oil": "item:minecraft:cocoa_beans",
    "cruciblecraft:olive_oil": "item:minecraft:apple",
    "cruciblecraft:seed_oil": "item:minecraft:wheat_seeds",
    "cruciblecraft:sunflower_oil": "item:minecraft:sunflower",
}
B0_OIL_ROUTE_NOTES = {
    "cruciblecraft:hemp_oil": "DESIGN_POLICY: vanilla wheat stands in for hemp fiber; no CC hemp crop.",
    "cruciblecraft:lin_oil": "SOURCE_DERIVED: linseed analog uses wheat seeds already in the overworld.",
    "cruciblecraft:molten_midasium": "SOURCE_BACKED: midasium rock is already in T45 typed B0.",
    "cruciblecraft:nut_oil": "SOURCE_DERIVED: cocoa beans are the legal B0 nut crop.",
    "cruciblecraft:olive_oil": "DESIGN_POLICY: vanilla has no olive; oak apples are the overworld fruit analog.",
    "cruciblecraft:seed_oil": "SOURCE_DERIVED: wheat seeds are the legal B0 seed crop.",
    "cruciblecraft:sunflower_oil": "SOURCE_BACKED: vanilla sunflower is an overworld harvestable.",
}
DYE_FLOWER_ITEMS = {
    "black": "minecraft:wither_rose",
    "blue": "minecraft:cornflower",
    "brown": "minecraft:cocoa_beans",
    "cyan": "minecraft:pitcher_plant",
    "gray": "minecraft:flint",
    "green": "minecraft:cactus",
    "lightblue": "minecraft:blue_orchid",
    "lightgray": "minecraft:azure_bluet",
    "lime": "minecraft:sea_pickle",
    "magenta": "minecraft:allium",
    "orange": "minecraft:orange_tulip",
    "pink": "minecraft:pink_tulip",
    "purple": "minecraft:lilac",
    "red": "minecraft:poppy",
    "white": "minecraft:lily_of_the_valley",
    "yellow": "minecraft:dandelion",
}
VANILLA_OVERWORLD_HARVESTABLES = frozenset(
    {
        "item:minecraft:wheat",
        "item:minecraft:wheat_seeds",
        "item:minecraft:sunflower",
        "item:minecraft:apple",
        "item:minecraft:ink_sac",
        "item:minecraft:wither_rose",
        "item:minecraft:cornflower",
        "item:minecraft:pitcher_plant",
        "item:minecraft:cactus",
        "item:minecraft:blue_orchid",
        "item:minecraft:azure_bluet",
        "item:minecraft:sea_pickle",
        "item:minecraft:allium",
        "item:minecraft:orange_tulip",
        "item:minecraft:pink_tulip",
        "item:minecraft:lilac",
        "item:minecraft:poppy",
        "item:minecraft:lily_of_the_valley",
        "item:minecraft:dandelion",
    }
)
PANEL_ZH = {
    "concrete_panel": "混凝土板",
    "cfoam_panel": "C-Foam 板",
    "asphalt_panel": "沥青板",
}


def fluid_runtime_id(source_fluid: str) -> str:
    if source_fluid == "squidink":
        return "cruciblecraft:squid_ink"
    path = source_fluid.replace(".", "_")
    return f"cruciblecraft:{path}"


def fluid_material_id(source_fluid: str) -> str:
    return fluid_runtime_id(source_fluid).split(":", 1)[1]


def fluid_english_name(source_fluid: str) -> str:
    if source_fluid == "indigo":
        return "Indigo"
    if source_fluid == "squidink":
        return "Squid ink"
    parts = source_fluid.split(".")
    if parts[0] == "dye" and len(parts) == 3:
        kind = "Flower" if parts[1] == "flower" else "Water-mixed"
        color = parts[2].replace("light", "light ").title()
        return f"{kind} {color} dye"
    return source_fluid


def fluid_chinese_name(source_fluid: str) -> str:
    if source_fluid == "indigo":
        return "靛蓝"
    if source_fluid == "squidink":
        return "鱿鱼墨"
    parts = source_fluid.split(".")
    colors = {
        "black": "黑色",
        "blue": "蓝色",
        "brown": "棕色",
        "cyan": "青色",
        "gray": "灰色",
        "green": "绿色",
        "lightblue": "淡蓝色",
        "lightgray": "淡灰色",
        "lime": "黄绿色",
        "magenta": "品红色",
        "orange": "橙色",
        "pink": "粉红色",
        "purple": "紫色",
        "red": "红色",
        "white": "白色",
        "yellow": "黄色",
    }
    if parts[0] == "dye" and len(parts) == 3:
        kind = "花卉" if parts[1] == "flower" else "水混"
        return f"{kind}{colors.get(parts[2], parts[2])}染料"
    return source_fluid


def fluid_color(source_fluid: str) -> str:
    if source_fluid == "indigo":
        return "#4B0082"
    if source_fluid == "squidink":
        return "#1B1B2A"
    color = source_fluid.rsplit(".", 1)[-1]
    return FLUID_COLORS.get(color, "#7A7A7A")


def existing_pipe_runtime(english_name: str) -> str | None:
    match = PIPE_RE.fullmatch(english_name)
    if match is None:
        return None
    size = (match.group(1) or "").strip()
    material = PIPE_MATERIALS[match.group(2)]
    form = PIPE_SIZE_FORMS.get(size)
    if form is None:
        return None
    return f"cruciblecraft:{material}/{form}"


def existing_wire_runtime(english_name: str) -> str | None:
    match = WIRE_RE.fullmatch(english_name)
    if match is None:
        return None
    count = int(match.group(1))
    material = match.group(2).lower()
    kind = match.group(3)
    forms = WIRE_FORMS if kind == "Wire" else CABLE_FORMS
    form = forms.get(count)
    if form is None:
        return None
    return f"cruciblecraft:{material}/{form}"


def existing_runtime(english_name: str) -> str | None:
    return existing_pipe_runtime(english_name) or existing_wire_runtime(english_name)


def panel_kind(english_name: str) -> str | None:
    return PANEL_KINDS.get(english_name)


def catalog_registry_path(meta: int) -> str:
    return common.mte_registry_path(SOURCE_ITEM, meta)


def identity_row(
    *,
    meta: int,
    english_name: str,
    input_count: int,
    output_count: int,
) -> dict[str, Any]:
    existing = existing_runtime(english_name)
    kind = panel_kind(english_name) or "mte_item"
    if existing:
        runtime_id = existing
        registry_kind = "existing_item"
        mapping_class = "exact_item"
        registry_path = existing.split(":", 1)[1]
        chinese = english_name
    else:
        runtime_id = common.mte_runtime_id(SOURCE_ITEM, meta)
        registry_kind = "item"
        mapping_class = "exact_item"
        registry_path = catalog_registry_path(meta)
        if kind in PANEL_ZH:
            chinese = f"{PANEL_ZH[kind]} #{meta}"
        else:
            chinese = f"{english_name} #{meta}"
    return {
        "acquisition_authority": "T46",
        "chinese_name": chinese,
        "display_requirements": {
            "distinguishable": True,
            "holdable": True,
            "model": "item/generated",
        },
        "english_name": english_name,
        "input_count": input_count,
        "kind": kind,
        "mapping_class": mapping_class,
        "meta": meta,
        "output_count": output_count,
        "registry_kind": registry_kind,
        "registry_path": registry_path,
        "runtime_id": runtime_id,
        "source_evidence": "gt6_dump/gt6_recipe_dump/maps/gt.recipe.bath.json",
        "source_item": SOURCE_ITEM,
        "source_semantic_name": english_name,
    }


def identity_source_keys(meta: int) -> list[str]:
    suffix = f"@{meta}"
    return [
        f"T46|item:{SOURCE_ITEM}{suffix}",
        f"mte:{SOURCE_ITEM}{suffix}",
    ]


def sha256_text(payload: str) -> str:
    return hashlib.sha256(payload.encode("utf-8")).hexdigest()
