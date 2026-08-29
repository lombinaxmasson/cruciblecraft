#!/usr/bin/env python3
"""Freeze the T45 block-object work set from the T42 owner overlay."""
from __future__ import annotations

import sys
from collections import defaultdict
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t45_common as common

OUTPUT = common.WORK_SET


def build() -> dict[str, Any]:
    selected = common.overlay_block_families()
    families_doc = common.load_json(common.RECIPE_FAMILIES)
    if families_doc.get("source_revision") != common.SOURCE_REVISION:
        raise ValueError("t35_recipe_families source_revision drifted")
    by_id = {
        str(row.get("family_id") or ""): row
        for row in families_doc.get("families") or []
        if isinstance(row, dict)
    }
    blocker = common.load_json(common.T42_BLOCKER)
    blocker_by_id = {str(row["family_id"]): row for row in blocker.get("families") or []}
    families: list[dict[str, Any]] = []
    expanded_counts: list[int] = []
    groups: dict[str, list[str]] = defaultdict(list)
    for row in selected:
        family_id = str(row["family_id"])
        record = by_id.get(family_id)
        if record is None:
            raise ValueError(f"T45 family missing from t35 ledger: {family_id}")
        if record.get("classification") != "ordinary_optional":
            raise ValueError(f"{family_id}: classification is not ordinary_optional")
        expanded = int(record.get("expanded_count") or 0)
        if expanded != 1:
            raise ValueError(f"{family_id}: T45 does not split non-singletons")
        blocker_row = blocker_by_id.get(family_id) or {}
        if blocker_row.get("recycling_candidate"):
            raise ValueError(f"{family_id}: recycling_candidate leaked into work set")
        if list(blocker_row.get("unique_kinds") or []) != ["block"]:
            raise ValueError(f"{family_id}: unique_kinds is not exactly block")
        host = str(row["host"])
        group = common.publication_group_for(host)
        families.append(
            {
                "current_owner": common.OWNER_TRACK,
                "expanded_count": expanded,
                "family_id": family_id,
                "host": host,
                "publication_group": group,
                "template_key": str(row["template_key"]),
            }
        )
        expanded_counts.append(expanded)
        groups[group].append(family_id)
    family_ids = [row["family_id"] for row in families]
    if len(family_ids) < common.MIN_PRODUCTION_FAMILIES:
        raise ValueError(
            f"T45 work set {len(family_ids)} is below the 300-family floor"
        )
    if len(set(family_ids)) != len(family_ids):
        raise ValueError("T45 work set has duplicate family ids")
    digest = common.selection_sha256(family_ids)
    publication_groups = {
        group: {
            "expanded_count": len(ids),
            "family_count": len(ids),
            "family_ids": ids,
            "rule": "exact singleton compact relations",
        }
        for group, ids in groups.items()
    }
    return {
        "authored_compact_family_entries": len(families),
        "catalog_only": False,
        "expected_logical_relations": len(families),
        "expanded_count_distribution": {"1": len(families)},
        "family_count": len(families),
        "family_ids": family_ids,
        "families": families,
        "generated_by": "python tools/build_t45_work_set.py",
        "hosts": sorted({row["host"] for row in families}),
        "note": (
            "Recomputed object_expression/block catalog excluding centrifuge sands "
            "recycling_candidate families and 15 lossy GT wildcard-meta families "
            "(asphalt/concrete/lily). Not a production lock."
        ),
        "owner": common.OWNER,
        "publication_group_count": len(publication_groups),
        "publication_groups": publication_groups,
        "schema_version": 1,
        "selection_rule": {
            "current_owner": common.OWNER_TRACK,
            "exclude": [
                "centrifuge sands recycling_candidate",
                "lossy GT wildcard meta (asphalt/concrete/lily)",
                "recipe_wave/drying",
                "recipe_wave/bath",
                "other object_expression/*",
            ],
            "hosts": list(common.ALLOWED_HOSTS),
            "sort": "family_id ascending",
        },
        "selection_sha256": digest,
        "source_revision": common.SOURCE_REVISION,
        "source_rows": sum(expanded_counts),
        "status": "T45_WORK_SET_FROZEN",
        "template_keys": [row["template_key"] for row in families],
        "unique_active_card": "T45",
    }


def main(argv: list[str] | None = None) -> int:
    return common.run_managed(
        "Freeze the T45 block-object work set",
        OUTPUT,
        build=build,
        argv=argv,
    )


if __name__ == "__main__":
    raise SystemExit(main())
