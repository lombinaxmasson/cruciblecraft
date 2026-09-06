#!/usr/bin/env python3
"""Copy SOURCE_BACKED gregtech6_w static block icons onto remaining identities."""
from __future__ import annotations

import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import block_art as art
from tools import census_common as census
from tools import bath_identity_common as common

OUTPUT = art.MANIFEST


def build() -> dict[str, Any]:
    if OUTPUT.is_file():
        return census.load_json(OUTPUT)
    return art.copy_and_write()


def write() -> dict[str, Any]:
    document = art.copy_and_write()
    errors = art.check_payload(document)
    if errors:
        raise ValueError("; ".join(errors))
    census.write_stable(OUTPUT, document)
    census.write_stable(art.BUNDLED_INDEX, art.bundled_index(document))
    return document


def check() -> list[str]:
    if not OUTPUT.is_file():
        return [f"missing {census.relative(OUTPUT)}"]
    try:
        document = census.load_json(OUTPUT)
    except ValueError as exc:
        return [str(exc)]
    errors = art.check_payload(document)
    errors.extend(art.check_models_and_pngs(document))
    if art.BUNDLED_INDEX.is_file():
        bundled = census.load_json(art.BUNDLED_INDEX)
        if bundled != art.bundled_index(document):
            errors.append("bundled block art index drifted from manifest")
    else:
        errors.append(f"missing bundled index {census.relative(art.BUNDLED_INDEX)}")
    return errors


def main(argv: list[str] | None = None) -> int:
    return common.run_managed(
        "Bind SOURCE_BACKED static block icons from gregtech6_w",
        OUTPUT,
        build=build,
        write=write,
        check=check,
        argv=argv,
    )


if __name__ == "__main__":
    raise SystemExit(main())
