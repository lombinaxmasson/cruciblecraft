#!/usr/bin/env python3
"""Build the T46+ forward-v2 load budget policy over the frozen v1 policy."""
from __future__ import annotations

import copy
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35
from tools import t46_common as common

OUTPUT = common.LOAD_POLICY_V2


def build() -> dict[str, Any]:
    v1 = common.load_json(common.V1_LOAD_POLICY)
    if v1.get("status") != "T14D_LOAD_BUDGET_POLICY_MEASURED":
        raise ValueError("v1 t14 load budget policy is not MEASURED")
    budgets = copy.deepcopy(v1["budgets"])
    authored = budgets["datapack_authored_entries"]
    authored["readiness_verdict"] = "REPORT_ONLY"
    authored["source"] = (
        "T46 forward-v2 keeps 6000/6600 as historical comparison references. "
        "datapack_authored_entries is REPORT_ONLY and does not raise "
        "BudgetExceededError or participate in winner / failed_gates."
    )
    v1_hash = t35.sha256_file(common.V1_LOAD_POLICY)
    return {
        "budgets": budgets,
        "composition": {
            "conflict_check": "pass",
            "consumed_deltas": [
                {
                    "order": 46,
                    "override": "datapack_authored_entries.readiness_verdict=REPORT_ONLY",
                    "wave_id": "T46",
                }
            ],
            "v1_file_sha256": v1_hash,
        },
        "generated_by": "python tools/build_t14_load_budget_policy_v2.py",
        "note": (
            "Forward-v2 load budget policy. Other axes inherit v1 hard gates. "
            "Authored entries stay counted but are report-only."
        ),
        "pending_measurements": list(v1.get("pending_measurements") or []),
        "pending_token": v1["pending_token"],
        "ratio_input_allowed": False,
        "schema_version": 2,
        "scope": {
            **dict(v1.get("scope") or {}),
            "authored_entries_verdict": "REPORT_ONLY",
            "hard_ceiling_behavior": (
                "Projection fails closed when a non-authored interval or scalar "
                "exceeds a hard ceiling. datapack_authored_entries overage is reported."
            ),
        },
        "status": "FORWARD_LOAD_BUDGET_POLICY_V2",
        "v1_base": {
            "file_sha256": v1_hash,
            "path": common.relative(common.V1_LOAD_POLICY),
            "status": v1.get("status"),
        },
    }


def main(argv: list[str] | None = None) -> int:
    return common.run_managed(
        "Build forward-v2 T14 load budget policy",
        OUTPUT,
        build=build,
        argv=argv,
    )


if __name__ == "__main__":
    raise SystemExit(main())
