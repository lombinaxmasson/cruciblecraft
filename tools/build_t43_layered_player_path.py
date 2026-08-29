#!/usr/bin/env python3
"""Build the non-circular B0/B1/B2 player-path proof for locked T43 production."""
from __future__ import annotations

import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35  # noqa: E402
from tools import t43_common as t43  # noqa: E402

OUTPUT = t43.LAYERED_PLAYER_PATH
T21 = t43.T21_REACHABILITY
SUPPORT = t43.PLAYER_PATH_SUPPORT


def build() -> dict[str, Any]:
    baseline_document = t35.load_json(T21)
    baseline = set(
        (baseline_document.get("closure") or {}).get("reachable_identities") or []
    )
    support = t35.load_json(SUPPORT)
    lock = t43.load_production_lock()
    expected_route_keys = set((lock.get("support") or {}).get("route_keys") or [])
    frontier = set(baseline)
    route_rows: list[dict[str, Any]] = []
    seen_route_keys: set[str] = set()
    for route in support.get("routes") or []:
        route_key = f"{route['source_map']}#{route['source_recipe']}"
        inputs = set(route.get("input_identities") or [])
        outputs = set(route.get("output_identities") or [route["output_identity"]])
        missing = sorted(inputs - frontier)
        route_rows.append({
            "input_identities": sorted(inputs),
            "inputs_reachable_before_route": not missing,
            "missing_inputs": missing,
            "output_identities": sorted(outputs),
            "route_key": route_key,
        })
        if missing:
            raise ValueError(
                f"T43 support route {route_key} is circular or unordered: {missing}"
            )
        frontier.update(outputs)
        seen_route_keys.add(route_key)
    if seen_route_keys != expected_route_keys:
        raise ValueError("T43 support routes drifted from production lock")

    player_path = t35.load_json(t43.PLAYER_PATH)
    locked_stable_ids = set((lock.get("production") or {}).get("stable_ids") or [])
    relation_rows: list[dict[str, Any]] = []
    b2 = set(frontier)
    seen_stable_ids: set[str] = set()
    for row in player_path.get("rows") or []:
        stable_id = str(row["stable_id"])
        inputs = {
            *(f"item:{value}" for value in row.get("consume_ids") or []),
            *(f"fluid:{value}" for value in row.get("fluid_input_ids") or []),
        }
        outputs = {
            *(f"item:{value}" for value in row.get("output_ids") or []),
            *(f"fluid:{value}" for value in row.get("fluid_output_ids") or []),
        }
        missing = sorted(inputs - frontier)
        relation_rows.append({
            "inputs_reachable_in_b1": not missing,
            "missing_inputs": missing,
            "outputs_registered": bool(row.get("outputs_registered")),
            "stable_id": stable_id,
        })
        if missing or row.get("alias_fail_closed") or not row.get("outputs_registered"):
            raise ValueError(f"T43 locked relation is not B1/B2 closed: {stable_id}")
        b2.update(outputs)
        seen_stable_ids.add(stable_id)
    if seen_stable_ids != locked_stable_ids:
        raise ValueError("T43 production player path stable ids drifted from lock")
    return {
        "b0": {
            "count": len(baseline),
            "note": "T21/T42 cumulative typed closure without T43 production or support",
        },
        "b1": {
            "added_identities": sorted(frontier - baseline),
            "count": len(frontier),
            "route_count": len(route_rows),
            "routes": route_rows,
        },
        "b2": {
            "added_identities": sorted(b2 - frontier),
            "count": len(b2),
            "relation_count": len(relation_rows),
            "relations": relation_rows,
        },
        "schema_version": 1,
        "status": "T43_LAYERED_PLAYER_PATH_READY",
        "production_lock_sha256": t43.production_lock_sha256(),
    }


def main(argv: list[str] | None = None) -> int:
    return t43.run_managed(
        "Build T43 layered B0/B1/B2 player path",
        OUTPUT,
        build=build,
        argv=argv,
    )


if __name__ == "__main__":
    raise SystemExit(main())
