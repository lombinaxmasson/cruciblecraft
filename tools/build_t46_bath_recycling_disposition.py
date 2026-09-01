#!/usr/bin/env python3
"""Prove the T46 Bath MTE work set is non-recycling treatment/reconditioning."""
from __future__ import annotations

import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t46_common as common

OUTPUT = common.RECYCLING_DISPOSITION


def build() -> dict[str, Any]:
    work_set = common.load_json(common.WORK_SET)
    blocker = common.load_json(common.T42_BLOCKER)
    by_id = {str(row["family_id"]): row for row in blocker.get("families") or []}
    rows: list[dict[str, Any]] = []
    for family in work_set.get("families") or []:
        family_id = str(family["family_id"])
        blocker_row = by_id.get(family_id) or {}
        if blocker_row.get("recycling_candidate"):
            raise ValueError(f"{family_id}: recycling_candidate leaked into T46")
        if list(blocker_row.get("unique_kinds") or []) != ["mte"]:
            raise ValueError(f"{family_id}: unique_kinds is not exactly mte")
        rows.append(
            {
                "family_id": family_id,
                "recycling_candidate": False,
                "semantic_role": "bath_treatment_reconditioning",
                "template_key": family["template_key"],
            }
        )
    family_ids = [row["family_id"] for row in rows]
    if family_ids != list(work_set.get("family_ids") or []):
        raise ValueError("T46 recycling disposition membership drifted from work set")
    if len(rows) != common.EXPECTED_FAMILY_COUNT:
        raise ValueError("T46 recycling disposition is not 803 families")
    return {
        "deferred_recycling_untouched": common.DEFERRED_RECYCLING_COUNT,
        "family_count": len(rows),
        "families": rows,
        "generated_by": "python tools/build_t46_bath_recycling_disposition.py",
        "note": (
            "803 Bath MTE families stay out of later:recycling. Membership of the "
            "1817 deferred recycling families is not rewritten."
        ),
        "recycling_candidate": False,
        "schema_version": 1,
        "selection_sha256": work_set.get("selection_sha256"),
        "status": "T46_BATH_RECYCLING_DISPOSITION",
    }


def main(argv: list[str] | None = None) -> int:
    return common.run_managed(
        "Freeze T46 non-recycling disposition",
        OUTPUT,
        build=build,
        argv=argv,
    )


if __name__ == "__main__":
    raise SystemExit(main())
