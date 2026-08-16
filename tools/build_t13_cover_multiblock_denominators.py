#!/usr/bin/env python3
"""Build the fixed-source T13d Cover and multiblock denominators."""

from __future__ import annotations

import argparse
import hashlib
import json
import re
from collections import Counter
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path
from typing import Any
from urllib.request import Request, urlopen


ROOT = Path(__file__).resolve().parents[1]
TOOLS = ROOT / "tools"
POLICY = TOOLS / "t13_cover_multiblock_policy.json"
OUTPUT_DIR = TOOLS / "t13_denominators"
OUTPUTS = {
    "cover_kinds": OUTPUT_DIR / "cover_kinds.json",
    "multiblock_kinds": OUTPUT_DIR / "multiblock_kinds.json",
}
DOMAIN_IDS = frozenset(OUTPUTS)
DISPOSITIONS = frozenset(
    {"in_scope", "deferred_with_reason", "out_of_scope"}
)
IMPLEMENTATION_STATUSES = {
    "cover_kinds": frozenset(
        {
            "implemented",
            "selected_t19",
            "deferred_with_reason",
            "out_of_scope",
        }
    ),
    "multiblock_kinds": frozenset(
        {
            "implemented",
            "third_stage_deferred",
            "post_t19",
            "selected_t23",
            "out_of_scope",
        }
    ),
}
COLLAPSE_DIMENSIONS = frozenset({"material", "numeric", "direction"})
REQUIRED_DEFERRED_FIELDS = (
    "reason",
    "owner",
    "replacement_condition",
    "recheck_point",
)
CC_COVER_BEHAVIORS = frozenset(
    {"FILTER", "ONE_WAY_VALVE", "OUTPUT_PUMP"}
)
CC_MULTIBLOCKS = frozenset({"coke_oven", "large_centrifuge"})
GENERATION_COMMAND = (
    "python tools/build_t13_cover_multiblock_denominators.py "
    "--refresh-source-manifest --write"
)


def load(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def stable_json(value: Any) -> str:
    return json.dumps(
        value,
        ensure_ascii=False,
        indent=2,
        sort_keys=True,
    ) + "\n"


def canonical_hash(value: Any) -> str:
    payload = json.dumps(
        value,
        ensure_ascii=False,
        sort_keys=True,
        separators=(",", ":"),
    ).encode("utf-8")
    return hashlib.sha256(payload).hexdigest()


def git_blob_sha1(data: bytes) -> str:
    header = f"blob {len(data)}\0".encode("ascii")
    return hashlib.sha1(header + data).hexdigest()


def _request_json(url: str) -> Any:
    request = Request(
        url,
        headers={
            "Accept": "application/vnd.github+json",
            "User-Agent": "CrucibleCraft-T13d-denominator-builder",
        },
    )
    with urlopen(request, timeout=60) as response:
        return json.loads(response.read().decode("utf-8"))


def _request_bytes(url: str) -> bytes:
    request = Request(
        url,
        headers={"User-Agent": "CrucibleCraft-T13d-denominator-builder"},
    )
    with urlopen(request, timeout=60) as response:
        return response.read()


def _mask_java_noncode(source: str) -> str:
    """Mask comments and literals while preserving newlines and braces."""
    out: list[str] = []
    index = 0
    state = "code"
    while index < len(source):
        char = source[index]
        next_char = source[index + 1] if index + 1 < len(source) else ""
        if state == "code":
            if char == "/" and next_char == "/":
                out.extend("  ")
                index += 2
                state = "line_comment"
                continue
            if char == "/" and next_char == "*":
                out.extend("  ")
                index += 2
                state = "block_comment"
                continue
            if char == '"':
                out.append(" ")
                index += 1
                state = "string"
                continue
            if char == "'":
                out.append(" ")
                index += 1
                state = "char"
                continue
            out.append(char)
            index += 1
            continue
        if state == "line_comment":
            if char == "\n":
                out.append("\n")
                state = "code"
            else:
                out.append(" ")
            index += 1
            continue
        if state == "block_comment":
            if char == "*" and next_char == "/":
                out.extend("  ")
                index += 2
                state = "code"
            else:
                out.append("\n" if char == "\n" else " ")
                index += 1
            continue
        if char == "\\":
            out.append(" ")
            if next_char:
                out.append("\n" if next_char == "\n" else " ")
                index += 2
            else:
                index += 1
            continue
        if (state == "string" and char == '"') or (
            state == "char" and char == "'"
        ):
            out.append(" ")
            index += 1
            state = "code"
            continue
        out.append("\n" if char == "\n" else " ")
        index += 1
    return "".join(out)


PUBLIC_TYPE = re.compile(
    r"^\s*public\s+"
    r"(?:(?P<abstract>abstract)\s+)?"
    r"(?P<kind>class|interface|enum)\s+"
    r"(?P<symbol>[A-Za-z_$][A-Za-z0-9_$]*)\b"
)


def parse_java_top_level_symbols(source: str) -> list[dict[str, Any]]:
    """Return public top-level Java declarations, excluding nested types."""
    masked = _mask_java_noncode(source)
    source_lines = source.splitlines()
    depth = 0
    declarations: list[dict[str, Any]] = []
    for line_number, masked_line in enumerate(
        masked.splitlines(), start=1
    ):
        if depth == 0:
            match = PUBLIC_TYPE.match(masked_line)
            if match:
                declarations.append(
                    {
                        "source_symbol": match.group("symbol"),
                        "declaration_kind": match.group("kind"),
                        "abstract": bool(match.group("abstract"))
                        or match.group("kind") == "interface",
                        "declaration": source_lines[line_number - 1].strip(),
                        "line": line_number,
                    }
                )
        depth += masked_line.count("{") - masked_line.count("}")
        if depth < 0:
            raise ValueError("Java brace depth became negative")
    if depth != 0:
        raise ValueError("Java source has unbalanced braces")
    return declarations


def expected_paths(
    policy: dict[str, Any],
    domain_id: str,
) -> set[str]:
    """Construct the expected path set independently of grouping rules."""
    paths: set[str] = set()
    for package in policy["domains"][domain_id]["source_packages"]:
        root = package["path"].rstrip("/")
        for symbol in package["expected_symbols"]:
            path = f"{root}/{symbol}.java"
            if path in paths:
                raise ValueError(f"duplicate expected source path {path}")
            paths.add(path)
    return paths


def _walk_contents(
    repository: str,
    revision: str,
    root_path: str,
) -> list[dict[str, Any]]:
    api_root = repository.replace(
        "https://github.com/", "https://api.github.com/repos/"
    )
    url = f"{api_root}/contents/{root_path}?ref={revision}"
    entries = _request_json(url)
    if not isinstance(entries, list):
        raise ValueError(
            f"GitHub contents response is not a list: {root_path}"
        )
    return [
        entry
        for entry in entries
        if entry["type"] == "file" and entry["path"].endswith(".java")
    ]


def _fetch_source_record(entry: dict[str, Any]) -> dict[str, Any]:
    data = _request_bytes(entry["download_url"])
    actual_blob = git_blob_sha1(data)
    if actual_blob != entry["sha"]:
        raise ValueError(
            f"{entry['path']}: expected blob {entry['sha']}, "
            f"downloaded {actual_blob}"
        )
    source = data.decode("utf-8")
    declarations = parse_java_top_level_symbols(source)
    expected_symbol = Path(entry["path"]).stem
    if [row["source_symbol"] for row in declarations] != [expected_symbol]:
        raise ValueError(
            f"{entry['path']}: expected one public top-level symbol "
            f"{expected_symbol}, got {declarations}"
        )
    declaration = declarations[0]
    return {
        "abstract": declaration["abstract"],
        "declaration": declaration["declaration"],
        "declaration_kind": declaration["declaration_kind"],
        "git_blob_sha1": actual_blob,
        "raw_url": entry["download_url"],
        "source_path": entry["path"],
        "source_sha256": hashlib.sha256(data).hexdigest(),
        "source_symbol": declaration["source_symbol"],
    }


def refresh_source_manifest(
    policy: dict[str, Any],
) -> dict[str, Any]:
    source = policy["source"]
    repository = source["repository"]
    revision = source["revision"]
    api_root = repository.replace(
        "https://github.com/", "https://api.github.com/repos/"
    )
    commit = _request_json(f"{api_root}/git/commits/{revision}")
    actual_tree = commit["tree"]["sha"]
    if actual_tree != source["tree_sha1"]:
        raise ValueError(
            f"revision tree drifted: expected {source['tree_sha1']}, "
            f"got {actual_tree}"
        )

    refreshed = json.loads(json.dumps(policy))
    for domain_id in sorted(DOMAIN_IDS):
        domain = refreshed["domains"][domain_id]
        entries: dict[str, dict[str, Any]] = {}
        for package in domain["source_packages"]:
            for entry in _walk_contents(
                repository,
                revision,
                package["path"],
            ):
                if entry["path"] in entries:
                    raise ValueError(
                        f"source path reached twice: {entry['path']}"
                    )
                entries[entry["path"]] = entry
        wanted = expected_paths(refreshed, domain_id)
        actual = set(entries)
        if actual != wanted:
            raise ValueError(
                f"{domain_id} fixed tree path set drifted; "
                f"missing={sorted(wanted - actual)}, "
                f"extra={sorted(actual - wanted)}"
            )
        with ThreadPoolExecutor(max_workers=12) as executor:
            records = list(
                executor.map(
                    _fetch_source_record,
                    (entries[path] for path in sorted(entries)),
                )
            )
        domain["source_manifest"] = records
    validate_policy(refreshed)
    return refreshed


def _rules_by_symbol(
    domain: dict[str, Any],
) -> tuple[dict[str, dict[str, Any]], dict[str, dict[str, Any]]]:
    canonical: dict[str, dict[str, Any]] = {}
    excluded: dict[str, dict[str, Any]] = {}
    rule_ids: set[str] = set()
    for rule in domain["canonical_rules"]:
        rule_id = rule.get("id")
        if not rule_id or rule_id in rule_ids:
            raise ValueError(f"invalid or duplicate canonical rule id {rule_id}")
        rule_ids.add(rule_id)
        if not rule.get("reason"):
            raise ValueError(f"{rule_id}: canonical rule needs a reason")
        members = rule.get("source_symbols") or []
        if len(members) != len(set(members)) or not members:
            raise ValueError(f"{rule_id}: invalid source_symbols")
        collapse = rule.get("collapse_rule")
        if len(members) > 1:
            if not collapse or not collapse.get("id"):
                raise ValueError(
                    f"{rule_id}: many-to-one canonicalization is undeclared"
                )
            dimensions = set(collapse.get("variant_dimensions") or [])
            if (
                not dimensions
                or not dimensions <= COLLAPSE_DIMENSIONS
                or not collapse.get("statement")
                or not collapse.get("source_evidence")
            ):
                raise ValueError(
                    f"{rule_id}: collapse must be limited to declared "
                    "material/numeric/direction variants"
                )
        elif collapse is not None:
            raise ValueError(
                f"{rule_id}: a singleton may not declare a collapse"
            )
        for symbol in members:
            if symbol in canonical or symbol in excluded:
                raise ValueError(f"{symbol}: assigned more than once")
            canonical[symbol] = rule
    for rule in domain["exclusion_rules"]:
        rule_id = rule.get("id")
        symbol = rule.get("source_symbol")
        if not rule_id or rule_id in rule_ids:
            raise ValueError(f"invalid or duplicate exclusion rule id {rule_id}")
        rule_ids.add(rule_id)
        if not symbol or not rule.get("reason"):
            raise ValueError(
                f"{rule_id}: exclusion needs source_symbol and reason"
            )
        if symbol in canonical or symbol in excluded:
            raise ValueError(f"{symbol}: assigned more than once")
        excluded[symbol] = rule
    return canonical, excluded


def _classification_by_canonical(
    domain_id: str,
    domain: dict[str, Any],
) -> dict[str, dict[str, Any]]:
    result: dict[str, dict[str, Any]] = {}
    assignment_ids: set[str] = set()
    for authored_row in domain["classification_assignments"]:
        row = dict(authored_row)
        template_id = row.pop("deferred_template", None)
        if template_id is not None:
            template = domain.get("deferred_templates", {}).get(template_id)
            if template is None:
                raise ValueError(
                    f"{row.get('id')}: unknown deferred template {template_id}"
                )
            if row.get("deferred") is not None:
                raise ValueError(
                    f"{row.get('id')}: deferred metadata and template conflict"
                )
            row["deferred"] = dict(template)
        assignment_id = row.get("id")
        canonical_id = row.get("canonical_id")
        if not assignment_id or assignment_id in assignment_ids:
            raise ValueError(
                f"invalid or duplicate classification id {assignment_id}"
            )
        assignment_ids.add(assignment_id)
        if not canonical_id or canonical_id in result:
            raise ValueError(
                f"duplicate or missing canonical classification {canonical_id}"
            )
        if row.get("disposition") not in DISPOSITIONS:
            raise ValueError(
                f"{assignment_id}: invalid disposition "
                f"{row.get('disposition')}"
            )
        if (
            row.get("implementation_status")
            not in IMPLEMENTATION_STATUSES[domain_id]
        ):
            raise ValueError(
                f"{assignment_id}: invalid implementation status"
            )
        if not row.get("reason"):
            raise ValueError(
                f"{assignment_id}: classification needs a reason"
            )
        if row["disposition"] == "deferred_with_reason":
            deferred = row.get("deferred") or {}
            missing = [
                field
                for field in REQUIRED_DEFERRED_FIELDS
                if not deferred.get(field)
            ]
            if missing:
                raise ValueError(
                    f"{assignment_id}: incomplete deferred fields {missing}"
                )
        elif row.get("deferred") is not None:
            raise ValueError(
                f"{assignment_id}: non-deferred row has deferred metadata"
            )
        if domain_id == "cover_kinds":
            behavior = row.get("cc_behavior")
            if row["implementation_status"] == "implemented":
                if behavior not in CC_COVER_BEHAVIORS:
                    raise ValueError(
                        f"{assignment_id}: implemented cover lacks CC mapping"
                    )
            elif behavior is not None:
                raise ValueError(
                    f"{assignment_id}: only implemented covers map to CC"
                )
        else:
            mapping = row.get("cc_multiblock")
            if row["implementation_status"] == "implemented":
                if mapping not in CC_MULTIBLOCKS:
                    raise ValueError(
                        f"{assignment_id}: implemented multiblock lacks CC "
                        "mapping"
                    )
            elif mapping is not None:
                raise ValueError(
                    f"{assignment_id}: only implemented multiblocks map to CC"
                )
        result[canonical_id] = row
    return result


def validate_policy(policy: dict[str, Any]) -> None:
    if policy.get("schema_version") != 1:
        raise ValueError("unsupported T13d policy schema")
    if set(policy.get("domains", {})) != DOMAIN_IDS:
        raise ValueError("T13d policy domain set drifted")
    source = policy.get("source") or {}
    for field in ("repository", "revision", "tree_sha1"):
        if not source.get(field):
            raise ValueError(f"source.{field} is required")
    if not re.fullmatch(r"[0-9a-f]{40}", source["revision"]):
        raise ValueError("source revision must be a full commit SHA")
    if not re.fullmatch(r"[0-9a-f]{40}", source["tree_sha1"]):
        raise ValueError("source tree must be a full tree SHA")

    for domain_id in sorted(DOMAIN_IDS):
        domain = policy["domains"][domain_id]
        wanted_paths = expected_paths(policy, domain_id)
        manifest = domain.get("source_manifest") or []
        manifest_by_path = {
            record["source_path"]: record for record in manifest
        }
        if len(manifest_by_path) != len(manifest):
            raise ValueError(f"{domain_id}: duplicate source manifest path")
        if set(manifest_by_path) != wanted_paths:
            raise ValueError(
                f"{domain_id}: source manifest does not match expected paths"
            )
        raw_symbols: set[str] = set()
        for path, record in manifest_by_path.items():
            if not re.fullmatch(
                r"[0-9a-f]{40}", record.get("git_blob_sha1", "")
            ):
                raise ValueError(f"{path}: invalid git blob SHA")
            if not re.fullmatch(
                r"[0-9a-f]{64}", record.get("source_sha256", "")
            ):
                raise ValueError(f"{path}: invalid source SHA-256")
            expected_symbol = Path(path).stem
            if record.get("source_symbol") != expected_symbol:
                raise ValueError(f"{path}: source symbol/path mismatch")
            declaration = record.get("declaration", "").rstrip()
            if declaration.endswith("{"):
                declaration = declaration[:-1].rstrip()
            parsed = parse_java_top_level_symbols(declaration + " {}\n")
            if (
                len(parsed) != 1
                or parsed[0]["source_symbol"] != expected_symbol
                or parsed[0]["declaration_kind"]
                != record.get("declaration_kind")
                or parsed[0]["abstract"] != record.get("abstract")
            ):
                raise ValueError(f"{path}: stored declaration does not parse")
            if expected_symbol in raw_symbols:
                raise ValueError(
                    f"{domain_id}: source symbol is not unique "
                    f"{expected_symbol}"
                )
            raw_symbols.add(expected_symbol)

        canonical_by_symbol, excluded_by_symbol = _rules_by_symbol(domain)
        assigned = set(canonical_by_symbol) | set(excluded_by_symbol)
        if assigned != raw_symbols:
            raise ValueError(
                f"{domain_id}: raw partition is not closed; "
                f"missing={sorted(raw_symbols - assigned)}, "
                f"extra={sorted(assigned - raw_symbols)}"
            )
        manifest_by_symbol = {
            row["source_symbol"]: row for row in manifest
        }
        non_concrete = [
            symbol
            for symbol in canonical_by_symbol
            if manifest_by_symbol[symbol]["abstract"]
            or manifest_by_symbol[symbol]["declaration_kind"] != "class"
        ]
        if non_concrete:
            raise ValueError(
                f"{domain_id}: non-concrete symbols entered canonical "
                f"kinds: {sorted(non_concrete)}"
            )
        canonical_ids = [row["canonical_id"] for row in domain["canonical_rules"]]
        if len(canonical_ids) != len(set(canonical_ids)):
            raise ValueError(f"{domain_id}: duplicate canonical id")
        classifications = _classification_by_canonical(domain_id, domain)
        if set(classifications) != set(canonical_ids):
            raise ValueError(
                f"{domain_id}: canonical classification is not closed"
            )


def _manifest_digest(records: list[dict[str, Any]]) -> str:
    identity_rows = [
        {
            "git_blob_sha1": row["git_blob_sha1"],
            "source_path": row["source_path"],
            "source_sha256": row["source_sha256"],
            "source_symbol": row["source_symbol"],
        }
        for row in sorted(records, key=lambda value: value["source_path"])
    ]
    return canonical_hash(identity_rows)


def _path_set_digest(paths: set[str]) -> str:
    payload = ("\n".join(sorted(paths)) + "\n").encode("utf-8")
    return hashlib.sha256(payload).hexdigest()


def _field_cardinality(
    raw_records: list[dict[str, Any]],
    canonical_rows: list[dict[str, Any]],
) -> dict[str, Any]:
    source_fields = (
        "source_path",
        "source_symbol",
        "source_blob",
        "declaration_kind",
    )
    output_fields = (
        "canonical_id",
        "disposition",
        "implementation_status",
        "roadmap_bucket",
    )
    return {
        "canonical_output": {
            field: len(
                {
                    json.dumps(row.get(field), sort_keys=True)
                    for row in canonical_rows
                }
            )
            for field in output_fields
        },
        "raw_source": {
            field: len(
                {
                    json.dumps(row.get(field), sort_keys=True)
                    for row in raw_records
                }
            )
            for field in source_fields
        },
    }


def build_domain(
    policy: dict[str, Any],
    domain_id: str,
) -> dict[str, Any]:
    validate_policy(policy)
    domain = policy["domains"][domain_id]
    source = policy["source"]
    manifest = sorted(
        domain["source_manifest"],
        key=lambda row: row["source_path"],
    )
    canonical_by_symbol, excluded_by_symbol = _rules_by_symbol(domain)
    classifications = _classification_by_canonical(domain_id, domain)
    canonical_rows: list[dict[str, Any]] = []
    transformations: list[dict[str, Any]] = []
    for rule in sorted(
        domain["canonical_rules"],
        key=lambda row: row["canonical_id"],
    ):
        assignment = classifications[rule["canonical_id"]]
        members = sorted(rule["source_symbols"])
        row = {
            "canonical_id": rule["canonical_id"],
            "canonical_rule_id": rule["id"],
            "cc_behavior": assignment.get("cc_behavior"),
            "cc_multiblock": assignment.get("cc_multiblock"),
            "classification_rule_id": assignment["id"],
            "deferred": assignment.get("deferred"),
            "disposition": assignment["disposition"],
            "implementation_status": assignment["implementation_status"],
            "normalization_reason": rule["reason"],
            "raw_members": members,
            "roadmap_bucket": assignment["roadmap_bucket"],
            "scope_reason": assignment["reason"],
        }
        canonical_rows.append(row)
        if rule.get("collapse_rule"):
            transformations.append(
                {
                    **rule["collapse_rule"],
                    "canonical_id": rule["canonical_id"],
                    "raw_members": members,
                }
            )

    manifest_by_symbol = {
        row["source_symbol"]: row for row in manifest
    }
    raw_records: list[dict[str, Any]] = []
    exclusions: list[dict[str, Any]] = []
    for symbol, record in sorted(manifest_by_symbol.items()):
        base = {
            "abstract": record["abstract"],
            "declaration": record["declaration"],
            "declaration_kind": record["declaration_kind"],
            "normalized_row_key": symbol,
            "source_blob": record["git_blob_sha1"],
            "source_path": record["source_path"],
            "source_revision": source["revision"],
            "source_symbol": symbol,
            "source_symbol_or_extraction_key": symbol,
        }
        if symbol in canonical_by_symbol:
            rule = canonical_by_symbol[symbol]
            raw_records.append(
                {
                    **base,
                    "canonical_id": rule["canonical_id"],
                    "partition": "canonical",
                    "rule_id": rule["id"],
                    "rule_reason": rule["reason"],
                }
            )
        else:
            rule = excluded_by_symbol[symbol]
            exclusion = {
                **base,
                "exclusion_kind": rule["exclusion_kind"],
                "partition": "excluded",
                "rule_id": rule["id"],
                "rule_reason": rule["reason"],
            }
            exclusions.append(exclusion)
            raw_records.append(exclusion)

    classification_counts = Counter(
        row["disposition"] for row in canonical_rows
    )
    implementation_counts = Counter(
        row["implementation_status"] for row in canonical_rows
    )
    field_cardinality = _field_cardinality(raw_records, canonical_rows)
    reduction = len(raw_records) - len(canonical_rows)
    declared_reduction = len(exclusions) + sum(
        len(row["raw_members"]) - 1 for row in transformations
    )
    undeclared = []
    if reduction != declared_reduction:
        undeclared.append(
            {
                "field": "source_symbol_to_canonical_id",
                "raw_reduction": reduction,
                "declared_reduction": declared_reduction,
            }
        )
    uniform_statuses: list[str] = []
    status = "READY" if not undeclared else "UNIFORM_UNDECLARED_COLLAPSE"
    if status.startswith("UNIFORM_"):
        uniform_statuses.append(status)
    paths = expected_paths(policy, domain_id)
    manifest_digest = _manifest_digest(manifest)
    return {
        "canonical_kinds": canonical_rows,
        "counts": {
            "canonical": len(canonical_rows),
            "classified": len(canonical_rows),
            "classifications": dict(sorted(classification_counts.items())),
            "excluded": len(exclusions),
            "implementation_statuses": dict(
                sorted(implementation_counts.items())
            ),
            "raw_classes": len(raw_records),
            "raw_files": len(raw_records),
            "unclassified": 0,
        },
        "declared_transformations": transformations,
        "exclusions": exclusions,
        "expected_path_set": {
            "construction": (
                "literal package path plus independently authored expected "
                "public symbol list; canonical rules are not consulted"
            ),
            "count": len(paths),
            "paths": sorted(paths),
            "sha256": _path_set_digest(paths),
        },
        "field_cardinality": field_cardinality,
        "generation_command": GENERATION_COMMAND,
        "identity_fields": [
            "source_revision",
            "source_blob",
            "source_path",
            "source_symbol_or_extraction_key",
            "normalized_row_key",
        ],
        "raw_records": raw_records,
        "schema_version": 1,
        "source": {
            "category_manifest_sha256": manifest_digest,
            "repository": source["repository"],
            "revision": source["revision"],
            "source_digest": (
                f"sha256:{manifest_digest};"
                f"git-tree-sha1:{source['tree_sha1']}"
            ),
            "source_packages": [
                package["path"] for package in domain["source_packages"]
            ],
            "tree_sha1": source["tree_sha1"],
        },
        "status": status,
        "table": domain_id,
        "uniform_audit": {
            "block_on_undeclared_many_to_one": True,
            "compared_fields": {
                "canonical_output": sorted(
                    field_cardinality["canonical_output"]
                ),
                "raw_source": sorted(field_cardinality["raw_source"]),
            },
            "declared_many_to_one_count": len(transformations),
            "open_items": undeclared,
            "status": "PASS" if not undeclared else status,
            "uniform_statuses": uniform_statuses,
            "undeclared_many_to_one": undeclared,
            "unexplained_field_cardinality_reductions": undeclared,
        },
    }


def build(policy: dict[str, Any] | None = None) -> dict[str, dict[str, Any]]:
    selected = load(POLICY) if policy is None else policy
    return {
        domain_id: build_domain(selected, domain_id)
        for domain_id in sorted(DOMAIN_IDS)
    }


def write(documents: dict[str, dict[str, Any]]) -> None:
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    for domain_id, document in documents.items():
        OUTPUTS[domain_id].write_text(
            stable_json(document),
            encoding="utf-8",
            newline="\n",
        )


def check(
    documents: dict[str, dict[str, Any]] | None = None,
) -> list[str]:
    selected = build() if documents is None else documents
    errors: list[str] = []
    for domain_id, document in selected.items():
        path = OUTPUTS[domain_id]
        expected = stable_json(document)
        if not path.is_file():
            errors.append(f"missing {path.relative_to(ROOT).as_posix()}")
        elif path.read_text(encoding="utf-8") != expected:
            errors.append(f"stale {path.relative_to(ROOT).as_posix()}")
    return errors


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--refresh-source-manifest", action="store_true")
    args = parser.parse_args()
    if args.check and (args.write or args.refresh_source_manifest):
        parser.error("--check cannot be combined with write/refresh")

    policy = load(POLICY)
    if args.refresh_source_manifest:
        policy = refresh_source_manifest(policy)
        POLICY.write_text(stable_json(policy), encoding="utf-8", newline="\n")
    documents = build(policy)
    if args.check:
        errors = check(documents)
        if errors:
            for error in errors:
                print(error)
            return 1
        print("T13d Cover/multiblock denominator tables are current")
        return 0
    if args.write or args.refresh_source_manifest:
        write(documents)
        for domain_id, document in documents.items():
            counts = document["counts"]
            print(
                f"{domain_id}: raw={counts['raw_classes']} "
                f"canonical={counts['canonical']} "
                f"excluded={counts['excluded']}"
            )
        return 0
    print(stable_json(documents), end="")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
