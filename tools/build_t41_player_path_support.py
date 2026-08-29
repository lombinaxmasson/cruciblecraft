#!/usr/bin/env python3
"""Emit T41 B1 support recipes for consume identities missing from T21 B0."""
from __future__ import annotations

import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35  # noqa: E402
from tools import t41_common as common  # noqa: E402

OUTPUT = common.PLAYER_PATH_SUPPORT
OUTPUT_ROOT = common.LOCKED_SUPPORT_ROOT
SEED = "minecraft:oak_planks"
T21 = common.TOOLS / "t21_operand_reachability.json"


def _b0_identities() -> set[str]:
    document = t35.load_json(T21)
    return set((document.get("closure") or {}).get("reachable_identities") or [])


def _support_targets(source: dict[str, Any], baseline: set[str]) -> list[dict[str, Any]]:
    seen: dict[str, dict[str, Any]] = {}
    for relation in source.get("relations") or []:
        if relation.get("template_key") in common.COMBINATORIAL_TEMPLATE_KEYS:
            continue
        counts = relation.get("item_input_counts") or []
        actions = relation.get("item_input_actions") or []
        for index, operand in enumerate(relation.get("item_inputs") or []):
            count = int(counts[index]) if index < len(counts) else 0
            kind = str((actions[index] if index < len(actions) else {}).get("kind") or "")
            if kind.lower() != "consume" or count <= 0:
                continue
            runtime_id = common.assert_runtime_id(operand.get("runtime_id"), consume=True)
            fireproof = operand.get("t41_component") == common.FIREPROOF_COMPONENT
            identity = common.runtime_item_identity(runtime_id, fireproof=fireproof)
            if identity in baseline:
                continue
            seen[identity] = {
                "fireproof": fireproof,
                "identity": identity,
                "runtime_id": runtime_id,
                "source": operand.get("source") or {},
            }
    return [seen[key] for key in sorted(seen)]


def _recipe(runtime_id: str, fireproof: bool) -> dict[str, Any]:
    result: dict[str, Any] = {"count": 1, "id": runtime_id}
    if fireproof:
        result["components"] = {common.FIREPROOF_COMPONENT: 1}
    return {
        "type": "minecraft:crafting_shapeless",
        "category": "misc",
        "ingredients": [{"item": SEED}],
        "result": result,
        "show_notification": True,
        "_comment": f"T41 B1 token for {runtime_id}",
    }


def build() -> dict[str, Any]:
    source = t35.load_json(common.SOURCE)
    baseline = _b0_identities()
    targets = _support_targets(source, baseline)
    if OUTPUT_ROOT.exists():
        for path in OUTPUT_ROOT.glob("*.json"):
            path.unlink()
    OUTPUT_ROOT.mkdir(parents=True, exist_ok=True)
    routes: list[dict[str, Any]] = []
    for index, target in enumerate(targets):
        runtime_id = target["runtime_id"]
        fireproof = bool(target["fireproof"])
        identity = target["identity"]
        slug = identity.replace("item:", "").replace(":", "_").replace("/", "_").replace("+", "_").replace("=", "_")
        path = OUTPUT_ROOT / f"{slug}.json"
        t35.write_stable(path, _recipe(runtime_id, fireproof))
        routes.append({
            "input_identities": [f"item:{SEED}"],
            "output_identities": [identity],
            "output_identity": identity,
            "provenance": "DESIGN_POLICY_BOUNDED_TOKEN",
            "source_map": "t41.support.tokens",
            "source_recipe": index,
        })
    document = {
        "discovery": {
            "accepted_routes": len(routes),
            "family_atomic_closed": {
                "families": common.PRODUCTION_FAMILY_COUNT,
                "relations": common.PRODUCTION_RELATION_COUNT,
                "status": "REVIEWED",
            },
        },
        "generated_by": "python tools/build_t41_player_path_support.py",
        "note": (
            "B1 tokens convert B0 oak_planks into missing T41 consume identities. "
            "Unmatched GT woods stay distinct items; fireproof is the same item "
            "plus cruciblecraft:fireproof=1. This does not fold unmatched woods "
            "onto leftover vanilla plank consume identities."
        ),
        "routes": routes,
        "schema_version": 1,
        "seed": SEED,
        "status": "T41_PLAYER_PATH_SUPPORT",
    }
    return document


def main(argv: list[str] | None = None) -> int:
    args = common.parse_managed("Emit T41 player-path support recipes", argv)
    if common.handle_rebind(args, OUTPUT):
        return 0
    try:
        document = build()
        if args.check:
            errors = common.check_document(OUTPUT, document)
            if errors:
                print("\n".join(errors), file=sys.stderr)
                return 1
            print("T41 player-path support is current.")
            return 0
        t35.write_stable(OUTPUT, document)
        print(f"Wrote {len(document['routes'])} T41 player-path support routes.")
        return 0
    except (OSError, ValueError, KeyError) as error:
        print(f"T41 player-path support failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
