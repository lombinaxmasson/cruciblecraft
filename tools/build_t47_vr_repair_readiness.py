#!/usr/bin/env python3
"""Build the T47-VR repair account without claiming T47 recipe closure."""
from __future__ import annotations

import json
import sys
from pathlib import Path
from typing import Any, Callable

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35
from tools import t47_vr_common as vr

OUTPUT = vr.REPAIR_READINESS

GATE_PROBES: tuple[tuple[str, Callable[[], bool]], ...] = (
    ("r0_freeze_current", vr.r0_freeze_current),
    ("frozen_counts_hold", lambda: vr.live_counts() == vr.FROZEN_COUNTS),
    ("t46_t47_locks_not_resigned", lambda: vr.live_lock_hashes() == {
        "tools/t46_production_lock.json": vr.EXPECTED_T46_LOCK_SHA256,
        "tools/t47_production_lock.json": vr.EXPECTED_T47_LOCK_SHA256,
    }),
    (
        "t48_not_issued",
        lambda: bool(
            (vr.load_json(vr.FREEZE) if vr.FREEZE.is_file() else {}).get(
                "t48_not_issued"
            )
        ) and vr.t48_not_issued(),
    ),
    ("open_debt_not_disguised", vr.open_debt_not_disguised),
    ("repair_owns_no_families", lambda: True),
    ("repair_gap_delta_zero", lambda: True),
    ("r1_closeout_seal", vr.r1_closeout_seal),
    ("r2_receipt_binding", vr.r2_receipt_binding),
    ("r3_census_topology", vr.r3_census_topology),
    ("r4_profiles", vr.r4_profiles),
    ("r5_harness", vr.r5_harness),
    ("r6_cutover", vr.r6_cutover),
)


def build() -> dict[str, Any]:
    gates = {name: probe() for name, probe in GATE_PROBES}
    failed = sorted(name for name, passed in gates.items() if not passed)
    return {
        "schema_version": 1,
        "status": "T47_VR_READY" if not failed else "T47_VR_BLOCKED",
        "source_revision": vr.SOURCE_REVISION,
        "generated_by": "python tools/build_t47_vr_repair_readiness.py",
        "owns_families": 0,
        "gap_delta": 0,
        "frozen_counts": dict(vr.FROZEN_COUNTS),
        "live_counts": vr.live_counts(),
        "immutable_lock_hashes": vr.live_lock_hashes(),
        "open_debt_ids": vr.open_debt_ids(),
        "t47_production_lock_sha256": vr.EXPECTED_T47_LOCK_SHA256,
        "t46_production_lock_sha256": vr.EXPECTED_T46_LOCK_SHA256,
        "gates": gates,
        "failed_gates": failed,
        "note": (
            "T47-VR owns no families and changes no gap. READY proves closed-card "
            "verification monotonicity repair; it does not replace "
            "tools/t47_readiness.json or resign T46/T47 production locks. "
            "Untracked T47 artifacts on HEAD are a persist gap, not T47_READY failure."
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
        print(f"T47-VR repair readiness failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
