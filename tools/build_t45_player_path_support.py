#!/usr/bin/env python3
"""Record T45 B1 worldgen scatter routes for catalog block-object identities."""
from __future__ import annotations

import json
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35
from tools import t45_common as common

OUTPUT = common.PLAYER_PATH_SUPPORT
OUTPUT_ROOT = common.LOCKED_SUPPORT_ROOT
SCATTER_FEATURE = common.SCATTER_FEATURE


def build() -> dict[str, Any]:
    catalog = t35.load_json(common.BLOCK_CATALOG)
    baseline = common.b0_identities()
    routes: list[dict[str, Any]] = []
    already: list[str] = []
    for identity in catalog.get("identities") or []:
        runtime_id = str(identity["runtime_id"])
        item_identity = f"item:{runtime_id}"
        if item_identity in baseline:
            already.append(item_identity)
            continue
        routes.append({
            "input_identities": [],
            "kind": "worldgen_drop",
            "output_identities": [item_identity],
            "output_identity": item_identity,
            "runtime_id": runtime_id,
            "source_map": SCATTER_FEATURE,
            "source_recipe": runtime_id.replace(":", "_").replace("/", "_"),
        })
    routes.sort(key=lambda row: str(row["runtime_id"]))
    already.sort()
    if not routes:
        raise ValueError("T45 B1 scatter must add at least one catalog identity")
    if OUTPUT_ROOT.exists():
        for path in OUTPUT_ROOT.glob("*.json"):
            path.unlink()
    OUTPUT_ROOT.mkdir(parents=True, exist_ok=True)
    declaration = {
        "feature": SCATTER_FEATURE,
        "kind": "worldgen_drop",
        "note": (
            "Empty-input overworld scatter of every locked catalog variant not "
            "already in B0. Creative tabs and GameTest injection are not acquisition."
        ),
        "route_count": len(routes),
        "schema_version": 1,
        "status": "T45_PLAYER_PATH_SUPPORT",
    }
    (OUTPUT_ROOT / "gt_block_object_scatter.json").write_text(
        json.dumps(declaration, ensure_ascii=False, indent=2, sort_keys=True) + "\n",
        encoding="utf-8",
        newline="\n",
    )
    return {
        "b0_already_reachable": already,
        "kind": "worldgen_drop",
        "note": declaration["note"],
        "route_count": len(routes),
        "routes": routes,
        "schema_version": 1,
        "source_map": SCATTER_FEATURE,
        "status": "T45_PLAYER_PATH_SUPPORT",
    }


def main(argv: list[str] | None = None) -> int:
    return common.run_managed(
        "Record T45 B1 worldgen scatter routes",
        OUTPUT,
        build=build,
        argv=argv,
    )


if __name__ == "__main__":
    raise SystemExit(main())
