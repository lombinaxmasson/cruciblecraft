#!/usr/bin/env python3
"""Finite V1 templates. No implicit cartesian product."""
from __future__ import annotations

from typing import Any

ALLOWED = {
    "exact_singleton",
    "exact_relation_set",
    "finite_bindings",
    "keyed_join",
}


def expand_family(
    *,
    family_id: str,
    relations: list[dict[str, Any]],
    template_kind: str | None = None,
    cardinality_proof: dict[str, Any] | None = None,
) -> dict[str, Any]:
    kind = template_kind or (
        "exact_singleton" if len(relations) == 1 else "exact_relation_set"
    )
    if kind not in ALLOWED:
        raise ValueError(f"unsupported V1 template: {kind}")
    if kind == "exact_singleton" and len(relations) != 1:
        raise ValueError(f"{family_id}: exact_singleton requires one relation")
    if kind == "keyed_join" and not cardinality_proof:
        raise ValueError(f"{family_id}: keyed_join requires cardinality proof")
    if kind == "finite_bindings" and not cardinality_proof:
        raise ValueError(f"{family_id}: finite_bindings requires cardinality proof")
    return {
        "cardinality_proof": cardinality_proof,
        "relations": list(relations),
        "semantic_family_id": family_id,
        "source_rows": [row.get("source_recipe_index") for row in relations],
        "template_kind": kind,
    }
