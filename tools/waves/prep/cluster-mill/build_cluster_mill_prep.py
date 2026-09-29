#!/usr/bin/env python3
"""Build the prep-only Cluster Mill Source Pack and isolated import outputs.

This lane intentionally does not register a RecipeMap or write
``src/recipe_generated``.  The selected work set contains only rows whose
source operands already have an exact CrucibleCraft expression; the remaining
GT6 rows are retained in the explicit overflow ledger.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import sys
import tempfile
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[4]
TOOLS = ROOT / "tools"
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))
if str(TOOLS) not in sys.path:
    sys.path.insert(1, str(TOOLS))

from tools import census_common as census
from tools.recipe_bulk import source_import
from tools.recipe_bulk.dialects import gt6
from tools.recipe_bulk.pilot import compile_fixture, reviewed_lock
from tools.gt6_recipe_templates import _stable_json

SOURCE_REVISION = "3703e40308c8c030763fd6297dea8b210d2a77b1"
SOURCE_MAP = "gt.recipe.clustermill"
TARGET_MAP = "cruciblecraft:clustermill"
HOST = TARGET_MAP
IMPORT_SLUG = "prep/cluster-mill"
SOURCE_PACK_ID = "prep/machines-cluster-mill"
FAMILY_ID = (
    "portfolio:track_a/cruciblecraft:clustermill/"
    "gt.recipe.clustermill#0000"
)
TEMPLATE_KEY = "gt.recipe.clustermill#0000"

WAVE = ROOT / "tools" / "waves" / "prep" / "cluster-mill"
SOURCE_PACK = WAVE / "source_pack"
RAW_DUMP = ROOT / "gt6_dump" / "gt6_recipe_dump" / "maps" / (
    "gt.recipe.clustermill.json"
)
DUMP_SLICE = SOURCE_PACK / "dump_slice.json"
WORK_SET = SOURCE_PACK / "work_set.json"
MANIFEST = WAVE / "source_pack_manifest.json"
IMPORT = WAVE / "recipe_import.json"
OVERFLOW = WAVE / "overflow.json"
LOCK_CANDIDATE = WAVE / "lock_candidate.json"
PRODUCTION_LOCK = WAVE / "production_lock.json"
LIVE_GENERATED = (
    ROOT / "src" / "recipe_generated" / "resources" / "data" / "cruciblecraft"
    / "recipe"
)
CLUSTER_MILL_HOSTS = ("bronze", "steel", "titanium", "tungstensteel")
GATE = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "material_registration_gate.json"
)
CASING_FAMILY_FLAG = "cruciblecraft:generates_machine_casing"
LOCK_NOTE = (
    "prep isolated compile for machines/cluster-mill; "
    "not a live RecipeMap import; not player_complete"
)


def _write(path: Path, document: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    census.write_stable(path, document)


def _row_hash(recipe: dict[str, Any]) -> str:
    """Hash the source row before prep metadata is attached."""
    return hashlib.sha256(_stable_json(recipe).encode("utf-8")).hexdigest()


def _source_row(
    recipe: dict[str, Any], index: int, digest: str
) -> dict[str, Any]:
    row = dict(recipe)
    row["source_recipe_index"] = index
    row["source_row_sha256"] = digest
    row["template_key"] = TEMPLATE_KEY
    return row


def _audit_rows(
    dump: dict[str, Any],
) -> tuple[list[dict[str, Any]], list[dict[str, Any]]]:
    maps = gt6._maps()
    selected: list[dict[str, Any]] = []
    overflow: list[dict[str, Any]] = []
    for index, recipe in enumerate(dump.get("recipes") or []):
        digest = _row_hash(recipe)
        relation, errors = gt6.compile_row(
            recipe,
            host=HOST,
            target_map=TARGET_MAP,
            source_map=SOURCE_MAP,
            family_id=FAMILY_ID,
            template_key=TEMPLATE_KEY,
            recipe_index=index,
            shadow_order=0,
            source_revision=SOURCE_REVISION,
            source_row_sha256=digest,
            maps=maps,
        )
        blockers = list(dict.fromkeys(str(error) for error in errors))
        if source_import._relation_unmapped(relation):
            blockers.append("unmapped operand")
        entry = {
            "source_recipe_index": index,
            "source_row_sha256": digest,
            "input": (recipe.get("inputs") or [{}])[0].get("displayName"),
            "output": (recipe.get("outputs") or [{}])[0].get("displayName"),
        }
        if blockers:
            entry["status"] = "blocked"
            entry["reasons"] = blockers
            overflow.append(entry)
        else:
            selected.append(
                {
                    "source_recipe_index": index,
                    "source_row_sha256": digest,
                    "shadow_order": len(selected),
                }
            )
    return selected, overflow


def _build_work_set(
    selected: list[dict[str, Any]],
    overflow: list[dict[str, Any]],
    source_rows: int,
) -> dict[str, Any]:
    return {
        "accounting": {
            "blocked_rows": len(overflow),
            "overflow_rows": len(overflow),
            "selected_rows": len(selected),
            "source_rows": source_rows,
            "selection_rule": (
                "retain only rows whose every item/fluid operand has an "
                "exact current expression"
            ),
        },
        "families": [
            {
                "family_id": FAMILY_ID,
                "relations": selected,
                "template_key": TEMPLATE_KEY,
            }
        ],
        "host": HOST,
        "overflow": overflow,
        "source_revision": SOURCE_REVISION,
    }


def _build_manifest() -> dict[str, Any]:
    return {
        "files": [
            {
                "path": census.relative(DUMP_SLICE),
                "role": "dump_slice",
                "sha256": census.sha256_file(DUMP_SLICE),
            },
            {
                "path": census.relative(WORK_SET),
                "role": "work_set",
                "sha256": census.sha256_file(WORK_SET),
            },
        ],
        "full_replay": {
            "required_for_first_generation": True,
            "skip_is_not_pass": True,
        },
        "provenance_policy": {
            "append_only": True,
            "forbid_gt6u": True,
        },
        "schema_version": 1,
        "source_dialect": "gt6",
        "source_pack_id": SOURCE_PACK_ID,
        "source_revision": SOURCE_REVISION,
        "source_system": "gt6",
    }


def _build_import_spec() -> dict[str, Any]:
    return {
        "family_membership_source": {
            "kind": "work_set",
            "path": census.relative(WORK_SET),
        },
        "host": HOST,
        "import_slug": IMPORT_SLUG,
        "operand_authorities": [
            {"id": "identity_ledger_v3", "kind": "data"},
            {"id": "material_form_authority", "kind": "data"},
            {"adapter": "gt6", "kind": "source_dialect"},
        ],
        "output_paths": {
            "lock_candidate": census.relative(WAVE / "lock_candidate.json"),
            "receipt": census.relative(WAVE / "source_receipt.json"),
            "review": census.relative(WAVE / "source_review.json"),
            "source": census.relative(WAVE / "source.json"),
        },
        "representation_policy": {"allowed": ["exact_multi"]},
        "schema_version": 1,
        "selection_rule": {"kind": "work_set_members"},
        "source_maps": [SOURCE_MAP],
        "source_pack": census.relative(MANIFEST),
        "stable_id_policy": {
            "algorithm": "source_system_revision_family_relation",
            "include_card_number": False,
        },
        "target_map": TARGET_MAP,
    }


def _pilot_lock(candidate: dict[str, Any] | None = None) -> dict[str, Any]:
    payload = candidate if candidate is not None else census.load_json(LOCK_CANDIDATE)
    spec_doc = census.load_json(IMPORT)
    cohort = IMPORT_SLUG.split("/", 1)[-1].replace("-", "_")
    publication_group = f"{spec_doc['target_map']}/pilot/{cohort}"
    lock = reviewed_lock(
        payload,
        publication_group=publication_group,
        cohort=cohort,
    )
    lock["note"] = LOCK_NOTE
    return lock


def freeze_lock() -> dict[str, Any]:
    lock = _pilot_lock()
    _write(PRODUCTION_LOCK, lock)
    return lock


def isolated_compile() -> dict[str, Any]:
    live_before = {
        path.name
        for path in LIVE_GENERATED.rglob("*clustermill*")
        if path.is_file()
    }
    with tempfile.TemporaryDirectory(prefix="cluster-mill-prep-") as tmp:
        dest = Path(tmp)
        metrics = compile_fixture(IMPORT, dest)
        generated = dest / "recipe_generated"
        if not generated.is_dir():
            raise ValueError("isolated compile did not write a recipe tree")
        generated_root = generated.resolve()
        live_root = LIVE_GENERATED.resolve()
        if generated_root == live_root or live_root in generated_root.parents:
            raise ValueError("isolated compile must not use src/recipe_generated")
        live_after = {
            path.name
            for path in LIVE_GENERATED.rglob("*clustermill*")
            if path.is_file()
        }
        if live_after != live_before:
            raise ValueError("isolated compile wrote live clustermill recipes")
        return metrics


def _gated_forms(form: str) -> set[str]:
    gate = census.load_json(GATE)
    materials = gate.get("materials") or {}
    return {
        material
        for material, forms in materials.items()
        if form in (forms or [])
    }


def _materials_with_flag(flag: str) -> set[str]:
    materials = ROOT / "src" / "main" / "resources" / "data" / "cruciblecraft" / "materials"
    found: set[str] = set()
    for path in materials.glob("*.json"):
        document = census.load_json(path)
        if not isinstance(document, dict):
            continue
        flags = document.get("generation_flags") or []
        if flag in flags:
            found.add(path.stem)
    return found


def write() -> dict[str, int]:
    dump = json.loads(RAW_DUMP.read_text(encoding="utf-8"))
    recipes = list(dump.get("recipes") or [])
    if len(recipes) != 307:
        raise ValueError(f"Cluster Mill dump must contain 307 rows, got {len(recipes)}")

    selected, overflow = _audit_rows(dump)
    dump_slice = {
        "recipes": [
            _source_row(recipe, index, _row_hash(recipe))
            for index, recipe in enumerate(recipes)
        ],
        "source_map": SOURCE_MAP,
        "source_revision": SOURCE_REVISION,
    }
    work_set = _build_work_set(selected, overflow, len(recipes))
    _write(DUMP_SLICE, dump_slice)
    _write(WORK_SET, work_set)
    _write(OVERFLOW, {
        "blocked_rows": len(overflow),
        "overflow": overflow,
        "schema_version": 1,
        "source_map": SOURCE_MAP,
        "source_revision": SOURCE_REVISION,
        "status": "EXPLICITLY_BLOCKED_OVERFLOW",
    })
    _write(MANIFEST, _build_manifest())
    _write(IMPORT, _build_import_spec())
    source_import.write_import(IMPORT)
    return {
        "source_rows": len(recipes),
        "selected_rows": len(selected),
        "overflow_rows": len(overflow),
    }


def check() -> list[str]:
    errors: list[str] = []
    required = (
        WORK_SET,
        MANIFEST,
        IMPORT,
        OVERFLOW,
        LOCK_CANDIDATE,
        PRODUCTION_LOCK,
    )
    for path in required:
        if not path.is_file():
            errors.append(f"missing {census.relative(path)}")
    if errors:
        return errors

    try:
        manifest = source_import.load_manifest(MANIFEST)
        work_entry = next(
            entry
            for entry in manifest.get("files") or []
            if entry.get("role") == "work_set"
        )
        actual_hash = census.sha256_file(WORK_SET)
        if actual_hash != str(work_entry["sha256"]):
            errors.append("work_set hash drifted from source pack manifest")
    except Exception as error:
        errors.append(f"source pack: {error}")

    work = census.load_json(WORK_SET)
    overflow = census.load_json(OVERFLOW)
    accounting = work.get("accounting") or {}
    selected = (work.get("families") or [{}])[0].get("relations") or []
    if accounting.get("source_rows") != 307:
        errors.append("work_set accounting source_rows must be 307")
    if accounting.get("selected_rows") != len(selected):
        errors.append("work_set accounting selected_rows disagrees with family relations")
    if accounting.get("overflow_rows") != len(overflow.get("overflow") or []):
        errors.append("work_set accounting overflow_rows disagrees with overflow")
    if len(work.get("overflow") or []) != 0:
        errors.append("work_set overflow must be empty after foil forms are gated")
    if accounting.get("selected_rows") != 307:
        errors.append("work_set selected_rows must be 307")
    if accounting.get("source_rows") != (
        int(accounting.get("selected_rows") or 0)
        + int(accounting.get("overflow_rows") or 0)
    ):
        errors.append("307-row dump must split into selected + overflow")

    expected_lock = _pilot_lock()
    actual_lock = census.load_json(PRODUCTION_LOCK)
    if actual_lock != expected_lock:
        errors.append("production_lock drifted from reviewed lock_candidate")
    if actual_lock.get("production_authority") is not True:
        errors.append("production_lock must keep production_authority for isolated compile")
    if "not player_complete" not in str(actual_lock.get("note") or ""):
        errors.append("production_lock must not claim player_complete")

    prefix = census.load_json(
        ROOT
        / "src"
        / "main"
        / "resources"
        / "data"
        / "cruciblecraft"
        / "material_prefixes"
        / "machine_casing_quadruple.json"
    )
    if prefix.get("units") != 3744:
        errors.append("quadruple casing prefix units drifted")
    if "casingMachineQuadruple" not in (prefix.get("aliases") or []):
        errors.append("quadruple casing alias drifted")
    if prefix.get("generation_flag") != CASING_FAMILY_FLAG:
        errors.append(
            "quadruple casing must share generates_machine_casing with ordinary/double"
        )
    ordinary = _gated_forms("machine_casing")
    quadruple = _gated_forms("machine_casing_quadruple")
    missing = ordinary - quadruple
    if missing:
        errors.append(
            "quadruple casing missing for ordinary casing materials: "
            + ",".join(sorted(missing))
        )
    flagged = _materials_with_flag(CASING_FAMILY_FLAG)
    if not set(CLUSTER_MILL_HOSTS) <= quadruple:
        errors.append("cluster mill host quadruple casings are not gated")
    if not set(CLUSTER_MILL_HOSTS) <= flagged:
        errors.append("cluster mill hosts must keep generates_machine_casing")
    extra_flag = _materials_with_flag(
        "cruciblecraft:generates_machine_casing_quadruple"
    )
    if extra_flag:
        errors.append(
            "narrow generates_machine_casing_quadruple flag still present: "
            + ",".join(sorted(extra_flag))
        )

    if DUMP_SLICE.is_file():
        try:
            metrics = isolated_compile()
        except Exception as error:
            errors.append(f"isolated compile: {error}")
            return errors
        if metrics.get("family_count") != 1:
            errors.append("isolated compile must emit one family")
        if metrics.get("relation_count") != 307:
            errors.append("isolated compile must emit 307 relations")
        if metrics.get("representations", {}).get("exact_multi") != 1:
            errors.append("isolated compile must stay exact_multi")
        if metrics.get("overflow"):
            errors.append("isolated compile must not silently overflow")
        if metrics.get("parameterized"):
            errors.append("isolated compile must not emit parameterized families")
    return errors


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    mode = parser.add_mutually_exclusive_group()
    mode.add_argument("--write", action="store_true", help="rebuild prep artifacts")
    mode.add_argument("--check", action="store_true", help="check existing prep artifacts")
    mode.add_argument(
        "--freeze-lock",
        action="store_true",
        help="review lock_candidate into production_lock.json",
    )
    args = parser.parse_args(argv)
    try:
        if args.check:
            errors = check()
            if errors:
                print("\n".join(errors), file=sys.stderr)
                return 1
            print("cluster-mill prep artifacts are current")
            return 0
        if args.freeze_lock:
            freeze_lock()
            print("froze cluster-mill prep production_lock")
            return 0
        counts = write()
        print(
            "wrote cluster-mill prep artifacts: "
            f"{counts['source_rows']} source / "
            f"{counts['selected_rows']} selected / "
            f"{counts['overflow_rows']} overflow"
        )
        return 0
    except Exception as error:
        print(str(error), file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
