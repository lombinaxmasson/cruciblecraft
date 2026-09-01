#!/usr/bin/env python3
"""Freeze T49 Bath fluid overlay. Remainder uses T46-T48 acids; no new fluids."""
from __future__ import annotations

import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35
from tools import wave_bath_tiny_purified as common

OUTPUT = common.FLUID_MAPPING


def build() -> dict[str, Any]:
    return {
        "counts": {"mapped": 0},
        "frozen_t22_5_fluid_mapping_sha256": t35.sha256_file(common.T22_5_FLUID_MAPPING),
        "frozen_t46_fluid_mapping_sha256": t35.sha256_file(common.T46_FLUID_MAPPING),
        "frozen_t47_fluid_mapping_sha256": t35.sha256_file(common.T47_FLUID_MAPPING),
        "frozen_t48_fluid_mapping_sha256": t35.sha256_file(common.T48_FLUID_MAPPING),
        "generated_by": "python tools/build_t49_bath_fluid_mapping.py",
        "mapping": [],
        "note": (
            "T49 tiny-purified remainder consumes acids already in T46-T48 B0. "
            "No water placeholder. Overlay stays empty."
        ),
        "schema_version": 1,
        "source_revision": common.SOURCE_REVISION,
        "status": "T49_BATH_FLUID_MAPPING",
    }


def main(argv: list[str] | None = None) -> int:
    return common.run_managed(
        "Freeze the T49 Bath fluid overlay",
        OUTPUT,
        build=build,
        argv=argv,
    )


if __name__ == "__main__":
    raise SystemExit(main())
