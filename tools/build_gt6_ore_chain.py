#!/usr/bin/env python3
"""Bake the L1b-selected six-stage ore chain into concrete GTRecipe JSON."""
from __future__ import annotations

import argparse
import copy
import hashlib
import json
import math
import re
import shutil
import sys
from collections import Counter, defaultdict
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
TOOLS = ROOT / "tools"
sys.path.insert(0, str(ROOT))

from tools import compare_gt6_recipes as compare  # noqa: E402

SELECTED = TOOLS / "gt6_l1b_selected.json"
REFERENCE = TOOLS / "gt6_recipe_normalized_reference.json"
LOCAL_ARTIFACT_MANIFEST = TOOLS / "local_artifact_manifest.json"
INDEX_OUT = TOOLS / "gt6_ore_chain.json"
OPERANDS_OUT = TOOLS / "gt6_ore_chain_operands.json"
REGISTRATION_GATE = (
    ROOT
    / "src/main/resources/data/cruciblecraft/material_registration_gate.json"
)
ROASTER_COMPACT_ACQUISITION = TOOLS / "roaster_source_backed_acquisition.json"
PREFIX_ROOT = (
    ROOT / "src/main/resources/data/cruciblecraft/material_prefixes"
)
OUTPUT_ROOT = (
    ROOT
    / "src/ore_chain_generated/resources/data/cruciblecraft/recipe/ore_chain"
)

FAMILY_TO_MAP = {
    "crush_raw_to_crushed": "crusher",
    "crush_ore_block_to_crushed": "crusher",
    "chain_sluice": "sluice",
    "chain_centrifuge": "centrifuge",
    "chain_shredder": "shredder",
    "chain_sifter": "sifter",
    "chain_smelter": "smelter",
}
STAGE_ORDER = (
    "crush_raw_to_crushed",
    "chain_sluice",
    "chain_centrifuge",
    "chain_shredder",
    "chain_sifter",
    "chain_smelter",
)
STAGE_FORMS = {
    "crush_raw_to_crushed": ("raw_ore", "crushed_ore"),
    "crush_ore_block_to_crushed": ("ore", "crushed_ore"),
    "chain_sluice": ("crushed_ore", "washed_crushed_ore"),
    "chain_centrifuge": (
        "washed_crushed_ore",
        "centrifuged_crushed_ore",
    ),
    "chain_shredder": (
        "centrifuged_crushed_ore",
        "purified_dust",
    ),
    "chain_sifter": ("purified_dust", "dust"),
    "chain_smelter": ("dust", "ingot"),
}
ORE_BLOCK_ROUTE_FAMILY = "crush_ore_block_to_crushed"
ORE_BLOCK_OUTPUT_COUNT = 5
ORE_BLOCK_SOURCE_KIND = "high_version_ore_block_projection"
ACCEPTANCE_MATERIALS = ("copper", "tin", "iron", "gold", "tungsten")
COMPAT_SHORTCUT_GROUP = "cruciblecraft:compat_shortcut"
MACHINE_LIMITS = {
    "crusher": (1, 1, 0, 0),
    "sluice": (1, 4, 1, 0),
    "centrifuge": (1, 4, 0, 0),
    "shredder": (1, 4, 0, 0),
    "sifter": (1, 4, 0, 0),
    "smelter": (1, 1, 0, 0),
}


def load(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def directory_sha256(root: Path) -> str:
    entries = [
        (path.relative_to(root).as_posix(), sha256(path))
        for path in sorted(root.rglob("*.json"))
    ]
    return value_hash(entries, 64)


def stable_json(value: Any) -> str:
    return json.dumps(value, ensure_ascii=False, sort_keys=True, indent=2) + "\n"


def compact_json(value: Any) -> str:
    return json.dumps(
        value, ensure_ascii=False, sort_keys=True, separators=(",", ":")
    ) + "\n"


def value_hash(value: Any, length: int = 20) -> str:
    encoded = json.dumps(
        value, ensure_ascii=False, sort_keys=True, separators=(",", ":")
    ).encode("utf-8")
    return hashlib.sha256(encoded).hexdigest()[:length]


def prefix_definitions() -> dict[str, dict[str, Any]]:
    definitions = {}
    for filename in load(PREFIX_ROOT / "index.json"):
        document = load(PREFIX_ROOT / filename)
        definitions[Path(filename).stem] = document
    return definitions


def worldgen_ore_materials(
    materials: dict[str, dict[str, Any]] | None = None,
) -> set[str]:
    """Derive the ledger from semantic vein sources, never runtime block ids."""
    from tools import build_gt6_veins as vein_builder

    capabilities = None
    if materials is not None:
        capabilities = {
            material_id: {
                "factual": set(material["_factual_forms"]),
                "registered": set(material["_resolved_forms"]),
            }
            for material_id, material in materials.items()
        }
    veins = vein_builder.load_veins(capabilities=capabilities)
    return {
        entry["material"]
        for vein in veins
        for layer in vein_builder.LAYERS
        for entry in vein[layer]
    }


def material_resource(
    resource: compare.Resource,
    materials: dict[str, dict[str, Any]],
) -> tuple[str, str] | None:
    if resource.kind != "item" or resource.id.count(":") != 1:
        return None
    material_id, form = resource.id.split(":", 1)
    if material_id not in materials:
        return None
    return material_id, form


def ingredient_json(
    resource: compare.Resource,
    materials: dict[str, dict[str, Any]],
    prefixes: dict[str, dict[str, Any]],
) -> dict[str, str]:
    parsed = material_resource(resource, materials)
    if parsed is None:
        if resource.kind != "item" or ":" not in resource.id:
            raise ValueError(f"unsupported fixed ingredient: {resource}")
        return {"item": resource.id}
    material_id, form = parsed
    prefix = prefixes.get(form)
    if prefix is None:
        raise ValueError(f"unknown CC prefix {form}")
    namespace = prefix.get("tag_namespace") or "cruciblecraft"
    return {"tag": f"{namespace}:{prefix['tag_directory']}/{material_id}"}


def output_json(
    resource: compare.Resource,
    materials: dict[str, dict[str, Any]],
) -> dict[str, Any]:
    parsed = material_resource(resource, materials)
    if parsed is None:
        if resource.kind != "item" or ":" not in resource.id:
            raise ValueError(f"unsupported fixed output: {resource}")
        item_id = resource.id
    else:
        material = materials[parsed[0]]
        item_id = (
            (material.get("form_items") or {}).get(parsed[1])
            or f"cruciblecraft:{parsed[0]}/{parsed[1]}"
        )
    return {"id": item_id, "count": resource.count}


def fluid_json(resource: compare.Resource) -> dict[str, Any]:
    if resource.kind != "fluid" or not resource.id:
        raise ValueError(f"unsupported fluid resource: {resource}")
    fluid_id = (
        resource.id
        if ":" in resource.id
        else f"minecraft:{resource.id}"
    )
    return {"id": fluid_id, "amount": resource.count}


def recipe_document(
    recipe: compare.NormRecipe,
    materials: dict[str, dict[str, Any]],
    prefixes: dict[str, dict[str, Any]],
) -> dict[str, Any]:
    item_inputs = [
        resource for resource in recipe.inputs if resource.kind == "item"
    ]
    fluid_inputs = [
        resource for resource in recipe.inputs if resource.kind == "fluid"
    ]
    item_outputs = [
        resource for resource in recipe.outputs if resource.kind == "item"
    ]
    fluid_outputs = [
        resource for resource in recipe.outputs if resource.kind == "fluid"
    ]
    chances = list(recipe.chances)
    if not chances:
        chances = [10_000] * len(item_outputs)
    if len(chances) != len(item_outputs):
        raise ValueError(
            f"{recipe.family}/{recipe.material}: output chance arity mismatch"
        )
    document: dict[str, Any] = {
        "type": "cruciblecraft:gt_recipe",
        "map": f"cruciblecraft:{FAMILY_TO_MAP[recipe.family]}",
        "item_inputs": [
            ingredient_json(resource, materials, prefixes)
            for resource in item_inputs
        ],
        "item_input_counts": [resource.count for resource in item_inputs],
        "item_outputs": [
            output_json(resource, materials) for resource in item_outputs
        ],
        "fluid_inputs": [fluid_json(resource) for resource in fluid_inputs],
        "fluid_outputs": [fluid_json(resource) for resource in fluid_outputs],
        "output_chances": chances,
        "duration": recipe.duration,
        "eut": recipe.eut,
        "special_value": recipe.special_value,
        "can_be_buffered": True,
    }
    return {
        key: value
        for key, value in document.items()
        if value not in ([], 0) or key == "duration"
    }


def recipe_operands(
    recipe: compare.NormRecipe,
    materials: dict[str, dict[str, Any]],
) -> list[dict[str, str]]:
    operands = set()
    for side, resources in (
        ("input", recipe.inputs),
        ("output", recipe.outputs),
    ):
        for resource in resources:
            parsed = material_resource(resource, materials)
            if parsed is not None:
                operands.add((parsed[0], parsed[1], side))
    return [
        {"material": material, "form": form, "side": side}
        for material, form, side in sorted(operands)
    ]


def validate_recipe_shape(
    recipe: compare.NormRecipe,
    materials: dict[str, dict[str, Any]],
    prefixes: dict[str, dict[str, Any]],
) -> None:
    map_name = FAMILY_TO_MAP[recipe.family]
    counts = (
        sum(resource.kind == "item" for resource in recipe.inputs),
        sum(resource.kind == "item" for resource in recipe.outputs),
        sum(resource.kind == "fluid" for resource in recipe.inputs),
        sum(resource.kind == "fluid" for resource in recipe.outputs),
    )
    limits = MACHINE_LIMITS[map_name]
    if any(actual > limit for actual, limit in zip(counts, limits)):
        raise ValueError(
            f"{recipe.family}/{recipe.material}: machine capacity "
            f"{counts} exceeds {limits}"
        )
    if recipe.duration <= 0:
        raise ValueError(f"{recipe.family}/{recipe.material}: non-positive duration")
    if recipe.family == "chain_smelter":
        input_units = 0
        output_units = 0
        target = (
            materials[recipe.material]
            .get("gt6_metadata", {})
            .get("processing_targets", {})
            .get("smelting")
        )
        for resource in recipe.inputs:
            parsed = material_resource(resource, materials)
            if parsed is not None:
                if (
                    parsed == (recipe.material, "dust")
                    and isinstance(target, dict)
                    and isinstance(target.get("cc_units"), int)
                    and target["cc_units"] > 0
                ):
                    input_units += target["cc_units"] * resource.count
                else:
                    input_units += (
                        int(prefixes[parsed[1]]["units"]) * resource.count
                    )
        for resource in recipe.outputs:
            parsed = material_resource(resource, materials)
            if parsed is not None:
                output_units += int(prefixes[parsed[1]]["units"]) * resource.count
        if input_units != output_units:
            raise ValueError(
                f"{recipe.family}/{recipe.material}: units are not conserved "
                f"({input_units} != {output_units})"
            )


def reference_recipe(
    reference: dict[str, Any],
    family: str,
    material_id: str,
    output_material: str,
    primary_output_count: int,
    materials: dict[str, dict[str, Any]],
) -> compare.NormRecipe | None:
    """Select the modal directly expressible GT6 recipe for one stage."""
    input_form, output_form = STAGE_FORMS[family]
    expected_input = f"{material_id}:{input_form}"
    expected_output = f"{output_material}:{output_form}"
    candidates: list[tuple[str, compare.NormRecipe]] = []
    frequencies: Counter[str] = Counter()
    for row in reference["families"][family]:
        if row.get("material") != material_id or row.get("fake"):
            continue
        inputs = [
            compare.Resource(entry["kind"], entry["id"], int(entry["count"]))
            for entry in row["inputs"]
        ]
        outputs = [
            compare.Resource(entry["kind"], entry["id"], int(entry["count"]))
            for entry in row["outputs"]
        ]
        if not any(
            resource.kind == "item" and resource.id == expected_input
            for resource in inputs
        ):
            continue
        item_inputs = [resource for resource in inputs if resource.kind == "item"]
        if len(item_inputs) != 1 or any(
            material_resource(resource, materials) is None
            for resource in item_inputs
        ):
            continue
        source_item_outputs = [
            resource for resource in outputs if resource.kind == "item"
        ]
        source_chances = [int(chance) for chance in row.get("chances") or []]
        if source_chances and len(source_chances) != len(source_item_outputs):
            continue
        if not source_chances:
            source_chances = [10_000] * len(source_item_outputs)
        normalized_outputs: list[compare.Resource] = []
        normalized_chances: list[int] = []
        seen_byproducts: set[str] = set()
        primary_found = False
        for output, chance in zip(source_item_outputs, source_chances):
            parsed = material_resource(output, materials)
            if parsed is None:
                match = re.fullmatch(
                    r"fixed:gregtech:gt\.meta\.([^@]+)@(\d+)",
                    output.id,
                )
                if match:
                    mapped = compare.OREDICT_CROSS_REFERENCE[
                        "material_id_to_cc"
                    ].get(match.group(2))
                    parsed = (mapped, "fixed_gt_prefix") if mapped else None
            if parsed is None:
                continue
            output_id, output_form = parsed
            if output_id == output_material and (
                output.id == expected_output
                or output_form == "fixed_gt_prefix"
            ) and not primary_found:
                if chance != 10_000:
                    continue
                normalized_outputs.append(
                    compare.Resource(
                        "item",
                        expected_output,
                        primary_output_count,
                    )
                )
                normalized_chances.append(10_000)
                primary_found = True
                continue
            if (
                family in {
                    "chain_sluice",
                    "chain_centrifuge",
                    "chain_shredder",
                }
                and output_id != output_material
                and output_id not in seen_byproducts
                and "dust" in compare.material_forms(materials[output_id])
                and len(normalized_outputs) < MACHINE_LIMITS[
                    FAMILY_TO_MAP[family]
                ][1]
            ):
                normalized_outputs.append(
                    compare.Resource("item", f"{output_id}:dust", 1)
                )
                normalized_chances.append(chance)
                seen_byproducts.add(output_id)
        if not primary_found:
            continue
        primary_index = next(
            index
            for index, resource in enumerate(normalized_outputs)
            if resource.id == expected_output
        )
        primary = normalized_outputs.pop(primary_index)
        primary_chance = normalized_chances.pop(primary_index)
        normalized_outputs.insert(0, primary)
        normalized_chances.insert(0, primary_chance)
        normalized_inputs = [
            compare.Resource(
                "item",
                expected_input,
                next(
                    resource.count
                    for resource in item_inputs
                    if resource.id == expected_input
                ),
            )
        ]
        if family == "chain_sluice":
            water = next(
                (
                    resource
                    for resource in inputs
                    if resource.kind == "fluid"
                    and resource.id in {"water", "minecraft:water"}
                ),
                None,
            )
            if water is None:
                continue
            normalized_inputs.append(water)
        shape = (
            1,
            len(normalized_outputs),
            sum(resource.kind == "fluid" for resource in normalized_inputs),
            0,
        )
        if any(
            actual > limit
            for actual, limit in zip(
                shape, MACHINE_LIMITS[FAMILY_TO_MAP[family]]
            )
        ):
            continue
        normalized = compare.NormRecipe(
            family=family,
            source="gt",
            map_name=f"cruciblecraft:{FAMILY_TO_MAP[family]}",
            material=material_id,
            inputs=normalized_inputs,
            outputs=normalized_outputs,
            duration=int(row["duration"]),
            eut=int(row["eut"]),
            special_value=int(row.get("special_value") or 0),
            chances=normalized_chances,
            raw_hint=f"gt6:{value_hash(row)}",
        )
        signature = value_hash({
            "inputs": [
                resource.key() for resource in normalized_inputs
            ],
            "outputs": [
                resource.key() for resource in normalized_outputs
            ],
            "duration": row["duration"],
            "eut": row["eut"],
            "special_value": row.get("special_value") or 0,
            "chances": normalized_chances,
        }, 64)
        frequencies[signature] += 1
        candidates.append((signature, normalized))
    if not candidates:
        return None
    candidates.sort(key=lambda candidate: (
        -frequencies[candidate[0]],
        candidate[0],
        candidate[1].raw_hint,
    ))
    return candidates[0][1]


def expand_offline_chain(
    materials: dict[str, dict[str, Any]],
    core: set[str],
    reference: dict[str, Any],
    prefixes: dict[str, dict[str, Any]],
) -> list[compare.NormRecipe]:
    """Build an upstream-closed chain, preferring direct GT6 stage evidence."""
    recipes: list[compare.NormRecipe] = []

    def item(material_id: str, form: str, count: int = 1) -> compare.Resource:
        return compare.Resource("item", f"{material_id}:{form}", count)

    def has(material_id: str, form: str) -> bool:
        return (
            material_id in materials
            and form in compare.material_forms(materials[material_id])
        )

    def byproduct_outputs(
        material_id: str, primary: str, family: str
    ) -> tuple[list[compare.Resource], list[int]]:
        outputs = [item(material_id, primary)]
        chances = [10_000]
        byproducts = (
            materials[material_id].get("gt6_metadata", {}).get("byproducts")
            or []
        )
        start = {
            "chain_sluice": 0,
            "chain_centrifuge": 1,
            "chain_shredder": 2,
        }[family]
        for index, chance in ((start, 2_500), (start + 1, 1_000)):
            if index >= len(byproducts):
                continue
            selected = byproducts[index].get("material")
            if selected and has(selected, "dust"):
                outputs.append(item(selected, "dust"))
                chances.append(chance)
        return outputs, chances

    def append(
        family: str,
        material_id: str,
        inputs: list[compare.Resource],
        outputs: list[compare.Resource],
        duration: int,
        eut: int,
        chances: list[int] | None = None,
        output_material: str | None = None,
    ) -> bool:
        projected = compare.NormRecipe(
            family=family,
            source="cc",
            map_name=f"cruciblecraft:{FAMILY_TO_MAP[family]}",
            material=material_id,
            inputs=inputs,
            outputs=outputs,
            duration=duration,
            eut=eut,
            chances=chances or [],
            raw_hint="offline_ore_chain_projection",
        )
        selected = reference_recipe(
            reference,
            family,
            material_id,
            output_material or material_id,
            next(
                resource.count
                for resource in outputs
                if resource.id
                == (
                    f"{output_material or material_id}:"
                    f"{STAGE_FORMS[family][1]}"
                )
            ),
            materials,
        )
        if selected is not None and family == "chain_smelter":
            try:
                validate_recipe_shape(selected, materials, prefixes)
            except ValueError:
                selected = None
        recipes.append(selected or projected)
        return True

    for material_id in sorted(core):
        if material_id not in materials:
            raise ValueError(f"L1b CORE references unknown material {material_id}")
        if not (
            has(material_id, "raw_ore")
            and has(material_id, "crushed_ore")
        ):
            continue
        append(
            "crush_raw_to_crushed",
            material_id,
            [item(material_id, "raw_ore")],
            [item(material_id, "crushed_ore", 2)],
            {"lead": 256, "nickel": 384}.get(material_id, 128),
            16,
        )
        if not has(material_id, "washed_crushed_ore"):
            continue
        outputs, chances = byproduct_outputs(
            material_id, "washed_crushed_ore", "chain_sluice"
        )
        append(
            "chain_sluice",
            material_id,
            [
                item(material_id, "crushed_ore"),
                compare.Resource("fluid", "water", 250),
            ],
            outputs,
            240,
            16,
            chances,
        )
        if not has(material_id, "centrifuged_crushed_ore"):
            continue
        outputs, chances = byproduct_outputs(
            material_id,
            "centrifuged_crushed_ore",
            "chain_centrifuge",
        )
        append(
            "chain_centrifuge",
            material_id,
            [item(material_id, "washed_crushed_ore")],
            outputs,
            300,
            32,
            chances,
        )
        if not has(material_id, "purified_dust"):
            continue
        outputs, chances = byproduct_outputs(
            material_id, "purified_dust", "chain_shredder"
        )
        append(
            "chain_shredder",
            material_id,
            [item(material_id, "centrifuged_crushed_ore")],
            outputs,
            180,
            24,
            chances,
        )
        if not has(material_id, "dust"):
            continue
        append(
            "chain_sifter",
            material_id,
            [item(material_id, "purified_dust")],
            [item(material_id, "dust")],
            160,
            16,
        )
        target = (
            materials[material_id]
            .get("gt6_metadata", {})
            .get("processing_targets", {})
            .get("smelting")
        )
        if has(material_id, "dust") and target:
            target_id = target.get("material")
            target_units = target.get("cc_units")
            if (
                isinstance(target_id, str)
                and isinstance(target_units, int)
                and target_units > 0
                and has(target_id, "ingot")
            ):
                divisor = math.gcd(target_units, 144)
                input_count = 144 // divisor
                output_count = target_units // divisor
                append(
                    "chain_smelter",
                    material_id,
                    [item(material_id, "dust", input_count)],
                    [item(target_id, "ingot", output_count)],
                    800 * input_count,
                    8,
                    output_material=target_id,
                )
    return recipes


def build_documents() -> tuple[dict[str, Any], dict[str, Any], dict[str, str]]:
    selected = load(SELECTED)
    registration_gate = load(REGISTRATION_GATE)
    core = {
        record["cc_id"]
        for record in selected["records"].values()
        if record.get("layer") == "CORE" and record.get("cc_id")
    }
    materials = compare.cached_cc_materials()
    worldgen_materials = worldgen_ore_materials(materials)
    for material in materials.values():
        material["_resolved_forms"] = list(material["_factual_forms"])
    factual_ore_materials = {
        material_id
        for material_id, material in materials.items()
        if "ore" in material["_factual_forms"]
    }
    processing_candidates = core | factual_ore_materials
    prefixes = prefix_definitions()
    reference = load(REFERENCE)

    expanded = expand_offline_chain(
        materials, processing_candidates, reference, prefixes
    )
    by_key: dict[tuple[str, str], list[compare.NormRecipe]] = defaultdict(list)
    for recipe in expanded:
        by_key[(recipe.family, recipe.material)].append(recipe)

    evidence: dict[tuple[str, str], list[str]] = defaultdict(list)
    reference_rows: list[tuple[str, str | None, str]] = []
    for family in STAGE_ORDER:
        for row in reference["families"][family]:
            material = row.get("material")
            source_hash = value_hash(row)
            reference_rows.append((family, material, source_hash))
            if material:
                evidence[(family, material)].append(source_hash)

    emitted: list[dict[str, Any]] = []
    output_files: dict[str, str] = {}
    operand_rows: list[dict[str, Any]] = []
    map_counts: Counter[str] = Counter()
    signatures: set[tuple[str, str]] = set()
    projection_rejections: dict[tuple[str, str], str] = {}
    emitted_recipes: list[compare.NormRecipe] = []
    for (family, material_id), candidates in sorted(by_key.items()):
        if len(candidates) != 1:
            raise ValueError(
                f"{family}/{material_id}: expected one expanded recipe, "
                f"found {len(candidates)}"
            )
        recipe = candidates[0]
        try:
            validate_recipe_shape(recipe, materials, prefixes)
        except ValueError as error:
            if family == "chain_smelter" and "units are not conserved" in str(error):
                projection_rejections[(family, material_id)] = str(error)
                continue
            raise
        document = recipe_document(recipe, materials, prefixes)
        gt_evidence = sorted(set(evidence.get((family, material_id), [])))
        selected_source = (
            recipe.raw_hint.removeprefix("gt6:")
            if recipe.raw_hint.startswith("gt6:")
            else None
        )
        signature = value_hash(
            {
                "map": document["map"],
                "item_inputs": document.get("item_inputs", []),
                "item_input_counts": document.get("item_input_counts", []),
                "fluid_inputs": document.get("fluid_inputs", []),
            },
            64,
        )
        signature_key = (document["map"], signature)
        if signature_key in signatures:
            raise ValueError(
                f"duplicate concrete input signature in {document['map']}: "
                f"{material_id}"
            )
        signatures.add(signature_key)
        semantic_hash = value_hash(document)
        document["provenance"] = {
            "source_kind": (
                "gt6_evidence" if selected_source else "topology_fallback"
            ),
            "selected_source_recipe": (
                selected_source
                or f"topology_projection:{semantic_hash}"
            ),
            "evidence_hashes": gt_evidence,
        }
        relative = (
            f"{FAMILY_TO_MAP[family]}/{material_id}/{semantic_hash}.json"
        )
        output_files[relative] = stable_json(document)
        emitted_recipes.append(recipe)
        operands = recipe_operands(recipe, materials)
        operand_rows.append({
            "recipe": relative.removesuffix(".json"),
            "operands": operands,
        })
        emitted.append({
            "family": family,
            "map": document["map"],
            "material": material_id,
            "path": (
                "src/ore_chain_generated/resources/data/cruciblecraft/recipe/"
                f"ore_chain/{relative}"
            ),
            "semantic_hash": semantic_hash,
            "source": (
                "gt6_normalized_stage_evidence_projection"
                if selected_source
                else "data_driven_ore_topology_projection"
            ),
            "selected_source_recipe": selected_source,
            "gt6_evidence_hashes": gt_evidence,
            **(
                {"input_substituted": True}
                if family == "crush_raw_to_crushed"
                else {}
            ),
            "operands": operands,
        })
        map_counts[FAMILY_TO_MAP[family]] += 1

    base_crusher_rows = {
        row["material"]: row
        for row in emitted
        if row["family"] == "crush_raw_to_crushed"
    }
    registered_ore_materials = {
        material_id
        for material_id, forms in registration_gate["materials"].items()
        if "ore" in forms
    }
    missing_base_crusher = registered_ore_materials - set(base_crusher_rows)
    if missing_base_crusher:
        raise ValueError(
            "registered ore materials lack raw-ore crusher baselines: "
            + json.dumps(sorted(missing_base_crusher))
        )
    ore_prefix = prefixes["ore"]
    ore_tag_namespace = ore_prefix.get("tag_namespace") or "cruciblecraft"
    for material_id in sorted(registered_ore_materials):
        baseline = base_crusher_rows[material_id]
        baseline_relative = baseline["path"].split("ore_chain/", 1)[1]
        baseline_document = json.loads(output_files[baseline_relative])
        if (
            len(baseline_document.get("item_inputs", [])) != 1
            or baseline_document.get("item_input_counts") != [1]
            or len(baseline_document.get("item_outputs", [])) != 1
            or baseline_document.get("fluid_inputs")
            or baseline_document.get("fluid_outputs")
        ):
            raise ValueError(
                f"{material_id}: raw-ore crusher baseline is not a one-in/one-out "
                "item recipe"
            )
        document = {
            key: copy.deepcopy(value)
            for key, value in baseline_document.items()
            if key != "provenance"
        }
        tag_name = materials[material_id].get("tag_name") or material_id
        document["item_inputs"] = [{
            "tag": (
                f"{ore_tag_namespace}:{ore_prefix['tag_directory']}/{tag_name}"
            )
        }]
        document["item_outputs"][0]["count"] = ORE_BLOCK_OUTPUT_COUNT
        signature = value_hash(
            {
                "map": document["map"],
                "item_inputs": document.get("item_inputs", []),
                "item_input_counts": document.get("item_input_counts", []),
                "fluid_inputs": document.get("fluid_inputs", []),
            },
            64,
        )
        signature_key = (document["map"], signature)
        if signature_key in signatures:
            raise ValueError(
                "duplicate concrete ore-block input signature in "
                f"{document['map']}: {material_id}"
            )
        signatures.add(signature_key)
        semantic_hash = value_hash(document)
        runtime_source = (
            f"ore_block_projection:{baseline['semantic_hash']}:"
            f"k{ORE_BLOCK_OUTPUT_COUNT}"
        )
        document["provenance"] = {
            "source_kind": ORE_BLOCK_SOURCE_KIND,
            "selected_source_recipe": runtime_source,
            "evidence_hashes": [],
        }
        relative = (
            f"crusher/{material_id}/{semantic_hash}.json"
        )
        output_files[relative] = stable_json(document)
        operands = [
            {"material": material_id, "form": "crushed_ore", "side": "output"},
            {"material": material_id, "form": "ore", "side": "input"},
        ]
        operand_rows.append({
            "recipe": relative.removesuffix(".json"),
            "operands": operands,
        })
        emitted.append({
            "family": ORE_BLOCK_ROUTE_FAMILY,
            "map": document["map"],
            "material": material_id,
            "path": (
                "src/ore_chain_generated/resources/data/cruciblecraft/recipe/"
                f"ore_chain/{relative}"
            ),
            "semantic_hash": semantic_hash,
            "source": ORE_BLOCK_SOURCE_KIND,
            "selected_source_recipe": None,
            "gt6_evidence_hashes": [],
            "derivation": {
                "base_recipe_path": baseline["path"],
                "base_recipe_semantic_hash": baseline["semantic_hash"],
                "output_count": ORE_BLOCK_OUTPUT_COUNT,
                "output_multiplier": {
                    "numerator": ORE_BLOCK_OUTPUT_COUNT,
                    "denominator": baseline_document["item_outputs"][0]["count"],
                },
                "runtime_source": runtime_source,
            },
            "operands": operands,
        })
        map_counts["crusher"] += 1

    emitted_keys = {
        (row["family"], row["material"]) for row in emitted
    }
    missing_acceptance = {
        material: [
            family for family in STAGE_ORDER
            if (family, material) not in emitted_keys
        ]
        for material in ACCEPTANCE_MATERIALS
    }
    missing_acceptance = {
        material: stages
        for material, stages in missing_acceptance.items()
        if stages
    }
    if missing_acceptance:
        raise ValueError(
            "acceptance materials lack complete ore chain: "
            + json.dumps(missing_acceptance, sort_keys=True)
        )

    emitted_path_by_key = {
        (row["family"], row["material"]): row["path"]
        for row in emitted
    }
    selected_source_by_key = {
        (row["family"], row["material"]): row["selected_source_recipe"]
        for row in emitted
    }
    source_accounting = []
    accounting_counts: Counter[str] = Counter()
    for family, material, source_hash in reference_rows:
        emitted_path = emitted_path_by_key.get((family, material))
        if (
            emitted_path is not None
            and selected_source_by_key[(family, material)] == source_hash
        ):
            status = "emitted"
            reason = None
        elif material not in processing_candidates:
            status = "unsupported"
            reason = (
                "source material is neither L1b CORE nor a factual ore "
                "declaration"
            )
        elif (family, material) in projection_rejections:
            status = "unsupported"
            reason = projection_rejections[(family, material)]
        else:
            status = "unsupported"
            reason = (
                "not selected by the deterministic representative policy"
                if emitted_path is not None
                else "material lacks an upstream-closed factual route"
            )
        accounting_counts[status] += 1
        source_accounting.append({
            "family": family,
            "material": material,
            "source_hash": source_hash,
            "status": status,
            **({"recipe_path": emitted_path} if emitted_path else {}),
            **({"reason": reason} if reason else {}),
        })

    produced: set[tuple[str, str]] = set()
    stage_materials: dict[str, set[str]] = defaultdict(set)
    recipes_by_stage: dict[str, list[compare.NormRecipe]] = defaultdict(list)
    for recipe in emitted_recipes:
        recipes_by_stage[recipe.family].append(recipe)
        stage_materials[recipe.family].add(str(recipe.material))
    for family in STAGE_ORDER:
        input_form, _ = STAGE_FORMS[family]
        for recipe in recipes_by_stage[family]:
            primary_input = (str(recipe.material), input_form)
            if family != STAGE_ORDER[0] and primary_input not in produced:
                raise ValueError(
                    f"{family}/{recipe.material}: input has no upstream producer"
                )
        for recipe in recipes_by_stage[family]:
            for resource in recipe.outputs:
                parsed = material_resource(resource, materials)
                if parsed is not None:
                    produced.add(parsed)

    generated_recipe_root = (
        ROOT / "src/generated/resources/data/cruciblecraft/recipe"
    )
    shortcut_paths = {
        (form, process): sorted(
            path.relative_to(generated_recipe_root).as_posix()
            for path in generated_recipe_root.glob(
                f"*/{form}_ore_{process}.json"
            )
        )
        for form in ("raw", "crushed")
        for process in ("smelting", "blasting")
    }
    shortcut_materials = {
        key: {Path(path).parts[0] for path in paths}
        for key, paths in shortcut_paths.items()
    }
    for paths in shortcut_paths.values():
        for relative in paths:
            document = load(generated_recipe_root / relative)
            if document.get("group") != COMPAT_SHORTCUT_GROUP:
                raise ValueError(
                    f"{relative}: furnace shortcut group must be "
                    f"{COMPAT_SHORTCUT_GROUP}"
                )
    for form in ("raw", "crushed"):
        if shortcut_materials[(form, "smelting")] != shortcut_materials[
            (form, "blasting")
        ]:
            raise ValueError(
                f"{form} ore furnace shortcuts are not paired between "
                "smelting and blasting"
            )
    raw_pairs = len(shortcut_materials[("raw", "smelting")])
    crushed_pairs = len(shortcut_materials[("crushed", "smelting")])
    smelting_files = sum(
        len(shortcut_paths[(form, "smelting")])
        for form in ("raw", "crushed")
    )
    blasting_files = sum(
        len(shortcut_paths[(form, "blasting")])
        for form in ("raw", "crushed")
    )
    crusher_materials = stage_materials["crush_raw_to_crushed"]
    sifter_materials = stage_materials["chain_sifter"]
    smelter_materials = stage_materials["chain_smelter"]
    coverage_ledger = {
        "worldgen_ore_materials": sorted(worldgen_materials),
        "crusher_without_worldgen": sorted(
            crusher_materials - worldgen_materials
        ),
        "sifter_dust_without_smelter": sorted(
            sifter_materials - smelter_materials
        ),
        "incomplete_routes_from_crusher": {
            material: [
                family
                for family in STAGE_ORDER[1:]
                if material not in stage_materials[family]
            ]
            for material in sorted(crusher_materials)
            if any(
                material not in stage_materials[family]
                for family in STAGE_ORDER[1:]
            )
        },
        "ore_block_crusher_ingress": {
            "policy": (
                "registered high-version ore blocks provide an additional "
                "crusher ingress without changing the six-stage chain"
            ),
            "registered_ore_materials": len(registered_ore_materials),
            "raw_ore_crusher_materials": len(crusher_materials),
            "intersection": len(registered_ore_materials & crusher_materials),
            "ore_without_raw_crusher": sorted(
                registered_ore_materials - crusher_materials
            ),
            "raw_crusher_without_ore": sorted(
                crusher_materials - registered_ore_materials
            ),
            "output_count": ORE_BLOCK_OUTPUT_COUNT,
        },
        "furnace_shortcut_policy": {
            "decision": "retained compatibility route",
            "scope": "generated raw/crushed ore smelting and blasting recipes",
            "main_output_policy": (
                "furnace shortcuts and the six-stage chain yield equal "
                "main-output material units"
            ),
            "byproduct_value": (
                "the six-stage chain's additional value is staged byproducts"
            ),
            "group": COMPAT_SHORTCUT_GROUP,
            "raw_pairs": raw_pairs,
            "crushed_pairs": crushed_pairs,
            "smelting_files": smelting_files,
            "blasting_files": blasting_files,
            "total_files": smelting_files + blasting_files,
        },
    }

    source_inputs = {
        "l1b_selection": {
            "path": "tools/gt6_l1b_selected.json",
            "sha256": sha256(SELECTED),
            "layer": "CORE",
        },
        "gt6_reference": {
            "path": "tools/gt6_recipe_normalized_reference.json",
            "sha256": sha256(REFERENCE),
        },
        "material_registration_gate": {
            "path": (
                "src/main/resources/data/cruciblecraft/"
                "material_registration_gate.json"
            ),
            "field": "materials with registered ore form",
            "registered_ore_material_set_sha256": value_hash(
                sorted(registered_ore_materials), 64
            ),
        },
        "material_catalog": {
            "path": "src/main/resources/data/cruciblecraft/materials",
            "sha256": directory_sha256(
                ROOT / "src/main/resources/data/cruciblecraft/materials"
            ),
        },
        "prefix_catalog": {
            "path": "src/main/resources/data/cruciblecraft/material_prefixes",
            "sha256": directory_sha256(PREFIX_ROOT),
        },
        "prefix_mapping": {
            "path": "tools/gt6_prefix_mapping.json",
            "sha256": sha256(TOOLS / "gt6_prefix_mapping.json"),
        },
        "l3_form_plan": {
            "path": "tools/gt6_l3_prefix_plan.json",
            "sha256": sha256(TOOLS / "gt6_l3_prefix_plan.json"),
        },
        "normalizer": {
            "path": "tools/compare_gt6_recipes.py",
            "sha256": sha256(TOOLS / "compare_gt6_recipes.py"),
        },
    }
    acceptance_reference_samples = {
        material: {
            family: sorted(
                (
                    {
                        "source_hash": value_hash(row),
                        "inputs": row["inputs"],
                        "outputs": row["outputs"],
                        "duration": row["duration"],
                        "eut": row["eut"],
                        "chances": row.get("chances") or [],
                    }
                    for row in reference["families"][family]
                    if row.get("material") == material
                ),
                key=lambda row: row["source_hash"],
            )[:2]
            for family in STAGE_ORDER
        }
        for material in ACCEPTANCE_MATERIALS
    }
    index = {
        "schema_version": 3,
        "policy": (
            "upstream-closed concrete six-stage chain for the union of L1b CORE "
            "and factual ore declarations; "
            "topology falls back to reviewed constants, while directly "
            "expressible stage parameters and byproducts use a deterministic "
            "normalized GT6 representative; registered high-version ore blocks "
            "add a derived crusher ingress with K=5 and no GT6 evidence claim"
        ),
        "inputs": source_inputs,
        "counts": {
            "core_materials": len(core),
            "factual_ore_materials": len(factual_ore_materials),
            "processing_candidates": len(processing_candidates),
            "recipes": len(emitted),
            "recipes_by_map": dict(sorted(map_counts.items())),
            "gt6_evidenced_recipes": sum(
                row["source"] == "gt6_normalized_stage_evidence_projection"
                for row in emitted
            ),
            "projected_recipes": sum(
                row["source"] == "data_driven_ore_topology_projection"
                for row in emitted
            ),
            "high_version_ore_block_recipes": sum(
                row["source"] == ORE_BLOCK_SOURCE_KIND
                for row in emitted
            ),
            "normalized_sources": len(source_accounting),
            "source_accounting": dict(sorted(accounting_counts.items())),
        },
        "acceptance_materials": list(ACCEPTANCE_MATERIALS),
        "acceptance_reference_samples": acceptance_reference_samples,
        "coverage_ledger": coverage_ledger,
        "recipes": emitted,
        "source_accounting": source_accounting,
    }
    operands = {
        "schema_version": 1,
        "source": {
            "path": "tools/gt6_ore_chain.json",
            "sha256": hashlib.sha256(
                compact_json(index).encode("utf-8")
            ).hexdigest(),
        },
        "counts": {
            "recipes": len(operand_rows),
            "material_form_pairs": len({
                (operand["material"], operand["form"])
                for row in operand_rows
                for operand in row["operands"]
            }),
        },
        "recipes": operand_rows,
    }
    return index, operands, output_files


def check_outputs(
    index: dict[str, Any],
    operands: dict[str, Any],
    output_files: dict[str, str],
) -> list[str]:
    errors = []
    expected_index = compact_json(index)
    expected_operands = compact_json(operands)
    if not INDEX_OUT.is_file() or INDEX_OUT.read_text(encoding="utf-8") != expected_index:
        errors.append(str(INDEX_OUT.relative_to(ROOT)))
    if (
        not OPERANDS_OUT.is_file()
        or OPERANDS_OUT.read_text(encoding="utf-8") != expected_operands
    ):
        errors.append(str(OPERANDS_OUT.relative_to(ROOT)))
    actual = {
        path.relative_to(OUTPUT_ROOT).as_posix(): path
        for path in OUTPUT_ROOT.rglob("*.json")
    } if OUTPUT_ROOT.is_dir() else {}
    if set(actual) != set(output_files):
        errors.append(str(OUTPUT_ROOT.relative_to(ROOT)))
    else:
        for relative, content in output_files.items():
            if actual[relative].read_text(encoding="utf-8") != content:
                errors.append(
                    str(actual[relative].relative_to(ROOT))
                )
    return errors


def load_committed_outputs() -> tuple[
    dict[str, Any],
    dict[str, Any],
    dict[str, str],
]:
    index = load(INDEX_OUT)
    operands = load(OPERANDS_OUT)
    files = {
        path.relative_to(OUTPUT_ROOT).as_posix(): path.read_text(encoding="utf-8")
        for path in OUTPUT_ROOT.rglob("*.json")
    } if OUTPUT_ROOT.is_dir() else {}
    return index, operands, files


def check_committed_outputs() -> list[str]:
    errors: list[str] = []
    index, operands, files = load_committed_outputs()
    manifest = load(LOCAL_ARTIFACT_MANIFEST)
    reference_metadata = next(
        (
            row for row in manifest.get("artifacts") or []
            if row.get("path") == "tools/gt6_recipe_normalized_reference.json"
        ),
        None,
    )
    if reference_metadata is None:
        errors.append("tools/local_artifact_manifest.json: reference metadata")
    elif (
        index.get("inputs", {}).get("gt6_reference", {}).get("sha256")
        != reference_metadata.get("sha256")
    ):
        errors.append("tools/gt6_ore_chain.json: GT6 reference hash")

    recipes = index.get("recipes") or []
    expected_files = {
        row["path"].split("ore_chain/", 1)[1]
        for row in recipes
    }
    if set(files) != expected_files:
        errors.append(str(OUTPUT_ROOT.relative_to(ROOT)))
    for row in recipes:
        relative = row["path"].split("ore_chain/", 1)[1]
        content = files.get(relative)
        if content is None:
            continue
        document = json.loads(content)
        semantic = {
            key: value
            for key, value in document.items()
            if key != "provenance"
        }
        if value_hash(semantic) != row.get("semantic_hash"):
            errors.append(row["path"] + ": semantic hash")
        provenance = document.get("provenance") or {}
        if provenance.get("evidence_hashes") != row.get("gt6_evidence_hashes"):
            errors.append(row["path"] + ": evidence hashes")

    operand_rows = operands.get("recipes") or []
    if int(index.get("counts", {}).get("recipes") or -1) != len(recipes):
        errors.append("tools/gt6_ore_chain.json: recipe count")
    if int(operands.get("counts", {}).get("recipes") or -1) != len(operand_rows):
        errors.append("tools/gt6_ore_chain_operands.json: recipe count")
    indexed_ids = {
        path.removesuffix(".json")
        for path in expected_files
    }
    if indexed_ids != {row.get("recipe") for row in operand_rows}:
        errors.append("tools/gt6_ore_chain_operands.json: recipe index")
    source_rows = index.get("source_accounting") or []
    if int(index.get("counts", {}).get("normalized_sources") or -1) != len(
        source_rows
    ):
        errors.append("tools/gt6_ore_chain.json: source accounting")
    return sorted(set(errors))


def write_outputs(
    index: dict[str, Any],
    operands: dict[str, Any],
    output_files: dict[str, str],
) -> None:
    INDEX_OUT.write_text(
        compact_json(index), encoding="utf-8", newline="\n"
    )
    OPERANDS_OUT.write_text(
        compact_json(operands), encoding="utf-8", newline="\n"
    )
    if OUTPUT_ROOT.exists():
        shutil.rmtree(OUTPUT_ROOT)
    for relative, content in output_files.items():
        path = OUTPUT_ROOT / relative
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(content, encoding="utf-8", newline="\n")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--review", action="store_true")
    replay_mode = parser.add_mutually_exclusive_group()
    replay_mode.add_argument(
        "--reference-only",
        action="store_true",
        help="validate committed compact outputs without rebuilding from cache",
    )
    replay_mode.add_argument(
        "--full-replay",
        action="store_true",
        help="force regeneration from the normalized GT6 reference",
    )
    args = parser.parse_args()
    if sum((args.write, args.check, args.review)) != 1:
        parser.error("choose exactly one of --write, --check, or --review")
    if (args.reference_only or args.full_replay) and not args.check:
        parser.error("--reference-only and --full-replay require --check")
    if args.check and (
        args.reference_only
        or (not args.full_replay and not REFERENCE.is_file())
    ):
        errors = check_committed_outputs()
        if errors:
            print(
                "Compact ore-chain artifact validation failed:\n"
                + "\n".join(f"- {error}" for error in errors),
                file=sys.stderr,
            )
            return 1
        print("Compact ore-chain artifacts are internally current.")
        print(
            "SKIP: full ore-chain regeneration requires "
            "tools/gt6_recipe_normalized_reference.json. Restore it with "
            "python tools/compare_gt6_recipes.py --write-reference, then rerun "
            "--check for full replay."
        )
        return 0
    if not REFERENCE.is_file():
        print(
            "Ore-chain regeneration requires "
            "tools/gt6_recipe_normalized_reference.json. Restore the "
            "authoritative gt6_dump and run "
            "python tools/compare_gt6_recipes.py --write-reference.",
            file=sys.stderr,
        )
        return 2
    index, operands, output_files = build_documents()
    if args.review:
        print(stable_json({
            "counts": index["counts"],
            "operand_counts": operands["counts"],
            "acceptance_materials": index["acceptance_materials"],
            "acceptance_reference_samples": index[
                "acceptance_reference_samples"
            ],
        }), end="")
        return 0
    if args.write:
        write_outputs(index, operands, output_files)
        print(f"Wrote {INDEX_OUT}")
        print(f"Wrote {OPERANDS_OUT}")
        print(f"Wrote {len(output_files)} concrete recipes under {OUTPUT_ROOT}")
        return 0
    errors = check_outputs(index, operands, output_files)
    if errors:
        print(
            "Ore-chain artifacts are stale:\n"
            + "\n".join(f"- {error}" for error in errors),
            file=sys.stderr,
        )
        return 1
    print("Ore-chain artifacts are current.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
