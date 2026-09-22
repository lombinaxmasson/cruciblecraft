#!/usr/bin/env python3
"""Landing checks for GT6 Large Shredder 17109."""
from __future__ import annotations

import runpy
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[4]
BUILDER = Path(__file__).with_name("build_large_shredder.py")


def _load(path: Path) -> Any:
    import json

    return json.loads(path.read_text(encoding="utf-8"))


def check() -> list[str]:
    namespace = runpy.run_path(str(BUILDER))
    errors = list(namespace["check"]())
    structure = _load(
        ROOT
        / "src/main/resources/data/cruciblecraft/multiblock_structures/large_shredder.json"
    )
    predicates = [
        (structure.get("palette") or {}).get(row.get("predicate"), {})
        for row in structure.get("structure") or []
    ]
    if len(predicates) != 75:
        errors.append("Large Shredder structure must contain 75 positions")
    if (structure.get("source") or {}).get("class") != (
        "gregtech.tileentity.multiblocks.MultiTileEntityShredder"
    ):
        errors.append("structure source is not MultiTileEntityShredder")
    return errors


if __name__ == "__main__":
    problems = check()
    if problems:
        raise SystemExit("\n".join(problems))
    print("GT6 Large Shredder landing checks passed")
