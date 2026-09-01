#!/usr/bin/env python3
"""Convert GT6 basic-machine slot tables into CC UiLayout coordinates.

GT6 writes item/fluid slot positions in
``gt6_code/gregtech6/src/main/java/gregapi/gui/ContainerCommonBasicMachine.java``.
The painted GUI PNGs match the RecipeMap *panel* counts in ``RM.java``
(``mInputItemsCount`` / ``mOutputItemsCount`` / fluid counts), not the smaller
slot counts CC currently exposes.

This script:

1. Ports that switch table to plain ``{x, y}`` lists.
2. Slices each panel down to CC's used slot counts (or extends past the panel
   when CC has *more* slots than GT6 painted).
3. Emits CC-shaped JSON: ``item_slots``, ``tanks``, ``progress``.

Runtime Java (``Gt6BasicMachineGui``) implements the same algorithm. Re-run
this file after changing the table; ``--check`` fails if the committed JSON
is stale.
"""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "tools" / "gt6_basic_machine_layouts.json"
GT6_CONTAINER = (
    ROOT
    / "gt6_code"
    / "gregtech6"
    / "src"
    / "main"
    / "java"
    / "gregapi"
    / "gui"
    / "ContainerCommonBasicMachine.java"
)

PROGRESS = {
    "x": 78,
    "y": 24,
    "width": 20,
    "height": 18,
    "atlas_u": 176,
    "atlas_v": 0,
}
SPECIAL_SLOT = {"x": 80, "y": 43}
FLUID_SLOT = 18

# CC machine path → GT6 RecipeMap panel + CC used counts.
# special_index: insert GT6 special slot (80, 43) after concatenating
# used item inputs then outputs (extruder tool lives at index 1).
MACHINES: dict[str, dict] = {
    "bronze_crusher": {
        "gt6_map": "Crusher",
        "panel": (1, 12, 0, 0),
        "used": (1, 1, 0, 0),
    },
    "sluice": {
        "gt6_map": "Sluice",
        "panel": (1, 9, 1, 1),
        "used": (1, 4, 1, 0),
    },
    "bath": {
        "gt6_map": "Bath",
        "panel": (6, 6, 1, 3),
        "used": (1, 6, 1, 3),
    },
    "centrifuge": {
        "gt6_map": "Centrifuge",
        "panel": (1, 6, 1, 6),
        "used": (1, 6, 1, 2),
    },
    "shredder": {
        "gt6_map": "Shredder",
        "panel": (1, 12, 0, 0),
        "used": (1, 4, 0, 0),
    },
    "sifter": {
        "gt6_map": "Sifting",
        "panel": (1, 12, 0, 0),
        "used": (1, 4, 0, 0),
    },
    "mortar": {
        "gt6_map": "Mortar",
        "panel": (1, 2, 0, 0),
        "used": (1, 4, 0, 0),
    },
    "smelter": {
        "gt6_map": "Smelter",
        "panel": (1, 1, 1, 1),
        "used": (1, 4, 0, 1),
    },
    "extruder": {
        "gt6_map": "Extruder",
        "panel": (2, 2, 0, 0),
        "used": (1, 1, 0, 0),
        "special_index": 1,
    },
    "cutter": {
        "gt6_map": "Cutter",
        "panel": (1, 3, 1, 0),
        "used": (1, 1, 0, 0),
    },
    "lathe": {
        "gt6_map": "Lathe",
        "panel": (1, 2, 0, 0),
        "used": (1, 1, 0, 0),
    },
    "rollingmill": {
        "gt6_map": "RollingMill",
        "panel": (1, 1, 0, 0),
        "used": (1, 1, 0, 0),
    },
    "rollbender": {
        "gt6_map": "RollBender",
        "panel": (1, 1, 0, 0),
        "used": (1, 1, 0, 0),
    },
    "wiremill": {
        "gt6_map": "Wiremill",
        "panel": (1, 1, 0, 0),
        "used": (2, 1, 0, 0),
    },
    "bender": {
        "gt6_map": "RollBender",
        "panel": (1, 1, 0, 0),
        "used": (1, 1, 0, 0),
    },
    "assembler": {
        "gt6_map": "Assembler",
        "panel": (2, 1, 1, 0),
        "used": (6, 1, 1, 0),
    },
    "welder": {
        "gt6_map": "Welder",
        "panel": (9, 1, 1, 0),
        "used": (2, 1, 1, 0),
    },
    "press": {
        "gt6_map": "Press",
        "panel": (3, 1, 0, 0),
        "used": (2, 1, 0, 0),
    },
    "electrolyzer": {
        "gt6_map": "Electrolyzer",
        "panel": (2, 6, 2, 6),
        "used": (2, 6, 2, 3),
    },
    "mixer": {
        "gt6_map": "Mixer",
        "panel": (6, 1, 6, 2),
        "used": (4, 1, 3, 2),
    },
    "distillery": {
        "gt6_map": "Distillery",
        "panel": (1, 2, 1, 2),
        "used": (2, 2, 2, 3),
    },
    "autoclave": {
        "gt6_map": "Autoclave",
        "panel": (2, 3, 1, 1),
        "used": (2, 3, 1, 1),
    },
    "drying": {
        "gt6_map": "Drying",
        "panel": (1, 1, 1, 3),
        "used": (1, 1, 0, 1),
    },
    "compressor": {
        "gt6_map": "Compressor",
        "panel": (1, 1, 0, 0),
        "used": (1, 1, 0, 0),
    },
    "generifier": {
        "gt6_map": "Generifier",
        "panel": (1, 1, 1, 1),
        "used": (0, 0, 1, 1),
    },
    "coke_oven": {
        "gt6_map": "CokeOven",
        "panel": (1, 9, 0, 1),
        "used": (1, 1, 0, 1),
    },
}


def xy(x: int, y: int) -> dict[str, int]:
    return {"x": x, "y": y}


def input_slots(count: int, in_fluids: int) -> list[dict[str, int]]:
    """Port of ContainerCommonBasicMachine input switch."""
    y_high = 7 if in_fluids > 6 else 25
    y0 = 7 if in_fluids > 3 else 16
    y1 = 25 if in_fluids > 3 else 34
    if count <= 0:
        return []
    if count == 1:
        return [xy(53, y_high)]
    if count == 2:
        return [xy(35, y_high), xy(53, y_high)]
    if count == 3:
        return [xy(17, y_high), xy(35, y_high), xy(53, y_high)]
    if count == 4:
        return [xy(35, y0), xy(53, y0), xy(35, y1), xy(53, y1)]
    if count == 5:
        return [xy(17, y0), xy(35, y0), xy(53, y0), xy(35, y1), xy(53, y1)]
    if count == 6:
        return [
            xy(17, y0),
            xy(35, y0),
            xy(53, y0),
            xy(17, y1),
            xy(35, y1),
            xy(53, y1),
        ]
    grid = [
        xy(17, 7),
        xy(35, 7),
        xy(53, 7),
        xy(17, 25),
        xy(35, 25),
        xy(53, 25),
        xy(17, 43),
        xy(35, 43),
        xy(53, 43),
        xy(17, 61),
        xy(35, 61),
        xy(53, 61),
    ]
    return grid[: min(count, 12)]


def output_slots(count: int, out_fluids: int) -> list[dict[str, int]]:
    """Port of ContainerCommonBasicMachine output switch."""
    y_high = 7 if out_fluids > 6 else 25
    y0 = 7 if out_fluids > 3 else 16
    y1 = 25 if out_fluids > 3 else 34
    if count <= 0:
        return []
    if count == 1:
        return [xy(107, y_high)]
    if count == 2:
        return [xy(107, y_high), xy(125, y_high)]
    if count == 3:
        return [xy(107, y_high), xy(125, y_high), xy(143, y_high)]
    if count == 4:
        return [xy(107, y0), xy(125, y0), xy(107, y1), xy(125, y1)]
    if count == 5:
        return [xy(107, y0), xy(125, y0), xy(143, y0), xy(107, y1), xy(125, y1)]
    if count == 6:
        return [
            xy(107, y0),
            xy(125, y0),
            xy(143, y0),
            xy(107, y1),
            xy(125, y1),
            xy(143, y1),
        ]
    grid = [
        xy(107, 7),
        xy(125, 7),
        xy(143, 7),
        xy(107, 25),
        xy(125, 25),
        xy(143, 25),
        xy(107, 43),
        xy(125, 43),
        xy(143, 43),
        xy(107, 61),
        xy(125, 61),
        xy(143, 61),
    ]
    return grid[: min(count, 12)]


def fluid_slots(count: int, outputs: bool) -> list[dict[str, int]]:
    origin_x = 107 if outputs else 53
    sign = 1 if outputs else -1
    return [
        xy(origin_x + sign * (i % 3) * 18, 63 - (i // 3) * 18)
        for i in range(count)
    ]


def take_or_extend(
    panel: list[dict[str, int]],
    used_count: int,
    used_layout: list[dict[str, int]],
) -> list[dict[str, int]]:
    if used_count <= 0:
        return []
    if used_count <= len(panel):
        return panel[:used_count]
    # CC has more slots than GT6 painted. Use the larger grid as a whole so
    # mixed switch-tables cannot stack two different Y rows on the same X.
    return used_layout[:used_count]


def layout(
    panel_in: int,
    panel_out: int,
    panel_in_f: int,
    panel_out_f: int,
    used_in: int,
    used_out: int,
    used_in_f: int,
    used_out_f: int,
    special_index: int | None = None,
) -> dict:
    shift_in_f = max(panel_in_f, used_in_f)
    shift_out_f = max(panel_out_f, used_out_f)
    items = take_or_extend(
        input_slots(panel_in, shift_in_f),
        used_in,
        input_slots(used_in, shift_in_f),
    ) + take_or_extend(
        output_slots(panel_out, shift_out_f),
        used_out,
        output_slots(used_out, shift_out_f),
    )
    if special_index is not None:
        items.insert(special_index, dict(SPECIAL_SLOT))
    tanks = []
    for index, slot in enumerate(
        fluid_slots(used_in_f, False) + fluid_slots(used_out_f, True)
    ):
        tanks.append(
            {
                "tank": index,
                "x": slot["x"],
                "y": slot["y"],
                "width": FLUID_SLOT,
                "height": FLUID_SLOT,
            }
        )
    return {
        "item_slots": items,
        "tanks": tanks,
        "progress": dict(PROGRESS),
        "special_index": special_index,
    }


def build() -> dict:
    machines = {}
    for name, spec in MACHINES.items():
        panel = spec["panel"]
        used = spec["used"]
        converted = layout(*panel, *used, spec.get("special_index"))
        machines[name] = {
            "gt6_map": spec["gt6_map"],
            "panel": {
                "in_items": panel[0],
                "out_items": panel[1],
                "in_fluids": panel[2],
                "out_fluids": panel[3],
            },
            "used": {
                "in_items": used[0],
                "out_items": used[1],
                "in_fluids": used[2],
                "out_fluids": used[3],
            },
            **converted,
        }
    return {
        "source": str(GT6_CONTAINER.relative_to(ROOT)).replace("\\", "/"),
        "progress": dict(PROGRESS),
        "special_slot": dict(SPECIAL_SLOT),
        "machines": machines,
    }


def dump_json(payload: dict) -> str:
    return json.dumps(payload, indent=2, ensure_ascii=False) + "\n"


def write(path: Path = OUTPUT) -> dict:
    payload = build()
    path.write_text(dump_json(payload), encoding="utf-8", newline="\n")
    return payload


def check(path: Path = OUTPUT) -> None:
    expected = dump_json(build())
    actual = path.read_text(encoding="utf-8") if path.exists() else ""
    if actual != expected:
        raise SystemExit(
            f"{path.relative_to(ROOT)} is stale; run tools/gt6_gui_layout.py"
        )


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--check",
        action="store_true",
        help="exit non-zero if the committed JSON does not match",
    )
    args = parser.parse_args(argv)
    if args.check:
        check()
        return 0
    write()
    print(f"wrote {OUTPUT.relative_to(ROOT)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
