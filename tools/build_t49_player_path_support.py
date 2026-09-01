#!/usr/bin/env python3
"""Record T49 B1: tiny-washed item scatter. No Bath fluid support recipes."""
from __future__ import annotations

import json
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35
from tools import wave_bath_tiny_purified as common

OUTPUT = common.PLAYER_PATH_SUPPORT
OUTPUT_ROOT = common.LOCKED_SUPPORT_ROOT
RECIPE_ROOT = common.LOCKED_SUPPORT_RECIPE_ROOT
ITEM_SCATTER_FEATURE = common.ITEM_SCATTER_FEATURE
ITEM_TAG = common.ITEM_SCATTER_TAG


def _route(
    *,
    output_identity: str,
    inputs: list[str],
    kind: str,
    source_map: str,
    source_recipe: str,
    note: str | None = None,
    runtime_id: str | None = None,
) -> dict[str, Any]:
    row = {
        "input_identities": inputs,
        "kind": kind,
        "output_identities": [output_identity],
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
    return row


def _write_json(path: Path, document: dict[str, Any]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(
        json.dumps(document, ensure_ascii=False, indent=2, sort_keys=True) + "\n",
        encoding="utf-8",
        newline="\n",
    )


def _player_path_inputs() -> tuple[set[str], set[str]]:
    if not common.PLAYER_PATH.is_file():
        raise FileNotFoundError("T49 player path must be frozen before B1 support")
    items: set[str] = set()
    fluids: set[str] = set()
    for row in t35.load_json(common.PLAYER_PATH).get("rows") or []:
        items.update(f"item:{value}" for value in row.get("consume_ids") or [])
        fluids.update(f"fluid:{value}" for value in row.get("fluid_input_ids") or [])
    return items, fluids


def _write_item_scatter_datapack(tag_ids: list[str]) -> None:
    _write_json(common.SCATTER_ITEM_TAG, {"replace": False, "values": tag_ids})
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
                "id": "add_bath_tiny_washed_scatter",
                "step": "top_layer_modification",
            },
            "config": {"item_tag": ITEM_TAG, "rarity": 128},
            "feature_type": ITEM_SCATTER_FEATURE,
            "id": "bath_tiny_washed_scatter",
            "kind": "scatter",
            "placed_feature": ITEM_SCATTER_FEATURE,
        },
    )


def build() -> dict[str, Any]:
    baseline = common.b0_identities()
    consume_items, consume_fluids = _player_path_inputs()
    missing_fluids = sorted(
        ident for ident in consume_fluids if ident not in baseline
    )
    if missing_fluids:
        raise ValueError(f"T49 lock fluids are not in T48 B0: {missing_fluids}")
    missing_items = sorted(
        ident
        for ident in consume_items
        if ident not in baseline and ident.startswith("item:")
    )
    tag_ids: list[str] = []
    routes: list[dict[str, Any]] = []
    for ident in missing_items:
        runtime_id = ident.split(":", 1)[1]
        if runtime_id.endswith("iron_ingot"):
            raise ValueError("T49 B1 must not scatter iron_ingot as a filler")
        tag_ids.append(runtime_id)
        routes.append(
            _route(
                output_identity=ident,
                inputs=[],
                kind="worldgen_drop",
                source_map=ITEM_SCATTER_FEATURE,
                source_recipe=runtime_id.replace(":", "_").replace("/", "_"),
                runtime_id=runtime_id,
                note=(
                    "Empty-input overworld item scatter via "
                    "cruciblecraft:bath_tiny_washed_items. "
                    "Creative tabs and GameTest injection are not acquisition."
                ),
            )
        )
    routes.sort(key=lambda row: str(row["runtime_id"]))
    tag_ids = sorted(set(tag_ids))
    if not tag_ids:
        raise ValueError("T49 tiny-washed item scatter is empty")
    _write_item_scatter_datapack(tag_ids)

    if RECIPE_ROOT.exists():
        for path in RECIPE_ROOT.glob("*.json"):
            path.unlink()
    RECIPE_ROOT.mkdir(parents=True, exist_ok=True)
    if OUTPUT_ROOT.exists():
        for path in OUTPUT_ROOT.glob("*.json"):
            path.unlink()
    OUTPUT_ROOT.mkdir(parents=True, exist_ok=True)

    declaration = {
        "feature": ITEM_SCATTER_FEATURE,
        "fluid_route_count": 0,
        "item_tag": ITEM_TAG,
        "kind": "worldgen_drop",
        "note": (
            "T49 tiny_washed_crushed_ore consume items join "
            "bath_tiny_washed_scatter. Acids are already in T46-T48 B0. "
            "No Bath support recipes. T49 production outputs are not B1 inputs."
        ),
        "route_count": len(routes),
        "schema_version": 1,
        "status": "T49_PLAYER_PATH_SUPPORT",
    }
    _write_json(OUTPUT_ROOT / "bath_tiny_washed_scatter.json", declaration)
    already = sorted(ident for ident in consume_items if ident in baseline)
    return {
        "b0_already_reachable": already,
        "kind": common.PLAYER_PATH_REAL_KIND,
        "note": declaration["note"],
        "route_count": len(routes),
        "routes": routes,
        "schema_version": 1,
        "source_map": ITEM_SCATTER_FEATURE,
        "status": "T49_PLAYER_PATH_SUPPORT",
    }


def main(argv: list[str] | None = None) -> int:
    return common.run_managed(
        "Record T49 B1 tiny-washed scatter",
        OUTPUT,
        build=build,
        argv=argv,
    )


if __name__ == "__main__":
    raise SystemExit(main())
