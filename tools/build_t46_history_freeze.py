#!/usr/bin/env python3
"""Freeze T45-and-earlier card artifacts and v1 authorities as byte-identical T46 base."""
from __future__ import annotations

import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35
from tools import t46_common as common

OUTPUT = common.HISTORY_FREEZE


def _iter_frozen_files() -> list[Path]:
    seen: set[str] = set()
    files: list[Path] = []
    for path in common.FROZEN_AUTHORITY_FILES:
        if path.is_file():
            rel = common.relative(path)
            if rel not in seen:
                seen.add(rel)
                files.append(path)
    for pattern in common.FROZEN_CARD_GLOBS:
        for path in sorted(common.ROOT.glob(pattern)):
            if not path.is_file():
                continue
            rel = common.relative(path)
            if rel.startswith("tools/t46_"):
                continue
            if rel.endswith("_closeout_seal.json"):
                continue
            if rel not in seen:
                seen.add(rel)
                files.append(path)
    files.sort(key=lambda path: common.relative(path))
    return files


def build() -> dict[str, Any]:
    opening = common.opening_from_t45()
    files = []
    for path in _iter_frozen_files():
        files.append(
            {
                "path": common.relative(path),
                "sha256": t35.sha256_file(path),
            }
        )
    directories = []
    for root in common.FROZEN_GENERATED_ROOTS:
        directories.append(
            {
                "path": common.relative(root),
                "sha256": common.tree_sha256(root),
            }
        )
    return {
        "check_mode": "byte_hash_only",
        "directories": directories,
        "directory_count": len(directories),
        "file_count": len(files),
        "files": files,
        "generated_by": "python tools/build_t46_history_freeze.py",
        "note": (
            "T46 history freeze. --check compares SHA-256 only and must not "
            "rebuild or rewrite frozen v1 authorities or T37-T45 artifacts."
        ),
        "opening": opening,
        "schema_version": 1,
        "source_revision": common.SOURCE_REVISION,
        "status": "T46_HISTORY_FREEZE",
        "v1_authorities": {
            "compact_recipe_runtime_manifest": t35.sha256_file(common.V1_RUNTIME_MANIFEST),
            "global_build_identity_ledger": t35.sha256_file(common.V1_IDENTITY_LEDGER),
            "t14_load_budget_policy": t35.sha256_file(common.V1_LOAD_POLICY),
        },
    }


def main(argv: list[str] | None = None) -> int:
    return common.run_managed(
        "Freeze T46 historical v1 authorities and prior card artifacts",
        OUTPUT,
        build=build,
        argv=argv,
    )


if __name__ == "__main__":
    raise SystemExit(main())
