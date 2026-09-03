#!/usr/bin/python3
"""Historical source adapters. Canonical IR comes from the unified compile path."""
from __future__ import annotations

import json
from typing import Any

from tools import t35_common as t35
from tools.recipe_bulk.compile import compile_wave
from tools.recipe_bulk.ir import validate_wave_ir
from tools.recipe_bulk.models import ShadowFamily, WaveIR, WaveSpec
from tools.recipe_bulk.selection import (
    group_relations,
    relative_path,
    select_source_relations,
)
from tools.recipe_bulk.templates import expand_family
from tools.recipe_bulk.waves import recipe_wave
from tools.recipe_bulk.matrix import authored_relation_count, authored_relations

SOURCE_REVISION = t35.SOURCE_REVISION


def _resolved_operands(document: dict[str, Any]) -> list[dict[str, Any]]:
    operands: list[dict[str, Any]] = []
    for relation in authored_relations(document):
        for item in relation.get("item_inputs") or []:
            operands.append({"direction": "item_input", "value": item})
        for item in relation.get("item_outputs") or []:
            operands.append({"direction": "item_output", "value": item})
        for fluid in relation.get("fluid_inputs") or []:
            operands.append({"direction": "fluid_input", "value": fluid})
        for fluid in relation.get("fluid_outputs") or []:
            operands.append({"direction": "fluid_output", "value": fluid})
    return operands


def _package_family(
    spec: WaveSpec,
    *,
    template_key: str,
    source_rows: list[dict[str, Any]],
    document: dict[str, Any],
    lock_row: dict[str, Any] | None,
) -> ShadowFamily:
    expanded = expand_family(
        family_id=template_key,
        relations=source_rows,
        template_kind=(
            spec.template_kind
            if spec.archetype.endswith("singleton") or len(source_rows) == 1
            else "exact_relation_set"
        ),
    )
    template_kind = str(expanded["template_kind"])
    publication_group = document.get("publication_group")
    return ShadowFamily(
        semantic_family_id=template_key,
        template_kind=template_kind,
        relative_path=relative_path(spec, template_key, lock_row),
        target_map=str(document.get("target_map") or spec.target_map),
        source_revision=str(document.get("source_revision") or SOURCE_REVISION),
        document=document,
        source_rows=list(expanded["source_rows"]),
        resolved_operands=_resolved_operands(document),
        cardinality_proof={
            "lock_template_key": template_key if lock_row is not None else None,
            "ordered_by": spec.relation_sort,
            "relation_count": authored_relation_count(document),
        },
        provenance={
            "archetype": spec.archetype,
            "authority": t35.relative(spec.lock_path)
            if spec.lock_path is not None
            else t35.relative(spec.source_path),
            "compile_authority": spec.compile_authority,
            "wave": spec.wave_id,
        },
        publication_group=str(publication_group) if publication_group else None,
        lock_row=lock_row,
    )


def adapt_wave(spec: WaveSpec | str) -> WaveIR:
    if isinstance(spec, str):
        spec = recipe_wave(spec)
    if spec.identity_only:
        raise ValueError(f"{spec.wave_id} is identity-only and has no recipe IR")
    built = compile_wave(spec.wave_id)
    relations, lock_rows = select_source_relations(spec)
    grouped = group_relations(spec, relations)
    by_template = {
        str(document["family_id"]): document for _path, document in built["planned"]
    }
    if set(by_template) != set(grouped):
        raise ValueError(f"{spec.wave_id} emit family set drifted from selected source")
    families: list[ShadowFamily] = []
    for template_key in sorted(grouped):
        families.append(
            _package_family(
                spec,
                template_key=template_key,
                source_rows=grouped[template_key],
                document=by_template[template_key],
                lock_row=lock_rows.get(template_key),
            )
        )
    ir = WaveIR(
        wave_id=spec.wave_id,
        archetype=spec.archetype,
        template_kind=spec.template_kind,
        source_revision=SOURCE_REVISION,
        source_path=t35.relative(spec.source_path),
        source_sha256=t35.sha256_file(spec.source_path),
        target_map=spec.target_map,
        families=families,
        lock_path=t35.relative(spec.lock_path) if spec.lock_path else None,
        lock_sha256=t35.sha256_file(spec.lock_path) if spec.lock_path else None,
    )
    errors = validate_wave_ir(ir)
    if errors:
        raise ValueError(f"{spec.wave_id} IR invalid: " + "; ".join(errors[:8]))
    if spec.expected_family_count is not None and len(families) != spec.expected_family_count:
        raise ValueError(
            f"{spec.wave_id} family count {len(families)} != {spec.expected_family_count}"
        )
    relation_count = sum(authored_relation_count(family.document) for family in families)
    if (
        spec.expected_relation_count is not None
        and relation_count != spec.expected_relation_count
    ):
        raise ValueError(
            f"{spec.wave_id} relation count {relation_count} != {spec.expected_relation_count}"
        )
    return ir


def planned_documents(spec: WaveSpec | str) -> dict[str, dict[str, Any]]:
    ir = adapt_wave(spec)
    return {family.relative_path: family.document for family in ir.families}


def consume_identity(relation: dict[str, Any]) -> str:
    return json.dumps(
        {
            "fluid_inputs": relation.get("fluid_inputs") or [],
            "item_input_actions": relation.get("item_input_actions") or [],
            "item_input_counts": relation.get("item_input_counts") or [],
            "item_inputs": relation.get("item_inputs") or [],
        },
        sort_keys=True,
        separators=(",", ":"),
    )
