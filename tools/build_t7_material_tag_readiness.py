#!/usr/bin/env python3
"""Build the deterministic T7 material-tag readiness ledger."""
from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
from collections import Counter
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
TOOLS = ROOT / "tools"
POLICY = TOOLS / "t7_material_tag_policy.json"
OUTPUT = TOOLS / "t7_material_tag_readiness.json"
REFERENCE_METADATA = TOOLS / "gt6_reference_metadata.json"
MATERIAL_ROOT = ROOT / "src/main/resources/data/cruciblecraft/materials"
MATERIAL_INDEX = MATERIAL_ROOT / "index.json"
REGISTRATION_GATE = (
    ROOT / "src/main/resources/data/cruciblecraft/material_registration_gate.json"
)
RULE_EXPRESSION = (
    ROOT / "src/main/java/com/masson/cruciblecraft/recipe/rule/RuleExpression.java"
)
RULE_EXPANSION = (
    ROOT
    / "src/main/java/com/masson/cruciblecraft/recipe/rule/MaterialRuleExpansion.java"
)
ENERGY_TYPE = (
    ROOT / "src/main/java/com/masson/cruciblecraft/api/energy/EnergyType.java"
)
CABLE_TRAVERSAL = (
    ROOT
    / "src/main/java/com/masson/cruciblecraft/energy/cable/"
    "CableNetworkTraversal.java"
)
METADATA_MODEL = (
    ROOT
    / "src/main/java/com/masson/cruciblecraft/material/def/"
    "GT6MaterialMetadata.java"
)
CLASSIFICATIONS = {
    "rule-input",
    "build-time-only",
    "not-applicable",
    "deferred",
}


def load(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def stable_json(value: Any) -> str:
    return json.dumps(
        value, ensure_ascii=False, indent=2, sort_keys=True
    ) + "\n"


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def stable_hash(value: Any) -> str:
    payload = json.dumps(
        value, ensure_ascii=False, sort_keys=True, separators=(",", ":")
    )
    return hashlib.sha256(payload.encode("utf-8")).hexdigest()


def load_materials(index: list[str]) -> list[dict[str, Any]]:
    result = []
    seen = set()
    for filename in index:
        document = load(MATERIAL_ROOT / filename)
        material_id = str(document["id"])
        if filename != f"{material_id}.json" or material_id in seen:
            raise ValueError(f"invalid material index entry: {filename}")
        seen.add(material_id)
        result.append(document)
    return result


def tags(document: dict[str, Any]) -> set[str]:
    metadata = document.get("gt6_metadata") or {}
    return set(map(str, metadata.get("material_tags") or []))


def validate_policy(
    policy: dict[str, Any], actual_counts: Counter[str]
) -> dict[str, dict[str, Any]]:
    result = {}
    for row in policy.get("tags") or []:
        tag = str(row.get("tag") or "")
        if not tag.startswith(("PROCESSING.", "PROPERTIES.")):
            raise ValueError(f"invalid T7 tag domain: {tag}")
        if tag in result:
            raise ValueError(f"duplicate T7 tag policy row: {tag}")
        if row.get("classification") not in CLASSIFICATIONS:
            raise ValueError(f"{tag}: invalid classification")
        if not str(row.get("reason") or "").strip():
            raise ValueError(f"{tag}: classification reason is required")
        if not str(row.get("expected_consumer") or "").strip():
            raise ValueError(f"{tag}: expected consumer is required")
        expected = row.get("expected_material_count")
        if expected != actual_counts.get(tag, 0):
            raise ValueError(
                f"{tag}: material count drifted "
                f"{expected} != {actual_counts.get(tag, 0)}"
            )
        result[tag] = row
    if set(result) != set(actual_counts):
        raise ValueError(
            "T7 tag classification is not closed: "
            f"missing={sorted(set(actual_counts) - set(result))}, "
            f"stale={sorted(set(result) - set(actual_counts))}"
        )
    return result


def registered_rule_set(
    materials: list[dict[str, Any]],
    registered: dict[str, list[str]],
    input_form: str,
) -> list[str]:
    result = []
    for document in materials:
        material_id = str(document["id"])
        forms = set(registered.get(material_id) or [])
        if (
            "PROCESSING.MORTAR_GRINDABLE" in tags(document)
            and input_form in forms
            and "dust" in forms
        ):
            result.append(material_id)
    return sorted(result)


def material_tree_hash(index: list[str]) -> str:
    return stable_hash(
        [(filename, sha256(MATERIAL_ROOT / filename)) for filename in index]
    )


def validate_runtime_contract(policy: dict[str, Any]) -> dict[str, Any]:
    expression = RULE_EXPRESSION.read_text(encoding="utf-8")
    expansion = RULE_EXPANSION.read_text(encoding="utf-8")
    energy = ENERGY_TYPE.read_text(encoding="utf-8")
    cable = CABLE_TRAVERSAL.read_text(encoding="utf-8")
    metadata_model = METADATA_MODEL.read_text(encoding="utf-8")
    for token in ('case "material.tag"', 'case "has_registered"'):
        if token not in expression:
            raise ValueError(f"rule language is missing {token}")
    for token in (
        'case "material.thermal.melting_point"',
        'case "material.thermal.boiling_point"',
        'case "material.thermal.density"',
        'case "material.explosion_damage"',
        'case "material.heat_damage"',
    ):
        if token not in expansion:
            raise ValueError(f"rule language is missing {token}")
    decision = policy["energy_type_decision"]
    for name in decision["kept"]:
        if not re.search(rf"\b{re.escape(name)}\b", energy):
            raise ValueError(f"kept EnergyType is missing: {name}")
    for name in decision["removed"]:
        if re.search(rf"\b{re.escape(name)}\b", energy):
            raise ValueError(f"removed EnergyType is still declared: {name}")
    if "if (lossPerMeter == 0L)" not in cable:
        raise ValueError("O-22 zero-loss cable passthrough is missing")
    if "lossPerMeter < 0" not in metadata_model:
        raise ValueError("O-22 metadata still rejects zero cable loss")
    return {
        "tag_predicate": "material.tag",
        "registered_form_predicate": "has_registered",
        "thermal_numeric_comparison": True,
        "damage_numeric_comparison": True,
        "energy_types": list(decision["kept"]),
        "zero_loss_cable_domain": True,
    }


def build(policy_override: dict[str, Any] | None = None) -> dict[str, Any]:
    policy = policy_override or load(POLICY)
    reference = load(REFERENCE_METADATA)
    index = load(MATERIAL_INDEX)
    materials = load_materials(index)
    gate = load(REGISTRATION_GATE)
    registered = gate["materials"]
    if set(registered) != {str(row["id"]) for row in materials}:
        raise ValueError("registration gate material domain drifted")
    source = policy["gt6_source"]
    expected_version = reference["gt6_version"]
    expected_revision = expected_version.rsplit("g", 1)[-1]
    if source["version"] != expected_version:
        raise ValueError("T7 GT6 version does not match reference metadata")
    if not source["revision"].startswith(expected_revision):
        raise ValueError("T7 GT6 revision does not match reference metadata")

    relevant_counts: Counter[str] = Counter()
    formula_count = 0
    explosion_nonzero = 0
    heat_nonzero = 0
    for document in materials:
        metadata = document.get("gt6_metadata") or {}
        relevant_counts.update(
            tag
            for tag in tags(document)
            if tag.startswith(("PROCESSING.", "PROPERTIES."))
        )
        formula_count += bool(metadata.get("formula"))
        explosion_nonzero += float(metadata.get("explosion_damage", 0)) != 0
        heat_nonzero += float(metadata.get("heat_damage", 0)) != 0
    policy_by_tag = validate_policy(policy, relevant_counts)

    mortar_tagged = sorted(
        str(row["id"])
        for row in materials
        if "PROCESSING.MORTAR_GRINDABLE" in tags(row)
    )
    mortar_ingot = sorted(
        material_id
        for material_id in mortar_tagged
        if "ingot" in set(registered[material_id])
    )
    ingot_to_dust = registered_rule_set(materials, registered, "ingot")
    gem_to_dust = registered_rule_set(materials, registered, "gem")
    counts = {
        "material_count": len(materials),
        "processing_tag_count": sum(
            tag.startswith("PROCESSING.") for tag in relevant_counts
        ),
        "property_tag_count": sum(
            tag.startswith("PROPERTIES.") for tag in relevant_counts
        ),
        "tag_assignment_count": sum(relevant_counts.values()),
        "formula_count": formula_count,
        "nonzero_explosion_damage_count": explosion_nonzero,
        "nonzero_heat_damage_count": heat_nonzero,
        "mortar_tagged_material_count": len(mortar_tagged),
        "mortar_registered_ingot_count": len(mortar_ingot),
        "mortar_ingot_to_dust_count": len(ingot_to_dust),
        "mortar_gem_to_dust_count": len(gem_to_dust),
        "new_mortar_rule_expansion_count": (
            len(ingot_to_dust) + len(gem_to_dust)
        ),
    }
    if counts != policy["acceptance"]:
        raise ValueError(
            f"T7 acceptance counts drifted: {counts} != "
            f"{policy['acceptance']}"
        )
    classifications = Counter(
        row["classification"] for row in policy_by_tag.values()
    )
    publication = policy["runtime_publication_acceptance"]
    if (
        publication["pre_t7_mortar_recipes"]
        + publication["t7_added_mortar_recipes"]
        != publication["post_t7_mortar_recipes"]
        or publication["pre_t7_all_published_recipes"]
        + publication["t7_added_mortar_recipes"]
        != publication["post_t7_all_published_recipes"]
        or publication["post_t7_all_published_recipes"]
        > publication["all_published_recipe_budget"]
        or publication["shadowed_input_signatures"] != 0
    ):
        raise ValueError("T7 runtime publication acceptance is inconsistent")
    inputs = [
        POLICY,
        REFERENCE_METADATA,
        MATERIAL_INDEX,
        REGISTRATION_GATE,
        RULE_EXPRESSION,
        RULE_EXPANSION,
        ENERGY_TYPE,
        CABLE_TRAVERSAL,
        METADATA_MODEL,
    ]
    return {
        "schema_version": 1,
        "gate": policy["gate"],
        "status": "READY",
        "generated_by": "tools/build_t7_material_tag_readiness.py",
        "gt6_source": source,
        "input_sha256": {
            path.relative_to(ROOT).as_posix(): sha256(path)
            for path in inputs
        },
        "material_directory_sha256": material_tree_hash(index),
        "classification_policy": policy["classification_policy"],
        "counts": counts,
        "classification_counts": dict(sorted(classifications.items())),
        "classified": len(policy_by_tag),
        "unclassified": 0,
        "tag_counts": dict(sorted(relevant_counts.items())),
        "rule_language": validate_runtime_contract(policy),
        "energy_type_decision": policy["energy_type_decision"],
        "runtime_publication_acceptance": publication,
        "mortar_rules": {
            "ingot_to_dust": {
                "count": len(ingot_to_dust),
                "materials": ingot_to_dust,
            },
            "gem_to_dust": {
                "count": len(gem_to_dust),
                "materials": gem_to_dust,
            },
            "tagged_registered_ingot_without_dust": sorted(
                set(mortar_ingot) - set(ingot_to_dust)
            ),
        },
        "tags": [
            {
                **row,
                "material_count": relevant_counts[tag],
            }
            for tag, row in sorted(policy_by_tag.items())
        ],
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--check",
        action="store_true",
        help="fail if the committed T7 readiness ledger is stale",
    )
    args = parser.parse_args()
    try:
        encoded = stable_json(build())
        if args.check:
            if not OUTPUT.is_file():
                raise ValueError(f"missing committed ledger: {OUTPUT}")
            if OUTPUT.read_text(encoding="utf-8") != encoded:
                raise ValueError("committed T7 readiness ledger is stale")
        else:
            OUTPUT.write_text(encoded, encoding="utf-8", newline="\n")
        print(json.dumps({
            "output": OUTPUT.relative_to(ROOT).as_posix(),
            "status": json.loads(encoded)["status"],
        }, sort_keys=True))
        return 0
    except (OSError, ValueError, KeyError, TypeError, json.JSONDecodeError) as error:
        print(f"T7 material-tag readiness failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
