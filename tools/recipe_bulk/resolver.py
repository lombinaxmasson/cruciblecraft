#!/usr/bin/env python3
"""Deterministic operand resolver. Ledger is the fail-closed compile authority."""
from __future__ import annotations

from typing import Any

from tools.recipe_bulk import identity as identity_mod
from tools import tool_head_prefix as thp

MAPPING_CLASSES = (
    "exact_runtime",
    "explicit_source_mapping",
    "registered_material_form",
    "proven_canonical_tag",
    "proven_source_derived_alias",
    "explicit_object_expression",
    "unresolved",
)

PUBLISHED_PREFIXES = ("minecraft:", "cruciblecraft:")
GT_PREFIXES = ("gregtech:", "gregapi:", "fixed:")


class ResolutionError(ValueError):
    """Compile-time identity resolution failed closed."""


def _unresolved() -> dict[str, Any]:
    return {
        "class": "unresolved",
        "disposition": "unsupported",
        "evidence": "resolver_chain_exhausted",
        "registration_authority": None,
        "route_key": None,
        "runtime_id": None,
        "components": None,
        "source_key": None,
        "source_kind": None,
        "target_identity": None,
    }


def _legacy_resolve(
    operand: dict[str, Any],
    *,
    runtime_map: dict[tuple[str, int | None], dict[str, Any]] | None,
    catalogs: dict[str, Any] | None,
) -> dict[str, Any]:
    source = operand.get("source") or {}
    item = str(source.get("item") or operand.get("item") or "")
    meta = source.get("meta")
    existing = str(operand.get("runtime_id") or "")
    if existing.startswith(PUBLISHED_PREFIXES):
        existing = thp.rewrite_published_runtime(existing, item, meta)
        return {
            "class": "exact_runtime",
            "disposition": "proven_equivalent",
            "evidence": "already_mapped_runtime",
            "registration_authority": "cruciblecraft",
            "route_key": existing,
            "runtime_id": existing,
            "components": None,
            "source_key": None,
            "source_kind": None,
            "target_identity": existing,
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
            "components": None,
            "source_key": None,
            "source_kind": None,
            "target_identity": runtime,
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
            "components": None,
            "source_key": None,
            "source_kind": None,
            "target_identity": str(vanilla),
        }
    return _unresolved()


def _existing_runtime(operand: dict[str, Any]) -> str:
    source = operand.get("source") or {}
    item = str(source.get("item") or operand.get("item") or "")
    meta = source.get("meta")
    runtime = str(operand.get("runtime_id") or "")
    if runtime.startswith(PUBLISHED_PREFIXES):
        return thp.rewrite_published_runtime(runtime, item, meta)
    alias = operand.get("alias")
    if isinstance(alias, str) and alias.startswith(PUBLISHED_PREFIXES):
        return thp.rewrite_published_runtime(alias, item, meta)
    return ""


def resolve_operand(
    operand: dict[str, Any],
    *,
    wave_id: str | None = None,
    ledger: dict[str, Any] | None = None,
    index: dict[str, dict[str, dict[str, Any]]] | None = None,
    require_proven: bool = False,
    runtime_map: dict[tuple[str, int | None], dict[str, Any]] | None = None,
    catalogs: dict[str, Any] | None = None,
) -> dict[str, Any]:
    """Resolve one source operand. Fail closed rather than guess by display name."""
    source = operand.get("source") or {}
    item = source.get("item") or operand.get("item")
    meta = source.get("meta")
    if item == identity_mod.CIRCUIT_ITEM and not isinstance(meta, int):
        if require_proven or wave_id:
            raise ResolutionError("malformed circuit operand is missing integer meta")
        return _unresolved()
    if wave_id:
        store = index
        if store is None:
            store = identity_mod.index_ledger(ledger)
        existing = _existing_runtime(operand)
        semantic = "/" in str(wave_id)
        hit = identity_mod.lookup_first(wave_id, operand, index=store)
        if semantic and existing and (
            hit is None or (hit is not None and hit[0] == "blocker")
        ):
            return {
                "class": "exact_runtime",
                "disposition": "proven_equivalent",
                "evidence": "already_mapped_runtime",
                "registration_authority": "cruciblecraft",
                "route_key": existing,
                "runtime_id": existing,
                "components": None,
                "source_key": None,
                "source_kind": None,
                "target_identity": existing,
            }
        if hit is None:
            if existing:
                return {
                    "class": "exact_runtime",
                    "disposition": "proven_equivalent",
                    "evidence": "already_mapped_runtime",
                    "registration_authority": "cruciblecraft",
                    "route_key": existing,
                    "runtime_id": existing,
                    "components": None,
                    "source_key": None,
                    "source_kind": None,
                    "target_identity": existing,
                }
            if require_proven:
                raise ResolutionError(
                    f"{wave_id} missing identity for {identity_mod.candidate_keys(wave_id, operand)}"
                )
            return _unresolved()
        kind, record = hit
        if kind == "blocker":
            if require_proven:
                raise ResolutionError(
                    f"{wave_id} identity blocker {record.get('source_key')}: "
                    f"{record.get('blocker_reason')}"
                )
            return {
                "class": str(record.get("mapping_class") or "unresolved"),
                "disposition": "blocker",
                "evidence": str(record.get("blocker_reason") or "typed_blocker"),
                "registration_authority": None,
                "route_key": None,
                "runtime_id": None,
                "components": None,
                "source_key": record.get("source_key"),
                "source_kind": None,
                "target_identity": None,
            }
        target = str(record.get("target_identity") or "")
        if not target:
            raise ResolutionError(
                f"{wave_id} proven record {record.get('source_key')} has no target"
            )
        runtime_id, components = identity_mod.project_target_identity(target)
        source_kind = identity_mod.source_kind_from_record(operand, record)
        if item == identity_mod.CIRCUIT_ITEM and isinstance(meta, int):
            overlay = {identity_mod.CIRCUIT_CONFIG: meta}
            if components and components != overlay:
                raise ResolutionError(
                    f"{wave_id} circuit component mismatch {components} vs {overlay}"
                )
            components = overlay
            runtime_id = "cruciblecraft:programmed_circuit"
        existing = _existing_runtime(operand)
        if existing and existing != runtime_id:
            raise ResolutionError(
                f"{wave_id} runtime mismatch {existing} vs ledger {runtime_id} "
                f"({record.get('source_key')})"
            )
        return {
            "class": str(record.get("mapping_class") or "exact_item"),
            "disposition": "proven_equivalent",
            "evidence": ",".join(record.get("evidence") or []),
            "registration_authority": "cruciblecraft",
            "route_key": runtime_id,
            "runtime_id": runtime_id,
            "components": components,
            "source_key": record.get("source_key"),
            "source_kind": source_kind,
            "target_identity": target,
        }
    return _legacy_resolve(operand, runtime_map=runtime_map, catalogs=catalogs)


def bind_resolved_operand(
    operand: dict[str, Any],
    resolved: dict[str, Any],
) -> dict[str, Any]:
    bound = dict(operand)
    bound["runtime_id"] = resolved["runtime_id"]
    if resolved.get("components"):
        bound["_components"] = dict(resolved["components"])
    bound["_source_kind"] = resolved.get("source_kind")
    bound["_source_key"] = resolved.get("source_key")
    return bound


def resolve_relation_operands(
    relation: dict[str, Any],
    *,
    wave_id: str,
    index: dict[str, dict[str, dict[str, Any]]] | None = None,
) -> tuple[dict[str, Any], list[str]]:
    store = index if index is not None else identity_mod.index_ledger()
    bound = dict(relation)
    kinds: list[str] = []
    for field in ("item_inputs", "item_outputs", "fluid_inputs", "fluid_outputs"):
        resolved_ops: list[dict[str, Any]] = []
        for operand in relation.get(field) or []:
            result = resolve_operand(
                operand,
                wave_id=wave_id,
                index=store,
                require_proven=True,
            )
            resolved_ops.append(bind_resolved_operand(operand, result))
            kinds.append(str(result.get("source_kind") or "SOURCE_BACKED"))
        bound[field] = resolved_ops
    return bound, kinds
