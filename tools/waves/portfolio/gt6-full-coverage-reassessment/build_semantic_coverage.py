#!/usr/bin/env python3
"""Build the semantic recipe comparison sidecar for GT6 coverage.

This sidecar reuses the existing normalized comparison IR and material-rule
expander from ``tools/compare_gt6_recipes.py``. It intentionally does not use
that tool's human expectation ledger: a stale TODO_PORT decision must not
prevent an observational coverage report from being generated.

The result is a semantic-evidence report, not a GT6 source-row completion
claim. ``EXACT`` means exact normalized IO/chance signature in the comparator
IR; ``FORM_PATH`` and ``SEMANTIC`` are progressively weaker candidate tiers.
None of those tiers proves machine metadata, player obtainability, or raw GT6
row multiplicity.
"""
from __future__ import annotations

import argparse
import hashlib
import importlib.util
import json
import re
import sys
from collections import Counter
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[4]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))
if str(ROOT / "tools") not in sys.path:
    sys.path.insert(0, str(ROOT / "tools"))
WAVE_ROOT = ROOT / "tools" / "waves" / "portfolio" / "gt6-full-coverage-reassessment"
OUTPUT_PATH = WAVE_ROOT / "semantic_coverage.json"
REFERENCE_PATH = ROOT / "tools" / "gt6_recipe_normalized_reference.json"
COMPARATOR_PATH = ROOT / "tools" / "compare_gt6_recipes.py"
SCOPE_PATH = WAVE_ROOT / "scope.json"
BUILDER_PATH = Path(__file__).resolve()

COMPARATOR_MODULE_NAME = "cruciblecraft_gt6_recipe_comparator"
RECONCILIATION_MODULE_NAME = "cruciblecraft_gt6_coverage_reconciliation"
RECONCILIATION_PATH = WAVE_ROOT / "build_reconciliation.py"


def stable_json(value: Any) -> str:
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":"))


def sha256_text(value: str) -> str:
    return hashlib.sha256(value.encode("utf-8")).hexdigest()


def sha256_file(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def load_json(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def load_comparator():
    spec = importlib.util.spec_from_file_location(COMPARATOR_MODULE_NAME, COMPARATOR_PATH)
    if spec is None or spec.loader is None:
        raise RuntimeError(f"cannot load {COMPARATOR_PATH}")
    module = importlib.util.module_from_spec(spec)
    sys.modules[COMPARATOR_MODULE_NAME] = module
    spec.loader.exec_module(module)
    return module


def load_reconciliation_helpers():
    spec = importlib.util.spec_from_file_location(
        RECONCILIATION_MODULE_NAME, RECONCILIATION_PATH
    )
    if spec is None or spec.loader is None:
        raise RuntimeError(f"cannot load {RECONCILIATION_PATH}")
    module = importlib.util.module_from_spec(spec)
    sys.modules[RECONCILIATION_MODULE_NAME] = module
    spec.loader.exec_module(module)
    return module


def _simple_item_resource(
    comparator: Any, operand: Any
) -> tuple[str, str] | None:
    if not isinstance(operand, dict):
        return None
    item_id = operand.get("item") or operand.get("id") or operand.get("items")
    if not isinstance(item_id, str) or not item_id.startswith("cruciblecraft:"):
        return None
    path = item_id.split(":", 1)[1]
    if "/" in path:
        material, form = path.split("/", 1)
    else:
        components = operand.get("components") or {}
        material = components.get("cruciblecraft:prefix_material")
        form = path if isinstance(material, str) else None
        if not isinstance(material, str):
            return None
    if material not in comparator.cached_cc_materials():
        return None
    return material, form


def _item_resource(comparator: Any, operand: Any, count: int) -> Any | None:
    simple = _simple_item_resource(comparator, operand)
    if simple is None:
        return None
    return comparator.Resource("item", f"{simple[0]}:{simple[1]}", count)


def _fluid_resource(comparator: Any, operand: Any) -> Any | None:
    if not isinstance(operand, dict):
        return None
    fluid_id = operand.get("id") or operand.get("fluid")
    if not isinstance(fluid_id, str):
        return None
    normalized = comparator.FLUID_ALIASES.get(
        fluid_id.split(":", 1)[-1], fluid_id.split(":", 1)[-1]
    )
    amount = int(operand.get("amount") or 0)
    if amount <= 0:
        return None
    return comparator.Resource("fluid", normalized, amount)


def _family_for_runtime_row(
    comparator: Any,
    target: str,
    item_inputs: list[Any],
    item_outputs: list[Any],
) -> str | None:
    if not item_inputs or not item_outputs:
        return None
    input_form = _simple_item_resource(comparator, item_inputs[0])
    output_form = _simple_item_resource(comparator, item_outputs[0])
    if input_form is None or output_form is None:
        return None
    return comparator.material_rule_family(
        target,
        input_form[1],
        output_form[1],
    )


def _runtime_relations(document: dict[str, Any]) -> tuple[str | None, list[dict[str, Any]]]:
    kind = document.get("type")
    if kind == "cruciblecraft:gt_recipe":
        return document.get("map"), [document]
    if kind != "cruciblecraft:compact_gt_recipe_family":
        return None, []
    target = document.get("target_map")
    if isinstance(document.get("matrix"), dict):
        from tools.recipe_bulk.matrix import expand_matrix

        return target, expand_matrix(document["matrix"])
    relations = document.get("relations")
    return target, relations if isinstance(relations, list) else []


def load_compact_runtime_recipes(
    comparator: Any,
) -> tuple[list[Any], dict[str, int]]:
    helpers = load_reconciliation_helpers()
    rows: list[Any] = []
    scanned_documents = 0
    runtime_rows_seen = 0
    recognized_documents = 0
    seen: set[str] = set()
    for path in helpers.runtime_recipe_files():
        try:
            document = json.loads(path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError):
            continue
        if not isinstance(document, dict):
            continue
        target, relations = _runtime_relations(document)
        if not target or not relations:
            continue
        scanned_documents += 1
        target = str(target)
        for relation in relations:
            runtime_rows_seen += 1
            item_inputs = list(relation.get("item_inputs") or [])
            item_outputs = list(relation.get("item_outputs") or [])
            family = _family_for_runtime_row(
                comparator, target, item_inputs, item_outputs
            )
            if family is None:
                continue
            recognized_documents += 1
            counts = [int(value) for value in relation.get("item_input_counts") or []]
            inputs = [
                _item_resource(
                    comparator,
                    operand,
                    counts[index] if index < len(counts) else 1,
                )
                for index, operand in enumerate(item_inputs)
            ]
            outputs = [
                _item_resource(comparator, operand, int(operand.get("count") or 1))
                for operand in item_outputs
                if isinstance(operand, dict)
            ]
            if any(item is None for item in inputs + outputs):
                continue
            fluids_in = [
                _fluid_resource(comparator, operand)
                for operand in relation.get("fluid_inputs") or []
            ]
            fluids_out = [
                _fluid_resource(comparator, operand)
                for operand in relation.get("fluid_outputs") or []
            ]
            if any(item is None for item in fluids_in + fluids_out):
                continue
            material = next(
                (
                    resource.id.split(":", 1)[0]
                    for resource in inputs + outputs
                    if resource.id.count(":") == 1
                ),
                None,
            )
            recipe = comparator.NormRecipe(
                family=family,
                source="cc",
                map_name=target,
                material=material,
                inputs=[*inputs, *fluids_in],
                outputs=[*outputs, *fluids_out],
                duration=int(relation.get("duration") or 0),
                eut=int(relation.get("eut") or 0),
                special_value=int(relation.get("special_value") or 0),
                chances=[
                    int(value) for value in relation.get("output_chances") or []
                ],
                raw_hint=path.relative_to(ROOT).as_posix(),
            )
            key = stable_json(recipe_projection_with(comparator, recipe))
            if key in seen:
                continue
            seen.add(key)
            rows.append(recipe)
    return rows, {
        "runtime_documents_seen": scanned_documents,
        "runtime_rows_seen": runtime_rows_seen,
        "recognized_runtime_rows": recognized_documents,
    }


def _generic_runtime_resource(
    comparator: Any, operand: Any, count: int
) -> Any | None:
    if count <= 0 or not isinstance(operand, dict):
        return None
    simple = _simple_item_resource(comparator, operand)
    if simple is not None:
        return comparator.Resource("item", f"{simple[0]}:{simple[1]}", count)
    item_id = operand.get("item") or operand.get("id") or operand.get("items")
    if isinstance(item_id, str) and item_id:
        return comparator.Resource("item", f"fixed:{item_id}@none", count)
    return None


def _source_map_from_provenance(value: Any) -> str | None:
    if not isinstance(value, str):
        return None
    match = re.search(r"((?:gt|mc)\.recipe\.[a-z0-9.]+)", value)
    return match.group(1) if match else None


def _source_map_index(helpers: Any) -> tuple[dict[str, list[str]], set[str]]:
    local_to_source: dict[str, list[str]] = {}
    all_sources: set[str] = set()
    for row in helpers.load_recipe_maps():
        source = str(row.get("name_internal") or "")
        if not source:
            continue
        all_sources.add(source)
        local_to_source.setdefault(helpers.map_key(source), []).append(source)
    return local_to_source, all_sources


def _generic_source_maps_for_row(
    helpers: Any,
    local_to_source: dict[str, list[str]],
    all_sources: set[str],
    target: str,
    relation: dict[str, Any],
) -> list[str]:
    provenance = relation.get("provenance") or {}
    declared = _source_map_from_provenance(
        provenance.get("selected_source_recipe")
    )
    if declared in all_sources:
        return [declared]
    local = str(target).split(":", 1)[-1]
    candidates = local_to_source.get(local) or []
    return candidates[:1]


def load_generic_runtime_rows(
    comparator: Any,
) -> tuple[dict[str, list[Any]], dict[str, int]]:
    """Normalize runtime rows for every GT map with a simple raw-map match.

    This is intentionally exact-only and separate from the family heuristics.
    It expands the map boundary before we attempt richer family semantics.
    """
    helpers = load_reconciliation_helpers()
    local_to_source, all_sources = _source_map_index(helpers)
    by_source: dict[str, list[Any]] = {}
    seen: set[str] = set()
    rows_seen = 0
    rows_normalized = 0
    for path in helpers.runtime_recipe_files():
        try:
            document = json.loads(path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError):
            continue
        if not isinstance(document, dict):
            continue
        target, relations = _runtime_relations(document)
        if not target:
            continue
        for relation in relations:
            rows_seen += 1
            item_inputs = list(relation.get("item_inputs") or [])
            item_outputs = list(relation.get("item_outputs") or [])
            counts = [int(value) for value in relation.get("item_input_counts") or []]
            inputs = [
                _generic_runtime_resource(
                    comparator,
                    operand,
                    counts[index] if index < len(counts) else 1,
                )
                for index, operand in enumerate(item_inputs)
            ]
            outputs = [
                _generic_runtime_resource(
                    comparator,
                    operand,
                    int(operand.get("count") or 1),
                )
                for operand in item_outputs
                if isinstance(operand, dict)
            ]
            if any(resource is None for resource in inputs + outputs):
                continue
            fluid_inputs = [
                _fluid_resource(comparator, operand)
                for operand in relation.get("fluid_inputs") or []
            ]
            fluid_outputs = [
                _fluid_resource(comparator, operand)
                for operand in relation.get("fluid_outputs") or []
            ]
            if any(resource is None for resource in fluid_inputs + fluid_outputs):
                continue
            source_maps = _generic_source_maps_for_row(
                helpers, local_to_source, all_sources, str(target), relation
            )
            for source_map in source_maps:
                family = f"raw_map::{source_map}"
                material = next(
                    (
                        resource.id.split(":", 1)[0]
                        for resource in inputs + outputs
                        if resource.id.count(":") == 1
                        and not resource.id.startswith("fixed:")
                    ),
                    None,
                )
                recipe = comparator.NormRecipe(
                    family=family,
                    source="cc",
                    map_name=str(target),
                    material=material,
                    inputs=[*inputs, *fluid_inputs],
                    outputs=[*outputs, *fluid_outputs],
                    duration=int(relation.get("duration") or 0),
                    eut=int(relation.get("eut") or 0),
                    special_value=int(relation.get("special_value") or 0),
                    chances=[
                        int(value)
                        for value in relation.get("output_chances") or []
                    ],
                    raw_hint=path.relative_to(ROOT).as_posix(),
                )
                key = stable_json(recipe_projection_with(comparator, recipe))
                if key in seen:
                    continue
                seen.add(key)
                by_source.setdefault(source_map, []).append(recipe)
                rows_normalized += 1
    return by_source, {
        "runtime_rows_seen": rows_seen,
        "runtime_rows_normalized": rows_normalized,
        "source_maps_with_generic_rows": len(by_source),
    }


def load_generic_gt_rows(
    comparator: Any,
    source_maps: set[str],
    materials: dict[str, dict[str, Any]],
) -> dict[str, list[Any]]:
    meta_map = comparator.build_meta_map(materials)
    known = set(materials)
    result: dict[str, list[Any]] = {}
    for source_map in sorted(source_maps):
        path = comparator.GT_MAPS / f"{source_map}.json"
        if not path.is_file():
            continue
        rows: list[Any] = []
        document = json.loads(path.read_text(encoding="utf-8"))
        for index, raw in enumerate(document.get("recipes") or []):
            if raw.get("enabled") is False:
                continue
            inputs = []
            outputs = []
            for item in raw.get("inputs") or []:
                normalized = comparator.normalize_gt_item(item, known, meta_map)
                if normalized is not None:
                    inputs.append(normalized)
            for item in raw.get("outputs") or []:
                normalized = comparator.normalize_gt_item(item, known, meta_map)
                if normalized is not None:
                    outputs.append(normalized)
            for fluid in raw.get("fluidInputs") or []:
                normalized = comparator.normalize_gt_fluid(fluid)
                if normalized is not None:
                    inputs.append(normalized)
            for fluid in raw.get("fluidOutputs") or []:
                normalized = comparator.normalize_gt_fluid(fluid)
                if normalized is not None:
                    outputs.append(normalized)
            if not inputs or not outputs:
                continue
            material = next(
                (
                    resource.id.split(":", 1)[0]
                    for resource in inputs + outputs
                    if resource.kind == "item"
                    and ":" in resource.id
                    and not resource.id.startswith("fixed:")
                    and resource.id.split(":", 1)[0] in known
                ),
                None,
            )
            rows.append(
                comparator.NormRecipe(
                    family=f"raw_map::{source_map}",
                    source="gt",
                    map_name=source_map,
                    material=material,
                    inputs=inputs,
                    outputs=outputs,
                    duration=int(raw.get("duration") or 0),
                    eut=int(raw.get("euPerTick") or 0),
                    special_value=comparator.gt_special_value_for_compare(
                        source_map, int(raw.get("specialValue") or 0)
                    ),
                    chances=[
                        int(value)
                        for value in raw.get("chances") or []
                    ],
                    fake=bool(raw.get("fake", False)),
                    raw_hint=f"{source_map}#recipes[{index}]",
                    source_refs=(
                        {
                            "map": source_map,
                            "index": index,
                            "sha256": comparator.stable_hash(raw),
                        },
                    ),
                )
            )
        result[source_map] = rows
    return result


def generic_map_summary(
    comparator: Any,
    cc_by_source: dict[str, list[Any]],
    gt_by_source: dict[str, list[Any]],
) -> tuple[dict[str, Any], dict[str, int]]:
    rows = []
    totals = Counter()
    raw_refs = 0
    for source_map in sorted(set(cc_by_source) | set(gt_by_source)):
        cc = cc_by_source.get(source_map, [])
        gt = gt_by_source.get(source_map, [])
        cc_counts = Counter(recipe.signature() for recipe in cc)
        gt_counts = Counter(recipe.signature() for recipe in gt)
        exact = sum(min(cc_counts[key], gt_counts[key]) for key in cc_counts)
        cc_none = len(cc) - exact
        gt_exact = exact
        gt_none = len(gt) - gt_exact
        raw_refs_for_exact = []
        remaining = Counter(cc_counts)
        for recipe in gt:
            signature = recipe.signature()
            if remaining[signature] <= 0:
                continue
            remaining[signature] -= 1
            raw_refs_for_exact.extend(recipe.source_refs)
        raw_refs += len(raw_refs_for_exact)
        row = {
            "source_map": source_map,
            "cc_rows": len(cc),
            "gt_rows": len(gt),
            "cc_exact": exact,
            "cc_none": cc_none,
            "gt_exact": gt_exact,
            "gt_none": gt_none,
            "raw_exact_ref_count": len(raw_refs_for_exact),
            "raw_exact_ref_digest": sha256_text(
                stable_json(sorted(raw_refs_for_exact, key=stable_json))
            )
            if raw_refs_for_exact
            else None,
        }
        rows.append(row)
        totals.update(
            {
                "cc_rows": len(cc),
                "gt_rows": len(gt),
                "cc_exact": exact,
                "cc_none": cc_none,
                "gt_exact": gt_exact,
                "gt_none": gt_none,
            }
        )
    return {
        "source_map_count": len(rows),
        "rows": rows,
        "totals": dict(totals),
        "raw_exact_ref_count": raw_refs,
    }, {
        "source_maps_with_generic_rows": len(rows),
        "generic_cc_rows": totals["cc_rows"],
        "generic_gt_rows": totals["gt_rows"],
    }


def load_gt_families(
    comparator: Any,
    materials: dict[str, dict[str, Any]],
    cc_all: list[Any],
    reference: dict[str, Any],
) -> tuple[dict[str, list[Any]], str]:
    if comparator.GT_INDEX.is_file():
        meta_map = comparator.build_meta_map(materials)
        meta_map = {
            key: value
            for key, value in meta_map.items()
            if value in materials or value in {"coal", "coal_coke"}
        }
        families = {}
        for family in comparator.FAMILIES:
            if family in {"cook_smelting", "cook_blasting"}:
                families[family] = comparator.source_derived_gt_recipes(
                    family, cc_all
                )
            else:
                families[family] = comparator.gt_recipes_for_family(
                    family, materials, meta_map
                )
        return families, "raw_dump"
    return (
        {
            family: [
                comparator.recipe_from_reference(family, row)
                for row in reference["families"][family]
            ]
            for family in comparator.FAMILIES
        },
        "normalized_reference",
    )


def cc_projection_digest(comparator: Any, recipes: list[Any]) -> str:
    rows = [recipe_projection_with(comparator, recipe) for recipe in recipes]
    rows.sort(key=stable_json)
    return sha256_text(stable_json(rows))


def reverse_gt_tier_labels(
    comparator: Any, family: str, cc: list[Any], gt: list[Any]
) -> list[str]:
    """Classify normalized GT rows against the expanded CC projection.

    This is deliberately separate from the comparator's CC→GT rows. It makes
    the denominator visible in the sidecar, while staying at normalized-row
    granularity rather than pretending these rows are raw dump rows.
    """
    labels: list[str] = []
    cc_signatures = {recipe.signature() for recipe in cc}
    cc_forms = {
        recipe.io_form_signature()
        for recipe in cc
        if recipe.io_form_signature() is not None
    }
    component_outputs: dict[str, set[tuple[str, str]]] = {}
    if family.startswith("component_"):
        for recipe in cc:
            if recipe.material:
                component_outputs.setdefault(recipe.material, set()).update(
                    (output.kind, output.id) for output in recipe.outputs
                )
    crush_candidates: set[tuple[str, str]] = set()
    if family in {"crush_raw_to_crushed", "anvil_raw_to_crushed"}:
        for recipe in cc:
            if not recipe.inputs or not recipe.material:
                continue
            output_key = f"item:{recipe.material}:crushed_ore"
            if output_key in comparator.primary_output_counts(recipe):
                crush_candidates.add((recipe.inputs[0].id, output_key))
    alloy_candidates: set[tuple[tuple[str, ...], str]] = set()
    if family == "alloy":
        for recipe in cc:
            if recipe.outputs:
                alloy_candidates.add(
                    (
                        tuple(sorted(resource.id for resource in recipe.inputs)),
                        recipe.outputs[0].id,
                    )
                )
    for recipe in gt:
        if recipe.signature() in cc_signatures:
            labels.append("EXACT")
            continue
        form = recipe.io_form_signature()
        if form is not None and form in cc_forms:
            labels.append("FORM_PATH")
            continue
        semantic = False
        if family.startswith("component_") and recipe.material:
            outputs = component_outputs.get(recipe.material, set())
            semantic = any(
                (output.kind, output.id) in outputs for output in recipe.outputs
            )
        elif family in {"crush_raw_to_crushed", "anvil_raw_to_crushed"}:
            if recipe.inputs:
                output_keys = set(comparator.primary_output_counts(recipe))
                semantic = any(
                    (recipe.inputs[0].id, output_key) in crush_candidates
                    for output_key in output_keys
                )
        elif family == "alloy" and recipe.outputs:
            semantic = (
                tuple(sorted(resource.id for resource in recipe.inputs)),
                recipe.outputs[0].id,
            ) in alloy_candidates
        elif family == "coke_oven":
            semantic = any(
                item.id.startswith("coal:") and item.count == 1
                for item in recipe.inputs
            ) and any(
                item.id.startswith("coal_coke:") and item.count == 1
                for item in recipe.outputs
            )
        labels.append("SEMANTIC" if semantic else "NONE")
    return labels


def reverse_gt_tier_counts(
    comparator: Any, family: str, cc: list[Any], gt: list[Any]
) -> dict[str, int]:
    counts = Counter(reverse_gt_tier_labels(comparator, family, cc, gt))
    return {tier: counts[tier] for tier in ("EXACT", "FORM_PATH", "SEMANTIC", "NONE")}


def recipe_projection_with(comparator: Any, recipe: Any) -> dict[str, Any]:
    return {
        "family": recipe.family,
        "map": recipe.map_name,
        "material": recipe.material,
        "inputs": [
            {
                "kind": resource.kind,
                "id": resource.id,
                "count": resource.count,
            }
            for resource in comparator.canonical_resources(recipe.inputs)
        ],
        "outputs": [
            {
                "kind": resource.kind,
                "id": resource.id,
                "count": resource.count,
            }
            for resource in comparator.canonical_resources(recipe.outputs)
        ],
        "duration": recipe.duration,
        "eut": recipe.eut,
        "special_value": recipe.special_value,
        "chances": list(recipe.canonical_output_chances()),
    }


def family_summary(comparator: Any, family: str, cc: list[Any], gt: list[Any]) -> dict[str, Any]:
    comparison = comparator.compare_family(family, cc, gt)
    buckets = comparison["bucket_counts"]
    gt_labels = reverse_gt_tier_labels(comparator, family, cc, gt)
    gt_tiers_counter = Counter(gt_labels)
    gt_tiers = {
        tier: gt_tiers_counter[tier]
        for tier in ("EXACT", "FORM_PATH", "SEMANTIC", "NONE")
    }
    raw_tiers_counter = Counter(
        label
        for label, recipe in zip(gt_labels, gt)
        if recipe.source_refs
    )
    source_refs = [
        reference
        for recipe in gt
        for reference in recipe.source_refs
    ]
    return {
        "family": family,
        "gt_maps": list(comparator.FAMILIES[family]["gt_maps"]),
        "note": comparator.FAMILIES[family]["note"],
        "cc_normalized_rows": comparison["cc_count"],
        "gt_normalized_rows": comparison["gt_normalized_count"],
        "match_tiers": dict(buckets),
        "gt_match_tiers": gt_tiers,
        "raw_source_ref_tiers": {
            tier: raw_tiers_counter[tier]
            for tier in ("EXACT", "FORM_PATH", "SEMANTIC", "NONE")
        },
        "raw_source_ref_count": len(source_refs),
        "raw_source_ref_digest": sha256_text(
            stable_json(sorted(source_refs, key=stable_json))
        )
        if source_refs
        else None,
        "raw_source_ref_samples": sorted(source_refs, key=stable_json)[:8],
        "candidate_rows": (
            buckets["EXACT"] + buckets["FORM_PATH"] + buckets["SEMANTIC"]
        ),
        "unmatched_cc_rows": buckets["NONE"],
        "gt_only_signature_groups": comparison["gt_only_count"],
        "samples": {
            "exact": [
                row["row_id"] for row in comparison["buckets"]["EXACT"][:12]
            ],
            "form_path": [
                row["row_id"] for row in comparison["buckets"]["FORM_PATH"][:12]
            ],
            "semantic": [
                row["row_id"] for row in comparison["buckets"]["SEMANTIC"][:12]
            ],
            "none": [
                row["row_id"] for row in comparison["buckets"]["NONE"][:12]
            ],
        },
    }


def build_report() -> dict[str, Any]:
    if not REFERENCE_PATH.is_file():
        raise FileNotFoundError(
            f"{REFERENCE_PATH.relative_to(ROOT)} is missing; semantic --write "
            "requires the local normalized GT6 reference"
        )
    comparator = load_comparator()
    comparator.clear_process_caches()
    materials = comparator.cached_cc_materials()
    cc_all = list(comparator.cached_expanded_cc_recipes())
    compact_rows, runtime_stats = load_compact_runtime_recipes(comparator)
    cc_all.extend(compact_rows)
    generic_cc, generic_runtime_stats = load_generic_runtime_rows(comparator)
    generic_gt = load_generic_gt_rows(
        comparator, set(generic_cc), materials
    )
    generic_projection, generic_projection_stats = generic_map_summary(
        comparator, generic_cc, generic_gt
    )
    reference = load_json(REFERENCE_PATH)
    gt_by_family, gt_source_mode = load_gt_families(
        comparator, materials, cc_all, reference
    )
    families = []
    for family in comparator.FAMILIES:
        gt = gt_by_family[family]
        cc = [recipe for recipe in cc_all if recipe.family == family]
        families.append(family_summary(comparator, family, cc, gt))
    tier_counts = {
        tier: sum(row["match_tiers"][tier] for row in families)
        for tier in ("EXACT", "FORM_PATH", "SEMANTIC", "NONE")
    }
    gt_tier_counts = {
        tier: sum(row["gt_match_tiers"][tier] for row in families)
        for tier in ("EXACT", "FORM_PATH", "SEMANTIC", "NONE")
    }
    raw_tier_counts = {
        tier: sum(row["raw_source_ref_tiers"][tier] for row in families)
        for tier in ("EXACT", "FORM_PATH", "SEMANTIC", "NONE")
    }
    scope = load_json(SCOPE_PATH)
    return {
        "schema_version": 1,
        "assessment_id": scope["assessment_id"],
        "status": "CURRENT_SEMANTIC_PROJECTION",
        "source_revision": scope["source"]["revision"],
        "comparator": {
            "path": COMPARATOR_PATH.relative_to(ROOT).as_posix(),
            "sha256": sha256_file(COMPARATOR_PATH),
            "ir": "compare_gt6_recipes.NormRecipe",
        },
        "builder": {
            "path": BUILDER_PATH.relative_to(ROOT).as_posix(),
            "sha256": sha256_file(BUILDER_PATH),
        },
        "reference": {
            "path": REFERENCE_PATH.relative_to(ROOT).as_posix(),
            "sha256": sha256_file(REFERENCE_PATH),
            "reference_fingerprint": reference.get("reference_fingerprint"),
        },
        "gt_source_mode": gt_source_mode,
        "cc_projection": {
            "material_count": len(materials),
            "normalized_recipe_count": len(cc_all),
            "digest": cc_projection_digest(comparator, cc_all),
            **runtime_stats,
            "generic_digest": cc_projection_digest(
                comparator,
                [recipe for rows in generic_cc.values() for recipe in rows],
            ),
            **generic_runtime_stats,
            **generic_projection_stats,
        },
        "summary": {
            "family_count": len(families),
            "cc_normalized_rows": sum(row["cc_normalized_rows"] for row in families),
            "gt_normalized_rows": sum(row["gt_normalized_rows"] for row in families),
            "raw_source_ref_count": sum(
                row["raw_source_ref_count"] for row in families
            ),
            "tier_counts": tier_counts,
            "gt_tier_counts": gt_tier_counts,
            "raw_source_ref_tiers": raw_tier_counts,
            "candidate_rows": sum(row["candidate_rows"] for row in families),
            "unmatched_cc_rows": sum(row["unmatched_cc_rows"] for row in families),
            "gt_only_signature_groups": sum(
                row["gt_only_signature_groups"] for row in families
            ),
        },
        "families": families,
        "generic_map_projection": generic_projection,
    }


def current_cc_digest() -> tuple[str, int, int]:
    comparator = load_comparator()
    comparator.clear_process_caches()
    materials = comparator.cached_cc_materials()
    cc_all = list(comparator.cached_expanded_cc_recipes())
    compact_rows, runtime_stats = load_compact_runtime_recipes(comparator)
    cc_all.extend(compact_rows)
    generic_cc, generic_runtime_stats = load_generic_runtime_rows(comparator)
    return (
        cc_projection_digest(comparator, cc_all),
        len(materials),
        len(cc_all),
        {
            **runtime_stats,
            "generic_digest": cc_projection_digest(
                comparator,
                [recipe for rows in generic_cc.values() for recipe in rows],
            ),
            **generic_runtime_stats,
        },
    )


def check_report() -> int:
    if not OUTPUT_PATH.is_file():
        print(f"{OUTPUT_PATH.relative_to(ROOT)} is missing; run --write")
        return 1
    pinned = load_json(OUTPUT_PATH)
    digest, material_count, recipe_count, runtime_stats = current_cc_digest()
    errors = []
    if pinned.get("cc_projection", {}).get("digest") != digest:
        errors.append("CC semantic projection changed")
    if pinned.get("cc_projection", {}).get("material_count") != material_count:
        errors.append("CC material count changed")
    if pinned.get("cc_projection", {}).get("normalized_recipe_count") != recipe_count:
        errors.append("CC normalized recipe count changed")
    for key, value in runtime_stats.items():
        if pinned.get("cc_projection", {}).get(key) != value:
            errors.append(f"runtime semantic row stat changed: {key}")
    if pinned.get("comparator", {}).get("sha256") != sha256_file(COMPARATOR_PATH):
        errors.append("semantic comparator source changed")
    if pinned.get("builder", {}).get("sha256") != sha256_file(BUILDER_PATH):
        errors.append("semantic coverage builder changed")
    if (
        REFERENCE_PATH.is_file()
        and pinned.get("reference", {}).get("sha256") != sha256_file(REFERENCE_PATH)
    ):
        errors.append("normalized GT6 reference changed")
    if errors:
        print("semantic coverage is stale:")
        print("\n".join(f"- {error}" for error in errors))
        print(
            "run python tools/waves/portfolio/gt6-full-coverage-reassessment/"
            "build_semantic_coverage.py --write"
        )
        return 1
    print("GT6 semantic coverage projection is current")
    return 0


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args(argv)
    if args.write == args.check:
        parser.error("use exactly one of --write or --check")
    if args.check:
        return check_report()
    report = build_report()
    OUTPUT_PATH.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT_PATH.write_text(
        json.dumps(report, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print(f"wrote {OUTPUT_PATH.relative_to(ROOT)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
