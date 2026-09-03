#!/usr/bin/env python3
"""Convert GT6 setBlockBounds JSON models into CC block models.

GT6 exports default to facing south (Z+). CC ``horizontalBlock`` models face
north, so horizontal machines are rotated 180° around Y. Textures are remapped
to existing CC assets; tinted machines get ``tintindex`` 0 on ``#texture``.
"""
from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "textures" / "gt6模型"
OUT = ROOT / "src" / "main" / "resources" / "assets" / "cruciblecraft" / "models" / "block"

METAL = "cruciblecraft:block/metal_surface"
FIREBRICK = "cruciblecraft:block/firebrick"
MOLTEN = "cruciblecraft:block/ceramic_mold_filled_top"

FACE_180 = {
    "north": "south",
    "south": "north",
    "east": "west",
    "west": "east",
    "up": "up",
    "down": "down",
}


def load(name: str) -> dict:
    return json.loads((SRC / name).read_text(encoding="utf-8"))


def rotate_180_y(elements: list[dict]) -> list[dict]:
    rotated = []
    for element in elements:
        fx, fy, fz = element["from"]
        tx, ty, tz = element["to"]
        faces = {}
        for direction, face in element.get("faces", {}).items():
            faces[FACE_180.get(direction, direction)] = dict(face)
        rotated.append(
            {
                **{key: value for key, value in element.items() if key not in {"from", "to", "faces"}},
                "from": [round(16.0 - tx, 4), fy, round(16.0 - tz, 4)],
                "to": [round(16.0 - fx, 4), ty, round(16.0 - fz, 4)],
                "faces": faces,
            }
        )
    return rotated


def retarget_faces(
    elements: list[dict],
    *,
    tint: bool,
    molten_to_texture: bool,
) -> list[dict]:
    out = []
    for element in elements:
        faces = {}
        for direction, face in element.get("faces", {}).items():
            mapped = dict(face)
            if molten_to_texture and mapped.get("texture") == "#molten":
                mapped["texture"] = "#texture"
            if tint and mapped.get("texture", "#texture") == "#texture":
                mapped["tintindex"] = 0
            faces[direction] = mapped
        out.append({**element, "faces": faces})
    return out


def write_model(
    dest: str,
    source: str,
    *,
    texture: str,
    molten: str | None = None,
    tint: bool,
    rotate: bool,
    molten_to_texture: bool = False,
) -> None:
    raw = load(source)
    elements = list(raw["elements"])
    if rotate:
        elements = rotate_180_y(elements)
    elements = retarget_faces(
        elements, tint=tint, molten_to_texture=molten_to_texture
    )
    textures = {"texture": texture, "particle": texture}
    if molten is not None and not molten_to_texture:
        textures["molten"] = molten
    payload = {
        "credit": f"GT6 {source.removesuffix('.json')} via tools/gt6_voxel_to_cc.py",
        "parent": "minecraft:block/block",
        "textures": textures,
        "elements": elements,
    }
    path = OUT / dest
    path.write_text(json.dumps(payload, indent=2, ensure_ascii=False) + "\n", encoding="utf-8", newline="\n")
    print(f"wrote {path.relative_to(ROOT)} ({len(elements)} boxes)")


def main() -> None:
    write_model(
        "mortar.json",
        "MultiTileEntityMortar.json",
        texture=METAL,
        tint=True,
        rotate=True,
    )
    write_model(
        "sifter.json",
        "MultiTileEntitySiftingTable.json",
        texture=METAL,
        tint=True,
        rotate=True,
    )
    write_model(
        "bath.json",
        "MultiTileEntityBathingPot.json",
        texture=METAL,
        tint=True,
        rotate=True,
    )
    # smelter is a heat processing cube (basicmachines/smelter), not the
    # MultiTileEntitySmeltery pot shared with the T34 crucible. Do not regenerate
    # src/main/resources/.../models/block/smelter.json from this converter.
    write_model(
        "bronze_steam_engine.json",
        "MultiTileEntityEngineSteam.json",
        texture=METAL,
        tint=True,
        rotate=True,
    )
    write_model(
        "ceramic_mold.json",
        "MultiTileEntityMold.json",
        texture=FIREBRICK,
        tint=False,
        rotate=False,
        molten_to_texture=True,
    )
    write_model(
        "ceramic_mold_filled.json",
        "MultiTileEntityMold.json",
        texture=FIREBRICK,
        molten=MOLTEN,
        tint=False,
        rotate=False,
    )


if __name__ == "__main__":
    main()
