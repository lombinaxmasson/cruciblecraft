"""Build the T13b GT6 prefix and ITEMGENERATOR denominators.

OP.java and TD.java are pinned by Git blob identity.  Source text is cached
under build/ only; the committed artifacts contain identities and normalized
facts, never a copy of either source file.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import urllib.request
from collections import Counter, defaultdict
from pathlib import Path
from typing import Any, Iterable


ROOT = Path(__file__).resolve().parents[1]
TOOLS = ROOT / "tools"
POLICY_PATH = TOOLS / "t13_prefix_domain_policy.json"
PREFIX_OUTPUT = TOOLS / "t13_denominators" / "prefixes.json"
DOMAIN_OUTPUT = TOOLS / "t13_denominators" / "itemgenerator_domains.json"
NORMALIZED_PREFIXES = TOOLS / "gt6_oredict_prefixes_normalized.json"
NORMALIZED_MATERIALS = TOOLS / "gt6_oredict_materials_normalized.json"
PREFIX_MAPPING = TOOLS / "gt6_prefix_mapping.json"
L3_PREFIX_PLAN = TOOLS / "gt6_l3_prefix_plan.json"
TAG_DOMAIN_REPORT = TOOLS / "gt6_tag_domain_report.json"
GENERATION_BITS = TOOLS / "gt6_generation_bits.json"
RUNTIME_PREFIX_ROOT = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "material_prefixes"
)
RUNTIME_PREFIX_INDEX = RUNTIME_PREFIX_ROOT / "index.json"
SOURCE_CACHE = ROOT / "build" / "t13-gt6-source"

SCHEMA_VERSION = 2
CLASSIFICATION_VOCAB = {
    "in_scope",
    "deferred_with_reason",
    "out_of_scope",
    "unclassified",
}
DEFERRED_FIELDS = {
    "reason",
    "owner",
    "replacement_condition",
    "recheck_point",
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


def compact_json(value: Any) -> str:
    return json.dumps(
        value,
        ensure_ascii=False,
        separators=(",", ":"),
        sort_keys=True,
    )


def sha256_bytes(value: bytes) -> str:
    return hashlib.sha256(value).hexdigest()


def sha256_file(path: Path) -> str:
    return sha256_bytes(path.read_bytes())


def content_sha256(value: Any) -> str:
    return sha256_bytes(compact_json(value).encode("utf-8"))


def git_blob_sha1(value: bytes) -> str:
    header = f"blob {len(value)}\0".encode("ascii")
    return hashlib.sha1(header + value).hexdigest()


def verify_git_blob(value: bytes, expected: str, label: str) -> None:
    actual = git_blob_sha1(value)
    if actual != expected:
        raise ValueError(
            f"{label} git blob drifted: expected {expected}, got {actual}"
        )


def source_cache_path(source_policy: dict[str, Any]) -> Path:
    return SOURCE_CACHE / source_policy["path"]


def load_fixed_source(
    source_policy: dict[str, Any],
    *,
    override: Path | None = None,
    allow_fetch: bool = True,
) -> tuple[str, dict[str, Any]]:
    path = override or source_cache_path(source_policy)
    if not path.is_file():
        if override is not None or not allow_fetch:
            raise FileNotFoundError(path)
        with urllib.request.urlopen(source_policy["raw_url"], timeout=60) as response:
            value = response.read()
        verify_git_blob(
            value,
            source_policy["git_blob_sha1"],
            source_policy["path"],
        )
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(value)
    value = path.read_bytes()
    verify_git_blob(
        value,
        source_policy["git_blob_sha1"],
        source_policy["path"],
    )
    return value.decode("utf-8"), {
        "path": source_policy["path"],
        "git_blob_sha1": source_policy["git_blob_sha1"],
        "sha256": sha256_bytes(value),
        "byte_count": len(value),
        "cache_path": path.relative_to(ROOT).as_posix()
        if path.is_relative_to(ROOT)
        else str(path),
        "acquisition": "verified_build_cache_or_pinned_raw_url",
    }


def strip_java_comments(source: str) -> str:
    result: list[str] = []
    index = 0
    in_string = False
    escaped = False
    while index < len(source):
        char = source[index]
        next_char = source[index + 1] if index + 1 < len(source) else ""
        if in_string:
            result.append(char)
            if escaped:
                escaped = False
            elif char == "\\":
                escaped = True
            elif char == '"':
                in_string = False
            index += 1
            continue
        if char == '"':
            in_string = True
            result.append(char)
            index += 1
            continue
        if char == "/" and next_char == "/":
            while index < len(source) and source[index] != "\n":
                index += 1
            result.append("\n")
            index += 1
            continue
        if char == "/" and next_char == "*":
            index += 2
            while index + 1 < len(source) and source[index : index + 2] != "*/":
                result.append("\n" if source[index] == "\n" else " ")
                index += 1
            index += 2
            continue
        result.append(char)
        index += 1
    return "".join(result)


def split_top_level(value: str, delimiter: str = ",") -> list[str]:
    result: list[str] = []
    start = 0
    depths = {"(": 0, "[": 0, "{": 0}
    closing = {")": "(", "]": "[", "}": "{"}
    in_string = False
    escaped = False
    for index, char in enumerate(value):
        if in_string:
            if escaped:
                escaped = False
            elif char == "\\":
                escaped = True
            elif char == '"':
                in_string = False
            continue
        if char == '"':
            in_string = True
        elif char in depths:
            depths[char] += 1
        elif char in closing:
            depths[closing[char]] -= 1
        elif char == delimiter and not any(depths.values()):
            result.append(value[start:index].strip())
            start = index + 1
    tail = value[start:].strip()
    if tail:
        result.append(tail)
    return result


def extract_balanced_call(value: str, method: str) -> str | None:
    marker = f".{method}("
    start = value.find(marker)
    if start < 0:
        return None
    start += len(marker)
    depth = 1
    in_string = False
    escaped = False
    for index in range(start, len(value)):
        char = value[index]
        if in_string:
            if escaped:
                escaped = False
            elif char == "\\":
                escaped = True
            elif char == '"':
                in_string = False
            continue
        if char == '"':
            in_string = True
        elif char == "(":
            depth += 1
        elif char == ")":
            depth -= 1
            if depth == 0:
                return value[start:index].strip()
    raise ValueError(f"unterminated .{method}(...) call")


def _op_declaration_body(source: str) -> str:
    clean = strip_java_comments(source)
    marker = "public static final OreDictPrefix"
    start = clean.index(marker) + len(marker)
    end = clean.index("public static final OreDictPrefix[]", start)
    body = clean[start:end].strip()
    if body.endswith(";"):
        body = body[:-1]
    return body


def parse_op_prefixes(source: str) -> dict[str, Any]:
    canonical: dict[str, dict[str, Any]] = {}
    variants: list[dict[str, str]] = []
    for statement in split_top_level(_op_declaration_body(source)):
        if not statement:
            continue
        match = re.match(
            r"^([A-Za-z_$][A-Za-z0-9_$]*)\s*=\s*(.*)$",
            statement,
            flags=re.DOTALL,
        )
        if not match:
            raise ValueError(f"unparsed OP prefix declaration: {statement[:120]}")
        field, initializer = match.groups()
        initializer = initializer.strip().rstrip(";").strip()
        created = re.match(
            r'^(create|unused)\(\s*"([^"]+)"',
            initializer,
            flags=re.DOTALL,
        )
        if created:
            kind, serialized_identity = created.groups()
            if serialized_identity in canonical:
                raise ValueError(
                    f"duplicate OP serialized prefix: {serialized_identity}"
                )
            alias_call = extract_balanced_call(initializer, "addIdenticalNames")
            aliases = (
                re.findall(r'"((?:\\.|[^"\\])*)"', alias_call)
                if alias_call is not None
                else []
            )
            method_counts = Counter(
                re.findall(r"\.([A-Za-z_$][A-Za-z0-9_$]*)\s*\(", initializer)
            )
            canonical[serialized_identity] = {
                "canonical_id": serialized_identity,
                "source_field": field,
                "serialized_identity": serialized_identity,
                "initializer_kind": kind,
                "explicit_unused": kind == "unused",
                "identical_names": aliases,
                "condition_expression": extract_balanced_call(
                    initializer, "setCondition"
                ),
                "property_method_counts": dict(sorted(method_counts.items())),
                "initializer_sha256": sha256_bytes(
                    initializer.encode("utf-8")
                ),
            }
            continue
        variant = re.fullmatch(r"([A-Za-z_$][A-Za-z0-9_$]*)", initializer)
        if not variant:
            raise ValueError(
                f"OP prefix {field} is neither canonical nor a direct variant"
            )
        variants.append(
            {
                "source_field": field,
                "target_field": variant.group(1),
                "transformation_rule": "PFX_FIELD_VARIANT",
            }
        )

    field_to_identity = {
        row["source_field"]: identity for identity, row in canonical.items()
    }
    for variant in variants:
        target = field_to_identity.get(variant["target_field"])
        if target is None:
            raise ValueError(
                f"OP variant target is not canonical: {variant['target_field']}"
            )
        variant["canonical_id"] = target
    alias_to_identity: dict[str, str] = {}
    for identity, row in canonical.items():
        for alias in row["identical_names"]:
            previous = alias_to_identity.setdefault(alias, identity)
            if previous != identity:
                raise ValueError(
                    f"OP identical-name alias {alias} has multiple owners"
                )
    return {
        "canonical": canonical,
        "variants": sorted(variants, key=lambda row: row["source_field"]),
        "alias_to_identity": dict(sorted(alias_to_identity.items())),
    }


def parse_td_itemgenerator(source: str) -> dict[str, Any]:
    clean = strip_java_comments(source)
    start = clean.index("public static class ItemGenerator")
    body = clean[start:]
    declarations: dict[str, dict[str, str]] = {}
    for match in re.finditer(
        r"([A-Za-z_$][A-Za-z0-9_$]*)\s*=\s*"
        r'TagData\.createTagData\(\s*"(ITEMGENERATOR\.[A-Z0-9_]+)"\s*\)',
        body,
    ):
        field, identity = match.groups()
        if identity in declarations:
            raise ValueError(f"duplicate TD ITEMGENERATOR identity: {identity}")
        declarations[identity] = {
            "source_field": field,
            "canonical_id": identity,
        }
    field_to_identity = {
        value["source_field"]: identity
        for identity, value in declarations.items()
    }
    aliases = []
    for field, target in re.findall(
        r"\b([A-Za-z_$][A-Za-z0-9_$]*)\s*=\s*"
        r"([A-Za-z_$][A-Za-z0-9_$]*)\s*[,;]",
        body,
    ):
        if field in field_to_identity or target not in field_to_identity:
            continue
        aliases.append(
            {
                "source_field": field,
                "target_field": target,
                "canonical_id": field_to_identity[target],
                "transformation_rule": "DOMAIN_SOURCE_ALIAS",
            }
        )
    return {
        "declarations": declarations,
        "aliases": sorted(aliases, key=lambda row: row["source_field"]),
    }


def value_type(value: Any) -> str:
    if value is None:
        return "null"
    if isinstance(value, bool):
        return "boolean"
    if isinstance(value, int):
        return "integer"
    if isinstance(value, float):
        return "number"
    if isinstance(value, str):
        return "string"
    if isinstance(value, list):
        return "array"
    if isinstance(value, dict):
        return "object"
    return type(value).__name__


def field_cardinality(rows: Iterable[dict[str, Any]]) -> dict[str, Any]:
    materialized = list(rows)
    fields = sorted(set().union(*(row.keys() for row in materialized)))
    profiles: dict[str, Any] = {}
    for field in fields:
        values = [row[field] for row in materialized if field in row]
        distinct = {compact_json(value) for value in values}
        profiles[field] = {
            "present_count": len(values),
            "missing_count": len(materialized) - len(values),
            "null_count": sum(value is None for value in values),
            "distinct_count": len(distinct),
            "type_distribution": dict(
                sorted(Counter(value_type(value) for value in values).items())
            ),
        }
    return {
        "row_count": len(materialized),
        "field_count": len(fields),
        "fields": profiles,
        "distinct_count_distribution": dict(
            sorted(
                Counter(
                    str(profile["distinct_count"])
                    for profile in profiles.values()
                ).items(),
                key=lambda item: int(item[0]),
            )
        ),
    }


def declared_transformation_ids(
    transformations: list[dict[str, Any]],
) -> set[str]:
    return {row["id"] for row in transformations}


def audit_assignments(
    assignments: list[dict[str, Any]],
    declared_ids: set[str],
) -> dict[str, Any]:
    blockers: list[dict[str, Any]] = []
    raw_counts = Counter(row["raw_key"] for row in assignments)
    for raw_key, count in sorted(raw_counts.items()):
        if count != 1:
            blockers.append(
                {
                    "code": "UNIFORM_RAW_MEMBERSHIP_NOT_UNIQUE",
                    "raw_key": raw_key,
                    "membership_count": count,
                }
            )
    groups: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for row in assignments:
        groups[row["canonical_key"]].append(row)
        rule = row.get("transformation_rule")
        if rule not in declared_ids:
            blockers.append(
                {
                    "code": "UNIFORM_UNDECLARED_TRANSFORMATION",
                    "raw_key": row["raw_key"],
                    "transformation_rule": rule,
                }
            )
    for canonical_key, members in sorted(groups.items()):
        if len(members) <= 1:
            continue
        for member in members:
            if member.get("transformation_rule") not in declared_ids:
                blockers.append(
                    {
                        "code": "UNIFORM_UNDECLARED_COLLAPSE",
                        "canonical_key": canonical_key,
                        "raw_key": member["raw_key"],
                    }
                )
    collapse_histogram = Counter(len(members) for members in groups.values())
    return {
        "status": "PASS" if not blockers else "BLOCKED",
        "assignment_count": len(assignments),
        "unique_raw_count": len(raw_counts),
        "output_bucket_count": len(groups),
        "collapse_size_distribution": {
            str(key): value for key, value in sorted(collapse_histogram.items())
        },
        "blockers": blockers,
    }


def audit_field_policy(
    rows: list[dict[str, Any]],
    field_policy: dict[str, str],
) -> list[dict[str, Any]]:
    fields = set().union(*(row.keys() for row in rows))
    undeclared = sorted(fields - set(field_policy))
    stale = sorted(set(field_policy) - fields)
    blockers = [
        {
            "code": "UNIFORM_UNDECLARED_FIELD_COLLAPSE",
            "field": field,
        }
        for field in undeclared
    ]
    blockers.extend(
        {
            "code": "UNIFORM_STALE_FIELD_DECLARATION",
            "field": field,
        }
        for field in stale
    )
    return blockers


def runtime_prefix_documents() -> list[dict[str, Any]]:
    index = load(RUNTIME_PREFIX_INDEX)
    return [load(RUNTIME_PREFIX_ROOT / filename) for filename in index]


def runtime_prefix_tree_digest() -> str:
    digest = hashlib.sha256()
    for filename in load(RUNTIME_PREFIX_INDEX):
        digest.update(filename.encode("utf-8"))
        digest.update(b"\0")
        digest.update((RUNTIME_PREFIX_ROOT / filename).read_bytes())
        digest.update(b"\0")
    return digest.hexdigest()


def source_entry(path: Path) -> dict[str, Any]:
    return {
        "path": path.relative_to(ROOT).as_posix(),
        "sha256": sha256_file(path),
    }


def validate_deferred_and_vocab(
    records: list[dict[str, Any]],
    owner_vocab: set[str],
) -> None:
    for row in records:
        classification = row.get("classification", "unclassified")
        if classification not in CLASSIFICATION_VOCAB:
            raise ValueError(
                f"unknown classification {classification}: {row.get('canonical_id')}"
            )
        if row.get("owner") not in owner_vocab:
            raise ValueError(f"unknown owner in {row.get('canonical_id')}")
        if classification == "deferred_with_reason":
            missing = DEFERRED_FIELDS - set(row)
            if missing:
                raise ValueError(
                    f"deferred row {row.get('canonical_id')} lacks {sorted(missing)}"
                )


def _runtime_mappings(
    policy: dict[str, Any],
    mapping_document: dict[str, Any],
) -> tuple[dict[str, list[dict[str, str]]], list[dict[str, str]]]:
    by_source: dict[str, list[dict[str, str]]] = defaultdict(list)
    relations: list[dict[str, str]] = []
    for row in mapping_document["mappings"]:
        cc_prefix = row["cc_prefix"]
        if cc_prefix is None:
            continue
        rule = (
            "CC_BLOCK_COLLAPSE"
            if row["gt_prefix"] in {"blockGem", "blockIngot"}
            else "CC_PREFIX_RULE_MAPPING"
        )
        relation = {
            "source_prefix": row["gt_prefix"],
            "cc_prefix": cc_prefix,
            "mapping_rule": rule,
            "strategy": row["strategy"],
        }
        by_source[row["gt_prefix"]].append(relation)
        relations.append(relation)
    for row in policy["prefix"]["additional_runtime_mappings"]:
        for cc_prefix in row["cc_prefixes"]:
            relation = {
                "source_prefix": row["source_prefix"],
                "cc_prefix": cc_prefix,
                "mapping_rule": row["rule"],
                "strategy": "stage_extension",
            }
            by_source[row["source_prefix"]].append(relation)
            relations.append(relation)
    return by_source, sorted(
        relations,
        key=lambda row: (row["source_prefix"], row["cc_prefix"]),
    )


def _prefix_classification(
    row: dict[str, Any],
    cc_mappings: list[dict[str, str]],
    policy: dict[str, Any],
) -> dict[str, str]:
    if cc_mappings:
        return {
            "classification": "in_scope",
            "owner": policy["prefix"]["mapped_owner"],
            "reason": "Canonical OP prefix has a current CrucibleCraft runtime mapping.",
        }
    key = "unused_unmapped" if row["explicit_unused"] else "created_unmapped"
    return dict(policy["prefix"][key])


def build_prefix_document(
    op_source: str,
    op_source_entry: dict[str, Any],
    policy: dict[str, Any],
    normalized_document: dict[str, Any],
    mapping_document: dict[str, Any],
    l3_document: dict[str, Any],
    runtime_documents: list[dict[str, Any]],
) -> dict[str, Any]:
    prefix_policy = policy["prefix"]
    expected = policy["expected_counts"]
    parsed = parse_op_prefixes(op_source)
    canonical = parsed["canonical"]
    variants = parsed["variants"]
    normalized_rows = normalized_document["records"]
    if len(normalized_rows) != expected["normalized_prefix_dump"]:
        raise ValueError("normalized prefix dump count drifted")
    source_fields = set().union(*(row.keys() for row in normalized_rows))
    if source_fields != set(prefix_policy["normalized_dump_field_policy"]):
        raise ValueError("normalized prefix dump field schema drifted")

    by_source, cc_relations = _runtime_mappings(policy, mapping_document)
    unknown_mapping_sources = sorted(set(by_source) - set(canonical))
    if unknown_mapping_sources:
        raise ValueError(
            f"runtime mappings reference noncanonical OP prefixes: "
            f"{unknown_mapping_sources}"
        )
    runtime_ids = {
        row["id"].split(":", 1)[-1] for row in runtime_documents
    }
    mapped_runtime_ids = {row["cc_prefix"] for row in cc_relations}
    if runtime_ids != mapped_runtime_ids:
        raise ValueError(
            "current runtime prefix mapping is not 56/56: "
            f"missing={sorted(runtime_ids - mapped_runtime_ids)}, "
            f"extra={sorted(mapped_runtime_ids - runtime_ids)}"
        )
    if len(runtime_ids) != expected["runtime_prefixes"]:
        raise ValueError("runtime prefix count drifted")

    alias_to_identity = parsed["alias_to_identity"]
    variant_to_identity = {
        row["source_field"]: row["canonical_id"] for row in variants
    }
    shadowed_aliases = [
        {
            "raw_key": alias,
            "declared_alias_owner": alias_owner,
            "canonical_owner": alias,
            "resolution": "canonical_declaration_precedence",
            "transformation_rule": "PFX_CANONICAL_PRECEDENCE",
        }
        for alias, alias_owner in sorted(alias_to_identity.items())
        if alias in canonical
    ]
    assignments: list[dict[str, Any]] = []
    exclusions: list[dict[str, Any]] = []
    raw_members_by_canonical: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for raw in sorted(normalized_rows, key=lambda row: row["source_name"]):
        raw_name = raw["source_name"]
        if raw_name in canonical:
            canonical_id = raw_name
            relation = "canonical_identity"
            rule = "PFX_IDENTITY"
        elif raw_name in alias_to_identity:
            canonical_id = alias_to_identity[raw_name]
            relation = "identical_name_alias"
            rule = "PFX_IDENTICAL_NAME_ALIAS"
        elif raw_name in variant_to_identity:
            canonical_id = variant_to_identity[raw_name]
            relation = "source_field_variant"
            rule = "PFX_FIELD_VARIANT"
        else:
            canonical_id = f"excluded:{raw_name}"
            relation = "dump_only"
            rule = "PFX_DUMP_ONLY_EXCLUSION"
        assignment = {
            "raw_key": raw_name,
            "canonical_key": canonical_id,
            "relation": relation,
            "transformation_rule": rule,
            "source_record_sha256": content_sha256(raw),
        }
        assignments.append(assignment)
        if relation == "dump_only":
            exclusions.append(
                {
                    "raw_key": raw_name,
                    "canonical_key": canonical_id,
                    "relation": relation,
                    "transformation_rule": rule,
                    **prefix_policy["dump_only"],
                }
            )
        else:
            raw_members_by_canonical[canonical_id].append(assignment)

    records = []
    for canonical_id, source_row in sorted(canonical.items()):
        mappings = sorted(
            by_source.get(canonical_id, []),
            key=lambda row: row["cc_prefix"],
        )
        record = {
            **source_row,
            "source_variant_fields": [
                row["source_field"]
                for row in variants
                if row["canonical_id"] == canonical_id
            ],
            "raw_members": raw_members_by_canonical.get(canonical_id, []),
            "dump_presence": bool(raw_members_by_canonical.get(canonical_id)),
            "cc_mappings": mappings,
            **_prefix_classification(source_row, mappings, policy),
        }
        records.append(record)

    owner_vocab = set(policy["owner_vocab"])
    validate_deferred_and_vocab(records + exclusions, owner_vocab)
    transformation_ids = declared_transformation_ids(
        prefix_policy["declared_transformations"]
    )
    assignment_audit = audit_assignments(assignments, transformation_ids)
    assignment_audit["blockers"].extend(
        audit_field_policy(
            normalized_rows,
            prefix_policy["normalized_dump_field_policy"],
        )
    )
    assignment_audit["status"] = (
        "PASS" if not assignment_audit["blockers"] else "BLOCKED"
    )
    if assignment_audit["blockers"]:
        raise ValueError(
            f"prefix UNIFORM audit blocked: {assignment_audit['blockers']}"
        )

    classifications = Counter(row["classification"] for row in records)
    excluded_classifications = Counter(
        row["classification"] for row in exclusions
    )
    counted_relations = Counter(row["relation"] for row in assignments)
    relation_counts = {
        relation: counted_relations[relation]
        for relation in (
            "canonical_identity",
            "identical_name_alias",
            "source_field_variant",
            "dump_only",
        )
    }
    output_profile_rows = [
        {
            key: value
            for key, value in row.items()
            if key not in {"raw_members", "property_method_counts"}
        }
        for row in records
    ]
    return {
        "schema": {
            "name": "t13_prefix_denominator",
            "version": SCHEMA_VERSION,
            "canonical_key": "canonical_id",
            "raw_key": "source_name",
            "classification_vocab": policy["classification_vocab"],
            "owner_vocab": policy["owner_vocab"],
        },
        "artifact_kind": "t13_prefix_denominator",
        "generation": {
            "command": policy["generation_command"],
            "policy": POLICY_PATH.relative_to(ROOT).as_posix(),
            "policy_sha256": sha256_file(POLICY_PATH),
            "builder_sha256": sha256_file(Path(__file__).resolve()),
            "proof_tier": "full_replay",
        },
        "source": {
            "revision": policy["fixed_source"]["revision"],
            "op_java": op_source_entry,
            "normalized_prefix_dump": source_entry(NORMALIZED_PREFIXES),
            "prefix_mapping": source_entry(PREFIX_MAPPING),
            "l3_prefix_plan": source_entry(L3_PREFIX_PLAN),
            "runtime_prefix_index": source_entry(RUNTIME_PREFIX_INDEX),
            "runtime_prefix_tree_sha256": runtime_prefix_tree_digest(),
        },
        "declared_transformations": prefix_policy[
            "declared_transformations"
        ],
        "field_cardinality": {
            "source_normalized_dump": field_cardinality(normalized_rows),
            "output_canonical_records": field_cardinality(output_profile_rows),
        },
        "summary": {
            "op_canonical_count": len(records),
            "op_created_count": sum(
                not row["explicit_unused"] for row in records
            ),
            "op_explicit_unused_count": sum(
                row["explicit_unused"] for row in records
            ),
            "op_field_variant_count": len(variants),
            "op_field_variants_present_in_dump_count": relation_counts[
                "source_field_variant"
            ],
            "op_identical_name_alias_count": len(alias_to_identity),
            "op_shadowed_alias_count": len(shadowed_aliases),
            "raw_dump_count": len(normalized_rows),
            "raw_relation_counts": relation_counts,
            "dump_only_count": len(exclusions),
            "canonical_with_cc_mapping_count": sum(
                bool(row["cc_mappings"]) for row in records
            ),
            "cc_runtime_prefix_count": len(runtime_ids),
            "cc_mapping_relation_count": len(cc_relations),
            "classification_counts": dict(sorted(classifications.items())),
            "excluded_classification_counts": dict(
                sorted(excluded_classifications.items())
            ),
            "unclassified_count": classifications["unclassified"]
            + excluded_classifications["unclassified"],
        },
        "difference": {
            "op_without_cc_mapping": [
                row["canonical_id"] for row in records if not row["cc_mappings"]
            ],
            "cc_runtime_prefixes": sorted(runtime_ids),
            "dump_only_raw_members": [row["raw_key"] for row in exclusions],
            "semantics": (
                "These sets are typed differences; raw_dump_count minus "
                "cc_runtime_prefix_count is not an implementation count."
            ),
        },
        "op_field_variants": variants,
        "shadowed_identical_name_aliases": shadowed_aliases,
        "raw_membership": assignments,
        "excluded_raw_members": exclusions,
        "cc_mapping_relations": cc_relations,
        "uniform_audit": assignment_audit,
        "records": records,
        "l3_evidence": {
            "prefix_count": len(l3_document["prefixes"]),
            "prefix_ids": sorted(l3_document["prefixes"]),
            "sha256": content_sha256(l3_document["prefixes"]),
        },
    }


def _material_domain_sets(
    normalized_material_document: dict[str, Any],
) -> dict[str, set[int]]:
    result: dict[str, set[int]] = defaultdict(set)
    for row in normalized_material_document["records"]:
        source_id = row.get("source_id")
        if not isinstance(source_id, int) or source_id < 0:
            continue
        for tag in row.get("generation_tags") or []:
            if tag.startswith("ITEMGENERATOR."):
                result[tag].add(source_id)
    return result


def _direct_runtime_domain(flag: str | None) -> str | None:
    if not flag or not flag.startswith("gt6:itemgenerator/"):
        return None
    suffix = flag.split("/", 1)[1]
    special = {"hotingots": "INGOTS_HOT"}
    return "ITEMGENERATOR." + special.get(suffix, suffix.upper())


def build_domain_document(
    td_source: str,
    td_source_entry: dict[str, Any],
    policy: dict[str, Any],
    report: dict[str, Any],
    generation_bits: dict[str, Any],
    normalized_material_document: dict[str, Any],
    runtime_documents: list[dict[str, Any]],
    prefix_document: dict[str, Any],
) -> dict[str, Any]:
    domain_policy = policy["itemgenerator_domains"]
    expected = policy["expected_counts"]
    parsed = parse_td_itemgenerator(td_source)
    declarations = parsed["declarations"]
    if len(declarations) != expected["td_itemgenerator_declarations"]:
        raise ValueError("TD ItemGenerator declaration count drifted")
    report_rows = report["tags"]["ITEMGENERATOR"]
    if len(report_rows) != expected["observed_itemgenerator_domains"]:
        raise ValueError("ITEMGENERATOR report domain count drifted")
    report_blockers = audit_field_policy(
        report_rows, domain_policy["report_field_policy"]
    )
    report_by_tag = {row["tag"]: row for row in report_rows}
    observed = set(report_by_tag)
    if len(report_by_tag) != len(report_rows):
        raise ValueError("duplicate ITEMGENERATOR report tag")
    if not observed <= set(declarations):
        raise ValueError("report contains ITEMGENERATOR tag absent from TD.java")

    bit_tags = {
        tag
        for tag in generation_bits["generation_tag_flags"]
        if tag.startswith("ITEMGENERATOR.")
    }
    if bit_tags != observed:
        raise ValueError(
            "gt6_generation_bits ITEMGENERATOR universe differs from report"
        )
    material_sets = _material_domain_sets(normalized_material_document)
    if set(material_sets) != observed:
        raise ValueError(
            "normalized material ITEMGENERATOR universe differs from report"
        )
    for tag, row in report_by_tag.items():
        if len(material_sets[tag]) != row["material_count"]:
            raise ValueError(f"independent material count drifted for {tag}")

    current = set(domain_policy["current_cc_domains"])
    if len(current) != expected["current_cc_itemgenerator_domains"]:
        raise ValueError("current CC ITEMGENERATOR policy count drifted")
    if not current <= observed:
        raise ValueError("current CC domain absent from source denominator")
    noncurrent_policy = domain_policy["classification"]
    if set(noncurrent_policy) != observed - current:
        raise ValueError("noncurrent domain classification is not 25/25")
    current_owners = domain_policy["current_owner"]
    if set(current_owners) != current:
        raise ValueError("current domain owner policy is not 16/16")

    direct_runtime: dict[str, list[str]] = defaultdict(list)
    for runtime in runtime_documents:
        domain = _direct_runtime_domain(runtime.get("generation_flag"))
        if domain:
            direct_runtime[domain].append(runtime["id"])
    prefix_cc_map: dict[str, list[str]] = defaultdict(list)
    for relation in prefix_document["cc_mapping_relations"]:
        prefix_cc_map[relation["source_prefix"]].append(
            "cruciblecraft:" + relation["cc_prefix"]
        )

    exact_prefixes = generation_bits["itemgenerator_to_prefixes"]
    records = []
    for tag in sorted(observed):
        source = report_by_tag[tag]
        mapped_prefixes = exact_prefixes.get(tag, [])
        exact_runtime_forms = sorted(
            {
                cc_id
                for source_prefix in mapped_prefixes
                for cc_id in prefix_cc_map.get(source_prefix, [])
            }
        )
        if tag in current:
            classification = {
                "classification": "in_scope",
                "owner": current_owners[tag],
                "scope_group": "current_cc_16",
                "reason": (
                    "Domain is part of the frozen second-stage 16/25 "
                    "CrucibleCraft boundary."
                ),
            }
        else:
            classification = dict(noncurrent_policy[tag])
        members = sorted(material_sets[tag])
        records.append(
            {
                "canonical_id": tag,
                "source_field": declarations[tag]["source_field"],
                "source_material_count": len(members),
                "source_material_id_set_sha256": content_sha256(members),
                "generation_flag": generation_bits[
                    "generation_tag_flags"
                ][tag],
                "exact_single_tag_prefixes": mapped_prefixes,
                "exact_single_tag_cc_runtime_forms": exact_runtime_forms,
                "direct_cc_generation_flag_forms": sorted(
                    direct_runtime.get(tag, [])
                ),
                "cc_current_domain": tag in current,
                **classification,
            }
        )

    source_exclusions = [
        {
            "canonical_id": tag,
            "source_field": declarations[tag]["source_field"],
            "classification": "out_of_scope",
            "owner": "out",
            "reason": (
                "TD.ItemGenerator declares the identity, but the fixed material "
                "dump has no members and therefore it is outside the 25-domain "
                "observed denominator."
            ),
            "transformation_rule": "DOMAIN_OBSERVED_FILTER",
        }
        for tag in sorted(set(declarations) - observed)
    ]
    validate_deferred_and_vocab(records + source_exclusions, set(policy["owner_vocab"]))
    transformations = domain_policy["declared_transformations"]
    transformation_ids = declared_transformation_ids(transformations)
    assignments = [
        {
            "raw_key": tag,
            "canonical_key": tag,
            "transformation_rule": "DOMAIN_TD_IDENTITY",
        }
        for tag in sorted(observed)
    ]
    uniform = audit_assignments(assignments, transformation_ids)
    uniform["blockers"].extend(report_blockers)
    uniform["status"] = "PASS" if not uniform["blockers"] else "BLOCKED"
    if uniform["blockers"]:
        raise ValueError(f"domain UNIFORM audit blocked: {uniform['blockers']}")

    classifications = Counter(row["classification"] for row in records)
    owners = Counter(row["owner"] for row in records)
    return {
        "schema": {
            "name": "t13_itemgenerator_domain_denominator",
            "version": SCHEMA_VERSION,
            "canonical_key": "canonical_id",
            "classification_vocab": policy["classification_vocab"],
            "owner_vocab": policy["owner_vocab"],
        },
        "artifact_kind": "t13_itemgenerator_domain_denominator",
        "generation": {
            "command": policy["generation_command"],
            "policy": POLICY_PATH.relative_to(ROOT).as_posix(),
            "policy_sha256": sha256_file(POLICY_PATH),
            "builder_sha256": sha256_file(Path(__file__).resolve()),
            "proof_tier": "full_replay",
        },
        "source": {
            "revision": policy["fixed_source"]["revision"],
            "td_java": td_source_entry,
            "tag_domain_report": source_entry(TAG_DOMAIN_REPORT),
            "generation_bits": source_entry(GENERATION_BITS),
            "normalized_materials": source_entry(NORMALIZED_MATERIALS),
            "runtime_prefix_index": source_entry(RUNTIME_PREFIX_INDEX),
            "runtime_prefix_tree_sha256": runtime_prefix_tree_digest(),
        },
        "identity_policy": {
            "source_declaration": "TD.ItemGenerator TagData.createTagData",
            "observed_filter": (
                "identity must be present in the fixed tag report, generation "
                "bits, and independent nonnegative source_id material scan"
            ),
            "material_key": "source_id >= 0",
        },
        "declared_transformations": transformations,
        "field_cardinality": {
            "source_report_rows": field_cardinality(report_rows),
            "output_domain_records": field_cardinality(records),
        },
        "summary": {
            "td_itemgenerator_declaration_count": len(declarations),
            "td_source_alias_count": len(parsed["aliases"]),
            "observed_domain_count": len(records),
            "source_only_exclusion_count": len(source_exclusions),
            "cc_current_domain_count": sum(
                row["cc_current_domain"] for row in records
            ),
            "classification_counts": dict(sorted(classifications.items())),
            "owner_counts": dict(sorted(owners.items())),
            "unclassified_count": classifications["unclassified"],
            "all_source_material_counts_independently_rebuilt": True,
        },
        "td_source_aliases": parsed["aliases"],
        "source_only_exclusions": source_exclusions,
        "uniform_audit": uniform,
        "records": records,
    }


def build_documents(
    *,
    op_source_path: Path | None = None,
    td_source_path: Path | None = None,
    allow_fetch: bool = True,
) -> tuple[dict[str, Any], dict[str, Any]]:
    policy = load(POLICY_PATH)
    if set(policy["classification_vocab"]) != CLASSIFICATION_VOCAB:
        raise ValueError("T13 classification vocabulary drifted")
    fixed = policy["fixed_source"]
    op_source, op_entry = load_fixed_source(
        fixed["op_java"],
        override=op_source_path,
        allow_fetch=allow_fetch,
    )
    td_source, td_entry = load_fixed_source(
        fixed["td_java"],
        override=td_source_path,
        allow_fetch=allow_fetch,
    )
    normalized_prefixes = load(NORMALIZED_PREFIXES)
    normalized_materials = load(NORMALIZED_MATERIALS)
    mapping = load(PREFIX_MAPPING)
    l3 = load(L3_PREFIX_PLAN)
    runtime = runtime_prefix_documents()
    prefix_document = build_prefix_document(
        op_source,
        op_entry,
        policy,
        normalized_prefixes,
        mapping,
        l3,
        runtime,
    )
    domain_document = build_domain_document(
        td_source,
        td_entry,
        policy,
        load(TAG_DOMAIN_REPORT),
        load(GENERATION_BITS),
        normalized_materials,
        runtime,
        prefix_document,
    )
    return prefix_document, domain_document


def check_or_write(
    outputs: dict[Path, dict[str, Any]],
    *,
    check: bool,
) -> None:
    stale = []
    for path, document in outputs.items():
        expected = stable_json(document)
        if check:
            actual = path.read_text(encoding="utf-8") if path.is_file() else None
            if actual != expected:
                stale.append(path.relative_to(ROOT).as_posix())
        else:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(expected, encoding="utf-8")
    if stale:
        raise SystemExit(
            "T13b denominator artifacts are stale: " + ", ".join(stale)
        )


def reference_only_check() -> list[str]:
    errors: list[str] = []
    if not PREFIX_OUTPUT.is_file() or not DOMAIN_OUTPUT.is_file():
        return ["missing committed T13b denominator artifact"]
    try:
        prefix = load(PREFIX_OUTPUT)
        domain = load(DOMAIN_OUTPUT)
        policy = load(POLICY_PATH)
    except (OSError, ValueError, json.JSONDecodeError) as exc:
        return [f"invalid committed T13b denominator artifact: {exc}"]

    expected_generation = {
        "policy_sha256": sha256_file(POLICY_PATH),
        "builder_sha256": sha256_file(Path(__file__).resolve()),
        "proof_tier": "full_replay",
    }
    for label, document in (("prefix", prefix), ("domain", domain)):
        if document.get("schema", {}).get("version") != SCHEMA_VERSION:
            errors.append(f"T13b {label} compact schema drifted")
        generation = document.get("generation") or {}
        for key, expected in expected_generation.items():
            if generation.get(key) != expected:
                errors.append(
                    f"T13b {label} generation {key} drifted"
                )
        if document.get("summary", {}).get("unclassified_count") != 0:
            errors.append(f"T13b {label} has unclassified records")
        if document.get("uniform_audit", {}).get("blockers"):
            errors.append(f"T13b {label} has uniformity blockers")

    fixed = policy["fixed_source"]
    source_pairs = (
        ("prefix", prefix["source"]["op_java"], fixed["op_java"]),
        ("domain", domain["source"]["td_java"], fixed["td_java"]),
    )
    for label, receipt, expected in source_pairs:
        if (
            receipt.get("path") != expected["path"]
            or receipt.get("git_blob_sha1") != expected["git_blob_sha1"]
            or not isinstance(receipt.get("sha256"), str)
            or len(receipt["sha256"]) != 64
        ):
            errors.append(f"T13b {label} fixed-source receipt drifted")

    for label, document, fetched_key in (
        ("prefix", prefix, "op_java"),
        ("domain", domain, "td_java"),
    ):
        for key, receipt in document["source"].items():
            if key in {fetched_key, "revision", "runtime_prefix_tree_sha256"}:
                continue
            if not isinstance(receipt, dict) or "path" not in receipt:
                continue
            path = ROOT / receipt["path"]
            if not path.is_file():
                errors.append(f"T13b {label} input missing: {key}")
            elif sha256_file(path) != receipt.get("sha256"):
                errors.append(f"T13b {label} input hash drifted: {key}")
        if (
            document["source"].get("runtime_prefix_tree_sha256")
            != runtime_prefix_tree_digest()
        ):
            errors.append(f"T13b {label} runtime prefix tree drifted")
    return errors


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--no-fetch", action="store_true")
    check_mode = parser.add_mutually_exclusive_group()
    check_mode.add_argument("--reference-only", action="store_true")
    check_mode.add_argument("--full-replay", action="store_true")
    parser.add_argument("--op-source", type=Path)
    parser.add_argument("--td-source", type=Path)
    args = parser.parse_args()
    if (
        args.reference_only or args.full_replay
    ) and not args.check:
        parser.error("--reference-only and --full-replay require --check")
    if args.reference_only:
        errors = reference_only_check()
        if errors:
            raise SystemExit(
                "T13b compact denominator artifacts are stale: "
                + ", ".join(errors)
            )
        prefix = load(PREFIX_OUTPUT)
        domains = load(DOMAIN_OUTPUT)
    else:
        prefix, domains = build_documents(
            op_source_path=args.op_source,
            td_source_path=args.td_source,
            allow_fetch=not args.no_fetch,
        )
        check_or_write(
            {
                PREFIX_OUTPUT: prefix,
                DOMAIN_OUTPUT: domains,
            },
            check=args.check,
        )
    print(
        compact_json(
            {
                "op_canonical": prefix["summary"]["op_canonical_count"],
                "raw_dump": prefix["summary"]["raw_dump_count"],
                "cc_runtime_prefixes": prefix["summary"][
                    "cc_runtime_prefix_count"
                ],
                "domain_classifications": domains["summary"][
                    "classification_counts"
                ],
                "domains": domains["summary"]["observed_domain_count"],
                "unclassified": (
                    prefix["summary"]["unclassified_count"]
                    + domains["summary"]["unclassified_count"]
                ),
            }
        )
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
