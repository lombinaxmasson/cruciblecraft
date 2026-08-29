#!/usr/bin/env python3
"""Freeze the T41 Assembler catalog work set from the immutable T35 family ledger."""
from __future__ import annotations

import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35  # noqa: E402
from tools import t41_common as common  # noqa: E402

OUTPUT = common.WORK_SET


def build_work_set(families_doc: dict[str, Any] | None = None) -> dict[str, Any]:
    selected = common.select_remaining_assembler_families(families_doc)
    family_ids = [str(row["family_id"]) for row in selected]
    template_keys = [str(row["template_key"]) for row in selected]
    expanded_counts = [int(row["expanded_count"]) for row in selected]
    if len(family_ids) != common.CATALOG_FAMILY_COUNT:
        raise ValueError(
            f"T41 catalog work set must contain {common.CATALOG_FAMILY_COUNT} families, "
            f"got {len(family_ids)}"
        )
    if len(set(family_ids)) != common.CATALOG_FAMILY_COUNT:
        raise ValueError("T41 work set has duplicate family ids")
    if len(set(template_keys)) != common.CATALOG_FAMILY_COUNT:
        raise ValueError("T41 work set has duplicate template keys")
    if template_keys != sorted(template_keys):
        raise ValueError("T41 template keys are not sorted ascending")
    source_rows = sum(expanded_counts)
    if source_rows != common.CATALOG_RELATION_COUNT:
        raise ValueError(
            f"T41 catalog expanded_count total must be {common.CATALOG_RELATION_COUNT}, "
            f"got {source_rows}"
        )
    digest = common.selection_sha256(family_ids)
    if digest != common.EXPECTED_SELECTION_SHA256:
        raise ValueError(
            "T41 catalog selection_sha256 drifted: "
            f"computed {digest}, expected {common.EXPECTED_SELECTION_SHA256}"
        )
    distribution = common.expanded_count_distribution(expanded_counts)
    expected_distribution = {
        str(size): count for size, count in common.EXPECTED_DISTRIBUTION.items()
    }
    if distribution != expected_distribution:
        raise ValueError(
            f"T41 catalog expanded_count distribution drifted: {distribution}"
        )
    families = []
    singleton_ids: list[str] = []
    combinatorial_ids: list[str] = []
    singleton_rows = 0
    combinatorial_rows = 0
    for row, expanded_count in zip(selected, expanded_counts):
        template_key = str(row["template_key"])
        family_id = str(row["family_id"])
        catalog_group = (
            common.COMBINATORIAL_CATALOG_GROUP
            if template_key in common.COMBINATORIAL_TEMPLATE_KEYS
            else common.SINGLETON_CATALOG_GROUP
        )
        families.append(
            {
                "expanded_count": expanded_count,
                "family_id": family_id,
                "publication_group": None,
                "catalog_group": catalog_group,
                "template_key": template_key,
            }
        )
        if catalog_group == common.COMBINATORIAL_CATALOG_GROUP:
            combinatorial_ids.append(family_id)
            combinatorial_rows += expanded_count
        else:
            singleton_ids.append(family_id)
            singleton_rows += expanded_count
    if len(singleton_ids) != common.CATALOG_SINGLETON_FAMILIES:
        raise ValueError(
            f"T41 catalog singleton families must be {common.CATALOG_SINGLETON_FAMILIES}, "
            f"got {len(singleton_ids)}"
        )
    if singleton_rows != common.CATALOG_SINGLETON_RELATIONS:
        raise ValueError(
            f"T41 catalog singleton relations must be {common.CATALOG_SINGLETON_RELATIONS}, "
            f"got {singleton_rows}"
        )
    if len(combinatorial_ids) != common.CATALOG_COMBINATORIAL_FAMILIES:
        raise ValueError(
            "T41 catalog combinatorial families must be "
            f"{common.CATALOG_COMBINATORIAL_FAMILIES}, got {len(combinatorial_ids)}"
        )
    if combinatorial_rows != common.CATALOG_COMBINATORIAL_RELATIONS:
        raise ValueError(
            "T41 catalog combinatorial relations must be "
            f"{common.CATALOG_COMBINATORIAL_RELATIONS}, got {combinatorial_rows}"
        )
    combinatorial_keys = [
        row["template_key"]
        for row in families
        if row["catalog_group"] == common.COMBINATORIAL_CATALOG_GROUP
    ]
    if combinatorial_keys != list(common.COMBINATORIAL_TEMPLATE_KEYS):
        raise ValueError(
            "T41 combinatorial catalog families must be "
            f"{common.COMBINATORIAL_TEMPLATE_KEYS}, got {combinatorial_keys}"
        )
    return {
        "authored_compact_family_entries": common.CATALOG_FAMILY_COUNT,
        "catalog_only": True,
        "expected_logical_relations": common.CATALOG_RELATION_COUNT,
        "expanded_count_distribution": distribution,
        "family_count": common.CATALOG_FAMILY_COUNT,
        "family_ids": family_ids,
        "families": families,
        "generated_by": "python tools/build_t41_work_set.py",
        "host": common.HOST,
        "note": (
            "294/1531 is the catalog fixture, not the production denominator. "
            "Plank publication groups are assigned after source replay. "
            "Production counts are frozen later in tools/t41_production_lock.json."
        ),
        "owner": common.OWNER,
        "publication_group_count": common.PUBLICATION_GROUP_COUNT,
        "publication_groups": {
            common.COMBINATORIAL_CATALOG_GROUP: {
                "expanded_count": common.CATALOG_COMBINATORIAL_RELATIONS,
                "family_count": common.CATALOG_COMBINATORIAL_FAMILIES,
                "family_ids": combinatorial_ids,
                "rule": "template_key in {gt.recipe.assembler#0000, #0001}",
            },
            common.SINGLETON_CATALOG_GROUP: {
                "expanded_count": common.CATALOG_SINGLETON_RELATIONS,
                "family_count": common.CATALOG_SINGLETON_FAMILIES,
                "family_ids": singleton_ids,
                "rule": "expanded_count == 1 and not T37-closed",
            },
        },
        "schema_version": 1,
        "selection_rule": {
            "cc_host_map": common.HOST,
            "classification": "ordinary_optional",
            "exclude_family_numbers": "0002-0051",
            "membership_kind": "semantic_template",
            "sort": "template_key ascending",
            "take": "remaining assembler families",
        },
        "selection_sha256": digest,
        "source_map": common.SOURCE_MAP,
        "source_revision": common.SOURCE_REVISION,
        "source_rows": source_rows,
        "status": "T41_CATALOG_WORK_SET_FROZEN",
        "target_map": common.TARGET_MAP,
        "template_keys": template_keys,
        "unique_active_card": "T41",
    }


def build() -> dict[str, Any]:
    return build_work_set()


def main(argv: list[str] | None = None) -> int:
    return common.run_managed(
        "Freeze the T41 Assembler catalog work set",
        OUTPUT,
        build=build_work_set,
        argv=argv,
    )


if __name__ == "__main__":
    raise SystemExit(main())
