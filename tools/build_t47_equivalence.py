#!/usr/bin/env python3
"""Bind T47 source pack rows to generated compact families field-by-field."""
from __future__ import annotations

import hashlib
import json
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35
from tools import t47_common as common

OUTPUT = common.EQUIVALENCE
LOCKED_FIELDS = (
    "stable_id",
    "shadow_order",
    "duration",
    "eut",
    "special_value",
    "can_be_buffered",
    "item_inputs",
    "item_input_counts",
    "item_input_actions",
    "item_outputs",
    "fluid_inputs",
    "fluid_outputs",
    "output_chances",
    "source_row_sha256",
)


def _stable(value: Any) -> str:
    return json.dumps(value, ensure_ascii=False, indent=2, sort_keys=True) + "\n"


def _digest_text(text: str) -> str:
    return hashlib.sha256(text.encode("utf-8")).hexdigest()


def _item_id(stack: dict[str, Any]) -> str:
    return str(stack.get("item") or stack.get("id") or stack.get("items") or "")


def _action_kind(action: dict[str, Any]) -> str:
    return str(action.get("kind") or "").lower()


def _generated_view(relation: dict[str, Any]) -> dict[str, Any]:
    provenance = relation.get("provenance") or {}
    hashes = list(provenance.get("evidence_hashes") or [])
    source_row = str(relation.get("source_row_sha256") or (hashes[0] if hashes else ""))
    return {
        "stable_id": str(relation["stable_id"]),
        "shadow_order": int(relation.get("shadow_order") or 0),
        "duration": int(relation["duration"]),
        "eut": int(relation.get("eut") or 0),
        "special_value": int(relation.get("special_value") or 0),
        "can_be_buffered": bool(relation.get("can_be_buffered")),
        "item_inputs": [
            {"id": _item_id(stack), "count": int(count or 0), "action": _action_kind(action)}
            for stack, count, action in zip(
                relation.get("item_inputs") or [],
                relation.get("item_input_counts") or [],
                relation.get("item_input_actions") or [],
                strict=False,
            )
        ],
        "item_outputs": [
            {"id": str(stack.get("id") or ""), "count": int(stack.get("count") or 0)}
            for stack in relation.get("item_outputs") or []
        ],
        "fluid_inputs": [
            {"id": str(stack.get("id") or ""), "amount": int(stack.get("amount") or 0)}
            for stack in relation.get("fluid_inputs") or []
        ],
        "fluid_outputs": [
            {"id": str(stack.get("id") or ""), "amount": int(stack.get("amount") or 0)}
            for stack in relation.get("fluid_outputs") or []
        ],
        "output_chances": [int(value) for value in relation.get("output_chances") or []],
        "source_row_sha256": source_row,
    }


def _source_view(relation: dict[str, Any]) -> dict[str, Any]:
    return {
        "stable_id": str(relation["stable_id"]),
        "shadow_order": int(relation.get("shadow_order") or 0),
        "duration": int(relation["duration"]),
        "eut": int(relation.get("eut") or 0),
        "special_value": int(relation.get("special_value") or 0),
        "can_be_buffered": bool(relation.get("can_be_buffered")),
        "item_inputs": [
            {
                "id": str(stack.get("runtime_id") or ""),
                "count": int(count or 0),
                "action": _action_kind(action),
            }
            for stack, count, action in zip(
                relation.get("item_inputs") or [],
                relation.get("item_input_counts") or [],
                relation.get("item_input_actions") or [],
                strict=False,
            )
        ],
        "item_outputs": [
            {
                "id": str(stack.get("runtime_id") or ""),
                "count": int((stack.get("source") or {}).get("count") or 0),
            }
            for stack in relation.get("item_outputs") or []
        ],
        "fluid_inputs": [
            {
                "id": str(stack.get("runtime_id") or ""),
                "amount": int((stack.get("source") or {}).get("amount") or 0),
            }
            for stack in relation.get("fluid_inputs") or []
        ],
        "fluid_outputs": [
            {
                "id": str(stack.get("runtime_id") or ""),
                "amount": int((stack.get("source") or {}).get("amount") or 0),
            }
            for stack in relation.get("fluid_outputs") or []
        ],
        "output_chances": [int(value) for value in relation.get("output_chances") or []],
        "source_row_sha256": str(relation.get("source_row_sha256") or ""),
    }


def build() -> dict[str, Any]:
    files = common.generated_family_files()
    expected = common.production_family_count()
    if len(files) != expected:
        raise ValueError(f"T47 generated family count drifted: {len(files)} != {expected}")
    lock = common.load_production_lock()
    lock_ids = list((lock.get("production") or {}).get("stable_ids") or [])
    lock_id_set = set(lock_ids)
    generated_hashes: dict[str, str] = {}
    generated_by_id: dict[str, dict[str, Any]] = {}
    stable_ids: list[str] = []
    for path in files:
        content = path.read_text(encoding="utf-8")
        relative = path.relative_to(common.GENERATED_ROOT).as_posix()
        generated_hashes[relative] = _digest_text(content)
        document = json.loads(content)
        if document.get("parameterized"):
            raise ValueError(f"T47 authored JSON must not carry parameterized: {relative}")
        group_id = str(document.get("publication_group") or "")
        if group_id not in common.PUBLICATION_GROUPS:
            raise ValueError(f"T47 generated family used unexpected group {group_id}")
        for relation in document.get("relations") or []:
            stable_id = str(relation["stable_id"])
            stable_ids.append(stable_id)
            generated_by_id[stable_id] = _generated_view(relation)
    if sorted(stable_ids) != sorted(lock_ids):
        raise ValueError("T47 generated stable ids drifted from the production lock")
    source = common.load_json(common.SOURCE)
    rows: list[dict[str, Any]] = []
    mismatches: list[str] = []
    seen: set[str] = set()
    for relation in source.get("relations") or []:
        stable_id = str(relation["stable_id"])
        if stable_id not in lock_id_set:
            continue
        if stable_id in seen:
            raise ValueError(f"duplicate T47 source stable id {stable_id}")
        seen.add(stable_id)
        generated = generated_by_id.get(stable_id)
        if generated is None:
            mismatches.append(f"missing generated {stable_id}")
            continue
        source_view = _source_view(relation)
        if source_view != generated:
            mismatches.append(stable_id)
        rows.append({
            "stable_id": stable_id,
            "fields_sha256": _digest_text(_stable(generated)),
            "source_row_sha256": source_view["source_row_sha256"],
        })
    missing = sorted(lock_id_set - seen)
    if missing:
        mismatches.extend(f"missing source {stable_id}" for stable_id in missing[:8])
    if mismatches:
        raise ValueError(
            "T47 source→generated field mismatch: " + ", ".join(mismatches[:8])
        )
    if len(rows) != common.production_relation_count():
        raise ValueError("T47 equivalence relation count drifted")
    return {
        "generated": {
            "file_count": len(files),
            "files": generated_hashes,
            "root": common.relative(common.GENERATED_ROOT),
            "sha256": _digest_text(_stable(generated_hashes)),
        },
        "locked_fields": list(LOCKED_FIELDS),
        "mutation_sensitive": True,
        "parameterized": "absent_fail_closed",
        "relations": rows,
        "runtime_expected": {
            "logical_ids": len(stable_ids),
            "sha256": _digest_text(_stable(stable_ids)),
            "stable_ids": stable_ids,
        },
        "schema_version": 1,
        "source": {
            "family_count": expected,
            "path": common.relative(common.SOURCE),
            "relation_count": common.production_relation_count(),
            "scope": "production",
            "sha256": t35.sha256_file(common.SOURCE),
            "source_revision": common.SOURCE_REVISION,
        },
        "status": "T47_EQUIVALENCE",
    }


def main(argv: list[str] | None = None) -> int:
    return common.run_managed(
        "Bind T47 generated equivalence",
        OUTPUT,
        build=build,
        argv=argv,
    )


if __name__ == "__main__":
    raise SystemExit(main())
