#!/usr/bin/env python3
"""Family-atomic object-boundary and runtime-behavior evidence for the T45 lock."""
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
from tools import t45_common as common

OUTPUT = common.OBJECT_EVIDENCE


def _digest(payload: dict[str, Any]) -> str:
    return hashlib.sha256(
        json.dumps(payload, ensure_ascii=False, sort_keys=True).encode("utf-8")
    ).hexdigest()


def build() -> dict[str, Any]:
    candidate = common.load_json(common.CANDIDATE_SELECTION)
    catalog = common.load_json(common.BLOCK_CATALOG)
    source = common.load_json(common.SOURCE)
    by_runtime = {str(row["runtime_id"]): row for row in catalog.get("identities") or []}
    by_template = {
        str(row["template_key"]): row for row in source.get("relations") or []
    }
    approvals: list[dict[str, Any]] = []
    for row in candidate.get("accepted") or []:
        relation = by_template[str(row["template_key"])]
        consume_runtime = None
        for operand, count, action in zip(
            relation.get("item_inputs") or [],
            relation.get("item_input_counts") or [],
            relation.get("item_input_actions") or [],
            strict=False,
        ):
            if str((action or {}).get("kind") or "").lower() != "consume":
                continue
            if int(count or 0) <= 0:
                continue
            consume_runtime = str(operand.get("runtime_id") or "")
            break
        identity = by_runtime.get(consume_runtime or "")
        if identity is None:
            raise ValueError(f"{row['family_id']} consume identity missing from catalog")
        boundary = {
            "behavior": identity["behavior"],
            "family_id": row["family_id"],
            "runtime_id": identity["runtime_id"],
            "source_item": identity["source_item"],
            "source_meta": identity["meta"],
        }
        behavior = {
            "behavior": identity["behavior"],
            "player_distinguishable": True,
            "registry_path": identity["registry_path"],
            "texture": identity["texture"],
        }
        approvals.append(
            {
                "family_id": row["family_id"],
                "future_owner": "later:object_expression/block",
                "object_boundary_root_sha256": _digest(boundary),
                "recheck_condition": "runtime_block_behavior_and_player_path",
                "runtime_behavior_root_sha256": _digest(behavior),
            }
        )
    return {
        "approvals": approvals,
        "approval_count": len(approvals),
        "generated_by": "python tools/build_t45_object_expression_evidence.py",
        "note": "Family-atomic object-boundary and runtime-behavior proof. Empty approvals are not proof.",
        "schema_version": 1,
        "status": "T45_OBJECT_EXPRESSION_EVIDENCE",
    }


def main(argv: list[str] | None = None) -> int:
    return common.run_managed(
        "Freeze T45 object-expression evidence",
        OUTPUT,
        build=build,
        argv=argv,
    )


if __name__ == "__main__":
    raise SystemExit(main())
