#!/usr/bin/env python3
"""Write or check the GT6 pipe/cable/redstone baseline audit."""
from __future__ import annotations

import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import gt6_pipe_cable_baseline as baseline


def main(argv: list[str] | None = None) -> int:
    return baseline.main(argv)


if __name__ == "__main__":
    raise SystemExit(main())
