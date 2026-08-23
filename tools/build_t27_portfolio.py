#!/usr/bin/env python3
"""Build and check T27 per-table portfolio classification files.

Table JSON files are derived from T13 canonical rows plus a reviewed
policy. Open items and A-E tracks are separate ledgers. The aggregate
`tools/t27_portfolio.json` is a derived view. T28+ topology stays out of
scope until the aggregate passes.
"""
from __future__ import annotations

import argparse
import json
import sys
from collections import Counter
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from tools import t27_common as common  # noqa: E402

TOOLS = common.TOOLS
BUILDER = Path(__file__).resolve()
POLICY_DIR = TOOLS / "t27_portfolio_policy"
OUTPUT_DIR = TOOLS / "t27_portfolio"
ROADMAP = TOOLS / "gt6_map_roadmap.json"
KNOWN_ISSUES = TOOLS / "t26_known_issues.json"
PHASE5_CONTRACT = TOOLS / "phase5_portfolio_contract.json"
OPENING_SNAPSHOT = TOOLS / "t27_opening_snapshot.json"
T14_POLICY = TOOLS / "t14_load_budget_policy.json"
AGGREGATE = TOOLS / "t27_portfolio.json"
TRACKS_NAME = "tracks"
TRACK_IDS = ("A", "B", "C", "D", "E")
NS_PER_MS = 1_000_000.0
OPEN_ITEMS_NAME = "deferred_open_items"
OPEN_ITEM_KINDS = (
    "open_item",
    "closed_open_item",
    "canonical_coverage",
    "known_issue",
)
OPEN_ITEM_DISPOSITIONS = common.DISPOSITIONS + (
    "closed",
    "post_beta_polish",
    "non_blocking",
)
OPEN_ITEM_KEYS = (
    "id",
    "kind",
    "disposition",
    "owner",
    "cc_implementation",
    "reason",
    "replacement_condition",
    "recheck_point",
    "dependencies",
    "axes",
    "source",
    "coverage",
)
IDENTITY_KEYS = (
    "table",
    "canonical_id",
    "source",
    "t13_classification",
    "cc_implementation",
    "disposition",
    "reason",
    "owner",
    "dependencies",
    "replacement_condition",
    "recheck_point",
    "axes",
)


def _deep_merge(base: Any, override: Any) -> Any:
    if not isinstance(base, dict) or not isinstance(override, dict):
        return override
    merged = dict(base)
    for key, value in override.items():
        merged[key] = _deep_merge(base.get(key), value)
    return merged


def _roadmap_row(row: dict[str, Any], roadmap: dict[str, Any]) -> dict[str, Any]:
    source_key = row.get("name_internal")
    if source_key is None:
        source_key = ""
    return roadmap.get(source_key) or roadmap.get(row.get("normalized_row_key") or "") or {}


MACHINE_BATCH_ARTIFACTS = (
    "tools/t16_machine_denominator.json",
    "tools/t17_machine_denominator.json",
    "tools/t18_machine_energy_denominator.json",
)


def _reason(
    table: str,
    ident: str,
    t13_class: str,
    t13_row: dict[str, Any],
    roadmap_row: dict[str, Any],
    merged: dict[str, Any],
) -> str:
    if isinstance(merged.get("reason"), str) and merged["reason"].strip():
        return merged["reason"]
    status = roadmap_row.get("status") or "MISSING"
    category = roadmap_row.get("deferral_category") or "none"
    t13_reason = str(t13_row.get("reason") or t13_row.get("scope_reason") or "").strip()
    return (
        f"T13 {t13_class}; roadmap {status} ({category}). {t13_reason}".strip()
    )


def _prefix_implementation(t13_row: dict[str, Any]) -> str | None:
    mappings = t13_row.get("cc_mappings") or []
    prefixes = []
    for item in mappings:
        if not isinstance(item, dict):
            continue
        prefix = str(item.get("cc_prefix") or "").strip()
        if prefix and prefix not in prefixes:
            prefixes.append(prefix)
    if not prefixes:
        return None
    descriptors = [f"cruciblecraft:{name}" for name in prefixes]
    if len(descriptors) == 1:
        return descriptors[0]
    return "aliases:" + "|".join(descriptors)


def _load_machine_batches() -> dict[str, dict[str, Any]]:
    by_key: dict[str, dict[str, Any]] = {}
    for relative in MACHINE_BATCH_ARTIFACTS:
        document = common.load_json(ROOT / relative)
        for row in document.get("rows") or []:
            key = str(row.get("canonical_key") or "")
            if key:
                by_key[key] = row
    return by_key


def _machine_implementation(
    t13_row: dict[str, Any],
    batch_row: dict[str, Any],
    policy: dict[str, Any],
) -> str | None:
    for key in ("catalog_kind", "local_map", "local_reference"):
        value = batch_row.get(key)
        if isinstance(value, str) and value.strip():
            if batch_row.get("disposition") in {
                "MAPPED_DEFERRED",
            }:
                return f"related_host:{value}"
            return value
    behavior = str(t13_row.get("behavior_class") or "")
    recipe_map = str(t13_row.get("recipe_map") or "")
    behavior_impl = (policy.get("behavior_implementations") or {}).get(behavior)
    if isinstance(behavior_impl, str) and behavior_impl.strip():
        return behavior_impl
    recipe_impl = (policy.get("recipe_map_implementations") or {}).get(recipe_map)
    if isinstance(recipe_impl, str) and recipe_impl.strip():
        return recipe_impl
    return None


def build_table(table: str) -> dict[str, Any]:
    if table not in common.TABLE_SPECS:
        raise ValueError(f"unknown table: {table}")
    policy_path = POLICY_DIR / f"{table}.json"
    if not policy_path.is_file():
        raise ValueError(f"missing table policy: {common.relative(policy_path)}")
    policy = common.load_json(policy_path)
    spec = common.TABLE_SPECS[table]
    artifact = ROOT / spec["artifact"]
    t13_document = common.load_json(artifact)
    artifact_sha = common.sha256_file(artifact)
    roadmap = {}
    if table == "recipe_maps" and ROADMAP.is_file():
        roadmap = (common.load_json(ROADMAP).get("maps") or {})
    machine_batches = _load_machine_batches() if table == "machine_kinds" else {}
    defaults = policy.get("defaults") or {}
    overrides = policy.get("overrides") or {}
    implementations = policy.get("cc_implementations") or {}
    batch_disposition_map = policy.get("batch_disposition_map") or {}
    recipe_map_overrides = policy.get("recipe_map_overrides") or {}
    behavior_overrides = policy.get("behavior_overrides") or {}
    records: list[dict[str, Any]] = []
    extra_inputs: dict[str, str] = {}
    if table == "machine_kinds":
        for relative in MACHINE_BATCH_ARTIFACTS:
            extra_inputs[relative] = common.sha256_file(ROOT / relative)
    for t13_row in common.t13_rows(table, t13_document):
        ident = str(t13_row.get(spec["id_field"]) or "")
        t13_class = str(t13_row.get(spec["classification_field"]) or "")
        if t13_class not in defaults:
            raise ValueError(f"{table}:{ident} has no default for {t13_class}")
        merged = dict(defaults[t13_class])
        if table == "machine_kinds":
            batch_row = machine_batches.get(ident) or {}
            batch_disposition = str(batch_row.get("disposition") or "")
            if batch_disposition:
                mapped = batch_disposition_map.get(batch_disposition)
                if not isinstance(mapped, dict):
                    raise ValueError(
                        f"{ident} batch disposition {batch_disposition} has no T27 map"
                    )
                merged = _deep_merge(merged, mapped)
            recipe_map = str(t13_row.get("recipe_map") or "")
            behavior = str(t13_row.get("behavior_class") or "")
            merged = _deep_merge(merged, recipe_map_overrides.get(recipe_map) or {})
            merged = _deep_merge(merged, behavior_overrides.get(behavior) or {})
        else:
            batch_row = {}
        merged = _deep_merge(merged, overrides.get(ident) or {})
        roadmap_row = _roadmap_row(t13_row, roadmap) if table == "recipe_maps" else {}
        implementation = implementations.get(ident)
        if implementation is None and table == "prefixes":
            implementation = _prefix_implementation(t13_row)
        if implementation is None and table == "machine_kinds":
            implementation = _machine_implementation(t13_row, batch_row, policy)
        if implementation is None:
            implementation = merged.get("cc_implementation", "none")
        if table == "recipe_maps" and t13_class == "in_scope" and implementation == "none":
            raise ValueError(f"{ident} is in_scope but has no CC implementation mapping")
        if table == "prefixes" and t13_class == "in_scope" and implementation == "none":
            raise ValueError(f"{ident} is in_scope but has no CC prefix mapping")
        axes = merged.get("axes") or {}
        record = {
            "table": table,
            "canonical_id": ident,
            "source": {
                "revision": common.SOURCE_REVISION,
                "artifact": spec["artifact"],
                "artifact_sha256": artifact_sha,
                "record_sha256": common.sha256_record(t13_row),
            },
            "t13_classification": t13_class,
            "cc_implementation": implementation,
            "disposition": merged.get("disposition"),
            "reason": _reason(table, ident, t13_class, t13_row, roadmap_row, merged),
            "owner": merged.get("owner"),
            "dependencies": merged.get("dependencies") or [],
            "replacement_condition": merged.get("replacement_condition"),
            "recheck_point": merged.get("recheck_point"),
            "axes": {
                "closure": dict(axes.get("closure") or {}),
                "fidelity": dict(axes.get("fidelity") or {}),
                "load": dict(axes.get("load") or {}),
            },
        }
        extra = set(record) - set(IDENTITY_KEYS)
        if extra:
            raise ValueError(f"{table}:{ident} extra keys {sorted(extra)}")
        errors = common.validate_identity_record(record)
        if errors:
            raise ValueError(f"{table}:{ident}: {'; '.join(errors)}")
        records.append(record)
    records.sort(key=lambda item: item["canonical_id"])
    expected = set(common.t13_canonical_ids(table, t13_document))
    actual = {item["canonical_id"] for item in records}
    equality = common.set_equality_errors(expected, actual, label=table)
    if equality:
        raise ValueError("; ".join(equality))
    counts = Counter(item["disposition"] for item in records)
    unclassified = sum(1 for item in records if item["disposition"] not in common.DISPOSITIONS)
    if unclassified:
        raise ValueError(f"{table} unclassified={unclassified}")
    owned_inputs = {
        common.relative(BUILDER): common.sha256_file(BUILDER),
        common.relative(policy_path): common.sha256_file(policy_path),
        spec["artifact"]: artifact_sha,
    }
    owned_inputs.update(extra_inputs)
    return {
        "schema_version": 1,
        "status": "T27_TABLE_CLASSIFIED",
        "table": table,
        "source_revision": common.SOURCE_REVISION,
        "generated_by": "python tools/build_t27_portfolio.py",
        "currentness": {
            "owned_inputs": owned_inputs,
        },
        "counts": {
            "canonical": len(records),
            "v1_required": counts.get("v1_required", 0),
            "post_1_0": counts.get("post_1_0", 0),
            "out_of_scope": counts.get("out_of_scope", 0),
            "unclassified": 0,
            "missing": 0,
            "unexpected": 0,
        },
        "records": records,
    }


def _validate_open_item(record: dict[str, Any]) -> list[str]:
    errors: list[str] = []
    extra = set(record) - set(OPEN_ITEM_KEYS)
    if extra:
        errors.append(f"{record.get('id')}: extra keys {sorted(extra)}")
    ident = record.get("id")
    if not isinstance(ident, str) or not ident.strip():
        errors.append("open item id must be non-empty")
    if record.get("kind") not in OPEN_ITEM_KINDS:
        errors.append(f"{ident}: unknown kind")
    if record.get("disposition") not in OPEN_ITEM_DISPOSITIONS:
        errors.append(f"{ident}: unknown disposition")
    owner = record.get("owner")
    if not isinstance(owner, str) or owner.strip() in common.OWNER_FORBIDDEN:
        errors.append(f"{ident}: owner must be a unique non-empty key")
    for field in (
        "cc_implementation",
        "reason",
        "replacement_condition",
        "recheck_point",
    ):
        if not isinstance(record.get(field), str) or not record[field].strip():
            errors.append(f"{ident}: {field} is required")
    dependencies = record.get("dependencies")
    if not isinstance(dependencies, list):
        errors.append(f"{ident}: dependencies must be a list")
    else:
        for index, item in enumerate(dependencies):
            errors.extend(
                common.validate_dependency(item, path=f"{ident}.dependencies[{index}]")
            )
    axes = record.get("axes") or {}
    fake = {
        "table": "recipe_maps",
        "canonical_id": ident or "open-item",
        "source": {
            "revision": common.SOURCE_REVISION,
            "artifact": "tools/t26_known_issues.json",
            "artifact_sha256": "0" * 64,
            "record_sha256": "0" * 64,
        },
        "t13_classification": "deferred_with_reason",
        "cc_implementation": record.get("cc_implementation") or "none",
        "disposition": "post_1_0"
        if record.get("disposition") not in common.DISPOSITIONS
        else record.get("disposition"),
        "reason": record.get("reason") or "open item",
        "owner": owner or "portfolio:open",
        "dependencies": dependencies if isinstance(dependencies, list) else [],
        "replacement_condition": record.get("replacement_condition") or "pending",
        "recheck_point": record.get("recheck_point") or "pending",
        "axes": axes,
    }
    errors.extend(
        error
        for error in common.validate_identity_record(fake)
        if error.startswith("axes.")
        or "pending load" in error
        or error.startswith("pending load")
    )
    if record.get("kind") == "canonical_coverage":
        coverage = record.get("coverage") or {}
        if coverage.get("table") not in common.TABLE_SPECS:
            errors.append(f"{ident}: coverage.table is required")
        if not isinstance(coverage.get("canonical_id"), str) or not coverage["canonical_id"]:
            errors.append(f"{ident}: coverage.canonical_id is required")
    elif record.get("coverage") not in (None, {}):
        errors.append(f"{ident}: coverage is only valid on canonical_coverage")
    return errors


def _table_open_item_ids() -> set[str]:
    found: set[str] = set()
    for table in common.TABLE_SPECS:
        path = output_path(table)
        if not path.is_file():
            continue
        document = common.load_json(path)
        for row in document.get("records") or []:
            for item in row.get("dependencies") or []:
                if isinstance(item, dict) and item.get("kind") == "open_item_id":
                    found.add(str(item.get("id") or ""))
    found.discard("")
    return found


def _superseded_known_issue_ids(policy: dict[str, Any]) -> set[str]:
    superseded: set[str] = set()
    for item in policy.get("records") or []:
        if item.get("kind") != "closed_open_item":
            continue
        source = item.get("source") or {}
        if source.get("artifact") != "tools/t26_known_issues.json":
            continue
        ident = str(item.get("id") or "")
        if ident:
            superseded.add(ident)
    return superseded


def build_open_items() -> dict[str, Any]:
    policy_path = POLICY_DIR / f"{OPEN_ITEMS_NAME}.json"
    if not policy_path.is_file():
        raise ValueError(f"missing open-item policy: {common.relative(policy_path)}")
    policy = common.load_json(policy_path)
    known = common.load_json(KNOWN_ISSUES)
    phase5 = common.load_json(PHASE5_CONTRACT)
    superseded_known_issues = _superseded_known_issue_ids(policy)
    records: list[dict[str, Any]] = []
    for item in policy.get("records") or []:
        record = {
            "id": item["id"],
            "kind": item["kind"],
            "disposition": item["disposition"],
            "owner": item["owner"],
            "cc_implementation": item.get("cc_implementation", "none"),
            "reason": item["reason"],
            "replacement_condition": item["replacement_condition"],
            "recheck_point": item["recheck_point"],
            "dependencies": item.get("dependencies") or [],
            "axes": {
                "closure": dict((item.get("axes") or {}).get("closure") or {}),
                "fidelity": dict((item.get("axes") or {}).get("fidelity") or {}),
                "load": dict((item.get("axes") or {}).get("load") or {}),
            },
            "source": dict(item.get("source") or {}),
            "coverage": dict(item.get("coverage") or {}),
        }
        if record["kind"] != "canonical_coverage":
            record["coverage"] = {}
        errors = _validate_open_item(record)
        if errors:
            raise ValueError("; ".join(errors))
        records.append(record)
    defaults = policy.get("known_issue_defaults") or {}
    overrides = policy.get("known_issue_overrides") or {}
    for issue in known.get("issues") or []:
        ident = str(issue.get("id") or "")
        if ident in superseded_known_issues:
            continue
        disposition = str(issue.get("disposition") or "")
        if disposition not in defaults:
            raise ValueError(f"{ident} has no known-issue default for {disposition}")
        merged = _deep_merge(defaults[disposition], overrides.get(ident) or {})
        contract = issue.get("recheck_contract") or {}
        replacement = (
            merged.get("replacement_condition")
            or contract.get("replacement_condition")
            or issue.get("workaround")
        )
        recheck = (
            merged.get("recheck_point")
            or contract.get("recheck_point")
            or "T27 freeze recheck"
        )
        if ident in {"T24-F003", "T24-F005"}:
            expected = contract.get("replacement_condition")
            if replacement != expected:
                raise ValueError(
                    f"{ident} replacement_condition must copy t26 verbatim"
                )
            if recheck != contract.get("recheck_point"):
                raise ValueError(f"{ident} recheck_point must copy t26 verbatim")
        record = {
            "id": ident,
            "kind": "known_issue",
            "disposition": disposition,
            "owner": str(issue.get("owner") or merged.get("owner")),
            "cc_implementation": merged.get("cc_implementation", "none"),
            "reason": str(issue.get("title") or merged.get("reason") or ident),
            "replacement_condition": replacement,
            "recheck_point": recheck,
            "dependencies": merged.get("dependencies") or [],
            "axes": {
                "closure": dict((merged.get("axes") or {}).get("closure") or {}),
                "fidelity": dict((merged.get("axes") or {}).get("fidelity") or {}),
                "load": dict((merged.get("axes") or {}).get("load") or {}),
            },
            "source": {
                "artifact": "tools/t26_known_issues.json",
                "issue_id": ident,
            },
            "coverage": {},
        }
        errors = _validate_open_item(record)
        if errors:
            raise ValueError("; ".join(errors))
        records.append(record)
    records.sort(key=lambda item: item["id"])
    ids = [item["id"] for item in records]
    if len(ids) != len(set(ids)):
        raise ValueError("duplicate open-item ids")
    coverage_errors: list[str] = []
    by_id = {item["id"]: item for item in records}
    for item in records:
        if item["kind"] != "canonical_coverage":
            continue
        table = item["coverage"]["table"]
        canonical_id = item["coverage"]["canonical_id"]
        table_document = common.load_json(output_path(table))
        table_row = next(
            (
                row
                for row in table_document.get("records") or []
                if row.get("canonical_id") == canonical_id
            ),
            None,
        )
        if table_row is None:
            coverage_errors.append(f"{item['id']} missing canonical {table}:{canonical_id}")
        elif table_row.get("owner") != item["owner"]:
            coverage_errors.append(
                f"{item['id']} owner {item['owner']} != table owner {table_row.get('owner')}"
            )
    frozen = (phase5.get("frozen_open_items") or {})
    missing_frozen = sorted(set(frozen) - set(by_id))
    if missing_frozen:
        coverage_errors.append(f"phase5 frozen_open_items missing: {missing_frozen}")
    referenced = _table_open_item_ids()
    missing_refs = sorted(referenced - set(by_id))
    if missing_refs:
        coverage_errors.append(f"orphan open item dependencies: {missing_refs}")
    known_ids = {str(issue.get("id") or "") for issue in known.get("issues") or []}
    known_ids -= superseded_known_issues
    ledger_known = {item["id"] for item in records if item["kind"] == "known_issue"}
    coverage_errors.extend(
        common.set_equality_errors(known_ids, ledger_known, label="known_issues")
    )
    if coverage_errors:
        raise ValueError("; ".join(coverage_errors))
    counts = Counter(item["kind"] for item in records)
    return {
        "schema_version": 1,
        "status": "T27_OPEN_ITEMS_CLASSIFIED",
        "source_revision": common.SOURCE_REVISION,
        "generated_by": "python tools/build_t27_portfolio.py --write-open-items",
        "currentness": {
            "owned_inputs": {
                common.relative(BUILDER): common.sha256_file(BUILDER),
                common.relative(policy_path): common.sha256_file(policy_path),
                "tools/t26_known_issues.json": common.sha256_file(KNOWN_ISSUES),
                "tools/phase5_portfolio_contract.json": common.sha256_file(
                    PHASE5_CONTRACT
                ),
            }
        },
        "counts": {
            "records": len(records),
            "known_issues": counts.get("known_issue", 0),
            "orphan": 0,
            "duplicate_ownership": 0,
            "unclassified": 0,
        },
        "records": records,
    }


def open_items_path() -> Path:
    return OUTPUT_DIR / f"{OPEN_ITEMS_NAME}.json"


def write_open_items() -> dict[str, Any]:
    document = build_open_items()
    common.write_stable(open_items_path(), document)
    return document


def check_open_items() -> list[str]:
    path = open_items_path()
    if not path.is_file():
        return [f"missing generated file: {common.relative(path)}"]
    expected = common.stable_json(build_open_items())
    actual = path.read_text(encoding="utf-8")
    if actual != expected:
        return [f"{common.relative(path)} is stale"]
    return []


def _track_for_owner(owner: str, prefixes: dict[str, tuple[str, ...]]) -> str | None:
    matches = [
        track
        for track, items in prefixes.items()
        if any(owner.startswith(prefix) for prefix in items)
    ]
    if len(matches) > 1:
        raise ValueError(f"owner {owner} matches tracks {matches}")
    return matches[0] if matches else None


def _pending_axis(spec: dict[str, Any], *, base: Any, method: str) -> dict[str, Any]:
    return {
        "base": base,
        "delta": None,
        "hard_ceiling": spec["hard_ceiling"],
        "lower_bound": None,
        "method": method,
        "soft_budget": spec["soft_budget"],
        "unit": spec.get("unit"),
        "upper_bound": None,
        "verdict": common.PENDING_LOAD_VERDICT,
    }


def _measured_axis(
    spec: dict[str, Any],
    *,
    actual: int | float,
    method: str,
    base: Any | None = None,
) -> dict[str, Any]:
    soft = spec["soft_budget"]
    hard = spec["hard_ceiling"]
    if actual > hard:
        raise ValueError(
            f"{spec.get('unit')} actual {actual} exceeds hard ceiling {hard}"
        )
    verdict = "SOFT_BUDGET_EXCEEDED" if actual > soft else "PASS"
    return {
        "actual": actual,
        "base": actual if base is None else base,
        "delta": 0,
        "hard_ceiling": hard,
        "method": method,
        "projected": actual,
        "soft_budget": soft,
        "unit": spec.get("unit"),
        "verdict": verdict,
    }


def _validate_projection_axes(axes: dict[str, Any], budgets: dict[str, Any]) -> None:
    missing = sorted(set(budgets) - set(axes))
    extra = sorted(set(axes) - set(budgets))
    if missing or extra:
        raise ValueError(f"load axes mismatch missing={missing} extra={extra}")
    for name, axis in axes.items():
        if axis.get("verdict") == common.PENDING_LOAD_VERDICT:
            for key in common.FAKE_ZERO_LOAD_KEYS:
                if axis.get(key) == 0:
                    raise ValueError(f"{name} pending load must not fill {key}=0")
            if axis.get("upper_bound") == 0 or axis.get("delta") == 0:
                raise ValueError(f"{name} pending load must not fill bound/delta=0")
            continue
        upper = axis.get("upper_bound")
        projected = axis.get("projected", axis.get("actual"))
        hard = budgets[name]["hard_ceiling"]
        for value in (upper, projected, axis.get("actual")):
            if isinstance(value, (int, float)) and value > hard:
                raise ValueError(
                    f"{name} upper/projected {value} exceeds hard ceiling {hard}"
                )


def _identity_ref(table: str, row: dict[str, Any]) -> dict[str, str]:
    return {
        "canonical_id": row["canonical_id"],
        "owner": row["owner"],
        "table": table,
    }


def _collect_portfolio_rows() -> list[tuple[str, dict[str, Any]]]:
    rows: list[tuple[str, dict[str, Any]]] = []
    for table in common.TABLE_SPECS:
        document = common.load_json(output_path(table))
        for row in document.get("records") or []:
            rows.append((table, row))
    return rows


def build_tracks() -> dict[str, Any]:
    missing_tables = check_all_tables()
    if missing_tables:
        raise ValueError("; ".join(missing_tables))
    open_errors = check_open_items()
    if open_errors:
        raise ValueError("; ".join(open_errors))
    policy_path = POLICY_DIR / f"{TRACKS_NAME}.json"
    policy = common.load_json(policy_path)
    opening = common.load_json(OPENING_SNAPSHOT)
    t14 = common.load_json(T14_POLICY)
    budgets = t14.get("budgets") or {}
    open_items = common.load_json(open_items_path())
    roadmap = (common.load_json(ROADMAP).get("maps") or {}) if ROADMAP.is_file() else {}
    prefixes = {
        track: tuple((spec.get("owner_prefixes") or []))
        for track, spec in (policy.get("tracks") or {}).items()
    }
    primary: dict[str, list[dict[str, str]]] = {track: [] for track in TRACK_IDS}
    references: dict[str, list[dict[str, str]]] = {track: [] for track in TRACK_IDS}
    seen_primary: dict[tuple[str, str], str] = {}
    for table, row in _collect_portfolio_rows():
        owner = str(row.get("owner") or "")
        track = _track_for_owner(owner, prefixes)
        ref = _identity_ref(table, row)
        key = (table, row["canonical_id"])
        if track:
            if key in seen_primary and seen_primary[key] != track:
                raise ValueError(f"{key} owned by tracks {seen_primary[key]} and {track}")
            seen_primary[key] = track
            primary[track].append(ref)
        for track_id, spec in (policy.get("tracks") or {}).items():
            if owner in (spec.get("reference_owners") or []):
                references[track_id].append(ref)
    for row in open_items.get("records") or []:
        owner = str(row.get("owner") or "")
        track = _track_for_owner(owner, prefixes)
        if not track:
            continue
        ref = {
            "canonical_id": row["id"],
            "owner": owner,
            "table": "deferred_open_items",
        }
        key = ("deferred_open_items", row["id"])
        if key in seen_primary and seen_primary[key] != track:
            raise ValueError(f"{key} owned by tracks {seen_primary[key]} and {track}")
        seen_primary[key] = track
        primary[track].append(ref)
    for track in TRACK_IDS:
        primary[track].sort(key=lambda item: (item["table"], item["canonical_id"]))
        references[track].sort(key=lambda item: (item["table"], item["canonical_id"]))
        spec = (policy.get("tracks") or {}).get(track) or {}
        if not primary[track] and not spec.get("allow_empty_primary"):
            raise ValueError(f"track {track} has no primary identities")
    bounded = sorted(
        ident
        for ident, row in roadmap.items()
        if isinstance(row, dict) and row.get("status") == "BOUNDED_SUBSET_PORTED"
    )
    publication = (opening.get("publication") or {}).get("current") or {}
    datapack = ((opening.get("datapack") or {}).get("t14_authored_files") or {}).get(
        "value"
    )
    measured_1x = (opening.get("measurements") or {}).get("selected_1x") or {}
    ordinary = ((opening.get("t22_5") or {}).get("ordinary_optional"))
    freeze_method = "T27 freeze uses opening snapshot actuals; publication delta is 0/0/0."
    freeze_axes = {
        "eager_publication_rows": _measured_axis(
            budgets["eager_publication_rows"],
            actual=int(publication["eager"]),
            method=freeze_method,
        ),
        "lazy_logical_rows": _measured_axis(
            budgets["lazy_logical_rows"],
            actual=int(publication["lazy"]),
            method=freeze_method,
        ),
        "datapack_authored_entries": _measured_axis(
            budgets["datapack_authored_entries"],
            actual=int(datapack),
            method=freeze_method,
        ),
        "sync_bytes": _measured_axis(
            budgets["sync_bytes"],
            actual=int(measured_1x["sync_bytes"]),
            method="opening.measurements.selected_1x.sync_bytes",
        ),
        "server_reload_ms": _measured_axis(
            budgets["server_reload_ms"],
            actual=measured_1x["server_reload_p95_ns"] / NS_PER_MS,
            method="opening.measurements.selected_1x.server_reload_p95_ns / 1e6",
        ),
        "server_index_ms": _measured_axis(
            budgets["server_index_ms"],
            actual=measured_1x["server_index_p95_ns"] / NS_PER_MS,
            method="opening.measurements.selected_1x.server_index_p95_ns / 1e6",
        ),
        "client_reload_ms": _measured_axis(
            budgets["client_reload_ms"],
            actual=measured_1x["dedicated_client_reexpansion_p95_ns"] / NS_PER_MS,
            method="opening.measurements.selected_1x.dedicated_client_reexpansion_p95_ns / 1e6",
        ),
        "client_index_ms": _measured_axis(
            budgets["client_index_ms"],
            actual=measured_1x["dedicated_client_index_p95_ns"] / NS_PER_MS,
            method="opening.measurements.selected_1x.dedicated_client_index_p95_ns / 1e6",
        ),
        "retained_memory_bytes": _measured_axis(
            budgets["retained_memory_bytes"],
            actual=int(measured_1x["retained_total_bytes_p50"]),
            method="opening.measurements.selected_1x.retained_total_bytes_p50",
        ),
        "allocation_bytes": _measured_axis(
            budgets["allocation_bytes"],
            actual=int(measured_1x["jfr_allocation_bytes_p50"]),
            method="opening.measurements.selected_1x.jfr_allocation_bytes_p50",
        ),
        "lookup_p95_ns": _measured_axis(
            budgets["lookup_p95_ns"],
            actual=int(measured_1x["lookup_p95_ns"]),
            method="opening.measurements.selected_1x.lookup_p95_ns",
        ),
        "lookup_candidate_count": _measured_axis(
            budgets["lookup_candidate_count"],
            actual=int(measured_1x["lookup_candidates_p95"]),
            method="opening.measurements.selected_1x.lookup_candidates_p95",
        ),
        "lazy_cache_ceiling_rows": _pending_axis(
            budgets["lazy_cache_ceiling_rows"],
            base=None,
            method="Opening snapshot does not copy T14 lazy_cache_ceiling; T27 does not fill 0.",
        ),
    }
    _validate_projection_axes(freeze_axes, budgets)
    initial_cards: dict[str, Any] = {}
    for track, spec in (policy.get("tracks") or {}).items():
        card = spec["initial_card"]
        method = str(spec.get("method") or "")
        if "Extruder" in method and track != "A":
            raise ValueError("T14 Extruder ratio leaked onto a non-A card")
        axes = {
            name: _pending_axis(
                budget,
                base=freeze_axes[name].get("actual", freeze_axes[name].get("base")),
                method=method,
            )
            for name, budget in budgets.items()
        }
        _validate_projection_axes(axes, budgets)
        initial_cards[card] = {
            "axes": axes,
            "kind": "measurement_card",
            "method": method,
            "opening_sha256": common.sha256_file(OPENING_SNAPSHOT),
            "opening_snapshot": common.relative(OPENING_SNAPSHOT),
            "publication_delta": {
                "eager": None,
                "lazy": None,
                "logical": None,
            },
            "started": False,
            "t14_policy": common.relative(T14_POLICY),
            "t14_policy_sha256": common.sha256_file(T14_POLICY),
            "track": track,
        }
    initial_cards["A0"]["candidate"] = {
        "bounded_subset_ported_maps": bounded,
        "host_maps": [item["canonical_id"] for item in references["A"]],
        "source_rows": ordinary,
        "source_rows_are_not_publication": True,
    }
    tracks_out: dict[str, Any] = {}
    for track in TRACK_IDS:
        spec = (policy.get("tracks") or {}).get(track) or {}
        tracks_out[track] = {
            "gt6u_revision": spec.get("gt6u_revision", None) if track == "D" else None,
            "initial_card": spec.get("initial_card"),
            "method": spec.get("method"),
            "primary_identities": primary[track],
            "primary_identity_count": len(primary[track]),
            "reference_identities": references[track],
            "scope": spec.get("scope"),
            "started": False,
        }
    if tracks_out["C"]["primary_identity_count"] < 1:
        raise ValueError("track C lost its unique nuclear/fusion owner set")
    fusion = {
        item["canonical_id"]
        for item in primary["C"]
        if item["table"] == "recipe_maps"
    }
    if "gt.recipe.fusionreactor" not in fusion or "gt.recipe.fuels.plasma" not in fusion:
        raise ValueError("fusion maps must be uniquely owned by Track C")
    if any(
        item["canonical_id"] in fusion for item in primary["E"]
    ):
        raise ValueError("Track E must not own Track C fusion maps")
    document = {
        "schema_version": 1,
        "status": "T27_TRACKS_CLASSIFIED",
        "source_revision": common.SOURCE_REVISION,
        "generated_by": "python tools/build_t27_portfolio.py --write-tracks",
        "currentness": {
            "owned_inputs": {
                common.relative(BUILDER): common.sha256_file(BUILDER),
                common.relative(policy_path): common.sha256_file(policy_path),
                common.relative(OPENING_SNAPSHOT): common.sha256_file(OPENING_SNAPSHOT),
                common.relative(T14_POLICY): common.sha256_file(T14_POLICY),
                common.relative(open_items_path()): common.sha256_file(open_items_path()),
                **{
                    common.relative(output_path(table)): common.sha256_file(
                        output_path(table)
                    )
                    for table in common.TABLE_SPECS
                },
            }
        },
        "publication_delta": {"eager": 0, "lazy": 0, "logical": 0},
        "started": False,
        "eager_hard_ceiling_is_21000": True,
        "logical_21000_is_not_authoritative": True,
        "t27_card": {
            "axes": freeze_axes,
            "kind": "portfolio_freeze",
            "method": freeze_method,
            "opening_sha256": common.sha256_file(OPENING_SNAPSHOT),
            "opening_snapshot": common.relative(OPENING_SNAPSHOT),
            "publication": publication,
            "publication_delta": {"eager": 0, "lazy": 0, "logical": 0},
            "t14_policy": common.relative(T14_POLICY),
            "t14_policy_sha256": common.sha256_file(T14_POLICY),
        },
        "initial_cards": initial_cards,
        "tracks": tracks_out,
        "counts": {
            "primary_identities": {
                track: len(primary[track]) for track in TRACK_IDS
            },
            "started_tracks": 0,
            "unclassified_tracks": 0,
        },
    }
    return document


def tracks_path() -> Path:
    return OUTPUT_DIR / f"{TRACKS_NAME}.json"


def write_tracks() -> dict[str, Any]:
    document = build_tracks()
    common.write_stable(tracks_path(), document)
    return document


def check_tracks() -> list[str]:
    path = tracks_path()
    if not path.is_file():
        return [f"missing generated file: {common.relative(path)}"]
    expected = common.stable_json(build_tracks())
    actual = path.read_text(encoding="utf-8")
    if actual != expected:
        return [f"{common.relative(path)} is stale"]
    document = common.load_json(path)
    errors: list[str] = []
    if document.get("started"):
        errors.append("tracks must not be started in T27")
    if document.get("publication_delta") != {"eager": 0, "lazy": 0, "logical": 0}:
        errors.append("T27 publication delta must remain 0/0/0")
    tracks = document.get("tracks") or {}
    if sorted(tracks) != list(TRACK_IDS):
        errors.append(f"tracks must be A-E, got {sorted(tracks)}")
    owners: dict[tuple[str, str], str] = {}
    for track, spec in tracks.items():
        if spec.get("started"):
            errors.append(f"track {track} started")
        for item in spec.get("primary_identities") or []:
            key = (item.get("table"), item.get("canonical_id"))
            if key in owners and owners[key] != track:
                errors.append(f"{key} has duplicate track owners")
            owners[key] = track
        if track == "D" and spec.get("gt6u_revision") not in (None,):
            errors.append("Track D must not pin a GT6U revision in T27")
    return errors


def check_load_projections() -> list[str]:
    errors = check_tracks()
    if errors:
        return errors
    document = common.load_json(tracks_path())
    t14 = common.load_json(T14_POLICY)
    budgets = t14.get("budgets") or {}
    opening_sha = common.sha256_file(OPENING_SNAPSHOT)
    policy_sha = common.sha256_file(T14_POLICY)
    try:
        _validate_projection_axes((document.get("t27_card") or {}).get("axes") or {}, budgets)
        freeze = document.get("t27_card") or {}
        if freeze.get("opening_sha256") != opening_sha:
            errors.append("T27 card opening hash is stale")
        if freeze.get("t14_policy_sha256") != policy_sha:
            errors.append("T27 card T14 policy hash is stale")
        eager = ((freeze.get("axes") or {}).get("eager_publication_rows") or {})
        if eager.get("hard_ceiling") != 21000:
            errors.append("eager hard ceiling must be 21000")
        if freeze.get("publication_delta") != {"eager": 0, "lazy": 0, "logical": 0}:
            errors.append("T27 card publication delta must remain 0/0/0")
        for card, spec in (document.get("initial_cards") or {}).items():
            _validate_projection_axes(spec.get("axes") or {}, budgets)
            if spec.get("started"):
                errors.append(f"{card} started")
            if spec.get("opening_sha256") != opening_sha:
                errors.append(f"{card} opening hash is stale")
            if spec.get("t14_policy_sha256") != policy_sha:
                errors.append(f"{card} T14 policy hash is stale")
            if spec.get("publication_delta") != {
                "eager": None,
                "lazy": None,
                "logical": None,
            }:
                errors.append(f"{card} must not project a publication delta")
    except ValueError as error:
        errors.append(str(error))
    return errors


def policy_tables() -> list[str]:
    names = []
    for spec_name in common.TABLE_SPECS:
        if (POLICY_DIR / f"{spec_name}.json").is_file():
            names.append(spec_name)
    return names


def output_path(table: str) -> Path:
    return OUTPUT_DIR / f"{table}.json"


def check_table(table: str) -> list[str]:
    path = output_path(table)
    if not path.is_file():
        return [f"missing generated file: {common.relative(path)}"]
    expected = common.stable_json(build_table(table))
    actual = path.read_text(encoding="utf-8")
    if actual != expected:
        return [f"{common.relative(path)} is stale"]
    return []


def check_all_tables() -> list[str]:
    errors: list[str] = []
    missing_policy = [name for name in common.TABLE_SPECS if name not in policy_tables()]
    if missing_policy:
        errors.append(f"tables without policy: {missing_policy}")
    for table in policy_tables():
        errors.extend(check_table(table))
    if not missing_policy:
        total = 0
        unclassified = 0
        for table in common.TABLE_SPECS:
            document = common.load_json(output_path(table))
            total += int((document.get("counts") or {}).get("canonical") or 0)
            unclassified += int((document.get("counts") or {}).get("unclassified") or 0)
        if total != 765:
            errors.append(f"canonical identities={total}, expected 765")
        if unclassified:
            errors.append(f"unclassified={unclassified}")
    return errors


def _require_frozen_inputs() -> None:
    errors = check_all_tables()
    errors.extend(check_open_items())
    errors.extend(check_tracks())
    errors.extend(check_load_projections())
    if errors:
        raise ValueError("; ".join(errors))


def _pointer(table: str, ident: str) -> dict[str, str]:
    return {"canonical_id": ident, "table": table}


def _node_id(table: str, ident: str) -> str:
    return f"{table}/{ident}"


def _topo_layers(nodes: set[str], edges: list[tuple[str, str]]) -> list[list[str]]:
    incoming: dict[str, int] = {node: 0 for node in nodes}
    outgoing: dict[str, list[str]] = {node: [] for node in nodes}
    for dependent, dependency in edges:
        if dependent not in nodes or dependency not in nodes:
            continue
        outgoing[dependency].append(dependent)
        incoming[dependent] += 1
    ready = sorted(node for node, count in incoming.items() if count == 0)
    layers: list[list[str]] = []
    seen = 0
    while ready:
        layer = list(ready)
        layers.append(layer)
        ready = []
        for node in layer:
            seen += 1
            for dest in outgoing[node]:
                incoming[dest] -= 1
                if incoming[dest] == 0:
                    ready.append(dest)
        ready.sort()
    if seen != len(nodes):
        raise ValueError("dependency graph has a cycle")
    return layers


def build_aggregate() -> dict[str, Any]:
    _require_frozen_inputs()
    contract = common.load_json(PHASE5_CONTRACT)
    opening = common.load_json(OPENING_SNAPSHOT)
    tracks = common.load_json(tracks_path())
    open_items = common.load_json(open_items_path())
    table_docs = {
        table: common.load_json(output_path(table)) for table in common.TABLE_SPECS
    }
    identities: list[dict[str, Any]] = []
    by_id: dict[str, list[tuple[str, dict[str, Any]]]] = {}
    disposition_sets: dict[str, list[dict[str, str]]] = {
        name: [] for name in common.DISPOSITIONS
    }
    implementation_none: list[dict[str, str]] = []
    implementation_mapped: list[dict[str, str]] = []
    owner_index: dict[str, list[dict[str, str]]] = {}
    closure_counts: Counter[str] = Counter()
    fidelity_counts: Counter[str] = Counter()
    load_counts: Counter[str] = Counter()
    table_counts: dict[str, Any] = {}
    nodes: set[str] = set()
    edges: list[tuple[str, str]] = []
    broken: list[str] = []
    owner_violations: list[str] = []
    for table, document in table_docs.items():
        counts = document.get("counts") or {}
        table_counts[table] = {
            "canonical": counts.get("canonical"),
            "out_of_scope": counts.get("out_of_scope"),
            "post_1_0": counts.get("post_1_0"),
            "source_artifact": common.relative(output_path(table)),
            "t13_artifact": common.TABLE_SPECS[table]["artifact"],
            "v1_required": counts.get("v1_required"),
        }
        for row in document.get("records") or []:
            ident = row["canonical_id"]
            pointer = _pointer(table, ident)
            identities.append(
                {
                    "canonical_id": ident,
                    "cc_implementation": row["cc_implementation"],
                    "closure": row["axes"]["closure"]["status"],
                    "disposition": row["disposition"],
                    "fidelity": row["axes"]["fidelity"]["status"],
                    "load": row["axes"]["load"]["status"],
                    "owner": row["owner"],
                    "record_sha256": row["source"]["record_sha256"],
                    "source_artifact": common.relative(output_path(table)),
                    "table": table,
                    "t13_artifact": row["source"]["artifact"],
                }
            )
            by_id.setdefault(ident, []).append((table, row))
            nodes.add(_node_id(table, ident))
            disposition_sets[row["disposition"]].append(pointer)
            if row["cc_implementation"] == "none":
                implementation_none.append(pointer)
            else:
                implementation_mapped.append(pointer)
            owner_index.setdefault(row["owner"], []).append(pointer)
            closure_counts[row["axes"]["closure"]["status"]] += 1
            fidelity_counts[row["axes"]["fidelity"]["status"]] += 1
            load_counts[row["axes"]["load"]["status"]] += 1
            if row["disposition"] in {"v1_required", "post_1_0"}:
                if not isinstance(row.get("owner"), str) or row["owner"].strip() in common.OWNER_FORBIDDEN:
                    owner_violations.append(f"{table}/{ident}")
            errors = common.validate_identity_record(row)
            if errors:
                raise ValueError(f"{table}/{ident}: {'; '.join(errors)}")
    expected_ids: dict[str, set[str]] = {}
    actual_ids: dict[str, set[str]] = {}
    for table in common.TABLE_SPECS:
        expected_ids[table] = set(common.t13_canonical_ids(table))
        actual_ids[table] = {item["canonical_id"] for item in identities if item["table"] == table}
        equality = common.set_equality_errors(
            expected_ids[table], actual_ids[table], label=table
        )
        if equality:
            raise ValueError("; ".join(equality))
    if len(identities) != 765:
        raise ValueError(f"canonical identities={len(identities)}, expected 765")
    open_index: list[dict[str, Any]] = []
    open_ids: set[str] = set()
    for row in open_items.get("records") or []:
        ident = row["id"]
        open_ids.add(ident)
        nodes.add(_node_id("deferred_open_items", ident))
        open_index.append(
            {
                "canonical_id": ident,
                "disposition": row["disposition"],
                "kind": row["kind"],
                "owner": row["owner"],
                "source_artifact": common.relative(open_items_path()),
                "table": "deferred_open_items",
            }
        )
        if row["kind"] != "canonical_coverage" and row["disposition"] in {
            "v1_required",
            "post_1_0",
        }:
            if not isinstance(row.get("owner"), str) or row["owner"].strip() in common.OWNER_FORBIDDEN:
                owner_violations.append(f"deferred_open_items/{ident}")
    for table, document in table_docs.items():
        for row in document.get("records") or []:
            src = _node_id(table, row["canonical_id"])
            for item in row.get("dependencies") or []:
                kind = item.get("kind")
                target = str(item.get("id") or "")
                if kind == "open_item_id":
                    if target not in open_ids:
                        broken.append(f"{src} -> missing open_item {target}")
                    else:
                        edges.append((src, _node_id("deferred_open_items", target)))
                elif kind == "canonical_id":
                    matches = by_id.get(target) or []
                    if len(matches) != 1:
                        broken.append(
                            f"{src} -> canonical_id {target} matches {len(matches)}"
                        )
                    else:
                        edges.append((src, _node_id(matches[0][0], target)))
                elif kind == "runtime_capability":
                    if not target:
                        broken.append(f"{src} -> blank runtime_capability")
                else:
                    broken.append(f"{src} -> unknown dependency kind {kind}")
    for row in open_items.get("records") or []:
        src = _node_id("deferred_open_items", row["id"])
        for item in row.get("dependencies") or []:
            kind = item.get("kind")
            target = str(item.get("id") or "")
            if kind == "open_item_id":
                if target not in open_ids:
                    broken.append(f"{src} -> missing open_item {target}")
                else:
                    edges.append((src, _node_id("deferred_open_items", target)))
            elif kind == "canonical_id":
                matches = by_id.get(target) or []
                if len(matches) != 1:
                    broken.append(
                        f"{src} -> canonical_id {target} matches {len(matches)}"
                    )
                else:
                    edges.append((src, _node_id(matches[0][0], target)))
            elif kind == "runtime_capability":
                if not target:
                    broken.append(f"{src} -> blank runtime_capability")
            else:
                broken.append(f"{src} -> unknown dependency kind {kind}")
    if broken:
        raise ValueError("broken dependencies: " + "; ".join(broken))
    if owner_violations:
        raise ValueError("owner violations: " + ", ".join(owner_violations))
    layers = _topo_layers(nodes, sorted(set(edges)))
    identities.sort(key=lambda item: (item["table"], item["canonical_id"]))
    for name in disposition_sets:
        disposition_sets[name].sort(key=lambda item: (item["table"], item["canonical_id"]))
    implementation_none.sort(key=lambda item: (item["table"], item["canonical_id"]))
    implementation_mapped.sort(key=lambda item: (item["table"], item["canonical_id"]))
    open_index.sort(key=lambda item: item["canonical_id"])
    for owner in owner_index:
        owner_index[owner].sort(key=lambda item: (item["table"], item["canonical_id"]))
    v1_work_set = [
        {
            "canonical_id": item["canonical_id"],
            "closure": item["closure"],
            "owner": item["owner"],
            "source_artifact": item["source_artifact"],
            "table": item["table"],
        }
        for item in identities
        if item["disposition"] == "v1_required" and item["closure"] != "closed"
    ]
    for row in open_index:
        if (
            row["kind"] != "canonical_coverage"
            and row["disposition"] == "v1_required"
        ):
            source_row = next(
                item
                for item in open_items["records"]
                if item["id"] == row["canonical_id"]
            )
            if source_row["axes"]["closure"]["status"] != "closed":
                v1_work_set.append(
                    {
                        "canonical_id": row["canonical_id"],
                        "closure": source_row["axes"]["closure"]["status"],
                        "owner": row["owner"],
                        "source_artifact": row["source_artifact"],
                        "table": "deferred_open_items",
                    }
                )
    v1_work_set.sort(key=lambda item: (item["table"], item["canonical_id"]))
    t28_count = (contract.get("execution_policy") or {}).get("t28_plus_card_count")
    next_t = (contract.get("execution_policy") or {}).get("next_t")
    if t28_count is not None:
        raise ValueError("t28_plus_card_count must remain null until topology")
    if next_t is not None:
        raise ValueError("next_t must remain null until topology")
    delta = contract.get("publication_delta") or {}
    if delta != {"eager": 0, "lazy": 0, "logical": 0}:
        raise ValueError("phase5 publication_delta is not 0/0/0")
    publication = (opening.get("publication") or {}).get("current") or {}
    owned_inputs = {
        common.relative(BUILDER): common.sha256_file(BUILDER),
        common.relative(PHASE5_CONTRACT): common.sha256_file(PHASE5_CONTRACT),
        common.relative(OPENING_SNAPSHOT): common.sha256_file(OPENING_SNAPSHOT),
        common.relative(open_items_path()): common.sha256_file(open_items_path()),
        common.relative(tracks_path()): common.sha256_file(tracks_path()),
        **{
            common.relative(output_path(table)): common.sha256_file(output_path(table))
            for table in common.TABLE_SPECS
        },
    }
    return {
        "schema_version": 1,
        "status": "T27_PORTFOLIO_AGGREGATED",
        "source_revision": common.SOURCE_REVISION,
        "generated_by": "python tools/build_t27_portfolio.py --write",
        "currentness": {"owned_inputs": owned_inputs},
        "publication_delta": {"eager": 0, "lazy": 0, "logical": 0},
        "opening_publication": publication,
        "t28_plus_card_count": t28_count,
        "next_t": next_t,
        "counts": {
            "canonical": len(identities),
            "implementation_mapped": len(implementation_mapped),
            "implementation_none": len(implementation_none),
            "open_items": len(open_index),
            "out_of_scope": len(disposition_sets["out_of_scope"]),
            "post_1_0": len(disposition_sets["post_1_0"]),
            "tables": table_counts,
            "unclassified": 0,
            "v1_required": len(disposition_sets["v1_required"]),
            "v1_work_set": len(v1_work_set),
        },
        "axes": {
            "closure": dict(sorted(closure_counts.items())),
            "fidelity": dict(sorted(fidelity_counts.items())),
            "load": dict(sorted(load_counts.items())),
        },
        "identities": identities,
        "disposition_sets": disposition_sets,
        "implementation_mapped": implementation_mapped,
        "implementation_none": implementation_none,
        "owners": {
            owner: owner_index[owner] for owner in sorted(owner_index)
        },
        "open_items": open_index,
        "tracks": {
            track: {
                "initial_card": spec.get("initial_card"),
                "primary_identity_count": spec.get("primary_identity_count"),
                "started": spec.get("started"),
            }
            for track, spec in (tracks.get("tracks") or {}).items()
        },
        "dependency_graph": {
            "edge_count": len(set(edges)),
            "edges": [
                {"from": src, "to": dst} for src, dst in sorted(set(edges))
            ],
            "layers": layers,
            "node_count": len(nodes),
        },
        "v1_work_set": v1_work_set,
        "validators": {
            "broken_dependencies": 0,
            "cycles": 0,
            "missing": 0,
            "open_item_orphans": 0,
            "owner_violations": 0,
            "unclassified": 0,
            "unexpected": 0,
        },
    }


def write_aggregate() -> dict[str, Any]:
    document = build_aggregate()
    common.write_stable(AGGREGATE, document)
    return document


def check_aggregate() -> list[str]:
    if not AGGREGATE.is_file():
        return [f"missing generated file: {common.relative(AGGREGATE)}"]
    expected = common.stable_json(build_aggregate())
    actual = AGGREGATE.read_text(encoding="utf-8")
    if actual != expected:
        return [f"{common.relative(AGGREGATE)} is stale"]
    return []


def write_table(table: str) -> dict[str, Any]:
    document = build_table(table)
    common.write_stable(output_path(table), document)
    return document


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--write-table")
    parser.add_argument("--check-table")
    parser.add_argument("--check-all-tables", action="store_true")
    parser.add_argument("--write-open-items", action="store_true")
    parser.add_argument("--check-open-items", action="store_true")
    parser.add_argument("--write-tracks", action="store_true")
    parser.add_argument("--check-tracks", action="store_true")
    parser.add_argument("--check-load-projections", action="store_true")
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args(argv)
    selected = [
        flag
        for flag in (
            args.write_table,
            args.check_table,
            args.check_all_tables,
            args.write_open_items,
            args.check_open_items,
            args.write_tracks,
            args.check_tracks,
            args.check_load_projections,
            args.write,
            args.check,
        )
        if flag
    ]
    if len(selected) != 1:
        parser.error(
            "choose exactly one of --write-table, --check-table, "
            "--check-all-tables, --write-open-items, --check-open-items, "
            "--write-tracks, --check-tracks, --check-load-projections, "
            "--write, --check"
        )
    try:
        if args.write_table:
            document = write_table(args.write_table)
            summary = {"table": args.write_table, "counts": document.get("counts")}
        elif args.check_table:
            errors = check_table(args.check_table)
            if errors:
                raise ValueError("; ".join(errors))
            document = common.load_json(output_path(args.check_table))
            summary = {"table": args.check_table, "counts": document.get("counts")}
        elif args.write_open_items:
            document = write_open_items()
            summary = {"table": OPEN_ITEMS_NAME, "counts": document.get("counts")}
        elif args.check_open_items:
            errors = check_open_items()
            if errors:
                raise ValueError("; ".join(errors))
            document = common.load_json(open_items_path())
            summary = {"table": OPEN_ITEMS_NAME, "counts": document.get("counts")}
        elif args.write_tracks:
            document = write_tracks()
            summary = {"table": TRACKS_NAME, "counts": document.get("counts")}
        elif args.check_tracks:
            errors = check_tracks()
            if errors:
                raise ValueError("; ".join(errors))
            document = common.load_json(tracks_path())
            summary = {"table": TRACKS_NAME, "counts": document.get("counts")}
        elif args.check_load_projections:
            errors = check_load_projections()
            if errors:
                raise ValueError("; ".join(errors))
            document = common.load_json(tracks_path())
            summary = {
                "table": TRACKS_NAME,
                "eager_hard_ceiling": 21000,
                "publication_delta": document.get("publication_delta"),
                "started": document.get("started"),
            }
        elif args.write:
            document = write_aggregate()
            summary = {
                "canonical identities": document["counts"]["canonical"],
                "publication_delta": document.get("publication_delta"),
                "unclassified": document["counts"]["unclassified"],
                "v1_work_set": document["counts"]["v1_work_set"],
            }
        elif args.check:
            errors = check_aggregate()
            if errors:
                raise ValueError("; ".join(errors))
            document = common.load_json(AGGREGATE)
            summary = {
                "canonical identities": document["counts"]["canonical"],
                "publication_delta": document.get("publication_delta"),
                "unclassified": document["counts"]["unclassified"],
                "v1_work_set": document["counts"]["v1_work_set"],
            }
        else:
            errors = check_all_tables()
            if errors:
                raise ValueError("; ".join(errors))
            summary = {
                "tables": 7,
                "canonical identities": 765,
                "unclassified": 0,
            }
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"T27 portfolio failed: {error}", file=sys.stderr)
        return 1
    print(json.dumps(summary, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
