#!/usr/bin/env python3
"""Build the bounded GT6 Loader_Recipes_Decomp projection for T5."""

from __future__ import annotations

import argparse
import hashlib
import json
import shutil
import sys
from collections import Counter
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))
TOOLS = ROOT / "tools"
MATERIAL_DIR = (
    ROOT / "src/main/resources/data/cruciblecraft/materials"
)
REGISTRATION_GATE = (
    ROOT
    / "src/main/resources/data/cruciblecraft/material_registration_gate.json"
)
NORMALIZED_MATERIALS = TOOLS / "gt6_oredict_materials_normalized.json"
ORE_CHAIN_CLOSURE = TOOLS / "gt6_ore_chain_closure.json"
READINESS = TOOLS / "t5_chemical_readiness.json"
OUTPUT_ROOT = (
    ROOT
    / "src/t5_chemical_generated/resources/data/cruciblecraft"
)
RECIPE_ROOT = OUTPUT_ROOT / "recipe/t5"
FLUID_GATE = OUTPUT_ROOT / "t5_chemical_fluid_gate.json"
MANIFEST = TOOLS / "t5_chemical_recipe_manifest.json"
DISTILLERY_LEDGER = TOOLS / "t5_distillery_projection.json"
SOURCE_PROJECTION = TOOLS / "build_t5_source_projection.py"

GT6_REPOSITORY = "GregTech6/gregtech6"
GT6_REVISION = "3703e40308c8c030763fd6297dea8b210d2a77b1"
GT6_LOADER = "src/main/java/gregtech/loaders/c/Loader_Recipes_Decomp.java"
GT6_U = 648_648_000
AMBIENT_KELVIN = 300
MAX_COMPONENT_DIVIDER = 64
MAX_ITEM_OUTPUTS = 6
MAX_FLUID_OUTPUTS = 6
DUST_FORMS = (
    ("dust", 144),
    ("small_dust", 36),
    ("tiny_dust", 16),
)

ELECTROLYSABLE = "PROCESSING.ELECTROLYSABLE"
CENTRIFUGABLE = "PROCESSING.CENTRIFUGABLE"
DECOMPOSABLE = "COMPOUNDS.DECOMPOSABLE"


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
    for path in sorted(MATERIAL_DIR.glob("*.json")):
        if path.name == "index.json":
            continue
        document = load(path)
        result[document["id"]] = document
    return result


def exact_floor(numerator: int, denominator: int, multiplier: int) -> int:
    if numerator < 0 or denominator <= 0 or multiplier <= 0:
        raise ValueError("invalid GT6 unit conversion")
    return numerator * multiplier // denominator


def source_material_id(
    reference: dict[str, Any],
    cc_by_source_id: dict[int, str],
) -> str | None:
    return cc_by_source_id.get(int(reference.get("material_id", -1)))


def output_fluid(
    source: dict[str, Any],
    component: dict[str, Any],
    component_id: str,
) -> tuple[str, int] | None:
    if float(source["thermal"]["melting_point_kelvin"]) > AMBIENT_KELVIN:
        return None
    fluid = source.get("fluid")
    if not isinstance(fluid, dict) or not fluid.get("fluid"):
        return None
    state = source.get("state")
    unit_key = "gasUnit" if state == "gas" else "liquidUnit"
    unit = source.get("units", {}).get(unit_key) or {}
    unit_u = int(unit.get("numerator_u", 0))
    amount = exact_floor(
        int(component["numerator_u"]),
        unit_u,
        int(fluid["amount"]),
    )
    if amount <= 0:
        return None
    if component_id == "water":
        return "minecraft:water", amount
    return f"cruciblecraft:{component_id}", amount


def output_dust(
    component: dict[str, Any],
    component_id: str,
    registration: dict[str, list[str]],
) -> tuple[dict[str, Any] | None, str | None]:
    numerator_u = int(component["numerator_u"])
    for prefix, units in DUST_FORMS:
        denominator_u = GT6_U * units
        numerator = numerator_u * 144
        if numerator % denominator_u:
            continue
        count = numerator // denominator_u
        if count <= 0 or count > 64:
            continue
        if prefix not in set(registration.get(component_id, [])):
            return None, f"output_{prefix}_not_registered:{component_id}"
        return {
            "count": count,
            "id": f"cruciblecraft:{component_id}/{prefix}",
        }, None
    return None, f"output_dust_amount_unrepresentable:{component_id}"


def fluid_definition(
    material_id: str,
    source: dict[str, Any],
    material: dict[str, Any],
) -> dict[str, Any]:
    state = source["state"]
    if state not in {"liquid", "gas"}:
        raise ValueError(
            f"ambient fluid {material_id} has unsupported state {state}"
        )
    density_value = max(
        1,
        round(float(source["thermal"]["density"]) * 1000),
    )
    density = -density_value if state == "gas" else density_value
    return {
        "color": material["color"],
        "density": density,
        "id": material_id,
        "material": material_id,
        "source": {
            "path": GT6_LOADER,
            "reason": (
                "GT6 decomposition emits this below-ambient component through "
                "its registered material fluid form"
            ),
            "repository": GT6_REPOSITORY,
            "revision": GT6_REVISION,
        },
        "state": state,
        "temperature_kelvin": AMBIENT_KELVIN,
        "viscosity": 200 if state == "gas" else 1000,
        "world_placeable": False,
    }


def recipe(
    material_id: str,
    route: str,
    divider: int,
    item_outputs: list[dict[str, Any]],
    fluid_outputs: list[dict[str, Any]],
    total_u: int,
) -> dict[str, Any]:
    duration = max(16, total_u * 14 // GT6_U)
    eut = max(1, (total_u * 292 + GT6_U - 1) // GT6_U)
    document: dict[str, Any] = {
        "can_be_buffered": True,
        "duration": duration,
        "eut": eut,
        "item_input_counts": [divider],
        "item_inputs": [
            {"tag": f"c:dusts/{material_id}"}
        ],
        "map": f"cruciblecraft:{route}",
        "provenance": {
            "evidence_hashes": [GT6_REVISION],
            "selected_source_recipe": GT6_LOADER,
            "source_kind": "gt6_loader_projection",
        },
        "type": "cruciblecraft:gt_recipe",
    }
    if item_outputs:
        document["item_outputs"] = item_outputs
        document["output_chances"] = [10_000] * len(item_outputs)
    if fluid_outputs:
        document["fluid_outputs"] = fluid_outputs
    return document


def _build_loader_decomp(
    write: bool = True,
    planned_files: dict[Path, bytes] | None = None,
) -> dict[str, Any]:
    materials = material_documents()
    registration = load(REGISTRATION_GATE)["materials"]
    normalized = load(NORMALIZED_MATERIALS)["records"]
    closure = load(ORE_CHAIN_CLOSURE)
    readiness = load(READINESS)
    readiness_by_material = {
        row["material"]: row
        for row in readiness["chemical_materials"]
    }
    selected_materials = {
        material_id
        for material_id, row in readiness_by_material.items()
        if row["classification"] == "route_ready"
    }
    terminal_materials = {
        row["material"]
        for row in closure["sifter_dust_without_smelter"]
        if row["classification"] == "t5_chemical"
    }
    cc_by_source_id = {
        int(document["gt6_metadata"]["source_id"]): material_id
        for material_id, document in materials.items()
        if document.get("gt6_metadata")
    }
    source_by_id = {
        int(document["source_id"]): document for document in normalized
    }

    if write:
        if RECIPE_ROOT.exists():
            shutil.rmtree(RECIPE_ROOT)
        RECIPE_ROOT.mkdir(parents=True, exist_ok=True)

    fluid_definitions: dict[str, dict[str, Any]] = {}
    generated: list[dict[str, Any]] = []
    rejected: list[dict[str, Any]] = []
    counters: Counter[str] = Counter()

    for material_id, material in sorted(materials.items()):
        if material_id not in selected_materials:
            continue
        metadata = material.get("gt6_metadata") or {}
        tags = set(metadata.get("material_tags") or [])
        routes = []
        if ELECTROLYSABLE in tags:
            routes.append("electrolyzer")
        if CENTRIFUGABLE in tags:
            routes.append("centrifuge")
        if not routes:
            continue
        counters["route_tagged_materials"] += 1
        source = source_by_id.get(int(metadata.get("source_id", -1)))
        reasons: list[str] = []
        if source is None:
            reasons.append("missing_normalized_source")
        elif DECOMPOSABLE not in tags:
            reasons.append("missing_decomposable_tag")
        components = [] if source is None else list(source.get("components") or [])
        divider = 0 if source is None else int(
            source.get("component_common_divider") or 0
        )
        if not components:
            reasons.append("missing_components")
        if divider <= 0 or divider > MAX_COMPONENT_DIVIDER:
            reasons.append("component_divider_out_of_range")
        if "dust" not in set(registration.get(material_id, [])):
            reasons.append("input_dust_not_registered")

        item_outputs: list[dict[str, Any]] = []
        fluid_outputs: list[dict[str, Any]] = []
        total_u = 0
        if not reasons:
            for component in components:
                total_u += int(component["numerator_u"])
                component_id = source_material_id(
                    component,
                    cc_by_source_id,
                )
                if component_id is None:
                    reasons.append(
                        f"unresolved_component:{component.get('material')}"
                    )
                    continue
                component_source = source_by_id[int(component["material_id"])]
                fluid = output_fluid(
                    component_source,
                    component,
                    component_id,
                )
                if fluid is not None:
                    fluid_id, amount = fluid
                    fluid_outputs.append({"amount": amount, "id": fluid_id})
                    if fluid_id.startswith("cruciblecraft:") and component_id not in {
                        "creosote",
                        "steam",
                    }:
                        fluid_definitions.setdefault(
                            component_id,
                            fluid_definition(
                                component_id,
                                component_source,
                                materials[component_id],
                            ),
                        )
                    continue
                item_output, reason = output_dust(
                    component,
                    component_id,
                    registration,
                )
                if reason is not None:
                    reasons.append(reason)
                    continue
                item_outputs.append(item_output)
        if len(item_outputs) > MAX_ITEM_OUTPUTS:
            reasons.append("too_many_item_outputs")
        if len(fluid_outputs) > MAX_FLUID_OUTPUTS:
            reasons.append("too_many_fluid_outputs")
        if not item_outputs and not fluid_outputs:
            reasons.append("no_resolved_outputs")

        if reasons:
            for reason in sorted(set(reasons)):
                counters[f"rejected:{reason.split(':', 1)[0]}"] += 1
            rejected.append(
                {
                    "material": material_id,
                    "reasons": sorted(set(reasons)),
                    "routes": routes,
                    "terminal_dust": material_id in terminal_materials,
                }
            )
            counters["rejected_materials"] += 1
            continue

        for route in routes:
            output = RECIPE_ROOT / route / f"{material_id}.json"
            output.parent.mkdir(parents=True, exist_ok=True)
            document = recipe(
                material_id,
                route,
                divider,
                item_outputs,
                fluid_outputs,
                total_u,
            )
            counters["max_eut"] = max(counters["max_eut"], document["eut"])
            counters["max_item_outputs"] = max(
                counters["max_item_outputs"],
                len(document.get("item_outputs", [])),
            )
            counters["max_fluid_outputs"] = max(
                counters["max_fluid_outputs"],
                len(document.get("fluid_outputs", [])),
            )
            counters["max_fluid_output_amount"] = max(
                counters["max_fluid_output_amount"],
                max(
                    (
                        stack["amount"]
                        for stack in document.get("fluid_outputs", [])
                    ),
                    default=0,
                ),
            )
            encoded = stable(document).encode("utf-8")
            if planned_files is not None:
                planned_files[output] = encoded
            if write:
                output.write_bytes(encoded)
            generated.append(
                {
                    "id": f"cruciblecraft:t5/{route}/{material_id}",
                    "material": material_id,
                    "route": route,
                }
            )
            counters[f"{route}_recipes"] += 1
        counters["recipe_ready_materials"] += 1

    fluid_gate = {
        "fluids": [
            fluid_definitions[key] for key in sorted(fluid_definitions)
        ],
        "schema_version": 1,
    }
    encoded_fluid_gate = stable(fluid_gate).encode("utf-8")
    if planned_files is not None:
        planned_files[FLUID_GATE] = encoded_fluid_gate
    if write:
        FLUID_GATE.parent.mkdir(parents=True, exist_ok=True)
        FLUID_GATE.write_bytes(encoded_fluid_gate)

    generated_materials = {row["material"] for row in generated}
    terminal_ready = terminal_materials & generated_materials
    counters["terminal_dust_denominator"] = len(terminal_materials)
    counters["terminal_dust_recipe_ready"] = len(terminal_ready)
    counters["terminal_dust_unresolved"] = len(
        terminal_materials - terminal_ready
    )

    manifest = {
        "counts": {
            **dict(sorted(counters.items())),
            "chemical_fluids": len(fluid_definitions),
            "generated_recipes": len(generated),
        },
        "generated": generated,
        "inputs": {
            str(NORMALIZED_MATERIALS.relative_to(ROOT)): digest(
                NORMALIZED_MATERIALS
            ),
            str(REGISTRATION_GATE.relative_to(ROOT)): digest(
                REGISTRATION_GATE
            ),
            str(ORE_CHAIN_CLOSURE.relative_to(ROOT)): digest(
                ORE_CHAIN_CLOSURE
            ),
            str(READINESS.relative_to(ROOT)): digest(READINESS),
        },
        "policy": {
            "ambient_kelvin": AMBIENT_KELVIN,
            "loader_path": GT6_LOADER,
            "max_component_divider": MAX_COMPONENT_DIVIDER,
            "selection": "t5_chemical_readiness.chemical_materials[classification=route_ready]",
            "repository": GT6_REPOSITORY,
            "revision": GT6_REVISION,
        },
        "rejected": rejected,
        "schema_version": 1,
    }
    encoded_manifest = stable(manifest).encode("utf-8")
    if planned_files is not None:
        planned_files[MANIFEST] = encoded_manifest
    if write:
        MANIFEST.write_bytes(encoded_manifest)
    return manifest


def build(
    write: bool = True,
    planned_files: dict[Path, bytes] | None = None,
) -> dict[str, Any]:
    """Build the final pinned-dump projection.

    The Loader_Recipes_Decomp implementation above remains as an auditable
    narrow projection, but T5 closure is selected from the complete pinned dump.
    """
    try:
        from tools.build_t5_source_projection import (
            build as build_source_projection,
        )
    except ModuleNotFoundError:
        from build_t5_source_projection import (
            build as build_source_projection,
        )

    return build_source_projection(
        write=write,
        planned_files=planned_files,
    )


METADATA_KEYS = ("inputs", "proof", "output_hashes")


def semantic_manifest(document: dict[str, Any]) -> dict[str, Any]:
    result = dict(document)
    for key in METADATA_KEYS:
        result.pop(key, None)
    return result


def verify_metadata_rebase(
    committed: dict[str, Any],
    candidate: dict[str, Any],
) -> list[str]:
    errors: list[str] = []
    if semantic_manifest(committed) != semantic_manifest(candidate):
        errors.append("T5 recipe metadata rebase changed semantic fields")
    return errors


def check() -> list[str]:
    planned: dict[Path, bytes] = {}
    build(write=False, planned_files=planned)
    actual = set(RECIPE_ROOT.rglob("*.json")) if RECIPE_ROOT.exists() else set()
    actual.update(
        path
        for path in (FLUID_GATE, MANIFEST, DISTILLERY_LEDGER)
        if path.is_file()
    )
    errors = [
        f"missing generated file: {path.relative_to(ROOT)}"
        for path in sorted(set(planned) - actual)
    ]
    errors.extend(
        f"extra generated file: {path.relative_to(ROOT)}"
        for path in sorted(actual - set(planned))
    )
    errors.extend(
        f"stale generated file: {path.relative_to(ROOT)}"
        for path in sorted(actual & set(planned))
        if path.read_bytes() != planned[path]
    )
    return errors


def reference_only_check() -> list[str]:
    if not MANIFEST.is_file():
        return [f"missing compact manifest: {MANIFEST.relative_to(ROOT)}"]
    try:
        manifest = load(MANIFEST)
    except (OSError, ValueError, json.JSONDecodeError) as exc:
        return [f"invalid compact manifest: {exc}"]
    from tools import currentness

    errors: list[str] = []
    if manifest.get("schema_version") != 5:
        errors.append("compact manifest schema_version must be 5")
    if manifest.get("status") != "closure_ready":
        errors.append("compact manifest status is not closure_ready")
    errors.extend(currentness.check_sidecar(MANIFEST))

    output_hashes = manifest.get("output_hashes") or {}
    expected_paths = {ROOT / relative for relative in output_hashes}
    actual_paths = (
        set(RECIPE_ROOT.rglob("*.json"))
        if RECIPE_ROOT.is_dir()
        else set()
    )
    actual_paths.update(
        path for path in (FLUID_GATE, DISTILLERY_LEDGER) if path.is_file()
    )
    for path in sorted(expected_paths - actual_paths):
        errors.append(f"missing generated file: {path.relative_to(ROOT)}")
    for path in sorted(actual_paths - expected_paths):
        errors.append(f"extra generated file: {path.relative_to(ROOT)}")
    if (
        manifest.get("counts", {}).get("generated_recipes")
        != len(set(RECIPE_ROOT.rglob("*.json")))
    ):
        errors.append("generated recipe count drifted")
    return errors


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    mode = parser.add_mutually_exclusive_group()
    mode.add_argument("--reference-only", action="store_true")
    mode.add_argument("--full-replay", action="store_true")
    args = parser.parse_args()
    if (args.reference_only or args.full_replay) and not args.check:
        parser.error("--reference-only and --full-replay require --check")
    if args.check:
        errors = reference_only_check() if args.reference_only else check()
        if errors:
            print("T5 decomposition projection is stale:")
            for error in errors:
                print(f"- {error}")
            return 1
        tier = "compact" if args.reference_only else "full replay"
        print(
            "T5 decomposition projection matches committed "
            f"{tier} resources."
        )
        return 0
    document = build()
    counts = document["counts"]
    print(
        "T5 decomposition projection: "
        f"{counts['generated_recipes']} recipes, "
        f"{counts['chemical_fluids']} chemical fluids, "
        f"{counts.get('rejected_materials', 0)} rejected materials"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
