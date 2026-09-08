#!/usr/bin/env python3
"""Emit remaining GT6 T2/T3/T4/T6 circuit parts, recipes, and art."""
from __future__ import annotations

import json
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
REV = "3703e40308c8c030763fd6297dea8b210d2a77b1"
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
TECH = SRC / "data" / "cruciblecraft" / "technological_parts.json"
RECIPE = SRC / "data" / "cruciblecraft" / "recipe"
ART_MANIFEST = SRC / "assets" / "cruciblecraft" / "gt6_circuit_tiers_art_manifest.json"
DST_TEX = SRC / "assets" / "cruciblecraft" / "textures" / "item" / "gt6_import"

NEW_PARTS = [
    ("circuit_wire_gold", 30003, "Circuit Wiring (Gold)", "电路布线（金）"),
    ("circuit_plate_gold", 30004, "Circuit Plate (Gold)", "电路板（金）"),
    ("circuit_part_good", 30102, "Circuit Part (Good)", "电路元件（优良）"),
    ("circuit_part_advanced", 30103, "Circuit Part (Advanced)", "电路元件（进阶）"),
    ("circuit_part_elite", 30104, "Circuit Part (Elite)", "电路元件（精英）"),
    ("circuit_part_ultimate", 30106, "Circuit Part (Ultimate)", "电路元件（终极）"),
    ("circuit_board_good", 30202, "Circuit Board (Good)", "电路基板（优良）"),
    ("circuit_board_advanced", 30203, "Circuit Board (Advanced)", "电路基板（进阶）"),
    ("circuit_board_elite", 30204, "Circuit Board (Elite)", "电路基板（精英）"),
    ("circuit_board_ultimate", 30206, "Circuit Board (Ultimate)", "电路基板（终极）"),
    ("circuit_good", 30302, "Circuit T2 (Good)", "电路 T2（优良）"),
    ("circuit_advanced", 30303, "Circuit T3 (Advanced)", "电路 T3（进阶）"),
    ("circuit_elite", 30304, "Circuit T4 (Elite)", "电路 T4（精英）"),
    ("circuit_ultimate", 30306, "Circuit T6 (Ultimate)", "电路 T6（终极）"),
]


def dump(path: Path, payload: object) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(
        json.dumps(payload, indent=2, ensure_ascii=False) + "\n",
        encoding="utf-8",
    )


def press(name: str, duration: int, inputs: list[tuple[str, int]], output: str, source: str) -> None:
    items = [{"item": item} for item, _ in inputs]
    dump(
        RECIPE / "machine" / "press" / f"{name}.json",
        {
            "type": "cruciblecraft:gt_recipe",
            "map": "cruciblecraft:press",
            "duration": duration,
            "eut": 16,
            "can_be_buffered": True,
            "item_inputs": items,
            "item_input_counts": [count for _, count in inputs],
            "item_outputs": [{"id": f"cruciblecraft:{output}", "count": 1}],
            "output_chances": [10000],
            "provenance": {
                "selected_source_recipe": source,
                "source_kind": "gt6_source",
            },
        },
    )


def bath(name: str, board: str, fluid: str, output: str, source: str) -> None:
    dump(
        RECIPE / "machine" / "bath" / f"{name}.json",
        {
            "type": "cruciblecraft:gt_recipe",
            "map": "cruciblecraft:bath",
            "duration": 64,
            "eut": 0,
            "can_be_buffered": True,
            "item_inputs": [{"item": f"cruciblecraft:{board}"}],
            "item_input_counts": [1],
            "item_outputs": [{"id": f"cruciblecraft:{output}", "count": 1}],
            "output_chances": [10000],
            "provenance": {
                "selected_source_recipe": source,
                "source_kind": "gt6_source",
            },
            "fluid_inputs": [{"id": f"cruciblecraft:{fluid}", "amount": 72}],
        },
    )


def write_parts() -> None:
    document = json.loads(TECH.read_text(encoding="utf-8"))
    existing = {row["registry_path"] for row in document["parts"]}
    for path, source_id, english, chinese in NEW_PARTS:
        if path in existing:
            continue
        document["parts"].append(
            {
                "id": f"cruciblecraft:{path}",
                "registry_path": path,
                "source_id": source_id,
                "english_name": english,
                "chinese_name": chinese,
                "texture": path,
            }
        )
    dump(TECH, document)


def copy_art() -> None:
    DST_TEX.mkdir(parents=True, exist_ok=True)
    imports = []
    for path, source_id, _en, _zh in NEW_PARTS:
        src = GT6_ART / "items" / "gt.multiitem.technological" / f"{source_id}.png"
        dst = DST_TEX / f"{path}.png"
        shutil.copy2(src, dst)
        imports.append(
            {
                "gt6_source": (
                    "assets/gregtech/textures/items/"
                    f"gt.multiitem.technological/{source_id}.png"
                ),
                "destination": f"assets/cruciblecraft/textures/item/gt6_import/{path}.png",
            }
        )
    dump(
        ART_MANIFEST,
        {
            "source": "gt6_referencable_port_code/gregtech6_w",
            "source_revision": REV,
            "imports": imports,
        },
    )


def write_recipes() -> None:
    dump(
        RECIPE / "machine" / "laser_engraver" / "circuit_wire_gold.json",
        {
            "type": "cruciblecraft:gt_recipe",
            "map": "cruciblecraft:laser_engraver",
            "duration": 64,
            "eut": 16,
            "can_be_buffered": True,
            "item_inputs": [
                {"item": "cruciblecraft:gold/foil"},
                {"item": "cruciblecraft:ruby/lens"},
            ],
            "item_input_counts": [4, 0],
            "item_outputs": [{"id": "cruciblecraft:circuit_wire_gold", "count": 1}],
            "output_chances": [10000],
            "provenance": {
                "selected_source_recipe": "Loader_Recipes_Other.java:159",
                "source_kind": "gt6_source",
            },
        },
    )
    press(
        "circuit_plate_gold",
        64,
        [("cruciblecraft:circuit_plate_empty", 1), ("cruciblecraft:circuit_wire_gold", 1)],
        "circuit_plate_gold",
        "MultiItemTechnological.java:574",
    )
    press(
        "circuit_part_good",
        16,
        [
            ("cruciblecraft:copper/fine_wire", 1),
            ("cruciblecraft:signalum/fine_wire", 1),
            ("cruciblecraft:silicon/tiny_plate_gem", 1),
        ],
        "circuit_part_good",
        "MultiItemTechnological.java:604",
    )
    press(
        "circuit_part_advanced",
        16,
        [
            ("cruciblecraft:gold/fine_wire", 1),
            ("cruciblecraft:signalum/fine_wire", 1),
            ("cruciblecraft:silicon/tiny_plate_gem", 1),
        ],
        "circuit_part_advanced",
        "MultiItemTechnological.java:611",
    )
    press(
        "circuit_part_elite",
        16,
        [
            ("cruciblecraft:gold/fine_wire", 1),
            ("cruciblecraft:signalum/fine_wire", 1),
            ("cruciblecraft:redstone_alloy/tiny_plate_gem", 1),
        ],
        "circuit_part_elite",
        "MultiItemTechnological.java:613",
    )
    press(
        "circuit_part_ultimate",
        16,
        [
            ("cruciblecraft:platinum/fine_wire", 1),
            ("cruciblecraft:signalum/fine_wire", 1),
            ("cruciblecraft:redstone_alloy/tiny_plate_gem", 1),
        ],
        "circuit_part_ultimate",
        "MultiItemTechnological.java:616",
    )
    press(
        "circuit_board_good",
        64,
        [("cruciblecraft:circuit_plate_copper", 1), ("cruciblecraft:circuit_part_good", 4)],
        "circuit_board_good",
        "MultiItemTechnological.java:675",
    )
    press(
        "circuit_board_advanced",
        64,
        [("cruciblecraft:circuit_plate_gold", 1), ("cruciblecraft:circuit_part_advanced", 4)],
        "circuit_board_advanced",
        "MultiItemTechnological.java:682",
    )
    press(
        "circuit_board_elite",
        64,
        [("cruciblecraft:circuit_plate_gold", 1), ("cruciblecraft:circuit_part_elite", 4)],
        "circuit_board_elite",
        "MultiItemTechnological.java:683",
    )
    press(
        "circuit_board_ultimate",
        64,
        [
            ("cruciblecraft:circuit_plate_platinum", 1),
            ("cruciblecraft:circuit_part_ultimate", 4),
        ],
        "circuit_board_ultimate",
        "MultiItemTechnological.java:691",
    )
    bath(
        "circuit_good_molten_tin",
        "circuit_board_good",
        "molten_tin",
        "circuit_good",
        "MultiItemTechnological.java:725",
    )
    bath(
        "circuit_good_molten_soldering_alloy",
        "circuit_board_good",
        "molten_soldering_alloy",
        "circuit_good",
        "MultiItemTechnological.java:725",
    )
    bath(
        "circuit_advanced_molten_soldering_alloy",
        "circuit_board_advanced",
        "molten_soldering_alloy",
        "circuit_advanced",
        "MultiItemTechnological.java:728",
    )
    bath(
        "circuit_elite_molten_soldering_alloy",
        "circuit_board_elite",
        "molten_soldering_alloy",
        "circuit_elite",
        "MultiItemTechnological.java:731",
    )
    bath(
        "circuit_ultimate_molten_soldering_alloy",
        "circuit_board_ultimate",
        "molten_soldering_alloy",
        "circuit_ultimate",
        "MultiItemTechnological.java:737",
    )


def main() -> None:
    write_parts()
    copy_art()
    write_recipes()


if __name__ == "__main__":
    main()
