#!/usr/bin/env python3
"""Publication delta for T44 storage recipes and registrations. Not a recipe-wave close."""
from __future__ import annotations

import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t44_common as common  # noqa: E402

OUTPUT = common.PUBLICATION_DELTA


def build() -> dict[str, Any]:
    variants = common.expand_variants()
    authored = sum(
        1 for row in variants if row["visibility"] == "source_visible"
    )
    return {
        "generated_by": "python tools/build_t44_storage_publication_delta.py",
        "schema_version": 1,
        "status": "T44_STORAGE_PUBLICATION_DELTA_READY",
        "source_revision": common.SOURCE_REVISION,
        "recipe_completion_delta": common.COMPLETION_DELTA,
        "recipe_reclassification_delta": common.RECLASSIFICATION_DELTA,
        "storage_registrations": common.STORAGE_REGISTRATIONS,
        "logistics_registrations": common.LOGISTICS_REGISTRATIONS,
        "authored_acquisition_recipes": authored,
        "logical_ordinary_families": 0,
        "eager": 0,
        "lazy": 0,
        "note": (
            "Authored shaped recipes are acquisition only. They do not close "
            "ordinary recipe families and do not change gap 3076."
        ),
    }


def main(argv: list[str] | None = None) -> int:
    return common.run_managed(
        "Write T44 storage publication delta",
        OUTPUT,
        build=build,
        argv=argv,
    )


if __name__ == "__main__":
    raise SystemExit(main())
