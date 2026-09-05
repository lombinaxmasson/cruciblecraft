#!/usr/bin/env python3
"""Extract energy-converter kinds/tiers from pinned Loader_MultiTileEntities."""
from __future__ import annotations

import json
import re
import shutil
from collections import Counter
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
SOURCE_REVISION = "3703e40308c8c030763fd6297dea8b210d2a77b1"
LOADER = (
    ROOT
    / "gt6_code"
    / "gregtech6"
    / "src"
    / "main"
    / "java"
    / "gregtech"
    / "loaders"
    / "b"
    / "Loader_MultiTileEntities.java"
)
GT6_W_TEX = (
    ROOT
    / "gt6_referencable_port_code"
    / "gregtech6_w"
    / "src"
    / "main"
    / "resources"
    / "assets"
    / "gregtech"
    / "textures"
    / "blocks"
    / "machines"
)
DEST_TEX = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "assets"
    / "cruciblecraft"
    / "textures"
    / "block"
    / "machine"
)
DATA = ROOT / "src" / "main" / "resources" / "data" / "cruciblecraft"
CROSS_REF = ROOT / "tools" / "gt6_oredict_cross_reference.json"
FLUIDBED_DUMP = (
    ROOT / "gt6_dump" / "gt6_recipe_dump" / "maps" / "gt.recipe.fuels.fluidbed.json"
)
FLUIDBED_OUT = DATA / "recipe" / "energy" / "fuels_fluidbed"

MATERIAL_MAP = {
    "MT.Brick": "clay_brick",
    "MT.Pb": "lead",
    "MT.Bi": "bismuth",
    "MT.Bronze": "bronze",
    "MT.ArsenicCopper": "arsenic_copper",
    "MT.ArsenicBronze": "arsenic_bronze",
    "MT.Invar": "invar",
    "ANY.Steel": "steel",
    "MT.Cr": "chromium",
    "MT.Ti": "titanium",
    "MT.Netherite": "netherite",
    "ANY.W": "tungsten",
    "MT.TungstenSteel": "tungstensteel",
    "MT.Ta4HfC5": "tantalum_hafnium_carbide",
    "MT.Ultimet": "ultimet",
    "MT.TinAlloy": "tin_alloy",
    "MT.Brass": "brass",
    "MT.IronWood": "ironwood",
    "MT.FierySteel": "fiery_steel",
    "MT.Ir": "iridium",
    "MT.DATA.Electric_T[1]": "steel_galvanized",
    "MT.DATA.Electric_T[2]": "aluminium",
    "MT.DATA.Electric_T[3]": "stainless_steel",
    "MT.DATA.Electric_T[4]": "chromium",
    "MT.DATA.Electric_T[5]": "titanium",
}

KIND_BY_CLASS = {
    "MultiTileEntityGeneratorBrick": "burning_box_brick",
    "MultiTileEntityGeneratorMetal": "burning_box_solid",
    "MultiTileEntityGeneratorLiquid": "burning_box_liquid",
    "MultiTileEntityGeneratorGas": "burning_box_gas",
    "MultiTileEntityGeneratorFluidBed": "burning_box_fluid_bed",
    "MultiTileEntityBoilerTank": "boiler",
    "MultiTileEntityEngineSteam": "steam_engine",
    "MultiTileEntityMotorLiquid": "fuel_engine",
    "MultiTileEntityDynamoElectric": "dynamo",
    "MultiTileEntityMotorElectric": "electric_motor",
}

DENSE_KINDS = {
    "burning_box_solid": "burning_box_solid_dense",
    "burning_box_liquid": "burning_box_liquid_dense",
    "burning_box_gas": "burning_box_gas_dense",
    "burning_box_fluid_bed": "burning_box_fluid_bed_dense",
}

STRONG_KINDS = {
    "boiler": "strong_boiler",
    "steam_engine": "strong_steam_engine",
}

KIND_META: dict[str, dict[str, Any]] = {
    "burning_box_brick": {
        "runtime": "solid_burning_box",
        "fuel_map": "FM.Furnace",
        "accepts": ["ITEM_FURNACE_FUEL"],
        "emits": ["HU"],
        "faces": {"energyInputs": [], "energyOutputs": ["UP"], "fluidInputs": [], "fluidOutputs": []},
        "texture_profile": "burning_box_brick",
        "overlay_active": True,
        "lang_en": "Brick Burning Box",
        "lang_zh": "砖燃烧室",
        "gt6_class": "MultiTileEntityGeneratorBrick",
    },
    "burning_box_solid": {
        "runtime": "solid_burning_box",
        "fuel_map": "FM.Furnace",
        "accepts": ["ITEM_FURNACE_FUEL"],
        "emits": ["HU"],
        "faces": {"energyInputs": [], "energyOutputs": ["UP"], "fluidInputs": [], "fluidOutputs": []},
        "texture_profile": "burning_box_solid",
        "overlay_active": True,
        "lang_en": "Solid Burning Box",
        "lang_zh": "固体燃烧室",
        "gt6_class": "MultiTileEntityGeneratorMetal",
    },
    "burning_box_solid_dense": {
        "runtime": "solid_burning_box",
        "fuel_map": "FM.Furnace",
        "accepts": ["ITEM_FURNACE_FUEL"],
        "emits": ["HU"],
        "faces": {"energyInputs": [], "energyOutputs": ["UP"], "fluidInputs": [], "fluidOutputs": []},
        "texture_profile": "burning_box_solid",
        "overlay_active": True,
        "lang_en": "Dense Solid Burning Box",
        "lang_zh": "致密固体燃烧室",
        "gt6_class": "MultiTileEntityGeneratorMetal",
    },
    "burning_box_liquid": {
        "runtime": "fluid_burning_box",
        "fuel_map": "FM.Burn",
        "accepts": ["FLUID_BURN"],
        "emits": ["HU"],
        "faces": {
            "energyInputs": [],
            "energyOutputs": ["UP"],
            "fluidInputs": ["NORTH", "SOUTH"],
            "fluidOutputs": ["NORTH", "SOUTH"],
        },
        "texture_profile": "burning_box_liquid",
        "overlay_active": True,
        "lang_en": "Liquid Burning Box",
        "lang_zh": "液体燃烧室",
        "gt6_class": "MultiTileEntityGeneratorLiquid",
    },
    "burning_box_liquid_dense": {
        "runtime": "fluid_burning_box",
        "fuel_map": "FM.Burn",
        "accepts": ["FLUID_BURN"],
        "emits": ["HU"],
        "faces": {
            "energyInputs": [],
            "energyOutputs": ["UP"],
            "fluidInputs": ["NORTH", "SOUTH"],
            "fluidOutputs": ["NORTH", "SOUTH"],
        },
        "texture_profile": "burning_box_liquid",
        "overlay_active": True,
        "lang_en": "Dense Liquid Burning Box",
        "lang_zh": "致密液体燃烧室",
        "gt6_class": "MultiTileEntityGeneratorLiquid",
    },
    "burning_box_gas": {
        "runtime": "fluid_burning_box",
        "fuel_map": "FM.Burn",
        "accepts": ["FLUID_BURN"],
        "emits": ["HU"],
        "faces": {
            "energyInputs": [],
            "energyOutputs": ["UP"],
            "fluidInputs": ["NORTH", "SOUTH"],
            "fluidOutputs": ["NORTH", "SOUTH"],
        },
        "texture_profile": "burning_gas_generator",
        "overlay_active": True,
        "lang_en": "Gas Burning Box",
        "lang_zh": "燃气燃烧室",
        "gt6_class": "MultiTileEntityGeneratorGas",
    },
    "burning_box_gas_dense": {
        "runtime": "fluid_burning_box",
        "fuel_map": "FM.Burn",
        "accepts": ["FLUID_BURN"],
        "emits": ["HU"],
        "faces": {
            "energyInputs": [],
            "energyOutputs": ["UP"],
            "fluidInputs": ["NORTH", "SOUTH"],
            "fluidOutputs": ["NORTH", "SOUTH"],
        },
        "texture_profile": "burning_gas_generator",
        "overlay_active": True,
        "lang_en": "Dense Gas Burning Box",
        "lang_zh": "致密燃气燃烧室",
        "gt6_class": "MultiTileEntityGeneratorGas",
    },
    "burning_box_fluid_bed": {
        "runtime": "fluid_bed_burning_box",
        "fuel_map": "FM.FluidBed",
        "accepts": ["ITEM_FLUIDBED", "FLUID_FLUIDBED"],
        "emits": ["HU"],
        "faces": {
            "energyInputs": [],
            "energyOutputs": ["UP"],
            "fluidInputs": ["NORTH"],
            "fluidOutputs": [],
        },
        "texture_profile": "burning_box_fluid_bed",
        "overlay_active": True,
        "lang_en": "Fluidized Bed Burning Box",
        "lang_zh": "流化床燃烧室",
        "gt6_class": "MultiTileEntityGeneratorFluidBed",
    },
    "burning_box_fluid_bed_dense": {
        "runtime": "fluid_bed_burning_box",
        "fuel_map": "FM.FluidBed",
        "accepts": ["ITEM_FLUIDBED", "FLUID_FLUIDBED"],
        "emits": ["HU"],
        "faces": {
            "energyInputs": [],
            "energyOutputs": ["UP"],
            "fluidInputs": ["NORTH"],
            "fluidOutputs": [],
        },
        "texture_profile": "burning_box_fluid_bed",
        "overlay_active": True,
        "lang_en": "Dense Fluidized Bed Burning Box",
        "lang_zh": "致密流化床燃烧室",
        "gt6_class": "MultiTileEntityGeneratorFluidBed",
    },
    "boiler": {
        "runtime": "boiler",
        "fuel_map": "NONE",
        "accepts": ["HU", "minecraft:water"],
        "emits": ["STEAM"],
        "faces": {
            "energyInputs": ["DOWN"],
            "energyOutputs": [],
            "fluidInputs": ["SIDES"],
            "fluidOutputs": ["UP"],
        },
        "texture_profile": "bronze_boiler",
        "overlay_active": False,
        "lang_en": "Boiler",
        "lang_zh": "锅炉",
        "gt6_class": "MultiTileEntityBoilerTank",
    },
    "strong_boiler": {
        "runtime": "boiler",
        "fuel_map": "NONE",
        "accepts": ["HU", "minecraft:water"],
        "emits": ["STEAM"],
        "faces": {
            "energyInputs": ["DOWN"],
            "energyOutputs": [],
            "fluidInputs": ["SIDES"],
            "fluidOutputs": ["UP"],
        },
        "texture_profile": "bronze_boiler",
        "overlay_active": False,
        "lang_en": "Strong Boiler",
        "lang_zh": "强化锅炉",
        "gt6_class": "MultiTileEntityBoilerTank",
    },
    "steam_engine": {
        "runtime": "steam_engine",
        "fuel_map": "NONE",
        "accepts": ["STEAM"],
        "emits": ["KU"],
        "faces": {
            "energyInputs": [],
            "energyOutputs": ["FRONT"],
            "fluidInputs": ["BACK"],
            "fluidOutputs": ["SIDES"],
        },
        "texture_profile": "bronze_steam_engine",
        "overlay_active": False,
        "lang_en": "Steam Engine",
        "lang_zh": "蒸汽机",
        "gt6_class": "MultiTileEntityEngineSteam",
    },
    "strong_steam_engine": {
        "runtime": "steam_engine",
        "fuel_map": "NONE",
        "accepts": ["STEAM"],
        "emits": ["KU"],
        "faces": {
            "energyInputs": [],
            "energyOutputs": ["FRONT"],
            "fluidInputs": ["BACK"],
            "fluidOutputs": ["SIDES"],
        },
        "texture_profile": "bronze_steam_engine",
        "overlay_active": False,
        "lang_en": "Strong Steam Engine",
        "lang_zh": "强化蒸汽机",
        "gt6_class": "MultiTileEntityEngineSteam",
    },
    "fuel_engine": {
        "runtime": "fuel_engine",
        "fuel_map": "FM.Engine",
        "accepts": ["FLUID_ENGINE"],
        "emits": ["RU"],
        "faces": {
            "energyInputs": [],
            "energyOutputs": ["FRONT"],
            "fluidInputs": ["UP"],
            "fluidOutputs": ["UP"],
        },
        "texture_profile": "fuel_engine",
        "overlay_active": True,
        "lang_en": "Fuel Engine",
        "lang_zh": "燃油引擎",
        "gt6_class": "MultiTileEntityMotorLiquid",
    },
    "dynamo": {
        "runtime": "dynamo",
        "fuel_map": "NONE",
        "accepts": ["RU"],
        "emits": ["EU"],
        "faces": {
            "energyInputs": ["BACK"],
            "energyOutputs": ["FRONT"],
            "fluidInputs": [],
            "fluidOutputs": [],
        },
        "texture_profile": "bronze_dynamo",
        "overlay_active": True,
        "lang_en": "Dynamo",
        "lang_zh": "发电机",
        "gt6_class": "MultiTileEntityDynamoElectric",
    },
    "electric_motor": {
        "runtime": "electric_motor",
        "fuel_map": "NONE",
        "accepts": ["EU"],
        "emits": ["RU"],
        "faces": {
            "energyInputs": ["BACK"],
            "energyOutputs": ["FRONT"],
            "fluidInputs": [],
            "fluidOutputs": [],
        },
        "texture_profile": "electric_motor",
        "overlay_active": False,
        "lang_en": "Electric Motor",
        "lang_zh": "电机",
        "gt6_class": "MultiTileEntityMotorElectric",
    },
}

MATERIAL_LANG = {
    "clay_brick": {"lang_key_zh": "砖", "lang_key_en": "Brick"},
    "lead": {"lang_key_zh": "铅", "lang_key_en": "Lead"},
    "bismuth": {"lang_key_zh": "铋", "lang_key_en": "Bismuth"},
    "bronze": {"lang_key_zh": "青铜", "lang_key_en": "Bronze"},
    "arsenic_copper": {"lang_key_zh": "砷铜", "lang_key_en": "Arsenic Copper"},
    "arsenic_bronze": {"lang_key_zh": "砷青铜", "lang_key_en": "Arsenic Bronze"},
    "invar": {"lang_key_zh": "殷钢", "lang_key_en": "Invar"},
    "steel": {"lang_key_zh": "钢制", "lang_key_en": "Steel"},
    "chromium": {"lang_key_zh": "铬制", "lang_key_en": "Chromium"},
    "titanium": {"lang_key_zh": "钛制", "lang_key_en": "Titanium"},
    "netherite": {"lang_key_zh": "下界合金", "lang_key_en": "Netherite"},
    "tungsten": {"lang_key_zh": "钨制", "lang_key_en": "Tungsten"},
    "tungstensteel": {"lang_key_zh": "钨钢", "lang_key_en": "Tungstensteel"},
    "tantalum_hafnium_carbide": {
        "lang_key_zh": "钽铪碳化物",
        "lang_key_en": "Tantalum Hafnium Carbide",
    },
    "ultimet": {"lang_key_zh": "阿尔蒂姆", "lang_key_en": "Ultimet"},
    "tin_alloy": {"lang_key_zh": "锡合金", "lang_key_en": "Tin Alloy"},
    "brass": {"lang_key_zh": "黄铜", "lang_key_en": "Brass"},
    "ironwood": {"lang_key_zh": "铁木", "lang_key_en": "Ironwood"},
    "fiery_steel": {"lang_key_zh": "炽钢", "lang_key_en": "Fiery Steel"},
    "iridium": {"lang_key_zh": "铱制", "lang_key_en": "Iridium"},
    "aluminium": {"lang_key_zh": "铝制", "lang_key_en": "Aluminium"},
    "stainless_steel": {"lang_key_zh": "不锈钢", "lang_key_en": "Stainless Steel"},
    "steel_galvanized": {"lang_key_zh": "镀锌钢", "lang_key_en": "Steel Galvanized"},
    "copper": {"lang_key_zh": "铜制", "lang_key_en": "Copper"},
}

ART_COPIES = [
    ("generators/burning_solid", "burning_box_solid"),
    ("generators/burning_brick", "burning_box_brick"),
    ("generators/burning_liquid", "burning_box_liquid"),
    ("generators/burning_fluidbed", "burning_box_fluid_bed"),
]

PREFIX_ITEMS = {
    "gregtech:gt.meta.dust": "dust",
    "gregtech:gt.meta.dustTiny": "tiny_dust",
    "gregtech:gt.meta.dustSmall": "small_dust",
    "gregtech:gt.meta.dustDiv72": "dust_div72",
    "gregtech:gt.meta.ingot": "ingot",
    "gregtech:gt.meta.gem": "gem",
}

ADD_RE = re.compile(
    r"^\s*aMat\s*=\s*([^;]+);\s*aRegistry\.add\((.*)$"
)
CLASS_RE = re.compile(r"^\s*aClass\s*=\s*(\w+)\.class;")
NBT_OUTPUT_RE = re.compile(r"NBT_OUTPUT(?:_SU)?,\s*([0-9]+)(?:\s*([*/])\s*STEAM_PER_EU)?")
NBT_INPUT_RE = re.compile(r"NBT_INPUT,\s*([0-9]+)")
NBT_EFF_RE = re.compile(r"NBT_EFFICIENCY,\s*([0-9]+)")
NBT_FUEL_RE = re.compile(r"NBT_FUELMAP,\s*(FM\.\w+)")
ID_RE = re.compile(r'"[^"]*"\s*,\s*"[^"]*"\s*,\s*(\d+)\s*,')
NAME_RE = re.compile(r'^"([^"]*)"')


def dumps(document: Any) -> str:
    return json.dumps(document, indent=2, ensure_ascii=False) + "\n"


def resolve_kind(gt_class: str, display_name: str) -> str:
    base = KIND_BY_CLASS[gt_class]
    if "Dense" in display_name or "Dense Fluidized" in display_name:
        return DENSE_KINDS.get(base, base)
    if display_name.startswith("Strong "):
        return STRONG_KINDS.get(base, base)
    return base


def variant_id(material: str, kind: str) -> str:
    if kind == "dynamo" and material == "steel_galvanized":
        return "bronze_dynamo"
    return f"{material}_{kind}"


def parse_output(nbt: str) -> tuple[int, str]:
    match = NBT_OUTPUT_RE.search(nbt)
    if not match:
        return 0, "NONE"
    value = int(match.group(1))
    op = match.group(2)
    if op == "*":
        return value * 2, f"{value}*STEAM_PER_EU"
    if op == "/":
        return value // 2, f"{value}/STEAM_PER_EU"
    return value, str(value)


def parse_recipe(rest: str) -> dict[str, Any]:
    make = rest.find("UT.NBT.make(")
    if make < 0:
        return {"pattern": [], "keys": {}}
    i = make + len("UT.NBT.make(")
    depth = 1
    while i < len(rest) and depth:
        if rest[i] == "(":
            depth += 1
        elif rest[i] == ")":
            depth -= 1
        i += 1
    tail = rest[i:]
    strings = re.findall(r'"([^"]*)"', tail)
    patterns = [row for row in strings if 1 <= len(row) <= 3]
    if len(patterns) > 3:
        patterns = patterns[:3]
    cleaned = []
    for row in patterns:
        cleaned.append("".join(" " if ch in "whdxf" else ch for ch in row))
    keys: dict[str, Any] = {}
    for letter, expr in re.findall(r"'([A-Z])'\s*,\s*([^,]+(?:,\s*1)?)", tail):
        parsed = parse_ingredient(expr.strip().rstrip(");"))
        if parsed:
            keys[letter] = parsed
    return {"pattern": cleaned, "keys": keys}


def parse_ingredient(expr: str) -> dict[str, Any] | None:
    expr = re.sub(r"\s+", "", expr)
    if "brick_block" in expr:
        return {"item": "minecraft:bricks"}
    if "craftingFirestarter" in expr:
        return {"item": "minecraft:flint_and_steel"}
    if "itemLubricant" in expr:
        return {"item": "minecraft:honey_bottle"}
    if "plateCurved" in expr:
        return prefix_ingredient("plate", expr)
    if "casingMachineDouble" in expr or "casingMachine" in expr:
        return prefix_ingredient("double_plate", expr)
    op_map = {
        "plateQuintuple": "quintuple_plate",
        "plateTriple": "triple_plate",
        "plateDouble": "double_plate",
        "plateDense": "dense_plate",
        "plate": "plate",
        "stickLong": "long_rod",
        "stick": "rod",
        "springSmall": "small_spring",
        "spring": "spring",
        "pipeLarge": "large_fluid_pipe",
        "rotor": "rotor",
        "gearGtSmall": "small_gear",
        "gearGt": "gear",
        "screw": "screw",
        "ingot": "ingot",
        "wireGt01": "wire",
        "wireGt02": "double_wire",
        "wireGt04": "quadruple_wire",
        "wireGt08": "octuple_wire",
        "wireGt16": "hexadecuple_wire",
    }
    for token, prefix in op_map.items():
        if token in expr:
            return prefix_ingredient(prefix, expr)
    return None


def prefix_ingredient(prefix: str, expr: str) -> dict[str, Any]:
    if "aMat" in expr:
        return {"prefix": prefix, "material": "variant"}
    if "ANY.Cu" in expr or "AnnealedCopper" in expr:
        return {"prefix": prefix, "material": "copper"}
    if "MT.IronMagnetic" in expr:
        return {"prefix": prefix, "material": "iron"}
    if "MT.SteelMagnetic" in expr:
        return {"prefix": prefix, "material": "steel"}
    if "MT.NeodymiumMagnetic" in expr:
        return {"prefix": prefix, "material": "iron"}
    if "MT.Brick" in expr:
        return {"prefix": prefix, "material": "clay_brick"}
    if "MT.Pb" in expr:
        return {"prefix": prefix, "material": "lead"}
    return {"prefix": prefix, "material": "variant"}


def extract_rows() -> list[dict[str, Any]]:
    current_class = ""
    rows: list[dict[str, Any]] = []
    for line_no, raw in enumerate(LOADER.read_text(encoding="utf-8").splitlines(), 1):
        stripped = raw.strip()
        if stripped.startswith("//"):
            continue
        class_match = CLASS_RE.match(raw)
        if class_match:
            current_class = class_match.group(1)
            continue
        add_match = ADD_RE.match(raw)
        if not add_match:
            continue
        rest = add_match.group(2)
        if ",aClass," not in rest.replace(" ", ""):
            continue
        if current_class not in KIND_BY_CLASS:
            continue
        material_expr = add_match.group(1).strip()
        name_match = NAME_RE.match(rest)
        display_name = name_match.group(1) if name_match else ""
        source_id = int(ID_RE.search(rest).group(1))
        kind = resolve_kind(current_class, display_name)
        material = MATERIAL_MAP[material_expr]
        nbt_output, output_expr = parse_output(rest)
        nbt_input = int(m.group(1)) if (m := NBT_INPUT_RE.search(rest)) else 0
        efficiency = int(m.group(1)) if (m := NBT_EFF_RE.search(rest)) else 0
        fuel_map = m.group(1) if (m := NBT_FUEL_RE.search(rest)) else KIND_META[kind]["fuel_map"]
        rows.append(
            {
                "id": f"cruciblecraft:{variant_id(material, kind)}",
                "kind": f"cruciblecraft:{kind}",
                "material": material,
                "source_id": source_id,
                "source_line": line_no,
                "gt6_class": current_class,
                "material_expression": material_expr,
                "nbt_output": nbt_output,
                "nbt_input": nbt_input,
                "efficiency_bps": efficiency,
                "output_expression": output_expr,
                "fuel_map": fuel_map,
                "recipe": parse_recipe(rest),
            }
        )
    return rows


def write_kinds() -> None:
    kinds = []
    for kind_id, meta in KIND_META.items():
        kinds.append(
            {
                "id": f"cruciblecraft:{kind_id}",
                "runtime": meta["runtime"],
                "fuel_map": meta["fuel_map"],
                "accepts": meta["accepts"],
                "emits": meta["emits"],
                "faces": meta["faces"],
                "texture_profile": meta["texture_profile"],
                "overlay_active": meta["overlay_active"],
                "lang_key_en": meta["lang_en"],
                "lang_key_zh": meta["lang_zh"],
                "gt6_class": meta["gt6_class"],
            }
        )
    document = {
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "kinds": kinds,
        "material_lang": MATERIAL_LANG,
    }
    (DATA / "energy_converter_kinds.json").write_text(dumps(document), encoding="utf-8")


def write_tiers(rows: list[dict[str, Any]]) -> None:
    if len(rows) != 169:
        counts = Counter(row["kind"] for row in rows)
        raise SystemExit(f"expected 169 converter rows, got {len(rows)}: {counts}")
    document = {
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "steam_per_eu": 2,
        "tiers": rows,
    }
    (DATA / "energy_converter_tiers.json").write_text(dumps(document), encoding="utf-8")


def copy_art() -> None:
    imports: list[dict[str, str]] = []
    for src_rel, dest_name in ART_COPIES:
        src = GT6_W_TEX / src_rel
        dest = DEST_TEX / dest_name
        if not src.is_dir():
            raise SystemExit(f"missing GT6 art {src}")
        for path in src.rglob("*"):
            if not path.is_file():
                continue
            relative = path.relative_to(src)
            target = dest / relative
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(path, target)
            imports.append(
                {
                    "destination": str(
                        target.relative_to(ROOT / "src" / "main" / "resources")
                    ).replace("\\", "/"),
                    "gt6_source": str(
                        path.relative_to(
                            ROOT
                            / "gt6_referencable_port_code"
                            / "gregtech6_w"
                            / "src"
                            / "main"
                            / "resources"
                        )
                    ).replace("\\", "/"),
                }
            )
    manifest = {
        "source": "gt6_referencable_port_code/gregtech6_w",
        "source_revision": SOURCE_REVISION,
        "imports": imports,
    }
    (
        ROOT
        / "src"
        / "main"
        / "resources"
        / "assets"
        / "cruciblecraft"
        / "gt6_converter_catalog_art_manifest.json"
    ).write_text(dumps(manifest), encoding="utf-8")


def import_fluidbed() -> int:
    dump = json.loads(FLUIDBED_DUMP.read_text(encoding="utf-8"))
    xref = json.loads(CROSS_REF.read_text(encoding="utf-8"))
    materials = xref["material_id_to_cc"]
    fluids = xref["fluid_to_material"]
    FLUIDBED_OUT.mkdir(parents=True, exist_ok=True)
    written = 0
    for index, recipe in enumerate(dump.get("recipes") or []):
        mapped = map_fluidbed_recipe(recipe, materials, fluids, index)
        if mapped is None:
            continue
        slug = mapped.pop("_slug")
        path = FLUIDBED_OUT / f"{slug}.json"
        path.write_text(dumps(mapped), encoding="utf-8")
        written += 1
    return written


def map_fluidbed_recipe(
    recipe: dict[str, Any],
    materials: dict[str, str],
    fluids: dict[str, str],
    index: int,
) -> dict[str, Any] | None:
    inputs = recipe.get("inputs") or []
    outputs = recipe.get("outputs") or []
    fluid_inputs = recipe.get("fluidInputs") or []
    if len(inputs) != 1 or not fluid_inputs:
        return None
    item_in = map_item(inputs[0], materials)
    if item_in is None:
        return None
    fluid_in = map_fluid(fluid_inputs[0], fluids)
    if fluid_in is None:
        return None
    item_outs = []
    for output in outputs:
        mapped = map_item_stack(output, materials)
        if mapped is None:
            return None
        item_outs.append(mapped)
    slug_parts = [
        item_in["item"].split(":")[-1].replace("/", "_"),
        fluid_in["id"].split(":")[-1],
        str(index),
    ]
    document: dict[str, Any] = {
        "type": "cruciblecraft:gt_recipe",
        "map": "cruciblecraft:fuels_fluidbed",
        "duration": int(recipe["duration"]),
        "eut": int(recipe.get("euPerTick") or -1),
        "can_be_buffered": True,
        "item_inputs": [{"item": item_in["item"]}],
        "item_input_counts": [item_in["count"]],
        "item_outputs": item_outs,
        "fluid_inputs": [fluid_in],
        "output_chances": [10_000] * max(1, len(item_outs)),
        "provenance": {
            "selected_source_recipe": (
                "gt6_dump/gt6_recipe_dump/maps/gt.recipe.fuels.fluidbed.json"
                f"#recipes[{index}]"
            ),
            "source_kind": "gt6_pinned_dump_projection",
        },
        "_slug": "_".join(slug_parts),
    }
    return document


def map_item(stack: dict[str, Any], materials: dict[str, str]) -> dict[str, Any] | None:
    prefix = PREFIX_ITEMS.get(stack.get("item") or "")
    material = materials.get(str(stack.get("meta")))
    if prefix is None or not material:
        return None
    return {
        "item": f"cruciblecraft:{material}/{prefix}",
        "count": int(stack.get("count") or 1),
    }


def map_item_stack(stack: dict[str, Any], materials: dict[str, str]) -> dict[str, Any] | None:
    mapped = map_item(stack, materials)
    if mapped is None:
        return None
    return {"count": mapped["count"], "id": mapped["item"]}


def map_fluid(stack: dict[str, Any], fluids: dict[str, str]) -> dict[str, Any] | None:
    raw = str(stack.get("fluid") or "")
    material = fluids.get(raw)
    if not material:
        return None
    fluid_id = (
        "minecraft:water"
        if material == "water"
        else f"cruciblecraft:molten_{material}"
        if raw.startswith("molten.")
        else f"cruciblecraft:{material}"
    )
    return {"amount": int(stack.get("amount") or 1), "id": fluid_id}


def main() -> None:
    rows = extract_rows()
    write_kinds()
    write_tiers(rows)
    copy_art()
    imported = import_fluidbed()
    counts = Counter(row["kind"].split(":")[-1] for row in rows)
    print(f"wrote {len(rows)} converter tiers")
    print(dict(counts))
    print(f"imported {imported} fluidbed recipes")


if __name__ == "__main__":
    main()
