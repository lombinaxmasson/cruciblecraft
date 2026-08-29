#!/usr/bin/env python3
"""Freeze the T44 storage production lock and append-only runtime id map."""
from __future__ import annotations

import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t44_common as common  # noqa: E402

OUTPUT = common.PRODUCTION_LOCK


def build() -> dict[str, Any]:
    variants = common.expand_variants()
    runtime_ids = common.variant_ids(variants)
    digest = common.selection_sha256(runtime_ids)
    if digest != common.EXPECTED_SELECTION_SHA256:
        raise ValueError("T44 production selection_sha256 drifted")
    mappings = [
        {
            "runtime_id": row["runtime_id"],
            "source_site_id": row["source_site_id"],
            "source_legacy_id": row["source_legacy_id"],
            "family": row["family"],
            "expansion_key": row["expansion_key"],
            "behavior_profile": row["behavior_profile"],
            "visibility": row["visibility"],
            "counts_toward_storage_624": row["counts_toward_storage_624"],
        }
        for row in variants
    ]
    return {
        "append_only": True,
        "generated_by": "python tools/build_t44_storage_production_lock.py",
        "note": (
            "624 storage registrations plus independent logistics 1/1. "
            "Runtime ids are source numeric identities, not localization names. "
            "+aID sites use steel as DESIGN_POLICY representative material."
        ),
        "owner": common.OWNER,
        "representative_material": common.REPRESENTATIVE_MATERIAL,
        "recipe_opening_execution_gap": common.OPENING_EXECUTION_GAP,
        "recipe_closing_execution_gap": common.CLOSING_EXECUTION_GAP,
        "recipe_completion_delta": common.COMPLETION_DELTA,
        "schema_version": 1,
        "selection_sha256": digest,
        "source_revision": common.SOURCE_REVISION,
        "status": "T44_STORAGE_PRODUCTION_LOCK",
        "storage_source_sites": common.STORAGE_SOURCE_SITES,
        "storage_registrations": common.STORAGE_REGISTRATIONS,
        "logistics_source_sites": common.LOGISTICS_SOURCE_SITES,
        "logistics_registrations": common.LOGISTICS_REGISTRATIONS,
        "runtime_ids": runtime_ids,
        "mappings": mappings,
    }


def main(argv: list[str] | None = None) -> int:
    return common.run_managed(
        "Freeze the T44 storage production lock",
        OUTPUT,
        build=build,
        argv=argv,
    )


if __name__ == "__main__":
    raise SystemExit(main())
