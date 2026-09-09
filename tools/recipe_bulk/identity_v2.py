#!/usr/bin/python3
"""Compose immutable v1 identity ledger with ordered forward-v2 deltas."""
from __future__ import annotations

import copy
from typing import Any

import hashlib

from tools import io_common as files
from tools.recipe_bulk import identity as identity_v1
from tools.recipe_bulk.membership import identity_semantic_root

STATUS = "GLOBAL_BUILD_IDENTITY_LEDGER_V2"
DELTA_ORDER = ("bath/mte", "bath/remainder", "bath/identity", "bath/tiny-purified")
V1_PATH = files.TOOLS / "global_build_identity_ledger.json"
DELTA_PATHS = {
    "bath/mte": files.TOOLS / "bath_mte_identity_ledger_delta.json",
    "bath/remainder": files.TOOLS / "bath_remainder_identity_ledger_delta.json",
    "bath/identity": files.TOOLS / "bath_identity_identity_ledger_delta.json",
    "bath/tiny-purified": files.TOOLS / "bath_tiny_purified_identity_ledger_delta.json",
}
WAVE_ORDER = {
    "bath/mte": 1,
    "bath/remainder": 2,
    "bath/identity": 3,
    "bath/tiny-purified": 4,
}


class IdentityV2ConflictError(ValueError):
    """Forward-v2 composition detected a fail-closed identity conflict."""


def _file_hash(path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest() if path.is_file() else ""


def v1_semantic_root(document: dict[str, Any] | None = None) -> str:
    payload = document if document is not None else files.load_json(V1_PATH)
    return identity_semantic_root(
        list(payload.get("records") or []) + list(payload.get("blockers") or [])
    )


def _index_rows(rows: list[dict[str, Any]]) -> dict[str, dict[str, Any]]:
    indexed: dict[str, dict[str, Any]] = {}
    for row in rows:
        key = str(row["source_key"])
        if key in indexed:
            raise IdentityV2ConflictError(f"duplicate source key in store: {key}")
        indexed[key] = row
    return indexed


def _apply_row(
    *,
    store: dict[str, dict[str, Any]],
    other: dict[str, dict[str, Any]],
    row: dict[str, Any],
    wave_id: str,
) -> None:
    key = str(row["source_key"])
    if key in other:
        raise IdentityV2ConflictError(
            f"{wave_id}: {key} overlaps a frozen v1 typed opposite store "
            "without an explicit resolution delta"
        )
    existing = store.get(key)
    if existing is None:
        store[key] = copy.deepcopy(row)
        return
    if existing.get("target_identity") != row.get("target_identity"):
        raise IdentityV2ConflictError(
            f"{wave_id}: duplicate source key with different target: {key}"
        )
    if existing.get("mapping_class") != row.get("mapping_class"):
        raise IdentityV2ConflictError(
            f"{wave_id}: mapping_class conflict for {key}"
        )


def load_delta(wave_id: str) -> dict[str, Any]:
    path = DELTA_PATHS[wave_id]
    if not path.is_file():
        return {
            "blockers": [],
            "order": WAVE_ORDER[wave_id],
            "records": [],
            "schema_version": 1,
            "status": f"{wave_id.replace('/', '_').upper()}_IDENTITY_LEDGER_DELTA",
            "wave_id": wave_id,
        }
    document = files.load_json(path)
    if document.get("wave_id") != wave_id:
        raise IdentityV2ConflictError(f"{path} wave_id drifted")
    return document


def compose(v1: dict[str, Any] | None = None) -> dict[str, Any]:
    global _COMPOSE_CACHE
    if v1 is None and _COMPOSE_CACHE is not None:
        return _COMPOSE_CACHE
    payload = _compose(v1)
    if v1 is None:
        _COMPOSE_CACHE = payload
    return payload


_COMPOSE_CACHE: dict[str, Any] | None = None
_INDEX_CACHE: dict[str, dict[str, dict[str, Any]]] | None = None


def _compose(v1: dict[str, Any] | None = None) -> dict[str, Any]:
    base = v1 if v1 is not None else files.load_json(V1_PATH)
    if base.get("status") != "GLOBAL_BUILD_IDENTITY_LEDGER_V1":
        raise IdentityV2ConflictError("v1 identity ledger status drifted")
    records = _index_rows(list(base.get("records") or []))
    blockers = _index_rows(list(base.get("blockers") or []))
    consumed: list[dict[str, Any]] = []
    for wave_id in DELTA_ORDER:
        delta = load_delta(wave_id)
        path = DELTA_PATHS[wave_id]
        for row in delta.get("records") or []:
            _apply_row(store=records, other=blockers, row=row, wave_id=wave_id)
        for row in delta.get("blockers") or []:
            _apply_row(store=blockers, other=records, row=row, wave_id=wave_id)
        consumed.append(
            {
                "file_sha256": _file_hash(path),
                "order": int(delta.get("order") or WAVE_ORDER[wave_id]),
                "path": files.relative(path),
                "wave_id": wave_id,
            }
        )
    record_rows = [records[key] for key in sorted(records)]
    blocker_rows = [blockers[key] for key in sorted(blockers)]
    counts: dict[str, int] = {}
    for row in record_rows + blocker_rows:
        counts[row["mapping_class"]] = counts.get(row["mapping_class"], 0) + 1
    v1_hash = _file_hash(V1_PATH)
    semantic = identity_semantic_root(record_rows + blocker_rows)
    return {
        "blocker_count": len(blocker_rows),
        "blockers": blocker_rows,
        "composition": {
            "base_record_count": int(base.get("record_count") or len(base.get("records") or [])),
            "conflict_check": "pass",
            "consumed_deltas": consumed,
            "semantic_root_sha256": semantic,
            "v1_file_sha256": v1_hash,
            "v1_semantic_root_sha256": v1_semantic_root(base),
        },
        "counts": dict(sorted(counts.items())),
        "generated_by": "python tools/build_global_build_identity_ledger_v2.py",
        "note": (
            "Forward-v2 identity ledger. Immutable v1 base plus ordered wave "
            "deltas. Does not rewrite v1 records."
        ),
        "record_count": len(record_rows),
        "records": record_rows,
        "schema_version": 2,
        "status": STATUS,
        "v1_base": {
            "file_sha256": v1_hash,
            "path": files.relative(V1_PATH),
            "semantic_root_sha256": v1_semantic_root(base),
            "status": base.get("status"),
        },
    }


def build() -> dict[str, Any]:
    return compose()


def index_ledger(document: dict[str, Any] | None = None) -> dict[str, dict[str, dict[str, Any]]]:
    global _INDEX_CACHE
    if document is None:
        if _INDEX_CACHE is None:
            _INDEX_CACHE = identity_v1.index_ledger(compose())
        return _INDEX_CACHE
    return identity_v1.index_ledger(document)


def lookup_first(
    wave_id: str | None,
    operand: dict[str, Any],
    *,
    index: dict[str, dict[str, dict[str, Any]]] | None = None,
) -> tuple[str, dict[str, Any]] | None:
    store = index if index is not None else index_ledger()
    source = operand.get("source") or {}
    item = source.get("item") or operand.get("item")
    meta = source.get("meta")
    extra: list[str] = []
    if item == "gregtech:gt.multitileentity":
        extra.append(f"mte:{item}{identity_v1.meta_suffix(meta)}")
    for key in extra + identity_v1.candidate_keys(wave_id, operand):
        record = store["records"].get(key)
        if record is not None:
            return "proven", record
        blocker = store["blockers"].get(key)
        if blocker is not None:
            return "blocker", blocker
    return None
