#!/usr/bin/env python3
"""Emit fission-survival wave artifacts, nuclear recipes, art, and form includes."""
from __future__ import annotations

import json
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
REV = "3703e40308c8c030763fd6297dea8b210d2a77b1"
WAVE = ROOT / "tools" / "waves" / "runtime" / "fission-survival"
SRC = ROOT / "src" / "main" / "resources"
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
CC_ASSETS = SRC / "assets" / "cruciblecraft"
RECIPE = SRC / "data" / "cruciblecraft" / "recipe"


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
            "cohort": "fission-survival",
            "depends_on": ["runtime/batteries"],
            "generated_by": "runtime/fission-survival implementation",
            "owns_families": 0,
            "program": "runtime/fission-survival",
            "schema_version": 1,
            "wave_slug": "runtime/fission-survival",
        },
    )
    dump(
        WAVE / "topology.json",
        {
            "append_only": False,
            "complete_family_count": 0,
            "generated_by": "runtime/fission-survival implementation",
            "next_unassigned": True,
            "remaining_recipe_gap": 0,
            "schema_version": 1,
            "source_revision": REV,
            "status": "WAVE_READY",
            "unique_active_wave": None,
            "wave_slug": "runtime/fission-survival",
        },
    )
    dump(
        WAVE / "readiness.json",
        {
            "evidence": {
                "acquisition": "survival_recipes",
                "canner_machine_tiers": 1,
                "core_identity_count": 2,
                "fission_survival_status": "runtime_ready",
                "nuclear_source_relations": 48,
                "rod_identity_count": 46,
                "rod_kind_count": 8,
            },
            "generated_by": "runtime/fission-survival implementation",
            "generated_recipe_count": 0,
            "next_unassigned": True,
            "nuclear_started": True,
            "owns_families": 0,
            "production_lock": None,
            "schema_version": 1,
            "source_revision": REV,
            "status": "FISSION_SURVIVAL_READY",
            "unique_active_wave": None,
            "wave_slug": "runtime/fission-survival",
        },
    )
    dump(
        WAVE / "census_delta.json",
        {
            "complete_family_count": 0,
            "completion_delta": 0,
            "generated_by": "runtime/fission-survival implementation",
            "leftover_later_count": 0,
            "partial_family_count": 0,
            "remaining_recipe_gap": 0,
            "schema_version": 1,
            "source_revision": REV,
            "status": "CENSUS_DELTA_IN_PROGRESS",
            "wave_slug": "runtime/fission-survival",
            "work_set": {"family_count": 0, "source_rows": 48},
        },
    )
    dump(
        WAVE / "load_axis.json",
        {
            "canner_machine_tiers": 1,
            "generated_by": "runtime/fission-survival implementation",
            "nuclear_source_relations": 48,
            "rod_identity_count": 46,
            "schema_version": 1,
            "source_revision": REV,
            "wave_slug": "runtime/fission-survival",
        },
    )


def relations() -> list[dict]:
    rows: list[dict] = []

    def add(
        index: int,
        source_id: int,
        relation: str,
        recipe_id: str,
        machine: str,
        target: str,
        status: str = "ready",
    ) -> None:
        rows.append(
            {
                "index": index,
                "source_id": source_id,
                "relation": relation,
                "recipe_id": recipe_id,
                "machine": machine,
                "target": target,
                "status": status,
            }
        )

    add(1, 9201, "empty_rod_extruder", "nuclear/empty_reactor_rod", "extruder", "empty_reactor_rod")
    add(2, 9202, "utility_canner", "nuclear/neutron_absorber_rod", "canner", "neutron_absorber_rod")
    add(3, 9203, "utility_canner", "nuclear/neutron_reflector_rod", "canner", "neutron_reflector_rod")
    add(4, 9204, "utility_canner", "nuclear/neutron_moderator_rod", "canner", "neutron_moderator_rod")
    fuels = [
        (9210, "thorium232_fuel_rod"),
        (9219, "cyanite_fuel_rod"),
        (9220, "uranium238_fuel_rod"),
        (9221, "uranium235_fuel_rod"),
        (9222, "uranium233_fuel_rod"),
        (9229, "yellorium_fuel_rod"),
        (9230, "plutonium244_fuel_rod"),
        (9231, "plutonium241_fuel_rod"),
        (9232, "plutonium243_fuel_rod"),
        (9233, "plutonium239_fuel_rod"),
        (9239, "blutonium_fuel_rod"),
        (9240, "americium245_fuel_rod"),
        (9241, "americium241_fuel_rod"),
        (9249, "ludicrite_fuel_rod"),
        (9250, "cobalt60_fuel_rod"),
        (9260, "enriched_naquadah_fuel_rod"),
        (9261, "naquadria_fuel_rod"),
    ]
    for offset, (sid, rod) in enumerate(fuels, start=5):
        add(offset, sid, "fuel_canner", f"nuclear/{rod}", "canner", rod)
    breeders = [
        (9410, "thorium232_breeder_rod"),
        (9420, "uranium238_breeder_rod"),
        (9430, "lithium_breeder_rod"),
        (9440, "naquadah_breeder_rod"),
    ]
    for offset, (sid, rod) in enumerate(breeders, start=22):
        add(offset, sid, "breeder_canner", f"nuclear/{rod}", "canner", rod)
    depleted = [
        (9310, "thorium232_depleted_rod"),
        (9319, "cyanite_depleted_rod"),
        (9320, "uranium238_depleted_rod"),
        (9321, "uranium235_depleted_rod"),
        (9322, "uranium233_depleted_rod"),
        (9329, "yellorium_depleted_rod"),
        (9330, "plutonium244_depleted_rod"),
        (9331, "plutonium241_depleted_rod"),
        (9332, "plutonium243_depleted_rod"),
        (9333, "plutonium239_depleted_rod"),
        (9339, "blutonium_depleted_rod"),
        (9340, "americium245_depleted_rod"),
        (9341, "americium241_depleted_rod"),
        (9349, "ludicrite_depleted_rod"),
        (9350, "cobalt60_depleted_rod"),
        (9360, "enriched_naquadah_depleted_rod"),
        (9361, "naquadria_depleted_rod"),
    ]
    for offset, (sid, rod) in enumerate(depleted, start=26):
        add(offset, sid, "depleted_centrifuge", f"nuclear/{rod}_recovery", "centrifuge", rod)
    add(43, 9411, "product_centrifuge", "nuclear/uranium233_enriched_rod_recovery", "centrifuge", "uranium233_enriched_rod")
    add(44, 9421, "product_centrifuge", "nuclear/plutonium239_enriched_rod_recovery", "centrifuge", "plutonium239_enriched_rod")
    add(45, 9431, "tritium_canner_unload", "nuclear/tritium_enriched_rod_unload", "canner", "tritium_enriched_rod")
    add(46, 9441, "product_centrifuge", "nuclear/enriched_naquadah_enriched_rod_recovery", "centrifuge", "enriched_naquadah_enriched_rod")
    add(47, 9300, "core_shaped", "reactor_core_1x1", "shaped_catalyst", "reactor_core_1x1")
    add(48, 9200, "core_shaped", "reactor_core_2x2", "shaped_catalyst", "reactor_core_2x2")
    assert len(rows) == 48, len(rows)
    ids = [row["recipe_id"] for row in rows]
    assert len(ids) == len(set(ids))
    return rows


def write_d0(rows: list[dict]) -> None:
    dump(
        WAVE / "d0_nuclear_source_matrix.json",
        {
            "schema_version": 1,
            "source_revision": REV,
            "wave_slug": "runtime/fission-survival",
            "nuclear_source_relations": 48,
            "missing": 0,
            "extra": 0,
            "duplicate": 0,
            "status": "D0_MATRIX_READY",
            "relations": rows,
        },
    )


def write_required_forms() -> None:
    required: dict[str, list[str]] = {
        "zirconium": ["scrap", "ingot"],
        "lead": ["machine_casing_dense", "dense_plate", "long_rod"],
        "graphite": ["rod"],
        "beryllium": ["rod"],
        "cd_in_ag_alloy": ["rod"],
        "lithium": ["bolt"],
        "thorium": ["rod", "bolt", "tiny_dust", "dust_div72"],
        "uranium": ["rod", "bolt", "tiny_dust", "dust_div72"],
        "naquadah": ["rod", "bolt", "tiny_dust", "dust_div72"],
        "cyanite": ["rod", "tiny_dust", "dust_div72"],
        "uranium235": ["rod", "tiny_dust", "dust_div72"],
        "uranium233": ["rod", "tiny_dust", "dust_div72"],
        "yellorium": ["rod", "tiny_dust", "dust_div72"],
        "plutonium": ["rod", "tiny_dust", "dust_div72"],
        "plutonium241": ["rod", "tiny_dust", "dust_div72"],
        "plutonium243": ["rod", "tiny_dust", "dust_div72"],
        "plutonium239": ["rod", "tiny_dust", "dust_div72"],
        "blutonium": ["rod", "tiny_dust", "dust_div72"],
        "americium": ["rod", "tiny_dust", "dust_div72"],
        "americium241": ["rod", "tiny_dust", "dust_div72"],
        "ludicrite": ["rod", "tiny_dust", "dust_div72"],
        "cobalt60": ["rod", "tiny_dust", "dust_div72"],
        "naquadah_enriched": ["rod", "tiny_dust", "dust_div72"],
        "naquadria": ["rod", "tiny_dust", "dust_div72"],
        "ruby": ["lens"],
        "silicon": ["tiny_plate_gem"],
    }
    dump(
        WAVE / "required_forms.json",
        {
            "counts": {
                "required_form_pairs": sum(len(v) for v in required.values()),
                "required_materials": len(required),
            },
            "generated_by": "runtime/fission-survival implementation",
            "note": "Card builder writes required forms only. material_registration_gate.json is produced by tools/build_gt6_material_form_gate.py.",
            "required_forms": required,
            "schema_version": 1,
            "source_revision": REV,
            "status": "SEMANTIC_REQUIRED_FORMS",
            "wave_slug": "runtime/fission-survival",
        },
    )


def patch_authority() -> None:
    path = ROOT / "tools" / "material_form_authority.json"
    doc = json.loads(path.read_text(encoding="utf-8"))
    section = "fission_survival_required_forms"
    if section not in doc["java_overlay_sections"]:
        doc["java_overlay_sections"].append(section)
    if not any(src.get("id") == "fission_survival_required_forms" for src in doc["sources"]):
        doc["sources"].append(
            {
                "extra_factual_forms": [
                    "dust",
                    "dust_div72",
                    "lens",
                    "machine_casing_dense",
                    "scrap",
                    "tiny_dust",
                    "tiny_plate_gem",
                ],
                "field": "required_forms",
                "gate_section": section,
                "id": "fission_survival_required_forms",
                "java_runtime_visible": True,
                "owner": "runtime/fission-survival",
                "path": "tools/waves/runtime/fission-survival/required_forms.json",
                "required_factual_prereqs": {"dust_div72": ["dust"]},
            }
        )
    dump(path, doc)


def add_include(material: str, prefixes: list[str]) -> None:
    path = SRC / "data" / "cruciblecraft" / "materials" / f"{material}.json"
    text = path.read_text(encoding="utf-8")
    data = json.loads(text)
    existing = list(data.get("include_prefixes") or [])
    changed = False
    for prefix in prefixes:
        if prefix not in existing:
            existing.append(prefix)
            changed = True
    if not changed and "include_prefixes" in data:
        return
    if "include_prefixes" in data:
        start = text.index('"include_prefixes"')
        bracket = text.index("[", start)
        end = text.index("]", bracket)
        inner = text[bracket + 1 : end]
        rebuilt = ",\n".join(f'    "{item}"' for item in existing)
        path.write_text(
            text[: bracket + 1] + "\n" + rebuilt + "\n  " + text[end:],
            encoding="utf-8",
        )
        return
    color_line = None
    lines = text.splitlines(keepends=True)
    for index, line in enumerate(lines):
        if line.strip().startswith('"color"'):
            color_line = index
            break
    if color_line is None:
        raise SystemExit(f"no color field in {material}")
    block = '  "include_prefixes": [\n' + "".join(
        f'    "{prefix}"' + ("," if i + 1 < len(existing) else "") + "\n"
        for i, prefix in enumerate(existing)
    ) + "  ],\n"
    lines.insert(color_line + 1, block)
    path.write_text("".join(lines), encoding="utf-8")


def patch_lead_flag() -> None:
    path = SRC / "data" / "cruciblecraft" / "materials" / "lead.json"
    text = path.read_text(encoding="utf-8")
    flag = '"cruciblecraft:generates_machine_casing_dense"'
    if flag in text:
        return
    needle = '"cruciblecraft:generates_machine_casing"'
    if needle not in text:
        raise SystemExit("lead generation_flags missing machine_casing")
    path.write_text(
        text.replace(needle, needle + ",\n    " + flag, 1),
        encoding="utf-8",
    )


def write_prefix() -> None:
    dump(
        SRC / "data" / "cruciblecraft" / "material_prefixes" / "machine_casing_dense.json",
        {
            "aliases": ["casingMachineDense"],
            "capabilities": [],
            "generation_flag": "cruciblecraft:generates_machine_casing_dense",
            "id": "cruciblecraft:machine_casing_dense",
            "model_template": "minecraft:item/generated",
            "model_texture": "cruciblecraft:block/material/machine_casing_dense",
            "serialized_path": "machine_casing_dense",
            "tag_directory": "machine_casing_denses",
            "tag_namespace": "c",
            "units": 8064,
        },
    )
    index_path = SRC / "data" / "cruciblecraft" / "material_prefixes" / "index.json"
    index = json.loads(index_path.read_text(encoding="utf-8"))
    if "machine_casing_dense.json" not in index:
        index.append("machine_casing_dense.json")
        dump(index_path, index)


def gt_recipe(
    path: str,
    recipe_map: str,
    duration: int,
    eut: int,
    item_inputs: list[tuple[str, int]],
    item_outputs: list[tuple[str, int]],
    fluid_inputs: list[tuple[str, int]] | None = None,
    fluid_outputs: list[tuple[str, int]] | None = None,
    source: str = "",
) -> None:
    payload: dict = {
        "type": "cruciblecraft:gt_recipe",
        "map": f"cruciblecraft:{recipe_map}",
        "duration": duration,
        "eut": eut,
        "can_be_buffered": True,
        "item_inputs": [{"item": item} for item, _count in item_inputs],
        "item_input_counts": [count for _item, count in item_inputs],
        "item_outputs": [
            {"id": item, "count": count} for item, count in item_outputs
        ],
        "output_chances": [10000] * len(item_outputs),
        "provenance": {
            "selected_source_recipe": source or path,
            "source_kind": "gt6_source",
        },
    }
    if fluid_inputs:
        payload["fluid_inputs"] = [
            {"id": fluid, "amount": amount} for fluid, amount in fluid_inputs
        ]
    if fluid_outputs:
        payload["fluid_outputs"] = [
            {"id": fluid, "amount": amount} for fluid, amount in fluid_outputs
        ]
    dump(RECIPE / f"{path}.json", payload)


def shaped(
    path: str,
    pattern: list[str],
    ingredients: dict[str, str],
    result: str,
    catalysts: dict[str, str] | None = None,
    count: int = 1,
) -> None:
    payload: dict = {
        "type": "cruciblecraft:shaped_catalyst",
        "pattern": pattern,
        "ingredients": {key: {"item": value} for key, value in ingredients.items()},
        "result": {"id": result, "count": count},
    }
    if catalysts:
        payload["catalysts"] = {key: {"item": value} for key, value in catalysts.items()}
    dump(RECIPE / f"{path}.json", payload)


def write_nuclear_recipes() -> None:
    gt_recipe(
        "nuclear/empty_reactor_rod",
        "extruder",
        64,
        96,
        [
            ("cruciblecraft:zirconium/ingot", 1),
            ("cruciblecraft:extruder_shape_cell", 0),
        ],
        [("cruciblecraft:empty_reactor_rod", 1)],
        source="Loader_Recipes_Extruder.java:288-293",
    )
    utility = [
        ("neutron_absorber_rod", "cd_in_ag_alloy"),
        ("neutron_reflector_rod", "beryllium"),
        ("neutron_moderator_rod", "graphite"),
    ]
    for rod, material in utility:
        gt_recipe(
            f"nuclear/{rod}",
            "canner",
            16,
            16,
            [
                (f"cruciblecraft:{material}/rod", 1),
                ("cruciblecraft:empty_reactor_rod", 1),
            ],
            [(f"cruciblecraft:{rod}", 1)],
        )
    fuels = [
        ("thorium232_fuel_rod", "thorium"),
        ("cyanite_fuel_rod", "cyanite"),
        ("uranium238_fuel_rod", "uranium"),
        ("uranium235_fuel_rod", "uranium235"),
        ("uranium233_fuel_rod", "uranium233"),
        ("yellorium_fuel_rod", "yellorium"),
        ("plutonium244_fuel_rod", "plutonium"),
        ("plutonium241_fuel_rod", "plutonium241"),
        ("plutonium243_fuel_rod", "plutonium243"),
        ("plutonium239_fuel_rod", "plutonium239"),
        ("blutonium_fuel_rod", "blutonium"),
        ("americium245_fuel_rod", "americium"),
        ("americium241_fuel_rod", "americium241"),
        ("ludicrite_fuel_rod", "ludicrite"),
        ("cobalt60_fuel_rod", "cobalt60"),
        ("enriched_naquadah_fuel_rod", "naquadah_enriched"),
        ("naquadria_fuel_rod", "naquadria"),
    ]
    for rod, material in fuels:
        gt_recipe(
            f"nuclear/{rod}",
            "canner",
            16,
            16,
            [
                (f"cruciblecraft:{material}/rod", 1),
                ("cruciblecraft:empty_reactor_rod", 1),
            ],
            [(f"cruciblecraft:{rod}", 1)],
        )
    breeders = [
        ("thorium232_breeder_rod", "thorium"),
        ("uranium238_breeder_rod", "uranium"),
        ("lithium_breeder_rod", "lithium"),
        ("naquadah_breeder_rod", "naquadah"),
    ]
    for rod, material in breeders:
        gt_recipe(
            f"nuclear/{rod}",
            "canner",
            16,
            16,
            [
                (f"cruciblecraft:{material}/bolt", 4),
                ("cruciblecraft:empty_reactor_rod", 1),
            ],
            [(f"cruciblecraft:{rod}", 1)],
        )
    depleted = [
        ("thorium232_depleted_rod", "thorium", "uranium"),
        ("cyanite_depleted_rod", "cyanite", "blutonium"),
        ("uranium238_depleted_rod", "uranium", "uranium235"),
        ("uranium235_depleted_rod", "uranium235", "plutonium"),
        ("uranium233_depleted_rod", "uranium233", "plutonium243"),
        ("yellorium_depleted_rod", "yellorium", "cyanite"),
        ("plutonium244_depleted_rod", "plutonium", "plutonium241"),
        ("plutonium241_depleted_rod", "plutonium241", "plutonium243"),
        ("plutonium243_depleted_rod", "plutonium243", "americium"),
        ("plutonium239_depleted_rod", "plutonium239", "americium241"),
        ("blutonium_depleted_rod", "blutonium", "ludicrite"),
        ("americium245_depleted_rod", "americium", "americium241"),
        ("americium241_depleted_rod", "americium241", "naquadah_enriched"),
        ("ludicrite_depleted_rod", "ludicrite", "yellorium"),
        ("cobalt60_depleted_rod", "cobalt60", "thorium"),
        ("enriched_naquadah_depleted_rod", "naquadah_enriched", "naquadria"),
        ("naquadria_depleted_rod", "naquadria", "cobalt60"),
    ]
    for rod, fuel, byproduct in depleted:
        gt_recipe(
            f"nuclear/{rod}_recovery",
            "centrifuge",
            256,
            64,
            [(f"cruciblecraft:{rod}", 1)],
            [
                ("cruciblecraft:zirconium/scrap", 9),
                (f"cruciblecraft:{fuel}/tiny_dust", 1),
                (f"cruciblecraft:{byproduct}/dust_div72", 6),
            ],
        )
    products = [
        ("uranium233_enriched_rod", "uranium233", "thorium", 64),
        ("plutonium239_enriched_rod", "plutonium239", "uranium", 64),
        ("enriched_naquadah_enriched_rod", "naquadah_enriched", "naquadah", 512),
    ]
    for rod, product, parent, eut in products:
        gt_recipe(
            f"nuclear/{rod}_recovery",
            "centrifuge",
            256,
            eut,
            [(f"cruciblecraft:{rod}", 1)],
            [
                ("cruciblecraft:zirconium/scrap", 9),
                (f"cruciblecraft:{product}/tiny_dust", 4),
                (f"cruciblecraft:{parent}/dust_div72", 4),
            ],
        )
    gt_recipe(
        "nuclear/tritium_enriched_rod_unload",
        "canner",
        16,
        16,
        [("cruciblecraft:tritium_enriched_rod", 1)],
        [("cruciblecraft:empty_reactor_rod", 1)],
        fluid_outputs=[("cruciblecraft:tritium", 500)],
    )
    shaped(
        "reactor_core_1x1",
        ["CP ", "wM ", "   "],
        {
            "C": "cruciblecraft:circuit_master",
            "P": "cruciblecraft:compact_electric_piston_ev",
            "M": "cruciblecraft:lead/machine_casing_dense",
        },
        "cruciblecraft:reactor_core_1x1",
        catalysts={"w": "cruciblecraft:material_wrench"},
    )
    shaped(
        "reactor_core_2x2",
        ["PCP", "CMC", "PCP"],
        {
            "C": "cruciblecraft:circuit_master",
            "P": "cruciblecraft:compact_electric_piston_ev",
            "M": "cruciblecraft:lead/machine_casing_dense",
        },
        "cruciblecraft:reactor_core_2x2",
    )


def write_circuit_and_machine_recipes() -> None:
    gt_recipe(
        "machine/press/circuit_plate_empty",
        "press",
        64,
        16,
        [
            ("cruciblecraft:plastic/plate", 1),
            ("cruciblecraft:silicon_dioxide/dust", 1),
        ],
        [("cruciblecraft:circuit_plate_empty", 1)],
        source="MultiItemTechnological.java:30000",
    )
    shaped(
        "circuit_wire_copper",
        ["WWW", "WxW", "WWW"],
        {"W": "cruciblecraft:copper/fine_wire"},
        "cruciblecraft:circuit_wire_copper",
        catalysts={"x": "cruciblecraft:material_wire_cutter"},
    )
    gt_recipe(
        "machine/press/circuit_plate_copper",
        "press",
        64,
        16,
        [
            ("cruciblecraft:circuit_plate_empty", 1),
            ("cruciblecraft:circuit_wire_copper", 1),
        ],
        [("cruciblecraft:circuit_plate_copper", 1)],
    )
    gt_recipe(
        "machine/press/circuit_part_basic",
        "press",
        16,
        16,
        [
            ("cruciblecraft:copper/fine_wire", 1),
            ("cruciblecraft:redstone/tiny_dust", 1),
            ("cruciblecraft:silicon/tiny_plate_gem", 1),
        ],
        [("cruciblecraft:circuit_part_basic", 1)],
    )
    gt_recipe(
        "machine/press/circuit_board_basic",
        "press",
        64,
        16,
        [
            ("cruciblecraft:circuit_plate_copper", 1),
            ("cruciblecraft:circuit_part_basic", 4),
        ],
        [("cruciblecraft:circuit_board_basic", 1)],
    )
    for fluid in ("molten_lead", "molten_tin", "molten_soldering_alloy"):
        gt_recipe(
            f"machine/bath/circuit_basic_{fluid}",
            "bath",
            64,
            0,
            [("cruciblecraft:circuit_board_basic", 1)],
            [("cruciblecraft:circuit_basic", 1)],
            fluid_inputs=[(f"cruciblecraft:{fluid}", 72)],
        )
    gt_recipe(
        "machine/laser_engraver/circuit_wire_platinum",
        "laser_engraver",
        64,
        16,
        [
            ("cruciblecraft:platinum/foil", 4),
            ("cruciblecraft:ruby/lens", 0),
        ],
        [("cruciblecraft:circuit_wire_platinum", 1)],
        source="Loader_Recipes_Other.java:160",
    )
    gt_recipe(
        "machine/press/circuit_plate_platinum",
        "press",
        64,
        16,
        [
            ("cruciblecraft:circuit_plate_empty", 1),
            ("cruciblecraft:circuit_wire_platinum", 1),
        ],
        [("cruciblecraft:circuit_plate_platinum", 1)],
    )
    gt_recipe(
        "machine/press/circuit_part_master",
        "press",
        16,
        16,
        [
            ("cruciblecraft:platinum/fine_wire", 1),
            ("cruciblecraft:signalum/fine_wire", 1),
            ("cruciblecraft:silicon/tiny_plate_gem", 1),
        ],
        [("cruciblecraft:circuit_part_master", 1)],
    )
    gt_recipe(
        "machine/press/circuit_board_master",
        "press",
        64,
        16,
        [
            ("cruciblecraft:circuit_plate_platinum", 1),
            ("cruciblecraft:circuit_part_master", 4),
        ],
        [("cruciblecraft:circuit_board_master", 1)],
    )
    gt_recipe(
        "machine/bath/circuit_master_molten_soldering_alloy",
        "bath",
        64,
        0,
        [("cruciblecraft:circuit_board_master", 1)],
        [("cruciblecraft:circuit_master", 1)],
        fluid_inputs=[("cruciblecraft:molten_soldering_alloy", 72)],
    )
    for suffix, magnet in (
        ("iron_magnetic", "iron_magnetic"),
        ("steel_magnetic", "steel_magnetic"),
    ):
        shaped(
            f"compact_electric_motor_lv_{suffix}",
            ["CWR", "WIW", "PWC"],
            {
                "I": f"cruciblecraft:{magnet}/rod",
                "P": "cruciblecraft:steel_galvanized/curved_plate",
                "R": "cruciblecraft:steel_galvanized/rod",
                "W": "cruciblecraft:copper/wire",
                "C": "cruciblecraft:tin/cable",
            },
            "cruciblecraft:compact_electric_motor_lv",
        )
    shaped(
        "compact_electric_motor_ev",
        ["CWR", "WIW", "PWC"],
        {
            "I": "cruciblecraft:neodymium_magnetic/rod",
            "P": "cruciblecraft:chromium/curved_plate",
            "R": "cruciblecraft:chromium/rod",
            "W": "cruciblecraft:annealed_copper/quadruple_wire",
            "C": "cruciblecraft:aluminium/cable",
        },
        "cruciblecraft:compact_electric_motor_ev",
    )
    shaped(
        "compact_electric_piston_ev",
        ["TPP", "dSS", "TMG"],
        {
            "T": "cruciblecraft:chromium/screw",
            "P": "cruciblecraft:chromium/plate",
            "S": "cruciblecraft:chromium/rod",
            "M": "cruciblecraft:compact_electric_motor_ev",
            "G": "cruciblecraft:chromium/small_gear",
        },
        "cruciblecraft:compact_electric_piston_ev",
        catalysts={"d": "cruciblecraft:material_screwdriver"},
    )
    shaped(
        "compact_electric_pump_lv",
        ["TXO", "dPw", "OMT"],
        {
            "T": "cruciblecraft:steel_galvanized/screw",
            "X": "cruciblecraft:steel_galvanized/rotor",
            "O": "cruciblecraft:rubber/ring",
            "P": "cruciblecraft:steel_galvanized/curved_plate",
            "M": "cruciblecraft:compact_electric_motor_lv",
        },
        "cruciblecraft:compact_electric_pump_lv",
        catalysts={
            "d": "cruciblecraft:material_screwdriver",
            "w": "cruciblecraft:material_wrench",
        },
    )
    shaped(
        "machines/laser_engraver",
        ["TdT", "GPG", "CMC"],
        {
            "T": "cruciblecraft:steel_galvanized/screw",
            "G": "cruciblecraft:steel_galvanized/small_gear",
            "P": "minecraft:terracotta",
            "C": "cruciblecraft:circuit_basic",
            "M": "cruciblecraft:steel_galvanized/machine_casing",
        },
        "cruciblecraft:laser_engraver",
        catalysts={"d": "cruciblecraft:material_screwdriver"},
    )


def write_parts_catalog() -> None:
    parts = [
        ("compact_electric_motor_lv", 12001, "Compact Electric Motor (LV)", "紧凑电动机（LV）"),
        ("compact_electric_motor_ev", 12004, "Compact Electric Motor (EV)", "紧凑电动机（EV）"),
        ("compact_electric_piston_ev", 12064, "Compact Electric Piston (EV)", "紧凑电动活塞（EV）"),
        ("circuit_plate_empty", 30000, "Circuit Plate", "电路板"),
        ("circuit_wire_copper", 30001, "Circuit Wiring (Copper)", "电路布线（铜）"),
        ("circuit_plate_copper", 30002, "Circuit Plate (Copper)", "电路板（铜）"),
        ("circuit_wire_platinum", 30005, "Circuit Wiring (Platinum)", "电路布线（铂）"),
        ("circuit_plate_platinum", 30006, "Circuit Plate (Platinum)", "电路板（铂）"),
        ("circuit_part_basic", 30101, "Circuit Part (Basic)", "电路元件（基础）"),
        ("circuit_part_master", 30105, "Circuit Part (Master)", "电路元件（大师）"),
        ("circuit_board_basic", 30201, "Circuit Board (Basic)", "电路基板（基础）"),
        ("circuit_board_master", 30205, "Circuit Board (Master)", "电路基板（大师）"),
        ("circuit_basic", 30301, "Circuit T1 (Basic)", "电路 T1（基础）"),
        ("circuit_master", 30305, "Circuit T5 (Master)", "电路 T5（大师）"),
    ]
    dump(
        SRC / "data" / "cruciblecraft" / "technological_parts.json",
        {
            "schema_version": 1,
            "source_revision": REV,
            "status": "TECHNOLOGICAL_PARTS",
            "parts": [
                {
                    "id": f"cruciblecraft:{path}",
                    "registry_path": path,
                    "source_id": source_id,
                    "english_name": english,
                    "chinese_name": chinese,
                    "texture": path,
                }
                for path, source_id, english, chinese in parts
            ],
        },
    )
    return parts


def copy_art(parts: list[tuple]) -> None:
    imports = []
    tech_src = GT6_ART / "items" / "gt.multiitem.technological"
    tech_dst = CC_ASSETS / "textures" / "item" / "gt6_import"
    tech_dst.mkdir(parents=True, exist_ok=True)
    for path, source_id, _en, _zh in parts:
        src = tech_src / f"{source_id}.png"
        dst = tech_dst / f"{path}.png"
        shutil.copy2(src, dst)
        imports.append(
            {
                "gt6_source": f"assets/gregtech/textures/items/gt.multiitem.technological/{source_id}.png",
                "destination": f"assets/cruciblecraft/textures/item/gt6_import/{path}.png",
            }
        )
    for name, overlay in (
        ("casingmachinedense.png", "machine_casing_dense.png"),
        ("casingmachinedense_overlay.png", "machine_casing_dense_overlay.png"),
    ):
        src = GT6_ART / "blocks" / "materialicons" / "metallic" / name
        dst = CC_ASSETS / "textures" / "block" / "material" / overlay
        dst.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(src, dst)
        imports.append(
            {
                "gt6_source": f"assets/gregtech/textures/blocks/materialicons/metallic/{name}",
                "destination": f"assets/cruciblecraft/textures/block/material/{overlay}",
            }
        )
    canner_src = GT6_ART / "blocks" / "machines" / "basicmachines" / "canner"
    canner_dst = CC_ASSETS / "textures" / "block" / "machine" / "canner"
    for folder in ("colored", "overlay", "overlay_active", "overlay_running"):
        for src in (canner_src / folder).iterdir():
            dst = canner_dst / folder / src.name
            dst.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(src, dst)
        imports.append(
            {
                "gt6_source": f"assets/gregtech/textures/blocks/machines/basicmachines/canner/{folder}/*",
                "destination": f"assets/cruciblecraft/textures/block/machine/canner/{folder}/*",
            }
        )
    dump(
        CC_ASSETS / "gt6_fission_survival_art_manifest.json",
        {
            "source": "gt6_referencable_port_code/gregtech6_w",
            "source_revision": REV,
            "imports": imports,
        },
    )


def main() -> None:
    write_waves()
    rows = relations()
    write_d0(rows)
    write_required_forms()
    patch_authority()
    write_prefix()
    add_include("zirconium", ["scrap"])
    add_include("lead", ["machine_casing_dense"])
    add_include("graphite", ["rod"])
    add_include("beryllium", ["rod"])
    add_include("cd_in_ag_alloy", ["rod"])
    add_include("lithium", ["bolt"])
    for material in (
        "thorium",
        "uranium",
        "naquadah",
        "cyanite",
        "uranium235",
        "uranium233",
        "yellorium",
        "plutonium",
        "plutonium241",
        "plutonium243",
        "plutonium239",
        "blutonium",
        "americium",
        "americium241",
        "ludicrite",
        "cobalt60",
        "naquadah_enriched",
        "naquadria",
    ):
        prefixes = ["tiny_dust", "dust_div72"]
        if material in ("thorium", "uranium", "naquadah"):
            prefixes = ["rod", "bolt", "tiny_dust", "dust_div72"]
        elif material not in ("lithium",):
            prefixes = ["rod", "tiny_dust", "dust_div72"]
        add_include(material, prefixes)
    patch_lead_flag()
    write_nuclear_recipes()
    write_circuit_and_machine_recipes()
    parts = write_parts_catalog()
    copy_art(parts)
    print("emitted fission-survival wave artifacts")


if __name__ == "__main__":
    main()
