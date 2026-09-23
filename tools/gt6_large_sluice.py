#!/usr/bin/env python3
"""Repository landing checks for GT6 Large Sluice 17107."""
from __future__ import annotations

import json
import runpy
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
STRUCTURE = ROOT / "src/main/resources/data/cruciblecraft/multiblock_structures/large_sluice.json"
ART = ROOT / "src/main/resources/assets/cruciblecraft/gt6_large_sluice_art_manifest.json"
TIERS = ROOT / "src/main/resources/data/cruciblecraft/machine_tiers.json"
CAPABILITY = ROOT / "tools/capabilities/machines/large-sluice/capability.json"
WAVE_CHECK = ROOT / "tools/waves/machines/large-sluice/gt6_large_sluice.py"


def load(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def check() -> list[str]:
    errors: list[str] = []
    document = load(STRUCTURE)
    palette = document.get("palette", {})
    if len(document.get("structure", [])) != 63:
        errors.append("structure must contain 63 positions")
    counts = {"controller": 0, "item_fluid_in": 0, "item_fluid_out": 0, "energy_input": 0}
    for row in document.get("structure", []):
        predicate = palette.get(row.get("predicate"), {})
        if predicate.get("type") == "controller":
            counts["controller"] += 1
        elif predicate.get("port") in counts:
            counts[predicate["port"]] += 1
    for key, expected in {
        "controller": 1,
        "item_fluid_in": 3,
        "item_fluid_out": 2,
        "energy_input": 2,
    }.items():
        if counts[key] != expected:
            errors.append(f"{key} count {counts[key]}, expected {expected}")
    if document.get("source", {}).get("class") != (
        "gregtech.tileentity.multiblocks.MultiTileEntitySluice"
    ):
        errors.append("structure source class is not MultiTileEntitySluice")

    art = load(ART)
    if not art.get("source_present"):
        errors.append("Large Sluice art source is not present")
    for row in art.get("imports", []):
        relative = str(row.get("destination", "")).split(
            "assets/cruciblecraft/", 1
        )[-1]
        if not (ROOT / "src/main/resources/assets/cruciblecraft" / relative).is_file():
            errors.append(f"missing art {relative}")

    tiers = load(TIERS)
    profiles = {
        row["tierBand"]: row
        for row in tiers.get("controller_profiles", [])
        if "tierBand" in row
    }
    profile = profiles.get("cruciblecraft:large_sluice_profile")
    if profile is None:
        errors.append("missing large_sluice_profile")
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
                errors.append(f"large sluice {key} drifted")

    capability = load(CAPABILITY)
    if capability.get("slug") != "machines/large-sluice":
        errors.append("capability slug drifted")
    if capability.get("workflow") != "accepted":
        errors.append("Large Sluice capability is not accepted")

    wave = runpy.run_path(str(WAVE_CHECK))
    errors.extend(wave["check"]())
    return errors


if __name__ == "__main__":
    problems = check()
    if problems:
        raise SystemExit("\n".join(problems))
    print("GT6 Large Sluice landing checks passed")
