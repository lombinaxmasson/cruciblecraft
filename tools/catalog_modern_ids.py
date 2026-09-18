#!/usr/bin/env python3
"""Authority table: numbered catalog (source_item, meta) → modern runtime id.

Fail-closed. No slug_m{meta} formula. Do not steal live host ids.
"""
from __future__ import annotations

import argparse
import ast
import json
import re
import sys
from functools import lru_cache
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import census_common as census
from tools import gt6_resolve
from tools import io_common as io
from tools import language_names
from tools import tool_head_prefix as thp

MAP_PATH = io.TOOLS / "catalog_modern_id_map.json"
DATA = ROOT / "src" / "main" / "resources" / "data" / "cruciblecraft"
ASSETS = ROOT / "src" / "main" / "resources" / "assets" / "cruciblecraft"
GATE_PATH = DATA / "material_registration_gate.json"
SMELTER_TAG = DATA / "tags" / "item" / "smelter_mte_items.json"
BATH_MTE_TAG = DATA / "tags" / "item" / "bath_mte_items.json"
DUMMY_REMAP_DIRS = (
    io.TOOLS / "waves" / "smelter" / "deferred-recycling",
    io.TOOLS / "waves" / "smelter" / "deferred-recycling-edge",
    io.TOOLS / "waves" / "recycling" / "smelter-mte-identity",
)
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
GT_ITEMS = ROOT / "gt6_code" / "gregtech6" / "src" / "main" / "java" / "gregtech" / "items"
GT_BLOCKS = ROOT / "gt6_code" / "gregtech6" / "src" / "main" / "java" / "gregtech" / "blocks"
R0_LEDGER = (
    io.TOOLS
    / "waves"
    / "portfolio"
    / "mte-identity-disposition-r0"
    / "disposition_ledger.json"
)

NUMBERED_RE = re.compile(
    r"^(?:gt_mte/mte_\d+|gt_multiitem/.+_m\d+|gt_block/.+_m\d+"
    r"|gt_stone/.+_m\d+|gt_object/.+_m\d+|gt_tool_head/.+)$"
)
NUMBERED_FORMULA_RE = re.compile(
    r"(?:gt_mte/mte_\d+|gt_multiitem/.+_m\d+|gt_block/.+_m\d+"
    r"|gt_stone/.+_m\d+|gt_object/.+_m\d+)"
)
GARBAGE_NAME_RE = re.compile(
    r"^(?:GT\s+(?:Multitileentity|bale|asphalt|方块)|GT方块)",
    re.I,
)
GARBAGE_META_TAIL_RE = re.compile(r"\bm\d+$", re.I)
PATH_RE = re.compile(r"^[a-z][a-z0-9_]*(?:/[a-z0-9_]+)+$|^[a-z][a-z0-9_]+$")
ADD_LITERAL = re.compile(
    r'aRegistry\.add\(\s*"([^"]+)"\s*,\s*"[^"]+"\s*,\s*(\d+)\s*,'
)
METALSET_CALL = re.compile(
    r"metalset\(\s*aRegistry,\s*aMetal,\s*aUtilMetal,\s*aMachine,\s*aWooden,\s*"
    r"(MT\.[A-Za-z0-9_]+|ANY\.[A-Za-z0-9_]+)\s*,\s*(\d+)\s*,"
)
METALSET_ADD = re.compile(
    r'aRegistry\.add\((.+?),\s*"[^"]+"\s*,\s*((?:\d+\s*\+\s*)?aID)\s*,'
)
ADD_ITEM = re.compile(r'addItem\(\s*(\d+)\s*,\s*"([^"]+)"(?:\s*,\s*"([^"]*)")?')
BUCKET_MATERIALS = re.compile(
    r"OreDictMaterial\[\] tBucketMaterials = new OreDictMaterial\[\] \{([^}]+)\}"
)
LH_ADD = re.compile(
    r'LH\.add\(\s*getUnlocalizedName\(\)\s*\+\s*"\.(\d+)"\s*,\s*"([^"]+)"\s*\)'
)
UNLOC = re.compile(r'"(gt\.(?:block|stone)[^"]+)"')
SAFE_EXPR = re.compile(r"^[0-9A-Za-z+]+$")
SET_LOCAL = re.compile(
    r"\b([A-Za-z][A-Za-z0-9_]*)\s*=[^\n]*?"
    r"\.setLocal\(\s*\"([^\"]+)\"\s*\)"
)

DYE_COLORS = (
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
STONE_VARIANT = {
    0: "stone",
    1: "cobble",
    2: "mossy_cobble",
    3: "bricks",
    4: "cracked_bricks",
    5: "mossy_bricks",
    6: "chiseled",
    7: "smooth",
    8: "reinforced_bricks",
    9: "redstoned_bricks",
    10: "tiles",
    11: "small_tiles",
    12: "small_bricks",
    13: "windmill_tiles_a",
    14: "windmill_tiles_b",
    15: "square_bricks",
}
SLAB_FACE = ("down", "up", "north", "south", "west", "east")
BALE_CROP = ("rye", "oats", "barley", "rice")
BALE_GRASS = ("grass", "dry_grass", "moldy_grass", "rotten_grass")
BALE_AXIS = ("", "axis_x", "axis_z", "axis_y")
TIER_WORDS = {
    "ulv",
    "lv",
    "mv",
    "hv",
    "ev",
    "iv",
    "luv",
    "zpm",
    "uv",
    "uhv",
    "uev",
    "uiv",
    "umv",
    "uxv",
    "max",
    "slv",
}
TAG_FAMILY_PREFIX = {
    "Electric Wires": "electric_wire",
    "Item Pipes": "item_pipe_tile",
    "Fluid Pipes": "fluid_pipe_tile",
    "Redstone Wires": "redstone_wire",
    "Chests": "chest",
    "Safes": "safe",
    "Hoppers": "hopper",
    "Panels": "panel",
    "Ropes": "rope",
    "Sensors": "sensor",
}
FAMILY_PREFIX = {
    "connector": "connector",
    "furniture_storage": "furniture",
    "processing_machine": "processing",
    "energy_converter": "energy",
    "decorative": "decorative",
    "drive": "drive",
    "redstone_wire": "redstone_wire",
    "hopper": "hopper",
    "fluid_attachment": "fluid_attachment",
    "crucible_foundry": "foundry",
    "extender": "extender",
    "multiblock": "multiblock",
    "sensor": "sensor",
    "computer": "computer",
    "logistics": "logistics",
    "reactor": "reactor",
    "misc_tool": "misc_tool",
    "untyped": "untyped",
}
MULTIITEM_FAMILY = {
    "gregtech:gt.multiitem.books": "book",
    "gregtech:gt.multiitem.bottles": "bottle",
    "gregtech:gt.multiitem.bumblebee": "bumblebee",
    "gregtech:gt.multiitem.cans": "can",
    "gregtech:gt.multiitem.food": "food",
    "gregtech:gt.multiitem.randomtools": "tool",
    "gregtech:gt.multiitem.technological": "part",
}
CATALOG_FILES = (
    DATA / "smelter_mte_identity_catalog.json",
    DATA / "bath_mte_identity_catalog.json",
    DATA / "semantic_object_catalog.json",
    DATA / "bath_identity_catalog.json",
    DATA / "gt_block_object_catalog.json",
    DATA / "bath_remainder_identity_catalog.json",
    DATA / "gt_building_block_catalog.json",
    DATA / "gt_stone_catalog.json",
    DATA / "slicer_operands.json",
)
SKIP_REPLACE_PARTS = (
    "gt6_code",
    "gt6_referencable_port_code",
    "gt6u_code",
    "gt6_dump",
    "build",
    ".git",
    "card-plans/closed",
    "card-plans/prep",
)
SKIP_REPLACE_FILES = {
    "catalog_modern_id_map.json",
    "catalog_modern_ids.py",
    "test_electric_wire_cable_mte_fold.py",
    "test_catalog_modern_ids.py",
    "ordinary_source.py",
}
_MAP_CACHE: dict[str, Any] | None = None
_LAST_REPAIRS: list[tuple[str, str]] = []
_FAMILY_ROOTS = frozenset(
    {
        *TAG_FAMILY_PREFIX.values(),
        *FAMILY_PREFIX.values(),
        *MULTIITEM_FAMILY.values(),
    }
)


def is_numbered_path(path: str) -> bool:
    return bool(NUMBERED_RE.fullmatch(str(path)))


def strip_ns(runtime: str) -> str:
    text = str(runtime)
    if ":" in text:
        return text.split(":", 1)[1]
    return text


def _eval_sum(expr: str, **names: int) -> int:
    compact = expr.replace(" ", "")
    if not SAFE_EXPR.fullmatch(compact):
        raise ValueError(f"unsafe id expression {expr!r}")
    for name, value in names.items():
        compact = compact.replace(name, str(value))
    if not re.fullmatch(r"\d+(?:\+\d+)*", compact):
        raise ValueError(f"unresolved id expression {expr!r}")
    return sum(int(part) for part in compact.split("+"))


def slug_token(text: str) -> str:
    lowered = str(text).strip().lower().replace("c-foam", "cfoam")
    lowered = lowered.replace("o-meter", "ometer").replace("o'lantern", "o_lantern")
    parts = re.sub(r"[^a-z0-9]+", "_", lowered).strip("_")
    return parts


def is_garbage_name(name: str) -> bool:
    text = str(name or "").strip()
    if not text:
        return True
    if GARBAGE_NAME_RE.search(text):
        return True
    if GARBAGE_META_TAIL_RE.search(text) and text.lower().startswith("gt"):
        return True
    return False


def _mt_set_locals() -> dict[str, str]:
    """MT.Os.setLocal("Osmium") display names used by MTE getLocal()."""
    if not gt6_resolve.MT_JAVA.is_file():
        return {}
    text = gt6_resolve.MT_JAVA.read_text(encoding="utf-8", errors="replace")
    return {match.group(1): match.group(2) for match in SET_LOCAL.finditer(text)}


def _material_index() -> dict[str, str]:
    materials: dict[str, str] = {}
    for key, row in gt6_resolve.live_materials().items():
        cc = str(row.get("cc_material") or "")
        if not cc:
            continue
        materials[slug_token(str(key))] = cc
        materials[cc] = cc
    for field, source in gt6_resolve.mt_fields().items():
        cc = slug_token(source) if source else slug_token(field)
        resolved = gt6_resolve.resolve_material(field)
        if resolved.get("cc_material"):
            cc = str(resolved["cc_material"])
        materials[slug_token(field)] = cc
        if source:
            materials[slug_token(source)] = cc
    # OreDict aliases such as Germanium's "Osmium" (FakeOsmium = Ge) must not
    # beat MT.setLocal display names. MTE english uses getLocal(), so Os pipes
    # are "Osmium Item Pipe" and belong to osmium_elemental.
    for field, local in _mt_set_locals().items():
        resolved = gt6_resolve.resolve_material(field)
        cc = str(resolved.get("cc_material") or "")
        if cc:
            materials[slug_token(local)] = cc
    return materials


def _material_tokens(cc: str) -> set[str]:
    return {token for token in [cc, *cc.split("_")] if token}


def _borrowed_material_segments(
    path: str, expected_cc: str, cc_ids: set[str]
) -> list[str]:
    allowed = _material_tokens(expected_cc)
    return [
        segment
        for segment in path.split("/")
        if segment in cc_ids and segment not in allowed
    ]


def _english_material(english: str, materials: dict[str, str]) -> str:
    words = [part for part in slug_token(english).split("_") if part]
    if not words:
        return ""
    for width in range(min(3, len(words)), 0, -1):
        key = "_".join(words[:width])
        if key in materials:
            return materials[key]
    for width in range(min(3, len(words)), 0, -1):
        key = "_".join(words[-width:])
        if key in materials and width < len(words):
            return materials[key]
    return ""


def catalog_item_paths() -> set[str]:
    """Holdable paths actually registered as catalog `item` rows."""
    paths: set[str] = set()
    for path in CATALOG_FILES:
        if not path.is_file():
            continue
        document = census.load_json(path)
        if path.name == "gt_stone_catalog.json":
            for identity in document.get("identities") or []:
                for variant in identity.get("variants") or []:
                    if str(variant.get("registry_kind") or "item") == "item":
                        paths.add(
                            strip_ns(str(variant.get("registry_path") or ""))
                        )
            continue
        key = "operands" if "operands" in document else "identities"
        for identity in document.get(key) or []:
            if str(identity.get("registry_kind") or "item") == "item":
                paths.add(
                    strip_ns(
                        str(
                            identity.get("registry_path")
                            or identity.get("runtime_id")
                            or ""
                        )
                    )
                )
    return {path for path in paths if path}


def folded_existing_item_paths() -> set[str]:
    """Catalog rows already folded onto a live host. Not dummy theft."""
    paths: set[str] = set()
    for path in CATALOG_FILES:
        if not path.is_file():
            continue
        document = census.load_json(path)
        key = "operands" if "operands" in document else "identities"
        identities = list(document.get(key) or [])
        if path.name == "gt_stone_catalog.json":
            identities = []
            for identity in document.get("identities") or []:
                identities.extend(identity.get("variants") or [])
        for identity in identities:
            if str(identity.get("registry_kind") or "") != "existing_item":
                continue
            target = strip_ns(
                str(
                    identity.get("registry_path")
                    or identity.get("runtime_id")
                    or ""
                )
            )
            if target:
                paths.add(target)
    return paths


def registered_holdable_paths() -> set[str]:
    return live_host_paths() | catalog_item_paths()


def _render_concat(expr: str, local: str) -> str | None:
    parts = re.split(r"\s*\+\s*", expr.strip())
    out: list[str] = []
    for part in parts:
        token = part.strip()
        if token in {"aMat.getLocal()", "aMat.getLocal()"}:
            out.append(local)
            continue
        if (token.startswith('"') and token.endswith('"')) or (
            token.startswith("'") and token.endswith("'")
        ):
            try:
                out.append(str(ast.literal_eval(token)))
            except (SyntaxError, ValueError):
                return None
            continue
        return None
    return "".join(out).strip() or None


def loader_mte_names() -> dict[int, str]:
    names: dict[int, str] = {}
    if not LOADER.is_file():
        return names
    text = LOADER.read_text(encoding="utf-8")
    for name, meta in ADD_LITERAL.findall(text):
        names[int(meta)] = name
    metalset_fn = re.search(r"private static void metalset\((.*?)\n\t\}", text, re.S)
    templates: list[tuple[str, str]] = []
    if metalset_fn:
        for match in METALSET_ADD.finditer(metalset_fn.group(1)):
            templates.append((match.group(1), match.group(2).replace(" ", "")))
    for mat_token, aid in METALSET_CALL.findall(text):
        field = mat_token.split(".", 1)[-1]
        resolved = gt6_resolve.resolve_material(field)
        local = str(resolved.get("source_name") or resolved.get("cc_material") or field)
        if resolved.get("status") != "ok":
            source = gt6_resolve.mt_fields().get(field)
            local = str(source or field)
        aid_i = int(aid)
        for expr, id_expr in templates:
            rendered = _render_concat(expr, local)
            if not rendered:
                continue
            try:
                meta = _eval_sum(id_expr, aID=aid_i)
            except ValueError:
                continue
            names.setdefault(meta, rendered)
    return names


def multiitem_java_names() -> dict[tuple[str, int], str]:
    names: dict[tuple[str, int], str] = {}
    if not GT_ITEMS.is_dir():
        return names
    family_files = {
        "MultiItemBooks.java": "gregtech:gt.multiitem.books",
        "MultiItemBottles.java": "gregtech:gt.multiitem.bottles",
        "MultiItemBumblebee.java": "gregtech:gt.multiitem.bumblebee",
        "MultiItemCans.java": "gregtech:gt.multiitem.cans",
        "MultiItemFood.java": "gregtech:gt.multiitem.food",
        "MultiItemRandomTools.java": "gregtech:gt.multiitem.randomtools",
        "MultiItemTechnological.java": "gregtech:gt.multiitem.technological",
    }
    for filename, source_item in family_files.items():
        path = GT_ITEMS / filename
        if not path.is_file():
            continue
        text = path.read_text(encoding="utf-8", errors="replace")
        for meta, name, tooltip in ADD_ITEM.findall(text):
            label = name.strip()
            extra = (tooltip or "").strip()
            if extra and extra.lower() not in {label.lower(), "empty"}:
                extra = re.sub(r"\s*\(.*\)\s*$", "", extra).strip()
                if extra:
                    label = f"{label} {extra}"
            names[(source_item, int(meta))] = label
        bucket_mats = BUCKET_MATERIALS.search(text)
        if bucket_mats:
            tokens = [part.strip() for part in bucket_mats.group(1).split(",") if part.strip()]
            for index, token in enumerate(tokens):
                field = token.split(".", 1)[-1]
                resolved = gt6_resolve.resolve_material(field)
                local = str(
                    resolved.get("cc_material")
                    or resolved.get("source_name")
                    or field
                )
                names[(source_item, 2000 + index * 100)] = (
                    f"{language_names.format_english_id(local)} Wooden Bucket"
                )
    return names


def block_lh_names() -> dict[tuple[str, int], str]:
    names: dict[tuple[str, int], str] = {}
    roots = [GT_BLOCKS]
    gregapi_blocks = (
        ROOT / "gt6_code" / "gregtech6" / "src" / "main" / "java" / "gregapi" / "block"
    )
    if gregapi_blocks.is_dir():
        roots.append(gregapi_blocks)
    for root in roots:
        for path in root.rglob("*.java"):
            text = path.read_text(encoding="utf-8", errors="replace")
            unlocs = UNLOC.findall(text)
            adds = {int(meta): name for meta, name in LH_ADD.findall(text)}
            if not unlocs or not adds:
                continue
            for unloc in unlocs:
                source = f"gregtech:{unloc}"
                for meta, name in adds.items():
                    names.setdefault((source, meta), name)
    return names


@lru_cache(maxsize=1)
def r0_by_meta() -> dict[int, dict[str, Any]]:
    if not R0_LEDGER.is_file():
        return {}
    document = census.load_json(R0_LEDGER)
    return {int(row["meta"]): row for row in document.get("identities") or []}


def _iter_identity_rows() -> list[dict[str, Any]]:
    rows: list[dict[str, Any]] = []
    for path in CATALOG_FILES:
        if not path.is_file():
            continue
        document = census.load_json(path)
        identities = document.get("identities") or document.get("operands") or []
        catalog = path.name
        if catalog == "gt_stone_catalog.json":
            for identity in identities:
                source_item = str(identity.get("source_item") or "")
                english = str(identity.get("english_name") or "")
                stone = str(identity.get("stone") or "")
                kind = str(identity.get("kind") or "full")
                slab = identity.get("slab_variant")
                for variant in identity.get("variants") or []:
                    path_id = strip_ns(str(variant.get("registry_path") or ""))
                    if not is_numbered_path(path_id):
                        continue
                    rows.append(
                        {
                            "catalog": catalog,
                            "source_item": source_item,
                            "meta": int(variant["meta"]),
                            "old_registry_path": path_id,
                            "english_name": english,
                            "kind": "stone",
                            "stone": stone,
                            "slab_kind": kind,
                            "slab_variant": slab,
                        }
                    )
            continue
        for identity in identities:
            path_id = strip_ns(
                str(identity.get("registry_path") or identity.get("runtime_id") or "")
            )
            if not is_numbered_path(path_id):
                continue
            meta = identity.get("meta")
            if not isinstance(meta, int):
                continue
            rows.append(
                {
                    "catalog": catalog,
                    "source_item": str(identity.get("source_item") or ""),
                    "meta": int(meta),
                    "old_registry_path": path_id,
                    "english_name": str(identity.get("english_name") or ""),
                    "kind": str(identity.get("kind") or identity.get("behavior") or ""),
                    "family": str(identity.get("family") or ""),
                }
            )
    extras = (
        (
            "gregtech:gt.multiitem.food",
            280,
            "gt_multiitem/multiitem_food_m280",
            "Cinnamon Bark",
            "multiitem",
        ),
    )
    seen = {(row["source_item"], row["meta"]) for row in rows}
    mapped_keys: set[tuple[str, int]] = set()
    if MAP_PATH.is_file():
        mapped_keys = {
            (str(row["source_item"]), int(row["meta"]))
            for row in census.load_json(MAP_PATH).get("rows") or []
        }
    for source_item, meta, path_id, english, kind in extras:
        if (source_item, meta) in seen or (source_item, meta) in mapped_keys:
            continue
        if is_numbered_path(path_id):
            rows.append(
                {
                    "catalog": "java_hardcoded",
                    "source_item": source_item,
                    "meta": meta,
                    "old_registry_path": path_id,
                    "english_name": english,
                    "kind": kind,
                    "family": "",
                }
            )
    return rows


def live_host_paths() -> set[str]:
    """Material forms and hardcoded hosts that Java actually registers.

    Catalog dummies stay out of this set. After the numbered rewrite,
    registry_identity live_entries include those dummies, so they cannot
    be used as the occupied set.
    """
    occupied: set[str] = {
        "slicer",
        "tin/wire",
        "tin/item_pipe",
        "steel_dust_funnel",
        "programmed_circuit",
        "cover_blank_cover",
    }
    if GATE_PATH.is_file():
        gate = census.load_json(GATE_PATH)
        for section in ("materials", "pipe_forms", "electrical_wire_forms"):
            for material, forms in (gate.get(section) or {}).items():
                for form in forms or []:
                    if material and form:
                        occupied.add(f"{material}/{form}")
    seen_materials: set[str] = set()
    for row in gt6_resolve.live_materials().values():
        cc = str(row.get("cc_material") or "")
        if not cc or cc in seen_materials:
            continue
        seen_materials.add(cc)
        for form in row.get("include_prefixes") or []:
            occupied.add(f"{cc}/{form}")
        for flag in row.get("generation_flags") or []:
            token = str(flag)
            prefix = "cruciblecraft:generates_"
            if token.startswith(prefix):
                occupied.add(f"{cc}/{token.removeprefix(prefix)}")
        for item in (row.get("form_items") or {}).values():
            occupied.add(strip_ns(str(item)))
    occupied.update(_bundled_machine_host_paths())
    occupied.update(_mte_inplace_host_paths())
    return {path for path in occupied if path}


def _mte_inplace_host_paths() -> set[str]:
    catalog = DATA / "mte_inplace_catalog.json"
    if not catalog.is_file():
        return set()
    document = census.load_json(catalog)
    occupied: set[str] = set()
    for row in document.get("identities") or []:
        occupied.add(strip_ns(str(row.get("registry_path") or "")))
    return {path for path in occupied if path}


def _bundled_machine_host_paths() -> set[str]:
    """BlockItems/items already registered from bundled machine catalogs."""
    occupied: set[str] = set()
    converter = DATA / "energy_converter_tiers.json"
    if converter.is_file():
        document = census.load_json(converter)
        for row in document.get("tiers") or []:
            occupied.add(strip_ns(str(row.get("id") or "")))
    hoppers = DATA / "hopper_variants.json"
    if hoppers.is_file():
        document = census.load_json(hoppers)
        for row in document.get("variants") or []:
            material = strip_ns(str(row.get("material") or ""))
            if material:
                occupied.add(f"{material}_hopper")
                occupied.add(f"{material}_queue_hopper")
    machines = DATA / "machine_tiers.json"
    if machines.is_file():
        document = census.load_json(machines)
        for row in document.get("variants") or []:
            occupied.add(strip_ns(str(row.get("id") or "")))
    rods = DATA / "nuclear_reactor_rods.json"
    if rods.is_file():
        document = census.load_json(rods)
        for row in document.get("rods") or []:
            occupied.add(strip_ns(str(row.get("id") or "")))
    return {path for path in occupied if path}


def occupied_live_paths(numbered: set[str]) -> set[str]:
    del numbered
    return live_host_paths()


def resolve_english(
    row: dict[str, Any],
    *,
    loader_names: dict[int, str],
    multi_names: dict[tuple[str, int], str],
    block_names: dict[tuple[str, int], str],
) -> str:
    source = str(row["source_item"])
    meta = int(row["meta"])
    if source == "gregtech:gt.multitileentity" and meta in loader_names:
        name = loader_names[meta]
        if not is_garbage_name(name):
            return name
    key = (source, meta)
    if key in multi_names and not is_garbage_name(multi_names[key]):
        return multi_names[key]
    if key in block_names and not is_garbage_name(block_names[key]):
        return block_names[key]
    current = str(row.get("english_name") or "").strip()
    if not is_garbage_name(current):
        return current
    if source.startswith("gregtech:gt.stone."):
        stone = str(row.get("stone") or slug_token(source.split("gt.stone.", 1)[-1]))
        variant = STONE_VARIANT.get(meta)
        if variant:
            slab_kind = str(row.get("slab_kind") or "full")
            if slab_kind == "slab":
                face = SLAB_FACE[int(row.get("slab_variant") or 0) % 6]
                return (
                    f"{language_names.format_english_id(stone)} "
                    f"{language_names.format_english_id(variant)} Slab {face}"
                )
            return (
                f"{language_names.format_english_id(stone)} "
                f"{language_names.format_english_id(variant)}"
            )
    if source == "gregtech:gt.block.bale.crop":
        crop = BALE_CROP[meta % 4]
        axis = BALE_AXIS[meta // 4]
        return " ".join(
            part
            for part in (
                language_names.format_english_id(crop),
                "Bale",
                axis.replace("_", " "),
            )
            if part
        )
    if source == "gregtech:gt.block.bale.grass":
        grass = BALE_GRASS[meta % 4]
        axis = BALE_AXIS[meta // 4]
        return " ".join(
            part
            for part in (
                language_names.format_english_id(grass),
                "Bale",
                axis.replace("_", " "),
            )
            if part
        )
    if "asphalt" in source or "cfoam" in source:
        color = DYE_COLORS[meta % 16]
        kind = "cfoam" if "cfoam" in source else "asphalt"
        fresh = "fresh_" if "fresh" in source else ""
        return language_names.format_english_id(f"{fresh}{kind}_{color}")
    if source.startswith("gregtech:gt.block."):
        tail = source.removeprefix("gregtech:gt.block.")
        slab = ""
        if ".slab." in tail:
            tail, slab_s = tail.rsplit(".slab.", 1)
            slab = f" {SLAB_FACE[int(slab_s) % 6].title()} Slab"
        parts = [part for part in tail.split(".") if part]
        if len(parts) >= 2:
            return (
                f"{language_names.format_english_id(parts[-1])} "
                f"{language_names.format_english_id(parts[0])}{slab}"
            ).strip()
        return f"{language_names.format_english_id(tail.replace('.', '_'))}{slab}".strip()
    if source.startswith("gregtech:gt.multiitem."):
        family = language_names.format_english_id(source.rsplit(".", 1)[-1])
        return f"{family} {meta}"
    if source == "gregtech:gt.multitileentity":
        r0 = r0_by_meta().get(meta) or {}
        tag = str(r0.get("gt6_class_or_tag") or "").replace(" / ", " ").replace("MultiTileEntity", "")
        if tag.strip():
            return tag.strip()
    tail = source.split(":", 1)[-1].replace(".", "_").replace(" ", "_")
    return language_names.format_english_id(tail)


def _panel_path(meta: int) -> str | None:
    if 32452 <= meta <= 32467:
        return f"panel/concrete_{DYE_COLORS[meta - 32452]}"
    if 32468 <= meta <= 32483:
        return f"panel/cfoam_{DYE_COLORS[meta - 32468]}"
    if 32484 <= meta <= 32499:
        return f"panel/asphalt_{DYE_COLORS[meta - 32484]}"
    if 32500 <= meta <= 32599:
        return f"panel/wood_{meta - 32500}"
    if 32352 <= meta <= 32451:
        return f"panel/wood_{meta - 32352 + 100}"
    if 32252 <= meta <= 32351:
        return f"panel/wood_{meta - 32252 + 200}"
    return None


def _stone_path(row: dict[str, Any]) -> str:
    stone = slug_token(str(row.get("stone") or "stone"))
    variant = STONE_VARIANT.get(int(row["meta"]), f"variant_{int(row['meta'])}")
    if variant.startswith("variant_"):
        variant = STONE_VARIANT.get(int(row["meta"]) % 16, "stone")
    slab_kind = str(row.get("slab_kind") or "full")
    if slab_kind == "slab":
        face = SLAB_FACE[int(row.get("slab_variant") or 0) % 6]
        return f"{stone}/{variant}/slab_{face}"
    return f"{stone}/{variant}"


def _name_candidates(
    english: str,
    row: dict[str, Any],
    materials: dict[str, str],
    r0: dict[int, dict[str, Any]],
) -> list[str]:
    source = str(row["source_item"])
    meta = int(row["meta"])
    if source.startswith("gregtech:gt.stone."):
        return [_stone_path(row)]
    if source.startswith("gregtech:gt.block."):
        tail = source.removeprefix("gregtech:gt.block.")
        slab_face = None
        if ".slab." in tail:
            tail, slab_s = tail.rsplit(".slab.", 1)
            slab_face = SLAB_FACE[int(slab_s) % 6]
        if tail.startswith("bale.crop") or source.endswith("bale.crop"):
            crop = BALE_CROP[meta % 4]
            axis = BALE_AXIS[meta // 4]
            path = f"bale/{crop}" if not axis else f"bale/{crop}_{axis}"
            return [path]
        if tail.startswith("bale.grass") or source.endswith("bale.grass"):
            grass = BALE_GRASS[meta % 4]
            axis = BALE_AXIS[meta // 4]
            path = f"bale/{grass}" if not axis else f"bale/{grass}_{axis}"
            return [path]
        kind = slug_token(tail.replace(".", "_"))
        cores: list[str] = []
        if "asphalt" in source or "cfoam" in source:
            color = DYE_COLORS[meta % 16]
            prefix = "cfoam" if "cfoam" in source else "asphalt"
            if "fresh" in source:
                prefix = f"{prefix}_fresh"
            cores = [f"{prefix}/{color}"]
        else:
            english_slug = slug_token(english)
            color = DYE_COLORS[meta % 16]
            if english_slug:
                cores.append(f"{kind}/{english_slug}")
            cores.append(f"{kind}/{color}")
            cores.append(kind)
        if slab_face:
            expanded: list[str] = []
            for core in cores:
                expanded.append(f"{core}/slab_{slab_face}")
            cores = expanded
        return cores
    panel = _panel_path(meta) if source == "gregtech:gt.multitileentity" else None
    if panel:
        return [panel]
    family_row = r0.get(meta) if source == "gregtech:gt.multitileentity" else {}
    tag = ""
    family = str(row.get("family") or "")
    if family_row:
        family = str(family_row.get("family") or family)
        tag = str(family_row.get("gt6_class_or_tag") or "").split(" / ", 1)[-1]
    family_prefix = TAG_FAMILY_PREFIX.get(tag) or FAMILY_PREFIX.get(family)
    mi_family = MULTIITEM_FAMILY.get(source)
    words = [part for part in slug_token(english).split("_") if part]
    paren = re.search(r"\(([^)]+)\)\s*$", english)
    variant = slug_token(paren.group(1)) if paren else ""
    if variant in TIER_WORDS or (variant and re.fullmatch(r"[a-z0-9]+", variant)):
        base_words = [
            part
            for part in slug_token(re.sub(r"\([^)]*\)\s*$", "", english)).split("_")
            if part
        ]
    else:
        base_words = words
        variant = ""
    material = ""
    rest: list[str] = []
    if base_words:
        for width in range(min(3, len(base_words)), 0, -1):
            key = "_".join(base_words[:width])
            if key in materials:
                material = materials[key]
                rest = base_words[width:]
                break
        if not material:
            for width in range(min(3, len(base_words)), 0, -1):
                key = "_".join(base_words[-width:])
                if key in materials and width < len(base_words):
                    material = materials[key]
                    rest = base_words[:-width]
                    break
        if not rest:
            rest = [part for part in base_words if part != slug_token(material)]
    kind = "_".join(rest) if rest else (mi_family or family_prefix or "item")
    if variant:
        kind = f"{kind}_{variant}" if kind not in {variant, ""} else variant
    candidates: list[str] = []

    def add(path: str) -> None:
        path = path.strip("/")
        if path and path not in candidates and PATH_RE.fullmatch(path):
            if not re.search(r"_m\d+$", path):
                candidates.append(path)

    if material and kind and kind != material:
        add(f"{material}/{kind}")
        add(f"{kind}/{material}")
    if mi_family and kind:
        add(f"{mi_family}/{slug_token(english) or kind}")
        if variant:
            add(f"{mi_family}/{kind}")
    if family_prefix and material:
        add(f"{family_prefix}/{material}")
    if family_prefix and kind:
        add(f"{family_prefix}/{kind}")
    if family_prefix and material and kind:
        add(f"{family_prefix}/{material}_{kind}")
    whole = slug_token(english)
    if whole:
        add(whole.replace("_", "/", 1) if "_" in whole else whole)
        add(whole)
    if family_prefix and whole:
        add(f"{family_prefix}/{whole}")
    if family_row:
        class_name = str(family_row.get("gt6_class_or_tag") or "").split(" / ", 1)[0]
        class_slug = slug_token(class_name.replace("MultiTileEntity", ""))
        if class_slug:
            if whole:
                add(f"{class_slug}/{whole}")
            if material and kind:
                add(f"{class_slug}/{material}_{kind}")
            add(f"{class_slug}/{slug_token(english) or kind}")
    return candidates


def _order_candidates(candidates: list[str], *, prefer_family: bool) -> list[str]:
    if not prefer_family:
        return candidates
    preferred = [path for path in candidates if path.split("/", 1)[0] in _FAMILY_ROOTS]
    rest = [path for path in candidates if path not in preferred]
    return preferred + rest


def _choose_registry_path(
    row: dict[str, Any],
    english: str,
    materials: dict[str, str],
    r0: dict[int, dict[str, Any]],
    taken: set[str],
    *,
    prefer_family: bool = False,
) -> tuple[str, str, list[str]]:
    candidates = _name_candidates(english, row, materials, r0)
    for candidate in _order_candidates(candidates, prefer_family=prefer_family):
        if candidate not in taken:
            reason = ""
            if candidate != (candidates[0] if candidates else ""):
                reason = f"collision avoided {candidates[0]}"
            return candidate, reason, candidates
    source_slug = slug_token(
        str(row["source_item"]).removeprefix("gregtech:").replace(".", "_")
    )
    english_slug = slug_token(english)
    meta = int(row["meta"])
    extras = [
        f"{source_slug}/{english_slug}",
        f"{source_slug}/{english_slug}/{STONE_VARIANT.get(meta, DYE_COLORS[meta % 16])}",
    ]
    family_row = (
        r0.get(meta) if str(row["source_item"]).endswith("multitileentity") else {}
    )
    if family_row:
        class_name = str(family_row.get("gt6_class_or_tag") or "").split(" / ", 1)[0]
        class_slug = slug_token(class_name.replace("MultiTileEntity", ""))
        extras.append(f"{class_slug}/{english_slug}")
    for index in range(26):
        letter = chr(ord("a") + index)
        extras.append(f"{source_slug}/{english_slug}/{letter}")
        extras.append(f"{english_slug}/{letter}")
    for candidate in extras:
        if (
            candidate
            and candidate not in taken
            and PATH_RE.fullmatch(candidate)
            and not re.search(r"_m\d+$", candidate)
        ):
            return (
                candidate,
                f"collision avoided {candidates[0] if candidates else 'empty'}",
                candidates,
            )
    return "", "", candidates


def repair_live_host_collisions(document: dict[str, Any]) -> list[tuple[str, str]]:
    live = live_host_paths()
    folded = folded_existing_item_paths()
    rows = list(document.get("rows") or [])
    taken = {str(row["registry_path"]) for row in rows} | set(live)
    materials = _material_index()
    r0 = r0_by_meta()
    remaps: list[tuple[str, str]] = []
    for row in rows:
        current = str(row["registry_path"])
        if current not in live or current in folded:
            continue
        taken.discard(current)
        chosen, reason, candidates = _choose_registry_path(
            row,
            str(row.get("english_name") or ""),
            materials,
            r0,
            taken,
            prefer_family=True,
        )
        if not chosen:
            raise ValueError(
                "no free semantic id for live-host collision "
                f"{current} {row.get('source_item')}@{row.get('meta')} "
                f"candidates={candidates}"
            )
        remaps.append((current, chosen))
        row["registry_path"] = chosen
        row["runtime_id"] = f"cruciblecraft:{chosen}"
        row["collision_reason"] = reason or f"collision avoided live host {current}"
        taken.add(chosen)
    document["collision_count"] = sum(
        1 for row in rows if row.get("collision_reason")
    )
    return remaps


def repair_wrong_material_paths(document: dict[str, Any]) -> list[tuple[str, str]]:
    """Reassign rows whose english material borrowed another CC material path."""
    materials = _material_index()
    cc_ids = {cc for cc in materials.values() if cc}
    live = live_host_paths()
    rows = list(document.get("rows") or [])
    taken = {str(row["registry_path"]) for row in rows} | set(live)
    r0 = r0_by_meta()
    remaps: list[tuple[str, str]] = []
    for row in rows:
        if str(row.get("source_item") or "") != "gregtech:gt.multitileentity":
            continue
        expected = _english_material(str(row.get("english_name") or ""), materials)
        if not expected:
            continue
        current = str(row["registry_path"])
        borrowed = _borrowed_material_segments(current, expected, cc_ids)
        if not borrowed:
            continue
        taken.discard(current)
        chosen, reason, candidates = _choose_registry_path(
            row,
            str(row.get("english_name") or ""),
            materials,
            r0,
            taken,
            prefer_family=True,
        )
        if not chosen:
            taken.add(current)
            raise ValueError(
                "no free semantic id for material repair "
                f"{current} {row.get('source_item')}@{row.get('meta')} "
                f"expected={expected} borrowed={borrowed} candidates={candidates}"
            )
        remaps.append((current, chosen))
        row["registry_path"] = chosen
        row["runtime_id"] = f"cruciblecraft:{chosen}"
        row["collision_reason"] = (
            reason or f"material {expected} replaced {borrowed[0]}"
        )
        taken.add(chosen)
    document["collision_count"] = sum(
        1 for row in rows if row.get("collision_reason")
    )
    return remaps


def assign_paths(rows: list[dict[str, Any]]) -> dict[str, Any]:
    numbered = {row["old_registry_path"] for row in rows}
    reserved = occupied_live_paths(numbered)
    taken: set[str] = set(reserved)
    loader_names = loader_mte_names()
    multi_names = multiitem_java_names()
    block_names = block_lh_names()
    materials = _material_index()
    r0 = r0_by_meta()
    unique: dict[tuple[str, int], dict[str, Any]] = {}
    for row in rows:
        key = (row["source_item"], int(row["meta"]))
        unique.setdefault(key, row)
    assigned: list[dict[str, Any]] = []
    blocked: list[dict[str, Any]] = []
    for key in sorted(unique, key=lambda item: (item[0], item[1])):
        row = unique[key]
        english = resolve_english(
            row,
            loader_names=loader_names,
            multi_names=multi_names,
            block_names=block_names,
        )
        if not english:
            blocked.append(
                {
                    "source_item": row["source_item"],
                    "meta": row["meta"],
                    "old_registry_path": row["old_registry_path"],
                    "reason": "unnamed",
                }
            )
            continue
        chosen, reason, candidates = _choose_registry_path(
            row, english, materials, r0, taken
        )
        if not chosen:
            blocked.append(
                {
                    "source_item": row["source_item"],
                    "meta": row["meta"],
                    "old_registry_path": row["old_registry_path"],
                    "english_name": english,
                    "reason": "no_free_semantic_id",
                    "candidates": candidates,
                }
            )
            continue
        taken.add(chosen)
        assigned.append(
            {
                "source_item": row["source_item"],
                "meta": row["meta"],
                "old_registry_path": row["old_registry_path"],
                "registry_path": chosen,
                "runtime_id": f"cruciblecraft:{chosen}",
                "english_name": english,
                "collision_reason": reason,
                "catalog": row.get("catalog"),
            }
        )
    if blocked:
        raise ValueError(
            "catalog modern id mapping blocked "
            f"{len(blocked)} rows; first={blocked[0]}"
        )
    by_new = [row["registry_path"] for row in assigned]
    if len(by_new) != len(set(by_new)):
        raise ValueError("duplicate modern registry_path in authority table")
    return {
        "generated_by": "python tools/catalog_modern_ids.py",
        "row_count": len(assigned),
        "collision_count": sum(1 for row in assigned if row["collision_reason"]),
        "rows": assigned,
    }


def build_map() -> dict[str, Any]:
    global _LAST_REPAIRS
    numbered = _iter_identity_rows()
    if numbered:
        document = assign_paths(numbered)
    elif MAP_PATH.is_file():
        document = census.load_json(MAP_PATH)
    else:
        raise ValueError("no numbered catalog identities to map")
    _LAST_REPAIRS = repair_live_host_collisions(document)
    _LAST_REPAIRS.extend(repair_wrong_material_paths(document))
    return document


def load_map() -> dict[str, Any]:
    global _MAP_CACHE
    if _MAP_CACHE is None:
        if not MAP_PATH.is_file():
            raise ValueError("missing tools/catalog_modern_id_map.json")
        _MAP_CACHE = census.load_json(MAP_PATH)
    return _MAP_CACHE


def by_source() -> dict[tuple[str, int], dict[str, Any]]:
    return {
        (str(row["source_item"]), int(row["meta"])): row
        for row in load_map().get("rows") or []
    }


def by_old_path() -> dict[str, dict[str, Any]]:
    return {
        str(row["old_registry_path"]): row for row in load_map().get("rows") or []
    }


def registry_path_for(source_item: str, meta: int) -> str:
    row = by_source().get((str(source_item), int(meta)))
    if row is None:
        raise ValueError(f"no modern id for {source_item}@{meta}")
    return str(row["registry_path"])


def runtime_id_for(source_item: str, meta: int) -> str:
    return f"cruciblecraft:{registry_path_for(source_item, meta)}"


def rewrite_identity_fields(identity: dict[str, Any]) -> bool:
    source = str(identity.get("source_item") or "")
    meta = identity.get("meta")
    if not source or not isinstance(meta, int):
        return False
    row = by_source().get((source, int(meta)))
    if row is None:
        return False
    changed = False
    if identity.get("registry_path") != row["registry_path"]:
        identity["registry_path"] = row["registry_path"]
        changed = True
    runtime = f"cruciblecraft:{row['registry_path']}"
    if identity.get("runtime_id") != runtime:
        identity["runtime_id"] = runtime
        changed = True
    if is_garbage_name(str(identity.get("english_name") or "")):
        identity["english_name"] = row["english_name"]
        if is_garbage_name(str(identity.get("chinese_name") or "")):
            identity["chinese_name"] = row["english_name"]
        changed = True
    return changed


def apply_catalogs() -> None:
    load_map()
    for path in CATALOG_FILES:
        if not path.is_file():
            continue
        document = census.load_json(path)
        if path.name == "gt_stone_catalog.json":
            for identity in document.get("identities") or []:
                for variant in identity.get("variants") or []:
                    rewrite_identity_fields(
                        {
                            **variant,
                            "source_item": identity.get("source_item"),
                        }
                    )
                    source = str(identity.get("source_item") or "")
                    meta = variant.get("meta")
                    if isinstance(meta, int):
                        row = by_source().get((source, int(meta)))
                        if row:
                            variant["registry_path"] = row["registry_path"]
                            variant["runtime_id"] = row["runtime_id"]
            census.write_stable(path, document)
            continue
        key = "operands" if "operands" in document else "identities"
        for identity in document.get(key) or []:
            rewrite_identity_fields(identity)
        census.write_stable(path, document)
    tools_smelter = io.TOOLS / "smelter_mte_identity_catalog.json"
    bundled = DATA / "smelter_mte_identity_catalog.json"
    if tools_smelter.is_file() and bundled.is_file():
        census.write_stable(tools_smelter, census.load_json(bundled))


def replacement_pairs() -> list[tuple[str, str]]:
    pairs: list[tuple[str, str]] = []
    seen: set[str] = set()
    for row in load_map().get("rows") or []:
        old = str(row["old_registry_path"])
        new = str(row["registry_path"])
        if old == new or old in seen:
            continue
        seen.add(old)
        pairs.append((old, new))
        pairs.append((f"cruciblecraft:{old}", f"cruciblecraft:{new}"))
        dotted = old.replace("/", ".")
        if dotted != old and re.search(r"(?:_m\d+|mte_\d+)$", old):
            pairs.append((dotted, new.replace("/", ".")))
    pairs.sort(key=lambda item: len(item[0]), reverse=True)
    return pairs


def _should_skip(path: Path) -> bool:
    if path.name in SKIP_REPLACE_FILES:
        return True
    rel = path.as_posix().replace("\\", "/")
    if path.resolve() == MAP_PATH.resolve():
        return True
    for part in SKIP_REPLACE_PARTS:
        if f"/{part}/" in f"/{rel}/" or rel.endswith(f"/{part}"):
            return True
    if path.suffix.lower() not in {".json", ".java", ".md", ".txt", ".py"}:
        return True
    return False


def apply_text_replacements(roots: list[Path]) -> int:
    pairs = replacement_pairs()
    if not pairs:
        return 0
    mapping = {old: new for old, new in pairs}
    pattern = re.compile("|".join(re.escape(old) for old, _ in pairs))
    hints = (
        "gt_mte/",
        "gt_multiitem/",
        "gt_block/",
        "gt_stone/",
        "gt_object/",
        "gt_tool_head/",
        "gt_mte.",
        "gt_multiitem.",
        "gt_block.",
        "gt_stone.",
        "gt_object.",
    )
    changed = 0
    for root in roots:
        if not root.exists():
            continue
        files = [root] if root.is_file() else root.rglob("*")
        for path in files:
            if not path.is_file() or _should_skip(path):
                continue
            try:
                text = path.read_text(encoding="utf-8")
            except (OSError, UnicodeDecodeError):
                continue
            if not any(hint in text for hint in hints):
                continue
            updated = pattern.sub(lambda match: mapping[match.group(0)], text)
            if updated != text:
                path.write_text(updated, encoding="utf-8", newline="\n")
                changed += 1
    return changed


def _relocate(src: Path, dest: Path) -> None:
    if not src.is_file():
        return
    dest.parent.mkdir(parents=True, exist_ok=True)
    if dest.exists() and dest.resolve() != src.resolve():
        dest.unlink()
    if dest.resolve() != src.resolve():
        src.replace(dest)


def _relocate_prefixed(root: Path, old: str, new: str, suffix: str) -> None:
    src = root / f"{old}{suffix}"
    dest = root / f"{new}{suffix}"
    if src.is_file():
        _relocate(src, dest)
    parent = (root / old).parent
    stem = (root / old).name
    if not parent.is_dir():
        return
    for path in parent.glob(f"{stem}_*{suffix}"):
        if not path.is_file() or path.suffix != suffix:
            continue
        extra = path.name[len(stem) :]
        _relocate(path, root / f"{new}{extra}")


def relocate_asset_files() -> None:
    generated = (
        ROOT / "src" / "generated" / "resources" / "assets" / "cruciblecraft"
    )
    model_roots = [
        ASSETS / "models",
        ASSETS / "models" / "item",
        ASSETS / "models" / "block",
        generated / "models",
        generated / "models" / "item",
        generated / "models" / "block",
        ASSETS / "blockstates",
        generated / "blockstates",
    ]
    data_roots = [
        ROOT / "src" / "main" / "resources" / "data" / "cruciblecraft" / "loot_table" / "blocks",
        ROOT / "src" / "generated" / "resources" / "data" / "cruciblecraft" / "loot_table" / "blocks",
        ROOT / "src" / "main" / "resources" / "data" / "cruciblecraft" / "recipe",
        ROOT / "src" / "generated" / "resources" / "data" / "cruciblecraft" / "recipe",
    ]
    texture_roots = [
        ASSETS / "textures" / "item",
        ASSETS / "textures" / "block",
        generated / "textures" / "item",
        generated / "textures" / "block",
    ]
    for row in load_map().get("rows") or []:
        old = str(row["old_registry_path"])
        new = str(row["registry_path"])
        if old == new:
            continue
        for root in model_roots:
            _relocate_prefixed(root, old, new, ".json")
        for root in texture_roots:
            _relocate_prefixed(root, old, new, ".png")
        for root in data_roots:
            _relocate_prefixed(root, old, new, ".json")


def relocate_collision_item_models(remaps: list[tuple[str, str]]) -> None:
    for old, new in remaps:
        src = ASSETS / "models" / "item" / f"{old}.json"
        dest = ASSETS / "models" / "item" / f"{new}.json"
        if not src.is_file():
            continue
        try:
            text = src.read_text(encoding="utf-8")
        except (OSError, UnicodeDecodeError):
            continue
        if "minecraft:item/iron_ingot" not in text:
            continue
        _relocate(src, dest)


def rewrite_catalog_item_tags() -> None:
    for catalog_path, tag_path in (
        (DATA / "smelter_mte_identity_catalog.json", SMELTER_TAG),
        (DATA / "bath_mte_identity_catalog.json", BATH_MTE_TAG),
    ):
        if not catalog_path.is_file() or not tag_path.is_file():
            continue
        catalog = census.load_json(catalog_path)
        new_ids = [
            identity["runtime_id"]
            for identity in catalog.get("identities") or []
            if identity.get("registry_kind") == "item"
        ]
        census.write_stable(tag_path, {"replace": False, "values": new_ids})


def patch_r0_paths() -> None:
    if not R0_LEDGER.is_file():
        return
    mapped = {
        int(row["meta"]): str(row["registry_path"])
        for row in load_map().get("rows") or []
        if row.get("source_item") == "gregtech:gt.multitileentity"
    }
    document = census.load_json(R0_LEDGER)
    changed = False
    for identity in document.get("identities") or []:
        meta = identity.get("meta")
        if not isinstance(meta, int) or meta not in mapped:
            continue
        if identity.get("registry_path") != mapped[meta]:
            identity["registry_path"] = mapped[meta]
            changed = True
    if changed:
        census.write_stable(R0_LEDGER, document)
        r0_by_meta.cache_clear()


def apply_dummy_scoped_remaps(remaps: list[tuple[str, str]]) -> int:
    if not remaps:
        return 0
    pairs = []
    for old, new in remaps:
        pairs.append((f"cruciblecraft:{old}", f"cruciblecraft:{new}"))
        pairs.append((old, new))
    pairs.sort(key=lambda item: len(item[0]), reverse=True)
    changed = 0
    files: list[Path] = []
    for root in DUMMY_REMAP_DIRS:
        if root.is_file():
            files.append(root)
        elif root.is_dir():
            files.extend(path for path in root.rglob("*.json") if path.is_file())
    for path in files:
        if _should_skip(path):
            continue
        try:
            text = path.read_text(encoding="utf-8")
        except (OSError, UnicodeDecodeError):
            continue
        updated = text
        for old, new in pairs:
            updated = updated.replace(old, new)
        if updated != text:
            path.write_text(updated, encoding="utf-8", newline="\n")
            changed += 1
    return changed


def write_registry_identity_manifest() -> None:
    from tools import registry_identity

    manifest = registry_identity.compile_manifest()
    if manifest.get("errors"):
        raise ValueError(
            "registry identity errors after catalog repair: "
            + "; ".join(str(item) for item in manifest["errors"][:8])
        )
    encoded = registry_identity.dumps(manifest)
    registry_identity.atomic_io.write_bytes(registry_identity.MANIFEST, encoded)


def numbered_live_hits() -> list[str]:
    hits: list[str] = []
    scan = [
        DATA,
        ROOT / "src" / "recipe_generated",
        ROOT / "src" / "main" / "java",
        ROOT / "src" / "test" / "java",
        ROOT / "src" / "main" / "resources",
        ROOT / "src" / "generated" / "resources",
    ]
    for root in scan:
        if not root.exists():
            continue
        for path in root.rglob("*"):
            if not path.is_file() or path.suffix.lower() not in {".json", ".java"}:
                continue
            rel = path.as_posix()
            if any(part in rel for part in SKIP_REPLACE_PARTS):
                continue
            try:
                text = path.read_text(encoding="utf-8")
            except (OSError, UnicodeDecodeError):
                continue
            if NUMBERED_FORMULA_RE.search(text):
                hits.append(census.relative(path) if hasattr(census, "relative") else str(path))
    return hits


def _catalog_paths_by_source() -> dict[tuple[str, int], set[str]]:
    found: dict[tuple[str, int], set[str]] = {}
    for path in CATALOG_FILES:
        if not path.is_file():
            continue
        document = census.load_json(path)
        if path.name == "gt_stone_catalog.json":
            for identity in document.get("identities") or []:
                source = str(identity.get("source_item") or "")
                for variant in identity.get("variants") or []:
                    meta = variant.get("meta")
                    if not isinstance(meta, int):
                        continue
                    found.setdefault((source, int(meta)), set()).add(
                        strip_ns(str(variant.get("registry_path") or ""))
                    )
            continue
        key = "operands" if "operands" in document else "identities"
        for identity in document.get(key) or []:
            source = str(identity.get("source_item") or "")
            meta = identity.get("meta")
            if not source or not isinstance(meta, int):
                continue
            found.setdefault((source, int(meta)), set()).add(
                strip_ns(
                    str(identity.get("registry_path") or identity.get("runtime_id") or "")
                )
            )
    return found


def check() -> list[str]:
    errors: list[str] = []
    if not MAP_PATH.is_file():
        return ["missing tools/catalog_modern_id_map.json"]
    committed = census.load_json(MAP_PATH)
    live = build_map()
    drift = census.first_json_diff(live, committed)
    if drift:
        errors.append(f"catalog_modern_id_map.json drifted: {drift}")
    rows = committed.get("rows") or []
    if not rows:
        errors.append("catalog_modern_id_map.json has no rows")
    live_hosts = live_host_paths()
    folded = folded_existing_item_paths()
    catalog_paths = _catalog_paths_by_source()
    for row in rows:
        key = (str(row["source_item"]), int(row["meta"]))
        paths = catalog_paths.get(key)
        expected = str(row["registry_path"])
        if not paths:
            if row.get("catalog") == "java_hardcoded":
                continue
            errors.append(f"map row missing from catalogs: {key}")
            continue
        if expected not in paths:
            errors.append(f"{key} catalogs have {sorted(paths)} not {expected}")
        if any(is_numbered_path(path) for path in paths):
            errors.append(f"{key} still numbered {sorted(paths)}")
        if re.search(r"_m\d+$", expected):
            errors.append(f"modern id still numbered: {expected}")
        source = str(row.get("source_item") or "")
        if re.match(r"^gregtech:gt_", source):
            errors.append(f"corrupted GT6 source_item {source}@{row['meta']}")
        if expected in live_hosts and expected not in folded:
            errors.append(
                f"dummy stole live host {expected} ({row['source_item']}@{row['meta']})"
            )
    remainder = thp.load_remap().get("remainder") or []
    if remainder:
        errors.append(f"tool-head remainder must be 0, found {len(remainder)}")
    materials = _material_index()
    cc_ids = {cc for cc in materials.values() if cc}
    registered = registered_holdable_paths()
    for path in CATALOG_FILES:
        if not path.is_file():
            continue
        document = census.load_json(path)
        identities = []
        if path.name == "gt_stone_catalog.json":
            for identity in document.get("identities") or []:
                for variant in identity.get("variants") or []:
                    identities.append(variant)
        else:
            key = "operands" if "operands" in document else "identities"
            identities = list(document.get(key) or [])
        for identity in identities:
            if str(identity.get("registry_kind") or "") != "existing_item":
                continue
            target = strip_ns(
                str(
                    identity.get("registry_path")
                    or identity.get("runtime_id")
                    or ""
                )
            )
            if target not in registered:
                errors.append(
                    f"existing_item {identity.get('meta')} -> {target} is not "
                    "a live host or catalog item"
                )
    for row in rows:
        if str(row.get("source_item") or "") != "gregtech:gt.multitileentity":
            continue
        expected_cc = _english_material(str(row.get("english_name") or ""), materials)
        if not expected_cc:
            continue
        borrowed = _borrowed_material_segments(
            str(row["registry_path"]), expected_cc, cc_ids
        )
        for segment in borrowed:
            errors.append(
                f"meta {row['meta']} english {row.get('english_name')!r} "
                f"is {expected_cc} but path borrowed {segment}: "
                f"{row['registry_path']}"
            )
    for path in CATALOG_FILES:
        if not path.is_file():
            continue
        blob = path.read_text(encoding="utf-8")
        if NUMBERED_FORMULA_RE.search(blob):
            errors.append(f"numbered formula remains in {census.relative(path)}")
    hits = numbered_live_hits()
    if hits:
        errors.append(
            "numbered formula remains in live trees: " + ", ".join(hits[:12])
        )
    return errors


def write_map() -> dict[str, Any]:
    global _MAP_CACHE
    document = build_map()
    census.write_stable(MAP_PATH, document)
    _MAP_CACHE = document
    return document


def _modern_path_remaps(
    previous: dict[str, Any], document: dict[str, Any]
) -> list[tuple[str, str]]:
    old_by_key = {
        (str(row["source_item"]), int(row["meta"])): str(row["registry_path"])
        for row in previous.get("rows") or []
    }
    remaps: list[tuple[str, str]] = []
    for row in document.get("rows") or []:
        key = (str(row["source_item"]), int(row["meta"]))
        old = old_by_key.get(key)
        new = str(row["registry_path"])
        if old and old != new:
            remaps.append((old, new))
    return remaps


def apply() -> dict[str, Any]:
    previous = census.load_json(MAP_PATH) if MAP_PATH.is_file() else {"rows": []}
    document = write_map()
    path_remaps = _LAST_REPAIRS + _modern_path_remaps(previous, document)
    apply_catalogs()
    relocate_asset_files()
    relocate_collision_item_models(path_remaps)
    rewrite_catalog_item_tags()
    patch_r0_paths()
    dummy_files = apply_dummy_scoped_remaps(path_remaps)
    numbered_files = 0
    if _iter_identity_rows():
        numbered_files = apply_text_replacements(
            [
                ROOT / "src",
                io.TOOLS,
                ROOT / "docs",
            ]
        )
    write_registry_identity_manifest()
    return {
        "row_count": document["row_count"],
        "live_host_repairs": len(_LAST_REPAIRS),
        "path_remaps": len(path_remaps),
        "dummy_remap_files": dummy_files,
        "numbered_replace_files": numbered_files,
    }


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--apply", action="store_true")
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args(argv)
    if args.write == args.check == args.apply and not (args.write or args.check or args.apply):
        parser.error("choose --write, --apply, and/or --check")
    try:
        if args.apply:
            result = apply()
            print(json.dumps(result))
        elif args.write:
            document = write_map()
            print(
                json.dumps(
                    {
                        "wrote": str(MAP_PATH),
                        "row_count": document["row_count"],
                        "live_host_repairs": len(_LAST_REPAIRS),
                    }
                )
            )
        if args.check:
            errors = check()
            if errors:
                print("\n".join(errors), file=sys.stderr)
                return 1
            print("catalog modern ids are current")
        return 0
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"catalog modern ids failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
