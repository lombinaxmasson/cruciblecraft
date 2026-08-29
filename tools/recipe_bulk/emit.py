#!/usr/bin/env python3
"""Emit CompactGTRecipeFamilyDefinition JSON from resolved source relations."""
from __future__ import annotations

from typing import Any

from tools.recipe_bulk.models import WaveSpec

SOURCE_REVISION_DEFAULT = "3703e40308c8c030763fd6297dea8b210d2a77b1"
GT_PREFIXES = ("gregtech:", "gregapi:", "fixed:")


def family_filename(template_key: str) -> str:
    return template_key.replace(".", "_").replace("#", "_") + ".json"


def emit_action(action: dict[str, Any]) -> dict[str, Any]:
    kind = str(action.get("kind") or "consume").lower()
    emitted: dict[str, Any] = {"kind": kind}
    if kind == "wear":
        emitted["damage"] = int(action.get("damage") or 0)
    return emitted


def _runtime_id(operand: dict[str, Any]) -> str:
    runtime = str(operand.get("runtime_id") or "")
    if not runtime:
        raise ValueError(f"missing runtime_id: {operand}")
    if runtime.startswith(GT_PREFIXES):
        raise ValueError(f"refusing to emit GT runtime id: {runtime}")
    if not runtime.startswith(("minecraft:", "cruciblecraft:")):
        raise ValueError(f"runtime id is not a published vanilla/CC identity: {runtime}")
    return runtime


def emit_item(operand: dict[str, Any]) -> dict[str, Any]:
    runtime = _runtime_id(operand)
    components = operand.get("_components")
    if components:
        return {
            "type": "neoforge:components",
            "items": runtime,
            "components": dict(components),
        }
    return {"item": runtime}


def emit_item_output(operand: dict[str, Any]) -> dict[str, Any]:
    runtime = _runtime_id(operand)
    count = int((operand.get("source") or {}).get("count") or operand.get("count") or 1)
    return {"count": count, "id": runtime}


def emit_fluid(operand: dict[str, Any]) -> dict[str, Any]:
    runtime = _runtime_id(operand)
    amount = int((operand.get("source") or {}).get("amount") or operand.get("amount") or 0)
    if amount <= 0:
        raise ValueError(f"fluid amount must be positive: {operand}")
    return {"amount": amount, "id": runtime}


def hex_stable_id(source_stable_id: str, prefix: str) -> str:
    hex16 = source_stable_id.rsplit("/", 1)[-1]
    if len(hex16) != 16 or any(char not in "0123456789abcdef" for char in hex16):
        raise ValueError(f"Frozen stable_id suffix is not 16 hex chars: {source_stable_id}")
    return f"cruciblecraft:{prefix}/{hex16}"


def relation_source_kind(
    spec: WaveSpec | None,
    relation: dict[str, Any],
    operand_classes: list[str],
) -> str:
    policy = spec.source_kind_policy if spec is not None else "relation_provenance"
    if policy == "relation_provenance":
        kinds = set((relation.get("provenance") or {}).get("kinds") or [])
        if "DESIGN_POLICY" in kinds:
            return "DESIGN_POLICY"
        if "SOURCE_DERIVED" in kinds:
            return "SOURCE_DERIVED"
        return "SOURCE_BACKED"
    if policy == "operand_design_first":
        if "DESIGN_POLICY" in operand_classes:
            return "DESIGN_POLICY"
        if "SOURCE_DERIVED" in operand_classes:
            return "SOURCE_DERIVED"
        return "SOURCE_BACKED"
    if "SOURCE_DERIVED" in operand_classes:
        return "SOURCE_DERIVED"
    return "SOURCE_BACKED"


def stable_id_for(
    spec: WaveSpec | None,
    relation: dict[str, Any],
    lock_row: dict[str, Any] | None,
) -> str:
    if spec is None or spec.stable_id_policy == "lock":
        if lock_row is None or not lock_row.get("stable_id"):
            raise ValueError("lock stable_id policy requires a lock row")
        return str(lock_row["stable_id"])
    if spec.stable_id_policy == "hex_suffix":
        if not spec.stable_id_prefix:
            raise ValueError(f"{spec.wave_id} hex_suffix stable ids require a prefix")
        return hex_stable_id(str(relation["stable_id"]), spec.stable_id_prefix)
    return str(relation["stable_id"])


def emit_resolved_relation(
    relation: dict[str, Any],
    *,
    spec: WaveSpec | None = None,
    lock_row: dict[str, Any] | None = None,
    operand_classes: list[str] | None = None,
    template_key: str | None = None,
) -> dict[str, Any]:
    if relation.get("parameterized"):
        raise ValueError("V1 compiler must not emit runtime parameterized fields")
    key = str(
        template_key
        or (lock_row or {}).get("template_key")
        or relation.get("template_key")
    )
    item_inputs = [emit_item(op) for op in relation.get("item_inputs") or []]
    item_outputs = [emit_item_output(op) for op in relation.get("item_outputs") or []]
    fluid_inputs = [emit_fluid(op) for op in relation.get("fluid_inputs") or []]
    fluid_outputs = [emit_fluid(op) for op in relation.get("fluid_outputs") or []]
    counts = [int(v) for v in relation.get("item_input_counts") or []]
    actions = [emit_action(action) for action in relation.get("item_input_actions") or []]
    if item_inputs and not (len(item_inputs) == len(counts) == len(actions)):
        raise ValueError(f"{key} input arity drifted")
    for count, action in zip(counts, actions, strict=False):
        if action["kind"] == "consume" and count <= 0:
            raise ValueError(f"{key} consume counts must be positive")
        if action["kind"] in {"preserve", "wear"} and count != 0:
            raise ValueError(f"{key} preserve/wear counts must be zero")
    chances = [int(v) for v in relation.get("output_chances") or []]
    classes = operand_classes or [
        str(op.get("_source_kind") or "SOURCE_BACKED")
        for field in ("item_inputs", "item_outputs", "fluid_inputs", "fluid_outputs")
        for op in relation.get(field) or []
    ]
    evidence = (
        [relation["source_row_sha256"]]
        if relation.get("source_row_sha256")
        else list((relation.get("provenance") or {}).get("evidence_hashes") or [])
    )
    return {
        "stable_id": stable_id_for(spec, relation, lock_row),
        "item_inputs": item_inputs,
        "item_input_counts": counts,
        "item_input_actions": actions,
        "item_outputs": item_outputs,
        "fluid_inputs": fluid_inputs,
        "fluid_outputs": fluid_outputs,
        "output_chances": chances,
        "duration": int(relation["duration"]),
        "eut": int(relation.get("eut") or 0),
        "special_value": int(relation.get("special_value") or 0),
        "can_be_buffered": bool(relation.get("can_be_buffered", True)),
        "shadow_order": int(relation.get("shadow_order") or 0),
        "provenance": {
            "source_kind": relation_source_kind(spec, relation, classes),
            "selected_source_recipe": key,
            "evidence_hashes": evidence,
        },
    }


def emit_wave_document(
    spec: WaveSpec,
    *,
    template_key: str,
    source_rows: list[dict[str, Any]],
    resolved_rows: list[dict[str, Any]],
    lock_row: dict[str, Any] | None,
    target_map: str,
    publication_group: str | None,
    operand_classes: list[list[str]],
) -> dict[str, Any]:
    relations = [
        emit_resolved_relation(
            resolved,
            spec=spec,
            lock_row=lock_row,
            operand_classes=classes,
            template_key=template_key,
        )
        for resolved, classes in zip(resolved_rows, operand_classes, strict=True)
    ]
    source_revision = str(
        source_rows[0].get("source_revision") or SOURCE_REVISION_DEFAULT
    )
    document: dict[str, Any] = {
        "type": "cruciblecraft:compact_gt_recipe_family",
        "family_id": template_key,
        "target_map": target_map,
        "source_revision": source_revision,
        "relations": relations,
    }
    if publication_group:
        document["publication_group"] = publication_group
    return document


def emit_family(
    relation: dict[str, Any],
    lock_row: dict[str, Any],
    *,
    target_map: str,
    publication_group: str,
    source_revision: str = SOURCE_REVISION_DEFAULT,
) -> dict[str, Any]:
    emitted = emit_resolved_relation(relation, lock_row=lock_row)
    template_key = str(lock_row.get("template_key") or relation.get("template_key"))
    return {
        "type": "cruciblecraft:compact_gt_recipe_family",
        "family_id": template_key,
        "target_map": target_map,
        "source_revision": relation.get("source_revision") or source_revision,
        "publication_group": publication_group,
        "relations": [emitted],
    }


def semantic_relation_key(relation: dict[str, Any]) -> dict[str, Any]:
    return {
        "can_be_buffered": relation.get("can_be_buffered"),
        "duration": relation.get("duration"),
        "eut": relation.get("eut"),
        "fluid_inputs": relation.get("fluid_inputs"),
        "fluid_outputs": relation.get("fluid_outputs"),
        "item_input_actions": relation.get("item_input_actions"),
        "item_input_counts": relation.get("item_input_counts"),
        "item_inputs": relation.get("item_inputs"),
        "item_outputs": relation.get("item_outputs"),
        "output_chances": relation.get("output_chances"),
        "shadow_order": relation.get("shadow_order"),
        "special_value": relation.get("special_value"),
    }


def semantic_replay_key(document: dict[str, Any]) -> dict[str, Any]:
    relations = document.get("relations") or []
    return {
        "family_id": document.get("family_id"),
        "publication_group": document.get("publication_group"),
        "relations": [semantic_relation_key(relation) for relation in relations],
        "target_map": document.get("target_map"),
    }
