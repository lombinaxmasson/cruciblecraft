#!/usr/bin/env python3
"""Freeze T48 Bath fluid overlay. Replay currently needs no new fluids."""
from __future__ import annotations

import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35
from tools import t48_common as common

OUTPUT = common.FLUID_MAPPING
BUNDLED = common.BUNDLED_FLUID_MAPPING


def build() -> dict[str, Any]:
    frozen_t22 = t35.sha256_file(common.T22_5_FLUID_MAPPING)
    return {
        "counts": {"mapped": 0},
        "frozen_t22_5_fluid_mapping_sha256": frozen_t22,
        "frozen_t46_fluid_mapping_sha256": t35.sha256_file(common.T46_FLUID_MAPPING),
        "frozen_t47_fluid_mapping_sha256": t35.sha256_file(common.T47_FLUID_MAPPING),
        "generated_by": "python tools/build_t48_bath_fluid_mapping.py",
        "mapping": [],
        "note": (
            "T48 blocked remainder currently has missing_fluids=0. "
            "No water placeholder. Overlay stays empty unless replay adds a lock fluid."
        ),
        "schema_version": 1,
        "source_revision": common.SOURCE_REVISION,
        "status": "T48_BATH_FLUID_MAPPING",
    }


def write() -> dict[str, Any]:
    document = build()
    t35.write_stable(OUTPUT, document)
    BUNDLED.parent.mkdir(parents=True, exist_ok=True)
    t35.write_stable(BUNDLED, document)
    return document


def main(argv: list[str] | None = None) -> int:
    return common.run_managed(
        "Freeze the T48 Bath fluid overlay",
        OUTPUT,
        build=build,
        write=write,
        argv=argv,
    )


if __name__ == "__main__":
    raise SystemExit(main())
