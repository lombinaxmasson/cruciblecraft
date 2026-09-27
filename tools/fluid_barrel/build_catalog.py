#!/usr/bin/env python3
"""Build the GT6 barrel/drum catalog, art, models, and source-exact recipes.

Rows are the Fluid Containers block plus the Logistics Tank in
Loader_MultiTileEntities at the pinned GT6 revision. A recipe is written only
when every grid token resolves to the same GT6 object.
"""
from __future__ import annotations

import json
import re
import shutil
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import gt6_resolve
from tools.gt6_mte_inplace_acquisition import (
    CATALYSTS,
    FAMILY_PREFER,
    _item_is_live,
    resolve_operand,
)

SOURCE_REVISION = "3703e40308c8c030763fd6297dea8b210d2a77b1"
MATERIALS = ROOT / "src" / "main" / "resources" / "data" / "cruciblecraft" / "materials"
CATALOG = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "fluid_barrel_catalog.json"
)
SIMPLE = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "fluid_barrel_simple_fluids.json"
)
RECIPE_ROOT = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "recipe"
    / "fluid_barrel"
)
ASSETS = ROOT / "src" / "main" / "resources" / "assets" / "cruciblecraft"
TEX_ROOT = ASSETS / "textures" / "block" / "gt6_import" / "fluid_barrel"
GT6_TEX = (
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
    / "tanks"
)
FL_JAVA = (
    ROOT
    / "gt6_code"
    / "gregtech6"
    / "src"
    / "main"
    / "java"
    / "gregapi"
    / "data"
    / "FL.java"
)
LOADER_FLUIDS = (
    ROOT
    / "gt6_code"
    / "gregtech6"
    / "src"
    / "main"
    / "java"
    / "gregtech"
    / "loaders"
    / "a"
    / "Loader_Fluids.java"
)

# meta, path, english, chinese, kind, gt6 class, material candidates,
# capacity, proofs (gas, acid, plasma, magic), melting kelvin or None,
# hardness, resistance, flammability, art, pattern, keys
ROWS: list[dict] = []


def add(
    meta: int,
    path: str,
    english: str,
    chinese: str,
    kind: str,
    gt6_class: str,
    materials: list[str],
    capacity: int,
    gas: bool,
    acid: bool,
    plasma: bool,
    magic: bool,
    melting: int | None,
    hardness: float,
    resistance: float,
    flammability: int,
    art: str,
    pattern: list[str] | None = None,
    keys: dict[str, str] | None = None,
) -> None:
    ROWS.append(
        {
            "meta": meta,
            "path": path,
            "english": english,
            "chinese": chinese,
            "kind": kind,
            "gt6_class": gt6_class,
            "materials": materials,
            "capacity": capacity,
            "gas_proof": gas,
            "acid_proof": acid,
            "plasma_proof": plasma,
            "magic_proof": magic,
            "melting": melting,
            "hardness": hardness,
            "resistance": resistance,
            "flammability": flammability,
            "art": art,
            "pattern": pattern or [],
            "keys": keys or {},
        }
    )


def wood(
    meta: int,
    path: str,
    english: str,
    chinese: str,
    materials: list[str],
    capacity: int,
    magic: bool,
    melting: int | None,
    flammability: int,
    keys: dict[str, str],
) -> None:
    add(
        meta,
        path,
        english,
        chinese,
        "WOOD",
        "MultiTileEntityBarrelWood",
        materials,
        capacity,
        False,
        False,
        False,
        magic,
        melting,
        1.0,
        5.0,
        flammability,
        "barrel",
        ["rGs", "PSP", "PSP"],
        keys,
    )


def drum(
    meta: int,
    path: str,
    english: str,
    chinese: str,
    materials: list[str],
    capacity: int,
    gas: bool,
    acid: bool,
    plasma: bool,
    magic: bool,
    resistance: float,
    melting: int | None,
    plate: str,
    stick: str,
) -> None:
    add(
        meta,
        path,
        english,
        chinese,
        "METAL",
        "MultiTileEntityBarrelMetal",
        materials,
        capacity,
        gas,
        acid,
        plasma,
        magic,
        melting,
        1.0,
        resistance,
        0,
        "drum",
        [" h ", "PSP", "PSP"],
        {"P": plate, "S": stick},
    )


def fill_rows() -> None:
    glue = "OD.itemGlue"
    wood(
        32733,
        "fluid_barrel/cheap_lead",
        "Wooden Barrel (Cheap)",
        "廉价木桶",
        ["wood"],
        8000,
        False,
        340,
        100,
        {"G": glue, "P": "OD.plankAnyWood", "S": "OP.stickLong.dat(MT.Pb)"},
    )
    wood(
        32752,
        "fluid_barrel/cheap_bismuth",
        "Wooden Barrel (Cheap)",
        "廉价木桶",
        ["wood"],
        8000,
        False,
        340,
        100,
        {"G": glue, "P": "OD.plankAnyWood", "S": "OP.stickLong.dat(MT.Bi)"},
    )
    wood(
        32753,
        "fluid_barrel/cheap_bronze",
        "Wooden Barrel (Cheap)",
        "廉价木桶",
        ["wood"],
        8000,
        False,
        340,
        100,
        {"G": glue, "P": "OD.plankAnyWood", "S": "OP.stickLong.dat(MT.Bronze)"},
    )
    wood(
        32754,
        "fluid_barrel/cheap_brass",
        "Wooden Barrel (Cheap)",
        "廉价木桶",
        ["wood"],
        8000,
        False,
        340,
        100,
        {"G": glue, "P": "OD.plankAnyWood", "S": "OP.stickLong.dat(MT.Brass)"},
    )
    wood(
        32714,
        "fluid_barrel/treated_wood",
        "Wooden Barrel",
        "木桶",
        ["treated_wood", "wood_treated", "wood"],
        16000,
        False,
        340,
        100,
        {
            "G": glue,
            "P": "OP.plate.dat(MT.WoodTreated)",
            "S": "OP.stickLong.dat(ANY.Iron)",
        },
    )
    wood(
        32109,
        "fluid_barrel/rainbowood",
        "Rainbowood Barrel",
        "彩虹木桶",
        ["rainbowood", "rainbow_wood"],
        32000,
        True,
        390,
        0,
        {
            "G": glue,
            "P": "OP.plate.dat(MT.WOODS.Rainbowood)",
            "S": "OP.stickLong.dat(ANY.Iron)",
        },
    )
    wood(
        32019,
        "fluid_barrel/skyroot",
        "Skyroot Barrel",
        "天根木桶",
        ["skyroot"],
        16000,
        True,
        340,
        100,
        {"G": glue, "P": "OD.plankSkyroot", "S": "OP.stickLong.dat(ANY.Iron)"},
    )
    wood(
        32008,
        "fluid_barrel/weedwood",
        "Weedwood Barrel",
        "杂草木桶",
        ["weedwood"],
        8000,
        True,
        340,
        0,
        {
            "G": glue,
            "P": "OD.plankWeedwood",
            "S": "OP.stickLong.dat(MT.Syrmorite)",
        },
    )
    wood(
        32010,
        "fluid_barrel/livingwood",
        "Livingwood Barrel",
        "活木桶",
        ["livingwood"],
        16000,
        True,
        340,
        100,
        {
            "G": glue,
            "P": "OP.plate.dat(MT.Livingwood)",
            "S": "OP.stickLong.dat(ANY.Iron)",
        },
    )
    wood(
        32009,
        "fluid_barrel/dreamwood",
        "Dreamwood Barrel",
        "梦木桶",
        ["dreamwood"],
        64000,
        True,
        340,
        0,
        {
            "G": glue,
            "P": "OP.plate.dat(MT.Dreamwood)",
            "S": "OP.stickLong.dat(ANY.MagicIron)",
        },
    )
    wood(
        32016,
        "fluid_barrel/shimmerwood",
        "Shimmerwood Barrel",
        "微光木桶",
        ["shimmerwood"],
        64000,
        True,
        340,
        0,
        {
            "G": glue,
            "P": "OP.plate.dat(MT.Shimmerwood)",
            "S": "OP.stickLong.dat(ANY.MagicIron)",
        },
    )
    wood(
        32734,
        "fluid_barrel/ironwood",
        "Ironwood Barrel",
        "铁木桶",
        ["ironwood", "iron_wood"],
        32000,
        True,
        None,
        0,
        {
            "G": glue,
            "P": "OP.plate.dat(MT.IronWood)",
            "S": "OP.stickLong.dat(ANY.Iron)",
        },
    )
    wood(
        32017,
        "fluid_barrel/greatwood",
        "Greatwood Barrel",
        "巨木桶",
        ["greatwood"],
        16000,
        True,
        390,
        100,
        {
            "G": glue,
            "P": "OP.plate.dat(MT.Greatwood)",
            "S": "OP.stickLong.dat(ANY.Iron)",
        },
    )
    wood(
        32018,
        "fluid_barrel/silverwood",
        "Silverwood Barrel",
        "银木桶",
        ["silverwood"],
        64000,
        True,
        440,
        0,
        {
            "G": glue,
            "P": "OP.plate.dat(MT.Silverwood)",
            "S": "OP.stickLong.dat(ANY.MagicIron)",
        },
    )
    add(
        32715,
        "fluid_barrel/plastic_canister",
        "Plastic Canister",
        "塑料罐",
        "PLASTIC",
        "MultiTileEntityBarrelPlastic",
        ["plastic"],
        32000,
        True,
        False,
        False,
        False,
        370,
        1.0,
        3.0,
        0,
        "plasticcan",
    )
    drum(32102, "fluid_barrel/bronze_drum", "Bronze Drum", "青铜流体储罐", ["bronze"], 64000, True, False, False, False, 6.0, None, "OP.plateCurved.dat(MT.Bronze)", "OP.stickLong.dat(MT.Bronze)")
    drum(32064, "fluid_barrel/invar_drum", "Invar Drum", "殷钢流体储罐", ["invar"], 64000, True, False, False, False, 6.0, None, "OP.plateCurved.dat(MT.Invar)", "OP.stickLong.dat(MT.Invar)")
    drum(32716, "fluid_barrel/stainless_steel_drum", "Stainless Steel Drum", "不锈钢流体储罐", ["stainless_steel"], 64000, True, True, False, False, 6.0, None, "OP.plateCurved.dat(MT.StainlessSteel)", "OP.stickLong.dat(MT.StainlessSteel)")
    drum(32070, "fluid_barrel/desh_drum", "Desh Drum", "戴斯流体储罐", ["desh"], 64000, True, False, False, True, 6.0, None, "OP.plateCurved.dat(MT.Desh)", "OP.stickLong.dat(MT.Desh)")
    drum(32020, "fluid_barrel/syrmorite_drum", "Syrmorite Drum", "瑟钼流体储罐", ["syrmorite"], 64000, True, True, False, True, 6.0, None, "OP.plateCurved.dat(MT.Syrmorite)", "OP.stickLong.dat(MT.Syrmorite)")
    drum(32093, "fluid_barrel/efrine_drum", "Efrine Drum", "埃弗林流体储罐", ["efrine"], 64000, True, False, True, True, 6.0, None, "OP.plateCurved.dat(MT.Efrine)", "OP.stickLong.dat(MT.Efrine)")
    drum(32022, "fluid_barrel/thaumium_drum", "Thaumium Drum", "神秘流体储罐", ["thaumium"], 64000, True, True, False, True, 6.0, None, "OP.plateCurved.dat(MT.Thaumium)", "OP.stickLong.dat(MT.Thaumium)")
    drum(32023, "fluid_barrel/manasteel_drum", "Manasteel Drum", "魔力钢流体储罐", ["manasteel"], 64000, True, True, False, True, 6.0, None, "OP.plateCurved.dat(MT.Manasteel)", "OP.stickLong.dat(MT.Manasteel)")
    drum(32090, "fluid_barrel/tungsten_alloy_drum", "Tungsten Alloy Drum", "钨合金流体储罐", ["tungsten_alloy", "tungstenalloy"], 128000, True, False, False, True, 9.0, None, "OP.plateCurved.dat(MT.TungstenAlloy)", "OP.stickLong.dat(MT.TungstenAlloy)")
    drum(32742, "fluid_barrel/titanium_drum", "Titanium Drum", "钛流体储罐", ["titanium"], 128000, True, False, False, False, 9.0, None, "OP.plateCurved.dat(MT.Ti)", "OP.stickLong.dat(MT.Ti)")
    drum(32087, "fluid_barrel/netherite_drum", "Netherite Drum", "下界合金流体储罐", ["netherite"], 128000, True, True, True, True, 9.0, None, "OP.plateCurved.dat(MT.Netherite)", "OP.stickLong.dat(MT.Netherite)")
    drum(32717, "fluid_barrel/tungstensteel_drum", "Tungstensteel Drum", "钨钢流体储罐", ["tungstensteel", "tungsten_steel"], 256000, True, False, False, True, 12.5, None, "OP.plateCurved.dat(MT.TungstenSteel)", "OP.stickLong.dat(MT.TungstenSteel)")
    drum(32718, "fluid_barrel/tungsten_drum", "Tungsten Drum", "钨流体储罐", ["tungsten"], 256000, True, True, False, True, 10.0, None, "OP.plateCurved.dat(ANY.W)", "OP.stickLong.dat(ANY.W)")
    drum(32083, "fluid_barrel/tantalum_hafnium_carbide_drum", "Tantalum Hafnium Carbide Drum", "碳化钽铪流体储罐", ["tantalum_hafnium_carbide", "ta4hfc5"], 512000, True, False, False, True, 10.0, None, "OP.plateCurved.dat(MT.Ta4HfC5)", "OP.stickLong.dat(MT.Ta4HfC5)")
    drum(32063, "fluid_barrel/voidmetal_drum", "Voidmetal Drum", "虚空金属流体储罐", ["voidmetal", "void_metal"], 256000, True, True, True, True, 10.0, None, "OP.plateCurved.dat(MT.VoidMetal)", "OP.stickLong.dat(MT.VoidMetal)")
    drum(32024, "fluid_barrel/gaia_drum", "Gaia Drum", "盖亚流体储罐", ["gaia_spirit", "gaia"], 1024000, True, True, True, True, 25.0, None, "OP.plateCurved.dat(MT.GaiaSpirit)", "OP.stickLong.dat(MT.GaiaSpirit)")
    drum(32719, "fluid_barrel/adamantium_drum", "Adamantium Drum", "精金流体储罐", ["adamantium"], 4096000, True, True, True, True, 100.0, None, "OP.plateCurved.dat(MT.Ad)", "OP.stickLong.dat(MT.Ad)")
    drum(32021, "fluid_barrel/draconium_drum", "Draconium Drum", "龙素流体储罐", ["draconium"], 4096000, True, True, True, True, 100.0, None, "OP.plateCurved.dat(MT.Draconium)", "OP.stickLong.dat(MT.Draconium)")
    drum(32066, "fluid_barrel/awakened_draconium_drum", "Awakened Draconium Drum", "觉醒龙素流体储罐", ["draconium_awakened", "awakened_draconium"], 8192000, True, True, True, True, 100.0, 10000, "OP.plateCurved.dat(MT.DraconiumAwakened)", "OP.stickLong.dat(MT.DraconiumAwakened)")
    drum(32067, "fluid_barrel/infinity_drum", "Infinity Drum", "无尽流体储罐", ["infinity"], 10000000000, True, True, True, True, 100.0, 1000000000, "OP.plateCurved.dat(MT.Infinity)", "OP.stickLong.dat(MT.Infinity)")
    add(
        32072,
        "fluid_barrel/logistics_tank",
        "Logistics Tank",
        "物流储罐",
        "LOGISTICS",
        "MultiTileEntityBarrelLogistics",
        ["tungsten"],
        1000000,
        True,
        True,
        True,
        True,
        100000,
        1.0,
        10.0,
        0,
        "logistics",
        ["TQT", "wFd", "TMT"],
        {
            "T": "OP.screw.dat(ANY.W)",
            "Q": "IL.Cover_Logistics_Generic_Storage",
            "F": "IL.FIELD_GENERATORS[0]",
            "M": "aRegistry.getItem(32718)",
        },
    )


def material_id(candidates: list[str]) -> str | None:
    for candidate in candidates:
        if (MATERIALS / f"{candidate}.json").is_file():
            return candidate
    return None


def melting_from_material(material: str | None, explicit: int | None) -> int | None:
    if explicit is not None:
        return explicit
    if material is None:
        return None
    document = json.loads((MATERIALS / f"{material}.json").read_text(encoding="utf-8"))
    thermal = ((document.get("gt6_metadata") or {}).get("source_thermal") or {})
    kelvin = thermal.get("melting_point_kelvin")
    if not isinstance(kelvin, (int, float)) or kelvin <= 0:
        return None
    return int(kelvin * 1.25)


def glowing(material: str | None) -> bool:
    if material is None:
        return False
    document = json.loads((MATERIALS / f"{material}.json").read_text(encoding="utf-8"))
    tags = ((document.get("gt6_metadata") or {}).get("material_tags") or [])
    return any("GLOWING" in str(tag) for tag in tags)


SELF_ITEMS = {
    "aRegistry.getItem(32718)": "cruciblecraft:fluid_barrel/tungsten_drum",
    "IL.Cover_Logistics_Generic_Storage": "cruciblecraft:logistics_generic_storage_cover",
}


def live_gate_item(token: str, resolved: dict) -> str | None:
    """Curved plates sit on the registration gate without a generation flag.

    The shared resolver leaves those as missing, including ANY.W, so other
    recipe matrices stay unchanged. This catalog still uses the live item.
    """
    document = gt6_resolve.resolve(token)
    if document.get("kind") == "prefix_family":
        family = str((document.get("material") or {}).get("gt") or "").split(".", 1)[-1]
        prefer = FAMILY_PREFER.get(family)
        rows = list((document.get("form") or {}).get("items") or [])
        ordered = sorted(
            rows,
            key=lambda row: 0 if row.get("cc_material") == prefer else 1,
        )
        for row in ordered:
            item = row.get("item")
            if isinstance(item, str) and _item_is_live(item):
                return item
    item = resolved.get("cc")
    if (
        resolved.get("reason") == "missing_form"
        and isinstance(item, str)
        and not resolved.get("tag")
        and _item_is_live(item)
    ):
        return item
    return None


def operand_value(token: str) -> dict | None:
    if token in SELF_ITEMS:
        return {"item": SELF_ITEMS[token]}
    resolved = resolve_operand(token)
    if resolved.get("status") != "source_exact":
        item = live_gate_item(token, resolved)
        if item is None:
            return None
        return {"item": item}
    if resolved.get("tag") and not resolved.get("cc"):
        tag = str(resolved["tag"])
        return {"tag": tag}
    if resolved.get("cc"):
        if resolved.get("tag") and resolved.get("kind") == "tag":
            return {"tag": str(resolved["tag"])}
        return {"item": str(resolved["cc"])}
    return None


def recipe_document(row: dict) -> dict | None:
    if not row["pattern"]:
        return None
    ingredients: dict[str, dict] = {}
    catalysts: dict[str, dict] = {}
    for key, token in row["keys"].items():
        value = operand_value(token)
        if value is None:
            return None
        ingredients[key] = value
    for line in row["pattern"]:
        for char in line:
            if char == " " or char in ingredients:
                continue
            if char in CATALYSTS:
                catalysts[char] = {"item": CATALYSTS[char]}
                continue
            return None
    document = {
        "type": "cruciblecraft:shaped_catalyst",
        "pattern": row["pattern"],
        "ingredients": ingredients,
        "result": {"count": 1, "id": f"cruciblecraft:{row['path']}"},
    }
    if catalysts:
        document["catalysts"] = catalysts
    return document


def write_catalog() -> tuple[int, int]:
    identities = []
    written = 0
    blocked = 0
    if RECIPE_ROOT.exists():
        for stale in RECIPE_ROOT.glob("*.json"):
            stale.unlink()
    RECIPE_ROOT.mkdir(parents=True, exist_ok=True)
    for row in ROWS:
        material = material_id(row["materials"])
        melting = melting_from_material(material, row["melting"])
        recipe = recipe_document(row)
        identity = {
            "acid_proof": row["acid_proof"],
            "art": row["art"],
            "capacity": row["capacity"],
            "chinese_name": row["chinese"],
            "english_name": row["english"],
            "flammability": row["flammability"],
            "gas_proof": row["gas_proof"],
            "glowing": glowing(material),
            "gt6_class": row["gt6_class"],
            "hardness": row["hardness"],
            "kind": row["kind"],
            "magic_proof": row["magic_proof"],
            "material_id": material or "",
            "melting_kelvin": melting if melting is not None else 0,
            "meta": row["meta"],
            "plasma_proof": row["plasma_proof"],
            "registry_path": row["path"],
            "resistance": row["resistance"],
            "recipe_status": "source_exact" if recipe else "explicitly_blocked",
        }
        if melting is None:
            identity["melting_kelvin"] = None
        identities.append(identity)
        if recipe:
            (RECIPE_ROOT / f"{row['path'].split('/')[-1]}.json").write_text(
                json.dumps(recipe, indent=2, ensure_ascii=False) + "\n",
                encoding="utf-8",
            )
            written += 1
        else:
            blocked += 1
    document = {
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "status": "FLUID_BARREL_CATALOG",
        "identities": identities,
    }
    CATALOG.write_text(
        json.dumps(document, indent=2, ensure_ascii=False) + "\n",
        encoding="utf-8",
    )
    return written, blocked


def simple_names() -> list[str]:
    names = {"poison", "coffee"}
    enum_line = re.compile(
        r'\("([^"]+)"\s*,([^)]*)\)'
    )
    text = FL_JAVA.read_text(encoding="utf-8", errors="replace")
    for match in enum_line.finditer(text):
        if re.search(r"\bSIMPLE\b", match.group(2)):
            names.add(match.group(1))
    fluids = LOADER_FLUIDS.read_text(encoding="utf-8", errors="replace")
    for match in re.finditer(
        r'FL\.create\(\s*"([^"]+)"[\s\S]{0,400}?FluidsGT\.SIMPLE',
        fluids,
    ):
        names.add(match.group(1))
    for match in re.finditer(
        r"FL\.createMolten\(\s*MT\.(\w+)[\s\S]{0,200}?FluidsGT\.SIMPLE",
        fluids,
    ):
        names.add("molten." + match.group(1).lower())
    dyes = [
        "black",
        "red",
        "green",
        "brown",
        "blue",
        "purple",
        "cyan",
        "lightgray",
        "gray",
        "pink",
        "lime",
        "yellow",
        "lightblue",
        "magenta",
        "orange",
        "white",
    ]
    for dye in dyes:
        names.add(f"dye.watermixed.{dye}")
        names.add(f"dye.flower.{dye}")
        names.add(f"dye.chemical.{dye}")
    return sorted(names)


def write_simple() -> int:
    names = simple_names()
    SIMPLE.write_text(
        json.dumps(
            {
                "schema_version": 1,
                "source_revision": SOURCE_REVISION,
                "names": names,
            },
            indent=2,
        )
        + "\n",
        encoding="utf-8",
    )
    return len(names)


def copy_art() -> list[dict]:
    rows = []
    for family in ("barrel", "drum", "plasticcan", "logistics"):
        for layer in ("colored", "overlay"):
            for face in ("bottom", "top", "side"):
                source = GT6_TEX / family / layer / f"{face}.png"
                if not source.is_file():
                    raise SystemExit(f"missing GT6 texture {source}")
                destination = TEX_ROOT / family / f"{layer}_{face}.png"
                destination.parent.mkdir(parents=True, exist_ok=True)
                shutil.copyfile(source, destination)
                rows.append(
                    {
                        "source": str(source.relative_to(ROOT)).replace("\\", "/"),
                        "gt6_source": (
                            "assets/gregtech/textures/blocks/machines/tanks/"
                            f"{family}/{layer}/{face}.png"
                        ),
                        "destination": str(
                            destination.relative_to(
                                ROOT / "src" / "main" / "resources"
                            )
                        ).replace("\\", "/"),
                    }
                )
    (TEX_ROOT / "art_manifest.json").write_text(
        json.dumps({"rows": rows}, indent=2) + "\n",
        encoding="utf-8",
    )
    return rows


def cube_model(art: str) -> dict:
    textures = {}
    for layer in ("colored", "overlay"):
        for face in ("down", "up", "side"):
            source_face = {"down": "bottom", "up": "top", "side": "side"}[face]
            textures[f"{layer}_{face}"] = (
                f"cruciblecraft:block/gt6_import/fluid_barrel/{art}/{layer}_{source_face}"
            )
    textures["particle"] = textures["colored_side"]
    faces_colored = {}
    faces_overlay = {}
    for face in ("down", "up", "north", "south", "west", "east"):
        key = "side" if face not in ("down", "up") else face
        faces_colored[face] = {
            "texture": f"#colored_{key}",
            "tintindex": 0,
            "cullface": face,
        }
        faces_overlay[face] = {
            "texture": f"#overlay_{key}",
            "cullface": face,
        }
    return {
        "parent": "minecraft:block/block",
        "textures": textures,
        "elements": [
            {
                "from": [0, 0, 0],
                "to": [16, 16, 16],
                "faces": faces_colored,
            },
            {
                "from": [0, 0, 0],
                "to": [16, 16, 16],
                "faces": faces_overlay,
            },
        ],
    }


def write_models() -> None:
    model_root = ASSETS / "models" / "block" / "fluid_barrel"
    item_root = ASSETS / "models" / "item" / "fluid_barrel"
    state_root = ASSETS / "blockstates" / "fluid_barrel"
    for folder in (model_root, item_root, state_root):
        folder.mkdir(parents=True, exist_ok=True)
    arts = sorted({row["art"] for row in ROWS})
    for art in arts:
        (model_root / f"{art}.json").write_text(
            json.dumps(cube_model(art), indent=2) + "\n",
            encoding="utf-8",
        )
    for row in ROWS:
        name = row["path"].split("/")[-1]
        (state_root / f"{name}.json").write_text(
            json.dumps(
                {
                    "variants": {
                        "": {
                            "model": f"cruciblecraft:block/fluid_barrel/{row['art']}"
                        }
                    }
                },
                indent=2,
            )
            + "\n",
            encoding="utf-8",
        )
        (item_root / f"{name}.json").write_text(
            json.dumps(
                {"parent": f"cruciblecraft:block/fluid_barrel/{row['art']}"},
                indent=2,
            )
            + "\n",
            encoding="utf-8",
        )


def main() -> None:
    fill_rows()
    if len(ROWS) != 36:
        raise SystemExit(f"expected 36 barrel rows, got {len(ROWS)}")
    metas = [row["meta"] for row in ROWS]
    if len(metas) != len(set(metas)):
        raise SystemExit("duplicate barrel meta")
    written, blocked = write_catalog()
    simple_count = write_simple()
    art_rows = copy_art()
    write_models()
    print(
        json.dumps(
            {
                "identities": len(ROWS),
                "recipes": written,
                "blocked_recipes": blocked,
                "simple_fluids": simple_count,
                "art": len(art_rows),
            },
            indent=2,
        )
    )


if __name__ == "__main__":
    main()
