#!/usr/bin/env python3
"""Load the semantic ID map and remap live compact identities.

Active source must not embed historical milestone literals. The map is the
only place those keys are listed.
"""
from __future__ import annotations

import json
from functools import lru_cache
from pathlib import Path
from typing import Any

from tools import io_common as io

MAP_PATH = io.TOOLS / "semantic_id_map.json"


class SemanticIdError(ValueError):
    """Semantic ID map is missing or internally inconsistent."""


@lru_cache(maxsize=1)
def load_map() -> dict[str, Any]:
    if not MAP_PATH.is_file():
        raise SemanticIdError(f"missing semantic id map {io.relative(MAP_PATH)}")
    document = json.loads(MAP_PATH.read_text(encoding="utf-8"))
    if not isinstance(document, dict):
        raise SemanticIdError("semantic id map must be an object")
    if not document.get("publication_groups"):
        raise SemanticIdError("semantic id map missing publication_groups")
    return document


def publication_groups() -> dict[str, str]:
    raw = load_map().get("publication_groups") or {}
    return {str(key): str(value) for key, value in raw.items()}


def remap_publication_group(group_id: str) -> str:
    return publication_groups().get(group_id, group_id)


def routing_schema_new() -> str:
    return str((load_map().get("routing_schema") or {}).get("new") or "")


def routing_schema_old() -> tuple[str, ...]:
    raw = (load_map().get("routing_schema") or {}).get("old") or []
    return tuple(str(value) for value in raw)


def remap_routing_schema(version: str) -> str:
    if version in routing_schema_old():
        return routing_schema_new()
    return version


def remap_envelope(name: str) -> str:
    envelope = load_map().get("envelope") or {}
    if name == envelope.get("old"):
        return str(envelope.get("new") or name)
    return name


def _prefix_rows(key: str) -> list[dict[str, str]]:
    rows = load_map().get(key) or []
    return [dict(row) for row in rows if isinstance(row, dict)]


def remap_stable_id(stable_id: str, source_path: str = "") -> str:
    posix = source_path.replace("\\", "/")
    contextual = [
        row
        for row in _prefix_rows("stable_id_prefixes")
        if row.get("path_contains")
    ]
    generic = [
        row
        for row in _prefix_rows("stable_id_prefixes")
        if not row.get("path_contains")
    ]
    for row in contextual:
        needle = str(row.get("path_contains") or "")
        old = str(row.get("old") or "")
        new = str(row.get("new") or "")
        if needle and needle in posix and old and stable_id.startswith(old):
            return new + stable_id[len(old) :]
    for row in generic:
        old = str(row.get("old") or "")
        new = str(row.get("new") or "")
        if old and stable_id.startswith(old):
            return new + stable_id[len(old) :]
    return stable_id


def remap_recipe_path(rel_posix: str) -> str:
    for key in (
        "recipe_path_prefixes",
        "support_path_prefixes",
        "recipe_id_prefixes",
    ):
        for row in _prefix_rows(key):
            old = str(row.get("old") or "")
            new = str(row.get("new") or "")
            if old and old in rel_posix:
                return rel_posix.replace(old, new, 1)
    return rel_posix


def remap_owner(owner: str) -> str:
    return str((load_map().get("dedup_owners") or {}).get(owner, owner))


def inferred_publication_group(rel_posix: str) -> str | None:
    posix = rel_posix.replace("\\", "/")
    for prefix, group in (load_map().get("inferred_publication_groups") or {}).items():
        needle = f"/recipe/{prefix}/"
        if needle in posix or posix.endswith(f"/recipe/{prefix}"):
            return str(group)
    return None


def remap_policy_filename(name: str) -> str:
    return str((load_map().get("policy_filenames") or {}).get(name, name))


def remap_dedup_rule_id(rule_id: str) -> str:
    return str((load_map().get("dedup_rule_ids") or {}).get(rule_id, rule_id))


def remap_dedup_filename(name: str) -> str:
    return str((load_map().get("dedup_filenames") or {}).get(name, name))


def remap_policy_resource(path: str) -> str:
    posix = path.replace("\\", "/")
    for old, new in (load_map().get("policy_resource_roots") or {}).items():
        if posix.startswith(str(old)):
            posix = str(new) + posix[len(str(old)) :]
            break
    parent, slash, name = posix.rpartition("/")
    if slash:
        return f"{parent}/{remap_policy_filename(name)}"
    return remap_policy_filename(posix)


def live_generated_roots() -> tuple[Path, ...]:
    rows = load_map().get("live_generated_roots") or []
    return tuple(io.ROOT / str(row) for row in rows)


def remap_json_value(value: Any, source_path: str = "") -> Any:
    if isinstance(value, str):
        remapped = remap_publication_group(value)
        remapped = remap_stable_id(remapped, source_path)
        remapped = remap_dedup_rule_id(remapped)
        remapped = remap_owner(remapped)
        remapped = remap_routing_schema(remapped)
        remapped = remap_envelope(remapped)
        remapped = remap_recipe_path(remapped)
        return remapped
    if isinstance(value, list):
        return [remap_json_value(item, source_path) for item in value]
    if isinstance(value, dict):
        return {
            key: remap_json_value(item, source_path)
            for key, item in value.items()
        }
    return value


def remap_runtime_group(row: dict[str, Any]) -> dict[str, Any]:
    remapped = dict(row)
    remapped["publication_group"] = remap_publication_group(
        str(row.get("publication_group") or "")
    )
    remapped["eager_stable_ids"] = [
        remap_stable_id(str(item))
        for item in (row.get("eager_stable_ids") or [])
    ]
    if row.get("policy_resource"):
        remapped["policy_resource"] = remap_policy_resource(
            str(row["policy_resource"])
        )
    remapped.pop("wave_id", None)
    return remapped
