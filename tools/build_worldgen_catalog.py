#!/usr/bin/env python3
"""Build and audit the expanded ore and subsurface-fluid worldgen catalog."""
from __future__ import annotations

import argparse
import hashlib
import json
import math
import re
import shutil
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
TOOLS = ROOT / "tools"
sys.path.insert(0, str(ROOT))

from tools import build_gt6_veins as vein_builder  # noqa: E402
from tools import t20_worldgen_rows  # noqa: E402

ORE_DECLARATIONS = (
    ROOT
    / "src/main/resources/data/cruciblecraft/worldgen_catalog/ore_veins.json"
)
FLUID_DECLARATIONS = (
    ROOT
    / "src/main/resources/data/cruciblecraft/worldgen_catalog/fluid_deposits.json"
)
SURFACE_SCATTER_DECLARATIONS = (
    ROOT
    / "src/main/resources/data/cruciblecraft/worldgen_catalog/surface_scatter.json"
)
CONFIGURED_SURFACE_RUNTIME = (
    ROOT
    / "src/main/resources/data/cruciblecraft/worldgen/configured_feature"
    / "surface_rock_scatter.json"
)
PLACED_SURFACE_RUNTIME = (
    ROOT
    / "src/main/resources/data/cruciblecraft/worldgen/placed_feature"
    / "surface_rock_scatter.json"
)
BIOME_MODIFIER_SURFACE_RUNTIME = (
    ROOT
    / "src/main/resources/data/cruciblecraft/neoforge/biome_modifier"
    / "add_surface_rocks.json"
)
ROCK_PREFIX_DEFINITION = (
    ROOT
    / "src/main/resources/data/cruciblecraft/material_prefixes/rock.json"
)
MATERIAL_REGISTRATION_GATE = (
    ROOT
    / "src/main/resources/data/cruciblecraft/material_registration_gate.json"
)
T11_RUNTIME_POLICY = (
    ROOT
    / "src/main/resources/data/cruciblecraft"
    / "t11_hydrocarbon_runtime_policy.json"
)
T11_NATURAL_GAS_MATERIAL = (
    ROOT
    / "src/main/resources/data/cruciblecraft/t11_materials"
    / "natural_gas.json"
)
CLOSURE_LEDGER = TOOLS / "gt6_ore_chain_closure.json"
T20_SOURCE_POLICY = TOOLS / "t20_worldgen_source_policy.json"
OUTPUT_RESOURCE_ROOT = ROOT / "src/worldgen_catalog_generated/resources"
READINESS = TOOLS / "worldgen_catalog_readiness.json"
RESOURCE_PATH = re.compile(r"[a-z0-9_.-]+")
RESOURCE_LOCATION = re.compile(r"([a-z0-9_.-]+):([a-z0-9_./-]+)")
FLUID_FEATURE_TYPE = "cruciblecraft:subsurface_fluid_deposit"
SURFACE_FEATURE_TYPE = "cruciblecraft:surface_rock_scatter"
SURFACE_BIOME_MODIFIER_ID = "add_surface_rocks"
SURFACE_DECORATION_STEP = "top_layer_modification"
SURFACE_OVERWORLD_BIOMES = "#minecraft:is_overworld"


def load(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def stable_json(value: Any, *, compact: bool = False) -> str:
    if compact:
        return json.dumps(
            value,
            ensure_ascii=False,
            separators=(",", ":"),
            sort_keys=True,
        ) + "\n"
    return json.dumps(
        value,
        ensure_ascii=False,
        indent=2,
        sort_keys=True,
    ) + "\n"


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def stable_salt(identifier: str) -> int:
    digest = hashlib.sha256(
        f"cruciblecraft:worldgen_catalog:{identifier}".encode("utf-8")
    ).digest()
    return int.from_bytes(digest[:4], byteorder="big", signed=True)


def require_mapping(value: Any, label: str) -> dict[str, Any]:
    if not isinstance(value, dict):
        raise ValueError(f"{label} must be an object")
    return value


def require_int(
    document: dict[str, Any],
    field: str,
    minimum: int,
    maximum: int,
    label: str,
) -> int:
    value = document.get(field)
    if isinstance(value, bool) or not isinstance(value, int):
        raise ValueError(f"{label}.{field} must be an integer")
    if not minimum <= value <= maximum:
        raise ValueError(
            f"{label}.{field} must be between {minimum} and {maximum}"
        )
    return value


def require_number(
    document: dict[str, Any],
    field: str,
    minimum: float,
    maximum: float,
    label: str,
) -> float:
    value = document.get(field)
    if isinstance(value, bool) or not isinstance(value, (int, float)):
        raise ValueError(f"{label}.{field} must be a number")
    result = float(value)
    if not math.isfinite(result) or not minimum <= result <= maximum:
        raise ValueError(
            f"{label}.{field} must be between {minimum} and {maximum}"
        )
    return result


def closure_vein_materials() -> set[str]:
    document = require_mapping(load(CLOSURE_LEDGER), str(CLOSURE_LEDGER))
    rows = document.get("crusher_without_worldgen")
    if not isinstance(rows, list):
        raise ValueError("closure crusher_without_worldgen must be an array")
    materials = {
        row["material"]
        for row in rows
        if isinstance(row, dict) and row.get("classification") == "vein"
    }
    reported = (
        document.get("counts", {})
        .get("crusher_classifications", {})
        .get("vein")
    )
    if reported != len(materials):
        raise ValueError(
            "closure vein classification count does not match unique rows"
        )
    return materials


def load_closure_veins(
    capabilities: dict[str, dict[str, set[str]]],
) -> list[dict[str, Any]]:
    document = require_mapping(load(ORE_DECLARATIONS), str(ORE_DECLARATIONS))
    if document.get("schema_version") != 2:
        raise ValueError("Closure ore declarations schema_version must be 2")
    if document.get("id") != "ore_vein_closure":
        raise ValueError(
            "Closure ore declarations id must be ore_vein_closure"
        )
    if document.get("profile_version") != 2:
        raise ValueError("Closure ore declarations profile_version must be 2")
    source_ledger = require_mapping(
        document.get("source_ledger"), "source_ledger"
    )
    if (
        source_ledger.get("path")
        != "tools/gt6_ore_chain_closure.json"
        or source_ledger.get("classification") != "vein"
    ):
        raise ValueError("Closure ore declarations must name the fixed T2c ledger")
    source_contract = require_mapping(
        document.get("source_contract"), "source_contract"
    )
    if (
        source_contract.get("policy")
        != "tools/t20_worldgen_source_policy.json"
        or source_contract.get("expected")
        != "tools/t20_worldgen_expected.json"
        or source_contract.get("revision")
        != "3703e40308c8c030763fd6297dea8b210d2a77b1"
    ):
        raise ValueError("Closure ore declarations T20 source contract drifted")
    rows = document.get("veins")
    if not isinstance(rows, list) or not rows:
        raise ValueError("Closure ore declarations veins must be non-empty")
    materials = [
        row.get("catalog_material")
        for row in rows
        if isinstance(row, dict)
    ]
    if len(materials) != len(rows) or any(
        not isinstance(material, str)
        or RESOURCE_PATH.fullmatch(material) is None
        for material in materials
    ):
        raise ValueError("Closure ore declarations contain an invalid material id")
    if len(materials) != len(set(materials)):
        raise ValueError("Closure ore declarations contain duplicate materials")

    expected = closure_vein_materials()
    actual = set(materials)
    if actual != expected:
        raise ValueError(
            "Closure vein ledger mismatch; "
            f"missing={sorted(expected - actual)}, "
            f"stale={sorted(actual - expected)}"
        )
    # Equality between the authored rows here and the independent T20 expected
    # rows is proven by build_t20_worldgen_projection (index 51) via
    # compare_authored, a forward edge 15 -> 51.  This builder must not pin
    # #51's artifact or the graph regains a back edge.
    veins = [
        vein_builder.validate_vein(
            Path(f"{row['id']}.json"), row, capabilities
        )
        for row in rows
    ]
    return veins


def load_geometry_policy(rows: list[dict[str, Any]]) -> dict[str, Any]:
    counts = t20_worldgen_rows.row_count_summary(rows)
    if (
        counts.get("catalog_entries") != 129
        or counts.get("placeholder") != 0
        or counts.get("unverified") != 0
        or counts.get("unclassified") != 0
    ):
        raise ValueError("T20 authored geometry is not fully classified")
    return {
        "status": "T20_CLASSIFIED",
        "profile_version": 2,
        "source_revision": (
            "3703e40308c8c030763fd6297dea8b210d2a77b1"
        ),
        "open_item": None,
        "gt6_worldgen_import": "CLASSIFIED_WITH_EXPLICIT_POLICY",
        "classifications": counts["classifications"],
        "fidelity_statuses": counts["statuses"],
        "placeholder": 0,
        "unverified": 0,
        "distinct_geometry_signatures": counts[
            "distinct_geometry_signatures"
        ],
        "non_claim": (
            "DESIGN_POLICY rows and source-derived NeoForge geometry are not "
            "claimed as behaviorally identical GT6 large veins."
        ),
    }


def validate_fluid_deposit(
    entry: Any,
    index: int,
    material_documents: dict[str, dict[str, Any]],
    production_policy: dict[str, Any],
) -> dict[str, Any]:
    label = f"fluid_deposits[{index}]"
    document = require_mapping(entry, label)
    deposit_id = document.get("id")
    if (
        not isinstance(deposit_id, str)
        or RESOURCE_PATH.fullmatch(deposit_id) is None
    ):
        raise ValueError(f"{label}.id must be an unnamespaced resource path")
    material = document.get("material")
    match = (
        RESOURCE_LOCATION.fullmatch(material)
        if isinstance(material, str)
        else None
    )
    if match is None or match.group(1) != "cruciblecraft":
        raise ValueError(
            f"{label}.material must be a cruciblecraft resource location"
        )
    material_id = match.group(2)
    material_document = material_documents.get(material_id)
    if material_document is None:
        raise ValueError(f"{label} references unknown material {material}")
    production = production_policy.get(material)
    state = (material_document.get("gt6_metadata") or {}).get("state")
    if state is None and isinstance(production, dict):
        state = production.get("state")
    if state not in {"liquid", "gas"}:
        raise ValueError(
            f"{label} material {material} is not source-state liquid or gas"
        )
    min_y = require_int(document, "min_y", -64, 320, label)
    max_y = require_int(document, "max_y", -64, 320, label)
    if min_y > max_y:
        raise ValueError(f"{label}.min_y must not exceed max_y")
    minimum = require_int(
        document, "min_amount_mb", 1, 1_000_000_000, label
    )
    maximum = require_int(
        document, "max_amount_mb", 1, 1_000_000_000, label
    )
    if minimum > maximum:
        raise ValueError(
            f"{label}.min_amount_mb must not exceed max_amount_mb"
        )
    replaceable = document.get("replaceable")
    if (
        not isinstance(replaceable, str)
        or RESOURCE_LOCATION.fullmatch(replaceable) is None
    ):
        raise ValueError(
            f"{label}.replaceable must be a resource location"
        )
    provenance = document.get("provenance")
    if not isinstance(provenance, str) or not provenance.strip():
        raise ValueError(f"{label}.provenance must be non-empty")
    if not isinstance(production, dict):
        raise ValueError(f"{label} is missing T11 production policy for {material}")
    amount = require_int(production, "amount_mb", 1, 1_000_000, f"{label}.production")
    interval = require_int(
        production, "interval_ticks", 1, 72_000, f"{label}.production"
    )
    accumulation_cap = require_int(
        production,
        "accumulation_cap_mb",
        amount,
        1_000_000_000,
        f"{label}.production",
    )
    vent_overflow = production.get("vent_overflow")
    if not isinstance(vent_overflow, bool):
        raise ValueError(f"{label}.production.vent_overflow must be a boolean")
    if vent_overflow != (state == "gas"):
        raise ValueError(
            f"{label}.production.vent_overflow must match gas state"
        )
    return {
        "id": deposit_id,
        "material": material,
        "material_state": state,
        "min_amount_mb": minimum,
        "max_amount_mb": maximum,
        "min_y": min_y,
        "max_y": max_y,
        "search_range": require_int(
            document, "search_range", 0, 64, label
        ),
        "replaceable": replaceable,
        "region_size_chunks": require_int(
            document, "region_size_chunks", 2, 64, label
        ),
        "generation_chance": require_number(
            document, "generation_chance", 0.0, 1.0, label
        ),
        "salt": require_int(
            document, "salt", -(2**31), 2**31 - 1, label
        ),
        "provenance": provenance,
        "production_amount_mb": amount,
        "production_interval_ticks": interval,
        "accumulation_cap_mb": accumulation_cap,
        "vent_overflow": vent_overflow,
    }


def _require_resource_location(value: Any, label: str) -> str:
    if (
        not isinstance(value, str)
        or RESOURCE_LOCATION.fullmatch(value) is None
    ):
        raise ValueError(f"{label} must be a resource location")
    return value


def _configured_surface_from_declaration(
    declaration: dict[str, Any],
) -> dict[str, Any]:
    config = require_mapping(declaration.get("config"), "surface_scatter.config")
    return {
        "type": declaration["feature_type"],
        "config": {
            "rarity": require_int(config, "rarity", 1, 1_000_000, "surface_scatter.config"),
            "rock_tag": _require_resource_location(
                config.get("rock_tag"),
                "surface_scatter.config.rock_tag",
            ),
        },
    }


def _biome_modifier_from_declaration(
    declaration: dict[str, Any],
) -> dict[str, Any]:
    modifier = require_mapping(
        declaration.get("biome_modifier"),
        "surface_scatter.biome_modifier",
    )
    modifier_id = modifier.get("id")
    if (
        not isinstance(modifier_id, str)
        or RESOURCE_PATH.fullmatch(modifier_id) is None
    ):
        raise ValueError(
            "surface_scatter.biome_modifier.id must be an unnamespaced resource path"
        )
    if modifier_id != SURFACE_BIOME_MODIFIER_ID:
        raise ValueError(
            "surface_scatter.biome_modifier.id must be add_surface_rocks"
        )
    modifier_type = modifier.get("type")
    if modifier_type != "neoforge:add_features":
        raise ValueError(
            "surface_scatter.biome_modifier.type must be neoforge:add_features"
        )
    biomes = modifier.get("biomes")
    if biomes != SURFACE_OVERWORLD_BIOMES:
        raise ValueError(
            "surface_scatter.biome_modifier.biomes must be #minecraft:is_overworld"
        )
    step = modifier.get("step")
    if step != SURFACE_DECORATION_STEP:
        raise ValueError(
            "surface_scatter.biome_modifier.step must be top_layer_modification"
        )
    features = modifier.get("features")
    if not isinstance(features, list) or len(features) != 1:
        raise ValueError(
            "surface_scatter.biome_modifier.features must be a single-entry array"
        )
    placed_feature = declaration.get("placed_feature")
    if features[0] != placed_feature:
        raise ValueError(
            "surface_scatter.biome_modifier.features must match placed_feature"
        )
    return {
        "type": modifier_type,
        "biomes": biomes,
        "features": features,
        "step": step,
    }


def _placed_surface_from_declaration(
    declaration: dict[str, Any],
) -> dict[str, Any]:
    placed_feature = _require_resource_location(
        declaration.get("placed_feature"),
        "surface_scatter.placed_feature",
    )
    scatter_id = declaration.get("id")
    if (
        not isinstance(scatter_id, str)
        or RESOURCE_PATH.fullmatch(scatter_id) is None
    ):
        raise ValueError(
            "surface_scatter.id must be an unnamespaced resource path"
        )
    expected = f"cruciblecraft:{scatter_id}"
    if placed_feature != expected:
        raise ValueError(
            "surface_scatter.placed_feature must reference cruciblecraft:id"
        )
    return {
        "feature": placed_feature,
        "placement": [],
    }


def _cross_check_surface_runtime(
    declaration: dict[str, Any],
    *,
    configured: dict[str, Any],
    placed: dict[str, Any],
    modifier: dict[str, Any],
) -> None:
    expected_configured = _configured_surface_from_declaration(declaration)
    expected_placed = _placed_surface_from_declaration(declaration)
    expected_modifier = _biome_modifier_from_declaration(declaration)
    if configured != expected_configured:
        raise ValueError(
            "surface scatter configured_feature runtime JSON drifted from declaration"
        )
    if placed != expected_placed:
        raise ValueError(
            "surface scatter placed_feature runtime JSON drifted from declaration"
        )
    if modifier != expected_modifier:
        raise ValueError(
            "surface scatter biome_modifier runtime JSON drifted from declaration"
        )
    if declaration.get("feature_type") != configured.get("type"):
        raise ValueError(
            "surface scatter declaration feature_type must match configured type"
        )
    if declaration.get("placed_feature") != placed.get("feature"):
        raise ValueError(
            "surface scatter declaration placed_feature must match placed feature"
        )


def _validate_rock_tag_source(source: dict[str, Any]) -> dict[str, Any]:
    prefix = source.get("prefix")
    if prefix != "rock":
        raise ValueError("surface_scatter.rock_tag_source.prefix must be rock")
    generation_flag = source.get("generation_flag")
    if generation_flag != "cruciblecraft:generates_rock":
        raise ValueError(
            "surface_scatter.rock_tag_source.generation_flag drifted"
        )
    tag_namespace = source.get("tag_namespace")
    tag_directory = source.get("tag_directory")
    if tag_namespace != "c" or tag_directory != "rocks":
        raise ValueError(
            "surface_scatter.rock_tag_source tag namespace/directory drifted"
        )
    runtime_pack = source.get("runtime_pack")
    if runtime_pack != "GeneratedMaterialPack":
        raise ValueError(
            "surface_scatter.rock_tag_source.runtime_pack must be "
            "GeneratedMaterialPack"
        )
    gate_path = source.get("material_gate")
    prefix_path = source.get("prefix_definition")
    if gate_path != str(
        MATERIAL_REGISTRATION_GATE.relative_to(ROOT).as_posix()
    ):
        raise ValueError(
            "surface_scatter.rock_tag_source.material_gate path drifted"
        )
    if prefix_path != str(
        ROCK_PREFIX_DEFINITION.relative_to(ROOT).as_posix()
    ):
        raise ValueError(
            "surface_scatter.rock_tag_source.prefix_definition path drifted"
        )
    prefix_document = require_mapping(
        load(ROCK_PREFIX_DEFINITION),
        str(ROCK_PREFIX_DEFINITION),
    )
    if (
        prefix_document.get("tag_namespace") != tag_namespace
        or prefix_document.get("tag_directory") != tag_directory
        or prefix_document.get("generation_flag") != generation_flag
    ):
        raise ValueError(
            "surface_scatter rock prefix definition does not match rock_tag_source"
        )
    gate_document = require_mapping(
        load(MATERIAL_REGISTRATION_GATE),
        str(MATERIAL_REGISTRATION_GATE),
    )
    materials = gate_document.get("materials")
    if not isinstance(materials, dict):
        raise ValueError("material registration gate materials must be an object")
    rock_materials = sorted(
        material_id
        for material_id, forms in materials.items()
        if isinstance(forms, list) and prefix in forms
    )
    if not rock_materials:
        raise ValueError(
            "material registration gate has no rock-form materials"
        )
    return {
        "prefix": prefix,
        "generation_flag": generation_flag,
        "tag": f"{tag_namespace}:{tag_directory}",
        "runtime_pack": runtime_pack,
        "rock_material_count": len(rock_materials),
    }


def load_surface_scatter() -> dict[str, Any]:
    document = require_mapping(
        load(SURFACE_SCATTER_DECLARATIONS),
        str(SURFACE_SCATTER_DECLARATIONS),
    )
    if document.get("schema_version") != 1:
        raise ValueError("surface scatter schema_version must be 1")
    scatter_id = document.get("id")
    if scatter_id != "surface_rock_scatter":
        raise ValueError("surface scatter id must be surface_rock_scatter")
    feature_type = document.get("feature_type")
    if feature_type != SURFACE_FEATURE_TYPE:
        raise ValueError(
            "surface scatter feature_type must be cruciblecraft:surface_rock_scatter"
        )
    provenance = document.get("provenance")
    if not isinstance(provenance, str) or not provenance.strip():
        raise ValueError("surface scatter provenance must be non-empty")
    design_policy = document.get("design_policy")
    if design_policy != "DESIGN_POLICY":
        raise ValueError("surface scatter design_policy must be DESIGN_POLICY")
    rock_tag_source = _validate_rock_tag_source(
        require_mapping(
            document.get("rock_tag_source"),
            "surface_scatter.rock_tag_source",
        )
    )
    configured_runtime = require_mapping(
        load(CONFIGURED_SURFACE_RUNTIME),
        str(CONFIGURED_SURFACE_RUNTIME),
    )
    placed_runtime = require_mapping(
        load(PLACED_SURFACE_RUNTIME),
        str(PLACED_SURFACE_RUNTIME),
    )
    modifier_runtime = require_mapping(
        load(BIOME_MODIFIER_SURFACE_RUNTIME),
        str(BIOME_MODIFIER_SURFACE_RUNTIME),
    )
    _cross_check_surface_runtime(
        document,
        configured=configured_runtime,
        placed=placed_runtime,
        modifier=modifier_runtime,
    )
    configured = _configured_surface_from_declaration(document)
    modifier = _biome_modifier_from_declaration(document)
    return {
        "id": scatter_id,
        "feature_type": feature_type,
        "configured_feature": f"cruciblecraft:{scatter_id}",
        "placed_feature": document["placed_feature"],
        "biome_modifier_id": modifier["features"][0],
        "biomes": modifier["biomes"],
        "decoration_step": modifier["step"],
        "rarity": configured["config"]["rarity"],
        "rock_tag": configured["config"]["rock_tag"],
        "provenance": provenance,
        "design_policy": design_policy,
        "rock_tag_source": rock_tag_source,
        "audit_mode": "check_only",
    }


def load_fluid_deposits() -> list[dict[str, Any]]:
    document = require_mapping(
        load(FLUID_DECLARATIONS), str(FLUID_DECLARATIONS)
    )
    if document.get("schema_version") != 1:
        raise ValueError("fluid deposits schema_version must be 1")
    entries = document.get("deposits")
    if not isinstance(entries, list) or not entries:
        raise ValueError("fluid deposits must be a non-empty array")
    material_documents, _ = vein_builder.gate_builder.material_documents()
    natural_gas = require_mapping(
        load(T11_NATURAL_GAS_MATERIAL),
        str(T11_NATURAL_GAS_MATERIAL),
    )
    if natural_gas.get("id") != "natural_gas":
        raise ValueError("T11 supplemental natural-gas material drifted")
    material_documents = {
        **material_documents,
        "natural_gas": natural_gas,
    }
    runtime_policy = require_mapping(
        load(T11_RUNTIME_POLICY), str(T11_RUNTIME_POLICY)
    )
    if (
        runtime_policy.get("schema_version") != 1
        or runtime_policy.get("status") != "DESIGN_POLICY"
    ):
        raise ValueError("T11 hydrocarbon runtime policy schema/status drifted")
    production_policy = require_mapping(
        runtime_policy.get("production"), "T11 runtime production"
    )
    deposits = [
        validate_fluid_deposit(
            entry, index, material_documents, production_policy
        )
        for index, entry in enumerate(entries)
    ]
    ids = [deposit["id"] for deposit in deposits]
    salts = [deposit["salt"] for deposit in deposits]
    if len(ids) != len(set(ids)):
        raise ValueError("fluid deposit ids must be unique")
    if len(salts) != len(set(salts)):
        raise ValueError("fluid deposit salts must be unique")
    return deposits


def configured_fluid_deposit(deposit: dict[str, Any]) -> dict[str, Any]:
    config = {
        key: deposit[key]
        for key in (
            "material",
            "min_amount_mb",
            "max_amount_mb",
            "min_y",
            "max_y",
            "search_range",
            "replaceable",
            "region_size_chunks",
            "generation_chance",
            "salt",
            "production_amount_mb",
            "production_interval_ticks",
            "accumulation_cap_mb",
            "vent_overflow",
        )
    }
    return {
        "type": FLUID_FEATURE_TYPE,
        "config": config,
    }


def build_documents() -> tuple[
    list[dict[str, Any]],
    list[dict[str, Any]],
    dict[str, str],
    dict[str, Any],
]:
    capabilities = vein_builder.material_capabilities()
    closure_veins = load_closure_veins(capabilities)
    geometry_policy = load_geometry_policy(closure_veins)
    deposits = load_fluid_deposits()
    surface_scatter = load_surface_scatter()
    t2_veins = vein_builder.load_veins()

    t2_materials = {
        entry["material"]
        for vein in t2_veins
        for layer in vein_builder.LAYERS
        for entry in vein[layer]
    }
    closure_materials = {
        entry["material"]
        for vein in closure_veins
        for layer in vein_builder.LAYERS
        for entry in vein[layer]
    }
    registered_ore_materials = {
        material
        for material, forms in capabilities.items()
        if "ore" in forms["factual"] and "ore" in forms["registered"]
    }
    if t2_materials & closure_materials:
        raise ValueError(
            "Base and closure vein material sets overlap: "
            f"{sorted(t2_materials & closure_materials)}"
        )
    if t2_materials | closure_materials != registered_ore_materials:
        raise ValueError(
            "Base + closure worldgen does not cover the registered ore domain; "
            f"missing={sorted(registered_ore_materials - t2_materials - closure_materials)}, "
            f"stale={sorted((t2_materials | closure_materials) - registered_ore_materials)}"
        )

    ids = [vein["id"] for vein in closure_veins] + [
        deposit["id"] for deposit in deposits
    ]
    salts = (
        [vein["salt"] for vein in t2_veins]
        + [vein["salt"] for vein in closure_veins]
        + [deposit["salt"] for deposit in deposits]
    )
    if len(ids) != len(set(ids)):
        raise ValueError("Catalog configured feature ids must be unique")
    if len(salts) != len(set(salts)):
        raise ValueError("Worldgen salts must be globally unique")

    files: dict[str, str] = {}
    for vein in closure_veins:
        vein_id = vein["id"]
        files[
            f"data/cruciblecraft/worldgen/configured_feature/{vein_id}.json"
        ] = stable_json(vein_builder.configured_feature(vein))
        files[
            f"data/cruciblecraft/worldgen/placed_feature/{vein_id}.json"
        ] = stable_json({
            "feature": f"cruciblecraft:{vein_id}",
            "placement": [],
        })
    for deposit in deposits:
        deposit_id = deposit["id"]
        files[
            f"data/cruciblecraft/worldgen/configured_feature/{deposit_id}.json"
        ] = stable_json(configured_fluid_deposit(deposit))
        files[
            f"data/cruciblecraft/worldgen/placed_feature/{deposit_id}.json"
        ] = stable_json({
            "feature": f"cruciblecraft:{deposit_id}",
            "placement": [],
        })
    files[
        "data/cruciblecraft/neoforge/biome_modifier/add_worldgen_catalog.json"
    ] = stable_json({
        "type": "neoforge:add_features",
        "biomes": "#minecraft:is_overworld",
        "features": [f"cruciblecraft:{identifier}" for identifier in ids],
        "step": "underground_ores",
    })

    t2_hits = sum(
        vein["generation_chance"] / vein["region_size_chunks"] ** 2
        for vein in t2_veins
    )
    closure_hits = sum(
        vein["generation_chance"] / vein["region_size_chunks"] ** 2
        for vein in closure_veins
    )
    fluid_hits = sum(
        deposit["generation_chance"]
        / deposit["region_size_chunks"] ** 2
        for deposit in deposits
    )
    readiness = {
        "schema_version": 1,
        "inputs": {
            "t2c_closure": {
                "path": "tools/gt6_ore_chain_closure.json",
                "sha256": sha256(CLOSURE_LEDGER),
            },
            "ore_declarations": {
                "path": (
                    "src/main/resources/data/cruciblecraft/"
                    "worldgen_catalog/ore_veins.json"
                ),
                "sha256": sha256(ORE_DECLARATIONS),
            },
            "t20_source_policy": {
                "path": "tools/t20_worldgen_source_policy.json",
                "sha256": sha256(T20_SOURCE_POLICY),
            },
            "fluid_declarations": {
                "path": (
                    "src/main/resources/data/cruciblecraft/"
                    "worldgen_catalog/fluid_deposits.json"
                ),
                "sha256": sha256(FLUID_DECLARATIONS),
            },
            "t11_runtime_policy": {
                "path": (
                    "src/main/resources/data/cruciblecraft/"
                    "t11_hydrocarbon_runtime_policy.json"
                ),
                "sha256": sha256(T11_RUNTIME_POLICY),
            },
            "t11_natural_gas_material": {
                "path": (
                    "src/main/resources/data/cruciblecraft/"
                    "t11_materials/natural_gas.json"
                ),
                "sha256": sha256(T11_NATURAL_GAS_MATERIAL),
            },
            "surface_scatter_declarations": {
                "path": (
                    "src/main/resources/data/cruciblecraft/"
                    "worldgen_catalog/surface_scatter.json"
                ),
                "sha256": sha256(SURFACE_SCATTER_DECLARATIONS),
            },
            "rock_prefix_definition": {
                "path": (
                    "src/main/resources/data/cruciblecraft/"
                    "material_prefixes/rock.json"
                ),
                "sha256": sha256(ROCK_PREFIX_DEFINITION),
            },
            "material_registration_gate": {
                "path": (
                    "src/main/resources/data/cruciblecraft/"
                    "material_registration_gate.json"
                ),
                "sha256": sha256(MATERIAL_REGISTRATION_GATE),
            },
            "configured_surface_runtime": {
                "path": (
                    "src/main/resources/data/cruciblecraft/worldgen/"
                    "configured_feature/surface_rock_scatter.json"
                ),
                "sha256": sha256(CONFIGURED_SURFACE_RUNTIME),
            },
            "placed_surface_runtime": {
                "path": (
                    "src/main/resources/data/cruciblecraft/worldgen/"
                    "placed_feature/surface_rock_scatter.json"
                ),
                "sha256": sha256(PLACED_SURFACE_RUNTIME),
            },
            "surface_biome_modifier_runtime": {
                "path": (
                    "src/main/resources/data/cruciblecraft/neoforge/"
                    "biome_modifier/add_surface_rocks.json"
                ),
                "sha256": sha256(BIOME_MODIFIER_SURFACE_RUNTIME),
            },
        },
        "counts": {
            "t2_vein_families": len(t2_veins),
            "t2_worldgen_materials": len(t2_materials),
            "closure_vein_classifications": len(closure_veins),
            "t20_profile_v2_veins": sum(
                vein["profile_version"] == 2 for vein in closure_veins
            ),
            "closure_configured_ore_features": len(closure_veins),
            "closure_placed_ore_features": len(closure_veins),
            "registered_ore_materials": len(registered_ore_materials),
            "ore_host_types": 2,
            "registered_ore_blocks": len(registered_ore_materials) * 2,
            "fluid_deposits": len(deposits),
            "catalog_generated_files": len(files),
            "all_worldgen_files": len(files) + (len(t2_veins) * 2 + 1),
            "unclassified": 0,
        },
        "density": {
            "t2_expected_ore_veins_per_chunk": t2_hits,
            "closure_expected_ore_veins_per_chunk": closure_hits,
            "combined_expected_ore_veins_per_chunk": t2_hits + closure_hits,
            "combined_expected_chunks_per_ore_vein": 1.0 / (t2_hits + closure_hits),
            "expected_fluid_deposits_per_chunk": fluid_hits,
            "expected_chunks_per_fluid_deposit": 1.0 / fluid_hits,
        },
        "host_policy": {
            "hosts": ["stone", "deepslate"],
            "additional_blocks_per_new_host": len(registered_ore_materials),
            "decision": "keep_two_hosts",
        },
        "geometry_policy": geometry_policy,
        "t20_fidelity": t20_worldgen_rows.row_count_summary(closure_veins),
        "closure_vein_materials": sorted(closure_materials),
        "closure_vein_feature_ids": sorted(vein["id"] for vein in closure_veins),
        "all_worldgen_ore_materials": sorted(
            t2_materials | closure_materials
        ),
        "fluid_deposits": [
            {
                key: deposit[key]
                for key in (
                    "id",
                    "material",
                    "material_state",
                    "min_amount_mb",
                    "max_amount_mb",
                    "min_y",
                    "max_y",
                    "replaceable",
                    "provenance",
                    "production_amount_mb",
                    "production_interval_ticks",
                    "accumulation_cap_mb",
                    "vent_overflow",
                )
            }
            for deposit in deposits
        ],
        "surface_scatter": {
            key: surface_scatter[key]
            for key in (
                "id",
                "feature_type",
                "configured_feature",
                "placed_feature",
                "biome_modifier_id",
                "biomes",
                "decoration_step",
                "rarity",
                "rock_tag",
                "provenance",
                "design_policy",
                "audit_mode",
            )
        },
        "surface_scatter_rock_tag_source": surface_scatter["rock_tag_source"],
    }
    return closure_veins, deposits, files, readiness


def check_outputs(files: dict[str, str]) -> list[str]:
    actual = (
        {
            path.relative_to(OUTPUT_RESOURCE_ROOT).as_posix(): path
            for path in OUTPUT_RESOURCE_ROOT.rglob("*.json")
        }
        if OUTPUT_RESOURCE_ROOT.is_dir()
        else {}
    )
    errors = sorted(set(files) ^ set(actual))
    for relative in sorted(set(files) & set(actual)):
        if actual[relative].read_text(encoding="utf-8") != files[relative]:
            errors.append(relative)
    return errors


def write_outputs(files: dict[str, str]) -> None:
    if OUTPUT_RESOURCE_ROOT.exists():
        shutil.rmtree(OUTPUT_RESOURCE_ROOT)
    for relative, content in files.items():
        path = OUTPUT_RESOURCE_ROOT / relative
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(content.encode("utf-8"))


def main() -> int:
    parser = argparse.ArgumentParser()
    modes = parser.add_mutually_exclusive_group(required=True)
    modes.add_argument("--write", action="store_true")
    modes.add_argument("--check", action="store_true")
    modes.add_argument("--review", action="store_true")
    args = parser.parse_args()
    try:
        _, _, files, readiness = build_documents()
    except (OSError, ValueError, json.JSONDecodeError) as error:
        print(f"Worldgen catalog build failed: {error}", file=sys.stderr)
        return 1
    expected_readiness = stable_json(readiness, compact=True)
    if args.review:
        print(
            stable_json({
                "counts": readiness["counts"],
                "density": readiness["density"],
                "host_policy": readiness["host_policy"],
                "geometry_policy": readiness["geometry_policy"],
                "fluid_deposits": readiness["fluid_deposits"],
            }),
            end="",
        )
        return 0
    if args.write:
        write_outputs(files)
        READINESS.write_bytes(expected_readiness.encode("utf-8"))
        print(
            f"Wrote {len(files)} worldgen catalog files and {READINESS}"
        )
        return 0
    errors = check_outputs(files)
    if (
        not READINESS.is_file()
        or READINESS.read_text(encoding="utf-8") != expected_readiness
    ):
        errors.append(READINESS.relative_to(ROOT).as_posix())
    if errors:
        print(
            "Worldgen catalog outputs are stale:\n"
            + "\n".join(f"- {error}" for error in errors),
            file=sys.stderr,
        )
        return 1
    print("Worldgen catalog outputs and readiness are current.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
