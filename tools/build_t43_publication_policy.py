#!/usr/bin/env python3
"""Write the schema-validated T43 datapack publication-policy after the winner."""
from __future__ import annotations

import json
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35  # noqa: E402
from tools import t43_common as t43  # noqa: E402
from tools.recipe_bulk.membership import membership_root  # noqa: E402

OUTPUT = t43.PUBLICATION_POLICY_DATAPACK_FILE


def build() -> dict[str, Any]:
    strategy = t43.production_strategy()
    if strategy["blocked"]:
        raise ValueError(
            "T43 publication policy cannot be written while the production winner is BLOCKED"
        )
    winner = strategy["group_winners"]["stone"]
    if winner not in {"immediate", "on_demand", "hybrid"}:
        raise ValueError(f"unknown T43 stone winner: {winner}")
    families = [
        json.loads(path.read_text(encoding="utf-8"))
        for path in t43.generated_family_files()
    ]
    family_ids = [str(family["family_id"]) for family in families]
    stable_ids = [
        str(relation["stable_id"])
        for family in families
        for relation in family.get("relations") or []
    ]
    membership = membership_root(family_ids, stable_ids)
    partition = t43.partition_for_winner(winner, "stone")
    cache = int(partition["lazy_cache_ceiling_rows"] or 0)
    eager = (
        t43.hybrid_eager_stable_ids("stone") if winner == "hybrid" else []
    )
    if winner == "immediate":
        cache = 0
        eager = []
    document = {
        "type": "cruciblecraft:compact_publication_policy",
        "target_map": t43.TARGET_MAP,
        "publication_group": t43.STONE_GROUP,
        "policy_type": winner,
        "cache_ceiling": cache,
        "eager_stable_ids": eager,
        "routing_schema_version": "t39-shard-v1",
        "membership_root_sha256": membership,
        "family_count": t43.PRODUCTION_FAMILY_COUNT,
        "relation_count": t43.PRODUCTION_RELATION_COUNT,
    }
    schema = t35.load_json(t43.PUBLICATION_POLICY_SCHEMA)
    required = set(schema.get("required") or [])
    missing = sorted(required - set(document))
    if missing:
        raise ValueError(f"T43 publication policy missing required fields: {missing}")
    if document["policy_type"] not in (schema.get("properties") or {}).get(
        "policy_type", {}
    ).get("enum", ["immediate", "on_demand", "hybrid"]):
        raise ValueError("T43 publication policy_type is not in the schema enum")
    return document


def write() -> dict[str, Any]:
    document = build()
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    t35.write_stable(OUTPUT, document)
    return document


def check() -> list[str]:
    return t43.check_document(OUTPUT, build())


def main(argv: list[str] | None = None) -> int:
    args = t43.parse_write_check("Write T43 datapack publication policy", argv)
    try:
        if args.write:
            document = write()
            print(json.dumps({
                "path": t43.relative(OUTPUT),
                "policy_type": document["policy_type"],
                "cache_ceiling": document["cache_ceiling"],
                "eager_stable_ids": len(document["eager_stable_ids"]),
            }))
            return 0
        errors = check()
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        print(f"{t43.relative(OUTPUT)} is current")
        return 0
    except ValueError as error:
        print(f"T43 publication policy failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
