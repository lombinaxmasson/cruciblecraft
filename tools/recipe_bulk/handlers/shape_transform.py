#!/usr/bin/python3
"""Same-material extrude: one consumed form plus one preserved shape."""
from __future__ import annotations

from typing import Any

SHAPE_PREFIX = "cruciblecraft:extruder_shape_"


def matches(relation: dict[str, Any], template: dict[str, Any]) -> bool:
    if relation.get("fluid_inputs") or relation.get("fluid_outputs"):
        return False
    inputs = list(relation.get("item_inputs") or [])
    actions = list(relation.get("item_input_actions") or [])
    outputs = list(relation.get("item_outputs") or [])
    if len(inputs) != 2 or len(actions) != 2 or not outputs:
        return False
    consumed = [
        item
        for item, action in zip(inputs, actions)
        if str(action.get("kind") or "") == "CONSUME"
    ]
    preserved = [
        item
        for item, action in zip(inputs, actions)
        if str(action.get("kind") or "") == "PRESERVE"
    ]
    if len(consumed) != 1 or len(preserved) != 1:
        return False
    material = consumed[0].get("material")
    from_form = consumed[0].get("form")
    shape = str(preserved[0].get("runtime_id") or preserved[0].get("value") or "")
    if not material or not from_form or not shape.startswith(SHAPE_PREFIX):
        return False
    for output in outputs:
        if output.get("material") != material or not output.get("form"):
            return False
        if output.get("form") == from_form:
            return False
    if template.get("eut") is not None and int(relation.get("eut") or 0) != int(template["eut"]):
        return False
    if template.get("duration") is not None and int(relation.get("duration") or 0) != int(
        template["duration"]
    ):
        return False
    return True


def expand(
    handler: dict[str, Any], dump_relations: list[dict[str, Any]]
) -> list[dict[str, Any]]:
    template = handler.get("template") or {}
    return [row for row in dump_relations if matches(row, template)]
