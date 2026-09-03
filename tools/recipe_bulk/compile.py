#!/usr/bin/env python3
"""Compile locked historical recipe waves. recipe_bulk is the sole family emitter."""
from __future__ import annotations

import json
from collections import defaultdict
from pathlib import Path
from typing import Any

from tools import t35_common as t35
from tools import t45_common as common
from tools import tool_head_prefix as thp
from tools.recipe_bulk.emit import emit_wave_document
from tools.recipe_bulk.matrix import authored_relation_count, authored_relations
from tools.recipe_bulk.identity import index_ledger
from tools.recipe_bulk.resolver import resolve_relation_operands
from tools.recipe_bulk.selection import (
    group_relations,
    publication_group,
    relative_path,
    select_source_relations,
    target_map_for,
)
from tools.recipe_bulk.templates import expand_family
from tools.recipe_bulk.waves import COMPILE_ORDER, recipe_wave


def _load_t46_spec() -> dict[str, Any]:
    from tools import t46_common as t46

    if not t46.COMPILE_SPEC.is_file():
        raise FileNotFoundError("t46_recipe_compile_spec.json is required for compile")
    spec = common.load_json(t46.COMPILE_SPEC)
    lock = t46.load_production_lock()
    if spec.get("production_lock_sha256") != t46.production_lock_sha256():
        raise ValueError("T46 compile spec is not bound to the issued production lock")
    if spec.get("selection_sha256") != lock["production"]["selection_sha256"]:
        raise ValueError("T46 compile spec selection_sha256 drifted from lock")
    return spec


def _load_t47_spec() -> dict[str, Any]:
    from tools import t47_common as t47

    if not t47.COMPILE_SPEC.is_file():
        raise FileNotFoundError("t47_recipe_compile_spec.json is required for compile")
    spec = common.load_json(t47.COMPILE_SPEC)
    lock = t47.load_production_lock()
    if spec.get("production_lock_sha256") != t47.production_lock_sha256():
        raise ValueError("T47 compile spec is not bound to the issued production lock")
    if spec.get("selection_sha256") != lock["production"]["selection_sha256"]:
        raise ValueError("T47 compile spec selection_sha256 drifted from lock")
    return spec


def _load_t48_spec() -> dict[str, Any]:
    from tools import t48_common as t48

    if not t48.COMPILE_SPEC.is_file():
        raise FileNotFoundError("t48_recipe_compile_spec.json is required for compile")
    spec = common.load_json(t48.COMPILE_SPEC)
    lock = t48.load_production_lock()
    if spec.get("production_lock_sha256") != t48.production_lock_sha256():
        raise ValueError("T48 compile spec is not bound to the issued production lock")
    if spec.get("selection_sha256") != lock["production"]["selection_sha256"]:
        raise ValueError("T48 compile spec selection_sha256 drifted from lock")
    return spec


def _load_t49_spec() -> dict[str, Any]:
    from tools import wave_bath_tiny_purified as tiny_purified

    if not tiny_purified.COMPILE_SPEC.is_file():
        raise FileNotFoundError("t49_recipe_compile_spec.json is required for compile")
    spec = common.load_json(tiny_purified.COMPILE_SPEC)
    lock = tiny_purified.load_production_lock()
    if spec.get("production_lock_sha256") != tiny_purified.production_lock_sha256():
        raise ValueError("T49 compile spec is not bound to the issued production lock")
    if spec.get("selection_sha256") != lock["production"]["selection_sha256"]:
        raise ValueError("T49 compile spec selection_sha256 drifted from lock")
    return spec


def _ledger_index_for(wave_id: str):
    if wave_id in {"T46", "T47", "T48", "T49"}:
        from tools.recipe_bulk import identity_v2

        return identity_v2.index_ledger()
    spec = recipe_wave(wave_id)
    if spec.wave_slug:
        from tools.recipe_bulk import identity_v3

        return identity_v3.index_ledger()
    return index_ledger()


def _load_t45_spec() -> dict[str, Any]:
    if not common.COMPILE_SPEC.is_file():
        raise FileNotFoundError("t45_recipe_compile_spec.json is required for compile")
    spec = common.load_json(common.COMPILE_SPEC)
    lock = common.load_production_lock()
    lock_hash = spec.get("production_lock_sha256")
    if lock_hash != common.production_lock_sha256():
        raise ValueError("compile spec is not bound to the issued production lock")
    if spec.get("selection_sha256") != lock["production"]["selection_sha256"]:
        raise ValueError("compile spec selection_sha256 drifted from lock")
    return spec


def _template_kind(spec, source_rows: list[dict[str, Any]]) -> str:
    if spec.archetype.endswith("singleton") or len(source_rows) == 1:
        return spec.template_kind if spec.archetype.endswith("singleton") else (
            "exact_singleton" if len(source_rows) == 1 else "exact_relation_set"
        )
    return spec.template_kind


def _consume_identity(relation: dict[str, Any], *, wave_id: str) -> str:
    payload = {
        "fluid_inputs": relation.get("fluid_inputs") or [],
        "item_input_actions": relation.get("item_input_actions") or [],
        "item_input_counts": relation.get("item_input_counts") or [],
        "item_inputs": relation.get("item_inputs") or [],
    }
    if wave_id == "T45":
        payload = {"item_inputs": relation.get("item_inputs") or []}
    if wave_id in {"T47", "T48", "T49"} or "/" in wave_id:
        payload = {
            "fluid_inputs": relation.get("fluid_inputs") or [],
            "fluid_outputs": relation.get("fluid_outputs") or [],
            "item_input_actions": relation.get("item_input_actions") or [],
            "item_input_counts": relation.get("item_input_counts") or [],
            "item_inputs": relation.get("item_inputs") or [],
            "item_outputs": relation.get("item_outputs") or [],
        }
    return json.dumps(payload, sort_keys=True, separators=(",", ":"))


def planned_documents_for(wave_id: str) -> list[tuple[Path, dict[str, Any]]]:
    spec = recipe_wave(wave_id)
    if spec.identity_only:
        raise ValueError(f"{wave_id} is identity-only and has no recipe compile")
    if spec.dry_run_without_lock and (
        spec.lock_path is None or not spec.lock_path.is_file()
    ):
        return []
    if wave_id == "T45":
        _load_t45_spec()
    if wave_id == "T46":
        _load_t46_spec()
    if wave_id == "T47":
        _load_t47_spec()
    if wave_id == "T48":
        _load_t48_spec()
    if wave_id == "T49":
        _load_t49_spec()
    ledger_index = _ledger_index_for(wave_id)
    relations, lock_rows = select_source_relations(spec)
    grouped = group_relations(spec, relations)
    planned: list[tuple[Path, dict[str, Any]]] = []
    seen_consume: dict[str, str] = {}
    for template_key in sorted(grouped):
        source_rows = grouped[template_key]
        lock_row = lock_rows.get(template_key)
        kind = _template_kind(spec, source_rows)
        expanded = expand_family(
            family_id=template_key,
            relations=source_rows,
            template_kind=kind,
        )
        resolved_rows: list[dict[str, Any]] = []
        operand_classes: list[list[str]] = []
        for relation in source_rows:
            if relation.get("parameterized"):
                raise ValueError(f"{template_key}: parameterized source must not compile")
            resolved, classes = resolve_relation_operands(
                relation,
                wave_id=wave_id,
                index=ledger_index,
            )
            resolved_rows.append(resolved)
            operand_classes.append(classes)
        group = publication_group(
            spec,
            lock_row=lock_row,
            relation=source_rows[0],
            expanded_count=len(source_rows),
        )
        target_map = target_map_for(spec, lock_row=lock_row, relation=source_rows[0])
        document = emit_wave_document(
            spec,
            template_key=template_key,
            source_rows=source_rows,
            resolved_rows=resolved_rows,
            lock_row=lock_row,
            target_map=target_map,
            publication_group=group,
            operand_classes=operand_classes,
        )
        for relation in authored_relations(document):
            consume = _consume_identity(relation, wave_id=wave_id)
            previous = seen_consume.get(f"{target_map}:{consume}")
            if previous:
                raise ValueError(
                    f"consume-identity collision on {target_map}: {previous} vs {template_key}"
                )
            seen_consume[f"{target_map}:{consume}"] = (
                f"{template_key}:{relation.get('stable_id')}"
            )
        rel = relative_path(spec, template_key, lock_row)
        path = spec.generated_root / rel
        planned.append((path, document))
        if spec.expected_family_count is None:
            continue
        _ = expanded
    if spec.expected_family_count is not None and len(planned) != spec.expected_family_count:
        if not (spec.dry_run_without_lock and not planned):
            raise ValueError(
                f"{wave_id} family count {len(planned)} != {spec.expected_family_count}"
            )
    relation_count = sum(authored_relation_count(doc) for _path, doc in planned)
    if (
        spec.expected_relation_count is not None
        and relation_count != spec.expected_relation_count
    ):
        if not (spec.dry_run_without_lock and not planned):
            raise ValueError(
                f"{wave_id} relation count {relation_count} != {spec.expected_relation_count}"
            )
    return planned


def planned_documents() -> list[tuple[Path, dict[str, Any]]]:
    return planned_documents_for("T45")


def write_tree(
    planned: list[tuple[Path, dict[str, Any]]],
    root: Path,
    generated_root: Path | None = None,
    *,
    path_prefix: str | None = None,
) -> None:
    origin = generated_root or common.GENERATED_ROOT
    if root.exists():
        marker = None
        if path_prefix:
            marker = "/" + path_prefix.replace("\\", "/").strip("/") + "/"
        for path in root.rglob("gt_recipe_*.json"):
            if marker and marker not in str(path).replace("\\", "/"):
                continue
            path.unlink()
    for path, document in planned:
        rel = path.relative_to(origin)
        dest = root / rel
        dest.parent.mkdir(parents=True, exist_ok=True)
        payload = t35.stable_json(document)
        thp.assert_text_has_no_mapped_unique_tool_heads(payload, t35.relative(dest))
        t35.write_stable(dest, document)


def compile_report(planned: list[tuple[Path, dict[str, Any]]]) -> dict[str, Any]:
    lock = common.load_production_lock()
    by_group: dict[str, int] = defaultdict(int)
    for _path, document in planned:
        group = document.get("publication_group")
        if group:
            by_group[str(group)] += 1
    authored = common.OPENING_AUTHORED_ENTRIES + len(planned)
    return {
        "authored_entries_closing": authored,
        "authored_entries_opening": common.OPENING_AUTHORED_ENTRIES,
        "authored_hard_ceiling": common.T14_HARD_AUTHORED,
        "authored_under_ceiling": authored < common.T14_HARD_AUTHORED,
        "bundling": "v1_one_family_one_document",
        "family_count": len(planned),
        "generated_by": "python tools/build_recipe_bulk.py compile",
        "groups": dict(sorted(by_group.items())),
        "production_lock_sha256": common.production_lock_sha256(),
        "relation_count": len(planned),
        "schema_version": 1,
        "selection_sha256": lock["production"]["selection_sha256"],
        "status": "T45_COMPILE_REPORT",
        "t43_rewritten": False,
    }


def wave_compile_report(
    wave_id: str,
    planned: list[tuple[Path, dict[str, Any]]],
) -> dict[str, Any]:
    spec = recipe_wave(wave_id)
    by_group: dict[str, int] = defaultdict(int)
    relation_count = 0
    for _path, document in planned:
        relation_count += authored_relation_count(document)
        group = document.get("publication_group")
        if group:
            by_group[str(group)] += 1
    return {
        "family_count": len(planned),
        "groups": dict(sorted(by_group.items())),
        "lock_sha256": t35.sha256_file(spec.lock_path) if spec.lock_path and spec.lock_path.is_file() else None,
        "relation_count": relation_count,
        "source_sha256": t35.sha256_file(spec.source_path) if spec.source_path.is_file() else None,
        "wave_id": wave_id,
        "wave_slug": spec.wave_slug,
    }


def compile_wave(wave_id: str) -> dict[str, Any]:
    spec = recipe_wave(wave_id)
    planned = planned_documents_for(wave_id)
    payload: dict[str, Any] = {
        "planned": planned,
        "recipes": {str(path): doc for path, doc in planned},
        "report": wave_compile_report(wave_id, planned),
        "wave_id": wave_id,
    }
    if wave_id == "T45":
        payload["fixture"] = {
            str(path.relative_to(spec.generated_root)): doc for path, doc in planned
        }
        payload["report"] = compile_report(planned)
    return payload


def compile(wave_id: str | None = None) -> dict[str, Any]:
    return compile_wave(wave_id or "T45")


def compile_all() -> dict[str, Any]:
    waves: dict[str, dict[str, Any]] = {}
    for wave_id in COMPILE_ORDER:
        waves[wave_id] = compile_wave(wave_id)
    return {"ok": True, "waves": waves}
