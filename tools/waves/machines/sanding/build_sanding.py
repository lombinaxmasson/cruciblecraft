#!/usr/bin/env python3
"""Build and verify the live GT6 Sanding Machine family.

All source rows now have exact runtime operands and stay under the hard
publication ceiling. This lane does not claim player_complete.
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
from tools.recipe_bulk.matrix import authored_relations
from tools.recipe_bulk.membership import membership_root
from tools.recipe_bulk.transport import reassemble_documents
from tools.recipe_bulk.waves import recipe_wave

COMMON_PATH = ROOT / "tools" / "waves" / "prep" / "machine_prep_common.py"
COMMON_SPEC = importlib.util.spec_from_file_location(
    "machine_prep_common_for_sanding",
    COMMON_PATH,
)
assert COMMON_SPEC is not None and COMMON_SPEC.loader is not None
common = importlib.util.module_from_spec(COMMON_SPEC)
COMMON_SPEC.loader.exec_module(common)

SOURCE_REVISION = common.SOURCE_REVISION
SOURCE_MAP = "gt.recipe.sharpener"
TARGET_MAP = "cruciblecraft:sanding"
HOST = TARGET_MAP
IMPORT_SLUG = "machines/sanding"
SOURCE_PACK_ID = IMPORT_SLUG
FAMILY_ID = (
    "portfolio:track_a/cruciblecraft:sanding/"
    "gt.recipe.sharpener#0000"
)
TEMPLATE_KEY = "gt.recipe.sharpener#0000"
SOURCE_ROWS = 7_637
SELECTED_ROWS = 7_637
OVERFLOW_ROWS = 0
LOAD_HARD_CAP = 21_000
LIVE_NEEDLE = "sanding"
WAVE = ROOT / "tools" / "waves" / "machines" / "sanding"
CAPABILITY_PATH = (
    ROOT / "tools" / "capabilities" / "machines" / "sanding" / "capability.json"
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
POLICY_PATH = LIVE_GENERATED / "publication_policy" / "sanding.json"
PUBLICATION_GROUP = f"{TARGET_MAP}/pilot/sanding"
LOCK_NOTE = (
    "live compile for machines/sanding; 7637 runtime-registered exact rows "
    "from the 7637-row gt.recipe.sharpener dump; 0 overflow rows remain; "
    "load publication is UNVERIFIED_SCALE and below the 21000 hard "
    "cap; not player_complete"
)
ART_MANIFEST = "gt6_sanding_art_manifest.json"
PREP_WAVE = ROOT / "tools" / "waves" / "prep" / "sanding"

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
    if not families:
        raise ValueError("need live compact families to bind publication policy")
    documents = [census.load_json(path) for path in sorted(families)]
    family_ids = {str(document.get("family_id") or "") for document in documents}
    if family_ids != {TEMPLATE_KEY}:
        raise ValueError(f"live sanding semantic family drifted: {family_ids}")
    assembled = reassemble_documents(documents)
    relations = authored_relations(assembled)
    family_id = str(assembled.get("family_id") or "")
    stable_ids = [str(row.get("stable_id") or "") for row in relations]
    if family_id != TEMPLATE_KEY or not all(stable_ids) or len(relations) != SELECTED_ROWS:
        raise ValueError("live sanding family cannot bind publication policy")
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


def expected_rule_ir() -> dict[str, Any]:
    from tools.recipe_bulk import rule_ir as rule_ir_mod

    source = census.load_json(WAVE / "source.json")
    return rule_ir_mod.build_sanding_rule_ir(
        list(source.get("relations") or []),
        family_id=TEMPLATE_KEY,
        source_map=SOURCE_MAP,
        target_map=TARGET_MAP,
        source_revision=SOURCE_REVISION,
    )


def write_sidecars() -> None:
    wave = unique_active_wave()
    _write(
        WAVE / "topology.json",
        {
            "append_only": False,
            "complete_family_count": 1,
            "generated_by": "machines/sanding implementation",
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
                "hosts": 4,
                "load_scale": "UNVERIFIED_SCALE",
                "owns_families": 1,
                "selected_rows": SELECTED_ROWS,
                "status": "runtime_ready",
            },
            "generated_by": "machines/sanding implementation",
            "generated_recipe_count": SELECTED_ROWS,
            "next_unassigned": True,
            "owns_families": 1,
            "production_lock": census.relative(WAVE / "production_lock.json"),
            "schema_version": 1,
            "source_revision": SOURCE_REVISION,
            "status": "SANDING_RUNTIME_READY",
            "unique_active_wave": wave,
            "wave_slug": IMPORT_SLUG,
        },
    )
    _write(
        WAVE / "runtime_notes.json",
        {
            "dump_rows": SOURCE_ROWS,
            "energy_accepted_sides": "UP",
            "eut": 16,
            "gt6_hosts": [20511, 20512, 20513, 20514],
            "hard_load_cap": LOAD_HARD_CAP,
            "load_scale": "UNVERIFIED_SCALE",
            "note": (
                "GT6 NBT_ENERGY_ACCEPTED_SIDES=SBIT_U. Grindstone 32703 stays "
                "out of this child. Tool-head remap, material forms, and the "
                "two existing Energium MTE identities are source-backed. The "
                "selected family is below the hard cap; "
                "load telemetry is UNVERIFIED_SCALE."
            ),
            "out_of_scope": ["grindstone_32703"],
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
        slug="sanding",
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
        raise ValueError(f"Sanding source accounting drifted: {counts}")
    _write(
        WAVE / "d0_obtain_matrix.json",
        census.load_json(PREP_WAVE / "d0_obtain_matrix.json"),
    )
    common.freeze_lock(WAVE, IMPORT_SLUG, LOCK_NOTE)
    write_sidecars()
    isolated = common.isolated_compile(WAVE, LIVE_NEEDLE)
    live = live_compile()
    _write(POLICY_PATH, expected_publication_policy())
    _write(WAVE / "rule_ir.json", expected_rule_ir())
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
        WAVE / "rule_ir.json",
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
            forbidden_art=("multiblock_casing", "heat_exchanger", "conveyor_cover"),
        )
    )
    d0 = census.load_json(WAVE / "d0_obtain_matrix.json")
    if d0.get("grid") != ["SGS", "XXX", "wMh"]:
        errors.append("D0 grid drifted from the Sanding source grid")
    hosts = {row.get("host") for row in d0.get("hosts") or []}
    if hosts != {20511, 20512, 20513, 20514}:
        errors.append("D0 must contain the four Kinetic_T hosts")
    if any(row.get("status") != "source_exact" for row in d0.get("hosts") or []):
        errors.append("Sanding D0 hosts must remain source_exact")
    for row in d0.get("hosts") or []:
        if row.get("sandstone", {}).get("cc") != "minecraft:sandstone":
            errors.append("Sanding X slot must be vanilla sandstone")

    notes = census.load_json(WAVE / "runtime_notes.json")
    if notes.get("energy_accepted_sides") != "UP":
        errors.append("Sanding energy side must stay UP")
    if "grindstone_32703" not in (notes.get("out_of_scope") or []):
        errors.append("Sanding must keep grindstone 32703 out of scope")
    if notes.get("load_scale") != "UNVERIFIED_SCALE":
        errors.append("Sanding load scale must remain UNVERIFIED_SCALE")

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
        if spec.path_prefix != "sanding":
            errors.append(f"derived path_prefix {spec.path_prefix!r} != sanding")
        if spec.compile_authority != "rule_ir_v1":
            errors.append("sanding compile_authority must be rule_ir_v1")
        if census.load_json(WAVE / "rule_ir.json") != expected_rule_ir():
            errors.append("sanding rule_ir drifted from dump-proven classifier")
        built = compile_mod.compile_wave(IMPORT_SLUG)
        if int(built["report"].get("relation_count") or 0) != SELECTED_ROWS:
            errors.append("live compile relation_count drifted")
        if int(built["report"].get("semantic_family_count") or 0) != 1:
            errors.append("live compile semantic_family_count drifted")
        if not live_family_files():
            errors.append("live sanding tree is missing compact family fragments")
        else:
            assembled = reassemble_documents(
                [census.load_json(path) for path in live_family_files()]
            )
            if str(assembled.get("family_id")) != TEMPLATE_KEY:
                errors.append("live sanding semantic family_id drifted")
            if len(authored_relations(assembled)) != SELECTED_ROWS:
                errors.append("live sanding reassembled relation_count drifted")
        if not POLICY_PATH.is_file():
            errors.append("missing sanding publication policy")
        elif census.load_json(POLICY_PATH) != expected_publication_policy():
            errors.append("sanding publication policy drifted")
    except Exception as error:
        errors.append(f"live compile: {error}")
    return errors


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Sanding live wave")
    modes = parser.add_mutually_exclusive_group(required=True)
    modes.add_argument("--write", action="store_true")
    modes.add_argument("--check", action="store_true")
    args = parser.parse_args(argv)
    try:
        if args.write:
            report = write()
            print(
                "Wrote machines/sanding: "
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
    print("machines/sanding is current")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
