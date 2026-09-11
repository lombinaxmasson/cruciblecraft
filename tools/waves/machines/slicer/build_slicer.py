#!/usr/bin/env python3
"""Build the live Slicer Source Pack, freeze the lock, and compile 32 rows.

LV/EV hosts are source-exact via technological-parts-foundation. MV/HV/IV
stay blocked. This lane does not invent missing parts and does not claim
``player_complete``.
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
from tools import technological_parts_foundation as parts
from tools.gt6_resolve import resolve
from tools.recipe_bulk import compile as compile_mod
from tools.recipe_bulk import source_import
from tools.recipe_bulk.membership import membership_root
from tools.recipe_bulk.waves import recipe_wave

_COMMON_PATH = (
    ROOT / "tools" / "waves" / "prep" / "machine_prep_common.py"
)
_COMMON_SPEC = importlib.util.spec_from_file_location(
    "machine_prep_common", _COMMON_PATH
)
assert _COMMON_SPEC is not None and _COMMON_SPEC.loader is not None
common = importlib.util.module_from_spec(_COMMON_SPEC)
_COMMON_SPEC.loader.exec_module(common)

SOURCE_REVISION = common.SOURCE_REVISION
SOURCE_MAP = "gt.recipe.slicer"
TARGET_MAP = "cruciblecraft:slicer"
HOST = TARGET_MAP
IMPORT_SLUG = "machines/slicer"
SOURCE_PACK_ID = "machines/slicer"
FAMILY_ID = (
    "portfolio:track_a/cruciblecraft:slicer/"
    "gt.recipe.slicer#0000"
)
TEMPLATE_KEY = "gt.recipe.slicer#0000"
SOURCE_ROWS = 33
SELECTED_ROWS = 32
OVERFLOW_ROWS = 1
LIVE_NEEDLE = "slicer"
CAPABILITY_PATH = (
    ROOT / "tools" / "capabilities" / "machines" / "slicer" / "capability.json"
)

WAVE = ROOT / "tools" / "waves" / "machines" / "slicer"
LIVE_GENERATED = (
    ROOT / "src" / "recipe_generated" / "resources" / "data" / "cruciblecraft"
    / "recipe"
)
POLICY_PATH = LIVE_GENERATED / "publication_policy" / "slicer.json"
PUBLICATION_GROUP = f"{TARGET_MAP}/pilot/slicer"
LOCK_NOTE = (
    "live compile for machines/slicer; 32 selected exact rows; "
    "1 unmapped paper:tiny_plate row explicitly_blocked; not player_complete"
)
ART_MANIFEST = "gt6_slicer_art_manifest.json"
CIRCUITS = {
    1: "cruciblecraft:circuit_basic",
    2: "cruciblecraft:circuit_good",
    3: "cruciblecraft:circuit_advanced",
    4: "cruciblecraft:circuit_elite",
    5: "cruciblecraft:circuit_master",
}
PISTONS = dict(parts.PISTONS)
CONVEYERS = dict(parts.CONVEYERS)
D0_HOSTS = (
    (20381, "steel_galvanized", "SteelGalvanized", 1),
    (20382, "aluminium", "Al", 2),
    (20383, "stainless_steel", "StainlessSteel", 3),
    (20384, "chromium", "Cr", 4),
    (20385, "titanium", "Ti", 5),
)


def live_family_files() -> list[Path]:
    root = LIVE_GENERATED / LIVE_NEEDLE
    if not root.is_dir():
        return []
    return [
        path
        for path in root.rglob("*.json")
        if path.is_file() and "publication_policy" not in path.as_posix()
    ]


def _write(path: Path, document: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    census.write_stable(path, document)


def _ok(gt6: str, cc: str) -> dict[str, str]:
    return {"cc": cc, "gt6": gt6, "status": "ok"}


def _blocked(gt6: str, reason: str) -> dict[str, str]:
    return {"cc": "", "gt6": gt6, "reason": reason, "status": "blocked"}


def _form(token: str, *, prefer: tuple[str, ...] = ()) -> dict[str, str]:
    result = resolve(token)
    status = result.get("status")
    form = result.get("form") or {}
    if result.get("kind") == "prefix_family":
        items = list(form.get("items") or [])
        for name in prefer:
            for row in items:
                if row.get("cc_material") == name and row.get("registered"):
                    return _ok(token, str(row["item"]))
        for row in items:
            if row.get("registered"):
                return _ok(token, str(row["item"]))
        return _blocked(token, "family member form missing")
    item = form.get("item")
    if status == "ok" and item:
        return _ok(token, str(item))
    return _blocked(token, str(status or "unmapped"))


def _circuit(tier: int) -> dict[str, str]:
    return _ok(f"OD_CIRCUITS[{tier}]", CIRCUITS[tier])


def _piston(tier: int) -> dict[str, str]:
    token = f"IL.PISTONS[{tier}]"
    item = PISTONS.get(tier)
    if item:
        return _ok(token, item)
    return _blocked(token, "compact piston module missing for this tier")


def _conveyor(tier: int) -> dict[str, str]:
    token = f"IL.CONVEYERS[{tier}]"
    item = CONVEYERS.get(tier)
    if item:
        return _ok(token, item)
    return _blocked(
        token,
        "compact conveyor module missing; cover is not this slot",
    )


def _host_status(slots: dict[str, dict[str, str]]) -> str:
    if all(slot.get("status") == "ok" for slot in slots.values()):
        return "source_exact"
    return "explicitly_blocked"


def unique_active_wave() -> str | None:
    if not CAPABILITY_PATH.is_file():
        return IMPORT_SLUG
    document = census.load_json(CAPABILITY_PATH)
    if document.get("workflow") == "active":
        return IMPORT_SLUG
    return None


def d0_matrix() -> dict[str, Any]:
    hosts: list[dict[str, Any]] = []
    for host, cc, mt, tier in D0_HOSTS:
        slots = {
            "casing": _form(f"OP.casingMachine(MT.{mt})"),
            "circuit": _circuit(tier),
            "conveyor": _conveyor(tier),
            "piston": _piston(tier),
            "rod": _form(f"OP.stick(MT.{mt})"),
        }
        material = ""
        casing_cc = slots["casing"].get("cc") or ""
        if "/" in casing_cc:
            material = casing_cc.split(":", 1)[-1].split("/", 1)[0]
        hosts.append(
            {
                "host": host,
                "material": material or cc,
                **slots,
                "status": _host_status(slots),
            }
        )
    return {
        "capability_slug": IMPORT_SLUG,
        "grid": ["PRw", "YMC"],
        "hosts": hosts,
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "status": "runtime_ready",
    }


def write_source_pack() -> dict[str, int]:
    raw = common.dump_path(SOURCE_MAP)
    dump = json.loads(raw.read_text(encoding="utf-8"))
    recipes = list(dump.get("recipes") or [])
    if len(recipes) != SOURCE_ROWS:
        raise ValueError(
            f"{SOURCE_MAP} dump must contain {SOURCE_ROWS} rows, "
            f"got {len(recipes)}"
        )
    selected, overflow = common.audit_rows(
        dump,
        host=HOST,
        target_map=TARGET_MAP,
        source_map=SOURCE_MAP,
        family_id=FAMILY_ID,
        template_key=TEMPLATE_KEY,
    )
    dump_slice = {
        "recipes": [
            common.source_row(recipe, index, common.row_hash(recipe), TEMPLATE_KEY)
            for index, recipe in enumerate(recipes)
        ],
        "source_map": SOURCE_MAP,
        "source_revision": SOURCE_REVISION,
    }
    work_set = common.build_work_set(
        selected,
        overflow,
        len(recipes),
        family_id=FAMILY_ID,
        template_key=TEMPLATE_KEY,
        host=HOST,
    )
    _write(WAVE / "source_pack" / "dump_slice.json", dump_slice)
    _write(WAVE / "source_pack" / "work_set.json", work_set)
    _write(
        WAVE / "overflow.json",
        {
            "blocked_rows": len(overflow),
            "overflow": overflow,
            "schema_version": 1,
            "source_map": SOURCE_MAP,
            "source_revision": SOURCE_REVISION,
            "status": "EXPLICITLY_BLOCKED_OVERFLOW",
        },
    )
    _write(WAVE / "source_pack_manifest.json", common.build_manifest(WAVE, SOURCE_PACK_ID))
    _write(
        WAVE / "recipe_import.json",
        common.build_import_spec(
            WAVE,
            host=HOST,
            import_slug=IMPORT_SLUG,
            source_map=SOURCE_MAP,
            target_map=TARGET_MAP,
        ),
    )
    if not selected:
        raise ValueError("slicer selected work set must not be empty")
    source_import.write_import(WAVE / "recipe_import.json")
    dropped = common._drop_consume_collisions(
        WAVE,
        host=HOST,
        family_id=FAMILY_ID,
        template_key=TEMPLATE_KEY,
        import_slug=IMPORT_SLUG,
    )
    if dropped:
        source_import.write_import(WAVE / "recipe_import.json")
    accounting = census.load_json(WAVE / "source_pack" / "work_set.json").get(
        "accounting"
    ) or {}
    return {
        "source_rows": len(recipes),
        "selected_rows": int(accounting.get("selected_rows") or 0),
        "overflow_rows": int(accounting.get("overflow_rows") or 0),
    }


def write_wave_sidecars() -> None:
    wave = unique_active_wave()
    _write(
        WAVE / "topology.json",
        {
            "append_only": False,
            "complete_family_count": 1,
            "generated_by": "machines/slicer implementation",
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
                "blocked_host_obtain": 0,
                "blocked_tiny_plate_rows": OVERFLOW_ROWS,
                "dump_rows": SOURCE_ROWS,
                "hosts": 5,
                "owns_families": 1,
                "selected_rows": SELECTED_ROWS,
                "status": "runtime_ready",
            },
            "generated_by": "machines/slicer implementation",
            "generated_recipe_count": SELECTED_ROWS,
            "next_unassigned": True,
            "owns_families": 1,
            "production_lock": census.relative(WAVE / "production_lock.json"),
            "schema_version": 1,
            "source_revision": SOURCE_REVISION,
            "status": "SLICER_RUNTIME_READY",
            "unique_active_wave": wave,
            "wave_slug": IMPORT_SLUG,
        },
    )


def expected_publication_policy() -> dict[str, Any]:
    families = live_family_files()
    if len(families) != 1:
        raise ValueError(
            f"need one live compact family to bind publication policy, "
            f"got {len(families)}"
        )
    document = census.load_json(families[0])
    relations = list(document.get("relations") or [])
    family_id = str(document.get("family_id") or "")
    stable_ids = [str(row.get("stable_id") or "") for row in relations]
    if not family_id or not all(stable_ids) or len(relations) != SELECTED_ROWS:
        raise ValueError("live compact family cannot bind publication policy")
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


def write_publication_policy() -> None:
    _write(POLICY_PATH, expected_publication_policy())


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
    counts = write_source_pack()
    if counts["selected_rows"] != SELECTED_ROWS:
        raise ValueError(
            f"selected rows {counts['selected_rows']} != {SELECTED_ROWS}"
        )
    if counts["overflow_rows"] != OVERFLOW_ROWS:
        raise ValueError(
            f"overflow rows {counts['overflow_rows']} != {OVERFLOW_ROWS}"
        )
    common.freeze_lock(WAVE, IMPORT_SLUG, LOCK_NOTE)
    _write(WAVE / "d0_obtain_matrix.json", d0_matrix())
    write_wave_sidecars()
    isolated = common.isolated_compile(WAVE, LIVE_NEEDLE)
    live = live_compile()
    write_publication_policy()
    return {
        "isolated": isolated,
        "live": live,
        "source": counts,
    }


def check() -> list[str]:
    errors: list[str] = []
    required = (
        WAVE / "source_pack" / "dump_slice.json",
        WAVE / "source_pack" / "work_set.json",
        WAVE / "source_pack_manifest.json",
        WAVE / "recipe_import.json",
        WAVE / "overflow.json",
        WAVE / "lock_candidate.json",
        WAVE / "production_lock.json",
        WAVE / "d0_obtain_matrix.json",
        WAVE / "topology.json",
        WAVE / "readiness.json",
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
        errors.append(f"work_set accounting source_rows must be {SOURCE_ROWS}")
    if accounting.get("selected_rows") != SELECTED_ROWS:
        errors.append(f"work_set accounting selected_rows must be {SELECTED_ROWS}")
    if accounting.get("selected_rows") != len(selected):
        errors.append("work_set accounting selected_rows disagrees with family relations")
    if accounting.get("overflow_rows") != OVERFLOW_ROWS:
        errors.append(f"work_set accounting overflow_rows must be {OVERFLOW_ROWS}")
    if accounting.get("overflow_rows") != len(overflow.get("overflow") or []):
        errors.append("work_set accounting overflow_rows disagrees with overflow")
    blob = str(overflow)
    if "programmed_circuit" in blob:
        errors.append("overflow must not invent programmed_circuit stand-ins")
    if "compact_electric_conveyor" in blob:
        errors.append("overflow must not invent conveyor-cover stand-ins")
    plate = [
        row
        for row in overflow.get("overflow") or []
        if "tiny_plate" in str(row) or "paper" in str(row).lower()
    ]
    if len(plate) != OVERFLOW_ROWS:
        errors.append("overflow must keep the unmapped paper:tiny_plate row")

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
        errors.append(f"production_lock relation_count must be {SELECTED_ROWS}")

    errors.extend(
        common.check_art_manifest(
            ART_MANIFEST,
            min_art_imports=20,
            forbidden_art=(
                "multiblock_casing",
                "heat_exchanger",
                "smelter",
                "conveyor_cover",
            ),
        )
    )

    d0 = census.load_json(WAVE / "d0_obtain_matrix.json")
    if d0.get("grid") != ["PRw", "YMC"]:
        errors.append("d0 grid must match GT6 PRw / YMC")
    statuses = {row["host"]: row["status"] for row in d0.get("hosts") or []}
    if statuses != {
        20381: "source_exact",
        20382: "source_exact",
        20383: "source_exact",
        20384: "source_exact",
        20385: "source_exact",
    }:
        errors.append("slicer D0 host statuses drifted")
    ev = next((row for row in d0.get("hosts") or [] if row.get("host") == 20384), None)
    lv = next((row for row in d0.get("hosts") or [] if row.get("host") == 20381), None)
    if lv is None or ev is None:
        errors.append("slicer D0 is missing LV or EV host")
    elif lv.get("piston", {}).get("status") != "ok" or lv.get("conveyor", {}).get("status") != "ok":
        errors.append("20381 LV piston and conveyor must be source-exact")
    elif ev.get("piston", {}).get("status") != "ok" or ev.get("conveyor", {}).get("status") != "ok":
        errors.append("20384 EV piston and conveyor must be source-exact")
    if "programmed_circuit" in str(d0) or parts.has_split_module_standin(str(d0)):
        errors.append("d0 must not invent programmed_circuit or split conveyor modules")

    topology = census.load_json(WAVE / "topology.json")
    readiness = census.load_json(WAVE / "readiness.json")
    expected_wave = unique_active_wave()
    if topology.get("unique_active_wave") != expected_wave:
        errors.append("topology unique_active_wave drifted from capability workflow")
    if readiness.get("unique_active_wave") != expected_wave:
        errors.append("readiness unique_active_wave drifted from capability workflow")

    if errors:
        return errors
    try:
        common.isolated_compile(WAVE, LIVE_NEEDLE)
    except Exception as error:
        errors.append(f"isolated compile: {error}")
    try:
        spec = recipe_wave(IMPORT_SLUG)
        if spec.path_prefix != "slicer":
            errors.append(f"derived path_prefix {spec.path_prefix!r} != slicer")
        built = compile_mod.compile_wave(IMPORT_SLUG)
        relation_count = int(built["report"].get("relation_count") or 0)
        if relation_count != SELECTED_ROWS:
            errors.append(
                f"live compile relation_count {relation_count} != {SELECTED_ROWS}"
            )
        generated = live_family_files()
        if len(generated) != 1:
            errors.append(
                f"live slicer tree must contain one compact family, "
                f"got {len(generated)}"
            )
        from tools.recipe_bulk.matrix import authored_relation_count

        for path in generated:
            document = census.load_json(path)
            if authored_relation_count(document) != SELECTED_ROWS:
                errors.append(
                    f"{census.relative(path)} authored relations != {SELECTED_ROWS}"
                )
        try:
            expected = expected_publication_policy()
        except Exception as error:
            errors.append(f"publication policy: {error}")
        else:
            if not POLICY_PATH.is_file():
                errors.append("missing compact publication policy slicer.json")
            elif census.load_json(POLICY_PATH) != expected:
                errors.append("publication policy drifted from live compact family")
    except Exception as error:
        errors.append(f"live compile: {error}")
    return errors


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Slicer live wave")
    modes = parser.add_mutually_exclusive_group(required=True)
    modes.add_argument("--write", action="store_true")
    modes.add_argument("--check", action="store_true")
    args = parser.parse_args(argv)
    if args.write:
        report = write()
        print(
            "Wrote machines/slicer: "
            f"selected={report['source']['selected_rows']} "
            f"overflow={report['source']['overflow_rows']} "
            f"live={report['live'].get('relation_count')}"
        )
        return 0
    errors = check()
    if errors:
        print("\n".join(errors), file=sys.stderr)
        return 1
    print("machines/slicer is current")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
