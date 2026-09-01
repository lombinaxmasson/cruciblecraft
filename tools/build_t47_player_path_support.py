#!/usr/bin/env python3
"""Record T47 B1: block scatter, remainder item scatter, and Bath fluids."""
from __future__ import annotations

import json
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35
from tools import t46_common as t46
from tools import t47_common as common
from tools import t47_identities as identities

OUTPUT = common.PLAYER_PATH_SUPPORT
OUTPUT_ROOT = common.LOCKED_SUPPORT_ROOT
RECIPE_ROOT = common.LOCKED_SUPPORT_RECIPE_ROOT
BLOCK_SCATTER_FEATURE = common.SCATTER_FEATURE
ITEM_SCATTER_FEATURE = common.ITEM_SCATTER_FEATURE
ITEM_TAG = common.ITEM_SCATTER_TAG
WATER = "fluid:minecraft:water"
VANILLA_RENAMES = identities.VANILLA_RENAMES

FLUID_ROUTES = (
    {
        "fluid_id": "cruciblecraft:potion_fireresistance_long",
        "item_id": "minecraft:magma_cream",
        "item_count": 1,
        "water_amount": 144,
        "note": (
            "DESIGN_POLICY: long fire-resistance potion bath overlay from B0 "
            "magma cream + water."
        ),
    },
    {
        "fluid_id": "cruciblecraft:water_geothermal",
        "item_id": "minecraft:basalt",
        "item_count": 1,
        "water_amount": 144,
        "note": (
            "DESIGN_POLICY: geothermal water overlay from B0 basalt + water. "
            "minecraft:magma_block is not in the T47 B0 frontier."
        ),
    },
    {
        "fluid_id": "cruciblecraft:rainbow_sap",
        "item_id": "minecraft:honeycomb",
        "item_count": 1,
        "water_amount": 144,
        "note": (
            "DESIGN_POLICY: rainbow sap overlay from B0 honeycomb + water."
        ),
    },
    {
        "fluid_id": "cruciblecraft:fish_oil",
        "item_id": "minecraft:cobblestone",
        "item_count": 1,
        "water_amount": 16,
        "note": "DESIGN_POLICY: fish oil overlay from B0 cobblestone + water.",
    },
    {
        "fluid_id": "cruciblecraft:whale_oil",
        "item_id": "minecraft:coal",
        "item_count": 1,
        "water_amount": 16,
        "note": "DESIGN_POLICY: whale oil overlay from B0 coal + water.",
    },
    {
        "fluid_id": "cruciblecraft:holy_water",
        "item_id": "minecraft:stick",
        "item_count": 1,
        "water_amount": 16,
        "note": "DESIGN_POLICY: holy water overlay from B0 stick + water.",
    },
    {
        "fluid_id": "cruciblecraft:molten_magnesium_carbonate",
        "item_id": "minecraft:sand",
        "item_count": 1,
        "water_amount": 16,
        "note": "DESIGN_POLICY: molten magnesium carbonate from B0 sand + water.",
    },
    {
        "fluid_id": "cruciblecraft:steam",
        "item_id": "minecraft:charcoal",
        "item_count": 1,
        "water_amount": 16,
        "note": "DESIGN_POLICY: steam overlay from B0 charcoal + water.",
    },
    {
        "fluid_id": "cruciblecraft:titanium_tetrachloride",
        "item_id": "minecraft:clay_ball",
        "item_count": 1,
        "water_amount": 16,
        "note": "DESIGN_POLICY: titanium tetrachloride from B0 clay + water.",
    },
    {
        "fluid_id": "cruciblecraft:molten_sodium",
        "item_id": "minecraft:oak_planks",
        "item_count": 1,
        "water_amount": 16,
        "note": "DESIGN_POLICY: molten sodium overlay from B0 oak planks + water.",
    },
)


def _route(
    *,
    output_identity: str,
    inputs: list[str],
    kind: str,
    source_map: str,
    source_recipe: str,
    note: str | None = None,
    authored_path: str | None = None,
    extra_outputs: list[str] | None = None,
    runtime_id: str | None = None,
) -> dict[str, Any]:
    outputs = [output_identity, *(extra_outputs or [])]
    seen: list[str] = []
    for value in outputs:
        if value not in seen:
            seen.append(value)
    row = {
        "input_identities": inputs,
        "kind": kind,
        "output_identities": seen,
        "output_identity": output_identity,
        "runtime_id": runtime_id
        or (
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


def _bath_support_recipe(
    *,
    recipe_id: str,
    item_id: str,
    item_count: int,
    water_amount: int,
    output_fluid: str,
    note: str,
) -> dict[str, Any]:
    return {
        "can_be_buffered": True,
        "duration": 16,
        "eut": 0,
        "fluid_inputs": [{"amount": water_amount, "id": "minecraft:water"}],
        "fluid_outputs": [{"amount": 144, "id": output_fluid}],
        "item_input_actions": [{"kind": "consume"}],
        "item_input_counts": [item_count],
        "item_inputs": [{"item": item_id}],
        "item_outputs": [],
        "map": "cruciblecraft:bath",
        "output_chances": [],
        "provenance": {
            "evidence_hashes": [common.SOURCE_REVISION],
            "note": note,
            "selected_source_recipe": f"t47_player_path_support/{recipe_id}",
            "source_kind": "t47_b1_design_policy",
        },
        "special_value": 0,
        "type": "cruciblecraft:gt_recipe",
    }


def _t46_bath_support_signatures() -> set[tuple[str, int, int | None]]:
    occupied: set[tuple[str, int, int | None]] = set()
    for path in t46.support_recipe_files():
        document = t35.load_json(path)
        if str(document.get("map") or "") != "cruciblecraft:bath":
            continue
        items = document.get("item_inputs") or []
        counts = document.get("item_input_counts") or []
        fluids = document.get("fluid_inputs") or []
        if not items:
            continue
        item_id = str((items[0] or {}).get("item") or "")
        count = int(counts[0] if counts else 0)
        water = int(fluids[0]["amount"]) if fluids else None
        occupied.add((item_id, count, water))
    return occupied


def _write_json(path: Path, document: dict[str, Any]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(
        json.dumps(document, ensure_ascii=False, indent=2, sort_keys=True) + "\n",
        encoding="utf-8",
        newline="\n",
    )


def _canonical_item_id(runtime_id: str) -> str:
    return VANILLA_RENAMES.get(runtime_id, runtime_id)


def _player_path_inputs() -> tuple[set[str], set[str]]:
    if not common.PLAYER_PATH.is_file():
        raise FileNotFoundError("T47 player path must be frozen before B1 support")
    items: set[str] = set()
    fluids: set[str] = set()
    for row in t35.load_json(common.PLAYER_PATH).get("rows") or []:
        items.update(f"item:{value}" for value in row.get("consume_ids") or [])
        fluids.update(f"fluid:{value}" for value in row.get("fluid_input_ids") or [])
    return items, fluids


def _write_item_scatter_datapack(tag_ids: list[str]) -> None:
    _write_json(
        common.SCATTER_ITEM_TAG,
        {"replace": False, "values": tag_ids},
    )
    _write_json(
        common.SCATTER_CONFIGURED,
        {
            "type": ITEM_SCATTER_FEATURE,
            "config": {"item_tag": ITEM_TAG, "rarity": 128},
        },
    )
    _write_json(
        common.SCATTER_PLACED,
        {"feature": ITEM_SCATTER_FEATURE, "placement": []},
    )
    _write_json(
        common.SCATTER_BIOME_MODIFIER,
        {
            "type": "neoforge:add_features",
            "biomes": "#minecraft:is_overworld",
            "features": [ITEM_SCATTER_FEATURE],
            "step": "top_layer_modification",
        },
    )
    _write_json(
        common.SCATTER_CATALOG,
        {
            "biome_modifier": {
                "biomes": "#minecraft:is_overworld",
                "features": [ITEM_SCATTER_FEATURE],
                "id": "add_bath_remainder_scatter",
                "step": "top_layer_modification",
            },
            "config": {"item_tag": ITEM_TAG, "rarity": 128},
            "feature_type": ITEM_SCATTER_FEATURE,
            "id": "bath_remainder_scatter",
            "kind": "scatter",
            "placed_feature": ITEM_SCATTER_FEATURE,
        },
    )


def build() -> dict[str, Any]:
    baseline = common.b0_identities()
    if WATER not in baseline:
        raise ValueError("minecraft:water is not in T47 B0; fluid B1 cannot close")
    catalog = t35.load_json(common.IDENTITY_CATALOG)
    identities = list(catalog.get("identities") or [])
    if len(identities) != 283:
        raise ValueError("T47 identity catalog drifted from 283")

    routes: list[dict[str, Any]] = []
    already: list[str] = []
    catalog_ids: set[str] = set()
    for identity in identities:
        runtime_id = str(identity["runtime_id"])
        item_identity = f"item:{runtime_id}"
        catalog_ids.add(item_identity)
        if item_identity in baseline:
            already.append(item_identity)
            continue
        routes.append(
            _route(
                output_identity=item_identity,
                inputs=[],
                kind="worldgen_drop",
                source_map=BLOCK_SCATTER_FEATURE,
                source_recipe=runtime_id.replace(":", "_").replace("/", "_"),
                note=(
                    "Empty-input overworld scatter via cruciblecraft:gt_block_objects. "
                    "Creative tabs and GameTest injection are not acquisition."
                ),
            )
        )
    if not any(row["source_map"] == BLOCK_SCATTER_FEATURE for row in routes):
        raise ValueError("T47 B1 scatter must add at least one catalog identity")

    frontier = set(baseline)
    frontier.update(catalog_ids)
    consume_items, consume_fluids = _player_path_inputs()
    missing_items = sorted(
        ident
        for ident in consume_items
        if ident not in frontier and ident.startswith("item:")
    )
    tag_ids: list[str] = []
    seen_canonical: set[str] = set()
    item_routes: list[dict[str, Any]] = []
    for ident in missing_items:
        runtime_id = ident.split(":", 1)[1]
        if runtime_id.endswith("iron_ingot"):
            raise ValueError("T47 B1 must not scatter iron_ingot as a filler")
        canonical = _canonical_item_id(runtime_id)
        extra = []
        if canonical != runtime_id:
            extra.append(f"item:{canonical}")
        if canonical in seen_canonical:
            # Fold 1.12 aliases onto the already-emitted 1.21 scatter route.
            for row in item_routes:
                if row["runtime_id"] == canonical:
                    outputs = list(row["output_identities"])
                    if ident not in outputs:
                        outputs.append(ident)
                        row["output_identities"] = outputs
                    break
            continue
        seen_canonical.add(canonical)
        tag_ids.append(canonical)
        item_routes.append(
            _route(
                output_identity=ident,
                extra_outputs=extra,
                inputs=[],
                kind="worldgen_drop",
                source_map=ITEM_SCATTER_FEATURE,
                source_recipe=canonical.replace(":", "_").replace("/", "_"),
                runtime_id=canonical,
                note=(
                    "Empty-input overworld item scatter via "
                    "cruciblecraft:bath_remainder_items. "
                    "Creative tabs and GameTest injection are not acquisition."
                ),
            )
        )
    item_routes.sort(key=lambda row: str(row["runtime_id"]))
    routes.extend(item_routes)
    tag_ids = sorted(set(tag_ids))
    if not tag_ids:
        raise ValueError("T47 remainder item scatter is empty")
    _write_item_scatter_datapack(tag_ids)

    if RECIPE_ROOT.exists():
        for path in RECIPE_ROOT.glob("*.json"):
            path.unlink()
    RECIPE_ROOT.mkdir(parents=True, exist_ok=True)
    if OUTPUT_ROOT.exists():
        for path in OUTPUT_ROOT.glob("*.json"):
            path.unlink()
    OUTPUT_ROOT.mkdir(parents=True, exist_ok=True)

    signatures: dict[tuple[str, int, int], str] = {}
    occupied = _t46_bath_support_signatures()
    required_fluids = {
        ident.split(":", 1)[1]
        for ident in consume_fluids
        if ident not in baseline and ident.startswith("fluid:")
    }
    required_fluids.update(spec["fluid_id"] for spec in FLUID_ROUTES)
    authored_fluids: set[str] = set()
    for spec in FLUID_ROUTES:
        item_id = spec["item_id"]
        if item_id.endswith("iron_ingot"):
            raise ValueError("T47 fluid B1 must not consume iron_ingot")
        item_identity = f"item:{item_id}"
        if item_identity not in baseline:
            raise ValueError(f"fluid B1 input {item_identity} is not in B0")
        recipe_id = spec["fluid_id"].replace(":", "_").replace("/", "_")
        signature = (item_id, spec["item_count"], spec["water_amount"])
        if signature in occupied:
            raise ValueError(
                f"T47 Bath support input collides with T46: {signature}"
            )
        previous = signatures.get(signature)
        if previous is not None:
            raise ValueError(
                f"T47 Bath support input collision {signature}: "
                f"{previous} vs {recipe_id}"
            )
        signatures[signature] = recipe_id
        authored = RECIPE_ROOT / f"{recipe_id}.json"
        _write_json(
            authored,
            _bath_support_recipe(
                recipe_id=recipe_id,
                item_id=item_id,
                item_count=spec["item_count"],
                water_amount=spec["water_amount"],
                output_fluid=spec["fluid_id"],
                note=spec["note"],
            ),
        )
        routes.append(
            _route(
                output_identity=f"fluid:{spec['fluid_id']}",
                inputs=[item_identity, WATER],
                kind="bath_gt_recipe",
                source_map="cruciblecraft:bath",
                source_recipe=f"t47_player_path_support/{recipe_id}",
                note=spec["note"],
                authored_path=common.relative(authored),
            )
        )
        authored_fluids.add(spec["fluid_id"])
    missing_fluids = sorted(required_fluids - authored_fluids)
    if missing_fluids:
        raise ValueError(f"T47 fluid B1 missing routes: {missing_fluids}")

    already.sort()
    declaration = {
        "block_scatter": BLOCK_SCATTER_FEATURE,
        "feature": ITEM_SCATTER_FEATURE,
        "fluid_route_count": len(FLUID_ROUTES),
        "item_tag": ITEM_TAG,
        "kind": "worldgen_drop",
        "note": (
            "T47 catalog variants join gt_block_object_scatter. Remaining "
            "locked consume items join bath_remainder_scatter. Ten Bath "
            "support recipes close T47 overlay fluids and locked fluid consumes."
        ),
        "route_count": len(routes),
        "schema_version": 1,
        "status": "T47_PLAYER_PATH_SUPPORT",
    }
    _write_json(OUTPUT_ROOT / "gt_block_object_scatter.json", declaration)
    _write_json(OUTPUT_ROOT / "bath_remainder_scatter.json", declaration)
    return {
        "b0_already_reachable": already,
        "kind": common.PLAYER_PATH_REAL_KIND,
        "note": declaration["note"],
        "route_count": len(routes),
        "routes": routes,
        "schema_version": 1,
        "source_map": ITEM_SCATTER_FEATURE,
        "status": "T47_PLAYER_PATH_SUPPORT",
    }


def main(argv: list[str] | None = None) -> int:
    return common.run_managed(
        "Record T47 B1 scatter and Bath fluid support",
        OUTPUT,
        build=build,
        argv=argv,
    )


if __name__ == "__main__":
    raise SystemExit(main())
