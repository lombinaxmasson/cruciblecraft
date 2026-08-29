#!/usr/bin/env python3
"""Freeze T40 publication-group membership from generated compact families."""
from __future__ import annotations

import hashlib
import json
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35  # noqa: E402
from tools import t40_common as common  # noqa: E402


def load_generated_families() -> list[dict[str, Any]]:
    files = common.generated_family_files()
    expected = common.production_family_count()
    if len(files) != expected:
        raise ValueError(
            f"T40 generated family count drifted: {len(files)} != {expected}"
        )
    families = [json.loads(path.read_text(encoding="utf-8")) for path in files]
    return families


def build() -> dict[str, Any]:
    families = load_generated_families()
    grouped: dict[str, dict[str, Any]] = {}
    for family in families:
        group_id = str(family.get("publication_group") or "")
        if group_id not in {common.SINGLETON_GROUP, common.MULTI_GROUP}:
            raise ValueError(f"unexpected T40 publication_group: {group_id}")
        bucket = grouped.setdefault(
            group_id,
            {
                "family_ids": [],
                "publication_group": group_id,
                "stable_ids": [],
            },
        )
        bucket["family_ids"].append(str(family["family_id"]))
        for relation in family.get("relations") or []:
            bucket["stable_ids"].append(str(relation["stable_id"]))
    groups = []
    for group_id in (common.SINGLETON_GROUP, common.MULTI_GROUP):
        bucket = grouped.get(group_id)
        if bucket is None:
            raise ValueError(f"missing T40 publication group {group_id}")
        family_ids = sorted(bucket["family_ids"])
        stable_ids = sorted(bucket["stable_ids"])
        groups.append({
            "family_count": len(family_ids),
            "family_ids": family_ids,
            "publication_group": group_id,
            "relation_count": len(stable_ids),
            "stable_ids": stable_ids,
            "target_map": common.TARGET_MAP,
        })
    expected_groups = common.production_group_counts()
    if groups[0]["family_count"] != expected_groups[common.SINGLETON_GROUP]["families"]:
        raise ValueError("T40 singleton family count drifted")
    if groups[0]["relation_count"] != expected_groups[common.SINGLETON_GROUP]["relations"]:
        raise ValueError("T40 singleton relation count drifted")
    if groups[1]["family_count"] != expected_groups[common.MULTI_GROUP]["families"]:
        raise ValueError("T40 multi family count drifted")
    if groups[1]["relation_count"] != expected_groups[common.MULTI_GROUP]["relations"]:
        raise ValueError("T40 multi relation count drifted")
    document = {
        "group_count": len(groups),
        "groups": groups,
        "schema_version": 1,
        "status": "T40_PRODUCTION_PUBLICATION_GROUPS_FROZEN",
        "target_map": common.TARGET_MAP,
        "production_lock": {
            "path": common.relative(common.PRODUCTION_LOCK),
            "selection_sha256": common.load_production_lock()["production"][
                "selection_sha256"
            ],
            "sha256": common.production_lock_sha256(),
        },
    }
    document["aggregate_root_sha256"] = hashlib.sha256(
        t35.stable_json(document).encode("utf-8")
    ).hexdigest()
    return document


def main(argv: list[str] | None = None) -> int:
    args = common.parse_write_check(
        "Freeze T40 publication-group membership.", argv
    )
    document = build()
    if args.check:
        errors = common.check_document(common.PUBLICATION_GROUP_MANIFEST, document)
        if errors:
            print("T40 publication-group manifest is stale:")
            for error in errors:
                print(f"- {error}")
            return 1
        print("T40 publication-group manifest is current.")
        return 0
    common.PUBLICATION_GROUP_MANIFEST.write_text(
        t35.stable_json(document), encoding="utf-8", newline="\n"
    )
    print(
        "Wrote T40 publication-group manifest "
        f"({document['groups'][0]['family_count']}/"
        f"{document['groups'][1]['family_count']} families)."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
