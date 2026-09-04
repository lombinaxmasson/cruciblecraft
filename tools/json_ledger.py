#!/usr/bin/env python3
"""Shared T27 helpers: stable JSON, hashes, T13 table specs, identity gates."""
from __future__ import annotations

import hashlib
import json
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
TOOLS = ROOT / "tools"

SOURCE_REVISION = "3703e40308c8c030763fd6297dea8b210d2a77b1"
DISPOSITIONS = ("v1_required", "post_1_0", "out_of_scope")
T13_CLASSIFICATIONS = (
    "in_scope",
    "deferred_with_reason",
    "out_of_scope",
)
OWNER_FORBIDDEN = ("", "todo", "deferred", "TBD", "tbd", "unassigned")
DEPENDENCY_KINDS = (
    "canonical_id",
    "open_item_id",
    "runtime_capability",
)
CLOSURE_STATUSES = ("closed", "incomplete", "not_applicable")
FIDELITY_STATUSES = ("source_backed", "source_derived", "design_policy")
LOAD_STATUSES = ("measured", "projected", "pending", "not_applicable")
PENDING_LOAD_VERDICT = "BLOCKED_PENDING_MEASUREMENT"
FAKE_ZERO_LOAD_KEYS = (
    "value",
    "eager",
    "logical",
    "lazy",
    "datapack",
    "sync_bytes",
    "reload_ms",
)

TABLE_SPECS: dict[str, dict[str, str]] = {
    "recipe_maps": {
        "artifact": "tools/machine_tree_denominators/recipe_maps.json",
        "rows_key": "rows",
        "id_field": "normalized_row_key",
        "classification_field": "classification",
    },
    "prefixes": {
        "artifact": "tools/machine_tree_denominators/prefixes.json",
        "rows_key": "records",
        "id_field": "canonical_id",
        "classification_field": "classification",
    },
    "itemgenerator_domains": {
        "artifact": "tools/machine_tree_denominators/itemgenerator_domains.json",
        "rows_key": "records",
        "id_field": "canonical_id",
        "classification_field": "classification",
    },
    "machine_kinds": {
        "artifact": "tools/machine_tree_denominators/machine_kinds.json",
        "rows_key": "canonical_kinds",
        "id_field": "canonical_key",
        "classification_field": "classification",
    },
    "energy_identities": {
        "artifact": "tools/machine_tree_denominators/energy_identities.json",
        "rows_key": "rows",
        "id_field": "symbol",
        "classification_field": "classification",
    },
    "cover_kinds": {
        "artifact": "tools/machine_tree_denominators/cover_kinds.json",
        "rows_key": "canonical_kinds",
        "id_field": "canonical_id",
        "classification_field": "disposition",
    },
    "multiblock_kinds": {
        "artifact": "tools/machine_tree_denominators/multiblock_kinds.json",
        "rows_key": "canonical_kinds",
        "id_field": "canonical_id",
        "classification_field": "disposition",
    },
}


def load_json(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def stable_json(value: Any) -> str:
    return json.dumps(value, ensure_ascii=False, indent=2, sort_keys=True) + "\n"


def sha256_bytes(payload: bytes) -> str:
    return hashlib.sha256(payload).hexdigest()


def sha256_file(path: Path) -> str:
    return sha256_bytes(path.read_bytes())


def sha256_record(value: Any) -> str:
    return sha256_bytes(stable_json(value).encode("utf-8"))


def write_stable(path: Path, document: Any) -> None:
    from tools import atomic_io

    atomic_io.write_bytes(path, stable_json(document).encode("utf-8"))


def relative(path: Path) -> str:
    try:
        return path.resolve().relative_to(ROOT.resolve()).as_posix()
    except ValueError:
        return path.as_posix()


def t13_rows(table: str, document: dict[str, Any] | None = None) -> list[dict[str, Any]]:
    spec = TABLE_SPECS[table]
    if document is None:
        document = load_json(ROOT / spec["artifact"])
    rows = document.get(spec["rows_key"]) or []
    if not isinstance(rows, list):
        raise ValueError(f"{table} rows are not a list")
    return rows


def set_equality_errors(
    expected: set[str],
    actual: set[str],
    *,
    label: str,
) -> list[str]:
    errors: list[str] = []
    missing = sorted(expected - actual)
    unexpected = sorted(actual - expected)
    if missing:
        errors.append(f"{label} missing: {missing}")
    if unexpected:
        errors.append(f"{label} unexpected: {unexpected}")
    return errors


def t13_canonical_ids(table: str, document: dict[str, Any] | None = None) -> list[str]:
    spec = TABLE_SPECS[table]
    ids = [str(row.get(spec["id_field"]) or "") for row in t13_rows(table, document)]
    if any(not item for item in ids):
        raise ValueError(f"{table} has a blank canonical id")
    if len(ids) != len(set(ids)):
        raise ValueError(f"{table} has duplicate canonical ids")
    return ids


def validate_dependency(item: Any, *, path: str) -> list[str]:
    errors: list[str] = []
    if isinstance(item, str):
        errors.append(f"{path} is free-text; use kind+id objects")
        return errors
    if not isinstance(item, dict):
        errors.append(f"{path} must be an object")
        return errors
    kind = item.get("kind")
    ident = item.get("id")
    extra = set(item) - {"kind", "id"}
    if extra:
        errors.append(f"{path} has unknown keys: {sorted(extra)}")
    if kind not in DEPENDENCY_KINDS:
        errors.append(f"{path}.kind must be one of {list(DEPENDENCY_KINDS)}")
    if not isinstance(ident, str) or not ident.strip() or ident in OWNER_FORBIDDEN:
        errors.append(f"{path}.id must be a non-empty concrete identifier")
    return errors


def validate_identity_record(record: Any) -> list[str]:
    errors: list[str] = []
    if not isinstance(record, dict):
        return ["identity record must be an object"]
    required = {
        "table",
        "canonical_id",
        "source",
        "machine_tree_classification",
        "cc_implementation",
        "disposition",
        "reason",
        "owner",
        "dependencies",
        "replacement_condition",
        "recheck_point",
        "axes",
    }
    missing = required - set(record)
    if missing:
        errors.append(f"missing keys: {sorted(missing)}")
        return errors
    if record.get("table") not in TABLE_SPECS:
        errors.append("table is not a T13 canonical table")
    ident = record.get("canonical_id")
    if not isinstance(ident, str) or not ident:
        errors.append("canonical_id must be a non-empty string")
    source = record.get("source") or {}
    if not isinstance(source, dict):
        errors.append("source must be an object")
    else:
        for key in ("revision", "artifact", "artifact_sha256", "record_sha256"):
            value = source.get(key)
            if not isinstance(value, str) or not value:
                errors.append(f"source.{key} must be a non-empty string")
        if source.get("revision") != SOURCE_REVISION:
            errors.append("source.revision is not the fixed GT6 revision")
    if record.get("machine_tree_classification") not in T13_CLASSIFICATIONS:
        errors.append("t13_classification is not a T13 vocabulary value")
    implementation = record.get("cc_implementation")
    if not isinstance(implementation, str) or not implementation:
        errors.append("cc_implementation must be a descriptor or 'none'")
    disposition = record.get("disposition")
    if disposition not in DISPOSITIONS:
        errors.append("unknown or empty disposition")
    owner = record.get("owner")
    if not isinstance(owner, str) or owner.strip() in OWNER_FORBIDDEN:
        errors.append("owner must be a unique non-empty key")
    if not isinstance(record.get("reason"), str) or not record["reason"].strip():
        errors.append("reason must be non-empty")
    if not isinstance(record.get("replacement_condition"), str) or not record[
        "replacement_condition"
    ].strip():
        errors.append("replacement_condition must be machine-verifiable text")
    if not isinstance(record.get("recheck_point"), str) or not record[
        "recheck_point"
    ].strip():
        errors.append("recheck_point must be a named milestone")
    dependencies = record.get("dependencies")
    if not isinstance(dependencies, list):
        errors.append("dependencies must be a list")
    else:
        for index, item in enumerate(dependencies):
            errors.extend(validate_dependency(item, path=f"dependencies[{index}]"))
    axes = record.get("axes") or {}
    if not isinstance(axes, dict):
        errors.append("axes must be an object")
        return errors
    closure = axes.get("closure") or {}
    fidelity = axes.get("fidelity") or {}
    load = axes.get("load") or {}
    if not isinstance(closure, dict) or closure.get("status") not in CLOSURE_STATUSES:
        errors.append("axes.closure.status is invalid")
    elif not isinstance(closure.get("evidence"), str) or not closure["evidence"]:
        errors.append("axes.closure.evidence is required")
    if not isinstance(fidelity, dict) or fidelity.get("status") not in FIDELITY_STATUSES:
        errors.append("axes.fidelity.status is invalid")
    elif not isinstance(fidelity.get("evidence"), str) or not fidelity["evidence"]:
        errors.append("axes.fidelity.evidence is required")
    if not isinstance(load, dict) or load.get("status") not in LOAD_STATUSES:
        errors.append("axes.load.status is invalid")
    else:
        if not isinstance(load.get("evidence"), str) or not load["evidence"]:
            errors.append("axes.load.evidence is required")
        if load.get("status") == "pending":
            if load.get("verdict") != PENDING_LOAD_VERDICT:
                errors.append(
                    "pending load must use BLOCKED_PENDING_MEASUREMENT"
                )
            for key in FAKE_ZERO_LOAD_KEYS:
                if load.get(key) == 0:
                    errors.append(
                        f"pending load must not fill {key}=0"
                    )
    return errors
