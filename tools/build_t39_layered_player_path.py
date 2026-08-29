#!/usr/bin/env python3
"""Build the non-circular B0/B1/B2 player-path proof for locked T39 production."""
from __future__ import annotations

import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35  # noqa: E402
from tools import t39_common as t39  # noqa: E402

OUTPUT = t39.LAYERED_PLAYER_PATH
T21 = t39.TOOLS / "t21_operand_reachability.json"
SUPPORT = t39.TOOLS / "t39_player_path_support.json"


def build() -> dict[str, Any]:
    baseline_document = t35.load_json(T21)
    baseline = set(
        (baseline_document.get("closure") or {}).get("reachable_identities") or []
    )
    support = t35.load_json(SUPPORT)
    lock = t39.load_production_lock()
    expected_route_keys = set((lock.get("support") or {}).get("route_keys") or [])
    frontier = set(baseline)
    route_rows: list[dict[str, Any]] = []
    seen_route_keys: set[str] = set()
    for route in support.get("routes") or []:
        route_key = f"{route['source_map']}#{int(route['source_recipe'])}"
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
                f"T39 support route {route_key} is circular or unordered: {missing}"
            )
        frontier.update(outputs)
        seen_route_keys.add(route_key)
    if seen_route_keys != expected_route_keys:
        raise ValueError("T39 support routes drifted from production lock")

    player_path = t35.load_json(t39.PLAYER_PATH)
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
            raise ValueError(f"T39 locked relation is not B1/B2 closed: {stable_id}")
        b2.update(outputs)
        seen_stable_ids.add(stable_id)
    if seen_stable_ids != locked_stable_ids:
        raise ValueError("T39 production player path stable ids drifted from lock")

    return {
        "schema_version": 1,
        "status": "T39_LAYERED_PLAYER_PATH_READY",
        "production_lock_sha256": t39.production_lock_sha256(),
        "layers": {
            "b0": {
                "identity_count": len(baseline),
                "source": "tools/t21_operand_reachability.json",
                "excludes_t39_production": True,
                "excludes_t39_support": True,
            },
            "b1": {
                "identity_count": len(frontier),
                "route_count": len(route_rows),
                "source": "B0 + locked support",
            },
            "b2": {
                "identity_count": len(b2),
                "relation_count": len(relation_rows),
                "source": "B1 + locked T39 production",
            },
        },
        "support_routes": route_rows,
        "production_relations": relation_rows,
        "validators": {
            "support_inputs_reachable_before_route": all(
                row["inputs_reachable_before_route"] for row in route_rows
            ),
            "production_inputs_reachable_in_b1": all(
                row["inputs_reachable_in_b1"] for row in relation_rows
            ),
            "production_outputs_registered": all(
                row["outputs_registered"] for row in relation_rows
            ),
            "target_does_not_self_justify_inputs": True,
        },
    }


def main(argv: list[str] | None = None) -> int:
    args = t39.parse_write_check(__doc__, argv)
    document = build()
    if args.write:
        t35.write_stable(OUTPUT, document)
        return 0
    errors = t39.check_document(OUTPUT, document)
    if errors:
        print("\n".join(errors), file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
