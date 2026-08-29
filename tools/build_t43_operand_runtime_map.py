#!/usr/bin/env python3
"""Emit the T43 operand runtime map from the frozen smelter source."""
from __future__ import annotations

import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t43_common as common  # noqa: E402

OUTPUT = common.OPERAND_RUNTIME_MAP


def build() -> dict[str, Any]:
    source = common.load_json(common.SOURCE)
    seen: dict[tuple[str, str], dict[str, Any]] = {}
    for relation in source.get("relations") or []:
        for operand in list(relation.get("item_inputs") or []) + list(
            relation.get("item_outputs") or []
        ):
            source_row = operand.get("source") or {}
            item = str(source_row.get("item") or operand.get("runtime_id") or "")
            runtime = str(operand.get("runtime_id") or "")
            if not item or not runtime:
                continue
            key = (item, runtime)
            seen[key] = {
                "class": operand.get("mapping") or operand.get("t43_class") or "SOURCE_DERIVED",
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
            key = (fluid, runtime)
            seen[key] = {
                "class": operand.get("mapping") or "SOURCE_DERIVED",
                "reason": operand.get("mapping"),
                "runtime": {"id": runtime, "kind": "fluid"},
                "source": {"fluid": fluid, "amount": source_row.get("amount")},
            }
    operands = [seen[key] for key in sorted(seen)]
    return {
        "generated_by": "python tools/build_t43_operand_runtime_map.py",
        "operand_count": len(operands),
        "operands": operands,
        "schema_version": 1,
        "status": "T43_OPERAND_RUNTIME_MAP",
    }


def main(argv: list[str] | None = None) -> int:
    return common.run_managed(
        "Freeze the T43 operand runtime map",
        OUTPUT,
        build=build,
        argv=argv,
    )


if __name__ == "__main__":
    raise SystemExit(main())
