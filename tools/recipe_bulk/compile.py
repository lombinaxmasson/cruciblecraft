#!/usr/bin/env python3
"""Compile locked historical recipe waves. recipe_bulk is the sole family emitter."""
from __future__ import annotations

import json
from collections import defaultdict
from pathlib import Path
from typing import Any

import hashlib

from tools import io_common as files
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
from tools.recipe_bulk.waves import BATH_WAVES, COMPILE_ORDER, recipe_wave


def _load_bath_mte_spec() -> dict[str, Any]:
    from tools import bath_mte_common as bath_mte

    if not bath_mte.COMPILE_SPEC.is_file():
        raise FileNotFoundError("bath_mte_recipe_compile_spec.json is required for compile")
    spec = files.load_json(bath_mte.COMPILE_SPEC)
    lock = bath_mte.load_production_lock()
    if spec.get("production_lock_sha256") != bath_mte.production_lock_sha256():
        raise ValueError("bath/mte compile spec is not bound to the issued production lock")
    if spec.get("selection_sha256") != lock["production"]["selection_sha256"]:
        raise ValueError("bath/mte compile spec selection_sha256 drifted from lock")
    return spec


def _load_bath_remainder_spec() -> dict[str, Any]:
    from tools import bath_remainder_common as bath_remainder

    if not bath_remainder.COMPILE_SPEC.is_file():
        raise FileNotFoundError("bath_remainder_recipe_compile_spec.json is required for compile")
    spec = files.load_json(bath_remainder.COMPILE_SPEC)
    lock = bath_remainder.load_production_lock()
    if spec.get("production_lock_sha256") != bath_remainder.production_lock_sha256():
        raise ValueError("bath/remainder compile spec is not bound to the issued production lock")
    if spec.get("selection_sha256") != lock["production"]["selection_sha256"]:
        raise ValueError("bath/remainder compile spec selection_sha256 drifted from lock")
    return spec


def _load_bath_identity_spec() -> dict[str, Any]:
    from tools import bath_identity_common as bath_identity

    if not bath_identity.COMPILE_SPEC.is_file():
        raise FileNotFoundError("bath_identity_recipe_compile_spec.json is required for compile")
    spec = files.load_json(bath_identity.COMPILE_SPEC)
    lock = bath_identity.load_production_lock()
    if spec.get("production_lock_sha256") != bath_identity.production_lock_sha256():
        raise ValueError("bath/identity compile spec is not bound to the issued production lock")
    if spec.get("selection_sha256") != lock["production"]["selection_sha256"]:
        raise ValueError("bath/identity compile spec selection_sha256 drifted from lock")
    return spec


def _load_bath_tiny_purified_spec() -> dict[str, Any]:
    from tools import wave_bath_tiny_purified as tiny_purified

    if not tiny_purified.COMPILE_SPEC.is_file():
        raise FileNotFoundError("bath_tiny_purified_recipe_compile_spec.json is required for compile")
    spec = files.load_json(tiny_purified.COMPILE_SPEC)
    lock = tiny_purified.load_production_lock()
    if spec.get("production_lock_sha256") != tiny_purified.production_lock_sha256():
        raise ValueError("bath/tiny-purified compile spec is not bound to the issued production lock")
    if spec.get("selection_sha256") != lock["production"]["selection_sha256"]:
        raise ValueError("bath/tiny-purified compile spec selection_sha256 drifted from lock")
    return spec


def _ledger_index_for(wave_id: str, spec: Any | None = None):
    if wave_id in BATH_WAVES:
        from tools.recipe_bulk import identity_v2

        return identity_v2.index_ledger()
    resolved = spec if spec is not None else recipe_wave(wave_id)
    if str(getattr(resolved, "path_prefix", "") or "").startswith("pilot/"):
        return {"records": {}, "blockers": {}}
    if resolved.wave_slug:
        from tools.recipe_bulk import identity_v3

        return identity_v3.index_ledger()
    return index_ledger()


def _block_object_common():
    from tools import block_object_common as common

    return common


def _load_block_object_spec() -> dict[str, Any]:
    common = _block_object_common()
    if not common.COMPILE_SPEC.is_file():
        raise FileNotFoundError("block_object_recipe_compile_spec.json is required for compile")
    spec = files.load_json(common.COMPILE_SPEC)
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
    if wave_id == "block/object":
        payload = {"item_inputs": relation.get("item_inputs") or []}
    if wave_id in BATH_WAVES or "/" in wave_id:
        payload = {
            "fluid_inputs": relation.get("fluid_inputs") or [],
            "fluid_outputs": relation.get("fluid_outputs") or [],
            "item_input_actions": relation.get("item_input_actions") or [],
            "item_input_counts": relation.get("item_input_counts") or [],
            "item_inputs": relation.get("item_inputs") or [],
            "item_outputs": relation.get("item_outputs") or [],
        }
    return json.dumps(payload, sort_keys=True, separators=(",", ":"))


def planned_documents_for(
    wave_id: str,
    *,
    spec: Any | None = None,
) -> list[tuple[Path, dict[str, Any]]]:
    spec = spec or recipe_wave(wave_id)
    if spec.identity_only:
        raise ValueError(f"{wave_id} is identity-only and has no recipe compile")
    if spec.dry_run_without_lock and (
        spec.lock_path is None or not spec.lock_path.is_file()
    ):
        return []
    if wave_id == "block/object":
        _load_block_object_spec()
    if wave_id == "bath/mte":
        _load_bath_mte_spec()
    if wave_id == "bath/remainder":
        _load_bath_remainder_spec()
    if wave_id == "bath/identity":
        _load_bath_identity_spec()
    if wave_id == "bath/tiny-purified":
        _load_bath_tiny_purified_spec()
    ledger_index = _ledger_index_for(wave_id, spec)
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


def scoped_recipe_paths(
    root: Path,
    path_prefix: str | None = None,
    tree_prefixes: tuple[str, ...] = (),
) -> list[Path]:
    if not root.exists():
        return []
    path_marker = (
        "/" + path_prefix.replace("\\", "/").strip("/") + "/"
        if path_prefix
        else None
    )
    tree_markers = tuple(
        "/" + prefix.replace("\\", "/").strip("/") + "/"
        for prefix in tree_prefixes
    )
    found: list[Path] = []
    for path in root.rglob("gt_recipe_*.json"):
        normalized = "/" + path.relative_to(root).as_posix()
        if path_marker and path_marker not in normalized:
            continue
        if tree_markers and not any(
            normalized.startswith(marker) for marker in tree_markers
        ):
            continue
        found.append(path)
    return found


def planned_documents() -> list[tuple[Path, dict[str, Any]]]:
    return planned_documents_for("block/object")


def write_tree(
    planned: list[tuple[Path, dict[str, Any]]],
    root: Path,
    generated_root: Path | None = None,
    *,
    path_prefix: str | None = None,
    tree_prefixes: tuple[str, ...] = (),
) -> None:
    origin = generated_root or _block_object_common().GENERATED_ROOT
    planned_dest: dict[Path, dict[str, Any]] = {}
    for path, document in planned:
        rel = path.relative_to(origin)
        planned_dest[root / rel] = document
    if root.exists():
        for path in scoped_recipe_paths(root, path_prefix, tree_prefixes):
            if path not in planned_dest:
                path.unlink()
    for dest, document in planned_dest.items():
        dest.parent.mkdir(parents=True, exist_ok=True)
        payload = files.stable_json(document)
        thp.assert_text_has_no_mapped_unique_tool_heads(payload, files.relative(dest))
        files.write_stable(dest, document)


def compile_report(planned: list[tuple[Path, dict[str, Any]]]) -> dict[str, Any]:
    common = _block_object_common()
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
        "status": "BLOCK_OBJECT_COMPILE_REPORT",
        "smelter_stone_rewritten": False,
    }


def wave_compile_report(
    wave_id: str,
    planned: list[tuple[Path, dict[str, Any]]],
    *,
    spec: Any | None = None,
) -> dict[str, Any]:
    spec = spec or recipe_wave(wave_id)
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
        "lock_sha256": (
            hashlib.sha256(spec.lock_path.read_bytes()).hexdigest()
            if spec.lock_path and spec.lock_path.is_file()
            else None
        ),
        "relation_count": relation_count,
        "source_sha256": (
            hashlib.sha256(spec.source_path.read_bytes()).hexdigest()
            if spec.source_path.is_file()
            else None
        ),
        "wave_id": wave_id,
        "wave_slug": spec.wave_slug,
    }


def compile_wave(wave_id: str, *, spec: Any | None = None) -> dict[str, Any]:
    spec = spec or recipe_wave(wave_id)
    planned = planned_documents_for(wave_id, spec=spec)
    payload: dict[str, Any] = {
        "planned": planned,
        "recipes": {str(path): doc for path, doc in planned},
        "report": wave_compile_report(wave_id, planned, spec=spec),
        "wave_id": wave_id,
    }
    if wave_id == "block/object":
        payload["fixture"] = {
            str(path.relative_to(spec.generated_root)): doc for path, doc in planned
        }
        payload["report"] = compile_report(planned)
    return payload


def compile(wave_id: str | None = None) -> dict[str, Any]:
    return compile_wave(wave_id or "block/object")


def compile_all() -> dict[str, Any]:
    waves: dict[str, dict[str, Any]] = {}
    for wave_id in COMPILE_ORDER:
        waves[wave_id] = compile_wave(wave_id)
    return {"ok": True, "waves": waves}
