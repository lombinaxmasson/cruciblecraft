#!/usr/bin/env python3
"""Build the reproducible T4 tool-domain and form-closure readiness ledger."""
from __future__ import annotations

import argparse
import hashlib
import json
import math
import sys
from collections import Counter
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))
TOOLS = ROOT / "tools"
MATERIALS = (
    ROOT / "src/main/resources/data/cruciblecraft/materials"
)
GATE = (
    ROOT
    / "src/main/resources/data/cruciblecraft/material_registration_gate.json"
)
POLICY = TOOLS / "t4_tool_policy.json"
OUTPUT = TOOLS / "t4_tool_readiness.json"


def load(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def stable_json(value: Any) -> str:
    return json.dumps(value, ensure_ascii=False, indent=2, sort_keys=True) + "\n"


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def load_materials() -> dict[str, dict[str, Any]]:
    documents: dict[str, dict[str, Any]] = {}
    for filename in load(MATERIALS / "index.json"):
        document = load(MATERIALS / filename)
        material_id = document["id"]
        if material_id in documents:
            raise ValueError(f"duplicate material id {material_id}")
        documents[material_id] = document
    return documents


def identity_references(value: Any) -> list[str]:
    if isinstance(value, dict):
        references = []
        if "identity_use" in value:
            references.append(str(value["identity_use"]))
        for child in value.values():
            references.extend(identity_references(child))
        return references
    if isinstance(value, list):
        references = []
        for child in value:
            references.extend(identity_references(child))
        return references
    return []


def identity_matches(
    material_id: str,
    identity_use: str,
    policy: dict[str, Any],
) -> bool:
    row = policy["condition_semantics"]["identity_uses"][identity_use]
    equal = material_id == str(row["material"])
    operator = row["operator"]
    if operator == "equal":
        return equal
    if operator == "not_equal":
        return not equal
    raise ValueError(f"unknown identity operator {operator!r}")


def condition_matches(
    condition: dict[str, Any],
    material_id: str,
    tags: set[str],
    tool: dict[str, Any],
    policy: dict[str, Any],
) -> bool:
    if "all" in condition:
        return all(
            condition_matches(row, material_id, tags, tool, policy)
            for row in condition["all"]
        )
    if "any" in condition:
        return any(
            condition_matches(row, material_id, tags, tool, policy)
            for row in condition["any"]
        )
    if "identity_use" in condition:
        return identity_matches(
            material_id, str(condition["identity_use"]), policy
        )
    if "tag" in condition:
        return (str(condition["tag"]) in tags) == bool(condition["present"])
    if "field" in condition:
        fields = {
            "tool.types": int(tool["types"]),
            "tool.quality": int(tool["quality"]),
        }
        field = str(condition["field"])
        if field not in fields:
            raise ValueError(f"unknown T4 condition field {field!r}")
        value = fields[field]
        return (
            ("minimum" not in condition or value >= int(condition["minimum"]))
            and (
                "maximum" not in condition
                or value <= int(condition["maximum"])
            )
        )
    raise ValueError(f"invalid T4 condition {condition!r}")


def merged_inputs(
    rows: list[dict[str, Any]],
    form_mapping: dict[str, str],
) -> list[dict[str, Any]]:
    counts: Counter[str] = Counter()
    for row in rows:
        form = form_mapping.get(str(row["form"]), str(row["form"]))
        counts[form] += int(row["count"])
    return [
        {"form": form, "count": count}
        for form, count in sorted(counts.items())
    ]


def tool_patterns(
    tool_id: str,
    policy: dict[str, Any],
) -> list[dict[str, Any]]:
    strategy = policy["port_recipe_strategy"]
    facts = policy["gt6_recipe_facts"]["tools"][tool_id]
    mapping = strategy["source_to_registered_form"]
    patterns: dict[str, dict[str, Any]] = {}
    if "full_patterns" in facts:
        source_patterns = facts["full_patterns"]
        assembly = None
    else:
        source_patterns = facts["head_patterns"]
        assembly = facts["full_assembly"]
    for source in source_patterns:
        inputs = list(source["material_inputs"])
        handle_count = int(
            source.get(
                "handle_count",
                0 if assembly is None else assembly["handle_count"],
            )
        )
        if handle_count:
            inputs.append(
                {
                    "form": assembly["handle_form"],
                    "count": handle_count,
                }
            )
        patterns[str(source["id"])] = {
            "id": str(source["id"]),
            "kind": "gt6_full" if assembly is None else "flattened_gt6_head",
            "material_inputs": merged_inputs(inputs, mapping),
            "handle_count": handle_count,
            "catalysts": list(source.get("catalysts") or []),
            "source_rows": list(source["rows"]),
        }
    for row in strategy["material_identity_exceptions"]:
        if row["tool"] != tool_id:
            continue
        patterns[str(row["pattern"])] = {
            "id": str(row["pattern"]),
            "kind": "material_identity_exception",
            "identity_use": str(row["identity_use"]),
            "material_inputs": merged_inputs(
                list(row["material_inputs"]), mapping
            ),
            "handle_count": int(row["handle_count"]),
            "catalysts": list(row.get("catalysts") or []),
            "source_rows": [],
        }
    precedence = strategy["tool_pattern_precedence"][tool_id]
    if len(precedence) != len(set(precedence)):
        raise ValueError(f"duplicate T4 pattern precedence for {tool_id}")
    if set(precedence) != set(patterns):
        raise ValueError(
            f"T4 pattern precedence mismatch for {tool_id}: "
            f"{precedence!r} vs {sorted(patterns)!r}"
        )
    return [patterns[pattern_id] for pattern_id in precedence]


def pattern_is_closed(pattern: dict[str, Any], forms: set[str]) -> bool:
    return {
        str(row["form"]) for row in pattern["material_inputs"]
    } <= forms


def choose_pattern(
    material_id: str,
    forms: set[str],
    patterns: list[dict[str, Any]],
    policy: dict[str, Any],
) -> str | None:
    exception_patterns = [
        pattern
        for pattern in patterns
        if "identity_use" in pattern
        and identity_matches(
            material_id, str(pattern["identity_use"]), policy
        )
    ]
    if len(exception_patterns) > 1:
        raise ValueError(
            f"multiple T4 identity patterns match {material_id}"
        )
    candidates = exception_patterns or [
        pattern for pattern in patterns if "identity_use" not in pattern
    ]
    for pattern in candidates:
        if pattern_is_closed(pattern, forms):
            return str(pattern["id"])
    return None


def pattern_by_id(
    patterns: list[dict[str, Any]], pattern_id: str
) -> dict[str, Any]:
    return next(pattern for pattern in patterns if pattern["id"] == pattern_id)


def mining_tier(quality: int, policy: dict[str, Any]) -> str:
    for row in policy["mining_level_policy"]["quality_to_vanilla_tier"]:
        if int(row["quality_min"]) <= quality <= int(row["quality_max"]):
            return str(row["tier"])
    raise ValueError(f"tool quality {quality} has no mining tier strategy")


def max_damage(durability: Any, policy: dict[str, Any]) -> int:
    cap = int(policy["durability_policy"]["non_finite_or_gt_int_max"])
    value = float(durability)
    if not math.isfinite(value) or value >= cap:
        return cap
    return max(1, min(cap, round(value)))


def mining_speed(speed: Any, policy: dict[str, Any]) -> float:
    strategy = policy["mining_speed_policy"]
    return max(
        float(strategy["minimum"]),
        min(float(strategy["maximum"]), float(speed)),
    )


def catalyst_inputs(
    tool_id: str,
    pattern: dict[str, Any],
    policy: dict[str, Any],
) -> list[dict[str, Any]]:
    strategy = policy["port_recipe_strategy"]
    result = [
        {
            **strategy["catalyst_mapping"][catalyst],
            "source_catalyst": catalyst,
        }
        for catalyst in pattern["catalysts"]
    ]
    result.append(
        {
            "item": (
                strategy["pattern_selector"]["item_prefix"] + tool_id
            ),
            "action": strategy["pattern_selector"]["action"],
            "source_catalyst": "tool_pattern_selector",
        }
    )
    return result


def recipe_signature_input(
    tool_id: str,
    material_id: str,
    pattern: dict[str, Any],
    policy: dict[str, Any],
) -> dict[str, Any]:
    ingredients = [
        {
            "ingredient": f"material_form:{row['form']}:{material_id}",
            "count": int(row["count"]),
            "action": "consume",
        }
        for row in pattern["material_inputs"]
    ]
    for catalyst in catalyst_inputs(tool_id, pattern, policy):
        identity = str(catalyst["item"])
        if "material" in catalyst:
            identity += f"#tool_material={catalyst['material']}"
        ingredients.append(
            {
                "ingredient": identity,
                "count": 0,
                "action": str(catalyst["action"]),
                **(
                    {"damage": int(catalyst["damage"])}
                    if "damage" in catalyst
                    else {}
                ),
            }
        )
    return {
        "ingredients": sorted(
            ingredients,
            key=lambda row: stable_json(row),
        )
    }


def validate_identity_uses(policy: dict[str, Any]) -> dict[str, Any]:
    uses = policy["condition_semantics"]["identity_uses"]
    material_ids: list[str] = []
    for use_id, row in uses.items():
        material_id = str(row.get("material") or "")
        if not material_id:
            raise ValueError(f"identity use {use_id} lacks material")
        if row.get("operator") not in {"equal", "not_equal"}:
            raise ValueError(f"identity use {use_id} has invalid operator")
        if not str(row.get("reason") or "").strip():
            raise ValueError(f"identity use {use_id} lacks reason")
        source = row.get("source")
        if (
            not isinstance(source, dict)
            or not str(source.get("repository") or "").strip()
            or not str(source.get("revision") or "").strip()
        ):
            raise ValueError(f"identity use {use_id} lacks source metadata")
        material_ids.append(material_id)
    references = identity_references(
        {
            "tool_rules": policy["tool_rules"],
            "port_recipe_strategy": policy["port_recipe_strategy"],
        }
    )
    unknown = sorted(set(references) - set(uses))
    if unknown:
        raise ValueError(f"unknown T4 identity uses: {', '.join(unknown)}")
    unused = sorted(set(uses) - set(references))
    if unused:
        raise ValueError(f"unused T4 identity uses: {', '.join(unused)}")
    return {
        "distinct_literal_material_ids": sorted(set(material_ids)),
        "identity_use_count": len(uses),
        "policy_reference_occurrences": len(references),
        "policy_references_by_use": dict(sorted(Counter(references).items())),
    }


def validate_predicate_sources(
    policy: dict[str, Any],
    tool_materials: dict[str, dict[str, Any]],
) -> dict[str, Any]:
    predicates = policy.get("eligibility_predicate_sources") or {}
    expected = {
        "tool_types_minimum",
        "antimatter_exclusion",
        "coated_exclusion_with_wrench_exception",
        "exact_wood_exclusion",
        "hammer_wood_exclusion",
        "bouncy_exclusion_with_screwdriver_exception",
        "stretchy_exclusion_with_screwdriver_exception",
        "quality_minimum",
        "file_quality_maximum",
    }
    if set(predicates) != expected:
        raise ValueError(
            "eligibility predicate source ledger drifted: "
            f"{sorted(predicates)!r}"
        )
    revision = policy["gt6_source"]["revision"]
    for predicate_id, row in predicates.items():
        if not str(row.get("reason") or "").strip():
            raise ValueError(
                f"eligibility predicate {predicate_id} lacks reason"
            )
        source = row.get("source")
        if (
            not isinstance(source, dict)
            or source.get("repository") != policy["gt6_source"]["repository"]
            or source.get("revision") != revision
        ):
            raise ValueError(
                f"eligibility predicate {predicate_id} lacks pinned source"
            )
    tag_rows = {
        "antimatter_exclusion": "ATOMIC.ANTIMATTER",
        "coated_exclusion_with_wrench_exception": "COMPOUNDS.COATED",
        "hammer_wood_exclusion": "PROPERTIES.WOOD",
        "bouncy_exclusion_with_screwdriver_exception": "PROPERTIES.BOUNCY",
        "stretchy_exclusion_with_screwdriver_exception": "PROPERTIES.STRETCHY",
    }
    populations: dict[str, int] = {}
    for predicate_id, tag in tag_rows.items():
        actual = sum(
            tag in set(
                document["gt6_metadata"].get("material_tags") or []
            )
            for document in tool_materials.values()
        )
        expected_population = predicates[predicate_id][
            "observed_tag_population"
        ]
        if actual != expected_population:
            raise ValueError(
                f"{predicate_id} population drifted: "
                f"expected {expected_population}, got {actual}"
            )
        populations[predicate_id] = actual
    return {
        "predicate_count": len(predicates),
        "pinned_revision": revision,
        "observed_tag_populations": populations,
    }


def build() -> dict[str, Any]:
    policy = load(POLICY)
    documents = load_materials()
    gate = load(GATE)
    registered = {
        material_id: set(forms)
        for material_id, forms in gate["materials"].items()
    }
    identity_projection = validate_identity_uses(policy)
    required_tag = policy["material_domain"]["required_tag"]
    tool_materials: dict[str, dict[str, Any]] = {}
    durability_only_noise: list[str] = []
    domain_mismatches: list[str] = []

    for material_id, document in documents.items():
        metadata = document.get("gt6_metadata") or {}
        tags = set(metadata.get("material_tags") or [])
        tool = metadata.get("tool") or {}
        types = int(tool.get("types") or 0)
        tagged = required_tag in tags
        if tagged != (types > 0):
            domain_mismatches.append(material_id)
        if tagged:
            tool_materials[material_id] = document
        elif float(tool.get("durability") or 0) > 0:
            durability_only_noise.append(material_id)
    unknown_identity_materials = sorted(
        set(identity_projection["distinct_literal_material_ids"])
        - set(tool_materials)
    )
    if unknown_identity_materials:
        raise ValueError(
            "T4 identity uses reference unknown tool materials: "
            + ", ".join(unknown_identity_materials)
        )
    predicate_source_projection = validate_predicate_sources(
        policy, tool_materials
    )

    tool_ids = list(policy["tool_rules"])
    source_tools = set(policy["gt6_recipe_facts"]["tools"])
    if set(tool_ids) != source_tools:
        raise ValueError(
            "T4 tool rules/source recipe facts mismatch: "
            f"{sorted(tool_ids)!r} vs {sorted(source_tools)!r}"
        )
    patterns_by_tool = {
        tool_id: tool_patterns(tool_id, policy) for tool_id in tool_ids
    }
    records: dict[str, Any] = {}
    decision_counts: Counter[str] = Counter()
    types_counts: Counter[int] = Counter()
    quality_counts: Counter[int] = Counter()
    form_counts: Counter[str] = Counter()
    no_advanced_count = 0
    types_no_advanced: Counter[int] = Counter()
    tool_counts: dict[str, dict[str, Any]] = {
        tool_id: {
            "prefix_eligible": 0,
            "listener_eligible": 0,
            "full_tool_eligible": 0,
            "exact_eligible": 0,
            "recipe_ready": 0,
            "eligible_without_route": [],
            "decisions": Counter(),
            "patterns": Counter(),
            "materials_by_pattern": {
                pattern["id"]: [] for pattern in patterns_by_tool[tool_id]
            },
        }
        for tool_id in tool_ids
    }
    signature_owners: dict[str, list[str]] = {}

    for material_id, document in sorted(tool_materials.items()):
        metadata = document["gt6_metadata"]
        tags = set(metadata.get("material_tags") or [])
        tool = metadata["tool"]
        forms = registered.get(material_id, set())
        types = int(tool["types"])
        quality = int(tool["quality"])
        no_advanced = "PROPERTIES.NO_ADVANCED_TOOLS" in tags
        eligible_tools: list[str] = []
        tool_decisions: dict[str, Any] = {}
        for tool_id, rule in policy["tool_rules"].items():
            prefix = condition_matches(
                rule["prefix"], material_id, tags, tool, policy
            )
            listener = condition_matches(
                rule["listener"], material_id, tags, tool, policy
            )
            full_tool = condition_matches(
                rule["full_tool"], material_id, tags, tool, policy
            )
            exact_eligible = prefix and listener and full_tool
            counts = tool_counts[tool_id]
            counts["prefix_eligible"] += prefix
            counts["listener_eligible"] += listener
            counts["full_tool_eligible"] += full_tool
            counts["exact_eligible"] += exact_eligible
            patterns = patterns_by_tool[tool_id]
            selected = (
                choose_pattern(material_id, forms, patterns, policy)
                if exact_eligible
                else None
            )
            if not exact_eligible:
                decision = "excluded_condition"
            elif selected is None:
                decision = "skipped_missing_registered_forms"
                eligible_tools.append(tool_id)
                counts["eligible_without_route"].append(material_id)
            else:
                decision = "ready"
                eligible_tools.append(tool_id)
                counts["recipe_ready"] += 1
                counts["patterns"][selected] += 1
                counts["materials_by_pattern"][selected].append(material_id)
                selected_pattern = pattern_by_id(patterns, selected)
                signature_input = recipe_signature_input(
                    tool_id,
                    material_id,
                    selected_pattern,
                    policy,
                )
                signature = stable_json(signature_input)
                signature_owners.setdefault(signature, []).append(
                    f"{tool_id}:{selected}:{material_id}"
                )
            counts["decisions"][decision] += 1
            decision_counts[decision] += 1
            missing_by_pattern = {
                pattern["id"]: sorted(
                    {
                        str(row["form"])
                        for row in pattern["material_inputs"]
                    }
                    - forms
                )
                for pattern in patterns
            }
            tool_decisions[tool_id] = {
                "decision": decision,
                "eligible": exact_eligible,
                "missing_registered_forms_by_pattern": missing_by_pattern,
                "pattern": selected,
            }
        records[material_id] = {
            "durability": tool["durability"],
            "eligible_tools": eligible_tools,
            "max_damage": max_damage(tool["durability"], policy),
            "mining_speed": mining_speed(tool["speed"], policy),
            "mining_tier": mining_tier(quality, policy),
            "no_advanced_tools": no_advanced,
            "quality": quality,
            "registered_forms": sorted(forms),
            "speed": tool["speed"],
            "tool_decisions": tool_decisions,
            "types": types,
        }
        types_counts[types] += 1
        quality_counts[quality] += 1
        no_advanced_count += no_advanced
        if no_advanced:
            types_no_advanced[types] += 1
        for form in ("rod", "bolt", "dust", "screw", "plate", "ingot", "gem"):
            form_counts[form] += form in forms

    plate_and_rod = sum(
        {"plate", "rod"} <= set(record["registered_forms"])
        for record in records.values()
    )
    facts = {
        "durability_positive_without_tool_domain": len(durability_only_noise),
        "forms": dict(sorted(form_counts.items())),
        "gate_empty_materials": sorted(
            material_id
            for material_id, record in records.items()
            if not record["registered_forms"]
        ),
        "no_advanced_tools": no_advanced_count,
        "plate_and_rod": plate_and_rod,
        "plate_rod_quadrants": {
            "both": plate_and_rod,
            "neither": sum(
                not ({"plate", "rod"} & set(record["registered_forms"]))
                for record in records.values()
            ),
            "plate_without_rod": sum(
                "plate" in record["registered_forms"]
                and "rod" not in record["registered_forms"]
                for record in records.values()
            ),
            "rod_without_plate": sum(
                "rod" in record["registered_forms"]
                and "plate" not in record["registered_forms"]
                for record in records.values()
            ),
        },
        "quality": {
            str(key): value for key, value in sorted(quality_counts.items())
        },
        "tool_materials": len(records),
        "types": {
            str(key): value for key, value in sorted(types_counts.items())
        },
        "types_no_advanced": {
            str(key): value
            for key, value in sorted(types_no_advanced.items())
        },
    }
    expected = policy["expected_source_facts"]
    actual_expected_shape = {
        "tool_materials": facts["tool_materials"],
        "types": facts["types"],
        "types_no_advanced": facts["types_no_advanced"],
        "no_advanced_tools": facts["no_advanced_tools"],
        "plate": facts["forms"]["plate"],
        "rod": facts["forms"]["rod"],
        "plate_and_rod": facts["plate_and_rod"],
    }
    if domain_mismatches:
        raise ValueError(
            "HAS_TOOL_STATS/types domain mismatch: "
            + ", ".join(sorted(domain_mismatches))
        )
    if actual_expected_shape != expected:
        raise ValueError(
            "T4 source facts drifted: "
            f"expected {expected!r}, got {actual_expected_shape!r}"
        )

    classified = sum(decision_counts.values())
    expected_pairs = len(records) * len(tool_ids)
    unclassified = expected_pairs - classified
    if unclassified:
        raise ValueError(f"{unclassified} T4 tool/material pairs unclassified")
    speed_maximum = float(policy["mining_speed_policy"]["maximum"])
    speed_clamped_materials = sorted(
        material_id
        for material_id, record in records.items()
        if float(record["speed"]) > speed_maximum
    )
    collision_groups = [
        owners
        for owners in signature_owners.values()
        if len(owners) > 1
    ]
    pattern_projection: dict[str, Any] = {}
    for tool_id, counts in tool_counts.items():
        pattern_projection[tool_id] = {
            "prefix_eligible": counts["prefix_eligible"],
            "listener_eligible": counts["listener_eligible"],
            "full_tool_eligible": counts["full_tool_eligible"],
            "exact_eligible": counts["exact_eligible"],
            "recipe_ready": counts["recipe_ready"],
            "eligible_without_route": len(counts["eligible_without_route"]),
            "eligible_without_route_materials": counts[
                "eligible_without_route"
            ],
            "decisions": dict(sorted(counts["decisions"].items())),
            "patterns": dict(sorted(counts["patterns"].items())),
            "materials_by_pattern": {
                pattern: materials
                for pattern, materials in counts[
                    "materials_by_pattern"
                ].items()
            },
            "signature_projection_inputs": [
                {
                    "pattern": pattern["id"],
                    "kind": pattern["kind"],
                    "material_inputs": pattern["material_inputs"],
                    "handle_count": pattern["handle_count"],
                    "catalysts": catalyst_inputs(
                        tool_id, pattern, policy
                    ),
                }
                for pattern in patterns_by_tool[tool_id]
            ],
        }
    projected_material_is_by_id: Counter[str] = Counter()
    uses = policy["condition_semantics"]["identity_uses"]
    for tool_id, patterns in patterns_by_tool.items():
        refs = identity_references(policy["tool_rules"][tool_id])
        for use_id in refs:
            projected_material_is_by_id[uses[use_id]["material"]] += len(
                patterns
            )
    for row in policy["port_recipe_strategy"][
        "material_identity_exceptions"
    ]:
        projected_material_is_by_id[
            uses[row["identity_use"]]["material"]
        ] += len(patterns_by_tool[row["tool"]])
    identity_projection["projected_material_is_occurrences"] = sum(
        projected_material_is_by_id.values()
    )
    identity_projection["projected_occurrences_by_material_id"] = dict(
        sorted(projected_material_is_by_id.items())
    )
    identity_projection["entries"] = policy["condition_semantics"][
        "identity_uses"
    ]
    if collision_groups:
        raise ValueError(
            f"T4 projected recipe signatures collide: {collision_groups!r}"
        )
    return {
        "schema_version": 2,
        "status": "READY_FOR_T4_IMPLEMENTATION",
        "delivery_boundary": policy["delivery_boundary"],
        "source_hashes": {
            "material_index": sha256(MATERIALS / "index.json"),
            "material_registration_gate": sha256(GATE),
            "policy": sha256(POLICY),
        },
        "facts": facts,
        "closure": {
            "classified": classified,
            "decisions": dict(sorted(decision_counts.items())),
            "material_count": len(records),
            "tool_material_pair_count": expected_pairs,
            "unclassified": unclassified,
        },
        "gt6_source_projection": {
            "conditions": {
                tool_id: {
                    "prefix": policy["tool_rules"][tool_id]["prefix"],
                    "listener": policy["tool_rules"][tool_id]["listener"],
                    "full_tool": policy["tool_rules"][tool_id]["full_tool"],
                    "source_lines": policy["tool_rules"][tool_id][
                        "source_lines"
                    ],
                }
                for tool_id in tool_ids
            },
            "recipes": policy["gt6_recipe_facts"],
        },
        "strategy_projections": {
            "identity_ledger": identity_projection,
            "eligibility_predicate_sources": predicate_source_projection,
            "eligibility_route_gaps": {
                "total": sum(
                    len(counts["eligible_without_route"])
                    for counts in tool_counts.values()
                ),
                "by_tool": {
                    tool_id: {
                        "count": len(counts["eligible_without_route"]),
                        "materials": counts["eligible_without_route"],
                    }
                    for tool_id, counts in tool_counts.items()
                },
            },
            "mining_speed": {
                "maximum": speed_maximum,
                "source_above_max": len(speed_clamped_materials),
                "clamped_materials": speed_clamped_materials,
            },
            "tool_recipes": pattern_projection,
            "recipe_signatures": {
                "projected_recipes": len(signature_owners),
                "distinct_signatures": len(signature_owners),
                "collision_count": 0,
                "collision_groups": [],
                "pattern_selector_action": policy[
                    "port_recipe_strategy"
                ]["pattern_selector"]["action"],
            },
        },
        "architecture": policy["architecture"],
        "records": records,
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--check",
        action="store_true",
        help="fail if the committed readiness ledger is stale",
    )
    args = parser.parse_args()
    encoded = stable_json(build())
    if args.check:
        from tools import currentness

        errors = currentness.check_rebuilt(OUTPUT, json.loads(encoded))
        if errors:
            raise SystemExit("\n".join(errors))
        print("T4 tool readiness ledger is current.")
        return 0
    OUTPUT.write_text(encoded, encoding="utf-8", newline="\n")
    print(f"wrote {OUTPUT.relative_to(ROOT)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
