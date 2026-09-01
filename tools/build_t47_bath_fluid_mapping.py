#!/usr/bin/env python3
"""Freeze T47 Bath fluid overlay for lock-needed unmapped source fluids."""
from __future__ import annotations

import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35
from tools import t47_common as common

OUTPUT = common.FLUID_MAPPING
BUNDLED = common.BUNDLED_FLUID_MAPPING

LOCK_FLUIDS = (
    {
        "source_fluid": "potion.fireresistance.long",
        "cc_fluid_id": "cruciblecraft:potion_fireresistance_long",
        "english_name": "Long Fire Resistance potion",
        "semantic_category": "potion",
        "color_rgb": 0xE49A3A,
    },
    {
        "source_fluid": "watergeothermal",
        "cc_fluid_id": "cruciblecraft:water_geothermal",
        "english_name": "Geothermal water",
        "semantic_category": "water",
        "color_rgb": 0x3D6B8A,
    },
    {
        "source_fluid": "rainbowsap",
        "cc_fluid_id": "cruciblecraft:rainbow_sap",
        "english_name": "Rainbow sap",
        "semantic_category": "sap",
        "color_rgb": 0xC45AD4,
    },
)


def build() -> dict[str, Any]:
    base = common.load_json(common.T46_FLUID_MAPPING)
    frozen_t22 = t35.sha256_file(common.T22_5_FLUID_MAPPING)
    rows = []
    for spec in LOCK_FLUIDS:
        rows.append(
            {
                "b0_reachability": "needs_b1_support",
                "cc_fluid_id": spec["cc_fluid_id"],
                "color_rgb": spec["color_rgb"],
                "container_source_path": None,
                "english_name": spec["english_name"],
                "mapping_evidence": "no_legal_t22_5_or_t46_equivalent; explicit CC fluid",
                "registration_state": "t47_bath_fluid_catalog",
                "semantic_category": spec["semantic_category"],
                "source_fluid": spec["source_fluid"],
            }
        )
    return {
        "counts": {"mapped": len(rows)},
        "frozen_t22_5_fluid_mapping_sha256": frozen_t22,
        "frozen_t46_fluid_mapping_sha256": t35.sha256_file(common.T46_FLUID_MAPPING),
        "generated_by": "python tools/build_t47_bath_fluid_mapping.py",
        "mapping": rows,
        "note": (
            "Overlay only. Does not rewrite t22_5 or T46 fluid mapping. "
            "T46 overlay status remains " + str(base.get("status")) + "."
        ),
        "schema_version": 1,
        "source_revision": common.SOURCE_REVISION,
        "status": "T47_BATH_FLUID_MAPPING",
    }


def write() -> dict[str, Any]:
    document = build()
    t35.write_stable(OUTPUT, document)
    BUNDLED.parent.mkdir(parents=True, exist_ok=True)
    t35.write_stable(BUNDLED, document)
    return document


def main(argv: list[str] | None = None) -> int:
    return common.run_managed(
        "Freeze the T47 Bath fluid overlay",
        OUTPUT,
        build=build,
        write=write,
        argv=argv,
    )


if __name__ == "__main__":
    raise SystemExit(main())
