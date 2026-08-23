#!/usr/bin/env python3
"""Build T30 Hopper-family ART_DERIVED provenance.

Projects 121 block identities onto shared geometry parents and shared GT6
hopper/queuehopper/dust_funnel colored textures. Does not write per-material
PNGs or 120 blockstates.
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
BLOCK_TEXTURES = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "assets"
    / "cruciblecraft"
    / "textures"
    / "block"
)
OUTPUT = TOOLS / "t30_art_asset_provenance.json"
BUILDER = Path(__file__).resolve()

DUST_FUNNEL_ID = "cruciblecraft:steel_dust_funnel"

HOPPER_TEXTURES = {
    "bottom": "cruciblecraft:block/hopper/colored_bottom",
    "top": "cruciblecraft:block/hopper/colored_top",
    "side": "cruciblecraft:block/hopper/colored_side",
}
QUEUE_TEXTURES = {
    "bottom": "cruciblecraft:block/queue_hopper/colored_bottom",
    "top": "cruciblecraft:block/queue_hopper/colored_top",
    "side": "cruciblecraft:block/queue_hopper/colored_side",
}
DUST_FUNNEL_TEXTURES = {
    "bottom": "cruciblecraft:block/dust_funnel/colored_bottom",
    "top": "cruciblecraft:block/dust_funnel/colored_top",
    "sides": "cruciblecraft:block/dust_funnel/colored_sides",
    "hole": "cruciblecraft:block/dust_funnel/colored_hole",
}

FAMILIES = (
    {
        "family": "hopper",
        "source_model": "textures/gt6模型/MultiTileEntityHopper.json",
        "shared_parent": "cruciblecraft:block/hopper",
        "shared_side_parent": "cruciblecraft:block/hopper_side",
        "shared_texture": HOPPER_TEXTURES["side"],
        "overlay_texture": None,
        "tintindex": 0,
        "model_files": ("hopper.json", "hopper_side.json"),
        "textures": HOPPER_TEXTURES,
        "face_slots": ("#bottom", "#top", "#side"),
        "requires_hole": False,
        "pngs": (
            "hopper/colored_bottom.png",
            "hopper/colored_top.png",
            "hopper/colored_side.png",
        ),
    },
    {
        "family": "queue_hopper",
        "source_model": "textures/gt6模型/MultiTileEntityQueueHopper.json",
        "shared_parent": "cruciblecraft:block/queue_hopper",
        "shared_side_parent": "cruciblecraft:block/queue_hopper_side",
        "shared_texture": QUEUE_TEXTURES["side"],
        "overlay_texture": None,
        "tintindex": 0,
        "model_files": ("queue_hopper.json", "queue_hopper_side.json"),
        "textures": QUEUE_TEXTURES,
        "face_slots": ("#bottom", "#top", "#side"),
        "requires_hole": False,
        "pngs": (
            "queue_hopper/colored_bottom.png",
            "queue_hopper/colored_top.png",
            "queue_hopper/colored_side.png",
        ),
    },
    {
        "family": "dust_funnel",
        "source_model": "textures/gt6模型/MultiTileEntityDustFunnel.json",
        "shared_parent": "cruciblecraft:block/dust_funnel",
        "shared_side_parent": None,
        "shared_texture": DUST_FUNNEL_TEXTURES["sides"],
        "overlay_texture": DUST_FUNNEL_TEXTURES["hole"],
        "tintindex": 0,
        "model_files": ("dust_funnel.json",),
        "textures": DUST_FUNNEL_TEXTURES,
        "face_slots": ("#bottom", "#top", "#sides", "#hole"),
        "requires_hole": True,
        "pngs": (
            "dust_funnel/colored_bottom.png",
            "dust_funnel/colored_top.png",
            "dust_funnel/colored_sides.png",
            "dust_funnel/colored_hole.png",
        ),
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


def _validate_model(path: Path, family: dict[str, Any]) -> None:
    _require(path)
    model = json.loads(path.read_text(encoding="utf-8"))
    textures = model.get("textures") or {}
    expected = family["textures"]
    for key, value in expected.items():
        if textures.get(key) != value:
            raise ValueError(f"{common.relative(path)} {key} texture drifted")
    particle = textures.get("particle")
    if particle not in ("#side", "#sides"):
        raise ValueError(f"{common.relative(path)} particle texture is invalid")
    faces = _faces(model)
    if not faces:
        raise ValueError(f"{common.relative(path)} has no faces")
    allowed = set(family["face_slots"])
    if any(face.get("texture") not in allowed for face in faces):
        raise ValueError(f"{common.relative(path)} uses a non-GT6 hopper face slot")
    if any(face.get("tintindex") != 0 for face in faces):
        raise ValueError(f"{common.relative(path)} tintable faces must use tintindex 0")
    if family["requires_hole"] and not any(face.get("texture") == "#hole" for face in faces):
        raise ValueError(f"{common.relative(path)} is missing the dust-funnel hole sheet")
    if not family["requires_hole"] and any(face.get("texture") == "#hole" for face in faces):
        raise ValueError(f"{common.relative(path)} must not carry a dust-funnel hole")


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
    rows = _catalog_rows()
    for family in FAMILIES:
        for png in family["pngs"]:
            _require(BLOCK_TEXTURES / png)
        for name in family["model_files"]:
            _validate_model(MODELS / name, family)
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
            "one shared geometry parent per visual family; tintable GT6 hopper/"
            "queuehopper/dust_funnel colored top/side/bottom (dust funnel also "
            "composites the hole sheet); tintindex 0; GT6 overlay icons are "
            "empty and omitted; Dust Funnel keeps a fixed steel tint at "
            "runtime; no <material>_hopper.png"
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
