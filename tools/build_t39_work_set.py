#!/usr/bin/env python3
"""Freeze the T39 Centrifuge work set from the immutable T35 family ledger."""
from __future__ import annotations

import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35  # noqa: E402
from tools import t39_common as common  # noqa: E402

BUILDER = Path(__file__).resolve()
OUTPUT = common.WORK_SET


def build_work_set(families_doc: dict[str, Any] | None = None) -> dict[str, Any]:
    selected = common.select_centrifuge_families(families_doc)
    family_ids = [str(row["family_id"]) for row in selected]
    template_keys = [str(row["template_key"]) for row in selected]
    expanded_counts = [int(row["expanded_count"]) for row in selected]
    if len(family_ids) != common.FAMILY_COUNT:
        raise ValueError(
            f"T39 work set must contain {common.FAMILY_COUNT} families, "
            f"got {len(family_ids)}"
        )
    if len(set(family_ids)) != common.FAMILY_COUNT:
        raise ValueError("T39 work set has duplicate family ids")
    if len(set(template_keys)) != common.FAMILY_COUNT:
        raise ValueError("T39 work set has duplicate template keys")
    if template_keys != sorted(template_keys):
        raise ValueError("T39 template keys are not sorted ascending")
    source_rows = sum(expanded_counts)
    if source_rows != common.SOURCE_ROWS:
        raise ValueError(
            f"T39 expanded_count total must be {common.SOURCE_ROWS}, got {source_rows}"
        )
    digest = common.selection_sha256(family_ids)
    if digest != common.EXPECTED_SELECTION_SHA256:
        raise ValueError(
            "T39 selection_sha256 drifted: "
            f"computed {digest}, expected {common.EXPECTED_SELECTION_SHA256}"
        )
    distribution = common.expanded_count_distribution(expanded_counts)
    expected_distribution = {
        str(size): count for size, count in common.EXPECTED_DISTRIBUTION.items()
    }
    if distribution != expected_distribution:
        raise ValueError(
            f"T39 expanded_count distribution drifted: {distribution}"
        )
    families = []
    singleton_ids: list[str] = []
    multi_ids: list[str] = []
    singleton_rows = 0
    multi_rows = 0
    for row, expanded_count in zip(selected, expanded_counts):
        group = common.publication_group_for_expanded_count(expanded_count)
        family_id = str(row["family_id"])
        families.append(
            {
                "expanded_count": expanded_count,
                "family_id": family_id,
                "publication_group": group,
                "template_key": str(row["template_key"]),
            }
        )
        if group == common.SINGLETON_GROUP:
            singleton_ids.append(family_id)
            singleton_rows += expanded_count
        else:
            multi_ids.append(family_id)
            multi_rows += expanded_count
    if len(singleton_ids) != common.SINGLETON_FAMILIES:
        raise ValueError(
            f"T39 singleton families must be {common.SINGLETON_FAMILIES}, "
            f"got {len(singleton_ids)}"
        )
    if singleton_rows != common.SINGLETON_RELATIONS:
        raise ValueError(
            f"T39 singleton relations must be {common.SINGLETON_RELATIONS}, "
            f"got {singleton_rows}"
        )
    if len(multi_ids) != common.MULTI_FAMILIES:
        raise ValueError(
            f"T39 multi families must be {common.MULTI_FAMILIES}, "
            f"got {len(multi_ids)}"
        )
    if multi_rows != common.MULTI_RELATIONS:
        raise ValueError(
            f"T39 multi relations must be {common.MULTI_RELATIONS}, "
            f"got {multi_rows}"
        )
    return {
        "authored_compact_family_entries": common.FAMILY_COUNT,
        "expected_logical_relations": common.SOURCE_ROWS,
        "expanded_count_distribution": distribution,
        "family_count": common.FAMILY_COUNT,
        "family_ids": family_ids,
        "families": families,
        "generated_by": "python tools/build_t39_work_set.py",
        "host": common.HOST,
        "owner": common.OWNER,
        "publication_group_count": common.PUBLICATION_GROUP_COUNT,
        "publication_groups": {
            common.MULTI_GROUP: {
                "expanded_count": common.MULTI_RELATIONS,
                "family_count": common.MULTI_FAMILIES,
                "family_ids": multi_ids,
                "rule": "expanded_count > 1",
            },
            common.SINGLETON_GROUP: {
                "expanded_count": common.SINGLETON_RELATIONS,
                "family_count": common.SINGLETON_FAMILIES,
                "family_ids": singleton_ids,
                "rule": "expanded_count == 1",
            },
        },
        "schema_version": 1,
        "selection_rule": {
            "cc_host_map": common.HOST,
            "classification": "ordinary_optional",
            "membership_kind": "semantic_template",
            "sort": "template_key ascending",
            "take": "all",
        },
        "selection_sha256": digest,
        "source_map": common.SOURCE_MAP,
        "source_revision": common.SOURCE_REVISION,
        "source_rows": common.SOURCE_ROWS,
        "status": "T39_WORK_SET_FROZEN",
        "target_map": common.TARGET_MAP,
        "template_keys": template_keys,
        "unique_active_card": "T39",
    }


def build() -> dict[str, Any]:
    return build_work_set()


def main(argv: list[str] | None = None) -> int:
    args = common.parse_write_check("Freeze the T39 Centrifuge work set", argv)
    document = build_work_set()
    if args.check:
        errors = common.check_document(OUTPUT, document)
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        print("T39 work set is current.")
        return 0
    t35.write_stable(OUTPUT, document)
    print(f"Wrote {common.relative(OUTPUT)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
