#!/usr/bin/env python3
"""Freeze the T46 Bath fluid overlay for the 30 unmapped source fluids."""
from __future__ import annotations

import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35
from tools import t46_common as common
from tools import t46_identities as identities

OUTPUT = common.FLUID_MAPPING
BUNDLED = common.BUNDLED_FLUID_MAPPING


def _missing_source_fluids() -> list[str]:
    selected = {str(row["family_id"]) for row in common.overlay_bath_mte_families()}
    blocker = common.load_json(common.T42_BLOCKER)
    fluids: set[str] = set()
    for row in blocker.get("families") or []:
        if str(row.get("family_id") or "") not in selected:
            continue
        for fluid in row.get("missing_fluids") or []:
            if isinstance(fluid, str) and fluid:
                fluids.add(fluid)
    ordered = sorted(fluids)
    if len(ordered) != common.EXPECTED_FLUID_OVERLAY:
        raise ValueError(f"T46 missing fluids {len(ordered)} != 30")
    return ordered


def _frozen_t22_ids() -> set[str]:
    mapping = common.load_json(common.T22_5_FLUID_MAPPING)
    return {
        str(row.get("fluid") or "")
        for row in mapping.get("mapping") or []
        if isinstance(row, dict)
    }


def build() -> dict[str, Any]:
    frozen = _frozen_t22_ids()
    rows = []
    for source in _missing_source_fluids():
        if source in frozen:
            raise ValueError(f"{source} is already in frozen t22_5_fluid_mapping")
        runtime = identities.fluid_runtime_id(source)
        rows.append(
            {
                "b0_reachability": "needs_b1_support",
                "cc_fluid_id": runtime,
                "container_source_path": None,
                "english_name": identities.fluid_english_name(source),
                "mapping_evidence": "no_legal_t22_5_equivalent; explicit CC fluid",
                "registration_state": "t46_bath_fluid_catalog",
                "semantic_category": "bath_dye_or_ink",
                "source_fluid": source,
            }
        )
    return {
        "counts": {"mapped": len(rows)},
        "frozen_t22_5_fluid_mapping_sha256": t35.sha256_file(common.T22_5_FLUID_MAPPING),
        "generated_by": "python tools/build_t46_bath_fluid_mapping.py",
        "mapping": rows,
        "note": (
            "Overlay only. Does not rewrite tools/t22_5_fluid_mapping.json. "
            "These 30 source fluids have no proven CC/vanilla equivalent."
        ),
        "schema_version": 1,
        "source_revision": common.SOURCE_REVISION,
        "status": "T46_BATH_FLUID_MAPPING",
    }


def write() -> dict[str, Any]:
    document = build()
    t35.write_stable(OUTPUT, document)
    BUNDLED.parent.mkdir(parents=True, exist_ok=True)
    t35.write_stable(BUNDLED, document)
    return document


def check() -> list[str]:
    errors = common.check_document(OUTPUT, build())
    if BUNDLED.is_file():
        if common.load_json(BUNDLED) != common.load_json(OUTPUT):
            errors.append("bundled T46 fluid mapping drifted from tools mapping")
    else:
        errors.append("missing bundled T46 fluid mapping")
    return errors


def main(argv: list[str] | None = None) -> int:
    return common.run_managed(
        "Freeze the T46 Bath fluid overlay",
        OUTPUT,
        build=build,
        write=write,
        check=check,
        argv=argv,
    )


if __name__ == "__main__":
    raise SystemExit(main())
