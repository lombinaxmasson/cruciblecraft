#!/usr/bin/env python3
"""Freeze T49 Bath remainder source by slicing the frozen T48 remainder source."""
from __future__ import annotations

import sys
from collections import Counter
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35
from tools import wave_bath_tiny_purified as common

OUTPUT = common.SOURCE
RECEIPT = common.RECEIPT
REVIEW = common.REVIEW
SOURCE_PACK = common.SOURCE_PACK
WORK_SET = common.WORK_SET
BUILDER = Path(__file__).resolve()


def _mapping_counts(relations: list[dict[str, Any]]) -> dict[str, int]:
    counts: Counter[str] = Counter()
    for relation in relations:
        for operand in (
            list(relation.get("item_inputs") or [])
            + list(relation.get("item_outputs") or [])
            + list(relation.get("fluid_inputs") or [])
            + list(relation.get("fluid_outputs") or [])
        ):
            mapping = str(operand.get("mapping") or "")
            if mapping:
                counts[mapping] += 1
    return dict(sorted(counts.items()))


def _unmapped(relations: list[dict[str, Any]]) -> tuple[list[str], list[str]]:
    items: set[str] = set()
    fluids: set[str] = set()
    for relation in relations:
        for operand in (
            list(relation.get("item_inputs") or [])
            + list(relation.get("item_outputs") or [])
            + list(relation.get("fluid_inputs") or [])
            + list(relation.get("fluid_outputs") or [])
        ):
            if operand.get("mapping") != "blocked_unmapped" and operand.get("runtime_id"):
                continue
            source = operand.get("source") or {}
            if source.get("item"):
                items.add(f"{source.get('item')}@{source.get('meta')}")
            if source.get("fluid"):
                fluids.add(str(source.get("fluid")))
    return sorted(items), sorted(fluids)


def build() -> dict[str, Any]:
    work_set = common.load_json(WORK_SET)
    if work_set.get("status") != "T49_WORK_SET_FROZEN":
        raise ValueError("T49 source requires a frozen work set")
    if int(work_set.get("family_count") or 0) != common.CANDIDATE_FAMILY_COUNT:
        raise ValueError("T49 work set is not the 5-family remainder")
    wanted = set(work_set.get("family_ids") or [])
    if wanted != set(common.LOCK_FAMILY_IDS):
        raise ValueError("T49 work set family ids drifted from the lock remainder")
    parent = common.load_json(common.T48_SOURCE)
    if parent.get("status") != "T48_BATH_SOURCE_FROZEN":
        raise ValueError("T49 source requires frozen T48 remainder source")
    relations: list[dict[str, Any]] = []
    for relation in parent.get("relations") or []:
        family_id = str(relation.get("family_id") or "")
        if family_id not in wanted:
            continue
        row = dict(relation)
        row["publication_group"] = common.CANDIDATE_PUBLICATION_GROUP
        row["host"] = common.HOST
        stable_id = str(row.get("stable_id") or "")
        if stable_id.startswith("cruciblecraft:t48/"):
            row["stable_id"] = "cruciblecraft:t49/" + stable_id.split("/", 1)[1]
        elif stable_id and not stable_id.startswith("cruciblecraft:t49/"):
            raise ValueError(f"T49 source stable_id is not remintable: {stable_id}")
        relations.append(row)
    if len(relations) != common.CANDIDATE_RELATION_COUNT:
        raise ValueError(
            f"T49 source slice {len(relations)} != {common.CANDIDATE_RELATION_COUNT}"
        )
    assigned_ids = sorted({str(row["family_id"]) for row in relations})
    if assigned_ids != sorted(wanted):
        raise ValueError("T49 source slice families drifted from the work set")
    items, fluids = _unmapped(relations)
    if items or fluids:
        raise ValueError("T49 remainder slice still has unmapped operands")
    return {
        "assignment": {"assigned": len(relations), "missing": []},
        "blockers": [],
        "generated_by": "python tools/build_t49_bath_source.py",
        "host": common.HOST,
        "mapping_blockers": [],
        "owner": common.OWNER,
        "parent": {
            "path": common.relative(common.T48_SOURCE),
            "sha256": t35.sha256_file(common.T48_SOURCE),
            "status": parent.get("status"),
        },
        "relations": relations,
        "schema_version": 1,
        "source_revision": common.SOURCE_REVISION,
        "status": "T49_BATH_SOURCE_FROZEN",
        "work_set": {
            "family_count": len(wanted),
            "family_ids": list(work_set.get("family_ids") or []),
            "selection_sha256": work_set["selection_sha256"],
            "source_rows": len(relations),
        },
    }


def _receipt(source: dict[str, Any]) -> dict[str, Any]:
    parent_receipt = common.load_json(common.T48_RECEIPT)
    return {
        "builder_sha256": t35.sha256_file(BUILDER),
        "compact_sha256": t35.sha256_file(OUTPUT),
        "denominator_source_revision": common.SOURCE_REVISION,
        "full_replay": False,
        "generated_by": "python tools/build_t49_bath_source.py",
        "parent": {
            "compact_sha256": parent_receipt.get("compact_sha256"),
            "dump_file_hashes": parent_receipt.get("dump_file_hashes"),
            "path": common.relative(common.T48_SOURCE),
            "proof_tier": parent_receipt.get("proof_tier"),
            "receipt_path": common.relative(common.T48_RECEIPT),
            "source_oracle_dirty": parent_receipt.get("source_oracle_dirty"),
            "source_oracle_revision": parent_receipt.get("source_oracle_revision"),
        },
        "proof_tier": "parent_slice",
        "schema_version": 1,
        "selection_sha256": source["work_set"]["selection_sha256"],
        "skip_is_not_pass": True,
        "status": "T49_BATH_SOURCE_RECEIPT",
        "work_set_sha256": t35.sha256_file(WORK_SET),
    }


def _review(source: dict[str, Any]) -> dict[str, Any]:
    relations = list(source.get("relations") or [])
    items, fluids = _unmapped(relations)
    return {
        "generated_by": "python tools/build_t49_bath_source.py",
        "mapping_counts": _mapping_counts(relations),
        "r0_reconstruction_blockers": [],
        "relation_count": len(relations),
        "schema_version": 1,
        "source_revision": common.SOURCE_REVISION,
        "status": "T49_BATH_SOURCE_REVIEW",
        "unmapped_fluid_count": len(fluids),
        "unmapped_fluids": fluids,
        "unmapped_item_count": len(items),
        "unmapped_items": items,
    }


def _pack(source: dict[str, Any]) -> dict[str, Any]:
    return {
        "catalog": {
            "families": int(source["work_set"]["family_count"]),
            "relations": int(source["work_set"]["source_rows"]),
            "selection_sha256": source["work_set"]["selection_sha256"],
        },
        "files": {
            "receipt": {"path": common.relative(RECEIPT)},
            "review": {"path": common.relative(REVIEW)},
            "source": {
                "path": common.relative(OUTPUT),
                "sha256": t35.sha256_file(OUTPUT),
            },
            "work_set": {"path": common.relative(WORK_SET)},
        },
        "host": common.HOST,
        "owner": common.OWNER,
        "parent": {
            "path": common.relative(common.T48_SOURCE),
            "sha256": t35.sha256_file(common.T48_SOURCE),
        },
        "proof_tier": "parent_slice",
        "review_status": "T49_BATH_SOURCE_REVIEW",
        "schema_version": 1,
        "source_revision": common.SOURCE_REVISION,
        "status": "T49_SOURCE_PACK_FROZEN",
    }


def write() -> dict[str, Any]:
    source = build()
    t35.write_stable(OUTPUT, source)
    t35.write_stable(RECEIPT, _receipt(source))
    t35.write_stable(REVIEW, _review(source))
    t35.write_stable(SOURCE_PACK, _pack(source))
    return source


def check() -> list[str]:
    errors = common.check_document(OUTPUT, build())
    if not RECEIPT.is_file():
        errors.append("missing T49 source receipt")
        return errors
    receipt = common.load_json(RECEIPT)
    if receipt.get("compact_sha256") != t35.sha256_file(OUTPUT):
        errors.append("T49 source receipt compact_sha256 drifted")
    if receipt.get("proof_tier") != "parent_slice":
        errors.append("T49 source receipt must be parent_slice")
    if receipt.get("skip_is_not_pass") is not True:
        errors.append("T49 source receipt must record skip_is_not_pass")
    if REVIEW.is_file():
        errors.extend(common.check_document(REVIEW, _review(common.load_json(OUTPUT))))
    if SOURCE_PACK.is_file():
        errors.extend(common.check_document(SOURCE_PACK, _pack(common.load_json(OUTPUT))))
    return errors


def main(argv: list[str] | None = None) -> int:
    return common.run_managed(
        "Freeze the T49 Bath remainder source slice",
        OUTPUT,
        build=build,
        write=write,
        check=check,
        argv=argv,
    )


if __name__ == "__main__":
    raise SystemExit(main())
