#!/usr/bin/env python3
"""Write or check the electric wire/cable MTE fold overlay."""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import electric_wire_cable_mte_fold as fold


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    if args.write:
        result = fold.write()
        print(json.dumps(result, indent=2, sort_keys=True))
        return 0
    if args.check:
        errors = fold.check()
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        print("electric wire/cable MTE fold is current")
        return 0
    parser.error("specify --write or --check")
    return 2


if __name__ == "__main__":
    raise SystemExit(main())
