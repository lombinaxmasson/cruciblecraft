#!/usr/bin/env python3
"""Write the composed forward-v3 global identity ledger envelope."""
from __future__ import annotations

import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35
from tools.recipe_bulk import identity_v3

OUTPUT = identity_v3.OUTPUT


def build():
    return identity_v3.build()


def _write() -> None:
    t35.write_stable(OUTPUT, build())


def main(argv: list[str] | None = None) -> int:
    import argparse

    parser = argparse.ArgumentParser(description=__doc__)
    modes = parser.add_mutually_exclusive_group(required=True)
    modes.add_argument("--check", action="store_true")
    modes.add_argument("--write", action="store_true")
    args = parser.parse_args(argv)
    expected = build()
    if args.write:
        _write()
        print(f"Wrote {t35.relative(OUTPUT)}")
        return 0
    if not OUTPUT.is_file():
        print(f"missing {t35.relative(OUTPUT)}", file=sys.stderr)
        return 1
    actual = t35.load_json(OUTPUT)
    errors = t35.first_json_diff(expected, actual)
    if errors:
        print(errors, file=sys.stderr)
        return 1
    print("global_build_identity_ledger.v3.json is current")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
