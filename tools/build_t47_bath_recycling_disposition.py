#!/usr/bin/env python3
"""Prove T47 production-lock families are non-recycling Bath treatment."""
from __future__ import annotations

import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import build_t47_identity_catalog as identity
from tools import t47_common as common

OUTPUT = common.RECYCLING_DISPOSITION


def build() -> dict[str, Any]:
    work_set = common.load_json(common.WORK_SET)
    lock_ids = identity.lock_family_ids()
    blocker = common.load_json(common.T42_BLOCKER)
    by_id = {str(row["family_id"]): row for row in blocker.get("families") or []}
    rows = []
    for family in work_set.get("families") or []:
        family_id = str(family["family_id"])
        if family_id not in lock_ids:
            continue
        blocker_row = by_id.get(family_id) or {}
        if blocker_row.get("recycling_candidate"):
            raise ValueError(f"{family_id}: recycling_candidate leaked into T47 lock set")
        rows.append(
            {
                "family_id": family_id,
                "recycling_candidate": False,
                "semantic_role": "bath_treatment_reconditioning",
                "template_key": family.get("template_key"),
            }
        )
    return {
        "deferred_recycling_untouched": common.DEFERRED_RECYCLING_COUNT,
        "family_count": len(rows),
        "families": rows,
        "generated_by": "python tools/build_t47_bath_recycling_disposition.py",
        "recycling_candidate": False,
        "schema_version": 1,
        "source_revision": common.SOURCE_REVISION,
        "status": "T47_BATH_RECYCLING_DISPOSITION",
    }


def main(argv: list[str] | None = None) -> int:
    return common.run_managed(
        "Freeze T47 non-recycling disposition",
        OUTPUT,
        build=build,
        argv=argv,
    )


if __name__ == "__main__":
    raise SystemExit(main())
