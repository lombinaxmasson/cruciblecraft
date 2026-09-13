#!/usr/bin/env python3
"""portfolio/mte-identity-disposition-r0: freeze 1,817 catalog MTE identities."""
from __future__ import annotations

import argparse
import json
import re
import sys
from collections import Counter
from typing import Any

from tools import closeout_seal
from tools import census_common as census
from tools import portfolio_one_x as one_x
from tools.recipe_bulk.slugs import KNOWN_SEMANTIC_SLUGS
from tools.wave_closeout import known_slugs
from tools.wave_closeout import spec_for
from tools.wave_closeout import wave_dir

SLUG = "portfolio/mte-identity-disposition-r0"
PREDECESSOR = "recycling/smelter-mte-identity"
PREDECESSOR_STATUS = "SMELTER_MTE_IDENTITY_READY"
STATUS = "MTE_IDENTITY_DISPOSITION_R0_READY"
SOURCE_REVISION = census.SOURCE_REVISION
GENERATED_BY = "python tools/build_mte_identity_disposition_r0.py"
IDENTITY_ANCHOR = "Bath / Smelter MTE holdable identity"
EXPECTED_IDENTITIES = 1817
EXPECTED_SCATTER = 1771
EXPECTED_BATH = 46
EXPECTED_ATTACHMENTS = 33
EXPECTED_WOODEN_PANELS = 23
EXPECTED_DRIVE = 63
EXPECTED_LEFTOVER = 39
DUST_FUNNEL_META = 32704
FAUCET_METAS = frozenset(
    {
        1700,
        1720,
        1721,
        1722,
        1723,
        1724,
        1725,
        1727,
        1728,
        1729,
        1730,
        1731,
        1732,
        1733,
        1734,
        1737,
        1738,
        1739,
        1741,
        1742,
        1744,
        1749,
    }
)
TAP_METAS = frozenset({32730, 32731, 32732})
NOZZLE_METAS = frozenset({32749, 32750})
CAP_NOZZLE_METAS = frozenset({32061, 32062, 32082})
FLUID_FUNNEL_METAS = frozenset({32725, 32726, 32727})
ATTACHMENT_METAS = (
    FAUCET_METAS | TAP_METAS | NOZZLE_METAS | CAP_NOZZLE_METAS | FLUID_FUNNEL_METAS
)
ATTACHMENT_CLASS_FAMILY = {
    "MultiTileEntityFaucet": "fluid_attachment",
    "MultiTileEntityFluidTap": "fluid_attachment",
    "MultiTileEntityFluidNozzle": "fluid_attachment",
    "MultiTileEntityFluidCapNozzle": "fluid_attachment",
    "MultiTileEntityFluidFunnel": "fluid_attachment",
    "MultiTileEntityDustFunnel": "hopper",
}
DISPOSITIONS = (
    "identity_only",
    "realized_natively",
    "attachment_candidate",
    "deferred",
)
FEASIBILITY_VALUES = (
    "bounded_extension",
    "requires_new_runtime",
    "defer_to_portfolio",
    "blocked",
)
FORBIDDEN_SUCCESSORS = (
    "attachment-implementation",
    "faucet-implementation",
    "tap-implementation",
    "nozzle-implementation",
    "funnel-implementation",
    "mte-implementation",
    "nuclear",
    "combinatorial",
    "remainder-other-features",
    "logistics-cover-net-core",
    "t13c-exclusion-reclaim-core",
)
ALLOWED_TOPOLOGY_KEYS = (
    "append_only",
    "complete_family_count",
    "generated_by",
    "next_unassigned",
    "remaining_recipe_gap",
    "schema_version",
    "source_revision",
    "status",
    "unique_active_wave",
    "wave_slug",
)
TAG_FAMILY = {
    "Chests": "furniture_storage",
    "Safes": "furniture_storage",
    "Storage": "furniture_storage",
    "Crafting Tables": "furniture_storage",
    "Scaffolds": "furniture_storage",
    "Logistics": "logistics",
    "Hoppers": "hopper",
    "Basic Machines": "processing_machine",
    "Automatic Tools": "processing_machine",
    "Engines": "energy_converter",
    "Motors": "energy_converter",
    "Magnets": "energy_converter",
    "Steam Boilers": "energy_converter",
    "Burning Boxes": "energy_converter",
    "Turbines": "energy_converter",
    "Heaters": "energy_converter",
    "Transformers": "energy_converter",
    "Battery Boxes": "energy_converter",
    "Solar Panels": "energy_converter",
    "Coolers": "energy_converter",
    "Laser Absorbers": "energy_converter",
    "Crystal Chargers": "energy_converter",
    "Quantum Energizers": "energy_converter",
    "Reactors": "reactor",
    "Smelting Crucibles": "crucible_foundry",
    "Molds": "crucible_foundry",
    "Crucibles Faucets": "fluid_attachment",
    "Axles and Gearboxes": "drive",
    "Misc Tool Blocks": "misc_tool",
    "Redstone Wires": "redstone_wire",
    "Extenders": "extender",
    "Ropes": "decorative",
    "Panels": "decorative",
    "Multiblock Machines": "multiblock",
    "Item Pipes": "connector",
    "Fluid Pipes": "connector",
    "Electric Wires": "connector",
    "Untyped": "untyped",
    "Sensors": "sensor",
    "Computer": "computer",
    "Long Distance Transport": "connector",
}
FURNITURE_TAGS = frozenset(
    {
        "Chests",
        "Safes",
        "Storage",
        "Crafting Tables",
        "Scaffolds",
        "Logistics",
    }
)
CRUCIBLE_TAGS = frozenset({"Smelting Crucibles", "Molds"})
REALIZED_TAGS = frozenset({"Hoppers", "Reactors", "Transformers"})
IDENTITY_TAGS = frozenset(
    {
        "Axles and Gearboxes",
        "Redstone Wires",
        "Extenders",
        "Ropes",
        "Panels",
        "Item Pipes",
        "Fluid Pipes",
        "Electric Wires",
        "Sensors",
        "Computer",
        "Long Distance Transport",
        "Untyped",
        "Misc Tool Blocks",
        "Battery Boxes",
        "Solar Panels",
        "Coolers",
        "Laser Absorbers",
        "Crystal Chargers",
        "Quantum Energizers",
        "Engines",
        "Motors",
        "Magnets",
        "Steam Boilers",
        "Burning Boxes",
        "Turbines",
        "Heaters",
        "Multiblock Machines",
        "Automatic Tools",
    }
)
CATALOG_PATH = (
    census.ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "smelter_mte_identity_catalog.json"
)
LOADER_PATH = (
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
CAPABILITY_MAP = (
    census.TOOLS
    / "waves"
    / "portfolio"
    / "source-capability-inventory"
    / "capability_map.json"
)
GROWTH_ORDER = (
    census.TOOLS
    / "waves"
    / "portfolio"
    / "source-capability-growth-order"
    / "growth_order.json"
)
MACHINE_TIERS = (
    census.ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "machine_tiers.json"
)
LEFTOVER_LATER = (
    census.TOOLS
    / "waves"
    / "portfolio"
    / "source-capability-map-r0"
    / "leftover_later.json"
)
CAPABILITY_JSON = (
    census.ROOT
    / "tools"
    / "capabilities"
    / "portfolio"
    / "mte-identity-disposition-r0"
    / "capability.json"
)
TAG_ID = re.compile(r'"([^"]+)"\s*,\s*(\d+)\s*,\s*(\d+)\s*,')
METALSET_TAG = re.compile(r'"([^"]+)"\s*,\s*((?:\d+\s*\+\s*)?aID)\s*,')
CLASS_TOKEN = re.compile(r"(MultiTileEntity\w+)\.class|\baClass\b")
ACLASS_ASSIGN = re.compile(r"aClass\s*=\s*(MultiTileEntity\w+)\.class")
METHOD_START = re.compile(r"private static void (\w+)\(")
METALSET_CALL = re.compile(
    r"metalset\(\s*aRegistry,\s*aMetal,\s*aUtilMetal,\s*aMachine,\s*aWooden,\s*[^,]+,\s*(\d+)\s*,"
)
ITEM_PIPES = re.compile(r"MultiTileEntityPipeItem\.addItemPipes\((\d+)\s*,")
FLUID_PIPES = re.compile(r"MultiTileEntityPipeFluid\.addFluidPipes\((\d+)\s*,")
ELECTRIC_WIRES = re.compile(
    r"MultiTileEntityWireElectric\.addElectricWires\((\d+)\s*,\s*\d+\s*,.*?,\s*([TF])\s*,\s*([TF])\s*,\s*([TF])\s*,\s*aRegistry"
)
FOR_HEAD = re.compile(
    r"for\s*\(\s*int\s+i\s*=\s*0\s*;\s*i\s*<\s*(\d+)\s*;\s*i\+\+\s*\)\s*"
)
LOOP_TAG = re.compile(r'"([^"]+)"\s*,\s*([^,]+?)\s*,\s*(\d+)\s*,')
SAFE_EXPR = re.compile(r"^[0-9A-Za-z+]+$")


def leftover_later_count() -> int:
    leftover = census.load_json(LEFTOVER_LATER)
    total = int(leftover["counts"]["total"])
    if total != EXPECTED_LEFTOVER:
        raise ValueError(f"leftover_later_count {total} != {EXPECTED_LEFTOVER}")
    return total


def nuclear_started() -> bool:
    return one_x.nuclear_started()


def require_registered(slug: str) -> list[str]:
    errors: list[str] = []
    if slug not in KNOWN_SEMANTIC_SLUGS:
        errors.append(f"{slug} missing from KNOWN_SEMANTIC_SLUGS")
    if slug not in known_slugs():
        errors.append(f"{slug} missing from wave_closeout")
    return errors


def identity_capability_row() -> dict[str, Any]:
    document = census.load_json(CAPABILITY_MAP)
    for row in document.get("rows") or document.get("capabilities") or []:
        if row.get("gt6_anchor") == IDENTITY_ANCHOR:
            return row
    raise ValueError(f"missing capability-map row {IDENTITY_ANCHOR}")


def catalog_identities() -> list[dict[str, Any]]:
    catalog = census.load_json(CATALOG_PATH)
    rows = list(catalog.get("identities") or [])
    if len(rows) != EXPECTED_IDENTITIES:
        raise ValueError(f"catalog identities {len(rows)} != {EXPECTED_IDENTITIES}")
    if int(catalog.get("bath_overlap_count", -1)) != EXPECTED_BATH:
        raise ValueError("catalog bath_overlap_count drifted")
    return rows


def processing_kind_tokens() -> tuple[str, ...]:
    tiers = census.load_json(MACHINE_TIERS)
    tokens: set[str] = set()
    for row in tiers.get("variants") or []:
        kind = str(row.get("kind") or "")
        token = kind.split(":")[-1].replace("_", " ").strip()
        if token:
            tokens.add(token)
            last = token.split()[-1]
            if len(last) >= 5:
                tokens.add(last)
    return tuple(sorted(tokens))


def authority_hashes() -> dict[str, str]:
    return {
        "capability_map": census.sha256_file(CAPABILITY_MAP),
        "growth_order": census.sha256_file(GROWTH_ORDER),
        "smelter_mte_identity_catalog": census.sha256_file(CATALOG_PATH),
        "smelter_mte_identity_seal": census.sha256_file(
            wave_dir(PREDECESSOR) / "closeout_seal.json"
        ),
        "loader_multi_tile_entities": census.sha256_file(LOADER_PATH),
    }


def _strip_line_comments(source: str) -> str:
    lines = []
    for line in source.splitlines():
        if line.lstrip().startswith("//"):
            continue
        lines.append(line)
    return "\n".join(lines)


def _eval_expr(expr: str, **names: int) -> int:
    compact = expr.replace(" ", "")
    if not SAFE_EXPR.fullmatch(compact):
        raise ValueError(f"unsafe id expression {expr!r}")
    for name, value in names.items():
        compact = compact.replace(name, str(value))
    if not re.fullmatch(r"\d+(?:\+\d+)*", compact):
        raise ValueError(f"unresolved id expression {expr!r}")
    return sum(int(part) for part in compact.split("+"))


def _class_from(text: str, current: str) -> str:
    match = CLASS_TOKEN.search(text)
    if match and match.group(1):
        return match.group(1)
    if match:
        return current
    return current


def _put(
    regs: dict[int, dict[str, str]],
    meta: int,
    tag: str,
    gt6_class: str,
    method: str,
) -> None:
    regs[int(meta)] = {
        "gt6_class": gt6_class,
        "method": method,
        "tag": tag,
    }


def parse_loader_registry(source: str | None = None) -> dict[int, dict[str, str]]:
    text = _strip_line_comments(
        source if source is not None else LOADER_PATH.read_text(encoding="utf-8")
    )
    regs: dict[int, dict[str, str]] = {}
    metalset_fn = re.search(r"private static void metalset\((.*?)\n\t\}", text, re.S)
    if metalset_fn is None:
        raise ValueError("metalset() missing from Loader_MultiTileEntities.java")
    offsets: list[tuple[str, str, str]] = []
    current = "unknown"
    for line in metalset_fn.group(0).splitlines():
        assigned = ACLASS_ASSIGN.search(line)
        if assigned:
            current = assigned.group(1)
        match = METALSET_TAG.search(line)
        if not match:
            continue
        tag, expr = match.groups()
        offsets.append((tag, expr.replace(" ", ""), _class_from(line, current)))
    for aid in METALSET_CALL.findall(text):
        aid_i = int(aid)
        for tag, expr, cls in offsets:
            _put(regs, _eval_expr(expr, aID=aid_i), tag, cls, "metalset")
    for match in ITEM_PIPES.finditer(text):
        base = int(match.group(1))
        for offset in (2, 3, 4, 5, 6, 7):
            _put(
                regs,
                base + offset,
                "Item Pipes",
                "MultiTileEntityPipeItem",
                "connectors",
            )
    for match in FLUID_PIPES.finditer(text):
        base = int(match.group(1))
        for offset in range(7):
            _put(
                regs,
                base + offset,
                "Fluid Pipes",
                "MultiTileEntityPipeFluid",
                "connectors",
            )
    for match in ELECTRIC_WIRES.finditer(text):
        base = int(match.group(1))
        cable = match.group(4) == "T"
        for offset in range(16):
            _put(
                regs,
                base + offset,
                "Electric Wires",
                "MultiTileEntityWireElectric",
                "connectors",
            )
        if cable:
            for offset in (16, 17, 19, 23, 27):
                _put(
                    regs,
                    base + offset,
                    "Electric Wires",
                    "MultiTileEntityWireElectric",
                    "connectors",
                )
    methods = list(METHOD_START.finditer(text))
    bodies: list[tuple[str, str]] = [
        ("run", text[: methods[0].start()] if methods else text)
    ]
    for index, method in enumerate(methods):
        name = method.group(1)
        end = methods[index + 1].start() if index + 1 < len(methods) else len(text)
        bodies.append((name, text[method.start() : end]))
    for method_name, body in bodies:
        if method_name == "metalset":
            continue
        current = "unknown"
        search_from = 0
        while True:
            head = FOR_HEAD.search(body, search_from)
            if head is None:
                break
            limit = int(head.group(1))
            start = head.end()
            if start < len(body) and body[start] == "{":
                depth = 0
                index = start
                while index < len(body):
                    if body[index] == "{":
                        depth += 1
                    elif body[index] == "}":
                        depth -= 1
                        if depth == 0:
                            index += 1
                            break
                    index += 1
                loop_body = body[start:index]
                search_from = index
            else:
                newline = body.find("\n", start)
                loop_body = body[start:] if newline < 0 else body[start:newline]
                search_from = start + len(loop_body)
            loop_class = _class_from(loop_body, current)
            assigned = list(ACLASS_ASSIGN.finditer(loop_body))
            if assigned:
                loop_class = assigned[-1].group(1)
            for tag_match in LOOP_TAG.finditer(loop_body):
                tag, expr, _creative = tag_match.groups()
                if "i" not in expr.replace(" ", ""):
                    continue
                cls = _class_from(
                    loop_body[tag_match.start() : tag_match.start() + 240],
                    loop_class,
                )
                for index in range(limit):
                    _put(
                        regs,
                        _eval_expr(expr, i=index),
                        tag,
                        cls,
                        method_name,
                    )
        for line in body.splitlines():
            assigned = ACLASS_ASSIGN.search(line)
            if assigned:
                current = assigned.group(1)
            if "aRegistry.add(" not in line:
                continue
            match = TAG_ID.search(line)
            if not match:
                continue
            tag, mid, _creative = match.groups()
            _put(regs, int(mid), tag, _class_from(line, current), method_name)
    return regs


def _family_for(tag: str, gt6_class: str) -> str:
    if gt6_class in ATTACHMENT_CLASS_FAMILY:
        return ATTACHMENT_CLASS_FAMILY[gt6_class]
    if tag in TAG_FAMILY:
        return TAG_FAMILY[tag]
    slug = re.sub(r"[^a-z0-9]+", "_", tag.lower()).strip("_")
    return slug or "unmatched"


def _has_processing_kind(english_name: str, tokens: tuple[str, ...]) -> bool:
    lowered = english_name.lower()
    for token in tokens:
        if re.search(rf"\b{re.escape(token)}\b", lowered):
            return True
    return False


def classify_identity(
    ident: dict[str, Any],
    mapping: dict[str, str] | None,
    tokens: tuple[str, ...],
) -> dict[str, str]:
    meta = int(ident["meta"])
    english = str(ident["english_name"])
    tag = mapping["tag"] if mapping else "unmatched"
    gt6_class = mapping["gt6_class"] if mapping else "unmatched"
    method = mapping["method"] if mapping else "unmatched"
    family = _family_for(tag, gt6_class)
    evidence = (
        f"Loader_MultiTileEntities.java {method} {gt6_class} / {tag}"
        if mapping
        else "Loader 无此 id; catalog dump identity"
    )
    if not mapping:
        if english == "Wooden Panel":
            family = "decorative"
            tag = "Panels"
            gt6_class = "MultiTileEntityPanelWood"
            evidence = "catalog english_name Wooden Panel; Loader loop hidden-plank subset"
        else:
            family = "untyped"
    if ident.get("acquisition_authority") == "bath_mte":
        return {
            "disposition": "realized_natively",
            "evidence": "BathMteIdentityCatalog",
            "family": family,
            "gt6_class": gt6_class,
            "tag": tag,
        }
    if meta == DUST_FUNNEL_META:
        return {
            "disposition": "realized_natively",
            "evidence": "DustFunnelBlock / steel_dust_funnel",
            "family": "hopper",
            "gt6_class": "MultiTileEntityDustFunnel",
            "tag": tag,
        }
    if meta in ATTACHMENT_METAS:
        return {
            "disposition": "attachment_candidate",
            "evidence": evidence,
            "family": "fluid_attachment",
            "gt6_class": gt6_class,
            "tag": tag,
        }
    if tag in CRUCIBLE_TAGS or gt6_class in {
        "MultiTileEntitySmeltery",
        "MultiTileEntityMold",
        "MultiTileEntityBasin",
        "MultiTileEntityCrossing",
    }:
        return {
            "disposition": "realized_natively",
            "evidence": "CrucibleBlock / CeramicMoldBlock adjacent pour",
            "family": "crucible_foundry",
            "gt6_class": gt6_class,
            "tag": tag,
        }
    if tag == "Hoppers" or gt6_class in {
        "MultiTileEntityHopper",
        "MultiTileEntityQueueHopper",
    }:
        return {
            "disposition": "realized_natively",
            "evidence": "HopperBlock",
            "family": "hopper",
            "gt6_class": gt6_class,
            "tag": tag,
        }
    if tag == "Reactors" or "Reactor" in gt6_class:
        return {
            "disposition": "realized_natively",
            "evidence": "energy/nuclear-fission-survival",
            "family": "reactor",
            "gt6_class": gt6_class,
            "tag": tag,
        }
    if tag == "Transformers":
        return {
            "disposition": "realized_natively",
            "evidence": "energy/transformers",
            "family": "energy_converter",
            "gt6_class": gt6_class,
            "tag": tag,
        }
    if tag in {"Basic Machines", "Automatic Tools"} and _has_processing_kind(
        english, tokens
    ):
        return {
            "disposition": "realized_natively",
            "evidence": "machine_tiers.json / ModProcessingMachines",
            "family": "processing_machine",
            "gt6_class": gt6_class,
            "tag": tag,
        }
    if tag in FURNITURE_TAGS or tag in IDENTITY_TAGS or family in {
        "furniture_storage",
        "drive",
        "decorative",
        "connector",
        "redstone_wire",
        "extender",
        "logistics",
        "sensor",
        "computer",
        "untyped",
        "misc_tool",
        "multiblock",
        "energy_converter",
        "processing_machine",
    }:
        return {
            "disposition": "identity_only",
            "evidence": evidence,
            "family": family,
            "gt6_class": gt6_class,
            "tag": tag,
        }
    return {
        "disposition": "identity_only",
        "evidence": evidence,
        "family": family,
        "gt6_class": gt6_class,
        "tag": tag,
    }


def build_ledger() -> list[dict[str, Any]]:
    identities = catalog_identities()
    regs = parse_loader_registry()
    tokens = processing_kind_tokens()
    rows: list[dict[str, Any]] = []
    for ident in identities:
        meta = int(ident["meta"])
        classified = classify_identity(ident, regs.get(meta), tokens)
        rows.append(
            {
                "disposition": classified["disposition"],
                "english_name": ident["english_name"],
                "evidence": classified["evidence"],
                "family": classified["family"],
                "gt6_class_or_tag": f"{classified['gt6_class']} / {classified['tag']}",
                "meta": meta,
                "registry_path": ident["registry_path"],
            }
        )
    rows.sort(key=lambda row: int(row["meta"]))
    return rows


def _gt6_faucet_tag_count(regs: dict[int, dict[str, str]]) -> int:
    return sum(1 for row in regs.values() if row["tag"] == "Crucibles Faucets")


def build_r0_documents() -> dict[str, Any]:
    identities = catalog_identities()
    ledger = build_ledger()
    regs = parse_loader_registry()
    capability_row = identity_capability_row()
    hashes = authority_hashes()
    leftover = leftover_later_count()
    scatter = sum(
        1
        for row in identities
        if row.get("acquisition_authority") == "smelter_mte_scatter"
    )
    bath = sum(
        1 for row in identities if row.get("acquisition_authority") == "bath_mte"
    )
    family_counts = Counter(row["family"] for row in ledger)
    disposition_counts = Counter(row["disposition"] for row in ledger)
    unmatched = sum(1 for row in ledger if row["family"] == "unmatched")
    wooden = sum(1 for row in ledger if row["english_name"] == "Wooden Panel")
    drive = sum(1 for row in ledger if row["family"] == "drive")
    attachments = [row for row in ledger if row["disposition"] == "attachment_candidate"]
    dust = next(row for row in ledger if int(row["meta"]) == DUST_FUNNEL_META)
    if scatter != EXPECTED_SCATTER or bath != EXPECTED_BATH:
        raise ValueError(f"scatter/bath {scatter}/{bath} drifted")
    if unmatched != 0:
        raise ValueError(f"unmatched metas {unmatched} != 0")
    if any(not row["disposition"] for row in ledger):
        raise ValueError("empty dispositions")
    if len(attachments) != EXPECTED_ATTACHMENTS:
        raise ValueError(
            f"attachment_candidate {len(attachments)} != {EXPECTED_ATTACHMENTS}"
        )
    faucet_n = sum(1 for row in attachments if int(row["meta"]) in FAUCET_METAS)
    tap_n = sum(1 for row in attachments if int(row["meta"]) in TAP_METAS)
    nozzle_n = sum(1 for row in attachments if int(row["meta"]) in NOZZLE_METAS)
    cap_n = sum(1 for row in attachments if int(row["meta"]) in CAP_NOZZLE_METAS)
    funnel_n = sum(1 for row in attachments if int(row["meta"]) in FLUID_FUNNEL_METAS)
    if (faucet_n, tap_n, nozzle_n, cap_n, funnel_n) != (22, 3, 2, 3, 3):
        raise ValueError("attachment subset counts drifted")
    if dust["disposition"] != "realized_natively":
        raise ValueError("dust funnel must be realized_natively")
    if wooden != EXPECTED_WOODEN_PANELS:
        raise ValueError(f"wooden_panel {wooden} != {EXPECTED_WOODEN_PANELS}")
    if drive != EXPECTED_DRIVE:
        raise ValueError(f"axle+gearbox {drive} != {EXPECTED_DRIVE}")
    if nuclear_started():
        raise ValueError("nuclear_started must stay false")
    family_tags: dict[str, set[str]] = {}
    drive_classes: Counter[str] = Counter()
    for row in ledger:
        tag = row["gt6_class_or_tag"].split(" / ", 1)[-1]
        family_tags.setdefault(row["family"], set()).add(tag)
        if row["family"] == "drive":
            drive_classes[row["gt6_class_or_tag"].split(" / ", 1)[0]] += 1
    common = {
        "generated_by": GENERATED_BY,
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "wave_slug": SLUG,
    }
    inherited = {
        **common,
        "acquisition": {
            "bath_mte": bath,
            "smelter_mte_scatter": scatter,
        },
        "authority_hashes": hashes,
        "capability_row": capability_row,
        "catalog_identity_count": EXPECTED_IDENTITIES,
        "kind": "mte_item",
        "registry_path_authority": "tools/catalog_modern_id_map.json",
        "registry_path_key": "meta",
        "registry_path_pattern": "semantic registry_path; numbered tail forbidden",
        "smelter_mte_identity_status": PREDECESSOR_STATUS,
        "status": "INHERITED_DENOMINATOR_READY",
    }
    semantics = {
        **common,
        "catalog_denominator": EXPECTED_IDENTITIES,
        "context_not_rows": {
            "gt6_cap_nozzle_registration": 6,
            "gt6_faucet_tag_registration": _gt6_faucet_tag_count(regs),
            "gt6_fluid_funnel_registration": 6,
            "gt6_fluid_nozzle_registration": 6,
            "gt6_fluid_tap_registration": 6,
            "note": (
                "Exclusion-table Crucibles Faucets 39 and GT6 Misc Tool Block "
                "registration counts are Loader context. This card freezes only "
                "the 1,817 catalog identities. Uncatalogued GT6 variants get no "
                "ledger row."
            ),
        },
        "identity_is_not_behavior": True,
        "status": "SOURCE_SEMANTICS_READY",
        "uncatalogued_gt6_get_no_row": True,
    }
    family_map = {
        **common,
        "families": [
            {
                "count": int(family_counts[name]),
                "family": name,
                "gt6_tags": sorted(family_tags[name]),
            }
            for name in sorted(family_counts)
        ],
        "identity_count": EXPECTED_IDENTITIES,
        "status": "FAMILY_MAP_READY",
        "unmatched": 0,
        "wooden_panel_count": wooden,
        "wooden_panel_counted_once": True,
        "drive_tag_breakdown": {
            "by_class": dict(drive_classes),
            "catalog_rows": drive,
            "gt6_tag": "Axles and Gearboxes",
            "note": (
                "Preread Axle 36 + Gearbox 18 = 54 counted names. Live catalog "
                "rows with Loader tag Axles and Gearboxes stay one family. "
                "identity_only because CrucibleCraft rotation is a different object."
            ),
        },
    }
    disposition_ledger = {
        **common,
        "attachment_candidate": EXPECTED_ATTACHMENTS,
        "attachment_subsets": {
            "cap_nozzle": cap_n,
            "fluid_funnel": funnel_n,
            "fluid_nozzle": nozzle_n,
            "fluid_tap": tap_n,
            "faucet": faucet_n,
        },
        "axle_gearbox_count": drive,
        "counts": {
            name: int(disposition_counts[name]) for name in DISPOSITIONS
        },
        "dust_funnel": {
            "disposition": dust["disposition"],
            "evidence": dust["evidence"],
            "meta": DUST_FUNNEL_META,
        },
        "empty_dispositions": 0,
        "identities": ledger,
        "identity_count": EXPECTED_IDENTITIES,
        "status": "DISPOSITION_LEDGER_READY",
        "unmatched": 0,
        "wooden_panel_count": wooden,
    }
    mechanism = {
        **common,
        "kinds": [
            {
                "covers": "1,817 holdable CatalogNamedItem identities",
                "does_not_cover": "faucet / tap / nozzle / fluid-funnel behavior",
                "mechanism": "SmelterMteIdentityCatalog / BathMteIdentityCatalog",
            },
            {
                "covers": "closed storage/lock 28/624 registration",
                "does_not_cover": "folding chest scatter back onto native blocks",
                "mechanism": "storage/lock",
            },
            {
                "covers": "named ProcessingMachine / converter hosts",
                "does_not_cover": "scatter MTE item is not that live block",
                "mechanism": "machine_tiers.json / ModProcessingMachines",
            },
            {
                "covers": "ceramic crucible + CeramicMoldBlock adjacent pour",
                "does_not_cover": "GT6 AttachmentSmall crucible faucet",
                "mechanism": "CrucibleBlock / CeramicMoldBlock",
            },
            {
                "covers": "steel_dust_funnel dust insertion",
                "does_not_cover": "item barrels or blockDust",
                "mechanism": "DustFunnelBlock",
            },
            {
                "covers": "independent KU/RU shafts",
                "does_not_cover": "MultiTileEntityAxle / GearBox identities",
                "mechanism": "rotation energy",
            },
            {
                "covers": "pipe cover behaviors",
                "does_not_cover": "TileEntityBase11AttachmentSmall placement",
                "mechanism": "CoverBehaviorRegistry",
            },
            {
                "covers": "cell fill/drain later clue",
                "does_not_cover": "this card implementing funnels; Canner is out",
                "mechanism": "CellItem / CellFluidHandler",
            },
            {
                "covers": "world gas-cloud landing clue",
                "does_not_cover": "this card implementing nozzles",
                "mechanism": "GasCloudBlock",
            },
        ],
        "status": "EXISTING_MECHANISM_READY",
    }
    contract = {
        **common,
        "allows_implementation_child": False,
        "questions": [
            {
                "id": "host_bounds",
                "must_answer": (
                    "Which BEs implement tapDrain / nozzleDrain / funnelFill / "
                    "crucible pour. GT6 also hits barrels, basic machines, "
                    "reactor cores, hot-fluid machines, and multiblock tanks."
                ),
            },
            {
                "id": "placement",
                "must_answer": (
                    "AttachmentSmall face placement vs pipe covers. "
                    "CoverBehaviorRegistry must not be used as completion."
                ),
            },
            {
                "id": "four_behaviors",
                "must_answer": (
                    "Crucible faucet, fluid tap, nozzle, and fluid funnel are "
                    "four settlements sharing placement, not one drain interface."
                ),
            },
            {
                "id": "uncatalogued_gt6",
                "must_answer": (
                    "Whether to register variants absent from this catalog "
                    "(ceramic faucet 1705 and similar). Registration is "
                    "new_distinct, not a rewrite of these 1,817 rows."
                ),
            },
            {
                "id": "player_acquisition",
                "must_answer": "Survival path for the 33 registered identities.",
            },
            {
                "id": "fail_closed",
                "must_answer": "Unknown attachment variants must not silent no-op.",
            },
            {
                "id": "delete_later",
                "must_answer": (
                    "How to keep this card's hashes if a later card marks one "
                    "behavior out_of_scope."
                ),
            },
        ],
        "status": "ATTACHMENT_CONTRACT_READY",
    }
    reasons = [
        "The 33 catalog identities remain behaviorless CatalogNamedItem rows.",
        "CrucibleCraft has no ITileEntityTapAccessible / ITileEntityFunnelAccessible / faucet face placement.",
        "Adjacent mold pour already exists, so the gap is attachment behavior, not a casting RecipeMap.",
        "CoverBehaviorRegistry is not bounded_extension evidence.",
    ]
    feasibility = {
        **common,
        "allows_implementation_child": False,
        "categories": [
            {
                "allows_implementation_child": False,
                "category": name,
                "count": count,
                "destination": None,
                "missing_evidence": [],
                "reasons": reasons,
                "verdict": "requires_new_runtime",
            }
            for name, count in (
                ("crucible_faucet", 22),
                ("fluid_tap", 3),
                ("fluid_nozzle", 2),
                ("cap_nozzle", 3),
                ("fluid_funnel", 3),
            )
        ],
        "status": "FEASIBILITY_READY",
        "subset": "catalog_fluid_attachments",
        "verdict": "requires_new_runtime",
    }
    documents = {
        "wave.json": {
            "cohort": "mte-identity-disposition-r0",
            "depends_on": [PREDECESSOR],
            "generated_by": GENERATED_BY,
            "owns_families": 0,
            "program": SLUG,
            "schema_version": 1,
            "wave_slug": SLUG,
        },
        "census_delta.json": {
            "complete_family_count": 0,
            "completion_delta": 0,
            "generated_by": GENERATED_BY,
            "leftover_later_count": leftover,
            "partial_family_count": 0,
            "remaining_recipe_gap": 0,
            "schema_version": 1,
            "source_revision": SOURCE_REVISION,
            "status": "CENSUS_DELTA_READY",
            "wave_slug": SLUG,
            "work_set": {"family_count": 0, "source_rows": 0},
        },
        "topology.json": {
            "append_only": False,
            "complete_family_count": 0,
            "generated_by": GENERATED_BY,
            "next_unassigned": True,
            "remaining_recipe_gap": 0,
            "schema_version": 1,
            "source_revision": SOURCE_REVISION,
            "status": "WAVE_READY",
            "unique_active_wave": None,
            "wave_slug": SLUG,
        },
        "readiness.json": {
            "evidence": {
                "allows_implementation_child": False,
                "attachment_candidate": EXPECTED_ATTACHMENTS,
                "axle_gearbox_count": drive,
                "bath_mte": bath,
                "catalog_identities": EXPECTED_IDENTITIES,
                "completion_delta": 0,
                "dust_funnel_realized_natively": True,
                "empty_dispositions": 0,
                "feasibility_verdict": "requires_new_runtime",
                "generated_recipe_count": 0,
                "leftover_later_count": leftover,
                "nuclear_track_c_started": False,
                "owns_families": 0,
                "partial_family_count": 0,
                "production_lock": None,
                "recipe_files_generated": False,
                "smelter_mte_scatter": scatter,
                "unmatched": 0,
                "wooden_panel_count": wooden,
            },
            "generated_by": GENERATED_BY,
            "next_unassigned": True,
            "note": (
                "MTE_IDENTITY_DISPOSITION_R0_READY. Catalog 1,817 identities "
                "have family and disposition. Catalog fluid attachments (33) "
                "are requires_new_runtime. The game still has no faucet / "
                "nozzle / fluid funnel / crucible-faucet attachment; adjacent "
                "mold pour remains the existing simplified path. "
                "unique_active_wave is null."
            ),
            "schema_version": 1,
            "source_revision": SOURCE_REVISION,
            "status": STATUS,
            "unique_active_wave": None,
            "wave_complete": True,
            "wave_slug": SLUG,
        },
    }
    documents["inherited_denominator.json"] = inherited
    documents["source_semantics.json"] = semantics
    documents["family_map.json"] = family_map
    documents["disposition_ledger.json"] = disposition_ledger
    documents["existing_mechanism.json"] = mechanism
    documents["attachment_contract.json"] = contract
    documents["feasibility.json"] = feasibility
    return documents


def write_seal() -> dict[str, Any]:
    root = wave_dir(SLUG)
    hashes = {
        "census": census.sha256_file(root / "census_delta.json"),
        "gametest_java": None,
        "gametest_log": None,
        "generated_recipes": None,
        "locked_support": None,
        "production_lock": None,
        "publication_group_manifest": None,
        "readiness": census.sha256_file(root / "readiness.json"),
        "receipt": None,
        "runtime_dependency_manifest": None,
        "shard_manifest": None,
        "topology": census.sha256_file(root / "topology.json"),
    }
    seal = {
        "card_id": SLUG,
        "complete_family_count": 0,
        "composed_identity_ledger_v2_sha256": census.sha256_file(
            closeout_seal.IDENTITY_LEDGER_V2
        ),
        "composed_runtime_manifest_v2_sha256": census.sha256_file(
            closeout_seal.RUNTIME_MANIFEST_V2
        ),
        "gametest_status": "NONE",
        "generated_by": f"{GENERATED_BY} --write",
        "hashes": hashes,
        "note": census.load_json(root / "readiness.json").get("note"),
        "production_lock_sha256": None,
        "receipt_sha256": None,
        "reclassification_delta": 0,
        "relation_count": 0,
        "remaining_recipe_gap": 0,
        "schema_version": 1,
        "sealed_at_wave": SLUG,
        "source_revision": SOURCE_REVISION,
        "status": "SEALED",
    }
    census.write_stable(root / "closeout_seal.json", seal)
    return seal


def write_artifacts() -> dict[str, Any]:
    documents = build_r0_documents()
    root = wave_dir(SLUG)
    root.mkdir(parents=True, exist_ok=True)
    for name, document in documents.items():
        census.write_stable(root / name, document)
    write_seal()
    spec = spec_for(SLUG)
    return {
        "status": STATUS,
        "unique_active_wave": spec.unique_active_wave,
        "wave_slug": SLUG,
    }


def check_forbidden_successors(haystack: str) -> list[str]:
    errors: list[str] = []
    for token in FORBIDDEN_SUCCESSORS:
        if token in haystack:
            errors.append(f"topology successor {token} is forbidden")
    return errors


def check_artifacts() -> list[str]:
    errors = require_registered(SLUG)
    spec = spec_for(SLUG)
    if spec.owns_families != 0:
        errors.append(f"{SLUG} owns_families must be 0")
    if spec.production_lock is not None:
        errors.append(f"{SLUG} must not carry a production lock")
    if spec.unique_active_wave is not None:
        errors.append("unique_active_wave must be null")
    if not spec.next_unassigned:
        errors.append("next_unassigned must be true")
    if CAPABILITY_JSON.is_file():
        errors.append("capability.json must not be created")
    predecessor = census.load_json(wave_dir(PREDECESSOR) / "readiness.json")
    if predecessor.get("status") != PREDECESSOR_STATUS:
        errors.append(f"{PREDECESSOR} status drifted")
    errors.extend(closeout_seal.check_wave_seal(PREDECESSOR))
    root = wave_dir(SLUG)
    if not (root / "readiness.json").is_file():
        return errors + [f"{SLUG} artifacts are missing"]
    try:
        live = build_r0_documents()
    except ValueError as error:
        return errors + [str(error)]
    for name, document in live.items():
        committed = census.load_json(root / name)
        drift = census.first_json_diff(document, committed)
        if drift:
            errors.append(f"{name} drifted: {drift}")
    readiness = census.load_json(root / "readiness.json")
    if readiness.get("status") != STATUS:
        errors.append(f"{SLUG} status drifted")
    if readiness.get("unique_active_wave") is not None:
        errors.append(f"{SLUG} unique_active_wave must be null")
    if nuclear_started():
        errors.append("nuclear Track C started must stay false")
    evidence = readiness.get("evidence") or {}
    if int(evidence.get("completion_delta", 1)) != 0:
        errors.append(f"{SLUG} completion_delta must be 0")
    if evidence.get("recipe_files_generated"):
        errors.append(f"{SLUG} must not generate recipes")
    if int(evidence.get("catalog_identities", 0)) != EXPECTED_IDENTITIES:
        errors.append("catalog identities must be 1817")
    if int(evidence.get("attachment_candidate", 0)) != EXPECTED_ATTACHMENTS:
        errors.append("attachment_candidate must be 33")
    if int(evidence.get("unmatched", 1)) != 0:
        errors.append("unmatched must be 0")
    if int(evidence.get("empty_dispositions", 1)) != 0:
        errors.append("empty dispositions must be 0")
    if int(evidence.get("wooden_panel_count", 0)) != EXPECTED_WOODEN_PANELS:
        errors.append("wooden_panel must be 23")
    if int(evidence.get("axle_gearbox_count", 0)) != EXPECTED_DRIVE:
        errors.append("axle+gearbox must be 54")
    if not evidence.get("dust_funnel_realized_natively"):
        errors.append("dust funnel must be realized_natively")
    if evidence.get("allows_implementation_child"):
        errors.append("allows_implementation_child must be false")
    topology = census.load_json(root / "topology.json")
    extra = sorted(set(topology) - set(ALLOWED_TOPOLOGY_KEYS))
    if extra:
        errors.append(f"topology has forbidden keys: {extra}")
    errors.extend(check_forbidden_successors(json.dumps(topology, sort_keys=True)))
    feasibility = census.load_json(root / "feasibility.json")
    if feasibility.get("verdict") != "requires_new_runtime":
        errors.append("feasibility verdict must be requires_new_runtime")
    if feasibility.get("allows_implementation_child"):
        errors.append("feasibility must not allow an implementation child")
    inherited = census.load_json(root / "inherited_denominator.json")
    drift = census.first_json_diff(
        identity_capability_row(), inherited.get("capability_row")
    )
    if drift:
        errors.append(f"capability-map identity row drifted: {drift}")
    live_hashes = authority_hashes()
    stored = inherited.get("authority_hashes") or {}
    for key, digest in live_hashes.items():
        if stored.get(key) != digest:
            errors.append(f"authority hash {key} drifted")
    if stored.get("growth_order") != live_hashes["growth_order"]:
        errors.append("growth_order hash must stay unchanged")
    if stored.get("smelter_mte_identity_seal") != live_hashes[
        "smelter_mte_identity_seal"
    ]:
        errors.append("SMELTER_MTE_IDENTITY_READY seal drifted")
    errors.extend(closeout_seal.check_wave_seal(SLUG))
    return errors


def main_for(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=f"Write or check {SLUG}.")
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args(argv)
    if args.write == args.check:
        parser.error("choose --write or --check")
    try:
        if args.write:
            print(json.dumps(write_artifacts(), sort_keys=True))
            return 0
        errors = check_artifacts()
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        print(f"{SLUG} closeout derivation is current")
        return 0
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"{SLUG} failed: {error}", file=sys.stderr)
        return 1
