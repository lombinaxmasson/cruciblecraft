#!/usr/bin/env python3
"""Copy GT6 hammer/squeezer/laser art and patch machine catalogs."""
from __future__ import annotations

import json
import shutil
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[4]
ASSETS = ROOT / "src" / "main" / "resources" / "assets" / "cruciblecraft"
DATA = ROOT / "src" / "main" / "resources" / "data" / "cruciblecraft"
GT6_ART = ROOT / "gt6_referencable_port_code" / "gregtech6_w"
GT6_BLOCKS = (
    GT6_ART
    / "src"
    / "main"
    / "resources"
    / "assets"
    / "gregtech"
    / "textures"
    / "blocks"
    / "machines"
)
GT6_GUI = (
    GT6_ART
    / "src"
    / "main"
    / "resources"
    / "assets"
    / "gregtech"
    / "textures"
    / "gui"
    / "machines"
)
FACES = ("front", "back", "left", "right", "top", "bottom")
SOURCE_REVISION = "3703e40308c8c030763fd6297dea8b210d2a77b1"

SQUEEZER_VARIANTS = [
    ("cruciblecraft:squeezer", 1, "bronze", 20071, 4),
    ("cruciblecraft:steel_squeezer", 2, "steel", 20072, 8),
    ("cruciblecraft:titanium_squeezer", 3, "titanium", 20073, 16),
    ("cruciblecraft:tungstensteel_squeezer", 4, "tungstensteel", 20074, 32),
]
LASER_MATERIALS = [
    (1, "steel_galvanized", "laser_engraver", "laser_welder", 20321, 20331),
    (2, "aluminium", "aluminium_laser_engraver", "aluminium_laser_welder", 20322, 20332),
    (3, "stainless_steel", "stainless_steel_laser_engraver", "stainless_steel_laser_welder", 20323, 20333),
    (4, "chromium", "chromium_laser_engraver", "chromium_laser_welder", 20324, 20334),
    (5, "titanium", "titanium_laser_engraver", "titanium_laser_welder", 20325, 20335),
]
KU_WINDOWS = {
    1: (16, 32, 64, 64),
    2: (64, 128, 256, 256),
    3: (256, 512, 1024, 1024),
    4: (1024, 2048, 4096, 4096),
}
LU_WINDOWS = {
    1: (16, 32, 64, 64),
    2: (64, 128, 256, 256),
    3: (256, 512, 1024, 1024),
    4: (1024, 2048, 4096, 4096),
    5: (4096, 8192, 16384, 16384),
}
FOLD_IDS = {
    "processing/automatic_hammer_bronze": "automatic_hammer",
    "processing/automatic_hammer_steel": "steel_automatic_hammer",
    "processing/automatic_hammer_titanium": "titanium_automatic_hammer",
    "processing/automatic_hammer_tungstensteel": "tungstensteel_automatic_hammer",
    "processing/squeezer_bronze": "squeezer",
    "processing/squeezer_steel": "steel_squeezer",
    "processing/squeezer_titanium": "titanium_squeezer",
    "processing/squeezer_tungstensteel": "tungstensteel_squeezer",
    "processing/laser_engraver_t1": "laser_engraver",
    "processing/laser_engraver_t2": "aluminium_laser_engraver",
    "processing/laser_engraver_t3": "stainless_steel_laser_engraver",
    "processing/laser_engraver_t4": "chromium_laser_engraver",
    "processing/laser_engraver_t5": "titanium_laser_engraver",
    "processing/laser_welder_t1": "laser_welder",
    "processing/laser_welder_t2": "aluminium_laser_welder",
    "processing/laser_welder_t3": "stainless_steel_laser_welder",
    "processing/laser_welder_t4": "chromium_laser_welder",
    "processing/laser_welder_t5": "titanium_laser_welder",
}


def _dump(path: Path, document: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(
        json.dumps(document, indent=2, ensure_ascii=False) + "\n",
        encoding="utf-8",
    )


def _copy(src: Path, dest: Path, imports: list[dict[str, str]]) -> None:
    dest.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(src, dest)
    imports.append(
        {
            "source": "gt6_referencable_port_code/gregtech6_w",
            "gt6_source": src.relative_to(GT6_ART).as_posix(),
            "destination": dest.relative_to(ROOT / "src" / "main" / "resources").as_posix(),
        }
    )


def copy_art() -> list[dict[str, str]]:
    imports: list[dict[str, str]] = []
    hammer_src = GT6_BLOCKS / "autotools" / "hammer"
    hammer_dest = ASSETS / "textures" / "block" / "machine" / "automatic_hammer"
    for layer in ("colored", "overlay"):
        side = hammer_src / layer / "side.png"
        for face in FACES:
            src = hammer_src / layer / f"{face}.png"
            if not src.is_file():
                src = side
            if not src.is_file():
                raise FileNotFoundError(src)
            _copy(src, hammer_dest / layer / f"{face}.png", imports)
    for folder, dest_name in (
        ("basicmachines/squeezer", "squeezer"),
        ("basicmachines/laserengraver", "laser_engraver"),
        ("basicmachines/laserwelder", "laser_welder"),
    ):
        src_root = GT6_BLOCKS / folder
        dest_root = ASSETS / "textures" / "block" / "machine" / dest_name
        for layer in ("colored", "overlay"):
            for face in FACES:
                src = src_root / layer / f"{face}.png"
                if not src.is_file():
                    raise FileNotFoundError(src)
                _copy(src, dest_root / layer / f"{face}.png", imports)
    gui_dest = ASSETS / "textures" / "gui" / "machines"
    # GT6 RM.Squeezer / RM.LaserEngraver GUI stems. RM.Welder already has welder.png.
    for name, dest_name in (
        ("Squeezer.png", "squeezer.png"),
        ("LaserEngraver.png", "laserengraver.png"),
    ):
        src = GT6_GUI / name
        if not src.is_file():
            raise FileNotFoundError(src)
        _copy(src, gui_dest / dest_name, imports)
    _dump(
        ASSETS / "gt6_hammer_squeezer_laser_art_manifest.json",
        {"source_revision": SOURCE_REVISION, "imports": imports},
    )
    return imports


def _variant(
    variant_id: str,
    kind: str,
    tier: int,
    material: str,
    energy: str,
    source_id: int,
    parallel: int,
    parallel_duration: bool,
    texture: str,
    skip: bool,
    windows: dict[int, tuple[int, int, int, int]],
    band_prefix: str,
) -> dict[str, Any]:
    lo, nom, hi, cap = windows[tier]
    return {
        "id": variant_id,
        "kind": kind,
        "tierBand": f"cruciblecraft:{band_prefix}_{tier}",
        "material": f"cruciblecraft:{material}",
        "energy": energy,
        "sourceId": source_id,
        "sourceTier": tier,
        "overclock": "STANDARD",
        "parallelDuration": parallel_duration,
        "inputMinimum": lo,
        "inputNominal": nom,
        "inputMaximum": hi,
        "energyCapacity": cap,
        "parallel": parallel,
        "efficiency": 10000,
        "variantSemantics": "material",
        "resourceProfile": {
            "sharedModel": "processing_machine",
            "textureProfile": texture,
            "skipGenericRegistration": skip,
        },
    }


def patch_machine_tiers() -> None:
    path = DATA / "machine_tiers.json"
    document = json.loads(path.read_text(encoding="utf-8"))
    rows = document["source"]["variant_rows"]
    rows["cruciblecraft:squeezer"] = "Loader_MultiTileEntities.java:1324"
    rows["cruciblecraft:steel_squeezer"] = "Loader_MultiTileEntities.java:1325"
    rows["cruciblecraft:titanium_squeezer"] = "Loader_MultiTileEntities.java:1326"
    rows["cruciblecraft:tungstensteel_squeezer"] = "Loader_MultiTileEntities.java:1327"
    rows["cruciblecraft:laser_engraver"] = "Loader_MultiTileEntities.java:1483"
    rows["cruciblecraft:aluminium_laser_engraver"] = "Loader_MultiTileEntities.java:1484"
    rows["cruciblecraft:stainless_steel_laser_engraver"] = "Loader_MultiTileEntities.java:1485"
    rows["cruciblecraft:chromium_laser_engraver"] = "Loader_MultiTileEntities.java:1486"
    rows["cruciblecraft:titanium_laser_engraver"] = "Loader_MultiTileEntities.java:1487"
    rows["cruciblecraft:laser_welder"] = "Loader_MultiTileEntities.java:1490"
    rows["cruciblecraft:aluminium_laser_welder"] = "Loader_MultiTileEntities.java:1491"
    rows["cruciblecraft:stainless_steel_laser_welder"] = "Loader_MultiTileEntities.java:1492"
    rows["cruciblecraft:chromium_laser_welder"] = "Loader_MultiTileEntities.java:1493"
    rows["cruciblecraft:titanium_laser_welder"] = "Loader_MultiTileEntities.java:1494"
    existing = {row["id"] for row in document["variants"]}
    added: list[dict[str, Any]] = []
    for variant_id, tier, material, source_id, parallel in SQUEEZER_VARIANTS:
        added.append(
            _variant(
                variant_id,
                "cruciblecraft:squeezer",
                tier,
                material,
                "KINETIC_PUSH",
                source_id,
                parallel,
                True,
                "squeezer",
                False,
                KU_WINDOWS,
                "ku_tier",
            )
        )
    for tier, material, engraver, welder, engraver_id, welder_id in LASER_MATERIALS:
        added.append(
            _variant(
                f"cruciblecraft:{engraver}",
                "cruciblecraft:laser_engraver",
                tier,
                material,
                "LU",
                engraver_id,
                1,
                False,
                "laser_engraver",
                tier == 1,
                LU_WINDOWS,
                "lu_tier",
            )
        )
        added.append(
            _variant(
                f"cruciblecraft:{welder}",
                "cruciblecraft:laser_welder",
                tier,
                material,
                "LU",
                welder_id,
                1,
                False,
                "laser_welder",
                False,
                LU_WINDOWS,
                "lu_tier",
            )
        )
    for row in added:
        if row["id"] in existing:
            continue
        document["variants"].append(row)
    _dump(path, document)


def _insert_kind(kinds: list[dict[str, Any]], kind: dict[str, Any]) -> None:
    if any(row["id"] == kind["id"] for row in kinds):
        return
    kinds.append(kind)
    kinds.sort(key=lambda row: row["id"])


def patch_kinds_and_delivery() -> None:
    schema = DATA / "schema" / "machine_kinds.schema.json"
    document = json.loads(schema.read_text(encoding="utf-8"))
    enum = document["properties"]["kinds"]["items"]["properties"]["acquisition_template"]["enum"]
    for name in ("squeezer", "laser_engraver", "laser_welder"):
        if name not in enum:
            enum.append(name)
    _dump(schema, document)

    kinds_path = DATA / "machine_kinds.json"
    kinds = json.loads(kinds_path.read_text(encoding="utf-8"))
    _insert_kind(
        kinds["kinds"],
        {
            "id": "cruciblecraft:squeezer",
            "acquisition_template": "squeezer",
            "lang_key_zh": "榨汁机",
            "lang_key_en": "Squeezer",
        },
    )
    _insert_kind(
        kinds["kinds"],
        {
            "id": "cruciblecraft:laser_engraver",
            "acquisition_template": "laser_engraver",
            "lang_key_zh": "激光雕刻机",
            "lang_key_en": "Laser Engraver",
        },
    )
    _insert_kind(
        kinds["kinds"],
        {
            "id": "cruciblecraft:laser_welder",
            "acquisition_template": "laser_welder",
            "lang_key_zh": "激光焊接机",
            "lang_key_en": "Laser Welder",
        },
    )
    _dump(kinds_path, kinds)

    delivery_path = DATA / "machine_delivery.json"
    delivery = json.loads(delivery_path.read_text(encoding="utf-8"))
    hosts = delivery["hosts"]
    by_id = {host["id"]: host for host in hosts}
    by_id["cruciblecraft:laser_engraver"]["kind_catalog"] = True
    if "cruciblecraft:squeezer" not in by_id:
        hosts.append(
            {
                "id": "cruciblecraft:squeezer",
                "recipe_map": "cruciblecraft:squeezer",
                "spec_family": "squeezer",
                "energy": {"type": "KINETIC_PUSH", "mode": "BUFFERED"},
                "slots": {
                    "item_inputs": 1,
                    "item_outputs": 2,
                    "fluid_inputs": 0,
                    "fluid_outputs": 1,
                },
                "gt6_panel": {
                    "in_items": 1,
                    "out_items": 2,
                    "in_fluids": 0,
                    "out_fluids": 1,
                },
                "texture_profile": "squeezer",
                "gt6_source": "Loader_MultiTileEntities.java:1324",
                "art_destination": "textures/block/machine/squeezer",
                "kind_catalog": True,
            }
        )
    if "cruciblecraft:laser_welder" not in by_id:
        hosts.append(
            {
                "id": "cruciblecraft:laser_welder",
                "recipe_map": "cruciblecraft:welder",
                "spec_family": "laser",
                "energy": {"type": "LU", "mode": "BUFFERED"},
                "slots": {
                    "item_inputs": 9,
                    "item_outputs": 1,
                    "fluid_inputs": 1,
                    "fluid_outputs": 0,
                },
                "gt6_panel": {
                    "in_items": 9,
                    "out_items": 1,
                    "in_fluids": 1,
                    "out_fluids": 0,
                },
                "texture_profile": "laser_welder",
                "gt6_source": "Loader_MultiTileEntities.java:1490",
                "art_destination": "textures/block/machine/laser_welder",
                "kind_catalog": True,
            }
        )
    hosts.sort(key=lambda row: row["id"])
    _dump(delivery_path, delivery)


def fold_identities() -> None:
    for path in (
        DATA / "smelter_mte_identity_catalog.json",
        ROOT / "tools" / "smelter_mte_identity_catalog.json",
    ):
        if not path.is_file():
            continue
        document = json.loads(path.read_text(encoding="utf-8"))
        changed = 0
        matched = 0
        for identity in document.get("identities") or []:
            dummy = str(identity.get("registry_path") or "")
            live = FOLD_IDS.get(dummy)
            if live is None and dummy in FOLD_IDS.values():
                live = dummy
            if live is None:
                continue
            matched += 1
            identity["registry_kind"] = "existing_item"
            identity["registry_path"] = live
            identity["runtime_id"] = f"cruciblecraft:{live}"
            changed += 1
        created = [
            row
            for row in document.get("identities") or []
            if row.get("registry_kind") == "item"
        ]
        document["new_item_count"] = len(created)
        _dump(path, document)
        if matched != 18:
            raise ValueError(f"{path} matched {matched} identities, expected 18")


def rewrite_recycling() -> None:
    replacements = {
        f"cruciblecraft:{dummy}": f"cruciblecraft:{live}"
        for dummy, live in FOLD_IDS.items()
    }
    roots = [
        ROOT / "src" / "recipe_generated" / "resources" / "data" / "cruciblecraft" / "recipe",
    ]
    for root in roots:
        if not root.is_dir():
            continue
        for path in root.rglob("*.json"):
            text = path.read_text(encoding="utf-8")
            updated = text
            for dummy, live in replacements.items():
                updated = updated.replace(dummy, live)
            if updated != text:
                path.write_text(updated, encoding="utf-8")


def write_overflow() -> None:
    wave = ROOT / "tools" / "waves" / "machines" / "hammer-squeezer-laser"
    wave.mkdir(parents=True, exist_ok=True)
    _dump(
        wave / "squeezer_overflow.json",
        {
            "source_map": "gt.recipe.squeezer",
            "source_rows": 5322,
            "selected_rows": 0,
            "overflow_rows": 5322,
            "note": "Dump stays overflow. Empty live map is runtime_ready.",
        },
    )


def copy_empty_nbt() -> None:
    src = (
        ROOT
        / "src"
        / "main"
        / "resources"
        / "data"
        / "cruciblecraft_wave_machines"
        / "structure"
        / "empty.nbt"
    )
    if not src.is_file():
        src = (
            ROOT
            / "src"
            / "main"
            / "resources"
            / "data"
            / "cruciblecraft_wave_machines_cluster_mill"
            / "structure"
            / "empty.nbt"
        )
    pack = (
        ROOT
        / "src"
        / "main"
        / "resources"
        / "data"
        / "cruciblecraft_wave_machines_hammer_squeezer_laser"
    )
    for dest in (
        pack / "structure" / "empty.nbt",
        pack / "gametest" / "structure" / "empty.nbt",
    ):
        dest.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(src, dest)


def main() -> None:
    copy_art()
    patch_machine_tiers()
    patch_kinds_and_delivery()
    fold_identities()
    rewrite_recycling()
    copy_empty_nbt()
    print("hammer-squeezer-laser catalogs and art landed")


if __name__ == "__main__":
    main()
