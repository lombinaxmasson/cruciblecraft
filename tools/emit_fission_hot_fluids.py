#!/usr/bin/env python3
"""Emit fission hot-fluid wave artifacts, art, and container-gate rows."""
from __future__ import annotations

import json
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
REV = "3703e40308c8c030763fd6297dea8b210d2a77b1"
WAVE = ROOT / "tools" / "waves" / "runtime" / "fission-hot-fluids"
SRC = ROOT / "src" / "main" / "resources"
GT6_FLUIDS = (
    ROOT
    / "gt6_referencable_port_code"
    / "gregtech6_w"
    / "src"
    / "main"
    / "resources"
    / "assets"
    / "gregtech"
    / "textures"
    / "blocks"
    / "fluids"
)
CC_ASSETS = SRC / "assets" / "cruciblecraft"
CORE_1X1 = (
    "gt6_code/gregtech6/src/main/java/gregtech/tileentity/energy/"
    "reactors/MultiTileEntityReactorCore1x1.java"
)
CORE_2X2 = (
    "gt6_code/gregtech6/src/main/java/gregtech/tileentity/energy/"
    "reactors/MultiTileEntityReactorCore2x2.java"
)
CORE_BASE = (
    "gt6_code/gregtech6/src/main/java/gregtech/tileentity/energy/"
    "reactors/MultiTileEntityReactorCore.java"
)
FL_JAVA = "gt6_code/gregtech6/src/main/java/gregapi/data/FL.java"
CS_JAVA = "gt6_code/gregtech6/src/main/java/gregapi/data/CS.java"
LOADER_FLUIDS = (
    "gt6_code/gregtech6/src/main/java/gregtech/loaders/a/Loader_Fluids.java"
)
ROD_NUCLEAR = (
    "gt6_code/gregtech6/src/main/java/gregtech/tileentity/energy/"
    "reactors/MultiTileEntityReactorRodNuclear.java"
)

HOT_FLUIDS = (
    {
        "id": "hot_molten_tin",
        "gt6_fluid": "hotmoltentin",
        "english": "Hot Molten Tin",
        "chinese": "热熔融锡",
        "source_material": "tin",
        "state": "liquid",
        "cell_kind": "fluid",
        "temperature_kelvin": 2800,
        "density": 1000,
        "viscosity": 1000,
        "color": "#D8E0E8",
        "loader_line": 88,
    },
    {
        "id": "hot_molten_sodium",
        "gt6_fluid": "hotmoltensodium",
        "english": "Hot Molten Sodium",
        "chinese": "热熔融钠",
        "source_material": "sodium",
        "state": "liquid",
        "cell_kind": "fluid",
        "temperature_kelvin": 1100,
        "density": 1000,
        "viscosity": 1000,
        "color": "#000096",
        "loader_line": 87,
    },
    {
        "id": "hot_semiheavy_water",
        "gt6_fluid": "hotsemiheavywater",
        "english": "Hot Semiheavy Water",
        "chinese": "热半重水",
        "source_material": "semiheavy_water",
        "state": "liquid",
        "cell_kind": "fluid",
        "temperature_kelvin": 550,
        "density": 1000,
        "viscosity": 1000,
        "color": "#C8C89B",
        "loader_line": 92,
    },
    {
        "id": "hot_heavy_water",
        "gt6_fluid": "hotheavywater",
        "english": "Hot Heavy Water",
        "chinese": "热重水",
        "source_material": "heavy_water",
        "state": "liquid",
        "cell_kind": "fluid",
        "temperature_kelvin": 600,
        "density": 1000,
        "viscosity": 1000,
        "color": "#FFFF64",
        "loader_line": 91,
    },
    {
        "id": "hot_tritiated_water",
        "gt6_fluid": "hottritiatedwater",
        "english": "Hot Tritiated Water",
        "chinese": "热氚水",
        "source_material": "tritiated_water",
        "state": "liquid",
        "cell_kind": "fluid",
        "temperature_kelvin": 650,
        "density": 1000,
        "viscosity": 1000,
        "color": "#FF6464",
        "loader_line": 93,
    },
    {
        "id": "hot_molten_licl",
        "gt6_fluid": "hotmoltenlicl",
        "english": "Hot Molten Lithium Chloride",
        "chinese": "热熔融氯化锂",
        "source_material": "lithium_chloride",
        "state": "liquid",
        "cell_kind": "fluid",
        "temperature_kelvin": 1600,
        "density": 1000,
        "viscosity": 1000,
        "color": "#DEDEFA",
        "loader_line": 89,
    },
    {
        "id": "hot_carbon_dioxide",
        "gt6_fluid": "hotcarbondioxide",
        "english": "Hot Carbon Dioxide",
        "chinese": "热二氧化碳",
        "source_material": "carbon_dioxide",
        "state": "gas",
        "cell_kind": "gas",
        "temperature_kelvin": 950,
        "density": 1000,
        "viscosity": 1000,
        "color": "#282828",
        "loader_line": 95,
        "fl_enum_class": "GAS",
        "loader_astate": 1,
    },
    {
        "id": "hot_helium",
        "gt6_fluid": "hothelium",
        "english": "Hot Helium",
        "chinese": "热氦",
        "source_material": "helium",
        "state": "gas",
        "cell_kind": "gas",
        "temperature_kelvin": 1150,
        "density": 1000,
        "viscosity": 1000,
        "color": "#FFFF78",
        "loader_line": 96,
        "fl_enum_class": "GAS",
        "loader_astate": 1,
    },
)


def dump(path: Path, payload: object) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(
        json.dumps(payload, indent=2, ensure_ascii=False) + "\n",
        encoding="utf-8",
    )


def write_waves() -> None:
    WAVE.mkdir(parents=True, exist_ok=True)
    dump(
        WAVE / "wave.json",
        {
            "cohort": "fission-hot-fluids",
            "depends_on": ["runtime/fission-survival"],
            "generated_by": "runtime/fission-hot-fluids implementation",
            "owns_families": 0,
            "program": "runtime/fission-hot-fluids",
            "schema_version": 1,
            "wave_slug": "runtime/fission-hot-fluids",
        },
    )
    dump(
        WAVE / "topology.json",
        {
            "append_only": False,
            "complete_family_count": 0,
            "generated_by": "runtime/fission-hot-fluids implementation",
            "next_unassigned": True,
            "remaining_recipe_gap": 0,
            "schema_version": 1,
            "source_revision": REV,
            "status": "WAVE_READY",
            "unique_active_wave": None,
            "wave_slug": "runtime/fission-hot-fluids",
        },
    )
    dump(
        WAVE / "readiness.json",
        {
            "evidence": {
                "acquisition": "reactor_hot_output",
                "cc_owned_conversion_rows": 9,
                "core_identity_count": 2,
                "hot_output_identity_rows": 8,
                "hot_fluids_status": "runtime_ready",
                "source_contract_rows": 11,
            },
            "generated_by": "runtime/fission-hot-fluids implementation",
            "generated_recipe_count": 0,
            "next_unassigned": True,
            "nuclear_started": True,
            "owns_families": 0,
            "production_lock": None,
            "schema_version": 1,
            "source_revision": REV,
            "status": "FISSION_HOT_FLUIDS_READY",
            "unique_active_wave": None,
            "wave_slug": "runtime/fission-hot-fluids",
        },
    )
    dump(
        WAVE / "census_delta.json",
        {
            "complete_family_count": 0,
            "completion_delta": 0,
            "generated_by": "runtime/fission-hot-fluids implementation",
            "leftover_later_count": 0,
            "partial_family_count": 0,
            "remaining_recipe_gap": 0,
            "schema_version": 1,
            "source_revision": REV,
            "status": "CENSUS_DELTA_IN_PROGRESS",
            "wave_slug": "runtime/fission-hot-fluids",
            "work_set": {
                "family_count": 0,
                "source_rows": 11,
            },
        },
    )
    dump(
        WAVE / "load_axis.json",
        {
            "cc_owned_conversion_rows": 9,
            "generated_by": "runtime/fission-hot-fluids implementation",
            "hot_output_identity_rows": 8,
            "schema_version": 1,
            "source_contract_rows": 11,
            "source_revision": REV,
            "wave_slug": "runtime/fission-hot-fluids",
        },
    )
    dump(
        WAVE / "required_forms.json",
        {
            "counts": {"required_form_pairs": 0, "required_materials": 0},
            "generated_by": "runtime/fission-hot-fluids implementation",
            "note": "No new recipes. Coolant inputs already exist.",
            "required_forms": {},
            "schema_version": 1,
            "wave_slug": "runtime/fission-hot-fluids",
        },
    )


def conversion_row(
    index: int,
    branch: str,
    source_coolant: str,
    cc_input: str | None,
    hot_output: str | None,
    eu_per_unit: int | None,
    output_multiplier: int,
    heat_divider: int,
    disposition: str,
    owner: str,
    notes: str,
    source_lines: str,
) -> dict[str, object]:
    return {
        "index": index,
        "branch": branch,
        "source_file": CORE_1X1,
        "source_file_2x2": CORE_2X2,
        "source_lines": source_lines,
        "source_revision": REV,
        "source_coolant_identity": source_coolant,
        "cc_input_registry_identity": cc_input,
        "hot_output_registry_identity": hot_output,
        "input_units": 1,
        "eu_per_unit": eu_per_unit,
        "output_units": output_multiplier,
        "output_multiplier": output_multiplier,
        "heat_divider": heat_divider,
        "backpressure_behavior": (
            "retain_heat_and_input"
            if disposition == "ready"
            else "not_player_path"
        ),
        "gt6_fillAll_failure": "slotKill_and_commented_explode",
        "cc_backpressure_policy": (
            "DESIGN_POLICY retain; explosion/radiation stay on the safety card"
            if disposition == "ready"
            else "blocked"
        ),
        "insufficient_coolant": (
            "SOURCE_BACKED slotKill / destroyRods"
            if disposition == "ready"
            else "blocked"
        ),
        "empty_coolant_rod_loss": (
            "SOURCE_BACKED slotKill when oEnergy/lastHeat > 0 and rods present"
            if disposition == "ready"
            else "blocked"
        ),
        "save_load_fields": [
            "heat",
            "lastHeat",
            "coolant",
            "output",
            "stopped",
            "running",
            "mode",
            "rods",
            "n.*",
            "o.*",
        ],
        "1x1_2x2_identical": True,
        "disposition": disposition,
        "owner": owner,
        "notes": notes,
    }


def write_d0() -> None:
    rows = [
        conversion_row(
            1,
            "Coolant_IC2 -> Coolant_IC2_Hot",
            "FL.Coolant_IC2 / ic2coolant",
            None,
            None,
            20,
            1,
            1,
            "out_of_scope_external",
            "external/ic2_compat",
            "IC2 industrial coolant is not a CC-owned fluid. Not substituted.",
            "111-115 (1x1) / 138-142 (2x2); CS.EU_PER_COOLANT=20; FL.java:89-90",
        ),
        conversion_row(
            2,
            "distilled_water -> steam",
            "FL.distw / ic2distilledwater / MT.DistWater",
            "cruciblecraft:water_distilled",
            "cruciblecraft:steam",
            80,
            160,
            1,
            "ready",
            "energy/nuclear-fission-hot-fluids",
            "Existing player steam path. Not a hot_* identity.",
            "116-119 (1x1) / 143-146 (2x2); CS.EU_PER_WATER=80 STEAM_PER_WATER=160",
        ),
        conversion_row(
            3,
            "molten_tin -> Hot_Molten_Tin",
            "MT.Sn.mLiquid",
            "cruciblecraft:molten_tin",
            "cruciblecraft:hot_molten_tin",
            40,
            1,
            3,
            "ready",
            "energy/nuclear-fission-hot-fluids",
            "Independent hot identity. heat divider 3.",
            "121-124 (1x1) / 148-151 (2x2); CS.EU_PER_TIN=40; Loader_Fluids.java:88",
        ),
        conversion_row(
            4,
            "molten_sodium -> Hot_Molten_Sodium",
            "MT.Na.mLiquid",
            "cruciblecraft:molten_sodium",
            "cruciblecraft:hot_molten_sodium",
            30,
            1,
            6,
            "ready",
            "energy/nuclear-fission-hot-fluids",
            "Independent hot identity. heat divider 6.",
            "126-129 (1x1) / 153-156 (2x2); CS.EU_PER_SODIUM=30; Loader_Fluids.java:87",
        ),
        conversion_row(
            5,
            "semiheavy_water -> Hot_Semi_Heavy_Water",
            "MT.HDO.mLiquid",
            "cruciblecraft:semiheavy_water",
            "cruciblecraft:hot_semiheavy_water",
            40,
            1,
            1,
            "ready",
            "energy/nuclear-fission-hot-fluids",
            "Independent hot identity.",
            "131-134 (1x1) / 158-161 (2x2); CS.EU_PER_SEMI_HEAVY_WATER=40",
        ),
        conversion_row(
            6,
            "heavy_water -> Hot_Heavy_Water",
            "MT.D2O.mLiquid",
            "cruciblecraft:heavy_water",
            "cruciblecraft:hot_heavy_water",
            50,
            1,
            1,
            "ready",
            "energy/nuclear-fission-hot-fluids",
            "Independent hot identity. neutron_max / 8 stays on rod physics.",
            "136-139 (1x1) / 163-166 (2x2); CS.EU_PER_HEAVY_WATER=50",
        ),
        conversion_row(
            7,
            "tritiated_water -> Hot_Tritiated_Water",
            "MT.T2O.mLiquid",
            "cruciblecraft:tritiated_water",
            "cruciblecraft:hot_tritiated_water",
            60,
            1,
            1,
            "ready",
            "energy/nuclear-fission-hot-fluids",
            "CC already owns tritiated_water. neutron_max / 16.",
            "141-144 (1x1) / 168-171 (2x2); CS.EU_PER_TRITIATED_WATER=60",
        ),
        conversion_row(
            8,
            "molten_licl -> Hot_Molten_LiCl",
            "MT.LiCl.mLiquid",
            "cruciblecraft:molten_lithium_chloride",
            "cruciblecraft:hot_molten_licl",
            15,
            1,
            1,
            "ready",
            "energy/nuclear-fission-hot-fluids",
            "Independent hot identity. Input is molten, not a chemical alias.",
            "146-149 (1x1) / 173-176 (2x2); CS.EU_PER_LICL=15",
        ),
        conversion_row(
            9,
            "carbon_dioxide -> Hot_Carbon_Dioxide",
            "MT.CO2.mGas",
            "cruciblecraft:carbon_dioxide",
            "cruciblecraft:hot_carbon_dioxide",
            20,
            1,
            1,
            "ready",
            "energy/nuclear-fission-hot-fluids",
            "FL enum GAS; Loader_Fluids aState=1 so Fluid density/viscosity are liquid defaults.",
            "151-154 (1x1) / 178-181 (2x2); CS.EU_PER_CO2=20; Loader_Fluids.java:95",
        ),
        conversion_row(
            10,
            "helium -> Hot_Helium",
            "MT.He.mGas",
            "cruciblecraft:helium",
            "cruciblecraft:hot_helium",
            30,
            1,
            1,
            "ready",
            "energy/nuclear-fission-hot-fluids",
            "FL enum GAS; Loader_Fluids aState=1 so Fluid density/viscosity are liquid defaults.",
            "156-159 (1x1) / 183-186 (2x2); CS.EU_PER_HELIUM=30; Loader_Fluids.java:96",
        ),
        conversion_row(
            11,
            "Thorium_Salt -> LiCl",
            "FL.Thorium_Salt / thoriumsalt",
            None,
            "cruciblecraft:molten_lithium_chloride",
            2560000,
            1,
            1,
            "out_of_scope_external",
            "external/thorium_salt",
            "Not a hot-fluid output. Molten thorium salt is not a CC-owned input. Not substituted.",
            "161-164 (1x1) / 188-191 (2x2); CS.EU_PER_THORIUM_SALT=2560000; Loader_Fluids.java:97",
        ),
    ]
    ready = [row for row in rows if row["disposition"] == "ready"]
    blocked = [row for row in rows if row["disposition"] != "ready"]
    dump(
        WAVE / "d0_hot_fluid_source_matrix.json",
        {
            "schema_version": 1,
            "source_revision": REV,
            "wave_slug": "runtime/fission-hot-fluids",
            "source_contract_rows": 11,
            "cc_owned_conversion_rows": 9,
            "hot_output_identity_rows": 8,
            "core_identity_count": 2,
            "missing": 0,
            "extra": 0,
            "duplicate": 0,
            "hot_identity_aliases": 0,
            "stand_in_fluids": 0,
            "1x1_2x2_branches_identical": True,
            "temperature_contract": {
                "choice": 2,
                "heat_unit": "HU",
                "gt6_thermometer": "oEnergy HU; MultiTileEntityReactorCore.java:192-196",
                "temperature_k": "blocked",
                "meltdown_at_k": "blocked",
                "radiation": "blocked",
                "heat_damage": "blocked",
                "owner": "energy/nuclear-fission-observation-safety",
                "forbidden": "HU multiplied by an unproven constant to invent Kelvin",
            },
            "fail_semantics": {
                "backpressure": (
                    "DESIGN_POLICY retain heat and input. GT6 fillAll failure "
                    "sets tIsExploding and slotKill, but explode() is commented "
                    "TODO and radiation/sound stay on the safety card. Plan C.4 "
                    "forbids backpressure from silently deciding player loss."
                ),
                "insufficient_coolant": (
                    "SOURCE_BACKED destroyRods / slotKill. Heat is not consumed."
                ),
                "empty_coolant": (
                    "SOURCE_BACKED destroyRods when lastHeat/oEnergy > 0 and rods "
                    "are present. Explosion, sound and radioactivity stay blocked."
                ),
                "missing_hot_identity": (
                    "Do not consume, do not emit a cold alias, do not destroy rods."
                ),
            },
            "save_load": {
                "gt6_persists": ["mEnergy", "mRunning", "mStopped", "mMode", "tanks", "neutrons"],
                "gt6_does_not_persist": ["oEnergy"],
                "cc_persists_lastHeat": (
                    "DESIGN_POLICY persist lastHeat so the heat contract does not "
                    "drift across reload. Missing field loads as 0."
                ),
                "old_output_identity": (
                    "Do not reinterpret a cold coolant fluid sitting in the output "
                    "tank as the new hot identity. Incompatible fill becomes backpressure."
                ),
            },
            "status": "D0_MATRIX_READY",
            "references": {
                "cs": CS_JAVA,
                "fl": FL_JAVA,
                "loader_fluids": LOADER_FLUIDS,
                "core": CORE_BASE,
                "rod_nuclear": ROD_NUCLEAR,
            },
            "branches": rows,
            "cc_owned": [row["branch"] for row in ready],
            "blocked_ledger": [
                {
                    "branch": row["branch"],
                    "disposition": row["disposition"],
                    "owner": row["owner"],
                    "notes": row["notes"],
                }
                for row in blocked
            ],
        },
    )


def write_hot_fluid_gate() -> None:
    fluids = []
    for fluid in HOT_FLUIDS:
        fluids.append(
            {
                "id": fluid["id"],
                "gt6_fluid": fluid["gt6_fluid"],
                "source_material": fluid["source_material"],
                "state": fluid["state"],
                "temperature_kelvin": fluid["temperature_kelvin"],
                "density": fluid["density"],
                "viscosity": fluid["viscosity"],
                "color": fluid["color"],
                "world_placeable": False,
                "bucket": False,
                "cell_kind": fluid["cell_kind"],
                "english": fluid["english"],
                "chinese": fluid["chinese"],
                "source": {
                    "repository": "GregTech6/gregtech6",
                    "revision": REV,
                    "path": f"{LOADER_FLUIDS}:{fluid['loader_line']}",
                    "reason": (
                        "GT6 Loader_Fluids.create independent hot-fluid identity. "
                        "Must not resolve through materialFluid of the cold material."
                    ),
                },
            }
        )
    dump(
        SRC / "data" / "cruciblecraft" / "hot_fluid_gate.json",
        {
            "schema_version": 1,
            "fluids": fluids,
            "note": (
                "Independent hot-fluid identities. Not loaded by "
                "ChemicalFluidRegistrationGate so they cannot steal materialFluid()."
            ),
        },
    )


def write_art() -> None:
    dest_dir = CC_ASSETS / "textures" / "fluid" / "gt6_import"
    dest_dir.mkdir(parents=True, exist_ok=True)
    imports = []
    for fluid in HOT_FLUIDS:
        src_name = f"{fluid['gt6_fluid']}.png"
        src = GT6_FLUIDS / src_name
        if not src.is_file():
            raise SystemExit(f"missing GT6 fluid texture {src}")
        dest = dest_dir / f"{fluid['id']}.png"
        shutil.copy2(src, dest)
        imports.append(
            {
                "gt6_source": (
                    "assets/gregtech/textures/blocks/fluids/" + src_name
                ),
                "destination": (
                    "assets/cruciblecraft/textures/fluid/gt6_import/"
                    + fluid["id"]
                    + ".png"
                ),
            }
        )
        meta_src = GT6_FLUIDS / f"{fluid['gt6_fluid']}.png.mcmeta"
        if meta_src.is_file():
            shutil.copy2(meta_src, dest_dir / f"{fluid['id']}.png.mcmeta")
            imports.append(
                {
                    "gt6_source": (
                        "assets/gregtech/textures/blocks/fluids/"
                        + fluid["gt6_fluid"]
                        + ".png.mcmeta"
                    ),
                    "destination": (
                        "assets/cruciblecraft/textures/fluid/gt6_import/"
                        + fluid["id"]
                        + ".png.mcmeta"
                    ),
                }
            )
    dump(
        CC_ASSETS / "gt6_fission_hot_fluids_art_manifest.json",
        {
            "source": "gt6_referencable_port_code/gregtech6_w",
            "source_revision": REV,
            "imports": imports,
        },
    )


def patch_cell_content_gate() -> None:
    path = SRC / "data" / "cruciblecraft" / "cell_content_gate.json"
    document = json.loads(path.read_text(encoding="utf-8"))
    fluids = document["fluids"]
    by_id = {row["id"]: row for row in fluids}
    for fluid in HOT_FLUIDS:
        fluid_id = f"cruciblecraft:{fluid['id']}"
        by_id[fluid_id] = {
            "id": fluid_id,
            "kind": fluid["cell_kind"],
            "material": fluid["id"],
        }
    document["fluids"] = [by_id[key] for key in sorted(by_id)]
    dump(path, document)


def copy_gametest_structures() -> None:
    src_ns = SRC / "data" / "cruciblecraft_wave_runtime_fission_survival"
    dst_ns = SRC / "data" / "cruciblecraft_wave_runtime_fission_hot_fluids"
    for relative in (
        Path("structure") / "empty.nbt",
        Path("gametest") / "structure" / "empty.nbt",
    ):
        src = src_ns / relative
        dst = dst_ns / relative
        dst.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(src, dst)


def main() -> None:
    write_waves()
    write_d0()
    write_hot_fluid_gate()
    write_art()
    patch_cell_content_gate()
    copy_gametest_structures()


if __name__ == "__main__":
    main()
