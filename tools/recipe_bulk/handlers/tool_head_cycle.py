#!/usr/bin/python3
"""Raw tool-head to finished tool-head cycle proven by selected dump rows."""
from __future__ import annotations

from typing import Any


def _runtime(operand: dict[str, Any]) -> str:
    return str(operand.get("runtime_id") or operand.get("value") or "")


def matches(relation: dict[str, Any], template: dict[str, Any] | None = None) -> bool:
    del template
    inputs = [_runtime(item) for item in relation.get("item_inputs") or []]
    outputs = [_runtime(item) for item in relation.get("item_outputs") or []]
    if len(inputs) != 1:
        return False
    if "/tool_head_raw_" not in inputs[0]:
        return False
    return any(
        "/tool_head_" in output and "/tool_head_raw_" not in output
        for output in outputs
    )


def expand(
    handler: dict[str, Any], dump_relations: list[dict[str, Any]]
) -> list[dict[str, Any]]:
    template = handler.get("template") or {}
    return [row for row in dump_relations if matches(row, template)]
