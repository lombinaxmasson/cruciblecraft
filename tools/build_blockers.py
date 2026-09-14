#!/usr/bin/env python3
"""Write or check the cross-capability blocker ledger."""
from __future__ import annotations

import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import blockers as runtime


def main(argv: list[str] | None = None) -> int:
    return runtime.main(argv)


if __name__ == "__main__":
    raise SystemExit(main())
