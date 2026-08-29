#!/usr/bin/env python3
"""Shared deterministic helpers and vocabulary for T35 census builders."""
from __future__ import annotations

import hashlib
import json
from pathlib import Path
from typing import Any

from tools import t27_common as base

ROOT = base.ROOT
TOOLS = base.TOOLS
SOURCE_REVISION = base.SOURCE_REVISION

POLICY = TOOLS / "t35_census_policy.json"
INPUTS = TOOLS / "t35_census_inputs.json"
RUNTIME_REGISTRY = TOOLS / "t35_runtime_registry.json"
EXCLUSION_RECLAIM = TOOLS / "t35_excluded_object_reclaim.json"
RECIPE_FAMILIES = TOOLS / "t35_recipe_families.json"
LOAD_BASELINE = TOOLS / "t35_load_baseline.json"
CENSUS = TOOLS / "t35_census.json"
CARD_TOPOLOGY = TOOLS / "t35_card_topology.json"
READINESS = TOOLS / "t35_readiness.json"
STORAGE_SCOPE = TOOLS / "t35_storage_scope.json"
MACHINE_TRACK = TOOLS / "t35_machine_track.json"
MACHINE_TIERS = (
    ROOT / "src" / "main" / "resources" / "data" / "cruciblecraft" / "machine_tiers.json"
)
MACHINE_TIERS_SCHEMA = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "schema"
    / "machine_tiers.schema.json"
)

DISPOSITIONS = ("implemented", "planned", "deferred", "out_of_scope")
PRIORITIES = ("P0", "P1", "P2", "P3")
PORTFOLIO_SCOPES = ("in_scope_1x", "candidate_1x", "post_1x", "out_of_scope")
CLOSURE_STATUSES = ("closed", "incomplete", "not_applicable")
FIDELITY_STATUSES = ("source_backed", "source_derived", "design_policy")
LOAD_STATUSES = ("measured", "projected", "pending", "not_applicable")
PENDING_LOAD_VERDICT = base.PENDING_LOAD_VERDICT
P2_ADMISSION = "ADMITTED_UNDER_SOFT"
FIXED_CARDS = ("T36", "T37")
GLOBAL_EXECUTION_GATE = "T37"
TOPOLOGY_EPOCH = "T35R_A"
NUMBERING_POLICY = "regenerate_once_then_append_only"
DEPENDENCY_KINDS = (
    "canonical_family",
    "fixed_card",
    "open_item",
    "runtime_capability",
)
PUBLICATION_DELTA = {"eager": 0, "lazy": 0, "logical": 0}
ENERGY_TRACKS = {
    "KINETIC_ROTATION": "RU",
    "KINETIC_PUSH": "KU",
    "HEAT": "HU",
    "ELECTRIC": "EU",
    "TIME": "TU",
}


def relative(path: Path) -> str:
    return base.relative(path)


def load_json(path: Path) -> Any:
    return base.load_json(path)


def sha256_file(path: Path) -> str:
    return base.sha256_file(path)


def stable_json(value: Any) -> str:
    return base.stable_json(value)


def write_stable(path: Path, document: Any) -> None:
    from tools import atomic_io

    atomic_io.write_bytes(path, stable_json(document).encode("utf-8"))


def first_json_diff(expected: Any, actual: Any, path: str = "$") -> str | None:
    if type(expected) is not type(actual):
        return (
            f"{path}: type {type(expected).__name__} != {type(actual).__name__}"
        )
    if isinstance(expected, dict):
        expected_keys = set(expected)
        actual_keys = set(actual)
        missing = sorted(expected_keys - actual_keys)
        extra = sorted(actual_keys - expected_keys)
        if missing:
            return f"{path}.{missing[0]}: missing in actual"
        if extra:
            return f"{path}.{extra[0]}: extra in actual"
        for key in sorted(expected_keys):
            diff = first_json_diff(expected[key], actual[key], f"{path}.{key}")
            if diff:
                return diff
        return None
    if isinstance(expected, list):
        if len(expected) != len(actual):
            return f"{path}: len {len(expected)} != {len(actual)}"
        for index, (left, right) in enumerate(zip(expected, actual)):
            diff = first_json_diff(left, right, f"{path}[{index}]")
            if diff:
                return diff
        return None
    if expected != actual:
        left = repr(expected)
        right = repr(actual)
        if len(left) > 80:
            left = left[:77] + "..."
        if len(right) > 80:
            right = right[:77] + "..."
        return f"{path}: {left} != {right}"
    return None


CURRENTNESS_FIELD_NAMES = frozenset(
    {
        "currentness",
        "owned_inputs",
        "generated_by",
        "generated_at",
        "currentness_root_sha256",
        "semantic_root_sha256",
    }
)


def _strip_currentness(document: Any) -> Any:
    if isinstance(document, dict):
        return {
            key: _strip_currentness(value)
            for key, value in document.items()
            if key not in CURRENTNESS_FIELD_NAMES
        }
    if isinstance(document, list):
        return [_strip_currentness(item) for item in document]
    return document


def classify_stale(
    path: Path,
    expected_text: str,
    actual_text: str,
) -> tuple[str, str]:
    try:
        rel = relative(path)
    except ValueError:
        rel = path.as_posix()
    if not path.is_file():
        return "MISSING", f"{rel} is stale (missing)"
    try:
        actual_doc = json.loads(actual_text)
    except json.JSONDecodeError as error:
        size = path.stat().st_size
        return (
            "CORRUPT",
            (
                f"{rel} is stale "
                f"(JSON line={error.lineno} column={error.colno} size={size})"
            ),
        )
    try:
        expected_doc = json.loads(expected_text)
    except json.JSONDecodeError:
        rebuilt_hash = hashlib.sha256(expected_text.encode("utf-8")).hexdigest()
        return (
            "HASH_ONLY_DRIFT",
            (
                f"{rel} is stale "
                f"(on_disk_sha256={sha256_file(path)} rebuilt_sha256={rebuilt_hash})"
            ),
        )
    diff = first_json_diff(expected_doc, actual_doc)
    semantic_diff = first_json_diff(
        _strip_currentness(expected_doc),
        _strip_currentness(actual_doc),
    )
    rebuilt_hash = hashlib.sha256(expected_text.encode("utf-8")).hexdigest()
    disk_hash = sha256_file(path)
    if semantic_diff:
        return (
            "SEMANTIC_DRIFT",
            f"{rel} is stale at {semantic_diff}",
        )
    if diff or disk_hash != rebuilt_hash:
        return (
            "HASH_ONLY_DRIFT",
            (
                f"{rel} is stale "
                f"(on_disk_sha256={disk_hash} rebuilt_sha256={rebuilt_hash})"
            ),
        )
    return "HASH_ONLY_DRIFT", f"{rel} is stale"


def stale_error(path: Path, expected_text: str, actual_text: str) -> str:
    classification, message = classify_stale(path, expected_text, actual_text)
    return f"{classification}: {message}"


def check_generated_document(path: Path, document: Any) -> list[str]:
    """Compare a rebuilt document to disk, using a currentness sidecar for hash-only drift."""
    if not path.is_file():
        return [f"MISSING: {relative(path)} is stale (missing)"]
    expected = stable_json(document)
    actual = path.read_text(encoding="utf-8")
    if actual == expected:
        return _sidecar_check(path, required=False)
    classification, message = classify_stale(path, expected, actual)
    if classification == "HASH_ONLY_DRIFT":
        sidecar_errors = _sidecar_check(path, required=True)
        if sidecar_errors is not None:
            return sidecar_errors
    return [f"{classification}: {message}"]


def _sidecar_check(path: Path, *, required: bool) -> list[str] | None:
    from tools import currentness

    sidecar = currentness.sidecar_path(path)
    if not sidecar.is_file():
        return [] if not required else None
    return currentness.check_sidecar(path)


def compact_check(module: Any) -> list[str]:
    if hasattr(module, "reference_only_check"):
        return module.reference_only_check()
    return module.check()


def validate_epoch_b_append_only(
    previous_cards: list[dict[str, Any]],
    current_cards: list[dict[str, Any]],
) -> list[str]:
    errors: list[str] = []
    previous_ids = [card.get("id") for card in previous_cards]
    current_ids = [card.get("id") for card in current_cards]
    if current_ids[: len(previous_ids)] != previous_ids:
        errors.append("Epoch B must not renumber Epoch A cards")
        return errors
    for previous, current in zip(previous_cards, current_cards):
        if previous != current:
            errors.append(f"Epoch A card {previous.get('id')} mutated under append-only policy")
            break
    return errors


def scope_contract_for(scope: str, record: dict[str, Any]) -> dict[str, Any]:
    if scope == "in_scope_1x":
        contract = {"owner": record.get("owner")}
        closure = (record.get("axes") or {}).get("closure", {}).get("status")
        if closure != "closed":
            contract["work_set"] = "fixed_or_generated"
        return contract
    if scope == "candidate_1x":
        return {
            "admission_criterion": (
                "ADMITTED_UNDER_SOFT after T37 family-or-bundle measurement; "
                "no map-wide compression ratio"
            ),
            "measurement_owner": "T37",
            "recheck_epoch": "T37_closure",
        }
    if scope == "post_1x":
        track = record.get("subsequent_track")
        if not isinstance(track, str) or not track.startswith("post_1x/"):
            raise ValueError(
                f"{record.get('canonical_id')} post_1x missing subsequent_track"
            )
        if track in {"post_1x", "post_1_0", "post_1x/post_1_0"}:
            raise ValueError(
                f"{record.get('canonical_id')} subsequent_track too generic: {track}"
            )
        return {"subsequent_track": track}
    if scope == "out_of_scope":
        reason = str(record.get("reason") or "").strip()
        if not reason:
            raise ValueError(
                f"{record.get('canonical_id')} out_of_scope missing exclusion reason"
            )
        return {"exclusion_reason": reason}
    raise ValueError(f"illegal portfolio_scope {scope!r}")


def default_subsequent_track(record: dict[str, Any]) -> str:
    canonical_id = str(record.get("canonical_id") or "")
    if canonical_id.startswith("exclusion/"):
        return f"post_1x/{canonical_id}"
    owner = str(record.get("owner") or "")
    if owner.startswith("portfolio:post_1_0/"):
        return "post_1x/" + owner.split("portfolio:post_1_0/", 1)[1]
    table = record.get("table")
    if isinstance(table, str) and table:
        return f"post_1x/{table}"
    domain = str(record.get("domain") or "unspecified")
    return f"post_1x/{domain}"


def assign_portfolio_scope(
    record: dict[str, Any],
    *,
    storage_scope: dict[str, Any] | None = None,
    t37_ids: set[str] | None = None,
) -> dict[str, Any]:
    canonical_id = str(record.get("canonical_id") or "")
    disposition = record.get("disposition")
    priority = record.get("portfolio_priority")
    closure = (record.get("axes") or {}).get("closure", {}).get("status")
    updated = dict(record)

    family_override = None
    if canonical_id.startswith("exclusion/") and storage_scope:
        family = canonical_id.split("/", 1)[1]
        family_override = (storage_scope.get("families") or {}).get(family)

    if isinstance(family_override, dict) and family_override.get("portfolio_scope"):
        scope = family_override["portfolio_scope"]
        if family_override.get("subsequent_track"):
            updated["subsequent_track"] = family_override["subsequent_track"]
    elif canonical_id.startswith("runtime_local:machine_tier/"):
        scope = "in_scope_1x"
    elif t37_ids and canonical_id in t37_ids:
        scope = "in_scope_1x"
    elif disposition == "out_of_scope":
        scope = "out_of_scope"
    elif disposition == "implemented" and closure == "closed":
        scope = "in_scope_1x"
    elif disposition == "planned" and priority in {"P0", "P1"}:
        scope = "in_scope_1x"
    elif (
        disposition == "deferred"
        and priority == "P2"
        and record.get("p2_admission") == "BLOCKED_PENDING_MEASUREMENT"
    ):
        scope = "candidate_1x"
    elif disposition == "deferred" and priority == "P2":
        scope = "candidate_1x"
    elif disposition == "deferred" and priority == "P3":
        updated.setdefault("subsequent_track", default_subsequent_track(record))
        scope = "post_1x"
    elif record.get("historical_classification", {}).get("t27") == "post_1_0":
        updated.setdefault("subsequent_track", default_subsequent_track(record))
        scope = "post_1x"
    else:
        raise ValueError(f"{canonical_id} cannot assign portfolio_scope")

    if scope not in PORTFOLIO_SCOPES:
        raise ValueError(f"{canonical_id} illegal portfolio_scope {scope!r}")
    updated["portfolio_scope"] = scope
    updated["scope_contract"] = scope_contract_for(scope, updated)
    updated.pop("subsequent_track", None)
    return updated


def source_hashes(*paths: Path) -> dict[str, str]:
    missing = [relative(path) for path in paths if not path.is_file()]
    if missing:
        raise FileNotFoundError(", ".join(missing))
    return {relative(path): sha256_file(path) for path in sorted(paths)}


def validate_publication_delta(value: Any, *, label: str) -> list[str]:
    if value != PUBLICATION_DELTA:
        return [f"{label} publication_delta must equal {PUBLICATION_DELTA}"]
    return []


def validate_pending_load(load: Any, *, label: str) -> list[str]:
    if not isinstance(load, dict) or load.get("status") != "pending":
        return []
    errors: list[str] = []
    if load.get("verdict") != PENDING_LOAD_VERDICT:
        errors.append(f"{label} pending load must use {PENDING_LOAD_VERDICT}")
    for key in base.FAKE_ZERO_LOAD_KEYS:
        if load.get(key) == 0:
            errors.append(f"{label} pending load must not zero-fill {key}")
    return errors


def require_single(mapping: dict[str, Any], key: str, *, label: str) -> Any:
    value = mapping.get(key)
    if value is None:
        raise ValueError(f"{label} is missing {key}")
    return value
