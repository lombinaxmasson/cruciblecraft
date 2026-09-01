#!/usr/bin/env python3
"""Prove T49 production-lock families are non-recycling Bath chemical refine."""
from __future__ import annotations

import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import recycling_candidate
from tools import build_t49_identity_catalog as identity
from tools import wave_bath_tiny_purified as common

OUTPUT = common.RECYCLING_DISPOSITION


def build() -> dict[str, Any]:
    work_set = common.load_json(common.WORK_SET)
    lock_ids = identity.lock_family_ids()
    if lock_ids != set(common.LOCK_FAMILY_IDS):
        raise ValueError("T49 recycling disposition requires the 5-family lock set")
    overlay_by_id = recycling_candidate.overlay_by_id()
    rows = []
    for family in work_set.get("families") or []:
        family_id = str(family["family_id"])
        if family_id not in lock_ids:
            continue
        if recycling_candidate.effective_recycling_candidate(
            family_id,
            overlay_by_id.get(family_id),
        ):
            raise ValueError(f"{family_id}: effective recycling_candidate leaked into T49 lock")
        overlay_row = overlay_by_id.get(family_id) or {}
        if overlay_row.get("recycling_candidate") is not True:
            raise ValueError(f"{family_id}: overlay diagnostic flag must stay true")
        rows.append(
            {
                "family_id": family_id,
                "overlay_recycling_candidate": True,
                "recycling_candidate": False,
                "semantic_role": "bath_chemical_refine",
                "template_key": family.get("template_key"),
            }
        )
    if len(rows) != common.EXPECTED_FAMILY_COUNT:
        raise ValueError("T49 recycling disposition must cover the 5 lock families")
    return {
        "deferred_recycling_untouched": common.DEFERRED_RECYCLING_COUNT,
        "family_count": len(rows),
        "families": rows,
        "generated_by": "python tools/build_t49_bath_recycling_disposition.py",
        "recycling_candidate": False,
        "schema_version": 1,
        "source_revision": common.SOURCE_REVISION,
        "status": "T49_BATH_RECYCLING_DISPOSITION",
    }


def main(argv: list[str] | None = None) -> int:
    return common.run_managed(
        "Freeze T49 non-recycling disposition",
        OUTPUT,
        build=build,
        argv=argv,
    )


if __name__ == "__main__":
    raise SystemExit(main())
