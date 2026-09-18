#!/usr/bin/env python3
"""GT6 storage art: kind-level colored+overlay, not vanilla oak/iron cubes."""
from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import shutil
import sys
import time
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import census_common as census
from tools import io_common as io

SLUG = "content/gt6-storage-art"
STATUS = "STORAGE_ART_READY"
GT6_REVISION = io.SOURCE_REVISION
WAVE = census.TOOLS / "waves" / "content" / "gt6-storage-art"
GT6_W = census.ROOT / "gt6_referencable_port_code" / "gregtech6_w"
GT6_TEX = Path("src/main/resources/assets/gregtech/textures/blocks")
ASSETS = census.ROOT / "src" / "main" / "resources" / "assets" / "cruciblecraft"
GENERATED = (
    census.ROOT / "src" / "generated" / "resources" / "assets" / "cruciblecraft"
)
BLOCK_MODELS = ASSETS / "models" / "block"
DEST_ROOT = "assets/cruciblecraft/textures/block/gt6_import/storage"
MANIFEST = ASSETS / "gt6_storage_art_manifest.json"
CATALOG = (
    census.ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "storage_variants.json"
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
    / "cruciblecraft_wave_content_gt6_storage_art"
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
    / "StorageArtGameTests.java"
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
EXPECTED_TESTS = [
    "storageArtBarrelNotVanillaOak",
    "storageArtBookshelfShowsBookDisplay",
    "storageArtBottleCrateShowsBottleFluid",
    "storageArtChestIsFiftyFour",
    "storageArtManifestResolvesLocalGt6",
    "storageArtMassStorageFrontCountSync",
    "storageArtMetalBookshelfIsLive",
    "storageArtMetalBottleCrateIsLive",
    "storageArtMetalDrawerIsQuad",
    "storageArtMetalLockerSwapsArmor",
    "storageArtMetalMassStorageIsLive",
    "storageArtMteBarrelNotMissingCube",
    "storageArtSafeIsFifteen",
]
FACES = ("bottom", "top", "front", "back", "side")
LAYERS = ("colored", "overlay")
MC_FACES = (
    ("down", "bottom"),
    ("up", "top"),
    ("north", "front"),
    ("south", "back"),
    ("west", "side"),
    ("east", "side"),
)
MACHINE_SETS: dict[str, str] = {
    "mass_storage_barrel": "machines/massstorage/barrel",
    "mass_storage_box": "machines/massstorage/box",
    "mass_storage_standard": "machines/massstorage/standard",
    "mass_storage_logistics": "machines/massstorage/logistics",
    "drawer": "machines/drawers/quad",
    "locker": "machines/lockers/normal",
    "charging_locker": "machines/lockers/charging",
}
INSERTER_REL = {
    "colored": "machines/massstorage/inserter/colored/sides.png",
    "overlay": "machines/massstorage/inserter/overlay/sides.png",
}
PLANKS_REL = "iconsets/planks_wood.png"
METALLIC_REL = "materialicons/METALLIC/blocksolid.png"
BOOK_PNGS = (
    "book_vanilla_back",
    "book_vanilla_side",
    "book_enchanted_back",
    "book_enchanted_side",
    "book_colored_back",
    "book_colored_side",
    "book_gt_back",
    "book_gt_side",
)
BOTTLE_PNGS = (
    "bottlecrate_bottle_top",
    "bottlecrate_bottle_sides",
    "bottlecrate_bottle_cap",
)
RENDERER_MARKERS = (
    "MassStorageRenderer",
    "BookshelfRenderer",
    "BottleCrateRenderer",
    "MteInPlaceStorageRenderer",
    "GtChestRenderer",
)
CLIENT_SETUP = (
    census.ROOT
    / "src"
    / "main"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "client"
    / "ClientSetup.java"
)
MTE_KIND = (
    census.ROOT
    / "src"
    / "main"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "content"
    / "mte"
    / "MteInPlaceKind.java"
)
MTE_BLOCK = (
    census.ROOT
    / "src"
    / "main"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "content"
    / "block"
    / "MteInPlaceBlock.java"
)
MTE_BE = (
    census.ROOT
    / "src"
    / "main"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "content"
    / "blockentity"
    / "MteInPlaceBlockEntity.java"
)
MENUS = (
    census.ROOT
    / "src"
    / "main"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "registry"
    / "ModMenus.java"
)
CREATIVE_TABS = (
    census.ROOT
    / "src"
    / "main"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "registry"
    / "ModCreativeTabs.java"
)
PLAN_ACTIVE = (
    census.ROOT / "docs" / "history" / "card-plans" / "active" / "GT6仓储美术详细计划.md"
)
MODEL_BY_PROFILE = {
    "bookshelf_metal": "block/storage_bookshelf_metal",
    "bookshelf_wood": "block/storage_bookshelf_wood",
    "bottle_crate": "block/storage_bottle_crate_wood",
    "bottle_crate_wood": "block/storage_bottle_crate_wood",
    "bottle_crate_metal": "block/storage_bottle_crate_metal",
    "drawer": "block/storage_drawer",
    "locker": "block/storage_locker",
    "charging_locker": "block/storage_charging_locker",
    "mass_storage_barrel": "block/storage_mass_barrel",
    "mass_storage_box": "block/storage_mass_box",
    "mass_storage_standard": "block/storage_mass",
    "mass_storage_logistics": "block/storage_mass_logistics",
    "storage_inserter": "block/storage_inserter",
    "inserter": "block/storage_inserter",
}
MTE_PARENTS = {
    "BARREL": "cruciblecraft:block/storage_mass_barrel",
    "BOOKSHELF": "cruciblecraft:block/storage_bookshelf_metal",
    "BOTTLE_CRATE": "cruciblecraft:block/storage_bottle_crate_wood",
    "DRAWER": "cruciblecraft:block/storage_drawer",
    "LOCKER": "cruciblecraft:block/storage_locker",
    "MASS_STORAGE": "cruciblecraft:block/storage_mass",
    "SAFE": "cruciblecraft:block/storage_safe_mechanical",
}
SAFE_FACE_FILES = {
    "bottom": "side",
    "top": "side",
    "front": "front",
    "back": "back",
    "side": "side",
}
CHEST_ENTITY = ("metalchest", "woodchest", "lootchest")
CHEST_ITEM_DISPLAY = {
    "gui": {"rotation": [30, 45, 0], "scale": [0.625, 0.625, 0.625]},
    "ground": {"translation": [0, 3, 0], "scale": [0.25, 0.25, 0.25]},
    "head": {"rotation": [0, 180, 0]},
    "fixed": {"rotation": [0, 180, 0], "scale": [0.5, 0.5, 0.5]},
    "thirdperson_righthand": {
        "rotation": [75, 45, 0],
        "translation": [0, 2.5, 0],
        "scale": [0.375, 0.375, 0.375],
    },
    "firstperson_righthand": {"rotation": [0, 45, 0], "scale": [0.4, 0.4, 0.4]},
    "firstperson_lefthand": {"rotation": [0, 225, 0], "scale": [0.4, 0.4, 0.4]},
}
ID_RENAMES = (
    (re.compile(r"mass_storage_barrel_(\d+)"), r"item_barrel_\1"),
    (re.compile(r"mass_storage_box_(\d+)"), r"plastic_storage_box_\1"),
)
REMAP_ROOTS = (
    census.ROOT / "src" / "main",
    census.ROOT / "src" / "test",
    census.ROOT / "tools",
    census.ROOT / "docs" / "history" / "card-plans" / "active",
)
STALE_GENERATED = re.compile(
    r"^mass_storage_(?:barrel|box)_\d+(?:_from_\d+)?\.json$"
)
LEGACY_SIDE_COPIES = {
    "BARREL": ("machines/massstorage/barrel/colored/side.png", "mte/barrel.png"),
    "BOOKSHELF": (PLANKS_REL, "mte/bookshelf.png"),
    "BOTTLE_CRATE": (PLANKS_REL, "mte/bottle_crate.png"),
    "DRAWER": ("machines/drawers/quad/colored/side.png", "mte/drawer.png"),
    "LOCKER": ("machines/lockers/normal/colored/side.png", "mte/locker.png"),
    "MASS_STORAGE": (
        "machines/massstorage/standard/colored/side.png",
        "mte/mass_storage.png",
    ),
}
VANILLA_FORBIDDEN = (
    "minecraft:block/barrel",
    "minecraft:block/oak_planks",
    "minecraft:block/bookshelf",
    "minecraft:block/iron_block",
)


def _write_json(path: Path, document: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    payload = json.dumps(document, ensure_ascii=False, indent=2) + "\n"
    if path.is_file():
        try:
            if json.loads(path.read_text(encoding="utf-8")) == document:
                return
        except (OSError, json.JSONDecodeError):
            pass
    tmp = path.with_name(path.name + ".tmp")
    last_error: OSError | None = None
    for attempt in range(8):
        try:
            tmp.write_text(payload, encoding="utf-8")
            os.replace(tmp, path)
            return
        except OSError as error:
            last_error = error
            time.sleep(0.05 * (attempt + 1))
            try:
                tmp.unlink(missing_ok=True)
            except OSError:
                pass
    raise last_error or OSError(f"could not write {path}")


def _sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def _gt6_file(relative: str) -> Path:
    rel = relative.replace("\\", "/")
    if rel.startswith("model/") or rel.startswith("gui/"):
        return GT6_W / "src/main/resources/assets/gregtech/textures" / rel
    return GT6_W / GT6_TEX / rel


def _dest_file(destination: str) -> Path:
    rel = destination.replace("\\", "/")
    prefix = "assets/cruciblecraft/"
    if not rel.startswith(prefix):
        raise ValueError(destination)
    return ASSETS / rel[len(prefix) :]


def _tex(folder: str, layer: str, face: str) -> str:
    return f"cruciblecraft:block/gt6_import/storage/{folder}/{layer}_{face}"


def _overlay_cube(
        folder: str,
        *,
        all_sides: bool = False,
        face_files: dict[str, str] | None = None,
) -> dict[str, Any]:
    textures: dict[str, str] = {}
    for layer, prefix in (("colored", "bot"), ("overlay", "top")):
        for face in FACES:
            if all_sides:
                textures[f"{prefix}_{face}"] = _tex(folder, layer, "sides")
            else:
                src = (face_files or {}).get(face, face)
                textures[f"{prefix}_{face}"] = _tex(folder, layer, src)
    textures["particle"] = textures["bot_side"]
    elements = []
    for prefix, tint in (("bot", True), ("top", False)):
        faces = {}
        for mc, gt in MC_FACES:
            face: dict[str, Any] = {
                "texture": f"#{prefix}_{gt}",
                "cullface": mc,
            }
            if tint:
                face["tintindex"] = 0
            faces[mc] = face
        elements.append(
            {"from": [0, 0, 0], "to": [16, 16, 16], "faces": faces}
        )
    return {
        "parent": "minecraft:block/block",
        "render_type": "minecraft:cutout",
        "textures": textures,
        "elements": elements,
    }


def _voxel_from_isbrh(src: Path, body: str, credit: str) -> dict[str, Any]:
    raw = json.loads(src.read_text(encoding="utf-8"))
    elements = []
    for element in raw["elements"]:
        faces = {}
        for name, face in (element.get("faces") or {}).items():
            mapped: dict[str, Any] = {"texture": "#body", "tintindex": 0}
            if "cullface" in face:
                mapped["cullface"] = face["cullface"]
            faces[name] = mapped
        elements.append(
            {
                "from": element["from"],
                "to": element["to"],
                "faces": faces,
            }
        )
    return {
        "credit": credit,
        "parent": "minecraft:block/block",
        "render_type": "minecraft:cutout",
        "textures": {"body": body, "particle": "#body"},
        "elements": elements,
    }


def _copy_png(relative: str, destination: str, kind: str) -> dict[str, Any]:
    source = _gt6_file(relative)
    if not source.is_file():
        raise FileNotFoundError(f"missing GT6 art {source} for {kind}")
    dest = _dest_file(destination)
    dest.parent.mkdir(parents=True, exist_ok=True)
    if not dest.is_file() or dest.read_bytes() != source.read_bytes():
        shutil.copyfile(source, dest)
    return {
        "source": "gt6_referencable_port_code/gregtech6_w",
        "gt6_source": str(source.relative_to(GT6_W)).replace("\\", "/"),
        "destination": destination,
        "kind": kind,
        "copied": True,
        "sha256": _sha256(source),
    }


def _copy_art() -> list[dict[str, Any]]:
    rows: list[dict[str, Any]] = []
    for kind, folder in MACHINE_SETS.items():
        for layer in LAYERS:
            for face in FACES:
                rel = f"{folder}/{layer}/{face}.png"
                dest = f"{DEST_ROOT}/{kind}/{layer}_{face}.png"
                rows.append(_copy_png(rel, dest, kind))
    for layer, rel in INSERTER_REL.items():
        dest = f"{DEST_ROOT}/storage_inserter/{layer}_sides.png"
        rows.append(_copy_png(rel, dest, "storage_inserter"))
    rows.append(
        _copy_png(PLANKS_REL, f"{DEST_ROOT}/planks_wood.png", "bookshelf_wood")
    )
    rows.append(
        _copy_png(
            METALLIC_REL,
            f"{DEST_ROOT}/metallic_blocksolid.png",
            "bookshelf_metal",
        )
    )
    for kind, (rel, dest_name) in LEGACY_SIDE_COPIES.items():
        rows.append(
            _copy_png(
                rel,
                f"assets/cruciblecraft/textures/block/gt6_import/{dest_name}",
                f"legacy_{kind.lower()}",
            )
        )
    for name in BOOK_PNGS:
        rows.append(
            _copy_png(
                f"books/{name}.png",
                f"{DEST_ROOT}/books/{name}.png",
                "bookshelf_book",
            )
        )
    for name in BOTTLE_PNGS:
        rows.append(
            _copy_png(
                f"iconsets/{name}.png",
                f"{DEST_ROOT}/bottle/{name}.png",
                "bottle_crate_bottle",
            )
        )
    for kind, folder in (
        ("safe_mechanical", "machines/safes/mechanical"),
        ("safe_keylocked", "machines/safes/keylocked"),
    ):
        for layer in LAYERS:
            for face in ("front", "back", "side"):
                rows.append(
                    _copy_png(
                        f"{folder}/{layer}/{face}.png",
                        f"{DEST_ROOT}/{kind}/{layer}_{face}.png",
                        kind,
                    )
                )
    for name in CHEST_ENTITY:
        for layer in ("colored", "plain"):
            rel = f"model/gt.multitileentity/{name}.{layer}.png"
            rows.append(
                _copy_png(
                    rel,
                    "assets/cruciblecraft/textures/entity/gt6_import/"
                    f"chest/{name}_{layer}.png",
                    "chest",
                )
            )
            rows.append(
                _copy_png(
                    rel,
                    f"{DEST_ROOT}/chest/{name}_{layer}.png",
                    "chest",
                )
            )
    return rows


def _write_models() -> None:
    mapping = {
        "storage_mass_barrel": "mass_storage_barrel",
        "storage_mass_box": "mass_storage_box",
        "storage_mass": "mass_storage_standard",
        "storage_mass_logistics": "mass_storage_logistics",
        "storage_drawer": "drawer",
        "storage_locker": "locker",
        "storage_charging_locker": "charging_locker",
    }
    for name, folder in mapping.items():
        _write_json(BLOCK_MODELS / f"{name}.json", _overlay_cube(folder))
    _write_json(
        BLOCK_MODELS / "storage_inserter.json",
        _overlay_cube("storage_inserter", all_sides=True),
    )
    _write_json(
        BLOCK_MODELS / "storage_safe_mechanical.json",
        _overlay_cube("safe_mechanical", face_files=SAFE_FACE_FILES),
    )
    _write_json(
        BLOCK_MODELS / "storage_safe_keylocked.json",
        _overlay_cube("safe_keylocked", face_files=SAFE_FACE_FILES),
    )
    _write_json(
        BLOCK_MODELS / "mte_inplace_safe_keylocked.json",
        {"parent": "cruciblecraft:block/storage_safe_keylocked"},
    )
    for name in CHEST_ENTITY:
        _write_json(
            BLOCK_MODELS / f"mte_inplace_chest_{name}.json",
            _chest_block_model(name),
        )
    _write_json(
        BLOCK_MODELS / "mte_inplace_chest.json",
        {"parent": "cruciblecraft:block/mte_inplace_chest_metalchest"},
    )
    plank = "cruciblecraft:block/gt6_import/storage/planks_wood"
    metal = "cruciblecraft:block/gt6_import/storage/metallic_blocksolid"
    _write_json(
        BLOCK_MODELS / "storage_bookshelf_wood.json",
        _voxel_from_isbrh(
            ISBRH / "MultiTileEntityBookShelf.json",
            plank,
            "GT6 MultiTileEntityBookShelf empty-state ISBRH; plank + tintindex 0",
        ),
    )
    _write_json(
        BLOCK_MODELS / "storage_bookshelf_metal.json",
        _voxel_from_isbrh(
            ISBRH / "MultiTileEntityBookShelf.json",
            metal,
            "GT6 MultiTileEntityBookShelf empty-state ISBRH; metallic casing + tintindex 0",
        ),
    )
    _write_json(
        BLOCK_MODELS / "storage_bookshelf.json",
        {"parent": "cruciblecraft:block/storage_bookshelf_metal"},
    )
    crate = ISBRH / "MultiTileEntityBottleCrate.json"
    _write_json(
        BLOCK_MODELS / "storage_bottle_crate_wood.json",
        _voxel_from_isbrh(
            crate,
            plank,
            "GT6 MultiTileEntityBottleCrate empty-state ISBRH; plank + tintindex 0",
        ),
    )
    _write_json(
        BLOCK_MODELS / "storage_bottle_crate_metal.json",
        _voxel_from_isbrh(
            crate,
            metal,
            "GT6 MultiTileEntityBottleCrate empty-state ISBRH; metallic casing + tintindex 0",
        ),
    )
    _write_json(
        BLOCK_MODELS / "storage_bottle_crate.json",
        {"parent": "cruciblecraft:block/storage_bottle_crate_wood"},
    )
    for kind, parent in MTE_PARENTS.items():
        _write_json(
            BLOCK_MODELS / f"mte_inplace_{kind.lower()}.json",
            {"parent": parent},
        )


def model_path(profile: str, family: str) -> str:
    if profile in MODEL_BY_PROFILE:
        return MODEL_BY_PROFILE[profile]
    if family in MODEL_BY_PROFILE:
        return MODEL_BY_PROFILE[family]
    if family == "mass_storage_standard":
        return "block/storage_mass"
    if family == "locker" and profile == "charging_locker":
        return "block/storage_charging_locker"
    raise KeyError(f"unmapped storage model {profile=} {family=}")


def _horizontal_blockstate(model: str) -> dict[str, Any]:
    namespaced = f"cruciblecraft:{model}"
    return {
        "variants": {
            "facing=east": {"model": namespaced, "y": 90},
            "facing=north": {"model": namespaced},
            "facing=south": {"model": namespaced, "y": 180},
            "facing=west": {"model": namespaced, "y": 270},
        }
    }


def _facing_blockstate(model: str) -> dict[str, Any]:
    namespaced = f"cruciblecraft:{model}"
    return {
        "variants": {
            "facing=down": {"model": namespaced, "x": 90},
            "facing=east": {"model": namespaced, "y": 90},
            "facing=north": {"model": namespaced},
            "facing=south": {"model": namespaced, "y": 180},
            "facing=up": {"model": namespaced, "x": 270},
            "facing=west": {"model": namespaced, "y": 270},
        }
    }


def _entity_box_uvs(
        u: float, v: float, width: float, height: float, depth: float
) -> dict[str, list[float]]:
    """Vanilla 64x64 entity-box UVs expressed in block-model 0-16 space."""
    scale = 16.0 / 64.0

    def px(u0: float, v0: float, u1: float, v1: float) -> list[float]:
        return [u0 * scale, v0 * scale, u1 * scale, v1 * scale]

    return {
        "west": px(u, v + depth, u + depth, v + depth + height),
        "north": px(u + depth, v + depth, u + depth + width, v + depth + height),
        "east": px(
            u + depth + width,
            v + depth,
            u + depth + width + depth,
            v + depth + height,
        ),
        "south": px(
            u + depth + width + depth,
            v + depth,
            u + depth + width + depth + width,
            v + depth + height,
        ),
        "up": px(u + depth, v, u + depth + width, v + depth),
        "down": px(u + depth + width, v, u + depth + width + width, v + depth),
    }


def _chest_faces(
        texture: str, tint: bool, uvs: dict[str, list[float]]
) -> dict[str, Any]:
    mapped: dict[str, Any] = {}
    for mc, uv in uvs.items():
        face: dict[str, Any] = {"uv": uv, "texture": texture}
        if tint:
            face["tintindex"] = 0
        mapped[mc] = face
    return mapped


def _chest_layer(
        from_pos: list[float],
        to_pos: list[float],
        u: float,
        v: float,
        width: float,
        height: float,
        depth: float,
        texture: str,
        tint: bool,
) -> dict[str, Any]:
    return {
        "from": from_pos,
        "to": to_pos,
        "faces": _chest_faces(
            texture, tint, _entity_box_uvs(u, v, width, height, depth)
        ),
    }


def _chest_block_model(name: str) -> dict[str, Any]:
    """Closed vanilla chest voxels mapped from the GT6 64x64 TESR sheet."""
    colored = f"cruciblecraft:block/gt6_import/storage/chest/{name}_colored"
    plain = f"cruciblecraft:block/gt6_import/storage/chest/{name}_plain"
    elements: list[dict[str, Any]] = []
    for texture, tint in (("#colored", True), ("#plain", False)):
        elements.append(
            _chest_layer([1, 0, 1], [15, 10, 15], 0, 19, 14, 10, 14, texture, tint)
        )
        elements.append(
            _chest_layer([1, 9, 1], [15, 14, 15], 0, 0, 14, 5, 14, texture, tint)
        )
        elements.append(
            _chest_layer([7, 8, 0], [9, 12, 1], 0, 0, 2, 4, 1, texture, tint)
        )
    return {
        "parent": "minecraft:block/block",
        "render_type": "minecraft:cutout",
        "textures": {
            "colored": colored,
            "plain": plain,
            "particle": colored,
        },
        "elements": elements,
        "display": CHEST_ITEM_DISPLAY,
    }


def _chest_item_model(name: str) -> dict[str, Any]:
    return {
        "parent": f"cruciblecraft:block/mte_inplace_chest_{name}",
        "display": CHEST_ITEM_DISPLAY,
    }


def _chest_profile(path: str) -> str:
    if "reinforced_wooden" in path:
        return "woodchest"
    if "stone_chest" in path or path.endswith("/lootchest"):
        return "lootchest"
    return "metalchest"


def _rewrite_chest_safe_assets() -> None:
    from tools import gt6_mte_inplace_runtime as inplace

    item_models = ASSETS / "models" / "item"
    for row in census.load_json(inplace.INPLACE_CATALOG).get("identities") or []:
        kind = str(row.get("kind") or "")
        path = str(row.get("registry_path") or "")
        if kind == "SAFE":
            model = (
                "block/mte_inplace_safe_keylocked"
                if "key_locked" in path
                else "block/mte_inplace_safe"
            )
            _write_json(ASSETS / "blockstates" / f"{path}.json", _facing_blockstate(model))
            _write_json(
                item_models / f"{path}.json",
                {"parent": f"cruciblecraft:{model}"},
            )
            continue
        if kind != "CHEST":
            continue
        profile = _chest_profile(path)
        model = f"block/mte_inplace_chest_{profile}"
        _write_json(ASSETS / "blockstates" / f"{path}.json", _facing_blockstate(model))
        _write_json(item_models / f"{path}.json", _chest_item_model(profile))


def remap_barrel_box_ids(text: str) -> str:
    for pattern, replacement in ID_RENAMES:
        text = pattern.sub(replacement, text)
    return text


def _remap_barrel_box_id_tree() -> None:
    suffixes = {".java", ".json", ".md", ".py"}
    for root in REMAP_ROOTS:
        if not root.is_dir():
            continue
        for path in root.rglob("*"):
            if not path.is_file() or path.suffix.lower() not in suffixes:
                continue
            if path.resolve() == Path(__file__).resolve():
                continue
            if path.name in {"storage_catalog.json", "source.json"}:
                continue
            if "__pycache__" in path.parts:
                continue
            try:
                original = path.read_text(encoding="utf-8")
            except (OSError, UnicodeDecodeError):
                continue
            updated = remap_barrel_box_ids(original)
            if updated == original:
                continue
            try:
                path.write_text(updated, encoding="utf-8")
            except OSError:
                continue


def _delete_stale_generated_ids() -> None:
    roots = (
        GENERATED / "blockstates",
        GENERATED / "models" / "item",
        census.ROOT
        / "src"
        / "generated"
        / "resources"
        / "data"
        / "cruciblecraft"
        / "loot_table"
        / "blocks",
        census.ROOT
        / "src"
        / "generated"
        / "resources"
        / "data"
        / "cruciblecraft"
        / "recipe"
        / "storage",
    )
    for root in roots:
        if not root.is_dir():
            continue
        for path in list(root.iterdir()):
            if not path.is_file() or not STALE_GENERATED.match(path.name):
                continue
            new_name = remap_barrel_box_ids(path.name)
            dest = path.with_name(new_name)
            if new_name == path.name:
                continue
            if dest.is_file():
                path.unlink()
            else:
                path.rename(dest)


def _rewrite_t44_assets() -> None:
    catalog = census.load_json(CATALOG)
    for row in catalog["variants"]:
        profile = str(row.get("model_profile") or "")
        family = str(row["family"])
        model = model_path(profile, family)
        runtime_id = str(row["runtime_id"])
        path = runtime_id.split(":", 1)[1]
        _write_json(
            GENERATED / "blockstates" / f"{path}.json",
            _horizontal_blockstate(model),
        )
        _write_json(
            GENERATED / "models" / "item" / f"{path}.json",
            {"parent": f"cruciblecraft:{model}"},
        )


def _copy_empty_nbt() -> None:
    if not EMPTY_SRC.is_file():
        raise FileNotFoundError(EMPTY_SRC)
    for dest in (
        PACK / "structure" / "empty.nbt",
        PACK / "gametest" / "structure" / "empty.nbt",
    ):
        dest.parent.mkdir(parents=True, exist_ok=True)
        if not dest.is_file() or dest.read_bytes() != EMPTY_SRC.read_bytes():
            shutil.copyfile(EMPTY_SRC, dest)


def write(unique_active: bool = True) -> None:
    WAVE.mkdir(parents=True, exist_ok=True)
    _remap_barrel_box_id_tree()
    _delete_stale_generated_ids()
    manifest = {"rows": _copy_art()}
    _write_json(MANIFEST, manifest)
    _write_json(WAVE / "art_manifest.json", manifest)
    _write_models()
    _rewrite_t44_assets()
    _rewrite_chest_safe_assets()
    _copy_empty_nbt()
    ensure_wood_treated_plate()
    _write_json(
        WAVE / "topology.json",
        {
            "schema_version": 1,
            "wave_slug": SLUG,
            "unique_active_wave": SLUG if unique_active else None,
            "status": STATUS,
            "source_revision": GT6_REVISION,
            "generated_by": "content/gt6-storage-art implementation",
            "append_only": False,
            "next_unassigned": False,
            "remaining_recipe_gap": 0,
        },
    )
    _write_json(
        WAVE / "readiness.json",
        {
            "schema_version": 1,
            "wave_slug": SLUG,
            "unique_active_wave": SLUG if unique_active else None,
            "status": STATUS,
            "source_revision": GT6_REVISION,
            "close_target": "runtime_ready",
            "obtain": "explicitly_blocked",
        },
    )
    _write_json(
        WAVE / "runtime_notes.json",
        {
            "no_vanilla_oak_iron": True,
            "no_per_material_png": True,
            "kind_level_colored_overlay": True,
            "digits_books_bottles_remain_ber": False,
            "face_item_count_books_bottles": True,
            "dual_t44_and_inplace_ids_remain": False,
            "reopens_furniture_required_test_ids": False,
            "metal_inplace_player_storage": True,
            "obtain": "explicitly_blocked",
        },
    )
    _write_json(
        WAVE / "production_lock.json",
        {
            "note": (
                "storage kind-level art on live BlockItems; 17 T44/MTE duals "
                "folded; not player_complete"
            )
        },
    )
    from tools import gt6_mte_inplace_runtime as inplace

    inplace.apply_t44_storage_host_fold()


def check() -> list[str]:
    errors: list[str] = []
    if not MANIFEST.is_file():
        return ["missing gt6_storage_art_manifest.json"]
    manifest = census.load_json(MANIFEST)
    rows = manifest.get("rows") or []
    if len(rows) < 40:
        errors.append(f"storage art manifest too small: {len(rows)}")
    for row in rows:
        dest = _dest_file(str(row["destination"]))
        source = GT6_W / str(row["gt6_source"])
        if not dest.is_file():
            errors.append(f"missing dest {row['destination']}")
            continue
        if not source.is_file():
            errors.append(f"missing GT6 {row['gt6_source']}")
            continue
        if dest.read_bytes() != source.read_bytes():
            errors.append(f"bytes drifted {row['destination']}")
        if "minecraft:block" in str(row.get("gt6_source")):
            errors.append(f"vanilla source {row['gt6_source']}")
    for name in (
        "storage_mass_barrel.json",
        "storage_mass_box.json",
        "storage_mass.json",
        "storage_mass_logistics.json",
        "storage_drawer.json",
        "storage_locker.json",
        "storage_charging_locker.json",
        "storage_inserter.json",
        "storage_bookshelf_wood.json",
        "storage_bookshelf_metal.json",
        "storage_bottle_crate.json",
        "storage_bottle_crate_wood.json",
        "storage_bottle_crate_metal.json",
        "storage_safe_mechanical.json",
        "storage_safe_keylocked.json",
        "mte_inplace_barrel.json",
        "mte_inplace_mass_storage.json",
        "mte_inplace_safe.json",
        "mte_inplace_safe_keylocked.json",
        "mte_inplace_chest.json",
        "mte_inplace_chest_metalchest.json",
        "mte_inplace_chest_woodchest.json",
        "mte_inplace_chest_lootchest.json",
    ):
        path = BLOCK_MODELS / name
        if not path.is_file():
            errors.append(f"missing parent {name}")
            continue
        text = path.read_text(encoding="utf-8")
        for token in VANILLA_FORBIDDEN:
            if token in text:
                errors.append(f"{name} still uses {token}")
        if name.startswith("mte_inplace_"):
            if "cube_all" in text:
                errors.append(f"{name} still cube_all")
            if "gt6_import/mte/" in text:
                errors.append(f"{name} still points at missing mte png")
            if "chest" in name and "minecraft:block/chest" in text:
                errors.append(f"{name} aliases the vanilla chest")
            if "chest" in name and name != "mte_inplace_chest.json":
                model = json.loads(text)
                tos = {
                    tuple(element.get("to") or [])
                    for element in model.get("elements") or []
                }
                if (15, 10, 15) not in tos or (15, 14, 15) not in tos:
                    errors.append(f"{name} is not lid+body chest voxels")
                if "gt6_import/storage/chest/" not in text:
                    errors.append(f"{name} does not use the block chest sheet")
        elif "tintindex" not in text and "parent" not in text:
            errors.append(f"{name} missing tintindex")
    barrel_state = GENERATED / "blockstates" / "item_barrel_6999.json"
    if not barrel_state.is_file():
        errors.append("missing generated item_barrel_6999 blockstate")
    else:
        text = barrel_state.read_text(encoding="utf-8")
        if "storage_mass_barrel" not in text:
            errors.append("T44 barrel blockstate is not storage_mass_barrel")
        for token in VANILLA_FORBIDDEN:
            if token in text:
                errors.append(f"T44 barrel blockstate uses {token}")
    catalog_ids = {
        str(row.get("runtime_id") or "")
        for row in census.load_json(CATALOG).get("variants") or []
    }
    if "cruciblecraft:item_barrel_6999" not in catalog_ids:
        errors.append("T44 treated barrel id is not item_barrel_6999")
    if "cruciblecraft:plastic_storage_box_6996" not in catalog_ids:
        errors.append("T44 plastic box id is not plastic_storage_box_6996")
    if "cruciblecraft:mass_storage_barrel_6999" in catalog_ids:
        errors.append("T44 treated barrel still uses mass_storage_barrel id")
    if "cruciblecraft:mass_storage_6000" not in catalog_ids:
        errors.append("Mass Storage 6000 id drifted")
    lead_chest = ASSETS / "blockstates" / "lead" / "chest.json"
    if not lead_chest.is_file():
        errors.append("missing lead/chest blockstate")
    else:
        text = lead_chest.read_text(encoding="utf-8")
        if "mte_inplace_chest" not in text or "cube_all" in text:
            errors.append("lead/chest still uses the cube_all placeholder")
    lead_safe = ASSETS / "blockstates" / "safe" / "mechanical_lead_safe.json"
    if not lead_safe.is_file():
        errors.append("missing mechanical lead safe blockstate")
    else:
        text = lead_safe.read_text(encoding="utf-8")
        if "storage_safe_mechanical" not in text and "mte_inplace_safe" not in text:
            errors.append("mechanical lead safe is not the GT6 overlay model")
    aluminium_chest_item = ASSETS / "models" / "item" / "aluminium" / "chest.json"
    if not aluminium_chest_item.is_file():
        errors.append("missing aluminium chest item model")
    elif "builtin/entity" in aluminium_chest_item.read_text(encoding="utf-8"):
        errors.append("aluminium chest item still uses builtin/entity")
    for name in CHEST_ENTITY:
        if not _dest_file(f"{DEST_ROOT}/chest/{name}_colored.png").is_file():
            errors.append(f"missing block chest sheet {name}_colored")
        if not _dest_file(f"{DEST_ROOT}/chest/{name}_plain.png").is_file():
            errors.append(f"missing block chest sheet {name}_plain")
        entity_texture = (
            ASSETS
            / "textures"
            / "entity"
            / "gt6_import"
            / "chest"
            / f"{name}_colored.png"
        )
        if not entity_texture.is_file():
            errors.append(f"missing entity chest sheet {name}_colored")
        entity_texture = entity_texture.with_name(f"{name}_plain.png")
        if not entity_texture.is_file():
            errors.append(f"missing entity chest sheet {name}_plain")
    materials = (
        census.ROOT
        / "src"
        / "main"
        / "java"
        / "com"
        / "masson"
        / "cruciblecraft"
        / "content"
        / "mte"
        / "MteInPlaceMaterials.java"
    )
    if not materials.is_file():
        errors.append("missing MteInPlaceMaterials")
    else:
        text = materials.read_text(encoding="utf-8")
        if "compartment_drawer_" not in text or "key_locked_" not in text:
            errors.append("MteInPlaceMaterials dropped furniture/safe tokens")
        if "cube_all" in text:
            errors.append("mechanical lead safe still uses cube_all")
    if not (PACK / "structure" / "empty.nbt").is_file():
        errors.append("missing storage-art structure/empty.nbt")
    if not (PACK / "gametest" / "structure" / "empty.nbt").is_file():
        errors.append("missing storage-art gametest/structure/empty.nbt")
    tests = GAME_TESTS.read_text(encoding="utf-8") if GAME_TESTS.is_file() else ""
    for name in EXPECTED_TESTS:
        if f"void {name}" not in tests:
            errors.append(f"missing GameTest {name}")
        if name in CORE_TESTS.read_text(encoding="utf-8"):
            errors.append(f"CrucibleCraftGameTests absorbed {name}")
    notes = (
        census.load_json(WAVE / "runtime_notes.json")
        if (WAVE / "runtime_notes.json").is_file()
        else {}
    )
    if notes.get("reopens_furniture_required_test_ids"):
        errors.append("runtime_notes reopens furniture required_test_ids")
    if notes.get("dual_t44_and_inplace_ids_remain"):
        errors.append("runtime_notes still keeps T44/in-place dual ids")
    errors.extend(_check_source_exact_storage_recipes())
    errors.extend(_check_wood_treated_plate_gate())
    from tools import gt6_mte_inplace_runtime as inplace

    inplace_ids = {
        str(row.get("registry_path") or "")
        for row in census.load_json(inplace.INPLACE_CATALOG).get("identities") or []
    }
    overlay_fold = 0
    for domain in inplace.T44_FOLD_DOMAINS:
        overlay = census.load_json(
            inplace._wave(domain) / "runtime_overlay.json"
        )
        for row in overlay.get("rows") or []:
            if row.get("disposition") != "fold_live_block":
                continue
            overlay_fold += 1
            dummy = str(row.get("dummy_path") or "")
            live = str(row.get("live_block") or "").split(":", 1)[-1]
            if dummy in inplace_ids:
                errors.append(f"folded dummy still in-place {dummy}")
            if live not in {
                str(item.get("runtime_id") or "").split(":", 1)[-1]
                for item in census.load_json(CATALOG).get("variants") or []
            }:
                errors.append(f"folded live host missing from T44 {live}")
    if overlay_fold != 17:
        errors.append(f"expected 17 T44 folds, found {overlay_fold}")
    if not notes.get("face_item_count_books_bottles"):
        errors.append("runtime_notes missing face_item_count_books_bottles")
    if notes.get("digits_books_bottles_remain_ber"):
        errors.append("runtime_notes still defers digits/books/bottles")
    for name in BOOK_PNGS:
        if not _dest_file(f"{DEST_ROOT}/books/{name}.png").is_file():
            errors.append(f"missing book texture {name}")
    for name in BOTTLE_PNGS:
        if not _dest_file(f"{DEST_ROOT}/bottle/{name}.png").is_file():
            errors.append(f"missing bottle texture {name}")
    setup = CLIENT_SETUP.read_text(encoding="utf-8") if CLIENT_SETUP.is_file() else ""
    for marker in RENDERER_MARKERS:
        path = (
            census.ROOT
            / "src"
            / "main"
            / "java"
            / "com"
            / "masson"
            / "cruciblecraft"
            / "client"
            / "render"
            / f"{marker}.java"
        )
        if not path.is_file():
            errors.append(f"missing renderer {marker}")
        if marker not in setup:
            errors.append(f"ClientSetup does not register {marker}")
    if "registerChestItemRenderers" in setup:
        errors.append("chest items still use the TESR item renderer")
    kind = MTE_KIND.read_text(encoding="utf-8") if MTE_KIND.is_file() else ""
    if "case CHEST -> 54" not in kind:
        errors.append("in-place chest is not 54 slots")
    if "case DRAWER -> 144" not in kind:
        errors.append("in-place drawer is not 144 slots")
    if "case SAFE -> 15" not in kind:
        errors.append("in-place safe is not 15 slots")
    if "playerInventoryGui" not in kind or "storageTab" not in kind:
        errors.append("in-place storage flags missing")
    block = MTE_BLOCK.read_text(encoding="utf-8") if MTE_BLOCK.is_file() else ""
    if "openMenu" not in block:
        errors.append("in-place storage still has no player GUI")
    if "ENTITYBLOCK_ANIMATED" not in block:
        errors.append("in-place chest is not TESR-animated")
    chest_renderer = (
        census.ROOT
        / "src"
        / "main"
        / "java"
        / "com"
        / "masson"
        / "cruciblecraft"
        / "client"
        / "render"
        / "GtChestRenderer.java"
    ).read_text(encoding="utf-8")
    if "lid.xRot = lidAngle" not in chest_renderer:
        errors.append("GT6 chest lid is still locked shut")
    if "materialColor(spec)" not in chest_renderer:
        errors.append("GT6 chest colored pass lost material tint")
    if "textures/entity/gt6_import/chest/" not in chest_renderer:
        errors.append("GT6 chest TESR texture path is missing textures/")
    if '" + set + "_" + layer + ".png"' not in chest_renderer:
        errors.append("GT6 chest TESR texture path is missing .png")
    storage_color = (
        census.ROOT
        / "src"
        / "main"
        / "java"
        / "com"
        / "masson"
        / "cruciblecraft"
        / "client"
        / "color"
        / "StorageArtColor.java"
    ).read_text(encoding="utf-8")
    if "CHEST" not in storage_color:
        errors.append("GT6 chest item color registration is missing")
    host = (
        census.ROOT
        / "src"
        / "main"
        / "java"
        / "com"
        / "masson"
        / "cruciblecraft"
        / "content"
        / "blockentity"
        / "MteInPlaceBlockEntity.java"
    ).read_text(encoding="utf-8")
    if "lidOpenness" not in host or "CHEST_OPEN" not in host:
        errors.append("GT6 chest opener count does not drive the lid")
    mass_renderer = (
        census.ROOT
        / "src"
        / "main"
        / "java"
        / "com"
        / "masson"
        / "cruciblecraft"
        / "client"
        / "render"
        / "MassStorageRenderer.java"
    ).read_text(encoding="utf-8")
    if "-facing.toYRot()" not in mass_renderer:
        errors.append("mass-storage face item still uses the compass yaw")
    if "StorageCountFormat.face" not in mass_renderer:
        errors.append("mass-storage face count is not drawn")
    if "swapArmor" not in block:
        errors.append("in-place locker still has no armor swap")
    if "massStorageActivated" not in block:
        errors.append("in-place mass storage still has no face click")
    be = MTE_BE.read_text(encoding="utf-8") if MTE_BE.is_file() else ""
    if "MASS_CAPACITY = 1_000_000" not in be:
        errors.append("in-place mass storage capacity drifted")
    if "ModMenus.MTE_STORAGE" not in be:
        errors.append("in-place storage menu missing")
    menus = MENUS.read_text(encoding="utf-8") if MENUS.is_file() else ""
    if "MTE_STORAGE" not in menus:
        errors.append("ModMenus missing MTE_STORAGE")
    tabs = CREATIVE_TABS.read_text(encoding="utf-8") if CREATIVE_TABS.is_file() else ""
    if "storageTab()" not in tabs:
        errors.append("STORAGE tab still omits remaining in-place furniture")
    if not notes.get("metal_inplace_player_storage"):
        errors.append("runtime_notes still treats remaining metals as placeholders")
    plan = PLAN_ACTIVE.read_text(encoding="utf-8") if PLAN_ACTIVE.is_file() else ""
    if "仍是独立 dummy" in plan:
        errors.append("plan still calls remaining metals dummy")
    zh_path = GENERATED / "lang" / "zh_cn.json"
    if not zh_path.is_file():
        errors.append("missing generated zh_cn.json")
    else:
        zh = census.load_json(zh_path)
        expected_zh = {
            "block.cruciblecraft.furniture.locker_aluminium": "铝储物柜",
            "item.cruciblecraft.furniture.locker_aluminium": "铝储物柜",
            "block.cruciblecraft.furniture.compartment_drawer_aluminium": "铝分区抽屉",
            "block.cruciblecraft.furniture.mass_storage_aluminium": "铝大容量仓储",
            "block.cruciblecraft.furniture.bottlecrate_aluminium": "铝瓶箱",
            "block.cruciblecraft.safe.mechanical_lead_safe": "铅机械保险箱",
            "block.cruciblecraft.safe.key_locked_lead_safe": "铅钥匙保险箱",
        }
        for key, name in expected_zh.items():
            got = zh.get(key)
            if got != name:
                errors.append(f"zh_cn {key} is {got!r}, expected {name!r}")
    catalog = census.load_json(CATALOG)
    seen: set[str] = set()
    for row in catalog["variants"]:
        try:
            seen.add(model_path(str(row.get("model_profile") or ""), str(row["family"])))
        except KeyError as failure:
            errors.append(str(failure))
    return errors


def _recipe_json(path: Path) -> dict[str, Any]:
    if not path.is_file():
        return {}
    return census.load_json(path)


def _check_source_exact_storage_recipes() -> list[str]:
    errors: list[str] = []
    recipes = (
        census.ROOT
        / "src"
        / "generated"
        / "resources"
        / "data"
        / "cruciblecraft"
        / "recipe"
        / "storage"
    )
    treated = _recipe_json(recipes / "item_barrel_6999.json")
    if treated.get("type") != "cruciblecraft:shaped_catalyst":
        errors.append("treated barrel recipe is not shaped_catalyst")
    else:
        ingredients = treated.get("ingredients") or {}
        if (ingredients.get("P") or {}).get("item") != "cruciblecraft:wood_treated/plate":
            errors.append("treated barrel P is not wood_treated/plate")
        if (ingredients.get("S") or {}).get("item") != "cruciblecraft:iron/long_rod":
            errors.append("treated barrel S is not iron/long_rod")
        if treated.get("pattern") != ["rCs", "PSP", "PSP"]:
            errors.append("treated barrel pattern drifted from GT6")
        if "wood_treated/rod" in json.dumps(treated, ensure_ascii=False):
            errors.append("treated barrel still uses wood_treated/rod stand-in")
    expected_patterns = {
        "6993": ["TPT", "PCP", "TPd"],
        "6994": ["TPT", "PCP", "dPT"],
        "6995": ["TPd", "PCP", "TPT"],
        "6996": ["dPT", "PCP", "TPT"],
    }
    seen_patterns: list[str] = []
    for meta, pattern in expected_patterns.items():
        document = _recipe_json(recipes / f"plastic_storage_box_{meta}.json")
        if document.get("type") != "cruciblecraft:shaped_catalyst":
            errors.append(f"plastic box {meta} is not shaped_catalyst")
            continue
        if document.get("pattern") != pattern:
            errors.append(f"plastic box {meta} screwdriver letter drifted")
        seen_patterns.append(json.dumps(document.get("pattern")))
        ingredients = document.get("ingredients") or {}
        if (ingredients.get("P") or {}).get("item") != "cruciblecraft:plastic/plate":
            errors.append(f"plastic box {meta} P is not plastic/plate")
        if (ingredients.get("T") or {}).get("item") != "cruciblecraft:plastic/screw":
            errors.append(f"plastic box {meta} T is not plastic/screw")
    if len(set(seen_patterns)) != 4:
        errors.append("plastic boxes still share one recipe pattern")
    cycles = (
        (
            "plastic_storage_box_6996_from_6995.json",
            "cruciblecraft:plastic_storage_box_6995",
            "cruciblecraft:plastic_storage_box_6996",
        ),
        (
            "plastic_storage_box_6995_from_6994.json",
            "cruciblecraft:plastic_storage_box_6994",
            "cruciblecraft:plastic_storage_box_6995",
        ),
        (
            "plastic_storage_box_6994_from_6993.json",
            "cruciblecraft:plastic_storage_box_6993",
            "cruciblecraft:plastic_storage_box_6994",
        ),
        (
            "plastic_storage_box_6993_from_6996.json",
            "cruciblecraft:plastic_storage_box_6996",
            "cruciblecraft:plastic_storage_box_6993",
        ),
    )
    for name, source, result in cycles:
        document = _recipe_json(recipes / name)
        if document.get("type") != "minecraft:crafting_shapeless":
            errors.append(f"missing plastic box cycle {name}")
            continue
        ingredients = document.get("ingredients") or []
        ids = [str((slot or {}).get("item") or "") for slot in ingredients]
        if source not in ids:
            errors.append(f"{name} input is not {source}")
        if (document.get("result") or {}).get("id") != result:
            errors.append(f"{name} result is not {result}")
    return errors


def _check_wood_treated_plate_gate() -> list[str]:
    gate_path = (
        census.ROOT
        / "src"
        / "main"
        / "resources"
        / "data"
        / "cruciblecraft"
        / "material_registration_gate.json"
    )
    if not gate_path.is_file():
        return ["missing material registration gate"]
    gate = census.load_json(gate_path)
    forms = (gate.get("materials") or {}).get("wood_treated") or []
    if "plate" not in forms:
        return ["wood_treated/plate is still gated off"]
    overlay = gate.get("gt6_storage_art_required_forms") or {}
    if overlay.get("wood_treated") != ["plate"]:
        return ["gt6_storage_art_required_forms overlay missing wood_treated/plate"]
    return []


def ensure_wood_treated_plate() -> None:
    """Open census-openable wood_treated/plate for GT6 barrel 6999."""
    from tools import currentness

    required = WAVE / "required_forms.json"
    if not required.is_file():
        raise FileNotFoundError(required)
    source_sha = hashlib.sha256(required.read_bytes()).hexdigest()
    gate_path = (
        census.ROOT
        / "src"
        / "main"
        / "resources"
        / "data"
        / "cruciblecraft"
        / "material_registration_gate.json"
    )
    original = gate_path.read_text(encoding="utf-8")
    text = original
    data = json.loads(text)
    overlay_object = (
        '  "gt6_storage_art_required_forms": {\n'
        '    "wood_treated": [\n'
        '      "plate"\n'
        '    ]\n'
        "  },\n"
    )
    overlay = data.get("gt6_storage_art_required_forms")
    if not isinstance(overlay, dict) or overlay.get("wood_treated") != ["plate"]:
        marker = (
            '  "machines_slicer_paper_tiny_plate_required_forms": {\n'
            '    "paper": [\n'
            '      "tiny_plate"\n'
            '    ]\n'
            "  },\n"
        )
        if text.count(marker) != 1:
            raise ValueError("paper tiny_plate overlay marker drifted")
        text = text.replace(marker, overlay_object + marker, 1)
        data = json.loads(text)
    forms = list((data.get("materials") or {}).get("wood_treated") or [])
    if "plate" not in forms:
        old_forms = list(forms)
        new_forms = sorted([*old_forms, "plate"])
        pattern = (
            '    "wood_treated": [\n'
            + ",\n".join(f'      "{form}"' for form in old_forms)
            + "\n    ]"
        )
        replacement = (
            '    "wood_treated": [\n'
            + ",\n".join(f'      "{form}"' for form in new_forms)
            + "\n    ]"
        )
        if text.count(pattern) != 1:
            raise ValueError("materials.wood_treated form list drifted")
        text = text.replace(pattern, replacement, 1)
        registered = int((data.get("counts") or {}).get("registered_forms") or 0)
        text = text.replace(
            f'"registered_forms": {registered}',
            f'"registered_forms": {registered + 1}',
            1,
        )
        data = json.loads(text)
    sources = data.get("sources") or {}
    expected_source = {
        "classification": "semantic_ordinary_runtime_required",
        "field": "required_forms",
        "path": "tools/waves/content/gt6-storage-art/required_forms.json",
        "sha256": source_sha,
    }
    source_row = (
        sources.get("gt6_storage_art_required_forms")
        if isinstance(sources, dict)
        else None
    )
    if source_row != expected_source:
        source_blob = (
            '    "gt6_storage_art_required_forms": {\n'
            '      "classification": "semantic_ordinary_runtime_required",\n'
            '      "field": "required_forms",\n'
            '      "path": "tools/waves/content/gt6-storage-art/required_forms.json",\n'
            f'      "sha256": "{source_sha}"\n'
            "    },\n"
        )
        marker = (
            '    "machines_slicer_paper_tiny_plate_required_forms": {\n'
            '      "classification": "semantic_ordinary_runtime_required",\n'
            '      "field": "required_forms",\n'
            '      "path": "tools/waves/content/gt6-paper-tiny-plate/required_forms.json",\n'
        )
        if "gt6_storage_art_required_forms" in sources:
            old_sha = str((source_row or {}).get("sha256") or "")
            if not old_sha:
                raise ValueError("storage-art gate source sha256 missing")
            text = text.replace(old_sha, source_sha, 1)
        else:
            if text.count(marker) != 1:
                raise ValueError("paper tiny_plate sources marker drifted")
            text = text.replace(marker, source_blob + marker, 1)
        data = json.loads(text)
    sections = list(data.get("java_overlay_sections") or [])
    if "gt6_storage_art_required_forms" not in sections:
        marker = (
            '    "machines_slicer_paper_tiny_plate_required_forms",\n'
            '    "fission_observation_safety_required_forms"'
        )
        replacement = (
            '    "machines_slicer_paper_tiny_plate_required_forms",\n'
            '    "gt6_storage_art_required_forms",\n'
            '    "fission_observation_safety_required_forms"'
        )
        if text.count(marker) != 1:
            raise ValueError("java_overlay_sections marker drifted")
        text = text.replace(marker, replacement, 1)
        json.loads(text)
    if text != original:
        gate_path.write_text(text, encoding="utf-8")
    currentness.write_sidecar(gate_path)


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
    print("storage art is current")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
