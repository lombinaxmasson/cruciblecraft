#!/usr/bin/env python3
"""Build the source-exact GT6 RM.Shredder recipe wave.

The dump is the authority. Rows whose real operands are currently registered
are compiled into the canonical ``cruciblecraft:shredder`` family; unresolved
material forms remain explicit overflow. The bounded selection guard prevents
this card from becoming an unbounded long-tail import.
"""
from __future__ import annotations

import argparse
import importlib.util
import json
import re
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
from tools.recipe_bulk.matrix import authored_relations
from tools.recipe_bulk import source_import
from tools.recipe_bulk.membership import membership_root
from tools.recipe_bulk.waves import recipe_wave

_COMMON_PATH = ROOT / "tools" / "waves" / "prep" / "machine_prep_common.py"
_COMMON_SPEC = importlib.util.spec_from_file_location(
    "machine_prep_common", _COMMON_PATH
)
assert _COMMON_SPEC is not None and _COMMON_SPEC.loader is not None
common = importlib.util.module_from_spec(_COMMON_SPEC)
_COMMON_SPEC.loader.exec_module(common)

SOURCE_REVISION = common.SOURCE_REVISION
SOURCE_MAP = "gt.recipe.shredder"
TARGET_MAP = "cruciblecraft:shredder"
HOST = TARGET_MAP
IMPORT_SLUG = "machines/large-shredder"
SOURCE_PACK_ID = IMPORT_SLUG
FAMILY_ID = (
    "portfolio:track_a/cruciblecraft:shredder/"
    "gt.recipe.shredder#0000"
)
TEMPLATE_KEY = "gt.recipe.shredder#0000"
LIVE_NEEDLE = "shredder"
MAX_SELECTED_ROWS = 30_000
ART_MANIFEST = "gt6_large_shredder_art_manifest.json"

WAVE = ROOT / "tools" / "waves" / "machines" / "large-shredder"
LIVE_GENERATED = (
    ROOT
    / "src"
    / "recipe_generated"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "recipe"
)
POLICY_PATH = LIVE_GENERATED / "publication_policy" / "shredder.json"
PUBLICATION_GROUP = f"{TARGET_MAP}/pilot/large_shredder"
LOCK_NOTE = (
    "live compile for machines/large-shredder; source-exact registered "
    "RM.Shredder rows only; unresolved real forms remain blocked overflow; "
    "canonical family is shared with single-block shredder; not player_complete"
)


def _write(path: Path, document: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    census.write_stable(path, document)


def _dump() -> dict[str, Any]:
    path = common.dump_path(SOURCE_MAP)
    return json.loads(path.read_text(encoding="utf-8"))


def _audited() -> tuple[list[dict[str, Any]], list[dict[str, Any]]]:
    dump = _dump()
    selected, overflow = common.audit_rows(
        dump,
        host=HOST,
        target_map=TARGET_MAP,
        source_map=SOURCE_MAP,
        family_id=FAMILY_ID,
        template_key=TEMPLATE_KEY,
    )
    if len(selected) > MAX_SELECTED_ROWS:
        raise ValueError(
            f"bounded Shredder selection exceeded {MAX_SELECTED_ROWS}: "
            f"{len(selected)}"
        )
    return selected, overflow


def _mte(meta: int) -> dict[str, Any]:
    result = resolve(f"getItem({meta})")
    if result.get("status") != "ok" or not result.get("item"):
        return {
            "gt": f"getItem({meta})",
            "meta": meta,
            "cc": "",
            "status": "blocked",
            "reason": str(result.get("status") or "unmapped"),
        }
    return {
        "gt": f"getItem({meta})",
        "meta": meta,
        "cc": str(result["item"]),
        "status": "ok",
        "self_ref": bool(result.get("self_ref")),
    }


def _form(token: str) -> dict[str, Any]:
    result = resolve(token)
    form = result.get("form") or {}
    item = form.get("item")
    if result.get("status") == "ok" and item:
        return {
            "gt": token,
            "cc": str(item),
            "status": "ok",
        }
    return {
        "gt": token,
        "cc": "",
        "status": "blocked",
        "reason": str(result.get("status") or "unmapped"),
    }


def _il(token: str) -> dict[str, Any]:
    result = resolve(token)
    if result.get("status") == "ok" and result.get("item"):
        return {
            "gt": token,
            "cc": str(result["item"]),
            "status": "ok",
        }
    return {
        "gt": token,
        "cc": "",
        "status": "blocked",
        "reason": str(result.get("status") or "unmapped"),
    }


def d0_matrix() -> dict[str, Any]:
    cells = {
        "G": _form("OP.gearGt(MT.TungstenSteel)"),
        "S": _form("OP.gearGtSmall(MT.TungstenSteel)"),
        "R": _il("IL.Processor_Crystal_Ruby"),
        "M": _mte(18003),
        "C": {
            "gt": "OD_CIRCUITS[6]",
            "cc": "cruciblecraft:circuit_ultimate",
            "status": "ok",
        },
    }
    structure = {
        "wall": _mte(18003),
        "blades": _mte(18108),
    }
    return {
        "capability_slug": IMPORT_SLUG,
        "controller": {
            "class": "MultiTileEntityShredder",
            "meta": 17109,
            "recipe_map": "RM.Shredder",
        },
        "cells": cells,
        "grid": ["SGS", "GSG", "RMC"],
        "structure": structure,
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "status": (
            "source_exact"
            if all(
                cell.get("status") == "ok"
                for cell in [*cells.values(), *structure.values()]
            )
            else "explicitly_blocked"
        ),
    }


def required_forms(overflow: list[dict[str, Any]]) -> dict[str, Any]:
    pairs: dict[str, set[str]] = {}
    examples: dict[tuple[str, str], set[str]] = {}
    pattern = re.compile(r"unregistered material form ([^:]+):([^ ]+)")
    for row in overflow:
        for reason in row.get("reasons") or []:
            match = pattern.search(str(reason))
            if not match:
                continue
            material, form = match.groups()
            pairs.setdefault(material, set()).add(form)
            examples.setdefault((material, form), set()).add(
                str(row.get("input") or row.get("output") or "unknown")
            )
    normalized = {
        material: sorted(forms)
        for material, forms in sorted(pairs.items())
    }
    return {
        "counts": {
            "required_form_pairs": sum(len(forms) for forms in normalized.values()),
            "required_materials": len(normalized),
        },
        "generated_by": "machines/large-shredder source-pack audit",
        "note": (
            "Demand evidence only. This file does not open forms or create "
            "stand-ins; openable pairs belong to the material-form-demand census."
        ),
        "required_forms": normalized,
        "examples": {
            f"{material}:{form}": sorted(values)
            for (material, form), values in sorted(examples.items())
        },
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "status": "bounded_demand_evidence",
    }


def write_source_pack() -> dict[str, int]:
    dump = _dump()
    recipes = list(dump.get("recipes") or [])
    selected, overflow = _audited()
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
    _write(WAVE / "required_forms.json", required_forms(overflow))
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
    source_import.write_import(WAVE / "recipe_import.json")
    common._drop_consume_collisions(
        WAVE,
        host=HOST,
        family_id=FAMILY_ID,
        template_key=TEMPLATE_KEY,
        import_slug=IMPORT_SLUG,
    )
    accounting = census.load_json(WAVE / "source_pack" / "work_set.json").get(
        "accounting"
    ) or {}
    return {
        "source_rows": int(accounting.get("source_rows") or 0),
        "selected_rows": int(accounting.get("selected_rows") or 0),
        "overflow_rows": int(accounting.get("overflow_rows") or 0),
    }


def write_sidecars(counts: dict[str, int]) -> None:
    _write(
        WAVE / "source_contract.json",
        {
            "damage": {"amount": 5, "kind": "shredder", "on_walk_over": True},
            "energy": {
                "capacity": 4096,
                "input_maximum": 4096,
                "input_minimum": 512,
                "type": "RU",
            },
            "gt6": {
                "class": "gregtech.tileentity.multiblocks.MultiTileEntityShredder",
                "loader_meta": 17109,
                "method": "checkStructure2",
                "recipe_map": "RM.Shredder",
                "source_revision": SOURCE_REVISION,
            },
            "io": {
                "fluid_inputs": 0,
                "fluid_outputs": 0,
                "item_inputs": 1,
                "item_outputs": 12,
                "item_input_layer": "top_blades_only",
                "item_output_layer": "bottom",
                "automatic_input": False,
                "automatic_output": True,
            },
            "no_constant_power": True,
            "parallel": 64,
            "parallel_duration": True,
            "overclock": "cheap",
            "efficiency": 5000,
            "schema_version": 1,
            "status": "source_exact",
            "structure": {
                "dimensions": [5, 5, 3],
                "wall_blocks": 56,
                "middle_blades": 9,
                "top_input_blades": 9,
                "energy_ports": 2,
                "controller": "side_bottom_center",
            },
        },
    )
    _write(
        WAVE / "topology.json",
        {
            "append_only": False,
            "canonical_map": TARGET_MAP,
            "complete_family_count": 1,
            "generated_by": "machines/large-shredder implementation",
            "remaining_recipe_gap": counts["overflow_rows"],
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
                "dump_rows": counts["source_rows"],
                "overflow_rows": counts["overflow_rows"],
                "selected_rows": counts["selected_rows"],
                "status": "runtime_ready",
            },
            "generated_by": "machines/large-shredder implementation",
            "generated_recipe_count": counts["selected_rows"],
            "next_unassigned": True,
            "owns_families": 1,
            "production_lock": census.relative(WAVE / "production_lock.json"),
            "schema_version": 1,
            "source_revision": SOURCE_REVISION,
            "status": "LARGE_SHREDDER_RUNTIME_READY",
            "unique_active_wave": None,
            "wave_slug": IMPORT_SLUG,
        },
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


def live_family_documents() -> dict[str, list[dict[str, Any]]]:
    families: dict[str, list[dict[str, Any]]] = {}
    for path in live_family_files():
        document = census.load_json(path)
        family_id = str(document.get("family_id") or "")
        if family_id:
            families.setdefault(family_id, []).append(document)
    return families


def expected_publication_policy() -> dict[str, Any]:
    families = live_family_documents()
    if len(families) != 1:
        raise ValueError(f"need one live Shredder family, got {len(families)}")
    family_id, documents = next(iter(families.items()))
    relations = [
        relation
        for document in documents
        for relation in authored_relations(document)
    ]
    stable_ids = [str(row.get("stable_id") or "") for row in relations]
    if not family_id or not all(stable_ids):
        raise ValueError("live Shredder family has incomplete stable ids")
    return {
        "cache_ceiling": 0,
        "eager_stable_ids": [],
        "family_count": 1,
        "membership_root_sha256": membership_root([family_id], stable_ids),
        "policy_type": "immediate",
        "publication_group": PUBLICATION_GROUP,
        "relation_count": len(relations),
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
    if counts["selected_rows"] > MAX_SELECTED_ROWS:
        raise ValueError("bounded Shredder selection exceeded guard")
    common.freeze_lock(WAVE, IMPORT_SLUG, LOCK_NOTE)
    _write(WAVE / "d0_obtain_matrix.json", d0_matrix())
    write_sidecars(counts)
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
        WAVE / "source_contract.json",
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
        errors.extend(source_import.check_import(WAVE / "recipe_import.json"))
    except Exception as error:
        errors.append(f"source pack/import: {error}")

    dump = _dump()
    work = census.load_json(WAVE / "source_pack" / "work_set.json")
    overflow = census.load_json(WAVE / "overflow.json")
    accounting = work.get("accounting") or {}
    selected = (work.get("families") or [{}])[0].get("relations") or []
    if accounting.get("source_rows") != len(dump.get("recipes") or []):
        errors.append("work_set source_rows disagrees with the live GT6 dump")
    if accounting.get("selected_rows") != len(selected):
        errors.append("work_set selected_rows disagrees with family relations")
    if accounting.get("overflow_rows") != len(overflow.get("overflow") or []):
        errors.append("work_set overflow_rows disagrees with overflow")
    if accounting.get("selected_rows", 0) > MAX_SELECTED_ROWS:
        errors.append("bounded Shredder selection guard was exceeded")
    if "programmed_circuit" in str(overflow):
        errors.append("overflow must not contain programmed_circuit stand-ins")

    lock = census.load_json(WAVE / "production_lock.json")
    if lock != common.pilot_lock(WAVE, IMPORT_SLUG, LOCK_NOTE):
        errors.append("production_lock drifted from lock_candidate")
    if lock.get("production_authority") is not True:
        errors.append("production_lock must keep production_authority")
    if "not player_complete" not in str(lock.get("note") or ""):
        errors.append("production_lock must not claim player_complete")

    contract = census.load_json(WAVE / "source_contract.json")
    if contract.get("gt6", {}).get("loader_meta") != 17109:
        errors.append("source contract meta must be 17109")
    if contract.get("gt6", {}).get("recipe_map") != "RM.Shredder":
        errors.append("source contract map must be RM.Shredder")
    if contract.get("io", {}).get("item_outputs") != 12:
        errors.append("source contract must expose twelve item outputs")
    if contract.get("no_constant_power") is not True:
        errors.append("source contract must retain no-constant-power behavior")

    d0 = census.load_json(WAVE / "d0_obtain_matrix.json")
    if d0.get("grid") != ["SGS", "GSG", "RMC"]:
        errors.append("D0 grid must match GT6 SGS/GSG/RMC")
    if d0.get("status") != "source_exact":
        errors.append("D0 controller recipe must remain source_exact")
    if (
        d0.get("cells", {}).get("M", {}).get("cc")
        != "cruciblecraft:tungstensteel/wall"
        or d0.get("cells", {}).get("M", {}).get("meta") != 18003
    ):
        errors.append("D0 M must be the source-exact tungstensteel wall")
    if "programmed_circuit" in str(d0):
        errors.append("D0 must not contain programmed_circuit stand-ins")

    errors.extend(
        common.check_art_manifest(
            ART_MANIFEST,
            min_art_imports=49,
            forbidden_art=("multiblock_casing", "pipe_filter_cover", "crusher"),
        )
    )

    structure = census.load_json(
        ROOT
        / "src"
        / "main"
        / "resources"
        / "data"
        / "cruciblecraft"
        / "multiblock_structures"
        / "large_shredder.json"
    )
    predicates = [
        row.get("predicate") for row in structure.get("structure") or []
    ]
    if predicates.count("C") != 1 or predicates.count("E") != 2:
        errors.append("structure controller/energy-port count drifted")
    if predicates.count("N") != 9 or predicates.count("I") != 9:
        errors.append("structure blade-layer counts drifted")
    if predicates.count("W") + predicates.count("O") != 54:
        errors.append("structure wall count drifted")
    if structure.get("source", {}).get("class", "").endswith("Shredder") is False:
        errors.append("structure source class must be MultiTileEntityShredder")

    try:
        common.isolated_compile(WAVE, LIVE_NEEDLE)
        built = compile_mod.compile_wave(IMPORT_SLUG)
        relation_count = int(built["report"].get("relation_count") or 0)
        if relation_count != accounting.get("selected_rows"):
            errors.append("compiled relation count disagrees with work_set")
        families = live_family_documents()
        if len(families) != 1:
            errors.append(
                f"live Shredder tree must contain one family, got {len(families)}"
            )
        elif sum(
                len(authored_relations(document))
                for document in next(iter(families.values()))
        ) != accounting.get("selected_rows"):
            errors.append("live Shredder fragments disagree with work_set")
        if POLICY_PATH.is_file() and census.load_json(POLICY_PATH) != expected_publication_policy():
            errors.append("Shredder publication policy drifted")
    except Exception as error:
        errors.append(f"live compile: {error}")
    return errors


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="GT6 Large Shredder recipe wave")
    modes = parser.add_mutually_exclusive_group(required=True)
    modes.add_argument("--write", action="store_true")
    modes.add_argument("--check", action="store_true")
    args = parser.parse_args(argv)
    if args.write:
        report = write()
        print(
            "Wrote machines/large-shredder: "
            f"selected={report['source']['selected_rows']} "
            f"overflow={report['source']['overflow_rows']} "
            f"live={report['live'].get('relation_count')}"
        )
        return 0
    errors = check()
    if errors:
        print("\n".join(errors), file=sys.stderr)
        return 1
    print("machines/large-shredder is current")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
