#!/usr/bin/env python3
"""Freeze the T46 Bath MTE work set from the T42 owner overlay."""
from __future__ import annotations

import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t46_common as common

OUTPUT = common.WORK_SET


def build() -> dict[str, Any]:
    selected = common.overlay_bath_mte_families()
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
    exact = 0
    multi = 0
    relations = 0
    for row in selected:
        family_id = str(row["family_id"])
        record = by_id.get(family_id)
        if record is None:
            raise ValueError(f"T46 family missing from t35 ledger: {family_id}")
        if record.get("classification") != common.ORDINARY_CLASS:
            raise ValueError(f"{family_id}: classification is not ordinary_optional")
        blocker_row = blocker_by_id.get(family_id) or {}
        if blocker_row.get("recycling_candidate"):
            raise ValueError(f"{family_id}: recycling_candidate leaked into work set")
        if list(blocker_row.get("unique_kinds") or []) != ["mte"]:
            raise ValueError(f"{family_id}: unique_kinds is not exactly mte")
        representation = str(blocker_row.get("representation_status") or "")
        if representation not in {"exact", "exact_multi"}:
            raise ValueError(f"{family_id}: representation is not exact/exact_multi")
        expanded = int(record.get("expanded_count") or blocker_row.get("relation_count") or 0)
        if expanded <= 0:
            raise ValueError(f"{family_id}: expanded_count must be positive")
        if representation == "exact":
            exact += 1
        else:
            multi += 1
        relations += expanded
        families.append(
            {
                "current_owner": common.OWNER_TRACK,
                "expanded_count": expanded,
                "family_id": family_id,
                "host": common.HOST,
                "publication_group": common.PUBLICATION_GROUP,
                "representation": representation,
                "template_key": str(row["template_key"]),
            }
        )
    family_ids = [row["family_id"] for row in families]
    if len(family_ids) != common.EXPECTED_FAMILY_COUNT:
        raise ValueError(
            f"T46 work set {len(family_ids)} != {common.EXPECTED_FAMILY_COUNT}"
        )
    if exact != common.EXPECTED_EXACT_FAMILIES or multi != common.EXPECTED_EXACT_MULTI_FAMILIES:
        raise ValueError(
            f"T46 representation drifted exact={exact} multi={multi}"
        )
    if relations != common.EXPECTED_RELATION_COUNT:
        raise ValueError(
            f"T46 work set relations {relations} != {common.EXPECTED_RELATION_COUNT}"
        )
    digest = common.selection_sha256(family_ids)
    return {
        "family_count": len(families),
        "family_ids": family_ids,
        "families": families,
        "generated_by": "python tools/build_t46_work_set.py",
        "host": common.HOST,
        "owner": common.OWNER,
        "publication_groups": {
            common.PUBLICATION_GROUP: {
                "expanded_count": relations,
                "family_count": len(families),
                "family_ids": family_ids,
                "rule": "exact and exact_multi Bath MTE compact relations",
            }
        },
        "relation_count": relations,
        "representation": {
            "exact": exact,
            "exact_multi": multi,
        },
        "schema_version": 1,
        "selection_rule": (
            "host==cruciblecraft:bath unique_kinds==[mte] "
            "current_owner==recycling/evidence_needed recycling_candidate==false "
            "representation in {exact,exact_multi}"
        ),
        "selection_sha256": digest,
        "source_revision": common.SOURCE_REVISION,
        "status": "T46_WORK_SET_FROZEN",
        "template_keys": [row["template_key"] for row in families],
        "unique_active_card": "T46",
    }


def main(argv: list[str] | None = None) -> int:
    return common.run_managed(
        "Freeze the T46 Bath MTE work set",
        OUTPUT,
        build=build,
        argv=argv,
    )


if __name__ == "__main__":
    raise SystemExit(main())
