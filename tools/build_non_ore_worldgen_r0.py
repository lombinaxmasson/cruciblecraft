#!/usr/bin/env python3
"""Write or check portfolio/non-ore-worldgen-r0."""
from __future__ import annotations

import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import portfolio_non_ore_worldgen as worldgen


def main(argv: list[str] | None = None) -> int:
    return worldgen.main_for(argv)


if __name__ == "__main__":
    raise SystemExit(main())
