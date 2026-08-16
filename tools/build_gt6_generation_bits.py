"""Build GT6 generation rules and exact prefix domains.

The canonical comparison domain contains only material records with a stable
nonnegative ID. Name-only id=-1 records are retained in diagnostics but cannot
participate in an identity set, so they never affect the generated mapping.

Exact ITEMGENERATOR single-tag mappings are retained, then material-generating
prefixes are tested against discriminatively selected Boolean expressions over
all stable-material tags. Small differences are stored as explicit
include/exclude exceptions and replayed exactly. Compatibility-only prefixes
and intrinsically discrete domains are classified without guessing rules.
"""

from __future__ import annotations

import hashlib
import json
import math
from pathlib import Path
from typing import Any

try:
    from .gt6_mapping import (
        EXPLICIT_ENUMERATION_PREFIXES,
        generation_tag_to_flag,
    )
except ImportError:
    from gt6_mapping import (
        EXPLICIT_ENUMERATION_PREFIXES,
        generation_tag_to_flag,
    )

ROOT = Path(__file__).resolve().parents[1]
DUMP = ROOT / "gt6_dump" / "gt6_recipe_dump" / "oredict"
MATERIALS_PATH = DUMP / "materials.json"
PREFIXES_PATH = DUMP / "prefixes.json"
FLUID_MAP_PATH = DUMP / "fluid_map.json"
OUT = ROOT / "tools" / "gt6_generation_bits.json"

SCHEMA_VERSION = 3
EXPECTED_MAPPED_TAGS = 13
EXPECTED_MAPPED_PREFIXES = 70
EXPECTED_PREFIX_RULES = 155
EXPECTED_EXACT_PREFIX_RULES = 110
EXPECTED_EXCEPTION_PREFIX_RULES = 45
TAG_PREFIX = "ITEMGENERATOR."
MAX_EXPRESSION_TAGS = 3
MAX_PREFIX_EXCEPTIONS = 16
MAX_PREFIX_EXCEPTION_RATIO = 0.05
MAX_FLUID_EXCEPTIONS = 16
MAX_FLUID_EXCEPTION_RATIO = 0.10
DISCRIMINATING_ANCHOR_COUNT = 12
DISCRIMINATING_TAGS_PER_RESIDUAL = 4
MATERIAL_BASED_FALSE_RULE_TAGS = frozenset({"PREFIX.STANDARD_ORE"})

# Includes the names requested during the T0 interop investigation plus the
# concrete GT6 prefixes used for raw ore and wire forms.
CORE_PREFIXES = (
    "ingot",
    "dust",
    "plate",
    "stick",
    "nugget",
    "gem",
    "block",
    "wire",
    "wireFine",
    "wireGt01",
    "crushed",
    "crushedTiny",
    "crushedPurified",
    "crushedCentrifuged",
    "ore",
    "oreRaw",
    "blockIngot",
)


def stable_json(value: Any) -> str:
    return json.dumps(
        value,
        sort_keys=True,
        separators=(",", ":"),
        ensure_ascii=False,
    )


def content_hash(value: Any) -> str:
    return hashlib.sha256(stable_json(value).encode("utf-8")).hexdigest()


def file_sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def _named_rows(rows: list[dict[str, Any]], kind: str) -> dict[str, dict[str, Any]]:
    result: dict[str, dict[str, Any]] = {}
    for row in rows:
        name = row.get("nameInternal")
        if not isinstance(name, str) or not name:
            raise ValueError(f"{kind} row has no nameInternal: {row}")
        if name in result:
            raise ValueError(f"duplicate {kind} nameInternal: {name}")
        result[name] = row
    return result


def evaluate_expression(
    expression: dict[str, Any],
    tag_sets: dict[str, frozenset[str]],
) -> frozenset[str]:
    """Evaluate the public v2 Boolean expression format."""
    op = expression.get("op")
    if op == "tag":
        tag = expression.get("tag")
        if tag not in tag_sets:
            raise ValueError(f"generation expression references unknown tag: {tag}")
        return tag_sets[tag]
    if op in {"union", "intersection"}:
        args = expression.get("args") or []
        if len(args) != 2:
            raise ValueError(f"{op} expression must contain exactly two args")
        left = evaluate_expression(args[0], tag_sets)
        right = evaluate_expression(args[1], tag_sets)
        return left | right if op == "union" else left & right
    if op == "difference":
        if "left" not in expression or "right" not in expression:
            raise ValueError("difference expression requires left and right")
        return (
            evaluate_expression(expression["left"], tag_sets)
            - evaluate_expression(expression["right"], tag_sets)
        )
    raise ValueError(f"unknown generation expression operator: {op}")


def replay_rule(
    rule: dict[str, Any],
    tag_sets: dict[str, frozenset[str]],
) -> frozenset[str]:
    """Expand a generation rule to its exact canonical material domain."""
    domain = set(evaluate_expression(rule["expression"], tag_sets))
    domain.difference_update(rule.get("exclude_materials") or [])
    domain.update(rule.get("include_materials") or [])
    return frozenset(domain)


def _names_to_bits(names: Any, indexes: dict[str, int]) -> int:
    result = 0
    for name in names:
        index = indexes.get(name)
        if index is not None:
            result |= 1 << index
    return result


def _bits_to_names(bits: int, names: list[str]) -> list[str]:
    result = []
    while bits:
        lowest = bits & -bits
        index = lowest.bit_length() - 1
        result.append(names[index])
        bits ^= lowest
    return result


def _tag_candidate(tag: str, bits: int) -> dict[str, Any]:
    return {
        "bits": bits,
        "expression": {"op": "tag", "tag": tag},
        "expression_text": tag,
        "tags": frozenset({tag}),
        "tag_count": 1,
    }


def _combined_candidate(
    op: str,
    left: dict[str, Any],
    right: dict[str, Any],
    universe_mask: int,
) -> dict[str, Any]:
    if op in {"union", "intersection"}:
        ordered = sorted(
            (left, right),
            key=lambda row: row["expression_text"],
        )
        left, right = ordered
        symbol = "|" if op == "union" else "&"
        bits = (
            left["bits"] | right["bits"]
            if op == "union"
            else left["bits"] & right["bits"]
        )
        expression = {
            "op": op,
            "args": [left["expression"], right["expression"]],
        }
    elif op == "difference":
        symbol = "-"
        bits = left["bits"] & ~right["bits"] & universe_mask
        expression = {
            "op": "difference",
            "left": left["expression"],
            "right": right["expression"],
        }
    else:
        raise ValueError(f"unknown Boolean-domain operator: {op}")
    return {
        "bits": bits,
        "expression": expression,
        "expression_text": (
            f"({left['expression_text']} {symbol} "
            f"{right['expression_text']})"
        ),
        "tags": left["tags"] | right["tags"],
        "tag_count": left["tag_count"] + right["tag_count"],
    }


def _candidate_score(candidate: dict[str, Any]) -> tuple[int, int, str]:
    return (
        int(candidate["tag_count"]),
        len(candidate["expression_text"]),
        candidate["expression_text"],
    )


def _register_candidate(
    candidates: dict[int, dict[str, Any]],
    candidate: dict[str, Any],
) -> None:
    old = candidates.get(candidate["bits"])
    if old is None or _candidate_score(candidate) < _candidate_score(old):
        candidates[candidate["bits"]] = candidate


def build_boolean_domains(
    tag_sets: dict[str, frozenset[str]],
    canonical_names: frozenset[str],
    max_expression_tags: int = MAX_EXPRESSION_TAGS,
) -> tuple[dict[int, dict[str, Any]], list[str], dict[str, int], int]:
    """Enumerate unique domains expressible within the requested tag limit."""
    if max_expression_tags not in {1, 2, 3}:
        raise ValueError("Boolean-domain tag limit must be between one and three")
    names = sorted(canonical_names)
    indexes = {name: index for index, name in enumerate(names)}
    universe_mask = (1 << len(names)) - 1
    tag_candidates = {
        tag: _tag_candidate(
            tag,
            _names_to_bits(members, indexes),
        )
        for tag, members in sorted(tag_sets.items())
    }
    candidates = {
        candidate["bits"]: candidate
        for candidate in tag_candidates.values()
    }

    tags = sorted(tag_candidates)
    if max_expression_tags >= 2:
        for left_index, left_tag in enumerate(tags):
            left = tag_candidates[left_tag]
            for right_tag in tags[left_index + 1:]:
                right = tag_candidates[right_tag]
                for op in ("union", "intersection", "difference"):
                    _register_candidate(
                        candidates,
                        _combined_candidate(
                            op,
                            left,
                            right,
                            universe_mask,
                        ),
                    )
                _register_candidate(
                    candidates,
                    _combined_candidate(
                        "difference",
                        right,
                        left,
                        universe_mask,
                    ),
                )

    if max_expression_tags >= 3:
        two_tag_candidates = [
            candidate
            for candidate in candidates.values()
            if candidate["tag_count"] == 2
        ]
        for left in two_tag_candidates:
            for tag in tags:
                if tag in left["tags"]:
                    continue
                right = tag_candidates[tag]
                for op in ("union", "intersection", "difference"):
                    _register_candidate(
                        candidates,
                        _combined_candidate(
                            op,
                            left,
                            right,
                            universe_mask,
                        ),
                    )
                _register_candidate(
                    candidates,
                    _combined_candidate(
                        "difference",
                        right,
                        left,
                        universe_mask,
                    ),
                )

    if any(candidate["tag_count"] > max_expression_tags for candidate in candidates.values()):
        raise ValueError("Boolean-domain search exceeded its tag limit")
    return candidates, names, indexes, universe_mask


def _target_score(
    candidate: dict[str, Any],
    target_bits: int,
    universe_mask: int,
) -> tuple[int, int, int, int, int, str]:
    return (
        (candidate["bits"] ^ target_bits).bit_count(),
        (candidate["bits"] & ~target_bits & universe_mask).bit_count(),
        (target_bits & ~candidate["bits"] & universe_mask).bit_count(),
        *_candidate_score(candidate),
    )


def _best_candidate(
    target_bits: int,
    candidates: dict[int, dict[str, Any]],
    universe_mask: int,
) -> dict[str, Any]:
    return min(
        candidates.values(),
        key=lambda candidate: _target_score(candidate, target_bits, universe_mask),
    )


def _discriminating_tags(
    target_bits: int,
    tag_candidates: dict[str, dict[str, Any]],
    universe_mask: int,
) -> list[str]:
    """Keep direct anchors plus tags that separate each anchor's mistakes."""
    anchors = sorted(
        tag_candidates.values(),
        key=lambda candidate: _target_score(candidate, target_bits, universe_mask),
    )[:DISCRIMINATING_ANCHOR_COUNT]
    selected = {next(iter(candidate["tags"])) for candidate in anchors}

    for anchor in anchors:
        residuals = (
            target_bits & ~anchor["bits"] & universe_mask,
            anchor["bits"] & ~target_bits & universe_mask,
        )
        for residual in residuals:
            if not residual:
                continue
            ranked = sorted(
                tag_candidates.values(),
                key=lambda candidate: (
                    (candidate["bits"] ^ residual).bit_count(),
                    -(candidate["bits"] & residual).bit_count(),
                    (candidate["bits"] & ~residual & universe_mask).bit_count(),
                    candidate["expression_text"],
                ),
            )
            selected.update(
                next(iter(candidate["tags"]))
                for candidate in ranked[:DISCRIMINATING_TAGS_PER_RESIDUAL]
                if candidate["bits"] & residual
            )
    return sorted(selected)


def _best_discriminative_candidate(
    target_bits: int,
    tag_sets: dict[str, frozenset[str]],
    canonical_names: frozenset[str],
    base_candidates: dict[int, dict[str, Any]],
    universe_mask: int,
) -> tuple[dict[str, Any], int]:
    base = _best_candidate(target_bits, base_candidates, universe_mask)
    if (base["bits"] ^ target_bits).bit_count() == 0:
        return base, 0

    names = sorted(canonical_names)
    indexes = {name: index for index, name in enumerate(names)}
    tag_candidates = {
        tag: _tag_candidate(tag, _names_to_bits(members, indexes))
        for tag, members in sorted(tag_sets.items())
    }
    selected_tags = _discriminating_tags(
        target_bits,
        tag_candidates,
        universe_mask,
    )
    local_candidates, _, _, _ = build_boolean_domains(
        {tag: tag_sets[tag] for tag in selected_tags},
        canonical_names,
        MAX_EXPRESSION_TAGS,
    )
    local = _best_candidate(target_bits, local_candidates, universe_mask)
    return min(
        (base, local),
        key=lambda candidate: _target_score(candidate, target_bits, universe_mask),
    ), len(selected_tags)


def _domain_rule(
    target_bits: int,
    candidate: dict[str, Any],
    names: list[str],
    universe_mask: int,
    max_exceptions: int,
    max_exception_ratio: float,
    dynamic_exception_budget: bool = False,
) -> dict[str, Any]:
    exclude_bits = candidate["bits"] & ~target_bits & universe_mask
    include_bits = target_bits & ~candidate["bits"] & universe_mask
    exception_count = exclude_bits.bit_count() + include_bits.bit_count()
    target_count = target_bits.bit_count()
    exception_ratio = (
        exception_count / target_count
        if target_count
        else (0.0 if exception_count == 0 else 1.0)
    )
    exception_budget = (
        max(max_exceptions, math.ceil(max_exception_ratio * target_count))
        if dynamic_exception_budget
        else max_exceptions
    )
    if exception_count == 0:
        status = "exact"
    elif exception_count <= exception_budget and (
        dynamic_exception_budget
        or exception_ratio <= max_exception_ratio
    ):
        status = "accepted_with_exceptions"
    else:
        status = "diagnostic_only"

    exclude = _bits_to_names(exclude_bits, names)
    include = _bits_to_names(include_bits, names)
    rule = {
        "status": status,
        "expression": candidate["expression"],
        "expression_text": candidate["expression_text"],
        "expression_tag_count": candidate["tag_count"],
        "base_material_count": candidate["bits"].bit_count(),
        "target_material_count": target_count,
        "exclude_materials": exclude,
        "include_materials": include,
        "exception_count": exception_count,
        "exception_ratio": exception_ratio,
        "exception_budget": exception_budget,
        "target_set_sha256": content_hash(
            _bits_to_names(target_bits, names)
        ),
    }
    replay_bits = (
        candidate["bits"] & ~exclude_bits & universe_mask
    ) | include_bits
    if replay_bits != target_bits:
        raise ValueError("generation rule failed exact bitset replay")
    rule["replay_verified"] = True
    return rule


def build_document(
    materials: list[dict[str, Any]],
    prefixes: list[dict[str, Any]],
    fluid_map: dict[str, dict[str, Any]],
) -> dict[str, Any]:
    materials_by_name = _named_rows(materials, "material")
    prefixes_by_name = _named_rows(prefixes, "prefix")
    canonical_names = frozenset(
        name
        for name, row in materials_by_name.items()
        if isinstance(row.get("id"), int) and int(row["id"]) >= 0
    )
    sentinel_names = frozenset(
        name
        for name, row in materials_by_name.items()
        if row.get("id") == -1
    )
    negative_names = frozenset(
        name
        for name, row in materials_by_name.items()
        if isinstance(row.get("id"), int) and int(row["id"]) < 0
    )
    if negative_names != sentinel_names:
        raise ValueError("negative material IDs other than the -1 sentinel exist")

    generation_tags = sorted({
        tag
        for name in canonical_names
        for tag in materials_by_name[name].get("tags") or []
    })
    for tag in generation_tags:
        generation_tag_to_flag(tag)
    itemgenerator_tags = [
        tag for tag in generation_tags if tag.startswith(TAG_PREFIX)
    ]
    canonical_generation_tag_sets = {
        tag: frozenset(
            name
            for name in canonical_names
            if tag in (materials_by_name[name].get("tags") or [])
        )
        for tag in generation_tags
    }
    canonical_itemgenerator_tag_sets = {
        tag: canonical_generation_tag_sets[tag]
        for tag in itemgenerator_tags
    }
    full_itemgenerator_tag_sets = {
        tag: frozenset(
            name
            for name, row in materials_by_name.items()
            if tag in (row.get("tags") or [])
        )
        for tag in itemgenerator_tags
    }

    unknown_prefix_materials = sorted({
        name
        for row in prefixes
        for name in row.get("registeredMaterials") or []
        if name not in materials_by_name
    })
    if unknown_prefix_materials:
        raise ValueError(
            "prefixes reference unknown materials: "
            + ", ".join(unknown_prefix_materials[:20])
        )
    full_prefix_sets = {
        name: frozenset(row.get("registeredMaterials") or [])
        for name, row in prefixes_by_name.items()
        if row.get("registeredMaterials")
    }
    canonical_prefix_sets = {
        name: members & canonical_names
        for name, members in full_prefix_sets.items()
    }

    mapping = {
        tag: sorted(
            prefix
            for prefix, members in canonical_prefix_sets.items()
            if members == canonical_itemgenerator_tag_sets[tag]
        )
        for tag in itemgenerator_tags
    }
    mapping = {
        tag: prefix_names
        for tag, prefix_names in mapping.items()
        if prefix_names
    }
    mapped_prefixes = [
        prefix
        for prefix_names in mapping.values()
        for prefix in prefix_names
    ]
    if len(mapping) != EXPECTED_MAPPED_TAGS:
        raise ValueError(
            f"expected {EXPECTED_MAPPED_TAGS} exact ITEMGENERATOR tags, "
            f"got {len(mapping)}"
        )
    if len(mapped_prefixes) != EXPECTED_MAPPED_PREFIXES:
        raise ValueError(
            f"expected {EXPECTED_MAPPED_PREFIXES} mapped prefixes, "
            f"got {len(mapped_prefixes)}"
        )
    if len(set(mapped_prefixes)) != len(mapped_prefixes):
        raise ValueError("a prefix maps exactly to more than one generation bit")

    (
        base_candidates,
        canonical_name_list,
        canonical_name_indexes,
        universe_mask,
    ) = build_boolean_domains(
        canonical_generation_tag_sets,
        canonical_names,
        2,
    )
    all_canonical_prefix_sets = {
        name: frozenset(row.get("registeredMaterials") or [])
        & canonical_names
        for name, row in prefixes_by_name.items()
    }
    prefix_generation_rules: dict[str, dict[str, Any]] = {}
    prefix_diagnostics: dict[str, dict[str, Any]] = {}
    compatibility_absorption_prefixes: dict[str, dict[str, Any]] = {}
    explicit_prefix_domains: dict[str, dict[str, Any]] = {}
    for prefix, target in sorted(all_canonical_prefix_sets.items()):
        if not target:
            if prefix in CORE_PREFIXES:
                prefix_diagnostics[prefix] = {
                    "status": "empty_target",
                    "target_material_count": 0,
                    "replay_verified": True,
                }
            continue
        prefix_row = prefixes_by_name[prefix]
        if prefix in EXPLICIT_ENUMERATION_PREFIXES:
            explicit = {
                "status": "explicit_enumeration",
                "materials": sorted(target),
                "target_material_count": len(target),
                "target_set_sha256": content_hash(sorted(target)),
                "replay_verified": True,
            }
            explicit_prefix_domains[prefix] = explicit
            if prefix in CORE_PREFIXES:
                prefix_diagnostics[prefix] = explicit
            continue
        if (
            prefix_row.get("materialBased") is False
            and not (
                set(prefix_row.get("tags") or [])
                & MATERIAL_BASED_FALSE_RULE_TAGS
            )
        ):
            compatibility = {
                "status": "compatibility_absorption",
                "reason": "materialBased=false without a direct generation marker",
                "target_material_count": len(target),
                "target_set_sha256": content_hash(sorted(target)),
                "replay_verified": True,
            }
            compatibility_absorption_prefixes[prefix] = compatibility
            if prefix in CORE_PREFIXES:
                prefix_diagnostics[prefix] = compatibility
            continue
        target_bits = _names_to_bits(
            target,
            canonical_name_indexes,
        )
        candidate, selected_tag_count = _best_discriminative_candidate(
            target_bits,
            canonical_generation_tag_sets,
            canonical_names,
            base_candidates,
            universe_mask,
        )
        rule = _domain_rule(
            target_bits,
            candidate,
            canonical_name_list,
            universe_mask,
            MAX_PREFIX_EXCEPTIONS,
            MAX_PREFIX_EXCEPTION_RATIO,
            dynamic_exception_budget=True,
        )
        rule["discriminating_tag_count"] = selected_tag_count
        if prefix in CORE_PREFIXES:
            prefix_diagnostics[prefix] = rule
        if rule["status"] != "diagnostic_only":
            prefix_generation_rules[prefix] = rule

    exact_prefix_rule_count = sum(
        rule["status"] == "exact"
        for rule in prefix_generation_rules.values()
    )
    exception_prefix_rule_count = sum(
        rule["status"] == "accepted_with_exceptions"
        for rule in prefix_generation_rules.values()
    )
    if (
        EXPECTED_PREFIX_RULES is not None
        and len(prefix_generation_rules) != EXPECTED_PREFIX_RULES
    ):
        raise ValueError(
            f"expected {EXPECTED_PREFIX_RULES} accepted prefix rules, "
            f"got {len(prefix_generation_rules)}"
        )
    if (
        EXPECTED_EXACT_PREFIX_RULES is not None
        and exact_prefix_rule_count != EXPECTED_EXACT_PREFIX_RULES
    ):
        raise ValueError(
            f"expected {EXPECTED_EXACT_PREFIX_RULES} exact prefix rules, "
            f"got {exact_prefix_rule_count}"
        )
    if (
        EXPECTED_EXCEPTION_PREFIX_RULES is not None
        and exception_prefix_rule_count != EXPECTED_EXCEPTION_PREFIX_RULES
    ):
        raise ValueError(
            f"expected {EXPECTED_EXCEPTION_PREFIX_RULES} exception prefix "
            f"rules, got {exception_prefix_rule_count}"
        )

    molten_rows = [
        value
        for fluid_id, value in fluid_map.items()
        if fluid_id.startswith("molten.")
        and isinstance(value, dict)
    ]
    molten_names = frozenset(
        value.get("material")
        for value in molten_rows
        if value.get("material") in canonical_names
        and isinstance(value.get("materialId"), int)
        and int(value["materialId"]) >= 0
    )
    molten_target_bits = _names_to_bits(
        molten_names,
        canonical_name_indexes,
    )
    molten_tag = f"{TAG_PREFIX}MOLTEN"
    liquid_tag = f"{TAG_PREFIX}LIQUID"
    molten_tag_candidate = _tag_candidate(
        molten_tag,
        _names_to_bits(
            canonical_generation_tag_sets[molten_tag],
            canonical_name_indexes,
        ),
    )
    liquid_tag_candidate = _tag_candidate(
        liquid_tag,
        _names_to_bits(
            canonical_generation_tag_sets[liquid_tag],
            canonical_name_indexes,
        ),
    )
    molten_union_candidate = _combined_candidate(
        "union",
        molten_tag_candidate,
        liquid_tag_candidate,
        universe_mask,
    )
    molten_single_rule = _domain_rule(
        molten_target_bits,
        molten_tag_candidate,
        canonical_name_list,
        universe_mask,
        MAX_FLUID_EXCEPTIONS,
        MAX_FLUID_EXCEPTION_RATIO,
    )
    molten_union_rule = _domain_rule(
        molten_target_bits,
        molten_union_candidate,
        canonical_name_list,
        universe_mask,
        MAX_FLUID_EXCEPTIONS,
        MAX_FLUID_EXCEPTION_RATIO,
    )
    molten_rule = min(
        (molten_single_rule, molten_union_rule),
        key=lambda rule: (
            rule["exception_count"],
            rule["expression_tag_count"],
            rule["expression_text"],
        ),
    )
    molten_rule.update({
        "target": "fluid_map keys with molten.*",
        "full_fluid_record_count": len(molten_rows),
        "canonical_fluid_material_count": len(molten_names),
        "molten_or_liquid_trial": {
            "status": molten_union_rule["status"],
            "expression": molten_union_rule["expression"],
            "expression_text": molten_union_rule["expression_text"],
            "base_material_count": molten_union_rule["base_material_count"],
            "exception_count": molten_union_rule["exception_count"],
            "exception_ratio": molten_union_rule["exception_ratio"],
        },
        "liquid_incremental_material_count": len(
            canonical_generation_tag_sets[liquid_tag]
            - canonical_generation_tag_sets[molten_tag]
        ),
        "sentinel_fluid_record_count": sum(
            value.get("materialId") == -1
            for value in molten_rows
        ),
    })
    if molten_rule["status"] != "accepted_with_exceptions":
        raise ValueError(
            "molten generation candidates did not produce the expected "
            "small-exception fluid rule"
        )

    verification_records = []
    noncanonical_differences: set[str] = set()
    for tag, prefix_names in mapping.items():
        full_prefix_domains = {
            full_prefix_sets[prefix] for prefix in prefix_names
        }
        if len(full_prefix_domains) != 1:
            raise ValueError(
                f"mapped prefixes disagree outside canonical scope for {tag}"
            )
        full_prefix_domain = next(iter(full_prefix_domains))
        tag_only = full_itemgenerator_tag_sets[tag] - full_prefix_domain
        prefix_only = full_prefix_domain - full_itemgenerator_tag_sets[tag]
        noncanonical_differences.update(tag_only)
        noncanonical_differences.update(prefix_only)
        verification_records.append({
            "tag": tag,
            "canonical_material_count": len(canonical_itemgenerator_tag_sets[tag]),
            "prefix_count": len(prefix_names),
            "canonical_set_sha256": content_hash(
                sorted(canonical_itemgenerator_tag_sets[tag])
            ),
            "tag_sentinel_record_count": len(
                full_itemgenerator_tag_sets[tag] & sentinel_names
            ),
            "prefix_sentinel_record_count": len(
                full_prefix_domain & sentinel_names
            ),
            "tag_only_sentinel_count": len(tag_only),
            "prefix_only_sentinel_count": len(prefix_only),
            "full_name_sets_equal": not tag_only and not prefix_only,
        })

    all_differences_are_sentinels = (
        noncanonical_differences <= sentinel_names
    )
    if not all_differences_are_sentinels:
        raise ValueError(
            "mapped domains differ on stable or unknown material identities"
        )

    return {
        "schema_version": SCHEMA_VERSION,
        "artifact_kind": "gt6_generation_bits",
        "identity_policy": {
            "material_key": "nameInternal",
            "canonical_filter": "material.id >= 0",
            "sentinel_policy": (
                "id=-1 name-only records are excluded from set equality and "
                "reported only as noncanonical diagnostics"
            ),
        },
        "rule_language": {
            "max_expression_tags": MAX_EXPRESSION_TAGS,
            "operators": ["union", "intersection", "difference"],
            "replay": (
                "(evaluate(expression) - exclude_materials) "
                "| include_materials"
            ),
            "prefix_acceptance": {
                "exception_budget": (
                    "max(16, ceil(0.05 * target_material_count))"
                ),
                "minimum_exception_count": MAX_PREFIX_EXCEPTIONS,
                "target_exception_ratio": MAX_PREFIX_EXCEPTION_RATIO,
            },
            "fluid_acceptance": {
                "max_exception_count": MAX_FLUID_EXCEPTIONS,
                "max_exception_ratio": MAX_FLUID_EXCEPTION_RATIO,
            },
            "diagnostic_policy": (
                "The best candidate is reported for core prefixes even when "
                "its exception set is too large to be a generation rule."
            ),
            "search_policy": (
                "All stable-material tags participate in one- and two-tag "
                "searches. Three-tag searches use direct target anchors plus "
                "tags ranked by residual coverage and spill."
            ),
        },
        "source": {
            "materials": MATERIALS_PATH.relative_to(ROOT).as_posix(),
            "materials_raw_sha256": file_sha256(MATERIALS_PATH),
            "materials_canonical_sha256": content_hash(materials),
            "prefixes": PREFIXES_PATH.relative_to(ROOT).as_posix(),
            "prefixes_raw_sha256": file_sha256(PREFIXES_PATH),
            "prefixes_canonical_sha256": content_hash(prefixes),
            "fluid_map": FLUID_MAP_PATH.relative_to(ROOT).as_posix(),
            "fluid_map_raw_sha256": file_sha256(FLUID_MAP_PATH),
            "fluid_map_canonical_sha256": content_hash(fluid_map),
        },
        "generation_tag_flags": {
            tag: generation_tag_to_flag(tag)
            for tag in generation_tags
        },
        "itemgenerator_to_prefixes": dict(sorted(mapping.items())),
        "prefix_generation_rules": dict(
            sorted(prefix_generation_rules.items())
        ),
        "explicit_prefix_domains": dict(
            sorted(explicit_prefix_domains.items())
        ),
        "compatibility_absorption_prefixes": dict(
            sorted(compatibility_absorption_prefixes.items())
        ),
        "core_prefix_diagnostics": {
            prefix: prefix_diagnostics[prefix]
            for prefix in CORE_PREFIXES
            if prefix in prefix_diagnostics
        },
        "fluid_generation_rules": {
            "molten": molten_rule,
        },
        "verification": {
            "canonical_set_equality_verified": True,
            "mapped_tag_count": len(mapping),
            "mapped_prefix_count": len(mapped_prefixes),
            "all_noncanonical_differences_are_sentinel_names": (
                all_differences_are_sentinels
            ),
            "source_generation_tag_count": len(generation_tags),
            "source_itemgenerator_tag_count": len(itemgenerator_tags),
            "unmapped_itemgenerator_tags": sorted(
                set(itemgenerator_tags) - set(mapping)
            ),
            "unmapped_itemgenerator_tags_semantics": (
                "No exact single-tag prefix domain; tags may still appear in "
                "Boolean prefix rules or fluid rules."
            ),
            "base_boolean_candidate_domain_count": len(base_candidates),
            "accepted_prefix_rule_count": len(
                prefix_generation_rules
            ),
            "exact_prefix_rule_count": exact_prefix_rule_count,
            "exception_prefix_rule_count": (
                exception_prefix_rule_count
            ),
            "accepted_core_prefixes": sorted(
                set(CORE_PREFIXES) & set(prefix_generation_rules)
            ),
            "compatibility_absorption_core_prefixes": sorted(
                set(CORE_PREFIXES)
                & set(compatibility_absorption_prefixes)
            ),
            "explicit_core_prefixes": sorted(
                set(CORE_PREFIXES) & set(explicit_prefix_domains)
            ),
            "unresolved_core_prefixes": sorted(
                set(CORE_PREFIXES)
                - set(prefix_generation_rules)
                - set(compatibility_absorption_prefixes)
                - set(explicit_prefix_domains)
            ),
            "all_prefix_rules_replay_exactly": all(
                rule["replay_verified"]
                for rule in prefix_generation_rules.values()
            ),
            "molten_rule_replays_exactly": molten_rule[
                "replay_verified"
            ],
            "canonical_material_count": len(canonical_names),
            "sentinel_record_count": len(sentinel_names),
            "full_name_exact_tag_count": sum(
                row["full_name_sets_equal"]
                for row in verification_records
            ),
            "records": verification_records,
        },
    }


def extract() -> dict[str, Any]:
    materials = json.loads(MATERIALS_PATH.read_text(encoding="utf-8"))
    prefixes = json.loads(PREFIXES_PATH.read_text(encoding="utf-8"))
    fluid_map = json.loads(
        FLUID_MAP_PATH.read_text(encoding="utf-8")
    )
    document = build_document(materials, prefixes, fluid_map)
    OUT.write_text(
        json.dumps(document, indent=2, ensure_ascii=False) + "\n",
        encoding="utf-8",
        newline="\n",
    )
    return document


def main() -> int:
    document = extract()
    print(f"Wrote {OUT}")
    print(json.dumps({
        key: value
        for key, value in document["verification"].items()
        if key != "records"
    }, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
