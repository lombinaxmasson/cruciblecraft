#!/usr/bin/env python3
"""Patch catalogs, copy GT6 art, and emit PUV/OMEGA runtime sidecars."""
from __future__ import annotations

import json
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
REV = "3703e40308c8c030763fd6297dea8b210d2a77b1"
DATA = ROOT / "src" / "main" / "resources" / "data" / "cruciblecraft"
ASSETS = ROOT / "src" / "main" / "resources" / "assets" / "cruciblecraft"
SRC = ROOT / "src" / "main" / "resources"
GT6_TEX = (
    ROOT
    / "gt6_referencable_port_code"
    / "gregtech6_w"
    / "src"
    / "main"
    / "resources"
    / "assets"
    / "gregtech"
    / "textures"
)
BASIC = GT6_TEX / "blocks" / "machines" / "basicmachines"
ITEM_TECH = GT6_TEX / "items" / "gt.multiitem.technological"

VOLTAGES = [8, 32, 128, 512, 2048, 8192, 32768, 131072, 524288, 2097152]
SOURCE_FIX = {
    "freezer": 20561,
    "cryo_mixer": 20571,
    "polarizer": 20221,
    "magnetic_separator": 20301,
}

EXTRA_PARTS = [
    {
        "id": "cruciblecraft:laser_gas_empty",
        "registry_path": "laser_gas_empty",
        "source_id": 11000,
        "english_name": "Empty Laser Gas Emitter",
        "chinese_name": "空激光气体发射器",
        "texture": "laser_gas_empty",
        "meta": 11000,
    },
    {
        "id": "cruciblecraft:laser_gas_hene",
        "registry_path": "laser_gas_hene",
        "source_id": 11006,
        "english_name": "Helium-Neon Laser Gas Emitter",
        "chinese_name": "氦氖激光气体发射器",
        "texture": "laser_gas_hene",
        "meta": 11006,
    },
    {
        "id": "cruciblecraft:circuit_crystal_diamond",
        "registry_path": "circuit_crystal_diamond",
        "source_id": 30401,
        "english_name": "Diamond Crystal Circuit",
        "chinese_name": "钻石晶体电路",
        "texture": "circuit_crystal_diamond",
        "meta": 30401,
    },
    {
        "id": "cruciblecraft:circuit_crystal_ruby",
        "registry_path": "circuit_crystal_ruby",
        "source_id": 30402,
        "english_name": "Ruby Crystal Circuit",
        "chinese_name": "红宝石晶体电路",
        "texture": "circuit_crystal_ruby",
        "meta": 30402,
    },
    {
        "id": "cruciblecraft:circuit_crystal_emerald",
        "registry_path": "circuit_crystal_emerald",
        "source_id": 30403,
        "english_name": "Emerald Crystal Circuit",
        "chinese_name": "绿宝石晶体电路",
        "texture": "circuit_crystal_emerald",
        "meta": 30403,
    },
    {
        "id": "cruciblecraft:circuit_crystal_sapphire",
        "registry_path": "circuit_crystal_sapphire",
        "source_id": 30404,
        "english_name": "Sapphire Crystal Circuit",
        "chinese_name": "蓝宝石晶体电路",
        "texture": "circuit_crystal_sapphire",
        "meta": 30404,
    },
    {
        "id": "cruciblecraft:processor_crystal_empty",
        "registry_path": "processor_crystal_empty",
        "source_id": 30500,
        "english_name": "Empty Crystal Processor",
        "chinese_name": "空晶体处理器",
        "texture": "processor_crystal_empty",
        "meta": 30500,
    },
    {
        "id": "cruciblecraft:processor_crystal_diamond",
        "registry_path": "processor_crystal_diamond",
        "source_id": 30501,
        "english_name": "Diamond Crystal Processor",
        "chinese_name": "钻石晶体处理器",
        "texture": "processor_crystal_diamond",
        "meta": 30501,
    },
    {
        "id": "cruciblecraft:processor_crystal_ruby",
        "registry_path": "processor_crystal_ruby",
        "source_id": 30502,
        "english_name": "Ruby Crystal Processor",
        "chinese_name": "红宝石晶体处理器",
        "texture": "processor_crystal_ruby",
        "meta": 30502,
    },
    {
        "id": "cruciblecraft:processor_crystal_emerald",
        "registry_path": "processor_crystal_emerald",
        "source_id": 30503,
        "english_name": "Emerald Crystal Processor",
        "chinese_name": "绿宝石晶体处理器",
        "texture": "processor_crystal_emerald",
        "meta": 30503,
    },
    {
        "id": "cruciblecraft:processor_crystal_sapphire",
        "registry_path": "processor_crystal_sapphire",
        "source_id": 30504,
        "english_name": "Sapphire Crystal Processor",
        "chinese_name": "蓝宝石晶体处理器",
        "texture": "processor_crystal_sapphire",
        "meta": 30504,
    },
]


def dump(path: Path, document: object) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(document, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def copy_file(src: Path, dest: Path) -> bool:
    if not src.is_file():
        return False
    dest.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(src, dest)
    return True


def copy_tree(src: Path, dest: Path) -> int:
    count = 0
    if not src.is_dir():
        return 0
    dest.mkdir(parents=True, exist_ok=True)
    for png in src.rglob("*.png"):
        target = dest / png.relative_to(src)
        if copy_file(png, target):
            count += 1
    return count


def alias_faces(folder: Path, source_name: str, dest_names: list[str]) -> int:
    src = folder / source_name
    if not src.is_file():
        return 0
    count = 0
    for name in dest_names:
        target = folder / name
        if target == src:
            continue
        if copy_file(src, target):
            count += 1
    return count


def copy_cube_layer(src_dir: Path, dest_dir: Path, layer: str) -> int:
    dest = dest_dir / layer
    dest.mkdir(parents=True, exist_ok=True)
    copied = 0
    mapping = {
        "front.png": ["colored_front.png", "front.png", "side.png"],
        "back.png": ["back.png", "side.png"],
        "left.png": ["side.png", "left.png"],
        "right.png": ["side.png", "right.png"],
        "top.png": ["top.png"],
        "bottom.png": ["bottom.png"],
    }
    # Prefer explicit GT6 names, then fall back to a present sibling.
    candidates = {
        "front.png": [
            src_dir / "colored_front.png",
            src_dir / "front.png",
            src_dir / "side.png",
            src_dir / "colored" / "front.png",
            src_dir / "overlay" / "front.png",
        ],
        "back.png": [
            src_dir / "back.png",
            src_dir / "side.png",
            src_dir / "colored_front.png",
            src_dir / "colored" / "side.png",
            src_dir / "overlay" / "back.png",
            src_dir / "overlay" / "front.png",
        ],
        "left.png": [
            src_dir / "left.png",
            src_dir / "side.png",
            src_dir / "colored" / "side.png",
            src_dir / "overlay" / "side.png",
            src_dir / "overlay" / "front.png",
            src_dir / "colored_front.png",
        ],
        "right.png": [
            src_dir / "right.png",
            src_dir / "side.png",
            src_dir / "colored" / "side.png",
            src_dir / "overlay" / "side.png",
            src_dir / "overlay" / "front.png",
            src_dir / "colored_front.png",
        ],
        "top.png": [
            src_dir / "top.png",
            src_dir / "colored" / "top.png",
            src_dir / "overlay" / "top.png",
            src_dir / "side.png",
            src_dir / "colored_front.png",
        ],
        "bottom.png": [
            src_dir / "bottom.png",
            src_dir / "colored" / "bottom.png",
            src_dir / "overlay" / "bottom.png",
            src_dir / "side.png",
            src_dir / "colored_front.png",
        ],
    }
    for dest_name, sources in candidates.items():
        for source in sources:
            if copy_file(source, dest / dest_name):
                copied += 1
                break
    _ = mapping
    return copied


def copy_oriented_faces(src_dir: Path, dest_dir: Path) -> int:
    dest_dir.mkdir(parents=True, exist_ok=True)
    copied = 0
    side = src_dir / "side.png"
    front = src_dir / "front.png"
    back = src_dir / "back.png"
    mapping = {
        "front.png": [front, src_dir / "colored_front.png", side],
        "back.png": [back, side, front],
        "left.png": [src_dir / "left.png", side, front],
        "right.png": [src_dir / "right.png", side, front],
        "top.png": [src_dir / "top.png", side, front],
        "bottom.png": [src_dir / "bottom.png", side, front],
    }
    for dest_name, sources in mapping.items():
        for source in sources:
            if source is not None and copy_file(source, dest_dir / dest_name):
                copied += 1
                break
    return copied


def copy_hex_layer(src_root: Path, dest_root: Path, layer: str, src_layer: str, src_front: str) -> int:
    dest = dest_root / layer
    dest.mkdir(parents=True, exist_ok=True)
    copied = 0
    body = src_root / src_layer
    front = src_root / src_front
    mapping = {
        "front.png": [front / "side.png", front / "front.png", body / "side.png"],
        "back.png": [body / "side.png", front / "side.png"],
        "left.png": [body / "side.png", front / "side.png"],
        "right.png": [body / "side.png", front / "side.png"],
        "top.png": [body / "top.png", front / "top.png", body / "side.png"],
        "bottom.png": [body / "bottom.png", front / "bottom.png", body / "side.png"],
    }
    for dest_name, sources in mapping.items():
        for source in sources:
            if copy_file(source, dest / dest_name):
                copied += 1
                break
    return copied


def copy_machine_art() -> None:
    mapping = {
        "scanner": "scannervisuals",
        "autocrafter": "autocrafter",
        "electric_mixer": "electricmixer",
        "boxinator": "boxinator",
        "lightning": "lightning",
        "plantalyzer": "plantalyzer",
        "bumblelyzer": "bumblelyzer",
        "massfab": "massfab",
        "replicator": "replicator",
        "freezer": "freezer",
        "cryo_mixer": "cryomixer",
        "polarizer": "polarizer",
        "magnetic_separator": "magneticseparator",
        "printer": "printer",
    }
    copied = 0
    dest_root = ASSETS / "textures" / "block" / "machine"
    for dest_name, src_name in mapping.items():
        copied += copy_tree(BASIC / src_name, dest_root / dest_name)
    qe_src = GT6_TEX / "blocks" / "machines" / "quantumenergizer" / "quantum_laser"
    qe_dest = dest_root / "quantum_energizer"
    for layer in ("colored", "overlay", "overlay_active"):
        copied += copy_oriented_faces(qe_src / layer, qe_dest / layer)
    machines = GT6_TEX / "blocks" / "machines"
    hex_src = machines / "multiblockmains" / "largeheatexchanger"
    hex_dest = dest_root / "large_heat_exchanger"
    copied += copy_hex_layer(hex_src, hex_dest, "colored", "colored", "colored_front")
    copied += copy_hex_layer(hex_src, hex_dest, "overlay", "overlay", "overlay_front")
    copied += copy_hex_layer(hex_src, hex_dest, "overlay_active", "overlay", "overlay_front")
    steam_src = machines / "turbines" / "rotation_steam"
    steam_dest = dest_root / "steam_turbine"
    copied += copy_oriented_faces(steam_src / "colored", steam_dest / "colored")
    copied += copy_oriented_faces(steam_src / "overlay", steam_dest / "overlay")
    copied += copy_oriented_faces(steam_src / "overlay_active_lf", steam_dest / "overlay_active")
    large_src = machines / "multiblockmains" / "largeturbine"
    large_dest = dest_root / "large_steam_turbine"
    copied += copy_hex_layer(large_src, large_dest, "colored", "colored", "colored_front")
    copied += copy_hex_layer(large_src, large_dest, "overlay", "overlay", "overlay_front")
    copied += copy_hex_layer(large_src, large_dest, "overlay_active", "overlay", "overlay_front")
    print("copied machine pngs", copied)


def copy_part_art() -> None:
    dest = ASSETS / "textures" / "item" / "gt6_import"
    copied = 0
    for part in EXTRA_PARTS:
        src = ITEM_TECH / f"{part['meta']}.png"
        if copy_file(src, dest / f"{part['texture']}.png"):
            copied += 1
    print("copied part pngs", copied)


def append_extra_parts() -> None:
    path = DATA / "technological_parts.json"
    document = json.loads(path.read_text(encoding="utf-8"))
    existing = {row["id"] for row in document["parts"]}
    for part in EXTRA_PARTS:
        if part["id"] in existing:
            continue
        document["parts"].append(
            {
                "id": part["id"],
                "registry_path": part["registry_path"],
                "source_id": part["source_id"],
                "english_name": part["english_name"],
                "chinese_name": part["chinese_name"],
                "texture": part["texture"],
            }
        )
        existing.add(part["id"])
    dump(path, document)
    print("technological parts", len(document["parts"]))


def patch_kinds() -> None:
    path = DATA / "machine_kinds.json"
    document = json.loads(path.read_text(encoding="utf-8"))
    document["kinds"] = [
        row
        for row in document["kinds"]
        if row["id"]
        not in ("cruciblecraft:lu_engraver", "cruciblecraft:lu_welder")
    ]
    dump(path, document)


def patch_tiers() -> None:
    path = DATA / "machine_tiers.json"
    document = json.loads(path.read_text(encoding="utf-8"))
    rows = document["variants"]
    notes = document.setdefault("source", {}).setdefault("variant_rows", {})
    existing = {row["id"] for row in rows}
    for row in rows:
        kind = row["kind"].split(":")[-1]
        if kind in SOURCE_FIX and 1 <= int(row.get("sourceTier", 0)) <= 5:
            row["sourceId"] = SOURCE_FIX[kind] + int(row["sourceTier"]) - 1
            notes[row["id"]] = f"Loader_MultiTileEntities.java:{row['sourceId']}"
        if kind == "replicator":
            row["energy"] = "QUANTUM"
            if row.get("sourceTier", 0) >= 10:
                notes[row["id"]] = f"CC_EXTENSION PUV2+/OMEGA replicator {row.get('sourceTier')}"
        if kind == "massfab":
            row["energy"] = "QUANTUM"
    for tier in range(1, 6):
        ident = f"cruciblecraft:osmiridium_replicator_t{tier}"
        if ident in existing:
            continue
        voltage = VOLTAGES[tier]
        row = {
            "id": ident,
            "kind": "cruciblecraft:replicator",
            "tierBand": ident,
            "material": "cruciblecraft:osmiridium",
            "energy": "QUANTUM",
            "sourceId": 20430 + tier,
            "sourceTier": tier,
            "overclock": "CHEAP",
            "parallelDuration": False,
            "inputMinimum": voltage // 2,
            "inputNominal": voltage,
            "inputMaximum": voltage * 2,
            "energyCapacity": voltage * 2,
            "parallel": 1,
            "efficiency": 5000 + (tier - 1) * 1250,
            "variantSemantics": "material",
            "resourceProfile": {
                "sharedModel": "processing_machine",
                "textureProfile": "replicator",
                "skipGenericRegistration": False,
            },
        }
        rows.append(row)
        notes[ident] = f"Loader_MultiTileEntities.java:{20430 + tier}"
        existing.add(ident)
    dump(path, document)
    print("machine variants", len(rows))


def patch_delivery() -> None:
    schema = json.loads(
        (DATA / "schema" / "machine_delivery.schema.json").read_text(encoding="utf-8")
    )
    energy_enum = schema["$defs"]["host"]["properties"]["energy"]["properties"]["type"]["enum"]
    for extra in ("QUANTUM", "CU", "MU"):
        if extra not in energy_enum:
            energy_enum.append(extra)
    dump(DATA / "schema" / "machine_delivery.schema.json", schema)

    path = DATA / "machine_delivery.json"
    document = json.loads(path.read_text(encoding="utf-8"))
    existing = {host["id"] for host in document["hosts"]}
    hosts = [
        ("printer", "printer", "ELECTRIC", 2, 1, 1, 0, "Loader_MultiTileEntities.java:1450"),
        ("scanner", "scanner", "ELECTRIC", 1, 1, 0, 0, "Loader_MultiTileEntities.java:1457"),
        ("autocrafter", "autocrafter", "ELECTRIC", 1, 1, 0, 0, "Loader_MultiTileEntities.java:1497"),
        ("electric_mixer", "mixer", "ELECTRIC", 6, 1, 6, 2, "Loader_MultiTileEntities.java:1504"),
        ("boxinator", "boxinator", "ELECTRIC", 1, 1, 0, 0, "Loader_MultiTileEntities.java:1635"),
        ("lightning", "lightning", "ELECTRIC", 2, 1, 1, 1, "Loader_MultiTileEntities.java:1582"),
        ("plantalyzer", "plantalyzer", "ELECTRIC", 1, 1, 1, 0, "Loader_MultiTileEntities.java:1601"),
        ("bumblelyzer", "bumblelyzer", "ELECTRIC", 1, 1, 1, 0, "Loader_MultiTileEntities.java:1608"),
        ("massfab", "massfab", "QUANTUM", 1, 1, 1, 1, "Loader_MultiTileEntities.java:1542"),
        ("replicator", "replicator", "QUANTUM", 1, 1, 1, 1, "Loader_MultiTileEntities.java:1556"),
        ("freezer", "freezer", "CU", 1, 1, 1, 1, "Loader_MultiTileEntities.java:1621"),
        ("cryo_mixer", "cryo_mixer", "CU", 6, 1, 6, 2, "Loader_MultiTileEntities.java:1628"),
        ("polarizer", "polarizer", "MU", 1, 1, 0, 0, "Loader_MultiTileEntities.java:1418"),
        ("magnetic_separator", "magnetic_separator", "MU", 1, 6, 1, 1, "Loader_MultiTileEntities.java:1470"),
    ]
    for path_id, recipe_map, energy, inn, out, fin, fout, source in hosts:
        ident = f"cruciblecraft:{path_id}"
        if ident in existing:
            continue
        document["hosts"].append(
            {
                "id": ident,
                "recipe_map": f"cruciblecraft:{recipe_map}",
                "spec_family": "chemical" if fin or fout else "component",
                "energy": {"type": energy, "mode": "BUFFERED"},
                "slots": {
                    "item_inputs": inn,
                    "item_outputs": out,
                    "fluid_inputs": fin,
                    "fluid_outputs": fout,
                },
                "gt6_panel": {
                    "in_items": inn,
                    "out_items": out,
                    "in_fluids": fin,
                    "out_fluids": fout,
                },
                "texture_profile": path_id,
                "gt6_source": source,
                "art_destination": f"textures/block/machine/{path_id}",
                "kind_catalog": True,
            }
        )
        existing.add(ident)
    dump(path, document)
    print("delivery hosts", len(document["hosts"]))


def emit_quantum_energizers() -> None:
    machines = []
    for tier in range(1, 6):
        voltage = VOLTAGES[tier]
        machines.append(
            {
                "id": f"cruciblecraft:quantum_energizer_t{tier}",
                "source_id": 10120 + tier,
                "source_line": 961 + tier,
                "source_policy": "source_backed",
                "gt6_class": "MultiTileEntityQuantumEnergizerLaser",
                "material": "osmiridium",
                "lu_input": voltage,
                "qu_output": voltage // 2,
                "hardness": 16.0,
                "resistance": 16.0,
                "lang_en": f"Quantum Energizer (T{tier})",
                "lang_zh": f"量子充能器（T{tier}）",
                "recipe": {
                    "pattern": ["CFC", "SME", "CFC"],
                    "keys": {
                        "M": {"prefix": "machine_casing", "material": "osmiridium"},
                        "F": {"item": f"cruciblecraft:compact_force_field_emitter_{['lv','mv','hv','ev','iv'][tier-1]}"},
                        "S": {"item": f"cruciblecraft:compact_sensor_{['lv','mv','hv','ev','iv'][tier-1]}"},
                        "E": {"item": f"cruciblecraft:compact_signal_emitter_{['lv','mv','hv','ev','iv'][tier-1]}"},
                        "C": {"item": "cruciblecraft:processor_crystal_sapphire"},
                    },
                },
            }
        )
    machines.append(
        {
            "id": "cruciblecraft:quantum_energizer_omega",
            "source_id": 81199,
            "source_line": 0,
            "source_policy": "CC_EXTENSION",
            "gt6_class": "MultiTileEntityQuantumEnergizerLaser",
            "material": "neutronium",
            "lu_input": 2147483648,
            "qu_output": 1073741824,
            "hardness": 16.0,
            "resistance": 16.0,
            "lang_en": "Quantum Energizer (OMEGA)",
            "lang_zh": "量子充能器（OMEGA）",
            "recipe": {
                "pattern": ["CFC", "SME", "CFC"],
                "keys": {
                    "M": {"prefix": "machine_casing", "material": "neutronium"},
                    "F": {"item": "cruciblecraft:compact_force_field_emitter_omega"},
                    "S": {"item": "cruciblecraft:compact_sensor_omega"},
                    "E": {"item": "cruciblecraft:compact_signal_emitter_omega"},
                    "C": {"item": "cruciblecraft:processor_crystal_sapphire"},
                },
            },
        }
    )
    dump(
        DATA / "quantum_energizers.json",
        {
            "schema_version": 1,
            "source_revision": REV,
            "expected_size": len(machines),
            "machines": machines,
        },
    )


def emit_neutral_matter() -> None:
    deuterium = json.loads((DATA / "materials" / "deuterium.json").read_text(encoding="utf-8"))
    material = json.loads(json.dumps(deuterium))
    material["id"] = "matter_neutral"
    material["tag_name"] = "matter_neutral"
    material["metadata_only"] = True
    material["molten_fluid"] = False
    material["no_decompose"] = True
    material["color"] = "#E6E6FF"
    material["name"] = {"en_us": "Neutral Matter", "zh_cn": "中性物质"}
    material["generation_flags"] = []
    material["gt6_metadata"]["aliases"] = []
    material["gt6_metadata"]["formula"] = "NM"
    material["gt6_metadata"]["generation_tags"] = ["ITEMGENERATOR.GASES"]
    material["gt6_metadata"]["material_tags"] = ["CC_EXTENSION", "ATOMIC.ELEMENT"]
    material["gt6_metadata"]["source_id"] = 0
    material["gt6_metadata"]["source_name"] = "NeutralMatter"
    material["gt6_metadata"]["source_material_id"] = 0
    material["gt6_metadata"]["source_material_name"] = "NeutralMatter"
    for target in material["gt6_metadata"].get("processing_targets", {}).values():
        target["material"] = "matter_neutral"
        target["source_material_id"] = 0
        target["source_material_name"] = "NeutralMatter"
    dump(DATA / "materials" / "matter_neutral.json", material)
    index_path = DATA / "materials" / "index.json"
    index = json.loads(index_path.read_text(encoding="utf-8"))
    if "matter_neutral.json" not in index:
        index.append("matter_neutral.json")
        index.sort()
        dump(index_path, index)
    dump(
        DATA / "puv_omega_fluid_gate.json",
        {
            "fluids": [
                {
                    "color": "#E6E6FF",
                    "density": -1,
                    "id": "matter_neutral",
                    "material": "matter_neutral",
                    "source": {
                        "path": "src/main/resources/data/cruciblecraft/materials/matter_neutral.json",
                        "reason": "CC_EXTENSION fusion bootstrap fluid; GT6 FL.neutralmatter",
                        "repository": "GregTech6/gregtech6",
                        "revision": REV,
                    },
                    "state": "gas",
                    "temperature_kelvin": 300,
                    "viscosity": 200,
                    "world_placeable": False,
                }
            ],
            "schema_version": 1,
        },
    )
    insert_matter_neutral_gate()


def insert_matter_neutral_gate() -> None:
    path = DATA / "material_registration_gate.json"
    text = path.read_text(encoding="utf-8")
    if '"matter_neutral"' in text:
        return
    needle = '  "materials": {\n    "superconductor":'
    insert = '  "materials": {\n    "matter_neutral": [],\n    "superconductor":'
    if needle not in text:
        raise SystemExit("material_registration_gate.json materials needle missing")
    path.write_text(text.replace(needle, insert, 1), encoding="utf-8")


def strip_material_aliases() -> None:
    neutronium = json.loads((DATA / "materials" / "neutronium.json").read_text(encoding="utf-8"))
    neutronium.pop("composition", None)
    neutronium.setdefault("gt6_metadata", {})["aliases"] = []
    dump(DATA / "materials" / "neutronium.json", neutronium)
    superconductor = json.loads(
        (DATA / "materials" / "superconductor.json").read_text(encoding="utf-8")
    )
    superconductor.pop("composition", None)
    superconductor.setdefault("gt6_metadata", {})["aliases"] = []
    dump(DATA / "materials" / "superconductor.json", superconductor)
    graphene = json.loads((DATA / "materials" / "graphene.json").read_text(encoding="utf-8"))
    graphene.setdefault("gt6_metadata", {})["aliases"] = []
    dump(DATA / "materials" / "graphene.json", graphene)


def copy_empty_nbt() -> None:
    src = (
        SRC
        / "data"
        / "cruciblecraft_wave_runtime_heat_exchangers"
        / "structure"
        / "empty.nbt"
    )
    if not src.is_file():
        src = (
            SRC
            / "data"
            / "cruciblecraft_fusion_plasma"
            / "structure"
            / "empty.nbt"
        )
    namespaces = [
        "cruciblecraft_wave_energy_large_heat_exchanger",
        "cruciblecraft_wave_energy_steam_turbine",
        "cruciblecraft_wave_energy_quantum_massfab",
        "cruciblecraft_wave_energy_puv_omega_parts",
        "cruciblecraft_wave_machines_puv_omega_matrix",
    ]
    for ns in namespaces:
        pack = SRC / "data" / ns
        for rel in ("structure/empty.nbt", "gametest/structure/empty.nbt"):
            dest = pack / rel
            dest.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(src, dest)


def write_art_manifest() -> None:
    dump(
        ASSETS / "gt6_puv_omega_runtime_art_manifest.json",
        {
            "schema_version": 1,
            "source_revision": REV,
            "copies": [
                {
                    "source": "gt6_referencable_port_code/gregtech6_w",
                    "gt6_source": "textures/blocks/multiblockmains/largeheatexchanger",
                    "destination": "assets/cruciblecraft/textures/block/machine/large_heat_exchanger",
                },
                {
                    "source": "gt6_referencable_port_code/gregtech6_w",
                    "gt6_source": "textures/blocks/machines/quantumenergizer",
                    "destination": "assets/cruciblecraft/textures/block/machine/quantum_energizer",
                },
            ],
        },
    )


def main() -> None:
    copy_machine_art()
    copy_part_art()
    append_extra_parts()
    patch_kinds()
    patch_tiers()
    patch_delivery()
    emit_quantum_energizers()
    emit_neutral_matter()
    strip_material_aliases()
    copy_empty_nbt()
    write_art_manifest()
    print("puv omega runtime patch complete")


if __name__ == "__main__":
    main()
