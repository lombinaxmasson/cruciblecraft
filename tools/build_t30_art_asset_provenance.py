#!/usr/bin/env python3
"""Build T30 Hopper-family ART_DERIVED provenance.

Projects 121 block identities onto shared geometry parents and existing CC
grayscale textures. Does not write per-material PNGs or 120 blockstates.
"""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from tools import t27_common as common  # noqa: E402

TOOLS = common.TOOLS
CATALOG = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "hopper_variants.json"
)
MODELS = ROOT / "src" / "main" / "resources" / "assets" / "cruciblecraft" / "models" / "block"
TEXTURES = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "assets"
    / "cruciblecraft"
    / "textures"
    / "block"
    / "material"
)
OUTPUT = TOOLS / "t30_art_asset_provenance.json"
BUILDER = Path(__file__).resolve()

SHARED_TEXTURE = "cruciblecraft:block/material/block"
OVERLAY_TEXTURE = "cruciblecraft:block/material/block_overlay"
DUST_FUNNEL_ID = "cruciblecraft:steel_dust_funnel"

FAMILIES = (
    {
        "family": "hopper",
        "source_model": "textures/gt6模型/MultiTileEntityHopper.json",
        "shared_parent": "cruciblecraft:block/hopper",
        "shared_side_parent": "cruciblecraft:block/hopper_side",
        "shared_texture": SHARED_TEXTURE,
        "overlay_texture": None,
        "tintindex": 0,
        "model_files": ("hopper.json", "hopper_side.json"),
        "requires_overlay": False,
    },
    {
        "family": "queue_hopper",
        "source_model": "textures/gt6模型/MultiTileEntityQueueHopper.json",
        "shared_parent": "cruciblecraft:block/queue_hopper",
        "shared_side_parent": "cruciblecraft:block/queue_hopper_side",
        "shared_texture": SHARED_TEXTURE,
        "overlay_texture": OVERLAY_TEXTURE,
        "tintindex": 0,
        "model_files": ("queue_hopper.json", "queue_hopper_side.json"),
        "requires_overlay": True,
    },
    {
        "family": "dust_funnel",
        "source_model": "textures/gt6模型/MultiTileEntityDustFunnel.json",
        "shared_parent": "cruciblecraft:block/dust_funnel",
        "shared_side_parent": None,
        "shared_texture": SHARED_TEXTURE,
        "overlay_texture": OVERLAY_TEXTURE,
        "tintindex": 0,
        "model_files": ("dust_funnel.json",),
        "requires_overlay": True,
    },
)


def _require(path: Path) -> None:
    if not path.is_file():
        raise FileNotFoundError(common.relative(path))


def _material_path(material: str) -> str:
    if not material.startswith("cruciblecraft:"):
        raise ValueError(f"hopper catalog material is not CC-namespaced: {material}")
    return material.split(":", 1)[1]


def _catalog_rows() -> list[dict[str, Any]]:
    _require(CATALOG)
    document = common.load_json(CATALOG)
    if document.get("schema_version") != 1:
        raise ValueError("hopper_variants schema_version drifted")
    if document.get("source_revision") != common.SOURCE_REVISION:
        raise ValueError("hopper_variants source revision drifted")
    rows = document.get("variants")
    if not isinstance(rows, list) or len(rows) != 60:
        raise ValueError("hopper_variants must remain 60 SOURCE_BACKED rows")
    return rows


def _faces(model: dict[str, Any]) -> list[dict[str, Any]]:
    faces: list[dict[str, Any]] = []
    for element in model.get("elements") or []:
        for face in (element.get("faces") or {}).values():
            if isinstance(face, dict):
                faces.append(face)
    return faces


def _validate_model(path: Path, *, requires_overlay: bool) -> None:
    _require(path)
    model = json.loads(path.read_text(encoding="utf-8"))
    textures = model.get("textures") or {}
    if textures.get("colored") != SHARED_TEXTURE:
        raise ValueError(f"{common.relative(path)} colored texture drifted")
    if textures.get("particle") not in ("#colored", SHARED_TEXTURE):
        raise ValueError(f"{common.relative(path)} particle texture is invalid")
    faces = _faces(model)
    if not faces:
        raise ValueError(f"{common.relative(path)} has no faces")
    colored = [face for face in faces if face.get("texture") == "#colored"]
    overlay = [face for face in faces if face.get("texture") == "#overlay"]
    if not colored:
        raise ValueError(f"{common.relative(path)} has no tintable faces")
    if any(face.get("tintindex") != 0 for face in colored):
        raise ValueError(f"{common.relative(path)} tintable faces must use tintindex 0")
    if requires_overlay:
        if textures.get("overlay") != OVERLAY_TEXTURE:
            raise ValueError(f"{common.relative(path)} overlay texture drifted")
        if not overlay:
            raise ValueError(f"{common.relative(path)} is missing the untinted overlay")
        if any("tintindex" in face for face in overlay):
            raise ValueError(
                f"{common.relative(path)} overlay faces must not be tinted"
            )
    elif overlay:
        raise ValueError(f"{common.relative(path)} must not carry a queue overlay")


def _projected_members(rows: list[dict[str, Any]]) -> list[str]:
    members: list[str] = []
    seen: set[str] = set()
    for row in rows:
        path = _material_path(str(row["material"]))
        for suffix in ("_hopper", "_queue_hopper"):
            identity = f"cruciblecraft:{path}{suffix}"
            if identity in seen:
                raise ValueError(f"duplicate projected hopper identity {identity}")
            seen.add(identity)
            members.append(identity)
    if DUST_FUNNEL_ID in seen:
        raise ValueError("dust funnel id collided with a hopper variant")
    members.append(DUST_FUNNEL_ID)
    if len(members) != 121:
        raise ValueError(f"projected hopper art members drifted: {len(members)}")
    if any(identity in {"cruciblecraft:hopper", "cruciblecraft:queue_hopper"} for identity in members):
        raise ValueError("bare hopper ids are forbidden")
    return members


def build() -> dict[str, Any]:
    _require(TEXTURES / "block.png")
    _require(TEXTURES / "block_overlay.png")
    rows = _catalog_rows()
    for family in FAMILIES:
        for name in family["model_files"]:
            _validate_model(
                MODELS / name,
                requires_overlay=bool(family["requires_overlay"]),
            )
    members = _projected_members(rows)
    per_material = list(
        (ROOT / "src" / "main" / "resources" / "assets" / "cruciblecraft" / "textures")
        .rglob("*_hopper.png")
    )
    if per_material:
        raise ValueError("per-material hopper PNGs are forbidden")
    return {
        "status": "T30_ART_ASSET_PROVENANCE",
        "schema_version": 1,
        "classification": "ART_DERIVED",
        "source_revision": common.SOURCE_REVISION,
        "derivation_rule": (
            "one shared geometry parent per visual family; tintable grayscale "
            "from existing CC block.png; tintindex 0; Queue/Dust Funnel overlay "
            "uses block_overlay without tint; Dust Funnel keeps a fixed steel "
            "tint at runtime; no <material>_hopper.png"
        ),
        "families": [
            {
                "family": family["family"],
                "source_model": family["source_model"],
                "shared_parent": family["shared_parent"],
                "shared_side_parent": family["shared_side_parent"],
                "shared_texture": family["shared_texture"],
                "overlay_texture": family["overlay_texture"],
                "tintindex": family["tintindex"],
            }
            for family in FAMILIES
        ],
        "projected_members": members,
        "counts": {
            "hopper": 60,
            "queue_hopper": 60,
            "dust_funnel": 1,
            "total": 121,
            "per_material_pngs": 0,
        },
    }


def write() -> dict[str, Any]:
    document = build()
    common.write_stable(OUTPUT, document)
    return document


def check() -> list[str]:
    if not OUTPUT.is_file():
        return [f"missing generated file: {common.relative(OUTPUT)}"]
    expected = common.stable_json(build())
    actual = OUTPUT.read_text(encoding="utf-8")
    if actual != expected:
        return [f"{common.relative(OUTPUT)} is stale"]
    return []


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args(argv)
    if bool(args.write) == bool(args.check):
        parser.error("choose exactly one of --write, --check")
    try:
        if args.write:
            document = write()
            print(
                f"wrote {common.relative(OUTPUT)} "
                f"members={document['counts']['total']} "
                f"classification={document['classification']}"
            )
            return 0
        errors = check()
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        print(f"{common.relative(OUTPUT)} is current")
        return 0
    except (OSError, ValueError, json.JSONDecodeError, KeyError) as exc:
        print(str(exc), file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
