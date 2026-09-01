#!/usr/bin/env python3
"""Record T44 runtime dependencies that must stay outside this card."""
from __future__ import annotations

import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t44_common as common  # noqa: E402

OUTPUT = common.RUNTIME_DEPENDENCY


def build() -> dict[str, Any]:
    return {
        "generated_by": "python tools/build_t44_runtime_dependency_manifest.py",
        "schema_version": 1,
        "status": "T44_RUNTIME_DEPENDENCY_MANIFEST",
        "source_revision": common.SOURCE_REVISION,
        "t36_live_machine_rows": common.T36_LIVE_MACHINE_ROWS,
        "recipe_closing_execution_gap": common.CLOSING_EXECUTION_GAP,
        "b0_inputs": [
            "minecraft:oak_planks",
            "minecraft:chest",
            "minecraft:book",
            "minecraft:glass_bottle",
            "cruciblecraft:steel",
            "cruciblecraft:plastic",
            "cruciblecraft:wood_treated",
            "cruciblecraft:programmed_circuit",
        ],
        "external_cards": ["T30", "T36", "T36-Repair", "T43"],
        "must_not_expand": {
            "t36_machine_rows": common.T36_LIVE_MACHINE_ROWS,
            "recipe_gap": common.CLOSING_EXECUTION_GAP,
        },
        "notes": [
            "Hopper remains T30 and is not re-registered by T44.",
            "Storage recipes are vanilla/GT shaped acquisition, not ordinary family completion.",
        ],
    }


def main(argv: list[str] | None = None) -> int:
    from tools import closeout_seal

    return common.run_managed(
        "Write the T44 runtime dependency manifest",
        OUTPUT,
        build=build,
        check=lambda: closeout_seal.live_or_sealed_errors(
            "T44",
            "runtime_dependency_manifest",
            lambda: common.check_document(OUTPUT, build()),
        ),
        argv=argv,
    )


if __name__ == "__main__":
    raise SystemExit(main())
