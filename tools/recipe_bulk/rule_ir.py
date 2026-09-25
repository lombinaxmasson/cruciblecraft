#!/usr/bin/python3
"""Dump-proven Rule IR: handlers plus explicit remainder, no fuzzy tags."""
from __future__ import annotations

from pathlib import Path
from typing import Any

from tools import census_common as census
from tools.recipe_bulk.handlers import exact_remainder, prefix_transform, shape_transform, tool_head_cycle
from tools.recipe_bulk.models import WaveSpec
from tools.recipe_bulk.schema_lite import validate

SCHEMA_PATH = census.TOOLS / "recipe_rule_ir.schema.json"
HANDLERS = {
    "exact_remainder": exact_remainder,
    "prefix_transform": prefix_transform,
    "shape_transform": shape_transform,
    "tool_head_cycle": tool_head_cycle,
}


def load_rule_ir(path: Path) -> dict[str, Any]:
    document = census.load_json(path)
    validate(document, census.load_json(SCHEMA_PATH))
    return document


def row_fingerprint(relation: dict[str, Any]) -> tuple[Any, ...]:
    return (
        str(relation.get("stable_id") or ""),
        str(relation.get("source_row_sha256") or ""),
        int(relation.get("duration") or 0),
        int(relation.get("eut") or 0),
        tuple(
            str(action.get("kind") or "")
            for action in relation.get("item_input_actions") or []
        ),
        tuple(_runtime(item) for item in relation.get("item_inputs") or []),
        tuple(_runtime(item) for item in relation.get("item_outputs") or []),
        str(((relation.get("provenance") or {}).get("kinds") or [""])[0]),
    )


def expand(document: dict[str, Any], dump_relations: list[dict[str, Any]]) -> list[dict[str, Any]]:
    claimed: dict[str, str] = {}
    expanded: list[dict[str, Any]] = []
    for handler in document.get("handlers") or []:
        kind = str(handler.get("kind") or "")
        module = HANDLERS.get(kind)
        if module is None:
            raise ValueError(f"unknown Rule IR handler kind {kind}")
        rows = module.expand(handler, dump_relations)
        handler_id = str(handler.get("handler_id") or kind)
        for row in rows:
            digest = str(row.get("source_row_sha256") or "")
            previous = claimed.get(digest)
            if previous:
                raise ValueError(
                    f"Rule IR overlap on {digest}: {previous} vs {handler_id}"
                )
            claimed[digest] = handler_id
            expanded.append(row)
    return expanded


def prove_coverage(
    document: dict[str, Any],
    dump_relations: list[dict[str, Any]],
) -> dict[str, Any]:
    expanded = expand(document, dump_relations)
    blocked = {str(digest) for digest in document.get("blocked") or []}
    dump_digests = [str(row.get("source_row_sha256") or "") for row in dump_relations]
    expanded_digests = [str(row.get("source_row_sha256") or "") for row in expanded]
    if any(digest in blocked for digest in expanded_digests):
        raise ValueError("Rule IR expansion collided with blocked ledger rows")
    dump_set = set(dump_digests)
    expanded_set = set(expanded_digests)
    if dump_set != expanded_set:
        missing = sorted(dump_set - expanded_set)
        extra = sorted(expanded_set - dump_set)
        raise ValueError(
            "Rule IR coverage drifted: missing "
            f"{len(missing)} extra {len(extra)}"
        )
    dump_keys = [row_fingerprint(row) for row in dump_relations]
    expanded_by_digest = {
        str(row.get("source_row_sha256") or ""): row_fingerprint(row)
        for row in expanded
    }
    for row in dump_relations:
        digest = str(row.get("source_row_sha256") or "")
        if expanded_by_digest[digest] != row_fingerprint(row):
            raise ValueError(f"Rule IR row drifted from dump: {digest}")
    return {
        "blocked": len(blocked),
        "covered": len(expanded),
        "dump_row_count": len(dump_relations),
        "handlers": {
            str(handler.get("handler_id")): len(
                HANDLERS[str(handler.get("kind"))].expand(handler, dump_relations)
            )
            for handler in document.get("handlers") or []
        },
        "ok": True,
        "row_fingerprints": len(dump_keys),
    }


def prove_and_select(
    spec: WaveSpec,
    relations: list[dict[str, Any]],
    lock_rows: dict[str, dict[str, Any]],
) -> tuple[list[dict[str, Any]], dict[str, dict[str, Any]]]:
    if spec.rule_ir_path is None or not spec.rule_ir_path.is_file():
        raise ValueError(f"{spec.wave_id} compile_authority rule_ir_v1 missing rule_ir.json")
    document = load_rule_ir(spec.rule_ir_path)
    prove_coverage(document, relations)
    expanded = expand(document, relations)
    by_digest = {str(row.get("source_row_sha256") or ""): row for row in relations}
    selected = [
        by_digest[str(row.get("source_row_sha256") or "")] for row in expanded
    ]
    selected_order = {
        str(row.get("source_row_sha256") or ""): index
        for index, row in enumerate(relations)
    }
    selected.sort(
        key=lambda row: selected_order[str(row.get("source_row_sha256") or "")]
    )
    if [row_fingerprint(row) for row in selected] != [
        row_fingerprint(row) for row in relations
    ]:
        raise ValueError(f"{spec.wave_id} Rule IR selected rows drifted from dump order")
    return selected, lock_rows


def build_sanding_rule_ir(
    relations: list[dict[str, Any]],
    *,
    family_id: str,
    source_map: str,
    target_map: str,
    source_revision: str,
    blocked: list[str] | None = None,
) -> dict[str, Any]:
    tool_head = [
        row for row in relations if tool_head_cycle.matches(row, {})
    ]
    prefix = [
        row
        for row in relations
        if prefix_transform.matches(
            row,
            {
                "eut": 16,
                "item_input_count": 1,
                "item_output_count": 1,
                "transforms": [{"from_form": "nugget", "to_form": "round"}],
            },
        )
        and not tool_head_cycle.matches(row, {})
    ]
    covered = {
        str(row.get("source_row_sha256") or "") for row in tool_head
    } | {str(row.get("source_row_sha256") or "") for row in prefix}
    remainder = [
        str(row.get("source_row_sha256") or "")
        for row in relations
        if str(row.get("source_row_sha256") or "") not in covered
    ]
    document = {
        "blocked": list(blocked or []),
        "family_id": family_id,
        "handlers": [
            {
                "handler_id": "sanding_tool_head_cycle",
                "kind": "tool_head_cycle",
                "template": {
                    "item_input_count": 1,
                    "item_input_prefix": "tool_head_raw_",
                    "item_output_prefix": "tool_head_",
                },
            },
            {
                "handler_id": "sanding_prefix_transform",
                "kind": "prefix_transform",
                "template": {
                    "eut": 16,
                    "item_input_count": 1,
                    "item_output_count": 1,
                    "transforms": [{"from_form": "nugget", "to_form": "round"}],
                },
            },
            {
                "covered_source_row_sha256": remainder,
                "handler_id": "sanding_exact_remainder",
                "kind": "exact_remainder",
            },
        ],
        "schema": "rule_ir_v1",
        "schema_version": 1,
        "source_map": source_map,
        "source_revision": source_revision,
        "target_map": target_map,
    }
    prove_coverage(document, relations)
    return document


def _runtime(operand: dict[str, Any]) -> str:
    return str(operand.get("runtime_id") or operand.get("value") or "")
