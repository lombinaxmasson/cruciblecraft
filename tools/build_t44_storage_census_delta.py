#!/usr/bin/env python3
"""Overlay T44 storage census. Does not rewrite T35-T43 historical JSON."""
from __future__ import annotations

import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t44_common as common  # noqa: E402

OUTPUT = common.CENSUS_DELTA
T43_AUTHORED_CLOSING = 4466


def _identity(family: str, *, storage: bool) -> dict[str, Any]:
    expected = (
        common.FAMILY_COUNTS[family]["expanded"]
        if storage
        else common.LOGISTICS_REGISTRATIONS
    )
    sites = (
        common.FAMILY_COUNTS[family]["source_sites"]
        if storage
        else common.LOGISTICS_SOURCE_SITES
    )
    return {
        "canonical_id": f"exclusion/{family}",
        "family": family,
        "counts_toward_storage_624": storage,
        "counts_toward_ordinary_recipe_completion": False,
        "source_sites": sites,
        "expanded_registrations": expected,
        "opening": {
            "closure": "incomplete",
            "disposition": "planned",
            "fidelity": "source_backed",
            "load": "pending",
        },
        "closing": {
            "closure": "closed",
            "disposition": "implemented",
            "expressed_by": "t44",
            "fidelity": "source_backed",
            "load": "measured",
        },
        "owner": common.OWNER,
    }


def build() -> dict[str, Any]:
    variants = common.expand_variants()
    authored = sum(
        1 for row in variants if row["visibility"] == "source_visible"
    )
    identities = [
        _identity(family, storage=True) for family in common.FAMILY_COUNTS
    ]
    identities.append(_identity(common.LOGISTICS_FAMILY, storage=False))
    return {
        "complete_storage_registrations": common.STORAGE_REGISTRATIONS,
        "complete_logistics_registrations": common.LOGISTICS_REGISTRATIONS,
        "completion_delta": common.COMPLETION_DELTA,
        "fidelity": "source_backed",
        "generated_by": "python tools/build_t44_storage_census_delta.py",
        "host": common.HOST,
        "identities": identities,
        "note": (
            "624 storage registrations plus logistics 1/1. This overlay does "
            "not count registrations as ordinary recipe completion."
        ),
        "reclassification_delta": common.RECLASSIFICATION_DELTA,
        "remaining_ordinary": {
            "remaining_ordinary_families": common.CLOSING_EXECUTION_GAP,
            "completion_delta": 0,
            "reclassification_delta": 0,
        },
        "recipe_opening_execution_gap": common.OPENING_EXECUTION_GAP,
        "recipe_closing_execution_gap": common.CLOSING_EXECUTION_GAP,
        "schema_version": 1,
        "source_revision": common.SOURCE_REVISION,
        "status": "T44_STORAGE_CENSUS_DELTA_READY",
        "storage_source_sites": common.STORAGE_SOURCE_SITES,
        "logistics_source_sites": common.LOGISTICS_SOURCE_SITES,
        "t14_load": {
            "axes": [
                {
                    "axis": "datapack_authored_entries",
                    "opening": T43_AUTHORED_CLOSING,
                    "delta": authored,
                    "closing": T43_AUTHORED_CLOSING + authored,
                    "measured": True,
                    "pending": False,
                    "hard_ceiling": common.T14_HARD["datapack_authored_entries"],
                    "hard_ceiling_raised": False,
                    "evidence": "T44 source-visible acquisition recipes only",
                }
            ]
        },
        "t35_history_readonly": True,
        "t36_live_machine_rows": common.T36_LIVE_MACHINE_ROWS,
        "t43_history_readonly": True,
    }


def main(argv: list[str] | None = None) -> int:
    return common.run_managed(
        "Write the T44 storage census overlay",
        OUTPUT,
        build=build,
        argv=argv,
    )


if __name__ == "__main__":
    raise SystemExit(main())
