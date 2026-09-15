#!/usr/bin/env python3
"""Build the live Cluster Mill Source Pack, freeze the lock, and compile 307 rows.

Foil is the real GT6 prefix gated by this card. The production lock does not
claim ``player_complete``; that lives on the capability.
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
SOURCE_MAP = "gt.recipe.clustermill"
TARGET_MAP = "cruciblecraft:clustermill"
HOST = TARGET_MAP
IMPORT_SLUG = "machines/cluster-mill"
SOURCE_PACK_ID = "machines/cluster-mill"
FAMILY_ID = (
    "portfolio:track_a/cruciblecraft:clustermill/"
    "gt.recipe.clustermill#0000"
)
TEMPLATE_KEY = "gt.recipe.clustermill#0000"
SOURCE_ROWS = 307
SELECTED_ROWS = 307
OVERFLOW_ROWS = 0
LIVE_NEEDLE = "clustermill"

WAVE = ROOT / "tools" / "waves" / "machines" / "cluster-mill"
LIVE_GENERATED = (
    ROOT / "src" / "recipe_generated" / "resources" / "data" / "cruciblecraft"
    / "recipe"
)
POLICY_PATH = LIVE_GENERATED / "publication_policy" / "cluster_mill.json"
PUBLICATION_GROUP = f"{TARGET_MAP}/pilot/cluster_mill"
LOCK_NOTE = (
    "live compile for machines/cluster-mill; 307 selected exact rows; "
    "foil prefix gated as the real GT6 form; not player_complete"
)
ART_MANIFEST = "gt6_cluster_mill_art_manifest.json"
D0_HOSTS = (
    (20141, "Bronze", ()),
    (20142, "ANY.Steel", ("steel",)),
    (20143, "Ti", ()),
    (20144, "TungstenSteel", ()),
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


def _host_status(slots: dict[str, dict[str, str]]) -> str:
    if all(slot.get("status") == "ok" for slot in slots.values()):
        return "source_exact"
    return "explicitly_blocked"


def d0_matrix() -> dict[str, Any]:
    hosts: list[dict[str, Any]] = []
    for host, mt, prefer in D0_HOSTS:
        token_mat = mt if mt.startswith("ANY.") else f"MT.{mt}"
        slots = {
            "casing": _form(
                f"OP.casingMachineQuadruple({token_mat})", prefer=prefer
            ),
            "gear": _form(f"OP.gearGt({token_mat})", prefer=prefer),
            "small_gear": _form(f"OP.gearGtSmall({token_mat})", prefer=prefer),
        }
        material = ""
        casing_cc = slots["casing"].get("cc") or ""
        if "/" in casing_cc:
            material = casing_cc.split(":", 1)[-1].split("/", 1)[0]
        hosts.append(
            {
                "host": host,
                "material": material,
                **slots,
                "status": _host_status(slots),
            }
        )
    return {
        "capability_slug": IMPORT_SLUG,
        "grid": ["SSS", "wGh", "SMS"],
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
        raise ValueError("cluster mill selected work set must not be empty")
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
    _write(
        WAVE / "topology.json",
        {
            "append_only": False,
            "complete_family_count": 1,
            "generated_by": "machines/cluster-mill implementation",
            "next_unassigned": True,
            "remaining_recipe_gap": OVERFLOW_ROWS,
            "schema_version": 1,
            "source_revision": SOURCE_REVISION,
            "status": "WAVE_READY",
            "unique_active_wave": None,
            "wave_slug": IMPORT_SLUG,
        },
    )
    _write(
        WAVE / "readiness.json",
        {
            "evidence": {
                "dump_rows": SOURCE_ROWS,
                "hosts": 4,
                "owns_families": 1,
                "overflow_rows": OVERFLOW_ROWS,
                "selected_rows": SELECTED_ROWS,
                "status": "runtime_ready",
            },
            "generated_by": "machines/cluster-mill implementation",
            "generated_recipe_count": SELECTED_ROWS,
            "next_unassigned": True,
            "owns_families": 1,
            "production_lock": census.relative(WAVE / "production_lock.json"),
            "schema_version": 1,
            "source_revision": SOURCE_REVISION,
            "status": "CLUSTER_MILL_PLAYER_COMPLETE",
            "unique_active_wave": None,
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
        WAVE / "required_forms.json",
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
    if overflow.get("overflow"):
        errors.append("overflow must be empty after foil forms are gated")

    required_forms = census.load_json(WAVE / "required_forms.json")
    gate = census.load_json(
        ROOT
        / "src"
        / "main"
        / "resources"
        / "data"
        / "cruciblecraft"
        / "material_registration_gate.json"
    )
    overlay_id = "machines_cluster_mill_required_forms"
    if overlay_id not in (gate.get("java_overlay_sections") or []):
        errors.append("gate java_overlay_sections must include cluster mill foil overlay")
    overlay = gate.get(overlay_id) or {}
    materials = gate.get("materials") or {}
    for material, forms in (required_forms.get("required_forms") or {}).items():
        gated = set(materials.get(material) or [])
        overlay_forms = set(overlay.get(material) or [])
        missing = [form for form in forms if form not in gated]
        if missing:
            errors.append(
                f"gate materials missing {material} forms: {','.join(missing)}"
            )
        if overlay_forms != set(forms):
            errors.append(f"gate overlay drifted for {material}")
        if "plate_gem" in forms or "dense_plate" in forms:
            errors.append(f"{material} must not stand in plate_gem or dense_plate")

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
    if "not player_complete" in str(actual_lock.get("note") or ""):
        pass
    else:
        errors.append("production_lock must not claim player_complete")
    if actual_lock.get("production", {}).get("relation_count") != SELECTED_ROWS:
        errors.append(f"production_lock relation_count must be {SELECTED_ROWS}")

    errors.extend(
        common.check_art_manifest(
            ART_MANIFEST,
            min_art_imports=24,
            forbidden_art=("multiblock_casing", "heat_exchanger", "smelter"),
        )
    )

    d0 = census.load_json(WAVE / "d0_obtain_matrix.json")
    if d0.get("grid") != ["SSS", "wGh", "SMS"]:
        errors.append("d0 grid must match GT6 SSS / wGh / SMS")
    statuses = {row["host"]: row["status"] for row in d0.get("hosts") or []}
    if statuses != {
        20141: "source_exact",
        20142: "source_exact",
        20143: "source_exact",
        20144: "source_exact",
    }:
        errors.append("all four D0 hosts must stay source_exact")
    for row in d0.get("hosts") or []:
        casing = (row.get("casing") or {}).get("cc") or ""
        if not casing.endswith("/machine_casing_quadruple"):
            errors.append("d0 casing must stay quadruple, not double/dense")
        if "machine_casing_double" in casing:
            errors.append("d0 must not stand in double casing")
    if "programmed_circuit" in str(d0):
        errors.append("d0 must not invent programmed_circuit stand-ins")

    topology = census.load_json(WAVE / "topology.json")
    readiness = census.load_json(WAVE / "readiness.json")
    if topology.get("unique_active_wave") is not None:
        errors.append("topology unique_active_wave must be null after close")
    if readiness.get("unique_active_wave") is not None:
        errors.append("readiness unique_active_wave must be null after close")

    if errors:
        return errors
    try:
        common.isolated_compile(WAVE, LIVE_NEEDLE)
    except Exception as error:
        errors.append(f"isolated compile: {error}")
    try:
        spec = recipe_wave(IMPORT_SLUG)
        if spec.path_prefix != "clustermill":
            errors.append(f"derived path_prefix {spec.path_prefix!r} != clustermill")
        built = compile_mod.compile_wave(IMPORT_SLUG)
        relation_count = int(built["report"].get("relation_count") or 0)
        if relation_count != SELECTED_ROWS:
            errors.append(
                f"live compile relation_count {relation_count} != {SELECTED_ROWS}"
            )
        generated = live_family_files()
        if len(generated) != 1:
            errors.append(
                f"live clustermill tree must contain one compact family, "
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
                errors.append("missing compact publication policy cluster_mill.json")
            elif census.load_json(POLICY_PATH) != expected:
                errors.append("publication policy drifted from live compact family")
            elif "player_complete" in str(expected):
                errors.append("publication policy must not claim player_complete")
    except Exception as error:
        errors.append(f"live compile: {error}")
    return errors


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Cluster Mill live wave")
    modes = parser.add_mutually_exclusive_group(required=True)
    modes.add_argument("--write", action="store_true")
    modes.add_argument("--check", action="store_true")
    args = parser.parse_args(argv)
    if args.write:
        report = write()
        print(
            "Wrote machines/cluster-mill: "
            f"selected={report['source']['selected_rows']} "
            f"overflow={report['source']['overflow_rows']} "
            f"live={report['live'].get('relation_count')}"
        )
        return 0
    errors = check()
    if errors:
        print("\n".join(errors), file=sys.stderr)
        return 1
    print("machines/cluster-mill is current")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
