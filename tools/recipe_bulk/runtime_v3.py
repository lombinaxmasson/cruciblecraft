#!/usr/bin/python3
"""Compose frozen v2 compact runtime manifest with ordered semantic-slug deltas."""
from __future__ import annotations

import copy
import hashlib
from pathlib import Path
from typing import Any

from tools import census_common as census
from tools.recipe_bulk import runtime_v2
from tools.recipe_bulk.membership import identity_semantic_root
from tools.recipe_bulk.slugs import parse_wave_token

STATUS = "COMPACT_RECIPE_RUNTIME_MANIFEST_V3"
V2_PATH = census.TOOLS / "compact_recipe_runtime_manifest.v2.json"
OUTPUT = census.TOOLS / "compact_recipe_runtime_manifest.v3.json"
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
    slug: census.TOOLS / "waves" / slug / "runtime_manifest_delta.json"
    for slug in DELTA_ORDER
}


class RuntimeV3ConflictError(ValueError):
    """Semantic-v3 runtime composition failed closed."""


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


def _empty_alias_root() -> str:
    return identity_semantic_root([])


def _composition_root(
    v2_file_sha256: str,
    alias_root: str,
    deltas: list[dict[str, Any]],
) -> str:
    payload = (
        f"{v2_file_sha256}\t{alias_root}\t"
        + census.stable_json(deltas)
    )
    return hashlib.sha256(payload.encode("utf-8")).hexdigest()


def load_delta(wave_slug: str) -> dict[str, Any]:
    parse_wave_token(wave_slug, schema="semantic-v3")
    path = DELTA_PATHS.get(wave_slug)
    if path is None or not path.is_file():
        return {
            "groups": [],
            "order": wave_slug,
            "schema_version": 1,
            "status": f"{wave_slug.replace('/', '_').upper()}_RUNTIME_MANIFEST_DELTA",
            "wave_slug": wave_slug,
        }
    document = census.load_json(path)
    if document.get("wave_id") is not None:
        raise RuntimeV3ConflictError(
            f"{path} must not write wave_id; use wave_slug only"
        )
    if str(document.get("wave_slug") or "") != wave_slug:
        raise RuntimeV3ConflictError(f"{path} wave_slug drifted")
    return document


_COMPOSE_CACHE: tuple[str, dict[str, Any]] | None = None


def _cache_key() -> str:
    return _file_hash(V2_PATH) + "".join(
        _file_hash(path) for path in DELTA_PATHS.values()
    )


def compose(v2: dict[str, Any] | None = None) -> dict[str, Any]:
    global _COMPOSE_CACHE
    if v2 is None and _COMPOSE_CACHE is not None and _COMPOSE_CACHE[0] == _cache_key():
        return _COMPOSE_CACHE[1]
    payload = _compose(v2)
    if v2 is None:
        _COMPOSE_CACHE = (_cache_key(), payload)
    return payload


def _compose(v2: dict[str, Any] | None = None) -> dict[str, Any]:
    if not V2_PATH.is_file():
        raise RuntimeV3ConflictError(f"missing frozen v2 runtime manifest {V2_PATH}")
    v2_hash = _file_hash(V2_PATH)
    base = v2 if v2 is not None else census.load_json(V2_PATH)
    if base.get("status") != runtime_v2.STATUS:
        raise RuntimeV3ConflictError("v2 runtime manifest status drifted")
    from tools import semantic_ids

    groups = [
        semantic_ids.remap_runtime_group(row)
        for row in copy.deepcopy(list(base.get("groups") or []))
    ]
    for row in groups:
        policy_path = census.ROOT / str(row.get("policy_resource") or "")
        if policy_path.is_file():
            policy = census.load_json(policy_path)
            if policy.get("membership_root_sha256"):
                row["membership_root_sha256"] = policy["membership_root_sha256"]
            if policy.get("eager_stable_ids") is not None:
                row["eager_stable_ids"] = list(policy.get("eager_stable_ids") or [])
    remapped_rules = []
    for row in copy.deepcopy(list(base.get("dedup_rules") or [])):
        remapped = dict(row)
        if remapped.get("rule_id"):
            remapped["rule_id"] = semantic_ids.remap_dedup_rule_id(
                str(remapped["rule_id"])
            )
        if remapped.get("resource"):
            remapped["resource"] = semantic_ids.remap_recipe_path(
                semantic_ids.remap_policy_resource(str(remapped["resource"]))
            )
            name = Path(str(remapped["resource"])).name
            remapped["resource"] = str(Path(remapped["resource"]).with_name(
                semantic_ids.remap_dedup_filename(name)
            )).replace("\\", "/")
        remapped.pop("wave_id", None)
        if remapped.get("owner"):
            remapped["owner"] = semantic_ids.remap_owner(str(remapped["owner"]))
        remapped_rules.append(remapped)
    dedup_rules = remapped_rules
    historical_roots = {
        str(row["publication_group"]): str(row["membership_root_sha256"])
        for row in groups
    }
    seen = set(historical_roots)
    consumed: list[dict[str, Any]] = []
    for wave_slug in DELTA_ORDER:
        delta = load_delta(wave_slug)
        path = DELTA_PATHS.get(wave_slug)
        for row in delta.get("groups") or []:
            group_id = str(row.get("publication_group") or "")
            if not group_id:
                raise RuntimeV3ConflictError(
                    f"{wave_slug} delta group missing publication_group"
                )
            if group_id in seen:
                raise RuntimeV3ConflictError(
                    f"{wave_slug} duplicate group id {group_id}"
                )
            seen.add(group_id)
            groups.append(copy.deepcopy(row))
        for row in delta.get("dedup_rules") or []:
            rule_id = str(row.get("rule_id") or "")
            dedup_rules.append(copy.deepcopy(row))
        consumed.append(
            {
                "file_sha256": _file_hash(path) if path is not None else "",
                "order": str(delta.get("order") or wave_slug),
                "path": census.relative(path) if path is not None else "",
                "wave_slug": wave_slug,
            }
        )
    v2_logical = str((base.get("composition") or {}).get("semantic_root_sha256") or "")
    logical = _group_semantic_root(groups)
    alias_root = _empty_alias_root()
    empty = not DELTA_ORDER
    if empty and logical != v2_logical:
        raise RuntimeV3ConflictError(
            "empty v3 composition must preserve frozen v2 logical identities"
        )
    return {
        "bindings": dict(base.get("bindings") or {}),
        "composition": {
            "base_group_count": int(base.get("group_count") or 0),
            "composed_group_count": len(groups),
            "conflict_check": "pass",
            "consumed_deltas": consumed,
            "logical_identities_equal_v2": empty and logical == v2_logical,
            "semantic_alias_root_sha256": alias_root,
            "semantic_root_sha256": _composition_root(v2_hash, alias_root, consumed),
            "v2_file_sha256": v2_hash,
            "v2_logical_identity_root_sha256": v2_logical,
        },
        "dedup_rules": dedup_rules,
        "generated_by": "python tools/build_compact_recipe_runtime_manifest_v3.py",
        "group_count": len(groups),
        "groups": groups,
        "note": (
            "Forward-v3 runtime manifest. Frozen v2 file hash plus semantic "
            "alias root plus ordered slug deltas. Does not rewrite v2."
        ),
        "owns_families": 0,
        "schema_version": 3,
        "status": STATUS,
        "v2_base": {
            "file_sha256": v2_hash,
            "group_count": int(base.get("group_count") or 0),
            "path": census.relative(V2_PATH),
            "status": base.get("status"),
        },
    }


def envelope_document(composed: dict[str, Any] | None = None) -> dict[str, Any]:
    payload = composed if composed is not None else compose()
    return {
        "composition": payload["composition"],
        "generated_by": payload["generated_by"],
        "group_count": payload["group_count"],
        "note": payload["note"],
        "owns_families": payload["owns_families"],
        "schema_version": 3,
        "status": STATUS,
        "v2_base": payload["v2_base"],
    }


def build() -> dict[str, Any]:
    return envelope_document()
