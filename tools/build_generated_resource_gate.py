#!/usr/bin/env python3
"""CLI for the live generated-resource / art gate."""
from __future__ import annotations

import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools.generated_resource_gate import main

if __name__ == "__main__":
    raise SystemExit(main())
