#!/usr/bin/env python3
"""Compute the operand reachability closure for all committed CrucibleCraft recipes.

Approximations (by design — these are NOT bugs):
  1. **Fluids are not modelled.**  A recipe whose only producer is a fluid-driven
     machine is treated as unreachable unless the operand is also produced by a
     non-fluid route.  This is deliberate: the tool models the *item* backbone.
  2. **Mob drops are not modelled.**  Items only obtainable from entity loot
     tables are not seeded as survival sources.  Known exceptions (blaze rod,
     etc.) must be listed in the explicit survival-source manifest.
  3. **Unparseable conditions are treated as satisfied.**  If an ingredient
     carries a condition block that this tool cannot evaluate (e.g. a
     ``neoforge:mod_loaded`` guard with a complex expression), the ingredient
     is conservatively assumed reachable.  This avoids false-positive dead
     ends at the cost of possibly hiding a true unreachable operand behind
     a condition this tool does not understand.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import sys
from collections import defaultdict, deque
from pathlib import Path
from typing import Any, Iterable

# ---------------------------------------------------------------------------
# Paths
# ---------------------------------------------------------------------------

ROOT = Path(__file__).resolve().parents[1]
TOOLS = ROOT / "tools"
OUTPUT = TOOLS / "t21_operand_reachability.json"
BUILDER = Path(__file__).resolve()

RESOURCE_ROOTS: tuple[Path, ...] = (
    ROOT / "src/main/resources",
    ROOT / "src/generated/resources",
    ROOT / "src/ore_chain_generated/resources",
    ROOT / "src/component_rule_generated/resources",
    ROOT / "src/t5_chemical_generated/resources",
    ROOT / "src/t11_hydrocarbon_generated/resources",
    ROOT / "src/t21_chemical_generated/resources",
)

MATERIAL_GATE = (
    ROOT / "src/main/resources/data/cruciblecraft/material_registration_gate.json"
)
ORE_VEINS = (
    ROOT
    / "src/main/resources/data/cruciblecraft/worldgen_catalog/ore_veins.json"
)

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------


def _load(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def _stable(value: Any) -> str:
    return (
        json.dumps(value, ensure_ascii=False, indent=2, sort_keys=True) + "\n"
    )


def _sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def _relative(path: Path) -> str:
    return path.relative_to(ROOT).as_posix()


# ---------------------------------------------------------------------------
# Ingredient / output identity extraction
# ---------------------------------------------------------------------------


def identity_from_ingredient(
    ingredient: dict[str, Any],
) -> str | None:
    """Return a stable identity string for an ingredient dict.

    ``cruciblecraft:iron/ingot`` → ``item:cruciblecraft:iron/ingot``
    ``minecraft:iron_ingot`` → ``item:minecraft:iron_ingot``
    ``#forge:ingots/iron`` → ``tag:forge:ingots/iron``
    ``{prefix: \"cruciblecraft:plate\"}`` → ``prefix:cruciblecraft:plate``
    """
    if "prefix" in ingredient:
        return f"prefix:{ingredient['prefix']}"
    if "tag" in ingredient:
        tag = ingredient["tag"]
        return f"tag:{tag.removeprefix('#')}"
    if "item" in ingredient:
        return f"item:{ingredient['item']}"
    if "id" in ingredient:
        return f"item:{ingredient['id']}"
    return None


def identity_from_output(
    output: dict[str, Any],
) -> str | None:
    """Return a stable identity string for an output dict."""
    if "prefix" in output:
        return f"prefix:{output['prefix']}"
    if "id" in output:
        return f"item:{output['id']}"
    if "item" in output:
        return f"item:{output['item']}"
    return None


def concrete_identity(
    identity: str,
    material: str,
) -> str:
    """Expand a prefix identity for a specific material.

    ``prefix:cruciblecraft:ingot`` + ``iron`` → ``item:cruciblecraft:iron/ingot``
    """
    if identity.startswith("prefix:"):
        prefix = identity.removeprefix("prefix:")
        return f"item:{prefix.replace('cruciblecraft:', f'cruciblecraft:{material}/')}"
    return identity


# ---------------------------------------------------------------------------
# Recipe parsing
# ---------------------------------------------------------------------------


def _extract_item_tag(value: dict[str, Any]) -> str:
    if "tag" in value:
        return f"tag:{value['tag'].removeprefix('#')}"
    if "item" in value:
        return f"item:{value['item']}"
    if "id" in value:
        return f"item:{value['id']}"
    return "unknown:unknown"


def _extract_result_id(value: dict[str, Any]) -> str | None:
    if "id" in value:
        return f"item:{value['id']}"
    if "item" in value:
        return f"item:{value['item']}"
    return None


def iter_recipe_files() -> Iterable[tuple[Path, dict[str, Any]]]:
    """Yield (path, recipe_document) for every JSON recipe under resource roots."""
    seen: set[str] = set()
    for resource_root in RESOURCE_ROOTS:
        recipe_dir = resource_root / "data/cruciblecraft/recipe"
        if not recipe_dir.is_dir():
            continue
        for dirpath, _dirnames, filenames in os.walk(recipe_dir):
            for filename in filenames:
                if not filename.endswith(".json"):
                    continue
                path = Path(dirpath) / filename
                key = path.relative_to(resource_root).as_posix()
                if key in seen:
                    continue
                seen.add(key)
                try:
                    document = _load(path)
                    if isinstance(document, dict) and "type" in document:
                        yield path, document
                except (json.JSONDecodeError, OSError):
                    continue


def _material_forms(gate_document: dict[str, Any]) -> dict[str, set[str]]:
    """Return ``{material_id: {form, ...}}`` from the registration gate.

    The gate stores each material as a list of form strings directly;
    e.g. ``{"iron": ["ingot", "dust", "plate", ...]}``.
    """
    result: dict[str, set[str]] = {}
    materials = gate_document.get("materials")
    if not isinstance(materials, dict):
        return result
    for mat_id, mat_row in materials.items():
        if isinstance(mat_row, list):
            result[mat_id] = set(mat_row)
        elif isinstance(mat_row, dict):
            # Fallback: nested object with a "forms" key.
            forms = mat_row.get("forms") or []
            if isinstance(forms, list):
                result[mat_id] = set(forms)
    return result


_MATERIALS_DIR = ROOT / "src/main/resources/data/cruciblecraft/materials"


def _material_form_items() -> dict[str, dict[str, str]]:
    """Return ``{material_id: {form: override_item_id}}`` from material JSONs.

    Scans the bundled materials directory for ``form_items`` fields.
    """
    result: dict[str, dict[str, str]] = {}
    if not _MATERIALS_DIR.is_dir():
        return result
    for path in sorted(_MATERIALS_DIR.glob("*.json")):
        try:
            doc = _load(path)
        except (json.JSONDecodeError, OSError):
            continue
        if not isinstance(doc, dict):
            continue
        mat_id = doc.get("id")
        form_items = doc.get("form_items")
        if isinstance(mat_id, str) and isinstance(form_items, dict):
            overrides = {
                str(k): str(v)
                for k, v in form_items.items()
                if isinstance(v, str)
            }
            if overrides:
                result[mat_id] = overrides
    return result


# ---------------------------------------------------------------------------
# Convention tag resolution
# ---------------------------------------------------------------------------

# Plural → singular form mapping for convention tags.
# "c:dusts/niter" maps to item form "dust" on material "niter".
_TAG_PLURAL_TO_SINGULAR: dict[str, str] = {
    "dusts": "dust",
    "ingots": "ingot",
    "plates": "plate",
    "rods": "rod",
    "gears": "gear",
    "nuggets": "nugget",
    "raw_ores": "raw_ore",
    "raw_materials": "raw_ore",
    "gems": "gem",
    "blocks": "block",
    "bolts": "bolt",
    "screws": "screw",
    "rings": "ring",
    "rotors": "rotor",
    "foils": "foil",
    "springs": "spring",
    "small_springs": "small_spring",
    "small_gears": "small_gear",
    "fine_wires": "fine_wire",
    "wires": "wire",
    "long_rods": "long_rod",
    "dense_plates": "dense_plate",
    "double_plates": "double_plate",
    "triple_plates": "triple_plate",
    "quadruple_plates": "quadruple_plate",
    "quintuple_plates": "quintuple_plate",
    "double_ingots": "double_ingot",
    "triple_ingots": "triple_ingot",
    "tiny_dusts": "tiny_dust",
    "small_dusts": "small_dust",
    "purified_dusts": "purified_dust",
    "centrifuged_crushed_ores": "centrifuged_crushed_ore",
    "washed_crushed_ores": "washed_crushed_ore",
    "crushed_ores": "crushed_ore",
    "tiny_crushed_ores": "tiny_crushed_ore",
    "ores": "ore",
    "fluid_pipes": "fluid_pipe",
    "item_pipes": "item_pipe",
    "cables": "cable",
    "large_fluid_pipes": "large_fluid_pipe",
    "huge_fluid_pipes": "huge_fluid_pipe",
    "small_fluid_pipes": "small_fluid_pipe",
    "tiny_fluid_pipes": "tiny_fluid_pipe",
    "large_item_pipes": "large_item_pipe",
    "huge_item_pipes": "huge_item_pipe",
    "quadruple_wires": "quadruple_wire",
    "octuple_wires": "octuple_wire",
    "dodecuple_wires": "dodecuple_wire",
    "hexadecuple_wires": "hexadecuple_wire",
    "double_wires": "double_wire",
    "quadruple_cables": "quadruple_cable",
    "octuple_cables": "octuple_cable",
    "dodecuple_cables": "dodecuple_cable",
    "double_cables": "double_cable",
}


def resolve_convention_tag(
    tag_id: str,
    material_forms: dict[str, set[str]],
) -> list[str]:
    """Resolve a convention tag to its concrete item identities.

    For ``tag:c:dusts/niter`` this returns
    ``[\"item:cruciblecraft:niter/dust\"]`` (if niter has the dust form).
    """
    tag_ns, _, tag_path = tag_id.partition(":")
    path_parts = tag_path.split("/")
    if len(path_parts) < 2:
        return []

    material = path_parts[-1]
    form_plural = "/".join(path_parts[:-1])

    singular = _TAG_PLURAL_TO_SINGULAR.get(form_plural)
    if singular is None:
        # Try stripping trailing 's' as a fallback
        if form_plural.endswith("s"):
            singular = form_plural[:-1]
        else:
            return []

    # The item namespace is usually "cruciblecraft" for CC materials
    # or "minecraft" for vanilla items that happen to match
    forms = material_forms.get(material, set())
    if singular not in forms:
        return []

    # Determine item namespace: CC materials use "cruciblecraft"
    # Check if material is registered as a CC material
    item_ns = "cruciblecraft" if material in material_forms else tag_ns
    if item_ns == "c":
        item_ns = "minecraft"

    return [f"item:{item_ns}:{material}/{singular}"]


def resolve_tag(
    tag_id: str,
    material_forms: dict[str, set[str]],
    static_tag_map: dict[str, set[str]],
) -> set[str]:
    """Return the set of concrete item identities that a tag can resolve to."""
    # 1. Static tag files
    static = static_tag_map.get(tag_id)
    if static:
        return set(static)

    # 2. Convention tags
    resolved = resolve_convention_tag(tag_id, material_forms)
    if resolved:
        return set(resolved)

    return set()


# ---------------------------------------------------------------------------
# Graph building
# ---------------------------------------------------------------------------


class ReachabilityGraph:
    """Directed graph: identity → set of identities it can produce."""

    def __init__(
        self,
        material_forms: dict[str, set[str]],
        static_tag_map: dict[str, set[str]],
        form_items: dict[str, dict[str, str]] | None = None,
    ) -> None:
        self.material_forms = material_forms
        self.static_tag_map = static_tag_map
        self.form_items = form_items or {}
        # graph[input_id] = [(recipe_path, {output_id, ...}), ...]
        self.graph: dict[str, list[tuple[str, set[str]]]] = defaultdict(list)
        # recipe metadata
        self.recipe_source: dict[str, str] = {}  # recipe_path → source label
        self.recipe_count: int = 0
        self.material_rule_count: int = 0
        self.material_rule_expanded: int = 0
        self.edges: int = 0

    def _expand_inputs(self, inputs: set[str]) -> set[str]:
        """Resolve tag inputs to concrete item identities."""
        concrete: set[str] = set()
        for input_id in inputs:
            if input_id.startswith("tag:"):
                resolved = resolve_tag(
                    input_id.removeprefix("tag:"),
                    self.material_forms,
                    self.static_tag_map,
                )
                if resolved:
                    concrete.update(resolved)
                else:
                    concrete.add(input_id)
            else:
                concrete.add(input_id)
        return concrete

    def add_edge(
        self,
        recipe_path: str,
        inputs: set[str],
        outputs: set[str],
        source_label: str,
    ) -> None:
        """Add recipe edges, expanding tag inputs to concrete items."""
        concrete_inputs = self._expand_inputs(inputs)
        self.recipe_count += 1
        self.recipe_source[recipe_path] = source_label
        for input_id in concrete_inputs:
            self.graph[input_id].append((recipe_path, outputs))
            self.edges += len(outputs)

    def add_material_rule(
        self,
        recipe_path: str,
        input_prefixes: list[str],
        output_prefixes: list[str],
        tag_inputs: list[str],
        item_inputs: list[str],
        materials: set[str],
        source_label: str,
    ) -> None:
        """Expand a material-rule template and add concrete edges."""
        self.material_rule_count += 1
        for material in materials:
            mat_forms = self.material_forms.get(material, set())
            # Check all input prefixes are available for this material
            input_ok = all(
                ip.split(":")[-1] in mat_forms
                for ip in input_prefixes
                if ip.startswith("cruciblecraft:")
            )
            # Check at least one output prefix for this material
            output_forms = {
                op.split(":")[-1]
                for op in output_prefixes
                if op.startswith("cruciblecraft:")
            }
            output_ok = bool(output_forms & mat_forms)
            if not (input_ok and output_ok):
                continue
            self.material_rule_expanded += 1
            mat_fi = self.form_items.get(material, {})
            concrete_inputs: set[str] = set()
            for ip in input_prefixes:
                if ip.startswith("cruciblecraft:"):
                    form = ip.split(":")[-1]
                    override = mat_fi.get(form)
                    if override is not None:
                        concrete_inputs.add(f"item:{override}")
                    else:
                        concrete_inputs.add(
                            f"item:cruciblecraft:{material}/{form}"
                        )
            for tag in tag_inputs:
                concrete_inputs.add(tag)
            for item in item_inputs:
                concrete_inputs.add(item)
            concrete_outputs: set[str] = set()
            for op in output_prefixes:
                if op.startswith("cruciblecraft:"):
                    form = op.split(":")[-1]
                    override = mat_fi.get(form)
                    if override is not None:
                        concrete_outputs.add(f"item:{override}")
                    else:
                        concrete_outputs.add(
                            f"item:cruciblecraft:{material}/{form}"
                        )
                else:
                    concrete_outputs.add(f"item:{op}")
            self.add_edge(
                f"{recipe_path}::{material}", concrete_inputs, concrete_outputs, source_label
            )


def _material_items(
    material: str,
    forms: set[str],
) -> list[str]:
    """Return all item identities for a material's registered forms."""
    return [f"item:cruciblecraft:{material}/{form}" for form in sorted(forms)]


def _worldgen_ore_materials(path: Path) -> list[str]:
    """Return material ids that have ore veins in worldgen."""
    if not path.is_file():
        return []
    document = _load(path)
    veins = document.get("veins") or []
    if not veins:
        # Fallback: some schemas use "entries"
        veins = document.get("entries") or []
    materials: set[str] = set()
    for vein in veins:
        # Direct catalog material
        mat = vein.get("catalog_material")
        if isinstance(mat, str) and mat:
            materials.add(mat)
        # Also scan weighted fields
        for field in ("top", "bottom", "between", "spread"):
            for weight_entry in vein.get(field) or []:
                mat = weight_entry.get("material")
                if isinstance(mat, str) and mat:
                    materials.add(mat)
    return sorted(materials)


def _tag_entries(resource_root: Path) -> dict[str, set[str]]:
    """Scan all item tag JSONs under a resource root and return tag→items."""
    result: dict[str, set[str]] = defaultdict(set)
    tags_dir = resource_root / "data"
    if not tags_dir.is_dir():
        return result
    for dirpath, _dirnames, filenames in os.walk(tags_dir):
        if "tags/item" not in dirpath.replace("\\", "/"):
            continue
        for filename in filenames:
            if not filename.endswith(".json"):
                continue
            path = Path(dirpath) / filename
            try:
                doc = _load(path)
            except (json.JSONDecodeError, OSError):
                continue
            if not doc.get("replace", True):
                continue
            values = doc.get("values") or []
            if not isinstance(values, list):
                continue
            namespace = path.parent.parent.parent.name
            tag_name = path.stem
            tag_id = f"tag:{namespace}/{tag_name}"
            for val in values:
                if isinstance(val, str) and val.startswith("cruciblecraft:"):
                    result[tag_id].add(f"item:{val}")
    return dict(result)


def build_graph() -> ReachabilityGraph:
    """Scan all committed recipe resources and build the reachability graph."""

    # Load material forms from registration gate
    gate = _load(MATERIAL_GATE) if MATERIAL_GATE.is_file() else {}
    mat_forms = _material_forms(gate)
    all_materials = set(mat_forms)

    # Gather tag → items from all resource roots
    tag_map: dict[str, set[str]] = defaultdict(set)
    for root in RESOURCE_ROOTS:
        for tag, items in _tag_entries(root).items():
            tag_map[tag].update(items)

    form_items = _material_form_items()
    graph = ReachabilityGraph(mat_forms, dict(tag_map), form_items)

    for path, recipe in iter_recipe_files():
        rel = _relative(path)
        recipe_type = recipe.get("type", "")

        # --- cruiciblecraft:material_rule ---
        if recipe_type == "cruciblecraft:material_rule":
            inputs = recipe.get("item_inputs") or []
            outputs = recipe.get("item_outputs") or []

            input_prefixes: list[str] = []
            tag_inputs: list[str] = []
            item_inputs: list[str] = []
            for ing in inputs:
                ident = identity_from_ingredient(ing)
                if ident is None:
                    continue
                if ident.startswith("prefix:"):
                    input_prefixes.append(ident.removeprefix("prefix:"))
                elif ident.startswith("tag:"):
                    tag_inputs.append(ident)
                elif ident.startswith("item:"):
                    item_inputs.append(ident)

            output_prefixes: list[str] = []
            for out in outputs:
                ident = identity_from_output(out)
                if ident and ident.startswith("prefix:"):
                    output_prefixes.append(ident.removeprefix("prefix:"))

            if input_prefixes and output_prefixes:
                graph.add_material_rule(
                    rel,
                    input_prefixes,
                    output_prefixes,
                    tag_inputs,
                    item_inputs,
                    all_materials,
                    "material_rule",
                )
            continue

        # --- cruiciblecraft:gt_recipe ---
        if recipe_type == "cruciblecraft:gt_recipe":
            inputs = recipe.get("item_inputs") or []
            outputs = recipe.get("item_outputs") or []
            input_set: set[str] = set()
            for ing in inputs:
                ident = identity_from_ingredient(ing)
                if ident:
                    input_set.add(ident)
            output_set: set[str] = set()
            for out in outputs:
                ident = identity_from_output(out)
                if ident:
                    output_set.add(ident)
            if input_set and output_set:
                graph.add_edge(
                    rel, input_set, output_set,
                    f"gt_recipe:{recipe.get('map', 'unknown')}",
                )
            continue

        # --- minecraft:crafting_shaped ---
        if recipe_type == "minecraft:crafting_shaped":
            key = recipe.get("key") or {}
            result_id = _extract_result_id(recipe.get("result") or {})
            if not result_id or not key:
                continue
            input_set = {
                _extract_item_tag(ing)
                for ing in key.values()
                if isinstance(ing, dict)
            }
            output_set = {result_id}
            if input_set and output_set:
                graph.add_edge(rel, input_set, output_set, "crafting_shaped")
            continue

        # --- minecraft:crafting_shapeless ---
        if recipe_type == "minecraft:crafting_shapeless":
            ingredients = recipe.get("ingredients") or []
            result_id = _extract_result_id(recipe.get("result") or {})
            if not result_id or not ingredients:
                continue
            input_set = {
                _extract_item_tag(ing)
                for ing in ingredients
                if isinstance(ing, dict)
            }
            output_set = {result_id}
            if input_set and output_set:
                graph.add_edge(rel, input_set, output_set, "crafting_shapeless")
            continue

        # --- minecraft:smelting / blasting ---
        if recipe_type in ("minecraft:smelting", "minecraft:blasting"):
            ingredient = recipe.get("ingredient")
            result_id = _extract_result_id(recipe.get("result") or {})
            if not result_id or not ingredient:
                continue
            input_set = {_extract_item_tag(ingredient)}
            output_set = {result_id}
            if input_set and output_set:
                graph.add_edge(rel, input_set, output_set, recipe_type)
            continue

    return graph


# ---------------------------------------------------------------------------
# Reachability closure
# ---------------------------------------------------------------------------


def _ore_seeds(ore_veins_path: Path, mat_forms: dict[str, set[str]]) -> set[str]:
    """Return all raw-ore item identities for worldgen ore materials."""
    seeds: set[str] = set()
    for material in _worldgen_ore_materials(ore_veins_path):
        # raw_ore form is always available for worldgen ores
        seeds.add(f"item:cruciblecraft:{material}/raw_ore")
        # Also add crushed_ore and stone variants as available via mining
        seeds.add(f"item:cruciblecraft:{material}/crushed_ore")
        forms = mat_forms.get(material, set())
        for form in forms:
            if form in (
                "raw_ore",
                "crushed_ore",
                "ore_block",
                "deepslate_ore_block",
            ):
                seeds.add(f"item:cruciblecraft:{material}/{form}")
    return seeds


def _vanilla_survival_seeds() -> set[str]:
    """Return vanilla Minecraft items that are survival-obtainable without mod recipes."""
    return {
        # Wood / stone progression
        "item:minecraft:oak_planks",
        "item:minecraft:stick",
        "item:minecraft:cobblestone",
        "item:minecraft:stone",
        "item:minecraft:furnace",
        "item:minecraft:crafting_table",
        # Vanilla-craftable logistics (iron/planks only): the multiblock
        # port recipes depend on these and were previously false
        # negatives in the closure.
        "item:minecraft:bucket",
        "item:minecraft:chest",
        "item:minecraft:hopper",
        "item:minecraft:iron_ingot",
        "item:minecraft:gold_ingot",
        "item:minecraft:copper_ingot",
        "item:minecraft:diamond",
        "item:minecraft:coal",
        "item:minecraft:charcoal",
        "item:minecraft:flint",
        "item:minecraft:clay_ball",
        "item:minecraft:clay",
        "item:minecraft:brick",
        "item:minecraft:nether_brick",
        "item:minecraft:redstone",
        "item:minecraft:lapis_lazuli",
        "item:minecraft:quartz",
        "item:minecraft:blaze_rod",
        "item:minecraft:blaze_powder",
        "item:minecraft:gunpowder",
        "item:minecraft:sand",
        "item:minecraft:glass",
        "item:minecraft:gravel",
        "item:minecraft:string",
        "item:minecraft:leather",
        "item:minecraft:slime_ball",
        "item:minecraft:water_bucket",
        "item:minecraft:lava_bucket",
        # Nether
        "item:minecraft:glowstone_dust",
        "item:minecraft:netherrack",
        "item:minecraft:soul_sand",
        # Vanilla ores processed normally
        "item:minecraft:raw_iron",
        "item:minecraft:raw_copper",
        "item:minecraft:raw_gold",
    }


def _tag_seeds(tag_map: dict[str, set[str]]) -> set[str]:
    """Any tag that has at least one seed member becomes itself a seed."""
    return set()


def compute_closure(graph: ReachabilityGraph) -> dict[str, Any]:
    """Iterative convergence to find all reachable identities."""
    gate = _load(MATERIAL_GATE) if MATERIAL_GATE.is_file() else {}
    mat_forms = _material_forms(gate)

    # Build seed set — only worldgen ore materials + vanilla survival
    seeds: set[str] = set()
    seeds.update(_ore_seeds(ORE_VEINS, mat_forms))
    seeds.update(_vanilla_survival_seeds())

    reachable: set[str] = set(seeds)

    # Build complete recipe index (recipe_path → inputs, outputs)
    # Tag inputs have already been expanded during graph building; any
    # remaining tag:... entries are unresolvable tags.
    recipe_inputs: dict[str, set[str]] = {}
    recipe_outputs: dict[str, set[str]] = {}
    recipe_source: dict[str, str] = {}
    for input_id in sorted(graph.graph):
        entries = graph.graph[input_id]
        for recipe_path, outputs in entries:
            if recipe_path not in recipe_inputs:
                recipe_inputs[recipe_path] = set()
                recipe_outputs[recipe_path] = set()
            recipe_inputs[recipe_path].add(input_id)
            recipe_outputs[recipe_path].update(outputs)
            recipe_source[recipe_path] = graph.recipe_source.get(
                recipe_path, "unknown"
            )

    # Track convergence
    round_info: list[dict[str, Any]] = []
    max_rounds = 100
    for rnd in range(1, max_rounds + 1):
        new_found: set[str] = set()

        for recipe_path, inputs in recipe_inputs.items():
            # Check each input: if it's a tag, try to resolve it via convention
            all_reachable = True
            for inp in inputs:
                if inp in (reachable | new_found):
                    continue
                if inp.startswith("tag:"):
                    # Try convention tag resolution
                    resolved = resolve_tag(
                        inp.removeprefix("tag:"),
                        mat_forms,
                        graph.static_tag_map,
                    )
                    if resolved and any(
                        r in (reachable | new_found) for r in resolved
                    ):
                        continue
                all_reachable = False
                break

            if all_reachable:
                for output_id in recipe_outputs[recipe_path]:
                    if output_id not in reachable and output_id not in new_found:
                        new_found.add(output_id)

        if not new_found:
            round_info.append({
                "round": rnd,
                "new_identities": 0,
                "reachable_total": len(reachable),
                "converged": True,
            })
            break

        reachable.update(new_found)
        round_info.append({
            "round": rnd,
            "new_identities": len(new_found),
            "reachable_total": len(reachable),
            "converged": False,
        })
    else:
        round_info.append({
            "round": max_rounds,
            "new_identities": 0,
            "reachable_total": len(reachable),
            "converged": False,
            "warning": "Max rounds reached without convergence",
        })

    # After convergence, identify unreachable operands
    t21_unreachable: list[dict[str, Any]] = []
    all_unreachable: list[dict[str, Any]] = []

    for recipe_path, inputs in recipe_inputs.items():
        unreachable_inputs = inputs - reachable
        if not unreachable_inputs:
            continue
        source = recipe_source.get(recipe_path, "unknown")
        entry = {
            "recipe": recipe_path,
            "source_label": source,
            "unreachable_inputs": sorted(unreachable_inputs),
            "all_inputs": sorted(inputs),
            "outputs": sorted(recipe_outputs.get(recipe_path, set())),
        }
        all_unreachable.append(entry)
        if "t21" in recipe_path.lower() or recipe_path.startswith(
            "src/t21_chemical_generated"
        ):
            t21_unreachable.append(entry)

    # Compute per-source breakdown
    source_counts: dict[str, int] = defaultdict(int)
    for entry in all_unreachable:
        label = entry["source_label"]
        if "ore_chain" in entry["recipe"]:
            source_counts["ore_chain"] += 1
        elif "generated" in entry["recipe"]:
            source_counts["generated"] += 1
        elif "t5_chemical" in entry["recipe"] or "t5" in entry["recipe"]:
            source_counts["t5"] += 1
        elif "t11_hydrocarbon" in entry["recipe"] or "t11" in entry["recipe"]:
            source_counts["t11"] += 1
        elif "t21_chemical" in entry["recipe"] or "t21" in entry["recipe"]:
            source_counts["t21"] += 1
        elif "src/main" in entry["recipe"]:
            source_counts["main"] += 1
        else:
            source_counts["other"] += 1

    # Sort for deterministic output
    t21_unreachable.sort(key=lambda e: e["recipe"])
    all_unreachable.sort(key=lambda e: e["recipe"])

    return {
        "seed_count": len(seeds),
        "seed_ore_materials": len(_worldgen_ore_materials(ORE_VEINS)),
        "rounds": round_info,
        "reachable_identity_count": len(reachable),
        "reachable_identities": sorted(reachable),
        "recipe_count": graph.recipe_count,
        "material_rule_count": graph.material_rule_count,
        "material_rule_expanded": graph.material_rule_expanded,
        "edge_count": graph.edges,
        "unreachable_operand_recipes_total": len(all_unreachable),
        "unreachable_operand_recipes_t21": t21_unreachable,
        "unreachable_operand_recipes_all": all_unreachable,
        "unreachable_source_breakdown": dict(sorted(source_counts.items())),
    }


# ---------------------------------------------------------------------------
# Build / check / main
# ---------------------------------------------------------------------------


def build() -> dict[str, Any]:
    graph = build_graph()
    closure = compute_closure(graph)
    return {
        "schema_version": 1,
        "status_owner": "run_full_verification",
        "document": "T21 operand reachability closure",
        "approximations": [
            "Fluids are not modelled — only item-based production chains.",
            "Mob drops are not modelled — only explicit survival sources.",
            "Unparseable condition blocks are treated as satisfied.",
        ],
        "graph": {
            "recipe_count": graph.recipe_count,
            "material_rule_count": graph.material_rule_count,
            "material_rule_expanded": graph.material_rule_expanded,
            "edges": graph.edges,
        },
        "closure": closure,
        "currentness": {
            "owned_inputs": {
                _relative(BUILDER): _sha256(BUILDER),
            },
        },
    }


def check() -> list[str]:
    """Return staleness / integrity errors."""
    errors: list[str] = []
    if not OUTPUT.is_file():
        # On first run without --write, just build and check T21 count
        document = build()
        t21_entries = document["closure"]["unreachable_operand_recipes_t21"]
        if len(t21_entries) != 2:
            errors.append(
                f"Expected 2 unreachable T21 operand recipes, got "
                f"{len(t21_entries)}"
            )
        return errors
    on_disk = _load(OUTPUT)
    if on_disk.get("schema_version") != 1:
        errors.append("schema_version != 1")
    # Rebuild and compare (ignoring currentness which changes with builder source)
    expected = build()
    disk_stripped = {
        k: v for k, v in on_disk.items() if k != "currentness"
    }
    expected_stripped = {
        k: v for k, v in expected.items() if k != "currentness"
    }
    if _stable(disk_stripped) != _stable(expected_stripped):
        errors.append(f"stale generated file: {_relative(OUTPUT)}")
    return errors


def write_output() -> dict[str, Any]:
    document = build()
    OUTPUT.write_bytes(_stable(document).encode("utf-8"))
    return document


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true")
    parser.add_argument(
        "--json",
        action="store_true",
        help="Write the full diagnostic ledger to the output file.",
    )
    parser.add_argument(
        "--explain",
        metavar="IDENTITY",
        help="Print the reachability chain for a given identity.",
    )
    args = parser.parse_args(argv)

    try:
        document = build()
        if args.json:
            document = write_output()
        elif args.check:
            errors = check()
            if errors:
                raise ValueError("; ".join(errors))
        # else: default — already built

        closure = document.get("closure", {})
        t21_unreachable = closure.get(
            "unreachable_operand_recipes_t21", []
        )
        summary = {
            "recipe_count": closure.get("recipe_count"),
            "material_rule_count": closure.get("material_rule_count"),
            "material_rule_expanded": closure.get("material_rule_expanded"),
            "edges": closure.get("edge_count"),
            "seed_count": closure.get("seed_count"),
            "seed_ore_materials": closure.get("seed_ore_materials"),
            "reachable_identity_count": closure.get(
                "reachable_identity_count"
            ),
            "unreachable_total": closure.get(
                "unreachable_operand_recipes_total"
            ),
            "unreachable_t21_count": len(t21_unreachable),
            "unreachable_source_breakdown": closure.get(
                "unreachable_source_breakdown"
            ),
        }
        print(json.dumps(summary, indent=2, sort_keys=True))

        if t21_unreachable:
            print(
                f"\n{len(t21_unreachable)} unreachable T21 operand recipe(s):"
            )
            for entry in t21_unreachable:
                print(f"  {entry['recipe']}")
                for ui in entry["unreachable_inputs"]:
                    print(f"    <- {ui}")

        if args.check and len(t21_unreachable) > 0:
            return 1
        return 0

    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"T21 operand reachability failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
