#!/usr/bin/env python3
"""Project the pinned GT6 recovery routes needed by the T38 player path.

The three metal routes start from T33 surface-scatter rocks, while blaze starts
from the explicitly seeded vanilla blaze rod. Diamantine is projected from the
pinned GT6 AnyDiamond alias using a vanilla diamond seed. GT6 emits small dust
piles and packs four into one dust; the compact recipes preserve that two-step
path where applicable.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))
TOOLS = ROOT / "tools"
DUMP_ROOT = ROOT / "gt6_dump/gt6_recipe_dump/maps"
MATERIAL_GATE = (
    ROOT / "src/main/resources/data/cruciblecraft/material_registration_gate.json"
)
MATERIALS = (
    ROOT / "src/main/resources/data/cruciblecraft/materials"
)
OUTPUT_ROOT = (
    ROOT
    / "src/main/resources/data/cruciblecraft/recipe/t38_player_path_recovery"
)
OUTPUT = TOOLS / "t38_player_path_recovery.json"

GT6_REVISION = "3703e40308c8c030763fd6297dea8b210d2a77b1"

ROUTES = (
    {
        "material": "vanadium",
        "seed": "cruciblecraft:vanadium/rock",
        "source_map": "gt.recipe.mortar",
        "source_recipe": 3580,
        "map": "cruciblecraft:mortar",
        "source_input": ("gregtech:gt.meta.rockGt", 230, 1),
        "source_output": ("gregtech:gt.meta.dustSmall", 230, 9),
        "output_count": 9,
        "packing_source_recipe": 244,
    },
    {
        "material": "niobium",
        "seed": "cruciblecraft:niobium/rock",
        "source_map": "gt.recipe.mortar",
        "source_recipe": 989,
        "map": "cruciblecraft:mortar",
        "source_input": ("gregtech:gt.meta.rockGt", 410, 1),
        "source_output": ("gregtech:gt.meta.dustSmall", 410, 9),
        "output_count": 9,
        "packing_source_recipe": 10460,
    },
    {
        "material": "tantalum",
        "seed": "cruciblecraft:tantalum/rock",
        "source_map": "gt.recipe.mortar",
        "source_recipe": 2499,
        "map": "cruciblecraft:mortar",
        "source_input": ("gregtech:gt.meta.rockGt", 730, 1),
        "source_output": ("gregtech:gt.meta.dustSmall", 730, 9),
        "output_count": 9,
        "packing_source_recipe": 3409,
    },
    {
        "material": "blaze",
        "seed": "minecraft:blaze_rod",
        "source_map": "gt.recipe.shredder",
        "source_recipe": 26950,
        "map": "cruciblecraft:shredder",
        "source_input": ("minecraft:blaze_rod", 0, 1),
        "source_output": ("gregtech:gt.meta.dustSmall", 8211, 2),
        "output_count": 2,
        "packing_source_recipe": 24273,
    },
)


def _stable(value: Any) -> str:
    return json.dumps(value, ensure_ascii=False, indent=2, sort_keys=True) + "\n"


def _digest(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def _load(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def _source_recipe(source_map: str, index: int) -> dict[str, Any]:
    path = DUMP_ROOT / f"{source_map}.json"
    recipes = _load(path).get("recipes")
    if not isinstance(recipes, list) or not (0 <= index < len(recipes)):
        raise ValueError(f"missing pinned GT6 source {source_map}#{index}")
    recipe = recipes[index]
    if not isinstance(recipe, dict):
        raise ValueError(f"invalid pinned GT6 source {source_map}#{index}")
    return recipe


def _stack_matches(
    stack: Any,
    expected: tuple[str, int, int],
) -> bool:
    return (
        isinstance(stack, dict)
        and stack.get("item") == expected[0]
        and stack.get("meta") == expected[1]
        and stack.get("count") == expected[2]
    )


def _validate_source(route: dict[str, Any]) -> None:
    source = _source_recipe(route["source_map"], route["source_recipe"])
    inputs = source.get("inputs")
    outputs = source.get("outputs")
    if (
        not isinstance(inputs, list)
        or not inputs
        or not _stack_matches(inputs[0], route["source_input"])
        or not isinstance(outputs, list)
        or not outputs
        or not _stack_matches(outputs[0], route["source_output"])
    ):
        raise ValueError(
            f"GT6 {route['source_map']}#{route['source_recipe']} drifted"
        )
    packing = _source_recipe("gt.recipe.boxinator", route["packing_source_recipe"])
    packing_inputs = packing.get("inputs")
    packing_outputs = packing.get("outputs")
    material_meta = route["source_output"][1]
    if (
        not isinstance(packing_inputs, list)
        or not _stack_matches(
            packing_inputs[0],
            ("gregtech:gt.meta.dustSmall", material_meta, 4),
        )
        or not isinstance(packing_outputs, list)
        or not _stack_matches(
            packing_outputs[0],
            ("gregtech:gt.meta.dust", material_meta, 1),
        )
    ):
        raise ValueError(
            "GT6 boxinator small-dust packing route drifted for "
            f"{route['material']}"
        )


def _validate_material_forms(routes: tuple[dict[str, Any], ...]) -> None:
    gate = _load(MATERIAL_GATE)
    materials = gate.get("materials")
    if not isinstance(materials, dict):
        raise ValueError("material registration gate is invalid")
    for route in routes:
        forms = set(materials.get(route["material"], []))
        required = {"small_dust", "dust"}
        if route["seed"].startswith("cruciblecraft:"):
            required.add("rock")
        missing = required - forms
        if missing:
            raise ValueError(
                f"{route['material']} lacks registered forms: {sorted(missing)}"
            )


def _validate_diamantine_alias() -> None:
    diamantine = _load(MATERIALS / "diamantine.json")
    aliases = set(diamantine.get("gt6_metadata", {}).get("aliases") or [])
    if (
        "AnyDiamond" not in aliases
        or diamantine.get("composition") != {"diamond": 1}
    ):
        raise ValueError(
            "Diamantine no longer has the pinned GT6 AnyDiamond alias evidence"
        )


def _recovery_recipe(route: dict[str, Any]) -> dict[str, Any]:
    material = route["material"]
    return {
        "can_be_buffered": True,
        "duration": 36 if material != "blaze" else 32,
        "eut": 16,
        "item_input_counts": [1],
        "item_inputs": [{"item": route["seed"]}],
        "item_outputs": [
            {
                "count": route["output_count"],
                "id": f"cruciblecraft:{material}/small_dust",
            }
        ],
        "map": route["map"],
        "output_chances": [10_000],
        "provenance": {
            "evidence_hashes": [GT6_REVISION],
            "selected_source_recipe": (
                f"gt6_dump/gt6_recipe_dump/maps/{route['source_map']}.json"
                f"#recipes[{route['source_recipe']}]"
            ),
            "source_kind": "gt6_pinned_dump_projection",
        },
        "type": "cruciblecraft:gt_recipe",
    }


def _diamantine_alias_recipe() -> dict[str, Any]:
    return {
        "can_be_buffered": True,
        "duration": 32,
        "eut": 16,
        "item_input_counts": [1],
        "item_inputs": [{"item": "minecraft:diamond"}],
        "item_outputs": [
            {"count": 1, "id": "cruciblecraft:diamantine/dust"}
        ],
        "map": "cruciblecraft:mortar",
        "output_chances": [10_000],
        "provenance": {
            "alias": "AnyDiamond",
            "evidence_hashes": [
                _digest(MATERIALS / "diamantine.json"),
            ],
            "source_kind": "gt6_oredict_alias_projection",
            "source_material": (
                "src/main/resources/data/cruciblecraft/"
                "materials/diamantine.json"
            ),
        },
        "type": "cruciblecraft:gt_recipe",
    }


def _packing_recipe(route: dict[str, Any]) -> dict[str, Any]:
    material = route["material"]
    return {
        "category": "misc",
        "ingredients": [
            {"item": f"cruciblecraft:{material}/small_dust"},
            {"item": f"cruciblecraft:{material}/small_dust"},
            {"item": f"cruciblecraft:{material}/small_dust"},
            {"item": f"cruciblecraft:{material}/small_dust"},
        ],
        "result": {"count": 1, "id": f"cruciblecraft:{material}/dust"},
        "type": "minecraft:crafting_shapeless",
    }


def build() -> tuple[dict[str, Any], dict[Path, str]]:
    _validate_material_forms(ROUTES)
    _validate_diamantine_alias()
    files: dict[Path, str] = {}
    route_rows: list[dict[str, Any]] = []
    for route in ROUTES:
        _validate_source(route)
        material = route["material"]
        recovery_path = OUTPUT_ROOT / f"{material}_recovery.json"
        packing_path = OUTPUT_ROOT / f"{material}_packing.json"
        files[recovery_path] = _stable(_recovery_recipe(route))
        files[packing_path] = _stable(_packing_recipe(route))
        route_rows.append(
            {
                "material": material,
                "seed": route["seed"],
                "recovery_recipe": recovery_path.relative_to(ROOT).as_posix(),
                "packing_recipe": packing_path.relative_to(ROOT).as_posix(),
                "source": {
                    "map": route["source_map"],
                    "recipe": route["source_recipe"],
                    "packing_map": "gt.recipe.boxinator",
                    "packing_recipe": route["packing_source_recipe"],
                },
            }
        )
    diamantine_path = OUTPUT_ROOT / "diamantine_alias_recovery.json"
    files[diamantine_path] = _stable(_diamantine_alias_recipe())
    route_rows.append(
        {
            "material": "diamantine",
            "seed": "minecraft:diamond",
            "recovery_recipe": diamantine_path.relative_to(ROOT).as_posix(),
            "source": {
                "alias": "AnyDiamond",
                "material": "diamantine",
                "source_kind": "gt6_oredict_alias_projection",
            },
        }
    )
    document = {
        "schema_version": 1,
        "status": "T38_PLAYER_PATH_RECOVERY",
        "source_revision": GT6_REVISION,
        "tracked_input_hashes": {
            "material_registration_gate": _digest(MATERIAL_GATE),
            "mortar": _digest(DUMP_ROOT / "gt.recipe.mortar.json"),
            "shredder": _digest(DUMP_ROOT / "gt.recipe.shredder.json"),
            "boxinator": _digest(DUMP_ROOT / "gt.recipe.boxinator.json"),
        },
        "routes": route_rows,
    }
    return document, files


def check() -> list[str]:
    from tools import currentness

    document, files = build()
    errors = currentness.check_rebuilt(OUTPUT, document)
    for path, content in files.items():
        if not path.is_file() or path.read_text(encoding="utf-8") != content:
            errors.append(f"stale: {path.relative_to(ROOT).as_posix()}")
    return errors


def write() -> None:
    document, files = build()
    OUTPUT.write_text(_stable(document), encoding="utf-8")
    OUTPUT_ROOT.mkdir(parents=True, exist_ok=True)
    for path, content in files.items():
        path.write_text(content, encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    if args.write:
        write()
    if args.check:
        errors = check()
        if errors:
            print("\n".join(errors))
            return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
