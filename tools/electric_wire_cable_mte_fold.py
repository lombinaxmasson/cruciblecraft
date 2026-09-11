#!/usr/bin/env python3
"""Fold GT6 electric wire/cable MTE ids onto registered CC conductors.

Source of truth is ``MultiTileEntityWireElectric.addElectricWires`` in the pinned
local GT6 tree. Catalog scatter ``gt_mte`` identities are not recipe objects.
Missing gauges and unregistered materials stay unmapped.
"""
from __future__ import annotations

import re
from typing import Any

from tools import census_common as census
from tools.gt6_resolve import registered_ids, resolve

SLUG = "content/electric-wire-cable-mte-fold"
SOURCE_REVISION = "3703e40308c8c030763fd6297dea8b210d2a77b1"
SOURCE_ITEM = "gregtech:gt.multitileentity"
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
WAVE = census.TOOLS / "waves" / "content" / "electric-wire-cable-mte-fold"
OPERAND_MAP = WAVE / "operand_runtime_map.json"
INVOCATIONS = WAVE / "invocations.json"
UNMAPPED = WAVE / "unmapped.json"
READINESS = WAVE / "readiness.json"
TOPOLOGY = WAVE / "topology.json"
WAVE_JSON = WAVE / "wave.json"
CAPABILITY = (
    census.TOOLS / "capabilities" / "content" / "electric-wire-cable-mte-fold"
    / "capability.json"
)
ELECTRIC_WIRES = re.compile(
    r"MultiTileEntityWireElectric\.addElectricWires\("
    r"(\d+)\s*,\s*\d+\s*,\s*[^,]+,\s*\d+\s*,\s*\d+\s*,\s*\d+\s*,\s*"
    r"([TF])\s*,\s*([TF])\s*,\s*([TF])\s*,\s*"
    r"aRegistry,\s*aMetalWires,\s*aClass,\s*MT\.(\w+)\s*\)"
)
WIRE_FORMS = (
    (0, 1, "wire", "wireGt01"),
    (1, 2, "double_wire", "wireGt02"),
    (3, 4, "quadruple_wire", "wireGt04"),
    (7, 8, "octuple_wire", "wireGt08"),
    (11, 12, "dodecuple_wire", "wireGt12"),
    (15, 16, "hexadecuple_wire", "wireGt16"),
)
CABLE_FORMS = (
    (16, 1, "cable", "cableGt01"),
    (17, 2, "double_cable", "cableGt02"),
    (19, 4, "quadruple_cable", "cableGt04"),
    (23, 8, "octuple_cable", "cableGt08"),
    (27, 12, "dodecuple_cable", "cableGt12"),
)


def parse_invocations() -> list[dict[str, Any]]:
    if not LOADER.is_file():
        raise FileNotFoundError(f"missing local GT6 loader {LOADER}")
    text = LOADER.read_text(encoding="utf-8")
    rows: list[dict[str, Any]] = []
    for match in ELECTRIC_WIRES.finditer(text):
        rows.append(
            {
                "base_id": int(match.group(1)),
                "wire_contact_damage": match.group(2) == "T",
                "cable_contact_damage": match.group(3) == "T",
                "cable_generated": match.group(4) == "T",
                "source_symbol": match.group(5),
            }
        )
    if len(rows) != 30:
        raise ValueError(f"addElectricWires count {len(rows)} != 30")
    return rows


def _resolve_runtime(spec: str, symbol: str) -> dict[str, Any]:
    query = f"OP.{spec}(MT.{symbol})"
    result = resolve(query)
    form = result.get("form") or {}
    item = str(form.get("item") or "")
    registered = bool(form.get("registered"))
    if result.get("status") != "ok" or not item:
        return {
            "query": query,
            "status": str(result.get("status") or "unmapped"),
            "runtime_id": None,
        }
    if "/gt_mte/" in item:
        return {
            "query": query,
            "status": "scatter_identity",
            "runtime_id": None,
        }
    if item not in registered_ids():
        return {
            "query": query,
            "status": "unregistered",
            "runtime_id": None,
        }
    return {
        "query": query,
        "status": "ok",
        "runtime_id": item,
        "cc_material": (result.get("material") or {}).get("cc_material"),
        "cc_prefix": (result.get("prefix") or {}).get("cc_prefix"),
    }


def build_overlay() -> dict[str, Any]:
    mapped: list[dict[str, Any]] = []
    skipped: list[dict[str, Any]] = []
    seen: dict[int, str] = {}
    for invocation in parse_invocations():
        forms = list(WIRE_FORMS)
        if invocation["cable_generated"]:
            forms.extend(CABLE_FORMS)
        for offset, gauge, _cc_form, spec in forms:
            meta = invocation["base_id"] + offset
            resolved = _resolve_runtime(spec, invocation["source_symbol"])
            row = {
                "meta": meta,
                "base_id": invocation["base_id"],
                "gauge": gauge,
                "source_specification": spec,
                "source_symbol": invocation["source_symbol"],
                "cable": spec.startswith("cable"),
                **resolved,
            }
            if resolved["status"] == "ok" and resolved["runtime_id"]:
                previous = seen.get(meta)
                if previous and previous != resolved["runtime_id"]:
                    raise ValueError(
                        f"MTE {meta} maps to both {previous} and {resolved['runtime_id']}"
                    )
                seen[meta] = str(resolved["runtime_id"])
                mapped.append(row)
            else:
                skipped.append(row)
    mapped.sort(key=lambda row: int(row["meta"]))
    skipped.sort(key=lambda row: int(row["meta"]))
    return {
        "generated_by": "python tools/build_electric_wire_cable_mte_fold.py",
        "schema_version": 1,
        "source_item": SOURCE_ITEM,
        "source_revision": SOURCE_REVISION,
        "wave_slug": SLUG,
        "mapped_count": len(mapped),
        "unmapped_count": len(skipped),
        "mappings": [
            {
                "meta": row["meta"],
                "runtime_id": row["runtime_id"],
                "source_specification": row["source_specification"],
                "source_symbol": row["source_symbol"],
                "query": row["query"],
            }
            for row in mapped
        ],
        "invocations": parse_invocations(),
        "unmapped": skipped,
    }


def overlay_runtime_map() -> dict[tuple[str, int], str]:
    if not OPERAND_MAP.is_file():
        return {}
    document = census.load_json(OPERAND_MAP)
    mapped: dict[tuple[str, int], str] = {}
    for row in document.get("mappings") or []:
        mapped[(SOURCE_ITEM, int(row["meta"]))] = str(row["runtime_id"])
    return mapped


def unique_active_wave() -> str | None:
    if not CAPABILITY.is_file():
        return SLUG
    document = census.load_json(CAPABILITY)
    if document.get("workflow") == "active":
        return SLUG
    return None


def documents() -> dict[str, Any]:
    overlay = build_overlay()
    mapped = overlay["mappings"]
    wave = unique_active_wave()
    closed = wave is None
    return {
        "operand_runtime_map": {
            "generated_by": overlay["generated_by"],
            "mapped_count": overlay["mapped_count"],
            "mappings": mapped,
            "schema_version": 1,
            "source_item": SOURCE_ITEM,
            "source_revision": SOURCE_REVISION,
            "status": "ELECTRIC_WIRE_CABLE_MTE_FOLD_MAPPED",
            "wave_slug": SLUG,
        },
        "invocations": {
            "count": len(overlay["invocations"]),
            "generated_by": overlay["generated_by"],
            "invocations": overlay["invocations"],
            "schema_version": 1,
            "source_revision": SOURCE_REVISION,
            "wave_slug": SLUG,
        },
        "unmapped": {
            "generated_by": overlay["generated_by"],
            "rows": overlay["unmapped"],
            "schema_version": 1,
            "unmapped_count": overlay["unmapped_count"],
            "wave_slug": SLUG,
        },
        "readiness": {
            "evidence": {
                "cable_forms": [row[3] for row in CABLE_FORMS],
                "invocations": 30,
                "mapped_count": overlay["mapped_count"],
                "owns_families": 0,
                "unmapped_count": overlay["unmapped_count"],
                "wire_forms": [row[3] for row in WIRE_FORMS],
            },
            "generated_by": overlay["generated_by"],
            "next_unassigned": closed,
            "schema_version": 1,
            "source_revision": SOURCE_REVISION,
            "status": (
                "ELECTRIC_WIRE_CABLE_MTE_FOLD_READY"
                if closed
                else "ELECTRIC_WIRE_CABLE_MTE_FOLD_ACTIVE"
            ),
            "unique_active_wave": wave,
            "wave_slug": SLUG,
        },
        "topology": {
            "append_only": False,
            "complete_family_count": 0,
            "generated_by": overlay["generated_by"],
            "next_unassigned": closed,
            "remaining_recipe_gap": overlay["unmapped_count"],
            "schema_version": 1,
            "source_revision": SOURCE_REVISION,
            "status": "WAVE_READY",
            "unique_active_wave": wave,
            "wave_slug": SLUG,
        },
        "wave": {
            "depends_on": [
                "machines/laminator",
                "machines/loom",
                "machines/melter",
                "machines/nanofab",
            ],
            "generated_by": overlay["generated_by"],
            "program": SLUG,
            "schema_version": 1,
            "source_revision": SOURCE_REVISION,
            "unique_active_wave": wave,
        },
    }


def write() -> dict[str, Any]:
    WAVE.mkdir(parents=True, exist_ok=True)
    built = documents()
    census.write_stable(OPERAND_MAP, built["operand_runtime_map"])
    census.write_stable(INVOCATIONS, built["invocations"])
    census.write_stable(UNMAPPED, built["unmapped"])
    census.write_stable(READINESS, built["readiness"])
    census.write_stable(TOPOLOGY, built["topology"])
    census.write_stable(WAVE_JSON, built["wave"])
    return {
        "mapped_count": built["operand_runtime_map"]["mapped_count"],
        "unmapped_count": built["unmapped"]["unmapped_count"],
    }


def check() -> list[str]:
    errors: list[str] = []
    if not LOADER.is_file():
        return [f"missing local GT6 loader {LOADER}"]
    expected = documents()
    required = (
        (OPERAND_MAP, expected["operand_runtime_map"]),
        (INVOCATIONS, expected["invocations"]),
        (UNMAPPED, expected["unmapped"]),
        (READINESS, expected["readiness"]),
        (TOPOLOGY, expected["topology"]),
        (WAVE_JSON, expected["wave"]),
    )
    for path, document in required:
        if not path.is_file():
            errors.append(f"missing {census.relative(path)}")
            continue
        actual = census.load_json(path)
        if actual != document:
            errors.append(f"{census.relative(path)} drifted")
    mapped = expected["operand_runtime_map"]["mappings"]
    if not mapped:
        errors.append("overlay mapped_count must be > 0")
    tin = next((row for row in mapped if row["meta"] == 28066), None)
    if tin is None or tin["runtime_id"] != "cruciblecraft:tin/cable":
        errors.append("1x tin cable 28066 must fold to cruciblecraft:tin/cable")
    wire = next((row for row in mapped if row["meta"] == 28050), None)
    if wire is None or wire["runtime_id"] != "cruciblecraft:tin/wire":
        errors.append("1x tin wire 28050 must fold to cruciblecraft:tin/wire")
    if any("/gt_mte/" in str(row["runtime_id"]) for row in mapped):
        errors.append("overlay must not target scatter gt_mte identities")
    if CAPABILITY.is_file():
        capability = census.load_json(CAPABILITY)
        if capability.get("slug") != SLUG:
            errors.append("capability slug drifted")
        if capability.get("maturity") != "runtime_ready":
            errors.append("fold card stays runtime_ready")
        if capability.get("workflow") not in {"active", "accepted"}:
            errors.append("fold workflow must be active or accepted")
    return errors
