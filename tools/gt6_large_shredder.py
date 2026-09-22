#!/usr/bin/env python3
"""Landing checks for GT6 Large Shredder 17109."""
from __future__ import annotations

import json
import runpy
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
    / "large_shredder.json"
)
ART = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "assets"
    / "cruciblecraft"
    / "gt6_large_shredder_art_manifest.json"
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
CAPABILITY = ROOT / "tools/capabilities/machines/large-shredder/capability.json"
WAVE_CHECK = (
    ROOT
    / "tools"
    / "waves"
    / "machines"
    / "large-shredder"
    / "gt6_large_shredder.py"
)


def _load(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def _destination(root: Path, manifest_path: str) -> Path:
    prefix = "assets/cruciblecraft/"
    relative = manifest_path.removeprefix(prefix)
    return root / "src/main/resources/assets/cruciblecraft" / relative


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
        "blades": 0,
    }
    for row in document.get("structure") or []:
        predicate = palette.get(row.get("predicate")) or {}
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
        elif (
            kind == "block"
            and block == "cruciblecraft:multiblock/shredder_blades"
        ):
            counts["blades"] += 1
        else:
            errors.append(f"unexpected palette {row.get('predicate')}: {predicate}")

    if len(document.get("structure") or []) != 75:
        errors.append("structure must contain 75 positions")
    if counts["controller"] != 1:
        errors.append(f"controller count {counts['controller']}, expected 1")
    if counts["item_fluid_in"] != 9:
        errors.append(f"top blade IN count {counts['item_fluid_in']}, expected 9")
    if counts["item_fluid_out"] != 24:
        errors.append(f"bottom OUT count {counts['item_fluid_out']}, expected 24")
    if counts["energy_input"] != 2:
        errors.append(f"energy IN count {counts['energy_input']}, expected 2")
    if counts["blades"] != 9:
        errors.append(f"middle blade count {counts['blades']}, expected 9")
    if (
        counts["wall"]
        + counts["item_fluid_out"]
        + counts["energy_input"]
        != 56
    ):
        errors.append("tungstensteel wall total is not 56")

    source = document.get("source") or {}
    if source.get("class") != (
        "gregtech.tileentity.multiblocks.MultiTileEntityShredder"
    ):
        errors.append("structure source class is not MultiTileEntityShredder")
    if source.get("revision") != io.SOURCE_REVISION:
        errors.append("structure source revision drifted")

    if not ART.is_file():
        errors.append(f"missing {io.relative(ART)}")
    else:
        art = _load(ART)
        if art.get("source_revision") != io.SOURCE_REVISION:
            errors.append("art manifest revision drifted")
        if len(art.get("imports") or []) < 49:
            errors.append("Large Shredder art manifest is incomplete")
        for row in art.get("imports") or []:
            destination = _destination(ROOT, str(row.get("destination") or ""))
            if not destination.is_file():
                errors.append(f"missing art {io.relative(destination)}")

    tiers = _load(TIERS)
    rows = {
        row["tierBand"]: row
        for row in tiers.get("controller_profiles") or []
        if "tierBand" in row
    }
    profile = rows.get("cruciblecraft:large_shredder_profile")
    if profile is None:
        errors.append("missing large_shredder_profile")
    else:
        expected = {
            "energy": "KINETIC_ROTATION",
            "inputMinimum": 512,
            "inputNominal": 512,
            "inputMaximum": 4096,
            "energyCapacity": 4096,
            "parallel": 64,
            "efficiency": 5000,
        }
        for key, value in expected.items():
            if profile.get(key) != value:
                errors.append(f"large shredder {key} drifted")
    source_rows = (tiers.get("source") or {}).get("controller_profile_rows") or {}
    if "cruciblecraft:large_shredder_profile" not in source_rows:
        errors.append("controller_profile_rows missing large_shredder_profile")

    if not CAPABILITY.is_file():
        errors.append(f"missing {io.relative(CAPABILITY)}")
    else:
        capability = _load(CAPABILITY)
        if capability.get("slug") != "machines/large-shredder":
            errors.append("capability slug drifted")
        if capability.get("maturity") != "runtime_ready":
            errors.append("Large Shredder capability is not runtime_ready")
        if capability.get("workflow") != "accepted":
            errors.append("Large Shredder capability is not accepted")

    if WAVE_CHECK.is_file():
        wave = runpy.run_path(str(WAVE_CHECK))
        errors.extend(wave["check"]())
    else:
        errors.append(f"missing {io.relative(WAVE_CHECK)}")
    return errors


if __name__ == "__main__":
    problems = check()
    if problems:
        raise SystemExit("\n".join(problems))
    print("GT6 Large Shredder landing checks passed")
