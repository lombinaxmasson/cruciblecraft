#!/usr/bin/env python3
"""Bind the T48 compile spec to the issued production lock."""
from __future__ import annotations

import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t48_common as common

OUTPUT = common.COMPILE_SPEC


def build() -> dict[str, Any]:
    lock = common.load_production_lock()
    groups = list(common.production_publication_groups())
    forbidden = {
        common.T46_PUBLICATION_GROUP,
        common.T47_PUBLICATION_GROUP_EXACT,
        common.T47_PUBLICATION_GROUP_EXACT_MULTI,
    }
    if forbidden.intersection(groups):
        raise ValueError("T48 compile spec must not reuse T46/T47 publication groups")
    return {
        "generated_by": "python tools/build_t48_recipe_compile_spec.py",
        "lock_path": common.relative(common.PRODUCTION_LOCK),
        "note": "Compile may only emit families listed in this lock.",
        "production_lock_sha256": common.production_lock_sha256(),
        "publication_groups": groups,
        "schema_version": 1,
        "selection_sha256": lock["production"]["selection_sha256"],
        "source_path": common.relative(common.SOURCE),
        "status": "T48_RECIPE_COMPILE_SPEC",
    }


def main(argv: list[str] | None = None) -> int:
    return common.run_managed(
        "Bind the T48 recipe compile spec",
        OUTPUT,
        build=build,
        argv=argv,
    )


if __name__ == "__main__":
    raise SystemExit(main())
