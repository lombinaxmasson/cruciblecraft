#!/usr/bin/env python3
"""Write or check portfolio/source-capability-growth-order."""
from __future__ import annotations

import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import portfolio_source_capability as cap

SLUG = "portfolio/source-capability-growth-order"


def main(argv: list[str] | None = None) -> int:
    return cap.main_for(SLUG, argv)


if __name__ == "__main__":
    raise SystemExit(main())
