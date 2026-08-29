#!/usr/bin/env python3
"""Deterministic operand resolver chain for recipe bulk compilation."""
from __future__ import annotations

from typing import Any

MAPPING_CLASSES = (
    "exact_runtime",
    "explicit_source_mapping",
    "registered_material_form",
    "proven_canonical_tag",
    "proven_source_derived_alias",
    "explicit_object_expression",
    "unresolved",
)


def resolve_operand(
    operand: dict[str, Any],
    *,
    runtime_map: dict[tuple[str, int | None], dict[str, Any]] | None = None,
    catalogs: dict[str, Any] | None = None,
) -> dict[str, Any]:
    """Resolve one source operand. Fail closed rather than guess by display name."""
    source = operand.get("source") or {}
    item = str(source.get("item") or operand.get("item") or "")
    meta = source.get("meta")
    existing = str(operand.get("runtime_id") or "")
    if existing.startswith(("minecraft:", "cruciblecraft:")):
        return {
            "class": "exact_runtime",
            "disposition": "proven_equivalent",
            "evidence": "already_mapped_runtime",
            "registration_authority": "cruciblecraft",
            "route_key": existing,
            "runtime_id": existing,
        }
    key = (item, int(meta) if isinstance(meta, int) else None)
    mapped = (runtime_map or {}).get(key)
    if mapped and mapped.get("runtime_id"):
        runtime = str(mapped["runtime_id"])
        return {
            "class": str(mapped.get("class") or "explicit_object_expression"),
            "disposition": "proven_equivalent",
            "evidence": str(mapped.get("evidence") or "explicit_source_object_map"),
            "registration_authority": "cruciblecraft",
            "route_key": runtime,
            "runtime_id": runtime,
        }
    catalogs = catalogs or {}
    vanilla = (catalogs.get("vanilla_aliases") or {}).get(item)
    if vanilla:
        return {
            "class": "proven_source_derived_alias",
            "disposition": "proven_equivalent",
            "evidence": "vanilla_alias",
            "registration_authority": "minecraft",
            "route_key": str(vanilla),
            "runtime_id": str(vanilla),
        }
    return {
        "class": "unresolved",
        "disposition": "unsupported",
        "evidence": "resolver_chain_exhausted",
        "registration_authority": None,
        "route_key": None,
        "runtime_id": None,
    }
