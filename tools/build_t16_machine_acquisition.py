#!/usr/bin/env python3
"""Build the independent T16c selected-machine acquisition evidence."""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
from typing import Any

try:
    from tools import build_t15_machine_acquisition as acquisition_support
except ModuleNotFoundError:
    import build_t15_machine_acquisition as acquisition_support


ROOT = Path(__file__).resolve().parents[1]
TOOLS = ROOT / "tools"
BUILDER = Path(__file__).resolve()
POLICY = TOOLS / "t16_machine_acquisition_policy.json"
OUTPUT = TOOLS / "t16_machine_acquisition.json"
MACHINE_TIERS = (
    ROOT / "src/main/resources/data/cruciblecraft/machine_tiers.json"
)
MATERIAL_GATE = (
    ROOT
    / "src/main/resources/data/cruciblecraft/material_registration_gate.json"
)
MOD_ITEMS = (
    ROOT
    / "src/main/java/com/masson/cruciblecraft/registry/ModItems.java"
)
REQUIRED_PRODUCER_FORMS = {
    "bolt",
    "double_plate",
    "gear",
    "long_rod",
    "plate",
    "rod",
    "screw",
    "small_gear",
    "spring",
}


def load(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def stable(value: Any) -> str:
    return json.dumps(
        value, ensure_ascii=False, indent=2, sort_keys=True
    ) + "\n"


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def relative(path: Path) -> str:
    return path.relative_to(ROOT).as_posix()


def validate_policy(policy: dict[str, Any]) -> None:
    if (
        policy.get("schema_version") != 1
        or policy.get("status") != "T16C_MACHINE_ACQUISITION_POLICY"
        or policy.get("expected_set_source")
        != "independent_t16c_acceptance_policy"
        or policy.get("production_mapping_reused") is not False
    ):
        raise ValueError("T16c acquisition policy header drifted")
    selected_kinds = policy.get("selected_kinds")
    if (
        not isinstance(selected_kinds, list)
        or len(selected_kinds) != 5
        or len(set(selected_kinds)) != 5
    ):
        raise ValueError("T16c policy must independently name five kinds")
    casings = policy.get("casing_dependencies")
    machines = policy.get("machine_variant_recipes")
    if not isinstance(casings, dict) or len(casings) != 3:
        raise ValueError("T16c policy must name three casing dependencies")
    if not isinstance(machines, dict) or len(machines) != 15:
        raise ValueError("T16c policy must independently name 15 recipes")
    if set(policy.get("producer_rules") or {}) != REQUIRED_PRODUCER_FORMS:
        raise ValueError("T16c producer graph form set drifted")
    sources = policy.get("source_declarations")
    if not isinstance(sources, dict) or len(sources) != 4:
        raise ValueError("T16c source declaration set drifted")
    for identity, source in sources.items():
        if (
            acquisition_support.material_item(identity) is None
            or source.get("classification")
            != "registered_survival_source"
            or not str(source.get("evidence") or "").strip()
        ):
            raise ValueError(f"{identity}: invalid survival source declaration")


def selected_catalog_rows(
    policy: dict[str, Any],
    tiers: dict[str, Any],
) -> dict[str, dict[str, Any]]:
    variants = tiers.get("variants")
    if not isinstance(variants, list):
        raise ValueError("machine tier catalog variants must be a list")
    variant_ids = [row["id"] for row in variants]
    if len(variant_ids) != len(set(variant_ids)):
        raise ValueError("machine tier catalog variant ids are duplicated")
    selected_kinds = set(policy["selected_kinds"])
    rows = {
        row["id"]: row
        for row in variants
        if row.get("kind") in selected_kinds
    }
    if len(rows) != 15:
        raise ValueError("selected five-by-three catalog matrix is incomplete")
    if {row["kind"] for row in rows.values()} != selected_kinds:
        raise ValueError("selected kind set differs from the catalog")
    return rows


def validate_registrations_and_recipes(
    specifications: dict[str, dict[str, Any]],
    registrations: dict[str, str],
) -> tuple[dict[str, str], dict[str, dict[str, str]]]:
    hashes: dict[str, str] = {}
    item_rows: dict[str, dict[str, str]] = {}
    for recipe_id, spec in specifications.items():
        path, _ = acquisition_support.validate_recipe_document(
            recipe_id, spec
        )
        hashes[relative(path)] = sha256(path)
        registered = registrations.get(spec["item_field"])
        expected_path = spec["result"].partition(":")[2]
        if registered != expected_path:
            raise ValueError(
                f"{recipe_id}: ModItems.{spec['item_field']} drifted"
            )
        item_rows[spec["result"]] = {
            "field": spec["item_field"],
            "registered_id": f"cruciblecraft:{registered}",
        }
    if len(item_rows) != len(specifications):
        raise ValueError("T16c recipe result registrations are duplicated")
    return hashes, item_rows


def build(policy: dict[str, Any] | None = None) -> dict[str, Any]:
    policy = load(POLICY) if policy is None else policy
    validate_policy(policy)
    registration_document = load(MATERIAL_GATE)
    gate = registration_document.get("materials")
    if not isinstance(gate, dict):
        raise ValueError("material registration gate has no material map")
    registrations = acquisition_support.mod_item_registrations()
    selected_rows = selected_catalog_rows(policy, load(MACHINE_TIERS))
    casing_specs = policy["casing_dependencies"]
    machine_specs = policy["machine_variant_recipes"]
    expected_variants = {spec["result"] for spec in machine_specs.values()}
    if expected_variants != set(selected_rows):
        raise ValueError(
            "independent T16c recipe set differs from selected variants"
        )

    all_specs = {**casing_specs, **machine_specs}
    recipe_hashes, item_rows = validate_registrations_and_recipes(
        all_specs, registrations
    )
    casing_by_result = {
        spec["result"]: recipe_id
        for recipe_id, spec in casing_specs.items()
    }
    if len(casing_by_result) != 3:
        raise ValueError("T16c casing dependency results are duplicated")
    for recipe_id, spec in machine_specs.items():
        selected_casing = spec["selected_casing"]
        casing_recipe_id = casing_by_result.get(selected_casing)
        if casing_recipe_id is None:
            raise ValueError(f"{recipe_id}: casing is outside T16c")
        if list(spec["key"].values()).count(selected_casing) != 1:
            raise ValueError(
                f"{recipe_id}: selected casing must occupy one key slot"
            )
        casing_spec = casing_specs[casing_recipe_id]
        casing_material, _ = (
            acquisition_support.validate_registered_material_item(
                next(iter(casing_spec["key"].values())), gate
            )
        )
        if (
            selected_rows[spec["result"]]["material"]
            != f"cruciblecraft:{casing_material}"
        ):
            raise ValueError(
                f"{recipe_id}: casing material differs from tier material"
            )

    closure = acquisition_support.Closure(
        policy, gate, casing_by_result, casing_specs
    )
    casing_rows = []
    for recipe_id, spec in sorted(casing_specs.items()):
        ingredients = acquisition_support.ingredient_rows(spec, closure)
        casing_rows.append({
            "recipe_id": recipe_id,
            "result_item": spec["result"],
            "recipe_path": relative(
                acquisition_support.recipe_path(recipe_id)
            ),
            "ingredients": ingredients,
            "reachable": all(row["reachable"] for row in ingredients),
        })
    machine_rows = []
    for recipe_id, spec in sorted(machine_specs.items()):
        ingredients = acquisition_support.ingredient_rows(spec, closure)
        machine_rows.append({
            "recipe_id": recipe_id,
            "result_item": spec["result"],
            "selected_casing": spec["selected_casing"],
            "recipe_path": relative(
                acquisition_support.recipe_path(recipe_id)
            ),
            "pattern": spec["pattern"],
            "ingredients": ingredients,
            "reachable": all(row["reachable"] for row in ingredients),
        })
    unreachable = [
        {
            "recipe_id": recipe["recipe_id"],
            "item": ingredient["item"],
            "reason": ingredient.get(
                "reason", "producer input is unreachable"
            ),
        }
        for recipe in (*casing_rows, *machine_rows)
        for ingredient in recipe["ingredients"]
        if not ingredient["reachable"]
    ]
    if unreachable:
        raise ValueError(
            f"T16c acquisition has unreachable operands: {unreachable}"
        )
    source_rows = [
        closure.resolve(identity)
        for identity in sorted(policy["source_declarations"])
    ]
    if not all(row["reachable"] for row in source_rows):
        raise ValueError("T16c source declarations are not all reachable")

    currentness = {
        "owned_inputs": {
            relative(BUILDER): sha256(BUILDER),
            relative(POLICY): sha256(POLICY),
        },
        "support_implementation": {
            relative(acquisition_support.BUILDER):
                sha256(acquisition_support.BUILDER),
        },
        "registration_sources": {
            relative(MATERIAL_GATE): sha256(MATERIAL_GATE),
            relative(MOD_ITEMS): sha256(MOD_ITEMS),
            relative(MACHINE_TIERS): sha256(MACHINE_TIERS),
        },
        "crafting_recipes": dict(sorted(recipe_hashes.items())),
        "producer_rules": dict(sorted(closure.producer_hashes.items())),
        "source_materials": dict(sorted(closure.material_hashes.items())),
    }
    machine_ingredient_rows = [
        ingredient
        for recipe in machine_rows
        for ingredient in recipe["ingredients"]
    ]
    return {
        "schema_version": 1,
        "status": "T16C_MACHINE_ACQUISITION_READY",
        "delivery": "T16c",
        "expected_set": {
            "source": policy["expected_set_source"],
            "production_mapping_reused": policy[
                "production_mapping_reused"
            ],
            "selected_kinds": sorted(policy["selected_kinds"]),
            "machine_variant_recipe_ids": sorted(machine_specs),
            "casing_dependency_recipe_ids": sorted(casing_specs),
        },
        "counts": {
            "selected_kinds": 5,
            "selected_variants": len(machine_rows),
            "casing_dependencies": len(casing_rows),
            "machine_ingredient_rows": len(machine_ingredient_rows),
            "registered_machine_results": len(machine_specs),
            "producer_forms": len(policy["producer_rules"]),
            "source_declarations": len(source_rows),
            "unreachable": len(unreachable),
        },
        "item_registrations": dict(sorted(item_rows.items())),
        "source_declarations": source_rows,
        "casing_dependencies": casing_rows,
        "machine_variant_recipes": machine_rows,
        "unreachable": unreachable,
        "currentness": currentness,
    }


def check() -> list[str]:
    encoded = stable(build())
    if not OUTPUT.is_file():
        return [f"missing generated file: {relative(OUTPUT)}"]
    if OUTPUT.read_text(encoding="utf-8") != encoded:
        return [f"stale generated file: {relative(OUTPUT)}"]
    return []


def write() -> dict[str, Any]:
    document = build()
    OUTPUT.write_text(stable(document), encoding="utf-8", newline="\n")
    return document


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--check",
        action="store_true",
        help="fail if the committed T16c acquisition artifact is stale",
    )
    args = parser.parse_args()
    try:
        if args.check:
            errors = check()
            if errors:
                raise ValueError("; ".join(errors))
            document = load(OUTPUT)
        else:
            document = write()
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"T16c acquisition failed: {error}")
        return 1
    print(json.dumps({
        "status": document["status"],
        "variants": document["counts"]["selected_variants"],
        "unreachable": document["counts"]["unreachable"],
    }, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
