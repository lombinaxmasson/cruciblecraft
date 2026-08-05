#!/usr/bin/env python3
"""Validate semantic vein sources and bake their runtime worldgen resources."""
from __future__ import annotations

import argparse
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

from tools import build_gt6_material_form_gate as gate_builder  # noqa: E402

AUTHOR_ROOT = (
    ROOT / "src/main/resources/data/cruciblecraft/veins"
)
REGISTRATION_GATE = (
    ROOT
    / "src/main/resources/data/cruciblecraft/material_registration_gate.json"
)
OUTPUT_RESOURCE_ROOT = ROOT / "src/worldgen_generated/resources"
LAYERS = ("top", "bottom", "between", "spread")
RESOURCE_PATH = re.compile(r"[a-z0-9_.-]+")
REPLACEABLE_TAG = "cruciblecraft:large_vein_replaceables"
# A 3x3-chunk WorldGenRegion anchored at min + 8 has 24 blocks toward its
# minimum edge and 23 toward its maximum edge. Keep authored radii symmetric.
MAX_SAFE_HORIZONTAL_RADIUS = 23
T8_PIPE_FORMS = {
    "tiny_fluid_pipe",
    "small_fluid_pipe",
    "fluid_pipe",
    "large_fluid_pipe",
    "huge_fluid_pipe",
    "item_pipe",
    "large_item_pipe",
    "huge_item_pipe",
}
T10_FORM_FLAGS = {
    "gt6:itemgenerator/multiingots": {
        "double_ingot",
        "triple_ingot",
    },
    "gt6:itemgenerator/hotingots": {"ingot_hot"},
}


def load(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def stable_json(value: Any) -> str:
    return json.dumps(
        value,
        ensure_ascii=False,
        indent=2,
        sort_keys=True,
    ) + "\n"


def material_capabilities() -> dict[str, dict[str, set[str]]]:
    """Read independently-derived factual forms and the runtime registration gate."""
    documents, factual_forms = gate_builder.material_documents()
    gate = load(REGISTRATION_GATE).get("materials") or {}
    if set(documents) != set(gate):
        raise ValueError("material registration gate does not cover the factual catalog")

    capabilities: dict[str, dict[str, set[str]]] = {}
    for material_id in sorted(documents):
        registered = set(gate[material_id])
        factual = set(factual_forms[material_id])
        flags = set(documents[material_id].get("generation_flags") or [])
        t10_forms = set().union(*(
            forms
            for flag, forms in T10_FORM_FLAGS.items()
            if flag in flags
        )) if flags & set(T10_FORM_FLAGS) else set()
        if not (registered - factual) <= T8_PIPE_FORMS | t10_forms:
            raise ValueError(
                "registration gate exceeds factual or source-backed T8/T10 "
                f"forms for {material_id}"
            )
        capabilities[material_id] = {
            "factual": factual,
            "registered": registered,
        }
    return capabilities


def require_int(
    document: dict[str, Any],
    field: str,
    minimum: int,
    maximum: int,
    source: Path,
) -> int:
    value = document.get(field)
    if isinstance(value, bool) or not isinstance(value, int):
        raise ValueError(f"{source}: {field} must be an integer")
    if not minimum <= value <= maximum:
        raise ValueError(
            f"{source}: {field} must be between {minimum} and {maximum}"
        )
    return value


def require_number(
    document: dict[str, Any],
    field: str,
    minimum: float,
    maximum: float,
    source: Path,
) -> float:
    value = document.get(field)
    if isinstance(value, bool) or not isinstance(value, (int, float)):
        raise ValueError(f"{source}: {field} must be a number")
    result = float(value)
    if not math.isfinite(result) or not minimum <= result <= maximum:
        raise ValueError(
            f"{source}: {field} must be between {minimum} and {maximum}"
        )
    return result


def validate_vein(
    source: Path,
    document: Any,
    capabilities: dict[str, dict[str, set[str]]],
) -> dict[str, Any]:
    if not isinstance(document, dict):
        raise ValueError(f"{source}: vein source must be a JSON object")
    if document.get("schema_version") != 1:
        raise ValueError(f"{source}: schema_version must be 1")

    vein_id = document.get("id")
    if not isinstance(vein_id, str) or RESOURCE_PATH.fullmatch(vein_id) is None:
        raise ValueError(f"{source}: id must be an unnamespaced resource path")
    if source.stem != vein_id:
        raise ValueError(
            f"{source}: filename stem {source.stem!r} must equal id {vein_id!r}"
        )

    provenance = document.get("provenance")
    if not isinstance(provenance, str) or not provenance.strip():
        raise ValueError(f"{source}: provenance must be a non-empty string")

    normalized: dict[str, Any] = {
        "schema_version": 1,
        "id": vein_id,
    }
    for layer in LAYERS:
        entries = document.get(layer)
        if not isinstance(entries, list) or not entries:
            raise ValueError(f"{source}: {layer} must be a non-empty list")
        normalized_entries = []
        for index, entry in enumerate(entries):
            label = f"{source}: {layer}[{index}]"
            if not isinstance(entry, dict):
                raise ValueError(f"{label} must be an object")
            material = entry.get("material")
            if (
                not isinstance(material, str)
                or RESOURCE_PATH.fullmatch(material) is None
            ):
                raise ValueError(
                    f"{label}.material must be an unnamespaced CC material id"
                )
            capability = capabilities.get(material)
            if capability is None:
                raise ValueError(f"{label} references unknown material {material}")
            if "ore" not in capability["factual"]:
                raise ValueError(f"{label} material {material} is not factual ORE")
            if "ore" not in capability["registered"]:
                raise ValueError(
                    f"{label} material {material} is not gate-registered ORE"
                )
            weight = entry.get("weight")
            if (
                isinstance(weight, bool)
                or not isinstance(weight, int)
                or not 1 <= weight <= 10_000
            ):
                raise ValueError(f"{label}.weight must be between 1 and 10000")
            normalized_entries.append({
                "material": material,
                "weight": weight,
            })
        normalized[layer] = normalized_entries

    min_y = require_int(document, "min_y", -64, 320, source)
    max_y = require_int(document, "max_y", -64, 320, source)
    if min_y > max_y:
        raise ValueError(f"{source}: min_y must not exceed max_y")
    normalized.update({
        "min_y": min_y,
        "max_y": max_y,
        "horizontal_radius": require_int(
            document, "horizontal_radius", 4, MAX_SAFE_HORIZONTAL_RADIUS, source
        ),
        "vertical_radius": require_int(
            document, "vertical_radius", 2, 24, source
        ),
        "density": require_number(document, "density", 0.01, 1.0, source),
        "region_size_chunks": require_int(
            document, "region_size_chunks", 2, 32, source
        ),
        "generation_chance": require_number(
            document, "generation_chance", 0.0, 1.0, source
        ),
        "salt": require_int(
            document, "salt", -(2**31), 2**31 - 1, source
        ),
        "provenance": provenance,
    })
    return normalized


def load_veins(
    author_root: Path = AUTHOR_ROOT,
    capabilities: dict[str, dict[str, set[str]]] | None = None,
) -> list[dict[str, Any]]:
    if not author_root.is_dir():
        raise ValueError(f"vein author root does not exist: {author_root}")
    sources = sorted(author_root.glob("*.json"))
    if not sources:
        raise ValueError(f"vein author root is empty: {author_root}")
    resolved_capabilities = capabilities or material_capabilities()
    veins = [
        validate_vein(source, load(source), resolved_capabilities)
        for source in sources
    ]

    ids: set[str] = set()
    salts: dict[int, str] = {}
    for vein in veins:
        vein_id = vein["id"]
        if vein_id in ids:
            raise ValueError(f"duplicate vein id: {vein_id}")
        ids.add(vein_id)
        salt = vein["salt"]
        if salt in salts:
            raise ValueError(
                f"duplicate salt {salt}: {salts[salt]} and {vein_id}"
            )
        salts[salt] = vein_id
    return veins


def runtime_state(entry: dict[str, Any]) -> dict[str, Any]:
    # All authored materials compile to the stone variant. The feature's
    # OreHostStateAdapter deterministically selects stone/deepslate at placement.
    return {
        "state": {
            "Name": f"cruciblecraft:{entry['material']}_ore",
        },
        "weight": entry["weight"],
    }


def configured_feature(vein: dict[str, Any]) -> dict[str, Any]:
    config = {
        layer: [runtime_state(entry) for entry in vein[layer]]
        for layer in LAYERS
    }
    config.update({
        "min_y": vein["min_y"],
        "max_y": vein["max_y"],
        "horizontal_radius": vein["horizontal_radius"],
        "vertical_radius": vein["vertical_radius"],
        "density": vein["density"],
        "replaceable": REPLACEABLE_TAG,
        "region_size_chunks": vein["region_size_chunks"],
        "generation_chance": vein["generation_chance"],
        "salt": vein["salt"],
    })
    return {
        "type": "cruciblecraft:large_vein",
        "config": config,
    }


def build_documents(
    author_root: Path = AUTHOR_ROOT,
    capabilities: dict[str, dict[str, set[str]]] | None = None,
) -> tuple[list[dict[str, Any]], dict[str, str]]:
    veins = load_veins(author_root, capabilities)
    files: dict[str, str] = {}
    for vein in veins:
        vein_id = vein["id"]
        files[
            f"data/cruciblecraft/worldgen/configured_feature/{vein_id}.json"
        ] = stable_json(configured_feature(vein))
        files[
            f"data/cruciblecraft/worldgen/placed_feature/{vein_id}.json"
        ] = stable_json({
            "feature": f"cruciblecraft:{vein_id}",
            "placement": [],
        })
    files[
        "data/cruciblecraft/neoforge/biome_modifier/add_large_veins.json"
    ] = stable_json({
        "type": "neoforge:add_features",
        "biomes": "#minecraft:is_overworld",
        "features": [
            f"cruciblecraft:{vein['id']}"
            for vein in veins
        ],
        "step": "underground_ores",
    })
    return veins, files


def check_outputs(files: dict[str, str]) -> list[str]:
    actual = {
        path.relative_to(OUTPUT_RESOURCE_ROOT).as_posix(): path
        for path in OUTPUT_RESOURCE_ROOT.rglob("*.json")
    } if OUTPUT_RESOURCE_ROOT.is_dir() else {}
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


def review_document(
    veins: list[dict[str, Any]],
    files: dict[str, str],
) -> dict[str, Any]:
    materials = {
        entry["material"]
        for vein in veins
        for layer in LAYERS
        for entry in vein[layer]
    }
    return {
        "schema_version": 1,
        "author_root": AUTHOR_ROOT.relative_to(ROOT).as_posix(),
        "output_root": OUTPUT_RESOURCE_ROOT.relative_to(ROOT).as_posix(),
        "veins": [
            {
                "id": vein["id"],
                "salt": vein["salt"],
                "provenance": vein["provenance"],
            }
            for vein in veins
        ],
        "counts": {
            "veins": len(veins),
            "materials": len(materials),
            "configured_features": len(veins),
            "placed_features": len(veins),
            "biome_modifiers": 1,
            "generated_files": len(files),
        },
        "worldgen_materials": sorted(materials),
        "host_policy": "stone source state adapted to the replaced host at runtime",
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    modes = parser.add_mutually_exclusive_group(required=True)
    modes.add_argument("--write", action="store_true")
    modes.add_argument("--check", action="store_true")
    modes.add_argument("--review", action="store_true")
    args = parser.parse_args()
    try:
        veins, files = build_documents()
    except (OSError, ValueError, json.JSONDecodeError) as error:
        print(f"Vein build failed: {error}", file=sys.stderr)
        return 1

    if args.review:
        print(stable_json(review_document(veins, files)), end="")
        return 0
    if args.write:
        write_outputs(files)
        print(
            f"Wrote {len(files)} worldgen files under {OUTPUT_RESOURCE_ROOT}"
        )
        return 0
    errors = check_outputs(files)
    if errors:
        print(
            "Generated vein resources are stale:\n"
            + "\n".join(f"- {error}" for error in errors),
            file=sys.stderr,
        )
        return 1
    print("Generated vein resources are current.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
