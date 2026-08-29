#!/usr/bin/env python3
"""Discover GT6 mixer/smelter/unboxinator routes for remaining T39 player-path inputs.

Routes are pinned to the local GT6 dump, emitted as explicit-item gt_recipe JSON
(or crafting_shapeless for unboxinator unpack), and tracked in
tools/t39_player_path_support.json. Discovery ignores unproven lossy aliases
and keeps only the family-atomic reverse-dependency slice.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
from collections import defaultdict
from pathlib import Path
from typing import Any

_ROOT = Path(__file__).resolve().parents[1]
if str(_ROOT) not in sys.path:
    sys.path.insert(0, str(_ROOT))

from tools import t39_common as t39  # noqa: E402
from tools import t21_operand_reachability as t21  # noqa: E402

ROOT = t39.ROOT
TOOLS = t39.TOOLS
DUMP_ROOT = ROOT / "gt6_dump/gt6_recipe_dump/maps"
CROSS = TOOLS / "gt6_oredict_cross_reference.json"
PLAYER_PATH = t39.PLAYER_PATH
T21 = TOOLS / "t21_operand_reachability.json"
OUTPUT = TOOLS / "t39_player_path_support.json"
OUTPUT_ROOT = t39.LOCKED_SUPPORT_ROOT
MATERIALS_DIR = ROOT / "src/main/resources/data/cruciblecraft/materials"
CHEMICAL_FLUID_GATES = (
    ROOT / "src/t5_chemical_generated/resources/data/cruciblecraft/t5_chemical_fluid_gate.json",
    ROOT / "src/main/resources/data/cruciblecraft/t10_container_fluid_gate.json",
    ROOT / "src/main/resources/data/cruciblecraft/t11_hydrocarbon_fluid_gate.json",
)

CC_MAP = {
    "gt.recipe.mixer": "cruciblecraft:mixer",
    "gt.recipe.smelter": "cruciblecraft:smelter",
    "gt.recipe.unboxinator": "cruciblecraft:unboxinator",
}

# T5 host envelopes. Oversized GT6 mixer/smelter rows cannot load on current
# single-block specs; skip them and keep looking for a fitting dump recipe.
HOST_ENVELOPE = {
    "gt.recipe.mixer": {
        "item_inputs": 4,
        "item_outputs": 1,
        "fluid_inputs": 3,
        "fluid_outputs": 2,
        "fluid_input_capacity": 32_000,
        "fluid_output_capacity": 32_000,
        "eut": 1_024,
    },
    "gt.recipe.smelter": {
        "item_inputs": 1,
        "item_outputs": 4,
        "fluid_inputs": 0,
        "fluid_outputs": 1,
        "fluid_input_capacity": 4_000,
        "fluid_output_capacity": 8_000,
        "eut": 1_024,
    },
}

RESERVED_STEMS = frozenset({"recovery", "packing"})
GT6_REVISION = t39.SOURCE_REVISION


def _load_registered_fluids() -> tuple[set[str], dict[str, str]]:
    molten: set[str] = set()
    if MATERIALS_DIR.is_dir():
        for path in MATERIALS_DIR.glob("*.json"):
            document = _load(path)
            if not isinstance(document, dict):
                continue
            material = str(document.get("id") or path.stem)
            if document.get("molten_fluid"):
                molten.add(material)
    chemical: dict[str, str] = {}
    for gate_path in CHEMICAL_FLUID_GATES:
        if not gate_path.is_file():
            continue
        document = _load(gate_path)
        for entry in document.get("fluids") or []:
            if not isinstance(entry, dict):
                continue
            material = entry.get("material")
            fluid_id = entry.get("id")
            if isinstance(material, str) and isinstance(fluid_id, str):
                chemical[material] = fluid_id
    return molten, chemical


def _stable(value: Any) -> str:
    return json.dumps(value, ensure_ascii=False, indent=2, sort_keys=True) + "\n"


def _digest(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def _load(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def _identity_from_cc_item(item_id: str) -> str:
    return f"item:{item_id}"


def _identity_from_cc_fluid(fluid_id: str) -> str:
    return f"fluid:{fluid_id}"


def _cc_item_from_identity(identity: str) -> str | None:
    if identity.startswith("item:"):
        return identity.removeprefix("item:")
    return None


def _cc_fluid_from_identity(identity: str) -> str | None:
    if identity.startswith("fluid:"):
        return identity.removeprefix("fluid:")
    return None


def _material_from_cc_item(item_id: str) -> str | None:
    if not item_id.startswith("cruciblecraft:") or "/" not in item_id:
        return None
    return item_id.split(":", 1)[1].split("/", 1)[0]


def _material_from_cc_fluid(fluid_id: str) -> str | None:
    if fluid_id == "minecraft:lava":
        return None
    if fluid_id.startswith("cruciblecraft:molten_"):
        return fluid_id.removeprefix("cruciblecraft:molten_")
    if fluid_id.startswith("cruciblecraft:"):
        return fluid_id.removeprefix("cruciblecraft:")
    return None


class Crosswalk:
    def __init__(self, document: dict[str, Any]) -> None:
        self.material_id_to_cc = document.get("material_id_to_cc") or {}
        self.prefix_item_to_form = document.get("prefix_item_to_form") or {}
        self.fluid_to_material = document.get("fluid_to_material") or {}
        self.molten_materials, self.chemical_fluid_by_material = _load_registered_fluids()

    def map_item_stack(self, stack: dict[str, Any]) -> str | None:
        item = stack.get("item")
        meta = stack.get("meta")
        if not isinstance(item, str):
            return None
        if item.startswith("minecraft:"):
            if isinstance(meta, int):
                aliased = t39.LEGACY_VANILLA_ITEMS.get((item, meta))
                if aliased is not None:
                    return f"item:{aliased}"
                if item == "minecraft:dye":
                    return None
            return f"item:{item}"
        form = self.prefix_item_to_form.get(item)
        if form and isinstance(meta, int):
            material = self.material_id_to_cc.get(str(meta))
            if material:
                return f"item:cruciblecraft:{material}/{form}"
        return None

    def map_fluid_stack(self, stack: dict[str, Any]) -> str | None:
        fluid = stack.get("fluid") or stack.get("id") or stack.get("name")
        if not isinstance(fluid, str) or not fluid:
            return None
        if fluid in {"lava", "minecraft:lava"}:
            return "fluid:minecraft:lava"
        if fluid.startswith("minecraft:"):
            return f"fluid:{fluid}"
        material = self.fluid_to_material.get(fluid)
        if material is None and fluid.startswith("molten."):
            token = fluid.split(".", 1)[1]
            material = self.fluid_to_material.get(token) or token
        elif material is None and fluid.startswith("molten_"):
            token = fluid.removeprefix("molten_")
            material = self.fluid_to_material.get(token) or token
        if not material:
            return None
        if material == "lava":
            return "fluid:cruciblecraft:lava"
        chemical = self.chemical_fluid_by_material.get(material)
        if chemical:
            return f"fluid:cruciblecraft:{chemical}"
        if material in self.molten_materials:
            return f"fluid:cruciblecraft:molten_{material}"
        return None


def _row_inputs_satisfied(row: dict[str, Any], identities: set[str]) -> bool:
    if row.get("alias_fail_closed"):
        return False
    for item in row.get("consume_ids") or []:
        if _identity_from_cc_item(item) not in identities:
            return False
    for fluid in row.get("fluid_input_ids") or []:
        if _identity_from_cc_fluid(fluid) not in identities:
            return False
    return True


def _families_by_template(player_path: dict[str, Any]) -> dict[str, list[dict[str, Any]]]:
    grouped: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for row in player_path.get("rows") or []:
        grouped[str(row.get("template_key") or "")].append(row)
    grouped.pop("", None)
    return grouped


def _source_by_template() -> dict[str, list[dict[str, Any]]]:
    from tools.build_t39_centrifuge_recipes import load_source

    grouped: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for relation in load_source()["relations"]:
        grouped[relation["template_key"]].append(relation)
    return grouped


def _family_is_signable(
    template_key: str,
    rows: list[dict[str, Any]],
    source_by_template: dict[str, list[dict[str, Any]]] | None = None,
) -> bool:
    if not rows or any(row.get("alias_fail_closed") for row in rows):
        return False
    source_rels = (source_by_template or {}).get(template_key) or []
    return not t39.family_fidelity_blockers(source_rels)


def _needed_identities(player_path: dict[str, Any], reachable: set[str]) -> tuple[set[str], set[str]]:
    needed_items: set[str] = set()
    needed_fluids: set[str] = set()
    source_by_template = _source_by_template()
    for template_key, rows in _families_by_template(player_path).items():
        if not _family_is_signable(template_key, rows, source_by_template):
            continue
        if all(_row_inputs_satisfied(row, reachable) for row in rows):
            continue
        for row in rows:
            if _row_inputs_satisfied(row, reachable):
                continue
            for item in row.get("consume_ids") or []:
                identity = _identity_from_cc_item(item)
                if identity not in reachable:
                    needed_items.add(identity)
            for fluid in row.get("fluid_input_ids") or []:
                identity = _identity_from_cc_fluid(fluid)
                if identity not in reachable:
                    needed_fluids.add(identity)
    return needed_items, needed_fluids


def _relevant_materials(needed: set[str]) -> set[str]:
    materials: set[str] = set()
    for identity in needed:
        item = _cc_item_from_identity(identity)
        if item:
            material = _material_from_cc_item(item)
            if material:
                materials.add(material)
            continue
        fluid = _cc_fluid_from_identity(identity)
        if fluid:
            material = _material_from_cc_fluid(fluid)
            if material:
                materials.add(material)
    return materials


def _output_materials(outputs: list[str]) -> set[str]:
    materials: set[str] = set()
    for identity in outputs:
        item = _cc_item_from_identity(identity)
        if item:
            material = _material_from_cc_item(item)
            if material:
                materials.add(material)
            continue
        fluid = _cc_fluid_from_identity(identity)
        if fluid:
            material = _material_from_cc_fluid(fluid)
            if material:
                materials.add(material)
    return materials


def _identity_allowed(identity: str, needed: set[str], relevant: set[str]) -> bool:
    if identity in needed:
        return True
    item = _cc_item_from_identity(identity)
    if item:
        material = _material_from_cc_item(item)
        return bool(material and material in relevant)
    fluid = _cc_fluid_from_identity(identity)
    if fluid:
        material = _material_from_cc_fluid(fluid)
        return bool(material and material in relevant)
    return False


def _map_recipe_operands(
    recipe: dict[str, Any],
    crosswalk: Crosswalk,
) -> tuple[list[str], list[str], list[str], list[str]]:
    item_inputs: list[str] = []
    item_outputs: list[str] = []
    fluid_inputs: list[str] = []
    fluid_outputs: list[str] = []
    for stack in recipe.get("inputs") or []:
        if isinstance(stack, dict):
            mapped = crosswalk.map_item_stack(stack)
            if mapped:
                item_inputs.append(mapped)
    for stack in recipe.get("outputs") or []:
        if isinstance(stack, dict):
            mapped = crosswalk.map_item_stack(stack)
            if mapped:
                item_outputs.append(mapped)
    for side in ("fluidInputs", "inputFluids"):
        for stack in recipe.get(side) or []:
            if isinstance(stack, dict):
                mapped = crosswalk.map_fluid_stack(stack)
                if mapped:
                    fluid_inputs.append(mapped)
    for side in ("fluidOutputs", "outputFluids"):
        for stack in recipe.get(side) or []:
            if isinstance(stack, dict):
                mapped = crosswalk.map_fluid_stack(stack)
                if mapped:
                    fluid_outputs.append(mapped)
    return item_inputs, item_outputs, fluid_inputs, fluid_outputs


def _recipe_enabled(recipe: dict[str, Any]) -> bool:
    return recipe.get("enabled", True) is not False and recipe.get("fake") is not True


def _fits_host_envelope(
    source_map: str,
    recipe: dict[str, Any],
    item_ins: list[str],
    item_outs: list[str],
    fluid_ins: list[str],
    fluid_outs: list[str],
) -> bool:
    envelope = HOST_ENVELOPE.get(source_map)
    if envelope is None:
        return True
    if (
        len(item_ins) > envelope["item_inputs"]
        or len(item_outs) > envelope["item_outputs"]
        or len(fluid_ins) > envelope["fluid_inputs"]
        or len(fluid_outs) > envelope["fluid_outputs"]
    ):
        return False
    eut = int(recipe.get("euPerTick") or recipe.get("eut") or 0)
    if eut <= 0 or eut > envelope["eut"]:
        return False
    for side, cap_key in (
        ("fluidInputs", "fluid_input_capacity"),
        ("inputFluids", "fluid_input_capacity"),
        ("fluidOutputs", "fluid_output_capacity"),
        ("outputFluids", "fluid_output_capacity"),
    ):
        for stack in recipe.get(side) or []:
            if not isinstance(stack, dict):
                continue
            if int(stack.get("amount") or 0) > envelope[cap_key]:
                return False
    return True
    return recipe.get("enabled", True) is not False and recipe.get("fake") is not True


def _slug(value: str) -> str:
    return re.sub(r"[^a-z0-9_]+", "_", value.lower()).strip("_")


def _filename_stem(prefix: str, identity: str) -> str:
    item = _cc_item_from_identity(identity)
    if item:
        material = _material_from_cc_item(item) or "item"
        form = item.split("/", 1)[-1]
        stem = f"{prefix}_{material}_{form}"
    else:
        fluid = _cc_fluid_from_identity(identity) or identity
        material = _material_from_cc_fluid(fluid) or _slug(fluid)
        stem = f"{prefix}_{material}"
    stem = _slug(stem)
    if stem.split("_")[-1] in RESERVED_STEMS:
        stem = f"{stem}_route"
    return stem


def _gt_recipe_document(
    *,
    source_map: str,
    source_index: int,
    cc_map: str,
    recipe: dict[str, Any],
    item_inputs: list[dict[str, Any]],
    item_input_counts: list[int],
    item_input_actions: list[dict[str, Any]] | None = None,
    item_outputs: list[dict[str, Any]],
    fluid_inputs: list[dict[str, Any]] | None = None,
    fluid_outputs: list[dict[str, Any]] | None = None,
) -> dict[str, Any]:
    fingerprint = hashlib.sha256(
        json.dumps(
            {
                "source_map": source_map,
                "source_recipe": source_index,
                "source_revision": GT6_REVISION,
                "item_inputs": item_inputs,
                "item_input_counts": item_input_counts,
                "item_input_actions": item_input_actions
                or [{"kind": "consume"} for _ in item_inputs],
                "item_outputs": item_outputs,
                "fluid_inputs": fluid_inputs or [],
                "fluid_outputs": fluid_outputs or [],
            },
            ensure_ascii=False,
            sort_keys=True,
        ).encode("utf-8")
    ).hexdigest()
    document: dict[str, Any] = {
        "can_be_buffered": bool(recipe.get("canBeBuffered", True)),
        "duration": int(recipe.get("duration") or 1),
        "eut": int(recipe.get("euPerTick") or 1),
        "item_input_counts": item_input_counts,
        "item_input_actions": item_input_actions
        or [{"kind": "consume"} for _ in item_inputs],
        "item_inputs": item_inputs,
        "item_outputs": item_outputs,
        "map": cc_map,
        "provenance": {
            "claim_owner": t39.OWNER,
            "evidence_hashes": [GT6_REVISION],
            "selected_source_recipe": (
                f"gt6_dump/gt6_recipe_dump/maps/{source_map}.json"
                f"#recipes[{source_index}]"
            ),
            "source_fingerprint": fingerprint,
            "source_kind": "gt6_pinned_dump_projection",
        },
        "type": "cruciblecraft:gt_recipe",
    }
    if fluid_inputs:
        document["fluid_inputs"] = fluid_inputs
    if fluid_outputs:
        document["fluid_outputs"] = fluid_outputs
    if item_outputs:
        document["output_chances"] = [10_000 for _ in item_outputs]
        chances = recipe.get("chances")
        if isinstance(chances, list) and len(chances) == len(item_outputs):
            document["output_chances"] = [int(value) for value in chances]
    return document


def _emit_gt_route(
    *,
    source_map: str,
    source_index: int,
    recipe: dict[str, Any],
    crosswalk: Crosswalk,
    output_identity: str,
) -> tuple[dict[str, Any], str]:
    cc_map = CC_MAP[source_map]
    item_inputs: list[dict[str, Any]] = []
    item_input_counts: list[int] = []
    item_input_actions: list[dict[str, Any]] = []
    for stack in recipe.get("inputs") or []:
        if not isinstance(stack, dict):
            continue
        count = int(stack.get("count") or 0)
        identity = crosswalk.map_item_stack(stack)
        if not identity:
            continue
        cc_item = _cc_item_from_identity(identity)
        if not cc_item:
            continue
        item_inputs.append({"item": cc_item})
        item_input_counts.append(max(0, count))
        item_input_actions.append(
            {"kind": "preserve" if count <= 0 else "consume"}
        )

    item_outputs: list[dict[str, Any]] = []
    for stack in recipe.get("outputs") or []:
        if not isinstance(stack, dict):
            continue
        identity = crosswalk.map_item_stack(stack)
        if not identity:
            continue
        cc_item = _cc_item_from_identity(identity)
        if not cc_item:
            continue
        item_outputs.append(
            {
                "count": int(stack.get("count") or 1),
                "id": cc_item,
            }
        )

    fluid_inputs: list[dict[str, Any]] = []
    for side in ("fluidInputs", "inputFluids"):
        for stack in recipe.get(side) or []:
            if not isinstance(stack, dict):
                continue
            identity = crosswalk.map_fluid_stack(stack)
            if not identity:
                raise ValueError(f"unmapped GT6 fluid input in {source_map}#{source_index}")
            cc_fluid = _cc_fluid_from_identity(identity)
            if not cc_fluid:
                raise ValueError(f"non-CC fluid input {identity}")
            fluid_inputs.append(
                {
                    "amount": int(stack.get("amount") or 0),
                    "id": cc_fluid,
                }
            )

    fluid_outputs: list[dict[str, Any]] = []
    for side in ("fluidOutputs", "outputFluids"):
        for stack in recipe.get(side) or []:
            if not isinstance(stack, dict):
                continue
            identity = crosswalk.map_fluid_stack(stack)
            if not identity:
                continue
            cc_fluid = _cc_fluid_from_identity(identity)
            if not cc_fluid:
                continue
            fluid_outputs.append(
                {
                    "amount": int(stack.get("amount") or 0),
                    "id": cc_fluid,
                }
            )

    if not item_inputs and not fluid_inputs:
        raise ValueError(f"{source_map}#{source_index} has no mapped inputs")

    if output_identity.startswith("fluid:"):
        cc_fluid = _cc_fluid_from_identity(output_identity)
        if not cc_fluid or not any(row["id"] == cc_fluid for row in fluid_outputs):
            raise ValueError(f"{source_map}#{source_index} missing output {output_identity}")
    else:
        cc_item = _cc_item_from_identity(output_identity)
        if not cc_item or not any(row["id"] == cc_item for row in item_outputs):
            raise ValueError(f"{source_map}#{source_index} missing output {output_identity}")

    prefix = source_map.removeprefix("gt.recipe.")
    stem = _filename_stem(prefix, output_identity)
    document = _gt_recipe_document(
        source_map=source_map,
        source_index=source_index,
        cc_map=cc_map,
        recipe=recipe,
        item_inputs=item_inputs,
        item_input_counts=item_input_counts,
        item_input_actions=item_input_actions,
        item_outputs=item_outputs,
        fluid_inputs=fluid_inputs or None,
        fluid_outputs=fluid_outputs or None,
    )
    return document, stem


def _emit_unpack_route(
    *,
    source_map: str,
    source_index: int,
    recipe: dict[str, Any],
    crosswalk: Crosswalk,
    output_identity: str,
) -> tuple[dict[str, Any], str]:
    inputs = recipe.get("inputs") or []
    outputs = recipe.get("outputs") or []
    if len(inputs) != 1 or len(outputs) != 1:
        raise ValueError(f"boxinator route {source_index} is not 1->1")
    in_identity = crosswalk.map_item_stack(inputs[0])
    out_identity = crosswalk.map_item_stack(outputs[0])
    if not in_identity or not out_identity:
        raise ValueError(f"unmapped boxinator stack in #{source_index}")
    in_item = _cc_item_from_identity(in_identity)
    out_item = _cc_item_from_identity(out_identity)
    if not in_item or not out_item:
        raise ValueError(f"non-CC boxinator stack in #{source_index}")
    if out_identity != output_identity:
        raise ValueError(f"boxinator #{source_index} output mismatch")
    count = int(inputs[0].get("count") or 1)
    document = {
        "category": "misc",
        "ingredients": [{"item": in_item} for _ in range(count)],
        "provenance": {
            "claim_owner": t39.OWNER,
            "evidence_hashes": [GT6_REVISION],
            "selected_source_recipe": (
                f"gt6_dump/gt6_recipe_dump/maps/{source_map}.json"
                f"#recipes[{source_index}]"
            ),
            "source_fingerprint": hashlib.sha256(
                json.dumps(
                    {
                        "source_map": source_map,
                        "source_recipe": source_index,
                        "in_item": in_item,
                        "out_item": out_item,
                    },
                    ensure_ascii=False,
                    sort_keys=True,
                ).encode("utf-8")
            ).hexdigest(),
            "source_kind": "gt6_pinned_dump_projection",
        },
        "result": {"count": int(outputs[0].get("count") or 1), "id": out_item},
        "type": "minecraft:crafting_shapeless",
    }
    stem = _filename_stem("unpack", output_identity)
    return document, stem


def _load_map(name: str) -> list[dict[str, Any]]:
    path = DUMP_ROOT / f"{name}.json"
    if not path.is_file():
        raise FileNotFoundError(f"missing GT6 dump map: {path}")
    recipes = _load(path).get("recipes")
    if not isinstance(recipes, list):
        raise ValueError(f"invalid GT6 dump map: {path}")
    return recipes


def _expand_relevant_materials(
    needed: set[str],
    crosswalk: Crosswalk,
    maps: dict[str, list[dict[str, Any]]],
) -> set[str]:
    relevant = _relevant_materials(needed)
    changed = True
    while changed:
        changed = False
        for recipes in maps.values():
            for recipe in recipes:
                if not isinstance(recipe, dict) or not _recipe_enabled(recipe):
                    continue
                item_ins, item_outs, fluid_ins, fluid_outs = _map_recipe_operands(
                    recipe,
                    crosswalk,
                )
                outputs = item_outs + fluid_outs
                if not any(
                    _identity_allowed(output, needed, relevant) for output in outputs
                ):
                    continue
                for identity in item_ins + fluid_ins:
                    item = _cc_item_from_identity(identity)
                    if item:
                        material = _material_from_cc_item(item)
                        if material and material not in relevant:
                            relevant.add(material)
                            changed = True
                        continue
                    fluid = _cc_fluid_from_identity(identity)
                    if fluid:
                        material = _material_from_cc_fluid(fluid)
                        if material and material not in relevant:
                            relevant.add(material)
                            changed = True
    return relevant


def _family_atomic_closed(
    player_path: dict[str, Any],
    identities: set[str],
) -> dict[str, Any]:
    trusted: list[dict[str, Any]] = []
    source_by_template = _source_by_template()
    for template_key, rows in sorted(_families_by_template(player_path).items()):
        if not _family_is_signable(template_key, rows, source_by_template):
            continue
        if not all(_row_inputs_satisfied(row, identities) for row in rows):
            continue
        trusted.append(
            {
                "expanded_count": len(rows),
                "template_key": template_key,
            }
        )
    singleton = [row for row in trusted if row["expanded_count"] == 1]
    multi = [row for row in trusted if row["expanded_count"] > 1]
    return {
        "families": len(trusted),
        "multi_families": len(multi),
        "multi_relations": sum(row["expanded_count"] for row in multi),
        "relations": sum(row["expanded_count"] for row in trusted),
        "singleton_families": len(singleton),
        "template_keys": [row["template_key"] for row in trusted],
    }


def _missing_inputs_for_closed_families(
    player_path: dict[str, Any],
    baseline: set[str],
    frontier: set[str],
) -> set[str]:
    needed: set[str] = set()
    source_by_template = _source_by_template()
    for template_key, rows in _families_by_template(player_path).items():
        if not _family_is_signable(template_key, rows, source_by_template):
            continue
        if not all(_row_inputs_satisfied(row, frontier) for row in rows):
            continue
        for row in rows:
            for item in row.get("consume_ids") or []:
                identity = _identity_from_cc_item(item)
                if identity not in baseline:
                    needed.add(identity)
            for fluid in row.get("fluid_input_ids") or []:
                identity = _identity_from_cc_fluid(fluid)
                if identity not in baseline:
                    needed.add(identity)
    return needed


def _min_family_atomic_slice(
    routes: list[dict[str, Any]],
    files: dict[Path, str],
    player_path: dict[str, Any],
    baseline: set[str],
    frontier: set[str],
) -> tuple[list[dict[str, Any]], dict[Path, str], set[str]]:
    targets = _missing_inputs_for_closed_families(player_path, baseline, frontier)
    by_output: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for route in routes:
        outputs = route.get("output_identities") or [route["output_identity"]]
        for output in outputs:
            by_output[output].append(route)
    keep: set[tuple[str, int]] = set()
    pending = set(targets)
    seen: set[str] = set()
    while pending:
        identity = pending.pop()
        if identity in seen or identity in baseline:
            continue
        seen.add(identity)
        for route in by_output.get(identity, []):
            key = (route["source_map"], route["source_recipe"])
            if key in keep:
                continue
            keep.add(key)
            for input_identity in route.get("input_identities") or []:
                if input_identity not in baseline:
                    pending.add(input_identity)
    kept_routes = [
        route
        for route in routes
        if (route["source_map"], route["source_recipe"]) in keep
    ]
    kept_files: dict[Path, str] = {}
    pruned_frontier = set(baseline)
    for route in kept_routes:
        path = ROOT / route["recipe_path"]
        if path in files:
            kept_files[path] = files[path]
        for output in route.get("output_identities") or [route["output_identity"]]:
            pruned_frontier.add(output)
    return kept_routes, kept_files, pruned_frontier


def _discover_routes(
    player_path: dict[str, Any],
    reachable: set[str],
    crosswalk: Crosswalk,
    maps: dict[str, list[dict[str, Any]]],
) -> tuple[list[dict[str, Any]], dict[Path, str], set[str]]:
    needed_items, needed_fluids = _needed_identities(player_path, reachable)
    needed = needed_items | needed_fluids
    relevant = _expand_relevant_materials(needed, crosswalk, maps)
    frontier = set(reachable)

    accepted: list[dict[str, Any]] = []
    files: dict[Path, str] = {}
    accepted_outputs: set[str] = set()
    accepted_sources: set[tuple[str, int]] = set()
    max_rounds = 32

    for round_index in range(max_rounds):
        progress = False
        for source_map, recipes in maps.items():
            for index, recipe in enumerate(recipes):
                if not isinstance(recipe, dict) or not _recipe_enabled(recipe):
                    continue
                if (source_map, index) in accepted_sources:
                    continue
                item_ins, item_outs, fluid_ins, fluid_outs = _map_recipe_operands(
                    recipe,
                    crosswalk,
                )
                all_inputs = item_ins + fluid_ins
                all_outputs = item_outs + fluid_outs
                if not all_inputs or not all_outputs:
                    continue
                if any(operand not in frontier for operand in all_inputs):
                    continue
                useful_outputs = [
                    output
                    for output in all_outputs
                    if _identity_allowed(output, needed, relevant)
                    and output not in accepted_outputs
                ]
                if not useful_outputs:
                    continue
                if not _fits_host_envelope(
                    source_map,
                    recipe,
                    item_ins,
                    item_outs,
                    fluid_ins,
                    fluid_outs,
                ):
                    continue
                if source_map == "gt.recipe.unboxinator":
                    if len(all_inputs) != 1 or len(all_outputs) != 1:
                        continue
                    try:
                        document, stem = _emit_unpack_route(
                            source_map=source_map,
                            source_index=index,
                            recipe=recipe,
                            crosswalk=crosswalk,
                            output_identity=useful_outputs[0],
                        )
                    except ValueError:
                        continue
                    map_kind = "unboxinator"
                    route_output = useful_outputs[0]
                elif source_map == "gt.recipe.smelter":
                    if not fluid_outs:
                        continue
                    primary = next(
                        (output for output in fluid_outs if output in useful_outputs),
                        None,
                    )
                    if primary is None:
                        continue
                    try:
                        document, stem = _emit_gt_route(
                            source_map=source_map,
                            source_index=index,
                            recipe=recipe,
                            crosswalk=crosswalk,
                            output_identity=primary,
                        )
                    except ValueError:
                        continue
                    map_kind = "smelter"
                    route_output = primary
                elif source_map == "gt.recipe.mixer":
                    primary = next(
                        (output for output in (item_outs + fluid_outs) if output in useful_outputs),
                        None,
                    )
                    if primary is None:
                        continue
                    try:
                        document, stem = _emit_gt_route(
                            source_map=source_map,
                            source_index=index,
                            recipe=recipe,
                            crosswalk=crosswalk,
                            output_identity=primary,
                        )
                    except ValueError:
                        continue
                    map_kind = "mixer"
                    route_output = primary
                else:
                    continue

                path = OUTPUT_ROOT / f"{stem}.json"
                if path.name.endswith("_recovery.json") or path.name.endswith("_packing.json"):
                    path = OUTPUT_ROOT / f"{stem}_support.json"
                if path in files:
                    continue

                accepted_sources.add((source_map, index))
                route = {
                    "input_identities": sorted(set(all_inputs)),
                    "kind": map_kind,
                    "output_identities": sorted(set(all_outputs)),
                    "output_identity": route_output,
                    "recipe_path": path.relative_to(ROOT).as_posix(),
                    "source_map": source_map,
                    "source_recipe": index,
                }
                accepted.append(route)
                files[path] = _stable(document)
                for output in all_outputs:
                    if _identity_allowed(output, needed, relevant):
                        frontier.add(output)
                        accepted_outputs.add(output)
                relevant.update(_output_materials(all_outputs))
                progress = True
        if not progress:
            break

    accepted, files, frontier = _min_family_atomic_slice(
        accepted,
        files,
        player_path,
        reachable,
        frontier,
    )
    remaining = {
        identity
        for identity in needed
        if identity not in frontier
    }
    return accepted, files, remaining


def _locked_route_keys() -> list[str] | None:
    if not t39.PRODUCTION_LOCK.is_file():
        return None
    lock = _load(t39.PRODUCTION_LOCK)
    if lock.get("status") != "T39_PRODUCTION_LOCKED":
        return None
    keys = list((lock.get("support") or {}).get("route_keys") or [])
    if not keys:
        return None
    layered_path = t39.LAYERED_PLAYER_PATH
    if layered_path.is_file():
        layered = _load(layered_path)
        ordered = [
            str(row.get("route_key") or "")
            for row in (layered.get("support_routes") or [])
            if row.get("route_key")
        ]
        if sorted(ordered) == sorted(keys):
            return ordered
    return keys


def _emit_locked_routes(
    route_keys: list[str],
    player_path: dict[str, Any],
    reachable: set[str],
    crosswalk: Crosswalk,
    maps: dict[str, list[dict[str, Any]]],
) -> tuple[list[dict[str, Any]], dict[Path, str], set[str]]:
    needed_items, needed_fluids = _needed_identities(player_path, reachable)
    needed = needed_items | needed_fluids
    frontier = set(reachable)
    accepted: list[dict[str, Any]] = []
    files: dict[Path, str] = {}
    for route_key in route_keys:
        source_map, _, index_text = route_key.partition("#")
        index = int(index_text)
        recipes = maps.get(source_map) or []
        if index < 0 or index >= len(recipes):
            raise ValueError(f"T39 locked support route is missing from dump: {route_key}")
        recipe = recipes[index]
        item_ins, item_outs, fluid_ins, fluid_outs = _map_recipe_operands(
            recipe,
            crosswalk,
        )
        all_inputs = item_ins + fluid_ins
        all_outputs = item_outs + fluid_outs
        if source_map == "gt.recipe.unboxinator":
            document, stem = _emit_unpack_route(
                source_map=source_map,
                source_index=index,
                recipe=recipe,
                crosswalk=crosswalk,
                output_identity=all_outputs[0],
            )
            map_kind = "unboxinator"
            route_output = all_outputs[0]
        elif source_map == "gt.recipe.smelter":
            primary = fluid_outs[0]
            document, stem = _emit_gt_route(
                source_map=source_map,
                source_index=index,
                recipe=recipe,
                crosswalk=crosswalk,
                output_identity=primary,
            )
            map_kind = "smelter"
            route_output = primary
        elif source_map == "gt.recipe.mixer":
            primary = (item_outs + fluid_outs)[0]
            document, stem = _emit_gt_route(
                source_map=source_map,
                source_index=index,
                recipe=recipe,
                crosswalk=crosswalk,
                output_identity=primary,
            )
            map_kind = "mixer"
            route_output = primary
        else:
            raise ValueError(f"T39 locked support map is not emit-able: {source_map}")
        path = OUTPUT_ROOT / f"{stem}.json"
        if path.name.endswith("_recovery.json") or path.name.endswith("_packing.json"):
            path = OUTPUT_ROOT / f"{stem}_support.json"
        if path in files:
            path = OUTPUT_ROOT / f"{stem}_{index}.json"
        files[path] = _stable(document)
        accepted.append({
            "input_identities": sorted(set(all_inputs)),
            "kind": map_kind,
            "output_identities": sorted(set(all_outputs)),
            "output_identity": route_output,
            "recipe_path": path.relative_to(ROOT).as_posix(),
            "source_map": source_map,
            "source_recipe": index,
        })
        frontier.update(all_outputs)
    remaining = {
        identity
        for identity in needed
        if identity not in frontier
    }
    return accepted, files, remaining


def build() -> tuple[dict[str, Any], dict[Path, str]]:
    player_path = _load(PLAYER_PATH)
    t21_doc = _load(T21)
    reachable = set(t21_doc["closure"]["reachable_identities"])
    crosswalk = Crosswalk(_load(CROSS))

    maps = {
        "gt.recipe.smelter": _load_map("gt.recipe.smelter"),
        "gt.recipe.mixer": _load_map("gt.recipe.mixer"),
        "gt.recipe.unboxinator": _load_map("gt.recipe.unboxinator"),
    }
    locked_keys = _locked_route_keys()
    if locked_keys is not None:
        routes, files, remaining = _emit_locked_routes(
            locked_keys,
            player_path,
            reachable,
            crosswalk,
            maps,
        )
    else:
        routes, files, remaining = _discover_routes(
            player_path, reachable, crosswalk, maps
        )
    support_identities = set(reachable)
    for route in routes:
        support_identities.update(route.get("output_identities") or [route["output_identity"]])

    document = {
        "schema_version": 1,
        "status": "T39_PLAYER_PATH_SUPPORT",
        "source_revision": GT6_REVISION,
        "discovery": {
            "accepted_routes": len(routes),
            "family_atomic_closed": _family_atomic_closed(player_path, support_identities),
            "remaining_unmapped": sorted(remaining),
        },
        "tracked_input_hashes": {
            "gt6_mixer_dump": _digest(DUMP_ROOT / "gt.recipe.mixer.json"),
            "gt6_smelter_dump": _digest(DUMP_ROOT / "gt.recipe.smelter.json"),
            "gt6_unboxinator_dump": _digest(DUMP_ROOT / "gt.recipe.unboxinator.json"),
            "gt6_oredict_cross_reference": _digest(CROSS),
            "t21_operand_reachability": _digest(T21),
            "t39_player_path": _digest(PLAYER_PATH),
        },
        "routes": routes,
    }
    return document, files


def check() -> list[str]:
    document, files = build()
    errors: list[str] = []
    if not OUTPUT.is_file() or _load(OUTPUT) != document:
        errors.append(f"stale: {OUTPUT.relative_to(ROOT).as_posix()}")
    for path, content in files.items():
        if not path.is_file() or path.read_text(encoding="utf-8") != content:
            errors.append(f"stale: {path.relative_to(ROOT).as_posix()}")
    return errors


def write() -> None:
    document, files = build()
    OUTPUT.write_text(_stable(document), encoding="utf-8")
    OUTPUT_ROOT.mkdir(parents=True, exist_ok=True)
    keep = set(files)
    for pattern in ("mixer_*.json", "smelter_*.json", "unpack_*.json", "boxinator_*.json"):
        for path in OUTPUT_ROOT.glob(pattern):
            if path not in keep:
                path.unlink()
    for path, content in files.items():
        path.write_text(content, encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    parser.add_argument(
        "--diagnose",
        action="store_true",
        help="Run order-independent route discovery without writing production files",
    )
    args = parser.parse_args()
    if sum(bool(flag) for flag in (args.write, args.check, args.diagnose)) != 1:
        parser.error("choose exactly one of --write, --check, or --diagnose")
    if args.diagnose:
        player_path = _load(PLAYER_PATH)
        t21_doc = _load(T21)
        reachable = set(t21_doc["closure"]["reachable_identities"])
        crosswalk = Crosswalk(_load(CROSS))
        maps = {
            "gt.recipe.smelter": _load_map("gt.recipe.smelter"),
            "gt.recipe.mixer": _load_map("gt.recipe.mixer"),
            "gt.recipe.unboxinator": _load_map("gt.recipe.unboxinator"),
        }
        routes, _files, remaining = _discover_routes(
            player_path, reachable, crosswalk, maps
        )
        print(
            json.dumps(
                {
                    "mode": "diagnose",
                    "discovered_routes": len(routes),
                    "locked_routes": 34,
                    "remaining_unmapped": sorted(remaining),
                    "production_unchanged": True,
                },
                indent=2,
                sort_keys=True,
            )
        )
        if len(routes) != 34:
            print(
                "discovery drifted from 34; production emit stays locked",
                file=sys.stderr,
            )
        return 0
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
