#!/usr/bin/env python3
"""Validate and deterministically normalize the authoritative GT6 ore dictionary dump."""

from __future__ import annotations

import argparse
import hashlib
import json
import math
import re
import sys
from collections import Counter, defaultdict
from functools import reduce
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Iterable

try:
    from tools import gt6_electrical
    from tools import gt6_l3_materials
    from tools.gt6_mapping import GT6_PREFIX_TO_CC
except ModuleNotFoundError:
    import gt6_electrical
    import gt6_l3_materials
    from gt6_mapping import GT6_PREFIX_TO_CC

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_DUMP = ROOT / "gt6_dump" / "gt6_recipe_dump" / "oredict"
TOOLS = ROOT / "tools"
MATERIAL_DIR = ROOT / "src" / "main" / "resources" / "data" / "cruciblecraft" / "materials"

NORMALIZED_MATERIALS = TOOLS / "gt6_oredict_materials_normalized.json"
NORMALIZED_PREFIXES = TOOLS / "gt6_oredict_prefixes_normalized.json"
NORMALIZED_FLUIDS = TOOLS / "gt6_oredict_fluids_normalized.json"
POLICY = TOOLS / "gt6_material_activation_policy.json"
CROSS_REFERENCE = TOOLS / "gt6_oredict_cross_reference.json"
MANIFEST = TOOLS / "gt6_oredict_import_manifest.json"
ALIASES = TOOLS / "gt6_material_alias_overrides.json"
ACTIVATION_OVERRIDES = TOOLS / "gt6_material_activation_overrides.json"
AUTHORED_BASELINE = TOOLS / "gt6_authored_material_baseline.json"
ELECTRICAL_SOURCE = TOOLS / "gt6_electrical_source.json"
ACCEPTANCE_FORM_CORRECTIONS = (
    TOOLS / "component_rule_sources" / "acceptance_form_corrections.json"
)
REGISTRY_BUDGET = TOOLS / "material_registry_budget.json"
RECIPE_DUMP_ROOT = ROOT / "gt6_dump" / "gt6_recipe_dump"

EXPECTED_COUNTS = {"materialCount": 2214, "prefixCount": 468, "fluidMapCount": 322}
T8_PIPE_FLAGS = {
    "cruciblecraft:generates_tiny_fluid_pipe",
    "cruciblecraft:generates_small_fluid_pipe",
    "cruciblecraft:generates_fluid_pipe",
    "cruciblecraft:generates_large_fluid_pipe",
    "cruciblecraft:generates_huge_fluid_pipe",
    "cruciblecraft:generates_item_pipe",
    "cruciblecraft:generates_large_item_pipe",
    "cruciblecraft:generates_huge_item_pipe",
}
T10_FORM_FLAGS = {
    "gt6:itemgenerator/multiingots",
    "gt6:itemgenerator/hotingots",
}
INDEX_FILES = {
    "materialsFile": "materials.json",
    "prefixesFile": "prefixes.json",
    "fluidMapFile": "fluid_map.json",
}
OPTIONAL_INDEX_FILES = {
    "aliasArraySlotsFile": "alias_array_slots.json",
}
GT6_U_PER_INGOT = 648_648_000
CC_UNITS_PER_INGOT = 144
GT6_U_PER_CC_UNIT = GT6_U_PER_INGOT // CC_UNITS_PER_INGOT
AMBIENT_KELVIN = 293.15

T0_PREFIXES = dict(GT6_PREFIX_TO_CC)
IMPORTED_FORMS = {
    "ore",
    "tiny_crushed_ore",
    "washed_crushed_ore",
    "centrifuged_crushed_ore",
    "tiny_centrifuged_crushed_ore",
    "purified_dust",
    "gem",
    "plate_gem",
    "tiny_dust",
    "long_rod", "screw", "ring", "spring", "small_spring",
    "gear", "small_gear", "rotor", "foil",
    "double_plate", "triple_plate", "quadruple_plate",
    "quintuple_plate", "dense_plate",
    "fine_wire", "wire", "double_wire", "quadruple_wire",
    "octuple_wire", "dodecuple_wire", "hexadecuple_wire",
    "cable", "double_cable", "quadruple_cable",
    "octuple_cable", "dodecuple_cable",
}
T3_COMPONENT_FORMS = IMPORTED_FORMS - {
    "tiny_crushed_ore",
    "washed_crushed_ore",
    "centrifuged_crushed_ore",
    "tiny_centrifuged_crushed_ore",
    "purified_dust",
    "gem",
    "plate_gem",
    "tiny_dust",
}
PREFIX_UNITS = {
    "ingot": 144, "plate": 144, "plate_gem": 144, "rod": 72, "bolt": 18,
    "long_rod": 144, "screw": 16, "ring": 36,
    "spring": 144, "small_spring": 36,
    "gear": 576, "small_gear": 144, "rotor": 612, "foil": 36,
    "double_plate": 288, "triple_plate": 432,
    "quadruple_plate": 576, "quintuple_plate": 720,
    "dense_plate": 1296, "fine_wire": 18, "wire": 72,
    "double_wire": 144, "quadruple_wire": 288,
    "octuple_wire": 576, "dodecuple_wire": 864,
    "hexadecuple_wire": 1152,
    "cable": 72, "double_cable": 144, "quadruple_cable": 288,
    "octuple_cable": 576, "dodecuple_cable": 864,
}
# Mirrors enabled tools/component_rule_sources rules. Fixed-point closure
# intentionally has no cable routes until an obtainable insulation input is modeled.
T3_PRODUCTION_ROUTES = (
    ("working", (("ingot", 1),), "long_rod"),
    ("cutting", (("plate", 1),), "foil"),
    ("cutting", (("gear", 1),), "small_gear"),
    ("working", (("bolt", 1),), "screw"),
    ("bending", (("rod", 1),), "ring"),
    ("working", (("ingot", 1),), "wire"),
    ("working", (("foil", 1),), "fine_wire"),
    ("working", (("wire", 1),), "double_wire"),
    ("working", (("double_wire", 1),), "quadruple_wire"),
    ("working", (("quadruple_wire", 1),), "octuple_wire"),
    ("working", (("octuple_wire", 1), ("quadruple_wire", 1)),
     "dodecuple_wire"),
    ("working", (("octuple_wire", 2),), "hexadecuple_wire"),
    ("bending", (("long_rod", 1),), "spring"),
    ("bending", (("rod", 1),), "small_spring"),
    ("working", (("plate", 4), ("ring", 1)), "rotor"),
    ("working", (("plate", 4),), "gear"),
    ("working", (("plate", 2),), "double_plate"),
    ("working", (("double_plate", 1), ("plate", 1)), "triple_plate"),
    ("working", (("double_plate", 2),), "quadruple_plate"),
    ("working", (("quadruple_plate", 1), ("plate", 1)), "quintuple_plate"),
    ("working", (("triple_plate", 3),), "dense_plate"),
)
T3_CABLE_ROUTES = (
    ("wire", "cable", 1),
    ("double_wire", "double_cable", 1),
    ("quadruple_wire", "quadruple_cable", 2),
    ("octuple_wire", "octuple_cable", 3),
    ("dodecuple_wire", "dodecuple_cable", 4),
)
COMPARATOR_ONLY_PREFIXES: dict[str, str] = {}
GT_PREFIX_ITEMS = {
    "blockIngot": "gregtech:gt.meta.storage.solid",
    "blockGem": "gregtech:gt.meta.storage.solid",
    "ore": "gregtech:gt.meta.ore",
    "oreRaw": "gregtech:gt.meta.oreRaw",
    "crushed": "gregtech:gt.meta.crushed",
    "crushedTiny": "gregtech:gt.meta.crushedTiny",
    "crushedPurified": "gregtech:gt.meta.crushedPurified",
    "crushedCentrifuged": "gregtech:gt.meta.crushedCentrifuged",
    "crushedCentrifugedTiny": "gregtech:gt.meta.crushedCentrifugedTiny",
    "dustPure": "gregtech:gt.meta.dustPure",
    "ingot": "gregtech:gt.meta.ingot",
    "dust": "gregtech:gt.meta.dust",
    "dustTiny": "gregtech:gt.meta.dustTiny",
    "gem": "gregtech:gt.meta.gem",
    "plate": "gregtech:gt.meta.plate",
    "plateGem": "gregtech:gt.meta.plateGem",
    "stick": "gregtech:gt.meta.stick",
    "dustSmall": "gregtech:gt.meta.dustSmall",
    "bolt": "gregtech:gt.meta.bolt",
    "stickLong": "gregtech:gt.meta.stickLong",
    "screw": "gregtech:gt.meta.screw",
    "ring": "gregtech:gt.meta.ring",
    "spring": "gregtech:gt.meta.spring",
    "springSmall": "gregtech:gt.meta.springSmall",
    "gearGt": "gregtech:gt.meta.gearGt",
    "gearGtSmall": "gregtech:gt.meta.gearGtSmall",
    "rotor": "gregtech:gt.meta.rotor",
    "foil": "gregtech:gt.meta.foil",
    "plateDouble": "gregtech:gt.meta.plateDouble",
    "plateTriple": "gregtech:gt.meta.plateTriple",
    "plateQuadruple": "gregtech:gt.meta.plateQuadruple",
    "plateQuintuple": "gregtech:gt.meta.plateQuintuple",
    "plateDense": "gregtech:gt.meta.plateDense",
    "wireFine": "gregtech:gt.meta.wireFine",
    "wireGt01": "gregtech:gt.meta.wireGt01",
    "wireGt02": "gregtech:gt.meta.wireGt02",
    "wireGt04": "gregtech:gt.meta.wireGt04",
    "wireGt08": "gregtech:gt.meta.wireGt08",
    "wireGt12": "gregtech:gt.meta.wireGt12",
    "wireGt16": "gregtech:gt.meta.wireGt16",
    "cableGt01": "gregtech:gt.meta.cableGt01",
    "cableGt02": "gregtech:gt.meta.cableGt02",
    "cableGt04": "gregtech:gt.meta.cableGt04",
    "cableGt08": "gregtech:gt.meta.cableGt08",
    "cableGt12": "gregtech:gt.meta.cableGt12",
    "nugget": "gregtech:gt.meta.nugget",
}
AUTHORED_IDS = {
    "bronze": "Bronze",
    "carbon": "Carbon",
    "ceramic": "Ceramic",
    "clay": "Clay",
    "copper": "Copper",
    "gold": "Gold",
    "iron": "Iron",
    "lead": "Lead",
    "nickel": "Nickel",
    "steel": "Steel",
    "tin": "Tin",
    "zinc": "Zinc",
}
AUTHORED_BASE_FORMS = {
    "bronze": ["ingot", "dust", "plate", "rod", "bolt"],
    "carbon": ["dust", "small_dust", "rod"],
    "ceramic": ["dust", "plate"],
    "clay": ["dust"],
    "copper": ["raw_ore", "crushed_ore", "ingot", "dust", "plate", "rod", "bolt"],
    "gold": ["raw_ore", "crushed_ore", "ingot", "dust", "plate", "rod", "bolt"],
    "iron": [
        "raw_ore",
        "crushed_ore",
        "washed_crushed_ore",
        "centrifuged_crushed_ore",
        "purified_dust",
        "ingot",
        "dust",
        "plate",
        "rod",
        "bolt",
    ],
    "lead": ["raw_ore", "crushed_ore", "ingot", "dust", "plate", "rod", "bolt"],
    "nickel": ["raw_ore", "crushed_ore", "ingot", "dust", "plate", "rod", "bolt"],
    "steel": ["ingot", "dust", "plate", "rod", "bolt"],
    "tin": ["raw_ore", "crushed_ore", "ingot", "dust", "plate", "rod", "bolt"],
    "zinc": ["raw_ore", "crushed_ore", "ingot", "dust", "plate", "rod", "bolt"],
}
EXPLICIT_MATERIAL_FORM_INCLUDES = {
    # Reviewed world-ore exception. Tungsten is not in GT6's COMMON_ORE domain.
    "Tungsten": {"ore"},
}
REQUIRED_MATERIAL_FIELDS = {
    "id": int,
    "nameInternal": str,
    "nameLocal": str,
    "hidden": bool,
    "hasMetallum": bool,
    "meltingPoint": int,
    "boilingPoint": int,
    "mass": int,
    "gramPerCubicCentimeter": float,
    "toolTypes": int,
    "toolQuality": int,
    "toolDurability": int,
    "toolSpeed": float,
    "liquidUnit": int,
    "gasUnit": int,
    "plasmaUnit": int,
    "targets": dict,
    "byProducts": list,
    "reRegistrations": list,
    "toThis": list,
    "tags": list,
}
REQUIRED_PREFIX_FIELDS = {
    "nameInternal": str,
    "nameLocal": str,
    "amount": int,
    "state": int,
    "tags": list,
    "familiarPrefixes": list,
    "byProducts": list,
    "registeredMaterials": list,
    "registeredMaterialCount": int,
}


class ImportError(ValueError):
    pass


class GT6ImportUnits:
    """Python import-boundary mirror of the Java GT6ImportUnits constants."""

    @staticmethod
    def kelvin_to_celsius(kelvin: int | float) -> float:
        value = float(kelvin)
        if not math.isfinite(value) or value < 0:
            raise ImportError(f"invalid Kelvin temperature: {kelvin!r}")
        return value - 273.15

    @staticmethod
    def exact_amount(gt6_u: int) -> dict[str, Any]:
        if not isinstance(gt6_u, int) or gt6_u < 0:
            raise ImportError(f"invalid GT6 U amount: {gt6_u!r}")
        quotient, remainder = divmod(gt6_u, GT6_U_PER_CC_UNIT)
        return {
            "numerator_u": gt6_u,
            "cc_units": quotient if remainder == 0 else None,
            "integral_cc_units": remainder == 0,
        }


def stable_json(value: Any, *, pretty: bool = True) -> str:
    if pretty:
        return json.dumps(value, ensure_ascii=False, sort_keys=True, indent=2) + "\n"
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":")) + "\n"


def stable_hash(value: Any) -> str:
    payload = json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":"))
    return hashlib.sha256(payload.encode("utf-8")).hexdigest()


def strip_t8_pipe_projection(value: Any) -> Any:
    """Remove post-import overlays before structural comparison.

    T8/T10 pipe/form flags and T21+ form_items are CC-authored identity
    bridges, not GT6 source data.  Stripping them ensures the hash
    represents only the GT6-owned portion of the material definition.
    """
    if not isinstance(value, dict):
        return value
    result = dict(value)
    metadata = result.get("gt6_metadata")
    if isinstance(metadata, dict) and "pipe_properties" in metadata:
        metadata = dict(metadata)
        metadata.pop("pipe_properties", None)
        result["gt6_metadata"] = metadata
    if "generation_flags" in result:
        result["generation_flags"] = sorted(
            set(result.get("generation_flags") or [])
            - T8_PIPE_FLAGS
            - T10_FORM_FLAGS
        )
    # form_items are CC-authored identity bridges (e.g. charcoal/gem →
    # minecraft:charcoal).  They are preserved during writes but must
    # not affect the GT6-content hash.
    result.pop("form_items", None)
    return result


def preserve_t8_pipe_projection(
    imported: dict[str, Any],
    current: dict[str, Any],
) -> dict[str, Any]:
    """Keep post-import T8/T10 overlays when rewriting GT6-owned fields."""
    result = dict(imported)
    current_metadata = current.get("gt6_metadata")
    if (
        isinstance(current_metadata, dict)
        and "pipe_properties" in current_metadata
    ):
        metadata = dict(result.get("gt6_metadata") or {})
        metadata["pipe_properties"] = current_metadata["pipe_properties"]
        result["gt6_metadata"] = metadata
    post_import_flags = set(current.get("generation_flags") or []) & (
        T8_PIPE_FLAGS | T10_FORM_FLAGS
    )
    if post_import_flags:
        result["generation_flags"] = sorted(
            set(result.get("generation_flags") or []) | post_import_flags
        )
    # Preserve form_items overrides (T21+).  These are CC-authored identity
    # bridges, not GT6 source data, and must survive import rewrites.
    if "form_items" in current and isinstance(current["form_items"], dict):
        result["form_items"] = dict(current["form_items"])
    return result


def structural_material(value: dict[str, Any]) -> dict[str, Any]:
    result = strip_t8_pipe_projection(value)
    result.pop("gt6_metadata", None)
    return result


def apply_acceptance_form_encoding(
    material: dict[str, Any],
    material_id: str,
    desired_forms: Iterable[str],
    l3_document: dict[str, Any],
) -> dict[str, Any]:
    corrections = json.loads(
        ACCEPTANCE_FORM_CORRECTIONS.read_text(encoding="utf-8")
    ).get("corrections") or []
    corrected_forms = {
        form
        for correction in corrections
        if correction.get("material") == material_id
        for form in correction.get("add_forms") or []
    }
    if not corrected_forms:
        return material
    result = dict(material)
    flags = set(result.get("generation_flags") or [])
    includes = set(result.get("include_prefixes") or [])
    for form in corrected_forms:
        plan = l3_document["prefixes"].get(form)
        if plan is None:
            raise ImportError(
                f"acceptance form correction has no L3 prefix plan: {material_id}/{form}"
            )
        flags.add(plan["generation_flag"])
        includes.discard(form)
    result["generation_flags"] = sorted(flags)
    if includes:
        result["include_prefixes"] = sorted(includes)
    else:
        result.pop("include_prefixes", None)
    if gt6_l3_materials.resolve_material_forms(result, l3_document) != set(
        gt6_l3_materials.close_implied_prefixes(desired_forms)
    ):
        raise ImportError(
            f"acceptance generation-flag encoding changed forms: {material_id}"
        )
    return result


def file_hash(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def load_electrical_source() -> dict[str, Any]:
    try:
        return gt6_electrical.load(ELECTRICAL_SOURCE)
    except gt6_electrical.ElectricalSourceError as error:
        raise ImportError(str(error)) from error


def apply_electrical_source(
        materials: list[dict[str, Any]],
        source_document: dict[str, Any],
) -> None:
    specifications = gt6_electrical.specifications_by_source(source_document)
    available = {value["source_name"] for value in materials}
    missing = sorted(set(specifications) - available)
    if missing:
        raise ImportError(
            "GT6 electrical source references missing material(s): "
            + ", ".join(missing)
        )
    for value in materials:
        dumped = value.get("electrical_by_specification") or {}
        if dumped:
            raise ImportError(
                f"{value['source_name']}: pinned dump unexpectedly contains "
                "electricalBySpecification; review source precedence"
            )
        value["electrical_by_specification"] = specifications.get(
            value["source_name"], {}
        )


def slug(name: str) -> str:
    value = re.sub(r"(?<=[a-z0-9])(?=[A-Z])", "_", name)
    value = re.sub(r"[^a-zA-Z0-9]+", "_", value).strip("_").lower()
    if not value or not re.fullmatch(r"[a-z0-9_]+", value):
        raise ImportError(f"cannot form CrucibleCraft id from GT6 name {name!r}")
    return value


def require_type(record: dict[str, Any], fields: dict[str, type], context: str) -> None:
    for key, expected in fields.items():
        if key not in record or not isinstance(record[key], expected):
            raise ImportError(f"{context}.{key} must be {expected.__name__}")


def load_dump(dump_root: Path) -> tuple[dict[str, Any], list[dict], list[dict], dict]:
    index_path = dump_root / "index.json"
    if not index_path.is_file():
        raise ImportError(f"missing GT6 dump index: {index_path}")
    index = json.loads(index_path.read_text(encoding="utf-8"))
    require_type(index, {**{key: int for key in EXPECTED_COUNTS}, **{key: str for key in INDEX_FILES}},
                 "index")
    for key, expected in EXPECTED_COUNTS.items():
        if index[key] != expected:
            raise ImportError(f"index {key} is {index[key]}, expected authoritative {expected}")
    paths: dict[str, Path] = {}
    for key, expected_name in INDEX_FILES.items():
        raw = index[key].replace("\\", "/")
        if Path(raw).name != expected_name:
            raise ImportError(f"index {key} must identify {expected_name}, got {raw!r}")
        candidate = dump_root / Path(raw).name
        if not candidate.is_file():
            candidate = dump_root.parent / raw
        if not candidate.is_file():
            raise ImportError(f"missing indexed dump file: {raw}")
        paths[key] = candidate
    optional_paths: dict[str, Path] = {}
    for key, expected_name in OPTIONAL_INDEX_FILES.items():
        raw = index.get(key)
        if not raw:
            continue
        raw = str(raw).replace("\\", "/")
        if Path(raw).name != expected_name:
            raise ImportError(f"index {key} must identify {expected_name}, got {raw!r}")
        candidate = dump_root / Path(raw).name
        if not candidate.is_file():
            candidate = dump_root.parent / raw
        if not candidate.is_file():
            raise ImportError(f"missing optional indexed dump file: {raw}")
        optional_paths[key] = candidate
    materials = json.loads(paths["materialsFile"].read_text(encoding="utf-8"))
    prefixes = json.loads(paths["prefixesFile"].read_text(encoding="utf-8"))
    fluids = json.loads(paths["fluidMapFile"].read_text(encoding="utf-8"))
    if not isinstance(materials, list) or len(materials) != index["materialCount"]:
        raise ImportError("materials.json count/type disagrees with index")
    if not isinstance(prefixes, list) or len(prefixes) != index["prefixCount"]:
        raise ImportError("prefixes.json count/type disagrees with index")
    if not isinstance(fluids, dict) or len(fluids) != index["fluidMapCount"]:
        raise ImportError("fluid_map.json count/type disagrees with index")
    index_keys = sorted(EXPECTED_COUNTS | INDEX_FILES.keys())
    if "aliasSlotCount" in index:
        index_keys = sorted(set(index_keys) | {"aliasSlotCount"})
    provenance = {
        "index": {key: index[key] for key in index_keys},
        "sha256": {
            "index.json": file_hash(index_path),
            **{Path(path).name: file_hash(path) for path in paths.values()},
            **{Path(path).name: file_hash(path) for path in optional_paths.values()},
        },
    }
    return provenance, materials, prefixes, fluids


def load_material_texture_sets(recipe_dump_root: Path) -> dict[int, dict[str, str]]:
    path = recipe_dump_root / "textures" / "materials.json"
    if not path.is_file():
        return {}
    rows = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(rows, list):
        raise ImportError("textures/materials.json must be a list")
    by_id: dict[int, dict[str, str]] = {}
    for row in rows:
        if not isinstance(row, dict) or not isinstance(row.get("id"), int):
            continue
        item_set = row.get("textureSetItem")
        block_set = row.get("textureSetBlock")
        if not isinstance(item_set, str) or not isinstance(block_set, str):
            raise ImportError(f"texture row {row.get('nameInternal')} missing texture sets")
        by_id[row["id"]] = {
            "texture_set_item": item_set,
            "texture_set_block": block_set,
        }
    return by_id


def amount(material: str, material_id: int, raw_u: int) -> dict[str, Any]:
    return {"material": material, "material_id": material_id, **GT6ImportUnits.exact_amount(raw_u)}


def normalize_material(raw: dict[str, Any]) -> dict[str, Any]:
    require_type(raw, REQUIRED_MATERIAL_FIELDS, f"material[{raw.get('nameInternal', '?')}]")
    for key in ("meltingPoint", "boilingPoint", "plasmaPoint"):
        if raw[key] < 0:
            raise ImportError(f"{raw['nameInternal']} has negative {key}")
    density = float(raw["gramPerCubicCentimeter"])
    if not math.isfinite(density) or density < 0:
        raise ImportError(f"{raw['nameInternal']} has invalid density")
    thermal_valid = density > 0 and raw["boilingPoint"] > raw["meltingPoint"]
    if (raw["toolDurability"] < 0 or raw["toolQuality"] < 0 or raw["toolTypes"] < 0
            or not math.isfinite(raw["toolSpeed"]) or raw["toolSpeed"] < 0):
        raise ImportError(f"{raw['nameInternal']} has invalid tool bounds")
    components: list[dict[str, Any]] = []
    component_data = raw.get("components") or {}
    for entry in component_data.get("components") or []:
        components.append(amount(entry["material"], entry["materialId"], entry["amount"]))
    targets = {
        key: amount(value["material"], value["materialId"], value["amount"])
        for key, value in sorted(raw["targets"].items())
    }
    aliases = sorted(set(raw["reRegistrations"]) | set(raw["toThis"]))
    generation_tags = sorted(tag for tag in raw["tags"] if tag.startswith("ITEMGENERATOR."))
    property_tags = sorted(tag for tag in raw["tags"] if not tag.startswith("ITEMGENERATOR."))
    # Ambient phase is thermodynamic, not inferred from registered fluid handles.
    # GT sentinel/missing boundaries (0 or unordered) fall back to solid so they
    # remain normalized but cannot pass runtime thermal activation.
    if thermal_valid:
        if AMBIENT_KELVIN < raw["meltingPoint"]:
            state = "solid"
        elif AMBIENT_KELVIN < raw["boilingPoint"]:
            state = "liquid"
        else:
            state = "gas"
        phase_fallback = None
    else:
        state = "solid"
        phase_fallback = "solid_for_missing_or_invalid_boundaries"
    thermal = {
        "melting_point_kelvin": raw["meltingPoint"],
        "melting_point_celsius": GT6ImportUnits.kelvin_to_celsius(raw["meltingPoint"]),
        "boiling_point_kelvin": raw["boilingPoint"],
        "boiling_point_celsius": GT6ImportUnits.kelvin_to_celsius(raw["boilingPoint"]),
        "plasma_point_kelvin": raw["plasmaPoint"],
        "plasma_point_celsius": GT6ImportUnits.kelvin_to_celsius(raw["plasmaPoint"]),
        "density": density,
    }
    return {
        "source_id": raw["id"],
        "source_name": raw["nameInternal"],
        "display_name": raw["nameLocal"],
        "aliases": aliases,
        "hidden": raw["hidden"],
        "has_metallum": raw["hasMetallum"],
        "original_mod": raw.get("originalMod"),
        "formula": raw.get("tooltipChemical"),
        "state": state,
        "state_basis": {
            "ambient_kelvin": AMBIENT_KELVIN,
            "fallback": phase_fallback,
        },
        "color_rgba": raw["rgbaSolid"],
        "thermal": thermal,
        "thermal_valid": thermal_valid,
        "mass": raw["mass"],
        "tool": {
            "durability": raw["toolDurability"],
            "speed": raw["toolSpeed"],
            "quality": raw["toolQuality"],
            "types": raw["toolTypes"],
        },
        "components": components,
        "component_common_divider": component_data.get("commonDivider"),
        "byproducts": list(raw["byProducts"]),
        "processing_targets": targets,
        "material_tags": property_tags,
        "generation_tags": generation_tags,
        "heat_damage": raw["heatDamage"],
        "explosion_damage": float(raw.get("explosionDamage", 0.0)),
        "blast_furnace_temperature_kelvin": raw.get("blastFurnaceTemperature"),
        "blast_furnace_temperature_celsius": (
            GT6ImportUnits.kelvin_to_celsius(raw["blastFurnaceTemperature"])
            if raw.get("blastFurnaceTemperature") is not None else None
        ),
        "electrical_by_specification": raw.get("electricalBySpecification") or {},
        "units": {
            key: GT6ImportUnits.exact_amount(raw[key])
            for key in ("liquidUnit", "gasUnit", "plasmaUnit")
        },
        "fluid": raw.get("liquid") or raw.get("gas") or raw.get("plasma"),
        "target_registration": raw["targetRegistration"],
        "target_reversing": raw["targetReversing"],
    }


def normalize_prefix(raw: dict[str, Any]) -> dict[str, Any]:
    require_type(raw, REQUIRED_PREFIX_FIELDS, f"prefix[{raw.get('nameInternal', '?')}]")
    if raw["registeredMaterialCount"] != len(raw["registeredMaterials"]):
        raise ImportError(f"prefix {raw['nameInternal']} registered material count mismatch")
    amount_data = (
        {"numerator_u": raw["amount"], "cc_units": None, "integral_cc_units": False}
        if raw["amount"] < 0 else GT6ImportUnits.exact_amount(raw["amount"])
    )
    byproducts = [
        amount(value["material"], value["materialId"], value["amount"])
        for value in raw["byProducts"]
    ]
    return {
        "source_name": raw["nameInternal"],
        "display_name": raw["nameLocal"],
        "category": raw["nameCategory"],
        "texture_set": raw["nameTextureSet"],
        "material_pre": raw.get("materialPre"),
        "material_post": raw.get("materialPost"),
        "amount": amount_data,
        "state": raw["state"],
        "heat_damage": raw["heatDamage"],
        "tags": sorted(raw["tags"]),
        "familiar_prefixes": sorted(set(raw["familiarPrefixes"])),
        "byproducts": byproducts,
        "registered_materials": sorted(set(raw["registeredMaterials"])),
        "registered_item_count": raw["registeredItemCount"],
        "registered_prefix_item_count": raw["registeredPrefixItemCount"],
        "t0_mapping": T0_PREFIXES.get(raw["nameInternal"]),
        "activated": False,
    }


def validate_references(materials: list[dict[str, Any]]) -> None:
    by_name: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for value in materials:
        by_name[value["source_name"]].append(value)
    id_names: dict[int, set[str]] = defaultdict(set)
    for value in materials:
        id_names[value["source_id"]].add(value["source_name"])
    aliases = {alias for value in materials for alias in value["aliases"]}

    def require_reference(name: str, source_id: int, context: str) -> None:
        if name not in by_name and name not in aliases:
            raise ImportError(f"{context} references unknown material {name}")
        if source_id >= 0 and source_id not in id_names:
            raise ImportError(f"{context} references unknown material id {source_id}")

    graph: dict[str, list[str]] = {}
    for value in materials:
        source = value["source_name"]
        source_key = f"{value['source_id']}:{source}"
        graph[source_key] = []
        for entry in value["components"]:
            require_reference(entry["material"], entry["material_id"], f"{source}.components")
            candidates = by_name.get(entry["material"], [])
            target = next(
                (candidate for candidate in candidates
                 if candidate["source_id"] == entry["material_id"]),
                candidates[0] if candidates else None,
            )
            if target is not None:
                graph[source_key].append(f"{target['source_id']}:{target['source_name']}")
        for target, entry in value["processing_targets"].items():
            require_reference(entry["material"], entry["material_id"], f"{source}.targets.{target}")
        for byproduct in value["byproducts"]:
            if byproduct not in by_name and byproduct not in aliases:
                raise ImportError(f"{source}.byproducts references unknown material {byproduct}")

    visiting: set[str] = set()
    visited: set[str] = set()

    def visit(name: str, path: list[str]) -> None:
        if name in visiting:
            raise ImportError("composition cycle: " + " -> ".join(path + [name]))
        if name in visited:
            return
        visiting.add(name)
        for target in graph[name]:
            visit(target, path + [name])
        visiting.remove(name)
        visited.add(name)

    for name in sorted(graph):
        visit(name, [])


def load_alias_overrides() -> dict[str, Any]:
    if not ALIASES.is_file():
        raise ImportError(f"missing explicit alias override artifact: {ALIASES}")
    value = json.loads(ALIASES.read_text(encoding="utf-8"))
    if not isinstance(value, dict) or not isinstance(value.get("source_to_cc"), dict):
        raise ImportError("alias override file must contain source_to_cc object")
    return value


def load_activation_overrides() -> dict[str, Any]:
    if not ACTIVATION_OVERRIDES.is_file():
        raise ImportError(f"missing reviewed activation overrides: {ACTIVATION_OVERRIDES}")
    value = json.loads(ACTIVATION_OVERRIDES.read_text(encoding="utf-8"))
    for key in (
            "evidence_maps", "include", "dependency_include", "exclude", "excluded_tags"):
        if key not in value or not isinstance(value[key], list if key == "evidence_maps" else dict):
            raise ImportError(f"activation overrides must contain {key}")
    overlap = set(value["include"]) & set(value["exclude"])
    if overlap:
        raise ImportError(f"activation overrides both include and exclude: {sorted(overlap)}")
    form_include = value.setdefault("form_include", {})
    if not isinstance(form_include, dict) or any(
            not isinstance(source, str)
            or not isinstance(forms, list)
            or any(not isinstance(form, str) for form in forms)
            for source, forms in form_include.items()):
        raise ImportError("activation form_include must map source names to form lists")
    return value


def recipe_material_evidence(
        recipe_root: Path,
        map_names: list[str],
        fluid_map: dict[str, dict[str, Any]],
) -> dict[int, dict[str, dict[str, int]]]:
    evidence: dict[int, dict[str, Counter[str]]] = defaultdict(
        lambda: {"item": Counter(), "fluid": Counter()})
    for map_name in map_names:
        path = recipe_root / "maps" / f"{map_name}.json"
        if not path.is_file():
            raise ImportError(f"missing reviewed recipe evidence map: {map_name}")
        data = json.loads(path.read_text(encoding="utf-8"))
        if data.get("nameInternal") != map_name:
            raise ImportError(f"recipe evidence map identity mismatch: {map_name}")
        for recipe in data.get("recipes") or []:
            if recipe.get("enabled") is False:
                continue
            for side in ("inputs", "outputs"):
                for item in recipe.get(side) or []:
                    if (item and isinstance(item.get("meta"), int)
                            and str(item.get("item") or "").startswith("gregtech:gt.meta.")):
                        material_id = int(item["meta"])
                        if material_id < 0:
                            raise ImportError(
                                "recipe appearance cannot use negative material "
                                f"id {material_id}: {map_name} {side}"
                            )
                        evidence[material_id]["item"][map_name] += 1
            for side in ("fluidInputs", "fluidOutputs"):
                for fluid in recipe.get(side) or []:
                    fluid_id = str((fluid or {}).get("fluid") or "")
                    mapped = fluid_map.get(fluid_id)
                    if mapped and mapped.get("materialId", -1) >= 0:
                        evidence[mapped["materialId"]]["fluid"][map_name] += 1
    result = {
        source_id: {
            kind: dict(sorted(counts.items()))
            for kind, counts in sorted(kinds.items())
            if counts
        }
        for source_id, kinds in evidence.items()
    }
    if any(source_id < 0 for source_id in result):
        raise ImportError("recipe appearance evidence contains a negative material id")
    return result


def material_evidence_for_source(
        evidence: dict[int, dict[str, dict[str, int]]],
        source_id: Any,
) -> dict[str, dict[str, int]]:
    """Lookup evidence only for canonical material IDs; never query id=-1."""
    if not isinstance(source_id, int) or source_id < 0:
        return {}
    return evidence.get(source_id, {})


def activation_evidence_map_hashes(
        recipe_root: Path,
        map_names: list[str],
) -> dict[str, dict[str, str]]:
    if len(map_names) != len(set(map_names)):
        raise ImportError("activation evidence_maps contains duplicates")
    result: dict[str, dict[str, str]] = {}
    for map_name in sorted(map_names):
        path = recipe_root / "maps" / f"{map_name}.json"
        if not path.is_file():
            raise ImportError(f"missing activation evidence map: {map_name}")
        result[map_name] = {
            "path": path.relative_to(ROOT).as_posix(),
            "sha256": file_hash(path),
        }
    return result


def _reference_source(
        reference_name: str,
        reference_id: int | None,
        by_name: dict[str, list[dict[str, Any]]],
        canonical_by_id: dict[int, dict[str, Any]],
) -> str | None:
    if reference_id is not None and reference_id >= 0 and reference_id in canonical_by_id:
        return canonical_by_id[reference_id]["source_name"]
    candidates = by_name.get(reference_name) or []
    if len(candidates) == 1:
        return candidates[0]["source_name"]
    candidate_ids = {candidate["source_id"] for candidate in candidates}
    if len(candidate_ids) == 1:
        source_id = next(iter(candidate_ids))
        if source_id in canonical_by_id:
            return canonical_by_id[source_id]["source_name"]
    return None


def close_component_forms(
        active: set[str],
        forms_by_source: dict[str, list[str]],
        by_source: dict[str, dict[str, Any]],
        by_name: dict[str, list[dict[str, Any]]],
        canonical_by_id: dict[int, dict[str, Any]],
) -> tuple[dict[str, list[str]], dict[str, int]]:
    """Retain only GT-evidenced component forms with a concrete enabled route."""
    reachable = {
        source: set(forms_by_source[source]) - T3_COMPONENT_FORMS
        for source in active
    }
    candidate_count = sum(
        len(set(forms_by_source[source]) & T3_COMPONENT_FORMS)
        for source in active)
    changed = True
    while changed:
        changed = False
        for source in sorted(active):
            source_forms = reachable[source]
            for target_key, inputs, output in T3_PRODUCTION_ROUTES:
                if not all(form in source_forms for form, _ in inputs):
                    continue
                target = by_source[source]["processing_targets"].get(target_key)
                if not target or target.get("cc_units") is None:
                    continue
                target_units = target["cc_units"]
                if not isinstance(target_units, int) or target_units <= 0:
                    continue
                target_source = _reference_source(
                    target["material"], target["material_id"],
                    by_name, canonical_by_id)
                if (target_source not in active
                        or output not in forms_by_source[target_source]
                        or output in reachable[target_source]):
                    continue
                source_units = sum(PREFIX_UNITS[form] * count for form, count in inputs)
                numerator = target_units * source_units
                denominator = CC_UNITS_PER_INGOT * PREFIX_UNITS[output]
                divisor = math.gcd(numerator, denominator)
                input_batch = denominator // divisor
                output_count = numerator // divisor
                if (output_count > 64
                        or any(count * input_batch > 64 for _, count in inputs)):
                    continue
                reachable[target_source].add(output)
                changed = True
            # GT6 Loader_OreProcessing.java:183-184 directly combines conductor
            # wire with ANY.Rubber plate. OP.java:644-648 records the embedded
            # Rubber plate amounts for all retained cable gauges.
            if "plate" in reachable.get("Rubber", set()):
                for wire, cable, _rubber_plates in T3_CABLE_ROUTES:
                    if (wire in source_forms
                            and cable in forms_by_source[source]
                            and cable not in source_forms):
                        source_forms.add(cable)
                        changed = True

    ordered_forms = list(T0_PREFIXES.values())
    result = {
        source: [form for form in ordered_forms if form in reachable[source]]
        for source in sorted(active)
    }
    retained_count = sum(
        len(set(forms) & T3_COMPONENT_FORMS) for forms in result.values())
    return result, {
        "component_form_candidates": candidate_count,
        "retained_component_forms": retained_count,
        "pruned_unproducible_component_forms": candidate_count - retained_count,
        "unproduced_component_items": 0,
    }


def choose_policy(
        materials: list[dict[str, Any]],
        prefixes: list[dict[str, Any]],
        overrides: dict[str, Any],
        activation: dict[str, Any],
        evidence: dict[int, dict[str, dict[str, int]]],
) -> tuple[
    list[dict[str, Any]],
    dict[str, str],
    dict[str, list[str]],
    set[str],
    set[str],
    dict[str, int],
]:
    prefix_regs = {
        value["source_name"]: set(value["registered_materials"])
        for value in prefixes if value["source_name"] in T0_PREFIXES
    }
    # GT6's dustPure prefix is process-generated and has no direct registration
    # list. Restrict it to materials evidenced in both the final ore stage and
    # ordinary dust generation rather than granting it globally.
    prefix_regs["dustPure"] = (
        prefix_regs.get("crushedCentrifuged", set())
        & prefix_regs.get("dust", set()))
    by_id: dict[int, list[dict[str, Any]]] = defaultdict(list)
    for value in materials:
        by_id[value["source_id"]].append(value)
    canonical_by_id: dict[int, dict[str, Any]] = {}
    for source_id, records in by_id.items():
        if source_id < 0:
            continue
        ranked = sorted(
            records,
            key=lambda value: (
                value["target_registration"] != value["source_name"],
                value["hidden"],
                value["source_name"],
            ),
        )
        canonical_by_id[source_id] = ranked[0]

    forced = {source: cc for cc, source in AUTHORED_IDS.items()}
    if set(activation["include"]) != set(forced):
        raise ImportError("reviewed include overrides must exactly preserve the 12 authored sources")
    decisions: list[dict[str, Any]] = []
    decision_by_source: dict[str, dict[str, Any]] = {}
    cc_ids: dict[str, str] = {}
    forms_by_source: dict[str, list[str]] = {}
    by_name: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for value in materials:
        by_name[value["source_name"]].append(value)
    by_source = {
        value["source_name"]: value
        for value in materials
        if canonical_by_id.get(value["source_id"]) is value
    }
    authored_sources = set(forced)
    local_errors: dict[str, tuple[str, str]] = {}
    force_no_decompose: set[str] = set()
    validation_counts: Counter[str] = Counter()

    for source_record_index, value in enumerate(materials):
        source = value["source_name"]
        forms = sorted(
            {
                T0_PREFIXES[prefix]
                for prefix, registered in prefix_regs.items()
                if source in registered
            },
            key=lambda form: list(T0_PREFIXES.values()).index(form),
        )
        if source in authored_sources:
            authored_forms = list(AUTHORED_BASE_FORMS[forced[source]])
            forms = authored_forms + [
                form for form in forms
                if form in IMPORTED_FORMS and form not in authored_forms
            ]
        status = "DEFERRED"
        reason = "not used by reviewed maps and not required by active material references"
        cc_id = forced.get(source, slug(source))
        if source in authored_sources:
            reason = "preserved authored CrucibleCraft material; imported metadata merged"
            cc_id = forced[source]
        elif source in activation["exclude"]:
            status, reason = "OUT_OF_SCOPE", activation["exclude"][source]
            local_errors[source] = (status, reason)
        elif value["source_id"] < 0:
            status, reason = "OUT_OF_SCOPE", "external/placeholder material has no stable GT6 source id"
            local_errors[source] = (status, reason)
        elif canonical_by_id.get(value["source_id"]) is not value:
            status, reason = "OUT_OF_SCOPE", "duplicate-id alias; canonical record retained"
        elif value["hidden"]:
            status, reason = "OUT_OF_SCOPE", "GT6 marks the record hidden"
            local_errors[source] = (status, reason)
        elif (source not in activation["dependency_include"]
              and any(tag in activation["excluded_tags"] for tag in value["material_tags"])):
            tag = next(tag for tag in value["material_tags"] if tag in activation["excluded_tags"])
            status, reason = "OUT_OF_SCOPE", activation["excluded_tags"][tag]
            local_errors[source] = (status, reason)
        if canonical_by_id.get(value["source_id"]) is value:
            cc_ids[source] = cc_id
            forms_by_source[source] = forms
        decision = {
            "source_record_index": source_record_index,
            "source_id": value["source_id"],
            "source_name": source,
            "cc_id": None,
            "status": status,
            "reason": reason,
            "available_t0_forms": forms,
            "t0_forms": [],
            "metadata_only": False,
            "recipe_evidence": material_evidence_for_source(
                evidence,
                value["source_id"],
            ),
        }
        decisions.append(decision)
        if canonical_by_id.get(value["source_id"]) is value:
            decision_by_source[source] = decision

    # Deterministic ids must be unique even when activation is reached transitively.
    id_sources: dict[str, list[str]] = defaultdict(list)
    for source, cc_id in cc_ids.items():
        id_sources[cc_id].append(source)
    for cc_id, sources in id_sources.items():
        if len(sources) > 1:
            for source in sources:
                if source not in authored_sources:
                    local_errors[source] = (
                        "DEFERRED",
                        f"CrucibleCraft id collision {cc_id} after deterministic normalization")

    def references(source: str) -> list[tuple[str | None, str, str]]:
        value = by_source[source]
        result = [
            (_reference_source(entry["material"], entry["material_id"],
                               by_name, canonical_by_id),
             "composition", entry["material"])
            for entry in value["components"]
        ]
        result.extend(
            (_reference_source(entry["material"], entry["material_id"],
                               by_name, canonical_by_id),
             "target", entry["material"])
            for entry in value["processing_targets"].values())
        result.extend(
            (_reference_source(name, None, by_name, canonical_by_id),
             "byproduct", name)
            for name in value["byproducts"])
        return result

    # Generated compositions that cannot exactly decompose one 144-unit ingot
    # may still have items, but must not expose runtime decomposition.
    for source, value in by_source.items():
        if source in authored_sources or source in local_errors or not value["components"]:
            continue
        component_units: dict[str, int] = defaultdict(int)
        for component in value["components"]:
            target = _reference_source(
                component["material"], component["material_id"], by_name, canonical_by_id)
            if component["numerator_u"] <= 0:
                local_errors[source] = (
                    "DEFERRED", "composition amount is not a positive exact GT6-U ratio")
                validation_counts["non_integral_compositions"] += 1
                break
            if target is None:
                local_errors[source] = (
                    "DEFERRED", f"composition reference {component['material']} is unresolved")
                validation_counts["unresolved_composition_dependencies"] += 1
                break
            component_units[target] += component["numerator_u"]
        if source in local_errors:
            continue
        divisor = reduce(math.gcd, component_units.values())
        ratio_total = sum(units // divisor for units in component_units.values())
        if CC_UNITS_PER_INGOT % ratio_total != 0:
            force_no_decompose.add(source)
            validation_counts["non_decomposable_composition_ratios"] += 1

    direct_reasons: dict[str, str] = {}
    roots: set[str] = set(authored_sources)
    for source, value in by_source.items():
        source_evidence = evidence.get(value["source_id"], {})
        item_evidence = bool(source_evidence.get("item"))
        fluid_evidence = bool(source_evidence.get("fluid"))
        if source in authored_sources:
            direct_reasons[source] = (
                "preserved authored CrucibleCraft material; imported metadata merged")
        elif source in local_errors:
            continue
        elif fluid_evidence:
            roots.add(source)
            direct_reasons[source] = (
                "fluid evidence in reviewed T2/T3 material-processing maps"
                + ("; item-prefix evidence also present" if item_evidence else ""))
        elif (
            item_evidence
            and forms_by_source[source]
            and source not in force_no_decompose
            and value["generation_tags"]
        ):
            roots.add(source)
            direct_reasons[source] = (
                "item-prefix evidence in reviewed T2/T3 material-processing maps")
        elif item_evidence and (
            not forms_by_source[source] or source in force_no_decompose
        ):
            decision_by_source[source]["reason"] = (
                "item evidence exists but no safe current-T0 activation root is "
                "available")
        elif item_evidence:
            decision_by_source[source]["reason"] = (
                "item evidence exists but GT6 has no ITEMGENERATOR evidence")

    active: set[str] = set()
    visiting: set[str] = set()
    transitive_provenance: dict[str, dict[str, Any]] = {}
    failures: dict[str, str] = {}

    def evidence_maps(source: str) -> list[str]:
        source_evidence = evidence.get(by_source[source]["source_id"], {})
        return sorted({
            map_name
            for maps in source_evidence.values()
            for map_name in maps
        })

    def admit(source: str, chain: list[str], root: str) -> bool:
        if source in active or source in visiting:
            return True
        if source not in by_source:
            failures[source] = f"unresolved material identity in {' -> '.join(chain)}"
            return False
        if source in local_errors:
            failures[source] = local_errors[source][1]
            return False
        visiting.add(source)
        for target, kind, raw_name in references(source):
            if target is None or not admit(
                    target, chain + [f"{kind}:{raw_name}"], root):
                visiting.remove(source)
                failures[source] = (
                    f"{kind} dependency {raw_name} cannot be activated"
                    + (f": {failures.get(target, 'unresolved reference')}" if target else ""))
                return False
            if target != source:
                transitive_provenance.setdefault(target, {
                    "kind": f"transitive_{kind}",
                    "edge_kind": kind,
                    "parent": source,
                    "root": root,
                    "root_maps": evidence_maps(root),
                })
        visiting.remove(source)
        active.add(source)
        return True

    successful_roots: set[str] = set()
    for source in sorted(roots, key=lambda value: (value not in authored_sources, value)):
        if admit(source, [source], source):
            successful_roots.add(source)
        elif source in authored_sources:
            raise ImportError(
                f"authored material {source} has unresolvable strict reference closure: "
                f"{failures[source]}")
        else:
            decision_by_source[source]["reason"] = failures[source]

    # A cycle encountered from one failed root must not leave a partially
    # admitted node. Prune until every retained node has a strict closed graph.
    while True:
        unclosed: dict[str, str] = {}
        for source in active:
            for target, kind, raw_name in references(source):
                if target not in active:
                    unclosed[source] = f"{kind} dependency {raw_name} is not ACTIVE"
                    break
        if not unclosed:
            break
        authored_unclosed = sorted(set(unclosed) & authored_sources)
        if authored_unclosed:
            source = authored_unclosed[0]
            raise ImportError(
                f"authored material {source} has unresolvable strict reference closure: "
                f"{unclosed[source]}")
        for source, reason in unclosed.items():
            active.remove(source)
            failures[source] = reason
            decision_by_source[source]["reason"] = reason
    successful_roots &= active

    # Remove dependencies reached only while evaluating a root that later failed.
    reachable: set[str] = set()
    pending = list(successful_roots)
    while pending:
        source = pending.pop()
        if source in reachable:
            continue
        reachable.add(source)
        pending.extend(
            target for target, _, _ in references(source)
            if target is not None and target in active)
    active &= reachable

    # Closure nodes may carry exact source compositions whose recursive
    # decomposition quantum is not representable by the runtime int API. Keep
    # their observed item forms and composition provenance, but explicitly
    # disable runtime decomposition.
    while True:
        quantum_cache: dict[str, int] = {}

        def runtime_quantum(source: str, stack: set[str]) -> int:
            if source in quantum_cache:
                return quantum_cache[source]
            value = by_source[source]
            if (source in force_no_decompose
                    or "COMPOUNDS.DECOMPOSABLE" not in value["material_tags"]
                    or not value["components"]):
                quantum_cache[source] = 1
                return 1
            if source in stack:
                raise ImportError(f"runtime composition cycle involving {source}")
            stack.add(source)
            component_units: dict[str, int] = defaultdict(int)
            for component in value["components"]:
                target = _reference_source(
                    component["material"], component["material_id"],
                    by_name, canonical_by_id)
                if target not in active:
                    raise ImportError(f"ACTIVE {source} has missing composition {target}")
                component_units[target] += component["numerator_u"]
            divisor = reduce(math.gcd, component_units.values())
            ratios = {
                target: units // divisor for target, units in component_units.items()
            }
            ratio_total = sum(ratios.values())
            quantum = 1
            for target, ratio in ratios.items():
                required = ratio_total * runtime_quantum(target, stack)
                required //= math.gcd(required, ratio)
                quantum = math.lcm(quantum, required)
            stack.remove(source)
            quantum_cache[source] = quantum
            return quantum

        newly_no_decompose = {
            source for source in active
            if source not in authored_sources
            and source not in activation["form_include"]
            and source not in force_no_decompose
            and (
                runtime_quantum(source, set()) > 2_147_483_647
                or (
                    "ingot" in forms_by_source[source]
                    and CC_UNITS_PER_INGOT % runtime_quantum(source, set()) != 0
                )
            )
        }
        if not newly_no_decompose:
            break
        force_no_decompose.update(newly_no_decompose)
        validation_counts["runtime_quantum_no_decompose"] += len(
            newly_no_decompose
        )

    reviewed_active = set(active)
    pre_gate_registered_forms = {
        source: list(forms_by_source[source])
        for source in reviewed_active
    }
    for source, forms in activation["form_include"].items():
        if source in pre_gate_registered_forms:
            pre_gate_registered_forms[source] = list(forms)
    pre_gate_registered_forms, _ = close_component_forms(
        reviewed_active,
        pre_gate_registered_forms,
        by_source,
        by_name,
        canonical_by_id,
    )
    stable_catalog = set(by_source)
    newly_imported = stable_catalog - reviewed_active
    active = stable_catalog
    # The remaining stable canonical rows are imported for factual metadata and
    # forms. Their decomposition graph was not part of the reviewed T0 recipe
    # closure, so keep it inert until a later phase explicitly enables it.
    force_no_decompose.update(newly_imported)
    source_to_cc = {source: cc_ids[source] for source in sorted(active)}
    source_forms = {
        source: forms_by_source[source]
        for source in sorted(active)
    }
    for source, explicit_forms in EXPLICIT_MATERIAL_FORM_INCLUDES.items():
        if source not in active:
            raise ImportError(f"explicit material form source is not ACTIVE: {source}")
        source_forms[source] = [
            *source_forms[source],
            *sorted(explicit_forms - set(source_forms[source])),
        ]
    for source, forms in activation["form_include"].items():
        if source not in active:
            raise ImportError(f"form_include source is not ACTIVE: {source}")
        unsupported = set(forms) - set(forms_by_source[source])
        if unsupported:
            raise ImportError(
                f"form_include {source} lacks GT prefix evidence: {sorted(unsupported)}")
        source_forms[source] = list(forms)
    gt6_original_forms = {
        source: list(forms) for source, forms in source_forms.items()
    }
    corrections = json.loads(
        ACCEPTANCE_FORM_CORRECTIONS.read_text(encoding="utf-8")
    ).get("corrections") or []
    acceptance_by_source: dict[str, list[dict[str, Any]]] = defaultdict(list)
    source_by_cc = {cc_id: source for source, cc_id in source_to_cc.items()}
    for correction in corrections:
        if (
            correction.get("classification")
            != "t3_acceptance_required_not_gt6_original_gate"
        ):
            raise ImportError("acceptance form correction classification drift")
        material_id = correction.get("material")
        source = source_by_cc.get(material_id)
        if source is None:
            raise ImportError(
                f"acceptance form correction references inactive material: {material_id}"
            )
        forms = correction.get("add_forms") or []
        if not forms or any(form not in T0_PREFIXES.values() for form in forms):
            raise ImportError(
                f"acceptance form correction has unsupported forms: {material_id}"
            )
        source_forms[source] = [
            *source_forms[source],
            *sorted(set(forms) - set(source_forms[source])),
        ]
        acceptance_by_source[source].append(correction)
    metadata_only_sources = {
        source for source in active if not source_forms[source]
    }
    validation_counts["t3_acceptance_form_corrections"] = sum(
        len(row["add_forms"]) for row in corrections
    )
    validation_counts["component_form_candidates"] = sum(
        len(set(forms) & T3_COMPONENT_FORMS) for forms in source_forms.values())
    validation_counts["retained_component_forms"] = validation_counts[
        "component_form_candidates"
    ]
    validation_counts["pruned_unproducible_component_forms"] = 0
    validation_counts["unproduced_component_items"] = 0
    for source in active:
        decision = decision_by_source[source]
        provenance = (
            {
                "kind": "authored",
                "root": source,
                "root_maps": evidence_maps(source),
            }
            if source in authored_sources
            else {
                "kind": (
                    "direct_fluid" if direct_reasons.get(source, "").startswith("fluid")
                    else "direct_item"),
                "root": source,
                "root_maps": evidence_maps(source),
            }
            if source in direct_reasons
            else transitive_provenance[source]
            if source in transitive_provenance
            else {
                "kind": "stable_catalog",
                "root": source,
                "root_maps": evidence_maps(source),
            }
        )
        transitive_reason = (
            f"transitive {provenance.get('edge_kind')} dependency required by "
            f"parent={provenance.get('parent')} root={provenance['root']}"
            + (
                f" maps={provenance['root_maps']}"
                if provenance["root_maps"] else "")
        )
        decision.update(
            status="ACTIVE",
            cc_id=cc_ids[source],
            reason=(
                direct_reasons.get(
                    source,
                    transitive_reason
                    if source in transitive_provenance
                    else "stable canonical GT6 material imported for factual metadata/forms"
                )
                + (
                    "; metadata_only=true (no GT6-registered prefix maps to a CC prefix)"
                    if source in metadata_only_sources else "")
                + (
                    "; runtime decomposition disabled (source ratio is not exactly "
                    "representable)"
                    if source in force_no_decompose else "")
                + (
                    "; reviewed insulation form include from GT6 cable Rubber-plate evidence"
                    if source in activation["form_include"] else "")
                + (
                    "; T3 acceptance form correction explicitly not present in "
                    "the original GT6 registration gate"
                    if source in acceptance_by_source else "")
            ),
            t0_forms=source_forms[source],
            gt6_original_t0_forms=gt6_original_forms[source],
            acceptance_form_corrections=acceptance_by_source.get(source, []),
            pre_gate_registered_forms=pre_gate_registered_forms.get(source, []),
            metadata_only=source in metadata_only_sources,
            activation_provenance=provenance)
    policy = sorted(decisions, key=lambda value: (
        value["source_id"], value["source_name"], value["source_record_index"]))
    validation_counts["active_composition_records"] = sum(
        bool(by_source[source]["components"]) for source in active)
    validation_counts["active_processing_targets"] = sum(
        len(by_source[source]["processing_targets"]) for source in active)
    validation_counts["active_byproduct_references"] = sum(
        len(by_source[source]["byproducts"]) for source in active)
    validation_counts["active_item_bearing_materials"] = sum(
        bool(source_forms[source]) for source in active)
    validation_counts["active_metadata_only_materials"] = len(metadata_only_sources)
    validation_counts["dropped_active_references"] = 0
    return (
        policy,
        source_to_cc,
        source_forms,
        force_no_decompose & active,
        metadata_only_sources,
        dict(sorted(validation_counts.items())),
    )


def metadata_for(
        value: dict[str, Any],
        source_to_cc: dict[str, str],
        by_name: dict[str, list[dict[str, Any]]],
        canonical_by_id: dict[int, dict[str, Any]],
        *,
        strict: bool,
) -> dict[str, Any]:
    def resolve(
            name: str, source_id: int | None, context: str
    ) -> tuple[str, int, str] | None:
        source = _reference_source(name, source_id, by_name, canonical_by_id)
        if source is None or source not in source_to_cc:
            if strict:
                raise ImportError(
                    f"ACTIVE {value['source_name']} has orphan {context} reference {name}")
            return None
        record = next(record for record in canonical_by_id.values()
                      if record["source_name"] == source)
        return source_to_cc[source], record["source_id"], source

    def model_amount(entry: dict[str, Any]) -> dict[str, Any]:
        resolved = resolve(entry["material"], entry["material_id"], "processing target")
        if resolved is None:
            return {}
        cc_id, source_id, source_name = resolved
        result = {
            "material": cc_id,
            "source_material_id": source_id,
            "source_material_name": source_name,
            "numerator_u": entry["numerator_u"],
        }
        if entry["cc_units"] is not None:
            result["cc_units"] = entry["cc_units"]
        return result

    byproducts = []
    for name in value["byproducts"]:
        resolved = resolve(name, None, "byproduct")
        if resolved is None:
            continue
        cc_id, source_id, source_name = resolved
        byproducts.append({
            "material": cc_id,
            "source_id": source_id,
            "source_name": source_name,
        })
    return {
        "source_id": value["source_id"],
        "source_name": value["source_name"],
        "aliases": value["aliases"],
        "state": value["state"],
        **({"formula": value["formula"]} if value["formula"] else {}),
        "source_thermal": value["thermal"],
        "tool": value["tool"],
        "byproducts": byproducts,
        "processing_targets": {
            key: resolved
            for key, entry in value["processing_targets"].items()
            if (resolved := model_amount(entry))
        },
        "material_tags": value["material_tags"],
        "generation_tags": value["generation_tags"],
        "explosion_damage": value["explosion_damage"],
        "heat_damage": value["heat_damage"],
        **({
            "blast_furnace_temperature_celsius":
                value["blast_furnace_temperature_celsius"]
        } if value["blast_furnace_temperature_celsius"] is not None else {}),
        "electrical_by_specification": value["electrical_by_specification"],
    }


def build_material_files(
        materials: list[dict[str, Any]],
        source_to_cc: dict[str, str],
        source_forms: dict[str, list[str]],
        source_no_decompose: set[str],
        metadata_only_sources: set[str],
        l3_document: dict[str, Any],
) -> dict[str, str]:
    by_name: dict[str, list[dict[str, Any]]] = defaultdict(list)
    by_id: dict[int, list[dict[str, Any]]] = defaultdict(list)
    for value in materials:
        by_name[value["source_name"]].append(value)
        by_id[value["source_id"]].append(value)
    canonical_by_id = {
        source_id: sorted(records, key=lambda value: (
            value["target_registration"] != value["source_name"],
            value["hidden"],
            value["source_name"],
        ))[0]
        for source_id, records in by_id.items() if source_id >= 0
    }
    by_source = {
        value["source_name"]: value for value in canonical_by_id.values()
    }
    files: dict[str, str] = {}
    for source, cc_id in sorted(source_to_cc.items(), key=lambda item: item[1]):
        value = by_source[source]
        metadata_only = source in metadata_only_sources
        if bool(source_forms[source]) == metadata_only:
            raise ImportError(
                f"metadata-only/form invariant failed for ACTIVE material: {source}"
            )
        path = MATERIAL_DIR / f"{cc_id}.json"
        if cc_id in AUTHORED_IDS:
            authored = json.loads(path.read_text(encoding="utf-8"))
            authored = gt6_l3_materials.apply_material_structure(
                authored,
                source,
                source_forms[source],
                l3_document,
                metadata_only=metadata_only,
            )
            authored = apply_acceptance_form_encoding(
                authored,
                cc_id,
                source_forms[source],
                l3_document,
            )
            authored["gt6_metadata"] = metadata_for(
                value, source_to_cc, by_name, canonical_by_id, strict=True)
            files[path.as_posix()] = stable_json(authored)
            continue
        rgba = value["color_rgba"]
        melting_point = value["thermal"]["melting_point_celsius"]
        boiling_point = value["thermal"]["boiling_point_celsius"]
        if boiling_point <= melting_point:
            boiling_point = max(melting_point + 1.0, melting_point * 2.0)
        density = value["thermal"]["density"]
        if density <= 0:
            density = 1.0
        result = {
            "id": cc_id,
            "tag_name": cc_id,
            "tier": 0,
            "color": f"#{rgba[0]:02X}{rgba[1]:02X}{rgba[2]:02X}",
            "tint_style": "metallic" if value["has_metallum"] else "matte",
            "thermal": {
                "melting_point": melting_point,
                "boiling_point": boiling_point,
                "density": density,
            },
            "molten_fluid": bool(value["fluid"] and value["fluid"].get("amount") == 144),
            "no_decompose": (
                not source_forms[source]
                or source in source_no_decompose
                or "COMPOUNDS.DECOMPOSABLE" not in value["material_tags"]),
            "gt6_metadata": metadata_for(
                value, source_to_cc, by_name, canonical_by_id, strict=True),
        }
        if value["components"]:
            component_units: dict[str, int] = defaultdict(int)
            for component in value["components"]:
                component_source = _reference_source(
                    component["material"], component["material_id"],
                    by_name, canonical_by_id)
                if component_source is None or component_source not in source_to_cc:
                    raise ImportError(
                        f"ACTIVE {value['source_name']} has unresolved runtime composition")
                units = component["numerator_u"]
                if units <= 0:
                    raise ImportError(
                        f"ACTIVE {value['source_name']} has non-positive runtime composition")
                component_units[source_to_cc[component_source]] += units
            divisor = reduce(math.gcd, component_units.values())
            result["composition"] = {
                material: units // divisor
                for material, units in sorted(component_units.items())
            }
        result = gt6_l3_materials.apply_material_structure(
            result,
            source,
            source_forms[source],
            l3_document,
            metadata_only=metadata_only,
        )
        result = apply_acceptance_form_encoding(
            result,
            cc_id,
            source_forms[source],
            l3_document,
        )
        files[path.as_posix()] = stable_json(result)
    index = [f"{cc_id}.json" for cc_id in sorted(source_to_cc.values())]
    files[(MATERIAL_DIR / "index.json").as_posix()] = stable_json(index)
    return files


@dataclass(frozen=True)
class ImportResult:
    outputs: dict[Path, str]
    generated_material_files: tuple[str, ...]
    counts: dict[str, int]


def validate_authored_structural_baseline(
        expected: dict[str, str] | None = None) -> None:
    baseline = expected or json.loads(
        AUTHORED_BASELINE.read_text(encoding="utf-8"))["structural_hashes"]
    if set(baseline) != set(AUTHORED_IDS):
        raise ImportError("authored structural baseline does not cover exactly the 12 materials")
    for cc_id, expected_hash in baseline.items():
        path = MATERIAL_DIR / f"{cc_id}.json"
        if not path.is_file():
            raise ImportError(f"missing authored material definition: {cc_id}")
        value = json.loads(path.read_text(encoding="utf-8"))
        actual = stable_hash(structural_material(value))
        if actual != expected_hash:
            raise ImportError(f"authored structural gameplay fields changed: {cc_id}")


def write_authored_structural_baseline() -> None:
    document = {
        "schema_version": 1,
        "description": (
            "Intentional structural baseline for the twelve authored materials; "
            "update only after reviewing registration-facing changes."
        ),
        "structural_hashes": {
            cc_id: stable_hash(structural_material(json.loads(
                (MATERIAL_DIR / f"{cc_id}.json").read_text(encoding="utf-8")
            )))
            for cc_id in sorted(AUTHORED_IDS)
        },
    }
    AUTHORED_BASELINE.write_text(stable_json(document), encoding="utf-8", newline="\n")


def build_outputs(dump_root: Path) -> ImportResult:
    validate_authored_structural_baseline()
    provenance, raw_materials, raw_prefixes, raw_fluids = load_dump(dump_root)
    electrical_source = load_electrical_source()
    provenance["electrical_source"] = {
        "path": ELECTRICAL_SOURCE.relative_to(ROOT).as_posix(),
        "sha256": file_hash(ELECTRICAL_SOURCE),
        "revision": electrical_source["source"]["revision"],
    }
    l3_document = gt6_l3_materials.build_document(
        raw_materials,
        json.loads(
            gt6_l3_materials.GENERATION_BITS_PATH.read_text(encoding="utf-8")
        ),
    )
    texture_sets = load_material_texture_sets(RECIPE_DUMP_ROOT)
    materials = sorted((normalize_material(value) for value in raw_materials),
                       key=lambda value: (value["source_id"], value["source_name"]))
    apply_electrical_source(materials, electrical_source)
    for value in materials:
        mapped = texture_sets.get(value["source_id"])
        if mapped:
            value["texture_set_item"] = mapped["texture_set_item"]
            value["texture_set_block"] = mapped["texture_set_block"]
        else:
            value["texture_set_item"] = None
            value["texture_set_block"] = None
    if texture_sets:
        provenance["sha256"]["textures/materials.json"] = file_hash(
            RECIPE_DUMP_ROOT / "textures" / "materials.json")
    prefixes = sorted((normalize_prefix(value) for value in raw_prefixes),
                      key=lambda value: value["source_name"])
    validate_references(materials)
    if len({value["source_name"] for value in prefixes}) != len(prefixes):
        raise ImportError("duplicate GT6 prefix IDs")
    overrides = load_alias_overrides()
    activation = load_activation_overrides()
    provenance["activation_evidence_maps"] = activation_evidence_map_hashes(
        RECIPE_DUMP_ROOT,
        activation["evidence_maps"],
    )
    evidence = recipe_material_evidence(
        RECIPE_DUMP_ROOT, activation["evidence_maps"], raw_fluids)
    (
        policy,
        source_to_cc,
        source_forms,
        source_no_decompose,
        metadata_only_sources,
        reference_validation,
    ) = choose_policy(
        materials, prefixes, overrides, activation, evidence)
    policy.sort(key=lambda value: (value["source_id"], value["source_name"]))
    statuses = Counter(value["status"] for value in policy)

    fluids = []
    material_names = {value["source_name"] for value in materials}
    for fluid_id, value in sorted(raw_fluids.items()):
        require_type(value, {"material": str, "materialId": int, "amount": int},
                     f"fluid[{fluid_id}]")
        if value["material"] not in material_names and value["materialId"] >= 0:
            raise ImportError(f"fluid {fluid_id} references unknown material")
        fluids.append({"fluid": fluid_id, **amount(
            value["material"], value["materialId"], value["amount"] * GT6_U_PER_INGOT // 144
        ), "fluid_amount": value["amount"]})

    prefix_item_to_form = {
        GT_PREFIX_ITEMS[source]: T0_PREFIXES[source]
        for source in T0_PREFIXES
        if source in GT_PREFIX_ITEMS
    }
    prefix_item_to_form.update({
        f"gregtech:gt.meta.{source}": form
        for source, form in COMPARATOR_ONLY_PREFIXES.items()
    })
    aliases_to_cc: dict[str, list[str]] = defaultdict(list)
    by_source = {value["source_name"]: value for value in materials}
    for source, cc_id in source_to_cc.items():
        for alias in [source, by_source[source]["display_name"], *by_source[source]["aliases"]]:
            aliases_to_cc[alias].append(cc_id)
    for source, cc_id in overrides["source_to_cc"].items():
        if cc_id in source_to_cc.values():
            aliases_to_cc[source].append(cc_id)
    cross_reference = {
        "schema_version": 1,
        "material_id_to_cc": {
            str(by_source[source]["source_id"]): cc_id
            for source, cc_id in source_to_cc.items()
            if by_source[source]["source_id"] >= 0
        },
        "material_name_to_cc": {
            alias: sorted(set(ids))
            for alias, ids in sorted(aliases_to_cc.items())
        },
        "prefix_item_to_form": dict(sorted(prefix_item_to_form.items())),
        "prefix_item_to_gt_prefix": {
            (
                "gregtech:gt.meta.storage.solid"
                if value["source_name"] == "blockIngot"
                else f"gregtech:gt.meta.{value['source_name']}"
            ): value["source_name"]
            for value in prefixes
        },
        "gt_prefix_to_cc": dict(sorted(T0_PREFIXES.items())),
        "fluid_to_material": {
            value["fluid"]: source_to_cc[value["material"]]
            for value in fluids if value["material"] in source_to_cc
        },
        "alias_overrides_sha256": file_hash(ALIASES),
    }
    material_files = build_material_files(
        materials,
        source_to_cc,
        source_forms,
        source_no_decompose,
        metadata_only_sources,
        l3_document,
    )
    authored_baseline_doc = {
        "schema_version": 1,
        "structural_hashes": {
            cc_id: stable_hash(structural_material(json.loads(
                material_files[(MATERIAL_DIR / f"{cc_id}.json").as_posix()]
            )))
            for cc_id in sorted(AUTHORED_IDS)
        },
    }
    runtime_materials = [
        json.loads(content)
        for path, content in material_files.items()
        if Path(path).parent == MATERIAL_DIR and Path(path).name != "index.json"
    ]
    active_policy_by_id = {
        record["cc_id"]: record
        for record in policy if record["status"] == "ACTIVE"
    }
    form_mismatches = []
    for runtime in runtime_materials:
        runtime_forms = gt6_l3_materials.resolve_material_forms(
            runtime,
            l3_document,
        )
        policy_forms = gt6_l3_materials.close_implied_prefixes(
            active_policy_by_id[runtime["id"]]["t0_forms"]
        )
        if policy_forms != runtime_forms:
            form_mismatches.append(runtime["id"])
    if form_mismatches:
        raise ImportError(
            "activation policy/runtime form mismatch: "
            + ", ".join(sorted(form_mismatches)))
    reference_validation["policy_runtime_form_mismatches"] = 0
    runtime_reference_counts = {
        "runtime_composition_records": sum(
            bool(value.get("composition")) for value in runtime_materials),
        "runtime_byproduct_references": sum(
            len((value.get("gt6_metadata") or {}).get("byproducts") or [])
            for value in runtime_materials),
        "runtime_processing_targets": sum(
            len((value.get("gt6_metadata") or {}).get("processing_targets") or {})
            for value in runtime_materials),
        "runtime_zero_processing_targets": sum(
            target.get("numerator_u") == 0
            for value in runtime_materials
            for target in (value.get("gt6_metadata") or {})
                .get("processing_targets", {}).values()),
    }
    reference_validation.update(runtime_reference_counts)
    generated = tuple(sorted(
        Path(path).name for path in material_files
        if Path(path).parent == MATERIAL_DIR and Path(path).name not in
        {"index.json", *(f"{cc_id}.json" for cc_id in AUTHORED_IDS)}
    ))
    schema = {
        "material_fields": sorted(set().union(*(value.keys() for value in raw_materials))),
        "prefix_fields": sorted(set().union(*(value.keys() for value in raw_prefixes))),
        "fluid_value_fields": sorted(set().union(*(value.keys() for value in raw_fluids.values()))),
    }
    counts = {
        "normalized_materials": len(materials),
        "normalized_prefixes": len(prefixes),
        "normalized_fluids": len(fluids),
        "active_materials": statuses["ACTIVE"],
        "deferred_materials": statuses["DEFERRED"],
        "out_of_scope_materials": statuses["OUT_OF_SCOPE"],
        "active_item_bearing_materials": reference_validation.get(
            "active_item_bearing_materials", 0),
        "active_metadata_only_materials": reference_validation.get(
            "active_metadata_only_materials", 0),
        "dropped_active_references": reference_validation.get(
            "dropped_active_references", -1),
        "policy_runtime_form_mismatches": reference_validation.get(
            "policy_runtime_form_mismatches", -1),
        "retained_component_forms": reference_validation.get(
            "retained_component_forms", 0),
        "pruned_unproducible_component_forms": reference_validation.get(
            "pruned_unproducible_component_forms", 0),
        "unproduced_component_items": reference_validation.get(
            "unproduced_component_items", -1),
        "generated_materials": len(generated),
        "newly_activated_prefixes": 0,
        "runtime_prefixes_total": len(json.loads(
            (ROOT / "src" / "main" / "resources" / "data"
             / "cruciblecraft" / "material_prefixes" / "index.json")
            .read_text(encoding="utf-8")
        )),
        "composition_records": runtime_reference_counts["runtime_composition_records"],
        "processing_target_references": runtime_reference_counts[
            "runtime_processing_targets"],
        "byproduct_references": runtime_reference_counts[
            "runtime_byproduct_references"],
    }
    generated_item_estimate_by_prefix: Counter[str] = Counter()
    for source, cc_id in source_to_cc.items():
        overridden_forms = set()
        if cc_id in AUTHORED_IDS:
            authored = json.loads(
                (MATERIAL_DIR / f"{cc_id}.json").read_text(encoding="utf-8"))
            overridden_forms = set((authored.get("form_items") or {}).keys())
        generated_item_estimate_by_prefix.update(
            form for form in source_forms[source] if form not in overridden_forms)
    normalized_material_doc = {
        "schema_version": 1, "provenance": provenance, "records": materials,
    }
    normalized_prefix_doc = {
        "schema_version": 1, "provenance": provenance, "records": prefixes,
    }
    normalized_fluid_doc = {
        "schema_version": 1, "provenance": provenance, "records": fluids,
    }
    policy_doc = {
        "schema_version": 1,
        "policy": {
            "rule": (
                "canonical visible records with registered T0 forms, reviewed-map "
                "recipe evidence, category exclusions, and closed runtime references"),
            "t0_only": False,
            "non_integral_u": "DEFERRED",
            "authored_materials": dict(sorted(AUTHORED_IDS.items())),
            "activation_overrides_sha256": file_hash(ACTIVATION_OVERRIDES),
            "acceptance_form_corrections_sha256": file_hash(
                ACCEPTANCE_FORM_CORRECTIONS
            ),
            "acceptance_form_correction_classification": (
                "t3_acceptance_required_not_gt6_original_gate"
            ),
            "evidence_maps": activation["evidence_maps"],
        },
        "counts": dict(sorted(statuses.items())),
        "reference_validation": reference_validation,
        "generated_item_estimate_by_prefix": dict(
            sorted(generated_item_estimate_by_prefix.items())),
        "records": policy,
    }
    material_definition_hashes = {
        Path(path).name: stable_hash(strip_t8_pipe_projection(
            json.loads(content)))
        for path, content in material_files.items()
        if Path(path).parent == MATERIAL_DIR
    }
    expected_index = json.loads(
        material_files[(MATERIAL_DIR / "index.json").as_posix()])
    manifest = {
        "schema_version": 1,
        "provenance": provenance,
        "actual_dump_schema": schema,
        "counts": counts,
        "artifact_hashes": {
            NORMALIZED_MATERIALS.name: stable_hash(normalized_material_doc),
            NORMALIZED_PREFIXES.name: stable_hash(normalized_prefix_doc),
            NORMALIZED_FLUIDS.name: stable_hash(normalized_fluid_doc),
            POLICY.name: stable_hash(policy_doc),
            CROSS_REFERENCE.name: stable_hash(cross_reference),
            ALIASES.name: stable_hash(overrides),
            ACTIVATION_OVERRIDES.name: stable_hash(activation),
            ELECTRICAL_SOURCE.name: stable_hash(electrical_source),
            "acceptance_form_corrections.json": stable_hash(json.loads(
                ACCEPTANCE_FORM_CORRECTIONS.read_text(encoding="utf-8")
            )),
            AUTHORED_BASELINE.name: stable_hash(authored_baseline_doc),
            gt6_l3_materials.GENERATION_BITS_PATH.name: stable_hash(json.loads(
                gt6_l3_materials.GENERATION_BITS_PATH.read_text(encoding="utf-8")
            )),
            gt6_l3_materials.MAPPING_PATH.name: stable_hash(json.loads(
                gt6_l3_materials.MAPPING_PATH.read_text(encoding="utf-8")
            )),
            gt6_l3_materials.OUT.name: stable_hash(l3_document),
            # artifact_hashes records self/upstream integrity only; consumers
            # #3 (ore chain) and #5 (form gate) pin this import's outputs as
            # forward edges, so their artifacts are deliberately absent here.
            REGISTRY_BUDGET.name: stable_hash(json.loads(
                REGISTRY_BUDGET.read_text(encoding="utf-8")
            )),
        },
        "generated_material_files": list(generated),
        "expected_material_index": expected_index,
        "material_definition_hashes": dict(sorted(material_definition_hashes.items())),
        "authored_structural_hashes": authored_baseline_doc["structural_hashes"],
        "generated_item_estimate_by_prefix": dict(
            sorted(generated_item_estimate_by_prefix.items())),
    }
    outputs = {
        NORMALIZED_MATERIALS: stable_json(normalized_material_doc, pretty=False),
        NORMALIZED_PREFIXES: stable_json(normalized_prefix_doc, pretty=False),
        NORMALIZED_FLUIDS: stable_json(normalized_fluid_doc, pretty=False),
        POLICY: stable_json(policy_doc),
        CROSS_REFERENCE: stable_json(cross_reference),
        AUTHORED_BASELINE: stable_json(authored_baseline_doc),
        gt6_l3_materials.OUT: (
            json.dumps(l3_document, indent=2, ensure_ascii=False) + "\n"
        ),
        MANIFEST: stable_json(manifest),
        **gt6_l3_materials.prefix_definition_outputs(l3_document),
        **{Path(path): content for path, content in material_files.items()},
    }
    return ImportResult(outputs, generated, counts)


def validate_manifest_artifact_hashes(
        manifest: dict[str, Any],
        tools_dir: Path,
) -> None:
    required_hashes = {
        NORMALIZED_MATERIALS.name,
        NORMALIZED_PREFIXES.name,
        NORMALIZED_FLUIDS.name,
        POLICY.name,
        CROSS_REFERENCE.name,
        ALIASES.name,
        ACTIVATION_OVERRIDES.name,
        ELECTRICAL_SOURCE.name,
        "acceptance_form_corrections.json",
        AUTHORED_BASELINE.name,
        gt6_l3_materials.GENERATION_BITS_PATH.name,
        gt6_l3_materials.MAPPING_PATH.name,
        gt6_l3_materials.OUT.name,
        REGISTRY_BUDGET.name,
    }
    missing_hashes = sorted(required_hashes - set(manifest["artifact_hashes"]))
    if missing_hashes:
        raise ImportError(
            "import manifest omits integrity artifact(s): "
            + ", ".join(missing_hashes)
        )
    for filename, expected in manifest["artifact_hashes"].items():
        path = (
            ACCEPTANCE_FORM_CORRECTIONS
            if filename == "acceptance_form_corrections.json"
            and tools_dir.resolve() == TOOLS.resolve()
            else tools_dir / filename
        )
        if not path.is_file():
            raise ImportError(f"missing manifest integrity artifact: {filename}")
        actual = stable_hash(json.loads(path.read_text(encoding="utf-8")))
        if actual != expected:
            raise ImportError(f"normalized reference hash mismatch: {filename}")


def check_committed() -> dict[str, int]:
    required = [
        NORMALIZED_MATERIALS, NORMALIZED_PREFIXES, NORMALIZED_FLUIDS,
        POLICY, CROSS_REFERENCE, MANIFEST,
    ]
    missing = [str(path) for path in required if not path.is_file()]
    if missing:
        raise ImportError("missing normalized references: " + ", ".join(missing))
    manifest = json.loads(MANIFEST.read_text(encoding="utf-8"))
    validate_manifest_artifact_hashes(manifest, TOOLS)
    policy = json.loads(POLICY.read_text(encoding="utf-8"))
    if len(policy["records"]) != EXPECTED_COUNTS["materialCount"]:
        raise ImportError("activation policy does not cover every normalized material")
    expected_evidence_maps = set(policy["policy"]["evidence_maps"])
    evidence_provenance = manifest["provenance"].get(
        "activation_evidence_maps"
    ) or {}
    if set(evidence_provenance) != expected_evidence_maps:
        raise ImportError(
            "activation evidence-map provenance does not exactly cover policy maps"
        )
    if RECIPE_DUMP_ROOT.is_dir():
        actual_evidence = activation_evidence_map_hashes(
            RECIPE_DUMP_ROOT,
            sorted(expected_evidence_maps),
        )
        if evidence_provenance != actual_evidence:
            raise ImportError("activation evidence-map provenance hash mismatch")
    validate_material_manifest(manifest, MATERIAL_DIR, check_authored=True)
    return manifest["counts"]


def validate_material_manifest(
        manifest: dict[str, Any],
        material_dir: Path,
        *,
        check_authored: bool,
) -> None:
    expected_index = manifest["expected_material_index"]
    actual_index = json.loads((material_dir / "index.json").read_text(encoding="utf-8"))
    if actual_index != expected_index:
        raise ImportError("material index contents/order differ from import manifest")
    expected_generated = set(manifest["generated_material_files"])
    indexed_generated = {
        filename for filename in actual_index
        if Path(filename).stem not in AUTHORED_IDS
    }
    if indexed_generated != expected_generated:
        raise ImportError("material index generated-file set differs from import manifest")
    actual_generated = {
        path.name for path in material_dir.glob("*.json")
        if path.name != "index.json" and path.stem not in AUTHORED_IDS
    }
    if actual_generated != expected_generated:
        missing_generated = sorted(expected_generated - actual_generated)
        extra_generated = sorted(actual_generated - expected_generated)
        raise ImportError(
            f"generated material file set mismatch; missing={missing_generated[:8]}, "
            f"extra={extra_generated[:8]}")
    for filename, expected_hash in manifest["material_definition_hashes"].items():
        path = material_dir / filename
        if not path.is_file():
            raise ImportError(f"missing indexed material definition: {filename}")
        actual_hash = stable_hash(strip_t8_pipe_projection(
            json.loads(path.read_text(encoding="utf-8"))
        ))
        if actual_hash != expected_hash:
            raise ImportError(f"material definition hash mismatch: {filename}")
    if check_authored:
        validate_authored_structural_baseline(manifest["authored_structural_hashes"])



def write_or_check(result: ImportResult, check: bool) -> None:
    mismatches = []
    for path, expected in result.outputs.items():
        if not path.is_file():
            mismatches.append(str(path))
            continue
        actual = path.read_text(encoding="utf-8")
        if (
            path.parent == MATERIAL_DIR
            and path.name != "index.json"
        ):
            actual = stable_json(strip_t8_pipe_projection(
                json.loads(actual)
            ))
        if actual != expected:
            mismatches.append(str(path))
    if check:
        if mismatches:
            raise ImportError("generated GT6 import artifacts are stale: " + ", ".join(mismatches[:12]))
        return
    previous: set[str] = set()
    if MANIFEST.is_file():
        previous = set(json.loads(MANIFEST.read_text(encoding="utf-8"))
                       .get("generated_material_files") or [])
    for path, content in result.outputs.items():
        if (
            path.is_file()
            and path.parent == MATERIAL_DIR
            and path.name != "index.json"
        ):
            content = stable_json(preserve_t8_pipe_projection(
                json.loads(content),
                json.loads(path.read_text(encoding="utf-8")),
            ))
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(content, encoding="utf-8", newline="\n")
    for filename in previous - set(result.generated_material_files):
        path = MATERIAL_DIR / filename
        if path.is_file():
            path.unlink()


def parse_args(argv: Iterable[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--dump-root", type=Path, default=DEFAULT_DUMP)
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--write-authored-baseline", action="store_true")
    parser.add_argument("--reference-only", action="store_true")
    return parser.parse_args(argv)


def main(argv: Iterable[str] | None = None) -> int:
    args = parse_args(argv)
    try:
        if args.check and args.write:
            raise ImportError("--check is read-only and cannot be combined with --write")
        if args.write_authored_baseline:
            if args.check or args.write or args.reference_only:
                raise ImportError(
                    "--write-authored-baseline cannot be combined with other modes"
                )
            write_authored_structural_baseline()
            print(json.dumps({"authored_materials": len(AUTHORED_IDS)}))
            return 0
        if args.reference_only and args.write:
            raise ImportError("--reference-only cannot be combined with --write")
        if args.reference_only or (args.check and not args.dump_root.is_dir()):
            counts = check_committed()
        else:
            result = build_outputs(args.dump_root)
            if args.check:
                write_or_check(result, True)
            elif args.write:
                write_or_check(result, False)
            counts = result.counts
        print(json.dumps(counts, sort_keys=True))
        return 0
    except (ImportError, OSError, json.JSONDecodeError, KeyError, TypeError) as error:
        print(f"GT6 import failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
