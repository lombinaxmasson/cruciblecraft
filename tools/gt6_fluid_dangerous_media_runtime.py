#!/usr/bin/env python3
"""GT6 fluid-pipe plasma/magic tick, flammable, and contactDamage.

Fill is allowed. Tick trashes plasma/magic. Magic 1% replace is air (no
Thaumcraft). Close at runtime_ready. Do not import gt6_fluid_pipe_runtime.
"""
from __future__ import annotations

import argparse
import json
import shutil
import sys
from pathlib import Path
from typing import Any

from tools import census_common as census
from tools import io_common as io

SLUG = "content/gt6-fluid-dangerous-media-runtime"
STATUS = "FLUID_DANGEROUS_MEDIA_RUNTIME_READY"
GT6_REVISION = io.SOURCE_REVISION
WAVE = census.TOOLS / "waves" / "content" / "gt6-fluid-dangerous-media-runtime"
BASELINE = census.TOOLS / "waves" / "content" / "gt6-pipe-cable-baseline"
LEDGER = BASELINE / "identity_resolution_ledger.json"
R0 = (
    census.TOOLS
    / "waves"
    / "portfolio"
    / "mte-identity-disposition-r0"
    / "disposition_ledger.json"
)
BE = (
    census.ROOT
    / "src"
    / "main"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "content"
    / "blockentity"
    / "FluidPipeBlockEntity.java"
)
BLOCK = (
    census.ROOT
    / "src"
    / "main"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "content"
    / "block"
    / "FluidPipeBlock.java"
)
DANGER = (
    census.ROOT
    / "src"
    / "main"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "logistics"
    / "pipe"
    / "fluid"
    / "FluidPipeDangerousMedia.java"
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
    / "FluidDangerousMediaRuntimeGameTests.java"
)
CLOSED_TESTS = (
    census.ROOT
    / "src"
    / "test"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "gametest"
    / "FluidPipeRuntimeGameTests.java"
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
    / "cruciblecraft_wave_content_gt6_fluid_dangerous_media_runtime"
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
OVERLAY_PATH = WAVE / "dangerous_overlay.json"
EXPECTED_TESTS = [
    "contactDamageHurtsLivingEntity",
    "flammableOverTempLightsAdjacent",
    "magicFluidFillsThenTrashes",
    "plasmaMagicNotMappedToGasAcid",
]


def _write_json(path: Path, document: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    payload = json.dumps(document, ensure_ascii=False, indent=2) + "\n"
    if path.is_file() and path.read_text(encoding="utf-8") == payload:
        return
    path.write_text(payload, encoding="utf-8")


def topology(unique_active: bool) -> dict[str, Any]:
    return {
        "schema_version": 1,
        "wave_slug": SLUG,
        "unique_active_wave": SLUG if unique_active else None,
        "status": "WAVE_READY",
        "source_revision": GT6_REVISION,
        "generated_by": "content/gt6-fluid-dangerous-media-runtime implementation",
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
    return {
        "schema": "gt6-fluid-dangerous-media-overlay-v1",
        "capability_slug": SLUG,
        "source_revision": GT6_REVISION,
        "fill_gate": "removed",
        "tick": "FluidPipeDangerousMedia",
        "magic_replace": "air (no Thaumcraft flux)",
        "plasma_fluids": "tick path live; CC may have no PLASMA-tagged fluid",
        "flammable": "ignitedByLava + getFlammability 150 + adjacent fire on over-temp",
        "contact_damage": "entityInside hotFloor when tank is not empty",
        "note": (
            "Closed fluid-pipe runtime_notes stay historical fail-closed. "
            "This overlay is the current live projection. No new pipe specs."
        ),
    }


def current_gap(overlay: dict[str, Any]) -> dict[str, Any]:
    return {
        "schema": "gt6-fluid-dangerous-media-gap-v1",
        "capability_slug": SLUG,
        "overlay": overlay["schema"],
        "blocked": [
            "survival obtain grids",
            "Thaumcraft flux gas/goo replace (air fallback)",
        ],
        "close_target": "runtime_ready",
        "note": (
            "Closed gt6-fluid-pipe-runtime notes keep the fail-closed sentence. "
            "Live fill then tick is this overlay."
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
                "plasma/magic fill-then-tick, flammable, and contactDamage "
                "are live; not player_complete"
            )
        },
    )
    _copy_empty_nbt()
    return overlay


def check() -> list[str]:
    errors: list[str] = []
    if not OVERLAY_PATH.is_file():
        return [f"missing {census.relative(OVERLAY_PATH)}"]
    live = build_overlay()
    committed = census.load_json(OVERLAY_PATH)
    drift = census.first_json_diff(live, committed)
    if drift:
        errors.append(f"dangerous_overlay.json drifted: {drift}")
    gap_path = WAVE / "current_gap.json"
    if not gap_path.is_file():
        errors.append("missing current_gap.json")
    else:
        gap_drift = census.first_json_diff(
            current_gap(committed), census.load_json(gap_path)
        )
        if gap_drift:
            errors.append(f"current_gap.json drifted: {gap_drift}")
    be = BE.read_text(encoding="utf-8") if BE.is_file() else ""
    if "FluidPipeDangerousMedia.tick" not in be:
        errors.append("FluidPipeBlockEntity lost dangerous-media tick")
    if "FluidPipeBlockedMedia.rejects(" in be:
        errors.append("plasma/magic is still rejected at fill")
    if "kindOf(resource)" not in be:
        errors.append("validateFluid no longer skips plasma/magic")
    danger = DANGER.read_text(encoding="utf-8") if DANGER.is_file() else ""
    if "PLASMA_TRASH = 64" not in danger:
        errors.append("plasma trash drifted from 64")
    if "MAGIC_TRASH_LIQUID = 4" not in danger:
        errors.append("magic liquid trash drifted from 4")
    if "Blocks.AIR" not in danger:
        errors.append("magic 1% replace is not air")
    if "tickDeterministic" not in danger:
        errors.append("missing deterministic GameTest tick")
    block = BLOCK.read_text(encoding="utf-8") if BLOCK.is_file() else ""
    if "GT6_FLAMMABILITY = 150" not in block:
        errors.append("FluidPipeBlock flammability drifted from 150")
    if "entityInside" not in block:
        errors.append("FluidPipeBlock lost contact entityInside")
    if "ignitedByLava" not in block:
        errors.append("flammable pipes lost ignitedByLava")
    tests = GAME_TESTS.read_text(encoding="utf-8") if GAME_TESTS.is_file() else ""
    core = CORE_TESTS.read_text(encoding="utf-8")
    for name in EXPECTED_TESTS:
        if name in core:
            errors.append(f"CrucibleCraftGameTests absorbed {name}")
    closed = CLOSED_TESTS.read_text(encoding="utf-8")
    if 'filled == 0' in closed and "plasma/magic fluid was silently accepted" in closed:
        errors.append("closed fluidPipePlasmaMagicAcidGasFailures still rejects fill")
    if "filled > 0" not in closed:
        errors.append("closed plasma/magic GameTest does not allow fill")
    if not (PACK / "structure" / "empty.nbt").is_file():
        errors.append("missing wave structure/empty.nbt")
    if not (PACK / "gametest" / "structure" / "empty.nbt").is_file():
        errors.append("missing wave gametest/structure/empty.nbt")
    this_file = Path(__file__).read_text(encoding="utf-8")
    closed_runtime = "gt6_fluid" + "_pipe_runtime"
    if any(
        line.startswith("from tools import " + closed_runtime)
        for line in this_file.splitlines()
    ):
        errors.append("dangerous-media module imported closed fluid-pipe runtime")
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
