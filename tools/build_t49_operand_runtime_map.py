#!/usr/bin/env python3
"""Emit the T49 operand runtime map from the frozen remainder source."""
from __future__ import annotations

import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import build_t49_identity_catalog as identity
from tools import wave_bath_tiny_purified as common

OUTPUT = common.OPERAND_RUNTIME_MAP


def _meta_token(meta: Any) -> str:
    if meta is None:
        return ""
    return str(meta)


def build() -> dict[str, Any]:
    source = common.load_json(common.SOURCE)
    if source.get("status") != "T49_BATH_SOURCE_FROZEN":
        raise ValueError("T49 operand map requires a frozen remainder source")
    lock_ids = identity.lock_family_ids()
    if lock_ids != set(common.LOCK_FAMILY_IDS):
        raise ValueError("T49 operand map lock set drifted")
    seen: dict[tuple[str, str, str, str], dict[str, Any]] = {}
    for relation in source.get("relations") or []:
        if str(relation.get("family_id") or "") not in lock_ids:
            continue
        for operand in list(relation.get("item_inputs") or []) + list(
            relation.get("item_outputs") or []
        ):
            source_row = operand.get("source") or {}
            item = str(source_row.get("item") or operand.get("runtime_id") or "")
            runtime = str(operand.get("runtime_id") or "")
            if not item or not runtime:
                continue
            common.assert_runtime_id(runtime, consume=False)
            seen[("item", item, _meta_token(source_row.get("meta")), runtime)] = {
                "class": operand.get("mapping") or "SOURCE_DERIVED",
                "reason": operand.get("mapping"),
                "runtime": {"id": runtime, "kind": "item"},
                "source": {
                    "count": source_row.get("count"),
                    "displayName": source_row.get("displayName"),
                    "item": item,
                    "meta": source_row.get("meta"),
                },
            }
        for operand in list(relation.get("fluid_inputs") or []) + list(
            relation.get("fluid_outputs") or []
        ):
            source_row = operand.get("source") or {}
            fluid = str(source_row.get("fluid") or operand.get("runtime_id") or "")
            runtime = str(operand.get("runtime_id") or "")
            if not fluid or not runtime:
                continue
            common.assert_runtime_id(runtime, consume=False)
            seen[("fluid", fluid, "", runtime)] = {
                "class": operand.get("mapping") or "SOURCE_DERIVED",
                "reason": operand.get("mapping"),
                "runtime": {"id": runtime, "kind": "fluid"},
                "source": {"amount": source_row.get("amount"), "fluid": fluid},
            }
    operands = [seen[key] for key in sorted(seen)]
    return {
        "generated_by": "python tools/build_t49_operand_runtime_map.py",
        "lock_family_count": len(lock_ids),
        "operand_count": len(operands),
        "operands": operands,
        "schema_version": 1,
        "status": "T49_OPERAND_RUNTIME_MAP",
    }


def main(argv: list[str] | None = None) -> int:
    return common.run_managed(
        "Freeze the T49 operand runtime map",
        OUTPUT,
        build=build,
        argv=argv,
    )


if __name__ == "__main__":
    raise SystemExit(main())
