#!/usr/bin/env python3
"""Build T38's source-backed worldgen acquisition overlay.

The T20 closure catalog remains the immutable 129-row core.  T38 adds the
three pinned GT6 large-vein facts needed to acquire its four otherwise
unreachable Roaster dust inputs without claiming that a namespace is a player
path.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
TOOLS = ROOT / "tools"
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import build_t20_worldgen_projection as t20  # noqa: E402

SOURCE = TOOLS / "t20_gt6_worldgen_source.json"
OUTPUT = TOOLS / "t38_source_backed_acquisition.json"
SOURCE_VEINS = (
    {
        "id": "large_t38_gold_sulfide_vein",
        "catalog_material": "gold",
        "source_fact_id": "ore.large.gold",
        "required_materials": ("arsenopyrite", "chalcopyrite"),
    },
    {
        "id": "large_t38_platinum_group_vein",
        "catalog_material": "platinum",
        "source_fact_id": "ore.large.platinum",
        "required_materials": ("cooperite",),
    },
    {
        "id": "large_t38_molybdenum_vein",
        "catalog_material": "molybdenum",
        "source_fact_id": "ore.large.molybdenum",
        "required_materials": ("molybdenite",),
    },
)
REQUIRED_MATERIALS = frozenset(
    material
    for row in SOURCE_VEINS
    for material in row["required_materials"]
)


def stable(value: Any) -> str:
    return json.dumps(value, ensure_ascii=False, indent=2, sort_keys=True) + "\n"


def load(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def _large_source_rows(source: dict[str, Any]) -> dict[str, dict[str, Any]]:
    return {
        str(row["source_fact_id"]): row
        for row in source.get("large_veins") or []
        if isinstance(row, dict) and row.get("source_fact_id")
    }


def build() -> dict[str, Any]:
    source = load(SOURCE)
    if source.get("status") != "T20_GT6_WORLDGEN_SOURCE_READY":
        raise ValueError("T20 GT6 worldgen source is not ready")
    source_revision = (source.get("source") or {}).get("revision")
    if source_revision != "3703e40308c8c030763fd6297dea8b210d2a77b1":
        raise ValueError("T20 worldgen source revision drifted")

    source_rows = _large_source_rows(source)
    veins: list[dict[str, Any]] = []
    discovered_materials: set[str] = set()
    for declaration in SOURCE_VEINS:
        source_row = source_rows.get(declaration["source_fact_id"])
        if source_row is None:
            raise ValueError(
                f"missing pinned GT6 source fact {declaration['source_fact_id']}"
            )
        source_layers = {
            str(layer.get("cc_material") or "")
            for layer in (source_row.get("layers") or {}).values()
            if isinstance(layer, dict)
        }
        required = set(declaration["required_materials"])
        if not required <= source_layers:
            raise ValueError(
                f"{declaration['source_fact_id']} does not cover "
                f"{sorted(required - source_layers)}"
            )
        values, provenance = t20.large_projection(
            declaration["catalog_material"], source_row, source_layers
        )
        provenance["t38_player_path_materials"] = sorted(required)
        veins.append({
            "id": declaration["id"],
            "catalog_material": declaration["catalog_material"],
            "profile_version": 2,
            **values,
            "salt": t20.stable_salt(declaration["id"]),
            "provenance": provenance,
        })
        discovered_materials.update(required)
    if discovered_materials != REQUIRED_MATERIALS:
        raise ValueError("T38 source-backed worldgen does not cover every input")
    if len({row["id"] for row in veins}) != len(veins):
        raise ValueError("T38 source-backed worldgen ids must be unique")
    if len({row["salt"] for row in veins}) != len(veins):
        raise ValueError("T38 source-backed worldgen salts must be unique")
    source_materials = {
        entry["material"]
        for vein in veins
        for layer in ("top", "bottom", "between", "spread")
        for entry in vein[layer]
    }
    required_forms = {
        material: ["ore", "raw_ore"]
        for material in sorted(source_materials)
    }
    for material in REQUIRED_MATERIALS:
        required_forms[material] = [
            "ore",
            "raw_ore",
            "crushed_ore",
            "washed_crushed_ore",
            "centrifuged_crushed_ore",
            "purified_dust",
            "dust",
        ]
    return {
        "schema_version": 1,
        "status": "T38_SOURCE_BACKED_ACQUISITION_READY",
        "generated_by": "python tools/build_t38_source_backed_acquisition.py",
        "source_revision": source_revision,
        "source": {
            "path": SOURCE.relative_to(ROOT).as_posix(),
            "sha256": sha256(SOURCE),
        },
        "core_catalog": {
            "path": "src/main/resources/data/cruciblecraft/worldgen_catalog/ore_veins.json",
            "preserved_rows": 129,
        },
        "required_materials": sorted(REQUIRED_MATERIALS),
        "required_forms": required_forms,
        "veins": veins,
    }


def write() -> dict[str, Any]:
    document = build()
    OUTPUT.write_text(stable(document), encoding="utf-8", newline="\n")
    return document


def check() -> list[str]:
    if not OUTPUT.is_file():
        return [f"missing generated file: {OUTPUT.relative_to(ROOT)}"]
    expected = stable(build())
    actual = OUTPUT.read_text(encoding="utf-8")
    return [] if actual == expected else [
        f"stale generated file: {OUTPUT.relative_to(ROOT)}"
    ]


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args(argv)
    if args.write == args.check:
        parser.error("choose exactly one of --write or --check")
    if args.check:
        errors = check()
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        print(f"{OUTPUT.relative_to(ROOT)} is current")
        return 0
    document = write()
    print(json.dumps({
        "status": document["status"],
        "veins": len(document["veins"]),
        "materials": document["required_materials"],
    }, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
