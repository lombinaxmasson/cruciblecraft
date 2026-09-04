"""Single authority for folding GT6 toolHead* identities into material prefixes."""
from __future__ import annotations

import json
import re
from collections import defaultdict
from dataclasses import dataclass
from pathlib import Path
from typing import Any

from tools import io_common as files

ROOT = files.ROOT
TOOLS = files.TOOLS
SOURCE_REVISION = files.SOURCE_REVISION

REMAP_TOOLS = TOOLS / "tool_head_prefix_remap.json"
REQUIRED_FORMS = TOOLS / "tool_head_required_forms.json"
BUNDLED_REMAP = (
    ROOT / "src/main/resources/data/cruciblecraft/tool_head_prefix_remap.json"
)
PREFIX_ROOT = ROOT / "src/main/resources/data/cruciblecraft/material_prefixes"
PREFIX_INDEX = PREFIX_ROOT / "index.json"
TEXTURE_DEST = (
    ROOT
    / "src/main/resources/assets/cruciblecraft/textures/item/material"
)
GT6_METALLIC = (
    ROOT
    / "gt6_code/gregtech6/src/main/resources/assets/gregtech/textures/items"
    / "materialicons/METALLIC"
)
OREDICT_PREFIXES = TOOLS / "gt6_oredict_prefixes_normalized.json"
OREDICT_XREF = TOOLS / "gt6_oredict_cross_reference.json"
BUNDLED_BATH = (
    ROOT / "src/main/resources/data/cruciblecraft/bath_identity_catalog.json"
)
BUNDLED_SEMANTIC = (
    ROOT / "src/main/resources/data/cruciblecraft/semantic_object_catalog.json"
)
BATH_IDENTITY_IDENTITY_CATALOG = TOOLS / "bath_identity_identity_catalog.json"

_CAMEL_RE = re.compile(r"([a-z0-9])([A-Z])")
_META_PREFIX = "gregtech:gt.meta."

CHINESE_FORM: dict[str, str] = {
    "tool_head_arrow": "箭头",
    "tool_head_axe": "斧头",
    "tool_head_axe_double": "双刃斧头",
    "tool_head_builderwand": "建筑杖头",
    "tool_head_buzz_saw": "圆锯片",
    "tool_head_chainsaw": "链锯头",
    "tool_head_chisel": "凿头",
    "tool_head_construction_pickaxe": "建筑镐头",
    "tool_head_drill": "钻头",
    "tool_head_file": "锉头",
    "tool_head_hammer": "锤头",
    "tool_head_hoe": "锄头",
    "tool_head_pickaxe": "镐头",
    "tool_head_pickaxe_gem": "宝石镐头",
    "tool_head_plow": "犁头",
    "tool_head_raw_arrow": "生箭头",
    "tool_head_raw_axe": "生斧头",
    "tool_head_raw_axe_double": "生双刃斧头",
    "tool_head_raw_chisel": "生凿头",
    "tool_head_raw_hoe": "生锄头",
    "tool_head_raw_pickaxe": "生镐头",
    "tool_head_raw_plow": "生犁头",
    "tool_head_raw_saw": "生锯片",
    "tool_head_raw_sense": "生镰刀片",
    "tool_head_raw_shovel": "生铲头",
    "tool_head_raw_spade": "生锹头",
    "tool_head_raw_sword": "生剑刃",
    "tool_head_raw_universal_spade": "生万能锹头",
    "tool_head_saw": "锯片",
    "tool_head_screwdriver": "起子头",
    "tool_head_sense": "镰刀片",
    "tool_head_shovel": "铲头",
    "tool_head_spade": "锹头",
    "tool_head_sword": "剑刃",
    "tool_head_universal_spade": "万能锹头",
    "tool_head_wrench": "扳手头",
}

ENGLISH_FORM: dict[str, str] = {
    "tool_head_arrow": "Arrow Head",
    "tool_head_axe": "Axe Head",
    "tool_head_axe_double": "Double Axe Head",
    "tool_head_builderwand": "Builder's Wand Cap",
    "tool_head_buzz_saw": "Buzzsaw Blade",
    "tool_head_chainsaw": "Chainsaw Tip",
    "tool_head_chisel": "Chisel Head",
    "tool_head_construction_pickaxe": "Construction Pickaxe Head",
    "tool_head_drill": "Drill Tip",
    "tool_head_file": "File Head",
    "tool_head_hammer": "Hammer Head",
    "tool_head_hoe": "Hoe Head",
    "tool_head_pickaxe": "Pickaxe Head",
    "tool_head_pickaxe_gem": "Gem-Tipped Pickaxe Head",
    "tool_head_plow": "Plow Head",
    "tool_head_raw_arrow": "Raw Arrow Head",
    "tool_head_raw_axe": "Raw Axe Head",
    "tool_head_raw_axe_double": "Raw Double Axe Head",
    "tool_head_raw_chisel": "Raw Chisel Head",
    "tool_head_raw_hoe": "Raw Hoe Head",
    "tool_head_raw_pickaxe": "Raw Pickaxe Head",
    "tool_head_raw_plow": "Raw Plow Head",
    "tool_head_raw_saw": "Raw Saw Blade",
    "tool_head_raw_sense": "Raw Sense Blade",
    "tool_head_raw_shovel": "Raw Shovel Head",
    "tool_head_raw_spade": "Raw Spade Head",
    "tool_head_raw_sword": "Raw Sword Blade",
    "tool_head_raw_universal_spade": "Raw Universal Spade Head",
    "tool_head_saw": "Saw Blade",
    "tool_head_screwdriver": "Screwdriver Tip",
    "tool_head_sense": "Sense Blade",
    "tool_head_shovel": "Shovel Head",
    "tool_head_spade": "Spade Head",
    "tool_head_sword": "Sword Blade",
    "tool_head_universal_spade": "Universal Spade Head",
    "tool_head_wrench": "Wrench Tip",
}

ORDINARY_OBJECT_CATALOGS = (
    "smelter/ordinary-closure",
    "mixer/ordinary-closure",
    "drying/ordinary-closure",
    "electrolyzer/ordinary-closure",
    "centrifuge/ordinary-closure",
    "autoclave/ordinary-closure",
    "compressor/ordinary-closure",
)

_CACHE: dict[str, Any] = {}


def gt_prefix_name(source_item: str) -> str | None:
    item = str(source_item or "")
    if not item.startswith(_META_PREFIX):
        return None
    name = item[len(_META_PREFIX) :]
    if not name.startswith("toolHead"):
        return None
    return name


def cc_prefix_id(gt_prefix: str) -> str:
    return _CAMEL_RE.sub(r"\1_\2", gt_prefix).lower()


def source_item_to_prefix(source_item: str) -> str | None:
    gt_prefix = gt_prefix_name(source_item)
    if not gt_prefix:
        return None
    return cc_prefix_id(gt_prefix)


def load_gt_prefix_units() -> dict[str, tuple[int, str]]:
    cached = _CACHE.get("gt_prefix_units")
    if cached is not None:
        return cached
    document = files.load_json(OREDICT_PREFIXES)
    mapped: dict[str, tuple[int, str]] = {}
    for row in document.get("records") or []:
        name = str(row.get("source_name") or "")
        if not name.startswith("toolHead"):
            continue
        amount = row.get("amount") or {}
        units = amount.get("cc_units")
        integral = bool(amount.get("integral_cc_units"))
        if isinstance(units, int) and integral:
            mapped[name] = (units, "SOURCE_BACKED")
        else:
            mapped[name] = (144, "DESIGN_POLICY")
    _CACHE["gt_prefix_units"] = mapped
    return mapped


def load_material_id_to_cc() -> dict[int, str]:
    cached = _CACHE.get("material_id_to_cc")
    if cached is not None:
        return cached
    xref = files.load_json(OREDICT_XREF)
    mapped = {
        int(key): str(value)
        for key, value in (xref.get("material_id_to_cc") or {}).items()
        if str(key).lstrip("-").isdigit()
    }
    _CACHE["material_id_to_cc"] = mapped
    return mapped


def prefix_item_to_form_overlay() -> dict[str, str]:
    return {
        f"{_META_PREFIX}{gt_prefix}": cc_prefix_id(gt_prefix)
        for gt_prefix in load_gt_prefix_units()
    }


def prefix_document(form: str, gt_prefix: str) -> dict[str, Any]:
    units_table = load_gt_prefix_units()
    units, _authority = units_table.get(gt_prefix, (144, "DESIGN_POLICY"))
    return {
        "aliases": [form.replace("_", "")],
        "capabilities": [],
        "generation_flag": f"cruciblecraft:generates_{form}",
        "id": f"cruciblecraft:{form}",
        "model_template": "minecraft:item/generated",
        "model_texture": f"cruciblecraft:item/material/{form}",
        "serialized_path": form,
        "tag_directory": f"{form}s",
        "tag_namespace": "c",
        "units": int(units),
    }


def _catalog_identities(path: Path) -> list[dict[str, Any]]:
    if not path.is_file():
        return []
    document = files.load_json(path)
    return [row for row in document.get("identities") or [] if isinstance(row, dict)]


def collect_tool_head_rows() -> dict[tuple[str, int], dict[str, Any]]:
    wanted: dict[tuple[str, int], dict[str, Any]] = {}
    paths = [
        BATH_IDENTITY_IDENTITY_CATALOG,
        BUNDLED_BATH,
        BUNDLED_SEMANTIC,
        *[
            TOOLS / "waves" / slug / "object_catalog.json"
            for slug in ORDINARY_OBJECT_CATALOGS
        ],
    ]
    if REMAP_TOOLS.is_file():
        previous = files.load_json(REMAP_TOOLS)
        for row in list(previous.get("mapped") or []) + list(previous.get("remainder") or []):
            source_item = str(row.get("source_item") or "")
            meta = row.get("meta")
            if source_item and isinstance(meta, int):
                wanted.setdefault((source_item, meta), {
                    "kind": "tool_head",
                    "meta": meta,
                    "source_item": source_item,
                })
    for path in paths:
        for row in _catalog_identities(path):
            if str(row.get("kind") or "") != "tool_head":
                continue
            source_item = str(row.get("source_item") or "")
            meta = row.get("meta")
            if not source_item or not isinstance(meta, int):
                continue
            if gt_prefix_name(source_item) is None:
                continue
            key = (source_item, meta)
            wanted.setdefault(key, row)
    return wanted


@dataclass(frozen=True)
class MappedRow:
    source_item: str
    meta: int
    material: str
    prefix: str
    gt_prefix: str
    runtime_id: str
    units: int
    units_authority: str
    catalog_sources: tuple[str, ...]


def resolve_row(
    source_item: str,
    meta: int,
    *,
    materials: dict[int, str],
    units_table: dict[str, tuple[int, str]],
) -> tuple[MappedRow | None, dict[str, Any] | None]:
    gt_prefix = gt_prefix_name(source_item)
    if gt_prefix is None:
        return None, {
            "meta": meta,
            "reason": "source_item_is_not_toolHead_prefix",
            "source_item": source_item,
        }
    prefix = cc_prefix_id(gt_prefix)
    material = materials.get(meta)
    if not material:
        return None, {
            "meta": meta,
            "reason": "material_id_to_cc_miss",
            "source_item": source_item,
        }
    units, units_authority = units_table.get(gt_prefix, (144, "DESIGN_POLICY"))
    mapped = MappedRow(
        source_item=source_item,
        meta=meta,
        material=material,
        prefix=prefix,
        gt_prefix=gt_prefix,
        runtime_id=f"cruciblecraft:{material}/{prefix}",
        units=int(units),
        units_authority=units_authority,
        catalog_sources=(),
    )
    return mapped, None


def _mapped_payload(row: MappedRow) -> dict[str, Any]:
    return {
        "gt_prefix": row.gt_prefix,
        "material": row.material,
        "meta": row.meta,
        "prefix": row.prefix,
        "runtime_id": row.runtime_id,
        "source_item": row.source_item,
        "units": row.units,
        "units_authority": row.units_authority,
    }


def build_remap() -> dict[str, Any]:
    materials = load_material_id_to_cc()
    units_table = load_gt_prefix_units()
    rows = collect_tool_head_rows()
    mapped: list[dict[str, Any]] = []
    remainder: list[dict[str, Any]] = []
    prefixes: dict[str, str] = {}
    winners: dict[tuple[str, int], str] = {}
    for (source_item, meta), _raw in sorted(rows.items()):
        resolved, leftover = resolve_row(
            source_item, meta, materials=materials, units_table=units_table
        )
        if leftover is not None:
            remainder.append(leftover)
            continue
        assert resolved is not None
        key = (source_item, meta)
        if key in winners and winners[key] != resolved.runtime_id:
            raise ValueError(
                f"tool-head remap winner conflict for {source_item}@{meta}: "
                f"{winners[key]} vs {resolved.runtime_id}"
            )
        winners[key] = resolved.runtime_id
        mapped.append(_mapped_payload(resolved))
        prefixes[resolved.prefix] = resolved.gt_prefix
    required: dict[str, set[str]] = defaultdict(set)
    for row in mapped:
        required[str(row["material"])].add(str(row["prefix"]))
    source_items = sorted({row["source_item"] for row in mapped} | {
        row["source_item"] for row in remainder
    })
    return {
        "counts": {
            "mapped": len(mapped),
            "prefix_count": len(prefixes),
            "remainder": len(remainder),
            "required_form_pairs": sum(len(forms) for forms in required.values()),
            "required_materials": len(required),
            "source_item_count": len(source_items),
            "union_keys": len(rows),
        },
        "generated_by": "python tools/build_tool_head_prefix_remap.py",
        "mapped": mapped,
        "note": (
            "One remap for bath ∪ semantic toolHead* identities. "
            "material_id_to_cc misses stay remainder. No unique-item aliases."
        ),
        "prefixes": {
            prefix: {
                "chinese_form": CHINESE_FORM.get(prefix, prefix),
                "english_form": ENGLISH_FORM.get(prefix, prefix.replace("_", " ").title()),
                "gt_prefix": gt_prefix,
                "source_item": f"{_META_PREFIX}{gt_prefix}",
            }
            for prefix, gt_prefix in sorted(prefixes.items())
        },
        "remainder": remainder,
        "required_forms": {
            material: sorted(forms) for material, forms in sorted(required.items())
        },
        "schema_version": 1,
        "source_item_set": source_items,
        "source_revision": SOURCE_REVISION,
        "status": "TOOL_HEAD_PREFIX_REMAP",
    }


def required_forms_document(remap: dict[str, Any] | None = None) -> dict[str, Any]:
    document = remap if remap is not None else build_remap()
    prefixes = document.get("prefixes") or {}
    return {
        "counts": dict(document.get("counts") or {}),
        "generated_by": "python tools/build_tool_head_prefix_remap.py",
        "new_prefix_forms": sorted(prefixes),
        "note": (
            "Tool-head required forms from the single remap. "
            "Does not write material_registration_gate.json."
        ),
        "required_forms": dict(document.get("required_forms") or {}),
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "status": "TOOL_HEAD_REQUIRED_FORMS",
    }


def load_remap() -> dict[str, Any]:
    cached = _CACHE.get("remap")
    if cached is not None:
        return cached
    path = REMAP_TOOLS if REMAP_TOOLS.is_file() else BUNDLED_REMAP
    if not path.is_file():
        return {}
    document = files.load_json(path)
    _CACHE["remap"] = document
    return document


def mapped_runtime(source_item: str, meta: int) -> str | None:
    index = _mapped_index()
    return index.get((str(source_item), int(meta)))


def is_mapped(source_item: str, meta: int) -> bool:
    return mapped_runtime(source_item, meta) is not None


def remainder_runtime_ids() -> set[str]:
    document = load_remap()
    ids: set[str] = set()
    for row in document.get("remainder") or []:
        runtime = str(row.get("runtime_id") or "")
        if runtime:
            ids.add(runtime)
    return ids


def is_forbidden_unique_tool_head(runtime_id: str) -> bool:
    runtime = str(runtime_id or "")
    if not runtime.startswith("cruciblecraft:gt_tool_head/"):
        return False
    return runtime not in remainder_runtime_ids()


def rewrite_published_runtime(runtime: str, source_item: str, meta: Any) -> str:
    published = str(runtime or "")
    item = str(source_item or "")
    if isinstance(meta, int):
        mapped = mapped_runtime(item, meta)
        if mapped:
            return mapped
    if is_forbidden_unique_tool_head(published):
        raise ValueError(f"refusing unique tool_head item: {published}")
    return published


def assert_text_has_no_mapped_unique_tool_heads(text: str, where: str) -> None:
    if "cruciblecraft:gt_tool_head/" not in text:
        return
    remainder = remainder_runtime_ids()
    for match in re.findall(r"cruciblecraft:gt_tool_head/[A-Za-z0-9_./-]+", text):
        if match not in remainder:
            raise ValueError(f"mapped tool_head unique item in {where}: {match}")


def _mapped_index() -> dict[tuple[str, int], str]:
    cached = _CACHE.get("mapped_index")
    if cached is not None:
        return cached
    document = load_remap()
    index: dict[tuple[str, int], str] = {}
    for row in document.get("mapped") or []:
        source_item = str(row.get("source_item") or "")
        meta = row.get("meta")
        runtime = str(row.get("runtime_id") or "")
        if source_item and isinstance(meta, int) and runtime:
            index[(source_item, meta)] = runtime
    _CACHE["mapped_index"] = index
    return index


def mapped_records() -> list[dict[str, Any]]:
    return list((load_remap().get("mapped") or []))


def copy_source_backed_textures(remap: dict[str, Any]) -> list[str]:
    if not GT6_METALLIC.is_dir():
        raise ValueError(
            "GT6 METALLIC toolHead textures missing; "
            f"expected {files.relative(GT6_METALLIC)}"
        )
    TEXTURE_DEST.mkdir(parents=True, exist_ok=True)
    written: list[str] = []
    for prefix, meta in sorted((remap.get("prefixes") or {}).items()):
        gt_prefix = str(meta.get("gt_prefix") or "")
        source = GT6_METALLIC / f"{gt_prefix}.png"
        if not source.is_file():
            raise ValueError(f"missing SOURCE_BACKED tool head texture: {source}")
        dest = TEXTURE_DEST / f"{prefix}.png"
        dest.write_bytes(source.read_bytes())
        written.append(files.relative(dest))
    return written


def write_prefix_json(remap: dict[str, Any]) -> None:
    PREFIX_ROOT.mkdir(parents=True, exist_ok=True)
    index = list(files.load_json(PREFIX_INDEX)) if PREFIX_INDEX.is_file() else []
    known = set(index)
    for prefix, meta in sorted((remap.get("prefixes") or {}).items()):
        gt_prefix = str(meta.get("gt_prefix") or "")
        filename = f"{prefix}.json"
        files.write_stable(PREFIX_ROOT / filename, prefix_document(prefix, gt_prefix))
        if filename not in known:
            index.append(filename)
            known.add(filename)
    files.write_stable(PREFIX_INDEX, index)


def write_zh_prefix_names(remap: dict[str, Any]) -> None:
    path = ROOT / "src/main/resources/data/cruciblecraft/material_zh_cn.json"
    document = files.load_json(path)
    prefixes = dict(document.get("prefixes") or {})
    for prefix, meta in (remap.get("prefixes") or {}).items():
        prefixes[prefix] = str(meta.get("chinese_form") or CHINESE_FORM[prefix])
    document["prefixes"] = dict(sorted(prefixes.items()))
    files.write_stable(path, document)


def clear_cache() -> None:
    _CACHE.clear()
