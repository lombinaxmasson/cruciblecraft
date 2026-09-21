#!/usr/bin/env python3
"""Landing checks for GT6 Large Autoclave 17112."""
from __future__ import annotations

import json
from pathlib import Path
from typing import Any

from tools import io_common as io

SLUG = "machines/large-autoclave"
ROOT = io.ROOT
STRUCTURE = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "multiblock_structures"
    / "large_autoclave.json"
)
ART = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "assets"
    / "cruciblecraft"
    / "gt6_large_autoclave_art_manifest.json"
)
TIERS = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "machine_tiers.json"
)
TEXTURES = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "assets"
    / "cruciblecraft"
    / "textures"
    / "block"
    / "machine"
    / "large_autoclave"
)


def _load(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def check() -> list[str]:
    errors: list[str] = []
    if not STRUCTURE.is_file():
        return [f"missing {io.relative(STRUCTURE)}"]
    document = _load(STRUCTURE)
    palette = document.get("palette") or {}
    walls = air = controllers = 0
    for row in document.get("structure") or []:
        key = row.get("predicate")
        predicate = palette.get(key) or {}
        kind = predicate.get("type")
        port = predicate.get("port")
        if kind == "controller":
            controllers += 1
        elif kind == "air":
            air += 1
        elif port == "item_fluid_energy":
            walls += 1
        else:
            errors.append(f"unexpected palette {key}: {predicate}")
    if walls != 25:
        errors.append(f"ITEM_FLUID_ENERGY wall count {walls}, expected 25")
    if air != 1:
        errors.append(f"air count {air}, expected 1")
    if controllers != 1:
        errors.append(f"controller count {controllers}, expected 1")
    source = document.get("source") or {}
    if source.get("class") != (
        "gregtech.tileentity.multiblocks.MultiTileEntityAutoclave"
    ):
        errors.append("structure source class is not MultiTileEntityAutoclave")
    if source.get("revision") != io.SOURCE_REVISION:
        errors.append("structure source revision drifted")
    art = _load(ART) if ART.is_file() else {}
    if art.get("source_revision") != io.SOURCE_REVISION:
        errors.append("art manifest revision drifted")
    for face in ("front", "back", "left", "right", "top", "bottom"):
        for layer in ("colored", "overlay"):
            path = TEXTURES / layer / f"{face}.png"
            if not path.is_file():
                errors.append(f"missing texture {io.relative(path)}")
    tiers = _load(TIERS)
    rows = {
        row["tierBand"]: row
        for row in tiers.get("controller_profiles") or []
    }
    profile = rows.get("cruciblecraft:large_autoclave_profile")
    if profile is None:
        errors.append("missing large_autoclave_profile")
    else:
        if profile.get("energy") != "TIME":
            errors.append("large autoclave energy is not TIME")
        if profile.get("inputMinimum") != 1 or profile.get("inputNominal") != 1:
            errors.append("large autoclave input window drifted")
        if profile.get("inputMaximum") != 16:
            errors.append("large autoclave inputMaximum drifted")
        if profile.get("parallel") != 16:
            errors.append("large autoclave parallel drifted")
        if profile.get("efficiency") != 10000:
            errors.append("large autoclave efficiency drifted")
    source_rows = (tiers.get("source") or {}).get("controller_profile_rows") or {}
    if "cruciblecraft:large_autoclave_profile" not in source_rows:
        errors.append("controller_profile_rows missing large_autoclave_profile")
    io_java = (
        ROOT
        / "src"
        / "main"
        / "java"
        / "com"
        / "masson"
        / "cruciblecraft"
        / "machine"
        / "processing"
        / "Gt6SidedIo.java"
    )
    io_text = io_java.read_text(encoding="utf-8") if io_java.is_file() else ""
    if 'put("large_autoclave"' not in io_text:
        errors.append("Gt6SidedIo is missing the 17112 large_autoclave profile")
    if 'put("autoclave"' not in io_text:
        errors.append("Gt6SidedIo dropped the 22004 autoclave profile")
    return errors
