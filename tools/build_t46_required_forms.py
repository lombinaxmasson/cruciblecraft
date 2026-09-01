#!/usr/bin/env python3
"""Emit the empty T46 required-forms sidecar. Recipe builder must not write the gate."""
from __future__ import annotations

import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35
from tools import t46_common as common

OUTPUT = common.REQUIRED_FORMS


def build() -> dict[str, Any]:
    return {
        "counts": {
            "dust_div72_materials": 0,
            "required_form_pairs": 0,
            "small_dust_materials": 0,
        },
        "dust_div72_source_metas": [],
        "note": (
            "T46 Bath MTE operands are exact item/fluid identities, not material "
            "forms. Recipe builder does not write material_registration_gate.json."
        ),
        "required_forms": {},
        "schema_version": 1,
        "source": {
            "path": common.relative(common.SOURCE),
            "sha256": t35.sha256_file(common.SOURCE),
        },
        "status": "T46_REQUIRED_FORMS",
    }


def main(argv: list[str] | None = None) -> int:
    return common.run_managed(
        "Write the empty T46 required-forms sidecar",
        OUTPUT,
        build=build,
        argv=argv,
    )


if __name__ == "__main__":
    raise SystemExit(main())
