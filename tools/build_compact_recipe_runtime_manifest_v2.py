#!/usr/bin/env python3
"""Write the composed forward-v2 compact runtime manifest."""
from __future__ import annotations

import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35
from tools import t46_common as common
from tools.recipe_bulk import runtime_v2

OUTPUT = common.RUNTIME_MANIFEST_V2


def build():
    return runtime_v2.build()


def _write() -> None:
    t35.write_stable(OUTPUT, build())
    from tools import currentness

    if currentness.target_row(OUTPUT) is not None:
        currentness.write_sidecar(OUTPUT)


def main(argv: list[str] | None = None) -> int:
    return common.run_managed(
        "Compose forward-v2 compact recipe runtime manifest",
        OUTPUT,
        build=build,
        write=_write,
        argv=argv,
    )


if __name__ == "__main__":
    raise SystemExit(main())
