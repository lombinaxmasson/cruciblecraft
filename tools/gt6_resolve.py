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
    "casingMachineQuadruple": "machine_casing_quadruple",
}

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
_CATALOG_SHAPE = re.compile(r'shape\(\s*"([^"]+)"')
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
    if SHAPE_CATALOG.is_file():
        for match in _CATALOG_SHAPE.finditer(SHAPE_CATALOG.read_text(encoding="utf-8")):
            shape_id = match.group(1)
            shapes[shape_id] = {
                "cc_item": f"cruciblecraft:extruder_shape_{shape_id}",
                "live": True,
            }
            shapes[_norm(shape_id)] = shapes[shape_id]
            shapes[f"Shape_Extruder_{shape_id}"] = shapes[shape_id]
    if TECH_JAVA.is_file():
        for match in _TECH_SHAPE.finditer(TECH_JAVA.read_text(encoding="utf-8", errors="replace")):
            token, meta = match.group(1), int(match.group(2))
            suffix = token.removeprefix("Shape_Extruder_").lower()
            row = shapes.get(_norm(suffix)) or shapes.get(token) or {
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


def resolve_form(prefix: dict[str, Any], material: dict[str, Any]) -> dict[str, Any]:
    cc_prefix = prefix.get("cc_prefix")
    cc_material = material.get("cc_material")
    if material.get("kind") == "family":
        items = []
        for member in material.get("cc_materials") or []:
            item = f"cruciblecraft:{member}/{cc_prefix}" if cc_prefix else None
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
    item = f"cruciblecraft:{cc_material}/{cc_prefix}"
    exists = form_exists(cc_material, cc_prefix)
    return {
        "status": "ok" if exists else "missing_form",
        "item": item,
        "registered": exists,
    }


def resolve(query: str) -> dict[str, Any]:
    raw = query.strip()
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

    any_match = _ANY_CALL.fullmatch(raw)
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

    op_match = _OP_CALL.fullmatch(raw)
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

    if raw.startswith("ANY."):
        return {"query": raw, **resolve_material(raw[4:], family=True)}
    if raw.startswith("MT.") or _MT_ONLY.fullmatch(raw):
        token = raw.removeprefix("MT.")
        return {"query": raw, **resolve_material(token)}
    prefix = resolve_prefix(raw)
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
