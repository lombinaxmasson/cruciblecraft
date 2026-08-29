#!/usr/bin/env python3
"""Accept T45 production candidates from the frozen block-object source."""
from __future__ import annotations

import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t45_common as common

OUTPUT = common.CANDIDATE_SELECTION


def _blockers(relation: dict[str, Any]) -> list[str]:
    blockers: list[str] = []
    if relation.get("unsupported_semantics"):
        blockers.append("unsupported_semantics")
    operands = (
        list(relation.get("item_inputs") or [])
        + list(relation.get("item_outputs") or [])
        + list(relation.get("fluid_inputs") or [])
        + list(relation.get("fluid_outputs") or [])
    )
    if any(operand.get("mapping") == "blocked_unmapped" for operand in operands):
        blockers.append("blocked_unmapped")
    if any(not operand.get("runtime_id") for operand in operands):
        blockers.append("unresolved_operand")
    consume = 0
    for operand, count, action in zip(
        relation.get("item_inputs") or [],
        relation.get("item_input_counts") or [],
        relation.get("item_input_actions") or [],
        strict=False,
    ):
        kind = str((action or {}).get("kind") or "").lower()
        if kind == "consume" and int(count or 0) > 0:
            consume += 1
            runtime = operand.get("runtime_id")
            if runtime:
                common.assert_runtime_id(runtime, consume=True)
    if consume < 1:
        blockers.append("missing_consume")
    return blockers


def build() -> dict[str, Any]:
    work_set = common.load_json(common.WORK_SET)
    source = common.load_json(common.SOURCE)
    if source.get("status") != "T45_BLOCK_OBJECT_SOURCE_FROZEN":
        raise ValueError("T45 candidate selection requires a frozen source")
    by_template = {
        str(row["template_key"]): row for row in source.get("relations") or []
    }
    accepted: list[dict[str, Any]] = []
    rejected: list[dict[str, Any]] = []
    for row in work_set.get("families") or []:
        template_key = str(row["template_key"])
        relation = by_template.get(template_key)
        payload = {
            "expanded_count": 1,
            "family_id": row["family_id"],
            "host": row["host"],
            "publication_group": row["publication_group"],
            "template_key": template_key,
        }
        if relation is None:
            payload["blockers"] = ["missing_source_relation"]
            rejected.append(payload)
            continue
        blockers = _blockers(relation)
        if blockers:
            payload["blockers"] = blockers
            rejected.append(payload)
        else:
            accepted.append(payload)
    if len(accepted) < common.MIN_PRODUCTION_FAMILIES:
        raise ValueError(
            f"T45 candidate accepted {len(accepted)} < {common.MIN_PRODUCTION_FAMILIES}"
        )
    if rejected:
        raise ValueError(
            f"T45 candidate rejected {len(rejected)} families: "
            + ",".join(row["template_key"] for row in rejected[:8])
        )
    family_ids = [row["family_id"] for row in accepted]
    return {
        "accepted": accepted,
        "accepted_count": len(accepted),
        "generated_by": "python tools/build_t45_candidate_selection.py",
        "rejected": rejected,
        "rejected_count": 0,
        "schema_version": 1,
        "selection_sha256": common.selection_sha256(family_ids),
        "status": "T45_PRODUCTION_CANDIDATE",
    }


def main(argv: list[str] | None = None) -> int:
    return common.run_managed(
        "Freeze the T45 production candidate selection",
        OUTPUT,
        build=build,
        argv=argv,
    )


if __name__ == "__main__":
    raise SystemExit(main())
