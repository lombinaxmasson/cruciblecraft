#!/usr/bin/env python3
"""Emit PUV2+/OMEGA tech-line catalogs, forms, art, and matrix rows.

Java/tick source: gt6_code/gregtech6 @ 3703e40308c8c030763fd6297dea8b210d2a77b1
Art: gt6_referencable_port_code/gregtech6_w
"""
from __future__ import annotations

import hashlib
import json
import re
import shutil
from collections import defaultdict
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
REV = "3703e40308c8c030763fd6297dea8b210d2a77b1"
DATA = ROOT / "src" / "main" / "resources" / "data" / "cruciblecraft"
ASSETS = ROOT / "src" / "main" / "resources" / "assets" / "cruciblecraft"
GT6_W = ROOT / "gt6_referencable_port_code" / "gregtech6_w"
GT6_TEX = GT6_W / "src" / "main" / "resources" / "assets" / "gregtech" / "textures" / "blocks"
LOADER = ROOT / "gt6_code" / "gregtech6" / "src" / "main" / "java" / "gregtech" / "loaders" / "b" / "Loader_MultiTileEntities.java"

STEAM_PER_EU = 2

SINGLES = [
    # meta, rotor, kinetic_index, input_eu, output_ru, hardness, line
    (1512, "bronze", 1, 24, 16, 4.0, 794),
    (1515, "brass", 1, 36, 24, 4.0, 795),
    (1518, "invar", 1, 48, 32, 4.0, 796),
    (1522, "steel", 2, 96, 64, 4.0, 798),
    (1525, "chromium", 2, 144, 96, 4.0, 799),
    (1527, "ironwood", 2, 192, 128, 4.0, 800),
    (1528, "steeleaf", 2, 192, 128, 4.0, 801),
    (1529, "thaumium", 2, 192, 128, 4.0, 802),
    (1530, "titanium", 3, 384, 256, 4.0, 804),
    (1531, "fiery_steel", 3, 384, 256, 4.0, 805),
    (1535, "aluminium", 3, 576, 384, 4.0, 806),
    (1538, "magnalium", 3, 768, 512, 4.0, 807),
    (1540, "void_metal", 4, 1152, 768, 4.0, 809),
    (1545, "trinitanium", 4, 1536, 1024, 4.0, 810),
    (1548, "graphene", 4, 3072, 2048, 4.0, 811),
]

KINETIC_CASING = {1: "bronze", 2: "steel", 3: "titanium", 4: "tungstensteel"}

LARGES = [
    (17211, "magnalium", "stainless_steel", 18022, "multiblock/dense_stainless_steel_wall", 6144, 4096, 6.0, 1254),
    (17212, "trinitanium", "titanium", 18026, "multiblock/dense_titanium_wall", 12288, 8192, 9.0, 1255),
    (17213, "graphene", "tungstensteel", 18023, "multiblock/dense_tungstensteel_wall", 24576, 16384, 12.5, 1256),
    (17214, "vibramantium", "adamantium", 18025, "multiblock/dense_adamantium_wall", 196608, 131072, 100.0, 1257),
]

VN = ["ulv", "lv", "mv", "hv", "ev", "iv", "luv", "zpm", "uv", "puv1", "puv2", "puv3", "puv4", "puv5", "omega"]
VN_LABEL = ["ULV", "LV", "MV", "HV", "EV", "IV", "LuV", "ZPM", "UV", "PUV1", "PUV2", "PUV3", "PUV4", "PUV5", "OMEGA"]
VOLTAGES = [8, 32, 128, 512, 2048, 8192, 32768, 131072, 524288, 2097152, 8388608, 33554432, 134217728, 536870912, 2147483648]
ELECTRIC_MAT = {
    0: "tin_alloy",
    1: "steel_galvanized",
    2: "aluminium",
    3: "stainless_steel",
    4: "chromium",
    5: "titanium",
    6: "iridium",
    7: "osmium_elemental",
    8: "trinitanium",
    9: "trinaquadalloy",
    10: "neutronium",
    11: "neutronium",
    12: "neutronium",
    13: "neutronium",
    14: "neutronium",
}

CC_ID_BAND = 81000  # PUV2+ machines; never reuse GT6 meta arithmetic


def dump(path: Path, document: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    payload = json.dumps(document, ensure_ascii=False, indent=2) + "\n"
    if path.is_file() and path.read_text(encoding="utf-8") == payload:
        return
    path.write_text(payload, encoding="utf-8")


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def write_steam_catalog() -> None:
    machines: list[dict[str, Any]] = []
    for meta, rotor, kidx, input_eu, output_ru, hardness, line in SINGLES:
        casing = KINETIC_CASING[kidx]
        steam_in = input_eu * STEAM_PER_EU
        machines.append(
            {
                "id": f"cruciblecraft:steam/turbine_{rotor}",
                "source_id": meta,
                "source_line": line,
                "gt6_class": "MultiTileEntityTurbineSteam",
                "kind": "single",
                "source_policy": "source_backed",
                "rotor_material": rotor,
                "casing_material": casing,
                "steam_input_max": steam_in,
                "ru_output": output_ru,
                "steam_per_eu": STEAM_PER_EU,
                "steam_per_water": 200,
                "hardness": hardness,
                "resistance": hardness,
                "lang_en": f"Steam Turbine ({rotor.replace('_', ' ').title()})",
                "lang_zh": f"蒸汽涡轮（{rotor}）",
                "recipe": {
                    "pattern": ["TwT", "GSG", "TMT"],
                    "keys": {
                        "S": {"prefix": "long_rod", "material": casing},
                        "M": {"prefix": "machine_casing_double", "material": casing},
                        "G": {"prefix": "gear", "material": casing},
                        "T": {"prefix": "rotor", "material": rotor},
                    },
                    "catalyst": "w",
                },
            }
        )
    for meta, plate, housing, wall_meta, wall_path, input_eu, output_ru, hardness, line in LARGES:
        steam_in = input_eu * STEAM_PER_EU
        machines.append(
            {
                "id": f"cruciblecraft:{plate}/steam_turbine_main_housing",
                "source_id": meta,
                "source_line": line,
                "gt6_class": "MultiTileEntityLargeTurbineSteam",
                "kind": "large",
                "source_policy": "source_backed",
                "housing_material": housing,
                "plate_material": plate,
                "wall_source_id": wall_meta,
                "wall_id": f"cruciblecraft:{wall_path}",
                "steam_input_max": steam_in,
                "ru_output": output_ru,
                "steam_per_eu": STEAM_PER_EU,
                "steam_per_water": 170,
                "structure": "3x3x4",
                "wall_count": 35,
                "hardness": hardness,
                "resistance": hardness,
                "lang_en": f"{plate.replace('_', ' ').title()} Steam Turbine Main Housing",
                "lang_zh": f"{plate}蒸汽涡轮主机壳",
                "recipe": {
                    "pattern": ["PPP", "PMP", "PPP"],
                    "keys": {
                        "M": {"item": f"cruciblecraft:{wall_path}"},
                        "P": {"prefix": "block", "material": plate},
                    },
                    "catalyst": None,
                },
            }
        )
    dump(
        DATA / "steam_turbines.json",
        {
            "schema_version": 1,
            "source_revision": REV,
            "source_path": "gt6_code/gregtech6/src/main/java/gregtech/loaders/b/Loader_MultiTileEntities.java",
            "steam_per_eu": STEAM_PER_EU,
            "expected_singles": 15,
            "expected_larges": 4,
            "machines": machines,
        },
    )


def write_large_hex_catalog() -> None:
    dump(
        DATA / "large_heat_exchanger.json",
        {
            "schema_version": 1,
            "source_revision": REV,
            "source_path": "Loader_MultiTileEntities.java:1245",
            "machine": {
                "id": "cruciblecraft:large_heat_exchanger",
                "source_id": 17197,
                "source_line": 1245,
                "gt6_class": "MultiTileEntityLargeHeatExchanger",
                "source_policy": "source_backed",
                "hu_rate": 16384,
                "efficiency_bps": 10000,
                "packet_size": 1,
                "wall_source_id": 18024,
                "wall_id": "cruciblecraft:multiblock/dense_tungsten_wall",
                "transmitter_source_id": 18101,
                "transmitter_id": "cruciblecraft:multiblock/heat_transmitter",
                "structure": "3x3x2",
                "lang_en": "Large Heat Exchanger",
                "lang_zh": "大型热交换器",
                "recipe": {
                    "pattern": ["DDD", "PMP", "DDD"],
                    "keys": {
                        "M": {"item": "cruciblecraft:multiblock/dense_tungsten_wall"},
                        "D": {"prefix": "dense_plate", "material": "annealed_copper"},
                        "P": {
                            "prefix": "huge_fluid_pipe",
                            "material": "copper",
                            "family": "ANY.Cu",
                        },
                    },
                    "catalyst": None,
                },
            },
            "transmitter": {
                "id": "cruciblecraft:multiblock/heat_transmitter",
                "source_id": 18101,
                "source_line": 1176,
                "gt6_class": "MultiTileEntityMultiBlockPart",
                "source_policy": "source_backed",
                "lang_en": "Heat Transmitter",
                "lang_zh": "热传递器",
                "recipe": {
                    "pattern": ["MPM", "hRw", "MPM"],
                    "keys": {
                        "M": {"prefix": "plate", "material": "invar"},
                        "P": {"prefix": "triple_plate", "material": "copper", "family": "ANY.Cu"},
                        "R": {"prefix": "long_rod", "material": "copper", "family": "ANY.Cu"},
                    },
                    "catalysts": ["h", "w"],
                },
            },
        },
    )


def patch_mte_catalog() -> None:
    path = DATA / "mte_inplace_catalog.json"
    document = json.loads(path.read_text(encoding="utf-8"))
    identities: list[dict[str, Any]] = document["identities"]
    by_meta = {row["meta"]: row for row in identities}
    added = 0
    for meta, rotor, *_rest in SINGLES:
        if meta in by_meta:
            continue
        title = rotor.replace("_", " ").title()
        row = {
            "chinese_name": f"Steam Turbine ({title})",
            "english_name": f"Steam Turbine ({title})",
            "family": "energy_converter",
            "gt6_class": "MultiTileEntityTurbineSteam / Turbines",
            "kind": "STEAM_TURBINE",
            "meta": meta,
            "registry_path": f"steam/turbine_{rotor}",
            "runtime_id": f"cruciblecraft:steam/turbine_{rotor}",
        }
        identities.append(row)
        by_meta[meta] = row
        added += 1
    if 17211 not in by_meta:
        identities.append(
            {
                "chinese_name": "Magnalium Steam Turbine Main Housing",
                "english_name": "Magnalium Steam Turbine Main Housing",
                "family": "multiblock",
                "gt6_class": "MultiTileEntityLargeTurbineSteam / Multiblock Machines",
                "kind": "STEAM_TURBINE",
                "meta": 17211,
                "registry_path": "magnalium/steam_turbine_main_housing",
                "runtime_id": "cruciblecraft:magnalium/steam_turbine_main_housing",
            }
        )
        added += 1
    if 18101 not in by_meta:
        identities.append(
            {
                "chinese_name": "Heat Transmitter",
                "english_name": "Heat Transmitter",
                "family": "multiblock",
                "gt6_class": "MultiTileEntityMultiBlockPart / Multiblock Machines",
                "kind": "MULTIBLOCK_PART",
                "meta": 18101,
                "registry_path": "multiblock/heat_transmitter",
                "runtime_id": "cruciblecraft:multiblock/heat_transmitter",
            }
        )
        added += 1
    identities.sort(key=lambda row: (row["meta"], row["registry_path"]))
    document["identities"] = identities
    dump(path, document)
    print(f"mte catalog added {added} identities, total {len(identities)}")


def copy_mte_assets_for_new_turbines() -> None:
    template_state = ASSETS / "blockstates" / "steam" / "turbine_bronze.json"
    template_item = ASSETS / "models" / "item" / "steam" / "turbine_bronze.json"
    template_loot = (
        ROOT
        / "src"
        / "main"
        / "resources"
        / "data"
        / "cruciblecraft"
        / "loot_table"
        / "blocks"
        / "steam"
        / "turbine_bronze.json"
    )
    housing_state = ASSETS / "blockstates" / "graphene" / "steam_turbine_main_housing.json"
    for _meta, rotor, *_rest in SINGLES:
        dest_state = ASSETS / "blockstates" / "steam" / f"turbine_{rotor}.json"
        dest_item = ASSETS / "models" / "item" / "steam" / f"turbine_{rotor}.json"
        dest_loot = (
            ROOT
            / "src"
            / "main"
            / "resources"
            / "data"
            / "cruciblecraft"
            / "loot_table"
            / "blocks"
            / "steam"
            / f"turbine_{rotor}.json"
        )
        dest_state.parent.mkdir(parents=True, exist_ok=True)
        dest_item.parent.mkdir(parents=True, exist_ok=True)
        dest_loot.parent.mkdir(parents=True, exist_ok=True)
        if not dest_state.exists():
            dest_state.write_text(template_state.read_text(encoding="utf-8"), encoding="utf-8")
        if not dest_item.exists():
            dest_item.write_text(
                template_item.read_text(encoding="utf-8").replace(
                    "turbine_bronze", f"turbine_{rotor}"
                ),
                encoding="utf-8",
            )
        if not dest_loot.exists():
            dest_loot.write_text(
                template_loot.read_text(encoding="utf-8").replace(
                    "steam/turbine_bronze", f"steam/turbine_{rotor}"
                ),
                encoding="utf-8",
            )
    if housing_state.is_file():
        dest = ASSETS / "blockstates" / "magnalium" / "steam_turbine_main_housing.json"
        dest.parent.mkdir(parents=True, exist_ok=True)
        if not dest.exists():
            dest.write_text(housing_state.read_text(encoding="utf-8"), encoding="utf-8")
        item = ASSETS / "models" / "item" / "magnalium" / "steam_turbine_main_housing.json"
        src_item = ASSETS / "models" / "item" / "graphene" / "steam_turbine_main_housing.json"
        item.parent.mkdir(parents=True, exist_ok=True)
        if src_item.is_file() and not item.exists():
            item.write_text(
                src_item.read_text(encoding="utf-8").replace(
                    "graphene/steam_turbine_main_housing",
                    "magnalium/steam_turbine_main_housing",
                ),
                encoding="utf-8",
            )
        loot_src = (
            ROOT
            / "src"
            / "main"
            / "resources"
            / "data"
            / "cruciblecraft"
            / "loot_table"
            / "blocks"
            / "graphene"
            / "steam_turbine_main_housing.json"
        )
        loot_dest = loot_src.parent.parent / "magnalium" / "steam_turbine_main_housing.json"
        loot_dest.parent.mkdir(parents=True, exist_ok=True)
        if loot_src.is_file() and not loot_dest.exists():
            loot_dest.write_text(
                loot_src.read_text(encoding="utf-8").replace(
                    "graphene/steam_turbine_main_housing",
                    "magnalium/steam_turbine_main_housing",
                ),
                encoding="utf-8",
            )


def copy_art() -> list[dict[str, str]]:
    imports: list[dict[str, str]] = []

    def copy(src_rel: str, dest_rel: str, note: str) -> None:
        src = GT6_W / "src" / "main" / "resources" / src_rel
        dest = ROOT / "src" / "main" / "resources" / dest_rel
        dest.parent.mkdir(parents=True, exist_ok=True)
        if not src.is_file():
            print(f"missing art {src}")
            return
        if not dest.exists() or dest.read_bytes() != src.read_bytes():
            shutil.copy2(src, dest)
        imports.append(
            {
                "source": "gt6_referencable_port_code/gregtech6_w",
                "gt6_source": src_rel,
                "destination": dest_rel.replace("src/main/resources/", ""),
                "note": note,
            }
        )

    sides = ["bottom", "top", "front", "back", "left", "right"]
    hex_base = "assets/gregtech/textures/blocks/machines/multiblockmains"
    # Prefer dedicated folders when present; otherwise reuse hot_fluid / turbine sheets.
    for folder, dest_folder, note in [
        (
            "assets/gregtech/textures/blocks/machines/generators/hot_fluid",
            "assets/cruciblecraft/textures/block/machine/large_heat_exchanger",
            "Large HEX 17197 reuses GeneratorHotFluid sheet until largeheatexchanger folder exists.",
        ),
        (
            "assets/gregtech/textures/blocks/machines/turbines/rotation_steam",
            "assets/cruciblecraft/textures/block/machine/steam_turbine",
            "MultiTileEntityTurbineSteam rotation_steam sheet.",
        ),
        (
            "assets/gregtech/textures/blocks/machines/quantumenergizer",
            "assets/cruciblecraft/textures/block/machine/quantum_energizer",
            "Quantum Energizer T1-T5.",
        ),
    ]:
        src_dir = GT6_W / "src" / "main" / "resources" / folder
        if not src_dir.is_dir():
            print(f"missing texture folder {src_dir}")
            continue
        for png in src_dir.rglob("*.png"):
            rel = png.relative_to(src_dir).as_posix()
            dest_rel = f"src/main/resources/{dest_folder}/{rel}"
            copy(f"{folder}/{rel}", dest_rel.replace("src/main/resources/", ""), note)

    tech = "assets/gregtech/textures/items/gt.multiitem.technological"
    families = [
        (12000, "compact_electric_motor"),
        (12020, "compact_electric_pump"),
        (12040, "compact_electric_conveyor"),
        (12060, "compact_electric_piston"),
        (12080, "compact_electric_robot_arm"),
        (12100, "compact_force_field_emitter"),
        (12120, "compact_signal_emitter"),
        (12140, "compact_sensor"),
    ]
    for base, family in families:
        for tier in range(15):
            native = min(tier, 9)
            src_rel = f"{tech}/{base + native}.png"
            dest = f"assets/cruciblecraft/textures/item/gt6_import/{family}_{VN[tier]}.png"
            note = (
                f"{family} {VN_LABEL[tier]}"
                if tier < 10
                else f"{family} {VN_LABEL[tier]} CC_EXTENSION reuses native PUV1 sheet"
            )
            copy(src_rel, dest, note)
    copy(
        f"{tech}/30307.png" if (GT6_W / "src/main/resources" / tech / "30307.png").is_file()
        else f"{tech}/12140.png",
        "assets/cruciblecraft/textures/item/gt6_import/circuit_quantum.png",
        "Quantum circuit texture; OD_CIRCUITS[7-9] has no single MultiItem png.",
    )

    dump(
        ASSETS / "gt6_puv_omega_art_manifest.json",
        {
            "source": "gt6_referencable_port_code/gregtech6_w",
            "source_revision": REV,
            "source_present": GT6_W.is_dir(),
            "imports": imports,
        },
    )
    return imports


def patch_prefix_mapping() -> None:
    path = ROOT / "tools" / "gt6_prefix_mapping.json"
    document = json.loads(path.read_text(encoding="utf-8"))
    existing = {row["gt_prefix"] for row in document["mappings"]}
    extras = [
        ("wireGt07", "septuple_wire"),
        ("wireGt09", "nonuple_wire"),
        ("wireGt10", "decuple_wire"),
        ("wireGt11", "undecuple_wire"),
        ("wireGt13", "tredecuple_wire"),
        ("wireGt14", "tetradecuple_wire"),
        ("wireGt15", "pentadecuple_wire"),
        ("blockPlate", "block"),
    ]
    for gt_prefix, cc_prefix in extras:
        if gt_prefix in existing:
            continue
        document["mappings"].append(
            {"gt_prefix": gt_prefix, "cc_prefix": cc_prefix, "strategy": "rule"}
        )
    dump(path, document)


def _materials_span(text: str) -> tuple[int, int]:
    start = text.find('\n  "materials": {')
    if start < 0:
        start = text.find('"materials": {')
        if start < 0:
            raise ValueError("materials map missing from gate")
        body_start = text.find("{", start)
    else:
        body_start = text.find("{", start)
    depth = 0
    for index in range(body_start, len(text)):
        char = text[index]
        if char == "{":
            depth += 1
        elif char == "}":
            depth -= 1
            if depth == 0:
                return body_start, index
    raise ValueError("unterminated materials map")


def _insert_gate_forms(text: str, material: str, forms: list[str]) -> str:
    """Append missing live forms without rewriting the rest of the gate file."""
    span_start, span_end = _materials_span(text)
    section = text[span_start : span_end + 1]
    marker = f'    "{material}": ['
    start = section.find(marker)
    if start < 0:
        body = ",\n".join(f'      "{form}"' for form in sorted(forms))
        block = f'\n    "{material}": [\n{body}\n    ],'
        # Insert after opening brace.
        patched = section[0] + block + section[1:]
        return text[:span_start] + patched + text[span_end + 1 :]
    abs_start = span_start + start
    bracket = text.find("[", abs_start)
    end = text.find("\n    ]", bracket)
    chunk = text[bracket : end + 6]
    current = re.findall(r'"([^"]+)"', chunk)
    missing = [form for form in forms if form not in current]
    if not missing:
        return text
    merged = sorted(set(current + missing))
    body = ",\n".join(f'      "{form}"' for form in merged)
    replacement = "[\n" + body + "\n    ]"
    return text[:bracket] + replacement + text[end + 6 :]


def patch_gate_forms() -> None:
    path = DATA / "material_registration_gate.json"
    text = path.read_text(encoding="utf-8")
    additions = {
        "graphene": ["quadruple_wire", "block", "machine_casing", "ingot"],
        "trinitanium": ["curved_plate", "wire", "quadruple_wire"],
        "trinaquadalloy": ["curved_plate", "wire", "quadruple_wire"],
        "vibramantium": ["curved_plate"],
        "neutronium": [
            "ingot",
            "nugget",
            "dust",
            "small_dust",
            "tiny_dust",
            "plate",
            "dense_plate",
            "curved_plate",
            "rod",
            "long_rod",
            "bolt",
            "screw",
            "gear",
            "small_gear",
            "rotor",
            "ring",
            "spring",
            "small_spring",
            "foil",
            "wire",
            "double_wire",
            "quadruple_wire",
            "cable",
            "machine_casing",
            "machine_casing_double",
            "block",
        ],
        "superconductor": [
            "ingot",
            "dust",
            "plate",
            "wire",
            "double_wire",
            "quadruple_wire",
            "cable",
            "rod",
            "block",
        ],
    }
    original = text
    for material, forms in additions.items():
        text = _insert_gate_forms(text, material, forms)
    if text != original:
        path.write_text(text, encoding="utf-8")
        print("patched material_registration_gate.json forms")
    else:
        print("gate forms already present")


def copy_material_json(src_id: str, dest_id: str, color: str, formula: str, en: str, zh: str) -> None:
    src = DATA / "materials" / f"{src_id}.json"
    dest = DATA / "materials" / f"{dest_id}.json"
    document = json.loads(src.read_text(encoding="utf-8"))
    document["id"] = dest_id
    document["tag_name"] = dest_id
    document["color"] = color
    if "gt6_metadata" in document:
        document["gt6_metadata"]["formula"] = formula
        document["gt6_metadata"]["source_material_name"] = en
    dump(dest, document)
    index = json.loads((DATA / "materials" / "index.json").read_text(encoding="utf-8"))
    name = f"{dest_id}.json"
    if name not in index:
        index.append(name)
        index.sort()
        dump(DATA / "materials" / "index.json", index)
    zh_path = DATA / "material_zh_cn.json"
    zh_doc = json.loads(zh_path.read_text(encoding="utf-8"))
    if dest_id not in zh_doc:
        zh_doc[dest_id] = zh
        dump(zh_path, dict(sorted(zh_doc.items())))


def expand_technological_parts() -> None:
    path = DATA / "technological_parts.json"
    document = json.loads(path.read_text(encoding="utf-8"))
    parts: list[dict[str, Any]] = document["parts"]
    paths = {row["registry_path"] for row in parts}
    families = [
        ("compact_electric_motor", "紧凑电动机", "Compact Electric Motor", 12000),
        ("compact_electric_piston", "紧凑电动活塞", "Compact Electric Piston", 12060),
        ("compact_electric_conveyor", "紧凑电动传送带", "Compact Electric Conveyor", 12040),
        ("compact_electric_pump", "紧凑电动泵", "Compact Electric Pump", 12020),
        ("compact_electric_robot_arm", "紧凑电动机械臂", "Compact Electric Robot Arm", 12080),
        ("compact_force_field_emitter", "紧凑力场发生器", "Compact Force Field Emitter", 12100),
        ("compact_signal_emitter", "紧凑信号发射器", "Compact Signal Emitter", 12120),
        ("compact_sensor", "紧凑传感器", "Compact Sensor", 12140),
    ]
    # GT6 compact loop is i<10 (ULV-PUV1). Indices 10-14 are CC_EXTENSION.
    for family, zh, en, base in families:
        for tier in range(15):
            path_id = f"{family}_{VN[tier]}"
            if path_id in paths:
                continue
            source_id = base + tier if tier < 10 else CC_ID_BAND + (base % 1000) + tier
            parts.append(
                {
                    "id": f"cruciblecraft:{path_id}",
                    "registry_path": path_id,
                    "source_id": source_id,
                    "english_name": f"{en} ({VN_LABEL[tier]})",
                    "chinese_name": f"{zh}（{VN_LABEL[tier]}）",
                    "texture": path_id,
                    "source_policy": "source_backed" if tier < 10 else "CC_EXTENSION",
                }
            )
            paths.add(path_id)
    if "circuit_quantum" not in paths:
        parts.append(
            {
                "id": "cruciblecraft:circuit_quantum",
                "registry_path": "circuit_quantum",
                "source_id": CC_ID_BAND + 307,
                "english_name": "Quantum Circuit",
                "chinese_name": "量子电路",
                "texture": "circuit_quantum",
                "source_policy": "CC_EXTENSION",
            }
        )
    document["parts"] = parts
    dump(path, document)
    print("technological parts", len(parts))


def patch_graphene_flags() -> None:
    path = DATA / "materials" / "graphene.json"
    document = json.loads(path.read_text(encoding="utf-8"))
    flags = set(document.get("generation_flags", []))
    flags.update(
        {
            "cruciblecraft:generates_block",
            "cruciblecraft:generates_ingot",
            "cruciblecraft:generates_machine_casing",
        }
    )
    document["generation_flags"] = sorted(flags)
    dump(path, document)


def expand_transformers() -> None:
    path = DATA / "energy_transformer_tiers.json"
    document = json.loads(path.read_text(encoding="utf-8"))
    tiers: list[dict[str, Any]] = document["tiers"]
    existing = {row["id"] for row in tiers}
    # Native GT6 transformers are i<10 (ULV-PUV1 pairs). PUV1-PUV2 through PUV5-OMEGA are CC_EXTENSION.
    for i in range(9, 14):
        low, high = VN[i], VN[i + 1]
        ident = f"cruciblecraft:electric_transformer_{low}_{high}"
        if ident in existing:
            continue
        material = ELECTRIC_MAT[i]
        conductor = "graphene" if i < 11 else "superconductor"
        prefix = "wire"
        tiers.append(
            {
                "id": ident,
                "kind": "cruciblecraft:electric_transformer",
                "low_voltage": low,
                "high_voltage": high,
                "voltage_index": i,
                "input_size": VOLTAGES[i + 1],
                "output_size": VOLTAGES[i],
                "multiplier": 4,
                "capacity": VOLTAGES[i + 1] * 2,
                "material": material,
                "energy": "EU",
                "source_id": CC_ID_BAND + 40 + i,
                "source_line": 0,
                "gt6_class": "CC_EXTENSION",
                "source_policy": "CC_EXTENSION",
                "recipe": {
                    "pattern": ["WIW", "XM ", "WIW"],
                    "keys": {
                        "W": {"prefix": prefix, "material": conductor},
                        "I": {"prefix": "double_plate", "material": "iron"},
                        "X": {"prefix": "quadruple_wire", "material": conductor},
                        "M": {"prefix": "machine_casing", "material": material},
                    },
                },
            }
        )
    document["tiers"] = tiers
    dump(path, document)
    print("transformer tiers", len(tiers))


def expand_machine_matrix() -> None:
    kinds_path = DATA / "machine_kinds.json"
    tiers_path = DATA / "machine_tiers.json"
    kinds_doc = json.loads(kinds_path.read_text(encoding="utf-8"))
    tiers_doc = json.loads(tiers_path.read_text(encoding="utf-8"))
    existing_kinds = {row["id"] for row in kinds_doc["kinds"]}
    extra_kinds = [
        ("printer", "印刷机", "Printer"),
        ("scanner", "扫描仪", "Scanner"),
        ("autocrafter", "自动合成机", "Autocrafter"),
        ("electric_mixer", "电动搅拌机", "Electric Mixer"),
        ("boxinator", "装箱机", "Boxinator"),
        ("lightning", "引雷机", "Lightning"),
        ("plantalyzer", "植物分析仪", "Plantalyzer"),
        ("bumblelyzer", "蜂类分析仪", "Bumblelyzer"),
        ("massfab", "物质制造机", "Matter Fabricator"),
        ("replicator", "复制机", "Replicator"),
        ("freezer", "冷冻机", "Freezer"),
        ("cryo_mixer", "低温搅拌机", "Cryo Mixer"),
        ("polarizer", "磁极化机", "Polarizer"),
        ("magnetic_separator", "磁选机", "Magnetic Separator"),
        ("lu_engraver", "LU雕刻机", "LU Engraver"),
        ("lu_welder", "LU焊机", "LU Welder"),
    ]
    for path_id, zh, en in extra_kinds:
        ident = f"cruciblecraft:{path_id}"
        if ident in existing_kinds:
            continue
        kinds_doc["kinds"].append(
            {
                "id": ident,
                "acquisition_template": "machine_generic",
                "lang_key_zh": zh,
                "lang_key_en": en,
            }
        )
        existing_kinds.add(ident)
    for mat, zh, en in (
        ("neutronium", "中子素", "Neutronium"),
        ("osmiridium", "铱锇合金", "Osmiridium"),
        ("iridium", "铱", "Iridium"),
        ("osmium_elemental", "锇", "Osmium"),
        ("superconductor", "超导体", "Superconductor"),
        ("graphene", "石墨烯", "Graphene"),
        ("trinitanium", "三重钛", "Trinitanium"),
        ("trinaquadalloy", "三重硅岩合金", "Trinaquadalloy"),
        ("vibramantium", "振金钨钢", "Vibramantium"),
        ("magnalium", "镁铝合金", "Magnalium"),
    ):
        if mat not in kinds_doc["material_lang"]:
            kinds_doc["material_lang"][mat] = {
                "lang_key_zh": zh,
                "lang_key_en": en,
            }
    dump(kinds_path, kinds_doc)

    variants: list[dict[str, Any]] = tiers_doc["variants"]
    existing_ids = {row["id"] for row in variants}
    kind_parallel = {}
    kind_overclock = {}
    for row in variants:
        kind_parallel.setdefault(row["kind"], bool(row.get("parallelDuration", False)))
        kind_overclock.setdefault(row["kind"], row.get("overclock", "STANDARD"))
    source_rows: dict[str, str] = tiers_doc.setdefault("source", {}).setdefault(
        "variant_rows", {}
    )
    electric_kinds = [
        "canner",
        "electricloom",
        "electrolyzer",
        "injector",
        "nanofab",
        "slicer",
        "printer",
        "scanner",
        "autocrafter",
        "electric_mixer",
        "boxinator",
        "lightning",
        "plantalyzer",
        "bumblelyzer",
        "replicator",
    ]
    # Native LV-IV gaps: add sourceTier 2-5 for canner (only LV exists) and new families LV-IV.
    native_family_source = {
        "canner": 20161,
        "printer": 20271,
        "scanner": 20281,
        "autocrafter": 20341,
        "electric_mixer": 20351,
        "lightning": 20501,
        "plantalyzer": 20531,
        "bumblelyzer": 20541,
        "boxinator": 20581,
    }
    def add_variant(row: dict[str, Any], source_note: str) -> None:
        nonlocal added
        ident = row["id"]
        if ident in existing_ids:
            return
        kind = row["kind"]
        row["overclock"] = kind_overclock.get(kind, row.get("overclock", "STANDARD"))
        row["parallelDuration"] = kind_parallel.get(
            kind, bool(row.get("parallelDuration", False))
        )
        variants.append(row)
        existing_ids.add(ident)
        kind_overclock.setdefault(kind, row["overclock"])
        kind_parallel.setdefault(kind, row["parallelDuration"])
        source_rows[ident] = source_note
        added += 1

    added = 0
    for family, base in native_family_source.items():
        for tier in range(1, 6):
            material = ELECTRIC_MAT[tier]
            ident = (
                f"cruciblecraft:{family}"
                if tier == 1
                else f"cruciblecraft:{material}_{family}"
            )
            voltage = VOLTAGES[tier]
            add_variant(
                {
                    "id": ident,
                    "kind": f"cruciblecraft:{family}",
                    "tierBand": f"cruciblecraft:{material}_{family}",
                    "material": f"cruciblecraft:{material}",
                    "energy": "ELECTRIC",
                    "sourceId": base + (tier - 1),
                    "sourceTier": tier,
                    "overclock": "STANDARD",
                    "parallelDuration": False,
                    "inputMinimum": voltage // 2,
                    "inputNominal": voltage,
                    "inputMaximum": voltage * 2,
                    "energyCapacity": voltage * 2,
                    "parallel": 1,
                    "efficiency": 10000,
                    "variantSemantics": "material",
                    "resourceProfile": {
                        "sharedModel": "processing_machine",
                        "textureProfile": family,
                        "skipGenericRegistration": False,
                    },
                },
                f"Loader_MultiTileEntities.java:{base + (tier - 1)}",
            )
    for family in electric_kinds:
        for tier in range(10, 15):
            material = ELECTRIC_MAT[tier]
            ident = f"cruciblecraft:{material}_{family}_{VN[tier]}"
            voltage = VOLTAGES[tier]
            add_variant(
                {
                    "id": ident,
                    "kind": f"cruciblecraft:{family}",
                    "tierBand": f"cruciblecraft:{material}_{family}_{VN[tier]}",
                    "material": f"cruciblecraft:{material}",
                    "energy": "ELECTRIC",
                    "sourceId": CC_ID_BAND + electric_kinds.index(family) * 10 + tier,
                    "sourceTier": tier,
                    "overclock": "STANDARD",
                    "parallelDuration": False,
                    "inputMinimum": voltage // 2,
                    "inputNominal": voltage,
                    "inputMaximum": voltage * 2,
                    "energyCapacity": voltage * 2,
                    "parallel": 1,
                    "efficiency": 10000,
                    "variantSemantics": "material",
                    "resourceProfile": {
                        "sharedModel": "processing_machine",
                        "textureProfile": family,
                        "skipGenericRegistration": False,
                    },
                },
                f"CC_EXTENSION PUV2+/OMEGA {family} {VN_LABEL[tier]}",
            )
    for tier in range(1, 6):
        ident = f"cruciblecraft:osmiridium_massfab_t{tier}"
        voltage = VOLTAGES[tier]
        add_variant(
            {
                "id": ident,
                "kind": "cruciblecraft:massfab",
                "tierBand": f"cruciblecraft:osmiridium_massfab_t{tier}",
                "material": "cruciblecraft:osmiridium",
                "energy": "QUANTUM",
                "sourceId": 20410 + tier,
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
                    "textureProfile": "massfab",
                    "skipGenericRegistration": False,
                },
            },
            f"Loader_MultiTileEntities.java:{20410 + tier}",
        )
    add_variant(
        {
            "id": "cruciblecraft:neutronium_massfab_omega",
            "kind": "cruciblecraft:massfab",
            "tierBand": "cruciblecraft:neutronium_massfab_omega",
            "material": "cruciblecraft:neutronium",
            "energy": "QUANTUM",
            "sourceId": CC_ID_BAND + 199,
            "sourceTier": 14,
            "overclock": "CHEAP",
            "parallelDuration": False,
            "inputMinimum": VOLTAGES[14] // 2,
            "inputNominal": VOLTAGES[14],
            "inputMaximum": VOLTAGES[14] * 2,
            "energyCapacity": VOLTAGES[14] * 2,
            "parallel": 1,
            "efficiency": 10000,
            "variantSemantics": "material",
            "resourceProfile": {
                "sharedModel": "processing_machine",
                "textureProfile": "massfab",
                "skipGenericRegistration": False,
            },
        },
        "CC_EXTENSION Matter Fabricator OMEGA",
    )
    for family, base, energy in (
        ("freezer", 20301, "CU"),
        ("cryo_mixer", 20311, "CU"),
        ("polarizer", 20321, "MU"),
        ("magnetic_separator", 20331, "MU"),
    ):
        for tier in range(1, 6):
            material = ELECTRIC_MAT[tier]
            ident = f"cruciblecraft:{material}_{family}"
            voltage = VOLTAGES[tier]
            add_variant(
                {
                    "id": ident,
                    "kind": f"cruciblecraft:{family}",
                    "tierBand": f"cruciblecraft:{material}_{family}",
                    "material": f"cruciblecraft:{material}",
                    "energy": energy,
                    "sourceId": base + tier - 1,
                    "sourceTier": tier,
                    "overclock": "STANDARD",
                    "parallelDuration": False,
                    "inputMinimum": voltage // 2,
                    "inputNominal": voltage,
                    "inputMaximum": voltage * 2,
                    "energyCapacity": voltage * 2,
                    "parallel": 1,
                    "efficiency": 10000,
                    "variantSemantics": "material",
                    "resourceProfile": {
                        "sharedModel": "processing_machine",
                        "textureProfile": family,
                        "skipGenericRegistration": False,
                    },
                },
                f"Loader_MultiTileEntities.java:{base + tier - 1}",
            )
    tiers_doc["variants"] = variants
    dump(tiers_path, tiers_doc)
    print("machine variants", len(variants), "added", added)


def trim_material_flags(material: str, keep: set[str]) -> None:
    path = DATA / "materials" / f"{material}.json"
    document = json.loads(path.read_text(encoding="utf-8"))
    document["generation_flags"] = sorted(keep)
    dump(path, document)


def write_wave_pack() -> None:
    wave = ROOT / "tools" / "waves" / "content" / "puv-omega-tech-line"
    wave.mkdir(parents=True, exist_ok=True)
    dump(
        wave / "wave.json",
        {
            "schema_version": 1,
            "wave_slug": "content/puv-omega-tech-line",
            "program": "content/puv-omega-tech-line",
            "cohort": "puv-omega",
            "unique_active_wave": None,
            "source_revision": REV,
            "generated_by": "tools/emit_puv_omega_tech_line.py",
        },
    )
    dump(
        wave / "readiness.json",
        {
            "schema_version": 1,
            "wave_slug": "content/puv-omega-tech-line",
            "unique_active_wave": None,
            "maturity": "frozen",
            "source_revision": REV,
        },
    )
    dump(
        wave / "production_lock.json",
        {
            "schema_version": 1,
            "source_revision": REV,
            "neutral_matter_bootstrap": {
                "policy": "CC_EXTENSION",
                "slug": "fusion_quantum_massfab",
                "note": "18 GT6 fusion rows stay source-backed and do not emit MatterNeutral. CC adds one labeled extension recipe, then QU Massfab compacts to Neutronium.",
                "input_fluid": "matter_neutral",
                "output_item": "cruciblecraft:neutronium/ingot",
                "qu_per_ingot": 524288,
                "neutral_mb_per_ingot": 144,
            },
            "omega_compact_index": 14,
            "forbidden_vn15_parts": True,
            "plasma_fuel_map": "empty",
            "automaticKindTierCompletion": False,
        },
    )


def main() -> None:
    write_wave_pack()
    write_steam_catalog()
    write_large_hex_catalog()
    patch_mte_catalog()
    copy_mte_assets_for_new_turbines()
    copy_art()
    patch_prefix_mapping()
    patch_gate_forms()
    if not (DATA / "materials" / "neutronium.json").exists():
        copy_material_json("tungstensteel", "neutronium", "#C8C8E6", "Nt", "Neutronium", "中子素")
        trim_material_flags(
            "neutronium",
            {
                "cruciblecraft:generates_ingot",
                "cruciblecraft:generates_nugget",
                "cruciblecraft:generates_dust",
                "cruciblecraft:generates_plate",
                "cruciblecraft:generates_wire",
                "cruciblecraft:generates_double_wire",
                "cruciblecraft:generates_quadruple_wire",
                "cruciblecraft:generates_cable",
                "cruciblecraft:generates_machine_casing",
                "cruciblecraft:generates_block",
                "gt6:itemgenerator/denseplates",
                "gt6:itemgenerator/parts",
                "gt6:itemgenerator/sticks",
            },
        )
    if not (DATA / "materials" / "superconductor.json").exists():
        copy_material_json("graphene", "superconductor", "#7A00FF", "Sc", "Superconductor", "超导体")
        trim_material_flags(
            "superconductor",
            {
                "cruciblecraft:generates_ingot",
                "cruciblecraft:generates_dust",
                "cruciblecraft:generates_plate",
                "cruciblecraft:generates_wire",
                "cruciblecraft:generates_double_wire",
                "cruciblecraft:generates_quadruple_wire",
                "cruciblecraft:generates_cable",
                "cruciblecraft:generates_block",
            },
        )
    expand_technological_parts()
    expand_transformers()
    expand_machine_matrix()
    patch_graphene_flags()
    print("emit_puv_omega_tech_line done")


if __name__ == "__main__":
    main()
