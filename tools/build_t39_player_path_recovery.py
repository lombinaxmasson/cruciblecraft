#!/usr/bin/env python3
"""Project pinned GT6 mortar and boxinator recovery routes for T39 player path.

Rock-seeded materials with registered dust forms get a two-step path:
rock -> small_dust (mortar) and 4 small_dust -> dust (boxinator packing).
Routes are discovered from T39 player-path gap taxonomy, validated against
material registration gate + T39 required forms, and pinned to the local
GT6 dump. Materials without dump evidence or required forms are skipped.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import sys
from pathlib import Path
from typing import Any

_ROOT = Path(__file__).resolve().parents[1]
if str(_ROOT) not in sys.path:
    sys.path.insert(0, str(_ROOT))

from tools import t39_common as t39  # noqa: E402

ROOT = t39.ROOT
TOOLS = t39.TOOLS
DUMP_ROOT = ROOT / "gt6_dump/gt6_recipe_dump/maps"
MATERIAL_GATE = t39.MATERIAL_REGISTRATION_GATE_JSON
MATERIALS_DIR = ROOT / "src/main/resources/data/cruciblecraft/materials"
OUTPUT_ROOT = t39.PLAYER_PATH_RECOVERY_ROOT
OUTPUT = TOOLS / "t39_player_path_recovery.json"
FLUID_BLOCKERS = TOOLS / "t39_player_path_fluid_blockers.json"
GAPS = TOOLS / "t39_player_path_gaps.json"
REQUIRED_FORMS = t39.REQUIRED_FORMS
T21 = ROOT / "tools/t21_operand_reachability.json"

GT6_REVISION = t39.SOURCE_REVISION

RESOURCE_ROOTS: tuple[Path, ...] = (
    ROOT / "src/main/resources",
    ROOT / "src/generated/resources",
    ROOT / "src/ore_chain_generated/resources",
    ROOT / "src/component_rule_generated/resources",
    ROOT / "src/chemical_recipe_generated/resources",
    ROOT / "src/hydrocarbon_recipe_generated/resources",
    ROOT / "src/chemical_recipe_generated/resources",
    ROOT / "src/t39_support_generated/resources",
)

PLAYER_PATH = TOOLS / "t39_player_path.json"
CONSUME_FORMS = (
    "centrifuged_crushed_ore",
    "dust",
    "small_dust",
    "tiny_centrifuged_crushed_ore",
)
CONSUME_RE = re.compile(r"^cruciblecraft:([^/]+)/(.+)$")


def _rock_seeded_consume_materials(player_path: dict[str, Any]) -> set[str]:
    materials: set[str] = set()
    for row in player_path.get("rows") or []:
        for consume_id in row.get("consume_ids") or []:
            match = CONSUME_RE.match(str(consume_id))
            if not match:
                continue
            material, form = match.group(1), match.group(2)
            if form in CONSUME_FORMS:
                materials.add(material)
    return materials


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


def _stack_matches(stack: Any, expected: tuple[str, int, int]) -> bool:
    return (
        isinstance(stack, dict)
        and stack.get("item") == expected[0]
        and stack.get("meta") == expected[1]
        and stack.get("count") == expected[2]
    )


def _material_source_id(material: str) -> int:
    path = MATERIALS_DIR / f"{material}.json"
    if not path.is_file():
        raise ValueError(f"missing material document for {material}")
    meta = _load(path).get("gt6_metadata")
    if not isinstance(meta, dict):
        raise ValueError(f"{material} lacks GT6 metadata")
    source_id = meta.get("source_id")
    if not isinstance(source_id, int) or source_id < 0:
        raise ValueError(f"{material} lacks nonnegative GT6 source_id")
    return source_id


def _effective_forms(
    material: str,
    gate_materials: dict[str, list[str]],
    required_forms: dict[str, list[str]],
) -> set[str]:
    forms = set(gate_materials.get(material, []))
    forms.update(required_forms.get(material, []))
    return forms


def _forms_valid(forms: set[str]) -> bool:
    if "rock" not in forms or "dust" not in forms:
        return False
    return "small_dust" in forms or "dust_div72" in forms


def _discover_materials(
    reachable: set[str],
    gate_materials: dict[str, list[str]],
    required_forms: dict[str, list[str]],
    player_path: dict[str, Any],
) -> tuple[list[str], list[dict[str, str]]]:
    materials = _rock_seeded_consume_materials(player_path)

    selected: list[str] = []
    skipped: list[dict[str, str]] = []
    for material in sorted(materials):
        rock_identity = f"item:cruciblecraft:{material}/rock"
        if rock_identity not in reachable:
            skipped.append(
                {
                    "material": material,
                    "reason": "rock_not_in_t21_closure",
                }
            )
            continue
        forms = _effective_forms(material, gate_materials, required_forms)
        if not _forms_valid(forms):
            skipped.append(
                {
                    "material": material,
                    "reason": "missing_rock_small_dust_or_dust_forms",
                    "forms": ",".join(sorted(forms)),
                }
            )
            continue
        if "small_dust" not in forms:
            skipped.append(
                {
                    "material": material,
                    "reason": "mortar_outputs_small_dust_but_form_unregistered",
                    "forms": ",".join(sorted(forms)),
                }
            )
            continue
        selected.append(material)
    return selected, skipped


def _find_mortar(source_id: int) -> tuple[int, int]:
    path = DUMP_ROOT / "gt.recipe.mortar.json"
    recipes = _load(path).get("recipes")
    if not isinstance(recipes, list):
        raise ValueError("GT6 mortar dump is invalid")
    for index, recipe in enumerate(recipes):
        if not isinstance(recipe, dict):
            continue
        inputs = recipe.get("inputs") or []
        outputs = recipe.get("outputs") or []
        if (
            isinstance(inputs, list)
            and inputs
            and isinstance(outputs, list)
            and outputs
            and _stack_matches(
                inputs[0],
                ("gregtech:gt.meta.rockGt", source_id, 1),
            )
            and outputs[0].get("item") == "gregtech:gt.meta.dustSmall"
            and outputs[0].get("meta") == source_id
        ):
            count = outputs[0].get("count")
            if isinstance(count, int) and count > 0:
                return index, count
    raise ValueError(
        f"no GT6 mortar rockGt->{source_id} dustSmall route in pinned dump"
    )


def _find_boxinator(source_id: int) -> int:
    path = DUMP_ROOT / "gt.recipe.boxinator.json"
    recipes = _load(path).get("recipes")
    if not isinstance(recipes, list):
        raise ValueError("GT6 boxinator dump is invalid")
    for index, recipe in enumerate(recipes):
        if not isinstance(recipe, dict):
            continue
        inputs = recipe.get("inputs") or []
        outputs = recipe.get("outputs") or []
        if (
            isinstance(inputs, list)
            and inputs
            and isinstance(outputs, list)
            and outputs
            and inputs[0].get("item") == "gregtech:gt.meta.dustSmall"
            and inputs[0].get("meta") == source_id
            and inputs[0].get("count") == 4
            and _stack_matches(
                outputs[0],
                ("gregtech:gt.meta.dust", source_id, 1),
            )
        ):
            return index
    raise ValueError(
        f"no GT6 boxinator 4x dustSmall->{source_id} dust route in pinned dump"
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


def _recovery_recipe(route: dict[str, Any]) -> dict[str, Any]:
    material = route["material"]
    return {
        "can_be_buffered": True,
        "duration": 36,
        "eut": 16,
        "item_input_counts": [1],
        "item_inputs": [{"item": route["seed"]}],
        "item_outputs": [
            {
                "count": route["output_count"],
                "id": f"cruciblecraft:{material}/small_dust",
            }
        ],
        "map": "cruciblecraft:mortar",
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


def _route_for_material(material: str) -> dict[str, Any]:
    source_id = _material_source_id(material)
    mortar_index, output_count = _find_mortar(source_id)
    packing_index = _find_boxinator(source_id)
    return {
        "material": material,
        "seed": f"cruciblecraft:{material}/rock",
        "source_map": "gt.recipe.mortar",
        "source_recipe": mortar_index,
        "source_input": ("gregtech:gt.meta.rockGt", source_id, 1),
        "source_output": ("gregtech:gt.meta.dustSmall", source_id, output_count),
        "output_count": output_count,
        "packing_source_recipe": packing_index,
    }


def _iter_recipe_files() -> list[tuple[Path, dict[str, Any]]]:
    seen: set[str] = set()
    rows: list[tuple[Path, dict[str, Any]]] = []
    for resource_root in RESOURCE_ROOTS:
        recipe_dir = resource_root / "data/cruciblecraft/recipe"
        if not recipe_dir.is_dir():
            continue
        for dirpath, _dirnames, filenames in os.walk(recipe_dir):
            for filename in filenames:
                if not filename.endswith(".json"):
                    continue
                path = Path(dirpath) / filename
                if "t39_player_path_recovery" in path.as_posix():
                    continue
                key = path.relative_to(resource_root).as_posix()
                if key in seen:
                    continue
                seen.add(key)
                try:
                    document = _load(path)
                except (json.JSONDecodeError, OSError):
                    continue
                if isinstance(document, dict) and document.get("type"):
                    rows.append((path, document))
    return rows


def _fluid_ids_from_recipe(recipe: dict[str, Any]) -> set[str]:
    fluid_ids: set[str] = set()
    for side in ("fluid_inputs", "fluid_outputs", "fluidInputs", "fluidOutputs"):
        for stack in recipe.get(side) or []:
            if not isinstance(stack, dict):
                continue
            fluid_id = stack.get("id") or stack.get("fluid")
            if isinstance(fluid_id, str) and fluid_id:
                fluid_ids.add(fluid_id)
    return fluid_ids


def build_fluid_blockers() -> dict[str, Any]:
    gaps = _load(GAPS)
    t21 = _load(T21)
    reachable = set(t21["closure"]["reachable_identities"])
    fluid_rows = gaps.get("fluid_input_ids") or []
    unreachable_fluids: list[dict[str, Any]] = []
    for row in fluid_rows:
        if not isinstance(row, dict):
            continue
        fluid_id = str(row.get("id", ""))
        if not fluid_id or f"fluid:{fluid_id}" in reachable:
            continue
        unreachable_fluids.append(row)

    producers: dict[str, list[str]] = {}
    for path, recipe in _iter_recipe_files():
        rel = path.relative_to(ROOT).as_posix()
        for fluid_id in _fluid_ids_from_recipe(recipe):
            producers.setdefault(fluid_id, []).append(rel)

    already_produced: list[dict[str, Any]] = []
    truly_unmodeled: list[dict[str, Any]] = []
    for row in unreachable_fluids:
        fluid_id = str(row["id"])
        recipe_paths = sorted(set(producers.get(fluid_id, [])))
        entry = {
            "count": row.get("count", 0),
            "id": fluid_id,
            "in_t21_closure": False,
            "producer_recipe_count": len(recipe_paths),
            "producer_recipes": recipe_paths[:5],
        }
        if recipe_paths:
            already_produced.append(entry)
        else:
            truly_unmodeled.append(entry)

    return {
        "schema_version": 1,
        "status": "T39_PLAYER_PATH_FLUID_BLOCKERS",
        "reachability_source": gaps.get("reachability_source"),
        "counts": {
            "unreachable_fluid_ids": len(unreachable_fluids),
            "already_produced_by_cc_recipes": len(already_produced),
            "truly_unmodeled": len(truly_unmodeled),
        },
        "already_produced_by_cc_recipes_but_missing_from_t21": already_produced,
        "truly_unmodeled": truly_unmodeled,
        "notes": {
            "classification": (
                "Fluids listed under already_produced have at least one committed "
                "CC recipe mentioning the fluid id, but the fluid identity is "
                "still outside the current T21 closure. truly_unmodeled fluids "
                "have no committed CC recipe producer yet."
            ),
        },
    }


def build() -> tuple[dict[str, Any], dict[Path, str], dict[str, Any]]:
    if not (DUMP_ROOT / "gt.recipe.mortar.json").is_file():
        raise FileNotFoundError("GT6 mortar dump is missing under gt6_dump/")
    if not (DUMP_ROOT / "gt.recipe.boxinator.json").is_file():
        raise FileNotFoundError("GT6 boxinator dump is missing under gt6_dump/")

    t21 = _load(T21)
    reachable = set(t21["closure"]["reachable_identities"])
    player_path = _load(PLAYER_PATH)
    gate_materials = (_load(MATERIAL_GATE).get("materials") or {})
    required_forms = (_load(REQUIRED_FORMS).get("required_forms") or {})

    materials, skipped = _discover_materials(
        reachable,
        gate_materials,
        required_forms,
        player_path,
    )

    files: dict[Path, str] = {}
    route_rows: list[dict[str, Any]] = []
    dump_skipped: list[dict[str, str]] = []

    for material in materials:
        try:
            route = _route_for_material(material)
            _validate_source(route)
        except ValueError as exc:
            dump_skipped.append({"material": material, "reason": str(exc)})
            continue

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
                    "source_material_id": route["source_input"][1],
                    "mortar_output_count": route["output_count"],
                },
            }
        )

    document = {
        "schema_version": 1,
        "status": "T39_PLAYER_PATH_RECOVERY",
        "source_revision": GT6_REVISION,
        "discovery": {
            "taxonomy_category": "rock_seeded_t39_consumes",
            "candidate_materials": len(materials) + len(skipped),
            "selected_materials": len(route_rows),
            "skipped_form_or_reachability": skipped,
            "skipped_missing_dump_route": dump_skipped,
        },
        "tracked_input_hashes": {
            "material_registration_gate": _digest(MATERIAL_GATE),
            "mortar": _digest(DUMP_ROOT / "gt.recipe.mortar.json"),
            "boxinator": _digest(DUMP_ROOT / "gt.recipe.boxinator.json"),
            "t39_player_path_gaps": _digest(GAPS),
            "t39_required_forms": _digest(REQUIRED_FORMS),
            "t21_operand_reachability": _digest(T21),
        },
        "routes": route_rows,
    }
    fluid_blockers = build_fluid_blockers()
    return document, files, fluid_blockers


def check() -> list[str]:
    document, files, fluid_blockers = build()
    errors: list[str] = []
    if not OUTPUT.is_file() or _load(OUTPUT) != document:
        errors.append(f"stale: {OUTPUT.relative_to(ROOT).as_posix()}")
    if not FLUID_BLOCKERS.is_file() or _load(FLUID_BLOCKERS) != fluid_blockers:
        errors.append(f"stale: {FLUID_BLOCKERS.relative_to(ROOT).as_posix()}")
    for path, content in files.items():
        if not path.is_file() or path.read_text(encoding="utf-8") != content:
            errors.append(f"stale: {path.relative_to(ROOT).as_posix()}")
    return errors


def _clear_legacy_recovery_tree() -> None:
    if not t39.LEGACY_PLAYER_PATH_RECOVERY_ROOT.is_dir():
        return
    for path in t39.LEGACY_PLAYER_PATH_RECOVERY_ROOT.glob("*.json"):
        path.unlink()
    leftover = t39.LEGACY_PLAYER_PATH_RECOVERY_ROOT
    while leftover != ROOT / "src/main/resources" and leftover.is_dir():
        try:
            leftover.rmdir()
        except OSError:
            break
        leftover = leftover.parent


def write() -> None:
    _clear_legacy_recovery_tree()
    document, files, fluid_blockers = build()
    OUTPUT.write_text(_stable(document), encoding="utf-8")
    FLUID_BLOCKERS.write_text(_stable(fluid_blockers), encoding="utf-8")
    OUTPUT_ROOT.mkdir(parents=True, exist_ok=True)
    wanted = set(files)
    for path, content in files.items():
        path.write_text(content, encoding="utf-8")
    for path in OUTPUT_ROOT.glob("*.json"):
        if path not in wanted:
            path.unlink()


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
