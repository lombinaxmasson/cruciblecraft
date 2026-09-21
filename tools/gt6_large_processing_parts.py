#!/usr/bin/env python3
"""Census and landing checks for GT6 large processing-multiblock parts and maps.

Denominator: Loader_MultiTileEntities 17102–17114 plus specialized parts
18105/18107/18108. Not GT6U. Not distillation 17101 / centrifuge 17100.
Close criterion for this card: a later host card must not record blocked.md
for missing structure parts, missing RecipeMaps, or unresolved controller/part
D0 grids. Process-dump rows are not this denominator.
"""
from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path
from typing import Any

from tools import gt6_resolve
from tools import io_common as io

SLUG = "machines/large-processing-parts"
GT6_REVISION = io.SOURCE_REVISION
ROOT = io.ROOT
WAVE = ROOT / "tools" / "waves" / "machines" / "large-processing-parts"
CENSUS_PATH = WAVE / "census.json"
INPLACE = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "mte_inplace_catalog.json"
)
RECIPE_MAPS_JAVA = (
    ROOT
    / "src"
    / "main"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "registry"
    / "ModRecipeMaps.java"
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
MULTIBLOCKS = (
    ROOT
    / "gt6_code"
    / "gregtech6"
    / "src"
    / "main"
    / "java"
    / "gregtech"
    / "tileentity"
    / "multiblocks"
)
TARGET_ID = re.compile(
    r"checkAndSetTarget\(\s*this\s*,[^,]+,[^,]+,[^,]+,\s*(\d+)\s*,"
)
CREATE_MAP = re.compile(r'create\("([^"]+)"\)')
TOOL_KEYS = frozenset("acdfhknopqrstwxyz")

# GT6 large processing hosts in the user's list. 17100/17101 already landed.
MACHINES: list[dict[str, Any]] = [
    {
        "meta": 17102,
        "english": "Large Batch Mixer",
        "class_name": "MultiTileEntityMixer",
        "gt6_map": "RM.Mixer",
        "cc_map": "mixer",
        "energy": "RU",
        "parallel": 256,
        "parallel_duration": True,
        "no_constant_power": False,
        "craft_tokens": {
            "M": "getItem(18002)",
            "R": "IL.Processor_Crystal_Ruby",
            "C": "OD_CIRCUITS[6]",
            "P": "OP.plateDense(MT.StainlessSteel)",
            "S": "OP.stickLong(MT.StainlessSteel)",
        },
        "pattern": ["PSP", "PSP", "RMC"],
    },
    {
        "meta": 17103,
        "english": "Large Electrolyzer",
        "class_name": "MultiTileEntityElectrolyzer",
        "gt6_map": "RM.Electrolyzer",
        "cc_map": "electrolyzer",
        "energy": "EU",
        "parallel": 16,
        "parallel_duration": True,
        "no_constant_power": False,
        "craft_tokens": {
            "M": "getItem(18105)",
            "R": "IL.Processor_Crystal_Ruby",
            "C": "OD_CIRCUITS[6]",
        },
        "pattern": ["CMC", "RCR"],
    },
    {
        "meta": 17104,
        "english": "Large Bathing Vat",
        "class_name": "MultiTileEntityBath",
        "gt6_map": "RM.Bath",
        "cc_map": "bath",
        "energy": "TU",
        "parallel": 64,
        "parallel_duration": False,
        "no_constant_power": True,
        "craft_tokens": {
            "M": "getItem(18002)",
            "R": "IL.Processor_Crystal_Ruby",
            "C": "OD_CIRCUITS[6]",
            "P": "OP.plateDense(MT.StainlessSteel)",
            "A": "IL.ROBOT_ARMS[2]",
        },
        "pattern": ["CRC", "PMP", "APA"],
    },
    {
        "meta": 17105,
        "english": "Large Coagulator Array",
        "class_name": "MultiTileEntityCoagulator",
        "gt6_map": "RM.Coagulator",
        "cc_map": "coagulator",
        "energy": "TU",
        "parallel": 64,
        "parallel_duration": False,
        "no_constant_power": True,
        "craft_tokens": {
            "M": "getItem(18002)",
            "R": "IL.Processor_Crystal_Ruby",
            "C": "OD_CIRCUITS[6]",
            "P": "OP.plateDense(MT.StainlessSteel)",
        },
        "pattern": ["CRC", "PMP", "PPP"],
    },
    {
        "meta": 17106,
        "english": "Large Electric Oven",
        "class_name": "MultiTileEntityOven",
        "gt6_map": "RM.Furnace",
        "cc_map": "oven",
        "energy": "EU",
        "parallel": 64,
        "parallel_duration": True,
        "no_constant_power": False,
        "craft_tokens": {
            "M": "getItem(18007)",
            "R": "IL.Processor_Crystal_Ruby",
            "C": "OD_CIRCUITS[6]",
            "P": "OP.plateDense(MT.Invar)",
            "w": "wrench",
        },
        "pattern": ["PPP", "PwP", "RMC"],
    },
    {
        "meta": 17107,
        "english": "Large Sluice",
        "class_name": "MultiTileEntitySluice",
        "gt6_map": "RM.Sluice",
        "cc_map": "sluice",
        "energy": "RU",
        "parallel": 64,
        "parallel_duration": True,
        "no_constant_power": False,
        "craft_tokens": {
            "M": "getItem(18006)",
            "R": "IL.Processor_Crystal_Ruby",
            "C": "OD_CIRCUITS[6]",
            "G": "OP.gearGt(MT.Ti)",
            "S": "OP.stick(MT.Ti)",
            "w": "wrench",
        },
        "pattern": ["GGG", "SwS", "RMC"],
    },
    {
        "meta": 17108,
        "english": "Large Crusher",
        "class_name": "MultiTileEntityCrusher",
        "gt6_map": "RM.Crusher",
        "cc_map": "crusher",
        "energy": "RU",
        "parallel": 64,
        "parallel_duration": True,
        "no_constant_power": True,
        "craft_tokens": {
            "M": "getItem(18003)",
            "R": "IL.Processor_Crystal_Ruby",
            "C": "OD_CIRCUITS[6]",
            "G": "OP.gearGt(MT.TungstenSteel)",
            "S": "OP.gearGtSmall(MT.TungstenSteel)",
        },
        "pattern": ["GSG", "SGS", "RMC"],
    },
    {
        "meta": 17109,
        "english": "Large Shredder",
        "class_name": "MultiTileEntityShredder",
        "gt6_map": "RM.Shredder",
        "cc_map": "shredder",
        "energy": "RU",
        "parallel": 64,
        "parallel_duration": True,
        "no_constant_power": True,
        "craft_tokens": {
            "M": "getItem(18003)",
            "R": "IL.Processor_Crystal_Ruby",
            "C": "OD_CIRCUITS[6]",
            "G": "OP.gearGt(MT.TungstenSteel)",
            "S": "OP.gearGtSmall(MT.TungstenSteel)",
        },
        "pattern": ["SGS", "GSG", "RMC"],
    },
    {
        "meta": 17110,
        "english": "Implosion Compressor",
        "class_name": "MultiTileEntityImplosionCompressor",
        "gt6_map": "RM.ImplosionCompressor",
        "cc_map": "implosion_compressor",
        "energy": "TU",
        "parallel": 64,
        "parallel_duration": False,
        "no_constant_power": True,
        "craft_tokens": {
            "M": "getItem(18023)",
            "R": "IL.Processor_Crystal_Ruby",
            "C": "OD_CIRCUITS[6]",
            "P": "OP.plateDense(MT.TungstenSteel)",
            "A": "IL.ROBOT_ARMS[2]",
        },
        "pattern": ["CPC", "PAP", "RMR"],
    },
    {
        "meta": 17112,
        "english": "Large Autoclave",
        "class_name": "MultiTileEntityAutoclave",
        "gt6_map": "RM.Autoclave",
        "cc_map": "autoclave",
        "energy": "TU",
        "parallel": 16,
        "parallel_duration": False,
        "no_constant_power": True,
        "craft_tokens": {
            "M": "getItem(18022)",
            "R": "IL.Processor_Crystal_Ruby",
            "C": "OD_CIRCUITS[6]",
            "P": "OP.plateDense(MT.StainlessSteel)",
        },
        "pattern": ["CRC", "PMP", "PPP"],
    },
    {
        "meta": 17113,
        "english": "Large Fermenter",
        "class_name": "MultiTileEntityFermenter",
        "gt6_map": "RM.Fermenter",
        "cc_map": "fermenter",
        "energy": "HU",
        "parallel": 256,
        "parallel_duration": True,
        "no_constant_power": False,
        "craft_tokens": {
            "M": "getItem(18002)",
            "R": "IL.Processor_Crystal_Ruby",
            "C": "OD_CIRCUITS[6]",
            "P": "OP.plateDense(MT.StainlessSteel)",
        },
        "pattern": ["PPP", "CRC", "PMP"],
    },
    {
        "meta": 17114,
        "english": "Large Squeezer",
        "class_name": "MultiTileEntitySqueezer",
        "gt6_map": "RM.Squeezer",
        "cc_map": "squeezer",
        "energy": "RU",
        "parallel": 64,
        "parallel_duration": True,
        "no_constant_power": True,
        "craft_tokens": {
            "M": "getItem(18009)",
            "R": "IL.Processor_Crystal_Ruby",
            "C": "OD_CIRCUITS[6]",
            "G": "OP.gearGt(ANY.Steel)",
            "S": "OP.gearGtSmall(ANY.Steel)",
        },
        "pattern": ["GSG", "GSG", "RMC"],
    },
]

SPECIALIZED_PARTS: list[dict[str, Any]] = [
    {
        "meta": 18105,
        "english": "Electrolyzer Part",
        "registry_path": "multiblock/electrolyzer_part",
        "gt6_texture": "electrolyzerparts",
        "craft_tokens": {
            "M": "OP.casingMachine(MT.StainlessSteel)",
            "W": "OP.wireGt01(MT.Pt)",
            "C": "OD_CIRCUITS[6]",
            "w": "wrench",
        },
        "pattern": ["WwW", "WMW", "CCC"],
    },
    {
        "meta": 18107,
        "english": "Crusher Wheels",
        "registry_path": "multiblock/crusher_wheels",
        "gt6_texture": "crusherwheels",
        "craft_tokens": {
            "M": "OP.casingMachineDouble(MT.TungstenSteel)",
            "G": "OP.gearGt(MT.TungstenSteel)",
            "D": "OP.gem(ANY.Diamond)",
        },
        "pattern": ["DDD", "GDG", "GMG"],
    },
    {
        "meta": 18108,
        "english": "Shredder Blades",
        "registry_path": "multiblock/shredder_blades",
        "gt6_texture": "shredderblades",
        "craft_tokens": {
            "M": "OP.casingMachineDouble(MT.TungstenSteel)",
            "G": "OP.gearGt(MT.TungstenSteel)",
            "D": "OP.plateGem(ANY.Diamond)",
            "w": "wrench",
        },
        "pattern": ["DGD", "GwG", "GMG"],
    },
]


def _inplace_by_meta() -> dict[int, dict[str, Any]]:
    if not INPLACE.is_file():
        return {}
    document = json.loads(INPLACE.read_text(encoding="utf-8"))
    out: dict[int, dict[str, Any]] = {}
    for row in document.get("identities") or []:
        try:
            out[int(row["meta"])] = row
        except (TypeError, ValueError, KeyError):
            continue
    return out


def _cc_maps() -> set[str]:
    if not RECIPE_MAPS_JAVA.is_file():
        return set()
    return set(CREATE_MAP.findall(RECIPE_MAPS_JAVA.read_text(encoding="utf-8")))


def _structure_metas(class_name: str) -> list[int]:
    path = MULTIBLOCKS / f"{class_name}.java"
    if not path.is_file():
        return []
    found = [int(match) for match in TARGET_ID.findall(path.read_text(encoding="utf-8"))]
    ordered: list[int] = []
    seen: set[int] = set()
    for meta in found:
        if meta not in seen:
            seen.add(meta)
            ordered.append(meta)
    return ordered


def _resolve_token(token: str) -> dict[str, Any]:
    if token == "wrench":
        return {
            "query": token,
            "gt": "catalyst wrench",
            "kind": "catalyst",
            "item": "cruciblecraft:material_wrench",
            "status": "ok",
            "live": True,
        }
    return gt6_resolve.resolve(token)


def _grid_status(tokens: dict[str, str]) -> dict[str, Any]:
    cells: dict[str, Any] = {}
    missing: list[str] = []
    for key, token in tokens.items():
        if key in TOOL_KEYS and token == "wrench":
            cells[key] = _resolve_token(token)
            continue
        resolved = _resolve_token(token)
        cells[key] = resolved
        if resolved.get("status") != "ok":
            missing.append(key)
    return {
        "ok": not missing,
        "missing_keys": missing,
        "cells": cells,
    }


def _part_row(meta: int, catalog: dict[int, dict[str, Any]]) -> dict[str, Any]:
    live = catalog.get(meta)
    return {
        "meta": meta,
        "live": live is not None,
        "runtime_id": None if live is None else live.get("runtime_id"),
        "registry_path": None if live is None else live.get("registry_path"),
    }


def build_census() -> dict[str, Any]:
    catalog = _inplace_by_meta()
    maps = _cc_maps()
    machines_out: list[dict[str, Any]] = []
    structure_metas: set[int] = set()
    for machine in MACHINES:
        parts = _structure_metas(str(machine["class_name"]))
        structure_metas.update(parts)
        map_id = str(machine["cc_map"])
        machines_out.append(
            {
                "meta": machine["meta"],
                "english": machine["english"],
                "class_name": machine["class_name"],
                "gt6_map": machine["gt6_map"],
                "cc_map": map_id,
                "map_registered": map_id in maps,
                "energy": machine["energy"],
                "parallel": machine["parallel"],
                "parallel_duration": machine["parallel_duration"],
                "no_constant_power": machine["no_constant_power"],
                "structure_part_metas": parts,
                "structure_parts": [_part_row(meta, catalog) for meta in parts],
                "controller_live": catalog.get(int(machine["meta"])) is not None,
                "obtain": _grid_status(dict(machine["craft_tokens"])),
                "pattern": list(machine["pattern"]),
            }
        )
    specialized_out: list[dict[str, Any]] = []
    for part in SPECIALIZED_PARTS:
        meta = int(part["meta"])
        specialized_out.append(
            {
                **_part_row(meta, catalog),
                "english": part["english"],
                "intended_registry_path": part["registry_path"],
                "gt6_texture": part["gt6_texture"],
                "obtain": _grid_status(dict(part["craft_tokens"])),
                "pattern": list(part["pattern"]),
            }
        )
    missing_maps = sorted(
        {row["cc_map"] for row in machines_out if not row["map_registered"]}
    )
    missing_specialized = [
        row["meta"] for row in specialized_out if not row["live"]
    ]
    missing_structure = sorted(
        meta for meta in structure_metas if catalog.get(meta) is None
    )
    blocked_obtain = []
    for row in machines_out:
        if not row["obtain"]["ok"]:
            blocked_obtain.append(
                {"kind": "controller", "meta": row["meta"], "keys": row["obtain"]["missing_keys"]}
            )
    for row in specialized_out:
        if not row["obtain"]["ok"]:
            blocked_obtain.append(
                {"kind": "part", "meta": row["meta"], "keys": row["obtain"]["missing_keys"]}
            )
    later_host_blocked = bool(
        missing_maps or missing_specialized or missing_structure or blocked_obtain
    )
    return {
        "schema_version": 1,
        "slug": SLUG,
        "source_revision": GT6_REVISION,
        "close_criterion": (
            "Later large-processing host cards must not record blocked.md "
            "for missing structure parts, missing RecipeMaps, or unresolved "
            "controller/part D0 grids."
        ),
        "out_of_scope": [
            "GT6U large machines",
            "17100 Large Centrifuge",
            "17101 Distillation Tower",
            "process dump rows (fermenter/squeezer/etc.)",
            "formation Java / energy plugins / host BlockItems",
        ],
        "later_host_would_block": later_host_blocked,
        "missing_recipe_maps": missing_maps,
        "missing_specialized_parts": missing_specialized,
        "missing_structure_part_metas": missing_structure,
        "blocked_obtain": blocked_obtain,
        "machines": machines_out,
        "specialized_parts": specialized_out,
    }


def check() -> list[str]:
    errors: list[str] = []
    expected = io.stable_json(build_census())
    if not CENSUS_PATH.is_file():
        errors.append(io.stale_error(CENSUS_PATH, expected, ""))
        return errors
    actual = CENSUS_PATH.read_text(encoding="utf-8")
    if actual != expected:
        errors.append(io.stale_error(CENSUS_PATH, expected, actual))
    return errors


def close_errors() -> list[str]:
    errors = check()
    if errors:
        return errors
    census = json.loads(CENSUS_PATH.read_text(encoding="utf-8"))
    if census.get("later_host_would_block"):
        errors.append(
            "later large-processing host cards would still record blocked "
            "(missing maps, parts, or D0 grids); see census.json"
        )
    return errors


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    if args.write:
        WAVE.mkdir(parents=True, exist_ok=True)
        io.write_stable(CENSUS_PATH, build_census())
        print(io.relative(CENSUS_PATH))
        return 0
    if args.check:
        errors = check()
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        return 0
    print(io.stable_json(build_census()), end="")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
