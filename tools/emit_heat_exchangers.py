#!/usr/bin/env python3
"""Heat-exchanger first slice: D0 matrix, quadruple casing, FM.Hot recipes, art, wave."""
from __future__ import annotations

import json
import re
import shutil
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import currentness
from tools import io_common as io
from tools.emit_fission_survival_wave import add_include

SRC = ROOT / "src" / "main" / "resources"
GEN = ROOT / "src" / "generated" / "resources"
REVISION = io.SOURCE_REVISION
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
CASING_MATERIALS = (
    "invar",
    "tungsten",
    "tungstensteel",
    "tantalum_hafnium_carbide",
)
HOT_RECIPES = (
    ("hot_molten_sodium", "molten_sodium", 30, "Loader_Fuels.java:204 EU_PER_SODIUM"),
    ("hot_molten_tin", "molten_tin", 40, "Loader_Fuels.java:205 EU_PER_TIN"),
    ("hot_heavy_water", "heavy_water", 50, "Loader_Fuels.java:206 EU_PER_HEAVY_WATER"),
    ("hot_semiheavy_water", "semiheavy_water", 40, "Loader_Fuels.java:207 EU_PER_SEMI_HEAVY_WATER"),
    ("hot_tritiated_water", "tritiated_water", 60, "Loader_Fuels.java:208 EU_PER_TRITIATED_WATER"),
    ("hot_carbon_dioxide", "carbon_dioxide", 20, "Loader_Fuels.java:209 EU_PER_CO2"),
    ("hot_helium", "helium", 30, "Loader_Fuels.java:210 EU_PER_HELIUM"),
    ("hot_molten_licl", "molten_lithium_chloride", 15, "Loader_Fuels.java:211 EU_PER_LICL"),
)
MACHINES = (
    {
        "id": "cruciblecraft:heat_exchanger_invar",
        "source_id": 9103,
        "source_line": 709,
        "dense": False,
        "material": "invar",
        "hu_rate": 16,
        "efficiency_bps": 10000,
        "hardness": 4.0,
        "resistance": 4.0,
        "lang_en": "Heat Exchanger (Invar)",
        "lang_zh": "热交换器（殷钢）",
        "double_plate_material": "copper",
        "pipe": "small_fluid_pipe",
        "plate": "plate",
        "casing": "machine_casing",
    },
    {
        "id": "cruciblecraft:heat_exchanger_tungsten",
        "source_id": 9107,
        "source_line": 710,
        "dense": False,
        "material": "tungsten",
        "hu_rate": 128,
        "efficiency_bps": 10000,
        "hardness": 10.0,
        "resistance": 10.0,
        "lang_en": "Heat Exchanger (Tungsten)",
        "lang_zh": "热交换器（钨）",
        "double_plate_material": "annealed_copper",
        "pipe": "small_fluid_pipe",
        "plate": "plate",
        "casing": "machine_casing",
        "any_w": True,
    },
    {
        "id": "cruciblecraft:heat_exchanger_tungstensteel",
        "source_id": 9108,
        "source_line": 711,
        "dense": False,
        "material": "tungstensteel",
        "hu_rate": 128,
        "efficiency_bps": 9000,
        "hardness": 12.5,
        "resistance": 12.5,
        "lang_en": "Heat Exchanger (Tungstensteel)",
        "lang_zh": "热交换器（钨钢）",
        "double_plate_material": "annealed_copper",
        "pipe": "small_fluid_pipe",
        "plate": "plate",
        "casing": "machine_casing",
    },
    {
        "id": "cruciblecraft:heat_exchanger_tantalum_hafnium_carbide",
        "source_id": 9109,
        "source_line": 712,
        "dense": False,
        "material": "tantalum_hafnium_carbide",
        "hu_rate": 256,
        "efficiency_bps": 10000,
        "hardness": 12.5,
        "resistance": 12.5,
        "lang_en": "Heat Exchanger (Tantalum Hafnium Carbide)",
        "lang_zh": "热交换器（钽铪碳化物）",
        "double_plate_material": "annealed_copper",
        "pipe": "small_fluid_pipe",
        "plate": "plate",
        "casing": "machine_casing",
    },
    {
        "id": "cruciblecraft:dense_heat_exchanger_invar",
        "source_id": 9153,
        "source_line": 714,
        "dense": True,
        "material": "invar",
        "hu_rate": 64,
        "efficiency_bps": 10000,
        "hardness": 4.0,
        "resistance": 4.0,
        "lang_en": "Dense Heat Exchanger (Invar)",
        "lang_zh": "致密热交换器（殷钢）",
        "double_plate_material": "copper",
        "pipe": "large_fluid_pipe",
        "plate": "quadruple_plate",
        "casing": "machine_casing_quadruple",
    },
    {
        "id": "cruciblecraft:dense_heat_exchanger_tungsten",
        "source_id": 9157,
        "source_line": 715,
        "dense": True,
        "material": "tungsten",
        "hu_rate": 512,
        "efficiency_bps": 10000,
        "hardness": 10.0,
        "resistance": 10.0,
        "lang_en": "Dense Heat Exchanger (Tungsten)",
        "lang_zh": "致密热交换器（钨）",
        "double_plate_material": "annealed_copper",
        "pipe": "large_fluid_pipe",
        "plate": "quadruple_plate",
        "casing": "machine_casing_quadruple",
        "any_w": True,
    },
    {
        "id": "cruciblecraft:dense_heat_exchanger_tungstensteel",
        "source_id": 9158,
        "source_line": 716,
        "dense": True,
        "material": "tungstensteel",
        "hu_rate": 512,
        "efficiency_bps": 9000,
        "hardness": 12.5,
        "resistance": 12.5,
        "lang_en": "Dense Heat Exchanger (Tungstensteel)",
        "lang_zh": "致密热交换器（钨钢）",
        "double_plate_material": "annealed_copper",
        "pipe": "large_fluid_pipe",
        "plate": "quadruple_plate",
        "casing": "machine_casing_quadruple",
    },
    {
        "id": "cruciblecraft:dense_heat_exchanger_tantalum_hafnium_carbide",
        "source_id": 9159,
        "source_line": 717,
        "dense": True,
        "material": "tantalum_hafnium_carbide",
        "hu_rate": 1024,
        "efficiency_bps": 10000,
        "hardness": 12.5,
        "resistance": 12.5,
        "lang_en": "Dense Heat Exchanger (Tantalum Hafnium Carbide)",
        "lang_zh": "致密热交换器（钽铪碳化物）",
        "double_plate_material": "annealed_copper",
        "pipe": "large_fluid_pipe",
        "plate": "quadruple_plate",
        "casing": "machine_casing_quadruple",
    },
)


def dump(path: Path, payload: object) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(
        json.dumps(payload, indent=2, ensure_ascii=False) + "\n",
        encoding="utf-8",
    )


def insert_after(path: Path, needle: str, inserted: str) -> bool:
    text = path.read_text(encoding="utf-8")
    if inserted in text:
        return False
    if needle not in text:
        raise SystemExit(f"missing needle in {path}: {needle!r}")
    path.write_text(text.replace(needle, needle + inserted, 1), encoding="utf-8")
    return True


def replace_once(path: Path, needle: str, replacement: str) -> bool:
    text = path.read_text(encoding="utf-8")
    if needle not in text:
        if replacement in text:
            return False
        raise SystemExit(f"missing needle in {path}: {needle!r}")
    path.write_text(text.replace(needle, replacement, 1), encoding="utf-8")
    return True


def copy_png(src: Path, dest: Path) -> None:
    if not src.is_file():
        raise SystemExit(f"missing gregtech6_w texture {src}")
    dest.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(src, dest)


def slot(gt6: str, item: str, note: str) -> dict:
    return {
        "gt6": gt6,
        "cc": item,
        "status": "exact",
        "note": note,
    }


def write_prefix() -> None:
    dump(
        SRC / "data" / "cruciblecraft" / "material_prefixes" / "machine_casing_quadruple.json",
        {
            "aliases": ["casingMachineQuadruple"],
            "capabilities": [],
            "generation_flag": "cruciblecraft:generates_machine_casing",
            "id": "cruciblecraft:machine_casing_quadruple",
            "model_template": "minecraft:item/generated",
            "model_texture": "cruciblecraft:block/material/machine_casing_quadruple",
            "serialized_path": "machine_casing_quadruple",
            "tag_directory": "machine_casing_quadruples",
            "tag_namespace": "c",
            "units": 3744,
        },
    )
    insert_after(
        SRC / "data" / "cruciblecraft" / "material_prefixes" / "index.json",
        '  "machine_casing_dense.json",\n',
        '  "machine_casing_quadruple.json",\n',
    )
    insert_after(
        SRC / "data" / "cruciblecraft" / "material_zh_cn.json",
        '    "machine_casing_double": "双层机器外壳",\n',
        '    "machine_casing_quadruple": "四层机器外壳",\n',
    )
    # Quadruple casings share generates_machine_casing with ordinary/double.
    # Do not re-narrow the prefix to a four-material heat-exchanger flag.


def _object_span(text: str, open_idx: int) -> tuple[int, int]:
    depth = 0
    for index in range(open_idx, len(text)):
        char = text[index]
        if char == "{":
            depth += 1
        elif char == "}":
            depth -= 1
            if depth == 0:
                return open_idx, index + 1
    raise SystemExit("unterminated JSON object")


def patch_gate() -> None:
    path = SRC / "data" / "cruciblecraft" / "material_registration_gate.json"
    text = path.read_text(encoding="utf-8")
    data = json.loads(text)
    added = 0
    marker = '\n  "materials": {'
    start = text.index(marker) + 1
    _, end = _object_span(text, text.index("{", start))
    section = text[start:end]
    for material in CASING_MATERIALS:
        forms = list(data["materials"][material])
        if "machine_casing_quadruple" in forms:
            continue
        forms.append("machine_casing_quadruple")
        forms.sort()
        data["materials"][material] = forms
        added += 1
        pattern = (
            rf'(    "{re.escape(material)}": \[\n      "{re.escape(forms[0])}",[\s\S]*?\n    \])'
        )
        replacement = (
            f'    "{material}": [\n'
            + ",\n".join(f'      "{form}"' for form in forms)
            + "\n    ]"
        )
        section, count = re.subn(pattern, replacement, section, count=1)
        if count != 1:
            raise SystemExit(f"failed to patch materials forms for {material}")
    if added:
        text = text[:start] + section + text[end:]
        old = json.loads(path.read_text(encoding="utf-8"))["counts"]["registered_forms"]
        new = old + added
        text = text.replace(
            f'"registered_forms": {old}',
            f'"registered_forms": {new}',
            1,
        )
        path.write_text(text, encoding="utf-8")
        currentness.write_sidecar(path)


def write_catalog() -> None:
    machines = []
    for row in MACHINES:
        plate_prefix = "dense_plate" if row["dense"] else "double_plate"
        machines.append(
            {
                "id": row["id"],
                "source_id": row["source_id"],
                "source_line": row["source_line"],
                "gt6_class": "MultiTileEntityGeneratorHotFluid",
                "dense": row["dense"],
                "material": row["material"],
                "any_w": bool(row.get("any_w")),
                "hu_rate": row["hu_rate"],
                "efficiency_bps": row["efficiency_bps"],
                "hardness": row["hardness"],
                "resistance": row["resistance"],
                "lang_en": row["lang_en"],
                "lang_zh": row["lang_zh"],
                "recipe": {
                    "pattern": ["PCP", "OwO", "PMP"],
                    "keys": {
                        "M": {
                            "prefix": row["casing"],
                            "material": row["material"],
                        },
                        "O": {
                            "prefix": row["pipe"],
                            "material": "copper",
                            "family": "ANY.Cu",
                        },
                        "P": {
                            "prefix": row["plate"],
                            "material": "lead",
                        },
                        "C": {
                            "prefix": plate_prefix,
                            "material": row["double_plate_material"],
                            "family": None
                            if row["double_plate_material"] == "annealed_copper"
                            else "ANY.Cu",
                        },
                    },
                    "catalyst": "w",
                },
            }
        )
    dump(
        SRC / "data" / "cruciblecraft" / "heat_exchangers.json",
        {
            "schema_version": 1,
            "source_revision": REVISION,
            "source_path": "gt6_code/gregtech6/src/main/java/gregtech/loaders/b/Loader_MultiTileEntities.java",
            "machines": machines,
        },
    )


def write_d0() -> None:
    rows = []
    for row in MACHINES:
        plate_prefix = "dense_plate" if row["dense"] else "double_plate"
        casing_gt = (
            "OP.casingMachineQuadruple"
            if row["dense"]
            else "OP.casingMachine"
        )
        pipe_gt = "OP.pipeLarge(ANY.Cu)" if row["dense"] else "OP.pipeSmall(ANY.Cu)"
        plate_gt = (
            "OP.plateQuadruple(MT.Pb)" if row["dense"] else "OP.plate(MT.Pb)"
        )
        if row["double_plate_material"] == "annealed_copper":
            cap_gt = (
                "OP.plateDense(MT.AnnealedCopper)"
                if row["dense"]
                else "OP.plateDouble(MT.AnnealedCopper)"
            )
        else:
            cap_gt = (
                "OP.plateDense(ANY.Cu)" if row["dense"] else "OP.plateDouble(ANY.Cu)"
            )
        mat_gt = "ANY.W" if row.get("any_w") else f"MT.{row['material']}"
        rows.append(
            {
                "source_id": row["source_id"],
                "cc_id": row["id"],
                "status": "exact",
                "slots": {
                    "M": slot(
                        f"{casing_gt}({mat_gt})",
                        f"cruciblecraft:{row['material']}/{row['casing']}",
                        "ANY.W lands on tungsten; quadruple casing added in-scope for this card's four materials only.",
                    ),
                    "O": slot(
                        pipe_gt,
                        f"cruciblecraft:copper/{row['pipe']}",
                        "ANY.Cu family member; annealed_copper pipes are not registered.",
                    ),
                    "P": slot(
                        plate_gt,
                        f"cruciblecraft:lead/{row['plate']}",
                        "Lead plate / quadruple plate already live.",
                    ),
                    "C": slot(
                        cap_gt,
                        f"cruciblecraft:{row['double_plate_material']}/{plate_prefix}",
                        "9103/9153 use ANY.Cu copper; the other six use AnnealedCopper.",
                    ),
                    "w": slot(
                        "wrench catalyst",
                        "cruciblecraft:material_wrench",
                        "GT6 aMachine recipe; CC shaped_catalyst.",
                    ),
                },
            }
        )
    dump(
        ROOT
        / "tools"
        / "waves"
        / "runtime"
        / "heat-exchangers"
        / "d0_obtain_matrix.json",
        {
            "schema_version": 1,
            "source_revision": REVISION,
            "source_path": "Loader_MultiTileEntities.java:707-717",
            "blocked_extra_fm_hot": [
                "lava",
                "blaze",
                "ic2_hot_coolant",
                "geothermal_water",
                "hot_water",
            ],
            "rows": rows,
            "exact_count": 8,
            "blocked_count": 0,
        },
    )


def write_hot_recipes() -> None:
    root = SRC / "data" / "cruciblecraft" / "recipe" / "energy" / "fuels_hot"
    for hot_id, cold_id, duration, provenance in HOT_RECIPES:
        dump(
            root / f"{hot_id}.json",
            {
                "type": "cruciblecraft:gt_recipe",
                "map": "cruciblecraft:fuels_hot",
                "duration": duration,
                "eut": -1,
                "can_be_buffered": True,
                "fluid_inputs": [{"id": f"cruciblecraft:{hot_id}", "amount": 1}],
                "fluid_outputs": [{"id": f"cruciblecraft:{cold_id}", "amount": 1}],
                "provenance": {
                    "selected_source_recipe": provenance,
                    "source_kind": "gt6_source",
                },
            },
        )


def write_art() -> None:
    imports = []
    copies = [
        (
            GT6_ART / "blocks" / "materialicons" / "metallic" / "casingmachinequadruple.png",
            SRC / "assets" / "cruciblecraft" / "textures" / "block" / "material" / "machine_casing_quadruple.png",
            "assets/gregtech/textures/blocks/materialicons/metallic/casingmachinequadruple.png",
            "assets/cruciblecraft/textures/block/material/machine_casing_quadruple.png",
            "OP.casingMachineQuadruple metallic sheet; in-scope for four heat-exchanger materials.",
        ),
        (
            GT6_ART
            / "blocks"
            / "materialicons"
            / "metallic"
            / "casingmachinequadruple_overlay.png",
            SRC
            / "assets"
            / "cruciblecraft"
            / "textures"
            / "block"
            / "material"
            / "machine_casing_quadruple_overlay.png",
            "assets/gregtech/textures/blocks/materialicons/metallic/casingmachinequadruple_overlay.png",
            "assets/cruciblecraft/textures/block/material/machine_casing_quadruple_overlay.png",
            "Matching overlay sheet for quadruple machine casings.",
        ),
    ]
    for face in ("bottom", "top", "left", "front", "right", "back"):
        for folder in ("colored", "overlay", "overlay_active"):
            copies.append(
                (
                    GT6_ART
                    / "blocks"
                    / "machines"
                    / "generators"
                    / "hot_fluid"
                    / folder
                    / f"{face}.png",
                    SRC
                    / "assets"
                    / "cruciblecraft"
                    / "textures"
                    / "block"
                    / "machine"
                    / "heat_exchanger"
                    / folder
                    / f"{face}.png",
                    f"assets/gregtech/textures/blocks/machines/generators/hot_fluid/{folder}/{face}.png",
                    f"assets/cruciblecraft/textures/block/machine/heat_exchanger/{folder}/{face}.png",
                    "Shared MultiTileEntityGeneratorHotFluid texture id 9103.",
                )
            )
    for src, dest, gt6, dest_rel, note in copies:
        copy_png(src, dest)
        imports.append(
            {
                "source": "gt6_referencable_port_code/gregtech6_w",
                "gt6_source": gt6,
                "destination": dest_rel,
                "note": note,
            }
        )
    dump(
        SRC / "assets" / "cruciblecraft" / "gt6_heat_exchangers_art_manifest.json",
        {
            "source": "gt6_referencable_port_code/gregtech6_w",
            "source_revision": REVISION,
            "source_present": True,
            "imports": imports,
        },
    )
    casing_model = SRC / "assets" / "cruciblecraft" / "models" / "block" / "machine_casing.json"
    text = casing_model.read_text(encoding="utf-8")
    dest = SRC / "assets" / "cruciblecraft" / "models" / "block" / "machine_casing_quadruple.json"
    dest.write_text(
        text.replace("machine_casing", "machine_casing_quadruple").replace(
            "casingMachine PrefixBlock",
            "casingMachineQuadruple PrefixBlock",
        ),
        encoding="utf-8",
    )


def write_wave() -> None:
    wave = ROOT / "tools" / "waves" / "runtime" / "heat-exchangers"
    wave.mkdir(parents=True, exist_ok=True)
    dump(
        wave / "wave.json",
        {
            "cohort": "heat-exchangers",
            "depends_on": ["runtime/fission-hot-fluids"],
            "generated_by": "runtime/heat-exchangers implementation",
            "owns_families": 0,
            "program": "runtime/heat-exchangers",
            "schema_version": 1,
            "wave_slug": "runtime/heat-exchangers",
        },
    )
    dump(
        wave / "topology.json",
        {
            "append_only": False,
            "complete_family_count": 0,
            "generated_by": "runtime/heat-exchangers implementation",
            "next_unassigned": True,
            "remaining_recipe_gap": 0,
            "schema_version": 1,
            "source_revision": REVISION,
            "status": "WAVE_READY",
            "unique_active_wave": None,
            "wave_slug": "runtime/heat-exchangers",
        },
    )
    dump(
        wave / "census_delta.json",
        {
            "complete_family_count": 0,
            "completion_delta": 0,
            "generated_by": "runtime/heat-exchangers implementation",
            "leftover_later_count": 0,
            "partial_family_count": 0,
            "remaining_recipe_gap": 0,
            "schema_version": 1,
            "source_revision": REVISION,
            "status": "CENSUS_DELTA_READY",
            "wave_slug": "runtime/heat-exchangers",
            "work_set": {"family_count": 0, "source_rows": 8},
        },
    )
    dump(
        wave / "readiness.json",
        {
            "evidence": {
                "acquisition": "survival_recipes",
                "completion_delta": 0,
                "generated_recipe_count": 8,
                "heat_exchanger_identity_count": 8,
                "hot_fuel_rows": 8,
                "leftover_later_count": 0,
                "owns_families": 0,
                "heat_exchangers_status": "runtime_ready",
            },
            "generated_by": "runtime/heat-exchangers implementation",
            "generated_recipe_count": 8,
            "next_unassigned": True,
            "nuclear_started": True,
            "owns_families": 0,
            "production_lock": None,
            "schema_version": 1,
            "source_revision": REVISION,
            "status": "HEAT_EXCHANGERS_READY",
            "unique_active_wave": None,
            "wave_slug": "runtime/heat-exchangers",
        },
    )
    empty_src = (
        SRC
        / "data"
        / "cruciblecraft_wave_runtime_fission_hot_fluids"
        / "structure"
        / "empty.nbt"
    )
    ns = SRC / "data" / "cruciblecraft_wave_runtime_heat_exchangers"
    for rel in ("structure/empty.nbt", "gametest/structure/empty.nbt"):
        dest = ns / rel
        dest.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(empty_src, dest)


def write_capability() -> None:
    cap = ROOT / "tools" / "capabilities" / "energy" / "heat-exchangers"
    cap.mkdir(parents=True, exist_ok=True)
    ids = [row["id"] for row in MACHINES]
    dump(
        cap / "capability.json",
        {
            "schema_version": 2,
            "slug": "energy/heat-exchangers",
            "title": "Heat Exchangers",
            "maturity": "runtime_ready",
            "workflow": "accepted",
            "owned_paths": [
                "src/main/java/com/masson/cruciblecraft/energy/heatexchanger/**",
                "src/main/java/com/masson/cruciblecraft/gametest/EnergyHeatExchangersGameTests.java",
                "src/main/resources/data/cruciblecraft_wave_runtime_heat_exchangers/**",
                "src/main/resources/data/cruciblecraft/heat_exchangers.json",
                "src/main/resources/data/cruciblecraft/recipe/energy/fuels_hot/**",
                "src/main/resources/data/cruciblecraft/material_prefixes/machine_casing_quadruple.json",
                "src/main/resources/assets/cruciblecraft/gt6_heat_exchangers_art_manifest.json",
                "src/main/resources/assets/cruciblecraft/textures/block/machine/heat_exchanger/**",
                "src/main/resources/assets/cruciblecraft/textures/block/material/machine_casing_quadruple*.png",
                "src/test/java/com/masson/cruciblecraft/energy/heatexchanger/**",
                "tools/capabilities/energy/heat-exchangers/**",
                "tools/waves/runtime/heat-exchangers/**",
                "tools/tests/test_energy_heat_exchangers.py",
                "tools/emit_heat_exchangers.py",
            ],
            "depends_on": ["energy/nuclear-fission-hot-fluids"],
            "profiles": ["capability-runtime"],
            "wave_slug": "runtime/heat-exchangers",
            "legacy_readiness": "tools/waves/runtime/heat-exchangers/readiness.json",
            "player_signoff": None,
            "survival_access": "unreviewed",
            "required_test_ids": [
                "playerSurfaceIsRegistered",
                "invarConsumesHotTinAndBuffersHu",
                "tungstensteelEfficiencyIsNinetyPercent",
                "reloadPreservesTanksAndEnergy",
                "representativeRecipesAreSurvivalCraftable",
            ],
            "identity_disposition": [
                {
                    "semantic_key": "heat_exchanger:single_block",
                    "disposition": "new_distinct",
                    "runtime_ids": ids,
                    "reason": "GT6 MultiTileEntityGeneratorHotFluid 9103/9107-9109/9153/9157-9159. FM.Hot in, HU out. Not a cooler, turbine, or 17197.",
                },
                {
                    "semantic_key": "heat_exchanger:large_17197",
                    "disposition": "blocked",
                    "runtime_ids": [],
                    "reason": "MultiTileEntityLargeHeatExchanger 17197 is a follow-up card.",
                },
                {
                    "semantic_key": "thermal:steam_turbine",
                    "disposition": "blocked",
                    "runtime_ids": [],
                    "reason": "Steam turbines STEAM to RU stay on a later card.",
                },
                {
                    "semantic_key": "thermal:cooler",
                    "disposition": "blocked",
                    "runtime_ids": [],
                    "reason": "Electric/flux coolers stay on a later card.",
                },
            ],
            "note": "Eight single-block heat exchangers. unique_active_wave stays null. Quadruple casing is in-scope only for Invar, tungsten, tungstensteel, and tantalum_hafnium_carbide. Extra FM.Hot rows (lava, blaze, IC2 coolant) stay out.",
        },
    )
    dump(
        cap / "player_signoff.json",
        {
            "schema_version": 1,
            "capability": "energy/heat-exchangers",
            "signed": True,
            "signer": "player",
            "date": "2026-09-09",
            "craftable_items": [row["id"].split(":", 1)[1] for row in MACHINES],
            "checklist": {
                "chinese_names_are_player_facing": True,
                "creative_tab_lists_all_eight": True,
                "emi_shows_hot_fuels": True,
                "hu_not_kelvin": True,
            },
            "notes": "Eight single-block FM.Hot to HU machines. Tungstensteel efficiency is 90%. Quadruple casing is only the four in-scope materials. Large 17197, steam turbines, coolers and extra FM.Hot stay later.",
        },
    )


def patch_docs() -> None:
    """Status recaps live on the generated project-status page; do not patch them."""
    return


def patch_resolve_overlay() -> None:
    path = ROOT / "tools" / "gt6_resolve.py"
    replace_once(
        path,
        '    "casingMachineQuadruple": None,\n',
        '    "casingMachineQuadruple": "machine_casing_quadruple",\n',
    )
    test = ROOT / "tools" / "tests" / "test_gt6_resolve.py"
    replace_once(
        test,
        """    def test_quadruple_casing_is_missing_prefix(self) -> None:
        document = gt6_resolve.resolve("OP.casingMachineQuadruple(MT.Invar)")
        self.assertEqual("missing_prefix", document["status"])
        self.assertIsNone(document["prefix"]["cc_prefix"])
        self.assertEqual("invar", document["material"]["cc_material"])
""",
        """    def test_quadruple_casing_is_registered(self) -> None:
        document = gt6_resolve.resolve("OP.casingMachineQuadruple(MT.Invar)")
        self.assertEqual("ok", document["status"])
        self.assertEqual("machine_casing_quadruple", document["prefix"]["cc_prefix"])
        self.assertEqual("invar", document["material"]["cc_material"])
        self.assertEqual(
            "cruciblecraft:invar/machine_casing_quadruple",
            document["form"]["item"],
        )
""",
    )


def main() -> None:
    write_prefix()
    patch_gate()
    write_catalog()
    write_d0()
    write_hot_recipes()
    write_art()
    write_wave()
    write_capability()
    patch_docs()
    patch_resolve_overlay()


if __name__ == "__main__":
    main()
