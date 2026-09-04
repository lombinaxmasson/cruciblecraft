#!/usr/bin/env python3
"""Build the fail-closed V1 global build-time identity ledger."""
from __future__ import annotations

import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import builder_cli
from tools import census_common as census
from tools.recipe_bulk import identity as identity_mod
from tools.recipe_bulk.write_guard import assert_ledger_write

OUTPUT = census.TOOLS / "global_build_identity_ledger.json"


def build():
    return identity_mod.build()


def _write() -> None:
    document = build()
    assert_ledger_write(OUTPUT)
    census.write_stable(OUTPUT, document)
    from tools import currentness

    if currentness.target_row(OUTPUT) is not None:
        currentness.write_sidecar(OUTPUT)


def _check() -> list[str]:
    return census.check_generated_document(OUTPUT, build())


def main(argv: list[str] | None = None) -> int:
    args = builder_cli.parse_managed(
        "Build the V1 global build-time identity ledger",
        argv,
    )
    if args.rebind_currentness_only:
        from tools import currentness

        currentness.rebind_sidecar(OUTPUT)
        print(f"rebound currentness sidecar for {census.relative(OUTPUT)}")
        return 0
    if args.write:
        _write()
        print(f"Wrote {census.relative(OUTPUT)}")
        return 0
    errors = _check()
    if errors:
        print("\n".join(errors), file=sys.stderr)
        return 1
    print(f"{census.relative(OUTPUT)} is current")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
