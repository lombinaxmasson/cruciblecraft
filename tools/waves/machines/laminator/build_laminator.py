#!/usr/bin/env python3
"""Build the live Laminator Source Pack and compile its 486 exact rows."""
from __future__ import annotations

import argparse
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
from tools.gt6_resolve import resolve
from tools.recipe_bulk import compile as compile_mod
from tools.recipe_bulk import source_import
from tools.recipe_bulk.membership import membership_root
from tools.recipe_bulk.waves import recipe_wave
from tools.waves.prep import machine_prep_common as common

SOURCE_REVISION = common.SOURCE_REVISION
SOURCE_MAP = "gt.recipe.laminator"
TARGET_MAP = "cruciblecraft:laminator"
HOST = TARGET_MAP
IMPORT_SLUG = "machines/laminator"
SOURCE_PACK_ID = IMPORT_SLUG
FAMILY_ID = (
    "portfolio:track_a/cruciblecraft:laminator/"
    "gt.recipe.laminator#0000"
)
TEMPLATE_KEY = "gt.recipe.laminator#0000"
SOURCE_ROWS = 498
PREP_SELECTED_ROWS = 444
SELECTED_ROWS = 486
PREP_OVERFLOW_ROWS = 6
OVERFLOW_ROWS = 12
LIVE_NEEDLE = "laminator"
CAPABILITY_PATH = (
    ROOT / "tools" / "capabilities" / "machines" / "laminator" / "capability.json"
)
WAVE = ROOT / "tools" / "waves" / "machines" / "laminator"
LIVE_GENERATED = (
    ROOT / "src" / "recipe_generated" / "resources" / "data"
    / "cruciblecraft" / "recipe"
)
POLICY_PATH = LIVE_GENERATED / "publication_policy" / "laminator.json"
PUBLICATION_GROUP = f"{TARGET_MAP}/pilot/laminator"
LOCK_NOTE = (
    "live compile for machines/laminator; 486 runtime-registered exact rows; "
    "6 gt_block log rows and 6 unmapped MTE rows explicitly_blocked; "
    "not player_complete"
)
ART_MANIFEST = "gt6_laminator_art_manifest.json"
RUNTIME_BLOCKED_ROWS = {75, 161, 242, 326, 366, 432}
RUNTIME_BLOCKED_OPERANDS = {
    "cruciblecraft:gt_object/gt_block_log_c_m1",
    "cruciblecraft:gt_object/gt_block_log_c_m2",
    "cruciblecraft:gt_object/gt_block_log_c_m3",
}
D0_HOSTS = (
    (20391, "ANY.Steel", ("steel",)),
    (20392, "Invar", ()),
    (20393, "Ti", ()),
    (20394, "TungstenCarbide", ()),
)


def live_family_files() -> list[Path]:
    root = LIVE_GENERATED / LIVE_NEEDLE
    if not root.is_dir():
        return []
    return [
        path for path in root.rglob("*.json")
        if path.is_file() and "publication_policy" not in path.as_posix()
    ]


def _write(path: Path, document: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    census.write_stable(path, document)


def _form(token: str, *, prefer: tuple[str, ...] = ()) -> dict[str, str]:
    result = resolve(token)
    form = result.get("form") or {}
    if result.get("kind") == "prefix_family":
        items = list(form.get("items") or [])
        for name in prefer:
            for row in items:
                if row.get("cc_material") == name and row.get("registered"):
                    return {"cc": str(row["item"]), "gt6": token, "status": "ok"}
        for row in items:
            if row.get("registered"):
                return {"cc": str(row["item"]), "gt6": token, "status": "ok"}
        return {
            "cc": "",
            "gt6": token,
            "reason": "family member form missing",
            "status": "blocked",
        }
    item = form.get("item")
    if result.get("status") == "ok" and item:
        return {"cc": str(item), "gt6": token, "status": "ok"}
    return {
        "cc": "",
        "gt6": token,
        "reason": str(result.get("status") or "unmapped"),
        "status": "blocked",
    }


def _host_status(slots: dict[str, dict[str, str]]) -> str:
    return (
        "source_exact"
        if all(slot.get("status") == "ok" for slot in slots.values())
        else "explicitly_blocked"
    )


def unique_active_wave() -> str | None:
    if not CAPABILITY_PATH.is_file():
        return IMPORT_SLUG
    document = census.load_json(CAPABILITY_PATH)
    if document.get("workflow") == "active":
        return IMPORT_SLUG
    return None


def d0_matrix() -> dict[str, Any]:
    hosts: list[dict[str, Any]] = []
    for host, material, prefer in D0_HOSTS:
        token = material if material.startswith("ANY.") else f"MT.{material}"
        slots = {
            "casing": _form(f"OP.casingMachine({token})", prefer=prefer),
            "gear": _form(f"OP.gearGtSmall({token})", prefer=prefer),
            "rod": _form(f"OP.stick({token})", prefer=prefer),
            "copper_double_plate": _form("OP.plateDouble(ANY.Cu)"),
        }
        hosts.append(
            {
                "host": host,
                "material": (
                    slots["casing"]["cc"].split(":", 1)[-1]
                    .split("/", 1)[0]
                    if slots["casing"]["cc"]
                    else material,
                ),
                **slots,
                "status": _host_status(slots),
            }
        )
    return {
        "capability_slug": IMPORT_SLUG,
        "grid": ["SwS", "GMG", "SCS"],
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
            f"{SOURCE_MAP} dump must contain {SOURCE_ROWS} rows, got {len(recipes)}"
        )
    selected, overflow = common.audit_rows(
        dump,
        host=HOST,
        target_map=TARGET_MAP,
        source_map=SOURCE_MAP,
        family_id=FAMILY_ID,
        template_key=TEMPLATE_KEY,
    )
    runtime_blocked: list[dict[str, Any]] = []
    retained: list[dict[str, Any]] = []
    for row in selected:
        if row["source_recipe_index"] not in RUNTIME_BLOCKED_ROWS:
            retained.append(row)
            continue
        recipe = recipes[row["source_recipe_index"]]
        runtime_blocked.append(
            {
                "input": (recipe.get("inputs") or [{}])[0].get("displayName"),
                "output": (recipe.get("outputs") or [{}])[0].get("displayName"),
                "reasons": [
                    "blocked_unregistered runtime operand "
                    + ", ".join(sorted(RUNTIME_BLOCKED_OPERANDS))
                ],
                "source_recipe_index": row["source_recipe_index"],
                "source_row_sha256": row["source_row_sha256"],
                "status": "blocked",
            }
        )
    selected = retained
    for shadow_order, row in enumerate(selected):
        row["shadow_order"] = shadow_order
    overflow.extend(runtime_blocked)
    _write(
        WAVE / "source_pack" / "dump_slice.json",
        {
            "recipes": [
                common.source_row(recipe, index, common.row_hash(recipe), TEMPLATE_KEY)
                for index, recipe in enumerate(recipes)
            ],
            "source_map": SOURCE_MAP,
            "source_revision": SOURCE_REVISION,
        },
    )
    _write(
        WAVE / "source_pack" / "work_set.json",
        common.build_work_set(
            selected,
            overflow,
            len(recipes),
            family_id=FAMILY_ID,
            template_key=TEMPLATE_KEY,
            host=HOST,
        ),
    )
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
    _write(
        WAVE / "source_pack_manifest.json",
        common.build_manifest(WAVE, SOURCE_PACK_ID),
    )
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
        raise ValueError("laminator selected work set must not be empty")
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
    accounting = census.load_json(
        WAVE / "source_pack" / "work_set.json"
    ).get("accounting") or {}
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
            "generated_by": "machines/laminator implementation",
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
                "blocked_mte_rows": PREP_OVERFLOW_ROWS,
                "blocked_runtime_operand_rows": len(RUNTIME_BLOCKED_ROWS),
                "blocked_consume_collision_rows": 0,
                "dump_rows": SOURCE_ROWS,
                "hosts": 4,
                "owns_families": 1,
                "prep_selected_rows": PREP_SELECTED_ROWS,
                "selected_rows": SELECTED_ROWS,
                "status": "runtime_ready",
            },
            "generated_by": "machines/laminator implementation",
            "generated_recipe_count": SELECTED_ROWS,
            "next_unassigned": True,
            "owns_families": 1,
            "production_lock": census.relative(WAVE / "production_lock.json"),
            "schema_version": 1,
            "source_revision": SOURCE_REVISION,
            "status": "LAMINATOR_RUNTIME_READY",
            "unique_active_wave": wave,
            "wave_slug": IMPORT_SLUG,
        },
    )


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
    _write(POLICY_PATH, expected_publication_policy())
    return {"isolated": isolated, "live": live, "source": counts}


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
        errors.append(f"source_rows must be {SOURCE_ROWS}")
    if accounting.get("selected_rows") != SELECTED_ROWS:
        errors.append(f"selected_rows must be {SELECTED_ROWS}")
    if accounting.get("selected_rows") != len(selected):
        errors.append("selected_rows disagrees with family relations")
    if accounting.get("overflow_rows") != OVERFLOW_ROWS:
        errors.append(f"overflow_rows must be {OVERFLOW_ROWS}")
    if accounting.get("overflow_rows") != len(overflow.get("overflow") or []):
        errors.append("overflow_rows disagrees with overflow")
    if "programmed_circuit" in str(overflow):
        errors.append("overflow must not invent programmed_circuit")
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
    if d0.get("grid") != ["SwS", "GMG", "SCS"]:
        errors.append("d0 grid drifted")
    statuses = {row["host"]: row["status"] for row in d0.get("hosts") or []}
    if statuses != {
        20391: "source_exact",
        20392: "source_exact",
        20393: "source_exact",
        20394: "source_exact",
    }:
        errors.append("all four D0 hosts must stay source_exact")
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
        built = compile_mod.compile_wave(IMPORT_SLUG)
        if int(built["report"].get("relation_count") or 0) != SELECTED_ROWS:
            errors.append("live compile relation count drifted")
        if len(live_family_files()) != 1:
            errors.append("live laminator tree must contain one compact family")
        if not POLICY_PATH.is_file():
            errors.append("missing laminator publication policy")
        elif census.load_json(POLICY_PATH) != expected_publication_policy():
            errors.append("publication policy drifted from live family")
    except Exception as error:
        errors.append(f"live compile: {error}")
    return errors


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    mode = parser.add_mutually_exclusive_group()
    mode.add_argument("--write", action="store_true")
    mode.add_argument("--check", action="store_true")
    args = parser.parse_args(argv)
    try:
        if args.check:
            errors = check()
            if errors:
                print("\n".join(errors), file=sys.stderr)
                return 1
            print("machines/laminator is current")
            return 0
        result = write()
        print(
            "Wrote machines/laminator: "
            f"selected={result['source']['selected_rows']} "
            f"overflow={result['source']['overflow_rows']} "
            f"live={result['live'].get('relation_count', 0)}"
        )
        return 0
    except Exception as error:
        print(str(error), file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
