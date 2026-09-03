#!/usr/bin/python3
"""GT6 dump-shaped dialect: one adapter owned by the GT6 source system."""
from __future__ import annotations

import hashlib
from pathlib import Path
from typing import Any

from tools import t35_common as t35

ADAPTER_ID = "gt6"
ADAPTER_ABI = "gt6-canonical-v1"
ADAPTER_PATH = Path(__file__).resolve()

FINGERPRINT_EXCLUDE = frozenset(
    {
        "source_recipe_index",
        "source_row_sha256",
        "stable_id",
        "slot_notes",
        "unsupported_semantics",
        "shadow_order",
        "publication_group",
        "host",
        "owner",
        "cohort",
        "provenance",
    }
)


class DialectError(ValueError):
    """GT6 dialect failed a fail-closed mapping."""


def adapter_sha256() -> str:
    return hashlib.sha256(ADAPTER_PATH.read_bytes()).hexdigest()


def _maps() -> dict[str, Any]:
    from tools.recipe_bulk import ordinary_source as ordinary

    return {
        "stone": ordinary.load_stone_runtime(),
        "mte": ordinary.load_mte_runtime(),
        "block": ordinary.load_block_runtime(),
        "items": ordinary.load_item_overlay(),
        "aliases": ordinary.identities.load_reused_aliases(),
        "fluids": ordinary.load_fluid_overlay(),
        "catalogs": ordinary.load_catalogs(),
    }


def compile_row(
    recipe: dict[str, Any],
    *,
    host: str,
    target_map: str,
    source_map: str,
    family_id: str,
    template_key: str,
    recipe_index: int,
    shadow_order: int,
    source_revision: str,
    source_row_sha256: str,
    maps: dict[str, Any] | None = None,
) -> tuple[dict[str, Any], list[str]]:
    from tools import build_t37_assembler_source as t37
    from tools.recipe_bulk import ordinary_source as ordinary

    loaded = maps or _maps()
    catalogs = loaded["catalogs"]
    errors: list[str] = []
    unsupported: list[str] = []
    slot_notes: list[dict[str, Any]] = []
    extra_keys = sorted(set(recipe) - t37.CONSUMED_RECIPE_KEYS)
    if extra_keys:
        raise DialectError(
            f"{template_key}: unknown dump field {extra_keys[0]}"
        )
    if recipe.get("hidden") is True:
        unsupported.append("hidden")
    if recipe.get("fake") is True:
        unsupported.append("fake")
    item_inputs: list[dict[str, Any]] = []
    item_input_counts: list[int] = []
    item_input_actions: list[dict[str, Any]] = []
    for slot, item in enumerate(recipe.get("inputs") or []):
        if t37._empty_item(item):
            slot_notes.append({"side": "item_input", "slot": slot, "class": "empty_slot"})
            continue
        operand, operand_errors = ordinary.map_item_operand(
            item,
            catalogs,
            stone_runtime=loaded["stone"],
            mte_runtime=loaded["mte"],
            block_runtime=loaded["block"],
            item_overlay=loaded["items"],
            reused_aliases=loaded["aliases"],
            side="item_input",
        )
        errors.extend(operand_errors)
        action = t37.classify_item_action(item)
        count = int(item.get("count") or 0)
        if action["kind"] != "CONSUME":
            count = 0
        if action["kind"] == "PRESERVE":
            operand["slot_class"] = "catalyst"
        item_inputs.append(operand)
        item_input_counts.append(count)
        item_input_actions.append(action)
    item_outputs: list[dict[str, Any]] = []
    for slot, item in enumerate(recipe.get("outputs") or []):
        if t37._empty_item(item):
            slot_notes.append({"side": "item_output", "slot": slot, "class": "empty_slot"})
            continue
        operand, operand_errors = ordinary.map_item_operand(
            item,
            catalogs,
            stone_runtime=loaded["stone"],
            mte_runtime=loaded["mte"],
            block_runtime=loaded["block"],
            item_overlay=loaded["items"],
            reused_aliases=loaded["aliases"],
            side="item_output",
        )
        errors.extend(operand_errors)
        item_outputs.append(operand)
    fluid_inputs: list[dict[str, Any]] = []
    for fluid in recipe.get("fluidInputs") or []:
        if not fluid:
            continue
        operand, operand_errors = ordinary.map_fluid_operand(
            fluid, catalogs, loaded["fluids"], side="fluid_input"
        )
        errors.extend(operand_errors)
        if operand.get("slot_class") != "empty":
            fluid_inputs.append(operand)
    fluid_outputs: list[dict[str, Any]] = []
    for fluid in recipe.get("fluidOutputs") or []:
        if not fluid:
            continue
        operand, operand_errors = ordinary.map_fluid_operand(
            fluid, catalogs, loaded["fluids"], side="fluid_output"
        )
        errors.extend(operand_errors)
        if operand.get("slot_class") != "empty":
            fluid_outputs.append(operand)
    chances = [int(value) for value in (recipe.get("chances") or [])]
    if not chances and item_outputs:
        chances = [10000] * len(item_outputs)
    elif chances and len(chances) < len(item_outputs):
        chances.extend([10000] * (len(item_outputs) - len(chances)))
    duration = int(recipe.get("duration") or 0)
    if duration <= 0:
        errors.append(f"{template_key}: duration must be positive")
    relation = {
        "family_id": family_id,
        "host": host,
        "source_map": source_map,
        "source_recipe_index": recipe_index,
        "source_revision": source_revision,
        "source_row_sha256": source_row_sha256,
        "target_map": target_map,
        "template_key": template_key,
        "item_inputs": item_inputs,
        "item_input_counts": item_input_counts,
        "item_input_actions": item_input_actions,
        "item_outputs": item_outputs,
        "fluid_inputs": fluid_inputs,
        "fluid_outputs": fluid_outputs,
        "output_chances": chances,
        "duration": duration,
        "eut": int(recipe.get("euPerTick") or 0),
        "special_value": int(recipe.get("specialValue") or 0),
        "can_be_buffered": bool(recipe.get("canBeBuffered", True)),
        "needs_empty_output": bool(recipe.get("needsEmptyOutput")),
        "no_nbt_checks": True,
        "shadow_order": shadow_order,
        "slot_notes": slot_notes,
        "unsupported_semantics": unsupported,
        "provenance": {
            "kinds": ["SOURCE_BACKED", "SOURCE_DERIVED"],
            "fields": {
                "item_inputs": "SOURCE_DERIVED",
                "item_input_counts": "SOURCE_BACKED",
                "duration": "SOURCE_BACKED",
                "eut": "SOURCE_BACKED",
                "special_value": "SOURCE_BACKED",
                "can_be_buffered": "SOURCE_BACKED",
            },
        },
    }
    return relation, errors


def fingerprint_payload(relation: dict[str, Any]) -> dict[str, Any]:
    return {
        key: value
        for key, value in relation.items()
        if key not in FINGERPRINT_EXCLUDE
    }


def stable_id_for(
    *,
    source_system: str,
    source_revision: str,
    family_id: str,
    relation: dict[str, Any],
) -> str:
    payload = {
        "family_id": family_id,
        "fingerprint": fingerprint_payload(relation),
        "source_revision": source_revision,
        "source_system": source_system,
    }
    digest = hashlib.sha256(t35.stable_json(payload).encode("utf-8")).hexdigest()[:16]
    return f"cruciblecraft:gt6/{digest}"


def semantic_payload(relation: dict[str, Any]) -> dict[str, Any]:
    return {
        "can_be_buffered": relation.get("can_be_buffered"),
        "duration": relation.get("duration"),
        "eut": relation.get("eut"),
        "family_id": relation.get("family_id"),
        "fluid_inputs": _operand_identity(relation.get("fluid_inputs") or []),
        "fluid_outputs": _operand_identity(relation.get("fluid_outputs") or []),
        "item_input_actions": relation.get("item_input_actions") or [],
        "item_input_counts": relation.get("item_input_counts") or [],
        "item_inputs": _operand_identity(relation.get("item_inputs") or []),
        "item_outputs": _operand_identity(relation.get("item_outputs") or []),
        "output_chances": relation.get("output_chances") or [],
        "shadow_order": relation.get("shadow_order"),
        "source_map": relation.get("source_map"),
        "source_row_sha256": relation.get("source_row_sha256"),
        "special_value": relation.get("special_value"),
        "template_key": relation.get("template_key"),
    }


def _operand_identity(operands: list[dict[str, Any]]) -> list[dict[str, Any]]:
    rows: list[dict[str, Any]] = []
    for operand in operands:
        source = operand.get("source") or {}
        rows.append(
            {
                "fluid": source.get("fluid"),
                "item": source.get("item"),
                "mapping": operand.get("mapping"),
                "meta": source.get("meta"),
                "runtime_id": operand.get("runtime_id"),
                "slot_class": operand.get("slot_class"),
                "value": operand.get("value"),
            }
        )
    return rows


def dump_row_from_relation(relation: dict[str, Any]) -> dict[str, Any]:
    inputs: list[dict[str, Any]] = []
    not_consumed: list[bool] = []
    actions = list(relation.get("item_input_actions") or [])
    counts = list(relation.get("item_input_counts") or [])
    for index, operand in enumerate(relation.get("item_inputs") or []):
        source = dict(operand.get("source") or {})
        action = actions[index] if index < len(actions) else {"kind": "CONSUME"}
        kind = str(action.get("kind") or "CONSUME")
        count = int(counts[index]) if index < len(counts) else int(source.get("count") or 0)
        if kind != "CONSUME":
            source["count"] = 0
            source["notConsumed"] = True
            not_consumed.append(True)
        else:
            source["count"] = count
            source["notConsumed"] = False
            not_consumed.append(False)
        if kind == "WEAR" and action.get("damage"):
            source["damage"] = int(action["damage"])
        inputs.append(source)
    return {
        "canBeBuffered": bool(relation.get("can_be_buffered", True)),
        "chances": list(relation.get("output_chances") or []),
        "duration": int(relation.get("duration") or 0),
        "enabled": True,
        "euPerTick": int(relation.get("eut") or 0),
        "fake": False,
        "fluidInputs": [dict(op.get("source") or {}) for op in relation.get("fluid_inputs") or []],
        "fluidOutputs": [
            dict(op.get("source") or {}) for op in relation.get("fluid_outputs") or []
        ],
        "hidden": False,
        "inputs": inputs,
        "maxChances": list(relation.get("output_chances") or []),
        "needsEmptyOutput": bool(relation.get("needs_empty_output")),
        "noNbtChecks": True,
        "notConsumed": not_consumed,
        "outputs": [dict(op.get("source") or {}) for op in relation.get("item_outputs") or []],
        "specialValue": int(relation.get("special_value") or 0),
    }
