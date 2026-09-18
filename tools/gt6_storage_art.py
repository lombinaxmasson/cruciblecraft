#!/usr/bin/env python3
"""GT6 storage art: kind-level colored+overlay, not vanilla oak/iron cubes."""
from __future__ import annotations

import argparse
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
    "storageArtManifestResolvesLocalGt6",
    "storageArtMassStorageFrontCountSync",
    "storageArtMteBarrelNotMissingCube",
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
}
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
    if path.is_file() and path.read_text(encoding="utf-8") == payload:
        return
    path.write_text(payload, encoding="utf-8")


def _sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def _gt6_file(relative: str) -> Path:
    return GT6_W / GT6_TEX / relative.replace("\\", "/")


def _dest_file(destination: str) -> Path:
    rel = destination.replace("\\", "/")
    prefix = "assets/cruciblecraft/"
    if not rel.startswith(prefix):
        raise ValueError(destination)
    return ASSETS / rel[len(prefix) :]


def _tex(folder: str, layer: str, face: str) -> str:
    return f"cruciblecraft:block/gt6_import/storage/{folder}/{layer}_{face}"


def _overlay_cube(folder: str, *, all_sides: bool = False) -> dict[str, Any]:
    textures: dict[str, str] = {}
    for layer, prefix in (("colored", "bot"), ("overlay", "top")):
        for face in FACES:
            if all_sides:
                textures[f"{prefix}_{face}"] = _tex(folder, layer, "sides")
            else:
                textures[f"{prefix}_{face}"] = _tex(folder, layer, face)
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
    manifest = {"rows": _copy_art()}
    _write_json(MANIFEST, manifest)
    _write_json(WAVE / "art_manifest.json", manifest)
    _write_models()
    _rewrite_t44_assets()
    _copy_empty_nbt()
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
            "dual_t44_and_inplace_ids_remain": True,
            "reopens_furniture_required_test_ids": False,
            "obtain": "explicitly_blocked",
        },
    )
    _write_json(
        WAVE / "production_lock.json",
        {"note": "storage kind-level art on live BlockItems; not player_complete"},
    )


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
        "mte_inplace_barrel.json",
        "mte_inplace_mass_storage.json",
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
        elif "tintindex" not in text and "parent" not in text:
            errors.append(f"{name} missing tintindex")
    barrel_state = (
        GENERATED / "blockstates" / "mass_storage_barrel_6999.json"
    )
    if not barrel_state.is_file():
        errors.append("missing generated mass_storage_barrel_6999 blockstate")
    else:
        text = barrel_state.read_text(encoding="utf-8")
        if "storage_mass_barrel" not in text:
            errors.append("T44 barrel blockstate is not storage_mass_barrel")
        for token in VANILLA_FORBIDDEN:
            if token in text:
                errors.append(f"T44 barrel blockstate uses {token}")
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
    catalog = census.load_json(CATALOG)
    seen: set[str] = set()
    for row in catalog["variants"]:
        try:
            seen.add(model_path(str(row.get("model_profile") or ""), str(row["family"])))
        except KeyError as failure:
            errors.append(str(failure))
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
    print("storage art is current")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
