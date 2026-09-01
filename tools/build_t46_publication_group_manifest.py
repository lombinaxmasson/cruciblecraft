#!/usr/bin/env python3
"""Freeze T46 publication-group membership from generated compact families."""
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

from tools import t35_common as t35
from tools import t46_common as common


def load_generated_families() -> list[dict[str, Any]]:
    files = common.generated_family_files()
    expected = common.production_family_count()
    if len(files) != expected:
        raise ValueError(
            f"T46 generated family count drifted: {len(files)} != {expected}"
        )
    return [json.loads(path.read_text(encoding="utf-8")) for path in files]


def build() -> dict[str, Any]:
    families = load_generated_families()
    grouped: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for family in families:
        group_id = str(family.get("publication_group") or "")
        if group_id not in common.PUBLICATION_GROUPS:
            raise ValueError(f"unexpected T46 publication_group: {group_id}")
        grouped[group_id].append(family)
    groups = []
    for group_id in common.PUBLICATION_GROUPS:
        rows = grouped.get(group_id) or []
        family_ids = sorted(str(row["family_id"]) for row in rows)
        stable_ids = sorted(
            str(relation["stable_id"])
            for row in rows
            for relation in row.get("relations") or []
        )
        host = common.TARGET_MAP
        groups.append({
            "family_count": len(family_ids),
            "family_ids": family_ids,
            "membership_root_sha256": common.membership_root_sha256(family_ids, stable_ids),
            "publication_group": group_id,
            "relation_count": len(stable_ids),
            "stable_ids": stable_ids,
            "target_map": host,
        })
    if sum(group["family_count"] for group in groups) != common.production_family_count():
        raise ValueError("T46 publication group family count drifted")
    document = {
        "group_count": len(groups),
        "groups": groups,
        "schema_version": 1,
        "status": "T46_PRODUCTION_PUBLICATION_GROUPS_FROZEN",
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
        "Freeze T46 publication-group membership.", argv
    )
    if args.check:
        from tools import closeout_seal

        errors = closeout_seal.live_or_sealed_errors(
            "T46",
            "publication_group_manifest",
            lambda: common.check_document(
                common.PUBLICATION_GROUP_MANIFEST, build()
            ),
        )
        if errors:
            print("T46 publication-group manifest is stale:")
            for error in errors:
                print(f"- {error}")
            return 1
        print("T46 publication-group manifest is current.")
        return 0
    document = build()
    t35.write_stable(common.PUBLICATION_GROUP_MANIFEST, document)
    print(
        "Wrote T46 publication-group manifest "
        f"({document['group_count']} groups)."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
