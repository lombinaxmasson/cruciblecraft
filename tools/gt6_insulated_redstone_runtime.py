#!/usr/bin/env python3
"""Register GT6 insulated redstone 27006/27056/27506 in-place.

Not EU CableBlock. Do not fold onto ordinary metal */cable. Closed mte-redstone-wire
keeps insulated metas out_of_denominator. Close at runtime_ready.
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
from tools import io_common as io

SLUG = "content/gt6-insulated-redstone-runtime"
STATUS = "INSULATED_REDSTONE_RUNTIME_READY"
GT6_REVISION = io.SOURCE_REVISION
WAVE = census.TOOLS / "waves" / "content" / "gt6-insulated-redstone-runtime"
BASELINE = census.TOOLS / "waves" / "content" / "gt6-pipe-cable-baseline"
LEDGER = BASELINE / "identity_resolution_ledger.json"
R0 = (
    census.TOOLS
    / "waves"
    / "portfolio"
    / "mte-identity-disposition-r0"
    / "disposition_ledger.json"
)
KIND_JAVA = (
    census.ROOT
    / "src"
    / "main"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "content"
    / "redstonewire"
    / "RedstoneWireKind.java"
)
BLOCKS_JAVA = (
    census.ROOT
    / "src"
    / "main"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "registry"
    / "ModBlocks.java"
)
CATALOG_JAVA = (
    census.ROOT
    / "src"
    / "main"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "energy"
    / "cable"
    / "ElectricalConductorCatalog.java"
)
GAME_TESTS = (
    census.ROOT
    / "src"
    / "main"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "gametest"
    / "InsulatedRedstoneRuntimeGameTests.java"
)
CORE_TESTS = (
    census.ROOT
    / "src"
    / "main"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "gametest"
    / "CrucibleCraftGameTests.java"
)
ASSETS = census.ROOT / "src" / "main" / "resources" / "assets" / "cruciblecraft"
PACK = (
    census.ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft_wave_content_gt6_insulated_redstone_runtime"
)
EMPTY_SRC = (
    census.ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft_wave_content_mte_redstone_wire"
    / "structure"
    / "empty.nbt"
)
OVERLAY_PATH = WAVE / "insulated_overlay.json"
LIVE_PATHS = {
    27006: "red_alloy/cable",
    27056: "signalum/cable",
    27506: "lumium/cable",
}
EXPECTED_TESTS = [
    "insulatedCablesAreLiveRedstoneBlocks",
    "insulatedCablesAreNotEuOrTinAlias",
    "insulatedJoinsBareRedstoneNetwork",
    "lumiumCableDoesNotGlow",
]
MODEL_FILES = (
    ASSETS / "models" / "block" / "redstone_cable" / "core.json",
    ASSETS / "models" / "block" / "redstone_cable" / "arm.json",
    ASSETS / "models" / "block" / "redstone_cable" / "item.json",
    ASSETS / "blockstates" / "red_alloy" / "cable.json",
    ASSETS / "blockstates" / "signalum" / "cable.json",
    ASSETS / "blockstates" / "lumium" / "cable.json",
    ASSETS / "models" / "item" / "red_alloy" / "cable.json",
    ASSETS / "models" / "item" / "signalum" / "cable.json",
    ASSETS / "models" / "item" / "lumium" / "cable.json",
)


def _write_json(path: Path, document: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    payload = json.dumps(document, ensure_ascii=False, indent=2) + "\n"
    if path.is_file() and path.read_text(encoding="utf-8") == payload:
        return
    path.write_text(payload, encoding="utf-8")


def _sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def topology(unique_active: bool) -> dict[str, Any]:
    return {
        "schema_version": 1,
        "wave_slug": SLUG,
        "unique_active_wave": SLUG if unique_active else None,
        "status": "WAVE_READY",
        "source_revision": GT6_REVISION,
        "generated_by": "content/gt6-insulated-redstone-runtime implementation",
        "append_only": False,
        "next_unassigned": True,
        "remaining_recipe_gap": 0,
    }


def readiness(unique_active: bool) -> dict[str, Any]:
    return {
        "schema_version": 1,
        "wave_slug": SLUG,
        "unique_active_wave": SLUG if unique_active else None,
        "status": STATUS,
        "source_revision": GT6_REVISION,
    }


def build_overlay() -> dict[str, Any]:
    rows = [
        {
            "meta": meta,
            "live_block": f"cruciblecraft:{path}",
            "disposition": "in_place",
            "not_eu_cable": True,
            "not_tin_cable_alias": True,
        }
        for meta, path in LIVE_PATHS.items()
    ]
    return {
        "schema": "gt6-insulated-redstone-overlay-v1",
        "capability_slug": SLUG,
        "source_revision": GT6_REVISION,
        "counts": {"in_place": len(rows), "catalog_dummy_folds": 0},
        "rows": rows,
        "note": (
            "Loader-out 27006/27056/27506 become live RedstoneWireBlockItems at "
            "modern */cable ids. Not ElectricalConductorCatalog. Closed "
            "mte-redstone-wire identity ledger stays historical out_of_denominator."
        ),
    }


def current_gap(overlay: dict[str, Any]) -> dict[str, Any]:
    return {
        "schema": "gt6-insulated-redstone-gap-v1",
        "capability_slug": SLUG,
        "in_place": overlay["counts"]["in_place"],
        "still_dummy": [],
        "blocked": [
            "laminator survival obtain grids",
            "torch/repeater host-attach on insulated redstone",
            "plasma/magic destroy/replace, flammable, contactDamage",
        ],
        "close_target": "runtime_ready",
        "note": (
            "Closed mte-redstone-wire still lists insulated extras as blocked. "
            "This overlay is the current live projection."
        ),
    }


def _copy_empty_nbt() -> None:
    if not EMPTY_SRC.is_file():
        raise FileNotFoundError(f"missing {census.relative(EMPTY_SRC)}")
    for dest in (
        PACK / "structure" / "empty.nbt",
        PACK / "gametest" / "structure" / "empty.nbt",
    ):
        dest.parent.mkdir(parents=True, exist_ok=True)
        if dest.is_file() and dest.read_bytes() == EMPTY_SRC.read_bytes():
            continue
        shutil.copyfile(EMPTY_SRC, dest)


def write(unique_active: bool = True) -> dict[str, Any]:
    overlay = build_overlay()
    WAVE.mkdir(parents=True, exist_ok=True)
    _write_json(OVERLAY_PATH, overlay)
    _write_json(WAVE / "current_gap.json", current_gap(overlay))
    _write_json(WAVE / "topology.json", topology(unique_active))
    _write_json(WAVE / "readiness.json", readiness(unique_active))
    _write_json(
        WAVE / "production_lock.json",
        {
            "note": (
                "insulated redstone 27006/27056/27506 are live RedstoneWireBlockItems; "
                "not player_complete"
            )
        },
    )
    _copy_empty_nbt()
    (WAVE / "r0_disposition_sha256.txt").write_text(
        _sha256(R0) + "\n", encoding="utf-8"
    )
    (WAVE / "baseline_ledger_sha256.txt").write_text(
        _sha256(LEDGER) + "\n", encoding="utf-8"
    )
    return overlay


def check() -> list[str]:
    errors: list[str] = []
    if not OVERLAY_PATH.is_file():
        return [f"missing {census.relative(OVERLAY_PATH)}"]
    live = build_overlay()
    committed = census.load_json(OVERLAY_PATH)
    drift = census.first_json_diff(live, committed)
    if drift:
        errors.append(f"insulated_overlay.json drifted: {drift}")
    gap_path = WAVE / "current_gap.json"
    if not gap_path.is_file():
        errors.append("missing current_gap.json")
    else:
        gap_drift = census.first_json_diff(
            current_gap(committed), census.load_json(gap_path)
        )
        if gap_drift:
            errors.append(f"current_gap.json drifted: {gap_drift}")
    kind = KIND_JAVA.read_text(encoding="utf-8")
    if "EXPECTED_SIZE = 3" not in kind:
        errors.append("RedstoneWireKind.EXPECTED_SIZE drifted from 3")
    if "EXPECTED_CATALOG = 6" not in kind:
        errors.append("RedstoneWireKind.EXPECTED_CATALOG drifted from 6")
    if "RED_ALLOY_CABLE" not in kind or "LUMIUM_CABLE" not in kind:
        errors.append("insulated enum values missing")
    blocks = BLOCKS_JAVA.read_text(encoding="utf-8")
    if "redstoneWireCatalogById" not in blocks:
        errors.append("ModBlocks lost redstoneWireCatalogById")
    if "Bare redstone wire map drifted from 3" not in blocks:
        errors.append("bare redstone map no longer pinned at 3")
    catalog = CATALOG_JAVA.read_text(encoding="utf-8")
    if '"red_alloy", "signalum", "lumium"' not in catalog.replace("\n", " "):
        if "red_alloy" not in catalog or "signalum" not in catalog:
            errors.append("ElectricalConductorCatalog lost redstone exclusion")
    tests = GAME_TESTS.read_text(encoding="utf-8") if GAME_TESTS.is_file() else ""
    for name in EXPECTED_TESTS:
        if f"void {name}" not in tests:
            errors.append(f"missing GameTest {name}")
    core = CORE_TESTS.read_text(encoding="utf-8")
    for name in EXPECTED_TESTS:
        if name in core:
            errors.append(f"CrucibleCraftGameTests absorbed {name}")
    for path in MODEL_FILES:
        if not path.is_file():
            errors.append(f"missing {census.relative(path)}")
        else:
            text = path.read_text(encoding="utf-8")
            if "iron_ingot" in text:
                errors.append(f"{census.relative(path)} uses iron_ingot")
            if "insulation_tiny" not in text and "redstone_cable" not in text:
                errors.append(f"{census.relative(path)} missing insulation art")
    if not (PACK / "structure" / "empty.nbt").is_file():
        errors.append("missing wave structure/empty.nbt")
    if not (PACK / "gametest" / "structure" / "empty.nbt").is_file():
        errors.append("missing wave gametest/structure/empty.nbt")
    r0_hash = WAVE / "r0_disposition_sha256.txt"
    if not r0_hash.is_file() or r0_hash.read_text(encoding="utf-8").strip() != _sha256(R0):
        errors.append("R0 disposition ledger was modified")
    baseline_hash = WAVE / "baseline_ledger_sha256.txt"
    if (
        not baseline_hash.is_file()
        or baseline_hash.read_text(encoding="utf-8").strip() != _sha256(LEDGER)
    ):
        errors.append("baseline identity_resolution_ledger was modified")
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
    args = parser.parse_args(argv)
    if args.write:
        write(unique_active=args.unique_active)
    errors = check() if args.check or not args.write else []
    if errors:
        print("\n".join(errors), file=sys.stderr)
        return 1
    if args.write:
        print(f"wrote {SLUG}")
    else:
        print(f"{SLUG} ok")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
