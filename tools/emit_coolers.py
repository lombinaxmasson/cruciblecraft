#!/usr/bin/env python3
"""Electric/flux cooler unique-active: D0 matrix, catalog, art, wave."""
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
CAPABILITY_SLUG = "energy/cooler"
WAVE_SLUG = "runtime/cooler"
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
PLATE_PREFIX = {
    1: "plate",
    2: "double_plate",
    3: "triple_plate",
    4: "quadruple_plate",
    5: "quintuple_plate",
}
ELECTRIC = (
    {
        "id": "cruciblecraft:thermoelectric_cooler_lv",
        "source_id": 10161,
        "source_line": 985,
        "voltage": "lv",
        "material": "steel_galvanized",
        "cable_material": "tin",
        "cable_family": None,
        "plate_tier": 1,
        "nbt_input": 32,
        "nbt_output": 8,
        "lang_en": "Thermoelectric Cooler (LV)",
        "lang_zh": "热电冷却器（低压）",
    },
    {
        "id": "cruciblecraft:thermoelectric_cooler_mv",
        "source_id": 10162,
        "source_line": 986,
        "voltage": "mv",
        "material": "aluminium",
        "cable_material": "copper",
        "cable_family": "ANY.Cu",
        "plate_tier": 2,
        "nbt_input": 128,
        "nbt_output": 32,
        "lang_en": "Thermoelectric Cooler (MV)",
        "lang_zh": "热电冷却器（中压）",
    },
    {
        "id": "cruciblecraft:thermoelectric_cooler_hv",
        "source_id": 10163,
        "source_line": 987,
        "voltage": "hv",
        "material": "stainless_steel",
        "cable_material": "gold",
        "cable_family": None,
        "plate_tier": 3,
        "nbt_input": 512,
        "nbt_output": 128,
        "lang_en": "Thermoelectric Cooler (HV)",
        "lang_zh": "热电冷却器（高压）",
    },
    {
        "id": "cruciblecraft:thermoelectric_cooler_ev",
        "source_id": 10164,
        "source_line": 988,
        "voltage": "ev",
        "material": "chromium",
        "cable_material": "aluminium",
        "cable_family": None,
        "plate_tier": 4,
        "nbt_input": 2048,
        "nbt_output": 512,
        "lang_en": "Thermoelectric Cooler (EV)",
        "lang_zh": "热电冷却器（超高压）",
    },
    {
        "id": "cruciblecraft:thermoelectric_cooler_iv",
        "source_id": 10165,
        "source_line": 989,
        "voltage": "iv",
        "material": "titanium",
        "cable_material": "platinum",
        "cable_family": None,
        "plate_tier": 5,
        "nbt_input": 8192,
        "nbt_output": 2048,
        "lang_en": "Thermoelectric Cooler (IV)",
        "lang_zh": "热电冷却器（极高压）",
    },
)
FLUX = (
    {
        "id": "cruciblecraft:thermofluxic_cooler_lead",
        "source_id": 11161,
        "source_line": 992,
        "material": "lead",
        "host_id": "cruciblecraft:thermoelectric_cooler_lv",
        "nbt_input": 128,
        "nbt_output": 8,
        "lang_en": "Thermofluxic Cooler (Lead)",
        "lang_zh": "热通量冷却器（铅）",
    },
    {
        "id": "cruciblecraft:thermofluxic_cooler_invar",
        "source_id": 11162,
        "source_line": 993,
        "material": "invar",
        "host_id": "cruciblecraft:thermoelectric_cooler_mv",
        "nbt_input": 512,
        "nbt_output": 32,
        "lang_en": "Thermofluxic Cooler (Invar)",
        "lang_zh": "热通量冷却器（殷钢）",
    },
    {
        "id": "cruciblecraft:thermofluxic_cooler_electrum",
        "source_id": 11163,
        "source_line": 994,
        "material": "electrum",
        "host_id": "cruciblecraft:thermoelectric_cooler_hv",
        "nbt_input": 2048,
        "nbt_output": 128,
        "lang_en": "Thermofluxic Cooler (Electrum)",
        "lang_zh": "热通量冷却器（琥珀金）",
    },
    {
        "id": "cruciblecraft:thermofluxic_cooler_enderium_base",
        "source_id": 11164,
        "source_line": 995,
        "material": "enderium_base",
        "host_id": "cruciblecraft:thermoelectric_cooler_ev",
        "nbt_input": 8192,
        "nbt_output": 512,
        "lang_en": "Thermofluxic Cooler (Enderium Base)",
        "lang_zh": "热通量冷却器（末影基）",
    },
    {
        "id": "cruciblecraft:thermofluxic_cooler_enderium",
        "source_id": 11165,
        "source_line": 996,
        "material": "enderium",
        "host_id": "cruciblecraft:thermoelectric_cooler_iv",
        "nbt_input": 32768,
        "nbt_output": 2048,
        "lang_en": "Thermofluxic Cooler (Enderium)",
        "lang_zh": "热通量冷却器（末影）",
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
    path = ROOT / "tools" / "capabilities" / "energy" / "cooler" / "capability.json"
    if not path.is_file():
        return WAVE_SLUG
    document = io.load_json(path)
    return WAVE_SLUG if document.get("workflow") == "active" else None


def electric_row(row: dict) -> dict:
    plate = PLATE_PREFIX[row["plate_tier"]]
    cable = {
        "prefix": "cable",
        "material": row["cable_material"],
    }
    if row["cable_family"]:
        cable["family"] = row["cable_family"]
    copper = {
        "prefix": plate,
        "material": "copper",
        "family": "ANY.Cu",
    }
    return {
        "id": row["id"],
        "source_id": row["source_id"],
        "source_line": row["source_line"],
        "gt6_class": "MultiTileEntityCoolerElectric",
        "kind": "electric",
        "voltage": row["voltage"],
        "material": row["material"],
        "nbt_input": row["nbt_input"],
        "nbt_output": row["nbt_output"],
        "hardness": 4.0,
        "resistance": 4.0,
        "lang_en": row["lang_en"],
        "lang_zh": row["lang_zh"],
        "texture_folder": "cryo_electric",
        "host_id": None,
        "recipe": {
            "pattern": ["WPw", "CMC", "xPW"],
            "keys": {
                "M": {
                    "prefix": "machine_casing",
                    "material": row["material"],
                },
                "W": cable,
                "P": {
                    "prefix": plate,
                    "material": "silicon",
                },
                "C": copper,
            },
            "catalysts": ["w", "x"],
        },
    }


def flux_row(row: dict) -> dict:
    return {
        "id": row["id"],
        "source_id": row["source_id"],
        "source_line": row["source_line"],
        "gt6_class": "MultiTileEntityCoolerFlux",
        "kind": "flux",
        "voltage": None,
        "material": row["material"],
        "nbt_input": row["nbt_input"],
        "nbt_output": row["nbt_output"],
        "hardness": 4.0,
        "resistance": 4.0,
        "lang_en": row["lang_en"],
        "lang_zh": row["lang_zh"],
        "texture_folder": "cryo_flux",
        "host_id": row["host_id"],
        "recipe": {
            "pattern": ["PSP", "PMP", "PSP"],
            "keys": {
                "M": {"item": row["host_id"]},
                "P": {
                    "prefix": "plate",
                    "material": row["material"],
                },
                "S": {
                    "prefix": "long_rod",
                    "material": row["material"],
                },
            },
            "catalysts": [],
        },
    }


def write_catalog() -> None:
    machines = [electric_row(row) for row in ELECTRIC]
    machines.extend(flux_row(row) for row in FLUX)
    dump(
        SRC / "data" / "cruciblecraft" / "coolers.json",
        {
            "schema_version": 1,
            "source_revision": REVISION,
            "source_path": (
                "gt6_code/gregtech6/src/main/java/gregtech/loaders/b/"
                "Loader_MultiTileEntities.java"
            ),
            "machines": machines,
        },
    )


def slot(gt6: str, item: str, note: str) -> dict:
    return {
        "gt6": gt6,
        "cc": item,
        "status": "exact",
        "note": note,
    }


def write_d0() -> None:
    rows = []
    for row in ELECTRIC:
        plate = PLATE_PREFIX[row["plate_tier"]]
        gt_plate = {
            1: "OP.plate(MT.Si)",
            2: "OP.plateDouble(MT.Si)",
            3: "OP.plateTriple(MT.Si)",
            4: "OP.plateQuadruple(MT.Si)",
            5: "OP.plateQuintuple(MT.Si)",
        }[row["plate_tier"]]
        gt_copper = {
            1: "OP.plate(ANY.Cu)",
            2: "OP.plateDouble(ANY.Cu)",
            3: "OP.plateTriple(ANY.Cu)",
            4: "OP.plateQuadruple(ANY.Cu)",
            5: "OP.plateQuintuple(ANY.Cu)",
        }[row["plate_tier"]]
        cable_gt = f"MT.DATA.CABLES_01[{row['source_id'] - 10160}]"
        rows.append(
            {
                "source_id": row["source_id"],
                "cc_id": row["id"],
                "status": "exact",
                "slots": {
                    "M": slot(
                        f"OP.casingMachine(MT.DATA.Electric_T[{row['source_id'] - 10160}])",
                        f"cruciblecraft:{row['material']}/machine_casing",
                        "Electric_T[1]=SteelGalvanized LV through Electric_T[5]=Ti IV.",
                    ),
                    "W": slot(
                        cable_gt,
                        f"cruciblecraft:{row['cable_material']}/cable",
                        "CABLES_01[2] ANY.Cu lands on copper.",
                    ),
                    "P": slot(
                        gt_plate,
                        f"cruciblecraft:silicon/{plate}",
                        "Silicon multiplates are live; do not substitute gem plates.",
                    ),
                    "C": slot(
                        gt_copper,
                        f"cruciblecraft:copper/{plate}",
                        "ANY.Cu family member; not an unrelated metal.",
                    ),
                    "w": slot(
                        "wrench catalyst",
                        "cruciblecraft:material_wrench",
                        "GT6 CR lowercase w is OreDictToolNames.wrench.",
                    ),
                    "x": slot(
                        "wirecutter catalyst",
                        "cruciblecraft:material_wire_cutter",
                        "GT6 CR lowercase x is OreDictToolNames.wirecutter.",
                    ),
                },
            }
        )
    for row in FLUX:
        rows.append(
            {
                "source_id": row["source_id"],
                "cc_id": row["id"],
                "status": "exact",
                "slots": {
                    "M": slot(
                        f"aRegistry.getItem({row['source_id'] - 1000})",
                        row["host_id"],
                        "Flux cooler wraps the matching thermoelectric cooler.",
                    ),
                    "P": slot(
                        f"OP.plate(MT.DATA.Flux_T[{row['source_id'] - 11160}])",
                        f"cruciblecraft:{row['material']}/plate",
                        "Flux_T[1]=Pb through Flux_T[5]=Enderium.",
                    ),
                    "S": slot(
                        f"OP.stickLong(MT.DATA.Flux_T[{row['source_id'] - 11160}])",
                        f"cruciblecraft:{row['material']}/long_rod",
                        "Long rod, not a short rod stand-in.",
                    ),
                },
            }
        )
    dump(
        ROOT / "tools" / "waves" / "runtime" / "cooler" / "d0_obtain_matrix.json",
        {
            "schema_version": 1,
            "source_revision": REVISION,
            "source_path": "Loader_MultiTileEntities.java:984-996",
            "rf_translation": "NeoForge FE 1:1 with GT6 RF amount; no EnergyType.RF.",
            "rows": rows,
            "exact_count": 10,
            "blocked_count": 0,
        },
    )


def write_art() -> None:
    imports = []
    for family in ("cryo_electric", "cryo_flux"):
        for folder in ("colored", "overlay", "overlay_active"):
            for face in ("front", "back", "side"):
                src = (
                    GT6_ART
                    / "blocks"
                    / "machines"
                    / "cooler"
                    / family
                    / folder
                    / f"{face}.png"
                )
                dest = (
                    SRC
                    / "assets"
                    / "cruciblecraft"
                    / "textures"
                    / "block"
                    / "machine"
                    / "cooler"
                    / family
                    / folder
                    / f"{face}.png"
                )
                copy_png(src, dest)
                imports.append(
                    {
                        "source": "gt6_referencable_port_code/gregtech6_w",
                        "gt6_source": (
                            "assets/gregtech/textures/blocks/machines/cooler/"
                            f"{family}/{folder}/{face}.png"
                        ),
                        "destination": (
                            "assets/cruciblecraft/textures/block/machine/cooler/"
                            f"{family}/{folder}/{face}.png"
                        ),
                        "note": f"GT6 {family} cooler {folder}/{face}.",
                    }
                )
    dump(
        SRC / "assets" / "cruciblecraft" / "gt6_coolers_art_manifest.json",
        {
            "source": "gt6_referencable_port_code/gregtech6_w",
            "source_revision": REVISION,
            "source_present": True,
            "imports": imports,
        },
    )


def write_wave() -> None:
    wave = ROOT / "tools" / "waves" / "runtime" / "cooler"
    wave.mkdir(parents=True, exist_ok=True)
    active = unique_active_wave()
    dump(
        wave / "wave.json",
        {
            "cohort": "cooler",
            "depends_on": [],
            "generated_by": "runtime/cooler implementation",
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
            "generated_by": "runtime/cooler implementation",
            "next_unassigned": True,
            "remaining_recipe_gap": 0,
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
            "generated_by": "runtime/cooler implementation",
            "leftover_later_count": 0,
            "partial_family_count": 0,
            "remaining_recipe_gap": 0,
            "schema_version": 1,
            "source_revision": REVISION,
            "status": "CENSUS_DELTA_READY",
            "wave_slug": WAVE_SLUG,
            "work_set": {"family_count": 0, "source_rows": 10},
        },
    )
    dump(
        wave / "readiness.json",
        {
            "evidence": {
                "acquisition": "survival_recipes",
                "completion_delta": 0,
                "cooler_identity_count": 10,
                "generated_recipe_count": 10,
                "leftover_later_count": 0,
                "owns_families": 0,
                "coolers_status": "runtime_ready",
            },
            "generated_by": "runtime/cooler implementation",
            "generated_recipe_count": 10,
            "next_unassigned": True,
            "owns_families": 0,
            "production_lock": None,
            "schema_version": 1,
            "source_revision": REVISION,
            "status": "COOLERS_READY",
            "unique_active_wave": active,
            "wave_slug": WAVE_SLUG,
        },
    )
    empty_src = (
        SRC
        / "data"
        / "cruciblecraft_wave_runtime_heat_exchangers"
        / "structure"
        / "empty.nbt"
    )
    ns = SRC / "data" / "cruciblecraft_wave_runtime_cooler"
    for rel in ("structure/empty.nbt", "gametest/structure/empty.nbt"):
        dest = ns / rel
        dest.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(empty_src, dest)


def write_capability() -> None:
    ids = [row["id"] for row in ELECTRIC] + [row["id"] for row in FLUX]
    cap = ROOT / "tools" / "capabilities" / "energy" / "cooler"
    cap.mkdir(parents=True, exist_ok=True)
    dump(
        cap / "capability.json",
        {
            "schema_version": 2,
            "slug": CAPABILITY_SLUG,
            "title": "Electric and Flux Coolers",
            "maturity": "runtime_ready",
            "workflow": "paused",
            "owned_paths": [
                "docs/history/card-plans/prep/冷却器详细计划.md",
                "src/main/java/com/masson/cruciblecraft/energy/cooler/**",
                "src/test/java/com/masson/cruciblecraft/gametest/EnergyCoolersGameTests.java",
                "src/test/java/com/masson/cruciblecraft/energy/cooler/**",
                "src/main/resources/data/cruciblecraft_wave_runtime_cooler/**",
                "src/main/resources/data/cruciblecraft/coolers.json",
                "src/main/resources/assets/cruciblecraft/gt6_coolers_art_manifest.json",
                "src/main/resources/assets/cruciblecraft/textures/block/machine/cooler/**",
                "tools/capabilities/energy/cooler/**",
                "tools/waves/runtime/cooler/**",
                "tools/tests/test_energy_coolers.py",
                "tools/emit_coolers.py",
            ],
            "depends_on": [],
            "profiles": ["capability-runtime"],
            "wave_slug": WAVE_SLUG,
            "legacy_readiness": "tools/waves/runtime/cooler/readiness.json",
            "player_signoff": None,
            "required_test_ids": [
                "playerSurfaceIsRegistered",
                "lvElectricConvertsEuToCuAndHu",
                "underpoweredEuIsWastedWithoutEmit",
                "fluxLeadConvertsFeToCuAndHu",
                "screwdriverModeCapsOutput",
                "reloadPreservesBufferAndMode",
                "representativeRecipesAreSurvivalCraftable",
            ],
            "identity_disposition": [
                {
                    "semantic_key": "thermal:cooler",
                    "disposition": "new_distinct",
                    "runtime_ids": ids,
                    "reason": (
                        "GT6 MultiTileEntityCoolerElectric 10161-10165 and "
                        "MultiTileEntityCoolerFlux 11161-11165. EU or RF in, "
                        "CU front + HU back, NBT_WASTE_ENERGY. Flux input is "
                        "NeoForge FE amount-translated from GT6 RF; not EnergyType.RF."
                    ),
                }
            ],
            "note": (
                "Ten GT6 coolers. Paused so machines/distillation-tower can "
                "occupy unique-active. Flux RF has no CC EnergyType; FE is the "
                "platform translation. survival_access is unreviewed."
            ),
            "survival_access": "unreviewed",
        },
    )


def write_surfaces() -> None:
    path = SRC.parent.parent / "main" / "resources" / "cruciblecraft" / "player_complete_surfaces.json"
    path = ROOT / "src" / "main" / "resources" / "cruciblecraft" / "player_complete_surfaces.json"
    document = io.load_json(path)
    document["surfaces"]["energy/cooler"] = {
        "registry_ids": [row["id"] for row in ELECTRIC] + [row["id"] for row in FLUX],
        "definition_ids": [],
    }
    io.write_stable(path, document)


def main() -> None:
    write_catalog()
    write_d0()
    write_art()
    write_capability()
    write_wave()
    write_surfaces()


if __name__ == "__main__":
    main()
