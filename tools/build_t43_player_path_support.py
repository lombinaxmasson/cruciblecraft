#!/usr/bin/env python3
"""Record T43 B1 worldgen scatter routes for catalog stone identities."""
from __future__ import annotations

import json
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35  # noqa: E402
from tools import t43_common as common  # noqa: E402

OUTPUT = common.PLAYER_PATH_SUPPORT
OUTPUT_ROOT = common.LOCKED_SUPPORT_ROOT
T21 = common.T21_REACHABILITY
SCATTER_FEATURE = "cruciblecraft:gt_stone_scatter"


def _b0_identities() -> set[str]:
    document = t35.load_json(T21)
    return set((document.get("closure") or {}).get("reachable_identities") or [])


def build() -> dict[str, Any]:
    catalog = t35.load_json(common.STONE_CATALOG)
    baseline = _b0_identities()
    routes: list[dict[str, Any]] = []
    for identity in catalog.get("identities") or []:
        for variant in identity.get("variants") or []:
            runtime_id = str(variant["runtime_id"])
            item_identity = f"item:{runtime_id}"
            if item_identity in baseline:
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
    if len(routes) != 406:
        raise ValueError(f"T43 B1 scatter must cover 406 catalog variants, got {len(routes)}")
    if OUTPUT_ROOT.exists():
        for path in OUTPUT_ROOT.glob("*.json"):
            path.unlink()
    OUTPUT_ROOT.mkdir(parents=True, exist_ok=True)
    declaration = {
        "feature": SCATTER_FEATURE,
        "kind": "worldgen_drop",
        "note": (
            "Empty-input overworld scatter of every locked catalog variant. "
            "Creative tabs and GameTest injection are not acquisition."
        ),
        "route_count": len(routes),
        "schema_version": 1,
        "status": "T43_PLAYER_PATH_SUPPORT",
    }
    (OUTPUT_ROOT / "gt_stone_scatter.json").write_text(
        json.dumps(declaration, ensure_ascii=False, indent=2, sort_keys=True) + "\n",
        encoding="utf-8",
        newline="\n",
    )
    return {
        "b0_already_reachable": sorted(
            {
                f"item:{variant['runtime_id']}"
                for identity in catalog.get("identities") or []
                for variant in identity.get("variants") or []
                if f"item:{variant['runtime_id']}" in baseline
            }
        ),
        "kind": "worldgen_drop",
        "note": (
            "B1 acquisition is the catalog-backed gt_stone_scatter feature. "
            "Inputs are empty, so routes are not circular with T43 production."
        ),
        "route_count": len(routes),
        "routes": routes,
        "schema_version": 1,
        "source_map": SCATTER_FEATURE,
        "status": "T43_PLAYER_PATH_SUPPORT",
    }


def main(argv: list[str] | None = None) -> int:
    return common.run_managed(
        "Record T43 B1 stone scatter support",
        OUTPUT,
        build=build,
        argv=argv,
    )


if __name__ == "__main__":
    raise SystemExit(main())
