#!/usr/bin/python3
"""Compose frozen v2 identity ledger with ordered semantic-slug deltas."""
from __future__ import annotations

import copy
import hashlib
from typing import Any

from tools import io_common as files
from tools.recipe_bulk import identity as identity_v1
from tools.recipe_bulk import identity_v2
from tools.recipe_bulk.membership import identity_semantic_root
from tools.recipe_bulk.slugs import WaveSlugError, parse_wave_token

STATUS = "GLOBAL_BUILD_IDENTITY_LEDGER_V3"
V2_PATH = files.TOOLS / "global_build_identity_ledger.v2.json"
OUTPUT = files.TOOLS / "global_build_identity_ledger.v3.json"
DELTA_ORDER: tuple[str, ...] = (
    "smelter/ordinary-closure",
    "mixer/ordinary-closure",
    "drying/ordinary-closure",
    "electrolyzer/ordinary-closure",
    "centrifuge/ordinary-closure",
    "autoclave/ordinary-closure",
    "compressor/ordinary-closure",
    "smelter/deferred-recycling",
)
DELTA_PATHS: dict[str, Any] = {
    slug: files.TOOLS / "waves" / slug / "identity_ledger_delta.json"
    for slug in DELTA_ORDER
}


class IdentityV3ConflictError(ValueError):
    """Semantic-v3 composition detected a fail-closed identity conflict."""


def _file_hash(path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest() if path.is_file() else ""


def _empty_alias_root() -> str:
    return identity_semantic_root([])


def _composition_root(
    v2_file_sha256: str,
    alias_root: str,
    deltas: list[dict[str, Any]],
) -> str:
    payload = (
        f"{v2_file_sha256}\t{alias_root}\t"
        + files.stable_json(deltas)
    )
    return hashlib.sha256(payload.encode("utf-8")).hexdigest()


def load_delta(wave_slug: str) -> dict[str, Any]:
    parse_wave_token(wave_slug, schema="semantic-v3")
    path = DELTA_PATHS.get(wave_slug)
    if path is None or not path.is_file():
        return {
            "blockers": [],
            "order": wave_slug,
            "records": [],
            "schema_version": 1,
            "status": f"{wave_slug.replace('/', '_').upper()}_IDENTITY_LEDGER_DELTA",
            "wave_slug": wave_slug,
        }
    document = files.load_json(path)
    if document.get("wave_id") is not None:
        raise IdentityV3ConflictError(
            f"{path} must not write wave_id; use wave_slug only"
        )
    if str(document.get("wave_slug") or "") != wave_slug:
        raise IdentityV3ConflictError(f"{path} wave_slug drifted")
    return document


def _apply_row(
    *,
    store: dict[str, dict[str, Any]],
    other: dict[str, dict[str, Any]],
    row: dict[str, Any],
    wave_slug: str,
) -> None:
    key = str(row["source_key"])
    if key in other:
        raise IdentityV3ConflictError(
            f"{wave_slug}: {key} overlaps the opposite store without a resolution delta"
        )
    existing = store.get(key)
    if existing is None:
        store[key] = copy.deepcopy(row)
        return
    if existing.get("target_identity") != row.get("target_identity"):
        raise IdentityV3ConflictError(
            f"{wave_slug}: duplicate source key with different target: {key}"
        )
    if existing.get("mapping_class") != row.get("mapping_class"):
        raise IdentityV3ConflictError(
            f"{wave_slug}: mapping_class conflict for {key}"
        )


def compose(v2: dict[str, Any] | None = None) -> dict[str, Any]:
    if not V2_PATH.is_file():
        raise IdentityV3ConflictError(f"missing frozen v2 ledger {V2_PATH}")
    v2_hash = _file_hash(V2_PATH)
    base = v2 if v2 is not None else files.load_json(V2_PATH)
    if base.get("status") != identity_v2.STATUS:
        raise IdentityV3ConflictError("v2 identity ledger status drifted")
    records = {
        str(row["source_key"]): copy.deepcopy(row)
        for row in list(base.get("records") or [])
    }
    blockers = {
        str(row["source_key"]): copy.deepcopy(row)
        for row in list(base.get("blockers") or [])
    }
    consumed: list[dict[str, Any]] = []
    for wave_slug in DELTA_ORDER:
        delta = load_delta(wave_slug)
        path = DELTA_PATHS.get(wave_slug)
        for row in delta.get("records") or []:
            _apply_row(store=records, other=blockers, row=row, wave_slug=wave_slug)
        for row in delta.get("blockers") or []:
            _apply_row(store=blockers, other=records, row=row, wave_slug=wave_slug)
        consumed.append(
            {
                "file_sha256": _file_hash(path) if path is not None else "",
                "order": str(delta.get("order") or wave_slug),
                "path": files.relative(path) if path is not None else "",
                "wave_slug": wave_slug,
            }
        )
    record_rows = [records[key] for key in sorted(records)]
    blocker_rows = [blockers[key] for key in sorted(blockers)]
    v2_logical = str((base.get("composition") or {}).get("semantic_root_sha256") or "")
    logical = identity_semantic_root(record_rows + blocker_rows)
    alias_root = _empty_alias_root()
    envelope = _composition_root(v2_hash, alias_root, consumed)
    empty = not DELTA_ORDER
    if empty and logical != v2_logical:
        raise IdentityV3ConflictError(
            "empty v3 composition must preserve frozen v2 logical identities"
        )
    counts: dict[str, int] = {}
    for row in record_rows + blocker_rows:
        counts[row["mapping_class"]] = counts.get(row["mapping_class"], 0) + 1
    return {
        "blocker_count": len(blocker_rows),
        "blockers": blocker_rows,
        "composition": {
            "conflict_check": "pass",
            "consumed_deltas": consumed,
            "logical_identities_equal_v2": empty and logical == v2_logical,
            "semantic_alias_root_sha256": alias_root,
            "semantic_root_sha256": envelope,
            "v2_file_sha256": v2_hash,
            "v2_logical_identity_root_sha256": v2_logical,
        },
        "counts": dict(sorted(counts.items())),
        "generated_by": "python tools/build_global_build_identity_ledger_v3.py",
        "note": (
            "Forward-v3 identity ledger. Frozen v2 file hash plus semantic "
            "alias root plus ordered slug deltas. Does not rewrite v2."
        ),
        "record_count": len(record_rows),
        "records": record_rows,
        "schema_version": 3,
        "status": STATUS,
        "v2_base": {
            "file_sha256": v2_hash,
            "path": files.relative(V2_PATH),
            "semantic_root_sha256": v2_logical,
            "status": base.get("status"),
        },
    }


def envelope_document(composed: dict[str, Any] | None = None) -> dict[str, Any]:
    """Persistable composition contract without duplicating v2 record bytes."""
    payload = composed if composed is not None else compose()
    return {
        "blocker_count": payload["blocker_count"],
        "composition": payload["composition"],
        "counts": payload["counts"],
        "generated_by": payload["generated_by"],
        "note": payload["note"],
        "record_count": payload["record_count"],
        "schema_version": 3,
        "status": STATUS,
        "v2_base": payload["v2_base"],
    }


def build() -> dict[str, Any]:
    return envelope_document()


def index_ledger(document: dict[str, Any] | None = None) -> dict[str, dict[str, dict[str, Any]]]:
    payload = document if document is not None and "records" in document else compose()
    return identity_v1.index_ledger(payload)


def lookup_first(
    wave_id: str | None,
    operand: dict[str, Any],
    *,
    index: dict[str, dict[str, dict[str, Any]]] | None = None,
) -> tuple[str, dict[str, Any]] | None:
    if wave_id is not None:
        try:
            parse_wave_token(wave_id, schema="semantic-v3")
        except WaveSlugError as error:
            raise IdentityV3ConflictError(
                f"semantic-v3 identity lookup requires a host/cohort slug: {wave_id!r}"
            ) from error
    return identity_v2.lookup_first(wave_id, operand, index=index)
