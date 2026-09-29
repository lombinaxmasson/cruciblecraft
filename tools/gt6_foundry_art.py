#!/usr/bin/env python3
"""GT6 foundry art: shared iconsets + four voxel parents, not 85 cubes."""
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

SLUG = "content/gt6-foundry-art"
STATUS = "FOUNDRY_ART_READY"
GT6_REVISION = io.SOURCE_REVISION
WAVE = census.TOOLS / "waves" / "content" / "gt6-foundry-art"
GT6_W = census.ROOT / "gt6_referencable_port_code" / "gregtech6_w"
GT6_TEX = Path("src/main/resources/assets/gregtech/textures/blocks")
ASSETS = census.ROOT / "src" / "main" / "resources" / "assets" / "cruciblecraft"
ITEM_MODELS = ASSETS / "models" / "item"
BLOCK_MODELS = ASSETS / "models" / "block"
BLOCKSTATES = ASSETS / "blockstates"
DEST_ROOT = "assets/cruciblecraft/textures/block/gt6_import"
MANIFEST = ASSETS / "gt6_foundry_art_manifest.json"
OVERLAY = (
    census.TOOLS
    / "waves"
    / "content"
    / "gt6-mte-crucible-foundry-runtime"
    / "runtime_overlay.json"
)
ISBRH = census.ROOT / "textures" / "gt6模型"
EMPTY_SRC = (
    census.ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft_wave_content_gt6_crucible_mold_interaction"
    / "structure"
    / "empty.nbt"
)
PACK = (
    census.ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft_wave_content_gt6_foundry_art"
)
GAME_TESTS = (
    census.ROOT
    / "src"
    / "test"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "gametest"
    / "FoundryArtGameTests.java"
)
CORE_TESTS = (
    census.ROOT
    / "src"
    / "test"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "gametest"
    / "CrucibleCraftGameTests.java"
)
FOUNDRY_TESTS = (
    census.ROOT
    / "src"
    / "test"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "gametest"
    / "MteCrucibleFoundryRuntimeGameTests.java"
)
EXPECTED_TESTS = [
    "foundryArtManifestResolvesLocalGt6",
    "foundryArtNotLargeCrucibleCube",
    "foundryArtSharedParentsNotPerMaterialPng",
]
PARENTS = {
    "Smeltery": ("MultiTileEntitySmeltery.json", "mte_foundry_smeltery.json"),
    "Basin": ("MultiTileEntityBasin.json", "mte_foundry_basin.json"),
    "Crossing": ("MultiTileEntityCrossing.json", "mte_foundry_crossing.json"),
    "Mold": ("MultiTileEntityMold.json", "mte_foundry_mold.json"),
}
METALLIC_BODY = f"{DEST_ROOT}/mte/faucet.png"
STONE_BODY = f"{DEST_ROOT}/materialicons/stone/blocksolid.png"
STONE_GT6 = "materialicons/STONE/blocksolid.png"
METALLIC_GT6 = "materialicons/METALLIC/blocksolid.png"
LARGE_CRUCIBLE = "crucible_foundry"


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


def foundry_rows() -> list[dict[str, Any]]:
    overlay = census.load_json(OVERLAY)
    return [row for row in overlay["rows"] if row["kind"] == "CRUCIBLE_FOUNDRY"]


def geometry_key(gt6_class: str) -> str:
    if "Crossing" in gt6_class:
        return "Crossing"
    if "Smeltery" in gt6_class:
        return "Smeltery"
    if "Basin" in gt6_class:
        return "Basin"
    if "Mold" in gt6_class:
        return "Mold"
    raise ValueError(f"unknown foundry class {gt6_class}")


def sits_flat(gt6_class: str) -> bool:
    """GT6 molds/basins stay world-aligned; the 5×5 is not rotated by facing."""
    return geometry_key(gt6_class) in {"Mold", "Basin"}


def material_token(registry_path: str) -> str:
    if registry_path.startswith("foundry/"):
        return registry_path.rsplit("_", 1)[-1]
    return registry_path.split("/", 1)[0]


def iconset_for(token: str) -> str:
    return "stone" if token == "stone" else "metallic"


def texture_path(token: str) -> str:
    if iconset_for(token) == "stone":
        return "cruciblecraft:block/gt6_import/materialicons/stone/blocksolid"
    return "cruciblecraft:block/gt6_import/mte/faucet"


def parent_model(gt6_class: str) -> str:
    return f"cruciblecraft:block/{PARENTS[geometry_key(gt6_class)][1].removesuffix('.json')}"


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
    for key, (source_name, dest_name) in PARENTS.items():
        raw = json.loads((ISBRH / source_name).read_text(encoding="utf-8"))
        payload = {
            "credit": (
                f"GT6 {source_name.removesuffix('.json')} empty-state ISBRH; "
                "shared materialicons blocksolid + tintindex 0. Overlay icons "
                "are empty in GT6 and omitted."
            ),
            "parent": "minecraft:block/block",
            "render_type": "minecraft:cutout",
            "textures": {
                "body": "cruciblecraft:block/gt6_import/mte/faucet",
                "particle": "#body",
            },
            "elements": _retint(list(raw["elements"])),
        }
        _write_json(BLOCK_MODELS / dest_name, payload)


def _blockstate(model: str, gt6_class: str = "") -> dict[str, Any]:
    faces = ("down", "east", "north", "south", "up", "west")
    if gt6_class and sits_flat(gt6_class):
        return {
            "variants": {f"facing={face}": {"model": model} for face in faces}
        }
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
    rows = rows if rows is not None else foundry_rows()
    _write_parents()
    for row in rows:
        path = str(row["dummy_path"])
        parent = parent_model(str(row["gt6_class"]))
        texture = texture_path(material_token(path))
        child = {
            "parent": parent,
            "textures": {"body": texture, "particle": "#body"},
        }
        _write_json(BLOCK_MODELS / f"{path}.json", child)
        _write_json(
            BLOCKSTATES / f"{path}.json",
            _blockstate(f"cruciblecraft:block/{path}", str(row["gt6_class"])),
        )
        _write_json(ITEM_MODELS / f"{path}.json", {"parent": f"cruciblecraft:block/{path}"})


def _copy_iconsets() -> list[dict[str, Any]]:
    rows: list[dict[str, Any]] = []
    metallic_src = _gt6_file(METALLIC_GT6)
    metallic_dst = _dest_file(METALLIC_BODY)
    if not metallic_src.is_file():
        raise FileNotFoundError(metallic_src)
    if not metallic_dst.is_file() or metallic_dst.read_bytes() != metallic_src.read_bytes():
        raise FileNotFoundError(
            f"metallic blocksolid must already be {METALLIC_BODY}"
        )
    rows.append(
        {
            "source": "gt6_referencable_port_code/gregtech6_w",
            "gt6_source": str(metallic_src.relative_to(GT6_W)).replace("\\", "/"),
            "destination": METALLIC_BODY,
            "copied": False,
            "reason": "reuse_faucet_metallic",
            "sha256": _sha256(metallic_dst),
        }
    )
    stone_src = _gt6_file(STONE_GT6)
    stone_dst = _dest_file(STONE_BODY)
    if not stone_src.is_file():
        raise FileNotFoundError(stone_src)
    stone_dst.parent.mkdir(parents=True, exist_ok=True)
    if not stone_dst.is_file() or stone_dst.read_bytes() != stone_src.read_bytes():
        shutil.copyfile(stone_src, stone_dst)
    rows.append(
        {
            "source": "gt6_referencable_port_code/gregtech6_w",
            "gt6_source": str(stone_src.relative_to(GT6_W)).replace("\\", "/"),
            "destination": STONE_BODY,
            "copied": True,
            "reason": "stone_blocksolid",
            "sha256": _sha256(stone_dst),
        }
    )
    return rows


def extra_png_for_metals() -> int:
    return sum(
        1
        for row in census.load_json(MANIFEST).get("rows") or []
        if row.get("copied") and row.get("reason") != "stone_blocksolid"
    )


def _copy_empty_nbt() -> None:
    if not EMPTY_SRC.is_file():
        raise FileNotFoundError(census.relative(EMPTY_SRC))
    for dest in (
        PACK / "structure" / "empty.nbt",
        PACK / "gametest" / "structure" / "empty.nbt",
    ):
        dest.parent.mkdir(parents=True, exist_ok=True)
        if dest.is_file() and dest.read_bytes() == EMPTY_SRC.read_bytes():
            continue
        shutil.copyfile(EMPTY_SRC, dest)


def topology(unique_active: bool) -> dict[str, Any]:
    return {
        "schema_version": 1,
        "wave_slug": SLUG,
        "unique_active_wave": SLUG if unique_active else None,
        "status": STATUS,
        "source_revision": GT6_REVISION,
        "generated_by": f"{SLUG} implementation",
        "append_only": False,
        "next_unassigned": not unique_active,
        "remaining_recipe_gap": 0,
    }


def write(*, unique_active: bool = True) -> None:
    rows = foundry_rows()
    if len(rows) != 85:
        raise ValueError(f"foundry overlay has {len(rows)} rows, expected 85")
    WAVE.mkdir(parents=True, exist_ok=True)
    manifest = {"rows": _copy_iconsets()}
    _write_json(MANIFEST, manifest)
    _write_json(WAVE / "art_manifest.json", manifest)
    write_models(rows)
    _copy_empty_nbt()
    _write_json(WAVE / "topology.json", topology(unique_active))
    _write_json(
        WAVE / "readiness.json",
        {
            "schema_version": 1,
            "wave_slug": SLUG,
            "unique_active_wave": SLUG if unique_active else None,
            "status": STATUS,
            "source_revision": GT6_REVISION,
        },
    )
    _write_json(
        WAVE / "runtime_notes.json",
        {
            "close_target": "runtime_ready",
            "obtain": "explicitly_blocked",
            "no_large_crucible_side": True,
            "no_per_material_png": True,
            "overlay_omitted_empty": True,
            "reuse_faucet_metallic": True,
            "foundry_tanks_remain_dummy": False,
            "reopens_foundry_identity": False,
            "reopens_foundry_required_test_ids": False,
        },
    )
    _write_json(
        WAVE / "execution_subset.json",
        {
            "schema": "gt6-foundry-art-subset-v1",
            "capability_slug": SLUG,
            "note": "Voxel parents + shared iconsets on the live 85. No tick.",
            "rows": [
                {
                    "live_block": f"cruciblecraft:{row['dummy_path']}",
                    "geometry": geometry_key(str(row["gt6_class"])),
                    "iconset": iconset_for(material_token(str(row["dummy_path"]))),
                }
                for row in rows
            ],
        },
    )
    _write_json(
        WAVE / "production_lock.json",
        {
            "note": "foundry voxel art on live BlockItems; not player_complete"
        },
    )


def check() -> list[str]:
    errors: list[str] = []
    if not OVERLAY.is_file():
        return ["missing foundry runtime overlay"]
    rows = foundry_rows()
    if len(rows) != 85:
        errors.append(f"foundry overlay has {len(rows)} rows, expected 85")
    if not MANIFEST.is_file():
        errors.append("missing gt6_foundry_art_manifest.json")
        return errors
    manifest = census.load_json(MANIFEST)
    copied = 0
    for row in manifest.get("rows") or []:
        dest = _dest_file(str(row["destination"]))
        if not dest.is_file():
            errors.append(f"missing dest {row['destination']}")
            continue
        if row.get("copied"):
            copied += 1
        if LARGE_CRUCIBLE in str(row["destination"]):
            errors.append("manifest still points at large-crucible side.png")
    if copied != 1:
        errors.append(f"expected 1 new PNG (stone), got {copied}")
    if extra_png_for_metals() != 0:
        errors.append("metal foundry rows copied extra PNG")
    for key, (_src, dest_name) in PARENTS.items():
        parent = BLOCK_MODELS / dest_name
        if not parent.is_file():
            errors.append(f"missing parent {dest_name}")
            continue
        text = parent.read_text(encoding="utf-8")
        if "cube_all" in text or LARGE_CRUCIBLE in text:
            errors.append(f"{dest_name} still uses the large-crucible cube")
        if "tintindex" not in text:
            errors.append(f"{dest_name} missing tintindex")
    for row in rows:
        path = str(row["dummy_path"])
        item = ITEM_MODELS / f"{path}.json"
        block = BLOCK_MODELS / f"{path}.json"
        for model in (item, block):
            if not model.is_file():
                errors.append(f"missing {census.relative(model)}")
                continue
            text = model.read_text(encoding="utf-8")
            if "iron_ingot" in text or "cube_all" in text or LARGE_CRUCIBLE in text:
                errors.append(f"{path} still uses cube_all / large crucible")
            parent = parent_model(str(row["gt6_class"]))
            if parent not in text and model == block:
                errors.append(f"{path} does not parent {parent}")
            expected = texture_path(material_token(path))
            if model == block and expected not in text:
                errors.append(f"{path} texture is not {expected}")
        blockstate = BLOCKSTATES / f"{path}.json"
        if not blockstate.is_file():
            errors.append(f"missing blockstate {path}")
        elif sits_flat(str(row["gt6_class"])):
            variants = census.load_json(blockstate).get("variants") or {}
            for name, variant in variants.items():
                if "x" in variant or "y" in variant:
                    errors.append(
                        f"{path} {name} rotates the floor mold/basin "
                        "(chisel hits world XZ)"
                    )
    if not (PACK / "structure" / "empty.nbt").is_file():
        errors.append("missing foundry-art structure/empty.nbt")
    if not (PACK / "gametest" / "structure" / "empty.nbt").is_file():
        errors.append("missing foundry-art gametest/structure/empty.nbt")
    return errors


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args(argv)
    if args.write == args.check:
        parser.error("choose exactly one of --write or --check")
    if args.write:
        write(unique_active=True)
        print(f"Wrote {SLUG}")
        return 0
    errors = check()
    if errors:
        print("\n".join(errors), file=sys.stderr)
        return 1
    print("foundry art is current")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
