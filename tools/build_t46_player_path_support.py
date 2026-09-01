#!/usr/bin/env python3
"""Record T46 B1 acquisition: item scatter plus B0-driven Bath support recipes."""
from __future__ import annotations

import json
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35
from tools import t46_common as common
from tools import t46_identities as identities

OUTPUT = common.PLAYER_PATH_SUPPORT
OUTPUT_ROOT = common.LOCKED_SUPPORT_ROOT
RECIPE_ROOT = common.LOCKED_SUPPORT_RECIPE_ROOT
SCATTER_FEATURE = "cruciblecraft:bath_mte_scatter"
ITEM_TAG = "cruciblecraft:bath_mte_items"
WATER = "fluid:minecraft:water"
WATER_OIL = 72
WATER_FLOWER = 144
WATER_MIXED = 288


def _source_consume_identities() -> set[str]:
    source = common.load_json(common.SOURCE)
    identities_found: set[str] = set()
    for relation in source.get("relations") or []:
        for operand, count, action in zip(
            relation.get("item_inputs") or [],
            relation.get("item_input_counts") or [],
            relation.get("item_input_actions") or [],
            strict=False,
        ):
            kind = str((action or {}).get("kind") or "").lower()
            runtime = operand.get("runtime_id")
            if kind == "consume" and int(count or 0) > 0 and runtime:
                identities_found.add(f"item:{runtime}")
        for operand in relation.get("fluid_inputs") or []:
            runtime = operand.get("runtime_id")
            if runtime:
                identities_found.add(f"fluid:{runtime}")
    return identities_found


def _route(
    *,
    output_identity: str,
    inputs: list[str],
    kind: str,
    source_map: str,
    source_recipe: str,
    note: str | None = None,
    authored_path: str | None = None,
) -> dict[str, Any]:
    row = {
        "input_identities": inputs,
        "kind": kind,
        "output_identities": [output_identity],
        "output_identity": output_identity,
        "runtime_id": (
            output_identity.split(":", 1)[1]
            if ":" in output_identity
            else output_identity
        ),
        "source_map": source_map,
        "source_recipe": source_recipe,
    }
    if note:
        row["note"] = note
    if authored_path:
        row["authored_path"] = authored_path
    return row


def _dye_input_item(fluid_id: str) -> tuple[str, int, str]:
    path = fluid_id.split(":", 1)[1]
    if path == "indigo":
        return "minecraft:lapis_lazuli", 1, "DESIGN_POLICY: indigo uses B0 lapis + water."
    if path == "squid_ink":
        return "minecraft:ink_sac", 1, "SOURCE_DERIVED: squid ink from vanilla ink sac + water."
    if path.startswith("dye_flower_"):
        color = path.removeprefix("dye_flower_")
        item_id = identities.DYE_FLOWER_ITEMS[color]
        return item_id, 1, "DESIGN_POLICY: overlay flower dye from vanilla harvestable + water."
    if path.startswith("dye_watermixed_"):
        color = path.removeprefix("dye_watermixed_")
        item_id = identities.DYE_FLOWER_ITEMS[color]
        return (
            item_id,
            2,
            "DESIGN_POLICY: watermixed overlay uses two of the same harvestable + extra water.",
        )
    raise ValueError(f"unmapped T46 overlay fluid {fluid_id}")


def _bath_support_recipe(
    *,
    recipe_id: str,
    item_id: str,
    item_count: int,
    water_amount: int | None,
    output_fluid: str,
    note: str,
) -> dict[str, Any]:
    item_inputs = [{"item": item_id}]
    document: dict[str, Any] = {
        "type": "cruciblecraft:gt_recipe",
        "map": "cruciblecraft:bath",
        "duration": 16,
        "eut": 0,
        "special_value": 0,
        "can_be_buffered": True,
        "item_inputs": item_inputs,
        "item_input_counts": [item_count],
        "item_input_actions": [{"kind": "consume"}],
        "item_outputs": [],
        "output_chances": [],
        "fluid_inputs": (
            [{"id": "minecraft:water", "amount": water_amount}]
            if water_amount
            else []
        ),
        "fluid_outputs": [{"id": output_fluid, "amount": 144}],
        "provenance": {
            "source_kind": "t46_b1_design_policy",
            "selected_source_recipe": f"t46_player_path_support/{recipe_id}",
            "evidence_hashes": [common.SOURCE_REVISION],
            "note": note,
        },
    }
    return document


def _input_signature(
    item_id: str,
    item_count: int,
    water_amount: int | None,
) -> tuple[str, int, int | None]:
    return (item_id, item_count, water_amount)


def _write_json(path: Path, document: dict[str, Any]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(
        json.dumps(document, ensure_ascii=False, indent=2, sort_keys=True) + "\n",
        encoding="utf-8",
        newline="\n",
    )


def build() -> dict[str, Any]:
    baseline = common.b0_identities()
    consume = _source_consume_identities()
    missing = sorted(identity for identity in consume if identity not in baseline)
    if WATER not in baseline:
        raise ValueError("minecraft:water is not in T46 B0; dye B1 cannot close")
    routes: list[dict[str, Any]] = []
    oil_ids = {f"fluid:{fluid_id}" for fluid_id in common.B0_FLUID_IDS}
    if RECIPE_ROOT.exists():
        for path in RECIPE_ROOT.glob("*.json"):
            path.unlink()
    RECIPE_ROOT.mkdir(parents=True, exist_ok=True)
    if OUTPUT_ROOT.exists():
        for path in OUTPUT_ROOT.glob("*.json"):
            path.unlink()
    OUTPUT_ROOT.mkdir(parents=True, exist_ok=True)
    signatures: dict[tuple[str, int, int | None], str] = {}

    for fluid_id, input_id in identities.B0_OIL_ROUTES.items():
        if input_id not in baseline:
            raise ValueError(f"oil B1 input {input_id} is not in B0")
        recipe_id = fluid_id.replace(":", "_").replace("/", "_")
        item_id = input_id.split(":", 1)[1]
        item_count = 2 if fluid_id.endswith("seed_oil") else 1
        note = identities.B0_OIL_ROUTE_NOTES[fluid_id]
        authored = RECIPE_ROOT / f"{recipe_id}.json"
        water_amount = None if fluid_id.endswith("molten_midasium") else WATER_OIL
        signature = _input_signature(item_id, item_count, water_amount)
        previous = signatures.get(signature)
        if previous is not None:
            raise ValueError(
                f"T46 Bath support input collision {signature}: "
                f"{previous} vs {recipe_id}"
            )
        signatures[signature] = recipe_id
        _write_json(
            authored,
            _bath_support_recipe(
                recipe_id=recipe_id,
                item_id=item_id,
                item_count=item_count,
                water_amount=water_amount,
                output_fluid=fluid_id,
                note=note,
            ),
        )
        inputs = [input_id] if water_amount is None else [input_id, WATER]
        routes.append(
            _route(
                output_identity=f"fluid:{fluid_id}",
                inputs=inputs,
                kind="mixer_gt_recipe",
                source_map="cruciblecraft:bath",
                source_recipe=f"t46_player_path_support/{recipe_id}",
                note=note,
                authored_path=common.relative(authored),
            )
        )
    oil_closed = len(routes)
    if oil_closed != len(common.B0_FLUID_IDS):
        raise ValueError(f"T46 oil B1 routes {oil_closed} != 7")

    overlay = common.load_json(common.FLUID_MAPPING)
    overlay_ids = [
        str(row["cc_fluid_id"])
        for row in overlay.get("mapping") or []
        if str(row.get("cc_fluid_id") or "")
    ]
    if len(overlay_ids) != common.EXPECTED_FLUID_OVERLAY:
        raise ValueError("T46 overlay fluid count drifted")
    for fluid_id in overlay_ids:
        item_id, item_count, note = _dye_input_item(fluid_id)
        item_identity = f"item:{item_id}"
        if item_identity not in baseline:
            raise ValueError(f"dye B1 input {item_identity} is not in B0")
        recipe_id = fluid_id.replace(":", "_").replace("/", "_")
        authored = RECIPE_ROOT / f"{recipe_id}.json"
        water_amount = WATER_MIXED if "watermixed" in fluid_id else WATER_FLOWER
        signature = _input_signature(item_id, item_count, water_amount)
        previous = signatures.get(signature)
        if previous is not None:
            raise ValueError(
                f"T46 Bath support input collision {signature}: "
                f"{previous} vs {recipe_id}"
            )
        signatures[signature] = recipe_id
        _write_json(
            authored,
            _bath_support_recipe(
                recipe_id=recipe_id,
                item_id=item_id,
                item_count=item_count,
                water_amount=water_amount,
                output_fluid=fluid_id,
                note=note,
            ),
        )
        routes.append(
            _route(
                output_identity=f"fluid:{fluid_id}",
                inputs=[item_identity, WATER],
                kind="mixer_gt_recipe",
                source_map="cruciblecraft:bath",
                source_recipe=f"t46_player_path_support/{recipe_id}",
                note=note,
                authored_path=common.relative(authored),
            )
        )

    scatter_items: list[str] = []
    for identity_id in missing:
        if identity_id in oil_ids or identity_id.startswith("fluid:"):
            continue
        if not identity_id.startswith("item:"):
            raise ValueError(f"T46 B1 cannot scatter non-item {identity_id}")
        scatter_items.append(identity_id.split(":", 1)[1])
        routes.append(
            _route(
                output_identity=identity_id,
                inputs=[],
                kind="worldgen_drop",
                source_map=SCATTER_FEATURE,
                source_recipe=identity_id.replace(":", "_").replace("/", "_"),
                note="Empty-input overworld item scatter; not a block or BE.",
            )
        )
    scatter_items = sorted(set(scatter_items))
    tag_document = {
        "replace": False,
        "values": scatter_items,
    }
    _write_json(common.SCATTER_ITEM_TAG, tag_document)
    routes.sort(key=lambda row: str(row["output_identity"]))
    fluid_routes = [row for row in routes if row["kind"] == "mixer_gt_recipe"]
    item_routes = [row for row in routes if row["kind"] == "worldgen_drop"]
    note = (
        "B1 item acquisition is empty-input worldgen_drop scatter. "
        "B1 fluids are TIME Bath support recipes driven by B0 harvestables. "
        "T46 production outputs are not reused as B1 inputs."
    )
    return {
        "authored_support_entries": len(fluid_routes),
        "b0_already_reachable": sorted(consume & baseline),
        "fluid_route_count": len(fluid_routes),
        "item_route_count": len(item_routes),
        "item_tag": ITEM_TAG,
        "kind": common.PLAYER_PATH_REAL_KIND,
        "note": note,
        "oil_b1_count": oil_closed,
        "route_count": len(routes),
        "routes": routes,
        "schema_version": 1,
        "scatter_feature": SCATTER_FEATURE,
        "scatter_item_count": len(scatter_items),
        "source_map": SCATTER_FEATURE,
        "status": "T46_PLAYER_PATH_SUPPORT",
        "worldgen": {
            "biome_modifier": common.relative(common.SCATTER_BIOME_MODIFIER),
            "catalog": common.relative(common.SCATTER_CATALOG),
            "configured_feature": common.relative(common.SCATTER_CONFIGURED),
            "item_tag": common.relative(common.SCATTER_ITEM_TAG),
            "placed_feature": common.relative(common.SCATTER_PLACED),
        },
    }


def main(argv: list[str] | None = None) -> int:
    return common.run_managed(
        "Record T46 B1 player-path support routes",
        OUTPUT,
        build=build,
        argv=argv,
    )


if __name__ == "__main__":
    raise SystemExit(main())
