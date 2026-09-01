#!/usr/bin/env python3
"""Freeze the T49 Bath tiny-purified remainder work set."""
from __future__ import annotations

import sys
from collections import Counter
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import wave_bath_tiny_purified as common

OUTPUT = common.WORK_SET


def build() -> dict[str, Any]:
    opening = common.assert_t48_opening_current()
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
    t48_candidate = common.load_json(common.T48_CANDIDATE_SELECTION)
    leftover = {
        str(row["family_id"]): row
        for row in t48_candidate.get("families") or []
        if str(row.get("family_id") or "") in common.LOCK_FAMILY_IDS
    }
    if set(leftover) != set(common.LOCK_FAMILY_IDS):
        raise ValueError("T49 work set is not the T48 leftover Bath tiny-purified set")
    prior = common.prior_locked_family_ids()
    families: list[dict[str, Any]] = []
    relations = 0
    owners: Counter[str] = Counter()
    for family_id in common.LOCK_FAMILY_IDS:
        if family_id in prior:
            raise ValueError(f"{family_id}: leaked into T46/T47/T48 lock")
        row = leftover[family_id]
        record = by_id.get(family_id)
        if record is None:
            raise ValueError(f"T49 family missing from t35 ledger: {family_id}")
        if record.get("classification") != common.ORDINARY_CLASS:
            raise ValueError(f"{family_id}: classification is not ordinary_optional")
        blocker_row = blocker_by_id.get(family_id) or {}
        representation = str(row.get("representation") or "exact_multi")
        if representation != "exact_multi":
            raise ValueError(f"{family_id}: T49 remainder is exact_multi only")
        expanded = int(
            row.get("source_relation_count")
            or row.get("expanded_count")
            or blocker_row.get("relation_count")
            or 0
        )
        if expanded <= 0:
            raise ValueError(f"{family_id}: expanded_count must be positive")
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
    if relations != common.CANDIDATE_RELATION_COUNT:
        raise ValueError(
            f"T49 work set relations {relations} != {common.CANDIDATE_RELATION_COUNT}"
        )
    family_ids = [row["family_id"] for row in families]
    digest = common.selection_sha256(family_ids)
    return {
        "candidate_universe": True,
        "family_count": len(families),
        "family_ids": family_ids,
        "families": families,
        "generated_by": "python tools/build_t49_work_set.py",
        "host": common.HOST,
        "opening": {
            "authored_entries": opening["authored_entries"],
            "eager_rows": opening["eager_rows"],
            "execution_gap": opening["execution_gap"],
            "lazy_rows": opening["lazy_rows"],
            "cache_ceiling_rows": opening["cache_ceiling_rows"],
            "t48_production_lock_sha256": opening["t48_production_lock_sha256"],
        },
        "owner": common.OWNER,
        "owner_counts": dict(sorted(owners.items())),
        "production_authority": False,
        "relation_count": relations,
        "representation": {
            "exact": 0,
            "exact_multi": len(families),
            "exact_multi_relations": relations,
        },
        "schema_version": 1,
        "selection_rule": (
            "family_id in T48 leftover Bath tiny-purified set "
            "and not in T46/T47/T48 lock host==cruciblecraft:bath exact_multi"
        ),
        "selection_sha256": digest,
        "size_exception": common.size_exception(),
        "source_revision": common.SOURCE_REVISION,
        "status": "T49_WORK_SET_FROZEN",
        "template_keys": [row["template_key"] for row in families],
        "unique_active_card": "T49",
    }


def main(argv: list[str] | None = None) -> int:
    return common.run_managed(
        "Freeze the T49 Bath tiny-purified remainder work set",
        OUTPUT,
        build=build,
        argv=argv,
    )


if __name__ == "__main__":
    raise SystemExit(main())
