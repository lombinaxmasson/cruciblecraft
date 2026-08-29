#!/usr/bin/env python3
"""Replay T21 + T39/T40/T41 support and production into the T42 B0 baseline."""
from __future__ import annotations

import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35
from tools import t39_common as t39
from tools import t40_common as t40
from tools import t41_common as t41
from tools import t42_common as common

OUTPUT = common.REACHABILITY_BASELINE


def build() -> dict[str, Any]:
    t21 = common.load_json(common.T21_REACHABILITY)
    frontier = set((t21.get("closure") or {}).get("reachable_identities") or [])
    if not frontier:
        raise ValueError("T21 reachable identities are empty")
    layers = [
        {
            "identity_count": len(frontier),
            "layer": "t21_base",
            "note": "T21 typed closure already contains T37/T38 generated recipes",
            "source": "tools/t21_operand_reachability.json",
        }
    ]
    t39_layer = common.apply_support_and_production(
        frontier,
        support_path=common.T39_PLAYER_PATH_SUPPORT,
        generated_root=t39.GENERATED_ROOT,
        layer_name="t39",
    )
    layers.append(t39_layer)
    t40_layer = common.apply_support_and_production(
        frontier,
        support_path=t40.PLAYER_PATH_SUPPORT,
        generated_root=t40.GENERATED_ROOT,
        layer_name="t40",
    )
    layers.append(t40_layer)
    t41_layer = common.apply_support_and_production(
        frontier,
        support_path=t41.PLAYER_PATH_SUPPORT,
        generated_root=t41.GENERATED_ROOT,
        layer_name="t41",
    )
    layers.append(t41_layer)
    blocked = [
        row
        for layer in (t39_layer, t40_layer, t41_layer)
        for row in layer.get("blocked") or []
    ]
    identities = sorted(frontier)
    t21_base = set((t21.get("closure") or {}).get("reachable_identities") or [])
    added = sorted(set(identities) - t21_base)
    return {
        "added_identities": added,
        "approximations": [
            "Fluids use typed fixed-point closure; quantities and containers are not modelled."
        ],
        "b0_identity_count": len(identities),
        "b0_semantic_root": common.sha256_stable(identities),
        "excludes_t42_outputs": True,
        "fluid_reachability_approximate": True,
        "generated_by": "python tools/build_t42_reachability_baseline.py",
        "layers": layers,
        "reachable_identities": identities,
        "replay_blockers": blocked,
        "schema_version": 1,
        "source_revision": common.SOURCE_REVISION,
        "status": "T42_REACHABILITY_BASELINE",
        "t21_identity_count": int(
            (t21.get("closure") or {}).get("reachable_identity_count") or len(t21_base)
        ),
        "t21_sha256": common.sha256_file(common.T21_REACHABILITY),
    }


def write() -> None:
    t35.write_stable(OUTPUT, build())


def check() -> list[str]:
    return common.check_document(OUTPUT, build())


def main(argv: list[str] | None = None) -> int:
    args = common.parse_managed("Build T42 cumulative B0 reachability baseline", argv)
    if common.handle_rebind(args, OUTPUT):
        return 0
    try:
        if args.write:
            write()
            print("Wrote T42 reachability baseline.")
            return 0
        errors = check()
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        print("T42 reachability baseline is current.")
        return 0
    except (OSError, ValueError, KeyError) as error:
        print(f"T42 reachability baseline failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
