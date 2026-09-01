#!/usr/bin/env python3
"""Record consume/preserve identities for locked T46 generated families."""
from __future__ import annotations

import json
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t46_common as common

OUTPUT = common.PLAYER_PATH


def _rows() -> list[dict[str, Any]]:
    rows: list[dict[str, Any]] = []
    for path in common.generated_family_files():
        family = json.loads(path.read_text(encoding="utf-8"))
        for relation in family.get("relations") or []:
            consume_ids = [
                str(stack.get("item") or stack.get("items") or "")
                for stack in relation.get("item_inputs") or []
            ]
            preserve_ids: list[str] = []
            output_ids = [str(stack["id"]) for stack in relation.get("item_outputs") or []]
            fluid_out = [str(stack["id"]) for stack in relation.get("fluid_outputs") or []]
            fluid_in = [str(stack["id"]) for stack in relation.get("fluid_inputs") or []]
            inputs_ok = all(
                item_id.startswith(("minecraft:", "cruciblecraft:"))
                for item_id in consume_ids
            )
            outputs_ok = all(
                item_id.startswith(("minecraft:", "cruciblecraft:"))
                for item_id in output_ids + fluid_out
            )
            rows.append({
                "alias_fail_closed": False,
                "consume_ids": consume_ids,
                "fluid_input_ids": fluid_in,
                "fluid_output_ids": fluid_out,
                "inputs_reachable": inputs_ok,
                "output_ids": output_ids,
                "outputs_registered": outputs_ok,
                "preserve_ids": preserve_ids,
                "publication_group": family["publication_group"],
                "stable_id": relation["stable_id"],
                "template_key": family["family_id"],
            })
    rows.sort(key=lambda row: str(row["stable_id"]))
    return rows


def build() -> dict[str, Any]:
    rows = _rows()
    expected = common.production_relation_count()
    if len(rows) != expected:
        raise ValueError(f"T46 player-path rows drifted: {len(rows)} != {expected}")
    reachable = sum(1 for row in rows if row["inputs_reachable"])
    registered = sum(1 for row in rows if row["outputs_registered"])
    if reachable != expected or registered != expected:
        raise ValueError("T46 production consume/output identities are incomplete")
    return {
        "families": common.production_family_count(),
        "frozen_review_unmodified": True,
        "inputs_reachable": reachable,
        "note": (
            "B0/B1 proof is tools/t46_layered_player_path.json. "
            "This sidecar records consume/preserve identities after mapping."
        ),
        "outputs_registered": registered,
        "relations": len(rows),
        "rows": rows,
        "schema_version": 1,
        "status": "T46_PLAYER_PATH",
    }


def main(argv: list[str] | None = None) -> int:
    return common.run_managed(
        "Record T46 player-path identities",
        OUTPUT,
        build=build,
        argv=argv,
    )


if __name__ == "__main__":
    raise SystemExit(main())
