#!/usr/bin/env python3
"""GT6 connector survival-obtain census and fluid-pipe acquisition overlay.

Shared across sequential unique-active children so the python-module budget
stays one slot. Do not import closed fluid/item/EU runtime modules.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import shutil
import sys
from pathlib import Path
from typing import Any

from tools import census_common as census
from tools import gt6_resolve
from tools import io_common as io

FLUID_SLUG = "content/gt6-fluid-pipe-acquisition"
ITEM_SLUG = "content/gt6-item-pipe-acquisition"
EU_SLUG = "content/gt6-eu-cable-acquisition"
REDSTONE_SLUG = "content/gt6-redstone-wire-acquisition"
FLUID_STATUS = "FLUID_PIPE_ACQUISITION_READY"
GT6_REVISION = io.SOURCE_REVISION
WAVE = census.TOOLS / "waves" / "content" / "gt6-fluid-pipe-acquisition"
ITEM_WAVE = census.TOOLS / "waves" / "content" / "gt6-item-pipe-acquisition"
ITEM_STATUS = "ITEM_PIPE_ACQUISITION_READY"
EU_WAVE = census.TOOLS / "waves" / "content" / "gt6-eu-cable-acquisition"
EU_STATUS = "EU_CABLE_ACQUISITION_READY"
REDSTONE_WAVE = census.TOOLS / "waves" / "content" / "gt6-redstone-wire-acquisition"
REDSTONE_STATUS = "REDSTONE_WIRE_ACQUISITION_READY"
COMBO_OVERLAY = (
    census.TOOLS
    / "waves"
    / "content"
    / "gt6-fluid-combo-pipe-runtime"
    / "combo_overlay.json"
)
BASELINE = census.TOOLS / "waves" / "content" / "gt6-pipe-cable-baseline"
LEDGER = BASELINE / "identity_resolution_ledger.json"
R0 = (
    census.TOOLS
    / "waves"
    / "portfolio"
    / "mte-identity-disposition-r0"
    / "disposition_ledger.json"
)
MATERIALS = (
    census.ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "materials"
)
GENERATED_COMBO = (
    census.ROOT
    / "src"
    / "generated"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "recipe"
    / "pipe"
    / "combo"
)
GENERATED_TABLE = (
    census.ROOT
    / "src"
    / "generated"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "recipe"
    / "pipe"
    / "table"
)
GENERATED_ITEM_TABLE = (
    census.ROOT
    / "src"
    / "generated"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "recipe"
    / "pipe"
    / "item_table"
)
GENERATED_RESTRICTIVE = (
    census.ROOT
    / "src"
    / "generated"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "recipe"
    / "pipe"
    / "restrictive"
)
GENERATED_CABLE_TABLE = (
    census.ROOT
    / "src"
    / "generated"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "recipe"
    / "cable"
    / "table"
)
GENERATED_CABLE_SHAPELESS = (
    census.ROOT
    / "src"
    / "generated"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "recipe"
    / "cable"
    / "shapeless"
)
GENERATED_CABLE_PACK = (
    census.ROOT
    / "src"
    / "generated"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "recipe"
    / "cable"
    / "pack"
)
GENERATED_CABLE_UNPACK = (
    census.ROOT
    / "src"
    / "generated"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "recipe"
    / "cable"
    / "unpack"
)
GENERATED_REDSTONE = (
    census.ROOT
    / "src"
    / "generated"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "recipe"
    / "redstone"
    / "laminator"
)
EXTRUDER = (
    census.ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "recipe"
    / "pipe"
    / "extruder"
)
RECIPE_PROVIDER = (
    census.ROOT
    / "src"
    / "main"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "datagen"
    / "ModRecipeProvider.java"
)
GAME_TESTS = (
    census.ROOT
    / "src"
    / "test"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "gametest"
    / "FluidPipeAcquisitionGameTests.java"
)
ITEM_GAME_TESTS = (
    census.ROOT
    / "src"
    / "test"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "gametest"
    / "ItemPipeAcquisitionGameTests.java"
)
EU_GAME_TESTS = (
    census.ROOT
    / "src"
    / "test"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "gametest"
    / "EuCableAcquisitionGameTests.java"
)
REDSTONE_GAME_TESTS = (
    census.ROOT
    / "src"
    / "test"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "gametest"
    / "RedstoneWireAcquisitionGameTests.java"
)
CORE_TESTS = (
    census.ROOT
    / "src"
    / "test"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "gametest"
    / "CrucibleCraftGameTests.java"
)
PACK = (
    census.ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft_wave_content_gt6_fluid_pipe_acquisition"
)
ITEM_PACK = (
    census.ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft_wave_content_gt6_item_pipe_acquisition"
)
EU_PACK = (
    census.ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft_wave_content_gt6_eu_cable_acquisition"
)
REDSTONE_PACK = (
    census.ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft_wave_content_gt6_redstone_wire_acquisition"
)
EMPTY_SRC = (
    census.ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft_wave_content_gt6_fluid_pipe_runtime"
    / "structure"
    / "empty.nbt"
)
OVERLAY_PATH = WAVE / "acquisition_overlay.json"
ITEM_OVERLAY_PATH = ITEM_WAVE / "acquisition_overlay.json"
EU_OVERLAY_PATH = EU_WAVE / "acquisition_overlay.json"
REDSTONE_OVERLAY_PATH = REDSTONE_WAVE / "acquisition_overlay.json"
CENSUS_PATH = (
    census.ROOT
    / "src"
    / "main"
    / "resources"
    / "census"
    / "runtime_registry_gate.json"
)
FIVE_GAUGE = (
    ("pipeTiny", "tiny_fluid_pipe", "curved_plate"),
    ("pipeSmall", "small_fluid_pipe", "curved_plate"),
    ("pipeMedium", "fluid_pipe", "curved_plate"),
    ("pipeLarge", "large_fluid_pipe", "curved_plate"),
    ("pipeHuge", "huge_fluid_pipe", "double_plate"),
)
EXPECTED_TESTS = [
    "comboQuadrupleCraftsFromMedium",
    "comboUnpackReturnsMedium",
    "copperHugeTableCraftsFromDoublePlate",
    "copperTinyTableCraftsFromCurvedPlate",
    "fiveGaugeTableDoesNotUseFlatPlate",
    "nonmetalCatalogStaysTwentyFive",
]
ITEM_EXPECTED_TESTS = [
    "copperHugeItemTableCraftsFromDoublePlate",
    "copperMediumTableCraftsFromCurvedPlate",
    "copperRestrictiveCraftsFromMediumAndSteelRing",
    "itemTableDoesNotUseFlatPlate",
    "nonmetalCatalogStaysTwentyFive",
    "restrictiveUsesSteelRingNotInvented",
]
EU_EXPECTED_TESTS = [
    "copperCableCraftsFromWireAndRubberPlate",
    "copperDoubleWirePacksFromTwoSingles",
    "copperPlateCraftsWireWithCutter",
    "copperUnpackDoubleReturnsTwoSingles",
    "plate2wireDoesNotUseProgrammedCircuit",
    "redAlloyHasNoEuPlate2wire",
]
REDSTONE_EXPECTED_TESTS = [
    "foilRecipeGatedOnLiveRubberFoil",
    "insulatedCablesAreNotEuTinOutputs",
    "lumiumCableLaminatesFromRubberPlate",
    "noProgrammedCircuitInLaminator",
    "redAlloyCableLaminatesFromRubberPlate",
    "signalumCableLaminatesFromRubberPlate",
]
EXTRUDER_FILES = (
    "plate_to_tiny_fluid_pipe.json",
    "plate_to_small_fluid_pipe.json",
    "plates_to_fluid_pipe.json",
    "plates_to_large_fluid_pipe.json",
    "plates_to_huge_fluid_pipe.json",
)
ITEM_EXTRUDER_FILES = (
    "plates_to_item_pipe.json",
    "plates_to_large_item_pipe.json",
    "plates_to_huge_item_pipe.json",
)
EU_ASSEMBLER = (
    census.ROOT
    / "src"
    / "component_rule_generated"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "recipe"
    / "assembler"
    / "wire_and_rubber_to_cable.json"
)
EU_WIREMILL = (
    census.ROOT
    / "src"
    / "component_rule_generated"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "recipe"
    / "wiremill"
    / "ingot_to_wire.json"
)


def _write_json(path: Path, document: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    payload = json.dumps(document, ensure_ascii=False, indent=2) + "\n"
    if path.is_file() and path.read_text(encoding="utf-8") == payload:
        return
    path.write_text(payload, encoding="utf-8")


def _sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def _registered_ids() -> set[str]:
    if not CENSUS_PATH.is_file():
        return set()
    document = json.loads(CENSUS_PATH.read_text(encoding="utf-8"))
    categories = document.get("categories") or {}
    ids: set[str] = set()
    for key in ("items", "blocks"):
        ids.update(categories.get(key) or [])
    return ids


def _combo_hosts() -> list[str]:
    if not COMBO_OVERLAY.is_file():
        return []
    rows = census.load_json(COMBO_OVERLAY).get("rows") or []
    hosts: list[str] = []
    seen: set[str] = set()
    for row in rows:
        host = str(row.get("live_block") or "")
        if host and host not in seen:
            seen.add(host)
            hosts.append(host)
    return hosts


def _fluid_table_census() -> dict[str, Any]:
    registered = _registered_ids()
    live: list[dict[str, str]] = []
    blocked: list[dict[str, str]] = []
    for path in sorted(MATERIALS.glob("*.json")):
        document = json.loads(path.read_text(encoding="utf-8"))
        if not isinstance(document, dict):
            continue
        metadata = document.get("gt6_metadata")
        if not isinstance(metadata, dict):
            continue
        pipes = ((metadata.get("pipe_properties") or {}).get(
            "fluid_by_specification"
        ) or {})
        if not isinstance(pipes, dict):
            continue
        material = path.stem
        for spec, form, plate in FIVE_GAUGE:
            props = pipes.get(spec)
            if not isinstance(props, dict) or not props.get("recipe"):
                continue
            pipe_id = f"cruciblecraft:{material}/{form}"
            if pipe_id not in registered:
                continue
            plate_id = f"cruciblecraft:{material}/{plate}"
            row = {
                "material": material,
                "form": form,
                "operand": plate,
                "pipe": pipe_id,
            }
            if plate_id in registered:
                live.append(row)
            else:
                row["missing"] = plate_id
                blocked.append(row)
    return {"live_table": live, "blocked_table": blocked}


def topology(unique_active: bool) -> dict[str, Any]:
    return {
        "schema_version": 1,
        "wave_slug": FLUID_SLUG,
        "unique_active_wave": FLUID_SLUG if unique_active else None,
        "status": "WAVE_READY",
        "source_revision": GT6_REVISION,
        "generated_by": "content/gt6-fluid-pipe-acquisition implementation",
        "append_only": False,
        "next_unassigned": True,
        "remaining_recipe_gap": 0,
    }


def readiness(unique_active: bool) -> dict[str, Any]:
    return {
        "schema_version": 1,
        "wave_slug": FLUID_SLUG,
        "unique_active_wave": FLUID_SLUG if unique_active else None,
        "status": FLUID_STATUS,
        "source_revision": GT6_REVISION,
    }


def _generated_combo_counts() -> tuple[int, int]:
    if not GENERATED_COMBO.is_dir():
        return 0, 0
    files = list(GENERATED_COMBO.glob("*/*.json"))
    packs = sum(1 for path in files if not path.name.startswith("unpack_"))
    unpacks = sum(1 for path in files if path.name.startswith("unpack_"))
    return packs, unpacks


def _generated_table_operands() -> dict[str, int]:
    counts: dict[str, int] = {}
    if not GENERATED_TABLE.is_dir():
        return counts
    for path in GENERATED_TABLE.glob("*/*.json"):
        document = json.loads(path.read_text(encoding="utf-8"))
        item = str(
            ((document.get("ingredients") or {}).get("P") or {}).get("item") or ""
        )
        operand = item.rsplit("/", 1)[-1] if item else ""
        counts[operand] = counts.get(operand, 0) + 1
    return {key: counts[key] for key in sorted(counts)}


def build_overlay() -> dict[str, Any]:
    combo = _combo_hosts()
    packs, unpacks = _generated_combo_counts()
    operands = _generated_table_operands()
    copper_curved = gt6_resolve.resolve("OP.plateCurved(MT.Cu)")
    copper_double = gt6_resolve.resolve("OP.plateDouble(MT.Cu)")
    copper_curved_item = str((copper_curved.get("form") or {}).get("item") or "")
    copper_double_item = str((copper_double.get("form") or {}).get("item") or "")
    copper_tiny = GENERATED_TABLE / "copper" / "tiny_fluid_pipe.json"
    copper_huge = GENERATED_TABLE / "copper" / "huge_fluid_pipe.json"
    return {
        "schema": "gt6-connector-acquisition-overlay-v1",
        "capability_slug": FLUID_SLUG,
        "domain": "fluid",
        "source_revision": GT6_REVISION,
        "combo_catalog_hosts": len(combo),
        "combo_pack_recipes": packs,
        "combo_unpack_recipes": unpacks,
        "five_gauge_table_live": sum(operands.values()),
        "five_gauge_table_operands": operands,
        "copper_tiny_table": copper_tiny.is_file(),
        "copper_huge_table": copper_huge.is_file(),
        "copper_plate_curved": {
            "status": copper_curved.get("status"),
            "item": copper_curved_item,
            "table_recipe": copper_tiny.is_file(),
        },
        "copper_plate_double": {
            "status": copper_double.get("status"),
            "item": copper_double_item,
            "table_recipe": copper_huge.is_file(),
        },
        "existing_extruder": list(EXTRUDER_FILES),
        "nonmetal_catalog": 25,
        "note": (
            "Combo 2x2 medium / 3x3 small and shapeless unpack are GT6 grids "
            "on live pipes. Five-gauge table crafts use live curved_plate / "
            "double_plate only. Flat plate is never a stand-in. Existing "
            "extruder material_rules stay the machine obtain path."
        ),
    }


def current_gap(overlay: dict[str, Any]) -> dict[str, Any]:
    return {
        "schema": "gt6-fluid-pipe-acquisition-gap-v1",
        "capability_slug": FLUID_SLUG,
        "overlay": overlay["schema"],
        "blocked": [
            "player_complete EMI/reload/player signoff",
        ],
        "close_target": "runtime_ready",
        "note": (
            "Combo pack/unpack and gated five-gauge table crafts are live. "
            "Do not substitute plate for plateCurved. Nonmetal 25-row catalog "
            "stays DESIGN_POLICY history."
        ),
    }


def _copy_empty_nbt(pack: Path) -> None:
    if not EMPTY_SRC.is_file():
        raise FileNotFoundError(f"missing {census.relative(EMPTY_SRC)}")
    for dest in (
        pack / "structure" / "empty.nbt",
        pack / "gametest" / "structure" / "empty.nbt",
    ):
        dest.parent.mkdir(parents=True, exist_ok=True)
        if dest.is_file() and dest.read_bytes() == EMPTY_SRC.read_bytes():
            continue
        shutil.copyfile(EMPTY_SRC, dest)


def _generated_item_table_operands() -> dict[str, int]:
    counts: dict[str, int] = {}
    if not GENERATED_ITEM_TABLE.is_dir():
        return counts
    for path in GENERATED_ITEM_TABLE.glob("*/*.json"):
        document = json.loads(path.read_text(encoding="utf-8"))
        item = str(
            ((document.get("ingredients") or {}).get("P") or {}).get("item") or ""
        )
        operand = item.rsplit("/", 1)[-1] if item else ""
        counts[operand] = counts.get(operand, 0) + 1
    return {key: counts[key] for key in sorted(counts)}


def _generated_restrictive_count() -> int:
    if not GENERATED_RESTRICTIVE.is_dir():
        return 0
    return len(list(GENERATED_RESTRICTIVE.glob("*/*.json")))


def item_topology(unique_active: bool) -> dict[str, Any]:
    return {
        "schema_version": 1,
        "wave_slug": ITEM_SLUG,
        "unique_active_wave": ITEM_SLUG if unique_active else None,
        "status": "WAVE_READY",
        "source_revision": GT6_REVISION,
        "generated_by": "content/gt6-item-pipe-acquisition implementation",
        "append_only": False,
        "next_unassigned": True,
        "remaining_recipe_gap": 0,
    }


def item_readiness(unique_active: bool) -> dict[str, Any]:
    return {
        "schema_version": 1,
        "wave_slug": ITEM_SLUG,
        "unique_active_wave": ITEM_SLUG if unique_active else None,
        "status": ITEM_STATUS,
        "source_revision": GT6_REVISION,
    }


def build_item_overlay() -> dict[str, Any]:
    operands = _generated_item_table_operands()
    copper_curved = gt6_resolve.resolve("OP.plateCurved(MT.Cu)")
    copper_double = gt6_resolve.resolve("OP.plateDouble(MT.Cu)")
    steel_ring = gt6_resolve.resolve("OP.ring(MT.Steel)")
    copper_medium = GENERATED_ITEM_TABLE / "copper" / "item_pipe.json"
    copper_huge = GENERATED_ITEM_TABLE / "copper" / "huge_item_pipe.json"
    copper_restrictive = (
        GENERATED_RESTRICTIVE / "copper" / "restrictive_item_pipe.json"
    )
    return {
        "schema": "gt6-connector-acquisition-overlay-v1",
        "capability_slug": ITEM_SLUG,
        "domain": "item",
        "source_revision": GT6_REVISION,
        "item_table_live": sum(operands.values()),
        "item_table_operands": operands,
        "restrictive_live": _generated_restrictive_count(),
        "copper_medium_table": copper_medium.is_file(),
        "copper_huge_table": copper_huge.is_file(),
        "copper_restrictive": copper_restrictive.is_file(),
        "copper_plate_curved": {
            "status": copper_curved.get("status"),
            "item": str((copper_curved.get("form") or {}).get("item") or ""),
            "table_recipe": copper_medium.is_file(),
        },
        "copper_plate_double": {
            "status": copper_double.get("status"),
            "item": str((copper_double.get("form") or {}).get("item") or ""),
            "table_recipe": copper_huge.is_file(),
        },
        "steel_ring": {
            "status": steel_ring.get("status"),
            "item": str((steel_ring.get("form") or {}).get("item") or ""),
            "restrictive_recipe": copper_restrictive.is_file(),
        },
        "existing_extruder": list(ITEM_EXTRUDER_FILES),
        "nonmetal_catalog": 25,
        "note": (
            "Ordinary medium/large table crafts use live curved_plate; huge "
            "uses live double_plate. Restrictive uses matching gauge plus "
            "steel/ring. Flat plate and programmed_circuit are never stand-ins."
        ),
    }


def item_current_gap(overlay: dict[str, Any]) -> dict[str, Any]:
    return {
        "schema": "gt6-item-pipe-acquisition-gap-v1",
        "capability_slug": ITEM_SLUG,
        "overlay": overlay["schema"],
        "blocked": [
            "player_complete EMI/reload/player signoff",
        ],
        "close_target": "runtime_ready",
        "note": (
            "Gated item-pipe table crafts and restrictive steel-ring crafts "
            "are live. Do not substitute plate for plateCurved or invent a "
            "ring. Nonmetal 25-row catalog stays DESIGN_POLICY history."
        ),
    }


def eu_topology(unique_active: bool) -> dict[str, Any]:
    return {
        "schema_version": 1,
        "wave_slug": EU_SLUG,
        "unique_active_wave": EU_SLUG if unique_active else None,
        "status": "WAVE_READY",
        "source_revision": GT6_REVISION,
        "generated_by": "content/gt6-eu-cable-acquisition implementation",
        "append_only": False,
        "next_unassigned": True,
        "remaining_recipe_gap": 0,
    }


def eu_readiness(unique_active: bool) -> dict[str, Any]:
    return {
        "schema_version": 1,
        "wave_slug": EU_SLUG,
        "unique_active_wave": EU_SLUG if unique_active else None,
        "status": EU_STATUS,
        "source_revision": GT6_REVISION,
    }


def _generated_count(folder: Path) -> int:
    if not folder.is_dir():
        return 0
    return len(list(folder.glob("*/*.json")))


def build_eu_overlay() -> dict[str, Any]:
    copper_plate = gt6_resolve.resolve("OP.plate(MT.Cu)")
    copper_wire = gt6_resolve.resolve("OP.wireGt01(MT.Cu)")
    copper_cable = gt6_resolve.resolve("OP.cableGt01(MT.Cu)")
    rubber_plate = gt6_resolve.resolve("OP.plate(MT.Rubber)")
    copper_table = GENERATED_CABLE_TABLE / "copper" / "wire.json"
    copper_shapeless = GENERATED_CABLE_SHAPELESS / "copper" / "cable.json"
    copper_pack = GENERATED_CABLE_PACK / "copper" / "double_wire_from_wire.json"
    copper_unpack = (
        GENERATED_CABLE_UNPACK / "copper" / "wire_from_double_wire.json"
    )
    red_alloy_table = GENERATED_CABLE_TABLE / "red_alloy" / "wire.json"
    return {
        "schema": "gt6-connector-acquisition-overlay-v1",
        "capability_slug": EU_SLUG,
        "domain": "eu",
        "source_revision": GT6_REVISION,
        "plate2wire_live": _generated_count(GENERATED_CABLE_TABLE),
        "shapeless_cable_live": _generated_count(GENERATED_CABLE_SHAPELESS),
        "pack_live": _generated_count(GENERATED_CABLE_PACK),
        "unpack_live": _generated_count(GENERATED_CABLE_UNPACK),
        "copper_plate2wire": copper_table.is_file(),
        "copper_shapeless_cable": copper_shapeless.is_file(),
        "copper_pack_double": copper_pack.is_file(),
        "copper_unpack_double": copper_unpack.is_file(),
        "red_alloy_eu_table": red_alloy_table.is_file(),
        "copper_plate": {
            "status": copper_plate.get("status"),
            "item": str((copper_plate.get("form") or {}).get("item") or ""),
        },
        "copper_wire": {
            "status": copper_wire.get("status"),
            "item": str((copper_wire.get("form") or {}).get("item") or ""),
        },
        "copper_cable": {
            "status": copper_cable.get("status"),
            "item": str((copper_cable.get("form") or {}).get("item") or ""),
        },
        "rubber_plate": {
            "status": rubber_plate.get("status"),
            "item": str((rubber_plate.get("form") or {}).get("item") or ""),
            "tag": "cruciblecraft:any_rubber_plates",
        },
        "existing_assembler": census.relative(EU_ASSEMBLER),
        "existing_wiremill": census.relative(EU_WIREMILL),
        "note": (
            "plate2wire uses live plate plus wire cutter. Shapeless cableGt01/02 "
            "uses the live any_rubber_plates tag. Packing follows GT6 "
            "tAmount<10 / unpack always. Red alloy is not EU."
        ),
    }


def eu_current_gap(overlay: dict[str, Any]) -> dict[str, Any]:
    return {
        "schema": "gt6-eu-cable-acquisition-gap-v1",
        "capability_slug": EU_SLUG,
        "overlay": overlay["schema"],
        "blocked": [
            "player_complete EMI/reload/player signoff",
        ],
        "close_target": "runtime_ready",
        "note": (
            "Gated EU plate2wire, shapeless rubber insulation, and wire packing "
            "are live. Do not substitute programmed_circuit. Red alloy stays "
            "out of EU."
        ),
    }


def _write_wave(
    wave: Path,
    overlay_path: Path,
    overlay: dict[str, Any],
    gap: dict[str, Any],
    topology_doc: dict[str, Any],
    readiness_doc: dict[str, Any],
    lock_note: str,
    pack: Path,
) -> None:
    wave.mkdir(parents=True, exist_ok=True)
    _write_json(overlay_path, overlay)
    _write_json(wave / "current_gap.json", gap)
    _write_json(wave / "topology.json", topology_doc)
    _write_json(wave / "readiness.json", readiness_doc)
    _write_json(wave / "production_lock.json", {"note": lock_note})
    _copy_empty_nbt(pack)
    (wave / "r0_disposition_sha256.txt").write_text(
        _sha256(R0) + "\n", encoding="utf-8"
    )
    (wave / "baseline_ledger_sha256.txt").write_text(
        _sha256(LEDGER) + "\n", encoding="utf-8"
    )


def write(unique_active: bool = True, domain: str = "fluid") -> dict[str, Any]:
    if domain == "fluid":
        overlay = build_overlay()
        _write_wave(
            WAVE,
            OVERLAY_PATH,
            overlay,
            current_gap(overlay),
            topology(unique_active),
            readiness(unique_active),
            (
                "combo pack/unpack and gated five-gauge table crafts are live; "
                "not player_complete"
            ),
            PACK,
        )
        return overlay
    if domain == "item":
        overlay = build_item_overlay()
        _write_wave(
            ITEM_WAVE,
            ITEM_OVERLAY_PATH,
            overlay,
            item_current_gap(overlay),
            item_topology(unique_active),
            item_readiness(unique_active),
            (
                "gated item-pipe table crafts and restrictive steel-ring "
                "crafts are live; not player_complete"
            ),
            ITEM_PACK,
        )
        return overlay
    if domain == "eu":
        overlay = build_eu_overlay()
        _write_wave(
            EU_WAVE,
            EU_OVERLAY_PATH,
            overlay,
            eu_current_gap(overlay),
            eu_topology(unique_active),
            eu_readiness(unique_active),
            (
                "gated EU plate2wire, shapeless rubber insulation, and wire "
                "packing are live; not player_complete"
            ),
            EU_PACK,
        )
        return overlay
    if domain == "redstone":
        overlay = build_redstone_overlay()
        _write_wave(
            REDSTONE_WAVE,
            REDSTONE_OVERLAY_PATH,
            overlay,
            redstone_current_gap(overlay),
            redstone_topology(unique_active),
            redstone_readiness(unique_active),
            (
                "gated laminator plate/foil insulation for 27006/27056/27506 "
                "is live; not player_complete"
            ),
            REDSTONE_PACK,
        )
        return overlay
    raise ValueError(f"domain {domain} is not issued yet")


def check() -> list[str]:
    errors: list[str] = []
    if not OVERLAY_PATH.is_file():
        return [f"missing {census.relative(OVERLAY_PATH)}"]
    live = build_overlay()
    committed = census.load_json(OVERLAY_PATH)
    drift = census.first_json_diff(live, committed)
    if drift:
        errors.append(f"acquisition_overlay.json drifted: {drift}")
    gap_path = WAVE / "current_gap.json"
    if not gap_path.is_file():
        errors.append("missing current_gap.json")
    else:
        gap_drift = census.first_json_diff(
            current_gap(committed), census.load_json(gap_path)
        )
        if gap_drift:
            errors.append(f"current_gap.json drifted: {gap_drift}")
    provider = RECIPE_PROVIDER.read_text(encoding="utf-8") if RECIPE_PROVIDER.is_file() else ""
    if "unpack_quadruple_fluid_pipe" not in provider:
        errors.append("ModRecipeProvider lost quadruple unpack")
    if "unpack_nonuple_fluid_pipe" not in provider:
        errors.append("ModRecipeProvider lost nonuple unpack")
    if "addMetalFluidPipeTableRecipes" not in provider:
        errors.append("ModRecipeProvider lost five-gauge table crafts")
    if "CURVED_PLATE" not in provider or "DOUBLE_PLATE" not in provider:
        errors.append("five-gauge table crafts are not gated on curved/double plate")
    tests = GAME_TESTS.read_text(encoding="utf-8") if GAME_TESTS.is_file() else ""
    for name in EXPECTED_TESTS:
        if f"void {name}" not in tests:
            errors.append(f"missing GameTest {name}")
    core = CORE_TESTS.read_text(encoding="utf-8")
    for name in EXPECTED_TESTS:
        if name in core:
            errors.append(f"CrucibleCraftGameTests absorbed {name}")
    expected_pack = live["combo_pack_recipes"]
    expected_unpack = live["combo_unpack_recipes"]
    if expected_pack <= 0 or expected_pack != expected_unpack:
        errors.append("generated combo pack/unpack counts drifted")
    copper_pack = GENERATED_COMBO / "copper" / "quadruple_fluid_pipe.json"
    copper_unpack = GENERATED_COMBO / "copper" / "unpack_quadruple_fluid_pipe.json"
    if not copper_pack.is_file():
        errors.append("missing generated copper quadruple pack recipe")
    if not copper_unpack.is_file():
        errors.append("missing generated copper quadruple unpack recipe")
    if not live["copper_tiny_table"]:
        errors.append("missing generated copper tiny table recipe")
    if not live["copper_huge_table"]:
        errors.append("missing generated copper huge table recipe")
    operands = live.get("five_gauge_table_operands") or {}
    if "plate" in operands:
        errors.append("five-gauge table crafts used flat plate")
    if "curved_plate" not in operands or "double_plate" not in operands:
        errors.append("five-gauge table crafts lost curved_plate or double_plate")
    for path in GENERATED_TABLE.glob("*/*.json") if GENERATED_TABLE.is_dir() else []:
        document = json.loads(path.read_text(encoding="utf-8"))
        item = str(
            ((document.get("ingredients") or {}).get("P") or {}).get("item") or ""
        )
        if (
            item.endswith("/plate")
            and not item.endswith("/curved_plate")
            and not item.endswith("/double_plate")
        ):
            errors.append(
                f"{census.relative(path)} used flat plate as a curved_plate stand-in"
            )
    for name in EXTRUDER_FILES:
        if not (EXTRUDER / name).is_file():
            errors.append(f"missing existing extruder obtain {name}")
    this_file = Path(__file__).read_text(encoding="utf-8")
    closed_runtime = "gt6_fluid" + "_pipe_runtime"
    if any(
        line.startswith("from tools import " + closed_runtime)
        for line in this_file.splitlines()
    ):
        errors.append("acquisition module imported closed fluid-pipe runtime")
    closed_item = "gt6_item" + "_pipe_runtime"
    if any(
        line.startswith("from tools import " + closed_item)
        for line in this_file.splitlines()
    ):
        errors.append("acquisition module imported closed item-pipe runtime")
    errors.extend(_check_item())
    errors.extend(_check_eu())
    errors.extend(_check_redstone())
    return errors


def _check_item() -> list[str]:
    errors: list[str] = []
    if not ITEM_OVERLAY_PATH.is_file():
        return errors
    live = build_item_overlay()
    committed = census.load_json(ITEM_OVERLAY_PATH)
    drift = census.first_json_diff(live, committed)
    if drift:
        errors.append(f"item acquisition_overlay.json drifted: {drift}")
    gap_path = ITEM_WAVE / "current_gap.json"
    if not gap_path.is_file():
        errors.append("missing item current_gap.json")
    else:
        gap_drift = census.first_json_diff(
            item_current_gap(committed), census.load_json(gap_path)
        )
        if gap_drift:
            errors.append(f"item current_gap.json drifted: {gap_drift}")
    provider = RECIPE_PROVIDER.read_text(encoding="utf-8") if RECIPE_PROVIDER.is_file() else ""
    if "addMetalItemPipeTableRecipes" not in provider:
        errors.append("ModRecipeProvider lost item-pipe table crafts")
    if "addRestrictiveItemPipeRecipes" not in provider:
        errors.append("ModRecipeProvider lost restrictive item-pipe crafts")
    if "pipe/item_table/" not in provider:
        errors.append("item-pipe table crafts are not isolated from fluid table/")
    if "steel" not in provider or "RING" not in provider:
        errors.append("restrictive crafts are not gated on steel/ring")
    tests = ITEM_GAME_TESTS.read_text(encoding="utf-8") if ITEM_GAME_TESTS.is_file() else ""
    for name in ITEM_EXPECTED_TESTS:
        if f"void {name}" not in tests:
            errors.append(f"missing item GameTest {name}")
    core = CORE_TESTS.read_text(encoding="utf-8")
    for name in ITEM_EXPECTED_TESTS:
        if name in core and name != "nonmetalCatalogStaysTwentyFive":
            errors.append(f"CrucibleCraftGameTests absorbed {name}")
    if not live["copper_medium_table"]:
        errors.append("missing generated copper medium item-pipe table recipe")
    if not live["copper_huge_table"]:
        errors.append("missing generated copper huge item-pipe table recipe")
    if not live["copper_restrictive"]:
        errors.append("missing generated copper restrictive recipe")
    operands = live.get("item_table_operands") or {}
    if "plate" in operands:
        errors.append("item-pipe table crafts used flat plate")
    if "curved_plate" not in operands or "double_plate" not in operands:
        errors.append("item-pipe table crafts lost curved_plate or double_plate")
    if live["restrictive_live"] <= 0:
        errors.append("restrictive crafts were not generated")
    for path in (
        GENERATED_ITEM_TABLE.glob("*/*.json")
        if GENERATED_ITEM_TABLE.is_dir()
        else []
    ):
        document = json.loads(path.read_text(encoding="utf-8"))
        item = str(
            ((document.get("ingredients") or {}).get("P") or {}).get("item") or ""
        )
        if (
            item.endswith("/plate")
            and not item.endswith("/curved_plate")
            and not item.endswith("/double_plate")
        ):
            errors.append(
                f"{census.relative(path)} used flat plate as a curved_plate stand-in"
            )
    for path in (
        GENERATED_RESTRICTIVE.glob("*/*.json")
        if GENERATED_RESTRICTIVE.is_dir()
        else []
    ):
        document = json.loads(path.read_text(encoding="utf-8"))
        ring = str(
            ((document.get("ingredients") or {}).get("R") or {}).get("item") or ""
        )
        if ring != "cruciblecraft:steel/ring":
            errors.append(
                f"{census.relative(path)} did not use steel/ring"
            )
        if "programmed_circuit" in json.dumps(document):
            errors.append(
                f"{census.relative(path)} used programmed_circuit"
            )
    for name in ITEM_EXTRUDER_FILES:
        if not (EXTRUDER / name).is_file():
            errors.append(f"missing existing item extruder obtain {name}")
    return errors


def _check_eu() -> list[str]:
    errors: list[str] = []
    if not EU_OVERLAY_PATH.is_file():
        return errors
    live = build_eu_overlay()
    committed = census.load_json(EU_OVERLAY_PATH)
    drift = census.first_json_diff(live, committed)
    if drift:
        errors.append(f"eu acquisition_overlay.json drifted: {drift}")
    gap_path = EU_WAVE / "current_gap.json"
    if not gap_path.is_file():
        errors.append("missing eu current_gap.json")
    else:
        gap_drift = census.first_json_diff(
            eu_current_gap(committed), census.load_json(gap_path)
        )
        if gap_drift:
            errors.append(f"eu current_gap.json drifted: {gap_drift}")
    provider = RECIPE_PROVIDER.read_text(encoding="utf-8") if RECIPE_PROVIDER.is_file() else ""
    if "addEuWireTableRecipes" not in provider:
        errors.append("ModRecipeProvider lost EU plate2wire")
    if "addEuCableShapelessRecipes" not in provider:
        errors.append("ModRecipeProvider lost EU shapeless cable")
    if "addEuWirePackRecipes" not in provider:
        errors.append("ModRecipeProvider lost EU wire packing")
    if "any_rubber_plates" not in provider:
        errors.append("EU shapeless cable is not gated on any_rubber_plates")
    if "cable/table/" not in provider:
        errors.append("EU plate2wire crafts are not isolated under cable/table/")
    tests = EU_GAME_TESTS.read_text(encoding="utf-8") if EU_GAME_TESTS.is_file() else ""
    for name in EU_EXPECTED_TESTS:
        if f"void {name}" not in tests:
            errors.append(f"missing EU GameTest {name}")
    core = CORE_TESTS.read_text(encoding="utf-8")
    for name in EU_EXPECTED_TESTS:
        if name in core:
            errors.append(f"CrucibleCraftGameTests absorbed {name}")
    if not live["copper_plate2wire"]:
        errors.append("missing generated copper plate2wire recipe")
    if not live["copper_shapeless_cable"]:
        errors.append("missing generated copper shapeless cable recipe")
    if not live["copper_pack_double"]:
        errors.append("missing generated copper double-wire pack recipe")
    if not live["copper_unpack_double"]:
        errors.append("missing generated copper double-wire unpack recipe")
    if live["red_alloy_eu_table"]:
        errors.append("red_alloy received an EU plate2wire craft")
    if live["plate2wire_live"] <= 0:
        errors.append("EU plate2wire crafts were not generated")
    if live["shapeless_cable_live"] <= 0:
        errors.append("EU shapeless cable crafts were not generated")
    red_dir = GENERATED_CABLE_TABLE / "red_alloy"
    if red_dir.is_dir():
        errors.append("EU table crafts included red_alloy")
    for folder in (
        GENERATED_CABLE_TABLE,
        GENERATED_CABLE_SHAPELESS,
        GENERATED_CABLE_PACK,
        GENERATED_CABLE_UNPACK,
    ):
        for path in folder.glob("*/*.json") if folder.is_dir() else []:
            text = path.read_text(encoding="utf-8")
            if "programmed_circuit" in text:
                errors.append(
                    f"{census.relative(path)} used programmed_circuit"
                )
    if not EU_ASSEMBLER.is_file():
        errors.append("missing existing assembler wire_and_rubber_to_cable")
    if not EU_WIREMILL.is_file():
        errors.append("missing existing wiremill ingot_to_wire")
    this_file = Path(__file__).read_text(encoding="utf-8")
    closed_eu = "gt6_eu" + "_wire_cable_runtime"
    if any(
        line.startswith("from tools import " + closed_eu)
        for line in this_file.splitlines()
    ):
        errors.append("acquisition module imported closed EU wire runtime")
    return errors


def redstone_topology(unique_active: bool) -> dict[str, Any]:
    return {
        "schema_version": 1,
        "wave_slug": REDSTONE_SLUG,
        "unique_active_wave": REDSTONE_SLUG if unique_active else None,
        "status": "WAVE_READY",
        "source_revision": GT6_REVISION,
        "generated_by": "content/gt6-redstone-wire-acquisition implementation",
        "append_only": False,
        "next_unassigned": True,
        "remaining_recipe_gap": 0,
    }


def redstone_readiness(unique_active: bool) -> dict[str, Any]:
    return {
        "schema_version": 1,
        "wave_slug": REDSTONE_SLUG,
        "unique_active_wave": REDSTONE_SLUG if unique_active else None,
        "status": REDSTONE_STATUS,
        "source_revision": GT6_REVISION,
    }


def build_redstone_overlay() -> dict[str, Any]:
    rubber_plate = gt6_resolve.resolve("OP.plate(MT.Rubber)")
    rubber_foil = gt6_resolve.resolve("OP.foil(MT.Rubber)")
    red_wire = gt6_resolve.resolve("OP.wireGt01(MT.RedAlloy)")
    red_cable = gt6_resolve.resolve("OP.cableGt01(MT.RedAlloy)")
    files = list(GENERATED_REDSTONE.glob("*/*.json")) if GENERATED_REDSTONE.is_dir() else []
    plates = [path for path in files if path.name.endswith("cable_from_plate.json")]
    foils = [path for path in files if path.name.endswith("cable_from_foil.json")]
    return {
        "schema": "gt6-connector-acquisition-overlay-v1",
        "capability_slug": REDSTONE_SLUG,
        "domain": "redstone",
        "source_revision": GT6_REVISION,
        "plate_live": len(plates),
        "foil_live": len(foils),
        "red_alloy_plate": (
            GENERATED_REDSTONE / "red_alloy" / "cable_from_plate.json"
        ).is_file(),
        "red_alloy_foil": (
            GENERATED_REDSTONE / "red_alloy" / "cable_from_foil.json"
        ).is_file(),
        "signalum_plate": (
            GENERATED_REDSTONE / "signalum" / "cable_from_plate.json"
        ).is_file(),
        "lumium_plate": (
            GENERATED_REDSTONE / "lumium" / "cable_from_plate.json"
        ).is_file(),
        "rubber_plate": {
            "status": rubber_plate.get("status"),
            "item": str((rubber_plate.get("form") or {}).get("item") or ""),
        },
        "rubber_foil": {
            "status": rubber_foil.get("status"),
            "item": str((rubber_foil.get("form") or {}).get("item") or ""),
            "recipe": (
                GENERATED_REDSTONE / "red_alloy" / "cable_from_foil.json"
            ).is_file(),
        },
        "red_alloy_wire": {
            "status": red_wire.get("status"),
            "item": str((red_wire.get("form") or {}).get("item") or ""),
        },
        "red_alloy_cable": {
            "status": red_cable.get("status"),
            "item": str((red_cable.get("form") or {}).get("item") or ""),
        },
        "note": (
            "Laminator plate/foil insulation on live redstone cables. "
            "Not EU and not tin/cable. Foil recipes stay gated on live rubber/foil."
        ),
    }


def redstone_current_gap(overlay: dict[str, Any]) -> dict[str, Any]:
    return {
        "schema": "gt6-redstone-wire-acquisition-gap-v1",
        "capability_slug": REDSTONE_SLUG,
        "overlay": overlay["schema"],
        "blocked": [
            "player_complete EMI/reload/player signoff",
            "torch/repeater host",
        ],
        "close_target": "runtime_ready",
        "note": (
            "Gated laminator insulation is live. Do not substitute "
            "programmed_circuit or fold onto tin/cable."
        ),
    }


def _check_redstone() -> list[str]:
    errors: list[str] = []
    if not REDSTONE_OVERLAY_PATH.is_file():
        return errors
    live = build_redstone_overlay()
    committed = census.load_json(REDSTONE_OVERLAY_PATH)
    drift = census.first_json_diff(live, committed)
    if drift:
        errors.append(f"redstone acquisition_overlay.json drifted: {drift}")
    gap_path = REDSTONE_WAVE / "current_gap.json"
    if not gap_path.is_file():
        errors.append("missing redstone current_gap.json")
    else:
        gap_drift = census.first_json_diff(
            redstone_current_gap(committed), census.load_json(gap_path)
        )
        if gap_drift:
            errors.append(f"redstone current_gap.json drifted: {gap_drift}")
    provider = RECIPE_PROVIDER.read_text(encoding="utf-8") if RECIPE_PROVIDER.is_file() else ""
    if "addInsulatedRedstoneLaminatorRecipes" not in provider:
        errors.append("ModRecipeProvider lost insulated redstone laminator")
    if "redstone/laminator/" not in provider:
        errors.append("redstone laminator crafts are not isolated")
    tests = (
        REDSTONE_GAME_TESTS.read_text(encoding="utf-8")
        if REDSTONE_GAME_TESTS.is_file()
        else ""
    )
    for name in REDSTONE_EXPECTED_TESTS:
        if f"void {name}" not in tests:
            errors.append(f"missing redstone GameTest {name}")
    core = CORE_TESTS.read_text(encoding="utf-8")
    for name in REDSTONE_EXPECTED_TESTS:
        if name in core:
            errors.append(f"CrucibleCraftGameTests absorbed {name}")
    if not live["red_alloy_plate"]:
        errors.append("missing generated red_alloy plate laminator recipe")
    if not live["signalum_plate"]:
        errors.append("missing generated signalum plate laminator recipe")
    if not live["lumium_plate"]:
        errors.append("missing generated lumium plate laminator recipe")
    foil_ok = str((live.get("rubber_foil") or {}).get("status") or "") == "ok"
    if foil_ok and not live["red_alloy_foil"]:
        errors.append("rubber/foil is live but foil laminator is missing")
    if not foil_ok and live["red_alloy_foil"]:
        errors.append("foil laminator was invented without rubber/foil")
    if live["plate_live"] != 3:
        errors.append(f"expected 3 plate laminator recipes, got {live['plate_live']}")
    for path in GENERATED_REDSTONE.glob("*/*.json") if GENERATED_REDSTONE.is_dir() else []:
        document = json.loads(path.read_text(encoding="utf-8"))
        text = json.dumps(document)
        if "programmed_circuit" in text:
            errors.append(f"{census.relative(path)} used programmed_circuit")
        if "tin/cable" in text:
            errors.append(f"{census.relative(path)} folded onto tin/cable")
        recipe = document.get("recipe") or document
        result = str(
            ((recipe.get("item_outputs") or [{}])[0] or {}).get("id") or ""
        )
        if result.endswith("tin/cable"):
            errors.append(f"{census.relative(path)} outputs tin/cable")
    this_file = Path(__file__).read_text(encoding="utf-8")
    closed_insulated = "gt6_insulated" + "_redstone_runtime"
    if any(
        line.startswith("from tools import " + closed_insulated)
        for line in this_file.splitlines()
    ):
        errors.append("acquisition module imported closed insulated redstone runtime")
    return errors


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    parser.add_argument(
        "--unique-active",
        action=argparse.BooleanOptionalAction,
        default=True,
    )
    parser.add_argument("--domain", default="fluid")
    args = parser.parse_args(argv)
    if args.write:
        write(unique_active=args.unique_active, domain=args.domain)
    errors = check() if args.check or not args.write else []
    if errors:
        print("\n".join(errors), file=sys.stderr)
        return 1
    if args.write:
        print(f"wrote {args.domain}")
    else:
        print("gt6-connector-acquisition ok")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
