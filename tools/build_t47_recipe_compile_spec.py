#!/usr/bin/env python3
"""Bind the T47 compile spec to the issued production lock."""
from __future__ import annotations

import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t47_common as common

OUTPUT = common.COMPILE_SPEC


def build() -> dict[str, Any]:
    lock = common.load_production_lock()
    groups = list(common.production_publication_groups())
    if tuple(groups) != common.PUBLICATION_GROUPS:
        raise ValueError("T47 compile spec publication groups drifted from lock")
    if common.T46_PUBLICATION_GROUP in groups:
        raise ValueError("T47 compile spec must not reuse cruciblecraft:t46_bath_mte")
    return {
        "generated_by": "python tools/build_t47_recipe_compile_spec.py",
        "lock_path": common.relative(common.PRODUCTION_LOCK),
        "note": "Compile may only emit families listed in this lock.",
        "production_lock_sha256": common.production_lock_sha256(),
        "publication_groups": groups,
        "schema_version": 1,
        "selection_sha256": lock["production"]["selection_sha256"],
        "source_path": common.relative(common.SOURCE),
        "status": "T47_RECIPE_COMPILE_SPEC",
    }


def main(argv: list[str] | None = None) -> int:
    return common.run_managed(
        "Bind the T47 recipe compile spec",
        OUTPUT,
        build=build,
        argv=argv,
    )


if __name__ == "__main__":
    raise SystemExit(main())
