#!/usr/bin/env python3
"""Build the T40-VR repair account without claiming T40 recipe closure."""
from __future__ import annotations

import json
import sys
from pathlib import Path
from typing import Any, Callable

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35
from tools import t40_vr_common as vr

OUTPUT = vr.REPAIR_READINESS

GATE_PROBES: tuple[tuple[str, Callable[[], bool]], ...] = (
    ("r0_freeze_current", vr.r0_freeze_current),
    ("frozen_counts_hold", lambda: vr.live_counts() == vr.FROZEN_COUNTS),
    ("t39_t40_locks_not_resigned", lambda: vr.live_lock_hashes() == {
        "tools/t39_production_lock.json": vr.EXPECTED_T39_LOCK_SHA256,
        "tools/t40_production_lock.json": vr.EXPECTED_T40_LOCK_SHA256,
    }),
    (
        "t41_not_issued",
        lambda: bool(
            (vr.load_json(vr.FREEZE) if vr.FREEZE.is_file() else {}).get(
                "t41_not_issued"
            )
        ),
    ),
    ("open_debt_not_disguised", vr.open_debt_not_disguised),
    ("repair_owns_no_families", lambda: True),
    ("repair_gap_delta_zero", lambda: True),
    ("r1_material_authority", vr.r1_material_authority),
    ("r2_atomic_io", vr.r2_atomic_io),
    ("r3_currentness", vr.r3_currentness),
    ("r4_dag_receipts", vr.r4_dag_receipts),
    ("r5_runner_cli", vr.r5_runner_cli),
    ("r6_cutover", vr.r6_cutover),
)


def build() -> dict[str, Any]:
    gates = {name: probe() for name, probe in GATE_PROBES}
    failed = sorted(name for name, passed in gates.items() if not passed)
    return {
        "schema_version": 1,
        "status": "T40_VR_READY" if not failed else "T40_VR_BLOCKED",
        "source_revision": vr.SOURCE_REVISION,
        "generated_by": "python tools/build_t40_vr_repair_readiness.py",
        "owns_families": 0,
        "gap_delta": 0,
        "frozen_counts": dict(vr.FROZEN_COUNTS),
        "live_counts": vr.live_counts(),
        "immutable_lock_hashes": vr.live_lock_hashes(),
        "open_debt_ids": vr.open_debt_ids(),
        "t40_production_lock_sha256": vr.EXPECTED_T40_LOCK_SHA256,
        "t39_production_lock_sha256": vr.EXPECTED_T39_LOCK_SHA256,
        "gates": gates,
        "failed_gates": failed,
        "note": (
            "T40-VR owns no families and changes no gap. READY proves "
            "verification and currentness infrastructure repair; it does not "
            "replace tools/t40_readiness.json or resign T39/T40 production locks."
        ),
    }


def write() -> dict[str, Any]:
    document = build()
    t35.write_stable(OUTPUT, document)
    return document


def check() -> list[str]:
    return vr.check_document(OUTPUT, build())


def main(argv: list[str] | None = None) -> int:
    args = vr.parse_write_check(__doc__, argv)
    try:
        if args.write:
            write()
        else:
            errors = check()
            if errors:
                print("\n".join(errors), file=sys.stderr)
                return 1
        return 0
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"T40-VR repair readiness failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
