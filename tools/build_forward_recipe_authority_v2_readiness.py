#!/usr/bin/env python3
"""Prove v1 authorities are unchanged and forward-v2 composition is legal."""
from __future__ import annotations

import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35
from tools import t46_common as common
from tools.recipe_bulk import identity_v2
from tools.recipe_bulk import runtime_v2
from tools import build_t14_load_budget_policy_v2 as policy_v2

OUTPUT = common.FORWARD_V2_READINESS


def build() -> dict[str, Any]:
    freeze = common.load_json(common.HISTORY_FREEZE) if common.HISTORY_FREEZE.is_file() else {}
    v1_policy_hash = t35.sha256_file(common.V1_LOAD_POLICY)
    v1_identity_hash = t35.sha256_file(common.V1_IDENTITY_LEDGER)
    v1_runtime_hash = t35.sha256_file(common.V1_RUNTIME_MANIFEST)
    frozen = freeze.get("v1_authorities") or {}
    v1_unchanged = (
        frozen.get("t14_load_budget_policy") == v1_policy_hash
        and frozen.get("global_build_identity_ledger") == v1_identity_hash
        and frozen.get("compact_recipe_runtime_manifest") == v1_runtime_hash
    )
    policy = policy_v2.build()
    identity = identity_v2.build()
    runtime = runtime_v2.build()
    authored = (policy.get("budgets") or {}).get("datapack_authored_entries") or {}
    policy_bounded = authored.get("readiness_verdict") == "REPORT_ONLY"
    identity_ok = identity.get("composition", {}).get("conflict_check") == "pass"
    runtime_ok = (
        runtime.get("composition", {}).get("conflict_check") == "pass"
        and runtime.get("composition", {}).get("historical_membership_roots_unchanged") is True
        and int(runtime.get("composition", {}).get("base_group_count") or 0) == 12
    )
    t46_group_current = any(
        row.get("publication_group") == common.PUBLICATION_GROUP
        or row.get("wave_id") == "T46"
        for row in runtime.get("groups") or []
    )
    t47_identity_delta_path = t35.TOOLS / "t47_identity_ledger_delta.json"
    t47_identity_delta = (
        t35.load_json(t47_identity_delta_path)
        if t47_identity_delta_path.is_file()
        else {}
    )
    t47_identity_delta_current = (
        t47_identity_delta_path.is_file() and t47_identity_delta.get("order") == 47
    )
    t47_runtime_delta = runtime_v2.load_delta("T47")
    t47_runtime_groups = list(t47_runtime_delta.get("groups") or [])
    if not t47_runtime_groups:
        t47_runtime_group_current = True
    else:
        published = {
            str(row.get("publication_group") or "")
            for row in runtime.get("groups") or []
        }
        t47_runtime_group_current = any(
            str(row.get("publication_group") or "") in published
            for row in t47_runtime_groups
        )
    gates = {
        "history_freeze_current": freeze.get("status") == "T46_HISTORY_FREEZE",
        "identity_composition_legal": identity_ok,
        "policy_override_bounded": policy_bounded,
        "runtime_composition_legal": runtime_ok,
        "t46_runtime_group_current": t46_group_current,
        "t47_identity_delta_current": t47_identity_delta_current,
        "t47_runtime_group_current": t47_runtime_group_current,
        "v1_global_authorities_byte_identical": v1_unchanged,
    }
    failed = [name for name, value in gates.items() if not value]
    # Runtime group is only required after the T46 delta exists; R0-A may
    # compose v1+empty-delta. Readiness stays BLOCKED until the group lands.
    status = (
        "FORWARD_RECIPE_AUTHORITY_V2_READY"
        if not failed
        else "FORWARD_RECIPE_AUTHORITY_V2_BLOCKED"
    )
    return {
        "failed_gates": failed,
        "gates": gates,
        "generated_by": "python tools/build_forward_recipe_authority_v2_readiness.py",
        "hashes": {
            "identity_v2": t35.sha256_file(common.IDENTITY_LEDGER_V2)
            if common.IDENTITY_LEDGER_V2.is_file()
            else "",
            "policy_v2": t35.sha256_file(common.LOAD_POLICY_V2)
            if common.LOAD_POLICY_V2.is_file()
            else "",
            "runtime_v2": t35.sha256_file(common.RUNTIME_MANIFEST_V2)
            if common.RUNTIME_MANIFEST_V2.is_file()
            else "",
            "v1_identity": v1_identity_hash,
            "v1_policy": v1_policy_hash,
            "v1_runtime": v1_runtime_hash,
        },
        "note": (
            "Forward-v2 readiness proves the frozen v1 base is unchanged, the "
            "authored override is bounded, and delta composition is legal. It "
            "does not rewrite closed v1 READY documents."
        ),
        "owns_families": 0,
        "schema_version": 1,
        "status": status,
    }


def _write() -> None:
    t35.write_stable(OUTPUT, build())
    from tools import currentness

    if currentness.target_row(OUTPUT) is not None:
        currentness.write_sidecar(OUTPUT)


def main(argv: list[str] | None = None) -> int:
    return common.run_managed(
        "Build forward-v2 recipe authority readiness",
        OUTPUT,
        build=build,
        write=_write,
        argv=argv,
    )


if __name__ == "__main__":
    raise SystemExit(main())
