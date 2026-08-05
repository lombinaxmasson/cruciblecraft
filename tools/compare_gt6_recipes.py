#!/usr/bin/env python3
"""Normalize CrucibleCraft recipes against a GT6 recipe dump and emit a JSON report.

Comparison IR:
  resource = item:<material>:<form> | item:<item_id> | fluid:<id>
  recipe key = (family, frozenset(inputs), frozenset(primary_outputs))
"""

from __future__ import annotations

import copy
import json
import hashlib
import ast
import math
import re
import sys
from collections import Counter, defaultdict
from dataclasses import dataclass, field
from functools import cache
from pathlib import Path
from typing import Any

try:
    from tools import gt6_l3_materials
except ModuleNotFoundError:
    import gt6_l3_materials

ROOT = Path(__file__).resolve().parents[1]
GT_MAPS = ROOT / "gt6_dump" / "gt6_recipe_dump" / "maps"
GT_INDEX = ROOT / "gt6_dump" / "gt6_recipe_dump" / "index.json"
CC_MATERIALS = ROOT / "src" / "main" / "resources" / "data" / "cruciblecraft" / "materials"
CC_PREFIXES = ROOT / "src" / "main" / "resources" / "data" / "cruciblecraft" / "material_prefixes"
CC_REGISTRATION_GATE = (
    ROOT / "src" / "main" / "resources" / "data" / "cruciblecraft"
    / "material_registration_gate.json"
)
L3_PREFIX_PLAN = ROOT / "tools" / "gt6_l3_prefix_plan.json"
CC_HAND = ROOT / "src" / "main" / "resources" / "data" / "cruciblecraft" / "recipe"
CC_GEN = ROOT / "src" / "generated" / "resources" / "data" / "cruciblecraft" / "recipe"
CC_COMPONENT_GEN = (
    ROOT
    / "src"
    / "component_rule_generated"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "recipe"
)
CC_COMPONENT_TAGS = (
    ROOT
    / "src"
    / "component_rule_generated"
    / "resources"
    / "data"
)
ORE_CHAIN_INDEX = ROOT / "tools" / "gt6_ore_chain.json"
OUT_JSON = ROOT / "tools" / "gt6_recipe_compare_report.json"
BASELINE_JSON = ROOT / "tools" / "gt6_recipe_compare_baseline.json"
REFERENCE_JSON = ROOT / "tools" / "gt6_recipe_normalized_reference.json"
LOCAL_ARTIFACT_MANIFEST = ROOT / "tools" / "local_artifact_manifest.json"
EXPECTATIONS_JSON = ROOT / "tools" / "gt6_recipe_expectations.json"
EXPECTATIONS_SUGGESTED_JSON = ROOT / "tools" / "gt6_recipe_expectations_suggested.json"
REFERENCE_METADATA_JSON = ROOT / "tools" / "gt6_reference_metadata.json"
ROADMAP_JSON = ROOT / "tools" / "gt6_map_roadmap.json"
PROCESS_EXPECTATIONS_JSON = ROOT / "tools" / "gt6_process_expectations.json"
OREDICT_CROSS_REFERENCE_JSON = ROOT / "tools" / "gt6_oredict_cross_reference.json"

MATCH_TIERS = ("EXACT", "FORM_PATH", "SEMANTIC", "NONE")
VERDICTS = ("EXACT", "INTENTIONAL", "TODO_PORT", "OUT_OF_SCOPE")
# Recipe maps whose specialValue is a GT6 Kelvin temperature. Normalized IR uses °C
# (same truncation as import_gt6_oredict.GT6ImportUnits.kelvin_to_celsius → int()).
KELVIN_SPECIAL_VALUE_MAPS = frozenset({
    "gt.recipe.cruciblealloying",
})
PLACEHOLDER_REASONS = {
    "",
    "review",
    "todo",
    "tbd",
    "fixme",
    "untracked",
    "no direct gt map counterpart",
    "reviewed difference",
}
GENERIC_RATIONALE_FRAGMENTS = (
    "reviewed cc/gt numerical model difference",
    "has no directly normalized gt6 counterpart",
    "authoritative numeric source-id and prefix mappings produced no comparable recipe",
    "authoritative numeric source-id/prefix normalization shows different material chemistry",
)


def is_placeholder_text(value: Any) -> bool:
    normalized = re.sub(r"\s+", " ", str(value or "").strip()).lower()
    if normalized.rstrip(".:;,-_/ ") in PLACEHOLDER_REASONS:
        return True
    return bool(
        re.search(
            r"(?:^|[\s:;/,()[\]{}_-])"
            r"(?:tbd|fixme|todo|review(?:ed|ing)?)"
            r"(?:$|[\s:;/,.()[\]{}_-])",
            normalized,
        )
    )


def load_oredict_cross_reference() -> dict[str, Any]:
    if not OREDICT_CROSS_REFERENCE_JSON.is_file():
        raise ValueError(
            "Missing authoritative GT6 ore-dictionary cross-reference; "
            "run tools/import_gt6_oredict.py"
        )
    return json.loads(OREDICT_CROSS_REFERENCE_JSON.read_text(encoding="utf-8"))


OREDICT_CROSS_REFERENCE = load_oredict_cross_reference()
# GT item path -> CC material-prefix path, authored by the deterministic importer.
FORM_FROM_ITEM: dict[str, str] = dict(
    OREDICT_CROSS_REFERENCE["prefix_item_to_form"]
)

# Vanilla / CC special items that map to material forms
VANILLA_FORM_ITEMS: dict[str, tuple[str, str]] = {
    "minecraft:iron_ingot": ("iron", "ingot"),
    "minecraft:raw_iron": ("iron", "raw_ore"),
    "minecraft:copper_ingot": ("copper", "ingot"),
    "minecraft:raw_copper": ("copper", "raw_ore"),
    "minecraft:gold_ingot": ("gold", "ingot"),
    "minecraft:raw_gold": ("gold", "raw_ore"),
    "minecraft:coal": ("coal", "gem"),  # special; coke oven only
    "cruciblecraft:coal_coke": ("coal_coke", "gem"),
}

FLUID_ALIASES: dict[str, str] = {
    "creosote": "creosote",
    "cruciblecraft:creosote": "creosote",
    "minecraft:water": "water",
}
CC_PREFIX_UNITS: dict[str, int] = {
    "block": 1296,
    "raw_ore": 144,
    "crushed_ore": 144,
    "tiny_crushed_ore": 16,
    "washed_crushed_ore": 144,
    "centrifuged_crushed_ore": 144,
    "purified_dust": 144,
    "ingot": 144,
    "dust": 144,
    "plate": 144,
    "rod": 72,
    "long_rod": 144,
    "small_dust": 36,
    "bolt": 18,
    "screw": 16,
    "ring": 36,
    "spring": 144,
    "small_spring": 36,
    "gear": 576,
    "small_gear": 144,
    "rotor": 612,
    "foil": 36,
    "double_plate": 288,
    "triple_plate": 432,
    "quadruple_plate": 576,
    "quintuple_plate": 720,
    "dense_plate": 1296,
    "fine_wire": 18,
    "wire": 72,
    "double_wire": 144,
    "quadruple_wire": 288,
    "octuple_wire": 576,
    "dodecuple_wire": 864,
    "hexadecuple_wire": 1152,
    "cable": 72,
    "double_cable": 144,
    "quadruple_cable": 288,
    "octuple_cable": 576,
    "dodecuple_cable": 864,
    "nugget": 16,
}
CABLE_INSULATION_PLATES = {
    "cable": 1,
    "double_cable": 1,
    "quadruple_cable": 2,
    "octuple_cable": 3,
    "dodecuple_cable": 4,
}

# Display-name prefixes / suffixes for material extraction
DISPLAY_FORM_SUFFIXES: list[tuple[str, str]] = [
    (" Crushed Ore", "crushed_ore"),
    (" Raw Ore", "raw_ore"),
    (" Dust", "dust"),
    (" Plate", "plate"),
    (" Rod", "rod"),
    (" Bolt", "bolt"),
    (" Nugget", "nugget"),
    (" Ingot", "ingot"),
    (" Block", "block"),
]

# Process families used for apples-to-apples comparison
FAMILIES: dict[str, dict[str, Any]] = {
    "coke_oven": {
        "gt_maps": ["gt.recipe.cokeoven"],
        "cc_kind": "coke_oven",
        "note": "Direct GTRecipe parity candidate",
    },
    "crush_raw_to_crushed": {
        "gt_maps": ["gt.recipe.crusher"],
        "cc_kind": "crusher_rule",
        "cc_io": ("raw_ore", "crushed_ore"),
        "note": "CC is same-material 1:1; GT6 ore chemistry often remaps minerals",
    },
    "form_ingot_to_plate": {
        "gt_maps": ["gt.recipe.rollingmill", "gt.recipe.extruder"],
        "cc_kind": "anvil_rule",
        "cc_io": ("ingot", "plate"),
        "preferred_gt": "gt.recipe.rollingmill",
        "note": "Machine IO benchmark: rolling mill / extruder; GT anvil audited separately",
    },
    "form_plate_to_rod": {
        "gt_maps": ["gt.recipe.extruder"],
        "cc_kind": "anvil_rule",
        "cc_io": ("plate", "rod"),
        "note": "Machine IO benchmark: GT extruder plate→2 rods; GT anvil audited separately",
    },
    "form_rod_to_bolt": {
        "gt_maps": ["gt.recipe.cutter", "gt.recipe.extruder"],
        "cc_kind": "anvil_rule",
        "cc_io": ("rod", "bolt"),
        "preferred_gt": "gt.recipe.cutter",
        "note": "Machine IO benchmark: GT cutter/extruder rod→4 bolts; GT anvil audited separately",
    },
    "anvil_raw_to_crushed": {
        "gt_maps": ["gt.recipe.anvil"],
        "cc_kind": "anvil_rule",
        "cc_io": ("raw_ore", "crushed_ore"),
        "note": "Direct GT anvil benchmark; GT adds 6 tiny crushed ore and uses 30–90 ticks",
    },
    "chain_sluice": {
        "gt_maps": ["gt.recipe.sluice"],
        "cc_kind": "material_rule",
        "cc_target": "cruciblecraft:sluice",
        "cc_io": ("crushed_ore", "washed_crushed_ore"),
        "note": "Water-assisted crushed-ore washing with ordered GT6 byproducts",
    },
    "chain_bath": {
        "gt_maps": ["gt.recipe.bath"],
        "cc_kind": "material_rule",
        "cc_target": "cruciblecraft:bath",
        "cc_io": ("crushed_ore", "washed_crushed_ore"),
        "note": "Bath alternative for water-assisted crushed-ore washing",
    },
    "chain_centrifuge": {
        "gt_maps": ["gt.recipe.centrifuge"],
        "cc_kind": "material_rule",
        "cc_target": "cruciblecraft:centrifuge",
        "cc_io": ("washed_crushed_ore", "centrifuged_crushed_ore"),
        "note": "Washed ore refinement with ordered GT6 byproducts",
    },
    "chain_shredder": {
        "gt_maps": ["gt.recipe.shredder"],
        "cc_kind": "material_rule",
        "cc_target": "cruciblecraft:shredder",
        "cc_io": ("centrifuged_crushed_ore", "purified_dust"),
        "note": "Refined ore comminution into purified dust",
    },
    "chain_sifter": {
        "gt_maps": ["gt.recipe.sifter"],
        "cc_kind": "material_rule",
        "cc_target": "cruciblecraft:sifter",
        "cc_io": ("purified_dust", "dust"),
        "note": "Purified dust finishing",
    },
    "chain_smelter": {
        "gt_maps": ["gt.recipe.smelter"],
        "cc_kind": "material_rule",
        "cc_target": "cruciblecraft:smelter",
        "cc_io": ("dust", "ingot"),
        "note": "Metadata-targeted dust smelting",
    },
    "chain_mortar": {
        "gt_maps": ["gt.recipe.mortar"],
        "cc_kind": "material_rule",
        "cc_target": "cruciblecraft:mortar",
        "cc_io": ("crushed_ore", "dust"),
        "note": "Mechanical crushed-ore grinding alternative",
    },
    **{
        f"component_{name}": {
            "gt_maps": [f"gt.recipe.{gt_map}"],
            "cc_kind": "material_rule",
            "cc_target": f"cruciblecraft:{name}",
            "note": "T3 metadata-gated component rules; machine implementation deferred.",
        }
        for name, gt_map in {
            "extruder": "extruder",
            "cutter": "cutter",
            "lathe": "lathe",
            "rollingmill": "rollingmill",
            "rollbender": "rollbender",
            "wiremill": "wiremill",
            "bender": "rollbender",
            "assembler": "assembler",
            "welder": "welder",
            "press": "press",
        }.items()
    },
    "alloy": {
        "gt_maps": ["gt.recipe.cruciblealloying"],
        "cc_kind": "composition",
        "note": "CC composition rules vs GT fake alloying recipes",
    },
    "cook_smelting": {
        "gt_maps": ["mc.recipe.furnace"],
        "cc_kind": "vanilla_smelting",
        "note": (
            "GT6 source-derived furnace IO; its non-GT recipe map executes "
            "at 16 ticks and 16 EU/t."
        ),
    },
    "cook_blasting": {
        "gt_maps": [],
        "cc_kind": "vanilla_blasting",
        "note": "Minecraft 1.21 blasting has no GT6/1.7.10 equivalent.",
    },
}


@dataclass(frozen=True)
class Resource:
    kind: str  # item | fluid
    id: str  # material:form | bare item/fluid id
    count: int

    def key(self) -> str:
        return f"{self.kind}:{self.id}x{self.count}"


def canonical_resources(resources: list[Resource]) -> list[Resource]:
    """Merge equal resources so GT's duplicate output slots compare by total count."""
    counts: Counter[tuple[str, str]] = Counter()
    for resource in resources:
        counts[(resource.kind, resource.id)] += resource.count
    return [
        Resource(kind, resource_id, count)
        for (kind, resource_id), count in sorted(counts.items())
    ]


@dataclass
class NormRecipe:
    family: str
    source: str  # cc | gt
    map_name: str
    material: str | None
    inputs: list[Resource]
    outputs: list[Resource]
    duration: int
    eut: int
    special_value: int = 0
    chances: list[int] = field(default_factory=list)
    fake: bool = False
    notes: list[str] = field(default_factory=list)
    raw_hint: str = ""

    def signature(self) -> tuple:
        in_keys = tuple(r.key() for r in canonical_resources(self.inputs))
        out_keys = tuple(r.key() for r in canonical_resources(self.outputs))
        return (self.family, in_keys, out_keys, self.canonical_output_chances())

    def identity_signature(self) -> tuple:
        """Stable policy-row identity; exact matching uses the chance-aware signature."""
        in_keys = tuple(r.key() for r in canonical_resources(self.inputs))
        out_keys = tuple(r.key() for r in canonical_resources(self.outputs))
        return (self.family, in_keys, out_keys)

    def canonical_output_chances(self) -> tuple:
        """Canonicalize chances without erasing independent probabilistic slots."""
        item_outputs = [resource for resource in self.outputs if resource.kind == "item"]
        normalized = tuple(
            self.chances[index] if index < len(self.chances) else 10_000
            for index in range(len(item_outputs))
        )
        guaranteed: Counter[tuple[str, str]] = Counter()
        probabilistic: list[tuple[str, int]] = []
        for resource, chance in zip(item_outputs, normalized):
            if chance == 10_000:
                guaranteed[(resource.kind, resource.id)] += resource.count
            else:
                probabilistic.append((resource.key(), chance))

        if not probabilistic and len(self.chances) <= len(item_outputs):
            return (10_000,) * len(guaranteed)

        paired = [
            (Resource(kind, resource_id, count).key(), 10_000)
            for (kind, resource_id), count in sorted(guaranteed.items())
        ]
        paired.extend(probabilistic)
        paired.extend(
            (f"orphan_chance:{index}", chance)
            for index, chance in enumerate(
                self.chances[len(item_outputs):],
                start=len(item_outputs),
            )
        )
        return tuple(sorted(paired))

    def io_form_signature(self) -> tuple | None:
        """Same-material form transform signature when applicable."""
        inputs = canonical_resources(self.inputs)
        outputs = canonical_resources(self.outputs)
        if len(inputs) != 1 or len(outputs) != 1:
            return None
        inp = inputs[0]
        out = outputs[0]
        if not (inp.id.count(":") == 1 and out.id.count(":") == 1):
            return None
        in_mat, in_form = inp.id.split(":", 1)
        out_mat, out_form = out.id.split(":", 1)
        if in_mat != out_mat:
            return (self.family, in_mat, in_form, inp.count, out_mat, out_form, out.count)
        return (self.family, in_mat, in_form, inp.count, out_form, out.count)


def stable_json(value: Any) -> str:
    return json.dumps(value, sort_keys=True, separators=(",", ":"), ensure_ascii=False)


def stable_hash(value: Any) -> str:
    return hashlib.sha256(stable_json(value).encode("utf-8")).hexdigest()


def shadowed_recipe_count(recipes: list[NormRecipe]) -> int:
    by_input: Counter[tuple] = Counter(
        (recipe.map_name, tuple(
            resource.key() for resource in canonical_resources(recipe.inputs)))
        for recipe in recipes)
    return sum(count - 1 for count in by_input.values() if count > 1)


def recipe_row_id(recipe: NormRecipe) -> str:
    material = recipe.material or "global"
    signature_hash = hashlib.sha256(
        repr(recipe.identity_signature()).encode("utf-8")
    ).hexdigest()[:12]
    return f"{recipe.family}/{material}/{signature_hash}"


def resolve_t8_pipe_forms(material: dict[str, Any]) -> set[str]:
    fluid_forms = {
        "pipeTiny": "tiny_fluid_pipe",
        "pipeSmall": "small_fluid_pipe",
        "pipeMedium": "fluid_pipe",
        "pipeLarge": "large_fluid_pipe",
        "pipeHuge": "huge_fluid_pipe",
    }
    item_forms = {
        "pipeMedium": "item_pipe",
        "pipeLarge": "large_item_pipe",
        "pipeHuge": "huge_item_pipe",
    }
    properties = (
        material.get("gt6_metadata", {}).get("pipe_properties", {})
    )
    forms = {
        fluid_forms[specification]
        for specification in (
            properties.get("fluid_by_specification") or {}
        )
    } | {
        item_forms[specification]
        for specification in (
            properties.get("item_by_specification") or {}
        )
    }
    generation_flags = set(material.get("generation_flags") or [])
    missing = {
        f"cruciblecraft:generates_{form}"
        for form in forms
        if f"cruciblecraft:generates_{form}" not in generation_flags
    }
    if missing:
        raise ValueError(
            f"T8 pipe forms lack generation flags for "
            f"{material.get('id')}: {sorted(missing)}"
        )
    return forms


@cache
def _load_cc_materials_cached() -> dict[str, dict[str, Any]]:
    materials: dict[str, dict[str, Any]] = {}
    index = json.loads((CC_MATERIALS / "index.json").read_text(encoding="utf-8"))
    gate = json.loads(CC_REGISTRATION_GATE.read_text(encoding="utf-8"))["materials"]
    if len(index) != len(set(index)):
        raise ValueError("Material index contains duplicate files")
    for filename in index:
        path = CC_MATERIALS / filename
        if not path.is_file():
            raise ValueError(f"Material index references missing file: {filename}")
        data = json.loads(path.read_text(encoding="utf-8"))
        if filename != f"{data['id']}.json":
            raise ValueError(f"Material index/file id mismatch: {filename}")
        factual_forms = resolve_material_forms(data)
        source_backed_pipe_forms = resolve_t8_pipe_forms(data)
        source_backed_t10_forms = resolve_t10_known_forms(data)
        if data["id"] not in gate:
            raise ValueError(f"Registration gate omits indexed material: {data['id']}")
        registered_forms = set(gate[data["id"]])
        if not registered_forms <= (
            factual_forms
            | source_backed_pipe_forms
            | source_backed_t10_forms
        ):
            raise ValueError(
                "Registration gate exceeds factual or source-backed T8/T10 "
                f"forms for {data['id']}"
            )
        data["_factual_forms"] = sorted(factual_forms)
        data["_resolved_forms"] = sorted(registered_forms)
        if data["id"] in materials:
            raise ValueError(f"Duplicate indexed material id: {data['id']}")
        materials[data["id"]] = data
    return materials


def load_cc_materials() -> dict[str, dict[str, Any]]:
    """Return an isolated view of the process-cached material catalog."""
    return copy.deepcopy(_load_cc_materials_cached())


def cached_cc_materials() -> dict[str, dict[str, Any]]:
    """Return the shared read-only-by-contract catalog for internal scanners."""
    return _load_cc_materials_cached()


def resolve_t10_known_forms(material: dict[str, Any]) -> set[str]:
    flags = set(material.get("generation_flags") or [])
    forms: set[str] = set()
    if "gt6:itemgenerator/multiingots" in flags:
        forms.update(("double_ingot", "triple_ingot"))
    if "gt6:itemgenerator/hotingots" in flags:
        forms.add("ingot_hot")
    return forms


@cache
def _prefix_resolution_inputs() -> tuple[dict[str, str], dict[str, Any]]:
    prefix_index = json.loads((CC_PREFIXES / "index.json").read_text(encoding="utf-8"))
    aliases: dict[str, str] = {}
    for filename in prefix_index:
        definition = json.loads((CC_PREFIXES / filename).read_text(encoding="utf-8"))
        form = definition["serialized_path"]
        aliases[definition["id"]] = form
        aliases[form] = form
        for alias in definition.get("aliases") or []:
            aliases[alias] = form
    document = json.loads(L3_PREFIX_PLAN.read_text(encoding="utf-8"))
    return aliases, document


def resolve_material_forms(material: dict[str, Any]) -> set[str]:
    if material.get("metadata_only"):
        return set()
    aliases, document = _prefix_resolution_inputs()
    generation_flags = material.get("generation_flags") or []
    includes = material.get("include_prefixes") or []
    excludes = material.get("exclude_prefixes") or []
    legacy = material.get("forms")
    if generation_flags or includes or excludes:
        normalized: dict[str, list[str]] = {
            "generation_flags": [
                flag if ":" in flag else f"cruciblecraft:{flag}"
                for flag in generation_flags
            ],
            "include_prefixes": [],
            "exclude_prefixes": [],
        }
        for field, prefixes in (
            ("include_prefixes", includes),
            ("exclude_prefixes", excludes),
        ):
            for prefix in prefixes:
                key = prefix if prefix in aliases else prefix.split(":")[-1]
                if key not in aliases:
                    raise ValueError(
                        f"Unknown {field.removesuffix('_prefixes')}d material "
                        f"prefix: {prefix}"
                    )
                normalized[field].append(aliases[key])
        return gt6_l3_materials.resolve_material_forms(normalized, document)

    forms = set()
    for prefix in (legacy if legacy is not None else ["dust"]):
        key = prefix if prefix in aliases else prefix.split(":")[-1]
        if key not in aliases:
            raise ValueError(f"Unknown legacy material prefix: {prefix}")
        forms.add(aliases[key])
    return forms


def clear_process_caches() -> None:
    """Clear process-local source caches after an intentional fixture mutation."""
    _load_cc_materials_cached.cache_clear()
    _prefix_resolution_inputs.cache_clear()
    _load_and_expand_cc_recipes_cached.cache_clear()


def material_forms(material: dict[str, Any]) -> set[str]:
    return (
        set(material["_resolved_forms"])
        if "_resolved_forms" in material
        else resolve_material_forms(material)
    )


def evaluate_rule_expression(
    expression: Any,
    material: dict[str, Any],
    extra_variables: dict[str, float] | None = None,
) -> int:
    """Evaluate the numeric, side-effect-free subset used by material_rule JSON."""
    text = str(expression)
    thermal = material.get("thermal") or {}
    metadata = material.get("gt6_metadata") or {}

    def replace_lookup(match: re.Match[str]) -> str:
        function, raw_key = match.group(1), match.group(2)
        key = raw_key.strip("'\"")
        if function == "prefix_units":
            value = CC_PREFIX_UNITS.get(key.split(":")[-1])
        else:
            target = (metadata.get("processing_targets") or {}).get(key)
            value = target and target.get("cc_units")
        if not isinstance(value, int) or value <= 0 or value > 9_007_199_254_740_991:
            raise ValueError(
                f"{function}({key}) requires a positive exact safe integer"
            )
        return str(value)

    text = re.sub(
        r"\b(target_units|prefix_units)\(\s*([A-Za-z0-9_.:-]+|'[^']+'|\"[^\"]+\")\s*\)",
        replace_lookup,
        text,
    )
    variables = {
        "material.tier": float(material.get("tier") or 0),
        "material.mass": float(thermal.get("density") or 0) * 144,
        "material.thermal.melting_point": float(thermal.get("melting_point") or 0),
        "material.thermal.boiling_point": float(thermal.get("boiling_point") or 0),
        "material.thermal.density": float(thermal.get("density") or 0),
    }
    variables.update(extra_variables or {})
    for name, value in variables.items():
        text = text.replace(name, str(value))
    tree = ast.parse(text, mode="eval")

    def visit(node: ast.AST) -> float:
        if isinstance(node, ast.Expression):
            return visit(node.body)
        if isinstance(node, ast.Constant) and isinstance(node.value, (int, float)):
            return float(node.value)
        if isinstance(node, ast.UnaryOp) and isinstance(node.op, (ast.UAdd, ast.USub)):
            value = visit(node.operand)
            return value if isinstance(node.op, ast.UAdd) else -value
        if isinstance(node, ast.BinOp) and isinstance(
            node.op, (ast.Add, ast.Sub, ast.Mult, ast.Div, ast.Mod)
        ):
            left, right = visit(node.left), visit(node.right)
            if isinstance(node.op, (ast.Div, ast.Mod)) and right == 0:
                raise ValueError("material_rule expression divides by zero")
            return {
                ast.Add: lambda: left + right,
                ast.Sub: lambda: left - right,
                ast.Mult: lambda: left * right,
                ast.Div: lambda: left / right,
                ast.Mod: lambda: left % right,
            }[type(node.op)]()
        if isinstance(node, ast.Call) and isinstance(node.func, ast.Name):
            args = [visit(arg) for arg in node.args]
            functions = {
                "min": min,
                "max": max,
                "ceil": math.ceil,
                "floor": math.floor,
                "tier_voltage": lambda tier: 8 * (4 ** int(tier)),
                "gcd": lambda left, right: math.gcd(
                    positive_safe_integer(left, "gcd"),
                    positive_safe_integer(right, "gcd"),
                ),
            }
            if node.func.id in functions:
                return float(functions[node.func.id](*args))
        raise ValueError(f"Unsupported material_rule expression: {expression!r}")

    def positive_safe_integer(value: float, function: str) -> int:
        if (
            not math.isfinite(value)
            or value != int(value)
            or value <= 0
            or value > 9_007_199_254_740_991
        ):
            raise ValueError(f"{function} requires positive exact safe integers")
        return int(value)

    value = visit(tree)
    if not math.isfinite(value) or value != int(value):
        raise ValueError(f"material_rule expression is not a finite integer: {expression!r}")
    return int(value)


def material_rule_family(target: str, input_prefix: str, output_prefix: str) -> str | None:
    for family, config in FAMILIES.items():
        io = config.get("cc_io")
        if io and tuple(io) != (input_prefix, output_prefix):
            continue
        kind = config.get("cc_kind")
        if kind == "crusher_rule" and target.endswith(":crusher"):
            return family
        if kind == "anvil_rule" and ":anvil" in target:
            return family
        if kind == "material_rule" and target == config.get("cc_target"):
            return family
    return None


def load_ore_chain_concrete_recipes(
    materials: dict[str, dict[str, Any]],
) -> list[NormRecipe]:
    """Normalize committed concrete ore-chain recipes into the comparison IR."""
    if not ORE_CHAIN_INDEX.is_file():
        return []
    prefix_by_tag: dict[tuple[str, str], str] = {}
    for filename in json.loads(
        (CC_PREFIXES / "index.json").read_text(encoding="utf-8")
    ):
        prefix = json.loads((CC_PREFIXES / filename).read_text(encoding="utf-8"))
        prefix_by_tag[(
            prefix.get("tag_namespace") or "cruciblecraft",
            prefix["tag_directory"],
        )] = Path(filename).stem
    override_items = {
        item_id: (material_id, form)
        for material_id, material in materials.items()
        for form, item_id in (material.get("form_items") or {}).items()
    }

    def material_item(item_id: str) -> tuple[str, str] | None:
        if item_id in override_items:
            return override_items[item_id]
        if item_id.startswith("cruciblecraft:"):
            path = item_id.split(":", 1)[1]
            if "/" in path:
                material_id, form = path.split("/", 1)
                if material_id in materials:
                    return material_id, form
        return None

    normalized: list[NormRecipe] = []
    index = json.loads(ORE_CHAIN_INDEX.read_text(encoding="utf-8"))
    for row in index["recipes"]:
        if row.get("source") == "high_version_ore_block_projection":
            # This is a Minecraft-version compatibility ingress derived from
            # the committed raw-ore recipe, not a GT6-equivalence candidate.
            continue
        path = ROOT / row["path"]
        document = json.loads(path.read_text(encoding="utf-8"))
        item_inputs = []
        for position, ingredient in enumerate(document.get("item_inputs") or []):
            count = int((document.get("item_input_counts") or [])[position])
            tag = ingredient.get("tag")
            if tag:
                namespace, tag_path = tag.split(":", 1)
                directory, material_id = tag_path.rsplit("/", 1)
                form = prefix_by_tag[(namespace, directory)]
                item_inputs.append(Resource("item", f"{material_id}:{form}", count))
            else:
                item_id = ingredient["item"]
                parsed = material_item(item_id)
                item_inputs.append(Resource(
                    "item",
                    f"{parsed[0]}:{parsed[1]}" if parsed else item_id,
                    count,
                ))
        item_outputs = []
        for output in document.get("item_outputs") or []:
            item_id = output["id"]
            parsed = material_item(item_id)
            item_outputs.append(Resource(
                "item",
                f"{parsed[0]}:{parsed[1]}" if parsed else item_id,
                int(output.get("count") or 1),
            ))
        fluid_inputs = [
            Resource(
                "fluid",
                FLUID_ALIASES.get(fluid["id"], fluid["id"]),
                int(fluid["amount"]),
            )
            for fluid in document.get("fluid_inputs") or []
        ]
        fluid_outputs = [
            Resource(
                "fluid",
                FLUID_ALIASES.get(fluid["id"], fluid["id"]),
                int(fluid["amount"]),
            )
            for fluid in document.get("fluid_outputs") or []
        ]
        normalized.append(NormRecipe(
            family=row["family"],
            source="cc",
            map_name=document["map"],
            material=row["material"],
            inputs=item_inputs + fluid_inputs,
            outputs=item_outputs + fluid_outputs,
            duration=int(document["duration"]),
            eut=int(document.get("eut") or 0),
            special_value=int(document.get("special_value") or 0),
            chances=[
                int(chance)
                for chance in document.get("output_chances") or []
            ],
            raw_hint=row["path"],
        ))
    return normalized


def title_case_material(material_id: str) -> str:
    return " ".join(part.capitalize() for part in material_id.replace("_", " ").split())


def build_meta_map(materials: dict[str, dict[str, Any]]) -> dict[int, str]:
    """Map GT6 numeric material IDs directly from the normalized source dump."""
    return {
        int(source_id): cc_id
        for source_id, cc_id in
        OREDICT_CROSS_REFERENCE["material_id_to_cc"].items()
        if cc_id in materials
    }


def parse_display_material(
    display: str, known: set[str]
) -> tuple[str, str] | None:
    # Display text is diagnostic only. Authoritative normalization requires a
    # numeric source id plus a mapped prefix item (or an explicit fixed-item map).
    return None


def normalize_gt_item(
    item: dict[str, Any],
    materials: set[str],
    meta_map: dict[int, str],
) -> Resource | None:
    if not item:
        return None
    count = int(item.get("count") or 0)
    if count <= 0:
        return None
    item_id = item.get("item") or ""
    meta = item.get("meta")
    if item_id.endswith("empty_slot"):
        return None

    if item_id in VANILLA_FORM_ITEMS:
        mat, form = VANILLA_FORM_ITEMS[item_id]
        return Resource("item", f"{mat}:{form}", count)

    if item_id in FORM_FROM_ITEM and isinstance(meta, int):
        form = FORM_FROM_ITEM[item_id]
        mat = meta_map.get(meta)
        if mat and (mat in materials or mat in {"coal", "coal_coke"}):
            return Resource("item", f"{mat}:{form}", count)
    # Preserve consuming molds, circuits, insulation and tools without guessing
    # a material mapping. Registry id + metadata is authoritative and stable.
    fixed_id = f"fixed:{item_id}@{meta if meta is not None else 'none'}"
    return Resource("item", fixed_id, count)


def gt_special_value_for_compare(map_name: str, raw_special: int) -> int:
    """Convert GT recipe specialValue into the unit CC uses for the same map."""
    if map_name in KELVIN_SPECIAL_VALUE_MAPS and raw_special > 0:
        return int(float(raw_special) - 273.15)
    return int(raw_special)


def normalize_gt_fluid(fluid: dict[str, Any]) -> Resource | None:
    if not fluid:
        return None
    amount = int(fluid.get("amount") or 0)
    if amount <= 0:
        return None
    fid = fluid.get("fluid") or ""
    fid = FLUID_ALIASES.get(fid, fid)
    return Resource("fluid", fid, amount)


def load_gt_map(name: str) -> dict[str, Any]:
    path = GT_MAPS / f"{name}.json"
    return json.loads(path.read_text(encoding="utf-8"))


def selected_rule_material(
    source_id: str,
    source: dict[str, Any],
    resource: dict[str, Any],
    materials: dict[str, dict[str, Any]],
) -> tuple[str, dict[str, Any], int | None] | None:
    selector = resource.get("material_selector") or "self"
    if selector == "self":
        return source_id, source, None
    metadata = source.get("gt6_metadata") or {}
    if selector.startswith("material:"):
        selected_id = selector.split(":", 1)[1]
        target_units = None
    elif selector.startswith("processing_target:"):
        key = selector.split(":", 1)[1]
        target = (metadata.get("processing_targets") or {}).get(key)
        selected_id = target and target.get("material")
        target_units = target and target.get("cc_units")
    elif selector.startswith("byproduct:"):
        index = int(selector.split(":", 1)[1])
        byproducts = metadata.get("byproducts") or []
        selected_id = byproducts[index].get("material") if index < len(byproducts) else None
        target_units = None
    else:
        raise ValueError(f"Unknown material selector {selector}")
    if not selected_id or selected_id not in materials:
        return None
    return selected_id, materials[selected_id], target_units


def validate_material_overrides(
    rule: dict[str, Any],
    materials: dict[str, dict[str, Any]],
    context: str,
) -> None:
    selected = rule.get("material")
    if selected is not None:
        if not isinstance(selected, str) or not re.fullmatch(r"[a-z0-9_]+", selected):
            raise ValueError(f"{context}: material must use [a-z0-9_]+ syntax")
        if selected not in materials:
            raise ValueError(f"{context}: unknown fixed material {selected!r}")
    overrides = rule.get("material_overrides") or {}
    if not isinstance(overrides, dict):
        raise ValueError(f"{context}: material_overrides must be an object")
    if selected is not None:
        dead_overrides = sorted(set(overrides) - {selected})
        if dead_overrides:
            raise ValueError(
                f"{context}: fixed material {selected!r} excludes material "
                "override(s) that can never be selected: "
                + ", ".join(dead_overrides)
            )
    allowed_fields = {
        "duration",
        "eut",
        "special_value",
        "item_input_counts",
        "item_output_counts",
        "output_chances",
        "fluid_amounts",
    }
    item_inputs = rule.get("item_inputs") or []
    item_outputs = rule.get("item_outputs") or []
    fluid_inputs = rule.get("fluid_inputs") or []
    fluid_outputs = rule.get("fluid_outputs") or []

    def expression(
        material_id: str,
        field: str,
        raw: Any,
        *,
        minimum: int,
        maximum: int,
        resource: dict[str, Any] | None = None,
    ) -> None:
        if not isinstance(raw, str) or not raw.strip():
            raise ValueError(
                f"{context}: material_overrides.{material_id}.{field} "
                "must be a non-empty expression string"
            )
        extra: dict[str, float] = {}
        if resource is not None:
            chosen = selected_rule_material(
                material_id, materials[material_id], resource, materials
            )
            if chosen is not None:
                _, _, target_units = chosen
                prefix = str(resource.get("prefix") or "").split(":")[-1]
                extra = resource_expression_variables(prefix, target_units)
        value = evaluate_rule_expression(raw, materials[material_id], extra)
        if value < minimum or value > maximum:
            raise ValueError(
                f"{context}: material_overrides.{material_id}.{field} "
                f"evaluates to {value}, accepted range is {minimum}..{maximum}"
            )

    def indexed_values(
        material_id: str,
        field: str,
        raw: Any,
        resources: list[dict[str, Any]],
        *,
        minimum: int,
        maximum: int,
    ) -> None:
        if not isinstance(raw, dict):
            raise ValueError(
                f"{context}: material_overrides.{material_id}.{field} "
                "must be an index-to-expression object"
            )
        for index_text, value in raw.items():
            if not isinstance(index_text, str) or not re.fullmatch(
                r"0|[1-9][0-9]*", index_text
            ):
                raise ValueError(
                    f"{context}: {field} key {index_text!r} is not an exact "
                    "zero-based decimal index"
                )
            index = int(index_text)
            if index >= len(resources):
                raise ValueError(
                    f"{context}: {field} index {index} does not target an "
                    f"existing slot (slot count {len(resources)})"
                )
            expression(
                material_id,
                f"{field}.{index_text}",
                value,
                minimum=minimum,
                maximum=maximum,
                resource=resources[index],
            )

    for material_id, override in overrides.items():
        if not isinstance(material_id, str) or not re.fullmatch(
            r"[a-z0-9_]+", material_id
        ):
            raise ValueError(
                f"{context}: material override key {material_id!r} "
                "must use [a-z0-9_]+ syntax"
            )
        if material_id not in materials:
            raise ValueError(
                f"{context}: material override references unknown material "
                f"{material_id!r}"
            )
        if not isinstance(override, dict):
            raise ValueError(
                f"{context}: material override {material_id!r} must be an object"
            )
        unknown = sorted(set(override) - allowed_fields)
        if unknown:
            raise ValueError(
                f"{context}: material override {material_id!r} has unknown "
                f"field(s): {', '.join(unknown)}"
            )
        scalar_bounds = {
            "duration": (1, 2_147_483_647),
            "eut": (0, 2_147_483_647),
            "special_value": (0, 2_147_483_647),
        }
        for field, bounds in scalar_bounds.items():
            if field in override:
                expression(
                    material_id,
                    field,
                    override[field],
                    minimum=bounds[0],
                    maximum=bounds[1],
                )
        if "item_input_counts" in override:
            indexed_values(
                material_id,
                "item_input_counts",
                override["item_input_counts"],
                item_inputs,
                minimum=1,
                maximum=64,
            )
        if "item_output_counts" in override:
            indexed_values(
                material_id,
                "item_output_counts",
                override["item_output_counts"],
                item_outputs,
                minimum=1,
                maximum=64,
            )
        if "output_chances" in override:
            indexed_values(
                material_id,
                "output_chances",
                override["output_chances"],
                item_outputs,
                minimum=0,
                maximum=10000,
            )
        if "fluid_amounts" in override:
            raw_fluid_amounts = override["fluid_amounts"]
            if not isinstance(raw_fluid_amounts, dict):
                raise ValueError(
                    f"{context}: material_overrides.{material_id}.fluid_amounts "
                    "must be a side:index-to-expression object"
                )
            for target, value in raw_fluid_amounts.items():
                match = re.fullmatch(r"(input|output):(0|[1-9][0-9]*)", target)
                if not match:
                    raise ValueError(
                        f"{context}: fluid_amounts key {target!r} must be "
                        "input:<index> or output:<index>"
                    )
                resources = fluid_inputs if match.group(1) == "input" else fluid_outputs
                index = int(match.group(2))
                if index >= len(resources):
                    raise ValueError(
                        f"{context}: fluid_amounts target {target!r} does not "
                        f"exist (slot count {len(resources)})"
                    )
                expression(
                    material_id,
                    f"fluid_amounts.{target}",
                    value,
                    minimum=1,
                    maximum=2_147_483_647,
                    resource=resources[index],
                )


def resource_expression_variables(
    prefix: str, target_units: int | None
) -> dict[str, float]:
    result: dict[str, float] = {}
    if prefix in CC_PREFIX_UNITS:
        result["resource.prefix.units"] = float(CC_PREFIX_UNITS[prefix])
    if target_units is not None:
        result["target.units"] = float(target_units)
    return result


def resolve_material_rule_tag(
    tag: str,
    materials: dict[str, dict[str, Any]],
) -> str:
    namespace, separator, path = tag.partition(":")
    if not separator:
        raise ValueError(f"invalid material-rule item tag: {tag}")
    tag_path = (
        CC_COMPONENT_TAGS
        / namespace
        / "tags"
        / "item"
        / f"{path}.json"
    )
    document = json.loads(tag_path.read_text(encoding="utf-8"))
    values = document.get("values")
    if not isinstance(values, list) or len(values) != 1:
        raise ValueError(
            f"comparator requires one concrete member for item tag {tag}"
        )
    member = str(values[0])
    marker = "#c:plates/"
    if not member.startswith(marker):
        raise ValueError(
            f"unsupported material-rule item tag member: {member}"
        )
    material_tag = member[len(marker):]
    matches = sorted(
        mid
        for mid, material in materials.items()
        if str(material.get("tag_name") or mid) == material_tag
    )
    if len(matches) != 1:
        raise ValueError(
            f"item tag {tag} member {member} resolves to {matches}"
        )
    return f"{matches[0]}:plate"


def expand_cc_recipes(materials: dict[str, dict[str, Any]]) -> list[NormRecipe]:
    recipes: list[NormRecipe] = []

    # Coke oven (concrete)
    recipes.append(
        NormRecipe(
            family="coke_oven",
            source="cc",
            map_name="cruciblecraft:coke_oven",
            material=None,
            inputs=[Resource("item", "coal:gem", 1)],
            outputs=[
                Resource("item", "coal_coke:gem", 1),
                Resource("fluid", "creosote", 500),
            ],
            duration=3600,
            eut=0,
            chances=[10000],
            raw_hint="coke_oven/coal.json",
        )
    )

    # Every parametric process is read from the same declarative rule shape,
    # regardless of whether datagen or the component builder owns the root.
    rule_paths = [
        (owner, path)
        for owner in (CC_GEN, CC_COMPONENT_GEN)
        for path in owner.rglob("*.json")
    ]
    for owner, path in sorted(rule_paths, key=lambda row: row[1].as_posix()):
        rule = json.loads(path.read_text(encoding="utf-8"))
        if rule.get("type") != "cruciblecraft:material_rule":
            continue
        validate_material_overrides(
            rule,
            materials,
            path.relative_to(ROOT).as_posix(),
        )
        if not rule.get("target"):
            continue
        item_inputs = rule.get("item_inputs") or []
        item_outputs = rule.get("item_outputs") or []
        fluid_inputs = rule.get("fluid_inputs") or []
        fluid_outputs = rule.get("fluid_outputs") or []
        if not item_inputs or not item_outputs:
            continue
        if any(
            resource.get("string_components")
            for resource in item_inputs + item_outputs
        ):
            continue
        input_prefix = str(item_inputs[0].get("prefix") or "").split(":")[-1]
        output_prefix = str(item_outputs[0].get("prefix") or "").split(":")[-1]
        family = material_rule_family(rule["target"], input_prefix, output_prefix)
        if family is None:
            continue
        selected = rule.get("material")
        for mid, mat in sorted(materials.items()):
            if selected and selected != mid:
                continue
            forms = material_forms(mat)
            referenced = {
                str(resource.get("prefix") or "").split(":")[-1]
                for resource in item_inputs + item_outputs
                if resource.get("prefix")
                and (resource.get("material_selector") or "self") == "self"
            }
            if not referenced.issubset(forms):
                continue
            override = (rule.get("material_overrides") or {}).get(mid) or {}

            inputs: list[Resource] = []
            for index, resource in enumerate(item_inputs):
                selected_resource = selected_rule_material(mid, mat, resource, materials)
                if selected_resource is None:
                    inputs = []
                    break
                resource_mid, resource_material, target_units = selected_resource
                count_expression = (
                    (override.get("item_input_counts") or {}).get(str(index))
                    or resource.get("count", "1")
                )
                prefix = str(resource.get("prefix") or "").split(":")[-1]
                if prefix and prefix not in material_forms(resource_material):
                    inputs = []
                    break
                resource_id = (
                    f"{resource_mid}:{prefix}"
                    if prefix
                    else resolve_material_rule_tag(
                        str(resource["tag"]), materials
                    )
                    if resource.get("tag")
                    else str(resource["item"])
                )
                try:
                    input_count = evaluate_rule_expression(
                        count_expression,
                        mat,
                        resource_expression_variables(prefix, target_units),
                    )
                except ValueError:
                    if (
                        (resource.get("material_selector") or "").startswith(
                            "processing_target:"
                        )
                        or "target_units(" in str(count_expression)
                    ):
                        inputs = []
                        break
                    raise
                presence_only_fixed_item = (
                    input_count == 0
                    and bool(resource.get("item"))
                    and not resource.get("prefix")
                )
                if (
                    input_count < 0
                    or input_count > 64
                    or input_count == 0
                    and not presence_only_fixed_item
                ):
                    inputs = []
                    break
                inputs.append(Resource("item", resource_id, input_count))
            if len(inputs) != len(item_inputs):
                continue

            outputs: list[Resource] = []
            chances: list[int] = []
            for index, resource in enumerate(item_outputs):
                selected_resource = selected_rule_material(mid, mat, resource, materials)
                if selected_resource is None:
                    if resource.get("optional"):
                        continue
                    outputs = []
                    break
                resource_mid, resource_material, target_units = selected_resource
                count_expression = (
                    (override.get("item_output_counts") or {}).get(str(index))
                    or resource.get("count", "1")
                )
                chance_expression = (
                    (override.get("output_chances") or {}).get(str(index))
                    or resource.get("chance", "10000")
                )
                prefix = str(resource.get("prefix") or "").split(":")[-1]
                if prefix and prefix not in material_forms(resource_material):
                    if resource.get("optional"):
                        continue
                    outputs = []
                    break
                resource_id = (
                    f"{resource_mid}:{prefix}" if prefix else str(resource["item"])
                )
                try:
                    output_count = evaluate_rule_expression(
                        count_expression,
                        mat,
                        resource_expression_variables(prefix, target_units),
                    )
                except ValueError:
                    if (
                        (resource.get("material_selector") or "").startswith(
                            "processing_target:"
                        )
                        or "target_units(" in str(count_expression)
                    ):
                        if resource.get("optional"):
                            continue
                        outputs = []
                        break
                    raise
                if output_count <= 0 or output_count > 64:
                    outputs = []
                    break
                outputs.append(Resource("item", resource_id, output_count))
                chances.append(evaluate_rule_expression(chance_expression, mat))
            if not outputs:
                continue
            for side, resources in (("input", fluid_inputs), ("output", fluid_outputs)):
                for index, resource in enumerate(resources):
                    amount_expression = (
                        (override.get("fluid_amounts") or {}).get(f"{side}:{index}")
                        or resource.get("amount", "1")
                    )
                    fluid_id = resource.get("fluid")
                    if fluid_id:
                        normalized_id = FLUID_ALIASES.get(fluid_id, fluid_id)
                    else:
                        selected_resource = selected_rule_material(
                            mid, mat, resource, materials
                        )
                        if selected_resource is None:
                            inputs = []
                            break
                        resource_mid, resource_material, target_units = selected_resource
                        prefix = str(resource.get("prefix") or "").split(":")[-1]
                        if prefix not in material_forms(resource_material):
                            inputs = []
                            break
                        normalized_id = f"{resource_mid}:{prefix}"
                    destination = inputs if side == "input" else outputs
                    destination.append(Resource(
                        "fluid",
                        normalized_id,
                        evaluate_rule_expression(amount_expression, mat),
                    ))
                if not inputs:
                    break
            if not inputs:
                continue

            duration = evaluate_rule_expression(
                override.get("duration", rule.get("duration", "1")), mat
            )
            eut = evaluate_rule_expression(
                override.get("eut", rule.get("eut", "0")), mat
            )
            special = evaluate_rule_expression(
                override.get("special_value", rule.get("special_value", "0")), mat
            )
            recipes.append(
                NormRecipe(
                    family=family,
                    source="cc",
                    map_name=rule["target"],
                    material=mid,
                    inputs=inputs,
                    outputs=outputs,
                    duration=duration,
                    eut=eut,
                    special_value=special,
                    chances=chances,
                    notes=[f"special_value={special}"] if special else [],
                    raw_hint=path.relative_to(owner).as_posix(),
                )
            )

    recipes.extend(load_ore_chain_concrete_recipes(materials))

    # Alloys from composition
    for mid, mat in materials.items():
        comp = mat.get("composition")
        if (not comp or len(comp) < 2 or "ingot" not in material_forms(mat)
                or any(component not in materials
                       or "ingot" not in material_forms(materials[component])
                       for component in comp)):
            continue
        inputs = [
            Resource("item", f"{comp_mat}:ingot", int(qty))
            for comp_mat, qty in sorted(comp.items())
        ]
        total = sum(int(q) for q in comp.values())
        recipes.append(
            NormRecipe(
                family="alloy",
                source="cc",
                map_name="cruciblecraft:composition",
                material=mid,
                inputs=inputs,
                outputs=[Resource("item", f"{mid}:ingot", total)],
                duration=0,
                eut=0,
                special_value=int((mat.get("thermal") or {}).get("melting_point") or 0),
                fake=True,
                notes=["composition rule"],
                raw_hint=f"materials/{mid}.json",
            )
        )

    # Smelting / blasting coverage (vanilla cooking) — informational family
    for mid, mat in materials.items():
        forms = material_forms(mat)
        if "ingot" not in forms:
            continue
        for ore_form in ("crushed_ore", "raw_ore"):
            if ore_form not in forms:
                continue
            # CC skips raw_ore smelting when form_items overrides raw (vanilla already cooks it)
            form_items = mat.get("form_items") or {}
            if ore_form == "raw_ore" and "raw_ore" in form_items:
                continue
            for process, dur in (("smelting", 200), ("blasting", 100)):
                recipes.append(
                    NormRecipe(
                        family=f"cook_{process}",
                        source="cc",
                        map_name=f"minecraft:{process}",
                        material=mid,
                        inputs=[Resource("item", f"{mid}:{ore_form}", 1)],
                        outputs=[Resource("item", f"{mid}:ingot", 1)],
                        duration=dur,
                        eut=0,
                        chances=[10000],
                        raw_hint=f"{mid}_{ore_form}_{process}",
                    )
                )

    return recipes


@cache
def _load_and_expand_cc_recipes_cached() -> tuple[NormRecipe, ...]:
    return tuple(expand_cc_recipes(cached_cc_materials()))


def load_and_expand_cc_recipes() -> list[NormRecipe]:
    """Return an isolated copy of the standard expanded CC recipe projection."""
    return copy.deepcopy(list(_load_and_expand_cc_recipes_cached()))


def cached_expanded_cc_recipes() -> tuple[NormRecipe, ...]:
    """Return the shared read-only-by-contract standard recipe projection."""
    return _load_and_expand_cc_recipes_cached()


def source_derived_gt_recipes(
    family: str, cc_all: list[NormRecipe]
) -> list[NormRecipe]:
    """Build only references whose generic GT6 behavior is explicit in source."""
    if family == "cook_blasting":
        return []
    if family != "cook_smelting":
        raise ValueError(f"No source-derived GT6 family builder for {family}")
    return [
        NormRecipe(
            family=family,
            source="gt",
            map_name="mc.recipe.furnace",
            material=cc.material,
            inputs=list(cc.inputs),
            outputs=list(cc.outputs),
            duration=16,
            eut=16,
            chances=[10_000],
            notes=[
                (
                    "Source: gt6_code/gregtech6/src/main/java/gregtech/loaders/c/"
                    "Loader_Recipes_Furnace.java:151-165,182-200"
                ),
                (
                    "Execution: gt6_code/gregtech6/src/main/java/gregapi/recipes/"
                    "maps/RecipeMapFurnace.java:50-55,154 (16 ticks, 16 EU/t)"
                ),
            ],
            raw_hint="GT6 source-derived mc.recipe.furnace",
        )
        for cc in cc_all
        if cc.family == family
    ]


def gt_recipes_for_family(
    family: str,
    materials: dict[str, dict[str, Any]],
    meta_map: dict[int, str],
    map_names: list[str] | None = None,
) -> list[NormRecipe]:
    cfg = FAMILIES[family]
    known = set(materials)
    out: list[NormRecipe] = []
    for map_name in map_names or cfg["gt_maps"]:
        data = load_gt_map(map_name)
        for recipe in data.get("recipes") or []:
            if recipe.get("enabled") is False:
                continue
            inputs: list[Resource] = []
            outputs: list[Resource] = []
            unresolved = False
            for item in recipe.get("inputs") or []:
                norm = normalize_gt_item(item, known, meta_map)
                if item and int(item.get("count") or 0) > 0 and norm is None:
                    # Empty-slot sentinels are non-resources; all real fixed
                    # consuming inputs are retained by normalize_gt_item.
                    if (item.get("item") or "").endswith("empty_slot"):
                        continue
                    unresolved = True
                    break
                if norm:
                    inputs.append(norm)
            if unresolved or not inputs:
                continue
            for item in recipe.get("outputs") or []:
                norm = normalize_gt_item(item, known, meta_map)
                if norm:
                    outputs.append(norm)
            for fluid in recipe.get("fluidInputs") or []:
                nf = normalize_gt_fluid(fluid)
                if nf:
                    inputs.append(nf)
            for fluid in recipe.get("fluidOutputs") or []:
                nf = normalize_gt_fluid(fluid)
                if nf:
                    outputs.append(nf)
            if not outputs:
                continue

            # Family-specific filters
            if family == "coke_oven":
                if not any(r.id == "coal:gem" for r in inputs):
                    # keep only coal→coke for direct CC overlap; still count others later
                    if not any(r.id.startswith("coal") for r in inputs):
                        continue
            if family == "crush_raw_to_crushed":
                if not (
                    len(inputs) == 1
                    and inputs[0].id.endswith(":raw_ore")
                    and any(o.id.endswith(":crushed_ore") for o in outputs)
                ):
                    continue
            if family == "form_ingot_to_plate":
                if not (
                    any(i.id.endswith(":ingot") for i in inputs)
                    and any(o.id.endswith(":plate") for o in outputs)
                    and all(":" in i.id for i in inputs)
                ):
                    continue
                # Prefer single-input 1→1 plate
                if len(inputs) != 1 or inputs[0].count != 1:
                    continue
            if family == "form_plate_to_rod":
                if not (
                    len(inputs) == 1
                    and inputs[0].id.endswith(":plate")
                    and any(o.id.endswith(":rod") for o in outputs)
                ):
                    continue
            if family == "form_rod_to_bolt":
                if not (
                    len(inputs) == 1
                    and inputs[0].id.endswith(":rod")
                    and any(o.id.endswith(":bolt") for o in outputs)
                ):
                    continue
            if family == "anvil_raw_to_crushed":
                if not (
                    len(inputs) == 1
                    and inputs[0].id.endswith(":raw_ore")
                    and any(o.id.endswith(":crushed_ore") for o in outputs)
                ):
                    continue
            if family == "alloy":
                if not recipe.get("fake", False):
                    continue
                # Prefer ingot-only recipes for parity with CC composition
                if not all(r.id.endswith(":ingot") for r in inputs + outputs):
                    continue

            chances = list(recipe.get("chances") or [])
            # trim chance padding to real outputs
            item_out_count = sum(
                1
                for o in (recipe.get("outputs") or [])
                if o and int(o.get("count") or 0) > 0
            )
            if chances and item_out_count:
                chances = chances[:item_out_count]

            mat = None
            for r in outputs + inputs:
                if r.kind == "item" and ":" in r.id:
                    candidate = r.id.split(":", 1)[0]
                    if candidate in known:
                        mat = candidate
                        break
            if mat is None:
                continue

            out.append(
                NormRecipe(
                    family=family,
                    source="gt",
                    map_name=map_name,
                    material=mat,
                    inputs=inputs,
                    outputs=outputs,
                    duration=int(recipe.get("duration") or 0),
                    eut=int(recipe.get("euPerTick") or 0),
                    special_value=gt_special_value_for_compare(
                        map_name, int(recipe.get("specialValue") or 0)
                    ),
                    chances=chances,
                    fake=bool(recipe.get("fake", False)),
                    raw_hint=display_hint(recipe),
                )
            )
    return out


def display_hint(recipe: dict[str, Any]) -> str:
    inns = [
        f"{i.get('displayName')}×{i.get('count')}"
        for i in (recipe.get("inputs") or [])
        if i and int(i.get("count") or 0) > 0
    ]
    outs = [
        f"{o.get('displayName')}×{o.get('count')}"
        for o in (recipe.get("outputs") or [])
        if o and int(o.get("count") or 0) > 0
    ]
    return f"{', '.join(inns)} -> {', '.join(outs)}"


def compare_family(
    family: str, cc: list[NormRecipe], gt: list[NormRecipe]
) -> dict[str, Any]:
    gt_by_sig = defaultdict(list)
    for r in gt:
        gt_by_sig[r.signature()].append(r)

    gt_form = defaultdict(list)
    gt_by_material = defaultdict(list)
    fixed_inputs_by_material = defaultdict(set)
    for r in gt:
        sig = r.io_form_signature()
        if sig:
            gt_form[sig].append(r)
        if r.material:
            gt_by_material[r.material].append(r)
            fixed_inputs_by_material[r.material].update(
                resource.key()
                for resource in r.inputs
                if resource.id.startswith("fixed:"))

    rows: list[dict[str, Any]] = []
    buckets: dict[str, list[dict[str, Any]]] = {tier: [] for tier in MATCH_TIERS}
    seen_row_ids: set[str] = set()
    for cc_r in cc:
        exact_candidates = gt_by_sig.get(cc_r.signature(), [])
        form_signature = cc_r.io_form_signature()
        form_candidates = gt_form.get(form_signature, []) if form_signature else []
        semantic_pool = (
            gt_by_material.get(cc_r.material, [])
            if family.startswith("component_") else gt)
        semantic_candidates = semantic_candidates_for(family, cc_r, semantic_pool)

        if exact_candidates:
            tier = "EXACT"
            primary_gt = pick_preferred(family, exact_candidates)
        elif form_candidates:
            tier = "FORM_PATH"
            primary_gt = pick_preferred(family, form_candidates)
        elif semantic_candidates:
            tier = "SEMANTIC"
            primary_gt = pick_preferred(family, semantic_candidates)
        else:
            tier = "NONE"
            primary_gt = None

        row_id = recipe_row_id(cc_r)
        if row_id in seen_row_ids:
            raise ValueError(f"Duplicate comparison row id: {row_id}")
        seen_row_ids.add(row_id)
        candidate_maps = sorted(
            {
                candidate.map_name
                for candidate in exact_candidates + form_candidates + semantic_candidates
            }
        )
        fixed_input_samples = sorted(
            fixed_inputs_by_material.get(cc_r.material, set()))[:12]
        cable_forms = sorted({
            resource.id.rsplit(":", 1)[-1]
            for resource in cc_r.outputs
            if resource.id.rsplit(":", 1)[-1] in CABLE_INSULATION_PLATES
        })
        insulation_evidence = (
            {
                "material": "rubber",
                "plate_counts": {
                    form: CABLE_INSULATION_PLATES[form] for form in cable_forms
                },
                "sources": [
                    "gt6_code/gregtech6/src/main/java/gregtech/loaders/b/"
                    "Loader_OreProcessing.java:183-184",
                    "gt6_code/gregtech6/src/main/java/gregapi/data/OP.java:644-648",
                ],
                "machine_dump_samples": 0,
                "note": (
                    "GT6 registers cable insulation as ANY.Rubber plates in "
                    "ore-processing/source metadata rather than assembler/wiremill map rows."
                ),
            }
            if cable_forms else None)
        row = {
            "row_id": row_id,
            "signature": str(cc_r.signature()),
            "material": cc_r.material,
            "match_tier": tier,
            "cc": summarize_recipe(cc_r),
            "primary_match": diff_pair(cc_r, primary_gt) if primary_gt else None,
            "evidence": {
                "exact_candidate_count": len(exact_candidates),
                "form_path_candidate_count": len(form_candidates),
                "semantic_candidate_count": len(semantic_candidates),
                "candidate_maps": candidate_maps,
                "fixed_input_samples": fixed_input_samples,
                "insulation_evidence": insulation_evidence,
            },
        }
        rows.append(row)
        buckets[tier].append(row)

    bucket_counts = {tier: len(buckets[tier]) for tier in MATCH_TIERS}
    if len(cc) != sum(bucket_counts.values()):
        raise AssertionError(
            f"{family}: cc_count={len(cc)} but bucket total={sum(bucket_counts.values())}"
        )

    gt_only = []
    cc_signatures = {recipe.signature() for recipe in cc}
    for sig, group in gt_by_sig.items():
        if sig in cc_signatures:
            continue
        # Don't flood with every GT ore-chemistry variant: only report if involves CC mats already scoped
        gt_only.append(summarize_recipe(pick_preferred(family, group)))

    # Alloy special: compare composition ratios
    alloy_rows = []
    if family == "alloy":
        for cc_r in cc:
            gt_hits = [
                g
                for g in gt
                if g.outputs
                and g.outputs[0].id == cc_r.outputs[0].id
                and sorted(x.id for x in g.inputs) == sorted(x.id for x in cc_r.inputs)
                and sorted((x.id, x.count) for x in g.inputs)
                == sorted((x.id, x.count) for x in cc_r.inputs)
            ]
            alloy_rows.append(
                {
                    "material": cc_r.material,
                    "cc": summarize_recipe(cc_r),
                    "gt_exact": [summarize_recipe(g) for g in gt_hits[:3]],
                    "ratio_match": bool(gt_hits),
                }
            )

    exact_matches = [
        row["primary_match"] for row in buckets["EXACT"] if row["primary_match"]
    ]
    form_matches = [
        row["primary_match"] for row in buckets["FORM_PATH"] if row["primary_match"]
    ]
    semantic_matches = [
        row["primary_match"] for row in buckets["SEMANTIC"] if row["primary_match"]
    ]
    cc_only = [row["cc"] for row in buckets["NONE"]]

    return {
        "family": family,
        "note": FAMILIES[family]["note"],
        "cc_count": len(cc),
        "gt_normalized_count": len(gt),
        "bucket_counts": bucket_counts,
        "bucket_invariant_holds": len(cc) == sum(bucket_counts.values()),
        "exact_signature_matches": bucket_counts["EXACT"],
        "form_path_matches": bucket_counts["FORM_PATH"],
        "semantic_matches": bucket_counts["SEMANTIC"],
        "none_matches": bucket_counts["NONE"],
        "cc_only_count": len(cc_only),
        "gt_only_count": len(gt_only),
        "rows": rows,
        "buckets": buckets,
        "matches": exact_matches[:50],
        "form_matches": form_matches[:50],
        "semantic": semantic_matches[:50],
        "cc_only": cc_only,
        "gt_only_sample": gt_only,
        "alloy_rows": alloy_rows,
    }


def semantic_candidates_for(
    family: str, cc: NormRecipe, gt_list: list[NormRecipe]
) -> list[NormRecipe]:
    if family == "coke_oven":
        return [
            gt
            for gt in gt_list
            if any(i.id.startswith("coal:") and i.count == 1 for i in gt.inputs)
            and any(o.id.startswith("coal_coke:") and o.count == 1 for o in gt.outputs)
            and any(
                o.kind == "fluid" and o.id == "creosote" and o.count == 500
                for o in gt.outputs
            )
        ]
    if family in {"crush_raw_to_crushed", "anvil_raw_to_crushed"}:
        crushed_key = f"item:{cc.material}:crushed_ore"
        return [
            gt
            for gt in gt_list
            if cc.inputs
            and gt.inputs
            and cc.inputs[0].id == gt.inputs[0].id
            and crushed_key in primary_output_counts(cc)
            and crushed_key in primary_output_counts(gt)
        ]
    if family == "alloy":
        return [
            gt
            for gt in gt_list
            if gt.outputs
            and cc.outputs
            and gt.outputs[0].id == cc.outputs[0].id
            and sorted(resource.id for resource in gt.inputs)
            == sorted(resource.id for resource in cc.inputs)
        ]
    if family.startswith("component_"):
        cc_outputs = {output.id for output in cc.outputs}
        return [
            gt for gt in gt_list
            if gt.material == cc.material
            and cc_outputs.intersection(output.id for output in gt.outputs)
        ]
    return []


def pick_preferred(family: str, recipes: list[NormRecipe]) -> NormRecipe:
    preferred = FAMILIES.get(family, {}).get("preferred_gt")
    if preferred:
        for r in recipes:
            if r.map_name == preferred:
                return r
    # Prefer item-only (no fluids), non-fake, then lower eut, then shorter duration
    return min(
        recipes,
        key=lambda r: (
            any(x.kind == "fluid" for x in r.inputs),
            r.fake,
            r.eut,
            r.duration,
            r.map_name,
            tuple(resource.key() for resource in canonical_resources(r.inputs)),
            tuple(resource.key() for resource in canonical_resources(r.outputs)),
            tuple(r.chances),
        ),
    )


def semantic_match_coke(cc: NormRecipe, gt_list: list[NormRecipe]) -> dict[str, Any] | None:
    """Match coal -> coal_coke + creosote regardless of gem/ingot form aliasing."""
    for gt in gt_list:
        has_coal = any(i.id.startswith("coal:") and i.count == 1 for i in gt.inputs)
        has_coke = any(o.id.startswith("coal_coke:") and o.count == 1 for o in gt.outputs)
        creosote = next((o for o in gt.outputs if o.kind == "fluid" and o.id == "creosote"), None)
        if has_coal and has_coke and creosote and creosote.count == 500:
            # Prefer exact minecraft coal gem input
            if any(i.id == "coal:gem" for i in gt.inputs):
                return diff_pair(cc, gt)
    for gt in gt_list:
        has_coal = any(i.id.startswith("coal:") and i.count == 1 for i in gt.inputs)
        has_coke = any(o.id.startswith("coal_coke:") for o in gt.outputs)
        creosote = next((o for o in gt.outputs if o.kind == "fluid" and o.id == "creosote"), None)
        if has_coal and has_coke and creosote and creosote.count == 500:
            return diff_pair(cc, gt)
    return None


def primary_output_counts(recipe: NormRecipe) -> Counter:
    """Collapse duplicate same-id outputs (GT often emits two identical stacks)."""
    counts: Counter = Counter()
    for o in recipe.outputs:
        counts[f"{o.kind}:{o.id}"] += o.count
    return counts


def diff_pair(cc: NormRecipe, gt: NormRecipe) -> dict[str, Any]:
    cc_inputs = canonical_resources(cc.inputs)
    gt_inputs = canonical_resources(gt.inputs)
    cc_outputs = canonical_resources(cc.outputs)
    gt_outputs = canonical_resources(gt.outputs)
    deltas = {
        "duration": gt.duration - cc.duration,
        "eut": gt.eut - cc.eut,
        "special_value": gt.special_value - cc.special_value,
        "output_counts_equal": [o.key() for o in cc_outputs]
        == [o.key() for o in gt_outputs],
        "input_counts_equal": [i.key() for i in cc_inputs]
        == [i.key() for i in gt_inputs],
    }
    if cc.canonical_output_chances() != gt.canonical_output_chances():
        deltas["output_chances_equal"] = False
    return {
        "signature": str(cc.signature()),
        "material": cc.material or gt.material,
        "cc": summarize_recipe(cc),
        "gt": summarize_recipe(gt),
        "deltas": deltas,
    }


def summarize_recipe(r: NormRecipe) -> dict[str, Any]:
    return {
        "source": r.source,
        "map": r.map_name,
        "material": r.material,
        "inputs": [x.key() for x in r.inputs],
        "outputs": [x.key() for x in r.outputs],
        "duration": r.duration,
        "eut": r.eut,
        "special_value": r.special_value,
        "output_chances": list(r.chances),
        "fake": r.fake,
        "hint": r.raw_hint,
        "notes": r.notes,
    }


def comparison_rows(family_reports: list[dict[str, Any]]) -> list[dict[str, Any]]:
    return [
        row
        for report in family_reports
        for row in report.get("rows", [])
    ]


def deltas_are_exact(deltas: dict[str, Any]) -> bool:
    return (
        deltas.get("duration") == 0
        and deltas.get("eut") == 0
        and deltas.get("special_value") == 0
        and deltas.get("output_counts_equal") is True
        and deltas.get("input_counts_equal") is True
        and deltas.get("output_chances_equal", True) is True
    )


def row_decision_evidence(row: dict[str, Any]) -> dict[str, Any]:
    def recipe(value: dict[str, Any]) -> dict[str, Any]:
        return {
            key: value[key]
            for key in (
                "map", "inputs", "outputs", "duration", "eut",
                "special_value", "output_chances")
        }

    primary = row.get("primary_match")
    if primary:
        return {
            "cc": recipe(primary["cc"]),
            "nearest_gt": recipe(primary["gt"]),
            "deltas": primary["deltas"],
            "fixed_input_samples": row["evidence"].get("fixed_input_samples", []),
            "insulation_evidence": row["evidence"].get("insulation_evidence"),
        }
    family = row["row_id"].split("/", 1)[0]
    material = row["cc"].get("material") or "global"
    source_ids = sorted(
        int(source_id)
        for source_id, cc_id in OREDICT_CROSS_REFERENCE["material_id_to_cc"].items()
        if cc_id == material
    )
    prefixes = sorted({
        resource.split(":", 2)[-1].split("x", 1)[0]
        for resource in row["cc"]["inputs"] + row["cc"]["outputs"]
        if resource.startswith(f"item:{material}:")
    })
    return {
        "cc": recipe(row["cc"]),
        "no_match_query": {
            "material": material,
            "source_ids": source_ids,
            "prefixes": prefixes,
            "gt_maps": list(FAMILIES[family]["gt_maps"]),
            "match_tier": row["match_tier"],
            "candidate_counts": {
                key: row["evidence"][key]
                for key in (
                    "exact_candidate_count",
                    "form_path_candidate_count",
                    "semantic_candidate_count",
                )
            },
            "fixed_input_samples": row["evidence"].get("fixed_input_samples", []),
            "insulation_evidence": row["evidence"].get("insulation_evidence"),
        },
    }


def matched_evidence_reason(row: dict[str, Any]) -> str:
    evidence = row_decision_evidence(row)
    cc = evidence["cc"]
    gt = evidence["nearest_gt"]
    delta = evidence["deltas"]
    catalyst = (
        f"; authoritative fixed consuming inputs={evidence['fixed_input_samples']} "
        "are omitted until non-consumed catalyst/insulation semantics exist"
        if evidence.get("fixed_input_samples") else "")
    insulation = (
        f"; embedded insulation evidence={evidence['insulation_evidence']}"
        if evidence.get("insulation_evidence") else "")
    return (
        f"CC {cc['map']} IO {cc['inputs']} -> {cc['outputs']} uses "
        f"duration={cc['duration']}, eut={cc['eut']}, special={cc['special_value']}, "
        f"chances={cc['output_chances']}; nearest authoritative GT {gt['map']} IO "
        f"{gt['inputs']} -> {gt['outputs']} uses duration={gt['duration']}, "
        f"eut={gt['eut']}, special={gt['special_value']}, "
        f"chances={gt['output_chances']}; observed deltas={delta}{catalyst}{insulation}."
    )


def no_match_evidence_issue(row: dict[str, Any]) -> str:
    evidence = row_decision_evidence(row)
    cc = evidence["cc"]
    query = evidence["no_match_query"]
    catalyst = (
        f"; authoritative fixed consuming inputs={query['fixed_input_samples']} "
        "show a catalyst/insulation mismatch"
        if query.get("fixed_input_samples") else "")
    insulation = (
        f"; embedded insulation evidence={query['insulation_evidence']}"
        if query.get("insulation_evidence") else "")
    return (
        f"CC {cc['map']} material={query['material']} IO {cc['inputs']} -> "
        f"{cc['outputs']} duration={cc['duration']} eut={cc['eut']} "
        f"special={cc['special_value']} chances={cc['output_chances']}; "
        f"authoritative query source_ids={query['source_ids']} "
        f"prefixes={query['prefixes']} maps={query['gt_maps']} returned "
        f"tier={query['match_tier']} counts={query['candidate_counts']}"
        f"{catalyst}{insulation}."
    )


def automated_review_fields(
    row: dict[str, Any],
    method: str,
) -> dict[str, Any]:
    evidence = row_decision_evidence(row)
    return {
        "review_mode": "automated",
        "evidence_source": (
            "gt6_recipe_normalized_reference.json plus deterministic "
            "numeric-id/prefix comparator"
        ),
        "evidence_method": method,
        "evidence_digest": stable_hash(evidence),
        "evidence": evidence,
    }


def expectation_template(family_reports: list[dict[str, Any]]) -> dict[str, Any]:
    """Machine-suggested decisions for human merge — never a committed source of truth.

    `gt6_recipe_expectations.json` is an *input*: tools must not rewrite existing
    entries. Callers write this document to `*_suggested.json`, or append only
    missing keys via `append_missing_expectations`.
    """
    expectations: dict[str, Any] = {}
    for row in comparison_rows(family_reports):
        tier = row["match_tier"]
        primary = row.get("primary_match")
        deltas = primary.get("deltas") if primary else None
        family_name = row["cc"]["map"]
        if row["cc"]["map"] == "minecraft:blasting":
            decision = {
                "verdict": "OUT_OF_SCOPE",
                "reason": (
                    "Minecraft 1.21 blasting has no equivalent in authoritative "
                    "GT6 1.7.10 furnace source."
                ),
                "source": (
                    "gt6_code/gregtech6/src/main/java/gregtech/loaders/c/"
                    "Loader_Recipes_Furnace.java"
                ),
                "match_tier": tier,
                **automated_review_fields(
                    row, "deterministic_source_scope_classification"
                ),
            }
        elif tier == "EXACT" and deltas_are_exact(deltas or {}):
            decision = {
                "verdict": "EXACT",
                "match_tier": tier,
                **automated_review_fields(
                    row, "normalized_exact_signature_and_zero_delta"
                ),
            }
        elif tier == "EXACT":
            decision = {
                "verdict": "INTENTIONAL",
                "reason": matched_evidence_reason(row),
                "match_tier": tier,
                "deltas": deltas,
                **automated_review_fields(
                    row, "normalized_nearest_match_delta_comparison"
                ),
            }
            if family_name == "minecraft:smelting":
                decision["source"] = (
                    "gt6_code/gregtech6/src/main/java/gregapi/recipes/maps/"
                    "RecipeMapFurnace.java:154"
                )
        else:
            decision = {
                "verdict": "TODO_PORT",
                "issue": (
                    matched_evidence_reason(row)
                    if primary else no_match_evidence_issue(row)
                ),
                "match_tier": tier,
                **automated_review_fields(
                    row,
                    (
                        "normalized_nearest_match_gap"
                        if primary
                        else "normalized_no_candidate_query"
                    ),
                ),
            }
        expectations[row["row_id"]] = decision
    return {
        "schema_version": 1,
        "_notes": (
            "SUGGESTED only — not authoritative. Merge reviewed rows into "
            "gt6_recipe_expectations.json by hand (or --append-missing-expectations "
            "for net-new keys). Existing committed entries must never be overwritten "
            "by regeneration. Long-term: classification rules cover the bulk; this "
            "file holds human exceptions / needs_review only."
        ),
        "expectations": dict(sorted(expectations.items())),
    }


def append_missing_expectations(
    existing_document: dict[str, Any],
    suggested_document: dict[str, Any],
) -> tuple[dict[str, Any], list[str]]:
    """Return a copy of existing with only absent keys filled from suggestions.

    Never replaces or mutates an already-present expectation entry.
    """
    existing = dict(existing_document.get("expectations") or {})
    suggested = suggested_document.get("expectations") or {}
    added: list[str] = []
    merged = dict(existing)
    for row_id, decision in suggested.items():
        if row_id in merged:
            continue
        merged[row_id] = decision
        added.append(row_id)
    out = {
        "schema_version": int(
            existing_document.get("schema_version")
            or suggested_document.get("schema_version")
            or 1
        ),
        "_notes": existing_document.get("_notes")
        or (
            "Input policy file. Tools may append missing keys only; never rewrite "
            "existing verdicts. Prefer reviewing gt6_recipe_expectations_suggested.json "
            "and merging by hand."
        ),
        "expectations": dict(sorted(merged.items())),
    }
    # Preserve any extra top-level keys from the committed document.
    for key, value in existing_document.items():
        if key in {"schema_version", "_notes", "expectations"}:
            continue
        out.setdefault(key, value)
    return out, sorted(added)


def validate_expectations(
    family_reports: list[dict[str, Any]],
    document: dict[str, Any],
) -> dict[str, Any]:
    decisions = document.get("expectations") or {}
    rows = {row["row_id"]: row for row in comparison_rows(family_reports)}
    errors: list[str] = []
    counts: Counter[str] = Counter()
    review_counts: Counter[str] = Counter()
    unreviewed_rows: set[str] = set()

    def mark_unreviewed(row_id: str, reason: str) -> None:
        unreviewed_rows.add(row_id)
        errors.append(f"UNREVIEWED {reason}: {row_id}")

    for row_id, row in sorted(rows.items()):
        decision = decisions.get(row_id)
        if decision is None:
            mark_unreviewed(row_id, "comparison row")
            continue
        verdict = decision.get("verdict")
        if verdict not in VERDICTS:
            errors.append(f"{row_id}: invalid verdict {verdict!r}")
            unreviewed_rows.add(row_id)
            continue
        counts[verdict] += 1
        row["expectation"] = decision
        tier = row["match_tier"]
        primary = row.get("primary_match")
        actual_deltas = primary.get("deltas") if primary else None
        try:
            expected_evidence = row_decision_evidence(row)
        except (KeyError, TypeError, ValueError):
            expected_evidence = None
        mode = decision.get("review_mode")
        if mode == "human":
            attribution = str(decision.get("reviewed_by") or "").strip()
            reviewed_at = str(decision.get("reviewed_at") or "").strip()
            source = str(decision.get("evidence_source") or "").strip()
            method = str(decision.get("evidence_method") or "").strip()
            digest = str(decision.get("evidence_digest") or "").strip()
            if not all((attribution, reviewed_at, source, method, digest)):
                mark_unreviewed(
                    row_id,
                    "human decision lacks durable attribution/evidence metadata",
                )
            else:
                review_counts["human_reviewed"] += 1
        elif mode == "automated":
            source = str(decision.get("evidence_source") or "").strip()
            method = str(decision.get("evidence_method") or "").strip()
            digest = decision.get("evidence_digest")
            if (
                not source
                or not method
                or expected_evidence is None
                or decision.get("evidence") != expected_evidence
                or digest != stable_hash(expected_evidence)
            ):
                mark_unreviewed(
                    row_id,
                    "automated decision lacks current deterministic source evidence",
                )
            else:
                review_counts["automated_evidence_reviewed"] += 1
        else:
            mark_unreviewed(row_id, "decision lacks valid review_mode")

        if verdict == "EXACT":
            if tier != "EXACT":
                errors.append(f"{row_id}: EXACT verdict has match tier {tier}")
            elif not deltas_are_exact(actual_deltas or {}):
                errors.append(
                    f"{row_id}: EXACT verdict has non-zero deltas {actual_deltas}"
                )
        elif verdict == "INTENTIONAL":
            reason = str(decision.get("reason") or "").strip()
            normalized_reason = reason.lower()
            if is_placeholder_text(reason):
                mark_unreviewed(row_id, "placeholder INTENTIONAL rationale")
            elif any(
                fragment in normalized_reason
                for fragment in GENERIC_RATIONALE_FRAGMENTS
            ):
                mark_unreviewed(row_id, "generic INTENTIONAL rationale")
            elif not reason:
                errors.append(f"{row_id}: INTENTIONAL requires reason")
            if (
                "cc" not in row
                or decision.get("evidence") != expected_evidence
            ):
                errors.append(f"{row_id}: INTENTIONAL evidence is missing or stale")
            if decision.get("match_tier") != tier:
                errors.append(
                    f"{row_id}: match tier changed from "
                    f"{decision.get('match_tier')} to {tier}"
                )
            if decision.get("deltas") != actual_deltas:
                errors.append(
                    f"{row_id}: intentional deltas changed; expected "
                    f"{decision.get('deltas')}, actual {actual_deltas}"
                )
        elif verdict == "TODO_PORT":
            issue = str(decision.get("issue") or "").strip()
            normalized_issue = re.sub(r"\s+", " ", issue).lower().rstrip(".")
            placeholder = (
                is_placeholder_text(issue)
                or normalized_issue.startswith("untracked:")
                or any(
                    fragment in normalized_issue
                    for fragment in GENERIC_RATIONALE_FRAGMENTS)
            )
            if placeholder:
                mark_unreviewed(row_id, "placeholder TODO_PORT issue")
            elif not issue:
                errors.append(f"{row_id}: TODO_PORT requires issue")
            if (
                "cc" not in row
                or decision.get("evidence") != expected_evidence
            ):
                errors.append(f"{row_id}: TODO_PORT evidence is missing or stale")
            if decision.get("match_tier") != tier:
                errors.append(
                    f"{row_id}: TODO_PORT match tier changed from "
                    f"{decision.get('match_tier')} to {tier}"
                )
        elif verdict == "OUT_OF_SCOPE":
            reason = str(decision.get("reason") or "").strip()
            if not reason:
                errors.append(f"{row_id}: OUT_OF_SCOPE requires reason")
            elif is_placeholder_text(reason):
                mark_unreviewed(row_id, "placeholder OUT_OF_SCOPE rationale")

    for stale_id in sorted(set(decisions) - set(rows)):
        errors.append(f"STALE expectation without comparison row: {stale_id}")

    included = sum(counts[verdict] for verdict in VERDICTS if verdict != "OUT_OF_SCOPE")
    return {
        "valid": not errors,
        "errors": errors,
        "counts": {verdict: counts[verdict] for verdict in VERDICTS},
        "review_counts": {
            "human_reviewed": review_counts["human_reviewed"],
            "automated_evidence_reviewed": review_counts[
                "automated_evidence_reviewed"
            ],
            "unreviewed": len(unreviewed_rows),
        },
        "human_reviewed_count": review_counts["human_reviewed"],
        "automated_evidence_reviewed_count": review_counts[
            "automated_evidence_reviewed"
        ],
        "unreviewed_count": len(unreviewed_rows),
        "included_count": included,
        "total_rows": len(rows),
    }


def coverage_stats(
    materials: dict[str, dict[str, Any]], meta_map: dict[int, str]
) -> dict[str, Any]:
    """Coverage with a hard all-map replay gate for template accounting."""
    index = json.loads(GT_INDEX.read_text(encoding="utf-8"))
    known = set(materials)
    known_metas = {
        meta for meta, material in meta_map.items() if material in known
    }
    required_template_maps = {
        "gt.recipe.mixer",
        "gt.recipe.extruder",
        "gt.recipe.bath",
        "gt.recipe.shredder",
    }
    verified_indexes: dict[str, dict[str, Any]] = {}
    for template_index_path in (ROOT / "tools").glob(
        "gt6_*_templates_index_v*.json"
    ):
        candidate = json.loads(
            template_index_path.read_text(encoding="utf-8")
        )
        map_name = candidate.get("map")
        if (
            map_name in required_template_maps
            and candidate.get("replay_verified") is True
        ):
            verified_indexes[map_name] = candidate
    missing_verified_maps = sorted(
        required_template_maps - set(verified_indexes)
    )
    template_gate_open = not missing_verified_maps
    per_map = []
    total_touch = 0
    total_logical_touch = 0
    form_counter = Counter()
    unresolved_counter: Counter[str] = Counter()
    for entry in index.get("maps") or []:
        name = entry.get("nameInternal") or ""
        count = int(entry.get("recipeCount") or 0)
        if count <= 0 or not name.startswith("gt.recipe."):
            continue
        path = GT_MAPS / f"{name}.json"
        if not path.exists():
            continue
        data = json.loads(path.read_text(encoding="utf-8"))
        touch = 0
        for recipe in data.get("recipes") or []:
            touched_mats = set()
            for side in ("inputs", "outputs"):
                for item in recipe.get(side) or []:
                    norm = normalize_gt_item(item, known, meta_map)
                    if (item and int(item.get("count") or 0) > 0 and norm is None
                            and str(item.get("item") or "").startswith("gregtech:gt.meta.")):
                        unresolved_counter[
                            f"{item.get('item')}@{item.get('meta')}"
                        ] += 1
                    if norm and ":" in norm.id:
                        mat = norm.id.split(":", 1)[0]
                        if mat in known:
                            touched_mats.add(mat)
                            form_counter[norm.id.split(":", 1)[1]] += 1
            if touched_mats:
                touch += 1
        verified_index = verified_indexes.get(name)
        template_rows = (
            verified_index.get("templates") or []
            if verified_index is not None
            else []
        )
        template_touch = 0
        exact_remainder_touch = 0
        if verified_index is not None:
            template_touch = sum(
                bool(known_metas.intersection(row.get("material_ids") or []))
                for row in template_rows
            )
            exact_remainder_touch = sum(
                int(row.get("multiplicity") or 0)
                for row in verified_index.get("exact_remainder") or []
                if known_metas.intersection(row.get("material_ids") or [])
            )
        if template_gate_open and verified_index is not None:
            accounting_touch = template_touch + exact_remainder_touch
            accounting_unit = "verified_template_plus_exact_remainder"
            total_logical_touch += accounting_touch
        else:
            accounting_touch = touch
            accounting_unit = "expanded_recipe"
        if touch or verified_index is not None:
            per_map.append({
                "map": name,
                "recipe_count": count,
                "cc_material_touch": touch,
                "verified_template_count": (
                    len(template_rows) if verified_index is not None else None
                ),
                "cc_template_touch": template_touch or None,
                "cc_exact_remainder_touch": exact_remainder_touch or None,
                "template_replay_verified": (
                    verified_index.get("replay_verified")
                    if verified_index is not None
                    else None
                ),
                "accounting_unit": accounting_unit,
                "accounting_touch": accounting_touch,
            })
            total_touch += touch
    per_map.sort(key=lambda x: -int(x["accounting_touch"]))
    return {
        "accounting_unit": (
            "verified_template_plus_exact_remainder"
            if template_gate_open
            else "expanded_recipe"
        ),
        "template_coverage_gate": {
            "open": template_gate_open,
            "required_maps": sorted(required_template_maps),
            "replay_verified_maps": sorted(verified_indexes),
            "missing_replay_verified_maps": missing_verified_maps,
            "policy": (
                "No template coverage is consumed until all four large maps "
                "have lossless replay-verified indexes."
            ),
        },
        "maps_with_cc_material_touch": len(per_map),
        "total_logical_recipes_touching_cc_materials": (
            total_logical_touch if template_gate_open else None
        ),
        "total_gt_recipes_touching_cc_materials": total_touch,
        "top_maps": per_map[:25],
        "cc_form_mentions_in_gt": form_counter.most_common(),
        "unresolved_authoritative_item_count": sum(unresolved_counter.values()),
        "unresolved_authoritative_item_keys": len(unresolved_counter),
        "unresolved_authoritative_item_sample": unresolved_counter.most_common(50),
    }


def map_overlap_degree(name: str) -> str:
    if name == "gt.recipe.cokeoven":
        return "DIRECT"
    if name == "gt.recipe.crusher":
        return "PARTIAL"
    if name.startswith("gt.recipe.anvil"):
        return "PARTIAL_DIFFERENT_SEMANTICS"
    if name in {
        "gt.recipe.rollingmill",
        "gt.recipe.extruder",
        "gt.recipe.cutter",
        "gt.recipe.lathe",
        "gt.recipe.cruciblealloying",
    }:
        return "SEMANTIC"
    return "NONE"


def roadmap_deferral(name: str, recipe_count: int) -> tuple[str, str]:
    if recipe_count == 0:
        category = "EMPTY_PINNED_MAP"
        scope = (
            "the pinned source map is empty, so there is no deterministic "
            "recipe set to port"
        )
    elif name.startswith("gt.recipe.fuels.") or name in {
        "gt.recipe.fusionreactor",
        "gt.recipe.massfab",
        "gt.recipe.replicator",
        "gt.recipe.lightning",
    }:
        category = "ENERGY_AND_FUEL_SYSTEM"
        scope = (
            "the current audited scope has no matching fuel-value, generator, "
            "fusion, or matter-production runtime"
        )
    elif name in {
        "gt.recipe.bumblelyzer",
        "gt.recipe.bumblequeen",
        "gt.recipe.plantalyzer",
        "gt.recipe.trees",
        "gt.recipe.juicer",
        "gt.recipe.squeezer",
        "gt.recipe.fermenter",
    }:
        category = "BIOLOGICAL_PROCESSING"
        scope = (
            "the current audited scope is material processing and does not "
            "include bee, plant, tree, juice, or fermentation systems"
        )
    elif name in {
        "gt.recipe.catalyticcracking",
        "gt.recipe.cryodistillationtower",
        "gt.recipe.cryomixer",
        "gt.recipe.distillationtower",
        "gt.recipe.distillery",
        "gt.recipe.electrolyzer",
        "gt.recipe.steamcracking",
        "gt.recipe.burnmixer",
        "gt.recipe.coagulator",
        "gt.recipe.crystallisationcrucible",
        "gt.recipe.drying",
        "gt.recipe.freezer",
        "gt.recipe.roaster",
        "gt.recipe.autoclave",
        "gt.recipe.melter",
    }:
        category = "CHEMICAL_OR_THERMAL_PIPELINE"
        scope = (
            "the current audited scope lacks the required chemical, pressure, "
            "phase-separation, or controlled-atmosphere pipeline"
        )
    elif name in {
        "gt.recipe.bedrockorelist",
        "gt.recipe.byproductlist",
        "gt.recipe.other",
        "gt.recipe.scannermolecular",
        "gt.recipe.scannervisuals",
    }:
        category = "REFERENCE_OR_ANALYSIS_MAP"
        scope = (
            "the current audited scope does not expose GT6 list, scanner, or "
            "analysis-map semantics as executable CrucibleCraft recipes"
        )
    elif name in {
        "gt.recipe.boxinator",
        "gt.recipe.canner",
        "gt.recipe.generifier",
        "gt.recipe.printer",
        "gt.recipe.unboxinator",
        "gt.recipe.autocrafting",
    }:
        category = "PACKAGING_OR_AUTOMATION"
        scope = (
            "the current audited scope does not implement packaging, container, "
            "printing, generic conversion, or autocrafting machines"
        )
    else:
        category = "UNIMPLEMENTED_SPECIALIZED_MACHINE"
        scope = (
            "the current audited scope implements the named T0-T3 processing "
            "maps only and has no executable map-specific machine"
        )
    reason = (
        f"DEFERRAL[{category}]: {name or '<unnamed>'} has {recipe_count} pinned "
        f"recipe(s); {scope}."
    )
    return category, reason


def ported_roadmap_fields(name: str) -> tuple[str, str, str]:
    t2_maps = {
        "gt.recipe.bath",
        "gt.recipe.centrifuge",
        "gt.recipe.crusher",
        "gt.recipe.mortar",
        "gt.recipe.shredder",
        "gt.recipe.sifter",
        "gt.recipe.sluice",
        "gt.recipe.smelter",
    }
    t3_maps = {
        "gt.recipe.anvil.bend.big",
        "gt.recipe.anvil.bend.small",
        "gt.recipe.assembler",
        "gt.recipe.cutter",
        "gt.recipe.extruder",
        "gt.recipe.lathe",
        "gt.recipe.press",
        "gt.recipe.rollbender",
        "gt.recipe.rollingmill",
        "gt.recipe.welder",
        "gt.recipe.wiremill",
    }
    if name == "gt.recipe.cokeoven":
        return (
            "T0",
            "CrucibleCraft coke-oven data/runtime",
            "The playable coke oven implements the committed coal-to-coke and "
            "creosote route; this is a bounded subset of the pinned GT6 map.",
        )
    if name in t2_maps:
        return (
            "T2",
            "CrucibleCraft ore-chain data/runtime",
            f"The playable {name.removeprefix('gt.recipe.')} route implements "
            "the committed ore-chain subset through declarative material rules; "
            "unmodeled GT6 recipes remain comparison TODOs.",
        )
    if name in t3_maps:
        return (
            "T3",
            "CrucibleCraft component-machine data/runtime",
            f"The playable {name.removeprefix('gt.recipe.')} machine implements "
            "its committed bounded component-rule subset through the shared KU "
            "runtime; PORTED does not claim full-map recipe parity.",
        )
    raise ValueError(f"No precise PORTED roadmap ownership for {name}")


def out_of_scope_roadmap_reason(name: str, recipe_count: int) -> str:
    if name == "gt.recipe.anvil":
        return (
            f"gt.recipe.anvil has {recipe_count} pinned recipes dominated by "
            "GT6 hand-tool, zero-count-tool, and broad crafting semantics; CC "
            "ports the bounded bend maps separately and does not claim this base "
            "map as a machine implementation."
        )
    if name == "gt.recipe.cruciblealloying":
        return (
            f"gt.recipe.cruciblealloying has {recipe_count} pinned fake/reference "
            "recipes; CC alloying is composition- and thermal-model-driven, so "
            "the comparator audits semantic parity without exposing a direct "
            "GT6-map runtime."
        )
    return (
        f"{name} has {recipe_count} pinned recipes whose semantics are represented "
        "by a different explicit CC mechanic; no direct map implementation is claimed."
    )


def roadmap_template(
    index: dict[str, Any],
    existing_document: dict[str, Any] | None = None,
) -> dict[str, Any]:
    all_maps = sorted(
        (
            {
                "gt_map": entry.get("nameInternal") or "",
                "recipes": int(entry.get("recipeCount") or 0),
            }
            for entry in index.get("maps") or []
        ),
        key=lambda row: (-row["recipes"], row["gt_map"]),
    )
    existing = (existing_document or {}).get("maps") or {}
    entries: dict[str, Any] = {}
    for row in all_maps:
        name = row["gt_map"]
        overlap = map_overlap_degree(name)
        if name in {
            "gt.recipe.cokeoven",
            "gt.recipe.crusher",
            "gt.recipe.bath",
            "gt.recipe.centrifuge",
            "gt.recipe.mortar",
            "gt.recipe.shredder",
            "gt.recipe.sifter",
            "gt.recipe.sluice",
            "gt.recipe.smelter",
            "gt.recipe.anvil.bend.big",
            "gt.recipe.anvil.bend.small",
            "gt.recipe.assembler",
            "gt.recipe.cutter",
            "gt.recipe.extruder",
            "gt.recipe.lathe",
            "gt.recipe.press",
            "gt.recipe.rollbender",
            "gt.recipe.rollingmill",
            "gt.recipe.welder",
            "gt.recipe.wiremill",
        }:
            status = "PORTED"
            phase, owner, reason = ported_roadmap_fields(name)
        elif name == "gt.recipe.mixer":
            status = "PLANNED_PHASE_3"
            reason = (
                f"gt.recipe.mixer has {row['recipes']} pinned recipes spanning "
                "multi-item and fluid chemistry; implementation is assigned to "
                "POST_T3 after the current bounded solid-component machine scope."
            )
            phase = "POST_T3"
            owner = "Unassigned future content owner"
        elif overlap in {"SEMANTIC", "PARTIAL_DIFFERENT_SEMANTICS"}:
            status = "OUT_OF_SCOPE"
            reason = out_of_scope_roadmap_reason(name, row["recipes"])
            phase = "N/A"
            owner = "CrucibleCraft compatibility policy"
        else:
            status = "DEFERRED"
            deferral_category, reason = roadmap_deferral(name, row["recipes"])
            phase = "POST_T3"
            owner = "Unassigned future content owner"
        reviewed = existing.get(name) or {}
        generic_existing = reviewed.get("reason") in {
            "Not assigned to a delivery phase yet.",
            "Pinned reference map is empty; revisit if it becomes active.",
            "A direct or partial CC implementation is regression-covered.",
            "CC currently represents this transformation with another mechanic.",
            "Large recipe family reserved for a later content phase.",
            "A playable configured CC processing block and its declarative "
            "recipe family are regression-covered through the shared runtime.",
        }
        entries[name] = {
            "status": reviewed.get("status", status),
            "reason": reason if generic_existing else reviewed.get("reason", reason),
            "owner": reviewed.get("owner", owner),
            "phase": reviewed.get("phase", phase),
            "reference_recipe_count": row["recipes"],
        }
        if entries[name]["status"] == "DEFERRED":
            entries[name]["deferral_category"] = reviewed.get(
                "deferral_category", deferral_category
            )
    return {
        "schema_version": 1,
        "_notes": "Every active and empty GT map in the pinned index has an explicit planning status.",
        "maps": dict(sorted(entries.items())),
    }


def valid_roadmap_status(status: str) -> bool:
    return status in {
        "PORTED",
        "BOUNDED_SUBSET_PORTED",
        "DEFERRED",
        "OUT_OF_SCOPE",
    } or (
        status.startswith("PLANNED_PHASE_")
        and status.removeprefix("PLANNED_PHASE_").isdigit()
    )


def machine_gap_summary(
    index: dict[str, Any],
    roadmap_document: dict[str, Any],
) -> tuple[list[dict[str, Any]], list[str]]:
    roadmap = roadmap_document.get("maps") or {}
    rows = []
    for entry in index.get("maps") or []:
        name = entry.get("nameInternal") or ""
        count = int(entry.get("recipeCount") or 0)
        plan = roadmap.get(name)
        rows.append(
            {
                "gt_map": name,
                "recipes": count,
                "overlap_degree": map_overlap_degree(name),
                "planning_status": plan.get("status") if plan else None,
                "planning_reason": plan.get("reason") if plan else None,
            }
        )
    rows.sort(key=lambda row: (-row["recipes"], row["gt_map"]))

    errors: list[str] = []
    emitted_names = {row["gt_map"] for row in rows}
    for row in rows:
        name = row["gt_map"]
        plan = roadmap.get(name)
        if plan is None:
            errors.append(f"{name}: missing roadmap entry")
            continue
        status = plan.get("status") or ""
        if not valid_roadmap_status(status):
            errors.append(f"{name}: invalid roadmap status {status!r}")
        if not plan.get("reason"):
            errors.append(f"{name}: roadmap status requires reason")
        reason = str(plan.get("reason") or "")
        if reason in {
            "Not assigned to a delivery phase yet.",
            "Pinned reference map is empty; revisit if it becomes active.",
            "A direct or partial CC implementation is regression-covered.",
        }:
            errors.append(f"{name}: formulaic roadmap rationale is not reviewable")
        if status in {"PORTED", "BOUNDED_SUBSET_PORTED"} or status.startswith(
            "PLANNED_PHASE_"
        ):
            if not plan.get("owner"):
                errors.append(f"{name}: {status} roadmap entry requires owner")
            if not plan.get("phase"):
                errors.append(f"{name}: {status} roadmap entry requires phase")
        if map_overlap_degree(name) != "NONE":
            if not plan.get("owner") or not plan.get("phase"):
                errors.append(
                    f"{name}: overlap-bearing roadmap entry requires owner and phase"
                )
        if status == "DEFERRED":
            category = str(plan.get("deferral_category") or "")
            if not category:
                errors.append(f"{name}: DEFERRED roadmap entry requires category")
            if (
                f"DEFERRAL[{category}]" not in reason
                or str(row["recipes"]) not in reason
                or (
                    "current audited scope" not in reason
                    and category != "EMPTY_PINNED_MAP"
                )
            ):
                errors.append(
                    f"{name}: DEFERRED reason must name category, count, and current scope"
                )
        if plan.get("reference_recipe_count") != row["recipes"]:
            errors.append(
                f"{name}: stale reference_recipe_count "
                f"{plan.get('reference_recipe_count')!r}; expected {row['recipes']}"
            )
    for stale_name in sorted(set(roadmap) - emitted_names):
        errors.append(f"{stale_name}: stale roadmap entry absent from pinned index")
    return rows, errors


def audit_gt_anvil(
    cc_all: list[NormRecipe],
    materials: dict[str, dict[str, Any]],
    meta_map: dict[int, str],
) -> dict[str, Any]:
    """Test the three CC form rules directly against gt.recipe.anvil."""
    known = set(materials)
    data = load_gt_map("gt.recipe.anvil")
    touching = 0
    zero_count_inputs: Counter[str] = Counter()
    touching_durations: Counter[int] = Counter()
    touching_eut: Counter[int] = Counter()
    touching_special_values: Counter[int] = Counter()
    touching_samples: list[str] = []
    loose_target_routes: Counter[str] = Counter()
    loose_target_samples: dict[str, list[str]] = defaultdict(list)
    target_routes = {
        ("ingot", "plate"),
        ("plate", "rod"),
        ("rod", "bolt"),
    }
    for recipe in data.get("recipes") or []:
        touches_cc_material = False
        recipe_zero_count_inputs: list[str] = []
        normalized_inputs: list[Resource] = []
        normalized_outputs: list[Resource] = []
        for side in ("inputs", "outputs"):
            for item in recipe.get(side) or []:
                if not item:
                    continue
                if side == "inputs" and int(item.get("count") or 0) == 0:
                    recipe_zero_count_inputs.append(
                        item.get("displayName") or item.get("item") or "<unknown>"
                    )
                normalized = normalize_gt_item(item, known, meta_map)
                if normalized and normalized.kind == "item" and ":" in normalized.id:
                    if int(item.get("count") or 0) > 0:
                        if side == "inputs":
                            normalized_inputs.append(normalized)
                        else:
                            normalized_outputs.append(normalized)
                    material = normalized.id.split(":", 1)[0]
                    touches_cc_material = touches_cc_material or material in known
        for input_resource in normalized_inputs:
            input_material, input_form = input_resource.id.split(":", 1)
            for output_resource in normalized_outputs:
                output_material, output_form = output_resource.id.split(":", 1)
                route = (input_form, output_form)
                if input_material != output_material or route not in target_routes:
                    continue
                route_name = f"{input_form}_to_{output_form}"
                loose_target_routes[route_name] += 1
                if len(loose_target_samples[route_name]) < 20:
                    loose_target_samples[route_name].append(display_hint(recipe))
        if touches_cc_material:
            touching += 1
            zero_count_inputs.update(recipe_zero_count_inputs)
            touching_durations[int(recipe.get("duration") or 0)] += 1
            touching_eut[int(recipe.get("euPerTick") or 0)] += 1
            touching_special_values[int(recipe.get("specialValue") or 0)] += 1
            if len(touching_samples) < 20:
                touching_samples.append(display_hint(recipe))

    target_families = (
        "form_ingot_to_plate",
        "form_plate_to_rod",
        "form_rod_to_bolt",
    )
    family_reports = []
    for family in target_families:
        cc = [recipe for recipe in cc_all if recipe.family == family]
        direct_gt = gt_recipes_for_family(
            family,
            materials,
            meta_map,
            map_names=["gt.recipe.anvil"],
        )
        family_reports.append(compare_family(family, cc, direct_gt))

    return {
        "map": "gt.recipe.anvil",
        "recipe_count": int(data.get("recipeCount") or len(data.get("recipes") or [])),
        "recipes_touching_cc_materials": touching,
        "target_candidate_count": sum(
            report["gt_normalized_count"] for report in family_reports
        ),
        "exact_matches": sum(
            report["exact_signature_matches"] for report in family_reports
        ),
        "direct_exact_signatures": sorted(
            match["signature"]
            for report in family_reports
            for match in report["matches"]
        ),
        "form_path_matches": sum(
            report["form_path_matches"] for report in family_reports
        ),
        "loose_target_routes": dict(sorted(loose_target_routes.items())),
        "loose_target_samples": dict(sorted(loose_target_samples.items())),
        "families": family_reports,
        "zero_count_inputs": zero_count_inputs.most_common(20),
        "touching_duration_distribution": touching_durations.most_common(),
        "touching_eut_distribution": touching_eut.most_common(),
        "touching_special_value_distribution": touching_special_values.most_common(),
        "touching_samples": touching_samples,
    }


def recipe_fingerprint(recipe: NormRecipe) -> str:
    return (
        f"{recipe.signature()}|map={recipe.map_name}"
        f"|duration={recipe.duration}|eut={recipe.eut}"
        f"|special={recipe.special_value}|chances={tuple(recipe.chances)}"
    )


def hand_authored_recipe_snapshot() -> list[dict[str, Any]]:
    rows: list[dict[str, Any]] = []
    for path in sorted(CC_HAND.rglob("*.json")):
        data = json.loads(path.read_text(encoding="utf-8"))
        relative_path = path.relative_to(CC_HAND).as_posix()
        rows.append(
            {
                "path": relative_path,
                "type": data.get("type"),
                "pattern": data.get("pattern"),
                "key": data.get("key"),
                "ingredient": data.get("ingredient"),
                "result": data.get("result"),
                "sha256": stable_hash(data),
                "fingerprint": f"{relative_path}|sha256={stable_hash(data)}",
            }
        )
    return rows


def material_item_id(
    material_id: str,
    form: str,
    materials: dict[str, dict[str, Any]],
) -> str:
    material = materials.get(material_id) or {}
    override = (material.get("form_items") or {}).get(form)
    return override or f"cruciblecraft:{material_id}_{form}"


def resource_item_id(
    resource: Resource,
    materials: dict[str, dict[str, Any]],
) -> str | None:
    if resource.kind != "item":
        return None
    if resource.id == "coal:gem":
        return "minecraft:coal"
    if resource.id.startswith("coal_coke:"):
        return "cruciblecraft:coal_coke"
    if resource.id.count(":") == 1:
        material, form = resource.id.split(":", 1)
        if material in materials:
            return material_item_id(material, form, materials)
    return resource.id if ":" in resource.id else None


def explicit_ingredient_items(value: Any) -> set[str]:
    if isinstance(value, list):
        return {
            item
            for entry in value
            for item in explicit_ingredient_items(entry)
        }
    if not isinstance(value, dict):
        return set()
    if isinstance(value.get("item"), str):
        return {value["item"]}
    return {
        item
        for nested in value.values()
        for item in explicit_ingredient_items(nested)
    }


def hand_recipe_edges() -> list[dict[str, Any]]:
    edges: list[dict[str, Any]] = []
    for path in sorted(CC_HAND.rglob("*.json")):
        data = json.loads(path.read_text(encoding="utf-8"))
        inputs: set[str] = set()
        if isinstance(data.get("key"), dict):
            for ingredient in data["key"].values():
                inputs.update(explicit_ingredient_items(ingredient))
        inputs.update(explicit_ingredient_items(data.get("ingredients")))
        inputs.update(explicit_ingredient_items(data.get("ingredient")))
        result = data.get("result")
        output = result.get("id") if isinstance(result, dict) else result
        if isinstance(output, str):
            edges.append(
                {
                    "id": f"datapack:{path.relative_to(CC_HAND).as_posix()}",
                    "source": "hand_authored_datapack",
                    "inputs": sorted(inputs),
                    "outputs": [output],
                }
            )
    return edges


def programmatic_process_edges(
    materials: dict[str, dict[str, Any]],
) -> list[dict[str, Any]]:
    edges: list[dict[str, Any]] = [
        {
            "id": "programmatic:steelmaking",
            "source": "SteelmakingProcess",
            "source_path": (
                "src/main/java/com/masson/cruciblecraft/recipe/"
                "SteelmakingProcess.java"
            ),
            "inputs": [
                material_item_id("iron", "ingot", materials),
                material_item_id("carbon", "dust", materials),
            ],
            "outputs": [material_item_id("steel", "ingot", materials)],
            "projection": "3 iron ingots + 1 carbon-ingot-equivalent -> 3 steel ingots",
        }
    ]
    for material_id, material in sorted(materials.items()):
        composition = material.get("composition") or {}
        if not composition:
            continue
        edges.append(
            {
                "id": f"programmatic:alloy/{material_id}",
                "source": "AlloyIndex+CompositionTank",
                "source_path": (
                    "src/main/java/com/masson/cruciblecraft/recipe/AlloyIndex.java"
                ),
                "inputs": sorted(
                    material_item_id(component, "ingot", materials)
                    for component in composition
                ),
                "outputs": [material_item_id(material_id, "ingot", materials)],
                "projection": dict(sorted(composition.items())),
            }
        )
    for material_id, material in sorted(materials.items()):
        if not material.get("molten_fluid"):
            continue
        forms = material_forms(material)
        for form in sorted(forms & {"ingot", "plate", "rod", "bolt"}):
            edges.append(
                {
                    "id": f"programmatic:mold_cast/{material_id}/{form}",
                    "source": "MoldCastingRules+CeramicMoldBlockEntity",
                    "source_path": (
                        "src/main/java/com/masson/cruciblecraft/content/mold/"
                        "MoldCastingRules.java"
                    ),
                    "inputs": [f"process:molten/{material_id}"],
                    "outputs": [material_item_id(material_id, form, materials)],
                    "projection": f"molten {material_id} -> {form}",
                }
            )
    for edge in edges:
        source_path = edge.get("source_path")
        if source_path:
            path = ROOT / source_path
            edge["source_sha256"] = hashlib.sha256(path.read_bytes()).hexdigest()
    return edges


def parse_java_numeric_constant(source: str, name: str) -> int | float:
    match = re.search(
        rf"public\s+static\s+final\s+(?:int|long|double|float)\s+{re.escape(name)}"
        rf"\s*=\s*([0-9][0-9_]*(?:\.[0-9_]+)?)[LlFfDd]?\s*;",
        source,
    )
    if not match:
        raise ValueError(f"Unable to extract Java constant {name}")
    literal = match.group(1).replace("_", "")
    return float(literal) if "." in literal else int(literal)


def energy_constants_snapshot() -> dict[str, Any]:
    declarations = {
        "steam": (
            "src/main/java/com/masson/cruciblecraft/steam/SteamConversion.java",
            ("HU_PER_BATCH", "WATER_PER_BATCH", "STEAM_PER_BATCH", "STEAM_PER_KU"),
        ),
        "air": (
            "src/main/java/com/masson/cruciblecraft/air/AirOutputModel.java",
            ("BELLOWS_AIR_PER_TICK", "BELLOWS_STROKE_TICKS", "MAX_STORED_AIR"),
        ),
        "thermal": (
            "src/main/java/com/masson/cruciblecraft/heat/CrucibleThermalModel.java",
            ("GRAMS_PER_ENERGY", "HOT_BUFFER_TICKS", "PASSIVE_DRIFT_INTERVAL"),
        ),
        "firebox": (
            "src/main/java/com/masson/cruciblecraft/heat/FireboxHeatBuffer.java",
            ("MAX_EQUIVALENT_TICKS",),
        ),
        "steam_engine": (
            (
                "src/main/java/com/masson/cruciblecraft/content/blockentity/"
                "SteamEngineBlockEntity.java"
            ),
            ("STEAM_CAPACITY", "KU_CAPACITY", "OUTPUT_RATE"),
        ),
        "steelmaking": (
            "src/main/java/com/masson/cruciblecraft/recipe/SteelmakingProcess.java",
            ("REACTION_INTERVAL_TICKS", "CARBON_CONSUMED_PER_CYCLE"),
        ),
    }
    groups: dict[str, Any] = {}
    for group, (relative_path, names) in declarations.items():
        path = ROOT / relative_path
        source = path.read_text(encoding="utf-8")
        groups[group] = {
            "source_path": relative_path,
            "source_sha256": hashlib.sha256(source.encode("utf-8")).hexdigest(),
            "constants": {
                name: parse_java_numeric_constant(source, name) for name in names
            },
            "gt6_reference_status": "TODO_PORT",
        }

    fuel_path = (
        ROOT
        / "src/main/java/com/masson/cruciblecraft/heat/FuelDefinition.java"
    )
    fuel_source = fuel_path.read_text(encoding="utf-8")
    fuels: dict[str, Any] = {}
    for constant, fuel_id, energy, ticks in re.findall(
        r"public\s+static\s+final\s+FuelDefinition\s+(\w+)\s*="
        r"\s*new\s+FuelDefinition\(\"([^\"]+)\",\s*([0-9_]+)L?,\s*([0-9_]+)\s*\)",
        fuel_source,
    ):
        fuels[constant] = {
            "id": fuel_id,
            "energy_per_tick": int(energy.replace("_", "")),
            "burn_ticks": int(ticks.replace("_", "")),
        }
    groups["fuels"] = {
        "source_path": fuel_path.relative_to(ROOT).as_posix(),
        "source_sha256": hashlib.sha256(fuel_source.encode("utf-8")).hexdigest(),
        "constants": fuels,
        "gt6_reference_status": "TODO_PORT",
    }
    return {
        "groups": groups,
        "unanchored_gt6_group_count": sum(
            group["gt6_reference_status"] == "TODO_PORT"
            for group in groups.values()
        ),
        "snapshot_sha256": stable_hash(groups),
    }


def validate_process_expectations(
    energy_constants: dict[str, Any],
    document: dict[str, Any],
) -> dict[str, Any]:
    decisions = document.get("groups") or {}
    actual_groups = energy_constants["groups"]
    errors: list[str] = []
    counts: Counter[str] = Counter()
    for name, group in sorted(actual_groups.items()):
        decision = decisions.get(name)
        if decision is None:
            errors.append(f"UNREVIEWED GT6 process group: {name}")
            continue
        verdict = decision.get("verdict")
        if verdict not in VERDICTS:
            errors.append(f"{name}: invalid process verdict {verdict!r}")
            continue
        counts[verdict] += 1
        group["expectation"] = decision
        source_evidence = str(decision.get("source_evidence") or "").strip()
        if not source_evidence or is_placeholder_text(source_evidence):
            errors.append(
                f"{name}: process verdict requires non-placeholder source_evidence"
            )
        expected_evidence = process_decision_evidence(name, group, decision)
        fingerprints = expected_evidence["gt6_source_fingerprints"]
        if not fingerprints or "MISSING" in fingerprints.values():
            errors.append(
                f"{name}: source_evidence does not resolve to deterministic GT6 content"
            )
        if decision.get("review_mode") != "automated":
            errors.append(f"{name}: process verdict requires automated review_mode")
        if is_placeholder_text(decision.get("evidence_method")):
            errors.append(f"{name}: process verdict requires evidence_method")
        if decision.get("evidence") != expected_evidence:
            errors.append(f"{name}: process deterministic evidence is missing or stale")
        if decision.get("evidence_digest") != stable_hash(expected_evidence):
            errors.append(f"{name}: process evidence digest is missing or stale")
        if verdict == "EXACT":
            if decision.get("gt6_constants") != group["constants"]:
                errors.append(
                    f"{name}: CC constants differ from pinned GT6 constants"
                )
        elif verdict == "INTENTIONAL":
            if is_placeholder_text(decision.get("reason")):
                errors.append(
                    f"{name}: INTENTIONAL requires non-placeholder reason"
                )
            if decision.get("cc_constants") != group["constants"]:
                errors.append(f"{name}: intentional CC constants changed")
            if not isinstance(decision.get("gt6_constants"), dict):
                errors.append(f"{name}: INTENTIONAL requires gt6_constants")
        elif verdict == "TODO_PORT" and is_placeholder_text(
            decision.get("issue")
        ):
            errors.append(f"{name}: TODO_PORT requires non-placeholder issue")
        elif verdict == "OUT_OF_SCOPE" and is_placeholder_text(
            decision.get("reason")
        ):
            errors.append(f"{name}: OUT_OF_SCOPE requires non-placeholder reason")
    for stale_name in sorted(set(decisions) - set(actual_groups)):
        errors.append(f"{stale_name}: stale GT6 process expectation")
    return {
        "valid": not errors,
        "errors": errors,
        "counts": {verdict: counts[verdict] for verdict in VERDICTS},
        "total_groups": len(actual_groups),
    }


def gt6_source_fingerprints(source_evidence: str) -> dict[str, str]:
    paths = sorted(set(re.findall(
        r"gt6_code/gregtech6/[A-Za-z0-9_./-]+(?:\.java)?",
        source_evidence,
    )))
    if not paths:
        return {}
    result: dict[str, str] = {}
    for relative in paths:
        path = ROOT / relative
        if path.is_file():
            result[relative] = hashlib.sha256(path.read_bytes()).hexdigest()
        elif path.is_dir():
            rows = [
                (
                    child.relative_to(path).as_posix(),
                    hashlib.sha256(child.read_bytes()).hexdigest(),
                )
                for child in sorted(path.rglob("*.java"))
            ]
            result[relative] = stable_hash(rows)
        else:
            result[relative] = "MISSING"
    return result


def process_decision_evidence(
    name: str,
    group: dict[str, Any],
    decision: dict[str, Any],
) -> dict[str, Any]:
    source_evidence = str(decision.get("source_evidence") or "").strip()
    return {
        "group": name,
        "cc_source_path": group["source_path"],
        "cc_source_sha256": group["source_sha256"],
        "cc_constants": group["constants"],
        "gt6_reference_status": group["gt6_reference_status"],
        "gt6_source_evidence": source_evidence,
        "gt6_source_fingerprints": gt6_source_fingerprints(source_evidence),
        "verdict": decision.get("verdict"),
        "declared_gt6_constants": decision.get("gt6_constants"),
    }


def pin_process_expectation_evidence(
    energy_constants: dict[str, Any],
    document: dict[str, Any],
) -> dict[str, Any]:
    output = json.loads(json.dumps(document))
    decisions = output.get("groups") or {}
    for name, group in sorted(energy_constants["groups"].items()):
        if name not in decisions:
            continue
        decision = decisions[name]
        evidence = process_decision_evidence(name, group, decision)
        decision.update(
            review_mode="automated",
            evidence_method="pinned_cc_constants_and_gt6_source_fingerprints",
            evidence=evidence,
            evidence_digest=stable_hash(evidence),
        )
    return output


def build_reachability(
    materials: dict[str, dict[str, Any]],
    cc_all: list[NormRecipe],
) -> dict[str, Any]:
    edges: list[dict[str, Any]] = []
    for index, recipe in enumerate(cc_all):
        inputs = sorted(
            {
                item_id
                for resource in recipe.inputs
                if (item_id := resource_item_id(resource, materials))
            }
        )
        outputs = sorted(
            {
                item_id
                for resource in recipe.outputs
                if (item_id := resource_item_id(resource, materials))
            }
        )
        if outputs:
            edges.append(
                {
                    "id": f"normalized:{index}:{recipe.family}",
                    "source": recipe.map_name,
                    "inputs": inputs,
                    "outputs": outputs,
                }
            )
    edges.extend(hand_recipe_edges())
    programmatic_edges = programmatic_process_edges(materials)
    edges.extend(programmatic_edges)

    material_items: list[dict[str, Any]] = []
    for material_id, material in sorted(materials.items()):
        for form in sorted(material_forms(material)):
            item_id = material_item_id(material_id, form, materials)
            material_items.append(
                {
                    "material": material_id,
                    "form": form,
                    "item": item_id,
                    "cc_registered": item_id.startswith("cruciblecraft:"),
                }
            )

    producers: dict[str, list[str]] = defaultdict(list)
    consumers: dict[str, list[str]] = defaultdict(list)
    for edge in edges:
        for item in edge["outputs"]:
            producers[item].append(edge["id"])
        for item in edge["inputs"]:
            consumers[item].append(edge["id"])

    worldgen_outputs = {
        row["item"] for row in material_items if row["form"] == "raw_ore"
    }
    external_starts = {
        item
        for edge in edges
        for item in edge["inputs"]
        if item.startswith("minecraft:")
    }
    reachable = set(worldgen_outputs) | external_starts
    changed = True
    while changed:
        changed = False
        for edge in edges:
            if set(edge["inputs"]).issubset(reachable):
                before = len(reachable)
                reachable.update(edge["outputs"])
                changed = changed or len(reachable) != before

    matrix = []
    unobtainable = []
    terminal = []
    for row in material_items:
        item = row["item"]
        has_producer = bool(producers[item]) or item in worldgen_outputs
        has_consumer = bool(consumers[item])
        status = {
            **row,
            "has_producer": has_producer,
            "has_consumer": has_consumer,
            "reachable": item in reachable,
            "producer_sources": sorted(producers[item]),
            "consumer_sources": sorted(consumers[item]),
        }
        matrix.append(status)
        if row["cc_registered"] and not has_producer:
            unobtainable.append(item)
        if row["cc_registered"] and not has_consumer:
            terminal.append(item)

    critical_nodes = {
        "ceramic_crucible": "cruciblecraft:crucible",
        "bronze_ingot": material_item_id("bronze", "ingot", materials),
        "steel_ingot": material_item_id("steel", "ingot", materials),
    }
    critical_reachability = {
        name: {"item": item, "reachable": item in reachable}
        for name, item in critical_nodes.items()
    }
    return {
        "matrix": matrix,
        "unobtainable_items": sorted(unobtainable),
        "terminal_items": sorted(terminal),
        "findings": [
            {
                "severity": "WARN",
                "rule": "REGISTERED_ITEM_WITHOUT_PRODUCER",
                "item": item,
            }
            for item in sorted(unobtainable)
        ]
        + [
            {
                "severity": "INFO",
                "rule": "REGISTERED_ITEM_WITHOUT_CONSUMER",
                "item": item,
            }
            for item in sorted(terminal)
        ],
        "critical_nodes": critical_reachability,
        "programmatic_processes": programmatic_edges,
        "edge_count": len(edges),
        "matrix_sha256": stable_hash(matrix),
    }


def match_fingerprint(match: dict[str, Any]) -> str:
    cc = match["cc"]
    gt = match["gt"]
    return (
        f"{match['signature']}"
        f"|cc(duration={cc['duration']},eut={cc['eut']},"
        f"special={cc['special_value']})"
        f"|gt(map={gt['map']},duration={gt['duration']},eut={gt['eut']},"
        f"special={gt['special_value']})"
    )


def regression_snapshot(
    cc_all: list[NormRecipe],
    family_reports: list[dict[str, Any]],
    anvil_audit: dict[str, Any],
    hand_snapshot: list[dict[str, Any]],
    reachability: dict[str, Any],
    energy_constants: dict[str, Any],
) -> dict[str, Any]:
    by_family = {report["family"]: report for report in family_reports}
    core_families = (
        "coke_oven",
        "crush_raw_to_crushed",
        "form_ingot_to_plate",
        "form_plate_to_rod",
        "form_rod_to_bolt",
        "alloy",
    )
    return {
        "_notes": {
            "gt_anvil_direct_exact_signatures": (
                "Expected to be empty: GT6 gt.recipe.anvil has no direct "
                "ingot→plate, plate→rod, or rod→bolt counterparts. A non-empty "
                "set is a deliberate model change that requires baseline review."
            )
        },
        "cc_recipe_fingerprints": sorted(
            recipe_fingerprint(recipe) for recipe in cc_all
        ),
        "core_exact_match_fingerprints": sorted(
            match_fingerprint(match)
            for family in core_families
            for match in by_family[family]["matches"]
        ),
        "gt_anvil_direct_exact_signatures": sorted(
            anvil_audit.get("direct_exact_signatures", [])
        ),
        "hand_authored_recipe_fingerprints": sorted(
            row["fingerprint"] for row in hand_snapshot
        ),
        "programmatic_process_fingerprints": sorted(
            f"{edge['id']}|sha256={stable_hash(edge)}"
            for edge in reachability["programmatic_processes"]
        ),
        "reachability_fingerprints": sorted(
            (
                f"{row['item']}|producer={row['has_producer']}"
                f"|consumer={row['has_consumer']}|reachable={row['reachable']}"
            )
            for row in reachability["matrix"]
        ),
        "critical_reachability": sorted(
            f"{name}|{row['item']}|reachable={row['reachable']}"
            for name, row in reachability["critical_nodes"].items()
        ),
        "energy_constant_fingerprints": sorted(
            energy_constant_fingerprint(name, group)
            for name, group in energy_constants["groups"].items()
        ),
    }


def energy_constant_fingerprint(name: str, group: dict[str, Any]) -> str:
    """Plaintext constant fingerprint so baseline diffs are reviewable."""
    constants = group.get("constants") or {}
    if name == "fuels":
        parts = [
            f"{fuel_name}.energy_per_tick={fuel['energy_per_tick']}|"
            f"{fuel_name}.burn_ticks={fuel['burn_ticks']}"
            for fuel_name, fuel in sorted(constants.items())
        ]
        return f"{name}|{'|'.join(parts)}"
    parts = [f"{key}={constants[key]}" for key in sorted(constants)]
    return f"{name}|{'|'.join(parts)}"


def regression_diff(
    expected: dict[str, Any],
    actual: dict[str, Any],
) -> dict[str, Any]:
    differences: dict[str, Any] = {}
    keys = (set(expected) | set(actual)) - {"_notes"}
    for key in sorted(keys):
        expected_values = set(expected.get(key, []))
        actual_values = set(actual.get(key, []))
        removed = sorted(expected_values - actual_values)
        added = sorted(actual_values - expected_values)
        if removed or added:
            differences[key] = {
                "removed": removed,
                "added": added,
            }
    if expected.get("_notes") != actual.get("_notes"):
        differences["_notes"] = {
            "expected": expected.get("_notes"),
            "actual": actual.get("_notes"),
        }
    return differences


def recipe_to_reference(recipe: NormRecipe) -> dict[str, Any]:
    return {
        "map": recipe.map_name,
        "material": recipe.material,
        "inputs": [
            {"kind": resource.kind, "id": resource.id, "count": resource.count}
            for resource in recipe.inputs
        ],
        "outputs": [
            {"kind": resource.kind, "id": resource.id, "count": resource.count}
            for resource in recipe.outputs
        ],
        "duration": recipe.duration,
        "eut": recipe.eut,
        "special_value": recipe.special_value,
        "chances": recipe.chances,
        "fake": recipe.fake,
        "hint": recipe.raw_hint,
    }


def recipe_from_reference(family: str, data: dict[str, Any]) -> NormRecipe:
    def resources(key: str) -> list[Resource]:
        return [
            Resource(entry["kind"], entry["id"], int(entry["count"]))
            for entry in data[key]
        ]

    return NormRecipe(
        family=family,
        source="gt",
        map_name=data["map"],
        material=data.get("material"),
        inputs=resources("inputs"),
        outputs=resources("outputs"),
        duration=int(data["duration"]),
        eut=int(data["eut"]),
        special_value=int(data.get("special_value", 0)),
        chances=[int(chance) for chance in data.get("chances", [])],
        fake=bool(data.get("fake", False)),
        raw_hint=data.get("hint", ""),
    )


def normalized_family_payload(
    gt_by_family: dict[str, list[NormRecipe]],
) -> dict[str, list[dict[str, Any]]]:
    return {
        family: sorted(
            (recipe_to_reference(recipe) for recipe in recipes),
            key=stable_json,
        )
        for family, recipes in sorted(gt_by_family.items())
    }


def load_reference_metadata() -> dict[str, Any]:
    if not REFERENCE_METADATA_JSON.is_file():
        raise FileNotFoundError(
            f"Missing GT6 reference metadata: {REFERENCE_METADATA_JSON}"
        )
    metadata = json.loads(REFERENCE_METADATA_JSON.read_text(encoding="utf-8"))
    required = ("gt6_version", "config_digest", "dump_tool_version")
    missing = [key for key in required if not metadata.get(key)]
    if missing:
        raise ValueError(
            f"GT6 reference metadata is missing required fields: {', '.join(missing)}"
        )
    return metadata


def build_reference_fingerprint(
    reference_content: dict[str, Any],
) -> dict[str, Any]:
    metadata = reference_content["metadata"]
    sections = {
        key: reference_content[key]
        for key in (
            "source",
            "metadata",
            "gt_map_count",
            "gt_recipe_count",
            "families",
            "map_inventory",
            "gt_anvil_audit",
            "coverage",
        )
    }
    return {
        "schema_version": 2,
        "gt6_version": metadata["gt6_version"],
        "config_digest": metadata["config_digest"],
        "dump_tool_version": metadata["dump_tool_version"],
        "gt_map_count": int(reference_content["gt_map_count"]),
        "gt_recipe_count": int(reference_content["gt_recipe_count"]),
        "section_sha256": {
            key: stable_hash(value)
            for key, value in sections.items()
        },
        "normalized_reference_sha256": stable_hash(sections),
    }


def fingerprint_from_reference(reference: dict[str, Any]) -> dict[str, Any]:
    required = (
        "source",
        "metadata",
        "gt_map_count",
        "gt_recipe_count",
        "families",
        "map_inventory",
        "gt_anvil_audit",
        "coverage",
    )
    missing = [key for key in required if key not in reference]
    if missing:
        raise ValueError(
            "Normalized GT reference is missing integrity-critical sections: "
            + ", ".join(missing)
        )
    metadata = reference["metadata"]
    metadata_missing = [
        key
        for key in ("gt6_version", "config_digest", "dump_tool_version")
        if not metadata.get(key)
    ]
    if metadata_missing:
        raise ValueError(
            "Normalized GT reference metadata is missing: "
            + ", ".join(metadata_missing)
        )
    return build_reference_fingerprint(reference)


def validate_reference_integrity(reference: dict[str, Any]) -> list[str]:
    try:
        expected = fingerprint_from_reference(reference)
    except (KeyError, TypeError, ValueError) as error:
        return [f"Normalized GT reference integrity schema is invalid: {error}"]
    if reference.get("reference_fingerprint") != expected:
        return [
            "Normalized GT reference content does not match its embedded "
            "fingerprint."
        ]
    return []


def refresh_source_derived_reference(cc_all: list[NormRecipe]) -> None:
    reference = json.loads(REFERENCE_JSON.read_text(encoding="utf-8"))
    for family in ("cook_smelting", "cook_blasting"):
        reference["families"][family] = sorted(
            (
                recipe_to_reference(recipe)
                for recipe in source_derived_gt_recipes(family, cc_all)
            ),
            key=stable_json,
        )
    reference["families"] = dict(sorted(reference["families"].items()))
    reference["reference_fingerprint"] = fingerprint_from_reference(reference)
    REFERENCE_JSON.write_text(
        json.dumps(
            reference,
            ensure_ascii=False,
            separators=(",", ":"),
        ) + "\n",
        encoding="utf-8",
    )


def build_normalized_reference(
    gt_by_family: dict[str, list[NormRecipe]],
    anvil_audit: dict[str, Any],
    coverage: dict[str, Any],
    index: dict[str, Any],
) -> dict[str, Any]:
    metadata = load_reference_metadata()
    families_payload = normalized_family_payload(gt_by_family)
    reference = {
        "source": "gt6_dump/gt6_recipe_dump",
        "metadata": metadata,
        "gt_map_count": int(index["mapCount"]),
        "gt_recipe_count": int(index["recipeCount"]),
        "families": families_payload,
        "map_inventory": [
            {
                "nameInternal": entry.get("nameInternal") or "",
                "recipeCount": int(entry.get("recipeCount") or 0),
            }
            for entry in index.get("maps") or []
        ],
        "gt_anvil_audit": {
            "map": anvil_audit["map"],
            "recipe_count": anvil_audit["recipe_count"],
            "recipes_touching_cc_materials": anvil_audit[
                "recipes_touching_cc_materials"
            ],
            "target_candidate_count": anvil_audit["target_candidate_count"],
            "exact_matches": anvil_audit["exact_matches"],
            "direct_exact_signatures": anvil_audit[
                "direct_exact_signatures"
            ],
            "form_path_matches": anvil_audit["form_path_matches"],
            "loose_target_routes": anvil_audit["loose_target_routes"],
            "zero_count_inputs": anvil_audit["zero_count_inputs"],
            "touching_special_value_distribution": anvil_audit[
                "touching_special_value_distribution"
            ],
        },
        "coverage": {
            "total_gt_recipes_touching_cc_materials": coverage[
                "total_gt_recipes_touching_cc_materials"
            ],
            "unresolved_authoritative_item_count": coverage[
                "unresolved_authoritative_item_count"
            ],
            "unresolved_authoritative_item_keys": coverage[
                "unresolved_authoritative_item_keys"
            ],
            "unresolved_authoritative_item_sample": coverage[
                "unresolved_authoritative_item_sample"
            ],
        },
    }
    reference["reference_fingerprint"] = fingerprint_from_reference(reference)
    return reference


def write_normalized_reference(reference: dict[str, Any]) -> None:
    REFERENCE_JSON.write_text(
        json.dumps(
            reference,
            ensure_ascii=False,
            separators=(",", ":"),
        ) + "\n",
        encoding="utf-8",
    )
    print(f"Wrote {REFERENCE_JSON}")


def validate_compact_expectations(
    document: dict[str, Any],
    expected_count: int,
) -> list[str]:
    errors: list[str] = []
    decisions = document.get("expectations")
    if not isinstance(decisions, dict):
        return ["compact expectations document has no expectations object"]
    if len(decisions) != expected_count:
        errors.append(
            "compact expectation count does not cover current CC recipes: "
            f"{len(decisions)} != {expected_count}"
        )
    for row_id, decision in decisions.items():
        if not isinstance(decision, dict):
            errors.append(f"{row_id}: expectation is not an object")
            continue
        verdict = decision.get("verdict")
        if verdict not in VERDICTS:
            errors.append(f"{row_id}: invalid verdict {verdict!r}")
        if decision.get("match_tier") not in MATCH_TIERS:
            errors.append(f"{row_id}: invalid match tier")
        evidence = decision.get("evidence")
        if (
            evidence is None
            or decision.get("evidence_digest") != stable_hash(evidence)
        ):
            errors.append(f"{row_id}: compact evidence digest is missing or stale")
        mode = decision.get("review_mode")
        if mode == "automated":
            if (
                is_placeholder_text(decision.get("evidence_source"))
                or is_placeholder_text(decision.get("evidence_method"))
            ):
                errors.append(f"{row_id}: automated evidence metadata is incomplete")
        elif mode == "human":
            if (
                not str(decision.get("reviewed_by") or "").strip()
                or not str(decision.get("reviewed_at") or "").strip()
            ):
                errors.append(f"{row_id}: human review attribution is incomplete")
        else:
            errors.append(f"{row_id}: decision lacks valid review_mode")
        rationale = (
            decision.get("reason")
            if verdict in {"INTENTIONAL", "OUT_OF_SCOPE"}
            else decision.get("issue")
        )
        if verdict != "EXACT" and is_placeholder_text(rationale):
            errors.append(f"{row_id}: verdict rationale is missing or placeholder")
        if verdict == "INTENTIONAL" and any(
            fragment in str(rationale or "").lower()
            for fragment in GENERIC_RATIONALE_FRAGMENTS
        ):
            errors.append(f"{row_id}: INTENTIONAL rationale is generic")
    return errors


def compact_check() -> int:
    if not BASELINE_JSON.is_file():
        print(f"Missing regression baseline: {BASELINE_JSON}", file=sys.stderr)
        return 1
    baseline = json.loads(BASELINE_JSON.read_text(encoding="utf-8"))
    expected = baseline.get("snapshot")
    reference_fingerprint = baseline.get("reference_fingerprint")
    if not isinstance(expected, dict) or not isinstance(reference_fingerprint, dict):
        print(
            "Regression baseline lacks compact snapshot/reference metadata.",
            file=sys.stderr,
        )
        return 1

    materials = cached_cc_materials()
    cc_all = list(cached_expanded_cc_recipes())
    hand_snapshot = hand_authored_recipe_snapshot()
    reachability = build_reachability(materials, cc_all)
    energy_constants = energy_constants_snapshot()
    current = {
        "cc_recipe_fingerprints": sorted(
            recipe_fingerprint(recipe) for recipe in cc_all
        ),
        "hand_authored_recipe_fingerprints": sorted(
            row["fingerprint"] for row in hand_snapshot
        ),
        "programmatic_process_fingerprints": sorted(
            f"{edge['id']}|sha256={stable_hash(edge)}"
            for edge in reachability["programmatic_processes"]
        ),
        "reachability_fingerprints": sorted(
            (
                f"{row['item']}|producer={row['has_producer']}"
                f"|consumer={row['has_consumer']}|reachable={row['reachable']}"
            )
            for row in reachability["matrix"]
        ),
        "critical_reachability": sorted(
            f"{name}|{row['item']}|reachable={row['reachable']}"
            for name, row in reachability["critical_nodes"].items()
        ),
        "energy_constant_fingerprints": sorted(
            energy_constant_fingerprint(name, group)
            for name, group in energy_constants["groups"].items()
        ),
    }
    errors: list[str] = []
    expected_current = {key: expected.get(key, []) for key in current}
    differences = regression_diff(expected_current, current)
    if differences:
        errors.append(
            "compact CC regression snapshot changed: "
            + json.dumps(differences, ensure_ascii=False)
        )
    if shadowed_recipe_count(cc_all):
        errors.append("CC contains shadowed same-map input signatures")

    metadata = load_reference_metadata()
    for key in ("gt6_version", "config_digest", "dump_tool_version"):
        if metadata.get(key) != reference_fingerprint.get(key):
            errors.append(f"reference metadata {key} differs from the baseline")
    roadmap = json.loads(ROADMAP_JSON.read_text(encoding="utf-8"))
    maps = roadmap.get("maps") or {}
    if len(maps) != int(reference_fingerprint.get("gt_map_count") or -1):
        errors.append("roadmap map count differs from compact reference metadata")
    if sum(int(row.get("reference_recipe_count") or 0) for row in maps.values()) != int(
        reference_fingerprint.get("gt_recipe_count") or -1
    ):
        errors.append("roadmap recipe count differs from compact reference metadata")

    expectations = json.loads(EXPECTATIONS_JSON.read_text(encoding="utf-8"))
    errors.extend(validate_compact_expectations(expectations, len(cc_all)))
    process_expectations = json.loads(
        PROCESS_EXPECTATIONS_JSON.read_text(encoding="utf-8")
    )
    errors.extend(
        validate_process_expectations(
            energy_constants,
            process_expectations,
        )["errors"]
    )
    manifest = json.loads(
        LOCAL_ARTIFACT_MANIFEST.read_text(encoding="utf-8")
    )
    cache_paths = {
        row.get("path")
        for row in manifest.get("artifacts") or []
        if isinstance(row, dict)
        and isinstance(row.get("bytes"), int)
        and row["bytes"] > 0
        and re.fullmatch(r"[0-9a-f]{64}", str(row.get("sha256") or ""))
        and str(row.get("rebuild_command") or "").strip()
    }
    required_cache_paths = {
        "tools/gt6_recipe_compare_report.json",
        "tools/gt6_recipe_normalized_reference.json",
        "tools/gt6_extruder_templates_v5.json",
    }
    if cache_paths != required_cache_paths:
        errors.append("local artifact manifest is incomplete or malformed")

    if errors:
        print("Compact recipe regression validation failed:", file=sys.stderr)
        print("\n".join(f"- {error}" for error in errors), file=sys.stderr)
        return 1
    print("Compact recipe regression snapshot matches the baseline.")
    print(
        "SKIP: full GT6 recipe replay requires the local normalized cache or "
        "authoritative gt6_dump. Run with --check --full-replay after restoring "
        "or rebuilding the cache."
    )
    return 0


def main() -> int:
    args = set(sys.argv[1:])
    known_args = {
        "--check",
        "--update-baseline",
        "--reference-only",
        "--write-expectations-template",
        "--append-missing-expectations",
        "--write-roadmap-template",
        "--refresh-source-reference",
        "--write-reference",
        "--write-automated-expectations",
        "--write-process-expectations-evidence",
        "--write-report",
        "--full-replay",
    }
    unknown_args = sorted(args - known_args)
    if unknown_args:
        print(f"Unknown arguments: {', '.join(unknown_args)}", file=sys.stderr)
        return 2
    check_baseline = "--check" in args
    update_baseline = "--update-baseline" in args
    reference_only = "--reference-only" in args
    write_expectations = "--write-expectations-template" in args
    append_missing = "--append-missing-expectations" in args
    write_roadmap = "--write-roadmap-template" in args
    refresh_source_reference = "--refresh-source-reference" in args
    write_reference = "--write-reference" in args
    write_automated_expectations = "--write-automated-expectations" in args
    write_process_expectations = (
        "--write-process-expectations-evidence" in args
    )
    write_report = "--write-report" in args
    full_replay = "--full-replay" in args
    if check_baseline and update_baseline:
        print("--check and --update-baseline are mutually exclusive.", file=sys.stderr)
        return 2
    mutation_flags = {
        flag
        for flag in (
            "--update-baseline",
            "--write-expectations-template",
            "--append-missing-expectations",
            "--write-roadmap-template",
            "--refresh-source-reference",
            "--write-reference",
            "--write-automated-expectations",
            "--write-process-expectations-evidence",
            "--write-report",
        )
        if flag in args
    }
    if check_baseline and mutation_flags:
        print(
            "--check is strictly read-only and cannot be combined with mutation "
            f"flags: {', '.join(sorted(mutation_flags))}",
            file=sys.stderr,
        )
        return 2
    if reference_only and write_reference:
        print("--write-reference requires the authoritative raw dump.", file=sys.stderr)
        return 2
    if append_missing and not write_expectations:
        # Append needs a fresh suggestion set from this run's family reports.
        write_expectations = True
    if check_baseline and not full_replay:
        return compact_check()
    if write_reference and not GT_INDEX.is_file():
        print(
            "--write-reference requires gt6_dump/gt6_recipe_dump. Restore the "
            "authoritative dump, then rerun the command.",
            file=sys.stderr,
        )
        return 2
    if reference_only and not REFERENCE_JSON.is_file():
        print(
            f"Full replay cache is missing: {REFERENCE_JSON}. Rebuild it with "
            "python tools/compare_gt6_recipes.py --write-reference (requires "
            "gt6_dump/gt6_recipe_dump), or omit --reference-only when the raw "
            "dump is available.",
            file=sys.stderr,
        )
        return 2
    if not GT_INDEX.is_file() and not REFERENCE_JSON.is_file():
        print(
            "Full recipe replay requires either gt6_dump/gt6_recipe_dump or "
            f"{REFERENCE_JSON}. Restore the dump and run --write-reference; "
            "ordinary CI should use --check without --full-replay.",
            file=sys.stderr,
        )
        return 2

    materials = cached_cc_materials()
    print(f"CC materials: {len(materials)}")
    cc_all = list(cached_expanded_cc_recipes())
    cc_shadowed = shadowed_recipe_count(cc_all)
    print(f"CC expanded/normalized recipes: {len(cc_all)}")
    if refresh_source_reference:
        refresh_source_derived_reference(cc_all)
        print(f"Refreshed source-derived families in {REFERENCE_JSON}")

    meta_map: dict[int, str] = {}
    gt_by_family: dict[str, list[NormRecipe]] = {}
    reference_integrity_errors: list[str] = []
    if GT_INDEX.is_file() and not reference_only:
        meta_map = build_meta_map(materials)
        print(f"GT meta→material entries for CC: {len(meta_map)}")
        meta_map = {
            key: value
            for key, value in meta_map.items()
            if value in materials or value in {"coal", "coal_coke"}
        }
        for family in FAMILIES:
            gt_by_family[family] = (
                source_derived_gt_recipes(family, cc_all)
                if family in {"cook_smelting", "cook_blasting"}
                else gt_recipes_for_family(family, materials, meta_map)
            )
        print("Scanning GT coverage (all maps)...")
        coverage = coverage_stats(materials, meta_map)
        anvil_audit = audit_gt_anvil(cc_all, materials, meta_map)
        index = json.loads(GT_INDEX.read_text(encoding="utf-8"))
        generated_reference = build_normalized_reference(
            gt_by_family, anvil_audit, coverage, index
        )
        reference_fingerprint = generated_reference["reference_fingerprint"]
        if check_baseline and REFERENCE_JSON.is_file():
            committed_reference = json.loads(
                REFERENCE_JSON.read_text(encoding="utf-8")
            )
            if stable_hash(committed_reference) != stable_hash(
                generated_reference
            ):
                reference_integrity_errors.append(
                    "Committed normalized GT reference differs from raw dump "
                    "normalization; rerun with --write-reference after review."
                )
        if write_reference:
            write_normalized_reference(generated_reference)
    else:
        if not REFERENCE_JSON.is_file():
            print(
                "GT6 dump and normalized reference are both missing.",
                file=sys.stderr,
            )
            return 1
        print(f"GT6 dump absent; using {REFERENCE_JSON}")
        reference = json.loads(REFERENCE_JSON.read_text(encoding="utf-8"))
        reference_integrity_errors.extend(validate_reference_integrity(reference))
        reference_fingerprint = (
            fingerprint_from_reference(reference)
            if not reference_integrity_errors
            else reference.get("reference_fingerprint") or {}
        )
        gt_by_family = {
            family: [
                recipe_from_reference(family, recipe)
                for recipe in reference["families"][family]
            ]
            for family in FAMILIES
        }
        coverage = reference["coverage"]
        anvil_audit = reference["gt_anvil_audit"]
        index = {
            "mapCount": reference["gt_map_count"],
            "recipeCount": reference["gt_recipe_count"],
            "maps": reference.get("map_inventory") or [],
        }
    print(
        "Unresolved authoritative GT item records: "
        f"{coverage.get('unresolved_authoritative_item_count', 0)}"
    )

    if write_roadmap:
        existing_roadmap = (
            json.loads(ROADMAP_JSON.read_text(encoding="utf-8"))
            if ROADMAP_JSON.is_file()
            else None
        )
        ROADMAP_JSON.write_text(
            json.dumps(
                roadmap_template(index, existing_roadmap),
                indent=2,
                ensure_ascii=False,
            ),
            encoding="utf-8",
        )
        print(f"Wrote {ROADMAP_JSON}")
    roadmap_document = (
        json.loads(ROADMAP_JSON.read_text(encoding="utf-8"))
        if ROADMAP_JSON.is_file()
        else {"maps": {}}
    )
    gaps, roadmap_errors = machine_gap_summary(index, roadmap_document)

    family_reports = []
    for family in FAMILIES:
        cc = [r for r in cc_all if r.family == family]
        gt = gt_by_family[family]
        report = compare_family(family, cc, gt)
        family_reports.append(report)
        print(
            f"  {family}: cc={report['cc_count']} gt={report['gt_normalized_count']} "
            f"exact={report['exact_signature_matches']} form={report['form_path_matches']} "
            f"semantic={report['semantic_matches']}"
        )

    print(
        "  gt.recipe.anvil: "
        f"touching={anvil_audit['recipes_touching_cc_materials']} "
        f"candidates={anvil_audit['target_candidate_count']} "
        f"exact={anvil_audit['exact_matches']} "
        f"form={anvil_audit['form_path_matches']}"
    )

    hand_snapshot = hand_authored_recipe_snapshot()
    reachability = build_reachability(materials, cc_all)
    energy_constants = energy_constants_snapshot()
    process_expectations_document = (
        json.loads(PROCESS_EXPECTATIONS_JSON.read_text(encoding="utf-8"))
        if PROCESS_EXPECTATIONS_JSON.is_file()
        else {"groups": {}}
    )
    if write_process_expectations:
        process_expectations_document = pin_process_expectation_evidence(
            energy_constants,
            process_expectations_document,
        )
        PROCESS_EXPECTATIONS_JSON.write_text(
            json.dumps(
                process_expectations_document,
                indent=2,
                ensure_ascii=False,
            ) + "\n",
            encoding="utf-8",
        )
        print(f"Wrote process expectation evidence {PROCESS_EXPECTATIONS_JSON}")
    process_expectation_validation = validate_process_expectations(
        energy_constants,
        process_expectations_document,
    )
    snapshot = regression_snapshot(
        cc_all,
        family_reports,
        anvil_audit,
        hand_snapshot,
        reachability,
        energy_constants,
    )

    if write_expectations:
        suggested = expectation_template(family_reports)
        EXPECTATIONS_SUGGESTED_JSON.write_text(
            json.dumps(suggested, indent=2, ensure_ascii=False) + "\n",
            encoding="utf-8",
        )
        print(f"Wrote suggested expectations {EXPECTATIONS_SUGGESTED_JSON}")
        print(
            "Note: gt6_recipe_expectations.json is an input — existing entries "
            "were not modified. Review the suggested file, then merge by hand "
            "or pass --append-missing-expectations to add only absent keys."
        )
        if append_missing:
            existing_document = (
                json.loads(EXPECTATIONS_JSON.read_text(encoding="utf-8"))
                if EXPECTATIONS_JSON.is_file()
                else {"schema_version": 1, "expectations": {}}
            )
            before = dict(existing_document.get("expectations") or {})
            merged, added = append_missing_expectations(existing_document, suggested)
            # Refuse to shrink or alter any pre-existing key.
            for row_id, decision in before.items():
                if merged["expectations"].get(row_id) != decision:
                    print(
                        f"Refusing to write: existing expectation mutated for {row_id}",
                        file=sys.stderr,
                    )
                    return 2
            EXPECTATIONS_JSON.write_text(
                json.dumps(merged, indent=2, ensure_ascii=False) + "\n",
                encoding="utf-8",
            )
            print(
                f"Appended {len(added)} missing expectation(s) into {EXPECTATIONS_JSON}"
            )
            if added:
                sample = ", ".join(added[:8])
                more = "" if len(added) <= 8 else f" (+{len(added) - 8} more)"
                print(f"  added: {sample}{more}")
    if write_automated_expectations:
        automated = expectation_template(family_reports)
        automated["_notes"] = (
            "Deterministic evidence classifications generated explicitly by "
            "--write-automated-expectations. No entry claims human review; each "
            "row pins source, method, evidence, and evidence digest."
        )
        EXPECTATIONS_JSON.write_text(
            json.dumps(automated, indent=2, ensure_ascii=False) + "\n",
            encoding="utf-8",
        )
        print(f"Wrote automated evidence expectations {EXPECTATIONS_JSON}")
    expectations_document = (
        json.loads(EXPECTATIONS_JSON.read_text(encoding="utf-8"))
        if EXPECTATIONS_JSON.is_file()
        else {"expectations": {}}
    )
    expectation_validation = validate_expectations(
        family_reports,
        expectations_document,
    )

    hand_recipes = [row["path"] for row in hand_snapshot]
    gen_recipes = sorted(p.relative_to(CC_GEN).as_posix() for p in CC_GEN.rglob("*.json"))

    report = {
        "summary": {
            "gt_map_count": index.get("mapCount"),
            "gt_recipe_count": index.get("recipeCount"),
            "cc_material_count": len(materials),
            "cc_materials": sorted(materials),
            "cc_normalized_recipe_count": len(cc_all),
            "cc_shadowed_recipe_count": cc_shadowed,
            "meta_map_size": len(meta_map),
            "unresolved_authoritative_item_count": coverage.get(
                "unresolved_authoritative_item_count", 0),
            "reference_fingerprint": reference_fingerprint,
            "reference_integrity_errors": reference_integrity_errors,
            "expectation_validation": expectation_validation,
            "roadmap_validation": {
                "valid": not roadmap_errors,
                "errors": roadmap_errors,
                "total_maps": len(gaps),
            },
            "process_expectation_validation": process_expectation_validation,
            "meta_map_sample": {
                str(k): v
                for k, v in sorted(meta_map.items(), key=lambda kv: kv[1])[:30]
            },
            "normalization_notes": [
                "Items normalize only through fixed-item mappings or authoritative numeric GT material id and prefix mappings; display names are diagnostics only.",
                "Duplicate equal GT input/output slots are merged before count comparison.",
                "GT ore chemistry (e.g. Raw Iron → Crushed Hematite) does not match CC same-material crush.",
                "CC anvil form baselines use rollingmill/extruder/cutter; the separate gt.recipe.anvil audit records whether direct equivalents exist.",
                "GT fake crucible alloying compared to CC composition maps.",
                "Catalysts/empty slots (count=0) stripped; chance arrays trimmed to real outputs.",
                "Unit convention: GT recipe specialValue on cruciblealloying is Kelvin; normalized IR converts it to Celsius with int(K - 273.15) before comparison. CC composition special_value is already Celsius (material thermal.melting_point).",
            ],
            "regression_snapshot": snapshot,
        },
        "families": family_reports,
        "gt_anvil_audit": anvil_audit,
        "coverage": coverage,
        "gt_map_gaps": gaps,
        "reachability": reachability,
        "energy_constants": energy_constants,
        "cc_datapack_inventory": {
            "hand_authored": hand_recipes,
            "hand_authored_snapshot": hand_snapshot,
            "generated": gen_recipes,
        },
    }

    if update_baseline:
        validation_errors = (
            reference_integrity_errors
            + roadmap_errors
            + expectation_validation["errors"]
            + process_expectation_validation["errors"]
        )
        if cc_shadowed:
            validation_errors.append(
                f"CC has {cc_shadowed} shadowed same-map input signatures")
        if validation_errors:
            print(
                "Refusing to update baseline while validation errors exist:",
                file=sys.stderr,
            )
            print(
                json.dumps(validation_errors, indent=2, ensure_ascii=False),
                file=sys.stderr,
            )
            return 1
        BASELINE_JSON.write_text(
            json.dumps(
                {
                    "schema_version": 2,
                    "reference_fingerprint": reference_fingerprint,
                    "snapshot": snapshot,
                },
                indent=2,
                ensure_ascii=False,
            ),
            encoding="utf-8",
        )
        print(f"Wrote {BASELINE_JSON}")
    if check_baseline:
        if not BASELINE_JSON.is_file():
            print(f"Missing regression baseline: {BASELINE_JSON}", file=sys.stderr)
            return 1
        baseline = json.loads(BASELINE_JSON.read_text(encoding="utf-8"))
        if "snapshot" not in baseline or "reference_fingerprint" not in baseline:
            print(
                "Legacy baseline has no reference fingerprint; explicit baseline "
                "review/update is required.",
                file=sys.stderr,
            )
            return 1
        if baseline["reference_fingerprint"] != reference_fingerprint:
            print(
                "GT6 reference fingerprint changed; baseline requires explicit review.",
                file=sys.stderr,
            )
            print(
                json.dumps(
                    {
                        "baseline": baseline["reference_fingerprint"],
                        "current": reference_fingerprint,
                    },
                    indent=2,
                    ensure_ascii=False,
                ),
                file=sys.stderr,
            )
            return 1
        validation_errors = (
            reference_integrity_errors
            + roadmap_errors
            + expectation_validation["errors"]
            + process_expectation_validation["errors"]
        )
        if cc_shadowed:
            validation_errors.append(
                f"CC has {cc_shadowed} shadowed same-map input signatures")
        unreachable_critical = [
            name
            for name, row in reachability["critical_nodes"].items()
            if not row["reachable"]
        ]
        if unreachable_critical:
            validation_errors.append(
                "Critical progression nodes are unreachable: "
                + ", ".join(sorted(unreachable_critical))
            )
        if validation_errors:
            print("Recipe regression policy validation failed.", file=sys.stderr)
            print(
                json.dumps(validation_errors, indent=2, ensure_ascii=False),
                file=sys.stderr,
            )
            return 1
        differences = regression_diff(baseline["snapshot"], snapshot)
        if differences:
            print("GT6 recipe regression snapshot changed.", file=sys.stderr)
            print(
                json.dumps(differences, indent=2, ensure_ascii=False),
                file=sys.stderr,
            )
            return 1
        print("GT6 recipe regression snapshot matches the baseline.")
    if write_report:
        validation_errors = (
            reference_integrity_errors
            + roadmap_errors
            + expectation_validation["errors"]
            + process_expectation_validation["errors"]
        )
        if cc_shadowed:
            validation_errors.append(
                f"CC has {cc_shadowed} shadowed same-map input signatures"
            )
        unreachable_critical = [
            name
            for name, row in reachability["critical_nodes"].items()
            if not row["reachable"]
        ]
        if unreachable_critical:
            validation_errors.append(
                "Critical progression nodes are unreachable: "
                + ", ".join(sorted(unreachable_critical))
            )
        if validation_errors:
            print(
                "Refusing --write-report because validation failed.",
                file=sys.stderr,
            )
            print(
                json.dumps(validation_errors, indent=2, ensure_ascii=False),
                file=sys.stderr,
            )
            return 1
        OUT_JSON.write_text(
            json.dumps(report, indent=2, ensure_ascii=False) + "\n",
            encoding="utf-8",
        )
        print(f"Wrote {OUT_JSON}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
