#!/usr/bin/env python3
"""Bind T45 generated compact families to lock stable ids."""
from __future__ import annotations

import hashlib
import json
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35
from tools import t45_common as common

OUTPUT = common.EQUIVALENCE


def _stable(value: Any) -> str:
    return json.dumps(value, ensure_ascii=False, indent=2, sort_keys=True) + "\n"


def _digest_text(text: str) -> str:
    return hashlib.sha256(text.encode("utf-8")).hexdigest()


def build() -> dict[str, Any]:
    files = common.generated_family_files()
    expected = common.production_family_count()
    if len(files) != expected:
        raise ValueError(f"T45 generated family count drifted: {len(files)} != {expected}")
    generated_hashes: dict[str, str] = {}
    stable_ids: list[str] = []
    for path in files:
        content = path.read_text(encoding="utf-8")
        relative = path.relative_to(common.GENERATED_ROOT).as_posix()
        generated_hashes[relative] = _digest_text(content)
        document = json.loads(content)
        if document.get("parameterized"):
            raise ValueError(f"T45 authored JSON must not carry parameterized: {relative}")
        for relation in document.get("relations") or []:
            stable_ids.append(str(relation["stable_id"]))
    lock_ids = list((common.load_production_lock().get("production") or {}).get("stable_ids") or [])
    if sorted(stable_ids) != sorted(lock_ids):
        raise ValueError("T45 generated stable ids drifted from the production lock")
    return {
        "generated": {
            "file_count": len(files),
            "files": generated_hashes,
            "root": common.relative(common.GENERATED_ROOT),
            "sha256": _digest_text(_stable(generated_hashes)),
        },
        "runtime_expected": {
            "logical_ids": len(stable_ids),
            "sha256": _digest_text(_stable(stable_ids)),
            "stable_ids": stable_ids,
        },
        "schema_version": 1,
        "source": {
            "family_count": expected,
            "path": common.relative(common.SOURCE),
            "relation_count": expected,
            "scope": "production",
            "sha256": t35.sha256_file(common.SOURCE),
            "source_revision": common.SOURCE_REVISION,
        },
        "status": "T45_EQUIVALENCE",
    }


def main(argv: list[str] | None = None) -> int:
    return common.run_managed(
        "Bind T45 generated equivalence",
        OUTPUT,
        build=build,
        argv=argv,
    )


if __name__ == "__main__":
    raise SystemExit(main())
