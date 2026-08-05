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

ORE_DECLARATIONS = (
    ROOT
    / "src/main/resources/data/cruciblecraft/worldgen_catalog/ore_veins.json"
)
FLUID_DECLARATIONS = (
    ROOT
    / "src/main/resources/data/cruciblecraft/worldgen_catalog/fluid_deposits.json"
)
CLOSURE_LEDGER = TOOLS / "gt6_ore_chain_closure.json"
OUTPUT_RESOURCE_ROOT = ROOT / "src/worldgen_catalog_generated/resources"
READINESS = TOOLS / "worldgen_catalog_readiness.json"
RESOURCE_PATH = re.compile(r"[a-z0-9_.-]+")
RESOURCE_LOCATION = re.compile(r"([a-z0-9_.-]+):([a-z0-9_./-]+)")
FLUID_FEATURE_TYPE = "cruciblecraft:subsurface_fluid_deposit"


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
    if document.get("schema_version") != 1:
        raise ValueError("Closure ore declarations schema_version must be 1")
    if document.get("id") != "ore_vein_closure":
        raise ValueError(
            "Closure ore declarations id must be ore_vein_closure"
        )
    source_ledger = require_mapping(
        document.get("source_ledger"), "source_ledger"
    )
    if (
        source_ledger.get("path")
        != "tools/gt6_ore_chain_closure.json"
        or source_ledger.get("classification") != "vein"
    ):
        raise ValueError("Closure ore declarations must name the fixed T2c ledger")
    defaults = require_mapping(document.get("defaults"), "defaults")
    materials = document.get("materials")
    if not isinstance(materials, list) or not materials:
        raise ValueError("Closure ore declarations materials must be non-empty")
    if any(
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

    settings = {
        "min_y": require_int(defaults, "min_y", -64, 320, "defaults"),
        "max_y": require_int(defaults, "max_y", -64, 320, "defaults"),
        "horizontal_radius": require_int(
            defaults,
            "horizontal_radius",
            4,
            vein_builder.MAX_SAFE_HORIZONTAL_RADIUS,
            "defaults",
        ),
        "vertical_radius": require_int(
            defaults, "vertical_radius", 2, 24, "defaults"
        ),
        "density": require_number(
            defaults, "density", 0.01, 1.0, "defaults"
        ),
        "region_size_chunks": require_int(
            defaults, "region_size_chunks", 2, 32, "defaults"
        ),
        "generation_chance": require_number(
            defaults, "generation_chance", 0.0, 1.0, "defaults"
        ),
    }
    if settings["min_y"] > settings["max_y"]:
        raise ValueError("defaults.min_y must not exceed defaults.max_y")
    weights = {
        layer: require_int(
            defaults,
            f"{layer}_weight",
            1,
            10_000,
            "defaults",
        )
        for layer in vein_builder.LAYERS
    }
    provenance = document.get("provenance")
    if not isinstance(provenance, str) or not provenance.strip():
        raise ValueError("Closure ore declarations provenance must be non-empty")

    veins: list[dict[str, Any]] = []
    for material in sorted(materials):
        vein_id = f"large_{material}_vein"
        source = {
            "schema_version": 1,
            "id": vein_id,
            **{
                layer: [{"material": material, "weight": weights[layer]}]
                for layer in vein_builder.LAYERS
            },
            **settings,
            "salt": stable_salt(vein_id),
            "provenance": provenance,
        }
        veins.append(
            vein_builder.validate_vein(
                Path(f"{vein_id}.json"), source, capabilities
            )
        )
    return veins


def load_geometry_policy() -> dict[str, Any]:
    document = require_mapping(
        load(ORE_DECLARATIONS), str(ORE_DECLARATIONS)
    )
    policy = require_mapping(
        document.get("geometry_policy"), "geometry_policy"
    )
    expected = {
        "status": "UNIFORM_PLACEHOLDER",
        "source": "CrucibleCraft T9 balance policy",
        "gt6_worldgen_import": "DEFERRED",
        "open_item": "O-29",
    }
    for key, value in expected.items():
        if policy.get(key) != value:
            raise ValueError(
                f"geometry_policy.{key} must be {value!r}"
            )
    reason = policy.get("reason")
    if not isinstance(reason, str) or not reason.strip():
        raise ValueError("geometry_policy.reason must be non-empty")
    defaults = require_mapping(document.get("defaults"), "defaults")
    return {
        **policy,
        "defaults": dict(defaults),
        "role_material_policy": "single_material_all_layers",
    }


def validate_fluid_deposit(
    entry: Any,
    index: int,
    material_documents: dict[str, dict[str, Any]],
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
    state = (material_document.get("gt6_metadata") or {}).get("state")
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
    deposits = [
        validate_fluid_deposit(entry, index, material_documents)
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
    geometry_policy = load_geometry_policy()
    deposits = load_fluid_deposits()
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
            "fluid_declarations": {
                "path": (
                    "src/main/resources/data/cruciblecraft/"
                    "worldgen_catalog/fluid_deposits.json"
                ),
                "sha256": sha256(FLUID_DECLARATIONS),
            },
        },
        "counts": {
            "t2_vein_families": len(t2_veins),
            "t2_worldgen_materials": len(t2_materials),
            "closure_vein_classifications": len(closure_veins),
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
                )
            }
            for deposit in deposits
        ],
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
        path.write_text(content, encoding="utf-8", newline="\n")


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
        READINESS.write_text(
            expected_readiness, encoding="utf-8", newline="\n"
        )
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
