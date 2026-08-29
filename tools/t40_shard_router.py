#!/usr/bin/env python3
"""Python twin of CompactRecipeShardRouter (t40-shard-v1)."""
from __future__ import annotations

import hashlib
from collections import defaultdict
from typing import Any, Iterable

ROUTING_SCHEMA_VERSION = "t40-shard-v1"
HARD_SHARD_CEILING = 128
OVERFLOW_ROUTE_KEY = "overflow"
PAIR_SEPARATOR = "\0"


def canonical_item(item_id: str) -> str:
    return f"item:{item_id}"


def canonical_fluid(fluid_id: str) -> str:
    return f"fluid:{fluid_id}"


def extract_index_keys(relation: dict[str, Any]) -> tuple[list[str], bool]:
    keys: set[str] = set()
    unindexed = False
    for ingredient in relation.get("item_inputs") or []:
        if not isinstance(ingredient, dict):
            unindexed = True
            continue
        if "tag" in ingredient or ingredient.get("type") not in (None, "minecraft:item"):
            if ingredient.get("item"):
                keys.add(canonical_item(str(ingredient["item"])))
            else:
                unindexed = True
            continue
        item_id = ingredient.get("item")
        if not item_id:
            unindexed = True
            continue
        keys.add(canonical_item(str(item_id)))
    for stack in relation.get("fluid_inputs") or []:
        if isinstance(stack, dict) and stack.get("id"):
            keys.add(canonical_fluid(str(stack["id"])))
    ordered = sorted(keys)
    if not ordered:
        unindexed = True
    return ordered, unindexed


def route_tuples(sorted_keys: list[str]) -> list[tuple[str, int, tuple[str, ...]]]:
    tuples: list[tuple[str, int, tuple[str, ...]]] = []
    for key in sorted_keys:
        tuples.append((key, 1, (key,)))
    for left in range(len(sorted_keys)):
        for right in range(left + 1, len(sorted_keys)):
            encoded = sorted_keys[left] + PAIR_SEPARATOR + sorted_keys[right]
            tuples.append((encoded, 2, (sorted_keys[left], sorted_keys[right])))
    return tuples


def shard_id(
        target_map: str,
        publication_group: str,
        route_key: str,
) -> str:
    payload = (
        f"{target_map}\n{publication_group}\n{ROUTING_SCHEMA_VERSION}\n{route_key}\n"
    )
    return hashlib.sha256(payload.encode("utf-8")).hexdigest()


def route_group(
        target_map: str,
        publication_group: str,
        relations: Iterable[dict[str, Any]],
) -> dict[str, Any]:
    ordered = sorted(
        relations,
        key=lambda relation: (
            int(relation.get("shadow_order") or 0),
            str(relation["stable_id"]),
        ),
    )
    keys_by_id: dict[str, tuple[list[str], bool]] = {}
    for relation in ordered:
        stable_id = str(relation["stable_id"])
        if stable_id in keys_by_id:
            raise ValueError(f"duplicate compact stable id in shard router: {stable_id}")
        keys_by_id[stable_id] = extract_index_keys(relation)

    posting_frequencies: dict[str, int] = defaultdict(int)
    for keys, unindexed in keys_by_id.values():
        if unindexed:
            continue
        for encoded, _length, _members in route_tuples(keys):
            posting_frequencies[encoded] += 1

    overflow_id = shard_id(target_map, publication_group, OVERFLOW_ROUTE_KEY)
    members_by_shard: dict[str, list[str]] = defaultdict(list)
    route_by_relation: dict[str, str] = {}
    shard_by_relation: dict[str, str] = {}
    route_key_by_shard: dict[str, str] = {}
    overflow_count = 0
    for relation in ordered:
        stable_id = str(relation["stable_id"])
        keys, unindexed = keys_by_id[stable_id]
        if unindexed:
            route_key = OVERFLOW_ROUTE_KEY
            overflow_count += 1
        else:
            selected = min(
                route_tuples(keys),
                key=lambda tuple_row: (
                    posting_frequencies[tuple_row[0]],
                    tuple_row[1],
                    tuple_row[0],
                ),
            )
            route_key = selected[0]
        relation_shard = (
            overflow_id if route_key == OVERFLOW_ROUTE_KEY
            else shard_id(target_map, publication_group, route_key)
        )
        previous = route_key_by_shard.get(relation_shard)
        if previous is not None and previous != route_key:
            raise ValueError(
                f"compact shard id collision between {previous} and {route_key}"
            )
        route_key_by_shard[relation_shard] = route_key
        members_by_shard[relation_shard].append(stable_id)
        shard_by_relation[stable_id] = relation_shard
        route_by_relation[stable_id] = route_key

    for shard, members in members_by_shard.items():
        if len(members) > HARD_SHARD_CEILING:
            raise ValueError(
                f"compact shard {shard} contains {len(members)} relations, "
                f"exceeding hard ceiling {HARD_SHARD_CEILING}"
            )
    if overflow_count > HARD_SHARD_CEILING:
        raise ValueError(
            f"compact overflow shard contains {overflow_count} relations, "
            f"exceeding hard ceiling {HARD_SHARD_CEILING}"
        )

    shards = []
    for shard, members in sorted(members_by_shard.items()):
        shards.append({
            "overflow": route_key_by_shard[shard] == OVERFLOW_ROUTE_KEY,
            "relation_count": len(members),
            "route_key": route_key_by_shard[shard],
            "shard_id": shard,
            "stable_ids": members,
        })
    worst = max((len(members) for members in members_by_shard.values()), default=0)
    return {
        "overflow_count": overflow_count,
        "overflow_shard_id": overflow_id,
        "publication_group": publication_group,
        "relations": {
            stable_id: {
                "route_key": route_by_relation[stable_id],
                "shard_id": shard_by_relation[stable_id],
            }
            for stable_id in sorted(shard_by_relation)
        },
        "shard_count": len(members_by_shard),
        "shards": shards,
        "target_map": target_map,
        "worst_shard_size": worst,
    }
