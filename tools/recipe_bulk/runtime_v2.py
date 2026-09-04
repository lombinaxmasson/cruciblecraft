#!/usr/bin/python3
"""Compose immutable v1 compact runtime manifest with ordered forward-v2 group deltas."""
from __future__ import annotations

import copy
from typing import Any

from tools import census_common as census
from tools.recipe_bulk.membership import identity_semantic_root

STATUS = "COMPACT_RECIPE_RUNTIME_MANIFEST_V2"
BASE_GROUP_COUNT = 12
DELTA_ORDER = ("bath/mte", "bath/remainder", "bath/identity", "bath/tiny-purified")
V1_PATH = census.TOOLS / "compact_recipe_runtime_manifest.json"
DELTA_PATHS = {
    "bath/mte": census.TOOLS / "bath_mte_runtime_manifest_delta.json",
    "bath/remainder": census.TOOLS / "bath_remainder_runtime_manifest_delta.json",
    "bath/identity": census.TOOLS / "bath_identity_runtime_manifest_delta.json",
    "bath/tiny-purified": census.TOOLS / "bath_tiny_purified_runtime_manifest_delta.json",
}
WAVE_ORDER = {
    "bath/mte": 1,
    "bath/remainder": 2,
    "bath/identity": 3,
    "bath/tiny-purified": 4,
}


class RuntimeV2ConflictError(ValueError):
    """Forward-v2 runtime composition failed closed."""


def _file_hash(path) -> str:
    return census.sha256_file(path) if path.is_file() else ""


def _group_semantic_root(groups: list[dict[str, Any]]) -> str:
    rows = [
        {
            "disposition": "group",
            "source_key": row.get("publication_group"),
            "target_identity": row.get("membership_root_sha256"),
        }
        for row in groups
    ]
    return identity_semantic_root(rows)


def load_delta(wave_id: str) -> dict[str, Any]:
    path = DELTA_PATHS[wave_id]
    if not path.is_file():
        return {
            "groups": [],
            "order": WAVE_ORDER[wave_id],
            "schema_version": 1,
            "status": f"{wave_id.replace('/', '_').upper()}_RUNTIME_MANIFEST_DELTA",
            "wave_id": wave_id,
        }
    document = census.load_json(path)
    if document.get("wave_id") != wave_id:
        raise RuntimeV2ConflictError(f"{path} wave_id drifted")
    return document


def compose(v1: dict[str, Any] | None = None) -> dict[str, Any]:
    base = v1 if v1 is not None else census.load_json(V1_PATH)
    if base.get("status") != "COMPACT_RECIPE_RUNTIME_MANIFEST":
        raise RuntimeV2ConflictError("v1 runtime manifest status drifted")
    if int(base.get("group_count") or 0) != BASE_GROUP_COUNT:
        raise RuntimeV2ConflictError("v1 runtime manifest group_count drifted from 12")
    groups = copy.deepcopy(list(base.get("groups") or []))
    historical_roots = {
        str(row["publication_group"]): str(row["membership_root_sha256"])
        for row in groups
    }
    seen = set(historical_roots)
    consumed: list[dict[str, Any]] = []
    for wave_id in DELTA_ORDER:
        delta = load_delta(wave_id)
        path = DELTA_PATHS[wave_id]
        for row in delta.get("groups") or []:
            group_id = str(row.get("publication_group") or "")
            if not group_id:
                raise RuntimeV2ConflictError(f"{wave_id} delta group missing publication_group")
            if group_id in seen:
                raise RuntimeV2ConflictError(f"{wave_id} duplicate group id {group_id}")
            seen.add(group_id)
            groups.append(copy.deepcopy(row))
        consumed.append(
            {
                "file_sha256": _file_hash(path),
                "order": int(delta.get("order") or WAVE_ORDER[wave_id]),
                "path": census.relative(path),
                "wave_id": wave_id,
            }
        )
    for row in groups[:BASE_GROUP_COUNT]:
        group_id = str(row["publication_group"])
        if historical_roots.get(group_id) != row.get("membership_root_sha256"):
            raise RuntimeV2ConflictError(
                f"historical membership root drifted for {group_id}"
            )
    v1_hash = _file_hash(V1_PATH)
    return {
        "bindings": dict(base.get("bindings") or {}),
        "composition": {
            "base_group_count": BASE_GROUP_COUNT,
            "composed_group_count": len(groups),
            "conflict_check": "pass",
            "consumed_deltas": consumed,
            "historical_membership_roots_unchanged": True,
            "semantic_root_sha256": _group_semantic_root(groups),
            "v1_file_sha256": v1_hash,
            "v1_semantic_root_sha256": _group_semantic_root(list(base.get("groups") or [])),
        },
        "dedup_rules": copy.deepcopy(list(base.get("dedup_rules") or [])),
        "generated_by": "python tools/build_compact_recipe_runtime_manifest_v2.py",
        "group_count": len(groups),
        "groups": groups,
        "note": (
            "Forward-v2 runtime manifest. Immutable 12-group v1 base plus "
            "ordered wave deltas. Historical membership roots stay unchanged."
        ),
        "owns_families": 0,
        "schema_version": 2,
        "status": STATUS,
        "v1_base": {
            "file_sha256": v1_hash,
            "group_count": BASE_GROUP_COUNT,
            "path": census.relative(V1_PATH),
            "status": base.get("status"),
        },
    }


def build() -> dict[str, Any]:
    return compose()
