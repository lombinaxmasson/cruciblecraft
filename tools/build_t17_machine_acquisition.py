#!/usr/bin/env python3
"""Build independent T17c HU acquisition and Electrolyzer evidence."""
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
try:
    from tools import t36_common as t36
except ModuleNotFoundError:
    import t36_common as t36


ROOT = Path(__file__).resolve().parents[1]
TOOLS = ROOT / "tools"
BUILDER = Path(__file__).resolve()
POLICY = TOOLS / "t17_machine_acquisition_policy.json"
OUTPUT = TOOLS / "t17_machine_acquisition.json"
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
    "double_plate",
    "double_wire",
    "octuple_wire",
    "plate",
    "quadruple_wire",
    "wire",
}
EXPECTED_CASING_BY_MATERIAL = {
    "cruciblecraft:steel": "cruciblecraft:steel_double_machine_casing",
    "cruciblecraft:invar": "cruciblecraft:steel_double_machine_casing",
    "cruciblecraft:titanium":
        "cruciblecraft:titanium_double_machine_casing",
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
        or policy.get("status") != "T17C_MACHINE_ACQUISITION_POLICY"
        or policy.get("expected_set_source")
        != "independent_t17c_acceptance_policy"
        or policy.get("production_mapping_reused") is not False
    ):
        raise ValueError("T17c acquisition policy header drifted")
    selected = policy.get("selected_kinds")
    if (
        not isinstance(selected, list)
        or len(selected) != 3
        or len(set(selected)) != 3
    ):
        raise ValueError("T17c policy must independently name three HU kinds")
    if len(policy.get("machine_variant_recipes") or {}) != 9:
        raise ValueError("T17c policy must independently name nine HU recipes")
    if len(policy.get("casing_dependencies") or {}) != 2:
        raise ValueError("T17c policy must name two existing casing recipes")
    if len(policy.get("preimplemented_reference_recipes") or {}) != 3:
        raise ValueError(
            "T17c policy must name three preimplemented Electrolyzers"
        )
    if set(policy.get("producer_rules") or {}) != REQUIRED_PRODUCER_FORMS:
        raise ValueError("T17c producer graph form set drifted")
    sources = policy.get("source_declarations")
    if not isinstance(sources, dict) or len(sources) != 7:
        raise ValueError("T17c material source declarations drifted")
    for identity, source in sources.items():
        if (
            acquisition_support.material_item(identity) is None
            or source.get("classification")
            != "registered_survival_source"
            or not str(source.get("evidence") or "").strip()
        ):
            raise ValueError(f"{identity}: invalid survival source")
    concrete = policy.get("concrete_sources")
    if not isinstance(concrete, dict) or len(concrete) != 3:
        raise ValueError("T17c concrete source declarations drifted")
    for identity, source in concrete.items():
        if (
            ":" not in identity
            or source.get("classification") not in {
                "existing_survival_recipe_chain",
                "minecraft_survival_source",
            }
            or not source.get("evidence")
        ):
            raise ValueError(f"{identity}: invalid concrete source")


def selected_catalog_rows(
    policy: dict[str, Any],
    tiers: dict[str, Any],
) -> dict[str, dict[str, Any]]:
    variants = t36.opening_variants(tiers)
    if not isinstance(variants, list) or len(variants) != 33:
        raise ValueError("machine tier catalog must contain 33 variants")
    kinds = set(policy["selected_kinds"])
    rows = {
        row["id"]: row
        for row in variants
        if row.get("kind") in kinds
    }
    if (
        len(rows) != 9
        or {row["kind"] for row in rows.values()} != kinds
        or any(row.get("energy") != "HEAT" for row in rows.values())
    ):
        raise ValueError("selected three-by-three HU catalog is incomplete")
    return rows


class T17Closure(acquisition_support.Closure):
    def resolve(
        self,
        identity: str,
        visiting: tuple[str, ...] = (),
    ) -> dict[str, Any]:
        concrete = self.policy["concrete_sources"].get(identity)
        if concrete is None:
            return super().resolve(identity, visiting)
        cached = self.cache.get(identity)
        if cached is not None:
            return cached
        evidence_hashes: dict[str, str] = {}
        for evidence in concrete["evidence"]:
            if evidence.startswith("minecraft:"):
                continue
            path = ROOT / evidence
            document = load(path)
            if identity == "cruciblecraft:crucible":
                encoded = json.dumps(document, sort_keys=True)
                if (
                    "cruciblecraft:raw_ceramic_crucible" not in encoded
                    and "cruciblecraft:crucible" not in encoded
                ):
                    raise ValueError(
                        f"{evidence}: crucible recipe chain drifted"
                    )
            evidence_hashes[evidence] = sha256(path)
            self.material_hashes[evidence] = evidence_hashes[evidence]
        row = {
            "item": identity,
            "classification": concrete["classification"],
            "producer": list(concrete["evidence"]),
            "registered": True,
            "reachable": True,
            "inputs": [],
        }
        self.cache[identity] = row
        return row


def validate_recipe_set(
    specifications: dict[str, dict[str, Any]],
    registrations: dict[str, str],
) -> tuple[dict[str, str], dict[str, dict[str, str]]]:
    hashes: dict[str, str] = {}
    items: dict[str, dict[str, str]] = {}
    for recipe_id, spec in specifications.items():
        path, _ = acquisition_support.validate_recipe_document(
            recipe_id, spec
        )
        hashes[relative(path)] = sha256(path)
        expected_path = spec["result"].partition(":")[2]
        if registrations.get(spec["item_field"]) != expected_path:
            raise ValueError(
                f"{recipe_id}: ModItems.{spec['item_field']} drifted"
            )
        items[spec["result"]] = {
            "field": spec["item_field"],
            "registered_id": spec["result"],
        }
    if len(items) != len(specifications):
        raise ValueError("T17c recipe result registrations are duplicated")
    return hashes, items


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
    reference_specs = policy["preimplemented_reference_recipes"]
    if {spec["result"] for spec in machine_specs.values()} != set(
        selected_rows
    ):
        raise ValueError(
            "independent T17c recipe set differs from selected variants"
        )

    generated_hashes, generated_items = validate_recipe_set(
        {**casing_specs, **machine_specs}, registrations
    )
    reference_hashes, reference_items = validate_recipe_set(
        reference_specs, registrations
    )
    casing_by_result = {
        spec["result"]: recipe_id
        for recipe_id, spec in casing_specs.items()
    }
    if len(casing_by_result) != 2:
        raise ValueError("T17c casing dependency results are duplicated")
    for recipe_id, spec in machine_specs.items():
        row = selected_rows[spec["result"]]
        expected_casing = EXPECTED_CASING_BY_MATERIAL.get(row["material"])
        if spec["selected_casing"] != expected_casing:
            raise ValueError(
                f"{recipe_id}: casing differs from the T17c tier policy"
            )
        if list(spec["key"].values()).count(expected_casing) != 1:
            raise ValueError(
                f"{recipe_id}: selected casing must occupy one key slot"
            )
        material_path = row["material"].partition(":")[2]
        if spec["key"].get("P") != (
            f"cruciblecraft:{material_path}/plate"
        ):
            raise ValueError(
                f"{recipe_id}: tier plate differs from catalog material"
            )

    closure = T17Closure(
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
            f"T17c acquisition has unreachable operands: {unreachable}"
        )
    source_rows = [
        closure.resolve(identity)
        for identity in sorted(policy["source_declarations"])
    ]
    concrete_rows = [
        closure.resolve(identity)
        for identity in sorted(policy["concrete_sources"])
    ]
    if not all(
        row["reachable"] for row in (*source_rows, *concrete_rows)
    ):
        raise ValueError("T17c declared sources are not all reachable")

    references = [
        {
            "recipe_id": recipe_id,
            "result_item": spec["result"],
            "selected_casing": spec["selected_casing"],
            "recipe_path": relative(
                acquisition_support.recipe_path(recipe_id)
            ),
            "preimplemented": True,
            "generated_by_t17c": False,
        }
        for recipe_id, spec in sorted(reference_specs.items())
    ]
    reference_results = {row["result_item"] for row in references}
    if reference_results & set(selected_rows):
        raise ValueError("Electrolyzer evidence overlaps T17c HU generation")

    recipe_hashes = {**generated_hashes, **reference_hashes}
    item_rows = {**generated_items, **reference_items}
    ingredient_rows = [
        ingredient
        for recipe in machine_rows
        for ingredient in recipe["ingredients"]
    ]
    return {
        "schema_version": 1,
        "status": "T17C_MACHINE_ACQUISITION_READY",
        "delivery": "T17c",
        "expected_set": {
            "source": policy["expected_set_source"],
            "production_mapping_reused":
                policy["production_mapping_reused"],
            "selected_kinds": sorted(policy["selected_kinds"]),
            "machine_variant_recipe_ids": sorted(machine_specs),
            "casing_dependency_recipe_ids": sorted(casing_specs),
        },
        "counts": {
            "selected_kinds": 3,
            "selected_variants": len(machine_rows),
            "casing_dependencies": len(casing_rows),
            "machine_ingredient_rows": len(ingredient_rows),
            "registered_machine_results": len(machine_specs),
            "producer_forms": len(policy["producer_rules"]),
            "material_source_declarations": len(source_rows),
            "concrete_source_declarations": len(concrete_rows),
            "preimplemented_reference_variants": len(references),
            "gt_recipe_rows_added": 0,
            "unreachable": len(unreachable),
        },
        "item_registrations": dict(sorted(item_rows.items())),
        "source_declarations": source_rows,
        "concrete_sources": concrete_rows,
        "casing_dependencies": casing_rows,
        "machine_variant_recipes": machine_rows,
        "preimplemented_references": references,
        "unreachable": unreachable,
        "currentness": {
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
            "producer_rules":
                dict(sorted(closure.producer_hashes.items())),
            "source_materials":
                dict(sorted(closure.material_hashes.items())),
        },
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
    encoded = stable(document).encode("utf-8")
    tmp = OUTPUT.with_name(OUTPUT.name + ".tmp")
    tmp.write_bytes(encoded)
    tmp.replace(OUTPUT)
    return document


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true")
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
        print(f"T17c acquisition failed: {error}")
        return 1
    print(json.dumps({
        "status": document["status"],
        "variants": document["counts"]["selected_variants"],
        "preimplemented": document["counts"][
            "preimplemented_reference_variants"
        ],
        "unreachable": document["counts"]["unreachable"],
    }, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
