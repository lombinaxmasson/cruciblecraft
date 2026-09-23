#!/usr/bin/env python3
"""Landing checks for GT6 Implosion Compressor 17110."""
from __future__ import annotations

import json
import runpy
from pathlib import Path

ROOT = Path(__file__).resolve().parents[4]
WAVE = ROOT / "tools/waves/machines/implosion-compressor"
STRUCTURE = ROOT / (
    "src/main/resources/data/cruciblecraft/multiblock_structures/"
    "implosion_compressor.json"
)
ART = ROOT / (
    "src/main/resources/assets/cruciblecraft/"
    "gt6_implosion_compressor_art_manifest.json"
)
CAPABILITY = ROOT / (
    "tools/capabilities/machines/implosion-compressor/capability.json"
)


def load(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


def check() -> list[str]:
    errors: list[str] = []
    errors.extend(runpy.run_path(
        str(WAVE / "build_implosion_compressor.py"))["check"]())
    if not STRUCTURE.is_file():
        errors.append("missing implosion structure")
    else:
        structure = load(STRUCTURE)
        palette = structure.get("palette") or {}
        ports = 0
        walls = 0
        controllers = 0
        air = 0
        for row in structure.get("structure") or []:
            predicate = palette.get(row.get("predicate")) or {}
            if predicate.get("type") == "controller":
                controllers += 1
            elif predicate.get("type") == "air":
                air += 1
            elif predicate.get("port") == "item_fluid_energy":
                ports += 1
            if predicate.get("block") == (
                    "cruciblecraft:multiblock/dense_tungstensteel_wall"):
                walls += 1
        if len(structure.get("structure") or []) != 27:
            errors.append("structure must contain 27 positions")
        if ports != 25:
            errors.append(f"expected 25 ITEM_FLUID_ENERGY ports, got {ports}")
        if walls != 25:
            errors.append(f"expected 25 dense tungstensteel walls, got {walls}")
        if air != 1 or controllers != 1:
            errors.append("structure must contain one air and one controller")
        if structure.get("source", {}).get("class") != (
                "gregtech.tileentity.multiblocks."
                "MultiTileEntityImplosionCompressor"):
            errors.append("structure source class drifted")
    if not ART.is_file():
        errors.append("missing implosion art manifest")
    else:
        art = load(ART)
        if not art.get("source_present"):
            errors.append("GT6 implosion art source is not present")
        for row in art.get("imports") or []:
            relative = str(row.get("destination") or "").split(
                "assets/cruciblecraft/", 1)[-1]
            if not (ROOT / "src/main/resources/assets/cruciblecraft" / relative).is_file():
                errors.append(f"missing art {relative}")
    if not CAPABILITY.is_file():
        errors.append("missing implosion capability")
    else:
        capability = load(CAPABILITY)
        if capability.get("slug") != "machines/implosion-compressor":
            errors.append("capability slug drifted")
        if capability.get("maturity") != "runtime_ready":
            errors.append("capability is not runtime_ready")
    return errors


if __name__ == "__main__":
    problems = check()
    if problems:
        raise SystemExit("\n".join(problems))
    print("GT6 Implosion Compressor landing checks passed")
