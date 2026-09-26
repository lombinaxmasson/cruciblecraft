#!/usr/bin/env python3
"""Resolve a GT6 recipe token to a CrucibleCraft id without inventing parts.

Examples:
  python tools/gt6_resolve.py "OP.capcellcon(MT.Al)"
  python tools/gt6_resolve.py "OP.casingMachineQuadruple(MT.Invar)"
  python tools/gt6_resolve.py "OP.pipeSmall(ANY.Cu)"
  python tools/gt6_resolve.py Shape_Extruder_CCC
"""
from __future__ import annotations

import argparse
import json
import re
import sys
from functools import lru_cache
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import gt6_mapping
from tools import io_common as io

PREFIX_DIR = (
    ROOT / "src" / "main" / "resources" / "data" / "cruciblecraft" / "material_prefixes"
)
MATERIAL_DIR = ROOT / "src" / "main" / "resources" / "data" / "cruciblecraft" / "materials"
CENSUS_PATH = ROOT / "src" / "main" / "resources" / "census" / "runtime_registry_gate.json"
MT_JAVA = ROOT / "gt6_code" / "gregtech6" / "src" / "main" / "java" / "gregapi" / "data" / "MT.java"
ANY_JAVA = ROOT / "gt6_code" / "gregtech6" / "src" / "main" / "java" / "gregapi" / "data" / "ANY.java"
SHAPE_CATALOG = (
    ROOT
    / "src"
    / "main"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "content"
    / "item"
    / "ExtruderShapeCatalog.java"
)
TECH_JAVA = (
    ROOT
    / "gt6_code"
    / "gregtech6"
    / "src"
    / "main"
    / "java"
    / "gregtech"
    / "items"
    / "MultiItemTechnological.java"
)
INPLACE_CATALOG = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "mte_inplace_catalog.json"
)
GT6_ART = (
    ROOT
    / "gt6_referencable_port_code"
    / "gregtech6_w"
    / "src"
    / "main"
    / "resources"
    / "assets"
    / "gregtech"
    / "textures"
)

# OrePrefix names used in recipes that are not prefix-JSON aliases today.
PREFIX_OVERLAY: dict[str, str | None] = {
    "pipeTiny": "tiny_fluid_pipe",
    "pipeSmall": "small_fluid_pipe",
    "pipeMedium": "fluid_pipe",
    "pipeLarge": "large_fluid_pipe",
    "pipeHuge": "huge_fluid_pipe",
    "pipeQuadruple": "quadruple_fluid_pipe",
    "pipeNonuple": "nonuple_fluid_pipe",
    "pipeRestrictiveMedium": "restrictive_item_pipe",
    "pipeRestrictiveLarge": "large_restrictive_item_pipe",
    "pipeRestrictiveHuge": "huge_restrictive_item_pipe",
    "wireGt07": "septuple_wire",
    "wireGt09": "nonuple_wire",
    "wireGt10": "decuple_wire",
    "wireGt11": "undecuple_wire",
    "wireGt13": "tredecuple_wire",
    "wireGt14": "tetradecuple_wire",
    "wireGt15": "pentadecuple_wire",
    "casingMachineQuadruple": "machine_casing_quadruple",
}

# Vanilla / OreDict tokens that are the GT6 object, not a stand-in.
OD_ITEMS: dict[str, dict[str, Any]] = {
    "craftingChest": {
        "item": "minecraft:chest",
        "kind": "od_member",
        "note": "GT6 OD.craftingChest includes vanilla chest; drawers still use aRegistry.getItem.",
    },
    "craftingWorkBench": {"item": "minecraft:crafting_table", "kind": "od_member"},
    "craftingLeather": {"item": "minecraft:leather", "kind": "od_member"},
    "plankAnyWood": {"tag": "minecraft:planks", "kind": "od_tag"},
    "craftingFirestarter": {"item": "minecraft:flint_and_steel", "kind": "od_member"},
    "blockGlassColorless": {"item": "minecraft:glass", "kind": "od_member"},
}
BLOCK_ITEMS: dict[str, str] = {
    "stone": "minecraft:stone",
    "brick_block": "minecraft:bricks",
    "vine": "minecraft:vine",
    "planks": "minecraft:oak_planks",
}
VANILLA_ITEMS: dict[str, str] = {
    "string": "minecraft:string",
}
CIRCUITS: dict[int, str] = {
    1: "cruciblecraft:circuit_basic",
    2: "cruciblecraft:circuit_good",
    3: "cruciblecraft:circuit_advanced",
    4: "cruciblecraft:circuit_elite",
    5: "cruciblecraft:circuit_master",
    6: "cruciblecraft:circuit_ultimate",
    7: "cruciblecraft:circuit_quantum",
}
IL_NAMED_ITEMS: dict[str, str] = {
    "Pellet_Wood": "cruciblecraft:wood_pellet",
    "Comp_Laser_Gas_Empty": "cruciblecraft:laser_gas_empty",
    "Comp_Laser_Gas_He": "cruciblecraft:laser_gas_he",
    "Comp_Laser_Gas_Ne": "cruciblecraft:laser_gas_ne",
    "Comp_Laser_Gas_Ar": "cruciblecraft:laser_gas_ar",
    "Comp_Laser_Gas_Kr": "cruciblecraft:laser_gas_kr",
    "Comp_Laser_Gas_Xe": "cruciblecraft:laser_gas_xe",
    "Comp_Laser_Gas_HeNe": "cruciblecraft:laser_gas_hene",
    "Comp_Laser_Gas_CO": "cruciblecraft:laser_gas_co",
    "Comp_Laser_Gas_CO2": "cruciblecraft:laser_gas_co2",
    "ZPM": "cruciblecraft:zero_point_module",
    "Processor_Crystal_Empty": "cruciblecraft:processor_crystal_empty",
    "Processor_Crystal_Diamond": "cruciblecraft:processor_crystal_diamond",
    "Processor_Crystal_Ruby": "cruciblecraft:processor_crystal_ruby",
    "Processor_Crystal_Emerald": "cruciblecraft:processor_crystal_emerald",
    "Processor_Crystal_Sapphire": "cruciblecraft:processor_crystal_sapphire",
}
_DATA_ARRAY = re.compile(
    r"(CABLES_\d+|WIRES_\d+|Kinetic_T|Electric_T|Heat_T|Flux_T)\s*=\s*\{([^;]*?)\}",
    re.S,
)
_INT_EXPR = re.compile(r"^[0-9+\-*]+$")
_DATA_EMBED = re.compile(r"MT\.DATA\.([A-Za-z][A-Za-z0-9_]*)\[(\d+)\]")
_OD_CIRCUIT = re.compile(r"^OD_CIRCUITS\[(\d+)\]$")
_IL_INDEX = re.compile(r"^IL\.([A-Za-z][A-Za-z0-9_]*)\[(\d+)\]$")
_IL_NAMED = re.compile(r"^IL\.([A-Za-z][A-Za-z0-9_]*)$")
_DATA_INDEX = re.compile(r"^MT\.DATA\.([A-Za-z][A-Za-z0-9_]*)\[(\d+)\]$")
_OD_NAME = re.compile(r"^OD\.([A-Za-z][A-Za-z0-9_]*)$")
_BLOCKS = re.compile(r"^Blocks\.([A-Za-z][A-Za-z0-9_]*)$")
_ITEMS = re.compile(r"^Items\.([A-Za-z][A-Za-z0-9_]*)$")
_GET_ITEM = re.compile(r"^(?:aRegistry\.)?getItem\((\d+)\)$")
_PLANK = re.compile(r"^PlankData\.PLANKS\[(\d+)\]$")
_OP_DAT = re.compile(
    r"^(?:OP\.)?([A-Za-z][A-Za-z0-9_]*)\((?:MT|ANY)\.([A-Za-z][A-Za-z0-9_]*)\)$"
)

_OP_CALL = re.compile(
    r"^(?:OP\.)?([A-Za-z][A-Za-z0-9_]*)\((?:MT|ANY)\.([A-Za-z][A-Za-z0-9_]*)\)$"
)
_ANY_CALL = re.compile(
    r"^(?:OP\.)?([A-Za-z][A-Za-z0-9_]*)\(ANY\.([A-Za-z][A-Za-z0-9_]*)\)$"
)
_MT_ONLY = re.compile(r"^(?:MT\.)?([A-Za-z][A-Za-z0-9_]*)$")
_FIELD = re.compile(
    r"^\s+([A-Za-z][A-Za-z0-9_]*)\s*=\s*([A-Za-z][A-Za-z0-9_]*)\s*\(\s*\)",
    re.MULTILINE,
)
_NAMED = re.compile(
    r"^\s+([A-Za-z][A-Za-z0-9_]*)\s*=\s*[A-Za-z][A-Za-z0-9_]*\s*\(\s*\d+\s*,\s*\"([^\"]+)\"",
    re.MULTILINE,
)
_FACTORY = re.compile(
    r"static OreDictMaterial\s+([A-Za-z][A-Za-z0-9_]*)\s*\(\s*\)\s*\{return \w+\s*\(\s*\d+\s*,\s*\"([^\"]+)\"",
)
_ANY_FIELD = re.compile(
    r"^\s+([A-Za-z][A-Za-z0-9_]*)\s+=\s+any\(\s*\"([^\"]+)\"\s*\)",
    re.MULTILINE,
)
_ANY_MEMBERS = re.compile(
    r"^\s+([A-Za-z][A-Za-z0-9_]*)\s+\.stealLooks.*?addReRegistrationToThis\(([^)]+)\)",
    re.MULTILINE,
)
_CATALOG_SHAPE = re.compile(
    r'(?:empty|shape)\(\s*"([^"]+)"\s*,\s*(\d+)'
)
_TECH_SHAPE = re.compile(
    r"IL\.(Shape_Extruder_[A-Za-z0-9_]+)\s*\.set\(\s*addItem\(\s*(\d+)"
)


def _norm(value: str) -> str:
    return re.sub(r"[^a-z0-9]+", "", value.lower())


@lru_cache(maxsize=1)
def live_prefixes() -> dict[str, dict[str, Any]]:
    index = json.loads((PREFIX_DIR / "index.json").read_text(encoding="utf-8"))
    by_key: dict[str, dict[str, Any]] = {}
    for filename in index:
        document = json.loads((PREFIX_DIR / filename).read_text(encoding="utf-8"))
        cc = document["serialized_path"]
        row = {
            "cc_prefix": cc,
            "live": True,
            "aliases": list(document.get("aliases") or []),
            "generation_flag": document.get("generation_flag"),
        }
        keys = {cc, document["id"].split(":", 1)[-1], *row["aliases"]}
        for key in keys:
            by_key[key] = row
            by_key[_norm(key)] = row
    for gt_prefix, cc_prefix in gt6_mapping.GT6_PREFIX_TO_CC.items():
        by_key.setdefault(gt_prefix, {"cc_prefix": cc_prefix, "live": cc_prefix in by_key, "aliases": []})
        by_key.setdefault(_norm(gt_prefix), by_key[gt_prefix])
    for gt_prefix, cc_prefix in PREFIX_OVERLAY.items():
        row = {
            "cc_prefix": cc_prefix,
            "live": bool(cc_prefix and cc_prefix in by_key),
            "aliases": [],
            "overlay": True,
        }
        by_key[gt_prefix] = row
        by_key[_norm(gt_prefix)] = row
    return by_key


@lru_cache(maxsize=1)
def live_materials() -> dict[str, dict[str, Any]]:
    by_key: dict[str, dict[str, Any]] = {}
    for path in sorted(MATERIAL_DIR.glob("*.json")):
        if path.name == "index.json":
            continue
        document = json.loads(path.read_text(encoding="utf-8"))
        if not isinstance(document, dict) or "id" not in document:
            continue
        material_id = document["id"]
        meta = document.get("gt6_metadata") or {}
        row = {
            "cc_material": material_id,
            "source_name": meta.get("source_name"),
            "kind": "material",
            "live": True,
            "generation_flags": list(document.get("generation_flags") or []),
            "include_prefixes": list(document.get("include_prefixes") or []),
            "form_items": dict(document.get("form_items") or {}),
        }
        keys = [material_id, meta.get("source_name"), *(meta.get("aliases") or [])]
        for key in keys:
            if not key:
                continue
            by_key[_norm(str(key))] = row
            by_key[str(key)] = row
    return by_key


@lru_cache(maxsize=1)
def mt_fields() -> dict[str, str]:
    if not MT_JAVA.is_file():
        return {}
    text = MT_JAVA.read_text(encoding="utf-8", errors="replace")
    factories = {match.group(1): match.group(2) for match in _FACTORY.finditer(text)}
    fields: dict[str, str] = {}
    for match in _FIELD.finditer(text):
        field, factory = match.group(1), match.group(2)
        source_name = factories.get(factory)
        if source_name:
            fields[field] = source_name
    for match in _NAMED.finditer(text):
        fields.setdefault(match.group(1), match.group(2))
    return fields


@lru_cache(maxsize=1)
def any_families() -> dict[str, dict[str, Any]]:
    if not ANY_JAVA.is_file():
        return {}
    text = ANY_JAVA.read_text(encoding="utf-8", errors="replace")
    families: dict[str, dict[str, Any]] = {}
    for match in _ANY_FIELD.finditer(text):
        families[match.group(1)] = {
            "kind": "family",
            "gt": f"ANY.{match.group(1)}",
            "local": match.group(2),
            "members": [],
        }
    for match in _ANY_MEMBERS.finditer(text):
        field = match.group(1)
        members = re.findall(r"MT\.([A-Za-z][A-Za-z0-9_]*)", match.group(2))
        if field in families:
            families[field]["members"] = members
    return families


@lru_cache(maxsize=1)
def registered_ids() -> set[str]:
    if not CENSUS_PATH.is_file():
        return set()
    document = json.loads(CENSUS_PATH.read_text(encoding="utf-8"))
    categories = document.get("categories") or {}
    ids: set[str] = set()
    for key in ("items", "blocks"):
        ids.update(categories.get(key) or [])
    return ids


@lru_cache(maxsize=1)
def extruder_shapes() -> dict[str, dict[str, Any]]:
    shapes: dict[str, dict[str, Any]] = {}
    catalog_by_meta: dict[int, dict[str, Any]] = {}
    if SHAPE_CATALOG.is_file():
        for match in _CATALOG_SHAPE.finditer(SHAPE_CATALOG.read_text(encoding="utf-8")):
            shape_id = match.group(1)
            row = {
                "cc_item": f"cruciblecraft:extruder_shape_{shape_id}",
                "live": True,
            }
            shapes[shape_id] = row
            shapes[_norm(shape_id)] = shapes[shape_id]
            shapes[f"Shape_Extruder_{shape_id}"] = shapes[shape_id]
            catalog_by_meta[int(match.group(2))] = row
    if TECH_JAVA.is_file():
        for match in _TECH_SHAPE.finditer(TECH_JAVA.read_text(encoding="utf-8", errors="replace")):
            token, meta = match.group(1), int(match.group(2))
            suffix = token.removeprefix("Shape_Extruder_").lower()
            row = shapes.get(_norm(suffix)) or shapes.get(token) or catalog_by_meta.get(meta) or {
                "cc_item": None,
                "live": False,
            }
            row = dict(row)
            row["gt"] = token
            row["meta"] = meta
            png = (
                GT6_ART
                / "items"
                / "gt.multiitem.technological"
                / f"{meta}.png"
            )
            if png.is_file():
                row["art"] = io.relative(png)
            shapes[token] = row
            shapes[_norm(token)] = row
            shapes[_norm(suffix)] = row
    return shapes


def resolve_prefix(token: str) -> dict[str, Any]:
    prefixes = live_prefixes()
    row = prefixes.get(token) or prefixes.get(_norm(token))
    if row is None:
        return {"gt": token, "cc_prefix": None, "live": False, "status": "unmapped"}
    if row.get("cc_prefix") is None:
        return {
            "gt": token,
            "cc_prefix": None,
            "live": False,
            "status": "missing_prefix",
        }
    status = "ok" if row.get("live") else "missing_prefix"
    return {"gt": token, "cc_prefix": row["cc_prefix"], "live": bool(row.get("live")), "status": status}


def resolve_material(token: str, *, family: bool = False) -> dict[str, Any]:
    if family:
        families = any_families()
        family_row = families.get(token)
        if family_row is None:
            return {"gt": f"ANY.{token}", "kind": "family", "status": "unmapped"}
        members = []
        for member in family_row["members"]:
            resolved = resolve_material(member)
            if resolved.get("cc_material"):
                members.append(resolved["cc_material"])
        return {
            "gt": family_row["gt"],
            "kind": "family",
            "local": family_row["local"],
            "cc_materials": members,
            "status": "ok" if members else "unmapped",
        }
    materials = live_materials()
    source_name = mt_fields().get(token)
    keys = [token, source_name, _norm(token)]
    if source_name:
        keys.append(_norm(source_name))
    for key in keys:
        if not key:
            continue
        row = materials.get(key) or materials.get(_norm(str(key)))
        if row:
            return {
                "gt": token,
                "kind": "material",
                "source_name": source_name or row.get("source_name"),
                "cc_material": row["cc_material"],
                "live": True,
                "status": "ok",
            }
    return {"gt": token, "kind": "material", "status": "unmapped"}


def form_exists(cc_material: str, cc_prefix: str) -> bool:
    if form_item(cc_material, cc_prefix) is not None:
        return True
    item = f"cruciblecraft:{cc_material}/{cc_prefix}"
    if item in registered_ids():
        return True
    row = live_materials().get(cc_material) or live_materials().get(_norm(cc_material))
    if row is None:
        return False
    if cc_prefix in (row.get("include_prefixes") or []):
        return True
    flag = None
    prefix_row = live_prefixes().get(cc_prefix)
    if prefix_row:
        flag = prefix_row.get("generation_flag")
    if flag and flag in (row.get("generation_flags") or []):
        return True
    return False


def form_item(cc_material: str, cc_prefix: str) -> str | None:
    row = live_materials().get(cc_material) or live_materials().get(_norm(cc_material))
    if row is None:
        return None
    item = (row.get("form_items") or {}).get(cc_prefix)
    return str(item) if item else None


def resolve_form(prefix: dict[str, Any], material: dict[str, Any]) -> dict[str, Any]:
    cc_prefix = prefix.get("cc_prefix")
    cc_material = material.get("cc_material")
    if material.get("kind") == "family":
        items = []
        for member in material.get("cc_materials") or []:
            item = form_item(member, cc_prefix) if cc_prefix else None
            if item is None and cc_prefix:
                item = f"cruciblecraft:{member}/{cc_prefix}"
            items.append(
                {
                    "cc_material": member,
                    "item": item,
                    "registered": bool(item and form_exists(member, cc_prefix)),
                }
            )
        registered_any = any(row["registered"] for row in items)
        return {
            "status": "ok" if prefix.get("status") == "ok" and registered_any else prefix.get("status"),
            "items": items,
            "note": "ANY token; pick a family member, never an unrelated metal.",
        }
    if not cc_prefix or not cc_material:
        return {"status": prefix.get("status") if not cc_prefix else material.get("status"), "item": None}
    item = form_item(cc_material, cc_prefix) or f"cruciblecraft:{cc_material}/{cc_prefix}"
    exists = form_exists(cc_material, cc_prefix)
    return {
        "status": "ok" if exists else "missing_form",
        "item": item,
        "registered": exists,
    }


def _eval_int_token(expr: str) -> str:
    compact = expr.replace(" ", "")
    if not _INT_EXPR.fullmatch(compact):
        return expr
    try:
        return str(int(eval(compact, {"__builtins__": {}}, {})))
    except Exception:
        return expr


def canonicalize(query: str) -> str:
    text = query.strip().strip("\"'")
    text = re.sub(r"\s+", "", text)
    text = text.replace(".dat(", "(")
    text = re.sub(
        r"\[([^\[\]]+)\]",
        lambda match: "[" + _eval_int_token(match.group(1)) + "]",
        text,
    )
    text = re.sub(
        r"(?:aRegistry\.)?getItem\(([^)]+)\)",
        lambda match: "getItem(" + _eval_int_token(match.group(1)) + ")",
        text,
    )
    return text


def expand_mt_data_embeds(token: str) -> str:
    current = token
    for _ in range(8):
        def repl(match: re.Match[str]) -> str:
            name, index = match.group(1), int(match.group(2))
            members = mt_data_arrays().get(name) or []
            if index >= len(members):
                return match.group(0)
            inner = canonicalize(members[index])
            if inner.startswith(("OP.", "MT.", "ANY.", "IL.", "OD.")):
                return inner
            return _qualify_material(inner)

        updated = _DATA_EMBED.sub(repl, current)
        if updated == current:
            break
        current = updated
    expanded = _expand_inner_form(current)
    return expanded or current


def _qualify_material(token: str) -> str:
    if token.startswith(("MT.", "ANY.", "OP.", "IL.", "OD.", "Blocks.", "Items.")):
        return token
    if token in mt_fields():
        return f"MT.{token}"
    if token in any_families():
        return f"ANY.{token}"
    return token


def _expand_inner_form(inner: str) -> str | None:
    match = re.fullmatch(r"(?:OP\.)?([A-Za-z][A-Za-z0-9_]*)\((.+)\)$", inner)
    if not match:
        return None
    prefix, material = match.group(1), match.group(2)
    if material.startswith("ANY."):
        return f"{prefix}({material})"
    if material.startswith("MT."):
        return f"{prefix}({material})"
    qualified = _qualify_material(material)
    if qualified.startswith(("ANY.", "MT.")):
        return f"{prefix}({qualified})"
    return None


@lru_cache(maxsize=1)
def mt_data_arrays() -> dict[str, list[str]]:
    if not MT_JAVA.is_file():
        return {}
    text = MT_JAVA.read_text(encoding="utf-8", errors="replace")
    arrays: dict[str, list[str]] = {}
    for match in _DATA_ARRAY.finditer(text):
        name = match.group(1)
        members = []
        for raw in match.group(2).split(","):
            token = canonicalize(raw)
            if not token:
                continue
            members.append(token)
        arrays[name] = members
    return arrays


@lru_cache(maxsize=1)
def il_modules() -> dict[str, dict[int, str]]:
    from tools import technological_parts_foundation as parts

    return {
        "PISTONS": dict(parts.PISTONS),
        "CONVEYERS": dict(parts.CONVEYERS),
        "PUMPS": dict(parts.PUMPS),
        "ROBOT_ARMS": dict(parts.ROBOT_ARMS),
        "MOTORS": dict(parts.MOTORS),
        "FIELD_GENERATORS": dict(parts.FIELD_GENERATORS),
        "EMITTERS": dict(parts.EMITTERS),
        "SENSORS": dict(parts.SENSORS),
    }


@lru_cache(maxsize=1)
def inplace_by_meta() -> dict[int, dict[str, Any]]:
    if not INPLACE_CATALOG.is_file():
        return {}
    document = json.loads(INPLACE_CATALOG.read_text(encoding="utf-8"))
    rows: dict[int, dict[str, Any]] = {}
    for row in document.get("identities") or []:
        try:
            meta = int(row.get("meta"))
        except (TypeError, ValueError):
            continue
        rows[meta] = row
    return rows


def _item_status(item: str | None, *, kind: str, gt: str, extra: dict[str, Any] | None = None) -> dict[str, Any]:
    result = {"query": gt, "kind": kind, "gt": gt, "item": item}
    if extra:
        result.update(extra)
    if not item:
        result["status"] = "unmapped"
        return result
    if (
        item.startswith("minecraft:")
        or item.startswith("#")
        or kind in {"circuit", "il_module", "il_named", "mte_item"}
    ):
        result["live"] = True
        result["status"] = "ok"
        return result
    live = item in registered_ids()
    result["live"] = live
    result["status"] = "ok" if live else "unmapped"
    return result


def resolve(query: str) -> dict[str, Any]:
    raw = query.strip()
    token = expand_mt_data_embeds(canonicalize(raw))

    circuit = _OD_CIRCUIT.fullmatch(token)
    if circuit:
        item = CIRCUITS.get(int(circuit.group(1)))
        return _item_status(item, kind="circuit", gt=token)

    il_index = _IL_INDEX.fullmatch(token)
    if il_index:
        family, index = il_index.group(1), int(il_index.group(2))
        item = (il_modules().get(family) or {}).get(index)
        extra = {"family": family, "index": index}
        if item is None:
            return {
                "query": raw,
                "kind": "il_module",
                "gt": token,
                "status": "unmapped",
                **extra,
            }
        return _item_status(item, kind="il_module", gt=token, extra=extra)

    data = _DATA_INDEX.fullmatch(token)
    if data:
        name, index = data.group(1), int(data.group(2))
        members = mt_data_arrays().get(name) or []
        if index >= len(members):
            return {"query": raw, "kind": "mt_data", "gt": token, "status": "unmapped"}
        inner = members[index]
        expanded = _expand_inner_form(inner)
        nested = resolve(expanded or _qualify_material(inner))
        nested["query"] = raw
        nested["data_array"] = name
        nested["data_index"] = index
        nested["data_token"] = inner
        return nested

    od_name = _OD_NAME.fullmatch(token)
    if od_name:
        row = OD_ITEMS.get(od_name.group(1))
        if row is None:
            return {"query": raw, "kind": "od", "gt": token, "status": "unmapped"}
        item = row.get("item")
        tag = row.get("tag")
        if tag and not item:
            return {
                "query": raw,
                "kind": "od_tag",
                "gt": token,
                "tag": tag,
                "status": "ok",
                "note": row.get("note"),
            }
        result = _item_status(item, kind=str(row.get("kind") or "od"), gt=token, extra={"note": row.get("note")})
        if tag:
            result["tag"] = tag
        return result

    block = _BLOCKS.fullmatch(token)
    if block:
        item = BLOCK_ITEMS.get(block.group(1))
        return _item_status(item, kind="block", gt=token)

    vanilla = _ITEMS.fullmatch(token)
    if vanilla:
        item = VANILLA_ITEMS.get(vanilla.group(1))
        return _item_status(item, kind="vanilla_item", gt=token)

    get_item = _GET_ITEM.fullmatch(token)
    if get_item:
        meta = int(get_item.group(1))
        row = inplace_by_meta().get(meta)
        item = None
        if row:
            item = str(row.get("runtime_id") or f"cruciblecraft:{row.get('registry_path')}")
        extra = {"meta": meta, "self_ref": True}
        if row:
            extra["registry_path"] = row.get("registry_path")
        return _item_status(item, kind="mte_item", gt=token, extra=extra)

    plank = _PLANK.fullmatch(token)
    if plank:
        return {
            "query": raw,
            "kind": "plank_data",
            "gt": token,
            "index": int(plank.group(1)),
            "status": "unmapped",
            "note": "PlankData.PLANKS is a 1.7.10 wood table; no proven CC plank identity.",
        }

    il_named = _IL_NAMED.fullmatch(token)
    if il_named and not token.startswith("IL.Shape_Extruder_"):
        item = IL_NAMED_ITEMS.get(il_named.group(1))
        if item:
            return _item_status(item, kind="il_named", gt=token)
        return {"query": raw, "kind": "il_named", "gt": token, "status": "unmapped"}

    if raw.startswith("IL.Shape_Extruder_") or raw.startswith("Shape_Extruder_") or token.startswith("IL.Shape_Extruder_") or token.startswith("Shape_Extruder_"):
        raw = token if token.startswith(("IL.Shape_Extruder_", "Shape_Extruder_")) else raw
    if raw.startswith("IL.Shape_Extruder_") or raw.startswith("Shape_Extruder_"):
        token = raw.removeprefix("IL.")
        row = extruder_shapes().get(token) or extruder_shapes().get(_norm(token))
        result = {"query": raw, "kind": "extruder_shape", "gt": token}
        if row is None:
            result["status"] = "unmapped"
            return result
        result.update(row)
        result["status"] = "ok" if row.get("live") else "unmapped"
        return result

    any_match = _ANY_CALL.fullmatch(token)
    if any_match:
        prefix = resolve_prefix(any_match.group(1))
        material = resolve_material(any_match.group(2), family=True)
        form = resolve_form(prefix, material)
        status = form.get("status") or "unmapped"
        if prefix["status"] != "ok":
            status = prefix["status"]
        elif material["status"] != "ok":
            status = material["status"]
        return {
            "query": raw,
            "kind": "prefix_family",
            "prefix": prefix,
            "material": material,
            "form": form,
            "status": status,
        }

    op_match = _OP_CALL.fullmatch(token)
    if op_match:
        prefix = resolve_prefix(op_match.group(1))
        material = resolve_material(op_match.group(2))
        form = resolve_form(prefix, material)
        status = form.get("status") or "unmapped"
        if prefix["status"] != "ok":
            status = prefix["status"]
        elif material["status"] != "ok":
            status = material["status"]
        return {
            "query": raw,
            "kind": "prefix_material",
            "prefix": prefix,
            "material": material,
            "form": form,
            "status": status,
        }

    if token.startswith("ANY."):
        return {"query": raw, **resolve_material(token[4:], family=True)}
    if token.startswith("MT.") or _MT_ONLY.fullmatch(token):
        field = token.removeprefix("MT.")
        return {"query": raw, **resolve_material(field)}
    prefix = resolve_prefix(token)
    if prefix["status"] != "unmapped":
        return {"query": raw, "kind": "prefix", **prefix}
    return {"query": raw, "status": "unmapped"}


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("query", nargs="+", help="GT6 token, e.g. OP.capcellcon(MT.Al)")
    args = parser.parse_args(argv)
    document = resolve(" ".join(args.query))
    sys.stdout.write(json.dumps(document, indent=2, ensure_ascii=False) + "\n")
    return 0 if document.get("status") == "ok" else 1


if __name__ == "__main__":
    raise SystemExit(main())
