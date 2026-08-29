#!/usr/bin/env python3
"""Record T44 player-path acquisition from the T44-external B0 baseline."""
from __future__ import annotations

import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t44_common as common  # noqa: E402

OUTPUT = common.PLAYER_PATH


def build() -> dict[str, Any]:
    variants = common.expand_variants()
    visible = [row for row in variants if row["visibility"] == "source_visible"]
    hidden = [row for row in variants if row["visibility"] == "source_hidden"]
    routes = []
    for row in visible:
        routes.append(
            {
                "runtime_id": row["runtime_id"],
                "family": row["family"],
                "acquisition_profile": row["acquisition_profile"],
                "b0_inputs": True,
                "creative_is_not_proof": True,
            }
        )
    return {
        "generated_by": "python tools/build_t44_storage_player_path.py",
        "schema_version": 1,
        "status": "T44_STORAGE_PLAYER_PATH_READY",
        "source_revision": common.SOURCE_REVISION,
        "baseline": "T44-external B0 (T43 closing + machines + hoppers + materials)",
        "source_visible": len(visible),
        "source_hidden": len(hidden),
        "hidden_retained_identities": True,
        "forged_recipes": 0,
        "routes": routes,
        "note": (
            "Creative-tab visibility and GameTest injection are not survival proof. "
            "Hidden plank/mod-wood/logistics rows keep stable ids without recipes."
        ),
    }


def main(argv: list[str] | None = None) -> int:
    return common.run_managed(
        "Write T44 storage player-path evidence",
        OUTPUT,
        build=build,
        argv=argv,
    )


if __name__ == "__main__":
    raise SystemExit(main())
