#!/usr/bin/env python3
"""Closed-card repair: real aluminium capcellcon + CCC shape + Geiger/Canner chain."""
from __future__ import annotations

import json
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


def insert_lang_before(path: Path, before_key: str, key: str, value: str) -> None:
    text = path.read_text(encoding="utf-8")
    line = f'  "{key}": {json.dumps(value, ensure_ascii=False)},\n'
    if f'  "{key}": ' in text:
        return
    needle = f'  "{before_key}": '
    if needle not in text:
        raise SystemExit(f"missing lang key {before_key} in {path}")
    path.write_text(text.replace(needle, line + needle, 1), encoding="utf-8")


def copy_png(src: Path, dest: Path) -> None:
    if not src.is_file():
        raise SystemExit(f"missing gregtech6_w texture {src}")
    dest.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(src, dest)


def write_prefix() -> None:
    dump(
        SRC / "data" / "cruciblecraft" / "material_prefixes" / "capcellcon.json",
        {
            "aliases": ["capcellcon"],
            "capabilities": [],
            "generation_flag": "cruciblecraft:generates_capcellcon",
            "id": "cruciblecraft:capcellcon",
            "model_template": "minecraft:item/generated",
            "model_texture": "cruciblecraft:item/material/capcellcon",
            "serialized_path": "capcellcon",
            "tag_directory": "capsule_cell_containers",
            "tag_namespace": "c",
            "units": 16,
        },
    )
    index_path = SRC / "data" / "cruciblecraft" / "material_prefixes" / "index.json"
    index_text = index_path.read_text(encoding="utf-8")
    if "capcellcon.json" not in index_text:
        index_path.write_text(
            index_text.replace(
                '  "machine_casing_dense.json"\n]',
                '  "machine_casing_dense.json",\n  "capcellcon.json"\n]',
                1,
            ),
            encoding="utf-8",
        )
    zh_path = SRC / "data" / "cruciblecraft" / "material_zh_cn.json"
    insert_after(
        zh_path,
        '    "bolt": "螺栓",\n',
        '    "capcellcon": "胶囊单元容器",\n',
    )
    add_include("aluminium", ["capcellcon"])
    aluminium = SRC / "data" / "cruciblecraft" / "materials" / "aluminium.json"
    insert_after(
        aluminium,
        '    "cruciblecraft:generates_cable",\n',
        '    "cruciblecraft:generates_capcellcon",\n',
    )


def patch_gate() -> None:
    path = SRC / "data" / "cruciblecraft" / "material_registration_gate.json"
    added = insert_after(
        path,
        '    "aluminium": [\n      "arrow_gt_wood",\n      "billet",\n      "block",\n      "bolt",\n      "cable",\n',
        '      "capcellcon",\n',
    )
    if added:
        text = path.read_text(encoding="utf-8")
        path.write_text(
            text.replace('"registered_forms": 31929', '"registered_forms": 31930', 1),
            encoding="utf-8",
        )
        currentness.write_sidecar(path)


def patch_census() -> None:
    path = SRC / "census" / "runtime_registry_gate.json"
    added = 0
    text = path.read_text(encoding="utf-8")
    if "cruciblecraft:aluminium/capcellcon" not in text:
        if replace_once(
            path,
            '      "cruciblecraft:aluminium/cable",\n      "cruciblecraft:aluminium/dense_plate",',
            '      "cruciblecraft:aluminium/cable",\n      "cruciblecraft:aluminium/capcellcon",\n      "cruciblecraft:aluminium/dense_plate",',
        ):
            added += 1
    if insert_after(
        path,
        '      "cruciblecraft:extruder_shape_bottle",\n',
        '      "cruciblecraft:extruder_shape_ccc",\n',
    ):
        added += 1
    if added:
        text = path.read_text(encoding="utf-8")
        text = text.replace('"items": 17150', f'"items": {17150 + added}', 1)
        text = text.replace(
            '"total_expected_ids": 20564',
            f'"total_expected_ids": {20564 + added}',
            1,
        )
        path.write_text(text, encoding="utf-8")


def write_art() -> None:
    copies = [
        (
            GT6_ART / "items" / "gt.multiitem.technological" / "10028.png",
            SRC / "assets" / "cruciblecraft" / "textures" / "item" / "extruder_shape_ccc.png",
            "assets/gregtech/textures/items/gt.multiitem.technological/10028.png",
            "assets/cruciblecraft/textures/item/extruder_shape_ccc.png",
            "Shape_Extruder_CCC meta 10028. CC unifies SimpleEx 10228 onto this identity.",
        ),
        (
            GT6_ART / "blocks" / "machines" / "tanks" / "cell" / "colored" / "sides.png",
            SRC / "assets" / "cruciblecraft" / "textures" / "item" / "material" / "capcellcon.png",
            "assets/gregtech/textures/blocks/machines/tanks/cell/colored/sides.png",
            "assets/cruciblecraft/textures/item/material/capcellcon.png",
            "OP.capcellcon is MTE MultiTileEntityCell; prefix icon uses the cell side sheet.",
        ),
        (
            GT6_ART / "blocks" / "machines" / "tanks" / "cell" / "overlay" / "sides.png",
            SRC
            / "assets"
            / "cruciblecraft"
            / "textures"
            / "item"
            / "material"
            / "capcellcon_overlay.png",
            "assets/gregtech/textures/blocks/machines/tanks/cell/overlay/sides.png",
            "assets/cruciblecraft/textures/item/material/capcellcon_overlay.png",
            "Matching cell overlay sheet for the aluminium capcellcon prefix.",
        ),
    ]
    imports = []
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
        SRC / "assets" / "cruciblecraft" / "gt6_geiger_obtain_art_manifest.json",
        {
            "source": "gt6_referencable_port_code/gregtech6_w",
            "source_revision": REVISION,
            "source_present": True,
            "imports": imports,
        },
    )


def write_generated_assets() -> None:
    dump(
        GEN / "assets" / "cruciblecraft" / "models" / "item" / "extruder_shape_ccc.json",
        {
            "parent": "minecraft:item/generated",
            "textures": {"layer0": "cruciblecraft:item/extruder_shape_ccc"},
        },
    )
    tags = GEN / "data" / "cruciblecraft" / "tags" / "item" / "extruder_shapes.json"
    insert_after(
        tags,
        '    "cruciblecraft:extruder_shape_cell",\n',
        '    "cruciblecraft:extruder_shape_ccc",\n',
    )
    insert_lang_before(
        GEN / "assets" / "cruciblecraft" / "lang" / "en_us.json",
        "item.cruciblecraft.extruder_shape_cell",
        "item.cruciblecraft.extruder_shape_ccc",
        "Extruder Shape (Capsule-Cell-Container)",
    )
    insert_lang_before(
        GEN / "assets" / "cruciblecraft" / "lang" / "zh_cn.json",
        "item.cruciblecraft.extruder_shape_cell",
        "item.cruciblecraft.extruder_shape_ccc",
        "挤压模具（胶囊单元容器）",
    )
    insert_lang_before(
        GEN / "assets" / "cruciblecraft" / "lang" / "en_us.json",
        "item.cruciblecraft.material_form.centrifuged_crushed_ore",
        "item.cruciblecraft.material_form.capcellcon",
        "%s Capsule Cell Container",
    )
    insert_lang_before(
        GEN / "assets" / "cruciblecraft" / "lang" / "zh_cn.json",
        "item.cruciblecraft.material_form.dense_plate",
        "item.cruciblecraft.material_form.capcellcon",
        "%s 胶囊单元容器",
    )


def gt_recipe(
    path: Path,
    recipe_map: str,
    duration: int,
    eut: int,
    item_inputs: list[dict],
    counts: list[int],
    output_id: str,
    output_count: int,
    provenance: str,
    fluid_inputs: list[dict] | None = None,
) -> None:
    document: dict = {
        "type": "cruciblecraft:gt_recipe",
        "map": recipe_map,
        "duration": duration,
        "eut": eut,
        "can_be_buffered": True,
        "item_inputs": item_inputs,
        "item_input_counts": counts,
        "item_outputs": [{"id": output_id, "count": output_count}],
        "output_chances": [10000],
        "provenance": {
            "selected_source_recipe": provenance,
            "source_kind": "gt6_source",
        },
    }
    if fluid_inputs:
        document["fluid_inputs"] = fluid_inputs
    dump(path, document)


def write_recipes() -> None:
    recipe = SRC / "data" / "cruciblecraft" / "recipe"
    dump(
        recipe / "gt_multiitem" / "extruder_shape_ccc.json",
        {
            "type": "cruciblecraft:shaped_catalyst",
            "pattern": ["   ", " P ", "  x"],
            "ingredients": {"P": {"item": "cruciblecraft:extruder_shape_ring"}},
            "result": {"id": "cruciblecraft:extruder_shape_ccc", "count": 1},
            "catalysts": {"x": {"item": "cruciblecraft:material_screwdriver"}},
        },
    )
    gt_recipe(
        recipe / "nuclear" / "aluminium_capcellcon.json",
        "cruciblecraft:extruder",
        64,
        16,
        [
            {"item": "cruciblecraft:aluminium/ingot"},
            {"item": "cruciblecraft:extruder_shape_ccc"},
        ],
        [1, 0],
        "cruciblecraft:aluminium/capcellcon",
        9,
        "Loader_Recipes_Handlers.java:766 aluminium/ingot -> 9 capcellcon",
    )
    dump(
        recipe / "gt_multiitem" / "multiitem_randomtools_m10001.json",
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
    empty = {"item": "cruciblecraft:gt_multiitem/multiitem_randomtools_m10001"}
    filled = "cruciblecraft:gt_multiitem/multiitem_randomtools_m10002"
    for gas in ("helium", "neon", "argon"):
        gt_recipe(
            recipe / "nuclear" / f"geiger_canner_{gas}.json",
            "cruciblecraft:canner",
            64,
            16,
            [empty],
            [1],
            filled,
            1,
            f"MultiItemRandomTools.java:541-543 {gas} 1000 mB",
            [{"id": f"cruciblecraft:{gas}", "amount": 1000}],
        )


def main() -> None:
    write_prefix()
    patch_gate()
    patch_census()
    write_art()
    write_generated_assets()
    write_recipes()


if __name__ == "__main__":
    main()
