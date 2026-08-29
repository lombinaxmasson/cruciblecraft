#!/usr/bin/env python3
"""Emit CompactGTRecipeFamilyDefinition JSON from resolved source relations."""
from __future__ import annotations

from typing import Any

SOURCE_REVISION_DEFAULT = "3703e40308c8c030763fd6297dea8b210d2a77b1"


def family_filename(template_key: str) -> str:
    return template_key.replace(".", "_").replace("#", "_") + ".json"


def emit_action(action: dict[str, Any]) -> dict[str, Any]:
    kind = str(action.get("kind") or "consume").lower()
    emitted: dict[str, Any] = {"kind": kind}
    if kind == "wear":
        emitted["damage"] = int(action.get("damage") or 0)
    return emitted


def emit_item(operand: dict[str, Any]) -> dict[str, Any]:
    runtime = str(operand.get("runtime_id") or "")
    if not runtime:
        raise ValueError(f"missing runtime_id: {operand}")
    return {"item": runtime}


def emit_item_output(operand: dict[str, Any]) -> dict[str, Any]:
    runtime = str(operand.get("runtime_id") or "")
    count = int((operand.get("source") or {}).get("count") or operand.get("count") or 1)
    return {"count": count, "id": runtime}


def emit_fluid(operand: dict[str, Any]) -> dict[str, Any]:
    runtime = str(operand.get("runtime_id") or "")
    amount = int((operand.get("source") or {}).get("amount") or operand.get("amount") or 0)
    if amount <= 0:
        raise ValueError(f"fluid amount must be positive: {operand}")
    return {"amount": amount, "id": runtime}


def emit_family(
    relation: dict[str, Any],
    lock_row: dict[str, Any],
    *,
    target_map: str,
    publication_group: str,
    source_revision: str = SOURCE_REVISION_DEFAULT,
) -> dict[str, Any]:
    if relation.get("parameterized"):
        raise ValueError("V1 compiler must not emit runtime parameterized fields")
    template_key = str(lock_row.get("template_key") or relation.get("template_key"))
    kinds = set((relation.get("provenance") or {}).get("kinds") or [])
    source_kind = (
        "DESIGN_POLICY"
        if "DESIGN_POLICY" in kinds
        else "SOURCE_DERIVED"
        if "SOURCE_DERIVED" in kinds
        else "SOURCE_BACKED"
    )
    stable_id = str(lock_row["stable_id"])
    return {
        "type": "cruciblecraft:compact_gt_recipe_family",
        "family_id": template_key,
        "target_map": target_map,
        "source_revision": relation.get("source_revision") or source_revision,
        "publication_group": publication_group,
        "relations": [
            {
                "stable_id": stable_id,
                "item_inputs": [emit_item(op) for op in relation.get("item_inputs") or []],
                "item_input_counts": [int(v) for v in relation.get("item_input_counts") or []],
                "item_input_actions": [
                    emit_action(action) for action in relation.get("item_input_actions") or []
                ],
                "item_outputs": [
                    emit_item_output(op) for op in relation.get("item_outputs") or []
                ],
                "fluid_inputs": [emit_fluid(op) for op in relation.get("fluid_inputs") or []],
                "fluid_outputs": [emit_fluid(op) for op in relation.get("fluid_outputs") or []],
                "output_chances": [int(v) for v in relation.get("output_chances") or []],
                "duration": int(relation["duration"]),
                "eut": int(relation.get("eut") or 0),
                "special_value": int(relation.get("special_value") or 0),
                "can_be_buffered": bool(relation.get("can_be_buffered", True)),
                "shadow_order": int(relation.get("shadow_order") or 0),
                "provenance": {
                    "source_kind": source_kind,
                    "selected_source_recipe": template_key,
                    "evidence_hashes": [relation["source_row_sha256"]]
                    if relation.get("source_row_sha256")
                    else list((relation.get("provenance") or {}).get("evidence_hashes") or []),
                },
            }
        ],
    }


def semantic_replay_key(document: dict[str, Any]) -> dict[str, Any]:
    relation = (document.get("relations") or [{}])[0]
    return {
        "family_id": document.get("family_id"),
        "publication_group": document.get("publication_group"),
        "relations": [
            {
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
        ],
        "target_map": document.get("target_map"),
    }
