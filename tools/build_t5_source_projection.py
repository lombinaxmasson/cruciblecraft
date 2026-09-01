#!/usr/bin/env python3
"""Project one executable pinned-GT6 destination for every T5 terminal dust."""

from __future__ import annotations

import hashlib
import json
import re
import shutil
import sys
from collections import Counter, defaultdict
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))
TOOLS = ROOT / "tools"
DUMP_ROOT = ROOT / "gt6_dump/gt6_recipe_dump"
MAP_ROOT = DUMP_ROOT / "maps"
MATERIAL_ROOT = ROOT / "src/main/resources/data/cruciblecraft/materials"
PREFIX_ROOT = (
    ROOT / "src/main/resources/data/cruciblecraft/material_prefixes"
)
REGISTRATION_GATE = (
    ROOT
    / "src/main/resources/data/cruciblecraft/material_registration_gate.json"
)
CROSS_REFERENCE = TOOLS / "gt6_oredict_cross_reference.json"
NORMALIZED_FLUIDS = TOOLS / "gt6_oredict_fluids_normalized.json"
ORE_CHAIN_CLOSURE = TOOLS / "gt6_ore_chain_closure.json"
READINESS = TOOLS / "t5_chemical_readiness.json"
POLICY = TOOLS / "t5_chemical_policy.json"
FIXED_ITEM_PROJECTION = TOOLS / "t5_source_item_projection.json"
OUTPUT_ROOT = (
    ROOT
    / "src/chemical_recipe_generated/resources/data/cruciblecraft"
)
RECIPE_ROOT = OUTPUT_ROOT / "recipe/chemical"
FLUID_GATE = OUTPUT_ROOT / "chemical_fluid_gate.json"
MANIFEST = TOOLS / "t5_chemical_recipe_manifest.json"
WRAPPER_BUILDER = TOOLS / "build_t5_chemical_recipes.py"

GT6_REPOSITORY = "GregTech6/gregtech6"
GT6_REVISION = "3703e40308c8c030763fd6297dea8b210d2a77b1"
AMBIENT_KELVIN = 300
MAX_STACK_COUNT = 64
MAX_EUT = 1024

# This order is policy: prefer the direct decomposition maps, then the smallest
# source maps that close the remaining pinned routes.
TARGET_MAPS = (
    "gt.recipe.electrolyzer",
    "gt.recipe.centrifuge",
    "gt.recipe.mixer",
    "gt.recipe.autoclave",
    "gt.recipe.bath",
    "gt.recipe.drying",
    "gt.recipe.compressor",
    "gt.recipe.smelter",
    "gt.recipe.roaster",
    "gt.recipe.assembler",
)
CC_MAP = {
    source: source.removeprefix("gt.recipe.")
    for source in TARGET_MAPS
}
CC_MAP["gt.recipe.distillery"] = "distillery"
MAP_LIMITS = {
    "gt.recipe.electrolyzer": (2, 6, 4, 6),
    "gt.recipe.centrifuge": (2, 6, 4, 6),
    "gt.recipe.mixer": (6, 3, 2, 2),
    "gt.recipe.autoclave": (2, 3, 1, 1),
    "gt.recipe.bath": (1, 4, 1, 1),
    "gt.recipe.drying": (1, 1, 0, 1),
    "gt.recipe.compressor": (1, 1, 0, 0),
    "gt.recipe.smelter": (1, 4, 0, 1),
    "gt.recipe.roaster": (2, 4, 2, 2),
    "gt.recipe.assembler": (6, 1, 1, 0),
    "gt.recipe.distillery": (1, 2, 1, 2),
}
BUILTIN_FLUIDS = {
    "water": "minecraft:water",
    "lava": "minecraft:lava",
    "steam": "cruciblecraft:steam",
    "creosote": "cruciblecraft:creosote",
}
_SAFE_PATH = re.compile(r"[^a-z0-9_./-]+")


def load(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def stable(document: Any) -> str:
    return json.dumps(
        document,
        ensure_ascii=False,
        indent=2,
        sort_keys=True,
    ) + "\n"


def digest(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def material_documents() -> dict[str, dict[str, Any]]:
    result: dict[str, dict[str, Any]] = {}
    for path in sorted(MATERIAL_ROOT.glob("*.json")):
        if path.name == "index.json":
            continue
        document = load(path)
        result[document["id"]] = document
    return result


def material_state(document: dict[str, Any]) -> str | None:
    metadata = document.get("gt6_metadata") or {}
    return metadata.get("state")


def prefix_documents() -> dict[str, dict[str, Any]]:
    result: dict[str, dict[str, Any]] = {}
    for name in load(PREFIX_ROOT / "index.json"):
        document = load(PREFIX_ROOT / name)
        result[document["serialized_path"]] = document
    return result


def source_material_index(
    materials: dict[str, dict[str, Any]],
) -> dict[int, str]:
    return {
        int(document["gt6_metadata"]["source_id"]): material_id
        for material_id, document in materials.items()
        if isinstance(document.get("gt6_metadata"), dict)
        and isinstance(
            document["gt6_metadata"].get("source_id"),
            int,
        )
    }


def chance(
    stack: dict[str, Any],
    scale: int,
    *,
    fallback: int | None = None,
) -> int:
    raw = stack.get("chance")
    if raw is None:
        raw = fallback
    if raw is None:
        return 10_000
    if (
        not isinstance(raw, int)
        or isinstance(raw, bool)
        or raw <= 0
        or scale <= 0
    ):
        raise ValueError("invalid output chance")
    return max(1, min(10_000, raw * 10_000 // scale))


def item_form(
    stack: dict[str, Any],
    *,
    source_by_id: dict[int, str],
    gt_prefix_by_item: dict[str, str],
    cc_prefix_by_gt: dict[str, str],
    registration: dict[str, list[str]],
    prefixes: dict[str, dict[str, Any]],
    fixed_items: dict[tuple[str, int], str],
    output: bool,
) -> tuple[dict[str, Any], dict[str, Any]]:
    item = stack.get("item")
    count = stack.get("count", 1)
    meta = stack.get("meta")
    if (
        not isinstance(item, str)
        or not isinstance(count, int)
        or isinstance(count, bool)
        or count <= 0
        or count > MAX_STACK_COUNT
    ):
        raise ValueError("item_stack")

    gt_prefix = gt_prefix_by_item.get(item)
    if gt_prefix is not None:
        if not isinstance(meta, int) or isinstance(meta, bool):
            raise ValueError("item_meta")
        material = source_by_id.get(meta)
        prefix = cc_prefix_by_gt.get(gt_prefix)
        if material is None or prefix is None:
            raise ValueError("item_mapping")
        if prefix not in set(registration.get(material, [])):
            raise ValueError("item_registration")
        if prefix not in prefixes:
            raise ValueError("prefix_registration")
        if output:
            projected = {
                "count": count,
                "id": f"cruciblecraft:{material}/{prefix}",
            }
        else:
            prefix_document = prefixes[prefix]
            namespace = prefix_document.get("tag_namespace", "cruciblecraft")
            directory = prefix_document["tag_directory"]
            projected = {
                "tag": f"{namespace}:{directory}/{material}",
            }
        return projected, {
            "count": count,
            "material": material,
            "prefix": prefix,
            "source_material_id": meta,
            "source_prefix": gt_prefix,
        }

    fixed = (
        fixed_items.get((item, meta))
        if isinstance(meta, int) and not isinstance(meta, bool)
        else None
    )
    if fixed is not None:
        projected = {"id": fixed}
        if output:
            projected["count"] = count
        return projected, {
            "count": count,
            "item": fixed,
            "material": None,
            "prefix": None,
            "source_item": item,
            "source_meta": meta,
        }

    # Vanilla stacks are the only non-material identities the projection may
    # preserve. External-mod item ids would make the committed route unplayable.
    if not item.startswith("minecraft:"):
        raise ValueError("external_item")
    projected = {"id": item} if output else {"item": item}
    if output:
        projected["count"] = count
    return projected, {
        "count": count,
        "item": item,
        "material": None,
        "prefix": None,
    }


def fluid_form(
    stack: dict[str, Any],
    *,
    normalized_by_fluid: dict[str, dict[str, Any]],
    source_by_id: dict[int, str],
    materials: dict[str, dict[str, Any]],
) -> tuple[dict[str, Any], dict[str, Any]]:
    source_fluid = stack.get("fluid")
    amount = stack.get("amount")
    if (
        not isinstance(source_fluid, str)
        or not isinstance(amount, int)
        or isinstance(amount, bool)
        or amount <= 0
    ):
        raise ValueError("fluid_stack")
    if source_fluid in BUILTIN_FLUIDS:
        return {
            "amount": amount,
            "id": BUILTIN_FLUIDS[source_fluid],
        }, {
            "amount": amount,
            "builtin": True,
            "source_fluid": source_fluid,
        }

    normalized = normalized_by_fluid.get(source_fluid)
    if normalized is None:
        raise ValueError("fluid_mapping")
    source_id = int(normalized["material_id"])
    material = source_by_id.get(source_id)
    if material is None:
        raise ValueError("fluid_material")
    document = materials[material]
    molten = source_fluid.startswith("molten.") or source_fluid.startswith(
        "molten "
    )
    if molten:
        fluid_id = f"cruciblecraft:molten_{material}"
        # Some GT6 recipe fluids are not the material's primary ambient fluid.
        # The T5 fluid gate records those route-local molten forms explicitly
        # instead of mutating the imported primary-fluid fact.
        chemical = not bool(document.get("molten_fluid"))
    else:
        state = material_state(document)
        if state not in {"liquid", "gas"}:
            raise ValueError("chemical_fluid_state")
        fluid_id = f"cruciblecraft:{material}"
        chemical = True
    return {
        "amount": amount,
        "id": fluid_id,
    }, {
        "amount": amount,
        "chemical": chemical,
        "material": material,
        "molten_projection": molten and chemical,
        "source_fluid": source_fluid,
        "source_material_id": source_id,
    }


def project_recipe(
    recipe: dict[str, Any],
    target: str | None,
    *,
    source_by_id: dict[int, str],
    gt_prefix_by_item: dict[str, str],
    cc_prefix_by_gt: dict[str, str],
    registration: dict[str, list[str]],
    prefixes: dict[str, dict[str, Any]],
    fixed_items: dict[tuple[str, int], str],
    normalized_by_fluid: dict[str, dict[str, Any]],
    materials: dict[str, dict[str, Any]],
    map_id: str,
) -> tuple[dict[str, Any], dict[str, Any]]:
    if not isinstance(recipe, dict):
        raise ValueError("recipe_record")
    if (
        recipe.get("enabled") is False
        or recipe.get("hidden") is True
        or recipe.get("fake") is True
    ):
        raise ValueError("inactive_recipe")
    all_item_inputs = recipe.get("inputs") or []
    raw_item_inputs = [
        stack
        for stack in all_item_inputs
        if not (
            isinstance(stack, dict)
            and stack.get("item") == "gregapi:gt.integrated_circuit"
            and stack.get("count") == 0
        )
    ]
    raw_item_outputs = recipe.get("outputs") or []
    raw_fluid_inputs = recipe.get("fluidInputs") or []
    raw_fluid_outputs = recipe.get("fluidOutputs") or []
    if not all(
        isinstance(values, list)
        for values in (
            raw_item_inputs,
            raw_item_outputs,
            raw_fluid_inputs,
            raw_fluid_outputs,
        )
    ):
        raise ValueError("recipe_shape")
    limits = MAP_LIMITS[map_id]
    actual = (
        len(raw_item_inputs),
        len(raw_item_outputs),
        len(raw_fluid_inputs),
        len(raw_fluid_outputs),
    )
    if any(value > limit for value, limit in zip(actual, limits)):
        raise ValueError("machine_shape")
    if not raw_item_inputs and not raw_fluid_inputs:
        raise ValueError("no_inputs")
    if not raw_item_outputs and not raw_fluid_outputs:
        raise ValueError("no_outputs")

    item_inputs: list[dict[str, Any]] = []
    item_input_counts: list[int] = []
    source_item_inputs: list[dict[str, Any]] = []
    for stack in raw_item_inputs:
        if not isinstance(stack, dict):
            raise ValueError("item_stack")
        projected, source = item_form(
            stack,
            source_by_id=source_by_id,
            gt_prefix_by_item=gt_prefix_by_item,
            cc_prefix_by_gt=cc_prefix_by_gt,
            registration=registration,
            prefixes=prefixes,
            fixed_items=fixed_items,
            output=False,
        )
        item_inputs.append(projected)
        item_input_counts.append(source["count"])
        source_item_inputs.append(source)

    fluid_inputs: list[dict[str, Any]] = []
    source_fluid_inputs: list[dict[str, Any]] = []
    for stack in raw_fluid_inputs:
        if not isinstance(stack, dict):
            raise ValueError("fluid_stack")
        projected, source = fluid_form(
            stack,
            normalized_by_fluid=normalized_by_fluid,
            source_by_id=source_by_id,
            materials=materials,
        )
        fluid_inputs.append(projected)
        source_fluid_inputs.append(source)

    if target is not None:
        target_dust_input = any(
            row.get("material") == target
            and row.get("prefix") in {"dust", "small_dust", "tiny_dust"}
            for row in source_item_inputs
        )
        target_fluid_input = any(
            row.get("material") == target for row in source_fluid_inputs
        )
        if not target_dust_input and not target_fluid_input:
            raise ValueError("target_not_input")

    item_outputs: list[dict[str, Any]] = []
    source_item_outputs: list[dict[str, Any]] = []
    output_chances: list[int] = []
    scale = recipe.get("chanceScale", 10_000)
    if not isinstance(scale, int) or isinstance(scale, bool):
        raise ValueError("chance_scale")
    source_chances = recipe.get("chances") or []
    source_max_chances = recipe.get("maxChances") or []
    for output_index, stack in enumerate(raw_item_outputs):
        if not isinstance(stack, dict):
            raise ValueError("item_stack")
        projected, source = item_form(
            stack,
            source_by_id=source_by_id,
            gt_prefix_by_item=gt_prefix_by_item,
            cc_prefix_by_gt=cc_prefix_by_gt,
            registration=registration,
            prefixes=prefixes,
            fixed_items=fixed_items,
            output=True,
        )
        item_outputs.append(projected)
        source_item_outputs.append(source)
        fallback = (
            source_chances[output_index]
            if output_index < len(source_chances)
            else None
        )
        chance_scale = (
            source_max_chances[output_index]
            if output_index < len(source_max_chances)
            else scale
        )
        output_chances.append(
            chance(stack, chance_scale, fallback=fallback)
        )

    fluid_outputs: list[dict[str, Any]] = []
    source_fluid_outputs: list[dict[str, Any]] = []
    for stack in raw_fluid_outputs:
        if not isinstance(stack, dict):
            raise ValueError("fluid_stack")
        projected, source = fluid_form(
            stack,
            normalized_by_fluid=normalized_by_fluid,
            source_by_id=source_by_id,
            materials=materials,
        )
        fluid_outputs.append(projected)
        source_fluid_outputs.append(source)

    duration = recipe.get("duration")
    eut = recipe.get("euPerTick", recipe.get("eut"))
    if (
        not isinstance(duration, int)
        or isinstance(duration, bool)
        or duration < 0
        or not isinstance(eut, int)
        or isinstance(eut, bool)
        or eut < 0
        or eut > MAX_EUT
    ):
        raise ValueError("energy")
    document: dict[str, Any] = {
        "can_be_buffered": bool(
            recipe.get(
                "canBeBuffered",
                recipe.get("can_be_buffered", True),
            )
        ),
        "duration": max(16, duration),
        "eut": max(16, eut),
        "map": f"cruciblecraft:{CC_MAP[map_id]}",
        "provenance": {
            "evidence_hashes": [GT6_REVISION],
            "selected_source_recipe": "",
            "source_kind": "gt6_pinned_dump_projection",
        },
        "type": "cruciblecraft:gt_recipe",
    }
    if item_inputs:
        document["item_input_counts"] = item_input_counts
        document["item_inputs"] = item_inputs
    if fluid_inputs:
        document["fluid_inputs"] = fluid_inputs
    if item_outputs:
        document["item_outputs"] = item_outputs
        document["output_chances"] = output_chances
    if fluid_outputs:
        document["fluid_outputs"] = fluid_outputs
    return document, {
        "fluid_inputs": source_fluid_inputs,
        "fluid_outputs": source_fluid_outputs,
        "item_inputs": source_item_inputs,
        "item_outputs": source_item_outputs,
    }


def recipe_target_materials(
    recipe: dict[str, Any],
    *,
    source_by_id: dict[int, str],
    gt_prefix_by_item: dict[str, str],
    normalized_by_fluid: dict[str, dict[str, Any]],
) -> set[str]:
    result: set[str] = set()
    if not isinstance(recipe, dict):
        return result
    for stack in recipe.get("inputs") or []:
        if not isinstance(stack, dict):
            continue
        if stack.get("item") not in gt_prefix_by_item:
            continue
        meta = stack.get("meta")
        if isinstance(meta, int) and not isinstance(meta, bool):
            material = source_by_id.get(meta)
            if material is not None:
                result.add(material)
    for stack in recipe.get("fluidInputs") or []:
        if not isinstance(stack, dict):
            continue
        fluid = normalized_by_fluid.get(stack.get("fluid"))
        if fluid is None:
            continue
        material = source_by_id.get(int(fluid["material_id"]))
        if material is not None:
            result.add(material)
    return result


def fluid_definition(
    material_id: str,
    material: dict[str, Any],
    sources: list[dict[str, Any]],
) -> dict[str, Any]:
    molten_projection = any(
        source.get("molten_projection") for source in sources
    )
    state = "liquid" if molten_projection else material_state(material)
    if state not in {"liquid", "gas"}:
        raise ValueError(
            f"chemical fluid {material_id} has unsupported state {state}"
        )
    density_value = max(
        1,
        round(float(material["thermal"]["density"]) * 1000),
    )
    return {
        "color": material["color"],
        "density": -density_value if state == "gas" else density_value,
        "id": (
            f"molten_{material_id}"
            if molten_projection
            else material_id
        ),
        "material": material_id,
        "source": {
            "path": sources[0]["source_path"],
            "reason": (
                "A selected pinned GT6 source route consumes or emits this "
                "registered non-molten material fluid"
            ),
            "repository": GT6_REPOSITORY,
            "revision": GT6_REVISION,
        },
        "state": state,
        "temperature_kelvin": (
            max(
                AMBIENT_KELVIN,
                round(float(material["thermal"]["melting_point"]) + 273.15),
            )
            if molten_projection
            else AMBIENT_KELVIN
        ),
        "viscosity": (
            6000
            if molten_projection
            else 200 if state == "gas" else 1000
        ),
        "world_placeable": False,
    }


def candidate_score(candidate: dict[str, Any]) -> tuple[Any, ...]:
    source = candidate["source"]
    recipe = candidate["recipe"]
    fluid_amounts = [
        stack["amount"]
        for key in ("fluid_inputs", "fluid_outputs")
        for stack in recipe.get(key, [])
    ]
    return (
        0 if source["direct_terminal_dust"] else 1,
        TARGET_MAPS.index(source["map"]),
        len(recipe.get("fluid_inputs", []))
        + len(recipe.get("fluid_outputs", [])),
        max(fluid_amounts, default=0),
        len(recipe.get("item_inputs", []))
        + len(recipe.get("item_outputs", [])),
        source["recipe_index"],
    )


def build(
    write: bool = True,
    planned_files: dict[Path, bytes] | None = None,
    include_distillery: bool = True,
) -> dict[str, Any]:
    materials = material_documents()
    prefixes = prefix_documents()
    source_by_id = source_material_index(materials)
    registration = load(REGISTRATION_GATE)["materials"]
    cross_reference = load(CROSS_REFERENCE)
    gt_prefix_by_item = cross_reference["prefix_item_to_gt_prefix"]
    cc_prefix_by_gt = cross_reference["gt_prefix_to_cc"]
    fixed_item_document = load(FIXED_ITEM_PROJECTION)
    fixed_items = {
        (row["source"]["item"], int(row["source"]["meta"])): row["cc_item"]
        for row in fixed_item_document["mappings"]
    }
    normalized_by_fluid = {
        row["fluid"]: row
        for row in load(NORMALIZED_FLUIDS)["records"]
    }
    closure = load(ORE_CHAIN_CLOSURE)
    policy = load(POLICY)
    fluid_closure_policy = policy["fluid_closure_routes"]
    if (
        fluid_closure_policy["source"]["revision"] != GT6_REVISION
        or fluid_closure_policy["source"]["index"]
        != str((DUMP_ROOT / "index.json").relative_to(ROOT)).replace("\\", "/")
    ):
        raise ValueError("fluid closure policy source does not match pinned dump")
    fluid_closure_rows = fluid_closure_policy["routes"]
    if len(fluid_closure_rows) != 6:
        raise ValueError("fluid closure policy must select exactly six routes")
    terminal_rows = [
        row
        for row in closure["sifter_dust_without_smelter"]
        if row["classification"] == "t5_chemical"
    ]
    terminal = {row["material"] for row in terminal_rows}
    dead_ends = set(policy["source_dead_end_policy"]["materials"])
    if terminal & dead_ends:
        raise ValueError("source dead ends leaked into the live denominator")
    if len(terminal) != 145 or len(dead_ends) != 17:
        raise ValueError(
            f"unexpected T5 denominator: live={len(terminal)}, "
            f"dead_end={len(dead_ends)}"
        )

    map_documents = {
        map_id: load(MAP_ROOT / f"{map_id}.json")
        for map_id in TARGET_MAPS
    }
    candidates: dict[str, list[dict[str, Any]]] = defaultdict(list)
    rejection_counts: Counter[str] = Counter()
    target_rejections: dict[str, Counter[str]] = defaultdict(Counter)
    target_examples: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for map_id in TARGET_MAPS:
        path = MAP_ROOT / f"{map_id}.json"
        document = map_documents[map_id]
        for index, recipe in enumerate(document["recipes"]):
            recipe_targets = recipe_target_materials(
                recipe,
                source_by_id=source_by_id,
                gt_prefix_by_item=gt_prefix_by_item,
                normalized_by_fluid=normalized_by_fluid,
            )
            for target in terminal & recipe_targets:
                try:
                    projected, source_projection = project_recipe(
                        recipe,
                        target,
                        source_by_id=source_by_id,
                        gt_prefix_by_item=gt_prefix_by_item,
                        cc_prefix_by_gt=cc_prefix_by_gt,
                        registration=registration,
                        prefixes=prefixes,
                        fixed_items=fixed_items,
                        normalized_by_fluid=normalized_by_fluid,
                        materials=materials,
                        map_id=map_id,
                    )
                except ValueError as error:
                    if str(error) != "target_not_input":
                        rejection_counts[str(error)] += 1
                        target_rejections[target][str(error)] += 1
                        if len(target_examples[target]) < 8:
                            target_examples[target].append({
                                "map": map_id,
                                "reason": str(error),
                                "recipe_index": index,
                                "shape": [
                                    len(recipe.get("inputs") or []),
                                    len(recipe.get("outputs") or []),
                                    len(recipe.get("fluidInputs") or []),
                                    len(recipe.get("fluidOutputs") or []),
                                ],
                            })
                    continue
                source_path = path.relative_to(ROOT).as_posix()
                projected["provenance"]["selected_source_recipe"] = (
                    f"{source_path}#recipes[{index}]"
                )
                candidates[target].append({
                    "recipe": projected,
                    "source": {
                        "direct_terminal_dust": any(
                            row.get("material") == target
                            and row.get("prefix")
                            in {"dust", "small_dust", "tiny_dust"}
                            for row in source_projection["item_inputs"]
                        ),
                        "map": map_id,
                        "recipe_index": index,
                        "source_path": source_path,
                    },
                    "source_projection": source_projection,
                })

    selected: dict[str, dict[str, Any]] = {}
    for material in sorted(terminal):
        available = candidates.get(material, [])
        if not available:
            raise ValueError(
                f"no translatable pinned source route for {material}: "
                f"{dict(target_rejections[material])}; "
                f"examples={target_examples[material]}"
            )
        selected[material] = min(available, key=candidate_score)

    planned: dict[Path, bytes] = {}
    generated: list[dict[str, Any]] = []
    map_counts: Counter[str] = Counter()
    fluid_producer_recipes: dict[str, list[str]] = defaultdict(list)
    fluid_consumer_recipes: dict[str, list[str]] = defaultdict(list)
    terminal_fluid_inputs: set[str] = set()
    terminal_fluid_outputs: set[str] = set()
    terminal_fluid_output_amounts: Counter[str] = Counter()
    terminal_fluid_producer_recipes: dict[str, list[str]] = defaultdict(list)
    chemical_fluid_sources: dict[str, list[dict[str, Any]]] = defaultdict(
        list
    )
    for material, candidate in sorted(selected.items()):
        map_id = candidate["source"]["map"]
        route = CC_MAP[map_id]
        output = RECIPE_ROOT / route / f"{material}.json"
        planned[output] = stable(candidate["recipe"]).encode("utf-8")
        map_counts[route] += 1
        recipe_id = f"cruciblecraft:chemical/{route}/{material}"
        for stack in candidate["recipe"].get("fluid_inputs", []):
            fluid_id = stack["id"]
            terminal_fluid_inputs.add(fluid_id)
            fluid_consumer_recipes[fluid_id].append(recipe_id)
        for stack in candidate["recipe"].get("fluid_outputs", []):
            fluid_id = stack["id"]
            terminal_fluid_outputs.add(fluid_id)
            terminal_fluid_output_amounts[fluid_id] += stack["amount"]
            fluid_producer_recipes[fluid_id].append(recipe_id)
            terminal_fluid_producer_recipes[fluid_id].append(recipe_id)
        for direction in ("fluid_inputs", "fluid_outputs"):
            for stack in candidate["source_projection"][direction]:
                if stack.get("chemical"):
                    chemical_fluid_sources[stack["material"]].append({
                        **stack,
                        "direction": direction,
                        "source_path": candidate["source"]["source_path"],
                    })
        generated.append({
            "id": recipe_id,
            "material": material,
            "route": route,
            "source": candidate["source"],
            "source_inputs": {
                "fluids": candidate["source_projection"]["fluid_inputs"],
                "items": candidate["source_projection"]["item_inputs"],
            },
            "source_outputs": {
                "fluids": candidate["source_projection"]["fluid_outputs"],
                "items": candidate["source_projection"]["item_outputs"],
            },
        })

    builtin_fluid_ids = set(BUILTIN_FLUIDS.values())
    previously_input_only = (
        terminal_fluid_inputs
        - terminal_fluid_outputs
        - builtin_fluid_ids
    )
    expected_input_only = set(
        fluid_closure_policy["previously_input_only_fluids"]
    )
    if previously_input_only != expected_input_only:
        raise ValueError(
            "fluid closure policy input-only ledger is stale: "
            f"expected={sorted(expected_input_only)}, "
            f"actual={sorted(previously_input_only)}"
        )

    fluid_closure_generated: list[dict[str, Any]] = []
    fluid_closure_map_counts: Counter[str] = Counter()
    fluid_closure_input_amounts: Counter[str] = Counter()
    seen_closure_ids: set[str] = set()
    for row in fluid_closure_rows:
        closure_id = row["id"]
        map_id = row["map"]
        recipe_index = row["recipe_index"]
        if (
            not isinstance(closure_id, str)
            or not closure_id
            or _SAFE_PATH.search(closure_id)
            or closure_id in seen_closure_ids
        ):
            raise ValueError(f"invalid fluid closure route id: {closure_id!r}")
        seen_closure_ids.add(closure_id)
        if map_id not in map_documents:
            raise ValueError(
                f"unsupported fluid closure source map: {map_id}"
            )
        recipes = map_documents[map_id]["recipes"]
        if (
            not isinstance(recipe_index, int)
            or isinstance(recipe_index, bool)
            or recipe_index < 0
            or recipe_index >= len(recipes)
        ):
            raise ValueError(
                f"invalid fluid closure recipe index: {map_id}#{recipe_index}"
            )
        projected, source_projection = project_recipe(
            recipes[recipe_index],
            None,
            source_by_id=source_by_id,
            gt_prefix_by_item=gt_prefix_by_item,
            cc_prefix_by_gt=cc_prefix_by_gt,
            registration=registration,
            prefixes=prefixes,
            fixed_items=fixed_items,
            normalized_by_fluid=normalized_by_fluid,
            materials=materials,
            map_id=map_id,
        )
        projected_inputs = [
            stack["id"] for stack in projected.get("fluid_inputs", [])
        ]
        projected_outputs = [
            stack["id"] for stack in projected.get("fluid_outputs", [])
        ]
        if projected_inputs != row["expected_fluid_inputs"]:
            raise ValueError(
                f"fluid closure input drift for {closure_id}: "
                f"{projected_inputs}"
            )
        if projected_outputs != row["expected_fluid_outputs"]:
            raise ValueError(
                f"fluid closure output drift for {closure_id}: "
                f"{projected_outputs}"
            )
        closes = set(row["closes"])
        if not closes or not closes <= set(projected_outputs):
            raise ValueError(
                f"fluid closure outputs do not satisfy {closure_id}: "
                f"{sorted(closes)}"
            )

        source_path = (
            MAP_ROOT / f"{map_id}.json"
        ).relative_to(ROOT).as_posix()
        projected["provenance"]["selected_source_recipe"] = (
            f"{source_path}#recipes[{recipe_index}]"
        )
        route = CC_MAP[map_id]
        file_name = f"fluid_closure_{closure_id}.json"
        output = RECIPE_ROOT / route / file_name
        if output in planned:
            raise ValueError(f"duplicate generated recipe path: {output}")
        planned[output] = stable(projected).encode("utf-8")
        recipe_id = (
            f"cruciblecraft:chemical/{route}/fluid_closure_{closure_id}"
        )
        map_counts[route] += 1
        fluid_closure_map_counts[route] += 1
        for stack in projected.get("fluid_inputs", []):
            fluid_id = stack["id"]
            fluid_consumer_recipes[fluid_id].append(recipe_id)
            fluid_closure_input_amounts[fluid_id] += stack["amount"]
        for stack in projected.get("fluid_outputs", []):
            fluid_producer_recipes[stack["id"]].append(recipe_id)
        for direction in ("fluid_inputs", "fluid_outputs"):
            for stack in source_projection[direction]:
                if stack.get("chemical"):
                    chemical_fluid_sources[stack["material"]].append({
                        **stack,
                        "direction": direction,
                        "source_path": source_path,
                    })
        fluid_closure_generated.append({
            "closes": sorted(closes),
            "id": recipe_id,
            "route": route,
            "source": {
                "map": map_id,
                "recipe_index": recipe_index,
                "source_path": source_path,
            },
            "source_inputs": {
                "fluids": source_projection["fluid_inputs"],
                "items": source_projection["item_inputs"],
            },
            "source_outputs": {
                "fluids": source_projection["fluid_outputs"],
                "items": source_projection["item_outputs"],
            },
        })

    missing_producers = sorted(
        expected_input_only - set(fluid_producer_recipes)
    )
    if missing_producers:
        raise ValueError(
            f"fluid closure routes leave input-only fluids: {missing_producers}"
        )
    recoverable_surplus = fluid_closure_policy["recoverable_surplus"]
    surplus_fluid = recoverable_surplus["fluid"]
    terminal_surplus_amount = terminal_fluid_output_amounts[surplus_fluid]
    closure_consumption_amount = fluid_closure_input_amounts[surplus_fluid]
    if (
        recoverable_surplus["classification"]
        != "recoverable_surplus_byproduct"
        or recoverable_surplus["fake_voiding_allowed"]
        or terminal_surplus_amount <= closure_consumption_amount
    ):
        raise ValueError("water_distilled is not a recoverable T5 surplus")

    fluid_gate = {
        "fluids": [
            fluid_definition(
                material,
                materials[material],
                chemical_fluid_sources[material],
            )
            for material in sorted(chemical_fluid_sources)
        ],
        "schema_version": 1,
    }
    planned[FLUID_GATE] = stable(fluid_gate).encode("utf-8")
    distillery_ledger: dict[str, Any] | None = None
    distillery_generated: list[dict[str, Any]] = []
    distillery_ledger_path: Path | None = None
    if include_distillery:
        try:
            from tools import build_t5_distillery_projection as distillery
        except ModuleNotFoundError:
            import build_t5_distillery_projection as distillery
        distillery_ledger, distillery_planned = distillery.plan(
            sys.modules[__name__],
            planned,
            fluid_gate,
        )
        planned.update(distillery_planned)
        distillery_generated = distillery_ledger["generated"]
        distillery_ledger_path = distillery.LEDGER
        map_counts["distillery"] += len(distillery_generated)
    output_hashes = {
        str(path.relative_to(ROOT)).replace("\\", "/"):
            hashlib.sha256(content).hexdigest()
        for path, content in sorted(planned.items())
    }
    output_tree_sha256 = hashlib.sha256(
        stable(output_hashes).encode("utf-8")
    ).hexdigest()
    manifest = {
        "acceptance": {
            "fluid_closure": {
                "all_previously_input_only_have_producers": True,
                "missing_producers": missing_producers,
                "previously_input_only_fluids": sorted(
                    previously_input_only
                ),
                "producer_recipe_ids": {
                    fluid_id: sorted(fluid_producer_recipes[fluid_id])
                    for fluid_id in sorted(previously_input_only)
                },
                "water_distilled": {
                    "classification": recoverable_surplus[
                        "classification"
                    ],
                    "closure_consumption_amount": closure_consumption_amount,
                    "consumer_recipe_ids": sorted(
                        fluid_consumer_recipes[surplus_fluid]
                    ),
                    "fake_voided": False,
                    "recoverable_surplus_amount": (
                        terminal_surplus_amount - closure_consumption_amount
                    ),
                    "terminal_output_amount": terminal_surplus_amount,
                    "terminal_producer_recipe_ids": sorted(
                        terminal_fluid_producer_recipes[surplus_fluid]
                    ),
                },
            },
        },
        "counts": {
            "chemical_fluids": len(chemical_fluid_sources),
            "fluid_closure_map_recipes": dict(
                sorted(fluid_closure_map_counts.items())
            ),
            "fluid_closure_recipes": len(fluid_closure_generated),
            "generated_recipes": (
                len(generated)
                + len(fluid_closure_generated)
                + len(distillery_generated)
            ),
            "map_recipes": dict(sorted(map_counts.items())),
            "distillery_recipes": len(distillery_generated),
            "distillery_source_rows": (
                distillery_ledger["counts"]["source_rows"]
                if distillery_ledger is not None
                else 0
            ),
            "source_dead_ends": len(dead_ends),
            "terminal_dust_recipes": len(generated),
            "terminal_dust_denominator": len(terminal),
            "terminal_dust_recipe_ready": len(generated),
            "terminal_dust_unresolved": len(terminal) - len(generated),
        },
        "dead_ends": [
            {
                "material": material,
                "reason": policy["source_dead_end_policy"]["reason"],
            }
            for material in sorted(dead_ends)
        ],
        "fluid_closure_routes": fluid_closure_generated,
        "distillery_routes": distillery_generated,
        "generated": generated,
        "inputs": {
            CROSS_REFERENCE.relative_to(ROOT).as_posix(): digest(CROSS_REFERENCE),
            NORMALIZED_FLUIDS.relative_to(ROOT).as_posix(): digest(
                NORMALIZED_FLUIDS
            ),
            REGISTRATION_GATE.relative_to(ROOT).as_posix(): digest(
                REGISTRATION_GATE
            ),
            ORE_CHAIN_CLOSURE.relative_to(ROOT).as_posix(): digest(
                ORE_CHAIN_CLOSURE
            ),
            READINESS.relative_to(ROOT).as_posix(): digest(READINESS),
            POLICY.relative_to(ROOT).as_posix(): digest(POLICY),
            FIXED_ITEM_PROJECTION.relative_to(ROOT).as_posix(): digest(
                FIXED_ITEM_PROJECTION
            ),
            **(
                {
                    distillery_ledger_path.relative_to(ROOT).as_posix():
                        hashlib.sha256(
                            planned[distillery_ledger_path]
                        ).hexdigest()
                }
                if distillery_ledger_path is not None
                else {}
            ),
            **{
                (MAP_ROOT / f"{map_id}.json").relative_to(ROOT).as_posix():
                    digest(MAP_ROOT / f"{map_id}.json")
                for map_id in TARGET_MAPS
            },
        },
        "policy": {
            "map_priority": list(TARGET_MAPS),
            "max_eut": MAX_EUT,
            "selection": (
                "one lowest-priority translatable pinned GT6 source recipe "
                "whose declared input names the terminal material"
            ),
            "repository": GT6_REPOSITORY,
            "revision": GT6_REVISION,
        },
        "proof": {
            "proof_tier": "full_replay",
            "builder_sha256": digest(WRAPPER_BUILDER),
            "source_projection_sha256": digest(Path(__file__).resolve()),
            "output_tree_sha256": output_tree_sha256,
        },
        "output_hashes": output_hashes,
        "rejection_counts": dict(sorted(rejection_counts.items())),
        "schema_version": 5,
        "status": "closure_ready",
    }
    planned[MANIFEST] = stable(manifest).encode("utf-8")

    if planned_files is not None:
        planned_files.update(planned)
    if write:
        if RECIPE_ROOT.exists():
            shutil.rmtree(RECIPE_ROOT)
        from tools import atomic_io

        for path, content in sorted(planned.items()):
            atomic_io.write_bytes(path, content)
    return manifest

