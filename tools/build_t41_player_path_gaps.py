#!/usr/bin/env python3
"""Summarize unreachable T41 assembler player-path inputs against T21 closure."""
from __future__ import annotations

import argparse
import json
import sys
from collections import Counter, defaultdict
from pathlib import Path
from typing import Any

_ROOT = Path(__file__).resolve().parents[1]
if str(_ROOT) not in sys.path:
    sys.path.insert(0, str(_ROOT))

from tools import t41_common as t41

ROOT = t41.ROOT
TOOLS = t41.TOOLS
PLAYER_PATH = t41.PLAYER_PATH
T21 = TOOLS / "t21_operand_reachability.json"
OUTPUT = t41.PLAYER_PATH_GAPS


def _load(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def _classify_consume(cid: str, reachable: set[str]) -> str:
    if cid.startswith("minecraft:"):
        return "vanilla_survival_missing_from_t21"
    if cid.startswith(t41.GT_WOOD_PREFIX):
        return "gt_wood_token"
    if cid.startswith("cruciblecraft:"):
        return "other_cc"
    return "other"


def build() -> dict[str, Any]:
    player_path = _load(PLAYER_PATH)
    t21 = _load(T21)
    reachable = set(t21["closure"]["reachable_identities"])

    consume_freq: Counter[str] = Counter()
    fluid_freq: Counter[str] = Counter()
    for row in player_path["rows"]:
        if row.get("inputs_reachable", True):
            continue
        consume_freq.update(row.get("consume_ids", []))
        fluid_freq.update(row.get("fluid_input_ids", []))

    taxonomy: dict[str, list[dict[str, object]]] = defaultdict(list)
    for cid, count in consume_freq.items():
        category = _classify_consume(cid, reachable)
        taxonomy[category].append(
            {
                "id": cid,
                "count": count,
                "in_t21_closure": f"item:{cid}" in reachable,
            }
        )
    for category in taxonomy:
        taxonomy[category].sort(key=lambda row: (-int(row["count"]), str(row["id"])))

    taxonomy_summary = {
        category: {
            "unique_ids": len(rows),
            "mention_count": sum(int(row["count"]) for row in rows),
        }
        for category, rows in sorted(taxonomy.items())
    }

    top20 = [
        {"id": cid, "count": count, "in_t21_closure": f"item:{cid}" in reachable}
        for cid, count in consume_freq.most_common(20)
    ]

    return {
        "schema_version": 1,
        "status": "T41_PLAYER_PATH_GAPS",
        "reachability_source": player_path.get("reachability_source"),
        "counts": {
            "relations": player_path["relations"],
            "inputs_reachable": player_path["inputs_reachable"],
            "relations_with_unreachable_inputs": player_path.get(
                "relations_with_unreachable_inputs",
                int(player_path["relations"]) - int(player_path["inputs_reachable"]),
            ),
            "unique_unreachable_consume_ids": len(consume_freq),
            "unique_unreachable_fluid_input_ids": len(fluid_freq),
            "unreachable_consume_mentions": sum(consume_freq.values()),
            "unreachable_fluid_mentions": sum(fluid_freq.values()),
        },
        "taxonomy_summary": taxonomy_summary,
        "top_consume_ids": top20,
        "taxonomy": {category: rows for category, rows in sorted(taxonomy.items())},
        "notes": {
            "support_builder": (
                "Unmatched GT woods and fireproof stacks are acquired by "
                "bounded oak_planks token recipes. T21 B0 does not scan "
                "src/t41_*_generated."
            ),
        },
    }


def _stable(document: dict[str, Any]) -> str:
    return json.dumps(document, ensure_ascii=False, indent=2, sort_keys=True) + "\n"


def write() -> dict[str, Any]:
    document = build()
    OUTPUT.write_text(_stable(document), encoding="utf-8")
    return document


def check() -> list[str]:
    document = build()
    if not OUTPUT.is_file() or OUTPUT.read_text(encoding="utf-8") != _stable(document):
        return [f"stale: {OUTPUT.relative_to(ROOT).as_posix()}"]
    return []


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    if args.write:
        document = write()
        print(json.dumps(document["counts"], sort_keys=True))
        print(f"Wrote {OUTPUT.relative_to(ROOT).as_posix()}")
        return 0
    if args.check:
        errors = check()
        if errors:
            print("\n".join(errors))
            return 1
        return 0
    print(json.dumps(build()["counts"], sort_keys=True, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
