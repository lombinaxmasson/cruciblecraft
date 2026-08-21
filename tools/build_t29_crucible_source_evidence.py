#!/usr/bin/env python3
"""Build the T29 GT6 multiblock-crucible source-evidence ledger.

Parses the fixed-revision MultiTileEntityCrucible checkStructure2 geometry,
capacity arithmetic, port-layer roles, and KU→Air condition. Records the
T23 positions:28 error as a fact to correct. Does not change runtime.
"""
from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from tools import t27_common as common  # noqa: E402

TOOLS = common.TOOLS
OUTPUT = TOOLS / "t29_crucible_source_evidence.json"
BUILDER = Path(__file__).resolve()

GT6_CRUCIBLE = (
    ROOT
    / "gt6_code"
    / "gregtech6"
    / "src"
    / "main"
    / "java"
    / "gregtech"
    / "tileentity"
    / "multiblocks"
    / "MultiTileEntityCrucible.java"
)
T23_POLICY = TOOLS / "t23_multiblock_policy.json"
T23_CLASSIFICATION = TOOLS / "t23_multiblock_behavior_classification.json"
CC_SINGLE = (
    ROOT
    / "src"
    / "main"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "content"
    / "blockentity"
    / "CrucibleBlockEntity.java"
)
CC_PROCESS_CORE = (
    ROOT
    / "src"
    / "main"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "machine"
    / "component"
    / "CrucibleProcessCore.java"
)

CONTROLLER_COUNT = 1
WALLS_PER_LAYER = 8
LAYER_COUNT = 3
AIR_ABOVE_CONTROLLER = 2
EXPECTED_POSITIONS = CONTROLLER_COUNT + WALLS_PER_LAYER * LAYER_COUNT + AIR_ABOVE_CONTROLLER
T23_ERRONEOUS_POSITIONS = 28
MAX_AMOUNT_FACTORS = (16, 3, 3, 3)
CAPACITY_INGOT_UNITS = 16 * 3 * 3 * 3
KG_PER_ENERGY = 100
HEAT_RESISTANCE_BONUS = 1.10
CORRECTED_NOTES = (
    "3x3x3 open-top: controller at bottom-center; "
    "8 walls × 3 layers = 24 walls; 2 AIR above controller; "
    "total 27 positions. Layer0 ONLY_ENERGY_IN (HU), "
    "layer1 ONLY_CRUCIBLE (CC: mold slots in controller UI, DESIGN_POLICY), "
    "layer2 ONLY_ITEM_FLUID. Wall meta 0/4 both accepted in GT6; "
    "CC collapses to a fixed casing."
)

LAYER_ROLES = (
    {
        "y": 0,
        "gt6_mask": "ONLY_ENERGY_IN",
        "cc_port": "energy_input",
        "energy": "HU",
        "fidelity": "SOURCE_BACKED",
    },
    {
        "y": 1,
        "gt6_mask": "ONLY_CRUCIBLE",
        "cc_port": None,
        "note": (
            "GT6 molds attach at the second wall layer. T23 froze CC molds in "
            "the controller UI (DESIGN_POLICY); this card does not reopen "
            "adjacent world molds."
        ),
        "fidelity": "DESIGN_POLICY",
    },
    {
        "y": 2,
        "gt6_mask": "ONLY_ITEM_FLUID",
        "cc_port": "item_fluid",
        "fidelity": "SOURCE_BACKED",
    },
)


def _require(path: Path) -> None:
    if not path.is_file():
        raise FileNotFoundError(common.relative(path))


def _read_java(path: Path) -> str:
    return path.read_text(encoding="utf-8", errors="replace").replace("\r", "")


def _line_of(text: str, needle: str) -> int:
    idx = text.find(needle)
    if idx < 0:
        raise ValueError(f"missing source fact: {needle}")
    return text[:idx].count("\n") + 1


def _method_span(text: str, signature: str) -> dict[str, Any]:
    start = _line_of(text, signature)
    lines = text.splitlines()
    depth = 0
    started = False
    end = start
    for index in range(start - 1, len(lines)):
        depth += lines[index].count("{") - lines[index].count("}")
        if "{" in lines[index]:
            started = True
        end = index + 1
        if started and depth == 0:
            break
    return {"end_line": end, "signature": signature, "start_line": start}


def _crucible_row(path: Path) -> dict[str, Any]:
    document = common.load_json(path)
    for row in document.get("kinds") or []:
        if row.get("canonical_id") == "crucible":
            return row
    raise ValueError(f"{common.relative(path)} has no crucible kind")


def _structure_positions() -> dict[str, Any]:
    wall_offsets: list[list[int]] = []
    for y in range(LAYER_COUNT):
        for i in (-1, 0, 1):
            for j in (-1, 0, 1):
                if i == 0 and j == 0:
                    continue
                wall_offsets.append([i, y, j])
    air_offsets = [[0, 1, 0], [0, 2, 0]]
    controller = [0, 0, 0]
    positions = [controller, *wall_offsets, *air_offsets]
    if len(positions) != EXPECTED_POSITIONS:
        raise ValueError(
            f"structure arithmetic drifted: {len(positions)} != {EXPECTED_POSITIONS}"
        )
    if len(wall_offsets) != WALLS_PER_LAYER * LAYER_COUNT:
        raise ValueError("wall count drifted from 8×3")
    if EXPECTED_POSITIONS == T23_ERRONEOUS_POSITIONS:
        raise ValueError("27-position ledger must reject the T23 28-count")
    return {
        "air": AIR_ABOVE_CONTROLLER,
        "air_offsets": air_offsets,
        "arithmetic": (
            f"{CONTROLLER_COUNT} controller + "
            f"{WALLS_PER_LAYER}×{LAYER_COUNT} walls + "
            f"{AIR_ABOVE_CONTROLLER} AIR = {EXPECTED_POSITIONS}"
        ),
        "controller": controller,
        "controller_location": "bottom-center",
        "open_top": True,
        "positions": EXPECTED_POSITIONS,
        "rejected_counts": [T23_ERRONEOUS_POSITIONS, 26],
        "second_loop_is_meta_alternate": True,
        "second_loop_note": (
            "The meta=4 loop is an alternate wall variant, not a second set of 24 walls."
        ),
        "wall_offsets": wall_offsets,
        "walls": WALLS_PER_LAYER * LAYER_COUNT,
        "walls_per_layer": WALLS_PER_LAYER,
    }


def build() -> dict[str, Any]:
    _require(GT6_CRUCIBLE)
    _require(T23_POLICY)
    _require(T23_CLASSIFICATION)
    _require(CC_SINGLE)
    _require(CC_PROCESS_CORE)
    text = _read_java(GT6_CRUCIBLE)
    cc_text = _read_java(CC_SINGLE)
    core_text = _read_java(CC_PROCESS_CORE)
    if "MAX_AMOUNT = 16*3*3*3*U" not in text:
        raise ValueError("MAX_AMOUNT formula drifted")
    if "KG_PER_ENERGY = 100" not in text:
        raise ValueError("KG_PER_ENERGY drifted")
    if "HEAT_RESISTANCE_BONUS = 1.10" not in text:
        raise ValueError("HEAT_RESISTANCE_BONUS drifted")
    if "Main at Bottom-Center." not in text:
        raise ValueError("bottom-center tooltip drifted")
    if "ONLY_ENERGY_IN" not in text or "ONLY_CRUCIBLE" not in text:
        raise ValueError("layer masks drifted")
    if "ONLY_ITEM_FLUID" not in text:
        raise ValueError("top-layer mask drifted")
    if "WD.oxygen" not in text or "MT.Air" not in text:
        raise ValueError("KU→Air condition drifted")
    if "fillMoldAtSide" not in text:
        raise ValueError("mold side fill drifted")
    if "SINGLE_BLOCK_MAX_INGOTS = 8" not in core_text:
        raise ValueError("single-block crucible capacity drifted")
    if "MAX_INGOTS = CrucibleProcessCore.SINGLE_BLOCK_MAX_INGOTS" not in cc_text:
        raise ValueError("single-block crucible adapter no longer uses the shared 8-ingot cap")

    t23_policy = _crucible_row(T23_POLICY)
    t23_classification = _crucible_row(T23_CLASSIFICATION)
    t23_positions = t23_policy["schema_expressibility"]["positions"]
    t23_class_positions = t23_classification["schema_expressibility"]["positions"]
    if t23_positions != t23_class_positions:
        raise ValueError("T23 policy and classification crucible positions disagree")
    t23_corrected = t23_positions == EXPECTED_POSITIONS
    if t23_positions not in (EXPECTED_POSITIONS, T23_ERRONEOUS_POSITIONS):
        raise ValueError(f"unexpected T23 crucible positions: {t23_positions}")

    structure = _structure_positions()
    check_structure = _method_span(
        text, "public boolean checkStructure2(ChunkCoordinates aCoordinates"
    )
    do_inject = _method_span(
        text,
        "public long doInject(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoInject)",
    )
    fill_mold = _method_span(
        text,
        "public boolean fillMoldAtSide(ITileEntityMold aMold, byte aSide, byte aSideOfMold)",
    )
    ku_air = {
        "condition": "bottom-layer KU and WD.oxygen at controller (x, y+1, z)",
        "fidelity": "SOURCE_DERIVED",
        "line": _line_of(text, "if (aEnergyType == TD.Energy.KU)"),
        "material": "MT.Air",
        "note": (
            "Wire into the existing AIR / steelmaking inlet if the port path "
            "exists; otherwise record a DESIGN_POLICY gap. Do not invent a "
            "new energy identity."
        ),
        "source_method": "doInject",
    }
    return {
        "capacity": {
            "cc_multiblock_ingot_units": CAPACITY_INGOT_UNITS,
            "cc_single_block_ingot_units": 8,
            "factors": list(MAX_AMOUNT_FACTORS),
            "formula": "16*3*3*3*U",
            "gt6_max_amount_line": _line_of(text, "MAX_AMOUNT = 16*3*3*3*U"),
            "ingot_units": CAPACITY_INGOT_UNITS,
            "note": (
                "U is one ingot unit. Single-block MAX_INGOTS=8 stays; "
                "the multiblock does not shrink it to 'align'."
            ),
        },
        "cc_single_block": {
            "air": "horizontal AIR",
            "capacity_ingots": 8,
            "heat": "HU from below",
            "mold": "adjacent CeramicMoldBlock",
            "path": common.relative(CC_SINGLE),
            "process": [
                "CompositionTank",
                "ThermalComponent",
                "SteelmakingController",
                "cast()",
            ],
        },
        "currentness": {
            "owned_inputs": {
                common.relative(BUILDER): common.sha256_file(BUILDER),
            }
        },
        "energy": {
            "accepted_types": ["KU", "HU", "CU", "VIS_IGNIS"],
            "heat_identity": "HU",
            "heat_resistance_bonus": HEAT_RESISTANCE_BONUS,
            "kg_per_energy": KG_PER_ENERGY,
            "ku_to_air": ku_air,
            "scale_note": (
                "GT6 is 1 HU / 100 kg → +1 K. CC reuses ThermalComponent / "
                "CrucibleThermalModel (DESIGN_POLICY) and does not rewrite physics."
            ),
        },
        "fidelity_layering": {
            "capacity_432": "SOURCE_BACKED",
            "heat_scale": "DESIGN_POLICY",
            "ku_to_air": "SOURCE_DERIVED",
            "layer_ports": "SOURCE_BACKED",
            "mold_in_controller_ui": "DESIGN_POLICY",
            "not_a_processing_host": True,
            "steelmaking": "SOURCE_DERIVED",
            "structure_27": "SOURCE_BACKED",
            "wall_meta_collapse": "DESIGN_POLICY",
        },
        "generated_by": "python tools/build_t29_crucible_source_evidence.py --write",
        "layers": list(LAYER_ROLES),
        "methods": {
            "checkStructure2": check_structure,
            "doInject": do_inject,
            "fillMoldAtSide": fill_mold,
        },
        "molds": {
            "cc_policy": "controller inventory/UI slots",
            "fidelity": "DESIGN_POLICY",
            "gt6": "second wall layer, fillMoldAtSide, not a RecipeMap",
            "gt6_line": fill_mold["start_line"],
        },
        "schema_version": 1,
        "source": {
            "blob_sha256": common.sha256_file(GT6_CRUCIBLE),
            "class": "gregtech.tileentity.multiblocks.MultiTileEntityCrucible",
            "method": "checkStructure2",
            "path": common.relative(GT6_CRUCIBLE),
            "repository": "https://github.com/GregTech6/gregtech6",
            "revision": common.SOURCE_REVISION,
        },
        "status": "T29_CRUCIBLE_SOURCE_EVIDENCE",
        "structure": structure,
        "t23_error": {
            "artifact": common.relative(T23_POLICY),
            "classification_artifact": common.relative(T23_CLASSIFICATION),
            "corrected_notes": CORRECTED_NOTES,
            "correction_positions": EXPECTED_POSITIONS,
            "current_notes": t23_policy["schema_expressibility"]["notes"],
            "current_positions": t23_positions,
            "erroneous_notes_fragment": "26 walls + 2 AIR",
            "erroneous_positions": T23_ERRONEOUS_POSITIONS,
            "reason": (
                "T23 counted 26 walls + 2 AIR. checkStructure2 is 1 controller "
                "+ 24 walls + 2 AIR. The meta=4 loop is not extra blocks."
            ),
            "status": "corrected" if t23_corrected else "pending_correction",
        },
        "tooltips": {
            "bottom_center_line": _line_of(text, "Main at Bottom-Center."),
            "energy_in_bottom_stuff_in_top": True,
            "ku_air_tooltip": True,
            "molds_second_layer": True,
        },
    }


def write() -> dict[str, Any]:
    document = build()
    common.write_stable(OUTPUT, document)
    return document


def check() -> list[str]:
    if not OUTPUT.is_file():
        return [f"missing generated file: {common.relative(OUTPUT)}"]
    expected = common.stable_json(build())
    actual = OUTPUT.read_text(encoding="utf-8")
    if actual != expected:
        return [f"{common.relative(OUTPUT)} is stale"]
    return []


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args(argv)
    if bool(args.write) == bool(args.check):
        parser.error("choose exactly one of --write, --check")
    try:
        if args.write:
            document = write()
            print(
                f"wrote {common.relative(OUTPUT)} "
                f"status={document['status']} "
                f"positions={document['structure']['positions']} "
                f"t23={document['t23_error']['status']}"
            )
            return 0
        errors = check()
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        print(f"{common.relative(OUTPUT)} is current")
        return 0
    except (OSError, ValueError, json.JSONDecodeError, KeyError) as exc:
        print(str(exc), file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
