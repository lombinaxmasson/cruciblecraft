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


def cc_projection_digest(comparator: Any, recipes: list[Any]) -> str:
    rows = [recipe_projection_with(comparator, recipe) for recipe in recipes]
    rows.sort(key=stable_json)
    return sha256_text(stable_json(rows))


def reverse_gt_tier_counts(
    comparator: Any, family: str, cc: list[Any], gt: list[Any]
) -> dict[str, int]:
    """Classify normalized GT rows against the expanded CC projection.

    This is deliberately separate from the comparator's CC→GT rows. It makes
    the denominator visible in the sidecar, while staying at normalized-row
    granularity rather than pretending these rows are raw dump rows.
    """
    counts: Counter[str] = Counter()
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
            counts["EXACT"] += 1
            continue
        form = recipe.io_form_signature()
        if form is not None and form in cc_forms:
            counts["FORM_PATH"] += 1
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
        counts["SEMANTIC" if semantic else "NONE"] += 1
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
    gt_tiers = reverse_gt_tier_counts(comparator, family, cc, gt)
    return {
        "family": family,
        "gt_maps": list(comparator.FAMILIES[family]["gt_maps"]),
        "note": comparator.FAMILIES[family]["note"],
        "cc_normalized_rows": comparison["cc_count"],
        "gt_normalized_rows": comparison["gt_normalized_count"],
        "match_tiers": dict(buckets),
        "gt_match_tiers": gt_tiers,
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
    reference = load_json(REFERENCE_PATH)
    families = []
    for family in comparator.FAMILIES:
        gt = [
            comparator.recipe_from_reference(family, row)
            for row in reference["families"][family]
        ]
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
        "cc_projection": {
            "material_count": len(materials),
            "normalized_recipe_count": len(cc_all),
            "digest": cc_projection_digest(comparator, cc_all),
            **runtime_stats,
        },
        "summary": {
            "family_count": len(families),
            "cc_normalized_rows": sum(row["cc_normalized_rows"] for row in families),
            "gt_normalized_rows": sum(row["gt_normalized_rows"] for row in families),
            "tier_counts": tier_counts,
            "gt_tier_counts": gt_tier_counts,
            "candidate_rows": sum(row["candidate_rows"] for row in families),
            "unmatched_cc_rows": sum(row["unmatched_cc_rows"] for row in families),
            "gt_only_signature_groups": sum(
                row["gt_only_signature_groups"] for row in families
            ),
        },
        "families": families,
    }


def current_cc_digest() -> tuple[str, int, int]:
    comparator = load_comparator()
    comparator.clear_process_caches()
    materials = comparator.cached_cc_materials()
    cc_all = list(comparator.cached_expanded_cc_recipes())
    compact_rows, runtime_stats = load_compact_runtime_recipes(comparator)
    cc_all.extend(compact_rows)
    return (
        cc_projection_digest(comparator, cc_all),
        len(materials),
        len(cc_all),
        runtime_stats,
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
