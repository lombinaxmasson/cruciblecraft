#!/usr/bin/env python3
"""Freeze T40 query-addressable shard membership from generated families."""
from __future__ import annotations

import hashlib
import json
import sys
from collections import defaultdict
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35  # noqa: E402
from tools import t40_common as common  # noqa: E402
from tools import t40_shard_router as router  # noqa: E402


def load_generated_families() -> list[dict[str, Any]]:
    files = common.generated_family_files()
    expected = common.production_family_count()
    if len(files) != expected:
        raise ValueError(
            f"T40 generated family count drifted: {len(files)} != {expected}"
        )
    return [json.loads(path.read_text(encoding="utf-8")) for path in files]


def build() -> dict[str, Any]:
    families = load_generated_families()
    grouped: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for family in families:
        group_id = str(family.get("publication_group") or "")
        if group_id not in {common.SINGLETON_GROUP, common.MULTI_GROUP}:
            raise ValueError(f"unexpected T40 publication_group: {group_id}")
        if str(family.get("target_map") or "") != common.TARGET_MAP:
            raise ValueError(f"T40 generated target_map drifted: {family.get('family_id')}")
        grouped[group_id].extend(family.get("relations") or [])

    groups = []
    all_stable_ids: set[str] = set()
    for group_id in (common.SINGLETON_GROUP, common.MULTI_GROUP):
        relations = grouped.get(group_id) or []
        routed = router.route_group(common.TARGET_MAP, group_id, relations)
        for stable_id in routed["relations"]:
            if stable_id in all_stable_ids:
                raise ValueError(f"duplicate T40 stable id across shards: {stable_id}")
            all_stable_ids.add(stable_id)
        groups.append({
            "overflow_count": routed["overflow_count"],
            "overflow_shard_id": routed["overflow_shard_id"],
            "publication_group": group_id,
            "relation_count": len(routed["relations"]),
            "relations": routed["relations"],
            "routing_schema_version": router.ROUTING_SCHEMA_VERSION,
            "shard_count": routed["shard_count"],
            "shards": routed["shards"],
            "target_map": common.TARGET_MAP,
            "worst_shard_size": routed["worst_shard_size"],
        })
    expected_relations = common.production_relation_count()
    if len(all_stable_ids) != expected_relations:
        raise ValueError(
            f"T40 shard membership drifted: {len(all_stable_ids)} != "
            f"{expected_relations}"
        )
    for group in groups:
        if group["worst_shard_size"] > router.HARD_SHARD_CEILING:
            raise ValueError("T40 shard exceeded hard ceiling")
        if group["overflow_count"] > router.HARD_SHARD_CEILING:
            raise ValueError("T40 overflow shard exceeded hard ceiling")
    document = {
        "group_count": len(groups),
        "groups": groups,
        "hard_ceiling": router.HARD_SHARD_CEILING,
        "relation_count": len(all_stable_ids),
        "routing_schema_version": router.ROUTING_SCHEMA_VERSION,
        "schema_version": 1,
        "shard_count": sum(group["shard_count"] for group in groups),
        "status": "T40_PRODUCTION_SHARDS_FROZEN",
        "target_map": common.TARGET_MAP,
        "production_lock_sha256": common.production_lock_sha256(),
    }
    document["aggregate_root_sha256"] = hashlib.sha256(
        t35.stable_json(document).encode("utf-8")
    ).hexdigest()
    return document


def main(argv: list[str] | None = None) -> int:
    args = common.parse_write_check("Freeze T40 query-addressable shards.", argv)
    document = build()
    if args.check:
        errors = common.check_document(common.SHARD_MANIFEST, document)
        if errors:
            print("T40 shard manifest is stale:")
            for error in errors:
                print(f"- {error}")
            return 1
        print(
            "T40 shard manifest is current "
            f"({document['shard_count']} shards / {document['relation_count']} relations)."
        )
        return 0
    common.SHARD_MANIFEST.write_text(
        t35.stable_json(document), encoding="utf-8", newline="\n"
    )
    print(
        "Wrote T40 shard manifest "
        f"({document['shard_count']} shards / {document['relation_count']} relations)."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
