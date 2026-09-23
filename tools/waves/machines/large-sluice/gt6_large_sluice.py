#!/usr/bin/env python3
"""Landing checks for the GT6 Large Sluice 17107 wave."""
from __future__ import annotations

import json
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[4]
STRUCTURE = ROOT / "src/main/resources/data/cruciblecraft/multiblock_structures/large_sluice.json"
CONTRACT = Path(__file__).with_name("source_contract.json")
D0 = Path(__file__).with_name("d0_obtain_matrix.json")


def load(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def check() -> list[str]:
    errors: list[str] = []
    document = load(STRUCTURE)
    if len(document.get("structure", [])) != 63:
        errors.append("Large Sluice structure must contain 63 positions")
    palette = document.get("palette", {})
    counts = {"controller": 0, "item_fluid_in": 0, "item_fluid_out": 0, "energy_input": 0}
    for row in document.get("structure", []):
        predicate = palette.get(row.get("predicate"), {})
        if predicate.get("type") == "controller":
            counts["controller"] += 1
        elif predicate.get("port") in counts:
            counts[predicate["port"]] += 1
    expected = {
        "controller": 1,
        "item_fluid_in": 3,
        "item_fluid_out": 2,
        "energy_input": 2,
    }
    for key, value in expected.items():
        if counts[key] != value:
            errors.append(f"{key} count {counts[key]}, expected {value}")
    source = document.get("source", {})
    if source.get("class") != "gregtech.tileentity.multiblocks.MultiTileEntitySluice":
        errors.append("structure source class drifted")
    if load(CONTRACT)["gt6"]["loader_meta"] != 17107:
        errors.append("source contract meta drifted")
    if load(D0).get("status") != "source_exact":
        errors.append("D0 obtain matrix is not source_exact")
    return errors


if __name__ == "__main__":
    problems = check()
    if problems:
        raise SystemExit("\n".join(problems))
    print("GT6 Large Sluice landing checks passed")
