#!/usr/bin/env python3
"""Landing checks for GT6 Large Bathing Vat 17104."""
from __future__ import annotations

import json
from pathlib import Path
from typing import Any

from tools import io_common as io

ROOT = io.ROOT
STRUCTURE = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "multiblock_structures"
    / "large_bath.json"
)
ART = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "assets"
    / "cruciblecraft"
    / "gt6_large_bath_art_manifest.json"
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
    / "large_bath"
)
CRAFT = (
    ROOT
    / "src"
    / "generated"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "recipe"
    / "machines"
    / "large_bath.json"
)


def _load(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def check() -> list[str]:
    errors: list[str] = []
    if not STRUCTURE.is_file():
        return [f"missing {io.relative(STRUCTURE)}"]
    document = _load(STRUCTURE)
    palette = document.get("palette") or {}
    item_fluid = controllers = 0
    for row in document.get("structure") or []:
        predicate = palette.get(row.get("predicate")) or {}
        kind = predicate.get("type")
        port = predicate.get("port")
        if kind == "controller":
            controllers += 1
        elif port == "item_fluid":
            item_fluid += 1
        else:
            errors.append(f"unexpected palette {row.get('predicate')}: {predicate}")
    if item_fluid != 49:
        errors.append(f"ITEM_FLUID count {item_fluid}, expected 49")
    if controllers != 1:
        errors.append(f"controller count {controllers}, expected 1")
    source = document.get("source") or {}
    if source.get("class") != "gregtech.tileentity.multiblocks.MultiTileEntityBath":
        errors.append("structure source class is not MultiTileEntityBath")
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
    profile = rows.get("cruciblecraft:large_bath_profile")
    if profile is None:
        errors.append("missing large_bath_profile")
    else:
        if profile.get("energy") != "TIME":
            errors.append("large bath energy is not TIME")
        if profile.get("inputMinimum") != 1 or profile.get("inputNominal") != 1:
            errors.append("large bath input window drifted")
        if profile.get("inputMaximum") != 16:
            errors.append("large bath inputMaximum drifted")
        if profile.get("parallel") != 64:
            errors.append("large bath parallel drifted")
    source_rows = (tiers.get("source") or {}).get("controller_profile_rows") or {}
    if "cruciblecraft:large_bath_profile" not in source_rows:
        errors.append("controller_profile_rows missing large_bath_profile")
    if not CRAFT.is_file():
        errors.append(f"missing {io.relative(CRAFT)}")
    else:
        craft = _load(CRAFT)
        if craft.get("pattern") != ["CRC", "PMP", "APA"]:
            errors.append("large bath craft grid drifted from GT6 CRC/PMP/APA")
        plate = (craft.get("ingredients") or {}).get("P") or {}
        if plate.get("items") != "cruciblecraft:dense_plate":
            errors.append("dense plate is not the shared prefix Item")
        components = plate.get("components") or {}
        if components.get("cruciblecraft:prefix_material") != "stainless_steel":
            errors.append("dense plate is not keyed to stainless_steel")
    return errors
