#!/usr/bin/env python3
"""Write schema-validated T49 datapack publication policies after the winner."""
from __future__ import annotations

import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35
from tools import wave_bath_tiny_purified as tiny_purified


def _policy_document(group_row: dict[str, Any], winner: str) -> dict[str, Any]:
    group_key = tiny_purified.PUBLICATION_GROUP_KEYS[group_row["publication_group"]]
    partition = tiny_purified.partition_for_winner(winner, group_key)
    cache = int(partition["lazy_cache_ceiling_rows"] or 0)
    eager = tiny_purified.hybrid_eager_stable_ids(group_key) if winner == "hybrid" else []
    if winner == "immediate":
        cache = 0
        eager = []
    membership = group_row.get("membership_root_sha256")
    if not isinstance(membership, str) or len(membership) != 64:
        raise ValueError(
            f"T49 membership root missing for {group_row['publication_group']}"
        )
    document = {
        "type": "cruciblecraft:compact_publication_policy",
        "target_map": group_row["target_map"],
        "publication_group": group_row["publication_group"],
        "policy_type": winner,
        "cache_ceiling": cache,
        "eager_stable_ids": eager,
        "routing_schema_version": "t39-shard-v1",
        "membership_root_sha256": membership,
        "family_count": group_row["family_count"],
        "relation_count": group_row["relation_count"],
    }
    schema = t35.load_json(tiny_purified.PUBLICATION_POLICY_SCHEMA)
    required = set(schema.get("required") or [])
    missing = sorted(required - set(document))
    if missing:
        raise ValueError(f"T49 publication policy missing required fields: {missing}")
    return document


def build() -> dict[str, dict[str, Any]]:
    strategy = tiny_purified.production_strategy()
    if strategy["blocked"]:
        raise ValueError(
            "T49 publication policy cannot be written while the production winner is BLOCKED"
        )
    group = t35.load_json(tiny_purified.PUBLICATION_GROUP_MANIFEST)
    documents: dict[str, dict[str, Any]] = {}
    for row in group.get("groups") or []:
        group_id = str(row["publication_group"])
        winners = strategy["group_winners"]
        winner = winners.get(group_id) or winners.get(tiny_purified.PUBLICATION_GROUP_KEYS[group_id])
        if winner not in {"immediate", "on_demand", "hybrid"}:
            raise ValueError(f"unknown T49 winner for {group_id}: {winner}")
        documents[group_id] = _policy_document(row, winner)
    if set(documents) != set(tiny_purified.PUBLICATION_GROUPS):
        raise ValueError("T49 publication policy groups drifted")
    return documents


def write() -> dict[str, dict[str, Any]]:
    documents = build()
    tiny_purified.PUBLICATION_POLICY_DATAPACK.mkdir(parents=True, exist_ok=True)
    for group_id, document in documents.items():
        t35.write_stable(tiny_purified.PUBLICATION_POLICY_DATAPACK_FILES[group_id], document)
    return documents


def check() -> list[str]:
    errors: list[str] = []
    documents = build()
    for group_id, document in documents.items():
        errors.extend(
            tiny_purified.check_document(tiny_purified.PUBLICATION_POLICY_DATAPACK_FILES[group_id], document)
        )
    return errors


def main(argv: list[str] | None = None) -> int:
    args = tiny_purified.parse_managed("Write T49 datapack publication policy", argv)
    try:
        if args.write:
            write()
            print("Wrote T49 datapack publication policies")
            return 0
        errors = check()
        if errors:
            print("T49 publication policy is stale:")
            for error in errors:
                print(f"- {error}")
            return 1
        print("T49 publication policy is current.")
        return 0
    except ValueError as failure:
        print(str(failure), file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
