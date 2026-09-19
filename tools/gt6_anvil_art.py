#!/usr/bin/env python3
"""GT6 anvil voxels: shared iconsets + empty-state ISBRH parent, not misc-tool cubes."""
from __future__ import annotations

import argparse
import copy
import hashlib
import json
import shutil
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import census_common as census
from tools import io_common as io

GT6_REVISION = io.SOURCE_REVISION
GT6_W = census.ROOT / "gt6_referencable_port_code" / "gregtech6_w"
GT6_TEX = Path("src/main/resources/assets/gregtech/textures/blocks")
ASSETS = census.ROOT / "src" / "main" / "resources" / "assets" / "cruciblecraft"
ITEM_MODELS = ASSETS / "models" / "item"
BLOCK_MODELS = ASSETS / "models" / "block"
BLOCKSTATES = ASSETS / "blockstates"
DEST_ROOT = "assets/cruciblecraft/textures/block/gt6_import"
MANIFEST = ASSETS / "gt6_anvil_art_manifest.json"
OVERLAY = (
    census.TOOLS
    / "waves"
    / "content"
    / "gt6-mte-misc-tool-runtime"
    / "runtime_overlay.json"
)
ISBRH = census.ROOT / "textures" / "gt6模型" / "MultiTileEntityAnvil.json"
PARENT_NAME = "anvil_gt6.json"
METALLIC_BODY = f"{DEST_ROOT}/mte/faucet.png"
STONE_BODY = f"{DEST_ROOT}/materialicons/stone/blocksolid.png"
WOOD_BODY = f"{DEST_ROOT}/materialicons/wood/blocksolid.png"
STONE_GT6 = "materialicons/STONE/blocksolid.png"
METALLIC_GT6 = "materialicons/METALLIC/blocksolid.png"
WOOD_GT6 = "materialicons/WOOD/blocksolid.png"


def _write_json(path: Path, document: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    payload = json.dumps(document, ensure_ascii=False, indent=2) + "\n"
    if path.is_file() and path.read_text(encoding="utf-8") == payload:
        return
    path.write_text(payload, encoding="utf-8")


def _sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def _gt6_file(rel: str) -> Path:
    return GT6_W / GT6_TEX / rel


def _dest_file(destination: str) -> Path:
    return census.ROOT / "src" / "main" / "resources" / destination


def anvil_rows() -> list[dict[str, Any]]:
    overlay = census.load_json(OVERLAY)
    return [row for row in overlay["rows"] if "MultiTileEntityAnvil" in str(row["gt6_class"])]


def token(registry_path: str) -> str:
    if registry_path.endswith("/anvil"):
        return registry_path[: -len("/anvil")]
    last = registry_path.rsplit("/", 1)[-1]
    if last.endswith("_anvil"):
        return last[: -len("_anvil")]
    return last


def iconset_for(path: str) -> str:
    material = token(path)
    if material == "stone":
        return "stone"
    if material == "ironwood":
        return "wood"
    return "metallic"


def texture_path(path: str) -> str:
    kind = iconset_for(path)
    if kind == "stone":
        return "cruciblecraft:block/gt6_import/materialicons/stone/blocksolid"
    if kind == "wood":
        return "cruciblecraft:block/gt6_import/materialicons/wood/blocksolid"
    return "cruciblecraft:block/gt6_import/mte/faucet"


def _retint(elements: list[dict[str, Any]]) -> list[dict[str, Any]]:
    out: list[dict[str, Any]] = []
    for element in elements:
        faces = {}
        for direction, face in element.get("faces", {}).items():
            mapped = dict(face)
            mapped["texture"] = "#body"
            mapped["tintindex"] = 0
            faces[direction] = mapped
        cloned = copy.deepcopy(element)
        cloned["faces"] = faces
        out.append(cloned)
    return out


def _write_parents() -> None:
    raw = json.loads(ISBRH.read_text(encoding="utf-8"))
    payload = {
        "credit": (
            "GT6 MultiTileEntityAnvil empty-state ISBRH; shared materialicons "
            "blocksolid + tintindex 0. Overlay icons are empty in GT6 and omitted."
        ),
        "parent": "minecraft:block/block",
        "render_type": "minecraft:cutout",
        "textures": {
            "body": "cruciblecraft:block/gt6_import/materialicons/stone/blocksolid",
            "particle": "#body",
        },
        "elements": _retint(list(raw["elements"])),
    }
    _write_json(BLOCK_MODELS / PARENT_NAME, payload)
    _write_json(
        BLOCK_MODELS / "anvil.json",
        {
            "parent": "cruciblecraft:block/anvil_gt6",
            "textures": {
                "body": "cruciblecraft:block/gt6_import/materialicons/stone/blocksolid",
                "particle": "#body",
            },
        },
    )
    _write_json(
        BLOCK_MODELS / "anvil_metallic.json",
        {
            "parent": "cruciblecraft:block/anvil_gt6",
            "textures": {
                "body": "cruciblecraft:block/gt6_import/mte/faucet",
                "particle": "#body",
            },
        },
    )
    _write_json(
        BLOCK_MODELS / "anvil_wood.json",
        {
            "parent": "cruciblecraft:block/anvil_gt6",
            "textures": {
                "body": "cruciblecraft:block/gt6_import/materialicons/wood/blocksolid",
                "particle": "#body",
            },
        },
    )


def _blockstate(model: str) -> dict[str, Any]:
    return {
        "variants": {
            "facing=down": {"model": model, "x": 90},
            "facing=east": {"model": model, "y": 90},
            "facing=north": {"model": model},
            "facing=south": {"model": model, "y": 180},
            "facing=up": {"model": model, "x": 270},
            "facing=west": {"model": model, "y": 270},
        }
    }


def write_models(rows: list[dict[str, Any]] | None = None) -> None:
    rows = rows if rows is not None else anvil_rows()
    _write_parents()
    for row in rows:
        path = str(row["dummy_path"])
        texture = texture_path(path)
        child = {
            "parent": "cruciblecraft:block/anvil_gt6",
            "textures": {"body": texture, "particle": "#body"},
        }
        _write_json(BLOCK_MODELS / f"{path}.json", child)
        _write_json(BLOCKSTATES / f"{path}.json", _blockstate(f"cruciblecraft:block/{path}"))
        _write_json(ITEM_MODELS / f"{path}.json", {"parent": f"cruciblecraft:block/{path}"})


def _copy_row(gt6_rel: str, destination: str, reason: str, copied: bool) -> dict[str, Any]:
    src = _gt6_file(gt6_rel)
    dst = _dest_file(destination)
    if not src.is_file():
        raise FileNotFoundError(src)
    dst.parent.mkdir(parents=True, exist_ok=True)
    if copied:
        if not dst.is_file() or dst.read_bytes() != src.read_bytes():
            shutil.copyfile(src, dst)
    elif not dst.is_file() or dst.read_bytes() != src.read_bytes():
        raise FileNotFoundError(f"{reason} must already be {destination}")
    return {
        "source": "gt6_referencable_port_code/gregtech6_w",
        "gt6_source": str(src.relative_to(GT6_W)).replace("\\", "/"),
        "destination": destination,
        "copied": copied,
        "reason": reason,
        "sha256": _sha256(dst),
    }


def _copy_iconsets() -> list[dict[str, Any]]:
    return [
        _copy_row(METALLIC_GT6, METALLIC_BODY, "reuse_faucet_metallic", False),
        _copy_row(STONE_GT6, STONE_BODY, "stone_blocksolid", True),
        _copy_row(WOOD_GT6, WOOD_BODY, "wood_blocksolid", True),
    ]


def check() -> list[str]:
    errors: list[str] = []
    rows = anvil_rows()
    if not rows:
        errors.append("no MultiTileEntityAnvil rows in misc-tool overlay")
    parent = BLOCK_MODELS / PARENT_NAME
    if not parent.is_file():
        errors.append(f"missing {parent}")
    else:
        text = parent.read_text(encoding="utf-8")
        if "cube_all" in text or "mte_inplace_misc_tool" in text:
            errors.append("anvil parent drifted back to a cube")
        if "rough_block_solid" in text:
            errors.append("anvil parent still uses ROUGH blocksolid")
    for row in rows:
        path = str(row["dummy_path"])
        model = BLOCK_MODELS / f"{path}.json"
        if not model.is_file():
            errors.append(f"missing model {path}")
            continue
        text = model.read_text(encoding="utf-8")
        if "anvil_gt6" not in text:
            errors.append(f"{path} is not parented to anvil_gt6")
        if "mte_inplace_misc_tool" in text or "cube_all" in text:
            errors.append(f"{path} is still the misc-tool cube")
        expected = texture_path(path)
        if expected not in text:
            errors.append(f"{path} iconset drifted from {expected}")
    live_path = BLOCK_MODELS / "anvil.json"
    if not live_path.is_file():
        errors.append("missing live anvil.json")
    else:
        live = live_path.read_text(encoding="utf-8")
        if "rough_block_solid" in live or "anvil_gt6" not in live:
            errors.append("live anvil.json is not the GT6 stone child")
    metallic_path = BLOCK_MODELS / "anvil_metallic.json"
    if not metallic_path.is_file():
        errors.append("missing live anvil_metallic.json")
    else:
        metallic = metallic_path.read_text(encoding="utf-8")
        if "anvil_gt6" not in metallic or "mte/faucet" not in metallic:
            errors.append("live anvil_metallic.json is not the GT6 metallic child")
    wood_path = BLOCK_MODELS / "anvil_wood.json"
    if not wood_path.is_file():
        errors.append("missing anvil_wood.json")
    else:
        wood = wood_path.read_text(encoding="utf-8")
        if "anvil_gt6" not in wood or "materialicons/wood/blocksolid" not in wood:
            errors.append("anvil_wood.json is not the GT6 wood child")
    if not _dest_file(METALLIC_BODY).is_file():
        errors.append("missing reused metallic faucet body")
    if not _dest_file(STONE_BODY).is_file():
        errors.append("missing stone blocksolid")
    if not _dest_file(WOOD_BODY).is_file():
        errors.append("missing wood blocksolid")
    return errors


def write() -> None:
    rows = anvil_rows()
    manifest = {"rows": _copy_iconsets()}
    _write_json(MANIFEST, manifest)
    write_models(rows)


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args(argv)
    if args.check:
        errors = check()
        for error in errors:
            print(error)
        return 1 if errors else 0
    write()
    errors = check()
    for error in errors:
        print(error)
    return 1 if errors else 0


if __name__ == "__main__":
    raise SystemExit(main())
