#!/usr/bin/env python3
"""Build and verify the live GT6 Melter family.

The Melter is distinct from the Smelter.  Its source family is large enough
to require an explicit load-scale note, but its exact selected rows remain
under the repository's hard publication ceiling.
"""
from __future__ import annotations

import argparse
import importlib.util
import json
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[4]
TOOLS = ROOT / "tools"
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))
if str(TOOLS) not in sys.path:
    sys.path.insert(1, str(TOOLS))

from tools import census_common as census
from tools.recipe_bulk import compile as compile_mod
from tools.recipe_bulk import source_import
from tools.recipe_bulk.membership import membership_root
from tools.recipe_bulk.waves import recipe_wave

COMMON_PATH = ROOT / "tools" / "waves" / "prep" / "machine_prep_common.py"
COMMON_SPEC = importlib.util.spec_from_file_location(
    "machine_prep_common_for_melter",
    COMMON_PATH,
)
assert COMMON_SPEC is not None and COMMON_SPEC.loader is not None
common = importlib.util.module_from_spec(COMMON_SPEC)
COMMON_SPEC.loader.exec_module(common)

SOURCE_REVISION = common.SOURCE_REVISION
SOURCE_MAP = "gt.recipe.melter"
TARGET_MAP = "cruciblecraft:melter"
HOST = TARGET_MAP
IMPORT_SLUG = "machines/melter"
SOURCE_PACK_ID = IMPORT_SLUG
FAMILY_ID = (
    "portfolio:track_a/cruciblecraft:melter/"
    "gt.recipe.melter#0000"
)
TEMPLATE_KEY = "gt.recipe.melter#0000"
SOURCE_ROWS = 6_756
SELECTED_ROWS = 3_961
OVERFLOW_ROWS = 2_795
LOAD_HARD_CAP = 21_000
LIVE_NEEDLE = "melter"
WAVE = ROOT / "tools" / "waves" / "machines" / "melter"
CAPABILITY_PATH = (
    ROOT / "tools" / "capabilities" / "machines" / "melter" / "capability.json"
)
LIVE_GENERATED = (
    ROOT
    / "src"
    / "recipe_generated"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "recipe"
)
POLICY_PATH = LIVE_GENERATED / "publication_policy" / "melter.json"
PUBLICATION_GROUP = f"{TARGET_MAP}/pilot/melter"
LOCK_NOTE = (
    "live compile for machines/melter; 3961 runtime-registered exact rows "
    "from the 6756-row gt.recipe.melter dump; 2795 overflow rows explicitly "
    "blocked; load publication is UNVERIFIED_SCALE and below the 21000 hard "
    "cap; not player_complete"
)
ART_MANIFEST = "gt6_melter_art_manifest.json"
PREP_WAVE = ROOT / "tools" / "waves" / "prep" / "melter"

# The prep helper writes to its own lane by default.  Reusing its audited
# source-pack algorithm here keeps the landing pack derived from the same
# GT6 dump and operand authorities without duplicating the converter.
common.wave_dir = lambda _slug: WAVE


def _write(path: Path, document: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    census.write_stable(path, document)


def live_family_files() -> list[Path]:
    root = LIVE_GENERATED / LIVE_NEEDLE
    if not root.is_dir():
        return []
    return [
        path
        for path in root.rglob("*.json")
        if path.is_file() and "publication_policy" not in path.as_posix()
    ]


def unique_active_wave() -> str | None:
    if not CAPABILITY_PATH.is_file():
        return IMPORT_SLUG
    capability = census.load_json(CAPABILITY_PATH)
    return IMPORT_SLUG if capability.get("workflow") == "active" else None


def expected_publication_policy() -> dict[str, Any]:
    families = live_family_files()
    if len(families) != 1:
        raise ValueError(
            "need one live compact family to bind publication policy, "
            f"got {len(families)}"
        )
    document = census.load_json(families[0])
    relations = list(document.get("relations") or [])
    family_id = str(document.get("family_id") or "")
    stable_ids = [str(row.get("stable_id") or "") for row in relations]
    if not family_id or not all(stable_ids) or len(relations) != SELECTED_ROWS:
        raise ValueError("live melter family cannot bind publication policy")
    return {
        "cache_ceiling": 0,
        "eager_stable_ids": [],
        "family_count": 1,
        "membership_root_sha256": membership_root([family_id], stable_ids),
        "policy_type": "immediate",
        "publication_group": PUBLICATION_GROUP,
        "relation_count": SELECTED_ROWS,
        "routing_schema_version": "compact-shard-v1",
        "target_map": TARGET_MAP,
        "type": "cruciblecraft:compact_publication_policy",
    }


def write_sidecars() -> None:
    wave = unique_active_wave()
    _write(
        WAVE / "topology.json",
        {
            "append_only": False,
            "complete_family_count": 1,
            "generated_by": "machines/melter implementation",
            "next_unassigned": True,
            "remaining_recipe_gap": OVERFLOW_ROWS,
            "schema_version": 1,
            "source_revision": SOURCE_REVISION,
            "status": "WAVE_READY",
            "unique_active_wave": wave,
            "wave_slug": IMPORT_SLUG,
        },
    )
    _write(
        WAVE / "readiness.json",
        {
            "evidence": {
                "blocked_overflow_rows": OVERFLOW_ROWS,
                "dump_rows": SOURCE_ROWS,
                "hard_load_cap": LOAD_HARD_CAP,
                "hosts": 1,
                "load_scale": "UNVERIFIED_SCALE",
                "owns_families": 1,
                "selected_rows": SELECTED_ROWS,
                "status": "runtime_ready",
            },
            "generated_by": "machines/melter implementation",
            "generated_recipe_count": SELECTED_ROWS,
            "next_unassigned": True,
            "owns_families": 1,
            "production_lock": census.relative(WAVE / "production_lock.json"),
            "schema_version": 1,
            "source_revision": SOURCE_REVISION,
            "status": "MELTER_RUNTIME_READY",
            "unique_active_wave": wave,
            "wave_slug": IMPORT_SLUG,
        },
    )
    _write(
        WAVE / "runtime_notes.json",
        {
            "dump_rows": SOURCE_ROWS,
            "hard_load_cap": LOAD_HARD_CAP,
            "load_scale": "UNVERIFIED_SCALE",
            "note": (
                "The selected family is below the hard cap. Runtime telemetry "
                "over the verified opening scale is recorded as a warning and "
                "does not turn a successfully loaded map into a failed close."
            ),
            "parallel": 1_000,
            "parallel_duration": True,
            "cheap_overclocking": True,
            "publication_capacity": "UNVERIFIED_SCALE",
            "schema_version": 1,
            "selected_rows": SELECTED_ROWS,
            "source_revision": SOURCE_REVISION,
        },
    )


def live_compile() -> dict[str, Any]:
    spec = recipe_wave(IMPORT_SLUG)
    built = compile_mod.compile_wave(IMPORT_SLUG)
    compile_mod.write_tree(
        built["planned"],
        spec.generated_root,
        spec.generated_root,
        path_prefix=spec.path_prefix,
        tree_prefixes=spec.tree_prefixes,
    )
    return built["report"]


def write() -> dict[str, Any]:
    counts = common.write_source_pack(
        slug="melter",
        source_map=SOURCE_MAP,
        target_map=TARGET_MAP,
        host=HOST,
        source_rows=SOURCE_ROWS,
        source_pack_id=SOURCE_PACK_ID,
        family_id=FAMILY_ID,
        template_key=TEMPLATE_KEY,
        import_slug=IMPORT_SLUG,
    )
    if counts != {
        "source_rows": SOURCE_ROWS,
        "selected_rows": SELECTED_ROWS,
        "overflow_rows": OVERFLOW_ROWS,
    }:
        raise ValueError(f"Melter source accounting drifted: {counts}")
    _write(
        WAVE / "d0_obtain_matrix.json",
        census.load_json(PREP_WAVE / "d0_obtain_matrix.json"),
    )
    common.freeze_lock(WAVE, IMPORT_SLUG, LOCK_NOTE)
    write_sidecars()
    isolated = common.isolated_compile(WAVE, LIVE_NEEDLE)
    live = live_compile()
    _write(POLICY_PATH, expected_publication_policy())
    return {"isolated": isolated, "live": live, "source": counts}


def check() -> list[str]:
    errors: list[str] = []
    required = (
        WAVE / "source_pack" / "dump_slice.json",
        WAVE / "source_pack" / "work_set.json",
        WAVE / "source_pack_manifest.json",
        WAVE / "recipe_import.json",
        WAVE / "source.json",
        WAVE / "source_receipt.json",
        WAVE / "source_review.json",
        WAVE / "overflow.json",
        WAVE / "lock_candidate.json",
        WAVE / "production_lock.json",
        WAVE / "d0_obtain_matrix.json",
        WAVE / "topology.json",
        WAVE / "readiness.json",
        WAVE / "runtime_notes.json",
    )
    for path in required:
        if not path.is_file():
            errors.append(f"missing {census.relative(path)}")
    if errors:
        return errors

    try:
        manifest = source_import.load_manifest(WAVE / "source_pack_manifest.json")
        source_import.verify_files(manifest, require_present=True)
    except Exception as error:
        errors.append(f"source pack: {error}")

    work = census.load_json(WAVE / "source_pack" / "work_set.json")
    overflow = census.load_json(WAVE / "overflow.json")
    accounting = work.get("accounting") or {}
    selected = (work.get("families") or [{}])[0].get("relations") or []
    if accounting.get("source_rows") != SOURCE_ROWS:
        errors.append(f"source_rows must be {SOURCE_ROWS}")
    if accounting.get("selected_rows") != SELECTED_ROWS:
        errors.append(f"selected_rows must be {SELECTED_ROWS}")
    if accounting.get("selected_rows") != len(selected):
        errors.append("selected_rows disagrees with family relations")
    if accounting.get("overflow_rows") != OVERFLOW_ROWS:
        errors.append(f"overflow_rows must be {OVERFLOW_ROWS}")
    if len(overflow.get("overflow") or []) != OVERFLOW_ROWS:
        errors.append("overflow row count drifted")
    if "programmed_circuit" in str(overflow):
        errors.append("overflow must not invent programmed_circuit stand-ins")
    if "smelter" in str(overflow).lower():
        errors.append("melter overflow must not use smelter stand-ins")

    try:
        errors.extend(source_import.check_import(WAVE / "recipe_import.json"))
    except Exception as error:
        errors.append(f"import: {error}")
    expected_lock = common.pilot_lock(WAVE, IMPORT_SLUG, LOCK_NOTE)
    actual_lock = census.load_json(WAVE / "production_lock.json")
    if actual_lock != expected_lock:
        errors.append("production_lock drifted from reviewed lock_candidate")
    if actual_lock.get("production_authority") is not True:
        errors.append("production_lock must keep production_authority")
    if "not player_complete" not in str(actual_lock.get("note") or ""):
        errors.append("production_lock must not claim player_complete")
    if actual_lock.get("production", {}).get("relation_count") != SELECTED_ROWS:
        errors.append(f"production relation_count must be {SELECTED_ROWS}")

    errors.extend(
        common.check_art_manifest(
            ART_MANIFEST,
            min_art_imports=20,
            forbidden_art=("multiblock_casing", "heat_exchanger", "smelter"),
        )
    )
    d0 = census.load_json(WAVE / "d0_obtain_matrix.json")
    if d0.get("grid") != ["wUh", "PMP", "BCB"]:
        errors.append("D0 grid drifted from the Melter source grid")
    if {row.get("host") for row in d0.get("hosts") or []} != {22010}:
        errors.append("D0 must contain only Melter host 22010")
    if any(row.get("status") != "source_exact" for row in d0.get("hosts") or []):
        errors.append("Melter D0 host must remain source_exact")
    host = (d0.get("hosts") or [{}])[0]
    if host.get("crucible", {}).get("cc") != "cruciblecraft:foundry/smelting_crucible_steel":
        errors.append("Melter U slot must be a live smelting crucible")
    if host.get("bricks", {}).get("cc") != "minecraft:bricks":
        errors.append("Melter B slot must be vanilla bricks")

    notes = census.load_json(WAVE / "runtime_notes.json")
    if notes.get("parallel") != 1_000 or notes.get("parallel_duration") is not True:
        errors.append("Melter parallel semantics drifted")
    if notes.get("load_scale") != "UNVERIFIED_SCALE":
        errors.append("Melter load scale must remain UNVERIFIED_SCALE")

    topology = census.load_json(WAVE / "topology.json")
    readiness = census.load_json(WAVE / "readiness.json")
    expected_wave = unique_active_wave()
    if topology.get("unique_active_wave") != expected_wave:
        errors.append("topology unique_active_wave drifted")
    if readiness.get("unique_active_wave") != expected_wave:
        errors.append("readiness unique_active_wave drifted")

    try:
        errors.extend(common.isolated_compile(WAVE, LIVE_NEEDLE) and [])
        spec = recipe_wave(IMPORT_SLUG)
        if spec.path_prefix != "melter":
            errors.append(f"derived path_prefix {spec.path_prefix!r} != melter")
        built = compile_mod.compile_wave(IMPORT_SLUG)
        if int(built["report"].get("relation_count") or 0) != SELECTED_ROWS:
            errors.append("live compile relation_count drifted")
        if len(live_family_files()) != 1:
            errors.append("live melter tree must contain one compact family")
        if not POLICY_PATH.is_file():
            errors.append("missing melter publication policy")
        elif census.load_json(POLICY_PATH) != expected_publication_policy():
            errors.append("melter publication policy drifted")
    except Exception as error:
        errors.append(f"live compile: {error}")
    return errors


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Melter live wave")
    modes = parser.add_mutually_exclusive_group(required=True)
    modes.add_argument("--write", action="store_true")
    modes.add_argument("--check", action="store_true")
    args = parser.parse_args(argv)
    try:
        if args.write:
            report = write()
            print(
                "Wrote machines/melter: "
                f"selected={report['source']['selected_rows']} "
                f"overflow={report['source']['overflow_rows']} "
                f"live={report['live'].get('relation_count')}"
            )
            return 0
        errors = check()
    except Exception as error:
        print(str(error), file=sys.stderr)
        return 1
    if errors:
        print("\n".join(errors), file=sys.stderr)
        return 1
    print("machines/melter is current")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
