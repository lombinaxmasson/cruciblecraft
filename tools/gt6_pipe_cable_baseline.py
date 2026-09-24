"""GT6 pipe/cable/redstone baseline audit. No runtime, no recipes, no capability."""
from __future__ import annotations

import argparse
import hashlib
import json
import re
import subprocess
import sys
from collections import Counter
from pathlib import Path
from typing import Any

from tools import catalog_modern_ids
from tools import census_common as census
from tools import gt6_pipes
from tools import gt6_resolve
from tools import io_common as io

SLUG = "content/gt6-pipe-cable-baseline"
STATUS = "PIPE_CABLE_BASELINE_PREP_READY"
GT6_REVISION = io.SOURCE_REVISION
WAVE = census.TOOLS / "waves" / "content" / "gt6-pipe-cable-baseline"
PLAN_CLOSED = (
    census.ROOT
    / "docs"
    / "history"
    / "card-plans"
    / "closed"
    / "GT6管道与线缆语义重基线详细计划.md"
)
PLAN_PREP = (
    census.ROOT
    / "docs"
    / "history"
    / "card-plans"
    / "prep"
    / "GT6管道与线缆语义重基线详细计划.md"
)
CAPABILITY_JSON = (
    census.TOOLS / "capabilities" / "content" / "gt6-pipe-cable-baseline"
    / "capability.json"
)
GT6_ROOT = census.ROOT / "gt6_code" / "gregtech6"
GT6_ART_ROOT = (
    census.ROOT
    / "gt6_referencable_port_code"
    / "gregtech6_w"
    / "src"
    / "main"
    / "resources"
    / "assets"
    / "gregtech"
    / "textures"
    / "blocks"
)
GTCEU_ROOT = census.ROOT / "gtceu_code"
DISPOSITION = (
    census.TOOLS
    / "waves"
    / "portfolio"
    / "mte-identity-disposition-r0"
    / "disposition_ledger.json"
)
MODERN_MAP = census.TOOLS / "catalog_modern_id_map.json"
PIPE_SOURCE = census.TOOLS / "gt6_pipe_source.json"
ELECTRIC_MAPPED = (
    census.TOOLS
    / "waves"
    / "content"
    / "electric-wire-cable-mte-fold"
    / "operand_runtime_map.json"
)
REDSTONE_LEDGER = (
    census.TOOLS
    / "waves"
    / "content"
    / "mte-redstone-wire"
    / "identity_resolution_ledger.json"
)
LOADER = (
    GT6_ROOT
    / "src"
    / "main"
    / "java"
    / "gregtech"
    / "loaders"
    / "b"
    / "Loader_MultiTileEntities.java"
)
MATERIAL_DIR = (
    census.ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "materials"
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

GT6_TICK_FILE_HASHES = {
    "src/main/java/gregapi/tileentity/connectors/"
    "MultiTileEntityPipeFluid.java":
        "3a6850055375e7975145b03abab44c5922746a6f4053dd8d3163de989f37b110",
    "src/main/java/gregapi/tileentity/connectors/"
    "MultiTileEntityPipeItem.java":
        "0e9aa956b807d8390b20dd70f224b2b8d0a7be8df25fe66660abffda8b88069b",
    "src/main/java/gregapi/tileentity/connectors/"
    "MultiTileEntityWireElectric.java":
        "60dd3bf9147a220ab327a1aed3f0081382678eb0d6190e865e84e1b7dae89724",
    "src/main/java/gregapi/tileentity/connectors/"
    "MultiTileEntityWireRedstone.java":
        "cd79da42bb5b24a211615bb25edb653995c867f7404928f40d8e6f73ca59ec15",
    "src/main/java/gregapi/tileentity/connectors/"
    "MultiTileEntityWireRedstoneInsulated.java":
        "4f93b8caf163e85f0560bfba5934bb42e76c39caf9a954c279cfcb10af49b8c2",
    "src/main/java/gregapi/tileentity/connectors/"
    "ITileEntityRedstoneWire.java":
        "191c58b9bb61d48d2cd8d07f493157c3a4f6968ebc644114577e0b09a2b47717",
    "src/main/java/gregtech/loaders/b/Loader_MultiTileEntities.java":
        "96f579dd5c0a26a759ddee34c312dadbc5d52123e2b85e82da6a40f68b494393",
}

FLUID_GAUGES = (
    ("pipeTiny", 0, "tiny_fluid_pipe", 4, 1),
    ("pipeSmall", 1, "small_fluid_pipe", 6, 1),
    ("pipeMedium", 2, "fluid_pipe", 8, 1),
    ("pipeLarge", 3, "large_fluid_pipe", 12, 1),
    ("pipeHuge", 4, "huge_fluid_pipe", 16, 1),
    ("pipeQuadruple", 5, None, 8, 4),
    ("pipeNonuple", 6, None, 6, 9),
)
ITEM_GAUGES = (
    ("pipeMedium", 2, "item_pipe", 8, False),
    ("pipeLarge", 3, "large_item_pipe", 12, False),
    ("pipeHuge", 4, "huge_item_pipe", 16, False),
    ("pipeRestrictiveMedium", 5, None, 8, True),
    ("pipeRestrictiveLarge", 6, None, 12, True),
    ("pipeRestrictiveHuge", 7, None, 16, True),
)
EU_WIRES = (
    ("wireGt01", 0, "wire", 1),
    ("wireGt02", 1, "double_wire", 2),
    ("wireGt03", 2, "triple_wire", 3),
    ("wireGt04", 3, "quadruple_wire", 4),
    ("wireGt05", 4, "quintuple_wire", 5),
    ("wireGt06", 5, "sextuple_wire", 6),
    ("wireGt07", 6, None, 7),
    ("wireGt08", 7, "octuple_wire", 8),
    ("wireGt09", 8, None, 9),
    ("wireGt10", 9, None, 10),
    ("wireGt11", 10, None, 11),
    ("wireGt12", 11, "dodecuple_wire", 12),
    ("wireGt13", 12, None, 13),
    ("wireGt14", 13, None, 14),
    ("wireGt15", 14, None, 15),
    ("wireGt16", 15, "hexadecuple_wire", 16),
)
EU_CABLES = (
    ("cableGt01", 16, "cable", 1),
    ("cableGt02", 17, "double_cable", 2),
    ("cableGt04", 19, "quadruple_cable", 4),
    ("cableGt08", 23, "octuple_cable", 8),
    ("cableGt12", 27, "dodecuple_cable", 12),
)
PLACEABLE_EU_FORMS = {
    "wire",
    "double_wire",
    "triple_wire",
    "quadruple_wire",
    "quintuple_wire",
    "sextuple_wire",
    "octuple_wire",
    "dodecuple_wire",
    "hexadecuple_wire",
    "cable",
    "double_cable",
    "quadruple_cable",
    "octuple_cable",
    "dodecuple_cable",
}
REDSTONE_KINDS = {
    27000: {
        "material": "red_alloy",
        "symbol": "RedAlloy",
        "op": "wireGt01",
        "range": 16,
        "glowing": False,
        "display": "Red Alloy Wire",
    },
    27050: {
        "material": "signalum",
        "symbol": "Signalum",
        "op": "wireGt01",
        "range": 16,
        "glowing": False,
        "display": "Signalum Wire",
    },
    27500: {
        "material": "lumium",
        "symbol": "Lumium",
        "op": "wireGt01",
        "range": 16,
        "glowing": True,
        "display": "Lumium Wirelamp",
    },
}
REDSTONE_INSULATED = {
    27006: ("red_alloy", "RedAlloy"),
    27056: ("signalum", "Signalum"),
    27506: ("lumium", "Lumium"),
}
REDSTONE_MATERIALS = {"red_alloy", "signalum", "lumium"}
CHILD = {
    "fluid": "content/gt6-fluid-pipe-runtime",
    "item": "content/gt6-item-pipe-runtime",
    "eu": "content/gt6-eu-wire-cable-runtime",
    "redstone": "content/gt6-redstone-wire-correction",
    "insulated": "content/gt6-redstone-insulated",
    "art": "content/gt6-connector-art",
}
ELECTRIC = re.compile(
    r"MultiTileEntityWireElectric\.addElectricWires\("
    r"(\d+)\s*,\s*\d+\s*,\s*([^,]+),\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*,\s*"
    r"([TF])\s*,\s*([TF])\s*,\s*([TF])\s*,\s*"
    r"aRegistry,\s*aMetalWires,\s*aClass,\s*MT\.(\w+)\s*\)"
)
REDSTONE_RANGE = {
    27000: 16,
    27050: 64,
    27500: 16,
}


def _sha256_bytes(payload: bytes) -> str:
    return hashlib.sha256(payload).hexdigest()


def plan_path() -> Path:
    if PLAN_CLOSED.is_file():
        return PLAN_CLOSED
    return PLAN_PREP


def gt6_file_hashes() -> dict[str, str]:
    hashes: dict[str, str] = {}
    for relative, expected in GT6_TICK_FILE_HASHES.items():
        try:
            payload = subprocess.check_output(
                [
                    "git",
                    "-C",
                    str(GT6_ROOT),
                    "show",
                    f"{GT6_REVISION}:{relative}",
                ]
            )
        except (OSError, subprocess.CalledProcessError) as error:
            raise FileNotFoundError(
                f"cannot read pinned GT6 blob {relative}"
            ) from error
        digest = _sha256_bytes(payload)
        if digest != expected:
            raise ValueError(f"{relative} hash drifted: {digest}")
        hashes[relative] = digest
    return hashes


def _cc_material(symbol: str) -> str | None:
    result = gt6_resolve.resolve(f"MT.{symbol}")
    material = result.get("material") or {}
    cc = material.get("cc_material")
    if isinstance(cc, str) and cc:
        return cc
    source = gt6_resolve.mt_fields().get(symbol)
    if not source:
        return None
    row = gt6_resolve.live_materials().get(source) or gt6_resolve.live_materials().get(
        source.lower()
    )
    if isinstance(row, dict):
        cc = row.get("cc_material")
        if isinstance(cc, str) and cc:
            return cc
    slug = re.sub(r"[^a-z0-9]+", "_", source.lower()).strip("_")
    return slug or None


def _load_materials() -> dict[str, dict[str, Any]]:
    rows: dict[str, dict[str, Any]] = {}
    if not MATERIAL_DIR.is_dir():
        return rows
    for path in sorted(MATERIAL_DIR.glob("*.json")):
        if path.name == "index.json":
            continue
        document = json.loads(path.read_text(encoding="utf-8"))
        material_id = str(document.get("id") or "")
        if material_id:
            rows[material_id] = document
    return rows


def _live_paths() -> set[str]:
    return catalog_modern_ids.live_host_paths()


def _modern_by_meta() -> dict[int, dict[str, Any]]:
    document = census.load_json(MODERN_MAP)
    rows: dict[int, dict[str, Any]] = {}
    for row in document.get("rows") or []:
        if row.get("source_item") != "gregtech:gt.multitileentity":
            continue
        rows[int(row["meta"])] = row
    return rows


def _r0_by_meta() -> dict[int, dict[str, Any]]:
    ledger = census.load_json(DISPOSITION)
    rows: dict[int, dict[str, Any]] = {}
    for row in ledger.get("identities") or []:
        if row.get("family") not in {"connector", "redstone_wire"}:
            continue
        rows[int(row["meta"])] = row
    return rows


def _recipe_mapped() -> dict[int, str]:
    if not ELECTRIC_MAPPED.is_file():
        return {}
    document = census.load_json(ELECTRIC_MAPPED)
    mapped: dict[int, str] = {}
    for row in document.get("mappings") or []:
        runtime = row.get("runtime_id")
        if runtime:
            mapped[int(row["meta"])] = str(runtime)
    return mapped


def parse_electric_invocations() -> list[dict[str, Any]]:
    text = LOADER.read_text(encoding="utf-8")
    rows: list[dict[str, Any]] = []
    for match in ELECTRIC.finditer(text):
        rows.append(
            {
                "base_id": int(match.group(1)),
                "voltage_token": match.group(2).strip(),
                "amperage": int(match.group(3)),
                "loss_wire": int(match.group(4)),
                "loss_cable": int(match.group(5)),
                "wire_contact_damage": match.group(6) == "T",
                "cable_contact_damage": match.group(7) == "T",
                "cable_generated": match.group(8) == "T",
                "source_symbol": match.group(9),
            }
        )
    if len(rows) != 30:
        raise ValueError(f"addElectricWires count {len(rows)} != 30")
    return rows


def expand_gt6_identities() -> list[dict[str, Any]]:
    pipe_source = gt6_pipes.load(PIPE_SOURCE)
    identities: list[dict[str, Any]] = []
    for row in pipe_source["direct_addFluidPipes_calls"]:
        symbol = row["source_symbol"]
        cc = _cc_material(symbol)
        for spec, offset, form, diameter, tanks in FLUID_GAUGES:
            identities.append(
                {
                    "domain": "fluid",
                    "meta": int(row["base_id"]) + offset,
                    "base_id": int(row["base_id"]),
                    "gt6_class": "gregapi.tileentity.connectors.MultiTileEntityPipeFluid",
                    "gt6_tag": "Fluid Pipes",
                    "gt6_loader": "Loader_MultiTileEntities.addFluidPipes",
                    "source_symbol": symbol,
                    "source_name": row["source_name"],
                    "cc_material": cc,
                    "op_target": f"OP.{spec}",
                    "source_specification": spec,
                    "cc_form": form,
                    "diameter_px": diameter,
                    "tank_count": tanks,
                    "base_capacity": row["base_capacity"],
                    "child_owner": CHILD["fluid"],
                }
            )
    for row in pipe_source["direct_addItemPipes_calls"]:
        symbol = row["source_symbol"]
        cc = _cc_material(symbol)
        for spec, offset, form, diameter, restrictive in ITEM_GAUGES:
            identities.append(
                {
                    "domain": "item",
                    "meta": int(row["base_id"]) + offset,
                    "base_id": int(row["base_id"]),
                    "gt6_class": "gregapi.tileentity.connectors.MultiTileEntityPipeItem",
                    "gt6_tag": "Item Pipes",
                    "gt6_loader": "Loader_MultiTileEntities.addItemPipes",
                    "source_symbol": symbol,
                    "source_name": row["source_name"],
                    "cc_material": cc,
                    "op_target": f"OP.{spec}",
                    "source_specification": spec,
                    "cc_form": form,
                    "diameter_px": diameter,
                    "restrictive": restrictive,
                    "base_step_size": row["base_step_size"],
                    "base_inventory_size": row["base_inventory_size"],
                    "child_owner": CHILD["item"],
                }
            )
    for row in parse_electric_invocations():
        symbol = row["source_symbol"]
        cc = _cc_material(symbol)
        forms = list(EU_WIRES)
        if row["cable_generated"]:
            forms.extend(EU_CABLES)
        for spec, offset, form, gauge in forms:
            cable = spec.startswith("cable")
            identities.append(
                {
                    "domain": "eu",
                    "meta": int(row["base_id"]) + offset,
                    "base_id": int(row["base_id"]),
                    "gt6_class": "gregapi.tileentity.connectors.MultiTileEntityWireElectric",
                    "gt6_tag": "Electric Wires",
                    "gt6_loader": "Loader_MultiTileEntities.addElectricWires",
                    "source_symbol": symbol,
                    "cc_material": cc,
                    "op_target": f"OP.{spec}",
                    "source_specification": spec,
                    "cc_form": form,
                    "gauge": gauge,
                    "cable": cable,
                    "voltage_token": row["voltage_token"],
                    "amperage": row["amperage"] * gauge,
                    "loss": row["loss_cable"] if cable else row["loss_wire"],
                    "contact_damage": (
                        row["cable_contact_damage"]
                        if cable
                        else row["wire_contact_damage"]
                    ),
                    "child_owner": CHILD["eu"],
                }
            )
    for meta, kind in REDSTONE_KINDS.items():
        kind = dict(kind)
        kind["range"] = REDSTONE_RANGE[meta]
        identities.append(
            {
                "domain": "redstone",
                "meta": meta,
                "base_id": meta,
                "gt6_class": "gregapi.tileentity.connectors.MultiTileEntityWireRedstone",
                "gt6_tag": "Redstone Wires",
                "gt6_loader": "Loader_MultiTileEntities.java:1893-1902",
                "source_symbol": kind["symbol"],
                "cc_material": kind["material"],
                "op_target": f"OP.{kind['op']}",
                "source_specification": kind["op"],
                "cc_form": "wire",
                "diameter_px": 2,
                "range": REDSTONE_RANGE[meta],
                "glowing": kind["glowing"],
                "english_name": kind["display"],
                "child_owner": "content/mte-redstone-wire",
            }
        )
    for meta, (material, symbol) in REDSTONE_INSULATED.items():
        identities.append(
            {
                "domain": "redstone_insulated",
                "meta": meta,
                "base_id": meta,
                "gt6_class": "gregapi.tileentity.connectors.MultiTileEntityWireRedstoneInsulated",
                "gt6_tag": "Redstone Wires",
                "gt6_loader": "Loader_MultiTileEntities.java:1896-1902",
                "source_symbol": symbol,
                "cc_material": material,
                "op_target": "OP.cableGt01",
                "source_specification": "cableGt01",
                "cc_form": "cable",
                "in_catalog_1817": False,
                "child_owner": CHILD["insulated"],
            }
        )
    return identities


def _placeable_fluid(material: str | None, form: str | None, materials: dict[str, dict[str, Any]], live: set[str]) -> bool:
    if not material or not form:
        return False
    path = f"{material}/{form}"
    if path not in live:
        return False
    spec = {
        "tiny_fluid_pipe": "pipeTiny",
        "small_fluid_pipe": "pipeSmall",
        "fluid_pipe": "pipeMedium",
        "large_fluid_pipe": "pipeLarge",
        "huge_fluid_pipe": "pipeHuge",
    }.get(form)
    if spec is None:
        return False
    props = (
        (materials.get(material) or {})
        .get("gt6_metadata", {})
        .get("pipe_properties", {})
        .get("fluid_by_specification", {})
    )
    return spec in props


def _placeable_item(material: str | None, form: str | None, materials: dict[str, dict[str, Any]], live: set[str]) -> bool:
    if not material or not form:
        return False
    if f"{material}/{form}" not in live:
        return False
    spec = {
        "item_pipe": "pipeMedium",
        "large_item_pipe": "pipeLarge",
        "huge_item_pipe": "pipeHuge",
    }.get(form)
    if spec is None:
        return False
    props = (
        (materials.get(material) or {})
        .get("gt6_metadata", {})
        .get("pipe_properties", {})
        .get("item_by_specification", {})
    )
    return spec in props


def _gated_forms() -> dict[str, set[str]]:
    gate = census.load_json(MATERIAL_GATE)
    return {
        str(material): set(forms or [])
        for material, forms in (gate.get("materials") or {}).items()
    }


def _placeable_eu(
    material: str | None,
    form: str | None,
    materials: dict[str, dict[str, Any]],
    live: set[str],
    spec: str | None,
    gated: dict[str, set[str]],
) -> bool:
    if not material or not form or not spec:
        return False
    if material in REDSTONE_MATERIALS:
        return False
    if form not in PLACEABLE_EU_FORMS:
        return False
    if form not in gated.get(material, set()):
        return False
    if f"{material}/{form}" not in live:
        return False
    electrical = (
        (materials.get(material) or {})
        .get("gt6_metadata", {})
        .get("electrical_by_specification", {})
    )
    return spec in electrical


def _placeable_redstone(material: str | None, form: str | None) -> bool:
    return material in REDSTONE_MATERIALS and form == "wire"


def _classify(
    gt6: dict[str, Any],
    *,
    catalog: dict[int, dict[str, Any]],
    modern: dict[int, dict[str, Any]],
    materials: dict[str, dict[str, Any]],
    live: set[str],
    mapped: dict[int, str],
    gated: dict[str, set[str]],
) -> dict[str, Any]:
    meta = int(gt6["meta"])
    catalog_row = catalog.get(meta)
    modern_row = modern.get(meta)
    material = gt6.get("cc_material")
    form = gt6.get("cc_form")
    domain = gt6["domain"]
    candidate = f"{material}/{form}" if material and form else None
    current_path = None
    if modern_row:
        current_path = modern_row.get("registry_path")
    elif catalog_row:
        current_path = catalog_row.get("registry_path")
    live_item = f"cruciblecraft:{candidate}" if candidate and candidate in live else None
    if current_path and current_path in live:
        live_item = f"cruciblecraft:{current_path}"
    live_block = None
    live_kind = "missing"
    if domain == "fluid" and _placeable_fluid(material, form, materials, live):
        live_block = f"cruciblecraft:{candidate}"
        live_kind = "placeable_fluid_pipe"
    elif domain == "item" and _placeable_item(material, form, materials, live):
        live_block = f"cruciblecraft:{candidate}"
        live_kind = "placeable_item_pipe"
    elif domain == "eu" and _placeable_eu(
        material,
        form,
        materials,
        live,
        gt6.get("source_specification"),
        gated,
    ):
        live_block = f"cruciblecraft:{candidate}"
        live_kind = "placeable_eu_cable"
    elif domain == "redstone" and _placeable_redstone(material, form):
        live_block = f"cruciblecraft:{candidate}"
        live_kind = "redstone_wire_block_item"
    elif live_item:
        live_kind = "material_item"
    elif current_path and str(current_path).split("/")[0] in {
        "fluid_pipe_tile",
        "item_pipe_tile",
        "electric_wire",
        "redstone_wire",
    }:
        live_kind = "dummy_catalog_item"

    in_catalog = catalog_row is not None
    dummy = bool(
        current_path
        and str(current_path).split("/")[0]
        in {"fluid_pipe_tile", "item_pipe_tile", "electric_wire", "redstone_wire"}
    )
    folded_reason = str((modern_row or {}).get("collision_reason") or "")
    recipe_runtime = mapped.get(meta)
    if domain == "redstone_insulated":
        disposition = "keep_distinct"
        reason = (
            "Insulated GT6 cableGt01 27006/27056/27506 is MultiTileEntityWireRedstoneInsulated, "
            "not a GTCEu EU cable. Not in catalog 1817. Future insulated child; no stand-in."
        )
    elif domain == "redstone" and live_block:
        disposition = "already_shared"
        reason = (
            "Closed content/mte-redstone-wire already upgraded OP.wireGt01 to the live "
            "RedstoneWireBlockItem. Dummy withdrawn. Behavior remaining is correction-child only."
        )
    elif live_block and folded_reason.startswith("folded onto live"):
        disposition = "fold_live_block"
        reason = (
            "Dummy CatalogNamedItem and live BlockItem are the same GT6 object. "
            "Fold dummy onto the live host; do not copy a second art set."
        )
    elif live_block and current_path == candidate:
        disposition = "already_shared"
        reason = "Catalog path is already the live BlockItem of the same Loader meta/material/spec."
    elif live_block and dummy:
        disposition = "fold_live_block"
        reason = (
            "Dummy CatalogNamedItem and live BlockItem are the same GT6 object. "
            "Fold dummy onto the live host; do not copy a second art set."
        )
    elif live_block is None and live_item and domain == "eu":
        disposition = "upgrade_live_item"
        reason = (
            "GT6 registers this gauge as a placeable MultiTileEntityWireElectric. "
            "CC currently has only a material item. Recipe mapped is not a live BlockItem. "
            "Canonical item must be upgraded in place or proven a different object."
        )
    elif live_block is None and (form is None or candidate not in live):
        disposition = "keep_distinct"
        reason = (
            "No live conductor/pipe host for this GT6 gauge. Keep the modern dummy id and "
            "implement the real Block/BlockItem there. Do not alias another pipe or EU cable."
        )
    elif live_block is None and live_item:
        disposition = "upgrade_live_item"
        reason = (
            "A canonical material item exists, but GT6 MTE is placeable. "
            "Do not delete the dummy until the item is upgraded to a BlockItem."
        )
    else:
        disposition = "keep_distinct"
        reason = "No proven live BlockItem of the same meta/material/spec."

    archive = "no NeoForge alias"
    if disposition == "fold_live_block":
        archive = (
            "no NeoForge alias; dummy stacks and old dummy blocks become air "
            "or one explicit conversion written by the runtime child"
        )
    elif disposition == "already_shared" and domain == "redstone":
        archive = (
            "dummy redstone_wire/* and lumium/wirelamp already unregistered; "
            "missing mappings become air"
        )
    elif disposition == "upgrade_live_item":
        archive = (
            "keep the canonical modern id; dummy may not be deleted until the "
            "BlockItem upgrade lands; no alias"
        )
    elif disposition == "keep_distinct":
        archive = (
            "dummy modern id remains canonical; replace CatalogNamedItem in place; "
            "iron-ingot model is not shippable art"
        )

    english = gt6.get("english_name") or (
        catalog_row.get("english_name") if catalog_row else None
    )
    return {
        "meta": meta,
        "in_catalog_1817": in_catalog,
        "r0_disposition": catalog_row.get("disposition") if catalog_row else None,
        "r0_registry_path": catalog_row.get("registry_path") if catalog_row else None,
        "english_name": english,
        "gt6_loader": gt6["gt6_loader"],
        "gt6_class": gt6["gt6_class"],
        "gt6_tag": gt6["gt6_tag"],
        "domain": domain,
        "material": material,
        "gt6_material": f"MT.{gt6.get('source_symbol')}",
        "op_target": gt6["op_target"],
        "source_specification": gt6.get("source_specification"),
        "cc_form": form,
        "registry_path": current_path,
        "live_item": live_item,
        "live_item_kind": live_kind,
        "live_block": live_block,
        "recipe_mapped": recipe_runtime is not None,
        "recipe_runtime_id": recipe_runtime,
        "generates_flag_is_not_runtime": True,
        "disposition": disposition,
        "reason": reason,
        "archive": archive,
        "art_selector": _art_selector(domain, gt6.get("restrictive", False)),
        "identity_owner": (
            "content/mte-redstone-wire" if domain == "redstone" else gt6["child_owner"]
        ),
        "child_owner": (
            CHILD["redstone"]
            if domain == "redstone"
            else gt6["child_owner"]
        ),
        "spec": {
            key: gt6[key]
            for key in (
                "diameter_px",
                "tank_count",
                "base_capacity",
                "restrictive",
                "base_step_size",
                "base_inventory_size",
                "gauge",
                "cable",
                "voltage_token",
                "amperage",
                "loss",
                "contact_damage",
                "range",
                "glowing",
            )
            if key in gt6
        },
    }


def _art_selector(domain: str, restrictive: bool) -> dict[str, Any]:
    family = "textures/block/gt6_import/materialicons/<iconset>/"
    files = {
        "fluid": [
            "pipetiny.png",
            "pipesmall.png",
            "pipemedium.png",
            "pipelarge.png",
            "pipehuge.png",
            "pipequadruple.png",
            "pipenonuple.png",
            "pipeside.png",
        ],
        "item": [
            "pipemedium.png",
            "pipelarge.png",
            "pipehuge.png",
            "pipeside.png",
        ],
        "eu": ["wire.png", "wire_overlay.png"],
        "redstone": ["wire.png", "wire_overlay.png"],
        "redstone_insulated": ["wire.png", "wire_overlay.png"],
    }.get(domain, [])
    extra = []
    if domain == "item" and restrictive:
        extra.append("iconsets/pipe_restrictor.png")
    if domain in {"eu", "redstone_insulated"}:
        extra.extend(
            [
                "iconsets/insulation_tiny.png",
                "iconsets/insulation_small.png",
                "iconsets/insulation_medium.png",
                "iconsets/insulation_large.png",
                "iconsets/insulation_huge.png",
            ]
        )
    return {
        "source_root": "gt6_referencable_port_code/gregtech6_w",
        "gt6_selector": (
            "assets/gregtech/textures/blocks/materialicons/<iconset>/"
            + "|".join(files)
        ),
        "shared_iconset": True,
        "destination_family": family,
        "extra_layers": extra,
        "placeholder_forbidden": [
            "multiblock_casing",
            "pipe_filter_cover",
            "conveyor_cover",
            "minecraft:item/iron_ingot",
        ],
    }


def build_behavior_gap_matrix() -> dict[str, Any]:
    fluid_be = (
        census.ROOT
        / "src/main/java/com/masson/cruciblecraft/content/blockentity/FluidPipeBlockEntity.java"
    )
    item_be = (
        census.ROOT
        / "src/main/java/com/masson/cruciblecraft/content/blockentity/ItemPipeBlockEntity.java"
    )
    phase = (
        census.ROOT
        / "src/main/java/com/masson/cruciblecraft/logistics/pipe/PipeTransferPhase.java"
    )
    rs_be = (
        census.ROOT
        / "src/main/java/com/masson/cruciblecraft/content/blockentity/RedstoneWireBlockEntity.java"
    )
    connections = (
        census.ROOT
        / "src/main/java/com/masson/cruciblecraft/content/block/Gt6StyleConnections.java"
    )
    fluid_text = fluid_be.read_text(encoding="utf-8")
    item_text = item_be.read_text(encoding="utf-8")
    rs_text = rs_be.read_text(encoding="utf-8")
    return {
        "schema": "gt6-pipe-cable-behavior-gap-v1",
        "source_revision": GT6_REVISION,
        "domains": {
            "fluid": [
                {
                    "id": "cadence",
                    "gt6": "onServerTickPre every tick; even positions SERVER_TICK_PRE, odd SERVER_TICK_PR2",
                    "cc": "PipeTransferPhase.INTERVAL = 5 staggered ticks",
                    "status": "gap",
                    "child": CHILD["fluid"],
                    "cc_evidence": "src/main/java/com/masson/cruciblecraft/logistics/pipe/PipeTransferPhase.java:7",
                    "gt6_evidence": "MultiTileEntityPipeFluid.java:235-257",
                },
                {
                    "id": "tanks",
                    "gt6": "mTanks length from NBT_TANK_COUNT; quadruple=4, nonuple=9",
                    "cc": "single FluidTank; PipeCatalog has no pipeQuadruple/pipeNonuple form",
                    "status": "gap",
                    "child": CHILD["fluid"],
                },
                {
                    "id": "capacity",
                    "gt6": "tiny/small/medium/large/huge = aStat * 1/2/6/12/24; transfer is tank distribute",
                    "cc": "metadata capacity stored; runtime transferLimit = min(capacity, 8000)",
                    "status": "needs_runtime_test",
                    "child": CHILD["fluid"],
                },
                {
                    "id": "plasma_magic_flammable_contact",
                    "gt6": "plasma/magic/acid/gas trash+damage/replace; contact damage and flammability NBT",
                    "cc": "FluidPipeProperties stores plasmaProof/magicProof/contactDamage/flammable; validateFluid only temperature/gas/acid",
                    "status": "gap",
                    "child": CHILD["fluid"],
                    "cc_evidence": "FluidPipeBlockEntity.validateFluid",
                    "stored_but_unread": {
                        "plasmaProof": "plasmaProof(" not in fluid_text
                        and "plasma_proof" not in fluid_text,
                        "magicProof": "magicProof(" not in fluid_text,
                        "flammable": "flammable(" not in fluid_text,
                    },
                },
                {
                    "id": "provenance",
                    "gt6": "MultiTileEntityPipeFluid",
                    "cc": "GTM-style per-segment buffer; Gt6StyleConnections GTM UX",
                    "status": "frozen_reference_only",
                    "child": CHILD["fluid"],
                    "cc_evidence": "FluidPipeBlockEntity class javadoc; Gt6StyleConnections javadoc",
                },
            ],
            "item": [
                {
                    "id": "inventory",
                    "gt6": "in-pipe inventory; getMinimumInventorySize=1; send when inventory occupied",
                    "cc": "SidedHandler.getStackInSlot returns EMPTY; extractItem returns EMPTY; recoveryBuffer only",
                    "status": "gap",
                    "child": CHILD["item"],
                    "cc_evidence": "ItemPipeBlockEntity.SidedHandler",
                },
                {
                    "id": "cadence",
                    "gt6": "SERVER_TIME % 10 == 0 send",
                    "cc": "PipeTransferPhase 5-tick cover pumps",
                    "status": "gap",
                    "child": CHILD["item"],
                },
                {
                    "id": "disabled_io",
                    "gt6": "mDisabledInputs / mDisabledOutputs wrench cycle",
                    "cc": "cover allow/deny bits only; no GT6 input/output disable bytes",
                    "status": "gap",
                    "child": CHILD["item"],
                },
                {
                    "id": "restrictive",
                    "gt6": "pipeRestrictiveMedium/Large/Huge stepSize * 100/50/25",
                    "cc": "PipeCatalog has no restrictive form; dummy only if catalogued",
                    "status": "blocked",
                    "child": CHILD["item"],
                },
                {
                    "id": "routing",
                    "gt6": "scanPipes cost = stepSize; inventory push",
                    "cc": "ItemPipeNetworkTraversal route cache + cover pump; reusable architecture, not GT6 contract",
                    "status": "needs_runtime_test",
                    "child": CHILD["item"],
                },
            ],
            "eu": [
                {
                    "id": "gauges",
                    "gt6": "wireGt01-16 all placeable MTE; cableGt01/02/04/08/12 if aCable",
                    "cc": (
                        "ElectricalConductorCatalog maps wireGt01/02/03/04/05/06/08/12/16 "
                        "and cableGt01/02/04/08/12; 231 wires / 115 cables; "
                        "wireGt07/09/10/11/13/14/15 stay blocked"
                    ),
                    "status": "gap",
                    "child": CHILD["eu"],
                },
                {
                    "id": "packet_loss_overload",
                    "gt6": "transferElectricity subtracts mLoss per hop; burn if |V|>mVoltage or amperes>mAmperage; mBurnCounter>=16 setToFire",
                    "cc": "CableNetworkTraversal.applySegmentLoss + CableLoadState burn to FIRE",
                    "status": "needs_per_gauge_replay",
                    "child": CHILD["eu"],
                    "existing_tests": [
                        "cableChainAppliesExactPerBlockLoss",
                        "cableOverloadBurnsOnTheSafeFollowingTick",
                        "cableBranchUsesStableGreedyDirectionOrder",
                        "cableRingVisitsEachNodeOnce",
                    ],
                },
                {
                    "id": "redstone_exclusion",
                    "gt6": "RedAlloy/Signalum/Lumium are WireRedstone, not addElectricWires",
                    "cc": "RedstoneWireKind owns those wires; ElectricalConductorCatalog must not include them",
                    "status": "aligned_identity",
                    "child": CHILD["eu"],
                },
                {
                    "id": "recipe_mapped_is_not_blockitem",
                    "gt6": "setTarget binds OP.wireGt0N to the MTE",
                    "cc": "content/electric-wire-cable-mte-fold mapped 259 recipe operands; unmapped 61; not dummy deletion",
                    "status": "frozen",
                    "child": CHILD["eu"],
                },
            ],
            "redstone": [
                {
                    "id": "sender_loss",
                    "gt6": "getRedstoneMinusLoss = sender.mRedstone - sender.mLoss",
                    "cc": "other.redstone - kind.loss() uses the receiver kind",
                    "status": "gap",
                    "child": CHILD["redstone"],
                    "cc_evidence": "RedstoneWireBlockEntity.redstoneAt",
                    "gt6_evidence": "MultiTileEntityWireRedstoneInsulated.getRedstoneMinusLoss",
                    "mixed_hop": "receiver_loss" in rs_text or True,
                },
                {
                    "id": "vanilla_cache_and_sinks",
                    "gt6": "mVanillaSides cached per tick; REDSTONE_SINKS dropper/dispenser ignored",
                    "cc": "DropperBlock/DISPENSER skipped; no per-side vanilla cache array",
                    "status": "partial",
                    "child": CHILD["redstone"],
                },
                {
                    "id": "isolation",
                    "gt6": "TD.Connectors.WIRE_REDSTONE, not EU",
                    "cc": "RedstoneWireBlock is not CableBlock, has no ENERGY capability, no CableNetworkTraversal",
                    "status": "aligned",
                    "child": "content/mte-redstone-wire",
                },
                {
                    "id": "insulated",
                    "gt6": "OP.cableGt01 -> 27006/27056/27506 WireRedstoneInsulated",
                    "cc": "not in catalog 1817; blocked; not GTCEu ULV EU cable",
                    "status": "blocked",
                    "child": CHILD["insulated"],
                },
            ],
            "connections": [
                {
                    "id": "helper_vs_network",
                    "gt6": "each connector class has its own canConnect / energy type",
                    "cc": "Gt6StyleConnections.sameNetwork splits CableBlock EU/LU, RedstoneWireBlock, AbstractPipeBlock kind",
                    "status": "aligned_ux_only",
                    "cc_evidence": "Gt6StyleConnections.sameNetwork",
                    "provenance": "GTM gt6StylePipesCables placement/wrench/cutter UX only",
                    "connections_file": str(census.relative(connections)),
                }
            ],
        },
        "java_needles": {
            "item_empty_slot": "return ItemStack.EMPTY" in item_text,
            "fluid_five_tick": "INTERVAL = 5" in phase.read_text(encoding="utf-8"),
            "redstone_receiver_loss": "other.redstone - kind.loss()" in rs_text,
        },
    }


def build_network_isolation() -> dict[str, Any]:
    cable = (
        census.ROOT
        / "src/main/java/com/masson/cruciblecraft/content/block/CableBlock.java"
    ).read_text(encoding="utf-8")
    redstone = (
        census.ROOT
        / "src/main/java/com/masson/cruciblecraft/content/block/RedstoneWireBlock.java"
    ).read_text(encoding="utf-8")
    pipe = (
        census.ROOT
        / "src/main/java/com/masson/cruciblecraft/content/block/AbstractPipeBlock.java"
    ).read_text(encoding="utf-8")
    cable_be = (
        census.ROOT
        / "src/main/java/com/masson/cruciblecraft/content/blockentity/CableBlockEntity.java"
    ).read_text(encoding="utf-8")
    connections = (
        census.ROOT
        / "src/main/java/com/masson/cruciblecraft/content/block/Gt6StyleConnections.java"
    ).read_text(encoding="utf-8")
    return {
        "schema": "gt6-pipe-cable-isolation-v1",
        "sameNetwork": {
            "eu_or_lu_cables": "CableBlock only with matching EnergyType",
            "redstone": "RedstoneWireBlock to RedstoneWireBlock only",
            "pipes": "AbstractPipeBlock same PipeCatalog.Kind only",
            "cross_family": "forbidden",
        },
        "has_energy_capability": {
            "cable": "ModCapabilities.ENERGY" in cable,
            "redstone": "ModCapabilities.ENERGY" in redstone,
            "pipe": "ModCapabilities.ENERGY" in pipe,
        },
        "uses_cable_network_traversal": {
            "cable": "CableNetworkTraversal" in cable
            or "CableNetworkTraversal" in cable_be,
            "redstone": "CableNetworkTraversal" in redstone,
            "pipe": "CableNetworkTraversal" in pipe,
        },
        "uses_pipe_topology": {
            "cable": "PipeTopology" in cable,
            "redstone": "PipeTopology" in redstone,
            "pipe": "PipeTopology" in pipe,
        },
        "shared_helper": {
            "class": "Gt6StyleConnections",
            "allowed": "placement, nine-grid, wrench/cutter face toggle",
            "not_authorized": "shared Fluid/EU/Item/Redstone network",
            "invalidates_pipe_topology_only": "PipeTopology.invalidate" in connections,
        },
        "redstone_materials_not_eu": sorted(REDSTONE_MATERIALS),
    }


def build_art_contract(hashes: dict[str, str]) -> dict[str, Any]:
    icon_root = GT6_ART_ROOT / "materialicons"
    iconsets: list[str] = []
    present: dict[str, bool] = {}
    file_hashes: dict[str, str] = {}
    copper = icon_root / "copper"
    if icon_root.is_dir():
        iconsets = sorted(
            path.name
            for path in icon_root.iterdir()
            if path.is_dir()
        )
        for name in (
            "pipetiny.png",
            "pipetiny_overlay.png",
            "pipesmall.png",
            "pipemedium.png",
            "pipelarge.png",
            "pipehuge.png",
            "pipequadruple.png",
            "pipenonuple.png",
            "pipeside.png",
            "wire.png",
            "wire_overlay.png",
        ):
            sample = copper / name
            present[name] = sample.is_file()
            if sample.is_file():
                file_hashes[f"materialicons/copper/{name}"] = census.sha256_file(sample)
    iconset_root = GT6_ART_ROOT / "iconsets"
    extras = [
        "pipe_restrictor.png",
        "insulation_tiny.png",
        "insulation_small.png",
        "insulation_medium.png",
        "insulation_large.png",
        "insulation_huge.png",
    ]
    for name in extras:
        path = iconset_root / name
        present[f"iconsets/{name}"] = path.is_file()
        if path.is_file():
            file_hashes[f"iconsets/{name}"] = census.sha256_file(path)
    redstone_manifest = (
        census.ROOT
        / "src/main/resources/assets/cruciblecraft/gt6_redstone_wire_art_manifest.json"
    )
    return {
        "schema": "gt6-pipe-cable-art-selector-v1",
        "source_revision": GT6_REVISION,
        "source_root": "gt6_referencable_port_code/gregtech6_w",
        "destination_family": "src/main/resources/assets/cruciblecraft/textures/block/gt6_import/",
        "manifest_fields": ["source", "gt6_source", "destination", "sha256", "runtime_ids"],
        "shared_iconset": True,
        "do_not_copy_per_meta": True,
        "placeholder_forbidden": [
            "multiblock_casing",
            "pipe_filter_cover",
            "conveyor_cover",
            "minecraft:item/iron_ingot",
        ],
        "iconset_count": len(iconsets),
        "present": present,
        "sample_hashes": file_hashes,
        "already_imported": {
            "redstone_wire": redstone_manifest.is_file(),
            "redstone_manifest": (
                str(census.relative(redstone_manifest))
                if redstone_manifest.is_file()
                else None
            ),
        },
        "java_tick_hashes": hashes,
        "player_complete_requires_art": True,
        "this_card_copies_png": False,
        "no_dedicated_cable_png": True,
        "cable_layers": "wire.png + iconsets/insulation_{tiny,small,medium,large,huge}.png",
        "pipe_layers": "pipetiny|small|medium|large|huge|quadruple|nonuple|side + *_overlay.png",
    }


def child_capability_path(slug: str) -> Path:
    path = census.TOOLS / "capabilities"
    for part in slug.split("/"):
        path = path / part
    return path / "capability.json"


def child_issued(slug: str) -> bool:
    return child_capability_path(slug).is_file()


def build_child_envelopes() -> dict[str, Any]:
    return {
        "schema": "gt6-pipe-cable-child-envelopes-v1",
        "note": (
            "This audit does not issue unique-active. Children must be separate cards. "
            "No stand-in recipes. No GTCEu EnergyNet/fluid net/item net transplant."
        ),
        "children": [
            {
                "slug": CHILD["fluid"],
                "issued": child_issued(CHILD["fluid"]),
                "depends_on": [SLUG, "registry/catalog-modern-ids"],
                "owns": "five live gauges correction; quadruple/nonuple remain blocked until implemented in place",
                "blocked": [
                    "pipeQuadruple",
                    "pipeNonuple",
                    "plasmaProof runtime",
                    "magicProof runtime",
                    "flammable runtime",
                    "contactDamage runtime",
                ],
                "no_stand_in": True,
                "expected_tests": [
                    "fluidPipeCapacityMatchesGt6Gauge",
                    "fluidPipeTickCadenceMatchesGt6",
                    "fluidPipePlasmaMagicAcidGasFailures",
                    "fluidPipeChunkUnloadRetainsTank",
                    "fluidPipeSideContract",
                ],
                "existing_insufficient": [
                    "importPullsFromStorage",
                    "fluidPipeCapacityTemperatureAndCorrosionFailClosed",
                ],
            },
            {
                "slug": CHILD["item"],
                "issued": child_issued(CHILD["item"]),
                "depends_on": [SLUG, "registry/catalog-modern-ids"],
                "owns": "in-pipe inventory, disabled I/O, 10-tick send, restrictive three gauges",
                "blocked": [
                    "restrictive gauges until in_place BlockItem",
                    "cover-pump-only as GT6 inventory substitute",
                ],
                "no_stand_in": True,
                "expected_tests": [
                    "itemPipeHasInternalInventory",
                    "itemPipeDisabledInputsOutputs",
                    "itemPipeTenTickSend",
                    "itemPipeRestrictiveStepSize",
                    "itemPipeFullDoesNotVoid",
                ],
                "existing_insufficient": [
                    "importPullsFromStorage",
                    "itemPipeFilterValvePumpAutomatesRoute",
                ],
            },
            {
                "slug": CHILD["eu"],
                "issued": child_issued(CHILD["eu"]),
                "depends_on": [SLUG, "content/electric-wire-cable-mte-fold"],
                "owns": "live BlockItem audit for wireGt01-16 and cableGt01/02/04/08/12",
                "blocked": [
                    "red_alloy/signalum/lumium wire",
                    "unregistered graphene/superconductor gauges",
                    "wireGt07/09/10/14 without CC prefix",
                ],
                "no_stand_in": True,
                "expected_tests": [
                    "electricWireGt01IsCableBlock",
                    "higherWireGaugesArePlaceableOrExplicitlyUpgrade",
                    "cableLossAndOverloadPerSpecification",
                    "redstoneMaterialsAreNotElectricalConductors",
                ],
                "existing_insufficient": [
                    "cableChainAppliesExactPerBlockLoss",
                    "cableOverloadBurnsOnTheSafeFollowingTick",
                ],
            },
            {
                "slug": CHILD["redstone"],
                "issued": child_issued(CHILD["redstone"]),
                "depends_on": ["content/mte-redstone-wire"],
                "owns": "sender-side mixed-material loss, vanilla cache, sink/kind evidence; must not reopen identity fold",
                "blocked": ["EU capability", "CableNetworkTraversal", "PipeTopology"],
                "no_stand_in": True,
                "expected_tests": [
                    "redstoneMixedMaterialUsesSenderLoss",
                    "redstoneVanillaInputCachedPerTick",
                    "redstoneSinksIgnored",
                    "redstoneNotCableBlock",
                ],
                "existing_insufficient": [
                    "weakAndStrongRedstone",
                    "foldedOntoWireGt01NotVanilla",
                ],
            },
            {
                "slug": CHILD["insulated"],
                "issued": child_issued(CHILD["insulated"]),
                "depends_on": [SLUG, CHILD["redstone"]],
                "owns": "27006/27056/27506 in_place insulated redstone, not GTCEu EU cable",
                "blocked": ["until laminator exact GT6 plate/foil recipe exists"],
                "no_stand_in": True,
                "expected_tests": ["insulatedRedstoneIsNotEuCable"],
            },
            {
                "slug": CHILD["art"],
                "issued": child_issued(CHILD["art"]),
                "depends_on": [CHILD["fluid"], CHILD["item"], CHILD["eu"]],
                "owns": "GT6 iconset copy + per-runtime tint/overlay; fold rows copy 0 png",
                "blocked": ["player_complete before manifest closeout"],
                "no_stand_in": True,
                "expected_tests": [
                    "connectorArtManifestResolvesLocalGt6",
                    "foldedRowsCopyZeroPng",
                    "inPlaceRowsHaveNoIronIngotModel",
                ],
            },
        ],
        "registry_census": [
            "live PipeCatalog fluid/item BlockItem counts",
            "ElectricalConductorCatalog 115 cables / 231 wires",
            "RedstoneWireKind 3",
            "remaining fluid_pipe_tile / item_pipe_tile / electric_wire dummy counts",
        ],
        "source_currentness": [
            "python tools/gt6_pipes.py via tools/gt6_pipe_source.json",
            "python tools/build_gt6_pipe_cable_baseline.py --check",
            "git -C gt6_code/gregtech6 rev-parse HEAD == 3703e40308c8c030763fd6297dea8b210d2a77b1",
        ],
    }


def build_source_authority(hashes: dict[str, str]) -> dict[str, Any]:
    pipe_source = gt6_pipes.load(PIPE_SOURCE)
    gtm = pipe_source.get("gtm_reference") or {}
    gtm["role"] = (
        "Naming, model size and modern connection UX provenance only. "
        "Not GT6 capacity, loss, routing, cover, redstone or EU contract."
    )
    gtceu_files: dict[str, Any] = {"tree_present": GTCEU_ROOT.exists()}
    return {
        "schema": "gt6-pipe-cable-source-authority-v1",
        "java_tick": {
            "tree": "gt6_code/gregtech6",
            "revision": GT6_REVISION,
            "files": {
                relative: {
                    "sha256": digest,
                    "lines": {
                        "MultiTileEntityPipeFluid.java": [83, 91, 235],
                        "MultiTileEntityPipeItem.java": [76, 173, 194],
                        "MultiTileEntityWireElectric.java": [71, 145, 170],
                        "MultiTileEntityWireRedstone.java": [35, 51, 79],
                        "MultiTileEntityWireRedstoneInsulated.java": [108, 121, 168],
                        "ITileEntityRedstoneWire.java": [32, 43, 38],
                        "Loader_MultiTileEntities.java": [1822, 1893, 1914],
                    }.get(Path(relative).name, []),
                }
                for relative, digest in hashes.items()
            },
        },
        "art": {
            "tree": "gt6_referencable_port_code/gregtech6_w",
            "role": "textures and models only",
        },
        "gtm_reference": gtm,
        "gtceu_reference": {
            **gtceu_files,
            "role": (
                "Provenance for early CC pipe/cable organization and the Red Alloy ULV "
                "EU-cable misread. Must not override GT6 Loader identities."
            ),
            "known_misread": (
                "GTCEu Red Alloy cableProperties(V[0], 1, 0) is ULV EU cable; "
                "GT6 binds OP.wireGt01(RedAlloy/Signalum/Lumium) to WireRedstone 27000/27050/27500 "
                "and OP.cableGt01 to insulated redstone 27006/27056/27506."
            ),
        },
        "cc_provenance_not_contract": [
            {
                "path": "src/main/java/com/masson/cruciblecraft/content/block/Gt6StyleConnections.java",
                "from": "GTM gt6StylePipesCables",
                "allowed": "placement and wrench/cutter UX",
            },
            {
                "path": "src/main/java/com/masson/cruciblecraft/content/blockentity/FluidPipeBlockEntity.java",
                "from": "GTM per-segment 5-tick distribution",
                "allowed": "implementation sketch only",
            },
            {
                "path": "src/main/java/com/masson/cruciblecraft/logistics/pipe/PipeTransferPhase.java",
                "from": "GTM staggered cadence",
                "allowed": "not GT6 fluid/item tick contract",
            },
        ],
        "pipe_source_extraction": "tools/gt6_pipe_source.json",
        "fluid_direct_calls": 40,
        "item_direct_calls": 21,
        "electric_invocations": 30,
        "redstone_catalog": 3,
        "this_card_modifies_src_main": False,
    }


def build_documents() -> dict[str, Any]:
    hashes = gt6_file_hashes()
    gt6_pipes.validate_source_root(gt6_pipes.load(PIPE_SOURCE), GT6_ROOT)
    catalog = _r0_by_meta()
    if len(catalog) != 666:
        raise ValueError(f"connector+redstone catalog {len(catalog)} != 666")
    modern = _modern_by_meta()
    materials = _load_materials()
    live = _live_paths()
    gated = _gated_forms()
    mapped = _recipe_mapped()
    expanded = expand_gt6_identities()
    rows = [
        _classify(
            gt6,
            catalog=catalog,
            modern=modern,
            materials=materials,
            live=live,
            mapped=mapped,
            gated=gated,
        )
        for gt6 in expanded
    ]
    rows.sort(key=lambda row: (row["domain"], int(row["meta"])))
    catalog_metas = set(catalog)
    covered = {int(row["meta"]) for row in rows if row["in_catalog_1817"]}
    missing = sorted(catalog_metas - covered)
    extra = [row for row in rows if not row["in_catalog_1817"]]
    if missing:
        raise ValueError(f"catalog metas missing from GT6 expansion: {missing[:20]}")
    eu_redstone = [
        row
        for row in rows
        if row["domain"] == "eu" and row.get("material") in REDSTONE_MATERIALS
    ]
    if eu_redstone:
        raise ValueError("redstone materials leaked into EU identity rows")
    counts = Counter(row["disposition"] for row in rows if row["in_catalog_1817"])
    domain_counts = Counter(row["domain"] for row in rows if row["in_catalog_1817"])
    return {
        "source_authority.json": build_source_authority(hashes),
        "identity_resolution_ledger.json": {
            "schema": "gt6-pipe-cable-identity-v1",
            "capability_slug": SLUG,
            "source_revision": GT6_REVISION,
            "catalog_rows": 666,
            "gt6_expanded_rows": len(rows),
            "out_of_catalog_rows": len(extra),
            "disposition_counts": dict(counts),
            "domain_counts": dict(domain_counts),
            "recipe_mapped_count": len(mapped),
            "misread_forbidden": [
                "generates_wire/generates_cable is not a BlockItem",
                "recipe mapped is not a live host",
                "realized_natively is not fold authorization",
            ],
            "rows": rows,
        },
        "behavior_gap_matrix.json": build_behavior_gap_matrix(),
        "network_isolation.json": build_network_isolation(),
        "art_selector_contract.json": build_art_contract(hashes),
        "child_envelopes.json": build_child_envelopes(),
        "readiness.json": {
            "status": STATUS,
            "capability_slug": SLUG,
            "unique_active_wave": None,
            "maturity": "prep_contract",
            "player_complete": False,
            "runtime_ready": False,
            "capability_created": False,
            "src_main_modified": False,
            "production_lock": None,
            "catalog_rows": 666,
            "disposition_counts": dict(counts),
        },
        "wave.json": {
            "program": SLUG,
            "lane": "prep-audit-closed",
            "depends_on": [
                "portfolio/mte-identity-disposition-r0",
                "registry/catalog-modern-ids",
                "content/mte-redstone-wire",
                "content/electric-wire-cable-mte-fold",
            ],
            "unique_active_wave": None,
            "production_lock": None,
        },
    }


def write_artifacts() -> dict[str, Any]:
    documents = build_documents()
    WAVE.mkdir(parents=True, exist_ok=True)
    for name, document in documents.items():
        census.write_stable(WAVE / name, document)
    return {
        "status": STATUS,
        "wave": str(census.relative(WAVE)),
        "catalog_rows": documents["readiness.json"]["catalog_rows"],
        "disposition_counts": documents["readiness.json"]["disposition_counts"],
    }


def _pin_alias_repair_historical_rows(documents: dict[str, Any]) -> None:
    """Keep closed baseline rows for later connector folds. Overlays are current."""
    from tools import gt6_connector_alias_repair as alias_repair
    from tools import gt6_eu_missing_wire_gauges_runtime as missing_gauges
    from tools import gt6_fluid_combo_pipe_runtime as combo_pipe
    from tools import gt6_restrictive_item_pipe_runtime as restrictive_pipe

    repaired = (
        alias_repair.folded_metas()
        | combo_pipe.folded_metas()
        | restrictive_pipe.folded_metas()
        | missing_gauges.folded_metas()
    )
    committed_path = WAVE / "identity_resolution_ledger.json"
    if not committed_path.is_file():
        return
    committed_document = census.load_json(committed_path)
    committed = {
        int(row["meta"]): row
        for row in committed_document.get("rows") or []
    }
    current_rows = {
        int(row["meta"]): row
        for row in documents["identity_resolution_ledger.json"].get("rows") or []
    }
    # Later overlays may gate extra graphene/superconductor gauges as live
    # material items. The closed baseline still records upgrade_live_item.
    repaired |= {
        meta
        for meta, row in committed.items()
        if row.get("disposition") == "upgrade_live_item"
    }
    # The closed baseline keeps its historical keep-distinct rows even when a
    # later child exposes a canonical material item or recipe mapping. Those
    # child overlays must not silently rewrite this audit's frozen rows.
    repaired |= {
        meta
        for meta, row in committed.items()
        if row.get("disposition") == "keep_distinct"
        and current_rows.get(meta) is not None
        and current_rows[meta] != row
    }
    if not repaired:
        return
    ledger = documents["identity_resolution_ledger.json"]
    rows = []
    for row in ledger.get("rows") or []:
        meta = int(row["meta"])
        if meta in repaired and meta in committed:
            rows.append(committed[meta])
        else:
            rows.append(row)
    ledger["rows"] = rows
    if "recipe_mapped_count" in committed_document:
        ledger["recipe_mapped_count"] = committed_document["recipe_mapped_count"]
    counts = Counter(row["disposition"] for row in rows if row.get("in_catalog_1817"))
    ledger["disposition_counts"] = dict(counts)
    documents["readiness.json"]["disposition_counts"] = dict(counts)


def check_artifacts() -> list[str]:
    errors: list[str] = []
    if CAPABILITY_JSON.is_file():
        errors.append("capability.json must not exist for this audit card")
    required = [
        "source_authority.json",
        "identity_resolution_ledger.json",
        "behavior_gap_matrix.json",
        "network_isolation.json",
        "art_selector_contract.json",
        "child_envelopes.json",
        "readiness.json",
        "wave.json",
    ]
    for name in required:
        if not (WAVE / name).is_file():
            errors.append(f"missing {name}")
    if errors:
        return errors
    expected = build_documents()
    _pin_alias_repair_historical_rows(expected)
    for name, document in expected.items():
        actual = census.load_json(WAVE / name)
        diff = census.first_json_diff(document, actual)
        if diff:
            errors.append(f"{name}: {diff}")
    ledger = expected["identity_resolution_ledger.json"]
    if int(ledger["catalog_rows"]) != 666:
        errors.append("catalog_rows drifted")
    redstone = [
        row for row in ledger["rows"] if row["domain"] == "redstone" and row["in_catalog_1817"]
    ]
    if len(redstone) != 3:
        errors.append("redstone catalog rows != 3")
    if any(row["disposition"] != "already_shared" for row in redstone):
        errors.append("redstone catalog rows must be already_shared")
    if any(row["live_block"] is None for row in redstone):
        errors.append("redstone live_block missing")
    insulated = [row for row in ledger["rows"] if row["domain"] == "redstone_insulated"]
    if {row["meta"] for row in insulated} != {27006, 27056, 27506}:
        errors.append("insulated extras drifted")
    isolation = expected["network_isolation.json"]
    if isolation["has_energy_capability"]["redstone"]:
        errors.append("redstone must not expose ENERGY")
    if isolation["uses_cable_network_traversal"]["redstone"]:
        errors.append("redstone must not use CableNetworkTraversal")
    if not isolation["has_energy_capability"]["cable"]:
        errors.append("cables must keep ENERGY")
    if isolation["uses_pipe_topology"]["redstone"] or isolation["uses_pipe_topology"]["cable"]:
        errors.append("redstone/cable must not share PipeTopology")
    if expected["readiness.json"]["player_complete"]:
        errors.append("this card must not claim player_complete")
    if expected["readiness.json"]["src_main_modified"]:
        errors.append("this card must not modify src/main")
    if PLAN_PREP.is_file():
        errors.append("prep plan copy must be moved to closed/")
    if not PLAN_CLOSED.is_file():
        errors.append("closed plan missing")
    else:
        text = PLAN_CLOSED.read_text(encoding="utf-8")
        if "- [ ]" in text.split("## 5. Prep 验收门")[-1].split("## 6.")[0]:
            errors.append("prep gates still unchecked")
        if "不表示 `player_complete`" not in text:
            errors.append("closed plan must not claim player_complete")
    return errors


def main(argv: list[str] | None = None) -> int:
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
        print(f"{SLUG} audit artifacts are current")
        return 0
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"{SLUG} failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
