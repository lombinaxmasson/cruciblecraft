#!/usr/bin/env python3
"""Landing checks for GT6 Large Crusher 17108."""
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
    / "large_crusher.json"
)
ART = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "assets"
    / "cruciblecraft"
    / "gt6_large_crusher_art_manifest.json"
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
    / "large_crusher"
)


def _load(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def check() -> list[str]:
    errors: list[str] = []
    if not STRUCTURE.is_file():
        return [f"missing {io.relative(STRUCTURE)}"]
    document = _load(STRUCTURE)
    palette = document.get("palette") or {}
    counts = {
        "controller": 0,
        "item_fluid_in": 0,
        "item_fluid_out": 0,
        "energy_input": 0,
        "wall": 0,
        "wheels": 0,
    }
    for row in document.get("structure") or []:
        key = row.get("predicate")
        predicate = palette.get(key) or {}
        kind = predicate.get("type")
        port = predicate.get("port")
        block = predicate.get("block")
        if kind == "controller":
            counts["controller"] += 1
        elif port == "item_fluid_in":
            counts["item_fluid_in"] += 1
        elif port == "item_fluid_out":
            counts["item_fluid_out"] += 1
        elif port == "energy_input":
            counts["energy_input"] += 1
        elif kind == "block" and block == "cruciblecraft:tungstensteel/wall":
            counts["wall"] += 1
        elif kind == "block" and block == "cruciblecraft:multiblock/crusher_wheels":
            counts["wheels"] += 1
        else:
            errors.append(f"unexpected palette {key}: {predicate}")
    if counts["item_fluid_out"] != 24:
        errors.append(f"bottom OUT count {counts['item_fluid_out']}, expected 24")
    if counts["item_fluid_in"] != 9:
        errors.append(f"top wheel IN count {counts['item_fluid_in']}, expected 9")
    if counts["energy_input"] != 2:
        errors.append(f"energy IN count {counts['energy_input']}, expected 2")
    if counts["wheels"] != 9:
        errors.append(f"middle wheel fill count {counts['wheels']}, expected 9")
    if counts["wall"] != 30:
        errors.append(f"structure-only wall count {counts['wall']}, expected 30")
    if counts["controller"] != 1:
        errors.append(f"controller count {counts['controller']}, expected 1")
    if counts["item_fluid_out"] + counts["energy_input"] + counts["wall"] != 56:
        errors.append("tungstensteel wall total is not 56")
    source = document.get("source") or {}
    if source.get("class") != (
        "gregtech.tileentity.multiblocks.MultiTileEntityCrusher"
    ):
        errors.append("structure source class is not MultiTileEntityCrusher")
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
    profile = rows.get("cruciblecraft:large_crusher_profile")
    if profile is None:
        errors.append("missing large_crusher_profile")
    else:
        if profile.get("energy") != "KINETIC_ROTATION":
            errors.append("large crusher energy is not RU")
        if profile.get("inputMinimum") != 512 or profile.get("inputNominal") != 512:
            errors.append("large crusher input window drifted")
        if profile.get("inputMaximum") != 4096:
            errors.append("large crusher inputMaximum drifted")
        if profile.get("parallel") != 64:
            errors.append("large crusher parallel drifted")
        if profile.get("efficiency") != 5000:
            errors.append("large crusher efficiency drifted")
    source_rows = (tiers.get("source") or {}).get("controller_profile_rows") or {}
    if "cruciblecraft:large_crusher_profile" not in source_rows:
        errors.append("controller_profile_rows missing large_crusher_profile")
    return errors
