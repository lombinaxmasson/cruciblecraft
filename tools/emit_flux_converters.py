#!/usr/bin/env python3
"""Flux FE↔GU converters: catalog, art, wave, capability."""
from __future__ import annotations

import json
import shutil
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import io_common as io

SRC = ROOT / "src" / "main" / "resources"
REVISION = io.SOURCE_REVISION
CAPABILITY_SLUG = "energy/flux-converters"
WAVE_SLUG = "runtime/flux-converters"
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
MATERIALS = (
    {
        "tier": 1,
        "material": "lead",
        "lang_en": "Lead",
        "lang_zh": "铅",
        "nbt_in_rf": 128,
        "nbt_out_gu": 16,
        "nbt_in_ru": 32,
        "nbt_out_rf": 88,
        "heater_host": "cruciblecraft:steel_galvanized_electric_heater",
        "engine_host": "cruciblecraft:steel_galvanized_electric_engine",
        "motor_host": "cruciblecraft:steel_galvanized_electric_motor",
        "magnet_host": "cruciblecraft:steel_galvanized_electromagnet",
        "laser_host": "cruciblecraft:steel_galvanized_electric_co2_laser",
        "dynamo_host": "cruciblecraft:bronze_dynamo",
        "magnet_live": False,
        "laser_live": False,
    },
    {
        "tier": 2,
        "material": "invar",
        "lang_en": "Invar",
        "lang_zh": "殷钢",
        "nbt_in_rf": 512,
        "nbt_out_gu": 64,
        "nbt_in_ru": 128,
        "nbt_out_rf": 352,
        "heater_host": "cruciblecraft:aluminium_electric_heater",
        "engine_host": "cruciblecraft:aluminium_electric_engine",
        "motor_host": "cruciblecraft:aluminium_electric_motor",
        "magnet_host": "cruciblecraft:aluminium_electromagnet",
        "laser_host": "cruciblecraft:aluminium_electric_co2_laser",
        "dynamo_host": "cruciblecraft:aluminium_dynamo",
        "magnet_live": False,
        "laser_live": False,
    },
    {
        "tier": 3,
        "material": "electrum",
        "lang_en": "Electrum",
        "lang_zh": "琥珀金",
        "nbt_in_rf": 2048,
        "nbt_out_gu": 256,
        "nbt_in_ru": 512,
        "nbt_out_rf": 1408,
        "heater_host": "cruciblecraft:stainless_steel_electric_heater",
        "engine_host": "cruciblecraft:stainless_steel_electric_engine",
        "motor_host": "cruciblecraft:stainless_steel_electric_motor",
        "magnet_host": "cruciblecraft:stainless_steel_electromagnet",
        "laser_host": "cruciblecraft:stainless_steel_electric_co2_laser",
        "dynamo_host": "cruciblecraft:stainless_steel_dynamo",
        "magnet_live": False,
        "laser_live": False,
    },
    {
        "tier": 4,
        "material": "enderium_base",
        "lang_en": "Enderium Base",
        "lang_zh": "末影基",
        "nbt_in_rf": 8192,
        "nbt_out_gu": 1024,
        "nbt_in_ru": 2048,
        "nbt_out_rf": 5632,
        "heater_host": "cruciblecraft:chromium_electric_heater",
        "engine_host": "cruciblecraft:chromium_electric_engine",
        "motor_host": "cruciblecraft:chromium_electric_motor",
        "magnet_host": "cruciblecraft:chromium_electromagnet",
        "laser_host": "cruciblecraft:chromium_electric_co2_laser",
        "dynamo_host": "cruciblecraft:chromium_dynamo",
        "magnet_live": False,
        "laser_live": False,
    },
    {
        "tier": 5,
        "material": "enderium",
        "lang_en": "Enderium",
        "lang_zh": "末影",
        "nbt_in_rf": 32768,
        "nbt_out_gu": 4096,
        "nbt_in_ru": 8192,
        "nbt_out_rf": 22528,
        "heater_host": "cruciblecraft:titanium_electric_heater",
        "engine_host": "cruciblecraft:titanium_electric_engine",
        "motor_host": "cruciblecraft:titanium_electric_motor",
        "magnet_host": "cruciblecraft:titanium_electromagnet",
        "laser_host": "cruciblecraft:titanium_electric_co2_laser",
        "dynamo_host": "cruciblecraft:titanium_dynamo",
        "magnet_live": False,
        "laser_live": False,
    },
)
KINDS = (
    {
        "kind": "heater",
        "gt6_class": "MultiTileEntityHeaterFlux",
        "source_base": 11001,
        "source_line_base": 824,
        "lang_en": "Flux Heater",
        "lang_zh": "通量加热器",
        "texture_folder": "flux_heater",
        "host_key": "heater_host",
        "accepts": "RF",
        "emits": "HU",
        "input_key": "nbt_in_rf",
        "output_key": "nbt_out_gu",
        "pattern": ["SSS", "SMS", "SSS"],
        "part": "long_rod",
        "part_letter": "S",
        "gt6_part": "OP.stickLong",
        "gt6_host": "aRegistry.getItem(1000{tier})",
        "recipe_live": True,
    },
    {
        "kind": "engine",
        "gt6_class": "MultiTileEntityEngineFlux",
        "source_base": 11011,
        "source_line_base": 840,
        "lang_en": "Flux Engine",
        "lang_zh": "通量引擎",
        "texture_folder": "flux_engine",
        "host_key": "engine_host",
        "accepts": "RF",
        "emits": "KU",
        "input_key": "nbt_in_rf",
        "output_key": "nbt_out_gu",
        "pattern": ["G", "M", "G"],
        "part": "gear",
        "part_letter": "G",
        "gt6_part": "OP.gearGt",
        "gt6_host": "aRegistry.getItem(1001{tier})",
        "recipe_live": True,
    },
    {
        "kind": "motor",
        "gt6_class": "MultiTileEntityMotorFlux",
        "source_base": 11021,
        "source_line_base": 856,
        "lang_en": "Flux Motor",
        "lang_zh": "通量电机",
        "texture_folder": "flux_motor",
        "host_key": "motor_host",
        "accepts": "RF",
        "emits": "RU",
        "input_key": "nbt_in_rf",
        "output_key": "nbt_out_gu",
        "pattern": ["GMG"],
        "part": "gear",
        "part_letter": "G",
        "gt6_part": "OP.gearGt",
        "gt6_host": "aRegistry.getItem(1002{tier})",
        "recipe_live": True,
    },
    {
        "kind": "magnet",
        "gt6_class": "MultiTileEntityMagnetFlux",
        "source_base": 11031,
        "source_line_base": 872,
        "lang_en": "Flux Magnet",
        "lang_zh": "通量磁铁",
        "texture_folder": "flux_magnet",
        "host_key": "magnet_host",
        "accepts": "RF",
        "emits": "MU",
        "input_key": "nbt_in_rf",
        "output_key": "nbt_out_gu",
        "pattern": ["SSS", "SMS", "SSS"],
        "part": "long_rod",
        "part_letter": "S",
        "gt6_part": "OP.stickLong",
        "gt6_host": "aRegistry.getItem(1003{tier})",
        "recipe_live": False,
        "blocked_reason": (
            "GT6 wraps Electromagnet 10031-10035; CC has no live electromagnet."
        ),
    },
    {
        "kind": "laser",
        "gt6_class": "MultiTileEntityLaserFlux",
        "source_base": 11101,
        "source_line_base": 937,
        "lang_en": "Flux Laser",
        "lang_zh": "通量激光",
        "texture_folder": "flux_laser",
        "host_key": "laser_host",
        "accepts": "RF",
        "emits": "LU",
        "input_key": "nbt_in_rf",
        "output_key": "nbt_out_gu",
        "pattern": ["PPP", "PMP", "PPP"],
        "part": "plate",
        "part_letter": "P",
        "gt6_part": "OP.plate",
        "gt6_host": "aRegistry.getItem(1010{tier})",
        "recipe_live": False,
        "blocked_reason": (
            "GT6 wraps Electric CO2 Laser 10101-10105; CC has no live electric laser."
        ),
    },
    {
        "kind": "dynamo",
        "gt6_class": "MultiTileEntityDynamoFlux",
        "source_base": 11111,
        "source_line_base": 953,
        "lang_en": "Flux Dynamo",
        "lang_zh": "通量发电机",
        "texture_folder": "flux_dynamo",
        "host_key": "dynamo_host",
        "accepts": "RU",
        "emits": "RF",
        "input_key": "nbt_in_ru",
        "output_key": "nbt_out_rf",
        "pattern": ["SMS", "SGS"],
        "part": "long_rod",
        "part_letter": "S",
        "extra_part": "gear",
        "extra_letter": "G",
        "gt6_part": "OP.stickLong",
        "gt6_extra": "OP.gearGt",
        "gt6_host": "aRegistry.getItem(1011{tier})",
        "recipe_live": True,
    },
)
ART = (
    {
        "folder": "flux_heater",
        "gt6": "heaters/heat_flux",
        "active": "overlay_active",
    },
    {
        "folder": "flux_engine",
        "gt6": "engines/kinetic_flux",
        "active": "overlay",
    },
    {
        "folder": "flux_motor",
        "gt6": "motors/rotation_flux",
        "active": "overlay_active_rs",
    },
    {
        "folder": "flux_magnet",
        "gt6": "magnets/magnet_flux",
        "active": "overlay_active",
    },
    {
        "folder": "flux_laser",
        "gt6": "lasers/laser_flux",
        "active": "overlay_active",
    },
    {
        "folder": "flux_dynamo",
        "gt6": "dynamos/flux_rotation",
        "active": "overlay_active",
    },
)


def dump(path: Path, payload: object) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(
        json.dumps(payload, indent=2, ensure_ascii=False) + "\n",
        encoding="utf-8",
    )


def copy_png(src: Path, dest: Path) -> None:
    if not src.is_file():
        raise SystemExit(f"missing gregtech6_w texture {src}")
    dest.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(src, dest)


def unique_active_wave() -> str | None:
    path = (
        ROOT
        / "tools"
        / "capabilities"
        / "energy"
        / "flux-converters"
        / "capability.json"
    )
    if not path.is_file():
        return None
    document = io.load_json(path)
    return WAVE_SLUG if document.get("workflow") == "active" else None


def machine_id(kind: str, material: str) -> str:
    return f"cruciblecraft:flux_{kind}_{material}"


def machine_row(kind: dict, material: dict) -> dict:
    host = material[kind["host_key"]]
    keys = {
        "M": {"item": host},
        kind["part_letter"]: {
            "prefix": kind["part"],
            "material": material["material"],
        },
    }
    if kind.get("extra_part"):
        keys[kind["extra_letter"]] = {
            "prefix": kind["extra_part"],
            "material": material["material"],
        }
    return {
        "id": machine_id(kind["kind"], material["material"]),
        "source_id": kind["source_base"] + material["tier"] - 1,
        "source_line": kind["source_line_base"] + material["tier"] - 1,
        "gt6_class": kind["gt6_class"],
        "kind": kind["kind"],
        "material": material["material"],
        "nbt_input": material[kind["input_key"]],
        "nbt_output": material[kind["output_key"]],
        "accepts": kind["accepts"],
        "emits": kind["emits"],
        "hardness": 4.0,
        "resistance": 4.0,
        "lang_en": f"{kind['lang_en']} ({material['lang_en']})",
        "lang_zh": f"{kind['lang_zh']}（{material['lang_zh']}）",
        "texture_folder": kind["texture_folder"],
        "host_id": host,
        "recipe_live": kind["recipe_live"],
        "recipe": {
            "pattern": list(kind["pattern"]),
            "keys": keys,
            "catalysts": [],
        },
    }


def all_machines() -> list[dict]:
    rows = []
    for kind in KINDS:
        for material in MATERIALS:
            rows.append(machine_row(kind, material))
    return rows


def write_catalog() -> None:
    dump(
        SRC / "data" / "cruciblecraft" / "flux_converters.json",
        {
            "schema_version": 1,
            "source_revision": REVISION,
            "source_path": (
                "gt6_code/gregtech6/src/main/java/gregtech/loaders/b/"
                "Loader_MultiTileEntities.java"
            ),
            "rf_per_eu": 4,
            "machines": all_machines(),
        },
    )


def write_art() -> None:
    imports = []
    faces = ("front", "back", "side")
    for family in ART:
        for folder, gt6_folder in (
            ("colored", "colored"),
            ("overlay", "overlay"),
            ("overlay_active", family["active"]),
        ):
            for face in faces:
                src = (
                    GT6_ART
                    / "blocks"
                    / "machines"
                    / family["gt6"]
                    / gt6_folder
                    / f"{face}.png"
                )
                dest = (
                    SRC
                    / "assets"
                    / "cruciblecraft"
                    / "textures"
                    / "block"
                    / "machine"
                    / family["folder"]
                    / folder
                    / f"{face}.png"
                )
                copy_png(src, dest)
                imports.append(
                    {
                        "source": "gt6_referencable_port_code/gregtech6_w",
                        "gt6_source": (
                            "assets/gregtech/textures/blocks/machines/"
                            f"{family['gt6']}/{gt6_folder}/{face}.png"
                        ),
                        "destination": (
                            "assets/cruciblecraft/textures/block/machine/"
                            f"{family['folder']}/{folder}/{face}.png"
                        ),
                        "note": (
                            f"GT6 {family['gt6']} {gt6_folder}/{face} "
                            f"as CC {folder}/{face}."
                        ),
                    }
                )
    dump(
        SRC / "assets" / "cruciblecraft" / "gt6_flux_converters_art_manifest.json",
        {
            "source": "gt6_referencable_port_code/gregtech6_w",
            "source_revision": REVISION,
            "source_present": True,
            "imports": imports,
        },
    )


def slot(gt6: str, item: str, status: str, note: str) -> dict:
    return {"gt6": gt6, "cc": item, "status": status, "note": note}


def write_d0() -> None:
    rows = []
    for kind in KINDS:
        for material in MATERIALS:
            live = kind["recipe_live"]
            host = material[kind["host_key"]]
            host_status = "exact" if live else "explicitly_blocked"
            host_note = (
                "Wraps the matching Electric_T host already live in CC."
                if live
                else kind["blocked_reason"]
            )
            slots = {
                "M": slot(
                    kind["gt6_host"].format(tier=material["tier"]),
                    host,
                    host_status,
                    host_note,
                ),
                kind["part_letter"]: slot(
                    f"{kind['gt6_part']}(MT.DATA.Flux_T[{material['tier']}])",
                    f"cruciblecraft:{material['material']}/{kind['part']}",
                    "exact",
                    "Flux_T[1]=Pb through Flux_T[5]=Enderium.",
                ),
            }
            if kind.get("extra_part"):
                slots[kind["extra_letter"]] = slot(
                    f"{kind['gt6_extra']}(MT.DATA.Flux_T[{material['tier']}])",
                    f"cruciblecraft:{material['material']}/{kind['extra_part']}",
                    "exact",
                    "Flux_T[1]=Pb through Flux_T[5]=Enderium.",
                )
            rows.append(
                {
                    "source_id": kind["source_base"] + material["tier"] - 1,
                    "cc_id": machine_id(kind["kind"], material["material"]),
                    "status": "exact" if live else "explicitly_blocked",
                    "slots": slots,
                }
            )
    dump(
        ROOT / "tools" / "waves" / "runtime" / "flux-converters" / "d0_obtain_matrix.json",
        {
            "schema_version": 1,
            "source_revision": REVISION,
            "source_path": "Loader_MultiTileEntities.java:823-957",
            "rf_translation": (
                "NeoForge FE 1:1 with GT6 RF amount; no EnergyType.RF. "
                "GT6 receiveEnergy injects RF size=1 amount=FE."
            ),
            "rows": rows,
        },
    )


def write_wave() -> None:
    wave = ROOT / "tools" / "waves" / "runtime" / "flux-converters"
    wave.mkdir(parents=True, exist_ok=True)
    active = unique_active_wave()
    dump(
        wave / "wave.json",
        {
            "cohort": "flux-converters",
            "depends_on": [],
            "generated_by": "runtime/flux-converters implementation",
            "owns_families": 0,
            "program": WAVE_SLUG,
            "schema_version": 1,
            "wave_slug": WAVE_SLUG,
        },
    )
    dump(
        wave / "topology.json",
        {
            "append_only": False,
            "complete_family_count": 0,
            "generated_by": "runtime/flux-converters implementation",
            "next_unassigned": True,
            "remaining_recipe_gap": 10,
            "schema_version": 1,
            "source_revision": REVISION,
            "status": "WAVE_READY",
            "unique_active_wave": active,
            "wave_slug": WAVE_SLUG,
        },
    )
    dump(
        wave / "census_delta.json",
        {
            "complete_family_count": 0,
            "completion_delta": 0,
            "generated_by": "runtime/flux-converters implementation",
            "leftover_later_count": 0,
            "partial_family_count": 0,
            "remaining_recipe_gap": 10,
            "schema_version": 1,
            "source_revision": REVISION,
            "status": "CENSUS_DELTA_READY",
            "wave_slug": WAVE_SLUG,
            "work_set": {"family_count": 0, "source_rows": 30},
        },
    )
    dump(
        wave / "readiness.json",
        {
            "evidence": {
                "acquisition": "survival_recipes",
                "completion_delta": 0,
                "flux_identity_count": 30,
                "generated_recipe_count": 20,
                "blocked_recipe_count": 10,
                "leftover_later_count": 0,
                "owns_families": 0,
                "flux_converters_status": "runtime_ready",
            },
            "generated_by": "runtime/flux-converters implementation",
            "generated_recipe_count": 20,
            "next_unassigned": True,
            "owns_families": 0,
            "production_lock": None,
            "schema_version": 1,
            "source_revision": REVISION,
            "status": "FLUX_CONVERTERS_READY",
            "unique_active_wave": active,
            "wave_slug": WAVE_SLUG,
        },
    )
    empty_src = (
        SRC
        / "data"
        / "cruciblecraft_wave_runtime_cooler"
        / "structure"
        / "empty.nbt"
    )
    ns = SRC / "data" / "cruciblecraft_wave_runtime_flux_converters"
    for rel in ("structure/empty.nbt", "gametest/structure/empty.nbt"):
        dest = ns / rel
        dest.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(empty_src, dest)


def write_capability() -> None:
    ids = [row["id"] for row in all_machines()]
    cap = ROOT / "tools" / "capabilities" / "energy" / "flux-converters"
    cap.mkdir(parents=True, exist_ok=True)
    dump(
        cap / "capability.json",
        {
            "schema_version": 2,
            "slug": CAPABILITY_SLUG,
            "title": "Flux FE to GU Converters",
            "maturity": "runtime_ready",
            "workflow": "paused",
            "owned_paths": [
                "docs/history/card-plans/prep/通量转换器详细计划.md",
                "src/main/java/com/masson/cruciblecraft/energy/flux/**",
                "src/test/java/com/masson/cruciblecraft/gametest/EnergyFluxConvertersGameTests.java",
                "src/test/java/com/masson/cruciblecraft/energy/flux/**",
                "src/main/resources/data/cruciblecraft_wave_runtime_flux_converters/**",
                "src/main/resources/data/cruciblecraft/flux_converters.json",
                "src/main/resources/assets/cruciblecraft/gt6_flux_converters_art_manifest.json",
                "src/main/resources/assets/cruciblecraft/textures/block/machine/flux_heater/**",
                "src/main/resources/assets/cruciblecraft/textures/block/machine/flux_engine/**",
                "src/main/resources/assets/cruciblecraft/textures/block/machine/flux_motor/**",
                "src/main/resources/assets/cruciblecraft/textures/block/machine/flux_magnet/**",
                "src/main/resources/assets/cruciblecraft/textures/block/machine/flux_laser/**",
                "src/main/resources/assets/cruciblecraft/textures/block/machine/flux_dynamo/**",
                "tools/capabilities/energy/flux-converters/**",
                "tools/waves/runtime/flux-converters/**",
                "tools/tests/test_energy_flux_converters.py",
                "tools/emit_flux_converters.py",
            ],
            "depends_on": [],
            "profiles": ["capability-runtime"],
            "wave_slug": WAVE_SLUG,
            "legacy_readiness": "tools/waves/runtime/flux-converters/readiness.json",
            "player_signoff": None,
            "required_test_ids": [
                "playerSurfaceIsRegistered",
                "leadHeaterConvertsFeToHu",
                "leadEngineConvertsFeToKu",
                "leadMotorConvertsFeToRu",
                "leadMagnetConvertsFeToMu",
                "leadLaserConvertsFeToLu",
                "leadDynamoConvertsRuToFe",
                "underpoweredFeIsWastedWithoutEmit",
                "reloadPreservesBufferAndMode",
                "representativeRecipesAreSurvivalCraftable",
            ],
            "identity_disposition": [
                {
                    "semantic_key": "energy:flux-converter",
                    "disposition": "new_distinct",
                    "runtime_ids": ids,
                    "reason": (
                        "GT6 Flux_T[1..5] wrappers 11001-11005 / 11011-11015 / "
                        "11021-11025 / 11031-11035 / 11101-11105 / 11111-11115. "
                        "RF/FE in to HU/KU/RU/MU/LU, or RU in to RF/FE out. "
                        "Flux RF has no CC EnergyType; FE is the platform translation."
                    ),
                }
            ],
            "note": (
                "Thirty GT6 flux converters. Paused so machines/distillation-tower "
                "can occupy unique-active. Magnet and laser recipes stay "
                "explicitly_blocked until electromagnets and electric CO2 lasers "
                "are live. survival_access is unreviewed."
            ),
            "survival_access": "unreviewed",
        },
    )


def main() -> None:
    write_catalog()
    write_art()
    write_d0()
    write_wave()
    write_capability()


if __name__ == "__main__":
    main()
