#!/usr/bin/env python3
"""Write or check portfolio/generic-recipe-import-proof."""
from __future__ import annotations

import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import portfolio_generic_recipe_import as importer

SLUG = "portfolio/generic-recipe-import-proof"


def main(argv: list[str] | None = None) -> int:
    return importer.main_for(SLUG, argv)


if __name__ == "__main__":
    raise SystemExit(main())
