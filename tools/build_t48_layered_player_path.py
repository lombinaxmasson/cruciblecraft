#!/usr/bin/env python3
"""Build the non-circular B0/B1/B2 player-path proof for locked T48 production."""
from __future__ import annotations

import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35
from tools import t48_common as t48

OUTPUT = t48.LAYERED_PLAYER_PATH
SUPPORT = t48.PLAYER_PATH_SUPPORT


def build() -> dict[str, Any]:
    baseline = t48.b0_identities()
    support = t35.load_json(SUPPORT)
    if support.get("kind") != t48.PLAYER_PATH_REAL_KIND:
        raise ValueError("T48 support kind is not a real layered B1 path")
    lock = t48.load_production_lock()
    expected_route_keys = set((lock.get("support") or {}).get("route_keys") or [])
    production_outputs: set[str] = set()
    player_path = t35.load_json(t48.PLAYER_PATH)
    for row in player_path.get("rows") or []:
        production_outputs.update(f"item:{value}" for value in row.get("output_ids") or [])
        production_outputs.update(
            f"fluid:{value}" for value in row.get("fluid_output_ids") or []
        )
    frontier = set(baseline)
    route_rows: list[dict[str, Any]] = []
    seen_route_keys: set[str] = set()
    for route in support.get("routes") or []:
        kind = str(route.get("kind") or "")
        if kind in {"b1_declaration", "declared_support", "b1_support"}:
            raise ValueError(f"T48 support route is still a declaration: {kind}")
        route_key = f"{route['source_map']}#{route['source_recipe']}"
        inputs = set(route.get("input_identities") or [])
        tainted = (inputs & production_outputs) - baseline
        if tainted:
            raise ValueError(
                f"T48 support route {route_key} used T48 production outputs as B1: "
                + ", ".join(sorted(tainted))
            )
        outputs = set(route.get("output_identities") or [route["output_identity"]])
        missing = sorted(inputs - frontier)
        route_rows.append({
            "input_identities": sorted(inputs),
            "inputs_reachable_before_route": not missing,
            "kind": kind,
            "missing_inputs": missing,
            "output_identities": sorted(outputs),
            "route_key": route_key,
        })
        if missing:
            raise ValueError(
                f"T48 support route {route_key} is circular or unordered: {missing}"
            )
        frontier.update(outputs)
        seen_route_keys.add(route_key)
    if seen_route_keys != expected_route_keys:
        raise ValueError("T48 support routes drifted from production lock")

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
            raise ValueError(f"T48 locked relation is not B1/B2 closed: {stable_id}")
        b2.update(outputs)
        seen_stable_ids.add(stable_id)
    if seen_stable_ids != locked_stable_ids:
        raise ValueError("T48 production player path stable ids drifted from lock")
    added_b1 = sorted(frontier - baseline)
    added_b2 = sorted(b2 - frontier)
    return {
        "b0": {
            "count": len(baseline),
            "note": "T46+T47 cumulative typed closure without T48 production or support",
        },
        "b1": {
            "added_identities": added_b1,
            "count": len(frontier),
            "route_count": len(route_rows),
            "routes": route_rows,
        },
        "b2": {
            "added_identities": added_b2,
            "count": len(b2),
            "relation_count": len(relation_rows),
        },
        "schema_version": 1,
        "status": "T48_LAYERED_PLAYER_PATH",
    }


def main(argv: list[str] | None = None) -> int:
    return t48.run_managed(
        "Build T48 layered B0/B1/B2 player path",
        OUTPUT,
        build=build,
        argv=argv,
    )


if __name__ == "__main__":
    raise SystemExit(main())
