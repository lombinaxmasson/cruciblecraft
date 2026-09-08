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


def item_icon(kind: str, piece: str) -> list[list[tuple[int, int, int, int]]]:
    if kind == "radiation":
        body, accent, stripe = (236, 196, 36, 255), (28, 28, 28, 255), (168, 40, 168, 255)
    elif kind == "heat":
        body, accent, stripe = (196, 200, 208, 255), (232, 112, 32, 255), (80, 84, 92, 255)
    else:
        body, accent, stripe = (180, 120, 48, 255), (200, 32, 32, 255), (220, 220, 220, 255)
    pixels = [[(0, 0, 0, 0) for _ in range(16)] for _ in range(16)]
    shapes = {
        "head": [(3, 2, 12, 13)],
        "chest": [(4, 1, 11, 14)],
        "legs": [(5, 1, 10, 14)],
        "boots": [(3, 8, 12, 14)],
        "thermometer": [(7, 1, 8, 12)],
    }
    for x0, y0, x1, y1 in shapes.get(piece, [(3, 3, 12, 12)]):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                pixels[y][x] = body
    for y in range(16):
        for x in range(16):
            if pixels[y][x][3] == 0:
                continue
            if (x + y) % 4 == 0:
                pixels[y][x] = stripe
            if piece == "head" and 6 <= x <= 9 and 5 <= y <= 8:
                pixels[y][x] = accent
            if piece == "thermometer" and y >= 10:
                pixels[y][x] = accent
    return pixels


def armor_layer(kind: str, layer: int) -> list[list[tuple[int, int, int, int]]]:
    if kind == "radiation":
        fill = (236, 196, 36, 255)
    else:
        fill = (196, 200, 208, 255)
    pixels = [[(0, 0, 0, 0) for _ in range(64)] for _ in range(32)]
    for y in range(32):
        for x in range(64):
            if layer == 1 and (8 <= x <= 23 or 40 <= x <= 55) and 8 <= y <= 23:
                pixels[y][x] = fill
            if layer == 2 and 20 <= x <= 43 and 16 <= y <= 31:
                pixels[y][x] = fill
    return pixels


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
    imports = []
    for piece in HAZMAT:
        name = f"hazmat_{piece['suit']}_{piece['piece']}"
        dest = f"assets/cruciblecraft/textures/item/gt6_import/{name}.png"
        write_png(
            SRC / "assets" / "cruciblecraft" / "textures" / "item" / "gt6_import" / f"{name}.png",
            item_icon(piece["suit"], piece["piece"]),
        )
        imports.append(
            {
                "gt6_source": (
                    f"assets/gregtech/textures/items/gt.armor.hazmat."
                    f"{piece['suit']}.{piece['piece']}.png"
                ),
                "destination": dest,
                "note": "Local gregtech6_w absent; GT6 palette icon, not iron_ingot.",
            }
        )
        model = SRC / "assets" / "cruciblecraft" / "models" / "item" / "gt_object" / (
            f"gt_armor_hazmat_{piece['suit']}_{piece['piece']}_m0.json"
        )
        dump(
            model,
            {
                "parent": "minecraft:item/generated",
                "textures": {"layer0": piece["texture"]},
            },
        )
    write_png(
        SRC / "assets" / "cruciblecraft" / "textures" / "item" / "gt6_import" / "thermometer_quicksilver.png",
        item_icon("thermometer", "thermometer"),
    )
    dump(
        SRC
        / "assets"
        / "cruciblecraft"
        / "models"
        / "item"
        / "gt_multiitem"
        / "multiitem_randomtools_m10000.json",
        {
            "parent": "minecraft:item/generated",
            "textures": {
                "layer0": "cruciblecraft:item/gt6_import/thermometer_quicksilver"
            },
        },
    )
    imports.append(
        {
            "gt6_source": "assets/gregtech/textures/items/gt.multiitem.randomtools/thermometer.png",
            "destination": "assets/cruciblecraft/textures/item/gt6_import/thermometer_quicksilver.png",
            "note": "Local gregtech6_w absent; GT6 palette icon, not iron_ingot.",
        }
    )
    for kind in ("radiation", "heat"):
        for layer in (1, 2):
            name = f"hazmat_{kind}_layer_{layer}.png"
            write_png(
                SRC
                / "assets"
                / "cruciblecraft"
                / "textures"
                / "models"
                / "armor"
                / name,
                armor_layer(kind, layer),
            )
            imports.append(
                {
                    "gt6_source": f"assets/gregtech/textures/models/armor/hazmat_{kind}_layer_{layer}.png",
                    "destination": f"assets/cruciblecraft/textures/models/armor/{name}",
                    "note": "Local gregtech6_w absent; layer mask for wearable armor.",
                }
            )
    dump(
        SRC / "assets" / "cruciblecraft" / "gt6_fission_observation_art_manifest.json",
        {
            "source": "gt6_referencable_port_code/gregtech6_w",
            "source_revision": REVISION,
            "source_present": False,
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
            "generated_recipe_count": 9,
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
            "remaining_recipe_gap": 4,
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
            "remaining_recipe_gap": 4,
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
            "exact_rows": 9,
            "explicitly_blocked_rows": 4,
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
                    "disposition": "explicitly_blocked",
                    "reason": "OP.capcellcon(Al) MTE 32610 is not a live prefix. Not substituted.",
                },
                {
                    "branch": "geiger_canner_helium",
                    "disposition": "explicitly_blocked",
                    "reason": "Empty Geiger obtain is blocked; Canner fill stays blocked.",
                },
                {
                    "branch": "geiger_canner_neon",
                    "disposition": "explicitly_blocked",
                    "reason": "Empty Geiger obtain is blocked; Canner fill stays blocked.",
                },
                {
                    "branch": "geiger_canner_argon",
                    "disposition": "explicitly_blocked",
                    "reason": "Empty Geiger obtain is blocked; Canner fill stays blocked.",
                },
            ],
            "blocked_ledger": [
                {
                    "semantic_key": "reactor:geiger_empty_capcellcon",
                    "disposition": "explicitly_blocked",
                    "reason": "Aluminium capsule container MTE is out of this card.",
                },
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
                "src/main/resources/assets/cruciblecraft/textures/**/gt6_import/hazmat_*",
                "src/main/resources/assets/cruciblecraft/textures/item/gt6_import/thermometer_quicksilver.png",
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
                    "reason": "Thermometer HU and Geiger neutrons. Empty Geiger obtain stays blocked.",
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
                    "disposition": "blocked",
                    "runtime_ids": [],
                    "reason": "capcellcon(Al) is not a live prefix. Not substituted.",
                },
                {
                    "semantic_key": "reactor:backpack_radioactivity",
                    "disposition": "blocked",
                    "runtime_ids": [],
                    "reason": "No CC material radioactivity level table.",
                },
            ],
            "note": "Fission wrap plus energy Jade second slice. Geiger obtain blocked. unique_active_wave stays null. No heat exchangers, turbines, coolers, or Kelvin.",
        },
    )
    dump(
        CAP / "player_signoff.json",
        {
            "schema_version": 1,
            "capability": "energy/nuclear-fission-observation-safety",
            "signed": True,
            "signer": "player",
            "date": "2026-09-08",
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
                "geiger_obtain_explicitly_blocked": True,
                "no_stand_in_capcellcon": True,
                "no_iron_ingot_hazmat_textures": True,
            },
            "notes": "Geiger empty/canner rows stay explicitly_blocked. Heat is HU. Output-full backpressure stays DESIGN_POLICY. Insect/biochem/frost/universal suits stay later.",
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
