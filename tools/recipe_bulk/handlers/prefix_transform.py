#!/usr/bin/python3
"""Same-material form transform that is proven by selected dump rows."""
from __future__ import annotations

from typing import Any


def matches(relation: dict[str, Any], template: dict[str, Any]) -> bool:
    inputs = list(relation.get("item_inputs") or [])
    outputs = list(relation.get("item_outputs") or [])
    if len(inputs) != int(template.get("item_input_count") or 1):
        return False
    if len(outputs) != int(template.get("item_output_count") or 1):
        return False
    incoming = inputs[0]
    outgoing = outputs[0]
    material = incoming.get("material")
    from_form = incoming.get("form")
    to_form = outgoing.get("form")
    if not material or not from_form or not to_form:
        return False
    if outgoing.get("material") != material:
        return False
    if from_form == to_form:
        return False
    allowed = {
        (str(row.get("from_form")), str(row.get("to_form")))
        for row in template.get("transforms") or []
    }
    if allowed and (str(from_form), str(to_form)) not in allowed:
        return False
    if template.get("eut") is not None and int(relation.get("eut") or 0) != int(
        template["eut"]
    ):
        return False
    return True


def expand(
    handler: dict[str, Any], dump_relations: list[dict[str, Any]]
) -> list[dict[str, Any]]:
    template = handler.get("template") or {}
    return [row for row in dump_relations if matches(row, template)]
