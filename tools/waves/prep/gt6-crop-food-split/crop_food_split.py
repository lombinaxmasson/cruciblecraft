#!/usr/bin/env python3
"""GT6 crop/food optional-split ledger, named plant forms, and Core plant-form strip."""
from __future__ import annotations

import argparse
import json
import re
import shutil
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[4]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import io_common as io

WAVE = Path(__file__).resolve().parent
GT6 = ROOT / "gt6_code" / "gregtech6"
COMPAT = (
    GT6
    / "src"
    / "main"
    / "java"
    / "gregtech"
    / "compat"
    / "Compat_Recipes_IndustrialCraft.java"
)
FOOD_JAVA = (
    GT6 / "src" / "main" / "java" / "gregtech" / "items" / "MultiItemFood.java"
)
GT6_W = ROOT / "gt6_referencable_port_code" / "gregtech6_w"
FOOD_TEXTURES = (
    GT6_W
    / "src"
    / "main"
    / "resources"
    / "assets"
    / "gregtech"
    / "textures"
    / "items"
    / "gt.multiitem.food"
)
GATE = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "material_registration_gate.json"
)
AUTHORITY = ROOT / "tools" / "material_form_authority.json"
SMELTER_FORMS = ROOT / "tools" / "waves" / "smelter" / "ordinary-closure" / "required_forms.json"
ORDINARY_WAVE = ROOT / "tools" / "build_ordinary_wave.py"
LEDGER = WAVE / "ledger.json"
NAMED_FORMS = WAVE / "named_plant_forms.json"
DENOMINATOR = WAVE / "denominator.json"
FOODS_ART = (
    ROOT
    / "src"
    / "addons"
    / "foods"
    / "resources"
    / "assets"
    / "cruciblecraft_foods"
    / "gt6_foods_art_manifest.json"
)
FOODS_TEXTURE_DIR = (
    ROOT
    / "src"
    / "addons"
    / "foods"
    / "resources"
    / "assets"
    / "cruciblecraft_foods"
    / "textures"
    / "item"
)
FOODS_DATA = (
    ROOT
    / "src"
    / "addons"
    / "foods"
    / "resources"
    / "data"
    / "cruciblecraft_foods"
)
SEMANTIC_CATALOG = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "semantic_object_catalog.json"
)
FOOD_STAT = re.compile(
    r"IL\.(Food_[A-Za-z0-9_]+|Crop_[A-Za-z0-9_]+)\s*\.set\(addItem\("
    r"[^;]*new FoodStat\(\s*(-?\d+)\s*,\s*([0-9.]+)F",
)
FOOD_TRAITS = {
    "ananas": ["food", "fruit", "crop"],
    "apple_darkred": ["food", "fruit", "crop"],
    "apple_green": ["food", "fruit", "crop"],
    "apple_red": ["food", "fruit", "crop"],
    "apple_yellow": ["food", "fruit", "crop"],
    "barley": ["grain", "crop"],
    "blackberry": ["food", "fruit", "berry", "crop"],
    "blueberry": ["food", "fruit", "berry", "crop"],
    "candleberry": ["food", "fruit", "berry", "crop"],
    "chili_pepper": ["food", "vegetable", "crop"],
    "cranberry": ["food", "fruit", "berry", "crop"],
    "cucumber": ["food", "vegetable", "crop"],
    "currants_black": ["food", "fruit", "berry", "crop"],
    "currants_red": ["food", "fruit", "berry", "crop"],
    "currants_white": ["food", "fruit", "berry", "crop"],
    "gooseberry": ["food", "fruit", "berry", "crop"],
    "grapes_green": ["food", "fruit", "crop"],
    "grapes_purple": ["food", "fruit", "crop"],
    "grapes_red": ["food", "fruit", "crop"],
    "grapes_white": ["food", "fruit", "crop"],
    "lemon": ["food", "fruit", "crop"],
    "oats": ["grain", "crop"],
    "onion": ["food", "vegetable", "crop"],
    "peanut": ["food", "crop"],
    "raspberry": ["food", "fruit", "berry", "crop"],
    "rice": ["grain", "crop"],
    "rye": ["grain", "crop"],
    "strawberry": ["food", "fruit", "berry", "crop"],
    "tomato": ["food", "vegetable", "crop"],
}
FOOD_ZH = {
    "ananas": "菠萝",
    "apple_darkred": "暗红苹果",
    "apple_green": "青苹果",
    "apple_red": "苹果",
    "apple_yellow": "黄苹果",
    "barley": "大麦",
    "blackberry": "黑莓",
    "blueberry": "蓝莓",
    "candleberry": "蜡果",
    "chili_pepper": "辣椒",
    "cranberry": "蔓越莓",
    "cucumber": "黄瓜",
    "currants_black": "黑醋栗",
    "currants_red": "红醋栗",
    "currants_white": "白醋栗",
    "gooseberry": "醋栗",
    "grapes_green": "青葡萄",
    "grapes_purple": "紫葡萄",
    "grapes_red": "红葡萄",
    "grapes_white": "白葡萄",
    "lemon": "柠檬",
    "oats": "燕麦",
    "onion": "洋葱",
    "peanut": "花生",
    "raspberry": "覆盆子",
    "rice": "稻米",
    "rye": "黑麦",
    "strawberry": "草莓",
    "tomato": "番茄",
}

PLANT_FORMS = (
    "plant_gt_berry",
    "plant_gt_blossom",
    "plant_gt_fiber",
    "plant_gt_twig",
    "plant_gt_wart",
)
MT_TO_CC = {
    "Indigo": "indigo",
    "Oil": "oil",
    "Emerald": "emerald",
    "Diamond": "diamond",
    "Coal": "coal",
    "Gunpowder": "gunpowder",
    "EnderPearl": "ender_pearl",
    "Milk": "milk",
    "Glowstone": "glowstone",
    "Sn": "tin",
    "Cu": "copper",
    "Ag": "silver",
    "Pb": "lead",
    "Steeleaf": "steeleaf",
    "LiveRoot": "live_root",
    "Tea": "tea",
    "Mint": "mint",
    "Hg": "mercury",
    "Fe": "iron",
    "Au": "gold",
}
OP_TO_PREFIX = {
    "plantGtBerry": "plant_gt_berry",
    "plantGtBlossom": "plant_gt_blossom",
    "plantGtFiber": "plant_gt_fiber",
    "plantGtTwig": "plant_gt_twig",
    "plantGtWart": "plant_gt_wart",
    "dustTiny": "tiny_dust",
    "dust": "dust",
    "nugget": "nugget",
    "ingot": "ingot",
    "chunkGt": "chunk",
}
def _split_ctor_args(blob: str) -> list[str]:
    args: list[str] = []
    current: list[str] = []
    depth = 0
    in_str = False
    for index, char in enumerate(blob):
        if char == '"' and (index == 0 or blob[index - 1] != "\\"):
            in_str = not in_str
            current.append(char)
            continue
        if not in_str:
            if char in "([{":
                depth += 1
            elif char in ")]}":
                depth -= 1
            elif char == "," and depth == 0:
                args.append("".join(current).strip())
                current = []
                continue
        current.append(char)
    if current:
        args.append("".join(current).strip())
    return args
FERRU_AURELIA = re.compile(
    r'getCropList\(\)\[(\d+)\].*?OP\.plantGtBlossom\.mat\(MT\.(\w+)',
    re.S,
)
FOOD_ADDITEM = re.compile(
    r"IL\.(Food_[A-Za-z0-9_]+|Crop_Rye|Crop_Oats|Crop_Barley|Crop_Rice)"
    r"\s*\.set\(addItem\(\s*(\d+)\s*,\s*\"([^\"]+)\"",
)
FOOD_VANILLA_APPLE = re.compile(
    r"IL\.Food_Apple_Red\s*\.set\(ST\.make\(Items\.apple"
)
IGNORED_SQUEEZER_PLANT_ROWS = 5215
DUMP_SQUEEZER_TOTAL = 5322
SQUEEZER_SELECTED = 15
SQUEEZER_ACTIONABLE = 92


def _slug(name: str) -> str:
    return re.sub(r"[^a-z0-9]+", "_", name.lower()).strip("_")


def _attrs(blob: str) -> list[str]:
    return [part.strip().strip('"') for part in blob.split(",") if part.strip().strip('"')]


def _parse_stack(token: str) -> dict[str, Any]:
    text = " ".join(token.split())
    if text == "null":
        return {"kind": "none"}
    plant = re.search(r"OP\.(plantGt\w+)\.mat\(MT\.(\w+),\s*(\d+)\)", text)
    if plant:
        prefix = OP_TO_PREFIX[plant.group(1)]
        material = MT_TO_CC.get(plant.group(2), _slug(plant.group(2)))
        return {
            "kind": "plant_form",
            "material": material,
            "prefix": prefix,
            "count": int(plant.group(3)),
            "gt6": text,
        }
    material_op = re.search(r"OP\.(\w+)\.mat\(MT\.(\w+)\s*,\s*(\d+)\)", text)
    if material_op:
        prefix = OP_TO_PREFIX.get(material_op.group(1), _slug(material_op.group(1)))
        material = MT_TO_CC.get(material_op.group(2), _slug(material_op.group(2)))
        return {
            "kind": "material_form",
            "material": material,
            "prefix": prefix,
            "count": int(material_op.group(3)),
            "gt6": text,
        }
    vanilla = re.search(r"ST\.make\((Items|Blocks)\.(\w+),\s*(\d+),\s*(\d+|W)\)", text)
    if vanilla:
        name = vanilla.group(2)
        mapped = {
            "dye": "pink_dye" if vanilla.group(4) == "9" else "dye",
            "web": "cobweb",
            "ender_eye": "ender_eye",
            "ender_pearl": "ender_pearl",
        }.get(name, _slug(name))
        return {
            "kind": "vanilla",
            "item": f"minecraft:{mapped}",
            "count": int(vanilla.group(3)),
            "gt6": text,
        }
    named_food = re.search(
        r'IL\.(Food_[A-Za-z0-9_]+)\.getWithName\((\d+),\s*"([^"]+)"\)',
        text,
    )
    if named_food:
        return {
            "kind": "foods_addon",
            "il": named_food.group(1),
            "count": int(named_food.group(2)),
            "display_name": named_food.group(3),
            "gt6": text,
        }
    il_food = re.search(r"IL\.(Food_[A-Za-z0-9_]+|Crop_[A-Za-z0-9_]+)\.get\((\d+)\)", text)
    if il_food:
        return {
            "kind": "foods_addon",
            "il": il_food.group(1),
            "count": int(il_food.group(2)),
            "gt6": text,
        }
    if "IL.ARS_" in text or "IL.DesertNova" in text or "IL.Cerublossom" in text:
        return {"kind": "external_arsmagica", "gt6": text, "blocked_without": "arsmagica"}
    if "IL.TC_" in text or "IL.TF_" in text:
        mod = "thaumcraft" if "IL.TC_" in text else "twilightforest"
        return {"kind": "external", "gt6": text, "blocked_without": mod}
    if "IL.Dye_Bonemeal" in text:
        return {
            "kind": "vanilla",
            "item": "minecraft:bone_meal",
            "count": 1,
            "gt6": text,
        }
    return {"kind": "unparsed", "gt6": text}


def _parse_specials(token: str) -> list[dict[str, Any]]:
    text = " ".join(token.split())
    if text == "null":
        return []
    inner = text
    if inner.startswith("ST.array(") and inner.endswith(")"):
        inner = inner[len("ST.array(") : -1]
    return [_parse_stack(part) for part in _split_ctor_args(inner) if part.strip()]


def parse_crops() -> list[dict[str, Any]]:
    text = COMPAT.read_text(encoding="utf-8")
    rows: list[dict[str, Any]] = []
    marker = "new GT_BaseCrop("
    start = 0
    while True:
        index = text.find(marker, start)
        if index < 0:
            break
        depth = 1
        cursor = index + len(marker)
        in_str = False
        while cursor < len(text) and depth:
            char = text[cursor]
            if char == '"' and text[cursor - 1] != "\\":
                in_str = not in_str
            elif not in_str:
                if char == "(":
                    depth += 1
                elif char == ")":
                    depth -= 1
            cursor += 1
        args = _split_ctor_args(text[index + len(marker) : cursor - 1])
        if len(args) != 16:
            raise ValueError(f"GT_BaseCrop arity drifted: {len(args)} at {index}")
        drop = _parse_stack(args[2])
        seed = _parse_stack(args[4])
        attributes = _attrs(args[15].replace("new String[]", "").strip().strip("{}"))
        rows.append(
            {
                "id": _slug(args[0].strip('"')),
                "kind": "gt_base_crop",
                "name": args[0].strip('"'),
                "discovered_by": args[1].strip('"'),
                "drop": drop,
                "special_drops": _parse_specials(args[3]),
                "base_seed": seed,
                "crossbreed_only": seed.get("kind") == "none",
                "tier": int(args[5]),
                "max_size": int(args[6]),
                "growth_speed": int(args[7]),
                "after_harvest_size": int(args[8]),
                "harvest_size": int(args[9]),
                "stat_chemical": int(args[10]),
                "stat_food": int(args[11]),
                "stat_defensive": int(args[12]),
                "stat_color": int(args[13]),
                "stat_weed": int(args[14]),
                "attributes": attributes,
                "food_card": "Food" in attributes,
                "depends_on_foods_addon": drop.get("kind") == "foods_addon"
                or seed.get("kind") == "foods_addon",
                "blocked_without": drop.get("blocked_without")
                or seed.get("blocked_without"),
            }
        )
        start = cursor
    if len(rows) != 59:
        raise ValueError(f"expected 59 GT_BaseCrop constructors, found {len(rows)}")
    ferru = FERRU_AURELIA.findall(text)
    mapped = {int(slot): material for slot, material in ferru}
    if mapped.get(13) != "Fe" or mapped.get(14) != "Au":
        raise ValueError(f"Ferru/Aurelia crop-list patches drifted: {mapped}")
    rows.append(
        {
            "id": "ferru",
            "kind": "ic2_drop_rewrite",
            "name": "Ferru",
            "discovered_by": "IC2",
            "ic2_slot": 13,
            "drop": {
                "kind": "plant_form",
                "material": "iron",
                "prefix": "plant_gt_blossom",
                "count": 1,
                "gt6": "OP.plantGtBlossom.mat(MT.Fe, 1)",
            },
            "special_drops": [],
            "base_seed": {"kind": "none"},
            "crossbreed_only": True,
            "tier": 6,
            "max_size": 4,
            "growth_speed": 0,
            "after_harvest_size": 1,
            "harvest_size": 4,
            "stat_chemical": 2,
            "stat_food": 0,
            "stat_defensive": 0,
            "stat_color": 0,
            "stat_weed": 0,
            "attributes": ["Flower", "Metal", "Iron"],
            "food_card": False,
            "depends_on_foods_addon": False,
            "blocked_without": None,
            "note": "Not a GT_BaseCrop. GT6 rewrites IC2 slot 13 drop to Fe blossom.",
        }
    )
    rows.append(
        {
            "id": "aurelia",
            "kind": "ic2_drop_rewrite",
            "name": "Aurelia",
            "discovered_by": "IC2",
            "ic2_slot": 14,
            "drop": {
                "kind": "plant_form",
                "material": "gold",
                "prefix": "plant_gt_blossom",
                "count": 1,
                "gt6": "OP.plantGtBlossom.mat(MT.Au, 1)",
            },
            "special_drops": [],
            "base_seed": {"kind": "none"},
            "crossbreed_only": True,
            "tier": 8,
            "max_size": 4,
            "growth_speed": 0,
            "after_harvest_size": 1,
            "harvest_size": 4,
            "stat_chemical": 2,
            "stat_food": 0,
            "stat_defensive": 0,
            "stat_color": 0,
            "stat_weed": 0,
            "attributes": ["Flower", "Metal", "Gold"],
            "food_card": False,
            "depends_on_foods_addon": False,
            "blocked_without": None,
            "note": "Not a GT_BaseCrop. GT6 rewrites IC2 slot 14 drop to Au blossom.",
        }
    )
    return rows


def named_plant_forms(crops: list[dict[str, Any]]) -> list[dict[str, str]]:
    pairs: dict[tuple[str, str], dict[str, str]] = {}
    for crop in crops:
        for stack in [crop["drop"], crop["base_seed"], *crop["special_drops"]]:
            if stack.get("kind") != "plant_form":
                continue
            key = (stack["material"], stack["prefix"])
            pairs[key] = {
                "material": stack["material"],
                "prefix": stack["prefix"],
                "crop_id": crop["id"],
            }
    return [pairs[key] for key in sorted(pairs)]


def parse_food_crop_items() -> list[dict[str, Any]]:
    text = FOOD_JAVA.read_text(encoding="utf-8")
    rows = [
        {
            "il": match.group(1),
            "meta": int(match.group(2)),
            "english": match.group(3),
            "path": _slug(match.group(1).removeprefix("Food_").removeprefix("Crop_")),
        }
        for match in FOOD_ADDITEM.finditer(text)
        if match.group(1)
        in {
            "Crop_Rye",
            "Crop_Oats",
            "Crop_Barley",
            "Crop_Rice",
            "Food_Lemon",
            "Food_Tomato",
            "Food_Onion",
            "Food_Cucumber",
            "Food_Chili_Pepper",
            "Food_Grapes_Green",
            "Food_Grapes_White",
            "Food_Grapes_Red",
            "Food_Grapes_Purple",
            "Food_Blueberry",
            "Food_Gooseberry",
            "Food_Candleberry",
            "Food_Cranberry",
            "Food_Currants_Black",
            "Food_Currants_White",
            "Food_Currants_Red",
            "Food_Blackberry",
            "Food_Raspberry",
            "Food_Strawberry",
            "Food_Apple_Green",
            "Food_Apple_Yellow",
            "Food_Apple_DarkRed",
            "Food_Peanut",
            "Food_Ananas",
        }
    ]
    if not FOOD_VANILLA_APPLE.search(text):
        raise ValueError("Food_Apple_Red is no longer vanilla apple")
    rows.append(
        {
            "il": "Food_Apple_Red",
            "meta": None,
            "english": "Apple",
            "path": "apple_red",
            "reuse_canonical": "minecraft:apple",
        }
    )
    stats = _parse_food_stats(text)
    for row in rows:
        row["traits"] = list(FOOD_TRAITS.get(row["path"], ["crop"]))
        row["chinese"] = FOOD_ZH.get(row["path"], row["english"])
        stat = stats.get(row["il"], {"nutrition": 0, "saturation": 0.0, "always_edible": False})
        row.update(stat)
    return sorted(rows, key=lambda row: (row["path"], row["il"]))


def _parse_food_stats(text: str) -> dict[str, dict[str, Any]]:
    stats: dict[str, dict[str, Any]] = {}
    for match in FOOD_STAT.finditer(text):
        stats[match.group(1)] = {
            "nutrition": int(match.group(2)),
            "saturation": float(match.group(3)),
            "always_edible": True,
        }
    return stats


def build_ledger() -> dict[str, Any]:
    crops = parse_crops()
    foods = parse_food_crop_items()
    named = named_plant_forms(crops)
    food_cards = [crop["id"] for crop in crops if crop["food_card"]]
    return {
        "schema_version": 1,
        "source_revision": io.SOURCE_REVISION,
        "generated_by": "python tools/waves/prep/gt6-crop-food-split/crop_food_split.py",
        "gt6_base_crop_count": 59,
        "ic2_drop_rewrites": ["ferru", "aurelia"],
        "crop_count": len(crops),
        "food_card_count": len(food_cards),
        "named_plant_form_count": len(named),
        "ignored_squeezer_plant_gt_rows": IGNORED_SQUEEZER_PLANT_ROWS,
        "not_crop_denominator": {
            "squeezer_dump_rows": DUMP_SQUEEZER_TOTAL,
            "ignored_plant_gt_rows": IGNORED_SQUEEZER_PLANT_ROWS,
            "selected": SQUEEZER_SELECTED,
            "actionable": SQUEEZER_ACTIONABLE,
            "plants_generation_flag_materials": "scale, not a crop card count",
        },
        "crops": crops,
        "food_crop_items": foods,
        "named_plant_forms": named,
        "food_card_ids": food_cards,
    }


def build_denominator(ledger: dict[str, Any]) -> dict[str, Any]:
    return {
        "schema_version": 1,
        "gt6_base_crop": 59,
        "ic2_drop_rewrites": 2,
        "crop_runtime_cards": ledger["crop_count"],
        "food_cards": ledger["food_card_count"],
        "named_plant_form_pairs": ledger["named_plant_form_count"],
        "excluded_squeezer_plant_gt_rows": IGNORED_SQUEEZER_PLANT_ROWS,
        "note": "5215 ignored plant_gt_* squeezer rows are dump scale, not crops.",
    }


def copy_food_art(foods: list[dict[str, Any]]) -> dict[str, Any]:
    FOODS_TEXTURE_DIR.mkdir(parents=True, exist_ok=True)
    imports: list[dict[str, str]] = []
    for row in foods:
        meta = row.get("meta")
        if meta is None:
            continue
        source_rel = (
            "src/main/resources/assets/gregtech/textures/items/"
            f"gt.multiitem.food/{meta}.png"
        )
        source = FOOD_TEXTURES / f"{meta}.png"
        if not source.is_file():
            raise FileNotFoundError(f"missing GT6 food texture {source_rel}")
        destination_rel = (
            "assets/cruciblecraft_foods/textures/item/" + row["path"] + ".png"
        )
        destination = FOODS_TEXTURE_DIR / f"{row['path']}.png"
        shutil.copyfile(source, destination)
        imports.append(
            {
                "source": "gregtech6_w",
                "gt6_source": source_rel,
                "destination": destination_rel,
                "il": row["il"],
            }
        )
    manifest = {
        "schema_version": 1,
        "imports": imports,
        "note": "GT6 MultiItemFood metas for crop-card food identities. Red apple stays minecraft:apple.",
    }
    FOODS_ART.parent.mkdir(parents=True, exist_ok=True)
    io.write_stable(FOODS_ART, manifest)
    return manifest


def _strip_plant_forms(value: Any) -> Any:
    if isinstance(value, dict):
        return {key: _strip_plant_forms(child) for key, child in value.items()}
    if isinstance(value, list):
        if value and all(isinstance(item, str) for item in value):
            return [item for item in value if item not in PLANT_FORMS]
        return [_strip_plant_forms(item) for item in value]
    return value


def strip_core_plant_forms() -> None:
    gate = json.loads(GATE.read_text(encoding="utf-8"))
    stripped = _strip_plant_forms(gate)
    GATE.write_text(
        json.dumps(stripped, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    authority = io.load_json(AUTHORITY)
    authority["sources"] = _strip_plant_forms(authority.get("sources") or [])
    io.write_stable(AUTHORITY, authority)
    if SMELTER_FORMS.is_file():
        smelter = io.load_json(SMELTER_FORMS)
        smelter = _strip_plant_forms(smelter)
        required = smelter.get("required_forms") or {}
        smelter["counts"] = {
            "new_prefix_forms": len(smelter.get("new_prefix_forms") or []),
            "required_form_pairs": sum(len(forms) for forms in required.values()),
            "required_materials": len(required),
        }
        io.write_stable(SMELTER_FORMS, smelter)
    text = ORDINARY_WAVE.read_text(encoding="utf-8")
    for prefix in PLANT_FORMS:
        text = text.replace(f'    "{prefix}",\n', "")
    ORDINARY_WAVE.write_text(text, encoding="utf-8")
    leftover_models = ROOT / "src" / "main" / "resources" / "assets" / "cruciblecraft" / "models" / "item"
    for path in leftover_models.glob("*/plant_gt_*.json"):
        path.unlink()
    if SEMANTIC_CATALOG.is_file():
        catalog = json.loads(SEMANTIC_CATALOG.read_text(encoding="utf-8"))
        kept = [
            identity
            for identity in catalog.get("identities") or []
            if "/plant_gt_" not in str(identity.get("registry_path") or "")
        ]
        catalog["identities"] = kept
        catalog["identity_count"] = len(kept)
        catalog["variant_count"] = len(kept)
        SEMANTIC_CATALOG.write_text(
            json.dumps(catalog, ensure_ascii=False, indent=2) + "\n",
            encoding="utf-8",
        )


def write_foods_catalog(foods: list[dict[str, Any]]) -> None:
    FOODS_DATA.mkdir(parents=True, exist_ok=True)
    catalog = {
        "schema_version": 1,
        "items": foods,
        "note": "Red apple reuses minecraft:apple. Grains without GT6 FoodStat are not edible.",
    }
    io.write_stable(FOODS_DATA / "food_catalog.json", catalog)
    models = (
        ROOT
        / "src"
        / "addons"
        / "foods"
        / "resources"
        / "assets"
        / "cruciblecraft_foods"
        / "models"
        / "item"
    )
    models.mkdir(parents=True, exist_ok=True)
    lang_en: dict[str, str] = {
        "itemGroup.cruciblecraft_foods.foods": "CrucibleCraft Foods",
    }
    lang_zh: dict[str, str] = {
        "itemGroup.cruciblecraft_foods.foods": "熔炉工艺：食物",
    }
    by_trait: dict[str, list[str]] = {
        "food": [],
        "fruit": [],
        "berry": [],
        "grain": [],
        "vegetable": [],
        "crop": [],
    }
    for row in foods:
        item_id = (
            row["reuse_canonical"]
            if row.get("reuse_canonical")
            else f"cruciblecraft_foods:{row['path']}"
        )
        if not row.get("reuse_canonical"):
            io.write_stable(
                models / f"{row['path']}.json",
                {
                    "parent": "minecraft:item/generated",
                    "textures": {
                        "layer0": f"cruciblecraft_foods:item/{row['path']}"
                    },
                },
            )
            lang_en[f"item.cruciblecraft_foods.{row['path']}"] = row["english"]
            lang_zh[f"item.cruciblecraft_foods.{row['path']}"] = row["chinese"]
        for trait in row.get("traits") or []:
            by_trait.setdefault(trait, []).append(item_id)
    lang_dir = (
        ROOT
        / "src"
        / "addons"
        / "foods"
        / "resources"
        / "assets"
        / "cruciblecraft_foods"
        / "lang"
    )
    lang_dir.mkdir(parents=True, exist_ok=True)
    io.write_stable(lang_dir / "en_us.json", lang_en)
    io.write_stable(lang_dir / "zh_cn.json", lang_zh)
    tag_files = {
        "foods": by_trait["food"],
        "fruits": by_trait["fruit"],
        "foods/berry": by_trait["berry"],
        "grain": by_trait["grain"],
        "vegetables": by_trait["vegetable"],
        "crops": by_trait["crop"],
        "crops/tomato": ["cruciblecraft_foods:tomato"],
        "crops/onion": ["cruciblecraft_foods:onion"],
        "crops/rice": ["cruciblecraft_foods:rice"],
    }
    for path, values in tag_files.items():
        tag_path = (
            ROOT
            / "src"
            / "addons"
            / "foods"
            / "resources"
            / "data"
            / "c"
            / "tags"
            / "item"
            / f"{path}.json"
        )
        tag_path.parent.mkdir(parents=True, exist_ok=True)
        io.write_stable(tag_path, {"replace": False, "values": values})
    fd_root = (
        ROOT
        / "src"
        / "addons"
        / "foods"
        / "resources"
        / "data"
        / "farmersdelight"
        / "tags"
        / "item"
    )
    fd_root.mkdir(parents=True, exist_ok=True)
    for name, item in (
        ("onion", "cruciblecraft_foods:onion"),
        ("tomato", "cruciblecraft_foods:tomato"),
        ("rice", "cruciblecraft_foods:rice"),
    ):
        io.write_stable(
            fd_root / f"{name}.json",
            {"replace": False, "values": [item]},
        )


def write_artifacts() -> dict[str, Any]:
    if not COMPAT.is_file():
        raise FileNotFoundError(f"missing GT6 source {COMPAT}")
    ledger = build_ledger()
    io.write_stable(LEDGER, ledger)
    io.write_stable(NAMED_FORMS, {"pairs": ledger["named_plant_forms"]})
    io.write_stable(DENOMINATOR, build_denominator(ledger))
    crops_ledger = (
        ROOT
        / "src"
        / "addons"
        / "crops"
        / "resources"
        / "data"
        / "cruciblecraft_crops"
        / "crop_ledger.json"
    )
    crops_ledger.parent.mkdir(parents=True, exist_ok=True)
    io.write_stable(crops_ledger, ledger)
    copy_food_art(ledger["food_crop_items"])
    write_foods_catalog(ledger["food_crop_items"])
    strip_core_plant_forms()
    return ledger


def check() -> list[str]:
    errors: list[str] = []
    if not LEDGER.is_file():
        return [f"missing {io.relative(LEDGER)}"]
    actual = io.load_json(LEDGER)
    if actual.get("gt6_base_crop_count") != 59:
        errors.append("ledger gt6_base_crop_count drifted")
    if actual.get("food_card_count") != 35:
        errors.append(f"food cards {actual.get('food_card_count')} != 35")
    if actual.get("named_plant_form_count") != 13:
        errors.append(
            f"named plant forms {actual.get('named_plant_form_count')} != 13"
        )
    if actual.get("ignored_squeezer_plant_gt_rows") != IGNORED_SQUEEZER_PLANT_ROWS:
        errors.append("squeezer ignore rows must stay 5215 and out of the crop denominator")
    if GATE.is_file():
        gate = io.load_json(GATE)
        live = 0
        for forms in (gate.get("materials") or {}).values():
            live += sum(1 for form in forms if form in PLANT_FORMS)
        if live:
            errors.append(f"core gate still has {live} plant form pairs")
    ordinary = ORDINARY_WAVE.read_text(encoding="utf-8")
    if "plant_gt_berry" in ordinary:
        errors.append("build_ordinary_wave.py still routes plant_gt_* as SEMANTIC_PREFIXES")
    leftover_models = list(
        (
            ROOT
            / "src"
            / "main"
            / "resources"
            / "assets"
            / "cruciblecraft"
            / "models"
            / "item"
        ).glob("*/plant_gt_*.json")
    )
    if leftover_models:
        errors.append(
            "leftover unique plant models still present: "
            + leftover_models[0].as_posix()
        )
    if SEMANTIC_CATALOG.is_file():
        catalog = io.load_json(SEMANTIC_CATALOG)
        leftover = [
            row["registry_path"]
            for row in catalog.get("identities") or []
            if "/plant_gt_" in str(row.get("registry_path") or "")
        ]
        if leftover:
            errors.append(
                f"semantic catalog still has {len(leftover)} leftover unique plant identities"
            )
        if int(catalog.get("identity_count") or 0) != len(catalog.get("identities") or []):
            errors.append("semantic catalog identity_count drifted after plant leftover strip")
    foods_catalog = FOODS_DATA / "food_catalog.json"
    if not foods_catalog.is_file():
        errors.append(f"missing {io.relative(foods_catalog)}")
    else:
        items = io.load_json(foods_catalog).get("items") or []
        if len(items) != len(actual.get("food_crop_items") or []):
            errors.append("foods catalog item count drifted")
    return errors


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args(argv)
    if args.write == args.check:
        parser.error("choose exactly one of --write or --check")
    try:
        if args.write:
            write_artifacts()
            return 0
        errors = check()
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        return 0
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"gt6 crop/food split failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
