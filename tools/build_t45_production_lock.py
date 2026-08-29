#!/usr/bin/env python3
"""Freeze the immutable T45 production lock."""
from __future__ import annotations

import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t45_common as common

OUTPUT = common.PRODUCTION_LOCK


def _consume(relation: dict[str, Any]) -> tuple[str, str, str]:
    for operand, count, action in zip(
        relation.get("item_inputs") or [],
        relation.get("item_input_counts") or [],
        relation.get("item_input_actions") or [],
        strict=False,
    ):
        kind = str((action or {}).get("kind") or "").lower()
        if kind != "consume" or int(count or 0) <= 0:
            continue
        source = operand.get("source") or {}
        runtime = common.assert_runtime_id(operand.get("runtime_id"), consume=True)
        mapping = str(operand.get("mapping") or "proven_equivalent")
        return str(source.get("item") or ""), runtime, mapping
    raise ValueError(f"missing consume operand for {relation.get('template_key')}")


def build() -> dict[str, Any]:
    work_set = common.load_json(common.WORK_SET)
    candidate = common.load_json(common.CANDIDATE_SELECTION)
    source = common.load_json(common.SOURCE)
    evidence = common.load_json(common.OBJECT_EVIDENCE)
    if candidate.get("accepted_count") != len(work_set.get("families") or []):
        raise ValueError("T45 candidate is not the full work set")
    if int(evidence.get("approval_count") or 0) != candidate.get("accepted_count"):
        raise ValueError("T45 object-expression evidence does not cover the candidate")
    by_template = {
        str(row["template_key"]): row for row in source.get("relations") or []
    }
    evidence_by_id = {
        str(row["family_id"]): row for row in evidence.get("approvals") or []
    }
    production_families: list[dict[str, Any]] = []
    stable_ids: list[str] = []
    for row in work_set.get("families") or []:
        relation = by_template[str(row["template_key"])]
        source_item, runtime_id, mapping_class = _consume(relation)
        proof = evidence_by_id[str(row["family_id"])]
        hex16 = str(relation["stable_id"]).rsplit("/", 1)[-1]
        runtime_stable = f"cruciblecraft:t45/{hex16}"
        production_families.append(
            {
                "b0_status": "needs_b1_support",
                "b1_status": "worldgen_scatter",
                "evidence": {
                    "object_boundary_root_sha256": proof["object_boundary_root_sha256"],
                    "runtime_behavior_root_sha256": proof["runtime_behavior_root_sha256"],
                },
                "expanded_count": 1,
                "family_id": row["family_id"],
                "host": row["host"],
                "mapped_cc_identity": runtime_id,
                "mapped_runtime_id": runtime_id,
                "mapping_class": mapping_class,
                "publication_group": row["publication_group"],
                "representation": "singleton",
                "selection_sha256": candidate["selection_sha256"],
                "source_identity": source_item,
                "source_template": row["template_key"],
                "stable_id": runtime_stable,
                "template_key": row["template_key"],
            }
        )
        stable_ids.append(runtime_stable)
    family_ids = [row["family_id"] for row in production_families]
    digest = common.selection_sha256(family_ids)
    count = len(production_families)
    if count < common.MIN_PRODUCTION_FAMILIES:
        raise ValueError(f"T45 lock {count} is below the 300-family floor")
    support = (
        common.load_json(common.PLAYER_PATH_SUPPORT)
        if common.PLAYER_PATH_SUPPORT.is_file()
        else {"routes": []}
    )
    route_keys = sorted(
        {
            f"{route['source_map']}#{route.get('source_recipe', route.get('route_key', ''))}"
            for route in support.get("routes") or []
        }
    )
    return {
        "candidate_snapshot": {
            "path": common.relative(common.CANDIDATE_SELECTION),
            "selection_sha256": candidate["selection_sha256"],
        },
        "catalog_fixture": {
            "families": count,
            "relations": count,
            "selection_sha256": digest,
            "status": "PRODUCTION_EQUALS_WORK_SET",
        },
        "generated_by": "python tools/build_t45_production_lock.py",
        "note": (
            f"{count}/{count} is the production authority. Centrifuge sands recycling "
            "families are excluded. No combinatorial deferral."
        ),
        "phase_deferred": [],
        "production": {
            "families": production_families,
            "family_count": count,
            "family_ids": family_ids,
            "relation_count": count,
            "selection_sha256": digest,
            "stable_ids": stable_ids,
            "template_keys": [row["template_key"] for row in production_families],
        },
        "schema_version": 1,
        "source_revision": common.SOURCE_REVISION,
        "status": "T45_PRODUCTION_LOCKED",
        "support": {
            "route_count": len(route_keys),
            "route_keys": route_keys,
        },
    }


def main(argv: list[str] | None = None) -> int:
    return common.run_managed(
        "Freeze the T45 production lock",
        OUTPUT,
        build=build,
        argv=argv,
    )


if __name__ == "__main__":
    raise SystemExit(main())
