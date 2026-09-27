#!/usr/bin/env python3
"""Landing checks for GT6 Large Electrolyzer 17103."""
from __future__ import annotations

import json
from pathlib import Path
from typing import Any

from tools import io_common as io

SLUG = "machines/large-electrolyzer"
ROOT = io.ROOT
STRUCTURE = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "multiblock_structures"
    / "large_electrolyzer.json"
)
ART = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "assets"
    / "cruciblecraft"
    / "gt6_large_electrolyzer_art_manifest.json"
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
    / "large_electrolyzer"
)
PART_TEXTURES = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "assets"
    / "cruciblecraft"
    / "textures"
    / "block"
    / "machine"
    / "electrolyzer_part"
)


def _load(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def check() -> list[str]:
    errors: list[str] = []
    if not STRUCTURE.is_file():
        return [f"missing {io.relative(STRUCTURE)}"]
    document = _load(STRUCTURE)
    palette = document.get("palette") or {}
    energy_ins = outs = controllers = 0
    for row in document.get("structure") or []:
        key = row.get("predicate")
        predicate = palette.get(key) or {}
        kind = predicate.get("type")
        port = predicate.get("port")
        if kind == "controller":
            controllers += 1
        elif port == "item_fluid_energy_in":
            energy_ins += 1
        elif port == "item_fluid_out":
            outs += 1
        else:
            errors.append(f"unexpected palette {key}: {predicate}")
    if energy_ins != 8:
        errors.append(
            f"bottom ITEM_FLUID_ENERGY_IN count {energy_ins}, expected 8"
        )
    if outs != 9:
        errors.append(f"top ITEM_FLUID_OUT count {outs}, expected 9")
    if controllers != 1:
        errors.append(f"controller count {controllers}, expected 1")
    source = document.get("source") or {}
    if source.get("class") != (
        "gregtech.tileentity.multiblocks.MultiTileEntityElectrolyzer"
    ):
        errors.append("structure source class is not MultiTileEntityElectrolyzer")
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
    for design in range(8):
        for layer in ("colored", "overlay"):
            for face in ("top", "side", "bottom"):
                path = PART_TEXTURES / str(design) / layer / f"{face}.png"
                if not path.is_file():
                    errors.append(f"missing texture {io.relative(path)}")
    tiers = _load(TIERS)
    rows = {
        row["tierBand"]: row
        for row in tiers.get("controller_profiles") or []
    }
    profile = rows.get("cruciblecraft:large_electrolyzer_profile")
    if profile is None:
        errors.append("missing large_electrolyzer_profile")
    else:
        if profile.get("energy") != "ELECTRIC":
            errors.append("large electrolyzer energy is not ELECTRIC")
        if profile.get("inputMinimum") != 512 or profile.get("inputNominal") != 512:
            errors.append("large electrolyzer input window drifted")
        if profile.get("inputMaximum") != 4096:
            errors.append("large electrolyzer inputMaximum drifted")
        if profile.get("parallel") != 16:
            errors.append("large electrolyzer parallel drifted")
        if profile.get("efficiency") != 5000:
            errors.append("large electrolyzer efficiency drifted")
    source_rows = (tiers.get("source") or {}).get("controller_profile_rows") or {}
    if "cruciblecraft:large_electrolyzer_profile" not in source_rows:
        errors.append("controller_profile_rows missing large_electrolyzer_profile")
    return errors
