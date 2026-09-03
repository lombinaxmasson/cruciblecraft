#!/usr/bin/python3
"""Freeze production generated trees, stable IDs, and equivalence roots."""
from __future__ import annotations

import hashlib
from pathlib import Path
from typing import Any

from tools import t27_common as t27
from tools import t35_common as t35
from tools.recipe_bulk.waves import SHADOW_ORDER, recipe_wave
from tools.recipe_bulk.matrix import authored_relations

STATUS = "RECIPE_WAVE_PRODUCTION_BASELINE"


def tree_sha256(root: Path) -> str:
    hasher = hashlib.sha256()
    for path in sorted(p for p in root.rglob("gt_recipe_*.json") if p.is_file()):
        relative = path.relative_to(root).as_posix().encode("utf-8")
        hasher.update(relative)
        hasher.update(b"\0")
        hasher.update(path.read_bytes())
        hasher.update(b"\0")
    return hasher.hexdigest()


def production_files(root: Path) -> dict[str, str]:
    files: dict[str, str] = {}
    for path in sorted(p for p in root.rglob("gt_recipe_*.json") if p.is_file()):
        relative = path.relative_to(root).as_posix()
        files[relative] = t35.sha256_file(path)
    return files


def stable_ids_from_tree(root: Path) -> list[str]:
    ids: list[str] = []
    for path in sorted(p for p in root.rglob("gt_recipe_*.json") if p.is_file()):
        document = t35.load_json(path)
        for relation in authored_relations(document):
            stable_id = relation.get("stable_id")
            if stable_id:
                ids.append(str(stable_id))
    return ids


def wave_snapshot(wave_id: str) -> dict[str, Any]:
    spec = recipe_wave(wave_id)
    files = production_files(spec.generated_root)
    stable_ids = stable_ids_from_tree(spec.generated_root)
    equivalence = t35.load_json(spec.equivalence_path) if spec.equivalence_path.is_file() else {}
    generated = equivalence.get("generated") or {}
    runtime = equivalence.get("runtime_expected") or {}
    return {
        "equivalence_generated_sha256": generated.get("sha256"),
        "equivalence_path": t35.relative(spec.equivalence_path),
        "equivalence_sha256": t35.sha256_file(spec.equivalence_path)
        if spec.equivalence_path.is_file()
        else None,
        "file_count": len(files),
        "generated_root": t35.relative(spec.generated_root),
        "generated_tree_sha256": tree_sha256(spec.generated_root),
        "stable_id_count": len(stable_ids),
        "stable_id_sha256": t27.sha256_record(stable_ids),
        "runtime_expected_sha256": runtime.get("sha256"),
    }


def build() -> dict[str, Any]:
    waves = {wave_id: wave_snapshot(wave_id) for wave_id in SHADOW_ORDER}
    return {
        "generated_by": "python tools/build_recipe_wave_production_baseline.py",
        "note": (
            "Frozen production trees/stable IDs/equivalence roots for the "
            "unified import shadow repair gate. Shadow must not rewrite these roots."
        ),
        "schema_version": 1,
        "status": STATUS,
        "waves": waves,
        "write_guard": {
            "authority_manifest": "tools/authority_manifest.json",
            "protected_generated_roots": [
                t35.relative(recipe_wave(wave_id).generated_root)
                for wave_id in SHADOW_ORDER
            ],
        },
    }
