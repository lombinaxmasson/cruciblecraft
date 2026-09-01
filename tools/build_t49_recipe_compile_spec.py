#!/usr/bin/env python3
"""Bind the T49 compile spec to the issued production lock."""
from __future__ import annotations

import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import wave_bath_tiny_purified as common

OUTPUT = common.COMPILE_SPEC


def build() -> dict[str, Any]:
    lock = common.load_production_lock()
    groups = list(common.production_publication_groups())
    if common.FORBIDDEN_PUBLICATION_GROUPS.intersection(groups):
        raise ValueError("T49 compile spec must not reuse T46/T47/T48 publication groups")
    if groups != [common.PUBLICATION_GROUP_EXACT_MULTI]:
        raise ValueError("T49 compile spec must use only t49_bath_exact_multi")
    return {
        "generated_by": "python tools/build_t49_recipe_compile_spec.py",
        "lock_path": common.relative(common.PRODUCTION_LOCK),
        "note": "Compile may only emit families listed in this lock.",
        "production_lock_sha256": common.production_lock_sha256(),
        "publication_groups": groups,
        "schema_version": 1,
        "selection_sha256": lock["production"]["selection_sha256"],
        "source_path": common.relative(common.SOURCE),
        "status": "T49_RECIPE_COMPILE_SPEC",
    }


def main(argv: list[str] | None = None) -> int:
    return common.run_managed(
        "Bind the T49 recipe compile spec",
        OUTPUT,
        build=build,
        argv=argv,
    )


if __name__ == "__main__":
    raise SystemExit(main())
