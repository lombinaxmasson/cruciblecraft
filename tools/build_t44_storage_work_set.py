#!/usr/bin/env python3
"""Freeze the T44 storage work set from T35 reclaim lineage."""
from __future__ import annotations

import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t44_common as common  # noqa: E402

OUTPUT = common.WORK_SET


def build() -> dict[str, Any]:
    sites = common.reclaim_storage_sites()
    common.assert_reclaim_counts(sites)
    families = []
    for family, expected in common.FAMILY_COUNTS.items():
        family_sites = [
            row for row in sites if row["canonical_family"] == family
        ]
        families.append(
            {
                "family": family,
                "owner": f"portfolio:storage/{family}",
                "source_sites": expected["source_sites"],
                "expanded": expected["expanded"],
                "counts_toward_storage_624": True,
                "site_keys": [row["site_key"] for row in family_sites],
            }
        )
    logistics = [
        row for row in sites if row["canonical_family"] == common.LOGISTICS_FAMILY
    ]
    families.append(
        {
            "family": common.LOGISTICS_FAMILY,
            "owner": f"portfolio:storage/{common.LOGISTICS_FAMILY}",
            "source_sites": common.LOGISTICS_SOURCE_SITES,
            "expanded": common.LOGISTICS_REGISTRATIONS,
            "counts_toward_storage_624": False,
            "site_keys": [row["site_key"] for row in logistics],
        }
    )
    return {
        "generated_by": "python tools/build_t44_storage_work_set.py",
        "host": common.HOST,
        "note": (
            "T44 is the Storage bundle. 28/624 and logistics 1/1 are separate "
            "denominators. This is not an ordinary recipe wave."
        ),
        "owner": common.OWNER,
        "schema_version": 1,
        "source_revision": common.SOURCE_REVISION,
        "status": "T44_WORK_SET_FROZEN",
        "storage_source_sites": common.STORAGE_SOURCE_SITES,
        "storage_registrations": common.STORAGE_REGISTRATIONS,
        "logistics_source_sites": common.LOGISTICS_SOURCE_SITES,
        "logistics_registrations": common.LOGISTICS_REGISTRATIONS,
        "unique_active_card": "T44",
        "families": families,
    }


def main(argv: list[str] | None = None) -> int:
    return common.run_managed(
        "Freeze the T44 storage work set",
        OUTPUT,
        build=build,
        argv=argv,
    )


if __name__ == "__main__":
    raise SystemExit(main())
