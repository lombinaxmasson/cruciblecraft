#!/usr/bin/python3
"""Validate unified recipe-wave IR documents."""
from __future__ import annotations

from typing import Any

from tools.recipe_bulk.models import WaveIR
from tools.recipe_bulk.templates import ALLOWED

FAMILY_REQUIRED = (
    "cardinality_proof",
    "relations",
    "resolved_operands",
    "semantic_family_id",
    "source_rows",
    "template_kind",
)


def validate_ir_document(document: dict[str, Any]) -> list[str]:
    errors: list[str] = []
    if document.get("schema_version") != 1:
        errors.append("IR schema_version must be 1")
    families = document.get("families")
    if not isinstance(families, list):
        errors.append("IR families must be a list")
        return errors
    if not document.get("wave_id"):
        errors.append("IR wave_id is required")
    if not document.get("source_revision"):
        errors.append("IR source_revision is required")
    if not document.get("archetype"):
        errors.append("IR archetype is required")
    seen_paths: set[str] = set()
    seen_families: set[str] = set()
    for index, family in enumerate(families):
        prefix = f"families[{index}]"
        if not isinstance(family, dict):
            errors.append(f"{prefix} is not an object")
            continue
        for key in FAMILY_REQUIRED:
            if key not in family:
                errors.append(f"{prefix} missing {key}")
        kind = family.get("template_kind")
        if kind not in ALLOWED:
            errors.append(f"{prefix} unsupported template_kind {kind}")
        relations = family.get("relations") or []
        source_rows = family.get("source_rows") or []
        if not isinstance(relations, list):
            errors.append(f"{prefix} relations must be a list")
            continue
        if kind == "exact_singleton" and len(relations) != 1:
            errors.append(f"{prefix} exact_singleton requires one relation")
        if len(source_rows) != len(relations):
            errors.append(
                f"{prefix} source_rows length {len(source_rows)} != relations {len(relations)}"
            )
        family_id = str(family.get("semantic_family_id") or "")
        if family_id in seen_families:
            errors.append(f"{prefix} duplicate semantic_family_id {family_id}")
        seen_families.add(family_id)
        relative = str(family.get("relative_path") or "")
        if relative:
            if relative in seen_paths:
                errors.append(f"{prefix} duplicate relative_path {relative}")
            seen_paths.add(relative)
        for rel_index, relation in enumerate(relations):
            if not isinstance(relation, dict):
                errors.append(f"{prefix}.relations[{rel_index}] is not an object")
                continue
            if not relation.get("stable_id"):
                errors.append(f"{prefix}.relations[{rel_index}] missing stable_id")
            shadow_order = relation.get("shadow_order")
            if shadow_order is None:
                errors.append(f"{prefix}.relations[{rel_index}] missing shadow_order")
        proof = family.get("cardinality_proof") or {}
        if proof.get("relation_count") not in {None, len(relations)}:
            errors.append(
                f"{prefix} cardinality_proof.relation_count drifted from relations"
            )
    return errors


def validate_wave_ir(ir: WaveIR) -> list[str]:
    return validate_ir_document(ir.to_document())
