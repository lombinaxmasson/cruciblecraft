#!/usr/bin/env python3
"""Write or check the prep-only GT tree freeze and art."""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import gt_trees as trees


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args(argv)
    if args.check:
        errors = trees.check()
        if errors:
            sys.stderr.write("\n".join(errors) + "\n")
            return 1
        return 0
    if args.write:
        document = trees.write()
        sys.stdout.write(json.dumps(document, indent=2, sort_keys=True) + "\n")
        return 0
    parser.print_help()
    return 2


if __name__ == "__main__":
    raise SystemExit(main())
