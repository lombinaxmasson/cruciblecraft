#!/usr/bin/env python3
"""Build the T35 expected CC runtime registry census.

Derives deterministic static expected content identities from Java registry
sources and canonical data files only. No census business logic belongs in
production Java; the slim gate fixture is the Java GameTest expected set.
"""
from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from tools import t27_common as common  # noqa: E402

TOOLS = common.TOOLS
POLICY = TOOLS / "t35_census_policy.json"
OUTPUT = TOOLS / "t35_runtime_registry.json"
GATE_FIXTURE = (
    ROOT
    / "src/main/resources/census/t35_runtime_registry_gate.json"
)
BUILDER = Path(__file__).resolve()
NAMESPACE = "cruciblecraft"

JAVA_SOURCES = {
    "blocks": ROOT / "src/main/java/com/masson/cruciblecraft/registry/ModBlocks.java",
    "items": ROOT / "src/main/java/com/masson/cruciblecraft/registry/ModItems.java",
    "fluids": ROOT / "src/main/java/com/masson/cruciblecraft/registry/ModFluids.java",
    "fluid_types": ROOT / "src/main/java/com/masson/cruciblecraft/registry/ModFluids.java",
    "block_entity_types": ROOT
    / "src/main/java/com/masson/cruciblecraft/registry/ModBlockEntities.java",
    "menu_types": ROOT / "src/main/java/com/masson/cruciblecraft/registry/ModMenus.java",
    "recipe_types": ROOT / "src/main/java/com/masson/cruciblecraft/registry/ModRecipes.java",
    "recipe_serializers": ROOT
    / "src/main/java/com/masson/cruciblecraft/registry/ModRecipes.java",
    "recipe_maps": ROOT
    / "src/main/java/com/masson/cruciblecraft/registry/ModRecipeMaps.java",
    "worldgen_features": ROOT
    / "src/main/java/com/masson/cruciblecraft/registry/ModFeatures.java",
    "creative_tabs": ROOT
    / "src/main/java/com/masson/cruciblecraft/registry/ModCreativeTabs.java",
    "cover_behaviors": ROOT
    / "src/main/java/com/masson/cruciblecraft/logistics/pipe/cover/CoverBehaviorRegistry.java",
    "processing_machines": ROOT
    / "src/main/java/com/masson/cruciblecraft/registry/ModProcessingMachines.java",
    "material_creative_tabs": ROOT
    / "src/main/java/com/masson/cruciblecraft/registry/MaterialCreativeTab.java",
    "extruder_shapes": ROOT
    / "src/main/java/com/masson/cruciblecraft/content/item/ExtruderShapeCatalog.java",
    "tool_patterns": ROOT
    / "src/main/java/com/masson/cruciblecraft/content/item/ToolPatternCatalog.java",
}

DATA_SOURCES = {
    "machine_tiers": ROOT
    / "src/main/resources/data/cruciblecraft/machine_tiers.json",
    "hopper_variants": ROOT
    / "src/main/resources/data/cruciblecraft/hopper_variants.json",
    "material_gate": ROOT
    / "src/main/resources/data/cruciblecraft/material_registration_gate.json",
    "material_index": ROOT
    / "src/main/resources/data/cruciblecraft/materials/index.json",
    "t11_material_index": ROOT
    / "src/main/resources/data/cruciblecraft/t11_materials/index.json",
    "multiblock_plugins": ROOT
    / "src/main/resources/data/cruciblecraft/multiblock_plugins.json",
    "multiblock_structures_dir": ROOT
    / "src/main/resources/data/cruciblecraft/multiblock_structures",
    "configured_features_dirs": [
        ROOT
        / "src/main/resources/data/cruciblecraft/worldgen/configured_feature",
        ROOT
        / "src/worldgen_catalog_generated/resources/data/cruciblecraft/worldgen/configured_feature",
        ROOT
        / "src/worldgen_generated/resources/data/cruciblecraft/worldgen/configured_feature",
    ],
    "placed_features_dirs": [
        ROOT
        / "src/main/resources/data/cruciblecraft/worldgen/placed_feature",
        ROOT
        / "src/worldgen_catalog_generated/resources/data/cruciblecraft/worldgen/placed_feature",
        ROOT
        / "src/worldgen_generated/resources/data/cruciblecraft/worldgen/placed_feature",
    ],
}

REGISTER_LITERAL = re.compile(
    r"""\.register(?:SimpleBlockItem|SimpleBlock|SimpleItem)?\(\s*["']([^"']+)["']"""
)
FLUID_TYPE_REGISTER = re.compile(r"""FLUID_TYPES\.register\(\s*["']([^"']+)["']""")
FLUID_REGISTER = re.compile(r"""FLUIDS\.register\(\s*["']([^"']+)["']""")
PROCESSING_BLOCK = re.compile(r"""processing\(\s*["']([^"']+)["']""")
CATALOG_ENTRY = re.compile(r"""(?:shape|pattern)\(\s*["']([^"']+)["']""")
MOLD_ITEM = re.compile(r"""\bmold\(\s*["']([^"']+)["']""")
MATERIAL_CREATIVE_TAB = re.compile(
    r"""^\s*[A-Z_]+\(["']([^"']+)["']\)[,;]""",
    re.MULTILINE,
)
CREATE_RECIPE_MAP = re.compile(r"""create\(\s*["']([^"']+)["']\s*\)""")
REGISTER_BUILTIN = re.compile(r"""registerBuiltin\(\s*["']([^"']+)["']""")
PROCESSING_MENU = re.compile(
    r"""processing\(\s*["']([^"']+)["']\s*,\s*ModProcessingMachines"""
)

ELECTRICAL_FORM_TO_SPEC = {
    "wire": "wireGt01",
    "cable": "cableGt01",
    "double_cable": "cableGt02",
    "quadruple_cable": "cableGt04",
    "octuple_cable": "cableGt08",
    "dodecuple_cable": "cableGt12",
}
FLUID_PIPE_FORM_TO_SPEC = {
    "tiny_fluid_pipe": "pipeTiny",
    "small_fluid_pipe": "pipeSmall",
    "fluid_pipe": "pipeMedium",
    "large_fluid_pipe": "pipeLarge",
    "huge_fluid_pipe": "pipeHuge",
}
ITEM_PIPE_FORM_TO_SPEC = {
    "item_pipe": "pipeMedium",
    "large_item_pipe": "pipeLarge",
    "huge_item_pipe": "pipeHuge",
}
ORE_HOSTS = ("stone", "deepslate")
HOPPER_KIND_SUFFIXES = ("hopper", "queue_hopper")
CHEMICAL_FLUID_GATES = (
    ROOT
    / "src/t5_chemical_generated/resources/data/cruciblecraft/t5_chemical_fluid_gate.json",
    ROOT
    / "src/main/resources/data/cruciblecraft/t10_container_fluid_gate.json",
    ROOT
    / "src/main/resources/data/cruciblecraft/t11_hydrocarbon_fluid_gate.json",
)
MATERIAL_INDEXES = (
    DATA_SOURCES["material_index"],
    DATA_SOURCES["t11_material_index"],
)


def _rl(path: str) -> str:
    if ":" in path:
        return path
    return f"{NAMESPACE}:{path}"


def _record(
    category: str,
    ident: str,
    *,
    identity_kind: str,
    source_kind: str,
    evidence: list[str],
) -> dict[str, Any]:
    return {
        "category": category,
        "id": _rl(ident),
        "identity_kind": identity_kind,
        "source": {
            "kind": source_kind,
            "evidence": sorted(evidence),
        },
    }


def _require_file(path: Path) -> None:
    if not path.is_file():
        raise FileNotFoundError(common.relative(path))


def _require_dir(path: Path) -> None:
    if not path.is_dir():
        raise FileNotFoundError(common.relative(path))


def _parse_java_literals(path: Path, pattern: re.Pattern[str]) -> list[str]:
    _require_file(path)
    return sorted(set(pattern.findall(path.read_text(encoding="utf-8"))))


def _static_java_records(category: str, path: Path) -> list[dict[str, Any]]:
    rel = common.relative(path)
    if category == "recipe_maps":
        ids = _parse_java_literals(path, CREATE_RECIPE_MAP)
    elif category == "cover_behaviors":
        ids = _parse_java_literals(path, REGISTER_BUILTIN)
    elif category == "fluid_types":
        ids = _parse_java_literals(path, FLUID_TYPE_REGISTER)
    elif category == "fluids":
        ids = [
            ident
            for ident in _parse_java_literals(path, FLUID_REGISTER)
            if ident != "flowing_"
        ]
    elif category == "menu_types":
        text = path.read_text(encoding="utf-8")
        ids = sorted(
            set(REGISTER_LITERAL.findall(text))
            | set(PROCESSING_MENU.findall(text))
        )
    else:
        ids = _parse_java_literals(path, REGISTER_LITERAL)
    return [
        _record(
            category,
            ident,
            identity_kind="static_one_off",
            source_kind="java_registry",
            evidence=[rel],
        )
        for ident in ids
    ]


def _hopper_block_records(document: dict[str, Any], evidence: str) -> list[dict[str, Any]]:
    variants = document.get("variants") or []
    if len(variants) != 60:
        raise ValueError("hopper_variants must contain exactly 60 base rows")
    records: list[dict[str, Any]] = []
    for row in variants:
        material = str(row.get("material") or "")
        if not material.startswith(f"{NAMESPACE}:"):
            raise ValueError(f"invalid hopper material id: {material}")
        material_path = material.split(":", 1)[1]
        for suffix in HOPPER_KIND_SUFFIXES:
            ident = f"{material_path}_{suffix}"
            records.append(
                _record(
                    "blocks",
                    ident,
                    identity_kind="generated_catalog",
                    source_kind="data_catalog",
                    evidence=[evidence, "ModBlocks.registerHopperBlocks"],
                )
            )
    if len(records) != 120:
        raise ValueError(f"hopper projection must be 120 blocks, got {len(records)}")
    return records


def _hopper_item_records(document: dict[str, Any], evidence: str) -> list[dict[str, Any]]:
    variants = document.get("variants") or []
    if len(variants) != 60:
        raise ValueError("hopper_variants must contain exactly 60 base rows")
    records: list[dict[str, Any]] = []
    for row in variants:
        material = str(row.get("material") or "")
        if not material.startswith(f"{NAMESPACE}:"):
            raise ValueError(f"invalid hopper material id: {material}")
        material_path = material.split(":", 1)[1]
        for suffix in HOPPER_KIND_SUFFIXES:
            records.append(
                _record(
                    "items",
                    f"{material_path}_{suffix}",
                    identity_kind="generated_catalog",
                    source_kind="data_catalog",
                    evidence=[evidence, "ModItems.registerHopperItems"],
                )
            )
    if len(records) != 120:
        raise ValueError(f"hopper projection must be 120 items, got {len(records)}")
    return records


def _processing_block_records(path: Path) -> list[dict[str, Any]]:
    return [
        _record(
            "blocks",
            ident,
            identity_kind="static_one_off",
            source_kind="java_registry",
            evidence=[common.relative(path), "ModBlocks.processing"],
        )
        for ident in _parse_java_literals(path, PROCESSING_BLOCK)
    ]


def _material_creative_tab_records(path: Path) -> list[dict[str, Any]]:
    return [
        _record(
            "creative_tabs",
            ident,
            identity_kind="static_one_off",
            source_kind="java_registry",
            evidence=[common.relative(path), "MaterialCreativeTab"],
        )
        for ident in _parse_java_literals(path, MATERIAL_CREATIVE_TAB)
    ]


def _machine_tier_block_records(
    document: dict[str, Any], evidence: str
) -> list[dict[str, Any]]:
    records: list[dict[str, Any]] = []
    for row in document.get("variants") or []:
        ident = str(row.get("id") or "")
        if not ident.startswith(f"{NAMESPACE}:"):
            raise ValueError(f"invalid machine tier id: {ident}")
        path = ident.split(":", 1)[1]
        records.append(
            _record(
                "blocks",
                path,
                identity_kind="source_backed_variant",
                source_kind="data_catalog",
                evidence=[evidence, "ModBlocks.tieredProcessing"],
            )
        )
    return records


def _machine_tier_item_records(
    document: dict[str, Any], evidence: str
) -> list[dict[str, Any]]:
    records: list[dict[str, Any]] = []
    for row in document.get("variants") or []:
        ident = str(row.get("id") or "")
        if not ident.startswith(f"{NAMESPACE}:"):
            raise ValueError(f"invalid machine tier id: {ident}")
        records.append(
            _record(
                "items",
                ident.split(":", 1)[1],
                identity_kind="source_backed_variant",
                source_kind="data_catalog",
                evidence=[evidence, "ModItems.registerTieredProcessingItems"],
            )
        )
    if len(records) != 33:
        raise ValueError(f"machine tier item projection must be 33 items, got {len(records)}")
    return records


def _catalog_item_records(
    path: Path,
    prefix: str,
    source: str,
) -> list[dict[str, Any]]:
    return [
        _record(
            "items",
            f"{prefix}{ident}",
            identity_kind="static_one_off",
            source_kind="java_registry",
            evidence=[common.relative(path), source],
        )
        for ident in _parse_java_literals(path, CATALOG_ENTRY)
    ]


def _mold_item_records(path: Path) -> list[dict[str, Any]]:
    return [
        _record(
            "items",
            ident,
            identity_kind="static_one_off",
            source_kind="java_registry",
            evidence=[common.relative(path), "ModItems.mold"],
        )
        for ident in _parse_java_literals(path, MOLD_ITEM)
    ]


def _load_materials(index_paths: tuple[Path, ...]) -> dict[str, dict[str, Any]]:
    materials: dict[str, dict[str, Any]] = {}
    for index_path in index_paths:
        index = common.load_json(index_path)
        if not isinstance(index, list):
            raise ValueError(f"{common.relative(index_path)} must be a list")
        root = index_path.parent
        for filename in index:
            path = root / filename
            document = common.load_json(path)
            material_id = str(document.get("id") or "")
            if filename != f"{material_id}.json":
                raise ValueError(f"material filename/id mismatch: {filename}")
            if material_id in materials:
                raise ValueError(f"duplicate material id across indexes: {material_id}")
            materials[material_id] = document
    return materials


def _material_registry_name(material_id: str, form: str) -> str:
    return f"{material_id}/{form}"


def _ore_registry_name(material_id: str, host: str) -> str:
    if host == "deepslate":
        return f"deepslate_{material_id}_ore"
    return f"{material_id}_ore"


def _material_records(
    gate: dict[str, Any],
    materials: dict[str, dict[str, Any]],
    gate_evidence: str,
    index_evidence: str,
    chemical_fluid_gates: tuple[Path, ...],
) -> dict[str, list[dict[str, Any]]]:
    gate_materials = gate.get("materials") or {}
    blocks: list[dict[str, Any]] = []
    items: list[dict[str, Any]] = []
    fluids: list[dict[str, Any]] = []
    fluid_types: list[dict[str, Any]] = []
    for material_id in sorted(gate_materials):
        if material_id not in materials:
            raise ValueError(f"gate material missing JSON: {material_id}")
        document = materials[material_id]
        forms = gate_materials[material_id]
        if not isinstance(forms, list):
            raise ValueError(f"gate forms must be a list for {material_id}")
        metadata = document.get("gt6_metadata") or {}
        electrical = metadata.get("electrical_by_specification") or {}
        pipe_properties = metadata.get("pipe_properties") or {}
        fluid_specs = pipe_properties.get("fluid_by_specification") or {}
        item_specs = pipe_properties.get("item_by_specification") or {}
        form_items = document.get("form_items") or {}
        molten = bool(document.get("molten_fluid"))
        for form in sorted(set(forms)):
            evidence = [gate_evidence, index_evidence, f"materials/{material_id}.json"]
            registry = _material_registry_name(material_id, form)
            spec = ELECTRICAL_FORM_TO_SPEC.get(form)
            if spec and spec in electrical:
                blocks.append(
                    _record(
                        "blocks",
                        registry,
                        identity_kind="generated_material_form",
                        source_kind="material_gate",
                        evidence=[*evidence, "ElectricalConductorCatalog"],
                    )
                )
                if form not in form_items:
                    items.append(
                        _record(
                            "items",
                            registry,
                            identity_kind="generated_material_form",
                            source_kind="material_gate",
                            evidence=[*evidence, "ModItems.registerMaterials"],
                        )
                    )
                continue
            fluid_spec = FLUID_PIPE_FORM_TO_SPEC.get(form)
            if fluid_spec and fluid_spec in fluid_specs:
                blocks.append(
                    _record(
                        "blocks",
                        registry,
                        identity_kind="generated_material_form",
                        source_kind="material_gate",
                        evidence=[*evidence, "PipeCatalog.FLUID"],
                    )
                )
                if form not in form_items:
                    items.append(
                        _record(
                            "items",
                            registry,
                            identity_kind="generated_material_form",
                            source_kind="material_gate",
                            evidence=[*evidence, "ModItems.registerMaterials"],
                        )
                    )
                continue
            item_spec = ITEM_PIPE_FORM_TO_SPEC.get(form)
            if item_spec and item_spec in item_specs:
                blocks.append(
                    _record(
                        "blocks",
                        registry,
                        identity_kind="generated_material_form",
                        source_kind="material_gate",
                        evidence=[*evidence, "PipeCatalog.ITEM"],
                    )
                )
                if form not in form_items:
                    items.append(
                        _record(
                            "items",
                            registry,
                            identity_kind="generated_material_form",
                            source_kind="material_gate",
                            evidence=[*evidence, "ModItems.registerMaterials"],
                        )
                    )
                continue
            if form == "block" and "block" in forms and "block" not in form_items:
                blocks.append(
                    _record(
                        "blocks",
                        registry,
                        identity_kind="generated_material_form",
                        source_kind="material_gate",
                        evidence=[*evidence, "ModBlocks.registerMaterials"],
                    )
                )
                items.append(
                    _record(
                        "items",
                        registry,
                        identity_kind="generated_material_form",
                        source_kind="material_gate",
                        evidence=[*evidence, "ModItems.registerMaterials"],
                    )
                )
                continue
            if form == "rock" and "rock" in forms and "rock" not in form_items:
                blocks.append(
                    _record(
                        "blocks",
                        registry,
                        identity_kind="generated_material_form",
                        source_kind="material_gate",
                        evidence=[*evidence, "ModBlocks.registerMaterials"],
                    )
                )
                items.append(
                    _record(
                        "items",
                        registry,
                        identity_kind="generated_material_form",
                        source_kind="material_gate",
                        evidence=[*evidence, "ModItems.registerMaterials"],
                    )
                )
                continue
            if form == "ore":
                for host in ORE_HOSTS:
                    ore_id = _ore_registry_name(material_id, host)
                    blocks.append(
                        _record(
                            "blocks",
                            ore_id,
                            identity_kind="generated_material_form",
                            source_kind="material_gate",
                            evidence=[*evidence, "ModBlocks.registerMaterials"],
                        )
                    )
                    items.append(
                        _record(
                            "items",
                            ore_id,
                            identity_kind="generated_material_form",
                            source_kind="material_gate",
                            evidence=[*evidence, "ModItems.registerMaterials"],
                        )
                    )
                continue
            if form in form_items:
                continue
            items.append(
                _record(
                    "items",
                    registry,
                    identity_kind="generated_material_form",
                    source_kind="material_gate",
                    evidence=[*evidence, "ModItems.registerMaterials"],
                )
            )
        if molten:
            source_id = f"molten_{material_id}"
            flowing_id = f"flowing_{source_id}"
            evidence = [gate_evidence, index_evidence, f"materials/{material_id}.json"]
            fluid_types.extend(
                [
                    _record(
                        "fluid_types",
                        source_id,
                        identity_kind="generated_material_form",
                        source_kind="material_gate",
                        evidence=[*evidence, "ModFluids.registerMaterials"],
                    )
                ]
            )
            fluids.extend(
                [
                    _record(
                        "fluids",
                        source_id,
                        identity_kind="generated_material_form",
                        source_kind="material_gate",
                        evidence=[*evidence, "ModFluids.registerMaterials"],
                    ),
                    _record(
                        "fluids",
                        flowing_id,
                        identity_kind="generated_material_form",
                        source_kind="material_gate",
                        evidence=[*evidence, "ModFluids.registerMaterials"],
                    ),
                ]
            )
    seen_chemical_ids: set[str] = set()
    for chemical_gate in chemical_fluid_gates:
        document = common.load_json(chemical_gate)
        rows = document.get("fluids")
        if not isinstance(rows, list):
            raise ValueError(f"{common.relative(chemical_gate)} fluids must be a list")
        for entry in rows:
            if not isinstance(entry, dict):
                raise ValueError("chemical fluid entries must be objects")
            ident = str(entry.get("id") or "")
            material_id = str(entry.get("material") or "")
            if not ident or not material_id:
                raise ValueError("chemical fluid id and material are required")
            if material_id not in materials:
                raise ValueError(f"chemical fluid references unknown material: {material_id}")
            if ident in seen_chemical_ids:
                raise ValueError(f"duplicate chemical fluid id: {ident}")
            seen_chemical_ids.add(ident)
            evidence = [
                common.relative(chemical_gate),
                f"materials/{material_id}.json",
                "ChemicalFluidRegistrationGate",
            ]
            fluid_types.append(
                _record(
                    "fluid_types",
                    ident,
                    identity_kind="generated_material_form",
                    source_kind="chemical_fluid_gate",
                    evidence=evidence,
                )
            )
            fluids.extend(
                [
                    _record(
                        "fluids",
                        ident,
                        identity_kind="generated_material_form",
                        source_kind="chemical_fluid_gate",
                        evidence=evidence,
                    ),
                    _record(
                        "fluids",
                        f"flowing_{ident}",
                        identity_kind="generated_material_form",
                        source_kind="chemical_fluid_gate",
                        evidence=evidence,
                    ),
                ]
            )
    return {
        "blocks": blocks,
        "items": items,
        "fluids": fluids,
        "fluid_types": fluid_types,
    }


def _json_stem_records(
    category: str,
    directory: Path,
    *,
    identity_kind: str,
    source_kind: str,
) -> list[dict[str, Any]]:
    return _json_stem_records_from_dirs(
        category,
        [directory],
        identity_kind=identity_kind,
        source_kind=source_kind,
    )


def _json_stem_records_from_dirs(
    category: str,
    directories: list[Path],
    *,
    identity_kind: str,
    source_kind: str,
) -> list[dict[str, Any]]:
    records: list[dict[str, Any]] = []
    for directory in directories:
        _require_dir(directory)
        for path in sorted(directory.glob("*.json")):
            records.append(
                _record(
                    category,
                    path.stem,
                    identity_kind=identity_kind,
                    source_kind=source_kind,
                    evidence=[common.relative(path)],
                )
            )
    return records


def _multiblock_plugin_records(document: dict[str, Any], evidence: str) -> list[dict[str, Any]]:
    records: list[dict[str, Any]] = []
    for row in document.get("plugins") or []:
        ident = str(row.get("id") or "")
        if not ident.startswith(f"{NAMESPACE}:"):
            raise ValueError(f"invalid multiblock plugin id: {ident}")
        records.append(
            _record(
                "multiblock_plugins",
                ident.split(":", 1)[1],
                identity_kind="static_one_off",
                source_kind="data_catalog",
                evidence=[evidence, "MultiblockControllerPluginRegistry"],
            )
        )
    return records


def _dedupe_records(records: list[dict[str, Any]]) -> list[dict[str, Any]]:
    seen: dict[str, dict[str, Any]] = {}
    for record in records:
        key = f"{record['category']}::{record['id']}"
        if key in seen:
            merged = seen[key]
            merged_evidence = sorted(
                set(merged["source"]["evidence"]) | set(record["source"]["evidence"])
            )
            merged["source"]["evidence"] = merged_evidence
            continue
        seen[key] = record
    return [seen[key] for key in sorted(seen)]


def _source_hashes() -> dict[str, str]:
    paths = [
        POLICY,
        BUILDER,
        *JAVA_SOURCES.values(),
        DATA_SOURCES["machine_tiers"],
        DATA_SOURCES["hopper_variants"],
        DATA_SOURCES["material_gate"],
        *MATERIAL_INDEXES,
        DATA_SOURCES["multiblock_plugins"],
        *CHEMICAL_FLUID_GATES,
    ]
    for material_index_path in MATERIAL_INDEXES:
        material_index = common.load_json(material_index_path)
        if not isinstance(material_index, list):
            raise ValueError(f"{common.relative(material_index_path)} must be a list")
        material_root = material_index_path.parent
        for filename in material_index:
            if not isinstance(filename, str) or not filename:
                raise ValueError("material index entries must be non-empty strings")
            paths.append(material_root / filename)
    paths.extend(
        sorted(DATA_SOURCES["multiblock_structures_dir"].glob("*.json"))
    )
    for directory in (
        *DATA_SOURCES["configured_features_dirs"],
        *DATA_SOURCES["placed_features_dirs"],
    ):
        paths.extend(sorted(directory.glob("*.json")))
    return {common.relative(path): common.sha256_file(path) for path in paths}


def build() -> dict[str, Any]:
    policy = common.load_json(POLICY)
    for path in (
        POLICY,
        *JAVA_SOURCES.values(),
        DATA_SOURCES["machine_tiers"],
        DATA_SOURCES["hopper_variants"],
        DATA_SOURCES["material_gate"],
        *MATERIAL_INDEXES,
        DATA_SOURCES["multiblock_plugins"],
        *CHEMICAL_FLUID_GATES,
    ):
        _require_file(path)
    _require_dir(DATA_SOURCES["multiblock_structures_dir"])
    for directory in (
        *DATA_SOURCES["configured_features_dirs"],
        *DATA_SOURCES["placed_features_dirs"],
    ):
        _require_dir(directory)
    registry_policy = policy["runtime_registry_policy"]
    categories: dict[str, list[dict[str, Any]]] = {
        name: [] for name in registry_policy["categories"]
    }

    static_map = {
        "blocks": JAVA_SOURCES["blocks"],
        "items": JAVA_SOURCES["items"],
        "fluids": JAVA_SOURCES["fluids"],
        "fluid_types": JAVA_SOURCES["fluid_types"],
        "block_entity_types": JAVA_SOURCES["block_entity_types"],
        "menu_types": JAVA_SOURCES["menu_types"],
        "recipe_types": JAVA_SOURCES["recipe_types"],
        "recipe_serializers": JAVA_SOURCES["recipe_serializers"],
        "recipe_maps": JAVA_SOURCES["recipe_maps"],
        "worldgen_features": JAVA_SOURCES["worldgen_features"],
        "creative_tabs": JAVA_SOURCES["creative_tabs"],
        "cover_behaviors": JAVA_SOURCES["cover_behaviors"],
    }
    for category, path in static_map.items():
        categories[category].extend(_static_java_records(category, path))

    machine_tiers = common.load_json(DATA_SOURCES["machine_tiers"])
    hopper_variants = common.load_json(DATA_SOURCES["hopper_variants"])
    gate = common.load_json(DATA_SOURCES["material_gate"])
    materials = _load_materials(MATERIAL_INDEXES)
    plugins = common.load_json(DATA_SOURCES["multiblock_plugins"])

    categories["blocks"].extend(
        _hopper_block_records(
            hopper_variants,
            common.relative(DATA_SOURCES["hopper_variants"]),
        )
    )
    categories["items"].extend(
        _hopper_item_records(
            hopper_variants,
            common.relative(DATA_SOURCES["hopper_variants"]),
        )
    )
    categories["blocks"].extend(_processing_block_records(JAVA_SOURCES["blocks"]))
    categories["creative_tabs"].extend(
        _material_creative_tab_records(JAVA_SOURCES["material_creative_tabs"])
    )
    categories["items"].extend(
        _catalog_item_records(
            JAVA_SOURCES["extruder_shapes"],
            "extruder_shape_",
            "ExtruderShapeCatalog",
        )
    )
    categories["items"].extend(
        _catalog_item_records(
            JAVA_SOURCES["tool_patterns"],
            "tool_pattern_",
            "ToolPatternCatalog",
        )
    )
    categories["items"].extend(_mold_item_records(JAVA_SOURCES["items"]))
    categories["blocks"].extend(
        _machine_tier_block_records(
            machine_tiers,
            common.relative(DATA_SOURCES["machine_tiers"]),
        )
    )
    categories["items"].extend(
        _machine_tier_item_records(
            machine_tiers,
            common.relative(DATA_SOURCES["machine_tiers"]),
        )
    )
    material_records = _material_records(
        gate,
        materials,
        common.relative(DATA_SOURCES["material_gate"]),
        common.relative(DATA_SOURCES["material_index"]),
        CHEMICAL_FLUID_GATES,
    )
    for key, rows in material_records.items():
        categories[key].extend(rows)

    categories["multiblock_structures"].extend(
        _json_stem_records(
            "multiblock_structures",
            DATA_SOURCES["multiblock_structures_dir"],
            identity_kind="static_one_off",
            source_kind="data_catalog",
        )
    )
    categories["multiblock_plugins"].extend(
        _multiblock_plugin_records(
            plugins,
            common.relative(DATA_SOURCES["multiblock_plugins"]),
        )
    )
    categories["configured_features"].extend(
        _json_stem_records_from_dirs(
            "configured_features",
            DATA_SOURCES["configured_features_dirs"],
            identity_kind="data_driven",
            source_kind="worldgen_data",
        )
    )
    categories["placed_features"].extend(
        _json_stem_records_from_dirs(
            "placed_features",
            DATA_SOURCES["placed_features_dirs"],
            identity_kind="data_driven",
            source_kind="worldgen_data",
        )
    )

    normalized: dict[str, list[dict[str, Any]]] = {}
    counts_by_category: dict[str, int] = {}
    for name in registry_policy["categories"]:
        rows = _dedupe_records(categories[name])
        rows.sort(key=lambda row: row["id"])
        normalized[name] = rows
        counts_by_category[name] = len(rows)

    total = sum(counts_by_category.values())
    return {
        "schema_version": 1,
        "status": "T35_RUNTIME_REGISTRY_EXPECTED",
        "source_revision": common.SOURCE_REVISION,
        "generated_by": "python tools/build_t35_runtime_registry.py",
        "policy": common.relative(POLICY),
        "policy_sha256": common.sha256_file(POLICY),
        "currentness": {
            "owned_inputs": {
                common.relative(BUILDER): common.sha256_file(BUILDER),
                common.relative(POLICY): common.sha256_file(POLICY),
            },
            "source_hashes": _source_hashes(),
        },
        "java_runtime_gate": {
            "compatible_schema_version": registry_policy[
                "java_runtime_gate_schema_version"
            ],
            "namespace": registry_policy["namespace"],
            "equality_mode": "bidirectional_set",
            "expected_categories": registry_policy["categories"],
            "instructions": (
                "After registry bootstrap, enumerate the cruciblecraft namespace "
                "for each category and require exact bidirectional set equality "
                "with categories[].records[].id. Extra, missing, or duplicate "
                "runtime ids must fail closed."
            ),
        },
        "categories": normalized,
        "counts_by_category": counts_by_category,
        "total_expected_ids": total,
    }


def _gate_fixture(
    document: dict[str, Any],
    *,
    full_artifact_sha256: str | None = None,
) -> dict[str, Any]:
    gate = document["java_runtime_gate"]
    categories = {
        name: [row["id"] for row in document["categories"][name]]
        for name in gate["expected_categories"]
    }
    artifact_hash = full_artifact_sha256 or common.sha256_bytes(
        common.stable_json(document).encode("utf-8")
    )
    return {
        "schema_version": 1,
        "compatible_schema_version": gate["compatible_schema_version"],
        "namespace": gate["namespace"],
        "full_artifact_sha256": artifact_hash,
        "total_expected_ids": document["total_expected_ids"],
        "counts_by_category": document["counts_by_category"],
        "categories": categories,
    }


def check() -> list[str]:
    errors: list[str] = []
    if not OUTPUT.is_file():
        return [f"missing generated file: {common.relative(OUTPUT)}"]
    frozen = common.load_json(OUTPUT)
    if frozen.get("total_expected_ids") != 20_553:
        errors.append("frozen T35 runtime registry id count drifted")
    if frozen.get("status") != "T35_RUNTIME_REGISTRY_EXPECTED":
        errors.append("frozen T35 runtime registry status drifted")
    if not GATE_FIXTURE.is_file():
        errors.append(f"missing generated file: {common.relative(GATE_FIXTURE)}")
    else:
        fixture = common.load_json(GATE_FIXTURE)
        if fixture.get("total_expected_ids") != frozen.get("total_expected_ids"):
            errors.append("T35 runtime registry gate fixture count drifted")
    return errors


def write() -> dict[str, Any]:
    document = build()
    common.write_stable(OUTPUT, document)
    artifact_hash = common.sha256_bytes(OUTPUT.read_bytes())
    common.write_stable(
        GATE_FIXTURE,
        _gate_fixture(document, full_artifact_sha256=artifact_hash),
    )
    return document


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--write", action="store_true")
    args = parser.parse_args(argv)
    if args.check == args.write:
        parser.error("choose exactly one of --check or --write")
    try:
        if args.check:
            errors = check()
            if errors:
                raise ValueError("; ".join(errors))
            document = common.load_json(OUTPUT)
        else:
            document = write()
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"T35 runtime registry failed: {error}", file=sys.stderr)
        return 1
    summary = {
        "status": document.get("status"),
        "total_expected_ids": document.get("total_expected_ids"),
        "counts_by_category": document.get("counts_by_category"),
    }
    print(json.dumps(summary, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
