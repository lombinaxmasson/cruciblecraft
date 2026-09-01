#!/usr/bin/env python3
"""Freeze the T48 Bath identity/form remainder work set from T47 blocked families."""
from __future__ import annotations

import sys
from collections import Counter
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t48_common as common

OUTPUT = common.WORK_SET


def build() -> dict[str, Any]:
    opening = common.assert_t47_opening_current()
    selected = common.overlay_bath_remainder_families()
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
    owners: Counter[str] = Counter()
    for row in selected:
        family_id = str(row["family_id"])
        record = by_id.get(family_id)
        if record is None:
            raise ValueError(f"T48 family missing from t35 ledger: {family_id}")
        if record.get("classification") != common.ORDINARY_CLASS:
            raise ValueError(f"{family_id}: classification is not ordinary_optional")
        blocker_row = blocker_by_id.get(family_id) or {}
        representation = str(
            row.get("representation") or blocker_row.get("representation_status") or ""
        )
        if representation not in {"exact", "exact_multi"}:
            raise ValueError(f"{family_id}: representation is not exact/exact_multi")
        expanded = int(
            row.get("source_relation_count")
            or row.get("expanded_count")
            or record.get("expanded_count")
            or blocker_row.get("relation_count")
            or 0
        )
        if expanded <= 0:
            raise ValueError(f"{family_id}: expanded_count must be positive")
        if representation == "exact":
            exact += 1
        else:
            multi += 1
        relations += expanded
        owner = str(row.get("current_owner") or "")
        owners[owner] += 1
        families.append(
            {
                "blocking_axes": list(row.get("blocking_axes") or []),
                "current_owner": owner,
                "expanded_count": expanded,
                "family_id": family_id,
                "host": common.HOST,
                "missing_fluids": bool(blocker_row.get("missing_fluids")),
                "publication_group": common.CANDIDATE_PUBLICATION_GROUP,
                "representation": representation,
                "secondary_owner_tracks": list(row.get("secondary_owner_tracks") or []),
                "template_key": str(row["template_key"]),
                "unique_kinds": list(
                    row.get("unique_kinds") or blocker_row.get("unique_kinds") or []
                ),
            }
        )
    family_ids = [row["family_id"] for row in families]
    if len(family_ids) != common.CANDIDATE_FAMILY_COUNT:
        raise ValueError(
            f"T48 work set {len(family_ids)} != {common.CANDIDATE_FAMILY_COUNT}"
        )
    if exact != common.CANDIDATE_EXACT_FAMILIES or multi != common.CANDIDATE_EXACT_MULTI_FAMILIES:
        raise ValueError(
            f"T48 representation drifted exact={exact} multi={multi}"
        )
    if relations != common.CANDIDATE_RELATION_COUNT:
        raise ValueError(
            f"T48 work set relations {relations} != {common.CANDIDATE_RELATION_COUNT}"
        )
    exact_multi_relations = relations - exact
    if exact_multi_relations != common.CANDIDATE_EXACT_MULTI_RELATIONS:
        raise ValueError(
            f"T48 exact_multi relations {exact_multi_relations} != "
            f"{common.CANDIDATE_EXACT_MULTI_RELATIONS}"
        )
    digest = common.selection_sha256(family_ids)
    return {
        "candidate_universe": True,
        "family_count": len(families),
        "family_ids": family_ids,
        "families": families,
        "generated_by": "python tools/build_t48_work_set.py",
        "host": common.HOST,
        "opening": {
            "authored_entries": opening["authored_entries"],
            "eager_rows": opening["eager_rows"],
            "execution_gap": opening["execution_gap"],
            "lazy_rows": opening["lazy_rows"],
            "cache_ceiling_rows": opening["cache_ceiling_rows"],
            "t47_production_lock_sha256": opening["t47_production_lock_sha256"],
        },
        "owner": common.OWNER,
        "owner_counts": dict(sorted(owners.items())),
        "production_authority": False,
        "relation_count": relations,
        "representation": {
            "exact": exact,
            "exact_multi": multi,
            "exact_multi_relations": exact_multi_relations,
        },
        "schema_version": 1,
        "selection_rule": (
            "family_id in T47 blocked set and not in T46 lock union T47 lock "
            "host==cruciblecraft:bath representation in {exact,exact_multi} "
            "ordinary_optional"
        ),
        "selection_sha256": digest,
        "size_exception": common.size_exception(),
        "source_revision": common.SOURCE_REVISION,
        "status": "T48_WORK_SET_FROZEN",
        "template_keys": [row["template_key"] for row in families],
        "unique_active_card": "T48",
    }


def main(argv: list[str] | None = None) -> int:
    return common.run_managed(
        "Freeze the T48 Bath identity/form remainder work set",
        OUTPUT,
        build=build,
        argv=argv,
    )


if __name__ == "__main__":
    raise SystemExit(main())
