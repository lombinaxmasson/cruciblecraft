#!/usr/bin/env python3
"""T42 census overlay: disposition only. No recipe/support tree, T14 delta 0."""
from __future__ import annotations

import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35
from tools import t41_common as t41
from tools import t42_common as common

OUTPUT = common.CENSUS_DELTA


def _gap() -> dict[str, Any]:
    if common.GAP_PARTITION.is_file():
        return common.load_json(common.GAP_PARTITION)
    from tools import build_t42_gap_partition as gap_builder

    return gap_builder.build()


def build() -> dict[str, Any]:
    gap = _gap()
    t41_census = common.load_json(t41.CENSUS_DELTA)
    t14 = t41_census.get("t14_load") or {}
    closing = dict(t14.get("closing") or common.t14_opening_load())
    if gap.get("completion_delta") != 0:
        raise ValueError("T42 census forbids a completion delta")
    axes = []
    for row in t14.get("axes") or []:
        if not isinstance(row, dict):
            continue
        copied = dict(row)
        copied["opening"] = copied.get("closing")
        copied["delta"] = 0
        copied["hard_ceiling_raised"] = False
        copied["evidence"] = (
            "T42 is a partition card; T14 opening equals T41 closing with delta 0."
        )
        axes.append(copied)
    return {
        "completion_delta": 0,
        "disposition_overlay": True,
        "eager_delta": 0,
        "generated_by": "python tools/build_t42_census_delta.py",
        "generated_recipe_tree": False,
        "generated_support_tree": False,
        "hard_ceiling_raised": False,
        "identity_closeout": {
            "already_expressed_delta": gap["already_expressed_delta"],
            "complete": True,
            "completion_delta": 0,
            "owns_families": 0,
            "publication_delta": 0,
            "reclassification_delta": gap["reclassification_delta"],
        },
        "note": (
            "T42 records current execution disposition. It does not author recipes "
            "or raise the T14 hard ceiling."
        ),
        "owns_families": 0,
        "publication_delta": 0,
        "remaining_ordinary": {
            "already_expressed_delta": gap["already_expressed_delta"],
            "closing_execution_gap": gap["closing_execution_gap"],
            "deferred_ordinary_ledger_count": gap["deferred_ordinary_ledger_count"],
            "opening_execution_gap": common.OPENING_EXECUTION_GAP,
            "reclassification_delta": gap["reclassification_delta"],
            "remaining_ordinary_families": gap["closing_execution_gap"],
            "t35_files_rewritten": False,
            "t37_files_rewritten": False,
            "t38_files_rewritten": False,
            "t39_files_rewritten": False,
            "t40_files_rewritten": False,
            "t41_files_rewritten": False,
        },
        "schema_version": 1,
        "source_revision": common.SOURCE_REVISION,
        "status": "T42_CENSUS_DELTA_READY",
        "t14_load": {
            "axes": axes,
            "closing": closing,
            "delta": {
                key: {"measured": True, "pending": False, "value": 0}
                for key in sorted(closing)
            },
            "hard_ceiling_raised": False,
            "opening": closing,
        },
        "t41_remaining_opening": common.OPENING_EXECUTION_GAP,
    }


def write() -> None:
    t35.write_stable(OUTPUT, build())


def check() -> list[str]:
    document = build()
    errors = common.check_document(OUTPUT, document)
    if document["t14_load"]["opening"] != document["t14_load"]["closing"]:
        errors.append("T42 T14 opening must equal closing")
    if document["hard_ceiling_raised"]:
        errors.append("T42 must not raise the T14 hard ceiling")
    return errors


def main(argv: list[str] | None = None) -> int:
    args = common.parse_managed("Build T42 census delta", argv)
    if common.handle_rebind(args, OUTPUT):
        return 0
    try:
        if args.write:
            write()
            print("Wrote T42 census delta.")
            return 0
        errors = check()
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        print("T42 census delta is current.")
        return 0
    except (OSError, ValueError, KeyError, FileNotFoundError) as error:
        print(f"T42 census delta failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
