#!/usr/bin/python3
"""Declarative source/lock selection, grouping, publication, and path layout."""
from __future__ import annotations

from collections import defaultdict
from typing import Any

from tools import t35_common as t35
from tools import t41_common as t41
from tools import t45_common as t45
from tools.recipe_bulk.emit import family_filename
from tools.recipe_bulk.models import WaveSpec


def load_source_relations(spec: WaveSpec) -> list[dict[str, Any]]:
    source = t35.load_json(spec.source_path)
    relations = list(source.get("relations") or [])
    if not relations:
        raise ValueError(f"{spec.wave_id} source has no relations")
    return relations


def lock_by_template(spec: WaveSpec) -> dict[str, dict[str, Any]]:
    if spec.lock_path is None:
        return {}
    lock = t35.load_json(spec.lock_path)
    families = (lock.get("production") or {}).get("families") or []
    return {str(row["template_key"]): row for row in families}


def select_source_relations(
    spec: WaveSpec,
) -> tuple[list[dict[str, Any]], dict[str, dict[str, Any]]]:
    relations = load_source_relations(spec)
    lock_rows = lock_by_template(spec)
    if spec.selection_policy == "all":
        return relations, lock_rows
    if spec.selection_policy == "exclude_combinatorial":
        excluded = set(spec.combinatorial_template_keys)
        selected = [
            relation
            for relation in relations
            if str(relation.get("template_key")) not in excluded
        ]
        return selected, lock_rows
    if spec.selection_policy == "lock_templates":
        if not lock_rows:
            raise ValueError(f"{spec.wave_id} lock selection requires a production lock")
        allowed = set(lock_rows)
        selected = [
            relation
            for relation in relations
            if str(relation.get("template_key")) in allowed
        ]
        missing = allowed - {str(row.get("template_key")) for row in selected}
        if missing:
            raise ValueError(
                f"{spec.wave_id} lock templates missing from source: "
                + ",".join(sorted(missing)[:8])
            )
        return selected, lock_rows
    raise ValueError(f"unsupported selection_policy {spec.selection_policy}")


def sort_group(spec: WaveSpec, rows: list[dict[str, Any]]) -> list[dict[str, Any]]:
    if spec.relation_sort == "source_recipe_index_then_stable_id":
        return sorted(
            rows,
            key=lambda row: (
                int(row.get("source_recipe_index") or 0),
                str(row.get("stable_id") or ""),
            ),
        )
    return sorted(rows, key=lambda row: str(row.get("template_key") or ""))


def group_relations(
    spec: WaveSpec,
    relations: list[dict[str, Any]],
) -> dict[str, list[dict[str, Any]]]:
    grouped: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for relation in relations:
        grouped[str(relation["template_key"])].append(relation)
    return {
        template_key: sort_group(spec, rows)
        for template_key, rows in grouped.items()
    }


def publication_group(
    spec: WaveSpec,
    *,
    lock_row: dict[str, Any] | None,
    relation: dict[str, Any] | None,
    expanded_count: int,
) -> str | None:
    if spec.publication_policy == "omit":
        return None
    if spec.publication_policy == "constant":
        return spec.default_publication_group
    if spec.publication_policy == "lock":
        if lock_row is None:
            raise ValueError(f"{spec.wave_id} lock publication group missing lock row")
        return str(lock_row["publication_group"])
    if spec.publication_policy == "expanded_count":
        if expanded_count == 1:
            if not spec.singleton_publication_group:
                raise ValueError(f"{spec.wave_id} missing singleton_publication_group")
            return spec.singleton_publication_group
        if expanded_count > 1:
            if not spec.multi_publication_group:
                raise ValueError(f"{spec.wave_id} missing multi_publication_group")
            return spec.multi_publication_group
        raise ValueError(f"{spec.wave_id} expanded_count must be positive")
    if spec.publication_policy == "relation":
        if relation is None:
            raise ValueError(f"{spec.wave_id} relation publication group missing relation")
        return str(
            relation.get("publication_group")
            or t41.publication_group_for_relation(relation)
        )
    raise ValueError(f"unsupported publication_policy {spec.publication_policy}")


def relative_path(
    spec: WaveSpec,
    template_key: str,
    lock_row: dict[str, Any] | None,
) -> str:
    filename = family_filename(template_key)
    if spec.path_layout == "flat":
        return filename
    if lock_row is None:
        raise ValueError(f"{spec.wave_id} host-nested path requires a lock row")
    host = str(lock_row["host"]).split(":", 1)[-1]
    return f"{host}/{filename}"


def target_map_for(
    spec: WaveSpec,
    *,
    lock_row: dict[str, Any] | None,
    relation: dict[str, Any] | None,
) -> str:
    if spec.target_map_policy == "spec":
        return spec.target_map
    if spec.target_map_policy == "relation":
        if relation is None:
            raise ValueError(f"{spec.wave_id} relation target_map missing relation")
        return str(relation.get("target_map") or spec.target_map)
    if spec.target_map_policy == "lock_host":
        if lock_row is None:
            raise ValueError(f"{spec.wave_id} lock-host target_map missing lock row")
        host = str(lock_row["host"])
        return str(t45.HOST_CONFIG[host]["target_map"])
    raise ValueError(f"unsupported target_map_policy {spec.target_map_policy}")


def source_revision_for(relation: dict[str, Any]) -> str:
    return str(relation.get("source_revision") or t35.SOURCE_REVISION)
