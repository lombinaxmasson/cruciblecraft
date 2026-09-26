#!/usr/bin/python3
"""Datapack compact publication policies for generic import compiles.

Historical machine builders write their own policy files. A generic
``recipe_import`` compile only emits family JSON, and the loader rejects a
compact group that has no policy. ``basic_batch`` cohorts have no per-wave
builder, so the shared compiler writes the policy next to the families.
"""
from __future__ import annotations

from pathlib import Path
from typing import Any

from tools import census_common as census
from tools.recipe_bulk.matrix import authored_relations
from tools.recipe_bulk.membership import membership_root

POLICY_DIR = (
    census.ROOT
    / "src/recipe_generated/resources/data/cruciblecraft/recipe/publication_policy"
)


def needs_policy(spec: Any) -> bool:
    return getattr(spec, "cohort", None) == "basic_batch"


def expected_policies(
    planned: list[tuple[Path, dict[str, Any]]],
    target_map: str,
) -> dict[str, dict[str, Any]]:
    grouped: dict[str, dict[str, Any]] = {}
    for _path, document in planned:
        group_id = str(document.get("publication_group") or "")
        if not group_id:
            continue
        relations = authored_relations(document)
        bucket = grouped.setdefault(
            group_id,
            {"family_ids": [], "stable_ids": [], "relation_count": 0},
        )
        bucket["family_ids"].append(str(document.get("family_id") or ""))
        bucket["stable_ids"].extend(
            str(relation.get("stable_id") or "") for relation in relations
        )
        bucket["relation_count"] += len(relations)
    policies: dict[str, dict[str, Any]] = {}
    for group_id, bucket in sorted(grouped.items()):
        policies[group_id] = {
            "cache_ceiling": 0,
            "eager_stable_ids": [],
            "family_count": len(bucket["family_ids"]),
            "membership_root_sha256": membership_root(
                bucket["family_ids"], bucket["stable_ids"]
            ),
            "policy_type": "immediate",
            "publication_group": group_id,
            "relation_count": bucket["relation_count"],
            "routing_schema_version": "compact-shard-v1",
            "target_map": target_map,
            "type": "cruciblecraft:compact_publication_policy",
        }
    return policies


def policy_path(group_id: str) -> Path:
    name = group_id.split(":", 1)[-1].replace("/", "_") + ".json"
    return POLICY_DIR / name


def existing_policy_path(group_id: str) -> Path | None:
    if not POLICY_DIR.is_dir():
        return None
    for path in sorted(POLICY_DIR.glob("*.json")):
        document = census.load_json(path)
        if str(document.get("publication_group") or "") == group_id:
            return path
    return None


def check_policies(
    planned: list[tuple[Path, dict[str, Any]]],
    target_map: str,
) -> list[str]:
    errors: list[str] = []
    for group_id, expected in expected_policies(planned, target_map).items():
        path = existing_policy_path(group_id)
        if path is None:
            errors.append(f"missing publication policy {group_id}")
            continue
        if census.load_json(path) != expected:
            errors.append(f"publication policy drifted for {group_id}")
    return errors


def write_policies(
    planned: list[tuple[Path, dict[str, Any]]],
    target_map: str,
) -> None:
    for group_id, expected in expected_policies(planned, target_map).items():
        path = existing_policy_path(group_id) or policy_path(group_id)
        census.write_stable(path, expected)
