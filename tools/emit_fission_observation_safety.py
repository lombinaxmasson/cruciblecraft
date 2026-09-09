#!/usr/bin/env python3
"""Emit fission observation/safety wave artifacts, catalog rows, recipes, art."""
from __future__ import annotations

import json
import shutil
import struct
import zlib
from pathlib import Path

from tools import io_common as io

ROOT = io.ROOT
SRC = ROOT / "src" / "main" / "resources"
WAVE = ROOT / "tools" / "waves" / "runtime" / "fission-observation-safety"
CAP = ROOT / "tools" / "capabilities" / "energy" / "nuclear-fission-observation-safety"
CATALOG = SRC / "data" / "cruciblecraft" / "semantic_object_catalog.json"
REVISION = "3703e40308c8c030763fd6297dea8b210d2a77b1"
NS = "cruciblecraft_wave_runtime_fission_observation_safety"
GT6_ART = (
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
SLOT = {"head": 0, "chest": 1, "legs": 2, "boots": 3}
HANDHELD = (
    ("thermometer_quicksilver", 10000, "gt_multiitem/multiitem_randomtools_m10000"),
    ("geiger_empty", 10001, "gt_multiitem/multiitem_randomtools_m10001"),
    ("geiger_filled", 10002, "gt_multiitem/multiitem_randomtools_m10002"),
)

HAZMAT = [
    {
        "suit": "radiation",
        "piece": "head",
        "english": "Radiation Hazard Suit Helmet",
        "chinese": "辐射防护服头罩",
        "source": "gregtech:gt.armor.hazmat.radiation.head",
        "texture": "cruciblecraft:item/gt6_import/hazmat_radiation_head",
    },
    {
        "suit": "radiation",
        "piece": "chest",
        "english": "Radiation Hazard Suit Shirt",
        "chinese": "辐射防护服上衣",
        "source": "gregtech:gt.armor.hazmat.radiation.chest",
        "texture": "cruciblecraft:item/gt6_import/hazmat_radiation_chest",
    },
    {
        "suit": "radiation",
        "piece": "legs",
        "english": "Radiation Hazard Suit Pants",
        "chinese": "辐射防护服裤子",
        "source": "gregtech:gt.armor.hazmat.radiation.legs",
        "texture": "cruciblecraft:item/gt6_import/hazmat_radiation_legs",
    },
    {
        "suit": "radiation",
        "piece": "boots",
        "english": "Radiation Hazard Suit Boots",
        "chinese": "辐射防护服靴子",
        "source": "gregtech:gt.armor.hazmat.radiation.boots",
        "texture": "cruciblecraft:item/gt6_import/hazmat_radiation_boots",
    },
    {
        "suit": "heat",
        "piece": "head",
        "english": "Heat Protection Suit Helmet",
        "chinese": "隔热防护服头罩",
        "source": "gregtech:gt.armor.hazmat.heat.head",
        "texture": "cruciblecraft:item/gt6_import/hazmat_heat_head",
    },
    {
        "suit": "heat",
        "piece": "chest",
        "english": "Heat Protection Suit Shirt",
        "chinese": "隔热防护服上衣",
        "source": "gregtech:gt.armor.hazmat.heat.chest",
        "texture": "cruciblecraft:item/gt6_import/hazmat_heat_chest",
    },
    {
        "suit": "heat",
        "piece": "legs",
        "english": "Heat Protection Suit Pants",
        "chinese": "隔热防护服裤子",
        "source": "gregtech:gt.armor.hazmat.heat.legs",
        "texture": "cruciblecraft:item/gt6_import/hazmat_heat_legs",
    },
    {
        "suit": "heat",
        "piece": "boots",
        "english": "Heat Protection Suit Boots",
        "chinese": "隔热防护服靴子",
        "source": "gregtech:gt.armor.hazmat.heat.boots",
        "texture": "cruciblecraft:item/gt6_import/hazmat_heat_boots",
    },
]


def dump(path: Path, document: object) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(
        json.dumps(document, indent=2, ensure_ascii=False) + "\n",
        encoding="utf-8",
    )


def _paeth(a: int, b: int, c: int) -> int:
    p = a + b - c
    pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
    if pa <= pb and pa <= pc:
        return a
    if pb <= pc:
        return b
    return c


def read_png_rgba(path: Path) -> list[list[tuple[int, int, int, int]]]:
    data = path.read_bytes()
    if data[:8] != b"\x89PNG\r\n\x1a\n":
        raise SystemExit(f"not a PNG: {path}")
    pos = 8
    width = height = bit_depth = color_type = 0
    palette: list[tuple[int, int, int]] = []
    trans: bytes | None = None
    idat = bytearray()
    while pos < len(data):
        length = struct.unpack(">I", data[pos : pos + 4])[0]
        tag = data[pos + 4 : pos + 8]
        chunk = data[pos + 8 : pos + 8 + length]
        pos += 12 + length
        if tag == b"IHDR":
            width, height, bit_depth, color_type, comp, filt, interlace = struct.unpack(
                ">IIBBBBB", chunk
            )
            if comp or filt or interlace or bit_depth != 8:
                raise SystemExit(f"unsupported PNG {path}")
        elif tag == b"PLTE":
            palette = [
                (chunk[i], chunk[i + 1], chunk[i + 2]) for i in range(0, len(chunk), 3)
            ]
        elif tag == b"tRNS":
            trans = chunk
        elif tag == b"IDAT":
            idat.extend(chunk)
        elif tag == b"IEND":
            break
    bpp = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}.get(color_type)
    if bpp is None:
        raise SystemExit(f"color type {color_type} in {path}")
    raw = zlib.decompress(bytes(idat))
    stride = width * bpp
    prev = bytearray(stride)
    offset = 0
    rows: list[list[tuple[int, int, int, int]]] = []
    for _ in range(height):
        filter_type = raw[offset]
        scan = bytearray(raw[offset + 1 : offset + 1 + stride])
        offset += 1 + stride
        if filter_type == 1:
            for i in range(stride):
                scan[i] = (scan[i] + (scan[i - bpp] if i >= bpp else 0)) & 255
        elif filter_type == 2:
            for i in range(stride):
                scan[i] = (scan[i] + prev[i]) & 255
        elif filter_type == 3:
            for i in range(stride):
                left = scan[i - bpp] if i >= bpp else 0
                scan[i] = (scan[i] + ((left + prev[i]) // 2)) & 255
        elif filter_type == 4:
            for i in range(stride):
                left = scan[i - bpp] if i >= bpp else 0
                up_left = prev[i - bpp] if i >= bpp else 0
                scan[i] = (scan[i] + _paeth(left, prev[i], up_left)) & 255
        elif filter_type:
            raise SystemExit(f"filter {filter_type} in {path}")
        prev = bytearray(scan)
        row: list[tuple[int, int, int, int]] = []
        for x in range(width):
            i = x * bpp
            if color_type == 6:
                row.append((scan[i], scan[i + 1], scan[i + 2], scan[i + 3]))
            elif color_type == 2:
                row.append((scan[i], scan[i + 1], scan[i + 2], 255))
            elif color_type == 3:
                red, green, blue = palette[scan[i]]
                alpha = trans[scan[i]] if trans is not None and scan[i] < len(trans) else 255
                row.append((red, green, blue, alpha))
            elif color_type == 0:
                row.append((scan[i], scan[i], scan[i], 255))
            else:
                row.append((scan[i], scan[i], scan[i], scan[i + 1]))
        rows.append(row)
    return rows


def write_png(path: Path, pixels: list[list[tuple[int, int, int, int]]]) -> None:
    height = len(pixels)
    width = len(pixels[0])
    raw = b"".join(
        b"\x00" + b"".join(bytes(pixel) for pixel in row) for row in pixels
    )

    def chunk(tag: bytes, data: bytes) -> bytes:
        return (
            struct.pack(">I", len(data))
            + tag
            + data
            + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)
        )

    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(
        b"\x89PNG\r\n\x1a\n"
        + chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
        + chunk(b"IDAT", zlib.compress(raw, 9))
        + chunk(b"IEND", b"")
    )


def require_gt6_art() -> None:
    if not GT6_ART.is_dir():
        raise SystemExit(f"missing gregtech6_w textures at {GT6_ART}")


def record_copy(imports: list[dict], gt6_source: str, destination: str, note: str | None = None) -> None:
    row = {
        "source": "gt6_referencable_port_code/gregtech6_w",
        "gt6_source": gt6_source,
        "destination": destination,
    }
    if note:
        row["note"] = note
    imports.append(row)


def copy_png(src: Path, dest: Path) -> None:
    if not src.is_file():
        raise SystemExit(f"missing gregtech6_w texture: {src}")
    dest.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(src, dest)


def overlay(
    base: list[list[tuple[int, int, int, int]]],
    top: list[list[tuple[int, int, int, int]]],
) -> list[list[tuple[int, int, int, int]]]:
    out = [list(row) for row in base]
    for y, row in enumerate(top):
        for x, pixel in enumerate(row):
            if pixel[3]:
                out[y][x] = pixel
    return out


def catalog_row(english: str, chinese: str, source: str, path: str, texture: str, meta: int, kind: str, behavior: str) -> dict:
    return {
        "acquisition_authority": "runtime/fission-observation-safety",
        "behavior": behavior,
        "chinese_name": chinese,
        "display_requirements": {
            "distinguishable": True,
            "holdable": True,
            "model": "item/generated",
        },
        "english_name": english,
        "input_count": 1,
        "kind": kind,
        "mapping_class": "exact_item",
        "meta": meta,
        "occurrence_count": 1,
        "output_count": 0,
        "registry_kind": "item",
        "registry_path": path,
        "runtime_id": f"cruciblecraft:{path}",
        "source_evidence": "docs/history/card-plans/closed/裂变观测安全与能源Jade详细计划.md",
        "source_item": source,
        "texture": texture,
    }


def patch_catalog() -> None:
    document = json.loads(CATALOG.read_text(encoding="utf-8"))
    identities = document["identities"]
    by_path = {row["registry_path"]: row for row in identities}
    thermometer = catalog_row(
        "Quicksilver Thermometer",
        "水银温度计",
        "gregtech:gt.multiitem.randomtools",
        "gt_multiitem/multiitem_randomtools_m10000",
        "cruciblecraft:item/gt6_import/thermometer_quicksilver",
        10000,
        "multiitem",
        "thermometer",
    )
    if thermometer["registry_path"] not in by_path:
        insert_at = next(
            index
            for index, row in enumerate(identities)
            if row["registry_path"] == "gt_multiitem/multiitem_randomtools_m10001"
        )
        identities.insert(insert_at, thermometer)
        by_path[thermometer["registry_path"]] = thermometer
    else:
        by_path[thermometer["registry_path"]].update(thermometer)

    geiger_textures = {
        "gt_multiitem/multiitem_randomtools_m10001": "cruciblecraft:item/gt6_import/geiger_empty",
        "gt_multiitem/multiitem_randomtools_m10002": "cruciblecraft:item/gt6_import/geiger_filled",
    }
    for path, texture in geiger_textures.items():
        if path not in by_path:
            raise SystemExit(f"missing catalog row {path}")
        by_path[path]["texture"] = texture

    for piece in HAZMAT:
        path = f"gt_object/gt_armor_hazmat_{piece['suit']}_{piece['piece']}_m0"
        row = catalog_row(
            piece["english"],
            piece["chinese"],
            piece["source"],
            path,
            piece["texture"],
            0,
            "armor",
            "armor",
        )
        if path in by_path:
            by_path[path]["texture"] = piece["texture"]
            by_path[path]["english_name"] = piece["english"]
            by_path[path]["chinese_name"] = piece["chinese"]
            by_path[path]["behavior"] = "armor"
            continue
        marker = f"gt_object/gt_armor_hazmat_{piece['suit']}_boots_m0"
        insert_at = next(
            index
            for index, existing in enumerate(identities)
            if existing["registry_path"] == marker
        )
        identities.insert(insert_at, row)
        by_path[path] = row

    document["identities"] = identities
    document["identity_count"] = len(identities)
    document["variant_count"] = len(identities)
    if document["identity_count"] != 247:
        raise SystemExit(
            f"catalog identity_count drifted to {document['identity_count']}"
        )
    dump(CATALOG, document)


def write_models_and_art() -> None:
    require_gt6_art()
    imports: list[dict] = []
    item_root = SRC / "assets" / "cruciblecraft" / "textures" / "item" / "gt6_import"
    model_root = SRC / "assets" / "cruciblecraft" / "models" / "item"
    armor_root = SRC / "assets" / "cruciblecraft" / "textures" / "models" / "armor"
    worn: dict[str, dict[int, list[list[tuple[int, int, int, int]]]]] = {
        "radiation": {},
        "heat": {},
    }
    for piece in HAZMAT:
        slot = SLOT[piece["piece"]]
        name = f"hazmat_{piece['suit']}_{piece['piece']}"
        src = GT6_ART / "items" / "armor" / f"hazard_{piece['suit']}" / f"{slot}.png"
        dest = item_root / f"{name}.png"
        copy_png(src, dest)
        record_copy(
            imports,
            f"assets/gregtech/textures/items/armor/hazard_{piece['suit']}/{slot}.png",
            f"assets/cruciblecraft/textures/item/gt6_import/{name}.png",
        )
        dump(
            model_root
            / "gt_object"
            / f"gt_armor_hazmat_{piece['suit']}_{piece['piece']}_m0.json",
            {
                "parent": "minecraft:item/generated",
                "textures": {"layer0": piece["texture"]},
            },
        )
        worn_src = GT6_ART / "armor" / f"hazard_{piece['suit']}" / f"{slot}.png"
        worn[piece["suit"]][slot] = read_png_rgba(worn_src)
    for name, meta, model_path in HANDHELD:
        src = GT6_ART / "items" / "gt.multiitem.randomtools" / f"{meta}.png"
        dest = item_root / f"{name}.png"
        copy_png(src, dest)
        record_copy(
            imports,
            f"assets/gregtech/textures/items/gt.multiitem.randomtools/{meta}.png",
            f"assets/cruciblecraft/textures/item/gt6_import/{name}.png",
        )
        canonical = (
            SRC / "assets" / "cruciblecraft" / "textures" / "item" / f"{model_path}.png"
        )
        copy_png(src, canonical)
        dump(
            model_root / Path(model_path + ".json"),
            {
                "parent": "minecraft:item/generated",
                "textures": {"layer0": f"cruciblecraft:item/{model_path}"},
            },
        )
    for kind, slots in worn.items():
        if slots.keys() != {0, 1, 2, 3}:
            raise SystemExit(f"missing worn slots for {kind}: {sorted(slots)}")
        write_png(
            armor_root / f"hazmat_{kind}_layer_1.png",
            overlay(overlay(slots[0], slots[1]), slots[3]),
        )
        copy_png(
            GT6_ART / "armor" / f"hazard_{kind}" / "2.png",
            armor_root / f"hazmat_{kind}_layer_2.png",
        )
        record_copy(
            imports,
            f"assets/gregtech/textures/armor/hazard_{kind}/{{0,1,3}}.png",
            f"assets/cruciblecraft/textures/models/armor/hazmat_{kind}_layer_1.png",
            "Vanilla 1.21 layer_1 composed from GT6 64x32 slot sheets 0+1+3.",
        )
        record_copy(
            imports,
            f"assets/gregtech/textures/armor/hazard_{kind}/2.png",
            f"assets/cruciblecraft/textures/models/armor/hazmat_{kind}_layer_2.png",
            "Vanilla 1.21 layer_2 copied from GT6 64x32 slot sheet 2.",
        )
    dump(
        SRC / "assets" / "cruciblecraft" / "gt6_fission_observation_art_manifest.json",
        {
            "source": "gt6_referencable_port_code/gregtech6_w",
            "source_revision": REVISION,
            "source_present": True,
            "imports": imports,
        },
    )


def shaped(path: Path, pattern: list[str], ingredients: dict, result: str, catalysts: dict | None = None) -> None:
    document = {
        "type": "cruciblecraft:shaped_catalyst",
        "pattern": pattern,
        "ingredients": ingredients,
        "result": {"id": result, "count": 1},
    }
    if catalysts:
        document["catalysts"] = catalysts
    dump(path, document)


def write_recipes() -> None:
    recipe_root = SRC / "data" / "cruciblecraft" / "recipe"
    lead = {"item": "cruciblecraft:lead/plate"}
    foil = {"item": "cruciblecraft:aluminium/foil"}
    glass = {"item": "minecraft:glass_pane"}
    black_glass = {"item": "minecraft:black_stained_glass_pane"}
    tools = {
        "x": {"item": "cruciblecraft:material_screwdriver"},
        "f": {"item": "cruciblecraft:material_file"},
    }
    shaped(
        recipe_root / "gt_object" / "gt_armor_hazmat_radiation_head_m0.json",
        ["PPP", "PGP", "x f"],
        {"P": lead, "G": glass},
        "cruciblecraft:gt_object/gt_armor_hazmat_radiation_head_m0",
        tools,
    )
    shaped(
        recipe_root / "gt_object" / "gt_armor_hazmat_radiation_chest_m0.json",
        ["PxP", "PPP", "PfP"],
        {"P": lead},
        "cruciblecraft:gt_object/gt_armor_hazmat_radiation_chest_m0",
        tools,
    )
    shaped(
        recipe_root / "gt_object" / "gt_armor_hazmat_radiation_legs_m0.json",
        ["PPP", "PxP", "PfP"],
        {"P": lead},
        "cruciblecraft:gt_object/gt_armor_hazmat_radiation_legs_m0",
        tools,
    )
    shaped(
        recipe_root / "gt_object" / "gt_armor_hazmat_radiation_boots_m0.json",
        ["x f", "P P", "P P"],
        {"P": lead},
        "cruciblecraft:gt_object/gt_armor_hazmat_radiation_boots_m0",
        tools,
    )
    shaped(
        recipe_root / "gt_object" / "gt_armor_hazmat_heat_head_m0.json",
        ["FFF", "FGF", "x f"],
        {"F": foil, "G": black_glass},
        "cruciblecraft:gt_object/gt_armor_hazmat_heat_head_m0",
        tools,
    )
    shaped(
        recipe_root / "gt_object" / "gt_armor_hazmat_heat_chest_m0.json",
        ["FxF", "FFF", "FfF"],
        {"F": foil},
        "cruciblecraft:gt_object/gt_armor_hazmat_heat_chest_m0",
        tools,
    )
    shaped(
        recipe_root / "gt_object" / "gt_armor_hazmat_heat_legs_m0.json",
        ["FFF", "FxF", "FfF"],
        {"F": foil},
        "cruciblecraft:gt_object/gt_armor_hazmat_heat_legs_m0",
        tools,
    )
    shaped(
        recipe_root / "gt_object" / "gt_armor_hazmat_heat_boots_m0.json",
        ["x f", "F F", "F F"],
        {"F": foil},
        "cruciblecraft:gt_object/gt_armor_hazmat_heat_boots_m0",
        tools,
    )
    shaped(
        recipe_root / "gt_multiitem" / "multiitem_randomtools_m10000.json",
        [" G ", "CMC", " D "],
        {
            "C": {"item": "cruciblecraft:copper/plate"},
            "G": glass,
            "M": {"item": "cruciblecraft:mercury/ingot"},
            "D": {"item": "minecraft:red_dye"},
        },
        "cruciblecraft:gt_multiitem/multiitem_randomtools_m10000",
    )
    dump(
        recipe_root / "gt_multiitem" / "extruder_shape_ccc.json",
        {
            "type": "cruciblecraft:shaped_catalyst",
            "pattern": ["   ", " P ", "  x"],
            "ingredients": {"P": {"item": "cruciblecraft:extruder_shape_ring"}},
            "result": {"id": "cruciblecraft:extruder_shape_ccc", "count": 1},
            "catalysts": {"x": {"item": "cruciblecraft:material_screwdriver"}},
        },
    )
    dump(
        recipe_root / "gt_multiitem" / "multiitem_randomtools_m10001.json",
        {
            "type": "cruciblecraft:shaped_catalyst",
            "pattern": ["TXT", "PCP", "TdT"],
            "ingredients": {
                "X": {"item": "cruciblecraft:aluminium/capcellcon"},
                "P": {"item": "cruciblecraft:aluminium/plate"},
                "T": {"item": "cruciblecraft:aluminium/screw"},
                "C": {"item": "cruciblecraft:circuit_basic"},
            },
            "result": {
                "id": "cruciblecraft:gt_multiitem/multiitem_randomtools_m10001",
                "count": 1,
            },
            "catalysts": {"d": {"item": "cruciblecraft:material_screwdriver"}},
        },
    )
    dump(
        recipe_root / "nuclear" / "aluminium_capcellcon.json",
        {
            "type": "cruciblecraft:gt_recipe",
            "map": "cruciblecraft:extruder",
            "duration": 64,
            "eut": 16,
            "can_be_buffered": True,
            "item_inputs": [
                {"item": "cruciblecraft:aluminium/ingot"},
                {"item": "cruciblecraft:extruder_shape_ccc"},
            ],
            "item_input_counts": [1, 0],
            "item_outputs": [
                {"id": "cruciblecraft:aluminium/capcellcon", "count": 9}
            ],
            "output_chances": [10000],
            "provenance": {
                "selected_source_recipe": "Loader_Recipes_Handlers.java:766 aluminium/ingot -> 9 capcellcon",
                "source_kind": "gt6_source",
            },
        },
    )
    empty_geiger = {
        "item": "cruciblecraft:gt_multiitem/multiitem_randomtools_m10001"
    }
    for gas in ("helium", "neon", "argon"):
        dump(
            recipe_root / "nuclear" / f"geiger_canner_{gas}.json",
            {
                "type": "cruciblecraft:gt_recipe",
                "map": "cruciblecraft:canner",
                "duration": 64,
                "eut": 16,
                "can_be_buffered": True,
                "item_inputs": [empty_geiger],
                "item_input_counts": [1],
                "item_outputs": [
                    {
                        "id": "cruciblecraft:gt_multiitem/multiitem_randomtools_m10002",
                        "count": 1,
                    }
                ],
                "output_chances": [10000],
                "provenance": {
                    "selected_source_recipe": f"MultiItemRandomTools.java:541-543 {gas} 1000 mB",
                    "source_kind": "gt6_source",
                },
                "fluid_inputs": [
                    {"id": f"cruciblecraft:{gas}", "amount": 1000}
                ],
            },
        )


def write_wave() -> None:
    WAVE.mkdir(parents=True, exist_ok=True)
    dump(
        WAVE / "wave.json",
        {
            "cohort": "fission-observation-safety",
            "depends_on": ["runtime/fission-hot-fluids"],
            "generated_by": "runtime/fission-observation-safety implementation",
            "owns_families": 0,
            "program": "runtime/fission-observation-safety",
            "schema_version": 1,
            "wave_slug": "runtime/fission-observation-safety",
        },
    )
    dump(
        WAVE / "readiness.json",
        {
            "evidence": {
                "acquisition": "reactor_observation_safety",
                "battery_identity_count": 37,
                "converter_live_rows": 179,
                "core_identity_count": 2,
                "hazmat_piece_count": 8,
                "handheld_tool_behaviors": 2,
                "jade_new_families": 3,
                "observation_safety_status": "player_complete",
                "radiation_apply_paths": 3,
            },
            "generated_by": "runtime/fission-observation-safety implementation",
            "generated_recipe_count": 15,
            "next_unassigned": True,
            "nuclear_started": True,
            "owns_families": 0,
            "production_lock": None,
            "schema_version": 1,
            "source_revision": REVISION,
            "status": "FISSION_OBSERVATION_SAFETY_READY",
            "unique_active_wave": None,
            "wave_slug": "runtime/fission-observation-safety",
        },
    )
    dump(
        WAVE / "topology.json",
        {
            "append_only": False,
            "complete_family_count": 0,
            "generated_by": "runtime/fission-observation-safety implementation",
            "next_unassigned": True,
            "remaining_recipe_gap": 0,
            "schema_version": 1,
            "source_revision": REVISION,
            "status": "WAVE_READY",
            "unique_active_wave": None,
            "wave_slug": "runtime/fission-observation-safety",
        },
    )
    dump(
        WAVE / "census_delta.json",
        {
            "complete_family_count": 0,
            "completion_delta": 0,
            "generated_by": "runtime/fission-observation-safety implementation",
            "leftover_later_count": 0,
            "partial_family_count": 0,
            "remaining_recipe_gap": 0,
            "schema_version": 1,
            "source_revision": REVISION,
            "status": "CENSUS_DELTA_IN_PROGRESS",
            "wave_slug": "runtime/fission-observation-safety",
            "work_set": {"family_count": 0, "source_rows": 13},
        },
    )
    dump(
        WAVE / "required_forms.json",
        {
            "counts": {"required_form_pairs": 4, "required_materials": 4},
            "generated_by": "runtime/fission-observation-safety implementation",
            "note": "Lead plate, aluminium foil, copper plate, mercury ingot already registered.",
            "required_forms": {
                "aluminium": ["foil"],
                "copper": ["plate"],
                "lead": ["plate"],
                "mercury": ["ingot"],
            },
            "schema_version": 1,
            "wave_slug": "runtime/fission-observation-safety",
        },
    )
    dump(
        WAVE / "load_axis.json",
        {
            "battery_identity_count": 37,
            "converter_live_rows": 179,
            "core_identity_count": 2,
            "generated_by": "runtime/fission-observation-safety implementation",
            "hazmat_piece_count": 8,
            "schema_version": 1,
            "source_revision": REVISION,
            "wave_slug": "runtime/fission-observation-safety",
        },
    )
    dump(
        WAVE / "d0_observation_source_matrix.json",
        {
            "schema_version": 1,
            "source_revision": REVISION,
            "wave_slug": "runtime/fission-observation-safety",
            "status": "D0_MATRIX_READY",
            "source_contract_rows": 13,
            "exact_rows": 13,
            "explicitly_blocked_rows": 0,
            "missing": 0,
            "extra": 0,
            "duplicate": 0,
            "stand_in_parts": 0,
            "kelvin_fields": 0,
            "world_explode_uncommented": 0,
            "branches": [
                {
                    "branch": "radiation_helmet",
                    "disposition": "ready",
                    "parts": ["lead/plate", "minecraft:glass_pane", "material_screwdriver", "material_file"],
                },
                {
                    "branch": "radiation_chest",
                    "disposition": "ready",
                    "parts": ["lead/plate", "material_screwdriver", "material_file"],
                },
                {
                    "branch": "radiation_legs",
                    "disposition": "ready",
                    "parts": ["lead/plate", "material_screwdriver", "material_file"],
                },
                {
                    "branch": "radiation_boots",
                    "disposition": "ready",
                    "parts": ["lead/plate", "material_screwdriver", "material_file"],
                },
                {
                    "branch": "heat_helmet",
                    "disposition": "ready",
                    "parts": ["aluminium/foil", "minecraft:black_stained_glass_pane", "material_screwdriver", "material_file"],
                },
                {
                    "branch": "heat_chest",
                    "disposition": "ready",
                    "parts": ["aluminium/foil", "material_screwdriver", "material_file"],
                },
                {
                    "branch": "heat_legs",
                    "disposition": "ready",
                    "parts": ["aluminium/foil", "material_screwdriver", "material_file"],
                },
                {
                    "branch": "heat_boots",
                    "disposition": "ready",
                    "parts": ["aluminium/foil", "material_screwdriver", "material_file"],
                },
                {
                    "branch": "thermometer",
                    "disposition": "ready",
                    "parts": ["copper/plate", "minecraft:glass_pane", "minecraft:red_dye", "mercury/ingot"],
                    "quicksilver_mapping": "mercury/ingot via GT6 QuickSilver alias",
                },
                {
                    "branch": "geiger_empty",
                    "disposition": "ready",
                    "parts": [
                        "aluminium/capcellcon",
                        "aluminium/plate",
                        "aluminium/screw",
                        "circuit_basic",
                        "material_screwdriver",
                    ],
                },
                {
                    "branch": "geiger_canner_helium",
                    "disposition": "ready",
                    "parts": [
                        "gt_multiitem/multiitem_randomtools_m10001",
                        "cruciblecraft:helium",
                    ],
                },
                {
                    "branch": "geiger_canner_neon",
                    "disposition": "ready",
                    "parts": [
                        "gt_multiitem/multiitem_randomtools_m10001",
                        "cruciblecraft:neon",
                    ],
                },
                {
                    "branch": "geiger_canner_argon",
                    "disposition": "ready",
                    "parts": [
                        "gt_multiitem/multiitem_randomtools_m10001",
                        "cruciblecraft:argon",
                    ],
                },
            ],
            "blocked_ledger": [
                {
                    "semantic_key": "reactor:temperature_kelvin",
                    "disposition": "blocked",
                    "reason": "Thermometer reports lastHeat as HU. No Kelvin conversion.",
                },
                {
                    "semantic_key": "reactor:world_explode",
                    "disposition": "blocked",
                    "reason": "GT6 explode() TODO stays commented. Sound + pulse only.",
                },
                {
                    "semantic_key": "reactor:backpack_radioactivity_enchantment",
                    "disposition": "blocked",
                    "reason": "CC materials have no getRadioactivityLevel table.",
                },
            ],
            "temperature_contract": {
                "heat_unit": "HU",
                "gt6_thermometer": "oEnergy HU",
                "temperature_k": "blocked",
                "meltdown_at_k": "blocked",
            },
            "fail_semantics": {
                "rods_destroyed": True,
                "explode_sound": True,
                "radiation_pulse": True,
                "world_explode": "blocked",
                "output_full_backpressure": "retain_heat_and_input",
            },
        },
    )


def write_capability() -> None:
    CAP.mkdir(parents=True, exist_ok=True)
    dump(
        CAP / "capability.json",
        {
            "schema_version": 2,
            "slug": "energy/nuclear-fission-observation-safety",
            "title": "Nuclear Fission Observation Safety",
            "maturity": "player_complete",
            "workflow": "accepted",
            "owned_paths": [
                "src/main/java/com/masson/cruciblecraft/compat/jade/**",
                "src/main/java/com/masson/cruciblecraft/content/block/ReactorCoreBlock.java",
                "src/main/java/com/masson/cruciblecraft/content/blockentity/ReactorCoreBlockEntity.java",
                "src/main/java/com/masson/cruciblecraft/nuclear/**",
                "src/main/java/com/masson/cruciblecraft/content/item/HazmatArmorItem.java",
                "src/main/java/com/masson/cruciblecraft/content/item/ThermometerItem.java",
                "src/main/java/com/masson/cruciblecraft/content/item/GeigerCounterItem.java",
                "src/main/java/com/masson/cruciblecraft/registry/ModItems.java",
                "src/main/java/com/masson/cruciblecraft/datagen/ModLanguageProvider.java",
                "src/main/java/com/masson/cruciblecraft/gametest/NuclearFissionObservationSafetyGameTests.java",
                "src/main/resources/data/cruciblecraft_wave_runtime_fission_observation_safety/**",
                "src/main/resources/assets/cruciblecraft/gt6_fission_observation_art_manifest.json",
                "src/main/resources/assets/cruciblecraft/gt6_geiger_obtain_art_manifest.json",
                "src/main/resources/data/cruciblecraft/material_prefixes/capcellcon.json",
                "src/main/resources/data/cruciblecraft/recipe/gt_multiitem/extruder_shape_ccc.json",
                "src/main/resources/data/cruciblecraft/recipe/gt_multiitem/multiitem_randomtools_m10001.json",
                "src/main/resources/data/cruciblecraft/recipe/nuclear/aluminium_capcellcon.json",
                "src/main/resources/data/cruciblecraft/recipe/nuclear/geiger_canner_*.json",
                "tools/emit_geiger_obtain.py",
                "src/main/resources/assets/cruciblecraft/textures/**/gt6_import/hazmat_*",
                "src/main/resources/assets/cruciblecraft/textures/item/gt6_import/thermometer_quicksilver.png",
                "src/main/resources/assets/cruciblecraft/textures/item/gt6_import/geiger_*.png",
                "src/main/resources/assets/cruciblecraft/textures/models/armor/hazmat_*",
                "src/test/java/com/masson/cruciblecraft/nuclear/**",
                "src/test/java/com/masson/cruciblecraft/compat/jade/**",
                "tools/jade_observation_matrix.json",
                "tools/capabilities/energy/nuclear-fission-observation-safety/**",
                "tools/waves/runtime/fission-observation-safety/**",
                "tools/tests/test_energy_nuclear_fission_observation_safety.py",
            ],
            "depends_on": ["energy/nuclear-fission-hot-fluids"],
            "profiles": ["capability-runtime", "player-complete"],
            "wave_slug": "runtime/fission-observation-safety",
            "legacy_readiness": "tools/waves/runtime/fission-observation-safety/readiness.json",
            "player_signoff": "tools/capabilities/energy/nuclear-fission-observation-safety/player_signoff.json",
            "required_test_ids": [
                "areaRadiationUsesNeutronOver256",
                "contactAppliesHeatAndRadiation",
                "eightHazmatPiecesAreWearable",
                "emptyCoolantDestroysRodsWithFailSemantics",
                "emptyGeigerDoesNotRead",
                "failBurstDoublesRangeAndPlaysSound",
                "filledGeigerReadsNeutrons",
                "fullHeatSuitBlocksHeat",
                "fullRadiationSuitBlocksRadiation",
                "jadeReactorHasNoKelvin",
                "outputFullDoesNotDestroyRods",
                "playerSurfaceIsRegistered",
                "thermometerReadsLastHeatHu",
            ],
            "identity_disposition": [
                {
                    "semantic_key": "reactor:observation_safety",
                    "disposition": "new_distinct",
                    "runtime_ids": [
                        "cruciblecraft:reactor_core_1x1",
                        "cruciblecraft:reactor_core_2x2",
                    ],
                    "reason": "Two cores share one HU Jade provider plus radiation, heat, fail sound/pulse.",
                },
                {
                    "semantic_key": "reactor:hazmat",
                    "disposition": "new_distinct",
                    "runtime_ids": [
                        "cruciblecraft:gt_object/gt_armor_hazmat_radiation_head_m0",
                        "cruciblecraft:gt_object/gt_armor_hazmat_radiation_chest_m0",
                        "cruciblecraft:gt_object/gt_armor_hazmat_radiation_legs_m0",
                        "cruciblecraft:gt_object/gt_armor_hazmat_radiation_boots_m0",
                        "cruciblecraft:gt_object/gt_armor_hazmat_heat_head_m0",
                        "cruciblecraft:gt_object/gt_armor_hazmat_heat_chest_m0",
                        "cruciblecraft:gt_object/gt_armor_hazmat_heat_legs_m0",
                        "cruciblecraft:gt_object/gt_armor_hazmat_heat_boots_m0",
                    ],
                    "reason": "Eight wearable radiation/heat pieces. Insect/biochem/frost/universal stay later.",
                },
                {
                    "semantic_key": "reactor:handheld_tools",
                    "disposition": "new_distinct",
                    "runtime_ids": [
                        "cruciblecraft:gt_multiitem/multiitem_randomtools_m10000",
                        "cruciblecraft:gt_multiitem/multiitem_randomtools_m10001",
                        "cruciblecraft:gt_multiitem/multiitem_randomtools_m10002",
                    ],
                    "reason": "Thermometer HU and Geiger neutrons. Empty Geiger is aluminium capcellcon + Canner fill.",
                },
                {
                    "semantic_key": "energy:battery_jade",
                    "disposition": "new_distinct",
                    "runtime_ids": ["cruciblecraft:energy_battery"],
                    "reason": "37 battery identities share one Jade provider. stored() not displayedEnergy.",
                },
                {
                    "semantic_key": "energy:converter_jade",
                    "disposition": "new_distinct",
                    "runtime_ids": ["cruciblecraft:energy_converter"],
                    "reason": "179 live converter rows share one observation adapter.",
                },
                {
                    "semantic_key": "reactor:temperature_kelvin",
                    "disposition": "blocked",
                    "runtime_ids": [],
                    "reason": "Heat is HU. Do not invent Kelvin as HU times a constant.",
                },
                {
                    "semantic_key": "reactor:world_explode",
                    "disposition": "blocked",
                    "runtime_ids": [],
                    "reason": "GT6 explode() TODO stays commented.",
                },
                {
                    "semantic_key": "reactor:geiger_empty_capcellcon",
                    "disposition": "new_distinct",
                    "runtime_ids": [
                        "cruciblecraft:aluminium/capcellcon",
                        "cruciblecraft:extruder_shape_ccc",
                        "cruciblecraft:gt_multiitem/multiitem_randomtools_m10001",
                        "cruciblecraft:gt_multiitem/multiitem_randomtools_m10002",
                    ],
                    "reason": "Aluminium OP.capcellcon only. CCC shape + extruder + empty Geiger + He/Ne/Ar Canner.",
                },
                {
                    "semantic_key": "reactor:backpack_radioactivity",
                    "disposition": "blocked",
                    "runtime_ids": [],
                    "reason": "No CC material radioactivity level table.",
                },
            ],
            "note": "Fission wrap plus energy Jade second slice. Geiger obtain is source-exact aluminium capcellcon. unique_active_wave stays null. No heat exchangers, turbines, coolers, or Kelvin.",
        },
    )
    dump(
        CAP / "player_signoff.json",
        {
            "schema_version": 1,
            "capability": "energy/nuclear-fission-observation-safety",
            "signed": True,
            "signer": "player",
            "date": "2026-09-09",
            "craftable_items": [
                "gt_object/gt_armor_hazmat_radiation_head_m0",
                "gt_object/gt_armor_hazmat_radiation_chest_m0",
                "gt_object/gt_armor_hazmat_radiation_legs_m0",
                "gt_object/gt_armor_hazmat_radiation_boots_m0",
                "gt_object/gt_armor_hazmat_heat_head_m0",
                "gt_object/gt_armor_hazmat_heat_chest_m0",
                "gt_object/gt_armor_hazmat_heat_legs_m0",
                "gt_object/gt_armor_hazmat_heat_boots_m0",
                "gt_multiitem/multiitem_randomtools_m10000",
                "gt_multiitem/multiitem_randomtools_m10001",
                "gt_multiitem/multiitem_randomtools_m10002",
            ],
            "checklist": {
                "two_cores_jade_hu_no_kelvin": True,
                "thirty_seven_batteries_one_provider": True,
                "one_hundred_seventy_nine_converters_one_adapter": True,
                "eight_hazmat_wearable": True,
                "contact_area_fail_radiation_and_heat": True,
                "fail_is_rods_sound_pulse": True,
                "world_explode_blocked": True,
                "thermometer_and_geiger_wired": True,
                "geiger_obtain_explicitly_blocked": False,
                "no_stand_in_capcellcon": True,
                "no_iron_ingot_hazmat_textures": True,
            },
            "notes": "Empty Geiger is aluminium capcellcon + CCC + Canner He/Ne/Ar. Heat is HU. Output-full backpressure stays DESIGN_POLICY. Insect/biochem/frost/universal suits stay later.",
        },
    )


def copy_gametest_structures() -> None:
    src_ns = SRC / "data" / "cruciblecraft_wave_runtime_fission_survival"
    dst_ns = SRC / "data" / NS
    for relative in (
        Path("structure") / "empty.nbt",
        Path("gametest") / "structure" / "empty.nbt",
    ):
        src = src_ns / relative
        dst = dst_ns / relative
        dst.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(src, dst)


def main() -> None:
    patch_catalog()
    write_models_and_art()
    write_recipes()
    write_wave()
    write_capability()
    copy_gametest_structures()


if __name__ == "__main__":
    main()
