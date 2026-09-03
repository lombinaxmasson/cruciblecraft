#!/usr/bin/env python3
"""Copy SOURCE_BACKED gregtech6_w multiitem icons onto remaining identities."""
from __future__ import annotations

import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import multiitem_art as art
from tools import t48_common as common
from tools import t35_common as t35

OUTPUT = art.MANIFEST


def build() -> dict[str, Any]:
    return art.build_manifest()


def write() -> dict[str, Any]:
    art.copy_source_backed()
    document = build()
    errors = art.check_models_and_pngs(document)
    if errors:
        raise ValueError("; ".join(errors))
    t35.write_stable(OUTPUT, document)
    return document


def check() -> list[str]:
    try:
        expected = build()
    except ValueError as exc:
        return [str(exc)]
    errors = common.check_document(OUTPUT, expected)
    errors.extend(art.check_models_and_pngs(expected))
    return errors


def main(argv: list[str] | None = None) -> int:
    return common.run_managed(
        "Bind SOURCE_BACKED multiitem icons from gregtech6_w",
        OUTPUT,
        build=build,
        write=write,
        check=check,
        argv=argv,
    )


if __name__ == "__main__":
    raise SystemExit(main())
