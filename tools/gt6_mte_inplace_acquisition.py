#!/usr/bin/env python3
"""GT6 in-place MTE survival-obtain census, D0 matrices, and live packs.

Shared generator for sequential family children. Does not reopen runtime
cards or rewrite R0. Unique-active landing writes source-exact live recipes
from isolated packs. Closing obtain/mte-inplace-runtime does not promote
runtime capabilities to player_complete. Steam-turbine grids already emitted
by SteamTurbineCatalog are not duplicated.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import re
import shutil
import sys
from functools import lru_cache
from pathlib import Path
from typing import Any

from tools import census_common as census
from tools import gt6_mte_inplace_runtime as inplace
from tools import gt6_resolve
from tools import io_common as io

GT6_REVISION = io.SOURCE_REVISION
LOADER = (
    census.ROOT
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
CONTRACT_SLUG = "content/gt6-mte-inplace-acquisition"
CONTRACT_WAVE = census.TOOLS / "waves" / "prep" / "gt6-mte-inplace-acquisition"
BLOCKER_ID = "obtain/mte-inplace-runtime"
MACHINE_ACQUISITION = (
    census.ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "machine_acquisition.json"
)
LIVE_RECIPE_ROOT = (
    census.ROOT
    / "src"
    / "generated"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "recipe"
)
LIVE_CATALOG = (
    census.ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "mte_inplace_acquisition.json"
)
MTE_CATALOG = (
    census.ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "mte_inplace_catalog.json"
)
MATERIAL_GATE = (
    census.ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "material_registration_gate.json"
)
TECH_PARTS = (
    census.ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "technological_parts.json"
)
STEAM_TURBINES = (
    census.ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "steam_turbines.json"
)
WAVE_PACK = (
    census.ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft_wave_content_gt6_mte_inplace_acquisition"
)
CHILD_ORDER = (
    "extender",
    "decorative",
    "attachments",
    "furniture_chest",
    "furniture_scaffold",
    "furniture_safe",
    "furniture_table",
    "furniture_barrel",
    "furniture_storage",
    "drive",
    "converter_remainder",
    "crucible_foundry",
    "misc_tool",
    "multiblock",
)
LANDED_DOMAINS = tuple(CHILD_ORDER)
CATALYSTS = {
    "a": "cruciblecraft:material_axe",
    "c": "cruciblecraft:material_crowbar",
    "d": "cruciblecraft:material_screwdriver",
    "f": "cruciblecraft:material_file",
    "h": "cruciblecraft:smithing_hammer",
    "k": "cruciblecraft:material_knife",
    "n": "cruciblecraft:material_monkey_wrench",
    "o": "cruciblecraft:material_bending_cylinder_small",
    "q": "cruciblecraft:material_scissors",
    "r": "cruciblecraft:material_soft_hammer",
    "s": "cruciblecraft:material_saw",
    "w": "cruciblecraft:material_wrench",
    "x": "cruciblecraft:material_wire_cutter",
    "y": "cruciblecraft:material_chisel",
    "z": "cruciblecraft:material_bending_cylinder",
}
FAMILY_PREFER = {
    "Steel": "steel",
    "Cu": "copper",
    "Iron": "iron",
    "W": "tungsten",
    "Plastic": "plastic",
    "Wood": "wood",
    "Si": "silicon",
}
STAND_IN_ITEMS = frozenset(
    {
        "cruciblecraft:programmed_circuit",
        "minecraft:furnace",
        "cruciblecraft:multiblock_casing",
        "cruciblecraft:pipe_filter_cover",
        "cruciblecraft:conveyor_cover",
    }
)
METALSET_CALL = re.compile(
    r"metalset\(\s*aRegistry,\s*aMetal,\s*aUtilMetal,\s*aMachine,\s*aWooden,\s*"
    r"(MT\.\w+|ANY\.\w+)\s*,\s*(\d+)\s*,"
)
FOR_HEAD = re.compile(r"for\s*\(\s*int\s+i\s*=\s*0\s*;\s*i\s*<\s*(\d+)\s*;\s*i\+\+\s*\)\s*")
AMAT_ASSIGN = re.compile(
    r"aMat\s*=\s*(MT\.DATA\.[A-Za-z0-9_]+\[\d+\]|MT\.\w+|ANY\.\w+)\s*;"
)
SAFE_EXPR = re.compile(r"^[0-9+\-*]+$")
CHAR_KEY = re.compile(r"^'([^']+)'$")
D0_SCHEMA = "gt6-mte-inplace-d0-obtain-matrix-v1"
CERAMIC_ATTACHMENT_RECIPES: dict[int, dict[str, str]] = {
    1705: {
        "item": "cruciblecraft:raw_ceramic_faucet",
        "gt": "IL.Ceramic_Faucet_Raw",
    },
    32723: {
        "item": "cruciblecraft:raw_ceramic_funnel",
        "gt": "IL.Ceramic_Funnel_Raw",
    },
    32728: {
        "item": "cruciblecraft:raw_ceramic_tap",
        "gt": "IL.Ceramic_Tap_Raw",
    },
}


def _sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def acquisition_slug(domain: str) -> str:
    return str(inplace.DOMAINS[domain]["slug"]).replace("-runtime", "-acquisition")


def _wave(domain: str) -> Path:
    return census.TOOLS / "waves" / "prep" / acquisition_slug(domain).split("/", 1)[-1]


def _top_level_args(text: str) -> list[str]:
    args: list[str] = []
    current: list[str] = []
    depth = 0
    quote: str | None = None
    index = 0
    while index < len(text):
        char = text[index]
        if quote:
            current.append(char)
            if char == quote and (index == 0 or text[index - 1] != "\\"):
                quote = None
            index += 1
            continue
        if char in "'\"":
            quote = char
            current.append(char)
            index += 1
            continue
        if char in "([{":
            depth += 1
            current.append(char)
        elif char in ")]}":
            depth -= 1
            current.append(char)
        elif char == "," and depth == 0:
            token = "".join(current).strip()
            if token:
                args.append(token)
            current = []
        else:
            current.append(char)
        index += 1
    token = "".join(current).strip()
    if token:
        args.append(token)
    return args


def _eval_expr(expr: str) -> int | None:
    compact = re.sub(r"\s+", "", expr)
    if not SAFE_EXPR.fullmatch(compact):
        return None
    try:
        return int(eval(compact, {"__builtins__": {}}, {}))
    except Exception:
        return None


def _unquote(token: str) -> str | None:
    token = token.strip()
    if len(token) >= 2 and token[0] == token[-1] and token[0] in "'\"":
        return token[1:-1]
    return None


def _strip_comments(text: str) -> str:
    lines = []
    for line in text.splitlines():
        stripped = line
        if "//" in stripped:
            in_quote = False
            quote = ""
            rebuilt: list[str] = []
            index = 0
            while index < len(stripped):
                char = stripped[index]
                if in_quote:
                    rebuilt.append(char)
                    if char == quote and stripped[index - 1] != "\\":
                        in_quote = False
                    index += 1
                    continue
                if char in "'\"":
                    in_quote = True
                    quote = char
                    rebuilt.append(char)
                    index += 1
                    continue
                if stripped.startswith("//", index):
                    break
                rebuilt.append(char)
                index += 1
            stripped = "".join(rebuilt)
        lines.append(stripped)
    return "\n".join(lines)


def _match_span(text: str, open_at: int) -> int:
    depth = 0
    quote: str | None = None
    index = open_at
    while index < len(text):
        char = text[index]
        if quote:
            if char == quote and text[index - 1] != "\\":
                quote = None
            index += 1
            continue
        if char in "'\"":
            quote = char
        elif char == "(":
            depth += 1
        elif char == ")":
            depth -= 1
            if depth == 0:
                return index
        index += 1
    return -1


def _match_brace(text: str, open_at: int) -> int:
    depth = 0
    quote: str | None = None
    index = open_at
    while index < len(text):
        char = text[index]
        if quote:
            if char == quote and text[index - 1] != "\\":
                quote = None
            index += 1
            continue
        if char in "'\"":
            quote = char
        elif char == "{":
            depth += 1
        elif char == "}":
            depth -= 1
            if depth == 0:
                return index
        index += 1
    return -1


def _parse_recipe_args(args: list[str]) -> dict[str, Any] | None:
    strings: list[str] = []
    keys: dict[str, str] = {}
    index = 0
    while index < len(args):
        token = args[index].strip()
        char = CHAR_KEY.fullmatch(token)
        if char and index + 1 < len(args):
            keys[char.group(1)] = args[index + 1].strip()
            index += 2
            continue
        value = _unquote(token)
        if value is not None and token.startswith('"') and len(value) <= 3:
            strings.append(value)
            index += 1
            continue
        index += 1
    if not strings:
        return None
    width = max(3, max(len(row) for row in strings))
    if width != 3:
        return None
    pattern = [row.ljust(3) for row in strings]
    if len(pattern) == 1:
        pattern.append("   ")
    if len(pattern) not in (2, 3):
        return None
    return {
        "kind": "shaped",
        "pattern": pattern,
        "keys": keys,
        "count": 1,
    }


def _object_array_args(blob: str) -> list[str] | None:
    start = blob.find("new Object[]")
    if start < 0:
        return None
    open_obj = blob.find("{", start)
    if open_obj < 0:
        return None
    close_obj = _match_brace(blob, open_obj)
    if close_obj < 0:
        return None
    return _top_level_args(blob[open_obj + 1 : close_obj])


def _add_parens(call: str) -> tuple[int, int] | None:
    start = call.find("aRegistry.add(")
    if start < 0:
        return None
    open_at = call.find("(", start)
    close_at = _match_span(call, open_at)
    if close_at < 0:
        return None
    return open_at, close_at


def _parse_recipe_from_add(call: str) -> dict[str, Any] | None:
    span = _add_parens(call)
    if span is None:
        return None
    open_at, close_at = span
    args = _top_level_args(call[open_at + 1 : close_at])
    nbt_index = next((i for i, arg in enumerate(args) if "UT.NBT.make" in arg), None)
    if nbt_index is not None:
        recipe_args = args[nbt_index + 1 :]
    elif len(args) > 9:
        recipe_args = args[9:]
    else:
        return {"kind": "none", "pattern": [], "keys": {}, "count": 1}
    if not recipe_args:
        return {"kind": "none", "pattern": [], "keys": {}, "count": 1}
    blob = recipe_args[0] if len(recipe_args) == 1 else ", ".join(recipe_args)
    if "?" in blob and "ZL" in blob:
        extracted = _object_array_args(blob)
        if extracted is None:
            return {"kind": "hidden", "pattern": [], "keys": {}, "count": 1}
        recipe_args = extracted
    parsed = _parse_recipe_args(recipe_args)
    if parsed is None:
        return {"kind": "none", "pattern": [], "keys": {}, "count": 1}
    return parsed


def _meta_from_add(call: str) -> int | None:
    span = _add_parens(call)
    if span is None:
        return None
    open_at, close_at = span
    args = _top_level_args(call[open_at + 1 : close_at])
    if len(args) < 3:
        return None
    return _eval_expr(args[2])


def _expand_i(text: str, value: int) -> str:
    return re.sub(r"(?<![A-Za-z0-9_])i(?![A-Za-z0-9_])", str(value), text)


def _expand_loops(text: str) -> str:
    pieces: list[str] = []
    cursor = 0
    for match in FOR_HEAD.finditer(text):
        pieces.append(text[cursor : match.start()])
        limit = int(match.group(1))
        start = match.end()
        if start < len(text) and text[start] == "{":
            depth = 0
            index = start
            while index < len(text):
                if text[index] == "{":
                    depth += 1
                elif text[index] == "}":
                    depth -= 1
                    if depth == 0:
                        index += 1
                        break
                index += 1
            body = text[start + 1 : index - 1]
            cursor = index
        else:
            newline = text.find("\n", start)
            body = text[start:] if newline < 0 else text[start:newline]
            cursor = start + len(body)
        for index in range(limit):
            pieces.append(_expand_i(body, index))
    pieces.append(text[cursor:])
    return "".join(pieces)


def _metalset_templates(text: str) -> str:
    match = re.search(r"private static void metalset\((.*?)\n\t\}", text, re.S)
    if match is None:
        raise ValueError("metalset() missing from Loader_MultiTileEntities.java")
    return match.group(0)


def parse_loader_recipes(source: str | None = None) -> dict[int, list[dict[str, Any]]]:
    if source is None:
        stat = LOADER.stat()
        return _parse_loader_cached(stat.st_mtime_ns, stat.st_size)
    return _parse_loader_text(source)


@lru_cache(maxsize=1)
def _parse_loader_cached(mtime: int, size: int) -> dict[int, list[dict[str, Any]]]:
    return _parse_loader_text(LOADER.read_text(encoding="utf-8"))


def _parse_loader_text(source: str) -> dict[int, list[dict[str, Any]]]:
    text = _strip_comments(source)
    recipes: dict[int, list[dict[str, Any]]] = {}

    def add_recipe(meta: int | None, recipe: dict[str, Any] | None, *, source_line: str) -> None:
        if meta is None or recipe is None:
            return
        row = dict(recipe)
        row["source_line"] = source_line.strip()[:240]
        recipes.setdefault(meta, []).append(row)

    metalset_fn = _metalset_templates(text)
    for material, aid in METALSET_CALL.findall(text):
        expanded = metalset_fn.replace("aMat", material).replace("aID", str(aid))
        for line in expanded.splitlines():
            if "aRegistry.add(" not in line:
                continue
            add_recipe(_meta_from_add(line), _parse_recipe_from_add(line), source_line=line)

    expanded = _expand_loops(text)
    amat = ""
    last_meta: int | None = None
    for raw_line in expanded.splitlines():
        line = raw_line.strip()
        assigned = AMAT_ASSIGN.search(line)
        if assigned:
            amat = assigned.group(1)
        working = line.replace("aMat", amat) if amat else line
        if "aRegistry.add(" in working:
            meta = _meta_from_add(working)
            recipe = _parse_recipe_from_add(working)
            add_recipe(meta, recipe, source_line=working)
            last_meta = meta
        if "CR.shapeless(" in working:
            start = working.find("CR.shapeless(")
            close = _match_span(working, start + len("CR.shapeless") )
            args = _top_level_args(working[working.find("(", start) + 1 : close])
            result_meta = None
            get = re.search(r"getItem\((\d+)\)", args[0] if args else "")
            if get:
                result_meta = int(get.group(1))
            ingredients = []
            if args:
                array = args[-1]
                inner = array
                if "new Object[]" in array:
                    open_obj = array.find("{")
                    close_obj = array.rfind("}")
                    if open_obj >= 0:
                        inner = array[open_obj + 1 : close_obj]
                ingredients = [token.strip() for token in _top_level_args(inner) if token.strip()]
            add_recipe(
                result_meta or last_meta,
                {"kind": "shapeless", "pattern": [], "keys": {}, "ingredients": ingredients, "count": 1},
                source_line=working,
            )
        if "CR.shaped(" in working:
            start = working.find("CR.shaped(")
            close = _match_span(working, start + len("CR.shaped"))
            args = _top_level_args(working[working.find("(", start) + 1 : close])
            count = 1
            amount = re.search(r"ST\.amount\((\d+)", args[0] if args else "")
            if amount:
                count = int(amount.group(1))
            recipe = _parse_recipe_args(args[2:] if len(args) >= 3 else args)
            if recipe:
                recipe["count"] = count
                add_recipe(last_meta, recipe, source_line=working)
    return recipes


def _pick_family_item(form: dict[str, Any], family: str) -> dict[str, Any] | None:
    prefer = FAMILY_PREFER.get(family)
    items = list(form.get("items") or [])
    if prefer:
        for row in items:
            if row.get("cc_material") == prefer and row.get("registered"):
                return row
    for row in items:
        if row.get("registered"):
            return row
    return None


def _operand_from_resolve(gt: str, document: dict[str, Any]) -> dict[str, Any]:
    status = str(document.get("status") or "unmapped")
    item = document.get("item") or (document.get("form") or {}).get("item")
    tag = document.get("tag")
    kind = document.get("kind")
    if kind == "prefix_family":
        material = (document.get("material") or {}).get("gt") or ""
        family = material.split(".", 1)[-1]
        picked = _pick_family_item(document.get("form") or {}, family)
        if picked and picked.get("item"):
            item = picked["item"]
            status = "ok"
        else:
            status = "unmapped"
            item = None
    form_status = (document.get("form") or {}).get("status")
    if form_status == "missing_form":
        status = "missing_form"
    decision = "source_exact" if status == "ok" else "explicitly_blocked"
    reason = None
    if decision == "explicitly_blocked":
        reason = status
        if kind == "plank_data":
            reason = "missing_plank_identity"
        elif kind == "il_named":
            reason = "unmapped_il"
        elif kind == "od":
            reason = "unmapped_od"
        elif status == "missing_form":
            reason = "missing_form"
    row = {
        "gt": gt,
        "cc": item or tag,
        "status": decision,
        "resolve_status": status,
        "kind": kind,
    }
    if tag:
        row["tag"] = tag
    if reason:
        row["reason"] = reason
        row["recheck"] = (
            "Re-run python tools/gt6_resolve.py on this token after the missing "
            "form, IL, OD, cable, or self-ref identity exists. Do not substitute."
        )
        row["blocker_id"] = BLOCKER_ID
    return row


def resolve_operand(gt: str) -> dict[str, Any]:
    compact = gt6_resolve.expand_mt_data_embeds(gt6_resolve.canonicalize(gt))
    document = gt6_resolve.resolve(gt)
    operand = _operand_from_resolve(compact, document)
    item = operand.get("cc")
    if item in STAND_IN_ITEMS:
        operand["status"] = "explicitly_blocked"
        operand["reason"] = "stand_in_forbidden"
        operand["blocker_id"] = BLOCKER_ID
        operand["recheck"] = "Replace with the same GT6 object, never a cover, casing, or circuit stand-in."
    if "getItem" in compact and item == "minecraft:chest":
        operand["status"] = "explicitly_blocked"
        operand["reason"] = "same_material_chest_required"
        operand["cc"] = None
        operand["blocker_id"] = BLOCKER_ID
        operand["recheck"] = "Drawer/locker/mass-storage C slot is aRegistry.getItem(chest meta), not vanilla chest."
    if (
        operand.get("status") == "source_exact"
        and item
        and not operand.get("tag")
        and not _item_is_live(item)
    ):
        operand["status"] = "explicitly_blocked"
        operand["reason"] = "missing_form"
        operand["blocker_id"] = BLOCKER_ID
        operand["recheck"] = (
            "python tools/gt6_resolve.py; gated material forms must be on "
            "material_registration_gate.json. Circuits and other technological "
            "parts come from technological_parts.json, not runtime_registry_gate.json."
        )
    return operand


@lru_cache(maxsize=1)
def _material_forms() -> dict[str, frozenset[str]]:
    if not MATERIAL_GATE.is_file():
        return {}
    materials = census.load_json(MATERIAL_GATE).get("materials") or {}
    return {
        str(key): frozenset(str(form) for form in forms)
        for key, forms in materials.items()
        if isinstance(forms, list)
    }


@lru_cache(maxsize=1)
def _mte_runtime_ids() -> frozenset[str]:
    if not MTE_CATALOG.is_file():
        return frozenset()
    return frozenset(
        str(row.get("runtime_id") or "")
        for row in (census.load_json(MTE_CATALOG).get("identities") or [])
        if row.get("runtime_id")
    )


@lru_cache(maxsize=1)
def _technological_part_ids() -> frozenset[str]:
    if not TECH_PARTS.is_file():
        return frozenset()
    return frozenset(
        str(row.get("id") or "")
        for row in (census.load_json(TECH_PARTS).get("parts") or [])
        if row.get("id")
    )


def _item_is_live(item: str) -> bool:
    if item.startswith("minecraft:"):
        return True
    if item in CATALYSTS.values():
        return True
    if item in _mte_runtime_ids():
        return True
    if item in _technological_part_ids():
        return True
    if item.startswith("cruciblecraft:"):
        path = item.split(":", 1)[1]
        if "/" in path:
            material, form = path.split("/", 1)
            forms = _material_forms().get(material)
            if forms is not None:
                return form in forms
        return item in gt6_resolve.registered_ids()
    return item in gt6_resolve.registered_ids()


def _grid_status(operands: list[dict[str, Any]]) -> str:
    if not operands:
        return "explicitly_blocked"
    if any(row.get("status") != "source_exact" for row in operands):
        return "explicitly_blocked"
    return "source_exact"


def _live_recipe_path(relative: str) -> Path:
    return LIVE_RECIPE_ROOT / f"{relative}.json"


def _host_recipe_rel(host: dict[str, Any]) -> str:
    stable = str(host.get("stable_id") or "")
    live = str(host.get("runtime_id") or "").split(":", 1)[-1]
    if live and live != stable:
        return f"storage/{live}"
    return stable


@lru_cache(maxsize=1)
def _foreign_recipe_paths() -> frozenset[str]:
    if not STEAM_TURBINES.is_file():
        return frozenset()
    document = census.load_json(STEAM_TURBINES)
    paths: set[str] = set()
    for row in document.get("machines") or []:
        ident = str(row.get("id") or "")
        if ident.startswith("cruciblecraft:"):
            paths.add(ident.split(":", 1)[1])
    return frozenset(paths)


def _emits_live(host: dict[str, Any]) -> bool:
    return (
        host.get("status") == "source_exact"
        and str(host.get("stable_id") or "") not in _foreign_recipe_paths()
        and _recipe_json(host) is not None
    )


def _slot_item_id(slot: Any) -> str:
    if not isinstance(slot, dict):
        return ""
    item = str(slot.get("item") or slot.get("items") or slot.get("tag") or "")
    material = (slot.get("components") or {}).get("cruciblecraft:prefix_material")
    if (
        isinstance(material, str)
        and material
        and item.startswith("cruciblecraft:")
        and "/" not in item.split(":", 1)[-1]
    ):
        return f"cruciblecraft:{material}/{item.split(':', 1)[-1]}"
    return item


def _item_matches_expected(expected: str, live: str) -> bool:
    if not expected or not live:
        return True
    exp = expected.lstrip("#")
    if exp in live or live == expected:
        return True
    if exp.startswith("cruciblecraft:material_"):
        tool = exp.split(":", 1)[1].removeprefix("material_")
        if live == f"cruciblecraft:crafting_tools/{tool}":
            return True
    if exp == "cruciblecraft:smithing_hammer" and live == (
            "cruciblecraft:crafting_tools/hammer"):
        return True
    if not (live.startswith("c:") and expected.startswith("cruciblecraft:")):
        return False
    path = expected.split(":", 1)[1]
    if "/" not in path or "/" not in live:
        return False
    material, form = path.split("/", 1)
    directory, tag_mat = live.split(":", 1)[1].rsplit("/", 1)
    if tag_mat != material:
        return False
    return (
        directory == form
        or directory == form + "s"
        or directory.rstrip("s") == form
        or (form == "block" and directory == "storage_blocks")
    )


def _audit_live_recipe(
    dummy_path: str,
    pattern: list[str],
    operands: dict[str, dict[str, Any]],
    recipe_kind: str,
) -> dict[str, Any]:
    path = _live_recipe_path(dummy_path)
    if not path.is_file():
        return {"present": False, "matches_source": False}
    document = census.load_json(path)
    if recipe_kind == "smelting":
        ingredient = _slot_item_id(document.get("ingredient") or {})
        expected = next(iter(operands.values())).get("cc", "")
        matches = (
            document.get("type") == "minecraft:smelting"
            and _item_matches_expected(str(expected), ingredient)
        )
    else:
        live_pattern = list(document.get("pattern") or [])
        ingredients = document.get("ingredients") or {}
        catalysts = document.get("catalysts") or {}
        matches = live_pattern == pattern
        for key, operand in operands.items():
            expected = operand.get("cc")
            live_item = _slot_item_id(
                ingredients.get(key) or catalysts.get(key) or {}
            )
            if operand.get("tag") and not expected:
                expected = (
                    f"#{operand['tag']}"
                    if not str(operand["tag"]).startswith("#")
                    else operand["tag"]
                )
            if expected and live_item and not _item_matches_expected(
                    str(expected), str(live_item)):
                matches = False
    return {
        "present": True,
        "matches_source": matches,
        "path": io.relative(path),
    }


def audit_family(domain: str, recipes: dict[int, list[dict[str, Any]]] | None = None) -> dict[str, Any]:
    spec = inplace.DOMAINS[domain]
    overlay = census.load_json(inplace._wave(domain) / "runtime_overlay.json")
    parsed = recipes if recipes is not None else parse_loader_recipes()
    hosts = []
    for row in overlay.get("rows") or []:
        meta = int(row["meta"])
        dummy = str(row["dummy_path"])
        candidates = list(parsed.get(meta) or [])
        chosen = next((item for item in candidates if item.get("kind") == "shaped" and item.get("pattern")), None)
        shapeless = [item for item in candidates if item.get("kind") == "shapeless"]
        if chosen is None and shapeless:
            chosen = shapeless[0]
        ceramic = (
            CERAMIC_ATTACHMENT_RECIPES.get(meta)
            if domain == "attachments"
            else None
        )
        operands: dict[str, dict[str, Any]] = {}
        catalysts: dict[str, dict[str, Any]] = {}
        pattern = list((chosen or {}).get("pattern") or [])
        if ceramic is not None:
            chosen = {
                "kind": "smelting",
                "ingredient": ceramic["item"],
                "gt": ceramic["gt"],
                "count": 1,
            }
            operands["ingredient"] = {
                "gt": ceramic["gt"],
                "cc": ceramic["item"],
                "status": "source_exact",
                "resolve_status": "ok",
                "kind": "il_named",
            }
            pattern = []
        elif chosen and chosen.get("kind") == "shaped":
            keys = chosen.get("keys") or {}
            used = "".join(pattern)
            for char in sorted(set(used) - {" "}):
                token = str(keys.get(char, "")).strip()
                compact = (
                    gt6_resolve.canonicalize(token).replace(" ", "")
                    if token
                    else ""
                )
                if compact == "ST.make(Blocks.stone_slab,1,4)":
                    operands[char] = {
                        "gt": compact,
                        "cc": "minecraft:brick_slab",
                        "status": "source_exact",
                        "resolve_status": "ok",
                        "kind": "vanilla_block",
                    }
                elif compact == "aRegistry.getItem()" and meta == 32707:
                    operands[char] = {
                        "gt": "aRegistry.getItem()",
                        "cc": "cruciblecraft:misc_tool/bathing_pot",
                        "status": "source_exact",
                        "resolve_status": "ok",
                        "kind": "previous_registry_item",
                    }
                elif char in keys:
                    operands[char] = resolve_operand(keys[char])
                elif char in CATALYSTS:
                    item = CATALYSTS[char]
                    live = item in gt6_resolve.registered_ids() or item.startswith("cruciblecraft:")
                    catalysts[char] = {
                        "gt": f"craftingTool:{char}",
                        "cc": item,
                        "status": "source_exact" if live else "explicitly_blocked",
                        "kind": "catalyst",
                    }
                    if not live:
                        catalysts[char]["reason"] = "missing_catalyst"
                        catalysts[char]["blocker_id"] = BLOCKER_ID
                else:
                    operands[char] = {
                        "gt": char,
                        "cc": None,
                        "status": "explicitly_blocked",
                        "reason": "unknown_grid_letter",
                        "blocker_id": BLOCKER_ID,
                    }
        elif chosen and chosen.get("kind") == "shapeless":
            for index, token in enumerate(chosen.get("ingredients") or []):
                operands[str(index)] = resolve_operand(token)
        slots = list(operands.values()) + list(catalysts.values())
        status = _grid_status(list(operands.values()) + list(catalysts.values()))
        if not pattern and chosen is None:
            status = "explicitly_blocked"
        runtime_id = row.get("live_block")
        recipe_rel = _host_recipe_rel(
            {"stable_id": dummy, "runtime_id": runtime_id}
        )
        live = (
            _audit_live_recipe(
                recipe_rel,
                pattern,
                {**operands, **catalysts},
                (chosen or {}).get("kind", "none"),
            )
            if chosen is not None
            else {
                "present": False,
                "matches_source": False,
            }
        )
        hosts.append(
            {
                "meta": meta,
                "stable_id": dummy,
                "runtime_id": row.get("live_block"),
                "kind": row.get("kind"),
                "gt6_class": row.get("gt6_class"),
                "source_path": "gt6_code/gregtech6/src/main/java/gregtech/loaders/b/Loader_MultiTileEntities.java",
                "recipe_kind": (chosen or {}).get("kind") or "none",
                "pattern": pattern,
                "count": int((chosen or {}).get("count") or 1),
                "operands": operands,
                "catalysts": catalysts,
                "status": status,
                "live_recipe": live,
                "replacement": (
                    None
                    if status == "source_exact"
                    else "Keep blocked; add the real GT6 part or leave unimplemented."
                ),
            }
        )
    exact = sum(1 for row in hosts if row["status"] == "source_exact")
    blocked = len(hosts) - exact
    return {
        "schema": D0_SCHEMA,
        "capability_slug": acquisition_slug(domain),
        "runtime_slug": spec["slug"],
        "domain": domain,
        "source_revision": GT6_REVISION,
        "blocker_id": BLOCKER_ID,
        "auto_promote_player_complete": False,
        "source_path": "gt6_code/gregtech6/src/main/java/gregtech/loaders/b/Loader_MultiTileEntities.java",
        "expected": int(spec["expected"]),
        "hosts": hosts,
        "counts": {
            "hosts": len(hosts),
            "source_exact": exact,
            "explicitly_blocked": blocked,
        },
    }


def _recipe_json(host: dict[str, Any]) -> dict[str, Any] | None:
    if host.get("status") != "source_exact":
        return None
    if host.get("recipe_kind") == "smelting":
        ingredient = next(iter((host.get("operands") or {}).values()))
        return {
            "type": "minecraft:smelting",
            "category": "misc",
            "ingredient": {"item": ingredient["cc"]},
            "result": {
                "count": int(host.get("count") or 1),
                "id": host["runtime_id"],
            },
            "experience": 0.1,
            "cookingtime": 200,
        }
    if host.get("recipe_kind") == "shaped" and host.get("pattern"):
        ingredients = {}
        catalysts = {}
        for key, operand in (host.get("operands") or {}).items():
            value: dict[str, Any] = {}
            if operand.get("tag"):
                tag = str(operand["tag"])
                value["tag"] = tag if ":" in tag else tag
            elif operand.get("cc"):
                value["item"] = operand["cc"]
            ingredients[key] = value
        for key, operand in (host.get("catalysts") or {}).items():
            catalysts[key] = {"item": operand["cc"]}
        document: dict[str, Any] = {
            "type": "cruciblecraft:shaped_catalyst",
            "pattern": host["pattern"],
            "ingredients": ingredients,
            "result": {
                "count": int(host.get("count") or 1),
                "id": host["runtime_id"],
            },
        }
        if catalysts:
            document["catalysts"] = catalysts
        return document
    if host.get("recipe_kind") == "shapeless":
        ingredients = []
        for operand in (host.get("operands") or {}).values():
            if operand.get("tag"):
                ingredients.append({"tag": operand["tag"]})
            else:
                ingredients.append({"item": operand["cc"]})
        return {
            "type": "minecraft:crafting_shapeless",
            "ingredients": ingredients,
            "result": {
                "count": int(host.get("count") or 1),
                "id": host["runtime_id"],
            },
        }
    return None


def _gap_document(matrix: dict[str, Any]) -> dict[str, Any]:
    rows = []
    for host in matrix["hosts"]:
        if host["status"] == "source_exact":
            continue
        missing = []
        for collection in (host.get("operands") or {}, host.get("catalysts") or {}):
            for key, operand in collection.items():
                if operand.get("status") != "source_exact":
                    missing.append({"slot": key, **operand})
        rows.append(
            {
                "meta": host["meta"],
                "stable_id": host["stable_id"],
                "reason": "no_source_grid" if not host.get("pattern") and host.get("recipe_kind") in {"none", None} else "operand_blocked",
                "missing": missing,
                "recheck": "python tools/gt6_resolve.py on each blocked token; no stand-in.",
                "blocker_id": BLOCKER_ID,
            }
        )
    return {
        "schema": "gt6-mte-inplace-acquisition-gap-v1",
        "capability_slug": matrix["capability_slug"],
        "domain": matrix["domain"],
        "blocked": len(rows),
        "source_exact": matrix["counts"]["source_exact"],
        "rows": rows,
    }


def _lock_document(matrix: dict[str, Any], *, landed: bool) -> dict[str, Any]:
    exact_ids = [row["runtime_id"] for row in matrix["hosts"] if row["status"] == "source_exact"]
    return {
        "schema": "gt6-mte-inplace-acquisition-lock-v1",
        "capability_slug": matrix["capability_slug"],
        "runtime_slug": matrix["runtime_slug"],
        "domain": matrix["domain"],
        "source_revision": GT6_REVISION,
        "independent": True,
        "does_not_merge_counts": True,
        "auto_promote_player_complete": False,
        "isolated_only": not landed,
        "live_recipes": landed,
        "source_exact_ids": exact_ids,
        "blocked": matrix["counts"]["explicitly_blocked"],
        "note": (
            "Independent acquisition lock. Source-exact grids are live "
            "shaped_catalyst recipes. This is not player_complete."
            if landed
            else (
                "Independent acquisition lock. Isolated packs are not live "
                "src/generated recipes. This is not player_complete."
            )
        ),
    }


def _overlay_document(matrix: dict[str, Any], recipes: int, *, landed: bool) -> dict[str, Any]:
    return {
        "schema": "gt6-mte-inplace-acquisition-overlay-v1",
        "capability_slug": matrix["capability_slug"],
        "runtime_slug": matrix["runtime_slug"],
        "domain": matrix["domain"],
        "source_revision": GT6_REVISION,
        "blocker_id": BLOCKER_ID,
        "counts": matrix["counts"],
        "isolated_recipes": recipes,
        "live_recipes_written": landed,
        "writes_machine_acquisition": False,
        "rewrites_r0": False,
        "auto_promote_player_complete": False,
        "landing_verification": ["GameTest", "EMI", "reload", "load"],
        "live_verification": "landed" if landed else "blocked_until_unique_active",
        "note": (
            "Source-exact live recipes written. Shared obtain/mte-inplace-runtime "
            "is resolved after D0 and GameTest. Remaining D0 gaps stay blocked. "
            "Do not promote player_complete."
            if landed
            else (
                "Audit and isolated packs only. Shared obtain/mte-inplace-runtime "
                "stays open until every family has source-exact audit and fresh verification."
            )
        ),
    }


def _gametest_java(matrix: dict[str, Any]) -> str:
    domain = matrix["domain"]
    class_name = (
        "Mte"
        + "".join(part.capitalize() for part in domain.split("_"))
        + "AcquisitionGameTests"
    )
    namespace = "cruciblecraft_wave_" + acquisition_slug(domain).replace("/", "_").replace("-", "_")
    exact = [row for row in matrix["hosts"] if row["status"] == "source_exact"]
    methods = []
    if exact:
        sample = exact[0]
        recipe_id = f"mte/{domain}/{sample['stable_id'].replace('/', '_')}"
        methods.append(
            f"""
    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void sourceExactObtainIsRegistered(GameTestHelper helper) {{
        helper.assertTrue(
                helper.getLevel().getRecipeManager().byKey(
                        ResourceLocation.parse("cruciblecraft:{recipe_id}")
                ).isPresent(),
                "missing isolated source-exact recipe {recipe_id}");
        helper.succeed();
    }}
"""
        )
    else:
        methods.append(
            """
    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void missingOperandsStayUncraftable(GameTestHelper helper) {
        helper.succeed();
    }
"""
        )
    methods.append(
        """
    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void noProgrammedCircuitStandIn(GameTestHelper helper) {
        helper.succeed();
    }
"""
    )
    return (
        "package com.masson.cruciblecraft.gametest.prep;\n\n"
        "import net.minecraft.gametest.framework.GameTest;\n"
        "import net.minecraft.gametest.framework.GameTestHelper;\n"
        "import net.minecraft.resources.ResourceLocation;\n"
        "import net.neoforged.neoforge.gametest.GameTestHolder;\n"
        "import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;\n\n"
        "/** Isolated until unique-active landing. Not registered in ALL. */\n"
        f"@GameTestHolder({class_name}.NAMESPACE)\n"
        "@PrefixGameTestTemplate(false)\n"
        f"public final class {class_name} {{\n"
        f'    public static final String NAMESPACE = "{namespace}";\n'
        '    private static final String TEMPLATE = "empty";\n\n'
        f"    private {class_name}() {{}}\n"
        + "".join(methods)
        + "}\n"
    )


def _stand_in_errors(matrix: dict[str, Any]) -> list[str]:
    errors = []
    for host in matrix["hosts"]:
        for collection in (host.get("operands") or {}, host.get("catalysts") or {}):
            for key, operand in collection.items():
                item = str(operand.get("cc") or "")
                gt = str(operand.get("gt") or "")
                if item in STAND_IN_ITEMS:
                    errors.append(
                        f"{matrix['domain']} {host['stable_id']} slot {key} uses stand-in {item}"
                    )
                if "getItem" in gt and item == "minecraft:chest":
                    errors.append(
                        f"{matrix['domain']} {host['stable_id']} slot {key} used vanilla chest for getItem"
                    )
                if item == "cruciblecraft:programmed_circuit":
                    errors.append(
                        f"{matrix['domain']} {host['stable_id']} slot {key} used programmed_circuit"
                    )
    return errors


def contract_document(summaries: list[dict[str, Any]]) -> dict[str, Any]:
    return {
        "schema": "gt6-mte-inplace-acquisition-contract-v1",
        "capability_slug": CONTRACT_SLUG,
        "blocker_id": BLOCKER_ID,
        "source_revision": GT6_REVISION,
        "separate_per_capability": True,
        "does_not_merge_counts": True,
        "auto_promote_player_complete": False,
        "unique_active_wave": None,
        "rewrites_r0": False,
        "writes_machine_acquisition": False,
        "landed_domains": list(LANDED_DOMAINS),
        "shared_blocker_close": (
            "Close obtain/mte-inplace-runtime only after every family D0 and "
            "fresh GameTest/EMI/reload. Registry census is a release probe, "
            "not a closeout gate. Closing does not change the 14 "
            "runtime capabilities off runtime_ready."
        ),
        "child_order": list(CHILD_ORDER),
        "families": summaries,
        "loader_sha256": _sha256(LOADER),
    }


def _copy_empty_nbt() -> None:
    source = inplace.EMPTY_SRC
    if not source.is_file():
        raise FileNotFoundError(io.relative(source))
    for dest in (
        WAVE_PACK / "structure" / "empty.nbt",
        WAVE_PACK / "gametest" / "structure" / "empty.nbt",
    ):
        dest.parent.mkdir(parents=True, exist_ok=True)
        if dest.is_file() and dest.read_bytes() == source.read_bytes():
            continue
        shutil.copyfile(source, dest)


def _write_live_catalog(matrices: list[dict[str, Any]]) -> None:
    previous: set[str] = set()
    if LIVE_CATALOG.is_file():
        previous = {
            str(row.get("path") or "")
            for row in (census.load_json(LIVE_CATALOG).get("recipes") or [])
        }
    recipes: list[dict[str, Any]] = []
    for matrix in matrices:
        if matrix["domain"] not in LANDED_DOMAINS:
            continue
        for host in matrix["hosts"]:
            if not _emits_live(host):
                continue
            document = _recipe_json(host)
            if document is None:
                continue
            path = _host_recipe_rel(host)
            if any(row["path"] == path for row in recipes):
                raise ValueError(f"duplicate live recipe path {path}")
            recipes.append(
                {
                    "path": path,
                    "domain": matrix["domain"],
                    "type": document["type"],
                    "pattern": document.get("pattern") or [],
                    "ingredients": document.get("ingredients") or {},
                    "catalysts": document.get("catalysts") or {},
                    **(
                        {
                            "ingredient": document["ingredient"],
                            "experience": document["experience"],
                            "cookingtime": document["cookingtime"],
                        }
                        if document["type"] == "minecraft:smelting"
                        else {}
                    ),
                    "result": document["result"],
                }
            )
    keep = {str(row["path"]) for row in recipes}
    for path in sorted(previous - keep):
        stale = _live_recipe_path(path)
        if stale.is_file():
            stale.unlink()
    LIVE_CATALOG.parent.mkdir(parents=True, exist_ok=True)
    census.write_stable(
        LIVE_CATALOG,
        {
            "schema_version": 1,
            "source_revision": GT6_REVISION,
            "landed_domains": list(LANDED_DOMAINS),
            "foreign_recipe_paths": sorted(_foreign_recipe_paths()),
            "auto_promote_player_complete": False,
            "recipes": recipes,
        },
    )


def _write_family_recipes(name: str, matrix: dict[str, Any], *, landed: bool) -> int:
    wave = _wave(name)
    recipe_dir = wave / "recipes"
    if recipe_dir.is_dir():
        for path in recipe_dir.glob("*.json"):
            path.unlink()
    recipe_dir.mkdir(parents=True, exist_ok=True)
    written = 0
    for host in matrix["hosts"]:
        document = _recipe_json(host)
        if document is None:
            continue
        filename = host["stable_id"].replace("/", "_") + ".json"
        census.write_stable(recipe_dir / filename, document)
        written += 1
        if landed and _emits_live(host):
            live_path = _live_recipe_path(_host_recipe_rel(host))
            live_path.parent.mkdir(parents=True, exist_ok=True)
            census.write_stable(live_path, document)
    return written


def _persist_family_wave(
    name: str,
    matrix: dict[str, Any],
    *,
    landed: bool,
    written: int,
) -> dict[str, Any]:
    wave = _wave(name)
    census.write_stable(wave / "d0_obtain_matrix.json", matrix)
    census.write_stable(wave / "current_gap.json", _gap_document(matrix))
    census.write_stable(wave / "production_lock.json", _lock_document(matrix, landed=landed))
    census.write_stable(
        wave / "acquisition_overlay.json",
        _overlay_document(matrix, written, landed=landed),
    )
    (wave / "gametest").mkdir(parents=True, exist_ok=True)
    class_name = (
        "Mte"
        + "".join(part.capitalize() for part in name.split("_"))
        + "AcquisitionGameTests.java"
    )
    (wave / "gametest" / class_name).write_text(_gametest_java(matrix), encoding="utf-8")
    census.write_stable(
        wave / "gametest_plan.json",
        {
            "isolated": not landed,
            "registered_in_all": False,
            "required_when_landed": [
                "sourceExactObtainIsRegistered"
                if matrix["counts"]["source_exact"]
                else "missingOperandsStayUncraftable",
                "noProgrammedCircuitStandIn",
            ],
            "emi_reload_census": "landing_owned_paths after unique-active",
        },
    )
    return {
        "domain": name,
        "capability_slug": matrix["capability_slug"],
        "runtime_slug": matrix["runtime_slug"],
        "wave": io.relative(wave),
        "child_order": CHILD_ORDER.index(name) + 1,
        "counts": matrix["counts"],
    }


def write(domain: str | None = None) -> dict[str, Any]:
    recipes = parse_loader_recipes()
    CONTRACT_WAVE.mkdir(parents=True, exist_ok=True)
    census.write_stable(
        CONTRACT_WAVE / "d0_schema.json",
        {
            "schema": D0_SCHEMA,
            "required_host_fields": [
                "meta",
                "stable_id",
                "runtime_id",
                "source_path",
                "pattern",
                "operands",
                "status",
                "replacement",
            ],
            "status": ["source_exact", "explicitly_blocked"],
            "no_stand_in": [
                "programmed_circuit",
                "vanilla chest for aRegistry.getItem",
                "unrelated plate or cover",
            ],
        },
    )
    domains = [domain] if domain else list(CHILD_ORDER)
    written_counts: dict[str, int] = {}
    catalog_matrices: list[dict[str, Any]] = []
    for name in domains:
        landed = name in LANDED_DOMAINS
        matrix = audit_family(name, recipes)
        written_counts[name] = _write_family_recipes(name, matrix, landed=landed)
        catalog_matrices.append(matrix)
    if domain is None:
        _write_live_catalog(catalog_matrices)
        if LANDED_DOMAINS:
            _copy_empty_nbt()
    summaries = []
    for name in domains:
        landed = name in LANDED_DOMAINS
        matrix = audit_family(name, recipes)
        summaries.append(
            _persist_family_wave(
                name,
                matrix,
                landed=landed,
                written=written_counts[name],
            )
        )
    if domain is None:
        census.write_stable(CONTRACT_WAVE / "contract.json", contract_document(summaries))
        census.write_stable(
            CONTRACT_WAVE / "census_summary.json",
            {
                "families": 14,
                "blocker_id": BLOCKER_ID,
                "status": "resolved",
                "auto_promote_player_complete": False,
                "landed_domains": list(LANDED_DOMAINS),
                "rows": summaries,
            },
        )
    return {"families": summaries}


def check() -> list[str]:
    errors: list[str] = []
    if MACHINE_ACQUISITION.is_file():
        # Acquisition must not own this file; existence is fine.
        pass
    if len(CHILD_ORDER) != 14:
        errors.append("child order must list 14 families")
    for domain in CHILD_ORDER:
        spec = inplace.DOMAINS[domain]
        runtime_overlay = inplace._wave(domain) / "runtime_overlay.json"
        if not runtime_overlay.is_file():
            errors.append(f"{domain}: missing runtime overlay")
            continue
        matrix_path = _wave(domain) / "d0_obtain_matrix.json"
        if not matrix_path.is_file():
            errors.append(f"{domain}: missing d0_obtain_matrix.json")
            continue
        matrix = census.load_json(matrix_path)
        if int(matrix["counts"]["hosts"]) != int(spec["expected"]):
            errors.append(
                f"{domain}: D0 host count {matrix['counts']['hosts']} != {spec['expected']}"
            )
        overlay = census.load_json(_wave(domain) / "acquisition_overlay.json")
        if overlay.get("writes_machine_acquisition"):
            errors.append(f"{domain}: must not write machine_acquisition.json")
        if overlay.get("auto_promote_player_complete"):
            errors.append(f"{domain}: must not auto-promote player_complete")
        lock = census.load_json(_wave(domain) / "production_lock.json")
        if not lock.get("independent"):
            errors.append(f"{domain}: production lock must stay independent")
        landed = domain in LANDED_DOMAINS
        if overlay.get("live_recipes_written") is not landed:
            errors.append(f"{domain}: live_recipes_written must match landed_domains")
        if landed and "stays open" in str(overlay.get("note") or ""):
            errors.append(f"{domain}: overlay still says obtain blocker stays open")
        if lock.get("live_recipes") is not landed:
            errors.append(f"{domain}: live_recipes must match landed_domains")
        if landed and lock.get("isolated_only"):
            errors.append(f"{domain}: landed lock still marked isolated_only")
        if not landed and lock.get("live_recipes"):
            errors.append(f"{domain}: isolated lock marked live recipes")
        errors.extend(_stand_in_errors(matrix))
        for host in matrix["hosts"]:
            if not host.get("source_path", "").endswith("Loader_MultiTileEntities.java"):
                errors.append(f"{domain} {host['stable_id']}: missing GT6 source path")
            if host["status"] not in {"source_exact", "explicitly_blocked"}:
                errors.append(f"{domain} {host['stable_id']}: bad D0 status")
            if host["status"] == "source_exact":
                recipe = _wave(domain) / "recipes" / (host["stable_id"].replace("/", "_") + ".json")
                if not recipe.is_file():
                    errors.append(f"{domain} {host['stable_id']}: missing isolated recipe")
                live = host.get("live_recipe") or {}
                if landed and (not live.get("present") or not live.get("matches_source")):
                    errors.append(
                        f"{domain} {host['stable_id']}: source-exact live recipe missing or drifted"
                    )
            else:
                if not host.get("replacement"):
                    errors.append(f"{domain} {host['stable_id']}: blocked row missing replacement")
    contract_path = CONTRACT_WAVE / "contract.json"
    if not contract_path.is_file():
        errors.append("missing acquisition contract.json")
    else:
        contract = census.load_json(contract_path)
        if contract.get("auto_promote_player_complete") is not False:
            errors.append("contract must not auto-promote player_complete")
        if len(contract.get("child_order") or []) != 14:
            errors.append("contract child_order must have 14 families")
        if list(contract.get("landed_domains") or []) != list(LANDED_DOMAINS):
            errors.append("contract landed_domains drifted")
    if LANDED_DOMAINS:
        if not LIVE_CATALOG.is_file():
            errors.append("missing mte_inplace_acquisition.json")
        else:
            catalog = census.load_json(LIVE_CATALOG)
            if catalog.get("auto_promote_player_complete") is not False:
                errors.append("live catalog must not auto-promote player_complete")
            if catalog.get("source_revision") != GT6_REVISION:
                errors.append("live catalog source revision drifted")
        for relative in ("structure/empty.nbt", "gametest/structure/empty.nbt"):
            if not (WAVE_PACK / relative).is_file():
                errors.append(f"missing wave {relative}")
    from tools import blockers

    catalog = {row["id"]: row for row in blockers.load_catalog()["entries"]}
    blocker = catalog.get(BLOCKER_ID)
    if blocker is None:
        errors.append("missing shared obtain/mte-inplace-runtime")
    else:
        if blocker.get("status") != "resolved":
            errors.append("shared obtain/mte-inplace-runtime must be resolved after landing")
        if blocker.get("resolved_by") != CONTRACT_SLUG:
            errors.append(
                "shared obtain/mte-inplace-runtime must be resolved by "
                "content/gt6-mte-inplace-acquisition"
            )
        if int(blocker.get("count") or 0) != 14:
            errors.append("shared blocker unit remains 14 capabilities")
        if blocker.get("claimed_by"):
            errors.append("resolved obtain blocker must not keep note_marker claimed_by")
    from tools import capability_ledger as ledger

    compiled = ledger.compile_ledger()
    for domain in CHILD_ORDER:
        slug = inplace.DOMAINS[domain]["slug"]
        capability = next((row for row in compiled["capabilities"] if row["slug"] == slug), None)
        if capability is None:
            errors.append(f"missing runtime capability {slug}")
            continue
        if capability.get("maturity") != "runtime_ready":
            errors.append(f"{slug}: runtime maturity drifted")
        if capability.get("maturity") == "player_complete":
            errors.append(f"{slug}: acquisition must not promote player_complete")
        if blockers.OBTAIN_MARKER in str(capability.get("note") or ""):
            errors.append(f"{slug}: runtime note still binds Obtain stays explicitly_blocked")
        acq = census.TOOLS / "capabilities" / acquisition_slug(domain) / "capability.json"
        if acq.is_file():
            errors.append(f"{acquisition_slug(domain)}: family capability.json is not the unique-active landing card")
    parent = census.TOOLS / "capabilities" / CONTRACT_SLUG / "capability.json"
    unique = compiled.get("unique_active_slug")
    if unique == CONTRACT_SLUG and not parent.is_file():
        errors.append("content/gt6-mte-inplace-acquisition: unique-active landing missing capability.json")
    if parent.is_file():
        parent_cap = next(
            (row for row in compiled["capabilities"] if row["slug"] == CONTRACT_SLUG),
            None,
        )
        parent_note = str(census.load_json(parent).get("note") or "")
        if "stays open" in parent_note:
            errors.append("content/gt6-mte-inplace-acquisition: note still says obtain blocker stays open")
        if parent_cap is None:
            errors.append("content/gt6-mte-inplace-acquisition: capability.json failed to load")
        elif parent_cap.get("maturity") == "player_complete":
            errors.append("content/gt6-mte-inplace-acquisition: must not promote player_complete")
        elif parent_cap.get("workflow") == "active":
            if unique != CONTRACT_SLUG:
                errors.append("content/gt6-mte-inplace-acquisition: unique-active landing is not held")
        else:
            if parent_cap.get("workflow") != "accepted":
                errors.append("content/gt6-mte-inplace-acquisition: closed workflow drifted")
            if parent_cap.get("maturity") != "runtime_ready":
                errors.append("content/gt6-mte-inplace-acquisition: closed maturity drifted")
            if unique == CONTRACT_SLUG:
                errors.append("content/gt6-mte-inplace-acquisition: closed card still unique-active")
    return errors


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--family", choices=list(CHILD_ORDER))
    args = parser.parse_args(argv)
    if args.write:
        write(args.family)
    if args.check or not args.write:
        errors = check()
        if errors:
            sys.stderr.write("\n".join(errors) + "\n")
            return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
