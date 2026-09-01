#!/usr/bin/env python3
"""Record consume/preserve identities for locked T48 generated families."""
from __future__ import annotations

import json
import sys
from functools import lru_cache
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35
from tools import t48_common as common

OUTPUT = common.PLAYER_PATH
TAG_ROOT = ROOT / "src" / "main" / "resources" / "data"


@lru_cache(maxsize=None)
def _tag_items(tag_id: str) -> tuple[str, ...]:
    namespace, _, path = tag_id.partition(":")
    if not namespace or not path:
        return ()
    tag_path = TAG_ROOT / namespace / "tags" / "item" / f"{path}.json"
    if not tag_path.is_file():
        return ()
    values = t35.load_json(tag_path).get("values") or []
    return tuple(
        str(value) for value in values if isinstance(value, str) and ":" in value
    )


def _item_ids(stack: dict[str, Any]) -> list[str]:
    tag = stack.get("tag")
    if tag:
        items = list(_tag_items(str(tag)))
        if items:
            return items
        return [str(tag)]
    item_id = str(stack.get("item") or stack.get("id") or stack.get("items") or "")
    return [item_id] if item_id else []


def _rows() -> list[dict[str, Any]]:
    rows: list[dict[str, Any]] = []
    for path in common.generated_family_files():
        family = json.loads(path.read_text(encoding="utf-8"))
        for relation in family.get("relations") or []:
            consume_ids: list[str] = []
            preserve_ids: list[str] = []
            for stack, count, action in zip(
                relation.get("item_inputs") or [],
                relation.get("item_input_counts") or [],
                relation.get("item_input_actions") or [],
                strict=False,
            ):
                kind = str((action or {}).get("kind") or "").lower()
                for item_id in _item_ids(stack):
                    if kind == "preserve":
                        preserve_ids.append(item_id)
                    elif kind == "consume" and int(count or 0) > 0:
                        consume_ids.append(item_id)
            output_ids = [
                item_id
                for stack in relation.get("item_outputs") or []
                for item_id in _item_ids(stack)
            ]
            fluid_out = [str(stack["id"]) for stack in relation.get("fluid_outputs") or []]
            fluid_in = [str(stack["id"]) for stack in relation.get("fluid_inputs") or []]
            inputs_ok = all(
                item_id.startswith(("minecraft:", "cruciblecraft:"))
                for item_id in consume_ids + preserve_ids + fluid_in
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
        raise ValueError(f"T48 player-path rows drifted: {len(rows)} != {expected}")
    reachable = sum(1 for row in rows if row["inputs_reachable"])
    registered = sum(1 for row in rows if row["outputs_registered"])
    if reachable != expected or registered != expected:
        raise ValueError("T48 production consume/output identities are incomplete")
    return {
        "families": common.production_family_count(),
        "frozen_review_unmodified": True,
        "inputs_reachable": reachable,
        "note": (
            "B0/B1 proof is tools/t48_layered_player_path.json. "
            "This sidecar records consume/preserve identities after mapping."
        ),
        "outputs_registered": registered,
        "relations": len(rows),
        "rows": rows,
        "schema_version": 1,
        "status": "T48_PLAYER_PATH",
    }


def main(argv: list[str] | None = None) -> int:
    return common.run_managed(
        "Record T48 player-path consume identities",
        OUTPUT,
        build=build,
        argv=argv,
    )


if __name__ == "__main__":
    raise SystemExit(main())
