#!/usr/bin/env python3
"""Build the T19 cover denominator from the fixed T13 canonical artifact."""
from __future__ import annotations

import argparse
import hashlib
import json
import sys
from collections import Counter
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))
from tools import t35_common as t35
TOOLS = ROOT / "tools"
POLICY = TOOLS / "t19_cover_denominator_policy.json"
OUTPUT = TOOLS / "t19_cover_denominator.json"


def load(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def stable(value: Any) -> str:
    return json.dumps(
        value, ensure_ascii=False, indent=2, sort_keys=True
    ) + "\n"


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def validate_policy(policy: dict[str, Any]) -> None:
    if (
        policy.get("schema_version") != 1
        or policy.get("status") != "T19_COVER_DENOMINATOR_POLICY"
    ):
        raise ValueError("T19 cover denominator policy header drifted")
    expected = policy.get("expected_counts") or {}
    if expected != {
        "canonical": 47,
        "implemented": 4,
        "selected_t19": 5,
        "deferred_with_reason": 28,
        "out_of_scope": 10,
        "unclassified": 0,
    }:
        raise ValueError("T19 expected cover counts drifted")
    if len(policy.get("implemented") or {}) != 4:
        raise ValueError("T19 implemented mapping must contain four kinds")
    if len(policy.get("selected") or {}) != 5:
        raise ValueError("T19 selected mapping must contain five kinds")


def build(policy: dict[str, Any] | None = None) -> dict[str, Any]:
    selected_policy = load(POLICY) if policy is None else policy
    validate_policy(selected_policy)
    source_path = ROOT / selected_policy["source_artifact"]
    source = load(source_path)
    rows = source.get("canonical_kinds") or []
    statuses = Counter(row.get("implementation_status") for row in rows)
    counts = {
        "canonical": len(rows),
        "implemented": statuses["implemented"],
        "selected_t19": statuses["selected_t19"],
        "deferred_with_reason": statuses["deferred_with_reason"],
        "out_of_scope": statuses["out_of_scope"],
        "unclassified": source.get("counts", {}).get("unclassified"),
    }
    if counts != selected_policy["expected_counts"]:
        raise ValueError(f"T13 cover classification drifted: {counts}")
    by_id = {row["canonical_id"]: row for row in rows}
    if len(by_id) != len(rows):
        raise ValueError("T13 canonical cover ids are not unique")
    implemented = set(selected_policy["implemented"])
    selected = set(selected_policy["selected"])
    actual_implemented = {
        row["canonical_id"]
        for row in rows
        if row["implementation_status"] == "implemented"
    }
    actual_selected = {
        row["canonical_id"]
        for row in rows
        if row["implementation_status"] == "selected_t19"
    }
    if implemented != actual_implemented or selected != actual_selected:
        raise ValueError("T19 expected sets do not match T13")
    for row in rows:
        status = row["implementation_status"]
        deferred = row.get("deferred")
        if status == "deferred_with_reason":
            required = {
                "reason",
                "owner",
                "replacement_condition",
                "recheck_point",
            }
            if not deferred or not all(deferred.get(key) for key in required):
                raise ValueError(
                    f"{row['canonical_id']}: incomplete deferred contract"
                )

    definitions_path = ROOT / selected_policy["definition_catalog"]
    schema_path = ROOT / selected_policy["definition_schema"]
    catalog = load(definitions_path)
    definitions = {
        row["id"]: row for row in catalog.get("definitions") or []
    }
    if catalog.get("schemaVersion") != 1 or len(definitions) != 9:
        raise ValueError("cover definition catalog shape drifted")
    mapped_definitions = set(selected_policy["implemented"].values()) | {
        row["definition"]
        for row in selected_policy["selected"].values()
    }
    if not mapped_definitions <= set(definitions):
        raise ValueError("T19 mapped definition is missing from the catalog")
    reuse = selected_policy["pure_json_reuse"]
    first = definitions.get(reuse["base"]) or {}
    second = definitions.get(reuse["second"]) or {}
    if (
        first.get("behavior") != reuse["required_behavior"]
        or second.get("behavior") != reuse["required_behavior"]
        or first.get("values") == second.get("values")
    ):
        raise ValueError("second same-behavior definition is not pure JSON")

    projected_rows = []
    for row in rows:
        canonical_id = row["canonical_id"]
        status = row["implementation_status"]
        mapped = selected_policy["implemented"].get(canonical_id)
        if status == "selected_t19":
            mapped = selected_policy["selected"][canonical_id]["definition"]
        projected_rows.append(
            {
                "canonical_id": canonical_id,
                "classification": status,
                "definition": mapped,
                "deferred": row.get("deferred"),
                "source_revision": source["source"]["revision"],
                "source_blob_members": [
                    raw["source_blob"]
                    for raw in source["raw_records"]
                    if raw.get("canonical_id") == canonical_id
                ],
            }
        )
    return {
        "schema_version": 1,
        "status": "T19_COVER_DENOMINATOR_READY",
        "counts": counts,
        "rows": projected_rows,
        "selected": selected_policy["selected"],
        "pure_json_reuse": {
            **reuse,
            "base_rate": first["values"]["rate"],
            "second_rate": second["values"]["rate"],
            "java_plugin_delta": 0,
        },
        "currentness": {
            "owned_inputs": [
                POLICY.relative_to(ROOT).as_posix(),
                Path(__file__).resolve().relative_to(ROOT).as_posix(),
            ],
            "source_artifact": {
                "path": source_path.relative_to(ROOT).as_posix(),
                "sha256": sha256(source_path),
            },
            "definition_catalog": {
                "path": definitions_path.relative_to(ROOT).as_posix(),
                "sha256": sha256(definitions_path),
            },
            "definition_schema": {
                "path": schema_path.relative_to(ROOT).as_posix(),
                "sha256": sha256(schema_path),
            },
        },
    }


def check(document: dict[str, Any] | None = None) -> list[str]:
    rebuilt = build() if document is None else document
    return t35.check_compact(OUTPUT, rebuilt, encode=stable)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--write", action="store_true")
    args = parser.parse_args()
    document = build()
    if args.check:
        errors = check(document)
        if errors:
            print("\n".join(errors))
            return 1
        print("T19 cover denominator is current")
        return 0
    if args.write:
        OUTPUT.write_bytes(stable(document).encode("utf-8"))
        print(f"wrote {OUTPUT.relative_to(ROOT).as_posix()}")
        return 0
    print(stable(document), end="")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
