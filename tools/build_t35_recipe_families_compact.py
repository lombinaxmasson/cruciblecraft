#!/usr/bin/env python3
"""Compact currentness entry point for T35 recipe-family evidence.

The verification policy requires every builder script to have one owning
profile.  The source-replay builder is owned by ``census-replay``; this small,
distinct entry point owns ordinary census compact verification without reading
the GT6 dump maps.
"""
from __future__ import annotations

import argparse
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from tools import build_t35_recipe_families as recipe_families  # noqa: E402


def check() -> list[str]:
    return recipe_families.reference_only_check()


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args(argv)
    if not args.check:
        parser.error("choose --check")
    errors = check()
    if errors:
        print("\n".join(errors), file=sys.stderr)
        return 1
    print("tools/t35_recipe_families.json is current (compact)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
