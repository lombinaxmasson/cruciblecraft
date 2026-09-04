#!/usr/bin/env python3
"""Shared-shape compact family matrix_v1 authoring."""
from __future__ import annotations

import json
from pathlib import Path
from typing import Any

from tools import io_common as files

AUTHORED_FORM_MATRIX_V1 = "matrix_v1"
COMPACT_FAMILY_TYPE = "cruciblecraft:compact_gt_recipe_family"
LIVE_RECIPE_ROOT = (
    files.ROOT / "src/recipe_generated/resources/data/cruciblecraft/recipe"
)


def _freeze(value: Any) -> str:
    return json.dumps(value, sort_keys=True, separators=(",", ":"))


def shape_key(relation: dict[str, Any]) -> tuple[Any, ...]:
    provenance = relation.get("provenance") or {}
    hashes = list(provenance.get("evidence_hashes") or [])
    return (
        bool(relation.get("can_be_buffered", True)),
        int(relation.get("duration") or 0),
        int(relation.get("eut") or 0),
        int(relation.get("special_value") or 0),
        tuple(int(v) for v in (relation.get("item_input_counts") or [])),
        _freeze(relation.get("item_input_actions") or []),
        tuple(int(v) for v in (relation.get("output_chances") or [])),
        str(provenance.get("source_kind") or ""),
        str(provenance.get("selected_source_recipe") or ""),
        len(relation.get("item_inputs") or []),
        len(relation.get("item_outputs") or []),
        len(relation.get("fluid_inputs") or []),
        len(relation.get("fluid_outputs") or []),
        len(hashes),
    )


def can_emit_matrix(relations: list[dict[str, Any]]) -> bool:
    if len(relations) < 2:
        return False
    shapes = {shape_key(relation) for relation in relations}
    if len(shapes) != 1:
        return False
    for relation in relations:
        hashes = list((relation.get("provenance") or {}).get("evidence_hashes") or [])
        if len(hashes) != 1 or not hashes[0]:
            return False
    return True


def _intern(values: list[Any]) -> tuple[list[Any], list[int]]:
    index: dict[str, int] = {}
    unique: list[Any] = []
    refs: list[int] = []
    for value in values:
        key = _freeze(value)
        found = index.get(key)
        if found is None:
            found = len(unique)
            index[key] = found
            unique.append(value)
        refs.append(found)
    return unique, refs


def emit_matrix_body(relations: list[dict[str, Any]]) -> dict[str, Any]:
    if not can_emit_matrix(relations):
        raise ValueError("family is not a shared-shape matrix candidate")
    first = relations[0]
    provenance = first.get("provenance") or {}
    item_inputs, input_refs = _intern(
        [list(relation.get("item_inputs") or []) for relation in relations]
    )
    item_outputs, output_refs = _intern(
        [list(relation.get("item_outputs") or []) for relation in relations]
    )
    fluids, fluid_refs = _intern(
        [
            {
                "fluid_inputs": list(relation.get("fluid_inputs") or []),
                "fluid_outputs": list(relation.get("fluid_outputs") or []),
            }
            for relation in relations
        ]
    )
    hashes, hash_refs = _intern(
        [
            str(((relation.get("provenance") or {}).get("evidence_hashes") or [""])[0])
            for relation in relations
        ]
    )
    rows = [
        [
            input_refs[index],
            output_refs[index],
            fluid_refs[index],
            str(relation["stable_id"]),
            hash_refs[index],
            int(relation.get("shadow_order") or 0),
        ]
        for index, relation in enumerate(relations)
    ]
    return {
        "shared": {
            "can_be_buffered": bool(first.get("can_be_buffered", True)),
            "duration": int(first.get("duration") or 0),
            "eut": int(first.get("eut") or 0),
            "item_input_actions": list(first.get("item_input_actions") or []),
            "item_input_counts": [int(v) for v in (first.get("item_input_counts") or [])],
            "output_chances": [int(v) for v in (first.get("output_chances") or [])],
            "selected_source_recipe": str(provenance.get("selected_source_recipe") or ""),
            "source_kind": str(provenance.get("source_kind") or ""),
            "special_value": int(first.get("special_value") or 0),
        },
        "dicts": {
            "fluids": fluids,
            "hashes": hashes,
            "item_inputs": item_inputs,
            "item_outputs": item_outputs,
        },
        "rows": rows,
    }


def expand_matrix(matrix: dict[str, Any]) -> list[dict[str, Any]]:
    shared = matrix.get("shared") or {}
    dicts = matrix.get("dicts") or {}
    item_inputs = list(dicts.get("item_inputs") or [])
    item_outputs = list(dicts.get("item_outputs") or [])
    fluids = list(dicts.get("fluids") or [])
    hashes = list(dicts.get("hashes") or [])
    relations: list[dict[str, Any]] = []
    for row in matrix.get("rows") or []:
        if not isinstance(row, list) or len(row) != 6:
            raise ValueError("matrix row must be a 6-tuple")
        input_idx, output_idx, fluid_idx, stable_id, evidence_idx, shadow_order = row
        fluid = fluids[int(fluid_idx)]
        relations.append(
            {
                "stable_id": str(stable_id),
                "item_inputs": list(item_inputs[int(input_idx)]),
                "item_input_counts": list(shared.get("item_input_counts") or []),
                "item_input_actions": list(shared.get("item_input_actions") or []),
                "item_outputs": list(item_outputs[int(output_idx)]),
                "fluid_inputs": list(fluid.get("fluid_inputs") or []),
                "fluid_outputs": list(fluid.get("fluid_outputs") or []),
                "output_chances": list(shared.get("output_chances") or []),
                "duration": int(shared.get("duration") or 0),
                "eut": int(shared.get("eut") or 0),
                "special_value": int(shared.get("special_value") or 0),
                "can_be_buffered": bool(shared.get("can_be_buffered", True)),
                "shadow_order": int(shadow_order or 0),
                "provenance": {
                    "source_kind": str(shared.get("source_kind") or ""),
                    "selected_source_recipe": str(
                        shared.get("selected_source_recipe") or ""
                    ),
                    "evidence_hashes": [str(hashes[int(evidence_idx)])],
                },
            }
        )
    return relations


def authored_relations(document: dict[str, Any]) -> list[dict[str, Any]]:
    matrix = document.get("matrix")
    if matrix:
        return expand_matrix(matrix)
    return list(document.get("relations") or [])


def authored_relation_count(document: dict[str, Any]) -> int:
    return len(authored_relations(document))


def wrap_document(document: dict[str, Any], relations: list[dict[str, Any]]) -> dict[str, Any]:
    if not can_emit_matrix(relations):
        document["relations"] = relations
        document.pop("matrix", None)
        document.pop("authored_form", None)
        return document
    wrapped = {
        key: value
        for key, value in document.items()
        if key not in {"relations", "matrix", "authored_form"}
    }
    wrapped["authored_form"] = AUTHORED_FORM_MATRIX_V1
    wrapped["matrix"] = emit_matrix_body(relations)
    return wrapped


def rewrite_document(document: dict[str, Any]) -> dict[str, Any]:
    if document.get("type") != COMPACT_FAMILY_TYPE:
        return document
    if document.get("matrix"):
        return document
    relations = list(document.get("relations") or [])
    if not can_emit_matrix(relations):
        return document
    return wrap_document(document, relations)


def load_compact_family_documents(root: Path) -> dict[str, dict[str, Any]]:
    documents: dict[str, dict[str, Any]] = {}
    for path in sorted(root.rglob("*.json")):
        if not path.is_file():
            continue
        document = files.load_json(path)
        if isinstance(document, dict) and document.get("type") == COMPACT_FAMILY_TYPE:
            documents[str(path)] = document
    return documents


def rewrite_tree(root: Path) -> dict[str, int]:
    rewritten = 0
    kept = 0
    files = 0
    for path in sorted(root.rglob("*.json")):
        if not path.is_file():
            continue
        document = files.load_json(path)
        if document.get("type") != COMPACT_FAMILY_TYPE:
            continue
        files += 1
        updated = rewrite_document(document)
        if updated is document or updated == document:
            kept += 1
            continue
        files.write_stable(path, updated)
        rewritten += 1
    return {"files": files, "rewritten": rewritten, "kept_inline": kept}
