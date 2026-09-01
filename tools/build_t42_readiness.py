#!/usr/bin/env python3
"""Derive T42_PARTITION_READY from freeze, overlay, lock, gap, census, topology."""
from __future__ import annotations

import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35
from tools import t42_common as common

OUTPUT = common.READINESS
JAVA_TESTS = (
    ROOT / "src/test/java/com/masson/cruciblecraft/recipe/gt/RecipeLogicalRelationIdentityTest.java",
    ROOT / "src/test/java/com/masson/cruciblecraft/census/CensusRuntimeInventoryClassifierTest.java",
)


def _load(path: Path, builder_name: str) -> dict[str, Any]:
    if path.is_file():
        return t35.load_json(path)
    module = __import__(f"tools.{builder_name}", fromlist=["build"])
    built = module.build()
    if builder_name == "build_t42_blocker_overlay":
        return built["overlay"]
    if builder_name == "build_t42_partition_freeze":
        return built["freeze"]
    return built


def _gate(name: str, ok: bool, detail: str | None = None) -> dict[str, Any]:
    row = {"gate": name, "ok": ok}
    if detail:
        row["detail"] = detail
    return row


def build() -> dict[str, Any]:
    freeze = _load(common.PARTITION_FREEZE, "build_t42_partition_freeze")
    catalog = (
        common.load_json(common.REMAINING_CATALOG)
        if common.REMAINING_CATALOG.is_file()
        else _load(common.PARTITION_FREEZE, "build_t42_partition_freeze")
    )
    if "catalog" in catalog:
        catalog = catalog["catalog"]
    snapshot_present = common.FAMILY_OPERAND_SNAPSHOT.is_file()
    inventory_present = common.RUNTIME_INVENTORY.is_file()
    baseline_present = common.REACHABILITY_BASELINE.is_file()
    overlay = _load(common.BLOCKER_OVERLAY, "build_t42_blocker_overlay")
    lock = _load(common.DISPOSITION_LOCK, "build_t42_disposition_lock")
    gap = _load(common.GAP_PARTITION, "build_t42_gap_partition")
    census = _load(common.CENSUS_DELTA, "build_t42_census_delta")
    topology = _load(common.CARD_TOPOLOGY, "build_t42_card_topology")
    java_ok = all(path.is_file() for path in JAVA_TESTS)
    lock_by_id = {row["family_id"]: row for row in lock.get("families") or []}
    gates = [
        _gate("owns_families_zero", freeze.get("owns_families") == 0 and gap.get("owns_families") == 0),
        _gate("publication_delta_zero", gap.get("publication_delta") == 0 and census.get("publication_delta") == 0),
        _gate("completion_delta_zero", gap.get("completion_delta") == 0),
        _gate("opening_5305", gap.get("opening_execution_gap") == common.OPENING_EXECUTION_GAP),
        _gate("remaining_catalog_5305", catalog.get("family_count") == common.OPENING_EXECUTION_GAP),
        _gate("snapshot_present", snapshot_present),
        _gate("runtime_inventory_present", inventory_present),
        _gate("b0_baseline_present", baseline_present),
        _gate("overlay_primary_buckets", overlay.get("family_count") == common.OPENING_EXECUTION_GAP),
        _gate("partial_family_count_zero", gap.get("partial_family_count") == 0),
        _gate(
            "partial_not_already_expressed",
            not any(
                row.get("primary_bucket") == "already_expressed"
                and "partial" in set(row.get("secondary_blockers") or [])
                for row in overlay.get("families") or []
            ),
        ),
        _gate(
            "partials_retained_in_gap",
            all(
                (lock_by_id.get(row["family_id"]) or {}).get("disposition")
                == "retained_current_execution_gap"
                for row in overlay.get("families") or []
                if "partial" in set(row.get("secondary_blockers") or [])
            ),
        ),
        _gate("lock_append_only", bool(lock.get("append_only"))),
        _gate("deferred_have_owner", all(
            (row.get("future_owner") and row.get("recheck_condition") and row.get("evidence_root_sha256"))
            for row in lock.get("families") or []
            if row.get("disposition") == "phase_deferred"
        )),
        _gate("t14_hard_ceiling_unraised", not census.get("hard_ceiling_raised")),
        _gate("no_recipe_tree", census.get("generated_recipe_tree") is False),
        _gate("next_issue_t43", topology.get("next_issue_id") == "T43"),
        _gate("t43_unassigned", topology.get("preassigned_host") is False and topology.get("preassigned_family_ids") is False),
        _gate("unique_active_content_card_null", topology.get("unique_active_content_card") is None),
        _gate("java_identity_inventory_tests", java_ok),
    ]
    failed = [row["gate"] for row in gates if not row["ok"]]
    status = "T42_PARTITION_READY" if not failed else "T42_PARTITION_BLOCKED"
    return {
        "failed_gates": failed,
        "gates": gates,
        "generated_by": "python tools/build_t42_readiness.py",
        "note": (
            "T42_PARTITION_READY requires failed_gates=[]. T43 is not issued from this card."
        ),
        "owns_families": 0,
        "schema_version": 1,
        "source_revision": common.SOURCE_REVISION,
        "status": status,
        "t43_not_issued": True,
    }


def write() -> None:
    t35.write_stable(OUTPUT, build())


def check() -> list[str]:
    return common.check_document(OUTPUT, build())


def main(argv: list[str] | None = None) -> int:
    args = common.parse_managed("Build T42 partition readiness", argv)
    if common.handle_rebind(args, OUTPUT):
        return 0
    try:
        if args.write:
            write()
            print("Wrote T42 readiness.")
            return 0
        errors = check()
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        print("T42 readiness is current.")
        return 0
    except (OSError, ValueError, KeyError, FileNotFoundError) as error:
        print(f"T42 readiness failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
