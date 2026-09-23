#!/usr/bin/env python3
"""Build the source-exact GT6 Implosion Compressor recipe wave."""
from __future__ import annotations

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
from tools.recipe_bulk import source_import

COMMON_PATH = TOOLS / "waves" / "prep" / "machine_prep_common.py"
COMMON_SPEC = importlib.util.spec_from_file_location("machine_prep_common", COMMON_PATH)
assert COMMON_SPEC is not None and COMMON_SPEC.loader is not None
common = importlib.util.module_from_spec(COMMON_SPEC)
COMMON_SPEC.loader.exec_module(common)

SOURCE_REVISION = common.SOURCE_REVISION
SOURCE_MAP = "gt.recipe.implosioncompressor"
TARGET_MAP = "cruciblecraft:implosion_compressor"
HOST = TARGET_MAP
IMPORT_SLUG = "machines/implosion-compressor"
SOURCE_PACK_ID = IMPORT_SLUG
FAMILY_ID = "portfolio:track_b/cruciblecraft:implosion_compressor/gt.recipe.implosioncompressor#0000"
TEMPLATE_KEY = "gt.recipe.implosioncompressor#0000"
WAVE = ROOT / "tools" / "waves" / "machines" / "implosion-compressor"


def _dump() -> dict[str, Any]:
    path = ROOT / "gt6_dump" / "gt6_recipe_dump" / "maps" / f"{SOURCE_MAP}.json"
    return json.loads(path.read_text(encoding="utf-8"))


def _write(path: Path, document: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    census.write_stable(path, document)


def _mte(meta: int) -> dict[str, Any]:
    result = resolve(f"getItem({meta})")
    return {
        "gt": f"getItem({meta})",
        "meta": meta,
        "cc": str(result.get("item") or ""),
        "status": "ok" if result.get("status") == "ok" else "blocked",
        "self_ref": bool(result.get("self_ref")),
    }


def _form(token: str) -> dict[str, Any]:
    result = resolve(token)
    form = result.get("form") or {}
    return {
        "gt": token,
        "cc": str(form.get("item") or ""),
        "status": "ok" if result.get("status") == "ok" and form.get("item") else "blocked",
        "reason": None if result.get("status") == "ok" else str(result.get("status") or "unmapped"),
    }


def _il(token: str) -> dict[str, Any]:
    result = resolve(token)
    return {
        "gt": token,
        "cc": str(result.get("item") or ""),
        "status": "ok" if result.get("status") == "ok" and result.get("item") else "blocked",
        "reason": None if result.get("status") == "ok" else str(result.get("status") or "unmapped"),
    }


def _required_forms(overflow: list[dict[str, Any]]) -> dict[str, Any]:
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
        material: sorted(forms) for material, forms in sorted(pairs.items())
    }
    return {
        "counts": {
            "required_form_pairs": sum(len(forms) for forms in normalized.values()),
            "required_materials": len(normalized),
        },
        "examples": {
            f"{material}:{form}": sorted(values)
            for (material, form), values in sorted(examples.items())
        },
        "generated_by": f"{IMPORT_SLUG} source-pack audit",
        "note": (
            "Demand evidence only. This file does not open forms or create "
            "stand-ins; openable pairs belong to the material-form census."
        ),
        "required_forms": normalized,
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "status": "bounded_demand_evidence",
    }


def write_source_pack() -> dict[str, int]:
    dump = _dump()
    recipes = list(dump.get("recipes") or [])
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
    _write(WAVE / "required_forms.json", _required_forms(overflow))
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
    accounting = census.load_json(WAVE / "source_pack" / "work_set.json").get("accounting") or {}
    return {
        "source_rows": int(accounting.get("source_rows") or 0),
        "selected_rows": int(accounting.get("selected_rows") or 0),
        "overflow_rows": int(accounting.get("overflow_rows") or 0),
    }


def write_sidecars(counts: dict[str, int]) -> None:
    _write(
        WAVE / "source_contract.json",
        {
            "energy": {
                "capacity": 16,
                "input_maximum": 16,
                "input_minimum": 1,
                "type": "TU",
            },
            "gt6": {
                "class": "gregtech.tileentity.multiblocks.MultiTileEntityImplosionCompressor",
                "loader_meta": 17110,
                "method": "checkStructure2",
                "recipe_map": "RM.ImplosionCompressor",
                "source_revision": SOURCE_REVISION,
            },
            "io": {
                "automatic_input": False,
                "automatic_output": True,
                "fluid_inputs": 0,
                "fluid_outputs": 0,
                "item_input_ports": 25,
                "item_inputs": 3,
                "item_outputs": 3,
                "port_type": "ITEM_FLUID_ENERGY",
                "output_side": "BOTTOM",
            },
            "no_constant_power": True,
            "parallel": 64,
            "parallel_duration": False,
            "recipe": {
                "duration": 256,
                "eut": 0,
                "output_chance": 10000,
                "source_rows": counts["source_rows"],
            },
            "schema_version": 1,
            "source_tokens": {
                "pattern": ["CPC", "PAP", "RMR"],
                "required": ["A=IL.ROBOT_ARMS[2]"],
            },
            "status": "source_exact",
            "structure": {
                "air_cells": 1,
                "controller": "side_bottom_center",
                "dimensions": [3, 3, 3],
                "wall_blocks": 25,
            },
        },
    )
    _write(
        WAVE / "d0_obtain_matrix.json",
        {
            "capability_slug": IMPORT_SLUG,
            "cells": {
                "A": _il("IL.ROBOT_ARMS[2]"),
                "C": {"cc": "cruciblecraft:circuit_ultimate", "gt": "OD_CIRCUITS[6]", "status": "ok"},
                "M": {**_mte(18023), "gt": "getItem(18023)"},
                "P": _form("OP.plateDense(MT.TungstenSteel)"),
                "R": _il("IL.Processor_Crystal_Ruby"),
            },
            "controller": {
                "class": "MultiTileEntityImplosionCompressor",
                "meta": 17110,
                "recipe_map": SOURCE_MAP,
            },
            "grid": ["CPC", "PAP", "RMR"],
            "schema_version": 1,
            "source_revision": SOURCE_REVISION,
            "status": "source_exact",
        },
    )
    _write(
        WAVE / "topology.json",
        {
            "append_only": False,
            "canonical_map": TARGET_MAP,
            "complete_family_count": 1,
            "generated_by": f"{IMPORT_SLUG} implementation",
            "remaining_recipe_gap": counts["overflow_rows"],
            "schema_version": 1,
            "source_revision": SOURCE_REVISION,
            "status": "WAVE_READY",
            "unique_active_wave": IMPORT_SLUG,
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
            "generated_by": f"{IMPORT_SLUG} implementation",
            "generated_recipe_count": counts["selected_rows"],
            "next_unassigned": False,
            "owns_families": 1,
            "production_lock": census.relative(WAVE / "production_lock.json"),
            "schema_version": 1,
            "source_revision": SOURCE_REVISION,
            "status": "IMPLOSION_COMPRESSOR_RUNTIME_READY",
            "unique_active_wave": IMPORT_SLUG,
            "wave_slug": IMPORT_SLUG,
        },
    )


def write() -> dict[str, int]:
    counts = write_source_pack()
    common.freeze_lock(
        WAVE,
        IMPORT_SLUG,
        "source-exact GT6 Implosion Compressor rows; unresolved material forms remain explicit blocked overflow",
    )
    write_sidecars(counts)
    return counts


def check() -> list[str]:
    errors: list[str] = []
    required = (
        WAVE / "source_pack" / "dump_slice.json",
        WAVE / "source_pack" / "work_set.json",
        WAVE / "source_pack_manifest.json",
        WAVE / "recipe_import.json",
        WAVE / "source.json",
        WAVE / "lock_candidate.json",
        WAVE / "production_lock.json",
        WAVE / "source_contract.json",
        WAVE / "required_forms.json",
    )
    errors.extend(
        f"missing {census.relative(path)}"
        for path in required
        if not path.is_file()
    )
    if errors:
        return errors
    try:
        manifest = source_import.load_manifest(
            WAVE / "source_pack_manifest.json")
        source_import.verify_files(manifest, require_present=True)
        errors.extend(source_import.check_import(
            WAVE / "recipe_import.json"))
    except Exception as error:
        errors.append(f"source pack/import: {error}")
    work = census.load_json(WAVE / "source_pack" / "work_set.json")
    accounting = work.get("accounting") or {}
    if accounting.get("source_rows") != 1072:
        errors.append("source row denominator drifted")
    if accounting.get("selected_rows") != 776:
        errors.append("selected row count drifted")
    if accounting.get("overflow_rows") != 296:
        errors.append("overflow row count drifted")
    contract = census.load_json(WAVE / "source_contract.json")
    if contract.get("gt6", {}).get("loader_meta") != 17110:
        errors.append("loader meta drifted")
    if contract.get("source_tokens", {}).get("pattern") != [
            "CPC", "PAP", "RMR"]:
        errors.append("crafting pattern drifted")
    return errors


if __name__ == "__main__":
    parser = __import__("argparse").ArgumentParser()
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    if args.check:
        errors = check()
        if errors:
            raise SystemExit("\n".join(errors))
        print("Implosion Compressor source pack passed")
    else:
        print(write())
