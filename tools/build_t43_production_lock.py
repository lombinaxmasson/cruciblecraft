#!/usr/bin/env python3
"""Freeze the T43 production lock for 407 Smelter stone singleton families."""
from __future__ import annotations

import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35  # noqa: E402
from tools import t43_common as common  # noqa: E402

OUTPUT = common.PRODUCTION_LOCK


def _consume_runtime(relation: dict[str, Any]) -> tuple[str, str]:
    for operand, count, action in zip(
        relation.get("item_inputs") or [],
        relation.get("item_input_counts") or [],
        relation.get("item_input_actions") or [],
        strict=False,
    ):
        kind = str((action or {}).get("kind") or "").lower()
        if kind != "consume" or int(count or 0) <= 0:
            continue
        source = (operand.get("source") or {})
        runtime = common.assert_runtime_id(operand.get("runtime_id"), consume=True)
        return str(source.get("item") or ""), runtime
    raise ValueError(f"missing consume operand for {relation.get('template_key')}")


def build() -> dict[str, Any]:
    work_set = common.load_json(common.WORK_SET)
    candidate = common.load_json(common.CANDIDATE_SELECTION)
    source = common.load_json(common.SOURCE)
    support = (
        common.load_json(common.PLAYER_PATH_SUPPORT)
        if common.PLAYER_PATH_SUPPORT.is_file()
        else {"routes": []}
    )
    if candidate.get("selection_sha256") != common.EXPECTED_SELECTION_SHA256:
        raise ValueError("T43 candidate selection_sha256 drifted")
    if candidate.get("accepted_count") != common.PRODUCTION_FAMILY_COUNT:
        raise ValueError("T43 candidate is not 407 accepted families")
    by_template = {
        str(row["template_key"]): row for row in source.get("relations") or []
    }
    production_families: list[dict[str, Any]] = []
    stable_ids: list[str] = []
    for row in work_set.get("families") or []:
        template_key = str(row["template_key"])
        relation = by_template[template_key]
        source_item, runtime_id = _consume_runtime(relation)
        hex16 = str(relation["stable_id"]).rsplit("/", 1)[-1]
        runtime_stable = f"cruciblecraft:t43/{hex16}"
        production_families.append(
            {
                "b0_status": "needs_b1_support" if row["cohort"] == "A" else "b0_ready",
                "cohort": row["cohort"],
                "expanded_count": 1,
                "family_id": row["family_id"],
                "mapped_runtime_id": runtime_id,
                "publication_group": common.STONE_GROUP,
                "representation": "singleton",
                "source_identity": source_item,
                "stable_id": runtime_stable,
                "template_key": template_key,
            }
        )
        stable_ids.append(runtime_stable)
    family_ids = [row["family_id"] for row in production_families]
    digest = common.selection_sha256(family_ids)
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
            "families": common.CATALOG_FAMILY_COUNT,
            "relations": common.CATALOG_RELATION_COUNT,
            "selection_sha256": common.EXPECTED_SELECTION_SHA256,
            "status": "PRODUCTION_EQUALS_WORK_SET",
        },
        "generated_by": "python tools/build_t43_production_lock.py",
        "note": "407/407 is the production authority. No combinatorial deferral.",
        "phase_deferred": [],
        "production": {
            "families": production_families,
            "family_count": len(production_families),
            "family_ids": family_ids,
            "relation_count": len(production_families),
            "selection_sha256": digest,
            "stable_ids": stable_ids,
            "template_keys": [row["template_key"] for row in production_families],
        },
        "schema_version": 1,
        "source_revision": common.SOURCE_REVISION,
        "status": "T43_PRODUCTION_LOCKED",
        "support": {
            "route_count": len(route_keys),
            "route_keys": route_keys,
        },
    }


def main(argv: list[str] | None = None) -> int:
    return common.run_managed(
        "Freeze the T43 production lock",
        OUTPUT,
        build=build,
        argv=argv,
    )


if __name__ == "__main__":
    raise SystemExit(main())
