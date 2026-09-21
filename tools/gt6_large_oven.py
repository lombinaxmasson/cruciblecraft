#!/usr/bin/env python3
"""Landing checks for GT6 Large Electric Oven 17106."""
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
    / "large_oven.json"
)
ART = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "assets"
    / "cruciblecraft"
    / "gt6_large_oven_art_manifest.json"
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
    / "large_oven"
)


def _load(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def check() -> list[str]:
    errors: list[str] = []
    if not STRUCTURE.is_file():
        return [f"missing {io.relative(STRUCTURE)}"]
    document = _load(STRUCTURE)
    palette = document.get("palette") or {}
    walls = coils = air = controllers = 0
    for row in document.get("structure") or []:
        key = row.get("predicate")
        predicate = palette.get(key) or {}
        kind = predicate.get("type")
        port = predicate.get("port")
        if kind == "controller":
            controllers += 1
        elif kind == "air":
            air += 1
        elif kind == "tag":
            coils += 1
            if predicate.get("tag") != "cruciblecraft:large_oven_coils":
                errors.append(f"unexpected coil tag {predicate}")
            if predicate.get("uniform_group") != "oven_coils":
                errors.append(f"oven coils missing uniform_group {predicate}")
        elif port == "item_fluid_energy":
            walls += 1
            if predicate.get("block") != "cruciblecraft:invar/wall":
                errors.append(f"unexpected wall {predicate}")
        else:
            errors.append(f"unexpected palette {key}: {predicate}")
    if walls != 17:
        errors.append(f"invar wall count {walls}, expected 17")
    if coils != 8:
        errors.append(f"nichrome coil count {coils}, expected 8")
    if air != 1:
        errors.append(f"air count {air}, expected 1")
    if controllers != 1:
        errors.append(f"controller count {controllers}, expected 1")
    source = document.get("source") or {}
    if source.get("class") != (
        "gregtech.tileentity.multiblocks.MultiTileEntityOven"
    ):
        errors.append("structure source class is not MultiTileEntityOven")
    if source.get("revision") != io.SOURCE_REVISION:
        errors.append("structure source revision drifted")
    art = _load(ART) if ART.is_file() else {}
    if art.get("source_revision") != io.SOURCE_REVISION:
        errors.append("art manifest revision drifted")
    for face in ("front", "back", "left", "right", "top", "bottom"):
        for layer in ("colored", "overlay", "overlay_active", "overlay_running"):
            path = TEXTURES / layer / f"{face}.png"
            if not path.is_file():
                errors.append(f"missing texture {io.relative(path)}")
    tiers = _load(TIERS)
    rows = {
        row["tierBand"]: row
        for row in tiers.get("controller_profiles") or []
    }
    profile = rows.get("cruciblecraft:large_oven_profile")
    if profile is None:
        errors.append("missing large_oven_profile")
    else:
        if profile.get("energy") != "ELECTRIC":
            errors.append("large oven energy is not ELECTRIC")
        if profile.get("inputMinimum") != 512 or profile.get("inputNominal") != 512:
            errors.append("large oven input window drifted")
        if profile.get("inputMaximum") != 4096:
            errors.append("large oven inputMaximum drifted")
        if profile.get("parallel") != 64:
            errors.append("large oven parallel drifted")
        if profile.get("efficiency") != 2500:
            errors.append("large oven efficiency drifted")
    source_rows = (tiers.get("source") or {}).get("controller_profile_rows") or {}
    if "cruciblecraft:large_oven_profile" not in source_rows:
        errors.append("controller_profile_rows missing large_oven_profile")
    return errors
