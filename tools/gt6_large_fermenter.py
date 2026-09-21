#!/usr/bin/env python3
"""GT6 Large Fermenter 17113: structure, art, controller profile checks."""
from __future__ import annotations

import argparse
import json
import shutil
from pathlib import Path
from typing import Any

from tools import io_common as io

SLUG = "machines/large-fermenter"
SOURCE_REVISION = io.SOURCE_REVISION
ROOT = io.ROOT
STRUCTURE = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "multiblock_structures"
    / "large_fermenter.json"
)
ART = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "assets"
    / "cruciblecraft"
    / "gt6_large_fermenter_art_manifest.json"
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
    / "large_fermenter"
)
GUI = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "assets"
    / "cruciblecraft"
    / "textures"
    / "gui"
    / "machines"
    / "fermenter.png"
)
GT6_W = ROOT / "gt6_referencable_port_code" / "gregtech6_w"
GT6_BLOCK = (
    Path("src/main/resources/assets/gregtech/textures/blocks/machines")
    / "basicmachines"
    / "largefermenter"
)
GT6_GUI = Path(
    "src/main/resources/assets/gregtech/textures/gui/machines/fermenter.png"
)
GT6_WALL = (
    Path("src/main/resources/assets/gregtech/textures/blocks/machines")
    / "multiblockparts"
    / "metalwall"
    / "7"
)
WALL_DEST = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "assets"
    / "cruciblecraft"
    / "textures"
    / "block"
    / "gt6_import"
    / "multiblockparts"
    / "metalwall"
    / "7"
)
FACES = ("front", "back", "left", "right", "top", "bottom")
LAYERS = ("colored", "overlay", "overlay_active", "overlay_running")
WALL_DESIGN = ("colored", "overlay")
WALL_FACES = ("bottom", "top", "side")


def _load(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def _write_json(path: Path, document: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(
        json.dumps(document, indent=2, ensure_ascii=False) + "\n",
        encoding="utf-8",
    )


def structure_document() -> dict[str, Any]:
    structure: list[dict[str, Any]] = []
    for y in (-1, 0, 1):
        for z in range(5):
            for x in range(-2, 3):
                if y == 0 and x == 0 and z == 0:
                    structure.append(
                        {"offset": [x, y, z], "predicate": "C"}
                    )
                elif y == -1:
                    structure.append(
                        {"offset": [x, y, z], "predicate": "E"}
                    )
                else:
                    structure.append(
                        {"offset": [x, y, z], "predicate": "I"}
                    )
    return {
        "schema_version": 1,
        "palette": {
            "I": {
                "type": "port",
                "block": "cruciblecraft:stainless_steel/wall",
                "port": "item_fluid",
            },
            "E": {
                "type": "port",
                "block": "cruciblecraft:multiblock/heat_transmitter",
                "port": "energy_input",
            },
            "C": {
                "type": "controller",
                "block": "cruciblecraft:large_fermenter",
            },
        },
        "structure": structure,
        "anchors": {
            "center": [0, 0, 2],
            "bottom_energy": [0, -1, 2],
            "item_emit": [0, 1, 5],
            "fluid_emit": [0, 0, 5],
        },
        "source": {
            "repository": "https://github.com/GregTech6/gregtech6",
            "revision": SOURCE_REVISION,
            "class": "gregtech.tileentity.multiblocks.MultiTileEntityFermenter",
            "method": "checkStructure2",
        },
    }


def art_manifest() -> dict[str, Any]:
    imports: list[dict[str, str]] = []
    for layer in LAYERS:
        for face in FACES:
            rel = f"{GT6_BLOCK.as_posix()}/{layer}/{face}.png"
            imports.append(
                {
                    "destination": (
                        "assets/cruciblecraft/textures/block/machine/"
                        f"large_fermenter/{layer}/{face}.png"
                    ),
                    "gt6_source": rel,
                    "source": "gt6_referencable_port_code/gregtech6_w",
                }
            )
    imports.append(
        {
            "destination": "assets/cruciblecraft/textures/gui/machines/fermenter.png",
            "gt6_source": GT6_GUI.as_posix(),
            "source": "gt6_referencable_port_code/gregtech6_w",
        }
    )
    for layer in WALL_DESIGN:
        for face in WALL_FACES:
            imports.append(
                {
                    "destination": (
                        "assets/cruciblecraft/textures/block/gt6_import/"
                        f"multiblockparts/metalwall/7/{layer}/{face}.png"
                    ),
                    "gt6_source": f"{GT6_WALL.as_posix()}/{layer}/{face}.png",
                    "source": "gt6_referencable_port_code/gregtech6_w",
                }
            )
    return {
        "imports": imports,
        "source": "gt6_referencable_port_code/gregtech6_w",
        "source_present": GT6_W.is_dir(),
        "source_revision": SOURCE_REVISION,
    }


def copy_art() -> None:
    if not GT6_W.is_dir():
        raise FileNotFoundError(str(GT6_W))
    for layer in LAYERS:
        dest_dir = TEXTURES / layer
        dest_dir.mkdir(parents=True, exist_ok=True)
        for face in FACES:
            src = GT6_W / GT6_BLOCK / layer / f"{face}.png"
            shutil.copy2(src, dest_dir / f"{face}.png")
    GUI.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(GT6_W / GT6_GUI, GUI)
    for layer in WALL_DESIGN:
        dest_dir = WALL_DEST / layer
        dest_dir.mkdir(parents=True, exist_ok=True)
        for face in WALL_FACES:
            shutil.copy2(
                GT6_W / GT6_WALL / layer / f"{face}.png",
                dest_dir / f"{face}.png",
            )


def write() -> None:
    _write_json(STRUCTURE, structure_document())
    copy_art()
    _write_json(ART, art_manifest())


def check() -> list[str]:
    errors: list[str] = []
    if not STRUCTURE.is_file():
        return [f"missing {io.relative(STRUCTURE)}"]
    document = _load(STRUCTURE)
    palette = document.get("palette") or {}
    ins = energy = controllers = 0
    for row in document.get("structure") or []:
        key = row.get("predicate")
        predicate = palette.get(key) or {}
        kind = predicate.get("type")
        port = predicate.get("port")
        if kind == "controller":
            controllers += 1
        elif port == "item_fluid":
            ins += 1
        elif port == "energy_input":
            energy += 1
        else:
            errors.append(f"unexpected palette {key}: {predicate}")
    if energy != 25:
        errors.append(f"heat-transmitter count {energy}, expected 25")
    if ins != 49:
        errors.append(f"stainless wall count {ins}, expected 49")
    if controllers != 1:
        errors.append(f"controller count {controllers}, expected 1")
    if len(document.get("structure") or []) != 75:
        errors.append(
            f"structure size {len(document.get('structure') or [])}, expected 75"
        )
    source = document.get("source") or {}
    if source.get("class") != (
        "gregtech.tileentity.multiblocks.MultiTileEntityFermenter"
    ):
        errors.append("structure source class is not MultiTileEntityFermenter")
    if source.get("revision") != SOURCE_REVISION:
        errors.append("structure source revision drifted")
    art = _load(ART) if ART.is_file() else {}
    if art.get("source_revision") != SOURCE_REVISION:
        errors.append("art manifest revision drifted")
    for layer in LAYERS:
        for face in FACES:
            path = TEXTURES / layer / f"{face}.png"
            if not path.is_file():
                errors.append(f"missing texture {io.relative(path)}")
    for layer in WALL_DESIGN:
        for face in WALL_FACES:
            path = WALL_DEST / layer / f"{face}.png"
            if not path.is_file():
                errors.append(f"missing texture {io.relative(path)}")
    if not GUI.is_file():
        errors.append(f"missing GUI {io.relative(GUI)}")
    tiers = _load(TIERS)
    rows = {
        row["tierBand"]: row
        for row in tiers.get("controller_profiles") or []
    }
    profile = rows.get("cruciblecraft:large_fermenter_profile")
    if profile is None:
        errors.append("missing large_fermenter_profile")
    else:
        if profile.get("energy") != "HEAT":
            errors.append("large fermenter energy is not HEAT")
        if profile.get("inputMinimum") != 1 or profile.get("inputNominal") != 512:
            errors.append("large fermenter input window drifted")
        if profile.get("inputMaximum") != 4096:
            errors.append("large fermenter inputMaximum drifted")
        if profile.get("parallel") != 256:
            errors.append("large fermenter parallel drifted")
        if profile.get("efficiency") != 10000:
            errors.append("large fermenter efficiency drifted")
    source_rows = (tiers.get("source") or {}).get("controller_profile_rows") or {}
    if "cruciblecraft:large_fermenter_profile" not in source_rows:
        errors.append("controller_profile_rows missing large_fermenter_profile")
    return errors


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args(argv)
    if args.write:
        write()
        print("wrote large fermenter structure and art")
    if args.check or not args.write:
        errors = check()
        if errors:
            print("\n".join(errors))
            return 1
        print("large fermenter check passed")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
