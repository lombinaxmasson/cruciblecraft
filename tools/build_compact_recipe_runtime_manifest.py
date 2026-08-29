#!/usr/bin/env python3
"""Freeze the 12-group compact runtime compatibility authority."""
from __future__ import annotations

import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import builder_cli
from tools import t35_common as t35
from tools.recipe_bulk import runtime as runtime_mod
from tools.recipe_bulk.write_guard import assert_ledger_write

OUTPUT = t35.TOOLS / "compact_recipe_runtime_manifest.json"


def build():
    return runtime_mod.build()


def _write() -> None:
    document = build()
    assert_ledger_write(OUTPUT)
    t35.write_stable(OUTPUT, document)
    from tools import currentness

    if currentness.target_row(OUTPUT) is not None:
        currentness.write_sidecar(OUTPUT)


def _check() -> list[str]:
    return t35.check_generated_document(OUTPUT, build())


def main(argv: list[str] | None = None) -> int:
    args = builder_cli.parse_managed(
        "Freeze compact recipe runtime compatibility groups and dedup rules",
        argv,
    )
    if args.rebind_currentness_only:
        from tools import currentness

        currentness.rebind_sidecar(OUTPUT)
        print(f"rebound currentness sidecar for {t35.relative(OUTPUT)}")
        return 0
    if args.write:
        _write()
        print(f"Wrote {t35.relative(OUTPUT)}")
        return 0
    errors = _check()
    if errors:
        print("\n".join(errors), file=sys.stderr)
        return 1
    print(f"{t35.relative(OUTPUT)} is current")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
